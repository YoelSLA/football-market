# Implementation Plan: Historial personal de operaciones (Backend)

**Feature Branch**: `feat/020-view-operation-history`
**Estado**: Arquitectura backend vigente.
**Referencias normativas**: [spec.md](spec.md), [contracts/operation-history-api.md](contracts/operation-history-api.md) y [data-model.md](data-model.md).

---

## 1. Contexto técnico

- **Lenguaje y runtime**: Java 21.
- **Framework y stack**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security con JWT, Hibernate, PostgreSQL, SpringDoc OpenAPI, Lombok.
- **Persistencia**: PostgreSQL; lectura sobre `market_transactions`, `users`, `players`, `teams` y `leagues`. Reutilización de índices existentes (`idx_market_transactions_buyer`, `idx_market_transactions_seller`). Sin cambios de esquema ni migraciones nuevas.
- **Pruebas backend**: JUnit Jupiter, `@Nested`, `@Tag`, `@WebMvcTest` para Controller y Testcontainers con PostgreSQL real para Service y Repository (`docs/backend/testing.md`).

---

## 2. Flujo de capas y componentes

```text
OperationHistoryController
  ├── @AuthenticationPrincipal Jwt (extracción de subject)
  ├── Validación de parámetros (page, size, search, type, fechas, sort)
  └── invocación a OperationHistoryService
        │
        ▼
OperationHistoryServiceImpl (@Transactional(readOnly = true))
  ├── Resolución del usuario por correo (subject)
  ├── Construcción y validación de OperationHistoryCriteria
  ├── MarketTransactionRepository
  │     ├── Consulta paginada con fetch joins (buyer, seller, player, team, league)
  │     ├── Consulta de conteo (count) con mismos predicados
  │     └── Consulta de agregación (OperationHistoryAggregate) con mismos predicados
  └── Composición de OperationHistoryView
        │
        ▼
OperationHistoryMapper
  └── Mapeo estático a OperationHistoryResponseDTO (summary, transactions, pagination)
```

---

## 3. Estrategia de consulta y rendimiento

1. **Evitar N+1**: La consulta de contenido carga en una sola sentencia las relaciones to-one:
   - `LEFT JOIN FETCH t.buyer`
   - `LEFT JOIN FETCH t.seller`
   - `LEFT JOIN FETCH t.player p`
   - `LEFT JOIN FETCH p.team tm`
   - `LEFT JOIN FETCH tm.league lg`
2. **Conteo desacoplado**: La consulta de conteo (`count`) aplica los mismos predicados de filtrado sin fetch joins para evitar sobrecarga en base de datos.
3. **Agregados optimizados**: La consulta de agregados (`OperationHistoryAggregate`) ejecuta en SQL sumas y conteos condicionales sobre el conjunto filtrado, evitando traer filas a memoria para calcular métricas.
4. **Desempate determinista**: Todo criterio de orden (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`) incluye desempate estable por fecha de ejecución e ID de transacción para garantizar que la paginación no salte ni duplique registros.

---

## 4. Tipos y modelos de dominio

- `OperationHistoryCriteria`: record inmutable que encapsula `page`, `size`, `search`, `type`, `executedFrom`, `executedBefore` y `sort`.
- `OperationHistoryAggregate`: record que contiene los 7 totales agregados (`totalTransactions`, `buyTransactions`, `sellTransactions`, `boughtTokens`, `soldTokens`, `purchasedVolume`, `soldVolume`).
- `OperationHistoryView`: record que agrupa la página de `OperationHistoryEntry`, el `OperationHistoryAggregate` y los metadatos de paginación.
- `OperationHistoryEntry`: record que representa una transacción individual tipificada (`BUY` o `SELL`), valores financieros históricos y perfil actual del jugador.
- `OperationHistoryResponseDTO`: DTO público documentado con anotaciones OpenAPI, dividido en `SummaryDTO`, `TransactionDTO`, `PlayerDTO`, `RelatedDTO` y `PaginationDTO`.

---

## 5. Manejo de excepciones y seguridad

- **Autenticación**: Extracción del subject del JWT; si el usuario no existe en la base de datos o el token es inválido, responde HTTP 401.
- **Rango de fechas**: Si `executedFrom >= executedBefore`, se lanza `InvalidOperationHistoryDateRangeException` que `PortfolioExceptionHandler` mapea a HTTP 400 (`INVALID_DATE_RANGE`).
- **Parámetros inválidos**: Errores de tipo en `page`, `size`, `type` o `sort` devuelven HTTP 400 (`INVALID_PARAMETER_TYPE`).
- **Fallo interno**: Excepciones no controladas devuelven HTTP 500 con código `INTERNAL_ERROR` y mensaje sanitizado, sin exponer datos parciales.

# Implementation Plan: Historial personal de transacciones (SPEC-025)

**Feature Branch**: `feat/025-view-transaction-history`
**Estado**: Plan de implementación vigente.
**Referencias normativas**: [spec.md](spec.md), [contracts/transaction-history.md](contracts/transaction-history.md) y [data-model.md](data-model.md).

---

## 1. Resumen de arquitectura

La funcionalidad integra el backend paginado de `GET /api/users/me/transactions` con la vista desktop `/portfolio/transactions` en la sección de Cartera. La solución ofrece:
- Controles de filtrado y orden sincronizados bidireccionalmente con los parámetros de la URL.
- Consulta mediante TanStack Query con clave compuesta de criterios.
- Componentes modulares reutilizables con tokens SCSS y retícula de 8 px.
- Región vertical flexible con estados de contenido compacto centrado, filas naturales y paginación inferior.

---

## 2. Contexto técnico

- **Backend**: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security JWT, PostgreSQL.
- **Frontend**: TypeScript 7.0.2, React 19.3.0, TanStack Query, React Router, SCSS Modules.
- **Persistencia**: Reutilización de `market_transactions`, `users`, `players`, `teams` y `leagues` sin migraciones nuevas.
- **Verificación**: Controles ejecutables de agente (`./gradlew build -x test`, `npm run biome:check`, `npm run build`); ejecución de pruebas automatizadas y validación visual reservadas al usuario.

---

## 3. Arquitectura backend

```text
OperationHistoryController
  → OperationHistoryService(criteria)
      → MarketTransactionRepository
          ├── Page<MarketTransaction> (filtrada, ordenada, con fetch joins to-one)
          ├── Conteo de totalElements (sin fetch joins)
          └── OperationHistoryAggregate (métricas sumadas y contadas en base de datos)
  → OperationHistoryMapper
  → OperationHistoryResponseDTO (summary, transactions, pagination)
```

- La consulta de contenido precarga `buyer`, `seller`, `player`, `player.team` y `player.team.league` en una única consulta para eliminar el problema de N+1.
- Conteo y agregados aplican exactamente los mismos predicados que la consulta de contenido.
- Los cuatro órdenes (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`) incorporan desempates deterministas por fecha de ejecución e ID de transacción.

---

## 4. Arquitectura frontend

```text
TransactionHistoryPage
  ├── useTransactionHistoryPage (sincronización con URL y coordinación de estado)
  │     └── useTransactionHistoryQuery (TanStack Query con query key parametrizada)
  ├── TransactionHistoryHeader (título y navegación de contexto)
  ├── TransactionHistoryMetrics (4 tarjetas resumen de compras, ventas y volúmenes)
  ├── TransactionHistoryFilters (búsqueda, tipo, fechas, orden y botón limpiar)
  ├── TransactionHistoryContent
  │     └── TransactionHistoryEntry (PlayerPortrait, datos de jugador, tipo y finanzas)
  ├── Pagination (componente compartido con navegación de páginas)
  └── TransactionHistoryState / TransactionHistoryEmptyState (contenido compacto centrado)
```

- `portfolio.service.ts`: cliente HTTP tipado que invoca `GET /api/users/me/transactions`.
- `portfolio.mapper.ts`: traduce `OperationHistoryResponseDTO` a `TransactionHistory`.
- La URL es la única fuente de verdad: cambiar filtros u orden reinicia la página a cero; paginar conserva los demás criterios.

---

## 5. Diseño de verificación

- **Backend**: Cobertura integral en Repository (consultas y desempates), Service (validaciones y agregados) y Controller (contrato JSON, códigos 200, 400, 401 y 500).
- **Frontend**: Cumplimiento de Biome, compilación de SCSS Modules y build de producción sin errores.
- **Validación visual**: Inspección en resolución desktop de referencia (1920×913 px) sin desbordamiento horizontal.

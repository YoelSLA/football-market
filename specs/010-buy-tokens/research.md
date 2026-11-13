# Research: Mercado Oficial — Compra de tokens y catálogo con cotizaciones integradas (SPEC-010)

Decisiones técnicas de la consolidación del 2026-10-16 (US1: Compra de tokens) y de la evolución del 2026-11-12 (US2: Catálogo del Mercado Oficial, Issue #90). Cada decisión indica los motivos y las alternativas descartadas. La fuente funcional normativa es [spec.md](spec.md).

---

## Decisiones vigentes de US1 — Compra de tokens (2026-10-16)

### R-01. Arquitectura de una sola fachada para la compra
*Origen*: `docs/backend/architecture.md` §1.1, §1.3 y §1.4.
**Decisión**: La compra se implementa con `BuyOrderService` (interfaz) y `BuyOrderServiceImpl` (implementación). La implementación depende de Model y Repository, y comparte un único límite transaccional. No existen `TokenService`, `CreditService` ni `MarketAuditService`.
**Razón**: §1.3 permite a un Service depender de Model, Repository e Integration. La prohibición de dependencias entre Services hace inviable invocar servicios financieros auxiliares desde la fachada.

### R-02. Identidad, vendedor y precios en la compra
*Origen*: FR-002, FR-003, FR-009, FR-013.
**Decisión**:
- El comprador se resuelve por el sujeto autenticado mediante `UserRepository`.
- El vendedor se resuelve con `UserRepository.findByRole(UserRole.SUPERUSER)`.
- El precio se obtiene del puerto Integration `QuoteSource`. Si no resuelve superusuario, lanza `SellerUnavailableException` y revierte con 500 genérico (FR-023, AC19).

### R-03. Unidad monetaria: centésimas enteras
*Origen*: CL-01, FR-008, FR-022.
**Decisión**: Todos los importes monetarios se representan en centésimas de crédito con `long` en Java y `BIGINT` en PostgreSQL. 1 crédito = 100 centésimas. Se calcula con `Math.multiplyExact` y un desbordamiento revierte con 500 genérico.

### R-04. Registro de auditoría financiera
*Origen*: FR-014, SC-010.
**Decisión**: La compra persiste un registro en `market_transactions` dentro de la misma transacción de compra. Sin servicio separado de auditoría.

### R-05. Concurrencia y bloqueos pesimistas
*Origen*: FR-011, FR-012, FR-013, AC08, AC09.
**Decisión**: Transacción única con bloqueos pesimistas en orden determinista estricto para evitar deadlocks y doble gasto.

---

## Decisiones técnicas de US2 — Catálogo del Mercado Oficial (Evolución 2026-11-12, Issue #90)

### R-16. Consulta consolidada mediante `LEFT JOIN LATERAL` en `features/market`

*Origen*: FR-024, FR-026, FR-027, SC-017, SC-018.

**Decisión**:
La consulta `GET /api/market/official` se resuelve encapsulada dentro del repositorio de infraestructura de mercado (`OfficialMarketRepository`) mediante una consulta SQL con `LEFT JOIN LATERAL`, sin crear vistas permanentes ni nuevas migraciones Flyway:

```sql
LEFT JOIN LATERAL (
    SELECT
        pq.id AS quote_id,
        pq.price,
        pq.previous_price,
        CASE
            WHEN pq.previous_price IS NOT NULL AND pq.previous_price > 0
            THEN ROUND(((pq.price - pq.previous_price)::numeric / pq.previous_price::numeric) * 100, 2)
            ELSE NULL
        END AS variation_percentage,
        pq.score,
        pq.multiplier,
        pq.origin,
        pq.effective_date,
        pq.calculated_at
    FROM player_quotes pq
    WHERE pq.player_id = p.id
    ORDER BY
        pq.effective_date DESC,
        (CASE WHEN pq.origin = 'RECOMPUTED' THEN 0 ELSE 1 END),
        pq.calculated_at DESC,
        pq.id DESC
    LIMIT 1
) q ON true
```

**Razón y evaluación frente a alternativas**:
1. **Sin migraciones de base de datos**: No requiere crear objetos DDL globales en PostgreSQL ni añadir archivos de migración Flyway adicionales.
2. **Aislamiento de la feature `market`**: Encapsula la lógica de lectura consolidada dentro de su propio repositorio, sin acoplar el esquema global de la base de datos a una vista específica.
3. **Equivalencia exacta de negocio**: Replica estrictamente los cuatro criterios de ordenamiento de [`PlayerQuoteRepository.findLatestByPlayerId`](file:///home/yoelsla/Escritorio/football-market-010/backend/src/main/java/footballmarket/features/quote/infraestructure/PlayerQuoteRepository.java#L27-L32):
   - Fecha de vigencia descendente (`effective_date DESC`).
   - Prevalencia de la cotización recalculada a igualdad de fecha (`RECOMPUTED` precede a `INITIAL` / `CARRIED_FORWARD`).
   - Instante de cálculo descendente (`calculated_at DESC`).
   - Desempate determinista por identificador de cotización descendente (`id DESC`).
4. **Cero N+1 en la JVM**: Resuelve jugadores y cotizaciones en una única consulta de datos paginada desde PostgreSQL.

**Riesgo de duplicación de reglas y mitigación**:
- *Riesgo*: La regla canónica de precedencia de cotizaciones se encuentra definida en JPQL en `PlayerQuoteRepository` (`features/quote`) y se formula en SQL en `OfficialMarketRepository` (`features/market`). Si en el futuro `quote` modifica su semántica, ambas consultas podrían divergir si no se sincronizan.
- *Mitigación*: Se incorpora una suite de pruebas automatizadas de equivalencia estricta (`OfficialMarketQuoteEquivalenceTest`). Estas pruebas configuran múltiples combinaciones de cotizaciones (iniciales, arrastradas, recalculadas en el mismo día, con instantes de cálculo cercanos) y comprueban asertivamente que la cotización embebida por `OfficialMarketRepository` coincida exactamente con la entregada por `PlayerQuoteRepository.findLatestByPlayerId`.

**Alternativas descartadas**:
- *Vista Flyway convencional `v_current_player_quotes`*: Descartada para evitar añadir objetos DDL permanentes al esquema y no forzar una evaluación global sobre todo el historial de la tabla antes de filtrar.
- *Subconsulta JPQL correlacionada con NOT EXISTS*: Extremadamente verbosa, de difícil mantenimiento y sin soporte nativo para `NULLS LAST` en JPA.
- *Lectura en dos fases en Java (Catalog Page + Batch Quotes)*: Descartada porque impide aplicar filtros financieros globales (`minPrice`, `maxPrice`, `hasQuote`) y ordenamiento por cotización sobre el conjunto total previo a paginar.

---

### R-17. Respeto estricto de la arquitectura y límites entre features

*Origen*: `docs/backend/architecture.md` §1.1, §1.3 y §1.4.

**Decisión**:
El catálogo del Mercado Oficial reside íntegramente dentro de la feature `market`:
- **Controller**: `OfficialMarketController` (`@RestController @RequestMapping("/api/market/official")`).
- **Service**: `OfficialMarketCatalogService` (interfaz) y `impl/OfficialMarketCatalogServiceImpl` (implementación).
- **Repository**: `OfficialMarketRepository` (Spring Data / Native query con `LEFT JOIN LATERAL`).
- **DTOs / Mappers**: `OfficialMarketQueryRequestDTO`, `OfficialMarketPageResponseDTO`, `OfficialMarketPlayerResponseDTO`, `OfficialMarketQuoteResponseDTO`, `OfficialMarketMapper`.

**Razón**:
1. Cumple la prohibición de dependencias `Service → Service` de §1.3: `OfficialMarketCatalogServiceImpl` depende exclusivamente de `OfficialMarketRepository` y de su modelo de proyección; no invoca a `PlayerCatalogService` ni a `QuoteQueryService`.
2. Aísla las responsabilidades: `features/catalog` gestiona identidades deportivas; `features/quote` calcula y almacena cotizaciones; `features/market` expone la vista consolidada de mercado primario y ejecuta compras.
3. No afecta el endpoint de compra existente `BuyOrderService` ni `OrderController`.

---

### R-18. Estrategia desacoplada de Paginación y Conteo

*Origen*: FR-029, FR-030, FR-031, FR-032, FR-036, D9, D12, D14.

**Decisión**:
Se adopta el patrón canónico de Spring Data desacoplando la consulta de datos de la consulta de conteo:
1. **Count Query**:
   - Si la consulta **no incluye filtros financieros** (`minPrice`, `maxPrice`, `hasQuote`), el conteo se realiza exclusivamente sobre `players`, `teams` y `leagues`, evitando unir innecesariamente con `player_quotes`.
   - Si la consulta **incluye filtros financieros**, ejecuta el `LEFT JOIN LATERAL` para contar únicamente los jugadores cuya cotización vigente satisface los rangos y condiciones solicitadas.
2. **Data Query**:
   - Aplica el `LEFT JOIN LATERAL`, las condiciones de filtrado, la cláusula de ordenamiento con `NULLS LAST` y las cláusulas `LIMIT :size OFFSET :offset`.
3. **Filtro de nombre (`name`)**: Se aplica `trim()`. Si tras el recorte queda vacío, se omite el filtro. Si contiene texto, se reutiliza la función SQL de base de datos `catalog_normalize_name` existente en PostgreSQL (`V20261107030011`).

---

### R-19. Ordenamiento con `NULLS LAST`, desempate determinista y verificación de ejecución

*Origen*: FR-033, FR-034, D10, D11, D13.

**Decisión**:
- Se valida que la solicitud contenga un **único criterio de ordenamiento principal**. Múltiples criterios o parámetros `sort` repetidos arrojan `400 Bad Request` (`INVALID_SORT_PARAMETER`).
- El ordenamiento por `price` y `variation` aplica explícitamente `NULLS LAST` tanto en dirección `ASC` como `DESC` en SQL:
  - `price,asc`: `ORDER BY q.price ASC NULLS LAST, p.id ASC`
  - `price,desc`: `ORDER BY q.price DESC NULLS LAST, p.id ASC`
  - `variation,asc`: `ORDER BY q.variation_percentage ASC NULLS LAST, p.id ASC`
  - `variation,desc`: `ORDER BY q.variation_percentage DESC NULLS LAST, p.id ASC`
  - `name,asc`: `ORDER BY catalog_normalize_name(p.name) ASC, p.id ASC`
  - `name,desc`: `ORDER BY catalog_normalize_name(p.name) DESC, p.id ASC`
  - `id,asc`: `ORDER BY p.id ASC`
- El desempate por `p.id ASC` se incluye de forma obligatoria en todas las variantes, asegurando paginación determinista sobre conjuntos de datos sin modificaciones concurrentes.
- **Verificación de plan de ejecución**: La eficiencia y comportamiento del plan de ejecución (utilización de índices en `player_quotes` y `players`) no se presupone a priori; se contemplará su verificación mediante logging de consultas e inspección del plan (`EXPLAIN`) durante las pruebas de integración en el entorno de testing.

---

### R-20. Sigilo estricto de inventario del superusuario

*Origen*: FR-026, FR-035, AC31, SC-020, D6.

**Decisión**:
La consulta SQL de `OfficialMarketRepository` y los DTOs de salida excluyen por diseño cualquier `JOIN` o proyección hacia `player_token_holdings`, `player_token_emissions` o balances de usuarios. La presencia de un jugador solo certifica que `p.active = true` y su equipo `t.current = true`. La existencia o agotamiento de stock se comprueba exclusivamente en el momento de invocar `POST /api/orders/buy`.

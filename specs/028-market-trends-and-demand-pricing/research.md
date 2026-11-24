# Investigación: tendencias y ajuste de oferta-demanda

## Alcance investigado

Decisiones de diseño para PLAN a partir de `spec.md`, arquitectura y tecnologías del backend, contratos y modelos vigentes de SPEC-016, SPEC-020 y SPEC-026, y código de `features/market` y `features/quote`. No se consultaron proveedores externos ni se ejecutaron aplicaciones o pruebas.

## Decisiones

### D-01 — Clasificación de transacciones

**Decisión**: usar las partes persistidas en `MarketTransaction`. Una fila con comprador `USER` cuenta como compra, incluida la compra de una publicación P2P; una fila con comprador `SUPERUSER` y vendedor `USER` cuenta como venta oficial. Cada fila pertenece a una sola categoría.

**Fundamento**: el buyback registra naturalmente al superusuario como comprador; las compras oficiales y P2P registran al usuario comprador. La aclaración del usuario confirma que la compra P2P incrementa la demanda y no se contabiliza otra vez como venta por tener vendedor. `listing_id` identifica el origen P2P y `discount_percentage` la auditoría de buyback, sin modificar transacciones confirmadas.

**Alternativas consideradas**: contar ambos lados del P2P produciría demanda neta cero por construcción; excluir el P2P perdería compras reales. Agregar un tipo de operación persistido no es necesario para distinguir las filas existentes.

### D-02 — Agregaciones sobre transacciones existentes

**Decisión**: consultar `market_transactions` directamente mediante Repository y agregaciones SQL/JPQL, filtradas por el rango temporal del contrato. La demanda neta que afecta cada cotización se calcula por jugador; sentimiento y volumen expuestos por el endpoint son globales. Para `topHotPlayers`, sumar cantidades e importes de compras por jugador y limitar a cinco con orden determinista. Volúmenes globales y porcentajes usan las filas clasificadas en D-01.

**Fundamento**: evita cargar todas las entidades financieras y reutiliza el ledger append-only. No requiere tabla, cache ni mutación de transacciones.

**Alternativas consideradas**: mantener snapshots o métricas materializadas agrega persistencia y sincronización que la especificación no requiere.

### D-03 — Una sola ventana configurable

**Decisión**: agregar una propiedad de duración a `market.quote`, con default `PT24H`; todos los casos de uso consumen el mismo valor y capturan un único `Instant` por consulta/ejecución.

**Fundamento**: implementa las clarificaciones vigentes de SPEC-028 y evita ventanas divergentes.

### D-04 — Reutilización del recálculo

**Decisión**: mantener `QuoteRecalculationService` como fachada y `QuotePlayerPublisher` como publicación atómica por jugador. El modo habitual resuelve score deportivo, obtiene demanda neta e incorpora el factor de mercado antes de aplicar cap y piso. El modo solo de mercado no consulta `QuoteScoreResolver`.

**Fundamento**: la arquitectura encapsula recorrido, bloqueo por jugador, persistencia diaria y límite financiero. Distinguir modos evita que una ejecución independiente consuma scores por accidente.

**Alternativas consideradas**: crear un segundo motor/publicador duplicaría reglas de precio, concurrencia y publicación diaria.

### D-05 — Auditoría explícita, sin reconstrucción histórica

**Decisión**: añadir a `player_quotes` componentes de mercado y deportivo, incluida la demanda neta individual, para reconstruir la fórmula; añadir a `quote_executions` modo y ventana. El multiplicador persistido actual conserva el multiplicador aplicado final. Filas históricas mantienen nulos donde no se pueda reconstruir el componente.

**Fundamento**: FR-005 y FR-008 exigen atribución diferenciada; `PlayerQuote` ya conserva precio previo y multiplicador final, y `QuoteExecution` guarda parámetros/resultados. No usar logs como auditoría persistente ni inventar auditoría pasada.

### D-06 — HTTP y seguridad existentes

**Decisión**: ampliar `QuoteController` para el POST exclusivo e incorporar Controller de tendencias; usar DTO `record`, Mapper estático y anotaciones OpenAPI. Mantener rutas `/api/...`, `ErrorResponseDTO` y seguridad JWT vigente; no añadir rol.

**Fundamento**: sigue convenciones y arquitectura backend. `SecurityConfig` exige autenticación a rutas API y SPEC-016 define el criterio JWT vigente para ejecución administrativa.

## Riesgos y controles

- La regla P2P queda fijada por la clarificación: compra solamente; no duplicar su cantidad como venta.
- Usar límites temporales coherentes y deterministas (inicio incluido, fin exclusivo) en endpoint y recálculos, y exponer el período observado.
- Los campos nuevos nulos en filas históricas significan que el componente no es reconstruible; no presentarlos como impacto cero conocido.
- Conservar el tratamiento individual de fallos del recálculo vigente y no guardar una cotización con componentes parciales.
- Añadir índices de ventana/player solo si el diseño de consulta y esquema demuestran que son necesarios; no fijar un SLA no solicitado.

## Fuentes técnicas

- `docs/backend/architecture.md` §§1, 1.7; `docs/backend/data-model.md` §5.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`.
- `specs/020-view-operation-history/contracts/operation-history-api.md`.
- `specs/026-market-instant-buyback-and-p2p-pricing/research.md` y contratos `official-buyback.md`, `p2p-listing-price.md`.
- `MarketTransactionRepository`, `QuoteRecalculationServiceImpl`, `QuotePlayerPublisher`, `QuoteAdjustment`, `PlayerQuote`, `QuoteExecution`, `QuoteProperties` y `SecurityConfig`.

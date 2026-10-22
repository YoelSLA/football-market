---

description: "Task list template for feature implementation"
---

# Tasks: Cotización de tokens de jugadores con historial trazable

**Input**: Design documents from `/specs/016-ejecutar-recalculo-manual-de-cotizaciones/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/quotes-http.md,
contracts/quote-source.md, quickstart.md

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend Java: `backend/src/main/java/footballmarket/...`
- Backend tests: `backend/src/test/java/footballmarket/...`
- Migraciones: `backend/src/main/resources/db/migration/`
- Configuración: `backend/src/main/resources/application.yml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Esquema de persistencia y configuración base del control de variabilidad.

- [X] T001 [P] Crear la migración `backend/src/main/resources/db/migration/V<timestamp>__create_player_quotes.sql` con `./gradlew createMigration -PmigrationDescription=create_player_quotes`: tablas `player_quotes` y `quote_executions`, secuencias, restricciones `uq_player_quote` sobre `(player_id, effective_date, origin)` y `uq_quote_executions_date`, CHECK de precio positivo, origen, fila inicial, fila arrastrada, fila recalculada y contadores, e índices `ix_player_quotes_player_date` e `ix_player_quotes_effective_price` (data-model.md §1, §2, §5)
- [X] T002 Crear `backend/src/main/java/footballmarket/config/QuoteProperties.java` con `@ConfigurationProperties("market.quote")`: factor de ajuste por defecto `0.10`, tope de variación por defecto `0.10` (fracción en el rango cerrado (0; 1]), piso mínimo por defecto `1` centésima y ancla inicial `100` centésimas, configurables sin declaración explícita (FR-012, FR-014, FR-015)
- [X] T003 [P] Declarar los valores por defecto `market.quote.adjustment-factor`, `market.quote.variation-cap` y `market.quote.min-price-floor` en `backend/src/main/resources/application.yml` y registrar `@EnableConfigurationProperties(QuoteProperties.class)` en la configuración de Spring que ya activa las propiedades existentes del área (FR-015)

**Checkpoint**: esquema y configuración base disponibles.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelo, repositorios, catálogo de estrategias, errores y componentes compartidos por todas las historias.

**⚠️ CRITICAL**: No puede iniciarse ninguna historia hasta completar esta fase.

- [X] T004 Crear el model `PlayerQuote` en `backend/src/main/java/footballmarket/models/PlayerQuote.java` con los campos de FR-023 (`player`, `effectiveDate`, `price`, `previousPrice`, `multiplier`, `score`, `strategyName`, `strategyVersion`, `sourceValuationDate`, `origin`, `calculatedAt`) y sus invariantes de origen (research.md §2)
- [X] T005 [P] Crear el enum `QuoteOrigin` en `backend/src/main/java/footballmarket/models/enums/QuoteOrigin.java` con `INITIAL`, `RECOMPUTED` y `CARRIED_FORWARD` (FR-021)
- [X] T006 [P] Reutilizar el enum existente `footballmarket.models.enums.ValuationStrategy` de SPEC-013 como catálogo cerrado versionado de estrategias de cotización, sin ampliarlo y sin crear un enum nuevo: sus constantes son exactamente `POSITIONAL_PERFORMANCE` y `GENERAL_ATTRIBUTES` (FR-018, FR-055, Constitution §V)
- [X] T007 Crear el model `QuoteExecution` en `backend/src/main/java/footballmarket/models/QuoteExecution.java` con `quoteDate`, estrategia y versión, factor, tope, piso, contadores (`pricedPlayers`, `recomputedRows`, `carriedForwardRows`, `notRepricedPlayers`, `failedPlayers`), `startedAt` y `finishedAt` (data-model.md §2)
- [X] T008 Crear `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java` con las consultas de última fila por jugador con desempate (`effectiveDate DESC, calculatedAt DESC`, prevalece la fila `RECOMPUTED`), historial ordenado por `effectiveDate DESC`, fila con `effectiveDate <= fecha` y existencia de fila `INITIAL` (mercado inicializado) (FR-026, FR-036 a FR-040, FR-044)
- [X] T009 [P] Crear `backend/src/main/java/footballmarket/repositories/QuoteExecutionRepository.java` con la persistencia de corridas y la consulta por fecha de cotización (FR-061)
- [X] T010 Crear las excepciones de dominio nuevas en `backend/src/main/java/footballmarket/services/exceptions/`: `MarketNotInitializedException`, `MarketAlreadyInitializedException`, `InvalidAdjustmentFactorException`, `InvalidVariationCapException`, `InvalidMinPriceFloorException`, `UnknownQuoteStrategyException`, `QuoteNotFoundException` y `ExecutionAlreadyExistsException` (research.md §15)
- [X] T011 Mapear en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` las ocho excepciones nuevas a sus códigos y estados HTTP (`MARKET_NOT_INITIALIZED` 409, `MARKET_ALREADY_INITIALIZED` 409, `INVALID_ADJUSTMENT_FACTOR` 400, `INVALID_VARIATION_CAP` 400, `INVALID_MIN_PRICE_FLOOR` 400, `UNKNOWN_QUOTE_STRATEGY` 400, `QUOTE_NOT_FOUND` 404, `EXECUTION_ALREADY_EXISTS` 409), conservando `QUOTE_UNAVAILABLE` intacto (FR-043, FR-045, research.md §15)
- [X] T012 [P] Crear `backend/src/main/java/footballmarket/services/internal/QuotePopulationSelector.java` para obtener la población cotizable desde los `player_id` distintos de `player_statistics` cuyo último registro tenga al menos una métrica informada, sin depender de la cobertura de las valuaciones (FR-001 a FR-003, research.md §6)
- [X] T013 [P] Crear `backend/src/main/java/footballmarket/services/internal/QuoteScoreResolver.java` para resolver el score persistido de SPEC-013 (`final_score` para `POSITIONAL_PERFORMANCE`, `final_grl` para `GENERAL_ATTRIBUTES`) usando `ValuationStrategy` como catálogo de estrategias, junto con su fecha de valuación y la estrategia y versión, sin recalcular valuaciones ni llamar a proveedores externos (FR-006, FR-010, T006)

**Checkpoint**: modelo, persistencia, errores y componentes compartidos listos; las historias pueden empezar.

---

## Phase 3: User Story 1 - Inicializar el mercado con el ancla de un crédito (Priority: P1) 🎯 MVP

**Goal**: Poder inicializar el mercado una única vez, dejando una fila `INITIAL` de 100 centésimas por jugador cotizable y rechazando operaciones de mercado sobre un mercado no inicializado.

**Independent Test**: Inicializar sobre un catálogo con métricas, comprobar una única fila inicial de 100 centésimas por jugador cotizable, idempotencia al repetir y rechazo con otra fecha; comprobar que compra y publicación de venta se rechazan con `MARKET_NOT_INITIALIZED` sin mover recursos.

### Tests para User Story 1 (backend según normas; sin tests frontend)

> Escribir tests backend necesarios; resultados y ejecución quedan a cargo del usuario. MUST NOT ejecutar tests desde el agente.

- [X] T014 [P] [US1] Escribir test de servicio de inicialización en `backend/src/test/java/footballmarket/services/QuoteMarketInitializationServiceTest.java`: escritura de una fila `INITIAL` por jugador cotizable, idempotencia en la misma fecha, rechazo con `MARKET_ALREADY_INITIALIZED` ante cualquier otra fecha de mercado, y no creación de filas para jugadores sin métricas ni para jugadores con un registro de métricas sin métricas informadas (FR-031 a FR-035)
- [X] T015 [P] [US1] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java` para `POST /api/quotes/initialize`: respuesta con `marketDate`, `initializedPlayers` y `alreadyInitialized`, y rechazo `409 MARKET_ALREADY_INITIALIZED` (contracts/quotes-http.md §1)
- [X] T016 [P] [US1] Escribir test de persistencia del esquema en `backend/src/test/java/footballmarket/repositories/PlayerQuoteMigrationTest.java` que verifique unicidad `(player_id, effective_date, origin)`, coexistencia de una fila `RECOMPUTED` y una `CARRIED_FORWARD` de la misma fecha de vigencia, CHECK de precio positivo y CHECK de invariantes de origen (data-model.md §1)

### Implementation for User Story 1

- [X] T017 [US1] Crear la interfaz `QuoteMarketInitializationService` en `backend/src/main/java/footballmarket/services/QuoteMarketInitializationService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java`: fecha de mercado derivada de las filas `INITIAL`, escritura de una fila inicial por jugador cotizable con `price = 100`, `multiplier = 1`, `previousPrice` nulo, sin estrategia ni score, idempotencia en la misma fecha de mercado y rechazo controlado `MARKET_ALREADY_INITIALIZED` ante cualquier otra fecha de mercado (FR-031 a FR-035, research.md §8)
- [X] T018 [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/InitializeQuotesRequestDTO.java` con el campo opcional `marketDate` y `backend/src/main/java/footballmarket/controllers/dtos/responses/InitializeMarketResponseDTO.java` con `marketDate`, `initializedPlayers` y `alreadyInitialized` (contracts/quotes-http.md §1)
- [X] T019 [US1] Crear `backend/src/main/java/footballmarket/controllers/mappers/QuoteMapper.java` con el mapeo de la respuesta de inicialización (sin lógica de negocio) (plan.md §Project Structure)
- [X] T020 [US1] Crear `backend/src/main/java/footballmarket/controllers/QuoteController.java` con `POST /api/quotes/initialize`, anotaciones OpenAPI y validación Jakarta del body, delegando en `QuoteMarketInitializationService` (FR-031, contracts/quotes-http.md §1)

**Checkpoint**: User Story 1 es funcional y verificable de forma independiente.

---

## Phase 4: User Story 2 - Recalcular y persistir la cotización con re-ajuste compuesto (Priority: P1)

**Goal**: Ejecutar el recálculo manual con estrategia y factor elegidos por el operador, aplicando `precioNuevo = precioVigente × (1 + k × (score − 0,5))` acotado por el tope y el piso, y persistiendo el historial trazable y el resumen de la corrida.

**Independent Test**: Catálogo con métricas y valuaciones de ambas estrategias; ejecutar el recálculo una vez por fecha de cotización y comprobar precio resultante, marca `CARRIED_FORWARD` en los no valuados, estrategia y versión registradas, ausencia de duplicados y rechazo de una segunda ejecución en la misma fecha.

### Tests para User Story 2 (backend según normas; sin tests frontend)

- [X] T021 [P] [US2] Escribir el test unitario de dominio `backend/src/test/java/footballmarket/models/QuoteAdjustmentTest.java` para la fórmula, redondeo half-up, acotamiento por tope y aplicación del piso mínimo; y el test de servicio de recálculo `backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java` para fila `RECOMPUTED` con score y estrategia, fila `CARRIED_FORWARD` sin score y con estrategia heredada, comparación contra la fecha de vigencia de referencia sin que una fila arrastrada la altere, coexistencia de una fila `RECOMPUTED` con una `CARRIED_FORWARD` de la misma fecha con prevalencia de la repreciada, jugador con valuación no posterior a su fecha de vigencia de referencia no repreciado, precedencia de rechazos FR-066 y rechazo `EXECUTION_ALREADY_EXISTS` en la segunda corrida del día y ante ejecuciones concurrentes de la misma fecha (FR-004 a FR-016, FR-026, FR-027, FR-060, FR-066)
- [X] T022 [P] [US2] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java` para `POST /api/quotes/recalculate` y `GET /api/quotes/executions?date=`: forma de la respuesta con contadores y `startedAt`/`finishedAt`, rechazo con `401` de toda solicitud sin JWT válido, y códigos `INVALID_ADJUSTMENT_FACTOR`, `INVALID_VARIATION_CAP`, `INVALID_MIN_PRICE_FLOOR`, `UNKNOWN_QUOTE_STRATEGY`, `MARKET_NOT_INITIALIZED` y `EXECUTION_ALREADY_EXISTS` (FR-059, contracts/quotes-http.md §2, §3)
- [X] T023 [P] [US2] Escribir test de persistencia en `backend/src/test/java/footballmarket/repositories/QuoteExecutionPersistenceTest.java` que verifique `uq_quote_executions_date`, que una ejecución rechazada no persiste fila y que dos ejecuciones concurrentes de la misma fecha producen una sola ejecución persistida y un `EXECUTION_ALREADY_EXISTS` controlado (FR-060, FR-066)

### Implementation for User Story 2

- [X] T024 [US2] Crear `backend/src/main/java/footballmarket/models/QuoteAdjustment.java` como Model inmutable responsable de calcular el multiplicador acotado y el precio resultante con redondeo half-up y piso mínimo; y crear `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java` para publicar la cotización de un jugador en su propia transacción `REQUIRES_NEW`, consumiendo ese resultado de dominio, aplicando la condición de escritura de FR-027 contra la fecha de vigencia de referencia —la última fila `INITIAL` o `RECOMPUTED` del jugador, que las filas arrastradas no alteran— y permitiendo la coexistencia de una fila `RECOMPUTED` con una `CARRIED_FORWARD` de la misma fecha, y registrando internamente el fallo individual con jugador, etapa y causa sin exponerlo en la respuesta (FR-005, FR-012, FR-013, FR-016, FR-026, FR-027, FR-062, research.md §13)
- [X] T025 [US2] Crear la interfaz `QuoteRecalculationService` en `backend/src/main/java/footballmarket/services/QuoteRecalculationService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java`: fecha de cotización UTC del día, orden de precedencia de rechazos FR-066, aplicación del multiplicador acotado, redondeo half-up en centésimas, piso mínimo, escritura condicional por posterioridad estricta respecto de la fecha de vigencia de referencia (última fila `INITIAL` o `RECOMPUTED`), contadores con la definición de FR-061 y persistencia del resumen de ejecución (FR-005, FR-011 a FR-013, FR-016, FR-027, FR-060, FR-061, FR-063, FR-066)
- [X] T026 [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/RecalculateQuotesRequestDTO.java` con `strategy` obligatorio y `adjustmentFactor`, `variationCap` (fracción en el rango cerrado (0; 1]) y `minPriceFloor` opcionales, y `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteExecutionResponseDTO.java` con la forma del resumen de ejecución (FR-012, contracts/quotes-http.md §2)
- [X] T027 [US2] Crear `backend/src/main/java/footballmarket/controllers/mappers/QuoteExecutionMapper.java` con el mapeo del resumen de ejecución persistido a la respuesta HTTP (plan.md §Project Structure)
- [X] T028 [US2] Completar `backend/src/main/java/footballmarket/controllers/QuoteController.java` con `POST /api/quotes/recalculate` y `GET /api/quotes/executions`, anotaciones OpenAPI y validación de los parámetros por rango antes de ejecutar, garantizando que ambas rutas exigen JWT válido y rechazan con `401` toda solicitud no autorizada (FR-011, FR-059, FR-061, contracts/quotes-http.md §2, §3)

**Checkpoint**: User Stories 1 y 2 son funcionales y verificables de forma independiente.

---

## Phase 5: User Story 3 - Consultar el historial y la cotización vigente e histórica (Priority: P2)

**Goal**: Exponer el historial ordenado, la cotización vigente y la cotización a una fecha dada, con los datos de auditoría completos.

**Independent Test**: Catálogo cotizado en varias fechas: el historial devuelve filas de más reciente a más antigua, la vigente devuelve la última con su instante de cálculo de origen y la consulta por fecha devuelve la última con fecha de vigencia no superior a la solicitada.

### Tests para User Story 3 (backend según normas; sin tests frontend)

- [X] T029 [P] [US3] Escribir test de servicio de consultas en `backend/src/test/java/footballmarket/services/QuoteQueryServiceTest.java`: historial ordenado, cotización vigente, cotización a una fecha dada, ausencia `QUOTE_NOT_FOUND` y ausencia de cotización en la respuesta (FR-036 a FR-040)
- [X] T030 [P] [US3] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/PlayerQuoteControllerTest.java` para `GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=` y `GET /api/players/{id}/quotes/current`, incluidos `404 QUOTE_NOT_FOUND` y el `calculatedAt` de la cotización y no el de la lectura (contracts/quotes-http.md §4, §5, §6)

### Implementation for User Story 3

- [X] T031 [US3] Crear la interfaz `QuoteQueryService` en `backend/src/main/java/footballmarket/services/QuoteQueryService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` con las consultas de historial, vigente y por fecha, con el desempate de FR-037 a igualdad de fecha de vigencia, resueltas por acceso directo al repositorio sin recalcular precios (FR-030, FR-036 a FR-038, FR-065)
- [X] T032 [US3] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteResponseDTO.java` con los campos de auditoría de `contracts/quotes-http.md` §8 y `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteHistoryResponseDTO.java` con `playerId` y `quotes` (FR-041)
- [X] T033 [US3] Completar `backend/src/main/java/footballmarket/controllers/mappers/QuoteMapper.java` con el mapeo de la cotización y de su historial (plan.md §Project Structure)
- [X] T034 [US3] Crear `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java` con `GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=` y `GET /api/players/{id}/quotes/current`, anotaciones OpenAPI y validación del identificador y de la fecha (FR-036 a FR-038, contracts/quotes-http.md §4, §5, §6)

**Checkpoint**: User Story 3 es funcional y verificable de forma independiente.

---

## Phase 6: User Story 4 - Obtener el ranking de jugadores por precio (Priority: P3)

**Goal**: Exponer el ranking de jugadores cotizables ordenados por su cotización vigente, de mayor a menor.

**Independent Test**: Catálogo cotizado con precios distintos: el ranking incluye una sola entrada por jugador cotizable, ordenada de mayor a menor, con el mismo precio que devuelve su cotización vigente.

### Tests para User Story 4 (backend según normas; sin tests frontend)

- [X] T035 [P] [US4] Escribir test de ranking en `backend/src/test/java/footballmarket/services/QuoteQueryServiceTest.java`: una sola entrada por jugador cotizable, orden descendente por precio, exclusión de jugadores sin métricas y presencia de los jugadores con precio `CARRIED_FORWARD` repetido (FR-039, SC-007)

### Implementation for User Story 4

- [X] T036 [US4] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java` la consulta de ranking: última fila por jugador con el desempate `effectiveDate DESC, calculatedAt DESC` (prevalece la fila `RECOMPUTED`) reordenada por precio descendente, sin recalcular precios (FR-039, FR-065)
- [X] T037 [US4] Añadir el método de ranking en `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` con su interfaz en `backend/src/main/java/footballmarket/services/QuoteQueryService.java` (FR-039)
- [X] T038 [US4] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/RankingResponseDTO.java` con `position`, `playerId`, `price`, `effectiveDate`, `origin` y `calculatedAt`, y exponer `GET /api/players/ranking` en `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java` con anotaciones OpenAPI (contracts/quotes-http.md §7)

**Checkpoint**: User Story 4 es funcional y verificable de forma independiente.

---

## Phase 7: User Story 5 - Operar el mercado con la cotización persistida (Priority: P2)

**Goal**: Que la compra y la publicación de venta consuman el repositorio canónico real de cotizaciones a través del puerto `QuoteSource`, sin fuente simulada, y que se rechacen de forma controlada cuando el mercado no esté inicializado.

**Independent Test**: Mercado inicializado y cotizado: la compra usa el precio vigente persistido y su instante de cálculo, la publicación congela ese precio, un recálculo posterior no altera saldos, tenencias ni transacciones, y una operación sobre un mercado no inicializado se rechaza con `MARKET_NOT_INITIALIZED`.

### Tests para User Story 5 (backend según normas; sin tests frontend)

- [X] T039 [P] [US5] Escribir test de la fuente real en `backend/src/test/java/footballmarket/integrations/RepositoryQuoteSourceTest.java`: devuelve la última fila del jugador, devuelve vacío si no hay cotización, conserva `calculatedAt` de la cotización y `strategyVersion` nulo en la fila inicial (contracts/quote-source.md §2)
- [X] T040 [P] [US5] Actualizar `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` y `backend/src/test/java/footballmarket/services/TokenListingServiceTest.java` con el rechazo `MARKET_NOT_INITIALIZED` antes de cualquier movimiento de recursos y con la comprobación de que un recálculo no altera saldos, tenencias ni transacciones (FR-043, FR-048)
- [X] T041 [P] [US5] Eliminar `backend/src/test/java/footballmarket/integrations/SimulatedQuoteSourceTest.java` y `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java`, y adaptar los tests de arranque y la configuración de test que dependan de la fuente simulada, verificado con la búsqueda `SimulatedQuoteSource|market.quote.simulator` en `backend/src` (quickstart §7) (FR-042, SC-011)

### Implementation for User Story 5

- [X] T042 [US5] Crear `backend/src/main/java/footballmarket/integrations/RepositoryQuoteSource.java` como implementación real del puerto `QuoteSource`, resuelta contra `PlayerQuoteRepository` con la última fila por `effective_date DESC, calculated_at DESC` (prevalece la fila `RECOMPUTED`) y sin cambiar la firma del puerto (FR-042, FR-058, contracts/quote-source.md §1, §2)
- [X] T043 [US5] Eliminar `backend/src/main/java/footballmarket/integrations/SimulatedQuoteSource.java`, `backend/src/main/java/footballmarket/config/QuoteSimulationConfiguration.java` y `backend/src/main/java/footballmarket/integrations/exceptions/MissingQuoteSourceException.java`, y retirar de `backend/src/main/resources/application.yml` y de `backend/src/test/resources/application-test.yml` las propiedades `market.quote.simulator.enabled` y `market.quote.simulator.price` (FR-042, SC-011, contracts/quote-source.md §3)
- [X] T044 [US5] Añadir en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` y `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java` la comprobación de mercado inicializado antes de leer la cotización y antes de cualquier movimiento de recursos, con rechazo `MARKET_NOT_INITIALIZED` (FR-043, FR-045)
- [X] T045 [US5] Corregir en `specs/010-buy-tokens/contracts/quote-source.md` la descripción de consumidores del puerto, que declara como único a `BuyOrderServiceImpl` cuando también lo consume `TokenListingServiceImpl`, en el mismo cambio que introduce la fuente real (research.md §10, Constitution §IV)

**Checkpoint**: User Story 5 es funcional y verificable de forma independiente.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia de contratos, documentación y control entregable.

- [X] T046 [P] Revisar y completar las anotaciones OpenAPI de `QuoteController` y `PlayerQuoteController` y de sus DTO en `backend/src/main/java/footballmarket/controllers/`, de modo que el contrato HTTP documentado y el OpenAPI coincidan con `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` (Constitution §IV)
- [X] T047 [P] Actualizar `docs/backend/architecture.md` §1.3 con la dependencia efectiva `Integration → Repository` introducida por `RepositoryQuoteSource` (research.md §9)
- [X] T048 [P] Revisar `docs/backend/technologies.md` y confirmar que no hay cambios en el inventario tecnológico: la feature no incorpora dependencias ni cambia versiones (Constitution §IV)
- [X] T049 Revisar en `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java` y `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` que la ejecución y las consultas usen los mecanismos de observabilidad vigentes (registros estructurados, correlación de solicitudes, métricas y health checks) sin reimplementarlos, y que el fallo individual quede registrado con jugador, etapa y causa (FR-062, FR-064)
- [X] T050 Ejecutar el control entregable permitido `./gradlew build -x test` desde `backend/` y reportar el resultado al usuario; los tests automatizados los ejecuta el usuario (Constitution, «Verificación y testing»)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories; consumes la inicialización solo como precondición de datos
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 5 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories; requiere filas `INITIAL` existentes solo como precondición de datos

### Within Each User Story

- Escribir tests backend necesarios; su ejecución y evidencia corresponden al usuario
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 2

```bash
# Escribir tests backend de US2; no ejecutarlos desde el agente:
Task: "Escribir test de servicio de recálculo en backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java"
Task: "Escribir test de contrato HTTP en backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java"
Task: "Escribir test de persistencia en backend/src/test/java/footballmarket/repositories/QuoteExecutionPersistenceTest.java"

# Launch all foundations for User Story 2 together:
Task: "Crear backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java"
Task: "Crear backend/src/main/java/footballmarket/controllers/dtos/requests/RecalculateQuotesRequestDTO.java"
Task: "Crear backend/src/main/java/footballmarket/controllers/mappers/QuoteExecutionMapper.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. US4 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
6. US5 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
7. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
   - Developer D: User Story 4
   - Developer E: User Story 5
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

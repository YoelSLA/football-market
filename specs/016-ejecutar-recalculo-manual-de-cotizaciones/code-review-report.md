# SPEC-016 — Code Review Report

**Fecha de la revisión:** 2026-10-21
**Rama revisada:** `feat/016-ejecutar-recalculo-manual-de-cotizaciones`
**Base del diff:** `origin/dev...HEAD` (commits `0b5dba2` implementación y `9e8d0a1` merge de `origin/dev`)
**Alcance revisado:** T001–T050 de `tasks.md` (US1–US5 + Polish), 92 archivos, +7.406 / −869 líneas
**Estado de este informe:** entregado para revisión humana. **El agente no aprueba CODE REVIEW.**

---

## 1. Resumen y funcionamiento

La feature reemplaza la fuente de cotizaciones simulada por una fuente real persistida y agrega un
recálculo manual trazable de precios de tokens de jugadores.

### 1.1 Modelo de datos

Dos tablas nuevas (`backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql`):

- `player_quotes`: una fila por **(jugador, fecha de vigencia, origen)**. Origenes cerrados
  `INITIAL | RECOMPUTED | CARRIED_FORWARD`. Restricción única `uq_player_quote
  (player_id, effective_date, origin)` y cinco `CHECK` que blindan las invariantes por origen
  (precio positivo, `previous_price` nulo solo en la fila inicial, estrategia y versión presentes
  en fila repreciada, `score` nulo en fila arrastrada).
- `quote_executions`: una fila por corrida, con `quote_date` único (`uq_quote_executions_date`),
  la estrategia y versión aplicadas, el factor, el tope y el piso usados, y los cinco contadores
  (`priced_players`, `recomputed_rows`, `carried_forward_rows`, `not_repriced_players`,
  `failed_players`).

### 1.2 Regla de cálculo

`backend/src/main/java/footballmarket/models/QuoteAdjustment.java`:

```
precio = vigente × (1 + k × (score − 0,5))
multiplicador = clamp(1 + k × (score − 0,5), 1 − variationCap, 1 + variationCap)
precio = max(precio, minPriceFloor)
multiplicador persistido redondeado a 6 decimales (HALF_UP)
```

El score es el persistido por SPEC-013: `final_score` para `POSITIONAL_PERFORMANCE` y `final_grl`
para `GENERAL_ATTRIBUTES`. No se recalcula ninguna valuación ni se llama a proveedores externos.

### 1.3 Ciclo de vida de la corrida (`QuoteRecalculationServiceImpl`)

1. Valida en el orden exacto de FR-066: estrategia → mercado inicializado → factor → tope → piso →
   ejecución ya existente.
2. Inserta la fila de `quote_executions` como reserva (contadores en cero) para apropiarse de la
   fecha; la unicidad `uq_quote_executions_date` materializa FR-060 y produce `409
   EXECUTION_ALREADY_EXISTS` ante una segunda corrida del día.
3. Por cada jugador cotizable (`QuotePopulationSelector`), `QuotePlayerPublisher` resuelve la fila
   vigente y la fecha de vigencia de referencia, decide entre reprecio, arrastre o no reprecio, y
   escribe la fila en su propia transacción (`REQUIRES_NEW`).
4. Actualiza el resumen con los contadores finales.

Precedencia de decisiones por jugador:

| Caso | Origen | Efecto |
| --- | --- | --- |
| Valuación posterior a la fecha de referencia | `RECOMPUTED` | Precio nuevo, `previous_price` = precio anterior |
| Sin valuación posterior | `CARRIED_FORWARD` | Precio repetido, multiplicador 1, estrategia heredada |
| Score neutro (0,5) | `RECOMPUTED` | Multiplicador 1, sin salto de precio |

### 1.4 Inicialización del mercado (`QuoteMarketInitializationServiceImpl`)

Una sola vez por fecha de mercado: escribe una fila `INITIAL` de 100 centésimas por jugador cotizable
y es idempotente en la misma fecha (`alreadyInitialized: true`). Cualquier otra fecha de mercado
produce `409 MARKET_ALREADY_INITIALIZED`. El estado del mercado **no** se duplica en una tabla
paralela: se deriva de la existencia de filas `INITIAL` (FR-044).

### 1.5 Puertos HTTP (`contracts/quotes-http.md`)

| Verbo y ruta | Propósito |
| --- | --- |
| `POST /api/quotes/initialize` | Inicializa el mercado (body opcional) |
| `POST /api/quotes/recalculate` | Ejecuta el recálculo del día |
| `GET /api/quotes/executions?date=` | Resumen de corridas por fecha |
| `GET /api/players/{id}/quotes` | Historial de cotizaciones |
| `GET /api/players/{id}/quotes?date=` | Cotización vigente en una fecha |
| `GET /api/players/{id}/quotes/current` | Cotización vigente |
| `GET /api/players/ranking` | Ranking por precio |

Todas las rutas exigen JWT válido (`anyRequest().authenticated()`), sin roles nuevos (FR-059).

### 1.6 Integración con el mercado

`RepositoryQuoteSource` implementa el puerto `QuoteSource` contra `PlayerQuoteRepository`: devuelve
la fila vigente por `effective_date DESC` con prevalencia de `RECOMPUTED`, conserva el
`calculated_at` persistido como instante de cálculo y mantiene la `strategy_version` nula de la fila
inicial. Se retiraron `SimulatedQuoteSource`, `QuoteSimulationConfiguration` y
`MissingQuoteSourceException` junto con su registro de auto-configuración y sus propiedades
`market.quote.simulator.*`.

`MarketInitializationGuard` bloquea compra y publicación de venta con `409
MARKET_NOT_INITIALIZED` antes de leer la cotización y antes de mover recursos, conservando intacto
`QUOTE_UNAVAILABLE` para el mercado inicializado sin cotización del jugador.

---

## 2. Verificación ejecutada por el agente

| Control | Resultado |
| --- | --- |
| `./gradlew build -x test` (backend/) | **BUILD SUCCESSFUL** — compilación productiva, Spotless y `validateTestClassification` ("Clasificación válida: 121 clases de test") |
| Tests de aplicación | **No ejecutados.** La Constitución y `backend/AGENTS.md` reservan la ejecución de tests al usuario. |
| Revisión estática del diff y de los artefactos de la spec | Realizada (este informe) |

El build acredita compilación, formato y clasificación de tests; **no acredita ejecución de tests**
ni aprobación de TESTING.

---

## 3. Archivos creados y modificados

Responsabilidad y razón de cada cambio. `A` = creado, `M` = modificado, `D` = eliminado.

### 3.1 Configuración

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../config/QuoteProperties.java` (A) | Backend | `@ConfigurationProperties("market.quote")` con factor 0,10, tope 0,10, piso 1 y ancla 100 | FR-011 a FR-015: parámetros configurables sin declaración explícita |
| `backend/.../config/QuoteConfig.java` (A) | Backend | Registro de `QuoteProperties` | T003 |
| `backend/.../config/QuoteSimulationConfiguration.java` (D) | Backend | Retirada del simulador | FR-042 / T043 |
| `backend/src/main/resources/application.yml` (M) | Backend | Bloque `market.quote.*`; retirada de `market.quote.simulator.*` | FR-015 / FR-042 |
| `backend/src/main/resources/META-INF/spring/...AutoConfiguration.imports` (M) | Backend | Retirada del registro del simulador | FR-042 |

### 3.2 Modelo y persistencia

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../models/PlayerQuote.java` (A) | Backend | Entidad de cotización con invariantes por origen | FR-021 a FR-028, research.md §2 |
| `backend/.../models/QuoteExecution.java` (A) | Backend | Resumen de corrida con parámetros aplicados y contadores | FR-029, FR-061 |
| `backend/.../models/QuoteAdjustment.java` (A) | Backend | Fórmula, tope, piso y redondeos | FR-004 a FR-016 |
| `backend/.../models/enums/QuoteOrigin.java` (A) | Backend | Catálogo cerrado de orígenes | FR-021 |
| `backend/.../models/exceptions/InvalidQuoteException.java` (A) | Backend | Error de invariantes del modelo | research.md §15 |
| `backend/.../models/records/MarketInitializationResult.java` (A) | Backend | Resultado de la inicialización | Contrato §1 |
| `backend/.../models/records/PlayerRankingEntry.java` (A) | Backend | Entrada del ranking | Contrato §7 |
| `backend/.../repositories/PlayerQuoteRepository.java` (A) | Backend | Última fila, historial, fila por fecha, existencia de `INITIAL`, ranking | FR-036 a FR-040, FR-044 |
| `backend/.../repositories/QuoteExecutionRepository.java` (A) | Backend | Persistencia y consulta de corridas | FR-061 |
| `backend/.../repositories/PlayerStatisticsRepository.java` (M) | Backend | Consulta de población cotizable | FR-001 a FR-003 |
| `backend/.../repositories/PositionalValuationRepository.java` (M) | Backend | Lectura de `final_score` más reciente | FR-006, FR-010 |
| `backend/.../repositories/AttributeValuationRepository.java` (M) | Backend | Lectura de `final_grl` más reciente | FR-006, FR-010 |
| `backend/.../db/migration/V20261021042325__create_player_quotes.sql` (A) | Backend | Esquema de `player_quotes` y `quote_executions` | data-model.md §1, §2, §5 |

### 3.3 Servicios

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../services/QuoteRecalculationService.java` (A) | Backend | Interfaz de la corrida | T004–T013 |
| `backend/.../services/QuoteMarketInitializationService.java` (A) | Backend | Interfaz de inicialización | US1 |
| `backend/.../services/QuoteQueryService.java` (A) | Backend | Interfaz de consultas y ranking | US3, US4 |
| `backend/.../services/impl/QuoteRecalculationServiceImpl.java` (A) | Backend | Corrida, precedencia de rechazos, contadores | US2, FR-060 a FR-066 |
| `backend/.../services/impl/QuoteMarketInitializationServiceImpl.java` (A) | Backend | Idempotencia y fila inicial | US1, FR-031 a FR-035 |
| `backend/.../services/impl/QuoteQueryServiceImpl.java` (A) | Backend | Historial, vigente por fecha, ranking | US3, US4 |
| `backend/.../services/internal/QuotePlayerPublisher.java` (A) | Backend | Decisión por jugador y escritura atómica | FR-004 a FR-010, FR-062 |
| `backend/.../services/internal/QuotePopulationSelector.java` (A) | Backend | Población cotizable | FR-001 a FR-003 |
| `backend/.../services/internal/QuoteScoreResolver.java` (A) | Backend | Resolución de score por estrategia | FR-006, FR-010, FR-055 |
| `backend/.../services/internal/MarketInitializationGuard.java` (A) | Backend | Guardia de mercado inicializado | FR-043 |
| `backend/.../services/impl/BuyOrderServiceImpl.java` (M) | Backend | Guardia antes de leer cotización y antes de mover recursos | FR-043 |
| `backend/.../services/impl/TokenListingServiceImpl.java` (M) | Backend | Guardia antes de publicar | FR-043 |

### 3.4 Capa HTTP

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../controllers/QuoteController.java` (A) | Backend | Inicialización, recálculo y consulta de ejecuciones | Contrato §1–§3 |
| `backend/.../controllers/PlayerQuoteController.java` (A) | Backend | Historial, vigente y ranking | Contrato §4–§7 |
| `backend/.../controllers/mappers/QuoteMapper.java` (A) | Backend | Mapeo de cotización y ranking | FR-041 |
| `backend/.../controllers/mappers/QuoteExecutionMapper.java` (A) | Backend | Mapeo del resumen de corrida | FR-061 |
| `backend/.../controllers/dtos/requests/InitializeQuotesRequestDTO.java` (A) | Backend | Body opcional de inicialización | Contrato §1 |
| `backend/.../controllers/dtos/requests/RecalculateQuotesRequestDTO.java` (A) | Backend | Body de recálculo | Contrato §2 |
| `backend/.../controllers/dtos/responses/QuoteResponseDTO.java` (A) | Backend | Objeto de cotización (10 campos) | Contrato §8, FR-041 |
| `backend/.../controllers/dtos/responses/QuoteExecutionResponseDTO.java` (A) | Backend | Resumen de corrida (13 campos) | Contrato §2 |
| `backend/.../controllers/dtos/responses/QuoteExecutionsResponseDTO.java` (A) | Backend | Envoltorio de lista de ejecuciones | Contrato §3 |
| `backend/.../controllers/dtos/responses/QuoteHistoryResponseDTO.java` (A) | Backend | Envoltorio de historial | Contrato §4 |
| `backend/.../controllers/dtos/responses/RankingResponseDTO.java` (A) | Backend | Entrada de ranking | Contrato §7 |
| `backend/.../controllers/dtos/responses/PlayerRankingResponseDTO.java` (A) | Backend | Envoltorio de ranking | Contrato §7 |
| `backend/.../controllers/dtos/responses/InitializeMarketResponseDTO.java` (A) | Backend | Respuesta de inicialización | Contrato §1 |
| `backend/.../controllers/exceptions/GlobalExceptionHandler.java` (M) | Backend | Ocho manejadores nuevos | FR-043, FR-045, research.md §15 |

### 3.5 Integración

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../integrations/RepositoryQuoteSource.java` (A) | Backend | Implementación real del puerto `QuoteSource` | FR-042, FR-058 |
| `backend/.../integrations/SimulatedQuoteSource.java` (D) | Backend | Retirada del simulador | FR-042 |
| `backend/.../integrations/exceptions/MissingQuoteSourceException.java` (D) | Backend | Retirada del simulador | FR-042 |

### 3.6 Excepciones de servicio

`backend/.../services/exceptions/`: `MarketNotInitializedException`,
`MarketAlreadyInitializedException`, `InvalidAdjustmentFactorException`,
`InvalidVariationCapException`, `InvalidMinPriceFloorException`, `UnknownQuoteStrategyException`,
`QuoteNotFoundException`, `ExecutionAlreadyExistsException` (todas `A`) — research.md §15.

### 3.7 Tests

| Archivo | Resp. | Categoría / tag | Cambio | Razón |
| --- | --- | --- | --- | --- |
| `backend/src/test/.../services/QuoteMarketInitializationServiceTest.java` (A) | Backend | Service / `integration` | Inicialización, idempotencia, rechazo | T014 |
| `backend/src/test/.../services/QuoteRecalculationServiceTest.java` (A) | Backend | Service / `integration` | Recálculo, reprecio, arrastre, rechazos, concurrencia | T018–T021 |
| `backend/src/test/.../services/QuoteQueryServiceTest.java` (A) | Backend | Service / `integration` | Historial, vigente, ranking | T024–T027 |
| `backend/src/test/.../controllers/QuoteControllerTest.java` (A) | Backend | Controller / `lightweight` | Contrato HTTP de mercado y ejecuciones | T015 |
| `backend/src/test/.../controllers/PlayerQuoteControllerTest.java` (A) | Backend | Controller / `lightweight` | Contrato HTTP de cotización y ranking | T028–T030 |
| `backend/src/test/.../repositories/PlayerQuoteMigrationTest.java` (A) | Backend | Repository / `integration` | Unicidad y `CHECK` del esquema | T016 |
| `backend/src/test/.../repositories/QuoteExecutionPersistenceTest.java` (A) | Backend | Repository / `integration` | Persistencia y unicidad de corridas | T022 |
| `backend/src/test/.../repositories/QuoteMarketOperationPersistenceTest.java` (A) | Backend | Repository / `integration` | Compra con precio persistido; recálculo no altera saldos | T040 |
| `backend/src/test/.../integrations/RepositoryQuoteSourceTest.java` (A) | Backend | Integration / `integration` | Adaptador real del puerto | T039 |
| `backend/src/test/.../models/QuoteAdjustmentTest.java` (A) | Backend | Model / `unit` | Fórmula, tope, piso | T017 |
| `backend/src/test/.../support/QuoteTestData.java` (A) | Backend | Fixture | Datos de cotización | T032 |
| `backend/src/test/.../support/MarketTestData.java` (M) | Backend | Fixture | `initializeMarket` / `clearMarketInitialization` | T044 |
| `backend/src/test/.../services/BuyOrderServiceTest.java` (M) | Backend | Service | Rechazo con mercado no inicializado | T044 |
| `backend/src/test/.../services/TokenListingServiceTest.java` (M) | Backend | Service | Rechazo con mercado no inicializado | T044 |
| `backend/src/test/.../repositories/ListingRepositorySupport.java` (M) | Backend | Repository | Precondición de mercado inicializado | T044 |
| `backend/src/test/.../repositories/MarketDomainPersistenceTest.java` (M) | Backend | Repository | Ídem | T044 |
| `backend/src/test/.../repositories/PlayerTokenEmissionPersistenceTest.java` (M) | Backend | Repository | Ídem | T044 |
| `backend/src/test/.../repositories/SuperuserMarketRecoveryPersistenceTest.java` (M) | Backend | Repository | Ídem | T044 |
| `backend/src/test/.../security/BuyOrderAuthorizationTest.java` (M) | Backend | Security | Ídem | T044 |
| `backend/src/test/resources/application-test.yml` (M) | Backend | Config | Retirada de `market.quote.simulator.*` | T043 |
| `backend/src/test/.../config/QuoteSimulationConfigurationTest.java` (D) | Backend | — | Retirada con el simulador | T043 |
| `backend/src/test/.../integrations/SimulatedQuoteSourceTest.java` (D) | Backend | — | Retirada con el simulador | T043 |

### 3.8 Documentación

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `specs/016-.../spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, `tasks.md` (A) | Spec | Artefactos de diseño de la feature | Ciclo de vida SDD |
| `specs/016-.../contracts/quotes-http.md`, `contracts/quote-source.md` (A) | Spec | Contratos normativos | FR-041 a FR-059 |
| `specs/016-.../checklists/cotizacion.md`, `checklists/requirements.md` (A) | Spec | Checklists del revisor | COMPLEMENT |
| `specs/016-.../sdd-gates.md` (A) | Spec | Registro local de gates | Contingencia de §12 del workflow |
| `specs/010-buy-tokens/contracts/quote-source.md` (M) | Spec | Consumidores del puerto `QuoteSource` | T045 |
| `specs/assignments.md` (M) | Spec | Asignación de la feature | Trazabilidad |
| `docs/backend/architecture.md` (M) | Backend | Dependencia efectiva `Integration -> Repository` | T048 |
| `docs/TECHNICAL_DEBT.md` (M) | Transversal | TD-021 y entradas de esta revisión | Registro de deuda |

---

## 4. Decisiones de diseño tomadas

1. **Estado del mercado derivado, no duplicado.** No hay tabla de estado de mercado: "inicializado"
   es la existencia de una fila `INITIAL` (FR-044). Evita dos fuentes de verdad.
2. **Una sola tabla de cotizaciones.** El historial completo vive en `player_quotes`; no hay tabla de
   "cotización actual" (FR-022).
3. **El catálogo de estrategias se reutiliza.** `ValuationStrategy` de SPEC-013
   (`POSITIONAL_PERFORMANCE`, `GENERAL_ATTRIBUTES`) es el catálogo cerrado y versionado; no se creó un
   enum nuevo (FR-018, FR-055).
4. **Los parámetros aplicados se persisten en la corrida, no en la fila.** La fila guarda el
   multiplicador, el score y la estrategia; el factor, el tope y el piso viven en `quote_executions`
   (FR-029, SC-013).
5. **Atomicidad por jugador con `REQUIRES_NEW`.** Una falla individual no revierte la corrida
   completa; el detalle del fallo queda en el log y solo el contador se expone (FR-062, FR-064).
6. **La fila arrastrada hereda la estrategia vigente** y repite el precio con multiplicador 1
   (FR-019, FR-021).
7. **Prevalencia de `RECOMPUTED` sobre `CARRIED_FORWARD`** a igualdad de fecha de vigencia, de forma
   consistente en las tres consultas de lectura (FR-026, FR-037).
8. **Retiro del simulador en lugar de coexistencia.** Una sola implementación del puerto
   `QuoteSource` (FR-042, SC-011).

---

## 5. Hallazgos de la revisión

Severidad: **Alta** = afecta corrección de datos, contrato o disponibilidad; **Media** = degradación
consistente o riesgo de regresión; **Baja** = mantenibilidad y limpieza.

| ID | Severidad | Hallazgo | Evidencia | Recomendación |
| --- | --- | --- | --- | --- |
| CR-01 | Media | El ranking **no tiene desempate total**: ante empate de fecha de vigencia e instante de cálculo entre dos filas del mismo jugador, ambas entran y el jugador aparece dos veces. La "última fila por jugador" tiene el mismo hueco. **Verificado en la base de desarrollo: el ranking actual devuelve 1.860 filas para 1.860 jugadores distintos, sin duplicados**; el defecto es latente y solo se alcanza con dos filas de la misma fecha y el mismo instante (`INITIAL` y `CARRIED_FORWARD`). | `backend/.../repositories/PlayerQuoteRepository.java:26-31`, `:72-88` | Añadir desempate final por `id` en los tres `ORDER BY` y en la condición del `not exists`. Registrado como TD-022. |
| CR-02 | Alta | Un **jugador cotizable sin fila `INITIAL`** (métricas obtenidas después de inicializar el mercado) provoca `NoSuchElementException` en `getFirst()`: se contabiliza como fallo permanente, nunca recibe precio y no aparece en el ranking, pese a ser cotizable (FR-001, FR-002, FR-034). | `backend/.../services/internal/QuotePlayerPublisher.java:44,47` | Tratar la ausencia de fila inicial como jugador no operable y no como fallo; o habilitar la idempotencia de la inicialización para completar filas `INITIAL` faltantes. Registrado como TD-023. |
| CR-03 | Media | **Filas confirmadas sin resumen de corrida.** Las publicaciones por jugador se confirman en su propia transacción; si la transacción global revierte al final (fallo del `save` del resumen, timeout), quedan cotizaciones movidas sin `quote_executions` y la fecha queda libre para una segunda corrida (FR-061, SC-013). | `backend/.../services/internal/QuotePlayerPublisher.java:34`; `backend/.../services/impl/QuoteRecalculationServiceImpl.java:32` | Alinear el compromiso: o todo en una transacción, o un estado de corrida que distinga "en curso" de "terminada". Registrado como TD-024. |
| CR-04 | Media | **`quote_executions` no es append-only ni tiene estado.** La fila se inserta como reserva con contadores en cero y `finished_at = started_at`, y se actualiza al final; un lector concurrente puede ver una ejecución aparentemente terminada y vacía. Contradice la calificación "append-only" de `data-model.md §2`. | `backend/.../services/impl/QuoteRecalculationServiceImpl.java:95-108,133-139`; `backend/.../models/QuoteExecution.java:157-176` | Añadir columna de estado o reservar la fila al final de forma transaccional. Registrado como TD-025. |
| CR-05 | Media | **Inicialización concurrente responde 500.** Dos `POST /api/quotes/initialize` simultáneos con fechas distintas: el perdedor recibe `DataIntegrityViolationException` → `500 INTERNAL_ERROR` en lugar de `409 MARKET_ALREADY_INITIALIZED` (contrato §1). | `backend/.../services/impl/QuoteMarketInitializationServiceImpl.java:48-57`; `backend/.../controllers/exceptions/GlobalExceptionHandler.java:185-197` | Traducir la violación de integridad al mismo código controlado del camino secuencial. Registrado como TD-026. |
| CR-06 | Media | **Sin índice sobre `origin`.** `existsByOrigin` se ejecuta en cada compra y cada publicación de venta; sin índice, es un recorrido secuencial de `player_quotes` por operación de mercado. | `backend/.../repositories/PlayerQuoteRepository.java:123`; `backend/.../services/impl/BuyOrderServiceImpl.java:95,200`; `backend/.../services/impl/TokenListingServiceImpl.java:66` | Índice parcial o compuesto que cubra `origin` en el camino caliente. Registrado como TD-027. |
| CR-07 | Media | **Operaciones de mercado sin control de rol.** Cualquier usuario autenticado puede fijar la fecha y el ancla del mercado y disparar un recálculo global que consume la unicidad diaria. Es conforme al contrato (FR-059 no introduce roles), pero es una superficie de privilegios amplia. | `backend/.../config/SecurityConfig.java:112-113`; `backend/.../controllers/QuoteController.java:50,84` | Decisión del responsable: rol administrativo o registro explícito del riesgo. Registrado como TD-028. |
| CR-08 | Media | **`InvalidQuoteException` sin manejador global.** Si escapara al controlador, el 500 no usaría `ErrorResponseDTO` ni el código `INTERNAL_ERROR` del contrato. Hoy está contenida dentro de la corrida. | `backend/.../models/exceptions/InvalidQuoteException.java`; `backend/.../controllers/exceptions/GlobalExceptionHandler.java` | Actualizado dentro de TD-011 (misma familia de deuda, sin duplicar). |
| CR-09 | Media | **`marketDate` futuro no se valida.** Inicializar con fecha `D > hoy` deja la fecha de vigencia de referencia en el futuro y ninguna valuación posterior la supera: toda corrida futura caería en "no reprecio" y el precio no se movería nunca. | `backend/.../services/impl/QuoteMarketInitializationServiceImpl.java:48` | Validar que la fecha de mercado no sea futura, o documentar la regla operativa. |
| CR-10 | Media | **Cobertura de tests incompleta.** Sin cobertura: el **fallo individual de un jugador** (FR-062, el requisito más sensible), la rama `GENERAL_ATTRIBUTES` (FR-006, FR-019, FR-009), el puerto por lotes `currentQuotes(Set)` (ruta del portfolio, que perdió el test del simulador), el redondeo `HALF_UP` y la escala de 6 decimales (FR-016), y la ausencia de efectos tras cada rechazo (FR-060, FR-063). | `backend/src/test/.../services/QuoteRecalculationServiceTest.java`; `backend/src/test/.../models/QuoteAdjustmentTest.java`; `backend/src/test/.../integrations/RepositoryQuoteSourceTest.java` | Ampliar la suite en la fase de TESTING. Registrado como TD-029. |
| CR-11 | Baja | "Última fila por jugador" **carga el historial completo** y duplica la query de historial; con historial largo, cada jugador arrastra todas sus filas y el llamador toma la primera. | `backend/.../repositories/PlayerQuoteRepository.java:26-31` vs `:39-44` | Acotar con `LIMIT 1` en una consulta propia. |
| CR-12 | Baja | **N+1 de entidades lazy** al leer `quote.getPlayer().getId()` en las lecturas masivas (ranking y fuente por lotes). | `backend/.../services/impl/QuoteQueryServiceImpl.java:74-83`; `backend/.../integrations/RepositoryQuoteSource.java:58-60` | Proyección del identificador del jugador. |
| CR-13 | Baja | **Rangos duplicados** en 4–5 sitios (factor, tope) y **versión de estrategia cableada** a `"v1"` en tres puntos. | `backend/.../services/impl/QuoteRecalculationServiceImpl.java:85,94,127`; `backend/.../models/QuoteExecution.java:121,125`; `backend/.../config/QuoteProperties.java:21-22` | Constantes únicas en `QuoteProperties`. |
| CR-14 | Baja | **Doble fuente de verdad para los defaults:** `@NotNull` es inoperante (el constructor compacto ya sustituye nulos) y los defaults existen en Java y en `application.yml`. Además, un factor de configuración fuera de rango aborta el arranque mientras que el mismo valor por HTTP produce un error controlado. | `backend/.../config/QuoteProperties.java:21-25,28-45`; `backend/src/main/resources/application.yml:97-100` | Consolidar defaults y documentar el régimen de validación de la configuración. |
| CR-15 | Baja | **Precedencia no definida** en `buy()`: cantidad inválida o jugador inexistente se resuelven antes que `MARKET_NOT_INITIALIZED`; la guarda de mercado se evalúa dos veces en el flujo de compra. | `backend/.../services/impl/BuyOrderServiceImpl.java:95,188,193,200` | Unificar la evaluación y documentar el orden. |
| CR-16 | Baja | **Frágilidad de enrutado por URI literal** para el 400 de parámetro ausente/malformado y posible colisión futura entre `GET /api/players/ranking` y `GET /api/players/{id}`. | `backend/.../controllers/exceptions/GlobalExceptionHandler.java:96-99,411-417`; `backend/.../controllers/PlayerQuoteController.java:123` | Sustituir por validación declarativa. |
| CR-17 | Baja | **Documentación residual de SPEC-010:** `specs/010-buy-tokens/contracts/quote-source.md` sigue describiendo el simulador como existente ("Ciclo de vida", "Selección de la fuente", "Simulador temporal"). | `specs/010-buy-tokens/contracts/quote-source.md` | Corrección documental mecánica, pendiente de autorización. |
| CR-18 | Baja | **Residuo documental:** el `previous_price` de una fila arrastrada documenta el precio anterior a su propio reprecio, no al reprecio posterior (FR-026); el `CHECK` no ata `previous_price` al `price` en `CARRIED_FORWARD`. | `backend/.../models/PlayerQuote.java:161-169`; `backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql:21` | Precisar la semántica en el modelo y en `data-model.md`. |

### 5.1 Verificaciones sin hallazgo

- Migración `V20261021042325` **coincide línea a línea** con `data-model.md §1, §2, §5`: columnas, tipos,
  secuencias, `uq_player_quote`, los cinco `CHECK` de `player_quotes`, `uq_quote_executions_date`, los
  cuatro `CHECK` de `quote_executions` e índices.
- La capa HTTP **cumple el contrato**: las siete rutas, verbos, códigos de estado, nombres y tipos de
  los campos JSON, envoltorios de lista, formatos de fecha y número (centésimas sin conversiones) y los
  ocho códigos de error nuevos, sin colisiones con los manejadores preexistentes
  (`QUOTE_UNAVAILABLE` y `INVALID_QUOTE_PRICE` intactos).
- La **precedencia de rechazos** de FR-066 se respeta exactamente: estrategia → mercado → factor →
  tope → piso → ejecución existente.
- Las **invariantes del modelo** coinciden con los `CHECK` de la base, y el mapeo JPA es correcto
  (`Instant`↔`TIMESTAMPTZ`, `LocalDate`↔`DATE`, enum como `VARCHAR`, `LAZY` sin cascada indebida).
- El reloj de mercado es **UTC** (`MarketTimeConfiguration.marketClock()`), conforme a FR-060.
- No hay **exposición de datos sensibles** ni del detalle de fallos individuales por HTTP (FR-062).
- La **fórmula, el tope y el piso** se aplican correctamente y el precio es positivo en tres capas
  (ajuste, entidad y `CHECK`).

---

## 6. Cobertura de requisitos

| Grupo | FR | Estado de la revisión |
| --- | --- | --- |
| Población cotizable | FR-001 a FR-003 | Cubierto, con la salvedad de CR-02 (jugadores cotizables sin fila inicial) |
| Fórmula y variación | FR-004 a FR-010, FR-016 | Cubierto; FR-004 con la salvedad de CR-02 y de TD-021 |
| Parámetros y configuración | FR-011 a FR-015 | Cubierto; FR-011/FR-012 con la salvedad de CR-14 |
| Estrategia del operador | FR-017 a FR-020 | Cubierto |
| Origen cerrado y trazabilidad | FR-021 a FR-030 | Cubierto; FR-029 con la salvedad de CR-13 (redondeo a 6 decimales en BD) |
| Inicialización | FR-031 a FR-035 | Cubierto, con la salvedad de CR-09 |
| Consultas y ranking | FR-036 a FR-040 | Cubierto; FR-039 verificado en la base de desarrollo (1.860 filas para 1.860 jugadores, sin duplicados), con el defecto latente de CR-01 |
| Auditoría y fuente | FR-041 a FR-046 | Cubierto; FR-042 verificado por búsqueda de residuos |
| Mercado y autorización | FR-047 a FR-059 | Cubierto; sin control de rol por diseño (FR-059), con CR-07 |
| Corrida | FR-060 a FR-066 | Cubierto; FR-060/FR-061 con las salvedades CR-03, CR-04 y CR-05 |

---

## 7. Revisión de la suite de tests

Cobertura correcta: inicialización e idempotencia, población cotizable, recálculo con reprecio y
arrastre, no reprecio por valuación no posterior, coexistencia `RECOMPUTED`/`CARRIED_FORWARD` de la
misma fecha, prevalencia de `RECOMPUTED` en consultas, historial, vigente por fecha, errores de
dominio, contrato HTTP de los siete endpoints, unicidad y `CHECK` del esquema, unicidad de la corrida
y concurrencia de la misma fecha, compra con precio persistido y no alteración de saldos por el
recálculo.

Deficiencias (detalle en CR-10): ausencia de cobertura del fallo individual por jugador; rama
`GENERAL_ATTRIBUTES` sin ejercitar; puerto por lotes `currentQuotes(Set)` sin cubrir tras la
retirada del simulador; redondeo `HALF_UP` y escala de 6 decimales no discriminantes; ausencia de
asertos sobre `previousPrice`, `effectiveDate`, `multiplier` y `origin` de la fila arrastrada; y
aserciones de "no escribe filas" ausentes en cada rechazo. La suite nueva no tiene evidencia de
ejecución: su ejecución y la de la regresión quedan a cargo del usuario.

Clasificación: los diez archivos nuevos declaran un único tag por clase y usan `@Nested`, conforme a
`docs/backend/testing.md §16`. `validateTestClassification` informa "Clasificación válida: 121 clases
de test" dentro del build permitido.

---

## 8. Deuda técnica registrada en esta revisión

| ID | Título | Prioridad |
| --- | --- | --- |
| TD-022 | Ranking de cotizaciones sin desempate determinista: un jugador puede aparecer dos veces | Media |
| TD-023 | Jugador cotizable sin fila `INITIAL` falla en cada corrida de recálculo | Media |
| TD-024 | Cotizaciones confirmadas sin resumen de ejecución persistido | Media |
| TD-025 | `quote_executions` sin estado: una ejecución en curso es indistinguible de una terminada | Media |
| TD-026 | Inicialización concurrente del mercado responde 500 en lugar de 409 | Media |
| TD-027 | Índice ausente sobre `player_quotes.origin` en el camino caliente de compra y publicación | Media |
| TD-028 | Operaciones de cotización del mercado sin control de rol | Media |
| TD-029 | Cobertura de tests incompleta para el recálculo de cotizaciones | Media |

TD-011 se actualizó con `InvalidQuoteException` entre las excepciones propias sin manejador global.
TD-021 ya existente conserva su estado: el comportamiento de la campaña reutilizada fue confirmado
como decidido por el responsable del proyecto.

---

## 9. Orden de revisión sugerido

1. **Fórmula y modelo** — `models/QuoteAdjustment.java`, `models/PlayerQuote.java`,
   `models/QuoteExecution.java`, `models/enums/QuoteOrigin.java`.
2. **Esquema** — `V20261021042325__create_player_quotes.sql` contra `data-model.md §1, §2, §5`.
3. **Consultas** — `repositories/PlayerQuoteRepository.java` (CR-01, CR-11), `QuoteExecutionRepository`.
4. **Corrida** — `services/impl/QuoteRecalculationServiceImpl.java`,
   `services/internal/QuotePlayerPublisher.java` (CR-02, CR-03), `QuoteScoreResolver`,
   `QuotePopulationSelector`.
5. **Inicialización y guardas** — `QuoteMarketInitializationServiceImpl` (CR-05, CR-09),
   `MarketInitializationGuard`, `BuyOrderServiceImpl`, `TokenListingServiceImpl`.
6. **HTTP** — `QuoteController`, `PlayerQuoteController`, mappers, DTOs, `GlobalExceptionHandler`.
7. **Fuente real** — `integrations/RepositoryQuoteSource.java` y la retirada del simulador.
8. **Tests** — los diez archivos nuevos y las modificaciones de fixtures.
9. **Documentación** — contratos de SPEC-016, `specs/010-buy-tokens/contracts/quote-source.md`
   (CR-17), `docs/backend/architecture.md`, `docs/TECHNICAL_DEBT.md`.

---

## 10. Checklist humano de verificación

El usuario revisa y marca cada ítem. **El agente no aprueba este checklist.**

### A. Corrección funcional

- [ ] A1. La fórmula, el tope de variación y el piso mínimo se aplican según FR-004 a FR-016.
- [ ] A2. Un score de 0,5 produce `RECOMPUTED` con multiplicador 1 (no arrastre).
- [ ] A3. Un jugador sin valuación posterior a su fecha de referencia conserva el precio y se informa
      en `notRepricedPlayers`.
- [ ] A4. Una fila arrastrada hereda la estrategia de la cotización vigente y repite el precio.
- [ ] A5. La inicialización es idempotente en la misma fecha y rechaza cualquier otra fecha.
- [ ] A6. La segunda corrida del mismo día se rechaza con 409 y no altera el resumen previo.
- [ ] A7. Compra y publicación de venta se rechazan con 409 `MARKET_NOT_INITIALIZED` sin mover recursos.
- [ ] A8. El ranking muestra una sola entrada por jugador (verificado en la base de desarrollo: 1.860 filas para 1.860 jugadores; queda el defecto latente CR-01).
- [ ] A9. Un jugador con métricas posterior a la inicialización no falla en cada corrida (CR-02
      pendiente de decisión).

### B. Contrato HTTP

- [ ] B1. Las siete rutas responden con los campos, envoltorios y formatos del contrato.
- [ ] B2. Los ocho códigos de error nuevos se devuelven con su estado HTTP.
- [ ] B3. La precedencia de rechazos de FR-066 es la esperada.
- [ ] B4. El detalle de fallos individuales de jugadores no se expone (FR-062).

### C. Persistencia

- [ ] C1. La migración coincide con `data-model.md` y no altera el esquema previo.
- [ ] C2. Las consultas de última fila, historial, fila por fecha y ranking devuelven lo esperado.
- [ ] C3. La existencia de fila `INITIAL` es el único criterio de mercado inicializado.
- [ ] C4. `validateTestClassification` y el esquema validan con `ddl-auto: validate`.

### D. Tests

- [ ] D1. La suite nueva compila y pasa (`./gradlew test` o el comando que indique el quickstart).
- [ ] D2. Los tests de persistencia corren contra PostgreSQL real, no embebido.
- [ ] D3. Se decide si se amplía la suite con los casos de CR-10 antes de aprobar TESTING.

### E. Riesgo y deuda

- [ ] E1. Se decide el tratamiento de CR-01 y CR-02 (corrección o registro como deuda aceptada).
- [ ] E2. Se decide CR-09 (fecha de mercado futura) y CR-07 (control de rol).
- [ ] E3. Se revisan las entradas TD-022 a TD-029 registradas en esta revisión.
- [ ] E4. Se revisa el residuo documental de SPEC-010 (CR-17).

### F. Git

- [ ] F1. El diff de la rama es el revisado (`origin/dev...HEAD`).
- [ ] F2. Los commits referencian SPEC-016, US y tareas.
- [ ] F3. No hay artefactos generados ni cambios ajenos en el commit.

---

## 11. Estado y siguiente acción

- **CODE REVIEW**: informe entregado, **pendiente de aprobación humana del checklist de §10**.
- **TESTING**: pendiente; los tests deben ejecutarlos el usuario. El build no acredita tests.
- **CONVERGE** y **FEATURE COMPLETION**: no habilitados.
- **Deuda técnica**: TD-022 a TD-029 registradas; TD-011 actualizada. Ninguna deuda se declara
  resuelta.
- **Siguiente acción permitida**: aprobación humana de CODE REVIEW con el checklist de §10, y,
  posteriormente, la fase de TESTING con la ejecución de tests a cargo del usuario.

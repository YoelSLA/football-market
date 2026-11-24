# Tasks: Métricas de tendencias y ajuste de oferta-demanda en cotizaciones

**Input**: Diseño en `specs/028-market-trends-and-demand-pricing/` (`plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`).

**Prerequisites**: `plan.md` y `spec.md`; checklists aplicables evaluados.

**Tests**: Crear o modificar las pruebas backend indicadas por responsabilidad y por `docs/backend/testing.md`. El usuario ejecuta las pruebas; el agente no las ejecuta. No crear pruebas ni infraestructura frontend.

**Workflow local**: Las tareas técnicas viven únicamente en este archivo. Esta fase no implementa código ni habilita IMPLEMENT; las tareas completadas significan implementación, no cierre.

## Formato y rutas

- Cada tarea usa `- [ ] Txxx [P?] [USn?]`, en orden de ejecución; `[P]` solo indica independencia y archivos distintos.
- Las pruebas se ubican bajo `backend/src/test/java/footballmarket/features/`, siguiendo el paquete productivo y la categoría de `docs/backend/testing.md`.
- Las rutas de código corresponden a `backend/src/main/java/footballmarket/features/`; las migraciones, a `backend/src/main/resources/db/migration/`.

## Phase 1: Setup

**Propósito**: La aplicación Spring Boot, el esquema Flyway y las áreas `market` y `quote` ya existen. No hace falta inicializar proyecto, agregar dependencias ni crear infraestructura nueva para esta feature.

## Phase 2: Foundational — esquema de auditoría compartido

**Propósito**: Preparar el modelo persistido de cotizaciones y ejecuciones requerido por el recálculo habitual y el exclusivo de mercado. US1 no depende de esta fase.

- [X] T001 Añadir una migración Flyway aditiva en `backend/src/main/resources/db/migration/` con los campos de auditoría nullable de mercado/deporte para `player_quotes` y el tipo de recálculo y ventana para `quote_executions`; permitir estrategia y factor deportivo nulos solo para `MARKET_DEMAND_ONLY`, preservando filas históricas no reconstruibles.
- [X] T002 Incorporar los campos y comportamiento de auditoría del recálculo a `backend/src/main/java/footballmarket/features/quote/domain/models/PlayerQuote.java`, `backend/src/main/java/footballmarket/features/quote/domain/models/QuoteExecution.java` y `backend/src/main/java/footballmarket/features/quote/domain/enums/QuoteOrigin.java`, manteniendo las invariantes de precio, origen y compatibilidad con el recálculo existente.
- [X] T003 [P] Cubrir componentes de cálculo y auditoría, incluyendo factor de demanda, multiplicadores, límites y casos nulos históricos, en `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteAdjustmentTest.java`, `backend/src/test/java/footballmarket/features/quote/domain/models/PlayerQuoteTest.java` y `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteExecutionTest.java`.
- [X] T004 Verificar persistencia, nulabilidad histórica, discriminador de ejecución y compatibilidad de esquema en `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteMarketDemandAuditPersistenceTest.java`.

---

## Phase 3: User Story 1 — Consultar tendencias del mercado (Priority: P1) 🎯 MVP

**Goal**: Exponer a usuarios autenticados los agregados globales de transacciones y el top cinco de compras, usando la ventana configurable común.

**Independent Test**: Consultar `GET /api/market/trends` con datos conocidos y contrastar período, clasificación única de compras/ventas, top cinco, cantidades/importes y sentimiento; confirmar respuesta protegida y ausencia de actividad distinguible de neutralidad.

### Tests para User Story 1

- [X] T005 [P] [US1] Cubrir agregaciones SQL/JPQL, ventana `[startsAt, endsAt)`, clasificación de compra/venta, P2P contado una sola vez, top cinco y orden determinista en `backend/src/test/java/footballmarket/features/market/infraestructure/MarketTransactionTrendsRepositoryTest.java`.
- [X] T006 [P] [US1] Cubrir porcentajes por conteo, sesgo, agregados por jugador, ausencia de actividad y ventana única capturada por consulta en `backend/src/test/java/footballmarket/features/market/application/MarketTrendsServiceTest.java`.
- [X] T007 [P] [US1] Verificar contrato HTTP completo, DTO, errores y JWT para `GET /api/market/trends` en `backend/src/test/java/footballmarket/features/market/presentation/MarketTrendsControllerTest.java` y `backend/src/test/java/footballmarket/features/market/security/MarketTrendsAuthorizationTest.java`.

### Implementación para User Story 1

- [X] T008 Agregar la propiedad compartida `market.quote.demand-window` con valor predeterminado `PT24H` y su binding en `backend/src/main/java/footballmarket/features/quote/infraestructure/config/QuoteProperties.java` y `backend/src/main/java/footballmarket/features/quote/infraestructure/config/QuoteConfig.java`; cubrir default y binding en `backend/src/test/java/footballmarket/features/quote/infraestructure/config/QuotePropertiesTest.java`.
- [X] T009 Implementar agregaciones de tendencias y demanda desde `market_transactions`, incluyendo proyecciones y clasificación de cada transacción una sola vez, en `backend/src/main/java/footballmarket/features/market/infraestructure/MarketTransactionRepository.java`, `backend/src/main/java/footballmarket/features/market/infraestructure/projections/MarketTrendProjection.java` y `backend/src/main/java/footballmarket/features/market/infraestructure/projections/PlayerMarketDemandProjection.java`.
- [X] T010 Implementar el caso de uso de consulta de tendencias, captura de un instante por solicitud, cálculo de ventana, porcentajes, sesgo y respuesta vacía diferenciada en `backend/src/main/java/footballmarket/features/market/application/MarketTrendsService.java` y `backend/src/main/java/footballmarket/features/market/application/impl/MarketTrendsServiceImpl.java`.
- [X] T011 [P] [US1] Añadir `MarketTrendsResponseDTO` como `record`, `MarketTrendsMapper` estático y `MarketTrendsController` para `GET /api/market/trends`, incluidas anotaciones OpenAPI y el contrato de `specs/028-market-trends-and-demand-pricing/contracts/market-trends-api.md`, en `backend/src/main/java/footballmarket/features/market/presentation/MarketTrendsResponseDTO.java`, `backend/src/main/java/footballmarket/features/market/presentation/MarketTrendsMapper.java` y `backend/src/main/java/footballmarket/features/market/presentation/MarketTrendsController.java`.

---

## Phase 4: User Story 2 — Incorporar la demanda al recálculo habitual (Priority: P1)

**Goal**: Combinar la demanda neta por jugador con el score deportivo aplicable sin cambiar el comportamiento existente cuando no hay score, y auditar cada componente.

**Independent Test**: Ejecutar el recálculo habitual con score y demanda conocidos; verificar fórmula combinada, topes, piso, componentes auditables y comportamiento existente si falta score.

### Tests para User Story 2

- [X] T012 [P] [US2] Cubrir recálculo con score y mercado, demandas +10, −20 y ±100, demanda cero, ventana compartida, cap/piso y fallback sin score en `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`.
- [X] T013 [P] [US2] Verificar persistencia de los componentes deportivo/mercado, demanda por jugador, origen y precio final en `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteSportAndMarketPersistenceTest.java`.
- [X] T014 [P] [US2] Verificar que `POST /api/quotes/recalculate` conserva su contrato y expone la auditoría nueva sin regresiones en `backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java`.

### Implementación para User Story 2

- [X] T015 Implementar el cálculo por jugador de demanda neta y la composición `sportMultiplier * (1 + marketDemandFactor)` en el recálculo habitual, aplicando las reglas existentes de cap y piso, en `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java` y `backend/src/main/java/footballmarket/features/quote/application/internal/QuotePlayerPublisher.java`.
- [X] T016 Persistir y exponer en las cotizaciones y resúmenes del recálculo habitual el precio anterior, componentes, límites y precio resultante, actualizando `backend/src/main/java/footballmarket/features/quote/application/internal/QuoteExecutionRecorder.java`, `backend/src/main/java/footballmarket/features/quote/presentation/QuoteExecutionMapper.java`, `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteExecutionResponseDTO.java`, `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteResponseDTO.java` y `backend/src/main/java/footballmarket/features/quote/presentation/QuoteMapper.java`.

---

## Phase 5: User Story 3 — Recalcular cotizaciones solo por mercado (Priority: P2)

**Goal**: Permitir a un operador autorizado recalcular todos los jugadores elegibles usando únicamente demanda y dejando evidencia de que no se consultaron scores.

**Independent Test**: Ejecutar el endpoint manual con actividad conocida y scores presentes/ausentes; comprobar población completa, ausencia de consulta de scores, fórmula, límites y auditoría sin impacto deportivo.

### Tests para User Story 3

- [X] T017 [P] [US3] Cubrir recálculo poblacional solo de mercado, factor cero, fórmula acotada, precio mínimo, fallos individuales y ausencia de consulta al resolver de scores en `backend/src/test/java/footballmarket/features/quote/application/MarketDemandOnlyRecalculationServiceTest.java` y `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteAdjustmentTest.java`.
- [X] T018 [P] [US3] Verificar contrato HTTP, resumen, JWT y autorización administrativa existente de `POST /api/quotes/recalculate/market-demand` en `backend/src/test/java/footballmarket/features/quote/presentation/MarketDemandRecalculationControllerTest.java`, `backend/src/test/java/footballmarket/security/PersistedUserJwtAuthenticationConverterTest.java` y `backend/src/test/java/footballmarket/features/auth/security/UserRepositoryIdentityValidatorTest.java`.

### Implementación para User Story 3

- [X] T019 Añadir el recálculo poblacional exclusivo de mercado en `backend/src/main/java/footballmarket/features/quote/application/QuoteRecalculationService.java` y `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java`; reutilizar publicación, ventana, cap y piso sin invocar `QuoteScoreResolver`, y auditar fallos individuales.
- [X] T020 Añadir el endpoint y su documentación OpenAPI, mantener el resumen compatible, y exigir la capacidad SUPERUSER ya existente para `POST /api/quotes/recalculate/market-demand`; cambios en `backend/src/main/java/footballmarket/features/quote/presentation/QuoteController.java`, `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteExecutionResponseDTO.java`, seguridad/JWT y validación de identidad.

---

## Phase 6: Polish y coherencia transversal

**Propósito**: La coherencia de contratos, anotaciones OpenAPI y pruebas se resuelve dentro de cada historia. No se identifica trabajo transversal adicional indispensable.

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Phase 1)**: no requiere tareas de inicialización.
- **Foundational (Phase 2)**: prepara modelo, esquema y pruebas de persistencia compartidos por US2 y US3; US1 puede avanzar independientemente.
- **US1 (Phase 3)**: puede implementarse independientemente de las otras historias; T008 precede a T009–T011.
- **US2 (Phase 4)**: depende del esquema y modelo de Phase 2, y de la ventana/proyección de demanda de US1 (T008–T009).
- **US3 (Phase 5)**: depende del esquema/modelo de Phase 2 y reutiliza la ventana/proyección de demanda de US1 (T008–T009); no depende de completar US2.
- **Polish (Phase 6)**: no agrega trabajo separado; la coherencia contractual se cubre dentro de cada historia.

### Dependencias dentro de cada User Story

- **US1 (P1)**: T005–T007 pueden prepararse en paralelo; implementar propiedad/configuración (T008), agregación (T009), servicio (T010) y HTTP (T011) respetando sus dependencias.
- **US2 (P1)**: T012–T014 pueden prepararse en paralelo; T015 precede a T016.
- **US3 (P2)**: T017–T018 pueden prepararse en paralelo; T019 precede a T020.
- Los tests de backend se escriben antes o junto con el cambio que cubren; su ejecución y aceptación corresponden al usuario.

### Oportunidades de paralelización

- T003 es independiente de los demás archivos de pruebas de Phase 2; T004 cubre persistencia.
- En US1, T005, T006 y T007 afectan archivos de test distintos y pueden trabajarse en paralelo; T011 es independiente de los archivos de Repository/Service, pero integra su contrato.
- En US2, T012–T014 son archivos de pruebas distintos y pueden trabajarse en paralelo.
- En US3, T017 y T018 son archivos de pruebas distintos y pueden trabajarse en paralelo.

## Estrategia de implementación

### MVP sugerido

**US1** es el MVP sugerido: aporta una consulta autenticada e independiente de tendencias y sentimiento, apoyada por la ventana compartida. No implica que US2 o US3 queden fuera del alcance especificado.

### Entrega incremental

1. Preparar y verificar el modelo/auditoría compartidos requeridos por los recálculos.
2. Implementar y verificar independientemente US1.
3. Implementar US2 preservando el comportamiento habitual y los contratos existentes.
4. Implementar US3 sin depender de scores ni de la finalización de US2.
5. Alinear contratos y quickstart; la ejecución de tests y su evidencia quedan a cargo del usuario.

Completar tareas no equivale a revisión, testing ni cierre de la Feature. No se incluyen tareas de autorización humana ni de ejecución de pruebas por el agente.

## Phase 7: Convergence

- [X] T021 Actualizar la documentación OpenAPI de `QuoteResponseDTO.origin` para incluir `MARKET_DEMAND` entre los valores y describir su semántica según FR-008 (partial).
- [X] T022 Actualizar el test de esquema Flyway al último versionado y al total de migraciones, incluida la migración de auditoría de mercado.
- [X] T023 Incorporar en Bruno los endpoints nuevos de tendencias y recálculo solo de mercado, y actualizar los nombres de las solicitudes cuyos contratos de cotización cambiaron.

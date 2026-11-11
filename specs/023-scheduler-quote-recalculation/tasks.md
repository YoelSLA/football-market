---

description: "Task list for feature implementation"
---

# Tasks: Recálculo automático programado de cotizaciones

**Input**: Design documents from `/specs/023-scheduler-quote-recalculation/`

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/quote-executions-http.md`, `contracts/scheduling-configuration.md`, `quickstart.md`

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend. Los ejemplos genéricos del template se sustituyen por las tareas reales de esta feature.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Checklists**: `checklists/quality.md` (13/13), `checklists/scheduling-config.md` (12/12), `checklists/concurrency-integrity.md` (15/15), `checklists/requirements.md` (16/16) y `checklists/daily-scheduling-integrity.md` (8/8) están evaluados sin ítems pendientes. Este archivo no modifica sus marcadores.

**Organization**: las tareas se agrupan por user story para permitir implementación y verificación independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias con tareas incompletas)
- **[Story]**: user story a la que pertenece la tarea (`US1`, `US2`, `US3`)
- Todas las descripciones incluyen la ruta exacta del archivo afectado

## Path Conventions

- Backend: `backend/src/main/java/footballmarket/` y `backend/src/test/java/footballmarket/`
- Configuración: `backend/src/main/resources/application.yml` y `backend/src/test/resources/application-test.yml`
- Migraciones: `backend/src/main/resources/db/migration/`
- Documentación de la feature: `specs/023-scheduler-quote-recalculation/`
- Frontend: no participa; no se crean tareas ni tests de frontend

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: habilitar el scheduling de plataforma y dejar la configuración base de la feature.

- [X] T001 [P] Añadir `@EnableScheduling` (una sola vez, sin crear configuración adicional) en `backend/src/main/java/footballmarket/FootballMarketApplication.java`
- [X] T002 [P] Añadir en `backend/src/main/resources/application.yml` las claves `market.quote.strategy` (`MARKET_QUOTE_STRATEGY`, por defecto `POSITIONAL_PERFORMANCE`), `market.quote.schedule.cron` (`MARKET_QUOTE_SCHEDULE_CRON`, configuración original `0 0 0 * * MON`, sustituida por T035) y `market.quote.schedule.enabled` (`MARKET_QUOTE_SCHEDULE_ENABLED`, por defecto `true`), y fijar `spring.task.scheduling.pool.size: 1` con prefijo de hilo propio `spring.task.scheduling.thread-name-prefix`
- [X] T003 [P] Dejar el disparador inerte en la suite añadiendo `market.quote.schedule.enabled: false` en `backend/src/test/resources/application-test.yml`

**Checkpoint**: el scheduling de plataforma está habilitado y la suite no dispara el proceso automático.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema, dominio y configuración sobre los que se apoyan las tres historias.

**⚠️ CRITICAL**: ninguna user story puede completarse antes de esta fase.

- [X] T004 [P] Crear el enum `QuoteExecutionOrigin` (`MANUAL`, `AUTOMATIC`) en `backend/src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionOrigin.java`
- [X] T005 [P] Crear el enum `QuoteExecutionResult` (`SUCCESS`, `FAILED`) en `backend/src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionResult.java`
- [X] T006 [P] Crear el enum `QuoteExecutionFailureReason` (`INDIVIDUAL_PLAYER_FAILURES`) en `backend/src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionFailureReason.java`
- [X] T007 Extender `QuoteExecution` con `origin` obligatorio (validado en el constructor con `InvalidQuoteException`) y accesores derivados `result()` y `failureReason()` a partir de `failedPlayers`, sin persistirlos, en `backend/src/main/java/footballmarket/features/quote/domain/models/QuoteExecution.java` (depende de T004, T005, T006)
- [X] T008 [P] Extender `QuoteProperties` con `strategy` (`ValuationStrategy`, por defecto `POSITIONAL_PERFORMANCE`) y el bloque anidado `schedule` (`cron` con `@NotBlank` y configuración original `0 0 0 * * MON`, sustituida por T035; `enabled` por defecto `true`), conservando los valores por defecto y la validación vigentes, en `backend/src/main/java/footballmarket/features/quote/infraestructure/config/QuoteProperties.java`
- [X] T009 [P] Crear la migración Flyway con `./gradlew createMigration -PmigrationDescription=add_quote_execution_origin` desde `backend/` y completar el SQL en `backend/src/main/resources/db/migration/V<timestamp>__add_quote_execution_origin.sql`: añadir `origin VARCHAR(16) NOT NULL DEFAULT 'MANUAL'`, retirar el `DEFAULT` y añadir la restricción `ck_quote_execution_origin CHECK (origin IN ('MANUAL', 'AUTOMATIC'))`
- [X] T010 Pasar `QuoteExecutionOrigin.MANUAL` en la construcción de la ejecución de la vía manual en `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java` (depende de T007)

**Checkpoint**: el modelo persiste el origen, la configuración valida estrategia y cadencia, y la migración aplica sin reescribir historial.

---

## Phase 3: User Story 1 - Recálculo periódico sin intervención (Priority: P1) 🎯 MVP

**Goal**: que el recálculo se dispare solo con la cadencia configurada (por defecto diaria a las 12:00 UTC), reutilizando el caso de uso y la configuración del mercado, sin intervención de un operador.

**Independent Test**: configurar una cadencia de prueba, comprobar el disparo automático y sus parámetros frente a una corrida manual equivalente; verificar que el valor predeterminado sea diario a las 12:00 UTC y que un disparo automático adicional de esa fecha se omita.

### Tests para User Story 1 (backend según normas; sin tests frontend)

> Escribir los tests antes de la implementación correspondiente. Su ejecución y evidencia quedan a cargo del usuario; el agente MUST NOT ejecutarlos.

- [X] T011 [P] [US1] Test unitario del disparador (`@Tag("unit")`, `@Nested`, Service mockeado) en `backend/src/test/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java`: invoca `recalculateAutomatic()` cuando está habilitado, no invoca nada cuando `schedule.enabled` es `false`, registra `triggered` y traduce la fecha ya registrada a `skipped reason=EXECUTION_ALREADY_EXISTS`
- [X] T012 [P] [US1] Test de configuración original (`@Tag("lightweight")`, `ApplicationContextRunner`, `@Nested`) en `backend/src/test/java/footballmarket/features/quote/infraestructure/config/QuotePropertiesTest.java`: binding del cron semanal previo (reemplazado por T034), `POSITIONAL_PERFORMANCE`, `enabled=true`, sobreescritura por propiedades, fallo de contexto con estrategia fuera del catálogo y, en un contexto que registre `AutomaticQuoteRecalculationScheduler`, fallo de arranque con cron inválido (la sintaxis del cron la valida Spring al construir el disparador, no el binding de `QuoteProperties`)
- [X] T034 [US1] Actualizar el test de configuración en `backend/src/test/java/footballmarket/features/quote/infraestructure/config/QuotePropertiesTest.java` para exigir el cron predeterminado `0 0 12 * * *` y conservar la cobertura de sobreescritura configurable y validación de cron inválido
- [X] T013 [US1] Test de integración de la ejecución automática (`@Tag("integration")`, `@Nested`) ampliando `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`: `recalculateAutomatic()` persiste `origin = AUTOMATIC`, aplica estrategia y parámetros de `market.quote` y produce los mismos contadores y precios que la vía manual con los mismos insumos
- [X] T014 [US1] Adaptar `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java` al nuevo esquema: incluir `origin` en cada inserción y verificar `NOT NULL` sin valor por defecto y el rechazo del catálogo cerrado por `ck_quote_execution_origin`

### Implementation for User Story 1

- [X] T015 [US1] Declarar `QuoteExecution recalculateAutomatic()` en `backend/src/main/java/footballmarket/features/quote/application/QuoteRecalculationService.java` sin alterar la firma de `recalculate(...)`
- [X] T016 [US1] Implementar en `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java` el núcleo privado compartido y `recalculateAutomatic()`, que resuelve estrategia, factor, tope y piso desde `QuoteProperties` y persiste `origin = AUTOMATIC`, conservando el orden de validaciones y el comportamiento manual (depende de T015)
- [X] T017 [US1] Crear el disparador `AutomaticQuoteRecalculationScheduler` en `backend/src/main/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationScheduler.java` con `@Scheduled(cron = "${market.quote.schedule.cron}", zone = "UTC")`, salida temprana silenciosa si `schedule.enabled` es `false`, dependencia exclusiva de `QuoteRecalculationService` y `QuoteProperties`, y eventos `quote_automatic_recalculation_triggered` y `quote_automatic_recalculation_skipped` (motivo `EXECUTION_ALREADY_EXISTS`)
- [X] T018 [US1] Incorporar `origin` a los eventos existentes `quote_execution_started` y `quote_execution_completed` en `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java`
- [X] T035 [US1] Cambiar el cron predeterminado de `market.quote.schedule.cron` a `0 0 12 * * *` en `backend/src/main/java/footballmarket/features/quote/infraestructure/config/QuoteProperties.java` y `backend/src/main/resources/application.yml`, manteniendo la zona UTC y la posibilidad de sobrescribirlo por configuración

**Checkpoint**: US1 funciona de forma independiente; el disparo programado produce una ejecución automática trazable y el disparo duplicado de la misma fecha se omite.

---

## Phase 4: User Story 2 - Trazabilidad de cada ejecución (Priority: P2)

**Goal**: identificar cada ejecución y su resultado por los medios operativos existentes, con origen, fecha, alcance y desenlace, sin exponer detalles internos.

**Independent Test**: consultar `GET /api/quotes/executions?date=` para una fecha con varias corridas y comprobar que cada ejecución manual y la automática (si existe) se devuelve con `origin`, `result` y `failureReason`; una fecha vacía devuelve `{"executions": []}`.

### Tests para User Story 2 (backend según normas; sin tests frontend)

- [X] T019 [P] [US2] Ampliar `backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java` (`@Tag("lightweight")`, `@Nested`) para afirmar los tres campos aditivos en `POST /api/quotes/recalculate` (`origin: "MANUAL"`, `result`, `failureReason`), su presencia en `GET /api/quotes/executions` y que los códigos de error vigentes no cambian
- [X] T020 [P] [US2] Test unitario del Model en `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteExecutionTest.java` (`@Tag("unit")`, `@Nested`): origen nulo rechazado con `InvalidQuoteException`, `SUCCESS`/`null` con `failedPlayers = 0` y `FAILED`/`INDIVIDUAL_PLAYER_FAILURES` con `failedPlayers > 0`
- [X] T021 [US2] Test de integración de la consulta por fecha (`@Tag("integration")`, `@Nested`) ampliando `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`: la ejecución recuperada expone origen, resultado y motivo derivados, y una fecha sin ejecución devuelve lista vacía (depende de T013 por compartir archivo)
- [X] T036 [P] [US2] Ampliar `backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java` para verificar que la consulta por fecha conserva y expone todas las ejecuciones registradas para el día, incluidas varias manuales y una automática, sin cambiar la forma del contrato HTTP

### Implementation for User Story 2

- [X] T022 [US2] Añadir `origin`, `result` y `failureReason` (con `@Schema` descriptivo y ejemplo de catálogo cerrado) a `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteExecutionResponseDTO.java`
- [X] T023 [US2] Transportar los campos derivados desde el Model en `backend/src/main/java/footballmarket/features/quote/presentation/QuoteExecutionMapper.java` (depende de T022)
- [X] T024 [US2] Actualizar las anotaciones OpenAPI de `POST /api/quotes/recalculate` y `GET /api/quotes/executions` en `backend/src/main/java/footballmarket/features/quote/presentation/QuoteController.java` para reflejar los campos aditivos, sin cambiar firmas, rutas ni códigos
- [X] T025 [US2] Actualizar de forma aditiva el contrato `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` con `origin`, `result` y `failureReason` en los objetos de ejecución de recálculo y consulta, sin alterar rutas ni errores

**Checkpoint**: US2 es verificable sobre la API existente; la consulta por fecha identifica origen y desenlace de cada corrida.

---

## Phase 5: User Story 3 - Integridad ante fallos y solapamientos (Priority: P3)

**Goal**: registrar los errores y rechazos del caso de uso existente, sin asignar al scheduler responsabilidad por las cotizaciones ni por la recuperación del recálculo.

**Independent Test**: comprobar que varias corridas manuales pueden registrarse en una fecha, que solo una automática se admite para esa fecha, que el segundo cálculo del jugador actualiza su fila diaria y que los fallos/rechazos del caso de uso se registran sin compensación del scheduler.

### Tests para User Story 3 (backend según normas; sin tests frontend)

- [X] T026 [US3] **Cobertura histórica, no garantía vigente de concurrencia**: test de integración de dos corridas concurrentes de la misma fecha en `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`. Las clarificaciones del 2026-11-11 reemplazan su expectativa de una única ejecución sin distinguir origen; no implementar exclusión mutua ni inferir que las ejecuciones manuales quedan limitadas. T037 define la cobertura vigente de ejecuciones manuales múltiples y máximo una automática por fecha.
- [X] T027 [US3] Test de integración en `backend/src/test/java/footballmarket/features/quote/application/QuoteAutomaticRecalculationFailureTest.java` (`@Tag("integration")`, `@Nested`): comprobar que la invocación automática delega los fallos y rechazos al caso de uso existente, sin añadir compensación ni garantías transaccionales propias del scheduler; verificar el reintento manual el mismo día cuando el intento automático no deja ejecución registrada y el rechazo existente cuando sí quedó registrada, incluso con fallos individuales. La persistencia y los reintentos pertenecen a SPEC-016.
- [X] T028 [US3] Ampliar `backend/src/test/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java` (`@Tag("unit")`, `@Nested`) con los desenlaces `rejected reason=MARKET_NOT_INITIALIZED` y `failed cause=<clase>`, afirmando que el disparador no propaga la excepción (depende de T011 por compartir archivo)
- [X] T037 [P] [US3] Actualizar `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java` (`@Tag("integration")`, `@Nested`) para comprobar que varias ejecuciones manuales se registran independientemente en la misma fecha, que puede coexistir como máximo una automática y que otra automática de esa fecha se rechaza; afirmar que un segundo cálculo del jugador actualiza la fila diaria existente con el último resultado y sustituir la expectativa histórica de T026 que imponía una sola ejecución sin distinguir origen
- [X] T038 [P] [US3] Adaptar `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java` y `backend/src/test/java/footballmarket/features/quote/infraestructure/PlayerQuoteMigrationTest.java` para afirmar la unicidad automática por fecha, la admisión de ejecuciones manuales múltiples y la unicidad de cotización por jugador-fecha independientemente del origen

### Implementation for User Story 3

- [x] T029 [US3] Retirada por decisión humana: no implementar exclusión global; se conserva este identificador como historial en `specs/023-scheduler-quote-recalculation/sdd-gates.md`.
- [x] T030 [US3] Retirada por decisión humana: no agregar el motivo `EXECUTION_IN_PROGRESS`; se conserva este identificador como historial en `specs/023-scheduler-quote-recalculation/sdd-gates.md`.
- [X] T031 [US3] Completar en `backend/src/main/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationScheduler.java` la traducción de rechazos y fallos: `MarketNotInitializedException` → `quote_automatic_recalculation_rejected reason=MARKET_NOT_INITIALIZED`, excepción no controlada → `quote_automatic_recalculation_failed cause=<clase>`, en ambos casos sin propagar y sin impedir disparos futuros
- [X] T039 [P] [US3] Actualizar `backend/src/main/java/footballmarket/features/quote/application/internal/QuotePlayerPublisher.java`, `backend/src/main/java/footballmarket/features/quote/domain/models/PlayerQuote.java` y `backend/src/main/java/footballmarket/features/quote/infraestructure/PlayerQuoteRepository.java` para que un recálculo del mismo jugador y fecha UTC actualice la fila existente —incluidos precio, estrategia, origen e instante de cálculo— preservando su identidad y sin insertar una duplicada
- [X] T040 [P] [US3] Crear una nueva migración Flyway `backend/src/main/resources/db/migration/V<timestamp>__allow_daily_quote_recalculation.sql` mediante `./gradlew createMigration` desde `backend/`; retirar la unicidad global de `quote_executions.quote_date`, añadir unicidad parcial para origen `AUTOMATIC`, sustituir la unicidad de `player_quotes` por `(player_id, effective_date)` y comprobar/documentar colisiones previas sin borrar filas automáticamente
- [X] T041 [P] [US3] Actualizar `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` y las anotaciones de `backend/src/main/java/footballmarket/features/quote/presentation/QuoteController.java` para reflejar que las ejecuciones manuales pueden repetirse en la misma fecha y que `EXECUTION_ALREADY_EXISTS` no las limita

**Checkpoint**: el scheduler registra los errores y rechazos recibidos sin responsabilizarse del estado de las cotizaciones ni de la recuperación del recálculo.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: coherencia documental y verificación de entrega.

- [X] T032 [P] Documentar la responsabilidad del punto de entrada batch de cotizaciones (dependencias permitidas, ausencia de lógica de negocio, reutilización por otros procesos) en `docs/backend/architecture.md`
- [X] T033 Ejecutar `./gradlew build -x test` desde `backend/` (tarea definida en `backend/build.gradle.kts`), revisar el diff final de los archivos de `backend/src/` y de `specs/023-scheduler-quote-recalculation/tasks.md`, y comprobar `git diff --check` y `git status --short`. Los tests aplicables quedan pendientes de ejecución y validación del usuario
- [X] T042 [P] Alinear la descripción arquitectónica de la unicidad y reejecución diaria en `docs/backend/architecture.md` con el límite de una corrida automática por fecha, las manuales múltiples y la cotización única por jugador-fecha

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las user stories
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de Foundational; consume el origen persistido por US1 para su verificación
- **US3 (Phase 5)**: depende de Foundational; reutiliza el disparador de US1
- **Polish (Phase 6)**: depende de las historias incluidas en el alcance

### User Story Dependencies

- **US1 (P1)**: independiente tras Foundational
- **US2 (P2)**: independiente tras Foundational; su test de consulta depende de que exista la ejecución con origen (T016)
- **US3 (P3)**: independiente tras Foundational; su test del disparador depende de T017. Para el delta diario, T037/T038 preceden a T039/T040; la migración de T040 debe aplicarse antes de ejecutar tests de persistencia e integración. No se requiere una garantía de exclusión mutua entre recálculos concurrentes; el escenario de concurrencia de T026 es histórico y no debe ampliarse a las ejecuciones manuales ilimitadas.
- **Dependencia técnica registrada**: la migración (T009) debe aplicarse antes de cualquier ejecución real de la suite de integración

### Within Each User Story

- Tests antes de la implementación correspondiente; su ejecución es del usuario
- Enums y Model antes de servicios
- Interfaz del caso de uso antes de su implementación
- Implementación del caso de uso antes del disparador
- Disparador antes de sus desenlaces de omisión, rechazo y fallo
- DTO antes de Mapper y de anotaciones OpenAPI

### Parallel Opportunities

- T001, T002 y T003 (archivos distintos)
- T004, T005, T006, T008 y T009 (archivos distintos)
- T011 y T012 (archivos de test distintos); T019 y T020 (archivos de test distintos)
- T036, T037 y T038 pueden prepararse en paralelo porque afectan, respectivamente, Controller, Service y persistencia/migración
- Tras T036–T038, T039, T040 y T041 pueden ejecutarse en paralelo en archivos de implementación, migración y contrato/OpenAPI distintos
- T032 en paralelo con la verificación de T033
- T042 puede realizarse en paralelo con cambios de backend al afectar solo documentación

---

## Parallel Example: User Story 1

```bash
# Tests backend de US1 en archivos distintos (no ejecutarlos desde el agente):
Task: "Test unitario del disparador en backend/src/test/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java"
Task: "Test de configuración en backend/src/test/java/footballmarket/features/quote/infraestructure/config/QuotePropertiesTest.java"
```

## Parallel Example: User Story 2

```bash
# Tests de contrato y de dominio en archivos distintos:
Task: "Ampliar QuoteControllerTest con los campos aditivos"
Task: "Test unitario del Model QuoteExecution para origen y resultado derivado"
```

## Parallel Example: Daily execution delta (US3)

```bash
# Los tests de Controller, Service y persistencia usan archivos distintos:
Task: "Probar la respuesta de consulta por fecha con varias ejecuciones en backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java"
Task: "Probar ejecuciones manuales repetidas y límite automático diario en backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java"
Task: "Probar restricciones de ejecución y cotización en backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java y backend/src/test/java/footballmarket/features/quote/infraestructure/PlayerQuoteMigrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (bloquea todas las historias)
3. Completar Phase 3: User Story 1
4. **STOP y validar**: solo con IMPLEMENT AUTHORIZATION vigente; los tests los ejecuta el usuario
5. No continuar con US2/US3 sin autorización del alcance correspondiente

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 autorizada → implementación → CODE REVIEW humana → TESTING humano
3. US2 autorizada → implementación → CODE REVIEW humana → TESTING humano
4. US3 autorizada → implementación → CODE REVIEW humana → TESTING humano
5. Trabajo técnico completo → CONVERGE → FEATURE COMPLETION AUTHORIZATION humana

Completar Tasks no equivale a Ready, revisión, testing ni cierre. Las autorizaciones humanas no son tareas técnicas y no se incluyen aquí.

---

## Trazabilidad de requisitos

| Requisito | Tareas |
| --- | --- |
| FR-001 | T002, T008, T011, T012, T017, T034, T035 |
| FR-002 | T008, T012, T013, T016 |
| FR-003 | T013, T016 (recorrido compartido, sin cambios en valuación) |
| FR-004 | T004, T005, T006, T007, T009, T010, T013, T014, T016, T018, T019, T020, T021, T022, T023, T024, T025 |
| FR-005 | T021, T024, T025, T036, T041 (consulta existente, sin ruta nueva) |
| FR-006 | T009, T011, T014, T017, T026, T027, T037, T038, T040, T041 |
| FR-009 | T016, T017 |
| FR-010 | T006, T019, T020, T022, T023, T025 |
| FR-011 | T001, T002, T017, T032 |
| FR-012 | T027, T028, T031 |
| FR-013 | T027, T031 |
| FR-014 | T037, T038, T039, T040 |
| FR-015 | T037, T038, T040, T041 |
| Cadencia diaria 12:00 UTC (clarificación 2026-11-11) | T034, T035 |
| Consulta de múltiples ejecuciones por fecha (US2) | T036, T041 |

---

## Notes

### Entrega del delta diario — 2026-11-11

- T034–T042 implementadas; T034 autorizada expresamente al confirmar el ajuste del test diario y T042 realizada como última tarea. Los tests están escritos/revisados estáticamente, no ejecutados ni compilados por el agente.
- Migración generada: `V20261111163841__allow_daily_quote_recalculation.sql`; no aplicada a una base. Incluye comprobación de colisiones previas y detención sin borrar filas.
- Último `gradlew.bat build -x test`: `BUILD SUCCESSFUL` en 5 s; 29 migraciones válidas y clasificación estática de 143 clases. Los fallos intermedios de formato del cambio fueron corregidos.
- Por coherencia de contratos se adaptaron las expectativas antiguas de `QuoteAutomaticRecalculationFailureTest` y `QuoteQueryServiceTest`; no se amplió comportamiento ni se añadió exclusión global.
- Informe del alcance y checklist humano: `daily-implementation-report.md`. Ejecución de tests, aplicación real de migración, CODE REVIEW, TESTING y cierre pendientes. Sin fases posteriores, handoffs, commits, push ni escritura remota.

### Ejecución parcial de IMPLEMENT — 2026-11-04

- T001–T010 implementadas e inspeccionadas; T007 incluye la adaptación mecánica de los constructores en `QuoteControllerTest` y `support/ScoreRankingTestData` al origen obligatorio.
- Migración generada por `createMigration`: `V20261104144409__add_quote_execution_origin.sql`; no aplicada a ninguna base por el agente.
- T011–T031 pendientes: la detención por la contradicción entre FR-013/US3/SC-003 y la conservación de `REQUIRES_NEW` quedó resuelta como **decisión de alcance** el 2026-11-04: la ejecución automática no compensa las cotizaciones ya confirmadas por jugadores individuales (TD-024), porque esa responsabilidad excede su alcance. Los artefactos se ajustaron en consecuencia; continúa pendiente un nuevo ANALYZE y la autorización humana vigente antes de reanudar.
- Ningún test ejecutado. Binding, migración real, invariantes del modelo y comportamiento funcional pendientes de tests y validación humana. El build no acredita esas comprobaciones.

### Reanudación de IMPLEMENT — 2026-11-04

- Alcance activo T011–T028 y T031 implementado; T029/T030 permanecen retiradas, sin marcar como trabajo completado. Phase 6 fuera del encargo.
- El ANALYZE de la base actual y la autorización persistida se recuperaron de `sdd-gates.md`; la restricción previa por TD-024 quedó superada por la decisión de alcance, sin resolver esa deuda.
- T013 se verifica por contadores y configuración desde `QuoteRecalculationServiceTest`, y por precios desde la API pública de consulta en `QuoteQueryServiceTest` (dos entradas con iguales fixtures y expectativas). No se accede a repositorios para verificar el caso de uso.
- T027 usa un reloj controlado para inducir fallo completo al cerrar la corrida, y fixtures de Arrange para fallos individuales; servicios y persistencia reales. No exige compensación.
- Las casillas de tests acreditan escritura e inspección, nunca ejecución. Compilación de tests, pruebas funcionales, migración aplicada y validación humana siguen pendientes. Comandos e informe completo en `implementation-report.md`.
- `gradlew.bat build -x test` satisfactorio tras resolver formato; no ejecutó tests ni compiló sus fuentes. No se ejecutaron fases posteriores, commits ni publicación.

### Phase 6 autorizada — 2026-11-04

- T032 documentada en `docs/backend/architecture.md` §1.8: dependencias del disparador, ausencia de lógica de negocio y reutilización de scheduling; sin ampliar responsabilidades ni implementar #41.
- T033: `gradlew.bat build -x test` satisfactorio (1 s; 1 tarea ejecutada y 10 up-to-date), revisión del diff y estado local. No se modificó código funcional en esta fase.
- Tests y migración real pendientes del usuario; completar tareas no aprueba CODE REVIEW, TESTING ni cierre. No se ejecutaron CONVERGE, handoffs, commits ni push.

- `[P]` indica archivos distintos y ausencia de dependencia con tareas incompletas; no autoriza delegación ni solapar archivos.
- T034–T042 desglosan el delta de clarificaciones del 2026-11-11; se conservan los IDs, estados completados y tareas retiradas del trabajo previo. Las tareas nuevas no implican autorización para IMPLEMENT.
- Si la comprobación previa a la migración T040 detecta colisiones de jugador-fecha, detener la migración y solicitar decisión humana; no eliminar ni escoger filas automáticamente.
- Los tests backend deben declarar exactamente una `@Tag` literal en la clase contenedora (`unit`, `lightweight` o `integration`) y agrupar todos los casos en clases `@Nested`.
- No se crean tests ni infraestructura de frontend: esta feature no modifica el frontend.
- No se añaden dependencias, rutas, tablas ni consultas históricas nuevas; cualquier necesidad en ese sentido requiere detenerse y resolverla upstream.
- No se ejecutan tests desde el agente. `./gradlew build -x test` no acredita testing ni cierre.
- Mantener `tasks.md` como fuente única de tareas; no duplicar su estado en Issues ni en el seguimiento operativo.

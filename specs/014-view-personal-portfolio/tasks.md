---
description: "Task list template for feature implementation"
---

# Tasks: Consultar portfolio personal (SPEC-014)

**Input**: Design documents from `specs/014-view-personal-portfolio/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md, checklists/consistency.md

**Tests**: Se incluyen tests backend necesarios según Constitution y normas aplicables; su ejecución corresponde al usuario, nunca al agente. No se crean tests ni infraestructura frontend.

**Workflow local**: Se sigue `docs/development/sdd-workflow.md`. Las Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. Se conservan IDs y avance al reanudar.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **[BLOQUEADA: SPEC-015]**: Tarea cuya ejecución depende de que el componente contable de SPEC-015 exista. No se puede cerrar con un doble: el comportamiento contable solo se acredita con el componente real. Ninguna tarea lo lleva hoy: el componente está implementado e integrado en `dev`.

## Path Conventions

- **Web app**: `backend/src/`, `frontend/src/`
- Rutas, listas y contratos en `specs/014-view-personal-portfolio/`
- Comandos Gradle desde `backend/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificaciones preparatorias y decisiones que condicionan a todas las historias. No son entregables de código.

- [X] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas: `BigDecimal` pertenece a la plataforma, `springdoc-openapi` ya está declarado y el generador de migraciones ya está registrado
- [X] T002 [P] Confirmar la siguiente versión de migración disponible con `ls backend/src/main/resources/db/migration/` y que la nueva usará timestamp UTC según `docs/development/flyway-migrations.md`
- [X] T003 [P] Verificar si `FinancialPositionService` de SPEC-015 está integrado y disponible en `backend/src/main/java/footballmarket/services/FinancialPositionService.java`; si no lo está, dejar constancia del bloqueo en `specs/014-view-personal-portfolio/tasks.md` y no simular ni sustituir el componente contable
- [X] T004 [P] Verificar si `docs/backend/architecture.md` §1.3 documenta ya la relación Service → Service que habilita el consumo del componente contable; si no lo está, registrar la dependencia en `specs/014-view-personal-portfolio/tasks.md` sin modificar ese documento, que corresponde a SPEC-015
- [X] T005 [P] Revisar `git status --short` y preservar los cambios locales ajenos antes de escribir
- [X] T006 [P] Verificar que la fuente de cotizaciones vigente es la del mercado y que `QuoteSource` y `CurrentQuote` se consumen sin modificación en `backend/src/main/java/footballmarket/integrations/QuoteSource.java`, que llega ya extendido por SPEC-015 con la adquisición en lote `currentQuotes(Set<Long>)`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Piezas que TODAS las historias necesitan. Ninguna historia puede comenzar antes de que esta fase esté completa.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Crear la migración del índice por propietario con `./gradlew createMigration -PmigrationDescription=index_player_tokens_owner` desde `backend/` y escribir `CREATE INDEX idx_player_tokens_owner ON player_tokens (owner_user_id, player_id);` en el archivo generado, sin alterar tablas, restricciones ni índices existentes. Antes de integrarla, verificar que su versión no colisiona con la migración del índice de emisiones de SPEC-015 —`player_token_emissions (owner_user_id)`, aplicada en otra rama—: si ambas coinciden, resolver la numeración de forma explícita y sin modificar la migración de SPEC-015
- [X] T008 [P] Implementar `PortfolioIntegrityException` en `backend/src/main/java/footballmarket/models/exceptions/PortfolioIntegrityException.java` heredando de `DomainException` conforme a `docs/backend/architecture.md` §1.6, para la divergencia entre la tenencia persistida y la reconstrucción contable
- [X] T009 [P] Implementar `InvalidPortfolioPageException` en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioPageException.java` heredando de `DomainException`, para `page` negativa, `size` fuera de 1–100 o valor no entero
- [X] T010 [P] Implementar `InvalidPortfolioStatusException` en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioStatusException.java` heredando de `DomainException`, para un `status` distinto de `OPEN` o `CLOSED`
- [X] T011 Añadir los handlers en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`: `PortfolioIntegrityException` y `FinancialPositionIntegrityException` a 500 `INTERNAL_ERROR` genérico sin detalle técnico; `InvalidPortfolioPageException` e `InvalidPortfolioStatusException` a 400 `INVALID_PARAMETER_TYPE`, reutilizando el código vigente y distinguiendo el motivo en el mensaje. El fallo técnico de la fuente de cotizaciones MUST mapearse a 503 `QUOTE_SOURCE_UNAVAILABLE` capturando **la excepción de integración que propague el propio componente contable al no poder valorar**, identificada por su origen y por el momento de la valoración, y MUST NOT registrarse como un handler genérico de `IntegrationException`: ese handler alteraría el tratamiento vigente de las integraciones de otras features —`FOOTBALL_DATA_UNAVAILABLE` en 502, `HEADED_BROWSER_UNAVAILABLE` en 503 y las de WhoScored—. Si al integrar SPEC-015 el tipo lanzado no resulta distinguible, MUST ajustarse al tipo efectivamente lanzado en lugar de broadening el alcance, y la coordinación queda registrada en `specs/014-view-personal-portfolio/tasks.md`
- [X] T012 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerTokenRepository.java` la consulta de las tenencias del propietario como **proyección de identificador de jugador y cantidad**, apoyada en el índice `idx_player_tokens_owner` de T007 y sin materializar las tenencias ni sus jugadores
- [X] T013 [P] Añadir en `backend/src/main/java/footballmarket/repositories/TokenListingRepository.java` la consulta agrupada de las reservas del propietario por jugador, entre publicaciones `ACTIVE` y no vencidas, sin índice nuevo
- [X] T014 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` la lectura por lote de los jugadores indicados con su equipo, sin filtrar por estado activo, para informar también los jugadores inactivos
- [X] T015 Crear en `backend/src/main/java/footballmarket/models/records/PortfolioView.java` el record de vista con el resumen, las posiciones de la página y los metadatos de paginación, y **como record anidado dentro de él** el record de la posición de la vista con sus campos y su nulabilidad, según `data-model.md` §3. Es un único archivo, de modo que el inventario de archivos del plan no crece
- [X] T016 Crear la interfaz `PortfolioService` en `backend/src/main/java/footballmarket/services/PortfolioService.java` con la operación de consulta del portfolio para un identificador de usuario y su documentación de contrato, con interfaz e implementación separadas conforme a `docs/backend/architecture.md` §1.1

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Consultar el resumen y las posiciones de mi portfolio (Priority: P1) 🎯 MVP

**Goal**: Un usuario autenticado consulta su portfolio y obtiene el resumen global y sus posiciones con las cifras contables, las cantidades y los datos descriptivos del jugador.

**Independent Test**: Con un usuario autenticado con posiciones en varios jugadores, con y sin cotización vigente, consultar su portfolio y comprobar la forma de la respuesta, las cantidades, las cifras y el resumen. No requiere ninguna operación de mercado ni el historial de transacciones.

### Tests para User Story 1 (backend; sin tests frontend)

> Escribir tests backend necesarios; resultados y ejecución quedan a cargo del usuario. MUST NOT ejecutar tests desde el agente.

- [X] T017 [P] [US1] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/PortfolioPersistenceTest.java` con grupos `@Nested` que cubran la consulta de las tenencias del propietario, la agrupación de sus reservas vigentes excluyendo publicadas, canceladas, vendidas y vencidas, la lectura por lote de jugadores con su equipo incluyendo jugadores inactivos, y la presencia y uso del índice por propietario (SC-011, E16, E30, E31, E32, E33)
- [X] T018 [P] [US1] Escribir tests de Service en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran la composición del portfolio, la identidad `quantity = reservedQuantity + availableQuantity`, la divergencia entre tenencia y reconstrucción con error de integridad, la ausencia de cotización con valoración incompleta y resultado realizado conservado, la propagación del fallo técnico de cotizaciones, el portfolio vacío con ceros, la coherencia de una respuesta bajo concurrencia y el valor reflejado de la cotización de la fuente, sin dependencia de un valor constante (SC-001, SC-002, SC-004, SC-012, E7, E8, E17, E18, E19, E24, E26, E29)
- [X] T019 [P] [US1] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` con `@Tag("lightweight")` que cubran la ruta, la autenticación con cadena de seguridad real, el 401 sin invocar el Service, la presencia de los tres objetos de nivel superior y de los campos que US1 entrega —identificación del jugador, estado, cantidades, cifras y resumen—, y el rechazo sin efectos ante parámetros inválidos (SC-009, E1, E2, E5, E6). La correspondencia completa con el contrato, incluidos los porcentajes por posición que expone US3, se verifica en T036

### Implementation for User Story 1

- [X] T020 [P] [US1] Crear `PortfolioSummaryResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioSummaryResponseDTO.java` con los trece indicadores globales del resumen y sus unidades, con las anotaciones de esquema
- [X] T021 [P] [US1] Crear `PortfolioPositionResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java` con los campos de la posición y su nulabilidad, distinguiendo los importes enteros en centésimas de los porcentajes decimales
- [X] T022 [P] [US1] Crear `PortfolioPaginationResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPaginationResponseDTO.java` con `page`, `size`, `totalElements` y `totalPages`
- [X] T023 [P] [US1] Crear `PortfolioResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioResponseDTO.java` con `summary`, `positions` y `pagination` como objetos de nivel superior
- [X] T024 [P] [US1] Crear `PortfolioMapper` en `backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java` con la traducción de la vista de dominio a los DTOs, sin cálculos ni lógica de negocio, con constructor privado y métodos estáticos
- [X] T025 [US1] Implementar `PortfolioServiceImpl` en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java`: declarar la operación con `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`; obtener el conjunto completo de posiciones del componente contable y su agregación contable sobre ese conjunto completo; leer las tenencias y las reservas del usuario; cruzar ambas fuentes y detectar la divergencia; componer el resumen con los agregados y los conteos propios; paginar; y devolver la vista, sin recalcular ningún importe contable ni copiar regla alguna de REQ-S2-20 (AC07, AC08, AC12, AC14)
- [X] T026 [US1] Exponer `GET /api/portfolio/me` en `backend/src/main/java/footballmarket/controllers/PortfolioController.java` con las anotaciones de OpenAPI de la ruta, sus parámetros `page` y `size`, sus respuestas y sus códigos de error, resolviendo la identidad desde el JWT y sin aceptar ningún identificador de usuario del cliente
- [X] T027 [US1] Verificar en `specs/014-view-personal-portfolio/contracts/portfolio-api.md` la correspondencia exacta campo por campo con la respuesta implementada y registrar el resultado; modificar el contrato solo si aparece una discrepancia real
- [X] T028 [US1] Verificar que la consulta no altera operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos, y que no expone posiciones, operaciones ni resultados de otro usuario, y que cada operación confirmada cuenta una sola vez por rol (SC-009, SC-010, E6, E32, E34, E35)

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Filtrar y paginar las posiciones de mi portfolio (Priority: P2)

**Goal**: El usuario filtra sus posiciones por estado y las recorre por páginas, con un orden determinista y un resumen que no depende ni del filtro ni de la página.

**Independent Test**: Con un usuario que tenga posiciones abiertas y cerradas, consultar su portfolio con y sin filtro y con paginación explícita, y comprobar qué posiciones aparecen, en qué orden y que el resumen es idéntico. No requiere operaciones de mercado.

### Tests para User Story 2 (backend; sin tests frontend)

- [X] T029 [P] [US2] Escribir tests de Service del filtro, del orden y de la paginación en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran el filtro `OPEN` y el filtro `CLOSED`, la paginación por defecto y la personalizada, la página fuera de rango, la independencia del resumen respecto del filtro y de la página, el reflejo del cambio de estado entre dos consultas paginadas distintas, el orden de las posiciones sin cotización después de las valoradas y el orden alfabético de las posiciones cerradas (SC-003, E10, E12, E13, E14, E15, E21, E28)
- [X] T030 [P] [US2] Escribir tests de Controller del filtro y de la paginación en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` que cubran el rechazo de `status` inválido y de `page` o `size` fuera de rango con 400 `INVALID_PARAMETER_TYPE`, y la forma de los metadatos de paginación sobre el conjunto filtrado (E3, E4, E14)

### Implementation for User Story 2

- [X] T031 [US2] Exponer el parámetro opcional `status` en `backend/src/main/java/footballmarket/controllers/PortfolioController.java`, admitiendo solo las formas exactas `OPEN` y `CLOSED`, sin conversión de mayúsculas ni tolerancia a valores desconocidos, y rechazando el resto con 400
- [X] T032 [US2] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` la aplicación del filtro por estado, el orden determinista —abiertas con cotización por valor actual descendente, abiertas sin cotización y cerradas por nombre de jugador, con `playerId` como desempate— y la paginación sobre el conjunto filtrado, con los metadatos contando ese conjunto, la página fuera de rango respondiendo 200 con `positions` vacío y el resumen del portfolio completo (AC12, AC13, AC14, AC15, AC21, AC22, AC23, AC24, AC25)
- [X] T033 [US2] Extender en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` la verificación de que dos consultas sobre el mismo estado devuelven la misma secuencia de posiciones y de que el resumen no depende del filtro ni de la página, con reloj controlado (SC-007, E14, E27)

**Checkpoint**: User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Consultar posiciones cerradas y el historial financiero acumulado (Priority: P3)

**Goal**: El usuario consulta también las posiciones que ya cerró, con su resultado realizado acumulado, su ciclo de tenencia, sus contadores y los porcentajes de rendimiento y peso.

**Independent Test**: Con un usuario que cerró una posición y la reabrió, y con el superusuario y su emisión inicial, consultar su portfolio y comprobar el estado de las posiciones, la acumulación del resultado realizado, las fechas, los contadores y los porcentajes. No requiere operaciones de mercado adicionales.

### Tests para User Story 3 (backend; sin tests frontend)

- [X] T034 [P] [US3] Escribir tests de Service de las posiciones cerradas y del ciclo de tenencia en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran la posición cerrada con valor actual y resultado no realizado nulos y resultado total igual al realizado acumulado, el estado de valoración `NOT_APPLICABLE`, el cierre y la reapertura del ciclo, la acumulación del resultado realizado sin reinicio, y la nulabilidad de `lastTransactionAt` en una posición sin operaciones confirmadas (SC-005, E9, E11, E21, E22)
- [X] T035 [P] [US3] Escribir tests de Service de los porcentajes en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran el rendimiento individual respecto del costo de adquisición de la posición, el peso porcentual sobre el valor total de todas las posiciones abiertas, la nulabilidad por denominador cero y por valor total indefinido, y el porcentaje global con el costo histórico total pagado como denominador (SC-013, SC-014, E22, E23)
- [X] T036 [P] [US3] Escribir tests de Controller de la respuesta completa en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` que cubran la presencia y la nulabilidad de todos los campos del contrato, incluidos los de las posiciones cerradas y los porcentajes nulos, y la correspondencia exacta con `contracts/portfolio-api.md`

### Implementation for User Story 3

- [X] T037 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` la normalización de las posiciones cerradas —cotización, instante de cotización, valor actual, resultado no realizado y precio medio de adquisición nulos, resultado total igual al realizado acumulado y estado de valoración `NOT_APPLICABLE`— y la propagación del nulo de `lastTransactionAt` cuando la posición no tiene operaciones confirmadas (FR-013, FR-020, FR-022, FR-030, research R-05)
- [X] T038 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` los porcentajes propios de la feature: el rendimiento no realizado de cada posición abierta respecto de su costo de adquisición y su peso porcentual sobre el valor total de todas las posiciones abiertas, con `HALF_UP` a dos decimales y nulos cuando su denominador es cero o el valor total no está disponible, sin recalcular ningún importe contable ni derivar el costo desde un precio medio redondeado (FR-028, FR-029, research R-03)
- [X] T039 [US3] Exponer en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java` y en `backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java` los porcentajes por posición, con su tipo decimal de dos decimales y su nulabilidad
- [X] T040 [US3] Verificar en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` que la emisión inicial de tokens del superusuario aparece como posición abierta con costo de adquisición cero, sin incremento de los contadores de compras y con participación en la valoración (SC-014, E20, AC36)
- [X] T041 [US3] Verificar en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` el caso del jugador que dejó de estar en el catálogo: la posición se conserva, su resultado financiero se preserva, los datos descriptivos irrecuperables se informan nulos y una inconsistencia que impida calcular el portfolio produce el error de integridad (FR-040, E24, E25, AC33)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificaciones y ajustes que afectan a varias historias.

- [X] T042 [P] Verificar la coherencia entre `specs/014-view-personal-portfolio/contracts/portfolio-api.md`, `specs/014-view-personal-portfolio/data-model.md` y la respuesta implementada, y registrar el resultado
- [X] T043 Ejecutar el control permitido `./gradlew build -x test` desde `backend/` y registrar el resultado; la ejecución de las pruebas queda a cargo del usuario
- [X] T044 [P] Revisar que la implementación no introduce consultas por posición ni dependencias nuevas, y que el conteo de consultas es el previsto en `plan.md` §11, con la proyección de tenencias y la adquisición de cotizaciones encapsulada en el componente contable (FR-039, SC-011)
- [X] T045 [P] Actualizar `docs/backend/architecture.md` con el apartado de los componentes de SPEC-014 solo si la actualización de la matriz de dependencias que corresponde a SPEC-015 ya está integrada, y sin duplicar su edición; si no lo está, dejar constancia de la dependencia sin tocar el documento
- [X] T046 Revisar `specs/014-view-personal-portfolio/spec.md` y `specs/014-view-personal-portfolio/plan.md` contra lo implementado, registrar las discrepancias y corregirlas solo en su origen
- [X] T047 [P] Revisar la coherencia de `specs/014-view-personal-portfolio/checklists/consistency.md` con el resultado de la implementación y actualizar su registro de hallazgos
- [X] T048 Compartir con el usuario la evidencia de las pruebas: escenarios ejecutados de `specs/014-view-personal-portfolio/quickstart.md` con sus resultados, y dejar constancia expresa de los pendientes

---

## Dependencias resueltas con SPEC-015

El componente contable de SPEC-015 está implementado e integrado en `dev`, y la matriz de dependencias de
la arquitectura ya documenta la relación Service → Service. **Ninguna tarea queda bloqueada.**

| Dependencia | Estado verificado | Tareas que la consumen |
| --- | --- | --- |
| `FinancialPositionService` real | Implementado e integrado en `dev`; su interfaz `positionsOf` y la agregación `FinancialPositionCalculator.aggregate` coinciden con lo planificado | T018, T025, T040, T041 |
| `docs/backend/architecture.md` §1.3 y §5 | Relación Service → Service autorizada para la integración de portfolio | T004, T045 |

Las tareas que consumen el componente **deben usar su implementación real**: el comportamiento contable —costo
de adquisición, resultados, porcentaje global, agregados, ciclo de tenencia, recuentos por rol e instante de la
cotización— no se acredita con un doble.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories. Su cierre contable depende de SPEC-015
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — Extiende `PortfolioServiceImpl` y los tests de US1, de modo que sus tareas de implementación y las de US1 se ejecutan en secuencia sobre los mismos archivos
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — Extiende `PortfolioServiceImpl`, el DTO de posición y el Mapper; ejecución secuencial sobre los mismos archivos

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
- Different user stories can be worked on in parallel by different team members, con la salvedad de que las tres historias comparten `PortfolioServiceImpl`, `PortfolioPositionResponseDTO` y `PortfolioMapper`: su implementación es secuencial, aunque sus tests puedan escribirse en paralelo

---

## Parallel Example: User Story 1

```bash
# Escribir tests backend de US1; no ejecutarlos desde el agente:
Task: "Tests de Repository de tenencias, reservas y jugadores en backend/src/test/java/footballmarket/repositories/PortfolioPersistenceTest.java"
Task: "Tests de Service del portfolio en backend/src/test/java/footballmarket/services/PortfolioServiceTest.java"
Task: "Tests de Controller del contrato HTTP en backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java"

# Lanzar en paralelo los DTOs y el Mapper de US1:
Task: "Crear PortfolioSummaryResponseDTO en backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioSummaryResponseDTO.java"
Task: "Crear PortfolioPositionResponseDTO en backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java"
Task: "Crear PortfolioPaginationResponseDTO en backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPaginationResponseDTO.java"
Task: "Crear PortfolioResponseDTO en backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioResponseDTO.java"
Task: "Crear PortfolioMapper en backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2 (tests primero; implementación secuencial sobre `PortfolioServiceImpl`)
   - Developer C: User Story 3 (tests primero; implementación secuencial sobre `PortfolioServiceImpl`)
3. Stories complete and integrate independently

---

## Notes
### Autorización de implementación — 2026-10-21

- **Autorización**: el responsable del proyecto invocó `/speckit.implement` sobre SPEC-014 con el
  alcance de las 48 tareas de este archivo, tras la revisión de preparación, el Analyze sin
  bloqueantes y la resolución de las dependencias A5 y A6 con SPEC-015 integrada en `dev`.
- **Alcance autorizado**: las tres historias de usuario de esta spec, sin exceder el contrato de
  `contracts/portfolio-api.md` ni las reglas contables de REQ-S2-20.
- **Evidencia de comparación**: `spec.md` (47 FR, 39 AC, 15 SC, 42 CL), `plan.md`, `research.md`,
  `data-model.md`, `contracts/portfolio-api.md` y `checklists/consistency.md` (39/39 revisados).
- **Checklists**: `checklists/consistency.md` con 39/39 ítems marcados y ninguno pendiente. SPEC-014 no
  tiene `checklists/requirements.md` porque la fase Specify se ejecutó de forma manual en esta
  feature; su verificación de calidad quedó cubierta por ese checklist y por el Analyze repetido.

### Evidencia de la implementación — 2026-10-21

- **Producción**: `PortfolioController`, `PortfolioResponseDTO`, `PortfolioSummaryResponseDTO`,
  `PortfolioPositionResponseDTO`, `PortfolioPaginationResponseDTO`, `PortfolioMapper`,
  `PortfolioService`, `PortfolioServiceImpl`, `PortfolioView`, `PortfolioIntegrityException`,
  `InvalidPortfolioPageException`, `InvalidPortfolioStatusException`, las tres consultas de conjunto
  en `PlayerTokenRepository`, `TokenListingRepository` y `PlayerRepository`, los handlers en
  `GlobalExceptionHandler` y la migración `V20261021004334__index_player_tokens_owner.sql`.
- **Documentación**: apartado 6 de `docs/backend/architecture.md` con los componentes de SPEC-014.
- **Pruebas escritas, ejecución pendiente del usuario**: `PortfolioServiceTest` (integration),
  `PortfolioControllerTest` (lightweight) y `PortfolioPersistenceTest` (integration). El agente no las
  ejecuta: su resultado y el de los escenarios de `quickstart.md` quedan pendientes de evidencia
  humana.
- **Control permitido ejecutado**: `./gradlew build -x test` desde `backend/`, con compilación de
  producción y de pruebas, `spotlessCheck` y `validateTestClassification` (**113 clases de test,
  clasificación válida**) en verde. El build no ejecutó ninguna suite de pruebas.
- **Verificación de contrato**: cotejo campo por campo entre `contracts/portfolio-api.md` y los DTOs
  implementados — 26 campos de posición, 13 de resumen y 4 de paginación, sin ausencias ni
  sobrantes.

### Correcciones del Code Review — 2026-10-21

La revisión de código de la implementación detectó tres hallazgos de severidad media, corregidos antes
de la entrega:

1. **N+1 en la consulta de tenencias**: `PlayerToken.player` es una asociación EAGER y el proyecto
   no configura `default_batch_fetch_size`, de modo que la consulta de tenencias abría un acceso por
   posición y cargaba los jugadores dos veces. La consulta **proyecta** ahora identificador de jugador
   y cantidad, sin materializar `PlayerToken` ni `Player` (T012, T044).
2. **Cobertura del ordenamiento**: las pruebas verificaban solo el grupo de posiciones valoradas. Se
   añadieron los casos de posiciones abiertas **sin cotización** y de **varias posiciones cerradas**
   ordenadas por nombre, conforme a D07, AC12 y al escenario E14 (T029).
3. **Divergencia con el plan**: `plan.md` §11 describía la lectura de jugadores como la de la página.
   Se alineó con el diseño real —el conjunto completo de posiciones, necesario para ordenar por
   nombre antes de paginar— y se dejó constancia de la razón.

Además se eliminó el mock innecesario de `QuoteSource` en el test de repositorio y se añadió la
aserción de `priceUpdatedAt` en la posición valorada.

### Correcciones de compilación y aislamiento — 2026-10-21

La primera ejecución de las pruebas de la feature no llegó a compilar las fuentes de prueba. Se
corrigieron los errores de `compileTestJava` sin tocar código de producción ni los contratos
funcionales:

1. **Import ausente**: `PortfolioServiceTest` usaba `List` sin importarlo.
2. **Contrato del repositorio**: `PortfolioPersistenceTest` seguía llamando a
   `findHoldingsByOwner` con el contrato anterior (entidades `PlayerToken`). Las dos pruebas se
   adaptaron a la proyección `HoldingQuantity`, conservando las aserciones funcionales originales
   —`playerId` y `quantity` por posición— sin materializar tenencias ni jugadores.
3. **Aislamiento de los datos compartidos**: el superusuario es un dato único y reutilizado por
   toda la base de pruebas, de modo que acumula tenencias entre pruebas y entre clases. Las
   afirmaciones que se apoyaban en «la primera posición» o en el tamaño exacto de la lista se
   acotaron a la posición del jugador de la prueba (`playerId`), siguiendo el patrón ya usado en
   las pruebas de persistencia del repositorio. Las de `PortfolioServiceTest` que consultan el
   portfolio del superusuario son `posicionEmitidaDelSuperusuario` y `porcentajeIndefinido`.

**Revisión de consumidores**: `findHoldingsByOwner` solo se consume en `PortfolioServiceImpl` y en
`PortfolioPersistenceTest`; ninguna otra prueba del repositorio dependía del contrato anterior.

### Refactor de las proyecciones de consulta — 2026-10-21

Las proyecciones `HoldingQuantity` y `ReservedQuantity`, declaradas como interfaces anidadas de
`PlayerTokenRepository` y `TokenListingRepository`, se extrajeron a interfaces públicas
independientes en `backend/src/main/java/footballmarket/repositories/projections/`. Motivo: el
servicio y las pruebas los usaban mediante la qualification anidada del repositorio, una
referencia cruzada entre capas.

- **Sin cambios de comportamiento**: se conservan los tipos (`Integer` y `Long`),
  los aliases de consulta (`playerId`, `quantity`), los nombres de los accesores y el texto de las
  consultas.
- **Referencias actualizadas**: `PlayerTokenRepository`, `TokenListingRepository`,
  `PortfolioServiceImpl` y `PortfolioPersistenceTest`.
- **Artefactos alineados**: `plan.md` (árbol de archivos, decisión de estructura y tabla de
  archivos) y `docs/backend/architecture.md` §6.

### Fallos de la primera ejecución de pruebas — 2026-10-21

La primera ejecución completa (`./gradlew clean test`, 1203 pruebas, 5 fallos) dejó en rojo cinco
pruebas de `PortfolioServiceTest`. El análisis de los informes JUnit, los stack traces y las
fixtures separa **un defecto de producción** de **cuatro errores de test**:

| Prueba | Causa raíz | Tipo | Corrección |
| --- | --- | --- | --- |
| `posicionSinCotizacion` | `PortfolioServiceImpl.toPosition` combinaba `calculated.totalProfitLoss()` —anulable— con `calculated.realizedProfitLoss()` —primitivo— en un mismo ternario: el resultado era `long` y la rama anulable se desempaquetaba, con `NullPointerException` | **Producción** | El total se toma directamente de `calculated.totalProfitLoss()`, que ya distingue posición abierta sin cotización (nulo) de posición cerrada (realizado acumulado). No se desempaqueta y se conserva la nulidad que exige FR-021 |
| `ordenaSinCotizacionDespuesDeValoradas` | La misma Goldberg del caso anterior | **Producción** | Misma corrección |
| `divergenciaEntreTenenciaYReconstruccion` | `PortfolioTestData.breakHolding` insertaba una tenencia nueva para un par jugador y propietario que ya existía: `uk_player_tokens_player_owner` lo rechazaba antes de llegar a la afirmación | **Test** | La fixture sobrescribe la tenencia existente con `PlayerToken.restore` y su identificador, que es lo que la divergencia buscada requiere |
| `posicionEmitidaDelSuperusuario` | La prueba esperaba `averageUnitPrice` nulo, pero el precio medio derivado de una posición abierta es `costo de adquisición / cantidad abierta` —cero para la emisión inicial— y solo es nulo cuando la cantidad abierta es cero (posición cerrada), según R-05 y `PlayerFinancialPosition.averageUnitPrice()` | **Test** | La afirmación pasa a `isZero()`; la regla contable es de SPEC-015 y no se altera |
| `reservaTokensVigentes` | La publicación se creaba con fecha fija `2026-10-18` y un día de vigencia, mientras la consulta decide la vigencia contra el reloj real del mercado: la publicación ya había vencido y no reservaba | **Test** | El reloj del mercado se fija en la prueba —`@TestBean` sobre `marketClock`, patrón ya usado en las pruebas E2E— y la publicación se crea en un instante vigente bajo ese reloj |

**Reglas funcionales y contratos**: no se alteró ninguna regla funcional ni contractual. La
corrección de producción se limita a conservar la nulidad que el contrato ya exige (FR-021, R-05) y
delega el total en el componente contable que lo posee (CL-03, CL-39). No se debilitó ninguna
afirmación: la aserción de `averageUnitPrice` se corrigió porque la expectativa era incorrecta
respecto de SPEC-015, no para dejar pasar una prueba.

**Aislamiento entre pruebas**: el superusuario es un dato único (índice
`idx_users_single_superuser`) que acumula tenencias entre pruebas y entre clases que comparten
contenedor. Las afirmaciones sobre su portfolio se acotaron a la posición del jugador de la prueba
—por `playerId`— y la página se dimensiona para que ese jugador no caiga fuera de ella.

### Limitaciones registradas

- **El caso de borrado físico de un jugador no es alcanzable**: la integridad referencial de
  `market_transactions` lo impide, y el borrado en cascada de `player_tokens` arrastraría las
  tenencias. El comportamiento implemented —posición conservada con datos descriptivos nulos cuando
  el jugador no existe, y error de integridad cuando el estado sí es inconsistente— queda escrito en
  el código, pero solo su variante alcanzable —el jugador desactivado— es verificable hoy.
- **La lectura de los jugadores cubre el conjunto de posiciones, no solo la página**: el orden de
  las posiciones cerradas es por nombre de jugador, de modo que la comparación de nombres exige
  conocerlos antes de ordenar. Sigue siendo una única consulta de conjunto, sin acceso por posición.


- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Las tareas que consumen el componente contable se cierran con su implementación real, nunca con un doble: el comportamiento contable se acredita con la implementación real
- Las tres historias comparten `PortfolioServiceImpl`, `PortfolioPositionResponseDTO` y `PortfolioMapper`: sus tareas de implementación se ejecutan en secuencia, y solo los tests se escriben en paralelo
- Ninguna tarea de esta feature modifica `specs/015-define-average-cost-and-financial-results-rules/`, `docs/backend/architecture.md` (salvo el apartado propio de T045) ni el worktree de SPEC-015

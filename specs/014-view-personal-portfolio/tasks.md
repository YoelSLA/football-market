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
- **[BLOQUEADA: SPEC-015]**: Tarea cuya ejecución depende de que el componente contable de SPEC-015 exista. No se puede cerrar con un doble: el comportamiento contable solo se acredita con el componente real.

## Path Conventions

- **Web app**: `backend/src/`, `frontend/src/`
- Rutas, listas y contratos en `specs/014-view-personal-portfolio/`
- Comandos Gradle desde `backend/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificaciones preparatorias y decisiones que condicionan a todas las historias. No son entregables de código.

- [ ] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas: `BigDecimal` pertenece a la plataforma, `springdoc-openapi` ya está declarado y el generador de migraciones ya está registrado
- [ ] T002 [P] Confirmar la siguiente versión de migración disponible con `ls backend/src/main/resources/db/migration/` y que la nueva usará timestamp UTC según `docs/development/flyway-migrations.md`
- [ ] T003 [P] Verificar si `FinancialPositionService` de SPEC-015 está integrado y disponible en `backend/src/main/java/footballmarket/services/FinancialPositionService.java`; si no lo está, dejar constancia del bloqueo en `specs/014-view-personal-portfolio/tasks.md` y no simular ni sustituir el componente contable
- [ ] T004 [P] Verificar si `docs/backend/architecture.md` §1.3 documenta ya la relación Service → Service que habilita el consumo del componente contable; si no lo está, registrar la dependencia en `specs/014-view-personal-portfolio/tasks.md` sin modificar ese documento, que corresponde a SPEC-015
- [ ] T005 [P] Revisar `git status --short` y preservar los cambios locales ajenos antes de escribir
- [ ] T006 [P] Verificar que la fuente de cotizaciones vigente es la del mercado y que `QuoteSource` y `CurrentQuote` se consumen sin modificación en `backend/src/main/java/footballmarket/integrations/QuoteSource.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Piezas que TODAS las historias necesitan. Ninguna historia puede comenzar antes de que esta fase esté completa.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Crear la migración del índice por propietario con `./gradlew createMigration -PmigrationDescription=index_player_tokens_owner` desde `backend/` y escribir `CREATE INDEX idx_player_tokens_owner ON player_tokens (owner_user_id, player_id);` en el archivo generado, sin alterar tablas, restricciones ni índices existentes. Antes de integrarla, verificar que su versión no colisiona con la migración del índice de emisiones de SPEC-015 —`player_token_emissions (owner_user_id)`, aplicada en otra rama—: si ambas coinciden, resolver la numeración de forma explícita y sin modificar la migración de SPEC-015
- [ ] T008 [P] Implementar `PortfolioIntegrityException` en `backend/src/main/java/footballmarket/models/exceptions/PortfolioIntegrityException.java` heredando de `DomainException` conforme a `docs/backend/architecture.md` §1.6, para la divergencia entre la tenencia persistida y la reconstrucción contable
- [ ] T009 [P] Implementar `InvalidPortfolioPageException` en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioPageException.java` heredando de `DomainException`, para `page` negativa, `size` fuera de 1–100 o valor no entero
- [ ] T010 [P] Implementar `InvalidPortfolioStatusException` en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioStatusException.java` heredando de `DomainException`, para un `status` distinto de `OPEN` o `CLOSED`
- [ ] T011 Añadir los handlers en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`: `PortfolioIntegrityException` y `FinancialPositionIntegrityException` a 500 `INTERNAL_ERROR` genérico sin detalle técnico; `InvalidPortfolioPageException` e `InvalidPortfolioStatusException` a 400 `INVALID_PARAMETER_TYPE`, reutilizando el código vigente y distinguiendo el motivo en el mensaje. El fallo técnico de la fuente de cotizaciones MUST mapearse a 503 `QUOTE_SOURCE_UNAVAILABLE` capturando **la excepción de integración que propague el propio componente contable al no poder valorar**, identificada por su origen y por el momento de la valoración, y MUST NOT registrarse como un handler genérico de `IntegrationException`: ese handler alteraría el tratamiento vigente de las integraciones de otras features —`FOOTBALL_DATA_UNAVAILABLE` en 502, `HEADED_BROWSER_UNAVAILABLE` en 503 y las de WhoScored—. Si al integrar SPEC-015 el tipo lanzado no resulta distinguible, MUST ajustarse al tipo efectivamente lanzado en lugar de broadening el alcance, y la coordinación queda registrada en `specs/014-view-personal-portfolio/tasks.md`
- [ ] T012 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerTokenRepository.java` la consulta de las tenencias del propietario con su jugador y su cantidad, apoyada en el índice `idx_player_tokens_owner` de T007
- [ ] T013 [P] Añadir en `backend/src/main/java/footballmarket/repositories/TokenListingRepository.java` la consulta agrupada de las reservas del propietario por jugador, entre publicaciones `ACTIVE` y no vencidas, sin índice nuevo
- [ ] T014 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` la lectura por lote de los jugadores indicados con su equipo, sin filtrar por estado activo, para informar también los jugadores inactivos
- [ ] T015 Crear en `backend/src/main/java/footballmarket/models/records/PortfolioView.java` el record de vista con el resumen, las posiciones de la página y los metadatos de paginación, y **como record anidado dentro de él** el record de la posición de la vista con sus campos y su nulabilidad, según `data-model.md` §3. Es un único archivo, de modo que el inventario de archivos del plan no crece
- [ ] T016 Crear la interfaz `PortfolioService` en `backend/src/main/java/footballmarket/services/PortfolioService.java` con la operación de consulta del portfolio para un identificador de usuario y su documentación de contrato, con interfaz e implementación separadas conforme a `docs/backend/architecture.md` §1.1

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Consultar el resumen y las posiciones de mi portfolio (Priority: P1) 🎯 MVP

**Goal**: Un usuario autenticado consulta su portfolio y obtiene el resumen global y sus posiciones con las cifras contables, las cantidades y los datos descriptivos del jugador.

**Independent Test**: Con un usuario autenticado con posiciones en varios jugadores, con y sin cotización vigente, consultar su portfolio y comprobar la forma de la respuesta, las cantidades, las cifras y el resumen. No requiere ninguna operación de mercado ni el historial de transacciones.

### Tests para User Story 1 (backend; sin tests frontend)

> Escribir tests backend necesarios; resultados y ejecución quedan a cargo del usuario. MUST NOT ejecutar tests desde el agente.

- [ ] T017 [P] [US1] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/PortfolioPersistenceTest.java` con grupos `@Nested` que cubran la consulta de las tenencias del propietario, la agrupación de sus reservas vigentes excluyendo publicadas, canceladas, vendidas y vencidas, la lectura por lote de jugadores con su equipo incluyendo jugadores inactivos, y la presencia y uso del índice por propietario (SC-011, E16, E30, E31, E32, E33)
- [ ] T018 [P] [US1] Escribir tests de Service en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran la composición del portfolio, la identidad `quantity = reservedQuantity + availableQuantity`, la divergencia entre tenencia y reconstrucción con error de integridad, la ausencia de cotización con valoración incompleta y resultado realizado conservado, la propagación del fallo técnico de cotizaciones, el portfolio vacío con ceros, la coherencia de una respuesta bajo concurrencia y el valor reflejado de la cotización de la fuente, sin dependencia de un valor constante (SC-001, SC-002, SC-004, SC-012, E7, E8, E17, E18, E19, E24, E26, E29) [BLOQUEADA: SPEC-015 en los casos de cifras contables]
- [ ] T019 [P] [US1] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` con `@Tag("lightweight")` que cubran la ruta, la autenticación con cadena de seguridad real, el 401 sin invocar el Service, la presencia de los tres objetos de nivel superior y de los campos que US1 entrega —identificación del jugador, estado, cantidades, cifras y resumen—, y el rechazo sin efectos ante parámetros inválidos (SC-009, E1, E2, E5, E6). La correspondencia completa con el contrato, incluidos los porcentajes por posición que expone US3, se verifica en T036

### Implementation for User Story 1

- [ ] T020 [P] [US1] Crear `PortfolioSummaryResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioSummaryResponseDTO.java` con los trece indicadores globales del resumen y sus unidades, con las anotaciones de esquema
- [ ] T021 [P] [US1] Crear `PortfolioPositionResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java` con los campos de la posición y su nulabilidad, distinguiendo los importes enteros en centésimas de los porcentajes decimales
- [ ] T022 [P] [US1] Crear `PortfolioPaginationResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPaginationResponseDTO.java` con `page`, `size`, `totalElements` y `totalPages`
- [ ] T023 [P] [US1] Crear `PortfolioResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioResponseDTO.java` con `summary`, `positions` y `pagination` como objetos de nivel superior
- [ ] T024 [P] [US1] Crear `PortfolioMapper` en `backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java` con la traducción de la vista de dominio a los DTOs, sin cálculos ni lógica de negocio, con constructor privado y métodos estáticos
- [ ] T025 [US1] Implementar `PortfolioServiceImpl` en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java`: declarar la operación con `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`; obtener el conjunto completo de posiciones del componente contable y su agregación contable sobre ese conjunto completo; leer las tenencias y las reservas del usuario; cruzar ambas fuentes y detectar la divergencia; componer el resumen con los agregados y los conteos propios; paginar; y devolver la vista, sin recalcular ningún importe contable ni copiar regla alguna de REQ-S2-20 (AC07, AC08, AC12, AC14) [BLOQUEADA: SPEC-015]
- [ ] T026 [US1] Exponer `GET /api/portfolio/me` en `backend/src/main/java/footballmarket/controllers/PortfolioController.java` con las anotaciones de OpenAPI de la ruta, sus parámetros `page` y `size`, sus respuestas y sus códigos de error, resolviendo la identidad desde el JWT y sin aceptar ningún identificador de usuario del cliente
- [ ] T027 [US1] Verificar en `specs/014-view-personal-portfolio/contracts/portfolio-api.md` la correspondencia exacta campo por campo con la respuesta implementada y registrar el resultado; modificar el contrato solo si aparece una discrepancia real
- [ ] T028 [US1] Verificar que la consulta no altera operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos, y que no expone posiciones, operaciones ni resultados de otro usuario, y que cada operación confirmada cuenta una sola vez por rol (SC-009, SC-010, E6, E32, E34, E35)

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Filtrar y paginar las posiciones de mi portfolio (Priority: P2)

**Goal**: El usuario filtra sus posiciones por estado y las recorre por páginas, con un orden determinista y un resumen que no depende ni del filtro ni de la página.

**Independent Test**: Con un usuario que tenga posiciones abiertas y cerradas, consultar su portfolio con y sin filtro y con paginación explícita, y comprobar qué posiciones aparecen, en qué orden y que el resumen es idéntico. No requiere operaciones de mercado.

### Tests para User Story 2 (backend; sin tests frontend)

- [ ] T029 [P] [US2] Escribir tests de Service del filtro y de la paginación en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran el filtro `OPEN` y el filtro `CLOSED`, la paginación por defecto y la personalizada, la página fuera de rango y la independencia del resumen respecto del filtro y de la página, y el reflejo del cambio de estado entre dos consultas paginadas distintas (SC-003, E10, E12, E13, E15, E21, E28)
- [ ] T030 [P] [US2] Escribir tests de Controller del filtro y de la paginación en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` que cubran el rechazo de `status` inválido y de `page` o `size` fuera de rango con 400 `INVALID_PARAMETER_TYPE`, y la forma de los metadatos de paginación sobre el conjunto filtrado (E3, E4, E14)

### Implementation for User Story 2

- [ ] T031 [US2] Exponer el parámetro opcional `status` en `backend/src/main/java/footballmarket/controllers/PortfolioController.java`, admitiendo solo las formas exactas `OPEN` y `CLOSED`, sin conversión de mayúsculas ni tolerancia a valores desconocidos, y rechazando el resto con 400
- [ ] T032 [US2] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` la aplicación del filtro por estado, el orden determinista —abiertas con cotización por valor actual descendente, abiertas sin cotización y cerradas por nombre de jugador, con `playerId` como desempate— y la paginación sobre el conjunto filtrado, con los metadatos contando ese conjunto, la página fuera de rango respondiendo 200 con `positions` vacío y el resumen del portfolio completo (AC12, AC13, AC14, AC15, AC21, AC22, AC23, AC24, AC25)
- [ ] T033 [US2] Extender en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` la verificación de que dos consultas sobre el mismo estado devuelven la misma secuencia de posiciones y de que el resumen no depende del filtro ni de la página, con reloj controlado (SC-007, E14, E27)

**Checkpoint**: User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Consultar posiciones cerradas y el historial financiero acumulado (Priority: P3)

**Goal**: El usuario consulta también las posiciones que ya cerró, con su resultado realizado acumulado, su ciclo de tenencia, sus contadores y los porcentajes de rendimiento y peso.

**Independent Test**: Con un usuario que cerró una posición y la reabrió, y con el superusuario y su emisión inicial, consultar su portfolio y comprobar el estado de las posiciones, la acumulación del resultado realizado, las fechas, los contadores y los porcentajes. No requiere operaciones de mercado adicionales.

### Tests para User Story 3 (backend; sin tests frontend)

- [ ] T034 [P] [US3] Escribir tests de Service de las posiciones cerradas y del ciclo de tenencia en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran la posición cerrada con valor actual y resultado no realizado nulos y resultado total igual al realizado acumulado, el estado de valoración `NOT_APPLICABLE`, el cierre y la reapertura del ciclo, la acumulación del resultado realizado sin reinicio, y la nulabilidad de `lastTransactionAt` en una posición sin operaciones confirmadas (SC-005, E9, E11, E21, E22)
- [ ] T035 [P] [US3] Escribir tests de Service de los porcentajes en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` con grupos `@Nested` que cubran el rendimiento individual respecto del costo de adquisición de la posición, el peso porcentual sobre el valor total de todas las posiciones abiertas, la nulabilidad por denominador cero y por valor total indefinido, y el porcentaje global con el costo histórico total pagado como denominador (SC-013, SC-014, E22, E23)
- [ ] T036 [P] [US3] Escribir tests de Controller de la respuesta completa en `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` que cubran la presencia y la nulabilidad de todos los campos del contrato, incluidos los de las posiciones cerradas y los porcentajes nulos, y la correspondencia exacta con `contracts/portfolio-api.md`

### Implementation for User Story 3

- [ ] T037 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` la normalización de las posiciones cerradas —cotización, instante de cotización, valor actual y resultado no realizado nulos, resultado total igual al realizado acumulado y estado de valoración `NOT_APPLICABLE`— y la propagación del nulo de `lastTransactionAt` cuando la posición no tiene operaciones confirmadas (FR-013, FR-020, FR-022, FR-030, research R-05)
- [ ] T038 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` los porcentajes propios de la feature: el rendimiento no realizado de cada posición abierta respecto de su costo de adquisición y su peso porcentual sobre el valor total de todas las posiciones abiertas, con `HALF_UP` a dos decimales y nulos cuando su denominador es cero o el valor total no está disponible, sin recalcular ningún importe contable ni derivar el costo desde un precio medio redondeado (FR-028, FR-029, research R-03)
- [ ] T039 [US3] Exponer en `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java` y en `backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java` los porcentajes por posición, con su tipo decimal de dos decimales y su nulabilidad
- [ ] T040 [US3] Verificar en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` que la emisión inicial de tokens del superusuario aparece como posición abierta con costo de adquisición cero, sin incremento de los contadores de compras y con participación en la valoración (SC-014, E20, AC36) [BLOQUEADA: SPEC-015]
- [ ] T041 [US3] Verificar en `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` el caso del jugador que dejó de estar en el catálogo: la posición se conserva, su resultado financiero se preserva, los datos descriptivos irrecuperables se informan nulos y una inconsistencia que impida calcular el portfolio produce el error de integridad (FR-040, E24, E25, AC33) [BLOQUEADA: SPEC-015]

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificaciones y ajustes que afectan a varias historias.

- [ ] T042 [P] Verificar la coherencia entre `specs/014-view-personal-portfolio/contracts/portfolio-api.md`, `specs/014-view-personal-portfolio/data-model.md` y la respuesta implementada, y registrar el resultado
- [ ] T043 Ejecutar el control permitido `./gradlew build -x test` desde `backend/` y registrar el resultado; la ejecución de las pruebas queda a cargo del usuario
- [ ] T044 [P] Revisar que la implementación no introduce consultas por posición ni dependencias nuevas, y que el conteo de consultas es el previsto en `plan.md` §11, con la adquisición de cotizaciones encapsulada en el componente contable (FR-039, SC-011)
- [ ] T045 [P] Actualizar `docs/backend/architecture.md` con el apartado de los componentes de SPEC-014 solo si la actualización de la matriz de dependencias que corresponde a SPEC-015 ya está integrada, y sin duplicar su edición; si no lo está, dejar constancia de la dependencia sin tocar el documento
- [ ] T046 Revisar `specs/014-view-personal-portfolio/spec.md` y `specs/014-view-personal-portfolio/plan.md` contra lo implementado, registrar las discrepancias y corregirlas solo en su origen
- [ ] T047 [P] Revisar la coherencia de `specs/014-view-personal-portfolio/checklists/consistency.md` con el resultado de la implementación y actualizar su registro de hallazgos
- [ ] T048 Compartir con el usuario la evidencia de las pruebas: escenarios ejecutados de `specs/014-view-personal-portfolio/quickstart.md` con sus resultados, y dejar constancia expresa de los pendientes

---

## Tareas bloqueadas por SPEC-015

Estas tareas no pueden cerrarse hasta que el componente contable de SPEC-015 exista. **No se sustituyen
por un doble**: el comportamiento contable —costo de adquisición, resultados, porcentaje global,
agregados, ciclo de tenencia, recuentos por rol e instante de la cotización— solo se acredita con el
componente real. Son de ejecución secuencial, porque las pruebas de T018, T040 y T041 deben usar la
implementación real del componente.

| Tarea | Dependencia | Motivo |
| --- | --- | --- |
| T003 | — | Verificación previa: deja constancia del bloqueo si el componente no está integrado |
| T018 | `FinancialPositionService` real | Los casos de cifras, ausencia de cotización y fallo técnico no se acreditan con un doble |
| T025 | `FinancialPositionService` real | La composición consume el componente y su agregación contable |
| T040 | `FinancialPositionService` real | La emisión inicial y la ausencia de costo de adquisición provienen del componente |
| T041 | `FinancialPositionService` real | La reconstrucción contable de la posición es la que se contrasta con la tenencia |
| T004 y T045 | Actualización de `docs/backend/architecture.md` de SPEC-015 | La relación Service → Service debe estar documentada antes de considerarla autorizada |

Mientras el bloqueo siga vigente, **son ejecutables** T001 a T017, T019 a T024, T026 a T039 y T042 a T048: toda la consulta, su forma, sus errores, su seguridad, el filtro, el orden, la paginación y
los porcentajes propios no dependen del componente contable.

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

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Las tareas marcadas `[BLOQUEADA: SPEC-015]` no se cierran con un doble del componente contable: el comportamiento contable se acredita con la implementación real
- Las tres historias comparten `PortfolioServiceImpl`, `PortfolioPositionResponseDTO` y `PortfolioMapper`: sus tareas de implementación se ejecutan en secuencia, y solo los tests se escriben en paralelo
- Ninguna tarea de esta feature modifica `specs/015-define-average-cost-and-financial-results-rules/`, `docs/backend/architecture.md` (salvo el apartado propio de T045) ni el worktree de SPEC-015

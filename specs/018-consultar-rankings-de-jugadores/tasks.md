---

description: "Task list for feature implementation"
---

# Tasks: Consultar ranking de jugadores por score

**Input**: Design documents from `/specs/018-consultar-rankings-de-jugadores/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/score-ranking-http.md, quickstart.md

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend. Este feature es exclusivamente backend: no se crean tareas de frontend.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Checklists**: `checklists/requirements.md` y `checklists/api-security.md` están completamente evaluados por el revisor. CHK009 fue retirado sin reutilizar su identificador tras eliminar FR-015 y SC-008.

**Organization**: Tasks are grouped by user story. US1 implements the shared flow; US2 and US3 depend technically on US1, but their scenarios can be verified separately after that base flow exists.

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

**Purpose**: Confirmar que la feature reutiliza la infraestructura existente y que no requiere cambios de esquema, dependencias ni configuración.

- [X] T001 [P] Verificar en `backend/src/main/java/footballmarket/models/QuoteExecution.java`, `backend/src/main/java/footballmarket/models/PlayerQuote.java`, `backend/src/main/java/footballmarket/models/PositionalValuation.java`, `backend/src/main/java/footballmarket/models/AttributeValuation.java` y `backend/src/main/java/footballmarket/models/enums/ValuationStrategy.java` que las entidades, columnas, índices y enum reutilizados cubren `data-model.md` §1–§2; confirmar en `backend/src/main/resources/db/migration/` que no se crea ninguna migración nueva
- [X] T002 [P] Verificar en `backend/src/main/java/footballmarket/config/SecurityConfig.java` que la ruta nueva queda cubierta por `anyRequest().authenticated()` sin reglas ni roles adicionales (FR-016), y en `backend/build.gradle.kts` que no se requieren dependencias nuevas (research.md §10)

**Checkpoint**: infraestructura reutilizable confirmada; sin cambios de esquema ni dependencias.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelos de salida y consultas de persistencia compartidos por las tres historias.

**⚠️ CRITICAL**: No puede iniciarse ninguna historia hasta completar esta fase.

- [X] T003 Crear el record `ScoreRanking` en `backend/src/main/java/footballmarket/models/records/ScoreRanking.java` con `strategy` (`ValuationStrategy`, nulable), `quoteDate` (`LocalDate`, nulable) y `entries` (`List<ScoreRankingEntry>`, no nulo, vacía si no hay entradas), incorporando una invariante de construcción que rechace `entries == null`; realizar después de T004 (data-model.md §3.1)
- [X] T004 [P] Crear el record `ScoreRankingEntry` en `backend/src/main/java/footballmarket/models/records/ScoreRankingEntry.java` con `position` (`Integer`), `playerId` (`Long`), `score` (`BigDecimal`) y `valuationDate` (`LocalDate`), sin incluir el identificador de la valuación (FR-007, FR-008, FR-017, data-model.md §3.2)
- [X] T005 [P] Añadir a `backend/src/main/java/footballmarket/repositories/QuoteExecutionRepository.java` la consulta derivada `findTopByOrderByQuoteDateDesc()` que devuelve la última ejecución persistida o vacío (FR-002, FR-010, data-model.md §4)
- [X] T006 [P] Añadir a `backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java` la consulta JPQL `findScoreRanking()`: valuación de mayor `valuationDate` por jugador mediante `not exists` de una fila con fecha posterior, población restringida con `exists` sobre `PlayerQuote`, y orden final `finalScore DESC, player.id ASC` (FR-003, FR-004, FR-006, FR-007, data-model.md §2.3–§2.4, §4)
- [X] T007 [P] Añadir a `backend/src/main/java/footballmarket/repositories/AttributeValuationRepository.java` la consulta JPQL `findScoreRanking()` con las mismas reglas de selección, población y orden que T006, usando `finalGrl` como score (FR-003, FR-004, FR-006, FR-007, data-model.md §1.4, §2.3–§2.4, §4)

**Checkpoint**: modelos y consultas listos; las historias pueden empezar.

---

## Phase 3: User Story 1 - Consultar el ranking por score de la última ejecución (Priority: P1) 🎯 MVP

**Goal**: Exponer `GET /api/players/ranking/score` con el encabezado de la última ejecución persistida y una entrada por jugador elegible, ordenada por score descendente y desempatada por `playerId` ascendente.

**Independent Test**: Con una última ejecución persistida, jugadores con cotización vigente y valuaciones de su estrategia, comprobar el encabezado (`strategy`, `quoteDate`), la población incluida, el score persistido, la fecha de la valuación usada y el orden con posiciones consecutivas. Incluir una valuación posterior a `quoteDate` y verificar además `401` sin JWT.

### Tests para User Story 1 (backend según normas; sin tests frontend)

> Escribir tests backend necesarios; resultados y ejecución quedan a cargo del usuario. MUST NOT ejecutar tests desde el agente.

- [X] T008 [P] [US1] Escribir el test de Repository en `backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java` con `@Tag("integration")` y `@Nested`/`@DisplayName` en español: última ejecución por fecha, valuación de mayor fecha por jugador —incluida una posterior a `quoteDate`—, exclusión de jugadores sin cotización, orden por score y `playerId`, y empate de dos jugadores (FR-002, FR-003, FR-004, FR-006 a FR-008, research.md §12)
- [X] T009 [P] [US1] Escribir el test de Service en `backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java` con `@Tag("integration")` y `@SpringBootTest` usando la interfaz `ScoreRankingQueryService`: encabezado con estrategia y fecha de la última ejecución, población elegible, selección de la valuación más reciente aunque sea posterior a `quoteDate`, empate de dos jugadores, posiciones 1..n y repetibilidad del resultado. La comparación integral del estado persistido para SC-007 queda en la validación manual de `quickstart.md` §7, porque no es observable mediante la frontera permitida del Service (FR-002 a FR-004, FR-006 a FR-008, FR-012)
- [X] T010 [P] [US1] Escribir el test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/ScoreRankingControllerTest.java` con `@Tag("lightweight")` y `@WebMvcTest`: `200` con el JSON exacto de `contracts/score-ranking-http.md` §1.2–§1.2.1 (campos, tipos, nulos y ausencia de campos extra), `401` sin JWT y con JWT inválido, y una llamada con query parameters no declarados que confirme que se ignoran sin alterar la respuesta ni los argumentos enviados al Service (FR-001, FR-008, FR-009, FR-016, FR-017)

### Implementation for User Story 1

- [X] T012 [US1] Crear la interfaz `ScoreRankingQueryService` en `backend/src/main/java/footballmarket/services/ScoreRankingQueryService.java` con `ScoreRanking findScoreRanking()` y su Javadoc de contrato (solo lectura, sin parámetros) (FR-001, FR-013)
- [X] T013 [US1] Crear `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java`: `@Service` con `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`, lectura de la última ejecución, despacho según `ValuationStrategy` a la consulta `findScoreRanking()` correspondiente, mapeo a `ScoreRanking`/`ScoreRankingEntry` conservando el orden de la consulta y asignando `position = índice + 1`, listas vacías explícitas cuando no exista ejecución o no haya valuaciones, y registro `debug` de la lectura; sin limitar valuaciones por `quoteDate` ni recalcular scores o precios (FR-002 a FR-004, FR-006 a FR-008, FR-010 a FR-012, research.md §2, §4, §5, §8)
- [X] T014 [P] [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/ScoreRankingResponseDTO.java` (`strategy`, `quoteDate`, `ranking`) y `backend/src/main/java/footballmarket/controllers/dtos/responses/ScoreRankingEntryResponseDTO.java` (`position`, `playerId`, `score`, `valuationDate`) con anotaciones `@Schema` que reflejen tipos, nulabilidad y semántica del contrato (FR-008, FR-009, data-model.md §3.3)
- [X] T015 [P] [US1] Crear `backend/src/main/java/footballmarket/controllers/mappers/ScoreRankingMapper.java` con el mapeo estático Model → DTO, copiando `position` y la lista no nula garantizada por `ScoreRanking` sin cálculos, lógica de negocio, reordenamiento ni fallback de `null` a lista vacía (FR-007, FR-008, data-model.md §3.1–§3.3)
- [X] T016 [US1] Crear `backend/src/main/java/footballmarket/controllers/ScoreRankingController.java` con `GET /api/players/ranking/score`, `@RestController`, `@RequestMapping("/api/players")`, `@Tag` propio, `@SecurityRequirement(name = "bearerAuth")`, anotaciones `@Operation`/`@ApiResponse` (`200`, `401`, `500`) y delegación en `ScoreRankingQueryService` (FR-001, FR-016, contracts/score-ranking-http.md §1, §2)

**Checkpoint**: User Story 1 implementa y permite verificar el flujo base; constituye el MVP técnico, no un cierre parcial de la feature.

---

## Phase 4: User Story 2 - Profundizar la resolución de empates de score (Priority: P2)

**Goal**: Garantizar que empates múltiples y consultas repetidas producen siempre el mismo orden, las mismas posiciones y la misma valuación elegida por jugador.

**Independent Test**: Con al menos tres jugadores empatados en un score y otro grupo con score distinto, comprobar el orden dentro de cada grupo, el orden entre grupos, la continuidad de las posiciones y la identidad de las respuestas en consultas repetidas.

### Tests para User Story 2 (backend según normas; sin tests frontend)

- [X] T017 [P] [US2] Ampliar `backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java` con empates múltiples por score, varios grupos de empate, posiciones consecutivas sin compartirse y repetibilidad estricta de dos consultas sobre los mismos datos (FR-006, FR-007, US2 escenarios 1–3, SC-001, SC-002)
- [X] T018 [P] [US2] Ampliar `backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java` con el orden total ante empates múltiples para ambas estrategias y resultados idénticos en ejecuciones repetidas (FR-006, research.md §5, §12)

### Implementation for User Story 2

- [X] T019 [US2] Asegurar en `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java` que el orden de las consultas es la única fuente del ranking, que no se aplica ningún ordenamiento adicional ni dependiente del estado de instancia y que el Service asigna la posición como índice + 1; verificar en `backend/src/main/java/footballmarket/controllers/mappers/ScoreRankingMapper.java` que solo la copia al DTO (FR-006, FR-007, research.md §5)

**Checkpoint**: los escenarios de US2 son verificables separadamente sobre el flujo base de US1; no constituyen un entregable ni un cierre independiente.

---

## Phase 5: User Story 3 - Consultar el ranking cuando no hay información disponible (Priority: P3)

**Goal**: Devolver una respuesta válida y vacía cuando no existen ejecuciones o cuando la última ejecución no produjo valuaciones de su estrategia, sin inventar posiciones ni scores.

**Independent Test**: Consultar primero sin ejecuciones de cotización y luego con una ejecución cuya estrategia no tiene valuaciones persistidas, comprobando en ambos casos `200`, el encabezado esperado y `ranking: []`.

### Tests para User Story 3 (backend según normas; sin tests frontend)

- [X] T020 [P] [US3] Ampliar `backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java` con ausencia total de ejecuciones (estrategia y fecha nulas, entradas vacías), ejecución sin valuaciones de su estrategia (encabezado no nulo, entradas vacías) y ejecución con todos los jugadores arrastrados (FR-010, FR-011, US3 escenarios 1–3, SC-006)
- [X] T021 [P] [US3] Ampliar `backend/src/test/java/footballmarket/controllers/ScoreRankingControllerTest.java` con el JSON exacto de las respuestas vacías: `strategy: null`, `quoteDate: null`, `ranking: []` y encabezado no nulo con `ranking: []`, sin campos adicionales (FR-010, FR-011, SC-006, SC-010)
- [X] T022 [US3] Después de T021, añadir a `backend/src/test/java/footballmarket/controllers/ScoreRankingControllerTest.java` el caso de fallo técnico del Service: `500 INTERNAL_ERROR` con la forma de `ErrorResponseDTO`, sin clasificación parcial ni detalles internos, y sin exponer identificadores de valuación ni la versión de estrategia (FR-014, FR-017, SC-010, contracts/score-ranking-http.md §1.4)

### Implementation for User Story 3

- [X] T023 [US3] Ajustar en `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java` el tratamiento de ausencia de datos: encabezado nulo y lista vacía explícita solo cuando no existe ejecución; encabezado no nulo y lista vacía explícita cuando la estrategia no tiene valuaciones; ninguna posición inventada. Verificar que `backend/src/main/java/footballmarket/controllers/mappers/ScoreRankingMapper.java` reciba y copie la lista no nula sin normalizar estados inválidos (FR-010, FR-011, data-model.md §3.1, §5)

**Checkpoint**: los escenarios de US3 son verificables separadamente sobre el flujo base de US1; las tres historias integran una única feature.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia de contrato, documentación, garantía de solo lectura y control entregable.

- [X] T024 [P] Revisar las anotaciones OpenAPI de `backend/src/main/java/footballmarket/controllers/ScoreRankingController.java` y de los DTO en `backend/src/main/java/footballmarket/controllers/dtos/responses/` para que coincidan con `specs/018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md` (Constitution §IV, docs/backend/testing.md §8)
- [X] T026 [P] Confirmar que no hay cambios de inventario tecnológico ni de esquema: revisar `docs/backend/technologies.md`, `docs/backend/data-model.md` y `backend/src/main/resources/db/migration/`, y no registrar deuda técnica sin evidencia (Constitution §IV, plan.md §Project Structure)
- [X] T027 Ejecutar el control entregable permitido `./gradlew build -x test` desde `backend/` y reportar el resultado observado al usuario; no ejecutar tests. Indicar los comandos de testing pendientes del usuario (`./gradlew lightweightTest` y `./gradlew integrationTest`) conforme a `quickstart.md` §9
- [X] T028 [P] Revisar en `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java` y en las consultas añadidas por T005–T007 que el caso de uso solo invoque operaciones de lectura, use `@Transactional(readOnly = true)` y no contenga `save`, `delete`, `flush` ni consultas de actualización; mantener la comparación integral del estado como validación manual del usuario en `quickstart.md` §7 (FR-013, SC-007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; puede iniciarse de inmediato.
- **Foundational (Phase 2)**: Depende de Setup; BLOQUEA todas las historias.
- **User Stories (Phase 3+)**: Dependen de Foundational.
  - US1 es el núcleo y debe completarse antes de US2 y US3 para que sus tests ejerciten el endpoint real.
  - US2 y US3 pueden ejecutarse en paralelo entre sí después de US1 cuando la capacidad lo permita.
- **Polish (Phase 6)**: Depende de las historias que se decidan entregar.

### User Story Dependencies

- **User Story 1 (P1)**: Depende de Foundational; no depende de US2 ni US3.
- **User Story 2 (P2)**: Depende de Foundational y de la implementación de US1 (T012–T016); profundiza el desempate y la repetibilidad del mismo endpoint.
- **User Story 3 (P3)**: Depende de Foundational y de la implementación de US1 (T012–T016); cubre los estados sin datos del mismo endpoint.

### Within Each User Story

- Escribir los tests backend necesarios; su ejecución y evidencia corresponden al usuario.
- Modelos antes que servicios; servicios antes que Controller.
- Implementación núcleo antes de la integración y del pulido.
- US1 completa antes de avanzar a US2 o US3.

### Parallel Opportunities

- T001 y T002 pueden ejecutarse en paralelo.
- T004, T005, T006 y T007 pueden ejecutarse en paralelo dentro de Foundational (archivos distintos); T003 depende de T004.
- Los tests T008–T010 pueden escribirse en paralelo (archivos distintos).
- T014 y T015 pueden ejecutarse en paralelo; T016 depende de T012–T015.
- T017 y T018 pueden ejecutarse en paralelo; T019 depende de la implementación de US1.
- T020 y T021 pueden ejecutarse en paralelo; T022 modifica el mismo archivo que T021 y se realiza después de ella; T023 depende de la implementación de US1.
- T024, T026 y T028 pueden ejecutarse en paralelo; T027 depende de todo el código de las historias entregadas y de T024, T026 y T028.

---

## Parallel Example: User Story 1

```bash
# Escribir tests backend de US1; no ejecutarlos desde el agente:
Task: "Escribir test de Repository en backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java"
Task: "Escribir test de Service en backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java"
Task: "Escribir test de contrato HTTP en backend/src/test/java/footballmarket/controllers/ScoreRankingControllerTest.java"

# Implementar en paralelo los DTO y el Mapper de US1:
Task: "Crear backend/src/main/java/footballmarket/controllers/dtos/responses/ScoreRankingResponseDTO.java y ScoreRankingEntryResponseDTO.java"
Task: "Crear backend/src/main/java/footballmarket/controllers/mappers/ScoreRankingMapper.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational (CRÍTICA: bloquea las historias).
3. Completar Phase 3: User Story 1.
4. **STOP y validar**: gates permitidos y evidencia disponible; los tests quedan a cargo del usuario.
5. No iniciar US2 ni US3 sin autorización del alcance correspondiente.

### Incremental Delivery

1. Setup + Foundational → base lista.
2. Implementar US1 → checkpoint técnico del flujo base.
3. Implementar US2 y US3 sobre US1 → checkpoints técnicos de sus escenarios, sin cierres parciales.
4. Completar pulido, documentación, tests escritos y control entregable del alcance autorizado.
5. Implementación completa de SPEC-018 → CODE REVIEW humano consolidado → TESTING humano consolidado → CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE.

READY y IMPLEMENT AUTHORIZATION no implican inicio. Los gates humanos no son Tasks técnicas. Build y entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

Con más de un desarrollador:

1. El equipo completa Setup + Foundational en conjunto.
2. Al terminar Foundational:
   - Developer A: User Story 1 (núcleo y MVP).
   - Developer B: User Story 2 (después de la implementación de US1).
   - Developer C: User Story 3 (después de la implementación de US1, en paralelo con US2).
3. US2 y US3 se integran sobre US1 y sus escenarios se verifican separadamente.

---

## Notes

- `[P]` tasks = archivos distintos, sin dependencias pendientes.
- `[Story]` vincula cada tarea con su user story para trazabilidad.
- US1 implementa el flujo base; US2 y US3 dependen técnicamente de él, aunque sus escenarios se verifican separadamente.
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos.
- Commit después de cada tarea o grupo lógico, conforme a `docs/development/git-workflow.md`.
- Evitar tareas vagas y conflictos sobre el mismo archivo; mantener explícita la dependencia de US2 y US3 respecto de US1.
- La ejecución de `./gradlew build -x test` solo verifica el build; no acredita tests ni revisión.

## Phase 7: Convergence

- [X] T029 Añadir en `backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java` un escenario con varias ejecuciones de cotización en fechas distintas y afirmar que `findTopByOrderByQuoteDateDesc()` devuelve la de mayor `quoteDate`, completando la cobertura de selección del encabezado per FR-002, T008 (partial)

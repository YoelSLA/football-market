---

description: "Task list for feature implementation"
---

# Tasks: Consultar ranking de jugadores por score

**Input**: Design documents from `/specs/018-consultar-rankings-de-jugadores/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/score-ranking-http.md, quickstart.md

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend. Este feature es exclusivamente backend: no se crean tareas de frontend.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze sin blockers. MUST conservar IDs/avance al reanudar.

**Checklists**: `checklists/requirements.md` y `checklists/api-security.md` corresponden al alcance anterior. Para el delta clarificado el 2026-11-12, `checklists/minimum-scope.md` fue revisado a solicitud explícita del usuario; CHK001–CHK009 están marcados como cubiertos. La decisión posterior del usuario precisa la elegibilidad conforme a FR-022: alguna fila de la fecha más reciente debe tener métricas. Esta anotación no sustituye Analyze.

> **Vigencia:** Phase 1–7 y T001–T031 son el registro histórico del alcance anterior, no describen el contrato ni trabajo ejecutable del delta 2026-11-12. Sus casillas `[X]` preservan evidencia histórica y no significan que las tareas pendientes del delta estén completadas ni autorizan IMPLEMENT. La lista vigente para el delta es T032–T050; T030 y T031 permanecen retiradas y no se reutilizan.

**Organization histórica**: las Tasks anteriores se agruparon por User Story; el delta vigente se organiza en Phase 8–12.

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

## Phase 1 histórica — Setup del alcance anterior (no vigente para el delta)

**Purpose**: Confirmar que la feature reutiliza la infraestructura existente y que no requiere cambios de esquema, dependencias ni configuración.

- [X] T001 [P] Verificar en `backend/src/main/java/footballmarket/models/QuoteExecution.java`, `backend/src/main/java/footballmarket/models/PlayerQuote.java`, `backend/src/main/java/footballmarket/models/PositionalValuation.java`, `backend/src/main/java/footballmarket/models/AttributeValuation.java` y `backend/src/main/java/footballmarket/models/enums/ValuationStrategy.java` que las entidades, columnas, índices y enum reutilizados cubren `data-model.md` §1–§2; confirmar en `backend/src/main/resources/db/migration/` que no se crea ninguna migración nueva
- [X] T002 [P] Verificar en `backend/src/main/java/footballmarket/config/SecurityConfig.java` que la ruta nueva queda cubierta por `anyRequest().authenticated()` sin reglas ni roles adicionales (FR-016), y en `backend/build.gradle.kts` que no se requieren dependencias nuevas (research.md §10)

**Checkpoint**: infraestructura reutilizable confirmada; sin cambios de esquema ni dependencias.

---

## Phase 2 histórica — Foundational (no vigente para el delta)

**Purpose**: Modelos de salida y consultas de persistencia compartidos por las tres historias.

**⚠️ CRITICAL**: No puede iniciarse ninguna historia hasta completar esta fase.

- [X] T003 Crear el record `ScoreRanking` en `backend/src/main/java/footballmarket/models/records/ScoreRanking.java` con `strategy` (`ValuationStrategy`, nulable), `quoteDate` (`LocalDate`, nulable) y `entries` (`List<ScoreRankingEntry>`, no nulo, vacía si no hay entradas), incorporando una invariante de construcción que rechace `entries == null`; realizar después de T004 (data-model.md §3.1)
- [X] T004 [P] Crear el record `ScoreRankingEntry` en `backend/src/main/java/footballmarket/models/records/ScoreRankingEntry.java` con `position` (`Integer`), `playerId` (`Long`), `score` (`BigDecimal`) y `valuationDate` (`LocalDate`), sin incluir el identificador de la valuación (FR-007, FR-008, FR-017, data-model.md §3.2)
- [X] T005 [P] Añadir a `backend/src/main/java/footballmarket/repositories/QuoteExecutionRepository.java` la consulta derivada `findTopByOrderByQuoteDateDesc()` que devuelve la última ejecución persistida o vacío (FR-002, FR-010, data-model.md §4)
- [X] T006 [P] Añadir a `backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java` la consulta JPQL `findScoreRanking()`: valuación de mayor `valuationDate` por jugador mediante `not exists` de una fila con fecha posterior, población restringida con `exists` sobre `PlayerQuote`, y orden final `finalScore DESC, player.id ASC` (FR-003, FR-004, FR-006, FR-007, data-model.md §2.3–§2.4, §4)
- [X] T007 [P] Añadir a `backend/src/main/java/footballmarket/repositories/AttributeValuationRepository.java` la consulta JPQL `findScoreRanking()` con las mismas reglas de selección, población y orden que T006, usando `finalGrl` como score (FR-003, FR-004, FR-006, FR-007, data-model.md §1.4, §2.3–§2.4, §4)

**Checkpoint**: modelos y consultas listos; las historias pueden empezar.

---

## Phase 3 histórica — User Story 1 del contrato anterior (no vigente para el delta)

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

## Phase 4 histórica — User Story 2 anterior (no vigente para el delta)

**Goal**: Garantizar que empates múltiples y consultas repetidas producen siempre el mismo orden, las mismas posiciones y la misma valuación elegida por jugador.

**Independent Test**: Con al menos tres jugadores empatados en un score y otro grupo con score distinto, comprobar el orden dentro de cada grupo, el orden entre grupos, la continuidad de las posiciones y la identidad de las respuestas en consultas repetidas.

### Tests para User Story 2 (backend según normas; sin tests frontend)

- [X] T017 [P] [US2] Ampliar `backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java` con empates múltiples por score, varios grupos de empate, posiciones consecutivas sin compartirse y repetibilidad estricta de dos consultas sobre los mismos datos (FR-006, FR-007, US2 escenarios 1–3, SC-001, SC-002)
- [X] T018 [P] [US2] Ampliar `backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java` con el orden total ante empates múltiples para ambas estrategias y resultados idénticos en ejecuciones repetidas (FR-006, research.md §5, §12)

### Implementation for User Story 2

- [X] T019 [US2] Asegurar en `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java` que el orden de las consultas es la única fuente del ranking, que no se aplica ningún ordenamiento adicional ni dependiente del estado de instancia y que el Service asigna la posición como índice + 1; verificar en `backend/src/main/java/footballmarket/controllers/mappers/ScoreRankingMapper.java` que solo la copia al DTO (FR-006, FR-007, research.md §5)

**Checkpoint**: los escenarios de US2 son verificables separadamente sobre el flujo base de US1; no constituyen un entregable ni un cierre independiente.

---

## Phase 5 histórica — User Story 3 anterior (no vigente para el delta)

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

## Phase 6 histórica — Polish del alcance anterior (no vigente para el delta)

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

## Phase 7 histórica — Convergence del alcance anterior (no vigente para el delta)

- [X] T029 Añadir en `backend/src/test/java/footballmarket/repositories/ScoreRankingQueryTest.java` un escenario con varias ejecuciones de cotización en fechas distintas y afirmar que `findTopByOrderByQuoteDateDesc()` devuelve la de mayor `quoteDate`, completando la cobertura de selección del encabezado per FR-002, T008 (partial)

---

## Delta vigente clarificado 2026-11-12 — tareas pendientes

Las tareas T001–T029 y sus estados se conservan como evidencia del alcance anterior. Las tareas siguientes cubren únicamente el delta de paginación, filtros, identidad de jugador y obtención del podio. La elegibilidad aplica FR-003 y FR-022: cotización vigente, valuación aplicable y alguna estadística de la fecha más reciente con `present_metrics > 0`. Tests backend se escriben conforme a `docs/backend/testing.md`; su ejecución corresponde al usuario.

### Phase 8: Setup y prerrequisitos compartidos

- [X] T049 [P] Verificar los contratos reutilizables en `backend/src/main/java/footballmarket/features/quote/domain/records/QuoteRankingQuery.java`, `backend/src/main/java/footballmarket/features/quote/presentation/dtos/request/QuoteRankingQueryRequestDTO.java`, `backend/src/main/java/footballmarket/features/quote/infraestructure/PlayerQuoteRepository.java` y `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteRankingPageResponseDTO.java`; confirmar en `backend/src/main/resources/db/migration/V20261107030025__create_positional_valuations.sql` y `backend/src/main/resources/db/migration/V20261107030026__create_attribute_valuations.sql` que no se requiere cambio de esquema (FR-018–FR-019).
- [X] T050 [P] Añadir `ScoreRankingQuery`, `ScoreRankingRow`, `ScoreRankingFilterOptions` y sus records de opciones en `backend/src/main/java/footballmarket/features/ranking/domain/records/`, respetando las validaciones y nulabilidad de `contracts/score-ranking-http.md` (FR-018–FR-020).
- [X] T032 Extender `backend/src/main/java/footballmarket/features/quote/infraestructure/QuoteExecutionRepository.java` para seleccionar la ejecución finalizada por `startedAt DESC, id DESC`, excluyendo las filas con `finished_at IS NULL`; ajustar `backend/src/main/java/footballmarket/features/ranking/application/ScoreRankingQueryService.java` para usar el selector nuevo (FR-002).

### Phase 9: User Story 1 - Consultar ranking paginado y filtrado (Priority: P1) 🎯 MVP

**Goal**: Consultar `GET /api/rankings/score` con filtros y paginación backend, respuesta identificatoria y posiciones globales.

**Independent Test**: Con una población elegible de más de una página, comprobar el contrato paginado, los filtros AND, el conteo filtrado y que un filtro encuentre un jugador fuera de la primera página conservando su posición global.

**Verificación pendiente del usuario:** los tests de Repository, Service y Controller escritos para el delta no fueron ejecutados ni compilados por el agente. El build sin tests no acredita su resultado ni TESTING.

#### Tests backend para User Story 1

- [X] T033 [P] [US1] Añadir en `backend/src/test/java/footballmarket/features/ranking/infraestructure/ScoreRankingQueryTest.java` cobertura de SQL para ambas estrategias: última valuación por jugador, elegibilidad por cotización y alguna estadística de la fecha más reciente con `present_metrics > 0` (incluidas fuentes con y sin métricas en la misma fecha), filtros AND, wildcard literal, opciones globales, total filtrado, página acotada y posición global anterior al filtrado (FR-003–FR-005, FR-018–FR-019, FR-022, SC-011–SC-013).
- [X] T034 [P] [US1] Añadir en `backend/src/test/java/footballmarket/features/ranking/application/ScoreRankingQueryServiceTest.java` cobertura de normalización de filtros, delegación de consulta paginada, totales, opciones globales y composición de respuesta desde la ejecución finalizada y las filas de la estrategia seleccionada (FR-002–FR-004, FR-006, FR-018–FR-019, FR-022, SC-011, SC-013–SC-015).
- [X] T035 [P] [US1] Añadir en `backend/src/test/java/footballmarket/features/ranking/presentation/ScoreRankingControllerTest.java` assertions del contrato exacto `GET /api/rankings/score`: defaults, parámetros documentados, serialización de respuestas con contenido y vacías (incluidos `content`, `page`, `size`, `totalElements`, `totalPages` y `filterOptions`), `strategy`/`quoteDate` nullable, validación `page`/`size`, rechazo previo al service de parámetros inválidos (verificando que no se invoque), y JWT requerido (FR-006–FR-007, FR-018–FR-020, SC-014–SC-015, SC-017; junto con T045 verifica el contrato 500).

#### Implementación de User Story 1

- [X] T036 [US1] Crear el request DTO y mapearlo al modelo de consulta en `backend/src/main/java/footballmarket/features/ranking/presentation/dtos/request/` y `backend/src/main/java/footballmarket/features/ranking/presentation/ScoreRankingMapper.java`, reusando las reglas de `QuoteRankingQueryRequestDTO` y `QuoteRankingQuery` sin ampliar parámetros (FR-018).
- [X] T037 [US1] Añadir proyecciones de ranking y consultas SQL nativas paginadas en `backend/src/main/java/footballmarket/features/valuation/infraestructure/PositionalValuationRepository.java` y `backend/src/main/java/footballmarket/features/valuation/infraestructure/AttributeValuationRepository.java`: ranking global score DESC/ID ASC, elegibilidad, filtros, offset/limit, conteo filtrado y opciones globales; no materializar el ranking completo (FR-003–FR-005, FR-018–FR-019, FR-022, SC-011–SC-013).
- [X] T038 [US1] Actualizar `backend/src/main/java/footballmarket/features/ranking/application/ScoreRankingQueryService.java` y `backend/src/main/java/footballmarket/features/ranking/application/impl/ScoreRankingQueryServiceImpl.java` para aceptar la consulta paginada, seleccionar estrategia/fecha desde la última ejecución finalizada y componer resultados de solo lectura coherentes por respuesta (FR-002–FR-004, FR-007, FR-018–FR-019, SC-014).
- [X] T039 [US1] Actualizar los DTO en `backend/src/main/java/footballmarket/features/ranking/presentation/dtos/response/` y `backend/src/main/java/footballmarket/features/ranking/presentation/ScoreRankingMapper.java` para exponer `content`, página, tamaño, totales, opciones y los datos identificatorios permitidos del jugador sin IDs internos ni valores inventados (FR-006, FR-019–FR-020, SC-014).
- [X] T040 [US1] Actualizar `backend/src/main/java/footballmarket/features/ranking/presentation/ScoreRankingController.java` a `GET /api/rankings/score`, documentando parámetros, validaciones, respuesta y errores con anotaciones OpenAPI, manteniendo JWT y sin crear rutas alternativas (FR-001, FR-007, FR-018, SC-014, SC-017).
- [X] T048 [US1] Revisar `backend/src/main/java/footballmarket/features/ranking/application/impl/ScoreRankingQueryServiceImpl.java`, `backend/src/main/java/footballmarket/features/valuation/infraestructure/PositionalValuationRepository.java` y `backend/src/main/java/footballmarket/features/valuation/infraestructure/AttributeValuationRepository.java`: confirmar que el Service es de solo lectura y que las consultas son exclusivamente de lectura, sin escrituras ni recálculo. Verificar además que T033 (`backend/src/test/java/footballmarket/features/ranking/infraestructure/ScoreRankingQueryTest.java`) y T034 (`backend/src/test/java/footballmarket/features/ranking/application/ScoreRankingQueryServiceTest.java`) comprueban los resultados del flujo nuevo sin eludir las fronteras de test de Repository y Service definidas en `docs/backend/testing.md` (FR-004, FR-007; escenario transversal vigente de spec.md).

### Phase 10: User Story 2 - Mantener posiciones globales y obtener podio (Priority: P2)

**Goal**: Preservar orden determinista y posiciones globales al filtrar/paginar; obtener Top 3 y total global con la misma ruta sin filtros.

**Independent Test**: Con empates y varios grupos de score, verificar orden score DESC/ID ASC, posiciones globales sin renumeración tras filtrar y que `page=0&size=3` sin filtros devuelve posiciones 1–3 y total global.

- [X] T041 [P] [US2] Ampliar `backend/src/test/java/footballmarket/features/ranking/infraestructure/ScoreRankingQueryTest.java` con empates múltiples y comprobaciones para ambas estrategias de que `row_number()` se calcula globalmente antes de filtros y paginación; verificar que las opciones no varían por los filtros (FR-005, FR-018–FR-019, SC-011–SC-012).
- [X] T042 [P] [US2] Ampliar `backend/src/test/java/footballmarket/features/ranking/application/ScoreRankingQueryServiceTest.java` con consultas sin filtros `page=0&size=3`, comprobando posiciones 1–3 y `totalElements` global, además de estabilidad del orden determinista (FR-005, FR-021, SC-012, SC-016).
- [X] T043 [US2] Verificar en `backend/src/main/java/footballmarket/features/valuation/infraestructure/PositionalValuationRepository.java`, `backend/src/main/java/footballmarket/features/valuation/infraestructure/AttributeValuationRepository.java` y `backend/src/main/java/footballmarket/features/ranking/application/impl/ScoreRankingQueryServiceImpl.java` que posiciones, paginación y podio usan el mismo orden global y que los filtros no alteran `filterOptions` (FR-005–FR-006, FR-019, FR-021).

### Phase 11: User Story 3 - Responder estados sin datos y errores (Priority: P3)

**Goal**: Devolver encabezado y página vacía válidos cuando falten ejecuciones, valuaciones o coincidencias, y respetar errores seguros.

**Independent Test**: Cubrir ausencia de ejecución finalizada, ejecución finalizada sin valuaciones aplicables, filtros sin coincidencias y página fuera de rango; confirmar respuestas 200 vacías con totales y opciones contractuales, además de 400/401/500 para los errores definidos.

- [X] T044 [P] [US3] Ampliar `backend/src/test/java/footballmarket/features/ranking/application/ScoreRankingQueryServiceTest.java` para ausencia de ejecución finalizada, estrategia sin valuaciones aplicables, filtro sin coincidencias y página fuera de rango; verificar encabezados nullable y que contenido, totales y opciones respeten FR-018–FR-019 (sin ejecución o sin valuaciones: total cero/opciones vacías; sin coincidencias: totales filtrados en cero/opciones globales; fuera de rango: totales filtrados reales/opciones globales; SC-014–SC-015).
- [X] T045 [P] [US3] Ampliar `backend/src/test/java/footballmarket/features/ranking/presentation/ScoreRankingControllerTest.java` para filtro sin coincidencias, página fuera de rango, `400 INVALID_REQUEST`, `401` y `500 INTERNAL_ERROR`, afirmando JSON exacto sin detalles internos ni respuesta parcial (FR-007, FR-018–FR-020, SC-015, SC-017).
- [X] T046 [US3] Completar el manejo de respuestas vacías y errores del contrato en `backend/src/main/java/footballmarket/features/ranking/application/impl/ScoreRankingQueryServiceImpl.java` y `backend/src/main/java/footballmarket/features/ranking/presentation/ScoreRankingController.java`, sin convertir ausencia de datos en error ni exponer información sensible (FR-007, FR-018–FR-019).

### Phase 12: Polish contractual

- [X] T047 [P] Durante IMPLEMENT, alinear `specs/018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md`, las anotaciones OpenAPI del Controller/DTO y los tests de Controller con el contrato delta; comprobar referencias afectadas de `specs/022-view-player-rankings/` y documentar la coordinación contractual sin implementar la feature frontend (FR-001, FR-006–FR-007, FR-018–FR-022).

### Dependencias y paralelismo del delta

- Setup T032, T049 y T050 precede las historias; T049 y T050 pueden hacerse en paralelo. T048 verifica la implementación de T037–T038 y la cobertura definida por T033–T034, por lo que depende de las cuatro tareas.
- US1: T033–T035 son paralelizables en archivos distintos; T036 precede T037–T040 cuando compartan modelos o contratos; T038 depende de las consultas T037; T039 y T040 consumen el modelo/servicio de T038.
- US2 depende de la base de US1; T041 y T042 pueden hacerse en paralelo; T043 verifica la implementación existente.
- US3 depende de la base de US1; T044 y T045 pueden hacerse en paralelo; T046 incorpora los ajustes que indiquen las pruebas.
- T047 depende de los cambios de respuesta/ruta de US1 y de las pruebas de contrato. La coordinación con SPEC-022 comprueba referencias/compatibilidad de consumidor, pero no depende de completar el flujo SDD de SPEC-022 ni autoriza trabajo frontend. El MVP es US1; US2 y US3 completan la cobertura requerida del delta.

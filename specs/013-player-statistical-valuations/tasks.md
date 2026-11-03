---

description: "Tareas de implementación de valoraciones estadísticas de jugadores"
---

# Tasks: Valoraciones estadísticas de jugadores

**Input**: Documentos de diseño de `/specs/013-player-statistical-valuations/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/http.md` y `quickstart.md`

**Tests**: La implementación backend requiere tests Model, Repository, Service, Controller, Security y Config conforme a `docs/backend/testing.md`. El usuario ejecutará los tests; el agente no debe ejecutarlos.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Estas Tasks viven exclusivamente en este archivo, no se duplican en el seguimiento operativo y no autorizan IMPLEMENT. Después de Tasks corresponde ANALYZE.

**Organization**: Las tareas se agrupan por User Story para que cada estrategia y la operación poblacional tengan criterios verificables propios.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede realizarse en paralelo porque afecta archivos distintos y no depende de una tarea incompleta.
- **[Story]**: User Story de `spec.md` cubierta por la tarea.
- Todas las descripciones incluyen rutas exactas.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Definir la configuración operativa y sus pruebas antes de construir el pipeline.

- [X] T001 [P] Crear tests de binding, valores iniciales y rechazo de umbrales o impactos inválidos en `backend/src/test/java/footballmarket/config/ValuationConfigurationTest.java`
- [X] T002 Implementar las properties validadas de mínimos de minutos e impactos máximos en `backend/src/main/java/footballmarket/config/ValuationProperties.java`
- [X] T003 Registrar `ValuationProperties` como configuración Spring en `backend/src/main/java/footballmarket/config/ValuationConfig.java`
- [X] T004 Declarar los valores iniciales de valuación sin agregar dependencias ni pesos editables en `backend/src/main/resources/application.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Materializar persistencia, tipos compartidos y pipeline común requerido por ambas estrategias.

**⚠️ CRITICAL**: Ninguna User Story puede implementarse hasta completar esta fase.

- [X] T005 [P] Crear tests de migración, constraints, nulabilidad por posición, unicidad e índices de ambas tablas en `backend/src/test/java/footballmarket/repositories/PlayerValuationMigrationTest.java`
- [X] T006 [P] Crear tests Model del fallback JSON/SCRAPING, cero válido, posición general, derivaciones, medianas, percentiles de rango medio, confianza, clamp y redondeo en `backend/src/test/java/footballmarket/models/ValuationMetricPipelineTest.java`
- [X] T007 Crear las tablas `player_positional_valuations` y `player_attribute_valuations` con FKs, arrays, checks, unicidad e índices de `data-model.md` en `backend/src/main/resources/db/migration/V20261019120000__create_player_valuations.sql`
- [X] T008 [P] Crear los enums de estrategia, posición, procedencia y exclusión en `backend/src/main/java/footballmarket/models/enums/ValuationStrategy.java`, `backend/src/main/java/footballmarket/models/enums/GeneralPosition.java`, `backend/src/main/java/footballmarket/models/enums/MetricSourceSummary.java` y `backend/src/main/java/footballmarket/models/enums/ValuationExclusionReason.java`
- [X] T009 [P] Crear los records internos de candidato, grupo de normalización, métricas preparadas y resumen de evaluación en `backend/src/main/java/footballmarket/models/records/ValuationCandidate.java`, `backend/src/main/java/footballmarket/models/records/ValuationPopulationKey.java`, `backend/src/main/java/footballmarket/models/records/PreparedValuationMetrics.java` y `backend/src/main/java/footballmarket/models/records/ValuationEvaluationSummary.java`
- [X] T010 Implementar fallback, procedencia agregada, conjunto requerido por posición y exclusión priorizada en `backend/src/main/java/footballmarket/models/ValuationMetricAssembler.java`
- [X] T011 Implementar derivación, imputación, percentil de rango medio, confianza, clamp y persistencia decimal a seis posiciones en `backend/src/main/java/footballmarket/models/ValuationMetricPipeline.java`

**Checkpoint**: El esquema y las reglas compartidas permiten implementar cada estrategia sin duplicar selección, normalización ni precisión.

---

## Phase 3: User Story 1 - Evaluar el rendimiento por posición (Priority: P1) 🎯 MVP

**Goal**: Calcular y representar `POSITIONAL_PERFORMANCE` v1 por posición, liga y temporada.

**Independent Test**: Con candidatos de las cuatro posiciones, comprobar fórmulas, poblaciones aisladas, fallback, cero válido, disciplina, confianza y ausencia de penalización de rendimiento para arqueros sin utilizar la estrategia de atributos.

### Tests para User Story 1

- [X] T012 [P] [US1] Crear tests Model de las fórmulas posicionales, intervalos interpretativos, ajustes, confianza y nulabilidad de arquero en `backend/src/test/java/footballmarket/models/PositionalValuationCalculatorTest.java`
- [X] T013 [P] [US1] Crear tests de persistencia de componentes reconstruibles, redondeo, reemplazo y consultas ordenadas de la estrategia posicional en `backend/src/test/java/footballmarket/repositories/PositionalValuationPersistenceTest.java`

### Implementation for User Story 1

- [X] T014 [US1] Implementar la entidad con invariantes y componentes de `POSITIONAL_PERFORMANCE` v1 en `backend/src/main/java/footballmarket/models/PositionalValuation.java`
- [X] T015 [P] [US1] Implementar las fórmulas deterministas de delanteros, mediocampistas, defensores y arqueros en `backend/src/main/java/footballmarket/models/PositionalValuationCalculator.java`
- [X] T016 [US1] Crear el acceso de persistencia, reemplazo y consultas indexadas de valoraciones posicionales en `backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java`

**Checkpoint**: US1 produce una valoración posicional completa y persistible sin depender de US2 ni del endpoint de US3.

---

## Phase 4: User Story 2 - Obtener atributos generales y GRL (Priority: P2)

**Goal**: Calcular y representar `GENERAL_ATTRIBUTES` v1 con atributos y GRL específicos para campo y arquero.

**Independent Test**: Comprobar SHO/PAS/DRI/DEF/PHY para campo, SAV/DIS/ACT para arqueros, nulabilidad exclusiva, aislamiento de poblaciones, disciplina, confianza y GRL final entre 0 y 1.

### Tests para User Story 2

- [X] T017 [P] [US2] Crear tests Model de atributos, penalizaciones, pesos GRL por posición, disciplina, confianza y nulabilidad exclusiva en `backend/src/test/java/footballmarket/models/AttributeValuationCalculatorTest.java`
- [X] T018 [P] [US2] Crear tests de persistencia de atributos reconstruibles, checks, redondeo, reemplazo y consultas ordenadas en `backend/src/test/java/footballmarket/repositories/AttributeValuationPersistenceTest.java`

### Implementation for User Story 2

- [X] T019 [US2] Implementar la entidad con invariantes y componentes de `GENERAL_ATTRIBUTES` v1 en `backend/src/main/java/footballmarket/models/AttributeValuation.java`
- [X] T020 [P] [US2] Implementar SHO/PAS/DRI/DEF/PHY, SAV/DIS/ACT y GRL por posición en `backend/src/main/java/footballmarket/models/AttributeValuationCalculator.java`
- [X] T021 [US2] Crear el acceso de persistencia, reemplazo y consultas indexadas de valoraciones de atributos en `backend/src/main/java/footballmarket/repositories/AttributeValuationRepository.java`

**Checkpoint**: US2 produce la segunda estrategia completa sin depender del endpoint y sin convertir el GRL a cotización.

---

## Phase 5: User Story 3 - Ejecutar y auditar una evaluación poblacional (Priority: P3)

**Goal**: Seleccionar la última campaña confirmada, publicar ambas estrategias por jugador y exponer el resumen síncrono autenticado.

**Independent Test**: Invocar dos veces `POST /api/valuations/evaluate` en la misma fecha UTC y verificar respuesta exacta, snapshot confirmado, publicación conjunta, exclusiones, imputaciones, continuidad ante fallos, limpieza de obsoletos, idempotencia y seguridad.

### Tests para User Story 3

- [X] T022 [P] [US3] Crear tests Repository de selección de la última campaña JSON confirmada y carga de filas JSON/SCRAPING del mismo jugador y fecha en `backend/src/test/java/footballmarket/repositories/ValuationSnapshotQueryTest.java`
- [X] T023 [P] [US3] Crear tests Service del recorrido poblacional, orden de exclusiones, publicación conjunta, limpieza, idempotencia, resumen, continuidad ante fallos individuales, éxito cuando todos los jugadores fallan después de iniciado y completado el recorrido y fallo global cuando el recorrido no puede completarse en `backend/src/test/java/footballmarket/services/ValuationEvaluationServiceTest.java`
- [X] T024 [P] [US3] Crear tests Controller de método, ruta, ausencia de entrada, respuesta exacta, respuesta 200 cuando todos los jugadores fallan después de completarse el recorrido y errores 400/409/500 para los fallos globales contractuales en `backend/src/test/java/footballmarket/controllers/ValuationControllerTest.java`
- [X] T025 [P] [US3] Crear tests Security de JWT ausente, inválido y usuario no persistido, y acceso autenticado equivalente a SPEC-012 en `backend/src/test/java/footballmarket/security/ValuationAuthorizationTest.java`

### Implementation for User Story 3

- [X] T026 [US3] Extender las consultas de cobertura confirmada y snapshots de la misma fecha para la valuación en `backend/src/main/java/footballmarket/repositories/PlayerStatisticsExtractionRunRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRunItemRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRepository.java`
- [X] T027 [US3] Implementar selección de campaña, carga de candidatos y construcción de poblaciones sin llamadas externas en `backend/src/main/java/footballmarket/services/internal/ValuationSnapshotSelector.java` y `backend/src/main/java/footballmarket/services/internal/ValuationPopulationPreparer.java`
- [X] T028 [US3] Implementar publicación transaccional por jugador, reemplazo coherente de ambas estrategias y limpieza ante obsolescencia o fallo en `backend/src/main/java/footballmarket/services/internal/ValuationResultPublisher.java`
- [X] T029 [US3] Definir la fachada pública del caso de uso poblacional en `backend/src/main/java/footballmarket/services/ValuationEvaluationService.java`
- [X] T030 [US3] Implementar el recorrido, fecha UTC fija, conteos, continuidad con logging saneado y traducción de fallos globales en `backend/src/main/java/footballmarket/services/impl/ValuationEvaluationServiceImpl.java`
- [X] T031 [P] [US3] Crear las excepciones controladas para snapshot no disponible, fallo global e input HTTP prohibido en `backend/src/main/java/footballmarket/services/exceptions/ValuationSnapshotUnavailableException.java`, `backend/src/main/java/footballmarket/services/exceptions/ValuationEvaluationException.java` y `backend/src/main/java/footballmarket/controllers/exceptions/InvalidValuationRequestException.java`
- [X] T032 [US3] Crear el response record y mapper estático del resumen, con anotaciones OpenAPI de campos en `backend/src/main/java/footballmarket/controllers/dtos/responses/ValuationEvaluationResponseDTO.java` y `backend/src/main/java/footballmarket/controllers/mappers/ValuationMapper.java`
- [X] T033 [US3] Exponer y documentar en OpenAPI `POST /api/valuations/evaluate`, rechazando body o query antes del Service en `backend/src/main/java/footballmarket/controllers/ValuationController.java`
- [X] T034 [US3] Traducir `VALUATION_SNAPSHOT_UNAVAILABLE`, `VALUATION_EVALUATION_FAILED` e `INVALID_REQUEST` al contrato común de error en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`

**Checkpoint**: Las tres User Stories quedan integradas por una única fachada, con contrato HTTP completo y sin scheduler, lecturas, ranking ni cotización.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Mantener documentación arquitectónica y validación trazable del alcance transversal.

- [X] T035 [P] Documentar componentes, responsabilidades, persistencia y contrato de SPEC-013 en `docs/backend/architecture.md`
- [X] T036 Revisar y sincronizar rutas, campos, errores y escenarios de validación humana posteriores a implementación en `specs/013-player-statistical-valuations/contracts/http.md` y `specs/013-player-statistical-valuations/quickstart.md`

---

## Phase 7: Corrección de confirmación de campaña y posiciones (2026-10-20)

**Purpose**: Desbloquear la evaluación poblacional con los datos reales del catálogo y del proveedor, sin exigir cobertura total de la población ni aceptar posiciones fuera del conjunto persistido.

- [X] T037 Amendar FR-004, FR-058, FR-009 y las aclaraciones de `spec.md` con el criterio de confirmación por campaña terminada y el conjunto de posiciones persistido por el catálogo
- [X] T038 Ajustar la confirmación de campaña en `backend/src/main/java/footballmarket/services/internal/ValuationSnapshotSelector.java` con los estados terminales, los fallos estructurales y la fila JSON de cada item confirmado
- [X] T039 Contar como `MISSING_OR_UNCONFIRMED_SNAPSHOT` a los jugadores elegibles sin fila JSON confirmada en `backend/src/main/java/footballmarket/services/internal/ValuationPopulationPreparer.java`
- [X] T040 Traducir las posiciones persistidas del catálogo a la posición general cerrada en `backend/src/main/java/footballmarket/models/enums/GeneralPosition.java`
- [X] T041 Añadir las consultas de apoyo de la confirmación en `backend/src/main/java/footballmarket/repositories/PlayerStatisticsExtractionRunRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRunItemRepository.java`
- [X] T042 Documentar la decisión y sus alternativas en `specs/013-player-statistical-valuations/research.md`
- [X] T043 Eliminar el código muerto creado por la feature: consultas de lectura sin uso en `backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java`, `backend/src/main/java/footballmarket/repositories/AttributeValuationRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRunItemRepository.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Setup y bloquea las tres User Stories.
- **US1 (Phase 3)** y **US2 (Phase 4)**: Dependen de Foundational; pueden desarrollarse en paralelo.
- **US3 (Phase 5)**: Depende de Foundational y de los resultados implementados por US1 y US2 para publicar ambas estrategias.
- **Polish (Phase 6)**: Depende de las User Stories incluidas en el alcance autorizado.

### User Story Dependencies

```text
Setup → Foundational ─┬→ US1 ─┐
                     └→ US2 ─┴→ US3 → Polish
```

- **US1 (P1)**: Independiente de US2 y US3 después de Foundational.
- **US2 (P2)**: Independiente de US1 y US3 después de Foundational.
- **US3 (P3)**: Integra las dos estrategias; requiere US1 y US2 para cumplir la publicación conjunta.

### Within Each User Story

- Escribir primero los tests backend que expresan el comportamiento; el usuario los ejecuta.
- Implementar Model antes de Repository y Service.
- Preparar selección y poblaciones antes de publicar resultados.
- Implementar Service antes de Controller.
- Mantener OpenAPI, DTO, errores y tests Controller coherentes con `contracts/http.md`.

### Parallel Opportunities

- T001 puede prepararse mientras se define la configuración productiva de T002–T004, sin ejecutar sus tests.
- T005 y T006 cubren fronteras diferentes y pueden escribirse en paralelo.
- T008 y T009 afectan tipos compartidos distintos y pueden realizarse en paralelo después del esquema definido.
- US1 y US2 pueden implementarse en paralelo tras Foundational.
- En US1, T012 y T013 pueden escribirse en paralelo; T015 puede avanzar en paralelo con T014 cuando el contrato del constructor ya esté acordado.
- En US2, T017 y T018 pueden escribirse en paralelo; T020 puede avanzar en paralelo con T019 bajo el mismo criterio.
- En US3, T022–T025 pueden escribirse en paralelo, y T031 puede implementarse en paralelo con la preparación interna T027–T030.
- T035 puede prepararse en paralelo con la revisión contractual T036 una vez estabilizado el alcance implementado.

---

## Parallel Example: User Story 1

```text
Task T012: tests de fórmulas en backend/src/test/java/footballmarket/models/PositionalValuationCalculatorTest.java
Task T013: tests de persistencia en backend/src/test/java/footballmarket/repositories/PositionalValuationPersistenceTest.java
```

## Parallel Example: User Story 2

```text
Task T017: tests de atributos en backend/src/test/java/footballmarket/models/AttributeValuationCalculatorTest.java
Task T018: tests de persistencia en backend/src/test/java/footballmarket/repositories/AttributeValuationPersistenceTest.java
```

## Parallel Example: User Story 3

```text
Task T022: tests de snapshot en backend/src/test/java/footballmarket/repositories/ValuationSnapshotQueryTest.java
Task T024: tests HTTP en backend/src/test/java/footballmarket/controllers/ValuationControllerTest.java
Task T025: tests de seguridad en backend/src/test/java/footballmarket/security/ValuationAuthorizationTest.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Setup.
2. Completar Foundational.
3. Implementar y revisar US1.
4. Detenerse para la evidencia y gates humanos aplicables; el usuario ejecuta los tests.

US1 es el MVP técnico de cálculo posicional, pero no expone todavía la operación HTTP poblacional, que pertenece a US3 y requiere ambas estrategias.

### Incremental Delivery

1. Setup + Foundational establecen configuración, esquema y pipeline común.
2. US1 agrega la estrategia posicional verificable aisladamente.
3. US2 agrega la estrategia de atributos verificable aisladamente.
4. US3 integra ambas en publicación coherente y contrato HTTP.
5. Polish sincroniza la documentación afectada sin ampliar el alcance.

### Parallel Team Strategy

1. Completar Setup y Foundational respetando sus dependencias internas.
2. Tras Foundational, un responsable puede abordar US1 y otro US2.
3. Iniciar US3 cuando ambas estrategias tengan contratos estables y completar su integración después de US1 y US2.
4. No interpretar `[P]` como autorización de delegación ni como permiso para solapar archivos.

---

## Notes

- Las Tasks no incluyen ejecución de tests, hooks, builds ni escenarios manuales; la evidencia de tests corresponde al usuario.
- No crear tests frontend ni infraestructura frontend: SPEC-013 es exclusivamente backend.
- No agregar scheduler, endpoints de lectura, ranking, estrategia activa, conversión a precio, extracción ni llamadas externas.
- Marcar una Task completada solo cuando su trabajo esté implementado y verificado conforme al workflow.
- Generar este archivo no autoriza IMPLEMENT; la siguiente operación obligatoria es `/speckit.analyze` en modo read-only.


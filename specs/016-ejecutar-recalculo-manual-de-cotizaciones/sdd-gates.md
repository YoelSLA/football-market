# Registro local de gates SDD — SPEC-016

Este archivo aplica la contingencia local de `docs/development/sdd-workflow.md` §12 mientras GitHub
Projects no está configurado. Registra evidencia histórica sin acreditar por sí mismo revisión de
código, testing ni cierre de la funcionalidad. Es append-only: los eventos previos no se reescriben
ni se borran.

**Estado del seguimiento operativo**: no sincronizado. GitHub Projects continúa sin configurar y
ninguna funcionalidad ha sido migrada (§12). No se afirma sincronización ni se declaran
funcionalidades migradas.

## CHECKLIST — 2026-10-20

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: CHECKLIST (obligatoria; general de calidad más especializados aplicables).
- **Resultado**: revisión de criterios aplicables registrada. Habilita el paso a TASKS.
- **Responsable del registro**: agente, por instrucción expresa del revisor. No constituye
  aprobación de implementación, revisión de código, testing ni cierre.
- **Checklist evaluado**: `checklists/cotizacion.md`, 19 criterios de calidad de requisitos
  (modelo de re-ajuste y control de variabilidad; persistencia, idempotencia e historial; consultas
  y contrato HTTP). Los 19 quedan marcados por el revisor tras la revisión de esta sesión.
  `checklists/requirements.md` conserva su ciclo de vida propio, mantenido por `/speckit.specify` y
  `/speckit.clarify`, y no fue modificado en esta operación.
- **Artefactos evaluados**: `spec.md`, `plan.md`, `research.md`, `data-model.md`,
  `contracts/quotes-http.md`, `contracts/quote-source.md` y `quickstart.md`.
- **Modo de revisión**: assisted review a petición expresa del revisor («podés verificar cuáles
  están para tachar»). Los marcadores se aplicaron con esa autorización explícita y son
  reversibles por el revisor.

### Decisiones humanas registradas

- **Decisión 1 — eliminación del requisito de eficiencia**. Evidencia textual: «Para CHK019, ignorar
  la eficiencia, no se pide en ninguna parte de parte mía, así que si la FR-030 la pide elimínala».
  Resultado: FR-030 y FR-065 reescritos sin exigencia de eficiencia ni de objetivos de rendimiento
  propios; `research.md` §18 y `plan.md` alineados.
- **Decisión 2 — orden de precedencia de rechazos**. Evidencia textual: «Para CHK018 muy bien, sería
  primero estrategia, que no haya mercado inicializado, k fuera de rango y por último el parámetro».
  Resultado: nuevo FR-066 con orden único y determinista; propagado a `research.md` §15,
  `contracts/quotes-http.md` §2 y §9 y `quickstart.md`; nuevo código de error
  `EXECUTION_ALREADY_EXISTS` (409) documentado en spec, contrato, research y plan.
- **Decisión 3 — una sola ejecución de cotización por fecha de cotización**. Evidencia textual:
  «esto es algo que jamás aclara, pero no debería haber recálculos en la misma fecha» y la selección
  expresa de la opción A ante el ejemplo trabajado del jugador sin valuación con fila arrastrada.
  Resultado: FR-060, FR-026, FR-027, FR-061 y SC-004 reescritos; FR-027 aplica la condición de
  posterioridad estricta también a las filas arrastradas; `research.md` §5 y §7 actualizados;
  `data-model.md` incorpora `uq_quote_executions_date`; contrato HTTP y `quickstart.md` alineados.
  Consecuencia documentada como caso límite: un jugador con fallo individual se reprecia al día
  siguiente, no dentro de la misma fecha de cotización.
- **Autorización de registro**: «Okey, registrar la revisión por favor en donde creas conveniente».

### Criterios que no pudieron cerrarse sin decisión y su resolución

- CHK009 (contradicción entre `research.md` §5 y FR-027 en el caso de la fila arrastrada): cerrado
  con la Decisión 3.
- CHK011 (ambigüedad de la idempotencia, de los contadores y de la respuesta ante reejecución):
  cerrado con la Decisión 3.
- CHK018 (ausencia de orden de precedencia entre causas de rechazo concurrentes): cerrado con la
  Decisión 2.
- CHK019 (eficiencia de las consultas no verificable): cerrado con la Decisión 1.

### Verificaciones y alcance de esta operación

- Cambios exclusivamente documentales: ningún archivo de código, migración, contrato OpenAPI ni
  configuración fue modificado.
- Tests y builds: no ejecutados. El agente tiene prohibido ejecutar tests (Constitución,
  «Verificación y testing»).
- Revisión de rutas, referencias cruzadas (`FR-`, `SC-`, secciones de `research.md`, contratos) y
  coherencia entre spec, plan, research, data-model, contratos, quickstart y checklist.
- Git: sin commits ni push en esta operación. `specs/assignments.md` figuraba modificado en el
  árbol de trabajo por trabajo previo y ajeno a esta operación; se preservó sin alterar.

### Estado de autorizaciones

- **IMPLEMENT**: no autorizada. Requiere autorización humana explícita y vigente sobre el alcance.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

### Siguiente acción permitida

- `/speckit.tasks` para descomponer el alcance en tareas preservando IDs y cobertura de FR y SC.
- `/speckit.analyze` en modo estrictamente de lectura para contrastar spec, plan, diseño, Tasks y
  checklists antes de cualquier autorización de implementación.

## TASKS — 2026-10-20

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: TASKS (descomposición del alcance en tareas técnicas).
- **Resultado**: `tasks.md` generado con 50 tareas (`T001`–`T050`) en 8 fases: Setup, Foundational,
  cinco fases de User Story (US1, US2, US3, US4, US5) y Polish. Habilita el paso a ANALYZE.
- **Cobertura**: tareas agrupadas por historia con criterio de prueba independiente, rutas exactas
  bajo `backend/`, tareas de tests backend y sin infraestructura de tests frontend, y ninguna tarea
  de ejecución de tests por parte del agente.
- **Artefactos leídos**: `spec.md`, `plan.md`, `research.md`, `data-model.md`,
  `contracts/quotes-http.md`, `contracts/quote-source.md`, `quickstart.md`, `checklists/`,
  `sdd-gates.md`, `constitution.md` y `docs/development/sdd-workflow.md`.
- **Modo**: generación de tareas; no se escribió código, ni migraciones, ni configuración.
- **Tests y builds**: no ejecutados. El agente tiene prohibido ejecutar tests.
- **Git**: sin commits ni push en esta operación.

### Estado de autorizaciones

- **IMPLEMENT**: no autorizada. Requiere autorización humana explícita y vigente sobre el alcance.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

### Siguiente acción permitida

- `/speckit.analyze` en modo estrictamente de lectura para contrastar spec, plan, diseño, Tasks y
  checklists antes de cualquier autorización de implementación.

## ANALYZE — 2026-10-21

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: ANALYZE (modo estrictamente de lectura).
- **Resultado**: informe de análisis con 16 hallazgos: 1 CRITICAL, 3 HIGH, 8 MEDIUM, 4 LOW. El
  hallazgo CRITICAL (I1) detectaba que la fila `CARRIED_FORWARD`, al tomar la fecha de cotización como
  fecha de vigencia, bloqueaba permanentemente todo reprecio posterior del jugador y dejaba sin
  efecto el ajuste siguiente al cambio de estrategia (FR-009). Bloqueante para la implementación.
- **Artefactos contrastados**: `spec.md`, `plan.md`, `research.md`, `data-model.md`,
  `contracts/quotes-http.md`, `contracts/quote-source.md`, `quickstart.md`, `tasks.md`,
  `constitution.md` y `docs/development/sdd-workflow.md`.
- **Modo**: solo lectura. No se modificó ningún archivo en esta fase ni se ejecutaron tests o builds.
- **Siguiente acción permitida**: corrección de artefactos, que requiere autorización humana
  explícita y una nueva ejecución de ANALYZE.

## REMEDIACIÓN — 2026-10-21

- **Autorización**: instrucción expresa del usuario: «dejo que apliques tu recomendación. Para I1 e I2
  la opción A», sobre el set de remediación propuesto tras el ANALYZE.
- **Alcance**: corrección de los hallazgos I1–I16 en los artefactos de la feature. Decisiones
  adoptadas: I1 Opción A (coexistencia de una fila `RECOMPUTED` y una `CARRIED_FORWARD` con la misma
  fecha de vigencia, con prevalencia de la repreciada, y fecha de vigencia de referencia tomada solo
  de las filas `INITIAL` y `RECOMPUTED`); I2 Opción (a) (los parámetros de la ejecución se registran
  en el resumen persistido, sin duplicarlos en la fila); I13 sin renumerar, porque renumerar
  FR-064/FR-065/FR-066 invalidaría las referencias de la evidencia histórica de este archivo y de los
  registros previos (Constitución §V).
- **Cambios por artefacto**:
  - `spec.md`: nueva sesión de aclaraciones (2026-10-21); FR-003, FR-012, FR-013, FR-015, FR-024,
    FR-026, FR-027, FR-028, FR-029, FR-033, FR-037, FR-038, FR-059, FR-060 y FR-061 reescritos;
    escenario 2 y 4 de US2 precisados y escenario 11 añadido; casos límite y supuestos ampliados;
    SC-003 precisado.
  - `research.md`: §1, §2, §5, §6, §8, §13, §15 y §16 actualizados, incluida la razón de fondo del
    bloqueo de reprecio y las alternativas descartadas.
  - `data-model.md`: unicidad `UNIQUE (player_id, effective_date, origin)`, índice con desempate,
    invariantes de vigencia y precio anterior, contadores, consultas y reutilización de
    `ValuationStrategy`.
  - `contracts/quotes-http.md`: rango de `variationCap`, definición de contadores, prevalencia a
    igualdad de fecha de vigencia, concurrencia y códigos de error.
  - `plan.md`: recuento de alcance y estructura corregidos, rango del tope y unicidad por origen.
  - `tasks.md`: T001, T002, T006, T008, T012, T013, T014, T017, T021, T022, T023, T024, T025, T026,
    T028, T031, T036, T041, T042 y T043 actualizados. IDs, fases y avance preservados; ninguna tarea
    marcada.
  - `quickstart.md`: casos funcionales y de error, y fila de SC-016 en la tabla de criterios.
- **Naturaleza**: cambios exclusivamente documentales. Ningún archivo de código, migración,
  configuración ni contrato OpenAPI fue modificado.
- **Tests y builds**: no ejecutados. El agente tiene prohibido ejecutar tests (Constitución,
  «Verificación y testing»).
- **Git**: sin commits ni push en esta operación. `specs/assignments.md` figuraba modificado en el
  árbol de trabajo por trabajo previo y ajeno a esta operación; se preservó sin alterar.

### Puntos que requieren reevaluación del revisor

- `checklists/cotizacion.md` es propiedad del revisor y no se ha modificado. Los criterios CHK005,
  CHK009, CHK010, CHK011, CHK012 y CHK014 se apoyan en FR-012, FR-024, FR-026, FR-027, FR-060 y
  FR-061, que han cambiado. Su reevaluación corresponde al revisor; el agente no modifica sus
  marcadores.
- `checklists/requirements.md` mantiene su ciclo de vida propio y no se ha modificado.

### Estado de autorizaciones

- **IMPLEMENT**: no autorizada. Requiere autorización humana explícita y vigente sobre el alcance.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

### Siguiente acción permitida

- `/speckit.analyze` en modo estrictamente de lectura para contrastar los artefactos corregidos.

## ANALYZE — 2026-10-21 (posterior a la remediación)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: ANALYZE (modo estrictamente de lectura), ejecutado sobre los artefactos ya corregidos por
  la remediación del 2026-10-21.
- **Artefactos contrastados**: `spec.md` (FR-001 a FR-066, SC-001 a SC-016, aclaraciones de las dos
  sesiones), `plan.md`, `research.md` §1–§19, `data-model.md` §1–§5,
  `contracts/quotes-http.md` §1–§10, `contracts/quote-source.md`, `quickstart.md`, `tasks.md`
  (T001–T050), `checklists/cotizacion.md`, `checklists/requirements.md`, `constitution.md` y
  `docs/development/sdd-workflow.md`.
- **Resultado**: sin hallazgos CRITICAL ni HIGH. La remediación cerró el bloqueo I1: la fecha de
  vigencia de referencia se define sobre las filas `INITIAL` y `RECOMPUTED`, y una fila
  `RECOMPUTED` y una `CARRIED_FORWARD` pueden coexistir con la misma fecha de vigencia.
  Cobertura de FR y SC completa en `tasks.md`; los criterios de aceptación de US1 tienen tarea
  propia de test y de implementación.
- **Hallazgos no bloqueantes**:
  - **M1 (MEDIUM)**: `tasks.md` T016 describía la unicidad como `(player_id, effective_date)`,
    mientras `data-model.md` §1 y FR-026 fijan `(player_id, effective_date, origin)`. Corregido en
    la misma operación que marca el avance, alineando la tarea con la fuente de verdad.
  - **L1 (LOW)**: `FR-003` no precisa el criterio cuando un jugador tiene filas de métricas de
    más de un canal en su última fecha. Interpretación adoptada y documentada: el jugador es
    cotizable si al menos una de sus filas de la última fecha tiene alguna métrica informada.
  - **L2 (LOW)**: el catálogo de estrategias se reutiliza de SPEC-013 (`ValuationStrategy`) como
    tipo cerrado, de modo que `PlayerQuote` y `QuoteExecution` registran la estrategia como enum y
    no como texto libre; esto no contradice `data-model.md` §1, que describe el tipo SQL.
- **Modo**: solo lectura sobre los artefactos; ningún archivo de código fue modificado en esta fase.
- **Tests y builds**: no ejecutados.

### Estado de autorizaciones

- **IMPLEMENT**: no autorizada al cierre de esta fase.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

## IMPLEMENT — 2026-10-21

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: IMPLEMENT.
- **Autorización humana**: instrucción expresa del usuario «autorizo a Setup + Foundational +
  US1», que identifica el alcance por fases del `tasks.md`: T001–T003 (Setup), T004–T013
  (Foundational) y T014–T020 (User Story 1). No incluye US2 a US5 ni la fase Polish.
- **Prerrequisitos**: preparación global válida, checklists evaluados (19/19 y 16/16) y ANALYZE
  vigente sin blockers, según la sección anterior.
- **Trabajo realizado**: T001 a T020 completadas y marcadas `[X]` en `tasks.md`. Ninguna tarea
  ajena al alcance fue marcada.
- **Archivos creados**: migración `V20261021042325__create_player_quotes.sql`; `config/
  QuoteProperties.java`; `config/QuoteConfig.java`; `models/PlayerQuote.java`;
  `models/QuoteExecution.java`; `models/enums/QuoteOrigin.java`;
  `models/exceptions/InvalidQuoteException.java`; `models/records/MarketInitializationResult.java`;
  `repositories/PlayerQuoteRepository.java`; `repositories/QuoteExecutionRepository.java`;
  ocho excepciones de dominio de cotización; `services/internal/QuotePopulationSelector.java`;
  `services/internal/QuoteScoreResolver.java`; `services/QuoteMarketInitializationService.java`;
  `services/impl/QuoteMarketInitializationServiceImpl.java`; `controllers/QuoteController.java`;
  `controllers/mappers/QuoteMapper.java`; `controllers/dtos/requests/InitializeQuotesRequestDTO.java`;
  `controllers/dtos/responses/InitializeMarketResponseDTO.java`; y los tres tests de US1.
- **Archivos modificados**: `application.yml` (valores por defecto de `market.quote`),
  `GlobalExceptionHandler.java` (ocho códigos nuevos), `PlayerStatisticsRepository.java` (población
  cotizable), `PositionalValuationRepository.java` y `AttributeValuationRepository.java` (valuación
  más reciente por jugador).
- **Control ejecutable**: `./gradlew build -x test` desde `backend/`: BUILD SUCCESSFUL, incluida la
  validación de clasificación de tests y el formateo de Spotless. `./gradlew compileTestJava`
  compila los tests nuevos sin ejecutarlos.
- **Tests**: escritos y pendientes de ejecución por el usuario. El agente no ejecutó tests.
- **Git**: sin commits ni push en esta operación. `specs/assignments.md` figuraba modificado en el
  árbol de trabajo por trabajo previo y ajeno a esta operación; se preservó sin alterar.

### Estado de autorizaciones

- **IMPLEMENT**: consumida en el alcance T001–T020. Vigente solo para ese alcance.
- **CODE REVIEW**: pendiente. La implementación del alcance habilitado CODE REVIEW con informe
  completo; no lo aprueba.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

### Siguiente acción permitida

- CODE REVIEW humano del alcance implementado (T001–T020), con el informe de revisión y la
  lista de verificación obligatorias.
- Con la revisión aprobada, TESTING humano sobre los tests escritos.
- La autorización de US2 a US5 requiere una autorización humana nueva y explícita.

## IMPLEMENT AUTHORIZATION - 2026-10-21 (US2)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Autorización humana**: instrucción expresa del usuario: «autorizo a implementar US02».
- **Alcance autorizado**: User Story 2 de `tasks.md`, tareas T021-T028, incluidos sus tests backend escritos pero no ejecutados y los prerrequisitos ya implementados T001-T020.
- **Evidencia de preparación**: checklists completos (19/19 y 16/16) y ANALYZE posterior a la remediación sin hallazgos CRITICAL ni HIGH; no existen blockers vigentes para este alcance.
- **Inicio efectivo**: esta entrada registra el comienzo real del trabajo autorizado. El estado de seguimiento remoto permanece no sincronizado conforme a la contingencia de §12.
- **Exclusiones**: US3-US5, Polish, CODE REVIEW, TESTING, CONVERGE y FEATURE COMPLETION no están autorizados por esta instrucción.

## IMPLEMENT BLOCKED - 2026-10-21 (US2)

- **Alcance afectado**: US2, T021-T028.
- **Hallazgo**: T021 exige demostrar fórmula, redondeo, tope, piso y filas persistidas desde `QuoteRecalculationServiceTest`, pero la API pública de US2 solo expone el resumen agregado. `docs/backend/testing.md` §§5-6 prohíbe acceder a Repository/DB en Assert desde un test Service, y las consultas públicas que permitirían observar las cotizaciones pertenecen a US3, fuera del alcance autorizado.
- **Impacto arquitectónico**: la fórmula de ajuste es una regla resoluble exclusivamente con datos de dominio y debe pertenecer a Model conforme a `docs/backend/architecture.md` §1.4; el desglose actual no contiene una tarea de Model ni su test específico.
- **Trabajo parcial conservado**: se inició la implementación productiva de T024-T028, pero ninguna tarea de US2 se marca completa ni verificada. No se ejecutaron tests ni build.
- **Remediación requerida**: corrección autorizada y no destructiva de `tasks.md`, preservando IDs y avance, para incorporar la responsabilidad de Model y su prueba; después debe ejecutarse un nuevo ANALYZE antes de reanudar IMPLEMENT.
- **Autorizaciones**: la autorización de implementación de US2 queda afectada por la corrección upstream requerida; CODE REVIEW, TESTING y cierre continúan pendientes.

## TASKS REMEDIATION - 2026-10-21 (US2 domain calculation)

- **Autorización humana**: «tenes autorizacion para agregarlo, ya que falto para esto para el correcto funcionamiento. Y asi no dependes de nada mas».
- **Corrección aplicada**: T021 incorpora `QuoteAdjustmentTest` para verificar fórmula, redondeo half-up, tope y piso desde la frontera Model; T024 incorpora `QuoteAdjustment` como Model inmutable consumido por `QuotePlayerPublisher`.
- **IDs y avance**: se preservaron T001-T050 y todos sus marcadores; no se añadió alcance funcional ni se marcó trabajo nuevo como completado.
- **Impacto**: la autorización IMPLEMENT anterior de US2 queda invalidada únicamente para T021/T024 y sus consumidores por el cambio material de `tasks.md`; requiere nuevo ANALYZE y renovación explícita para el alcance actualizado.
- **Tests y build**: no ejecutados; cambio documental de desglose.

## ANALYZE - 2026-10-21 (posterior a remediación de US2)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Fase**: ANALYZE, estrictamente de lectura sobre los artefactos corregidos.
- **Artefactos contrastados**: `spec.md` (66 FR y 16 SC), `plan.md`, `research.md`, `data-model.md`, contratos, `quickstart.md`, `tasks.md` (T001-T050), checklists, Constitución, workflow y normas backend aplicables.
- **Resultado**: sin hallazgos CRITICAL ni HIGH; cobertura completa y preparación sin blockers. T021 cubre la regla matemática desde Model y T024 asigna esa responsabilidad a `QuoteAdjustment`, sin depender de US3.
- **Hallazgo no bloqueante M1 (MEDIUM)**: T025 conserva la frase «aplicación del multiplicador acotado, redondeo half-up y piso mínimo» aunque T024 asigna el cálculo a `QuoteAdjustment`. Se interpreta conforme a arquitectura como consumo y orquestación del resultado del Model, no duplicación de la fórmula en Service. Puede precisarse editorialmente en una operación posterior, pero no altera alcance ni impide implementar.
- **Métricas**: 82 requisitos/criterios identificados (66 FR + 16 SC), 50 tareas, cobertura 100 %, 0 ambigüedades bloqueantes, 0 duplicaciones materiales, 0 issues CRITICAL/HIGH.
- **Tests y build**: no ejecutados. ANALYZE no modificó archivos de implementación.
- **Hooks**: `.specify/extensions.yml` no existe; no hubo hooks before/after analyze.

### Estado de autorizaciones tras ANALYZE

- **IMPLEMENT US2**: requiere renovación explícita para T021-T028 actualizadas; la autorización anterior fue invalidada por impacto material sobre T021/T024 y sus consumidores.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION**: pendiente.

## IMPLEMENT AUTHORIZATION - 2026-10-21 (US2 actualizada)

- **Autorización humana**: «Autorizo IMPLEMENT de US02 actualizada, tareas T021–T028, incluyendo QuoteAdjustment y sus tests.»
- **Alcance**: T021-T028 posteriores al ANALYZE vigente, incluido `QuoteAdjustment`, su test unitario, tests Service/Controller/Persistence y los componentes HTTP y de aplicación de US2.
- **Prerrequisitos**: T001-T020 implementadas; checklists completos; ANALYZE posterior a la remediación sin blockers.
- **Inicio efectivo**: el trabajo se reanuda con esta entrada. US3-US5, Polish, CODE REVIEW, TESTING, CONVERGE y cierre permanecen fuera de alcance.

## IMPLEMENT - 2026-10-21 (US2 actualizada completada)

- **Alcance**: T021-T028 de US2, conforme a la autorización renovada y al ANALYZE vigente.
- **Resultado**: T021-T028 implementadas y marcadas `[X]`; ninguna tarea de US3-US5 ni Polish fue modificada o marcada.
- **Dominio**: `QuoteAdjustment` encapsula fórmula compuesta, acotamiento del multiplicador, escala de seis decimales, redondeo half-up del precio y piso mínimo.
- **Aplicación y persistencia**: `QuotePlayerPublisher` publica cada jugador en `REQUIRES_NEW`, respeta la fecha de vigencia de referencia y registra fallos; `QuoteRecalculationServiceImpl` aplica la precedencia de rechazos, reserva una ejecución única por fecha UTC, continúa ante fallos individuales y completa el resumen persistido.
- **HTTP**: incorporados `POST /api/quotes/recalculate` y `GET /api/quotes/executions?date=`, DTO, mapper, OpenAPI, seguridad JWT y traducción de errores contractuales.
- **Tests escritos**: `QuoteAdjustmentTest`, `QuoteRecalculationServiceTest`, `QuoteExecutionPersistenceTest` y ampliación de `QuoteControllerTest`. No fueron ejecutados por el agente.
- **Control permitido**: `./gradlew build -x test` desde `backend/`: BUILD SUCCESSFUL. Incluyó compilación productiva, Spotless y validación estática de clasificación de 112 clases; no ejecutó tests y no acredita TESTING.
- **Git**: `git diff --check` sin errores; sin commit ni push. `specs/assignments.md` continúa como cambio previo preservado.
- **Hooks after_implement**: `.specify/extensions.yml` no existe; no hubo hooks que despachar.

### Estado de autorizaciones

- **IMPLEMENT US2**: alcance técnico completado; habilita solicitud de CODE REVIEW, no la aprueba.
- **CODE REVIEW**: pendiente de revisión humana del informe completo.
- **TESTING**: pendiente; tests a ejecutar por el usuario después de CODE REVIEW.
- **FEATURE COMPLETION**: pendiente y fuera de esta ejecución.

## IMPLEMENT AUTHORIZATION - 2026-10-21 (US3)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Autorización humana**: instrucción expresa del usuario: «autorizo la implementacion US03».
- **Alcance autorizado**: User Story 3 de `tasks.md`, tareas T029-T034, incluidos sus tests backend escritos pero no ejecutados y los prerrequisitos ya implementados T001-T028.
- **Evidencia de preparación**: checklists completos (`checklists/cotizacion.md` 19/19 y `checklists/requirements.md` 16/16) y ANALYZE posterior a la remediación de US2, registrado el 2026-10-21, sin hallazgos CRITICAL ni HIGH ni blockers vigentes para este alcance; el desglose de US3 no fue alterado desde ese ANALYZE.
- **Dependencias**: US3 depende solo de Foundational (T001-T013) y de T008 (consultas del repositorio de cotizaciones), ya implementados. No depende de US4 ni US5, que permanecen fuera de alcance.
- **Inicio efectivo**: esta entrada registra el comienzo real del trabajo autorizado. El estado de seguimiento remoto permanece no sincronizado conforme a la contingencia de §12.
- **Exclusiones**: US4, US5, Polish (T035-T050), CODE REVIEW, TESTING, CONVERGE y FEATURE COMPLETION no están autorizados por esta instrucción.

## IMPLEMENT - 2026-10-21 (US3 completada)

- **Alcance**: T029-T034 de US3, conforme a la autorización del 2026-10-21.
- **Resultado**: T029-T034 implementadas y marcadas `[X]`; ninguna tarea de US4, US5 ni Polish fue modificada o marcada.
- **Dominio y aplicación**: `QuoteQueryService` y `QuoteQueryServiceImpl` resuelven historial, cotización vigente y cotización a una fecha dada por acceso directo a `PlayerQuoteRepository`, sin recalcular precios (FR-030, FR-065), con el orden `effectiveDate DESC` y prevalencia de la fila `RECOMPUTED` a igualdad de fecha de vigencia (FR-037). La ausencia de cotización se informa con `QuoteNotFoundException` (`404 QUOTE_NOT_FOUND`, FR-040).
- **HTTP**: creados `PlayerQuoteController` (`GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=`, `GET /api/players/{id}/quotes/current`), `QuoteResponseDTO` y `QuoteHistoryResponseDTO`; `QuoteMapper` ampliado con el mapeo de cotización e historial. Las anotaciones OpenAPI siguen `contracts/quotes-http.md` §§4-6 y §8.
- **Tests escritos**: `QuoteQueryServiceTest` (categoría Service, `integration`) y `PlayerQuoteControllerTest` (categoría Controller, `lightweight`). No fueron ejecutados por el agente.
- **Defecto preexistente corregido**: `QuoteRecalculationServiceTest` (US2) no compilaba por una inferencia genérica en `containsExactlyInAnyOrder` sobre `List<Future<Class<?>>>`; se corrigió la aserción para comparar el nombre simple de la clase. Corrección mecánica, sin cambio de comportamiento ni de alcance.
- **Control permitido**: `./gradlew build -x test` desde `backend/`: BUILD SUCCESSFUL, con compilación productiva, Spotless y validación estática de clasificación de 114 clases de test. `./gradlew compileTestJava`: BUILD SUCCESSFUL; compila los tests nuevos sin ejecutarlos.
- **Tests**: escritos y pendientes de ejecución por el usuario. El agente no ejecutó tests.
- **Git**: sin commits ni push en esta operación. `specs/assignments.md` continúa como cambio previo preservado.

### Estado de autorizaciones

- **IMPLEMENT US3**: alcance técnico completado; habilita solicitud de CODE REVIEW, no la aprueba.
- **CODE REVIEW**: pendiente de revisión humana del informe completo.
- **TESTING**: pendiente; tests a ejecutar por el usuario después de CODE REVIEW.
- **FEATURE COMPLETION**: pendiente y fuera de esta ejecución.

## IMPLEMENT AUTHORIZATION - 2026-10-21 (US4)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Autorización humana**: instrucción expresa del usuario: «autorizo a implementar US04».
- **Alcance autorizado**: User Story 4 de `tasks.md`, tareas T035-T038, incluidos sus tests backend
  escritos pero no ejecutados y los prerrequisitos ya implementados T001-T034.
- **Evidencia de preparación**: checklists completos (`checklists/cotizacion.md` 19/19 y
  `checklists/requirements.md` 16/16) y ANALYZE vigente posterior a la remediación de US2, registrado
  el 2026-10-21, sin hallazgos CRITICAL ni HIGH ni blockers; el desglose de US4 (T035-T038) no fue
  alterado desde ese ANALYZE.
- **Dependencias**: US4 depende solo de Foundational (T001-T013) y de las consultas ya implementadas
  en US3 (T008, T031). No depende de US5 ni de Polish, que permanecen fuera de alcance.
- **Inicio efectivo**: esta entrada registra el comienzo real del trabajo autorizado. El estado de
  seguimiento remoto permanece no sincronizado conforme a la contingencia de §12.
- **Exclusiones**: US5, Polish (T039-T050), CODE REVIEW, TESTING, CONVERGE y FEATURE COMPLETION no
  están autorizados por esta instrucción.

## IMPLEMENT - 2026-10-21 (US4 completada)

- **Alcance**: T035-T038 de US4, conforme a la autorización del 2026-10-21.
- **Resultado**: T035-T038 implementadas y marcadas `[X]`; ninguna tarea de US5 ni Polish fue modificada
  o marcada.
- **Persistencia**: `PlayerQuoteRepository.findRanking(playerIds)` resuelve la fila vigente de cada
  jugador con el mismo orden de lectura del historial (fecha de vigencia descendente, prevalencia de
  la fila `RECOMPUTED` a igualdad de fecha e instante de cálculo descendente) y la ordena por precio
  descendente, sin recalcular precios.
- **Aplicación**: `QuoteQueryService.findRanking()` restringe el ranking a la población cotizable
  (regla ya encapsulada en `QuotePopulationSelector`), de modo que un jugador sin métricas no aparece
  (FR-001, FR-003, FR-039), y proyecta cada fila en el Model `PlayerRankingEntry`.
- **HTTP**: creados `RankingResponseDTO` y `PlayerRankingResponseDTO`, mapeo en `QuoteMapper` y
  exposición de `GET /api/players/ranking` en `PlayerQuoteController` con anotaciones OpenAPI
  coherentes con `contracts/quotes-http.md` §7.
- **Tests escritos**: ranking en `QuoteQueryServiceTest` (categoría Service, `integration`) y contrato
  HTTP de `GET /api/players/ranking` en `PlayerQuoteControllerTest` (categoría Controller,
  `lightweight`), este último exigido por `docs/backend/testing.md` §8 al añadir una ruta nueva. Se añadió
  el fixture compartido `support/QuoteTestData` para preparar métricas y filas de cotización en
  Arrange. No fueron ejecutados por el agente.
- **Control permitido**: `./gradlew build -x test` desde `backend/`: BUILD SUCCESSFUL, con compilación
  productiva y de tests, Spotless y validación estática de clasificación de 114 clases de test.
  `./gradlew compileTestJava`: BUILD SUCCESSFUL.
- **Tests**: escritos y pendientes de ejecución por el usuario. El agente no ejecutó tests.
- **Git**: sin commits ni push en esta operación. `specs/assignments.md` continúa como cambio previo
  preservado.

### Estado de autorizaciones

- **IMPLEMENT US4**: alcance técnico completado; habilita solicitud de CODE REVIEW, no la aprueba.
- **CODE REVIEW**: pendiente de revisión humana del informe completo.
- **TESTING**: pendiente; tests a ejecutar por el usuario después de CODE REVIEW.
- **FEATURE COMPLETION**: pendiente y fuera de esta ejecución.

## IMPLEMENT AUTHORIZATION - 2026-10-21 (US5 y Polish)

- **Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`.
- **Autorizacion humana**: instruccion expresa del usuario: "autorizo implementar US05 y Polish".
- **Alcance autorizado**: User Story 5 de `tasks.md` (T039-T045) y Polish & Cross-Cutting Concerns
  (T046-T050), incluidos los tests backend escritos pero no ejecutados y los prerrequisitos ya
  implementados T001-T038.
- **Evidencia de preparacion**: checklists completos (`checklists/cotizacion.md` 19/19 y
  `checklists/requirements.md` 16/16) y ANALYZE vigente posterior a la remediacion de US2,
  registrado el 2026-10-21, sin hallazgos CRITICAL ni HIGH ni blockers; el desglose de US5 y Polish
  (T039-T050) no fue alterado desde ese ANALYZE.
- **Dependencias**: US5 depende de Foundational (T001-T013), de T008 (consultas del repositorio de
  cotizaciones) y del puerto `QuoteSource` existente. Polish depende de US1-US5. No dependen de
  historias ni de gates no autorizados.
- **Inicio efectivo**: esta entrada registra el comienzo real del trabajo autorizado. El estado de
  seguimiento remoto permanece no sincronizado conforme a la contingencia de §12.
- **Exclusiones**: CODE REVIEW, TESTING, CONVERGE y FEATURE COMPLETION no estan autorizados por esta
  instruccion.

## IMPLEMENT - 2026-10-21 (US5 y Polish completada)

- **Alcance**: T039-T050 de US5 y Polish, conforme a la autorizacion del 2026-10-21.
- **Resultado**: T039-T050 implementadas y marcadas `[X]`. Ninguna tarea ajena al alcance fue modificada o marcada.
- **Fuente real (T042)**: creado `backend/src/main/java/footballmarket/integrations/RepositoryQuoteSource.java`, adaptador del puerto `QuoteSource` contra `PlayerQuoteRepository`: devuelve la fila vigente por `effective_date DESC` con prevalencia de la fila `RECOMPUTED`, conserva el `calculated_at` de la fila como instante de calculo y la `strategy_version` nula de la fila inicial. La firma del puerto no cambia (FR-058).
- **Retiro del simulador (T043)**: eliminados `SimulatedQuoteSource`, `QuoteSimulationConfiguration`, `MissingQuoteSourceException`, sus tests (`SimulatedQuoteSourceTest`, `QuoteSimulationConfigurationTest`) y el registro de auto-configuracion. Retiradas de `application.yml` y `application-test.yml` las propiedades `market.quote.simulator.enabled` y `market.quote.simulator.price`. La busqueda `SimulatedQuoteSource|market.quote.simulator|QuoteSimulationConfiguration|MissingQuoteSourceException` en `backend/src` no devuelve coincidencias (SC-011).
- **Mercado no inicializado (T044)**: creado el componente interno `services/internal/MarketInitializationGuard.java`, que resuelve por `PlayerQuoteRepository.existsByOrigin(INITIAL)`. `BuyOrderServiceImpl` (compra directa y compra de una publicacion) y `TokenListingServiceImpl` (publicacion de venta) lo invocan antes de leer la cotizacion y antes de cualquier movimiento de recursos, con rechazo `MARKET_NOT_INITIALIZED` (409). Se conserva la precedencia previa de las validaciones estructurales y de rol.
- **Tests escritos (T039-T041)**: `integrations/RepositoryQuoteSourceTest.java` (categoria Integration, tag `integration`); actualizados `services/BuyOrderServiceTest.java` y `services/TokenListingServiceTest.java` con el rechazo `MARKET_NOT_INITIALIZED` y la comprobacion de que el rechazo no consume recursos; creado `repositories/QuoteMarketOperationPersistenceTest.java` (categoria Repository) que afirma, con la fuente real y la persistencia real, que la compra consume el precio y el instante de calculo persistidos y que el recalculo posterior no altera saldos, tenencias ni transacciones. No fueron ejecutados por el agente.
- **Adaptacion de fixtures**: `support/MarketTestData.java` incorpora `initializeMarket` y `clearMarketInitialization`, usados solo en Arrange. Los tests de mercado que compran o publican (`ListingRepositorySupport` y sus subclases, `MarketDomainPersistenceTest`) inicializan el mercado en su preparacion, precondicion nueva derivada de FR-043.
- **Correccion documental (T045)**: `specs/010-buy-tokens/contracts/quote-source.md` declara ahora como consumidores del puerto a `BuyOrderServiceImpl` y `TokenListingServiceImpl`.
- **Polish (T046-T049)**: completadas las anotaciones OpenAPI de `QuoteController` (`500 INTERNAL_ERROR` en el recalculo, esquema de error en la consulta de ejecuciones) y las descripciones de campo de los DTO de cotizaciones, conforme a `contracts/quotes-http.md`; `docs/backend/architecture.md` §1.3 documenta la dependencia efectiva `Integration -> Repository` y su alcance; `docs/backend/technologies.md` no requiere cambios porque la feature no incorpora dependencias ni altera versiones (T048); la ejecucion y las consultas de cotizacion usan el registro estructurado vigente con la forma `clave=valor`, sin reimplementar mecanismos de observabilidad ni introducir correlacion de solicitudes que el proyecto no tiene, y el fallo individual queda registrado con jugador, etapa y causa en `QuotePlayerPublisher` y en el resumen de la corrida (T049, FR-062, FR-064).
- **Control permitido**: `./gradlew build -x test` desde `backend/`: BUILD SUCCESSFUL, con compilacion productiva, Spotless y validacion estatica de clasificacion. `./gradlew compileTestJava`: BUILD SUCCESSFUL; los tests nuevos compilan y no fueron ejecutados.
- **Git**: sin commits ni push. `specs/assignments.md` y los cambios previos de US1-US4 se conservan como estaban.

### Estado de autorizaciones

- **IMPLEMENT US5 y Polish**: alcance tecnico completado; habilita solicitud de CODE REVIEW, no la aprueba.
- **CODE REVIEW**: pendiente de revision humana del informe completo.
- **TESTING**: pendiente; tests a ejecutar por el usuario despues de CODE REVIEW.
- **CONVERGE y FEATURE COMPLETION**: pendientes y fuera de esta ejecucion.

## DOCUMENTACION - 2026-10-21 (regla operativa de la corrida y deriva por campana no utilizable)

- **Alcance**: actualizacion exclusivamente documental de los artefactos de la feature y del registro de deuda tecnica, a peticion expresa del usuario ("si porfavor, dejalo registrado esto"). No amplio ni redujo el alcance de ninguna historia ni tarea.
- **Artefactos modificados**:
  - `specs/016-ejecutar-recalculo-manual-de-cotizaciones/quickstart.md`: nueva seccion 7 "Regla operativa de la corrida semanal" (cuando evaluar y cuando no, lectura de los tres contadores de la corrida, distincion de precio congelado en el ranking y consulta de auditoria de la deriva). Las secciones 7 a 9 preexistentes pasan a 8 a 10 por renumeracion; no hay referencias cruzadas a numeros de seccion en el documento.
  - `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`: seccion 4 (historial) precisa que un jugador sin valuacion posterior a su fecha de vigencia de referencia no genera fila esa fecha y que el hecho se informa en el contador `notRepricedPlayers`; seccion 7 (ranking) precisa que `effectiveDate` y `origin` permiten distinguir a un jugador repreciado de uno con precio congelado.
  - `docs/TECHNICAL_DEBT.md`: alta de TD-021 "Re-evaluacion con una campana de metricas ya usada reprecia con datos estancados", con evidencia por archivo y linea, causa tecnica, impacto, solucion propuesta, criterios de resolucion, referencias e historial. Se agrego tambien la fila del indice y la fila del historial general del registro.
- **Naturaleza**: cambios exclusivamente documentales. Ningun archivo de codigo, migracion, contrato OpenAPI, configuracion ni test fue modificado. `tasks.md` no cambio: ninguna tarea nueva se marco ni se desmarco.
- **Decision del responsable del proyecto registrada**: no se modifica el codigo. El congelamiento por jugador -el item de campana fallido con `NO_DATA`, `NOT_FOUND` o `PROVIDER_ERROR` conserva el precio, no genera fila ese dia y se reprecia cuando una campana posterior aporte sus metricas- queda como comportamiento deseado y aceptado. El caso registrado en TD-021 es distinto: una campana no utilizable hace que la capa de valuacion reutilice en silencio la campana confirmada anterior y selle esas valuaciones con el dia de la evaluacion, con lo que el recalculo reprecia a todos los jugadores sobre datos que no cambiaron.
- **Tests y builds**: no ejecutados. Cambio documental; conforme a la Constitucion y al workflow, la validacion controlable es la revision de rutas, enlaces, comandos y coherencia del diff. No se ejecuto ningun test de aplicacion.
- **Git**: sin commits ni push. Los cambios previos de la implementacion de US1 a US5 y Polish, y el cambio previo de `specs/assignments.md`, se conservan intactos.

### Estado de autorizaciones

- **IMPLEMENT US1 a US5 y Polish**: sin cambios; esta operacion no modifica el alcance tecnico ya implementado ni lo habilita para cierre.
- **CODE REVIEW**: pendiente de revision humana del informe completo del alcance implementado.
- **TESTING**: pendiente; tests a ejecutar por el usuario despues de CODE REVIEW.
- **CONVERGE y FEATURE COMPLETION**: pendientes y fuera de esta ejecucion.

### Siguiente accion permitida

- CODE REVIEW humano del alcance implementado (T001 a T050), con el informe de revision y la lista de verificaciones obligatorias.

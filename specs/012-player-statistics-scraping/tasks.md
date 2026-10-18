---
description: "Task list for feature implementation"
---

# Tasks: Scraping de estadísticas de rendimiento de jugadores

**Input**: Design documents from `/specs/012-player-statistics-scraping/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/http.md, contracts/whoscored.md, quickstart.md

**Tests**: Se escriben tests backend necesarios según Constitution §6 y `docs/backend/testing.md`; su ejecución corresponde al usuario, nunca del agente. Frontend exento: no se crean tests ni infraestructura frontend. Los tests nunca accederán a WhoScored real; los providers se sustituyen por dobles controlados en `backend/src/test/java/footballmarket/support/`.

**Convenciones de test aplicables a toda tarea de test de este archivo** (derivadas de `docs/backend/testing.md`): `@Nested` obligatorio en toda clase contenedor y ningún método de test directamente en ella; prohibido `this` y `OuterTest.this`; `integrations` solo para adaptadores de proveedores externos; repositorios con PostgreSQL real vía Testcontainers y migraciones Flyway reales, H2 prohibido; `@ActiveProfiles("test")` obligatorio en Service y E2E, condicional en seguridad, prohibido en Model, Orquestador unitario, Integration autocontenido y Config con propiedades explícitas; E2E con `@SpringBootTest(RANDOM_PORT)` y `RestClient` real, nunca MockMvc, TestRestTemplate ni WebTestClient. Los nombres de archivo van en inglés y los nombres descriptivos de test en español. Ninguna tarea de este archivo autoriza al agente a ejecutar tests.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs y avance al reanudar.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**Estado vigente — 2026-10-18**: alineación documental autorizada por el usuario entre el código y los artefactos de la spec. El modelo implementado es aplanado: `V20261018191335__flatten_player_statistics.sql` sustituye las tablas de contextos, observaciones y comparaciones por una fila `player_statistics` por jugador, fecha y canal con las 60 métricas en columnas. En consecuencia: cobertura efectiva 45/57/42, quince combinaciones JSON, sin contraste entre canales, sin disponibilidad ni precisión por métrica. Las tareas de contraste (T061, T062, T069, T070) quedan retiradas y desmarcadas; las de entidades normalizadas se declaran superadas por `PlayerStatisticsWriter`. Los IDs y el resto de las casillas se conservan sin renumerar. Ninguna casilla acredita ejecución de tests.

**Estado vigente — 2026-10-16**: diseño decimal corregido bajo autorización humana: NUMERIC sin precisión/escala fijas para valores, tolerancias y diferencias; BigDecimal de extremo a extremo, sin redondear valores directos. Solo las derivadas conservan seis decimales HALF_UP (FR-027). El usuario confirmó que el Analyze posterior ya fue ejecutado y solicitó corregir el estado y continuar; READY LOCAL está restablecido e IMPLEMENT de US01 continúa IN PROGRESS. T001/T002/T003/T006 permanecen completadas según evidencia previa. Historial en `sdd-gates.md` y TD-019; US02/US03 siguen sin autorización de implementación.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend Java monolith: `backend/src/main/java/footballmarket/...`
- Tests: `backend/src/test/java/footballmarket/...`
- Migraciones: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencia de navegador, configuración efectiva, migración de esquema y properties de la feature.

- [X] T001 Declarar `com.microsoft.playwright:playwright:1.63.0` en `backend/gradle/libs.versions.toml` (alias `playwright`) y consumed como `implementation(libs.playwright)` en `backend/build.gradle.kts`
- [X] T002 Actualizar el inventario tecnológico con Playwright, su versión y su propósito en `docs/backend/technologies.md`
- [X] T003 [P] Añadir las propiedades `whoscored.statistics.*` (workers por canal, intervalo mínimo, tiempo máximo de lote solo para scraping, timeouts por consulta, filtrado opcional de terceros y base de confirmación headed) en `backend/src/main/resources/application.yml` con defaults de plan.md; JSON no hereda el corte temporal de scraping; sin credenciales embebidas ni referencias a secretos, única tarea que edita este archivo
- [X] T004 [P] Crear los dobles controlados de proveedor en `backend/src/test/java/footballmarket/support/WhoScoredStatisticsProviderStub.java` para el canal JSON y `backend/src/test/java/footballmarket/support/WhoScoredStatisticsPageStub.java` para el recorrido headed, de modo que ningún test acceda a WhoScored real ni exija sesión gráfica
- [X] T005 Crear la migración `backend/src/main/resources/db/migration/V20261018191334__create_player_statistics.sql` con las tablas de `data-model.md`, secuencias e índices; incluir `max_players` nullable positivo en runs, unicidad `(id, extraction_date, source)` en runs, fecha/canal no nulos en items con FK compuesta al run e índice único parcial `uq_statistics_item_completed_campaign` por fecha/canal/jugador solo para `COMPLETED`; `value`, `tolerance` y `difference` usan PostgreSQL `NUMERIC` sin precisión ni escala fijas. Completada además por `V20261018191335__flatten_player_statistics.sql`, que migra el histórico a `player_statistics` y elimina contextos, observaciones y comparaciones
- [X] T006 [P] Sobrescribir las nuevas propiedades de `whoscored.statistics` en `backend/src/test/resources/application-test.yml` con valores de prueba sin acceso a proveedor real

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Catálogo cerrado, modelo persistente, normalización, derivación, contratos HTTP y andamiaje de la fachada. Bloquea todas las US.

**Orden obligatorio**: completar el código fundacional antes del código funcional de las US. T031 y T033 conservan su identidad de US01, pero su escritura se adelanta al checkpoint fundacional; esta excepción de orden no autoriza otras US ni ejecución de tests por el agente.

- [X] T007 Crear los enums `MetricSource`, `MetricUnit`, `MetricAvailability`, `DerivationRule`, `ExtractionRunStatus`, `PlayerExtractionResult` y `ComparisonResult` en `backend/src/main/java/footballmarket/models/enums/`
- [X] T008 [P] Crear `MetricDefinition` en `backend/src/main/java/footballmarket/models/MetricDefinition.java` con clave, unidad, canales, columnas externas, opcionalidad y regla de derivación
- [X] T009 Crear el catálogo inmutable de 60 definiciones y su validación de cobertura 45/57/42, tres exclusivas de scraping y alias `foulsPerGame`/`foulCommitted` en `backend/src/main/java/footballmarket/models/MetricCatalog.java`
- [X] T010 [P] Crear la entidad `PlayerStatisticsExtractionRun` en `backend/src/main/java/footballmarket/models/PlayerStatisticsExtractionRun.java` con los contadores y transiciones de `data-model.md`
- [X] T011 [P] Crear la entidad `PlayerStatisticsRunItem` en `backend/src/main/java/footballmarket/models/PlayerStatisticsRunItem.java` con snapshot del ID externo, resultado estable y fecha/canal copiados del run sin cambios independientes, coherentes con la FK compuesta y el índice parcial de `data-model.md`
- [X] T012 [P] Superada: el modelo aplanado no tiene entidad de contexto. Los IDs de proveedor y temporada se conservan como columnas nullable de `PlayerStatistics` (`V11`, `data-model.md` §PlayerStatistics)
- [X] T013 [P] Superada: no existe entidad de observación. Cada métrica es una columna `NUMERIC` sin precisión/escala fijas en `PlayerStatistics`, sin redondear ni rechazar el rating por superar seis decimales; disponibilidad y precisión observada no se persisten
- [X] T014 [P] Retirada el 2026-10-18 con el contraste entre canales: no existe entidad de comparación ni auditoría de discrepancias
- [X] T015 [P] Crear la entidad `ExtractionIncident` en `backend/src/main/java/footballmarket/models/ExtractionIncident.java` sin HTML, JSON, cookies ni credenciales
- [X] T016 [P] Crear `PlayerStatisticsExtractionRunRepository` en `backend/src/main/java/footballmarket/repositories/PlayerStatisticsExtractionRunRepository.java` con consulta de run activo por canal y auditoría por fecha
- [X] T017 [P] Crear `PlayerStatisticsRunItemRepository` en `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRunItemRepository.java` con selección ascendente por ID local de pendientes y fallidos
- [X] T018 [P] Superada: no existe repositorio de contextos; el contexto vive en la fila `PlayerStatistics`
- [X] T019 [P] Superada por `PlayerStatisticsRepository`, que consulta las filas de campaña por fecha y canal y de donde `PlayerStatisticsCoverageQuery` deriva la ausencia sistémica
- [X] T020 [P] Retirada el 2026-10-18 con el contraste entre canales
- [X] T021 [P] Crear `ExtractionIncidentRepository` en `backend/src/main/java/footballmarket/repositories/ExtractionIncidentRepository.java`
- [X] T022 [P] Crear los records de salida `PlayerStatisticsSyncSummary`, `PlayerStatisticsSyncCounters` y `PlayerStatisticsCoverage` en `backend/src/main/java/footballmarket/models/records/`; `PlayerMetricComparisonCounters` queda retirado con el contraste entre canales
- [X] T023 [P] Crear `PlayerStatisticsProperties` en `backend/src/main/java/footballmarket/config/PlayerStatisticsProperties.java` con validación de valores por defecto de plan.md
- [X] T024 Implementar el normalizador de valores como regla de Model en `backend/src/main/java/footballmarket/models/MetricValueNormalizer.java`: recorte, punto y coma decimal, `BigDecimal` desde la representación decimal original, precisión observada y ausencia por guion o vacío; sin paso por double/float, escala fija ni límite de seis decimales para valores directos; sin dependencias de Service, Repository ni Integration
- [X] T025 Implementar la derivación por partido y el alias como reglas de Model en `backend/src/main/java/footballmarket/models/MetricDerivationApplier.java`: división por `apps` del mismo contexto, seis decimales half-up, `NOT_DERIVABLE` y normalización de `foulsPerGame` y `foulCommitted`, sin dependencias de Service, Repository ni Integration
- [X] T026 Crear `PlayerStatisticsWriter` en `backend/src/main/java/footballmarket/services/internal/PlayerStatisticsWriter.java` con confirmación atómica por jugador de su fila de campaña y su item, y reconfirmación limitada a filas sin contexto resuelto; una colisión del índice parcial de campaña revierte item, contextos y observaciones de esa confirmación y se traduce de forma controlada sin datos parciales
- [X] T027 Crear `ExtractionIncidentRecorder` en `backend/src/main/java/footballmarket/services/internal/ExtractionIncidentRecorder.java` con los códigos estables de `contracts/whoscored.md`
- [X] T028 [P] Crear `PlayerStatisticsSynchronizationException`, `PlayerStatisticsSyncInProgressException` y `PlayerStatisticsRunNotFoundException` en `backend/src/main/java/footballmarket/services/exceptions/`, y `HeadedBrowserUnavailableException`, `WhoScoredStatisticsException`, `WhoScoredStatisticsUnavailableException`, `WhoScoredStatisticsRateLimitException` e `WhoScoredStatisticsStructureChangedException` en `backend/src/main/java/footballmarket/integrations/exceptions/`, siguiendo la convención existente de errores de proveedor
- [X] T029 Mapear en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` los códigos de `contracts/http.md`: `INVALID_STATISTICS_SOURCE` solo para cobertura; fecha y máximo inválidos solo en scraping; `INVALID_REQUEST` para body de scraping ausente/malformado o entrada no permitida en JSON; `PLAYER_STATISTICS_SYNC_IN_PROGRESS` y `PLAYER_STATISTICS_SYNC_FAILED` para ambos canales y `HEADED_BROWSER_UNAVAILABLE` solo para scraping, con mensajes en español y sin cambiar errores ajenos; depende de T028
- [X] T030 Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/PlayerStatisticsSyncRequestDTO.java` exclusivamente para scraping (fecha y `maxPlayers` opcionales, sin `source`), responses comunes en `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerStatisticsSyncResponseDTO.java` y `PlayerStatisticsCoverageResponseDTO.java`, y `backend/src/main/java/footballmarket/controllers/mappers/PlayerStatisticsMapper.java`; JSON no tiene Request DTO ni argumentos funcionales, usa el mapper solo para salida; no introducir lógica de Model en Controller, mantener auditoría de canal según `contracts/http.md`

**Checkpoint**: código fundacional listo y tests T031/T033 escritos antes del código funcional de US01. Su ejecución queda pendiente del usuario y se registra por separado; no es condición para que el agente escriba el resto del alcance autorizado ni acredita TESTING.

---

## Phase 3: User Story 1 - Extraer métricas de la ficha de un jugador con recorrido de scraping (Priority: P1) 🎯 MVP

**Goal**: Recorrer la ficha pública headed de un jugador, extraer las 45 métricas del canal de scraping, normalizarlas al catálogo cerrado y persistirlas en la fila de campaña del jugador con su contexto de liga local.

**Independent Test**: Ejecutar la extracción sobre un solo identificador de jugador con referencia `WHOSCORED` confirmada y comprobar que quedan contextos y observaciones persistidas con valores numéricos, ausencias distinguibles de cero y contexto de competición y temporada correcto.

### Tests para User Story 1 (backend; su ejecución corresponde al usuario)

- [X] T031 [P] [US1] Test del catálogo cerrado de 60 métricas, cobertura 45/57/42, métricas exclusivas de scraping y alias en `backend/src/test/java/footballmarket/models/MetricCatalogTest.java`
- [X] T032 [P] [US1] Test del normalizador y de la derivación por partido, incluidos ausencia, cero, `apps = 0`, punto/coma decimal y rating `6.9033333333333333` exacto con precisión 16, sin límite de seis decimales ni conversión binaria; conservar `1 / 3 = 0.333333` y HALF_UP solo para derivadas en `backend/src/test/java/footballmarket/models/MetricValuePipelineTest.java`
- [X] T033 [P] [US1] Test de la migración y restricciones en `backend/src/test/java/footballmarket/repositories/PlayerStatisticsMigrationTest.java`: segundo `COMPLETED` rechazado entre runs de igual fecha/canal/jugador, intentos fallidos permitidos, confirmaciones de otra fecha/canal permitidas, FK compuesta que rechaza fecha/canal discrepantes y rollback sin contextos ni observaciones parciales; comprobar que las columnas de métricas de `player_statistics` son `NUMERIC` sin precisión ni escala fijas, sin default que limite decimales; escritura adelantada al checkpoint fundacional, ejecución solo humana
- [X] T034 [P] [US1] Test de la persistencia append-only y de la selección del valor vigente en `backend/src/test/java/footballmarket/repositories/PlayerStatisticsPersistenceTest.java`; demostrar con PostgreSQL y mapping JPA reales la ida/vuelta exacta de `6.9033333333333333` en su columna `numeric` sin escala fija, sin redondeo del rating ni alteración de las derivadas de seis decimales
- [X] T035 [US1] Test del recorrido de scraping contra navegador controlado, con contexto de carga inicial y de detalle y 45 columnas, en `backend/src/test/java/footballmarket/integrations/WhoScoredScrapingIntegrationTest.java`
- [X] T036 [US1] Test del servicio de extracción de un jugador con integraciones dobladas en `backend/src/test/java/footballmarket/services/PlayerStatisticsExtractionServiceTest.java`

### Implementation for User Story 1

- [X] T037 [P] [US1] Crear los records de contrato externo `WhoScoredScrapedPlayer`, `WhoScoredScrapedContextRow` y `WhoScoredScrapedMetricCell` en `backend/src/main/java/footballmarket/integrations/records/`
- [X] T038 [US1] Crear `WhoScoredStatisticsScrapingIntegration` en `backend/src/main/java/footballmarket/integrations/WhoScoredStatisticsScrapingIntegration.java` con Chromium headed, `BrowserContext` independiente para la carga inicial y otro para la sección de detalle, User-Agent real del navegador sin sobrescribirlo y recorrido de carga inicial, resumen y dieciséis subcategorías de detalle
- [X] T039 [P] [US1] Crear `WhoScoredScrapingTableParser` en `backend/src/main/java/footballmarket/integrations/WhoScoredScrapingTableParser.java` que identifique métricas por atributo de columna técnico, extraiga competición/equipo de los enlaces de la fila y enlace la temporada mediante IDs explícitos del HTML original y enlaces de temporada publicados en la misma página, rechazando contextos ambiguos
- [X] T040 [P] [US1] Crear `WhoScoredScrapingSectionNavigator` en `backend/src/main/java/footballmarket/integrations/WhoScoredScrapingSectionNavigator.java` que abra y cierre cada sección en su propio contexto y clasifique estructura cambiada frente a ausencia de datos
- [X] T041 [US1] Crear `ScrapingMetricAssembler` en `backend/src/main/java/footballmarket/services/internal/ScrapingMetricAssembler.java` que traduzca filas y celdas a definiciones del catálogo, aplique alias y descarte los acumulados
- [X] T042 [US1] Crear `PlayerStatisticsExtractionService` en `backend/src/main/java/footballmarket/services/PlayerStatisticsExtractionService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsExtractionServiceImpl.java` con confirmación atómica por jugador y sin transacción durante el acceso externo
- [X] T043 [P] [US1] Crear `HeadedBrowserAvailabilityChecker` en `backend/src/main/java/footballmarket/services/internal/HeadedBrowserAvailabilityChecker.java` que detecte ausencia de navegador o sesión gráfica antes de iniciar el recorrido
- [X] T044 [US1] Extender `WhoScoredRequestPacer` en `backend/src/main/java/footballmarket/integrations/WhoScoredRequestPacer.java` con valores por canal/worker, timeouts y único retry transitorio; pacing de scraping entre inicios de navegación de secciones, no por recurso, y de JSON entre consultas; solo scraping acota esperas al plazo restante de lote, JSON conserva timeouts por consulta; preservar identidad y paralelismo sin otro pacer/cliente, sin retry ante 403, 404, estructura inválida o dato incoherente; incluir los casos de scraping en T035 sin esperar T089 de US02
- [X] T079 [P] [US1] Crear el record `WhoScoredStructuredDataBlock` en `backend/src/main/java/footballmarket/integrations/records/WhoScoredStructuredDataBlock.java` con los campos `rating`, `ShotsOnTarget`, `SecondYellow`, `GameStarted` y `SubOn`
- [X] T080 [US1] Crear `WhoScoredStructuredDataParser` en `backend/src/main/java/footballmarket/integrations/WhoScoredStructuredDataParser.java` que extraiga el bloque de datos estructurado del HTML original y conserve el rating desde el token decimal original mediante BigDecimal, sin double/float ni redondeo; transportar los indicadores ausentes para su persistencia como `ABSENT` sin invalidar el resto de la ficha
- [X] T081 [US1] Integrar `WhoScoredStructuredDataParser` en `backend/src/main/java/footballmarket/integrations/WhoScoredStatisticsScrapingIntegration.java` durante la carga inicial y aplicar el filtrado opcional de recursos de terceros detrás de la propiedad de configuración, verificando que no altera el contenido extraído
- [X] T082 [US1] Inicializar el navegador de forma perezosa en `backend/src/main/java/footballmarket/services/internal/HeadedBrowserAvailabilityChecker.java` y en la implementación de `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsExtractionServiceImpl.java`, de modo que el arranque de la aplicación no abra Chromium ni acceda al proveedor y un fallo de inicialización del scraping no impida el funcionamiento del resto del sistema
- [X] T083 [P] [US1] Test del bloque de datos estructurado y de su opcionalidad con `backend/src/test/java/footballmarket/support/WhoScoredStatisticsPageStub.java` en `backend/src/test/java/footballmarket/integrations/WhoScoredStructuredDataParserTest.java`; afirmar lectura decimal exacta del rating `6.9033333333333333` desde un token numérico, sin convertirlo a punto flotante
- [X] T084 [US1] Test del recorrido completo en `backend/src/test/java/footballmarket/integrations/WhoScoredScrapingIntegrationTest.java`: ausencia de sesión headless en toda ejecución, dieciséis subcategorías recorridas incluidas las cinco sin métricas, User-Agent real sin sobrescribir y no visita de resumen ofensivo, resumen de distribución, xG detallado, mapas de calor, comparativas ni nacionalidad
- [X] T085 [US1] Test de arranque sin navegador ni consulta al proveedor y de fallo de inicialización no bloqueante en `backend/src/test/java/footballmarket/services/HeadedBrowserStartupTest.java`

**Checkpoint**: US1 funcional y testeable de forma independiente con un único jugador.

**Continuación — 2026-10-16**: recorrido y tests independientes implementados/escritos.
T036 permanece pendiente por dependencia de la creación productiva de runs de US02;
el usuario eligió mantener el diseño, sin ampliar US01 con esa infraestructura. Las marcas
de tests acreditan escritura y revisión, no compilación ni ejecución. US01 sigue IN PROGRESS
y no puede cerrar su implementación ni pasar gates humanos con T036 pendiente.

Precisión técnica de T039 contrastada con las capturas: las filas publican competición y
equipo, pero no repiten el ID de temporada. Se conserva FR-025/FR-026, sin resolver por
nombres ni inventar temporadas; se usan únicamente IDs y años publicados en la página.
No modifica el diseño de apertura/cierre de runs que el usuario decidió mantener.

**Regresiones de configuración/HTTP — 2026-10-16**: corrección de T023/T029–T030 tras
leer los fallos de la ejecución humana: aislamiento del binding de estadísticas en
`PlayerStatisticsConfig`, fecha de scraping estrictamente ISO y ampliación de
`WhoScoredConfigurationTest`, `PlayerStatisticsConfigurationTest` y
`PlayerStatisticsContractFoundationTest`. No dependen de US02. Se mantienen IDs y casillas;
la corrección requiere nueva ejecución humana de esos tests, sin acreditar TESTING por build.

**Avance fundacional — 2026-10-16**: T001–T034 implementadas o escritas según su tipo.
Las marcas de T031–T034 acreditan escritura y revisión de sus fuentes, no ejecución:
tests pendientes del usuario. Se añadió `PlayerStatisticsContractFoundationTest` para
verificar DTO, mapper y advice sin implementar el Controller de US02. El recorrido
funcional T035–T044/T079–T085 permanece pendiente; US01 sigue IN PROGRESS.

---

## Phase 4: User Story 2 - Recorrer la población por lotes reanudables (Priority: P2)

**Goal**: Ejecutar lotes síncronos acotados sobre la población elegible, con estado de cobertura consultable, reanudación determinista y tolerancia a fallos por jugador.

**Independent Test**: Lanzar un lote sobre una población mayor que el ámbito, comprobar conteos de seleccionados, completados, fallidos y pendientes, exclusiones sin referencia, y verificar que un relanzamiento no duplica ni corrompe lo ya persistido.

### Tests para User Story 2 (backend; su ejecución corresponde al usuario)

- [X] T045 [P] [US2] Test de elegibilidad, exclusión por falta de referencia y selección ascendente de pendientes en `backend/src/test/java/footballmarket/repositories/PlayerStatisticsCoverageTest.java`
- [X] T046 [P] [US2] Test de ambos POST y GET `/coverage` en `backend/src/test/java/footballmarket/controllers/PlayerStatisticsControllerTest.java`: scraping con DTO, fecha/`maxPlayers`, defaults y máximo de 1; JSON sin parámetros/body ni Request DTO, rechazo `INVALID_REQUEST` de entrada no permitida y ausencia de invocación de fachada al rechazar; contrato de resumen, caso JSON sin pendientes y errores por ruta; usar fachada doblada para JSON sin implementar US03
- [X] T047 [US2] Test del lote, reanudación, recuperación de run activo y fallo puntual por jugador en `backend/src/test/java/footballmarket/services/PlayerStatisticsSynchronizationServiceTest.java`
- [X] T048 [US2] Test de seguridad del contrato con JWT en `backend/src/test/java/footballmarket/security/PlayerStatisticsSecurityIntegrationTest.java`
- [X] T049 [US2] Test de journey de lote y cobertura con contexto real en `backend/src/test/java/footballmarket/e2e/PlayerStatisticsSyncJourneyE2ETest.java`

### Implementation for User Story 2

- [X] T050 [P] [US2] Crear `PlayerStatisticsEligibilitySelector` en `backend/src/main/java/footballmarket/services/internal/PlayerStatisticsEligibilitySelector.java` que recorra `PlayerExternalReference` con `ExternalProvider.WHOSCORED` y cuente exclusiones
- [X] T051 [US2] Crear `ExtractionRunManager` en `backend/src/main/java/footballmarket/services/internal/ExtractionRunManager.java` con apertura, cierre, invariantes de contadores y recuperación de runs activos previos como `INTERRUPTED`; cero pendientes implica `COMPLETED` con selección/llamadas cero, no `FAILED`; conservar fecha/canal de cada run y exclusión de confirmados por campaña
- [X] T052 [P] [US2] Crear `ExtractionBatchExecutor` en `backend/src/main/java/footballmarket/services/internal/ExtractionBatchExecutor.java` con corte por tiempo restante y ritmo de campaña reconstruido de las últimas 20 confirmaciones (arranque a 60 s sin muestras) según `plan.md`, límite opcional `maxPlayers` global entre workers, pendientes y exclusión de jugadores que no caben; limitar operaciones al deadline e interrumpir sin confirmar datos parciales
- [X] T053 [US2] Crear `PlayerStatisticsCoverageQuery` en `backend/src/main/java/footballmarket/services/internal/PlayerStatisticsCoverageQuery.java` con `catalogPlayers`, conteos por fecha y canal, y la lista de claves del catálogo sin ninguna observación `PRESENT` en la población elegible de esa fecha, que distingue la ausencia sistémica de la ausencia individual observada en jugadores concretos
- [X] T086 [US2] Exponer `systemicallyAbsentMetrics` en `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerStatisticsCoverageResponseDTO.java` y en `backend/src/main/java/footballmarket/controllers/mappers/PlayerStatisticsMapper.java` siguiendo el contrato de `specs/012-player-statistics-scraping/contracts/http.md`, sin computar esas claves como cero ni alterar el catálogo
- [X] T054 [P] [US2] Crear `ExtractionWorkerPool` en `backend/src/main/java/footballmarket/services/internal/ExtractionWorkerPool.java` con el paralelismo configurado por canal y liberación de recursos al cerrar el lote
- [X] T055 [US2] Crear la fachada `backend/src/main/java/footballmarket/services/PlayerStatisticsSynchronizationService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsSynchronizationServiceImpl.java`: entrada de scraping con fecha/máximo opcionales y corte de lote; entrada JSON sin argumentos funcionales, implementada en T072, con fecha UTC resuelta en backend; ambas devuelven resumen síncrono, tests de contrato JSON con fachada doblada
- [X] T056 [P] [US2] Crear `PlayerStatisticsCoverageService` en `backend/src/main/java/footballmarket/services/PlayerStatisticsCoverageService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsCoverageServiceImpl.java` sin acceso al proveedor
- [X] T057 [US2] Crear `backend/src/main/java/footballmarket/controllers/PlayerStatisticsController.java` con POST `/api/players/statistics/whoscored/sync/scraping` con DTO y mapper de entrada, POST `/api/players/statistics/whoscored/sync/json` sin parámetros/body (rechazar entrada no permitida) y GET `/api/players/statistics/whoscored/coverage`; JSON invoca la fachada sin argumentos funcionales y solo mapea salida; documentar OpenAPI en español sin requestBody/parameters para JSON, no exponer POST genérico `/sync`; funcionamiento JSON depende de T072
- [X] T058 [US2] Registrar las rutas de estadísticas en la seguridad de `backend/src/main/java/footballmarket/config/SecurityConfig.java` con las mismas condiciones que las sincronizaciones de catálogo y sin propagar el JWT al proveedor
- [X] T059 [US2] Verificar en `backend/src/main/java/footballmarket/controllers/PlayerStatisticsController.java` y en el service que ninguna credencial, cookie, HTML ni JSON completo del proveedor aparece en logs, respuestas o incidencias
- [X] T087 [US2] Gestionar la indisponibilidad prolongada en `backend/src/main/java/footballmarket/integrations/WhoScoredRequestPacer.java` y en `backend/src/main/java/footballmarket/services/internal/ExtractionBatchExecutor.java`: aplazar las consultas posteriores según `Retry-After` dentro del límite del lote, registrar la incidencia y cerrar el lote con el estado final alcanzado sin insistir de forma agresiva
- [X] T088 [US2] Test de `systemicallyAbsentMetrics`, de su distinción frente a la ausencia individual y de su no cómputo como cero en `backend/src/test/java/footballmarket/controllers/PlayerStatisticsControllerTest.java`, depending on T053 and T086
- [X] T089 [P] [US2] Test de la extensión por canal del ritmo, del único reintento transitorio y del aplazamiento por `Retry-After` con `backend/src/test/java/footballmarket/support/WhoScoredStatisticsProviderStub.java` en `backend/src/test/java/footballmarket/integrations/WhoScoredStatisticsRequestPacerTest.java`
- [X] T090 [P] [US2] Test de corte en `backend/src/test/java/footballmarket/services/ExtractionBatchExecutorCutoffTest.java`: media de hasta 20 confirmaciones de igual fecha/canal reconstruida entre lotes, arranque de 60 s sin muestras, exclusión si no cabe, `maxPlayers = 1` global con tres workers, combinación de límites y deadline sin datos parciales; aplicar la frontera de Service real con DB/Testcontainers e Integration doblada, sin acceso funcional directo a persistencia desde el test
- [X] T091 [US2] Crear el cliente CLI de scraping en `backend/src/main/java/footballmarket/tools/PlayerStatisticsSyncClient.java` y tarea JavaExec `statisticsSyncClient` en `backend/build.gradle.kts`, sin nuevas dependencias ni arranque Spring: modos batch/campaign de `plan.md`, JWT desde entorno, DTO/RestClient reales, fecha fija, máximo opcional por lote, cobertura inicial `SCRAPING`, progreso descendente y parada sin retry automático ni secretos; no hay opción de canal ni modo JSON, que se invoca directamente sin entrada
- [X] T092 [US2] Crear `backend/src/test/java/footballmarket/e2e/PlayerStatisticsCampaignClientE2ETest.java` con `@Nested`, aplicación `RANDOM_PORT`, perfil test y proveedores controlados: cliente de scraping productivo sobre HTTP real, lote con máximo de 1, campaña de varios lotes con fecha fija incluso al cruzar medianoche, cobertura completa sin POST, pendientes fallidos/interrumpidos y no intentados, falta de progreso y fallos con campaña incompleta sin filtración de JWT; no ejecutar desde el agente, JSON se prueba en T064 sin este cliente

**Checkpoint**: US1 y US2 operativas e independientes.

**Avance de US02 — 2026-10-16**: implementación autorizada de T045–T059 y T086–T092, y
completado de T036 con la infraestructura productiva de runs. `PlayerStatisticsController`
expone `POST /api/players/statistics/whoscored/sync/scraping`,
`POST /api/players/statistics/whoscored/sync/json` y
`GET /api/players/statistics/whoscored/coverage`. La entrada JSON conserva el contrato de
T055/T057 y rechaza cualquier entrada no permitida; su actualización real depende de T072
(US03), no autorizada, y la fachada la rechaza de forma controlada sin simular éxito. Los tests
escritos no han sido ejecutados por el agente.

---

## Phase 5: User Story 3 - Actualizar métricas desde la interfaz JSON del proveedor (Priority: P3)

**Goal**: Con un POST sin parámetros/body, consultar las 57 métricas JSON para todos los elegibles pendientes del día UTC (aproximadamente 2100 en campaña nueva), omitir confirmados del mismo día, persistir histórico y contraste sin scraping ni corte de lote temporal/cantidad.

**Independent Test**: Con catálogo controlado de uno o varios jugadores identificados, invocar JSON sin entrada y comprobar 57 métricas por contexto, procedencia y contraste sin scraping. Repetir el mismo día no vuelve a consultar los completados; avanzar Clock UTC abre otra campaña. No se usa parámetro de ID ni límite de población.

### Tests para User Story 3 (backend; su ejecución corresponde al usuario)

- [X] T060 [P] [US3] Test de las quince combinaciones recorridas, validación de JSON con MIME HTML y rechazo de combinaciones especulativas en `backend/src/test/java/footballmarket/integrations/WhoScoredStatisticsIntegrationTest.java`
- [ ] T061 [P] [US3] **Retirada el 2026-10-18** junto al contraste entre canales (FR-044, FR-047, FR-048 retirados). No se implementó `MetricComparisonRuleTest` y no se vuelve a marcar sin una spec que reinstale el requisito
- [ ] T062 [P] [US3] **Retirada el 2026-10-18** con la tabla `player_metric_comparisons`, eliminada en `V11`. No se implementó `PlayerMetricComparisonPersistenceTest`
- [X] T063 [US3] Test de actualización JSON independiente y vigencia temporal en `backend/src/test/java/footballmarket/services/JsonStatisticsUpdateServiceTest.java`, con Service real, persistencia real e Integration doblada; cubrir no borrar observaciones ausentes y no confundir una nueva observación de otra campaña con repetir un jugador ya completado el mismo día
- [X] T064 [US3] Test E2E de ambos POST en `backend/src/test/java/footballmarket/e2e/PlayerStatisticsCrossChannelJourneyE2ETest.java`: JSON sin parámetros/body procesa todos los pendientes de una población controlada sin corte de 30 minutos; segunda llamada el mismo día omite confirmados, sin pendientes no consulta proveedor; nueva fecha UTC inicia otra campaña, petición que cruza medianoche conserva fecha inicial; fallidos quedan pendientes sin reselección infinita dentro del run, scraping/JSON independientes; tiempo controlado, HTTP real y proveedores controlados, sin acceso real a WhoScored

### Implementation for User Story 3

- [X] T065 [P] [US3] Crear los records de contrato externo `WhoScoredStatisticsResponse`, `WhoScoredStatisticsTable` y `WhoScoredJsonQuery` en `backend/src/main/java/footballmarket/integrations/records/`
- [X] T066 [US3] Crear `WhoScoredStatisticsIntegration` en `backend/src/main/java/footballmarket/integrations/WhoScoredStatisticsIntegration.java` sobre el cliente seguro existente, recorriendo las quince combinaciones de `contracts/whoscored.md` y validando `playerTableStats` y `statColumns`
- [X] T067 [P] [US3] Crear `WhoScoredJsonResponseValidator` en `backend/src/main/java/footballmarket/integrations/WhoScoredJsonResponseValidator.java` que acepte JSON con MIME HTML y clasifique estructura inválida como `PROVIDER_STRUCTURE_CHANGED`
- [X] T068 [P] [US3] Crear `JsonMetricAssembler` en `backend/src/main/java/footballmarket/services/internal/JsonMetricAssembler.java` que traduzca la respuesta a definiciones del catálogo aplicando las 57 columnas declaradas
- [ ] T069 [P] [US3] **Retirada el 2026-10-18** con el contraste entre canales. `MetricComparisonRule` no forma parte del modelo vigente
- [ ] T070 [US3] **Retirada el 2026-10-18** con el contraste entre canales. `MetricComparisonRecorder` no forma parte del modelo vigente
- [X] T071 [US3] Crear `JsonStatisticsUpdateService` en `backend/src/main/java/footballmarket/services/JsonStatisticsUpdateService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/JsonStatisticsUpdateServiceImpl.java` con actualización independiente, ausencia que no borra observación y prevalencia por observación más reciente con desempate por `id`
- [X] T072 [US3] Implementar la entrada JSON sin argumentos funcionales en `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsSynchronizationServiceImpl.java`: fijar fecha mediante Clock UTC al inicio y snapshot de elegibles, recorrer todos sus pendientes por ID local ascendente sin `maxPlayers`, corte de lote ni cliente de encadenamiento; reutilizar selector/pool/pacer/auditoría, no el corte temporal de `ExtractionBatchExecutor`; omitir confirmados del día, cero trabajo sin proveedor, una pasada por run con retry técnico limitado, cierre controlado por fallo global/interrupción, cobertura vigente y fecha inicial fija al cruzar medianoche; sin navegador ni transacción durante acceso externo

**Checkpoint**: US1, US2 y US3 independientes; la unión de canales cubre las 60 métricas.

**Avance de US03 — 2026-10-16**: implementación autorizada de T065–T072 y escritura de
T060–T064. El canal JSON consulta las quince combinaciones recorridas sobre `whoScoredRestClient` y
el pacer existentes, valida `playerTableStats`/`statColumns` aunque el MIME sea `text/html`, clasifica
la forma inválida como `PROVIDER_STRUCTURE_CHANGED` y reintenta una sola vez ante timeout o
transporte. `synchronizeJson()` fija la fecha UTC al inicio, toma una instantánea de elegibles,
recorre todos sus pendientes en una pasada con `jsonWorkers` y sin `maxPlayers`, sin corte temporal
y sin navegador; cero pendientes cierra `COMPLETED` sin tocar al proveedor. Las marcas de T060–T064
acreditan escritura y revisión de sus fuentes, no ejecución.

**Corrección de tests de US02 — 2026-10-16**: los cinco tests de
`PlayerStatisticsSynchronizationServiceTest`, `ExtractionBatchExecutorCutoffTest`,
`PlayerStatisticsSyncJourneyE2ETest` y `PlayerStatisticsCampaignClientE2ETest` devolvían un scrape
doblado sin filas de contexto, de modo que el escritor no confirmaba `COMPLETED` y los pendientes no
disminuían. Ahora el doble devuelve una fila de contexto real, igual que en
`PlayerStatisticsExtractionServiceTest`, y `navegadorNoDisponible` restaura el stub de `openSession()`
tras el error esperado. No se modificó código de producción para estos ajustes. Su ejecución es
humana y pendiente.

**Corrección de producción autorizada — 2026-10-16 (T052)**: la ejecución humana de
`gradlew.bat test` (796 tests, 18 fallidos) reveló que `ExtractionBatchExecutor.paceMillis` dividía
la suma de tiempos por el número total de muestras, incluidas las de duración cero, y podía devolver
un ritmo de 0 ms; con ese ritmo `capacity` dividía por cero o seleccionaba cero jugadores. Ahora solo
entran en la media las muestras con duración conocida y, sin ninguna, se conserva el valor técnico
conservador. Corrige `reanudaSinDuplicar`, `loteDeUnJugadorYCoverage` y el cliente de campaña. No
cambia el contrato de T052 ni amplía su alcance; el rerun humano sigue pendiente.

**Corrección de T069 — 2026-10-16**: `MetricComparisonRule.tolerance` construía la tolerancia como
`BigDecimal.valueOf(5, -(p+1))`, que produce `5E+1` en lugar de `5E-1`. Corregido a
`BigDecimal.valueOf(5, p+1)`: dos observaciones publicadas con dieciséis decimales dan `5E-17`
exacto, sin reescalar a seis, y la diferencia dentro de media unidad del dígito menos preciso
sigue clasificándose como `ROUNDING_EQUIVALENT`.

**NO_DATA resuelto por decisión del usuario — 2026-10-17**: conforme a FR-013 solo la confirmación
`COMPLETED` excluye a un jugador de la campaña, de modo que un jugador con resultado `NO_DATA`
permanece pendiente y vuelve a ser seleccionado en invocaciones posteriores del mismo día, en ambos
canales. Se conserva `PlayerStatisticsEligibilitySelector` sin cambios: `NO_DATA` sigue sin ser
definitivo y el jugador continúa pendiente hasta confirmarse o cambiar de día.

La fila de métricas del jugador con `NO_DATA` se **reconfirma** en vez de competir por la clave única
`(extraction_date, source, player_id)`: al volver a encontrar la fila existente, el writer la
actualiza con el contexto y las métricas de la nueva observación. Sin este tratamiento la
reinserción chocaba con `uq_player_statistics`, la confirmación revertía, la excepción controlada
llegaba hasta `ExtractionWorkerPool.forEach` y `synchronizeJson` nunca alcanzaba `runs.close(...)`,
dejando la ejecución en `RUNNING` y bloqueando el canal hasta agotar `JSON_RUN_BUDGET_MS` de 24 h.
Solo una fila `NO_LEAGUE_CONTEXT` admite reconfirmación: una fila `RESOLVED` se rechaza como campaña
repetida y sus métricas no se sobrescriben ni se degradan. La colisión en base de datos se conserva
como detección de concurrencia real entre dos ejecuciones.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia de contratos, documentación y cierre transversal.

- [ ] T073 [P] Actualizar `docs/backend/architecture.md` con los nuevos componentes de estadísticas y el contrato HTTP correspondiente, sin duplicar el inventario de `docs/backend/technologies.md` ya cubierto por T002
- [ ] T074 [P] Revisar que `specs/012-player-statistics-scraping/contracts/http.md` coincide con el OpenAPI generado y con los DTO entregados, incluido el campo `systemicallyAbsentMetrics` añadido por T086
- [ ] T075 [P] Registrar en `docs/TECHNICAL_DEBT.md` cualquier deuda técnica real detectada durante la implementación, sin resolverla ni declararla resuelta sin evidencia
- [ ] T076 [P] Verificar en `backend/src/test/` que ningún test accede a WhoScored real ni exige sesión gráfica, y que los dobles controlados creados en T004 son los únicos puntos de sustitución de proveedor
- [ ] T077 Revisar diff final, `git diff --check` y `git status --short` confirmando que el build permitido `./gradlew build -x test` desde `backend/` es el único control ejecutable aplicado
- [ ] T078 Dejar preparado para el usuario el conjunto de tests de `quickstart.md` §Verificación automatizada, sin ejecutarlos desde el agente
- [ ] T093 [P] Actualizar `specs/012-player-statistics-scraping/quickstart.md` con comandos efectivos del cliente y verificaciones de ambos POST tras T091/T072, conservando ejemplos sin secretos; preparar la captura humana de población, configuración, inicio, fin y cobertura para la medición informativa de SC-012 (unas 10 h, aproximadamente 2100 jugadores, tres workers) sin convertirla en criterio de aceptación, sin ejecutar tests ni benchmark por el agente

**Nota de trazabilidad**: las tareas T079–T090 se añadieron durante la remediación posterior a Analyze; conservan los IDs existentes T001–T078 y se ejecutan dentro de la fase e historia indicadas.

**Corrección decimal autorizada del 2026-10-16**: precisiones de T005/T013/T014/T024,
T032–T034/T080/T083 y T061/T062 sincronizadas con FR-029 y FR-048. No se cambia el
redondeo de derivadas de T025/FR-027. Se conservan exactamente los 93 IDs y las cuatro
casillas completadas T001/T002/T003/T006; no se implementan ni autorizan otras US.
El nuevo diseño requiere Analyze en una operación posterior y recuperar READY antes de
continuar IMPLEMENT; esta corrección documental no acredita tests ni resuelve TD-019.

**Corrección documental autorizada del 2026-10-16**: T001–T090 conservan IDs y casillas. Se añaden T091–T092 a US02 y T093 al seguimiento transversal. La autorización de esta corrección no autoriza implementar esas tareas ni US02/US03. El desglose requiere nuevo Analyze y reevaluación de READY; no se acredita resolución verificada de hallazgos ni avance de implementación.

**Aclaración JSON del 2026-10-16**: conserva los 93 IDs existentes. T030 define request solo para scraping; T046/T057 separan las entradas HTTP; T064/T072 cubren la población JSON completa pendiente del día UTC en un POST sin parámetros/body, sin repetir confirmados del día y sin corte temporal/cantidad. T091/T092 quedan limitadas al cliente de scraping. Las casillas permanecen sin avance; nuevo Analyze sigue pendiente.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: dependen del código fundacional; escribir T031/T033 en su checkpoint antes del código funcional de US01, manteniendo sus IDs y dejando ejecución humana pendiente
- **Polish (Phase 6)**: depende de las historias requeridas; T093 necesita T091 y T072 para los comandos efectivos de ambos canales

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2. No dependencies on other stories. MVP.
- **US2 (P2)**: Can start after Phase 2; consume el canal de scraping de US1 a través de la fachada, pero debe ser testeable de forma aislada con integraciones dobladas.
- **US3 (P3)**: Can start after Phase 2; consume la regla de vigencia de Phase 2 y reutiliza lotes de US2, sin requerir scraping en la misma operación.

### Within Each User Story

- Escribir tests backend necesarios; su ejecución y evidencia corresponden al usuario
- Records y parsers before integrations
- Integrations before services
- Services before controllers
- Core implementation before integration

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Tras el fundamento, respetar las dependencias US01 → canal de scraping, US02 → lotes/cliente, US03 → JSON; `[P]` no elimina dependencias ni concede autorización de otra historia
- All tests for a story marked [P] can run in parallel
- Entities, repositories, records, properties and exceptions within a story marked [P] can run in parallel

---

## Parallel Example: User Story 1

```text
Task: "Test del catálogo cerrado de 60 métricas en backend/src/test/java/footballmarket/models/MetricCatalogTest.java"
Task: "Test del normalizador y de la derivación en backend/src/test/java/footballmarket/models/MetricValuePipelineTest.java"
Task: "Test de la migración en backend/src/test/java/footballmarket/repositories/PlayerStatisticsMigrationTest.java"

Task: "Crear WhoScoredScrapedPlayer en backend/src/main/java/footballmarket/integrations/records/"
Task: "Crear HeadedBrowserAvailabilityChecker en backend/src/main/java/footballmarket/services/internal/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP y validar**: build permitido y evidencia de tests del usuario disponible
5. Deliver/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Cada user story debe ser completable y testeable de forma independiente
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

# Evidencia local de gates — SPEC-012

Registro append-only conforme a Constitution §8 y workflow SDD §3.2.
Contingencia: Trello no accesible, confirmado por el usuario en esta conversación.
No se conocen IDs, URLs ni columnas remotas de tarjetas de SPEC-012.
Estado remoto: desconocido. Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY.

## 2026-10-16 — READY local

- Feature: `012-player-statistics-scraping`.
- Gate: READY.
- Result: READY LOCAL.
- Responsable: agente, comprobación de preparación; no es una aprobación humana ficticia.
- Base normativa: Constitution 8.1.0 §8 y workflow SDD §3.2, enmienda autorizada
  explícitamente por el usuario en esta conversación el 2026-10-16.
- Estado evaluado: artefactos locales de esta carpeta, aún sin seguimiento Git;
  rama comprobada `feat/012-player-statistics-scraping`. No hay commit de estos artefactos.
- Fuentes: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `tasks.md`,
  `contracts/http.md`, `contracts/whoscored.md`, `quickstart.md`,
  `checklists/requirements-quality.md` y `../trello.md`.
- Prerrequisitos: script oficial `check-prerequisites.sh --json --require-tasks --include-tasks`
  satisfactorio; directorio resuelto `/mnt/c/Users/Vaqui/IdeaProjects/football-market/specs/012-player-statistics-scraping`.
  Documentos disponibles: research, data-model, contracts, quickstart y tasks.
- Checklist: 48 criterios, 48 marcados, 0 pendientes. Solo lectura; no se modificaron marcadores.
- Analyze: se conserva la evidencia previa registrada en `../trello.md`: 0 CRITICAL,
  0 HIGH, 0 MEDIUM, 3 LOW; cobertura 66/66 FR y 13/13 SC sobre 90 Tasks.
  No se ejecutó un nuevo Analyze ni se atribuye ese resultado a esta operación.
- Vigencia: no se modificaron requisitos, diseño ni desglose funcional desde esa evidencia.
  La enmienda cambia la persistencia y sincronización, no el alcance técnico evaluado.
- Decisiones operativas: las preguntas de defaults al final de `spec.md` se consideran
  resueltas por `plan.md` y `research.md`: tres workers, 2500 ms, lote de 30 minutos,
  un reintento transitorio. No se exige medir de nuevo contra el proveedor para READY.
- Dependencias conocidas: referencias WHOSCORED confirmadas de SPEC-008; navegador headed
  y sesión gráfica para ejecución real; fases 1 y 2 previas a US01. Son dependencias
  técnicas, no bloqueantes de preparación ni autorizaciones de otras historias.
- Blockers de preparación identificados en esta comprobación: ninguno.
- Siguiente acción: persistir y releer la autorización por US antes de iniciar IMPLEMENT.

### Inventario completo y estado local al cerrar READY

Todas las historias derivan de `spec.md` y pertenecen a SPEC-012; no representan tarjetas remotas.

| US | Historia | Estado local | Autorización al cerrar READY |
| --- | --- | --- | --- |
| US01 | Extraer métricas de la ficha de un jugador | READY | Pendiente de registro en el evento siguiente |
| US02 | Recorrer la población de jugadores identificados por lotes | READY | No autorizada |
| US03 | Actualizar métricas desde la interfaz JSON del proveedor | READY | No autorizada |

## 2026-10-16 — IMPLEMENT AUTHORIZATION de US01

- Feature: SPEC-012.
- User Story: US01.
- Gate: IMPLEMENT AUTHORIZATION.
- Result: AUTHORIZED LOCAL.
- Validated by: HUMAN.
- Responsable de la decisión: usuario de esta conversación.
- Evidencia humana literal: «autorizo a implementar US01 y sus fases 1 y 2 para su implementación.»
- Confirmación posterior: el usuario autorizó explícitamente la enmienda de contingencia y,
  ante la propuesta de comprobar y persistir READY local y esta autorización, respondió
  «Si porfavor.» en esta ejecución.
- Alcance autorizado: fase 1 T001–T006, fase 2 T007–T030 y US01 T031–T044,
  T079–T085, según `tasks.md`. T001–T030 son prerrequisitos compartidos, no Tasks
  asignadas artificialmente a US01. Su implementación no autoriza US02 ni US03.
- Exclusiones: Tasks propias de US02 y US03, fase 6 T073–T078 como alcance transversal
  de Feature, cambios funcionales upstream y cierre de Feature. Se mantienen las
  verificaciones y sincronizaciones exigibles al código efectivamente modificado.
- Base evaluada y vigencia: mismos artefactos del evento READY anterior, sin cambios
  funcionales posteriores. READY LOCAL vigente; no hay autorización invalidada conocida.
- Orden técnico: Setup → Foundational → US01, con escritura de tests backend necesarios;
  la ejecución de tests corresponde al usuario y permanece pendiente.
- Estado local de US01: READY, autorizada; IMPLEMENT todavía no iniciado.
- Estado local de Feature: READY. US02 y US03 permanecen READY sin autorización.
- Siguiente acción permitida: leer normas y código backend aplicables, registrar toma
  al comenzar efectivamente IMPLEMENT y ejecutar exclusivamente el alcance autorizado.
- Gates posteriores: CODE REVIEW y TESTING pendientes; no aprobados por esta autorización.

## Sincronización remota pendiente

Crear o reutilizar épica y una tarjeta por cada US formal, vincularlas, registrar URLs
reales en `../trello.md` y trasladar estos eventos con su fecha y procedencia originales.
Releer antes de escribir, evitar duplicados y verificar lectura posterior. Conservar
este historial. No declarar SYNCED hasta completar la conciliación remota.

## 2026-10-16 — Corrección arquitectónica autorizada; READY pendiente de reevaluación

- Feature: SPEC-012; alcance afectado: prerrequisitos compartidos T024–T025.
- Gate: READY.
- Result: INVALIDATED LOCAL, pendiente de nuevo Analyze sobre el diseño corregido.
- Responsable: agente; invalidación por impacto, no aprobación humana.
- Reason: `plan.md` y `tasks.md` cambian la asignación arquitectónica de
  `MetricValueNormalizer` y `MetricDerivationApplier` de `services/internal/` a `models/`
  para cumplir `docs/backend/architecture.md` §1.4. No cambia el comportamiento funcional.
- Autorización de la corrección: el usuario respondió «Si» a la propuesta explícita de
  corregir de forma acotada `plan.md` y `tasks.md`, preservando IDs, y realizar después
  un nuevo Analyze antes de retomar IMPLEMENT.
- Fuentes evaluadas: diff local de `plan.md` §Project Structure y `tasks.md` T024–T025;
  arquitectura del backend §1.4 y workflow SDD §§6–7.
- Impacto: la preparación global requiere reevaluación porque cambió el diseño compartido
  que habilita US01–US03. Se conserva el historial previo de READY y Analyze.
- Autorización de IMPLEMENT de US01 y fases 1–2: conservada para el mismo alcance funcional;
  su ejecución queda suspendida hasta recuperar READY vigente. US02 y US03 siguen sin autorización.
- Inicio de IMPLEMENT: no realizado. Ninguna Task completada ni gate posterior aprobado.
- Siguiente acción: nuevo Analyze en una operación posterior; no se ejecuta en esta corrección.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; trasladar también este evento al recuperar acceso.

## 2026-10-16 — Evidencia del Analyze previo a la ampliación documental

- Feature: SPEC-012.
- Operación: Analyze, ejecutado en modo estrictamente de lectura en esta conversación,
  después de corregir T024–T025 y antes de los cambios documentales siguientes.
- Responsable: agente; no es gate humano aprobado.
- Resultado observado del informe: 1 CRITICAL, 3 HIGH, 2 MEDIUM y 1 LOW sobre 79 requisitos
  (66 FR y 13 SC) y 90 Tasks. La cobertura nominal de asociaciones no acreditó suficiencia.
- Blockers: C1, ubicación de `MetricComparisonRule`; G1, responsable del encadenamiento;
  I1, objetivo temporal incompatible con la estimación empleada; I2, unicidad de campaña
  sin mecanismo de esquema definido. U1 e I3 señalaron ritmo inicial/persistencia y orden
  de tests fundacionales; D1 señaló redundancia de FR-011/FR-061.
- Fuente: `Specification Analysis Report` emitido en esta conversación; artefactos locales
  previos a la revisión autorizada. Esta persistencia ocurre fuera de Analyze.
- READY: no recuperado. Ningún test ejecutado ni archivo modificado durante aquel Analyze.

## 2026-10-16 — Decisiones humanas y corrección documental del contrato

- Feature: SPEC-012; historias afectadas: US01–US03 por diseño compartido y contrato.
- Operación: corrección documental autorizada; no IMPLEMENT ni nuevo Analyze.
- Validated by: HUMAN, exclusivamente para las decisiones y autorización documental citadas.
- Evidencia humana: el usuario confirmó que el cliente lo ejecuta el desarrollador y
  precisó «puede llegar a un durar 10h una jornada con los 3 workers trabajando».
  Ante la propuesta de actualizar spec, plan, modelo, contratos y tareas con `maxPlayers`,
  modos lote/campaña y correcciones, respondió «si porfavor» y aclaró: «el sync, son dos
  enpoints distinto : uno que hace scraping y el otro utiliza el endpoint de whoscored
  leyendo el json.»
- Decisiones persistidas: dos POST `/sync/scraping` y `/sync/json`, sin `source` en body;
  GET de cobertura por canal; `maxPlayers` opcional positivo y global entre workers;
  cliente del desarrollador con lote/campaña; objetivo de 10 h para scraping con unos
  2100 jugadores, tres workers, 2500 ms y proveedor disponible, pendiente de medición humana.
- Fuentes modificadas: `spec.md`, `plan.md`, `research.md`, `data-model.md`,
  `contracts/http.md`, `contracts/whoscored.md`, `quickstart.md` y `tasks.md`.
- Correcciones técnicas: regla de comparación en Model; FK compuesta e índice parcial
  de confirmación; ritmo reconstruible de items (ventana de 20 y arranque conservador
  de 60 s); escritura adelantada de T031/T033 sin ejecución automática. Se conservan
  IDs T001–T090 y todas las casillas; T091–T092 pertenecen a US02 y T093 es transversal.
- Gate: READY.
- Result: INVALIDATED LOCAL, continúa pendiente de nuevo Analyze y reevaluación.
- Responsable de la invalidación: agente por impacto de diseño/contrato; no aprobación humana.
- Checklists del revisor: no modificados ni reevaluados por esta corrección. Conservar
  las evaluaciones no afectadas del catálogo; comprobar aplicabilidad de las restantes
  frente al contrato y criterios revisados antes de recuperar READY.
- Estado local: Feature y US01–US03 en preparación; ninguna US iniciada. `spec.md`
  refleja `Ready for planning`; esto no implica aprobación del diseño revisado.
- Implementación, CODE REVIEW, TESTING y cierre: no iniciados ni aprobados por esta operación.
- Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY. Trasladar los eventos, actualizar
  alcance/Tasks y asociaciones en tarjetas reales y verificar lectura posterior al recuperar acceso.
- Siguiente acción: nuevo Analyze en una operación posterior; ninguna resolución se
  presenta como verificada por Analyze ni se ejecuta el proveedor para validar rendimiento.

## 2026-10-16 — Impacto acotado sobre IMPLEMENT AUTHORIZATION de US01

- Feature: SPEC-012; User Story: US01.
- Gate: IMPLEMENT AUTHORIZATION.
- Result: INVALIDATED, únicamente para la ampliación contractual de los prerrequisitos
  T029–T030 (`maxPlayers` y canal fijado por rutas separadas) respecto de la base autorizada.
- Reason: la autorización humana actual corresponde a corrección documental, no acredita
  autorización de implementar el contrato ampliado. La autorización histórica se conserva
  para las partes no afectadas; no se invalida toda US01 ni se borra evidencia previa.
- Responsable: agente, evaluación de impacto; no aprobación humana.
- Tareas T005/T010/T011/T026/T033 se precisan para coherencia del límite y la unicidad;
  la implementación del alcance revisado deberá explicitar estos prerrequisitos al
  revalidar la autorización después de Analyze/READY, sin consumir nuevas tareas por defecto.
- US02, US03 y tareas nuevas T091–T093: sin autorización de implementación.
- Siguiente acción: tras recuperar READY, confirmar autorización solo del alcance
  de implementación afectado antes de iniciarlo. Ninguna tarea ha comenzado.

## 2026-10-16 — JSON sin entrada y confirmación de idempotencia diaria

- Feature: SPEC-012; alcance afectado: contrato/fachada compartidos T029–T030,
  T046/T055/T057, recorrido JSON T063–T064/T072 y alcance del cliente T091–T092.
- Operación: cierre documental autorizado, no implementación ni nuevo Analyze.
- Validated by: HUMAN, para las decisiones funcionales y autorización documental.
- Evidencia humana: «Quiero que el json no reciba parametros ni body, recorred los
  2100 jugadores(aprox).» Ante la pregunta de repetir todos o solo pendientes del día,
  el usuario confirmó «No, si es el mismo dia no hace falta» y solicitó «Podes terminar
  de cerrar para ejecutar devuelta el analyze».
- Decisión: `/sync/json` solo requiere autenticación, sin query parameters ni body.
  Fija fecha UTC al iniciar y procesa todos los elegibles pendientes del snapshot inicial,
  sin máximo de jugadores ni corte de lote de 30 minutos. No repite `COMPLETED` del
  mismo jugador/fecha/JSON; sin pendientes no consulta al proveedor. Otro día UTC es
  otra campaña, y cruzar medianoche no cambia la fecha del run ya iniciado.
- Scraping conserva lotes, fecha y máximo opcionales. Pendientes derivados de la DB:
  elegibles sin `COMPLETED` para jugador/fecha/SCRAPING, incluyendo fallidos,
  interrumpidos y no intentados, ordenados por ID local. No hay cursor del cliente.
- Cliente operativo T091/T092: exclusivamente scraping. JSON se invoca directamente,
  sin encadenamiento externo ni entrada de fecha, cantidad o ID. Se conservan los
  93 IDs y casillas; ninguna Task completada ni nueva US creada.
- Fuentes: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/http.md`,
  `contracts/whoscored.md`, `quickstart.md`, `tasks.md` y diff local documental.
- Gate: READY; Result: INVALIDATED LOCAL, continúa pendiente de nuevo Analyze y
  reevaluación. No se acredita que el diseño revisado haya superado ese análisis.
- Responsable del estado: agente por impacto sobre contrato/diseño, no aprobación humana.
- Autorizaciones: se conserva el historial y el alcance no afectado. La invalidación
  acotada previa de IMPLEMENT AUTHORIZATION sobre T029–T030 sigue vigente y alcanza
  la nueva separación de entradas HTTP; esta autorización documental no autoriza
  implementar US02, US03, T091–T093 ni la ampliación contractual afectada.
- Checklists del revisor: intactos, sin nuevos marcadores ni reevaluaciones atribuidas.
- Estado local: Feature/US en preparación, IMPLEMENT no iniciado; `Status` permanece
  `Ready for planning`. Objetivo de scraping de 10 h conservado, medición humana pendiente.
- Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY. Trasladar este evento y actualizar
  las descripciones/Tasks afectadas al recuperar acceso, conservando historial e identidades.
- Siguiente acción: ejecutar `/speckit.analyze` en una operación posterior, de lectura;
  solo tras resultado suficiente podrá reevaluarse READY. No se ejecutan tests ni builds.

## 2026-10-16 — Evidencia del último Analyze y correcciones editoriales I4/I5

- Feature: SPEC-012.
- Operación: persistencia fuera de Analyze del informe favorable emitido en esta
  conversación después de la aclaración de JSON sin entrada e idempotencia diaria.
- Responsable: agente, registro de resultados observados; no aprobación humana ficticia.
- Resultado del informe: 0 CRITICAL, 0 HIGH, 0 MEDIUM y 2 LOW; 79 requisitos (66 FR y
  13 SC), 93 Tasks y cobertura nominal 79/79. No se identificaron blockers de consistencia
  ni incertidumbres fundamentales. Los hallazgos anteriores quedaron resueltos a nivel
  documental; no se atribuye implementación ni verificación de comportamiento a Analyze.
- Fuente: último `Specification Analysis Report` de esta conversación, anterior a esta
  operación; `spec.md`, `plan.md`, `tasks.md`, modelo, contratos y checklist evaluados.
- Autorización de corrección y estado: el usuario solicitó «Si porfavor, plama estas
  correciones y deja el estado en Ready» después de recibir el informe con I4/I5.
- I4: `spec.md`, bloque estructurado, referencia FR-024 sustituida por FR-030.
- I5: `data-model.md`, `contracts/http.md` y `quickstart.md`, terminología común
  «lote» sustituida por «ejecución» en los puntos indicados por el informe.
- Impacto evaluado: correcciones exclusivamente editoriales, sin cambios de comportamiento,
  contrato efectivo, diseño, Tasks, criterios ni fórmulas. Las actualizaciones de estado en
  `spec.md`, `plan.md`, `tasks.md` e índice son metadatos de seguimiento, no nuevo alcance.
- Vigencia: se conserva el último Analyze por ausencia de impacto material sobre su base;
  no se ejecutó ni se afirma un Analyze nuevo o un resultado de cero LOW. I4/I5 se cierran
  por revisión directa de las ediciones autorizadas, fuera del comando.

## 2026-10-16 — Restablecimiento de READY local

- Feature: `012-player-statistics-scraping`.
- Gate: READY.
- Result: READY LOCAL.
- Responsable: agente, comprobación de preparación conforme a Constitution 8.1.0 §8 y
  workflow SDD §3.2. La solicitud humana de dejar READY no sustituye esta evaluación.
- Base: último Analyze sin blockers registrado arriba; correcciones I4/I5 sin impacto
  material; fuentes locales de la Feature, aún sin seguimiento Git, sin hash inventado.
- Artefactos: spec, plan, research, data-model, contratos HTTP/WhoScored, quickstart y
  desglose completo de 93 Tasks disponibles y coherentes para el alcance analizado.
- Checklist: `checklists/requirements-quality.md`, 48 criterios marcados y 0 pendientes,
  leído en el último Analyze. Las correcciones editoriales no afectan los criterios
  evaluados; se conservan sus marcas y propiedad del revisor, sin acreditar implementación.
- Preparación global: sin blockers identificados. Referencias externas de SPEC-008,
  entorno headed y orden de fases/US son dependencias conocidas, no blockers de READY.
- Estado documental: `spec.md` pasa a `Ready for implementation`; los metadatos de
  plan/tareas/índice reflejan el cierre local sin sobrescribir eventos anteriores.

### Inventario y estado local vigente

| Entidad | Relación | Estado local | Autorización de IMPLEMENT |
| --- | --- | --- | --- |
| SPEC-012 | Feature | READY | No es unidad ejecutable |
| US01 | Extraer métricas de la ficha de un jugador; pertenece a SPEC-012 | READY | Histórica conservada en alcance no afectado; pendiente confirmar ampliación contractual T029–T030 y prerrequisitos revisados antes de ejecutarlos |
| US02 | Recorrer la población de jugadores identificados por lotes; pertenece a SPEC-012 | READY | No autorizada |
| US03 | Actualizar métricas desde la interfaz JSON del proveedor; pertenece a SPEC-012 | READY | No autorizada |

- IMPLEMENT no iniciado; ninguna Task marcada, US movida a IN PROGRESS ni gate posterior
  aprobado. T091–T093 siguen sin autorización de implementación. Esta solicitud de READY
  no renueva ni amplía IMPLEMENT AUTHORIZATION, CODE REVIEW, TESTING o cierre de Feature.
- Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY. Trello sigue sin acceso en esta
  sesión y su estado remoto es desconocido. No se inventan tarjetas, IDs, URLs ni columnas.
- Pendientes remotos: releer tablero al recuperar acceso, crear o reutilizar épica y las
  tres tarjetas US sin duplicar, vincularlas, trasladar los eventos locales con procedencia
  y fechas originales, registrar URLs y verificar lectura posterior antes de declarar SYNCED.
- Siguiente acción permitida: confirmar únicamente la autorización de implementación
  afectada de US01 y sus prerrequisitos revisados si el usuario quiere iniciar ese trabajo;
  conservar autorizaciones no afectadas y registrar toma solo al comenzar efectivamente.

## 2026-10-16 — Renovación de IMPLEMENT AUTHORIZATION de US01

- Feature: SPEC-012; User Story: US01.
- Gate: IMPLEMENT AUTHORIZATION.
- Result: AUTHORIZED LOCAL.
- Validated by: HUMAN.
- Responsable: usuario de esta conversación.
- Evidencia humana: respuesta «si» a la pregunta explícita «¿Autorizás implementar US01
  con sus fases 1 y 2 —T001–T044 y T079–T085— conforme al contrato vigente, incluida la
  ampliación de T029–T030 y los prerrequisitos revisados T005/T010/T011/T026/T033?».
- Alcance: fases 1–2 T001–T030 y US01 T031–T044/T079–T085, según los artefactos
  vigentes. Se renueva el alcance contractual afectado y se conserva la autorización no
  afectada. No autoriza US02, US03, T073–T078 ni T086–T093.
- Base: READY LOCAL restablecido en el evento anterior; último Analyze sin blockers,
  checklist requirements-quality con 48 criterios marcados y cero pendientes. No se
  modifican requisitos, diseño ni desglose por esta renovación.
- Prerrequisitos comprobados: script oficial con --json --require-tasks --include-tasks
  satisfactorio; FEATURE_DIR absoluto /mnt/c/Users/Vaqui/IdeaProjects/football-market/specs/012-player-statistics-scraping;
  research, data-model, contracts, quickstart y tasks disponibles. Rama efectiva:
  feat/012-player-statistics-scraping.
- Dependencias: catálogo con referencias confirmadas, Chromium headed y sesión gráfica
  para uso real; orden Setup → Foundational → US01. Tests exclusivamente a cargo del usuario.
- Estado al registrar autorización: US02 READY; inicio efectivo tomado en esta misma operación.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; sin herramientas de Trello disponibles en esta
  sesión, estado remoto desconocido. Trasladar este evento al recuperar acceso.
- CODE REVIEW, TESTING y cierre de Feature: pendientes, no aprobados.

## 2026-10-16 — Implementación de US02 y cierre de la dependencia de T036

- Alcance ejecutado: T045–T059, T086–T092 y T036 de US01, más los ajustes de integración
  imprescindibles sobre el código de US01 sin alterar sus contratos ni su comportamiento.
- Componentes creados: `PlayerStatisticsEligibilitySelector`, `ExtractionRunManager`,
  `ExtractionBatchExecutor`, `ExtractionWorkerPool`, `PlayerStatisticsCoverageQuery`,
  `PlayerStatisticsSynchronizationService(+Impl)`, `PlayerStatisticsCoverageService(+Impl)`,
  `PlayerStatisticsController` y el cliente `tools/PlayerStatisticsSyncClient` con la tarea
  Gradle `statisticsSyncClient`.
- Consultas nuevas: `countEligiblePlayers` por proveedor y conteos de jugadores distintos
  confirmados y sin confirmación por fecha y canal.
- Decisiones: la ejecución activa del mismo canal solo se recupera como `INTERRUPTED` cuando su
  inicio excede el presupuesto del lote; en caso contrario la apertura responde
  `PLAYER_STATISTICS_SYNC_IN_PROGRESS`. El navegador no se abre cuando no hay pendiente real.
  `providerCalls` del lote se contabiliza como navegaciones iniciadas por sección recorrida y no
  incluye reintentos. El cliente expone una sobrecarga con entorno explícito para poder invocarlo
  con un entorno controlado sin alterar su comportamiento productivo.
- T087: el aplazamiento por `Retry-After` ya estaba cubierto en T044 y el ejecutor de lote deja de
  seleccionar jugadores ante indisponibilidad prolongada, registra el fallo del jugador y cierra
  con lo alcanzado y pendientes explícitos.
- Rutas: `POST /api/players/statistics/whoscored/sync/scraping` operativa con
  `{"maxPlayers": 1}`; `GET /api/players/statistics/whoscored/coverage` operativa;
  `POST /api/players/statistics/whoscored/sync/json` operativa en contrato y rechazo de entrada,
  con actualización real pendiente de T072.
- Verificación: `gradlew.bat build -x test` desde `backend/`, BUILD SUCCESSFUL en 9 s. No se
  ejecutaron tests; las marcas acreditan escritura y revisión de fuentes. Se compiló además la
  fuente de tests con `compileTestJava` para detectar errores de compilación, sin ejecutar prueba
  alguna.
- Riesgo real detectado: el conteo de `providerCalls` del lote no incorpora los reintentos
  transitorios admitidos, por lo que es un mínimo declarado. No se alteró el contrato HTTP.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; trasladar avance, decisiones y resultados al recuperar
  acceso. CODE REVIEW y TESTING de US02 y US01 pendientes.

## 2026-10-16 — Remediación de regresión global de la suite por mapping ambiguo

- Evidencia humana leída, no ejecutada por el agente: reportes locales de
  `backend/build/test-results/test/` de la ejecución del usuario. Causa raíz única para casi toda
  la suite: `Ambiguous mapping ... PlayerStatisticsController#synchronizeScraping` contra
  `PlayerStatisticsContractFoundationTest$ContractFixture#scraping`, que replicaba
  `POST /sync/scraping` y `GET /coverage`. El fallo rompía la carga del contexto completo y, por
  cascada, todos los `@SpringBootTest` y E2E (persistencia, seguridad, servicios, arranque).
- Corrección: el fixture de test se limita a la ruta exclusiva `/foundation-error` para los
  códigos del advice; la validación del body de scraping, la fecha, el máximo y el filtro de
  cobertura se verifican ahora contra el Controller real en `PlayerStatisticsControllerTest`, sin
  perder esas aserciones. No se modificó código productivo.
- Segunda causa, independiente: `WhoScoredStatisticsRequestPacerTest` fallaba por expectativas
  propias (espera restante de 1500 ms y `retrieve()` lanzando ante 429). Corregidas ambas; el
  aplazamiento por `Retry-After` ya estaba cubierto por T044.
- Verificación: `gradlew.bat build -x test` desde `backend/`, BUILD SUCCESSFUL; `compileTestJava`
  sin errores. Tests no ejecutados por el agente; persiste la necesidad de una ejecución humana
  completa para confirmar la ausencia de otras regresiones.

## 2026-10-16 — Segunda remediación de tests de US02

- Evidencia humana leída: reportes de `backend/build/test-results/test/` de la ejecución del
  usuario tras la remediación anterior.
- `PlayerStatisticsSynchronizationServiceTest`, `ExtractionBatchExecutorCutoffTest` y
  `PlayerStatisticsSyncJourneyE2ETest` seleccionaban cero jugadores: con el perfil `test`
  (presupuesto de lote de 60 s) y el ritmo conservador de arranque de 60 s por jugador, ningún
  jugador cabe en el presupuesto y la selección es legítimamente cero. Los tests ahora fijan el
  presupuesto productivo de 30 minutos mediante un spy de `PlayerStatisticsProperties`, sin
  cambiar la regla de corte de `plan.md`.
- `PlayerStatisticsCoverageTest` abría dos ejecuciones activas del mismo canal y violaba
  `uq_statistics_running_source`. El helper registra ahora todos los resultados en una sola
  ejecución, respetando la restricción de esquema.
- `PlayerStatisticsCampaignClientE2ETest`: dos defectos reales del cliente. El modo se aceptaba
  con cualquier valor distinto de `campaign`; ahora solo admite `batch` o `campaign`. El bucle de
  campaña comparaba los pendientes iniciales contra sí mismo y terminaba sin intentar ningún lote;
  ahora encadena lotes mientras la cobertura disminuye y exige progreso real entre lotes.
- Verificación: `gradlew.bat build -x test` desde `backend/`, BUILD SUCCESSFUL; `compileTestJava`
  sin errores. Tests no ejecutados por el agente; se requiere nueva ejecución humana.

## 2026-10-16 — Inicio efectivo de IMPLEMENT de US01

- Feature: SPEC-012; User Story: US01.
- Operación: IMPLEMENT, toma efectiva de fases 1–2 y US01 autorizadas.
- Result: IN PROGRESS LOCAL.
- Responsable: agente, ejecución de la autorización humana renovada y releída.
- READY LOCAL vigente según el último evento de restablecimiento; renovación de
  IMPLEMENT AUTHORIZATION persistida arriba y releída antes de modificar código.
- Primer cambio: declaración de Playwright 1.63.0 (T001) e inventario tecnológico
  correspondiente (T002). Verificación del build sin tests pendiente.
- Feature y US01: IN PROGRESS local; US02 y US03 conservan READY sin autorización.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; trasladar toma y estado agregado al recuperar acceso.
- Tests, CODE REVIEW, TESTING y cierre de Feature: pendientes, sin aprobación implícita.

## 2026-10-16 — Avance de configuración y bloqueo de precisión

- Feature: SPEC-012; US01 y prerrequisitos compartidos.
- Operación: IMPLEMENT; estado de ejecución BLOCKED, motivo de diseño, no de Java.
- Trabajo completado: T001 Playwright 1.63.0 declarado y consumido; T002 inventario
  tecnológico; T003 defaults whoscored.statistics; T006 overrides de testing sin proveedor real.
- Verificación: revisión de diff y build backend sin tests. `bash ./gradlew build -x test`
  falló antes de Gradle por falta de Java/JAVA_HOME en Bash. `java -version` desde Windows
  confirmó Temurin 21.0.11. El wrapper Windows equivalente `gradlew.bat build -x test`
  fue satisfactorio; la última ejecución tras modificar ambos YAML terminó BUILD SUCCESSFUL
  en 6 s, 10 tareas (4 ejecutadas, 6 up-to-date). No hubo tareas de ejecución de tests.
- No se ejecutaron tests, arranque de aplicación, navegador ni consultas al proveedor.
  El build no acredita binding funcional, migraciones ni pruebas de extracción.
- Hallazgo comprobado: `RecorridoHTML/PaginaPrincipal.txt`, línea 1219, bloque tournaments,
  publica `Rating: 6.9033333333333333`. FR-029 exige precisión publicada sin redondeo;
  data-model.md, research.md y T005/T013 prescriben NUMERIC(18,6), que no puede conservarla.
- Impacto: T005/T013/T024 y la extracción/persistencia del rating necesitan un diseño
  compatible. No se permite truncarlo, redondearlo ni rechazarlo por exceder seis decimales.
- Gate: READY; Result: INVALIDATED LOCAL, por impacto comprobado sobre diseño compartido.
  Responsable: agente; no es aprobación humana ni un nuevo Analyze.
- Autorización: se conserva la renovación de US01 y fases 1–2 para el mismo alcance funcional;
  continuación suspendida hasta corrección documental autorizada y Analyze/READY vigentes.
  US02/US03 sin autorización; sus evaluaciones no afectadas se conservan.
- Código incompleto elaborado durante esta ejecución para migración/modelo/configuración
  Java retirado antes de la entrega para no dejar un esquema que pierda precisión. Solo se
  retiraron archivos nuevos de esta ejecución; ningún archivo preexistente fue eliminado.
- Estado: Feature y US01 con IMPLEMENT iniciado, BLOCKED; preparación global pendiente.
  No se entra a CODE REVIEW ni se aprueba ningún gate posterior. Checklist del revisor intacto.
- Seguimiento: TD-019 registra el conflicto sin darlo por resuelto. T004/T005 y las fases
  posteriores siguen sin completar. T001/T002/T003/T006 marcadas por trabajo verificado.
- Siguiente acción propuesta: autorizar corrección acotada del almacenamiento decimal
  para conservar precisión publicada (p. ej. NUMERIC sin escala fija), sincronizando diseño
  y Tasks sin renumerar ni perder avance, seguida de Analyze en una operación posterior.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; estado remoto desconocido. Trasladar avance,
  invalidación y bloqueo al recuperar acceso. No se atribuyen cambios remotos.

## 2026-10-16 — Corrección documental decimal autorizada

- Feature: SPEC-012; alcance: precisión de observaciones y de auditoría de contraste.
- Operación: corrección documental fuera de Analyze e IMPLEMENT; no nuevo Analyze.
- Validated by: HUMAN, exclusivamente para la autorización de corregir el diseño.
- Evidencia humana: «lo autorizo», en respuesta a la propuesta explícita «¿Autorizás
  corregir el diseño de almacenamiento decimal para conservar la precisión publicada
  —recomiendo NUMERIC sin escala fija— y sincronizar los artefactos y Tasks afectados,
  preservando IDs y avance?». La propuesta indicaba nuevo Analyze antes de retomar IMPLEMENT.
- Responsable de la decisión: usuario de esta conversación; ejecución documental: agente.
- Decisión: PostgreSQL NUMERIC sin precisión/escala fijas para `value`, `tolerance` y
  `difference`; BigDecimal desde los tokens originales del bloque estructurado/JSON hasta
  persistencia y operaciones de contraste, sin double/float ni límite de seis decimales.
  Mappings JPA explícitos a numeric sin precisión/escala fijas, coherentes con Flyway.
- Ejemplo de regresión: rating `6.9033333333333333`, precisión 16, ida/vuelta exacta;
  tolerancia con ambos canales a precisión 16: `0.00000000000000005`, no cero por redondeo.
  Solo cocientes por apps mantienen seis decimales HALF_UP. FR-027/FR-029/FR-048 intactos.
- Fuentes sincronizadas: data-model.md, research.md, plan.md, contracts/whoscored.md,
  quickstart.md, metadatos de spec.md/tasks.md, ../trello.md y ../../docs/TECHNICAL_DEBT.md.
  El contrato HTTP no expone estos decimales y no cambia.
- Tasks precisadas: T005/T013/T014/T024, T032/T033/T034, T080/T083 y T061/T062.
  Se conservan los 93 IDs, las cuatro casillas completas T001/T002/T003/T006 y todas las
  restantes pendientes. No se crean Tasks ni se altera el redondeo de T025.
- Gate: READY; Result: PENDING REEVALUATION LOCAL. Sigue sin vigencia tras la invalidación
  anterior: la edición documental no acredita Analyze suficiente ni recupera READY.
- Autorización funcional de IMPLEMENT de US01/fases 1–2 conservada, con ejecución suspendida.
  US02/US03 y Tasks ajenas siguen sin autorización. No se invalidan otros gates no afectados.
- Checklist del revisor: intacto, sin modificación de criterios ni marcadores. Su referencia
  previa a NUMERIC(18,6) pertenece al historial de revisión; no es el diseño vigente ni una
  reevaluación atribuida al revisor. Analyze deberá evaluar la coherencia del diseño corregido.
- Verificaciones: revisión documental de precisión, rutas y referencias; sin código funcional,
  migraciones, build, tests, navegador ni consulta al proveedor en esta operación.
- TD-019 continúa Pendiente: se registra la corrección, sin declarar resuelta su implementación.
- Estado: continuación de US01 BLOCKED hasta nuevo Analyze y reevaluación global de READY;
  sin transición a CODE REVIEW, TESTING, DONE o cierre de Feature.
- Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY; sin herramientas de Trello en
  esta sesión, estado remoto desconocido. Trasladar este evento y las referencias actualizadas
  al recuperar acceso, preservando procedencia, fechas e identidades y verificando lectura posterior.
- Siguiente acción: ejecutar /speckit.analyze en una operación posterior sobre el diseño
  corregido; esta autorización documental no acredita su resultado ni inicia IMPLEMENT.

## 2026-10-16 — Corrección de estado posterior al Analyze decimal

- Feature: SPEC-012; User Story en ejecución: US01.
- Gate: READY.
- Result: READY LOCAL RESTORED.
- Validated by: HUMAN, respecto de la ejecución previa del Analyze y la instrucción de
  corregir el estado; la evaluación de READY y su registro corresponden al agente.
- Evidencia humana literal: «ya fue ejecutado anteriormente, corregir el estado y continuar
  con la implementacion», en respuesta a la advertencia de que el registro aún exigía el
  Analyze posterior a la corrección decimal.
- Alcance: diseño decimal vigente de `spec.md`, `plan.md`, `research.md`, `data-model.md`,
  `contracts/whoscored.md`, `quickstart.md` y `tasks.md`, junto con el checklist existente.
- Resultado detallado: el informe de esa ejecución previa no está disponible en esta sesión;
  no se atribuyen conteos de severidad ni hallazgos inventados. La confirmación humana resuelve
  el estado operativo desactualizado y no modifica requisitos, diseño ni alcance autorizado.
- Preparación: la revisión de los artefactos vigentes no identifica un blocker pendiente en
  la corrección decimal; NUMERIC sin escala fija, BigDecimal directo y HALF_UP exclusivo de
  derivadas permanecen coherentes entre diseño, contrato y Tasks.
- IMPLEMENT AUTHORIZATION: se conserva la renovación vigente de US01 para T001–T044 y
  T079–T085, incluidos los prerrequisitos compartidos. US02/US03 y T073–T078/T086–T093
  continúan sin autorización.
- Estado local: Feature y US01 IN PROGRESS; se reanuda IMPLEMENT desde T004/T005. CODE
  REVIEW, TESTING y cierre permanecen pendientes.
- Sincronización: TRELLO OUT OF SYNC — LOCAL CONTINGENCY; estado remoto desconocido.
  Trasladar este evento al recuperar acceso sin atribuirle una transición remota.

## 2026-10-16 — Avance fundacional reanudado de US01

- Feature: SPEC-012; US01 IN PROGRESS LOCAL.
- Operación: IMPLEMENT dentro del alcance autorizado de fases 1–2 y US01.
- Trabajo completado en esta reanudación: T005, T007–T015, T022–T025 y T028.
  Se conservan T001/T002/T003/T006 completadas anteriormente.
- Implementación: migración V10 con seis tablas, FK compuesta e índice parcial de campaña;
  catálogo cerrado 60/52/57/49; modelos persistentes con NUMERIC sin escala fija; records,
  properties, normalización decimal, derivación HALF_UP y excepciones controladas.
- Verificación observada: `gradlew.bat build -x test` satisfactorio después de aplicar el
  formateador del proyecto; 10 tareas, 6 ejecutadas y 4 up-to-date. No se ejecutaron tests.
- Pendientes inmediatos: T004, consultas especializadas T016–T021, writer/recorder
  T026–T027, contrato HTTP T029–T030 y tests fundacionales T031–T034 antes del recorrido
  funcional de scraping. Las interfaces Repository creadas como base no acreditan por sí
  solas esas Tasks mientras falten sus consultas completas.
- Gates posteriores: CODE REVIEW y TESTING pendientes. El build no acredita comportamiento,
  migración aplicada ni tests; TD-019 continúa pendiente hasta evidencia PostgreSQL humana.
- Trello: TRELLO OUT OF SYNC — LOCAL CONTINGENCY; trasladar este avance al recuperar acceso.

## 2026-10-16 — Continuación del fundamento autorizada por el usuario

- Feature: SPEC-012; User Story: US01, IN PROGRESS LOCAL.
- Operación: IMPLEMENT; continuación del alcance previamente autorizado.
- Evidencia humana: «Muy bien, porfavor termina las tareas pendientes que mencionaste.»
  Referencia inequívoca: T004, T016–T021, T026–T030 y T031–T034 de la entrega anterior.
- Trabajo: dobles HTTP/Playwright sin Internet ni sesión gráfica, consultas de pendientes,
  observaciones vigentes y contraste; writer transaccional por jugador e incidencias saneadas;
  Request DTO exclusivo de scraping, Response DTO comunes, mapper y errores HTTP.
- Regresiones escritas: catálogo/cobertura/aliases, ausencia y precisión decimal,
  HALF_UP, restricciones Flyway, rollback y selección histórica con JPA/PostgreSQL.
  `PlayerStatisticsContractFoundationTest` verifica DTO/mapper/advice con una frontera MVC
  exclusiva de test y Security real; no implementa el Controller de US02 ni su fachada.
- Ajustes de coherencia: FK compuesta conservada en V10; relación JPA por `run_id` con
  fecha/canal copiados e inmutables. Rating opcional y aliases completos en catálogo;
  columnas compartidas por acumulados y derivadas priorizan la definición directa.
- Tests: ejecución exclusivamente humana, pendiente. No se abren navegador ni proveedor
  real. TD-019 continúa pendiente por los criterios de cierre documentados.
- Verificación y cierre técnico de esta continuación: pendientes de build y revisión final.
- Autorizaciones y gates posteriores: no se autoriza ni implementa US02/US03. No se aprueba
  CODE REVIEW, TESTING ni cierre de Feature. Trello sigue OUT OF SYNC — LOCAL CONTINGENCY.

## 2026-10-16 — Verificación del cierre fundacional T001–T034

- Feature: SPEC-012; US01 IN PROGRESS LOCAL.
- Alcance concluido de esta continuación: T004, T016–T021, T026–T030 y escritura de
  T031–T034. Fases 1–2 y tests fundacionales escritos; no está completo el recorrido de US01.
- Verificación ejecutada: `gradlew.bat build -x test` desde `backend/`, BUILD SUCCESSFUL
  en 8 s, 10 tareas (5 ejecutadas, 5 up-to-date). Las ejecuciones previas encontraron
  diferencias de formato en las fuentes nuevas; se corrigieron antes del resultado final.
- No se ejecutó `spotlessApply`, tests, compilación separada de tests, aplicación,
  navegador, proveedor real ni migración sobre una base externa.
- Revisión estática: consultas y consumidores, límites transaccionales, conservación del
  decimal y restricciones de esquema; tests sin `this`, con `@Nested`, PostgreSQL
  Testcontainers/Flyway y dobles controlados de proveedor.
- Tests pendientes del usuario: `MetricCatalogTest`, `MetricValuePipelineTest`,
  `PlayerStatisticsMigrationTest`, `PlayerStatisticsPersistenceTest` y
  `PlayerStatisticsContractFoundationTest`. El build no acredita compilación ni ejecución
  de estas pruebas; los dos tests Repository requieren Docker disponible.
- Tracking: casillas T001–T034 completadas por implementación/escritura y revisión de
  fuentes; ejecución funcional pendiente, sin aprobar TESTING. TD-019 sigue pendiente.
- Próximo alcance de US01: T035–T044/T079–T085, recorrido de scraping y sus tests.
  Gates humanos y sincronización remota permanecen pendientes según su alcance ordinario.

## 2026-10-16 — Continuación de US01 y dependencia de T036

- Feature: SPEC-012; US01 IN PROGRESS LOCAL, mismo alcance autorizado.
- Evidencia humana: «Okey, segui con las siguientes tareas», referido a T035–T044/T079–T085.
- Decisión posterior: el usuario eligió «Mantener diseño actual» ante la alternativa de
  incorporar apertura/cierre de ejecución individual en US01 para preparar T036 mediante
  Service. No se autoriza ese ajuste ni se implementa infraestructura de US02.
- Dependencia identificada: el Service de extracción necesita un run persistido. T036 no
  puede preparar datos funcionales por Repository/JPA; su caso exitoso y el cierre de US01
  quedan pendientes de la API productiva de runs asignada a US02. No se crean APIs solo
  para tests ni se elude la frontera normativa. Es dependencia técnica, no cambio funcional
  ni invalidación automática de READY.
- Continúan las Tasks independientes del recorrido, parsers, pacing y startup. T036
  conserva su casilla pendiente hasta resolver la dependencia y escribir su cobertura.
- Trello: TRELLO OUT OF SYNC — LOCAL CONTINGENCY, sin cambios remotos ni gates aprobados.

## 2026-10-16 — Avance del recorrido de scraping

- Implementación: T037–T044/T079–T082; escritura de T035/T083–T085. T036 pendiente
  de US02 conforme a la decisión humana anterior. No se declara US01 terminada.
- Fuente DOM consultada: capturas locales ignoradas de `RecorridoHTML/`, especialmente
  `PaginaPrincipal.txt` §§Participaciones Actuales y require.config.params.args; tablas
  detalladas y defensiva. No se publican las capturas ni se accede a WhoScored.
- Selectores: `data-stat-name`, `#player-tournament-stats-options`, `#category`,
  `#statsAccumulationType`; 18 contextos independientes. Se solicita Total (`2`) en detalle
  antes de aplicar la división por apps, y se conserva el HTML original para el rating.
- Contexto: competición/equipo desde enlaces de la fila; la temporada se enlaza mediante
  IDs explícitos del bloque original y enlaces de temporada publicados en la misma página.
  La captura no repite el ID de temporada en cada fila. No se elige entre temporadas
  ambiguas, ni se resuelven identidades por nombres: se rechaza el contexto incompleto.
- Ciclo de vida: creación explícita de sesión headed al abrir el lote, sin User-Agent
  sobrescrito ni descargas de navegador en startup; cierre del navegador y driver al salir.
  Las llamadas Playwright comparten un monitor por sección porque su API Java no admite
  uso concurrente sin sincronización. El pacer estadístico mantiene estado por worker, sin
  serializar identidad/JSON bajo su monitor global ni bloquear todo el recorrido del jugador.
  No se atribuye rendimiento medido a este mecanismo.
- Persistencia: el ensamblador aplica las reglas de Model y descarta acumulados; la
  confirmación es por jugador, fuera del acceso externo. Los errores de Integration dejan
  item/incidencia de fallo sin métricas parciales; las columnas desconocidas se diagnostican
  sin crear claves. Una observación ausente no desplaza otra presente.
- Tests escritos sin ejecutarlos: recorrido con Playwright doblado, headed/contextos,
  exclusiones, filtro opcional, retry/403/404/429, workers/deadline; parser decimal exacto,
  ceros finales/opcionalidad; startup de Service con Integration doblada y DB de testing.
- Verificación de cierre: pendiente del build final y revisión de diff. Tests y medición
  real pendientes del usuario; no se aprueban CODE REVIEW ni TESTING. TD-019 sigue pendiente.

## 2026-10-16 — Verificación técnica del recorrido independiente

- Control ejecutado: `gradlew.bat build -x test`, desde `backend/`.
- Resultado observado: BUILD SUCCESSFUL en 7 s; 10 tareas, 5 ejecutadas y 5 up-to-date.
  Los intentos anteriores señalaron formato; las fuentes finales superaron Spotless dentro
  del build. No se ejecutó un formateador separado, tests, compilación de tests, navegador,
  aplicación ni consultas externas. La compilación previa notificó uso de API Jackson
  deprecada en el parser; no fue un error de compilación ni se presenta como test aprobado.
- Tests disponibles para ejecución humana: `WhoScoredScrapingIntegrationTest`,
  `WhoScoredStructuredDataParserTest`, `HeadedBrowserStartupTest` y regresión del pacer
  de identidad `WhoScoredRequestPacerTest`. El test de startup requiere Docker/Testcontainers.
- T039 precisa el origen técnico de temporada según la captura, sin modificar FR-025/026,
  las identidades, el catálogo ni la decisión humana sobre runs.
- Estado: T035, T037–T044, T079–T085 implementadas/escritas y revisadas; T036 pendiente
  de dependencia US02, sin acceso funcional de tests a Repository/JPA. US01 sigue IN PROGRESS
  y no se entrega como implementación completa ni se inicia CODE REVIEW global de la US.
- Trello permanece OUT OF SYNC — LOCAL CONTINGENCY; trasladar esta evidencia al recuperar acceso.

## 2026-10-16 — Remediación de fallos reportados en configuración y contrato HTTP

- Feature: SPEC-012; US01 IN PROGRESS LOCAL, alcance T023/T029–T030 y sus regresiones.
- Fuente humana: el usuario informó fallos únicamente en `WhoScoredConfigurationTest`
  y `PlayerStatisticsContractFoundationTest` antes de continuar con US02.
- Evidencia leída, no ejecutada por el agente: reports locales de
  `backend/build/test-results/test/TEST-footballmarket.config.WhoScoredConfigurationTest$Binding.xml`
  (15 casos, 2 fallos, timestamp 2026-10-16T18:42:39.921Z) y
  `TEST-footballmarket.controllers.PlayerStatisticsContractFoundationTest$Contract.xml`
  (20 casos, 1 fallo, timestamp 2026-10-16T18:42:45.093Z).
- Causas: los dos contextos válidos de identidad intentaban enlazar estadísticas sin sus
  propiedades por el registro añadido en `WhoScoredClientConfig`; el caso HTTP con
  `extractionDate: 123` devolvía 200 por coerción de Jackson en vez de 400.
- Corrección dentro del alcance vigente: binding de estadísticas en `PlayerStatisticsConfig`,
  separado del cliente de identidad; deserializador exclusivo del campo de fecha que exige
  cadena ISO real. Mantiene fecha opcional/null y validación UTC de fecha futura.
- Regresiones: aislamiento de ambos bindings, rechazo de propiedades estadísticas inválidas,
  fechas JSON de tipo incorrecto, fechas inexistentes y aceptación de ISO/omitida/null.
  Se conserva la expectativa de rechazo original; no se modifican las assertions para
  aceptar los comportamientos defectuosos.
- Los reports anteriores son evidencia previa a esta corrección, no resultado de los tests
  modificados. Rerun humano pendiente; sin ejecución de tests ni aprobación de TESTING.
- No hay dependencia de US02 en estos fallos. T036 conserva únicamente su dependencia
  ya documentada. No se modifica alcance funcional ni READY; no se implementa US02.
- Verificación técnica de la corrección: pendiente de build sin tests y revisión final.
- Trello OUT OF SYNC — LOCAL CONTINGENCY; trasladar diagnóstico y remediación al recuperar acceso.

## 2026-10-16 — Verificación técnica de la remediación de binding y fecha

- Control: `gradlew.bat build -x test` desde `backend/`.
- Resultado observado: BUILD SUCCESSFUL en 8 s, 10 tareas (6 ejecutadas y 4 up-to-date).
  El primer intento señaló formato; las fuentes finales superaron los controles del build.
- Revisión de fuentes: `WhoScoredClientConfig` vuelve a registrar solo sus properties de
  identidad; la aplicación registra estadísticas mediante `PlayerStatisticsConfig`. El
  deserializador de fecha actúa únicamente en el Request DTO de scraping y no cambia la
  configuración global de Jackson ni el formato de otras APIs.
- Pendiente de ejecución humana: `WhoScoredConfigurationTest`,
  `PlayerStatisticsConfigurationTest` y `PlayerStatisticsContractFoundationTest`.
  No se ejecutaron ni compilaron tests; los reports leídos conservan sus resultados previos.
- No se declara resuelto por evidencia funcional ni se aprueba TESTING. US01 permanece
  IN PROGRESS con T036 pendiente; US02 no fue implementada ni autorizada en esta operación.

## 2026-10-16 — IMPLEMENT AUTHORIZATION de US02 e inicio efectivo

- Feature: SPEC-012; User Story: US02.
- Gate: IMPLEMENT AUTHORIZATION.
- Result: AUTHORIZED LOCAL.
- Validated by: HUMAN.
- Evidencia humana: autorización explícita del usuario para implementar T045–T059 y
  T086–T092 de US02, completar T036 de US01 cuando exista la infraestructura productiva de
  runs y realizar los ajustes de integración imprescindibles sobre US01 conservando sus
  contratos y comportamiento.
- Alcance autorizado: US02 (T045–T059, T086–T092) y la integración de T036. No autoriza
  US03 (T060–T072) ni el trabajo transversal T073–T078/T093.
- Dependencia declarada por el usuario: la entrada JSON de T055/T057 y el cliente de
  campaña dependen de T072 (US03). Se implementa la estructura HTTP y el contrato de
  T055/T057 sin implementar la actualización JSON ni simular una actualización satisfactoria.
- Confirmación humana aportada: los tests escritos hasta ahora pasan, incluidos los de
  configuración y contrato HTTP corregidos. Acredita su ejecución por el usuario; no
  sustituye CODE REVIEW, TESTING ni otros gates humanos. El agente no ejecuta tests.
- Base verificada antes de modificar código: rama `feat/012-player-statistics-scraping` con
  árbol de trabajo con cambios previos preservados; `spec.md` en `Ready for implementation`;
  READY LOCAL vigente según el último restablecimiento y Analyze sin blockers;
  `tasks.md` con T001–T044/T079–T085 implementadas o escritas y T036 pendiente.
- Condiciones de ejecución: reutilizar el código de US01, conservar IDs y avance de
  `tasks.md`, escribir tests sin ejecutarlos, ejecutar únicamente
  `gradlew.bat build -x test` desde `backend/`, sin commits ni push.
- Inicio efectivo: US02 pasa a IN PROGRESS local al comenzar este trabajo; la Feature ya
  estaba IN PROGRESS local por US01.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; sin herramientas de Trello disponibles en esta
  sesión, estado remoto desconocido. Trasladar este evento al recuperar acceso.
- CODE REVIEW, TESTING y cierre de Feature: pendientes, no aprobados.

## 2026-10-16 — IMPLEMENT AUTHORIZATION de US03 y corrección de los tests de US02

- Feature: SPEC-012; historias: US03 (T060–T072) y corrección de los cinco tests de US02
  pendientes (Fase 0).
- Gate: IMPLEMENT AUTHORIZATION.
- Result: AUTHORIZED LOCAL.
- Validated by: HUMAN.
- Evidencia humana: autorización explícita del usuario para implementar T065–T072, escribir
  T060–T064 sin ejecutarlos y corregir los tests de US02 sin tocar producción.
- Alcance autorizado: US03 (T060–T072) y la corrección de los tests de US02. No autoriza el
  trabajo transversal T073–T078/T093 ni amplía US01 ni US02.
- Alcance de la corrección: los dobles de scrape devuelven una fila de contexto real, como en
  `PlayerStatisticsExtractionServiceTest`, para que el escritor confirme `COMPLETED` y los
  pendientes disminuyan; `navegadorNoDisponible` restaura el stub de `openSession()` tras el
  error esperado. Sin cambios en código de producción.
- Decisiones del usuario: `NO_DATA` se mantiene según FR-013 y queda pendiente de decisión; el
  agente no lo trata como confirmación definitiva. La fecha UTC de la campaña JSON se fija al
  inicio y se conserva aunque el recorrido cruce medianoche, con reloj UTC inyectable.
- Condiciones de ejecución: reutilizar el código de US01/US02, conservar IDs y avance de
  `tasks.md`, escribir tests sin ejecutarlos, ejecutar únicamente
  `gradlew.bat build -x test` desde `backend/`, sin commits ni push.
- Verificación técnica: `spotlessApply` por formato y `gradlew.bat build -x test` desde
  `backend/` con BUILD SUCCESSFUL; el build no acredita ejecución de tests.
- Pendiente de ejecución humana: los cinco tests de US02 corregidos y los cinco tests de US03
  escritos (T060–T064).
- Inicio efectivo: US03 pasa a IN PROGRESS local; US02 permanece IN PROGRESS local a la espera
  de CODE REVIEW y TESTING.
- Trello: OUT OF SYNC — LOCAL CONTINGENCY; sin herramientas de Trello disponibles en esta
  sesión, estado remoto desconocido. Trasladar este evento al recuperar acceso.
- CODE REVIEW, TESTING y cierre de Feature: pendientes, no aprobados.

## 2026-10-16 — Remediación de los 18 tests fallidos de la ejecución humana

- Evidencia humana: `gradlew.bat test` → 796 tests, 18 fallidos. El agente no ejecuta tests; el
  diagnóstico se hizo leyendo `build/test-results/test/*.xml` ya generados por esa ejecución.
- Causas identificadas y corregidas:
  1. `ExtractionBatchExecutor.paceMillis` dividía la suma de tiempos por el número total de muestras,
     incluidas las de duración cero, y podía devolver un ritmo de 0 ms. Con un ritmo de cero,
     `capacity` dividía por cero (`ArithmeticException`) o seleccionaba cero jugadores. Ahora solo
     entran en la media las muestras con duración conocida y, sin ninguna, se conserva el valor
     técnico conservador. Corrige `reanudaSinDuplicar`, `loteDeUnJugadorYCoverage` y el cliente de
     campaña (`exit` 1 por falta de progreso).
  2. `MetricComparisonRule.tolerance` construía la tolerancia como `BigDecimal.valueOf(5, -(p+1))`,
     que produce `5E+1` en lugar de `5E-1`. Corregido a `BigDecimal.valueOf(5, p+1)`: dieciséis
     decimales published dan `5E-17` exacto, sin reescalar a seis.
  3. `PlayerStatisticsSynchronizationServiceTest.navegadorNoDisponible` re-stubbeaba
     `openSession()` con `when(...)`, que invoca el stub que lanza la excepción. Sustituido por
     `doReturn(session).when(integration).openSession()`.
  4. `WhoScoredStatisticsIntegrationTest` encolaba menos respuestas que las veinte consultas del
     recorrido; el doble responde 500 al vaciarse la cola y se clasificaba como
     `PROVIDER_TRANSPORT_ERROR`. Ahora encola las veinte respuestas válidas donde el recorrido
     completo las necesita.
  5. `PlayerStatisticsCrossChannelJourneyE2ETest` doblaba `WhoScoredStatisticsIntegration`, por lo
     que `newWorker()` devolvía `null` y el servicio rechazaba el worker con
     `PLAYER_STATISTICS_SYNC_FAILED` (500) en todos los POST JSON. Ahora el worker lo aporta el pacer
     real del contexto. Además el caso de indisponibilidad re-stubbeaba `fetch` con `when(...)`,
     invocando la respuesta previa con argumentos nulos (`NullPointerException`); se usa
     `doThrow(...).when(...)`.
- Alcance: una corrección de producción en `ExtractionBatchExecutor` (T052, ya autorizada y dentro
  del alcance de US02/US03) y ajustes de dobles en tests. No se amplió el alcance funcional ni se
  modificaron expectativas para aceptar comportamientos defectuosos.
- Verificación técnica: `spotlessApply` y `gradlew.bat build -x test` con BUILD SUCCESSFUL. El build
  no acredita ejecución de tests.
- Pendiente: rerun humano de los tests afectados. TESTING sigue sin aprobarse.

## 2026-10-18 - Alineación documental de SPEC-012 con la implementación vigente

- Evento: corrección documental autorizada por el usuario, no implementación. No modifica código,
  migraciones, contratos HTTP ni configuración.
- Decisiones humanas: (1) la implementación vigente es la verdad y la spec se alinea a ella;
  (2) la cobertura efectiva es 45/57/42 y el canal JSON recorre quince combinaciones;
  (3) el contraste entre canales se retira porque no es un requisito según refleja la
  implementación; (4) la pérdida de disponibilidad y precisión por métrica se documenta tal cual.
- Cambios: data-model.md reescrito sobre el modelo aplanado de las dos migraciones de player_statistics
  (player_statistics_extraction_runs, player_statistics_run_items, player_statistics,
  player_statistics_incidents); contracts/http.md sin bloque comparisons;
  contracts/whoscored.md con 45 métricas y configuración vigente de cuatro workers y 1500 ms;
  spec.md con nota de alineación, grupo 2 atribuido a JSON, FR-026/FR-032/FR-033/FR-063/FR-064/
  FR-045 reescritos, FR-044/FR-047/FR-048 marcados como retirados sin renumerar y
  SC-002/SC-004/SC-006/SC-013 ajustados; plan.md, esearch.md, quickstart.md y el checklist de
  cobertura alineados; 	asks.md con T012, T013, T018, T019 y T026 declaradas superadas y T014,
  T020, T061, T062, T069 y T070 retiradas o superadas, conservando los 93 IDs.
- Deuda registrada: TD-020 (contraste entre canales retirado). No se declara resuelta. La cobertura
  efectiva de scraping de 45 métricas es la decisión vigente del usuario, no una deuda.
- Verificación técnica: revisión documental de rutas, enlaces y coherencia. No se ejecutaron
  tests ni builds por tratarse de cambios exclusivamente documentales.
- Pendientes: las tareas de contraste permanecen sin implementar y sus requisitos están
  retirados. Analyze, CODE REVIEW y TESTING no se aprueban con esta corrección; los tests
  pendientes del usuario siguen bloqueando el cierre de las historias.
- Trello: OUT OF SYNC - LOCAL CONTINGENCY; sin herramientas de Trello en esta sesión.

## 2026-10-18 - Renumeración de la SPEC a 012-player-statistics-scraping

- Evento: renumeración documental autorizada por el usuario al integrar `origin/dev`. No es
  implementación ni gate; no modifica alcance, requisitos ni estados de historias.
- Motivo: `dev` ya integró SPEC-010 (`010-buy-tokens`) y SPEC-011 (`011-sell-player-tokens`).
  El identificador 011 quedó ocupado y esta feature pasó a 012.
- Cambios: directorio de la spec renombrado de `011-player-statistics-scraping` a
  `012-player-statistics-scraping`; referencias al identificador de la feature y al nombre de
  rama actualizadas en los artefactos de la spec, `specs/trello.md`,
  `docs/TECHNICAL_DEBT.md`, `docs/backend/architecture.md`, `docs/backend/technologies.md`,
  `backend/src/main/resources/application.yml` y `MetricCatalog.java`. Los registros
  históricos conservan su contenido; solo se corrige el identificador de la feature.
- Rama: renombrada a `feat/012-player-statistics-scraping` conforme a Constitution §7.
- Sin cambios en código funcional, migraciones aplicadas, contratos ni configuración.
## 2026-10-18 - Renombrado de migraciones por convencion de timestamps

- Evento: aplicacion de docs/development/flyway-migrations.md al integrar origin/dev. No es
  implementacion funcional ni gate.
- Motivo: dev renombro V9__create_market_domain.sql a V10__create_market_domain.sql y sumo
  V20261017233439__create_token_listings.sql y V20261017233445__add_listing_to_market_transactions.sql.
  Nuestras migraciones V10__create_player_statistics.sql y V11__flatten_player_statistics.sql
  colisionaban en version con el dominio de mercado.
- Cambios: renombradas sin modificar su SQL a
  V20261018191334__create_player_statistics.sql y
  V20261018191335__flatten_player_statistics.sql, posteriores al maximo integrado. Se conserva
  el orden entre ambas: la segunda depende de la primera. Referenciasactualizadas en
  data-model.md, 	asks.md y docs/TECHNICAL_DEBT.md.
- Base de datos: no se inspecciono ninguna. Si player_statistics ya estuviera aplicada en alguna
  base, el responsable debe detener el despliegue y revisar lyway_schema_history antes de
  aplicar, conforme a la seccion de remediacion de lyway-migrations.md.
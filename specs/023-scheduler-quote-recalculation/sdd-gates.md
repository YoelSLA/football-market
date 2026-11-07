# Gates y evidencia SDD — SPEC-023

Registro persistente de las fases y gates de esta funcionalidad. Conserva la evidencia histórica y no
se reescribe para reflejar estados posteriores.

## Gate ANALYZE

**Estado**: superado sin blockers. Preparación global completa. **No autoriza IMPLEMENT.**

| Campo | Valor |
| --- | --- |
| **Fase** | ANALYZE (`/speckit.analyze`, estrictamente de solo lectura) |
| **Ejecuciones registradas** | 2 — 2026-11-03 y 2026-11-04 |
| **Rama** | `feat/023-scheduler-quote-recalculation` |
| **Base evaluada** | `HEAD` `a1cfb36` con los artefactos de la feature en el árbol de trabajo (sin commit) |
| **Artefactos contrastados** | `spec.md`, `plan.md`, `tasks.md`, `research.md`, `data-model.md`, `contracts/` (2), `quickstart.md`, `checklists/` (4) |
| **Normas aplicadas** | Constitución 9.1.0, `AGENTS.md` raíz, `backend/AGENTS.md`, `docs/development/sdd-workflow.md` |
| **Hooks** | `.specify/extensions.yml` no existe: sin hooks `before_analyze` ni `after_analyze` |
| **Resultado vigente** | 0 CRITICAL, 0 HIGH, 1 MEDIUM, 5 LOW |
| **Cobertura** | 11/11 requisitos funcionales y 8/8 criterios de éxito con verificación asociada; 33 tareas |
| **Conclusión** | Sin blockers de especificación, planificación o consistencia. `Status: Ready for implementation` en `spec.md` queda respaldado por este ANALYZE. |

### Ejecución 1 — 2026-11-03

0 CRITICAL, 0 HIGH, 7 MEDIUM, 6 LOW. Las correcciones requeridas se aplicaron en una operación
separada, autorizada por el usuario: ruta de `application-test.yml` en `plan.md` (F1), validación del
cron atribuida al disparador y T012 reformulada (C1), cierre de dependencias registrado (D2), `Status`
actualizado (D3), notas de `checklists/requirements.md` (F2), semántica de reintento en CHK006 (F3),
consolidación de FR-008 en FR-006 (A1), derivación determinista del resultado (B1) y retiro de un caso
límite no clasificado (C2).

### Ejecución 2 — 2026-11-04

0 CRITICAL, 0 HIGH. Hallazgos residuales, todos no bloqueantes:

| ID | Severidad | Resumen |
| --- | --- | --- |
| D1 | MEDIUM | SC-003 («historial incompleto») frente a TD-024: las publicaciones por jugador confirman por separado. FR-013 y SC-008 sí se satisfacen con el diseño actual. |
| F5 | LOW | `plan.md` enuncia «PostgreSQL 18» como plataforma; el documento de tecnologías solo la fija en tests. |
| F4 | LOW | Salto de numeración FR-007/FR-008 sin nota en el propio `spec.md`. |
| T1 | LOW | La tabla FR→tareas no incluye todas las tareas de tests. |
| E1 | LOW | Caso límite «sin valuaciones disponibles» sin requisito propio; cubierto por herencia de SPEC-016 según decisión del revisor. |
| D2 | LOW | El `Status` de SPEC-013 y SPEC-016 aún no refleja el cierre confirmado por el equipo el 2026-11-04; actualización ajena a esta feature. |

## Alcance de este registro

- Es evidencia de la fase ANALYZE. **No sustituye, concede ni renueva autorizaciones humanas.**
- IMPLEMENT requiere **IMPLEMENT AUTHORIZATION** humana explícita y vigente para el alcance; sin ella no
  se escribe código. Tampoco habilita CODE REVIEW, TESTING ni el cierre.
- No se modificó código, no se ejecutaron tests ni builds y no se alteró el seguimiento remoto
  (Issue #40) en ninguna de las dos ejecuciones.
- Una corrección posterior de los artefactos exige repetir ANALYZE y actualizar este registro como
  evento nuevo, sin borrar la evidencia anterior.

## IMPLEMENT AUTHORIZATION — 2026-11-04

- **Autor**: usuario de la sesión activa de OpenCode.
- **Evidencia**: «autorizo a implementar Phase 1 y 2, US1,2 y 3», seguida de «Listo, ya podes continuar» tras persistir el resultado de ANALYZE.
- **Alcance**: T001–T031 de `tasks.md`, incluidos los prerrequisitos compartidos de Phase 1 y 2; sin Phase 6, CONVERGE ni handoffs.
- **Base**: artefactos locales de SPEC-023 evaluados en la ejecución 2 de ANALYZE registrada arriba; diff local sobre `a1cfb36`.
- **Límites**: escribir tests backend, nunca ejecutarlos; revisión, testing y cierre humanos pendientes. No autoriza commits ni publicación.

## IMPLEMENT parcial — 2026-11-04

- T001–T010 implementadas; T011–T031 sin completar. Seguimiento de tareas y pendientes humanos en `tasks.md`.
- **BLOCKED — Reason**: FR-013 exige conservar las cotizaciones válidas previas ante fallo completo; `research.md` §16 conserva `REQUIRES_NEW` por jugador, que permite cotizaciones confirmadas si falla el guardado final del resumen. TD-024 documenta la evidencia. La clasificación no bloqueante D1 del ANALYZE histórico no demuestra cumplimiento de esa garantía; se conserva sin reescribirla.
- No se redujo la spec ni se alteró la transaccionalidad. La autorización original se conserva para lo no afectado, pero no habilita resolver silenciosamente la contradicción de diseño. Siguiente acción: decisión upstream y adecuación autorizada de artefactos, seguida de ANALYZE; no ejecutados en esta fase.
- Control permitido: `gradlew.bat build -x test` desde `backend/`: `BUILD SUCCESSFUL`; compilación productiva, Spotless y clasificación estática satisfactorios. No compila ni ejecuta tests, no aplica la migración ni demuestra funcionamiento.
- Migración creada con `gradlew.bat createMigration -PmigrationDescription=add_quote_execution_origin`; operación local sin conexión a base ni ejecución de tests.
- Hooks antes/después: `.specify/extensions.yml` ausente; no hay hooks que despachar. Sin CONVERGE, handoffs, commits, push ni cambios del seguimiento remoto. El último estado remoto observado fue `In Progress` en issue #40; no se afirma sincronización del bloqueo local.
- CODE REVIEW, TESTING y FEATURE COMPLETION AUTHORIZATION: pendientes. No se presenta el alcance incompleto como listo para revisión integral.

## Decisión de alcance sobre FR-013 — 2026-11-04

- **Autor**: usuario de la sesión activa de OpenCode.
- **Evidencia**: «Esta bien, el scheduler no es responsable que si hubo cotizaciones exitosas queden persistidas. Asi que aclara eso, que el mismo no puede ser responsables de aquellas porque supera su alcanze».
- **Decisión**: la ejecución automática **no** compensa ni revierte las cotizaciones ya confirmadas por jugadores individuales cuando falla el guardado del resumen. Esa responsabilidad excede su alcance y pertenece al comportamiento heredado de SPEC-016 (publicación `REQUIRES_NEW` por jugador), registrado como TD-024. FR-013 queda acotada a no persistir la ejecución de la fecha, registrar el error y dejar la fecha disponible para reintento manual.
- **Artefactos ajustados**: `spec.md` (Clarifications 2026-11-04, FR-013, SC-003, supuestos, Decisiones de CLARIFY), `plan.md` (Constraints), `research.md` §16, `data-model.md` (§3 transiciones, §7 invariantes), `contracts/scheduling-configuration.md` §4, `quickstart.md` §7, `tasks.md` (T027 y nota de ejecución parcial), `docs/TECHNICAL_DEBT.md` TD-024. No se modificó código.
- **Efecto sobre el gate**: la evidencia de ANALYZE 2 ya no está vigente; se requiere **repetir ANALYZE** sobre los artefactos ajustados. D1 deja de ser una ambigüedad no resuelta y pasa a ser un límite de alcance explícito.
- **TD-024**: permanece `Pendiente`. La decisión acota la responsabilidad de SPEC-023, no resuelve la falta de atomicidad.
- **Autorización**: la IMPLEMENT AUTHORIZATION de 2026-11-04 no se renueva ni se amplía. La implementación sigue detenida tras T001–T010; no se ejecutaron tests ni builds en esta operación y no se alteró el seguimiento remoto.

## Aclaración de responsabilidad del scheduler — 2026-11-04

- El usuario precisó que el scheduler solamente dispara el recálculo existente y registra el error si falla; no es responsable de la persistencia, integridad, compensación ni recuperación del recálculo.
- Se ajustaron la spec y documentos relacionados. Las afirmaciones históricas de gates se conservan como evidencia, no como estado vigente. TD-024 sigue pendiente.
- Esta modificación afecta el alcance evaluado: se requiere revisar la coherencia de plan y tareas y repetir ANALYZE antes de continuar. La autorización de implementación debe evaluarse por impacto; no se concede ni renueva aquí. Seguimiento remoto no verificado ni modificado.

## Unicidad por fecha — 2026-11-04

- El usuario aclara que, si el scheduler ya ejecutó el recálculo, no debe haber otra ejecución manual ni automática. Se registra por fecha UTC y ejecución persistida, conforme a la restricción existente de SPEC-016; si no hay registro, rige el comportamiento existente de reintento.
- No se añade un lock global para fechas distintas: T029 y T030 quedan retiradas sin borrar sus identificadores. No se modificó código. La coherencia de los demás artefactos y un nuevo ANALYZE siguen pendientes.

## Corrección humana: exclusión y reintento manual — 2026-11-04

- El usuario corrigió la interpretación anterior: nunca debe haber recálculos en paralelo, pero al finalizar un intento automático el administrador debe poder ejecutar manualmente, incluso el mismo día, para recuperarse de un fallo que haya consumido el intento.
- La decisión de no agregar exclusión global registrada en la sección anterior queda superada; la restricción actual de una ejecución por fecha UTC requiere revisión. La retirada de T029 y T030 no constituye un diseño válido para la nueva decisión: su estado y la solución deben revisarse sin ejecutar implementación.
- Pendientes: definir el diseño compatible con el recálculo, historial y contratos existentes; actualizar los artefactos afectados y repetir ANALYZE. No se modificó código ni se ejecutaron tests o builds.

## Confirmación de reintento y riesgo — 2026-11-04

- El usuario confirmó que se conserva SPEC-016: el administrador puede reintentar el mismo día si el intento automático no dejó ejecución registrada. Si sí quedó registrada, incluso con fallos individuales, la unicidad por fecha rechaza otro recálculo; se acepta ese riesgo. No se ha definido un límite de dos intentos sin registro.
- La afirmación anterior de permitir reintento incluso con fecha consumida queda superada. Se mantiene la decisión de no permitir recálculos en paralelo; la solución técnica y la disposición de T029/T030 deben revisarse. No se modifica código ni se autoriza implementación; actualizar derivados y repetir ANALYZE.

## Alineación de artefactos — 2026-11-04

- A petición del usuario se alinearon plan, investigación, modelo, contratos, guía, tareas y checklists con la confirmación de reintento y el alcance del scheduler. T029 y T030 vuelven a estar pendientes: se requiere exclusión compartida hasta el commit o rollback efectivo y distinguir el rechazo por concurrencia, conservando el contrato manual.
- No se modificó código ni se ejecutaron tests o builds. El resultado anterior de ANALYZE no está vigente; corresponde repetir la fase sobre la base actual. La vigencia de la autorización de implementación se evalúa por impacto; esta operación no la concede ni renueva. Seguimiento remoto no modificado.

## Ajuste de alcance tras ANALYZE — 2026-11-04

- El usuario retiró la exigencia adicional de exclusión global de FR-006: se conserva la unicidad por fecha de SPEC-016 y se retiran T029/T030 sin reutilizar sus identificadores.
- Se aclaró que FR-004 y SC-002 refieren a ejecuciones completadas; el fallo completo se registra operativamente. Se ajustaron trazabilidad y referencias de PostgreSQL e identificadores.
- Los artefactos cambiaron después de ANALYZE: repetir la fase antes de implementar. No se alteró código ni se ejecutaron tests o builds; no se modifica el seguimiento remoto ni se concede autorización.

## Precisiones posteriores a ANALYZE — 2026-11-04

- A petición del usuario se precisó T027 para verificar los dos resultados del reintento manual según exista o no una ejecución registrada; se corrigió la referencia de unicidad en `data-model.md` a FR-006.
- La actualización de `Status` de SPEC-013 y SPEC-016 queda pendiente: `specs/README.md` exige para `Approved` evidencia de trabajo completo, CONVERGE y autorización humana de cierre vigente; la confirmación de cierre del equipo no acredita por sí sola esos gates. No se inventó ni se concedió autorización.
- Los artefactos evaluados cambiaron: repetir ANALYZE. No se modificó código, no se ejecutaron tests ni builds y no se alteró el seguimiento remoto.

## ANALYZE de la base actual — 2026-11-04

- **Solicitud**: ejecución explícita del usuario tras las precisiones de T027 y `data-model.md`, con registro persistente posterior autorizado por separado de la fase de solo lectura.
- **Base evaluada**: rama `feat/023-scheduler-quote-recalculation`, HEAD `a1cfb36` y artefactos locales sin commit. Huellas SHA-256 de los archivos principales:
  - `spec.md`: `6e10f9c5a3f97873ea239d2f17dd4b92b80f456fdeb6be7261c87829190d334`
  - `plan.md`: `c19ee74e1a248f39b65118aebcfede555ea614f792840510b5183706fe6a46a5`
  - `tasks.md`: `ade6d8a974da5b936df3e4ee8d435b1e7833df459e5f753e7043fa9e2816fd55`
  - `data-model.md`: `131e8882893a9a23cbbf1406ceeb11df4234acc9197bdff2b96b3120d11e6b23`
- **Alcance contrastado**: spec y clarificaciones, plan, tareas, modelo, research, contratos, quickstart y checklists, Constitución y workflow. Prerrequisitos presentes; `.specify/extensions.yml` ausente, sin hooks de análisis.
- **Resultado**: 0 CRITICAL, 0 HIGH, 0 MEDIUM; sin blockers de preparación. Los 11 requisitos funcionales tienen tareas asociadas; los criterios SC-001–SC-008 tienen verificaciones previstas. T029 y T030 están retiradas, no son trabajo pendiente.
- **Decisiones confirmadas**: scheduler delega y registra errores sin compensar; se conserva unicidad por fecha de SPEC-016, sin exclusión global nueva. T027 distingue el reintento manual sin registro del rechazo si la fecha ya quedó consumida. TD-024 permanece pendiente y fuera de alcance.
- **Pendiente no bloqueante**: los campos `Status` de SPEC-013 y SPEC-016 aún no reflejan el cierre comunicado por el equipo; su actualización requiere evidencia de los gates correspondientes. No se modificaron esos artefactos.
- **Autorización y límites**: esta evidencia no concede ni renueva autorización humana. Existe la IMPLEMENT AUTHORIZATION histórica para Phase 1 y 2, US1–US3 registrada arriba; su vigencia debe comprobarse por impacto sobre el alcance antes de reanudar. No se ejecutaron tests ni builds, no se implementó código en esta operación ni se modificó el seguimiento remoto. CODE REVIEW, TESTING y cierre permanecen pendientes.

## IMPLEMENT de US1–US3 — 2026-11-04

- **Autorización recuperada**: sección IMPLEMENT AUTHORIZATION de este archivo y solicitud explícita del usuario: «Tenes autorizacion para continunar con la implementacion de US1-US3». Las clarificaciones redujeron el alcance (sin lock global ni compensación); no se amplió el permiso ni se requirió repetirlo.
- **Preparación**: se recuperó «ANALYZE de la base actual», posterior a T027 y modelo. Huellas de `plan.md`, `tasks.md` y `data-model.md` concordantes antes de cambiar avance; en la huella histórica de `spec.md` falta un carácter hexadecimal. La observada fue `6e10f9c5a3f97873ea239d2f17dd4b92b80f456fdeb6bef7261c87829190d334`. Se conserva el evento original y se registra esta discrepancia documental sin atribuirle un cambio funcional ni inventar una nueva ejecución de ANALYZE.
- **Alcance realizado**: T011–T028 y T031, sobre los prerrequisitos T001–T010 ya implementados. T029/T030 retiradas; no se marcan completadas. T032/T033 no se consumen; Phase 6 y fases posteriores fuera del encargo.
- **Resultado**: disparador UTC configurable; entrada automática al mismo núcleo manual; origen persistido y resultado derivado; contrato HTTP aditivo; rechazos y fallos operativos sin compensación ni cambios de unicidad. TD-024 sigue pendiente.
- **Verificación**: último `gradlew.bat build -x test` desde `backend/`: `BUILD SUCCESSFUL` (6 s), formato y clasificación estática de 141 clases satisfactorios. Los fallos intermedios fueron de formato del cambio y quedaron corregidos. Ningún test ejecutado ni compilado por el agente; migración y comprobación funcional pendientes del usuario.
- **Hooks**: `.specify/extensions.yml` ausente; no hay hooks antes/después que ejecutar. No se ejecutaron handoffs, CONVERGE, commits ni push.
- **Informe**: `implementation-report.md` contiene Code Review Report integral (incluidos los tests), decisiones, inventario de archivos, orden y checklist humano, cobertura y comandos exactos para el usuario. CODE REVIEW, TESTING y FEATURE COMPLETION AUTHORIZATION permanecen pendientes.
- **Seguimiento**: issue #40 observado OPEN y proyecto en `In Progress`. Sin escritura remota ni afirmación de sincronización con esta entrega. `spec.md` conserva `Ready for implementation`; no se declara Approved ni Done.

## IMPLEMENT AUTHORIZATION de Phase 6 — 2026-11-04

- **Autor**: usuario de la sesión activa de OpenCode.
- **Evidencia**: «Okey, tenes autorizacion para la Phase 6».
- **Alcance**: T032–T033 de `tasks.md`: documentación de arquitectura del disparador batch y verificación de la implementación local existente mediante build sin tests y revisión final del diff.
- **Base**: alcance US1–US3 entregado en el evento anterior y detallado en `implementation-report.md`; sin cambios funcionales adicionales.
- **Límites**: no ejecutar tests, fases posteriores, commits ni publicación; no implica CODE REVIEW, TESTING ni autorización de cierre.

## Resultado de Phase 6 — 2026-11-04

- T032/T033 completadas: arquitectura documentada en `docs/backend/architecture.md` §1.8 y verificación de entrega realizada; avance persistido en `tasks.md`.
- `gradlew.bat build -x test` desde `backend/`: `BUILD SUCCESSFUL` en 1 s; configuración reutilizada, 1 tarea ejecutada y 10 up-to-date. Clasificación estática válida: 141 clases. No se ejecutaron ni compilaron tests ni se aplicaron migraciones.
- Diff de `backend/src/`, tareas y documentación revisado; `git diff --check` sin errores y estado local inspeccionado. No se modificó código funcional ni se descartaron cambios previos.
- Code Review Report ampliado en `implementation-report.md` para incluir la documentación de arquitectura. El checklist humano y los tests siguen pendientes; no se concedió ninguna aprobación.
- Sin hooks de extensiones (`.specify/extensions.yml` ausente), CONVERGE, handoffs, commits, push ni modificación del seguimiento remoto. TD-024 permanece pendiente; no se declara cierre de SPEC-023.

## Corrección autorizada de T012 — 2026-11-04

- El usuario solicitó corregir únicamente `QuotePropertiesTest.rejectsInvalidCronAtStartup` y aportó la salida fallida. La causa fue una assertion sensible a mayúsculas (`cron` frente a `Cron`), no la validación productiva ni el aviso de Mockito.
- Corregida la assertion para comprobar la causa raíz y el motivo del cron inválido; no se alteran requisitos, código productivo ni el build. El informe de revisión incorpora esta corrección.
- Revisión del test y ejecución humana pendientes; no se aprueba TESTING ni cierre. Los gates ya estaban pendientes, por lo que no hay aprobaciones vigentes que invalidar.

## Corrección autorizada de T027 — 2026-11-04

- Continuando la corrección de tests solicitada por el usuario, se recibió la salida de `QuoteAutomaticRecalculationFailureTest.retriesManuallyWhenAutomaticAttemptLeavesNoExecution`: `BeanDefinitionOverrideException` para `marketClock`, antes de ejecutar el escenario.
- Campo `@TestBean` y fábrica alineados con el tipo productivo `Clock`; `enforceOverride = true` exige el reemplazo existente. La instancia sigue siendo el reloj controlado `FailureClock`. Sin cambios productivos ni overriding global.
- Informe de revisión actualizado. CODE REVIEW y TESTING siguen pendientes; deben revisarse y reejecutarse los tres escenarios que comparten el contexto. No se ejecutan tests desde el agente ni se declara PASS funcional.

## Corrección de expectativa de reintento en T027 — 2026-11-04

- El usuario confirma que otros dos tests de la clase se corrigieron y aporta la ejecución restante: el reintento manual se acepta, completa y cuenta cinco fallos por filas arrastradas confirmadas antes del rollback del resumen (`uq_player_quote`). Es evidencia parcial humana, no aprobación del gate TESTING.
- Se retira la garantía extra de `failedPlayers = 0`: la spec solo exige disponibilidad del reintento sin registro previo. Se comprueba población procesada y se mantienen fecha, origen MANUAL y persistencia del único resumen de reintento. No se compensa, limpia ni altera producción para hacer pasar la prueba.
- Evidencia añadida a TD-024 e informe de revisión actualizado. Deuda y gates humanos siguen pendientes; reejecución del escenario corregido a cargo del usuario.

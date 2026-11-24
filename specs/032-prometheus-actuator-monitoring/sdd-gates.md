# Registro SDD — SPEC-032

## ANALYZE — 2026-11-24

- **Rama**: `feat/032-prometheus-actuator-monitoring`.
- **Ejecución**: `/speckit.analyze` sobre los artefactos locales vigentes de SPEC-032, después de aclarar en `spec.md` qué información operativa se considera sensible y qué información técnica no sensible puede exponerse.
- **Prerrequisitos**: `check-prerequisites.sh --json --require-tasks --include-tasks` resolvió `specs/032-prometheus-actuator-monitoring` y encontró `research.md`, `data-model.md`, `contracts/`, `quickstart.md` y `tasks.md`.
- **Resultado**: sin hallazgos ni blockers. Se inventariaron 13 requisitos y criterios y 6 tareas; cobertura asociada 100%, sin tareas huérfanas, duplicaciones ni ambigüedades.
- **Constitución**: sin conflictos. Se mantiene la prohibición de exponer secretos, credenciales, tokens o configuración sensible en cualquier endpoint; los datos operativos estándar y la información técnica no sensible del programa o la máquina no se consideran sensibles por sí mismos.
- **Hooks y verificación**: `.specify/extensions.yml` no existe; no había hooks `before_analyze` ni `after_analyze`. No se ejecutaron tests ni builds. `git diff --check` no reportó problemas.
- **Status de la spec**: actualizado a `Ready for implementation` conforme a `specs/README.md`, por preparación validada y ANALYZE sin blockers. Este estado no autoriza ni inicia IMPLEMENT.
- **Seguimiento remoto**: PENDING / no verificado. Este registro es evidencia local y no afirma sincronización con GitHub Issues o Projects.
- **Alcance**: no se ejecutaron fases posteriores ni se concede autorización de implementación, revisión, testing o cierre.

## IMPLEMENT AUTHORIZATION — 2026-11-24

- **Autorizó**: usuario, mediante instrucción explícita en la sesión activa.
- **Alcance autorizado**: tareas T001–T006 de SPEC-032 y sus prerrequisitos declarados en `tasks.md`.
- **Evidencia comparativa**: instrucción del usuario «autorizo a implementar de T001 a T0006», recibida el 2026-11-24; tareas y criterios vigentes de esta SPEC.
- **Límites**: sin alcance funcional adicional; no ejecutar tests, fases posteriores, handoffs ni escrituras en el seguimiento operativo.
- **Vigencia**: autorización registrada para la implementación de este alcance; CODE REVIEW, TESTING y cierre no autorizados.

## IMPLEMENT — bloqueada por decisión funcional

- Se inició la implementación del alcance autorizado. Quedaron aplicados cambios locales en T001–T003 y parte de T006; no se ejecutaron tests ni builds, y las tareas permanecen sin marcar.
- **Blocker**: T005 requiere `403` para una identidad autenticada sin autorización, pero T003 solo exige autenticación JWT y la especificación no define un rol o permiso para separar identidades autenticadas autorizadas de no autorizadas. La configuración vigente anterior protegía rutas mediante autenticación y roles de negocio solo en rutas concretas; escoger un rol para operaciones añadiría una decisión funcional no especificada.
- No se completaron T004–T006 ni se marcaron tareas. Los cambios parciales se conservan para revisión y no se consideran implementación completa o verificada.
- **Decisión requerida**: definir en `spec.md` qué identidad/permiso existente autoriza `/actuator/prometheus` y, en consecuencia, el status esperado para una identidad autenticada sin ese permiso; actualizar tareas/contrato/quickstart y ejecutar ANALYZE antes de reanudar.
- **Seguimiento remoto**: PENDING / no verificado. No se modificó el sistema de seguimiento.

## IMPLEMENT — aclaración de T005 — 2026-11-24

- El usuario aclaró que la autenticación JWT es suficiente para acceder a Prometheus y que no corresponde distinguir roles/permisos de identidades autenticadas para este endpoint.
- Se actualizó el escenario independiente de US2 y T005 para eliminar la expectativa de `403` por falta de autorización.
- Esta actualización de `tasks.md` requiere ANALYZE antes de reanudar la implementación. No se reanudó IMPLEMENT ni se modificaron otros artefactos.

## ANALYZE — 2026-11-24 — revisión posterior a C1

- **Rama**: `feat/032-prometheus-actuator-monitoring`.
- **Ejecución**: `/speckit.analyze` sobre los artefactos locales vigentes de SPEC-032, incluidos los cambios de T004 y T005 para comprobar que las respuestas de monitoreo no expongan información sensible.
- **Prerrequisitos**: `check-prerequisites.sh --json --require-tasks --include-tasks` resolvió `specs/032-prometheus-actuator-monitoring` y encontró `research.md`, `data-model.md`, `contracts/`, `quickstart.md` y `tasks.md`.
- **Resultado**: sin hallazgos ni blockers. Se inventariaron 13 requisitos y criterios y 6 tareas; cobertura asociada 100%, sin tareas huérfanas, duplicaciones ni ambigüedades.
- **Constitución**: sin conflictos detectados. La cobertura de FR-005 y SC-003 queda explícita en T004 para Prometheus/health y T005 para info.
- **Hooks y verificación**: `.specify/extensions.yml` no existe; no había hooks `before_analyze` ni `after_analyze`. No se ejecutaron tests ni builds.
- **Status de la spec**: observado `Ready for implementation`; ANALYZE no concede ni inicia IMPLEMENT.
- **Seguimiento**: Issue #100 verificada como `OPEN`; no se modificó su estado. Este registro local documenta la evidencia SDD y no afirma sincronización de estados.
- **Alcance**: no se ejecutaron fases posteriores ni se concede autorización de implementación, revisión, testing o cierre.

## IMPLEMENT — resultado — 2026-11-24

- Se completaron T001–T003 y T006 conforme a la aclaración vigente: Prometheus requiere un JWT válido, sin distinción adicional por rol/permisos.
- T004 y T005 quedaron pendientes de compilación de tests y validación por el usuario.
- `./gradlew build -x test` desde `backend/` finalizó correctamente. El build no ejecutó tests ni acredita el comportamiento HTTP en ejecución.
- Verificaciones adicionales: `git diff --check` sin errores; estado de seguimiento remoto no actualizado ni afirmado.
- **CODE REVIEW** y **TESTING** quedan pendientes de autorización humana y de ejecución/revisión de los tests por el usuario. No se ejecutaron fases posteriores ni CONVERGE.

## IMPLEMENT — corrección tras compilación reportada — 2026-11-24

- El usuario aportó el resultado de dos invocaciones a `./gradlew test --tests ...`: `compileTestJava` falló porque no encontraba `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc`.
- Se corrigió el import de ambas clases a `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, conforme al paquete usado por las pruebas existentes del backend.
- No se ejecutaron tests ni se declara resuelta la compilación de tests; T004 y T005 permanecen sin marcar hasta recibir una nueva verificación.

## TESTING — fallo reportado por el usuario — 2026-11-24

- El usuario ejecutó `ActuatorMetricsHttpIntegrationTest`: 2 tests completados, 1 fallido.
- El fallo señalado en `ActuatorMetricsHttpIntegrationTest.java:69` correspondía a exigir la ausencia de `$.groups` en `/actuator/health`. El contrato requiere ocultar detalles de componentes, dependencias y configuración, pero no prohíbe metadatos de grupos de health.
- Se retiró esa assertion no requerida y se mantuvo la comprobación de ausencia de componentes y de contenido sensible.
- T004 permanece pendiente hasta que el usuario vuelva a ejecutar la prueba; no se ejecutaron tests en esta sesión.

## IMPLEMENT — verificación del usuario y colección Bruno — 2026-11-24

- El usuario reportó `BUILD SUCCESSFUL` al ejecutar por separado `ActuatorMonitoringSecurityIntegrationTest` y `ActuatorMetricsHttpIntegrationTest`. Se conserva como evidencia aportada por el usuario; no equivale a aprobación del gate TESTING.
- A solicitud del usuario, se actualizó `bruno/collection/actuator/actuator-prometheus.bru` para enviar JWT Bearer y se mantuvieron las solicitudes públicas de health e info. La solicitud de raíz Actuator ahora usa JWT para comprobar la ruta no expuesta incluso con identidad autenticada.
- T004 y T005 permanecen sin marcar mientras el usuario define qué falta para cerrarlas. No se ejecutaron nuevas pruebas ni se modificó el estado del seguimiento.

## ANALYZE — 2026-11-24 — metadata pública de info

- **Prerrequisitos**: el script resolvió esta carpeta SPEC-032 con `plan.md`, `tasks.md` y documentos auxiliares requeridos. `.specify/extensions.yml` no existe; sin hooks `before_analyze` ni `after_analyze`.
- **Alcance analizado**: clarificación de `info.app.name` y `info.app.description`, actualización de FR-009/SC-004, plan, T005 y contrato.
- **Resultado**: sin blockers. 9 requisitos funcionales y 4 criterios de éxito cubiertos por 6 tareas; T005 cubre ahora la respuesta pública acordada y su restricción de no exponer otros valores del entorno. Sin tareas huérfanas, duplicaciones ni ambigüedades.
- **Constitución**: sin conflictos; los dos valores son metadata estática no sensible y no se exponen credenciales ni configuración sensible.
- **Checklists**: ambas listas aplicables estaban completamente marcadas; no se modificaron.
- **Verificación**: no se ejecutaron tests ni builds durante ANALYZE.

## IMPLEMENT — metadata estática de `/actuator/info` — 2026-11-24

- Por solicitud explícita del usuario, se añadió el nombre público `Football Market` y la descripción `API de mercado de jugadores de fútbol` mediante el contributor de propiedades `info.*`.
- Se actualizaron la configuración, contrato, plan, spec, tecnologías, quickstart y T005 para exigir esos campos y seguir prohibiendo otros valores del entorno sensibles.
- T005 permanece sin marcar y requiere que el usuario vuelva a ejecutar su test actualizado. No se ejecutaron tests ni se modificó el seguimiento.

## TESTING — fallo reportado por el usuario — 2026-11-24

- El usuario reportó que `ActuatorMetricsHttpIntegrationTest.exponeSoloElEstadoMinimoDeSalud` falla: `GET /actuator/health` devolvió `components` con detalles de PostgreSQL y la ruta local del backend; la assertion `$.components.doesNotExist()` falló.
- La configuración efectiva local mostraba `management.endpoint.health.show-details: always`, incompatible con FR-007 y con el contrato. `show-details: never` evita detalles; se configuró también `show-components: never` para no publicar los componentes.
- Se actualizó el plan y la documentación tecnológica para reflejar ambas propiedades. No se cambiaron la expectativa del test ni el contrato.
- No se ejecutaron tests. T004 sigue pendiente de que el usuario vuelva a ejecutar `ActuatorMetricsHttpIntegrationTest`; el resultado reportado invalida la evidencia anterior para este comportamiento.

## TESTING — fallo reportado por el usuario — 2026-11-24 — métrica de pausas GC

- El usuario reportó que el test de métricas falla porque no aparece `jvm_gc_pause_seconds`. El endpoint respondió `200` con métricas JVM y `jvm_gc_overhead`; las advertencias de auto-attach de Mockito no son la causa del fallo.
- Micrometer registra las series `jvm.gc.pause` al recibir notificaciones de pausas GC; por tanto, no necesariamente aparecen en un scrape previo al primer evento desde que se vinculó el binder.
- Se ajustó el test para solicitar una recolección GC antes del scrape y se aclaró en FR-002, el contrato y el quickstart que las series de pausas aparecen cuando se producen eventos. No se relajó el requisito de métricas de pausa.
- No se ejecutaron tests. T004 permanece pendiente de una nueva ejecución del usuario.

## TESTING — confirmación del usuario para T004 — 2026-11-24

- El usuario confirmó que los tests actualizados pasaron y autorizó dar por finalizada T004.
- Se marca T004 como completada. Esta confirmación cubre `ActuatorMetricsHttpIntegrationTest`; no cierra T005 ni aprueba por sí sola el gate general de TESTING o la Feature.
- No se ejecutaron tests ni se modificó el seguimiento remoto.

## TESTING — confirmación del usuario para T005 — 2026-11-24

- El usuario confirmó que las pruebas de T005 también pasaron.
- Se marca T005 como completada. No se ejecutaron tests por el agente ni se modificó el seguimiento remoto; la confirmación de tareas no constituye por sí sola aprobación del gate general de TESTING ni cierre de la Feature.

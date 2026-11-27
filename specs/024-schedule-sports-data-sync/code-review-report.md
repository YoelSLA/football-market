# Code Review Report — SPEC-024

## Resultado y alcance

IMPLEMENT del alcance autorizado: fundamentos T001–T004, US1 (T005–T008), US2 (T009–T011) y coherencia arquitectónica T012. T001–T002 y parte de los fundamentos venían de la ejecución anterior; los cambios previos se conservaron y se completaron según el plan corregido. Spec: **Draft**. Revisión humana: **PENDING**. Tests: **no ejecutados ni compilados por el agente**. No se aprueba ningún gate.

Se reutiliza Spring Scheduling; se dispara domingo 12:00 en Buenos Aires y se espera JSON → scraping → evaluación. Un parcial individual no corta la secuencia. Un fallo global o interrupción omite etapas posteriores. Las operaciones conservan sus fechas y transacciones; no se accede a cotizaciones ni operaciones financieras.

## Archivos de producción

Rutas relativas a `backend/src/main/java/footballmarket/`:

| Archivo | Cambio, propósito y puntos de revisión |
| --- | --- |
| `features/statistics/application/PlayerStatisticsSynchronizationService.java` | Dos entradas automáticas aditivas con retorno booleano de parcialidad. Preserva firmas manuales y no expone contadores al scheduler. Javadoc corregido respecto del recorrido manual sin máximo. |
| `features/statistics/application/exceptions/ScheduledStatisticsSyncException.java` (nuevo) | `ApplicationException` con enum cerrado `Reason`: proveedor, navegador, interrupción, fallo inesperado, fallo de cierre y estados terminales fallidos. Mensaje propio seguro; permite conservar causas internas sin registrarlas. |
| `features/statistics/application/shared/impl/PlayerStatisticsSynchronizationServiceImpl.java` | Comparte recorridos privados entre entradas manuales y automáticas. `synchronizeJson(boolean)` protege selección y workers después de abrir el run. `abortJsonRun` limpia el indicador solo durante el cierre, restaura la interrupción y conserva causa principal y cierre suprimido. `closeRun` distingue fallo de persistencia de señal terminal. El camino manual conserva la política anterior; no se agregan reintentos. |
| `features/statistics/application/shared/internal/ExtractionRunManager.java` | `closeScheduled` confirma la transacción antes de señalar el fallo global, priorizando código sobre estado. `abortScheduled` fuerza estado interrumpido únicamente para el aborto automático tras error inesperado; las confirmaciones previas no se revierten. `finish` conserva su comportamiento anterior para llamadas manuales. |
| `features/statistics/infraestructure/scheduling/AutomaticSportsDataScheduler.java` (nuevo) | Constructor con solo dos fachadas y flag de habilitación. `synchronize`, líneas 31–70: cron/zona, espera síncrona, chequeo de interrupción entre operaciones, resultado completo/parcial y captura terminal. `isInterruption`, líneas 79–95: examina tipos/códigos, sin mensajes. No contiene lógica de dominio, persistencia ni transacciones. |

`backend/src/test/resources/application-test.yml` añade únicamente `whoscored.statistics.schedule.enabled=false`. Producción utiliza el valor por defecto habilitado; no hay configuración nueva del horario, nuevo `@EnableScheduling` ni scheduler adicional. No cambian HTTP, DTO, seguridad, dependencias, tecnología ni esquema de datos.

## Archivos de tests e infraestructura de testing

Rutas relativas a `backend/src/test/java/footballmarket/`:

| Archivo | Responsabilidad y revisión |
| --- | --- |
| `features/statistics/application/shared/PlayerStatisticsSynchronizationServiceTest.java` | Grupo `ScheduledSynchronization`: dieciséis métodos de regresión desde la interfaz real, once previos y cinco añadidos en esta reanudación. Proveedores externos mock, un worker JSON para orden determinista, PostgreSQL y JPA reales. Comprueba resultados, motivos, causa/suprimidos y relanzamiento manual sin leer repositorios. Se conserva el grupo manual existente. |
| `features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerTest.java` (nuevo) | Doce métodos (uno parametrizado con tres combinaciones): orden, entrada de scraping con fecha nula, evaluación sin argumentos, parcialidad, deshabilitación, fallos por etapa, log seguro, cierres fallidos, interrupciones y disparo posterior. Sin Spring, DB ni proveedores. |
| `features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerConfigurationTest.java` (nuevo) | Tres pruebas con contexto reducido: anotación cron/zona y siguiente domingo calculado, habilitación por defecto sin interacción con cotizaciones y YAML real de test que deja inerte el disparador. No inicia scheduling real ni espera reloj de pared. |
| `support/StatisticsCloseFailureInjector.java` (nuevo) | Instrumentación técnica de testing: decora DataSource/conexiones reales, sin datos funcionales ni assertions. Armado explícito para un solo update del run en el hilo coordinador; lanza una SQLException controlada para ejercitar rollback del cierre. Desarmado en `@AfterEach`. No mockea Services/Repository/JPA ni cambia configuración productiva. Revisar el alcance del matcher SQL y que solo opere en PostgreSQL aislado del test. |

Detalles de casos, mocks y comandos: [testing-report.md](testing-report.md).

## Documentación y trazabilidad

- `docs/backend/architecture.md`, §1.9: entrada batch, dependencias permitidas, señal interna, cierre, limitaciones y perfil test.
- `docs/TECHNICAL_DEBT.md`, TD-037: avanza a **En curso**, no resuelta; conserva evidencia original y registra la mitigación automática con validación humana pendiente. El problema manual preexistente no se corrige fuera del plan.
- `implement-preparation.md`: autorización original, ampliación a US1/US2 y reanudación tras corrección upstream, sin fabricar aprobaciones.
- `tasks.md`: avance implementado y evidencia de verificación separada de los tests pendientes.
- `implementation-report.md`: conserva el informe parcial y añade la entrega posterior con referencias al informe vigente.
- Este informe y `testing-report.md` documentan la entrega. `spec.md`, plan, tareas corregidas upstream y otros artefactos previos no fueron sobrescritos para justificar código.

## Decisiones y limitaciones que inspeccionar

1. El cierre transaccional precede a la señal automática. `PARTIAL` no se convierte en fallo por sí solo y ningún contador decide continuidad del scheduler.
2. El aborto por error inesperado se limita a JSON automático. Se espera salida de workers/limpieza antes de intentar cerrar; se conserva la causa original y cualquier cierre fallido como excepción suprimida. Si la DB impide cerrar, el log indica `closeFailed` y no se promete disponibilidad inmediata.
3. Los logs del disparador no reciben Throwable ni mensajes arbitrarios. Solo etapa fija, `Reason` o nombre de clase y fallo de cierre conocido. Revisar esto también en errores con causas/suprimidos sensibles.
4. El disparador no inspecciona el resultado de evaluación ni lo reclasifica por fallos individuales; no toca selección de campaña, valoración o cotización.
5. Se conserva la decisión upstream de no fabricar `PROVIDER_UNAVAILABLE` + `COMPLETED` cuando el fallo se registra como item. No se acredita haber probado esa combinación; la prioridad del código global se mantiene.
6. La inyección de fallo de cierre es exclusiva de los tests, de una sentencia y un hilo. No es un sustituto de DB ni un acceso de las assertions a persistencia.

## Verificación observada y seguimiento

- Prerrequisito real de IMPLEMENT correcto; checklists completas y sin modificación de marcadores; sin extension hooks registrados.
- `gradlew.bat build -x test`: **BUILD SUCCESSFUL**, formato correcto y clasificación estática de **143 clases**. Tres intentos previos de esta reanudación fallaron por formato del código nuevo; se corrigió manualmente y el último build pasó. La advertencia de API obsoleta corresponde al `Thread.getId()` ya usado por el recorrido previo; no se amplió ese cambio.
- Este build compila producción, no tests. Compilación de tests, resultados funcionales y revisión humana permanecen pendientes. No hay resultados de tests aportados para este cambio.
- GitHub Projects observado **In Progress** para issue #41; no se escribe en remoto. La sincronización a revisión queda **PENDING**, no se afirma In Review ni Done. PR de referencia #80; no se realizan commits ni push.
- IMPLEMENT termina aquí: no se ejecutan CONVERGE, handoffs ni cierre.

## Orden recomendado y checklist humano

### Contraste adicional del diff local — 2026-11-06

- **Hallazgo P2 — fallo de cierre scraping oculto en el registro automático.** En `PlayerStatisticsSynchronizationServiceImpl.java:149–160`, cuando scraping ya tiene un error, `scheduled && failure == null` selecciona el cierre manual y adjunta un fallo de cierre como excepción suprimida al error original. En `AutomaticSportsDataScheduler.java:61–64`, `closeFailed` solo se reconoce para `ScheduledStatisticsSyncException`. Por tanto, si falla la apertura del navegador y también la persistencia del cierre, el evento informa `stage=SCRAPING cause=HeadedBrowserUnavailableException closeFailed=false`, aunque el run puede quedar activo. Esto contradice la comunicación del fallo de cierre prevista en el diseño y puede ocultar la causa de indisponibilidad manual (FR-004/FR-006). Revisar una traducción segura exclusiva de la entrada automática que conserve ambos errores y añadir regresión de fachada y scheduler para este caso; no cambiar el contrato manual. **No corregido en esta revisión; requiere resolución antes de solicitar aprobación del alcance afectado.**
- Los tests actuales ejercitan el fallo de cierre JSON y la recuperación scraping tras navegador no disponible, pero no combinan fallo scraping y fallo de cierre. Los casos del scheduler usan señales construidas y no detectan esa diferencia de tipos real.
- `git diff --check`: sin incidencias. No se modificó código productivo ni tests; no se ejecutaron builds ni tests en esta revisión. El build y el estado remoto descritos arriba son evidencia histórica del 2026-11-05, no controles repetidos ni estado remoto verificado hoy.

Revisar fachada/Reason → cierre y aborto JSON → scheduler y perfil → tests de Service y fallo de cierre → tests unitarios/configuración → arquitectura y deuda.

- [ ] Revisé los archivos creados/modificados, incluidos los tests y su infraestructura.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes y las limitaciones.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar a TESTING.

**Estado: pendiente de revisión y aprobación humana explícita.** La lista no está aprobada por el agente y no se ejecuta la fase siguiente.

## Corrección P2 posterior — 2026-11-06 (parte del diff vigente)

- Hallazgo confirmado: scraping devolvía el error original con cierre suprimido, que el scheduler no reconocía como `closeFailed`. La autorización específica consta en `implement-preparation.md`.
- `PlayerStatisticsSynchronizationServiceImpl.synchronizeScraping`, catch de cierre: si hay ambos fallos y la entrada es automática, crea una señal controlada con motivo seguro, error original como causa y cierre como suprimido. El scheduler existente reconoce esa señal y registra `closeFailed=true`; no se modificó su lógica ni se interpretan suprimidos arbitrarios. Si la entrada es manual, conserva exactamente la excepción original y la política previa de suprimidos.
- `PlayerStatisticsSynchronizationServiceTest.ScheduledSynchronization`: dos tests nuevos comprueban motivo seguro/causa/suprimido en automático y la identidad de la excepción original en manual. El grupo pasa de dieciséis a dieciocho métodos nuevos respecto del inicio de IMPLEMENT.
- `AutomaticSportsDataSchedulerTest.Failures`: test nuevo para doble fallo scraping; afirma log exacto con etapa/motivo/`closeFailed=true`, sin datos privados ni Throwable y sin evaluar después. La clase pasa de doce a trece métodos.
- `tasks.md` e informes actualizados. TD-038 registrada En curso, no resuelta, por falta de resultados humanos de las regresiones. No se modifica la spec, los contratos manuales ni la arquitectura. Revisar especialmente la rama `scheduled` del catch y los tres tests nuevos.
- CODE REVIEW y TESTING continúan pendientes sobre este diff actualizado; la implementación previa no sustituye su revisión. Tests no ejecutados ni compilados por el agente. No se ejecutan fases posteriores ni operaciones Git de escritura.
- Verificación P2: build permitido `gradlew.bat build -x test`, `BUILD SUCCESSFUL`; formato y clasificación de 143 clases correctos. Se corrigió manualmente un fallo inicial de formato en un `@DisplayName`. Esta salida no compila ni ejecuta tests.

## Revisión de la corrección P2 — 2026-11-06

- Contraste estático del diff vigente: **el defecto P2 está corregido**. La rama automática del catch de cierre scraping envuelve el error original como causa y el cierre fallido como suprimido en `ScheduledStatisticsSyncException`; el scheduler reconoce esa señal y registra `closeFailed=true`. La rama manual conserva el error original y su cierre suprimido.
- Regresiones inspeccionadas: `scrapingAutomaticoComunicaDobleFallo` y `scrapingManualConservaContratoAnteDobleFallo` en `PlayerStatisticsSynchronizationServiceTest.java:179–215`, y `scrapingRegistraDobleFalloSeguro` en `AutomaticSportsDataSchedulerTest.java:138–160`. Cubren traducción real de fachada, compatibilidad manual, log seguro y ausencia de evaluación posterior. No se identifican nuevos hallazgos en esta corrección acotada.
- El usuario comunica en esta revisión: «Los tests pasaron todos». Se registra como resultado satisfactorio aportado por el usuario, no como ejecución del agente; no se aportan comandos ni selección concreta de suites. No se requiere salida completa satisfactoria.
- `git diff --check`: sin incidencias. No se ejecutan builds ni tests, ni se modifica código. Este registro complementa el informe completo anterior y no borra el hallazgo histórico. La comprobación técnica y el resultado comunicado no sustituyen la aprobación humana explícita de CODE REVIEW ni TESTING; no se autoriza cierre ni se modifica el seguimiento remoto.

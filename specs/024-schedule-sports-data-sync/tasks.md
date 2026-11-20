# Tareas: programación semanal de datos deportivos y evaluación

**Fuentes**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md) y [contrato interno](contracts/scheduled-execution.md).

Las tareas son exclusivamente técnicas. Su generación no autoriza implementación; requiere ANALYZE y autorización humana vigente. Los tests backend se escriben según `docs/backend/testing.md` y los ejecuta el usuario. No se crean tests frontend.

## Fase 1: Preparación

Se reutilizan proyecto, dependencias y scheduler existentes; no se requiere inicialización ni infraestructura nueva.

## Fase 2: Fundamentos compartidos

La señal interna es necesaria para distinguir fallos globales de resultados parciales sin alterar la entrada manual.

- [X] T001 Definir en `backend/src/main/java/footballmarket/features/statistics/application/PlayerStatisticsSynchronizationService.java` las entradas automáticas JSON y scraping y su resultado operativo mínimo de parcialidad, preservando las firmas manuales y sin exponer contadores.
- [X] T002 Definir en `backend/src/main/java/footballmarket/features/statistics/application/exceptions/ScheduledStatisticsSyncException.java` la señal controlada de fallo global con identidad y causa segura disponible, conforme a la jerarquía `FootballMarketException` y sin alterar excepciones manuales.
- [X] T003 Adaptar `backend/src/main/java/footballmarket/features/statistics/application/shared/impl/PlayerStatisticsSynchronizationServiceImpl.java` y `backend/src/main/java/footballmarket/features/statistics/application/shared/internal/ExtractionRunManager.java` para propagar a las entradas automáticas el fallo global donde se conoce, incluido `PROVIDER_UNAVAILABLE` con resumen `PARTIAL` o `COMPLETED`, y traducir `FAILED` e `INTERRUPTED`; reutilizar los recorridos y conservar resultados y errores manuales, transacciones y semántica de `PARTIAL` sin fallo global. Tras un fallo inesperado o interrupción posterior a la apertura JSON automática, completar la limpieza de workers y cerrar como interrumpida antes de propagar, preservando confirmaciones, interrupción y error principal; conservar también cualquier fallo de cierre sin afirmar que este tuvo éxito. Si la persistencia impide cerrar, comunicarlo sin prometer disponibilidad inmediata. Mantener el comportamiento manual existente.
- [X] T004 Ampliar `backend/src/test/java/footballmarket/features/statistics/application/shared/PlayerStatisticsSynchronizationServiceTest.java` con regresiones de la fachada automática y manual: indisponibilidad global JSON, incluido `PARTIAL`, estados `FAILED` e `INTERRUPTED`, parcial individual sin fallo global y conservación del comportamiento manual, respetando la frontera Service y el aislamiento de persistencia. Provocar un fallo inesperado posterior a la apertura automática, comprobar su señal y verificar relanzamiento manual y conservación observable de datos; cubrir también interrupción y fallo de cierre conforme al plan. Por decisión del usuario no se exige fabricar una prueba `PROVIDER_UNAVAILABLE` con `COMPLETED` cuando el recorrido registra un item fallido; se conserva la prioridad del fallo global sobre el estado.

### Evidencia de IMPLEMENT parcial — 2026-11-05

- Autorización persistida y releída en `implement-preparation.md`: únicamente fases 1 y 2. Fase 1 verificada sin inicialización ni dependencias nuevas.
- T001–T002 implementadas y compiladas. T003 adaptada para cerrar antes de señalar los fallos conocidos, sin alterar respuestas manuales; permanece abierta por el hallazgo de disponibilidad preexistente TD-037, que exige acordar cobertura y tratamiento de excepciones inesperadas antes de declarar los fundamentos completos.
- T004 tiene once regresiones nuevas desde la fachada real, con Integrations sustituidas, sin mocks de componentes internos ni acceso directo a persistencia. Pendiente el caso combinado `PROVIDER_UNAVAILABLE` + `COMPLETED`: no se ha identificado un Arrange reproducible desde esa frontera. No se sustituyen componentes internos para fabricar ese resultado ni se cambia el requisito.
- Build permitido `gradlew.bat build -x test`: satisfactorio; no ejecuta ni compila los tests. Primer intento falló por orden de imports del cambio, corregido antes del segundo intento satisfactorio. Tests y revisión humana pendientes; no se acredita PASS funcional ni se aprueban gates.
- Detalle y pendientes: `implementation-report.md`. No se inicia fase 3, CONVERGE ni handoff. El cambio de marcadores no altera el desglose ni invalida por sí solo el análisis; TD-037 sí exige reevaluación de impacto antes de reanudar lo afectado.

## Fase 3: US1 — Actualización y evaluación semanales (P1, MVP)

**Objetivo**: disparar semanalmente las tres operaciones existentes en orden, comunicando el resultado sin invocar cotizaciones ni operaciones financieras.

**Verificación independiente**: comprobar cron y zona y, con colaboradores controlados, observar exactamente JSON → scraping → evaluación, sin solapamiento ni llamadas financieras; comprobar resultado completo y parcial sin fallo global.

- [X] T005 [P] [US1] Crear `backend/src/test/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerTest.java` con pruebas deterministas de orden síncrono, entrada `PlayerStatisticsSyncRequest(null)`, evaluación sin argumentos, parcial sin fallo global, logs de inicio y resultado y ausencia de interacción con recálculo financiero.
- [X] T006 [P] [US1] Crear `backend/src/test/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerConfigurationTest.java` para verificar cron `0 0 12 * * SUN`, zona `America/Argentina/Buenos_Aires` y aislamiento del disparador en perfil `test`, usando el mínimo contexto necesario.
- [X] T007 [US1] Crear `backend/src/main/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataScheduler.java` reutilizando el scheduling existente y las fachadas de estadísticas y evaluación: ejecutar JSON → scraping → evaluación sincrónicamente, mantener fechas propias, registrar inicio y resultado completo o parcial y no invocar cotizaciones ni operaciones financieras.
- [X] T008 [US1] Ajustar `backend/src/test/resources/application-test.yml` y `backend/src/main/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataScheduler.java` según el mecanismo existente para mantener inerte el disparador en perfil `test`, sin cambiar el cron funcional ni agregar configuración de horario.

## Fase 4: US2 — Fallos y disponibilidad (P1)

**Objetivo**: cortar las etapas dependientes ante fallos globales o interrupción, comunicar la etapa y causa segura y conservar la disponibilidad posterior.

**Verificación independiente**: provocar fallos de JSON, scraping y evaluación; comprobar ausencia de etapas posteriores, logs seguros, preservación de interrupción y disponibilidad de los procesos manuales y de futuros disparos.

- [X] T009 [US2] Ampliar `backend/src/test/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerTest.java` para cubrir fallos globales por etapa, interrupción, supresión de etapas posteriores, código seguro o identidad sin detalle, ausencia de secretos y texto arbitrario, y ejecución posterior tras fallo.
- [X] T010 [US2] Completar `backend/src/main/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataScheduler.java` para capturar la señal o excepción de cada etapa, detener la secuencia, registrar etapa y causa segura disponible sin traza ni mensaje arbitrario, restaurar la interrupción cuando corresponda y retornar sin deshabilitar ejecuciones futuras.
- [X] T011 [US2] Ampliar `backend/src/test/java/footballmarket/features/statistics/application/shared/PlayerStatisticsSynchronizationServiceTest.java` para comprobar que una falla automática no altera la disponibilidad ni los resultados y errores de las entradas manuales existentes, respetando sus restricciones y las garantías de datos válidos observables desde el Service.

## Fase 5: Coherencia transversal

- [X] T012 Actualizar `docs/backend/architecture.md` con el punto de entrada batch deportivo, su dependencia de las fachadas y la señal interna de SPEC-012, preservando la descripción de los contratos manuales y del scheduler compartido.

## Dependencias y orden

- Preparación: sin tareas; T001 y T002 preceden T003, y T003 precede la verificación de fachada T004.
- US1 depende de los fundamentos; T005 y T006 pueden escribirse en paralelo porque afectan archivos distintos; T007 precede T008.
- US2 depende del disparador de US1 y de la señal de fundamentos; T009 precede T010. T011 amplía el mismo archivo que T004 y se realiza después de este, sin edición simultánea.
- T012 documenta la solución implementada y sigue a T003, T008 y T010. La independencia de las verificaciones por historia no elimina estas dependencias técnicas.

## Oportunidades de paralelismo

- US1: T005 y T006 son independientes entre sí y modifican archivos distintos; T007 y T008 comparten archivo y son secuenciales.
- US2: una vez terminadas T004 y T005, T009 y T011 pueden abordarse en paralelo: prueban archivos distintos. T010 depende de la estructura del disparador y no se solapa con T007/T008.
- `[P]` señala únicamente tareas sin dependencia de tareas incompletas dentro de su fase y no autoriza delegación ni implementación.

## Estrategia de implementación

### Corrección autorizada de hallazgo P2 — 2026-11-06

- T003/T004/T009/T011: corregida la combinación de fallo scraping y fallo de cierre. Solo la entrada automática envuelve ambos en `ScheduledStatisticsSyncException`, conserva causa y cierre suprimido y permite al scheduler registrar `closeFailed=true`. La entrada manual conserva la excepción original y suprimidos.
- Añadidas dos regresiones Service (doble fallo automático y contrato manual) y una del scheduler (etapa SCRAPING, cierre fallido, sin evaluación ni filtración de causas/trazas). Reutilizan la infraestructura de fallo de cierre de testing; no se ejecutan ni compilan tests.
- Autorización persistida en `implement-preparation.md`; TD-038 registrada En curso, pendiente de validación humana. No se agregan requisitos ni se cambia el plan. Los informes de revisión y testing incluyen el nuevo diff; gates humanos permanecen pendientes.
- Verificación de esta corrección: `gradlew.bat build -x test` satisfactorio, con formato y clasificación de 143 clases correctos. Primer intento falló únicamente por formato de una anotación nueva; corregido manualmente. Tests no compilados ni ejecutados; sin fases posteriores.

### Reanudación y entrega de IMPLEMENT — 2026-11-05

- Se implementan T003–T011 conforme al plan corregido y a la autorización persistida; se completa T012 como coherencia documental del mismo cambio, sin ejecutar otra fase SDD. Los marcadores significan trabajo escrito y revisado estáticamente, no ejecución de tests ni aprobación humana.
- T003: cierre automático JSON tras errores inesperados/interrupción, preservando confirmaciones, causa principal, fallo de cierre e indicador del hilo; entrada manual sin cambio funcional. T004/T011: cinco regresiones adicionales de cierre, interrupción y disponibilidad, con fachada/persistencia reales. El fallo de cierre se inyecta técnicamente sobre una única sentencia de la DB de testing; no se mockean repositorios ni componentes internos.
- US1: cron y zona fijos, secuencia síncrona y log terminal completo/parcial, sin cotizaciones. US2: corte por etapa, errores seguros sin mensajes/trazas, preservación de interrupción, disparos posteriores y regresión manual. Perfil test inerte mediante `whoscored.statistics.schedule.enabled=false`.
- Build final `gradlew.bat build -x test`: `BUILD SUCCESSFUL`; formato y clasificación de 143 clases correctos. Intentos anteriores de esta reanudación detectaron únicamente formato del código nuevo, corregido manualmente. No se ejecutaron ni compilaron tests. Compilación/ejecución de tests y aceptación funcional pendientes del usuario.
- TD-037 queda `En curso`, no resuelta: mitigación automática implementada, verificación humana pendiente y comportamiento manual preexistente conservado. Si falla la DB al cerrar, se informa sin prometer disponibilidad inmediata. No se exige fabricar la combinación no reproducible de T004, según la decisión upstream registrada.
- Entrega: `code-review-report.md` y `testing-report.md`. CODE REVIEW, TESTING y cierre pendientes. GitHub Projects observado `In Progress`; sincronización a revisión pendiente, sin escritura remota autorizada. No se ejecutan CONVERGE, handoffs, commits, push ni fases posteriores.

El MVP sugerido comprende fundamentos y US1. Añadir US2 para cubrir íntegramente la feature; ambas historias son obligatorias y se evalúan conjuntamente. Tras esta fase corresponde ANALYZE, sin ejecutarlo automáticamente. La implementación requiere autorización humana explícita; los tests los ejecuta el usuario y ninguna tarea o build acredita revisión, testing ni cierre.

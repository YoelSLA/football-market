# Investigación: programación semanal deportiva

## Programación y zona

**Decisión**: cron Spring `0 0 12 * * SUN` con zona `America/Argentina/Buenos_Aires`, reutilizando el `TaskScheduler` existente y desactivando el disparo en tests.

**Justificación**: `FootballMarketApplication` ya habilita scheduling; `application.yml` configura un hilo. La zona explícita no depende de la zona del servidor. La concurrencia está fuera de alcance; una tarea larga previa podría retrasar el disparo.

**Alternativas**: Quartz, otra infraestructura o cron configurable agregan alcance no requerido; cambiar la zona global afectaría consumidores ajenos.

## Coordinación y fronteras

**Decisión**: disparador de infraestructura delgado, sin Controller ni lógica de dominio, que secuencia las fachadas de aplicación y detiene por señal de fallo global.

**Justificación**: la arquitectura permite un disparador adicional y prohíbe dependencias generales Service → Service; Orchestrator no puede inspeccionar Model. El disparador no necesita inspeccionar los resúmenes si la fachada traduce el fallo a señal controlada.

**Alternativas**: llamadas HTTP internas duplican autenticación y transporte; Service → Service infringe la matriz; un Orchestrator que inspeccione `status()` infringe sus límites.

## Resultado parcial y fallo global JSON

**Decisión**: propagar una señal interna del fallo global desde la operación real, manteniendo el contrato manual y su resumen inalterados. El flujo automático continúa con `PARTIAL` solo si no se informó fallo global.

**Justificación**: `PlayerStatisticsSynchronizationServiceImpl.synchronizeJson()` marca `unavailable` y cierra con `PROVIDER_UNAVAILABLE`, pero `ExtractionRunManager.finish/resolve` puede devolver `PARTIAL` o `COMPLETED`; `PlayerStatisticsSyncSummary` no incluye el código. Inspeccionar únicamente el estado incumpliría FR-003 y FR-004. `FAILED` e `INTERRUPTED` también deben abortar. La señal interna no añade porcentajes ni altera resultados manuales.

**Alternativas**: tratar todo `PARTIAL` como fallo contradice la spec; usar `coverageComplete` añade un umbral prohibido; leer el repositorio desde el scheduler viola sus límites y acopla la coordinación a persistencia; cambiar el contrato HTTP manual no es necesario.

## Evaluación, errores y observabilidad

**Decisión**: llamar `evaluate()` únicamente tras ambas sincronizaciones sin fallo global; conservar su selección de campaña y fechas. Registrar resultado terminal y, ante fallo, etapa y código seguro o clase de excepción si no hay código seguro.

**Justificación**: la evaluación selecciona la última campaña JSON confirmada y puede informar fallos individuales sin fallo global. Los mensajes arbitrarios de excepciones pueden contener información sensible; un código seguro conserva el error comunicado sin registrar secretos. Una excepción que no expone causa solo permite registrar su identidad, sin inventar detalles.

**Alternativas**: forzar la fecha de la primera etapa, añadir cobertura mínima, loguear stack traces o mensajes sin sanitizar, o crear historial/consulta nuevos exceden el alcance o arriesgan seguridad.

## Verificación

**Decisión**: pruebas automatizadas backend de secuencia y ausencia de llamadas posteriores, interrupción, fallos por etapa, parciales, código global oculto, registro seguro, cron/zona y preservación de entradas manuales. El usuario las ejecuta; PLAN no ejecuta tests.

**Alternativas**: esperar un domingo real o usar proveedor real vuelve la prueba indeterminista; añadir E2E o infraestructura de frontend sin necesidad no aporta cobertura proporcional.

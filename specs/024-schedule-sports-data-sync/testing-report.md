# Testing Report — SPEC-024

**Estado: pendiente de validación humana.** El agente no ejecutó ni compiló tests. El build permitido sin tests pasó; no acredita ninguno de los resultados funcionales siguientes ni sustituye CODE REVIEW o TESTING.

## Tests creados y modificados

- Nuevo `backend/src/test/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerTest.java`: doce métodos, uno parametrizado con tres casos; categoría `unit`, con `@Nested`.
- Nuevo `backend/src/test/java/footballmarket/features/statistics/infraestructure/scheduling/AutomaticSportsDataSchedulerConfigurationTest.java`: tres métodos; categoría `lightweight`, con `@Nested` y contexto mínimo.
- Modificado `backend/src/test/java/footballmarket/features/statistics/application/shared/PlayerStatisticsSynchronizationServiceTest.java`: dieciséis métodos nuevos respecto del código anterior a IMPLEMENT, en `ScheduledSynchronization` (once de la entrega parcial y cinco de la reanudación). Se conservan nueve tests manuales del grupo `Batch`; categoría `integration`, PostgreSQL real de Testcontainers y perfil `test`.
- Nuevo helper técnico `backend/src/test/java/footballmarket/support/StatisticsCloseFailureInjector.java`; no es una suite ni contiene assertions funcionales. Se arma para una sentencia de cierre y se desarma después de cada escenario.
- `backend/src/test/resources/application-test.yml` deshabilita únicamente el disparador deportivo. No hay tests ni infraestructura frontend nuevos.

## Cobertura trazada y assertions principales

| Alcance / criterios | Casos escritos | Qué revisar |
| --- | --- | --- |
| US1, FR-001, SC-001 | Cron exacto `0 0 12 * * SUN`, zona Buenos Aires y siguiente domingo a mediodía; YAML real de test inerte. | Anotación productiva y `CronExpression` se contrastan sin esperar al domingo real; no es una ejecución del timer Spring real. |
| US1, FR-002, SC-002 | JSON → scraping con `PlayerStatisticsSyncRequest(null)` → `evaluate()`; tres combinaciones de parcialidad sin fallo global. | `InOrder`, ausencia de evaluación anticipada, sin llamadas adicionales y resultado terminal `COMPLETED`/`PARTIAL`. |
| US1, FR-004, SC-003 | Inicio y resultado terminal; fallo identifica JSON, scraping o evaluación. | Eventos exactos y causa segura; evaluación con fallos individuales no se reclasifica como fallo global. |
| US1, FR-008, SC-005 | Contexto con servicio de cotizaciones disponible pero no invocado. | `verifyNoInteractions` sobre cotizaciones; scheduler solo recibe dos fachadas deportivas. |
| US2, FR-003, SC-003 | JSON bloqueado/429, scraping fallido y evaluación sin snapshot; interrupción previa, entre etapas y anidada. | Omisión de etapas posteriores, preservación del indicador del hilo y ausencia de propagación al scheduler. |
| US2, FR-004 | Error inesperado con mensaje, causa y suprimido privados; fallo de cierre comunicado de forma segura. | `getThrowableProxy()==null`, ausencia de secretos de los dobles, solo `Reason`/identidad y `closeFailed`. |
| US2, FR-006, SC-004 | Disparo posterior tras fallo; relanzamiento manual tras corte global, error inesperado e interrupción del coordinador JSON; navegador no disponible y recuperación manual scraping. | No queda bloqueo de run tras un cierre exitoso; se conserva el error manual y los resultados previos. |
| Fundamentos, FR-007, SC-005 | Una confirmación JSON previa al error inesperado; relanzamiento selecciona solo cuatro pendientes y completa cobertura. | Conservación observable desde la API del Service, sin assertions de repositorio. |
| Fundamentos, FR-003, FR-009 | `FAILED`, `INTERRUPTED`, `PARTIAL` por fallo individual y corte global con resumen parcial; éxitos completos. | Motivo exacto vs booleano de parcialidad, sin umbrales ni porcentajes. |
| Fundamentos, TD-037 y plan §Diseño 7 | Error inesperado con fallo de cierre y fallo de cierre sin error previo. | Causa principal idéntica, excepción de cierre suprimida cuando corresponde, `CLOSE_FAILED` cuando el cierre es el único fallo y rechazo de relanzamiento si no pudo confirmarse el cierre. |

## Mocks e infraestructura

Los tests del scheduler sustituyen únicamente las dos fachadas y capturan Logback con `ListAppender`; los de configuración usan `ApplicationContextRunner`, proveedores de beans mock y el YAML de test sin DB ni timer real. El servicio de cotizaciones del contexto es un doble para comprobar que no se usa.

Los tests Service mantienen la interfaz y la implementación reales, repositorios, Hibernate y PostgreSQL reales. Solo se sustituyen las Integrations externas (JSON, scraping e identidad) y se fija la configuración de workers JSON a uno. Los datos de identidad y run se preparan con los helpers existentes en Arrange; Act y las assertions funcionales usan la fachada productiva. No se invocan proveedores reales.

El helper técnico de fallo de cierre decora conexiones reales y produce una SQLException controlada antes de una sentencia de update del run, únicamente en el hilo armado. No crea datos, no interroga resultados ni reemplaza Repository/JPA por mocks. Permite ejercer rollback y conservación del error sin detener contenedores ni afectar bases externas. Revisar su matcher, armado de un solo uso y reset en `@AfterEach`.

## Riesgos de falsos positivos y límites

- La suite Service presupone fixture aislada y orden ascendente del catálogo; un worker JSON hace determinista la confirmación previa al jugador `1002`. Confirmar esos supuestos al ejecutar.
- El helper debe interceptar efectivamente el update de cierre. Los tests afirman señal/suprimidos y rechazo manual posterior; si no intercepta, esas assertions deben fallar. No se valida el helper con assertions internas de persistencia.
- El test cron verifica contrato y cálculo temporal, no puntualidad real del timer, aplicación suspendida ni disparos perdidos. Esos escenarios están fuera del alcance.
- No se fabrica el caso `PROVIDER_UNAVAILABLE` con `COMPLETED` cuando el recorrido registra un item fallido, según la decisión upstream. No se afirma imposibilidad universal ni cobertura ejecutada de esa combinación.
- Si la DB no puede confirmar un cierre, no se promete disponibilidad inmediata. TD-037 permanece En curso; su cierre requiere evidencia humana. El comportamiento manual preexistente ante fallos inesperados permanece intacto por decisión del plan.
- No hay ejecución de tests ni resultados aportados. La clasificación estática de 143 clases y el build sin tests no prueban compilación de tests ni comportamiento funcional.

## Revisión y ejecución humana

Primero revisar el Code Review Report y sus archivos; después mocks, assertions, aislamiento y fallo de cierre de este informe. Requisitos: JDK 21 y, para Service, Docker disponible para PostgreSQL aislado. No utilizar DB de desarrollo o producción ni claves reales.

Desde `backend/`, ejecutar personalmente:

```powershell
.\gradlew.bat unitTest --tests 'footballmarket.features.statistics.infraestructure.scheduling.AutomaticSportsDataSchedulerTest*'
.\gradlew.bat lightweightTest --tests 'footballmarket.features.statistics.infraestructure.scheduling.AutomaticSportsDataSchedulerConfigurationTest*'
.\gradlew.bat integrationTest --tests 'footballmarket.features.statistics.application.shared.PlayerStatisticsSynchronizationServiceTest*'
```

Las selecciones incluyen grupos `@Nested`. Los comandos compilan los tests y ejecutan cada categoría; si falla la compilación o algún escenario, aportar la salida para clasificarlo antes de modificar expectativas o código. Si pasan, confirmar explícitamente los resultados y el alcance revisado; no es necesario pegar toda la salida satisfactoria. Ninguno de estos comandos fue ejecutado por el agente.

**CODE REVIEW y TESTING siguen pendientes y son autorizaciones humanas separadas.** Este informe no ejecuta ni aprueba la fase siguiente ni autoriza el cierre.

## Regresiones añadidas por hallazgo P2 — 2026-11-06

Se añaden tres tests a los archivos ya descritos, sin borrar ni cambiar las expectativas previas:

1. `scrapingAutomaticoComunicaDobleFallo`: Integration de navegador no disponible + fallo controlado del update de cierre en PostgreSQL de testing. Afirma `ScheduledStatisticsSyncException`, motivo `HEADED_BROWSER_UNAVAILABLE`, causa idéntica al fallo del navegador y un cierre suprimido; el mensaje propio no filtra detalle privado.
2. `scrapingManualConservaContratoAnteDobleFallo`: misma combinación en la entrada manual. Afirma identidad de la excepción original y un cierre suprimido, sin convertirla a señal automática.
3. `scrapingRegistraDobleFalloSeguro`: fachada mock devuelve la señal con causa y suprimido privados. Afirma log `stage=SCRAPING cause=HEADED_BROWSER_UNAVAILABLE closeFailed=true`, ausencia de secretos/trazas y cero evaluación posterior.

Trazabilidad: FR-004 (comunicación segura del fallo), FR-006 (contrato manual preservado) y T003/T004/T009/T011. Los dos primeros son Service con persistencia real y el inyector técnico existente; el tercero es unitario del disparador. El grupo Service tiene ahora dieciocho métodos nuevos y la clase del scheduler trece, uno parametrizado con tres casos. TD-038 queda En curso hasta revisión y ejecución humana. No se afirma resultado funcional ni se ejecutan tests.

Para estas regresiones siguen vigentes los comandos `unitTest` e `integrationTest` indicados arriba: sus filtros incluyen los casos nuevos. Revisar especialmente que el inyector intercepte el cierre, que el error original no se pierda y que la señal automática no cambie la excepción manual.

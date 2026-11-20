# Plan de implementación: programación semanal deportiva

**Rama**: `feat/024-schedule-sports-data-sync` | **Fecha**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Resumen

Incorporar un disparador batch independiente que reutilice Spring Scheduling y ejecute sincrónicamente JSON → scraping → evaluación. La zona es `America/Argentina/Buenos_Aires`, domingo 12:00. No invoca recálculo ni operaciones financieras. Las tres operaciones mantienen sus transacciones, fechas, resultados parciales y contratos manuales. El detalle de decisiones está en [research.md](research.md), los límites en [contracts/scheduled-execution.md](contracts/scheduled-execution.md) y la validación humana en [quickstart.md](quickstart.md).

## Contexto técnico

- **Lenguaje y plataforma**: Java 21, Spring Boot 4.1.1, Gradle 9.6.0; backend con PostgreSQL existente.
- **Dependencias**: scheduling Spring, SLF4J/Logback y operaciones de SPEC-012 y SPEC-013 ya disponibles; ninguna biblioteca, infraestructura, endpoint o interfaz de usuario nuevos.
- **Almacenamiento**: sin modelo persistente ni migración nuevos. Persistencia y auditoría pertenecen a las operaciones existentes; la ejecución automática se observa en logs.
- **Testing**: JUnit, Mockito y AssertJ existentes; pruebas deterministas de coordinación, fallos, logs y configuración/cron según `docs/backend/testing.md`, ejecutadas exclusivamente por el usuario. No tests frontend.
- **Restricciones**: no recuperación de disparos perdidos, retries, paralelismo, umbrales de cobertura, historial propio ni configuración funcional del horario. La aplicación debe estar activa y las operaciones no deben ejecutarse en paralelo, según la spec.
- **Alcance y rendimiento**: una secuencia semanal sin objetivo de latencia adicional. El scheduler compartido tiene un único hilo: una tarea previa larga puede retrasar el disparo; no se promete puntualidad bajo solapamiento, excluido por la spec.

## Constitution Check

| Garantía | Antes de investigación | Después del diseño |
| --- | --- | --- |
| Simplicidad y mínimo alcance | PASS: reutilizar scheduling y servicios | PASS: un disparador y señal interna mínima; sin infraestructura adicional |
| Proporcionalidad | PASS: solo escenarios de SPEC-024 | PASS: no se agregan cobertura, reintentos, recuperación ni concurrencia |
| Seguridad e integridad | PASS: operaciones existentes y registro sin secretos | PASS condicionado en implementación: no registrar mensajes arbitrarios ni datos sensibles; conservar códigos seguros y transacciones propias |
| Contratos y consumidores | PASS: preservar POST manuales | PASS condicionado: señal interna aditiva, sin alterar requests, respuestas ni semántica de SPEC-012/013; verificar consumidores y tests |
| Trazabilidad | PASS: spec y requisitos externos identificados | PASS: diseño y pruebas trazan FR y escenarios; seguimiento no es fuente de requisitos |
| Idioma y testing | PASS: documentación en español, código en inglés; tests a cargo del usuario | PASS: sin ejecución de tests ni build en PLAN |
| Autorizaciones | PASS: PLAN no autoriza implementar | PASS: CHECKLIST, TASKS, ANALYZE y autorización humana siguen pendientes |

No se identifican violaciones constitucionales que requieran justificar complejidad. Los PASS describen el diseño propuesto, no verifican implementación.

## Diseño

1. Añadir un punto de entrada de infraestructura batch en el área de estadísticas que invoque las interfaces de aplicación existentes, sin Controller, DTO HTTP, Repository ni Integration. Usar `@Scheduled(cron = "0 0 12 * * SUN", zone = "America/Argentina/Buenos_Aires")`; reutilizar `@EnableScheduling` y el `TaskScheduler` existentes. Mantener el disparador inerte en perfil `test` sin cambiar la programación funcional de producción.
2. Invocar síncronamente la operación JSON, luego scraping con `PlayerStatisticsSyncRequest(null)`, y finalmente `ValuationEvaluationService.evaluate()`. Cada llamada termina antes de la siguiente. La evaluación mantiene su selección propia de campaña; no se fuerza la fecha de JSON o scraping.
3. La interfaz actual de sincronización no comunica al consumidor la causa de interrupción global JSON en todos los casos: `PARTIAL` o `COMPLETED` pueden ocultar `PROVIDER_UNAVAILABLE`. Por ello, ampliar **internamente** la fachada de SPEC-012 con entradas específicas para ejecución automática que reutilicen exactamente los mismos recorridos y propaguen una señal controlada de fallo global/interrupción, incluida la causa segura comunicada por la operación. La detección debe originarse donde ya se conoce el fallo, no inferirse de `coverageComplete`, contadores ni estado parcial. La entrada manual y sus respuestas HTTP no cambian. Para `FAILED`/`INTERRUPTED` sin excepción, traducir el resultado y código disponibles en la misma fachada a la señal interna; `PARTIAL` sin fallo global continúa.
4. El disparador coordina las llamadas y captura la señal o excepción global de la etapa actual; no inspecciona Model, calcula métricas ni gestiona transacciones. La fachada automática comunica además un indicador operativo simple de parcialidad, derivado del resumen existente sin exponer contadores. Corta inmediatamente las etapas siguientes y registra etapa y código/motivo seguro; ante excepción inesperada, usar una identificación segura sin volcar texto arbitrario, credenciales ni stack trace. Preservar el indicador de interrupción cuando corresponda. Tras el fallo el disparador retorna y no deshabilita futuras ejecuciones ni llamadas manuales.
5. Registrar inicio y resultado terminal, distinguiendo éxito, resultado parcial de sincronización sin fallo global y fallo, sin copiar contadores ni cobertura. La evaluación conserva su resultado propio y su retorno normal se comunica como etapa terminada, sin reclasificar sus fallos individuales. No inventar un error original cuando la operación no lo haya comunicado: usar un código estable de estado y conservar la causa segura disponible. Las excepciones controladas nuevas deben respetar la jerarquía `FootballMarketException`; preservar las excepciones y contratos manuales existentes.
6. Actualizar en implementación la documentación arquitectónica del nuevo punto de entrada, sin cambiar el contrato HTTP. Añadir las pruebas backend necesarias, incluida regresión de indisponibilidad global JSON con estado `PARTIAL` y preservación del comportamiento manual. No ejecutar tests por el agente.
7. Ante una excepción inesperada o interrupción posterior a la apertura de una campaña JSON automática, finalizar primero los workers y su limpieza y cerrar la campaña como interrumpida antes de comunicar el fallo; conservar las confirmaciones previas y el indicador de interrupción. Preservar el error principal y, si el cierre también falla, conservar además ese fallo sin ocultarlo ni afirmar que la campaña se cerró. Si la persistencia impide el cierre, comunicar el fallo sin prometer disponibilidad inmediata. La entrada manual conserva sus contratos y comportamiento; no se agregan reintentos ni recuperación nueva. Esta decisión cubre FR-006 y el impacto de TD-037 en el recorrido automático.
8. Verificar la señal de `PROVIDER_UNAVAILABLE` desde la fachada real en los escenarios deterministas reproducibles, incluido el resumen `PARTIAL`, respetando las fronteras de testing. Por decisión del usuario no se exige fabricar una prueba para la combinación `PROVIDER_UNAVAILABLE` y `COMPLETED` cuando el recorrido registra un item fallido; se conserva la prioridad del fallo global sobre el estado definida en el contrato, sin afirmar la imposibilidad universal de esa combinación.

## Estructura prevista

```text
specs/024-schedule-sports-data-sync/
  spec.md  plan.md  research.md  data-model.md  quickstart.md
  contracts/scheduled-execution.md
backend/src/main/java/footballmarket/features/statistics/
  application/                         # fachada existente y señal interna mínima
  application/shared/                 # detección y cierre de los recorridos existentes
  infraestructure/scheduling/         # disparador nuevo
backend/src/main/java/footballmarket/features/valuation/application/
  ValuationEvaluationService.java     # consumidor sin cambios funcionales previstos
backend/src/test/java/footballmarket/features/statistics/
  ...                                 # pruebas de coordinación y regresión
backend/src/test/resources/application-test.yml  # aislamiento del disparador
docs/backend/architecture.md          # coherencia del punto de entrada en implementación
```

La ubicación concreta de pruebas y señal sigue la estructura vigente y la frontera de cada categoría; no se crea una abstracción de jobs ni una dependencia Service → Service. Los puntos de entrada y estados se precisan en el contrato de diseño.

## Límites y evidencia de fase

- PLAN se limita al diseño; no crea `tasks.md`, no cambia código ni autoriza implementación.
- Se consultaron Constitution, workflow, arquitectura, tecnologías, testing, requisitos académicos, spec y código relevante. Investigación delegada de solo lectura contrastada con fuentes.
- Setup real: `.specify/scripts/bash/setup-plan.sh --json` creó la plantilla; devolvió `BRANCH=024-schedule-sports-data-sync` como identificador de carpeta, mientras Git confirma la rama real indicada arriba.
- La siguiente fase es CHECKLIST y su evaluación; no se ejecuta automáticamente.

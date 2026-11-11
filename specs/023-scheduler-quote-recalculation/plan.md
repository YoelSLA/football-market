# Implementation Plan: Recálculo automático programado de cotizaciones

**Branch**: `feat/023-daily-quote-scheduling` | **Date**: 2026-11-11 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `/specs/023-scheduler-quote-recalculation/spec.md`

**Note**: Este documento es el resultado de la fase de planificación. Los artefactos de diseño que lo
acompañan son [`research.md`](research.md) (Phase 0), [`data-model.md`](data-model.md),
[`contracts/quote-executions-http.md`](contracts/quote-executions-http.md),
[`contracts/scheduling-configuration.md`](contracts/scheduling-configuration.md) y
[`quickstart.md`](quickstart.md).

## Summary

**Límite de responsabilidad (decisión 2026-11-04):** el scheduler invoca el caso de uso existente y registra sus errores; no es responsable de la persistencia, atomicidad, compensación, recuperación ni reintentos del recálculo. No se añade exclusión global entre corridas manuales. La corrida automática diaria requiere idempotencia por fecha y origen para impedir que disparos repetidos creen corridas automáticas adicionales.

Se añade la ejecución periódica y desatendida del recálculo de cotizaciones que hoy solo corre por
petición manual (SPEC-016) sobre las valuaciones de SPEC-013. La cadencia por defecto es diaria a las
12:00 UTC y se resuelve con una expresión cron configurable evaluada siempre en UTC.
El disparo automático reutiliza el mismo caso de uso, la misma configuración del mercado y las
mismas fórmulas: no se duplica ni se modifica lógica financiera.

El enfoque técnico es: habilitar el scheduling nativo de Spring una sola vez en el arranque y
apoyarse en el `TaskScheduler` que autoconfigura Spring Boot, con un único hilo; un disparador
delgado en el módulo de cotización que invoca `QuoteRecalculationService` y traduce los rechazos
controlados a eventos del registro operativo (logs SLF4J existentes); persistencia del origen
(`MANUAL` | `AUTOMATIC`) en la tabla `quote_executions` ya existente, con el resultado y el motivo de
fallo derivados de sus contadores; y exposición aditiva del origen, resultado y motivo en el DTO de
consulta de ejecuciones, sin rutas nuevas. Se admiten varias ejecuciones por fecha, con como máximo
una automática diaria. La unicidad de cotización pasa a jugador-fecha, independiente del origen; una
nueva corrida actualiza la fila existente con el último cálculo.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.1, Gradle 9.6.0 con Kotlin DSL).

**Primary Dependencies**: sin dependencias nuevas. El soporte de scheduling (`@EnableScheduling`,
`@Scheduled`, `TaskScheduler`) proviene de `spring-context` y de la autoconfiguración de Spring Boot,
ya presentes en el classpath. Se reutilizan Spring Data JPA, Flyway, Jakarta Validation, SpringDoc
OpenAPI, PostgreSQL (pgJDBC), SLF4J y Logback.

**Storage**: PostgreSQL. Sin tablas nuevas. El origen en `quote_executions` se añadió mediante la
migración T009 ya completada. El delta de corridas múltiples y cotización diaria única requiere una
migración Flyway adicional (T040): retirar la unicidad global por `quote_date`, añadir unicidad
condicional para ejecuciones `AUTOMATIC` por fecha y cambiar la unicidad de cotización a jugador-fecha
con actualización transaccional. La migración debe generarse con `./gradlew createMigration` y
timestamp estrictamente creciente. El registro operativo no se persiste: usa los logs estructurados
vigentes.

**Testing**: se mantienen las categorías y restricciones de
[`docs/backend/testing.md`](../../docs/backend/testing.md). No se añade E2E. Los tests correspondientes
se especificarán en una fase posterior y los ejecuta el usuario; el agente no ejecuta tests.

**Target Platform**: servidor con JVM 21 y PostgreSQL accesible; sin dependencias específicas de
sistema operativo. Despliegue de una única instancia (no existe configuración multiinstancia en el
repositorio).

**Project Type**: backend web-service (Spring Boot) dentro de un monorepo con frontend Angular. El
frontend no participa en esta feature.

**Performance Goals**: no se fijan objetivos propios. La feature añade un disparo periódico y tres
campos derivados a una consulta ya existente; no introduce caché, métricas ni procesamiento
adicional.

**Constraints**: evaluación en UTC fija, sin horario estacional; corridas manuales ilimitadas y como
máximo una corrida automática por fecha UTC; cadencia y estrategia ajustables por configuración sin
recompilar; el scheduler registra los errores sin compensar cotizaciones ya confirmadas por jugadores individuales;
la persistencia y recuperación son responsabilidad heredada de SPEC-016 y fuera del alcance del scheduler
(TD-024); el resultado expuesto no incluye trazas, mensajes técnicos ni identificadores internos;
retención indefinida de los registros de ejecución. El uso operativo no inicia recálculos simultáneos
ni dispara la ejecución automática mientras otro recálculo está en curso; esta expectativa no requiere
implementar exclusión mutua global.

**Scale/Scope**: una feature de backend. Añade un componente disparador, configuración, un método de
caso de uso, enums para origen/resultado/motivo, cambios de restricciones persistidas, migraciones
Flyway y campos aditivos en el DTO de ejecución. No añade rutas, tablas, consultas históricas nuevas,
dependencias ni cambios de frontend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / garantía | Evaluación | Resultado |
| --- | --- | --- |
| §I Simplicidad y calidad | Scheduling nativo de Spring, ya disponible; un único disparador y un único `@ConfigurationProperties` ampliado; ninguna abstracción de jobs, cola, bloqueo global ni registro central | PASS |
| §II Proporcionalidad del alcance | Ejecución periódica, trazabilidad por corrida, límite de una automática diaria, actualización de la cotización jugador-día y rechazos controlados. No se implementa #41, reintentos automáticos, purga, métricas ni pantalla de administración | PASS |
| §III Seguridad e integridad | Sin secretos nuevos; la ejecución automática reutiliza las validaciones de mercado, estrategia y parámetros; los motivos expuestos son códigos cerrados sin trazas ni detalles internos (FR-010); ningún endpoint nuevo que amplíe la superficie de ataque | PASS |
| §IV Coherencia de contratos | El request de `POST /api/quotes/recalculate` no cambia; los campos nuevos del DTO son aditivos y no rompen consumidores; se actualizan en el mismo cambio el contrato HTTP de SPEC-016, las anotaciones OpenAPI y los tests de Controller afectados | PASS |
| §V Trazabilidad y fuente de verdad | Cada corrida queda identificable con ID, origen, fecha UTC, resultado y alcance en el registro persistido existente; el registro operativo complementa ese registro | PASS |
| §VI Mínimo alcance necesario | Se reutilizan el caso de uso, la tabla de ejecuciones, la consulta por fecha, los logs y la configuración de mercado existentes; se descartan Quartz, locks consultivos, tablas nuevas, endpoints nuevos y abstracciones sin segundo consumidor | PASS |
| Gestión de datos en desarrollo | Migración aditiva que preserva filas existentes, retira unicidad incompatible y añade unicidad para corridas automáticas y cotización recalculada jugador-día; sin reescribir migraciones aplicadas | PASS |
| Idioma | Código y serialización en inglés; documentación, contratos y mensajes en español | PASS |
| Verificación y testing | El agente no ejecutará tests; el único control ejecutable admitido es `./gradlew build -x test` desde `backend/` | PASS |
| Autorizaciones humanas | El plan no implementa código ni habilita Implement; la fase siguiente es Checklists, luego Tasks y Analyze | PASS |

**Resultado del gate previo a Phase 0**: PASS, sin violaciones que exijan Complexity Tracking.

### Re-evaluación tras el diseño (Phase 1)

| Principio / garantía | Evaluación tras el diseño | Resultado |
| --- | --- | --- |
| §I Simplicidad | El disparador usa `TaskScheduler`; la idempotencia automática se fuerza en base y se maneja en el caso de uso, sin lock global manual | PASS |
| §II Proporcionalidad | Corridas manuales múltiples, una automática diaria y cotización única jugador-día quedan resueltas en [`research.md`](research.md); no se añaden requisitos | PASS |
| §III Seguridad e integridad | Restricciones de base evitan duplicados automáticos y cotizaciones jugador-día; rechazos por mercado no inicializado no escriben datos; los motivos expuestos son códigos cerrados | PASS |
| §IV Coherencia de contratos | Los campos nuevos se documentan en [`contracts/quote-executions-http.md`](contracts/quote-executions-http.md) y obligan a actualizar el contrato de SPEC-016 en el mismo cambio; la configuración y el registro operativo quedan fijados en [`contracts/scheduling-configuration.md`](contracts/scheduling-configuration.md) | PASS |
| §V Trazabilidad | Consulta por fecha retorna todas las corridas identificadas; resultado y motivo se derivan de datos persistidos y el registro operativo documenta intentos omitidos | PASS |
| Verificación y testing | La guía de validación es manual y la ejecuta el usuario; no se ejecutan tests ni builds de test | PASS |

**Resultado del gate posterior a Phase 1**: PASS. Sin violaciones justificables: la sección
Complexity Tracking queda vacía.

## Decisiones de diseño (resumen)

El detalle, con alternativas evaluadas, está en [`research.md`](research.md):

| Decisión | Referencia |
| --- | --- |
| `@EnableScheduling` + `@Scheduled` diario con cron por propiedad y `zone = "UTC"` | §1 |
| Un `TaskScheduler` autoconfigurado de un solo hilo y `spring.task.scheduling.pool.size: 1` | §2 |
| Corridas manuales ilimitadas; unicidad condicional automática por fecha, sin lock global manual | §3 |
| Actualización de cotización única por jugador y fecha UTC, último cálculo prevalece | §17 |
| `market.quote.strategy` y `market.quote.schedule.{cron,enabled}` en `QuoteProperties` | §4 |
| Columna `origin` en `quote_executions` con catálogo cerrado y sin valor por defecto permanente | §5, §11 |
| `result` y `failureReason` derivados de `failed_players`, no persistidos | §6 |
| Fallo completo sin compensación explícita: rollback de la transacción externa | §7, §16 |
| Traducción de rechazos a eventos operativos en el disparador | §8 |
| Expresión cron y fecha de cotización en UTC | §9 |
| DTO de ejecución aditivo, sin rutas nuevas | §10 |
| Sin purga: retención indefinida | §12 |
| Eventos de log acotados sobre SLF4J | §13 |
| `recalculateAutomatic()` como segundo método del Service, con núcleo privado compartido | §15 |

## Project Structure

### Documentation (this feature)

```text
specs/023-scheduler-quote-recalculation/
├── plan.md                                   # This file (/speckit.plan command output)
├── research.md                               # Phase 0 output (/speckit.plan command)
├── data-model.md                             # Phase 1 output (/speckit.plan command)
├── quickstart.md                             # Phase 1 output (/speckit.plan command)
├── contracts/                                # Phase 1 output (/speckit.plan command)
│   ├── quote-executions-http.md
│   └── scheduling-configuration.md
├── checklists/                               # Evaluación de calidad (fase siguiente)
└── tasks.md                                  # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── FootballMarketApplication.java                         # + @EnableScheduling (una sola vez)
└── features/quote/
    ├── application/
    │   ├── QuoteRecalculationService.java                  # + recalculateAutomatic()
    │   └── impl/QuoteRecalculationServiceImpl.java         # núcleo compartido y origen
    ├── domain/
    │   ├── enums/
    │   │   ├── QuoteExecutionOrigin.java                   # MANUAL | AUTOMATIC (nuevo)
    │   │   ├── QuoteExecutionResult.java                   # SUCCESS | FAILED (nuevo, derivado)
    │   │   └── QuoteExecutionFailureReason.java            # INDIVIDUAL_PLAYER_FAILURES (nuevo, derivado)
    │   └── models/QuoteExecution.java                      # + origin, + resultado y motivo derivados
    ├── infraestructure/
    │   ├── config/
    │   │   ├── QuoteProperties.java                        # + strategy, + schedule
    │   │   └── QuoteConfig.java                            # sin cambios de registro
    │   └── scheduling/
    │       └── AutomaticQuoteRecalculationScheduler.java   # disparador @Scheduled (nuevo)
    └── presentation/
        ├── QuoteExecutionMapper.java                       # + campos derivados
        ├── QuoteController.java                            # sin cambios de firma; revisar OpenAPI
        └── dtos/response/QuoteExecutionResponseDTO.java    # + origin, + result, + failureReason

backend/src/main/resources/
├── application.yml                                        # + market.quote.strategy y schedule.*
└── db/migration/
    ├── V<timestamp>__add_quote_execution_origin.sql        # T009, ya completada
    └── V<timestamp>__allow_daily_quote_recalculation.sql   # T040, delta pendiente

backend/src/test/resources/
└── application-test.yml                                   # scheduler inerte en la suite

specs/016-ejecutar-recalculo-manual-de-cotizaciones/
└── contracts/quotes-http.md                               # actualización aditiva obligatoria (§IV)
```

**Structure Decision**: se conserva la estructura real por funcionalidad del backend
(`features/quote/{application,domain,infraestructure,presentation}`, con la grafía `infraestructure`
vigente). El disparador se ubica en `features/quote/infraestructure/scheduling/` porque es un punto
de entrada de infraestructura hacia un caso de uso, no una frontera HTTP ni un rol de la matriz de
dependencias: solo depende de la interfaz `QuoteRecalculationService` y de `QuoteProperties`, y no
expone contrato HTTP. No se crean módulos, paquetes raíz ni proyectos nuevos, y no se mueve ningún
componente existente.

**Nota sobre documentación de arquitectura**: la sección de roles de
[`docs/backend/architecture.md`](../../docs/backend/architecture.md) no describe un punto de entrada
de proceso batch. El disparador se define aquí con responsabilidad y dependencias explícitas —invocar
el caso de uso, traducir rechazos controlados a eventos operativos y no contener lógica de negocio—
conforme a la regla de vacíos normativos de la arquitectura común. Si el equipo lo considera
necesario, la incorporación de esa responsabilidad a la documentación de arquitectura es una
obligación de coherencia del mismo cambio.

## Límites de la fase

- No se generan `tasks.md` ni ningún artefacto de TASKS: la fase siguiente es Checklist y su
  evaluación.
- No se implementa código ni se ejecutan builds ni tests de aplicación en esta fase.
- No se modifica `spec.md` ni `checklists/requirements.md`.
- El plan no autoriza implementación: requiere IMPLEMENT AUTHORIZATION humana explícita y vigente.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones: la sección queda vacía. Las decisiones con alternativas evaluadas están registradas
en [`research.md`](research.md) (mecanismo de programación, infraestructura reutilizable,
exclusión de solapamiento, origen de la configuración, persistencia del origen, resultado derivado,
fallo completo, traducción de rechazos, zona horaria, contrato HTTP, migración, retención,
observabilidad, pruebas y punto de entrada del caso de uso).

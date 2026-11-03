# Implementation Plan: Consultar ranking de jugadores por score

**Branch**: `feat/018-consultar-rankings-de-jugadores` | **Date**: 2026-10-23 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `/specs/018-consultar-rankings-de-jugadores/spec.md`

**Note**: Este documento es el resultado de la fase de planificación. Los artefactos de diseño que lo
acompañan son [`research.md`](research.md) (Phase 0), [`data-model.md`](data-model.md),
[`contracts/score-ranking-http.md`](contracts/score-ranking-http.md) y [`quickstart.md`](quickstart.md).

## Summary

Se añade una consulta de solo lectura del ranking de jugadores ordenado por el score final de la
estrategia registrada en la última ejecución de cotización persistida. La consulta toma el encabezado de
`quote_executions` (estrategia y fecha de cotización), selecciona para cada jugador con cotización
vigente su valuación más reciente de esa estrategia en `player_positional_valuations` o
`player_attribute_valuations`, y devuelve una entrada por jugador con posición, `playerId`, score y
fecha de valuación, ordenada por score descendente y, a igualdad, por `playerId` ascendente.

La valuación más reciente es la de mayor fecha. El esquema garantiza una sola fila por jugador,
fecha, estrategia y versión, y la publicación reemplaza el resultado anterior cuando se reevalúa la
misma fecha; por ello no se requieren desempates por instante de cálculo ni por identificador.

La ejecución determina únicamente la estrategia y el encabezado: la valuación elegida no se limita por
`quoteDate` y puede ser posterior o distinta de la usada para calcular la última cotización. El flujo
operativo habitual es métricas → valuaciones → cotizaciones con frecuencia semanal, pero la consulta
tolera estados intermedios o fallos parciales entre esas etapas.

No se crean tablas, migraciones ni índices nuevos: la feature reutiliza el historial de cotizaciones,
el resumen de ejecuciones y las valuaciones ya persistidas por SPEC-013 y SPEC-016. El ranking por
precio de `GET /api/players/ranking` se conserva sin cambios; el nuevo recurso se expone en una ruta
propia.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.1, Gradle 9.6.0 con Kotlin DSL)

**Primary Dependencies**: Spring Web MVC, Spring Data JPA (Hibernate), Spring Security (OAuth2
Resource Server, JWT), SpringDoc OpenAPI, PostgreSQL 18 (pgJDBC). Sin dependencias nuevas: se
reutilizan los Models, Repositories y enums de SPEC-013 y SPEC-016.

**Storage**: PostgreSQL. Sin migraciones nuevas ni cambios de esquema. Se leen `quote_executions`,
`player_quotes`, `player_positional_valuations` y `player_attribute_valuations`.

**Testing**: JUnit Jupiter + Mockito + AssertJ + MockMvc + Testcontainers, con las categorías
`lightweight` e `integration` del backend. Los tests se crean en la fase de implementación; el agente
no los ejecuta.

**Target Platform**: Servidor con JVM 21 y PostgreSQL accesible; sin dependencias específicas de
plataforma.

**Project Type**: backend web-service (Spring Boot) dentro del monorepo con frontend Angular; esta
feature no modifica el frontend.

**Performance Goals**: No se fijan objetivos propios. La consulta se resuelve con dos lecturas
acotadas (última ejecución y valuaciones más recientes), sin caché ni recálculo.

**Constraints**: Solo lectura; no declara ni utiliza parámetros de consulta y los no declarados se
ignoran sin alterar el resultado; sin recalcular precios, scores ni valuaciones; importes ajenos a
esta feature; score devuelto tal como está persistido a seis decimales; posiciones consecutivas de
1 a n calculadas por el Service; una sola entrada por jugador; selección de valuación únicamente por
mayor fecha conforme a la unicidad vigente; JWT válido obligatorio.

**Scale/Scope**: Una feature de backend. Añade 1 Controller, 1 Service (interfaz + implementación),
1 Mapper, 2 Response DTO, 2 records de Model, 2 consultas JPQL nuevas y 1 consulta derivada nueva.
No añade tablas, migraciones, dependencias, códigos de error ni cambios en el ranking por precio.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / garantía | Evaluación | Resultado |
| --- | --- | --- |
| §I Simplicidad y calidad | Se reutilizan las tablas y consultas del dominio existente; no se crean abstracciones, caché ni dependencias nuevas. La lectura se concentra en un Service con interfaz e implementación separadas, como exige la arquitectura | PASS |
| §II Proporcionalidad del alcance | Se implementa exactamente lo especificado para la parte faltante de `REQ-S2-08`: un ranking por score de solo lectura. No se incorporan filtros, paginación, estrategia por parámetro ni interfaz web, todos fuera de alcance | PASS |
| §III Seguridad e integridad | Endpoint bajo `/api/` con JWT obligatorio heredado de `SecurityConfig`; sin entradas externas que validar; solo lectura; sin secretos ni exposición del detalle interno en errores | PASS |
| §IV Coherencia de contratos | Se documenta el recurso nuevo en OpenAPI, en `contracts/` y en tests de Controller en el mismo cambio. El ranking por precio queda fuera de alcance y no se modifica | PASS |
| §V Trazabilidad y fuente de verdad | El encabezado se deriva de la ejecución persistida y cada entrada conserva la fecha de la valuación utilizada; no se duplica estado ni se inventan posiciones | PASS |
| Gestión de datos en desarrollo | Sin migraciones ni mecanismos de compatibilidad; no hay datos nuevos que conservar | PASS |
| Idioma | Código en inglés; documentación, contrato y mensajes en español | PASS |
| Verificación y testing | El agente no ejecutará tests. El control ejecutable admitido es `./gradlew build -x test` desde `backend/`, solo en la fase de implementación | PASS |
| Autorizaciones humanas | El plan no implementa ni habilita Implement. La fase siguiente es la evaluación de checklists; Tasks y Analyze siguen después | PASS |

**Resultado del gate previo a Phase 0**: PASS, sin violaciones que exijan Complexity Tracking.

### Re-evaluación tras el diseño (Phase 1)

| Principio / garantía | Evaluación tras el diseño | Resultado |
| --- | --- | --- |
| §I Simplicidad | El diseño mantiene una sola ruta nueva y un único Service; el orden se resuelve en la consulta JPQL, sin replicar la lógica en el Service ni en el Mapper | PASS |
| §II Proporcionalidad | Cada caso límite de `spec.md` tiene una regla explícita en [`research.md`](research.md) §3–§5, sin requisitos añadidos | PASS |
| §III Seguridad | La consulta no recibe entrada del usuario; la transacción es de solo lectura y la respuesta no expone información técnica ante fallos | PASS |
| §IV Coherencia de contratos | El contrato define campos, tipos, nulabilidad y errores; el recurso nuevo se implementa en componentes propios y el ranking por precio queda fuera de alcance | PASS |
| §V Trazabilidad | El score proviene de la fila persistida y la fecha de valuación identifica el dato usado; el encabezado identifica la ejecución | PASS |
| Verificación y testing | La validación descrita en [`quickstart.md`](quickstart.md) es manual y la ejecuta el usuario; no se ejecutaron builds ni tests en esta fase | PASS |

**Resultado del gate posterior a Phase 1**: PASS. Sin violaciones justificables: la sección
Complexity Tracking queda vacía.

## Project Structure

### Documentation (this feature)

```text
specs/018-consultar-rankings-de-jugadores/
├── plan.md                      # This file (/speckit.plan command output)
├── research.md                  # Phase 0 output
├── data-model.md                # Phase 1 output
├── quickstart.md                # Phase 1 output
├── contracts/
│   └── score-ranking-http.md    # Phase 1 output
├── checklists/
│   └── requirements.md          # Existente: se evalúa en la fase siguiente
└── tasks.md                     # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── controllers/
│   ├── ScoreRankingController.java                  # GET /api/players/ranking/score
│   ├── dtos/responses/
│   │   ├── ScoreRankingResponseDTO.java             # strategy, quoteDate y ranking
│   │   └── ScoreRankingEntryResponseDTO.java        # position, playerId, score, valuationDate
│   └── mappers/
│       └── ScoreRankingMapper.java                  # Model → DTO, copia position sin calcularla
├── models/records/
│   ├── ScoreRanking.java                            # Encabezado + entradas ordenadas
│   └── ScoreRankingEntry.java                       # position, playerId, score, valuationDate
├── repositories/
│   ├── QuoteExecutionRepository.java                # + última ejecución por fecha
│   ├── PositionalValuationRepository.java           # + ranking por final_score
│   └── AttributeValuationRepository.java            # + ranking por final_grl
└── services/
    ├── ScoreRankingQueryService.java                # Interfaz del caso de uso
    └── impl/ScoreRankingQueryServiceImpl.java       # Implementación de solo lectura
```

**Structure Decision**: Se mantiene la estructura real del backend (paquete raíz único
`footballmarket`, separación obligatoria entre interfaz e implementación de Service, Controller sin
lógica, Mapper sin negocio). No se crean módulos, paquetes raíz ni proyectos nuevos. La ruta se
expone en un Controller propio para no modificar `PlayerQuoteController` ni el contrato del ranking
por precio. Las consultas se añaden a los Repositories existentes de las entidades leídas. No hay
migración ni cambio en `docs/backend/data-model.md`, porque la feature no altera el esquema.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones: la sección queda vacía. Las decisiones de diseño con alternativas evaluadas están
registradas en [`research.md`](research.md) (superficie HTTP, última ejecución, población elegible,
selección de valuación, orden, forma del contrato, consistencia transaccional, errores, ausencia de
migración, separación respecto del ranking por precio y estrategia de tests).

## Evidencia de fase

| Elemento | Valor observado |
| --- | --- |
| Fecha de ejecución | 2026-10-23 |
| Comando | `.specify/scripts/bash/setup-plan.sh --json` desde la raíz del repositorio |
| FEATURE_SPEC | `specs/018-consultar-rankings-de-jugadores/spec.md` |
| IMPL_PLAN | `specs/018-consultar-rankings-de-jugadores/plan.md` |
| SPECS_DIR | `specs/018-consultar-rankings-de-jugadores` |
| BRANCH | `018-consultar-rankings-de-jugadores` (rama Git real: `feat/018-consultar-rankings-de-jugadores`) |
| Artefactos generados | `plan.md`, `research.md`, `data-model.md`, `contracts/score-ranking-http.md`, `quickstart.md` |
| Hooks | `.specify/extensions.yml` no existe; no se ejecutaron hooks de extensión |
| Fuera de esta fase | Evaluación de checklists, generación de `tasks.md`, Analyze, implementación, tests y builds |

El estado de `spec.md` no se modifica en esta fase y no se declara sincronización con el sistema de
seguimiento operativo, que permanece no configurado conforme a `docs/development/sdd-workflow.md` §12.

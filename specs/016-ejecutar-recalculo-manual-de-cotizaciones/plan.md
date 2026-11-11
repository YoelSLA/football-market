# Implementation Plan: Cotización de tokens de jugadores con historial trazable

## Plan vigente de integración — 2026-11-11

Base compartida de diseño, esquema, verificación y reparto de tareas:
[`integration-baseline.md`](../023-scheduler-quote-recalculation/integration-baseline.md).
La rama destino es `feat/023-daily-quote-scheduling`; las referencias siguientes a la rama y PR
de SPEC-016 describen su origen, no una publicación del merge. El análisis anterior no evalúa
esta integración. No se modifica código en esta alineación.

## Plan del delta de origen — 2026-11-10, actualizado por impacto

**Rama real**: `docs/016-daily-quote-recalculation-spec`. **PR**: [#86](https://github.com/YoelSLA/football-market/pull/86), base `dev`, relacionado con #39 (verificado por lectura). El scope inglés de la rama difiere del directorio histórico; no se renombra la spec. Esta revisión sustituye por impacto el diseño histórico de abajo, no acredita implementación ni autorizaciones.

### Resumen y contexto técnico

Conservar una fila por jugador/día UTC, actualizada por el último cálculo válido aplicando el último score persistido aunque sea antiguo. Admitir manuales ilimitadas y una automática diaria según SPEC-023. Separar fecha de vigencia diaria y fecha de valuación auditada. Ranking por precio en `/api/rankings/quote`, con posiciones globales, filtros backend y página acotada. Agenda diaria corresponde a SPEC-023; ranking por score a SPEC-018 y UI a SPEC-022. No se planifica aquí su implementación completa.

Reutilizar Java 21, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL, JWT, Flyway y Springdoc existentes; sin dependencias nuevas. Las versiones efectivas siguen en Gradle. No se crean tablas de precio actual, capas de caché ni SLAs nuevos. El código real está en `backend/src/main/java/footballmarket/features/quote/{application,domain,infraestructure,presentation}`, no en los paquetes planos del antecedente. Tests en el árbol equivalente de `backend/src/test/java/footballmarket/features/quote/`.

### Constitution Check previo a investigación

| Garantía | Evaluación |
| --- | --- |
| I y VI: simplicidad y mínimo alcance | PASS: modificar núcleo y consultas existentes; sin scheduler, UI ni funcionalidades adicionales |
| II: proporcionalidad | PASS: unicidad concurrente exigida por aceptación; recuperación automática y compensaciones no añadidas |
| III: seguridad e integridad | PASS de diseño: JWT, binding, filtros escapados, unicidad SQL y atomicidad por jugador |
| IV: contratos | PASS de diseño: incompatibilidades explícitas y consumidores identificados; adopción conjunta pendiente |
| V: trazabilidad | PASS de diseño: ejecución identificable y parámetros auditables; TD-024 sigue abierta como limitación conocida |
| Verificación y autorizaciones | PASS: solo documentación; sin tests/build ni autorización de IMPLEMENT |

### Diseño y componentes afectados

1. `QuoteRecalculationServiceImpl`: capturar un solo `Instant startedAt`; derivar `quoteDate` en UTC. Cada corrida conserva ID y resumen independientes; las manuales no se rechazan por fecha. Conservar reserva confirmada automática e identificar únicamente el conflicto del índice parcial `uq_quote_executions_automatic_date` para `ExecutionAlreadyExistsException`; no traducir otros errores SQL a ese motivo.
2. `QuotePlayerPublisher`: conservar `REQUIRES_NEW` y bloqueo de jugador compartido con inicialización. Omitir solo si `effectiveDate > quoteDate`. Con fecha igual, recalcular sobre precio vigente y actualizar la fila existente, incluidos datos de trazabilidad y `execution_id`, sin cambiar ID/jugador/fecha. Con fecha anterior insertar la nueva fila diaria. Mantener constraint único `(player_id,effective_date)` y no recalcular scores. El bloqueo por jugador no es exclusión global de corridas. Inicialización y verificación de mercado deben seguir funcionando aunque una fila INITIAL haya cambiado de origen: derivar existencia/primera fecha del historial, no solo del origen INITIAL.
3. `QuoteExecution` y DTO: `executionId` contractual; `notRepricedPlayers` cuenta omisiones por fecha vigente posterior. Reemplazos cuentan como recalculados o arrastrados, no omitidos; contadores disjuntos. Mantener `origin`, `result`, `failureReason`, tiempos y parámetros; contexto previo confirmado y cierre independiente. Lista finalizada ordenada por `startedAt DESC,id DESC`.
4. `PlayerQuoteRepository` y `QuoteQueryService`: consulta SQL de última fila por jugador elegible, posición con `row_number()` por `price DESC,player_id ASC`, filtros después de numerar y antes de contar/paginar. No materializar el ranking completo en Java para paginarlo. Leer contenido, total y opciones con snapshot `REPEATABLE_READ`. Mapper solo transforma Model a DTO, sin asignar posiciones.
5. Frontera de ranking bajo `/api/rankings/quote`: Request DTO record, Mapper estático y Service con interfaz. Reutilizar defaults y límites del catálogo. Entregar identificación por joins/proyecciones de conjunto, sin N+1 ni descargar todo el catálogo. Top 3 mediante el mismo endpoint sin filtros con `page=0&size=3`, sin ruta adicional.
6. Esquema: ambas migraciones de creación ya están presentes en la referencia local `origin/dev` (verificado con `git ls-tree`); planificar nueva migración de cambio de constraints/FK/nulabilidad, sin editar las integradas. No preservar datos descartables ni ejecutar reinicializaciones por iniciativa del agente. Ver detalle en `data-model.md`.

### Coordinación contractual y orden técnico

El núcleo de cotización y el esquema deben preceder a sus consumidores. Cambiar cardinalidad diaria obliga a reemplazar en SPEC-018 el selector `findTopByOrderByQuoteDateDesc`: usar orden total `quoteDate DESC,startedAt DESC,id DESC` para ejecuciones finalizadas. La semántica funcional de estrategia activa es propiedad de SPEC-018 y requiere su preparación vigente, no se decide silenciosamente aquí. La ruta antigua y respuesta completa se retiran solo en el mismo cambio que adapta consumidores de SPEC-022 y OpenAPI/tests; no se conserva un alias de descarga completa. No se implementa ni se modifica esas specs desde esta fase.

### Verificación prevista y cierre del diseño

En IMPLEMENT autorizado: adaptar `QuoteRecalculationServiceTest`, `QuoteQueryServiceTest`, `QuoteExecutionPersistenceTest`, `PlayerQuoteMigrationTest`, `QuoteControllerTest`, `PlayerQuoteControllerTest` y `QuoteAutomaticRecalculationFailureTest`. Model para fórmula e invariantes; Repository real para queries/constraints; Service con DB real y fixtures solo en Arrange; Controller slice para todos los casos HTTP. Clock controlado y concurrencia con barreras, sin esperas arbitrarias. El usuario ejecuta las pruebas; no se crean tests frontend. Regresiones: dos días/mismo score, varias corridas concurrentes, inicialización del mismo día, ausencia de score, fallo individual reintentable, página/filtros y posiciones globales. TD-024 no se da por resuelta por permitir reintentos.

**Constitution Check posterior**: PASS de diseño para las garantías anteriores; sin excepciones normativas ni dependencias nuevas. El resultado no es ANALYZE ni aprobación humana. Artefactos vigentes: encabezados de revisión de `research.md`, `data-model.md`, `contracts/quotes-http.md`, `contracts/quote-source.md`, `quickstart.md`. No hay incógnitas técnicas `NEEDS CLARIFICATION` del delta; preparación de SPEC-018/022/023 y revisión de checklists/Tasks/ANALYZE siguen pendientes. No se actualizan Tasks ni checklists en PLAN.

---

## Antecedente de planificación — no ejecutar reglas sustituidas

**Branch**: `feat/016-ejecutar-recalculo-manual-de-cotizaciones` | **Date**: 2026-10-20 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `/specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md`

**Note**: Este documento es el resultado de la fase de planificación. Los artefactos de diseño que
lo acompañan son [`research.md`](research.md) (Phase 0), [`data-model.md`](data-model.md),
[`contracts/quotes-http.md`](contracts/quotes-http.md),
[`contracts/quote-source.md`](contracts/quote-source.md) y [`quickstart.md`](quickstart.md).

## Summary

Se implementa el subsistema de cotización de tokens de jugadores: la inicialización del mercado con
el ancla de 1 crédito, el recálculo manual con re-ajuste compuesto sobre el último precio vigente,
la persistencia del historial completo con trazabilidad de la estrategia que produjo cada
movimiento, las consultas de historial, cotización vigente, cotización por fecha y ranking por
precio, y la sustitución de la fuente de cotización simulada por la fuente real que consumen la
compra y la publicación de venta.

El diseño se apoya en una única tabla `player_quotes` que es a la vez historial y estado vigente
(la vigente es la fila de fecha de vigencia máxima), una tabla `quote_executions` con una única
ejecución por fecha de cotización para el resumen por corrida, y el mantenimiento del puerto
`QuoteSource` como punto de desacople entre las cotizaciones y el mercado.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.1, Gradle 9.6.0 con Kotlin DSL)

**Primary Dependencies**: Spring Web MVC, Spring Data JPA (Hibernate), Spring Security (OAuth2
Resource Server, JWT), Spring Validation, Flyway, SpringDoc OpenAPI, PostgreSQL 18 (pgJDBC).
Sin dependencias nuevas: la feature reutiliza el catálogo de estrategias y las valuaciones de
SPEC-013, el puerto `QuoteSource` de SPEC-010 y la capa de observabilidad vigente (Actuator).

**Storage**: PostgreSQL. Dos tablas nuevas (`player_quotes`, `quote_executions`) mediante una
única migración Flyway generada con `./gradlew createMigration`.

**Testing**: JUnit Jupiter + Mockito + AssertJ + MockMvc + Testcontainers, con las categorías
`unit`, `lightweight`, `integration`, `e2e` ya definidas en el backend. Los tests los crea y
ejecuta el usuario; el agente no los ejecuta.

**Target Platform**: Servidor Linux/Windows con JVM 21 y PostgreSQL accesible; sin dependencias de
plataforma específicas.

**Project Type**: backend web-service (Spring Boot), dentro de un monorepo con frontend Angular
consumidor de la API.

**Performance Goals**: No se fijan objetivos propios (FR-065). Las consultas de cotización se resuelven
por acceso directo al repositorio de cotizaciones, sin recalcular precios y sin caché nueva.

**Constraints**: Importes en centésimas de crédito (100 centésimas = 1 crédito); precios
estrictamente mayores que cero; redondeo half-up al entero de centésimas; multiplicador persistido
a seis decimales; `k` en el rango cerrado [0,10; 1,00]; tope de variación en el rango cerrado
(0; 1] (fracción); unicidad de cotización por jugador, fecha de vigencia y origen; una sola fila
inicial por jugador cotizable; una sola ejecución de cotización por fecha de cotización; ninguna
operación de mercado ni ejecución de cotización sin mercado inicializado.

**Scale/Scope**: Una feature de backend. Añade 2 tablas, 2 repositorios, 2 modelos de Model
(`PlayerQuote` y `QuoteExecution`) más 2 enums (`QuoteOrigin` nuevo y el `ValuationStrategy`
reutilizado de SPEC-013), 3 Services (interfaz + implementación), componentes internos de
ejecución, 2 Controllers con 7 rutas en total, 2 Mappers, DTOs de request/response, 8 códigos de
error nuevos, la fuente real del puerto `QuoteSource` y la retirada del simulador. Sin cambios en
frontend, sin job programado.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / garantía | Evaluación | Resultado |
| --- | --- | --- |
| §I Simplicidad y calidad | Sin dependencias nuevas; el historial es una sola tabla; el mercado no cambia de contrato; la fuente simulada se retira por completo en lugar de coexistir con la real | PASS |
| §II Proporcionalidad del alcance | Se implementa lo especificado: cálculo, persistencia, inicialización, consultas, ranking e integración con el mercado. No se añade periodicity, interfaz de usuario, obsolescencia, regresión a la media, comisiones ni ampliación de estrategias | PASS |
| §III Seguridad e integridad | Sin secretos ni credenciales; JWT obligatorio en todas las rutas; entradas validadas con Jakarta Validation y rangos controlados en el Service; sin concatenación de entrada externa | PASS |
| §IV Coherencia de contratos | La compra y la publicación de venta conservan firma, respuesta y códigos; se añade un único rechazo nuevo (`MARKET_NOT_INITIALIZED`); se corrige la descripción de consumidores del contrato `quote-source.md` de SPEC-010 en el mismo cambio; OpenAPI obligatorio en la misma tarea | PASS |
| §V Trazabilidad y fuente de verdad | Cada fila registra estrategia, versión, score, precio anterior, multiplicador, origen e instante de cálculo; cada corrida tiene un resumen persistido; sin estado duplicado del mercado | PASS |
| Gestión de datos en desarrollo | Sin migraciones de compatibilidad ni columnas preservadoras; una única migración nueva | PASS |
| Idioma | Código en inglés, documentación y mensajes de validación en español | PASS |
| Verificación y testing | El agente no ejecutará tests; el control ejecutable admitido es `./gradlew build -x test` desde `backend/` | PASS |
| Autorizaciones humanas | El plan no implementa código ni habilita Implement; la fase siguiente es la evaluación de checklists | PASS |

**Resultado del gate previo a Phase 0**: PASS, sin violaciones que exijan Complexity Tracking.

### Re-evaluación tras el diseño (Phase 1)

| Principio / garantía | Evaluación tras el diseño | Resultado |
| --- | --- | --- |
| §I Simplicidad | El diseño confirma una sola tabla de cotizaciones, una de ejecuciones y ninguna capa adicional; no se introduce caché ni abstracción nueva; el puerto `QuoteSource` se mantiene sin ampliar | PASS |
| §II Proporcionalidad | Cada caso límite de la `spec.md` (valuación antigua, fila arrastrada, score neutro, fallo individual, segunda corrida de una misma fecha) tiene una regla explícita en [`research.md`](research.md) §5, §7 y §13, sin requisitos añadidos | PASS |
| §III Seguridad | El rango de `k`, el tope y el piso se validan antes de ejecutar; el detalle de fallo individual no se expone; las operaciones de mercado se rechazan antes de mover recursos | PASS |
| §IV Coherencia de contratos | Se documentan los 8 códigos de error nuevos y se preserva `QUOTE_UNAVAILABLE`; la corrección del contrato de SPEC-010 queda registrada como obligation del mismo cambio ([research.md §10](research.md), [contracts/quote-source.md §4](contracts/quote-source.md)) | PASS |
| §V Trazabilidad | Cada fila y cada corrida son reconstruibles; el estado de inicialización del mercado se deriva de las propias filas, sin fuente paralela | PASS |
| Verificación y testing | El agente no ejecutará tests ni builds de test; la guía de validación es manual y la ejecuta el usuario | PASS |

**Resultado del gate posterior a Phase 1**: PASS. Sin violaciones justificables: la sección
Complexity Tracking queda vacía.

## Project Structure

### Documentation (this feature)

```text
specs/016-ejecutar-recalculo-manual-de-cotizaciones/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── quotes-http.md
│   └── quote-source.md
├── checklists/          # Fase siguiente: checklists evaluados
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── config/
│   └── QuoteProperties.java              # Valores por defecto del control de variabilidad
├── controllers/
│   ├── QuoteController.java               # Rutas /api/quotes/*
│   ├── PlayerQuoteController.java         # Rutas /api/players/{id}/quotes* y /api/players/ranking
│   ├── dtos/requests/                    # InitializeQuotesRequestDTO, RecalculateQuotesRequestDTO
│   ├── dtos/responses/                   # QuoteResponseDTO, QuoteHistoryResponseDTO, RankingResponseDTO, ...
│   └── mappers/                          # QuoteMapper, QuoteExecutionMapper
├── models/
│   ├── PlayerQuote.java                  # Fila del historial de cotizaciones
│   ├── QuoteExecution.java                # Corrida persistida
│   └── enums/
│       ├── QuoteOrigin.java              # INITIAL, RECOMPUTED, CARRIED_FORWARD
│       └── (reutilizado) ValuationStrategy.java  # Catálogo cerrado de estrategias (SPEC-013)
├── repositories/
│   ├── PlayerQuoteRepository.java
│   └── QuoteExecutionRepository.java
├── services/
│   ├── QuoteMarketInitializationService.java  (+ impl)
│   ├── QuoteRecalculationService.java        (+ impl)
│   ├── QuoteQueryService.java                (+ impl)
│   └── internal/                             # Publicación por jugador, selección de población y score
└── integrations/
    ├── QuoteSource.java                  # Puerto vigente, sin cambios de firma
    └── RepositoryQuoteSource.java        # Implementación real sobre player_quotes

backend/src/main/resources/db/migration/
└── V<timestamp>__create_player_quotes.sql   # player_quotes + quote_executions
```

**Structure Decision**: Se mantiene la estructura real del backend (paquete raíz único
`footballmarket`, separación obligatoria entre interfaz de Service e implementación, Controllers sin
lógica, Mappers sin negocio). Los artefactos nuevos se ubican en las carpetas y paquetes ya
existentes; no se crean módulos, paquetes raíz ni proyectos nuevos. La decisión de mantener el puerto
`QuoteSource` como `Integration` con implementación real sobre el repositorio está justificada en
[`research.md` §9](research.md). El retiro de `SimulatedQuoteSource` y de
`QuoteSimulationConfiguration` afecta a `integrations/` y `config/` y a las propiedades
`market.quote.simulator.*` de `application.yml`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones: la sección queda vacía. Las decisiones de diseño con alternativas evaluadas
están registradas en [`research.md`](research.md) (persistencia del historial, forma de las filas,
unicidad de ejecución por fecha, población cotizable, resumen de ejecución, inicialización, fuente real,
autorización, atomicidad por jugador, superficie HTTP y códigos de error).

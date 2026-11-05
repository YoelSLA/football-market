# Research: Recálculo automático programado de cotizaciones (SPEC-023)

**Feature**: `023-scheduler-quote-recalculation`
**Spec**: [`spec.md`](spec.md)

Este documento resuelve las decisiones técnicas de la fase 0. No implementa código ni habilita
IMPLEMENT: las fases siguientes son Checklists, Tasks y Analyze.

---

## 0. Estado verificado del código (2026-11-03)

Leído antes de decidir; estas son las capacidades existentes que la feature reutiliza.

| Elemento | Estado observado |
| --- | --- |
| `QuoteRecalculationServiceImpl.recalculate(...)` | Valida estrategia, rechaza mercado no inicializado, aplica los valores por defecto de `QuoteProperties`, reserva la ejecución con `saveAndFlush` (la unicidad por fecha se impone en base), recorre la población con atomicidad por jugador y completa el resumen |
| `quote_executions` | Una fila por `quote_date` (`uq_quote_executions_date`); guarda estrategia, versión, parámetros, contadores e instantes. No distingue origen |
| `QuoteProperties` | `market.quote.{adjustment-factor, variation-cap, min-price-floor, initial-anchor}`, con valores por defecto y validación Jakarta |
| `Clock` | Bean `marketClock` = `Clock.systemUTC()`; la fecha de cotización es `LocalDate.now(marketClock)` |
| Consulta de ejecuciones | `GET /api/quotes/executions?date=`, servida por el mismo `QuoteRecalculationService` y su repositorio |
| Programación | No existe ningún `@Scheduled`, `@EnableScheduling`, `TaskScheduler` ni configuración `spring.task.*` |
| Exclusión de solapamiento | **No existe ningún lock ni control de concurrencia propio del recálculo.** La única garantía es la restricción `uq_quote_executions_date`, que rechaza la segunda inserción de la misma fecha |
| Atomicidad de la publicación por jugador | `QuotePlayerPublisher.publish(...)` es `@Transactional(propagation = REQUIRES_NEW)`: cada jugador confirma o revierte por separado, y el bucle captura y cuenta los fallos individuales |
| Registro operativo | Logs estructurados SLF4J ya usados por la ejecución (`quote_execution_started`, `quote_execution_completed`, `quote_player_recalculation_failed`) |
| Dependencias | Spring Boot 4.1.1 administra `spring-context` y `spring-boot-autoconfigure`, que incluyen el soporte de scheduling. No hace falta una dependencia nueva |

---

## 1. Mecanismo de programación (FR-001, FR-011)

**Decisión**: usar el soporte de scheduling de Spring (`@EnableScheduling` + `@Scheduled` con
expresión cron resuelta por propiedad y `zone = "UTC"`).

**Rationale**:
- Ya está en el classpath por Spring Boot; no incorpora tecnología ni dependencia.
- La expresión cron es configurable por entorno sin recompilar y se evalúa en UTC fija, tal como
  exige la clarificación.
- Cualquier otro proceso batch (#41) agrega su propio método `@Scheduled` sobre el mismo scheduler
  habilitado una sola vez; no hay que duplicar la infraestructura (FR-011).

**Alternativas consideradas**:
- **Quartz**: aporta persistencia de jobs, clustering y triggers complejos que esta feature no
  necesita; suma dependencia e infraestructura.
- **Cron del sistema operativo o servicio externo**: saca la programación de la aplicación, no es
  observable por ella, no se reutiliza desde el código y complica la validación local.
- **Bucle con hilo propio**: reimplementa lo que la plataforma ya resuelve y agrega riesgos de
  ciclo de vida.

---

## 2. Infraestructura reutilizable para otros procesos batch (FR-011)

**Decisión**: habilitar scheduling una sola vez en `FootballMarketApplication`, apoyarse en el
`TaskScheduler` que autoconfigura Spring Boot y fijar `spring.task.scheduling.pool.size: 1` y un
prefijo de hilo propio en `application.yml`. El disparador de esta feature vive en su módulo
(`features/quote/infraestructure/scheduling/`). Un proceso batch futuro agrega su `@Scheduled` en
el módulo de su feature, sin nueva infraestructura ni registro central.

**Rationale**: satisface FR-011 con la configuración de plataforma, sin introducir una abstracción
propia (`BatchJob`, registro de jobs, cola) para un segundo job que todavía no existe. El pool de
un hilo serializa los procesos batch y refuerza la no superposición.

**Alternativas consideradas**:
- Interfaz `BatchJob` + registro + despachador: abstracción sin segundo consumidor real;
  contradice el principio de mínimo alcance.
- Un `ThreadPoolTaskScheduler` propio: repite la autoconfiguración de Boot sin necesidad.

---

## 3. Unicidad y no superposición (FR-006)

**Decisión**: conservar `uq_quote_executions_date` y el rechazo existente de SPEC-016 para una
segunda ejecución de la misma fecha. Por decisión humana se retiró la exigencia adicional de
exclusión global: no se añade un lock de instancia ni se garantiza exclusión entre fechas distintas.
El scheduler usa el hilo de programación configurado; eso no implica una garantía global para la vía
manual. Un fallo sin ejecución registrada deja disponible el reintento conforme al servicio existente.

---

## 4. Origen de la configuración de la ejecución automática (FR-002)

**Decisión**: extender `QuoteProperties` (prefijo `market.quote`) con:
- `strategy`: estrategia de valuación aplicada por la ejecución automática, por defecto
  `POSITIONAL_PERFORMANCE`.
- `schedule.cron`: expresión cron por defecto `0 0 0 * * MON`.
- `schedule.enabled`: habilitación del disparador, por defecto `true`.

La ejecución automática toma estrategia, factor de ajuste, tope y piso de esa misma configuración.
La ejecución manual conserva sus parámetros de request y sigue permitiendo elegir estrategia.

**Rationale**: la clarificación exige que la estrategia se lea de una variable de configuración del
mercado y que el resto de parámetros provenga de la misma configuración; `market.quote` ya es ese
origen. Un único `@ConfigurationProperties` bajo el mismo prefijo evita dos clases que describan el
mismo bloque de configuración. El valor inicial `POSITIONAL_PERFORMANCE` es un valor configurado y
modificable, no una regla funcional nueva: la estrategia sigue siendo seleccionable.

**Interpretación registrada**: «variable de configuración del mercado» se materializa como
propiedad de configuración (`market.quote.strategy`, ajustable por entorno) y no como un ajuste
persistido en base de datos. La spec no pide una pantalla ni una operación de administración, y
FR-005 prohíbe introducir mecanismos nuevos; una propiedad satisface que el administrador pueda
cambiarla sin tocar la lógica de negocio ni las fórmulas.

**Sobre `schedule.enabled`**: no es un requisito funcional, pero se incluye porque la cadencia
configurable necesita un interruptor determinista: los contextos `@SpringBootTest` arrancan con el
scheduler activo y las pruebas que validen el disparo con una expresión de segundos necesitan poder
dejarlo inerte en el resto de la suite (`docs/backend/testing.md` §1 exige determinismo). El valor
por defecto es `true`; no altera ningún FR.

**Alternativas consideradas**:
- Nueva clase con prefijo `market.quote.recalculation`: duplica la superficie de configuración del
  mismo bloque.
- Reutilizar `ValuationProperties` (`valuation.*`): mezcla parámetros de valuación con parámetros
  de cotización.
- Tomar la estrategia de la última ejecución manual: no es una variable de configuración y haría
  que el comportamiento dependiera del historial.

---

## 5. Persistencia del origen (FR-004)

**Decisión**: agregar la columna `origin` a `quote_executions`
(`MANUAL` | `AUTOMATIC`), con `NOT NULL`, valor de relleno `MANUAL` para las filas existentes,
`CHECK` del catálogo cerrado y sin valor por defecto permanente. El `Model` la expone con el enum
`QuoteExecutionOrigin`, ubicado junto a los demás enums del dominio de cotización.

**Rationale**: el origen automático o manual no es deducible del resto del registro y FR-004 exige
que quede registrado. `quote_executions` es el registro persistido existente que la spec manda
ampliar (no se crea otro). El enum **no** puede llamarse `QuoteOrigin`: ese nombre ya está tomado
por el origen de una fila de `player_quotes` (`INITIAL`, `RECOMPUTED`, `CARRIED_FORWARD`) y describe
otro concepto.

**Alternativas consideradas**:
- Inferirlo del contexto de la petición o del hilo: no queda persistido ni es consultable después.
- Tabla separada de ejecuciones automáticas: duplica el registro y rompe la consulta única por
  fecha.
- Registrarlo solo en el log: no cumple que el registro persistido lo contenga.

---

## 6. Resultado y motivo de fallo (FR-004, FR-010, FR-013)

**Decisión**: el resultado y el motivo se derivan en el `Model` a partir de `failed_players`, no se
duplican en columnas:
- `failed_players = 0` → `result = SUCCESS`, `failureReason = null`.
- `failed_players > 0` → `result = FAILED`, `failureReason = INDIVIDUAL_PLAYER_FAILURES`.

Ambos se exponen en el DTO de consulta. Una corrida que falla por completo no persiste ejecución
(FR-013), de modo que su motivo vive únicamente en el registro operativo.

**Rationale**: la información necesaria ya está persistida en los contadores; una columna adicional
sería estado derivado duplicado. El catálogo cerrado de motivos evita exponer trazas o mensajes
técnicos (FR-010) y no inventa categorías de fallo que la spec no define.

**Alternativas consideradas**:
- Columnas `result` y `failure_reason`: duplican un valor derivable y abren la posibilidad de
  inconsistencia entre contadores y columna.
- Texto libre con la causa técnica: filtraría detalles internos (FR-010) y no sería estable como
  contrato.

---

## 7. Fallo completo de la corrida (FR-013)

**Decisión**: reutilizar el límite transaccional existente. `recalculate(...)` reserva la fila con
`saveAndFlush` y, si la corrida entera falla con una excepción no controlada por jugador, la
transacción se revierte y no queda ejecución para la fecha. El scheduler captura la excepción y
registra el evento operativo con su causa; la fecha queda libre para el reintento manual por el
comportamiento existente, no por una operación de recuperación del scheduler.

**Rationale**: ya es el comportamiento del servicio; no hace falta un mecanismo nuevo ni una
compensación explícita. El fallo individual por jugador sigue capturándose por dentro y no revierte
la corrida (SPEC-016 FR-062).

**Alternativas consideradas**:
- Persistir un nuevo estado terminal para fallos completos modificaría el comportamiento existente y bloquearía la fecha; no se requiere en esta feature.
- Reintentos automáticos: no están en la spec y podrían ocultar fallos.

---

## 8. Disparo duplicado o solapado y mercado no inicializado (FR-006, FR-012)

**Decisión**: el disparador automático es un componente delgado que invoca el caso de uso y traduce
las excepciones controladas a eventos del registro operativo, sin responder HTTP ni propagar el
error:
- `ExecutionAlreadyExistsException` → omitido, motivo `EXECUTION_ALREADY_EXISTS` (fecha ya
  registrada o conflicto de unicidad existente).
- `MarketNotInitializedException` → rechazo controlado, motivo `MARKET_NOT_INITIALIZED`.
- Cualquier `RuntimeException` no controlada → fallo completo, con la clase de la causa.

**Rationale**: la spec exige omitir el disparo duplicado y registrar el intento, no devolver un
error. Se reutilizan las validaciones y el orden de precedencia ya definidos por SPEC-016 (FR-066)
sin duplicar reglas: la estrategia del catálogo se valida primero, luego la inicialización del
mercado, después los parámetros y por último la reserva de la fecha. La vía automática no recibe
parámetros de request, así que sus valores provienen de configuración ya validada al arrancar y el
único rechazo de parámetros posible no depende del disparo. Los motivos son códigos cerrados y
estables.

**Alternativas consideradas**:
- Duplicar en el scheduler las comprobaciones de fecha y de mercado: repetiría la lógica del
  servicio y podría divergir.
- Silenciar el disparo sin registro: incumple FR-006 y FR-012.

---

## 9. Zona horaria y fecha de cotización (FR-001)

**Decisión**: la expresión cron se evalúa con `zone = "UTC"` y la fecha de cotización continúa
derivándose de `LocalDate.now(marketClock)` con el `Clock` UTC existente.

**Rationale**: la clarificación fija UTC y elimina el horario estacional; no se introduce una zona
configurable porque no está pedida.

**Alternativas consideradas**: cron en la zona del servidor o configurar la zona: agrega un
parámetro no solicitado y riesgo de desplazamiento.

---

## 10. Contrato HTTP de consulta (FR-004, FR-005, FR-010)

**Decisión**: extender de forma aditiva `QuoteExecutionResponseDTO` (y su Mapper) con `origin`,
`result` y `failureReason`. No se crea ni se modifica ninguna ruta: la consulta por fecha sigue
siendo `GET /api/quotes/executions?date=`. El `POST /api/quotes/recalculate` conserva su request y
su response gana los mismos campos derivados.

**Rationale**: FR-005 prohíbe introducir una consulta histórica nueva; el mecanismo existente basta.
Los campos nuevos son aditivos y no rompen consumidores. OpenAPI se actualiza en el mismo cambio.

**Alternativas consideradas**:
- Endpoint nuevo de ejecuciones automáticas: lo prohíbe FR-005.
- Devolver solo el origen y dejar el resultado implícito en los contadores: menos claro para el
  contrato.

---

## 11. Migración de base de datos

**Decisión**: una única migración Flyway nueva
(`V<timestamp>__add_quote_execution_origin.sql`) que agrega `origin` con relleno `MANUAL`, retira el
valor por defecto y agrega el `CHECK` del catálogo. No se toca ninguna migración previa.

**Rationale**: el proyecto está en desarrollo y los datos son descartables, pero la migración debe
poder aplicarse sobre una base existente sin reescribir historial. No se implementan mecanismos de
compatibilidad retroactiva ni columnas preservadoras.

**Alternativas consideradas**:
- Recrear `quote_executions`: destructivo sin necesidad y borraría evidencia de ejecuciones.
- Valor por defecto permanente en la nueva columna: dejaría inserciones silenciosas sin origen.

---

## 12. Retención y consulta (FR-005)

**Decisión**: no se agrega purga ni retención configurable; los registros de ejecución se conservan
indefinidamente junto con el historial de cotizaciones. La consulta sigue siendo la existente por
fecha, con una fila como máximo.

**Rationale**: es lo que fija la clarificación y no requiere trabajo adicional. Introducir una purga
sería alcance no solicitado.

---

## 13. Observabilidad

**Decisión**: reutilizar SLF4J y los logs estructurados existentes. Se agregan eventos acotados:
`quote_automatic_recalculation_triggered`,
`quote_automatic_recalculation_skipped`, `quote_automatic_recalculation_rejected` y
`quote_automatic_recalculation_failed`, y el evento de inicio de la ejecución incorpora el origen.
No se agregan métricas, Actuator ni trazas nuevas.

**Rationale**: cumple el registro operativo de FR-006, FR-012 y FR-013 con la infraestructura
vigente, y respeta la prohibición de reimplementar observabilidad.

---

## 14. Pruebas

**Decisión**: las pruebas se diseñan por categoría conforme a
[`docs/backend/testing.md`](../../docs/backend/testing.md):

- `unit`: el `Model` (`QuoteExecution`, origen y resultado derivado) y el disparador con el Service
  mockeado.
- `lightweight`: binding y validación de `QuoteProperties` con `ApplicationContextRunner`, y el
  contrato del Controller con los campos nuevos.
- `integration`: el caso de uso con PostgreSQL real (ambas vías, origen persistido y
  exclusión por fecha), la migración y la consulta de ejecuciones por fecha.

Las ubicaciones siguen la estructura real de tests bajo
`backend/src/test/java/footballmarket/features/quote/` (`domain`, `application`, `infraestructure`,
`presentation`), con una `@Tag` por clase contenedora y `@Nested` obligatorio. El agente escribe y
modifica tests cuando la implementación lo requiera, pero no los ejecuta: los ejecuta el usuario. El
frontend no participa y no se crean tests de frontend.

**Rationale**: no hay jornada E2E nueva que justifique un E2E; el comportamiento se demuestra en las
capas específicas.

---

## 15. Punto de entrada de la ejecución automática y contrato del Service

**Decisión**: el Service existente expone un segundo método de caso de uso,
`QuoteExecution recalculateAutomatic()`, que resuelve estrategia y parámetros desde
`QuoteProperties` y comparte con `recalculate(...)` un núcleo privado que recibe los valores ya
resueltos y el origen. `recalculate(...)` conserva su firma y sigue enviando `MANUAL`, de modo que el
contrato HTTP de `POST /api/quotes/recalculate` no cambia de request. El disparador automático solo
depende de la interfaz `QuoteRecalculationService`.

**Rationale**: FR-002 fija un origen de configuración distinto para la ejecución automática, así que
son dos casos de uso con entradas distintas sobre el mismo recorrido; un método por entrada mantiene
la interfaz explícita y evita que el Controller o el disparador resuelvan reglas. La alternativa de
añadir un parámetro de origen a `recalculate(...)` obligaría a cambiar el contrato del caso de uso
manual sin necesidad.

**Alternativas consideradas**:
- Añadir `QuoteExecutionOrigin` como parámetro de `recalculate(...)`: cambia la firma existente y
  obliga a tocar el Controller y sus tests para expresar algo que la vía manual ya sabe de antemano.
- Un Service nuevo para la ejecución automática: duplicaría el recorrido o crearía una dependencia
  Service → Service no autorizada.
- Resolver la configuración en el disparador: saca lógica de aplicación del Service y duplica la
  lectura de `QuoteProperties`.

---

## 16. Integridad ante fallo completo (FR-013)

**Alcance actualizado (2026-11-04):** FR-013 solo exige al scheduler registrar el error del caso de uso. El comportamiento transaccional descrito a continuación es heredado y no constituye una garantía adicional del scheduler; tampoco se le asignan compensación, recuperación ni reintentos. Las afirmaciones históricas que atribuyen esas garantías a FR-013 quedan superadas por esta decisión.

**Decisión**: no se añade ningún mecanismo de compensación. La garantía se apoya en el
comportamiento existente: toda validación de entrada y de mercado ocurre antes de la primera
escritura; la fila de `quote_executions` se reserva y se confirma en la transacción externa del
Service, que se revierte ante una excepción no controlada; y la publicación de cada jugador es
`REQUIRES_NEW`, de modo que un fallo individual aislado nunca deja una fila a medio escribir.

**Rationale**: el rollback de la transacción externa existente elimina la reserva de la ejecución;
FR-013 solo exige al scheduler registrar el error. La atomicidad por jugador de SPEC-016 (FR-062) garantiza que
cada fila de cotización se confirme completa o no exista, y un fallo individual se cuenta sin
interrumpir la corrida; esta feature no modifica ese comportamiento (FR-003).

**Límite registrado**: si el proceso muere de forma abrupta (por ejemplo, fin del proceso durante la
corrida), las filas ya confirmadas por jugador permanecen. Ese escenario no está en los criterios de
aceptación y no se compensa: exigiría un mecanismo de recuperación que la spec no pide. La fecha
queda libre para el reintento manual conforme a FR-013.

**Responsabilidad fuera de alcance (decisión 2026-11-04)**: el mismo límite aplica cuando el guardado
del resumen falla después de que algunos jugadores ya confirmaron su cotización. La ejecución automática
**no** compensa ni revierte esas filas, porque esa responsabilidad excede su alcance y pertenece al
comportamiento heredado de SPEC-016 (publicación `REQUIRES_NEW` por jugador), registrado como TD-024.
FR-013 exige registrar el error; el comportamiento existente determina si la fecha queda disponible
para reintento manual y no exige al scheduler compensar cotizaciones confirmadas. Esta feature
no resuelve TD-024 ni la declara resuelta.

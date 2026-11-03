# Research: Cotización de tokens de jugadores con historial trazable

**Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`
**Spec**: [`spec.md`](spec.md)
**Plan**: [`plan.md`](plan.md)
**Fecha**: 2026-10-20

Este documento resuelve todas las decisiones marcadas como `NEEDS CLARIFICATION` en el
[Technical Context](plan.md#technical-context) y fija el diseño técnico de la fase de
planificación. Su autoridad es subordinada a la Constitución y a `spec.md`.

---

## 1. Persistencia del historial

**Decision**: Una única tabla `player_quotes` es el historial completo (FR-022). No existe tabla
de cotizaciones actuales: la cotización vigente de un jugador es su fila de fecha de vigencia
máxima y, a igualdad de fecha de vigencia, la fila `RECOMPUTED` (escrita después que la arrastrada),
y el ranking se deriva de la misma tabla con una consulta de última fila por jugador.

**Rationale**: La Constitución §V y la simplicidad (Principio I) desaconsejan duplicar el estado.
FR-030 exige consultas eficientes de historial, vigente, por fecha y ranking sin recalcular
precios, lo que se satisface con índices sobre `(player_id, effective_date DESC, calculated_at DESC)` y
una consulta `DISTINCT ON (player_id) ... ORDER BY effective_date DESC, calculated_at DESC`
reordenada por precio. El desempate por instante de cálculo es necesario porque FR-026 permite
que una fila `RECOMPUTED` y una `CARRIED_FORWARD` compartan fecha de vigencia.

**Alternatives considered**:
- *Tabla `player_current_quotes` sincronizada*: rechazada; duplica estado y exige reglas de
  sincronización adicionales que FR-022 excluye expresamente.
- *Series temporales por proveedor (TimescaleDB)*: rechazada; nueva dependencia no justificada por
  los requisitos actuales (Principio I).

## 2. Modelo de una fila de cotización

**Decision**: `player_quotes` con las columnas de FR-023: `player_id`, `effective_date`,
`price` (precio resultante en centésimas), `previous_price`, `multiplier`, `score`,
`strategy_name`, `strategy_version`, `source_valuation_date`, `origin` y `calculated_at`.
Restricción `UNIQUE (player_id, effective_date, origin)` (FR-026): a diferencia de la clave por
jugador y fecha de vigencia, permite que una fila `RECOMPUTED` y una `CARRIED_FORWARD` del mismo
jugador compartan fecha de vigencia cuando la valuación que origina la fila `RECOMPUTED` tiene esa
fecha, sin perder el registro histórico del período sin cambio.

`previous_price` es **nulo únicamente en las filas `INITIAL`**, por un `CHECK` que lo garantiza.
El resto de filas lo exige; su valor es el precio de la cotización vigente del jugador
inmediatamente antes de escribir esa fila (FR-028).

Los parámetros de la ejecución —factor de ajuste, tope de variación y piso mínimo— **no se duplican
en la fila**: se consultan en el resumen persistido de la ejecución que la produjo (FR-012,
FR-029, FR-061), de modo que exista una única fuente de verdad por parámetro.

**Rationale**: En la fila de ancla no existe cotización previa, y un precio "anterior" inventado
(por ejemplo, igual al propio precio) falsearía la auditoría de SC-003. La ausencia del dato es un
hecho verificable; la auditoria distingue "no había precio previo" de "el precio se repitió".

**Alternatives considered**:
- *`previous_price = price` en la fila inicial*: rechazada; haría indistinguible el ancla de una
  fila arrastrada con multiplicador 1, que deben ser auditables por separado (FR-021, SC-002).

## 3. Origen de la fila

**Decision**: Enumeración `QuoteOrigin` con el conjunto cerrado `INITIAL`, `RECOMPUTED` y
`CARRIED_FORWARD`, persistida como `VARCHAR` con `CHECK` en base de datos.

**Rationale**: FR-021 exige un conjunto cerrado y no ambiguo. La enumeración impide por tipo la
combinación inválida "fila `CARRIED_FORWARD` con score" o "fila `INITIAL` con estrategia".

**Alternatives considered**:
- *Booleano `carried_forward`*: rechazada; no puede representar `INITIAL` y no soporta la distinción
  exigida en la aclaración sobre score neutro (0,5) frente a ausencia de score.

## 4. Fórmula, variabilidad y redondeo

**Decision**: El cálculo se realiza con `BigDecimal`:
`multiplicadorBruto = 1 + k × (score − 0,5)`, acotado por el tope de variación al intervalo
`[1 − tope, 1 + tope]` (FR-012), y `precioNuevo = redondeoHalfUp(precioVigente × multiplicadorBruto)`
en centésimas enteras (FR-016), con piso mínimo configurable y estrictamente mayor que cero
(FR-013). El multiplicador se persiste redondeado a seis decimales.

El score es el valor final persistido por SPEC-013: `final_score` para `POSITIONAL_PERFORMANCE` y
`final_grl` para `GENERAL_ATTRIBUTES` (FR-006), leído del repositorio, nunca recalculado (FR-010).

**Rationale**: `BigDecimal` con `RoundingMode.HALF_UP` es el único modo de satisfacer simultáneamente
FR-016 (precisión interna completa, redondeo half-up al entero de centésimas, multiplicador a seis
decimales) sin errores de punto flotante binario en los límites del piso y del tope.

**Alternatives considered**:
- *`double`*: rechazada; produce drifts en la comparación con el piso y con la unicidad de precio.
- *Redondear antes de acotar por el tope*: rechazada; el tope debe acotar el multiplicador real.

## 5. Fecha de vigencia, escritura condicional y unicidad de ejecución por fecha

**Decision**: Cada ejecución usa como **fecha de cotización** la fecha UTC del día en que corre y
no es un parámetro del operador (FR-060). Para cada jugador cotizable:
1. Se localiza la **cotización vigente** (fila de `effective_date` máxima; a igualdad de fecha
   prevalece la fila `RECOMPUTED`, escrita después que la arrastrada).
2. Se localiza la **fecha de vigencia de referencia**: la `effective_date` de la última fila
   `INITIAL` o `RECOMPUTED` del jugador. Las filas `CARRIED_FORWARD` **no** alteran la referencia.
3. Si no hay valuación y la fecha de cotización es **estrictamente posterior** a la fecha de
   vigencia de referencia, la fila es `CARRIED_FORWARD`: `effective_date` = fecha de cotización,
   `price` = precio vigente, `multiplier` = 1, `score` = nulo, `source_valuation_date` = nulo,
   estrategia heredada de la cotización vigente (nula si esa procede de una fila `INITIAL`).
4. Si hay valuación y su `valuation_date` es **estrictamente posterior** a la fecha de vigencia de
   referencia, la fila es `RECOMPUTED`: `effective_date` = `valuation_date`,
   `source_valuation_date` = `valuation_date`, estrategia y versión de la ejecución. Si ese
   `valuation_date` coincide con el de una fila `CARRIED_FORWARD` previa del mismo jugador, ambas
   filas coexisten —lo permite `UNIQUE (player_id, effective_date, origin)`— y la `RECOMPUTED` pasa
   a ser la cotización vigente del jugador.
5. En cualquier otro caso (valuación anterior o igual a la fecha de vigencia de referencia) no se
   escribe fila y el jugador se cuenta como no repreciado (FR-027).

Cada fecha de cotización admite **una sola ejecución** (FR-060): una segunda ejecución en la misma
fecha se rechaza con `EXECUTION_ALREADY_EXISTS` antes de escribir nada y no altera el resumen ya
registrado. Dos ejecuciones concurrentes de la misma fecha se resuelven igual: solo una se resuelve
como ejecución y la otra se traduce al mismo error controlado, sin filas adicionales ni cambio del
resumen existente.

**Rationale**: Es la formulación literal de FR-027 y de la aclaración "no se reprecia a un jugador
cuyas métricas no cambiaron". Concentra la condición de escritura en un único punto. La prohibición de
una segunda corrida por fecha de cotización elimina la ambigüedad de los contadores en una reejecución
—que de otro modo podría reportar cero filas escritas sin distinguir «no había nada que repreciar» de
«ya estaba persistido»— y hace que el resumen de cada fecha sea único e interpretable.

La fecha de vigencia de referencia se define sobre las filas `INITIAL` y `RECOMPUTED`, y no sobre
la última fila de cualquier origen, por una razón concreta: la fila arrastrada ocupa la fecha de
cotización del día y, si contara para la referencia, ocultaría toda valuación con
`valuation_date` igual o anterior a esa fecha. En ese caso el jugador quedaría congelado en su
precio de forma permanente, porque cada ejecución posterior seguiría bloqueada por la misma regla.
Con la referencia sobre las filas `INITIAL` y `RECOMPUTED`, la fila arrastrada no avanza la
referencia, de modo que la valuación queda aplicable en la ejecución siguiente y el ajuste
posterior al cambio de estrategia (FR-009) llega a materializarse.

**Alternatives considered**:
- *Clave de idempotencia `(player_id, quote_date)`*: rechazada; la unicidad exigida es por fecha de
  vigencia, y la fila arrastrada comparte fecha de cotización con otras filas del mismo jugador en
  fechas de vigencia distintas, por lo que esa clave impediría filas válidas.
- *Columna `quote_date` en `player_quotes`*: rechazada; es redundante, porque la fecha de vigencia
  ya determina si una fila puede reescribirse.
- *Permitir una segunda corrida el mismo día y deducir la idempotencia de FR-027*: rechazada; permite
  dos resúmenes con contadores distintos para la misma fecha de cotización y abre la puerta a
  reintentar un fallo individual el mismo día, lo que la feature no contempla (FR-062).
- *Usar la última fila de cualquier origen como referencia de reprecio*: rechazada; bloquea
  permanentemente el reprecio de un jugador arrastrado y deja sin efecto el ajuste siguiente al
  cambio de estrategia (FR-009).
- *Reescribir la fila arrastrada cuando llega la valuación de esa misma fecha*: rechazada;
  destruiría el historial, que FR-026 y la trazabilidad exigen preservar.

## 6. Población cotizable

**Decision**: La población cotizable es el conjunto de `player_id` distintos cuyo último registro
en `player_statistics` tiene al menos una métrica informada (métricas persistidas por SPEC-012).
Un jugador sin métricas, o con un registro de métricas sin ninguna métrica informada, no recibe
cotización, no aparece en el ranking y no es operable (FR-001, FR-003).

**Rationale**: FR-003 exige derivar la población de las métricas persistidas y no de la cobertura de
las valuaciones. `player_statistics` es la fuente única de métricas del proyecto.

**Alternatives considered**:
- *Población derivada de las valuaciones*: rechazada; excluiría a los jugadores nunca valuados
  (FR-002) y rompería la salida por arrastre.

## 7. Resumen de ejecución persistido

**Decision**: Una segunda tabla `quote_executions` registra cada corrida: `quote_date`, estrategia
y versión, factor de ajuste, tope de variación, piso mínimo, contadores (`priced_players`,
`recomputed_rows`, `carried_forward_rows`, `not_repriced_players`, `failed_players`), `started_at` y
`finished_at`. Las ejecuciones son **append-only** y hay **como máximo una por fecha de cotización**
(`UNIQUE (quote_date)`): una segunda corrida el mismo día se rechaza antes de escribir (FR-060,
FR-066). La consulta por fecha de cotización devuelve esa ejecución, o una lista vacía si la fecha no
tiene ejecuciones. Una ejecución rechazada no persiste resumen.

**Rationale**: FR-061 exige que el resumen se persista y sea consultable por fecha de cotización.
Con una única ejecución por fecha de cotización, el resumen de una fecha es inequívoco: sus
contadores describen exactamente lo que esa corrida escribió y lo que no pudo repreciar, sin la
ambigüedad de dos corridas del mismo día con contadores distintos. La unicidad sobre `quote_date` no
destruye historial porque no hay una segunda corrida que sobrescriba el resumen anterior; el append-only
preserva la trazabilidad de cada corrida ejecutada (FR-019, FR-064) sin reescribir historia.

**Alternatives considered**:
- *`UNIQUE (quote_date)` con actualización de contadores*: rechazada; la segunda corrida se rechaza
  en lugar de sobrescribir el resumen anterior (FR-060, FR-066).
- *Permitir varias corridas por fecha de cotización*: rechazada; obligaría a definir cómo se
  distinguen los jugadores no repreciados por falta de valuación posterior de los ya persistidos en
  esa misma fecha, y haría ambiguos los contadores del resumen.
- *Solo devolver el resumen en la respuesta HTTP*: rechazada; contradice FR-061 y deja el resumen de
  la corrida sin soporte verificable fuera del log.

## 8. Inicialización del mercado

**Decision**: La inicialización crea exactamente una fila `INITIAL` por jugador cotizable, con
`effective_date` = fecha de mercado, `price` = 100 centésimas (ancla de 1 crédito, FR-014),
`multiplier` = 1, `score` = nulo, sin estrategia y `previous_price` = nulo. La **fecha de mercado** es
la fecha efectiva de la fila `INITIAL`; no existe tabla de estado del mercado, se deriva de las filas
`INITIAL` (FR-044).

Reglas de repetición:
- Sin filas `INITIAL` previas → se escribe el conjunto completo (idempotente: repetir sobre un
  mercado ya inicializado no duplica porque la unicidad es por jugador y fecha de vigencia).
- Con filas `INITIAL` previas y **misma** fecha de mercado → no se escribe nada y la operación
  devuelve el mismo conjunto (FR-032).
- Con filas `INITIAL` previas y **otra** fecha de mercado → se rechaza con error controlado
  `MARKET_ALREADY_INITIALIZED`, porque escribir una segunda fila inicial por jugador violaría
  FR-034; si la fecha solicitada es anterior a la fecha de mercado vigente, el rechazo es el de
  FR-033 (existen cotizaciones en fechas posteriores). FR-033 reformulado en la remediación del
  2026-10-21 explicita este criterio: cualquier fecha de mercado distinta de la vigente se rechaza.

**Rationale**: FR-032, FR-033 y FR-034 solo son simultáneamente satisfacibles con esta
clasificación. Derivar el estado de inicialización de las propias filas `INITIAL` evita una
configuración externa (FR-044) y un estado duplicado.

**Alternatives considered**:
- *Tabla `market_state` con la fecha de mercado*: rechazada; estado duplicado que puede divergir de
  las filas y que la Constitución §V discourage.
- *Permitir la re-inicialización en una fecha posterior*: rechazada; crearía una segunda fila
  `INITIAL` por jugador, prohibida por FR-034.

## 9. Fuente de cotización real y retirada del simulador

**Decision**: `QuoteSource` se mantiene como puerto `Integration` en `footballmarket.integrations`
con la misma firma `Optional<CurrentQuote> currentQuote(Long playerId)`. Se implementa contra el
repositorio canónico real de cotizaciones (`player_quotes`, última fila por jugador) y se elimina
`SimulatedQuoteSource` junto con `QuoteSimulationConfiguration` y las propiedades
`market.quote.simulator.*` (FR-042, SC-011).

La implementación real resuelve contra el repositorio, por lo que la dependencia efectiva
`Integration → Repository` se documenta en `docs/backend/architecture.md` §1.3 como parte de esta
feature, en línea con el carácter de adaptador de puerto que ya describe
`specs/010-buy-tokens/contracts/quote-source.md`.

El `CurrentQuote` se completa con la estrategia que produjo la cotización. La interfaz de puerto no
cambia; solo cambia su implementación, de modo que `BuyOrderServiceImpl` y `TokenListingServiceImpl`
no requieren modificación por este motivo (FR-042, FR-058).

**Rationale**: FR-042 exige retirar la fuente simulada y que la compra y la publicación de venta
consuman el repositorio canónico real "a través del puerto de lectura de cotización ya existente".
Mantener el puerto evita extender el contrato de mercado (FR-058) y conserva el comportamiento de
error ya definido (`QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `QuoteContractViolationException`).

**Alternatives considered**:
- *Eliminar el puerto y leer el repositorio desde los servicios de mercado*: rechazada; rompe el
  contrato documentado en SPEC-010 y amplía el alcance del cambio de mercado.
- *Mantener el simulador tras la fuente real*: rechazada; FR-042 exige retirarlo del sistema.

## 10. Corrección del contrato del puerto en SPEC-010

**Decision**: `specs/010-buy-tokens/contracts/quote-source.md` declara como consumidor único a
`BuyOrderServiceImpl`, lo cual está desactualizado: desde SPEC-011 también lo consume
`TokenListingServiceImpl`. La descripción se corrige **en el mismo cambio que introduce la fuente
real**, conforme al principio de coherencia de contratos (Constitution §IV) y a la propia
`spec.md` de esta feature.

**Rationale**: Un contrato normativo que describe mal a sus consumidores induce errores de
arquitectura. La corrección es documental y no altera comportamiento.

**Alternatives considered**:
- *Corregirlo en la fase de planificación*: rechazada; la corrección pertenece al cambio que
  introduce la fuente real, para que no exista un intervalo con la fuente real ya activa y el
  contrato todavía desactualizado.

## 11. Autorización de la ejecución

**Decision**: `POST /api/quotes/recalculate` exige un JWT válido (criterio de autorización vigente
para las operaciones administrativas del mercado: `POST /api/valuations/evaluate` y la
sincronización de estadísticas exigen `.authenticated()`), y no introduce roles nuevos.

**Rationale**: La `spec.md` fija en sus Supuestos que la ejecución reutiliza las reglas vigentes
para las operaciones administrativas "sin introducir roles nuevos", y FR-055/FR-059 prohíben
ampliar el catálogo. El proyecto no dispone de infraestructura de roles (`@PreAuthorize` ni
`hasRole`); el único rol persistido es `SUPERUSER`, con semántica de propietario inicial de los
tokens, no de operador de mercado.

**Alternatives considered**:
- *Exigir rol `SUPERUSER`*: rechazada; reinterpretaría el rol de propietario inicial como
  administrador del mercado, un cambio de modelo de roles fuera de alcance.
- *Endpoint sin autenticación*: rechazada; contradice FR-059 y FR-017 (el usuario final no percibe
  la estrategia).

## 12. Rechazo de operaciones de mercado sin mercado inicializado

**Decision**: La compra y la publicación de venta comprueban, **antes de leer la cotización y antes
de cualquier movimiento de recursos**, si existe al menos una fila de origen `INITIAL`. Si no
existe, se rechaza con un error propio, código `MARKET_NOT_INITIALIZED` (HTTP 409), distinguible de
`QUOTE_UNAVAILABLE` (FR-043). La misma comprobación, con el mismo código, precede a toda ejecución
de cotización (FR-063).

**Rationale**: La ausencia de mercado inicializado y la ausencia de cotización para un jugador son
hechos distintos: la primera invalida toda operación, la segunda solo a un jugador. El enunciado
§3.3 exige que el mercado exista antes de operar. El punto de comprobación se sitúa antes del
movimiento de recursos para que el rechazo no deje saldos, tenencias ni transacciones alterados
(FR-043).

**Alternatives considered**:
- *Degradar a `QUOTE_UNAVAILABLE`*: rechazada; la aclaración lo descarta explícitamente.
- *Comprobar en el arranque que el mercado esté inicializado*: rechazada; el mercado se inicializa
  en runtime y la comprobación pertenece a la operación.

## 13. Atomicidad por jugador y continuidad de la ejecución

**Decision**: La ejecución escribe cada jugador en su propia transacción (`REQUIRES_NEW`) mediante un
componente interno que publica una cotización por jugador. Un fallo individual se registra
internamente (jugador, etapa y causa), no interrumpe la ejecución y no deja filas parciales
(FR-062). El detalle interno no se expone en la respuesta: solo el contador de fallos. La
concurrencia de dos ejecuciones sobre la misma fecha de cotización se resuelve en el mismo punto:
la violación de `uq_quote_executions_date` se traduce al error controlado
`EXECUTION_ALREADY_EXISTS`, de modo que nunca se expone un error de persistencia (FR-060).

**Rationale**: La atomicidad es por jugador, no por ejecución: "la cotización de un jugador se
persiste de forma coherente o no se persiste, y el fallo de uno no interrumpe la ejecución". El
patrón ya existe en SPEC-013 (`ValuationEvaluationServiceImpl` publica cada par de resultados en
una transacción independiente), de modo que se reutiliza sin inventar un mecanismo nuevo.

**Alternatives considered**:
- *Una única transacción para toda la ejecución*: rechazada; un fallo individual revertiria la
  ejecución completa, contra FR-062.
- *Capturar el fallo y continuar dentro de la misma transacción*: rechazada; la transacción quedaría
  marcada para reversión.

## 14. Superficie HTTP

**Decision**: Rutas bajo el prefijo `/api/`, siguiendo las APIs obligatorias de `tp/enunciado.md` §4:

| Método | Ruta | Propósito |
| --- | --- | --- |
| POST | `/api/quotes/initialize` | Inicializa el mercado (US1) |
| POST | `/api/quotes/recalculate` | Ejecuta el recálculo manual (US2) |
| GET | `/api/quotes/executions?date=` | Consulta el resumen de ejecuciones por fecha (US2) |
| GET | `/api/players/{id}/quotes` | Historial de cotizaciones, ordenado de más reciente a más antigua (US3) |
| GET | `/api/players/{id}/quotes?date=` | Cotización a una fecha dada (US3) |
| GET | `/api/players/{id}/quotes/current` | Cotización vigente (US3) |
| GET | `/api/players/ranking` | Ranking de jugadores por precio vigente (US4) |

`GET /api/players/ranking` y `GET /api/players/{id}/quotes` conviven con
`PlayerController` (`GET /api/players/{id}`): Spring resuelve el literal `ranking` por delante del
patrón variable, sin ambigüedad de mapeo.

**Rationale**: El enunciado §4 fija `GET /players/:id/quotes`, `GET /players/ranking` y
`POST /quotes/recalculate`. Las rutas de inicialización y de resumen se añaden bajo el mismo
prefijo `/api/quotes` porque son la operación complementaria de la inicialización y la lectura del
resumen exigido por FR-061. El prefijo `/api/` es obligatorio (arquitectura §1.7).

**Alternatives considered**:
- *Exponer el historial y la cotización por fecha en rutas separadas (`/quotes/history`)*:
  rechazada; duplicaría recursos para el mismo jugador y se apartaría de la ruta obligatoria del
  enunciado.
- *Exponer la cotización vigente sin ruta propia, deducida de la última fila del historial*:
  rechazada; FR-037 y el escenario de US3 exigen una consulta de vigente explícita, y su forma de
  respuesta es más simple que la del historial.

## 15. Códigos de error nuevos

**Decision**: Códigos nuevos, literales en `GlobalExceptionHandler` como el resto del proyecto
(no existe enumeración central):

| Código | HTTP | Origen |
| --- | --- | --- |
| `MARKET_NOT_INITIALIZED` | 409 | FR-043, FR-063 |
| `MARKET_ALREADY_INITIALIZED` | 409 | FR-033, FR-034 |
| `INVALID_ADJUSTMENT_FACTOR` | 400 | FR-011 (rango de `k` fuera de [0,10; 1,00]) |
| `INVALID_VARIATION_CAP` | 400 | FR-012, FR-015 |
| `INVALID_MIN_PRICE_FLOOR` | 400 | FR-013, FR-015 |
| `UNKNOWN_QUOTE_STRATEGY` | 400 | FR-018 (estrategia fuera del catálogo) |
| `QUOTE_NOT_FOUND` | 404 | FR-040 (jugador sin cotización) |
| `EXECUTION_ALREADY_EXISTS` | 409 | FR-060 (ya existe una ejecución para la fecha de cotización, incluida la concurrencia) |

**Orden de precedencia de rechazos (FR-066)**: una solicitud con más de una causa de rechazo devuelve
siempre el código de mayor precedencia, en este orden: (1) `UNKNOWN_QUOTE_STRATEGY`, (2)
`MARKET_NOT_INITIALIZED`, (3) `INVALID_ADJUSTMENT_FACTOR`, (4) `INVALID_VARIATION_CAP` e
`INVALID_MIN_PRICE_FLOOR`, (5) `EXECUTION_ALREADY_EXISTS`. Las causas (1) a (4) se evalúan antes de
cualquier escritura y no registran resumen de ejecución; la causa (5) se evalúa al final y no altera
el resumen ya registrado.

**Rationale**: FR-011 exige un error controlado para un `k` fuera de rango; FR-018 exige un rechazo
controlado de una estrategia inexistente; FR-040 exige informar de la ausencia de cotización;
FR-060 exige un rechazo controlado de la segunda corrida de una fecha de cotización.
`MARKET_NOT_INITIALIZED` debe ser distinguible de `QUOTE_UNAVAILABLE` (409), que se conserva intacto
para el jugador sin cotización (FR-045).

**Alternatives considered**:
- *Reutilizar `INVALID_REQUEST` para los parámetros*: rechazado; FR-011 exige un error controlado
  identificable y el proyecto ya usa códigos específicos por causa.

## 16. Parámetros de la ejecución

**Decision**: `POST /api/quotes/recalculate` admite `strategy` (obligatorio) y, opcionalmente,
`adjustmentFactor`, `variationCap` y `minPriceFloor`. Los valores por defecto son `k` = 0,10,
tope = 0,10 (10 %, fracción dentro del rango cerrado (0; 1]) y piso = 1 centésima (FR-012,
FR-015), configurables por propiedad de aplicación (`market.quote.*`) y ajustables por el operador en
cada ejecución (FR-011, FR-015). Un `variationCap` fuera del rango (0; 1] se rechaza con
`INVALID_VARIATION_CAP` (FR-012).

**Rationale**: FR-011 y FR-015 exigen valores por defecto disponibles sin configuración explícita y
ajustables en cada ejecución. La configuración por propiedad cubre el "valor por defecto
configurable" y el body cubre el ajuste por ejecución.

**Alternatives considered**:
- *Valores únicamente por body, sin propiedades*: rechazada; FR-015 exige que existan valores por
  defecto sin configuración explícita.
- *Valores únicamente por configuración*: rechazada; FR-011 y FR-015 exigen ajuste en cada
  ejecución.

## 17. Trazabilidad de la estrategia

**Decision**: Cada fila no inicial registra `strategy_name` y `strategy_version`. En una fila
`RECOMPUTED` se registra la estrategia de la ejecución (la que produjo el ajuste). En una fila
`CARRIED_FORWARD` se registra la estrategia heredada de la cotización vigente cuyo precio se
repite, que es nula solo cuando ese precio procede de una fila `INITIAL` (FR-019, FR-020).

La estrategia efectiva de cada corrida, su versión y sus parámetros constan en el resumen persistido
de la ejecución (FR-061), y el instante de cálculo identifica la corrida (FR-025).

**Rationale**: Es la formulación literal de FR-019 y de la aclaración sobre la estrategia de la
fila arrastrada: registrar la estrategia de la ejecución en una fila arrastrada haría mentir la
trazabilidad, porque esa estrategia no produjo el precio.

**Alternatives considered**:
- *Registrar siempre la estrategia de la ejecución*: rechazado; contradice FR-019 y la aclaración
  explícita sobre la trazabilidad.

## 18. Observabilidad y rendimiento

**Decision**: La ejecución y las consultas se apoyan en los mecanismos de observabilidad ya
vigentes (registros estructurados, correlación de solicitudes, métricas y health checks), sin
reimplementarlos ni introducir mecanismos nuevos de caché (FR-064, FR-065). Las consultas de
cotización se resuelven por acceso directo al repositorio de cotizaciones, sin recalcular precios
(FR-030, FR-065).

**Rationale**: FR-064 y FR-065 prohíben expresamente ampliar la capa de observabilidad y fijar
objetivos de rendimiento propios.

**Alternatives considered**:
- *Añadir una caché de cotizaciones vigentes*: rechazada; FR-065 lo excluye expresamente.
- *Definir objetivos de latencia propios*: rechazada; FR-065 los excluye.

## 19. Fuera de alcance confirmado

Se mantiene fuera de alcance, sin ampliación en esta feature: job programado y agenda (FR-050),
interfaz de usuario (FR-051), política de obsolescencia de precios arrastrados (FR-052), regresión a
la media (FR-053), comisiones y spreads (FR-054), ampliación del catálogo de estrategias (FR-055),
reprecio por cambio de estrategia (FR-056), modificación del congelamiento de precio de las
publicaciones (FR-057) y ampliación del contrato de mercado (FR-058).

El ranking se resuelve por precio vigente, no por "estrategia activa" en el sentido literal del
enunciado §4, conforme a la aclaración de alcance de `REQ-S2-08` en la `spec.md` de esta feature.

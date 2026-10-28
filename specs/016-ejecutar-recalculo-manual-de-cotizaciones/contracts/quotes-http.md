# Contrato HTTP: Cotización de tokens de jugadores

**Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`
**Spec**: [`spec.md`](../spec.md) · **Data model**: [`../data-model.md`](../data-model.md)

Este documento es el contrato funcional y normativo de la API de cotizaciones. El detalle
OpenAPI de los Controllers y los DTO lo complementa; ante discrepancia, prevalece este documento.

Todas las rutas empiezan por `/api/`. Todas las respuestas de error usan `ErrorResponseDTO`
(`timestamp`, `status`, `error`, `code`, `message`, `path`). Los importes son **centésimas de
crédito** (100 centésimas = 1 crédito).

Todas las rutas exigen JWT válido, conforme a `anyRequest().authenticated()` de `SecurityConfig`.

---

## 1. `POST /api/quotes/initialize`

Inicializa el mercado: fija la primera fecha del mercado y deja una cotización inicial de 100
centésimas (1 crédito) a cada jugador cotizable (US1, FR-031, FR-034).

**Request** (body opcional):

```json
{ "marketDate": "2026-10-20" }
```

| Campo | Tipo | Obligatorio | Regla |
| --- | --- | --- | --- |
| `marketDate` | `date` ISO | no | Fecha de vigencia de las filas iniciales. Si se omite, se usa la fecha UTC del día en que corre la operación |

**Response 200**:

```json
{
  "marketDate": "2026-10-20",
  "initializedPlayers": 42,
  "alreadyInitialized": false
}
```

| Campo | Tipo | Semántica |
| --- | --- | --- |
| `marketDate` | `date` | Fecha de mercado vigente: fecha efectiva de las filas `INITIAL` |
| `initializedPlayers` | `integer` | Jugadores cotizables con fila inicial vigente |
| `alreadyInitialized` | `boolean` | `true` cuando el mercado ya estaba inicializado en esa fecha y la operación no escribió filas |

**Comportamiento**:

| Situación | Resultado |
| --- | --- |
| Mercado sin filas `INITIAL` | Escribe exactamente una fila `INITIAL` por jugador cotizable (FR-034) |
| Mercado inicializado en la **misma** fecha | No escribe filas; devuelve el mismo conjunto (FR-032) |
| Mercado inicializado en **otra** fecha | `409 MARKET_ALREADY_INITIALIZED`; el historial no se altera (FR-032, FR-033, FR-034) |
| Fecha solicitada anterior a cotizaciones existentes | `409 MARKET_ALREADY_INITIALIZED` (FR-033) |

**Errores**: `400 INVALID_REQUEST`, `401`, `409 MARKET_ALREADY_INITIALIZED`, `500 INTERNAL_ERROR`.

---

## 2. `POST /api/quotes/recalculate`

Ejecuta el recálculo manual de cotizaciones (US2, FR-059, FR-060, FR-061).

**Request**:

```json
{
  "strategy": "POSITIONAL_PERFORMANCE",
  "adjustmentFactor": 0.10,
  "variationCap": 0.10,
  "minPriceFloor": 1
}
```

| Campo | Tipo | Obligatorio | Regla |
| --- | --- | --- | --- |
| `strategy` | `string` | sí | Estrategia del catálogo: `POSITIONAL_PERFORMANCE` o `GENERAL_ATTRIBUTES` (FR-018, FR-055) |
| `adjustmentFactor` | `decimal` | no | Factor de ajuste `k`, rango cerrado [0,10; 1,00]; por defecto 0,10 (FR-011) |
| `variationCap` | `decimal` | no | Tope de variación por período, expresado como fracción del precio vigente en el rango cerrado (0; 1] (0 %..100 %); por defecto 0,10 (FR-012, FR-015) |
| `minPriceFloor` | `integer` | no | Piso mínimo de precio en centésimas, siempre > 0; por defecto 1 (FR-013, FR-015) |

El campo `quoteDate` **no** existe: la fecha de cotización es siempre la fecha del día en que corre
la ejecución (FR-060). Cada fecha de cotización admite como máximo una ejecución: una segunda
ejecución en la misma fecha se rechaza con `409 EXECUTION_ALREADY_EXISTS`, no escribe filas de
cotización y no altera el resumen ya registrado (FR-060).

**Response 200**:

```json
{
  "quoteDate": "2026-10-20",
  "strategy": "POSITIONAL_PERFORMANCE",
  "strategyVersion": "v1",
  "adjustmentFactor": 0.10,
  "variationCap": 0.10,
  "minPriceFloor": 1,
  "pricedPlayers": 42,
  "recomputedRows": 30,
  "carriedForwardRows": 12,
  "notRepricedPlayers": 3,
  "failedPlayers": 0,
  "startedAt": "2026-10-20T18:00:00Z",
  "finishedAt": "2026-10-20T18:00:12Z"
}
```

| Campo | Semántica |
| --- | --- |
| `pricedPlayers` | Jugadores cotizados en la ejecución: la población cotizable evaluada, incluidos los que no se repreciaron |
| `recomputedRows` | Filas `RECOMPUTED` escritas |
| `carriedForwardRows` | Filas `CARRIED_FORWARD` escritas |
| `notRepricedPlayers` | Jugadores sin valuación posterior a su fecha de vigencia de referencia, que no se repreciaron; subconjunto de `pricedPlayers` |
| `failedPlayers` | Jugadores con fallo individual; el detalle interno no se expone (FR-062) |

**Errores**:

| Código | HTTP | Causa |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | Body ausente o malformado |
| `INVALID_ADJUSTMENT_FACTOR` | 400 | `adjustmentFactor` fuera de [0,10; 1,00] (FR-011) |
| `INVALID_VARIATION_CAP` | 400 | `variationCap` fuera del rango (0; 1] (FR-012) |
| `INVALID_MIN_PRICE_FLOOR` | 400 | `minPriceFloor` no positivo (FR-013) |
| `UNKNOWN_QUOTE_STRATEGY` | 400 | Estrategia fuera del catálogo (FR-018) |
| `MARKET_NOT_INITIALIZED` | 409 | El mercado no está inicializado; no se escribe ninguna fila (FR-063) |
| `EXECUTION_ALREADY_EXISTS` | 409 | Ya existe una ejecución de cotización para la fecha de cotización del día, incluida la concurrencia entre dos ejecuciones (FR-060) |
| `401` | — | JWT ausente o inválido |
| `INTERNAL_ERROR` | 500 | Fallo general de la ejecución |

**Orden de precedencia**: cuando una solicitud reúne más de una causa de rechazo, se devuelve
siempre el código de mayor precedencia (FR-066): `UNKNOWN_QUOTE_STRATEGY` → `MARKET_NOT_INITIALIZED`
→ `INVALID_ADJUSTMENT_FACTOR` → `INVALID_VARIATION_CAP` / `INVALID_MIN_PRICE_FLOOR` →
`EXECUTION_ALREADY_EXISTS`. Ningún rechazo escribe filas de cotización ni registra resumen de
ejecución.

---

## 3. `GET /api/quotes/executions`

Consulta el resumen de las ejecuciones de cotización por fecha de cotización (FR-061).

**Query**: `date` (ISO `date`, obligatorio).

**Response 200**:

```json
{ "executions": [ { "quoteDate": "2026-10-20", "strategy": "POSITIONAL_PERFORMANCE", "strategyVersion": "v1", "adjustmentFactor": 0.10, "variationCap": 0.10, "minPriceFloor": 1, "pricedPlayers": 42, "recomputedRows": 30, "carriedForwardRows": 12, "notRepricedPlayers": 3, "failedPlayers": 0, "startedAt": "2026-10-20T18:00:00Z", "finishedAt": "2026-10-20T18:00:12Z" } ] }
```

Como máximo hay una ejecución por fecha de cotización (FR-060), de modo que la lista contiene un
único elemento cuando la fecha tiene ejecución. Una fecha sin ejecuciones devuelve una lista vacía,
no un error.

**Errores**: `400 INVALID_REQUEST` (fecha ausente o malformada), `401`.

---

## 4. `GET /api/players/{id}/quotes`

Historial de cotizaciones de un jugador, de la más reciente a la más antigua (US3, FR-036).

**Response 200**:

```json
{
  "playerId": 7821,
  "quotes": [
    {
      "effectiveDate": "2026-10-20",
      "price": 105,
      "previousPrice": 100,
      "multiplier": 1.05,
      "score": 0.6,
      "strategy": "POSITIONAL_PERFORMANCE",
      "strategyVersion": "v1",
      "sourceValuationDate": "2026-10-20",
      "origin": "RECOMPUTED",
      "calculatedAt": "2026-10-20T18:00:00Z"
    }
  ]
}
```

Un jugador sin cotizaciones devuelve `quotes: []`.

Cuando un jugador tiene dos filas con la misma fecha de vigencia —una `CARRIED_FORWARD` y una
`RECOMPUTED`—, el historial las devuelve con la `RECOMPUTED` antes que la arrastrada, y la
`RECOMPUTED` es su cotización vigente (FR-026, FR-037). La fila arrastrada documenta el precio
anterior al reprecio y la repreciada, el precio vigente.

Cuando la valuación disponible del jugador no es posterior a su fecha de vigencia de referencia —porque
sus métricas no cambiaron o porque la campaña que las provee no aportó datos nuevos—, la ejecución no
escribe fila para ese jugador: su precio vigente se conserva y su historial presenta un salto en esa
fecha (FR-027). Esa situación no se comunica con un código de error ni con una fila de origen vacío: la
ejecución la informa en el contador `notRepricedPlayers` de su resumen, y la consulta por fecha (§5)
devuelve la última cotización cuya fecha de vigencia no supera la fecha consultada.

**Errores**: `400 INVALID_PARAMETER_TYPE` (identificador malformado), `401`.

---

## 5. `GET /api/players/{id}/quotes?date=YYYY-MM-DD`

Cotización de un jugador a una fecha dada: la última cotización cuya fecha de vigencia no supera la
fecha consultada (US3, FR-038).

**Response 200**: un único objeto de cotización, con la misma forma que un elemento de `quotes` del
historial.

**Errores**: `400 INVALID_PARAMETER_TYPE` (identificador o fecha malformados), `401`,
`404 QUOTE_NOT_FOUND` (el jugador no tiene cotización anterior o igual a la fecha consultada).

---

## 6. `GET /api/players/{id}/quotes/current`

Cotización vigente de un jugador: su última cotización persistida (US3, FR-037).

**Response 200**: un único objeto de cotización.

El `calculatedAt` devuelto es el instante de cálculo de esa cotización, nunca el de la lectura
(FR-046).

**Errores**: `400 INVALID_PARAMETER_TYPE`, `401`, `404 QUOTE_NOT_FOUND`.

---

## 7. `GET /api/players/ranking`

Ranking de jugadores por precio vigente, de mayor a menor, con una sola entrada por jugador
cotizable (US4, FR-039).

**Response 200**:

```json
{
  "ranking": [
    { "position": 1, "playerId": 7821, "price": 105, "effectiveDate": "2026-10-20", "origin": "RECOMPUTED", "calculatedAt": "2026-10-20T18:00:00Z" }
  ]
}
```

Solo aparecen los jugadores cotizables con cotización vigente; un jugador cuyo precio vigente sea
`CARRIED_FORWARD` aparece con su precio repetido (SC-007). Los jugadores sin métricas no aparecen
porque no son cotizables (FR-001).

Cada entrada expone la fecha de vigencia y el origen de la fila vigente. La fecha de vigencia permite
distinguir, sin consultar el historial, a un jugador repreciado en la última corrida —`effectiveDate`
igual a la fecha de cotización de esa corrida y `origin` `RECOMPUTED`— de un jugador cuyo precio quedó
congelado porque sus métricas no cambiaron, que conserva la fecha de vigencia de su última cotización
efectiva (FR-027, FR-039). La población cotizable se determina con la última fecha de métricas de cada
jugador, de modo que un jugador sin métricas nuevas sigue siendo cotizable y sigue visible con su
precio vigente.

**Errores**: `401`.

---

## 8. Objeto de cotización

Todos los endpoints de consulta exponen los mismos campos de auditoría (FR-041):

| Campo | Tipo | Nulable | Origen |
| --- | --- | --- | --- |
| `effectiveDate` | `date` | no | Fecha de vigencia |
| `price` | `long` | no | Precio vigente en centésimas |
| `previousPrice` | `long` | sí | Nulo solo en la fila inicial |
| `multiplier` | `decimal` | no | Multiplicador aplicado a seis decimales |
| `score` | `decimal` | sí | Nulo en filas iniciales y arrastradas |
| `strategy` | `string` | sí | Estrategia que produjo el precio |
| `strategyVersion` | `string` | sí | Versión de la estrategia |
| `sourceValuationDate` | `date` | sí | Fecha de la valuación de origen |
| `origin` | `string` | no | `INITIAL`, `RECOMPUTED` o `CARRIED_FORWARD` |
| `calculatedAt` | `datetime` | no | Instante de cálculo |

---

## 9. Códigos de error

| Código | HTTP | Significado |
| --- | --- | --- |
| `MARKET_NOT_INITIALIZED` | 409 | El mercado todavía no fue inicializado; ninguna operación de mercado ni ejecución de cotización es válida |
| `MARKET_ALREADY_INITIALIZED` | 409 | La inicialización solicitada no procede: el mercado ya está inicializado en otra fecha de mercado (FR-033, FR-034) |
| `INVALID_ADJUSTMENT_FACTOR` | 400 | Factor de ajuste fuera del rango [0,10; 1,00] |
| `INVALID_VARIATION_CAP` | 400 | Tope de variación inválido |
| `INVALID_MIN_PRICE_FLOOR` | 400 | Piso mínimo no positivo |
| `UNKNOWN_QUOTE_STRATEGY` | 400 | Estrategia fuera del catálogo vigente |
| `EXECUTION_ALREADY_EXISTS` | 409 | Ya existe una ejecución de cotización para la fecha de cotización (FR-060) |
| `QUOTE_NOT_FOUND` | 404 | El jugador no tiene cotización disponible |
| `QUOTE_UNAVAILABLE` | 409 | Error vigente del mercado: el jugador no tiene cotización vigente. Se conserva sin cambios (FR-045) |

---

## 10. Compatibilidad con el mercado existente

La compra (`POST /api/orders/buy`) y la publicación de venta (`POST /api/orders/sell`) no cambian de
firma, de respuesta ni de código de error, salvo por una condición previa: se rechazan con
`MARKET_NOT_INITIALIZED` cuando el mercado todavía no fue inicializado (FR-043). El congelamiento
del precio publicado y la auditoría del precio aplicado no se modifican (FR-047, FR-057, FR-058).

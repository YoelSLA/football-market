# Quickstart: validación de la cotización de tokens

**Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`
**Spec**: [`spec.md`](spec.md) · **Contrato**: [`contracts/quotes-http.md`](contracts/quotes-http.md)

Guía de validación end-to-end. No contiene código de implementación: los detalles de modelo están
en [`data-model.md`](data-model.md) y las decisiones de diseño en [`research.md`](research.md).

---

## 1. Requisitos previos

| Requisito | Detalle |
| --- | --- |
| Base de datos | PostgreSQL accesible por la aplicación, con las migraciones aplicadas |
| Entorno | Variables de `application.yml` configuradas (base de datos, JWT) |
| Datos | Al menos un jugador con métricas persistidas en `player_statistics` (SPEC-012) y valuaciones en `player_positional_valuations` / `player_attribute_valuations` (SPEC-013) |
| Token | JWT de un usuario válido, para las operaciones autenticadas |
| Build | `./gradlew build -x test` desde `backend/` (no ejecuta tests) |

> Los tests automatizados los ejecuta el usuario. Esta guía describe validaciones manuales sobre
> la aplicación en ejecución.

---

## 2. Levantar la aplicación

```bash
cd backend
./gradlew bootRun
```

Verificar la disponibilidad:

```bash
curl -s http://localhost:8080/actuator/health
```

---

## 3. Inicializar el mercado (US1)

```bash
curl -s -X POST http://localhost:8080/api/quotes/initialize \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"marketDate":"2026-10-20"}'
```

**Esperado**: `200` con `initializedPlayers` igual al número de jugadores cotizables y
`alreadyInitialized: false`.

Repetir exactamente la misma llamada:

**Esperado**: `200` con `alreadyInitialized: true`, las mismas `initializedPlayers` y **ninguna** fila
adicional (FR-032).

Intentar de nuevo con otra fecha:

**Esperado**: `409` con `code: "MARKET_ALREADY_INITIALIZED"` y el historial intacto (FR-033).

Verificar la fila inicial de un jugador:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/players/7821/quotes
```

**Esperado**: exactamente una fila con `origin: "INITIAL"`, `price: 100`, `previousPrice: null`,
`multiplier: 1`, `score: null`, `strategy: null` y `effectiveDate` igual a la fecha de mercado.

---

## 4. Ejecutar el recálculo (US2)

```bash
curl -s -X POST http://localhost:8080/api/quotes/recalculate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"strategy":"POSITIONAL_PERFORMANCE"}'
```

**Esperado**: `200` con el resumen de la ejecución: `quoteDate` igual a la fecha del día,
`strategy: "POSITIONAL_PERFORMANCE"`, `strategyVersion: "v1"`, `adjustmentFactor: 0.10`,
`variationCap: 0.10`, `minPriceFloor: 1` y los contadores.

Repetir la misma llamada el mismo día:

**Esperado**: `409 EXECUTION_ALREADY_EXISTS`, sin filas escritas y sin alterar el resumen ya
registrado (FR-060, FR-066). Cada fecha de cotización admite como máximo una ejecución; las
valuaciones nuevas que lleguen ese mismo día se reflejarán en la ejecución del día siguiente.

### Casos de error esperados

| Llamada | Respuesta esperada |
| --- | --- |
| `{"strategy":"DESCONOCIDA"}` | `400 UNKNOWN_QUOTE_STRATEGY` |
| `{"strategy":"POSITIONAL_PERFORMANCE","adjustmentFactor":2}` | `400 INVALID_ADJUSTMENT_FACTOR` |
| `{"strategy":"POSITIONAL_PERFORMANCE","variationCap":1.5}` | `400 INVALID_VARIATION_CAP` (rango (0; 1]) |
| `{"strategy":"POSITIONAL_PERFORMANCE","minPriceFloor":0}` | `400 INVALID_MIN_PRICE_FLOOR` |
| Sin inicializar el mercado (base vacía) | `409 MARKET_NOT_INITIALIZED`, sin filas escritas |
| Segunda ejecución el mismo día | `409 EXECUTION_ALREADY_EXISTS`, sin filas escritas |

Con más de una causa de rechazo a la vez, se devuelve la de mayor precedencia: estrategia →
mercado no inicializado → factor de ajuste → resto de parámetros → ejecución ya existente
(FR-066).

### Casos funcionales a comprobar

| Caso | Comprobación |
| --- | --- |
| Jugador con valuación posterior a su fecha de vigencia de referencia | Fila `RECOMPUTED` con `score`, `sourceValuationDate`, estrategia y versión; `price = redondeoHalfUp(previousPrice × multiplicador)` |
| Jugador sin valuación para la estrategia elegida | Fila `CARRIED_FORWARD` con `price` igual al vigente, `multiplier: 1`, `score: null`, `strategy` heredada de la cotización vigente |
| Jugador con valuación anterior o igual a su fecha de vigencia de referencia | Sin fila nueva; el jugador aparece en `notRepricedPlayers` y su precio no cambia (FR-027) |
| Jugador con fila arrastrada y valuación posterior de la misma fecha | Se escribe una fila `RECOMPUTED` con esa fecha de vigencia, coexiste con la arrastrada y pasa a ser su cotización vigente; la arrastrada se conserva en el historial (FR-026, FR-027) |
| Cambio de estrategia entre dos ejecuciones | Ningún token cambia de precio por el solo cambio de estrategia; el ajuste siguiente llega en la ejecución posterior en la que exista una valuación posterior a la fecha de vigencia de referencia (FR-009, FR-027, FR-056) |
| Score exactamente 0,5 | Fila `RECOMPUTED` con precio sin cambios, **no** `CARRIED_FORWARD` (FR-021) |
| `adjustmentFactor` mayor que el tope | El tope limita el multiplicador efectivo (FR-012) |
| Precio por debajo del piso | El precio persistido es el piso y nunca es nulo ni negativo (FR-013, SC-001) |

---

## 5. Consultar historial, vigente, por fecha y ranking (US3, US4)

```bash
# Historial, de la más reciente a la más antigua
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/players/7821/quotes

# Cotización a una fecha dada
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/players/7821/quotes?date=2026-10-20"

# Cotización vigente
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/players/7821/quotes/current

# Ranking por precio vigente
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/players/ranking
```

**Esperado**:

- El historial devuelve todas las filas del jugador ordenadas de más reciente a más antigua.
- La cotización a una fecha devuelve la última fila cuya `effectiveDate` no supera la fecha.
- La cotización vigente devuelve la última fila persistida, con su `calculatedAt` original.
- Un jugador sin cotizaciones: `404 QUOTE_NOT_FOUND` en las consultas de vigente y por fecha, y
  `quotes: []` en el historial.
- El ranking devuelve una sola entrada por jugador cotizable, de mayor a menor precio, con el mismo
  precio que devuelve su cotización vigente (SC-007). Los jugadores sin métricas no aparecen.

---

## 6. Consultar el resumen de ejecuciones (FR-061)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/quotes/executions?date=2026-10-20"
```

**Esperado**: `200` con la lista de ejecuciones de esa fecha, de la más reciente a la más antigua, con
estrategia, versión, factor de ajuste, tope, piso y contadores. Una fecha sin ejecuciones devuelve una
lista vacía.

---

## 7. Regla operativa de la corrida semanal

Las métricas de los jugadores se obtienen por campañas (SPEC-012) y las valuaciones se producen
evaluando esas campañas (SPEC-013). La cotización consume la valuación, no las métricas: una corrida solo
reprecia a los jugadores cuya valuación es **posterior** a su fecha de vigencia de referencia, que es la
de su última fila `INITIAL` o `RECOMPUTED` (FR-027).

### 7.1 Cuándo evaluar y cuándo no

| Situación | Qué hacer | Por qué |
| --- | --- | --- |
| Campaña de métricas nueva y **confirmada** | `POST /api/valuations/evaluate` y, a continuación, `POST /api/quotes/recalculate`, **una sola vez** ese día | Hay datos nuevos: la valuación queda sellada con el día de la evaluación y reprecia a los jugadores con métricas |
| Campaña de métricas nueva pero **no confirmada** | **No evaluar ni recalcular.** Relanzar la extracción hasta que la campaña esté confirmada o sea utilizable | La capa de valuación reutiliza en silencio la campaña confirmada anterior: evaluar en ese momento reprecia a todos con datos que no cambiaron (ver TD-021) |
| Sin campaña de métricas nueva | **No evaluar ni recalcular** | No hay datos nuevos: el recálculo informaría `notRepricedPlayers` para toda la población, sin mover ningún precio |

Para relanzar la extracción de una campaña concreta se usa el canal JSON o el de scraping, con la misma
fecha de extracción y un lote acotado:

```bash
curl -s -X POST http://localhost:8080/api/players/statistics/whoscored/sync/json \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"extractionDate":"2026-10-20","maxPlayers":1}'
```

### 7.2 Cómo leer los tres resultados de una corrida

| Contador del resumen | Qué ocurrió con ese jugador | Qué se observa en su historial |
| --- | --- | --- |
| `recomputedRows` | Tiene valuación posterior a su fecha de vigencia de referencia | Fila `RECOMPUTED`: precio nuevo, `previousPrice` con el precio anterior, `score`, `multiplier` y `sourceValuationDate` |
| `notRepricedPlayers` | Su valuación **no** es posterior a su fecha de vigencia de referencia: sus métricas no cambiaron | **Ninguna fila ese día.** Su precio vigente se conserva y su historial presenta un salto en esa fecha |
| `carriedForwardRows` | No tiene valuación para la estrategia elegida | Fila `CARRIED_FORWARD`: precio repetido, `multiplier: 1`, `score: null`, estrategia heredada y `effectiveDate` igual a la fecha de la corrida |

El caso `notRepricedPlayers` no es un error ni se comunica con un código de error: es la forma en que el
sistema materializa que las métricas de ese jugador no cambiaron (FR-027). Un jugador cuyo item de
campaña falló con `NO_DATA`, `NOT_FOUND` o `PROVIDER_ERROR` queda en este grupo, conserva su precio y
se reprecia cuando una campaña posterior aporte sus métricas.

### 7.3 Cómo se distingue un precio congelado en el ranking

La población cotizable se calcula con la última fecha de métricas **de cada jugador**, de modo que un
jugador sin métricas nuevas sigue siendo cotizable y sigue visible en el ranking, con su precio
vigente. En cada entrada del ranking, `effectiveDate` y `origin` permiten distinguirlo sin consultar el
historial:

| Entrada del ranking | Significado |
| --- | --- |
| `effectiveDate` igual a la fecha de la última corrida | El jugador fue repreciado en esa corrida (`origin: RECOMPUTED`) |
| `effectiveDate` anterior a la fecha de la última corrida | Su precio quedó **congelado**: sus métricas no cambiaron y conservó el precio de su última cotización efectiva |

### 7.4 Auditoría de la deriva

Cuando una campaña no está confirmada y la valuación reutiliza la anterior, el `effectiveDate` de las
cotizaciones avanza sin que avance la fecha de extracción de las valuaciones. La consulta siguiente
lo hace observable:

```sql
SELECT q.effective_date, q.origin, q.previous_price, q.price, v.source_extraction_date
FROM player_quotes q
JOIN player_positional_valuations v
  ON v.player_id = q.player_id AND v.valuation_date = q.source_valuation_date
WHERE q.origin = 'RECOMPUTED'
ORDER BY q.effective_date DESC
LIMIT 50;

-- Si effective_date avanza y source_extraction_date se queda quieto,
-- hubo reprecio con datos estancados.
```

La deuda asociada a este comportamiento está registrada en `docs/TECHNICAL_DEBT.md` (TD-021) con su
solución propuesta y sus criterios de resolución.

---

## 8. Integración con el mercado (US5)

Con el mercado inicializado y cotizado:

```bash
curl -s -X POST http://localhost:8080/api/orders/buy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId":7821,"quantity":5}'
```

**Esperado**: `200`; el `unitPrice` de la respuesta es el precio vigente persistido. Tras un recálculo
posterior, la compra usa el nuevo precio vigente, mientras que una publicación de venta ya
publicada conserva el precio congelado en el momento de su publicación (FR-047).

| Caso | Respuesta esperada |
| --- | --- |
| Mercado no inicializado, compra o publicación de venta | `409 MARKET_NOT_INITIALIZED`, sin mover saldos, tenencias ni transacciones (FR-043) |
| Jugador sin cotización | `409 QUOTE_UNAVAILABLE` (FR-045) |
| Recálculo con mercado cotizado | Saldos, tenencias y transacciones sin cambios (FR-048) |

Comprobar que no queda ninguna fuente simulada:

```bash
rg -n "SimulatedQuoteSource|market.quote.simulator" backend/src
```

**Esperado**: sin coincidencias (SC-011).

---

## 9. Criterios de aceptación verificables

| Criterio | Cómo se comprueba |
| --- | --- |
| SC-001 | Ninguna fila de `player_quotes` con `price <= 0` (consulta SQL directa) |
| SC-002 | Toda fila `RECOMPUTED` con estrategia y versión; toda `INITIAL` sin estrategia; toda `CARRIED_FORWARD` con la estrategia heredada |
| SC-003 | `price` reconstruible desde `previous_price`, `score`, `multiplier` y el piso aplicado |
| SC-004 | Dos ejecuciones del mismo día dejan el mismo conjunto de cotizaciones |
| SC-005 | Un jugador nunca evaluado conserva su precio, tiene fila consultable y aparece como vigente |
| SC-006 | Tras la inicialización, todos los jugadores cotizables tienen precio y ningún jugador sin métricas aparece |
| SC-007 | El ranking coincide con la cotización vigente de cada jugador, una entrada por jugador |
| SC-008 | Un recálculo no altera saldos, tenencias ni transacciones |
| SC-009 | La compra usa el precio persistido y su `calculatedAt` de origen |
| SC-010 | Toda operación de mercado previa a la inicialización se rechaza con `MARKET_NOT_INITIALIZED` |
| SC-011 | No queda ninguna fuente de cotización simulada en el sistema |
| SC-012 | La ejecución devuelve su resultado en una sola interacción, sin llamadas a proveedores externos |
| SC-013 | Toda ejecución queda registrada y es consultable por fecha de cotización |
| SC-014 | Los fallos individuales quedan registrados con jugador, etapa y causa, sin exponerse en la respuesta |
| SC-015 | Toda ejecución sobre un mercado no inicializado se rechaza sin escribir filas |
| SC-016 | Toda solicitud con más de una causa de rechazo devuelve el código de mayor precedencia conforme a FR-066 |

---

## 10. Verificación local

```bash
cd backend
./gradlew build -x test
```

El build no ejecuta tests y no acredita el testing de la funcionalidad: conforme a la Constitución,
los tests los ejecuta el usuario y la validación de tests es una autorización humana separada.

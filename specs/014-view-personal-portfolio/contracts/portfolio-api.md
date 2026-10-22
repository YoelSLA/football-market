# Contrato HTTP: Consulta del portfolio personal

**Feature**: SPEC-014 · **Rama**: `feat/014-consultar-portfolio-personal` · **Fecha**: 2026-10-20
**Plan**: [`../plan.md`](../plan.md) · **Modelo**: [`../data-model.md`](../data-model.md) ·
**Especificación**: [`../spec.md`](../spec.md)

Este documento define el contrato público de la consulta del portfolio. Es la referencia funcional y
normativa de la API de esta feature; las anotaciones OpenAPI del Controller y los tests funcionales
deben ser coherentes con él.

---

## GET /api/portfolio/me

Devuelve el portfolio personal del usuario autenticado: el resumen financiero global, las posiciones
del usuario y los metadatos de la página solicitada.

### Identidad y autorización

| Aspecto | Regla |
| --- | --- |
| Identidad | Se obtiene del contexto de seguridad. El cliente **no** puede indicar el usuario consultado: no se acepta ningún identificador de usuario en la ruta ni en la consulta |
| Alcance | Cada usuario consulta únicamente su propio portfolio. El superusuario también consulta únicamente el suyo, por la misma vía |
| Sesión | Requiere sesión autenticada válida. Sin ella, la cadena de seguridad responde 401 y la consulta no se ejecuta |
| Exposición | La respuesta no revela la existencia de las posiciones, operaciones ni resultados de ningún otro usuario |
| Otras rutas | No existe ninguna otra ruta de consulta de portfolio, ni administrativa, ni por identificador de usuario |

### Request

| Parámetro | Tipo | Obligatorio | Por defecto | Reglas |
| --- | --- | --- | --- | --- |
| `page` | Entero | No | `0` | Mayor o igual a cero. La numeración de páginas comienza en cero |
| `size` | Entero | No | `20` | Entre 1 y 100 inclusive |
| `status` | Texto | No | — | Solo las formas exactas `OPEN` y `CLOSED`. Sin el parámetro se devuelven todas las posiciones |

No hay cuerpo de solicitud.

Un `page` o un `size` fuera de rango, un valor que no sea un entero representable, o un `status`
distinto de `OPEN` o `CLOSED` se rechazan con **400** y no ejecutan ninguna lectura.

### Response 200 OK

La respuesta tiene tres objetos de nivel superior. Los indicadores financieros y los metadatos de
paginación no se mezclan.

```json
{
  "summary": {
    "totalPositions": 3,
    "totalTokens": 120,
    "totalReservedTokens": 20,
    "totalAvailableTokens": 100,
    "totalInvested": 12000,
    "totalCurrentValue": 13500,
    "totalRealizedProfitLoss": 500,
    "totalUnrealizedProfitLoss": 1000,
    "totalProfitLoss": 1500,
    "totalProfitLossPercentage": 12.50,
    "totalBuyTransactions": 4,
    "totalSellTransactions": 2,
    "valuationComplete": true
  },
  "positions": [
    {
      "playerId": 41,
      "playerName": "Joel Robles",
      "teamName": "Real Betis Balompié",
      "position": "Goalkeeper",
      "imageUrl": "https://example.org/player/41.png",
      "active": true,
      "status": "OPEN",
      "valuationStatus": "VALUED",
      "quantity": 60,
      "reservedQuantity": 20,
      "availableQuantity": 40,
      "acquisitionCost": 6000,
      "averageUnitPrice": 100,
      "currentUnitPrice": 125,
      "priceUpdatedAt": "2026-10-20T14:00:00Z",
      "currentValue": 7500,
      "realizedProfitLoss": 0,
      "unrealizedProfitLoss": 1500,
      "totalProfitLoss": 1500,
      "unrealizedProfitLossPercentage": 25.00,
      "portfolioWeightPercentage": 55.56,
      "openedAt": "2026-10-18T10:12:00Z",
      "closedAt": null,
      "lastTransactionAt": "2026-10-18T10:12:00Z",
      "totalBuyTransactions": 2,
      "totalSellTransactions": 0
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

#### `summary`

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `totalPositions` | Entero | Cantidad global de posiciones **abiertas**. No depende del filtro ni de la página |
| `totalTokens` | Entero | Tokens abiertos poseídos, en todas las posiciones |
| `totalReservedTokens` | Entero | Tokens reservados por publicaciones activas y vigentes |
| `totalAvailableTokens` | Entero | `totalTokens − totalReservedTokens` |
| `totalInvested` | Entero (centésimas) | Costo de adquisición de las posiciones **abiertas** |
| `totalCurrentValue` | Entero (centésimas) | Valor actual total. Nulo si alguna posición abierta carece de cotización |
| `totalRealizedProfitLoss` | Entero (centésimas) | Resultado realizado acumulado, incluidos los ciclos cerrados |
| `totalUnrealizedProfitLoss` | Entero (centésimas) | Resultado no realizado total. Nulo si alguna posición abierta carece de cotización |
| `totalProfitLoss` | Entero (centésimas) | Resultado total. Nulo si alguna posición abierta carece de cotización |
| `totalProfitLossPercentage` | Decimal | Resultado total sobre el **costo histórico total pagado**, en puntos porcentuales con dos decimales. Nulo si el denominador es cero o si falta una cotización |
| `totalBuyTransactions` | Entero | Operaciones confirmadas en las que el usuario es comprador, en todas sus posiciones |
| `totalSellTransactions` | Entero | Operaciones confirmadas en las que el usuario es vendedor, en todas sus posiciones |
| `valuationComplete` | Booleano | `false` si alguna posición abierta carece de cotización |

**Reglas del resumen.**

- Representa siempre el **portfolio completo**: es idéntico con y sin filtro y con cualquier página.
- `totalInvested` y el denominador del porcentaje global son **importes distintos**: el primero es el
  costo de las posiciones abiertas; el segundo, el costo histórico total pagado. El porcentaje no es
  `totalProfitLoss / totalInvested`.
- `totalTokens = totalReservedTokens + totalAvailableTokens`.
- Los importes son enteros en centésimas de crédito: `12550` representa 125,50 créditos. No se usa
  punto flotante para el dinero. Los resultados pueden ser negativos y conservan su signo.
- Un indicador que depende de la cotización es **nulo**, nunca cero, cuando alguna posición abierta
  carece de cotización.

#### `positions`

Una entrada por jugador con tenencia u operaciones históricas del usuario. Los campos y su
significado están en [`../data-model.md`](../data-model.md) §3.2.

**Reglas de las posiciones.**

- `quantity = reservedQuantity + availableQuantity`. Los tokens reservados siguen formando parte de
  la tenencia y de su valoración.
- `status` es `OPEN` o `CLOSED`; `valuationStatus` es `VALUED`, `UNVALUED` o `NOT_APPLICABLE`, y es
  `NOT_APPLICABLE` si y solo si `status` es `CLOSED`.
- Una posición **abierta sin cotización vigente** conserva cantidad, reserva, disponibilidad y
  resultado realizado, y sus campos dependientes de cotización son **nulos**.
- Una posición **cerrada** no tiene valor actual ni resultado no realizado: esos campos son nulos y su
  resultado total es el resultado realizado acumulado. El **precio medio de adquisición** es un dato
  derivado de la cantidad abierta: es **nulo** en una posición cerrada, porque no hay cantidad abierta
  que lo defina, y nunca se sustituye por cero.
- `lastTransactionAt` es nulo cuando la posición no tiene ninguna operación confirmada.
- `openedAt` es el inicio del ciclo de tenencia vigente: las compras adicionales no lo alteran y una
  liquidación completa seguida de una compra inicia un ciclo nuevo. `closedAt` es la venta que llevó la
  tenencia a cero, y vuelve a ser nulo si la posición se reabre.
- `unrealizedProfitLossPercentage` es el rendimiento no realizado respecto del **costo de adquisición
  de esa posición**; `portfolioWeightPercentage` es su peso sobre el valor total del portfolio,
  considerado sobre **todas** las posiciones abiertas y no solo sobre la página consultada. Ambos son
  nulos cuando su denominador es cero, cuando falta una cotización, o cuando la posición está cerrada.
  El denominador del peso es el valor actual total del portfolio, que es nulo cuando alguna posición
  abierta carece de cotización vigente; en ese caso el peso es nulo en todas las posiciones abiertas.
- `totalBuyTransactions` y `totalSellTransactions` cuentan operaciones confirmadas, una vez cada una
  e con independencia de la cantidad de tokens. Las publicaciones pendientes, canceladas o vencidas y
  las operaciones fallidas no cuentan. La emisión inicial de tokens no cuenta como compra.
- Un jugador inactivo permanece en el portfolio con sus datos descriptivos. La consulta no habilita ni
  restringe operaciones comerciales.
- Si un jugador dejara de estar en el catálogo, la posición se conserva, sus resultados se preservan y
  los datos descriptivos que no puedan recuperarse se informan nulos, sin inventarlos.

**Orden de las posiciones** (determinista):

1. Posiciones abiertas **con** cotización, de mayor valor actual a menor.
2. Posiciones abiertas **sin** cotización.
3. Posiciones **cerradas**, por nombre de jugador.

En cualquier empate se resuelve por `playerId`.

#### `pagination`

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `page` | Entero | Número de página solicitada, desde cero |
| `size` | Entero | Tamaño de página solicitado |
| `totalElements` | Entero | Cantidad de posiciones del conjunto **filtrado** |
| `totalPages` | Entero | Cantidad de páginas del conjunto filtrado |

Si se solicita una página superior a la última, la respuesta es **200** con `positions` vacío y
metadatos correctos; el resumen sigue representando el portfolio completo.

### Errores

| Situación | HTTP | `code` | Cuerpo |
| --- | --- | --- | --- |
| `page` negativa, `size` fuera de 1–100 o valor no entero | 400 | `INVALID_PARAMETER_TYPE` | `ErrorResponseDTO` |
| `status` con un valor distinto de `OPEN` o `CLOSED` | 400 | `INVALID_PARAMETER_TYPE` | `ErrorResponseDTO` |
| Sesión ausente, inválida o vencida | 401 | — | Emitido por la cadena de seguridad; su cuerpo no está garantizado por este contrato |
| El estado persistido no permite reconstruir una posición | 500 | `INTERNAL_ERROR` | `ErrorResponseDTO` |
| Divergencia entre la tenencia persistida y la reconstrucción contable | 500 | `INTERNAL_ERROR` | `ErrorResponseDTO` |
| Falla técnica del servicio de cotizaciones | 503 | `QUOTE_SOURCE_UNAVAILABLE` | `ErrorResponseDTO` |

**Reglas de error.**

- Una ausencia de cotización **no** es un error: responde 200 con la valoración incompleta.
- Una falla técnica del servicio de cotizaciones **sí** lo es: responde 503 y **no** devuelve datos
  financieros parciales, ni siquiera el resultado realizado, que no depende de la cotización.
- Los errores 500 son genéricos: no revelan la inconsistencia detectada ni detalle técnico.
- El contrato de error de la aplicación es `ErrorResponseDTO` con `timestamp`, `status`, `error`,
  `code`, `message` y `path`.

### Estructura de las respuestas de error

```json
{
  "timestamp": "2026-10-20T15:42:11Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_PARAMETER_TYPE",
  "message": "La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100",
  "path": "/api/portfolio/me"
}
```

---

## Estructura de la respuesta

| Objeto | Contenido |
| --- | --- |
| `summary` | Indicadores globales del portfolio, independientes del filtro y de la página |
| `positions` | Posiciones filtradas y paginadas, con su detalle |
| `pagination` | Metadatos de la página solicitada |

El documento JSON completo de la respuesta, con todos sus campos, está en
[`../data-model.md`](../data-model.md) §3.

## Idempotencia y reintentos

La consulta es de **solo lectura**: no crea, modifica ni elimina operaciones, tenencias, reservas,
publicaciones, cotizaciones ni saldos, y no altera el resultado de ningún otro usuario. Repetirla
devuelve el mismo resultado para el mismo estado de datos.

## Referencias

- Especificación funcional: [`../spec.md`](../spec.md)
- Modelo de datos: [`../data-model.md`](../data-model.md)
- Decisiones de diseño: [`../research.md`](../research.md)
- Contrato de errores del proyecto: `docs/backend/architecture.md` §1.7
- Contrato del componente contable consumido: su implementación está integrada en `dev`. Esta feature
  consume **capacidades** (cifras por posición, agregados contables, hechos del ciclo de tenencia,
  recuentos por rol e instante de la cotización), no firmas concretas:
  `specs/015-define-average-cost-and-financial-results-rules/contracts/financial-position-service.md`

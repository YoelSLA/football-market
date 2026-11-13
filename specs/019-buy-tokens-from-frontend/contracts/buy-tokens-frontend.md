# Contratos de consumo de Market (SPEC-019)

**Fuentes autoritativas:** [catálogo oficial SPEC-010 / Issue #90](market-official.md), [cotizaciones SPEC-016](../../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md) y [compra SPEC-010](../../010-buy-tokens/contracts/buy-tokens.md). Este documento no redefine esos endpoints. [Spec](../spec.md) · [Plan](../plan.md) · [Modelo](../data-model.md).

## Catálogo de emisión oficial (Evolución SPEC-010 / Issue #90)

El contrato de lectura consolidada del Mercado Oficial está definido en detalle en [market-official.md](market-official.md) (`GET /api/market/official`).
Este endpoint consolida la consulta deportiva con la cotización vigente y la variación porcentual en una única petición paginada con filtros y ordenamiento en el servidor, sustituyendo las consultas N+1 por jugador para la vista de catálogo.

## Lectura de cotización real (Histórico / Respaldo)

`GET /api/players/{id}/quotes/current`, autenticado, desde `marketService.getCurrentQuote(id, signal)`:

| Respuesta | Comportamiento consumidor |
| --- | --- |
| 200 | `QuoteResponseDTO`: usar `price` (centésimas), `effectiveDate` (fecha de vigencia) y `calculatedAt` (instante de cálculo de la cotización persistida). Mapper interno → `MarketQuote`. |
| 404 `QUOTE_NOT_FOUND` | Sin cotización operativa; no confirmar compra, sin fallback demo. |
| 401 | Mecanismo de sesión existente redirige al acceso público; no restaurar modal. |
| Otros fallos | Mostrar estado de error/reintento de cotización; no inventar precio ni timestamp. |

**Precisiones del diagnóstico frontend:** null solamente ante HTTP 404 con código `QUOTE_NOT_FOUND`, no ante cualquier 404. HTTP 401 conserva la limpieza/redirección global de sesión; HTTP 403 significa falta de permiso; otros HTTP y fallos sin respuesta se distinguen de ausencia. Una respuesta 200 se valida en el Mapper: precio entero positivo seguro y fechas válidas, sin coerción de strings ni timestamps inventados. Respuesta inválida bloquea confirmación y permite reintentar lectura. No se reintentan automáticamente autenticación/permisos/404 inesperado/respuesta inválida; red y 5xx admiten como máximo un reintento automático. El GET explícito de reintento no ejecuta compras.

La query expone carga, pausa, disponible, ausente y las categorías de error. Los datos cacheados de una consulta con error, pausa o refetch no habilitan Confirmar. Page Hook y handler requieren la misma lectura exitosa resuelta y sesión autenticada, además de cantidad 1–100 y ausencia de envío/éxito. El motivo se presenta junto al botón y en la ficha, sin cuerpos de error técnicos. No se cambia ningún contrato del servidor.

La lectura del precio no lo congela: el servidor decide el precio aplicado al confirmar compra. Se usa `marketQueryKeys.currentQuote(id)`, nunca `playersQueryKeys.tokenQuote(id)` (demo). La Page conecta `useMarketQuotesQuery(idsVisibles)`, que mantiene una consulta real por id; las fichas y el modal reciben los Models por props, sin Hooks propios ni GET duplicado al abrir.

## Compra

`POST /api/orders/buy`, autenticado, desde `marketService.buyTokens({ playerId, quantity })`:

```ts
interface OrderBuyRequestDTO { playerId: number; quantity: number }
interface OrderBuyResponseDTO {
  transactionId: number; playerId: number; quantity: number;
  unitPrice: number; totalAmount: number; executedAt: string;
  listingId?: number;
}
```

No enviar `listingId` ni identificador de usuario. El campo de formulario empieza con el texto `"1"`; `"10"` y otros textos de dígitos cuyo valor sea entero 1–100 se validan y se convierten a `number` antes de construir `OrderBuyRequestDTO`. Texto vacío, no numérico, decimal o fuera de rango no se envía. Los jugadores visibles del listado no se filtran por stock; el POST puede devolver `INSUFFICIENT_STOCK`. El Service mapea la respuesta a `BuyResult` **sin `playerName`** dentro de su frontera. El Page Hook toma `selectedPlayer.name` de los datos de presentación y lo combina con `BuyResult` exclusivamente en el texto del toast; el Service y el Mapper no aceptan ni devuelven el nombre. La notificación muestra cantidad, `transactionId`, precio unitario, importe total y `executedAt` efectivos; la cotización del GET solo sirve para la estimación previa. Cerrar modal a los 2 s o por acción del usuario, sin bloquear actualización eventual de Portfolio.

| Código POST | Mensaje seguro para el usuario |
| --- | --- |
| `INVALID_REQUEST` | La solicitud no es válida. Verificá los datos e intentá nuevamente. |
| `INVALID_QUANTITY` | Cantidad inválida. |
| `INSUFFICIENT_CREDITS` | Créditos insuficientes para completar la compra. |
| `INSUFFICIENT_STOCK` | No hay tokens disponibles para este jugador. |
| `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE` | Cotización no disponible, intente más tarde. |
| `SUPERUSER_CANNOT_BUY` | Los administradores no pueden comprar tokens. |
| `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE` | Jugador no disponible. |
| `INTERNAL_ERROR`, red, 500, desconocido | Error del servidor, intente más tarde. |
| 401 | Gestión global de sesión; no guardar intención ni restablecer modal. |

El Page Hook `useMarketBuyPage` interpreta `getErrorCode` de `@/infrastructure/http` y usa mensajes explícitos; no muestra textos desconocidos del servidor ni detalles técnicos. Compone el Form Hook y el Mutation Hook sin que el primero importe al segundo. Si falla el POST, conserva el modal y habilita reintento. Solo el éxito dispara invalidaciones.

**Cobertura objetiva SC-03:** la tabla contiene una entrada para cada uno de los diez códigos de compra de FR-10; los códigos desconocidos, 500 y red usan el fallback genérico y 401 delega la sesión. En cambio, el 404 `QUOTE_NOT_FOUND` del GET significa ausencia de cotización y no se interpreta como `PLAYER_NOT_FOUND` del POST. Esta delimitación permite cotejar la lista de FR-10, esta tabla y los escenarios de `quickstart.md` sin prometer que se conocen futuros códigos backend.

## Límite de integración

`market` invalida únicamente su key de cotización vigente y comunica éxito mediante `onPurchaseSuccess(playerId)` a `app`. `app`, importando `portfolioQueryKeys` desde `features/portfolio/index.ts`, invalida `summary` y el prefijo de todas las `holdings(...)`; no copia los literales de key ni introduce imports entre features. La app obtiene los jugadores vía API raíz de `players`, y entrega a market su Model estructural propio. `infrastructure/http` solo agrega autenticación y transporta AxiosError sin entender negocios.

## Modalidad secundaria: publicaciones de usuarios

### Lectura del mercado

`GET /api/listings`, autenticado, sin parámetros. El consumidor usa el contrato autoritativo de
[SPEC-011](../../011-sell-player-tokens/contracts/listings.md).

```ts
interface TokenListingDTO {
  listingId: number;
  sellerId: number;
  playerId: number;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: "ACTIVE";
  createdAt: string;
  expiresAt: string;
}

interface TokenListingsDTO { listings: TokenListingDTO[] }
```

El Mapper valida objeto, colección, enteros seguros positivos, `totalAmount === quantity * unitPrice`,
estado `ACTIVE` y fechas ISO válidas. No convierte strings numéricos ni fabrica campos. La vigencia del
backend es autoritativa; `expiresAt` solo permite feedback local y refetch. El `sellerId` no se presenta ni
se resuelve como identidad pública.

### Compra de una publicación

`POST /api/orders/buy`, autenticado:

```ts
interface BuyListingRequestDTO { listingId: number }
```

Con `listingId` presente no se envían `playerId`, `quantity` ni precio. La respuesta reutiliza
`OrderBuyResponseDTO` y exige que `listingId` coincida con el solicitado antes de presentar éxito.

| Código | Comportamiento consumidor |
|---|---|
| `LISTING_NOT_AVAILABLE` | Informar que la oferta dejó de estar disponible e invalidar/refrescar listings. |
| `LISTING_NOT_FOUND` | Informar indisponibilidad y retirar el recurso tras refetch. |
| `CANNOT_BUY_OWN_LISTING` | Rechazo defensivo; refrescar porque el mercado público no debería entregarla. |
| `INSUFFICIENT_CREDITS` | Informar saldo insuficiente; conservar modal para revisión/cierre. |
| `INSUFFICIENT_STOCK` | Informar que el lote ya no puede liquidarse y refrescar listings. |
| `SUPERUSER_CANNOT_BUY` | Informar que administradores no pueden comprar. |
| `INVALID_REQUEST` | Informar solicitud inválida sin reintento automático. |
| `INTERNAL_ERROR`, 500 | Error genérico; no afirmar resultado transaccional. |
| Red/sin respuesta concluyente | Resultado incierto; no reintentar automáticamente ni afirmar fallo. |
| 401 | Gestión global de sesión; no restaurar intención de compra. |

Una compra exitosa invalida publicaciones, usuario actual, resumen y tenencias. La query de publicaciones
es la única fuente remota para catálogo y detalle; estos no mantienen copias divergentes.

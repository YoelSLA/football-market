# Contratos de consumo de Market — Publicar ventas de tokens (SPEC-021)

**Fuentes autoritativas:** [cotizaciones SPEC-016](../../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md) y [venta SPEC-011](../../011-sell-player-tokens/spec.md). Este documento no redefine esos endpoints. [Spec](../spec.md) · [Plan](../plan.md) · [Modelo](../data-model.md).

## Lectura de cotización real

`GET /api/players/{id}/quotes/current`, autenticado, desde `marketService.getCurrentQuote(id, signal)`:

| Respuesta | Comportamiento consumidor |
| --- | --- |
| 200 | `QuoteResponseDTO`: usar `price` (centésimas), `effectiveDate` (fecha de vigencia) y `calculatedAt` (instante de cálculo de la cotización persistida). Mapper interno → `MarketQuote`. |
| 404 `QUOTE_NOT_FOUND` | Sin cotización operativa; no habilitar Confirmar, sin fallback demo. |
| 401 | Mecanismo de sesión existente redirige al acceso público; no restaurar modal. |
| Otros fallos | Mostrar estado de error/reintento de cotización; no inventar precio ni timestamp. |

**Precisiones del diagnóstico frontend:** null solamente ante HTTP 404 con código `QUOTE_NOT_FOUND`, no ante cualquier 404. HTTP 401 conserva la limpieza/redirección global de sesión; HTTP 403 significa falta de permiso; otros HTTP y fallos sin respuesta se distinguen de ausencia. Una respuesta 200 se valida en el Mapper: precio entero positivo seguro y fechas válidas, sin coerción de strings ni timestamps inventados. Respuesta inválida bloquea confirmación y permite reintentar lectura. No se reintentan automáticamente autenticación/permisos/404 inesperado/respuesta inválida; red y 5xx admiten como máximo un reintento automático. El GET explícito de reintento no ejecuta publicaciones.

La query expone carga, pausa, disponible, ausente y las categorías de error. Los datos cacheados de una consulta con error, pausa o refetch no habilitan Confirmar. Page Hook y handler requieren la misma lectura exitosa resuelta y sesión autenticada, además de cantidad 1-100 y ausencia de envío/éxito. El motivo se presenta junto al botón y en la ficha, sin cuerpos de error técnicos. No se cambia ningún contrato del servidor.

La lectura del precio **no lo congela**: antes del POST, precio e importe son estimados y no se presenta vencimiento definitivo. Al activar Confirmar se fuerza una nueva lectura de `GET /api/players/{id}/quotes/current`. Si el precio difiere del revisado, se actualiza la estimación y se requiere nueva confirmación expresa antes de enviar. Si coincide y la lectura es válida, se envía el POST. El backend decide autoritativamente el precio aplicado y devuelve los datos definitivos (`unitPrice`, `totalAmount`, `expiresAt`), presentados en la confirmación persistente de éxito. Una variación entre la lectura de revalidación y el POST no puede prevenirse con el contrato disponible; se muestran los valores devueltos por el POST. Se usa `marketQueryKeys.currentQuote(id)`, nunca `playersQueryKeys.tokenQuote` (demo).

## Publicación de venta

`POST /api/orders/sell`, autenticado, desde `marketService.sellTokens({ playerId, quantity })`:

```ts
interface OrderSellRequestDTO {
  playerId: number;
  quantity: number; // entero 1-100
}
interface OrderSellResponseDTO {
  listingId: number;
  playerId: number;
  quantity: number;
  unitPrice: number;   // centésimas, congelado
  totalAmount: number; // centésimas, congelado
  status: string;      // "ACTIVE"
  createdAt: string;   // ISO-8601 con offset
  expiresAt: string;   // ISO-8601 con offset, +24h
}
```

No enviar identificador de usuario ni `listingId` de entrada. El campo de formulario empieza con el texto `"1"`; `"10"` y otros textos de dígitos cuyo valor sea entero 1-100 se validan y se convierten a `number` antes de construir `OrderSellRequestDTO`. Texto vacío, no numérico, decimal o fuera de rango no se envía. Los jugadores visibles del listado no se filtran por stock; el POST puede devolver `INSUFFICIENT_STOCK`. El Service mapea la respuesta a `SellResult` **sin `playerName`** dentro de su frontera. El Page Hook toma `selectedPlayer.name` de los datos de presentación y lo combina con `SellResult` exclusivamente en el texto del toast. La notificación muestra cantidad, `listingId`, precio unitario, importe total y `expiresAt` efectivos; la cotización del GET solo sirve para la estimación previa. El modal de éxito permanece visible hasta que el usuario navegue o refresque (FR-F09).

| Código POST | Mensaje seguro para el usuario |
| --- | --- |
| `INVALID_REQUEST` | La solicitud no es válida. Verificá los datos e intentá nuevamente. |
| `INVALID_QUANTITY` | Cantidad inválida. |
| `INSUFFICIENT_STOCK` | No hay tokens disponibles para este jugador. |
| `QUOTE_UNAVAILABLE` | Cotización no disponible, intente más tarde. |
| `INVALID_QUOTE_PRICE` | Cotización no disponible, intente más tarde. |
| `SUPERUSER_CANNOT_SELL` | Los administradores no pueden publicar tokens. |
| `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE` | Jugador no disponible. |
| `INTERNAL_ERROR`, red, 500, desconocido | Error del servidor, intente más tarde. |
| 401 | Gestión global de sesión; no guardar intención ni restablecer modal. |

**`QUOTE_UNAVAILABLE` no es un error de cambio de precio:** significa que no hay precio vigente disponible en el momento de publicar (FR-007). La revalidación GET al confirmar compara la lectura con el precio revisado; si difiere, se requiere reconfirmación. No se garantiza detectar una variación ocurrida entre esa revalidación y el POST; el backend aplica autoritativamente el precio vigente al procesarlo.

**Cobertura objetiva:** la tabla contiene una entrada para cada uno de los códigos de publicación del contrato SPEC-011; los códigos desconocidos, 500 y red usan el fallback genérico y 401 delega la sesión. El 404 `QUOTE_NOT_FOUND` del GET significa ausencia de cotización y no se interpreta como `PLAYER_NOT_FOUND` del POST.

## Límite de integración

`market` invalida únicamente su key de cotización vigente y comunica éxito mediante `onSellSuccess(playerId)` a `app`. Para sincronizar tenencias al abrir el modal y después de un POST fallido con sesión activa, `market` invoca el callback `onRefreshHoldings(playerId)` provisto por `app`; `app` invalida el prefijo de todas las `portfolioQueryKeys.holdings(...)`, haciendo que las cantidades actualizadas vuelvan a `MarketSellPage` mediante props. No se invoca para 401, que mantiene la redirección global de sesión. Tras éxito, `app`, importando `portfolioQueryKeys` desde `features/portfolio/index.ts`, invalida `summary` y el prefijo de todas las `holdings(...)` mediante `onSellSuccess(playerId)`. `app` conserva la propiedad de las consultas; `market` no importa portfolio ni copia literales de key. `infrastructure/http` solo agrega autenticación y transporta AxiosError sin entender negocios.

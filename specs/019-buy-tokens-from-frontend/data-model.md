# Modelo de datos: Comprar tokens desde el frontend

## Contratos y propietarios

| Concepto | Propietario | Campos relevantes y procedencia |
| --- | --- | --- |
| `Player` | `players`, leído solo por `app` desde API raíz | `id`, `name`, `teamName`, `leagueName`, `position`, `imageUrl`, `fallbackImageUrl`; listado visible y paginado. |
| `MarketPlayer` | `market/types/models.ts` | `id`, `name`, `teamName`, `leagueName`, `position`, `imageUrl`, `fallbackImageUrl` como Model estructural propio. `app` lo construye desde `Player` sin importar tipos de players dentro de market. |
| `CurrentMarketQuoteDTO` | `market/types/dtos.ts`, usado solo en Service/Mapper | Respuesta GET real: `price: number` (centésimas), `effectiveDate: string` (fecha de vigencia), `calculatedAt: string` (instante de cálculo). El backend devuelve además campos de auditoría según SPEC-016; se tipan los necesarios para la presentación sin alterar su contrato. |
| `MarketQuote` | `market/types/models.ts` | `price`, `effectiveDate`, `calculatedAt` preservados sin inventar ni reemplazar fechas. No es `TokenQuoteSnapshot` demo. |
| `OrderBuyRequestDTO` | `market/types/dtos.ts` | `playerId: number`, `quantity: number`. Sin `listingId` ni identidad de usuario. |
| `OrderBuyResponseDTO` | `market/types/dtos.ts` | `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `executedAt`, `listingId?` del backend. |
| `BuyResult` | `market/types/models.ts` | Solo `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `executedAt` ejecutados; **no** contiene `playerName`. Precio e importe vienen del POST, no del GET estimado. |
| `BuyEstimation` | `market/types/models.ts` | `quantity`, `unitPriceCentimes`, `totalCentimes`, derivado de quote vigente consultada. |
| `BuyQuantityForm` | `market/types/models.ts` | `quantity` como string editable, inicial `"1"`; validación de dígitos que representan entero 1–100, conversión a number antes de enviar. |

`marketService.getCurrentQuote(playerId, signal)` y `marketService.buyTokens(request)` entregan **Models**: Mapper transforma DTO→Model dentro del Service, sin recibir el nombre de la UI. `BuyResult` conserva exclusivamente campos del POST. `useMarketBuyPage` combina `selectedPlayer.name` con `BuyResult` **solo al construir el texto de la notificación**, sin modificar el Model ni el Service. El POST puede tener precio diferente del GET: el toast informa el resultado efectivo, no el estimado.

## Validación y UI

- `playerId` entero positivo del listado de jugadores visibles, independientemente del stock (el POST lo verifica). `quantity` inicia como texto `"1"`; cadenas de dígitos que representan enteros 1–100 como `"10"` se validan y convierten a `number` antes de enviarlas (`10`). Texto vacío, letras, fracciones, exponentes y cantidades fuera de rango se rechazan; al borrar el campo se deshabilita Confirmar y se muestra error inline «Cantidad entre 1 y 100». El backend nunca recibe strings como `quantity` desde esta UI.
- Quote ausente (404), precio nulo/no positivo o error de carga: «Cotización no disponible» y Confirmar deshabilitado; error de red indica que no se pudo consultar, nunca presenta demo como reemplazo.
- `formatCredits(centimes)` utiliza 2 decimales con coma; `formatCentimes` agrupa millares con punto. `quantity * price` sigue en centésimas; ejemplo `10 × 125 = 1.250 centésimas = 12,50 créditos`.
- `formatQuoteCalculatedAt(calculatedAt)` muestra la fecha/hora reales del cálculo con locale `es-AR` (por ejemplo «Cotización calculada el …»); `effectiveDate` es otro dato, no un sustituto de la hora. Nunca usar `Date.now()` ni la `date` demo. Si no se dispone del instante, no se fabrica uno: se muestra estado indisponible.
- Error funcional: mapping de `INVALID_REQUEST`, `INVALID_QUANTITY`, `INSUFFICIENT_CREDITS`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `SUPERUSER_CANNOT_BUY`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INTERNAL_ERROR` a mensajes de [contrato](contracts/buy-tokens-frontend.md). `401` deja actuar al mecanismo de sesión.

## Claves e invalidación

### Estado de lectura de cotización

`MarketQuoteStatus`: loading, paused, available, missing, authentication-error, forbidden, http-error, network-error, invalid-response. `MarketQuoteReadError` es un Model de error seguro de market: razón tipada y HTTP status opcional, sin cuerpos, credenciales ni dependencias de Axios/React. Service traduce errores técnicos a este Model después de los interceptores globales; Query Hook no importa DTO ni HTTP. Mapper valida unknown antes de producir MarketQuote; price positivo/entero seguro y fechas reales válidas obligatorios.

La query solo entrega `quote` utilizable en available; el resto entrega null, razón visible y reintento GET cuando corresponde. No se borra/inventa precio en el servidor, ni se reemplaza por cotización demo. Ficha y modal comparten estado y query por id. Page Hook usa sesión pública (sin almacenamiento directo), estado y cantidad para guard de handler y botón, exponiendo explicación de bloqueo por props.

```text
marketQueryKeys.currentQuote(id) = ['market', 'current-quote', id]
portfolioQueryKeys.summary = ['portfolio', 'summary']
portfolioQueryKeys.holdings(status, page, pageSize) = ['portfolio', 'holdings', status, page, pageSize]
```

El Query Hook `useMarketQuotesQuery(ids)` utiliza las keys individuales `marketQueryKeys.currentQuote(id)` para los jugadores visibles; la Page distribuye las cotizaciones por props a tarjeta y modal, sin Hooks en Components locales ni GET adicional al abrir. Tras el POST exitoso: el Page Hook de market invalida `marketQueryKeys.currentQuote(id)`; `app` invalida `portfolioQueryKeys.summary` y todas las `holdings` usando prefijo derivado de la función pública (sin importar desde market). Ningún módulo usa `playersQueryKeys.tokenQuote`, que apunta a datos demo.

Estado local del modal: cerrado / abierto (cantidad inicial `"1"`, estimación si hay cotización) / enviando / éxito (auto-cierre 2 s o acción) / error (permanece abierto para reintentar). Una protección síncrona en el submit y el estado de la mutación impiden solicitudes duplicadas. Abrir/cerrar no remonta el mercado: preserva scroll, página, filtros que existan y pestaña; no requiere crear controles de filtro. El foco retorna al disparador existente salvo navegación a login.

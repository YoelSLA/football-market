# Modelo de datos: Publicar ventas de tokens desde el frontend (SPEC-021)

## Contratos y propietarios

| Concepto | Propietario | Campos relevantes y procedencia |
| --- | --- | --- |
| `MarketPlayer` | `market/types/models.ts` (definido en SPEC-019, reusado) | `id`, `name`, `teamName`, `leagueName`, `position`, `imageUrl`, `fallbackImageUrl`; `app` construye desde `Player` de `players` sin importar tipos de players dentro de market. |
| `MarketQuote` | `market/types/models.ts` (definido en SPEC-019, reusado) | `price` (centésimas), `effectiveDate`, `calculatedAt` preservados sin inventar ni reemplazar fechas. Obtenido de `GET /api/players/{id}/quotes/current`. |
| `OrderSellRequestDTO` | `market/types/dtos.ts` | `playerId: number`, `quantity: number` (entero 1-100). Cuerpo del `POST /api/orders/sell`. |
| `OrderSellResponseDTO` | `market/types/dtos.ts` | `listingId: number`, `playerId: number`, `quantity: number`, `unitPrice: number` (centésimas), `totalAmount: number` (centésimas), `status: string` ("ACTIVE"), `createdAt: string` (ISO-8601), `expiresAt: string` (ISO-8601, +24h). Respuesta del POST. |
| `SellResult` | `market/types/models.ts` (nuevo) | `listingId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `status`, `createdAt`, `expiresAt` — campos exclusivamente del POST; **no** contiene `playerName` ni datos de presentación. |
| `SellEstimation` | `market/types/models.ts` (nuevo) | `quantity`, `unitPriceCentimes`, `totalCentimes`, derivado del quote vigente consultada (estimación orientativa, no precio garantizado). |
| `SellQuantityForm` | `market/types/models.ts` (nuevo) | `quantity: string` editable, inicial `"1"`; validación de dígitos que representan entero 1-100, conversión a `number` antes de enviar. |

`marketService.sellTokens(request)` y `marketService.getCurrentQuote(playerId, signal)` entregan **Models**: Mapper transforma DTO → Model dentro del Service, sin recibir el nombre de la UI. `SellResult` conserva exclusivamente campos del backend. El Page Hook combina `selectedPlayer.name` con `SellResult` **solo al construir el texto de la notificación**, sin modificar el Model ni el Service.

Para las tenencias, T006 expone el Query Hook existente `usePortfolioHoldingsQuery` por la API pública raíz de portfolio, mediante sus barrels de hooks, y solo los Models necesarios mediante sus barrels de tipos. `app` consulta posiciones `OPEN` (incluidas las totalmente reservadas), respeta la paginación y adapta `quantity` → total, `availableQuantity` → disponible y `reservedQuantity` → reservada a las props propias de market. No se comparten DTO ni se importa portfolio desde market; la composición cubre el conjunto elegible de FR-F02 antes de la paginación visible y los filtros de venta.

## Validación y UI

- `playerId` entero positivo del listado de jugadores visibles, independientemente del stock (el POST lo verifica). `quantity` inicia como texto `"1"`; cadenas de dígitos que representan enteros 1-100 como `"10"` se validan y convierten a `number` antes de enviarlas (`10`). Texto vacío, letras, fracciones, exponentes y cantidades fuera de rango se rechazan; al borrar el campo se deshabilita Confirmar y se muestra error inline «Cantidad entre 1 y 100». El backend nunca recibe strings como `quantity` desde esta UI.
- Límite superior: `min(100, tokens disponibles)` donde "tokens disponibles" = tenencia menos reservas activas y vigentes (dato visible desde portfolio/holdings). Si se excede, error inline y Confirmar deshabilitado; el backend rechaza definitivamente con 409 `INSUFFICIENT_STOCK` (FR-006).
- Quote ausente (404 `QUOTE_NOT_FOUND`), precio nulo/no positivo o error de carga: «Cotización no disponible» y Confirmar deshabilitado; error de red indica que no se pudo consultar, nunca presenta demo como reemplazo.
- `formatCredits(centimes)` utiliza 2 decimales con coma; `formatCentimes` agrupa millares con punto. `quantity * price` sigue en centésimas; ejemplo `5 × 100 = 500 centésimas = 5,00 créditos`.
- `formatQuoteCalculatedAt(calculatedAt)` muestra la fecha/hora reales del cálculo con locale `es-AR`. Nunca usar `Date.now()` ni la `date` demo. Si no se dispone del instante, no se fabrica uno: se muestra estado indisponible.
- Error funcional: `useMarketSellPage` mapea `INVALID_REQUEST`, `INVALID_QUANTITY`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `SUPERUSER_CANNOT_SELL`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INTERNAL_ERROR` a mensajes seguros de [contrato](contracts/publish-token-sales-frontend.md), usando los helpers públicos de inspección de `infrastructure/http`. Service gestiona contratos HTTP y Mapper y propaga errores, sin traducirlos a mensajes de UI. `401` deja actuar al mecanismo de sesión.

## Claves e invalidación

`marketQueryKeys.currentQuote(id) = ['market', 'current-quote', id]` (definido en SPEC-019, reusado).

`portfolioQueryKeys.summary = ['portfolio', 'summary']`  
`portfolioQueryKeys.holdings(status, page, pageSize) = ['portfolio', 'holdings', status, page, pageSize]` (definido en SPEC-019, reusado desde `portfolio/index.ts`).

Tras el POST exitoso de publicación:
- `market` invalida `marketQueryKeys.currentQuote(id)` del jugador publicado.
- `app`, importando `portfolioQueryKeys` desde `features/portfolio/index.ts`, invalida `summary` y el prefijo de todas las `holdings(...)` tras éxito mediante `onSellSuccess(playerId)`. Para sincronizar al abrir el modal y después de un POST fallido con sesión activa, `market` llama `onRefreshHoldings(playerId)` y `app` invalida el prefijo `holdings(...)`; no se dispara para 401/redirección. No copiar literales de key ni importar portfolio desde market.
- Nunca invalidar `playersQueryKeys.tokenQuote` (demo).

Estado local del modal: cerrado / abierto (cantidad inicial `"1"`, precio e importe estimados si hay cotización; sin vencimiento definitivo) / revalidando cotización al confirmar / enviando / éxito (usa `unitPrice`, `totalAmount` y `expiresAt` devueltos por POST y permanece visible hasta navegación o refresh, FR-F09) / error (permanece abierto para reintentar explícitamente, FR-F10). Una protección síncrona en el submit y el estado de la mutación impiden solicitudes duplicadas (CA-15, FR-F08). Abrir/cerrar no remonta el mercado: preserva scroll, página, filtros y pestaña; no requiere crear controles de filtro. El foco retorna al disparador existente salvo navegación a login.

Revalidación de precio: al activar Confirmar, forzar refetch de `GET /api/players/{id}/quotes/current`; exigir estado disponible y precio positivo. Comparar el precio devuelto con el que se mostró/revisó. Si difiere, actualizar estimación, bloquear POST y exigir nueva acción expresa de confirmación. Si coincide, enviar POST. Una variación aún puede ocurrir entre GET y POST; el backend aplica el precio autoritativo y la respuesta POST provee los datos definitivos. No se afirma que el cliente pueda eliminar esa carrera.

## Detalles específicos de venta (diferencia vs compra SPEC-019)

- El POST es a `/orders/sell` (no `/orders/buy`).
- La respuesta incluye `listingId`, `status` ("ACTIVE"), `createdAt`, `expiresAt` (+24h) en lugar de `transactionId`, `executedAt`.
- El precio no es editable por el usuario; se congela en la publicación (FR-F03, FR-007, FR-008).
- No hay `listingId` de entrada (el backend asigna la publicación).
- La confirmación de éxito permanece en el modal hasta navegación/refresh (FR-F09), a diferencia de la compra que auto-cierra a los 2 s.

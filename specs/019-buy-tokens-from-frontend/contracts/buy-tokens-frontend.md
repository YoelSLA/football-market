# Contratos de compra consumidos por Market

**Fuentes autoritativas**: contrato de compra de [SPEC-010](../../010-buy-tokens/contracts/buy-tokens.md) y publicaciones de [SPEC-011](../../011-sell-player-tokens/contracts/listings.md).

## Compra oficial

`POST /api/orders/buy`, autenticado.

```ts
interface OrderBuyRequestDTO {
  playerId: number;
  quantity: number;
}

interface OrderBuyResponseDTO {
  transactionId: number;
  playerId: number;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  executedAt: string;
  listingId?: number;
}
```

- `quantity` se construye como número entero 1–100; nunca se envía el string del formulario.
- No se envían `listingId`, precio, usuario ni datos de presentación.
- La cotización embebida del catálogo solo estima. La respuesta POST es la fuente del precio y total ejecutados.
- El Mapper exige enteros positivos, total consistente e instante ISO válido.
- La guarda del handler repite autenticación, jugador cotizado, cantidad válida, ausencia de envío y ausencia de éxito.

### Errores oficiales

| Código | Mensaje o acción segura |
| --- | --- |
| `INVALID_REQUEST` | Solicitud inválida; revisar datos. |
| `INVALID_QUANTITY` | Cantidad inválida. |
| `INSUFFICIENT_CREDITS` | Créditos insuficientes. |
| `INSUFFICIENT_STOCK` | No hay tokens disponibles para el jugador. |
| `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE` | Cotización no disponible. |
| `SUPERUSER_CANNOT_BUY` | Administradores no pueden comprar. |
| `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE` | Jugador no disponible. |
| `401` | Gestión global de sesión; no restaurar intención. |
| `INTERNAL_ERROR`, 500, red, desconocido | Error genérico sin detalles técnicos; no afirmar éxito. |

### Éxito e invalidación

El resultado se combina con el nombre del jugador solo para presentación. Se invalidan:

1. `marketQueryKeys.officialAll()`;
2. `authQueryKeys.all`;
3. `portfolioQueryKeys.summary`;
4. todas las holdings mediante su prefijo público.

El modal puede cerrarse por acción del usuario o automáticamente dos segundos después del éxito.

## Lectura de publicaciones P2P

`GET /api/listings`, autenticado y sin parámetros.

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

interface TokenListingsDTO {
  listings: TokenListingDTO[];
}
```

Se conserva la validación actual de enteros positivos, total consistente, estado activo y fechas ISO. `sellerId` no se presenta. Catálogo y detalle comparten `marketQueryKeys.listings()`.

## Compra P2P

`POST /api/orders/buy`, autenticado.

```ts
interface BuyListingRequestDTO {
  listingId: number;
}
```

No se envían jugador, cantidad ni precio. La respuesta reutiliza `OrderBuyResponseDTO` y debe devolver el mismo `listingId`.

| Código | Comportamiento consumidor vigente |
| --- | --- |
| `LISTING_NOT_AVAILABLE`, `LISTING_NOT_FOUND` | Informar indisponibilidad y refrescar publicaciones. |
| `CANNOT_BUY_OWN_LISTING` | Rechazo defensivo y refetch. |
| `INSUFFICIENT_CREDITS` | Informar saldo insuficiente; conservar diálogo. |
| `INSUFFICIENT_STOCK` | Informar lote no liquidable y refrescar. |
| `SUPERUSER_CANNOT_BUY`, `INVALID_REQUEST` | Rechazo seguro sin datos sensibles. |
| `INTERNAL_ERROR`, 500 | Error genérico; no afirmar resultado. |
| Red sin respuesta | Estado incierto; no reintentar automáticamente. |
| `401` | Gestión global de sesión. |

El catálogo P2P sincroniza `search`, `league`, `position`, `sort` y `page` con `/market/p2p`; son parámetros exclusivos del navegador y NO se envían a `GET /api/listings`. `search` se publica después de 300 ms de debounce; `league`, `position` y `sort` se actualizan inmediatamente, y cualquier cambio de filtro u orden fija `page=0`. `player=<id>` selecciona el detalle completo usando la colección ya recibida. Abrir o cerrar el detalle modifica únicamente `player`, conserva los demás parámetros y permite reconstrucción por recarga y Back/Forward.

El éxito P2P invalida listings, usuario actual, resumen y holdings. No existe autocierre obligatorio. Un fallo de red sin respuesta concluyente no se reintenta automáticamente ni se interpreta como éxito o rechazo.

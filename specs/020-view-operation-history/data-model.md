# Modelo de datos: Historial personal de operaciones (SPEC-020)

## 1. Alcance

La feature no agrega persistencia. Proyecta datos existentes de `MarketTransaction` como una vista
personal de solo lectura. La fuente efectiva del esquema sigue siendo
`backend/src/main/resources/db/migration/`.

## 2. Entidades y relaciones existentes

### MarketTransaction

Registro confirmado y append-only que constituye la fuente de verdad.

| Campo usado | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Identificador de la operación; desempate descendente |
| `buyer` | User | Obligatorio; determina `BUY` para esa identidad |
| `seller` | User | Obligatorio; determina `SELL`. SPEC-010 y SPEC-011 garantizan que sea distinto del comprador |
| `player` | Player | Obligatorio; aporta identidad y nombre, que es inmutable |
| `quantity` | int | Positivo |
| `unitPrice` | long | Positivo, en centésimas de crédito |
| `totalAmount` | long | `quantity × unitPrice`, en centésimas |
| `executedAt` | OffsetDateTime | Instante confirmado, no nulo |

Relaciones: muchas transacciones pueden referir al mismo comprador, vendedor o jugador. Cada
transacción tiene exactamente un participante por rol y aparece una sola vez en la consulta de cada
participante.

### User

La identidad visible se resuelve por el subject del JWT y luego por el correo persistido. El cliente
no aporta `userId`. Ningún dato de otro usuario forma parte de la respuesta.

### Player

Entidad referida por la transacción. La vista usa `id` y `name`; el nombre es inmutable y no
requiere una copia histórica. No necesita equipo, liga, cotización ni estado activo.

## 3. Vista OperationHistoryEntry

Record de Model no persistido que separa el resultado del caso de uso del DTO HTTP.

| Campo | Tipo | Validación / derivación |
| --- | --- | --- |
| `transactionId` | Long | `MarketTransaction.id`, no nulo |
| `type` | enum/string | `BUY` si el usuario es comprador; `SELL` si es vendedor |
| `playerId` | Long | `MarketTransaction.player.id`, no nulo |
| `playerName` | String | Nombre persistido del jugador, no nulo |
| `quantity` | int | Copia exacta, mayor que cero |
| `unitPrice` | long | Copia exacta en centésimas, mayor que cero |
| `totalAmount` | long | Copia exacta en centésimas |
| `executedAt` | OffsetDateTime | Copia exacta, serializada en ISO-8601 |

## 4. Reglas de colección

1. Incluye solo filas donde el usuario es comprador o vendedor.
2. Cada `transactionId` aparece una vez.
3. Se ordena por `executedAt` descendente y luego `transactionId` descendente.
4. Sin filas, la representación es `[]`, no `null` ni error.
5. La consulta no recalcula valores ni lee cotizaciones actuales.

## 5. Transiciones de estado

No aplica. La consulta no crea ni cambia estados. `MarketTransaction` ya representa una operación
confirmada y permanece append-only.

## 6. Cambios de esquema

Ninguno. Los índices existentes cubren `(buyer_id, executed_at DESC, id DESC)` y
`(seller_id, executed_at DESC, id DESC)`; no se planifica migración.

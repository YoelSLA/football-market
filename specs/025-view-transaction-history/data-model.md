# Data Model: Historial personal de transacciones

## Transporte y Model frontend

`OperationHistoryResponseDTO` y `TransactionHistory` contienen:

- `summary`: conteos, tokens y volúmenes de compras/ventas del conjunto filtrado.
- `transactions`: página de `TransactionHistoryEntry`.
- `pagination`: `page`, `size`, `totalElements`, `totalPages`.

Cada entrada contiene `transactionId`, `TransactionType`, `player`, `quantity`, `unitPrice`,
`totalAmount` y `executedAt`. `TransactionType` es enum de dominio frontend; el DTO conserva los
literales JSON `BUY|SELL`.

`player` contiene `id`, `name`, `imageUrl`, `fallbackImageUrl`, `team` y `league`. Sus datos son el
perfil actual. Las imágenes admiten `null`; las identidades y nombres relacionados son obligatorios.

## Criterios

`TransactionHistoryQueryParams`:

- `page: number`
- `size: number`
- `search?: string`
- `type?: TransactionType`
- `executedFrom?: string`
- `executedBefore?: string`
- `sort: TransactionHistorySort`

`TransactionHistorySort` admite `NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`.

La URL es la fuente de verdad. Valores ausentes usan defaults; valores no reconocidos se
normalizan a defaults antes de llamar al backend. Cambiar criterios distintos de página restablece
`page=0`.

## Semántica histórica

Son históricos: identidad de transacción, rol del usuario, cantidad, precio, importe e instante.
Son actuales: nombre e imágenes del jugador y equipo/liga relacionados. No se agregan snapshots ni
campos persistidos.

## Estados UI

- `loading`: primera página pendiente, sin contador cero.
- `error`: error anunciable y reintento.
- `empty`: `totalElements=0` sin criterios activos; ofrece ir al mercado.
- `no-results`: `totalElements=0` con criterios activos; ofrece limpiar.
- `data`: indicadores, tabla y paginación.

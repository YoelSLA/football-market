# Contrato HTTP: Precio opcional de publicación P2P — SPEC-026

## `POST /api/orders/sell`

Se conserva la ruta y el comportamiento de publicación definidos por
[SPEC-011](../../011-sell-player-tokens/contracts/listings.md). Se agrega al body el campo
opcional `unitPrice`.

```json
{
  "playerId": 7821,
  "quantity": 5,
  "unitPrice": 85
}
```

| Campo | Tipo | Requerido | Regla |
|---|---|---|---|
| `playerId` | entero Long | Sí | Jugador existente y visible. |
| `quantity` | entero | Sí | Entero entre 1 y 100. |
| `unitPrice` | entero Long | No | Precio unitario en centésimas. Si se omite, se usa la cotización vigente; si se informa, debe ser positivo y estar entre 80% y 120%, inclusive, de esa cotización. Sin coerción decimal o textual. |

El response `201 Created` mantiene su estructura vigente. `unitPrice` informa el precio acordado
(solicitado o predeterminado) y `totalAmount` la cantidad multiplicada exactamente por ese precio.
El precio y el importe quedan congelados en la publicación.

Una cotización ausente o no positiva rechaza la publicación sin crear ni reservar recursos. Un precio
explícito fuera de banda se rechaza con `409 INVALID_LISTING_PRICE`. La validación de banda no
redondea la cotización ni sus límites.

`unitPrice` acepta exclusivamente literales enteros JSON representables como `Long`, sin coerción
desde decimales, exponentes, strings u otros tipos. Una representación estructural inválida responde
`400 INVALID_REQUEST`; un entero no positivo responde `409 INVALID_LISTING_PRICE`.

## Compatibilidad de compra

`POST /api/orders/buy` conserva su contrato. La compra P2P liquida con `unitPrice` y `totalAmount`
almacenados en la publicación, sin volver a leer el precio vigente para reprecificarla. El precio no
lo elige el comprador.

## Errores

Se mantienen los errores existentes de publicación (`INVALID_REQUEST`, `INVALID_QUANTITY`,
`SUPERUSER_CANNOT_SELL`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INSUFFICIENT_STOCK`,
`QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `INTERNAL_ERROR`) y se añade
`INVALID_LISTING_PRICE` cuando `unitPrice` se indica como un entero menor o igual a cero, o cuando
su valor positivo queda fuera del intervalo permitido. Todos los enteros no positivos usan el mismo
código, sin distinguir entre cero y valores negativos. Las representaciones estructuralmente inválidas
(por ejemplo, un valor decimal o textual) se rechazan con `INVALID_REQUEST`.

Los rechazos no crean publicación, no reservan tokens y no alteran cuentas o transacciones.

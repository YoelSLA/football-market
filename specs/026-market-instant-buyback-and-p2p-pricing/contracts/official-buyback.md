# Contrato HTTP: Venta oficial inmediata — SPEC-026

## `POST /api/orders/sell-official`

Vende tokens libres del usuario autenticado al superusuario. A diferencia de
`POST /api/orders/sell`, esta operación transfiere tokens y créditos y crea un registro financiero.

Requiere Bearer JWT. El usuario y su rol se obtienen de la sesión y la persistencia; no se aceptan
identificadores de comprador o vendedor en el body.

### Request

```http
POST /api/orders/sell-official
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "playerId": 7821,
  "quantity": 5
}
```

| Campo | Tipo | Requerido | Regla |
|---|---|---|---|
| `playerId` | entero Long | Sí | Jugador existente y visible. |
| `quantity` | entero | Sí | Entero entre 1 y 100 inclusive, sin coerción, redondeo ni truncamiento. |

### Response `200 OK`

```json
{
  "transactionId": 12345,
  "buyerId": 1,
  "sellerId": 42,
  "playerId": 7821,
  "quantity": 5,
  "quotePrice": 100,
  "discountPercentage": 10,
  "unitPrice": 90,
  "totalAmount": 450,
  "executedAt": "2026-11-22T10:30:00Z"
}
```

Todos los importes y precios son centésimas de crédito. `discountPercentage` es la comisión
porcentual aplicada (persistida como porcentaje descontado); el nombre se conserva por
compatibilidad. Admite valores enteros de 10 a 99 inclusive y su valor predeterminado es 10.
`unitPrice` es el precio neto por token después de la comisión y del redondeo
half-up; `totalAmount` es `quantity * unitPrice`. Si el precio neto redondeado es cero, se rechaza
la venta sin cambios. El comprobante identifica partes, jugador, cotización, comisión, precio neto
e importe transferido.

### Errores controlados

| Estado | Código | Causa |
|---|---|---|
| 400 | `INVALID_REQUEST` | Body/campos ausentes o con representación estructural no válida. |
| 400 | `INVALID_QUANTITY` | Entero fuera de 1–100. |
| 401 | — | Sesión ausente, inválida o vencida. |
| 403 | `SUPERUSER_CANNOT_SELL` | Solicitante con rol SUPERUSER. |
| 404 | `PLAYER_NOT_FOUND` / `PLAYER_NOT_VISIBLE` | Jugador ausente o no visible. |
| 409 | `QUOTE_UNAVAILABLE` / `INVALID_QUOTE_PRICE` | Cotización ausente/no positiva o precio neto redondeado a cero. |
| 409 | `INSUFFICIENT_STOCK` | Tenencia libre menor a la solicitada, excluidas reservas P2P. |
| 409 | `INSUFFICIENT_CREDITS` | El superusuario no dispone de saldo suficiente para pagar. |
| 500 | `INTERNAL_ERROR` | Fallo de cálculo, persistencia o liquidación; rollback completo. |

Un rechazo no cambia créditos, tenencias ni registros financieros. Los errores controlados usan el
`ErrorResponseDTO` global. La respuesta 401 corresponde a la cadena de seguridad existente.

### Atomicidad y auditoría

El saldo del superusuario se debita; el saldo del vendedor se acredita; los tokens pasan del
vendedor al superusuario; y la transacción se registra en una única transacción de persistencia.
La transacción conserva cotización, comisión, neto, total, partes y snapshots financieros
anteriores/posteriores. Si falla cualquiera de esos pasos, no se confirma ninguno.

La venta oficial no consume ni cancela publicaciones del usuario: tokens reservados en publicaciones
P2P activas no forman parte de su tenencia libre.

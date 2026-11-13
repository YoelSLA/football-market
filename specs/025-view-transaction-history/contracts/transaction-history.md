# Contract: Historial personal de transacciones

## `GET /api/users/me/transactions`

Lectura autenticada y paginada. La identidad procede exclusivamente del JWT.

### Query parameters

| Nombre | Tipo | Default | Regla |
| --- | --- | --- | --- |
| `page` | integer | `0` | Mínimo 0 |
| `size` | integer | `20` | Entre 1 y 100 |
| `search` | string | — | Nombre actual normalizado del jugador |
| `type` | string | — | `BUY` o `SELL` |
| `executedFrom` | date-time | — | Inclusivo |
| `executedBefore` | date-time | — | Exclusivo y posterior a `executedFrom` |
| `sort` | string | `NEWEST` | `NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC` |

### `200 OK`

```json
{
  "summary": {
    "totalTransactions": 124,
    "buyTransactions": 76,
    "sellTransactions": 48,
    "boughtTokens": 231,
    "soldTokens": 109,
    "purchasedVolume": 1245000,
    "soldVolume": 892000
  },
  "transactions": [
    {
      "transactionId": 92,
      "type": "BUY",
      "player": {
        "id": 41,
        "name": "Lionel Messi",
        "imageUrl": "https://example.test/player-cutout.png",
        "fallbackImageUrl": "https://example.test/player-thumbnail.png",
        "team": { "id": 17, "name": "Inter Miami CF" },
        "league": { "id": 8, "name": "Major League Soccer" }
      },
      "quantity": 5,
      "unitPrice": 125,
      "totalAmount": 625,
      "executedAt": "2026-11-10T14:30:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 124,
    "totalPages": 7
  }
}
```

`quantity`, `unitPrice`, `totalAmount` y `executedAt` proceden del registro confirmado. El perfil
anidado se resuelve desde el catálogo actual: no representa una fotografía histórica. Las imágenes
son nullable; jugador, equipo y liga son obligatorios en una respuesta exitosa.

`summary` y `pagination.totalElements` describen todo el conjunto filtrado. Una página fuera de
rango conserva esos metadatos y devuelve `transactions: []`.

### Orden

- `NEWEST`: `executedAt DESC, transactionId DESC`.
- `OLDEST`: `executedAt ASC, transactionId ASC`.
- `AMOUNT_DESC`: `totalAmount DESC, executedAt DESC, transactionId DESC`.
- `AMOUNT_ASC`: `totalAmount ASC, executedAt DESC, transactionId DESC`.

### Errores

| Estado | Código | Caso |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER_TYPE` | página, tamaño, tipo, orden o date-time inválido |
| 400 | `INVALID_DATE_RANGE` | inicio igual o posterior al final |
| 401 | — | JWT ausente, inválido, vencido o sin usuario persistido |
| 500 | `INTERNAL_ERROR` | fallo interno sin datos parciales |

## Navegación

| Acción | Destino |
| --- | --- |
| `Cartera > Historial de transacciones` | `/portfolio/transactions` |
| Estado vacío > `Ir al mercado` | `/market/buy` |
| Nombre o imagen del jugador | Sin navegación |

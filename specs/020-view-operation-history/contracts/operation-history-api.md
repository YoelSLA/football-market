# Contrato HTTP: Historial personal de operaciones

**Endpoint**: `GET /api/users/me/transactions`
**Estado**: Contrato backend vigente del sistema.
**Referencias normativas**: [spec.md](../spec.md), [plan.md](../plan.md) y [data-model.md](../data-model.md).

---

## 1. Solicitud

```http
GET /api/users/me/transactions?page=0&size=20&type=BUY&sort=NEWEST
Authorization: Bearer <JWT>
```

### Parámetros de consulta (Query parameters)

| Parámetro | Tipo | Default | Reglas |
| --- | --- | --- | --- |
| `page` | integer | `0` | Índice de página basado en cero; `page >= 0`. |
| `size` | integer | `20` | Tamaño de página; `1 <= size <= 100`. |
| `search` | string | `null` | Texto de búsqueda comparado con el nombre actual normalizado del jugador. |
| `type` | string | `null` | Filtro por tipo de operación: `BUY` o `SELL`. |
| `executedFrom` | string (ISO-8601) | `null` | Instante temporal inicial inclusivo con offset. |
| `executedBefore` | string (ISO-8601) | `null` | Instante temporal final exclusivo con offset; debe ser estrictamente posterior a `executedFrom`. |
| `sort` | string | `NEWEST` | Valores permitidos: `NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`. |

---

## 2. Respuesta 200 — Éxito

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
        "team": {
          "id": 17,
          "name": "Inter Miami CF"
        },
        "league": {
          "id": 8,
          "name": "Major League Soccer"
        }
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

### Reglas de campos de respuesta

- **`summary`**: Métricas agregadas sobre todo el conjunto filtrado. Los volúmenes se expresan en centésimas de crédito y no representan rentabilidad ni ganancias.
- **`transactions`**: Subconjunto paginado de transacciones. Si no hay transacciones o filtros sin coincidencias, devuelve array vacío `[]`.
- **`transactions[].type`**: `BUY` si el usuario de la sesión fue comprador; `SELL` si fue vendedor.
- **`transactions[].player`**: Perfil actual del jugador en el catálogo; `imageUrl` y `fallbackImageUrl` pueden ser `null`.
- **`transactions[].quantity` / `unitPrice` / `totalAmount` / `executedAt`**: Valores inmutables confirmados.
- **`pagination`**: Información de paginación server-side. Si `page` excede las páginas reales, conserva `totalElements` y `totalPages` válidos devolviendo `transactions: []`.

---

## 3. Criterios de ordenamiento determinista

Todos los criterios garantizan resultados reproducibles sin duplicación ni salto de filas entre páginas:

| Criterio | Clave primaria de orden | Desempate determinista |
| --- | --- | --- |
| `NEWEST` (default) | `executedAt DESC` | `transactionId DESC` |
| `OLDEST` | `executedAt ASC` | `transactionId ASC` |
| `AMOUNT_DESC` | `totalAmount DESC` | `executedAt DESC, transactionId DESC` |
| `AMOUNT_ASC` | `totalAmount ASC` | `executedAt DESC, transactionId DESC` |

---

## 4. Respuestas de error

| Código HTTP | `code` en cuerpo | Causa |
| --- | --- | --- |
| **400 Bad Request** | `INVALID_PARAMETER_TYPE` | Formato o rango inválido en `page`, `size`, `type` o `sort`. |
| **400 Bad Request** | `INVALID_DATE_RANGE` | `executedFrom` es igual o posterior a `executedBefore`. |
| **401 Unauthorized** | — | Token JWT ausente, inválido, vencido o con subject no persistido en la base de datos. |
| **500 Server Error** | `INTERNAL_ERROR` | Fallo interno en la ejecución; no expone información parcial. |

---

## 5. Seguridad y transaccionalidad

- La ruta requiere autenticación obligatoria mediante token Bearer JWT.
- La identidad se deriva exclusivamente del token; no se permite suplantación de usuario.
- La operación es transaccional y de solo lectura (`@Transactional(readOnly = true)`).

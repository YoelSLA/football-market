# Contrato de API: Gestión de órdenes de venta personales (Backend + Frontend)

**Funcionalidad**: SPEC-030 — Mis órdenes  
**Controlador backend**: [`ListingController`](../../../backend/src/main/java/footballmarket/features/market/presentation/ListingController.java)  
**Endpoints consumidos**:
- `GET /api/listings/mine`: Listar de forma paginada las publicaciones de venta del usuario autenticado con métricas consolidadas.
- `POST /api/listings/{id}/cancel`: Cancelar una publicación de venta activa propia.

---

## 1. `GET /api/listings/mine`

Recupera de forma paginada y ordenada las publicaciones creadas por el usuario autenticado (extraído exclusivamente del token JWT) junto con el resumen métrico global de sus órdenes.

### Parámetros de consulta (Query Params)

| Parámetro | Tipo | Requerido | Default | Valores admitidos / Formato | Descripción |
|---|:---:|:---:|:---:|---|---|
| `page` | Integer | No | `0` | Entero no negativo (`>= 0`) | Índice de página base cero. |
| `size` | Integer | No | `10` | Entero positivo (`1` a `100`) | Cantidad de elementos por página. |
| `sort` | String | No | `NEWEST` | `NEWEST`, `OLDEST`, `PRICE_DESC`, `PRICE_ASC` | Criterio de ordenamiento. Predeterminado: *Más recientes* (`createdAt DESC, id DESC`). |
| `status` | String | No | *(todos)* | `ACTIVE`, `SOLD`, `CANCELLED`, `EXPIRED` | Filtrado estricto por estado efectivo. Si se omite, devuelve todas las órdenes en cualquier estado. Sensible a mayúsculas; otro valor produce 400. |

### Respuestas HTTP

#### `200 OK`
```json
{
  "summary": {
    "activeOrders": 5,
    "activeTokens": 32,
    "activeValue": 4850,
    "soldOrders": 14,
    "soldTokens": 84,
    "soldValue": 12450,
    "cancelledOrders": 3,
    "expiredOrders": 2,
    "totalOrders": 24
  },
  "listings": [
    {
      "listingId": 104,
      "sellerId": 12,
      "playerId": 45,
      "quantity": 5,
      "unitPrice": 150,
      "totalAmount": 750,
      "status": "ACTIVE",
      "createdAt": "2026-11-19T14:10:00Z",
      "expiresAt": "2026-11-20T14:10:00Z"
    },
    {
      "listingId": 98,
      "sellerId": 12,
      "playerId": 22,
      "quantity": 10,
      "unitPrice": 220,
      "totalAmount": 2200,
      "status": "ACTIVE",
      "createdAt": "2026-11-19T10:30:00Z",
      "expiresAt": "2026-11-20T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 24,
    "totalPages": 3
  }
}
```

*Nota contractual:*
- Los importes (`unitPrice`, `totalAmount`, `activeValue`, `soldValue`) vienen expresados en centésimas de crédito. `150` centésimas = `1,50` créditos.
- La colección `listings` conserva compatibilidad y representa la página actual solicitada.
- `summary` calcula las métricas consolidadas del usuario en base a todas sus publicaciones, independientemente de la página actual.

#### `400 Bad Request`
Ocurre si el valor del parámetro `status` no coincide exactamente con uno de los estados permitidos, o si los parámetros de paginación/orden son inválidos:
```json
{
  "code": "INVALID_PARAMETER_TYPE",
  "message": "Parámetro desconocido, vacío o con formato inválido."
}
```

#### `401 Unauthorized`
Sesión ausente, inválida o expirada.

---

## 2. `POST /api/listings/{id}/cancel`

Cancela una publicación de venta propia que se encuentra actualmente activa y vigente.

### Parámetros de ruta (Path Params)

| Parámetro | Tipo | Descripción |
|---|:---:|---|
| `id` | Long (entero positivo) | Identificador de la publicación (`listingId`). |

### Respuestas HTTP

#### `200 OK`
Devuelve el detalle de la publicación cancelada con `status: "CANCELLED"` y `closedAt`:
```json
{
  "listingId": 104,
  "sellerId": 12,
  "playerId": 45,
  "quantity": 5,
  "unitPrice": 150,
  "totalAmount": 750,
  "status": "CANCELLED",
  "createdAt": "2026-11-19T14:10:00Z",
  "expiresAt": "2026-11-20T14:10:00Z",
  "closedAt": "2026-11-19T14:35:10Z"
}
```

#### `401 Unauthorized`
Sesión ausente o inválida.

#### `403 Forbidden`
```json
{
  "code": "ACCESS_DENIED",
  "message": "No posee autorización para cancelar una publicación ajena."
}
```

#### `404 Not Found`
```json
{
  "code": "LISTING_NOT_FOUND",
  "message": "Publicación inexistente."
}
```

#### `409 Conflict`
Ocurre si la publicación ya no está disponible para ser cancelada (por haber sido vendida previamente, cancelada con anterioridad o por haber alcanzado su fecha de expiración):
```json
{
  "code": "LISTING_NOT_AVAILABLE",
  "message": "La publicación ya no se encuentra activa para ser cancelada."
}
```

---

## 3. Enriquecimiento de datos en el Frontend

Dado que `TokenListingSummaryResponseDTO` expone `playerId`, el frontend cruza este identificador con la caché del catálogo de jugadores (`useAllPlayersQuery` / `GET /api/players`) para obtener:
- `name`: Nombre completo del futbolista.
- `team`: Nombre y escudo del equipo.
- `league`: Nombre de la liga.
- `imageUrl`: Retrato o foto oficial del jugador.
- `position`: Posición táctica normalizada.

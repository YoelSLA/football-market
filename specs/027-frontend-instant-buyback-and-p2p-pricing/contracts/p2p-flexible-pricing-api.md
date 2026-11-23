# Contrato de Interfaz: Publicación P2P con Precio Flexible

**Protocolo**: HTTP / REST  
**Seguridad**: Bearer JWT (`authenticated: true`)  
**Contexto**: Invocado desde `SellTokenModal` en modalidad *"Publicación en Comunidad P2P"*.

---

## Endpoint: Crear Publicación en Comunidad P2P

### `POST /api/listings`

Publica tokens de un jugador para su compra por otros usuarios en el mercado secundario P2P, permitiendo opcionalmente especificar un precio unitario acordado dentro de la banda de $[-20\%, +20\%]$ respecto a la cotización oficial vigente.

### Request

- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Body (`CreateListingRequestDTO`)**:
```json
{
  "playerId": 105,
  "quantity": 5,
  "unitPrice": 90
}
```

#### Parámetros:
- `playerId`: Entero positivo. Jugador visible en el catálogo.
- `quantity`: Entero entre 1 y 100.
- `unitPrice` *(opcional)*: Entero en centésimas de crédito. Si se envía, debe encontrarse en el rango $[P_{\text{oficial}} \times 0.80, P_{\text{oficial}} \times 1.20]$. Si se omite, el backend asigna la cotización oficial vigente ($P_{\text{oficial}}$).

---

### Response

#### 201 Created — Publicación P2P Creada
```json
{
  "listingId": 521,
  "sellerId": 42,
  "playerId": 105,
  "quantity": 5,
  "unitPrice": 90,
  "totalAmount": 450,
  "status": "ACTIVE",
  "createdAt": "2026-11-23T14:00:00Z",
  "expiresAt": "2026-11-24T14:00:00Z"
}
```

- `listingId`: Identificador único de la orden P2P creada.
- `unitPrice`: Precio unitario congelado acordado al publicar (90 centésimas).
- `totalAmount`: Importe total fijado para la orden ($5 \times 90 = 450$ centésimas).
- `status`: Estado inicial `"ACTIVE"`.
- `expiresAt`: Vencimiento automático a las 24 horas.

---

### Errores Posibles

| Código HTTP | Error Code | Causa | Mensaje de UI esperado |
|---|---|---|---|
| `400 Bad Request` | `INVALID_LISTING_PRICE` | El `unitPrice` enviado está fuera del rango permitido ($\pm 20\%$). | *"El precio unitario debe encontrarse entre -20% y +20% de la cotización oficial."* |
| `400 Bad Request` | `INVALID_QUANTITY` | Cantidad inválida ($<1$, $>100$, no entera). | *"Cantidad no válida."* |
| `400 Bad Request` | `INSUFFICIENT_STOCK` | Tokens disponibles insuficientes. | *"Saldo de tokens libres insuficiente."* |
| `409 Conflict` | `QUOTE_UNAVAILABLE` | Cotización no disponible para validar la banda. | *"Cotización de referencia no disponible."* |

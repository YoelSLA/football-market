# Contrato de Interfaz: Venta Inmediata Oficial (Buyback)

**Protocolo**: HTTP / REST  
**Seguridad**: Bearer JWT (`authenticated: true`)  
**Contexto**: Invocado desde `SellTokenModal` en modalidad *"Venta Inmediata Oficial"*.

---

## Endpoint: Ejecutar Venta Inmediata Oficial

### `POST /api/orders/sell-official`

Permite a un usuario autenticado vender tokens disponibles de su cartera directamente al superusuario (la casa), aplicando el spread oficial predeterminado del 10% sobre la cotización vigente.

### Request

- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Body (`OrderSellOfficialRequestDTO`)**:
```json
{
  "playerId": 105,
  "quantity": 5
}
```

#### Validaciones del Request
- `playerId`: Entero positivo. Debe corresponder a un jugador visible y cotizable.
- `quantity`: Entero entre 1 y 100. No puede superar la tenencia disponible no reservada (`availableQuantity`) del usuario.

---

### Response

#### 200 OK — Venta Oficial Exitosa (`OrderSellOfficialResponseDTO`)
```json
{
  "transactionId": 8042,
  "playerId": 105,
  "quantity": 5,
  "officialPrice": 100,
  "discountPercentage": 10,
  "unitPrice": 90,
  "totalAmount": 450,
  "executedAt": "2026-11-23T14:00:00Z"
}
```

- `transactionId`: Identificador único de la transacción registrada.
- `officialPrice`: Cotización oficial vigente al momento de la venta en centésimas (100 = 1.00 crédito).
- `discountPercentage`: Porcentaje de descuento/spread aplicado (10%).
- `unitPrice`: Precio unitario neto por token percibido por el vendedor ($100 \times 0.90 = 90$ centésimas).
- `totalAmount`: Importe neto total en centésimas acreditado al vendedor ($5 \times 90 = 450$ centésimas = 4.50 créditos).
- `executedAt`: Marca temporal ISO-8601 de la ejecución.

---

### Errores Posibles

| Código HTTP | Error Code | Causa | Mensaje de UI esperado |
|---|---|---|---|
| `400 Bad Request` | `INVALID_QUANTITY` | Cantidad $<1$, $>100$ o no entera. | *"La cantidad ingresada no es válida."* |
| `400 Bad Request` | `INSUFFICIENT_STOCK` | La cantidad solicitada supera los tokens libres del usuario. | *"No posees suficientes tokens disponibles para vender."* |
| `403 Forbidden` | `SUPERUSER_CANNOT_SELL` | El superusuario intentó vender tokens oficiales. | *"Los administradores no pueden vender tokens."* |
| `404 Not Found` | `PLAYER_NOT_FOUND` | El jugador no existe en la base de datos. | *"Jugador no encontrado."* |
| `404 Not Found` | `PLAYER_NOT_VISIBLE` | El jugador existe pero está oculto/inactivo. | *"El jugador no se encuentra disponible en el mercado."* |
| `409 Conflict` | `QUOTE_UNAVAILABLE` | No hay cotización oficial vigente para el futbolista. | *"Cotización oficial no disponible en este momento."* |
| `409 Conflict` | `SUPERUSER_INSUFFICIENT_FUNDS` | La tesorería del superusuario no dispone de créditos para liquidar. | *"El mercado oficial no dispone de liquidez momentáneamente. Intenta publicar en P2P."* |

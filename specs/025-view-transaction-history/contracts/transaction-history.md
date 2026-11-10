# Contract: Historial personal de transacciones

## Interfaz HTTP existente

### `GET /api/users/me/transactions`

Lectura autenticada y sin parámetros del historial completo del usuario identificado por el JWT.
SPEC-025 consume este contrato; no lo modifica.

**Request**

- Autenticación: Bearer token mediante `authenticated: true`.
- Path/query params: ninguno.
- Body: ninguno.

**200 OK**

```json
[
  {
    "transactionId": 92,
    "type": "SELL",
    "playerId": 41,
    "playerName": "Joel Robles",
    "quantity": 5,
    "unitPrice": 125,
    "totalAmount": 625,
    "executedAt": "2026-10-24T14:30:00Z"
  }
]
```

- `type`: `BUY | SELL` desde la perspectiva del usuario autenticado.
- Importes: centésimas de crédito.
- Orden: más reciente primero; desempate por `transactionId` descendente.
- Sin operaciones: `[]` con 200.

**Errores**

| Estado | Comportamiento frontend |
| --- | --- |
| `401` | El manejo global invalida/resuelve la sesión; la Page no expone datos. |
| `500` | La Page muestra error anunciable y acción `Reintentar`. |
| Error técnico/no clasificable | Se usa el fallback estable de la feature y se permite reintentar. |

## Contrato de navegación

| Origen/acción | Destino | Regla |
| --- | --- | --- |
| `Cartera > Historial de transacciones` | `/portfolio/transactions` | Única entrada del historial. |
| Estado vacío > `Ir al mercado` | `/market/buy` | Navegación interna al flujo existente. |
| Nombre del jugador | Ninguno | Texto sin interacción. |

El placeholder duplicado de historial bajo `Mercado` se retira al implementar.

## Contrato de UI

| Estado | Contenido obligatorio | Semántica accesible |
| --- | --- | --- |
| Carga | Mensaje de consulta; sin contador cero | `aria-busy="true"`, `role="status"` |
| Error | Mensaje, aclaración de no modificación y `Reintentar` | `role="alert"` |
| Vacío | Explicación e `Ir al mercado` | `role="status"` |
| Datos | Tipo, ID, jugador, cantidad, precio, total, fecha/hora | Colección etiquetada y controles con foco visible |

Presentación por rango:

- escritorio desde 1025 px: seis columnas y scroll interno de filas;
- tablet 769–1024 px: cuatro grupos sin scroll horizontal;
- móvil hasta 768 px: una tarjeta por operación y scroll documental natural.

La fuente normativa de composición, presupuesto vertical y Box Model es
`docs/wireframes/transaction-history.md`.

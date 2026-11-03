# Contrato HTTP: Historial personal de operaciones

**Feature**: SPEC-020 · **Rama**: `feat/020-view-operation-history` · **Fecha**: 2026-10-24

Este documento es la referencia funcional y normativa del endpoint. Las anotaciones OpenAPI, los
DTO y las pruebas funcionales de Controller deben permanecer coherentes con él.

## GET /api/users/me/transactions

Devuelve todas las operaciones confirmadas en las que participó el usuario autenticado.

### Identidad y autorización

- Requiere JWT Bearer válido.
- La identidad se obtiene exclusivamente del subject del JWT.
- No acepta path variable, query parameter ni body para seleccionar usuario.
- Una solicitud sin autenticación válida responde `401` antes de ejecutar el caso de uso.

### Request

Sin body y sin parámetros. Paginación, filtros, exportación, estadísticas y agregaciones no forman
parte del contrato.

### Response 200 OK

Array JSON completo, ordenado por `executedAt` descendente y, en empate, por `transactionId`
descendente.

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

| Campo | Tipo | Regla |
| --- | --- | --- |
| `transactionId` | entero 64 bits | Identificador persistido de la transacción |
| `type` | string | `BUY` si la sesión corresponde al comprador; `SELL` si corresponde al vendedor |
| `playerId` | entero 64 bits | Identificador interno del jugador |
| `playerName` | string | Nombre persistido del jugador |
| `quantity` | entero | Tokens transferidos |
| `unitPrice` | entero 64 bits | Precio histórico por token, en centésimas de crédito |
| `totalAmount` | entero 64 bits | Importe histórico total, en centésimas de crédito |
| `executedAt` | string `date-time` | Instante de confirmación ISO-8601 con offset |

Los precios y el importe se copian del registro confirmado; no se calculan con cotizaciones
vigentes. No se exponen comprador, vendedor, saldos, tenencias ni datos de otra persona.

### Historial vacío

Un usuario autenticado sin operaciones recibe `200 OK` con:

```json
[]
```

### Errores

| Situación | HTTP | Cuerpo |
| --- | --- | --- |
| JWT ausente, inválido o vencido | 401 | Emitido por Spring Security; no se garantiza cuerpo |
| El subject válido no corresponde a un usuario persistido | 401 | La autenticación persistida rechaza la sesión sin revelar historiales |
| Fallo interno inesperado | 500 | `ErrorResponseDTO`, sin datos financieros parciales |

### Solo lectura e idempotencia

La operación se ejecuta en transacción `readOnly`. Repetirla sobre el mismo estado devuelve la misma
colección y no crea, modifica ni elimina transacciones, saldos, tenencias, publicaciones o
cotizaciones.

## Referencias

- [`../spec.md`](../spec.md)
- [`../data-model.md`](../data-model.md)
- [`../research.md`](../research.md)
- `docs/backend/architecture.md` §1.7

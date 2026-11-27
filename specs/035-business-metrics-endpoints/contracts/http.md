# Contrato HTTP — métricas de negocio

## Autenticación

Ambos endpoints requieren autenticación Bearer mediante el mecanismo existente. Cualquier cuenta autenticada tiene acceso; no se requiere un rol adicional. Las solicitudes sin credenciales válidas reciben `401 Unauthorized`.

## `GET /api/business-metrics/historical`

Devuelve agregados calculados desde las filas financieras y publicaciones persistidas hasta el instante de consulta.

## `GET /api/business-metrics/current`

Devuelve agregados acumulados en Micrometer desde el arranque del proceso actual. No reemplaza ni modifica los totales históricos.

## Respuesta `200 OK`

Ambos endpoints devuelven exactamente el mismo objeto JSON:

```json
{
  "purchases": 0,
  "sales": 0,
  "cancelledListings": 0,
  "tokensTransferred": 0,
  "creditsTradedInCents": 0,
  "queriedAt": "2026-11-26T12:00:00Z"
}
```

| Campo | Tipo | Significado |
| --- | --- | --- |
| `purchases` | entero | Operaciones clasificadas como compras. Históricamente, fila con comprador `USER`. |
| `sales` | entero | Operaciones clasificadas como ventas. Históricamente, recompra oficial: comprador `SUPERUSER`, vendedor `USER`. |
| `cancelledListings` | entero | Publicaciones que alcanzaron estado persistido `CANCELLED`; no incluye vencimientos. |
| `tokensTransferred` | número | Suma de tokens transferidos por registros financieros, una vez por fila. |
| `creditsTradedInCents` | entero | Volumen negociado en centésimas de crédito, sumado una vez por fila financiera. |
| `queriedAt` | string | Instante de consulta en ISO 8601, UTC (`Z`). |

Cuando no exista actividad aplicable, los contadores y volúmenes son cero. Las respuestas y métricas no incluyen datos personales ni identificadores individuales.

La clasificación de operaciones es igual en ambas vistas: una operación con comprador `USER` cuenta como compra; una recompra oficial con comprador `SUPERUSER` y vendedor `USER` cuenta como venta. Una liquidación P2P con comprador `USER` cuenta solo como compra, no también como venta.

## Límites

- No se exponen métricas técnicas; estas permanecen separadas en `/actuator/prometheus`.
- No se agregan desgloses por usuario o jugador.
- Una fila financiera nunca se suma más de una vez a tokens o créditos ni se cuenta a la vez como compra y venta.

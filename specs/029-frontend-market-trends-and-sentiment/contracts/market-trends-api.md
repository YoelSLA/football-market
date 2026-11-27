# Contrato HTTP Frontend: Tendencias de Mercado

## `GET /api/market/trends`

Consulta macro de tendencias de mercado, porcentaje de sentimiento (compras vs ventas), sesgo general y ranking de activos más demandados en la ventana de actividad vigente.

### Autenticación
- **Header:** `Authorization: Bearer <JWT>`
- **Permisos:** Cualquier usuario autenticado. Consulta de solo lectura que no muta el estado.

### Parámetros de Consulta
- Sin parámetros en URL. La ventana temporal de análisis (ej. últimas 24 horas) es administrada por la configuración del backend.

### Respuesta `200 OK`

```json
{
  "window": {
    "startsAt": "2026-11-23T13:00:00Z",
    "endsAt": "2026-11-24T13:00:00Z"
  },
  "topHotPlayers": [
    {
      "playerId": 7821,
      "playerName": "Erling Haaland",
      "boughtTokens": 45,
      "buyVolume": 450000
    },
    {
      "playerId": 1045,
      "playerName": "Kylian Mbappé",
      "boughtTokens": 30,
      "buyVolume": 360000
    }
  ],
  "buyTransactionsPercentage": 75.0,
  "sellTransactionsPercentage": 25.0,
  "marketBias": "BULLISH",
  "totalTransactions": 28,
  "totalTradedTokens": 140,
  "totalTradedCredits": 1250000
}
```

### Definición de Campos

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `window.startsAt` | `string` (ISO-8601 UTC) | Inicio de la ventana de análisis. |
| `window.endsAt` | `string` (ISO-8601 UTC) | Fin de la ventana de análisis. |
| `topHotPlayers` | `array` | Lista de hasta 5 futbolistas más demandados, ordenada por tokens comprados descendente. |
| `topHotPlayers[].playerId` | `number` | Identificador único del futbolista. |
| `topHotPlayers[].playerName` | `string` | Nombre del futbolista. |
| `topHotPlayers[].boughtTokens` | `number` | Total de tokens comprados en la ventana. |
| `topHotPlayers[].buyVolume` | `number` | Volumen de compra acumulado en **centésimas de crédito** (el frontend divide por 100). |
| `buyTransactionsPercentage` | `number \| null` | Porcentaje de transacciones de compra sobre el total. Nulo si no hay operaciones. |
| `sellTransactionsPercentage` | `number \| null` | Porcentaje de transacciones de venta sobre el total. Nulo si no hay operaciones. |
| `marketBias` | `string \| null` | Sesgo del mercado: `"BULLISH"`, `"BEARISH"` o `"NEUTRAL"`. Nulo si no hay operaciones. |
| `totalTransactions` | `number` | Conteo total de transacciones en la ventana. |
| `totalTradedTokens` | `number` | Conteo total de tokens operados. |
| `totalTradedCredits` | `number` | Volumen total operado en **centésimas de crédito** (el frontend divide por 100). |

### Códigos de Error

| Código HTTP | Causa | Manejo en Frontend |
| :---: | :--- | :--- |
| `401 Unauthorized` | Token JWT ausente, expirado o inválido. | Redirección automática a login vía interceptor `http`. |
| `500 Internal Error` | Error no controlado en servidor. | Renderizado de `MarketTrendsErrorState` con botón para reintentar. |


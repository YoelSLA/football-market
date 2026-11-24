# Contrato HTTP: Recálculo con demanda de mercado

## `POST /api/quotes/recalculate/market-demand`

Requiere Bearer JWT y autorización vigente para operaciones administrativas del mercado, sin roles nuevos. Es manual, síncrono y poblacional; registra la ejecución con el contrato de cotizaciones existente.

### Request

Sin body. Captura un único `startedAt`; utiliza la misma ventana configurable que `GET /api/market/trends`, con rango `[startedAt - demandWindow, startedAt)`. Usa `variationCap` y `minPriceFloor` configurados para cotizaciones.

### Respuesta `200 OK`

Devuelve el resumen existente de ejecución, añadiendo:

| Campo | Significado |
| --- | --- |
| `recalculationType` | `MARKET_DEMAND_ONLY`. |
| `demandWindow` | Duración usada. |
| `strategy`, `strategyVersion`, `adjustmentFactor` | `null`: no se consultó ni aplicó score deportivo. |

Procesa todos los jugadores elegibles. Para cada jugador calcula `netDemandTokens = boughtTokens - soldTokens` dentro de la ventana; una compra P2P suma compra y no suma venta. Aplica `clamp((netDemandTokens / 100) × 0.50, -0.50, +0.50)` sobre el precio vigente; luego aplica cap y piso. Persiste cotización `MARKET_DEMAND` con precio anterior, demanda individual, componente de mercado y precio final. No consulta scores.

### Errores

| HTTP | Código | Causa |
| --- | --- | --- |
| `401` | — | JWT ausente/inválido o autorización vigente denegada. |
| `409` | `MARKET_NOT_INITIALIZED` | Mercado no inicializado. |
| `500` | `INTERNAL_ERROR` | Fallo general sin detalles internos. |

Factor cero deja el precio sin cambios y registra la ejecución. Si falla la lectura de actividad, no se publica una cotización parcial ni se inventa demanda.

## Compatibilidad de `POST /api/quotes/recalculate`

Conserva request y contrato vigente de SPEC-016. Con score aplicable, combina `sportMultiplier * (1 + marketDemandFactor)`, aplica límite de variación y piso, y audita ambos componentes. Sin score, conserva exactamente el comportamiento existente sin score y no aplica el factor de mercado en esa ejecución. Resúmenes y cotizaciones identifican tipo y componentes separadamente.

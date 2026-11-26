# Modelo de datos: métricas de negocio

No se añaden entidades persistidas ni migraciones. Los siguientes agregados son modelos de respuesta calculados:

| Agregado | Fuente y regla | Representación |
| --- | --- | --- |
| `purchases` | Operaciones con comprador `USER`; en histórico, filas de `market_transactions` con comprador `USER`. Una liquidación P2P se cuenta una vez como compra. | Entero |
| `sales` | Recompras oficiales con comprador `SUPERUSER` y vendedor `USER`; en histórico, filas que cumplen esos roles. | Entero |
| `cancelledListings` | Filas de `token_listings` con `status = CANCELLED`; no incluye `EXPIRED`. | Entero |
| `tokensTransferred` | Suma de `quantity` por fila de `market_transactions`, cada fila una vez. | Número acumulado |
| `creditsTradedInCents` | Suma de `total_amount` por fila financiera, cada fila una vez; unidad: centésimas de crédito. | Entero |
| `queriedAt` | Instante UTC capturado para producir la respuesta. | Fecha-hora ISO 8601 UTC |

La vista histórica consulta los registros persistidos y representa agregados vacíos como cero. La vista actual usa contadores Micrometer de la ejecución: cada transacción confirmada aporta una operación y su volumen; cada transición efectiva a `CANCELLED` aporta una publicación. Las métricas de ejecución son efímeras y se reinician al arrancar otro proceso.

Ambas vistas comparten el mismo esquema de respuesta y clasificación de operaciones. Una liquidación P2P con comprador `USER` cuenta solo como compra, no como venta. No exponen IDs, datos personales, etiquetas de alta cardinalidad ni desglose por usuario/jugador. La consulta no muta filas de negocio ni guarda snapshots.

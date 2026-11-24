# Contrato HTTP: Tendencias del mercado

## `GET /api/market/trends`

Requiere `Authorization: Bearer <JWT>`. La identidad se valida con la autenticación existente. La consulta no modifica datos.

### Ventana

Una duración configurable común a tendencias y recálculos, con valor predeterminado de 24 horas. `endsAt` es el instante capturado al comenzar y `startsAt = endsAt - demandWindow`; se consultan transacciones en el rango `[startsAt, endsAt)`. Instantes ISO-8601 UTC.

### Respuesta `200 OK`

```json
{
  "window": {
    "startsAt": "2026-11-22T12:00:00Z",
    "endsAt": "2026-11-23T12:00:00Z"
  },
  "topHotPlayers": [
    {
      "playerId": 7821,
      "playerName": "Jugador",
      "boughtTokens": 12,
      "buyVolume": 1200
    }
  ],
  "buyTransactionsPercentage": 75.0,
  "sellTransactionsPercentage": 25.0,
  "marketBias": "BULLISH",
  "totalTransactions": 4,
  "totalTradedTokens": 16,
  "totalTradedCredits": 1600
}
```

Importes en centésimas de crédito. Lista de hasta cinco jugadores, ordenada por tokens comprados descendentes, importe de compra descendente y `playerId` ascendente. Solo suma compras, incluida compra P2P.

Cada fila se clasifica una sola vez: comprador USER = compra, incluida P2P; comprador SUPERUSER y vendedor USER = venta oficial. Los porcentajes se calculan sobre el número de filas clasificadas; el sesgo refleja el conteo mayor. No se duplica una fila P2P por sus dos partes.

Con actividad, los porcentajes suman 100 y el sesgo es `BULLISH`, `BEARISH` o `NEUTRAL`. Sin filas, `topHotPlayers` es `[]`, volúmenes y total son cero, porcentajes y `marketBias` son `null`. Actividad con demanda neta cero no es ausencia.

### Errores

| HTTP | Resultado |
| --- | --- |
| `401` | JWT ausente, inválido, vencido o identidad no persistida; no se exponen métricas. |
| `500` | `INTERNAL_ERROR`, sin detalles internos. |

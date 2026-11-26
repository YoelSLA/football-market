# Contrato HTTP Frontend: Desglose de Cotización con Demanda

## `GET /api/players/{playerId}/quotes/current`

Recupera la cotización oficial vigente del jugador especificado, incluyendo los factores concurrentes deportivos y de mercado que la determinaron.

### Autenticación
- **Header:** `Authorization: Bearer <JWT>`
- **Permisos:** Cualquier usuario autenticado.

### Respuesta `200 OK`

```json
{
  "effectiveDate": "2026-11-24",
  "price": 105,
  "previousPrice": 100,
  "multiplier": 1.050000,
  "sportMultiplier": 1.025000,
  "marketDemandFactor": 0.024390,
  "marketMultiplier": 1.024390,
  "marketNetDemandTokens": 5,
  "variationCap": 0.50,
  "minPriceFloor": 10,
  "score": 0.72,
  "strategy": "POSITIONAL_PERFORMANCE",
  "strategyVersion": "v1",
  "sourceValuationDate": "2026-11-23",
  "origin": "RECOMPUTED",
  "calculatedAt": "2026-11-24T03:00:00Z"
}
```

### Interpretación de Factores en Frontend

1. **Recálculo Combinado (`origin === "RECOMPUTED"`):**
   - *Aporte Deportivo:* $(sportMultiplier - 1) \times 100$ $\rightarrow +2.50\%$.
   - *Presión de Mercado:* $marketDemandFactor \times 100$ $\rightarrow +2.44\%$.
   - *Variación Total:* $\frac{price - previousPrice}{previousPrice} \times 100$ $\rightarrow +5.00\%$.
2. **Recálculo Exclusivo de Mercado (`origin === "MARKET_DEMAND"`):**
   - *Aporte Deportivo:* $0.00\%$ (sin nuevo score deportivo).
   - *Presión de Mercado:* $marketDemandFactor \times 100$.
3. **Cotización Inicial (`origin === "INITIAL"`):**
   - Sin factores de variación (precio base de emisión).


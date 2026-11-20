# Contrato de API: Dashboard interactivo y resumen ejecutivo (Backend + Frontend)

**Funcionalidad**: SPEC-031 — Dashboard interactivo  
**Controlador backend**: `footballmarket.features.dashboard.presentation.DashboardController`  
**Seguridad**: `@SecurityRequirement(name = "bearerAuth")` (requiere JWT en header `Authorization: Bearer <token>`)

---

## 1. `GET /api/dashboard/overview`

Retorna el resumen ejecutivo consolidado del usuario autenticado, métricas de cartera, pulso de mercado, top movers, oportunidades de valor, ofertas P2P destacadas y actividad reciente.

### Encabezados de solicitud
| Encabezado | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| `Authorization` | String | Sí | Token JWT de sesión en formato `Bearer <token>` |

### Parámetros de consulta
Ninguno. Toda la información del usuario se extrae del subject del JWT.

### Respuestas HTTP

#### `200 OK`
```json
{
  "financialSummary": {
    "availableCredits": 450000,
    "totalPortfolioValue": 1285000,
    "netWorth": 1735000,
    "totalInvested": 1050000,
    "totalUnrealizedProfitLoss": 235000,
    "totalRealizedProfitLoss": 50000,
    "totalProfitLoss": 285000,
    "totalProfitLossPercentage": 27.14,
    "totalTokens": 85,
    "availableTokens": 70,
    "reservedTokens": 15,
    "valuationComplete": true
  },
  "allocation": {
    "byPosition": [
      {
        "categoryKey": "DELANTERO",
        "label": "Delanteros",
        "totalValue": 720000,
        "percentage": 56.03,
        "tokenCount": 40
      },
      {
        "categoryKey": "MEDIOCAMPISTA",
        "label": "Mediocampistas",
        "totalValue": 340000,
        "percentage": 26.46,
        "tokenCount": 25
      },
      {
        "categoryKey": "DEFENSOR",
        "label": "Defensores",
        "totalValue": 150000,
        "percentage": 11.67,
        "tokenCount": 12
      },
      {
        "categoryKey": "ARQUERO",
        "label": "Arqueros",
        "totalValue": 75000,
        "percentage": 5.84,
        "tokenCount": 8
      }
    ],
    "byLeague": [
      {
        "leagueName": "Premier League",
        "totalValue": 650000,
        "percentage": 50.58,
        "tokenCount": 35
      },
      {
        "leagueName": "La Liga",
        "totalValue": 410000,
        "percentage": 31.91,
        "tokenCount": 30
      },
      {
        "leagueName": "Serie A",
        "totalValue": 225000,
        "percentage": 17.51,
        "tokenCount": 20
      }
    ],
    "topHoldingConcentrationPercentage": 35.20,
    "topHoldingPlayerName": "Erling Haaland"
  },
  "marketPulse": {
    "totalMarketCap": 154200000,
    "volume24h": 3250000,
    "totalTransactionsCount": 1420,
    "activeListingsCount": 48
  },
  "topGainers": [
    {
      "playerId": 101,
      "firstName": "Kylian",
      "lastName": "Mbappé",
      "teamName": "Real Madrid",
      "leagueName": "La Liga",
      "position": "DELANTERO",
      "imageUrl": "https://media.footballmarket.com/players/mbappe.png",
      "currentPrice": 24500,
      "previousPrice": 21000,
      "deltaCredits": 3500,
      "deltaPercentage": 16.67,
      "score": 8.75
    },
    {
      "playerId": 105,
      "firstName": "Lamine",
      "lastName": "Yamal",
      "teamName": "FC Barcelona",
      "leagueName": "La Liga",
      "position": "DELANTERO",
      "imageUrl": "https://media.footballmarket.com/players/yamal.png",
      "currentPrice": 18200,
      "previousPrice": 16000,
      "deltaCredits": 2200,
      "deltaPercentage": 13.75,
      "score": 8.40
    }
  ],
  "topLosers": [
    {
      "playerId": 210,
      "firstName": "Joshua",
      "lastName": "Kimmich",
      "teamName": "Bayern München",
      "leagueName": "Bundesliga",
      "position": "MEDIOCAMPISTA",
      "imageUrl": "https://media.footballmarket.com/players/kimmich.png",
      "currentPrice": 11500,
      "previousPrice": 13000,
      "deltaCredits": -1500,
      "deltaPercentage": -11.54,
      "score": 6.80
    }
  ],
  "valuePicks": [
    {
      "playerId": 305,
      "firstName": "Florian",
      "lastName": "Wirtz",
      "teamName": "Bayer Leverkusen",
      "leagueName": "Bundesliga",
      "position": "MEDIOCAMPISTA",
      "imageUrl": "https://media.footballmarket.com/players/wirtz.png",
      "whoScoredScore": 8.25,
      "currentPrice": 12000,
      "valueRatio": 68.75
    }
  ],
  "bestP2PDeals": [
    {
      "listingId": 84,
      "playerId": 101,
      "playerName": "Kylian Mbappé",
      "teamName": "Real Madrid",
      "leagueName": "La Liga",
      "imageUrl": "https://media.footballmarket.com/players/mbappe.png",
      "availableTokens": 5,
      "p2pUnitPrice": 22000,
      "officialQuotePrice": 24500,
      "discountPercentage": 10.20
    }
  ],
  "recentActivity": [
    {
      "transactionId": 512,
      "type": "BUY",
      "marketType": "OFFICIAL",
      "playerId": 101,
      "playerName": "Kylian Mbappé",
      "quantity": 2,
      "unitPrice": 24500,
      "totalAmount": 49000,
      "executedAt": "2026-11-20T18:15:00Z"
    }
  ],
  "generatedAt": "2026-11-20T20:15:00Z"
}
```

#### `401 Unauthorized`
Sesión vencida, ausente o no autorizada.

---

## 2. `GET /api/dashboard/performance`

Retorna las series históricas temporales para el gráfico de rendimiento y benchmark comparativo.

### Parámetros de consulta
| Parámetro | Tipo | Requerido | Default | Valores | Descripción |
|---|:---:|:---:|:---:|---|---|
| `range` | String | No | `1M` | `1M`, `3M`, `ALL` | Rango temporal de fechas |

### Respuestas HTTP

#### `200 OK`
```json
{
  "dates": [
    "2026-10-20",
    "2026-10-27",
    "2026-11-03",
    "2026-11-10",
    "2026-11-17"
  ],
  "userPortfolioSeries": [
    950000,
    1080000,
    1150000,
    1210000,
    1285000
  ],
  "marketBenchmarkSeries": [
    100.00,
    102.45,
    101.80,
    105.10,
    108.35
  ]
}
```

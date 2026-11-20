# Data Model: Dashboard interactivo y resumen ejecutivo de mercado (SPEC-031)

**Feature Branch**: `feat/031-home-dashboard`
**Fecha**: 2026-11-20
**Spec de referencia**: [`spec.md`](spec.md)

---

## 1. DTOs de Backend (`footballmarket.features.dashboard.presentation.dtos`)

### 1.1. `DashboardOverviewResponseDTO`
Contrato consolidado que alimenta la vista principal del cockpit:

```java
public record DashboardOverviewResponseDTO(
    DashboardFinancialSummaryDTO financialSummary,
    DashboardAllocationDTO allocation,
    DashboardMarketPulseDTO marketPulse,
    List<DashboardPlayerMoverDTO> topGainers,
    List<DashboardPlayerMoverDTO> topLosers,
    List<DashboardValuePickDTO> valuePicks,
    List<DashboardP2PDealDTO> bestP2PDeals,
    List<DashboardRecentActivityDTO> recentActivity,
    OffsetDateTime generatedAt
) {}
```

### 1.2. Subestructuras de Overview

#### `DashboardFinancialSummaryDTO`
*Representa los valores en centésimas de crédito (enteros largos).*
- `long availableCredits`: Saldo líquido de créditos libres del usuario (`user_credits.available_credits`).
- `Long totalPortfolioValue`: Valoración de mercado actual de todas las posiciones abiertas. Nulo si alguna posición carece de cotización vigente.
- `Long netWorth`: Patrimonio neto total (`availableCredits + totalPortfolioValue`).
- `long totalInvested`: Costo histórico de adquisición de las posiciones actualmente abiertas.
- `Long totalUnrealizedProfitLoss`: Ganancia o pérdida no realizada (`totalPortfolioValue - totalInvested`).
- `long totalRealizedProfitLoss`: Ganancia o pérdida realizada acumulada en operaciones de venta cerradas.
- `Long totalProfitLoss`: Suma de realizado y no realizado (`totalRealizedProfitLoss + totalUnrealizedProfitLoss`).
- `BigDecimal totalProfitLossPercentage`: Retorno total sobre capital invertido (en puntos porcentuales con 2 decimales).
- `int totalTokens`: Cantidad total de tokens poseídos en cartera.
- `int availableTokens`: Tokens libres para transferir o vender.
- `int reservedTokens`: Tokens comprometidos en órdenes de venta P2P activas.
- `boolean valuationComplete`: Indica si todas las posiciones abiertas tienen cotización oficial vigente.

#### `DashboardAllocationDTO`
*Distribución de cartera para alimentar el Donut Chart SVG.*
- `List<AllocationItemDTO> byPosition`: Categorizado en `DELANTERO` (ST, CF, RW, LW), `MEDIOCAMPISTA` (CM, CAM, CDM), `DEFENSOR` (CB, LB, RB), `ARQUERO` (GK).
  - Cada item contiene: `String categoryKey`, `String label`, `long totalValue`, `BigDecimal percentage`, `int tokenCount`.
- `List<AllocationItemDTO> byLeague`: Categorizado en las 5 ligas: Premier League, La Liga, Serie A, Bundesliga, Ligue 1.
  - Cada item contiene: `String leagueName`, `long totalValue`, `BigDecimal percentage`, `int tokenCount`.
- `BigDecimal topHoldingConcentrationPercentage`: Porcentaje de la cartera que representa la posición de mayor valor.
- `String topHoldingPlayerName`: Nombre del jugador con mayor peso en cartera.

#### `DashboardMarketPulseDTO`
*Indicadores macro del mercado de Football Market.*
- `long totalMarketCap`: Suma de `(cotización_actual × 100 tokens)` de todos los jugadores cotizables de la plataforma (en centésimas).
- `long volume24h`: Volumen transaccionado en créditos durante las últimas 24 horas (o último recálculo).
- `long totalTransactionsCount`: Cantidad acumulada de operaciones ejecutadas en la historia de la plataforma.
- `int activeListingsCount`: Cantidad total de publicaciones de venta P2P activas en este momento.

#### `DashboardPlayerMoverDTO`
*Top Gainers y Top Losers calculados a partir de `player_quotes`.*
- `Long playerId`: ID del jugador.
- `String firstName`: Nombre.
- `String lastName`: Apellido.
- `String teamName`: Nombre del club.
- `String leagueName`: Nombre de la liga.
- `String position`: Posición táctica normalizada.
- `String imageUrl`: URL de la foto oficial.
- `long currentPrice`: Cotización oficial vigente (en centésimas).
- `Long previousPrice`: Cotización anterior inmediata (en centésimas).
- `Long deltaCredits`: Variación nominal (`currentPrice - previousPrice`).
- `BigDecimal deltaPercentage`: Variación porcentual $\left(\frac{\text{currentPrice} - \text{previousPrice}}{\text{previousPrice}} \times 100\right)$.
- `BigDecimal score`: Score de la última estrategia calculada.

#### `DashboardValuePickDTO`
*Oportunidades de inversión por alta relación rendimiento WhoScored / Precio.*
- `Long playerId`: ID del jugador.
- `String firstName`, `String lastName`, `String teamName`, `String leagueName`, `String position`, `String imageUrl`.
- `BigDecimal whoScoredScore`: Calificación promedio de rendimiento deportivo WhoScored.
- `long currentPrice`: Cotización actual en centésimas.
- `BigDecimal valueRatio`: $\frac{\text{Score}}{\text{Precio / 100}} \times 1000$.

#### `DashboardP2PDealDTO`
*Oportunidades de arbitraje comunitario.*
- `Long listingId`: ID de la orden activa en `token_listings`.
- `Long playerId`: ID del futbolista.
- `String playerName`, `String teamName`, `String leagueName`, `String imageUrl`.
- `int availableTokens`: Cantidad de tokens ofrecidos en el lote.
- `long p2pUnitPrice`: Precio unitario ofertado en el mercado P2P (en centésimas).
- `long officialQuotePrice`: Cotización oficial de referencia vigente (en centésimas).
- `BigDecimal discountPercentage`: Ahorro porcentual $\left(\frac{P_{\text{oficial}} - P_{\text{p2p}}}{P_{\text{oficial}}} \times 100\right)$.

#### `DashboardRecentActivityDTO`
*Timeline de últimas transacciones del usuario.*
- `Long transactionId`: ID en `market_transactions`.
- `String type`: `BUY` o `SELL`.
- `String marketType`: `OFFICIAL` o `P2P`.
- `Long playerId`: ID del futbolista.
- `String playerName`: Nombre del jugador.
- `int quantity`: Cantidad de tokens transaccionados.
- `long unitPrice`: Precio unitario en centésimas.
- `long totalAmount`: Importe total en centésimas.
- `OffsetDateTime executedAt`: Timestamp de ejecución.

---

### 1.3. `DashboardPerformanceResponseDTO`
Contrato para la curva temporal y benchmark (`GET /api/dashboard/performance`):

```java
public record DashboardPerformanceResponseDTO(
    List<LocalDate> dates,
    List<Long> userPortfolioSeries,      // Valoración del usuario en centésimas en cada fecha
    List<BigDecimal> marketBenchmarkSeries // Índice FMI normalizado (base 100) en cada fecha
) {}
```

---

## 2. Modelos de Frontend (`frontend/src/features/dashboard/types/`)

El frontend transforma las centésimas de crédito a números flotantes o cadenas formateadas dividiendo por 100 mediante mappers:

```typescript
export interface FinancialSummary {
  availableCredits: number;
  totalPortfolioValue: number | null;
  netWorth: number | null;
  totalInvested: number;
  totalUnrealizedProfitLoss: number | null;
  totalRealizedProfitLoss: number;
  totalProfitLoss: number | null;
  totalProfitLossPercentage: number | null;
  totalTokens: number;
  availableTokens: number;
  reservedTokens: number;
  valuationComplete: boolean;
}

export interface AllocationItem {
  key: string;
  label: string;
  totalValue: number;
  percentage: number;
  tokenCount: number;
}

export interface PortfolioAllocation {
  byPosition: AllocationItem[];
  byLeague: AllocationItem[];
  topHoldingConcentrationPercentage: number;
  topHoldingPlayerName: string;
}

export interface PlayerMover {
  playerId: number;
  name: string;
  team: string;
  league: string;
  position: string;
  imageUrl?: string;
  currentPrice: number;
  previousPrice: number | null;
  deltaCredits: number | null;
  deltaPercentage: number | null;
  score?: number;
}

export interface ValuePick {
  playerId: number;
  name: string;
  team: string;
  league: string;
  position: string;
  imageUrl?: string;
  score: number;
  price: number;
  valueRatio: number;
}

export interface P2PDeal {
  listingId: number;
  playerId: number;
  playerName: string;
  team: string;
  league: string;
  imageUrl?: string;
  availableTokens: number;
  p2pUnitPrice: number;
  officialQuotePrice: number;
  discountPercentage: number;
}

export interface RecentActivityItem {
  id: number;
  type: "BUY" | "SELL";
  marketType: "OFFICIAL" | "P2P";
  playerId: number;
  playerName: string;
  tokens: number;
  unitPrice: number;
  totalAmount: number;
  executedAt: string;
}

export interface DashboardOverview {
  financialSummary: FinancialSummary;
  allocation: PortfolioAllocation;
  marketPulse: {
    totalMarketCap: number;
    volume24h: number;
    totalTransactionsCount: number;
    activeListingsCount: number;
  };
  topGainers: PlayerMover[];
  topLosers: PlayerMover[];
  valuePicks: ValuePick[];
  bestP2PDeals: P2PDeal[];
  recentActivity: RecentActivityItem[];
}
```


# Data Model: Tablero de tendencias de mercado y demanda comunitaria (SPEC-029)

**Feature**: `specs/029-frontend-market-trends-and-sentiment`
**Fecha**: 2026-11-24
**Objetivo**: Formalizar DTOs de transporte, modelos de dominio enriquecidos, mappers y estados visuales del frontend.

---

## 1. DTOs (Data Transfer Objects)

### 1.1. Tendencias de Mercado (`frontend/src/features/market/types/dtos.ts`)

```typescript
export type MarketBiasDTO = "BULLISH" | "BEARISH" | "NEUTRAL";

export interface MarketTrendsWindowDTO {
	startsAt: string; // ISO-8601 UTC
	endsAt: string;   // ISO-8601 UTC
}

export interface TopHotPlayerDTO {
	playerId: number;
	playerName: string;
	boughtTokens: number;
	buyVolume: number; // En centésimas de crédito
}

export interface MarketTrendsResponseDTO {
	window: MarketTrendsWindowDTO;
	topHotPlayers: TopHotPlayerDTO[];
	buyTransactionsPercentage: number | null;
	sellTransactionsPercentage: number | null;
	marketBias: MarketBiasDTO | null;
	totalTransactions: number;
	totalTradedTokens: number;
	totalTradedCredits: number; // En centésimas de crédito
}
```

### 1.2. Extensión de Cotizaciones (`frontend/src/features/players/types/dtos.ts`)

```typescript
export interface PlayerQuoteResponseDTO {
	effectiveDate: string;
	price: number;
	previousPrice: number | null;
	multiplier: number | string;
	sportMultiplier?: number | string | null;
	marketDemandFactor?: number | string | null;
	marketMultiplier?: number | string | null;
	marketNetDemandTokens?: number | null;
	variationCap?: number | string | null;
	minPriceFloor?: number | null;
	score: number | string | null;
	strategy: string | null;
	strategyVersion: string | null;
	sourceValuationDate: string | null;
	origin: "INITIAL" | "RECOMPUTED" | "CARRIED_FORWARD" | "MARKET_DEMAND" | string;
	calculatedAt: string;
}
```

---

## 2. Modelos de Dominio Frontend

### 2.1. Modelos de Mercado (`frontend/src/features/market/types/models.ts`)

```typescript
export type MarketBias = "BULLISH" | "BEARISH" | "NEUTRAL";

export interface TopHotPlayer {
	playerId: number;
	playerName: string;
	boughtTokens: number;
	buyVolumeCredits: number; // Créditos decimales (buyVolume / 100)
}

export interface MarketTrends {
	window: {
		startsAt: string;
		endsAt: string;
	};
	topHotPlayers: TopHotPlayer[];
	buyTransactionsPercentage: number | null;
	sellTransactionsPercentage: number | null;
	marketBias: MarketBias | null;
	totalTransactions: number;
	totalTradedTokens: number;
	totalTradedCredits: number; // Créditos decimales (totalTradedCredits / 100)
	isEmpty: boolean;           // true cuando totalTransactions === 0 || topHotPlayers.length === 0
}
```

### 2.2. Modelo de Cotización Enriquecido (`frontend/src/features/players/types/models.ts`)

```typescript
export type PlayerQuoteOrigin = "INITIAL" | "RECOMPUTED" | "CARRIED_FORWARD" | "MARKET_DEMAND";

export interface QuoteBreakdownFactors {
	hasFactors: boolean;
	sportContributionPct: number | null;    // ej. +5.50 %
	demandContributionPct: number | null;   // ej. +2.50 %
	totalVariationPct: number | null;       // ej. +8.00 %
	isMarketDemandOnly: boolean;            // true si origin === "MARKET_DEMAND"
	netDemandTokens: number | null;
}

export interface PlayerQuote {
	effectiveDate: string;
	price: number;
	previousPrice: number | null;
	multiplier: number;
	sportMultiplier: number | null;
	marketDemandFactor: number | null;
	marketMultiplier: number | null;
	marketNetDemandTokens: number | null;
	score: number | null;
	strategy: string | null;
	strategyVersion: string | null;
	sourceValuationDate: string | null;
	origin: PlayerQuoteOrigin;
	calculatedAt: string;
	absoluteChange: number | null;
	percentageChange: number | null;
	direction: "up" | "down" | "flat" | "unknown";
	breakdown: QuoteBreakdownFactors;
}
```

---

## 3. Mappers y Transformaciones Puras

### 3.1. `toMarketTrends` (`frontend/src/features/market/market.mapper.ts`)

```typescript
export function toMarketTrends(dto: MarketTrendsResponseDTO): MarketTrends {
	const totalTransactions = Number(dto?.totalTransactions) || 0;
	const totalTradedTokens = Number(dto?.totalTradedTokens) || 0;
	const totalTradedCredits = (Number(dto?.totalTradedCredits) || 0) / 100;

	const topHotPlayers: TopHotPlayer[] = Array.isArray(dto?.topHotPlayers)
		? dto.topHotPlayers.map((player) => ({
				playerId: Number(player.playerId),
				playerName: player.playerName ?? "Desconocido",
				boughtTokens: Number(player.boughtTokens) || 0,
				buyVolumeCredits: (Number(player.buyVolume) || 0) / 100,
			}))
		: [];

	const isEmpty = totalTransactions === 0 && topHotPlayers.length === 0;

	return {
		window: {
			startsAt: dto?.window?.startsAt ?? "",
			endsAt: dto?.window?.endsAt ?? "",
		},
		topHotPlayers,
		buyTransactionsPercentage: dto?.buyTransactionsPercentage ?? null,
		sellTransactionsPercentage: dto?.sellTransactionsPercentage ?? null,
		marketBias: dto?.marketBias ?? null,
		totalTransactions,
		totalTradedTokens,
		totalTradedCredits,
		isEmpty,
	};
}
```

### 3.2. `deriveQuoteBreakdown` (`frontend/src/features/players/utils/playerQuotes.ts`)

```typescript
export function deriveQuoteBreakdown(dto: PlayerQuoteResponseDTO): QuoteBreakdownFactors {
	const isMarketDemandOnly = dto.origin === "MARKET_DEMAND";
	const rawSportMult = dto.sportMultiplier != null ? Number(dto.sportMultiplier) : null;
	const rawDemandFactor = dto.marketDemandFactor != null ? Number(dto.marketDemandFactor) : null;

	let sportContributionPct: number | null = null;
	if (isMarketDemandOnly) {
		sportContributionPct = 0;
	} else if (rawSportMult !== null && Number.isFinite(rawSportMult)) {
		sportContributionPct = (rawSportMult - 1) * 100;
	}

	let demandContributionPct: number | null = null;
	if (rawDemandFactor !== null && Number.isFinite(rawDemandFactor)) {
		demandContributionPct = rawDemandFactor * 100;
	}

	const hasFactors = sportContributionPct !== null || demandContributionPct !== null;

	return {
		hasFactors,
		sportContributionPct,
		demandContributionPct,
		totalVariationPct:
			dto.previousPrice && dto.price
				? ((dto.price - dto.previousPrice) / dto.previousPrice) * 100
				: null,
		isMarketDemandOnly,
		netDemandTokens: dto.marketNetDemandTokens ?? null,
	};
}
```

---

## 4. Estados de UI (Component States)

| Componente | Estados Soportados | Renderizado |
| :--- | :--- | :--- |
| `MarketTrendsPage` | `loading` | `MarketTrendsSkeleton` con tarjetas de métricas, barra esqueleto y grilla vacía. |
| | `error` | `MarketTrendsErrorState` con mensaje pedagógico y botón `Reintentar`. |
| | `empty` | `MarketTrendsEmptyState` cuando `trends.isEmpty === true`. |
| | `success` | Renderizado completo de métricas, barra de sentimiento y `MarketHotAssets`. |
| `MarketSentimentBar` | `active` | Barra dividida verde/rojo con tooltip de porcentaje y badge de sesgo (`Alcista`/`Bajista`/`Neutral`). |
| | `neutral_empty` | Barra unificada gris con rótulo *"Mercado en calma / Sin operaciones recientes"*. |
| `HotDemandBadge` | `visible` | Badge con icono llama (`🔥 Alta Demanda`) y tooltip accesible. |
| | `hidden` | No se renderiza si el jugador no pertenece al set de `topHotPlayers`. |
| `PlayerQuoteBreakdownPopover` | `hasFactors` | Botón interactivo con icono `info` que despliega el desglose deportivo vs. demanda. |
| | `noFactors` | Oculto o indicador neutro si la cotización no tiene componentes registrados. |


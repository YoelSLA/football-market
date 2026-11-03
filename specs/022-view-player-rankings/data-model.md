# Data Model & State Architecture: Visualizar ranking de jugadores (SPEC-022)

**Feature**: `022-view-player-rankings`
**Spec**: [`spec.md`](spec.md) · **Plan**: [`plan.md`](plan.md) · **Research**: [`research.md`](research.md)

Este documento define las estructuras de datos, tipos TypeScript, DTOs de transporte y el modelo de estado en memoria para la funcionalidad del ranking de jugadores.

---

## 1. DTOs de Transporte (Backend Contracts)

### 1.1. Ranking por Score (`GET /api/players/ranking/score`)

```typescript
export interface ScoreRankingEntryDTO {
  position: number;
  playerId: number;
  score: number;
  valuationDate: string; // ISO yyyy-MM-dd
}

export interface ScoreRankingResponseDTO {
  strategy: string | null; // POSITIONAL_PERFORMANCE | GENERAL_ATTRIBUTES
  quoteDate: string | null; // ISO yyyy-MM-dd
  ranking: ScoreRankingEntryDTO[];
}
```

### 1.2. Ranking por Cotización (`GET /api/players/ranking`)

```typescript
export interface QuoteRankingEntryDTO {
  position: number;
  playerId: number;
  price: number; // Valor numérico en créditos
  effectiveDate: string; // ISO yyyy-MM-dd
  origin: "RECOMPUTED" | "CARRIED_FORWARD";
  calculatedAt: string; // ISO datetime
}

export interface QuoteRankingResponseDTO {
  ranking: QuoteRankingEntryDTO[];
}
```

---

## 2. Modelos de Dominio de la Feature (Frontend Domain Models)

### 2.1. Entrada Enriquecida del Ranking (`PlayerRankingItem`)

Entidad que combina los datos posicionales provistos por el backend con los atributos visuales resueltos desde el catálogo de jugadores:

```typescript
export type TacticalPosition = "DEL" | "MED" | "DEF" | "POR";

export interface PlayerRankingItem {
  position: number;
  playerId: number;
  playerName: string;
  playerAvatar: string | null;
  teamName: string;
  teamBadge: string | null;
  leagueName: string;
  leagueFlag: string | null;
  tacticalPosition: TacticalPosition;
  
  // Datos específicos del modo activo
  score?: number; // Presente en modo rendimiento (escala con 6 decimales)
  price?: number; // Presente en modo cotización (en créditos)
  formattedDate: string; // Fecha legible formateada (dd MMM. yyyy)
  origin?: "RECOMPUTED" | "CARRIED_FORWARD"; // Origen en modo cotización
}
```

### 2.2. Metadatos de la Cabecera (`RankingMetadata`)

```typescript
export interface RankingMetadata {
  mode: RankingMode;
  strategyName: string | null; // Ej: "POSITIONAL_PERFORMANCE"
  strategyLabel: string; // Ej: "Desempeño por posición"
  referenceDate: string | null; // Fecha formateada de valuación o vigencia
  totalClassified: number; // Conteo total de jugadores en la lista
  quoteOriginSummary?: string; // Ej: "Mercado"
}
```

---

## 3. Estado de la Página y Control de Filtros

### 3.1. Modos de Visualización (`RankingMode`)

```typescript
export type RankingMode = "PERFORMANCE" | "VALUATION";
```

- `"PERFORMANCE"`: Consulta `/api/players/ranking/score`, exhibe el score de la estrategia activa.
- `"VALUATION"`: Consulta `/api/players/ranking`, exhibe la cotización vigente en créditos y el origen.

### 3.2. Criterios de Filtrado en Cliente (`RankingFilterCriteria`)

```typescript
export interface RankingFilterCriteria {
  searchTerm: string; // Búsqueda insensible a mayúsculas/acentos en el nombre del jugador
  selectedLeague: string; // "" para "Todas las ligas"
  selectedPosition: string; // "" para "Todas las posiciones"
}
```

### 3.3. Estado de Paginación (`RankingPaginationState`)

```typescript
export interface RankingPaginationState {
  currentPage: number; // Base 1
  pageSize: number; // 10 por defecto (configurable a 20 o 50)
  totalItems: number;
  totalPages: number;
}
```

---

## 4. Flujo de Transformación y Selectores

```text
[GET /api/players/ranking/score o /ranking] (DTO)
                 +
[useAllPlayersQuery()] (Catálogo de jugadores)
                 ↓
      [ranking.mapper.ts]
                 ↓
  [PlayerRankingItem[]] (Lista ordenada por position ascendente)
                 ↓
      ┌──────────┴──────────┐
      ↓                     ↓
  [Top 3 Podio]       [Filtros de Cliente]
  (Items 1, 2, 3)     (Búsqueda, Liga, Posición)
  (Fijo sin filtros)        ↓
                      [Lista Filtrada]
                            ↓
                      [Paginador (10/pág)]
                            ↓
                      [Tabla Visible]
```

---

## 5. Reglas de Validación y Consistencia

1. **Inmutabilidad del orden oficial**: El campo `position` devuelto por el backend es la clave de ordenamiento inalterable. Los filtros cliente ocultan filas pero **nunca alteran el número de posición** `#` asignado a cada jugador.
2. **Top 3 fijo**: Los integrantes del Podio corresponden a los elementos con `position === 1`, `position === 2` y `position === 3` de la lista general del modo activo, sin verse afectados por los filtros de la tabla.
3. **Manejo de jugadores sin imagen o datos de club**: Si un jugador no posee imagen en el catálogo, se utiliza el fallback visual semántico (`DefaultAvatar`); si el catálogo aún está cargando, se exhibe un esqueleto (`Skeleton`) sin bloquear la renderización de la posición y el score.

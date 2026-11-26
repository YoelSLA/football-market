# Tasks: Tablero de tendencias de mercado y visualización de demanda comunitaria (SPEC-029)

**Input**: Design documents from `/specs/029-frontend-market-trends-and-sentiment/`
**Prerequisites**: [`plan.md`](plan.md) (required), [`spec.md`](spec.md) (required for user stories), [`research.md`](research.md), [`data-model.md`](data-model.md), [`contracts/`](contracts/)
**Tests**: Frontend está exento conforme a la Constitución y normas del repositorio: MUST NOT crear tests ni infraestructura frontend; el agente no ejecuta tests en ninguna tarea.
**Workflow local**: Documentación viva según Constitución §V y `docs/development/sdd-workflow.md`. Tasks viven en este archivo (tríada permanente obligatoria junto a `spec.md` y `plan.md`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Definición de contratos de datos, modelos tipados, mappers y cliente de servicio para tendencias.

- [X] T001 Register `trends` query key in `frontend/src/features/market/constants/marketQueryKeys.ts`
- [X] T002 [P] Define market trends DTOs (`MarketTrendsResponseDTO`, `TopHotPlayerDTO`, `MarketTrendsWindowDTO`) in `frontend/src/features/market/types/dtos.ts`
- [X] T003 [P] Define market trends domain models (`MarketTrends`, `TopHotPlayer`, `MarketBias`) in `frontend/src/features/market/types/models.ts`
- [X] T004 [P] Export new market types in `frontend/src/features/market/types/index.ts`
- [X] T005 Implement `toMarketTrends` mapper normalizing centimes to credits in `frontend/src/features/market/market.mapper.ts`
- [X] T006 Implement `getTrends` API method in `frontend/src/features/market/market.service.ts`
- [X] T007 Implement `useMarketTrendsQuery` with 60s background polling and manual refetch in `frontend/src/features/market/hooks/queries/useMarketTrendsQuery.ts`
- [X] T008 [P] Implement `useHotPlayerIds` selector hook in `frontend/src/features/market/hooks/queries/useHotPlayerIds.ts`
- [X] T009 Export query hooks in `frontend/src/features/market/hooks/queries/index.ts` and `frontend/src/features/market/hooks/index.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura de rutas y navegación global en el frontend requerida antes de las pantallas.

- [X] T010 Add "Tendencias" tab to `market` section in `frontend/src/app/navigation/sections.ts`
- [X] T011 Register `/market/trends` route under `AppLayout` in `frontend/src/app/router/AppRouter/AppRouter.tsx`

**Checkpoint**: Base de datos y navegación listas. La implementación de historias de usuario puede comenzar.

---

## Phase 3: User Story 1 — Tablero de Tendencias y Sentimiento del Mercado (Priority: P1) 🎯 MVP

**Goal**: Crear la pantalla interactiva `/market/trends` con tacómetro radial, barra segmentada de sentimiento, cuota de demanda del Top 5, métricas cuantitativas y ranking de Hot Assets.

**Independent Test**: Navegar a `/market/trends` y verificar el tacómetro radial SVG, la barra proporcional de sentimiento, las tarjetas de métricas cuantitativas, las barras horizontales de cuota de demanda del Top 5 y las tarjetas interactivas de jugadores enlazadas a `/players/:id`.

### Implementation for User Story 1

- [X] T012 [P] [US1] Implement `MarketTrendsHeader` with title, window badge and manual refresh button in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketTrendsHeader/MarketTrendsHeader.tsx`
- [X] T013 [P] [US1] Implement `MarketSentimentGauge` semicircular SVG gauge with tricolor gradient, needle and percentage readout in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketSentimentGauge/MarketSentimentGauge.tsx`
- [X] T014 [P] [US1] Implement `MarketSentimentBar` segmented proportional bar with bullish/bearish/neutral status and transaction count in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketSentimentBar/MarketSentimentBar.tsx`
- [X] T015 [P] [US1] Implement `MarketDemandShareChart` horizontal bar distribution chart for Top 5 demand share in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketDemandShareChart/MarketDemandShareChart.tsx`
- [X] T016 [P] [US1] Implement `MarketMacroMetrics` cards for total volume in credits and total transaction count in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketMacroMetrics/MarketMacroMetrics.tsx`
- [X] T017 [P] [US1] Implement `MarketHotAssets` grid with interactive player cards linking to `/players/:id` in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketHotAssets/MarketHotAssets.tsx`
- [X] T018 [P] [US1] Implement `MarketTrendsSkeleton` for loading states in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketTrendsSkeleton/MarketTrendsSkeleton.tsx`
- [X] T019 [P] [US1] Implement `MarketTrendsEmptyState` for calm market without operations in `frontend/src/features/market/pages/MarketTrendsPage/components/MarketTrendsEmptyState/MarketTrendsEmptyState.tsx`
- [X] T020 [US1] Assemble `MarketTrendsPage` orchestrating query state, error retry, empty state, and layout in `frontend/src/features/market/pages/MarketTrendsPage/MarketTrendsPage.tsx`
- [X] T021 [US1] Export `MarketTrendsPage` in `frontend/src/features/market/pages/index.ts` and `frontend/src/features/market/index.ts`

**Checkpoint**: Tablero macro de tendencias 100% operativo y navegable de forma autónoma.

---

## Phase 4: User Story 2 — Insignias de Alta Demanda en Catálogos Oficial y P2P (Priority: P2)

**Goal**: Mostrar la insignia visual "🔥 Alta Demanda" en las tarjetas de jugadores del Catálogo Oficial y del Mercado P2P que figuren en el Top 5 de tendencias.

**Independent Test**: Navegar a `/market/buy` y `/market/p2p`, confirmando que los activos en el Top 5 exhiben el badge "🔥 Alta Demanda" con tooltip explicativo y los demás no.

### Implementation for User Story 2

- [X] T022 [P] [US2] Implement `HotDemandBadge` component with fire icon, tooltip on hover/focus and accessible text in `frontend/src/features/market/components/HotDemandBadge/HotDemandBadge.tsx`
- [X] T023 [US2] Integrate `HotDemandBadge` into official catalog card in `frontend/src/features/market/pages/MarketBuyPage/components/MarketPlayerCard/MarketPlayerCard.tsx` using `useHotPlayerIds`
- [X] T024 [US2] Integrate `HotDemandBadge` into community P2P listing card in `frontend/src/features/market/pages/MarketP2PPage/components/CommunityListingCard/CommunityListingCard.tsx` using `useHotPlayerIds`

**Checkpoint**: Catálogos enriquecidos con badges reactivos sin duplicación de peticiones de red.

---

## Phase 5: User Story 3 — Desglose de Cotización en Detalle del Jugador (Priority: P2)

**Goal**: Exponer con transparencia la descomposición de la cotización en `PlayerDetailPage` diferenciando Aporte Deportivo (WhoScored) y Presión de Mercado (Demanda comunitaria).

**Independent Test**: Acceder a `/players/:id` de un jugador con cotización recalculada, pulsar el botón de desglose y comprobar el popover con Aporte Deportivo (+X.XX%), Presión de Mercado (+Y.YY%) y Variación Total (+Z.ZZ%).

### Implementation for User Story 3

- [X] T025 [P] [US3] Add audit fields (`sportMultiplier`, `marketDemandFactor`, `marketMultiplier`, `marketNetDemandTokens`) to `PlayerQuoteResponseDTO` in `frontend/src/features/players/types/dtos.ts`
- [X] T026 [P] [US3] Define `QuoteBreakdownFactors` and update `PlayerQuote` model in `frontend/src/features/players/types/models.ts`
- [X] T027 [US3] Implement `deriveQuoteBreakdown` and update `toPlayerQuote` in `frontend/src/features/players/utils/playerQuotes.ts`
- [X] T028 [P] [US3] Implement `PlayerQuoteBreakdownPopover` component with sports and demand breakdown in `frontend/src/features/players/pages/PlayerDetailPage/components/PlayerOverview/components/PlayerQuoteBreakdownPopover/PlayerQuoteBreakdownPopover.tsx`
- [X] T029 [US3] Integrate `PlayerQuoteBreakdownPopover` trigger and layout into `frontend/src/features/players/pages/PlayerDetailPage/components/PlayerOverview/components/PlayerOverviewQuote/PlayerOverviewQuote.tsx`

**Checkpoint**: Transparencia financiera de factores de cotización disponible en la ficha del jugador.

---

## Phase 6: User Story 4 — Adaptabilidad Móvil, Resiliencia y Accesibilidad (Priority: P3)

**Goal**: Asegurar diseño responsivo completo desde 320px, accesibilidad semántica WCAG (`aria-label`, roles de meter/gauge) y resiliencia ante fallos de red.

**Independent Test**: Validar en viewports móviles (320px, 480px) y desktop (>1024px) que no existe scroll horizontal y que los lectores de pantalla anuncian porcentajes y sesgo sin depender solo del color.

### Implementation for User Story 4

- [X] T030 [P] [US4] Audit and adjust responsive media queries and CSS grid/flexbox in `frontend/src/features/market/pages/MarketTrendsPage/MarketTrendsPage.module.scss`
- [X] T031 [P] [US4] Add ARIA meter/gauge roles, accessible labels, and contrast tokens for `MarketSentimentGauge` and `MarketSentimentBar`
- [X] T032 [US4] Verify error boundary and network recovery state with retry action in `frontend/src/features/market/pages/MarketTrendsPage/MarketTrendsPage.tsx`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificación estática de tipos, higiene de código y validación interactiva end-to-end.

- [X] T033 [P] Run code hygiene, Biome formatting and TypeScript compilation checks (`npm run check && npm run build` in `frontend/`)
- [X] T034 Execute end-to-end interactive validation scenarios from `specs/029-frontend-market-trends-and-sentiment/quickstart.md`
- [X] T035 [P] Update technical debt register in `docs/TECHNICAL_DEBT.md` if any non-critical debt is identified during review

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — inicia de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 (DTOs y tipos). Bloquea la navegación a las historias.
- **User Story 1 (Phase 3)**: Depende de Phase 1 y 2. Entrega el MVP autónomo.
- **User Story 2 (Phase 4)**: Depende de Phase 1 (hook `useHotPlayerIds`). Se integra con catálogos existentes de forma independiente.
- **User Story 3 (Phase 5)**: Depende de Phase 1 (modelos y DTOs de players). Independiente de US1 y US2.
- **User Story 4 (Phase 6)**: Depende de US1 (tacómetro y barra).
- **Polish (Phase 7)**: Depende de todas las historias completadas.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Completar Setup (T001-T009) + Foundational (T010-T011).
2. Implementar User Story 1 (T012-T021).
3. Validar de forma independiente: la ruta `/market/trends` ya es un MVP completamente funcional y desplegable.
4. Proceder incrementalmente con US2 (Badges), US3 (Desglose) y US4 (A11y/Responsividad).

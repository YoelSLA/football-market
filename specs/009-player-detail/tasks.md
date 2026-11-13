# Tareas: Detalle individual del jugador

**Estado**: Implementado y verificado.
**Referencia visual canónica**: [wireframe canónico](../../docs/wireframes/player-detail.md).
**Integración autorizada 2026-11-12**: se conserva el diseño de `dev` con cotización en cabecera y pestaña dedicada. La lectura UI usa el historial oficial persistido; no se permiten datos de demostración como cotización del jugador.

---

## Tareas de la funcionalidad vigente

### Fase 1 — Fundamentos y modelos

- [x] **T01** Definir tipos, utilidades de presentación y metadatos canónicos de 60 métricas en `frontend/src/features/players/utils/playerStatisticsView.ts` y `frontend/src/features/players/models/playerStatistics.ts`, retirando el filtro de unidad y estructurando las 8 categorías canónicas.
- [x] **T02** Coordinar el estado local en `frontend/src/features/players/hooks/pages/usePlayerDetailPage.ts` con `playerId`, `activeTab` (`'quotes'` / `'summary'` / `'catalog'`), `search`, `category` y acción `clearFilters`, con reset automático al cambiar de jugador.

### Fase 2 — Cabecera de 3 zonas y cotización oficial

- [x] **T03** Componer la cabecera en tres zonas en `frontend/src/features/players/pages/PlayerDetailPage/components/PlayerOverview/PlayerOverview.tsx`: identidad a la izquierda (avatar 90×90 px), tres micro-tarjetas titulares en el centro y caja de cotización a la derecha.
- [x] **T04** Integrar en la cabecera la cotización oficial de 94 px con precio en créditos, fecha oficial, badges de variación semántica (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`) y diferencia absoluta en créditos.
- [x] **T05** Aplicar estilos en `PlayerOverview.module.scss` para altura de 125 px en escritorio, apilado de dos filas en tablet (768 px) y fila simétrica en móvil (360 px).

### Fase 3 — Pestaña de Cotizaciones

- [x] **T06** Configurar la pestaña `[ Cotizaciones ]` como predeterminada inicial en `PlayerDetailPage.tsx`, coordinando la carga de cotizaciones con `usePlayerQuoteQuery`.
- [x] **T07** Presentar el gráfico interactivo de evolución histórica en `PlayerQuoteEvolutionChart.tsx` y el resumen de cotizaciones en `PlayerQuotePanel.tsx`, con tooltip de valores y tratamiento de estados sin cotización.

### Fase 4 — Resumen deportivo

- [x] **T08** Implementar las cuatro tarjetas KPI principales (`SummaryKpiCard.tsx`) por perfil posicional, con iconos de 24 px en caja de 48×48 px, títulos de 12 px, valores en 28 px bold y sin unidades redundantes en el valor.
- [x] **T09** Implementar exactamente dos paneles temáticos específicos por perfil (`SummaryPanel.tsx`) con filas acotadas (`min-height: 44px; max-height: 58px;`) y espaciado armónico (`justify-content: space-evenly`).
- [x] **T10** Retirar barras de progreso relativas sin escala (`PlayerStatisticsBars`) y el bloque redundante de destacadas, consolidando el resumen exclusivamente en los 4 KPI y los 2 paneles.

### Fase 5 — Catálogo completo de 60 métricas

- [x] **T11** Implementar la pestaña `[ Todas las estadísticas ]` en `PlayerStatisticsCatalog.tsx` con las 60 métricas canónicas distribuidas en las 8 categorías normativas.
- [x] **T12** Integrar en el encabezado del catálogo el buscador normalizado por texto/clave, filtro de 8 categorías con conteo dinámico (`aria-pressed`) y botón de limpieza de filtros.
- [x] **T13** Aplicar ordenamiento estable por prioridad de disponibilidad (Principal → Secundaria → Base) y situar las métricas ausentes al final con indicación clara de ausencia (`-`).

### Fase 6 — Layout, paleta semántica, accesibilidad y backend

- [x] **T14** Implementar accesibilidad completa: navegación por pestañas con flechas/Tab/Home/End, foco visible, atributos `aria-selected` y contraste WCAG AA.
- [x] **T15** Aplicar paleta semántica estricta con tokens sólidos (`$surface-page-deep`, `$surface-card-dark`, `$surface-panel-to`, `$surface-accent-deep`, `$accent-sports-green`, `$border-panel`), eliminando gradientes y sombras decorativas.
- [x] **T16** Garantizar adaptación responsive y cero scroll vertical u horizontal en resoluciones de escritorio (1920×1080, 1920×913, 1366×768, 1280×800) en la vista de Resumen deportivo.
- [x] **T17** Proveer contratos y endpoints backend: `GET /api/players/{id}`, `GET /api/players/{id}/statistics`, `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current`, con validación estricta de IDs y tratamiento de estados (200, 400, 401, 404, 500).
- [x] **T18** Validar la experiencia de usuario y ausencia de scroll con suite automatizada de Playwright y comprobaciones manuales en los tres viewports normativos (360 px, 768 px y 1280+ px).

---

## Matriz de trazabilidad

| Requisito | Criterio de éxito | Componentes y archivos afectados | Tarea |
| --- | --- | --- | --- |
| **FR-001** (Consulta de identidad) | **SC-001** | `PlayerDetailPage.tsx`, `usePlayerDetailPage.ts`, `players.service.ts` | T02, T17 |
| **FR-002** (Autenticación y guard) | **SC-001** | `AppRouter.tsx`, `AuthGuard.tsx` | T17 |
| **FR-003** (Visibilidad y 404) | **SC-001** | `PlayerDetailPage.tsx`, `PlayerOverview.tsx`, Backend `PlayerService` | T03, T17 |
| **FR-004** (Validación de identificador) | **SC-001** | `PlayerDetailPage.tsx`, Backend `PlayerController` | T02, T17 |
| **FR-005** (Cabecera de tres zonas) | **SC-002** | `PlayerOverview.tsx`, `PlayerOverview.module.scss` | T03, T05 |
| **FR-006** (Tratamiento de atributos opcionales) | **SC-002** | `PlayerOverview.tsx`, `playerStatisticsView.ts` | T01, T03 |
| **FR-007** (Caja de cotización oficial) | **SC-002** | `PlayerOverview.tsx`, `PlayerOverview.module.scss` | T04, T05 |
| **FR-008** (Tres pestañas ordenadas) | **SC-003** | `PlayerDetailPage.tsx`, `PlayerDetailPage.module.scss` | T02, T06 |
| **FR-009** (Pestaña de cotizaciones) | **SC-003** | `PlayerQuotePanel.tsx`, `PlayerQuoteEvolutionChart.tsx` | T06, T07 |
| **FR-010** (Resumen: 4 KPI y 2 paneles) | **SC-004** | `PlayerStatisticsSummary.tsx`, `SummaryKpiCard.tsx`, `SummaryPanel.tsx` | T08, T09 |
| **FR-011** (Perfiles posicionales normativos) | **SC-004** | `playerStatisticsView.ts`, `PlayerStatisticsSummary.tsx` | T01, T08, T09 |
| **FR-012** (Eliminación de barras y redundancias)| **SC-004** | `PlayerStatisticsSummary.tsx` | T10 |
| **FR-013** (Catálogo de 60 métricas) | **SC-005** | `PlayerStatisticsCatalog.tsx`, `metric-glossary.md` | T11 |
| **FR-014** (8 categorías de métricas) | **SC-005** | `PlayerStatisticsCatalog.tsx`, `playerStatisticsView.ts` | T01, T11 |
| **FR-015** (Buscador y filtros en cabecera) | **SC-005** | `PlayerStatisticsCatalog.tsx` | T12 |
| **FR-016** (Tratamiento de ausencias y orden) | **SC-005** | `PlayerStatisticsCatalog.tsx`, `playerStatisticsView.ts` | T01, T13 |
| **FR-017** (Consulta de estadísticas backend) | **SC-006** | `PlayerStatisticsService.java`, `PlayerStatisticsRepository.java` | T17 |
| **FR-018** (Semántica de nulo frente a cero) | **SC-006** | `PlayerStatisticsView.ts`, `SummaryKpiCard.tsx`, `SummaryPanel.tsx` | T01, T08, T09 |
| **FR-019** (Retención de respuesta compatible)| **SC-006** | `usePlayerDetailPage.ts`, `QueryCache` | T02 |
| **FR-020** (Sin datos de jugador visible) | **SC-006** | `PlayerDetailPage.tsx`, `PlayerStatisticsSummary.tsx` | T08, T11 |
| **FR-021** (Paleta semántica de colores sólidos)| **SC-007** | `_tokens.scss`, `PlayerDetailPage.module.scss`, componentes | T15 |
| **FR-022** (Accesibilidad y foco visible) | **SC-007** | `PlayerDetailPage.tsx`, `PlayerOverview.tsx`, `PlayerStatisticsCatalog.tsx` | T14 |
| **FR-023** (Adaptación responsive) | **SC-008** | Módulos SCSS de cabecera, resumen, catálogo y layout | T05, T16 |
| **FR-024** (Cero scroll en escritorio) | **SC-008** | `PlayerDetailPage.module.scss`, `PlayerStatisticsSummary.module.scss` | T16, T18 |

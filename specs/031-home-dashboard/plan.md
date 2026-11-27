# Implementation Plan: Dashboard interactivo y resumen ejecutivo de mercado (SPEC-031)

**Feature Branch**: `feat/031-home-dashboard`  
**Estado**: Plan de implementación técnico propuesto.  
**Spec de referencia**: [`spec.md`](spec.md)  
**Contrato API**: [`contracts/dashboard-api.md`](contracts/dashboard-api.md)  
**Modelo de datos**: [`data-model.md`](data-model.md)  
**Investigación y decisiones**: [`research.md`](research.md)  
**Guía de validación**: [`quickstart.md`](quickstart.md)

---

## 1. Summary

Construcción integral del **Dashboard Financiero y Deportivo** en la ruta principal `/home`, transformando el placeholder actual en un cockpit de alta densidad y glamour visual inspirado en **Bloomberg Terminal** y **Sorare**:
1. **Backend (`footballmarket.features.dashboard`)**:
   - Creación del endpoint agregador `GET /api/dashboard/overview`, que consolida métricas patrimoniales (Net Worth, P&L, liquidez, custodia), distribución de cartera por liga y posición, pulso de mercado, top 5 gainers/losers del último recálculo, radar de 4 value picks, 3 mejores ofertas P2P con descuento y transacciones recientes.
   - Creación del endpoint `GET /api/dashboard/performance`, que reconstruye la curva histórica del usuario a partir de `market_transactions` cruzadas con `player_quotes` y la serie normalizada del índice de mercado Football Market Index (FMI).
   - Optimización de consultas analíticas SQL sobre índices existentes en `player_quotes` y `token_listings`.
2. **Frontend (`frontend/src/features/dashboard`)**:
   - Reemplazo de la página temporal en `/home` por `DashboardPage` modular bajo `AppLayout`.
   - Cinta horizontal superior de cotizaciones continuas (**Market Ticker**).
   - Tarjetas elevadas de patrimonio (**Financial Hero Cockpit**) con feedback cromático y micro-sparklines.
   - Gráfico interactivo **Donut SVG nativo** con toggle de dimensión (Posición vs. Liga) e indicador de concentración.
   - Gráfico interactivo de **Área SVG dual** de rendimiento frente al benchmark FMI.
   - Cartas coleccionables estilo Sorare con retratos, escudos, banderas y score WhoScored.
   - Bloque de arbitrajes P2P (*Best Deals*) y timeline de transacciones recientes.

---

## 2. Technical Context

- **Backend**: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security OAuth2 / JWT, PostgreSQL 16+.
- **Frontend**: React 19.3.0 + TypeScript 7.0.2, TanStack Query v5, React Router v7, SCSS Modules con tokens centralizados `@/styles/abstracts`.
- **Almacenamiento**: PostgreSQL canónico (`user_credits`, `player_tokens`, `player_quotes`, `market_transactions`, `token_listings`).
- **Target Platform & Viewport**: Aplicación web desktop adaptativa ($\ge 1024$ px, optimizada para 1920 × 913 px), rutas protegidas bajo `AuthGuard`.
- **Rendimiento**: Carga inicial del dashboard $<200$ ms; cero librerías pesadas en frontend (gráficos SVG nativos puros); renderizado fluido a 60 fps.
- **Testing y Verificación**: Escenarios interactivos end-to-end en `quickstart.md`; verificación estática sin tests automáticos de aplicación conforme a la Constitución.

---

## 3. Constitution Check

*GATE: Evaluación frente a los principios fundamentales de la Constitución.*

| Principio | Evaluación de Cumplimiento |
| :--- | :--- |
| **I. Simplicidad y calidad** | **PASA.** Cero dependencias externas agregadas en frontend (SVG nativo en lugar de Chart.js/Recharts). Paquete backend acotado en `features/dashboard` que reutiliza entidades canónicas existentes sin crear tablas innecesarias. |
| **II. Proporcionalidad del alcance** | **PASA.** Se ciñe estrictamente a las 7 User Stories acordadas y al requisito del TP (visualización gráfica y UX financiera). No incluye compra directa desde modales ni drag-and-drop. |
| **III. Seguridad e integridad** | **PASA.** Ambos endpoints requieren autenticación JWT estricta (`@SecurityRequirement(name = "bearerAuth")`). La identidad del usuario se deriva del token de sesión; no se exponen datos de otros usuarios. |
| **IV. Coherencia de contratos** | **PASA.** Contratos HTTP documentados en `contracts/dashboard-api.md`. Manejo exacto en centésimas en backend y división por 100 en frontend. Sincronización continua de 4 vías. |
| **V. Trazabilidad y fuente de verdad** | **PASA.** Registro de asignación en `specs/assignments.md`. Tríada obligatoria permanente preservada (`spec.md`, `plan.md`, `tasks.md`). Documentación viva del estado actual. |
| **Verificación y testing** | **PASA.** El agente no ejecuta tests de aplicación. Validaciones controladas de compilación (`./gradlew compileJava classes` y `pnpm build`) y linter (`pnpm biome:check`). |

---

## 4. Arquitectura del Backend

### 4.1. Organización de Paquetes (`backend/src/main/java/footballmarket/features/dashboard/`)
```text
footballmarket/features/dashboard/
├── presentation/
│   ├── DashboardController.java                 # Endpoints /api/dashboard/overview y /performance
│   ├── DashboardExceptionHandler.java           # Manejo de excepciones desacoplado
│   ├── dtos/response/
│   │   ├── DashboardOverviewResponseDTO.java
│   │   ├── DashboardFinancialSummaryDTO.java
│   │   ├── DashboardAllocationDTO.java
│   │   ├── DashboardMarketPulseDTO.java
│   │   ├── DashboardPlayerMoverDTO.java
│   │   ├── DashboardValuePickDTO.java
│   │   ├── DashboardP2PDealDTO.java
│   │   ├── DashboardRecentActivityDTO.java
│   │   └── DashboardPerformanceResponseDTO.java
│   └── mappers/
│       └── DashboardMapper.java                 # Mapeo de modelos de dominio a DTOs
├── application/
│   ├── DashboardOverviewService.java            # Orquestador del resumen de mercado y usuario
│   └── DashboardPerformanceService.java         # Algoritmo de reconstrucción histórica de cartera y FMI
└── infrastructure/
    └── DashboardRepository.java                 # Queries nativas de agregación SQL optimizadas
```

### 4.2. Flujo de Datos
```text
DashboardController.overview(jwt)
  └── DashboardOverviewService.getOverview(userEmail)
        ├── userCreditsRepository (saldo líquido disponible)
        ├── portfolioService (valor de tenencias, tokens disponibles/reservados, P&L)
        ├── dashboardRepository.findTopMovers(effectiveDate, limit: 5)
        ├── dashboardRepository.findValuePicks(effectiveDate, limit: 4)
        ├── dashboardRepository.findBestP2PDeals(effectiveDate, limit: 3)
        ├── dashboardRepository.getMarketPulseMetrics(effectiveDate)
        └── operationHistoryService.getRecentActivity(userEmail, limit: 5)
```

---

## 5. Arquitectura del Frontend

### 5.1. Organización Modular (`frontend/src/features/dashboard/`)
```text
frontend/src/features/dashboard/
├── types/
│   ├── dtos.ts                                  # Contratos con el backend (centésimas de crédito)
│   ├── models.ts                                # Modelos UI convertidos a créditos
│   └── index.ts
├── dashboard.service.ts                         # Cliente HTTP Axios (/api/dashboard/*)
├── dashboard.mapper.ts                          # Mapeo DTO -> UI Model con formato de créditos
├── constants/
│   └── dashboardQueryKeys.ts                    # Claves compuestas de TanStack Query
├── hooks/
│   ├── queries/
│   │   ├── useDashboardOverviewQuery.ts         # Hook TanStack Query para overview
│   │   └── useDashboardPerformanceQuery.ts      # Hook TanStack Query para performance
│   └── pages/
│       └── useDashboardPage.ts                  # Orquestador y ViewModel de la página
├── components/
│   ├── MarketTicker/                            # Cinta continua de cotizaciones Bloomberg
│   │   ├── MarketTicker.tsx
│   │   └── MarketTicker.module.scss
│   ├── FinancialHeroCockpit/                    # Tarjetas Net Worth, P&L, Liquidez, Custodia
│   │   ├── FinancialHeroCard/
│   │   ├── FinancialHeroCockpit.tsx
│   │   └── FinancialHeroCockpit.module.scss
│   ├── PortfolioAllocationChart/               # Donut SVG interactivo con leyenda y tooltips
│   │   ├── PortfolioAllocationChart.tsx
│   │   └── PortfolioAllocationChart.module.scss
│   ├── PerformanceBenchmarkChart/              # Gráfico SVG dual de área (Cartera vs FMI)
│   │   ├── PerformanceBenchmarkChart.tsx
│   │   └── PerformanceBenchmarkChart.module.scss
│   ├── MarketMoversTable/                       # Tablas de Top Gainers y Top Losers
│   │   ├── MarketMoversTable.tsx
│   │   └── MarketMoversTable.module.scss
│   ├── SorareShowcaseGrid/                      # Cartas coleccionables 3D con pestañas
│   │   ├── SorarePlayerCard/
│   │   ├── SorareShowcaseGrid.tsx
│   │   └── SorareShowcaseGrid.module.scss
│   ├── P2PBestDealsWidget/                      # Ofertas de arbitraje comunitario con descuento
│   │   ├── P2PBestDealsWidget.tsx
│   │   └── P2PBestDealsWidget.module.scss
│   ├── LiveActivityFeed/                        # Timeline de transacciones recientes
│   │   ├── LiveActivityFeed.tsx
│   │   └── LiveActivityFeed.module.scss
│   ├── QuickActionsBar/                         # Atajos a 1 clic hacia mercados y cartera
│   │   ├── QuickActionsBar.tsx
│   │   └── QuickActionsBar.module.scss
│   └── DashboardEmptyState/                     # Estado vacío motivador para usuarios nuevos
│       ├── DashboardEmptyState.tsx
│       └── DashboardEmptyState.module.scss
├── pages/
│   └── DashboardPage/
│       ├── DashboardPage.tsx                    # Layout responsive y ensamblado de bloques
│       └── DashboardPage.module.scss
└── index.ts                                     # Exportaciones públicas de la feature
```

### 5.2. Enrutamiento e Integración Global
1. **`frontend/src/app/router/AppRouter/AppRouter.tsx`**:
   - Montar `<Route path="/home" element={<DashboardPage />} />` dentro de `<Route element={<AuthGuard privateRoute />}> <Route element={<AppLayout />}>`.
2. **`frontend/src/app/navigation/sections.ts`**:
   - La pestaña `home-dashboard` ya apunta a `/home` con el icono `LayoutDashboard`.

---

## 6. Plan de Implementación por Fases

1. **Fase 1: Backend Foundations & Endpoints**:
   - Implementar DTOs de request/response en `footballmarket.features.dashboard.presentation.dtos`.
   - Implementar `DashboardRepository` con consultas SQL nativas para Top Movers, Value Picks, Best Deals y FMI.
   - Implementar `DashboardOverviewService` y `DashboardPerformanceService`.
   - Implementar `DashboardController` y documentar con anotaciones OpenAPI.
2. **Fase 2: Frontend Infrastructure & Mappers**:
   - Crear tipos TypeScript (`dtos.ts`, `models.ts`), servicio Axios y mappers de centésimas a créditos.
   - Crear queries de TanStack Query (`useDashboardOverviewQuery`, `useDashboardPerformanceQuery`).
3. **Fase 3: Componentes Visuales & Gráficos SVG**:
   - Diseñar `MarketTicker` con animación suave.
   - Implementar `FinancialHeroCockpit` con tarjetas de métricas elevadas y sparklines.
   - Implementar `PortfolioAllocationChart` (Donut SVG con toggle Posición/Liga).
   - Implementar `PerformanceBenchmarkChart` (Curva de área dual Cartera vs. FMI).
   - Implementar `SorareShowcaseGrid` con cartas coleccionables 3D y pestañas.
   - Implementar `P2PBestDealsWidget`, `LiveActivityFeed` y `QuickActionsBar`.
4. **Fase 4: Ensamblado y Resiliencia**:
   - Construir `DashboardPage`, skeletons de carga y `DashboardEmptyState`.
   - Reemplazar la ruta `/home` en `AppRouter.tsx`.
   - Verificación estática con Biome y compilación Gradle.

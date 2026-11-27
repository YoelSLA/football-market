# Tasks: Dashboard interactivo y resumen ejecutivo de mercado (SPEC-031)

**Input**: Documentos de diseño en `specs/031-home-dashboard/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/dashboard-api.md`, `quickstart.md`).
**Prerequisites**: `spec.md` (requerimientos y user stories), `plan.md` (diseño técnico), `contracts/` (contratos API), `data-model.md` (entidades).
**Tests**: Se incluye la creación de tests unitarios y de integración para el backend en `backend/src/test/java/`. La ejecución y validación de tests queda exclusivamente a cargo del usuario (Constitución §19.199). El frontend está exento de tests automatizados.
**Workflow**: Cumple con `docs/development/sdd-workflow.md`. `tasks.md` forma parte de la tríada obligatoria permanente junto a `spec.md` y `plan.md`.

---

## Phase 1: Setup (Infraestructura Compartida)

**Propósito**: Inicialización de la estructura de paquetes y tipos compartidos para Backend y Frontend.

- [x] T001 Crear estructura de paquetes backend en `backend/src/main/java/footballmarket/features/dashboard/`
- [x] T002 Crear estructura de directorios y componentes en `frontend/src/features/dashboard/`
- [x] T003 [P] Configurar constantes compuestas de TanStack Query en `frontend/src/features/dashboard/constants/dashboardQueryKeys.ts`
- [x] T004 [P] Definir interfaces TypeScript de DTOs en `frontend/src/features/dashboard/types/dtos.ts` y modelos de dominio UI en `frontend/src/features/dashboard/types/models.ts`

---

## Phase 2: Foundational (Backend Infrastructure & Endpoints)

**Propósito**: Infraestructura core de backend y clientes frontend bloqueantes para todas las historias de usuario.

- [x] T005 [P] Crear records DTO de respuesta en `backend/src/main/java/footballmarket/features/dashboard/presentation/dtos/response/` (`DashboardOverviewResponseDTO`, `DashboardPerformanceResponseDTO` y subestructuras)
- [x] T006 [P] Implementar `DashboardRepository` con consultas SQL nativas optimizadas para Top Movers, Value Picks, Best Deals y Market Pulse en `backend/src/main/java/footballmarket/features/dashboard/infrastructure/DashboardRepository.java`
- [x] T007 Implementar `DashboardOverviewService` consolidando créditos, tenencias, pulso y mercado en `backend/src/main/java/footballmarket/features/dashboard/application/DashboardOverviewService.java`
- [x] T008 Implementar `DashboardPerformanceService` reconstruyendo deterministamente tenencias pasadas y benchmark FMI en `backend/src/main/java/footballmarket/features/dashboard/application/DashboardPerformanceService.java`
- [x] T009 Implementar `DashboardMapper` para transformar entidades a DTOs en centésimas en `backend/src/main/java/footballmarket/features/dashboard/presentation/mappers/DashboardMapper.java`
- [x] T010 Implementar `DashboardController` exponiendo `GET /api/dashboard/overview` y `GET /api/dashboard/performance` con Swagger OpenAPI en `backend/src/main/java/footballmarket/features/dashboard/presentation/DashboardController.java`
- [x] T011 [P] Crear test unitario de controlador `DashboardControllerTest` en `backend/src/test/java/footballmarket/features/dashboard/presentation/DashboardControllerTest.java`
- [x] T012 [P] Crear test de integración `DashboardOverviewServiceIntegrationTest` en `backend/src/test/java/footballmarket/features/dashboard/application/DashboardOverviewServiceIntegrationTest.java`
- [x] T013 Implementar cliente HTTP Axios `dashboard.service.ts` y mappers de centésimas a créditos `dashboard.mapper.ts` en `frontend/src/features/dashboard/`
- [x] T014 Implementar hooks TanStack Query `useDashboardOverviewQuery.ts` y `useDashboardPerformanceQuery.ts` en `frontend/src/features/dashboard/hooks/queries/`

**Checkpoint**: Fundamentos listos. La implementación de componentes visuales puede proceder de forma independiente.

---

## Phase 3: User Story 1 - Financial Hero Cockpit (Priority: P1) 🎯 MVP

**Objetivo**: Resumen ejecutivo de posición patrimonial (Net Worth, P&L, liquidez disponible y custodia de tokens).
**Test independiente**: Acceder a `/home` con un usuario con tenencias; verificar la tarjeta de Patrimonio Neto, P&L con su variación porcentual cromática, saldo en efectivo y tokens libres vs reservados.

- [x] T015 [P] [US1] Crear componente reutilizable `FinancialHeroCard` en `frontend/src/features/dashboard/components/FinancialHeroCockpit/FinancialHeroCard/FinancialHeroCard.tsx` y su SCSS module
- [x] T016 [P] [US1] Implementar micro-sparkline SVG para la tendencia patrimonial en `frontend/src/features/dashboard/components/FinancialHeroCockpit/PatrimonySparkline/PatrimonySparkline.tsx` y su SCSS module
- [x] T017 [US1] Implementar contenedor `FinancialHeroCockpit` integrando las 4 métricas patrimoniales en `frontend/src/features/dashboard/components/FinancialHeroCockpit/FinancialHeroCockpit.tsx` y su SCSS module

---

## Phase 4: User Story 2 - Analytics de Cartera: Donut Chart de Asignación (Priority: P1)

**Objetivo**: Visualización interactiva de la composición de la cartera por posición táctica y por liga con alertas de concentración.
**Test independiente**: Conmutar entre vista por Liga y por Posición; verificar los arcos de circunferencia SVG, tooltips con importes y alerta si la posición top supera el 40%.

- [x] T018 [P] [US2] Implementar funciones trigonométricas de arcos y porcentajes en `frontend/src/features/dashboard/components/PortfolioAllocationChart/utils/donutMath.ts`
- [x] T019 [US2] Implementar componente `PortfolioAllocationChart` con Donut SVG nativo, leyenda interactiva y tabla semántica accesible en `frontend/src/features/dashboard/components/PortfolioAllocationChart/PortfolioAllocationChart.tsx` y su SCSS module
- [x] T020 [US2] Incorporar badge de alerta de riesgo de concentración (>40% en un futbolista) en `PortfolioAllocationChart.tsx`

---

## Phase 5: User Story 3 - Market Pulse y Benchmark: Rendimiento vs. FMI y Top Movers (Priority: P1)

**Objetivo**: Curva de rendimiento comparada contra el Football Market Index (FMI) y tablas de Top Gainers / Losers.
**Test independiente**: Alternar rangos temporales (`1M`, `3M`, `ALL`); comprobar las dos series temporales en el gráfico de área SVG y hacer clic en un jugador de Top Gainers para verificar la redirección a su perfil.

- [x] T021 [P] [US3] Implementar gráfico de área SVG dual `PerformanceBenchmarkChart` con gradientes y selectores temporales en `frontend/src/features/dashboard/components/PerformanceBenchmarkChart/PerformanceBenchmarkChart.tsx` y su SCSS module
- [x] T022 [P] [US3] Implementar tabla bursátil `MarketMoversTable` con pestañas para Top Gainers y Top Losers en `frontend/src/features/dashboard/components/MarketMoversTable/MarketMoversTable.tsx` y su SCSS module

---

## Phase 6: User Story 4 - Bloomberg Market Ticker (Priority: P2)

**Objetivo**: Cinta horizontal superior con cotizaciones en vivo y deltas porcentuales cromáticos de jugadores estrella.
**Test independiente**: Verificar el desplazamiento horizontal continuo bajo el Navbar, la pausa al hacer hover y la navegación al detalle del jugador al hacer clic.

- [x] T023 [US4] Implementar componente `MarketTicker` con animación suave, chips de deltas (+/-) y pausa en hover en `frontend/src/features/dashboard/components/MarketTicker/MarketTicker.tsx` y su SCSS module

---

## Phase 7: User Story 5 - Showcase Deportivo Coleccionable (Sorare Style) (Priority: P2)

**Objetivo**: Cartas coleccionables 3D de alta gama con retrato, escudo, bandera y score WhoScored organizadas por pestañas estratégicas.
**Test independiente**: Conmutar entre *Mis Joyas*, *Radar de Oportunidades* y *Estrellas en Racha*; pulsar en *"Comprar"* o *"Ver Detalle"* en una carta.

- [x] T024 [P] [US5] Implementar componente de carta coleccionable `SorarePlayerCard` en `frontend/src/features/dashboard/components/SorareShowcaseGrid/SorarePlayerCard/SorarePlayerCard.tsx` y su SCSS module
- [x] T025 [US5] Implementar `SorareShowcaseGrid` con pestañas interactivas (*Mis Joyas*, *Value Picks*, *Form Leaders*) en `frontend/src/features/dashboard/components/SorareShowcaseGrid/SorareShowcaseGrid.tsx` y su SCSS module

---

## Phase 8: User Story 6 - Mercado Comunitario P2P y Feed de Actividad (Priority: P2)

**Objetivo**: Detección de oportunidades de arbitraje en P2P con descuento, timeline de transacciones y atajos de acción rápida.
**Test independiente**: Verificar que Best Deals muestra el porcentaje de descuento respecto al precio oficial; comprobar el timeline de transacciones y navegar mediante los botones de Quick Actions.

- [x] T026 [P] [US6] Implementar widget `P2PBestDealsWidget` con tarjetas de descuento en `frontend/src/features/dashboard/components/P2PBestDealsWidget/P2PBestDealsWidget.tsx` y su SCSS module
- [x] T027 [P] [US6] Implementar timeline `LiveActivityFeed` con iconos de operación en `frontend/src/features/dashboard/components/LiveActivityFeed/LiveActivityFeed.tsx` y su SCSS module
- [x] T028 [P] [US6] Implementar barra de atajos `QuickActionsBar` en `frontend/src/features/dashboard/components/QuickActionsBar/QuickActionsBar.tsx` y su SCSS module

---

## Phase 9: User Story 7 - Estados Vacíos, Carga Inicial y Resiliencia (Priority: P3)

**Objetivo**: Experiencia de carga suave (skeletons sin CLS), empty state motivador para nuevos usuarios y panel de reintento.
**Test independiente**: Navegar a `/home` con un usuario nuevo; comprobar que Net Worth refleja el saldo de bienvenida y los gráficos despliegan el empty state con botón a `/players`.

- [x] T029 [P] [US7] Implementar esqueletos de carga en `frontend/src/features/dashboard/components/DashboardSkeleton/DashboardSkeleton.tsx` y su SCSS module
- [x] T030 [P] [US7] Implementar `DashboardEmptyState` con mensaje de bienvenida y llamada a invertir en `frontend/src/features/dashboard/components/DashboardEmptyState/DashboardEmptyState.tsx` y su SCSS module
- [x] T031 [US7] Implementar manejo de errores con panel de reintento manual ante fallos de conexión en `frontend/src/features/dashboard/pages/DashboardPage/DashboardPage.tsx`

---

## Phase 10: Integración Final, Enrutamiento y Verificación

**Propósito**: Ensamblado final, sustitución de la vista en `/home` y validación de compilación.

- [x] T032 Ensamblar `DashboardPage` integrando los 6 bloques en `frontend/src/features/dashboard/pages/DashboardPage/DashboardPage.tsx` y su SCSS module
- [x] T033 Exportar componentes y páginas públicas en `frontend/src/features/dashboard/index.ts`
- [x] T034 Actualizar enrutador en `frontend/src/app/router/AppRouter/AppRouter.tsx` reemplazando `HomePage` temporal por `DashboardPage` bajo `AuthGuard`
- [x] T035 Validar compilación y estilo frontend con `pnpm biome:check` y `pnpm build`
- [x] T036 Validar compilación backend con `./gradlew compileJava classes`

---

## Dependencies & Execution Order

1. **Phase 1 (Setup)** $\rightarrow$ **Phase 2 (Foundational)**: Bloquean toda la implementación posterior.
2. **Phase 3 (US1 - MVP)**: Puede iniciarse y verificarse de inmediato una vez completada la Fase 2.
3. **Phases 4, 5, 6, 7, 8 (US2 a US6)**: Pueden ejecutarse en paralelo o en secuencia incremental.
4. **Phase 9 (US7 - Resiliencia)**: Se acopla a las respuestas y estados de carga de las historias anteriores.
5. **Phase 10 (Integración y Entrega)**: Ensamblado final, comprobación de linter Biome y build Gradle.

---

## Resumen Cuantitativo de Tareas

- **Total de tareas**: 36 tareas (`T001` a `T036`).
- **Tareas paralelizable [P]**: 19 tareas.
- **Distribución por historia**:
  - *Setup & Foundational*: 14 tareas (`T001` - `T014`)
  - *US1 (Patrimonio & Liquidez - MVP)*: 3 tareas (`T015` - `T017`)
  - *US2 (Donut de Asignación)*: 3 tareas (`T018` - `T020`)
  - *US3 (Benchmark FMI & Top Movers)*: 2 tareas (`T021` - `T022`)
  - *US4 (Market Ticker)*: 1 tarea (`T023`)
  - *US5 (Showcase Sorare)*: 2 tareas (`T024` - `T025`)
  - *US6 (Mercado P2P & Feed)*: 3 tareas (`T026` - `T028`)
  - *US7 (Empty States & Skeletons)*: 3 tareas (`T029` - `T031`)
  - *Integración y Verificación*: 5 tareas (`T032` - `T036`)

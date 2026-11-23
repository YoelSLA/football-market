# Tasks: UI de venta inmediata oficial y selector de precio flexible en P2P (SPEC-027)

**Input**: Documentos de diseño desde `/specs/027-frontend-instant-buyback-and-p2p-pricing/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)  

**Tests**: Conforme a la Constitución y normas del proyecto, el frontend está exento de testing: MUST NOT crear tests ni infraestructura frontend. Los controles permitidos son la verificación estática de linter y compilación (`npm run build`).

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo (tríada obligatoria permanente junto a `spec.md` y `plan.md`); no se exportan a tarjetas de seguimiento. Generar Tasks no completa implementación ni habilita Ready: requiere ANALYZE OK y posterior IMPLEMENT AUTHORIZATION humana explícita.

**Organization**: Las tareas están agrupadas por fase y por User Story para habilitar la implementación y verificación independiente de cada incremento.

## Format: `[ID] [P?] [Story] Description with file path`

- **[P]**: Ejecutable en paralelo (archivos distintos, sin dependencias incompletas)
- **[Story]**: User Story a la que pertenece ([US1], [US2], [US3], [US4])
- Rutas de archivo exactas en cada descripción

---

## Phase 1: Setup & Data Structures (Shared Infrastructure)

**Purpose**: Definir modelos, interfaces TypeScript, DTOs y mappers compartidos que sustentan las User Stories.

- [ ] T001 [P] Definir tipos de dominio `SellMode`, `SellOfficialResult`, `SellOfficialEstimation`, `SellP2PEstimation` y `P2POpportunity` en `frontend/src/features/market/types/models.ts`
- [ ] T002 [P] Definir DTOs `OrderSellOfficialRequestDTO`, `OrderSellOfficialResponseDTO` y agregar `unitPrice` opcional a `CreateListingRequestDTO` en `frontend/src/features/market/types/dtos.ts`
- [ ] T003 [P] Implementar funciones mapper para transformar respuestas de venta oficial y calcular deltas de oportunidad en `frontend/src/features/market/market.mapper.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura de servicios y mutaciones HTTP que bloquean la implementación de las vistas.

**⚠️ CRITICAL**: Completar antes de implementar los componentes de las User Stories.

- [ ] T004 Implementar método de servicio HTTP `sellTokensOfficial(request)` y actualizar `sellTokens(request)` con `unitPrice` en `frontend/src/features/market/market.service.ts`
- [ ] T005 [P] Crear hook de mutación `useSellOfficialTokensMutation` en `frontend/src/features/market/hooks/mutations/useSellOfficialTokensMutation.ts`
- [ ] T006 [P] Actualizar hook de mutación `useSellTokensMutation` para admitir `unitPrice` opcional en `frontend/src/features/market/hooks/mutations/useSellTokensMutation.ts`

**Checkpoint**: Base de datos y servicios lista — la implementación de componentes de User Stories puede comenzar.

---

## Phase 3: User Story 1 — Selector de modalidad y Venta Inmediata Oficial (Priority: P1) 🎯 MVP

**Goal**: Permitir seleccionar la modalidad "Venta Inmediata Oficial" en `SellTokenModal`, ver el desglose en tiempo real con spread del 10%, confirmar contra `POST /api/orders/sell-official` y reflejar reactivamente los créditos y tenencias del usuario.

**Independent Test**: Abrir modal de venta de un jugador en `/market/sell`, comprobar selección oficial por defecto con spread del 10%, confirmar venta oficial, verificar comprobante de éxito y comprobar actualización inmediata del saldo de créditos en el header.

### Implementation for User Story 1

- [ ] T007 [P] [US1] Crear componente accesible `SellModeSelector` con tabs segmentadas (`OFFICIAL` vs `P2P`) en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/SellModeSelector.tsx`
- [ ] T008 [P] [US1] Crear componente `SellModalOfficialEstimation` con desglose en tiempo real de cotización oficial, spread del 10%, valor unitario neto y créditos netos en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/SellModalOfficialEstimation.tsx`
- [ ] T009 [US1] Actualizar hook `useMarketSellPage` para incorporar estado `sellMode`, mutación `useSellOfficialTokensMutation`, desglose de venta oficial e invalidación reactiva de queries (`userCredits`, `userHoldings`, `portfolioSummary`) en `frontend/src/features/market/hooks/pages/useMarketSellPage.ts`
- [ ] T010 [US1] Actualizar `SellModalFooter` para adaptar botón de confirmación ("Confirmar venta inmediata") y estado `isBusy` según `sellMode` en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/SellModalFooter.tsx`
- [ ] T011 [US1] Integrar `SellModeSelector`, `SellModalOfficialEstimation` y el flujo de confirmación oficial en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/SellTokenModal.tsx`

---

## Phase 4: User Story 2 — Selector de precio flexible en Publicación P2P (Priority: P1)

**Goal**: En modalidad P2P, proporcionar control interactivo de precio (slider + input numérico) acotado a $[-20\%, +20\%]$ e inicializado en -10%, calcular importe total y enviar `unitPrice` en `POST /api/listings`.

**Independent Test**: En el modal de venta, conmutar a modalidad P2P, verificar inicialización en -10% con badge de oferta, mover slider entre -20% y +20%, comprobar bloqueo fuera de banda y confirmar publicación P2P con el `unitPrice` acordado.

### Implementation for User Story 2

- [ ] T012 [P] [US2] Crear componente interactivo `P2PPriceSelector` con slider HTML5 nativo, input numérico y botones de salto rápido (-20%, -10%, 0%, +10%, +20%) en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/P2PPriceSelector.tsx`
- [ ] T013 [P] [US2] Crear estilos SCSS para el selector de precio en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/P2PPriceSelector.module.scss`
- [ ] T014 [US2] Extender `useMarketSellPage` con estado `p2pUnitPriceCentimes`, cálculo de variaciones, límites de banda y transmisión de `unitPrice` en la mutación P2P en `frontend/src/features/market/hooks/pages/useMarketSellPage.ts`
- [ ] T015 [US2] Integrar `P2PPriceSelector` en la vista de publicación comunitaria de `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/SellTokenModal.tsx`

---

## Phase 5: User Story 3 — Badges de Oportunidad y Arbitraje en Catálogo P2P (Priority: P2)

**Goal**: En `MarketP2PPage`, mostrar badges comparativos con la cotización oficial (`🏷️ -X% Oferta` verde esmeralda y `🔥 +X% Premium` naranja/rojo) con porcentaje redondeado al entero más cercano.

**Independent Test**: Navegar a `/market/p2p`, verificar que publicaciones con precio menor al oficial exhiben badge de oferta con ahorro exacto, publicaciones con sobreprecio exhiben badge premium y publicaciones a precio oficial no muestran badge.

### Implementation for User Story 3

- [ ] T016 [P] [US3] Crear componente `P2POpportunityBadge` con lógica de cálculo delta y estilos visuales verde/naranja en `frontend/src/features/market/pages/MarketP2PPage/components/P2POpportunityBadge/P2POpportunityBadge.tsx`
- [ ] T017 [P] [US3] Crear estilos SCSS encapsulados para el badge en `frontend/src/features/market/pages/MarketP2PPage/components/P2POpportunityBadge/P2POpportunityBadge.module.scss`
- [ ] T018 [US3] Integrar `P2POpportunityBadge` en las tarjetas y filas de órdenes de `frontend/src/features/market/pages/MarketP2PPage/components/CommunityMarketPanel/CommunityMarketPanel.tsx`

---

## Phase 6: User Story 4 — Feedback, Manejo de Errores y Usabilidad Desktop (Priority: P2)

**Goal**: Garantizar comprobante detallado (`SellModalSuccess`) y toast global en paralelo, deshabilitación durante carga (`isBusy`), mensajes descriptivos de error sin cerrar modal y layout exclusivo desktop ($\ge 1024\text{px}$).

**Independent Test**: Ejecutar venta oficial y verificar comprobante con créditos acreditados junto a toast flotante; simular error de red y comprobar alerta visible sin cierre de modal en resolución desktop $\ge 1024\text{px}$.

### Implementation for User Story 4

- [ ] T019 [P] [US4] Actualizar componente `SellModalSuccess` para presentar el comprobante de venta oficial detallando créditos netos percibidos y saldo actualizado en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/SellModalSuccess.tsx`
- [ ] T020 [US4] Asegurar emisión de toast global (`useMarketToast`) simultánea al comprobante en `frontend/src/features/market/hooks/pages/useMarketSellPage.ts`
- [ ] T021 [US4] Revisar estilos en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/SellTokenModal.module.scss` para asegurar layout desktop estricto ($\ge 1024\text{px}$) y contraste accesible (WCAG 2.1 AA)

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificaciones de consistencia, compilación y validación cruzada.

- [ ] T022 [P] Ejecutar verificación de tipos y linter Biome en `frontend/`
- [ ] T023 Ejecutar compilación de frontend (`npm run build` en `frontend/`) verificando ausencia de errores
- [ ] T024 Recorrer y validar los 4 escenarios interactivos de `specs/027-frontend-instant-buyback-and-p2p-pricing/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: Sin dependencias — inicia inmediatamente.
- **Phase 2 (Foundational)**: Depende de Phase 1 — BLOQUEA la implementación de las User Stories.
- **Phase 3 (US1 - MVP)**: Depende de Phase 2. Puede entregarse y validarse como incremento independiente.
- **Phase 4 (US2)**: Depende de Phase 2 y se integra sobre el modal de Phase 3.
- **Phase 5 (US3)**: Depende de Phase 1 y 2. Puede implementarse en paralelo con Phase 3 y 4 al afectar componentes de `MarketP2PPage`.
- **Phase 6 (US4)**: Depende de Phase 3 y 4.
- **Phase 7 (Polish)**: Depende de todas las historias completadas.

### Parallel Opportunities
- T001, T002, T003 pueden ejecutarse en paralelo (Phase 1).
- T005 y T006 pueden ejecutarse en paralelo tras T004 (Phase 2).
- T007 y T008 pueden ejecutarse en paralelo antes de la integración en T011 (Phase 3).
- T012 y T013 pueden ejecutarse en paralelo antes de T015 (Phase 4).
- T016 y T017 pueden ejecutarse en paralelo antes de T018 (Phase 5).

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Completar Phase 1: Setup & Data Structures (T001 - T003).
2. Completar Phase 2: Foundational (T004 - T006).
3. Completar Phase 3: User Story 1 (T007 - T011).
4. Validar MVP: Venta Inmediata Oficial funcional con acreditación de créditos y tenencias.

### Incremental Delivery
1. Setup + Foundational $\rightarrow$ Infraestructura lista.
2. US1 $\rightarrow$ Venta oficial inmediata (MVP).
3. US2 $\rightarrow$ Selector de precio flexible P2P ($\pm 20\%$).
4. US3 $\rightarrow$ Badges de oportunidad en catálogo P2P.
5. US4 $\rightarrow$ Feedback dual y endurecimiento desktop.
6. Polish $\rightarrow$ Build y validación de `quickstart.md`.

---
description: "Tareas vigentes para integrar y rediseñar los mercados Oficial y P2P"
---

# Tasks: Comprar tokens en el mercado (SPEC-019)

**Input**: Documentación vigente de `/specs/019-buy-tokens-from-frontend/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, checklists evaluados y wireframes aprobados.

**Tests**: No crear tests ni infraestructura frontend. No hay cambios backend planificados, por lo que no corresponden tests backend. La validación manual está en `quickstart.md` y pertenece al usuario. Tras modificar código frontend se permiten `npm run build` y `npm run biome:fix`, sin considerarlos tests.

**Workflow local**: Este archivo contiene únicamente trabajo técnico pendiente. El historial anterior permanece en Git y en la evidencia protegida. TASKS no autoriza implementación; el siguiente paso es repetir ANALYZE.

**Organization**: Tareas agrupadas por historia y por fase obligatoria del frontend. La lógica, el estado, el JSX semántico y la accesibilidad estructural deben quedar validados antes del gate humano; la maquetación fina se ejecuta solo después de su autorización.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Ejecutable en paralelo por modificar archivos distintos sin dependencia incompleta.
- **[Story]**: Historia de usuario de `spec.md`.
- Cada tarea incluye rutas exactas.

## Phase 1: Setup (Shared Configuration)

**Purpose**: Centralizar configuración, tipos y APIs públicas nuevas sin alterar contratos existentes.

- [X] T001 [P] Definir página oficial de 8, página P2P de 12 y debounce P2P de 300 ms en `frontend/src/features/market/constants/marketConfig.ts` y `frontend/src/features/market/constants/index.ts`
- [X] T002 [P] Extender DTOs y Models con catálogo oficial, criterios oficiales y estado URL P2P en `frontend/src/features/market/types/dtos.ts`, `frontend/src/features/market/types/models.ts` y `frontend/src/features/market/types/index.ts`
- [X] T003 [P] Publicar `usePlayerCatalogFiltersQuery` y `PlayerCatalogFilters` mediante los barrels vigentes `frontend/src/features/players/hooks/index.ts`, `frontend/src/features/players/types/index.ts` y `frontend/src/features/players/index.ts`, sin exponer Service, Mapper ni DTOs

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Completar fronteras de datos y navegación comunes.

- [X] T004 Incorporar validación y mapeo estricto del catálogo oficial preservando listings y compra en `frontend/src/features/market/market.mapper.ts`
- [X] T005 Extender el Service con `getOfficialMarket(criteria, signal)` y preservar `getListings`, `buyTokens` y `buyListing` en `frontend/src/features/market/market.service.ts`
- [X] T006 Crear parseo y serialización URL oficial, incluidos créditos↔centésimas y rango inválido, en `frontend/src/features/market/utils/officialMarketCriteria.ts` y exportarlo desde `frontend/src/features/market/utils/index.ts`
- [X] T007 Crear parseo y serialización de `search`, `league`, `position`, `sort`, `page` y `player` P2P en `frontend/src/features/market/utils/communityMarketCriteria.ts` y exportarlo desde `frontend/src/features/market/utils/index.ts` después de T006

**Checkpoint**: DTO → Mapper → Service, APIs públicas deportivas y URL → Criteria disponibles.

---

## Phase 3: User Story 1 - Explorar el Mercado Oficial (Priority: P1) 🎯 MVP

**Goal**: Catálogo consolidado con filtros automáticos, URL reconstruible y paginación oficial de 8.

**Independent Test**: Combinar los ocho controles, orden y página en `/market/buy`, recargar y usar Back/Forward con una única lectura consolidada de ocho jugadores y sin GET individual de cotizaciones; `/market/sell` conserva sus filtros y comportamiento vigentes.

- [X] T008 [US1] Extender query keys y crear `useOfficialMarketQuery` con AbortSignal en `frontend/src/features/market/constants/marketQueryKeys.ts`, `frontend/src/features/market/hooks/queries/useOfficialMarketQuery.ts` y `frontend/src/features/market/hooks/queries/index.ts`
- [X] T009 [US1] Refactorizar URL, borradores, debounce, filtros en cascada mediante la API pública de players, query consolidada y selección vigente en `frontend/src/features/market/hooks/pages/useMarketBuyPage.ts`
- [X] T010 [P] [US1] Separar `OfficialMarketFilterFields` y `SellMarketFilterFields` bajo props discriminadas en `frontend/src/features/market/components/MarketFilters/`, reutilizando búsqueda y select y manteniendo sin cambios el comportamiento consumido por `frontend/src/features/market/pages/MarketSellPage/MarketSellPage.tsx`
- [X] T011 [P] [US1] Adaptar header, card y catálogo a identidad deportiva y cotización nullable en `frontend/src/features/market/pages/MarketBuyPage/components/MarketHeader/MarketHeader.tsx`, `frontend/src/features/market/pages/MarketBuyPage/components/MarketPlayerCard/MarketPlayerCard.tsx` y `frontend/src/features/market/pages/MarketBuyPage/components/MarketBuyCatalog/MarketBuyCatalog.tsx`
- [X] T012 [US1] Componer carga, actualización, error, vacíos, grilla y paginación externa al scroll en `frontend/src/features/market/pages/MarketBuyPage/components/OfficialMarketPanel/OfficialMarketPanel.tsx`
- [X] T013 [US1] Integrar el Page Hook consolidado y retirar catálogo+quotes N+1 de `frontend/src/features/market/pages/MarketBuyPage/MarketBuyPage.tsx`

**Checkpoint**: US1 estructuralmente verificable sin comprar y sin regresión contractual en Mercado de Venta.

---

## Phase 4: User Story 2 - Comprar emisión oficial (Priority: P1)

**Goal**: Comprar desde la cotización embebida con cantidad 1–100 y resultado real.

**Independent Test**: Abrir una card cotizada, modificar cantidad, confirmar una sola solicitud y observar resultado e invalidaciones sin GET adicional.

- [X] T014 [P] [US2] Adaptar `BuyTokenModal` a los datos aprobados, controles ± y accesos absolutos en `frontend/src/features/market/pages/MarketBuyPage/components/BuyTokenModal/BuyTokenModal.tsx`
- [X] T015 [US2] Reutilizar formulario/mutación con cotización embebida, guardas e invalidaciones de oficial/usuario/portfolio en `frontend/src/features/market/hooks/pages/useMarketBuyPage.ts`
- [X] T016 [US2] Retirar consumidores N+1 y eliminar query/key individual solo si quedan sin uso en `frontend/src/features/market/hooks/queries/useMarketQuotesQuery.ts`, `frontend/src/features/market/hooks/queries/index.ts` y `frontend/src/features/market/constants/marketQueryKeys.ts`

---

## Phase 5: User Story 3 - Explorar ofertas del Mercado P2P (Priority: P1)

**Goal**: Catálogo agrupado, filtros/orden local, página de 12 y navegación URL completa.

**Independent Test**: `search` se publica a 300 ms, liga/posición/orden inmediatamente, y recarga/Back/Forward reconstruyen catálogo y página sin otro GET de listings.

- [X] T017 [P] [US3] Endurecer agrupación, sumas seguras, orden determinista y filtrado previo a paginación en `frontend/src/features/market/utils/communityMarket.ts`
- [X] T018 [US3] Derivar filtros/página/player desde URL, aplicar debounce 300 ms y normalizar página en `frontend/src/features/market/hooks/pages/useCommunityMarketPage.ts`
- [X] T019 [P] [US3] Conectar filtros horizontales automáticos a URL sin parámetros backend en `frontend/src/features/market/pages/MarketP2PPage/components/CommunityMarketFilters.tsx`
- [X] T020 [P] [US3] Adaptar catálogo, cards, total único y paginación local en `frontend/src/features/market/pages/MarketP2PPage/components/CommunityMarket.tsx` y `frontend/src/features/market/pages/MarketP2PPage/components/CommunityMarketPanel.tsx`
- [X] T021 [US3] Integrar header, saldo y catálogo navegable en `frontend/src/features/market/pages/MarketP2PPage/MarketP2PPage.tsx` y `frontend/src/features/market/pages/MarketP2PPage/components/MarketP2PHeader/MarketP2PHeader.tsx`

---

## Phase 6: User Story 4 - Elegir y comprar una oferta P2P (Priority: P1)

**Goal**: Detalle completo y compra exclusiva de lote completo.

**Independent Test**: Abrir `?player=<id>`, confirmar `{ listingId }`, conservar éxito hasta cierre y refrescar conflictos sin editar cantidad ni reintentar POST.

- [X] T022 [P] [US4] Adaptar detalle a listings ya cargados y estado sin ofertas en `frontend/src/features/market/pages/MarketP2PPage/components/CommunityListingsPanel.tsx`
- [X] T023 [P] [US4] Adaptar `BuyListingModal` a lote de solo lectura y éxito persistente en `frontend/src/features/market/pages/MarketP2PPage/components/BuyListingModal.tsx`
- [X] T024 [US4] Preservar compra por `listingId`, doble guarda, incertidumbre sin retry e invalidaciones en `frontend/src/features/market/hooks/pages/useCommunityMarketPage.ts`

---

## Phase 7: User Story 5 - Accesibilidad estructural y estados (Priority: P2)

**Goal**: Dejar ambos mercados y diálogos semánticamente operables antes de la maquetación fina.

**Independent Test**: Inspeccionar estructura, orden de foco, nombres accesibles, anuncios y guardas de cierre sin depender del SCSS definitivo.

- [X] T025 [P] [US5] Añadir semántica de estados, `aria-busy`, alertas, foco y operación por teclado a los componentes TSX oficiales modificados en `frontend/src/features/market/pages/MarketBuyPage/` y a la variante oficial de `frontend/src/features/market/components/MarketFilters/`, preservando la semántica de Mercado de Venta
- [X] T026 [P] [US5] Añadir semántica de catálogo/detalle, `aria-busy`, alertas, foco y operación por teclado a los componentes TSX P2P modificados en `frontend/src/features/market/pages/MarketP2PPage/`
- [X] T027 [US5] Auditar el contrato vigente de foco, Escape, overlay y bloqueo durante submit en `frontend/src/shared/components/Modal/Modal.tsx`, modificándolo solo ante una regresión concreta reproducible por inspección estática y sin alterar consumidores ajenos a SPEC-019

---

## Phase 8: Frontend Phase 1 Exit & Mandatory Human Gate

**Purpose**: Cerrar lógica, estado, estructura y accesibilidad antes de cualquier maquetación fina.

- [X] T028 Retirar únicamente código muerto confirmado de la Fase 1 y ajustar exports en `frontend/src/features/market/index.ts`, `frontend/src/features/market/hooks/index.ts`, `frontend/src/features/market/pages/index.ts` y barrels explícitamente afectados
- [X] T029 Ejecutar `npm run build` y `npm run biome:fix` desde `frontend/` sobre la Fase 1, revisar salidas generadas y corregir solo hallazgos del alcance antes de detenerse en el gate humano obligatorio de `frontend/AGENTS.md`

**🛑 Mandatory Gate**: Detener la implementación y esperar autorización humana explícita. T030–T035 no pueden comenzar aunque T029 resulte satisfactorio.

---

## Phase 9: Frontend Phase 2 - Fine Layout and Visual Accessibility

**Purpose**: Aplicar exclusivamente maquetación, wrappers visuales permitidos y SCSS conforme a los wireframes aprobados, sin modificar contratos, datos, eventos ni atributos accesibles validados en Fase 1.

- [X] T030 [US1] Aplicar en desktop un grid oficial de cuatro columnas y dos filas, controles de 38 px y altura automática mediante clases semánticas en `frontend/src/features/market/components/MarketFilters/`, preservando los estilos vigentes de `MarketSellPage`
- [X] T031 [US2] Ajustar modal oficial y estados al wireframe en `frontend/src/features/market/pages/MarketBuyPage/components/BuyTokenModal/BuyTokenModal.module.scss`
- [X] T032 [US3] Aplicar filtros 94 px, cuatro columnas y paginación externa al scroll en los módulos SCSS de `frontend/src/features/market/pages/MarketP2PPage/`
- [X] T033 [US4] Aplicar dimensiones de detalle, publicaciones, modal y éxito en `frontend/src/features/market/pages/MarketP2PPage/components/CommunityListingsPanel.module.scss` y `frontend/src/features/market/pages/MarketP2PPage/components/BuyListingModal.module.scss`
- [X] T034 [US5] Ajustar mediante wrappers puramente visuales y SCSS la altura y el scroll de ambas rutas sin afectar otras páginas en `frontend/src/app/layouts/AppLayout/AppLayout.tsx`, `frontend/src/app/layouts/AppLayout/AppLayout.module.scss`, `frontend/src/features/market/pages/MarketBuyPage/MarketBuyPage.module.scss` y `frontend/src/features/market/pages/MarketP2PPage/MarketP2PPage.module.scss`
- [X] T035 [US5] Corregir únicamente regresiones visuales comprobadas de overlay y scroll interno en `frontend/src/shared/components/Modal/Modal.module.scss`, sin modificar la lógica ni los atributos accesibles de `Modal.tsx`

---

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T036 Ejecutar `npm run build` y `npm run biome:fix` desde `frontend/` tras la Fase 2, revisar salidas generadas y corregir solo hallazgos del alcance en `frontend/src/`
- [ ] T037 Contrastar estáticamente ambos flujos con `specs/019-buy-tokens-from-frontend/quickstart.md`, revisar compatibilidad de `/market/sell`, ejecutar `git diff --check` y `git status --short`, y registrar desviaciones sin afirmar tests no ejecutados

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup T001–T003 → Foundation T004–T007.
- T007 depende de T006 porque ambos actualizan `frontend/src/features/market/utils/index.ts`; no son paralelizables.
- US1 depende de T003–T006; US3 depende de T001–T005 y T007.
- US2 depende de US1; US4 depende de US3.
- US1 y US3 pueden avanzar en paralelo una vez satisfechas sus dependencias fundacionales.
- US5 estructural depende del JSX final de US1–US4.
- T028–T029 dependen de toda la Fase 1 y deben completarse antes del gate humano.
- T030–T035 dependen de T029 y de autorización humana explícita.
- T036–T037 dependen de toda la Fase 2 incluida en el alcance.

### MarketFilters Compatibility

- T010 mantiene controles compartidos y separa las composiciones oficial y de venta mediante props discriminadas; no cambia defaults ni callbacks usados por `MarketSellPage`.
- T025 preserva la semántica vigente de venta al incorporar accesibilidad específica de la variante oficial.
- T030 limita selectores y estilos nuevos a la variante oficial y T037 revisa explícitamente la ausencia de regresiones en `/market/sell`.
- Si la implementación exigiera romper props, defaults o estructura consumida por venta, debe detenerse y solicitar una decisión arquitectónica antes de continuar.

### Frontend Phase Gate

- **Fase 1**: T001–T029 cubren contratos, APIs públicas, estado, Hooks, props, JSX semántico, accesibilidad estructural, limpieza y controles técnicos.
- **Gate**: después de T029 la ejecución se detiene aunque build y Biome finalicen sin errores.
- **Fase 2**: solo con autorización explícita se ejecutan T030–T035; estas tareas no pueden alterar lógica, eventos, contratos ni ARIA de Fase 1.
- **Cierre**: T036–T037 verifican el resultado técnico y la regresión cruzada, sin sustituir validación manual ni tests.

### Parallel Opportunities

- T001/T002/T003 pueden avanzar en paralelo.
- US1 y US3 pueden avanzar en paralelo tras Foundation.
- T010/T011, T014 junto a T015, T019/T020, T022/T023 y T025/T026 modifican archivos distintos.
- Tras el gate, T030–T033 pueden avanzar en paralelo; T034–T035 deben coordinarse por afectar layout o componentes compartidos.

## Parallel Examples

```text
Oficial: T008 → T009 → T010/T011 → T012 → T013
P2P:     T017 → T018 → T019/T020 → T021
Compra:  T014/T015 → T016 || T022/T023 → T024
Gate:    T025/T026 → T027 → T028 → T029 → PAUSA HUMANA
Visual:  autorización → T030/T031/T032/T033 → T034/T035 → T036 → T037
```

## Implementation Strategy

1. Setup + Foundation.
2. Estructura Oficial: US1 → US2.
3. Estructura P2P en paralelo o después: US3 → US4.
4. Accesibilidad estructural transversal: US5.
5. Limpieza y controles de Fase 1; detenerse en el gate.
6. Con autorización, ejecutar maquetación fina y cierre técnico.

## Notes

- Solo trabajo pendiente; los flujos y componentes existentes se reutilizan donde satisfacen el diseño.
- No crear tests frontend ni modificar backend mientras los contratos vigentes alcancen.
- No convertir CODE REVIEW, TESTING, validación manual o autorización visual en tareas técnicas.
- TASKS no autoriza implementación ni realiza commits, push o merges.

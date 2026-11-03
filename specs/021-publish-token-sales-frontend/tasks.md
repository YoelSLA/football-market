# Tasks: SPEC-021 Publicar ventas de tokens desde el frontend

**Input**: Design documents from `/specs/021-publish-token-sales-frontend/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/
**Tests**: Frontend exento de tests automatizados (Constitution §19.199, plan §Technical Context). MUST NOT crear tests ni infraestructura frontend. Validación manual exclusivamente mediante quickstart.md, ejecución a cargo del usuario.
**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `frontend/src/`
- Paths below assume `frontend/src/` per plan.md project structure

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and verification of existing SPEC-019 market feature to extend.

- [X] T001 [P] Verify frontend project structure and existing SPEC-019 market feature (frontend/src/features/market/) per plan.md Project Structure
- [X] T002 [P] Confirm required dependencies available (React Hook Form, Zod, TanStack Query 5, Axios, React-Toastify 11) per plan.md Technical Context — no new dependencies in frontend/package.json

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented. SPEC-021 extends the existing market feature; no database/backend work (backend out of scope per spec.md Out of Scope).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 [P] Add "Vender tokens" tab in navigation in frontend/src/app/navigation/sections.ts (path /market/sell)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Publicar una venta de tokens (Priority: P1) 🎯 MVP

**Goal**: Usuario autenticado inicia la publicación de venta de tokens de un jugador de su tenencia, comunicando condiciones al backend mediante `POST /api/orders/sell`, con precio obtenido de cotización vigente y congelado al crear. Cubre FR-F01, FR-F02 (listado con disponibles/reservadas, incluso cero), FR-F03 y FR-F11 (filtros de compra reutilizados, filtro por disponibilidad, ordenamiento por tenencias).

**Independent Test**: Con un usuario habilitado que posea tokens libres de un jugador visible, publicar una cantidad válida y comprobar que la publicación existe, su precio queda congelado y los tokens quedan reservados pero siguen siendo del vendedor (spec.md CA-01).

### Implementation for User Story 1

- [X] T004 [US1] Create MarketSellPage page in frontend/src/features/market/pages/MarketSellPage/MarketSellPage.tsx and export it from frontend/src/features/market/index.ts (reutiliza componentes de SPEC-019, sin Hooks en componentes locales)
- [X] T005 [US1] Register `/market/sell` under AuthGuard + AppLayout in frontend/src/app/router/AppRouter/AppRouter.tsx after MarketSellPage is available from the public market feature entry point (depends on T004)
- [X] T006 [US1] Compose catalog and holdings in frontend/src/app/router/AppRouter/AppRouter.tsx: expose the existing usePortfolioHoldingsQuery through frontend/src/features/portfolio/hooks/index.ts and frontend/src/features/portfolio/index.ts (from hooks/queries/index.ts), and only the required Model types through the public types barrels; app imports from the root public APIs of players/portfolio, queries OPEN holdings including fully reserved positions, maps Player to MarketPlayer and quantity/availableQuantity/reservedQuantity to market-owned presentation props, and passes onSellSuccess(playerId) and onRefreshHoldings(playerId). Preserve existing portfolio requests, pagination and query keys; no Service/DTO exposure, deep imports, new endpoint or cross-feature import from market (depends on T004, T005)

- [X] T007 [P] [US1] Add OrderSellRequestDTO (`playerId: number`, `quantity: number`) and OrderSellResponseDTO (`listingId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `status`, `createdAt`, `expiresAt`) in frontend/src/features/market/types/dtos.ts per contracts/ and data-model.md
- [X] T008 [P] [US1] Add SellResult, SellQuantityForm, SellEstimation models in frontend/src/features/market/types/models.ts per data-model.md (SellResult sin `playerName`)
- [X] T009 [US1] Add toOrderSellRequestDTO and toSellResponseDTO mappers (DTO → Model dentro del Service) in frontend/src/features/market/market.mapper.ts per data-model.md
- [X] T010 [US1] Implement sellTokens({ playerId, quantity }) → POST /api/orders/sell in frontend/src/features/market/market.service.ts (depends on T007, T009; autenticado, sin `listingId` de entrada, sin `playerId` de usuario)
- [X] T011 [P] [US1] Add sellQuantitySchema Zod validation (entero 1-100, texto vacío/no numérico/decimal/fuera de rango rechazados, sin coerción de tipos ni redondeo) in frontend/src/features/market/form/schemas/sellQuantitySchema.ts per spec.md CA-03 and data-model.md
- [X] T012 [P] [US1] Create useSellQuantityForm hook (cantidad inicial "1", controles −/+, accesos preestablecidos 1/2/5/Máx, límite superior min(100, tokens disponibles)) in frontend/src/features/market/form/hooks/useSellQuantityForm.ts per spec.md FR-F03
- [X] T013 [US1] Implement useSellTokensMutation for POST /orders/sell with duplicate prevention (isSubmittingRef + isPending mutation state, Confirmar/Cancelar/ESC/overlay deshabilitados mientras envía, aria-busy + spinner) in frontend/src/features/market/hooks/mutations/useSellTokensMutation.ts per spec.md CA-15, FR-F08 (depends on T010)
- [X] T014 [US1] Render listado de jugadores con cantidades disponibles y reservadas, incluyendo jugadores con disponibilidad cero (tenencia totalmente reservada) in frontend/src/features/market/pages/MarketSellPage/MarketSellPage.tsx per spec.md FR-F02, CA-02 (depends on T004) — G3
- [X] T015 [US1] Reutilizar la presentación de frontend/src/features/market/pages/MarketBuyPage/MarketFilters/ moviéndola al módulo compartido interno frontend/src/features/market/components/MarketFilters/ con su barrel y estilos; actualizar el consumidor de compra sin cambiar su comportamiento deshabilitado. Para venta, recibir valores, opciones y callbacks por props, sin Hooks en el componente; frontend/src/features/market/hooks/pages/useMarketSellPage.ts coordina búsqueda/filtros existentes, aplicación y limpieza, disponibilidad de tokens y ordenamiento por tenencia total/disponibles/reservados, y frontend/src/features/market/pages/MarketSellPage/MarketSellPage.tsx compone los controles funcionales. No asumir lógica de filtrado ya implementada en compra, no habilitarla allí ni agregar filtros o endpoints fuera de FR-F11 (depends on T014; coordinación de venta con T016) — G1

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Confirmación y resumen de publicación (Priority: P1)

**Goal**: Usuario revisa las condiciones de su publicación antes de confirmar explícitamente, para evitar envíos involuntarios. Cubre FR-F04 (resumen + confirmación expresa) y FR-F09 (estado de éxito persistente dentro del modal con datos definitivos del backend).

**Independent Test**: El usuario completa todos los campos del formulario de publicación, visualiza el resumen y hace clic en "Confirmar". El sistema debe crear la publicación. Si el usuario hace clic en "Cancelar", no debe crearse ninguna publicación (spec.md CA-12, CA-13, CA-14).

### Implementation for User Story 2

- [X] T016 [US2] Implement useMarketSellPage coordination (form, quote query and mutation state, duplicate prevention; quote invalidation is handled once by T025) in frontend/src/features/market/hooks/pages/useMarketSellPage.ts per plan.md and data-model.md (depends on T012, T013)
- [X] T017 [US2] Create SellTokenModal and display quantity × unit price estimation ("Importe estimado: 5,00 créditos") clearly labeled as estimative, NOT frozen/guaranteed price, in frontend/src/features/market/pages/MarketSellPage/SellTokenModal/SellTokenModal.tsx per quickstart US1 and data-model.md SellEstimation (depends on T004)
- [X] T018 [US2] Implement resumen previo al envío con jugador, cantidad, precio unitario e importe total estimados claramente identificados; no mostrar precio congelado ni vencimiento definitivo antes del POST; requerir confirmación explícita del usuario antes de solicitar la publicación al backend in frontend/src/features/market/pages/MarketSellPage/SellTokenModal/SellTokenModal.tsx per spec.md CA-12, CA-14, FR-F04 (depends on T017)
- [X] T019 [US2] Al activar Confirmar, forzar refetch de GET /api/players/{id}/quotes/current antes del POST; requerir estado disponible y precio positivo. Si el precio difiere del que el usuario revisó, actualizar estimación y detener el POST hasta que el usuario revise y confirme de nuevo. Si coincide, enviar POST. Documentar la carrera restante entre GET y POST: el backend aplica el precio vigente autoritativamente y su respuesta manda para el éxito in frontend/src/features/market/hooks/pages/useMarketSellPage.ts per spec.md FR-F03, research.md Riesgo 1 (depends on T016)
- [X] T020 [US2] Estado de éxito persistente dentro del modal usando la respuesta del POST como fuente definitiva: mostrar jugador, cantidad publicada, unitPrice, totalAmount y expiresAt devueltos por backend; mantener visible hasta navegación o refresh in frontend/src/features/market/pages/MarketSellPage/SellTokenModal/ per spec.md FR-F09, CA-09 (depends on T013, T018, T019) — G2

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Estados de carga, validación y error (Priority: P2)

**Goal**: Interfaz informa claramente sobre el estado de la solicitud, errores de validación y condiciones del sistema sin exponer información sensible. Cubre FR-F05, FR-F06, FR-F08 y FR-F10.

**Independent Test**: El usuario interactúa con el formulario en distintos escenarios (cantidad inválida, tenencia insuficiente, precio no disponible) y verifica que los mensajes de error son comprensibles y no revelan datos sensibles (spec.md CA-16, RF-05, RF-06).

### Implementation for User Story 3

- [X] T021 [US3] Add inline validation errors for cantidad (vacío, no numérico, decimal, fuera de rango 1-100) with "Cantidad entre 1 y 100" message and Confirmar disabled in frontend/src/features/market/form/hooks/useSellQuantityForm.ts per spec.md CA-03, data-model.md
- [X] T022 [US3] Implement error code mapping to safe user messages (INVALID_REQUEST, INVALID_QUANTITY, INSUFFICIENT_STOCK, QUOTE_UNAVAILABLE, INVALID_QUOTE_PRICE, SUPERUSER_CANNOT_SELL, PLAYER_NOT_FOUND, PLAYER_NOT_VISIBLE, INTERNAL_ERROR/red/500 genérico; 401 sesión global) in frontend/src/features/market/hooks/pages/useMarketSellPage.ts per contracts/ table and spec.md RF-05, RF-06. frontend/src/features/market/market.service.ts gestiona endpoints, autenticación de la solicitud y contratos DTO mediante Mapper, devuelve Models y propaga los errores HTTP sin traducirlos a mensajes de UI; el Page Hook inspecciona códigos con los helpers públicos de infrastructure/http y decide mensajes/estado de UI, sin HTTP directo ni DTO, conforme a roles.md artículos 13–15 (depends on T010, T016)
- [X] T023 [US3] Add loading/spinner state during submission with `aria-busy`, spinner visible, Confirmar/Cancelar/overlay/ESC no cierran ni envían otro POST in frontend/src/features/market/hooks/mutations/useSellTokensMutation.ts per spec.md CA-15, quickstart US3 (depends on T013)
- [X] T024 [US3] Keep modal open on validation/business error, preserve valid input data, allow user to correct invalid fields without losing player selection or partially completed quantity; no unsafe automatic retry in frontend/src/features/market/pages/MarketSellPage/ per spec.md FR-F10, quickstart US3

**Checkpoint**: At this point, User Stories 1, 2 AND 3 should all work independently

---

## Phase 6: User Story 4 - Actualización de tenencias y reflejo en portfolio (Priority: P2)

**Goal**: Después de una publicación exitosa, la información visible refleje los tokens disponibles o reservados según las reglas del mercado (SPEC-011 FR-006). Cubre FR-F06 y FR-F12. Invalidación unificada para evitar lógica duplicada (G6).

**Independent Test**: Después de una publicación exitosa, el usuario ve actualizada la información de sus tenencias: tokens disponibles y reservados. El cambio es consistente con las reglas del mercado (spec.md CA-17, RF-07).

### Implementation for User Story 4

- [X] T025 [US4] `app` invalida únicamente `portfolioQueryKeys.summary` y todas las `holdings(...)` vía `onSellSuccess(playerId)`, sin copiar literales de key ni imports entre features, in frontend/src/app/router/AppRouter/AppRouter.tsx (depends on T016); `market` invalida únicamente `marketQueryKeys.currentQuote(id)` in frontend/src/features/market/hooks/pages/useMarketSellPage.ts (T016). Un único responsable por cache.
- [X] T026 [US4] In frontend/src/features/market/hooks/pages/useMarketSellPage.ts call the app-provided onRefreshHoldings(playerId) when opening SellTokenModal and after a failed sell POST while the session remains active (not after 401 redirection); implement the callback in frontend/src/app/router/AppRouter/AppRouter.tsx to invalidate the prefix of all portfolioQueryKeys.holdings(...), so refreshed holdings props update MarketSellPage. Successful POST continues through onSellSuccess and T025. Do not import portfolio from market; satisfy entry/failure/success synchronization in spec.md FR-F12 and contracts/ (depends on T006, T016, T017, T025)

**Checkpoint**: At this point, User Stories 1, 2, 3 AND 4 should all work independently

---

## Phase 7: User Story 5 - Accesibilidad y distintos tamaños de pantalla (Priority: P2)

**Goal**: El proceso de publicación sea accesible y se adapte a distintos tamaños de pantalla (mockups 07/08). Cubre FR-F07 y CA-11.

**Independent Test**: El flujo se recorre con teclado y se revisa en mobile, tablet y desktop. Se comprueba el uso de controles HTML semánticos, navegación básica por teclado y reutilización del comportamiento accesible existente (spec.md CA-11, FR-F07).

### Implementation for User Story 5

- [X] T027 [US5] Use semantic HTML controls, support basic keyboard navigation, and reuse the existing accessible behavior of `shared/Modal` in frontend/src/features/market/pages/MarketSellPage/SellTokenModal/ per spec.md CA-11 and quickstart US4
- [ ] T028 [US5] Validate and adjust responsive layout at 375, 768 and 1440 px: mercado visible detrás, overlay semitransparente, cantidad inicial "1", sin scroll horizontal ni cambio de layout/scroll del mercado in frontend/src/features/market/pages/MarketSellPage/ per spec.md CA-11, quickstart US4, mockups 07/08 (depends on T027; same page/modal styles may be touched)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final integration per approved decisions.

- [X] T029 [P] Extend useMarketToast with the in-scope transient notifySellSuccess toast (cantidad, listingId, precio unitario, importe total, expiresAt efectivos) in frontend/src/features/market/hooks/useMarketToast.ts per spec.md FR-F09 and research.md decision 10 (no persistent/external notifications, separate hook, or new dependencies)
- [ ] T030 [P] Validate against quickstart.md manual scenarios (US1-US4): backend cotizaciones reales, JWT de usuario no superusuario, tenencia de jugador visible; resultados y tiempos registrados con evidencia observada (no pruebas automatizadas, frontend exento) in specs/021-publish-token-sales-frontend/quickstart.md
- [ ] T031 [P] Confirm fidelity to mockups 07 (`referencias-imagenes/07-mercado-vender.png`) and 08 (`referencias-imagenes/08-mercado-vender-modal-abierto.png`) as layout/component/interaction reference per spec.md Assumptions (imágenes guías de disposición, no saldos reales)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion; navigation tab is shared prerequisite
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can proceed in priority order (P1 → P2) or in parallel if staffed
  - Each story independently testable per its Independent Test
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories. MVP scope.
- **User Story 2 (P1)**: Depends on US1 form/mutation infrastructure (T012, T013) - independently testable
- **User Story 3 (P2)**: Depends on US1 mutation (T013) and US2 page coordination (T016) - independently testable
- **User Story 4 (P2)**: Depends on US1 app composition callbacks (T006), US2 page hook (T016), and success cache invalidation (T025) - independently testable
- **User Story 5 (P2)**: Depends on SellTokenModal existence (T017) - independently testable

### Within Each User Story

- DTO/model definitions (T007, T008) before mapper (T009) and service (T010)
- Services (T010) before mutations (T013)
- Core implementation before route/data integration (T005/T006) and page coordination (T016)
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001, T002) marked [P] can run in parallel
- Foundational navigation task T003 can proceed after Setup
- T007, T008, T011 and T012 within US1 marked [P] can run in parallel; T009 mapper waits for T007/T008; T010 service waits for the mapper
- User stories can only parallelize where their explicit dependencies permit; US2 requires US1 form/mutation, US3/US4 depend on prior integration, and US5 waits for the modal (T017)
- T027 and T028 are sequential because accessibility and responsive work may touch the same modal/page files
- T029, T030, T031 in Polish marked [P] can run in parallel

### Parallel Example: User Story 1

```bash
# Launch independent DTO/model/schema/form tasks together; mapper waits for DTO/model definitions:
Task: "Add OrderSellRequestDTO and OrderSellResponseDTO in frontend/src/features/market/types/dtos.ts" (T007)
Task: "Add SellResult, SellQuantityForm, SellEstimation models in frontend/src/features/market/types/models.ts" (T008)
Task: "Add sellQuantitySchema Zod validation in frontend/src/features/market/form/schemas/sellQuantitySchema.ts" (T011)
Task: "Create useSellQuantityForm hook in frontend/src/features/market/form/hooks/useSellQuantityForm.ts" (T012)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. US4 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
6. US5 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
7. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Frontend exento de tests: MUST NOT crear tests ni infraestructura de testing frontend (Constitution §19.199)
- Backend tests: per Constitution and applicable norms, ejecución a cargo del usuario, nunca del agente; MUST NOT generar tareas de ejecución por agente
- Respetar limitación documentada sobre cambios concurrentes de cotización (research.md Riesgo 1): frontend no puede garantizar detectar todos los cambios concurrentes; guard re-valida cotización disponible y positiva
- Respetar mockups 07/08 como referencia de layout, componentes e interacciones (imágenes guías, no saldos reales)
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

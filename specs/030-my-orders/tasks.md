# Tareas: Visualizar y gestionar órdenes de venta personales (SPEC-030)

**Estado**: Implementación completa y verificada; Fase 1 y Fase 2 finalizadas con éxito.  
**Referencias normativas**: [`spec.md`](spec.md), [`plan.md`](plan.md), [`contracts/my-orders-frontend.md`](contracts/my-orders-frontend.md) y [`docs/wireframes/my-orders.md`](../../docs/wireframes/my-orders.md).

---

## Tareas de la funcionalidad vigente

### Fase 1 — Extensión del backend (Persistencia, Servicio, DTOs y API)

- [x] **T01** Crear los DTOs backend para la respuesta paginada y resumen métrico de órdenes (`TokenListingsPageResponseDTO`, `UserOrdersSummaryResponseDTO`) en `backend/src/main/java/footballmarket/features/market/presentation/dtos/response/`.
- [x] **T02** Extender `TokenListingService` y su implementación `TokenListingServiceImpl` para incorporar paginación (`page`, `size`), ordenamiento por defecto *Más recientes* (`createdAt DESC, id DESC`), cálculo de métricas agregadas globales (`summary`) y filtrado por estado.
- [x] **T03** Actualizar `ListingController.mine` para recibir `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "10") int size`, `@RequestParam(required = false) String status` y `@RequestParam(defaultValue = "NEWEST") String sort`, retornando `TokenListingsPageResponseDTO`.
- [x] **T04** Actualizar y extender la cobertura de pruebas de backend en `ListingControllerTest` y `TokenListingServiceTest` para verificar la paginación y el resumen de métricas.

### Fase 2 — Frontera de datos del frontend (Tipos, Servicio y Hooks)

- [x] **T05** Declarar las interfaces DTO y modelos de dominio en `frontend/src/features/market/types/` (`UserOrdersPageResponseDTO`, `UserOrdersSummaryDTO`, `UserOrdersPage`, `UserOrderListing`).
- [x] **T06** Implementar las funciones de mapeo en `frontend/src/features/market/market.mapper.ts` para transformar la respuesta paginada y convertir importes de centésimas a créditos.
- [x] **T07** Actualizar `market.service.ts` con `getMyListings(params, signal)` paginado y `cancelListing(listingId)`.
- [x] **T08** Crear el hook de consulta `useMyListingsQuery.ts` y la mutación `useCancelListingMutation.ts` en `frontend/src/features/market/hooks/`.

### Fase 3 — Componentes visuales y maquetación desktop (UI)

- [x] **T09** Implementar el hook de coordinación `useMyOrdersPage.ts` en `frontend/src/features/market/hooks/pages/` (sincronización con search params de URL `page` y `status`, orden predeterminado y gestión de modal).
- [x] **T10** Crear el encabezado `MarketOrdersHeader` y las tarjetas de métricas `MarketOrdersMetrics` conectadas a los datos de `summary`.
- [x] **T11** Implementar la barra de filtros `MarketOrdersFilters` (filtro por estado, buscador de texto, orden predeterminado y botón limpiar).
- [x] **T12** Construir la tabla `MarketOrdersTable` y la fila `MarketOrdersRow` con badges semánticos por estado (`ACTIVE`, `SOLD`, `CANCELLED`, `EXPIRED`), retrato del futbolista y paginación compartida `Pagination`.
- [x] **T13** Crear el estado vacío `MarketOrdersEmptyState` con botón directo hacia `/market/sell`.

### Fase 4 — Flujo interactivo de cancelación con modal accesible

- [x] **T14** Implementar el modal de confirmación `CancelListingModal` en `frontend/src/features/market/pages/MarketOrdersPage/components/CancelListingModal/`, detallando lote, créditos y liberación de tokens a la cartera.
- [x] **T15** Conectar `CancelListingModal` con `useCancelListingMutation`, gestionando estado de carga (`isBusy`), toast de feedback tras éxito/error y cierre accesible.

### Fase 5 — Navegación, router y composición

- [x] **T16** Crear la página principal `MarketOrdersPage.tsx` integrando los subcomponentes dentro de `PageLayout`.
- [x] **T17** Habilitar la ruta protegida `/market/orders` en `frontend/src/app/router/AppRouter/AppRouter.tsx` y activar la pestaña `market-orders` con `path: "/market/orders"` en `frontend/src/app/navigation/sections.ts`.

### Fase 6 — Verificación y control de calidad

- [x] **T18** Ejecutar controles estáticos autorizados (`./gradlew build -x test`, `npm run biome:check`, `npm run build`), verificando ausencia de errores de compilación y cumplimiento de estilos.
- [x] **T19** Verificar la estricta sujeción al diseño desktop (`1920 × 913 px`) y la ausencia de media queries móviles o scroll horizontal global.

---

## Matriz de trazabilidad

| Requisito | Criterio de aceptación | Componentes afectados | Tarea(s) |
|---|:---:|---|:---:|
| **FR-001** (Paginación server-side `mine`) | **CA-01**, **CA-04** | `TokenListingServiceImpl.java`, `ListingController.java`, `Pagination.tsx` | T02, T03, T07, T12 |
| **FR-002** (Orden *Más recientes* por defecto) | **CA-02** | `TokenListingServiceImpl.java`, `useMyOrdersPage.ts` | T02, T03, T09 |
| **FR-003** (Resumen `summary` en respuesta) | **CA-01**, **CA-05** | `TokenListingsPageResponseDTO.java`, `MarketOrdersMetrics.tsx` | T01, T02, T06, T10 |
| **FR-004** (Conversión de centésimas a créditos) | **CA-04** | `market.mapper.ts`, `MarketOrdersRow.tsx` | T06, T12 |
| **FR-005** (Cancelación vía `POST /listings/{id}/cancel`) | **CA-06** | `ListingController.java`, `market.service.ts`, `useCancelListingMutation.ts` | T07, T08, T15 |
| **FR-006** (Ruta `/market/orders` y tab activa) | **CA-03** | `AppRouter.tsx`, `sections.ts`, `MarketOrdersPage.tsx` | T16, T17 |
| **FR-007** (Modal accesible `CancelListingModal`) | **CA-07** | `CancelListingModal.tsx` | T14, T15 |
| **FR-008** (Enriquecimiento con catálogo deportivo) | **CA-04** | `useMyOrdersPage.ts`, `MarketOrdersRow.tsx` | T09, T12 |
| **FR-009** (Diseño desktop 1920 × 913 px) | **CA-10** | `MarketOrdersPage.module.scss`, componentes SCSS | T10, T11, T12, T19 |
| **FR-010** (Inmutabilidad de edición de orden) | **CA-06** | `MarketOrdersRow.tsx`, `spec.md` | T12, T14 |

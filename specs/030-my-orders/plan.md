# Implementation Plan: Visualizar y gestionar órdenes de venta personales (SPEC-030)

**Feature Branch**: `feat/030-my-orders`  
**Estado**: Plan de implementación técnico propuesto.  
**Referencias normativas**: [`spec.md`](spec.md), [`contracts/my-orders-frontend.md`](contracts/my-orders-frontend.md) y [`docs/wireframes/my-orders.md`](../../docs/wireframes/my-orders.md).

---

## 1. Resumen de arquitectura

La solución abarca la extensión de la API en el backend y la construcción de la pantalla de **Mis órdenes** en el frontend:
1. **Backend**:
   - Paginación server-side, ordenamiento por defecto *Más recientes* (`createdAt DESC, id DESC`) y agregación de métricas de usuario en `GET /api/listings/mine`.
   - Reutilización de `POST /api/listings/{id}/cancel` para la cancelación atómica bajo bloqueo de ofertas activas.
2. **Frontend**:
   - Creación de la vista `/market/orders` en la feature `features/market/`.
   - Coordinación de estado mediante `useMyOrdersPage` sincronizado con los parámetros de la URL (`page`, `size`, `status`).
   - Modal de confirmación accesible `CancelListingModal`.
   - Enriquecimiento con la caché del catálogo de jugadores (`useAllPlayersQuery`).

---

## 2. Contexto técnico

- **Backend**: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security JWT, PostgreSQL.
- **Frontend**: React 19.3.0 + TypeScript 7.0.2, TanStack Query v5, React Router v7, SCSS Modules con tokens semánticos.
- **Viewport**: Desktop exclusivo (`>=1024 px`), optimizado para `1920 × 913 px`.
- **Verificación**: `./gradlew build -x test`, `npm run biome:check`, `npm run build`.

---

## 3. Arquitectura del Backend

### Flujo de consulta paginada (`GET /api/listings/mine`)
```text
ListingController.mine(jwt, page, size, sort, status)
  ├── TokenListingService.mine(viewerEmail, criteria)
  │     ├── TokenListingRepository (consulta de publicaciones por vendedor con orden createdAt DESC)
  │     ├── Cálculo de estado efectivo (ACTIVE vs EXPIRED según marketClock)
  │     ├── Paginación en memoria / query paginada
  │     └── Agregado de métricas globales del vendedor (summary)
  └── ListingMapper.toPageResponse(listingsPage, summary, pagination)
```

### DTOs de Backend
- `TokenListingsPageResponseDTO`:
  - `summary`: `UserOrdersSummaryResponseDTO` (`activeOrders`, `activeTokens`, `activeValue`, `soldOrders`, `soldTokens`, `soldValue`, `cancelledOrders`, `expiredOrders`, `totalOrders`).
  - `listings`: `List<TokenListingSummaryResponseDTO>` (elementos de la página).
  - `pagination`: `PortfolioPaginationResponseDTO` (`page`, `size`, `totalElements`, `totalPages`).

---

## 4. Arquitectura del Frontend

### Jerarquía de Componentes
```text
MarketOrdersPage
  ├── MarketOrdersHeader (utiliza MarketPageHeader con métricas integradas MarketHeaderMetric)
  ├── MarketFilters (variante "orders", con FilterSearch y FilterSelect)
  ├── MarketOrdersTable (tabla principal estilo trading desk con scroll interno y cabecera sticky)
  │     ├── MarketOrdersRow (renderiza cada publicación, badge de estado, TeamBadge y botón Cancelar)
  │     └── Pagination (componente compartido conectado a la paginación del backend)
  ├── MarketOrdersEmptyState (pantalla centrada cuando no hay órdenes registradas o sin coincidencias)
  └── CancelListingModal (modal accesible de confirmación para cancelar una publicación activa)
```

### Servicios, Mappers y Hooks
- **Servicio (`market.service.ts`)**:
  - `getMyListings(params: MyListingsQueryParams, signal?: AbortSignal): Promise<UserOrdersPage>`
  - `cancelListing(listingId: number): Promise<CancelListingResult>`
- **Mapeador (`market.mapper.ts`)**:
  - Conversión de centésimas a créditos y adaptación de DTO a modelos de dominio (`UserOrderListing`, `UserOrdersSummary`).
- **Hooks**:
  - `useMyListingsQuery`: Consulta TanStack Query con clave compuesta `["market", "listings", "mine", params]`.
  - `useCancelListingMutation`: Mutación TanStack Query para `cancelListing` con invalidación reactiva.
  - `useMyOrdersPage`: Manejo de search params de la URL (`page`, `status`), apertura de modal y selección de publicación a cancelar.

---

## 5. Integración y Enrutamiento

1. **`frontend/src/app/navigation/sections.ts`**:
   - Activar la pestaña `market-orders` con `path: "/market/orders"`.
2. **`frontend/src/app/router/AppRouter/AppRouter.tsx`**:
   - Incorporar la ruta protegida `<Route path="/market/orders" element={<MarketOrdersPage />} />`.
3. **`frontend/src/features/market/index.ts`**:
   - Exportar `MarketOrdersPage`.

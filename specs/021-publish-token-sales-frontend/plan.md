# Implementation Plan: Publicar ventas de tokens desde el frontend (SPEC-021)

**Branch**: `feat/021-publish-token-sales-frontend` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

## Summary

Flujo controlado para que un usuario autenticado publique (oferta de reserva, sin transferir) tokens que posee de un jugador visible, mediante `POST /api/orders/sell` (contrato SPEC-011). Antes del POST muestra el precio/importe estimados desde `GET /api/players/{id}/quotes/current` (SPEC-016); al confirmar revalida con ese GET y, si cambió, requiere nueva confirmación. El backend aplica el precio autoritativamente al POST y su respuesta provee los datos definitivos, incluido `expiresAt`. El proceso se ejecuta dentro de un modal sobre el mercado existente, sin modificar el layout del mercado. Incluye confirmación expresa, prevención de publicaciones duplicadas durante el envío, comunicación de rechazos sin datos sensibles y actualización de tenencias disponibles/reservadas tras publicación exitosa.

Reutiliza los componentes, services, hooks y patrones de SPEC-019 (compra de tokens): `marketService`, `market.mapper`, `types/`, `constants/marketQueryKeys`, `form/hooks/`, `form/schemas/`, `hooks/queries/`, `hooks/mutations/`, `hooks/pages/useMarketBuyPage`, `hooks/useMarketToast` (extendido con el toast transitorio de éxito de venta incluido en FR-F09; sin notificaciones persistentes o externas), `utils/`, `shared/Modal`, `Button`, `Field`.

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0, Node.js >=24  
**Primary Dependencies**: React, React DOM, TanStack Query 5, Axios 1.20, React Router 7, React Hook Form 7.88, Zod 4.6.5, Zustand 5, React-Toastify 11, Sass, Vite 8, Biome 2.5  
**Storage**: N/A (frontend; estado remoto en TanStack Query, sesión en Zustand + infrastructure/storage)  
**Testing**: Frontend exento de tests automatizados (Constitution §19.199); escenarios manuales en quickstart.md  
**Target Platform**: Navegador web (Vite build), rutas protegidas bajo AuthGuard  
**Project Type**: Web application (frontend), feature aislada dentro de `features/market`  
**Performance Goals**: Apertura de modal <200 ms; aviso tras respuesta ≤500 ms (objetivos de plan SPEC-019, referencia aplicable)  
**Constraints**: No modificar backend; no inventar precios; no reutilizar cotización demo (`playersQueryKeys.tokenQuote`); no duplicar publicaciones durante el envío; modal sin alterar layout/scroll del mercado  
**Scale/Scope**: Una feature de frontend (publicación/venta de tokens desde el mercado), aislada de otras features

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Evaluación |
| --- | --- |
| Simplicidad y calidad | Reutiliza la arquitectura de SPEC-019 sin crear capas nuevas; `market` sigue independiente de `players` y `portfolio`; composición solo desde `app`. |
| Proporcionalidad del alcance | Solo la publicación de ventas (`POST /api/orders/sell`); queda fuera compra, cancelación, modificación, historial y portfolio como consumidor directo. |
| Seguridad e integridad | Solicitudes autenticadas; validación de cantidad en frontend sin sustituir backend; errores genéricos sin filtrar detalles sensibles; `any` prohibido. |
| Coherencia de contratos | `POST /api/orders/sell` y `GET /api/players/{id}/quotes/current` son contratos backend existentes; mapper DTO → Model dentro del Service; no se introducen incompatibilidades silenciosas. |
| Trazabilidad y fuente de verdad | Spec, contratos, modelo, quickstart y tasks concordan; nuevo ANALYZE requerido tras el diseño. |
| Verificación y testing | Agente no ejecuta tests; build permitido `npm run build` solo tras cambios de código; documentación sin build. |

**Resultado**: Diseño documental coherente con las decisiones aprobadas; no constituye ANALYZE vigente ni autorización de IMPLEMENT.

## Project Structure

### Documentation (this feature)

```text
specs/021-publish-token-sales-frontend/
├── spec.md              # Espec funcional existente; preparación pendiente de nuevo ANALYZE
├── plan.md              # Este archivo (/speckit.plan output)
├── research.md          # Fase 0 output (/speckit.plan output)
├── data-model.md        # Fase 1 output (/speckit.plan output)
├── quickstart.md        # Fase 1 output (/speckit.plan output)
├── contracts/
│   └── publish-token-sales-frontend.md  # Fase 1 output (contrato de consumo HTTP)
└── tasks.md             # Fase 2 output (no creado por /speckit.plan)
```

### Source Code (frontend)

```text
frontend/
├── src/
│   ├── app/
│   │   ├── router/AppRouter/AppRouter.tsx        # Rutas independientes /market/buy y /market/sell bajo AuthGuard + AppLayout
│   │   ├── navigation/sections.ts                 # Pestaña "Vender tokens" en la navegación visual existente (path /market/sell)
│   │   └── providers/AppProviders/AppProviders.tsx  # ToastContainer (ya existe de SPEC-019)
│   ├── features/
│   │   ├── market/                                # Feature existente (SPEC-019), se extiende
│   │   │   ├── index.ts                           # Exporta MarketSellPage y únicamente los tipos públicos necesarios
│   │   │   ├── market.service.ts                  # Añadir sellTokens({ playerId, quantity }) → POST /orders/sell
│   │   │   ├── market.mapper.ts                   # Añadir toSellResponseDTO / toOrderSellRequestDTO
│   │   │   ├── types/dtos.ts                      # OrderSellRequestDTO, OrderSellResponseDTO
│   │   │   ├── types/models.ts                    # SellResult, SellQuantityForm, SellEstimation (si procede)
│   │   │   ├── constants/marketQueryKeys.ts       # currentQuote (ya existe, se reusa)
│   │   │   ├── form/hooks/useSellQuantityForm.ts  # Cantidad inicial "1", validación 1-100
│   │   │   ├── form/schemas/sellQuantitySchema.ts # Zod schema cantidad 1-100
│   │   │   ├── hooks/mutations/useSellTokensMutation.ts # POST /orders/sell
│   │   │   ├── hooks/pages/useMarketSellPage.ts   # Coordinación: form, quote query, mutation, duplicados, invalidación currentQuote
│   │   │   ├── hooks/queries/useMarketQuotesQuery.ts # Reusa existente (query por id)
│   │   │   ├── hooks/useMarketToast.ts            # Toast transitorio de éxito de venta (notifySellSuccess; FR-F09)
│   │   │   ├── utils/formatters.ts                # Reusa existentes
│   │   │   ├── utils/quoteStatus.ts               # Reusa existente
│   │   │   └── pages/MarketSellPage/              # Page local + Components locales (sin Hooks en componentes)
│   │   │       ├── MarketSellPage.tsx
│   │   │       ├── SellTokenModal/                # Modal local sin Hooks (props only)
│   │   │       ├── MarketPlayerSummary/           # Reusa de SPEC-019
│   │   │       └── MarketQuoteFeedback/           # Reusa de SPEC-019
│   │   ├── players/index.ts                       # Sin cambios (reusa usePlayersQuery, PLAYERS_PAGE_SIZE, Player)
│   │   └── portfolio/index.ts                     # Expone usePortfolioHoldingsQuery existente y Models necesarios vía barrels; conserva portfolioQueryKeys (T006)
│   └── shared/
│       ├── components/Modal/                      # Reusa existente (portal, foco, ARIA, ESC)
│       ├── components/Button/                     # Reusa existente
│       ├── components/Field/                      # Reusa existente
│       └── icons/                                 # Reusa existente
├── package.json                                   # Sin nuevas dependencias (React-Toastify ya instalado)
└── docs/frontend/technologies.md                  # Sin cambios de inventario (reusa existentes)
```

**Structure Decision**: Feature aislada en `features/market` (misma que SPEC-019), composición desde `app` (router + navigation + datos). `market` no importa de `players` ni `portfolio`; `app` consume sus APIs públicas, transforma el catálogo `Player` a `MarketPlayer`, obtiene las tenencias requeridas y las pasa junto con `onSellSuccess(playerId)` y `onRefreshHoldings(playerId)` como props. `market` llama `onRefreshHoldings` al abrir el modal y después de un POST fallido con sesión activa; `app` invalida holdings. En éxito, `onSellSuccess` invalida summary y holdings. `market` es responsable de invalidar solamente `marketQueryKeys.currentQuote(id)` tras éxito; `app` es responsable de invalidar `portfolioQueryKeys.summary` y el prefijo de `portfolioQueryKeys.holdings(...)`. La ruta `/market/sell` se registra cuando `MarketSellPage` está exportada/disponible. Rutas independientes `/market/buy` y `/market/sell` comparten la navegación visual existente. `shared` solo componentes genéricos. No se crean carpetas vacías ni abstracciones por uniformidad.

### Precisiones de implementación: C1, M1 y M2 (2026-10-26)

- **C1 — Errores:** `marketService.sellTokens` conserva la frontera HTTP/DTO/Mapper y propaga los errores; no genera mensajes de UI. `useMarketSellPage` interpreta los códigos mediante los helpers públicos de `infrastructure/http` y aplica los mensajes del contrato. T022 incluye esta responsabilidad y depende de T010/T016; no cambia el contrato HTTP.
- **M1 — API pública de holdings:** T006 incluye reexportar el Query Hook existente `usePortfolioHoldingsQuery` desde `portfolio/hooks/queries/index.ts` por `portfolio/hooks/index.ts` y `portfolio/index.ts`; los Models necesarios se exponen por sus barrels de tipos, sin publicar DTO ni Service. `app` consume únicamente `@/features/portfolio`, consulta las posiciones `OPEN` con la paginación existente (sin excluir las totalmente reservadas) y adapta `quantity`, `availableQuantity` y `reservedQuantity` a las props propias de market. La lectura remota sigue en el Query Hook y Service de portfolio; market no conoce sus tipos ni importa esa feature. La composición debe conservar la cobertura de todas las tenencias elegibles de FR-F02, sin confundir una página parcial del catálogo o de holdings con el conjunto completo. Los callbacks y la invalidación definidos en T025/T026 siguen siendo responsabilidad de `app`. Es una exposición mínima de una lectura existente, sin endpoint, infraestructura o reglas de portfolio nuevos.
- **M2 — Filtros reutilizados:** los controles actuales de `MarketBuyPage/MarketFilters` son visuales y están deshabilitados; no existe lógica operativa de compra que pueda darse por reutilizada. T015 mueve ese módulo visual y sus estilos a `market/components/MarketFilters/`, con su API local, para consumo por ambas Pages sin deep imports entre módulos de Page. El componente recibe props y callbacks, sin Hooks; la compra conserva sus controles deshabilitados y su comportamiento. En venta, `useMarketSellPage` coordina los valores, opciones, aplicación y limpieza de los filtros existentes y los de disponibilidad/ordenamiento por tenencias ya exigidos por FR-F11. `MarketSellPage` conecta esas props. El filtrado y ordenamiento se aplican al listado de tenencias elegibles antes de su paginación visible, sin nuevos endpoints ni funcionalidad adicional de compra. La extracción visual y la coordinación quedan en T015/T016, sin nuevas tareas.

Estas precisiones corrigen el diseño documental, no acreditan su implementación ni un ANALYZE favorable. Se mantienen las 31 tareas y las cinco historias; corresponde nuevo ANALYZE antes de IMPLEMENT.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Ninguna | N/A | N/A |

---

## Fase 0: Investigación (research.md)

Véase `research.md`. Todos los NEEDS CLARIFICATION del Technical Context quedaron resueltos con valores reales del contrato backend SPEC-011 y patrones SPEC-019.

## Fase 1: Diseño y contratos (data-model.md, contracts/, quickstart.md)

Véase `data-model.md`, `contracts/publish-token-sales-frontend.md` y `quickstart.md`.

## Post-diseño: Re-evaluación Constitution Check

Tras el diseño no hay violaciones injustificadas. El alcance se mantiene dentro de lo aprobado en spec.md; no se agregan requisitos por buenas prácticas generales. Las decisiones pendientes quedan resueltas en research.md (notificaciones de venta en useMarketToast, export público de MarketSellPage, rutas independientes /market/buy y /market/sell, validación manual en quickstart.md).

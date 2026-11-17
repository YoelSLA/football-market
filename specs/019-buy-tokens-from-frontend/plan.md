# Implementation Plan: Comprar tokens en el mercado

**Branch**: `feat/019-buy-tokens-from-frontend` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente la arquitectura, diseño y decisiones
técnicas vigentes y aplicables al sistema (Constitución §V). El historial de decisiones técnicas
superadas se delega a Git. Forma parte de la tríada permanente obligatoria junto a spec.md y tasks.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `/specs/019-buy-tokens-from-frontend/spec.md`

**Note**: Este plan finaliza en diseño técnico. La implementación frontend debe respetar las dos fases y el gate humano definidos en `frontend/AGENTS.md`.

## Summary

Evolucionar las dos páginas protegidas de Mercado dentro de `features/market`. `/market/buy` reemplazará la composición `usePlayersQuery` + consultas N+1 por una query paginada sobre `GET /api/market/official`, con ocho jugadores por página y filtros, orden y página sincronizados con la URL. `/market/p2p` conservará `GET /api/listings` como lectura completa sin parámetros, agrupará y paginará localmente, y sincronizará búsqueda, liga, posición, orden, página y jugador seleccionado con la URL. Ambos flujos reutilizarán `POST /api/orders/buy`, sus guardas e invalidaciones, manteniendo separados los payloads `{ playerId, quantity }` y `{ listingId }` y aplicando los wireframes aprobados.

## Technical Context

**Language/Version**: TypeScript 7.0.2 y React 19.3.0

**Primary Dependencies**: React Router DOM 7.18.4, TanStack Query 5.103.1, Axios 1.20.0, React Hook Form 7.88.0, Sass 1.104.1 y React-Toastify 11.1.0; sin dependencias nuevas

**Storage**: Estado remoto en TanStack Query; criterios navegables de ambos mercados en query parameters; edición debounced y estado efímero de modales en React

**Testing**: Frontend exento de tests automatizados. Validación manual a cargo del usuario mediante [quickstart.md](quickstart.md). Los controles futuros permitidos para cambios de código son `npm run build` y `npm run biome:fix`; no equivalen a tests

**Target Platform**: Navegador web responsive; referencia desktop 1920 × 913 px y validación adicional en 375, 768 y 1440 px

**Project Type**: Aplicación web frontend integrada con API HTTP existente

**Performance Goals**: Una sola solicitud consolidada por estado oficial estable; una lectura completa compartida para catálogo y detalle P2P; búsquedas con debounce; cancelación de solicitudes oficiales reemplazadas; sin consultas N+1 ni lectura adicional al abrir detalles o modales

**Constraints**: Sin cambios backend; inventario oficial oculto; importes en centésimas; página oficial fija en 8 y P2P fija en 12; filtros automáticos; P2P filtrado/ordenado/paginado localmente; compra P2P solo de lote completo; no mostrar score, multiplicador, origen ni `calculatedAt`

**Scale/Scope**: Dos Pages existentes, sus Page Hooks y componentes locales; un Query Hook oficial nuevo y el Query Hook P2P vigente; Service/Mapper/DTOs/Models de `market`; reutilización acotada de jugadores, compra, sesión, cartera y componentes shared

## Constitution Check

*GATE: evaluado antes de Phase 0 y nuevamente después de Phase 1.*

| Principio | Evaluación pre-diseño | Evidencia post-diseño |
| --- | --- | --- |
| I. Simplicidad y calidad | PASS | Se elimina el flujo N+1 y se reutilizan Service, Mapper, Query, Modal, Pagination y recursos existentes sin capas nuevas. |
| II. Proporcionalidad | PASS | El alcance se limita a los dos catálogos y compras aprobados; no modifica backend, no agrega endpoints ni introduce filtros sin respaldo. |
| III. Seguridad e integridad | PASS | HTTP autenticado existente, validación de DTOs externos, guardas duplicadas en UI/handler y mensajes seguros. |
| IV. Coherencia de contratos | PASS | [contracts/market-official.md](contracts/market-official.md) refleja SPEC-010 real; compra permanece alineada con [contracts/buy-tokens-frontend.md](contracts/buy-tokens-frontend.md). |
| V. Trazabilidad y fuente de verdad | PASS | Spec, plan, research, data model, contratos y quickstart describen solo el diseño vigente; Git conserva el historial. |
| VI. Mínimo alcance necesario | PASS | No se crean dependencias, stores, endpoints ni abstracciones hipotéticas. |

No existen violaciones que requieran Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/019-buy-tokens-from-frontend/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── market-official.md
│   └── buy-tokens-frontend.md
└── tasks.md                 # No se modifica en PLAN
```

### Source Code (repository root)

```text
frontend/src/
├── app/
│   ├── layouts/            # AppLayout y PageLayout existentes
│   └── router/             # Rutas protegidas existentes
├── features/
│   ├── auth/               # Usuario actual y saldo; API pública
│   ├── market/
│   │   ├── components/     # Componentes reutilizados dentro de market
│   │   ├── constants/      # marketQueryKeys
│   │   ├── form/           # Cantidad de compra existente
│   │   ├── hooks/
│   │   │   ├── mutations/  # Compra oficial/P2P existente
│   │   │   ├── pages/      # Coordinación oficial y P2P
│   │   │   └── queries/    # Query oficial consolidada y listings P2P
│   │   ├── pages/
│   │   │   ├── MarketBuyPage/
│   │   │   └── MarketP2PPage/
│   │   ├── types/          # DTOs y Models de market
│   │   ├── market.mapper.ts
│   │   └── market.service.ts
│   ├── players/            # API pública de opciones deportivas
│   └── portfolio/          # Query keys públicas para invalidación
├── infrastructure/http/    # Cliente autenticado existente
└── shared/components/      # Button, Field, Modal, Pagination y recursos deportivos
```

**Structure Decision**: La integración permanece dentro de `features/market`. Cada Page conserva sus componentes exclusivos y su Page Hook; `market` consume `auth`, `players` y `portfolio` solo por sus APIs públicas. No se mueve lógica de mercado a `app`, `shared` ni `players`, y no se unifican Hooks o Models con contratos distintos.

## Diseño técnico

### Flujo de datos oficial

```text
URLSearchParams ──parse/normalize──> OfficialMarketCriteria
        │                                  │
        │                                  ▼
        │                      useOfficialMarketQuery
        │                                  │
        │                                  ▼
        │                         marketService.getOfficialMarket
        │                                  │
        │                                  ▼
        │                         GET /api/market/official
        │                                  │
        │                                  ▼
        └──serialize<── Page Hook <── Mapper <── DTO paginado
                              │
                              ├── Components por props
                              └── BuyTokenModal ──> POST /api/orders/buy
```

- `useMarketBuyPage` será la única coordinación de URL, búsqueda editable/debounced, filtros, query, modal, formulario, compra, mensajes e invalidaciones.
- `useOfficialMarketQuery(criteria)` declarará una query por criterios normalizados y usará `AbortSignal`; no conservará DTOs ni lógica de UI.
- `marketService.getOfficialMarket(criteria, signal)` serializará solo parámetros definidos y devolverá `OfficialMarketPage` ya mapeado.
- `market.mapper.ts` validará la envoltura, nulabilidad y campos reales de SPEC-010. Los campos técnicos del bloque `quote` se validan como contrato de entrada pero se descartan del Model de presentación.
- `marketQueryKeys.official(criteria)` representará cada consulta; `marketQueryKeys.officialAll()` permitirá invalidar todas las variantes tras una compra oficial.
- El antiguo `getCurrentQuote`, `useMarketQuotesQuery` y `currentQuote(id)` dejan de participar en `/market/buy`; su eliminación física se decidirá por consumidores reales durante implementación, sin afectar APIs de `players`.

### URL, filtros y consulta

- Claves oficiales: `name`, `leagueId`, `teamId`, `position`, `nationality`, `minPrice`, `maxPrice`, `sort` y `page`. `hasQuote` y `size` heredados se eliminan con `replace`; el servicio fija `size=8` y no envía `hasQuote`.
- La URL usa `page` base 0, igual que el contrato; valores ausentes producen los defaults UI `page=0`, `size=8`, `sort=id,asc` sin necesidad de escribirlos hasta que cambien.
- Selects, rango y orden actualizan URL y consulta automáticamente. La búsqueda mantiene un valor editable local y publica `name` mediante debounce; limpiar búsqueda cancela el valor pendiente.
- Todo cambio de filtro u orden fija `page=0`. Cambiar página solo modifica `page`.
- La capa de parseo normaliza tipos y enumera valores permitidos antes de consultar. Un rango invertido queda en estado inválido local, muestra error inline y no habilita la query.
- `usePlayerCatalogFiltersQuery` se reutiliza desde la API pública de `players`; esa API debe exponer el Hook y `PlayerCatalogFilters`. Los equipos visibles se derivan por `leagueId`; limpiar/cambiar liga limpia `teamId` incompatible.
- La query distingue `pending`, `fetching` con datos previos, `success`, `error`, vacío global y sin resultados. El cambio de criterios puede conservar visualmente la página previa mientras anuncia actualización, pero nunca habilita compra con un jugador que ya no pertenece al resultado vigente.
- Los errores contractuales del catálogo se traducen a mensajes seguros; parámetros inválidos producidos por edición manual de URL se normalizan antes de la consulta y no se propagan como estado válido.

### DTOs, Models y Mapper

- `OfficialMarketPageResponseDTO`: `content`, `page`, `size`, `totalElements`, `totalPages`.
- `OfficialMarketPlayerResponseDTO`: `id`, `name`, `position`, `nationality?`, `teamId?`, `teamName?`, `teamBadgeUrl?`, `leagueId?`, `leagueName?`, `imageUrl?`, `quote` nullable.
- `OfficialMarketQuoteResponseDTO`: `price`, `previousPrice?`, `variationPercentage?`, `performanceScore`, `multiplier`, `calculationOrigin`, `effectiveDate`, `calculatedAt`.
- `OfficialMarketPlayer` conserva identidad deportiva nullable y solo los datos de cotización presentables: `price`, `previousPrice`, `variationPercentage`, `effectiveDate`. No contiene `performanceScore`, `multiplier`, `calculationOrigin` ni `calculatedAt`.
- `OfficialMarketPage` conserva metadatos paginados y Models mapeados. Ningún Component recibe DTOs.
- Validaciones: IDs e importes enteros seguros según nulabilidad; `price > 0`; `previousPrice` nulo o entero no negativo; porcentajes finitos; fechas ISO válidas; metadatos paginados coherentes y contenido no mayor que `size`.

### Responsabilidades UI

| Unidad | Responsabilidad |
| --- | --- |
| `MarketBuyPage` | Componer estructura semántica de la página, Page Hook y modal; sin reglas de negocio. |
| `MarketHeader` | Identidad de página y saldo disponible; no muestra total. |
| `MarketFilters` | Dos filas horizontales, controles automáticos, error de rango, actualización y limpiar; sin botón Aplicar. |
| `OfficialMarketPanel` | Barra única de resultados, estados de consulta, grilla desplazable y paginación externa al scroll. |
| `MarketBuyCatalog` | Renderizar la grilla adaptable y estados de lista por props. |
| `MarketPlayerCard` | Identidad deportiva, precio/variación/anterior/vigencia y compra; estado `quote: null` estable. |
| `BuyTokenModal` | Resumen aprobado, cantidad, accesos rápidos absolutos, estimación, procesamiento, éxito/error y accesibilidad. |
| `useMarketBuyPage` | URL, debounce, consulta, selección, formulario, guardas, compra, invalidaciones y foco. |
| `useOfficialMarketQuery` | Estado remoto del catálogo por criterios normalizados. |
| `marketService` | Transporte autenticado y serialización HTTP. |
| `market.mapper` | Validación de respuestas y transformación DTO ↔ Model. |

Los componentes exclusivos siguen locales a `MarketBuyPage`. Se reutilizan `Button`, `Field`, `Modal`, `Pagination`, `PlayerPortrait`, `TeamBadge`, `CompetitionLogo` y `NationalityFlag`; no se crean duplicados.

### Compra e invalidaciones

- `useBuyQuantityForm`, `useBuyTokensMutation`, `toOrderBuyRequestDTO`, `toBuyResult`, mensajes seguros, guarda síncrona y autocierre oficial se reutilizan.
- El modal usa la cotización ya embebida en `OfficialMarketPlayer`; abrirlo no genera GET adicional.
- Tras éxito oficial se invalidan `marketQueryKeys.officialAll()`, `authQueryKeys.all`, `portfolioQueryKeys.summary` y el prefijo público de holdings. El resultado efectivo del POST alimenta toast y estado de éxito.
- P2P conserva `marketQueryKeys.listings()`, `useCommunityMarketQuery`, compra por `{ listingId }`, invalidaciones y ausencia de autocierre obligatorio.

### Flujo de datos P2P

```text
GET /api/listings ──> Service ──> Mapper ──> MarketListing[]
                                              │
GET catálogo players ──> Models deportivos ───┤
                                              ▼
                                  buildCommunityPlayers
                                              │
URLSearchParams ──parse/normalize──> filtros, orden, page, player
                                              │
                                              ▼
                              filtrar ──> ordenar ──> paginar 12
                                              │
                         catálogo <───────────┴───────────> detalle
                                                                  │
                                                                  ▼
                                                        POST { listingId }
```

- `useCommunityMarketPage` seguirá siendo el único propietario de `useSearchParams`, derivación de catálogo, selección de jugador, modal, compra e invalidaciones.
- La URL P2P usa `search`, `league`, `position`, `sort`, `page` y `player`. Los defaults son búsqueda vacía, `league=all`, `position=all`, `sort=price` y `page=0`; pueden omitirse hasta cambiar.
- La búsqueda mantiene un borrador local y publica `search` después de 300 ms de debounce. Liga, posición y orden escriben inmediatamente; cualquier cambio de filtro u orden fija `page=0`.
- El orden de transformación es obligatorio: validar listings y jugadores, agrupar por jugador, filtrar el conjunto agrupado, ordenar con desempates deterministas y paginar localmente de a 12.
- `player=<id>` selecciona el detalle dentro de la misma Page. El detalle usa las publicaciones ya presentes en `marketQueryKeys.listings()`, sin `GET /api/listings/{id}`; cerrar elimina solo `player` y conserva el resto de la URL.
- Una URL inválida no dispara endpoints diferentes: valores desconocidos usan defaults seguros, una página fuera de rango se normaliza al rango disponible y un jugador ausente produce el estado de detalle sin ofertas con regreso al catálogo.
- La selección de una publicación abre `BuyListingModal`; cantidad, precio, total y vencimiento son solo lectura. La confirmación repite la guarda del botón y nunca envía cantidad, jugador, vendedor ni precio.
- Ante conflicto de disponibilidad se invalida `listings()` y se actualizan catálogo y detalle. Ante red sin respuesta concluyente no hay retry automático ni éxito inferido. El éxito permanece hasta cierre explícito.

### Responsabilidades P2P

| Unidad | Responsabilidad |
| --- | --- |
| `MarketP2PPage` | Componer encabezado, panel y Page Hook sin reglas de negocio. |
| `MarketP2PHeader` | Identidad de página y saldo; no posee filtros ni total. |
| `CommunityMarketFilters` | Controles horizontales automáticos y limpiar; recibe estado y callbacks por props. |
| `CommunityMarket` | Barra de resultados, estados del catálogo, grilla de cuatro columnas y paginación local. |
| `CommunityListingsPanel` | Detalle completo del jugador y sus publicaciones ya cargadas. |
| `BuyListingModal` | Confirmación de lote fijo, procesamiento, error y éxito persistente. |
| `useCommunityMarketPage` | URL, debounce, agrupación derivada, filtros, orden, página, selección, compra, guardas e invalidaciones. |
| `useCommunityMarketQuery` | Estado remoto de la colección completa bajo `marketQueryKeys.listings()`. |
| `marketService` | `GET /listings` y `POST /orders/buy` autenticados. |
| `market.mapper` | Validación estricta de envoltura/listings y respuesta de compra. |

### Layout y secuencia de implementación futura

1. **Fase frontend 1 — lógica, estado y estructura**: DTOs, Models, Mapper, Service, query keys/Hooks, parseo URL de ambos mercados, Page Hooks, props y JSX semántico según ambos wireframes. Sin maquetado fino.
2. Ejecutar únicamente los controles frontend permitidos cuando exista código modificado y entregar el informe de integridad.
3. **Gate humano obligatorio**: detenerse hasta autorización explícita.
4. **Fase frontend 2 — maquetado**: SCSS Modules y contenedores visuales, padding de página de 8 px por lado, filtros, grillas, detalle y modales conforme a `docs/wireframes/official-market.md` y `docs/wireframes/community-market.md`.
5. Validación visual humana de ambos mercados en 1920 × 913, 1440, 768 y 375 px, sin alterar lógica validada en Fase 1.

## Complexity Tracking

No aplica: el diseño no introduce violaciones constitucionales ni excepciones arquitectónicas.

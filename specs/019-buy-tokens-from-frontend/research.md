# Research: Comprar tokens en el mercado

## Fuentes verificadas

- [spec.md](spec.md), Constitución y workflow SDD vigentes.
- `GET /api/market/official` implementado por SPEC-010, sus DTOs backend y [contrato autoritativo](../010-buy-tokens/contracts/official-catalog.md).
- Implementación actual de `features/market`, `features/players`, `features/auth`, `features/portfolio`, `AppLayout`, `PageLayout` y componentes shared.
- Arquitectura, dependencias, convenciones y tecnologías del frontend.
- Wireframes aprobados: [Mercado Oficial](../../docs/wireframes/official-market.md) y [Mercado P2P](../../docs/wireframes/community-market.md).

## Decisiones

### Catálogo oficial consolidado

**Decision**: `market` consumirá directamente `GET /api/market/official` mediante un Service, Mapper, Query Hook y Models propios. `/market/buy` dejará de combinar el catálogo de `players` con consultas individuales de cotización.

**Rationale**: El endpoint entrega exactamente la página, filtros, orden y cotización requerida en una respuesta; elimina N+1, estados parciales y composición de contratos incompatibles.

**Alternatives considered**: Conservar `usePlayersQuery` + `useMarketQuotesQuery` fue descartado por contradecir SPEC-010 y FR-002. Usar ranking o cotizaciones demo fue descartado porque no representan el precio operativo de compra.

### Propiedad de DTOs y Models

**Decision**: Los DTOs reales del catálogo y sus Models de presentación pertenecerán a `features/market`. El Mapper validará todos los campos contractuales de entrada y descartará de la UI `performanceScore`, `multiplier`, `calculationOrigin` y `calculatedAt`.

**Rationale**: El catálogo es un contrato de mercado, no una extensión del catálogo genérico de jugadores. Separar DTO de Model evita filtrar detalles técnicos a Components y permite respetar el wireframe.

**Alternatives considered**: Ampliar `Player` fue descartado por mezclar contextos. Propagar el DTO completo fue descartado por vulnerar la frontera Service/Mapper y facilitar exposición accidental.

### Opciones deportivas

**Decision**: Reutilizar `usePlayerCatalogFiltersQuery` y `PlayerCatalogFilters` desde la API pública raíz de `players`; ampliar únicamente esos exports si todavía no están publicados.

**Rationale**: `GET /api/players/filters` ya tiene Service, Mapper, caché y Model correctos, y proporciona la relación liga-equipo exigida.

**Alternatives considered**: Duplicar el endpoint en `market` fue descartado por duplicar contrato y caché. Derivar opciones desde la página oficial fue descartado porque una página no representa el universo de filtros.

### URL y debounce

**Decision**: `useMarketBuyPage` será propietario de `useSearchParams`, normalización, serialización y valor editable de búsqueda. Los selects escriben inmediatamente; `name` se publica después de un debounce breve definido como constante local. Todo cambio salvo `page` restablece la página a cero.

**Rationale**: Un único propietario evita divergencia entre URL, controles y query key. Mantener la edición de búsqueda separada impide solicitudes por pulsación sin retrasar otros filtros.

**Alternatives considered**: Estado solo local fue descartado por FR-008. Botón Aplicar fue descartado por el diseño aprobado. Debounce en Service o Query Hook fue descartado porque es comportamiento de interacción, no transporte ni estado remoto.

### Parámetros inválidos de URL

**Decision**: Parsear la URL a criterios tipados, aceptar solo valores contractuales y reemplazar valores inválidos por defaults seguros. Un rango válido individualmente pero invertido permanece visible como error local y pausa la query hasta corregirse.

**Rationale**: La URL es entrada externa; la UI no debe emitir solicitudes inválidas ni quedar en un estado imposible.

**Alternatives considered**: Enviar parámetros crudos y mostrar el 400 fue descartado por seguridad, UX y FR-006. Reescribir agresivamente toda URL al montar fue descartado por generar historial innecesario.

### Estado remoto durante cambios

**Decision**: Cada criterio normalizado forma parte de `marketQueryKeys.official(criteria)`. Las solicitudes reciben `AbortSignal`; durante un cambio puede conservarse la página anterior como referencia visual con un indicador de actualización, pero acciones transaccionales solo usan el resultado vigente.

**Rationale**: Evita parpadeos sin confundir datos previos con el catálogo actual ni habilitar compras sobre selección obsoleta.

**Alternatives considered**: Vaciar siempre la grilla fue descartado por degradar filtros automáticos. Reutilizar datos previos como si fueran actuales fue descartado por integridad transaccional.

### Invalidación de compra oficial

**Decision**: Tras éxito se invalida el prefijo completo del catálogo oficial, además de usuario actual, resumen y holdings. No se invalida la cotización individual antigua ni datos demo.

**Rationale**: Precio, variación, disponibilidad visible y metadatos paginados pueden cambiar; invalidar solo el jugador no cubre todas las páginas y filtros.

**Alternatives considered**: Actualización optimista fue descartada porque el servidor decide precio y stock. Invalidar `currentQuote(id)` fue descartado porque deja de ser fuente de `/market/buy`.

### Componentes y layout

**Decision**: Mantener componentes locales existentes de `MarketBuyPage`, adaptar `MarketFilters` a dos filas automáticas y conservar `OfficialMarketPanel` como contenedor de barra de resultados, grilla desplazable y paginación. El diseño se implementará en dos fases con pausa humana.

**Rationale**: Las responsabilidades actuales son reutilizables y el wireframe define cajas concretas; no hace falta crear una jerarquía nueva.

**Alternatives considered**: Panel lateral fue descartado por decisión aprobada. Mover componentes exclusivos a `shared` fue descartado por falta de reutilización real.

### Fuente y transformación P2P

**Decision**: Mantener `GET /api/listings` sin parámetros como única fuente remota de publicaciones. `useCommunityMarketPage` combinará esa colección validada con el catálogo completo de jugadores, agrupará por jugador y aplicará filtros, orden y paginación local de 12 en ese orden.

**Rationale**: El backend real no pagina ni filtra listings y ya excluye publicaciones propias, inactivas, vencidas o de jugadores no visibles. La transformación local satisface el contrato sin inventar un endpoint.

**Alternatives considered**: Enviar query parameters a `/listings` fue descartado porque no existen. Paginar antes de agrupar o filtrar fue descartado porque produciría agregados y totales incorrectos.

### URL y navegación P2P

**Decision**: `useCommunityMarketPage` será el único propietario de `search`, `league`, `position`, `sort`, `page` y `player` en `useSearchParams`. La búsqueda tendrá borrador local y publicará `search` después de 300 ms de debounce; liga, posición y orden serán inmediatos. Cambiar filtro u orden fijará `page=0`; abrir/cerrar detalle solo agregará o quitará `player`.

**Rationale**: La URL aprobada debe reconstruir catálogo y detalle en recarga y Back/Forward sin duplicar estado navegable. El Page Hook ya posee la navegación `player` y es la extensión mínima coherente.

**Alternatives considered**: Conservar filtros y página en `useState` fue descartado por la spec vigente. Crear un store global fue descartado porque la URL ya es la fuente adecuada y el estado pertenece a una sola Page.

### Detalle y compra P2P

**Decision**: El detalle se deriva de la colección cacheada por `marketQueryKeys.listings()` y no consulta `GET /api/listings/{id}`. La selección de una publicación abre el modal existente y confirma exclusivamente `{ listingId }`; el resultado permanece visible hasta cierre explícito.

**Rationale**: La lectura de mercado contiene todos los campos necesarios y el POST es autoritativo para vigencia, saldo, stock y liquidación. Evitar otra lectura impide estados divergentes sin perder validación transaccional.

**Alternatives considered**: Consultar cada publicación por id fue descartado por redundante. Permitir cantidad parcial fue descartado porque el backend liquida lotes completos. Retry automático del POST fue descartado por incertidumbre transaccional.

### Preservación de flujos

**Decision**: Mantener separados los Page Hooks, Models navegables y query keys de Oficial y P2P, reutilizando únicamente transporte, Mapper de compra, mutaciones, mensajes, componentes shared e invalidaciones compatibles.

**Rationale**: Los mercados comparten infraestructura y resultado de compra, pero difieren en lectura, paginación, selección, payload y ciclo de éxito.

**Alternatives considered**: Unificar ambos mercados en un Hook, modelo o página fue descartado porque introduciría condicionales y acoplaría contratos diferentes.

## Resultado

No quedan decisiones arquitectónicas abiertas. Todas las decisiones necesarias están resueltas por la spec, contratos, arquitectura vigente o wireframe aprobado.

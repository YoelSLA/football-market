# Implementation Plan: Comprar tokens desde el frontend

**Branch**: `feat/019-buy-tokens-from-frontend` | **Date**: 2026-10-23 | **Spec**: [spec.md](spec.md)

## Resumen y límites

**Estado de entrega observado (2026-10-25):** código frontend presente y último build satisfactorio; la integración autenticada de cotizaciones reales y la compra E2E siguen pendientes. Sin quote válida no se habilita la compra. Este plan contiene antecedentes de alcance sustituidos explícitamente por decisiones posteriores; la preparación de cierre no los reactiva. Consultar [informe de implementación](implementation-report.md) y [trabajo requerido/futuro](future-work.md). No se ejecuta CONVERGE ni se declara cierre mientras falte esa evidencia.

Las rutas protegidas del módulo Mercado separan la emisión oficial (`/market/buy` — Mercado Oficial) de las ofertas de usuarios (`/market/p2p` — Mercado P2P) en dos páginas y pestañas de navegación independientes. En `/market/buy`, se listan los jugadores visibles del catálogo sin consultar stock, se consulta la cotización vigente persistida por jugador y se compran tokens directamente al superusuario mediante `POST /api/orders/buy`; el backend verifica stock al confirmar. En `/market/p2p`, se consultan las publicaciones de la comunidad (`GET /api/listings`) agrupadas por jugador y se compran lotes completos mediante `POST /api/orders/buy`. `market` es independiente de cualquier otra feature. No se cambian comportamientos de `players`, `portfolio` ni backend; sus `index.ts` raíz podrán ampliar **solo** los exports necesarios en la futura implementación. `app` es el único punto de composición entre features.

## Contexto técnico

- React 19, TypeScript 7, TanStack Query 5, Axios, React Router 7, Sass; versiones efectivas en `frontend/package-lock.json`.
- React-Toastify: nueva dependencia planificada para avisos de éxito/error; registrar la versión instalada y su propósito en `docs/frontend/technologies.md` durante la implementación.
- Frontend exento de tests automatizados. El control ejecutable autorizado para cambios de código es `npm run build` desde `frontend`; escenarios manuales en [quickstart.md](quickstart.md) a cargo del usuario.
- No introducir servicios transversales de negocio en `infrastructure`. El cliente `infrastructure/http` ya satisface la necesidad técnica de autenticación y HTTP.

## Constitution Check (antes y después del diseño)

| Principio | Evaluación |
| --- | --- |
| Alcance y simplicidad | API pública mínima en cada feature; sin duplicar el listado, validación ni keys existentes; modal genérico solo si su responsabilidad UI justifica shared. |
| Seguridad | Solicitudes autenticadas; validación de cantidad en frontend sin sustituir backend; errores genéricos sin filtrar detalles sensibles. |
| Contratos | `GET /api/players/{id}/quotes/current` y `POST /api/orders/buy` son contratos backend existentes; mapper DTO → Model dentro de Service. |
| Arquitectura | `app → players + market + portfolio` mediante `index.ts` raíz. Ninguna feature importa de otra. `shared` no depende de features. |
| Trazabilidad | Spec, contratos, modelo y tareas deben concordar; nuevo ANALYZE requerido después de estos cambios. |
| Verificación | Sin ejecución de tests, ni directa ni indirecta. El build solo corresponderá después de modificar código. |

**Resultado actual:** Diseño documental coherente con las decisiones del usuario; aún no constituye ANALYZE vigente ni autorización de IMPLEMENT.

## Fronteras y composición

```text
app/router: AuthGuard + ruta /market/buy
  └─ app/market/MarketBuyRoute (adaptador de composición, sin reglas de compra)
       ├─ players: playersService.getPage, playersQueryKeys, PLAYERS_PAGE_SIZE
       │          y tipos Player/PlayersPage (API raíz; sin cambios de lógica)
       ├─ market: MarketBuyPage con datos de catálogo mapeados por app a MarketPlayer,
       │          estado de carga completa y callback onPurchaseSuccess (API raíz)
       └─ portfolio: portfolioQueryKeys.summary y holdings (API raíz, solo en app)

market/MarketBuyPage → useMarketBuyPage (Page Hook de coordinación)
  ├─ useMarketQuotesQuery(playerIds) → marketService.getCurrentQuote
  │    → GET /api/players/{id}/quotes/current (una query por id, caché propia)
  ├─ useBuyQuantityForm (Form Hook: solo estado y validación 1–100)
  └─ useBuyTokensMutation → marketService.buyTokens → POST /api/orders/buy
       → DTO → Mapper → BuyResult (sin playerName)
  └─ éxito: invalidar marketQueryKeys.currentQuote(id), componer aviso con
     selectedPlayer.name + BuyResult y llamar onPurchaseSuccess(id)
market/MarketBuyPage → MarketPlayerCard y BuyTokenModal (Components locales
  solo UI y callbacks, sin Hooks ni HTTP propios)
app/MarketBuyRoute: onPurchaseSuccess → invalidación summary + holdings(*)
```

`MarketBuyPage` conecta su Page Hook, que coordina Form/Query/Mutation Hooks, filtros, orden, paginación y estado del modal. **El Form Hook solo valida/configura el formulario; no invoca el Mutation Hook.** `app` provee todo el catálogo mediante el Service/Mapper existente de players; no contiene reglas de filtrado ni compra. Los Components locales reciben exclusivamente props. Abrir/cerrar el portal no remonta listado ni reinicia scroll, borrador/aplicación de filtros, página, pestaña o URL. El Query Hook de market conserva una key por id real; ficha y modal comparten ese resultado. Solo `404 QUOTE_NOT_FOUND` significa ausencia: otros errores se muestran con reintento GET y no se reemplazan con demos. Se preservan `calculatedAt` y `effectiveDate`. El Service devuelve `BuyResult` sin nombre; el Page Hook compone la notificación con `selectedPlayer.name`. Market invalida su quote y notifica éxito a app, que invalida Portfolio sin retrasar la notificación.

**Exports mínimos futuros, no implementados aquí:**

- `features/players/index.ts`: conservar exports y añadir `playersService`, `playersQueryKeys`, `type PlayersPage` para el consumidor autorizado app. `PLAYERS_PAGE_SIZE` y `type Player` ya están expuestos. No cambiar Service/Mapper/Hooks existentes ni publicar DTOs o cotizaciones demo. El Service devuelve Models y es una dependencia permitida de app mediante API pública; no se convierte en dependencia de market.
- `features/portfolio/index.ts`: `portfolioQueryKeys`, reexportadas de `./constants` para uso exclusivo de `app`.
- `features/market/index.ts`: `MarketBuyPage` y `type MarketPlayer` (contrato estructural propio de `market`, sin importar `Player` de `players`). La app convierte los campos requeridos por la UI desde `Player`, sin copiar la lógica de mapeo HTTP.

La key de cotización persistida de `market` es propia, por ejemplo `['market', 'current-quote', playerId]`; no reutilizar `playersQueryKeys.tokenQuote` (`['players', 'demo-quotes', id]`). `app` invalida `portfolioQueryKeys.summary` y todas las keys `holdings` mediante el prefijo derivado de `portfolioQueryKeys.holdings('ALL', 0, 1).slice(0, 2)` (sin repetir los literales de key ni invalidar todas las consultas Portfolio). La Page de market puede seguir mostrando la cotización reconsultada al invalidarse su key; el modal preserva los datos mostrados durante la operación y no refluye el listado.

## Responsabilidades y ubicaciones previstas

| Área | Archivos previstos | Responsabilidad |
| --- | --- | --- |
| `app` | `router/AppRouter/AppRouter.tsx`, `navigation/sections.ts`, `providers/AppProviders/AppProviders.tsx` | Rutas protegidas (`/market/buy`, `/market/p2p`), navegación en Navbar, ToastContainer global y layout contenido. |
| `features/players` | Solo `index.ts` | Exportar contratos de lectura y tipo existentes, sin alterar lógica. |
| `features/portfolio` | Solo `index.ts` | Exportar query keys existentes, sin alterar lógica. |
| `features/market` | `pages/MarketBuyPage/` (Oficial), `pages/MarketP2PPage/` (P2P), `form/hooks/`, `form/schemas/`, `hooks/pages/`, `hooks/queries/`, `hooks/mutations/`, `market.service.ts`, `market.mapper.ts`, `constants/marketQueryKeys.ts`, `types/`, `utils/`, `index.ts` | Páginas independientes: `MarketBuyPage` coordina emisión oficial y cotizaciones; `MarketP2PPage` coordina listings comunitarios y detalle de ofertas. Componentes locales reciben datos por props. |
| `shared` | `components/Modal/` y barrel | Portal, foco, ARIA, ESC, overlay; ninguna lógica de compra. Reusar `Button` y `Field`. |
| Configuración | `frontend/package.json`, `package-lock.json`, `docs/frontend/technologies.md` | Instalación y registro de React-Toastify. |

El Modal compartido no conoce el campo de cantidad: recibe una referencia genérica de foco inicial, además del trigger al que devolver foco. No cambia la URL ni inserta contenido en el flujo de la página; portal/overlay sin bloquear scroll mediante cambios al layout o al `body`. El campo cantidad empieza en `1` cada vez que se inicia una compra y muestra estimación si hay cotización; el Form Hook acepta texto de dígitos que representa un entero 1–100 y transforma el valor válido en `number` antes del POST. Si se borra o introduce texto inválido, deshabilita Confirmar. La Page proporciona al Modal la referencia al campo de cantidad para foco inicial; el trap mantiene el foco dentro del diálogo y lo devuelve al botón que abrió la compra salvo que la sesión expire y la ruta se desmonte. Durante submit se deshabilitan cierre, cancelación y confirmación, con `aria-busy`, spinner y protección síncrona contra doble envío. La respuesta exitosa muestra identificador, cantidad, precio realmente aplicado, importe y `executedAt`, y permite cerrar manualmente o a los 2 s. React-Toastify se configura en `app/providers`.

## Contratos, decisiones y riesgo

- [Contrato de consumo](contracts/buy-tokens-frontend.md), [modelo](data-model.md), [decisiones](research.md) y [escenarios](quickstart.md).
- Backend ya expone el GET real con `price`, `effectiveDate`, `calculatedAt` y `404 QUOTE_NOT_FOUND`; el POST aplica el precio vigente de forma autoritativa. Una estimación puede diferir del importe confirmado: no presentarla como precio congelado.
- `usePlayersQuery(page)` consulta solo 12 jugadores. La revisión de filtros sustituye la composición de una sola página por la carga completa descrita abajo. No consultar stock ni modificar el hook existente.
- Las referencias 04/05 guían composición visual de cards/modal/overlay; cifras de saldo, órdenes pendientes, estadísticas deportivas y variaciones de 24 h no tienen fuente prevista en esta feature. No inventarlas ni presentar demos como reales; mantener la estructura visual aplicable al alcance definido por la spec.
- La existencia de React-Toastify o un build no acredita testing ni revisión humana.

## Revisión de filtros y fidelidad visual (2026-10-24)

La solicitud humana amplía explícitamente el alcance y autoriza correcciones después de ANALYZE sin blockers. Sustituye la exclusión anterior de filtros nuevos; no aprueba automáticamente los criterios del revisor afectados por el cambio.

### Carga completa sin duplicar players

`app/market/MarketBuyRoute/useMarketCatalog.ts` compone TanStack Query y el Service público existente. Lee página 0 con `size=PLAYERS_PAGE_SIZE` (12), descubre `totalPages` y recorre las restantes con **cuatro workers como máximo**, usando `queryClient.fetchQuery` y `playersQueryKeys.page(page, size)`. Reutiliza cada página fresca durante cinco minutos; no refetch automático al enfocar/reconectar y no reintenta automáticamente 401/403. La query agregada de composición tiene key bajo `app/market`, cancela al desmontar y no entrega un subconjunto como catálogo completo. El reintento reutiliza las páginas correctas y solo recupera las faltantes/obsoletas. Si los metadatos cambian durante el recorrido, informa inconsistencia y solicita reintento; no entra en bucle de recarga. Deduplicar ids no permite ocultar una carga incompleta.

No usar `size=100`: aunque el contrato backend lo admite, `players.mapper.ts` exige `content.length <= 12`; cambiarlo está fuera del permiso actual. No copiar DTO, validación o GET de players a market ni a app. Esta solución minimiza solicitudes dentro del contrato consumidor existente; una mejora de tamaño de página requiere otro alcance. La cantidad mínima de lecturas del catálogo es `ceil(totalElements / 12)` con esa restricción.

### Filtros y cotizaciones

`useMarketBuyPage` mantiene borrador/aplicación de filtros y página local (inicial seis elementos). El panel local `MarketFilters` no conecta Hooks. Una utilidad pura de market normaliza búsqueda y combina nombre/club, liga y posición sobre todo el catálogo; después aplica rango inclusivo de precio y orden ascendente/descendente y finalmente pagina. Limpiar un filtro o todos se aplica sin perder los demás y vuelve a página 0. Mostrar contador real y selector 6/12/24; precios en créditos con precisión de centésimas, sin rangos prefijados basados en valores inventados.

Para ordenar/filtrar por precio se necesitan todas las cotizaciones de los candidatos tras filtros de identidad: sin endpoint bulk no puede evitarse una lectura por id no cacheado. Se conserva `marketQueryKeys.currentQuote(id)`, con caché fresca de 30 segundos, **hasta seis lecturas simultáneas**, cancelación y sin ráfagas por cada pulsación (solo al aplicar). La ordenación inicial por precio coincide con la referencia; requiere completar esas lecturas. No crear miles de requests concurrentes ni reconsultar todas al abrir el modal, cambiar página o modificar cantidad. Errores se reintentan explícitamente; no ocultarlos como ausencia ni presentar totales definitivos mientras falten lecturas. `QUOTE_NOT_FOUND` y precios no operativos quedan al final sin rango y se excluyen con rango. Empates: nombre e id, independientemente de la dirección de precio. Si posteriormente se admite orden por nombre sin rango, solo se consultan quotes de la página visible y selección; no se añade como requisito del mockup.

### Presentación

Fondo en wrapper de Page; cadena flex/min-height/min-width de `PageLayout` existente, scroll de contenido desktop y natural móvil, sin modificar AppLayout/Navbar compartidos. Header y panel lateral reproducen la composición aplicable de ref. 04; tres columnas desktop, dos cuando el espacio de tarjeta lo permita y una a 375px. Las métricas sin fuente se omiten explícitamente según FR-19, no se rellenan con demos. La diferencia frente a la imagen es deliberada y debe constar en entrega.

Tarjetas compactas y modal reutilizan datos reales mediante props; fallback importado de `shared/assets/Player Generic-Icon.png`, último recurso tras URLs externas (Vite empaqueta el local). Modal con liga y posición, cantidad −/+ y 1/5/10/50/100, resumen de precio/cantidad/estimación y spinner real durante submit. `shared/Modal` conserva portal/foco/ARIA: overlay sin blur, scroll propio y overscroll contenido; impedir propagación hacia mercado sin alterar ancho/body, URL ni estado. Verificar teclado y restauración de foco sin scroll inesperado.

## Diseño vigente: rediseño exclusivamente visual

La última solicitud del usuario sustituye la sección «Revisión de filtros y fidelidad visual»: no se implementan catálogo completo, nuevos exports, workers ni filtros funcionales. Conservar `usePlayersQuery(page)`, lógica de compra y contratos HTTP. No modificar lógica de players/portfolio/backend. Los exports de Service/keys propuestos arriba **no se implementan**. Research conserva la investigación, no una obligación funcional vigente.

`MarketBuyPage` compone Components locales `MarketHeader`, `MarketFilters`, fichas y footer con datos/callbacks por props. El wrapper posee fondo y scroll vertical en el espacio flex disponible sin modificar layouts globales. Panel lateral completo deshabilitado con indicación «Pendiente»; métricas sin fuente «No disponible», contador real. Mantener tamaño efectivo 12 y paginación real; selector de tamaño visual deshabilitado. Tres columnas desktop junto al panel, dos tablet y una móvil; layout fluido y sin alturas fijas de Page. Los datos deportivos/variación sin fuente se representan con «Sin datos», no ceros ni porcentajes ficticios.

Cards y modal: retrato pequeño con fallback final local importado desde shared/assets; posición con badge por valor real, iconos genéricos para equipo/liga (sin asignar escudos/banderas no disponibles). Cantidades rápidas y −/+ se muestran deshabilitados y pendientes para no ampliar la lógica; la cantidad editable existente funciona. La modal muestra liga, mantiene resumen/precio/centésimas y resultado efectivo, foco/ARIA y controles existentes. Overlay sin blur; scroll propio y overscroll contenido, sin modificar ancho del body o flujo del mercado. Precio/cotización/errores siguen contratos existentes.

Esta revisión visual no cambia la composición entre features ni el origen real de cotizaciones. Los checklists históricos se conservan sin nuevas aprobaciones; no se considera satisfecho lo que no se verificó visualmente. Tareas V-01–V-04 en tasks.md delimitan este trabajo.

## Próxima fase

### Ajuste localizado de scroll

### Lectura segura de cotización y ajustes visuales

Mantener composición actual, scroll contenido y APIs de otras features. Metadata de tabs en app y nuevos iconos solo por shared/icons. Margen de filtros y scrollbar local por SCSS.

Service captura solo 404 + QUOTE_NOT_FOUND como null; otros fallos se convierten en Model de error de lectura tipado de market, preservando cancelación. Mapper valida respuesta desconocida antes de entregar MarketQuote (precio entero positivo seguro y fechas válidas), sin cambiar contrato HTTP. Query Hook produce estado de lectura, carga/pausa/error y reintento GET por id usando error Model, sin conocer DTO ni importar otras features. No reintentar automáticamente 401/403/404 inesperado/respuesta inválida; evitar redirecciones/reintentos en bucle. Page Hook selecciona únicamente quote de consulta exitosa y resuelta para estimación/confirmación y protege también handler. Component local MarketQuoteFeedback recibe estado/mensaje/acción por props y se reutiliza en ficha y modal, sin Hooks. Explicación accesible del bloqueo junto al botón, sin filtrar datos técnicos del servidor.

La falta de sesión del cliente HTTP se identifica en el Page Hook mediante la presencia de sesión pública de infrastructure/storage, no con acceso directo al almacenamiento ni cadenas de error de Axios. El interceptor global sigue siendo autoridad para limpiar sesión/redirigir ante 401; no modificar infraestructura.

AppLayout normaliza la ruta y aplica un modificador de contención de scroll cuando el pathname corresponde a `/market/buy` o `/market/p2p`; el CSS de body/html usa `:has` de ese modificador, por lo que desaparece al navegar fuera. No se mutan estilos globales con efectos ni se codifica la altura del navbar. AppLayout y su contenido continúan la cadena flex con altura `100dvh`, min-height:0 y overflow:hidden incluso bajo breakpoints móviles en ambas rutas de mercado.

MarketBuyPage distribuye encabezado, contenido y footer mediante flex, sin usar PageLayout como scroller. Contenido grid con min-height:0, filtros estáticos y main de jugadores con overflow-y:auto; footer no encoge. En móvil, filtros nativos details/summary y métricas compactas; panel de filtros expandido superpuesto, sin consumir altura del listado. Cambios exclusivamente de presentación; queries, compra, formulario, paginación y HTTP no se modifican. No cambiar PageLayout compartido ni el scroll de rutas ajenas.

Revalidar los criterios del revisor afectados por catálogo completo/filtros, ejecutar `/speckit.analyze` y solo después implementar el alcance autorizado si no quedan blockers. Sin compras reales, tests, commits, push ni merges.

## Ampliación de diseño — mercado P2P (2026-11-04)

### Antecedente histórico: Alternativa A (mercado P2P unificado en `/market/buy`)

Inicialmente, la modalidad P2P se concibió como un modo interno dentro de `MarketBuyPage`, alternado mediante
el componente `MarketSelector` y reflejado en la URL como `?mode=community`. Bajo ese esquema preliminar,
ambas modalidades compartían el mismo contenedor de página y el mismo hook coordinador `useMarketBuyPage`.

Esta alternativa fue sustituida por la refactorización arquitectónica (RF-01 a RF-08) para desacoplar
ambos dominios y ofrecer navegación directa desde el Navbar.

## Arquitectura vigente: Separación definitiva en dos páginas (Oficial vs P2P)

La sección Mercado separa de forma definitiva las modalidades en dos páginas y rutas protegidas independientes,
retirando el selector interno `MarketSelector` y el tipo `MarketTab`:

- **Mercado Oficial (`/market/buy`):** emite tokens directamente desde el superusuario.
- **Mercado P2P (`/market/p2p`):** permite la compraventa de publicaciones de tokens entre usuarios.

```text
AppRouter (rutas protegidas AuthGuard)
  ├─ /market/buy ── MarketBuyPage
  │                   └─ useMarketBuyPage
  │                        ├─ usePlayersQuery (catálogo deportivo de jugadores)
  │                        ├─ useMarketQuotesQuery (cotización oficial por jugador)
  │                        ├─ MarketHeader (especializado en emisión oficial)
  │                        ├─ OfficialMarketPanel (MarketFilters + MarketBuyCatalog + MarketPagination)
  │                        └─ BuyTokenModal (useBuyQuantityForm + useBuyTokensMutation → POST /api/orders/buy)
  │
  └─ /market/p2p ── MarketP2PPage
                      └─ useCommunityMarketPage (habilitado por defecto)
                           ├─ useCommunityMarketQuery (GET /api/listings)
                           ├─ queries públicas de players para playerIds únicos (TanStack Query caché)
                           │    ↓
                           │   agrupación + filtros locales + orden + página local (12 jugadores)
                           ├─ MarketP2PHeader (métricas P2P y saldo disponible)
                           ├─ CommunityMarketPanel
                           │    ├─ Vista catálogo: CommunityMarketFilters + CommunityMarket + MarketPagination
                           │    └─ Vista detalle (?player=<id>): CommunityListingsPanel
                           └─ BuyListingModal (useBuyListingMutation → POST /api/orders/buy {listingId})
```

### Datos y caché

- `marketQueryKeys.listings()` identifica la colección P2P y se invalida tras comprar.
- Service recibe `unknown`, Mapper valida el sobre y cada DTO antes de producir Models.
- La agrupación es una utilidad pura y usa aritmética segura; no muta la respuesta ni crea estado remoto
  duplicado.
- Los metadatos de los jugadores se obtienen exclusivamente mediante las queries públicas de `features/players`
  y se cachean por id. No se accede a Service, DTO o archivos privados de esa feature.
- Mientras falten metadatos indispensables, se comunica carga íntegra; los fallos permiten reintentar.
- La página local contiene 12 jugadores y se recalcula después de filtro/orden.

### Estado y URL

- La navegación entre modalidades se gestiona a nivel de aplicación en la barra de navegación superior
  (`NAV_SECTIONS` en `sections.ts`). Se eliminó el parámetro `mode=community` de la URL.
- En `/market/p2p`, la selección de un jugador establece `?player=<id>` en la URL; cerrar el detalle o volver
  al catálogo retira `player` y conserva filtros y orden en memoria.
- Un `player` inválido o que ya no tenga publicaciones vigentes conduce al estado vacío del detalle sin
  revelar recursos. Back/Forward y recarga reconstruyen la vista de forma determinista desde la URL y los datos remotos.

### Componentes y responsabilidades vigentes

| Componente | Ubicación | Responsabilidad |
|---|---|---|
| `MarketBuyPage` | `pages/MarketBuyPage/` | Coordinación de emisión oficial y diálogo de compra con formulario de cantidad. |
| `MarketHeader` | `pages/MarketBuyPage/components/` | Encabezado especializado en emisión oficial y métricas del mercado primario. |
| `OfficialMarketPanel` | `pages/MarketBuyPage/components/` | Contenedor de filtros, catálogo oficial y paginación (sin semántica de tabpanel). |
| `BuyTokenModal` | `pages/MarketBuyPage/components/` | Modal oficial con selección de cantidad 1–100, cotización autoritativa y confirmación. |
| `MarketP2PPage` | `pages/MarketP2PPage/` | Coordinación del mercado secundario P2P, catálogo agrupado y diálogo de compra. |
| `MarketP2PHeader` | `pages/MarketP2PPage/components/` | Encabezado especializado en mercado P2P con saldo disponible y métricas comunitarias. |
| `CommunityMarketPanel` | `pages/MarketP2PPage/components/` | Alterna entre vista de catálogo agrupado y detalle de publicaciones de un jugador. |
| `CommunityMarket` | `pages/MarketP2PPage/components/` | Grilla responsiva de tarjetas de jugadores con ofertas comunitarias. |
| `CommunityMarketFilters` | `pages/MarketP2PPage/components/` | Filtros locales de búsqueda, liga, posición y orden para el catálogo P2P. |
| `CommunityListingsPanel` | `pages/MarketP2PPage/components/` | Listado completo de publicaciones activas para el jugador seleccionado. |
| `BuyListingModal` | `pages/MarketP2PPage/components/` | Diálogo modal de confirmación de compra de lote fijo P2P mediante `listingId`. |

Se reutilizan `MarketFilters`, `MarketPlayerSummary`, `MarketPagination`, `Button` y `Modal` de forma compartida.
`MarketSelector` y el tipo `MarketTab` quedan eliminados definitivamente.

### Errores e invalidaciones

El hook `useCommunityMarketPage` traduce códigos P2P y coordina refetch. Ante respuesta incierta no reintenta
el POST. El éxito invalida `listings`, `authQueryKeys.all`, `portfolioQueryKeys.summary` y el prefijo de holdings.
El detalle se deriva de la misma query y se actualiza atómicamente con el catálogo.

### Secuencia frontend obligatoria

1. **Fase 1 — lógica, estado y estructura:** DTO/Model/Mapper/Service, query/mutation, agrupación, URL,
   JSX semántico y accesibilidad estructural. Sin maquetación fina.
2. **Gate humano obligatorio:** detenerse después del build permitido y revisión estática/diff. No avanzar
   por inferencia.
3. **Fase 2 — estética:** únicamente SCSS y wrappers de layout permitidos, contrastados con los wireframes;
   no cambiar contratos, datos, eventos ni ARIA validados.

### Riesgos aceptados vigentes

- `/api/listings` transfiere el mercado completo y la paginación local de 12 elementos no reduce carga backend.
- Resolver metadatos puede requerir múltiples lecturas, mitigadas por deduplicación y caché de TanStack Query.
- El contrato backend no fija orden; el frontend impone ordenamiento ascendente por precio unitario, vencimiento e id.
- Si el volumen deja de ser moderado, se evaluará otro alcance con agregación/paginación server-side; no
  se anticipa en esta implementación.

## Evolución del diseño — Catálogo consolidado del Mercado Oficial (SPEC-010 / Issue #90)

### Decisión arquitectónica y nuevo flujo de datos

La integración del catálogo oficial consolida la identidad deportiva de los jugadores y sus cotizaciones vigentes
en una única llamada a `GET /api/market/official`, eliminando las consultas individuales N+1 de cotización
(`useMarketQuotesQuery`) dentro de la vista de catálogo.

El flujo transaccional de compra `POST /api/orders/buy`, el diálogo `BuyTokenModal` y la totalidad de la
modalidad Mercado P2P (`/market/p2p`) se preservan sin modificaciones.

```text
AppRouter (/market/buy)
  └─ MarketBuyPage
       └─ useMarketBuyPage (coordinador oficial)
            ├─ sincronización con searchParams de la URL (?page=0&name=...&leagueId=...&teamId=...&sort=...)
            ├─ usePlayersFiltersQuery() ── GET /api/players/filters (ligas y equipos en cascada)
            ├─ useOfficialMarketQuery(params) ── GET /api/market/official
            │    └─ marketService.getOfficialCatalog(params)
            │         ↓
            │       DTO paginado con cotización integrada
            ├─ MarketHeader (título Oficial, saldo disponible y métricas)
            ├─ OfficialMarketPanel
            │    ├─ MarketFilters (filtros dinámicos en cascada, hasQuote, minPrice/maxPrice y orden)
            │    ├─ MarketBuyCatalog (grilla de jugadores)
            │    │    └─ MarketPlayerCard (datos deportivos, variación% del backend y botón Comprar)
            │    └─ MarketPagination (control de páginas server-side)
            └─ BuyTokenModal (abierto al pulsar Comprar con datos consolidados)
                 └─ useBuyQuantityForm + useBuyTokensMutation ── POST /api/orders/buy
```

### Componentes y adaptaciones técnicas

1. **`marketService.getOfficialCatalog(params, signal)`:**
   - Construye los query params normalizados: `page`, `size` (12), `name`, `leagueId`, `teamId`, `position`, `nationality`, `hasQuote`, `minPrice`, `maxPrice` (en centésimas) y `sort`.
   - Realiza la solicitud autenticada a `GET /api/market/official` según el contrato [market-official.md](contracts/market-official.md).
   - Mapper `toOfficialMarketPage(dto)` valida el sobre paginado y transforma cada entrada a `OfficialMarketPlayer`.
2. **Selectores dinámicos en cascada (`MarketFilters`):**
   - Consume las opciones de ligas y equipos desde `GET /api/players/filters` a través de la API pública de `features/players`.
   - La selección de `leagueId` filtra la lista de equipos correspondientes a esa liga. Limpiar o cambiar la liga deselecciona el equipo.
   - Reemplaza los campos obsoletos de inventario por el selector `hasQuote` (*"Todas"*, *"Con cotización"*, *"Sin cotización"*) y los inputs numéricos `minPrice` y `maxPrice` (en créditos).
   - Valida en cliente `minPrice <= maxPrice` antes de aplicar los filtros.
3. **Sincronización reactiva con la URL:**
   - Todos los filtros, el orden (`sort`) y la página (`page`) se reflejan en los query params de `/market/buy`.
   - Modificar cualquier filtro resetea la página a `0`.
   - La navegación Back/Forward y recarga del navegador reconstruye el estado directamente desde la URL.
4. **Adaptación de `MarketPlayerCard`:**
   - Consume directamente la cotización integrada y `variationPercentage` provistos por el backend.
   - Si `quote === null`, renderiza la etiqueta "Sin cotización" y deshabilita el botón "Comprar", sin inventar cotizaciones demo ni timestamps.
5. **Aislamiento y no regresión:**
   - No modifica contratos ni comportamiento de `/market/p2p`.
   - No altera el schema de mutación ni el payload `{ playerId, quantity }` de `POST /api/orders/buy`.

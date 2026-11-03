# Research: Comprar tokens desde el frontend

## Decisiones corregidas (2026-10-23)

| Tema | Decisión y evidencia | Alternativa descartada |
| --- | --- | --- |
| Aislamiento | Arquitectura frontend arts. 6–7, 43–44: features no dependen de otras features ni por barrels; `app` es el único compositor. | Imports de `market` desde `players/hooks`, `players/types` o `portfolio/constants`, aunque exista `index.ts` interno. |
| Listado | `app` usa `usePlayersQuery(page)` y el Model `Player`, exportados mínimamente por `features/players/index.ts`; convierte a `MarketPlayer` (solo campos visuales). Paginación: `PLAYERS_PAGE_SIZE` existente. | Duplicar `GET /players`, su mapper/validación, o renderizar `PlayersPage` sin posibilidad de comprar. |
| Cotización | `market` consulta `GET /api/players/{id}/quotes/current` por `marketService` y Query Hook con key propia. El backend ya ofrece `price`, `effectiveDate`, `calculatedAt`; `404 QUOTE_NOT_FOUND` implica ausencia de precio operativo. | `usePlayerTokenQuoteQuery` genera una serie **demo** con `date` e `isDemo: true`, no cotización operativa. No reutilizar su key ni formatear su fecha como timestamp real. |
| Invalidación | `market` invalida su propia quote tras éxito; `app` recibe callback de éxito e invalida Portfolio mediante `portfolioQueryKeys.summary` y el prefijo de `portfolioQueryKeys.holdings(...)`, exportado solo por `portfolio/index.ts`. | Strings de keys duplicadas, imports cruzados, invalidar toda la caché de Portfolio. |
| HTTP | Los endpoints pertenecen al Service de `market`; Axios autenticado ya existe en `infrastructure/http`. | Agregar lógica funcional o endpoint de negocio a `infrastructure`. |
| DTO/Model | `marketService` mapea DTOs de GET/POST a Models propios, según arts. 17–19. `BuyResult` contiene solo campos del POST; Page Hook usa el nombre del jugador seleccionado exclusivamente para el texto del toast. `executedAt` no es `calculatedAt`. | Incorporar `playerName` al Service, Mapper o Model de respuesta; propagar DTO a UI. |
| UI | `shared/Modal` solo portal, foco genérico por ref, ARIA, responsive y cierre; `BuyTokenModal` y `MarketPlayerCard` son Components **locales de `MarketBuyPage`** sin Hooks. ToastContainer global en app; React-Toastify justificada frente a toast propio por anuncios y gestión de avisos. | Conectar hooks en Components locales o ubicarlos en `features/market/components/` con una sola Page propietaria. |
| Formulario | Form Hook de market controla configuración y validación UX; `useMarketBuyPage` coordina Form, Query y Mutation Hooks, interpreta errores y compone mensajes. El Form Hook no invoca Mutation Hook. | Encadenar Form Hook → Mutation Hook o llevar la coordinación de negocio a un Component local. |
| Listado y filtros | El catálogo lista jugadores visibles independientemente del stock; el backend valida stock en POST. El modal no remonta el listado y preserva estado/filtros existentes, sin exigir filtros nuevos. | Filtrar por stock sin endpoint o duplicar controles internos de players para simular la ref. 04. |
| Cantidad inicial y textual | Inicial `"1"` según ref. 05; texto numérico entero 1–100 (p. ej. `"10"`) se convierte a number antes del DTO; se rechazan fracciones, vacíos y no numéricos. | Tratar el texto normal de un input HTML como payload string inválido. |

## Evidencia consultada

- `frontend/src/features/players/index.ts` y `features/portfolio/index.ts`: actualmente exportan solo Pages; los contratos previstos requieren únicamente ampliar esos dos barrels en la futura implementación.
- `frontend/src/features/players/hooks/queries/usePlayersQuery.ts`, `players.service.ts` y `players.mapper.ts`: listado visible ya resuelto y validado; no duplicarlo.
- `frontend/src/features/players/hooks/queries/usePlayerTokenQuoteQuery.ts` y `demo/tokenQuoteSeries.ts`: cotización de demostración, `staleTime: Infinity`, key `demo-quotes`; no apta para comprar.
- `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java` y `controllers/dtos/responses/QuoteResponseDTO.java`: GET vigente persistido, `calculatedAt` real; [contrato SPEC-016](../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md).
- `frontend/src/features/portfolio/constants/portfolioQueryKeys.ts`: keys `summary` y `holdings(status,page,pageSize)`.

No se requiere modificación funcional de `players`, `portfolio`, backend ni `infrastructure`. La accesibilidad y los objetivos visuales todavía requieren verificación humana; ANALYZE deberá reevaluar el diseño y las checklists existentes.

## Investigación de catálogo completo y filtros (2026-10-24)

Las decisiones de esta sección sustituyen las filas históricas «Listado» y «Listado y filtros» anteriores por ampliación humana explícita. La aprobación histórica no se extiende automáticamente al nuevo diseño.

- `specs/002-player-catalog/contracts/api.md` y `specs/005-players-page/contracts/players-catalog.md`: backend admite `size` 1–100, pero el cliente de catálogo usa 12. `frontend/src/features/players/players.mapper.ts` valida expresamente `content.length <= 12`. No es seguro pedir 100 a través de ese consumidor sin cambiar su lógica.
- `playersService.getPage(page,size,signal)` ya devuelve `PlayersPage` validado. Se propone exponer solo ese Service, las keys y el tipo de página por `players/index.ts`, para uso exclusivo de app. No modificar sus implementaciones, duplicar endpoint/DTO/Mapper ni conectar market a players.
- Alternativas descartadas: una sola página (filtros incompletos); `size=100` (incompatible con mapper actual); requests para todas las páginas simultáneas (ráfaga); cargar todos los componentes `PlayersPage` (UI y responsabilidad incorrectas); repetir el GET en un Service de market (duplicación de contrato de catálogo).
- Estrategia: descubrir metadatos en página 0 y recorrer el resto con cuatro workers; reutilizar caché pública de páginas frescas por cinco minutos, cancelación al salir, reintento explícito que aprovecha las páginas correctas. No confundir resultado parcial con catálogo completo. Validar estabilidad de metadatos durante el recorrido y presentar error recuperable si cambia.
- El contrato de quotes de SPEC-016 documenta GET por jugador, no bulk de cotizaciones actuales. Obtener precios globalmente exige una lectura por candidato no cacheado: no sustituir por ranking, histórico ni demo. Reducir candidatos mediante filtros de identidad antes de consultar precios, limitar a seis lecturas simultáneas, compartir caché por id de 30 s, evitar refetch al enfocar y pulsaciones sin aplicar.
- La ordenación inicial por precio necesita todas esas lecturas antes de paginar: se muestra progreso y fallo/reintento, no un falso total final. Los ausentes confirmados se ordenan al final o excluyen con rango; los errores se comunican por separado. Una mayor eficiencia de red requiere un contrato masivo o cambios de players/backend fuera del alcance; no se implementan aquí.
- El panel usa mínimo/máximo editables en créditos para no inventar umbrales comerciales. Liga y posición se derivan del catálogo completo; búsqueda sin distinguir mayúsculas/acentos en nombre o club. Rango inclusivo y mínimo ≤ máximo. No hay filtro de stock.
- Fidelidad visual limitada conscientemente por datos: sin saldo inventado, órdenes pendientes, estadísticas, variación 24h ni favoritos funcionales. Retratos externos conservan URL de catálogo, con recurso genérico local final existente.

**Decisión posterior vigente:** el usuario sustituyó el desarrollo de filtros por un rediseño exclusivamente visual. La investigación anterior se conserva como antecedente, pero no habilita ni exige catálogo completo, cambios de exports, filtros funcionales o workers. Se mantienen las APIs y lógica existentes; controles nuevos sin lógica quedan deshabilitados y rotulados pendientes. Los checklists históricos no son aprobación del resultado visual.

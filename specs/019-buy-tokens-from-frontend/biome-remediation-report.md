# Remediación manual de Biome — SPEC-019

## Evidencia de origen y alcance

El usuario aportó la salida de `npm run biome:fix`: 289 archivos revisados, 23 reformateados, tres errores, un warning y dos sugerencias restantes. Se corrigieron manualmente los seis hallazgos, sin ejecutar Biome, `--unsafe`, desactivar reglas ni alterar contratos HTTP/compra. Se preserva el formateo realizado por el usuario.

El índice ya contenía cambios staged al iniciar esta tarea (estados AM/MM). Se comparó working tree contra ese índice como base disponible para revisar los cambios automáticos. No se hizo stage, reset, commit, push ni merge; las correcciones nuevas quedan en working tree y **no actualizan automáticamente lo que el usuario había staged**.

## Correcciones

| Diagnóstico | Cambio manual | Conservación de comportamiento |
| --- | --- | --- |
| ARIA no soportado en BuyTokenModal | El contenedor de cantidades rápidas declara `role="group"` y conserva su nombre accesible | Grupo de controles relacionados; botones siguen deshabilitados y mismo DOM/estilos |
| ARIA no soportado en MarketPlayerSummary | Estadísticas declaran grupo con nombre en vez de div genérico nombrado | Agrupación informativa, sin acciones ni cifras nuevas; sin alterar layout |
| noStaticElementInteractions en Modal | Overlay div sin handlers; botón nativo de fondo absoluto, transparente, fuera de Tab; teclado en el elemento `role="dialog"` | Botón de fondo deshabilitado con busy o closeOnOverlayClick=false; diálogo encima del backdrop. Click dentro no alcanza el botón de fondo. Escape sigue condicionado por closeOnEsc/busy y Tab mantiene el trap dentro del diálogo |
| useOptionalChain | `returnTarget?.isConnected` en restitución de foco | Se conserva comprobación de focus y preventScroll; no cambia referencias ni ciclo de vida |
| noUselessSwitchCase | Quitar alias case INTERNAL_ERROR delante del default | INTERNAL_ERROR sigue recibiendo exactamente el mensaje genérico del default, al igual que desconocidos |
| noUselessFragments | Quitar Fragment de un único grid en MarketBuyPage | Fragment no generaba nodos; se conserva grid, contenido, callbacks y scroll |

Archivos de corrección: `MarketBuyPage.tsx`, `BuyTokenModal.tsx`, `MarketPlayerSummary.tsx`, `hooks/pages/useMarketBuyPage.ts`, `shared/components/Modal/Modal.tsx` y su SCSS. Cambios de CSS limitados al backdrop absoluto y z-index del diálogo; fondo, dimensiones, scroll y colores existentes conservados por implementación. La comprobación en navegador/teclado sigue pendiente, no se afirma ejecución observada.

## Auditoría de los 23 archivos de biome:fix

Se revisó el diff completo, incluido el diff ignorando espacios. No se detectaron cambios ajenos a SPEC-019 ni nuevas llamadas, endpoints, dependencias, reglas o lógica de players/portfolio. No se revirtieron cambios del usuario.

| Grupo | Archivos revisados (rutas relativas a frontend/src) | Cambios observados |
| --- | --- | --- |
| app (1) | `app/layouts/AppLayout/AppLayout.tsx` | Saltos de línea/indentación del modificador de scroll de mercado |
| market (15) | `features/market/form/schemas/index.ts`; `hooks/index.ts`; `hooks/pages/useMarketBuyPage.ts`; `hooks/queries/useMarketQuotesQuery.ts`; `market.mapper.ts`; `market.service.ts`; `pages/MarketBuyPage/BuyTokenModal/BuyTokenModal.tsx`; `pages/MarketBuyPage/MarketBuyPage.tsx`; `pages/MarketBuyPage/MarketFilters/MarketFilters.tsx`; `pages/MarketBuyPage/MarketHeader/MarketHeader.tsx`; `pages/MarketBuyPage/MarketPagination/MarketPagination.tsx`; `pages/MarketBuyPage/MarketPlayerCard/MarketPlayerCard.tsx`; `pages/MarketBuyPage/MarketPlayerSummary/MarketPlayerSummary.tsx`; `pages/MarketBuyPage/MarketQuoteFeedback/MarketQuoteFeedback.tsx`; `types/index.ts` | Formateo de TSX/expresiones y organización de imports/exports, sin cambiar condiciones ni contratos |
| APIs públicas (2) | `features/players/index.ts`; `features/portfolio/index.ts` | Quitar línea vacía final, sin cambiar exports ni lógica |
| shared (5) | `shared/components/Modal/Modal.tsx`; `shared/components/Modal/index.ts`; `shared/components/Pagination/Pagination.tsx`; `shared/components/index.ts`; `shared/icons/index.ts` | Formateo, paréntesis redundantes, orden de imports/exports y línea vacía final |

El cambio preexistente de `specs/assignments.md` y la carpeta documental sin seguimiento no forman parte de los 23 archivos frontend reformateados; se conservan intactos salvo tasks y este reporte de la remediación. Otros cambios staged de la implementación anterior no se atribuyen a biome:fix.

## Verificaciones y pendientes

- `npm run build` desde frontend: satisfactorio, TypeScript + Vite, 2483 módulos. Advertencia de chunk JS ~649 kB ya presente anteriormente. Sin tests.
- `git diff --check` y `git diff HEAD --check`: satisfactorios, sin salida. `git diff --cached --check`: conserva tres avisos de líneas vacías finales en los barrels players/portfolio/shared porque el índice todavía tiene la versión anterior; el usuario/formatter ya las corrigió en working tree. No se modifica el índice para ocultar esa diferencia.
- Revisión estática de la semántica de grupos/botón/diálogo, condiciones de cierre, selector de elementos focusables, ref inicial/restauración y comparación de contratos. No hay imports directos de otra feature, nuevas queries, compras o cambios de backend en esta remediación.
- No se ejecuta Biome por el agente conforme a AGENTS/Constitución. La salida aportada corresponde a **antes** de estas correcciones, no es un PASS posterior. Comando de verificación para el usuario: `npm run biome:check` desde frontend, sin `--unsafe`.
- Verificar visualmente el nuevo backdrop y con teclado: apertura/foco inicial, Tab/Shift+Tab, Escape, clic fuera/dentro, retorno al disparador y bloqueo de cierre durante busy. Sin confirmar compras reales. No se declara E2E validada ni cierre de SPEC-019; T-17/T-18 y cotizaciones reales siguen pendientes.
- Revisar y actualizar el stage de forma humana antes de publicar: el índice conserva la versión anterior al formatter y a esta remediación. No se desactivaron hooks ni se alteró Git.

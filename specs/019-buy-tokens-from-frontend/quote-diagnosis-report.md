# Correcciones visuales y diagnóstico de Confirmar compra

**Preparación de cierre posterior:** [implementation-report.md](implementation-report.md) consolida tareas/evidencia sin declarar compra E2E; [future-work.md](future-work.md) registra integración requerida y capacidades fuera de alcance. Este diagnóstico conserva sus resultados históricos y no acredita un GET current autenticado ni cierre funcional.

## Alcance autorizado y preparación

Solicitud humana vigente: corregir iconos de cuatro pestañas de mercado según ref. 04, margen inferior de Aplicar, scrollbar oscura del listado e investigar/corregir bloqueo frontend de compra; mantener arquitectura, scroll, lógica de players/portfolio/backend y contratos HTTP. Sin compras reales, tests, commits, push ni merges. El usuario autorizó continuar **sin respuesta autenticada de Network**, por lo que no se deduce una causa concreta de datos backend.

Prerrequisitos del comando ANALYZE leídos/ejecutados mediante `bash .specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`, resueltos a esta SPEC. Revisión de consistencia de spec/plan/tasks/contratos/modelo y normas aplicables: Q-01 cubre UI, Q-02 lectura validada, Q-03 guard/feedback, Q-04 verificación. Sin requisitos nuevos sin tarea ni cambios de contrato; sin bloqueantes para este alcance. Checklists históricos intactos, sin aprobar gates humanos. No hay hooks de extensión configurados. La persistencia de este informe está fuera de la operación read-only de análisis.

## Hallazgos confirmados por lectura

1. `market.service.ts` devolvía null para cualquier 404, ocultando fallos distintos de QUOTE_NOT_FOUND. Ahora solo 404 + código documentado representa ausencia; 401/403/otros HTTP/red se distinguen mediante error Model seguro.
2. El Mapper copiaba datos tipados estáticamente, sin validar respuesta real. Ahora valida unknown, precio entero positivo seguro y fechas requeridas válidas. No convierte precios string ni sustituye timestamps.
3. El Query Hook exponía `data` cacheada incluso con error/refetch y el Page Hook solo verificaba existencia/precio, cantidad y busy/éxito. Ahora solo disponible exitoso y resuelto entrega precio utilizable; carga/pausa/error bloquean tanto UI como handler. Se consulta la sesión pública sin acceso directo al almacenamiento; 401 sigue bajo autoridad del interceptor global.
4. La UI no explicaba todas las causas del disabled. Ahora presenta la razón accesible y feedback compartido por props; ausencia, autenticación, permisos, HTTP, red y respuesta inválida son diferentes. Reintentar invoca solo GET, comparte key/id y evita cancelación/reinicio por clics repetidos.

La cantidad inicial `"1"` es válida según Form Hook/schema existentes. No se la cambió para habilitar compras artificialmente. No hay evidencia de que ese campo causara el bloqueo observado. La condición correcta sigue siendo obtener cotización real válida; no se fuerza el botón.

## Cambios y orden de revisión

1. `app/navigation/sections.ts` y `shared/icons/index.ts`: Comprar/ShoppingCart, Vender/Tag, Mis órdenes/Puzzle, Historial/History. Tres destinos futuros siguen null/deshabilitados. Sin rutas ni capacidades nuevas.
2. `MarketFilters.module.scss`: padding inferior de panel desktop/compacto y popover móvil; no cambia controles ni lógica.
3. `MarketBuyPage.module.scss`: scrollbar fina con track/thumb oscuros, hover y compatibilidad estándar/WebKit, exclusivamente en main. No modifica contención de AppLayout ni scroll ajeno.
4. `market.mapper.ts`, `market.service.ts`, `types/models.ts`, `types/index.ts`: validación y errores seguros de lectura; POST buy permanece intacto.
5. `hooks/queries/useMarketQuotesQuery.ts`: estados, autenticación habilitante, política de reintentos y refetch por id. Precio cacheado no utilizable durante fallo/pausa/refetch. Sin importar HTTP/DTO en Query Hook.
6. `hooks/pages/useMarketBuyPage.ts`: mismo guard de disponibilidad/autenticación en Confirmar y handler, explicación del bloqueo; conserva guarda síncrona de doble envío, formulario, éxito e invalidaciones.
7. `utils/quoteStatus.ts`/barrel y `MarketQuoteFeedback/`: mensajes seguros por estado y reintento por props, sin Hooks en Component local.
8. `MarketBuyPage.tsx`, `MarketPlayerCard.tsx` y SCSS, `BuyTokenModal.tsx` y SCSS: distribuyen estado/acciones y muestran feedback/razón accesible; se eliminan estilos de mensajes sustituidos. Diseño y flujo de compra existentes conservados.
9. Artefactos de SPEC-019: alcance, plan, tareas, contrato de consumo, modelo, quickstart y este informe. No se modifican checklists del revisor ni el backend.

## Evidencia real y limitaciones

- Configuración efectiva observada: VITE_API_URL apunta a `http://localhost:8080/api`.
- Lectura manual de GET `/api/players?page=0&size=1` **sin credenciales**: HTTP 401, cuerpo vacío y WWW-Authenticate Bearer. Confirma protección de API, no ausencia de cotización.
- Browser sin pestañas y apertura de `/market/buy` con ERR_FAILED. No hay GET current autenticado observado ni estado de TanStack Query de esa sesión disponible. El usuario eligió continuar sin aportar esa evidencia. No se afirma haber comprobado respuesta/precio del jugador ni causa backend concreta.
- No se realizaron compras, inicialización, recálculo, consultas de BD ni cambios backend. Ningún precio o saldo se fabricó.

## Si se confirma falta de datos backend

Solo una respuesta autenticada `404 QUOTE_NOT_FOUND` confirma ausencia operativa para ese id. El contrato de SPEC-016 exige cotización persistida y población cotizable basada en métricas; catálogo visible no garantiza cotización para todos sus jugadores. El responsable debe verificar métricas/condición cotizable e inicialización del mercado y generar las cotizaciones reales mediante el flujo autorizado, no mediante un fallback frontend. No se ejecuta `POST /quotes/initialize` ni recálculo como parte de esta corrección. Un 401 requiere resolver sesión, un 403 permisos y otro HTTP/red su causa de integración, no cargar datos por suposición.

## Pendientes humanos

### Verificaciones técnicas

- Build final `npm run build` desde frontend: satisfactorio, TypeScript + Vite, 2483 módulos. Advertencia de chunk JS aproximadamente 649 kB; el build anterior ya advertía por tamaño. No se introdujo dependencia nueva ni se amplió el alcance para code-splitting.
- Revisión de consumidores y nombres de estado mediante búsqueda; ningún consumidor conserva props antiguas `isLoadingQuote/isQuoteError`. Las claves antiguas de tabs sin ruta no tenían otros consumidores. `TabLink` ya soportaba iconos tanto en destinos activos como futuros; no se modificó su lógica.
- `git diff --check` conserva los tres avisos preexistentes de líneas vacías finales en barrels players/portfolio/shared. No se editan ni revierten cambios previos ajenos. Estado final incluye archivos sin seguimiento de la implementación previa de SPEC-019; no se incorporaron a Git ni se eliminaron.
- Q-01–Q-03 implementadas/compiladas; Q-04 permanece pendiente por falta de inspección autenticada/visual. Ningún test ni compra ejecutados y ninguna revisión humana sustituida por el build.

Revisar diff y feedback, aportar opcionalmente URL/id + HTTP + cuerpo saneado del GET current de la sesión afectada, inspeccionar habilitación con 200 válido **sin pulsar Confirmar**, rechazo de cantidad inválida, fallo con datos cacheados y reintento GET. Comparar detalles visuales y scroll en 375/768/1440px cuando el navegador esté disponible. Gates de CODE REVIEW/TESTING/cierre y seguimiento remoto no aprobados ni afirmados.

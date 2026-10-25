# Informe de implementación y preparación de cierre — SPEC-019

**Revisión documental:** 2026-10-25 00:07:20 (`America/Argentina/Buenos_Aires`).
**Rama observada:** `feat/019-buy-tokens-from-frontend`.
**Resultado:** entrega parcial del frontend; **no apta todavía para declarar cierre funcional**.

## Implementación presente

- Ruta protegida `/market/buy`, pestañas con iconos y composición desde app del catálogo paginado de players e invalidación de Portfolio. Las funciones futuras de navegación permanecen deshabilitadas.
- UI de mercado: encabezado, métricas con total real/ausencia explícita, panel de filtros visual pendiente, cards compactas en tres columnas desktop, contador y paginación. Imágenes con fallback local. No se inventan precios, saldo, estadísticas ni variación.
- Scroll contenido en el listado para esta ruta; encabezado/navbar/filtros/footer fuera del scroller, métricas compactas y filtros plegables móviles. Su comportamiento visual real aún no está validado en los tres tamaños.
- Modal genérico con portal, foco/ARIA y overlay sin blur; modal de compra con cantidad editable inicial `"1"`, validación 1–100, estimación, estados/errores y notificaciones. −/+ y cantidades rápidas son visuales/deshabilitadas, según el alcance vigente.
- Cliente GET current y POST buy, DTO/Mapper/Models de market, Query/Mutation/Page Hooks y contratos existentes. Se distingue ausencia de quote de autenticación, permisos, HTTP/red o respuesta inválida; existe reintento GET.
- Guard frontend: requiere sesión autenticada, consulta exitosa resuelta, precio válido, cantidad válida y ausencia de envío/éxito. **Sin cotización válida la compra permanece deshabilitada**, también en el handler. No se usa demo ni un dato cacheado de una lectura fallida/pausada/en curso.

Son capacidades presentes en código y compiladas, **no resultados de una ejecución E2E**. La lógica de players/portfolio/backend no se modificó; los barrels públicos de players/portfolio solo exponen los elementos autorizados. No se modifica código en esta preparación de cierre.

## Revisión T-00–T-19

La fuente de marcadores es [tasks.md](tasks.md); esta tabla explica la evidencia, no crea otro seguimiento.

| Tareas | Evidencia revisada | Límite |
| --- | --- | --- |
| T-00/T-01/T-15 | package/lockfile y tecnologías: React-Toastify 11.1.0; AppProviders con ToastContainer; useMarketToast compone mensajes desde resultado | Toasts no observados tras una compra real |
| T-02/T-08/T-11 | AppRouter/AuthGuard, MarketBuyRoute, barrels públicos de players/portfolio/market | No acredita integración autenticada ni actualización real de Portfolio |
| T-03/T-04 | Modal genérico, portal, referencias de foco, trap, cierre y export compartido | Teclado/lector/restauración de foco pendientes de evidencia humana |
| T-05/T-06/T-07 | DTOs, Models propios y utilidades de importes/instantes reales | Importes de transacción no recibidos en ejecución observada |
| T-09/T-10 | Service/Mapper y query por id real con validación/estados/reintento | Integración de cotizaciones reales pendiente; no se observó GET current autenticado válido |
| T-12 | Form Hook/schema, Mutation/Page Hooks, guarda síncrona, errores y coordinación de invalidaciones | No se verificaron envío, doble click, éxito, fallos ni invalidaciones de extremo a extremo |
| T-13/T-14/T-16 | Page y Components locales por props, ficha/modal, estados de carga/error/vacío | Fidelidad 04/05 y responsive no aceptados visualmente |
| T-17/T-18 | No hay resultados humanos suficientes de escenarios ni revisión visual accesible | Permanecen sin marcar; no se sustituyen por build |
| T-19 | Último build de código satisfactorio, revisión de diff y status; advertencias registradas | No acredita testing ni aprobación; diff-check con avisos previos |

**Balance original: 18/20 tareas técnicas completadas; T-17 y T-18 pendientes.** Los grupos V/S/Q mantienen sus verificaciones pendientes en tasks.md. Marcar implementado T-09/T-10 no significa que la integración real esté terminada.

## Integración de cotizaciones reales: pendiente

Hay cliente implementado para `GET /api/players/{id}/quotes/current`, pero **la integración autenticada con cotizaciones reales está pendiente de validación**. No se dispone de respuesta 200 del jugador afectado ni de evidencia de su estado en TanStack Query. El usuario autorizó continuar sin aportar esa respuesta en el diagnóstico anterior.

Evidencia disponible: la API configurada apunta a `http://localhost:8080/api`; una lectura previa del catálogo **sin credenciales** devolvió 401 con cuerpo vacío. No fue una consulta autenticada de current y no prueba datos faltantes. El navegador disponible devolvió `ERR_FAILED` y no expuso pestañas. No afirmar que el mercado esté sin inicializar, sin métricas o sin precios basándose en esas observaciones.

Para resolverlo se necesita sesión válida y evidencia saneada de GET current (id/HTTP/cuerpo, sin JWT/cookies). Si es 404 QUOTE_NOT_FOUND, el responsable debe verificar población cotizable/métricas y cotización persistida conforme a SPEC-016; si es 401/403, resolver sesión/permisos; otros HTTP/red requieren diagnóstico de integración. No se inicializa/recalcula backend por suposición ni se inventa precio en frontend.

**El flujo de compra no está verificado de extremo a extremo.** No hubo compras reales, POST exitoso observado, prueba de saldo/stock, confirmación de auditoría, doble envío ni actualización efectiva de Portfolio. La validación no transaccional pendiente puede observar habilitación con quote válida sin pulsar Confirmar; cualquier prueba transaccional posterior requiere ejecución humana autorizada aparte.

## Verificaciones y formato

| Control | Evidencia / resultado |
| --- | --- |
| Build frontend | Último `npm run build` tras cambios de código: TypeScript y Vite satisfactorios, 2483 módulos; JS ~649 kB con advertencia de tamaño. Sin tests. Resultado observado en esta sesión y documentado en el [diagnóstico](quote-diagnosis-report.md) |
| Build en esta preparación | No se repite: cambios exclusivamente documentales; Constitución/AGENTS excluyen build para este caso. No se presenta el build previo como ejecución nueva |
| Formato | Revisión manual de Markdown/rutas/enlaces y `git diff --check`. Este último conserva tres líneas vacías finales preexistentes en barrels players/portfolio/shared; no se las revierte ni corrige fuera del alcance |
| Biome/lint | No ejecutados por el agente, conforme a AGENTS; no se afirma formato/lint completo aprobado. Comprobación opcional del usuario: `npm run biome:check` desde frontend |
| Tests | No creados/ejecutados; frontend exento de automatizados. Build no es prueba funcional ni E2E |
| Visual/accesibilidad | Pendientes 375/768/1440px, alturas reducidas/zoom, panel móvil expandido, foco y scroll exclusivo/regresión de rutas; no hay capturas autenticadas |
| Git | Estado local con cambios rastreados y carpetas sin seguimiento previas de SPEC-019; sin stage, commits, push, merge ni limpieza de trabajo ajeno |

## Pendientes y clasificación

[future-work.md](future-work.md) distingue integración/verificaciones requeridas para cerrar de capacidades excluidas del rediseño visual. No se crea deuda técnica especulativa sobre backend: falta evidencia del GET actual, no un defecto demostrado. Los requisitos vigentes no se rebajan por llamarlos trabajo futuro.

## Elegibilidad de cierre y próxima acción

**Preparación de publicación posterior:** se volvió a ejecutar el build frontend para publicar los cambios de código (2483 módulos, satisfactorio, misma advertencia ~649 kB). La publicación quedó bloqueada por hooks activos con controles no permitidos y por CI de PR con tests; no se desactivaron. Ver [entrega para publicación humana](publication-handoff.md) y [cuerpo del PR](pull-request.md). Este evento no altera la evidencia histórica de la preparación exclusivamente documental anterior ni acredita E2E, commit, push, PR o cierre.

- `Status` de spec.md se conserva en Draft; no se promueve por cantidad de tareas o build.
- Revisión humana de código, aceptación de verificación/no aplicabilidad y autorización de cierre no acreditadas. Checklists históricos no se alteran ni equivalen a revisión del resultado actual.
- No se ejecuta CONVERGE ni se declara cierre: integración/validaciones técnicas y visuales requeridas siguen pendientes.
- Seguimiento remoto no consultado ni modificado en este encargo; no se afirma estado/sincronización de GitHub Projects.
- Próximo paso: obtener evidencia autenticada no transaccional y revisión visual/humana, resolver hallazgos con autorización si requieren código y recién entonces evaluar los gates de cierre.

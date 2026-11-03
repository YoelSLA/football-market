## Resumen

Avances de **SPEC-019 — Comprar tokens desde el frontend**.

Refs #42. Este PR **no cierra #42 ni declara finalizada SPEC-019**.

- Ruta protegida `/market/buy`, catálogo paginado y composición entre features desde app.
- Mercado oscuro con fichas compactas, iconos, fallback de imágenes, contador y paginación; scroll exclusivo del listado, filtros plegables móviles y controles visuales pendientes identificados.
- Modal de compra, cantidad editable 1–100, estimación, estados de carga/error y notificaciones.
- Cliente de cotización real y compra, validación del precio/fechas, distinción entre ausencia, autenticación/permisos, HTTP/red y respuesta inválida, y reintento GET.
- Botón y handler bloqueados sin sesión/consulta exitosa/cotización válida; sin precios demo ni capacidades nuevas de backend.
- Contratos públicos y límites de market/app/shared/players/portfolio conservados; players/portfolio solo amplían exports autorizados.

## Verificaciones

- `npm run build` desde frontend: satisfactorio, TypeScript + Vite, 2483 módulos. Advertencia de chunk JS ~649 kB.
- `git diff --check`: tres avisos de línea vacía final ya presentes en los barrels de players, portfolio y shared/components. No se presenta como limpio.
- Tests, Biome/Spotless y compras reales **no ejecutados por el agente**. Los hooks y CI aplicables quedan a cargo de la publicación humana; su existencia no acredita que hayan pasado.
- La inspección visual autenticada no se completó por `ERR_FAILED` del navegador.

## Limitaciones y pendientes

**La compra E2E no está validada porque falta completar la integración/validación autenticada de cotizaciones reales.** El cliente está implementado, pero no se observó GET current autenticado válido del jugador afectado ni compra exitosa. No se atribuye una causa backend sin evidencia.

- Sin cotización válida, Confirmar compra y el handler permanecen bloqueados.
- T-00–T-19: **18/20 completadas técnicamente; T-17 y T-18 pendientes**. Las casillas técnicas no acreditan ejecución E2E.
- Pendientes lectura real autenticada, escenarios funcionales, foco/accesibilidad y comparación 04/05 en 375/768/1440px, incluido scroll y regresión de otras rutas.
- Filtros, favoritos, −/+, cantidades rápidas y selector de tamaño siguen visuales/deshabilitados. Venta, órdenes e historial no se habilitan.
- Saldo/órdenes/estadísticas/variación sin fuente real muestran ausencia explícita, no cifras inventadas.

## Documentación

- `specs/019-buy-tokens-from-frontend/tasks.md`
- `specs/019-buy-tokens-from-frontend/implementation-report.md`
- `specs/019-buy-tokens-from-frontend/future-work.md`
- `specs/019-buy-tokens-from-frontend/quote-diagnosis-report.md`

Revisión humana, verificación aplicable y cierre permanecen pendientes. No hacer merge como parte de esta publicación de avances.

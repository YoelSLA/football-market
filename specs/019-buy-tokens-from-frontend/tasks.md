# Tasks: Comprar tokens desde el frontend (SPEC-019)

**Branch**: `feat/019-buy-tokens-from-frontend` | **Fecha**: 2026-10-23 | **Status**: Pending

Revisión de entrega del 2026-10-25: se preservan IDs y evidencias históricas. `[X]` en tareas técnicas significa código implementado y revisado estáticamente, con build observado; **no acredita integración real, compra E2E, aceptación visual ni cierre**. `app` compone features; `market` no importa otras features; players/portfolio solo ampliaron sus exports autorizados. El usuario ejecuta validaciones manuales; el agente no ejecuta tests, lint ni Biome. El build frontend solo se ejecuta tras cambios de código, no por esta actualización documental.

**T-00–T-19: 18/20 completadas técnicamente (T-00–T-16 y T-19); T-17/T-18 pendientes.** La integración autenticada de cotizaciones reales está pendiente de validación; sin una cotización válida y una consulta exitosa resuelta, la compra permanece deshabilitada. El flujo no está verificado de extremo a extremo. Evidencia y límites: [informe de implementación](implementation-report.md). Trabajo requerido/futuro, sin duplicar marcadores de tareas: [registro](future-work.md).

## Fase 0 — preparación y composición pública

- [X] **T-00** Instalar React-Toastify en `frontend/package.json`/lockfile y registrar versión/propósito efectivo en `docs/frontend/technologies.md`. Sin tests. Dependencias: ninguna.
- [X] **T-01** Configurar `ToastContainer` y CSS en `frontend/src/app/providers/AppProviders/AppProviders.tsx`, preservando los providers actuales. Dependencias: T-00.
- [X] **T-02** Crear `frontend/src/app/market/MarketBuyRoute/` (módulo con barrel) para componer `usePlayersQuery(page)` y `PLAYERS_PAGE_SIZE`, convertir `Player` a `MarketPlayer` sin filtrar por stock, manejar página/listado/error y coordinar invalidación de Portfolio; habilitar `/market/buy` bajo `AuthGuard` + `AppLayout` en `frontend/src/app/router/AppRouter/AppRouter.tsx` y `market-buy` en `frontend/src/app/navigation/sections.ts`. Dependencias: T-08, T-11, T-16; integrar al final, sin ruta vacía permanente.

## Fase 1 — UI genérica y contratos de composición

- [X] **T-03** Crear `frontend/src/shared/components/Modal/` con `Modal.tsx`, módulo SCSS y barrel: portal en body, overlay, ref **genérica** de foco inicial (sin conocer cantidad ni market), trap Tab/Shift+Tab, ARIA, ESC/overlay configurables, bloqueo de cierre durante submit, restitución de foco al botón iniciador al desmontar, responsive sin cambiar layout/scroll del mercado. Sin lógica de negocio. Dependencias: ninguna.
- [X] **T-04** Exponer Modal desde `frontend/src/shared/components/index.ts`. Dependencias: T-03.
- [X] **T-05** Crear DTOs de lectura de cotización real y compra en `frontend/src/features/market/types/dtos.ts` según [contrato](contracts/buy-tokens-frontend.md): campos del GET `price/effectiveDate/calculatedAt`, body POST sin listingId, respuesta ejecutada. DTO solo para Service/Mapper. Dependencias: ninguna.
- [X] **T-06** Definir Models propios de market en `frontend/src/features/market/types/models.ts`: `MarketPlayer`, `MarketQuote`, `BuyResult` (sin `playerName`), `BuyEstimation`, `BuyQuantityForm` (campo textual inicial `"1"`); `types/index.ts` expone selectivamente para market. Sin imports de `players` ni Portfolio. Dependencias: T-05.
- [X] **T-07** Añadir utilidades puras de formato en `frontend/src/features/market/utils/` y barrel: créditos/centésimas, estimación y `calculatedAt` real con locale español. Sin fabricar fechas ni usar `date` demo. Dependencias: T-06.
- [X] **T-08** Ampliar **solo** `frontend/src/features/players/index.ts` para exponer `usePlayersQuery`, `PLAYERS_PAGE_SIZE`, `type Player` y `frontend/src/features/portfolio/index.ts` para exponer `portfolioQueryKeys`; reexportar mediante barrels internos existentes, sin modificar ninguna otra parte de esas features. La compra no usa ni exporta `usePlayerTokenQuoteQuery` demo. Dependencias: ninguna. Esta tarea sustituye la antigua verificación de timestamp demo.

## Fase 2 — acceso real y lógica de compra market

- [X] **T-09** Crear `frontend/src/features/market/market.service.ts` con GET current autenticado (signal del Query Hook) y POST buy autenticado; crear `market.mapper.ts` para DTO→Model (y Form Model→DTO) dentro de la frontera del Service. `buyTokens(request)` devuelve `BuyResult` con campos exclusivamente del POST, **sin recibir ni incorporar el nombre**; GET 404 `QUOTE_NOT_FOUND` se trata como indisponible sin simular precio. Dependencias: T-05, T-06. Cliente implementado; no acredita GET autenticado real ni POST ejecutado. Integración pendiente en T-17/Q-04.
- [X] **T-10** Crear `frontend/src/features/market/constants/marketQueryKeys.ts` y barrel; `hooks/queries/useMarketQuotesQuery.ts` coordina con `useQueries` las lecturas GET reales por id visible (key individual por id, sin `demo-quotes`) y estados de carga/error/ausencia, sin hooks dentro de `MarketPlayerCard` ni otro GET al abrir modal. Dependencias: T-09. Implementación y estados revisados en código; respuesta autenticada y comportamiento de caché pendientes de validación en T-17/Q-04.
- [X] **T-11** Exportar desde `frontend/src/features/market/index.ts` solo `MarketBuyPage` y `type MarketPlayer` para app; sin publicar Service, DTO, Mapper o claves internas. Dependencias: T-06, T-16.
- [X] **T-12** Crear `form/schemas/` y `form/hooks/` de market con valor inicial `"1"`: aceptar texto numérico de dígitos cuyo valor sea entero 1–100; exponer el valor validado para convertirlo a number **sin invocar** el Mutation Hook; rechazar vacío, no numérico, decimales y fuera de rango con error inline. Crear `hooks/mutations/useBuyTokensMutation.ts` para POST y `hooks/pages/useMarketBuyPage.ts` que compone Form Hook, Query Hook T-10 y Mutation Hook: coordina apertura, estimación, errores (diez códigos FR-10, fallback seguro, 401 por sesión), toast con `selectedPlayer.name` + `BuyResult` del Service, guarda doble envío, invalidación propia y callback `onPurchaseSuccess(id)` a app. El Page Hook no conoce Portfolio ni DTO. Dependencias: T-01, T-06, T-07, T-09, T-10, T-15.

## Fase 3 — UI market y app

- [X] **T-13** Crear `pages/MarketBuyPage/MarketPlayerCard/` (TSX, módulo SCSS, barrel), ficha local con `MarketPlayer`, `MarketQuote | null`, estado de carga/error y callbacks **solo por props** de la Page; muestra precio real + `calculatedAt` y botón Comprar con referencia para restauración de foco. Sin Hooks ni imports de players; ref visual 04. Dependencias: T-06, T-07.
- [X] **T-14** Crear `pages/MarketBuyPage/BuyTokenModal/` (TSX, módulo SCSS, barrel), Component local sin Hooks: compone Modal genérico, Field/Button y recibe por props del Page Hook estado de formulario, valor validado, estimación, quote de la Page, handlers, busy y cierre tras éxito (2 s o por usuario). Cantidad inicial 1, error inline, foco inicial desde ref genérica proporcionada por Page; ref visual 05. Sin GET demo ni HTTP directo. Dependencias: T-03, T-04, T-06, T-07, T-13.
- [X] **T-15** Usar React-Toastify (`toast.success/error`) desde hook de coordinación de market o encapsularlo en un hook de market si aporta responsabilidad; no crear `shared/useToast` por duplicación o anticipación. Dependencias: T-00, T-01. Debe resolverse antes de T-12.
- [X] **T-16** Crear `pages/MarketBuyPage/MarketBuyPage.tsx`, módulo SCSS y barrel: conecta `useMarketBuyPage` (T-12), recibe listado paginado por props de app y entrega a `MarketPlayerCard`/`BuyTokenModal` locales sus cotizaciones y acciones exclusivamente por props. Estados de carga/error/vacío; abrir/cerrar sin navegar ni remontar listado conserva scroll, página, filtros existentes y pestaña activa. No crear filtros nuevos ni simular stock. Prop `onPurchaseSuccess` hacia app sin conocer Portfolio. Dependencias: T-12, T-13, T-14. Después completar T-11 y T-02.

## Fase 4 — verificaciones y entrega técnica

- [ ] **T-17** Usuario: escenarios manuales [quickstart.md](quickstart.md) US1–US4 y errores, incluidos listado sin stock, cantidad `"1"`/`"10"` enviada como number, mapeo de diez códigos FR-10 + fallback + 401 + `QUOTE_NOT_FOUND`, respuesta real, doble envío y cache. No marcar hasta recibir evidencia del usuario. Dependencias: T-02.
- [ ] **T-18** Usuario: foco, ARIA, lector de pantalla, 375/768/1440px y referencias visuales 04/05 sin reflow. No marcar hasta recibir evidencia. Dependencias: T-02.
- [X] **T-19** Agente: solo `npm run build` desde `frontend` tras modificar código, revisar diff, `git diff --check` y status; `biome:check` queda a cargo del usuario porque no está autorizado como control ejecutable del agente. Último build de código observado: satisfactorio (2483 módulos, advertencia de chunk ~649 kB). Diff-check conserva tres avisos preexistentes; no se presenta como limpio. Sin nuevo build por cambios documentales. Dependencias: T-02; no requiere marcar T-17/T-18 como completadas para ejecutar el build, pero su aprobación humana sigue pendiente.

## Rediseño visual vigente (solicitud posterior)

Este alcance sustituye la propuesta funcional de filtros/catálogo completo antes de implementarla. No modificar lógica de players, portfolio, backend ni compra. Conservar T-00–T-19 como evidencia histórica.

- [X] **V-01** VR-01/VR-02: recomponer MarketBuyPage con encabezado/métricas/panel lateral locales por props, fondo oscuro, tres columnas compactas y footer con total real/paginación existente; controles visuales de filtros y tamaño deshabilitados y pendientes, sin lógica ni endpoints nuevos. Implementado y compilado; aceptación visual pendiente en V-04.
- [X] **V-02** VR-01/VR-04: rehacer MarketPlayerCard con retrato compacto, badge, iconos, celdas deportivas y variación «Sin datos», botón verde y fallback local. No inventar precio, escudos, banderas ni estadísticas. Implementado y compilado; aceptación visual pendiente en V-04.
- [X] **V-03** VR-03/VR-04: recomponer BuyTokenModal según ref. 05 conservando contratos y lógica de compra; liga y resumen existentes, controles rápidos visuales pendientes, overlay sin blur, fallback local, foco y scroll propio sin reflow. Implementado y compilado; comportamiento visual/foco pendiente de inspección en V-04.
- [ ] **V-04** VR-04: build permitido tras cambios, diff/status y comparación visual autenticada en 375/768/1440px sin compras ni tests; informar limitaciones y no declarar fidelidad terminada si no hay evidencia. Dependencias: V-01–V-03.

Orden: V-01/V-02 → V-03 → V-04. Preparación: ANALYZE de este alcance antes de código. No se aprueban checklists ni revisión visual en nombre del usuario.

## Dependencias críticas y alcance

### Remediación manual de Biome autorizada

- [X] **B-01** Revisar los 23 archivos frontend reformateados por el usuario y corregir manualmente los seis diagnósticos aportados: grupos ARIA con nombre, backdrop nativo sin handlers en contenedor estático, optional chaining, case redundante y fragmento de un hijo. Sin `--unsafe`, cambios de reglas, contratos ni lógica financiera. Implementado, revisión estática y build satisfactorios; reporte en [biome-remediation-report.md](biome-remediation-report.md).
- [ ] **B-02** Usuario: volver a ejecutar `npm run biome:check` y comprobar overlay/Escape/Tab/foco sin compras reales. No se infiere PASS de Biome ni comportamiento visual a partir del build. T-17/T-18 e integración de cotizaciones permanecen pendientes.

### Correcciones de cotización y detalles visuales autorizadas

- [X] **Q-01** Pestañas/iconos Lucide según referencia 04 sin habilitar funciones futuras, margen inferior de filtros y scrollbar oscura local, preservando scroll exclusivo. Implementado y compilado; cotejo visual en Q-04 pendiente.
- [X] **Q-02** Service/Mapper de market validan quote real y distinguen ausencia 404 QUOTE_NOT_FOUND de auth/permisos/HTTP/red/respuesta inválida. Query Hook expone estado carga/pausa/error y reintento GET por id; sin retry automático de fallos no transitorios ni contratos/endpoints nuevos. Implementado, revisado estáticamente y compilado; sin evidencia autenticada ni pruebas ejecutadas.
- [X] **Q-03** Page Hook y handler requieren lectura exitosa resuelta, sesión presente y precio válido; evitar confirmación por caché stale tras fallo. Feedback local por props en ficha/modal, motivo accesible del botón bloqueado y reintento sin POST. Dependencias: Q-02. Implementado y compilado; comportamiento en sesión real pendiente en Q-04.
- [ ] **Q-04** Build permitido y revisión de diff; inspección real de GET autenticado/estado UI si el entorno lo permite, sin compras/tests/commits/push/merges. Si falta evidencia, reportar limitación y no atribuir causa backend no comprobada. Dependencias: Q-01–Q-03.

### Ajuste de scroll autorizado (posterior al rediseño)

- [X] **S-01** Contener AppLayout/body por opt-in exclusivo de `/market/buy`, con altura dinámica de viewport y flex; no alterar otras rutas. Implementado y compilado; regresión visual entre rutas pendiente en S-03.
- [X] **S-02** MarketBuyPage con encabezado/filtros/footer estáticos y min-height:0 hasta main; scroll únicamente en jugadores; métricas compactas y filtros plegables móviles autorizados. Implementado y compilado; comprobación visual de alturas y desplegable pendiente en S-03.
- [ ] **S-03** Build sin tests, revisión de diff y verificación visual 375/768/1440, sin compras; registrar limitaciones y no acreditar inspecciones no realizadas. Dependencias: S-01/S-02.

```text
T-00 → T-01 → T-15 → T-12
T-03 → T-04 → T-14
T-05 → T-06 → T-07 → T-13/T-14
T-05/T-06 → T-09 → T-10 → T-12
T-03/T-04 + T-13 → T-14
T-12 + T-13 + T-14 → T-16 → T-11
T-08 + T-11 → T-02
T-02 → T-17/T-18 (usuario), T-19 (agente: build permitido)
```

La dependencia T-16 → T-11 → T-02 corresponde a la primera implementación. La propuesta posterior de filtros funcionales/catálogo completo fue sustituida por el rediseño visual: los controles nuevos permanecen deshabilitados y pendientes, sin lógica de filtro/stock. Los límites de players/portfolio/backend se mantienen. No crear tests frontend. Esta preparación documental no implementa nuevas capacidades, no renueva autorizaciones y no acredita CONVERGE ni cierre; primero deben resolverse las verificaciones e integración pendientes.

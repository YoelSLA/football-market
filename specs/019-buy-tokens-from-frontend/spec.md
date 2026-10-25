# Feature Specification: Comprar tokens desde el frontend

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/019-buy-tokens-from-frontend` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `019-buy-tokens-from-frontend`, sin el prefijo de tipo. La convención de ramas es normativa; ver la constitución §7. El `<scope>` va en inglés, con independencia del idioma de la descripción de origen.

**Created**: 2026-10-23

**Status**: Draft

**Entrega técnica parcial (2026-10-25):** frontend implementado y compilado; **integración de cotizaciones reales pendiente de validación autenticada**. Sin una cotización válida, Confirmar compra y el handler permanecen bloqueados. **Compra no verificada de extremo a extremo.** Preparación de cierre en [implementation-report.md](implementation-report.md), con trabajo requerido/futuro en [future-work.md](future-work.md). No equivale a cierre, revisión humana ni promoción de Status.

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: Issue #42 — REQ-S2-14-A | Comprar tokens desde el frontend

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Iniciar compra desde el mercado (Priority: P1)

Un usuario autenticado navega a la pestaña **Comprar** del **Mercado**, visualiza los jugadores del listado, selecciona uno y pulsa "Comprar" para abrir el modal de compra con la información del jugador y su cotización. La disponibilidad de stock se verifica al comprar, no determina qué jugadores aparecen en la lista.

**Why this priority**: Punto de entrada principal del flujo de compra; sin esto no hay feature. La navegación ya define la sección "Mercado > Comprar tokens" como punto de acceso.

**Independent Test**: Navegar a `/market/buy`, localizar un jugador, pulsar "Comprar" y verificar que el modal se abre con datos correctos (jugador, cotización persistida y cantidad inicial 1).

**Acceptance Scenarios**:

1. **Given** usuario autenticado en `/market/buy`, **When** pulsa "Comprar" en una ficha de jugador, **Then** se abre modal con nombre, equipo, liga, posición y cotización vigente obtenida del backend.
2. **Given** modal abierto, **When** el usuario pulsa "Cancelar" o clica fuera (overlay) o ESC, **Then** el modal se cierra y el mercado sigue visible sin cambios de layout ni pérdida de filtros o estado de navegación.
3. **Given** usuario no autenticado, **When** intenta acceder a `/market/buy`, **Then** se redirige a login (comportamiento existente de rutas protegidas).

---

### User Story 2 — Definir cantidad y revisar resumen (Priority: P1)

El modal inicia con 1 token. El usuario puede modificar la cantidad (1–100) y ve el importe estimado principalmente en créditos, con precisión de centésimas, antes de confirmar.

**Why this priority**: Decisión informada antes de comprometer créditos; evita compras involuntarias.

**Independent Test**: En modal abierto, ingresar cantidades válidas/inválidas y verificar cálculo y validación en tiempo real.

**Acceptance Scenarios**:

1. **Given** modal abierto con cotización (p.ej., 125 centésimas = 1,25 créditos), **When** ingresa `10`, **Then** muestra "Importe estimado: 12,50 créditos (1.250 centésimas)".
2. **Given** modal recién abierto con cotización disponible, **When** todavía no editó la cantidad, **Then** muestra `1` y la estimación de un token; "Confirmar" está habilitado si no hay una solicitud en curso.
3. **Given** modal abierto, **When** borra la cantidad o ingresa `0`, `101`, `-5`, `3.5`, **Then** muestra error inline "Cantidad entre 1 y 100" y deshabilita "Confirmar".
4. **Given** el usuario ingresa el texto `"10"` en el campo, **When** confirma, **Then** el frontend valida que representa un entero positivo entre 1 y 100 y envía `quantity: 10` como número, no como string.
5. **Given** modal abierto sin cotización disponible, **When** intenta comprar, **Then** muestra "Cotización no disponible" y deshabilita "Confirmar".

---

### User Story 3 — Confirmar compra y ver resultado (Priority: P1)

El usuario confirma la compra; el frontend invoca `POST /api/orders/buy` y muestra éxito o error comprensible mediante notificación visual no intrusiva.

**Why this priority**: Núcleo transaccional; debe comunicar resultado sin revelar información sensible.

**Independent Test**: Con cantidad válida y saldo/stock suficientes, confirmar y verificar notificación de éxito con detalles; con saldo insuficiente, verificar mensaje de error.

**Acceptance Scenarios**:

1. **Given** cantidad válida, saldo y stock suficientes, **When** pulsa "Confirmar", **Then** muestra estado "Procesando…", invoca API, en éxito muestra notificación "Compra confirmada: 10 tokens de [Jugador] por 12,50 créditos" y actualiza UI afectada.
2. **Given** saldo insuficiente, **When** confirma, **Then** muestra notificación "Créditos insuficientes para completar la compra" (código `INSUFFICIENT_CREDITS`).
3. **Given** stock insuficiente (superusuario sin tokens), **When** confirma, **Then** muestra notificación "No hay tokens disponibles para este jugador" (código `INSUFFICIENT_STOCK`).
4. **Given** cotización no vigente, **When** confirma, **Then** muestra notificación "Cotización no disponible, intente más tarde" (código `QUOTE_UNAVAILABLE`).
5. **Given** error de red/500, **When** confirma, **Then** muestra notificación "Error del servidor, intente más tarde" sin detalles técnicos.
6. **Given** compra exitosa, **When** modal se cierra, **Then** la página de mercado refleja datos actualizados (p.ej., tenencia en portfolio si navega allí).

---

### User Story 4 — Prevención de doble envío y accesibilidad (Priority: P2)

Durante el procesamiento, el botón "Confirmar" se deshabilita y muestra spinner; el modal es accesible (foco, ARIA, ESC, responsive).

**Why this priority**: Evita compras duplicadas involuntarias (RF-08, CA-08) y cumple usabilidad.

**Independent Test**: Click rápido múltiple en "Confirmar" → solo una request; navegar modal solo con teclado; probar en móvil (375px) y desktop (1440px).

**Acceptance Scenarios**:

1. **When** request en curso, **Then** botón deshabilitado con `aria-busy="true"`, spinner visible, overlay no clickeable.
2. **When** modal abierto, **Then** foco inicial en primer campo (cantidad), Tab recorre orden lógico, ESC cierra, overlay click cierra.
3. **When** viewport < 640px, **Then** modal ocupa ancho completo con márgenes seguros, sin scroll horizontal.

---

### Edge Cases

- **Cotización nula/0**: Backend rechaza con `INVALID_QUOTE_PRICE` o `QUOTE_UNAVAILABLE`; frontend muestra mensaje genérico.
- **Superusuario intenta comprar**: Backend devuelve 403 `SUPERUSER_CANNOT_BUY`; frontend muestra "Los administradores no pueden comprar tokens".
- **Jugador no visible (PLAYER_NOT_VISIBLE)**: Backend 404; frontend "Jugador no disponible para compra".
- **Cantidad textual**: El campo HTML contiene texto editable; `"10"` representa una cantidad válida y se transforma en el número `10` antes del POST. Texto vacío, no numérico, decimal o fuera de rango se rechaza en cliente. Si se envía un payload string al backend por fuera de la UI, corresponde `INVALID_REQUEST`.
- **Sesión expirada durante modal abierto**: 401 → redirige a login **sin restaurar el modal** (la intención de compra no se persiste).
- **Compra exitosa pero portfolio no actualizado inmediatamente**: Eventual consistency; no bloquea cierre de modal; notificación indica "Información actualizándose".

---

## Requirements *(mandatory)*

### Functional Requirements

| ID | Requisito | Trazabilidad Issue |
|----|-----------|-------------------|
| FR-01 | El sistema DEBE mostrar la pestaña **"Comprar tokens"** en la sección **Mercado** de la navegación principal, accesible en `/market/buy` para usuarios autenticados. | RF-01, CA-01 |
| FR-02 | La página `/market/buy` DEBE listar los jugadores visibles del catálogo (con su paginación) en fichas **`MarketPlayerCard`** propias de `features/market` y locales a la página, con botón "Comprar". No se filtra el listado por stock: el backend verifica la disponibilidad al confirmar y puede devolver `INSUFFICIENT_STOCK`. | RF-01, CA-01 |
| FR-03 | Al pulsar "Comprar" en `/market/buy`, el sistema DEBE abrir un modal superpuesto con overlay oscuro semitransparente **sin blur**, manteniendo el mercado visible sin cambiar layout, URL, scroll, página, filtros ni pestaña activa. Cerrarlo no reinicia ese estado. Los filtros nuevos se definen en FR-16–FR-18. | RF-02, CA-02, Ref visual 04/05 |
| FR-04 | El modal DEBE presentar: nombre del jugador, equipo, liga, posición, **cotización vigente persistida** (`GET /api/players/{id}/quotes/current`), precio unitario en créditos con 2 decimales e instante real `calculatedAt` de esa cotización, campo numérico "Cantidad (1–100)" **inicializado en 1** y resumen "Importe estimado: X,XX créditos (Y centésimas)". Si no hay cotización, no debe mostrar una cotización simulada. | RF-02, RF-03, RF-04, CA-02, CA-03, Ref visual 05 |
| FR-05 | El campo cantidad DEBE aceptar texto que represente un entero positivo entre 1 y 100, convertirlo a `number` antes de enviar, y rechazar vacío, no numérico, decimal y fuera de rango; mostrar error inline y deshabilitar "Confirmar" si es inválido. El payload nunca contiene un string para `quantity`. | RF-03, CA-03 |
| FR-06 | El botón "Confirmar" DEBE estar deshabilitado mientras: cantidad inválida, cotización no disponible, request en curso. | RF-04, RF-05, CA-04, CA-08 |
| FR-07 | El botón "Cancelar" y clic en overlay/ESC DEBEN cerrar el modal sin invocar API ni generar compra. | RF-05, CA-05 |
| FR-08 | Al confirmar, el frontend DEBE invocar `POST /api/orders/buy` con `{ playerId, quantity }` (compra directa al superusuario, sin `listingId`). | RF-01, RF-06, backend contract |
| FR-09 | En respuesta 200, el sistema DEBE mostrar notificación visual de éxito con: transactionId, jugador, cantidad, precio unitario e importe total realmente ejecutados y `executedAt` de la respuesta; y cerrar modal tras 2s o acción usuario. `executedAt` no sustituye el `calculatedAt` de la cotización. | RF-06, CA-06, OrderBuyResponseDTO |
| FR-10 | En respuesta 400 `INVALID_REQUEST` → notificación "La solicitud no es válida. Verificá los datos e intentá nuevamente."; 400 `INVALID_QUANTITY` → "Cantidad inválida"; 409 `INSUFFICIENT_CREDITS` → "Créditos insuficientes para completar la compra"; `INSUFFICIENT_STOCK` → "No hay tokens disponibles para este jugador"; `QUOTE_UNAVAILABLE`/`INVALID_QUOTE_PRICE` → "Cotización no disponible, intente más tarde"; 403 `SUPERUSER_CANNOT_BUY` → "Los administradores no pueden comprar tokens"; 404 → "Jugador no disponible"; 500/red → "Error del servidor, intente más tarde". | RF-06, CA-07, ErrorResponseDTO |
| FR-11 | Tras compra exitosa, el sistema DEBE invalidar la cotización real del jugador comprado (key propia de `market`), `portfolioQueryKeys.summary` y todas las keys de `portfolioQueryKeys.holdings(status, page, pageSize)`; la coordinación de Portfolio pertenece a `app`. No se invalida la key de cotizaciones demo de `players`. | RF-07, CA-06, CA-07 |
| FR-12 | Durante request, el modal DEBE mostrar estado de carga (spinner, `aria-busy`, botones deshabilitados) y prevenir doble envío. | RF-08, CA-08 |
| FR-13 | El modal DEBE ser accesible: foco inicial en campo cantidad, orden Tab lógico, ARIA roles/labels (`role="dialog" aria-modal="true"`), ESC/overlay para cerrar, **restaurar foco al botón "Comprar" de la ficha al cerrar**, responsive (<640px full-width con `max-w-[calc(100%-2rem)]`). | RF-08, CA-08, CA-09 |
| FR-14 | El sistema DEBE proveer notificaciones visuales (toast/alert) para éxito y errores; la spec no menciona librería concreta (implementación futura: React-Toastify). | RF-06, CA-07 |
| FR-15 | El sistema NO DEBE implementar compra de publicaciones de otros usuarios (`listingId`); solo compra directa al superusuario. | Fuera de alcance, OrderController docs |
| FR-16 | El panel lateral DEBE combinar búsqueda por nombre de jugador o club, liga, posición y rango de precio real sobre **todo el catálogo disponible**, nunca solo la página visible. Los valores de liga/posición provienen del catálogo. El rango admite límites mínimo/máximo en créditos, inclusivos; vacío significa sin límite, cero es válido y mínimo mayor que máximo muestra error sin aplicar. Cada filtro se limpia individualmente y todos mediante «Limpiar filtros». «Aplicar filtros» confirma los valores editados y vuelve a la primera página. | Ampliación humana de alcance, ref. 04 |
| FR-17 | Filtrar y ordenar DEBE preceder a paginar. El orden por precio admite ascendente/descendente, utiliza exclusivamente cotizaciones reales positivas y coloca jugadores sin cotización al final en ambas direcciones. Sin rango de precio esos jugadores permanecen visibles; con cualquier límite de precio quedan excluidos. Un error de consulta no se presenta como ausencia confirmada: se informa y permite reintentar GET; mientras falten lecturas necesarias no se presentan resultados de precio como completos. Empates se resuelven por nombre e id. | Decisiones humanas de filtros 1–8 |
| FR-18 | La carga completa DEBE reutilizar contratos y caché existentes, limitar concurrencia, permitir cancelación y reintento, evitar descargar el catálogo de nuevo al editar filtros o abrir el modal y distinguir carga parcial de catálogo completo. No inventar endpoints masivos, stock, precios ni timestamps. | Alcance y rendimiento aprobados |
| FR-19 | La presentación DEBE seguir las referencias 04/05 en fondo oscuro, encabezado, panel lateral, tarjetas compactas (tres columnas en escritorio), retrato junto a identidad, badges de posición y resumen del modal. Cantidad editable, controles −/+ y accesos 1/5/10/50/100 comparten validación 1–100. Incluir fallback de imagen local. No representar variación 24 h, estadísticas deportivas ni órdenes pendientes sin fuente real; tampoco inventar saldo. A 375/768/1440px no habrá recortes ni scroll horizontal; la página conserva acceso a su contenido bajo el navbar. | Corrección visual autorizada, ref. 04/05 |

### Key Entities

- **Player (jugador)**: `id`, `name`, `teamName`, `leagueName`, `position`, `imageUrl` — datos de catálogo y ficha de mercado.
- **MarketQuote**: `price` (centésimas), `effectiveDate` (fecha de vigencia), `calculatedAt` (instante real de cálculo ISO) — cotización vigente persistida para cálculo y presentación. No equivale a `TokenQuoteSnapshot` de demostración.
- **BuyOrderRequest**: `playerId: number`, `quantity: number (1–100)` — payload a `POST /api/orders/buy`.
- **OrderBuyResponse / BuyResult**: `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `executedAt` — respuesta de éxito y Model transformado dentro del Service, sin nombre del jugador; la UI compone el nombre desde la ficha seleccionada para la notificación.
- **ApiError**: `code` (`INSUFFICIENT_CREDITS`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `SUPERUSER_CANNOT_BUY`, `INVALID_REQUEST`, `INVALID_QUANTITY`, `INTERNAL_ERROR`), `message` — mapeo a mensajes de usuario.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-01**: Usuario autenticado completa compra en ≤ 4 interacciones (abrir modal → cantidad → confirmar → ver éxito).
- **SC-02**: 0 compras duplicadas por doble click (request idempotente por UI, no por backend).
- **SC-03**: Cobertura documentada **10/10**: cada uno de los diez códigos POST de FR-10/`contracts/buy-tokens-frontend.md` (`INVALID_REQUEST`, `INVALID_QUANTITY`, `INSUFFICIENT_CREDITS`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `SUPERUSER_CANNOT_BUY`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INTERNAL_ERROR`) tiene un mensaje de usuario definido sin detalles técnicos o sensibles. Un código POST desconocido, 500 o error de red recibe fallback genérico; 401 activa el flujo de sesión sin conservar la compra. Para el GET, `QUOTE_NOT_FOUND` muestra ausencia de cotización y no error de jugador. La cobertura documental se mide comparando los diez códigos con la tabla del contrato; el comportamiento observable queda pendiente de los escenarios manuales del quickstart ejecutados por el usuario. No se afirma cubrir códigos futuros desconocidos por su nombre.
- **SC-04**: Modal abre en < 200ms con los datos de ficha y cotización real ya disponibles en la query de `market`; si el GET aún está pendiente, el modal muestra carga o indisponibilidad sin inventar precio.
- **SC-05**: Layout del mercado no se desplaza ni reflow al abrir/cerrar modal (verificado visualmente en 375px, 768px, 1440px).
- **SC-06**: Notificaciones visuales aparecen ≤ 500ms tras respuesta API y no bloquean interacción.

---

## Assumptions

- **Auth**: Usuario ya autenticado (JWT en storage); `/market/buy` protegida por `AuthGuard` y compuesta dentro de `AppLayout` existentes.
- **Cotización**: `GET /api/players/{id}/quotes/current` de SPEC-016 devuelve el precio operativo y `calculatedAt` reales; la serie demo de `players` nunca se usa en la compra. El servidor confirma el precio aplicado en `POST /api/orders/buy`, que puede diferir de la estimación.
- **Backend**: `POST /api/orders/buy` operativo (SPEC-010 cerrado); errores según `ErrorResponseDTO` y códigos documentados en `OrderController`.
- **Superusuario**: Vendedor único en compra directa; no hay marketplace entre usuarios en esta feature.
- **Tokens/credits**: Centésimas de crédito (100 = 1 crédito). **Display principal en créditos con 2 decimales**: "12,50 créditos (1.250 centésimas)". Backend opera en centésimas; frontend convierte para display.
- **Componentes UI**: Reutilizar `Button`, `Field`; crear `Modal` (shared) y `BuyTokenModal` (feature market).
- **Composición**: `app` consulta el listado con la API pública de `players`, lo entrega a `market` mediante datos/props y coordina la invalidación de Portfolio con su API pública; `market` conserva sus queries de cotización real y su operación de compra. No hay store global de compras ni imports entre features.
- **Notificaciones**: Sistema de toast visual requerido; librería no especificada en spec (futura: React-Toastify).
- **Responsive**: Breakpoint móvil ≤ 639px (Tailwind `sm:`); modal full-width con `max-w-[calc(100%-2rem)]`.
- **Estado navegación**: Modal no altera URL ni history; al cerrar, `/market/buy` conserva scroll, paginación, filtros y pestaña activa. El panel nuevo está comprendido en FR-16–FR-19.

---

## Clarificaciones resueltas *(CLARIFY)*

| Tema | Decisión | Impacto en spec |
|------|----------|-----------------|
| **Ubicación botón Comprar** | En ficha de jugador dentro de `/market/buy` (pestaña "Comprar tokens" del Mercado), **no** en `/players` ni PlayerDetailPage. | FR-01, FR-02, US1 actualizados a `/market/buy`. |
| **Notificaciones visuales** | Requeridas (toast/alert) para éxito y errores; **sin mencionar librería** en spec. Implementación futura: React-Toastify. | FR-14 añadido; FR-09/FR-10 referencian "notificación visual". |
| **Invalidación queries** | Invalidar la key de cotización real definida por `market`, `portfolioQueryKeys.summary` y `portfolioQueryKeys.holdings(...)` (todas las combinaciones). La invalidación de Portfolio se coordina en `app`; nunca invalidar la cotización demo de `players`. | FR-11 actualizado; reemplaza la decisión anterior sobre H2. |
| **Formato precio display** | **Principal: créditos con 2 decimales** ("12,50 créditos"); secundario: centésimas entre paréntesis ("1.250 centésimas"). Backend usa centésimas; frontend convierte. | FR-04, US2 Acceptance Scenarios, Assumptions actualizados. |
| **Sesión expirada en modal** | 401 → redirige a login **sin restaurar modal** ni persistir intención de compra. | Edge Case actualizado; fuera de alcance v2. |
| **Componentes del mercado** | `MarketPlayerCard` y `BuyTokenModal` locales a `MarketBuyPage`, sin conectar Hooks directamente; la Page coordina sus datos y acciones. No modificar `features/players`. | FR-02, FR-04, FR-09 y diseño derivados. |
| **Resultado de compra** | El Service devuelve `BuyResult` sin nombre; la Page utiliza el nombre del jugador seleccionado solo para la notificación de éxito. | FR-09 y contrato HTTP. |
| **Foco al cerrar modal** | Restaurar foco al botón "Comprar" de la ficha que lo abrió. | FR-13, H3 aplicado. |
| **Origen y fecha de cotización** | Precio e instante `calculatedAt` reales de `GET /api/players/{id}/quotes/current`. Mostrar ese instante; nunca sustituirlo por fecha demo, fecha de lectura ni `Date.now()`. | FR-04, Key Entities; reemplaza el fallback anterior. |
| **Aislamiento de features** | `market` no importa de `players` ni `portfolio`; `app` compone datos y coordina invalidaciones mediante exports mínimos de sus `index.ts` raíz. Solo se autorizan esos exports nuevos, sin alterar lógica existente. | FR-02, FR-11 y plan afectados. |
| **Listado y stock** | Se listan jugadores visibles sin consultar stock; la compra puede rechazarse por `INSUFFICIENT_STOCK`. | FR-02 y US1 precisados. |
| **Estado del modal y cantidad** | Se preservan estado y filtros; cantidad inicial 1 según ref. 05. Texto numérico entero válido se envía como número; −/+ y cantidades rápidas comparten la validación. | FR-03–FR-05, FR-19 y US1/US2. |
| **Ampliación de filtros (2026-10-24)** | El usuario confirma búsqueda/liga/posición/precio combinables sobre catálogo completo, orden previo a paginación, limpieza individual/global y política de cotización ausente. Sustituye la exclusión anterior de filtros nuevos. | FR-16–FR-18; plan, research, modelo, contrato y tareas deben revalidarse. |

### Escenarios de aceptación de filtros y corrección visual

1. Buscar un jugador ubicado fuera de la primera página original: debe aparecer tras completar el catálogo y aplicar búsqueda, sin depender de la página original.
2. Combinar búsqueda, liga y posición; añadir rango de precio y ordenar descendente: todos los resultados cumplen los filtros, se ordenan antes de dividirlos en páginas y los límites son inclusivos.
3. Quitar solamente liga conserva los demás filtros; limpiar todo restaura el conjunto completo y la primera página. Opciones de liga/posición proceden de todo el catálogo, no de la página visible.
4. Sin rango, los jugadores con ausencia confirmada de cotización siguen visibles al final del orden por precio. Con mínimo o máximo, se excluyen. Fallos GET se diferencian de ausencia y pueden reintentarse sin POST.
5. Abrir/cerrar modal conserva filtros aplicados, borrador del panel, página y scroll. Cambiar cantidad con −/+, accesos rápidos o teclado no ejecuta compras.
6. A 375/768/1440px se accede a panel, fichas, paginación y modal sin recortes ni scroll horizontal; overlay sin blur y fallback local si ambas URLs externas fallan.

---

## Riesgos detectados

1. **Race condition cotización**: Usuario abre modal, cotización cambia antes de confirmar. Backend revalida bajo bloqueo; el frontend presenta estimación con `calculatedAt` de la cotización persistida consultada y comunica el precio realmente ejecutado devuelto por la compra.
2. **Doble envío por latencia**: Usuario impaciente clica 2x. *Mitigación*: `aria-busy`, deshabilitar botón, flag `isSubmitting` en hook.
3. **Stock superusuario**: Si superusuario agota tokens, `INSUFFICIENT_STOCK`. No hay mecanismo de reposición en esta feature. *Documentar* en CA-07.
4. **Accesibilidad overlay**: `role="dialog" aria-modal="true"` + foco trap y restauración; PLAN adopta un `Modal` genérico de `shared` sin biblioteca modal adicional. Su comportamiento requiere comprobación humana.
5. **Testabilidad**: Frontend exento de tests (Constitución §199); validación manual con casos de Acceptance Scenarios.
6. **Invalidación de holdings**: Invalidar todas las páginas/filtros puede disparar refetch de consultas activas; limitarse al prefijo público `portfolioQueryKeys.holdings(...)` desde `app`.
7. **Nueva feature `market`**: Requiere composición en `app` con `usePlayersQuery` y las keys públicas de Portfolio; `market` solo recibe datos de catálogo y callback de éxito, sin dependencia entre features.
8. **Cotización operativa**: La serie demo de `players` no representa precio de compra; consultar exclusivamente el endpoint de cotización vigente persistida y mostrar su `calculatedAt`.

---

## Alcance vigente del rediseño visual (solicitud posterior del usuario)

Esta decisión **sustituye FR-16–FR-18 y la ampliación funcional de filtros del 2026-10-24**, antes de su implementación. La tarea vigente es exclusivamente visual: no cargar catálogo completo ni implementar búsqueda, filtros o ordenamiento. Se conserva la lógica de compra y los contratos HTTP actuales. Los escenarios de filtros anteriores no son criterios de aceptación de este rediseño.

- **VR-01:** reproducir la composición 04: fondo azul oscuro/degradados, encabezado con icono, tres tarjetas de métricas, panel lateral completo y fichas compactas en tres columnas desktop. Métricas sin fuente muestran «No disponible», estadísticas/variación «Sin datos», nunca valores simulados. Contador usa total real entregado por app.
- **VR-02:** panel de búsqueda/liga/posición/precio/orden/dirección/aplicar/limpiar y favoritos meramente visuales, deshabilitados y rotulados «Pendiente». No simular filtros aplicados ni acciones exitosas. Selector de tamaño pendiente; paginación existente funcional y sin cambiar su contrato.
- **VR-03:** modal según 05, con identidad, posición/liga, precio real, resumen, controles −/+ y cantidades rápidas visuales pendientes (sin nueva lógica), campo editable y confirmación existentes. Overlay oscuro sin blur y sin reflow.
- **VR-04:** fallback local de imágenes, scroll vertical accesible y sin overflow horizontal a 375/768/1440px, sin modificar lógica de players/portfolio/backend ni realizar compras durante inspección.

La fidelidad estructural no exige inventar métricas, favoritos, escudos, banderas ni valores. Las discrepancias por datos ausentes y controles pendientes se informan explícitamente. No se aprueban gates humanos por este documento.

## Próximo paso SDD

### Ajuste de scroll autorizado

### Corrección de navegación y diagnóstico de cotización

El usuario autoriza iconos Lucide para las cuatro pestañas de referencia 04 (Comprar tokens, Vender tokens, Mis órdenes, Historial de operaciones); las tres últimas permanecen deshabilitadas y pendientes, sin rutas nuevas. Aumentar separación inferior del botón Aplicar y tematizar únicamente scrollbar del listado.

Confirmar compra requiere cantidad válida, cotización real válida (precio entero positivo y seguro en centésimas, effectiveDate/calculatedAt válidos), consulta exitosa sin carga/pausa/error, y ausencia de envío o éxito previo. Mismo guard en handler, no solo disabled. Solo `404 QUOTE_NOT_FOUND` es ausencia; 401/autenticación, 403/permisos, otros HTTP, red y respuesta inválida se distinguen sin revelar cuerpos técnicos. GET se puede reintentar sin POST; 401 conserva gestión global de sesión. No habilitar por datos stale de una consulta fallida ni usar demo.

Verificar respuesta real autenticada solo si hay acceso; no fabricar evidencia. El usuario autorizó continuar sin aportar respuesta de Network después de comprobar indisponibilidad del navegador. Si no se confirma la causa backend, describir condiciones documentadas y evidencia pendiente, sin inicializar/recalcular mercado ni ejecutar compras.

En `/market/buy`, únicamente el listado de jugadores tiene scroll vertical; navbar, encabezado, métricas, filtros y paginación permanecen estáticos. El listado ocupa el espacio restante del viewport descontado por flex/grid, sin alturas de navbar codificadas. Ni body, AppLayout ni MarketBuyPage se desplazan en esta ruta; las demás rutas conservan su comportamiento. El modal conserva su scroll propio cuando está abierto.

El usuario autorizó **filtros plegables en móvil**, con métricas compactas, para no consumir todo el viewport. El desplegable nativo no implementa lógica de filtros; su panel se superpone al listado sin desplazar encabezado ni paginación. Se conservan tres columnas desktop y adaptación de columnas existente. Verificación requerida: 375/768/1440px, sin compras ni tests.

> Revalidar plan, tareas y checklists contra esta aclaración arquitectónica y ejecutar `/speckit.analyze` antes de solicitar autorización para implementar.

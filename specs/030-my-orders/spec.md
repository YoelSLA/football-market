# Feature Specification: Visualizar y gestionar órdenes de venta personales (Mis órdenes)

**Feature Branch**: `feat/030-my-orders`  
**Estado**: Especificación viva del sistema.  
**Issue de origen**: [#105](https://github.com/YoelSLA/football-market/issues/105) (`SPEC-030 | Visualizar y gestionar órdenes de venta personales (Mis órdenes)`).  
**Referencias normativas**:
- [`docs/wireframes/my-orders.md`](../../docs/wireframes/my-orders.md)
- [`specs/030-my-orders/contracts/my-orders-frontend.md`](contracts/my-orders-frontend.md)
- [`specs/011-sell-player-tokens/spec.md`](../011-sell-player-tokens/spec.md) (Contrato backend base de órdenes y publicaciones)
- [`specs/021-publish-token-sales-frontend/spec.md`](../021-publish-token-sales-frontend/spec.md) (Flujo de venta en frontend)

---

## 1. User Scenarios & Testing

### User Story 1 — Consultar y visualizar mis órdenes de forma paginada (Priority: P1)

Como usuario autenticado de Football Market, quiero acceder a una pantalla dedicada dentro de la sección de Mercado para consultar de forma paginada y ordenada mis publicaciones de venta de tokens, conociendo su estado, cantidades y valores económicos.

**Independent Test**: Con un usuario autenticado que tiene publicaciones registradas en distintos estados (`ACTIVE`, `SOLD`, `CANCELLED`, `EXPIRED`), acceder a `/market/orders` y verificar que el backend entrega la primera página de publicaciones ordenadas por defecto por las más recientes, acompañadas de metadatos de paginación y enriquecidas con el perfil deportivo del jugador.

**Acceptance Scenarios**:
1. El acceso a la pantalla se realiza desde la pestaña `"Mis órdenes"` en el Navbar de Mercado (`/market/orders`).
2. Cada orden en la lista expone identificador (`#listingId`), fecha de creación, retrato y datos del jugador, cantidad de tokens, precio unitario, importe total y estado efectivo.
3. El orden predeterminado en la consulta y presentación es *Más recientes* (`createdAt DESC, id DESC`).
4. Los valores de precio unitario e importe total se formatean dividiendo las centésimas recibidas por 100 y presentándolas con 2 decimales en moneda créditos.
5. El backend responde con la colección `listings` paginada y metadatos de paginación (`page`, `size`, `totalElements`, `totalPages`).
6. La información del usuario se deriva estrictamente de la sesión autenticada; no se permite consultar publicaciones de otros usuarios en esta pantalla.

---

### User Story 2 — Filtrar por estado, paginar y buscar (Priority: P1)

Como usuario con múltiples órdenes registradas, quiero poder filtrar mis publicaciones por su estado efectivo (`Todas`, `Activas`, `Vendidas`, `Canceladas`, `Expiradas`), buscar por nombre de jugador y recorrer las distintas páginas de resultados.

**Independent Test**: Modificar el filtro de estado en la UI; verificar que se solicita al backend la página correspondiente con el parámetro `status`, reiniciando a la página 0 y actualizando los controles en los search params de la URL.

**Acceptance Scenarios**:
1. El selector de estado permite conmutar entre `Todas`, `Activas` (`ACTIVE`), `Vendidas` (`SOLD`), `Canceladas` (`CANCELLED`) y `Expiradas` (`EXPIRED`).
2. El cambio de filtro de estado envía el valor al backend y restablece el índice de página a 0.
3. La búsqueda de texto filtra sobre el nombre del futbolista o del club sobre los elementos visibles o mediante recarga coordinada.
4. Si la combinación de filtros no arroja resultados, se presenta un mensaje comprensible de *"Sin resultados"* y la acción *"Limpiar filtros"*.
5. La navegación entre páginas (siguiente, anterior, número de página) actualiza la llamada `GET /api/listings/mine?page=X&size=10`.

---

### User Story 3 — Cancelar una publicación activa propia con modal accesible (Priority: P1)

Como vendedor en el mercado secundario, quiero retirar una oferta activa que ya no deseo mantener a la venta para liberar de inmediato los tokens reservados y volver a disponer de ellos en mi cartera, mediando confirmación explícita para evitar errores involuntarios.

**Independent Test**: Seleccionar una publicación en estado `ACTIVE`, accionar el botón de cancelación, confirmar en el modal `CancelListingModal` y comprobar que se envía la solicitud `POST /api/listings/{id}/cancel`, se actualiza el estado de la publicación a `CANCELLED`, se liberan los tokens en la cartera y se notifica al usuario con un toast de confirmación.

**Acceptance Scenarios**:
1. El botón de cancelación solo está visible y habilitado para publicaciones cuyo estado efectivo sea `ACTIVE`.
2. Al pulsar *"Cancelar"*, se despliega el modal de confirmación `CancelListingModal`, indicando el jugador, cantidad de tokens y créditos del lote a retirar, advirtiendo que los tokens quedarán disponibles en la cartera.
3. Si el usuario cancela o cierra el modal, no se emite ninguna petición HTTP y la orden permanece activa.
4. Si el usuario confirma en el modal, se ejecuta `POST /api/listings/{id}/cancel`.
5. Tras una respuesta exitosa (HTTP 200), la orden se actualiza a estado `CANCELLED`, se muestra un toast informativo de éxito y se invalidan las consultas de órdenes y tenencias de cartera.
6. En caso de error (ej. 409 por venta concurrente o expiración), se presenta un mensaje descriptivo sin exponer datos sensibles.

---

### User Story 4 — Resumen analítico de órdenes provisto por el Backend (Priority: P2)

Como usuario operador del mercado, quiero ver tarjetas con métricas agregadas de mis publicaciones para comprender de un vistazo cuántos tokens tengo comprometidos y qué volumen de créditos representa mi oferta activa.

**Independent Test**: Visualizar las 4 tarjetas superiores de métricas y verificar que los datos provistos en el bloque `summary` del backend se exhiben fielmente.

**Acceptance Scenarios**:
1. *Órdenes activas*: Informa el total de publicaciones en estado `ACTIVE` y la suma de tokens en venta.
2. *Valor en venta*: Suma acumulada de `totalAmount` (en créditos) de todas las órdenes activas.
3. *Órdenes vendidas*: Cantidad de publicaciones en estado `SOLD` y tokens totales liquidados.
4. *Cerradas / Expiradas*: Conteo consolidado de órdenes en estado `CANCELLED` y `EXPIRED`.
5. Las métricas del resumen representan la totalidad de órdenes del usuario independiente de la página actual solicitada.

---

### User Story 5 — Manejo de estados de carga, vacío y error (Priority: P2)

Como usuario, quiero contar con respuestas visuales claras cuando el sistema está cargando, cuando no tengo órdenes registradas o si ocurre un fallo de conexión.

**Independent Test**: Provocar estados de carga, estado vacío (usuario sin órdenes) y simulación de error HTTP; comprobar la coherencia de los componentes y accesibilidad.

**Acceptance Scenarios**:
1. Durante la carga inicial, se muestran skeletons en métricas y tabla sin saltos de layout.
2. Si el usuario no tiene ninguna publicación creada, se presenta una pantalla vacía con botón directo *"Publicar tokens"* hacia `/market/sell`.
3. Ante errores de red o servidor (HTTP 500), se muestra un panel de error con botón *"Reintentar"*.

---

## 2. Requisitos funcionales

### Backend
- **FR-001**: `GET /api/listings/mine` MUST soportar paginación server-side mediante los parámetros `page` (base 0) y `size` (default 10).
- **FR-002**: `GET /api/listings/mine` MUST aplicar ordenamiento predeterminado *Más recientes* (`createdAt DESC, id DESC`).
- **FR-003**: `GET /api/listings/mine` MUST responder con una estructura que incluya `summary` (métricas globales), `listings` (elementos de la página) y `pagination` (`page`, `size`, `totalElements`, `totalPages`).
- **FR-004**: Los importes numéricos en backend se conservan en centésimas y en frontend MUST ser transformados dividiendo por 100.
- **FR-005**: La cancelación de publicaciones activas MUST invocar `POST /api/listings/{id}/cancel` con el `listingId` correspondiente.

### Frontend
- **FR-006**: La ruta protegida de la vista MUST ser `/market/orders` y la pestaña activa en el Navbar MUST ser `"Mis órdenes"`.
- **FR-007**: La confirmación de cancelación MUST requerir interacción deliberada del usuario a través del modal accesible `CancelListingModal`.
- **FR-008**: El listado MUST enriquecer la información de cada futbolista (`name`, `imageUrl`, `team`, `league`, `position`) mediante la información provista por el catálogo general de jugadores.
- **FR-009**: La pantalla MUST ajustarse al diseño desktop fijado en `docs/wireframes/my-orders.md` (1920 × 913 px), respetando la retícula de 8 px y los tokens SCSS, sin scroll global.
- **FR-010**: No se deben admitir modificaciones ni ediciones de precio o cantidad de órdenes existentes (solo cancelación).

---

## 3. Criterios de aceptación (Checklist)

- [ ] **CA-01**: `GET /api/listings/mine` responde de forma paginada con metadatos de paginación (`page`, `size`, `totalElements`, `totalPages`) y bloque `summary`.
- [ ] **CA-02**: Orden predeterminado en backend y frontend: *Más recientes* (`createdAt DESC, id DESC`).
- [ ] **CA-03**: Pestaña activa y ruta `/market/orders` operativa bajo la sección de Mercado.
- [ ] **CA-04**: La tabla presenta la paginación conectada al servidor (página solicitada, total de páginas y navegación fluida).
- [ ] **CA-05**: Tarjetas métricas calculadas con precisión a partir del bloque `summary` del backend.
- [ ] **CA-06**: Botón de cancelación disponible únicamente en publicaciones `ACTIVE`.
- [ ] **CA-07**: Modal de confirmación `CancelListingModal` con detalle de la orden y advertencia de liberación de tokens.
- [ ] **CA-08**: Ejecución exitosa de `POST /api/listings/{id}/cancel` con actualización reactiva inmediata e invalidación de caché TanStack Query.
- [ ] **CA-09**: Estado vacío con enlace a `/market/sell` cuando no existen órdenes.
- [ ] **CA-10**: Cumplimiento del viewport exclusivo desktop (>=1024 px) sin media queries móviles ni scroll horizontal.

---

## 4. Edge Cases

- **Cancelación concurrente / orden ya vendida**: Si el usuario intenta cancelar una orden que un comprador adquirió en ese mismo instante, el backend responderá `409 LISTING_NOT_AVAILABLE`. La UI debe capturar este error, cerrar el modal, informar mediante toast que la publicación ya fue adquirida o no está disponible, y refrescar la lista.
- **Orden expirada durante la visualización**: Si una orden activa cumple su tiempo de vigencia (`now >= expiresAt`) mientras el usuario está en la página, al intentar cancelarla el backend rechazará con `409 LISTING_NOT_AVAILABLE`. La interfaz actualizará su estado a `EXPIRED`.
- **Página fuera de rango tras cancelaciones**: Si se eliminan/cancelan órdenes y la página actual queda vacía, la interfaz debe navegar a la última página válida disponible.
- **Jugador no visible o desactivado**: Si el jugador asociado a una publicación deja de estar visible en el catálogo, la publicación propia sigue siendo visible para el vendedor conforme a las reglas de negocio de SPEC-011.

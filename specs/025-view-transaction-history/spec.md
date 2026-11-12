# Feature Specification: Historial personal de transacciones

**Feature Branch**: `feat/025-view-transaction-history`
**Estado**: Especificación viva del sistema.
**Referencias normativas**: [`docs/wireframes/transaction-history.md`](../../docs/wireframes/transaction-history.md), [contracts/transaction-history.md](contracts/transaction-history.md), [data-model.md](data-model.md) y [SPEC-020](../../specs/020-view-operation-history/spec.md).

---

## User Scenarios & Testing

### User Story 1 - Explorar el historial personal (Priority: P1)

Como usuario autenticado, quiero consultar una página de mis compras y ventas confirmadas con el perfil actual del jugador para revisar mis operaciones en el mercado sin abandonar mi cartera.

**Independent Test**: Con compras y ventas confirmadas, la pantalla presenta únicamente operaciones propias, una vez cada una, con perfil actual del jugador, tipo, cantidad, valores financieros históricos, fecha e identificador.

**Acceptance Scenarios**:
1. Una compra se presenta como `COMPRA` y una venta como `VENTA` desde la perspectiva de la sesión.
2. Cada fila agrupa imagen, nombre, equipo y liga actuales del jugador, además del número de transacción, sin convertir el jugador en enlace interactivo.
3. Cantidad, precio unitario, importe total y ejecución reproducen el registro confirmado y no se recalculan con cotizaciones vigentes.
4. El endpoint obtiene la identidad exclusivamente del JWT y nunca acepta un usuario seleccionable.

---

### User Story 2 - Buscar, filtrar y ordenar (Priority: P1)

Como usuario con muchas operaciones, quiero buscar por jugador y filtrar por tipo y fecha para encontrar movimientos concretos, conservando los criterios en la URL.

**Independent Test**: Aplicando cada criterio por separado y en combinación, el backend devuelve solo coincidencias, indicadores del conjunto filtrado y una página estable; recargar la URL conserva los controles.

**Acceptance Scenarios**:
1. La búsqueda compara el texto con el nombre actual normalizado del jugador.
2. `BUY` y `SELL` limitan las operaciones según el rol de la sesión.
3. El rango temporal usa inicio inclusivo (`executedFrom`) y final exclusivo (`executedBefore`) expresados como instantes con offset.
4. El orden admite únicamente `NEWEST`, `OLDEST`, `AMOUNT_DESC` y `AMOUNT_ASC`, con desempate determinista por ejecución e identificador.
5. Una combinación sin coincidencias muestra `Sin resultados`, distinto del historial inicialmente vacío.

---

### User Story 3 - Comprender actividad y paginación (Priority: P1)

Como usuario, quiero ver indicadores agregados y recorrer páginas para dimensionar la actividad coincidente sin descargar el historial completo.

**Independent Test**: Con más de 20 operaciones, la respuesta informa totales del conjunto filtrado, entrega solo la página solicitada y permite recorrer todas las páginas sin duplicados ni omisiones.

**Acceptance Scenarios**:
1. Los cuatro indicadores informan compras y tokens comprados, ventas y tokens vendidos, volumen comprado y volumen vendido sobre todo el conjunto filtrado.
2. Ningún volumen se rotula como ganancia, pérdida o rentabilidad.
3. La paginación usa base cero en HTTP, tamaño default 20 y máximo 100.
4. Cambiar búsqueda, filtro u orden restablece la página a cero.

---

### User Story 4 - Resolver estados de consulta (Priority: P2)

Como usuario, quiero distinguir carga, error, historial vacío y filtros sin resultados para saber si debo esperar, reintentar, ir al mercado o limpiar criterios.

**Independent Test**: Simular carga, error de red, usuario nuevo sin operaciones y filtros sin coincidencia; comprobar mensajes, botones de acción y centrado dentro de la región vertical disponible.

**Acceptance Scenarios**:
1. Carga y error son anunciables mediante atributos de accesibilidad; el error permite reintentar.
2. Un historial sin operaciones ofrece `Ir al mercado` hacia `/market/buy`.
3. Un conjunto filtrado vacío ofrece `Limpiar filtros`.
4. La región del historial ocupa el espacio vertical disponible; el contenido de los estados conserva composición compacta y se centra sin expandir filas.

---

### Edge Cases

- **Desempates estables**: Dos operaciones con igual valor de orden conservan un desempate determinista por ejecución e ID.
- **Página fuera de rango**: Una página fuera de rango devuelve `transactions: []` y metadatos de paginación válidos.
- **Imágenes ausentes**: Se representan como `null` y el frontend aplica el icono o retrato genérico compartido.
- **Integridad referencial**: Jugador, equipo y liga son relaciones obligatorias; una inconsistencia no se disfraza como perfil parcial.
- **Actualizaciones de catálogo**: Si cambian nombre, equipo, liga o imagen en el catálogo, el historial refleja el perfil actual en la siguiente consulta; no son snapshots históricos.
- **Rango temporal invertido**: Un `executedFrom` igual o posterior a `executedBefore` se rechaza con HTTP 400 `INVALID_DATE_RANGE`.

---

## Requirements

### Functional Requirements

- **FR-001**: La ruta protegida MUST ser `/portfolio/transactions` bajo `Cartera` y MUST ser la única entrada de navegación al historial.
- **FR-002**: `GET /api/users/me/transactions` MUST identificar al usuario exclusivamente por el JWT y MUST NOT aceptar selección de identidad.
- **FR-003**: El endpoint MUST responder un objeto con `summary`, `transactions` y `pagination`.
- **FR-004**: Cada operación MUST incluir `transactionId`, `type`, `player`, `quantity`, `unitPrice`, `totalAmount` y `executedAt`.
- **FR-005**: `player` MUST incluir ID, nombre e imágenes actuales, equipo actual con ID/nombre y liga actual con ID/nombre, obtenidos mediante relaciones existentes y sin llamadas frontend adicionales.
- **FR-006**: La documentación y la UI MUST identificar el perfil del jugador como actual; MUST NOT presentarlo como snapshot ni dato histórico de la operación.
- **FR-007**: Cantidad, precio, importe, tipo e instante MUST conservar los valores confirmados y MUST NOT modificarse ni recalcularse.
- **FR-008**: `imageUrl` y `fallbackImageUrl` MAY ser `null`; jugador, equipo y liga MUST estar completos en una respuesta exitosa.
- **FR-009**: El backend MUST admitir búsqueda opcional por nombre normalizado, tipo opcional `BUY|SELL`, `executedFrom` inclusivo y `executedBefore` exclusivo.
- **FR-010**: El backend MUST admitir únicamente los órdenes `NEWEST`, `OLDEST`, `AMOUNT_DESC` y `AMOUNT_ASC`.
- **FR-011**: Todo orden MUST incluir desempates estables para evitar duplicados o saltos entre páginas.
- **FR-012**: La paginación MUST usar `page >= 0`, `1 <= size <= 100`, defaults `0` y `20`, y MUST informar página, tamaño, elementos y páginas totales.
- **FR-013**: `summary` MUST calcularse sobre todo el conjunto filtrado y MUST informar conteos de operaciones, tokens y volúmenes separados para compras y ventas.
- **FR-014**: Los volúmenes MUST sumar `totalAmount` histórico y MUST NOT interpretarse como resultado financiero o rentabilidad.
- **FR-015**: Contenido, conteo y agregados MUST aplicar exactamente los mismos predicados de filtrado.
- **FR-016**: La lectura MUST evitar N+1 y MUST paginar en base de datos; no puede consultar un jugador por fila ni paginar en memoria.
- **FR-017**: Parámetros inválidos MUST producir HTTP 400 (`INVALID_PARAMETER_TYPE` o `INVALID_DATE_RANGE`); sesión inválida MUST producir HTTP 401 y fallos internos HTTP 500 sin datos parciales.
- **FR-018**: Los criterios de UI MUST usar URL search params como única fuente de verdad y la query key de TanStack Query MUST incluirlos.
- **FR-019**: El nombre y retrato del jugador MUST ser puramente informativos y MUST NOT navegar a su ficha de detalle.
- **FR-020**: La interfaz MUST reutilizar `PlayerPortrait`, `Pagination`, `Button`, iconos, formatters y tokens de diseño existentes.
- **FR-021**: El alcance visual MUST ser exclusivamente desktop y MUST seguir el wireframe aprobado sin agregar adaptaciones específicas para tablet o móvil.
- **FR-022**: La pantalla MUST presentar encabezado compacto, cuatro indicadores, filtros, tabla y paginación, con espacios locales de hasta 8 px.
- **FR-023**: La región del historial MUST ocupar el espacio vertical disponible en desktop. Carga, error, vacío y sin resultados MUST conservar contenido compacto, accesible y centrado dentro de esa región; las filas MUST mantener su altura natural y la paginación MUST permanecer alineada al borde inferior cuando exista.
- **FR-024**: El estado vacío MUST navegar a `/market/buy`; `Sin resultados` MUST limpiar los criterios sin abandonar la pantalla.
- **FR-025**: La implementación MUST NOT modificar migraciones, datos persistidos, reglas contables ni flujos de compra y venta.

---

## Key Entities

- **TransactionHistory**: Modelo frontend que agrupa el resumen filtrado, página de operaciones y metadatos de paginación.
- **TransactionHistoryEntry**: Representación de una transacción desde la perspectiva de la sesión (`BUY` o `SELL`).
- **TransactionPlayer**: Perfil actual del jugador y sus relaciones de equipo y liga.
- **TransactionHistoryQueryParams**: Criterios de consulta sincronizados con la URL (`page`, `size`, `search`, `type`, `executedFrom`, `executedBefore`, `sort`).

---

## Success Criteria

- **SC-001**: El 100% de las filas muestra identidad actual del jugador y los cinco datos financieros/temporales obligatorios sin consultas frontend adicionales.
- **SC-002**: El 100% de las combinaciones válidas aplica criterios idénticos a filas, total e indicadores agregados.
- **SC-003**: Un conjunto de más de 20 operaciones puede recorrerse por páginas sin duplicados ni omisiones bajo un estado estable de base de datos.
- **SC-004**: En resolución de referencia desktop (`1920 × 913 px`), la vista mantiene legibles indicadores, filtros, tabla y paginación sin scroll horizontal.
- **SC-005**: Carga, error, vacío y sin resultados se distinguen inequívocamente y quedan centrados dentro de una región de historial de altura consistente con la tabla, sin expandir artificialmente su contenido.
- **SC-006**: Una persona sin conocimiento previo identifica jugador, tipo e importe de una transacción indicada en ≤ 10 segundos sobre una pantalla cargada con 20 operaciones.

---

## Assumptions & Dependencies

- Las relaciones `MarketTransaction.player`, `Player.team` y `Team.league` son íntegras y obligatorias.
- Los datos descriptivos del catálogo pueden actualizarse sin alterar el registro financiero histórico.
- Reutiliza `AppLayout`, sesión JWT, cliente `http`, TanStack Query y componentes compartidos del proyecto.

---

## Out of Scope

- Snapshots históricos de datos de jugador, equipo, liga o imágenes.
- Migraciones de esquema, backfills o tablas adicionales en la base de datos.
- Contrapartes, saldos remanentes, comisiones, rentabilidad calculada o exportaciones a archivos externos.
- Navegación hacia la ficha del jugador desde la tabla de historial.
- Diseños o adaptaciones específicas para viewports de tablet o móvil.

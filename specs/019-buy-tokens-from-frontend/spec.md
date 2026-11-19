# Feature Specification: Comprar tokens en el mercado

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/019-buy-tokens-from-frontend` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `019-buy-tokens-from-frontend`, sin el prefijo de tipo.

**Created**: 2026-10-23

**Status**: Approved

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Consolidar la compra de tokens desde el Mercado Oficial y el Mercado P2P, integrar el catálogo oficial de SPEC-010 y aplicar el diseño aprobado del Mercado Oficial y su modal de compra."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Explorar el Mercado Oficial (Priority: P1)

Un usuario autenticado accede al Mercado Oficial, consulta jugadores con sus datos deportivos y cotizaciones vigentes, y reduce el catálogo mediante filtros, ordenamiento y paginación sin abandonar la página.

**Why this priority**: Es el punto de entrada a la emisión primaria y permite encontrar un jugador comprable antes de iniciar una transacción.

**Independent Test**: Abrir `/market/buy`, combinar los ocho controles disponibles, cambiar el orden, recargar la URL y comprobar que el catálogo, los controles y la página se reconstruyen con el mismo estado y ocho jugadores por página.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** abre `/market/buy`, **Then** visualiza el catálogo paginado consolidado con identidad deportiva, precio actual, variación, precio anterior y vigencia de cada jugador cotizado.
2. **Given** un jugador sin cotización vigente, **When** aparece en el catálogo, **Then** conserva sus datos deportivos, muestra «Sin cotización» y no permite iniciar la compra.
3. **Given** filtros u ordenamiento válidos, **When** el usuario modifica un control, **Then** los resultados se actualizan automáticamente sobre el catálogo completo, la página vuelve a la primera y la URL representa el estado vigente.
4. **Given** una búsqueda por nombre, **When** el usuario escribe, **Then** la actualización se difiere mediante debounce para evitar solicitudes por cada pulsación sin exigir un botón «Aplicar».
5. **Given** una liga seleccionada, **When** el usuario abre el filtro de equipo, **Then** solo encuentra equipos de esa liga; al cambiar o limpiar la liga se limpia cualquier equipo incompatible.
6. **Given** una URL válida del Mercado Oficial, **When** se recarga o se navega con Back/Forward, **Then** filtros, orden y página se reconstruyen de forma determinista.
7. **Given** resultados disponibles, **When** se renderiza la página, **Then** el total aparece una sola vez sobre la grilla y el catálogo muestra ocho jugadores por página en dos filas de cuatro sin scroll vertical en el viewport desktop aprobado.
8. **Given** una cotización sin precio anterior o sin variación calculable, **When** aparece en el catálogo o el modal, **Then** se indica de forma neutra que no existe una referencia previa sin confundirla con una variación de cero.

---

### User Story 2 - Comprar emisión oficial (Priority: P1)

Un usuario selecciona un jugador cotizado, define una cantidad entre 1 y 100, revisa el precio estimado y confirma una compra directa al emisor oficial.

**Why this priority**: Completa la operación principal de adquisición de tokens en el mercado primario.

**Independent Test**: Abrir el modal desde una ficha cotizada, cambiar la cantidad por campo, controles incrementales y accesos rápidos, confirmar y comprobar que se envía una sola compra y se informa el resultado real.

**Acceptance Scenarios**:

1. **Given** una ficha con cotización vigente, **When** el usuario pulsa «Comprar», **Then** se abre el modal sin alterar URL, filtros, página ni posición del mercado y con cantidad inicial 1.
2. **Given** el modal abierto, **When** el usuario ingresa un entero entre 1 y 100 o usa los controles −/+, **Then** se actualizan cantidad e importe estimado en créditos con precisión de centésimas.
3. **Given** el modal abierto, **When** el usuario pulsa 1, 5, 10, 50 o 100, **Then** el control establece esa cantidad absoluta en lugar de sumarla al valor actual.
4. **Given** una cantidad vacía, no numérica, decimal o fuera de 1–100, **When** se valida el formulario, **Then** se muestra un error junto al campo y no se permite confirmar.
5. **Given** cantidad válida y ausencia de una solicitud en curso, **When** el usuario confirma, **Then** se envía una única compra directa con jugador y cantidad, se muestra el estado de procesamiento y se previene el doble envío.
6. **Given** una compra exitosa, **When** llega la respuesta, **Then** se informa identificador de transacción, jugador, cantidad, precio unitario, total y fecha de ejecución reales, y se actualizan mercado, saldo y cartera relacionados.
7. **Given** que el usuario cancela, pulsa Escape o el overlay antes de confirmar, **When** se cierra el modal, **Then** no se genera una compra y el foco vuelve al control que abrió el diálogo.

---

### User Story 3 - Explorar ofertas del Mercado P2P (Priority: P1)

Un usuario autenticado accede al Mercado P2P y explora una ficha por jugador con publicaciones comprables de otros usuarios, agregadas y filtradas sobre el conjunto completo disponible.

**Why this priority**: Permite descubrir la oferta secundaria y elegir el activo antes de revisar publicaciones individuales.

**Independent Test**: Proveer varias publicaciones para jugadores repetidos, abrir `/market/p2p` y comprobar agrupación, agregados, filtros, orden y paginación local sin duplicar jugadores.

**Acceptance Scenarios**:

1. **Given** varias publicaciones comprables del mismo jugador, **When** carga el catálogo P2P, **Then** aparece una sola ficha con cantidad de publicaciones, tokens totales ofrecidos y mejor precio unitario.
2. **Given** búsqueda por jugador o club, **When** el usuario escribe, **Then** `search` se actualiza automáticamente después de 300 ms de debounce, se filtra el conjunto agrupado completo y se vuelve a la primera página.
3. **Given** liga, posición u orden, **When** cambia el control, **Then** su parámetro se actualiza inmediatamente, se filtra u ordena el conjunto agrupado completo antes de paginar y se vuelve a la primera página.
4. **Given** `search`, `league`, `position`, `sort` o `page` representados en una URL válida de `/market/p2p`, **When** el usuario comparte, recarga o recorre Back/Forward, **Then** el catálogo reconstruye el mismo estado visible sin perder esos parámetros.
5. **Given** un mercado sin publicaciones comprables, **When** abre `/market/p2p`, **Then** se muestra un estado vacío con una acción para volver al Mercado Oficial.
6. **Given** un fallo que impide formar un catálogo íntegro, **When** termina la lectura, **Then** no se presentan resultados parciales como completos y se ofrece reintentar.

---

### User Story 4 - Elegir y comprar una oferta P2P (Priority: P1)

Un usuario abre un jugador del Mercado P2P, compara todas sus publicaciones comprables y confirma la compra completa de una publicación elegida.

**Why this priority**: Completa la adquisición secundaria sin modificar la cantidad ni revelar la identidad del vendedor.

**Independent Test**: Abrir un jugador con varias publicaciones, elegir una, verificar cantidad fija y confirmar que la compra identifica exclusivamente la publicación seleccionada.

**Acceptance Scenarios**:

1. **Given** un jugador del catálogo P2P, **When** el usuario abre sus ofertas, **Then** la URL identifica al jugador y muestra todas sus publicaciones comprables con cantidad, precio unitario, total y vencimiento.
2. **Given** varias publicaciones, **When** se muestra el detalle, **Then** se ordenan inicialmente por precio unitario ascendente, vencimiento ascendente e identificador ascendente.
3. **Given** una publicación seleccionada, **When** se abre la confirmación, **Then** se muestran lote completo, precio unitario, total, vencimiento y saldo sin permitir editar la cantidad ni mostrar la identidad del vendedor.
4. **Given** una publicación disponible y saldo suficiente, **When** el usuario confirma, **Then** se envía una sola compra identificada exclusivamente por la publicación y se informa el resultado real.
5. **Given** una publicación vendida, vencida, cancelada o retirada concurrentemente, **When** el usuario intenta comprarla, **Then** se informa que dejó de estar disponible y se refrescan catálogo y detalle sin afirmar una compra.
6. **Given** una interrupción sin respuesta concluyente, **When** falla la solicitud, **Then** no se reintenta automáticamente ni se afirma éxito o fracaso transaccional.
7. **Given** una compra P2P exitosa, **When** llega la respuesta, **Then** se actualizan publicaciones, saldo, resumen y tenencias, y el resultado permanece disponible hasta que el usuario lo cierre.
8. **Given** una cantidad variable de publicaciones, **When** se abre el detalle, **Then** se muestra la identidad deportiva completa y un resumen de mejor precio, tokens disponibles y publicaciones activas, seguido por cards compactas de altura propia que no se estiran para llenar el viewport.
9. **Given** que el saldo persistido cubre el total del lote, incluso con centésimas, **When** se abre la confirmación, **Then** el cliente no bloquea la compra y muestra saldo disponible y saldo restante estimado con precisión monetaria.
10. **Given** una compra exitosa consume la última oferta elegible del jugador, **When** el usuario cierra la confirmación, **Then** se elimina `player` de la URL y se vuelve al catálogo; si quedan ofertas elegibles, el detalle permanece abierto.

---

### User Story 5 - Operar mercados y diálogos de forma accesible (Priority: P2)

Un usuario puede recorrer ambos mercados y completar sus diálogos con teclado, distinguir estados de carga y error, y usar la interfaz sin desbordamientos en los tamaños aprobados.

**Why this priority**: Protege la comprensión y el control de acciones transaccionales sin alterar los flujos principales.

**Independent Test**: Recorrer cards, filtros, paginación y modales solo con teclado; comprobar foco, anuncios de estado, cierre y adaptación visual en 375, 768, 1440 y 1920 × 913 px.

**Acceptance Scenarios**:

1. **Given** un modal abierto, **When** el usuario navega con teclado, **Then** el foco queda contenido, sigue un orden lógico y vuelve al disparador al cerrar.
2. **Given** una lectura en curso, un error o un estado vacío, **When** cambia el estado, **Then** se anuncia de forma accesible y las acciones disponibles son inequívocas.
3. **Given** un viewport aprobado, **When** se usa cualquiera de los mercados o modales, **Then** no aparece desplazamiento horizontal y el contenido transaccional sigue siendo operable.

### Edge Cases

- Una combinación válida de filtros sin coincidencias muestra un estado «Sin resultados» distinto del mercado vacío y permite limpiar filtros.
- Un rango con valores negativos o mínimo mayor que máximo se rechaza junto a los campos y no genera una consulta inválida.
- Los ordenamientos oficiales por precio o variación ubican siempre al final los jugadores sin el valor ordenado; los empates conservan un orden determinista.
- Una cotización puede cambiar entre la visualización y la confirmación: el importe del modal es estimado y el resultado debe usar los importes ejecutados por el servidor.
- La presencia de un jugador oficial no garantiza stock; la disponibilidad se valida al confirmar la compra.
- Una sesión que expira durante un diálogo activa el flujo global de autenticación y no restaura la intención de compra.
- Una respuesta inválida o un error de lectura no se interpreta como ausencia de cotización, jugador u oferta.
- Una publicación que alcanza su vencimiento se deshabilita como orientación y provoca actualización; el servidor continúa siendo la autoridad de disponibilidad.
- Si todas las publicaciones de un jugador desaparecen, el detalle muestra un estado vacío y permite volver al catálogo P2P.
- Un saldo insuficiente conocido bloquea la confirmación como ayuda preventiva, pero la validación definitiva corresponde al servidor.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer dos páginas protegidas e independientes dentro de Mercado: «Mercado Oficial» en `/market/buy` y «Mercado P2P» en `/market/p2p`, sin selector interno de modalidad.
- **FR-002**: El Mercado Oficial DEBE consumir el catálogo consolidado definido por SPEC-010, incluyendo en una sola lectura paginada los datos deportivos, la cotización vigente nullable y la variación calculada.
- **FR-003**: El Mercado Oficial DEBE presentar ocho controles: búsqueda por jugador, liga, equipo, posición, nacionalidad, precio mínimo, precio máximo y orden. La interfaz NO DEBE presentar filtros por disponibilidad de cotización ni tamaño de página.
- **FR-004**: La búsqueda oficial DEBE filtrar únicamente por nombre o apellido del jugador, normalizar espacios, mayúsculas y acentos, y aplicarse automáticamente con debounce.
- **FR-005**: Los filtros de liga y equipo DEBEN obtener opciones de los filtros deportivos vigentes; el equipo DEBE depender de la liga y limpiarse cuando deje de ser compatible.
- **FR-006**: El precio oficial DEBE seleccionarse mediante un slider de rango doble cuyos extremos provienen de `priceRange`; debe operar en incrementos de una centésima de crédito, reflejar sus valores y aplicar la consulta únicamente al confirmar el arrastre. Una URL fuera del rango disponible DEBE normalizarse al extremo válido y un catálogo sin cotizaciones DEBE deshabilitar el control.
- **FR-007**: El Mercado Oficial DEBE permitir ordenar por precio, variación, nombre e identificador en las direcciones admitidas por SPEC-010, con un único criterio principal; precio o variación nulos DEBEN ubicarse al final y los empates resolverse de forma determinista.
- **FR-008**: El estado oficial DEBE sincronizar bidireccionalmente `name`, `leagueId`, `teamId`, `position`, `nationality`, `minPrice`, `maxPrice`, `sort` y `page` con la URL; filtros u orden modificados DEBEN volver a la primera página. Los parámetros heredados `hasQuote` y `size` DEBEN ignorarse, retirarse mediante navegación `replace` y no enviarse como criterios variables al backend.
- **FR-009**: La paginación oficial DEBE ejecutarse sobre el conjunto completo filtrado y mostrar ocho jugadores por página; el total de resultados DEBE mostrarse únicamente sobre la grilla.
- **FR-010**: El encabezado oficial DEBE ser compacto y mostrar identidad de la página, explicación transaccional y saldo disponible; en desktop los ocho controles DEBEN distribuirse en cuatro columnas y dos filas, con búsqueda, liga, equipo y posición en la primera, y nacionalidad, precio mínimo, precio máximo y orden en la segunda. La grilla DEBE adaptarse a cuatro columnas desde 1440 px y tres entre 1100 y 1439 px.
- **FR-011**: Cada ficha oficial DEBE mostrar únicamente retrato, posición, nombre, equipo, liga, nacionalidad, precio actual, variación, precio anterior, vigencia y acción de compra. Una variación igual a cero DEBE mostrarse como neutra; cuando el precio anterior o la variación no sean calculables, DEBE indicarse de forma neutra la ausencia de referencia previa.
- **FR-012**: Las fichas y el modal oficial NO DEBEN mostrar performance score, multiplicador, origen de cálculo ni fecha de cálculo; tampoco DEBEN exponer stock, inventario, tenencias o saldo del emisor oficial.
- **FR-013**: Una ficha oficial sin cotización DEBE conservar el mismo espacio visual, mostrar «Sin cotización» y deshabilitar la compra sin fabricar precios, variaciones ni fechas.
- **FR-014**: El Mercado Oficial DEBE contemplar carga, error con reintento, mercado vacío, filtros sin resultados y cotización ausente, sin presentar datos parciales o inválidos como completos.
- **FR-015**: Seleccionar un jugador oficial cotizado DEBE abrir un diálogo superpuesto sin cambiar URL, filtros, página ni scroll del mercado; cerrarlo DEBE preservar ese estado.
- **FR-016**: El diálogo oficial DEBE mostrar identidad deportiva, precio vigente, variación, precio anterior, vigencia, cantidad editable y resumen del importe; la cantidad inicial DEBE ser 1.
- **FR-017**: La cantidad oficial DEBE aceptar solo enteros de 1 a 100, convertirse a número antes de comprar y poder modificarse mediante campo, controles de una unidad y accesos rápidos absolutos 1, 5, 10, 50 y 100.
- **FR-018**: La confirmación oficial DEBE enviar únicamente el jugador y la cantidad; DEBE permanecer bloqueada ante cantidad inválida, cotización ausente o inválida, lectura pendiente o fallida, sesión no autenticada, solicitud en curso o éxito ya recibido.
- **FR-019**: La guarda de confirmación oficial DEBE aplicarse tanto al estado del botón como al manejador de la acción y prevenir solicitudes duplicadas.
- **FR-020**: La estimación oficial DEBE usar el precio visible, mientras que el éxito DEBE mostrar identificador de transacción, cantidad, precio unitario, total y fecha de ejecución devueltos por la compra.
- **FR-021**: Ante éxito oficial, el sistema DEBE actualizar el catálogo oficial, el saldo actual, el resumen de cartera y todas las tenencias, mostrar una notificación comprensible y permitir cierre por acción del usuario o automáticamente después de dos segundos.
- **FR-022**: Los errores de compra oficial DEBEN distinguir solicitud o cantidad inválida, créditos o stock insuficientes, cotización no disponible o inválida, administrador no autorizado, jugador inexistente o no visible, sesión expirada y fallo interno o de red, mediante mensajes seguros sin detalles técnicos.
- **FR-023**: El Mercado P2P DEBE leer el conjunto completo de publicaciones comprables disponible para la sesión, validar sus datos y usarlo como única fuente remota para catálogo y detalle.
- **FR-024**: El catálogo P2P DEBE agrupar las publicaciones válidas por jugador y calcular cantidad de publicaciones, suma segura de tokens ofrecidos y precio unitario mínimo sin mostrar una publicación inválida como válida.
- **FR-025**: Los datos deportivos del P2P DEBEN reutilizar el catálogo de jugadores vigente; un jugador cuyos metadatos indispensables no puedan resolverse NO DEBE presentarse con datos inventados.
- **FR-026**: El catálogo P2P DEBE permitir búsqueda normalizada por jugador o club, filtros por liga, equipo dependiente, posición y precio máximo por token, orden por mejor precio, cantidad ofrecida o nombre, y paginación local de 8 jugadores; el precio máximo DEBE filtrar publicaciones elegibles antes de agrupar y recalcular métricas, y todo filtro y orden DEBE preceder a paginar.
- **FR-027**: El catálogo P2P DEBE sincronizar bidireccionalmente `search`, `leagueId`, `teamId`, `position`, `maxPrice`, `sort` y `page` con los query parameters de `/market/p2p`; debe migrar enlaces heredados con `league`, restablecer `page=0` ante cambios y reconstruir el mismo estado mediante recarga o Back/Forward. `maxPrice` se controla con un slider de una sola posición, paso de una centésima y acción «Sin límite», y solo se confirma al finalizar el arrastre.
- **FR-037**: Los extremos del slider P2P DEBEN derivarse de la colección completa ya obtenida por `GET /api/listings`, nunca solo de la página agrupada visible ni mediante una llamada adicional.
- **FR-028**: Seleccionar un jugador P2P DEBE representar `player=<id>` en `/market/p2p?player=<id>` y mostrar allí su detalle completo; cerrar o volver DEBE retirar únicamente `player`, conservar los demás query parameters y restaurar el catálogo en el estado representado por la URL.
- **FR-029**: El detalle P2P DEBE mostrar todas las publicaciones comprables vigentes del jugador presentes en la lectura actual, ordenadas inicialmente por precio unitario, vencimiento e identificador ascendentes.
- **FR-030**: Cada publicación P2P DEBE mostrar cantidad fija, precio unitario, total y vencimiento; NO DEBE admitir compra parcial, campo de cantidad ni identidad pública del vendedor.
- **FR-038**: El detalle P2P desktop DEBE reutilizar la identidad compartida del jugador, presentar tres métricas agregadas y distribuir las publicaciones en una grilla de cards compactas. Cada card DEBE destacar el total del lote, conservar la acción «Comprar lote» y comunicar identificador, precio unitario, cantidad y tiempo restante.
- **FR-039**: La confirmación P2P desktop DEBE reutilizar la identidad compartida, presentar una cabecera compacta con el identificador de publicación, distribuir cantidad, precio unitario y vencimiento con claridad, destacar el total y mostrar saldo disponible y saldo restante estimado. Toda comparación monetaria DEBE usar centésimas de crédito; el backend conserva la autoridad sobre saldo, disponibilidad y vigencia.
- **FR-040**: Al cerrar una compra P2P exitosa, el sistema DEBE conservar el detalle si el jugador mantiene ofertas elegibles y volver al catálogo, retirando únicamente `player` de la URL, si la compra consumió la última. Los estados de detalle sin ofertas DEBEN conservar una salida accesible y visualmente integrada para accesos directos y cambios concurrentes.
- **FR-031**: La confirmación P2P DEBE mostrar el lote completo y enviar exclusivamente el identificador de publicación; el manejador DEBE repetir las guardas del botón y prevenir doble envío.
- **FR-032**: El sistema DEBE tratar publicación no disponible o inexistente, compra propia defensiva, créditos o stock insuficientes, administrador no autorizado, solicitud inválida, sesión expirada y fallos internos o inciertos con mensajes seguros y la actualización del mercado que corresponda.
- **FR-033**: Ante éxito P2P, el sistema DEBE actualizar publicaciones, usuario actual, resumen de cartera y todas las tenencias, y mostrar identificador de transacción, cantidad, precio unitario, total y fecha de ejecución reales sin autocierre obligatorio.
- **FR-034**: El P2P DEBE contemplar carga, error con reintento, mercado vacío, filtros sin resultados, jugador sin ofertas, expiración orientativa, saldo insuficiente, conflicto concurrente, éxito e incertidumbre de red.
- **FR-035**: Ambos diálogos DEBEN permitir cierre por botón, Escape y overlay cuando no exista una operación bloqueante; DEBEN contener el foco, restaurarlo al disparador, anunciar procesamiento y errores, y conservar scroll interno cuando sea necesario.
- **FR-036**: Las páginas y diálogos DEBEN evitar desplazamiento horizontal a 375, 768 y 1440 px; el Mercado Oficial DEBE respetar además la distribución aprobada para 1920 × 913 px, con navegación fija, grilla desplazable y paginación fuera de ese scroll.

### Key Entities *(include if feature involves data)*

- **Jugador de Mercado Oficial**: jugador visible y elegible con identidad deportiva y una cotización vigente opcional.
- **Cotización oficial**: precio actual en centésimas, precio anterior opcional, variación opcional y fecha de vigencia; sus datos técnicos de cálculo no forman parte de la presentación.
- **Consulta del Mercado Oficial**: filtros, ordenamiento y página representables en la URL y aplicados al catálogo completo con tamaño fijo de ocho.
- **Solicitud de compra oficial**: jugador y cantidad entera entre 1 y 100.
- **Publicación P2P**: oferta comprable con identificador, vendedor interno no visible, jugador, cantidad fija, precio unitario, total, estado y vencimiento.
- **Jugador de Mercado P2P**: identidad deportiva enriquecida con cantidad de publicaciones, tokens totales y mejor precio.
- **Solicitud de compra P2P**: identificador de una publicación; no contiene cantidad ni datos del jugador.
- **Resultado de compra**: identificador de transacción, jugador, cantidad, precio unitario, total, fecha de ejecución e identificador de publicación cuando corresponde.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de cada página del Mercado Oficial se presenta a partir de una única lectura consolidada, sin lecturas individuales de cotización por jugador.
- **SC-002**: El 100 % de los filtros, orden y página oficiales se reconstruye de forma idéntica al compartir, recargar o recorrer el historial de una URL válida.
- **SC-003**: El 100 % de los jugadores oficiales sin cotización permanece identificable y tiene la compra deshabilitada sin valores simulados.
- **SC-004**: Una compra oficial válida puede completarse en cuatro interacciones principales: abrir, definir cantidad, confirmar y reconocer el resultado.
- **SC-005**: Ninguna interacción múltiple durante el procesamiento origina más de una solicitud de compra.
- **SC-006**: El 100 % de las publicaciones válidas recibidas para un jugador P2P aparece en su detalle antes de aplicar exclusivamente el orden visual.
- **SC-007**: Ninguna compra P2P permite modificar la cantidad ni envía datos distintos del identificador de publicación.
- **SC-008**: El 100 % de los filtros, el ordenamiento, la página y el jugador seleccionado del Mercado P2P se reconstruye de forma idéntica al compartir, recargar o recorrer Back/Forward sobre una URL válida.
- **SC-009**: Una publicación retirada concurrentemente desaparece del catálogo y del detalle después de la actualización provocada por el conflicto.
- **SC-010**: Todos los errores controlados de compra definidos para ambos mercados tienen un mensaje de usuario seguro y ningún error desconocido expone detalles técnicos.
- **SC-011**: Catálogos, controles y diálogos son operables con teclado y no producen desplazamiento horizontal en 375, 768 y 1440 px; el Mercado Oficial tampoco desborda el viewport de referencia de 1920 × 913 px.

## Assumptions

- Los usuarios de ambos mercados ya están autenticados mediante el mecanismo vigente y las rutas conservan la protección existente.
- SPEC-010 es la fuente autoritativa del catálogo oficial consolidado y de la compra; SPEC-011 es la fuente autoritativa de las publicaciones P2P.
- El catálogo oficial admite páginas de 1 a 100 elementos; la interfaz usa el tamaño aprobado de 8 para evitar scroll vertical en el viewport desktop de referencia.
- Los importes se transportan en centésimas y se presentan principalmente como créditos con dos decimales.
- La presencia de un jugador oficial no acredita stock y la presencia local de una publicación no acredita disponibilidad final; la compra valida precio, saldo, stock y vigencia.
- El Mercado P2P usa el conjunto completo provisto por el contrato vigente, agrupa y pagina localmente y no requiere cambios en el backend.
- La identidad del vendedor P2P no es pública y no se resuelve mediante consultas adicionales.
- El saldo posterior solo se presenta cuando la información vigente conserva precisión suficiente; el servidor siempre decide la suficiencia.
- Se reutilizan la navegación, las notificaciones, los controles, la paginación, los recursos deportivos y el diálogo accesible existentes sin cambiar el comportamiento ajeno a SPEC-019.
- Los diseños funcionales y dimensionales aprobados se encuentran en `docs/wireframes/official-market.md` y `docs/wireframes/community-market.md`; cualquier decisión funcional no definida allí requiere CLARIFY antes de planificación o implementación.

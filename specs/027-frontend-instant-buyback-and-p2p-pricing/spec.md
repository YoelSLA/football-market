# Feature Specification: UI de venta inmediata oficial y selector de precio flexible en P2P (SPEC-027)

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/027-frontend-instant-buyback-and-p2p-pricing`  
**Created**: 2026-11-23  
**Status**: Draft  
**Issue de origen**: [#95](https://github.com/YoelSLA/football-market/issues/95) (`SPEC-027 | UI de venta inmediata oficial y selector de precio flexible en P2P`)  
**Requerimiento de origen**: `REQ-S2-FEAT-ADD` (Sprint 3)  
**Referencias normativas**:
- [`tp/enunciado.md`](../../tp/enunciado.md) (Mercado oficial, mercado secundario y operaciones de venta)
- [`docs/development/sdd-workflow.md`](../../docs/development/sdd-workflow.md)
- [`specs/021-publish-token-sales-frontend/spec.md`](../021-publish-token-sales-frontend/spec.md) (Flujo base de venta y modal existente)
- [`specs/026-market-instant-buyback-and-p2p-pricing/spec.md`](../026-market-instant-buyback-and-p2p-pricing/spec.md) (Contratos backend de venta oficial `POST /api/orders/sell-official` y unitPrice en `POST /api/listings`)
- [`specs/030-my-orders/spec.md`](../030-my-orders/spec.md) (Gestión de órdenes activas y cancelaciones)

**Input**: Issue #95. Interfaz de usuario para operar ventas de tokens en el mercado:
1. Modal de venta dinámico (`SellTokenModal`) con dos modalidades: Venta Inmediata Oficial (contra el superusuario con spread del 10%) o Publicación en Comunidad P2P.
2. Selector de precio flexible en P2P (slider e input numérico limitado a +/- 20% de la cotización oficial).
3. Badges de oportunidad en el catálogo P2P (`MarketP2PPage`): etiquetas comparativas con la cotización oficial (`Oferta` para precios menores y `Premium` para precios mayores).

## Clarifications

### Session 2026-11-23

- Q: ¿Cuál debe ser el precio unitario inicial sugerido al activar la modalidad de publicación en la comunidad P2P dentro del modal de venta? → A: Descuento promocional predeterminado del -10% sobre la cotización oficial para incentivar la venta rápida en la comunidad.
- Q: ¿Cómo debe formatearse la cifra porcentual exhibida en los badges de oportunidad dentro del catálogo P2P? → A: Redondeo al entero más cercano (ej. -10%, +15%) para mantener badges compactos y legibles.
- Q: Tras confirmar una Venta Inmediata Oficial exitosa en `SellTokenModal`, ¿cómo debe presentarse el comprobante de acreditación de créditos al usuario? → A: Presentar la pantalla de comprobante detallado dentro del diálogo (`SellModalSuccess`) y emitir en paralelo la notificación toast global.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Selector de modalidad en Modal de Venta y Venta Inmediata Oficial (Priority: P1)

Como usuario autenticado que posee tokens libres de un futbolista, quiero poder abrir el modal de venta y elegir la modalidad de *"Venta Inmediata Oficial"* para vender mis tokens directamente a la casa (superusuario) con un spread transparente y recibir mis créditos al instante sin tener que esperar a que otro usuario los compre en el mercado P2P.

**Why this priority**: Es la funcionalidad principal solicitada para dotar de liquidez inmediata al mercado, evitando que los usuarios queden bloqueados con tokens ilíquidos.

**Independent Test**: Iniciar sesión con un usuario que posea tokens libres; ingresar a `/market/sell` o `/portfolio`, abrir el modal de venta de un jugador, seleccionar la modalidad *"Venta Inmediata Oficial"*, verificar el desglose con descuento del 10%, ingresar una cantidad válida y confirmar. Verificar que la transacción se ejecuta contra el endpoint oficial, que se muestra el comprobante de éxito y que los créditos y tokens del usuario se actualizan inmediatamente en pantalla.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado que abre el modal de venta de un jugador visible con cotización oficial vigente de 100 centésimas (1.00 crédito), **When** visualiza el modal, **Then** se presentan dos modalidades claramente diferenciadas: *"Venta Inmediata Oficial"* y *"Publicación en Comunidad P2P"*, estando la venta oficial seleccionada por defecto.
2. **Given** la modalidad *"Venta Inmediata Oficial"* activa y una cotización de referencia de 100 centésimas, **When** el usuario ingresa 5 tokens para vender, **Then** el resumen dinámico expone en tiempo real:
   - Cantidad seleccionada: `5 tokens`.
   - Cotización oficial vigente: `1.00 crédito`.
   - Comisión/Spread de liquidez (10%): `0.10 créditos por token`.
   - Valor unitario neto: `0.90 créditos por token`.
   - Total de créditos netos a percibir: `4.50 créditos`.
3. **Given** un desglose válido de venta oficial inmediata, **When** el usuario hace clic en el botón *"Confirmar venta inmediata"*, **Then** se deshabilita el botón mostrando estado de carga (`isBusy`), se invoca la operación de venta oficial (`POST /api/orders/sell-official`) y, al completarse con éxito, se presenta la pantalla interna de comprobante detallado (`SellModalSuccess`) y se emite simultáneamente la notificación toast global con el resumen de la operación.
4. **Given** una venta oficial exitosa, **When** se cierra el diálogo de comprobante, **Then** el balance de créditos en la barra superior (`userCredits`), las tenencias en cartera (`userHoldings`) y el resumen del portfolio (`portfolioSummary`) se actualizan automáticamente reflejando el incremento de créditos y la reducción de tokens en custodia.
5. **Given** que el usuario hace clic en *"Cancelar"* o presiona Escape (fuera de estado de carga), **When** se cierra el modal, **Then** no se realiza ninguna petición de venta y los balances permanecen intactos.
6. **Given** una falla de red o rechazo de negocio del backend (ej: stock insuficiente o fondos insuficientes del superusuario), **When** se intenta la venta oficial, **Then** el modal muestra un mensaje de error descriptivo en un contenedor de alerta accesible sin cerrar el diálogo ni descartar la cantidad ingresada.

---

### User Story 2 — Selector de precio flexible en Publicación P2P (Priority: P1)

Como usuario autenticado que desea vender sus tokens a otros miembros de la comunidad, quiero seleccionar la modalidad de *"Publicación en Comunidad P2P"* y ajustar el precio unitario deseado mediante un control flexible (slider e input numérico) dentro de la banda permitida (-20% a +20% respecto a la cotización oficial) para definir si ofrezco un descuento atractivo o solicito una prima por mis activos.

**Why this priority**: Otorga flexibilidad de fijación de precios en el mercado secundario entre usuarios, complementando el flujo estático previo.

**Independent Test**: En el modal de venta, alternar a la modalidad *"Publicación en Comunidad P2P"*; verificar que se habilita el control de precio flexible inicializado en la cotización oficial (0% delta); mover el control a -15% y verificar el recalculo automático del precio unitario e importe total; probar valores fuera de rango (+25%) y verificar que el formulario bloquea la confirmación; confirmar una publicación dentro del rango y verificar que se crea con el precio acordado.

**Acceptance Scenarios**:

1. **Given** el modal de venta abierto, **When** el usuario conmuta a la pestaña o modalidad *"Publicación en Comunidad P2P"*, **Then** la vista oculta el desglose del spread oficial y despliega el selector de precio flexible P2P.
2. **Given** una cotización oficial de referencia de 100 centésimas (1.00 crédito), **When** se inicializa la modalidad P2P, **Then** el control de precio flexible se establece inicialmente con un descuento promocional predeterminado del -10% (90 centésimas / 0.90 créditos) exhibiendo el badge verde `"🏷️ -10% Oferta"` para incentivar la venta rápida en la comunidad.
3. **Given** el control de precio flexible P2P, **When** el usuario manipula el slider o ingresa un valor numérico, **Then** el rango de ajuste queda restringido estrictamente entre 80 centésimas (-20%) y 120 centésimas (+20%), actualizando sincronizadamente el valor en créditos, el delta porcentual y el importe total resultante ($Total = Cantidad \times Precio\_Unitario$).
4. **Given** una reducción de precio respecto a la cotización (ej: 85 centésimas frente a 100 oficiales), **When** se observa el control, **Then** se exhibe un badge visual verde: `"🏷️ -15% Oferta"`.
5. **Given** un aumento de precio respecto a la cotización (ej: 110 centésimas frente a 100 oficiales), **When** se observa el control, **Then** se exhibe un badge visual naranja/rojo: `"🔥 +10% Premium"`.
6. **Given** que el usuario intenta forzar un precio menor a -20% o mayor a +20%, **When** interactúa con el formulario, **Then** el control limita físicamente la entrada a los topes permitidos y el botón de confirmación permanece deshabilitado si el valor es inválido.
7. **Given** una cantidad y un precio unitario acordado válidos, **When** el usuario hace clic en *"Publicar en P2P"*, **Then** se solicita la creación de la orden (`POST /api/listings`) transmitiendo el `unitPrice` seleccionado, mostrando feedback `isBusy`, reservando los tokens en cartera y refrescando reactivamente las queries del mercado P2P y de órdenes personales (`myOrders`).

---

### User Story 3 — Badges de Oportunidad y Arbitraje en Catálogo P2P (Priority: P2)

Como inversor o comprador que explora el mercado comunitario (`/market/p2p`), quiero ver badges y etiquetas distintivas en cada publicación que comparen su precio con la cotización oficial del futbolista para identificar rápidamente gangas u ofertas de arbitraje y distinguir las publicaciones con sobreprecio.

**Why this priority**: Facilita la toma de decisiones económicas y el descubrimiento de oportunidades en el mercado secundario.

**Independent Test**: Navegar a la página `/market/p2p` con publicaciones existentes a distintos precios respecto a la cotización oficial; verificar que las publicaciones con precio inferior presentan badge verde de oferta con el porcentaje de ahorro exacto, las publicaciones con sobreprecio exhiben badge premium y las publicaciones a precio oficial no exhiben badge comparativo.

**Acceptance Scenarios**:

1. **Given** una publicación comunitaria cuyo precio por token es menor que la cotización oficial vigente del jugador (ej: cotización oficial 1.00 crédito, precio publicado 0.85 créditos), **When** se renderiza su tarjeta o fila en `/market/p2p`, **Then** se muestra un badge verde esmeralda destacado: `"🏷️ -15% Oferta"`.
2. **Given** una publicación comunitaria cuyo precio por token es mayor que la cotización oficial vigente del jugador (ej: cotización oficial 1.00 crédito, precio publicado 1.15 créditos), **When** se renderiza en `/market/p2p`, **Then** se muestra un badge naranja/rojo: `"🔥 +15% Premium"`.
3. **Given** una publicación comunitaria cuyo precio por token coincide exactamente con la cotización oficial vigente, **When** se visualiza en el catálogo P2P, **Then** el badge comparativo permanece oculto para evitar polución visual.
4. **Given** un futbolista cuya cotización oficial no está disponible momentáneamente, **When** se visualiza una publicación suya en el catálogo P2P, **Then** se muestra únicamente el precio publicado sin badge de oportunidad relativo.

---

### User Story 4 — Feedback, Manejo de Errores y Usabilidad Desktop (Priority: P2)

Como usuario que opera transacciones financieras en la plataforma, quiero contar con un sistema robusto de estados de carga, prevención de duplicaciones y mensajes de error comprensibles, dentro de una interfaz de escritorio accesible y fluida.

**Why this priority**: Garantiza la integridad de las operaciones del usuario, la coherencia visual y la prevención de dobles transacciones involuntarias.

**Independent Test**: Simular demoras de red y errores del servidor (500, 400); verificar que el botón de confirmación previene doble submit, que los errores se explican amigablemente y que la interfaz se ajusta perfectamente a anchos de pantalla desktop (≥ 1024px).

**Acceptance Scenarios**:

1. **Given** una transacción en proceso en cualquiera de las modalidades, **When** el usuario intenta interactuar, **Then** los botones de acción, selector de modalidad y controles de cantidad/precio quedan deshabilitados con spinner visible (`isBusy`) impidiendo envíos concurrentes.
2. **Given** una respuesta de error del servidor o falla de conectividad, **When** la operación es rechazada, **Then** se presenta un mensaje comprensible y no técnico que orienta al usuario sin exponer trazas internas ni cerrar el modal.
3. **Given** la interfaz de la aplicación en pantallas con resolución desktop (ancho $\ge$ 1024px), **When** se opera el modal de venta o el catálogo P2P, **Then** el diseño mantiene legibilidad, alineación y navegación por teclado accesible (WCAG 2.1 AA) sin desbordes horizontales.

---

## Edge Cases

Clasificación conforme al Principio II de la Constitución:

- **OBLIGATORIO — Validación estricta de banda de precio P2P**: El precio unitario de una publicación comunitaria debe encontrarse estrictamente entre el -20% y el +20% de la cotización oficial vigente. Cualquier valor menor a $0.80 \times P_{\text{oficial}}$ o mayor a $1.20 \times P_{\text{oficial}}$ debe ser bloqueado por la interfaz y deshabilitar el botón de publicación.
- **OBLIGATORIO — Spread oficial fijo e inmutable por el usuario**: En la modalidad de venta inmediata oficial, el descuento/spread del 10% es una constante de negocio fija aplicada sobre la cotización; el usuario no puede alterar dicho porcentaje.
- **OBLIGATORIO — Cotización oficial no disponible o no positiva**: Si la cotización oficial no se puede recuperar o no es positiva (`QUOTE_UNAVAILABLE` o precio $\le 0$), el modal debe informar claramente que no es posible cotizar en el momento, bloqueando la confirmación tanto en venta oficial como en P2P y habilitando un botón para reintentar.
- **OBLIGATORIO — Control de tenencia disponible vs. reservada**: La cantidad máxima a vender o publicar está acotada estrictamente por los tokens disponibles libres (`availableQuantity`), descontando aquellos que ya se encuentren reservados en publicaciones P2P activas. Si el usuario ingresa una cantidad superior a la disponible, el formulario debe señalar el error y bloquear el envío.
- **OBLIGATORIO — Prevención de dobles solicitudes involuntarias**: Durante la comunicación con el servidor (`isBusy`), se deshabilitan el botón de confirmación, los controles de entrada y el cierre por clic exterior o Escape.
- **OBLIGATORIO — Viewport Desktop exclusivo**: La interfaz está concebida exclusivamente para resoluciones de escritorio con ancho $\ge$ 1024px, prohibiendo variantes específicas para mobile o tablet (Constitución §Viewport).
- **RECOMENDADO — Persistencia temporal de la última modalidad seleccionada**: Si el usuario cierra el modal y lo reabre en la misma sesión para otro jugador, recordar la última modalidad elegida (venta oficial vs. P2P).
- **OPCIONAL — Atajos de teclado para incremento porcentual**: Teclas de flecha o botones rápidos para saltar a -20%, -10%, 0%, +10% y +20%.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El componente `SellTokenModal` MUST incorporar un selector interactivo que permita conmutar entre las dos modalidades operativas: *"Venta Inmediata Oficial"* y *"Publicación en Comunidad P2P"*.
- **FR-002**: En modalidad *"Venta Inmediata Oficial"*, el sistema MUST calcular y exhibir en tiempo real el desglose financiero: cantidad de tokens, cotización oficial de referencia, comisión del 10%, valor unitario neto y total de créditos netos a percibir.
- **FR-003**: En modalidad *"Venta Inmediata Oficial"*, la confirmación MUST ejecutar la transacción contra el endpoint de venta oficial (`POST /api/orders/sell-official`), enviando `playerId` y `quantity`.
- **FR-004**: Tras una venta oficial exitosa, el sistema MUST mostrar un comprobante detallado dentro del diálogo (`SellModalSuccess`), emitir en paralelo la notificación toast global e invalidar reactivamente las queries de balance y cartera (`userCredits`, `userHoldings`, `portfolioSummary`).
- **FR-005**: En modalidad *"Publicación en Comunidad P2P"*, el sistema MUST proveer un control interactivo de fijación de precio inicializado por defecto en un descuento promocional del -10% sobre la cotización oficial, permitiendo al usuario definir libremente el `unitPrice` dentro del margen de $[-20\%, +20\%]$.
- **FR-006**: En modalidad *"Publicación en Comunidad P2P"*, el sistema MUST exhibir en tiempo real el delta porcentual y el importe total resultante en créditos.
- **FR-007**: El sistema MUST bloquear la confirmación de una publicación P2P si el precio seleccionado se encuentra fuera del rango permitido $[-20\%, +20\%]$.
- **FR-008**: Al confirmar la publicación P2P, el sistema MUST invocar la creación de la orden (`POST /api/listings`) transmitiendo la cantidad y el `unitPrice` acordado, reflejando de inmediato la reserva de tokens en el portfolio del vendedor y actualizando las consultas del mercado comunitario.
- **FR-009**: En el catálogo del mercado comunitario (`MarketP2PPage`), cada tarjeta de publicación MUST comparar el precio ofertado contra la cotización oficial vigente del futbolista, calculando el delta relativo y formateando el porcentaje con redondeo al entero más cercano:
  - Mostrar badge verde esmeralda `🏷️ -X% Oferta` cuando el precio ofertado sea inferior a la cotización oficial.
  - Mostrar badge naranja/rojo `🔥 +X% Premium` cuando el precio ofertado sea superior a la cotización oficial.
  - Ocultar el badge comparativo cuando el precio coincida con la cotización oficial o cuando no haya cotización de referencia disponible.
- **FR-010**: El sistema MUST proveer retroalimentación visual de carga (`isBusy`) durante las operaciones asíncronas, impidiendo envíos concurrentes y cierres accidentales.
- **FR-011**: Los errores de validación, de stock o de respuesta del backend MUST comunicarse con mensajes descriptivos sin cerrar el diálogo ni descartar el formulario.
- **FR-012**: La interfaz MUST soportar exclusivamente resoluciones desktop con ancho mínimo de 1024px.

---

### Key Entities

- **Modal de Venta Dinámico (`SellTokenModal`)**: Componente modal encargado de presentar el estado del jugador, gestionar la selección de modalidad (oficial vs P2P), capturar cantidad y precio, y coordinar las mutaciones de venta y publicación.
- **Modalidad de Venta**: Modo operativo seleccionado por el usuario (`OFFICIAL_BUYBACK` para liquidez inmediata contra el superusuario con 10% de spread; `P2P_LISTING` para publicación comunitaria con precio flexible).
- **Selector de Precio Flexible P2P**: Control que combina slider e input numérico, sincronizado bidireccionalmente con la cotización oficial y restringido a la banda de $\pm 20\%$.
- **Badge de Oportunidad P2P**: Elemento visual informativo adjunto a las ofertas del catálogo P2P que categoriza el precio unitario respecto al valor oficial vigente.
- **Comprobante de Venta Oficial**: Componente de feedback final que presenta el resumen de la operación confirmada y los créditos netos acreditados.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las ventas oficiales ejecutadas con éxito refleja de inmediato la acreditación neta de créditos en el encabezado de la aplicación y la reducción correspondiente en la tenencia de tokens del usuario.
- **SC-002**: El 100% de los desgloses de venta oficial calcula con exactitud matemática el 10% de spread respecto a la cotización oficial vigente antes de confirmar la operación.
- **SC-003**: El 100% de las publicaciones P2P generadas mediante el selector flexible cumple con la restricción de encontrarse entre el -20% y el +20% de la cotización oficial vigente; ninguna publicación fuera de banda puede enviarse.
- **SC-004**: En el catálogo P2P, el 100% de las publicaciones con precio inferior a la cotización oficial exhiben de forma destacada el badge verde de oferta indicando el porcentaje de ahorro respectivo.
- **SC-005**: Un usuario autenticado puede completar una venta oficial inmediata en menos de 10 segundos a partir de la apertura del modal.
- **SC-006**: Ante fallas de red o rechazos de negocio, el 100% de las pantallas conserva los datos ingresados y expone un mensaje claro sin cerrar el diálogo.

---

## Assumptions

- Se asume que el backend expone el endpoint `POST /api/orders/sell-official` y acepta el campo `unitPrice` opcional en `POST /api/listings` conforme a los contratos establecidos en `SPEC-026`.
- Los importes monetarios se transfieren en centésimas de crédito y se presentan en la interfaz divididos por 100 con dos decimales exactos.
- El spread predeterminado para la venta oficial inmediata es del 10% según las especificaciones del Sprint 3 / REQ-S2-FEAT-ADD.
- La autenticación y la sesión del usuario están gestionadas por la infraestructura existente (`useAuthStore`).
- Issue de origen: [#95](https://github.com/YoelSLA/football-market/issues/95).

---

## Out of Scope

- Implementación de endpoints o lógica de base de datos en backend (corresponden a `SPEC-026`).
- Sistema de subastas, pujas en tiempo real o negociación por chat.
- Modificaciones al flujo de compra oficial en `/market/buy`.
- Soporte para viewports móviles o tablets (< 1024px).

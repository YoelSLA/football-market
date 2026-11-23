# Feature Specification: Venta oficial inmediata y precio flexible en el mercado P2P (REQ-S2-FEAT-ADD)

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Este documento describe exclusivamente el comportamiento funcional vigente. El historial se
delega a Git. Forma parte de la tríada permanente junto a plan.md y tasks.md. -->

**Feature Branch**: `feat/026-market-instant-buyback-and-p2p-pricing`

**Created**: 2026-11-22

**Status**: Draft

**Input**: Issue #94 — REQ-S2-FEAT-ADD. Backend para venta inmediata de tokens al superusuario con comisión configurable y publicaciones P2P con precio unitario opcional sujeto a una banda respecto de la cotización vigente.

## Clarifications

### Session 2026-11-22

- Q: Si aplicar la comisión produce un precio con fracciones de centésima, ¿cómo debe redondearse el precio neto por token? → A: Redondear al entero más cercano; las mitades se redondean hacia arriba.
- Q: ¿Qué rango debe aceptarse para la comisión configurable de la venta oficial? → A: De 10% a 99%, inclusive; el valor predeterminado es 10%.
- Q: ¿Qué ocurre si aplicar y redondear la comisión deja el precio neto por token en cero? → A: Se rechaza la venta oficial completa, sin transferencias ni registro financiero.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Vender tokens al superusuario (Priority: P1)

Como usuario autenticado, quiero vender tokens disponibles directamente al superusuario para obtener créditos sin esperar a que otro usuario compre una publicación.

**Why this priority**: Proporciona liquidez inmediata y es el flujo principal nuevo solicitado en el issue #94.

**Independent Test**: Un usuario con tokens libres realiza una venta válida y verifica el importe recibido, las tenencias resultantes de ambas partes y el registro de la operación.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con 10 tokens libres, una cotización de 100 centésimas por token y la comisión predeterminada del 10%, **When** vende 5 tokens al superusuario, **Then** recibe 450 centésimas, su tenencia disminuye en 5, la del superusuario aumenta en 5 y queda registrado el importe y las condiciones de la venta.
2. **Given** una solicitud con cantidad entera menor que 1 o mayor que 100, **When** el usuario intenta vender, **Then** la solicitud se rechaza con `INVALID_QUANTITY` y no cambia ningún saldo, tenencia ni registro financiero.
3. **Given** que la cantidad solicitada supera los tokens libres porque parte está reservada en publicaciones P2P activas, **When** el usuario intenta vender, **Then** la venta se rechaza con `INSUFFICIENT_STOCK` y no produce cambios.
4. **Given** una solicitud de venta oficial iniciada por el superusuario, **When** se procesa, **Then** se rechaza con `SUPERUSER_CANNOT_SELL` y no se modifica ningún recurso.
5. **Given** que no hay cotización vigente o el precio cotizado no es positivo, **When** se solicita una venta oficial, **Then** la operación se rechaza sin transferencias ni registro de una venta confirmada.
6. **Given** que ocurre un fallo durante la liquidación de una venta oficial después de iniciar una transferencia de créditos o tokens, **When** la operación termina, **Then** no se confirma ninguna transferencia, los saldos y tenencias de ambas partes permanecen sin cambios y no se registra una venta confirmada.
7. **Given** que aplicar y redondear la comisión produce un precio neto por token de cero, **When** el usuario intenta vender al superusuario, **Then** se rechaza la venta completa sin modificar saldos o tenencias ni registrar una operación confirmada.

---

### User Story 2 - Publicar tokens P2P a un precio elegido (Priority: P1)

Como usuario autenticado, quiero fijar el precio unitario de una publicación P2P dentro de un margen permitido para decidir entre vender con descuento o pedir una prima.

**Why this priority**: Da flexibilidad de precio a las ofertas entre usuarios y complementa la publicación existente.

**Independent Test**: Crear publicaciones con precio unitario dentro, fuera y sin indicar precio; comparar el precio guardado con el solicitado y con la cotización vigente.

**Acceptance Scenarios**:

1. **Given** una cotización vigente de 100 centésimas, **When** el usuario publica tokens a 85 centésimas por token, **Then** la publicación se crea con ese precio unitario y su importe total correspondiente.
2. **Given** una cotización vigente de 100 centésimas, **When** el usuario publica tokens a 125 centésimas por token, **Then** la solicitud se rechaza con `INVALID_LISTING_PRICE` y no crea ni reserva una publicación.
3. **Given** una cotización vigente de 100 centésimas, **When** el usuario omite el precio unitario, **Then** la publicación usa 100 centésimas por token.
4. **Given** una publicación creada con precio unitario acordado, **When** cambia la cotización vigente, **Then** el precio unitario y el importe total de esa publicación permanecen sin cambios.
5. **Given** una publicación P2P activa con precio unitario acordado, **When** otro usuario la compra, **Then** el comprador paga y el vendedor recibe el importe calculado con ese precio, independientemente de la cotización posterior.
6. **Given** que no hay cotización vigente o su precio no es positivo, **When** se intenta crear una publicación, **Then** la solicitud se rechaza y no se crea ninguna publicación.

---

### User Story 3 - Mantener la compra oficial existente (Priority: P2)

Como usuario del mercado, quiero que la compra oficial de tokens al superusuario conserve su comportamiento actual mientras se incorpora la venta oficial.

**Why this priority**: La nueva operación de salida no debe alterar el flujo existente de compra oficial.

**Independent Test**: Ejecutar una compra oficial válida conforme al comportamiento existente y comprobar que conserva sus condiciones y resultado, sin invocar las nuevas reglas de venta.

**Acceptance Scenarios**:

1. **Given** las condiciones válidas para una compra oficial, **When** el usuario compra tokens al superusuario, **Then** la compra sigue utilizando el flujo y las reglas existentes.

## Edge Cases

- La cantidad de venta oficial debe ser entera y estar entre 1 y 100 inclusive; cantidades ausentes, nulas, fraccionarias, cero, negativas o superiores se rechazan sin coerción ni cambios.
- La tenencia libre excluye los tokens reservados por publicaciones P2P activas; las reservas no pueden venderse mediante la operación oficial.
- Los límites de precio P2P del 80% y 120% de la cotización son inclusivos; cualquier precio exterior se rechaza.
- Una cotización ausente, cero o negativa no habilita una venta ni una publicación y no se reemplaza por un precio inventado.
- Si aplicar y redondear la comisión configurable deja el precio neto de una venta oficial en cero, se rechaza la venta completa sin efectos; no se sustituye el precio por otro valor.
- Una falla durante la liquidación de una venta oficial no puede dejar transferido solo el dinero o solo los tokens.
- Un rechazo de venta o de publicación no altera saldos, tenencias, reservas, publicaciones ni registros financieros.
- Las compras de publicaciones P2P existentes respetan el precio congelado, aunque haya cambiado la cotización desde la creación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir a un usuario autenticado vender tokens al superusuario mediante la operación de venta oficial.
- **FR-002**: El sistema MUST validar que la cantidad de una venta oficial sea un entero entre 1 y 100 inclusive.
- **FR-003**: El sistema MUST calcular el precio unitario neto de la venta oficial como la cotización vigente multiplicada por (1 − comisión configurada / 100); la comisión predeterminada MUST ser 10%. Si el redondeo definido en FR-017 produce un precio neto de cero, MUST rechazar la venta sin modificar recursos ni registrar una operación confirmada.
- **FR-004**: El sistema MUST validar que el vendedor disponga de la cantidad solicitada como tenencia libre, excluyendo tokens reservados por publicaciones P2P activas.
- **FR-005**: El sistema MUST liquidar atómicamente los tokens y créditos entre vendedor y superusuario y registrar la operación como parte de la misma operación atómica. Si no puede completar la liquidación y el registro por entero, MUST conservar sin cambios los saldos y tenencias de ambas partes y MUST NOT registrar una venta confirmada.
- **FR-006**: El sistema MUST mantener un registro inmutable por cada venta oficial confirmada, con vendedor, superusuario comprador, jugador, cantidad, cotización aplicada, comisión, precio neto e importe transferido.
- **FR-007**: El sistema MUST impedir que el superusuario inicie una venta oficial y comunicar el rechazo con `SUPERUSER_CANNOT_SELL`.
- **FR-008**: Al crear una publicación P2P, el sistema MUST admitir un precio unitario opcional; si se omite, MUST asignar la cotización vigente al momento de crearla.
- **FR-009**: El precio P2P indicado MUST ser un número entero positivo expresado en centésimas de crédito.
- **FR-010**: El sistema MUST aceptar el precio P2P indicado solo si está entre el 80% y el 120%, inclusive, de la cotización vigente al crear la publicación; en caso contrario MUST rechazarlo con `INVALID_LISTING_PRICE`.
- **FR-011**: El sistema MUST conservar en cada publicación el precio unitario acordado y el importe total calculado al crearla, sin modificar esos valores ante cambios posteriores en la cotización.
- **FR-012**: Las compras P2P MUST liquidarse según el precio unitario y el importe total conservados en la publicación, no según la cotización vigente al comprar.
- **FR-013**: Si la cotización necesaria para una venta oficial o una publicación no está disponible o no es positiva, el sistema MUST rechazar la operación sin alterar recursos ni crear registros de operaciones confirmadas.
- **FR-014**: El sistema MUST conservar el comportamiento actual de compra oficial de tokens al superusuario; modificar ese flujo no forma parte de esta funcionalidad.
- **FR-015**: La funcionalidad MUST limitarse al backend. Las vistas y controles frontend para la venta oficial o el precio P2P pertenecen a SPEC-027 y quedan excluidos.
- **FR-016**: Una venta oficial aceptada MUST devolver un comprobante que permita identificar las partes, cantidad, cotización, comisión aplicada, precio neto e importe transferido.
- **FR-017**: Si el precio neto por token calculado con la comisión contiene fracciones de centésima, el sistema MUST redondearlo al entero más cercano y redondear las mitades hacia arriba antes de calcular el importe transferido.
- **FR-018**: La comisión configurable MUST estar entre 10% y 99%, inclusive; cualquier valor fuera de ese rango MUST impedir la venta oficial sin alterar recursos ni registrar una venta confirmada.

### Key Entities

- **Venta oficial**: Operación en la que un usuario vende tokens al superusuario a la cotización vigente menos la comisión configurada; incluye cantidad, precio aplicado, importe y partes.
- **Publicación P2P**: Oferta de tokens entre usuarios que conserva la cantidad, el precio unitario y el importe total acordados al crearla.
- **Tenencia libre**: Tokens del usuario que no están reservados por publicaciones P2P activas y que pueden venderse o publicarse.
- **Cotización vigente**: Precio actual del token utilizado como referencia para calcular la venta oficial y validar o predeterminar el precio de una publicación nueva.
- **Comisión de venta oficial**: Porcentaje configurable, entre 10% y 99% inclusive, que se descuenta de la cotización para calcular el precio neto pagado al usuario; su valor predeterminado es 10%.
- **Registro financiero**: Evidencia de una operación de mercado confirmada que identifica las partes, los recursos y las condiciones económicas aplicadas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las ventas oficiales aceptadas refleja exactamente la cantidad transferida entre usuario y superusuario, y acredita al usuario el importe resultante de la comisión configurada.
- **SC-002**: El 100% de las ventas oficiales rechazadas conserva sin cambios los saldos, tenencias y registros financieros.
- **SC-003**: El 100% de las publicaciones aceptadas conserva el precio unitario pactado aunque cambie la cotización después de su creación.
- **SC-004**: El 100% de las compras P2P aceptadas calcula la liquidación con el precio congelado de la publicación.
- **SC-005**: Todas las solicitudes con cantidad o precio fuera de los límites especificados son rechazadas sin crear publicaciones ni alterar reservas o recursos.
- **SC-006**: El 100% de los comprobantes de venta oficial presenta las partes, la cantidad, la cotización, la comisión, el precio neto y el importe transferido, de modo que el usuario puede verificar las condiciones de su operación.

## Assumptions

- La comisión es una configuración del sistema entre 10% y 99%, inclusive; no se incorpora en este alcance una interfaz para que usuarios la cambien. Si no existe un valor configurado, se aplica el 10%.
- El precio se expresa en centésimas de crédito, según los criterios de aceptación del issue #94; los cálculos y comprobantes usan la misma unidad monetaria vigente en el mercado.
- El usuario vendedor debe disponer de créditos suficientes en la cuenta del superusuario para que la liquidación oficial pueda completarse; si no es posible completar ambos lados, se rechaza toda la operación.
- Se reutilizan la autenticación, las tenencias, las reservas, las publicaciones, la cotización vigente y el registro financiero existentes en el mercado.
- Issue de origen: [#94 — Venta inmediata al superusuario (buyback) y precio flexible en P2P](https://github.com/YoelSLA/football-market/issues/94). Requisito asociado: `REQ-S2-FEAT-ADD`.

## Out of Scope

- Vistas, formularios y controles frontend correspondientes a SPEC-027.
- Cambios al flujo de compra oficial existente.
- Subastas, negociación continua, comisiones adicionales o mecanismos nuevos de obtención de créditos.
- Cambiar o recalcular las cotizaciones de jugadores.

# Feature Specification: Historial personal de operaciones

**Feature Branch**: `feat/020-view-operation-history`

**Created**: 2026-10-24

**Status**: Ready for implementation

**Input**: User description: "Implementar la consulta backend del historial personal de operaciones confirmadas de compra y venta para el usuario autenticado, usando la identidad de la sesión y sin modificar datos financieros."

**Trazabilidad**: SPEC-020 materializa `REQ-S2-10` de la issue [#21](https://github.com/YoelSLA/football-market/issues/21), conforme a la asignación explícita registrada en `specs/assignments.md`. Satisface el historial de operaciones requerido por `tp/enunciado.md` §3.4 y §4. La issue #27 es consumidora futura y queda fuera de alcance.

## Clarifications

### Session 2026-10-24

- Q: Si el usuario autenticado figura simultáneamente como comprador y vendedor en una misma transacción, ¿cómo debe mostrarse esa operación? → A: Ese estado no forma parte del alcance de esta consulta: SPEC-010 y SPEC-011 garantizan que comprador y vendedor sean usuarios distintos.
- Q: ¿Qué se considera una transacción confirmada para esta consulta? → A: Toda transacción persistida en `MarketTransaction` ya está confirmada; no existen transacciones pendientes o sin confirmar en esa fuente.
- Q: ¿Es necesario conservar el criterio de usabilidad basado en una “validación representativa”? → A: No; ese criterio no es necesario para esta consulta backend y se elimina del alcance.
- Q: ¿El nombre del jugador debe conservar una versión histórica? → A: No; el nombre del jugador es inmutable y se obtiene del jugador relacionado porque nunca cambia.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el historial personal (Priority: P1)

Como usuario autenticado, quiero consultar todas mis operaciones financieras confirmadas para revisar cronológicamente mi actividad de compra y venta en el mercado.

**Why this priority**: Es el valor principal de la funcionalidad y completa la consulta de historial exigida para el portfolio del usuario.

**Independent Test**: Puede verificarse con un usuario que haya comprado y vendido tokens: la consulta devuelve ambas operaciones una sola vez, con todos sus datos registrados, interpretadas desde la perspectiva del usuario y ordenadas de la más reciente a la más antigua.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado que figura como comprador en operaciones confirmadas, **When** consulta su historial, **Then** cada una aparece como `BUY` con identificador de transacción, jugador, cantidad, precio unitario, importe total y fecha y hora de ejecución.
2. **Given** un usuario autenticado que figura como vendedor en operaciones confirmadas, **When** consulta su historial, **Then** cada una aparece como `SELL` con identificador de transacción, jugador, cantidad, precio unitario, importe total y fecha y hora de ejecución.
3. **Given** un usuario con operaciones de compra y venta confirmadas, **When** consulta su historial, **Then** obtiene una sola entrada por transacción en la que participó, sin crear un registro adicional para representar la venta.
4. **Given** varias operaciones del usuario, **When** consulta su historial, **Then** se presentan desde la ejecución más reciente hasta la más antigua y, cuando dos ejecuciones tienen la misma fecha y hora, el identificador de transacción determina un orden descendente estable.

---

### User Story 2 - Proteger la privacidad del historial (Priority: P1)

Como usuario autenticado, quiero que la consulta determine mi identidad desde mi sesión para impedir que otra persona seleccione mi cuenta y acceda a mis operaciones privadas.

**Why this priority**: La confidencialidad de la actividad financiera es inseparable del valor de la consulta y no puede postergarse respecto del flujo principal.

**Independent Test**: Puede verificarse con dos usuarios con operaciones diferentes: cada sesión obtiene únicamente sus propias operaciones y la consulta no ofrece ningún dato para elegir arbitrariamente otra identidad.

**Acceptance Scenarios**:

1. **Given** dos usuarios autenticados con historiales distintos, **When** cada uno realiza la consulta desde su sesión, **Then** cada respuesta contiene exclusivamente las transacciones donde el usuario de esa sesión fue comprador o vendedor.
2. **Given** una solicitud sin una sesión válida, **When** intenta consultar el historial personal, **Then** la solicitud se rechaza sin exponer operaciones financieras.
3. **Given** una sesión válida, **When** se solicita el historial personal, **Then** la operación no admite un identificador de usuario que permita reemplazar la identidad de la sesión.
4. **Given** una credencial válida cuyo subject no corresponde a un usuario persistido, **When** solicita el historial personal, **Then** la solicitud se rechaza con `401` sin revelar la existencia de usuarios ni operaciones.

---

### User Story 3 - Consultar un historial vacío (Priority: P2)

Como usuario autenticado sin operaciones confirmadas, quiero recibir un resultado vacío y exitoso para distinguir la ausencia de actividad de un error de consulta.

**Why this priority**: Es necesario para que usuarios nuevos o inactivos puedan usar la misma consulta sin tratamientos excepcionales.

**Independent Test**: Puede verificarse con un usuario autenticado que no figure como comprador ni vendedor en ninguna transacción confirmada; la consulta finaliza correctamente con una colección vacía.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado sin operaciones confirmadas, **When** consulta su historial, **Then** recibe una respuesta exitosa con una colección vacía.

### Edge Cases

- La consulta consume la garantía de SPEC-010 y SPEC-011 de que comprador y vendedor son usuarios distintos; cada participante visualiza la transacción una sola vez desde su único rol.
- Si varias transacciones comparten exactamente la misma fecha y hora de ejecución, se ordenan por identificador de transacción descendente para conservar un resultado determinista.
- Las transacciones en las que el usuario no es comprador ni vendedor se excluyen aunque involucren al mismo jugador que alguna operación propia.
- Los valores financieros históricos se muestran tal como quedaron registrados al ejecutar la operación; una cotización vigente posterior no altera sus precios ni importes.
- Una solicitud sin autenticación válida se rechaza sin revelar si existen operaciones para usuario alguno.
- Una credencial válida cuyo subject no corresponda a un usuario persistido se rechaza con `401` sin revelar la existencia de usuarios ni operaciones.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir que un usuario autenticado consulte su historial personal de operaciones financieras confirmadas.
- **FR-002**: La identidad consultada MUST obtenerse exclusivamente de la sesión autenticada y la consulta MUST NOT aceptar un identificador que permita seleccionar arbitrariamente otro usuario.
- **FR-003**: El historial MUST incluir exclusivamente las transacciones confirmadas en las que el usuario autenticado figure como comprador o como vendedor.
- **FR-004**: Cada transacción MUST aparecer exactamente una vez en el resultado; el sistema MUST NOT crear ni requerir un segundo registro para representar la perspectiva de venta.
- **FR-005**: Una transacción MUST presentarse como `BUY` cuando el usuario autenticado sea su comprador y como `SELL` cuando sea su vendedor.
- **FR-006**: Cada operación MUST incluir el identificador de la transacción, el tipo `BUY` o `SELL`, la identificación y el nombre del jugador, la cantidad de tokens, el precio unitario registrado, el importe total registrado y la fecha y hora de ejecución.
- **FR-007**: Los datos financieros de cada operación MUST reflejar los valores registrados al confirmarse la transacción y MUST NOT recalcularse con información vigente al momento de la consulta.
- **FR-008**: El resultado MUST ordenarse por fecha y hora de ejecución descendente y, a igualdad de ese valor, por identificador de transacción descendente.
- **FR-009**: Cuando el usuario no tenga operaciones confirmadas, el sistema MUST responder exitosamente con una colección vacía.
- **FR-010**: Una solicitud sin autenticación válida o con una credencial válida cuyo subject no corresponda a un usuario persistido MUST rechazarse con `401`, sin exponer operaciones ni información que permita inferir la existencia o el historial de otro usuario.
- **FR-011**: La consulta MUST ejecutarse como una operación de solo lectura y MUST NOT realizar operaciones de escritura.
- **FR-012**: La funcionalidad MUST preservar sin cambios observables los flujos vigentes de compra directa, publicación de ventas, cancelación de publicaciones y liquidación de publicaciones.
- **FR-013**: El historial MUST devolverse como una única colección completa; esta funcionalidad MUST NOT incorporar paginación, filtros avanzados, exportaciones, estadísticas ni agregaciones.
- **FR-014**: El historial MUST obtenerse de los registros persistidos existentes de `MarketTransaction` y MUST NOT introducir registros duplicados ni una fuente financiera paralela para representar las ventas.
- **FR-015**: La operación de consulta MUST ejecutarse bajo una garantía transaccional de solo lectura.

### Key Entities

- **Transacción de mercado (`MarketTransaction`)**: Registro financiero confirmado existente que constituye la fuente de verdad de una operación. Relaciona comprador, vendedor y jugador, e incluye identificador, cantidad de tokens, precio unitario, importe total y fecha y hora de ejecución.
- **Operación del historial**: Vista de una transacción desde la perspectiva del usuario autenticado. Añade la interpretación `BUY` o `SELL` sin crear ni duplicar la transacción original.
- **Usuario autenticado**: Persona identificada por la sesión vigente, cuya participación como comprador o vendedor delimita el historial visible.
- **Jugador**: Activo cuyos tokens fueron negociados; se identifica y nombra en cada operación mostrada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de los escenarios de aceptación, cada transacción propia confirmada aparece una sola vez y con los ocho datos obligatorios completos.
- **SC-002**: En el 100% de las consultas con operaciones, el resultado respeta el orden descendente por ejecución y el desempate descendente por identificador.
- **SC-003**: En el 100% de las verificaciones con usuarios distintos, ninguna respuesta contiene una transacción ajena ni permite seleccionar la identidad de otro usuario.
- **SC-004**: En el 100% de las consultas de usuarios sin operaciones, la consulta finaliza exitosamente con una colección vacía.

## Assumptions

- Toda transacción persistida en `MarketTransaction` representa una operación confirmada; esa fuente no contiene transacciones pendientes o sin confirmar. Esta funcionalidad no redefine sus reglas de confirmación ni auditoría.
- Los registros financieros existentes conservan los valores históricos necesarios para informar precio unitario, importe total y fecha y hora de ejecución.
- El nombre de un jugador es inmutable; por lo tanto, el nombre obtenido de la entidad relacionada coincide con el correspondiente a todas sus operaciones históricas.
- El historial personal se expone mediante una operación dedicada al usuario actual, coherente con la preferencia de un recurso personal como `/api/users/me/transactions`; el contrato definitivo se documentará durante la planificación.
- La colección completa, sin paginación, es suficiente para el volumen esperado en esta entrega; una futura necesidad de paginación requerirá una decisión de alcance separada.
- La interfaz frontend del historial corresponde a la issue #27 y no forma parte de esta SPEC.
- La feature depende de los registros producidos por las funcionalidades de compra (`REQ-S2-06`) y venta (`REQ-S2-07`), pero no modifica sus comportamientos.
- SPEC-010 y SPEC-011 son responsables de impedir que una misma persona sea compradora y vendedora en una transacción; SPEC-020 consume esa garantía y no vuelve a validarla.

## Out of Scope

- La interfaz frontend para visualizar el historial.
- Filtros avanzados, exportaciones, estadísticas, agregaciones y paginación.
- Cambios en las reglas contables del portfolio.
- Cambios en la persistencia o auditoría de compras y ventas, salvo que una fase posterior demuestre una carencia indispensable y el alcance se autorice explícitamente.
- Cambios en compra directa, publicación de ventas, cancelación de publicaciones o liquidación de publicaciones.
- El registro de auditoría de invocaciones a servicios previsto para Sprint 3.
- La definición y validación de SLA de rendimiento, que se abordará como requisito transversal en el próximo sprint.

# Feature Specification: Publicar ventas de tokens desde el frontend (REQ-S2-14-B)

**Feature Branch**: `feat/021-publish-token-sales-frontend` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `021-publish-token-sales-frontend`, sin el prefijo de tipo. La convención de ramas es normativa; ver la constitución §7. El `<scope>` va en inglés, con independencia del idioma de la descripción de origen. La convención de ramas es normativa; ver la constitución §7. El `<scope>` va en inglés, con independencia del idioma de la descripción de origen.

**Created**: 2026-10-26

**Status**: Ready for implementation

**Nota de estado (2026-10-26)**: Preparación global validada mediante ANALYZE vigente sin blockers (evidencia en Issue #43, comentario 6426397068). Este estado no autoriza por sí solo IMPLEMENT ni acredita implementación, revisión, testing o cierre; la autorización humana de implementación permanece separada y `Approved` requiere los gates de cierre.

**Input**: Vender tokens de jugadores que posee un usuario autenticado desde la interfaz web para ofrecerlos en el mercado secundario. La interfaz debe permitir preparar y confirmar una publicación, informar las condiciones aplicables y comunicar su resultado, sin implementar nuevamente las reglas de negocio del mercado.

## User Scenarios & Testing

### User Story 1 - Publicar una venta de tokens (Priority: P1)

Como usuario registrado, quiero publicar tokens que poseo de un jugador visible en el catálogo para ofrecérmelos a otros usuarios a un precio que queda fijado en el momento de la publicación.

**Why this priority**: Es la operación de salida del mercado exigida por el TP (`tp/enunciado.md` §3.3 y §4). Sin publicación no existe oferta que comprar, por lo que ninguna otra historia de esta feature puede demostrarse.

**Independent Test**: Con un usuario habilitado que posea tokens libres de un jugador visible, publicar una cantidad válida y comprobar que la publicación existe, que su precio queda congelado y que los tokens quedan reservados pero siguen siendo del vendedor. Puede demostrarse sin que ningún otro usuario compre nada y sin consultar el mercado.

**Acceptance Scenarios**:

| ID | Given | When | Then |
|---|---|---|---|
| CA-01 | Usuario autenticado con rol USER que posee 20 tokens libres de un jugador visible; precio vigente de 100 centésimas por token | Inicia la publicación de 5 tokens de ese jugador | Existe una publicación de 5 tokens a 100 centésimas por token e importe total 500. El vendedor conserva los 20 tokens: publicar no transfiere tokens ni créditos. No existe ninguna transacción de venta asociada a esa publicación. |
| CA-02 | Usuario con 20 tokens, 5 ya reservados por una publicación vigente | Intenta publicar 16 tokens del mismo jugador | Rechazo por saldo no reservado insuficiente: 5 + 16 superan los 20 disponibles. No hay publicación nueva ni cambios en las reservas existentes. |
| CA-03 | Usuario con 20 tokens libres | Publica cantidad nula, ausente, cero, negativa, decimal o superior a 100 | Rechazo de solicitud inválida, sin publicación creada ni cambios en reservas o tenencias. No hay coerción de tipos ni redondeo. |
| CA-04 | Usuario con 20 tokens libres | Publica 21 tokens | Rechazo por saldo no reservado insuficiente, sin publicación creada ni cambios. |
| CA-05 | Usuario con rol SUPERUSER | Intenta publicar tokens | 403 `SUPERUSER_CANNOT_SELL`, sin publicación creada ni cambios. La prohibición no depende de su saldo ni de su tenencia. |
| CA-06 | Usuario que no posee tokens del jugador, o cuya tenencia está totalmente reservada | Publica tokens de ese jugador | Rechazo por saldo no reservado insuficiente, sin publicación creada ni cambios. |
| CA-07 | Jugador inexistente | Intenta publicar tokens de ese jugador | 404 `PLAYER_NOT_FOUND`, sin publicación creada. |
| CA-08 | Jugador existente que no cumple el predicado de visibilidad del catálogo | Publica tokens de ese jugador | 404 `PLAYER_NOT_VISIBLE`, distinto del 404 `PLAYER_NOT_FOUND` de un identificador inexistente. No se crea publicación, no se reservan tokens, no hay transferencias y no se registra ninguna transacción. |
| CA-09 | Publicación creada a 100 centésimas por token; la cotización del jugador cambia a otro valor | Consulta la publicación existente | La publicación conserva 100 centésimas por token y su importe total original. La modificación de la cotización no altera publicaciones ya creadas. |
| CA-10 | Jugador sin cotización vigente disponible | Intenta publicar tokens de ese jugador | Rechazo por precio no disponible (409 `QUOTE_UNAVAILABLE`), sin publicación creada. El sistema no inventa un precio. |
| CA-18 | Cotización vigente con precio cero o negativo | Intenta publicar tokens de ese jugador | 409 `INVALID_QUOTE_PRICE`, sin publicación creada. |

### User Story 2 - Confirmación y resumen de publicación (Priority: P1)

Como usuario registrado, quiero revisar las condiciones de mi publicación antes de confirmar expresamente, para evitar envíos involuntarios.

**Why this priority**: Los criterios de aceptación CA-04 y CA-06 exigen que el usuario revise y confirme explícitamente antes de que la publicación sea solicitada al sistema.

**Independent Test**: El usuario completa todos los campos del formulario de publicación, visualiza el resumen y hace clic en "Confirmar". El sistema debe crear la publicación. Si el usuario hace clic en "Cancelar", no debe crearse ninguna publicación.

**Acceptance Scenarios**:

| ID | Given | When | Then |
|---|---|---|---|
| CA-12 | Usuario ha completado los datos del formulario (jugador y cantidad) | Visualiza el resumen de condiciones antes de confirmar | Se muestra un resumen comprensible con jugador, cantidad, precio unitario e importe total estimados a partir de la cotización consultada. La estimación no garantiza el precio aplicado ni muestra un vencimiento definitivo. No se crea publicación hasta la confirmación explícita. |
| CA-13 | Usuario está en el resumen de condiciones y hace clic en "Cancelar" | Cancela el proceso de publicación | No se genera ninguna publicación. El estado vuelve al catálogo sin modificaciones. |
| CA-14 | Usuario ha revisado el resumen y hace clic en "Confirmar" | Confirma la publicación explícitamente | Se solicita la publicación al backend. La publicación solo se solicita tras la confirmación expresa del usuario. |
| CA-15 | Durante el envío de la publicación | Se informa el estado de la solicitud y se evitan publicaciones duplicadas involuntarias | El usuario recibe retroalimentación sobre el estado (pending, success, error). No se permiten publicaciones duplicadas involuntarias. |

### User Story 3 - Estados de carga, validación y error (Priority: P2)

Como usuario, quiero que la interfaz me informe claramente sobre el estado de la solicitud, los errores de validación y las condiciones del sistema.

**Why this priority**: Los criterios de aceptación CA-07, CA-10 y RF-05 exigen comunicaciones claras sin exponer información sensible.

**Independent Test**: El usuario interactúa con el formulario de publicación en distintos escenarios (cantidad inválida, tenencia insuficiente, precio no disponible) y verifica que los mensajes de error son comprensibles y no revelan datos sensibles.

**Acceptance Scenarios**:

| ID | Given | When | Then |
|---|---|---|---|
| CA-16 | Solicitud con error de validación (cantidad inválida, formato incorrecto) | Envía la publicación | El sistema comunica el error de validación sin exponer información sensible. El usuario puede corregir y reenviar. |
| Durante el envío de la publicación | El sistema procesa la solicitud | Se informa el estado de la solicitud (cargando, éxito, error). Se evitan publicaciones duplicadas involuntarias. |
| RF-05 | Solicitud con cantidad inválida, tenencia insuficiente o condiciones no permitidas | Envía la publicación | Se comunica el rechazo con mensaje comprensible. No se crea publicación ni se alteran reservas. |
| RF-06 | Compra o validación fallida | Se recibe respuesta de error | El usuario recibe una explicación comprensible del rechazo, sin detalles técnicos sensibles. |

### User Story 4 - Actualización de tenencias y reflejo en portfolio (Priority: P2)

Como usuario, quiero que después de una publicación exitosa, la información visible refleje los tokens disponibles o reservados según las reglas del mercado.

**Why this priority**: Los criterios de aceptación CA-08 y RF-07 exigen que la interfaz se actualice correctamente después de una publicación exitosa.

**Independent Test**: Después de una publicación exitosa, el usuario ve actualizada la información de sus tenencias: tokens disponibles y reservados. El cambio es consistente con las reglas del mercado (SPEC-011 FR-006).

**Acceptance Scenarios**:

| ID | Given | When | Then |
|---|---|---|---|
| CA-17 | Publicación exitosa | El usuario consulta su portfolio o tenencias | La información visible refleja los tokens disponibles o reservados según las reglas del mercado. |
| RF-07 | Tras una publicación exitosa | El usuario ve su tenencia actualizada | La información visible refleja los tokens disponibles o reservados según las reglas del mercado. |

### User Story 5 - Accesibilidad y distintos tamaños de pantalla (Priority: P2)

Como usuario, quiero que el proceso de publicación sea accesible y se adapte a distintos tamaños de pantalla.

**Why this priority**: El criterio de aceptación CA-11 exige usabilidad en dispositivos de distintos tamaños y con interacciones accesibles.

**Independent Test**: El flujo se recorre con teclado y se revisa en mobile, tablet y desktop. Los controles usan elementos HTML semánticos, la navegación básica por teclado funciona y el modal reutiliza el comportamiento accesible existente.

**Acceptance Scenarios**:

| ID | Given | When | Then |
|---|---|---|---|
| CA-11 | Modal de publicación en viewport mobile, tablet o desktop, usando teclado | Recorre los controles del modal | Los controles usan elementos HTML semánticos, permiten navegación básica por teclado y reutilizan el comportamiento accesible ya provisto por el modal compartido. No hay scroll horizontal en los tamaños definidos en quickstart.md. |

## Edge Cases

Clasificación conforme a Constitution §2.2; las mejoras recomendadas no condicionan la aceptación.

- **OBLIGATORIO — publicación y reserva simultáneas:** la suma de reservas activas y vigentes de un usuario para un jugador nunca puede exceder sus tokens disponibles (AC-02, CL-17).
- **OBLIGATORIO — confirmación explícita:** la publicación solo se solicita tras la confirmación expresa del usuario (CA-14).
- **OBLIGATORIO — prevención de solicitudes duplicadas:** durante el envío se informa el estado y se evitan publicaciones duplicadas involuntarias (CA-15).
- **OBLIGATORIO — distinción publicación/venta:** publicar no implica transferir inmediatamente los tokens a un comprador (CA-09).
- **OBLIGATORIO — precio congelado:** el precio unitario se obtiene de la cotización vigente al momento de publicar y queda congelado en la publicación (FR-007, CL-02).
- **OBLIGATORIO — vencimiento:** la publicación vence 24 horas después de su creación; el estado `EXPIRED` se deriva de `now >= expiresAt` sin proceso programado (FR-010, CL-07).
- **OBLIGATORIO — superusuario prohibido:** el usuario con rol SUPERISTER recibe 403 `SUPERUSER_CANNOT_SELL` independientemente de su saldo y tenencia (FR-003, CL-08).
- **OBLIGATORIO — jugador no visible:** si el jugador existe pero no es visible, se rechaza con 404 `PLAYER_NOT_VISIBLE` sin reservar tokens ni crear publicación (FR-005, CL-11).
- **OBLIGATORIO — error sin revelación sensible:** los errores de validación o rechazos se comunican sin exponer información sensible (RF-05, CA-16).
- **RECOMENDADO — orden estricto entre compra y pérdida de visibilidad del jugador:** garantizar que una compra en curso no se complete si el jugador se desactiva simultáneamente es una mejora no bloqueante.

## Requirements

### Functional Requirements (Frontend)

- **FR-F01**: La interfaz MUST exponer un flujo controlado para que un usuario autenticado inicie la publicación de venta de tokens de un jugador de su tenencia, comunicando las condiciones al backend mediante `POST /api/orders/sell`.
- **FR-F02**: La interfaz MUST mostrar todos los jugadores con los que el usuario tenga tokens (aunque la tenencia esté totalmente reservada), indicando la cantidad disponible para ofrecer (tenencia menos reservas activas y vigentes) y la cantidad reservada. Debe incluirse también el caso de jugadores cuyas tenencias están totalmente reservadas por publicaciones vigentes, mostrando cero tokens disponibles.
- **FR-F03**: El usuario MUST poder indicar la cantidad a publicar mediante un selector editable con controles −/+ y accesos preestablecidos 1/2/5/Máx., limitando la cantidad a min(100, tokens disponibles). El precio unitario debe obtenerse automáticamente de la cotización vigente consultada y **no debe ser directamente editable** por el usuario; antes del POST, precio e importe son estimaciones. Al confirmar, la interfaz MUST revalidar la cotización mediante una nueva lectura de `GET /api/players/{id}/quotes/current` y, si difiere de la cotización cuya estimación revisó el usuario, informar el nuevo precio y requerir una nueva confirmación explícita antes del POST. El backend aplica autoritativamente el precio vigente al procesar el POST y devuelve los valores definitivos; el frontend no puede garantizar detectar cambios ocurridos entre esa lectura y el POST.
- **FR-F04**: Antes de enviar la publicación, la interfaz MUST mostrar un resumen comprensible de las condiciones, identificando precio e importe como estimados y MUST requerir confirmación expresa del usuario antes de solicitar la publicación al backend. El vencimiento definitivo se informa solo después de recibir la respuesta del backend.
- **FR-F05**: La interfaz MUST comunicar los rechazos por cantidad inválida, tenencia insuficiente o condiciones de publicación no permitidas, sin exponer información sensible.
- **FR-F06**: Tras una publicación exitosa, la interfaz MUST reflejar los tokens disponibles o reservados según las reglas del mercado (SPEC-011 FR-006, FR-007).
- **FR-F07**: El modal MUST usar controles HTML semánticos y permitir navegación básica por teclado. MUST reutilizar el comportamiento accesible ya provisto por el componente `shared/Modal`, sin imponer mecanismos de accesibilidad adicionales específicos de esta feature. El flujo MUST adaptarse a los tamaños de pantalla definidos en quickstart.md (CA-11).
- **FR-F08**: Durante el envío, la interfaz MUST informar el estado de la solicitud y MUST evitar publicaciones duplicadas involuntarias (CA-15).
- **FR-F09**: La interfaz MUST mostrar una confirmación de éxito dentro del modal al publicar tokens, displaying los datos definitivos: jugador, cantidad publicada, precio unitario congelado, importe total y vencimiento (24h). Esta confirmación debe permanecer visible hasta que el usuario navegue Away o refresque la vista.
- **FR-F10**: Ante errores de validación o de negocio, la interfaz MUST mantener el modal abierto, conservar los datos de entrada válidos y permitir al usuario corregir los campos inválidos sin perder su selección de jugador ni la cantidad parcialmente completada; debe evitarse cualquier reintento automático e inseguro de la publicación fallida.
- **FR-F11**: La interfaz MUST reutilizar los filtros de compra existentes y agregar disponibilidad de tokens y ordenamiento por tenencias, permitiendo al usuario filtrar y ordenar la lista de jugadores por su tenencia total, tokens disponibles y tokens reservados.
- **FR-F12**: La sincronización de tenencias MUST ocurrir al ingresar al modal de publicación, al regresar de una publicación fallida o exitosa, y después de publicar exitosamente. La interfaz debe reflejar siempre la tenencia actual (total, disponible y reservada) del usuario para el jugador seleccionado.

### Key Entities

- **Publicación**: Oferta de una cantidad de tokens de un jugador, hecha por un usuario, con precio unitario congelado al crearse, importe total, instante de creación, instante de vencimiento y estado. Es el objeto que las personas usuarias publican, consultan, cancelan y compran. No es un jugador, ni una orden de mercado, ni una solicitud de compra.
- **Reserva**: Inmovilización lógica de una cantidad de tokens mientras una publicación está efectivamente activa y vigente. Los tokens siguen perteneciendo al vendedor. Una publicación deja de reservar al venderse, al cancelarse o al vencer.
- **Estado efectivo de una publicación**: `ACTIVE`, `SOLD`, `CANCELLED` o `EXPIRED`, derivado de su estado y de su vencimiento. Es el estado que observan las consultas, los filtros y las decisiones de compra y cancelación.
- **Usuario**: Persona registrada con un rol, una cuenta de créditos y tenencias de tokens. Solo los usuarios con rol de usuario normal publican y compran publicaciones. El superusuario conserva su papel de vendedor de las compras directas y no participa en el mercado secundario.
- **Jugador**: Recurso del catálogo cuya existencia y visibilidad se validan al publicar; solo un jugador visible admite publicación. Una publicación puede seguir existiendo aunque su jugador deje de estar visible.
- **Tenencia**: Cantidad de tokens de un jugador propiedad de un usuario, de la que se descuenta lo reservado por publicaciones activas y vigentes.
- **Cotización vigente**: Precio actual de un token de un jugador. Se consulta al publicar y su resultado queda congelado en la publicación.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Publicar nunca mueve recursos: tras cualquier publicación, los saldos de créditos de todas las partes son los mismos y el vendedor conserva la totalidad de sus tokens, que solo quedan reservados.
- **SC-002**: La suma de tokens reservados por un usuario para un jugador nunca exceda su tenencia de ese jugador, ni siquiera con publicaciones simultáneas, múltiples publicaciones del mismo jugador o publicaciones vencidas que ya no reservan.
- **SC-003**: El precio de una publicación no cambia cuando cambia la cotización del jugador, y el importe total siempre es el producto exacto de la cantidad por el precio unitario congelado.
- **SC-004**: Una publicación deja de ser comprable, cancelable y reservante en su vencimiento sin que exista ningún proceso programado que modifique su estado almacenado.
- **SC-005**: El mercado público muestra exactamente las publicaciones efectivamente activas y vigentes de otros usuarios sobre jugadores visibles, y no muestra ninguna publicación propia, vencida, no activa ni de jugador no visible.
- **SC-006**: El propietario consulta y filtra por estado efectivo todas sus publicaciones, incluidas las de jugadores no visibles, y un tercero no obtiene detalle de publicaciones que no le son visibles.
- **SC-007**: Cada publicación se liquida a lo sumo una vez: con dos compras simultáneas, exactamente una se confirma y existe un único registro financiero asociado a esa publicación.
- **SC-008**: Cada compra de publicación transfiere exactamente la cantidad publicada y su importe exacto, deja a las partes con saldos y tenencias no negativos y marca la publicación como vendida.
- **SC-009**: Ninguna publicación rechazada ni ninguna compra rechazada altera saldos, tenencias, reservas ni estados de publicación, y no existe ningún registro financiero de una venta no confirmada.
- **SC-010**: Hay exactamente un registro financiero completo por cada venta de publicación, con referencia a esa publicación.
- **SC-011**: Cancelar una publicación vigente libera sus tokens reservados y permite al propietario volver a publicarlos, y ninguna publicación puede cancelarse más de una vez ni después de vendida, cancelada o vencida.
- **SC-012**: Las respuestas de aceptación y rechazo de las operaciones de publicación, consulta, cancelación y compra coinciden con el contrato documentado, incluidos campos, unidades monetarias, autenticación, errores y ausencia de revelaciones prohibidas.
- **SC-013**: Ninguna publicación puede crearse para un jugador no visible, y toda publicación creada mientras su jugador era visible se conserva sin cancelación ni eliminación aunque el jugador deje de estar visible, permaneciendo consultable por su propietario.
- **SC-014**: Ninguna publicación de un jugador no visible puede comprarse por un tercero, y el intento de hacerlo no revela que la publicación ni su jugador existen.

## Assumptions

- **Cuentas de créditos**: se asume que todo usuario registrado del mercado posee una cuenta de créditos, creada junto con su alta y nunca eliminada durante la operación normal (SPEC-010 FR-015). Sobre esa base, cada usuario puede recibir el pago de una venta.
- **Nivel de aislamiento**: se asume el nivel por defecto de la plataforma y del servidor de base de datos, sin aislamiento reforzado configurado de forma explícita. La integridad de las operaciones concurrentes no se apoya en esa suposición sino en bloqueos explícitos y en la revalidación bajo bloqueo; el supuesto se declara para que su cambio sea una decisión consciente.
- **Fuente de cotización**: se asume que la fuente de cotización vigente está disponible al publicar. Su ausencia se trata como rechazo definido (FR-007), no como un precio de respaldo.
- **Integración portfolio**: la consultas de portfolio personal (SPEC-014 / REQ-S2-09) están resueltas en backend y están disponibles para reflejar tenencias actualizadas después de publicación.
- **Mockups 07 y 08**: el diseño visual y flujo de la interfaz deben respetar los mockups `07-mercado-vender.png` y `08-mercado-vender-modal-abierto.png` como referencia de layout, componentes y interacciones.

## Out of Scope

- Implementar las reglas de reserva de tokens, fijación de precios o persistencia de publicaciones en el backend (ya definidas en SPEC-011).
- Ejecutar la compra de publicaciones por parte de otros usuarios.
- Implementar compras directas de tokens al superusuario.
- Gestionar la cancelación, modificación o administración general de órdenes de venta.
- Implementar la consulta del historial de operaciones o del portfolio personal (corresponde a otra feature).
- Gestionar la cancelación o modificación de órdenes de venta existentes.
- Cualquier flujo de creación de publicaciones distinto de `POST /api/orders/sell` a través del frontend.
- Cálculo, recálculo, persistencia y consulta de cotizaciones, y la evolución de la fórmula de valuación.
- Notificaciones persistentes o externas (por ejemplo, email o push) al vendedor o al comprador. Se incluye únicamente el toast transitorio de éxito de la operación de publicación (FR-F09; `notifySellSuccess`); no implica notificaciones persistentes ni comunicación fuera de la interfaz.
- Comisiones, spreads, tasas, recargas de créditos o cualquier mecanismo de creación de créditos distinto del saldo inicial de SPEC-010.
- Procesos programados de expiración, tareas batch o procesos automáticos que actualicen el estado almacenado de las publicaciones.

## Trazabilidad

- **Issue de origen**: `#43 — REQ-S2-14-B | Publicar ventas de tokens desde el frontend`
- **SPEC identificador**: `SPEC-021`
- **Proyecto**: Football Market
- **Milestone**: Sprint 2 — 03/11/2026
- **Contratos HTTP asociados**: `POST /api/orders/sell`, `GET /api/listings`, `GET /api/listings/{id}`, `POST /api/orders/buy`, `POST /api/listings/{id}/cancel`, `GET /api/listings/mine`, `GET /api/listings`
- **Reglas de negocio asociadas**: SPEC-011 FR-001 a FR-033, CL-01 a CL-18
- **Dependencias funcionales resueltas**: REQ-S2-07 (#50 — Venta de tokens de un jugador), REQ-S2-09 (#20 — Consultar portfolio personal)
- **Mockups de referencia**: `07-mercado-vender.png`, `08-mercado-vender-modal-abierto.png`

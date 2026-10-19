# Feature Specification: Vender tokens de un jugador (REQ-S2-07)

**Feature Branch**: `feat/011-sell-player-tokens` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `011-sell-player-tokens`, sin el prefijo de tipo. La rama fue creada y está activa, siguiendo el formato de Constitution §7 y el mecanismo de creación previsto por `AGENTS.md`.

**Created**: 2026-10-17

**Status**: Approved

**Estado operacional (2026-10-18)**: Feature y US01–US04 DONE. CONVERGE satisfactorio y
autorización final humana expresa sobre su resultado presentado, persistida en Trello como
`6ad50693400c55a3098e9a48`. 60/60 tareas y SDD Lifecycle 8/8; limitaciones aceptadas conservadas.
Evidencia: [convergence-report.md](convergence-report.md).

**Input**: Vender tokens de jugadores en un mercado secundario. Un usuario autenticado publica tokens que posee mediante una operación que congela el precio y reserva esos tokens sin transferirlos. Otro usuario compra esa publicación por su totalidad, lo que transfiere tokens y créditos de forma atómica y deja registro financiero trazable a la publicación. La compra directa contra el superusuario definida en SPEC-010 se conserva sin cambios.

## User Scenarios & Testing

### User Story 1 - Publicar una venta de tokens (Priority: P1)

Como usuario registrado, quiero publicar tokens que poseo de un jugador visible en el catálogo para ofrecerlos a otros usuarios a un precio que queda fijado en el momento de la publicación.

**Why this priority**: Es la operación de salida del mercado exigida por el TP (`tp/enunciado.md` §3.3 y §4). Sin publicación no existe oferta que comprar, por lo que ninguna otra historia de esta feature puede demostrarse.

**Independent Test**: Con un usuario habilitado que posea tokens libres de un jugador visible, publicar una cantidad válida y comprobar que la publicación existe, que su precio queda congelado y que los tokens quedan reservados pero siguen siendo del vendedor. Puede demostrarse sin que ningún otro usuario compre nada y sin consultar el mercado.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC01 | Usuario con rol USER que posee 20 tokens libres de un jugador visible; precio vigente de 100 centésimas por token | Publica 5 tokens de ese jugador | Existe una publicación de 5 tokens a 100 centésimas por token e importe total 500. El vendedor conserva los 20 tokens: publicar no transfiere tokens ni créditos. No existe ninguna transacción de venta asociada a esa publicación. |
| AC02 | Usuario con 20 tokens, 5 ya reservados por una publicación vigente | Publica 16 tokens del mismo jugador | Rechazo por saldo no reservado insuficiente: 5 + 16 superan los 20 disponibles. No hay publicación nueva ni cambios en las reservas existentes. |
| AC03 | Usuario con 20 tokens libres | Publica cantidad nula, ausente, cero, negativa, decimal o superior a 100 | Rechazo de solicitud inválida, sin publicación creada ni cambios en reservas o tenencias. No hay coerción de tipos ni redondeo. |
| AC04 | Usuario con 20 tokens libres | Publica 21 tokens | Rechazo por saldo no reservado insuficiente, sin publicación creada ni cambios. |
| AC05 | Usuario con rol SUPERUSER | Intenta publicar tokens | 403 `SUPERUSER_CANNOT_SELL`, sin publicación creada ni cambios. La prohibición no depende de su saldo ni de su tenencia. |
| AC06 | Usuario que no posee tokens del jugador, o cuya tenencia está totalmente reservada | Publica tokens de ese jugador | Rechazo por saldo no reservado insuficiente, sin publicación creada ni cambios. |
| AC07 | Jugador inexistente | Publica tokens de ese jugador | 404 `PLAYER_NOT_FOUND`, sin publicación creada. |
| AC08 | Jugador existente que no cumple el predicado de visibilidad del catálogo | Publica tokens de ese jugador | 404 `PLAYER_NOT_VISIBLE`, distinto del 404 `PLAYER_NOT_FOUND` de un identificador inexistente. No se crea publicación, no se reservan tokens, no hay transferencias y no se registra ninguna transacción. |
| AC09 | Publicación creada a 100 centésimas por token; la cotización del jugador cambia a otro valor | Se consulta la publicación existente | La publicación conserva 100 centésimas por token y su importe total original. La modificación de la cotización no altera publicaciones ya creadas. |
| AC10 | Jugador sin cotización vigente disponible | Publica tokens de ese jugador | Rechazo por precio no disponible, sin publicación creada. El sistema no inventa un precio. |
| AC11 | Cotización vigente con precio cero o negativo | Publica tokens de ese jugador | 409 `INVALID_QUOTE_PRICE`, sin publicación creada. |
| AC12 | Usuario con 20 tokens libres de un jugador visible; ninguna publicación suya vigente | Publica 5 tokens y, de inmediato, antes de que expire, publica otros 5 del mismo jugador | Ambas publicaciones coexisten y las reservas activas suman 10, sin exceder los 20 disponibles. |
| AC13 | Usuario con 10 tokens libres de un jugador visible, sin reservas previas | Solicita dos publicaciones de 6 tokens del mismo jugador de forma simultánea | Solo una publicación se crea; la otra se rechaza por saldo no reservado insuficiente. En ningún momento las reservas activas superan los 10 tokens disponibles. |
| AC14 | Publicación creada correctamente | Se procesa la publicación | No se altera ningún saldo de créditos: publicar no debita ni acredita a nadie. |

En todo rechazo se conservan las reservas vigentes y no se crea publicación.

---

### User Story 2 - Comprar la publicación de otro usuario (Priority: P1)

Como usuario registrado, quiero comprar la publicación completa de otro usuario para adquirir tokens a un precio que ya conozco, y quiero que esa compra se ejecute una sola vez aunque compita con otro comprador.

**Why this priority**: Es la operación que liquida la venta y materializa el mercado secundario. Sin ella la publicación no tiene contraparte y no hay transferencia de valor.

**Independent Test**: Con dos usuarios autenticados, uno con una publicación vigente y el otro con créditos suficientes, comprar esa publicación y comprobar que los tokens se transfieren, que los créditos se acreditan al vendedor, que la publicación queda vendida y que existe exactamente un registro financiero asociado a ella. Puede demostrarse sin consultar el mercado ni usar la compra directa al superusuario.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC15 | Publicación `ACTIVE` y vigente de 5 tokens a 100 centésimas, vendedor con saldo 0; comprador con saldo suficiente | El comprador compra esa publicación | El comprador recibe 5 tokens, su saldo disminuye en 500 centésimas y el vendedor recibe 500 centésimas. La publicación queda en estado `SOLD` con su momento de cierre registrado. Existe exactamente un registro financiero que referencia esa publicación, con comprador, vendedor, jugador, cantidad, precio aplicado e importe. |
| AC16 | Publicación `ACTIVE` y vigente | El comprador envía la solicitud con identificador de publicación y, además, datos de jugador, cantidad o precio | El backend determina jugador, cantidad, precio unitario y vendedor exclusivamente desde la publicación. Los datos suministrados no alteran la operación ni el importe liquidado. |
| AC17 | Comprador con saldo inferior al importe total de la publicación vigente | Intenta comprarla | 409 `INSUFFICIENT_CREDITS`, sin cambios en saldos, en tenencias ni en el estado de la publicación. Ningún saldo queda negativo. |
| AC18 | Publicación en estado `SOLD` | Un tercero intenta comprarla | 409 `LISTING_NOT_AVAILABLE`, sin movimientos. No se diferencia públicamente el motivo entre `SOLD`, `CANCELLED` y `EXPIRED`. |
| AC19 | Publicación en estado `CANCELLED` | Un tercero intenta comprarla | 409 `LISTING_NOT_AVAILABLE`, sin movimientos. |
| AC20 | Publicación vencida y efectivamente expirada | Un tercero intenta comprarla | 409 `LISTING_NOT_AVAILABLE`, sin movimientos. |
| AC21 | Publicación `ACTIVE` y vigente cuyo vendedor no tiene una cuenta de créditos válida para recibir el pago | Un comprador con saldo suficiente intenta comprarla | 409 `LISTING_NOT_AVAILABLE`, sin transferencias de tokens ni de créditos, sin registro de transacción y sin marcar la publicación como `SOLD`. El rechazo no revela que el motivo sea la ausencia de cuenta del vendedor ni menciona al vendedor. |
| AC22 | Publicación `ACTIVE` y vigente del propio comprador | El comprador intenta comprarla | 403 `CANNOT_BUY_OWN_LISTING`, sin movimientos ni cambios de estado. |
| AC23 | Publicación inexistente | Un usuario intenta comprarla | 404 `LISTING_NOT_FOUND`, sin movimientos. |
| AC24 | Dos compradores autenticados, ambos con saldo suficiente, y una publicación `ACTIVE` y vigente | Ambos intentan comprarla de forma simultánea | Solo una compra se confirma y la otra recibe 409 `LISTING_NOT_AVAILABLE`. No hay doble transferencia, no hay saldo negativo y existe un único registro financiero por la publicación. |
| AC25 | Usuario con rol SUPERUSER | Intenta comprar una publicación | 403, sin movimientos. El superusuario no participa como comprador en el mercado secundario. |
| AC26 | Publicación `ACTIVE` y vigente, comprador con saldo suficiente | Se procesa la compra | Los tokens son transferidos al comprador y el importe se acredita al vendedor en una misma operación: no existe estado intermedio observable en el que los tokens se hayan movido sin los créditos, ni en el que los créditos se hayan movido sin los tokens. |
| AC27 | Compra que cumple todas las condiciones | Se procesa y no puede completarse el registro financiero | La operación se revierte por completo: saldos, tenencias y estado de la publicación quedan intactos y no queda registro de una venta no confirmada. |
| AC28 | Publicación `ACTIVE` y vigente | El comprador repite la solicitud de compra por la misma publicación | La segunda solicitud recibe 409 `LISTING_NOT_AVAILABLE` porque la publicación ya quedó `SOLD`. No se genera una segunda compra ni un segundo registro financiero. |
| AC29 | Compra directa de tokens al superusuario, según SPEC-010 | Un usuario compra con datos de jugador y cantidad, sin identificar publicación | Se conserva íntegramente el comportamiento de SPEC-010 contra el superusuario, y la compra no se asocia a ninguna publicación. |
| AC30 | Publicación `ACTIVE` y vigente cuyo vendedor, de forma excepcional, ya no tiene tokens suficientes para respaldar la cantidad publicada | Un comprador con saldo suficiente intenta comprarla | 409 `INSUFFICIENT_STOCK`, sin transferencias de tokens ni de créditos y sin registro de transacción. La publicación no se marca como `SOLD`, conserva su registro y su estado no cambia como consecuencia automática de este rechazo. Las operaciones normales respetan las reservas de las publicaciones activas y vigentes, por lo que esta situación no se produce en los flujos ordinarios. |
| AC31 | Publicación `ACTIVE` y vigente de otro usuario cuyo jugador ya no cumple el predicado de visibilidad del catálogo | Un tercero que conoce el identificador de la publicación intenta comprarla | 404 `LISTING_NOT_FOUND`, igual que si la publicación no existiera, sin revelar que sí existe ni la de su jugador. No hay transferencias ni registro de transacción, y la publicación conserva su estado. El mismo identificador sigue siendo accesible para su propietario. |

---

### User Story 3 - Consultar las publicaciones disponibles (Priority: P2)

Como usuario registrado, quiero ver las publicaciones de otros usuarios que puedo comprar y consultar el detalle de una publicación concreta para decidir si la compro.

**Why this priority**: Permite descubrir la oferta. La compra de US2 es demostrable sin esta historia, pero sin ella el mercado no es usable por un usuario real.

**Independent Test**: Con dos usuarios, uno con publicaciones en distintos estados y jugadores visibles e invisibles, listar el mercado, listar las publicaciones propias y consultar el detalle de publicaciones concretas. Puede demostrarse sin realizar ninguna compra.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC32 | Publicaciones `ACTIVE` y vigentes de otros usuarios sobre jugadores visibles | El usuario lista el mercado | Se devuelven esas publicaciones y ninguna más: se excluyen las propias, las vencidas, las no activas y las de jugadores no visibles. |
| AC33 | Usuario con publicaciones en distintos estados y sobre un jugador que dejó de estar visible | Lista el mercado | Ninguna de sus publicaciones aparece en el mercado. |
| AC34 | Usuario con publicaciones `ACTIVE` vigentes, `SOLD`, `CANCELLED` y vencidas | Lista sus publicaciones sin filtro | Se devuelven todas sus publicaciones, en cualquier estado y sobre jugadores visibles o no. |
| AC35 | Usuario con publicaciones en distintos estados efectivos, incluida una vencida | Lista sus publicaciones filtrando por un estado | Se devuelven solo las que se encuentran en ese estado efectivo. Sin filtro se devuelven todas. El filtro decide únicamente por el estado efectivo de la publicación y no considera la visibilidad del jugador. |
| AC36 | Usuario con una publicación `CANCELLED` o `SOLD`, o vencida, de un jugador ya no visible | El propietario consulta el detalle de esa publicación | El propietario la obtiene y puede ver su estado e importe. |
| AC37 | Publicación `SOLD`, `CANCELLED` o vencida de otro usuario | Un tercero consulta su detalle | 404 `LISTING_NOT_FOUND`, sin revelar su existencia ni sus datos. |
| AC38 | Publicación `ACTIVE` y vigente de otro usuario sobre un jugador visible | Un tercero consulta su detalle | El tercero la obtiene con su jugador, cantidad, precio e importe. Una publicación activa y vigente de un jugador no visible no es accesible para terceros, que reciben 404 `LISTING_NOT_FOUND`. |
| AC39 | Publicación inexistente | Un usuario consulta su detalle | 404 `LISTING_NOT_FOUND`. |
| AC40 | Publicación de un jugador que deja de estar visible | Transcurre el tiempo | La publicación no se cancela ni se elimina: deja de aparecer en el mercado y su propietario conserva acceso a ella en su historial. |
| AC41 | Usuario con una publicación vencida de un jugador que ya no es visible | Lista sus publicaciones filtrando por `EXPIRED` | La publicación aparece en el resultado, porque el filtro se aplica solo al estado efectivo y no a la visibilidad del jugador. El mismo usuario sí la sigue viendo en el detalle. |
| AC42 | Usuario autenticado | Lista sus publicaciones con el filtro escrito en minúsculas, en forma mixta o con un valor que no corresponde a ningún estado | Solicitud inválida. Solo se aceptan las formas exactas `ACTIVE`, `SOLD`, `CANCELLED` y `EXPIRED`; no hay conversión de mayúsculas, comparación sin distinción de caja ni tolerancia a valores desconocidos. No se devuelve ninguna publicación. |

---

### User Story 4 - Cancelar mi publicación (Priority: P2)

Como usuario registrado, quiero cancelar una publicación que ya no quiero ofrecer para liberar los tokens reservados y no seguir esperando comprador.

**Why this priority**: Permite recuperar los tokens reservados cuando el usuario cambia de opinión. Es una salida de la reserva, no un requisito para que el mercado funcione.

**Independent Test**: Con un usuario que tiene una publicación vigente y tokens libres restantes, cancelarla y comprobar que los tokens deja de estar reservados y que puede volver a publicarlos. Puede demostrarse sin que ningún otro usuario consulte ni compre.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC43 | Publicación `ACTIVE` y vigente del usuario, con tokens reservados | El propietario la cancela | La publicación pasa a `CANCELLED` con su momento de cierre registrado, deja de reservar tokens y se conserva en el historial. Los tokens vuelven a estar disponibles para nuevas publicaciones. |
| AC44 | Publicación `ACTIVE` y vigente de otro usuario | Un usuario que no es el propietario intenta cancelarla | Rechazo por falta de autorización, sin cambios en la publicación ni en las reservas. El propietario conserva el control exclusivo de la cancelación. |
| AC45 | Publicación en estado `SOLD` | El propietario intenta cancelarla | Rechazo, sin cambios. |
| AC46 | Publicación en estado `CANCELLED` | El propietario intenta cancelarla de nuevo | Rechazo, sin cambios. |
| AC47 | Publicación vencida y efectivamente expirada | El propietario intenta cancelarla | Rechazo, sin cambios. |
| AC48 | Publicación vigente del usuario con 5 tokens reservados de 20 poseídos | Cancela la publicación | Las reservas activas del usuario para ese jugador pasan a 0 y puede publicar los 20 tokens en una o varias publicaciones, siempre dentro del límite por publicación. |

### Edge Cases

Clasificación conforme a Constitution §2.2; las mejoras recomendadas no condicionan la aceptación.

- **OBLIGATORIO — publicación y reserva simultáneas:** la suma de reservas activas y vigentes de un usuario para un jugador nunca puede exceder sus tokens disponibles (AC13).
- **OBLIGATORIO — compra y cancelación simultáneas sobre la misma publicación:** solo una debe confirmarse y la otra debe rechazarse, sin movimientos parciales ni estado intermedio observable.
- **OBLIGATORIO — dos compradores simultáneos:** una sola liquidación por publicación (AC23).
- **OBLIGATORIO — fallo del registro financiero de la venta:** la operación se revierte por completo, sin registro de venta no confirmada (AC26).
- **OBLIGATORIO — publicación con tenencia del vendedor reducida por otra vía:** si en el momento de comprar la tenencia del vendedor no cubre la cantidad publicada, la compra se rechaza con 409 `INSUFFICIENT_STOCK` sin mover recursos, sin registrar transacción y sin marcar la publicación como `SOLD`; la publicación conserva su registro y su estado (AC30, CL-13, FR-021). Las operaciones normales respetan las reservas de las publicaciones activas y vigentes, de modo que esta situación no se produce en los flujos ordinarios.
- **OBLIGATORIO — Determinación de expiración:** `EXPIRED` es un estado efectivo derivado del vencimiento, sin proceso programado ni actualización automática del estado almacenado. Ninguna respuesta ni filtro puede tratar como vigente una publicación vencida. La frontera es `now >= expiresAt` para vencido y `now < expiresAt` para vigente.
- **OBLIGATORIO — vencimiento durante una compra en curso:** el punto decisivo es la única validación de vigencia realizada con la publicación y las tenencias necesarias ya bloqueadas, en ese orden. El instante se obtiene después de adquirir esos bloqueos, nunca antes de esperar por las tenencias. Si en ese instante `now >= expiresAt`, la compra se rechaza con 409 `LISTING_NOT_AVAILABLE` y no produce movimientos. Si la validación se supera, la compra puede confirmarse aunque el reloj alcance `expiresAt` durante la liquidación, siempre que las demás precondiciones se mantengan y la transacción confirme. Los bloqueos se conservan hasta el commit o rollback, de modo que otra publicación no puede reutilizar los tokens comprometidos mientras la compra está en curso. **No** se realiza una nueva validación de vencimiento al final, porque contradiría esta regla.
- **OBLIGATORIO — pérdida de visibilidad posterior a la publicación:** una publicación creada mientras su jugador era visible MUST NOT cancelarse ni eliminarse por el hecho de que el jugador deje de estar visible; deja de aparecer en el mercado y su propietario conserva el acceso (AC08, AC31, AC33, AC34, AC36, AC40).
- **RECOMENDADO — orden estricto entre compra y pérdida de visibilidad del jugador:** garantizar que una compra en curso no se complete si el jugador se desactiva simultáneamente es una mejora no bloqueante.
- **RECOMENDADO — orden de presentación y volumen de los listados:** el orden por precio o antigüedad y la paginación son mejoras de experiencia no bloqueantes.
- **RECOMENDADO — contenido de la notificación de compra al vendedor:** no se implementa ninguna notificación.

Stock o créditos insuficientes, entradas inválidas, jugador inexistente, jugador no visible, publicación inexistente, publicación propia, publicación no disponible y falta de autorización son validaciones obligatorias ya cubiertas por AC03–AC07, AC09–AC11, AC17–AC22, AC31, AC42 y AC44; no requieren escenarios borde adicionales.

## Requirements

### Functional Requirements

Los requisitos siguientes son obligatorios para las historias de esta feature.

- **FR-001**: El sistema MUST exponer `POST /api/orders/sell` para que un usuario autenticado publique tokens que posee. La publicación MUST NOT transferirse tokens ni créditos, MUST NOT modificar ningún saldo de créditos y MUST NOT generar ninguna transacción de venta.
- **FR-002**: El sistema MUST aceptar solicitudes de publicación solo de usuarios autenticados cuya identidad reconoce. El solicitante MUST NOT poder elegir otra identidad de vendedor.
- **FR-003**: El sistema MUST autorizar la publicación solo a usuarios con rol vigente de usuario normal según la persistencia. El superusuario recibe 403 `SUPERUSER_CANNOT_SELL`, con independencia de su saldo y de su tenencia.
- **FR-004**: La solicitud de publicación MUST incluir el identificador del jugador y una cantidad. `quantity` MUST ser una cantidad entera entre 1 y 100 inclusive. Si está ausente, es nula, no es un entero, es decimal, no es representable como entero o queda fuera del rango 1–100, el sistema MUST rechazarla sin crear publicación ni alterar reservas. No se permiten coerción de tipos, redondeo ni truncamiento.

  **Fundamento del rango 1–100.** El límite superior coincide con el máximo de tokens en circulación por jugador: el enunciado fija 100 tokens emitidos por jugador (`tp/enunciado.md` §3.3), SPEC-010 establece una emisión inicial única de 100 tokens por jugador sin reposición (FR-016) y la base de datos lo fija con una restricción de igualdad, de modo que ninguna publicación puede ofrecer más tokens de un jugador de los que existen. El límite inferior responde a que una publicación debe contener una cantidad positiva de tokens.

  Este fundamento describe por qué existe el rango y **no** introduce reglas nuevas: no habilita emitir tokens, modificar el suministro, reponer inventario ni establecer restricciones adicionales. La suma de las publicaciones de un mismo vendedor sigue limitada por su disponibilidad real —tenencia menos reservas activas y vigentes— según FR-006, de modo que un vendedor puede mantener varias publicaciones simultáneas del mismo jugador siempre que esa suma no exceda sus tokens disponibles.""
- **FR-005**: El sistema MUST comprobar que el jugador exista y esté visible en el catálogo al validar la publicación, conservando la distinción entre ambas situaciones. MUST rechazar con 404 `PLAYER_NOT_FOUND` cuando el identificador no corresponda a ningún jugador, y MUST rechazar con 404 `PLAYER_NOT_VISIBLE` cuando el jugador exista pero no cumpla el predicado de visibilidad vigente del catálogo. Ninguno de los dos códigos revela la existencia del jugador no visible más allá de lo que el propio contrato ya admite. Ante cualquiera de los dos rechazos MUST NOT crear publicación, MUST NOT reservar tokens, MUST NOT producir transferencias y MUST NOT generar registro de transacción.
- **FR-006**: La disponibilidad para publicar MUST ser la tenencia del vendedor menos la suma de las cantidades reservadas por sus publicaciones de ese jugador que estén efectivamente activas y vigentes. El sistema MUST rechazar íntegramente la publicación con 409 `INSUFFICIENT_STOCK` cuando la cantidad solicitada exceda esa disponibilidad, sin revelar la tenencia ni la cantidad reservada ni modificar las reservas existentes.
- **FR-007**: El precio unitario MUST obtenerse de la fuente de cotización vigente del sistema en el momento de publicar y MUST quedar congelado en la publicación. El importe total MUST ser el producto de la cantidad por ese precio unitario, y ambos MUST expresarse en la misma representación monetaria usada por SPEC-010. Si no hay precio vigente disponible, el sistema MUST rechazar con 409 `QUOTE_UNAVAILABLE`; si el precio no es positivo, con 409 `INVALID_QUOTE_PRICE`. El sistema MUST NOT inventar ni sustituir un precio ausente.
- **FR-008**: Publicar MUST NOT alterar ninguna cotización vigente. Una modificación posterior de la cotización MUST NOT alterar el precio, el importe ni los datos de una publicación ya creada.
- **FR-009**: Cada publicación MUST registrar su jugador, cantidad, precio unitario congelado, importe total, vendedor, instante de creación, instante de vencimiento y estado. La publicación MUST conservarse después de su cierre y MUST NOT ser eliminada como consecuencia de venderla, cancelarla o vencer.
- **FR-010**: La publicación MUST vencer 24 horas después de su creación. El estado efectivo `EXPIRED` MUST derivarse de que su vencimiento se haya alcanzado, sin proceso programado, sin tarea de expiración y sin actualización automática del estado almacenado. La frontera MUST evaluarse con el instante actual: una publicación está **vencida** cuando `now >= expiresAt` y está **vigente** únicamente cuando `now < expiresAt`.
- **FR-011**: Una publicación vencida MUST NOT poder comprarse, MUST NOT poder cancelarse, MUST NOT reservar tokens a partir de su vencimiento y MUST NOT aparecer en el mercado público. El filtro `?status=EXPIRED` MUST aplicar el estado efectivo, no el estado almacenado. La vigencia MUST decidirse una sola vez por operación, con la publicación bloqueada. En la compra, las tenencias necesarias MUST estar también bloqueadas antes de obtener el instante de esa única validación temporal decisiva, conservando el orden global `user_credits` → `token_listings` → `player_tokens` y los bloqueos hasta commit o rollback. La cancelación sigue bloqueando únicamente la publicación. Si la validación temporal decisiva se supera, la compra puede completarse aunque venza después y MUST NOT revalidar el vencimiento al final; ninguna publicación concurrente puede reutilizar entretanto los tokens comprometidos por esa compra.
- **FR-012**: El sistema MUST exponer `POST /api/orders/buy` y MUST conservar sin cambios el comportamiento de compra directa al superusuario definido en SPEC-010 cuando la solicitud no identifica una publicación.
- **FR-013**: Cuando la solicitud de compra identifica una publicación, el sistema MUST determinar el jugador, la cantidad, el precio unitario y el vendedor exclusivamente desde esa publicación. Los datos de jugador, cantidad o precio suministrados por el cliente MUST NOTinfluir en la operación.
- **FR-014**: La compra de una publicación MUST adquirir la totalidad de su cantidad. El sistema MUST NOT admitir compras parciales, fraccionadas ni solicitudes de compra pendientes.
- **FR-015**: Una compra válida MUST transferir al comprador exactamente la cantidad publicada, debitar al comprador exactamente el importe total, acreditar al vendedor exactamente ese mismo importe, marcar la publicación como `SOLD` y registrar la operación. Todas esas acciones MUST ocurrir atómicamente: si alguna falla, ninguna se conserva.
- **FR-016**: El sistema MUST rechazar con 409 `INSUFFICIENT_CREDITS` la compra de una publicación cuyo importe total exceda el saldo del comprador, sin revelar el saldo ni el importe requerido, sin saldo negativo y sin modificar tenencias ni el estado de la publicación.
- **FR-017**: El sistema MUST rechazar con 403 `CANNOT_BUY_OWN_LISTING` la compra de una publicación cuyo vendedor es el propio comprador, sin movimientos.
- **FR-018**: El sistema MUST rechazar con 409 `LISTING_NOT_AVAILABLE` la compra de una publicación que no esté efectivamente activa y vigente, y también cuando el vendedor no disponga de una cuenta de créditos válida para recibir el pago. La respuesta MUST NOT distinguir públicamente entre los motivos `SOLD`, `CANCELLED`, `EXPIRED` y la ausencia de cuenta de créditos del vendedor. Ninguno de estos rechazos MUST producir transferencias, registro de transacción ni cambio del estado de la publicación.
- **FR-019**: El sistema MUST rechazar con 404 `LISTING_NOT_FOUND` la compra de una publicación inexistente, y también la compra de una publicación cuyo jugador no cumpla el predicado de visibilidad vigente del catálogo, siempre que el solicitante no sea su propietario. Este rechazo MUST NOT revelar la existencia de la publicación ni la de su jugador, y MUST NOT producir transferencias ni registro de transacción.
- **FR-020**: El sistema MUST garantizar que cada publicación se liquide a lo sumo una vez. Con dos intentos de compra simultáneos de la misma publicación, solo uno puede confirmarse y el otro MUST recibir 409, sin doble transferencia, sin saldo negativo y con un único registro financiero asociado a esa publicación. La garantía se apoya en un orden global de protección de recursos que cada operación respeta como subconjunto ordenado; ese orden y su aplicación están definidos en `research.md` R-01 y en el flujo transaccional de `plan.md`.
- **FR-021**: Si, de forma excepcional, al comprar una publicación la tenencia del vendedor no cubre la cantidad publicada, el sistema MUST rechazar la compra con 409 `INSUFFICIENT_STOCK`, sin transferir tokens ni créditos, sin registrar transacción y sin marcar la publicación como `SOLD`. La publicación MUST conservar su registro y MUST NOT cambiar de estado como consecuencia automática de este rechazo; el sistema MUST NOT inventar un mecanismo de reparación o cancelación. Las operaciones normales MUST respetar las reservas de tokens de las publicaciones activas y vigentes, de modo que esta situación MUST NOT producirse en los flujos ordinarios.
- **FR-022**: Cada venta confirmada MUST tener exactamente un registro financiero que la referencie. Ese registro MUST contener comprador, vendedor, jugador, cantidad, precio unitario aplicado, importe total, instante de la operación y los estados de saldos y tenencias anteriores y posteriores de ambas partes, y MUST permanecer sin modificaciones ni eliminaciones durante las operaciones normales del sistema. Una venta no confirmada MUST NOT quedar registrada como exitosa.
- **FR-023**: El registro financiero de mercado MUST admitir una referencia opcional a la publicación de origen: presente en las compras de publicaciones y nula en las compras directas al superusuario. Ambas modalidades MUST conservar sus campos actuales y su trazabilidad.
- **FR-024**: La respuesta de compra MUST incorporar un identificador de publicación opcional, presente en compras de publicaciones y ausente o nulo en compras directas al superusuario, conservando los campos de respuesta existentes.
- **FR-025**: El sistema MUST exponer `POST /api/listings/{id}/cancel`, permitida exclusivamente al propietario de una publicación que esté efectivamente activa y vigente. La cancelación MUST llevar la publicación a `CANCELLED`, registrar su cierre y liberar los tokens que tenía reservados. Sobre una publicación `SOLD`, `CANCELLED` o efectivamente `EXPIRED` MUST rechazarse sin cambios, y sobre una publicación de otro usuario MUST rechazarse por falta de autorización sin cambios.
- **FR-026**: El sistema MUST exponer `GET /api/listings` para listar las publicaciones efectivamente activas y vigentes de otros usuarios cuyo jugador esté visible en el catálogo. MUST excluir las publicaciones propias, las vencidas, las que no estén activas y las de jugadores no visibles.
- **FR-027**: El sistema MUST exponer `GET /api/listings/mine` para listar todas las publicaciones del usuario autenticado, incluidas las de jugadores no visibles, con un filtro opcional de estado aplicado al estado efectivo. El filtro MUST decidir únicamente por el estado efectivo de la publicación y MUST NOT considerar la visibilidad del jugador. Sin filtro MUST devolverse todas las publicaciones propias. El filtro MUST aceptar únicamente las formas exactas `ACTIVE`, `SOLD`, `CANCELLED` y `EXPIRED`; cualquier otro valor, incluida otra capitalización del mismo estado, MUST rechazarse como solicitud inválida sin convertirlo ni devolver publicaciones.
- **FR-028**: El sistema MUST exponer `GET /api/listings/{id}`. El propietario MUST poder consultar su publicación en cualquier estado; un tercero MUST consultar solo publicaciones efectivamente activas y vigentes. Para una publicación inexistente o no visible al solicitante MUST responder 404 `LISTING_NOT_FOUND`, sin revelar su existencia.
- **FR-029**: Una publicación cuyo jugador deja de estar visible MUST NOT cancelarse ni eliminarse. MUST simplemente dejar de aparecer en el mercado público y su propietario MUST conservar el acceso a ella en su historial y en el detalle.
- **FR-030**: Todas las operaciones de publicación, consulta, cancelación y compra MUST requerir sesión autenticada válida, y MUST resolverse sin ejecución cuando la sesión es ausente, inválida o vencida.
- **FR-031**: SPEC-011 MUST NOT exponer `POST /api/listings` para crear publicaciones ni ningún flujo de solicitudes de compra pendientes. La única ruta autorizada para publicar una venta MUST ser `POST /api/orders/sell`.
- **FR-032**: Los endpoints, autenticación, solicitudes, respuestas y errores MUST documentarse en OpenAPI conforme a la documentación de la API del proyecto.
- **FR-033**: Los rechazos de publicación y de compra MUST preservar las reservas vigentes, los saldos, las tenencias y los estados de publicación, y MUST NOT revelar tenencias, saldos ni importes reservados.

### Key Entities

- **Publicación**: Oferta de una cantidad de tokens de un jugador, hecha por un usuario, con precio unitario congelado al crearse, importe total, instante de creación, instante de vencimiento y estado. Es el objeto que las personas usuarias publican, consultan, cancelan y compran. No es un jugador, ni una orden de mercado, ni una solicitud de compra.
- **Reserva**: Inmovilización lógica de una cantidad de tokens mientras una publicación está efectivamente activa y vigente. Los tokens siguen perteneciendo al vendedor. Una publicación deja de reservar al venderse, al cancelarse o al vencer.
- **Estado efectivo de una publicación**: `ACTIVE`, `SOLD`, `CANCELLED` o `EXPIRED`, derivado de su estado y de su vencimiento. Es el estado que observan las consultas, los filtros y las decisiones de compra y cancelación.
- **Usuario**: Persona registrada con un rol, una cuenta de créditos y tenencias de tokens. Solo los usuarios con rol de usuario normal publican y compran publicaciones. El superusuario conserva su papel de vendedor de las compras directas y no participa en el mercado secundario.
- **Jugador**: Recurso del catálogo cuya existencia y visibilidad se validan al publicar; solo un jugador visible admite publicación. Una publicación puede seguir existiendo aunque su jugador deje de estar visible.
- **Tenencia**: Cantidad de tokens de un jugador propiedad de un usuario, de la que se descuenta lo reservado por publicaciones activas y vigentes.
- **Transacción de mercado**: Registro financiero inmutable de una operación de compra, presente en compras directas al superusuario y en compras de publicaciones, con referencia opcional a la publicación de origen.
- **Cotización vigente**: Precio actual de un token de un jugador. Se consulta al publicar y su resultado queda congelado en la publicación.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Publicar nunca mueve recursos: tras cualquier publicación, los saldos de créditos de todas las partes son los mismos y el vendedor conserva la totalidad de sus tokens, que solo quedan reservados.
- **SC-002**: La suma de tokens reservados por un usuario para un jugador nunca excede su tenencia de ese jugador, ni siquiera con publicaciones simultáneas, múltiples publicaciones del mismo jugador o publicaciones vencidas que ya no reservan, incluida la carrera entre una compra en curso, su vencimiento y nuevas publicaciones sobre esa misma tenencia.
- **SC-003**: El precio de una publicación no cambia cuando cambia la cotización del jugador, y el importe total siempre es el producto exacto de la cantidad por el precio unitario congelado.
- **SC-004**: Una publicación deja de ser comprable, cancelable y reservante en su vencimiento sin que exista ningún proceso programado que modifique su estado almacenado.
- **SC-005**: El mercado público muestra exactamente las publicaciones efectivamente activas y vigentes de otros usuarios sobre jugadores visibles, y no muestra ninguna publicación propia, vencida, no activa ni de jugador no visible.
- **SC-006**: El propietario consulta y filtra por estado efectivo todas sus publicaciones, incluidas las de jugadores no visibles, y un tercero no obtiene detalle de publicaciones que no le son visibles.
- **SC-007**: Cada publicación se liquida a lo sumo una vez: con dos compras simultáneas, exactamente una se confirma y existe un único registro financiero asociado a esa publicación.
- **SC-008**: Cada compra de publicación transfiere exactamente la cantidad publicada y su importe exacto, deja a las partes con saldos y tenencias no negativos y marca la publicación como vendida.
- **SC-009**: Ninguna publicación rechazada ni ninguna compra rechazada altera saldos, tenencias, reservas ni estados de publicación, y no existe ningún registro financiero de una venta no confirmada.
- **SC-010**: Hay exactamente un registro financiero completo por cada venta de publicación, con referencia a esa publicación, y las compras directas al superusuario siguen teniendo exactamente uno sin referencia a publicación.
- **SC-011**: Cancelar una publicación vigente libera sus tokens reservados y permite al propietario volver a publicarlos, y ninguna publicación puede cancelarse más de una vez ni después de vendida, cancelada o vencida.
- **SC-012**: Las respuestas de aceptación y rechazo de las operaciones de publicación, consulta, cancelación y compra coinciden con el contrato documentado, incluidos campos, unidades monetarias, autenticación, errores y ausencia de revelaciones prohibidas.
- **SC-013**: Ninguna publicación puede crearse para un jugador no visible, y toda publicación creada mientras su jugador era visible se conserva sin cancelación ni eliminación aunque el jugador deje de estar visible, permaneciendo consultable por su propietario.
- **SC-014**: Ninguna publicación de un jugador no visible puede comprarse por un tercero, y el intento de hacerlo no revela que la publicación ni su jugador existen.

## Assumptions

No se requieren supuestos funcionales adicionales a las decisiones aprobadas y las dependencias siguientes.

### Dependencias funcionales

- **SPEC-001/003**: registro, identificación y autenticación de usuarios; distinción vigente entre usuario normal y superusuario único. La autorización se decide por el rol persistido.
- **SPEC-002/007**: catálogo con existencia y visibilidad vigentes de los jugadores. La publicación exige que el jugador exista y esté visible (FR-005); las mismas reglas de visibilidad determinan qué publicaciones aparecen en el mercado y cuáles puede consultar un tercero (FR-026, FR-028).
- **SPEC-010**: dominio de mercado ya establecido — saldos de créditos, tenencias de tokens, emisión inicial por jugador, representación monetaria en centésimas, compra directa contra el superusuario y registro financiero de compras. Esta feature se apoya en ese dominio y conserva su comportamiento de compra directa.
- **REQ-S2-03**: cotización vigente por token. Su cálculo, persistencia y evolución no pertenecen a esta feature; mientras no exista una fuente definitiva, la fuente de cotización vigente sigue siendo la que SPEC-010 utiliza.

Los objetivos de rendimiento generales corresponden al proyecto, conforme a `tp/enunciado.md` §5.3; no se fija aquí un umbral no acordado ni se confunde rapidez con integridad de las operaciones concurrentes.

### Supuestos

- **Cuentas de créditos**: se asume que todo usuario registrado del mercado posee una cuenta de créditos, creada junto con su alta y nunca eliminada durante la operación normal (SPEC-010 FR-015). Sobre esa base, cada usuario puede recibir el pago de una venta. Si la cuenta del vendedor faltara, situación excepcional y no prevista en el flujo ordinario, el comportamiento ya decidido es rechazarla con 409 `LISTING_NOT_AVAILABLE`, sin transferencias, sin registro de transacción y sin marcar la publicación como `SOLD` (FR-018). Esa respuesta es indistinguible de la de una publicación no disponible.
- **Nivel de aislamiento**: se asume el nivel por defecto de la plataforma y del servidor de base de datos, sin aislamiento reforzado configurado de forma explícita. La integridad de las operaciones concurrentes no se apoya en esa suposición sino en bloqueos explícitos y en la revalidación bajo bloqueo; el supuesto se declara para que su cambio sea una decisión consciente.
- **Fuente de cotización**: se asume que la fuente de cotización vigente está disponible al publicar. Su ausencia se trata como rechazo definido (FR-007), no como un precio de respaldo.

## Clarifications

### Decisiones funcionales vigentes

| ID | Decisión |
| --- | --- |
| CL-01 | Los importes se expresan en centésimas de crédito: 1 crédito equivale a 100 unidades. Rige también SPEC-010. |
| CL-02 | El precio unitario de una publicación se obtiene de la fuente de cotización en el momento de publicar y queda congelado; el valor inicial de referencia es 1 crédito por token. |
| CL-03 | Publicar es una operación distinta de la venta efectiva: reserva tokens, congela el precio y no transfiere ni registra una venta. La venta se materializa cuando otro usuario compra la publicación. |
| CL-04 | `POST /api/orders/sell` publica. No liquida. La única operación que liquida una venta entre usuarios es la compra de la publicación mediante `POST /api/orders/buy` con identificador de publicación. |
| CL-05 | La compra es siempre por la totalidad de la publicación. No hay compras parciales ni solicitudes de compra pendientes. |
| CL-06 | Los estados de negocio son `ACTIVE`, `SOLD`, `CANCELLED` y `EXPIRED`. `EXPIRED` es un estado efectivo calculado a partir del vencimiento, sin proceso programado ni actualización automática del estado almacenado. |
| CL-07 | La publicación vence a las 24 horas de su creación. La duración es el valor de negocio vigente y su posibilidad de ajuste en el backend se resolverá en la fase técnica, sin alterar el comportamiento descrito aquí. |
| CL-08 | Solo el propietario puede cancelar, y solo mientras la publicación esté efectivamente activa y vigente. |
| CL-09 | El superusuario no publica ni vende y no recompra tokens en el mercado secundario; continúa vendiendo mediante el mecanismo de compra directa de SPEC-010. |
| CL-10 | No existe mercado dinámico de oferta y demanda, ni motor de emparejamiento de órdenes, ni endpoint de creación de publicaciones distinto de `POST /api/orders/sell`. |
| CL-11 | Solo puede publicarse una venta de un jugador visible en el catálogo. Un jugador existente que no cumple el predicado de visibilidad vigente hace que la publicación se rechace con 404 `PLAYER_NOT_VISIBLE`, sin reservar tokens ni crear publicación ni generar transacciones. El identificador inexistente mantiene su 404 `PLAYER_NOT_FOUND`. Resuelve PENDIENTE-1. |
| CL-12 | El rechazo por jugador no visible conserva HTTP 404 y el identificador `PLAYER_NOT_VISIBLE`, en coherencia con el contrato vigente de SPEC-010 para el mismo caso conceptual. Para terceros, el detalle de una publicación no visible mantiene su 404 `LISTING_NOT_FOUND`. Resuelve PENDIENTE-4. |
| CL-13 | Si, excepcionalmente, al comprar una publicación el vendedor ya no tiene tokens suficientes para respaldarla, la compra se rechaza con 409 `INSUFFICIENT_STOCK`, sin transferencias, sin transacción y sin marcar la publicación como `SOLD`. La publicación conserva su registro y su estado no cambia de forma automática por ese rechazo. Las operaciones normales respetan las reservas de las publicaciones activas y vigentes, por lo que esta situación no se produce en los flujos ordinarios. |
| CL-14 | Si el vendedor no tiene una cuenta de créditos válida para recibir el pago, la compra se rechaza con 409 `LISTING_NOT_AVAILABLE`, sin transferencias, sin transacción y sin marcar la publicación como `SOLD`, sin revelar el motivo ni mencionar al vendedor. El motivo es indistinguible públicamente de `SOLD`, `CANCELLED` y `EXPIRED`. |
| CL-15 | El filtro de estado de `GET /api/listings/mine` decide únicamente por el estado efectivo de la publicación y no considera la visibilidad del jugador: una publicación de un jugador no visible aparece si su estado efectivo coincide con el filtro. |
| CL-16 | El filtro acepta solo las formas exactas `ACTIVE`, `SOLD`, `CANCELLED` y `EXPIRED`. Cualquier otra capitalización o un valor desconocido se rechazan como solicitud inválida, sin conversión ni tolerancia. |
| CL-17 | Publicar cuando la cantidad supera la **disponible** (tenencia menos reservas de publicaciones efectivamente activas y no vencidas) responde 409 `INSUFFICIENT_STOCK`, sin crear publicación ni modificar tenencias, créditos ni transacciones. Aplica a FR-006 y a los criterios AC02, AC04 y AC06. |
| CL-18 | El rango 1–100 tokens por publicación se apoya en el máximo de tokens en circulación por jugador: 100 tokens emitidos por jugador según el enunciado, emisión inicial única de 100 sin reposición según SPEC-010 FR-016, y una restricción de igualdad en la base de datos. El mínimo de 1 responde a que una publicación debe contener una cantidad positiva. El fundamento documenta el rango existente y no introduce reglas de emisión, suministro ni restricciones adicionales. |

### Sesión 2026-10-17

- Q: ¿Qué debe ocurrir cuando un usuario intenta comprar una publicación mediante su identificador y el jugador de esa publicación ya no es visible en el catálogo? → A: 404 `LISTING_NOT_FOUND`. Una publicación cuyo jugador no es visible se comporta como si no existiera para quien no es su propietario, sin revelar su existencia; el propietario conserva el acceso.
- Q: Si un comprador intenta adquirir una publicación y el vendedor no dispone de una cuenta de créditos válida para recibir el pago, ¿qué debe responder el sistema? → A: 409 `LISTING_NOT_AVAILABLE`, sin transferencias, sin registro de transacción y sin marcar la publicación como `SOLD`, sin revelar el motivo.
- Q: Cuando el propietario lista sus publicaciones con el filtro de estado y su publicación corresponde a un jugador que ya no es visible, ¿qué debe devolver el listado? → A: El filtro decide únicamente por el estado efectivo de la publicación y no considera la visibilidad del jugador, de modo que esa publicación aparece en el resultado.
- Q: Si alguien escribe el filtro de estado en minúsculas, por ejemplo `?status=active`, ¿el sistema debe aceptarlo o rechazarlo? → A: Rechazarlo. Solo se aceptan las formas exactas `ACTIVE`, `SOLD`, `CANCELLED` y `EXPIRED`, sin conversión de mayúsculas ni tolerancia a valores desconocidos.

### Historial de puntos pendientes — resueltos

Estos puntos se registraron como pendientes durante la preparación y se conservan con sus resoluciones. No quedan pendientes funcionales en esta lista.

- **[PENDIENTE-1 — RESUELTO] Publicación sobre jugador no visible**: resuelto por decisión funcional aprobada el 2026-10-17 e incorporado en FR-005, AC08, CL-11, SC-013 y los casos límite. Un usuario con rol USER solo puede publicar para un jugador visible; si el jugador existe pero no es visible, la publicación se rechaza sin reservar tokens, sin crearse y sin generar transacciones. La pérdida de visibilidad posterior no cancela ni elimina automáticamente una publicación ya creada: deja de aparecer en el mercado y su propietario conserva el acceso a ella y su historial.
- **[PENDIENTE-2 — RESUELTO en fase técnica e implementación autorizada] Modificación de SPEC-010**: la adaptación condicional del request, el campo aditivo de respuesta y la referencia nullable de auditoría se planificaron e implementaron dentro de SPEC-011. Contrato y modelo de SPEC-010 actualizados; CR-01 conserva la validación directa y selecciona la modalidad antes del binding. CODE REVIEW y TESTING humanos aprobados; evidencia en implementation-review.md y testing-report.md. Se conserva como antecedente la reserva de esta modificación a la fase técnica, ya superada.
- **[PENDIENTE-3 — RESUELTO en Plan/C1] Orden de protección transaccional**: research.md R-01 y plan.md fijan cuentas → publicación → tenencias, con instante decisivo posterior a adquirir las tenencias. Código y regresiones C1 aplicados y verificados en la suite humana; no queda pendiente la elección del orden. No acredita ausencia universal de interbloqueos.
- **[PENDIENTE-4 — RESUELTO] Código de respuesta del rechazo por jugador no visible al publicar**: resuelto por decisión funcional aprobada el 2026-10-17, coherente con SPEC-010. El rechazo responde HTTP 404 con identificador `PLAYER_NOT_VISIBLE`, distinto del 404 `PLAYER_NOT_FOUND` del identificador inexistente. Incorporado en FR-005, AC08, CL-11 y CL-12. El detalle de una publicación no visible visto por un tercero conserva su 404 `LISTING_NOT_FOUND` (AC36, AC37).

## Out of Scope

- Mercado dinámico de oferta y demanda, motor de emparejamiento de órdenes, libro de órdenes, órdenes a mercado y órdenes limitadas.
- Solicitudes de compra pendientes, compras parciales, fraccionadas o negociadas, y cualquier posibilidad de fijar el precio o la cantidad por el comprador.
- Cualquier flujo de creación de publicaciones distinto de `POST /api/orders/sell`.
- Portfolio del usuario y consulta del historial de transacciones (`GET /users/:id/portfolio` y `GET /users/:id/transactions`): el registro financiero conserva la información necesaria, pero su consulta corresponde a otra feature.
- Cálculo, recálculo, persistencia y consulta de cotizaciones, y la evolución de la fórmula de valuación (REQ-S2-03 y siguientes).
- Notificaciones, avisos o comunicación al vendedor o al comprador.
- Comisiones, spreads, tasas, recargas de créditos o cualquier mecanismo de creación de créditos distinto del saldo inicial de SPEC-010.
- Procesos programados de expiración, tareas batch o procesos automáticos que actualicen el estado almacenado de las publicaciones.
- Frontend. La entrega de interfaz de usuario para compra y venta de tokens (TP §11) corresponde a otra feature.
- Reparación automática de alteraciones manuales de la base de datos.

## Trazabilidad

- **REQ origen**: `REQ-S2-07` — Vender tokens de un jugador (épica `[EPIC] REQ-S2-07 - Vender tokens de un jugador`).
- **Fuentes**: `tp/enunciado.md` §3.3, §4, §5.2 y §6; `tp/entregas.md`, Sprint 2 (03/11/2026).
- **Compatibilidad**: SPEC-011 conserva el comportamiento de compra directa de SPEC-010 y se apoya en su dominio de mercado. La adaptación prevista en PENDIENTE-2 fue ejecutada en la fase técnica autorizada, documentada y verificada; la reserva original a una etapa posterior queda como historia, no como pendiente vigente.
- **Estado SDD histórico al redactar Specify, superado por el cierre registrado al inicio**: fase `Specify`. La existencia de este documento no completaba la preparación ni habilitaba `READY`. Las historias de usuario de esta spec todavía no tenían tarjeta en Trello: su alta o reutilización formaba parte del cierre de `READY`, después del gate humano de `Analyze`.

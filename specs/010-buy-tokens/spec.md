# Feature Specification: SPEC-010 | Mercado Oficial: Compra de tokens y catálogo con cotizaciones integradas (REQ-S2-06, Issue #90)

**Feature Branch**: `feat/010-buy-tokens`

**Created**: 2026-10-14

**Evolución**: 2026-11-12 (Sprint 3 — [Issue #90](https://github.com/YoelSLA/football-market/issues/90))

**Status**: Clarified

**Input**: User description: "Catálogo del Mercado Oficial con cotizaciones integradas ([Issue #90](https://github.com/YoelSLA/football-market/issues/90)) y compra de tokens al superusuario ([REQ-S2-06](https://github.com/YoelSLA/football-market/issues/49))."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprar tokens de un jugador (Priority: P1)

Como usuario registrado, quiero comprar tokens de un jugador con mis créditos, a su precio vigente, para adquirir una posición en el mercado.

**Why this priority**: Es la operación transaccional de entrada al mercado primario; aporta una posición financiada y un comprobante de operación auditable.

**Independent Test**: Con un usuario habilitado, créditos suficientes, jugador visible, stock del superusuario y cotización disponible, solicitar una compra y comprobar el resultado, las transferencias y su registro financiero. Puede demostrarse sin realizar ventas ni consultar portfolio o historial.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC01 | Usuario normal con 100 créditos, sin tenencia previa del jugador; superusuario con 100 tokens y 0 créditos; precio vigente de 1 crédito por token | Compra 10 tokens | 200 con los datos de FR-018, cantidad 10, `unitPrice=100` y `totalAmount=1000` en centésimas. Quedan 90 créditos y 10 tokens en el comprador, 10 créditos y 90 tokens en el vendedor; existe exactamente un registro financiero completo. |
| AC02 | Comprador con créditos inferiores al importe y stock suficiente | Solicita la compra | 409 `INSUFFICIENT_CREDITS`, sin revelar el saldo y sin efectos financieros. |
| AC03 | Comprador con créditos suficientes y stock inferior a lo solicitado | Solicita la compra | 409 `INSUFFICIENT_STOCK`, sin ejecución parcial ni revelación del stock. |
| AC04 | Cantidad ausente, nula, de tipo o representación inválidos, o entero representable fuera de 1–100 | Envía la solicitud | 400 `INVALID_REQUEST` antes de invocar el caso de uso para errores de estructura, tipo o representación; 400 `INVALID_QUANTITY` para literales enteros JSON representables como Integer fuera de 1–100. No hay compra ni coerción, redondeo o truncamiento. |
| AC05 | Identificador que no corresponde a un jugador | Solicita la compra | 404 `PLAYER_NOT_FOUND`; no hay movimientos. |
| AC06 | Jugador elegible sin cotización vigente disponible | Solicita la compra | 409 `QUOTE_UNAVAILABLE`; no se inventa un precio. |
| AC07 | Superusuario autenticado | Intenta comprar | 403 `SUPERUSER_CANNOT_BUY`; no hay movimientos. |
| AC08 | Dos compradores con créditos suficientes y un solo token disponible del mismo jugador | Solicitan simultáneamente un token cada uno | Una compra confirma y la otra recibe 409 `INSUFFICIENT_STOCK`; el stock termina en cero y el total emitido se conserva. |
| AC09 | Un comprador con saldo para una sola de dos compras, ambas con stock suficiente | Solicita ambas simultáneamente, incluso sobre jugadores diferentes | Solo una confirma; la otra recibe 409 `INSUFFICIENT_CREDITS`; no hay saldo negativo ni transferencias parciales. |
| AC11 | Una compra confirmada y saldo y stock suficientes para otra | El cliente repite la solicitud | Confirma una compra distinta con otro identificador y otro registro de auditoría; la repetición no es motivo de rechazo. |
| AC12 | Jugador existente pero inactivo o no visible al validar su elegibilidad | Solicita la compra | 404 `PLAYER_NOT_VISIBLE`; no hay movimientos. |
| AC13 | Importe o saldo resultante fuera del rango monetario soportado | Se intenta procesar la compra | 500 `INTERNAL_ERROR` genérico; no hay movimientos ni registro de compra confirmada. |
| AC14 | Sesión ausente, inválida o vencida al autenticar la solicitud | Intenta comprar | 401; no se ejecuta la compra. |
| AC15 | Cotización de precio cero o negativo | Solicita la compra | 409 `INVALID_QUOTE_PRICE`; no hay movimientos. |
| AC16 | Solicitud que cumple las condiciones de compra | Al procesarla no puede completarse su registro financiero | 500 `INTERNAL_ERROR` genérico; saldos y tenencias previos intactos, sin registro de compra confirmada. |
| AC19 | Superusuario inexistente o no identificable en el momento de la compra | Se procesa una solicitud de compra válida en lo demás | La operación se rechaza con 500 `INTERNAL_ERROR` genérico antes de mover recursos; no hay débitos, ni tenencias creadas o modificadas, ni registro de auditoría, y la atomicidad se mantiene. |

En todo rechazo se conservan saldos y tenencias y no se registra una compra confirmada (FR-011/014).

### Prerrequisitos funcionales verificables de US1

Estas condiciones habilitan US1; no son solicitudes al endpoint de compra ni historias independientes.

| ID | Given | When | Then | Cobertura |
| --- | --- | --- | --- | --- |
| AC10 | Usuarios registrados antes de incorporar créditos | Se les asigna el saldo inicial, incluso si se repite la asignación | Cada usuario normal recibe 100 créditos y el superusuario 0 una sola vez; los saldos existentes no se sobrescriben ni se restauran créditos consumidos. | FR-015, SC-013 |
| AC17 | Jugador sin emisión inicial | Se habilita su inventario y posteriormente se repite la habilitación tras compras | Se asignan 100 tokens al superusuario una sola vez; repetir no repone el inventario vendido ni aumenta el total. | FR-016/017, SC-012 |
| AC18 | Nuevo usuario que cumple las condiciones de registro | Se procesa su alta | El alta y su saldo inicial quedan disponibles juntos; si no puede asignarse el saldo, no se confirma el alta. | FR-015, SC-013 |

---

### User Story 2 - Consultar catálogo del Mercado Oficial con cotizaciones integradas (Priority: P1)

Como usuario autenticado de Football Market, quiero consultar el catálogo de jugadores del Mercado Oficial con su cotización vigente integrada en una única respuesta, para explorar el mercado primario, filtrar y ordenar activos por criterios deportivos y financieros, y preparar decisiones de compra informadas.

**Why this priority**: Es la capacidad de consulta fundamental del mercado primario requerida para el rediseño frontend (SPEC-019 / Issue #90). Resuelve el patrón N+1 de peticiones entre el catálogo y las cotizaciones, dotando a la aplicación de filtrado global (deportivo y financiero), ordenamiento por precio y variación, y sigilo sobre el inventario del superusuario.

**Independent Test**: Con usuarios autenticados y datos deportivos y de cotizaciones persistidos, consultar `GET /api/market/official` aplicando combinaciones de filtros, ordenamientos y paginación, verificando que los resultados coincidan exactamente con los criterios de negocio sobre el conjunto total sin exponer stock remanente ni saldo del superusuario. Puede probarse, verificarse y desplegarse independientemente sin requerir la ejecución previa de órdenes de compra.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC20 | Usuario autenticado en el sistema | Consulta `GET /api/market/official` con o sin paginación básica | 200 OK con metadatos (`page`, `size`, `totalElements`, `totalPages`) y la lista de jugadores activos y visibles con su cotización vigente consolidada sobre el total de la base de datos (CA-01). |
| AC21 | Jugador activo con cotización vigente en el sistema | Se incluye en la respuesta del catálogo | Informa `price`, `previousPrice`, `variationPercentage`, `performanceScore`, `multiplier`, `calculationOrigin`, `effectiveDate` y `calculatedAt` (CA-02). |
| AC22 | Activo con cotización vigente que no posee precio anterior válido (`previousPrice` nulo o `<= 0`) | Se incluye en la respuesta del catálogo | El campo `variationPercentage` se devuelve estrictamente como `null` (CA-03). |
| AC23 | Jugador activo y visible que no registra cotización vigente en el sistema | Se incluye en la respuesta del catálogo | Devuelve la identidad deportiva completa y el bloque `quote` en `null` (CA-04). |
| AC24 | Catálogo con jugadores de diversas identidades deportivas | Se filtra por `name` (normalizado con recorte de espacios y búsqueda parcial insensible a mayúsculas/acentos), `leagueId`, `teamId`, `position` o `nationality` | Si `name` normalizado queda vacío se ignora el filtro de nombre; si contiene texto, reduce el conjunto global antes de paginar, actualizando consistentemente `totalElements` y `totalPages` (CA-05). |
| AC25 | Catálogo con activos con y sin cotización | Se consulta con `hasQuote=true`, `hasQuote=false` o sin el parámetro | `true`: restringe exclusivamente a jugadores con cotización; `false`: restringe a jugadores sin cotización; ausente: incluye ambos conjuntos (CA-06). |
| AC26 | Catálogo con activos valorizados | Se envían `minPrice` y `maxPrice` (en centésimas) donde `0 <= minPrice <= maxPrice` | Filtra activos cuya cotización vigente se encuentra en el rango inclusive (CA-07). |
| AC27 | Solicitud con `minPrice < 0`, `maxPrice < 0` o `minPrice > maxPrice` | Se envía la consulta | 400 Bad Request con código `INVALID_PRICE_RANGE` (CA-07). |
| AC28 | Filtros sintácticamente válidos pero mutuamente excluyentes (ej. `hasQuote=false` y `minPrice=5000`) | Se envía la consulta | 200 OK con lista vacía (`content: []`), `totalElements: 0` y `totalPages: 0`, sin producir error 400 (CA-08). |
| AC29 | Consulta con ordenamiento por `price`, `variation`, `name` o `id` (ascendente o descendente) | Se solicita el listado ordenado con un único criterio principal | Aplica ordenación sobre el conjunto total y sitúa los valores nulos en `price` o `variation` al final (`NULLS LAST`) tanto en ASC como en DESC. Múltiples criterios explícitos responden 400 `INVALID_SORT_PARAMETER` (CA-09). |
| AC30 | Múltiples registros con valores idénticos en el criterio de ordenación principal | Se recorren páginas consecutivas sobre un estado de datos estable (sin modificaciones concurrentes) | El desempate determinista por `id ASC` garantiza no omitir ni repetir jugadores entre páginas (CA-10). |
| AC31 | Cualquier consulta al catálogo de emisión oficial | Se inspecciona la respuesta | No se expone ningún campo de stock, inventario remanente ni saldo del superusuario; la presencia de un jugador no afirma stock disponible para compra (CA-11). |
| AC32 | Solicitud sin credenciales de sesión válidas o con sesión expirada | Intenta consultar el catálogo | 401 Unauthorized sin acceso a datos de catálogo (CA-12). |
| AC33 | Transacción de compra existente `POST /api/orders/buy` | Se ejecuta antes o después de consultar el catálogo | Mantiene inalterado su contrato, validaciones, códigos de error y persistencia financiera (CA-13). |

---

### Edge Cases

Clasificación conforme a Constitution §2.2:

- **OBLIGATORIO — solicitudes de compra simultáneas:** conservar stock, saldos y tenencias (AC08/09, FR-011/013/017).
- **OBLIGATORIO — repetición de una compra:** tratarla como una operación nueva si cumple las condiciones (AC11, FR-021).
- **OBLIGATORIO — importe no representable o fallo del registro financiero:** rechazar sin movimientos parciales (AC13/16, FR-011/014/022).
- **OBLIGATORIO — comprador sin cuenta de créditos:** rechazar por créditos insuficientes, sin asignarlos durante la compra (FR-010/015).
- **OBLIGATORIO — reejecución de la asignación inicial o reinicio:** conservar recursos y registros existentes, sin restaurar créditos ni reemitir tokens (FR-014/015/016).
- **OBLIGATORIO — vendedor no identificable:** si el superusuario no existe o no puede resolverse, la compra se rechaza con 500 `INTERNAL_ERROR` genérico sin movimientos parciales ni auditoría (FR-023, AC19).
- **OBLIGATORIO — activo sin cotización en el catálogo oficial:** se devuelve con `quote: null` sin producir error de servidor (AC23, FR-027).
- **OBLIGATORIO — cotización inicial sin precio previo:** se calcula con `variationPercentage: null` cuando `previousPrice` es nulo o `<= 0` (AC22, FR-028).
- **OBLIGATORIO — precios negativos o rango invertido:** si `minPrice < 0`, `maxPrice < 0` o `minPrice > maxPrice`, responder 400 Bad Request `INVALID_PRICE_RANGE` antes de consultar datos (AC27, FR-031).
- **OBLIGATORIO — nombre vacío o solo espacios en blanco:** normalizar aplicando recorte (`trim`); si el texto resultante queda vacío, se omite el filtro de nombre devolviendo todos los jugadores (AC24, FR-029).
- **OBLIGATORIO — múltiples criterios de ordenamiento en la solicitud:** admitir estrictamente un único criterio de ordenación principal; responder 400 `INVALID_SORT_PARAMETER` si se envían múltiples criterios (AC29, FR-033).
- **OBLIGATORIO — filtros válidos pero lógicamente excluyentes:** responder 200 OK con lista vacía y totales en cero, sin error 400 (AC28, FR-032).
- **OBLIGATORIO — ordenamiento con valores nulos:** en `price` y `variation` (ASC y DESC), ubicar los nulos estrictamente al final (`NULLS LAST`) (AC29, FR-033).
- **OBLIGATORIO — sigilo de inventario:** la respuesta de catálogo no debe revelar jamás el stock remanente del superusuario (AC31, FR-035).
- **OBLIGATORIO — paginación fuera de rango:** si se solicita un número de página superior al total de páginas disponibles, responde 200 OK con `content: []`, conservando los totales globales (FR-036).
- **RECOMENDADO — compra y desactivación simultáneas:** garantizar un orden estricto entre ambas es una mejora no bloqueante. Se exige elegibilidad al validar la compra, no que permanezca sin cambios hasta su confirmación (D3).

---

## Requirements *(mandatory)*

### Functional Requirements

#### Requisitos de Compra de Tokens (US1 y Prerrequisitos)

- **FR-001**: El sistema MUST exponer `POST /api/orders/buy` para comprar tokens contra el superusuario.
- **FR-002**: El sistema MUST aceptar solicitudes de compra solo de usuarios autenticados cuya identidad reconoce; el solicitante no puede elegir otra identidad de comprador.
- **FR-003**: El sistema MUST autorizar la compra solo a usuarios normales según su rol vigente; el superusuario recibe 403 `SUPERUSER_CANNOT_BUY`.
- **FR-004**: La solicitud MUST incluir `playerId` entero y `quantity`; entradas ausentes o no interpretables se rechazan con 400, sin ejecutar la compra.
- **FR-005**: `quantity` MUST ser un literal entero JSON representable como Integer (−2147483648 a 2147483647) y estar entre 1 y 100 inclusive. Si está ausente, es `null`, string, booleano, array, objeto, decimal o entero fuera de rango, MUST responder 400 `INVALID_REQUEST`. Un entero representable fuera de 1–100 MUST responder 400 `INVALID_QUANTITY`. No se permiten coerciones implícitas, redondeo ni truncamiento.
- **FR-006**: El sistema MUST comprobar que el jugador exista y esté activo y visible según las reglas vigentes del catálogo al validar la compra; responde 404 `PLAYER_NOT_FOUND` o `PLAYER_NOT_VISIBLE` en caso contrario.
- **FR-007**: El sistema MUST utilizar el precio vigente por token: 409 `QUOTE_UNAVAILABLE` si no está disponible y 409 `INVALID_QUOTE_PRICE` si no es positivo. No utiliza precios de respaldo propios de la compra.
- **FR-008**: El importe MUST ser `quantity × unitPrice`, utilizando el mismo precio aplicado en la transferencia, respuesta y auditoría. Los importes se expresan en centésimas de crédito.
- **FR-009**: El sistema MUST rechazar íntegramente una compra sin stock suficiente del superusuario con 409 `INSUFFICIENT_STOCK`, sin revelar el stock disponible.
- **FR-010**: El sistema MUST rechazar una compra sin créditos suficientes con 409 `INSUFFICIENT_CREDITS`, sin revelar el saldo disponible ni el importe requerido; una cuenta ausente no se considera financiada.
- **FR-011**: Una compra MUST confirmarse solo si se completan todos sus movimientos de créditos y tokens y su registro financiero. Ante cualquier fallo, los recursos deben quedar como antes de la operación, sin movimientos parciales; también en compras simultáneas.
- **FR-012**: Una compra confirmada MUST restar al superusuario y sumar al comprador exactamente la cantidad comprada, incluyendo su primera tenencia.
- **FR-013**: Una compra confirmada MUST debitar al comprador y acreditar al superusuario exactamente el mismo importe, sin crear ni perder créditos en la transferencia y sin saldos negativos.
- **FR-014**: Cada compra confirmada MUST tener exactamente un registro financiero con autor, comprador, vendedor, jugador, cantidad, precio aplicado, importe, fecha/hora y saldos y tenencias anteriores y posteriores de ambas partes. Debe conservar los valores y permanecer sin modificaciones ni eliminaciones.
- **FR-015**: Cada usuario normal MUST recibir 100 créditos iniciales y el superusuario 0, una sola vez. En altas nuevas, cuenta y saldo se confirman juntos. Reejecutar la inicialización o reiniciar no restaura créditos consumidos.
- **FR-016**: Cada jugador MUST tener una única emisión inicial de 100 tokens, todos propiedad del superusuario, antes de operar. Repetir su habilitación o reiniciar no repone tokens vendidos ni vuelve a emitirlos.
- **FR-017**: La suma de tenencias de cada jugador emitido MUST permanecer exactamente en 100 tras las operaciones válidas, incluso concurrentes; no hay sobreventa ni tenencias negativas.
- **FR-018**: La respuesta exitosa MUST ser 200 con `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount` y `executedAt` de la compra confirmada.
- **FR-019**: Las respuestas de error MUST respetar el [contrato público de compra](contracts/buy-tokens.md), sin revelar detalles internos. Los fallos no controlados responden 500 `INTERNAL_ERROR`.
- **FR-020**: El endpoint, autenticación, request, respuesta y errores MUST documentarse en OpenAPI.
- **FR-021**: Cada solicitud de compra válida MUST representar una operación independiente, incluso cuando repita una anterior.
- **FR-022**: Si el importe de la compra o un saldo resultante no puede representarse correctamente, el sistema MUST rechazar con 500 `INTERNAL_ERROR` genérico, sin movimientos parciales.
- **FR-023**: La compra MUST identificar al superusuario como vendedor persistido antes de mover recursos; si no es identificable, rechaza con 500 `INTERNAL_ERROR` genérico sin movimientos ni auditoría.

#### Requisitos del Catálogo del Mercado Oficial (US2 — Issue #90)

- **FR-024**: El sistema MUST exponer `GET /api/market/official` para la consulta paginada del catálogo de emisión oficial del mercado primario conforme al [contrato público del catálogo](contracts/official-catalog.md).
- **FR-025**: El sistema MUST requerir autenticación para invocar `GET /api/market/official`; solicitudes sin sesión autenticada válida responden 401 Unauthorized.
- **FR-026**: El catálogo MUST devolver exclusivamente jugadores activos y visibles según las reglas de elegibilidad del catálogo deportivo. La inclusión de un jugador en la respuesta no afirma disponibilidad de stock del superusuario.
- **FR-027**: Cada activo devuelto MUST integrar su cotización vigente consolidada en la misma respuesta cuando exista (`price`, `previousPrice`, `variationPercentage`, `performanceScore`, `multiplier`, `calculationOrigin`, `effectiveDate`, `calculatedAt`). Si el jugador no registra cotización vigente, el bloque `quote` MUST devolverse como `null`.
- **FR-028**: Si un activo tiene cotización pero no posee un precio previo válido (`previousPrice` es nulo o `<= 0`), el campo `variationPercentage` MUST devolverse como `null`. Una variación calculable con precio idéntico al anterior MUST devolverse como `0.0`.
- **FR-029**: El sistema MUST admitir filtros deportivos sobre el conjunto completo de la base de datos antes de paginar: `name` (búsqueda parcial insensible a mayúsculas y acentos tras aplicar recorte de espacios; si queda vacío se ignora el filtro), `leagueId`, `teamId`, `position` y `nationality`.
- **FR-030**: El sistema MUST admitir el filtro booleano `hasQuote`: si está ausente devuelve activos con y sin cotización; si es `true` restringe la consulta y los totales exclusivamente a jugadores con cotización; si es `false` restringe exclusivamente a jugadores sin cotización.
- **FR-031**: El sistema MUST admitir filtros por rango de precio unitario vigente (`minPrice` y `maxPrice` en centésimas de crédito no negativas) aplicados de forma inclusiva sobre el universo completo. Si `minPrice < 0`, `maxPrice < 0` o `minPrice > maxPrice`, MUST responder 400 Bad Request con código `INVALID_PRICE_RANGE`.
- **FR-032**: Combinaciones de filtros válidos sintácticamente pero sin coincidencias lógicas (ej. `hasQuote=false` combinado con rango de precio) MUST responder 200 OK con `content: []`, `totalElements: 0` y `totalPages: 0`, sin producir error 400.
- **FR-033**: El endpoint MUST soportar exactamente un **único criterio de ordenamiento principal explícito** entre `price`, `variation`, `name` e `id` en sentidos ascendente (`asc`) y descendente (`desc`). En ordenamientos por `price` o `variation`, los valores nulos MUST posicionarse al final (`NULLS LAST`), tanto en sentido ascendente como descendente. Múltiples criterios explícitos o parámetros inválidos MUST responder 400 Bad Request con código `INVALID_SORT_PARAMETER`.
- **FR-034**: La paginación MUST desempatar de forma determinista por identificador de jugador (`id ASC`) ante igualdad en los criterios de ordenamiento principales, garantizando que no se omitan ni repitan elementos al recorrer páginas consecutivas sobre conjuntos de datos estables (sin modificaciones concurrentes).
- **FR-035**: El catálogo MUST respetar el sigilo de inventario: la respuesta no debe exponer el stock, tenencias ni balances remanentes del superusuario.
- **FR-036**: La respuesta paginada MUST proporcionar metadatos estándar (`page`, `size`, `totalElements`, `totalPages`) coherentes con los filtros aplicados sobre la totalidad de los datos.

---

### Key Entities *(mandatory)*

- **Usuario**: Persona registrada en el sistema. Los usuarios normales compran tokens; el superusuario es el emisor y vendedor en el mercado primario.
- **Jugador**: Recurso del catálogo con identidad deportiva (nombre, posición, nacionalidad, equipo, liga e imágenes) cuya elegibilidad se valida para operar.
- **Cotización vigente**: Valuación canónica de un jugador en una fecha de vigencia, que define su precio unitario, precio previo, score técnico, multiplicador y variación porcentual.
- **Catálogo Oficial**: Proyección de lectura consolidada que combina la identidad deportiva del jugador con su cotización vigente para el mercado primario.
- **Tenencia**: Cantidad de tokens de un jugador que posee un usuario determinado.
- **Cuenta de créditos**: Balance monetario disponible de un usuario en el sistema.
- **Compra registrada**: Asiento financiero inmutable que documenta la transferencia de créditos y tokens entre comprador y superusuario.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cada compra confirmada entrega los datos de FR-018 y transfiere exactamente la cantidad solicitada y su importe, incluida la primera compra de un jugador por el usuario.
- **SC-008**: Después de compras sucesivas, el total de tokens por jugador emitido sigue siendo 100 y la suma de créditos de las partes no cambia por las transferencias.
- **SC-009**: Ninguna compra confirmada o rechazada deja créditos o tenencias negativos.
- **SC-010**: Hay exactamente un registro completo por compra confirmada y ninguno de compra exitosa por operación rechazada. Ante cualquier fallo, saldos y tenencias quedan intactos.
- **SC-011**: Las compras simultáneas confirman únicamente cantidades que pueden abastecerse y financiarse, sin doble gasto ni sobreventa.
- **SC-012**: Cada jugador recibe una sola emisión de 100 tokens; repetir su habilitación o reiniciar no incrementa el total ni restaura inventario vendido (AC17).
- **SC-013**: Cada usuario, nuevo o preexistente, recibe su saldo inicial una sola vez; no hay altas confirmadas sin saldo inicial ni restauración de créditos consumidos (AC10/18).
- **SC-014**: Las respuestas de aceptación y rechazo coinciden con los contratos públicos documentados en OpenAPI, incluidos campos, unidades monetarias, autenticación, errores y ausencia de revelaciones prohibidas.
- **SC-016**: Dos solicitudes iguales que cumplen las condiciones producen dos compras con identificadores y registros financieros distintos.
- **SC-017**: La consulta `GET /api/market/official` consolida la identidad deportiva y la cotización vigente en una única respuesta, eliminando el patrón N+1 de peticiones individuales en clientes consumidores.
- **SC-018**: Todos los filtros (deportivos, financieros y de cotización) y ordenamientos se aplican sobre la totalidad de la base de datos antes de generar la paginación, reflejándose consistentemente en `totalElements` y `totalPages`.
- **SC-019**: El 100% de las consultas ordenadas por precio o variación ubican los valores nulos al final de la colección (`NULLS LAST`), tanto en sentido ascendente como descendente.
- **SC-020**: El 100% de las respuestas del catálogo oficial respetan el sigilo de inventario, sin exponer cantidades remanentes de emisión ni saldos del superusuario.
- **SC-021**: El endpoint de compra `POST /api/orders/buy` mantiene el 100% de compatibilidad hacia atrás en sus contratos, validaciones y efectos transaccionales.

---

## Assumptions

- **A01**: El mecanismo canónico de cotizaciones persistidas de `SPEC-016` (feature `quote`) es la única fuente de la verdad para determinar el precio vigente de un activo.
- **A02**: La autenticación se resuelve mediante los mecanismos de seguridad vigentes (JWT/Spring Security) sin introducir protocolos alternativos.
- **A03**: Los importes monetarios y precios unitarios se expresan uniformemente en centésimas de crédito (integer/Long: 100 unidades = 1.00 crédito).
- **A04**: Los clientes consumidores gestionan la paginación con índice base 0 conforme al estándar del backend.
- **A05**: La disponibilidad de stock del superusuario es dinámica y se protege estrictamente en la transacción de compra; el catálogo no garantiza reserva previa.

---

## Clarifications

### Session 2026-10-14 & 2026-10-15 (Compra de tokens)

| ID | Decisión vigente |
| --- | --- |
| CL-01 | Importes en centésimas: 1 crédito = 100 unidades. |
| CL-02 | La cotización consumida expresa precio por token; el precio inicial definido por TP es 1 crédito. |
| CL-03 | La compra consume el precio vigente; la fórmula y creación de cotizaciones pertenecen a REQ-S2-01/03. |
| CL-04 | Se conservan los estados anteriores y posteriores de saldos y tenencias; comprar no cambia la cotización. |
| CL-05 | El superusuario no puede comprar tokens. |
| D1 | Cada solicitud válida es una compra nueva, aunque repita una anterior. |
| D2 | Usuario normal inicia con 100 créditos y superusuario con 0. |
| D3 | Opción A aprobada: se comprueba elegibilidad al validar la compra; orden compra/desactivación es recomendado no bloqueante. |
| D4 | Importe no representable rechaza con 500 sin movimientos ni compra confirmada. |
| D5 | Stock insuficiente: rechazo total sin revelar stock; tampoco se revela saldo en errores de créditos. |

### Session 2026-11-12 (Evolución Issue #90 — Catálogo del Mercado Oficial)

| ID | Decisión vigente |
| --- | --- |
| D6 | La presencia de un jugador en el catálogo oficial refleja su elegibilidad y visibilidad general, pero no afirma stock disponible para compra. El inventario se valida exclusivamente en `POST /api/orders/buy`. |
| D7 | `variationPercentage` es estrictamente `null` si `previousPrice` es nulo o menor o igual a cero (cotizaciones iniciales o sin variación válida). Variación cero se expresa como `0.0`. |
| D8 | El filtro `hasQuote` tiene semántica tri-estado: ausente incluye todos; `true` restringe a activos con cotización; `false` restringe a activos sin cotización. |
| D9 | Filtros sintácticamente válidos pero mutuamente excluyentes o sin coincidencias lógicas responden `200 OK` con `content: []` y totales en 0. El error `400 Bad Request` queda reservado para parámetros con sintaxis o rango inválidos (`minPrice > maxPrice`, paginación negativa, etc.). |
| D10 | En ordenamientos por `price` o por `variation` (ascendente y descendente), los valores nulos siempre se ubican al final (`NULLS LAST`). |
| D11 | El desempate determinista por `id ASC` aplica sobre conjuntos de datos sin modificaciones concurrentes entre consultas consecutivas. |
| D12 | Si `minPrice < 0` o `maxPrice < 0`, la solicitud se rechaza con `400 Bad Request` (`INVALID_PRICE_RANGE`), ya que los precios monetarios son valores no negativos. |
| D13 | El parámetro `sort` admite exactamente un único criterio principal de ordenamiento explícito; solicitudes con múltiples criterios o parámetros repetidos se rechazan con `400 Bad Request` (`INVALID_SORT_PARAMETER`). |
| D14 | El filtro `name` se normaliza aplicando recorte de espacios (`trim`); si tras recortar el valor queda vacío, se omite el filtrado de nombre sin emitir error. |

---

## Out of Scope

- Modificar el flujo, contrato, validaciones o persistencia de `POST /api/orders/buy`.
- Exponer el inventario, stock remanente o balance del superusuario.
- Incluir en la respuesta del catálogo el desglose completo de 60 métricas de `player_statistics`.
- Modificar pantallas, hojas de estilo o componentes frontend (correspondiente a `SPEC-019`).
- Implementar funcionalidades del mercado secundario P2P o venta de tokens (`SPEC-011`).
- Generar cotizaciones retroactivas o recalcular precios durante la consulta de lectura.

---

## Trazabilidad

- **REQ origen**: [REQ-S2-06 (#49)](https://github.com/YoelSLA/football-market/issues/49) (Compra de tokens al superusuario).
- **Issue de evolución**: [Issue #90](https://github.com/YoelSLA/football-market/issues/90) (`SPEC-010 | Catálogo del Mercado Oficial con cotizaciones integradas`, Sprint 3).
- **Contratos asociados**:
  - [Contrato de Compra Directa](contracts/buy-tokens.md) (`POST /api/orders/buy`)
  - [Contrato de Catálogo Oficial](contracts/official-catalog.md) (`GET /api/market/official`)
- **Dependencias funcionales**:
  - `SPEC-002` ([#17](https://github.com/YoelSLA/football-market/issues/17)): Catálogo deportivo base.
  - `SPEC-016` ([#18](https://github.com/YoelSLA/football-market/issues/18) / [#39](https://github.com/YoelSLA/football-market/issues/39)): Cotización vigente canónica.
  - `SPEC-019` ([#42](https://github.com/YoelSLA/football-market/issues/42)): Interfaz web de compra de tokens (consumidora de este endpoint).

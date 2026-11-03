# Feature Specification: Comprar tokens de un jugador (REQ-S2-06)

**Feature Branch**: `feat/010-buy-tokens`

**Created**: 2026-10-14

**Status**: En cierre — US01 DONE, FEATURE COMPLETION bloqueada por T078 y T079

**Input**: Comprar tokens mediante `POST /api/orders/buy`, contra el inventario inicial del superusuario y a la cotización vigente. La compra transfiere créditos y tokens de forma atómica y deja un registro financiero auditable.

## User Scenarios & Testing

### User Story 1 - Comprar tokens de un jugador (Priority: P1)

Como usuario registrado, quiero comprar tokens de un jugador con mis créditos, a su precio vigente, para adquirir una posición en el mercado.

**Why this priority**: Es la operación de entrada al mercado exigida por el TP; aporta una posición financiada y un comprobante de operación.

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

### Prerrequisitos funcionales verificables

Estas condiciones habilitan US1; no son solicitudes al endpoint de compra ni historias independientes.

| ID | Given | When | Then | Cobertura |
| --- | --- | --- | --- | --- |
| AC10 | Usuarios registrados antes de incorporar créditos | Se les asigna el saldo inicial, incluso si se repite la asignación | Cada usuario normal recibe 100 créditos y el superusuario 0 una sola vez; los saldos existentes no se sobrescriben ni se restauran créditos consumidos. | FR-015, SC-013 |
| AC17 | Jugador sin emisión inicial | Se habilita su inventario y posteriormente se repite la habilitación tras compras | Se asignan 100 tokens al superusuario una sola vez; repetir no repone el inventario vendido ni aumenta el total. | FR-016/017, SC-012 |
| AC18 | Nuevo usuario que cumple las condiciones de registro | Se procesa su alta | El alta y su saldo inicial quedan disponibles juntos; si no puede asignarse el saldo, no se confirma el alta. | FR-015, SC-013 |

### Edge Cases

Clasificación conforme a Constitution §2.2; las mejoras recomendadas no condicionan la aceptación.

- **OBLIGATORIO — solicitudes simultáneas:** conservar stock, saldos y tenencias (AC08/09, FR-011/013/017).
- **OBLIGATORIO — repetición de una compra:** tratarla como una operación nueva si cumple las condiciones (AC11, FR-021).
- **OBLIGATORIO — importe no representable o fallo del registro financiero:** rechazar sin movimientos parciales (AC13/16, FR-011/014/022).
- **OBLIGATORIO — comprador sin cuenta de créditos:** rechazar por créditos insuficientes, sin asignarlos durante la compra (FR-010/015).
- **OBLIGATORIO — reejecución de la asignación inicial o reinicio:** conservar recursos y registros existentes, sin restaurar créditos ni reemitir tokens (FR-014/015/016).
- **OBLIGATORIO — vendedor no identificable:** si el superusuario no existe o no puede resolverse, la compra se rechaza con 500 `INTERNAL_ERROR` genérico sin movimientos parciales ni auditoría (FR-023, AC19). Es integridad transaccional: un fallo posterior dejaría una compra parcial.
- **RECOMENDADO — compra y desactivación simultáneas:** garantizar un orden estricto entre ambas es una mejora no bloqueante. Se exige elegibilidad al validar la compra, no que permanezca sin cambios hasta su confirmación (D3).

Stock y créditos insuficientes, entradas inválidas, jugador inexistente o no visible y cotización ausente o inválida son validaciones obligatorias ya cubiertas por AC02–AC06, AC12/15 y FR-004–FR-010; no requieren escenarios borde adicionales. La asignación inicial se verifica en los prerrequisitos AC10/17/18.

## Requirements

### Functional Requirements

Los requisitos siguientes son obligatorios para US1 o para sus prerrequisitos funcionales.

- **FR-001**: El sistema MUST exponer `POST /api/orders/buy` para comprar tokens contra el superusuario.
- **FR-002**: El sistema MUST aceptar solicitudes de compra solo de usuarios autenticados cuya identidad reconoce; el solicitante no puede elegir otra identidad de comprador.
- **FR-003**: El sistema MUST autorizar la compra solo a usuarios normales según su rol vigente; el superusuario recibe 403 `SUPERUSER_CANNOT_BUY`.
- **FR-004**: La solicitud MUST incluir `playerId` entero y `quantity`; entradas ausentes o no interpretables se rechazan con 400, sin ejecutar la compra.
- **FR-005**: `quantity` MUST ser un literal entero JSON representable como Integer (−2147483648 a 2147483647) y estar entre 1 y 100 inclusive. Si está ausente, es `null`, string (incluso numérico), booleano, array, objeto, representación decimal (como `10.0`), notación exponencial (como `1e1`) o entero fuera del rango de Integer, MUST responder 400 `INVALID_REQUEST` antes de invocar `BuyOrderService`, conforme a FR-004. Un entero representable fuera de 1–100 MUST responder 400 `INVALID_QUANTITY`. No se permiten coerciones implícitas, redondeo ni truncamiento. Una cantidad válida permite continuar el caso de uso, sin omitir las restantes condiciones de compra.
- **FR-006**: El sistema MUST comprobar que el jugador exista y esté activo y visible según las reglas vigentes del catálogo al validar la compra; responde 404 `PLAYER_NOT_FOUND` o `PLAYER_NOT_VISIBLE` en caso contrario.
- **FR-007**: El sistema MUST utilizar el precio vigente por token: 409 `QUOTE_UNAVAILABLE` si no está disponible y 409 `INVALID_QUOTE_PRICE` si no es positivo. No utiliza precios de respaldo propios de la compra.
- **FR-008**: El importe MUST ser `quantity × unitPrice`, utilizando el mismo precio aplicado en la transferencia, respuesta y auditoría. Los importes se expresan en centésimas de crédito.
- **FR-009**: El sistema MUST rechazar íntegramente una compra sin stock suficiente del superusuario con 409 `INSUFFICIENT_STOCK`, sin revelar el stock disponible.
- **FR-010**: El sistema MUST rechazar una compra sin créditos suficientes con 409 `INSUFFICIENT_CREDITS`, sin revelar el saldo disponible ni el importe requerido; una cuenta ausente no se considera financiada.
- **FR-011**: Una compra MUST confirmarse solo si se completan todos sus movimientos de créditos y tokens y su registro financiero. Ante cualquier fallo, los recursos deben quedar como antes de la operación, sin movimientos parciales; también en compras simultáneas.
- **FR-012**: Una compra confirmada MUST restar al superusuario y sumar al comprador exactamente la cantidad comprada, incluyendo su primera tenencia.
- **FR-013**: Una compra confirmada MUST debitar al comprador y acreditar al superusuario exactamente el mismo importe, sin crear ni perder créditos en la transferencia y sin saldos negativos.
- **FR-014**: Cada compra confirmada MUST tener exactamente un registro financiero con autor (comprador autenticado), comprador, vendedor, jugador, cantidad, precio aplicado, importe, fecha/hora y saldos y tenencias anteriores y posteriores de ambas partes. Debe conservar los valores de la operación y permanecer sin modificaciones ni eliminaciones durante las operaciones normales del sistema. Una compra no confirmada no debe quedar registrada como exitosa.
- **FR-015**: Cada usuario normal MUST recibir 100 créditos iniciales y el superusuario 0, una sola vez. Incluye usuarios preexistentes; en altas nuevas, cuenta y saldo se confirman juntos. Reejecutar la inicialización o reiniciar no sobrescribe cuentas ni restaura créditos consumidos. La compra nunca crea créditos para compensar una cuenta ausente.
- **FR-016**: Cada jugador, existente o nuevo, MUST tener una única emisión inicial de 100 tokens, todos propiedad del superusuario, antes de operar. Repetir su habilitación o reiniciar el sistema no repone tokens vendidos ni vuelve a emitirlos. Toda llamada exitosa al aprovisionamiento del SUPERUSER MUST recuperar jugadores sin registro de emisión, incluidos inactivos, tanto si el propietario es nuevo como si ya existe (también con configuración deshabilitada). La emisión y su tenencia inicial se confirman en la misma transacción del aprovisionamiento; cualquier fallo revierte la operación completa. No se modifican emisiones existentes, tenencias de compradores, créditos existentes ni auditorías. Tenencias sin emisión o emisiones sin tenencia del propietario inicial MUST provocar rechazo, sin reparación automática; una tenencia con cantidad cero no se considera ausente.
- **FR-017**: La suma de tenencias de cada jugador emitido MUST permanecer exactamente en 100 tras las operaciones válidas, incluso concurrentes; no hay sobreventa ni tenencias negativas.
- **FR-018**: La respuesta exitosa MUST ser 200 con `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount` y `executedAt` de la compra confirmada.
- **FR-019**: Las respuestas de error MUST respetar el [contrato público de compra](contracts/buy-tokens.md), sin revelar detalles internos. Los fallos de procesamiento, distintos de los rechazos de negocio documentados, responden 500 `INTERNAL_ERROR` genérico.
- **FR-020**: El endpoint, autenticación, request, respuesta y errores MUST documentarse en OpenAPI.
- **FR-021**: Cada solicitud de compra que cumpla las condiciones para confirmarse MUST representar una operación independiente, incluso cuando repita una solicitud anterior. Si el cliente no recibe una respuesta, no puede asumir que la compra haya fallado; repetir la solicitud puede confirmar una compra adicional.
- **FR-022**: Si el importe de la compra o un saldo resultante no puede representarse correctamente, el sistema MUST rechazar con 500 `INTERNAL_ERROR` genérico, sin alterar créditos ni tokens ni registrar una compra confirmada.
- **FR-023**: La compra MUST identificar al superusuario como vendedor persistido antes de mover recursos. Si no existe o no es identificable, el sistema MUST rechazar con 500 `INTERNAL_ERROR` genérico, sin débitos, sin crear ni modificar tenencias y sin registrar auditoría. La comprobación ocurre antes de cualquier transferencia y MUST NOT requerir un estado HTTP ni un código de error propio: reutiliza el fallo interno genérico ya definido en el contrato.

### Key Entities

- **Usuario**: Persona registrada con un rol, una cuenta de créditos y tenencias. El superusuario es el propietario inicial de los tokens y el vendedor de las compras de esta funcionalidad; no compra.
- **Jugador**: Recurso del catálogo cuya elegibilidad se valida antes de comprar.
- **Tenencia**: Cantidad de tokens de un jugador propiedad de un usuario.
- **Cuenta de créditos**: Recursos disponibles de un usuario para operar en el mercado.
- **Compra registrada**: Operación confirmada entre comprador y vendedor para un jugador, con su detalle financiero y los estados anteriores y posteriores.
- **Cotización vigente**: Precio actual de un token de un jugador, utilizado para determinar el importe de la compra.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cada compra confirmada entrega los datos de FR-018 y transfiere exactamente la cantidad solicitada y su importe, incluida la primera compra de un jugador por el usuario.
- **SC-008**: Después de compras sucesivas, el total de tokens por jugador emitido sigue siendo 100 y la suma de créditos de las partes no cambia por las transferencias.
- **SC-009**: Ninguna compra confirmada o rechazada deja créditos o tenencias negativos.
- **SC-010**: Hay exactamente un registro completo por compra confirmada y ninguno de compra exitosa por operación rechazada. Ante cualquier fallo, incluido un importe no representable o un fallo de registro, los saldos y tenencias quedan intactos. Los registros históricos conservan su contenido durante las operaciones normales.
- **SC-011**: Las compras simultáneas confirman únicamente cantidades que pueden abastecerse y financiarse, sin doble gasto ni sobreventa, y conservan los resultados SC-008–SC-010.
- **SC-012**: Cada jugador recibe una sola emisión de 100 tokens; repetir su habilitación o reiniciar no incrementa el total ni restaura inventario vendido (AC17).
- **SC-013**: Cada usuario, nuevo o preexistente, recibe su saldo inicial una sola vez; no hay altas confirmadas sin saldo inicial ni restauración de créditos consumidos (AC10/18).
- **SC-014**: Las respuestas de aceptación y rechazo coinciden con el contrato público documentado en OpenAPI, incluidos campos, unidades monetarias, autenticación, errores y ausencia de revelaciones prohibidas.
- **SC-016**: Dos solicitudes iguales que cumplen las condiciones producen dos compras con identificadores y registros financieros distintos.

**Referencias consolidadas**: SC-002–SC-007 quedan integrados en SC-014, con sus comportamientos cubiertos por AC02/03/04/06/07/15. SC-015 queda integrado en SC-010 y SC-014, con cobertura en AC13. Estos IDs se conservan como referencias, no como criterios adicionales.

## Assumptions

No se requieren supuestos funcionales adicionales a las decisiones aprobadas y las dependencias siguientes.

### Dependencias funcionales

- **SPEC-001/003**: registro, identificación y autenticación de usuarios; distinción entre usuario normal y superusuario único.
- **SPEC-002/007**: catálogo con existencia y elegibilidad vigentes de los jugadores.
- **REQ-S2-03**: cotización vigente por token. Su cálculo y la fórmula de evolución no pertenecen a la compra.
- **Prerrequisitos de US1**: saldo inicial e inventario emitido conforme a AC10/17/18. Sin precio válido, vendedor o recursos disponibles no se confirma la compra; la ausencia de vendedor se rechaza explícitamente conforme a FR-023 y AC19.

Los objetivos de rendimiento generales corresponden al proyecto, conforme a TP §5.3; no se fija aquí un umbral no acordado ni se confunde rapidez con integridad de compras simultáneas.

## Clarifications

### Session 2026-10-14

| ID | Decisión vigente |
| --- | --- |
| CL-01 | Importes en centésimas: 1 crédito = 100 unidades. |
| CL-02 | La cotización consumida expresa precio por token; el precio inicial definido por TP es 1 crédito. |
| CL-03 | La compra consume el precio vigente; la fórmula y creación de cotizaciones pertenecen a REQ-S2-01/03. |
| CL-04 | Se conservan los estados anteriores y posteriores de saldos y tenencias, y el precio aplicado con su procedencia; comprar no cambia la cotización. |
| CL-05 | El superusuario no puede comprar tokens, independientemente de su saldo de créditos o de la disponibilidad de tokens del jugador. |
| D1 | Cada solicitud válida es una compra nueva, aunque repita una anterior. |
| D2 | Usuario normal inicia con 100 créditos y superusuario con 0; incluye usuarios preexistentes. El alta y el saldo inicial se confirman juntos, sin restauración posterior. |
| D4 | Si un importe no puede representarse correctamente, no hay movimientos ni compra confirmada y se responde 500 genérico. |
| D5 | Stock insuficiente: rechazo total sin revelar stock; tampoco se revela saldo en errores de créditos. |

### Session 2026-10-15

- **D3 — opción A aprobada**: se comprueba elegibilidad al validar la compra. Garantizar un orden estricto con una desactivación simultánea posterior es recomendado y no bloqueante; no cambia las garantías obligatorias entre compradores simultáneos.
- **Emisión inicial**: 100 tokens por jugador, propiedad inicial del superusuario, una sola vez y sin reposición del inventario vendido (FR-016).

## Precondición operativa de recuperación (H06)

El arranque normal utiliza aprovisionamiento habilitado antes de admitir sincronizaciones normales
del catálogo. No se ejecutan sincronizaciones concurrentes con aprovisionamientos manuales
posteriores. La recuperación no proporciona coordinación distribuida ni garantiza atomicidad
global entre múltiples instancias. La ausencia de emisión, no el stock cero o parcialmente
vendido, identifica a un jugador pendiente.

## Out of Scope

- Venta, ofertas y mercado secundario (REQ-S2-07).
- Portfolio y consulta de historial (REQ-S2-09/10); el registro sí queda preparado para futuras consultas.
- Cálculo, recálculo, consulta y fórmula de cotizaciones (REQ-S2-01/03/04/05/11).
- Frontend, scraping, recargas, comisiones, spreads, órdenes pendientes y ejecuciones parciales.
- Ordenamiento estricto compra/desactivación y reparación automática de alteraciones manuales ajenas a las operaciones normales.

## Trazabilidad

- **REQ origen**: [REQ-S2-06](https://trello.com/c/aJe4VVNt).
- **Fuentes**: `tp/enunciado.md` §3.3, §4, §5.2 y §6; `tp/entregas.md`, Sprint 2 (03/11/2026).

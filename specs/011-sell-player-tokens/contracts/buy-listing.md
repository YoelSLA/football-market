# Contrato HTTP: Compra de una publicación

Endpoint: `POST /api/orders/buy`, en su variante secundaria.

Permite a un usuario autenticado (rol `USER`) comprar la **totalidad** de una publicación creada por
otro usuario. Transfiere tokens y créditos de forma atómica, marca la publicación como `SOLD` y
registra la operación.

La variante **directa** de este mismo endpoint, la compra de tokens contra el superusuario, no se
modifica. Su contrato vigente es
[`../010-buy-tokens/contracts/buy-tokens.md`](../../010-buy-tokens/contracts/buy-tokens.md).

---

## Convivencia de las dos modalidades

Una sola ruta sirve ambas compras y se distinguen por la presencia de `listingId` en el cuerpo.

| Solicitud | Modalidad | Contraparte | Referencia en la auditoría |
|---|---|---|---|
| `{"playerId": 7821, "quantity": 5}` | **Directa** | Superusuario | `listing_id` nula |
| `{"listingId": 25}` | **Secundaria** | Vendedor de la publicación | `listing_id` informada |

| Situación | Resultado |
|---|---|
| Solo `playerId` y `quantity` | Compra directa, según el contrato vigente de SPEC-010 |
| Solo `listingId` | Compra secundaria, según este documento |
| Ambos | `listingId` identifica la publicación; el resto de campos se ignora y no altera la operación |
| Ninguno | 400 `INVALID_REQUEST` |

La compra secundaria determina **jugador, cantidad, precio unitario y vendedor exclusivamente desde
la publicación**. Los datos de jugador, cantidad o precio que acompañen a `listingId` no influyen en
la operación ni en el importe liquidado.

---

## Request — compra secundaria

```http
POST /api/orders/buy
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "listingId": 25
}
```

| Campo | Tipo | Reglas |
|---|---|---|
| `listingId` | integer (Long) | Identificador de la publicación. Debe ser un entero JSON válido. Un entero `0` o negativo no corresponde a ninguna publicación y responde 404 `LISTING_NOT_FOUND`, no 400 |

### Validación estricta de `listingId`

`listingId` aplica el mismo criterio estricto que SPEC-010 usa para `quantity`: solo se acepta un
entero JSON dentro del rango representable, sin coerción, redondeo ni truncamiento.

| `listingId` recibido | Resultado |
|---|---|
| Ausente o `null` | 400 `INVALID_REQUEST` |
| `"25"`, `"abc"`, `true`, `false`, `[]`, `{}` | 400 `INVALID_REQUEST` |
| `25.0`, `1e1` | 400 `INVALID_REQUEST` |
| Entero fuera del rango representable de `Long` | 400 `INVALID_REQUEST` |
| `0` o negativo | 404 `LISTING_NOT_FOUND`: identificador bien formado que no corresponde a ninguna publicación |
| Entero positivo | Continúa el caso de uso; no garantiza una compra exitosa |

El criterio es el de `StrictIntegerDeserializer`, que rechaza todo nodo JSON que no sea entero. Ese
componente está tipado a `Integer`, de modo que aplicarlo a `listingId` requiere una variante para
`Long`; el comportamiento observable es el de la tabla anterior.

---

## Response 200 OK

La respuesta comparte la forma de la compra directa, con un campo adicional opcional.

```json
{
  "transactionId": 12345,
  "playerId": 7821,
  "quantity": 5,
  "unitPrice": 100,
  "totalAmount": 500,
  "executedAt": "2026-10-17T16:00:00.123Z",
  "listingId": 25
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `transactionId` | integer (Long) | Identificador del registro financiero de la venta |
| `playerId` | integer (Long) | Jugador, tomado de la publicación |
| `quantity` | integer | Cantidad transferida, la totalidad de la publicación |
| `unitPrice` | integer (Long) | Precio unitario **congelado** en la publicación, en centésimas |
| `totalAmount` | integer (Long) | Importe total en centésimas |
| `executedAt` | string (ISO-8601) | Instante de confirmación de la operación |
| `listingId` | integer (Long) | Publicación comprada. `null` o ausente en la compra directa |

Los campos preexistentes conservan su nombre, tipo, unidad y significado. Añadir `listingId` es un
cambio aditivo y compatible.

---

## Errores de la compra secundaria

| Estado | `code` | Causa | Efectos |
|---|---|---|---|
| 400 | `INVALID_REQUEST` | Cuerpo ilegible, `listingId` ausente o con formato no numérico, o cuerpo sin ningún dato identificable | Ninguno |
| 401 | — | Sesión ausente, inválida o vencida | Ninguno |
| 403 | `SUPERUSER_CANNOT_BUY` | El solicitante tiene rol `SUPERUSER` | Ninguno |
| 403 | `CANNOT_BUY_OWN_LISTING` | La publicación pertenece al propio comprador | Ninguno |
| 404 | `LISTING_NOT_FOUND` | La publicación no existe, o no es visible para el solicitante porque su jugador no es visible | Ninguno |
| 409 | `LISTING_NOT_AVAILABLE` | La publicación no está efectivamente activa y vigente; o el vendedor no tiene una cuenta de créditos válida para recibir el pago | Ninguno |
| 409 | `INSUFFICIENT_CREDITS` | El saldo del comprador no cubre el importe total | Ninguno |
| 409 | `INSUFFICIENT_STOCK` | La tenencia del vendedor no cubre la cantidad publicada | Ninguno |
| 500 | `INTERNAL_ERROR` | Fallo interno, incluido un importe no representable o un fallo al registrar la auditoría | Operación revertida por completo |

### Indistinguibilidad de motivos

`LISTING_NOT_AVAILABLE` **no distingue** públicamente entre publicación `SOLD`, `CANCELLED`,
efectivamente `EXPIRED` ni la ausencia de una cuenta de créditos válida en el vendedor. La respuesta
no menciona al vendedor ni revela ese motivo.

### No revelación

- `INSUFFICIENT_CREDITS` no revela el saldo del comprador ni el importe requerido.
- `INSUFFICIENT_STOCK` no revela la tenencia del vendedor.
- `LISTING_NOT_FOUND` no revela que la publicación exista cuando no es visible para el solicitante.

### Atomicidad

Ante cualquier rechazo **no** se altera ningún saldo, ninguna tenencia ni el estado de la
publicación, y no se registra ninguna transacción. Lo mismo ante un fallo interno: la operación se
revierte por completo y no queda registro de una venta no confirmada.

---

## Efectos observables de una compra confirmada

| Efecto | Detalle |
|---|---|
| Tenencias | El comprador recibe exactamente la cantidad publicada; el vendedor la pierde |
| Créditos | El comprador se debita y el vendedor se acredita exactamente el importe total, sin crear ni perder créditos |
| Publicación | Pasa a `SOLD` con su momento de cierre informado |
| Registro financiero | Exactamente uno, con comprador, vendedor, jugador, cantidad, precio aplicado, importe, instante, y estados de saldos y tenencias anteriores y posteriores de ambas partes |

La transferencia es **completa**: no existen compras parciales, fraccionadas ni solicitudes de compra
pendientes.

---

## Concurrencia

Cada publicación se liquida **a lo sumo una vez**.

| Escenario | Resultado |
|---|---|
| Dos compradores simultáneos de la misma publicación | Exactamente uno se confirma y registra la venta; el otro recibe 409 `LISTING_NOT_AVAILABLE`. No hay doble transferencia ni saldo negativo, y existe un único registro financiero |
| Compra y cancelación simultáneas de la misma publicación | Una se confirma y la otra recibe 409 `LISTING_NOT_AVAILABLE`. No hay estado intermedio observable |
| Dos compradores con saldo para una sola compra | Solo una se confirma; la otra recibe 409 `INSUFFICIENT_CREDITS`. No hay saldo negativo |
| Varias publicaciones simultáneas del mismo vendedor que superan su tenencia | Solo se crean las que la disponibilidad permite; la suma de reservas nunca excede la tenencia |

### Vencimiento y nuevas reservas durante una compra — C1

La compra adquiere `user_credits` → `token_listings` → `player_tokens`, con identificadores
ordenados dentro de cada categoría. Solo después de bloquear la publicación y **ambas tenencias**
obtiene `validatedAt` y comprueba una única vez `validatedAt < expiresAt`. No puede conservar una
decisión temporal obtenida antes de esperar por las tenencias. Los bloqueos se mantienen hasta el
commit o rollback, incluyendo transferencia de tokens, créditos, cambio a `SOLD` y auditoría.

- Si la oferta vence antes de esa validación, incluso durante una espera por las tenencias, responde
  `409 LISTING_NOT_AVAILABLE` y revierte toda preparación, sin movimientos ni auditoría de venta.
- Si supera la validación, puede confirmarse aunque el reloj alcance `expiresAt` después. No se
  efectúa otra validación temporal al cerrar ni al confirmar.
- Publicar tokens del mismo vendedor y jugador requiere la misma tenencia. Si la compra la retiene,
  publicar espera y calcula disponibilidad después de obtener el bloqueo: con la tenencia reducida
  tras una compra confirmada, o con el estado restaurado tras rollback. El vencimiento no permite
  saltar ese bloqueo ni reutilizar tokens comprometidos por una compra ya validada.

Ejemplo: con tenencia 20 y compra de 5, una nueva publicación de 20 puede confirmarse si precede a
una compra que al obtener la tenencia ya encuentra la oferta vencida y se rechaza. Si la compra
supera primero la validación temporal y confirma, quedan 15 tokens: la publicación concurrente de
20 se rechaza con `INSUFFICIENT_STOCK`. Nunca pueden confirmarse ambas dejando reserva 20 sobre
tenencia 15. Cobertura prevista en T029/T031 y `quickstart.md`, subescenario 9c; no ejecutada.

No se implementan reintentos automáticos. Ante un conflicto de bloqueo la transacción falla y el
cliente decide si reintenta.

---

## Reintentos e idempotencia

La compra secundaria **no es idempotente por solicitud**, pero sí lo es **por recurso**: la
publicación solo puede liquidarse una vez.

- Si el cliente repite la compra de una publicación ya comprada, la segunda solicitud recibe
  409 `LISTING_NOT_AVAILABLE`, porque la publicación quedó `SOLD`. No se genera una segunda compra
  ni un segundo registro financiero.
- No existe clave de idempotencia ni campo de deduplicación en el cuerpo.
- Si el cliente no recibe respuesta, no puede asumir que la compra haya fallado: reintentar produce un
  409, no una compra duplicada.

Esta propiedad **no** altera la decisión vigente de SPEC-010 para la compra directa, donde cada
solicitud válida representa una compra nueva.

---

## Modelo de datos

Ver [../data-model.md](../data-model.md) para el esquema, los índices, el orden total de bloqueos y
las consultas de revalidación bajo bloqueo.

## Auditoría

Cada venta confirmada escribe un registro en `market_transactions` en la misma transacción que las
transferencias, con `listing_id` informada. Si el registro falla, revierte la compra completa. Los
registros no se modifican ni se eliminan durante las operaciones normales del sistema.

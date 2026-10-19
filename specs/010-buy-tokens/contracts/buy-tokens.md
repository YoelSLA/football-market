# Contrato HTTP: Comprar tokens

Endpoint: `POST /api/orders/buy`

Permite a un usuario autenticado (role=USER) comprar tokens de un jugador contra el inventario inicial del superusuario.

## Extensión compatible de SPEC-011

Este documento conserva el contrato de la **modalidad directa**, sin `listingId`: `playerId` y
`quantity` siguen siendo obligatorios y mantienen sus validaciones y errores. Si se informa un
`listingId` no nulo, se selecciona la compra de una publicación completa, cuyos datos prevalecen
sobre `playerId` y `quantity`; esa modalidad se rige por
[el contrato de SPEC-011](../../011-sell-player-tokens/contracts/buy-listing.md).
La respuesta añade una referencia opcional `listingId`, ausente en compras directas; los seis campos
existentes conservan su significado. Una solicitud sin publicación ni datos completos de compra
directa sigue rechazándose con `400 INVALID_REQUEST` antes de ejecutar el caso de uso.

---

## Request

```http
POST /api/orders/buy
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "playerId": 7821,
  "quantity": 10
}
```

### Parámetros (Body)

| Campo | Tipo | Requerido | Reglas |
|-------|------|-----------|--------|
| `playerId` | integer (Long) | Sí | Identificador interno del jugador; debe existir, estar activo y visible. Un valor numérico no positivo es un identificador válido en formato que no corresponde a ningún jugador: responde 404 `PLAYER_NOT_FOUND`, no 400. |
| `quantity` | integer | Sí | Literal entero JSON entre 1 y 100; sin coerciones. Errores de tipo o representación responden `INVALID_REQUEST`; enteros representables como Integer fuera de 1–100 responden `INVALID_QUANTITY`. |

---

## Response 200 OK

```json
{
  "transactionId": 12345,
  "playerId": 7821,
  "quantity": 10,
  "unitPrice": 100,
  "totalAmount": 1000,
  "executedAt": "2026-10-14T10:30:00.123Z"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `transactionId` | integer (Long) | Identificador de la transacción registrada en auditoría. |
| `playerId` | integer (Long) | Identificador interno del jugador. |
| `quantity` | integer | Cantidad de tokens comprados. |
| `unitPrice` | integer (Long) | Precio unitario por token en **centésimas de crédito** (100 = 1.00 crédito). |
| `totalAmount` | integer (Long) | Importe total = `quantity * unitPrice` en centésimas. |
| `executedAt` | string (ISO-8601) | Timestamp de ejecución de la transacción. |

---

## Error Responses

### 400 Bad Request — Body ilegible o campo ausente

Error **estructural de deserialización HTTP**, no una regla de negocio de compra. No ejecuta lógica de compra.

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_REQUEST",
  "message": "Solicitud inválida",
  "path": "/api/orders/buy"
}
```

Causas: cuerpo ilegible, `playerId` ausente o con formato no numérico, o errores de estructura, tipo o representación de `quantity`. Para `quantity`, se rechazan antes de invocar `BuyOrderService`: ausencia, `null`, string (incluso numérico), booleano, array, objeto, representación decimal (incluido `10.0`), notación exponencial (incluido `1e1`) y enteros fuera del rango de Integer (−2147483648 a 2147483647).

| `quantity` | Resultado |
| --- | --- |
| Ausente o `null` | 400 `INVALID_REQUEST` |
| `"10"`, `"abc"`, `true`, `false`, `[]`, `{}` | 400 `INVALID_REQUEST` |
| `1.5`, `10.0`, `1e1` | 400 `INVALID_REQUEST` |
| `2147483648`, `-2147483649` | 400 `INVALID_REQUEST` |
| `-1`, `0`, `101` (enteros representables) | 400 `INVALID_QUANTITY` |
| `1` a `100` (literales enteros) | Continúa el caso de uso; no garantiza una compra exitosa |

No se permiten coerciones implícitas, strings numéricos, redondeo ni truncamiento. La validez de la cantidad no omite autorización, créditos, inventario ni cotización.

### 400 Bad Request — Cantidad inválida

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_QUANTITY",
  "message": "La cantidad debe ser un entero entre 1 y 100",
  "path": "/api/orders/buy"
}
```

Causas: `quantity` es un literal entero JSON representable como Integer, pero ≤ 0 o > 100. Los errores de estructura, tipo o representación responden `INVALID_REQUEST`, como se detalla en la sección anterior.

El valor se valida como entero. No se aceptan fracciones ni se aplica redondeo o truncamiento.

---

### 403 Forbidden — Superusuario no puede comprar

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 403,
  "error": "Forbidden",
  "code": "SUPERUSER_CANNOT_BUY",
  "message": "El superusuario no puede comprar tokens",
  "path": "/api/orders/buy"
}
```

El comprador autenticado tiene `role = SUPERUSER`. El rol se lee de la persistencia, no de los claims del token. La prohibición no depende del saldo ni de la disponibilidad de tokens.

---

### 404 Not Found — Jugador inexistente

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 404,
  "error": "Not Found",
  "code": "PLAYER_NOT_FOUND",
  "message": "No encontramos el jugador solicitado",
  "path": "/api/orders/buy"
}
```

El `playerId` no corresponde a ningún jugador en el catálogo.

#### Identificador no positivo

Un `playerId` con valor `0` o negativo responde **404 `PLAYER_NOT_FOUND`**, no 400 `INVALID_REQUEST`.

| Pregunta | Respuesta | Fundamento |
|---|---|---|
| ¿La SPEC lo prohíbe? | No | FR-004 exige un entero y no impone positividad |
| ¿Existe una regla nueva? | No | No se crea ninguna validación adicional |
| ¿Qué hace el backend hoy con `GET /api/players/{id}`? | Resuelve por consulta; un id inexistente produce ausencia natural | `getVisiblePlayerById` no valida el rango del identificador |
| ¿Qué es más coherente? | Tratarlo como identificador que no identifica a nadie | Introduce una regla de positividad que ningún endpoint del proyecto aplica |

**Justificación**: `0` y los negativos son enteros correctamente deserializables, por lo que no hay error estructural. No varían en la capa de contrato HTTP ni requieren un código nuevo: encajan en `PLAYER_NOT_FOUND`, que ya significa «no corresponde a ningún jugador». La decisión mantiene el mismo tratamiento en Controller, Service y contrato, porque la comprobación de existencia que hace la fachada es una consulta por identificador al catálogo, y un identificador que no existe —porque sea `0`, negativo o cualquier otro— produce la misma ausencia.

La positividad del `quantity` sí se valida, porque es una regla funcional explícita de FR-005 con su propio código `INVALID_QUANTITY`. La diferencia es intencional: la cantidad tiene rango de negocio; el identificador no.

Esta decisión **no** limita la capacidad de la compra de distinguir los dos 404. La fachada resuelve primero la existencia del jugador y después su visibilidad, y esa distinción es la que separa `PLAYER_NOT_FOUND` de `PLAYER_NOT_VISIBLE`; el detalle del mecanismo está en `plan.md`. Lo que aquí se decide es únicamente que un identificador no positivo se resuelve por ausencia, no por una regla de positividad añadida.

**Comportamiento esperado:**

| Entrada | Respuesta |
|---|---|
| `{"playerId": 0, "quantity": 10}` | 404 `PLAYER_NOT_FOUND` |
| `{"playerId": -1, "quantity": 10}` | 404 `PLAYER_NOT_FOUND` |
| `{"playerId": "abc", "quantity": 10}` | 400 `INVALID_REQUEST` (error estructural) |
| `{"quantity": 10}` sin `playerId` | 400 `INVALID_REQUEST` (campo ausente) |

No hay transferencias ni auditoría en ninguno de estos casos.

---

### 404 Not Found — Jugador no visible

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 404,
  "error": "Not Found",
  "code": "PLAYER_NOT_VISIBLE",
  "message": "No encontramos el jugador solicitado",
  "path": "/api/orders/buy"
}
```

El jugador existe en el catálogo pero no cumple el predicado vigente de visibilidad (jugador activo y equipo vigente). El mensaje coincide con el de `PLAYER_NOT_FOUND`, pero el código es distinto: la compra no promete ocultación absoluta de existencia.

---

### 409 Conflict — Stock insuficiente

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 409,
  "error": "Conflict",
  "code": "INSUFFICIENT_STOCK",
  "message": "No hay suficientes tokens disponibles para completar la compra.",
  "path": "/api/orders/buy"
}
```

El superusuario tiene menos tokens disponibles que la `quantity` solicitada.

El rechazo es **total**: no se ejecuta ninguna compra parcial y no se modifica ningún saldo ni
tenencia. La respuesta **no revela el stock disponible**, ni ningún campo equivalente. La misma
regla de no revelación aplica a `INSUFFICIENT_CREDITS`.

---

### 409 Conflict — Créditos insuficientes

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 409,
  "error": "Conflict",
  "code": "INSUFFICIENT_CREDITS",
  "message": "No tenés créditos suficientes para completar la compra.",
  "path": "/api/orders/buy"
}
```

El comprador no tiene suficientes créditos para `quantity * unitPrice`.

---

### 409 Conflict — Precio vigente no disponible

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 409,
  "error": "Conflict",
  "code": "QUOTE_UNAVAILABLE",
  "message": "No hay precio vigente para el jugador",
  "path": "/api/orders/buy"
}
```

No hay cotización vigente disponible para el `playerId`. La fuente de cotización pertenece a
REQ-S2-03; mientras no exista una integración real, el simulador solo se usa en desarrollo y
pruebas de forma explícita.

---

### 409 Conflict — Precio inválido

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 409,
  "error": "Conflict",
  "code": "INVALID_QUOTE_PRICE",
  "message": "El precio vigente debe ser mayor que cero",
  "path": "/api/orders/buy"
}
```

El precio vigente disponible para el jugador es ≤ 0 (centésimas).

---

### 401 Unauthorized — Sesión inválida

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_CREDENTIALS",
  "message": "Las credenciales proporcionadas son incorrectas",
  "path": "/api/orders/buy"
}
```

JWT ausente, inválido, expirado o sujeto sin usuario persistido. Se reutiliza el 401 de la cadena de seguridad existente (SPEC-001/003).

El cuerpo de esta respuesta **no está garantizado** por `ErrorResponseDTO`: la cadena de seguridad no tiene un punto de entrada propio que produzca ese cuerpo, y el ejemplo anterior corresponde al error controlado de credenciales de `POST /api/auth/login`, que es un contrato distinto. La implementación y los tests deben afirmar el comportamiento real de esta ruta, sin asumir un cuerpo que el proyecto no emite.

---

### 500 Internal Server Error — Fallo interno de la compra

```json
{
  "timestamp": "2026-10-14T10:30:00.123Z",
  "status": 500,
  "error": "Internal Server Error",
  "code": "INTERNAL_ERROR",
  "message": "No se pudo procesar la compra. Intentá nuevamente más tarde.",
  "path": "/api/orders/buy"
}
```

Cubre el desbordamiento del rango de la moneda al calcular o actualizar importes, entre otros fallos
internos de la compra.

También cubre la ausencia de superusuario identificable como vendedor (FR-023, AC19). No existe un código
propio para ese caso: la compra rechaza antes de mover recursos y responde con este mismo 500 genérico, sin
débitos, sin tenencias creadas o modificadas y sin registro de auditoría.

Garantías:

- La operación se revierte **completa**: no transfiere tokens, no modifica saldos y **no** registra
  compra.
- El `code` público es genérico. El identificador técnico de diagnóstico se conserva **solo en
  registros internos** y **no** se expone.
- No se exponen excepciones, trazas ni detalles de implementación.

---

## Estructura de las respuestas de error

Los errores controlados por la aplicación usan el contrato global `ErrorResponseDTO`
(`docs/backend/architecture.md` §1.7), con los seis campos obligatorios:

| Campo | Contenido |
|-------|-----------|
| `timestamp` | Instante de la respuesta. |
| `status` | Código HTTP; coincide con el de la línea de estado. |
| `error` | Razón HTTP estándar (`Bad Request`, `Conflict`, `Internal Server Error`). **No** es el identificador del error. |
| `code` | Identificador estable y programático del tipo de error. |
| `message` | Texto legible en español; nunca sirve como identificador. |
| `path` | Ruta solicitada. |

La respuesta 401 se emite por la cadena de seguridad y queda fuera de este contrato, como se
aclara en su sección.

Los identificadores técnicos internos nunca aparecen en `code` ni en ningún campo de la respuesta.
Se conservan solo en el registro interno de diagnóstico.

## Seguridad

- Requiere autenticación Bearer JWT válida (Spring Security OAuth2 Resource Server).
- El rol se resuelve desde la persistencia (`UserRole`), no desde claims del token.
- Endpoint no accesible para superusuarios, independientemente de su saldo o del stock del jugador.

---

## Auditoría

Cada compra exitosa registra una `MarketTransaction` append-only para operaciones normales, en la misma transacción PostgreSQL que los movimientos, con:
- Identificador, comprador, vendedor (superusuario), jugador, cantidad, precio unitario, importe total, timestamp.
- Autor: comprador autenticado persistido, nunca un identificador proporcionado en el body.
- Estados anteriores/posteriores de saldos y tenencias; precio aplicado y procedencia de la cotización conservados como datos de la operación (formato interno en `data-model.md`).
- Si falla el registro, revierte la compra. Sin modificación, eliminación, retención ni cascadas sobre auditoría histórica; no se promete resistencia a SQL privilegiado.

---

## Reintentos e idempotencia

`POST /api/orders/buy` **no es idempotente**. Cada solicitud válida representa una compra nueva.

- Si el cliente repite una solicitud, **se ejecuta otra compra** siempre que existan créditos y
  stock suficientes. La repetición no se rechaza por ser una repetición.
- No se implementa ni se exige clave de idempotencia, token de deduplicación ni mecanismo
  equivalente. El cuerpo de la solicitud no contiene ningún campo de deduplicación.
- No hay reintentos automáticos ante fallos técnicos: la transacción se revierte y el cliente
  decide si vuelve a solicitar.
- Esta decisión es **independiente** de otras dos idempotencias de la feature:
  - La inicialización de saldos de créditos es idempotente por usuario y nunca duplica ni
    restaura saldos.
  - La emisión inicial de tokens es idempotente por jugador y nunca repone inventario vendido.
- Un reintento tras timeout puede producir otra compra. No se promete evitarla consultando saldo, stock o un historial futuro; esta SPEC no implementa esos endpoints ni un mecanismo de deduplicación.

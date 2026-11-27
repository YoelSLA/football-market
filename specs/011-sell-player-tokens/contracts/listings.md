# Contrato HTTP: Publicaciones

Endpoints de publicación, consulta y cancelación de offerings de tokens.

| Método | Ruta | Propósito |
|---|---|---|
| POST | `/api/orders/sell` | Publicar tokens que el usuario posee |
| GET | `/api/listings` | Listar publicaciones disponibles en el mercado (opcionalmente por jugador) |
| GET | `/api/market/p2p` | Catálogo P2P paginado con publicaciones activas agrupadas por jugador |
| GET | `/api/listings/mine` | Listar las publicaciones del usuario autenticado |
| GET | `/api/listings/{id}` | Consultar el detalle de una publicación |
| POST | `/api/listings/{id}/cancel` | Cancelar una publicación propia y vigente |

La compra de una publicación se documenta en [buy-listing.md](buy-listing.md).

**No existe `POST /api/listings`.** La única vía de creación de publicaciones es
`POST /api/orders/sell`. `GET /api/listings` es exclusivamente de consulta: no publica, no modifica ni
crea recursos. Tampoco existe ninguna vía alternativa de creación, ni una ruta de solicitudes de
compra pendientes.

Todos los endpoints requieren sesión autenticada. El rol y la pertenencia se resuelven desde la
persistencia, nunca desde claims del token.

---

## POST /api/orders/sell

Publica una cantidad de tokens que el usuario posee. **Reserva** los tokens, congela el precio
unitario y **no transfiere** tokens ni créditos, ni genera ninguna transacción de venta.

### Request

```http
POST /api/orders/sell
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "playerId": 7821,
  "quantity": 5,
  "unitPrice": 85
}
```

| Campo | Tipo | Requerido | Reglas |
|---|---|---|---|
| `playerId` | integer (Long) | Sí | Identificador interno. Debe existir y estar visible. Un valor no positivo responde 404 `PLAYER_NOT_FOUND`, no 400 |
| `quantity` | integer | Sí | Literal entero entre 1 y 100, sin coerción, redondeo ni truncamiento |
| `unitPrice` | integer (Long) | No | Precio unitario opcional en centésimas. Si se omite se usa la cotización vigente; si se informa debe ser un entero JSON positivo entre el 80% y el 120%, inclusive, de esa cotización. Sin coerción decimal o textual. |

### Response 201 Created

El código `201 Created` sigue la convención ya establecida por el proyecto para operaciones que crean
un recurso: SPEC-001 lo exige para el registro de usuario (`FR-010` de esa especificación) y el
backend lo implementa con `HttpStatus.CREATED`. La respuesta de esta ruta difiere de la de registro en
que **sí** incluye el cuerpo con la representación del recurso creado y su `listingId`, lo que
permite operar sobre la publicación sin una consulta adicional. La divergencia respecto de la
convención es acotada y deliberada: `201` identifica la creación y el cuerpo transporta el recurso.


```json
{
  "listingId": 25,
  "playerId": 7821,
  "quantity": 5,
  "unitPrice": 85,
  "totalAmount": 425,
  "status": "ACTIVE",
  "createdAt": "2026-10-17T15:30:00.123Z",
  "expiresAt": "2026-10-18T15:30:00.123Z"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `listingId` | integer (Long) | Identificador de la publicación creada |
| `playerId` | integer (Long) | Jugador publicado |
| `quantity` | integer | Cantidad reservada |
| `unitPrice` | integer (Long) | Precio unitario congelado, en **centésimas de crédito** |
| `totalAmount` | integer (Long) | `quantity * unitPrice`, en centésimas |
| `status` | string | Siempre `ACTIVE` al crearse |
| `createdAt` | string (ISO-8601) | Instante de creación |
| `expiresAt` | string (ISO-8601) | Instante de vencimiento, 24 horas después de la creación |

Una publicación creada nunca arranca en `SOLD`, `CANCELLED` ni `EXPIRED`.
El precio e importe acordados quedan congelados en la publicación aunque cambie la cotización
posteriormente. Si `unitPrice` se omite, el precio congelado es la cotización vigente al crearla.
Cuando se omite el campo, el ejemplo de respuesta usa `unitPrice: 100` y `totalAmount: 500`.

### Errores

| Estado | `code` | Causa |
|---|---|---|
| 400 | `INVALID_REQUEST` | Cuerpo ilegible, `playerId` ausente o no numérico, `quantity` nula, decimal, exponencial, de tipo incorrecto o fuera del rango de Integer |
| 400 | `INVALID_QUANTITY` | `quantity` es un literal entero representable pero ≤ 0 o > 100 |
| 401 | — | Sesión ausente, inválida o vencida |
| 403 | `SUPERUSER_CANNOT_SELL` | El solicitante tiene rol `SUPERUSER` |
| 404 | `PLAYER_NOT_FOUND` | El identificador no corresponde a ningún jugador |
| 404 | `PLAYER_NOT_VISIBLE` | El jugador existe pero no cumple el predicado de visibilidad |
| 409 | `INSUFFICIENT_STOCK` | La cantidad supera la disponibilidad del vendedor, es decir su tenencia menos las reservas de publicaciones efectivamente activas y no vencidas |
| 409 | `QUOTE_UNAVAILABLE` | No hay cotización vigente para el jugador |
| 409 | `INVALID_QUOTE_PRICE` | La cotización vigente no es positiva |
| 409 | `INVALID_LISTING_PRICE` | `unitPrice` es no positivo o queda fuera del intervalo inclusivo del 80% al 120% de la cotización vigente |
| 500 | `INTERNAL_ERROR` | Fallo interno; la operación se revierte por completo |

Ningún rechazo crea publicación ni altera reservas, saldos, tenencias ni transacciones.

**Terminología de la disponibilidad**: *tenencia* es la cantidad que el usuario posee;
*reservada* es la suma de cantidades de publicaciones efectivamente activas y no vencidas;
*disponible* es la diferencia entre ambas. El rechazo por `INSUFFICIENT_STOCK` se dispara cuando la
cantidad solicitada supera la **disponible**, nunca la tenencia total. Esa respuesta **no revela** ni
la tenencia ni la cantidad reservada ni la disponible.

**Exclusión con compras en curso (C1)**: publicar bloquea la tenencia del vendedor y obtiene el
instante usado para sumar reservas y calcular disponibilidad **después** de adquirir ese bloqueo,
que conserva hasta commit o rollback. Una compra toma cuentas → publicación → tenencias antes de
su única validación temporal decisiva y conserva esos bloqueos hasta finalizar. Por eso una nueva
publicación espera si esa compra ya retiene la tenencia, aunque la oferta original venza mientras
espera: no puede reutilizar los tokens que esa compra aún puede transferir. Al obtener el bloqueo,
calcula disponibilidad con el estado confirmado resultante, nunca con la tenencia anterior a la
compra. No se añade reserva persistida ni se retrasa la expiración efectiva. La política temporal
completa está en [buy-listing.md](buy-listing.md#vencimiento-y-nuevas-reservas-durante-una-compra--c1).

---

## GET /api/listings

Mercado público: publicaciones **efectivamente activas y vigentes de otros usuarios** sobre jugadores
visibles. Admite filtrar por un jugador específico mediante el query parameter opcional `playerId`.

### Request

```http
GET /api/listings?playerId=7821
Authorization: Bearer <JWT>
```

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `playerId` | integer (Long) | No | Identificador opcional del jugador. Si se omite, retorna todas las ofertas del mercado. Si se especifica, retorna solo las ofertas de ese jugador ordenadas por menor precio unitario y antigüedad. |

### Response 200 OK

```json
{
  "listings": [
    {
      "listingId": 25,
      "sellerId": 12,
      "playerId": 7821,
      "quantity": 5,
      "unitPrice": 100,
      "totalAmount": 500,
      "status": "ACTIVE",
      "createdAt": "2026-10-17T15:30:00.123Z",
      "expiresAt": "2026-10-18T15:30:00.123Z"
    }
  ]
}
```

---

## GET /api/market/p2p

Catálogo del Mercado P2P: consulta paginada de jugadores con ofertas activas en la comunidad con agregación en servidor (`listingCount`, `availableQuantity`, `minimumUnitPrice`), filtros deportivos y financieros, ordenamiento determinista y rango global de precios.

### Request

```http
GET /api/market/p2p?page=0&size=8&sort=price,asc&search=Julian&leagueId=1&position=DEL
Authorization: Bearer <JWT>
```

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `page` | integer | No | Índice de página (base 0, defecto 0) |
| `size` | integer | No | Elementos por página (1 a 100, defecto 8) |
| `sort` | string | No | Criterio y dirección: `price,asc`, `price,desc`, `quantity,desc`, `name,asc` |
| `search` | string | No | Búsqueda por coincidencia en nombre de jugador o club |
| `leagueId` | integer | No | Identificador de liga deportiva |
| `teamId` | integer | No | Identificador de equipo o club |
| `position` | string | No | Posición deportiva |
| `minPrice` | integer | No | Precio unitario mínimo en centésimas |
| `maxPrice` | integer | No | Precio unitario máximo en centésimas |

### Response 200 OK

```json
{
  "content": [
    {
      "id": 7821,
      "name": "Julian Alvarez",
      "position": "DEL",
      "nationality": "Argentina",
      "teamId": 10,
      "teamName": "Club Atlético de Madrid",
      "teamBadgeUrl": null,
      "leagueId": 1,
      "leagueName": "LaLiga",
      "imageUrl": "https://images.footballmarket.com/players/7821.png",
      "listingCount": 3,
      "availableQuantity": 25,
      "minimumUnitPrice": 1500
    }
  ],
  "page": 0,
  "size": 8,
  "totalElements": 1,
  "totalPages": 1,
  "priceRange": {
    "min": 1500,
    "max": 5000
  }
}
```
      "sellerId": 12,
      "playerId": 7821,
      "quantity": 5,
      "unitPrice": 100,
      "totalAmount": 500,
      "status": "ACTIVE",
      "createdAt": "2026-10-17T15:30:00.123Z",
      "expiresAt": "2026-10-18T15:30:00.123Z"
    }
  ]
}
```

Se excluyen: las publicaciones propias, las vencidas, las que no estén activas y las de jugadores no
visibles.

---

## GET /api/listings/mine

Todas las publicaciones del usuario autenticado, incluidas las de jugadores no visibles, con filtro
opcional por **estado efectivo**.

### Request

```http
GET /api/listings/mine
Authorization: Bearer <JWT>
```

| Parámetro | Valores admitidos | Efecto |
|---|---|---|
| `status` | `ACTIVE`, `SOLD`, `CANCELLED`, `EXPIRED` | Filtra por estado efectivo |

- Sin `status` se devuelven **todas** las publicaciones propias.
- Solo se aceptan las cuatro formas **exactas**. Cualquier otra capitalización o un valor
  desconocido se rechaza como solicitud inválida: no hay conversión ni comparación sin distinción de
  caja.
- El filtro decide únicamente por el estado efectivo y **no** considera la visibilidad del jugador:
  una publicación vencida de un jugador no visible aparece si su estado efectivo coincide con el
  filtro.

### Errores

| Estado | `code` | Causa |
|---|---|---|
| 400 | `INVALID_PARAMETER_TYPE` | `status` ausente de los valores admitidos o con formato inválido |
| 401 | — | Sesión ausente, inválida o vencida |

---

## GET /api/listings/{id}

Detalle de una publicación.

| Solicitante | Publicación | Respuesta |
|---|---|---|
| Propietario | Cualquier estado, incluido `CANCELLED`, `SOLD` o vencido | 200 con el detalle |
| Propietario | Cualquiera | 200 con el detalle |
| Tercero | `ACTIVE` y vigente, sobre jugador visible | 200 con el detalle |
| Tercero | `SOLD`, `CANCELLED` o vencida | 404 `LISTING_NOT_FOUND` |
| Tercero | Sobre jugador no visible | 404 `LISTING_NOT_FOUND` |
| Cualquiera | Inexistente | 404 `LISTING_NOT_FOUND` |

```json
{
  "listingId": 25,
  "sellerId": 12,
  "playerId": 7821,
  "quantity": 5,
  "unitPrice": 100,
  "totalAmount": 500,
  "status": "ACTIVE",
  "createdAt": "2026-10-17T15:30:00.123Z",
  "expiresAt": "2026-10-18T15:30:00.123Z",
  "closedAt": null
}
```

`status` refleja el **estado efectivo**: una publicación almacenada como `ACTIVE` y ya vencida se
devuelve como `EXPIRED`. Ese estado efectivo **no implica** un cierre persistido: una publicación
vencida conserva su estado almacenado `ACTIVE` y su `closedAt` en `null`, porque no hay tarea que
actualice la fila.

| Estado efectivo | Estado almacenado | `closedAt` |
|---|---|---|
| `ACTIVE` | `ACTIVE` | `null` |
| `SOLD` | `SOLD` | informado, momento de la venta |
| `CANCELLED` | `CANCELLED` | informado, momento de la cancelación |
| `EXPIRED` | `ACTIVE` | `null` — venció sin acción del sistema |

`closedAt` se informa únicamente cuando la publicación se cierra por una operación explícita del
sistema: la venta o la cancelación.

La respuesta no distingue entre publicación inexistente y no visible para el solicitante. Un
identificador no positivo no corresponde a ninguna publicación y responde el mismo 404.

---

## POST /api/listings/{id}/cancel

Cancela una publicación propia, efectivamente activa y vigente.

```http
POST /api/listings/{id}/cancel
Authorization: Bearer <JWT>
```

Sin cuerpo.

### Response 200 OK

Devuelve la publicación ya cancelada, con `status` `CANCELLED` y `closedAt` informado.

### Errores

| Estado | `code` | Causa |
|---|---|---|
| 401 | — | Sesión ausente, inválida o vencida |
| 403 | `ACCESS_DENIED` | La publicación pertenece a otro usuario; usa el error global sin revelar datos de la publicación |
| 404 | `LISTING_NOT_FOUND` | La publicación no existe |
| 409 | `LISTING_NOT_AVAILABLE` | La publicación está `SOLD`, `CANCELLED` o efectivamente `EXPIRED` |

Ningún rechazo modifica la publicación ni las reservas. Cancelar es la única operación que libera
reservas, y lo hace por cancelación del estado: al dejar de estar `ACTIVE` y vigente, la publicación
deja de computar.

---

## Estructura de las respuestas de error

Los errores controlados por la aplicación usan el contrato global `ErrorResponseDTO`
(`docs/backend/architecture.md` §1.7):

| Campo | Contenido |
|---|---|
| `timestamp` | Instante de la respuesta |
| `status` | Código HTTP |
| `error` | Razón HTTP estándar; **no** es el identificador del error |
| `code` | Identificador estable y programático |
| `message` | Texto legible en español; nunca sirve como identificador |
| `path` | Ruta solicitada |

La respuesta 401 la emite la cadena de seguridad y queda fuera de este contrato.

Los identificadores técnicos internos nunca aparecen en `code` ni en ningún campo de la respuesta.

## Modelo de datos

Ver [../data-model.md](../data-model.md) para el esquema de `token_listings`, los índices y las
consultas de bloqueo.

## Reintentos e idempotencia

- `POST /api/orders/sell` **no es idempotente**: cada solicitud válida crea una publicación nueva.
  Repetirla con la misma cantidad puede rechazarse por saldo no reservado insuficiente, pero no
  devuelve la publicación anterior ni su identificador.
- `POST /api/listings/{id}/cancel` **sí es idempotente por recurso**: cancelar dos veces la misma
  publicación devuelve 409 `LISTING_NOT_AVAILABLE` la segunda vez, sin cambios.
- No se implementa clave de idempotencia ni deduplicación.

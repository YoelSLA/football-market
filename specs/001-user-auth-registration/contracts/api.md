# API Contracts: User Registration and Authentication

## Registro

### POST /api/auth/register

Registra un nuevo usuario.

**Request Body** (JSON):

```json
{
  "username": "juan.perez",
  "email": "user@example.com",
  "password": "strongPassword123"
}
```

**Responses**:

- `201 Created`: Usuario registrado exitosamente, sin body.
- `400 Bad Request`: Datos inválidos, como email no válido, nombre de usuario vacío o con formato inválido, o campos vacíos.
- `409 Conflict`: El email ya está registrado (`EMAIL_ALREADY_REGISTERED`) o el nombre de usuario ya está en uso (`USERNAME_ALREADY_TAKEN`).

La contraseña de registro debe tener al menos 8 caracteres. Los errores gestionados por la
aplicación devuelven `timestamp`, `status`, `error`, `message` y `path`; en errores de validación,
`message` contiene el primer mensaje disponible.

**Campos del body**:

| Campo | Obligatorio | Restricciones |
|-------|-------------|---------------|
| `username` | Sí | 3 a 30 caracteres, formado por letras, números, punto, guion y guion bajo; único en el sistema. No distingue mayúsculas de minúsculas. |
| `email` | Sí | Formato de correo electrónico válido, único en el sistema. |
| `password` | Sí | Mínimo 8 caracteres. Se persiste hasheada y nunca se devuelve. |

**Normalización del `username`**: el sistema normaliza el nombre de usuario a minúsculas antes de
persistirlo y antes de comprobar su unicidad. Por lo tanto `Juan.Perez`, `JUAN.PEREZ` y `juan.perez`
identifican el mismo nombre de usuario: el primero que se registra lo hace con la forma normalizada
y los siguientes reciben `409 Conflict` con `code` `USERNAME_ALREADY_TAKEN`. La forma normalizada es
la que devuelve `GET /api/auth/me`.

El body **no** admite rol. Si el cliente envía un campo `role`, el sistema lo ignora y asigna
`USER` al usuario creado.

**Cambio incompatible respecto de la versión anterior**: `username` pasa a ser obligatorio. Los
clientes que lo omitan reciben `400 Bad Request`. No se ofrece un período de compatibilidad
con clientes antiguos.

---

## Autenticación

### POST /api/auth/login

Autentica a un usuario y retorna un JWT.

**Request Body** (JSON):

```json
{
  "email": "user@example.com",
  "password": "strongPassword123"
}
```

**Responses**:

- `200 OK`: Autenticación exitosa.
  - Body:

```json
{
  "token": "JWT_TOKEN_HERE"
}
```

- `401 Unauthorized`: Credenciales inválidas.
- `400 Bad Request`: Email inválido o campos obligatorios vacíos.

El JWT recibido debe enviarse en `Authorization: Bearer <token>` para acceder a recursos
protegidos. El servidor valida su firma y vigencia. La ausencia de token, una firma inválida
o un token expirado producen `401 Unauthorized`.

**Sin cambios en esta ampliación**: el cuerpo del request, el campo `token` de la respuesta y el
formato del JWT son idénticos a los de la versión anterior. El nombre de usuario no es una
credencial y no puede utilizarse en lugar del correo electrónico.

## Identidad actual

### GET /api/auth/me

Devuelve la identidad persistida del usuario asociado al JWT de la solicitud.

**Request**: sin body. Requiere `Authorization: Bearer <token>` válido.

**Response `200 OK`**:

```json
{
  "id": 123,
  "username": "juan.perez",
  "email": "user@example.com",
  "role": "USER"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Integer | Identificador del usuario persistido. Sin cambios respecto de la versión anterior. |
| `username` | String | Nombre de usuario normalizado en minúsculas. Campo nuevo. || `email` | String | Correo electrónico del usuario. Sin cambios. |
| `role` | String | `USER` o `SUPERUSER`. Campo nuevo e informativo para el cliente. |

**Responses**:

- `200 OK`: Identidad actual del usuario.
- `401 Unauthorized`: La sesión no está autenticada o el token no es válido.

**Notas de compatibilidad**: el cambio es aditivo; `id` y `email` conservan su nombre, tipo y
significado. El campo `role` es informativo: toda autorización se evalúa en el backend a partir
del rol persistido, nunca a partir de lo que declara el cliente.

## Errores de aplicación

Las respuestas de error gestionadas por la aplicación utilizan `ErrorResponseDTO`:

```json
{
  "timestamp": "2026-09-18T15:00:00",
  "status": 409,
  "error": "Conflict",
  "code": "EMAIL_ALREADY_REGISTERED",
  "message": "El email ya esta registrado.",
  "path": "/api/auth/register"
}
```

`code` identifica el error de forma estable (`INVALID_CREDENTIALS` en login y `INVALID_REQUEST` en validación); `message` es legible. En errores de Jakarta Validation, `message` contiene el primer mensaje disponible. Los `401`
generados por la capa de seguridad al validar JWT pueden tener un body diferente, ya que no
pasan por el manejador de errores de aplicación.

### Códigos de error de la ampliación

| Código | HTTP | Origen | Significado |
|--------|------|--------|-------------|
| `USERNAME_ALREADY_TAKEN` | `409 Conflict` | Aplicación | El nombre de usuario, ya normalizado a minúsculas, ya está en uso. |
| `INVALID_REQUEST` | `400 Bad Request` | Validación | Nombre de usuario vacío, fuera del rango de longitud o con caracteres no admitidos. |

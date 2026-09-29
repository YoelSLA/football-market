# API Contracts: User Registration and Authentication

## Registro

### POST /api/auth/register

Registra un nuevo usuario.

**Request Body** (JSON):

```json
{
  "email": "user@example.com",
  "password": "strongPassword123"
}
```

**Responses**:

- `201 Created`: Usuario registrado exitosamente, sin body.
- `400 Bad Request`: Datos inválidos, como email no válido o campos vacíos.
- `409 Conflict`: El email ya está registrado.

La contraseña de registro debe tener al menos 8 caracteres. Los errores gestionados por la
aplicación devuelven `timestamp`, `status`, `error`, `message` y `path`; en errores de validación,
`message` contiene el primer mensaje disponible.

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

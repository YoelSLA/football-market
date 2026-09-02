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
- `201 Created`: Usuario registrado exitosamente.
- `400 Bad Request`: Datos inválidos (email no válido, campos vacíos).
- `409 Conflict`: El email ya está registrado.

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
  - Body: `{"token": "JWT_TOKEN_HERE"}`
- `401 Unauthorized`: Credenciales inválidas.

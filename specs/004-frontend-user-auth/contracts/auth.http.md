# Contrato HTTP de autenticación

> **Actualizado 2026-10-14** conforme a la ampliación de SPEC-001. Ver
> [spec.md](../spec.md) §Amendments. Los cuerpos reproducen el contrato del backend descrito en
> [contracts/api.md de SPEC-001](../../001-user-auth-registration/contracts/api.md).

## POST /api/auth/login

Request:
```json
{"email":"user@example.com","password":"password"}
```

Responses:
- `200`: `{"token":"<jwt>"}`.
- `400`: error de validación; mostrar feedback y conservar el formulario.
- `401`: credenciales incorrectas; mostrar error sin invalidar una eventual sesión.
- Otros errores: mostrar un mensaje y permitir reintento manual sin alterar los datos escritos.

No requiere `username`: el nombre de usuario no es una credencial.

## POST /api/auth/register

Request:
```json
{"username":"juan.perez","email":"user@example.com","password":"password123"}
```

El `username` es obligatorio, admite de 3 a 30 caracteres formados por letras, números, punto, guion
y guion bajo, y se envía con la capitalización que escribió el visitante. El backend lo normaliza a
minúsculas y resuelve los duplicados sobre esa forma normalizada.

Responses:
- `201`: sin contenido; navegar a `/login` y mostrar confirmación.
- `400`: feedback de validación sin alterar los datos escritos.
- `409`: mostrar el mensaje del servicio bajo el campo correspondiente, según el `code` de la respuesta: `EMAIL_ALREADY_REGISTERED` bajo el email y `USERNAME_ALREADY_TAKEN` bajo el nombre de usuario.
- Otros errores: mostrar un mensaje y permitir reintento manual sin alterar los datos escritos.

## GET /api/auth/me

Headers:
```text
Authorization: Bearer <token>
```

Responses:
- `200`: `{"id":123,"username":"juan.perez","email":"user@example.com","role":"USER"}`; confirma la identidad actual.
- `401`: invalidar la sesión local y navegar a `/login`.
- Fallo de comunicación: suspender acceso privado, informar y permitir reintento manual.

El `id` es numérico. El `username` llega normalizado a minúsculas. El `role` es informativo: el
frontend no deriva de él ninguna capacidad ni decisión de autorización.

## Reglas transversales

Las URLs permanecen en el Service de autenticación. Los DTO se convierten a modelos dentro del Service mediante Mapper. Ningún componente o página realiza llamadas HTTP ni accede directamente a `localStorage`.

Registro y login usan DTOs distintos: `RegisterRequestDTO` con `username`, `email` y `password`, y
`CredentialsDTO` con `email` y `password`. El login nunca envía `username`.

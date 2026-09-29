# Contrato HTTP de autenticación

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

## POST /api/auth/register

Request:
```json
{"email":"user@example.com","password":"password123"}
```

Responses:
- `201`: sin contenido; navegar a `/login` y mostrar confirmación.
- `400`: feedback de validación sin alterar los datos escritos.
- `409`: mostrar el mensaje de email ya registrado.
- Otros errores: mostrar un mensaje y permitir reintento manual sin alterar los datos escritos.

## GET /api/auth/me

Headers:
```text
Authorization: Bearer <token>
```

Responses:
- `200`: `{"id":"<id>","email":"user@example.com"}`; confirma la identidad actual.
- `401`: invalidar la sesión local y navegar a `/login`.
- Fallo de comunicación: suspender acceso privado, informar y permitir reintento manual.

## Reglas transversales

Las URLs permanecen en el Service de autenticación. Los DTO se convierten a modelos dentro del Service mediante Mapper. Ningún componente o página realiza llamadas HTTP ni accede directamente a `localStorage`.

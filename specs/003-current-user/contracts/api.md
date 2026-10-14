# API Contract: Current Authenticated User

> **Actualizado 2026-10-14** conforme a la ampliación de SPEC-001. Ver
> [spec.md](../spec.md) §Amendments.

## GET `/api/auth/me`

Devuelve la identidad actual del usuario asociado a la sesión JWT.

### Seguridad

- Requiere `Authorization: Bearer <token>`.
- No requiere roles ni scopes adicionales.
- El token debe tener firma válida, no estar expirado y contener un `sub` utilizable como email.
- El usuario identificado debe continuar existiendo en persistencia.

### Request

- Body: ninguno.
- Query parameters: ninguno.
- Path parameters: ninguno.

### `200 OK`

Content-Type: `application/json`

```json
{
  "id": 123,
  "username": "juan.perez",
  "email": "usuario@email.com",
  "role": "USER"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `id` | integer (`int64`) | Sí | Identificador del usuario persistido actual. |
| `username` | string | Sí | Nombre de usuario actual, normalizado a minúsculas. |
| `email` | string (`email`) | Sí | Email actual del usuario persistido identificado por el `sub`. |
| `role` | string (enumeración) | Sí | Rol del usuario: `USER` o `SUPERUSER`. Informativo para el cliente. |

La respuesta contiene exactamente esos cuatro campos. No incluye password, token ni datos provenientes de claims como sustituto de la consulta persistida.

`role` describe la capacidad del usuario en el dominio y se expone únicamente como información: el cliente no lo utiliza para autorizar, y toda autorización se evalúa en el backend.

### `401 Unauthorized`

Body: vacío.

Se devuelve cuando ocurre cualquiera de estos casos:

- falta el encabezado Bearer o no contiene un token utilizable;
- el JWT está malformado, tiene firma inválida o está expirado;
- el JWT no contiene un `sub` utilizable;
- el `sub` no corresponde a un usuario persistido actual.

No se devuelve ErrorResponseDTO ni información que permita distinguir el motivo del rechazo.

### Respuestas no aplicables

- `404 Not Found`: no se usa para una identidad de token sin usuario; ese caso es `401`.
- `403 Forbidden`: no hay reglas de rol o permiso en esta feature. La exposición del `role` no introduce autorización por rol.

## Compatibilidad

- `POST /api/auth/register` conserva su contrato actual.
- `POST /api/auth/login` conserva su contrato actual.
- El formato y duración del JWT no cambian.
- El campo `role` es aditivo e informativo. Los clientes que ignoren los dos campos nuevos siguen interpretando correctamente `id` y `email`.

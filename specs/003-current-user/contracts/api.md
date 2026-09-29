# API Contract: Current Authenticated User

## GET `/api/auth/me`

Devuelve la identidad básica actual del usuario asociado a la sesión JWT.

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
  "email": "usuario@email.com"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `id` | integer (`int64`) | Sí | Identificador del usuario persistido actual. |
| `email` | string (`email`) | Sí | Email actual del usuario persistido identificado por el `sub`. |

La respuesta contiene exactamente esos dos campos. No incluye password, token ni datos provenientes de claims como sustituto de la consulta persistida.

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
- `403 Forbidden`: no hay reglas adicionales de rol o permiso en esta feature.

## Compatibilidad

- `POST /api/auth/register` conserva su contrato actual.
- `POST /api/auth/login` conserva su contrato actual.
- El formato y duración del JWT no cambian.

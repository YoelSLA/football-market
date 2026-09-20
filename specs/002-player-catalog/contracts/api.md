# Contratos de API: Catálogo de jugadores

## `GET /api/players`

Consulta una página de jugadores activos del catálogo local.

### Parámetros de consulta

| Parámetro | Tipo | Predeterminado | Restricción |
|---|---|---:|---|
| `page` | entero | `0` | Debe ser mayor o igual a `0`. |
| `size` | entero | `20` | Debe estar entre `1` y `100`, inclusive. |

### Respuesta `200 OK`

```json
{
  "content": [
    {
      "id": 7821,
      "name": "Joel Robles",
      "team": "Real Betis Balompié",
      "league": "Primera Division",
      "position": "Goalkeeper"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Un catálogo vacío devuelve `content: []` y sus metadatos de paginación.

### Respuesta `400 Bad Request`

Se devuelve si `page < 0`, `size < 1` o `size > 100`. La respuesta gestionada por la aplicación usa `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`. El mensaje de validación es seguro y está en español; `status` coincide con el código HTTP y `path` identifica la ruta solicitada.

### Respuesta `401 Unauthorized`

Se devuelve cuando la solicitud no presenta un JWT válido, según la autenticación vigente del proyecto.

## `POST /api/players/sync`

Inicia una sincronización manual del catálogo con Football-Data.org para Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`), sin exigir un orden. No acepta body.

### Respuesta `200 OK`

```json
{
  "obtained": 500,
  "created": 10,
  "updated": 475,
  "markedInactive": 3,
  "discardedInvalid": 2
}
```

Los contadores describen la ejecución completada; no se incluyen credenciales ni datos internos del proveedor.

### Respuesta `502 Bad Gateway`

Se devuelve si no puede completarse la lectura de Football-Data.org. La respuesta gestionada por la aplicación usa `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`. El mensaje es seguro y está en español; `status` coincide con el código HTTP y `path` identifica la ruta solicitada. La ejecución no modifica el catálogo local.

### Respuesta `401 Unauthorized`

Se devuelve cuando la solicitud no presenta un JWT válido. La sincronización no se inicia.

## Seguridad

Ambos endpoints requieren un JWT válido conforme a la configuración vigente de Spring Security. Cualquier usuario autenticado puede invocar `POST /api/players/sync` sin un rol o permiso adicional. Esta feature no define roles, permisos ni mecanismos de autenticación nuevos.

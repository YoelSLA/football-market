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
      "id": 417,
      "name": "Joel Robles",
      "team": "Real Betis Balompié",
      "league": "Primera Division",
      "position": "Goalkeeper",
      "dateOfBirth": "1990-06-20",
      "nationality": "Spain",
      "imageUrl": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Un catálogo vacío devuelve `content: []` y sus metadatos de paginación.

Cada jugador contiene exactamente `id`, `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality` e `imageUrl`.

| Campo | Tipo | Obligatoriedad | Notas |
|---|---|---|---|
| `id` | entero | Obligatorio | Identificador interno de FootballMarket. Nunca es el identificador de Football-Data.org. |
| `name` | texto | Obligatorio | Nombre del jugador. |
| `team` | texto | Obligatorio | Equipo según la última sincronización. |
| `league` | texto | Obligatorio | Liga según la última sincronización. |
| `position` | texto | Obligatorio | Posición del jugador. |
| `dateOfBirth` | fecha `YYYY-MM-DD` o `null` | Opcional | Fecha de nacimiento informada por Football-Data.org. |
| `nationality` | texto o `null` | Opcional | Nacionalidad informada por Football-Data.org. |
| `imageUrl` | texto o `null` | Opcional | URL de la imagen del jugador. Esta feature no la resuelve. |

`dateOfBirth`, `nationality` e `imageUrl` se devuelven como `null` cuando el dato no está disponible; su ausencia no invalida la respuesta ni reduce el conjunto de jugadores devueltos. La respuesta no expone el estado activo ni las referencias externas del jugador, incluido el proveedor y su identificador externo.

### Respuesta `400 Bad Request`

Se devuelve si `page < 0`, `size < 1` o `size > 100`. La respuesta gestionada por la aplicación usa `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`. `code` es `INVALID_PLAYER_PAGE` o `INVALID_PARAMETER_TYPE` según el error. El mensaje de validación es seguro y está en español; `status` coincide con el código HTTP y `path` identifica la ruta solicitada.

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

- `obtained`: integrantes de planteles leídos antes de validar y consolidar.
- `created`: jugadores nuevos, cada uno creado junto con su referencia externa `FOOTBALL_DATA`.
- `updated`: jugadores existentes resueltos por su referencia `FOOTBALL_DATA` y actualizados, incluidas las reactivaciones.
- `markedInactive`: cambios efectivos de activo a inactivo tras una sincronización completa.
- `discardedInvalid`: registros descartados por datos obligatorios ausentes o por conflicto de identidad con un jugador existente.

### Respuesta `502 Bad Gateway`

Se devuelve si no puede completarse la lectura de Football-Data.org, o si la aplicación local de la foto falla por un error técnico. La respuesta gestionada por la aplicación usa `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`. `code` es `FOOTBALL_DATA_UNAVAILABLE`. El mensaje es seguro y está en español; `status` coincide con el código HTTP y `path` identifica la ruta solicitada. La ejecución no modifica el catálogo local.

Esta feature no consulta TheSportsDB ni ningún proveedor distinto de Football-Data.org.

### Respuesta `401 Unauthorized`

Se devuelve cuando la solicitud no presenta un JWT válido. La sincronización no se inicia.

## Seguridad

Ambos endpoints requieren un JWT válido conforme a la configuración vigente de Spring Security. Cualquier usuario autenticado puede invocar `POST /api/players/sync` sin un rol o permiso adicional. Esta feature no define roles, permisos ni mecanismos de autenticación nuevos.

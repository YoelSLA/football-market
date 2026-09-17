# API Contracts: Catálogo de jugadores

## `GET /players`

Consulta una página de jugadores activos del catálogo local.

### Query Parameters

| Parámetro | Tipo | Predeterminado | Restricción |
|---|---|---:|---|
| `page` | entero | `0` | Debe ser mayor o igual a `0`. |
| `size` | entero | `20` | Debe estar entre `1` y `100`, inclusive. |

### `200 OK` Response

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

### `400 Bad Request` Response

Se devuelve si `page < 0`, `size < 1` o `size > 100`. El body contiene un mensaje seguro de validación en español.

## `POST /players/sync`

Inicia una sincronización manual del catálogo con Football-Data.org. No acepta body.

### `200 OK` Response

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

### `502 Bad Gateway` Response

Se devuelve si no puede completarse la lectura de Football-Data.org. El body contiene un mensaje seguro en español; la ejecución no modifica el catálogo local.

## Security

Los endpoints conservan la configuración de Spring Security existente. Esta feature no define roles, permisos ni mecanismos de autenticación nuevos.

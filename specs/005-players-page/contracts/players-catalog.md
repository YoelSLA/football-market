# Contrato consumido: catálogo de jugadores

Este documento describe el contrato HTTP existente que consumirá la página. No propone cambios de backend.

## Consultar jugadores activos

```http
GET /api/players?page=0&size=12
Authorization: Bearer <JWT>
```

### Parámetros

| Nombre | Ubicación | Tipo | Reglas de la feature |
| --- | --- | --- | --- |
| `page` | query | integer | Requerido por el cliente, desde cero y no negativo. |
| `size` | query | integer | Fijado por el cliente a `12`; backend admite de 1 a 100. |

No se envían parámetros de búsqueda, liga, posición u orden.

### Respuesta 200

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
  "size": 12,
  "totalElements": 1,
  "totalPages": 1
}
```

Todos los campos son requeridos. `content` contiene exclusivamente jugadores activos y está ordenado por identificador ascendente. El estado activo y las estadísticas de muestra no son campos HTTP.

Una página no negativa que exceda el total es una solicitud válida y puede responder `200` con `content` vacío. El cliente corrige esa selección a la última página disponible cuando `totalPages > 0`; `totalPages = 0` representa un catálogo realmente vacío.

### Respuesta 400

Se produce para `page < 0`, `size < 1`, `size > 100` o tipos de parámetros inválidos. Cuando el backend devuelve cuerpo, sigue el contrato global:

```json
{
  "timestamp": "2026-09-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_PLAYER_PAGE",
  "message": "La paginación solicitada no es válida",
  "path": "/api/players"
}
```

La UI puede mostrar el `message` válido mediante el helper HTTP existente y debe disponer de un mensaje fallback.

### Respuesta 401

JWT ausente, inválido o expirado. La respuesta no contiene cuerpo contractual. En una solicitud marcada `authenticated: true`, la infraestructura finaliza la sesión si la credencial usada sigue siendo la actual; el guard privado retira el catálogo y dirige a `/login`.

## Configuración de URL

El Service usa la ruta relativa `/players`, siguiendo el patrón de la aplicación. Por ello `VITE_API_URL` debe apuntar a la base que ya incluya `/api`, por ejemplo `http://localhost:8080/api`. No existe proxy Vite que añada ese prefijo automáticamente.

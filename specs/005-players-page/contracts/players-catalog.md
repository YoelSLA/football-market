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

Todos los campos son requeridos. `content` contiene exclusivamente jugadores activos y está ordenado por identificador ascendente. `team` alimenta el bloque central de la tarjeta; el estado activo y las estadísticas de demostración no son campos HTTP.

**Revisión 2026-10-13**: las estadísticas de la tarjeta dejaron de ser valores fijos comunes (`24` partidos, `8` goles y `5` asistencias, redacción original de 2026-09-28, **derogada**) y pasaron a derivarse de forma determinista del identificador del jugador mediante el generador compartido con el detalle (SPEC-005 FR-005; SPEC-009 FR-011). El contrato HTTP del catálogo no cambia: las estadísticas siguen sin ser campos de la respuesta.

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

## Forma real del elemento `content` (sincronizado 2026-10-13)

El contrato vigente devuelve más campos de los que recogía la redacción original de este documento.
El mapper del frontend valida los siguientes:

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `number` | Identificador interno. |
| `name` | `string` | Nombre. |
| `teamId` / `teamName` | `number` / `string` | Equipo actual; ambos presentes. |
| `leagueId` / `leagueName` | `number` / `string` | Liga actual; ambos presentes. |
| `position` | `string` | Posición persistida, sin catálogo cerrado. |
| `dateOfBirth` | `string \| null` | `yyyy-MM-dd` o `null`. |
| `nationality` | `string \| null` | Nacionalidad o `null`. |
| `imageUrl` | `string \| null` | Retrato principal o `null`. |
| `fallbackImageUrl` | `string \| null` | Retrato alternativo o `null`. |

La ampliación es compatible: los campos opcionales aceptan `null` y el mapper conserva la identidad
recibida. Esta sincronización documenta una realidad preexistente del backend y no introduce
ningún cambio de contrato.

**Recursos no contractuales**: el escudo del equipo y la bandera de nacionalidad que puede mostrar la
tarjeta se resuelven fuera de la API, con registro local y servicio externo respectivamente. No son
campos del contrato y su ausencia nunca invalida la respuesta.

## Configuración de URL

El Service usa la ruta relativa `/players`, siguiendo el patrón de la aplicación. Por ello `VITE_API_URL` debe apuntar a la base que ya incluya `/api`, por ejemplo `http://localhost:8080/api`. No existe proxy Vite que añada ese prefijo automáticamente.

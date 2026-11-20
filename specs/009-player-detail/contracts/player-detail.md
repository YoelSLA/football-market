# Contrato HTTP: Detalle individual de jugador

**Endpoint**: `GET /api/players/{id}`
**Estado**: Contrato vigente del sistema.
**Referencias normativas**: [spec.md](../spec.md), [plan.md](../plan.md) y [data-model.md](../data-model.md).

---

## 1. Solicitud

```http
GET /api/players/7821
Authorization: Bearer <JWT>
```

### Parámetros de ruta

| Parámetro | Ubicación | Tipo | Reglas |
| --- | --- | --- | --- |
| `id` | path | integer (`Long`) | Identificador interno del jugador; requerido, número entero estrictamente positivo (`id > 0`). |

No se admiten parámetros de consulta (query params), filtros ni ordenamiento en esta ruta.

---

## 2. Respuestas

### Respuesta 200 — Éxito

Devuelve la información de identidad del jugador visible.

```json
{
  "id": 7821,
  "name": "Joel Robles",
  "teamId": 41,
  "teamName": "Real Betis Balompié",
  "leagueId": 12,
  "leagueName": "Primera Division",
  "position": "Goalkeeper",
  "dateOfBirth": "1990-06-20",
  "nationality": "Spain",
  "imageUrl": "https://example.test/cutout.png",
  "fallbackImageUrl": "https://example.test/thumb.png"
}
```

**Reglas de campos**:
- Todos los campos están presentes en el JSON.
- `dateOfBirth`, `nationality`, `imageUrl` y `fallbackImageUrl` pueden ser `null`. En la interfaz se presentan como «No disponible» o con icono genérico de respaldo, sin inventar valores.
- Solo responde `200` para jugadores que satisfacen la política de visibilidad del catálogo (`active = true`, equipo vigente y liga válida).

### Respuesta 400 — Petición inválida

Se produce cuando `id` no es un número entero válido o es menor o igual a cero (`id <= 0`).

```json
{
  "timestamp": "2026-11-12T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_PARAMETER_TYPE",
  "message": "El parámetro de consulta no tiene un formato válido",
  "path": "/api/players/abc"
}
```

### Respuesta 401 — No autorizado

Credencial JWT ausente, inválida o expirada. La infraestructura de seguridad intercepta la petición y redirige al flujo de autenticación (`/login`).

### Respuesta 404 — No encontrado

Se produce cuando el jugador no existe o **cuando existe pero no cumple los criterios de visibilidad** (`active = false`, equipo inactivo o liga no válida). Ambas situaciones devuelven idéntica respuesta para evitar revelar la existencia de jugadores inactivos.

```json
{
  "timestamp": "2026-11-12T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "PLAYER_NOT_FOUND",
  "message": "No encontramos el jugador solicitado",
  "path": "/api/players/999999"
}
```

### Respuesta 500 — Error interno del servidor

Fallo interno no controlado al consultar los datos. El frontend maneja este estado mostrando un mensaje de error y un botón de reintento.

---

## 3. Seguridad y contexto de uso

- La ruta requiere autenticación mediante token Bearer JWT válido.
- La consulta de identidad es una operación estrictamente de lectura sin efectos colaterales de escritura.
- Las estadísticas deportivas y las cotizaciones oficiales se consultan a través de sus endpoints dedicados (`/api/players/{id}/statistics` y `/api/players/{id}/quotes`).

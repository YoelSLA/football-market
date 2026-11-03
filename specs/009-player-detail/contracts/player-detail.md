# Contrato HTTP: detalle individual de jugador

> Vigencia del rediseño 2026-10-25: la identidad `GET /api/players/{id}` se conserva sin cambios. La consulta estadística real se diseña en [player-statistics.md](player-statistics.md). Las referencias siguientes a integración futura, ausencia de endpoint estadístico o datos de demostración son antecedentes históricos, no restricciones del dashboard vigente. No se reintroduce REST Docs como dependencia; OpenAPI y tests de Controller sostienen el contrato.

> Aclaración vigente FR-046: se conservan DTO, ruta y visibilidad de identidad; se requiere alinear en la implementación futura la validación de ID para responder 400 ante formato/rango inválido, cero o negativo, y 404 solo ante ID válido inexistente/no visible. La implementación actual carece de validación positiva explícita; este requisito no se presenta como ya implementado. Ausencia estadística de jugador visible corresponde a 200 NO_DATA en la consulta estadística, no en identidad.

Contrato nuevo que implementa `GET /api/players/{id}` y que consume la pantalla de detalle.
La respuesta individual reutiliza el contrato de identidad del catálogo; no introduce campos de
estadísticas ni de cotización.

## Consultar un jugador visible

```http
GET /api/players/7821
Authorization: Bearer <JWT>
```

### Parámetros

| Nombre | Ubicación | Tipo | Reglas |
| --- | --- | --- | --- |
| `id` | path | integer (Long) | Identificador interno del jugador; requerido. |

No se aceptan parámetros de consulta, filtros ni orden.

### Respuesta 200

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

Todos los campos están presentes. `dateOfBirth`, `nationality`, `imageUrl` y `fallbackImageUrl`
pueden ser `null`; la UI los presenta como «No disponible» o usa el icono genérico, sin
inventar valores ni consultar proveedores.

Solo responde `200` para jugadores que cumplen la visibilidad del catálogo: `active = true`,
equipo vigente y liga válida.

### Respuesta 400

`id` con formato inválido, conforme al contrato global existente:

```json
{
  "timestamp": "2026-10-12T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_PARAMETER_TYPE",
  "message": "El parámetro de consulta no tiene un formato válido",
  "path": "/api/players/abc"
}
```

### Respuesta 401

JWT ausente, inválido o expirado. La infraestructura existente finaliza la sesión si la
credencial usada sigue siendo la actual y el guard privado dirige a `/login`. No existe cuerpo
contractual nuevo.

### Respuesta 404

Jugador inexistente **o existente pero no visible** (inactivo, equipo no vigente, liga no
válida). Ambas situaciones producen la misma respuesta y no permiten distinguir la existencia
del jugador:

```json
{
  "timestamp": "2026-10-12T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "PLAYER_NOT_FOUND",
  "message": "No encontramos el jugador solicitado",
  "path": "/api/players/999999"
}
```

La UI muestra «Jugador no encontrado» y ofrece regreso al catálogo, sin identidad ni bloques de
demostración.

### Fallo técnico local de lectura

Este caso **no tiene código HTTP asignado en esta SPEC**.

Inconsistencia detectada durante la revisión documental y resuelta solo aquí: al contrastar el
contrato con el código existente, los `502` de `GlobalExceptionHandler` corresponden
exclusivamente a fallos de proveedores externos (`FOOTBALL_DATA_UNAVAILABLE`,
`PLAYER_IMAGE_SYNC_FAILED`) y no a fallos de acceso a datos locales. El proyecto no tiene hoy un
código asignado para un fallo técnico local de lectura, por lo que esta SPEC no reserva ni inventa
uno.

Comportamiento requerido de la UI: cualquier respuesta distinta de `200`, `400`, `401` y `404` se
trata como error de comunicación, con mensaje comprensible y acción de reintento, y nunca se
sustituye por identidad ni por datos de demostración. Si más adelante se requiere un código
propio para fallos técnicos locales, será una decisión posterior y explícita.

## Autenticación

La ruta requiere JWT válido, igual que el catálogo. Se documenta en OpenAPI junto con el resto
de respuestas aplicables y se cubre con tests de Controller y Spring REST Docs conforme a
`docs/backend/testing.md` §8.

## Configuración de URL en el frontend

El Service usa la ruta relativa `/players/{id}` porque `VITE_API_URL` ya incluye `/api`
(p. ej. `http://localhost:8080/api`). La URL HTTP efectiva es `GET /api/players/{id}`.

## Contratos de datos de demostración

Las estadísticas y cotizaciones de demostración **no tienen contrato HTTP**. Se generan en el
frontend a partir del `id` del jugador y de constantes de período. Esta decisión cumple FR-012 y
FR-021: ninguna respuesta simulada se presenta como confirmación del backend.

## Integraciones futuras (no implementadas)

El requerimiento REQ-S2-12 contempla historial real de cotizaciones y el enunciado §4 define
`GET /players/:id/quotes`. Esa integración **queda fuera de esta SPEC**. Cuando exista, el
contrato real se acordará con el backend y este documento se actualizará; los detalles
escritos aquí son intención conceptual y no constituyen una API disponible:

- Necesita identificar jugador, fechas, precios del token en créditos y procedencia.
- Reemplazará la fuente de demostración sin modificar la presentación funcional definida en
  `spec.md`.
- Mantendrá los estados de error y vacío definidos para la pantalla.

Hasta entonces, el cumplimiento de REQ-S2-12 es parcial.

## Dependencias de recursos no contractuales (2026-10-13)

Este contrato **no incluye** ningún recurso gráfico. El detalle puede presentar, como adorno, el
escudo del equipo resuelto desde un registro local y la bandera de nacionalidad resuelta desde un
servicio externo. Ninguno de los dos es parte de la respuesta ni altera su evaluación: son recursos
de presentación con marcador de posición accesible, y su falla no cambia el estado de la pantalla.
Las estadísticas y cotizaciones de demostración siguen sin contrato HTTP.

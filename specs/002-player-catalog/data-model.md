# Data Model: Catálogo de jugadores

## Entity `Player`

| Atributo | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `Long` | PK, obligatorio, no generado | ID estable entregado por Football-Data.org. |
| `name` | `String` | Obligatorio, no vacío | Nombre expuesto por el catálogo. |
| `team` | `String` | Obligatorio, no vacío | Equipo actual según la sincronización. |
| `league` | `String` | Obligatorio, no vacío | Competición configurada que determina el catálogo. |
| `position` | `String` | Obligatorio, no vacío | Posición recibida del plantel. |
| `active` | `boolean` | Obligatorio | Determina si se muestra en `GET /players`. |

## Transition Rules

- Un candidato válido sin `id` local crea un `Player` activo.
- Un candidato válido con `id` local actualiza sus datos y lo activa.
- Tras aplicar una foto externa completa, un jugador activo ausente pasa a inactivo; no se elimina.
- Un jugador inactivo que reaparece se actualiza y vuelve a estar activo.
- Una foto externa fallida nunca llega a modificar esta entidad.

## Indexes

- La PK `id` garantiza la unicidad del identificador externo.
- El índice `active` optimiza la consulta paginada del catálogo visible.

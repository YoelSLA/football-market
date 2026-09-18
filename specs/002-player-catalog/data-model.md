# Modelo de datos: Catálogo de jugadores

## Entidad `Player`

| Atributo | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `Long` | PK, obligatorio, no generado | ID estable entregado por Football-Data.org. |
| `name` | `String` | Obligatorio, no vacío | Nombre expuesto por el catálogo. |
| `team` | `String` | Obligatorio, no vacío | Equipo actual según la sincronización. |
| `league` | `String` | Obligatorio, no vacío | Competición configurada que determina el catálogo. |
| `position` | `String` | Obligatorio, no vacío | Posición recibida del plantel. |
| `active` | `boolean` | Obligatorio | Determina si se muestra en `GET /players`. |

## Reglas de transición

- Un candidato válido sin `id` local crea un `Player` activo.
- Un candidato válido con `id` local actualiza sus datos y lo activa.
- Tras aplicar una foto externa completa, un jugador activo ausente pasa a inactivo; no se elimina.
- Un jugador inactivo que reaparece se actualiza y vuelve a estar activo.
- Una foto externa fallida nunca llega a modificar esta entidad.

## Índices

- La PK `id` garantiza la unicidad del identificador externo.
- No se define de entrada un índice sobre `active`: al ser booleano, su utilidad depende de la distribución de datos y del plan real de las consultas paginadas.

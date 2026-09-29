# Modelo de datos: Catálogo de jugadores

## Entidad `Player`

| Atributo | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `Long` | PK, obligatorio, no generado | ID estable entregado por Football-Data.org. |
| `name` | `String` | Obligatorio, no vacío | Nombre expuesto por el catálogo. |
| `team` | `String` | Obligatorio, no vacío | Equipo actual según la sincronización. |
| `league` | `String` | Obligatorio, no vacío | Nombre entregado por Football-Data.org para una de las ligas `PL`, `BL1`, `PD`, `SA` o `FL1`. |
| `position` | `String` | Obligatorio, no vacío | Posición recibida del plantel. |
| `active` | `boolean` | Obligatorio | Determina si se muestra en `GET /api/players`. |

## Reglas de transición

- Un candidato válido sin `id` local crea un `Player` activo.
- Un candidato válido con `id` local actualiza sus datos y lo activa.
- Tras aplicar una foto externa completa, un jugador activo ausente pasa a inactivo; no se elimina.
- Un jugador inactivo que reaparece se actualiza y vuelve a estar activo.
- Una foto externa fallida nunca llega a modificar esta entidad.

## Modelos de sincronización

- `PlayerSnapshot`: transporta la foto completa de candidatos válidos y los contadores
  `obtained` y `discardedInvalid` entre la obtención externa y la aplicación local.
  Conserva una lista de miembros inmutable y valida la coherencia de los contadores.
- `PlayerSynchronizationResult`: contiene los cinco contadores contractuales, no negativos.
  La suma de creados, actualizados y descartados no supera los registros obtenidos;
  las inactivaciones son independientes de esa cantidad.

## Índices de persistencia

- La PK `id` garantiza la unicidad del identificador externo.
- No se define de entrada un índice sobre `active`: al ser booleano, su utilidad depende de la distribución de datos y del plan real de las consultas paginadas.

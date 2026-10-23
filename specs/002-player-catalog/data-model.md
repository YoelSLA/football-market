# Modelo de datos: Catálogo de jugadores

## Entidad `Player`

| Atributo | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `Long` | PK, obligatorio, generado localmente | Identidad interna de FootballMarket. No proviene de ningún proveedor. |
| `name` | `String` | Obligatorio, no vacío | Nombre expuesto por el catálogo. |
| `team` | `String` | Obligatorio, no vacío | Equipo actual según la sincronización. |
| `league` | `String` | Obligatorio, no vacío | Nombre entregado por Football-Data.org para una de las ligas `PL`, `BL1`, `PD`, `SA` o `FL1`. |
| `position` | `String` | Obligatorio, no vacío | Posición recibida del plantel. |
| `active` | `boolean` | Obligatorio | Determina si se muestra en `GET /api/players`. |
| `dateOfBirth` | `LocalDate` | Opcional, nullable | Fecha de nacimiento informada por Football-Data.org. |
| `nationality` | `String` | Opcional, nullable | Nacionalidad informada por Football-Data.org. |
| `imageUrl` | `String` | Opcional, nullable | URL de la imagen. Esta feature no la escribe ni la resuelve. |

La ausencia de valor en los tres atributos opcionales no invalida el jugador. Un valor previo válido nunca se borra por una respuesta degradada del proveedor.

## Entidad `PlayerExternalReference`

| Atributo | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `Long` | PK, obligatorio, generado localmente | Identidad de la referencia. |
| `player` | `Player` | Obligatorio, FK no nula | Jugador al que pertenece la identidad externa. |
| `provider` | `PlayerProvider` | Obligatorio | Proveedor que asignó el identificador. Persistido como `VARCHAR`. |
| `externalId` | `String` | Obligatorio, no vacío | Identificador asignado por el proveedor. |

Relación inversa `Player.externalReferences` con cascada en persistencia y borrado. Un jugador puede referenciarse en más de un proveedor sin que su identidad interna cambie.

## Enumeración `PlayerProvider`

| Valor | Estado en esta feature |
|---|---|
| `FOOTBALL_DATA` | Participa de la sincronización. |
| `THE_SPORTS_DB` | Modelado como proveedor admitido. Sin consultas, matching, sincronización ni resolución de imágenes. |

## Invariantes

- `(provider, external_id)` es único. La combinación identifica de forma unívoca una referencia externa y no puede asociarse a más de un jugador.
- Toda referencia externa apunta a un jugador existente.
- La resolución de un jugador durante la sincronización es `(FOOTBALL_DATA, externalId)`.
- El alta de un jugador procedente de Football-Data.org y la de su referencia forman una única operación lógica.

## Reglas de transición

- Un candidato válido cuya referencia `(FOOTBALL_DATA, externalId)` no existe crea un `Player` activo y su referencia en el mismo alta.
- Un candidato válido cuya referencia existe actualiza los datos obligatorios, aplica los opcionales que la fuente informe con valor válido y activa al jugador asociado.
- Tras aplicar una foto externa completa, un jugador activo cuya referencia `FOOTBALL_DATA` no aparece en ella pasa a inactivo; no se elimina. Un jugador sin referencia `FOOTBALL_DATA` no se inactiva en esta feature.
- Un jugador inactivo que reaparece se actualiza y vuelve a estar activo.
- Un candidato con `externalId` ausente, vacío, o cuyos datos obligatorios faltan, o que presenta conflicto de identidad con un jugador existente, se descarta y no modifica ningún jugador ni referencia.
- Una foto externa fallida nunca llega a modificar estas entidades.

## Modelos de sincronización

- `PlayerCandidate`: record inmutable con `externalId`, `name`, `team`, `league`,
  `position`, `dateOfBirth` y `nationality`. No es entidad JPA, no contiene un ID
  interno ni imágenes, y permite reaplicar una foto sin reutilizar entidades de un
  intento revertido. La Integration transforma datos opcionales inválidos en `null`.

- `PlayerSnapshot`: transporta la foto completa de candidatos válidos y los contadores
  `obtained` y `discardedInvalid` entre la obtención externa y la aplicación local.
  Conserva una lista inmutable de `PlayerCandidate` y valida la coherencia de los contadores.
  Sus candidatos se consolidan por identificador externo de Football-Data.org.
- `PlayerSynchronizationResult`: contiene los cinco contadores contractuales, no negativos.
  La suma de creados, actualizados y descartados no supera los registros obtenidos;
  las inactivaciones son independientes de esa cantidad.

## Índices de persistencia

- La PK `players.id` garantiza la unicidad del identificador interno.
- La restricción única `(provider, external_id)` sobre `player_external_references` garantiza la identidad unívoca de la referencia externa.
- Un índice sobre `player_external_references.player_id` acompaña a la anterior para resolver la carga de referencias y la inactivación por ausencia de referencia en la foto.
- No se define de entrada un índice sobre `active`: al ser booleano, su utilidad depende de la distribución de datos y del plan real de las consultas paginadas.

## Migración de datos existentes

`V2__create_players_table.sql` creó `players` con `id` como clave primaria sin generación y con el identificador de Football-Data.org. `V3__decouple_player_external_identity.sql` realiza la transición:

1. Añade a `players` las columnas nullable `date_of_birth`, `nationality` e `image_url`.
2. Sustituye `players.id` por una columna `BIGINT` con generación local, conservando el valor anterior en la columna auxiliar `legacy_external_id`.
3. Crea `player_external_references` con su PK, `player_id` no nulo con FK a `players.id` y borrado en cascada, y la restricción única `(provider, external_id)`.
4. Crea el índice sobre `player_id`.
5. Inserta una fila por jugador existente con `provider = 'FOOTBALL_DATA'` y `external_id = legacy_external_id`.
6. Elimina `legacy_external_id`.
7. Ajusta la secuencia al máximo de `players.id`.

La migración no pierde ni duplica jugadores y preserva la correspondencia uno a uno entre cada jugador y su referencia `FOOTBALL_DATA`. El identificador interno se reasigna conforme a la spec; el valor numérico anterior solo se conserva como `externalId`.

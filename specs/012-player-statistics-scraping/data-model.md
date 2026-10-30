# Data Model: Scraping de estadísticas de rendimiento

> **Alineado con la implementación vigente (2026-10-18).** El modelo es aplanado: una fila por
> jugador, campaña y canal con las 60 métricas en columnas. Describe lo que existe en
> `V20261018191334__create_player_statistics.sql` más `V20261018191335__flatten_player_statistics.sql` y en las entidades
> `PlayerStatisticsExtractionRun`, `PlayerStatisticsRunItem`, `PlayerStatistics` y
> `ExtractionIncident`. No existen contextos, observaciones ni auditorías de contraste.

> **Diseño objetivo del 2026-10-27**: recorrido sin lotes y no-data reintentable; cambios pendientes de implementación. Las referencias a esquema existente se distinguen de los cambios objetivo descritos abajo.

## Principios

- El catálogo inmutable contiene exactamente 60 métricas y vive en código.
- Cada campaña se identifica por fecha UTC y canal (`SCRAPING` o `JSON`).
- JSON fija fecha al iniciar su POST sin entrada; scraping admite fecha opcional o resuelve UTC al inicio. Ambos conservan fecha hasta cerrar el run y otras peticiones solo intentan pendientes de esa campaña.
- Cada campaña y canal conserva exactamente una fila por jugador confirmado; una nueva llamada actualiza esa fila en lugar de acumular histórico por métrica.
- El contexto se limita a la liga local vigente del jugador; nunca se suman contextos de competiciones distintas.
- Una ausencia no borra el resto de la fila y ningún acceso externo mantiene una transacción abierta.
- Los IDs externos de competición y equipo son evidencia del proveedor; esta feature no crea identidades locales.

## MetricDefinition

Definición inmutable, no persistida como tabla.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `key` | String | Clave canónica única de `spec.md`; `snake_case`. |
| `unit` | MetricUnit | `SEASON_TOTAL`, `PER_MATCH`, `PERCENTAGE` o `AVERAGE_RATING`. |
| `sources` | Set<MetricSource> | Uno o ambos valores: `SCRAPING`, `JSON`. |
| `providerFields` | Map<MetricSource, Set<String>> | Columnas o campos externos admitidos por canal. |
| `optional` | boolean | Solo para métricas declaradas opcionales. |
| `derivation` | DerivationRule | `NONE` o `DIVIDE_BY_APPEARANCES`. |

`derivation` conserva la regla base para entradas acumuladas y consumidores previos.
La regla efectiva por canal se obtiene con `derivationFor(source)`: scraping de detalle
en modo `0` usa `NONE` (ya publicado por partido); las dos derivadas del resumen mantienen
`DIVIDE_BY_APPEARANCES`. JSON conserva su comportamiento previo en este alcance. No se
agrega campo persistido ni se cambia el catálogo o cobertura; no se recalculan datos.

`normalizeFor(source, published, apps)` aplica esa regla en Model: conserva el valor
normalizado y su precisión cuando es nativo, o usa el cociente decimal existente si
el origen es un acumulado. Service no implementa la aritmética por su cuenta.

La inicialización valida exactamente 60 claves, cobertura 45/57/42, las tres exclusivas de
scraping y ausencia de columnas sin definición. `foulsPerGame` y `foulCommitted` son aliases de
`fouls_committed_per_match`; no existe `MetricEquivalenceDefinition`.

`MetricUnit`, `DerivationRule` y `MetricAvailability` se usan en el proceso de ensamblado en
memoria, pero no se persisten por métrica: el modelo aplanado no tiene filas de observación. El
contrato del proveedor y la normalización se describen en `contracts/whoscored.md`.

## PlayerStatisticsExtractionRun

Tabla: `player_statistics_extraction_runs`.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia propia. |
| `extractionDate` | LocalDate | Fecha UTC no futura. |
| `source` | MetricSource | `SCRAPING` o `JSON`; obligatorio. |
| `startedAt` | Instant | Obligatorio. |
| `finishedAt` | Instant? | Obligatorio al finalizar. |
| `status` | ExtractionRunStatus | `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`, `INTERRUPTED`. |
| `eligiblePlayers` | long | Población con referencia WhoScored confirmada. |
| `excludedWithoutReference` | long | Catálogo sin referencia confirmada. |
| `selectedPlayers` | long | Elegidos para la ejecución. |
| `completedPlayers` | long | Items `COMPLETED`. |
| `noDataPlayers` | long | Items `NO_DATA`. |
| `failedPlayers` | long | Items fallidos. |
| `pendingPlayers` | long | Pendientes para fecha y canal. |
| `presentMetrics` | long | Métricas con valor en la fila confirmada. |
| `absentMetrics` | long | Métricas confirmadas sin valor. |
| `notDerivableMetrics` | long | Se mantiene la columna y el contador por compatibilidad de contrato, pero **siempre vale 0**: una derivada no derivable se persiste como columna nula y no se distingue de una ausencia. |
| `providerCalls` | long | Navegaciones o consultas externas. |
| `failureCode` | String? | Fallo global estable y saneado. |
| `maxPlayers` | Integer? (existente, a retirar) | No forma parte del modelo objetivo ni del request de ningún canal. Retirar atributo y columna de auditoría obsoletos junto con sus consumidores; no convertirlos en un límite oculto. |

### Invariantes

- Contadores no negativos y `selected = completed + noData + failed` al finalizar.
- `pending <= eligible`.
- Solo un run `RUNNING` por canal en la instancia (índice único parcial `uq_statistics_running_source`).
- Puede haber varios runs por fecha y canal; cada uno conserva su auditoría.
- En ambos canales el snapshot cubre todos los pendientes iniciales, sin límite total de tiempo/cantidad. `selectedPlayers` cuenta intentos efectivamente auditados; un snapshot interrumpido puede dejar IDs sin intentar. Con cero pendientes, run `COMPLETED`, cero seleccionados y llamadas, sin proveedor ni navegador.
- Restricción única adicional `(id, extraction_date, source)` para la referencia compuesta desde items.

## PlayerStatisticsRunItem

Tabla: `player_statistics_run_items`. Conserva la auditoría por intento aunque la fila de métricas
se actualice.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia propia. |
| `run` | PlayerStatisticsExtractionRun | Relación obligatoria. |
| `extractionDate` | LocalDate | Copia obligatoria de la fecha del run, garantizada mediante FK compuesta. |
| `source` | MetricSource | Copia obligatoria del canal del run, garantizada mediante FK compuesta. |
| `player` | Player | Identidad local inmutable. |
| `providerPlayerId` | String | Snapshot del ID WhoScored. |
| `result` | PlayerExtractionResult | `COMPLETED`, `NO_DATA`, `BLOCKED`, `NOT_FOUND`, `PROVIDER_ERROR`, `STRUCTURE_CHANGED`, `INCOMPLETE`, `INTERRUPTED`. |
| `startedAt` | Instant | Obligatorio. |
| `finishedAt` | Instant? | Obligatorio para resultado final. |
| `contextsCount` | int | Contextos considerados; 1 cuando hay contexto de liga local resuelto, 0 cuando no. |
| `presentMetrics` | int | Métricas con valor confirmadas. |
| `absentMetrics` | int | Ausencias confirmadas. |
| `incidentCode` | String? | Código principal saneado. |

El resultado se escribe una sola vez. Un item solo alcanza `COMPLETED` después de confirmar atómicamente
su fila de métricas.

Garantías de esquema:

- FK `(run_id, extraction_date, source)` hacia `player_statistics_extraction_runs(id, extraction_date, source)`, con las tres columnas no nulas. Fecha y canal se copian al construir el item desde el run, sin cambios independientes.
- Índice único parcial `uq_statistics_item_completed_campaign` sobre `(extraction_date, source, player_id) WHERE result = 'COMPLETED'`. Aplica entre runs distintos; no limita el número de intentos fallidos y permite confirmar otra fecha o canal.
- La confirmación del item y de su fila `player_statistics` ocurre en una sola transacción. Una colisión revierte esa confirmación completa, sin datos parciales, y se traduce de forma controlada en la frontera de persistencia del caso de uso.

Los timestamps sirven para auditoría, no para calcular capacidad ni cortar el recorrido. Scraping no reintenta ni vuelve a seleccionar IDs dentro del mismo POST; JSON conserva sus retries técnicos por consulta sin reselección de jugadores.

Pendientes = referencias `WHOSCORED` confirmadas sin item `COMPLETED` de igual fecha/canal, mediante exclusión por existencia. Fallidos, interrumpidos, no intentados y `NO_DATA` siguen pendientes para otro POST. En scraping sin valor numérico presente no se confirma `COMPLETED`, aun con contexto resuelto. Orden ascendente de ID local; sin IDs de entrada. La cobertura usa elegibles vigentes, no solo snapshot, y cada canal es independiente.

## PlayerStatistics

Tabla: `player_statistics`. Una fila por jugador, fecha de campaña y canal; las 60 métricas del
catálogo son columnas `NUMERIC` sin precisión ni escala fijas y nullable.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia `player_statistics_id_seq`. |
| `player` | Player | Identidad local, obligatoria e inmutable. |
| `extractionDate` | LocalDate | Fecha UTC de la campaña, inmutable. |
| `source` | MetricSource | Canal productor de la fila, inmutable. |
| `providerCompetitionId` | String? | ID externo de competición del contexto de liga local. |
| `providerTeamId` | String? | ID externo de equipo del contexto. |
| `seasonKey` | String? | ID técnico de temporada. |
| `seasonLabel` | String? | Texto publicado y recortado. |
| `contextStatus` | String | `RESOLVED` o `NO_LEAGUE_CONTEXT`; obligatorio. |
| `runItem` | PlayerStatisticsRunItem? | Item que confirmó por última vez la fila. |
| `observedAt` | Instant | Momento de la última confirmación. |
| `presentMetrics` | int | Métricas con valor en esta fila. |
| `absentMetrics` | int | Métricas sin valor en esta fila. |
| `<metric_key>` × 60 | BigDecimal? | Una columna por clave del catálogo, con el mismo nombre `snake_case` de la clave canónica. |

Restricciones:

- `uq_player_statistics UNIQUE (extraction_date, source, player_id)`: una fila por campaña, canal y jugador. El canal forma parte de la clave, de modo que JSON y scraping nunca se pisan.
- `ck_player_statistics_context`: `context_status IN ('RESOLVED','NO_LEAGUE_CONTEXT')`.
- `ck_player_statistics_counts`: contadores no negativos.
- Índices `ix_player_statistics_player (player_id, extraction_date DESC)` e `ix_player_statistics_campaign (extraction_date, source)`.

Semántica:

- **Presente** es un valor `NUMERIC` distinto de nulo. **Ausente** es una columna nula; la ausencia nunca se codifica como cero.
- Una clave del catálogo que el proveedor no publica, y una derivada no derivable por `apps` ausente o cero, se persisten ambas como columna nula. `NOT_DERIVABLE` no se persiste de forma distinguible; solo el contador agregado `absent_metrics` las cuenta como ausencia.
- La precisión observada no se persiste por métrica. El decimal publicado se conserva íntegro en la columna `NUMERIC` sin escala fija: `6.9033333333333333` se recupera exactamente. Solo las derivadas de FR-027 se calculan con seis decimales `HALF_UP` antes de persistirse.
- El mapping JPA declara el tipo `numeric` sin `precision` ni `scale` fijas, coherente con Flyway. La ida y vuelta exacta por PostgreSQL se verifica en los tests de persistencia.
- `source` de la columna es el canal real que produjo el valor, conservado en la propia fila por la unicidad.

### Selección del contexto de liga local

La fila no guarda todos los contextos publicados por el proveedor: guarda el de la liga local
vigente del jugador. La selección mapea la liga local al `tournamentId` de WhoScored con la
configuración `whoscored.competitions` (`1→2`, `2→3`, `3→4`, `4→5`, `5→22`) y, entre las filas que
coinciden, elige la de `seasonKey` numérico mayor. Sin coincidencia la fila se confirma como
`NO_LEAGUE_CONTEXT` con el contexto nulo y todas las métricas nulas.

El mismo mapeo queda congelado en el backfill de `V20261018191335__flatten_player_statistics.sql`; la
selección en código lee la configuración, no la migración.

### Reconfirmación

Una fila `NO_LEAGUE_CONTEXT` admite reconfirmación. Además, para scraping una fila con contexto `RESOLVED` pero sin ningún valor numérico presente y sin item `COMPLETED` de esa campaña admite reconfirmación: su intento anterior es `NO_DATA`. Contar valores no nulos, no entradas del mapa que pueden representar ausencias. Actualizar contexto, métricas y `runItem`/`observedAt` atómicamente. Una campaña con item `COMPLETED` nunca se sobrescribe ni degrada; conservar unicidad y rollback ante colisión real. JSON mantiene la regla anterior: solo fila `NO_LEAGUE_CONTEXT` reconfirmable.

## ExtractionIncident

Tabla: `player_statistics_incidents`.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia propia. |
| `runItem` | PlayerStatisticsRunItem | Relación obligatoria. |
| `section` | String? | Sección o combinación JSON. |
| `code` | String | Bloqueo, timeout, transporte, estructura, columna no catalogada, dato incoherente, no encontrado o interrupción. |
| `providerStatus` | Integer? | HTTP cuando exista, entre 100 y 599. |
| `attempt` | int | Scraping: 1, sin retries. JSON conserva los intentos permitidos de su contrato externo. |
| `occurredAt` | Instant | Obligatorio. |

No guarda HTML, JSON completo, cookies, credenciales ni tokens.

## Relaciones

```text
Player 1 ── N PlayerStatisticsRunItem N ── 1 PlayerStatisticsExtractionRun
Player 1 ── N PlayerStatistics (una por fecha y canal)
PlayerStatisticsRunItem 1 ── 0..1 PlayerStatistics (última confirmación)
PlayerStatisticsRunItem 1 ── N ExtractionIncident
```

## Transiciones

```text
RUNNING → COMPLETED | PARTIAL | FAILED | INTERRUPTED
```

No hay transición desde un run final. En scraping no se deduce orfandad por antigüedad: el POST no tiene deadline total. En la instancia única, la exclusión por canal permanece hasta terminar workers/limpieza; con una ejecución viva se rechaza otra entrada. Solo sin ejecución viva propia se recupera un run huérfano (incluido reinicio), cerrándolo `INTERRUPTED` antes de crear otro y preservando datos confirmados. El índice único de run activo sigue como respaldo. No se agrega soporte multiinstancia; JSON no cambia su recuperación.

## Impacto de esquema previsto

Retirar `max_players` del modelo y construcción de runs. La columna existente y sus tres checks asociados se retiran mediante DDL destructivo mínimo en una nueva migración timestamp dentro de `backend/src/main/resources/db/migration/`, generada según `docs/development/flyway-migrations.md`. No editar checksums de migraciones ya aplicadas ni agregar capas preservadoras de datos de desarrollo; el drop solo elimina configuración obsoleta, no migra valores a otro lugar. La preparación no genera ni ejecuta SQL. Si el usuario prefiere recrear una base de desarrollo, requiere autorización destructiva separada; no se presume aquí.

## Consultas derivadas

- Elegibles y pendientes: `player_statistics_run_items` por `(extraction_date, source, result)` con exclusión por item `COMPLETED` y orden por ID local de jugador.
- Cobertura por `extractionDate` y `source`: elegibles, completados, fallidos y pendientes.
- Métricas de una campaña: `player_statistics` por `(extraction_date, source)`.
- Ausencia sistémica: claves del catálogo del canal que no tienen valor en ninguna fila de esa campaña; se contrasta con las ausencias de jugadores concretos.

No se crea tabla de cobertura ni tabla de valores actuales: la fila de `player_statistics` es a la
vez el dato vigente de su campaña.

## Historical

El diseño inicial de esta spec era normalizado, con `player_performance_contexts`,
`player_performance_metric_observations` y `player_metric_comparisons`, valores append-only por
métrica y un contraste de auditoria entre canales. `V20261018191335__flatten_player_statistics.sql` creo la
tabla aplanada, migro el historico existente y elimino las tres tablas y sus secuencias
(`player_metric_comparisons_id_seq` queda huerfana). El contraste entre canales, la disponibilidad
`NOT_DERIVABLE` y la precision observada por metrica no forman parte del modelo vigente; la perdida
esta registrada en `docs/TECHNICAL_DEBT.md`.

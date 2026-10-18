# Data Model: Scraping de estadísticas de rendimiento

> **Alineado con la implementación vigente (2026-10-18).** El modelo es aplanado: una fila por
> jugador, campaña y canal con las 60 métricas en columnas. Describe lo que existe en
> `V10__create_player_statistics.sql` más `V11__flatten_player_statistics.sql` y en las entidades
> `PlayerStatisticsExtractionRun`, `PlayerStatisticsRunItem`, `PlayerStatistics` y
> `ExtractionIncident`. No existen contextos, observaciones ni auditorías de contraste.

## Principios

- El catálogo inmutable contiene exactamente 60 métricas y vive en código.
- Cada campaña se identifica por fecha UTC y canal (`SCRAPING` o `JSON`).
- JSON fija su fecha en el backend al iniciar el POST sin parámetros ni body; conserva esa fecha hasta cerrar el run. Otras llamadas del mismo día procesan solo pendientes; otro día es otra campaña. Scraping permite fijar fecha entre lotes.
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
| `maxPlayers` | Integer? | Límite positivo solicitado solo para scraping; null sin límite de cantidad. Para JSON siempre null, sin corte de lote temporal. |

### Invariantes

- Contadores no negativos y `selected = completed + noData + failed` al finalizar.
- `pending <= eligible`.
- Solo un run `RUNNING` por canal en la instancia (índice único parcial `uq_statistics_running_source`).
- Puede haber varios runs por fecha y canal; cada uno conserva su auditoría.
- `selectedPlayers <= maxPlayers` cuando el límite no es null, contando todos los workers.
- En JSON `maxPlayers` es null. La selección cubre todos los pendientes del snapshot inicial de elegibles, sin máximo de cantidad ni corte temporal de scraping. Con cero pendientes, run `COMPLETED` con cero seleccionados y cero llamadas externas, sin tocar al proveedor.
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

El ritmo para cortar lotes de scraping se reconstruye de `finishedAt - startedAt` de las últimas 20 confirmaciones `COMPLETED` de igual fecha y canal `SCRAPING`, ordenadas por `finishedAt` e `id` descendentes. Solo entran en la media las muestras con duración conocida; sin ninguna, 60 segundos por jugador. JSON conserva tiempos de auditoría pero no usa esta media para cortar su población.

Pendientes de scraping = referencias `WHOSCORED` confirmadas menos jugadores con item `COMPLETED` de igual fecha y `SCRAPING`, mediante exclusión por existencia, no mediante un cursor del cliente. Los fallidos, interrumpidos y no intentados siguen pendientes. La selección ordena por ID local ascendente y nunca utiliza nombres o IDs proporcionados por el desarrollador. La misma exclusión se aplica a JSON para su fecha UTC inicial y canal `JSON`; completar uno de los canales no completa el otro. La cobertura usa el catálogo elegible vigente, no solo la población seleccionada en el último run.

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

El mismo mapeo queda congelado en el backfill de `V11__flatten_player_statistics.sql`; la
selección en código lee la configuración, no la migración.

### Reconfirmación

Solo una fila `NO_LEAGUE_CONTEXT` admite reconfirmación: si el jugador vuelve a aparecer en la
misma campaña y ahora tiene contexto de liga local resuelto, la fila se actualiza con el contexto,
las métricas y el nuevo `runItem`/`observedAt`. Una fila `RESOLVED` se rechaza como campaña repetida
y sus métricas no se sobrescriben ni se degradan. La colisión de `uq_player_statistics` entre dos
ejecuciones concurrentes se conserva como detección de concurrencia real y revierte la confirmación
completa.

## ExtractionIncident

Tabla: `player_statistics_incidents`.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia propia. |
| `runItem` | PlayerStatisticsRunItem | Relación obligatoria. |
| `section` | String? | Sección o combinación JSON. |
| `code` | String | Bloqueo, timeout, transporte, estructura, columna no catalogada, dato incoherente, no encontrado o interrupción. |
| `providerStatus` | Integer? | HTTP cuando exista, entre 100 y 599. |
| `attempt` | int | 1 o 2. |
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

No hay transición desde un run final. La recuperación marca runs activos previos del mismo canal como `INTERRUPTED`, crea otro run y conserva todos los datos confirmados.

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
métrica y un contraste de auditoria entre canales. `V11__flatten_player_statistics.sql` creo la
tabla aplanada, migro el historico existente y elimino las tres tablas y sus secuencias
(`player_metric_comparisons_id_seq` queda huerfana). El contraste entre canales, la disponibilidad
`NOT_DERIVABLE` y la precision observada por metrica no forman parte del modelo vigente; la perdida
esta registrada en `docs/TECHNICAL_DEBT.md`.
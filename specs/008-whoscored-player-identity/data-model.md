# Modelo de datos — Identidad WhoScored

Diseño de Phase 1 de [SPEC-008](spec.md). Identificadores/campos propuestos en inglés; no constituye implementación ni script de migración.

## 1. Catálogo y referencias existentes

`Player → Team → League` mantiene su identidad y atributos. Seleccionar los jugadores persistidos de los cinco bindings internos; no agregar filtros por `Player.active` o `Team.current` no exigidos por la spec. IDs internos no son IDs WhoScored.

`PlayerExternalReference` conserva `id`, `player`, `provider`, `externalId`. Agregar proveedor `WHOSCORED`; IDs externos como cadenas decimales positivas canónicas, sin ceros iniciales. Se preservan referencias Football-Data/TheSportsDB.

Invariantes respaldadas por DB:

- `UNIQUE(provider, external_id)` existente: una identidad externa no pertenece a dos jugadores.
- Índice único parcial sobre `player_id` cuando `provider = 'WHOSCORED'`, llamado `uk_player_external_references_whoscored_player`: un jugador tiene una sola identidad WhoScored.
- Model rechaza una segunda referencia WhoScored distinta; la misma referencia es idempotente. No extender esta nueva cardinalidad a otros proveedores: SPEC-007 permite varias por proveedor en Player.
- Sin actualización ni borrado de referencias confirmadas dentro de este caso de uso. No guardar entidades detached del snapshot mediante merge.

## 2. Ejecución: PlayerIdentitySyncRun

Tabla prevista `player_identity_sync_runs`:

| Campo | Tipo / significado |
| --- | --- |
| `id` | Long, PK generada por secuencia. |
| `passType` | CURRENT o HISTORICAL. |
| `sourceRunId` | FK a run actual; obligatorio solo en HISTORICAL. |
| `startedAt`, `finishedAt` | Instant; fin nullable mientras RUNNING. |
| `status` | RUNNING, COMPLETED, PARTIAL, FAILED. |
| `failureReason` | Código sanitizado nullable, para interrupción/fallo global. |
| `selectedPlayers` | Cantidad de snapshots locales de este run. |
| `discoveredUnique` | IDs externos distintos procedentes exclusivamente de ligas completas. |
| `resolved`, `alreadyReferenced`, `conflicts`, `noMatch`, `ambiguous` | Resultados locales finales, una vez por jugador. |
| `discoveryErrors`, `persistenceErrors`, `interruptedPlayers` | Otros resultados locales, separados de pendientes genuinos. |
| `providerErrors` | Cantidad de ámbitos de liga FAILED; no cantidad de jugadores ni requests. |
| `localDataErrors` | Jugadores con resultado LOCAL_DATA_ERROR, no pendientes históricos. |
| `discardedRows` | Filas externas rechazadas, no jugadores locales ni ligas fallidas. |

Origen elegible: CURRENT en COMPLETED o PARTIAL y con `finishedAt`. Un origen inexistente es 404; HISTORICAL, RUNNING o FAILED no es origen elegible (409). Un actual parcial es válido, pero solo sus items NO_MATCH/AMBIGUOUS pueden originar el histórico.

El histórico selecciona esos IDs que existan y sigan sin referencia al inicio; no incluye conflictos, errores ni resueltos. No crea items para quienes quedaron excluidos ni borra el item de origen. Cero elegibles produce COMPLETED vacío sin red.

Relación 1:N con ámbitos, candidatos e items. Índices de historial `(started_at, id)` y `source_run_id`. No purga implícita ni tabla de estadística/cotización.

## 3. Ámbito descubierto: PlayerIdentityLeagueDiscovery

Tabla `player_identity_league_discoveries`; UNIQUE `(run_id, league_id)`:

- `id`, `runId`, `leagueId` interno, `leagueName` capturado y `tournamentId` externo (2/3/4/5/22).
- `status`: NOT_STARTED → DISCOVERING → COMPLETED o FAILED; SKIPPED si no hay jugadores elegibles en todo el run.
- `startedAt`, `finishedAt`, `pagesRead`, `lastPage`, `totalPages`, `totalResults`, `rowsRead`: metadatos observados, no estadísticas deportivas. Los totales son nullable si no hubo página válida.
- `errorCode`, `errorMessage`, `failedPage`, `httpStatus` nullable: error de integración sanitizado. Nunca body crudo, cookies o headers sensibles.
- `discardedRows`: cantidad de filas rechazadas por identidad inválida, independientemente del resultado del recorrido. Auditoría interna de cada descarte: run/ámbito, página, posición de fila y reasonCode sanitizado; sin cuerpo crudo ni identidad inventada. No es un candidato ni un item local. Las filas originales, incluidas descartadas, cuentan en rowsRead y validación de paging.

El binding liga/competición se valida antes de iniciar el run y se captura para auditoría. Una liga fallida conserva diagnóstico pero no publica candidatos parciales al conjunto de resolución. No fabricar Player para representar un error de liga.

## 4. Candidato deduplicado: WhoScoredIdentityCandidate

Tabla `whoscored_identity_candidates`; UNIQUE `(run_id, external_id)`:

- `id`, `runId`, `externalId`: identidad del candidato para esta ejecución.
- `contexts`: JSONB de registros de identidad observados: `name`, `teamId`, `teamName`, `tournamentId`, `tournamentName`, `leagueId`, `leagueName`; valores externos ausentes se conservan null, sin inventarlos. Opcional `seasonId`/`seasonName` para distinguir contextos históricos, no para seleccionar identidad.
- Conservar todas las variantes de nombre/equipo observadas para un mismo ID. Un contexto idéntico repetido no genera otra identidad ni aumenta `discoveredUnique`.
- Solo contextos de ámbitos COMPLETED; ninguna métrica deportiva (`rating`, goles, etc.) ni copia del JSON externo completo.

JSONB se mapea como estructura tipada mediante soporte JSON de Hibernate, no como String sin tipo JDBC JSON. Los contratos externos de Integration no se reutilizan como Model ni DTO.

Las entidades de las secciones 4 y 5 son de auditoría interna: se persisten para la trazabilidad, la reconciliación de contadores y la elegibilidad del pase histórico, pero esta feature no expone ninguna proyección de lectura por API. Su estructura no se simplifica por ello, porque el pase histórico y la idempotencia dependen de ellas.

## 5. Snapshot y decisión: PlayerIdentitySyncItem

Tabla `player_identity_sync_items`; UNIQUE `(run_id, player_id)`:

| Campo | Significado |
| --- | --- |
| `id`, `runId`, `playerId` | Identidad de item/run y ID local capturado. |
| `playerName`, `teamId`, `teamName`, `leagueId`, `leagueName` | Snapshot local usado para decidir; no se consulta el nombre actual al presentar auditoría. |
| `initialExternalId` | Referencia WhoScored al inicio, nullable. |
| `initialExternalIds` | Lista JSONB de IDs WhoScored iniciales para evidenciar una inconsistencia preexistente sin elegir uno; normalmente vacía o de un elemento. |
| `result` | PENDING durante ejecución; después uno de los resultados de la tabla siguiente. |
| `reasonCode`, `reasonMessage` | Motivo estable y explicación sanitizada en español. |
| `candidateExternalIds` | Lista JSONB de IDs candidatos observados por nombre, sin duplicados; pertenecen al mismo run y se resuelven a sus contextos persistidos. |
| `confirmedExternalId` | Referencia confirmada/conservada cuando corresponde, nullable. |
| `decidedAt` | Instant nullable hasta resultado final. |

`playerId` es un identificador histórico capturado: la auditoría no depende de joins al catálogo mutable ni de borrados en cascada del jugador. Constraints FK a run; las relaciones actuales de catálogo se verifican durante confirmación. Índices `(run_id, result, id)` y `(run_id, player_id)`. Invariantes de Model validan que cada candidato enlazado pertenezca al run.

### Resolución global

Aplicar exactamente FR-005: Unicode NFD, eliminar Mn/Mc/Me solo asociadas a base LATIN, conservar marcas no latinas, minúsculas con Locale.ROOT, sustituir Pd y White_Space por U+0020, colapsar secuencias y quitar extremos. No eliminar puntuación adicional, sufijos FC, abreviaturas ni transliterar. Correspondencias solo del [catálogo normativo fijo](spec.md#catálogo-normativo-de-alias-de-equipos), como pares normalizados. Validar antes del run que un nombre externo normalizado no apunte a distintos locales; error de configuración sin red. Sin versiones dinámicas ni snapshot de alias.

Buscar todos los candidatos por nombre normalizado y conservar su evidencia, incluidos descartados por equipo. Un ID cumple si existe al menos un contexto donde nombre y equipo coincidan simultáneamente con snapshot/alias. Comparar unión de las cinco ligas completas, sin restringir a la liga local. Más de un ID válido implica AMBIGUOUS. Si un único ID cumple para varios jugadores locales distintos, registrar CONFLICT para los involucrados no referenciados antes de escribir, sin elegir un propietario por orden.

| Resultado final | Condición / motivo principal |
| --- | --- |
| RESOLVED | Coincidencia única y referencia nueva confirmada atómicamente. `UNIQUE_NAME_AND_TEAM`. |
| ALREADY_REFERENCED | Referencia inicial existente, sin rematching (`REFERENCE_ALREADY_PRESENT`), o misma referencia confirmada por otra ejecución (`REFERENCE_CONFIRMED_CONCURRENTLY`). |
| NO_MATCH | Ningún ID cumple y liga local fue COMPLETED; `NO_NAME_MATCH` o `TEAM_NOT_MATCHED`. Pendiente genuino en el ámbito válido declarado. |
| AMBIGUOUS | Más de un ID distinto cumple; `MULTIPLE_MATCHING_IDENTITIES`. Pendiente genuino con evidencia. |
| CONFLICT | Propietario distinto (`EXTERNAL_ID_ALREADY_OWNED`), referencia diferente concurrente (`PLAYER_REFERENCE_CONFLICT`) o varios locales para un candidato (`MULTIPLE_LOCAL_OWNERS`). Sin sobrescritura. |
| LOCAL_DATA_ERROR | Nombre/equipo local nulo, vacío normalizado o sin letras/dígitos: `INVALID_LOCAL_PLAYER_NAME` o `INVALID_LOCAL_TEAM_NAME`. Sin referencia ni NO_MATCH; no elegible para histórico. |
| DISCOVERY_ERROR | Sin coincidencia válida y liga local FAILED; `LOCAL_LEAGUE_INCOMPLETE`. No afirmar ausencia de coincidencias ni elegibilidad histórica. |
| PERSISTENCE_ERROR | Error de confirmación distinto de conflicto; `REFERENCE_CONFIRMATION_FAILED` o `PLAYER_NO_LONGER_EXISTS`. Rollback y auditoría separados. |
| INTERRUPTED | Procesamiento no finalizado en interrupción controlada; `EXECUTION_INTERRUPTED`. |

Referencias iniciales se clasifican antes de matching. Ambigüedades o coincidencias obtenidas en ligas completas conservan su resultado aunque otras ligas fallen; la auditoría declara el alcance parcial. Si no hay candidato, `candidateExternalIds=[]`, sin campos externos inventados.

Varias referencias iniciales distintas generan CONFLICT (`MULTIPLE_INITIAL_REFERENCES`) antes de evaluar atributos/matching: conservar los IDs en `initialExternalIds` para auditoría, sin elegir uno para `initialExternalId` ni borrarlos. Una sola referencia produce ALREADY_REFERENCED sin rematching. Sin referencia y con atributos inválidos produce LOCAL_DATA_ERROR. Snapshots admiten valores ausentes para auditar el defecto. LOCAL_DATA_ERROR es una decisión final de validación, no un fallo técnico de ejecución; no degrada por sí sola COMPLETED. Corregir datos permite participar en un nuevo actual, no cambia elegibilidad/evidencia del origen anterior.

Variantes de nombre del mismo ID son contextos independientes; jamás combinar el nombre de uno con el equipo de otro. Un `teamId` ausente se conserva null y no impide un contexto con nombres válidos. Filas descartadas no se incorporan a candidatos ni al conteo de IDs descubiertos.

### Transacciones y carreras

Capturar snapshot completo coherente al iniciar cada pase con aislamiento REPEATABLE_READ en transacción breve; la red empieza después del commit. Confirmación REQUIRES_NEW por jugador: recargar Player administrado con lock breve, comprobar referencias actuales y propietario, insertar referencia y guardar resultado en el mismo commit. No revalidar atributos del catálogo ni mantener locks durante red/matching.

Después de constraint violation, leer referencias en nueva transacción y clasificar idempotencia/conflicto; no continuar una transacción rollback-only. Clasificar PostgreSQL por SQLSTATE 23505 y constraint estructurada. Un fallo ajeno a unicidad no se transforma en CONFLICT. Si la escritura de auditoría también falla, abortar run con error controlado; no prometer trazabilidad que no pudo guardarse.

## 6. Estados y reconciliación

RUNNING → COMPLETED: todos los ámbitos necesarios completos y todos los items finales sin error técnico/interrupción. NO_MATCH/AMBIGUOUS/CONFLICT/LOCAL_DATA_ERROR y descartes de filas auditados no equivalen por sí solos a fallo técnico ni a liga incompleta. Ningún umbral de cobertura determina el estado.

RUNNING → PARTIAL: hubo errores de proveedor/persistencia/interrupción, pero al menos un ámbito terminó correctamente o existen resultados locales finales no técnicos. RUNNING → FAILED: ningún ámbito útil terminó ni hubo resultados locales no técnicos, o fallo global impidió cerrar coherentemente. Un vacío sin trabajo es COMPLETED. Fin no anterior al inicio.

Al cierre:

```text
selectedPlayers = resolved + alreadyReferenced + conflicts + noMatch + ambiguous
                + localDataErrors + discoveryErrors + persistenceErrors + interruptedPlayers
discoveredUnique = cantidad de candidatos persistidos de ese run
providerErrors = cantidad de ámbitos FAILED
discardedRows = cantidad de descartes externos auditados de ese run
```

Mientras RUNNING, los contadores finales más `pendingPlayers` reconcilian con `selectedPlayers`; `pendingPlayers` se deriva y no es otro resultado final. Nunca sumar providerErrors al total de jugadores. Resúmenes usan agregados de estado durable, no incrementos por excepción y reconciliación doble.

Nuevos pases no modifican evidencia de runs anteriores. Interrupción abrupta puede dejar RUNNING con items PENDING: consultas declaran ejecución sin finalización registrada; no es un origen histórico elegible. No inferir abandono desde otra instancia ni asociar desde ligas incompletas.

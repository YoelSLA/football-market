# Contrato HTTP — Identidad WhoScored

Diseño nuevo de [SPEC-008](../spec.md); endpoints aún no implementados. Prefijo `/api/`, DTO records y Mapper estático según normas vigentes. Tipos/resultados/campos de identidad definidos en [modelo](../data-model.md).

## Seguridad y operaciones

Todas las rutas requieren Bearer JWT válido y usuario persistido, igual que sincronizaciones actuales. No existe un nuevo rol/admin-only ni cambio de CORS. JWT se utiliza solo en la API propia y no se envía al proveedor.

Base: `/api/players/identities/whoscored`.

| Método / ruta relativa | Entrada | Respuesta |
| --- | --- | --- |
| POST `/sync` | Sin body ni selección arbitraria de ligas. | 200, `PlayerIdentitySyncRunResponseDTO` CURRENT terminado. |
| POST `/sync-runs/{id}/historical` | ID del actual de origen; sin body. | 200, mismo DTO HISTORICAL con sourceRunId. |

Esta feature no expone endpoints de consulta de ejecuciones, jugadores ni evidencia. El resumen y los contadores de la ejecución que provoca la operación se obtienen en su propia respuesta 200; el detalle por jugador y los candidatos observados quedan en la auditoría persistida sin proyección de consulta. Editar, reasignar o aprobar asociaciones manualmente tampoco se expone.

POST síncrono: no 202 ni worker. 200 significa resultado registrado, no que todas las identidades se resolvieron: el DTO puede declarar COMPLETED, PARTIAL o FAILED. Histórico sobre actual parcial permite solo NO_MATCH/AMBIGUOUS que sigan sin referencia al inicio. Actual sin pendientes elegibles produce histórico COMPLETED vacío, sin red.

Como no existe consulta de historial, una ejecución interrumpida por desconexión o fallo de transporte se relanza de forma segura: la idempotencia de las referencias garantiza que repetir un pase no duplica ni altera asociaciones confirmadas (FR-014, FR-026). Cada POST registra una ejecución nueva con `id` propio; la seguridad del reintento no depende de reutilizar la anterior. El operador no necesita comprobar el resultado anterior antes de repetir.

Estado: COMPLETED significa que el trabajo previsto terminó, no que todos los jugadores fueran identificados; NO_MATCH y AMBIGUOUS son resultados finales válidos y no degradan el estado. Una liga con descubrimiento incompleto o un error de confirmación sí lo degradan a PARTIAL aunque todas las coincidencias alcanzables se hayan confirmado (FR-016, FR-021).

## Validación

IDs `Long` positivos; valores no numéricos o no positivos producen 400. No hay parámetros de paginación ni de filtro. Validación estructural en Controller/DTO; origen elegible en Service.

## DTO de resumen

`PlayerIdentitySyncRunResponseDTO`:

- `id: long`, `passType: CURRENT|HISTORICAL`, `sourceRunId: long|null`.
- `startedAt`, `finishedAt`: strings ISO 8601 de Instant con offset; ambos presentes en la respuesta de una operación de ejecución, que solo devuelve ejecuciones terminadas.
- `status: COMPLETED|PARTIAL|FAILED`, `failureReason: string|null`. `RUNNING` es un estado interno del modelo y no se serializa en estas respuestas (FR-021).
- `counters`: `selectedPlayers`, `discoveredUnique`, `resolved`, `alreadyReferenced`, `conflicts`, `noMatch`, `ambiguous`, `discoveryErrors`, `persistenceErrors`, `interruptedPlayers`, `pendingPlayers`, `providerErrors` (longs no negativos).
- `counters.localDataErrors`, `counters.discardedRows`: longs no negativos; respectivamente jugadores con datos locales inválidos y filas externas descartadas. No sumar descartes al total de jugadores ni a providerErrors. LOCAL_DATA_ERROR y descartes auditados no degradan por sí solos el estado de finalización.
- `leagues`: array de DTO de ámbito: `leagueId`, `leagueName`, `tournamentId`, `status`, `startedAt`, `finishedAt`, `pagesRead`, `lastPage`, `totalPages`, `totalResults`, `rowsRead`, `errorCode`, `errorMessage`, `failedPage`, `httpStatus`; nulabilidad conforme al modelo. `status` por ámbito toma los valores `NOT_STARTED`, `DISCOVERING`, `COMPLETED`, `FAILED` y `SKIPPED`; un ámbito `SKIPPED` corresponde a un pase sin jugadores elegibles (FR-012).
- Cada ámbito incluye también `discardedRows`; rowsRead cuenta filas originales. El detalle de descartes es auditoría interna, no una nueva consulta HTTP.

Unidades: discoveredUnique cuenta IDs externos de ámbitos completos; resultados locales cuentan jugadores finales exclusivos; providerErrors cuenta ligas fallidas. Totales del proveedor son filas, no identidades. `discoveryErrors`, `persistenceErrors` e `interruptedPlayers` cuentan items; `providerErrors` cuenta los ámbitos fallidos y no se suma a los anteriores (FR-019). `pendingPlayers` es derivado y solo reconcilia durante la ejecución: en la respuesta de una operación de ejecución, que solo devuelve ejecuciones terminadas, MUST valer 0. La reconciliación de cierre es la de FR-019 y `data-model.md` §6. Los contadores por resultado permiten al operador distinguir ausencia de coincidencia, ambigüedad, conflicto y error de proveedor sin repetir el descubrimiento (SC-007). El detalle de descartes es auditoría interna, no una nueva consulta HTTP.

El DTO no incluye body externo, métricas, cotizaciones ni credenciales.

## Errores

Todo error HTTP controlado de aplicación utiliza `ErrorResponseDTO`: `timestamp`, `status`, `error`, `code`, `message`, `path`; status coincide con HTTP, message en español y code estable. Timestamp conserva el tipo/formato del contrato vigente, no se modifica globalmente en esta feature.

| HTTP | code | Situación |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER_TYPE` | Valor de `{id}` no convertible a numérico. |
| 400 | `INVALID_PLAYER_IDENTITY_ID` | ID no positivo. |
| 404 | `PLAYER_IDENTITY_RUN_NOT_FOUND` | Run de origen inexistente. |
| 409 | `PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE` | Origen no es CURRENT terminado COMPLETED/PARTIAL. |
| 500 | `PLAYER_IDENTITY_SYNC_FAILED` | Fallo global que impide registrar resultado coherente, incluido error de auditoría. Resultados confirmados previos permanecen. |

JWT ausente/inválido: 401 por cadena de seguridad vigente; no asumir que el filtro devuelve ErrorResponseDTO. Documentar el comportamiento real de seguridad sin modificarlo por esta feature. Los errores de proveedor aislados aparecen como motivos en los ámbitos y no como errores HTTP de cada jugador.

Configuración inválida se rechaza al binding/startup; bindings internos inexistentes/contradictorios detectados antes de run producen error controlado `PLAYER_IDENTITY_SYNC_FAILED` sin solicitudes externas. Las excepciones propias usan jerarquía FootballMarketException (Application/Integration/Persistence/Domain según responsabilidad).

Colisiones de nombres normalizados en la tabla fija se rechazan antes del pase como error de configuración `PLAYER_IDENTITY_SYNC_FAILED`, sin red ni asociaciones. Cobertura informativa: `(resolved + alreadyReferenced) / selectedPlayers`; denominador cero es no aplicable, no fallo. No existe porcentaje mínimo para COMPLETED.

## Documentación y consumidores

OpenAPI en español: cada endpoint, params, campos, seguridad bearerAuth, respuestas 200 y todos los errores aplicables. Tests de Controller con REST Docs documentarán cada caso contractual después de assertions; fuentes AsciiDoc se actualizarán al implementar, no snippets generados en Plan. E2E usará Request/Response DTO productivos y proveedor controlado.

Consumidor previsto: operador autenticado mediante HTTP/Swagger; sin frontend adicional. Nuevas rutas y DTO no alteran listado/detalle del catálogo ni contratos Football-Data/TheSportsDB.

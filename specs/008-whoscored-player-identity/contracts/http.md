# Contrato HTTP — Identidad WhoScored

## Contrato vigente del cambio — 2026-11-01

Sustituye el contrato histórico conservado debajo. Fuente: [spec.md](../spec.md), FR-001/012/016/019–023/026. Implementado en Controller/DTO y cubierto por tests escritos; verificación humana pendiente.

### Única operación

`POST /api/players/identities/whoscored/sync`, síncrono, sin parámetros propios ni body requerido. Mantiene autenticación Bearer JWT y usuario persistido según seguridad existente; no introduce roles, CORS ni envío del JWT al proveedor.

Ejecuta actual y después histórico sobre pendientes elegibles de esa misma invocación. Actual FAILED omite histórico; sin elegibles, complemento vacío sin solicitudes externas. No se expone `/sync-runs/{id}/historical`, ni consultas o edición manual de asociaciones. No requiere ni devuelve identificadores de runs.

### Respuesta 200 — único DTO

Se adapta `PlayerIdentitySyncRunResponseDTO` al siguiente contrato, sin DTO anidado de ligas ni contadores detallados:

```json
{
  "status": "COMPLETED",
  "selectedPlayers": 2600,
  "referencedPlayers": 2450,
  "noMatchPlayers": 100,
  "unresolvedPlayers": 50
}
```

Valores ilustrativos, no evidencia de cobertura. `selectedPlayers` cuenta los jugadores locales seleccionados inicialmente, no filas WhoScored. Los otros tres campos son categorías finales excluyentes y su suma coincide con `selectedPlayers`. Referencias válidas preexistentes cuentan en `referencedPlayers`; inconsistencias de referencias, ambigüedad, conflictos y errores cuentan en `unresolvedPlayers`. Un NO_MATCH provisional resuelto en histórico cuenta solo como referenciado. Vacío devuelve cuatro ceros.

`status` solo admite COMPLETED, PARTIAL, FAILED. Si algún pase ejecutado termina FAILED, el global es FAILED; en otro caso PARTIAL si alguno es parcial; resto COMPLETED. 200 puede contener cualquiera de esos tres estados: finalización del flujo no garantiza identificación de todos los jugadores. Sin tiempos de auditoría, `id`, `passType`, `sourceRunId`, `counters` ni `leagues`.

### Errores y seguridad

| HTTP | code / comportamiento | Situación |
| --- | --- | --- |
| 401 | Cadena de seguridad existente, sin asumir ErrorResponseDTO en filtro | JWT ausente, expirado, inválido o usuario no admitido por seguridad vigente. |
| 500 | `PLAYER_IDENTITY_SYNC_FAILED`, ErrorResponseDTO | Fallo de preparación/configuración o global que impide construir un resultado coherente; referencias ya confirmadas permanecen. |

ErrorResponseDTO conserva `timestamp`, `status`, `error`, `code`, `message`, `path`, con mensaje español y sin secretos o cuerpos del proveedor. Fallos aislados de descubrimiento/confirmación se expresan mediante estado global y totales, no errores HTTP por jugador. Colisión de alias/binding interno inexistente se rechaza antes de red. El endpoint histórico y sus errores específicos de origen/ID se retiran; no se redefine el código de ruta inexistente de la infraestructura global.

### Consumidores y documentación

Cambio incompatible deliberado: el operador deja de invocar dos POST. Adaptar OpenAPI de Controller/DTO, Mapper y tests de Controller a todos los casos contractuales, E2E y consumidores de tests de estadísticas que preparan identidades o deserializan el DTO. No modificar listado/detalle, Football-Data, TheSportsDB ni frontend. No recuperar REST Docs/AsciiDoc retirados del proyecto: las fuentes son este contrato, anotaciones OpenAPI y tests funcionales de Controller.

El reintento manual relanza la operación completa sin ID de origen; las referencias existentes se conservan por idempotencia. No se impone ejecución única irreversible ni scheduler.

## Antecedente histórico — contrato sustituido de dos endpoints

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

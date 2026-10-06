# Contrato HTTP: imágenes de jugadores

Amplía el [contrato de catálogo](../../002-player-catalog/contracts/api.md). Todas las rutas requieren autenticación vigente, sin rol administrativo nuevo. Errores gestionados mantienen el formato de error existente (`timestamp`, `status`, `error`, `code`, `message`, `path`); mensajes en español. Timestamps de ejemplos en UTC; `durationMillis` deriva de inicio/fin y es `null` mientras el run esté activo.

## `GET /api/players`

Mantener paginación y campos actuales (`id`, `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality`, `imageUrl`); **añadir `fallbackImageUrl` nullable**. Ejemplo de elemento en `content`:

```json
{"id":7821,"name":"Ejemplo","team":"Club","league":"Liga","position":"Forward","dateOfBirth":null,"nationality":null,"imageUrl":"https://www.thesportsdb.com/images/media/player/cutout/example.png","fallbackImageUrl":null}
```

Ambas URLs pueden ser `null`. Consulta local sin llamadas a TheSportsDB y sin referencias externas, estados de resolución o auditoría. Consumidores existentes conservan los campos actuales; los nuevos consumidores prueban URL principal, alternativa y placeholder local ante fallo real de carga.

## `POST /api/players/images/sync`

Solicitud manual sin body; query opcional `force` booleano (`false` por defecto). Síncrona: responde al concluir. `force=true` salta la elegibilidad de los activos, pero **no** vuelve a buscar identidad externa ya resuelta. Un único run activo por instancia; una solicitud concurrente devuelve `409` sin crear otro run.

`200 OK` para `COMPLETED` o `PARTIAL`:

```json
{
  "id": 43,
  "status": "PARTIAL",
  "force": false,
  "startedAt": "2026-10-01T10:15:00Z",
  "finishedAt": "2026-10-01T10:17:32Z",
  "durationMillis": 152000,
  "failureReason": null,
  "counters": {
    "evaluated": 105,
    "processed": 100,
    "found": 80,
    "notFound": 14,
    "retryableErrors": 5,
    "failed": 1,
    "conflicts": 1,
    "skippedFound": 3,
    "skippedRetryWindow": 1,
    "skippedFailed": 1,
    "interrupted": 0
  }
}
```

`NOT_FOUND` por sí solo permite `COMPLETED`; cualquier `RETRYABLE_ERROR`, `FAILED` de jugador o conflicto deja `PARTIAL`, incluso un conflicto único. `conflicts` es un subconjunto de `failed`: no sumarlo dos veces. `evaluated` coincide con los items de jugadores activos al inicio de su evaluación individual e incluye skips; `processed` solo cuenta items con intento real contra TheSportsDB. Un cambio posterior de `active` no cancela ni revierte un item iniciado. La respuesta nunca embebe items.

Errores: `401` sin credenciales válidas; `409 Conflict` (código `PLAYER_IMAGE_SYNC_IN_PROGRESS`) si ya hay run activo; un error global/fatal deja run `FAILED` y devuelve el estado HTTP de error correspondiente (código `PLAYER_IMAGE_SYNC_FAILED`, cuando sea posible), conservando resultados y auditoría previos. Un error de configuración antes de admitir la ejecución también devuelve error y no afirma que se haya creado un run.

## `GET /api/players/images/sync-runs`

Historial autenticado paginado: `page` desde 0 (por defecto 0), `size` 1–100 (por defecto 20). Estos defaults y límites son decisiones de diseño siguiendo la paginación existente del catálogo, **no** requisitos funcionales adicionales de la spec; esta solo exige paginación. `200 OK` con `content` de resúmenes con los campos del POST y metadatos `page`, `size`, `totalElements`, `totalPages`; ordenar por `startedAt` descendente, `id` descendente para empates. Una ejecución `RUNNING` muestra `finishedAt`, `durationMillis` y `failureReason` nulos. `400` ante paginación inválida, `401` sin autenticación. No se incluyen items.

## `GET /api/players/images/sync-runs/{id}`

`200 OK` devuelve solo el resumen del run (mismo esquema que POST), nunca el detalle; `404` (`PLAYER_IMAGE_SYNC_RUN_NOT_FOUND`) si no existe, `401` sin autenticación.

## `GET /api/players/images/sync-runs/{id}/items`

Detalle autenticado por run, paginado con `page` desde 0 (por defecto 0), `size` 1–100 (por defecto 20), orden estable `id` ascendente. Son las mismas decisiones de diseño de paginación del catálogo, no valores fijados por la spec. `200 OK`:

```json
{
  "content": [
    {"playerId":7821,"previousState":"PENDING","finalState":"FOUND","result":"FOUND","identityResolved":true,"imageFoundOrUpdated":true,"skipped":false,"skipReason":null,"conflict":false,"errorOccurred":false,"outcomeDetail":"Imagen asociada"},
    {"playerId":7822,"previousState":"FOUND","finalState":"FOUND","result":"SKIPPED","identityResolved":false,"imageFoundOrUpdated":false,"skipped":true,"skipReason":"FOUND","conflict":false,"errorOccurred":false,"outcomeDetail":"Omitido por imagen existente"},
    {"playerId":7823,"previousState":"PENDING","finalState":"FAILED","result":"FAILED","identityResolved":false,"imageFoundOrUpdated":false,"skipped":false,"skipReason":null,"conflict":true,"errorOccurred":true,"outcomeDetail":"Identidad externa ya vinculada a otro jugador"}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 105,
  "totalPages": 6
}
```

`skipReason`: `FOUND`, `RETRY_WINDOW`, `FAILED` solo para omisiones. `result` puede ser `INTERRUPTED` para un item inconcluso tras reinicio; `finalState` en ese caso conserva el último estado de resolución durable (no existe estado de resolución `INTERRUPTED`). Quien está inactivo al comenzar su evaluación individual no tiene item; si se inactiva después de comenzar como activo, conserva el item y su resultado normal. `404` si el run no existe, `400` por paginación inválida, `401` sin autenticación. No exponer secretos ni respuesta cruda de TheSportsDB.

En el detalle, `finalState: NOT_FOUND` puede coexistir con URLs válidas conservadas si un lookup por referencia existente devolvió una respuesta válida sin resultados; no implica error técnico ni retry dentro del run. Si devuelve la identidad esperada sin imágenes nuevas, `finalState` es `FOUND` cuando existían imágenes válidas, o `NOT_FOUND` si no. Datos presentes de esa identidad inequívocamente incompatibles según FR-015 producen `FAILED`, sin rematching ni cambio de referencia.

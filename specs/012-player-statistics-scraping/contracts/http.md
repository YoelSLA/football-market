# Contrato HTTP: extracción de estadísticas WhoScored

## Reglas comunes

- Base: `/api/players/statistics/whoscored`.
- JWT Bearer vigente y usuario persistido, sin rol adicional.
- El JWT nunca se propaga a WhoScored.
- Errores controlados mediante `ErrorResponseDTO`.
- Fechas ISO-8601 y timestamps UTC.
- La API no expone valores individuales de métricas.

## Endpoints de sincronización independientes

| Método y ruta relativa | Canal fijo | Acceso externo |
| --- | --- | --- |
| POST `/sync/scraping` | `SCRAPING` | Todos los pendientes de la fecha en un POST Playwright/Chromium headed, 45 métricas; fecha opcional en body. |
| POST `/sync/json` | `JSON` | Todos los pendientes JSON del día UTC; quince consultas conocidas por jugador, 57 métricas, sin navegador, parámetros ni body. |

Contrato objetivo actualizado el 2026-10-27, pendiente de implementación. Son endpoints distintos, sin POST genérico `/sync`. Comparten respuesta, no request; la ruta fija el canal. Ambos recorren todos sus pendientes en una petición síncrona sin límite total de tiempo o cantidad. Ninguna petición ejecuta ambos canales.

### Request de POST `/sync/scraping`

```json
{
  "extractionDate": "2026-10-15"
}
```

- `extractionDate`: opcional; por defecto, fecha UTC actual; no puede ser futura.
- `maxPlayers` retirado: su presencia, incluso null, se rechaza con `400 INVALID_REQUEST` antes de invocar Service. No se ignora ni se interpreta como límite. Cambio incompatible explícito respecto del contrato anterior; todos los clientes deben retirar ese campo.
- Body JSON obligatorio; `{}` usa los defaults anteriores. El Request DTO no declara `source`; la selección del canal solo depende de la ruta y ningún campo del body puede cambiarla.
- No acepta IDs: el universo proviene del catálogo local.
- Resuelve fecha al inicio y fija un snapshot de todos los pendientes por ID local ascendente con referencia confirmada. Lo conserva al cruzar medianoche; cada ID se intenta una sola vez, sin retries de jugador, sección, categoría o navegación dentro del POST.
- Timeouts por operación y ritmo siguen activos, sin deadline total ni corte por estimación. Un fallo individual no detiene el resto. Fallo global/interrupción cierra con pendientes explícitos y sin confirmar extracción incompleta por fallo.
- Fallidos, no-data y jugadores sin ningún valor numérico obtenido permanecen pendientes para otro POST; una métrica opcional ausente no invalida el resto. Los completados no vuelven a consultarse esa fecha/canal. Sin pendientes: `200 COMPLETED`, seleccionados y llamadas cero, sin abrir navegador.
- Cobertura al cerrar contra elegibles vigentes, incluidos nuevos elegibles posteriores al snapshot. Haber recorrido todos los seleccionados no garantiza cobertura completa.

### Request de POST `/sync/json`

```http
POST /api/players/statistics/whoscored/sync/json
Authorization: Bearer <token>
```

- Sin query parameters ni body: no acepta fecha, `source`, `maxPlayers` ni IDs. No se envía `{}` ni se requiere `Content-Type` de un body inexistente. Query parameters o body no vacío se rechazan con `400 INVALID_REQUEST`; los headers de autenticación se conservan.
- La fecha UTC se resuelve en el backend una vez al iniciar y se informa en el resumen. Se conserva al cruzar medianoche; una petición nueva otro día trabaja sobre la nueva campaña.
- Se fija la población elegible de referencias confirmadas al abrir el run y se seleccionan todos sus jugadores sin `COMPLETED` para esa fecha y `JSON`, por ID local ascendente. Aproximadamente 2100 si toda la población está pendiente; no se exige que el tamaño sea exactamente 2100 ni se hace muestreo.
- Una repetición el mismo día omite los ya completados y vuelve a intentar solo los pendientes/fallidos. Cada seleccionado se intenta una vez por run, además del retry técnico por consulta permitido. No hay ciclo infinito de reselección de fallidos.
- Sin pendientes devuelve `200`, `status = COMPLETED`, `players.selected = 0`, `providerCalls = 0` y `coverageComplete = true`, sin acceso a WhoScored. Los conteos del run son cero, salvo conteos de población/cobertura que corresponden al catálogo vigente.
- No aplica `maxPlayers`, presupuesto de 30 minutos ni corte por ritmo estimado. Mantiene workers configurados, pacing, timeouts de consultas y límite de retry. Responde al finalizar el recorrido o al cerrar por fallo global/interrupción con pendientes explícitos; fallo individual no invalida otros jugadores. La indisponibilidad global no se trata como un motivo para insistir indefinidamente.
- La cobertura se evalúa contra los elegibles vigentes al cerrar: si se añadieron referencias después del snapshot, quedan pendientes para una nueva petición y no se declara cobertura completa.

### Response común `200 OK`

```json
{
  "runId": 42,
  "source": "SCRAPING",
  "extractionDate": "2026-10-15",
  "status": "PARTIAL",
  "startedAt": "2026-10-15T12:00:00Z",
  "finishedAt": "2026-10-15T12:29:48Z",
  "players": {
    "eligible": 2100,
    "excludedWithoutReference": 35,
    "selected": 2100,
    "completed": 2097,
    "withoutData": 1,
    "failed": 2,
    "pending": 3
  },
"metrics": {
    "present": 12000,
    "absent": 75,
    "notDerivable": 0
  },
  "providerCalls": 2040,
  "coverageComplete": false
}
```

El ejemplo ilustra un recorrido completo de scraping con tres pendientes no resueltos, no una medición de duración, métricas ni llamadas. Para JSON, la misma estructura declara `source = "JSON"` y fecha UTC inicial, sin request DTO. `players.selected` cuenta jugadores efectivamente intentados y auditados, no los del snapshot que no llegaron a iniciarse por interrupción. `selected = completed + withoutData + failed`; los no intentados permanecen incluidos en `pending`. `providerCalls` cuenta accesos realmente iniciados, no el número teórico de secciones.

`status`:

- `COMPLETED`: recorrido sin fallos (puede haber no-data), o sin trabajo pendiente; no garantiza cobertura completa. También considera nuevos elegibles al evaluar cobertura, sin corte por límite temporal/cantidad.
- `PARTIAL`: al menos un jugador confirmado y uno fallido.
- `FAILED`: había jugadores seleccionados y no se confirmó ninguno por fallo. Cero trabajo con cobertura completa no es fallo.
- `INTERRUPTED`: cierre controlado tras interrupción.

`coverageComplete` solo es `true` si `pending = 0` para fecha y canal. Las ausencias se informan solo como conteo agregado; `notDerivable` se mantiene por compatibilidad de contrato y siempre vale 0, porque una derivada no derivable se persiste como columna nula y se cuenta como ausencia. La respuesta no expone contadores de contraste entre canales.

### Errores

| HTTP | `code` | Condición |
| --- | --- | --- |
| 400 | `INVALID_STATISTICS_EXTRACTION_DATE` | Solo scraping: fecha futura o formato inválido. |
| 400 | `INVALID_REQUEST` | Scraping: body requerido ausente, JSON malformado o presencia del campo retirado `maxPlayers`. JSON: query parameters o body no vacío. |
| 401 | gestionado por seguridad | JWT ausente, expirado o inválido. |
| 409 | `PLAYER_STATISTICS_SYNC_IN_PROGRESS` | Ya existe una ejecución activa para el canal. |
| 500 | `PLAYER_STATISTICS_SYNC_FAILED` | Fallo interno antes de emitir resumen. |
| 503 | `HEADED_BROWSER_UNAVAILABLE` | `SCRAPING` sin navegador o sesión gráfica. |

Los fallos individuales se reflejan en el resumen `200` si la ejecución pudo cerrarse y auditarse. `JSON` no depende del entorno gráfico y nunca produce `HEADED_BROWSER_UNAVAILABLE`.

`INVALID_STATISTICS_SOURCE` corresponde exclusivamente al filtro de `GET /coverage`, no a los POST con canal fijo. La operación de ambos POST está destinada al cliente del desarrollador y conserva la autenticación de catálogo, sin un rol nuevo.

## Cliente operativo de scraping: una petición

El cliente usa exclusivamente `/sync/scraping`, resuelve la fecha una vez y envía solo `extractionDate`. Hace un único POST; desaparecen `--mode` y `--max-players` y se rechazan localmente antes de HTTP. JWT desde entorno, nunca impreso. JSON conserva su POST directo sin entrada.

Solo `coverageComplete = true` acredita éxito de campaña (salida 0 del CLI). Si quedan pendientes, hay error HTTP/transporte o interrupción, salida distinta de cero sin repetir el POST, aunque haya progreso. El operador puede invocar otra petición para la misma fecha. Cliente y proxy deben permitir una conexión prolongada; perder la respuesta no implica que el servidor no haya confirmado datos. Consultar cobertura antes de decidir una nueva petición, sin reintento automático.

## GET `/coverage`

Consulta cobertura sin acceder al proveedor.

### Query

- `source`: obligatorio; `SCRAPING` o `JSON`.
- `extractionDate`: opcional; por defecto fecha UTC actual; no futura.

### Response `200 OK`

```json
{
  "source": "JSON",
  "extractionDate": "2026-10-15",
  "catalogPlayers": 2135,
  "eligiblePlayers": 2100,
  "excludedWithoutReference": 35,
  "completedPlayers": 102,
  "pendingPlayers": 1998,
  "failedPlayers": 2,
  "coverageComplete": false,
  "systemicallyAbsentMetrics": ["second_yellow_total"],
  "lastRunId": 42,
  "lastRunFinishedAt": "2026-10-15T12:29:48Z"
}
```

`failedPlayers` permanece incluido en `pendingPlayers`. Una fecha sin ejecuciones devuelve el catálogo vigente, cero completados y `lastRunId = null`.

`systemicallyAbsentMetrics` recoge, en español y solo cuando corresponda, las claves canónicas del catálogo sin ningún valor en la población elegible de esa fecha y canal (FR-066). Es un estado consultable derivado de las filas confirmadas, no un dato editable ni una métrica nueva; una ausencia observada solo en jugadores concretos no aparece aquí. El array se devuelve vacío cuando no hay ausencia sistémica.

### Errores

| HTTP | `code` | Condición |
| --- | --- | --- |
| 400 | `INVALID_STATISTICS_SOURCE` | Canal ausente o inválido. |
| 400 | `INVALID_STATISTICS_EXTRACTION_DATE` | Fecha futura o formato inválido. |
| 401 | gestionado por seguridad | JWT ausente, expirado o inválido. |

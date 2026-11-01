# Quickstart: validación de estadísticas WhoScored

Esta guía define escenarios posteriores a la implementación. Los tests los ejecuta el usuario; el agente no los ejecuta.

## Prerrequisitos

- Java 21 y PostgreSQL según la configuración del backend.
- Catálogo local con referencias `WHOSCORED` confirmadas.
- JWT válido de un usuario persistido.
- Para `SCRAPING`: Chromium de Playwright y sesión gráfica activa.
- Para `JSON`: acceso HTTPS al endpoint WhoScored; no requiere navegador.

Implementación en curso del cambio del 2026-10-27; validación humana pendiente. Defaults: cuatro workers por canal e intervalo de 1500 ms; ambos POST recorren todos sus pendientes sin máximo de jugadores ni deadline total. Scraping sin retries; JSON mantiene su política técnica. Contratos: [HTTP](contracts/http.md), [WhoScored](contracts/whoscored.md) y [modelo](data-model.md). Run=7 confirmó `N/A` como token rechazado: se admite como ausencia desde la corrección del 2026-10-28. La espera reconoce render con contenido idéntico; no se acredita resolución global de timeouts ni se solicita otra campaña larga como primera validación.

## Preparación

### Log completo en archivo

Al reiniciar el backend actualizado se conserva la consola y se escribe también
`logs/football-market.log` en UTF-8, relativo al directorio de trabajo. Si se inicia desde
`backend/`, queda en `backend/logs/football-market.log`; si el IDE usa la raíz, en `logs/`.
Se puede cambiar con `FOOTBALL_MARKET_LOG_FILE` o `-Dlogging.file.name=...`.
No depende del historial visible de la consola. La rotación estándar de Spring Boot
conserva segmentos comprimidos: para una corrida larga guardar el archivo actual **y sus
rotaciones**, no solo el último segmento. Copiarlos al terminar, antes de su retención/purga.
El perfil test deja vacío `logging.file.name`; no genera ese archivo al ejecutar tests.
Los directorios `logs/` de raíz/backend están ignorados por Git, incluidas rotaciones.
Revisar cualquier log general antes de compartirlo por posibles datos de otros módulos.

### Estado tras limpieza del 2026-10-30

La herramienta diagnóstica temporal y su tarea Gradle fueron retiradas. Se conservan
logs históricos sin modificarlos. La validación vigente usa tests de regresión y el
endpoint poblacional documentado abajo, no un CLI que consulte fichas individuales.

El usuario puede iniciar el backend desde `backend/`:

```bash
./gradlew bootRun
```

El arranque no abre Chromium ni consulta WhoScored. La falta de display afecta solo a una ejecución `SCRAPING`.

## Escenario 1: población JSON pendiente del día

```bash
curl -X POST http://localhost:8080/api/players/statistics/whoscored/sync/json \
  -H "Authorization: Bearer <token>"
```

Verificar que:

- no se abre navegador;
- se recorren las quince combinaciones conocidas;
- solo se aceptan las 57 métricas declaradas para JSON;
- `shots_on_target_total`, `second_yellow_total` y `starts_total` no se inventan;
- la respuesta separa jugadores, métricas y llamadas; no incluye contadores de contraste entre canales.
- no hay query parameters, body ni `Content-Type` requerido; se procesan todos los pendientes elegibles, aproximadamente 2100 si la campaña es nueva, sin corte de 30 minutos;
- la fecha de campaña es la UTC fijada al iniciar y se conserva al cruzar medianoche;
- otra llamada el mismo día omite los confirmados y solo reintenta pendientes/fallidos; con cobertura completa no hay consultas al proveedor;
- una llamada iniciada otro día UTC abre una campaña nueva sin borrar el histórico.

## Escenario 2: todos los pendientes de scraping en un POST

```bash
curl -X POST http://localhost:8080/api/players/statistics/whoscored/sync/scraping \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"extractionDate":"2026-10-15"}'
```

Verificar Chromium visible por worker, contextos independientes, recorrido de 45 métricas sin omitir carga inicial, resumen defensivo ni detalle. Los workers pueden solaparse sin monitor global; al cerrar no deben quedar recursos huérfanos. La petición intenta todos los pendientes iniciales una vez, sin retries de sección/categoría, sin corte a los 30 minutos ni reselección de fallidos. Para probar un solo jugador usar catálogo controlado, no un parámetro de selección.

Scraping conserva body obligatorio: `{}` resuelve UTC actual; fecha explícita nunca futura y fija durante todo el POST. `maxPlayers` se retira: enviarlo, incluso null, debe devolver `400 INVALID_REQUEST` antes del Service. Cliente y proxy deben permitir horas de espera; no repetir automáticamente si se pierde la respuesta. Consultar cobertura y decidir manualmente otra petición. JSON no cambia su entrada.

### Cómo consultar los pendientes de scraping

```bash
curl "http://localhost:8080/api/players/statistics/whoscored/coverage?source=SCRAPING&extractionDate=2026-10-15" \
  -H "Authorization: Bearer <token>"
```

Pendientes = elegibles sin item `COMPLETED` para fecha/`SCRAPING`; incluye no intentados, fallidos, interrumpidos y no-data. Por ejemplo, 2100 menos 300 completados dejan 1800 pendientes. Un segundo POST de la misma fecha intenta solo esos pendientes una vez, sin repetir completados; no se encadena automáticamente. Confirmar JSON no cambia esta cobertura. Nuevos elegibles durante el recorrido quedan para otra petición.

## Escenario 3: actualización entre canales

1. Completar por scraping un jugador para la fecha UTC actual.
2. Invocar JSON sin parámetros/body; el backend fija esa misma fecha si la petición se inicia ese día UTC.
3. Confirmar que la fila JSON de esa campaña existe con sus 57 métricas y que la fila de scraping sigue intacta, sin ser sobrescrita ni degradada.
4. Confirmar que las tres métricas exclusivas del scraping siguen presentes en su fila.
5. En una fecha de campaña posterior no futura, ejecutar nuevamente scraping y comprobar que crea una fila nueva sin alterar las anteriores. No intentar una segunda confirmación satisfactoria del mismo jugador/canal/fecha: ese caso está excluido por la unicidad de campaña.
6. Verificar que ninguna fila de campaña anterior fue sobrescrita.

## Escenario 4: alias y derivación

Para un contexto de scraping con `apps = 2`, `goal = 6` en el resumen y tasas nativas
de detalle `foulCommitted = 2.5`, `duelAerialTotal = 2.6`:

- se deriva `goals_per_match = 3.000000` desde el acumulado real del resumen;
- se conservan `fouls_committed_per_match = 2.5` y `total_aerial_duels_per_match = 2.6`
  sin dividirlas por apps otra vez;
- no se crea un acumulado a partir de esas tasas de detalle;
- `foulsPerGame` alimenta la misma clave y no crea equivalencia adicional;
- `apps = 0` deja ausentes las derivadas de acumulados reales, pero no elimina una tasa
  nativa publicada en detalle. JSON no se modifica; revisar el riesgo independiente TD-035.

## Escenario 5: ausencia frente a cero

- `-`, `N/A` (tras recorte), vacío o columna ausente esperada deja la columna de la métrica en `NULL`;
- `0` se persiste como valor numérico cero;
- una ausencia posterior no borra el resto de la fila de campaña;
- una estructura obligatoria ausente falla el jugador y no confirma datos parciales.

### Alcance de regresión conservado

Los tests DOM opt-in fueron retirados por solicitud expresa del usuario. No se requiere
configurar variables ni navegador para los tests de protocolo. Estos usan dobles de
Playwright y no ejecutan el JavaScript de las esperas; la brecha permanece en TD-034.
Las campañas reales no sustituyen regresiones deterministas de estados negativos.

### Precisión decimal sin pérdida — regresión de FR-029

En los tests backend previstos, utilizar un proveedor controlado cuyo bloque estructurado
publique `Rating: 6.9033333333333333`. Comprobar lectura y normalización exactas sin
conversión a punto flotante, e ida/vuelta por PostgreSQL sin redondeo de esa columna `NUMERIC`
sin precisión ni escala fijas. Esta comprobación se realiza por las fronteras de Integration,
Model y Repository correspondientes; no se añade un endpoint de métricas ni acceso directo a DB
desde los tests de Service.

Conservar además el caso de derivación `1 / 3 = 0.333333` con seis decimales HALF_UP:
la precisión variable de almacenamiento no modifica FR-027. El modelo vigente no persiste la
precisión observada por métrica ni tolerancias de contraste, de modo que no hay caso de contraste
que verificar. Son pruebas pendientes del usuario tras su implementación, no resultados observados.

## Escenario 6: cobertura independiente

```bash
curl "http://localhost:8080/api/players/statistics/whoscored/coverage?source=JSON&extractionDate=2026-10-15" \
  -H "Authorization: Bearer <token>"
```

Repetir con `source=SCRAPING`. Los conteos y `coverageComplete` deben ser independientes; completar JSON no completa scraping.

## Escenario 7: fallos y seguridad

- Sin JWT: 401 antes de acceder al proveedor.
- En GET `/coverage`, fuente inválida o ausente: `INVALID_STATISTICS_SOURCE`; los POST fijan su canal por ruta.
- Scraping: cualquier presencia de `maxPlayers`, body ausente o JSON malformado produce `INVALID_REQUEST`; fecha futura produce `INVALID_STATISTICS_EXTRACTION_DATE`.
- POST JSON con query parameters o body no vacío (incluso `{}`): `INVALID_REQUEST`; sin entrada es el uso válido.
- Segunda ejecución simultánea del mismo canal: `PLAYER_STATISTICS_SYNC_IN_PROGRESS`.
- `SCRAPING` sin display: `HEADED_BROWSER_UNAVAILABLE`, sin run parcial.
- `JSON` sin display: puede ejecutarse normalmente.
- 403, 404 y estructura inválida: sin retry.
- Logs, respuestas e incidencias: sin JWT, cookies, credenciales, HTML ni JSON completo.

## Cliente operativo de scraping: una petición

El cliente adaptado hace un único POST. El agente no lo ejecuta.

El desarrollador configura `FOOTBALL_MARKET_TOKEN` en su entorno sin publicarlo ni incluirlo en argumentos. Desde `backend/`:

```bash
# Un POST que intenta todos los pendientes de la fecha una vez
./gradlew statisticsSyncClient --console=plain --args="--extraction-date 2026-10-15 --base-url http://localhost:8080"
```

Sin fecha explícita, el CLI resuelve UTC al inicio y la conserva. `--mode` y `--max-players` se rechazan localmente sin HTTP. El cliente no ejecuta JSON ni envía JWT a WhoScored.

Código 0 significa cobertura completa. Con pendientes o error HTTP/transporte/interrupción, salida distinta de cero y fin, aunque haya progreso. Una nueva petición queda a decisión del operador. No se añade scheduler ni rol API.

## Regresiones del nuevo alcance (proveedor controlado)

- Snapshot con completados previos, fallido, no-data, contexto resuelto sin valores y jugadores válidos: una pasada intenta cada pendiente una vez; segundo POST solo pendientes; sin pendientes no abre navegador.
- Clock controlado: cruzar medianoche conserva fecha y superar los antiguos 30 minutos no corta ni permite otra ejecución concurrente. Un run realmente huérfano tras reinicio se recupera sin perder confirmaciones; no por edad de un POST vivo.
- Datos no numéricos: reproducir únicamente tokens observados y confirmar su clasificación correcta, sin inventar cero ni perder precisión. Si falta el token concreto, pedir captura mínima saneada antes de fijar la corrección.
- Navegación: una sección obligatoria que falla no dispara recargas/retries ni confirma extracción parcial; el siguiente jugador sigue.
- Limpieza: cierre por hilo y ordenado, incluido fallo de cierre sin ocultar fallo principal, sin pérdida de conteos ni recursos abandonados. No tests exclusivos de logs.
- JSON e identidad conservan comportamiento, contrato, ritmo y retries. Sin llamadas a WhoScored real en pruebas.

## Evidencia humana de rendimiento — SC-012

Objetivo histórico informativo: unas 10 horas para aproximadamente 2100 jugadores; no es deadline ni garantía. Registrar configuración efectiva (defaults actuales: cuatro workers y 1500 ms), población inicial, fecha/canal, inicio/fin, intentados, cobertura final e incidencias de ese POST. Duración y cobertura son resultados distintos; el objetivo temporal no aprueba gates. JSON se mide por separado.

El benchmark histórico de 21–27 s y la extrapolación de 4–5 horas no prueban el objetivo del diseño final. Esta guía no ejecuta campaña, benchmark ni tests.

## Verificación automatizada a cargo del usuario

`tasks.md` conserva los IDs históricos. Desde PowerShell en `backend/`, con Docker disponible para PostgreSQL/Testcontainers:

```powershell
.\gradlew.bat test --tests '*PlayerStatistics*Test' --tests '*ExtractionPopulationExecutorTest' --tests '*ExtractionWorkerPoolTest' --tests '*WhoScoredScraping*Test' --tests '*WhoScoredStatistics*Test' --tests '*WhoScoredStructuredDataParserTest' --tests '*WhoScoredRequestPacerTest' --tests '*MetricValuePipelineTest' --tests '*HeadedBrowserStartupTest'
```

Para la regresión completa, ejecutar `.\gradlew.bat test`. No se ejecutaron estos comandos desde el agente; el build sin tests no compila ni ejecuta estas pruebas. No usar `--mode`, `--max-players` ni `maxPlayers` en requests válidos.
## Regresión de apertura y lectura nativa tras limpieza (2026-10-30)

Corrección de apertura: espera resumen inicial renderizado/seleccionado y detalle
oculto antes del único clic. Confirma selección y visibilidad CSS real después.
No aumenta timeouts ni reintenta. Desde `backend/`, el usuario ejecuta:

```powershell
.\gradlew.bat lightweightTest --tests "footballmarket.integrations.WhoScoredScrapingIntegrationTest"
```

No se conserva suite DOM ni activación por variable de entorno.

Comprobar también normalización nativa y logging general conservado:

```powershell
.\gradlew.bat unitTest --tests "footballmarket.models.MetricCatalogTest" --tests "footballmarket.models.MetricValuePipelineTest"
.\gradlew.bat lightweightTest --tests "footballmarket.config.BackendFileLoggingTest"
```

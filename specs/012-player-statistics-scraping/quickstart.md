# Quickstart: validación de estadísticas WhoScored

Esta guía define escenarios posteriores a la implementación. Los tests los ejecuta el usuario; el agente no los ejecuta.

## Prerrequisitos

- Java 21 y PostgreSQL según la configuración del backend.
- Catálogo local con referencias `WHOSCORED` confirmadas.
- JWT válido de un usuario persistido.
- Para `SCRAPING`: Chromium de Playwright y sesión gráfica activa.
- Para `JSON`: acceso HTTPS al endpoint WhoScored; no requiere navegador.

Defaults: cuatro workers por canal, lote de 30 minutos solo para scraping, intervalo de 1500 ms y retries técnicos acotados. JSON recorre todos los pendientes del día UTC sin parámetros ni body, sin máximo de jugadores ni corte temporal de lote. Contratos: [HTTP](contracts/http.md), [WhoScored](contracts/whoscored.md) y [modelo](data-model.md).

## Preparación

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

## Escenario 2: lote de scraping

```bash
curl -X POST http://localhost:8080/api/players/statistics/whoscored/sync/scraping \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"extractionDate":"2026-10-15","maxPlayers":1}'
```

Verificar Chromium visible, contextos independientes y cobertura de 45 métricas. El recorrido no debe omitir carga inicial, resumen defensivo ni subcategorías de detalle.

Solo scraping recibe este body: `maxPlayers = 1` limita a un pendiente entre todos los workers, no elige un ID. El backend selecciona por ID local ascendente con referencia confirmada; omitir el máximo deja el límite temporal y `{}` utiliza fecha UTC actual. La fecha del ejemplo se reemplaza por la campaña elegida, nunca futura. JSON no recibe este DTO ni sus parámetros.

### Cómo consultar los pendientes de scraping

```bash
curl "http://localhost:8080/api/players/statistics/whoscored/coverage?source=SCRAPING&extractionDate=2026-10-15" \
  -H "Authorization: Bearer <token>"
```

Pendientes = elegibles sin item `COMPLETED` para esa fecha y `SCRAPING`; incluye no intentados, fallidos e interrumpidos. Por ejemplo, 2100 elegibles menos 300 completados dejan 1800 pendientes. Se excluyen por existencia persistida, no por un cursor o lista mantenidos por el desarrollador. Confirmar JSON no modifica este conteo de scraping. Para reanudar, conservar la misma fecha en los POST posteriores; otra fecha inicia otra campaña.

## Escenario 3: actualización entre canales

1. Completar por scraping un jugador para la fecha UTC actual.
2. Invocar JSON sin parámetros/body; el backend fija esa misma fecha si la petición se inicia ese día UTC.
3. Confirmar que la fila JSON de esa campaña existe con sus 57 métricas y que la fila de scraping sigue intacta, sin ser sobrescrita ni degradada.
4. Confirmar que las tres métricas exclusivas del scraping siguen presentes en su fila.
5. En una fecha de campaña posterior no futura, ejecutar nuevamente scraping y comprobar que crea una fila nueva sin alterar las anteriores. No intentar una segunda confirmación satisfactoria del mismo jugador/canal/fecha: ese caso está excluido por la unicidad de campaña.
6. Verificar que ninguna fila de campaña anterior fue sobrescrita.

## Escenario 4: alias y derivación

Para un contexto con `apps = 10` y `foulCommitted = 25`:

- se persiste `fouls_committed_per_match = 2.5`;
- no se persiste `foulCommitted = 25` como métrica;
- `foulsPerGame` alimenta la misma clave y no crea equivalencia adicional;
- `apps = 0` deja la métrica sin derivar y la persiste como columna nula, igual que una ausencia del proveedor.

## Escenario 5: ausencia frente a cero

- `-`, vacío o columna ausente esperada deja la columna de la métrica en `NULL`;
- `0` se persiste como valor numérico cero;
- una ausencia posterior no borra el resto de la fila de campaña;
- una estructura obligatoria ausente falla el jugador y no confirma datos parciales.

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
- Solo scraping: `maxPlayers` inválido produce `INVALID_STATISTICS_MAX_PLAYERS`; body requerido ausente/malformado produce `INVALID_REQUEST`; fecha futura produce `INVALID_STATISTICS_EXTRACTION_DATE`.
- POST JSON con query parameters o body no vacío (incluso `{}`): `INVALID_REQUEST`; sin entrada es el uso válido.
- Segunda ejecución simultánea del mismo canal: `PLAYER_STATISTICS_SYNC_IN_PROGRESS`.
- `SCRAPING` sin display: `HEADED_BROWSER_UNAVAILABLE`, sin run parcial.
- `JSON` sin display: puede ejecutarse normalmente.
- 403, 404 y estructura inválida: sin retry.
- Logs, respuestas e incidencias: sin JWT, cookies, credenciales, HTML ni JSON completo.

## Cliente operativo de scraping: lote o campaña

Contrato de ejecución entregado en T091 mediante `PlayerStatisticsSyncClient` y la tarea Gradle `statisticsSyncClient`. El agente no ejecuta el cliente.

El desarrollador configura `FOOTBALL_MARKET_TOKEN` en su entorno sin publicarlo ni incluirlo en `clientArgs`. Desde `backend/`:

```bash
# Una petición, como máximo un jugador pendiente por scraping
./gradlew statisticsSyncClient --console=plain -PclientArgs="--mode=batch --max-players=1 --extraction-date=2026-10-15 --base-url=http://localhost:8080"

# Campaña de scraping: lotes temporales encadenados hasta completar o detenerse
./gradlew statisticsSyncClient --console=plain -PclientArgs="--mode=campaign --extraction-date=2026-10-15 --base-url=http://localhost:8080"
```

Sin fecha explícita, el cliente de scraping resuelve la fecha UTC una vez y la conserva al cruzar medianoche. `campaign` consulta cobertura inicial y no hace POST si está completa; continúa solo con reducción de pendientes. Con máximo de 1 encadena lotes de un jugador; `batch` realiza solo uno. Este cliente no ejecuta JSON ni envía JWT a WhoScored; para JSON basta el POST directo del escenario 1.

Código 0 de campaña significa cobertura completa. Falta de progreso, `FAILED`, `INTERRUPTED`, error HTTP/transporte o interrupción local significan salida distinta de cero y campaña incompleta; no se repite automáticamente un POST incierto. El desarrollador puede reanudar luego con la misma fecha/canal sin repetir confirmaciones. No se añade scheduler ni rol nuevo a la API.

## Evidencia humana de rendimiento — SC-012

Objetivo informativo acordado: unas 10 horas de tiempo transcurrido para scraping de aproximadamente 2100 jugadores, con la configuración efectiva que se declare (cuatro workers e intervalo de 1500 ms) entre inicios de navegación de secciones por worker, con proveedor disponible. Registrar población elegible inicial, configuración efectiva, fecha/canal de campaña, inicio y fin, pendientes finales e incidencias de disponibilidad. El tiempo incluye los lotes encadenados. El resultado se informa como medición con sus limitaciones; no constituye criterio de aceptación ni habilita o bloquea gates, y no se afirma cumplimiento por alcanzar o no la estimación. JSON se mide por separado.

El benchmark histórico de 21–27 s y la extrapolación de 4–5 horas no prueban el objetivo del diseño final. Esta guía no ejecuta campaña, benchmark ni tests.

## Verificación automatizada a cargo del usuario

`tasks.md` define tests de catálogo, alias, derivaciones, persistencia de la fila de campaña y unicidad entre runs, Integrations controladas, Service, ambos POST, validación de `maxPlayers`, seguridad y journeys E2E del cliente/canales. T031 y T033 se escriben en el checkpoint fundacional conservando IDs; su ejecución queda pendiente del usuario. Ningún test accede a WhoScored real. Los comandos exactos de tests se obtendrán de la configuración efectiva al implementar y los ejecutará el usuario.

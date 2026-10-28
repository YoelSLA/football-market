# Research: Scraping de estadísticas de rendimiento de jugadores

## Catálogo y cobertura observada

**Decision**: Tratar como evidencia validada el catálogo cerrado de 60 métricas: 45 disponibles por scraping, 57 por JSON, 42 compartidas y tres exclusivas del scraping (`shots_on_target_total`, `second_yellow_total`, `starts_total`). Las quince combinaciones JSON recorridas forman parte del contrato externo.

**Rationale**: Las pruebas previas recorrieron el DOM y las respuestas JSON, identificaron columnas técnicas y permitieron enumerar la cobertura completa. Repetir esa investigación durante PLAN no aporta una decisión nueva y aumentaría presión sobre un endpoint no oficial.

**Alternatives considered**: Mantener solo ocho métricas JSON quedó invalidado por la evidencia; descubrir métricas durante ejecución impediría validar estructura y cobertura; configurar el catálogo en base de datos permitiría combinaciones fuera de la spec.

## Canales independientes y vigencia

**Decision**: Permitir ejecuciones independientes `SCRAPING` y `JSON`. Cada canal persiste su fila de campaña por jugador, fecha y canal, con la procedencia real en la propia fila; no existe valor vigente único entre canales. Una ausencia no sobrescribe ni degrada la fila del otro canal.

**Rationale**: Cumple la necesidad de actualizar por cualquiera de las vías, permite que un canal complete al otro y conserva auditoría sin duplicar conceptos canónicos.

**Alternatives considered**: Prioridad fija de scraping o JSON contradice la aclaración; sobrescribir filas elimina histórico; exigir ambos canales en una misma ejecución impide actualización independiente.

## Playwright y navegador

**Decision**: Incorporar `com.microsoft.playwright:playwright:1.63.0` y ejecutar Chromium con `headless=false`. La instalación del navegador es prerrequisito operativo, no acción del arranque de Spring.

**Rationale**: La verificación previa mostró 403 en cinco variantes headless y respuesta estable en headed. Playwright aporta aislamiento de contextos y acceso al DOM desde Java.

**Alternatives considered**: HTTP directo no reproduce el scraping exigido; Selenium agrega gestión de driver; ocultar automatización o falsear User-Agent no resolvió el bloqueo.

## Ciclo de vida del navegador

**Decision**: Playwright y Chromium headed por worker, confinados a su hilo; contextos independientes por sección. Sondeo headed al abrir sesión, recursos de worker bajo demanda y cierre en su hilo al terminar el recorrido. No compartir navegador ni driver. El orden de cierre y sus fallos observados requieren investigación acotada, sin atribuir una causa no demostrada.

**Rationale**: Playwright Java está confinado al hilo que lo crea. Compartir una instancia entre los cuatro workers obligaba a serializar el recorrido completo bajo un bloqueo y anulaba el paralelismo; el run `SCRAPING` del 2026-10-25 agotó su deadline con items que arrancaron juntos y terminaron escalonados. Una instancia por worker conserva el aislamiento de contexto, permite navegaciones simultáneas y mantiene la aplicación arrancable sin entorno gráfico.

**Alternatives considered**: El navegador compartido con `synchronized` serializaba navegación, esperas, las dieciséis categorías y el cierre del contexto, y contradecía FR-018/FR-012; un navegador por sección multiplica los procesos sin aportar aislamiento adicional; un singleton de arranque viola FR-055.

## Cliente JSON

**Decision**: Reutilizar la configuración segura del cliente dedicado WhoScored de SPEC-008 y ampliar una Integration separada para las quince combinaciones de categoría/subcategoría recorridas y documentadas en el contrato. Validar `playerTableStats`, `statColumns`, contexto y columnas esperadas aunque el MIME sea `text/html`.

**Rationale**: El endpoint observado devuelve JSON válido con MIME HTML y requiere User-Agent de navegador. Separar el adaptador estadístico evita acoplarlo al contrato de identidad y permite validar las 57 métricas conocidas.

**Alternatives considered**: Reutilizar records de identidad mezcla contratos; aceptar campos dinámicos sin catálogo oculta cambios de estructura; requerir MIME JSON rechaza respuestas válidas observadas.

El control de ritmo, timeouts y `Retry-After` no se duplica: se extiende el pacer y la configuración segura del cliente ya existentes para el canal de identidad, con valores por canal. Crear un segundo pacer o un segundo cliente habría producido dos políticas de ritmo que pueden divergir frente al mismo proveedor.

## Paralelismo, ritmo y duración

**Decision**: Desde la aclaración del 2026-10-27, ambos canales recorren todos sus pendientes en una petición, sin lote temporal ni máximo de jugadores. Cuatro workers por canal e intervalo de 1500 ms sin cambios. Scraping sin retries de jugador, sección, navegación o categoría; JSON mantiene su retry técnico existente. Conservar timeouts de operaciones externas.

**Rationale**: El usuario eligió un POST síncrono completo y una tentativa por pendiente, aceptando duración prolongada. El objetivo de 10 horas es informativo histórico, no deadline ni SLA. No se promete obtener métricas válidas de toda la población en una pasada.

Pacing por navegación de sección en scraping y por consulta/retry existente en JSON, no por recurso. Se retiran el deadline, la media de ritmo y `maxPlayers`; no sustituirlos por un presupuesto total de 24 h ni por otro límite oculto. Snapshot único de pendientes por POST. Fallos individuales continúan; interrupción o fallo global cierran con pendientes declarados. No recuperar scraping por antigüedad: exclusión por canal en la instancia única y recuperación solo de runs sin ejecución viva propia, conforme a `plan.md`.

**Alternatives considered**: Lotes encadenados, `maxPlayers`, retries de scraping y ejecución asíncrona quedan descartados por decisión humana. Más workers o bajar timeouts a ciegas no resuelven una causa demostrada y no forman parte del alcance.

## Reanudación e idempotencia

**Decision**: Campaña por fecha UTC y canal; solo `COMPLETED` excluye. Cada POST fija un snapshot ascendente de pendientes e intenta cada ID una vez. Fallidos, no-data y sin métricas permanecen para otro POST. Una ausencia opcional aislada no invalida la extracción con datos.

JSON no repite completados del día y fija UTC al inicio sin entrada. Scraping conserva fecha opcional/default durante la petición, incluso al cruzar medianoche. Otro POST de esa fecha reintenta solo pendientes. Sin pendientes no hay proveedor. No se añade modo forzado.

La unicidad entre runs se garantiza mediante fecha/canal no nulos en el item, FK compuesta `(run_id, extraction_date, source)` al run e índice único parcial por `(extraction_date, source, player_id)` solo para `COMPLETED`. Los intentos fallidos se conservan; una colisión revierte la confirmación atómica entera. Los detalles están en `data-model.md`.

**Rationale**: Evita que completar JSON impida ejecutar scraping o viceversa, permite reanudar cada vía y mantiene cortes históricos comparables.

**Alternatives considered**: Una campaña única para ambos canales confunde coberturas; cursor del cliente puede omitir jugadores; deduplicar solo por timestamp no identifica el corte lógico.

## Persistencia y transacciones

**Decision**: Transacciones breves al abrir/cerrar run y confirmar por jugador su fila aplanada e item; acceso externo sin transacción. Histórico por campaña/canal, no por observación. Scraping sin valor numérico presente es `NO_DATA`, incluso con contexto resuelto; su fila sin éxito previo es reconfirmable, sin sobrescribir un item `COMPLETED`. JSON conserva su semántica.

**Rationale**: Cumple FR-032/FR-035/FR-063, conserva todo el histórico y evita inconsistencias entre una tabla actual y sus observaciones.

**Alternatives considered**: Transacción por lote retiene recursos durante minutos; actualización destructiva pierde histórico; tabla current duplicada requiere sincronización innecesaria.

## Catálogo, alias y derivaciones

**Decision**: Implementar 60 definiciones inmutables con clave, unidad, canales admitidos, columnas externas y regla de derivación. `foulsPerGame` y `foulCommitted / apps` alimentan `fouls_committed_per_match`. Los 29 acumulados se descartan después de derivar y no son métricas persistidas.

**Rationale**: Mantiene el contrato acordado, permite validar cobertura por canal y evita 29 claves adicionales y una entidad de equivalencias.

**Alternatives considered**: Persistir acumulados amplía el catálogo a 89; una `MetricEquivalenceDefinition` es innecesaria para alias que convergen en una sola clave; inferir columnas desconocidas viola el catálogo cerrado.

## Valores ausentes y decimales

**Decision**: Columnas `NUMERIC` nullable sin escala fija, `BigDecimal` desde el token original. Ausencia y no derivable se guardan como null, no como cero. Precisión observada, tolerancias y diferencias no se persisten en el modelo vigente; el diseño normalizado anterior es histórico.

**Rationale**: Distingue ausencia de cero y conserva el decimal publicado, sin error binario ni redondeo de valores directos. No reintroduce el contraste retirado.

La corrección autorizada el 2026-10-16 sustituye el diseño anterior `NUMERIC(18,6)`:
la captura local `RecorridoHTML/PaginaPrincipal.txt:1219` publica el rating
`6.9033333333333333`, incompatible con seis decimales y con FR-029 si se redondea.
No se impone un límite de seis decimales al normalizador ni al parser. Solo las derivaciones
por `apps` conservan los seis decimales `HALF_UP` de FR-027; el rating directo no se deriva.
El mapping JPA debe declarar `numeric` sin precisión/escala fijas y coincidir con Flyway;
los tests previstos comprobarán ida/vuelta exacta y metadatos de precisión con PostgreSQL.

**Alternatives considered**: `NUMERIC(18,6)` redondea valores reales del proveedor;
fijar otra escala solo desplaza el límite sin responder al requisito de conservar el decimal
publicado; `double` pierde precisión; centinelas contaminan cálculos; omitir toda fila ausente
impide auditar cobertura. La precisión de comparaciones tampoco se limita artificialmente:
con 16 decimales publicados, la mitad de la unidad mínima es `0.00000000000000005`.

## Contrato HTTP

**Decision**: Scraping conserva POST con body obligatorio y fecha opcional, pero retira `maxPlayers` y rechaza su presencia con `400 INVALID_REQUEST`, incluso null. JSON mantiene POST sin entrada. Se conserva salida común y GET de cobertura. Actualización incompatible explícita en DTO, Model de entrada, Mapper, OpenAPI, advice, CLI y tests; no ignorar el campo retirado.

**Rationale**: Las rutas y entradas distintas expresan las operaciones requeridas por el usuario. Ambas son síncronas, con idempotencia diaria por canal y auditoría compartida; JSON no requiere conocer población, IDs ni tamaño de lote. Compartir salida e infraestructura interna no obliga a recibir el mismo request ni a limitar JSON por tiempo de scraping.

**Alternatives considered**: POST único con `source` descartado por la aclaración humana; ejecutar ambos canales juntos contradice su independencia; ejecución asíncrona contradice FR-050.

## Cliente operativo del desarrollador

**Decision**: Simplificar el CLI existente a una sola petición de scraping con fecha y base URL opcionales; retirar/rechazar `--mode` y `--max-players` antes de HTTP. Mantener tarea Gradle y JWT desde entorno sin impresión. Sin scheduler, bean nuevo ni loop de peticiones. JSON se invoca directamente sin entrada.

**Rationale**: Un POST ya recorre todo el snapshot; encadenar repetiría fallidos/no-data en contra de la decisión de reintento solo en otra acción del operador.

**Parada**: finalizar después de ese POST; código 0 solo con cobertura completa. Pendientes o error HTTP/transporte: salida distinta de cero, sin retry automático. Una conexión perdida puede coexistir con confirmaciones del servidor; consultar cobertura antes de otra acción manual.

## Investigación pendiente y límites de evidencia — 2026-10-27

Síntomas aportados: rechazo decimal `PROVIDER_DATA_INCONSISTENT`, fases de 45–47 s y `Failed to send message` en cierre de workers. La lectura estática encuentra bucles de retry y esperas en `WhoScoredScrapingSectionNavigator`, corte de población y parada por timeout individual en `ExtractionBatchExecutor`, recuperación por antigüedad en `ExtractionRunManager` y confirmación por contexto (no por valores presentes) en `PlayerStatisticsWriter`. Son puntos a adaptar o investigar; no demuestran la causa del token decimal ni de la excepción de cierre.

**Decision**: seguir datos y ciclo de vida con capturas/logs locales y fixtures controlados antes de corregir; registrar evidencia y regresión por causa confirmada. No introducir formatos, fallback a cero, optimización especulativa o cambios de JSON/identidad. Si falta el token concreto o una reproducción, pedir evidencia mínima al usuario. Tests y campaña solo del usuario; investigación técnica ordenada antes de la corrección, no hallazgo resuelto.

### Investigación de implementación — 2026-10-27

- **Esperas confirmadas por lectura estática**: `navigation-timeout-ms=45000` rige navegación, controles y tabla. El navigator anterior envolvía cada sección en dos intentos, detalle en dos cargas y categoría en tres intentos más cambio forzado de categoría. Se retiraron esos retries y el segundo clic dirigido; se conserva el timeout de operación. Esto elimina trabajo repetido, no acredita una mejora de duración medida ni explica todo bloqueo DOM.
- **Cierre confirmado por lectura estática**: `ExtractionWorkerPool.close()` llamaba siempre `shutdownNow()`. El `ThreadFactory` ejecutaba `releaseWorker()` después de `runnable.run()`; el Future podía terminar antes de la limpieza, y el executor podía recibir la interrupción de cierre mientras Browser/Playwright aún enviaban mensajes. Ahora una tarea duradera por worker consume IDs y ejecuta su cleanup en `finally` antes de completar el Future; `close()` hace shutdown normal. La interrupción explícita detiene nuevos IDs, espera los Futures y limpia con el indicador de interrupción temporalmente retirado, restaurándolo después. Orden: contextos de cada sección → Browser → Playwright, todos en su hilo. Los fallos secundarios se conservan como suppressed y la fachada cierra auditoría tras la limpieza. Se escribió regresión controlada, sin ejecutarla. **No se afirma reproducida ni resuelta la excepción específica `Failed to send message` del run humano**; falta contraste con su stacktrace y una nueva ejecución humana.
- **Ausencia demostrable en código**: `ScrapingMetricAssembler.publishedValue` convertía null/vacío/guion en cero, contradiciendo FR-022/031. Se delega al normalizador existente; no cambian JSON ni los formatos decimales admitidos. Un contexto sin valores es NO_DATA y reconfirmable; la regresión desde Service atraviesa ensamblador y persistencia reales.
- **Token decimal aún pendiente**: se consultaron las capturas locales ignoradas de `RecorridoHTML/` sin acceder al proveedor. `PaginaPrincipal.txt:1178` contiene `N/A` en la fila de amistoso internacional (`tournament 27`) del resumen; `:1219` contiene el bloque tournaments y el rating exacto `6.9033333333333333`. La fila N/A no demuestra la causa del run afectado: el parser descarta competiciones sin entrada vigente en el bloque, y no se dispone del token/celda y correlación del jugador fallido de ese run. No se convierte N/A en cero, no se amplía el normalizador ni se da por corregido el rechazo decimal. Evidencia mínima solicitada: run/jugador **local**, sección, nombre técnico de columna, token saneado tal como llega al ensamblador y stacktrace de cierre saneado, sin URL, JWT, cookies ni documento completo. T106 y las regresiones causales dependientes quedan parcialmente pendientes.

### Evidencia de run=6 e instrumentación autorizada

- Resumen humano: 2053 seleccionados, 442 completados, 57 NO_DATA, 1554 fallidos;
  4106 navegaciones observadas. El fragmento muestra timeouts de 45 s al seleccionar
  filtros, paneles de detalle vacíos y rechazos decimales después de extraer filas.
- En el código leído, `chooseCategory:253` del stack original corresponde al cambio de
  acumulación a `2`, no necesariamente al cambio de categoría `aerial`. La atribución
  depende de confirmar que el binario ejecutado coincide con esa fuente.
- La espera compara `innerHTML` y presencia de encabezados; no prueba finalización de
  la petición. No se cambia aún su criterio ni se atribuye una causa sin reproducción.
- Diagnóstico añadido: filtro anterior/solicitado, cambio de HTML, estado de controles y
  tabla al timeout; eventos XHR/fetch con estado HTTP y fallos de transporte; presencia
  de errores JavaScript sin su mensaje. No se registran URLs, headers, cuerpos ni cookies.
- Rechazos decimales: run/jugador local, worker, sección, columna y contexto. El token
  numérico corto o marcador conocido se representa por puntos Unicode para conservar
  espacios/guiones invisibles sin inyección de líneas; otros textos quedan redactados.
  No se amplían formatos admitidos ni se convierten ausencias en cero.
- Los eventos de red incluyen también peticiones auxiliares: un HTTP fallido aislado
  no acredita que haya fallado el endpoint de estadísticas. Los errores JavaScript se
  cuentan por evento, sin stack ni mensaje externo; si no basta se pedirá captura saneada.
- `scraping-diagnostics.sql` permite contrastar resultados e incidencias y los éxitos
  previos de campaña que explicarían los 51 no seleccionados. No ejecutado por el agente.
- T106 sigue pendiente de evidencia humana y corrección causal; instrumentar no acredita
  resolución de los fallos ni aceptación funcional.

### Evidencia correlacionada de run=7 — 2026-10-28

- Archivo humano: `Logs Scraping 27-10-2026.txt`, 5930 líneas. JSON completo:
  1611 seleccionados, 345 completados, 53 NO_DATA, 1213 fallidos, 1266 pendientes.
  El log comienza con línea cortada; timestamps completos de 02:28:33.848 a
  02:39:31.628 -03, solo los últimos once minutos de una ejecución de unas 4 h 35 min.
  No extrapolar sus frecuencias a toda la corrida.
- 46 eventos finales `PROVIDER_TIMEOUT` en detailed, de los cuales 35 tienen diagnóstico
  de filtro `#statsAccumulationType`, `0 → 2`, `cambioTabla=false`. Los otros once no
  se atribuyen al filtro sin evidencia adicional. Siete eventos finales
  `PROVIDER_DATA_INCONSISTENT`, todos correlacionados con `invalid-decimal` de
  `initial/minsplayed`, token `U+004E U+002F U+0041` (`N/A`). Confirma el token pendiente
  del diagnóstico anterior, no que todos los fallos históricos sean de la misma causa.
- Estados de respuestas observados: 200 XHR (651), 200 fetch (491), 204 fetch (111),
  ninguno 4xx/5xx registrado. Eventos JavaScript/fetch no prueban causalidad porque
  también aparecen en fases exitosas y no identifican el recurso de estadísticas.
- La condición anterior `innerHTML != previous` falla necesariamente si se renderiza
  una tabla idéntica. El log no distingue esa situación de una selección sin render.
  Bajo autorización humana se sustituye por MutationObserver armado antes de seleccionar,
  que detecta incorporación de tabla/cuerpo/fila completa o escritura de celda, aunque
  el texto sea igual. Exige encabezados, cuerpo, carga oculta y valor solicitado;
  no se espera red global. También se espera cuerpo/carga oculta en la tabla inicial.
- `N/A` literal recortado se admite como ausencia sin cero ni precisión; otros textos
  siguen rechazados. La regla compartida también afecta a consumidores JSON del normalizador,
  sin cambiar recorrido, requests ni política de retry de ese canal.
- Sin render observable se conserva el timeout; no se acepta automáticamente la tabla
  anterior ni se afirma que esta modificación resuelva los 35 casos. Las capturas locales
  de detalle muestran un spinner hermano del contenedor de tabla: observar únicamente
  la tabla evita contar sus cambios de estilo como actualización.
- Regresiones de protocolo escritas, no ejecutadas; no ejercitan el JavaScript real:
  brecha registrada como TD-034. Persisten validación humana de DOM/proveedor y causas
  de paneles vacíos. T106/T107 siguen abiertas; no se declara resolución global.

## Ubicación de las reglas de dominio

Normalización y derivación/alias se ubican en `models/` (`MetricValueNormalizer`, `MetricDerivationApplier`). Service obtiene datos y persiste resultados; la regla numérica no depende de Repository ni Integration. La tolerancia y clasificación de contraste se retiraron con el contraste entre canales el 2026-10-18, por lo que `MetricComparisonRule` no forma parte del modelo vigente.

## Historial de aclaraciones superadas

Esta sección conserva, sin aplicarlas, las decisiones de aclaración que quedaron
reemplazadas dentro de sus propias sesiones. Se mantienen como evidencia de proceso y
están resumidas en la tabla de decisiones vigentes de `spec.md`.

- 2026-10-15, conflicto de valor entre canales: la decisión inicial era resolver la
  precedencia por canal; quedó reemplazada por la regla temporal de FR-063 (observación
  más reciente, desempate por identificador mayor).
- 2026-10-15, persistencia de acumulados: la decisión inicial era conservar el valor
  acumulado junto a la derivada; quedó reemplazada por FR-027, que persiste solo la
  métrica por partido derivada.
- 2026-10-15, duración de la población: la extrapolación de 4 a 5 horas con tres workers
  a partir del recorrido completo de cinco secciones; quedó reemplazada como objetivo del
  diseño final por la aclaración del 2026-10-16, sin borrar la medición previa. El
  recorrido vigente es el reducido y la estimación es informativa (SC-012).
- 2026-10-15, alcance del lote: la decisión inicial era aplicar el lote acotado a ambos
  canales; quedó precisada el 2026-10-16, con lotes solo en scraping y recorrido completo
  de los pendientes del día en JSON (FR-012, FR-043).
- 2026-10-16, scraping por lotes de 30 minutos, media de 20 muestras, máximo opcional y cliente `batch/campaign`: sustituido el 2026-10-27 por snapshot completo en un POST, sin máximo/deadline ni retries de scraping. Las aclaraciones originales se conservan en `spec.md` como bitácora histórica.

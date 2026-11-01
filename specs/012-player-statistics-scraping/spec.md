# Feature Specification: Scraping de estadísticas de rendimiento de jugadores

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/012-player-statistics-scraping` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `<spec-id>-<scope>`, sin el prefijo de tipo. La convención de ramas es normativa; ver la constitución §7.

**Created**: 2026-10-15

**Status**: Ready for implementation. Se conserva el estado canónico de preparación; IMPLEMENT fue autorizado e iniciado para los alcances registrados en [sdd-gates.md](sdd-gates.md), incluidas las correcciones de lectura nativa, apertura y limpieza temporal. La campaña humana completa run 11 acredita recorrido poblacional, no cierre global ni aprobación de revisión. CODE REVIEW, validación posterior a la limpieza y cierre permanecen pendientes. Seguimiento remoto no verificado.

<!-- Estados y criterios de transición: ../../specs/README.md.
Histórico, no vigente: el Analyze previo a la corrección decimal reportó 0 CRITICAL, 0 HIGH,
0 MEDIUM y 2 LOW, con cobertura nominal 79/79 requisitos sobre 93 Tasks; I4/I5 se corrigieron
como ajustes editoriales sin alterar el alcance analizado. Ese resultado quedó invalidado por
la corrección decimal. El Analyze posterior fue confirmado por el usuario sin que su informe
detallado esté disponible en esta sesión; no se inventan conteos. Sin sincronización remota de Trello. -->

<!-- La numeración de requisitos no es secuencial: FR-043 a FR-048 y FR-062 a FR-066 se
incorporaron por bloques temáticos a medida que se precise el diseño. No MUST renumerarse,
porque hacerlo rompería la trazabilidad con sdd-gates.md, plan.md y tasks.md. -->

> **Alineada con la implementación vigente (2026-10-18).** La cobertura efectiva es 45/57/42 y el
> canal JSON recorre quince combinaciones. El contraste entre canales, el histórico por métrica y
> la disponibilidad por métrica no forman parte del modelo vigente; los FR que los exigían quedan
> **retirados** conservando su identificador, sin renumerar. Detalle en `data-model.md` §Historial
> y en `docs/TECHNICAL_DEBT.md`.

**Input**: User description: "Scraping de estadisticas de rendimiento de jugadores de las cinco ligas mediante scraping con Playwright sobre WhoScored, recorriendo los identificadores externos ya persistidos en el catalogo local, persistiendo las metricas obtenidas para toda la poblacion de jugadores, y verificando el resultado contrastandolo contra el endpoint publico del proveedor"

## Contexto y evidencia previa

### Estado vigente tras validación y limpieza — 2026-10-30

Scraping conserva resumen y dieciséis categorías en contextos independientes por
sección, Chromium headed por worker y una tentativa por jugador. La apertura espera
resumen renderizado/seleccionado y detalle oculto, hace un clic y confirma selección
y visibilidad CSS. Detalle conserva modo nativo `0` y espera render/carga real incluso
con HTML idéntico; sus valores por partido no se dividen otra vez por apariciones.
Resumen mantiene sus derivaciones; `N/A` es ausencia, no cero. Competición/temporada,
60/45/57/42, exactitud decimal, persistencia y contrato HTTP se conservan.

Run 11 humano: 2104 seleccionados, 1889 completados, 214 NO_DATA y un timeout;
89,78 % con datos, sin expectativa de que todos los elegibles publiquen estadísticas.
`coverageComplete` sigue siendo false con pendientes: no se relaja el contrato para
declarar éxito. El caso 2449 fue informado como arreglado por el usuario sin nuevo
run aportado; no se reescriben los resultados originales.

Se retira el código de diagnóstico temporal (CLI, captura red/DOM, hooks, Logback y
tarea Gradle). Se conservan la sincronización productiva, los tests de regresión y
el logging general a archivo. La auditoría numérica JSON TD-035 queda fuera de alcance.

Decisión posterior del 2026-10-30: retirar los tests DOM opt-in y su variable de
activación. Permanecen tests de protocolo/Model y evidencia humana histórica, pero
no se considera que una campaña sustituya la cobertura DOM determinista (TD-034).

Esta spec describe la extracción de estadísticas de rendimiento de jugadores mediante dos vías de WhoScored: scraping con Playwright sobre el sitio público y consulta de su interfaz JSON pública. Ambas recorren los jugadores que ya tienen un identificador externo WhoScored confirmado en el catálogo local y pueden actualizar datos de forma independiente. El scraping conserva íntegramente su recorrido y obtiene 45 métricas; la interfaz JSON obtiene 57. La unión de ambas coberturas alcanza las 60 métricas del catálogo.

El diseño se apoya en una verificación técnica previa (spike desechable, ejecutado fuera del repositorio) que estableció los siguientes hechos determinantes:

- **El scraping funciona con navegador visible (headed) y es rechazado con 403 si el navegador es headless.** Se probaron cinco configuraciones headless —incluidas `--headless=new`, Chrome real y ocultación completa de `navigator.webdriver`— y todas fueron bloqueadas. Headed respondió 200 de forma sostenida. La detección opera a nivel de protocolo de automatización, no solo sobre propiedades de JavaScript.
- **WhoScored agota el estado de sesión al recorrer pestañas sobre una misma página.** La tercera o cuarta pestaña deja de responder: el contenedor queda vacío y la llamada de red no llega a dispararse. Cargar cada sección en un contexto de navegador nuevo resuelve el problema de forma estable.
- **La respuesta de la página incluye un bloque de datos estructurado en el HTML original** que expone titulares, apariciones como suplente, tiros al arco, faltas recibidas y otros indicadores que las tablas visibles no presentan. Ese bloque es gratuito y no requiere interacción.
- **Cada fila de cada tabla expone los identificadores de competición y de equipo dentro de sus enlaces**, por lo que las claves foráneas se obtienen del propio DOM sin depender de nombres normalizados.
- **Las tablas declaran el nombre técnico de cada métrica como atributo de columna**, lo que evita depender de las cabeceras traducidas al idioma del sitio.
- **Las celdas vacías se representan con un guion**, distinguible de un cero real.

### Cobertura de métricas por ambos canales

La verificación previa recorrió el catálogo completo sobre tres jugadores de perfil distinto —un portero y dos jugadores de campo— y obtuvo **59 de 60 métricas** desde el DOM. Una verificación posterior del endpoint `getplayerstatistics`, sus quince combinaciones recorredas de categoría y subcategoría y las columnas declaradas en sus respuestas estableció la cobertura superpuesta que motiva esta spec:

- **45 de las 60 métricas se obtienen por scraping.** Recogen de la carga inicial de la ficha el bloque de datos estructurado y la sección de resumen, de las dieciséis subcategorías de detalle el resto de indicadores y del propio resumen las derivadas por partido. Las siete métricas del resumen defensivo se atribuyen en el catálogo vigente al canal JSON. Este recorrido no cambia por incorporar la vía JSON.
- **57 de las 60 métricas se obtienen de la interfaz JSON pública.** Incluyen las quince métricas de los resúmenes ofensivo, de distribución y defensivo que el scraping no atribuye a su canal y 42 métricas también cubiertas por scraping. Las métricas no observadas en ninguna de las quince respuestas JSON comprobadas son `shots_on_target_total`, `second_yellow_total` y `starts_total`.
- **42 métricas tienen cobertura por ambos canales.** Cada vía puede actualizar sus métricas sin exigir que la otra se ejecute en la misma operación; cada fila conserva la procedencia real del canal que la produjo.

El recorrido completo de cinco secciones de la ficha obtuvo **59 de 60 métricas** pero multiplicaba el coste por página. El recorrido definido en FR-005 mantiene la carga inicial y la sección de resumen defensiva en contextos independientes conforme a FR-016, y permite que la interfaz JSON actualice de forma independiente las 57 métricas que publica, sin eliminar ni reducir el scraping exigido por el enunciado.

### Coste medido del recorrido reducido

Un benchmark de rendimiento ejecutó el recorrido reducido —carga inicial más las dieciséis subcategorías de detalle— sobre dos jugadores de perfil distinto. El desglose por fase descarta la hipótesis de que el coste estuviera en la carga de la página:

| Fase | Tiempo |
| --- | --- |
| Carga inicial de la ficha | 1,3 s |
| Espera del render de la sección de resumen | 1,3 s |
| Lectura del bloque de datos y de la tabla de resumen | 0,4 s |
| Carga de la ficha para el recorrido de detalle | 1,1 s |
| Apertura de la pestaña de detalle | 2,8 s |
| Recorrido de las dieciséis subcategorías | entre 11,6 y 18,7 s |
| **Total por jugador** | **entre 21 y 27 s** |

La carga de la página resulta ser la fase más barata: **entre 1 y 1,4 s con independencia del filtrado de recursos de terceros**. El coste se concentra en el recorrido de las dieciséis subcategorías, donde domina el tiempo de espera entre interacciones y no la transferencia de datos.

La comparación con y sin filtrado de publicidad, seguimiento y notificaciones push **no mostró una diferencia significativa a favor del filtrado**: una corrida sin filtrado fue más rápida que otra con filtrado, dentro del margen de variación entre ejecuciones. Por tanto, **el filtrado de recursos de terceros no se considera un mecanismo de optimización de rendimiento** y queda como medida opcional de reducción de carga, no como requisito de rendimiento.

La extrapolación histórica de 21–27 s por jugador daba 12–15 horas secuenciales o 4–5 horas con tres workers. Es un antecedente, no una medición del diseño actual. El objetivo histórico de unas 10 horas para aproximadamente 2100 jugadores es informativo, sin umbral de aceptación ni deadline de petición. Su medición corresponde al usuario, con proveedor disponible y configuración declarada; no se extiende a JSON.

**Requisito del enunciado**: `tp/enunciado.md` §1.1 y §3.2 nombran WhoScored mediante **scraping** para extraer datos detallados de rendimiento (pases, tiros, intercepciones, calificaciones) y exigen al menos dos estrategias de valuación configurables basadas en métricas como minutos jugados, goles, asistencias, tiros al arco, pases, intercepciones, tarjetas y rating. Esta spec cubre la obtención de esas métricas mediante scraping como vía primaria; el cálculo de valuación pertenece a la feature correspondiente.

## Clarifications

Este bloque recoge únicamente las decisiones vigentes. Las decisiones que quedaron
reemplazadas, con su redacción original, se conservan en `research.md` §Historial de
aclaraciones superadas como evidencia de proceso y MUST NOT aplicarse.

| # | Pregunta | Decisión vigente |
| --- | --- | --- |
| C1 | ¿El scraping recorre la ficha completa o basta la sección de detalle? | Se limita a la carga inicial, el bloque de datos estructurado, la sección de resumen defensiva y las dieciséis subcategorías de detalle (FR-004, FR-005). |
| C2 | ¿Cómo se recorre la población pendiente? | Un POST síncrono de scraping intenta todos los pendientes de la fecha una vez, sin lotes ni límites de tiempo total o cantidad; los no resueltos pueden reintentarse en otro POST (FR-011–FR-014). |
| C3 | ¿Se promete cobertura total en una sola ejecución? | Se promete recorrer todos los pendientes salvo fallo global o interrupción, no obtener datos válidos de todos. Los fallos y no-data mantienen la cobertura incompleta (FR-011, FR-014). |
| C4 | ¿Qué métricas quedan fuera del recorrido de scraping y de dónde se obtienen? | Las 8 de los resúmenes ofensivo y de distribución se obtienen por JSON (FR-062), sin ampliar el recorrido de scraping (FR-001). |
| C5 | ¿La interfaz pública sustituye al scraping? | No. El scraping conserva su recorrido; JSON queda como actualización independiente (FR-001, FR-045). |
| C6 | ¿Qué ocurre si un jugador no registra goles o asistencias para derivarlas por partido? | La métrica queda sin derivar y marcada como tal; no se inventa valor (FR-027, FR-031). |
| C7 | ¿El filtrado de recursos de terceros mejora el rendimiento? | No. La medición no mostró diferencia significativa; es medida opcional de reducción de carga, no requisito de rendimiento (FR-019). |
| C8 | ¿La ejecución que lanza la extracción es síncrona o asíncrona? | Síncrona: cada canal recorre todos sus pendientes en una petición (FR-050, FR-043). |
| C9 | ¿Qué diferencia entre contenido visible e interfaz pública se acepta por redondeo? | No se contrasta el valor de ambos canales: cada fila conserva solo el valor y la procedencia del canal que la produjo (FR-044, FR-047 y FR-048 quedan retirados). |
| C10 | ¿Las métricas opcionales no publicadas se listan una a una en el resumen? | Solo como conteo agregado de métricas ausentes, sin listado individual por clave (FR-051). |
| C11 | ¿`foulsPerGame` y `foulCommitted` se persisten como métricas propias? | No. Ambos son alias técnicos de `fouls_committed_per_match`, interpretados según su origen (detalle nativo por partido, sin nueva división), sin entrada adicional; el catálogo conserva 60 métricas (FR-064, FR-003). |
| C12 | ¿Debe incorporarse `fouls_received`? | No. El catálogo conserva únicamente `fouls_committed_per_match` de esa sección (FR-030, FR-065). |
| C13 | ¿Qué cobertura debe tener la interfaz JSON sin reducir el scraping? | El scraping conserva su recorrido y 45 métricas; JSON obtiene 57 y ambos canales pueden actualizar por separado, con unión de 60 (FR-001, FR-045, FR-008). |
| C14 | ¿Qué valor prevalece cuando ambos canales actualizan una métrica compartida? | No hay valor vigente único entre canales: cada canal persiste su propia fila de campaña y conserva su procedencia (FR-063). |
| C15 | ¿Se convierten las 29 métricas de detalle a partir de supuestos acumulados? | No. En scraping se lee el modo publicado `0` (por partido), sin cambiar a Total ni volver a dividir por apps. Solo los acumulados reales del resumen se derivan (FR-027/FR-028). |

Las decisiones del 2026-10-16 se conservan como evidencia; las relativas a lotes, `maxPlayers` y encadenamiento quedan sustituidas por la sesión del 2026-10-27.

### Session 2026-10-16

Bitácora histórica: aplicar las sustituciones expresas de la sesión del 2026-10-27.

- Q: ¿Quién ejecuta y encadena los lotes? A: Para scraping, un cliente operativo del repositorio iniciado por el desarrollador, con modo `batch` para una petición o `campaign` para encadenarlas automáticamente. JSON se invoca directamente una vez, sin encadenamiento del cliente. No se añade scheduler ni rol nuevo.
- Q: ¿Puede limitarse la cantidad de jugadores? A: Solo el POST de scraping acepta `maxPlayers` opcional positivo. Con `1` selecciona como máximo un pendiente; sin él manda el presupuesto temporal. JSON no recibe parámetros ni body y recorre todos los pendientes de su campaña diaria. Ningún canal solicita IDs externos: usa las referencias confirmadas y el orden por ID local.
- Q: ¿Hay un único sync seleccionable? A: No. Hay dos endpoints distintos: `POST /api/players/statistics/whoscored/sync/scraping` para Playwright y `POST /api/players/statistics/whoscored/sync/json` para consultar y leer el JSON de WhoScored. La ruta determina el canal; el body no contiene `source`. `GET /coverage` conserva el filtro `source`.
- Q: ¿Cuánto significa una jornada? A: Objetivo informativo de unas 10 horas para una campaña de scraping de aproximadamente 2100 jugadores con tres workers y pacing de 2500 ms, con proveedor disponible. No es una ejecución ya medida ni un criterio de aceptación con umbral; se informa la medición humana con sus limitaciones (SC-012). JSON se evalúa por separado.
- Q: ¿Cuándo continúa el cliente? A: Conserva fecha y canal y continúa solo si disminuyen los pendientes respecto de la cobertura consultada antes del lote. Finaliza con éxito únicamente con cobertura completa; sin progreso, ante interrupción, fallo global o error HTTP/transporte termina como campaña incompleta sin reintentos automáticos de esa petición.
- Q: ¿Qué recibe el POST JSON y cuántos jugadores recorre? A: No recibe query parameters ni body, solo autenticación. Al iniciar fija la fecha UTC y recorre todos los jugadores elegibles pendientes de ese día (aproximadamente 2100 en una campaña nueva), sin `maxPlayers` ni corte de lote de 30 minutos. Si cruza medianoche conserva la fecha inicial. El resumen se devuelve al finalizar el recorrido o cerrarse por fallo global/interrupción.
- Q: ¿Una segunda llamada JSON vuelve a actualizar a todos el mismo día? A: No. El usuario confirmó «si es el mismo dia no hace falta». Se omiten los `COMPLETED` para jugador/fecha/canal y se reintentan únicamente pendientes o fallidos. Con cobertura completa no se consulta WhoScored. Una llamada iniciada otro día UTC abre otra campaña; no se introduce un modo forzado de actualización.

### Session 2026-10-27

- Q: ¿Cómo debe ejecutarse el scraping de todos los pendientes? → A: En un único POST síncrono, sin lotes, presupuesto temporal total ni máximo de jugadores; sustituye las decisiones históricas de encadenamiento y `maxPlayers`.
- Q: ¿Se reintenta un jugador dentro de la misma petición? → A: No. Cada pendiente se intenta una vez por POST, sin reintentos técnicos del scraping dentro de esa petición. Fallidos, no-data y jugadores sin métricas siguen pendientes para una petición posterior; una métrica opcional ausente no invalida por sí sola una extracción con datos.
- Q: ¿Qué correcciones adicionales de scraping están dentro del alcance? → A: Investigar y corregir únicamente los fallos observados: valores rechazados como decimales, esperas prolongadas y excepciones al cerrar workers. No se presupone su causa raíz ni se ejecutan tests o campañas desde el agente.

## Catálogo de métricas

Catálogo cerrado de **60 métricas**. Cada métrica declara en qué canales está disponible: scraping, interfaz JSON o ambos, exigido por FR-008. Las columnas `statColumn` son los nombres técnicos que el proveedor declara en las tablas y respuestas; el scraping MUST identificarlas por ese atributo y MUST NOT por las cabeceras visibles traducidas (FR-020).

El sufijo `/apps` indica una métrica que el proveedor entrega acumulada y que MUST derivarse por partido según FR-027.

### Grupo 1 — Resumen de la carga inicial (9 métricas, por scraping)

| # | Clave canónica | `statColumn` | Unidad |
| --- | --- | --- | --- |
| 1 | `minutes_played_total` | `minsPlayed` | `SEASON_TOTAL` |
| 2 | `appearances_total` | `apps` | `SEASON_TOTAL` |
| 3 | `goals_total` | `goal` | `SEASON_TOTAL` |
| 4 | `assists_total` | `assistTotal` | `SEASON_TOTAL` |
| 5 | `shots_per_match` | `shotsPerGame` | `PER_MATCH` |
| 6 | `pass_completion_percentage` | `passSuccess` | `PERCENTAGE` |
| 7 | `aerial_duels_won_per_match` | `aerialWonPerGame` | `PER_MATCH` |
| 8 | `yellow_cards_total` | `yellowCard` | `SEASON_TOTAL` |
| 9 | `red_cards_total` | `redCard` | `SEASON_TOTAL` |

### Grupo 2 — Resumen defensivo (7 métricas y un alias técnico, por interfaz JSON)

Publicadas por el proveedor también en las tablas del recorrido de scraping, pero atribuidas en el
catálogo vigente al canal JSON: el ensamblado de scraping no las confirma. Su recorrido se mantiene
por FR-004 y FR-005, pero no aportan métricas a la cobertura del canal de scraping.

| # | Clave canónica | `statColumn` | Unidad | Canal |
| --- | --- | --- | --- | --- |
| 10 | `tackles_per_match` | `tacklePerGame` | `PER_MATCH` | JSON |
| 11 | `interceptions_per_match` | `interceptionPerGame` | `PER_MATCH` | JSON |
| 12 | `clearances_per_match` | `clearancePerGame` | `PER_MATCH` | JSON |
| 13 | `blocked_shots_per_match` | `outfielderBlockPerGame` | `PER_MATCH` | JSON |
| 14 | `dribbled_past_per_match` | `wasDribbledPerGame` | `PER_MATCH` | JSON |
| 15 | `offsides_won_per_match` | `offsideWonPerGame` | `PER_MATCH` | JSON |
| 16 | `own_goals_total` | `goalOwn` | `SEASON_TOTAL` | JSON |
`foulsPerGame` no es una entrada adicional del catálogo: es un alias técnico de `fouls_committed_per_match`, al igual que `foulCommitted`. En el detalle nativo de scraping se lee por partido directamente; solo un acumulado real requiere división. Ambos nombres normalizan a la misma clave canónica conforme a FR-064.

### Grupo 3 — Subcategorías de detalle (29 métricas, por scraping)

Valores publicados en el modo `0` (por partido) de la fila de competición/temporada,
sin cambiar el selector de acumulación ni dividir nuevamente por `apps`. La fila
agregada «Total/Promedio» no sustituye al contexto de liga local.

| # | Clave canónica | `statColumn` | Subcategoría |
| --- | --- | --- | --- |
| 17 | `saves_per_match` | `saveTotal` | saves |
| 18 | `six_yard_box_saves_per_match` | `saveSixYardBox` | saves |
| 19 | `penalty_area_saves_per_match` | `savePenaltyArea` | saves |
| 20 | `outside_box_saves_per_match` | `saveObox` | saves |
| 21 | `attempted_tackles_per_match` | `tackleTotalAttempted` | tackles |
| 22 | `blocked_crosses_per_match` | `passCrossBlockedDefensive` | blocks |
| 23 | `blocked_passes_per_match` | `outfielderBlockedPass` | blocks |
| 24 | `failed_dribbles_per_match` | `dribbleLost` | dribbles |
| 25 | `total_dribbles_per_match` | `dribbleTotal` | dribbles |
| 26 | `aerial_duels_lost_per_match` | `duelAerialLost` | aerial |
| 27 | `total_aerial_duels_per_match` | `duelAerialTotal` | aerial |
| 28 | `inaccurate_long_balls_per_match` | `passLongBallInaccurate` | passes |
| 29 | `accurate_short_passes_per_match` | `shortPassAccurate` | passes |
| 30 | `inaccurate_short_passes_per_match` | `shortPassInaccurate` | passes |
| 31 | `long_key_passes_per_match` | `keyPassLong` | key-passes |
| 32 | `short_key_passes_per_match` | `keyPassShort` | key-passes |
| 33 | `six_yard_box_shots_per_match` | `shotSixYardBox` | shots |
| 34 | `penalty_area_shots_per_match` | `shotPenaltyArea` | shots |
| 35 | `outside_box_shots_per_match` | `shotOboxTotal` | shots |
| 36 | `six_yard_box_goals_per_match` | `goalSixYardBox` | goals |
| 37 | `penalty_area_goals_per_match` | `goalPenaltyArea` | goals |
| 38 | `outside_box_goals_per_match` | `goalObox` | goals |
| 39 | `cross_assists_per_match` | `assistCross` | assists |
| 40 | `corner_assists_per_match` | `assistCorner` | assists |
| 41 | `through_ball_assists_per_match` | `assistThroughball` | assists |
| 42 | `free_kick_assists_per_match` | `assistFreekick` | assists |
| 43 | `throw_in_assists_per_match` | `assistThrowin` | assists |
| 44 | `other_assists_per_match` | `assistOther` | assists |
| 45 | `fouls_committed_per_match` | `foulCommitted` | fouls |

### Grupo 4 — Derivadas del resumen (2 métricas, por scraping)

| # | Clave canónica | Regla | Unidad |
| --- | --- | --- | --- |
| 46 | `goals_per_match` | `goal` / `apps` | `PER_MATCH` |
| 47 | `assists_per_match` | `assistTotal` / `apps` | `PER_MATCH` |

### Grupo 5 — Bloque de datos estructurado (5 métricas, por scraping)

Indicadores disponibles sin interacción en el HTML original. Opcionales: si el proveedor no los publica para un jugador, se persisten como ausentes sin invalidar el resto (FR-030).

| # | Clave canónica | Campo | Unidad |
| --- | --- | --- | --- |
| 48 | `whoscored_rating_average` | `rating` | `AVERAGE_RATING` |
| 49 | `shots_on_target_total` | `ShotsOnTarget` | `SEASON_TOTAL` |
| 50 | `second_yellow_total` | `SecondYellow` | `SEASON_TOTAL` |
| 51 | `starts_total` | `GameStarted` | `SEASON_TOTAL` |
| 52 | `substitute_appearances_total` | `SubOn` | `SEASON_TOTAL` |

### Grupo 6 — Interfaz JSON y fuera del recorrido de scraping (8 métricas)

Publicadas por la interfaz JSON en las secciones de resumen ofensiva y de distribución, que el scraping MUST NOT recorrer (FR-062). La clave persistida MUST declarar su origen conforme a FR-007.

| # | Clave canónica | `statColumn` | Sección |
| --- | --- | --- | --- |
| 53 | `key_passes_per_match` | `keyPassPerGame` | summary/offensive |
| 54 | `successful_dribbles_per_match` | `dribbleWonPerGame` | summary/offensive |
| 55 | `offsides_per_match` | `offsideGivenPerGame` | summary/offensive |
| 56 | `dispossessed_per_match` | `dispossessedPerGame` | summary/offensive |
| 57 | `bad_controls_per_match` | `turnoverPerGame` | summary/offensive |
| 58 | `passes_per_match` | `totalPassesPerGame` | summary/passing |
| 59 | `successful_long_balls_per_match` | `accurateLongPassPerGame` | summary/passing |
| 60 | `successful_crosses_per_match` | `accurateCrossesPerGame` | summary/passing |

### Reparto total

| Origen | Métricas | Coste por jugador |
| --- | --- | --- |
| Scraping: carga inicial y bloque estructurado | 14 | Una carga en contexto independiente |
| Scraping: dieciséis subcategorías de detalle | 29 | Una carga por subcategoría, dentro del contexto de detalle |
| Scraping: derivadas del resumen | 2 | Sin coste adicional |
| Interfaz JSON pública | 57 | Quince combinaciones de categoría/subcategoría; su agrupación operativa se define en PLAN |

### Reglas transversales del catálogo

- El denominador de toda métrica marcada `/apps` es `apps` de la misma fila de contexto. Si es cero o no existe, la métrica queda sin derivar y marcada como tal (FR-027), sin inventar valor.
- Las métricas ausentes se persisten de forma distinguible de un cero (FR-022).
- La clave canónica es la identidad estable del dato. El `statColumn` documenta su origen técnico y no forma parte de la clave persistida.
- `shots_on_target_total`, `second_yellow_total` y `starts_total` no fueron observadas en las respuestas JSON comprobadas y mantienen el scraping del bloque estructurado como única vía definida.
- Las 42 métricas cubiertas por ambos canales conservan el mismo significado y clave canónica con independencia del canal que produzca la actualización; cada canal persiste su propia fila de campaña.

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Extraer métricas de la ficha de un jugador (Priority: P1)

Como responsable del sistema de mercado, quiero que el sistema recorra la ficha pública de un jugador en WhoScored leyendo su contenido visible y extraiga el conjunto completo de métricas de rendimiento de sus apartados, para disponer de datos reales de calidad sobre los jugadores valorados en lugar de valores de muestra.

**Why this priority**: Sin esta extracción no existe ninguna base de datos real de rendimiento, y tanto las estrategias de valuación como el cálculo de cotizaciones carecen de insumo. Es el resultado que desbloquea todo lo demás.

**Independent Test**: Se puede probar de forma independiente ejecutando la extracción sobre un solo identificador de jugador conocido y verificando que las métricas quedan persistidas con valores numéricos y sus claves de competición y temporada correctas.

**Acceptance Scenarios**:

1. **Given** un jugador con identificador externo WhoScored confirmado en el catálogo local, **When** se ejecuta la extracción para ese jugador, **Then** el sistema persiste la fila de campaña con el contexto de competición y temporada de su liga local, y las métricas asociadas a esa fila
2. **Given** la misma situación, **When** cambia el valor de una métrica en la fuente, **Then** una ejecución posterior actualiza la fila de esa misma campaña o crea la de una campaña nueva, sin sobrescribir las filas de campañas anteriores
3. **Given** un jugador cuyas celdas de una métrica están vacías en la fuente, **When** se extrae su ficha, **Then** esa métrica se persiste como ausente, de forma distinguible de un cero legítimo
4. **Given** un jugador sin datos suficientes en una pestaña concreta, **When** se extrae su ficha, **Then** esa ausencia se registra de forma explícita sin invalidar las métricas obtenidas en las demás pestañas

---

### User Story 2 - Recorrer todos los jugadores pendientes en una petición (Priority: P2)

Como responsable del sistema de mercado, quiero ejecutar la extracción sobre todos los jugadores que ya tienen identificador externo confirmado, y no solo sobre una muestra, para que la valuación cubra el catálogo completo de las cinco ligas.

**Why this priority**: El valor de una estrategia de valuación depende de la cobertura. Una población abandonada a medias produce rankings sesgados, pero depende de US1 para existir.

**Independent Test**: Con un catálogo controlado, verificar que un POST intenta todos los pendientes una sola vez, informa resultados y exclusiones, continúa tras fallos individuales y permite reintentar los no resueltos en otro POST sin repetir los completados.

**Acceptance Scenarios**:

1. **Given** el catálogo local con jugadores con y sin identificador externo confirmado, **When** se lanza el POST de scraping, **Then** se intenta cada elegible pendiente de la fecha una sola vez, sin límite de jugadores ni tiempo total, y se informan resultados, pendientes y excluidos por carecer de identificador
2. **Given** una ejecución en curso, **When** el proveedor rechaza o deja de responder para un jugador concreto, **Then** ese jugador queda registrado como fallido y el recorrido continúa con los restantes sin invalidar lo ya obtenido
3. **Given** una ejecución que se interrumpe, **When** el operador la relanza, **Then** el resultado es coherente y no duplica ni corrompe las métricas ya persistidas
4. **Given** un jugador que devuelve resultados inconsistentes o incompletos frente a lo esperado, **When** finaliza su procesamiento, **Then** no se persiste como válido y queda registrado el motivo
5. **Given** jugadores fallidos, no-data o sin ninguna métrica obtenida, **When** finaliza el POST, **Then** se reportan sin reintentarlos en esa petición y permanecen pendientes
6. **Given** un segundo POST para la misma fecha, **When** se selecciona la población, **Then** solo se intentan los pendientes, incluidos los no resueltos anteriormente, y se omiten los completados
7. **Given** un fallo global o interrupción, **When** se cierra el recorrido, **Then** se informa el resultado parcial y los pendientes sin declarar cobertura completa ni reiniciar automáticamente la petición

---

### User Story 3 - Actualizar métricas desde la interfaz JSON del proveedor (Priority: P3)

Como responsable del sistema de mercado, quiero actualizar desde la interfaz JSON pública las 57 métricas que ese canal expone usando los identificadores que ya tengo persistidos, para disponer de una segunda vía de actualización sin eliminar ni reducir el scraping.

**Why this priority**: Sin esta historia, 15 de las 60 métricas del catálogo quedan sin obtener y las 42 métricas compartidas dependen exclusivamente del scraping. Aporta completitud y una vía alternativa de actualización, pero el scraping continúa entregando métricas válidas sin ella.

**Independent Test**: Con un catálogo controlado de uno o varios jugadores identificados, invocar el POST JSON sin entrada y verificar sus 57 métricas con su procedencia de canal, sin scraping. Repetir ese día no consulta los completados; una nueva fecha UTC inicia otra campaña. No se solicita un ID al endpoint para preparar el caso de un solo jugador.

**Acceptance Scenarios**:

1. **Given** un jugador con identificador WhoScored confirmado, **When** se ejecuta la actualización desde la interfaz JSON, **Then** las 57 métricas disponibles quedan persistidas marcadas con su origen sin requerir una ejecución de scraping en la misma operación
2. **Given** una métrica disponible por ambos canales, **When** se ejecuta cualquiera de los dos, **Then** el sistema persiste la fila de campaña de ese canal sin sobrescribir ni degradar la fila del otro canal
3. **Given** una métrica que no puede obtenerse por indisponibilidad de la interfaz pública, **Then** se registra como no obtenida sin invalidar las métricas ya extraídas por scraping
4. **Given** un identificador externo persistido, **When** se ejecuta la obtención complementaria, **Then** no se requiere ninguna entrada adicional ni se solicitan identificadores fuera del catálogo local
5. **Given** una campaña JSON nueva, **When** se invoca `/sync/json` sin parámetros ni body, **Then** se recorren todos sus elegibles pendientes sin límite de jugadores ni corte de lote de 30 minutos, fijando fecha UTC al inicio
6. **Given** jugadores ya confirmados por JSON el mismo día UTC, **When** se repite la petición, **Then** se omiten esos jugadores y solo se procesan pendientes o fallidos; con cobertura completa no hay llamadas al proveedor
7. **Given** una petición iniciada otro día UTC, **When** se ejecuta JSON, **Then** se utiliza la nueva campaña diaria conservando el histórico; una petición que cruza medianoche no cambia su fecha inicial

---

### Edge Cases

- El navegador es rechazado por el proveedor con una respuesta de bloqueo durante el recorrido: el jugador se registra como fallido y el recorrido continúa, sin persistir datos parciales de esa ficha.
- La estructura del sitio cambia y una de las secciones deja de existir o cambia de identificador: el sistema lo detecta como cambio de estructura, no como ausencia de datos del jugador, y no persiste métricas incompletas como si fueran definitivas.
- Un jugador aparece en competiciones sin partidos, con tablas de una sola fila o sin ninguna fila: el sistema conserva el contexto disponible de su liga local y marca las métricas como ausentes.
- El bloque de datos estructurado del HTML original no está presente o llega incompleto en algún jugador: las métricas exclusivas de ese bloque se persisten como ausentes y el resto del scraping continúa siendo válido.
- El proveedor responde con lentitud o agota el cupo de solicitudes: el sistema respeta el ritmo configurado y registra la incidencia en lugar de intensificar las peticiones.
- Un mismo jugador tiene registros en varias temporadas simultáneas: solo se persiste la temporada vigente de su liga local, de modo que temporadas distintas no se sobrescriben entre sí.
- La ejecución se interrumpe a mitad del recorrido: al relanzarla, las métricas ya persistidas permanecen intactas y solo se procesa lo que falta.
- El proveedor cambia el número o el nombre de una métrica: el sistema registra la métrica desconocida sin abortar la extracción y sin inventar un valor.
- Un jugador desaparece del proveedor durante el recorrido: se registra como no encontrado, sin eliminar las métricas de ejecuciones anteriores.

## Requirements *(mandatory)*

### Functional Requirements

#### Alcance y fuente de datos

- **FR-001**: El sistema MUST mantener el scraping del contenido visible de la ficha pública de cada jugador como vía de extracción de 45 métricas de rendimiento, sin reducir su recorrido por la incorporación de la interfaz JSON. La interfaz JSON MUST constituir una segunda vía de actualización para las 57 métricas que publica.
- **FR-002**: El scraping MUST operar sobre un navegador con interfaz visible. El modo headless MUST considerarse no soportado por el proveedor y MUST NOT utilizarse como vía de extracción.
- **FR-003**: El sistema MUST extraer el conjunto de 60 métricas definido en el catálogo de esta spec, agrupadas en: datos de contexto y temporada, indicadores por partido, desgloses por zona de tiro y de gol, desgloses de pases, desgloses de regates, duelos aéreos y posesiones, desgloses de asistencias por tipo, indicadores derivados del total de apariciones, y rating del proveedor. Los alias técnicos no incrementan ese total.
- **FR-004**: El scraping MUST recorrer la totalidad de las dieciséis subcategorías de detalle ofrecidas por el proveedor —`aerial`, `assists`, `blocks`, `cards`, `clearances`, `dribbles`, `fouls`, `goals`, `interception`, `key-passes`, `offsides`, `passes`, `possession-loss`, `saves`, `shots` y `tackles`—, además del bloque de datos estructurado y de la sección de resumen presentes en la carga inicial de la ficha. Once de esas subcategorías aportan las 29 métricas del grupo 3 del catálogo; las cinco restantes (`cards`, `clearances`, `interception`, `offsides` y `possession-loss`) MUST recorrerse igualmente, aunque no aportan ninguna métrica del catálogo y su recorrido no amplía el conjunto de 60 métricas.
- **FR-005**: El scraping MUST extraer la sección de resumen defensiva de la ficha, además de la carga inicial y de las subcategorías de detalle. Su recorrido se conserva por exigencia del enunciado, pero las siete columnas exclusivas de esa sección se atribuyen en el catálogo vigente al canal JSON (grupo 2 del catálogo), junto con el alias técnico `foulsPerGame` de `fouls_committed_per_match`.
- **FR-062**: El scraping MUST NOT extraer las secciones de resumen ofensiva y de distribución de la ficha. Sus indicadores se obtienen por la vía complementaria definida en FR-043.
- **FR-006**: El sistema MUST NOT extraer el apartado de goles esperados en su forma detallada, ni los bloques de calor, comparativas entre jugadores ni datos de nacionalidad, que no forman parte del conjunto definido. Esta restricción no impide extraer el rating, presente en todas las secciones.
- **FR-007**: Cada valor persistido MUST declarar su origen real, de modo que se distinga la métrica obtenida mediante scraping de la obtenida mediante la interfaz JSON pública del proveedor.
- **FR-008**: El sistema MUST definir la cobertura por canal como parte del catálogo de métricas. Una métrica MAY estar disponible por scraping, por interfaz JSON o por ambos; esta cobertura MUST ser explícita e inequívoca.

#### Identificación y universo de recorrido

- **FR-009**: El recorrido MUST tomar exclusivamente los jugadores que ya tienen un identificador externo del proveedor confirmado en el catálogo local, conforme al mecanismo de referencias externas establecido por SPEC-008. MUST NOT crear jugadores, modificar su identidad interna ni alterar las asociaciones de referencias existentes.
- **FR-010**: El sistema MUST apuntar a la totalidad de los jugadores elegibles como objetivo del recorrido, sin muestreo ni descarte por criterio de posicionamiento, y MUST contabilizar de forma explícita y distinguible cuántos jugadores fueron excluidos por carecer de identificador externo confirmado.
- **FR-011**: Un POST síncrono de scraping MUST recorrer todos los elegibles pendientes de su fecha de extracción, sin lotes ni encadenamiento de peticiones. MUST intentar cada jugador una sola vez por petición, sin reintentos técnicos de scraping dentro de ella. Un fallo atribuible a un jugador MUST NOT impedir intentar los restantes; se registra para ese jugador y el recorrido continúa. Un fallo global —que impida iniciar o continuar operaciones para los jugadores restantes, o mantener/cerrar de forma segura la auditoría del run— o una interrupción MUST cerrar el recorrido con resultado y pendientes explícitos, sin reinicio automático de la petición. Solo MUST declarar cobertura completa cuando `coverageComplete = true`.
- **FR-012**: El POST de scraping MUST NOT aplicar presupuesto temporal total, deadline de lote ni máximo de jugadores y MUST NOT aceptar `maxPlayers`. La ausencia de límite total MUST NOT eliminar los timeouts de operaciones externas ni el ritmo configurado. MUST informar procesados y pendientes al finalizar. Ningún canal acepta IDs de jugadores como entrada.
- **FR-013**: El sistema MUST identificar pendientes como jugadores elegibles sin confirmación `COMPLETED` para la misma fecha de extracción y canal. MUST seleccionarlos por ID local ascendente, incluyendo no intentados, fallidos, interrumpidos, no-data y jugadores sin ninguna métrica obtenida, sin repetir confirmaciones satisfactorias de esa campaña. En scraping los no resueltos MUST permanecer pendientes para un POST posterior y MUST NOT volver a seleccionarse en el POST en curso. Scraping conserva la fecha solicitada o resuelta al inicio; JSON fija la fecha UTC al inicio. Una petición que cruza medianoche conserva su fecha inicial. Al cambiar de día se permite una nueva confirmación, conservando el histórico. Confirmar un canal MUST NOT completar el otro.
- **FR-014**: La cobertura de la población MUST ser un resultado observable y consultable del conjunto de ejecuciones, y MUST NOT presentarse como completada mientras queden jugadores elegibles sin procesar para la fecha de extracción en curso.
- **FR-015**: El sistema MUST NOT crear jugadores nuevos ni vincular jugadores locales que no estén ya en el catálogo. Un identificador externo que no resuelva contra ningún jugador local MUST registrarse como no procesado.

#### Estrategia de extracción por sesión

- **FR-016**: Cada sección del recorrido MUST cargarse en un contexto de navegador independiente, porque el proveedor agota el estado de la sesión al recorrer varias secciones sobre una misma página. El recorrido vigente MUST usar un contexto independiente para la carga inicial y otro para la sección de detalle, dentro del cual se recorren las dieciséis subcategorías. El incumplimiento de esta regla MUST considerarse fallo de extracción.
- **FR-017**: El sistema MUST NOT sobrescribir el identificador de usuario del navegador con un valor que contradiga la versión real del navegador en ejecución, por producir una huella incoherente rechazada por el proveedor.
- **FR-018**: El sistema MUST regular la velocidad de las solicitudes al proveedor mediante configuración explícita. En scraping el intervalo mínimo se aplica a los inicios de navegación de secciones por worker, no a cada recurso del navegador, y no hay reintentos dentro del POST. En JSON se aplica a los inicios de consultas por worker y se mantienen sus timeouts y retry acotado. Los aplazamientos del proveedor prevalecen sobre nuevas solicitudes; si `Retry-After` permite reanudar el pacing del recorrido, el sistema MUST respetar la espera y continuar sin reintentar al jugador afectado. Si el aplazamiento impide continuar con los jugadores restantes, MUST cerrar como fallo global conforme a FR-011. Ningún canal adopta un deadline total de lote.
- **FR-019**: El sistema MAY filtrar recursos de terceros que no aportan datos (publicidad, seguimiento y notificaciones push) para reducir carga del proveedor. El filtrado MUST NOT alterar el contenido extraído y MUST NOT entenderse como mecanismo de rendimiento: la medición previa no mostró diferencia significativa de tiempo con y sin filtrado.

#### Interpretación y normalización del contenido

- **FR-020**: El sistema MUST identificar cada métrica por su nombre técnico declarado en el atributo de columna de la tabla, y MUST NOT depender de las cabeceras visibles traducidas al idioma del sitio.
- **FR-021**: El sistema MUST normalizar los nombres de métrica a una forma canónica estable, resolviendo la diferencia de capitalización con la que el proveedor declara algunas de ellas, de modo que la misma métrica se identifique siempre igual con independencia de cómo el proveedor la etiquete.
- **FR-022**: El sistema MUST representar como ausente toda celda vacía, con guion o con el marcador `N/A` (después de recortar espacios), y MUST distinguirla de un cero legítimo. Otros textos no numéricos siguen siendo inválidos. La ausencia MUST persistirse de forma que no pueda confundirse con un valor cero al recuperar las métricas.
- **FR-023**: El sistema MUST recortar y normalizar el texto de cada celda antes de interpretarlo, porque el proveedor incluye tabuladores y espacios en los valores.
- **FR-024**: El sistema MUST interpretar los valores numéricos con independencia de que el proveedor los presente con punto o coma decimal.
- **FR-025**: El sistema MUST obtener la identidad de competición y de equipo de cada fila a partir de los identificadores presentes en los enlaces de esa misma fila, y MUST NOT depender de la coincidencia de nombres para establecer claves foráneas.
- **FR-026**: Cada fila confirmada MUST conservar un único contexto de competición, temporada y equipo, de modo que nunca se sumen métricas de competiciones distintas del mismo jugador. El contexto vigente es el de la liga local del jugador, identificado con los IDs publicados por el proveedor y el mapeo de configuración; sin coincidencia la fila se confirma con contexto nulo y todas sus métricas ausentes.
- **FR-027**: Las métricas que el proveedor entrega acumuladas MUST derivarse por partido mediante la división por el total de apariciones de esa misma fila de contexto. El sistema MUST obtener ese denominador antes de derivar y MUST dejar la métrica sin derivar cuando el denominador no exista o sea cero, persistiéndola como ausente sin distinguir esa causa de una ausencia del proveedor. El valor derivado MUST persistirse con seis decimales y redondeo decimal exacto (half-up) sobre el valor publicado por el proveedor. Solo MUST persistirse la métrica por partido derivada; el acumulado recibido se utiliza durante el cálculo y se descarta, sin crear otra clave ni ampliar el catálogo de 60 métricas.
- **FR-063**: El sistema MUST persistir el resultado de cada canal en su propia fila de campaña, con `extraction_date` y `source` parte de la clave, de modo que la actualización de un canal MUST NOT sobrescribir ni degradar la fila del otro. Una fila sin contexto de liga local resuelto admite reconfirmación posterior. En scraping también MUST admitirla una fila con contexto resuelto pero sin ningún valor numérico presente y sin confirmación `COMPLETED` de esa campaña, para reintentar el no-data de FR-013. Una campaña ya `COMPLETED` MUST rechazarse como repetida, sin sobrescribirla ni degradarla. JSON conserva la reconfirmación limitada a filas sin contexto resuelto.
- **FR-064**: `foulsPerGame` y `foulCommitted` MUST normalizarse como alias técnicos de la única clave canónica `fouls_committed_per_match`, respetando si el origen ya es por partido (detalle nativo de scraping) o es un acumulado real que requiere FR-027. El sistema MUST NOT crear una métrica, definición ni registro de equivalencia adicional por esos nombres externos. Si ambos aparecen en la misma fila, se confirman como una sola métrica conforme a FR-063.
- **FR-065**: El catálogo de métricas MUST permanecer cerrado durante la vigencia de la feature. Una métrica nueva, un nombre de métrica cambiado o una columna desconocida MUST registrarse como diagnóstico o incidencia del proveedor conforme a FR-040 y MUST NOT crear entradas en el catálogo, ampliar el conjunto de 60 métricas ni alterar la cobertura por canal declarada en FR-008.
- **FR-066**: El estado consultable de cobertura MUST distinguir la ausencia sistémica de una métrica —no publicada por el proveedor para ningún jugador elegible en la fecha de extracción— de la ausencia individual observada en un jugador concreto. La ausencia sistémica MUST reportarse como tal, MUST NOT computarse como cero, MUST NOT invalidar la extracción de los jugadores cuyos valores sí están publicados y MUST NOT modificar el catálogo conforme a FR-065.
- **FR-028**: Las métricas que el proveedor ya entrega por partido MUST persistirse tal cual, sin volver a derivarlas ni aplicarles transformación adicional. En scraping esto incluye las 29 métricas de detalle leídas con modo publicado `0`: MUST NOT cambiar a Total ni dividirlas nuevamente por apps. MUST confirmar carga/render de la categoría, con timeout por operación preservado, y validar modo `0` antes y después de la selección; un modo distinto o desconocido no se interpreta silenciosamente como por partido. Se conserva el contexto de competición/temporada, sin usar la fila agregada Total/Promedio. Los acumulados reales del resumen siguen FR-027; este cambio no modifica el procesamiento JSON ni recalcula filas ya persistidas.
- **FR-029**: El sistema MUST persistir el rating del proveedor con la precisión que publica la fuente, sin redondeo, por ser la entrada central de las estrategias de valuación.
- **FR-030**: El sistema MUST extraer, cuando el proveedor los incluya en el contenido de la página, los cinco indicadores que las tablas no muestran: titularidades, apariciones como suplente, tiros al arco, tarjetas de segunda amarilla y el rating del proveedor. Estos indicadores MUST ser opcionales: si no están presentes, las métricas correspondientes quedan ausentes y la extracción sigue siendo válida.
- **FR-031**: El sistema MUST NOT inventar, inferir ni completar métricas ausentes. Una métrica no publicada se persiste como ausente.

#### Persistencia e histórico

- **FR-032**: Cada ejecución MUST persistir las métricas en la fila de campaña del jugador, con una fila por `(extraction_date, source, player)`. Una nueva fecha de campaña MUST NOT sobrescribir las filas de campañas anteriores, de modo que la evolución temporal de cada jugador por campaña sea consultable. Dentro de una misma campaña, la confirmación de un jugador MUST actualizar su fila en lugar de acumular duplicados.
- **FR-033**: Cada fila persistida MUST asociar el jugador, su contexto de competición y temporada, la clave canónica de cada métrica de la fila y su valor, junto con la marca temporal de la confirmación y la procedencia de la extracción, que es el canal de la propia fila.
- **FR-034**: La persistencia MUST permitir coexistencia de métricas de diferente ámbito en el mismo jugador y campaña, sin colisión entre claves de métrica.
- **FR-035**: El sistema MUST NOT mantener una transacción de persistencia abierta durante el acceso al proveedor. Las métricas MUST confirmarse por jugador o por lote acotado, de forma que un fallo a mitad del recorrido no revierta el trabajo ya válido.
- **FR-036**: Cada ejecución MUST ser reentrante respecto al histórico: relanzarla MUST NOT duplicar métricas de una ejecución ya confirmada ni corromper las existentes.
- **FR-037**: El sistema MUST registrar cada ejecución con su inicio, fin, resultado y conteos por estado, incluyendo cuántos jugadores fueron procesados, omitidos, fallidos y cuántos quedaron sin datos.

#### Tolerancia a fallos

- **FR-038**: Un rechazo de acceso por parte del proveedor MUST registrarse como fallo del jugador afectado y MUST NOT invalidar las métricas ya confirmadas de los demás jugadores.
- **FR-039**: Un fallo de transporte, un tiempo de espera agotado o una respuesta no interpretable atribuibles al procesamiento de un jugador MUST registrarse como fallo de ese jugador y MUST NOT abortar la ejecución completa. Solo si el fallo impide iniciar o continuar operaciones para los jugadores restantes, o mantener/cerrar de forma segura la auditoría del run, MUST tratarse como fallo global conforme a FR-011; esta clasificación MUST NOT habilitar reintentos automáticos del jugador ni de la petición.
- **FR-040**: Un cambio en la estructura de la página que impida localizar una sección MUST registrarse como incidencia de estructura del proveedor, distinguible de la ausencia de datos de un jugador, y MUST NOT persistirse como extracción válida.
- **FR-041**: El sistema MUST NOT persistir como válidas las métricas de una ficha cuya extracción quedó incompleta por un fallo, y MUST registrar el fallo del jugador con su motivo.
- **FR-042**: La indisponibilidad del proveedor MUST gestionarse sin insistir de forma agresiva. Cada fallo individual se registra para el jugador afectado y el recorrido continúa si se pueden iniciar operaciones para los restantes respetando el ritmo configurado. El sistema MUST respetar `Retry-After` sin reintentar al jugador afectado; si el aplazamiento impide continuar con los restantes, MUST cerrar como fallo global conforme a FR-011, registrando el estado final y los pendientes, sin reiniciar automáticamente la petición.

#### Actualización mediante la interfaz JSON pública del proveedor

La interfaz JSON expone 57 métricas: las 15 métricas que el scraping no atribuye a su canal y 42
compartidas con él. `shots_on_target_total`, `second_yellow_total` y `starts_total` no fueron
observadas en el JSON comprobado. La interfaz JSON actualiza su cobertura de manera independiente y
MUST NOT requerir la ejecución del scraping en la misma operación.

- **FR-043**: `POST /api/players/statistics/whoscored/sync/json` MUST consultar la interfaz JSON pública para sus 57 métricas, sin query parameters ni body y con autenticación vigente. MUST fijar la fecha UTC al iniciar y recorrer en una ejecución síncrona todos los elegibles pendientes de esa campaña, usando las referencias confirmadas del catálogo. No hay `maxPlayers`, fecha solicitada ni corte de 30 minutos. MUST omitir los ya `COMPLETED` ese día para JSON, no consultar el proveedor si no quedan pendientes y conservar la fecha inicial aunque cruce medianoche. Cada pendiente se intenta una vez por run, además del retry técnico permitido; un fallo individual no impide recorrer los demás y un fallo global/interrupción deja pendientes explícitos sin repetirlos indefinidamente.
- **FR-044**: *Retirado en la alineación del 2026-10-18.* El sistema MUST NOT contrastar el valor de la interfaz pública contra el del scraping: cada canal persiste su fila de campaña con la procedencia real de sus valores.
- **FR-045**: La interfaz JSON MUST poder persistir actualizaciones de las 57 métricas que cubre, incluidas las 42 compartidas con scraping. Esta capacidad MUST NOT eliminar, reducir ni sustituir el recorrido de scraping, en los mismos términos fijados por FR-001.
- **FR-046**: Una métrica no disponible por indisponibilidad de la interfaz pública MUST registrarse como tal y MUST NOT invalidar ni alterar las métricas ya extraídas por scraping.
- **FR-047**: *Retirado en la alineación del 2026-10-18.* El sistema MUST NOT mantener discrepancias entre canales porque no existe registro de contraste entre ellos.
- **FR-048**: *Retirado en la alineación del 2026-10-18.* Al no existir contraste entre canales, no se define ni se aplica tolerancia de equivalencia entre el valor del contenido visible y el de la interfaz pública.

#### Ejecución, seguridad y contrato HTTP

- **FR-049**: El sistema MUST exponer dos operaciones independientes: `POST /api/players/statistics/whoscored/sync/scraping` y `POST /api/players/statistics/whoscored/sync/json`. La primera ejecuta scraping y la segunda consulta el endpoint JSON de WhoScored sin navegador. La ruta determina el canal, sin `source` en el body. Ambas MUST mantener las condiciones de autenticación y autorización de las sincronizaciones de catálogo, sin rol nuevo ni propagación de credenciales propias al proveedor. El cliente operativo está destinado al desarrollador; esto no crea una distinción de roles en la API.
- **FR-050**: Ambas ejecuciones MUST ser síncronas y devolver directamente su resumen final tras recorrer todos sus pendientes o cerrarse por fallo global/interrupción, sin identificador asíncrono, polling de progreso ni corte temporal total. La respuesta MUST distinguir finalización completa, parcial y fallida; devolver `200` MUST NOT implicar cobertura completa si quedan pendientes.
- **FR-051**: El resumen de la respuesta MUST declarar la unidad y el ámbito de cada conteo, de modo que el operador pueda distinguir métricas, jugadores, competición y llamadas al proveedor sin ambigüedad. Las métricas ausentes MUST aparecer en el resumen únicamente como conteo agregado por ejecución, y MUST NOT listarse de forma individual por clave de métrica.
- **FR-052**: Las credenciales de acceso al proveedor MUST residir en configuración y MUST NOT aparecer en logs, en la respuesta de la API ni en los registros de auditoría.
- **FR-053**: Los errores HTTP controlados MUST seguir el contrato vigente de respuesta de error del backend, con código estable y mensaje en español.
- **FR-054**: El arranque del scraping MUST requerir un entorno capaz de ejecutar un navegador con interfaz visible. La ausencia de ese entorno MUST detectarse antes de iniciar el recorrido y reportarse como error controlado, sin dejar una ejecución a medias.
- **FR-055**: El scraping MUST NOT ejecutarse durante el arranque de la aplicación ni bloquear el arranque. El fallo en la inicialización del componente de scraping MUST NOT impedir el funcionamiento del resto del sistema.
- **FR-061**: La cobertura MUST ser consultable por fecha y canal durante y entre ejecuciones. Para scraping otro POST de la misma fecha reintenta únicamente sus pendientes; para JSON una nueva llamada sin entrada reanuda los pendientes del día UTC actual. Ningún canal repite sus confirmaciones satisfactorias del mismo día. Si JSON se relanza otro día se inicia la nueva campaña; los pendientes históricos siguen auditados, sin parámetro para ejecutar una campaña JSON pasada.

#### Límites de alcance

- **FR-056**: Esta feature MUST NOT calcular ni persistir cotizaciones, valores de tokens, scores de estrategia ni rankings. Esas capacidades pertenecen a la feature de valuación correspondiente.
- **FR-057**: Esta feature MUST NOT modificar el flujo de datos de Football-Data ni de TheSportsDB, ni las asociaciones de identidad fuera del mecanismo de referencias ya establecido.
- **FR-058**: Esta feature MUST NOT exponer las métricas de rendimiento mediante endpoints de consulta pública. La lectura de métricas para otros fines del dominio corresponde a features posteriores que consuman este dato persistido.
- **FR-059**: Esta feature MUST NOT implementar estrategias de valuación, ni modificar el modelo de cotizaciones ni el mercado de tokens.
- **FR-060**: Esta feature MUST NOT requerir ninguna modificación del scraping de identidad descrito en SPEC-008. Ambas capacidades comparten el proveedor pero son independientes en su ejecución, su ritmo y su resultado.

### Key Entities *(include if feature involves data)*

- **Contexto de participación**: combinación única de jugador, competición y temporada en la que el proveedor registra su actividad. En el modelo vigente se conserva únicamente el contexto de liga local del jugador, identificado por los IDs externos de competición, temporada y equipo.

- **Métrica extraída**: valor numérico de una medida de rendimiento, ausente o no, persistido como columna de la fila de campaña del jugador, con su clave canónica, la marca temporal de la confirmación y la procedencia de la extracción que lo produjo.

- **Definición de métrica**: catálogo de las claves canónicas reconocidas por el sistema, con su nombre técnico de origen, su unidad, sus canales y su regla de derivación cuando corresponda. Permite distinguir métricas nativas del proveedor de métricas derivadas y de métricas opcionales.

- **Ejecución de extracción**: registro de una pasada de recorrido con su inicio, fin, resultado y conteos por estado, que agrupa los items confirmados y los fallos observados.

- **Incidencia de extracción**: registro de un fallo atribuible al jugador, al proveedor o a un cambio de estructura, con su motivo estable y su alcance, que permite reconstruir por qué un jugador no quedó completo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El recorrido apunta a todos los elegibles pendientes sin muestreo ni población parcial no declarada. La cobertura es consultable y declara pendientes mientras no haya confirmación satisfactoria, incluso después de intentar todo el snapshot: los fallos y no-data no garantizan cobertura completa por repetir peticiones.
- **SC-002**: La totalidad de las sesenta métricas definidas queda cubierta por el catálogo, con cobertura declarada por canal: 45 mediante scraping, 57 mediante interfaz JSON y 60 por la unión de ambas vías. Los alias técnicos no crean métricas adicionales.
- **SC-003**: Un jugador sin registros en una sección concreta no invalida la extracción de sus demás secciones: la confirmación depende de que terminen correctamente todas las secciones obligatorias de su canal y no de un porcentaje de métricas ausentes. En scraping, si no se obtiene ninguna métrica, se informa no-data y permanece pendiente conforme a FR-013.
- **SC-004**: Ninguna confirmación sobrescribe las filas de campañas anteriores; cada fecha de campaña conserva su propia fila por jugador y canal, de modo que la evolución temporal de un jugador es consultable.
- **SC-005**: Un fallo del proveedor sobre un jugador no impide completar la ejecución para el resto, y los conteos por estado de cada ejecución —incluidos cuántos jugadores fallaron— quedan registrados y son consultables por el operador mediante el resumen de la ejecución y la consulta de cobertura del canal. El porcentaje de fallidos es un valor derivado de esos conteos y MUST NOT persistirse ni exponerse como campo propio en la API.
- **SC-006**: La actualización desde la interfaz JSON cubre 57 métricas, incluidas las quince que el scraping no atribuye a su canal, y persiste cada canal en su propia fila de campaña sin impedir que cualquiera de ambos produzca una actualización independiente.
- **SC-007**: La ausencia de un valor del proveedor se persiste de forma distinguible de un cero, de modo que ninguna métrica ausente pueda computarse como cero en el cálculo de valuación.
- **SC-008**: El scraping se ejecuta con navegador visible en todas las ejecuciones, sin recurrir al modo headless rechazado por el proveedor.
- **SC-009**: Ninguna credencial del proveedor aparece en las respuestas de la API, en los logs ni en la auditoría.
- **SC-010**: La presión sobre el proveedor respeta el ritmo configurado, sin reintentos incontrolados, y las incidencias de disponibilidad quedan registradas.
- **SC-011**: Un POST de scraping intenta todos los pendientes de su fecha una sola vez salvo fallo global/interrupción, sin máximo de jugadores ni presupuesto temporal total. Fallidos y no-data se reportan y siguen pendientes para otro POST; los completados se omiten. Ambos canales declaran sus conteos finales.
- **SC-012**: Objetivo histórico informativo, no criterio de aceptación ni deadline: unas 10 horas para aproximadamente 2100 jugadores con tres workers y 2500 ms de intervalo. La medición humana debe declarar población inicial, configuración efectiva, inicio, fin, cobertura y duración; sus limitaciones se informan sin habilitar ni bloquear gates por la duración. La configuración documentada actual es cuatro workers y 1500 ms. Los fallos o no-data pueden dejar cobertura incompleta tras recorrer todos los pendientes. Este objetivo no se aplica a JSON.
- **SC-013**: El canal JSON cubre las 57 métricas declaradas para su contexto de liga local y persiste cada valor con la procedencia de su canal, sin dejar métricas sin clasificar entre presente y ausente. El ritmo de consultas al proveedor respeta el intervalo configurado y cada ejecución informa cuántos jugadores procesó y cuántos quedaron pendientes, sin declarar completada la cobertura mientras queden pendientes para la fecha y el canal en curso.
## Out of Scope

- Cálculo de cotizaciones, scores de estrategia, rankings y conversión a valor.
- Mercado de tokens, órdenes de compra y venta, y portfolio de usuario.
- Modificación del scraping de identidad de SPEC-008 o de sus referencias externas.
- Ingesta de datos de Football-Data o de TheSportsDB.
- Exposición pública de las métricas de rendimiento mediante endpoints de consulta.
- Extracción de apartados no incluidos en el conjunto de métricas: goles esperados en su forma detallada, mapas de calor, comparativas entre jugadores y datos de nacionalidad.
- Recorrido por scraping de las secciones de resumen ofensiva y de distribución, cuyas métricas se obtienen por la vía complementaria definida en FR-043.
- Sustitución de los valores de muestra del frontend, que pertenecen a SPEC-009.
- Medición del paralelismo óptimo contra el proveedor, que se fija al definir el plan.

## Assumptions

- El catálogo local ya dispone de identificadores externos WhoScored confirmados para la población de jugadores a procesar, resultado del trabajo de SPEC-008. Si esa población fuera vacía o parcial, la ejecución no tendría recorrido.
- Los identificadores externos WhoScored son estables entre ejecuciones para un mismo jugador y una misma competición.
- El proveedor mantiene la estructura de la ficha de jugador y sus atributos de columna durante la vigencia de la feature; un cambio de estructura se gestiona como incidencia, no como dato faltante.
- El entorno de ejecución dispone de un navegador con interfaz visible, sea en el equipo de desarrollo, sea en el entorno de ejecución del backend. Esta feature asume headed como única vía soportada.
- El ritmo de extracción se configura por propiedad y se ajusta según la disponibilidad observada del proveedor; el valor inicial y sus properties efectivas están fijados en [plan.md](plan.md) (`1500 ms`, cuatro workers por canal), no en esta spec.
- Las métricas se extraen con la precisión que el proveedor muestra en el contenido visible, sin reescalar el decimal publicado al persistirlo.
- El scraping conserva íntegramente su recorrido por exigencia funcional y académica. La interfaz JSON ofrece una segunda vía de actualización para 57 métricas, sin reemplazar ni reducir el scraping.
- El histórico de métricas por jugador se conserva por campañas de fecha y canal, sin política de purga en el alcance de esta feature; su retención se define en una feature posterior si el volumen lo exige.
- El enunciado `tp/enunciado.md` se toma como requisito externo no modificable: el scraping de WhoScored y la coexistencia con la API de Football-Data son parte del encargo.

## Verificaciones pendientes

- Plan, contratos, modelo, guía operativa y Tasks alineados documentalmente con la sesión del 2026-10-27; checklist nuevo evaluado de forma asistida sin marcar por el revisor. Pendientes: Analyze y aceptación del revisor, luego autorización de implementación. Los límites de lote y retries anteriores no aplican al nuevo alcance objetivo; el código aún no se adapta. JSON no cambia.
- Recoger evidencia humana de la medición temporal descrita en SC-012 como objetivo informativo; esta corrección documental no ejecuta el proveedor, tests ni benchmarks y no declara esa estimación cumplida ni incumplida.

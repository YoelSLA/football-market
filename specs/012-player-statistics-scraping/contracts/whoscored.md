# Contrato externo: WhoScored Statistics

## Límites comunes

- Base URL HTTPS configurable y sin credenciales embebidas.
- Player ID entero positivo obtenido de una referencia `WHOSCORED` confirmada.
- Competition ID y Team ID obtenidos de la misma fila externa.
- JWT y headers de autenticación de Football Market nunca se envían al proveedor.
- Contratos externos separados de DTO HTTP y Model.
- El catálogo de `spec.md` es cerrado: columnas desconocidas se diagnostican y no crean métricas.

## Canal de scraping

- Chromium administrado por Playwright con `headless=false`.
- User-Agent real del navegador, sin sobrescritura incompatible.
- Una instancia de Playwright y un `Browser` por worker, confinados al hilo que los crea y liberados en ese mismo hilo.
- Un `BrowserContext` independiente por sección dentro del navegador de cada worker.
- Recorrido obligatorio sin cambios:
  - carga inicial, resumen y bloque estructurado;
  - resumen defensivo;
  - dieciséis subcategorías de detalle.
- No recorre resumen ofensivo, resumen de distribución, xG detallado, mapas de calor, comparativas ni nacionalidad.
- Cobertura: 45 métricas.
- Detalle: modo nativo `statsAccumulationType=0` validado antes/después de cambiar
  categoría. No se selecciona Total ni se espera una recarga de acumulados que el
  proveedor no realiza. Se conserva el render/timeout de categoría, incluso HTML
  idéntico. La fila con competición/temporada sigue siendo obligatoria, no Total/Promedio.
- Las 29 métricas de detalle se leen por partido sin dividir por apps ni exigir su
  denominador; ausencias/ceros/precisión se conservan. Resumen: derivación de acumulados
  reales sin cambios. JSON y filas existentes no se modifican en este alcance.
- `foulsPerGame` normaliza a `fouls_committed_per_match`.
- `-`, `N/A`, vacío o blanco significa `ABSENT` después de recortar espacios, nunca cero. No se amplía la aceptación a otros textos no numéricos.
- Al cambiar un filtro se observa el renderizado de la tabla después de la selección,
  incluidos reemplazos por contenido idéntico. Se exige tabla con encabezados técnicos y
  cuerpo, indicador de carga oculto si existe y control con el valor solicitado;
  cambiar solo el selector o un recurso auxiliar
  no acredita actualización. Sin renderizado observable se conserva el timeout, sin retry.

## Canal JSON

Endpoint:

```text
GET /statisticsfeed/1/getplayerstatistics
```

Parámetros comunes observados:

- `statsAccumulationType=0`
- `isCurrent=true`
- `playerId=<id confirmado>`
- `includeZeroValues=true`
- `field=Overall` cuando aplica
- `sortBy=Rating`
- `page=1`

El cuerpo debe validarse como JSON aunque `Content-Type` sea `text/html`. Debe contener `playerTableStats` y `statColumns` coherentes.

`statColumns` se publica como **array de cadenas**; esa es la forma observada y la que se valida. También se admite la forma de objeto con `name`, `key` o `title` para no confundir un cambio de presentación con un cambio de estructura.

`rating` se publica en la fila del jugador pero **no** se declara en `statColumns`. Por eso se lee de forma explícita como columna de métrica; el resto de claves de la fila son identidad o contexto y no pueden entrar en el catálogo cerrado.

### Combinaciones conocidas

| Categoría | Subcategoría |
| --- | --- |
| `summary` | `all` |
| `summary` | `defensive` |
| `summary` | `offensive` |
| `summary` | `passing` |
| `aerial` | `success` |
| `assists` | `type` |
| `blocks` | `type` |
| `dribbles` | `success` |
| `fouls` | `type` |
| `goals` | `zones` |
| `key-passes` | `length` |
| `passes` | `length` |
| `saves` | `shotzone` |
| `shots` | `zones` |
| `tackles` | `success` |

La unión validada cubre 57 métricas. No se observaron en JSON `ShotsOnTarget`, `SecondYellow` ni `GameStarted`; no se consultan combinaciones especulativas para intentar obtenerlas.

### Combinaciones retiradas por redundancia

Se observaron además cinco combinaciones que se retiraron del recorrido:

| Categoría | Subcategoría | Columnas que declara |
| --- | --- | --- |
| `cards` | `type` | `apps, minsPlayed, redCard, subOn, yellowCard` |
| `clearances` | `success` | `apps, minsPlayed, clearanceTotal, subOn` |
| `interception` | `success` | `apps, minsPlayed, interceptionAll, subOn` |
| `offsides` | `type` | `apps, minsPlayed, offsideGiven, subOn` |
| `possession-loss` | `type` | `apps, minsPlayed, dispossessed, subOn, turnover` |

Todas declaran solo columnas que `summary/all` ya publica, más columnas ajenas al catálogo cerrado (`clearanceTotal`, `interceptionAll`, `offsideGiven`, `dispossessed`, `turnover`). Quitarlas no reduce la unión de métricas obtenible y evita una cuarta parte de las peticiones por jugador. La comprobación está en `evidence/json-combinations-coverage.md`.

## Normalización

- Punto y coma decimal se convierten a `BigDecimal`.
- Se conserva precisión observada: los tokens decimales del HTML estructurado y del JSON
  se leen como `BigDecimal` desde su representación original, sin conversión intermedia
  a `double`/`float`. El rating `6.9033333333333333` conserva sus 16 decimales sin redondeo.
  No se rechaza un decimal válido por superar seis decimales ni se le aplica una escala fija.
- Campos acumulados marcados `/apps` se dividen por `apps` del mismo contexto; el acumulado se descarta.
- Solo ese cociente se calcula con seis decimales y `HALF_UP` (FR-027); los valores
  publicados directamente, incluido el rating, se conservan tal cual (FR-028/FR-029).
- `foulsPerGame` y `foulCommitted` producen `fouls_committed_per_match`; detalle nativo
  scraping se conserva directamente, sin dividir otra vez por apps.
- `subOn` y `SubOn` se normalizan según la definición canónica.
- Una columna esperada ausente produce `ABSENT`; una sección o forma estructural inesperada produce `PROVIDER_STRUCTURE_CHANGED`.

## Ritmo y reintentos

- Máximo inicial de cuatro workers por canal.
- Intervalo mínimo inicial de 1500 ms por worker (`whoscored.statistics.request-interval-ms`).
- En scraping se aplica entre inicios de navegación de secciones sin retries, no por recurso. En JSON entre consultas/retries del worker. Scraping conserva contextos independientes de carga inicial y detalle, con dieciséis categorías en detalle. Cada worker tiene Playwright y Browser propios, sin monitor compartido.
- Timeouts explícitos por tipo de operación.
- Scraping intenta cada jugador una vez por POST: sin retry de navegación, sección, tabla o categoría ante fallo. Esperar el estado DOM de una operación ya iniciada no es volver a ejecutarla. Si falla una sección obligatoria no se confirma extracción parcial.
- Sin reintento ante 403, 404, estructura inesperada, ID inválido o dato incoherente.
- `Retry-After` aplaza requests posteriores; no hay deadline total de scraping ni retry del jugador. Rate limit que impide continuar puede cerrar controladamente con pendientes explícitos. Timeout, red o rechazo individual del scraping no detienen por sí solos el resto de jugadores. JSON conserva su política de parada global existente.
- JSON abandona la campaña completa solo ante indisponibilidad global: `PROVIDER_BLOCKED` (403) y el límite de peticiones (HTTP 429). Un timeout, un corte de red o un error 5xx puntual son de un jugador: se registran como fallo suyo y el recorrido continúa, porque la siguiente invocación los vuelve a seleccionar como pendientes.
- Ambos canales recorren todos sus pendientes sin corte temporal total ni máximo solicitado. Mantienen timeouts por operación y no confirman datos parciales por fallo. No convertir esperas de tabla de 45 s observadas en un SLA ni suponer su causa; investigar fases antes de cambiarlas.
- JSON reintenta una consulta hasta tres veces cuando el fallo es transitorio: un 5xx del proveedor o un timeout o corte de red. Un 4xx (`PROVIDER_BLOCKED`, `PLAYER_NOT_FOUND`), un cambio de estructura y un límite de peticiones no se reintentan. El intervalo del worker actúa como espera entre intentos y el presupuesto de la consulta acota el tiempo total.

## Validación de jugador

Un jugador se confirma cuando:

- todas las secciones obligatorias del canal finalizaron;
- cada columna conocida es interpretable o ausencia explícita;
- derivadas y aliases respetan su contexto;
- cada clave pertenece a la cobertura declarada del canal.

En scraping además debe existir al menos un valor numérico presente en el contexto seleccionado: de lo contrario es `NO_DATA`, sigue pendiente y puede reconfirmarse en otro POST sin colisionar con una fila sin éxito previo. No se exige un porcentaje mínimo ni se transforma ausencia en cero. JSON no cambia.

Al cerrar workers, liberar contextos, Browser y Playwright en orden y en su hilo, sin cierre concurrente desde el coordinador; un error de limpieza no oculta el fallo principal ni descarta confirmaciones válidas. La excepción observada de transporte al cerrar no tiene aún causa demostrada.

El canal no contrasta sus valores contra los del otro canal: cada fila conserva únicamente el
contexto de liga local del jugador y la procedencia real del canal que la produjo.

El bloque estructurado del scraping es opcional para sus métricas; su ausencia no invalida las demás. Un fallo estructural de una sección obligatoria impide confirmar datos parciales del jugador.

## Fallos estables

| Código | Significado |
| --- | --- |
| `PROVIDER_BLOCKED` | HTTP 403 o página de bloqueo. |
| `PLAYER_NOT_FOUND` | La ficha dejó de existir. |
| `PROVIDER_TIMEOUT` | Timeout de operación; sin retry en scraping, tras la política permitida en JSON. |
| `PROVIDER_TRANSPORT_ERROR` | Fallo de red; sin retry en scraping, tras la política permitida en JSON. |
| `PROVIDER_STRUCTURE_CHANGED` | Sección, JSON, columna o relación obligatoria ausente. |
| `PROVIDER_COLUMN_NOT_CATALOGED` | El proveedor publicó una columna fuera del catálogo cerrado. Se diagnostica sin crear métricas y **no** invalida al jugador: no es un cambio de estructura. |
| `PROVIDER_DATA_INCONSISTENT` | Fila o valor contradice invariantes. |
| `HEADED_BROWSER_UNAVAILABLE` | El navegador del worker perdió su conexión y no permite continuar el recorrido. |
| `EXTRACTION_INTERRUPTED` | Interrupción o cierre anticipado. |

No se persisten HTML, JSON completo, cookies, cabeceras sensibles ni cuerpos externos.
# Apertura inicial de pestañas — corrección autorizada 2026-10-29

Scraping espera resumen seleccionado, visible y renderizado (encabezados/cuerpo,
carga oculta), con panel de detalle existente y oculto, antes del único clic.
Confirma enlace seleccionado y visibilidad CSS real del panel; no basta display
inline. Preparación conserva timeout de operación y activación margen de 8 s.
Sin sleeps, retries, cambios de unidades, contexto ni persistencia. Verificación
humana local/live pendiente; no acredita resolución de todos los fallos poblacionales.

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
- Un `BrowserContext` independiente por sección.
- Recorrido obligatorio sin cambios:
  - carga inicial, resumen y bloque estructurado;
  - resumen defensivo;
  - dieciséis subcategorías de detalle.
- No recorre resumen ofensivo, resumen de distribución, xG detallado, mapas de calor, comparativas ni nacionalidad.
- Cobertura: 45 métricas.
- `foulsPerGame` normaliza a `fouls_committed_per_match`.
- `-`, vacío o blanco significa `ABSENT`, nunca cero.

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
- `foulsPerGame` y `foulCommitted / apps` producen `fouls_committed_per_match`.
- `subOn` y `SubOn` se normalizan según la definición canónica.
- Una columna esperada ausente produce `ABSENT`; una sección o forma estructural inesperada produce `PROVIDER_STRUCTURE_CHANGED`.

## Ritmo y reintentos

- Máximo inicial de cuatro workers por canal.
- Intervalo mínimo inicial de 1500 ms por worker (`whoscored.statistics.request-interval-ms`).
- En scraping se aplica entre inicios de navegación de secciones, incluidos reintentos, no por cada recurso cargado por el navegador. En JSON se aplica entre inicios de consultas de estadísticas del worker. El scraping abre un `BrowserContext` independiente para la carga inicial y otro para la sección de detalle, y recorre las dieciséis subcategorías dentro del contexto de detalle.
- Timeouts explícitos por tipo de operación.
- Un reintento máximo ante timeout o transporte transitorio en scraping.
- Sin reintento ante 403, 404, estructura inesperada, ID inválido o dato incoherente.
- `Retry-After`, si existe, aplaza requests posteriores. Scraping lo respeta dentro del deadline de lote; JSON respeta el aplazamiento o cierra controladamente ante indisponibilidad global, sin insistir indefinidamente.
- JSON abandona la campaña completa solo ante indisponibilidad global: `PROVIDER_BLOCKED` (403) y el límite de peticiones (HTTP 429). Un timeout, un corte de red o un error 5xx puntual son de un jugador: se registran como fallo suyo y el recorrido continúa, porque la siguiente invocación los vuelve a seleccionar como pendientes.
- Solo scraping limita además esperas y navegación por el plazo restante del lote; al vencer no confirma datos parciales. JSON mantiene timeouts por consulta y retry limitado, pero recorre todos los pendientes del día UTC sin corte de 30 minutos ni máximo de jugadores solicitado.
- JSON reintenta una consulta hasta tres veces cuando el fallo es transitorio: un 5xx del proveedor o un timeout o corte de red. Un 4xx (`PROVIDER_BLOCKED`, `PLAYER_NOT_FOUND`), un cambio de estructura y un límite de peticiones no se reintentan. El intervalo del worker actúa como espera entre intentos y el presupuesto de la consulta acota el tiempo total.

## Validación de jugador

Un jugador se confirma cuando:

- todas las secciones obligatorias del canal finalizaron;
- cada columna conocida es interpretable o ausencia explícita;
- derivadas y aliases respetan su contexto;
- cada clave pertenece a la cobertura declarada del canal.

El canal no contrasta sus valores contra los del otro canal: cada fila conserva únicamente el
contexto de liga local del jugador y la procedencia real del canal que la produjo.

El bloque estructurado del scraping es opcional para sus métricas; su ausencia no invalida las demás. Un fallo estructural de una sección obligatoria impide confirmar datos parciales del jugador.

## Fallos estables

| Código | Significado |
| --- | --- |
| `PROVIDER_BLOCKED` | HTTP 403 o página de bloqueo. |
| `PLAYER_NOT_FOUND` | La ficha dejó de existir. |
| `PROVIDER_TIMEOUT` | Timeout tras el reintento permitido. |
| `PROVIDER_TRANSPORT_ERROR` | Fallo de red tras el reintento. |
| `PROVIDER_STRUCTURE_CHANGED` | Sección, JSON, columna o relación obligatoria ausente. |
| `PROVIDER_COLUMN_NOT_CATALOGED` | El proveedor publicó una columna fuera del catálogo cerrado. Se diagnostica sin crear métricas y **no** invalida al jugador: no es un cambio de estructura. |
| `PROVIDER_DATA_INCONSISTENT` | Fila o valor contradice invariantes. |
| `EXTRACTION_INTERRUPTED` | Interrupción o cierre anticipado. |

No se persisten HTML, JSON completo, cookies, cabeceras sensibles ni cuerpos externos.

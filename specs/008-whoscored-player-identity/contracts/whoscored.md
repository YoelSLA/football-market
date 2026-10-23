# Contrato de integración — Catálogo WhoScored

Fuente: [spec](../spec.md), [observación fechada](../evidence/whoscored-catalog-contract.md) y cuatro JSON en `evidence/`. Contrato interno observado, no API oficial ni garantía de disponibilidad. Este documento diseña la validación del adaptador, no acredita requests nuevos.

## Solicitud

`GET https://es.whoscored.com/statisticsfeed/1/getplayerstatistics`

| Query | Valor |
| --- | --- |
| `category`, `subcategory` | `summary`, `all` |
| `statsAccumulationType`, `includeZeroValues`, `field`, `sortBy` | `0`, `true`, `Overall`, `Rating` |
| `playerId` | Cadena vacía, enviada explícitamente. |
| `isCurrent` | `true` CURRENT; `false` HISTORICAL. |
| `tournamentOptions` | Competición de la tabla inferior. |
| `numberOfPlayersToPick` | Property `page-size`, default 100, rango 1–100. |
| `page` | Entero desde 1 hasta totalPages validado. |

No usar `tournamentId`/`stageId` como filtros: la evidencia funcional indica que se ignoran. Construir URI mediante binding seguro, no concatenando entrada del operador.

| Liga | tournamentOptions |
| --- | --- |
| Premier League | 2 |
| Bundesliga | 3 |
| LaLiga | 4 |
| Serie A | 5 |
| Ligue 1 | 22 |

Properties `whoscored.competitions`: cinco entradas con `league-id` interno y `tournament-id` de esta tabla; IDs internos distintos, existentes y sin binding contradictorio. No crear ligas/equipos ni usar IDs Football-Data para decidir la identidad.

Header `User-Agent`: valor observado en la evidencia, configurable como `whoscored.user-agent`; no vacío ni CR/LF. Enviar con `defaultHeader` del RestClient dedicado. No reenviar Authorization JWT del usuario. No requiere credenciales, cookies, browser, Referer o X-Requested-With según las muestras. HTTPS obligatorio y sin redirecciones automáticas.

## Respuesta y adaptación

Exigir HTTP 2xx, cuerpo no vacío acotado a `max-response-bytes` y JSON objeto parseable. El MIME no decide validez: HTTP 200 con `text/html; charset=utf-8` puede contener JSON válido. Obtener bytes limitados y parsear explícitamente con Jackson existente, dentro de Integration; no ampliar converters globales ni parsear HTML como fallback.

Root obligatorio: `playerTableStats` array y `paging` objeto. `statColumns`, métricas y demás campos desconocidos se ignoran. El contrato externo no es DTO HTTP propio ni Model. Integration entrega datos de identidad y metadatos ya validados; Service los transforma a Model de candidatos/contextos.

| Campo de fila | Regla de identidad |
| --- | --- |
| `playerId` | Número entero positivo representable como Long o cadena de dígitos ASCII decimales con valor positivo representable como Long. Canonicalizar ceros iniciales; no coaccionar decimales. Ausente/vacío/cero/negativo/no numérico/fuera de rango: descartar solo la fila y auditar `INVALID_EXTERNAL_PLAYER_ID`. |
| `name` | Texto válido según FR-027, preservando original. Ausente, tipo inválido, vacío normalizado o solo signos: descartar solo la fila con `INVALID_EXTERNAL_PLAYER_NAME`. |
| `teamName` | Texto válido según FR-027; si falta o es inválido, descartar solo la fila con `INVALID_EXTERNAL_TEAM_NAME`. Si no está en la tabla, puede coincidir directamente por normalización. |
| `teamId` | ID positivo cuando disponible; null/ausente se conserva sin impedir matching por nombre válido. Tipo/valor inválido descarta la fila como `INVALID_EXTERNAL_TEAM_ID`, sin invalidar la página. |
| `tournamentId` | Si está presente, entero positivo igual a filtro; contradicción invalida liga. |
| `tournamentName` | Texto opcional observado; no inventarlo. |
| `seasonId`, `seasonName` | Contexto opcional histórico; no exigir temporada actual ni usarlo como criterio de selección. |
| Otros campos | No condicionan matching ni se persisten/exponen como estadísticas. |

`leagueId`/`leagueName` en Model son el binding local capturado, no atributos inventados del proveedor. Un equipo válido que no coincide produce falta de coincidencia por equipo, nunca asociación por nombre solo. Descartes se auditan con ejecución, liga, página, posición y motivo, sin cuerpo crudo. No son errores de descubrimiento de liga ni jugadores locales NO_MATCH. No reconstruir nombre desde firstName/lastName ni inventar IDs.

## Paginación completa

Metadatos enteros obligatorios: `currentPage`, `totalPages`, `resultsPerPage`, `totalResults`, `firstRecordIndex`, `lastRecordIndex`. No aceptar null, strings/decimales coercionados ni valores negativos donde se espera cantidad positiva.

Para resultados no vacíos:

1. `currentPage` igual a la página solicitada; `resultsPerPage` igual al tamaño solicitado y positivo.
2. `totalPages = ceil(totalResults / resultsPerPage)`, con aritmética sin overflow. `1 <= currentPage <= totalPages`.
3. `firstRecordIndex = (currentPage - 1) * resultsPerPage + 1`; `lastRecordIndex = min(currentPage * resultsPerPage, totalResults)`; cantidad de filas originales, antes de descartes de identidad, igual a `lastRecordIndex - firstRecordIndex + 1`.
4. `totalPages`, `totalResults`, `resultsPerPage` estables entre páginas del mismo recorrido. Un cambio invalida la liga; no reiniciar ni usar datos parciales.
5. Procesar todas las páginas hasta `currentPage == totalPages`. No pedir totalPages+1 para descubrir el fin. Una página corta no basta por sí sola.

Para liga sin resultados: primera página solicitada 1, array vacío, `totalResults=0`, `totalPages` 0 o 1, `currentPage=1`, `resultsPerPage` positivo igual al solicitado y rango vacío (índice inicial 0 o 1, final 0). Es la política de aceptación explícita del caso vacío, no una muestra observada de liga vacía. Otra combinación se rechaza.

La muestra `catalog-current-empty-page.json` tiene página 159 > 158 y rango invertido: es evidencia de request fuera de límites, **no** una primera página vacía válida ni condición de cierre. No relajar validaciones de páginas dentro del rango para admitirla.

Detectar página/índices repetidos, secuencia no ascendente y repetición completa de filas sin progreso (huella de identidad/contexto, incluida temporada cuando exista). Repetir un ID con contexto histórico distinto no es un error ni otro jugador; conservarlo. `totalResults` y rowsRead cuentan filas, no `discoveredUnique`. Alcanzar el límite configurable de páginas antes del fin invalida el ámbito.

Solo después del recorrido íntegro válido de una liga se incorporan sus candidatos. Un fallo descarta sus filas para resolver y contar descubiertos, conservando el diagnóstico. Luego deduplicar globalmente por playerId entre todas las ligas válidas.

## Ritmo, límites y fallos

Properties dedicadas: `request-interval-ms=2500`, `connect-timeout-ms=5000`, `read-timeout-ms=20000`, `max-pages-per-league=10000`, `max-response-bytes=5242880`. Todos positivos y configurables; defaults propios, sin afirmar límites oficiales. Las cinco ligas se recorren secuencialmente; pacer compartido del cliente por instancia, fuera de transacciones.

| Error interno auditable | Situación |
| --- | --- |
| `PROVIDER_BLOCKED` | 403/bloqueo identificado. |
| `PROVIDER_RATE_LIMITED` | 429; respetar Retry-After para posteriores requests, sin repetir la fallida. |
| `PROVIDER_HTTP_ERROR` | Otro status no 2xx, incluidas redirecciones. |
| `PROVIDER_UNAVAILABLE` | Timeout, DNS/conexión o error técnico del cliente. |
| `PROVIDER_INVALID_RESPONSE` | JSON/listado/metadatos inválidos o inconsistentes, o contradicción estructural de competición. No incluye descartes individuales de identidad definidos arriba. |
| `PROVIDER_NO_PROGRESS` | Repetición/ausencia de progreso. |
| `PROVIDER_LIMIT_EXCEEDED` | Límite de páginas/cuerpo excedido. |

Errores propios heredan IntegrationException. Guardar página/competición/status cuando estén disponibles y explicación sanitizada, nunca body crudo ni causas con URLs/headers sensibles. Sin retries automáticos. Fallos aislados por liga permiten procesar otras válidas; si la indisponibilidad o Retry-After impide continuar, registrar los ámbitos restantes incompletos sin nuevas solicitudes ni falsos NO_MATCH.

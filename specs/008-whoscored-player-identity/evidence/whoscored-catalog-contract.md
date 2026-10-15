# Evidencia del contrato observado del catálogo WhoScored

**Fecha de observación**: 2026-10-14

**Alcance**: solicitudes de solo lectura sobre LaLiga (`tournamentOptions=4`) para comprobar URL, parámetros, headers mínimos, cuerpos actual e histórico y terminación de paginación. Esta evidencia documenta un endpoint interno no oficial; no garantiza disponibilidad futura.

## Solicitud válida

```http
GET https://es.whoscored.com/statisticsfeed/1/getplayerstatistics?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true&playerId=&includeZeroValues=true&field=Overall&sortBy=Rating&tournamentOptions=4&numberOfPlayersToPick=3&page=1
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36
```

Parámetros observados:

| Parámetro | Valor de evidencia | Propósito observado |
| --- | --- | --- |
| `category` | `summary` | Categoría resumida de estadísticas |
| `subcategory` | `all` | Sin subcategoría adicional |
| `statsAccumulationType` | `0` | Modo de acumulación utilizado por la página |
| `isCurrent` | `true` / `false` | Catálogo actual / histórico |
| `playerId` | vacío | Lista paginada en lugar de un jugador específico |
| `includeZeroValues` | `true` | Conserva filas con valores cero |
| `field` | `Overall` | Ámbito general |
| `sortBy` | `Rating` | Orden observado |
| `tournamentOptions` | `4` | LaLiga; las cinco equivalencias están en la spec |
| `numberOfPlayersToPick` | `3` | Tamaño reducido para evidencia; se verificó soporte hasta 100 |
| `page` | `1` | Paginación base 1 |

Pruebas de headers realizadas secuencialmente sobre la misma consulta:

| Variante | Resultado observado |
| --- | --- |
| Solo `User-Agent` de navegador | JSON válido con `playerTableStats` |
| Sin `Referer` | JSON válido |
| Sin `X-Requested-With` | JSON válido |
| Sin `Accept` explícito | JSON válido |
| Sin `User-Agent` de navegador, usando curl por defecto | HTTP 403, HTML |

Por lo tanto, el único header indispensable comprobado es un `User-Agent` compatible con navegador. No fueron necesarios cookies ni `model-last-mode`. Esto no impide que el proveedor cambie sus controles.

La respuesta válida observada fue HTTP 200 con `Content-Type: text/html; charset=utf-8` aunque el cuerpo era JSON válido. Por eso el MIME no basta para aceptar o rechazar: debe parsearse el cuerpo y validarse `playerTableStats` junto con `paging`.

## Cuerpos conservados

- [Página 1 del catálogo actual](catalog-current-page-1.json): tres filas, `currentPage=1`, `totalPages=158`, `totalResults=472`.
- [Última página del catálogo actual](catalog-current-last-page.json): una fila, `currentPage=158`, `totalPages=158`.
- [Página posterior vacía](catalog-current-empty-page.json): cero filas, `currentPage=159`, `totalPages=158`.
- [Página 1 histórica](catalog-historical-page-1.json): tres filas, `currentPage=1`, `totalPages=4188`, `totalResults=12562`.

## Regla comprobada de terminación

El proveedor sí incluye metadatos suficientes. La página final válida se reconoce cuando:

```text
currentPage == totalPages
```

La evidencia muestra que esa página puede contener menos filas que `resultsPerPage`: página 158 contiene una fila aunque el tamaño solicitado sea 3. La página 159 devuelve un listado vacío, pero ya está fuera del límite porque `currentPage > totalPages`.

Regla normativa derivada:

1. Validar que `paging.currentPage` corresponda a la página solicitada.
2. Validar `totalPages >= 0`, `totalResults >= 0` y `resultsPerPage > 0` cuando existen resultados.
3. Continuar mientras `currentPage < totalPages`.
4. Terminar después de procesar la página donde `currentPage == totalPages`.
5. No inferir fin por `playerTableStats.size() < numberOfPlayersToPick`.
6. Si `paging` falta o es incoherente, tratar la liga como descubrimiento inválido; no usar una página corta o vacía como fallback.

Esta regla evita completar una liga a partir de una respuesta truncada, bloqueada o parcialmente válida y concreta FR-002, FR-015 y FR-017.

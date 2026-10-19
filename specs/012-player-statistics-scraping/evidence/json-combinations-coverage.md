# Cobertura de combinaciones del canal JSON

**Fecha de observación**: 2026-10-16
**Endpoint**: `GET https://es.whoscored.com/statisticsfeed/1/getplayerstatistics`
**Método**: `curl.exe` con `User-Agent` de navegador. No requiere `Referer`, cookies ni `X-Requested-With`. Fuera de un cliente con `User-Agent` de navegador la respuesta es HTTP 403 y no se puede observar la forma.

## Forma real observada del cuerpo

`statColumns` es un **array de cadenas**, no de objetos:

```json
"statColumns": ["apps","subOn","minsPlayed","goal","assistTotal","yellowCard",
                "redCard","shotsPerGame","passSuccess","aerialWonPerGame","manOfTheMatch"]
```

`playerTableStats` trae entre 3 y 5 filas por jugador (contextos), y en cada fila `seasonId` es un **entero** y `rating` está presente **aunque no se declare** en `statColumns`.

## Muestra

Cinco jugadores de perfil y liga distintos:

| Perfil | `playerId` | Liga |
| --- | --- | --- |
| Portero | 113880 | Premier League |
| Defensa | 122117 | Primera Division |
| Medio | 20874 | Serie A |
| Delantero | 354555 | Bundesliga |
| Medio | 328265 | Ligue 1 |

## Cobertura de las 57 métricas del canal JSON

Ninguna llamada aislada cubre el catálogo: `summary/all` aporta 10 de 57. La unión de las 15 combinaciones recorreplas alcanza en los cinco jugadores.

| Combinación | Aporte propio de métricas del catálogo |
| --- | --- |
| `summary/all` | imprescindible para los 5 |
| `summary/defensive` | imprescindible para los 5 |
| `summary/offensive` | imprescindible para los 5 |
| `summary/passing` | imprescindible para los 5 |
| `aerial/success` | imprescindible para los 5 |
| `assists/type` | imprescindible para los 5 |
| `blocks/type` | imprescindible para los 5 |
| `dribbles/success` | imprescindible para los 5 |
| `fouls/type` | imprescindible para los 5 |
| `goals/zones` | imprescindible para los 5 |
| `key-passes/length` | imprescindible para los 5 |
| `passes/length` | imprescindible para los 5 |
| `saves/shotzone` | imprescindible para los 5 |
| `shots/zones` | imprescindible para los 5 |
| `tackles/success` | imprescindible para los 5 |
| `cards/type` | **0** |
| `clearances/success` | **0** |
| `interception/success` | **0** |
| `offsides/type` | **0** |
| `possession-loss/type` | **0** |

"Imprescindible" significa que es la única combinación que aporta al menos una métrica del catálogo para ese jugador.

## Por qué las cinco retiradas son redundantes de forma estructural

No es una redundancia de muestra: las cinco declaran únicamente columnas que `summary/all` ya publica, más columnas que no pertenecen al catálogo cerrado de 60 métricas.

| Combinación retirada | Columnas únicas frente al catálogo cerrado |
| --- | --- |
| `cards/type` | ninguna |
| `clearances/success` | `clearanceTotal` |
| `interception/success` | `interceptionAll` |
| `offsides/type` | `offsideGiven` |
| `possession-loss/type` | `dispossessed`, `turnover` |

Ninguna de esas columnas únicas crea una métrica. Retirar las cinco no reduce la unión obtenible.

## Impacto

Para una campaña de 1374 jugadores pendientes con tres workers y pacing de 2500 ms:

| Recorrido | Peticiones por jugador | Duración estimada |
| --- | --- | --- |
| 20 combinaciones | 27.480 | ~6,3 h |
| 15 combinaciones | 20.610 | ~4,8 h |

## Límites de esta evidencia

- La muestra cubre cinco jugadores, uno por posición y liga configurada. La redundancia de las cinco combinaciones retiradas se apoya además en el análisis estructural de sus columnas declaradas, que no depende de la muestra.
- No se observaron cambios de `statColumns` por competici��n o por ventana temporal; una combinación retirada podría recuperar aporte si el proveedor publicara allí una columna del catálogo.
- La estimación de duración no se midió en ejecución: es aritmética sobre el pacing configurado.
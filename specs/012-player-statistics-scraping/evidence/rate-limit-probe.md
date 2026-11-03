# Sondeo de ritmo contra el feed JSON de WhoScored

**Fecha de observación**: 2026-10-16
**Endpoint**: `GET https://es.whoscored.com/statisticsfeed/1/getplayerstatistics`
**Método**: `curl.exe` con `User-Agent` de navegador, que es el único header indispensable comprobado.

## Resultado principal

**No se obtuvo ningún 429.** El proveedor no limitó peticiones en ningún ritmo probado. Lo que
ocurre es degradación por errores 5xx, y esa degradación responde a la **concurrencia**, no al
ritmo sostenido.

| Prueba | Concurrencia | Ritmo real | 200 | 5xx | 429 | 403 |
| --- | --- | --- | --- | --- | --- | --- |
| Secuencial, 2500 ms entre inicios | 1 | 1,68 req/s | 509 | 90 | 0 | 0 |
| Ráfaga, 2 procesos | 2 | 2,15 req/s | 91 | 0 | 0 | 0 |
| Ráfaga, 3 procesos | 3 | 1,39 req/s | 85 | 3 | 0 | 0 |
| Ráfaga, 6 procesos | 6 | 5,64 req/s | 89 | 214 | 0 | 0 |

La prueba de 3 procesos corrió **más lento** que la de 2 y aun así tuvo más errores, mientras la de
2 procesos a mayor ritmo no tuvo ninguno. El factor que acompaña a los errores es el número de
peticiones simultáneas.

## Configuración elegida

`json-workers: 4` y `request-interval-ms: 1500`.

Con un intervalo de 1500 ms y una latencia observada de ~400 ms por consulta, hay a lo sumo una
petición en vuelo por hilo. El resultado esperado es **~2,7 req/s** con concurrencia efectiva de 1,
que es la zona medida sin errores.

## Por qué no se sube el ritmo más

El daño no es del ritmo sino del **número de jugadores que fallan**, que se multiplica por las
quince peticiones que necesita cada jugador:

| Error por petición | Jugadores que fallan de 2072 |
| --- | --- |
| 0,3% | ~90 |
| 1% | ~270 |
| 3% | ~640 |
| 15% | ~2.000 |

Bajar el intervalo acelera pero multiplica ese daño. Subir los workers lo multiplica mucho más
rápido: a 6 workers el 70% de 5xx.

## Duración estimada con la configuración elegida

```
2072 jugadores x 15 combinaciones = 31.080 peticiones
/ 4 workers = 7.770 por hilo
x 1,5 s = 11.655 s ~ 3 h 15 min
```

## Límites de esta evidencia

- **No se alcanzó un umbral de 429**, de modo que no se conoce el límite real de peticiones. Puede
  existir muy por encima de 5,6 req/s, o ser una cuota diaria del orden de las 31.080 peticiones que
  necesita la campaña. No se sondó más alto para no provocar un 403 que bloqueara la IP.
- Los 5xx parecen **correlacionados con el tiempo** además de con la carga: la primera prueba dio 15%
  y las de 2 y 3 workers, minutos después, dieron 0% y 3% a ritmos comparables o mayores. Con un
  solo día de datos no se separa "proveedor inestable" de "carga aplicada".
- La muestra de la prueba de 2 workers es de 91 peticiones; la de 3 workers, de 90. Las cifras de
  esa zona tienen margen de error amplio.
- No se midió concurrencia por debajo de 6 procesos con peticiones rápidas, que sería el escenario
  más cercano a "muchos jugadores en paralelo".
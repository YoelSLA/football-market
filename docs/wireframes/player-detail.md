# Wireframe: detalle y estadísticas del jugador

Referencia visual canónica del rediseño de [SPEC-009](../../specs/009-player-detail/spec.md), aprobada el 2026-11-09. Define jerarquía, composición, contenido y orden responsive; no prescribe dimensiones pixel-perfect ni agrega datos, métricas o interacciones fuera de la especificación.

Las cifras de ejemplo sirven únicamente para mostrar la ubicación y el formato. La aplicación presenta los valores reales del snapshot seleccionado, conserva cero como dato válido y usa «Sin datos» ante ausencia.

## Estructura común

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET                 Inicio  Jugadores  Mercado  Cartera  Usuario│
├─────────────────────────────────────────────────────────────────────────────┤
│ Catálogo      Ranking      Historial de cotizaciones                        │
└─────────────────────────────────────────────────────────────────────────────┘

 Jugadores  >  Aaron Anselmino

┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌───────────┐                                                               │
│ │           │  AARON ANSELMINO                         Temporada 2026/2027  │
│ │   FOTO    │  Chelsea FC · Premier League                                  │
│ │           │                                                               │
│ └───────────┘  Defensor · Argentina · 29/04/2005                            │
│                                                                             │
│                Contexto estadístico                                         │
│                Competición: Premier League · Actualizado: 09/11/2026        │
│                Fuentes: JSON + Scraping                                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┬──────────────────────────────────────────────────┐
│ RESUMEN                  │ TODAS LAS ESTADÍSTICAS                           │
└──────────────────────────┴──────────────────────────────────────────────────┘
```

La cabecera separa la identidad vigente del jugador del contexto histórico de las estadísticas. Una competición, temporada, fecha o procedencia ausente se comunica como no disponible; no se sustituye por la liga actual ni por valores de ejemplo.

## Pestaña Resumen

La estructura compartida contiene exactamente cuatro indicadores, dos paneles específicos de la posición y un único bloque de estadísticas destacadas. Una métrica no se repite como dato principal dentro del Resumen.

### Defensor

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ RENDIMIENTO COMO DEFENSOR                                                   │
│ Datos de temporada · Los valores por partido no se convierten en totales   │
├──────────────────┬──────────────────┬──────────────────┬────────────────────┤
│ ENTRADAS         │ INTERCEPCIONES   │ DESPEJES         │ DUELOS AÉREOS      │
│ 2,4              │ 1,7              │ 4,1              │ 3,2                │
│ por partido      │ por partido      │ por partido      │ ganados/partido    │
├──────────────────┴──────────────────┴──────────────────┴────────────────────┤
│                                                                             │
│ ┌─────────────────────────────────────┐ ┌─────────────────────────────────┐ │
│ │ ACCIONES DEFENSIVAS                 │ │ DUELOS AÉREOS                  │ │
│ │                                     │ │                                │ │
│ │ Entradas           ████████  2,4    │ │ Ganados      ████████  3,2     │ │
│ │ Intercepciones     ██████    1,7    │ │ Perdidos     █████     2,1     │ │
│ │ Despejes           █████████ 4,1    │ │                                │ │
│ │ Tiros bloqueados   ███       0,8    │ │ Valores por partido            │ │
│ │ Centros bloqueados ████      1,1    │ └─────────────────────────────────┘ │
│ │ Pases bloqueados   ███       0,9    │                                     │
│ └─────────────────────────────────────┘ ┌─────────────────────────────────┐ │
│                                         │ ESTADÍSTICAS DESTACADAS        │ │
│                                         │                                │ │
│                                         │ Calificación             7,14  │ │
│                                         │ Minutos jugados          2.430 │ │
│                                         │ Apariciones                 29 │ │
│                                         │ Precisión de pases       86,4% │ │
│                                         │ Superado por regate         0,6 │ │
│                                         │ Entradas intentadas         3,0 │ │
│                                         │ Faltas / Amarillas / Rojas 1,1/5/0│
│                                         └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

Indicadores: entradas, intercepciones, despejes y duelos aéreos ganados. Paneles: acciones defensivas y duelos aéreos. Destacadas: minutos, apariciones, calificación, precisión de pases, veces superado por regate, entradas intentadas, faltas y tarjetas.

### Arquero

```text
┌────────────────┬────────────────┬────────────────────┬──────────────────────┐
│ ATAJADAS       │ PRECISIÓN      │ PASES LARGOS       │ CALIFICACIÓN         │
│ 3,8            │ 79,2%          │ 5,4                │ 7,03                 │
│ por partido    │ de pases       │ acertados/partido  │ promedio             │
└────────────────┴────────────────┴────────────────────┴──────────────────────┘

┌────────────────────────────────────┐ ┌──────────────────────────────────────┐
│ ATAJADAS POR ZONA                  │ │ DISTRIBUCIÓN                         │
│ Área chica        ███████  1,2     │ │ Pases cortos acertados  █████  18,3 │
│ Área penal        █████████ 2,1    │ │ Pases cortos fallidos   ██      3,1 │
│ Fuera del área    ███      0,5     │ │ Pases largos acertados  ████    5,4 │
│                                    │ │ Pases largos fallidos    ███     4,0 │
└────────────────────────────────────┘ └──────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ ESTADÍSTICAS DESTACADAS                                                     │
│ Minutos · Apariciones · Despejes · Duelos aéreos ganados · Pases · Tarjetas│
└─────────────────────────────────────────────────────────────────────────────┘
```

Indicadores: atajadas, precisión de pases, pases largos acertados y calificación. Paneles: atajadas por zona y distribución. Destacadas: minutos, apariciones, despejes, duelos aéreos ganados, pases por partido y tarjetas.

### Mediocampista

```text
┌────────────────┬────────────────┬────────────────┬──────────────────────────┐
│ PASES          │ PRECISIÓN      │ PASES CLAVE    │ ASISTENCIAS              │
│ por partido    │ de pases       │ por partido    │ total temporada          │
└────────────────┴────────────────┴────────────────┴──────────────────────────┘

┌────────────────────────────────────┐ ┌──────────────────────────────────────┐
│ DISTRIBUCIÓN                       │ │ CREACIÓN                            │
│ Pases cortos acertados             │ │ Pases clave largos                  │
│ Pases cortos fallidos              │ │ Pases clave cortos                  │
│ Pases largos acertados             │ └──────────────────────────────────────┘
│ Pases largos fallidos              │
└────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ ESTADÍSTICAS DESTACADAS                                                     │
│ Minutos · Apariciones · Calificación · Asistencias/partido · Regates        │
│ Entradas · Intercepciones · Pérdidas · Malos controles · Goles · Faltas    │
└─────────────────────────────────────────────────────────────────────────────┘
```

Indicadores: pases, precisión, pases clave y asistencias. Paneles: distribución y creación. Destacadas: minutos, apariciones, calificación, asistencias por partido, regates exitosos, entradas, intercepciones, pérdidas, malos controles, goles y faltas.

### Delantero

```text
┌────────────────┬────────────────┬────────────────┬──────────────────────────┐
│ GOLES          │ ASISTENCIAS    │ TIROS          │ CALIFICACIÓN             │
│ total temporada│ total temporada│ por partido    │ promedio                 │
└────────────────┴────────────────┴────────────────┴──────────────────────────┘

┌────────────────────────────────────┐ ┌──────────────────────────────────────┐
│ TIROS POR ZONA                     │ │ ASISTENCIAS POR TIPO                │
│ Área chica                         │ │ Centro                              │
│ Área penal                         │ │ Córner                              │
│ Fuera del área                     │ │ Pase filtrado                       │
│                                    │ │ Tiro libre                          │
│                                    │ │ Saque lateral                       │
│                                    │ │ Otras                               │
└────────────────────────────────────┘ └──────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ ESTADÍSTICAS DESTACADAS                                                     │
│ Minutos · Apariciones · Goles/partido · Asistencias/partido · Pases clave   │
│ Regates · Duelos aéreos · Fueras de juego · Precisión · Tarjetas           │
└─────────────────────────────────────────────────────────────────────────────┘
```

Indicadores: goles, asistencias, tiros y calificación. Paneles: tiros por zona y asistencias por tipo. Destacadas: minutos, apariciones, goles y asistencias por partido, pases clave, regates exitosos, duelos aéreos ganados, fueras de juego, precisión de pases y tarjetas.

## Pestaña Todas las estadísticas

La posición determina relevancia y orden, pero nunca oculta métricas. El catálogo conserva las ocho categorías y las 60 claves canónicas.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ TODAS LAS ESTADÍSTICAS                                      60 métricas     │
│ Ordenadas para: DEFENSOR                                                    │
│                                                                             │
│ [ Buscar por nombre o clave...                           ] [Limpiar]         │
│                                                                             │
│ [Todas 60] [General 5] [Ataque 19] [Pases 10] [Regates 5]                  │
│ [Duelos 3] [Defensa 10] [Portería 4] [Disciplina 4]                        │
│                                                                             │
│ Relevancia:  ● Principal   ○ Secundaria   · Poco relevante                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ GENERAL                                                             5      │
│                                                                             │
│ ● Minutos jugados                                      2.430 min           │
│ ● Apariciones                                             29 partidos       │
│ ● Titularidades                                           Sin datos         │
│ ● Calificación WhoScored                                   7,14 puntos       │
│ ○ Apariciones como suplente                                3 partidos       │
├─────────────────────────────────────────────────────────────────────────────┤
│ DEFENSA                                                            10      │
│                                                                             │
│ ● Entradas                                                 2,4 /partido    │
│ ● Intercepciones                                           1,7 /partido    │
│ ● Despejes                                                 4,1 /partido    │
│ ● Tiros bloqueados                                         0,8 /partido    │
│ ● Veces superado por regate                                0,6 /partido    │
│ ● Entradas intentadas                                      3,0 /partido    │
│ ● Centros bloqueados                                       1,1 /partido    │
│ ● Pases bloqueados                                         0,9 /partido    │
│ ○ Fueras de juego provocados                               0,4 /partido    │
│ ○ Autogoles                                                0 total         │
└─────────────────────────────────────────────────────────────────────────────┘
```

Orden dentro de cada categoría:

```text
1. Métricas principales con datos
2. Métricas secundarias con datos
3. Métricas poco relevantes con datos
4. Métricas sin datos, conservando su relevancia
```

La relevancia siempre se comunica mediante texto o símbolo además del color. Cada fila mantiene nombre, unidad y valor; cero no se desplaza al grupo de ausencias.

## Estados

### Carga

```text
┌──────────────────────────────────────────────────────────────┐
│ Consultando estadísticas del jugador…                        │
└──────────────────────────────────────────────────────────────┘
```

La identidad resuelta permanece visible mientras se consulta el bloque estadístico. Carga no se presenta como ausencia.

### Sin estadísticas confirmadas

```text
┌──────────────────────────────────────────────────────────────┐
│ No hay estadísticas confirmadas para esta temporada.         │
│ Las 60 métricas siguen disponibles en el catálogo y muestran │
│ «Sin datos».                                                 │
└──────────────────────────────────────────────────────────────┘
```

### Datos parciales

```text
┌──────────────────────────────────────────────────────────────┐
│ Datos parciales: 57 de 60 métricas disponibles.              │
│ Los valores ausentes no se consideran cero.                  │
└──────────────────────────────────────────────────────────────┘
```

Los paneles representan únicamente valores disponibles. Si todos los valores conocidos son cero, muestran cero explícito; no se reemplazan por un estado vacío.

### Error sin respuesta compatible

```text
┌──────────────────────────────────────────────────────────────┐
│ No se pudieron consultar las estadísticas.      [Reintentar]│
│ La identidad y la navegación permanecen disponibles.        │
└──────────────────────────────────────────────────────────────┘
```

### Error con respuesta anterior compatible

```text
┌──────────────────────────────────────────────────────────────┐
│ No se pudo actualizar. Mostrando la última consulta          │
│ disponible del 08/11/2026.                       [Reintentar] │
└──────────────────────────────────────────────────────────────┘
```

La respuesta anterior conserva su fecha y contexto originales. Nunca se reutilizan datos de otro jugador o temporada.

## Adaptación responsive

### Escritorio, desde 1280 px

```text
Cabecera: [retrato + identidad........................][contexto]
Resumen:  [indicador][indicador][indicador][indicador]
          [panel posicional A........][panel posicional B......]
          [estadísticas destacadas..............................]
Catálogo: [búsqueda...................][limpiar]
          [categorías en una o dos filas]
          [grupos de métricas a todo el ancho]
```

### Tablet, alrededor de 768 px

```text
Cabecera: [retrato + identidad]
          [contexto estadístico]
Resumen:  [indicador][indicador]
          [indicador][indicador]
          [panel posicional A]
          [panel posicional B]
          [estadísticas destacadas]
Catálogo: [búsqueda][limpiar]
          [categorías con ajuste de línea]
          [grupos de métricas]
```

### Móvil, desde 360 px

```text
Cabecera: [retrato + nombre]
          [equipo y liga]
          [posición · nacimiento · nacionalidad]
          [temporada · competición · extracción · procedencia]
Pestañas: [Resumen][Todas las estadísticas]
Resumen:  [indicador]
          [indicador]
          [indicador]
          [indicador]
          [panel posicional A]
          [panel posicional B]
          [estadísticas destacadas]
Catálogo: [búsqueda]
          [limpiar]
          [categorías envolventes o desplazables dentro de su fila]
          [métrica: nombre + relevancia]
          [unidad................valor]
```

En todos los anchos:

- El orden semántico es identidad → contexto → pestañas → contenido.
- La página usa desplazamiento vertical natural y no presenta desplazamiento horizontal.
- Las pestañas, búsqueda, categorías, limpiar y reintento conservan foco visible y nombres accesibles.
- Una fila de categorías puede envolver o desplazarse dentro de su propio contenedor, sin ocultar controles ni ensanchar el documento.
- No se ocultan métricas para hacer encajar el contenido.

## Límites

Este wireframe no autoriza selectores de temporada, filtros adicionales, paginación, radar, percentiles, comparación poblacional, xG/xA, mapas de eventos, series por partido, cotizaciones ni datos financieros. Las etiquetas, unidades, perfiles y prioridades exactos continúan gobernados por SPEC-009 y su [glosario canónico](../../specs/009-player-detail/contracts/metric-glossary.md).

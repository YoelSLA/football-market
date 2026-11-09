# Wireframe: detalle y estadísticas del jugador

Referencia visual canónica del rediseño de [SPEC-009](../../specs/009-player-detail/spec.md), aprobada el 2026-11-09. Define jerarquía, composición, contenido, distribución vertical y orden responsive; no agrega datos, métricas o interacciones fuera de la especificación.

Las cifras de ejemplo sirven únicamente para mostrar la ubicación y el formato. La aplicación presenta los valores reales del snapshot seleccionado, conserva cero como dato válido y usa «Sin datos» ante ausencia.

---

## Presupuesto vertical y Box Model (Resolución de referencia: 1920 × 913 px)

En la resolución de pantalla de referencia ($1920 \times 913\text{ px}$), la aplicación no presenta scroll global en la ventana del navegador. La navegación global superior permanece fija y todo el contenido de la página se distribuye de forma armónica dentro del espacio vertical visible.

$$\begin{aligned}
\text{Viewport del navegador:} & \quad \mathbf{913\text{ px}} \\
\text{Navbar y navegación global fija (2 niveles):} & \quad -\; 108\text{ px} \\
\hline
\text{Altura neta disponible para } \texttt{PlayerDetailPage}\text{:} & \quad \mathbf{805\text{ px}}
\end{aligned}$$

### Desglose vertical exacto (805 px)

| N° | Sección / Elemento | Tipo de altura | Altura neta | Box Model, Padding interno y Dimensiones clave |
| :-: | :--- | :---: | :---: | :--- |
| **1** | **Breadcrumb** | Fija | **32 px** | `height: 32px; padding: 6px 0;`<br>Línea tipográfica de 20 px (`font-size: 14px`, `line-height: 20px`). Migas de navegación fijas superiores. |
| **2** | **Cabecera (PlayerOverview)** | Acotada / Fija | **125 px** | `height: 125px; padding: 12px 20px; box-sizing: border-box; border: 1px;`<br>• **Izquierda**: Retrato de $90 \times 90\text{ px}$ e identidad en 3 líneas limpias.<br>• **Centro**: 3 micro-tarjetas compactas de $70\text{ px}$ de alto.<br>• **Derecha**: Píldora de temporada ($24\text{ px}$) + fecha de actualización. |
| **3** | **Separación vertical** | Fija | **12 px** | `margin: 12px 0 0 0;`<br>Espacio de separación visual entre el borde inferior de la cabecera y las pestañas. |
| **4** | **Pestañas (PlayerStatisticsTabs)** | Fija | **40 px** | `height: 40px; box-sizing: border-box; border-bottom: 1px;`<br>Botones de pestaña con `min-height: 40px`, tipografía 13 px, pestañas a todo el ancho. |
| **5** | **Región de contenido activa** | Flexible (`flex: 1`) | **596 px** | `height: 596px; min-height: 0; padding: 12px 4px 12px 0; box-sizing: border-box;`<br>Contenedor con `overflow-y: auto`, `scrollbar-width: thin`. Región con scroll interno exclusivo. |
| — | **TOTAL COMPROBADO** | **Exacto** | **805 px** | **$32\text{ px} + 125\text{ px} + 12\text{ px} + 40\text{ px} + 596\text{ px} = \mathbf{805\text{ px}}$** |

> [!IMPORTANT]
> **Normas de Box Model y Comportamiento de Scroll**:
> 1. **`box-sizing: border-box` estricto**: Aplica universalmente a todos los componentes, contenedores, tarjetas y paddings.
> 2. **596 px es un resultado de referencia**: Corresponde al espacio restante en un viewport de $913\text{ px}$ de alto con navbar de $108\text{ px}$. No debe declararse como una altura fija rígida en CSS (`height: 596px`), sino como un contenedor elástico flexible (`flex: 1; min-height: 0; overflow-y: auto;`).
> 3. **Zona Superior Fija en Desktop (209 px)**: Breadcrumb ($32\text{ px}$) + Cabecera ($125\text{ px}$) + Separación ($12\text{ px}$) + Pestañas ($40\text{ px}$) permanecen estáticos arriba. El usuario nunca pierde de vista la identidad ni los datos clave mientras navega.
> 4. **Scroll Exclusivo en Desktop**: Si el contenido de la pestaña activa excede la altura disponible (como ocurre con las 60 métricas de *Todas las estadísticas* o con pantallas de menor altura), el desplazamiento vertical pertenece exclusivamente a la región de contenido activa. Nunca existe scroll global en la ventana (`window`) del navegador.
> 5. **Tablet y Móvil**: Conmutan a scroll documental natural de página (`overflow: visible; height: auto;`), adaptando los bloques verticalmente sin overflow horizontal.

---

## Estructura común

```text
Viewport de referencia: 1920 × 913 px
Navbar global (Nivel 1 + Nivel 2):  108 px (Fijo superior)
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
Altura neta PlayerDetailPage:       805 px (Sin scroll global en ventana)

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐ ▲
│ FOOTBALL MARKET                 Inicio  Jugadores  Mercado  Cartera  Usuario                          68 px │ │ 108 px
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │ NAVBAR
│ Catálogo      Ranking      Historial de cotizaciones                                                  40 px │ ▼ GLOBAL
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Breadcrumb: Jugadores  >  Aaron Anselmino                                                             32 px │ ▲
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ CABECERA DEL JUGADOR (PlayerOverview)                                                                125 px │ │ 209 px
│ ┌──────────┐                               ┌─────────────┬─────────────┬─────────────┐                      │ │ ZONA
│ │          │ AARON ANSELMINO               │  ENTRADAS   │ INTERCEP.   │  DESPEJES   │  Temporada 2026/2027 │ │ FIJA
│ │  FOTO    │ Chelsea FC · Premier League   │     2,4     │     1,7     │     4,1     │                      │ │ SUPERIOR
│ │  (90px)  │ Defensor · Argentina · 29/04  │ por partido │ por partido │ por partido │ Actualizado:         │ │
│ └──────────┘                               └─────────────┴─────────────┴─────────────┘ 09/11/2026           │ │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ Separación vertical                                                                                   12 px │ │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ PESTAÑAS (PlayerStatisticsTabs):  [ RESUMEN ]   [ TODAS LAS ESTADÍSTICAS ]                            40 px │ ▼
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ ▲
│ REGIÓN DE CONTENIDO DE LA PESTAÑA ACTIVA (Scroll interno exclusivo si excede 596 px)                 596 px │ │
│                                                                                                             │ │
│ [ Panel de Resumen o Catálogo completo según la pestaña seleccionada ]                                      │ │ 596 px
│                                                                                                             │ │ FLEXIBLE
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘ ▼

TOTAL VERTICAL DISPONIBLE: 32 px + 125 px + 12 px + 40 px + 596 px = EXACTAMENTE 805 px
```

### Cabecera compacta en 3 zonas (125 px)

La cabecera se organiza horizontalmente en 3 zonas equilibradas:
1. **Zona Izquierda (Identidad)**: Retrato ($90 \times 90\text{ px}$), nombre destacado (`h1`), equipo y liga en una sola línea, y metadatos (`Posición · Nacionalidad · Nacimiento`) con separadores visuales `·`.
2. **Zona Centro (3 Métricas Titulares Compactas)**: Contenedor con 3 micro-tarjetas ($70\text{ px}$ de altura cada una) que muestran las 3 acciones más representativas de la posición del jugador.
3. **Zona Derecha (Actualización y Contexto Temporal)**: Píldora de temporada (`Temporada YYYY/YYYY` o `Temporada no disponible`) y fecha de corte de la última extracción (`Actualizado: DD/MM/YYYY` o `Actualizado: No disponible`). Se eliminan por completo «Contexto estadístico», «Fuentes» y «Competición» de la cabecera para optimizar la altura vertical.

---

## Pestaña Resumen

### Regla de desduplicación estricta
Para evitar repeticiones visuales innecesarias:
1. **Las 4 tarjetas principales de Resumen son complementarias** y no repiten ninguna de las 3 métricas ya visibles en la cabecera.
2. **Los 2 paneles específicos** desglosan en barras proporcionales las zonas o tipologías de juego propias de la posición.
3. **El bloque de estadísticas destacadas** presenta en una lista estructurada las métricas adicionales (minutos, apariciones, disciplina, etc.), deduplicadas contra la cabecera, contra las tarjetas y contra los paneles.

---

### Defensor

#### Cabecera (3 métricas titulares)
- **Entradas**: `tackles_per_match` (por partido)
- **Intercepciones**: `interceptions_per_match` (por partido)
- **Despejes**: `clearances_per_match` (por partido)

#### Resumen (4 tarjetas complementarias + 2 paneles + destacadas)
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ RENDIMIENTO COMO DEFENSOR                                                                                   │
│ Datos de temporada · Los valores por partido no se convierten en totales                                    │
├──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┤
│ CALIFICACIÓN         │ DUELOS AÉREOS        │ PRECISIÓN DE PASES   │ TIROS BLOQUEADOS                       │
│ 7,14                 │ 3,2                  │ 86,4%                │ 0,8                                    │
│ promedio WhoScored   │ ganados/partido      │ pases acertados      │ por partido                            │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─────────────────────────────────────────┐   ┌───────────────────────────────────────────────────────────┐ │
│ │ ACCIONES DEFENSIVAS                     │   │ DUELOS AÉREOS                                             │ │
│ │                                         │   │                                                           │ │
│ │ Entradas              ████████  2,4     │   │ Ganados         ████████  3,2                             │ │
│ │ Intercepciones        ██████    1,7     │   │ Perdidos        █████     2,1                             │ │
│ │ Despejes              █████████ 4,1     │   │                                                           │ │
│ │ Tiros bloqueados      ███       0,8     │   │ Valores promedio por partido                              │ │
│ │ Centros bloqueados    ████      1,1     │   └───────────────────────────────────────────────────────────┘ │
│ │ Pases bloqueados      ███       0,9     │   ┌───────────────────────────────────────────────────────────┐ │
│ │                                         │   │ ESTADÍSTICAS DESTACADAS                                   │ │
│ │ Valores promedio por partido            │   │                                                           │ │
│ └─────────────────────────────────────────┘   │ Minutos jugados                                2.430 mins │ │
│                                               │ Apariciones                                   29 partidos │ │
│                                               │ Superado por regate                          0,6 /partido │ │
│                                               │ Entradas intentadas                          3,0 /partido │ │
│                                               │ Faltas cometidas                             1,1 /partido │ │
│                                               │ Tarjetas amarillas                            5 tarjetas  │ │
│                                               │ Tarjetas rojas                                0 tarjetas  │ │
│                                               └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Arquero

#### Cabecera (3 métricas titulares)
- **Atajadas**: `saves_per_match` (por partido)
- **Precisión de pases**: `pass_completion_percentage` (% acierto)
- **Pases largos acertados**: `successful_long_balls_per_match` (por partido)

#### Resumen (4 tarjetas complementarias + 2 paneles + destacadas)
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ RENDIMIENTO COMO ARQUERO                                                                                    │
│ Datos de temporada · Los valores por partido no se convierten en totales                                    │
├──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┤
│ CALIFICACIÓN         │ DESPEJES             │ DUELOS AÉREOS        │ PASES TOTALES                          │
│ 7,03                 │ 1,8                  │ 0,9                  │ 28,4                                   │
│ promedio WhoScored   │ por partido          │ ganados/partido      │ por partido                            │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─────────────────────────────────────────┐   ┌───────────────────────────────────────────────────────────┐ │
│ │ ATAJADAS POR ZONA                       │   │ DISTRIBUCIÓN DE PASES                                     │ │
│ │                                         │   │                                                           │ │
│ │ Área chica            ███████  1,2      │   │ Cortos acertados        █████████ 18,3                    │ │
│ │ Área penal            █████████ 2,1     │   │ Cortos fallidos         ██        3,1                     │ │
│ │ Fuera del área        ███      0,5      │   │ Largos acertados        ████      5,4                     │ │
│ │                                         │   │ Largos fallidos         ███       4,0                     │ │
│ │ Valores promedio por partido            │   │ Valores promedio por partido                              │ │
│ └─────────────────────────────────────────┘   └───────────────────────────────────────────────────────────┘ │
│                                               ┌───────────────────────────────────────────────────────────┐ │
│                                               │ ESTADÍSTICAS DESTACADAS                                   │ │
│                                               │ Minutos: 2.700 · Partidos: 30 · Faltas: 0,1 · Tarjetas: 1 │ │
│                                               └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Mediocampista

#### Cabecera (3 métricas titulares)
- **Pases por partido**: `passes_per_match` (por partido)
- **Precisión de pases**: `pass_completion_percentage` (% acierto)
- **Pases clave**: `key_passes_per_match` (por partido)

#### Resumen (4 tarjetas complementarias + 2 paneles + destacadas)
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ RENDIMIENTO COMO MEDIOCAMPISTA                                                                              │
│ Datos de temporada · Los valores por partido no se convierten en totales                                    │
├──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┤
│ CALIFICACIÓN         │ ASISTENCIAS          │ ENTRADAS             │ REGATES EXITOSOS                       │
│ 7,22                 │ 8                    │ 2,1                  │ 1,9                                    │
│ promedio WhoScored   │ total temporada      │ por partido          │ por partido                            │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─────────────────────────────────────────┐   ┌───────────────────────────────────────────────────────────┐ │
│ │ DISTRIBUCIÓN DE PASES                   │   │ CREACIÓN DE JUEGO                                         │ │
│ │ Cortos acertados      █████████ 45,2    │   │ Pases clave cortos      ████████  1,2                     │ │
│ │ Cortos fallidos       ███       4,8     │   │ Pases clave largos      ███       0,4                     │ │
│ │ Largos acertados      █████     6,1     │   │                                                           │ │
│ │ Largos fallidos       ██        2,3     │   │ Valores promedio por partido                              │ │
│ └─────────────────────────────────────────┘   └───────────────────────────────────────────────────────────┘ │
│                                               ┌───────────────────────────────────────────────────────────┐ │
│                                               │ ESTADÍSTICAS DESTACADAS                                   │ │
│                                               │ Minutos: 2.340 · Partidos: 27 · Goles: 4 · Faltas: 1,3    │ │
│                                               └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Delantero

#### Cabecera (3 métricas titulares)
- **Goles**: `goals_total` (total temporada)
- **Tiros por partido**: `shots_per_match` (por partido)
- **Asistencias**: `assists_total` (total temporada)

#### Resumen (4 tarjetas complementarias + 2 paneles + destacadas)
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ RENDIMIENTO COMO DELANTERO                                                                                  │
│ Datos de temporada · Los valores por partido no se convierten en totales                                    │
├──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┤
│ CALIFICACIÓN         │ TIROS AL ARCO        │ PASES CLAVE          │ REGATES EXITOSOS                       │
│ 7,35                 │ 38                   │ 1,5                  │ 2,3                                    │
│ promedio WhoScored   │ tiros temporada      │ por partido          │ por partido                            │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─────────────────────────────────────────┐   ┌───────────────────────────────────────────────────────────┐ │
│ │ TIROS POR ZONA                          │   │ ASISTENCIAS POR TIPO                                      │ │
│ │ Área chica            ███       0,3     │   │ Centros                 ████      1                       │ │
│ │ Área penal            █████████ 2,1     │   │ Pases filtrados         ████████  3                       │ │
│ │ Fuera del área        ████      0,8     │   │ Tiro libre / Córner     ██        0                       │ │
│ └─────────────────────────────────────────┘   └───────────────────────────────────────────────────────────┘ │
│                                               ┌───────────────────────────────────────────────────────────┐ │
│                                               │ ESTADÍSTICAS DESTACADAS                                   │ │
│                                               │ Minutos: 2.150 · Partidos: 25 · Duelos aéreos ganados: 1,4│ │
│                                               └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Pestaña Todas las estadísticas

La posición determina relevancia y orden, pero nunca oculta métricas. El catálogo conserva las ocho categorías y las 60 claves canónicas dentro de la región con scroll interno.

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

---

## Estados del Snapshot

### Carga
```text
┌──────────────────────────────────────────────────────────────┐
│ Consultando estadísticas del jugador…                        │
└──────────────────────────────────────────────────────────────┘
```

### Sin estadísticas confirmadas (NO_DATA)
```text
┌──────────────────────────────────────────────────────────────┐
│ No hay estadísticas confirmadas para esta temporada.         │
│ Las 60 métricas siguen disponibles en el catálogo y muestran │
│ «Sin datos».                                                 │
└──────────────────────────────────────────────────────────────┘
```

### Datos parciales (PARTIAL)
```text
┌──────────────────────────────────────────────────────────────┐
│ Datos parciales: 57 de 60 métricas disponibles.              │
│ Los valores ausentes no se consideran cero.                  │
└──────────────────────────────────────────────────────────────┘
```

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

---

## Adaptación responsive

### Escritorio (desde 1280 px)
- **Zona Superior Fija (209 px)**: Breadcrumb ($32\text{ px}$) + Cabecera ($125\text{ px}$) + Separación ($12\text{ px}$) + Pestañas ($40\text{ px}$).
- **Región de Contenido Activa ($596\text{ px}$ en viewport de $913\text{ px}$)**: `flex: 1; min-height: 0; overflow-y: auto`. Scroll vertical interno exclusivo. Cero scroll global en ventana.
- **Cabecera**: 3 zonas horizontales alineadas al centro con $125\text{ px}$ de altura (`box-sizing: border-box`).

### Tablet (alrededor de 768 px)
- **Comportamiento vertical**: Pasa a scroll documental natural de página (`overflow: visible; height: auto;`).
- **Cabecera (~165–175 px)**:
  - Fila superior: Foto ($75\text{ px}$) + Nombre/Equipo/Liga a la izquierda, Temporada y Actualización a la derecha.
  - Fila inferior: 3 tarjetas en una fila horizontal a todo el ancho.
- **Resumen**: 4 tarjetas en grilla $2 \times 2$, y paneles de barras apilados verticalmente. Cero overflow horizontal.

### Móvil (desde 360 px)
- **Comportamiento vertical**: Desplazamiento vertical natural sin restricciones de altura fija.
- **Cabecera (~195 px)**:
  - Foto compacta ($64\text{ px}$) centrada o alineada a la izquierda con nombre y metadatos.
  - Temporada y fecha en una sola línea textual centrada ($11\text{ px}$).
  - **Fila de 3 micro-tarjetas simétricas en una sola línea ($108\text{ px}$ de ancho cada una)**:
    ```text
    ├─────────────┬─────────────┬─────────────┤
    │  ENTRADAS   │ INTERCEP.   │  DESPEJES   │
    │     1,8     │     1,4     │     3,9     │
    │  /partido   │  /partido   │  /partido   │
    └─────────────┴─────────────┴─────────────┘
    ```
- **Pestañas**: 40 px de alto, 50% de ancho cada pestaña.
- **Resumen**: Columna única vertical con tarjetas y barras adaptadas al ancho disponible de 336 px útiles, garantizando cero desplazamiento horizontal (`max-width: 100vw; min-width: 0`).

---

## Límites

Este wireframe no autoriza selectores de temporada, filtros adicionales, paginación, radar, percentiles, comparación poblacional, xG/xA, mapas de eventos, series por partido, cotizaciones ni datos financieros. Las etiquetas, unidades, perfiles y prioridades exactos continúan gobernados por SPEC-009 y su [glosario canónico](../../specs/009-player-detail/contracts/metric-glossary.md).

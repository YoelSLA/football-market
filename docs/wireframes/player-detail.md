# Wireframe: detalle, cotizaciones y estadísticas del jugador

Referencia visual canónica de `PlayerDetailPage` en Football Market, actualizada para la integración unificada de cotizaciones oficiales de tokens, resumen de rendimiento y catálogo completo de estadísticas. Define jerarquía, composición, contenido, distribución vertical y orden responsive; no agrega datos ni interacciones fuera de los contratos reales de la aplicación.

Las cifras de ejemplo sirven únicamente para mostrar ubicación y formato. La aplicación presenta valores reales: en cotizaciones consume el historial y cotización vigente de `GET /api/players/{id}/quotes`; en estadísticas consume el snapshot de `GET /api/players/{id}/statistics`. Conserva cero como dato válido y utiliza «Sin datos» ante ausencia.

---

## Diferenciación conceptual: cotización oficial vs. precio de mercado secundario

> [!IMPORTANT]
> **Regla de negocio e identidad de mercado:**
> 1. **Cotización oficial vigente (`Official Quote`)**: Es el valor de referencia algorítmico calculado automáticamente por el sistema tras cada evaluación de rendimiento deportivo (`/api/players/{id}/quotes`). Representa el precio institucional del token del jugador.
> 2. **Precio de venta entre usuarios (`Secondary Market Listing`)**: Corresponde al precio fijado libremente por usuarios que venden tokens de su cartera en el mercado secundario (`/api/market/listings`).
> 3. En la ficha de detalle del jugador se presenta **exclusivamente la cotización oficial y su evolución histórica institucional**, evitando cualquier confusión con ofertas privadas o precios especulativos de terceros.

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
| **1** | **Navegación contextual (`PlayerDetailNav`)** | Fija | **32 px** | `height: 32px; padding: 6px 0; box-sizing: border-box;`<br>Enlace semántico compacto «← Volver a jugadores» o «← Volver al ranking» según contexto de origen (`location.state.from`), sincronizado con query params. |
| **2** | **Cabecera (`PlayerOverview`)** | Acotada / Fija | **125 px** | `height: 125px; padding: 12px 20px; box-sizing: border-box; border: 1px;`<br>• **Izquierda**: Retrato ($90 \times 90\text{ px}$) e identidad en 3 líneas limpias.<br>• **Centro**: 3 micro-tarjetas deportivas titulares ($70\text{ px}$ de alto) según la posición.<br>• **Derecha**: Bloque financiero compacto con cotización oficial vigente destacada, variación porcentual y fecha de corte. |
| **3** | **Separación vertical** | Fija | **12 px** | `margin: 12px 0 0 0;`<br>Espacio de separación visual entre el borde inferior de la cabecera y la barra de pestañas. |
| **4** | **Pestañas (`PlayerStatisticsTabs`)** | Fija | **40 px** | `height: 40px; box-sizing: border-box; border-bottom: 1px;`<br>3 pestañas fijas a todo el ancho: `[ Cotizaciones ]` (predeterminada), `[ Resumen deportivo ]`, `[ Todas las estadísticas ]`. |
| **5** | **Región de contenido activa** | Flexible (`flex: 1`) | **596 px** | `height: 596px; min-height: 0; padding: 12px 4px 12px 0; box-sizing: border-box;`<br>Contenedor con `overflow-y: auto`, `scrollbar-width: thin`. Región con scroll interno exclusivo para la pestaña activa. |
| — | **TOTAL COMPROBADO** | **Exacto** | **805 px** | **$32\text{ px} + 125\text{ px} + 12\text{ px} + 40\text{ px} + 596\text{ px} = \mathbf{805\text{ px}}$** |

> [!IMPORTANT]
> **Normas de Box Model y Comportamiento de Scroll**:
> 1. **`box-sizing: border-box` estricto**: Aplica universalmente a todos los componentes, contenedores, tarjetas y paddings.
> 2. **596 px elástico**: Corresponde al remanente en desktop $1920 \times 913\text{ px}$. Se implementa con `flex: 1; min-height: 0; overflow-y: auto;` sin alturas fijas que provoquen desbordamiento.
> 3. **Zona Superior Fija en Desktop (209 px)**: Navegación ($32\text{ px}$) + Cabecera ($125\text{ px}$) + Separación ($12\text{ px}$) + Pestañas ($40\text{ px}$) permanecen estáticas. El usuario no pierde la identidad ni el valor de mercado mientras navega.
> 4. **Scroll Exclusivo en Desktop**: Si el contenido de la pestaña activa excede la altura disponible, el desplazamiento vertical pertenece exclusivamente a la región de contenido activa. Nunca existe scroll global en la ventana del navegador.
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
│ Catálogo      Ranking                                                                                 40 px │ ▼ GLOBAL
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ← Volver a jugadores (o «← Volver al ranking»)                                                        32 px │ ▲
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ CABECERA DEL JUGADOR (PlayerOverview)                                                                125 px │ │ 209 px
│ ┌──────────┐                               ┌─────────────┬─────────────┬─────────────┐ ┌──────────────────┐ │ │ ZONA
│ │          │ AARON ANSELMINO               │  ENTRADAS   │ INTERCEP.   │  DESPEJES   │ │ 1,05 FM ▲ +5,0%  │ │ │ FIJA
│ │  FOTO    │ Chelsea FC · Premier League   │     2,4     │     1,7     │     4,1     │ │ Cotización ofic. │ │ │ SUPERIOR
│ │  (90px)  │ Defensor · Argentina · 20 años│ por partido │ por partido │ por partido │ │ 20/10/2026       │ │ │
│ └──────────┘                               └─────────────┴─────────────┴─────────────┘ └──────────────────┘ │ │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ Separación vertical                                                                                   12 px │ │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ PESTAÑAS:  [ COTIZACIONES (Activa) ]    [ RESUMEN DEPORTIVO ]    [ TODAS LAS ESTADÍSTICAS ]                   40 px │ ▼
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ ▲
│ REGIÓN DE CONTENIDO DE LA PESTAÑA ACTIVA (Scroll interno exclusivo si excede 596 px)                 596 px │ │
│                                                                                                             │ │
│ [ Panel de Cotizaciones, Resumen deportivo o Catálogo completo según la pestaña seleccionada ]              │ │ 596 px
│                                                                                                             │ │ FLEXIBLE
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘ ▼

TOTAL VERTICAL DISPONIBLE: 32 px + 125 px + 12 px + 40 px + 596 px = EXACTAMENTE 805 px
```

### Cabecera compacta en 3 zonas (125 px)

1. **Zona Izquierda (Identidad)**: Retrato ($90 \times 90\text{ px}$) con borde de acento verde deportivo (`$surface-accent-dark`, borde `$accent-sports-green`), nombre destacado (`h1`), equipo y liga con escudo/logo en una sola línea, y metadatos (`Posición · Bandera/Nacionalidad · Nacimiento`).
2. **Zona Centro (3 Métricas Deportivas Titulares)**: Contenedor con 3 micro-tarjetas ($94\text{ px}$ de alto, fondo `$surface-accent-deep` con borde y divisores `$border-panel`) que sintetizan las acciones deportivas más representativas de la posición del jugador, con iconos en `$accent-sports-green` ($18\text{ px}$), etiquetas legibles ($10.5\text{ px}$) y cifras en negrita ($22\text{ px}$).
3. **Zona Derecha (Cotización Oficial y Actualización)**: Contenedor compacto ($94\text{ px}$ de alto, fondo `$surface-accent-deep` con borde `$border-panel`):
   - **Precio vigente oficial**: Formateado en créditos de mercado (ej. `1,00 crédito` / `1,05 créditos`, $22\text{ px}$, `$text-primary-on-dark`) con icono de moneda FM (`#f5c518`).
   - **Badge de variación semántico**:
     - Positiva: `▲ +X,XX %` (`$surface-accent-dark`, texto y borde `$accent-sports-green`).
     - Negativa: `▼ -X,XX %` (`$surface-status-closed`, texto `$state-loss`, borde `$border-status-closed`).
     - Neutra con precio anterior idéntico: `Sin variación` (`$surface-panel-from`, texto `$text-muted-on-dark`, borde `$border-panel`).
     - Sin precio anterior (`INITIAL`): `Cotización inicial` (`$surface-panel-from`, texto `$text-muted-on-dark`, borde `$border-panel`).
   - **Línea de metadata**: «Cotización oficial · DD/MM/AAAA», acompañada por la variación absoluta en créditos (ej. `· +0,05 créditos`) cuando exista variación calculable contra un precio anterior real.

---

## Pestaña 1: Cotizaciones (Predeterminada)

La pestaña predeterminada ubica como protagonista la evolución del valor del jugador en el mercado. Diseñada para priorizar comprensión inmediata para el usuario, sin tecnicismos internos de base de datos.

### Composición en Desktop

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ ┌─ GRÁFICO HISTÓRICO DE COTIZACIÓN (65% del ancho) ──────────┐ ┌─ PANEL FINANCIERO (35% del ancho) ───────┐ │
│ │ Evolución oficial del token                                │ │ Resumen de valuación oficial              │ │
│ │                                                            │ │                                           │ │
│ │  1,20 ┤              ╭───╮                                 │ │ Cotización vigente:            1,05 FM    │ │
│ │  1,10 ┤          ╭───╯   ╰───                              │ │ Cotización anterior:           1,00 FM    │ │
│ │  1,05 ┤      ────╯                                         │ │ Variación última:      +0,05 FM (+5,00 %) │ │
│ │  1,00 ┤  ────╯                                             │ │ Rendimiento base:                0,60 pts │ │
│ │       └──────┬─────┬─────┬─────┬─────┬─────┬               │ │ Actualización:                 20/10/2026 │ │
│ │            15/09 29/09 13/10 27/10 10/11                   │ │                                           │ │
│ │                                                            │ │ ℹ Cotización institucional de referencia  │ │
│ │ ℹ Serie histórica en base a rendimiento deportivo real.   │ │ calculada tras cada jornada oficial.      │ │
│ └────────────────────────────────────────────────────────────┘ └───────────────────────────────────────────┘ │
│                                                                                                             │
│ ┌─ HISTORIAL CRONOLÓGICO DE COTIZACIONES ─────────────────────────────────────────────────────────────────┐ │
│ │ Fecha        Cotización oficial    Variación vs. anterior    Rendimiento deportivo    Tipo actualización │ │
│ ├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ 20/10/2026   1,05 FM               ▲ +0,05 FM (+5,00 %)      0,60 pts                 Ajuste rendimiento │ │
│ │ 13/10/2026   1,00 FM               = 0,00 FM  (0,00 %)       —                        Sin variación      │ │
│ │ 06/10/2026   1,00 FM               Base de emisión           —                        Cotización inicial │ │
│ └─────────────────────────────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Componentes de la pestaña:
1. **Gráfico Histórico (`PlayerQuoteEvolutionChart`)**:
   - Gráfico SVG vectorial nativo interactivo con línea continua, puntos marcadores y área semitransparente.
   - Eje vertical con precios en créditos (`FM`) y eje horizontal con fechas de cotización (`DD/MM`).
   - Tooltips enriquecidos al interactuar con cada punto mostrando fecha, cotización y variación.
   - Tabla oculta accesible para lectores de pantalla.
2. **Panel Financiero Compacto (`PlayerQuotePanel`)**:
   - Tarjeta complementaria que resume el estado de valuación actual.
   - Explica de forma clara que el precio proviene del rendimiento deportivo del jugador.
3. **Historial Cronológico (`PlayerQuoteHistoryTable`)**:
   - Tabla accesible ordenada cronológicamente de la más reciente a la más antigua.
   - Muestra fecha, precio, variación absoluta/porcentual, score y estado en términos claros para el usuario (reemplazando tecnicismos como `RECOMPUTED`, `INITIAL` o `CARRIED_FORWARD` por «Ajuste por rendimiento», «Cotización inicial» o «Sin variación»).

---

## Pestaña 2: Resumen deportivo

Conserva íntegramente las 4 tarjetas complementarias y los paneles de gráficos de barras (`PlayerStatisticsBars`), desduplicadas contra la cabecera.

### Defensor

#### Cabecera (3 métricas titulares)
- **Entradas**: `tackles_per_match` (por partido)
- **Intercepciones**: `interceptions_per_match` (por partido)
- **Despejes**: `clearances_per_match` (por partido)

#### Resumen deportivo (4 tarjetas KPI + 2 paneles temáticos específicos)
```text
┌──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┐
│ [⭐] CALIF. WHOSCORED│ [⇮] DUELOS AÉREOS    │ [%] PRECISIÓN PASES  │ [🛡️] TIROS BLOQUEADOS                  │
│   7,14               │   3,2                │   86,48 %            │   0,8                                  │
│   promedio WhoScored │   ganados/partido    │   % acierto          │   bloqueos/partido                     │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─ Contención y duelos ─────────────────────────────┐ ┌─ Juego aéreo y disciplina ──────────────────────────┐ │
│ │ ✂️ Entradas intentadas                 3,0 veces  │ │ ⇮ Duelos aéreos perdidos                2,1 veces   │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🛡️ Pases bloqueados                   0,9 veces  │ │ 🚩 Faltas cometidas                     1,1 faltas  │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🚫 Veces superado por regate           0,6 veces  │ │ 🟨 Tarjetas amarillas                   5 tarjetas  │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🛡️ Centros bloqueados                 1,1 veces  │ │ 🟥 Tarjetas rojas                       0 tarjetas  │ │
│ └───────────────────────────────────────────────────┘ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Arquero

#### Cabecera (3 métricas titulares)
- **Atajadas**: `saves_per_match` (por partido)
- **Precisión de pases**: `pass_completion_percentage` (% acierto)
- **Pases largos acertados**: `successful_long_balls_per_match` (por partido)

#### Resumen deportivo (4 tarjetas KPI + 2 paneles temáticos específicos)
```text
┌──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┐
│ [⭐] CALIF. WHOSCORED│ [🛡️] APARICIONES     │ [🛡️] DESPEJES        │ [⚽] PASES TOTALES                     │
│   7,03               │   30                 │   1,8                │   28,4                                 │
│   promedio WhoScored │   partidos temporada │   despejes/partido   │   pases/partido                        │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─ Atajadas por sector ─────────────────────────────┐ ┌─ Distribución y juego con los pies ─────────────────┐ │
│ │ 🧤 Atajadas en el área chica           1,2 veces  │ │ ⊙ Pases por partido                    28,4 pases   │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🧤 Atajadas en el área penal           2,1 veces  │ │ ⇥ Pases cortos fallidos                 3,1 pases   │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🧤 Atajadas fuera del área             0,5 veces  │ │ ⇥ Pases largos fallidos                 4,0 pases   │ │
│ └───────────────────────────────────────────────────┘ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Mediocampista

#### Cabecera (3 métricas titulares)
- **Pases por partido**: `passes_per_match` (por partido)
- **Precisión de pases**: `pass_completion_percentage` (% acierto)
- **Pases clave**: `key_passes_per_match` (por partido)

#### Resumen deportivo (4 tarjetas KPI + 2 paneles temáticos específicos)
```text
┌──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┐
│ [⭐] CALIF. WHOSCORED│ [🤝] ASISTENCIAS     │ [✂️] ENTRADAS         │ [⚡] REGATES EXITOSOS                  │
│   7,04               │   0                  │   0,8                │   0,4                                  │
│   promedio WhoScored │   total temporada    │   entradas/partido   │   regates/partido                      │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─ Construcción y distribución ─────────────────────┐ ┌─ Posesión y equilibrio ─────────────────────────────┐ │
│ │ ⊙ Pases cortos acertados               5,12 pases │ │ ⚡ Pérdidas por desposesión              0,5 veces  │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ ⇥ Pases largos acertados                1,8 pases │ │ ⚠ Malos controles                       1,1 veces  │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ ⤤ Pases clave cortos                    0,2 pases │ │ ⇮ Duelos aéreos ganados                 0 duelos    │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ ⌖ Centros acertados                     0,4 centros││ 🚩 Faltas cometidas                     0,3 faltas  │ │
│ └───────────────────────────────────────────────────┘ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Delantero

#### Cabecera (3 métricas titulares)
- **Goles**: `goals_total` (total temporada)
- **Tiros por partido**: `shots_per_match` (por partido)
- **Asistencias**: `assists_total` (total temporada)

#### Resumen deportivo (4 tarjetas KPI + 2 paneles temáticos específicos)
```text
┌──────────────────────┬──────────────────────┬──────────────────────┬────────────────────────────────────────┐
│ [⭐] CALIF. WHOSCORED│ [🎯] TIROS AL ARCO   │ [⚽] GOLES           │ [⚡] REGATES EXITOSOS                  │
│   7,35               │   38                 │   0,6                │   2,3                                  │
│   promedio WhoScored │   tiros temporada    │   goles/partido      │   regates/partido                      │
├──────────────────────┴──────────────────────┴──────────────────────┴────────────────────────────────────────┤
│                                                                                                             │
│ ┌─ Definición por zona ─────────────────────────────┐ ┌─ Juego ofensivo y generación ───────────────────────┐ │
│ │ 🥅 Tiros desde el área penal            2,1 tiros │ │ ⤤ Pases clave                           1,5 pases   │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🥅 Tiros desde el área chica            0,3 tiros │ │ ⇮ Duelos aéreos ganados                 1,4 duelos  │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🥅 Tiros desde fuera del área           0,8 tiros │ │ ⚡ Pérdidas por desposesión              1,2 veces   │ │
│ │ ───────────────────────────────────────────────── │ │ ─────────────────────────────────────────────────── │ │
│ │ 🚩 Fueras de juego                      0,5 veces │ │ ⌖ Centros acertados                     0,6 centros │ │
│ └───────────────────────────────────────────────────┘ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Pestaña 3: Todas las estadísticas

Conserva el catálogo completo de 60 métricas organizadas en cards horizontales con CSS Grid responsive, ordenadas por relevancia según la posición del jugador, y la barra de controles compacta.

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ [ Buscar estadística por nombre...                ]   [Todas 60] [General 5] [Defensa 10] [Pases 10]...     │
│                                                       Leyenda: ● Principal  ○ Secundaria  · Complementaria  │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ GENERAL (5)                                                                                                 │
│ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐             │
│ │ Minutos        │ │ Apariciones    │ │ Titularidades  │ │ Calificación   │ │ Suplente       │             │
│ │ 2.430 min      │ │ 29 partidos    │ │ Sin datos      │ │ 7,14 puntos    │ │ 3 partidos     │             │
│ │ ● Principal    │ │ ● Principal    │ │ ● Principal    │ │ ● Principal    │ │ ○ Secundaria   │             │
│ └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘             │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ DEFENSA (10)                                                                                                │
│ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌──────────┐ │
│ │ Entradas       │ │ Intercepciones │ │ Despejes       │ │ Tiros bloquead.│ │ Regate superado│ │ Entradas…│ │
│ │ 2,4 /partido   │ │ 1,7 /partido   │ │ 4,1 /partido   │ │ 0,8 /partido   │ │ 0,6 /partido   │ │ 3,0 /p   │ │
│ │ ● Principal    │ │ ● Principal    │ │ ● Principal    │ │ ● Principal    │ │ ● Principal    │ │ ● Ppal   │ │
│ └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘ └──────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Adaptación responsive

### A. Escritorio ($1920 \times 913\text{ px}$ y desde $1280\text{ px}$)
- **Zona Superior Fija (209 px)**: Navegación ($32\text{ px}$) + Cabecera ($125\text{ px}$) + Separación ($12\text{ px}$) + Pestañas ($40\text{ px}$).
- **Región de Contenido Activa ($596\text{ px}$)**: `flex: 1; min-height: 0; overflow-y: auto`. Scroll vertical interno exclusivo. Cero scroll global en ventana.
- **Pestaña Cotizaciones**: Gráfico de cotización (65% ancho) y panel de auditoría (35% ancho) en la fila superior; tabla de historial cronológico a todo el ancho en la parte inferior.
- **Pestaña Resumen**: 4 tarjetas principales en fila, 2 paneles de barras lado a lado y estadísticas destacadas.
- **Pestaña Catálogo**: Grid responsive de 6 a 8 tarjetas por fila en 1920 px.

### B. Tablet (alrededor de 768 px a 1024 px)
- **Comportamiento vertical**: Conmuta a scroll documental natural de página (`overflow: visible; height: auto;`).
- **Cabecera (~155 px)**:
  - Fila superior: Foto ($70\text{ px}$) + Identidad a la izquierda; cotización oficial y variación en píldora compacta a la derecha.
  - Fila inferior: 3 micro-tarjetas deportivas a todo el ancho.
- **Pestaña Cotizaciones**:
  - Gráfico a todo el ancho en la parte superior.
  - Panel financiero en bloque apilado debajo.
  - Historial cronológico en tabla con scroll horizontal defensivo (`overflow-x: auto`).
- **Pestaña Resumen**: 4 tarjetas en cuadrícula $2 \times 2$; paneles de barras apilados verticalmente.
- **Pestaña Catálogo**: Grid responsive de 3 a 4 tarjetas por fila.

```text
┌─────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET                                         [=] │
├─────────────────────────────────────────────────────────────┤
│ Catálogo      Ranking                                       │
├─────────────────────────────────────────────────────────────┤
│ ← Volver a jugadores                                        │
├─────────────────────────────────────────────────────────────┤
│ ┌──────────┐ AARON ANSELMINO             ┌────────────────┐ │
│ │   FOTO   │ Chelsea FC · Premier League │ 1,05 FM +5,0%  │ │
│ │  (70px)  │ Defensor · Argentina        │ Cotización of. │ │
│ └──────────┘                             └────────────────┘ │
│ ┌─────────────┬───────────────────────────┬───────────────┐ │
│ │ ENTRADAS    │ INTERCEPCIONES            │ DESPEJES      │ │
│ │ 2,4 /p      │ 1,7 /p                    │ 4,1 /p        │ │
│ └─────────────┴───────────────────────────┴───────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ [ COTIZACIONES ]   [ RESUMEN DEPORTIVO ]   [ TODAS ]        │
├─────────────────────────────────────────────────────────────┤
│ GRÁFICO DE EVOLUCIÓN HISTÓRICA                              │
│  1,20 ┤          ╭───╮                                      │
│  1,05 ┤      ────╯   ╰──                                    │
│  1,00 ┤  ────╯                                              │
│       └──────┬─────┬─────┬─────┬─────┬                      │
│            13/10 20/10 27/10 03/11 10/11                    │
│                                                             │
│ RESUMEN FINANCIERO                                          │
│ • Cotización vigente: 1,05 FM    • Variación: +5,00 %       │
│ • Rendimiento: 0,60 pts          • Actualización: 20/10/26  │
│                                                             │
│ HISTORIAL DE COTIZACIONES                                   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 20/10/2026 │ 1,05 FM │ +5,00 % │ 0,60 pts │ Ajuste rend │ │
│ │ 13/10/2026 │ 1,00 FM │  0,00 % │ —        │ Sin cambios │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### C. Móvil (desde 360 px a 480 px)
- **Comportamiento vertical**: Desplazamiento vertical natural sin restricciones de altura fija.
- **Cabecera (~210 px)**:
  - Fila 1: Retrato ($60\text{ px}$) + Nombre y equipo.
  - Fila 2: Tarjeta destacada a todo el ancho con cotización oficial y variación porcentual.
  - Fila 3: Fila de 3 micro-tarjetas deportivas simétricas en una sola línea.
- **Pestañas**: Desplazamiento horizontal por deslizamiento (`overflow-x: auto; white-space: nowrap;`).
- **Pestaña Cotizaciones**:
  - Gráfico con `viewBox` responsive adaptado para pantallas estrechas.
  - Panel financiero en lista vertical compacta.
  - Historial en lista de tarjetas de fecha (cards) en lugar de tabla horizontal desbordante.
- **Pestaña Resumen**: Columna vertical única (tarjetas y barras a 1 columna).
- **Pestaña Catálogo**: Grid responsive de 1 a 2 columnas.

```text
┌──────────────────────────────────────┐
│ FOOTBALL MARKET                  [=] │
├──────────────────────────────────────┤
│ ← Volver a jugadores                 │
├──────────────────────────────────────┤
│ ┌──────┐ AARON ANSELMINO             │
│ │ FOTO │ Chelsea FC · Premier League │
│ └──────┘ Defensor · Argentina        │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ COTIZACIÓN OFICIAL: 1,05 FM      │ │
│ │ ▲ +5,00 % vs. anterior           │ │
│ └──────────────────────────────────┘ │
│ ┌───────────┬───────────┬──────────┐ │
│ │ ENTRADAS  │ INTERCEP. │ DESPEJES │ │
│ │ 2,4 /p    │ 1,7 /p    │ 4,1 /p   │ │
│ └───────────┴───────────┴──────────┘ │
├──────────────────────────────────────┤
│ [ COTIZACIONES ] [ RESUMEN ] [ TODAS]│ (Scroll horizontal)
├──────────────────────────────────────┤
│ EVOLUCIÓN HISTÓRICA                  │
│  1,20 ┤    ╭──╮                      │
│  1,05 ┤ ───╯  ╰──                    │
│       └────┬───┬──                   │
│          20/10 10/11                 │
│                                      │
│ RESUMEN DE COTIZACIÓN                │
│ • Cotización vigente: 1,05 FM        │
│ • Cotización anterior: 1,00 FM       │
│ • Variación: +0,05 FM (+5,00 %)      │
│ • Rendimiento base: 0,60 pts         │
│ • Fecha: 20/10/2026                  │
│                                      │
│ HISTORIAL DE COTIZACIONES            │
│ ┌──────────────────────────────────┐ │
│ │ 20/10/2026 · 1,05 FM             │ │
│ │ ▲ +5,00 % · Ajuste rendimiento   │ │
│ ├──────────────────────────────────┤ │
│ │ 13/10/2026 · 1,00 FM             │ │
│ │ = 0,00 % · Cotización inicial    │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

---

## Estados del Snapshot y Cotizaciones

### 1. Sin cotizaciones registradas en el mercado
Cuando un jugador recién registrado no cuenta con historial de cotizaciones (`quotes: []`), la cabecera y la pestaña presentan un estado claro y amigable sin romper la vista:
```text
┌──────────────────────────────────────────────────────────────┐
│ Este jugador aún no posee cotizaciones oficiales en el      │
│ mercado. Su precio inicial se registrará en la próxima       │
│ jornada de valuación.                                        │
└──────────────────────────────────────────────────────────────┘
```

### 2. Jugador con cotización única inicial
El gráfico SVG renderiza una línea plana de referencia o punto único centrado sin error de escala ni división por cero, indicando «Cotización inicial de emisión».

### 3. Estados de estadísticas (Carga, Sin datos, Parcial, Error)
Se preservan los estados informativos preexistentes para el rendimiento deportivo (`NO_DATA`, `PARTIAL`, errores con reintento), sin afectar la consulta independiente ni la disponibilidad de las cotizaciones oficiales.

---

## Límites

Este wireframe no autoriza selectores de temporada, radar, percentiles, comparación poblacional, xG/xA ni cotizaciones ficticias o de demostración. Los datos provienen exclusivamente de los contratos reales de backend en `PlayerQuoteController` (`GET /api/players/{id}/quotes`) y `PlayerStatisticsController` (`GET /api/players/{id}/statistics`).

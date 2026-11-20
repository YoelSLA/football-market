# Wireframe: Mis órdenes de venta en el mercado

**Estado**: diseño de alta fidelidad trading desk propuesto para la funcionalidad SPEC-030.
**Viewport de referencia**: `1920 × 913 px` (resolución desktop fija conforme a la Constitución §9.3.0).
**Ruta**: `/market/orders` bajo la sección `Mercado` (pestaña `market-orders`).
**Issue asociado**: [#105](https://github.com/YoelSLA/football-market/issues/105).
**Referencias normativas**: [`specs/030-my-orders/spec.md`](../../specs/030-my-orders/spec.md), [`specs/030-my-orders/contracts/my-orders-frontend.md`](../../specs/030-my-orders/contracts/my-orders-frontend.md) y [`docs/wireframes/transaction-history.md`](transaction-history.md).

La pantalla permite al usuario autenticado visualizar, filtrar y gestionar sus publicaciones de venta de tokens generadas en el mercado secundario (`TokenListing`), consumiendo `GET /api/listings/mine` y ejecutando cancelaciones mediante `POST /api/listings/{id}/cancel`. Los datos deportivos del jugador (nombre, escudo, club, posición e imagen) se enriquecen con el catálogo de jugadores vigente.

---

## 1. Composición desktop

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET                    Inicio   Jugadores   [Mercado]   Cartera               Usuario · 12.450,00 créditos │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ MERCADO             Mercado Oficial     Mercado P2P     Vender tokens     [Mis órdenes]                                │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                                        │
│ ┌────┐  MIS ÓRDENES DE VENTA             ┌─────── Órdenes activas ┐ ┌─────── Valor en venta ┐ ┌── Vendidas ┐ ┌── Cerradas ───┐ │
│ │ 🧩 │  Gestioná tus publicaciones en el │ [🏷️] 5 activas         │ │ [🪙] 4.850 créditos   │ │ [✅] 14     │ │ [✕] 5 cerradas │ │
│ └────┘  mercado secundario y cancelá…    └────────────────────────┘ └───────────────────────┘ └── 14 vendidas ┘ └── 5 cerradas ──┘ │
│                                                                                                                        │
│ ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐   │
│ │ 🔍 [ Buscar por jugador o club…     ]   [ Estado: Todas ▾ ]   [ Ordenar: Más recientes ▾ ]   [ Limpiar filtros ] │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ORDEN / REGISTRO    JUGADOR (ACTIVO)            CANTIDAD     PRECIO UNIT.     TOTAL IMPORTE   ESTADO      ACCIÓN │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ┌──────┐ 19/11/2026 ┌──────┐ LIONEL MESSI · del  5 tokens     1,50 cr.        7,50 créditos   ● ACTIVA   [✕ Cancelar]│
│ │ │ #104 │ 14:10 UTC  │ FOTO │ [🛡️] Inter M. · MLS              / token         $accent-high    Vence en 22h(hover:red)│
│ │ └──────┘            └──────┘                                                  (celeste)      (relativo)              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ┌──────┐ 19/11/2026 ┌──────┐ JULIÁN ÁLVAREZ · del10 tokens    2,20 cr.       22,00 créditos   ● ACTIVA   [✕ Cancelar]│
│ │ │ #98  │ 10:30 UTC  │ FOTO │ [🛡️] Atl. M. · LaLiga            / token         $accent-high    Vence en 18h           │
│ │ └──────┘            └──────┘                                                  (celeste)                              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ┌──────┐ 18/11/2026 ┌──────┐ ERLING HAALAND · del 4 tokens    3,10 cr.       12,40 créditos   ● VENDIDA       —     │   │
│ │ │ #89  │ 18:45 UTC  │ FOTO │ [🛡️] Man. C. · Premier           / token         $accent-high    Liquidada              │
│ │ └──────┘            └──────┘                                                  (celeste)                              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ┌──────┐ 17/11/2026 ┌──────┐ VINICIUS JR. · del   2 tokens    2,80 cr.        5,60 créditos   ○ CANCELADA     —     │   │
│ │ │ #76  │ 09:15 UTC  │ FOTO │ [🛡️] Real M. · LaLiga            / token         $text-muted     Retirada               │
│ │ └──────┘            └──────┘                                                                                     │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ┌──────┐ 14/11/2026 ┌──────┐ KYLIAN MBAPPÉ · del  8 tokens    3,50 cr.       28,00 créditos   ○ EXPIRADA      —     │   │
│ │ │ #62  │ 12:00 UTC  │ FOTO │ [🛡️] Real M. · LaLiga            / token         $text-muted     Expiró 15/11           │
│ │ └──────┘            └──────┘                                                                                     │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ Mostrando 1–5 de 24 órdenes                                                   [‹] [1] [2] [3] [4] [5] [›]        │   │
│ └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Anatomía de la fila trading desk

Cada fila de la tabla adopta un diseño inspirado en terminales financieras y exchanges de alta gama, con jerarquía visual rigurosa, componentes compartidos del sistema y tokens cromáticos vigentes.

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ ANATOMÍA DE FILA DE TRADING (Alto: 64 px | Fondo base: #071928 | Hover: #ffffff0a | Borde inferior: 1px solid #ffffff0a)    │
├───────────────┬──────────────────────────────┬──────────────┬──────────────┬───────────────┬──────────────┬──────────────────┤
│ ORDEN         │ JUGADOR (ACTIVO)             │ CANTIDAD     │ PRECIO UNIT. │ TOTAL IMPORTE │ ESTADO       │ ACCIÓN           │
│ (14%)         │ (30%)                        │ (10%)        │ (11%)        │ (13%)         │ (12%)        │ (10%)            │
├───────────────┼──────────────────────────────┼──────────────┼──────────────┼───────────────┼──────────────┼──────────────────┤
│ ┌────────┐    │ ┌────────┐ LIONEL MESSI      │     5        │   1,50 cr.   │ 7,50 créditos │ ┌──────────┐ │ ┌──────────────┐ │
│ │  #104  │    │ │        │ [del]             │   tokens     │   / token    │               │ │ ● ACTIVA │ │ │ ✕  Cancelar  │ │
│ └────────┘    │ │ PORTRAIT ───────────────── │              │              │ $accent-high  │ └──────────┘ │ └──────────────┘ │
│ 19/11/2026    │ │ 40×40  │ [🛡️] Inter Miami  │ font-weight: │ font-weight: │ (celeste)     │ ⏳ Vence 22h │ ghost-danger     │
│ 14:10 hs.     │ └────────┘ MLS               │ 700 tabular  │ 600 tabular  │ font-size:15px│ (relativo)   │ hover: solid red │
└───────────────┴──────────────────────────────┴──────────────┴──────────────┴───────────────┴──────────────┴──────────────────┘
```

### Especificaciones técnicas por celda

1. **Orden & Registro (`14%` | Izquierda)**:
   - **Ticket de orden**: El identificador `#104` se presenta en una píldora compacta con fondo `$surface-panel-to` (`#071a30`), borde `$border-panel-soft` (`#2b537859`) y texto `$text-soft-on-dark` (`#dbeafe`) en `12px`, `font-weight: 600`, cifras tabulares.
   - **Timestamp**: Fecha y hora en segunda línea `19/11/2026 · 14:10` en tipografía `$text-auxiliary-on-dark` (`#8fa2b4`), `11px`, `tabular-nums`.

2. **Jugador como Activo Subyacente (`30%` | Izquierda)**:
   - **Retrato deportivo**: Reutiliza el componente [`PlayerPortrait`](file:///home/yoelsla/Escritorio/football-market/frontend/src/shared/components/PlayerPortrait/PlayerPortrait.tsx) (`size="sm"` de `40×40 px`) con esquinas redondeadas (8 px), fondo `$surface-data-portrait` y micro-borde `$border-glass-faint`.
   - **Identidad deportiva**:
     - *Línea principal*: Nombre del futbolista en mayúsculas destacadas (`14px`, `font-weight: 700`, color `$text-title-on-dark` `#f2f6ff`), acompañado de un **badge de posición discreto**: micro-pill neutral (`font-size: 10px`, `font-weight: 600`, `padding: 1px 5px`, `border-radius: 3px`, fondo `$surface-data-control` `#05121e`, borde `$border-glass-faint`, texto `$text-dimmed-on-dark` `#9eafc9`). No compite visualmente con el nombre ni satura la fila.
     - *Línea secundaria*: Escudo del club mediante [`TeamBadge`](file:///home/yoelsla/Escritorio/football-market/frontend/src/shared/components/TeamBadge/TeamBadge.tsx) (`14×14 px`), nombre del equipo en `$text-dimmed-on-dark` (`#9eafc9`) + separador tenue `·` + liga en `$text-auxiliary-on-dark` (`#8fa2b4`), `12px`.

3. **Volumen de Tokens (`10%` | Derecha)**:
   - Cifra cuantitativa: `5` en `14px`, `font-weight: 700`, `tabular-nums`, color `$text-value-on-dark` (`#eef4ff`).
   - Sufijo de unidad: `tokens` en `11px`, `font-weight: 500`, color `$text-auxiliary-on-dark` (`#8fa2b4`), en línea o apilado compacto.

4. **Precio Unitario (`11%` | Derecha)**:
   - Cotización por token: `1,50 cr.` en `13px`, `font-weight: 600`, `tabular-nums`, color `$text-value-on-dark` (`#eef4ff`).
   - Subetiqueta: `/ token` en `11px`, color `$text-auxiliary-on-dark` (`#8fa2b4`).

5. **Total Importe — Celeste como Color Principal (`13%` | Derecha)**:
   - Tipografía: `15px`, `font-weight: 700`, `font-variant-numeric: tabular-nums;`.
   - **Color semántico**:
     - Órdenes `ACTIVE` y `SOLD`: Color **celeste** del sistema `$accent-highlight` (`#0dbbfa`), que unifica el lenguaje financiero de cotizaciones y volúmenes de Football Market sin interferir con la semántica del verde.
     - Órdenes `CANCELLED` y `EXPIRED`: Color neutral atenuado `$text-muted-on-dark` (`#94a3b8`) con `font-weight: 600` para indicar volumen inactivo o no devengado.
   - Sufijo `créditos` en `11px` con ligero desfase tipográfico para mantener la legibilidad numérica.

6. **Estado de la Orden — Verde Reservado para Positivo (`12%` | Centro)**:
   - **Trading Pill con indicador lumínico (`●`/`○`)**:
     - `ACTIVE` (Estado positivo): Fondo `$chip-recomputed-bg` (`#00c9881f`), borde `$chip-recomputed-border` (`#00c98859`), texto verde `$chip-recomputed-text` (`#00c988`). Etiqueta: `● Activa`. Subetiqueta inferior: **Texto relativo estático** (`Vence en 22h`, `Vence en 2 días`, `Vence hoy`), calculado en tiempo de renderizado a partir de `expiresAt` sin timers de segundo plano ni actualizaciones continuas.
     - `SOLD`: Fondo `$chip-carried-bg` (`#3b82f61f`), borde `$chip-carried-border` (`#3b82f659`), texto `$chip-carried-text` (`#60a5fa`). Etiqueta: `● Vendida`. Subetiqueta inferior: `Liquidada`.
     - `CANCELLED`: Fondo `#ff778b14`, borde `#ff778b40`, texto `$state-closed` (`#ff7388`). Etiqueta: `○ Cancelada`. Subetiqueta inferior: `Tokens devueltos`.
     - `EXPIRED`: Fondo `#fbbf2414`, borde `#fbbf2440`, texto `$state-caution` (`#fbbf24`). Etiqueta: `○ Expirada`. Subetiqueta inferior: `Vencida`.

7. **Acción Operativa (`10%` | Centro)**:
   - Para órdenes `ACTIVE`:
     - Botón `[✕ Cancelar]` con diseño **ghost-danger financiero**:
       - *Reposo*: Altura de `30 px`, `padding: 0 12px`, `border-radius: 6px`. Fondo `$surface-loss-subtle` (`#2d182240`), borde `1px solid $border-loss-emphasis` (`#ff778b59`), color `$state-closed` (`#ff7388`), icono `CircleX` de `13px` + texto `Cancelar` en `11px`, `font-weight: 600`.
       - *Hover*: Fondo `$state-error` (`#a32739`), borde `$state-negative` (`#f87171`), color `#ffffff`, sombra exterior `0 0 8px rgba(248, 113, 113, 0.3)`.
       - *Focus-visible*: Contorno accesible `2px solid $state-negative`, `outline-offset: 2px`.
       - *Active*: Transición de escala `transform: scale(0.97)`.
   - Para órdenes `SOLD`, `CANCELLED` y `EXPIRED`:
     - Guion tenue `—` en `$text-header-table` (`#7a91a6`), manteniendo la calma visual del panel.

---

## 3. Modal de confirmación de cancelación (`CancelListingModal`)

```text
┌─────────────────────────────────────────────────────────────┐
│ ✕ Cancelar publicación de venta                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ¿Estás seguro de que deseás retirar esta orden del mercado? │
│                                                             │
│   • Jugador: Lionel Messi (Inter Miami · MLS)               │
│   • Lote: 5 tokens                                          │
│   • Precio unitario: 1,50 créditos (Total: 7,50 créditos)   │
│                                                             │
│ Los 5 tokens reservados se liberarán y volverán a estar     │
│ disponibles de inmediato en tu cartera.                     │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                           [ Conservar orden ] [ Confirmar ] │
│                                               (danger solid)│
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Especificaciones dimensionales y Box Model

- **Viewport desktop**: `1920 px × 913 px` (fijo; sin scroll en la ventana global del navegador).
- **Navbar principal + Subnavegación**: `60 px + 48 px = 108 px`.
- **Área útil de pantalla**: `1920 px × 805 px`.
- **Contenedor de página (`PageLayout`)**: `padding: 8px`, espaciado vertical de `8 px` entre secciones.
- **Header unificado con métricas integradas**: Altura fija de `70 px`. Alberga a la izquierda el `PageHeading` y a la derecha las 4 métricas agregadas en pastillas estándar (altura de `58 px`, ancho mínimo `175 px`, icono en mosaico de 38×38 px, etiqueta y valor resaltado). Elimina la fila independiente de tarjetas para maximizar el área útil de la tabla.
- **Barra de filtros**: Altura fija de `56 px`, con inputs alineados en línea (búsqueda de texto, selector de estado, selector de orden y botón limpiar).
- **Tabla de órdenes**: Ocupa el espacio vertical restante (`flex: 1`, `min-height: 0`), con encabezado sticky y scroll vertical interno.
  - **Altura de fila**: `64 px` fija, permitiendo márgenes verticales simétricos de `12 px` para el retrato de 40 px y las dos líneas de datos.
  - **Interacción de fila**: `transition: background-color 0.15s ease`. Al posicionar el cursor sobre cualquier fila, el fondo adquiere `$surface-glass-subtle` (`#ffffff0a`), creando retroalimentación continua en la mesa de trading.
- **Paginación inferior**: `36 px` fija al pie del contenedor de la tabla.

---

## 5. Distribución de columnas de la tabla

| Columna | Ancho | Alineación | Rol de trading | Estilo y Tokens SCSS |
|---|---:|:---:|---|---|
| **Orden / Registro** | 14% | Izquierda | Identificación y timestamp | Pill `#id` en `$surface-panel-to` + fecha/hora en `$text-auxiliary-on-dark` (`11px`, tabular). |
| **Jugador (Activo)** | 30% | Izquierda | Activo negociable protagonista | `PlayerPortrait` 40×40, micro-pill de posición discreto, nombre en `$text-title-on-dark` (`14px`, 700), club + liga (`12px`). |
| **Cantidad** | 10% | Derecha | Volumen ofertado | Cifra en `$text-value-on-dark` (`14px`, 700, tabular) + unidad `tokens` (`11px`). |
| **Precio unitario** | 11% | Derecha | Cotización por token | Cifra en `$text-value-on-dark` (`13px`, 600, tabular) + `/ token` (`11px`). |
| **Total importe** | 13% | Derecha | Valor liquidable | Cifra en celeste `$accent-highlight` (`#0dbbfa`) para activas/vendidas (`15px`, 700, tabular); `$text-muted-on-dark` para cerradas. |
| **Estado** | 12% | Centro | Condición y vigencia | Trading pill con dot lumínico (`●`/`○`), verde `$chip-recomputed-text` exclusivo para `ACTIVE` y texto de vigencia estático relativo (`11px`). |
| **Acción** | 10% | Centro | Cancelación reactiva | Botón `Cancelar` ghost-danger (`30px` alto, hover rojo sólido con glow suave) para `ACTIVE`; guion `—` para el resto. |

---

## 6. Estados de la interfaz

1. **Estado de Carga (`Loading`)**: Skeletons en las pastillas métricas del header y 5 filas placeholder en la tabla durante el fetch de `GET /api/listings/mine`.
2. **Estado Vacío Absoluto (`Empty`)**: Si el usuario no tiene ninguna publicación registrada:
   - Se muestra contenedor centrado con icono de caja vacía, texto: *"Aún no tenés órdenes de venta publicadas"*, y botón de acción *"Publicar tokens"* que redirige a `/market/sell`.
3. **Estado Sin Resultados (`No Matches`)**: Si los filtros activos (búsqueda o estado) no producen coincidencias:
   - Mensaje centrado: *"No se encontraron órdenes que coincidan con los criterios seleccionados"*, con botón *"Limpiar filtros"*.
4. **Estado de Error (`Error`)**: Mensaje accesible de error de conexión con botón de reintento (`Reintentar consulta`).
5. **Estado de Mutación / Cancelación (`Cancelling`)**:
   - En el modal de cancelación, el botón de confirmación pasa a estado ocupado (`isBusy`, spinner o texto *"Cancelando..."*).
   - Al resolverse favorablemente, se dispara toast de notificación (`"Publicación #X cancelada con éxito. Tokens devueltos a tu tenencia"`), se cierra el modal y se invalidan las queries correspondientes (`marketQueryKeys.myListings`, `portfolioQueryKeys.allHoldings`).

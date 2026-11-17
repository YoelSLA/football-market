# Wireframe: Mercado Oficial y compra de tokens

**Estado**: rediseño desktop aprobado el 2026-11-13.
**Viewport de referencia**: `1920 × 913 px`.
**Ruta**: `/market/buy` bajo `Mercado`.

La pantalla consume el catálogo paginado y consolidado de `GET /api/market/official`. Los valores
son ilustrativos y representan únicamente ubicación y formato. La presencia de un jugador en el
catálogo no garantiza stock: el precio definitivo y la disponibilidad se validan al confirmar la
compra mediante `POST /api/orders/buy`.

## Composición desktop

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET        Inicio        Jugadores        [Mercado]        Cartera                         Usuario   Cerrar sesión │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Mercado                  [Mercado Oficial]          Mercado P2P          Vender tokens                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌────┐  MERCADO OFICIAL                                                            SALDO DISPONIBLE                       │
│ │ 🛒 │  Comprá tokens directamente de la emisión oficial.                           2.500,00 créditos                      │
│ └────┘  El precio y el stock se validan al confirmar la operación.                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ FILTROS                                                                                                    [Limpiar filtros]│
│ ┌─────────────────────────────┬─────────────────────────────┬─────────────────────────────┬─────────────────────────────┐   │
│ │ Buscar jugador              │ Liga                        │ Equipo                      │ Posición                    │   │
│ │ 🔍 Nombre o apellido…       │ Todas                     ▾ │ Todos                     ▾ │ Todas                     ▾ │   │
│ └─────────────────────────────┴─────────────────────────────┴─────────────────────────────┴─────────────────────────────┘   │
│ ┌─────────────────────────────┬─────────────────────────────┬─────────────────────────────┬─────────────────────────────┐   │
│ │ Nacionalidad                │ Precio mínimo               │ Precio máximo               │ Orden                       │   │
│ │ Todas                     ▾ │ 0,00 créditos               │ Sin máximo                  │ Precio: menor a mayor     ▾ │   │
│ └─────────────────────────────┴─────────────────────────────┴─────────────────────────────┴─────────────────────────────┘   │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 128 RESULTADOS                                                                                      Precio: menor a mayor   │
│                                                                                                                              │
│ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ │
│ │ ┌────────┐ [ARQ]           │ │ ┌────────┐ [DEF]           │ │ ┌────────┐ [MED]           │ │ ┌────────┐ [DEL]           │ │
│ │ │        │ David Raya      │ │ │        │ Ben White       │ │ │        │ M. Ødegaard     │ │ │        │ Bukayo Saka     │ │
│ │ │ FOTO   │ Arsenal FC      │ │ │ FOTO   │ Arsenal FC      │ │ │ FOTO   │ Arsenal FC      │ │ │ FOTO   │ Arsenal FC      │ │
│ │ └────────┘ Premier League  │ │ └────────┘ Premier League  │ │ └────────┘ Premier League  │ │ └────────┘ Premier League  │ │
│ │            Inglaterra     │ │            Inglaterra     │ │            Noruega        │ │            Inglaterra     │ │
│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │
│ │ PRECIO ACTUAL              │ │ PRECIO ACTUAL              │ │ PRECIO ACTUAL              │ │ SIN COTIZACIÓN              │ │
│ │ 1,20 créditos   ↘ −1,20 % │ │ 2,20 créditos   ↗ +4,80 % │ │ 3,40 créditos    → 0,00 % │ │                            │ │
│ │ Anterior: 1,22 créditos    │ │ Anterior: 2,10 créditos    │ │ Anterior: 3,40 créditos    │ │ No hay una cotización      │ │
│ │ Vigente: 12/11/2026        │ │ Vigente: 12/11/2026        │ │ Vigente: 12/11/2026        │ │ vigente para este jugador. │ │
│ │                            │ │                            │ │                            │ │                            │ │
│ │ [🛒 Comprar tokens]         │ │ [🛒 Comprar tokens]         │ │ [🛒 Comprar tokens]         │ │ [Compra no disponible]     │ │
│ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ │
│                                                                                                                              │
│ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ │
│ │ ┌────────┐ [DEF]           │ │ ┌────────┐ [MED]           │ │ ┌────────┐ [DEL]           │ │ ┌────────┐ [ARQ]           │ │
│ │ │ FOTO   │ W. Saliba       │ │ │ FOTO   │ D. Rice         │ │ │ FOTO   │ G. Martinelli   │ │ │ FOTO   │ K. Arrizabalaga │ │
│ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │
│ │            Premier League │ │            Premier League │ │            Premier League │ │            Premier League │ │
│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │
│ │ 3,00 créditos   ↗ +1,10 % │ │ 3,15 créditos   ↘ −0,60 % │ │ 3,55 créditos   ↗ +2,30 % │ │ 3,70 créditos    → 0,00 % │ │
│ │ Anterior: 2,97 créditos    │ │ Anterior: 3,17 créditos    │ │ Anterior: 3,47 créditos    │ │ Anterior: 3,70 créditos    │ │
│ │ Vigente: 12/11/2026        │ │ Vigente: 12/11/2026        │ │ Vigente: 12/11/2026        │ │ Vigente: 12/11/2026        │ │
│ │ [🛒 Comprar tokens]         │ │ [🛒 Comprar tokens]         │ │ [🛒 Comprar tokens]         │ │ [🛒 Comprar tokens]         │ │
│ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Mostrando 1–8 de 128                                       [‹] [1] [2] [3] [4] […] [16] [›]                               │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales de la página

Las medidas marcadas como **reales** proceden de los tokens y módulos SCSS vigentes. Las medidas
marcadas como **propuestas** definen el wireframe aprobado y todavía no existen en la
implementación. Todas las dimensiones utilizan `box-sizing: border-box`.

### Presupuesto del viewport

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Viewport de referencia | 1920 px fijo | 913 px fijo | Referencia aprobada del documento. |
| `AppLayout` | 1920 px adaptable al viewport | 913 px fijo al `100dvh` | **Real.** `overflow: hidden`; no agrega padding ni gap. |
| Navbar principal | 1920 px adaptable | 60 px mínimo | **Real.** Token `$navbar-top-height`; padding horizontal de 24 px y borde inferior de 1 px incluidos. |
| Navegación secundaria | 1920 px adaptable | 48 px mínimo | **Real.** Token `$navbar-tabs-height`; padding horizontal de 8 px y borde inferior de 1 px incluidos. |
| Navegación global total | 1920 px adaptable | 108 px mínimo | **Real.** Suma de los dos niveles. |
| Área disponible para la página | 1920 px adaptable | 805 px adaptable | **Real.** `913 − 108`; es el espacio restante del flex layout. |
| Página Mercado Oficial | 1920 px adaptable | 805 px adaptable | **Real en contención.** Padding de 8 px por cada lado; área interior propuesta: `1904 × 789 px`. |

### Distribución vertical propuesta a 1920 × 913 px

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Área interior de página | 1904 px adaptable | 789 px disponible | **Calculado.** La página conserva 8 px de padding en cada lado. |
| Encabezado compacto | 1904 px adaptable | 76 px fijo | **Propuesto.** Identidad a la izquierda y saldo a la derecha; sin total de resultados. |
| Separación encabezado–filtros | 1904 px | 8 px fijo | **Propuesto.** Gap vertical. |
| Contenedor de filtros | 1904 px adaptable | 187 px mínimo; adaptable | **Calculado.** Sin altura rígida; crece ante validaciones y no recorta contenido. |
| Separación filtros–resultados | 1904 px | 8 px fijo | **Propuesto.** Gap vertical. |
| Barra de resultados | 1904 px adaptable | 28 px fijo | **Propuesto.** Única ubicación del total y del orden activo. |
| Separación resultados–grilla | 1904 px | 8 px fijo | **Propuesto.** Gap vertical. |
| Región de grilla | 1904 px adaptable | 430 px flexible | **Propuesto.** Dos filas de cuatro; sin scroll vertical en el viewport aprobado. |
| Separación grilla–paginación | 1904 px | 8 px fijo | **Propuesto.** Gap vertical. |
| Paginación | 1904 px adaptable | 36 px fijo | **Real para `Pagination`; propuesto para su ubicación.** Fuera del scroll. |
| Total interior | 1904 px | 789 px | `76 + 8 + 187 + 8 + 28 + 8 + 430 + 8 + 36 = 789`; no produce overflow vertical. |

### Encabezado y filtros

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Icono del encabezado | 48 px fijo | 48 px fijo | **Propuesto.** Reduce el icono real actual de 64 px para mantener el header compacto. |
| Bloque título y descripción | mínimo 480 px; flexible | 48 px mínimo | **Propuesto.** Puede crecer; texto en dos líneas como máximo. |
| Bloque de saldo | 260 px fijo | 56 px fijo | **Propuesto.** Única métrica del encabezado; no compite con el título. |
| Padding del encabezado | 0 px horizontal | 10 px vertical | **Propuesto.** Incluido en los 76 px totales. |
| Interior de filtros | 1878 px adaptable | 161 px mínimo | **Calculado.** Padding de 12 px y borde incluidos en el contenedor. |
| Cabecera de filtros | 1878 px adaptable | 22 px mínimo | **Propuesto.** Título y acción «Limpiar filtros». |
| Primera fila | ancho adaptable | 57 px mínimo | **Propuesto.** Cuatro campos; gap horizontal de 16 px. |
| Buscar jugador | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** Label visible y control de 38 px. |
| Liga | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Equipo | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** Depende de liga. |
| Posición | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Nacionalidad | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Segunda fila | ancho adaptable | 57 px mínimo | **Propuesto.** Cuatro campos; gap horizontal de 16 px. |
| Precio mínimo | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Precio máximo | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Ordenamiento | 457–458 px adaptable | 57 px mínimo | **Propuesto a 1920.** |
| Control individual | 100% de su columna | 38 px fijo | **Aprobado.** Inputs y selects uniformes. |
| Separación entre filas | ancho adaptable | 12 px fijo | **Aprobado.** |

### Grilla y cards

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Grilla a cuatro columnas | 1904 px adaptable | 430 px flexible | **Propuesto desde 1440 px.** Cuatro columnas de 467 px con tres gaps de 12 px. |
| Grilla a tres columnas | ancho interior adaptable | 430 px flexible | **Propuesto entre 1100 y 1439 px.** Tres columnas; adaptación fuera del viewport de referencia. |
| Gap de grilla | 12 px fijo | 12 px fijo | **Propuesto.** Sustituye el gap real actual de `.85rem` (13,6 px) para cerrar el cálculo en píxeles enteros. |
| Card | 466–467 px adaptable | 209 px flexible | **Propuesto.** Dos filas ocupan `209 + 12 + 209 = 430 px`. |
| Padding de card | 14 px fijo por lado | 14 px fijo por lado | **Propuesto.** Próximo al valor real actual de `.85rem` (13,6 px). |
| Identidad deportiva | 438–439 px adaptable | 76 px fijo | **Propuesto.** Incluye retrato y textos. |
| Retrato | 68 px fijo | 72 px fijo | **Propuesto.** El real actual usa `clamp(70,4–86,4 px)` × `clamp(76,8–92,8 px)`. |
| Separación retrato–datos | 12 px fijo | — | **Propuesto.** Compatible con el gap real actual de `.85rem` (13,6 px). |
| Bloque de texto deportivo | mínimo 220 px; flexible | 72 px máximo | **Propuesto.** Nombre, equipo, liga y nacionalidad con elipsis cuando corresponda. |
| Separador interno | 438–439 px adaptable | 1 px fijo | **Propuesto.** No suma altura adicional fuera del flujo previsto. |
| Bloque de cotización | 438–439 px adaptable | 70 px mínimo | **Propuesto.** Precio, variación, anterior y vigencia; también reserva este alto para `quote: null`. |
| Precio actual | mínimo 150 px | 26 px fijo | **Propuesto.** Elemento principal del bloque financiero. |
| Variación | mínimo 90 px | 22 px fijo | **Propuesto.** Positiva, negativa o neutra. |
| Precio anterior y vigencia | 100% adaptable | 18 px cada línea | **Propuesto.** Dos líneas secundarias. |
| Acción de compra | 438–439 px adaptable | 34 px fijo | **Propuesto.** Real actual: padding vertical `.48rem` y contenido tipográfico; 34 px es coherente. |
| Card sin cotización | 466–467 px adaptable | 210 px mínimo | **Propuesto.** Conserva exactamente la misma caja que una card cotizada para evitar saltos de grilla. |

### Paginación y breakpoints

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Contenedor de paginación | 1904 px adaptable | 36 px fijo | **Propuesto.** Total a la izquierda y páginas centradas; no incluye selector de tamaño. |
| Texto «Mostrando…» | 260 px máximo | 20 px fijo | **Propuesto.** Se mantiene en una línea. |
| Componente `Pagination` | 372 px máximo; 100% adaptable | 36 px fijo | **Real.** Tres columnas internas: control de 36 px, páginas flexibles y control de 36 px; gaps de 8 px. |
| Botón anterior/siguiente | 36 px fijo | 36 px fijo | **Real.** Token CSS `--pagination-control-size`. |
| Botón de página | 36 px recomendado | 36 px fijo | **Real/adaptable.** La implementación compartida mantiene altura de 36 px. |
| Breakpoint de cuatro columnas | viewport ≥ 1440 px | — | **Propuesto.** Alineado con `$bp-grid-xl: 1439px`; mínimo de card ampliamente superior a 320 px. |
| Breakpoint de tres columnas | viewport 1100–1439 px | — | **Propuesto.** Alineado con `$bp-grid-lg: 1099px`; conserva aproximadamente 350 px o más por card. |
| Menos de tres columnas | viewport ≤ 1099 px | — | Fuera del wireframe desktop aprobado; requiere adaptación específica posterior. |

En el breakpoint de tres columnas, los filtros conservan dos filas pero reducen proporcionalmente
sus campos hasta los mínimos indicados. Si la suma de mínimos y gaps supera el ancho interior, la
adaptación debe cambiar la distribución de filtros antes de permitir overflow horizontal; no se
comprimen labels ni controles por debajo de esos mínimos.

## Distribución y comportamiento

- El encabezado es compacto y muestra únicamente identidad de página, explicación transaccional y
  saldo disponible. El total aparece una sola vez, inmediatamente sobre la grilla.
- Los ocho controles ocupan dos filas de cuatro: búsqueda, liga, equipo y posición; nacionalidad,
  precio mínimo, precio máximo y orden.
- Los filtros se aplican automáticamente. La búsqueda utiliza debounce; no existe botón Aplicar.
- El tamaño oficial queda fijado en `8` jugadores por página para mostrar dos filas de cuatro sin scroll vertical en el viewport aprobado.
- La grilla utiliza cuatro columnas cuando cada tarjeta conserva un ancho legible y baja a tres
  columnas en desktop reducido. La región de tarjetas absorbe el espacio vertical disponible.
- La paginación permanece fuera de la grilla y utiliza el tamaño fijo de ocho jugadores.

## Cards

Cada card muestra únicamente:

- retrato, posición, nombre, equipo, liga y nacionalidad;
- precio actual, variación porcentual, precio anterior y fecha de vigencia;
- acción de compra.

`quote: null` es un estado válido: conserva la identidad deportiva, muestra «Sin cotización» y
deshabilita la compra. La card no presenta score, multiplicador, origen ni fecha de cálculo.

## Modal de compra

```text
                                  ┌────────────────────────────────────────────────────────┐
                                  │ 🛒 COMPRAR TOKENS                                  [×] │
                                  │ Confirmá la compra al precio vigente.                  │
                                  ├────────────────────────────────────────────────────────┤
                                  │ ┌──────────┐ [ARQ]                                     │
                                  │ │   FOTO   │ David Raya                                │
                                  │ └──────────┘ Arsenal FC · Premier League · Inglaterra  │
                                  │                                                        │
                                  │ ┌────────────────────────┬───────────────────────────┐ │
                                  │ │ PRECIO ACTUAL          │ VARIACIÓN                 │ │
                                  │ │ 1,20 créditos          │ ↘ −1,20 %                 │ │
                                  │ │ Anterior: 1,22 cr.     │ Vigente: 12/11/2026       │ │
                                  │ └────────────────────────┴───────────────────────────┘ │
                                  │                                                        │
                                  │ Cantidad de tokens                                    │
                                  │                  [−] [   1   ] [+]                     │
                                  │                  [1] [5] [10] [50] [100]              │
                                  │                                                        │
                                  │ ┌────────────────────────────────────────────────────┐ │
                                  │ │ Precio por token                    1,20 créditos │ │
                                  │ │ Cantidad                                         1 │ │
                                  │ │────────────────────────────────────────────────────│ │
                                  │ │ IMPORTE TOTAL                      1,20 créditos │ │
                                  │ └────────────────────────────────────────────────────┘ │
                                  │                                                        │
                                  │ Precio y disponibilidad se validan al confirmar.       │
                                  ├────────────────────────────────────────────────────────┤
                                  │       [Cancelar]          [🛒 Confirmar compra]          │
                                  └────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales del modal

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Overlay | 1920 px fijo al viewport | 913 px fijo al viewport | **Real.** `position: fixed; inset: 0`; padding de 16 px y scroll vertical propio. |
| Diálogo reutilizable actual | 100% hasta 464 px | máximo 881 px | **Real.** `max-width: 29rem`; `max-height: calc(100dvh - 32px)`. |
| Diálogo del wireframe | 520 px máximo; 100% adaptable | 620 px objetivo; 720 px máximo | **Propuesto.** Requiere ampliar el máximo actual; queda centrado con 196,5 px libres arriba y abajo al alto objetivo. |
| Header del modal | 520 px adaptable | 72 px fijo | **Propuesto.** Padding horizontal de 18 px; incluye símbolo, textos y cierre. |
| Símbolo de compra | 38 px fijo | 38 px fijo | **Real aproximado.** El módulo actual usa `2.4rem` (38,4 px). |
| Botón cerrar | 42 px fijo | 42 px fijo | **Real aproximado.** El módulo actual usa `2.6rem` (41,6 px). |
| Contenido desplazable | 520 px adaptable | 476 px objetivo; flexible | **Propuesto.** Padding horizontal de 18 px, gap vertical de 10 px; puede desplazarse dentro del diálogo. |
| Resumen del jugador | 484 px adaptable | 84 px fijo | **Propuesto.** Padding de 8 px; retrato de 68 × 72 px y texto deportivo. |
| Panel de precio y variación | 484 px adaptable | 78 px fijo | **Propuesto.** Dos columnas de 237 px con gap de 10 px. |
| Columna de precio | 237 px adaptable | 78 px fijo | **Propuesto.** Precio actual y anterior. |
| Columna de variación | 237 px adaptable | 78 px fijo | **Propuesto.** Variación y vigencia; sin datos técnicos adicionales. |
| Fila de cantidad | 484 px adaptable | 42 px fijo | **Real/propuesto.** El control actual usa columnas `1fr 41,6px 76,8px 41,6px`; se redondean a `1fr 42px 77px 42px`. |
| Botón disminuir | 42 px fijo | 42 px fijo | **Real aproximado.** Modifica una unidad. |
| Campo cantidad | 77 px fijo | 42 px fijo | **Real aproximado.** Campo editable y foco inicial. |
| Botón aumentar | 42 px fijo | 42 px fijo | **Real aproximado.** Modifica una unidad. |
| Fila de cantidades rápidas | 484 px adaptable | 30 px fijo | **Propuesto.** Alineada a la derecha; cinco controles con gap de 6 px. |
| Botón rápido individual | 50 px mínimo | 30 px fijo | **Real aproximado.** El actual usa `min-width: 3.1rem` (49,6 px). Establece un valor absoluto. |
| Resumen de importe | 484 px adaptable | 104 px fijo | **Propuesto.** Padding de 12 px; dos filas de detalle, separador y total. |
| Aviso transaccional | 484 px adaptable | 28 px mínimo | **Propuesto.** Puede crecer a dos líneas sin exceder el máximo del modal. |
| Footer | 520 px adaptable | 72 px fijo | **Propuesto.** Padding `4px 18px 16px`; gap de 10 px. |
| Botón Cancelar | 190 px adaptable | 42 px fijo | **Propuesto.** Flex 1. |
| Botón Confirmar compra | 284 px adaptable | 42 px fijo | **Propuesto.** Flex 1,5; acción primaria. |
| Presupuesto vertical objetivo | 520 px | 620 px | `72 + 476 + 72 = 620`; no supera los 881 px reales disponibles dentro del overlay. |

El ancho propuesto de 520 px supera en 56 px el máximo real actual del componente compartido. Esta
diferencia queda documentada como una decisión de diseño pendiente de implementación, no como una
medida existente. Aun con el máximo propuesto de 720 px de alto, el overlay conserva 96,5 px libres
arriba y abajo en el viewport de referencia, por lo que no hay desbordamiento vertical. El contenido
mantiene `min-width: 0`, columnas flexibles y scroll interno para evitar overflow horizontal o que
variantes de error y éxito hagan crecer el documento.

Los botones rápidos establecen cantidades absolutas: pulsar `5` reemplaza el valor actual por `5`,
no suma cinco unidades. Los controles `−` y `+` modifican una unidad y comparten la validación del
campo editable. El modal no muestra score, multiplicador, origen ni fecha de cálculo.

## Estados

```text
CARGA:          Consultando el Mercado Oficial… + cards skeleton compactas
ERROR:          No pudimos cargar el Mercado Oficial. [Reintentar]
VACÍO:          No hay jugadores disponibles actualmente.
SIN RESULTADOS: No hay jugadores que coincidan con los filtros. [Limpiar filtros]
SIN COTIZACIÓN: Identidad deportiva + mensaje informativo + compra deshabilitada
```

Carga usa `role="status"`/`aria-busy`; error usa `role="alert"`; los estados vacíos son anunciables.
El modal conserva foco inicial en cantidad, navegación contenida, cierre accesible y restitución de
foco al botón que lo abrió.

# Wireframe: Mercado P2P y compra de publicaciones

Referencia funcional: `specs/019-buy-tokens-from-frontend/spec.md`, contratos de publicaciones de
SPEC-011 y contrato consumidor `specs/019-buy-tokens-from-frontend/contracts/buy-tokens-frontend.md`.
Referencia visual: [Mercado Oficial](official-market.md).

El documento representa el diseño aprobado para `/market/p2p`. No modifica contratos backend: el
catálogo se construye con todas las publicaciones comprables entregadas por `GET /api/listings`, se
agrupa y pagina localmente de a 12 jugadores, y la compra envía exclusivamente `listingId`.

## Catálogo desktop

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET                                [Jugadores] [Mercado] [Cartera]                             Usuario · Créditos │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ MERCADO             [🛒 Mercado Oficial] [👥 Mercado P2P] [🏷 Vender tokens]                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                                              │
│ ┌──────┐  MERCADO P2P                                                                  ┌────────────────────────────────┐  │
│ │  👥  │  Comprá lotes completos publicados por otros usuarios.                        │ SALDO DISPONIBLE              │  │
│ └──────┘  El precio y la disponibilidad se validan al confirmar.                        │ 12.450,00 créditos            │  │
│                                                                                          └────────────────────────────────┘  │
│                                                                                                                              │
│ ┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐  │
│ │ FILTROS                                                                                              [Limpiar filtros] │  │
│ │                                                                                                                        │  │
│ │ Buscar jugador o club                         Liga                     Posición                Orden                   │  │
│ │ [ Buscar...                              ]    [ Todas              ▾ ] [ Todas             ▾ ] [ Mejor precio      ▾ ] │  │
│ └────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                                                              │
│ 128 JUGADORES CON OFERTAS                                                                   Orden: mejor precio             │
│                                                                                                                              │
│ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ │
│ │ ┌────────┐ [ARQ]           │ │ ┌────────┐ [DEF]           │ │ ┌────────┐ [MED]           │ │ ┌────────┐ [DEL]           │ │
│ │ │ FOTO   │ David Raya      │ │ │ FOTO   │ Ben White       │ │ │ FOTO   │ M. Ødegaard     │ │ │ FOTO   │ Bukayo Saka     │ │
│ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │ └────────┘ Arsenal FC      │ │
│ │            Premier League │ │            Premier League │ │            Premier League │ │            Premier League │ │
│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │────────────────────────────│ │
│ │ PUBLICACIONES          4   │ │ PUBLICACIONES          2   │ │ PUBLICACIONES          5   │ │ PUBLICACIONES          3   │ │
│ │ TOKENS OFRECIDOS      18   │ │ TOKENS OFRECIDOS       7   │ │ TOKENS OFRECIDOS      22   │ │ TOKENS OFRECIDOS      12   │ │
│ │ MEJOR PRECIO               │ │ MEJOR PRECIO               │ │ MEJOR PRECIO               │ │ MEJOR PRECIO               │ │
│ │ 1,15 créditos / token      │ │ 2,05 créditos / token      │ │ 3,25 créditos / token      │ │ 3,40 créditos / token      │ │
│ │                            │ │                            │ │                            │ │                            │ │
│ │ [Ver 4 ofertas]            │ │ [Ver 2 ofertas]            │ │ [Ver 5 ofertas]            │ │ [Ver 3 ofertas]            │ │
│ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ │
│                                                                                                                              │
│ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ ┌────────────────────────────┐ │
│ │ …                          │ │ …                          │ │ …                          │ │ …                          │ │
│ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ └────────────────────────────┘ │
│                                                                                                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Mostrando 1–12 de 128                                      [‹] [1] [2] [3] [4] […] [11] [›]                               │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales del catálogo

Las medidas marcadas como **reales** proceden del frontend vigente. Las medidas **propuestas**
pertenecen al wireframe aprobado y todavía no describen necesariamente la implementación. Todas las
dimensiones usan `box-sizing: border-box`.

### Presupuesto del viewport

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Viewport de referencia | 1920 px fijo | 913 px fijo | Referencia compartida con el Mercado Oficial. |
| `AppLayout` | 1920 px adaptable | 913 px al `100dvh` | **Real.** Flex column y `overflow: hidden`. |
| Navbar principal | 1920 px adaptable | 60 px mínimo | **Real.** Token global vigente. |
| Navegación secundaria | 1920 px adaptable | 48 px mínimo | **Real.** Tabs de Mercado. |
| Navegación total | 1920 px adaptable | 108 px mínimo | **Real.** `60 + 48`. |
| Área disponible para página | 1920 px adaptable | 805 px adaptable | **Calculado.** `913 − 108`. |
| Página P2P | 1920 px adaptable | 805 px adaptable | **Real en contención.** Padding vigente de 8 px por cada lado. |
| Área interior | 1904 px adaptable | 789 px disponible | **Calculado.** Descuenta 8 px en cada uno de los cuatro lados. |

### Distribución vertical propuesta

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Encabezado compacto | 1904 px adaptable | 76 px fijo | **Propuesto.** Identidad y saldo; sin totales ni métricas sin respaldo. |
| Separación encabezado–filtros | 1904 px | 8 px fijo | **Propuesto.** |
| Contenedor de filtros | 1904 px adaptable | 94 px fijo | **Propuesto.** Incluye borde y padding de 8 px por lado. |
| Separación filtros–resultados | 1904 px | 8 px fijo | **Propuesto.** |
| Barra de resultados | 1904 px adaptable | 28 px fijo | **Propuesto.** Única ubicación del total y orden activo. |
| Separación resultados–grilla | 1904 px | 8 px fijo | **Propuesto.** |
| Región desplazable de grilla | 1904 px adaptable | 523 px flexible | **Propuesto.** `min-height: 0`; scroll vertical interno. |
| Separación grilla–paginación | 1904 px | 8 px fijo | **Propuesto.** |
| Paginación | 1904 px adaptable | 36 px fijo | **Real para el componente; propuesta para ubicación.** |
| Total interior | 1904 px | 789 px | `76 + 8 + 94 + 8 + 28 + 8 + 523 + 8 + 36 = 789`; sin overflow vertical. |

### Encabezado y filtros

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Icono del encabezado | 48 px fijo | 48 px fijo | **Propuesto.** Igual al Mercado Oficial. |
| Bloque título y descripción | mínimo 520 px; flexible | 48 px mínimo | **Propuesto.** Dos líneas como máximo. |
| Bloque de saldo | 260 px fijo | 56 px fijo | **Propuesto.** Única métrica del header. |
| Interior de filtros | 1888 px adaptable | 78 px | **Calculado.** Padding de 8 px en cada lado. |
| Cabecera de filtros | 1888 px adaptable | 22 px fijo | **Propuesto.** Título y «Limpiar filtros». |
| Separación interna | 1888 px | 6 px fijo | **Propuesto.** |
| Fila de filtros | 1888 px adaptable | 50 px fijo | **Propuesto.** Cuatro controles con gap de 12 px. |
| Búsqueda por jugador o club | 700 px adaptable | 50 px fijo | **Propuesto a 1920.** Mínimo 320 px. |
| Liga | 390 px adaptable | 50 px fijo | **Propuesto a 1920.** Mínimo 210 px. |
| Posición | 330 px adaptable | 50 px fijo | **Propuesto a 1920.** Mínimo 180 px. |
| Orden | 432 px adaptable | 50 px fijo | **Propuesto a 1920.** Mejor precio, mayor cantidad o nombre. |

Los controles son automáticos. El estado aprobado se sincroniza con `search`, `league`, `position`,
`sort` y `page` en la URL. `search` se publica después de 300 ms de debounce; `league`, `position` y
`sort` se actualizan inmediatamente. Cambiar cualquier filtro u orden vuelve a `page=0`. No existe
botón Aplicar, filtro por vendedor, precio editable, selector de tamaño ni parámetro backend inventado.

### Grilla y cards

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Grilla a cuatro columnas | 1904 px adaptable | 523 px flexible | **Propuesto desde 1440 px.** Cuatro columnas de 467 px y tres gaps de 12 px. |
| Grilla a tres columnas | ancho interior adaptable | 523 px flexible | **Propuesto entre 1100 y 1439 px.** Conserva cards legibles. |
| Gap de grilla | 12 px fijo | 12 px fijo | **Propuesto.** Coherente con Mercado Oficial. |
| Card P2P | 466–467 px adaptable | 246 px mínimo | **Propuesto.** Dos filas visibles ocupan `246 + 12 + 246 = 504 px`. |
| Padding de card | 14 px por lado | 14 px por lado | **Propuesto.** |
| Identidad deportiva | 438–439 px adaptable | 76 px fijo | **Propuesto.** Retrato, posición, nombre, equipo y liga. |
| Retrato | 68 px fijo | 72 px fijo | **Propuesto.** Reutiliza `PlayerPortrait`. |
| Separador interno | 438–439 px adaptable | 1 px fijo | **Propuesto.** |
| Bloque de agregados | 438–439 px adaptable | 92 px fijo | **Propuesto.** Publicaciones, tokens y mejor precio. |
| Acción «Ver ofertas» | 438–439 px adaptable | 34 px fijo | **Propuesto.** Etiqueta incluye cantidad de ofertas. |

Cada card se forma únicamente con información disponible: identidad deportiva resuelta desde el
catálogo de jugadores y agregados calculados sobre las publicaciones válidas. No muestra vendedor,
stock del emisor, cotización oficial, estadísticas ni cantidad editable.

### Paginación y breakpoints

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Contenedor de paginación | 1904 px adaptable | 36 px fijo | **Propuesto.** Total a la izquierda y controles centrados. |
| `Pagination` compartido | 372 px máximo; 100% adaptable | 36 px fijo | **Real.** Controles de 36 px y gaps de 8 px. |
| Tamaño de página | 12 jugadores fijo | — | **Aprobado.** Paginación local; no existe selector. |
| Cuatro columnas | viewport ≥ 1440 px | — | **Propuesto.** |
| Tres columnas | viewport 1100–1439 px | — | **Propuesto.** |
| Adaptación menor | viewport ≤ 1099 px | — | Debe reducir columnas antes de provocar overflow horizontal. |

## Detalle de publicaciones

El detalle reemplaza el área de filtros, catálogo y paginación dentro de la misma página. El header
compacto permanece estable y la URL adopta `/market/p2p?player=<id>`. Los filtros, orden y página se
conservan en los demás query params para restaurar el catálogo al volver, recargar o usar Back/Forward.

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET                                [Jugadores] [Mercado] [Cartera]                             Usuario · Créditos │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ MERCADO             [🛒 Mercado Oficial] [👥 Mercado P2P] [🏷 Vender tokens]                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌──────┐  MERCADO P2P                                                                  ┌────────────────────────────────┐  │
│ │  👥  │  Comprá lotes completos publicados por otros usuarios.                        │ SALDO DISPONIBLE              │  │
│ └──────┘                                                                                │ 12.450,00 créditos            │  │
│                                                                                          └────────────────────────────────┘  │
│                                                                                                                              │
│ [← Volver a jugadores con ofertas]                                                                                           │
│                                                                                                                              │
│ ┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐  │
│ │ ┌──────────┐ [ARQ]  DAVID RAYA                                          ┌────────────────────────────────────────────┐ │  │
│ │ │   FOTO   │        Arsenal FC · Premier League                         │ 4 publicaciones · 18 tokens              │ │  │
│ │ └──────────┘                                                            │ Mejor precio: 1,15 créditos / token      │ │  │
│ │                                                                         └────────────────────────────────────────────┘ │  │
│ └────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                                                              │
│ Las publicaciones se compran completas. La cantidad no puede modificarse.                                                    │
│                                                                                                                              │
│ 4 OFERTAS DISPONIBLES                                      Orden: precio, vencimiento e identificador                        │
│                                                                                                                              │
│ ┌──────────────┬──────────────────┬──────────────────┬──────────────────┬────────────────────┬─────────────────────────────┐ │
│ │ PUBLICACIÓN  │ CANTIDAD         │ PRECIO / TOKEN   │ TOTAL            │ VENCIMIENTO        │ ACCIÓN                      │ │
│ ├──────────────┼──────────────────┼──────────────────┼──────────────────┼────────────────────┼─────────────────────────────┤ │
│ │ #1842        │ 5 tokens         │ 1,15 créditos    │ 5,75 créditos    │ 2 h 18 min          │ [Comprar lote]              │ │
│ │ #1901        │ 3 tokens         │ 1,18 créditos    │ 3,54 créditos    │ 5 h 42 min          │ [Comprar lote]              │ │
│ │ #1756        │ 8 tokens         │ 1,20 créditos    │ 9,60 créditos    │ 38 min              │ [Comprar lote]              │ │
│ │ #1934        │ 2 tokens         │ 1,25 créditos    │ 2,50 créditos    │ Oferta vencida      │ [No disponible]            │ │
│ └──────────────┴──────────────────┴──────────────────┴──────────────────┴────────────────────┴─────────────────────────────┘ │
│                                                                                                                              │
│ La vigencia mostrada es orientativa. El servidor confirma disponibilidad, saldo y liquidación al comprar.                    │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales del detalle

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Área interior | 1904 px adaptable | 789 px disponible | **Calculado.** Igual al catálogo. |
| Encabezado compacto | 1904 px adaptable | 76 px fijo | **Propuesto.** Permanece estable. |
| Separación encabezado–regreso | 1904 px | 8 px fijo | **Propuesto.** |
| Acción de regreso | 320 px máximo | 40 px fijo | **Propuesto.** Retira solo `player` de la URL. |
| Separación regreso–resumen | 1904 px | 8 px fijo | **Propuesto.** |
| Resumen del jugador | 1904 px adaptable | 112 px fijo | **Propuesto.** Padding de 14 px; identidad y agregados. |
| Separación resumen–aviso | 1904 px | 8 px fijo | **Propuesto.** |
| Aviso de lote completo | 1904 px adaptable | 36 px fijo | **Propuesto.** No ofrece controles de cantidad. |
| Separación aviso–barra | 1904 px | 8 px fijo | **Propuesto.** |
| Barra de ofertas | 1904 px adaptable | 28 px fijo | **Propuesto.** Total y orden contractual. |
| Separación barra–lista | 1904 px | 8 px fijo | **Propuesto.** |
| Región desplazable de publicaciones | 1904 px adaptable | 457 px flexible | **Propuesto.** Completa `789 px`; scroll interno. |
| Fila de encabezados | 1904 px adaptable | 40 px fijo | **Propuesto.** Seis columnas. |
| Fila de publicación | 1904 px adaptable | 68 px mínimo | **Propuesto.** ID, cantidad, precio, total, vencimiento y acción. |
| Gap entre filas | 1904 px | 8 px fijo | **Propuesto.** |
| Columna publicación | 150 px fijo | 68 px | **Propuesto.** |
| Columna cantidad | 220 px adaptable | 68 px | **Propuesto.** |
| Columna precio unitario | 260 px adaptable | 68 px | **Propuesto.** |
| Columna total | 260 px adaptable | 68 px | **Propuesto.** |
| Columna vencimiento | 280 px adaptable | 68 px | **Propuesto.** Orientativo. |
| Columna acción | 300 px adaptable | 68 px | **Propuesto.** Botón de ancho completo. |

El detalle usa exclusivamente las publicaciones ya recibidas por `GET /api/listings`; no requiere
`GET /api/listings/{id}` para formar la vista. El orden inicial es precio unitario ascendente,
vencimiento ascendente e identificador ascendente. Una publicación vencida localmente se deshabilita
y dispara actualización, sin afirmar que el frontend sea autoridad de vigencia.

## Modal de confirmación

```text
                         ┌──────────────────────────────────────────────────────────────┐
                         │ 🛒 COMPRAR LOTE                                         [×] │
                         │ Confirmá la compra completa de esta publicación.             │
                         ├──────────────────────────────────────────────────────────────┤
                         │ ┌──────────┐ [ARQ]                                           │
                         │ │   FOTO   │ David Raya                                      │
                         │ └──────────┘ Arsenal FC · Premier League                     │
                         │                                                              │
                         │ Publicación #1842                                            │
                         │                                                              │
                         │ ┌──────────────────────────────────────────────────────────┐ │
                         │ │ Cantidad fija                              5 tokens      │ │
                         │ │ Precio por token                           1,15 créditos │ │
                         │ │ Vencimiento                                2 h 18 min    │ │
                         │ │──────────────────────────────────────────────────────────│ │
                         │ │ IMPORTE TOTAL                              5,75 créditos │ │
                         │ └──────────────────────────────────────────────────────────┘ │
                         │                                                              │
                         │ Saldo disponible                              12.450,00 cr.  │
                         │                                                              │
                         │ La disponibilidad y el saldo se validan al confirmar.         │
                         │ [Error o advertencia transaccional, cuando corresponda]       │
                         ├──────────────────────────────────────────────────────────────┤
                         │        [Cancelar]                 [Comprar lote completo]     │
                         └──────────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales del modal

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Overlay | viewport completo | viewport completo | **Real.** `position: fixed; inset: 0`, padding de 16 px y scroll vertical. |
| Diálogo compartido vigente | 100% hasta 464 px | máximo `100dvh − 32px` | **Real.** `shared/Modal` usa `max-width: 29rem`. |
| Clase P2P vigente | hasta 608 px solicitado | adaptable | **Real pero limitada.** Declara `38rem`, aunque el máximo compartido conserva 464 px. |
| Diálogo propuesto | 560 px máximo; 100% adaptable | 600 px objetivo; 720 px máximo | **Propuesto.** Requiere ampliar explícitamente el máximo permitido para esta variante. |
| Header | 560 px adaptable | 72 px fijo | **Propuesto.** Título, descripción y cierre. |
| Contenido desplazable | 560 px adaptable | 456 px objetivo | **Propuesto.** Padding horizontal de 18 px y gaps de 10 px. |
| Resumen del jugador | 524 px adaptable | 84 px fijo | **Propuesto.** Reutiliza `MarketPlayerSummary`. |
| Identificador de publicación | 524 px adaptable | 24 px fijo | **Propuesto.** No incluye vendedor. |
| Resumen del lote | 524 px adaptable | 150 px fijo | **Propuesto.** Cantidad, precio, vencimiento y total. |
| Cantidad | 252 px adaptable | 48 px fijo | **Propuesto.** Solo lectura. |
| Precio unitario | 252 px adaptable | 48 px fijo | **Propuesto.** |
| Vencimiento | 252 px adaptable | 48 px fijo | **Propuesto.** Orientativo. |
| Importe total | 524 px adaptable | 54 px fijo | **Propuesto.** Acción financiera principal. |
| Saldo disponible | 524 px adaptable | 28 px fijo | **Propuesto.** Solo si la consulta actual lo informa. |
| Aviso transaccional | 524 px adaptable | 36 px mínimo | **Propuesto.** Puede crecer por error. |
| Footer | 560 px adaptable | 72 px fijo | **Propuesto.** Dos acciones y gap de 10 px. |
| Botón Cancelar | 206 px adaptable | 42 px fijo | **Propuesto.** |
| Botón Comprar lote completo | 308 px adaptable | 42 px fijo | **Propuesto.** |

No existe campo de cantidad ni controles rápidos. Durante el envío, cierre, overlay, Escape y acción
primaria permanecen bloqueados. El handler aplica la misma guarda que el botón y no reintenta el POST
automáticamente.

## Estado de éxito

```text
                         ┌──────────────────────────────────────────────────────────────┐
                         │ ✓ COMPRA CONFIRMADA                                      [×] │
                         ├──────────────────────────────────────────────────────────────┤
                         │ Transacción #9284                                            │
                         │                                                              │
                         │ Jugador                                      David Raya      │
                         │ Tokens adquiridos                            5               │
                         │ Precio unitario                              1,15 créditos   │
                         │ Total ejecutado                              5,75 créditos   │
                         │ Ejecutada                                    13/11/2026 14:32│
                         │                                                              │
                         │                              [Volver a las ofertas]           │
                         └──────────────────────────────────────────────────────────────┘
```

## Especificaciones dimensionales del éxito

| Elemento | Ancho | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Diálogo de éxito | 560 px máximo; adaptable | 440 px objetivo | **Propuesto.** Misma variante visual del modal. |
| Header | 560 px adaptable | 72 px fijo | **Propuesto.** Estado confirmado y cierre. |
| Contenido | 524 px adaptable | 296 px objetivo | **Propuesto.** Identificador y cinco datos ejecutados. |
| Identificador de transacción | 524 px adaptable | 40 px fijo | **Propuesto.** |
| Fila de resultado | 524 px adaptable | 36 px mínimo | **Propuesto.** Jugador, cantidad, precio, total y ejecución. |
| Footer | 560 px adaptable | 72 px fijo | **Propuesto.** Una acción explícita. |
| Botón «Volver a las ofertas» | 260 px máximo | 42 px fijo | **Propuesto.** No hay autocierre. |

El éxito utiliza únicamente la respuesta real de compra: `transactionId`, `quantity`, `unitPrice`,
`totalAmount` y `executedAt`, más el nombre del jugador ya seleccionado. Permanece visible hasta que
el usuario lo cierre. Una interrupción sin respuesta concluyente nunca usa este estado.

## Estados de interfaz

```text
CARGA:          Cargando ofertas de usuarios…
ERROR:          No pudimos cargar las ofertas. [Reintentar]
MERCADO VACÍO:  No hay publicaciones disponibles. [Ir al Mercado Oficial]
SIN RESULTADOS: No hay jugadores que coincidan con los filtros. [Limpiar filtros]
SIN OFERTAS:    Este jugador ya no tiene publicaciones disponibles. [Volver]
VENCIDA:        Oferta vencida · compra deshabilitada · refresco del mercado
CONFLICTO:      La oferta dejó de estar disponible · datos actualizados
SALDO:          Créditos insuficientes · compra bloqueada preventivamente
PROCESANDO:     Compra en curso · cierre y segundo envío bloqueados
RED INCIERTA:   No pudimos confirmar el resultado · revisar operaciones antes de reintentar
```

| Estado | Caja | Alto | Observaciones |
| --- | ---: | ---: | --- |
| Carga de catálogo | 1904 px adaptable | 523 px flexible | **Propuesto.** `role="status"`; puede usar skeletons de cards. |
| Error de catálogo | 1904 px adaptable | 523 px flexible | **Propuesto.** `role="alert"` y acción Reintentar. |
| Mercado vacío | 1904 px adaptable | 523 px flexible | **Propuesto.** Acción al Mercado Oficial. |
| Sin resultados | 1904 px adaptable | 523 px flexible | **Propuesto.** Conserva filtros y ofrece limpiarlos. |
| Jugador sin ofertas | 1904 px adaptable | 457 px flexible | **Propuesto.** Dentro del detalle y con regreso al catálogo. |
| Error de compra | 524 px adaptable | 36 px mínimo | **Propuesto.** Dentro del modal; puede crecer a dos líneas. |
| Procesando | 560 px máximo | hasta 600 px | **Propuesto.** `aria-busy`; sin cierre ni doble envío. |
| Red incierta | 524 px adaptable | 56 px mínimo | **Propuesto.** No afirma resultado ni reintenta automáticamente. |

## Distribución y comportamiento

- El header replica la jerarquía del Mercado Oficial y elimina «Órdenes pendientes», dato que no
  existe en el contrato disponible.
- El total aparece únicamente sobre la grilla; el header muestra solo saldo.
- Los filtros son horizontales, automáticos y se sincronizan con la URL. El backend sigue recibiendo
  `GET /api/listings` sin parámetros; filtrado, orden y paginación se realizan localmente.
- Cada jugador aparece una sola vez. Los agregados se calculan exclusivamente con publicaciones
  válidas recibidas para la sesión.
- El detalle completo permanece en `/market/p2p?player=<id>` y conserva el resto de parámetros.
- La identidad del vendedor no se muestra ni se resuelve mediante otra API.
- Las publicaciones se compran completas. La cantidad, precio y total son solo lectura.
- El modal envía únicamente `{ listingId }`; el backend valida vigencia, saldo, stock y liquidación.
- El estado de éxito no se cierra automáticamente.
- Catálogo, detalle y modal reutilizan la cadena de altura y el scroll contenido de Mercado; ninguna
  variante debe producir overflow horizontal.

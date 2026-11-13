# Wireframe: historial personal de transacciones

**Estado**: rediseño desktop aprobado el 2026-11-12.
**Viewport de referencia**: `1920 × 913 px`.
**Ruta**: `/portfolio/transactions` bajo `Cartera`.

La pantalla consume el contrato paginado de SPEC-025. Los valores financieros son históricos; el
nombre, imágenes, equipo y liga pertenecen al perfil actual del catálogo. El jugador es informativo
y no navega.

## Composición desktop

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ FOOTBALL MARKET          Inicio   Jugadores   Mercado   [Cartera]                       60 px       │
├──────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Mi portfolio     Mis tenencias     [Historial de transacciones]                        48 px       │
├──────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌────┐  HISTORIAL DE TRANSACCIONES                                      124 operaciones              │
│ │ ↕  │  Revisá tus compras y ventas confirmadas.                                                     │
│ └────┘                                                                                               │
│ ┌─────────────────────┬─────────────────────┬─────────────────────┬──────────────────────────────┐   │
│ │ COMPRAS             │ VENTAS              │ VOLUMEN COMPRADO    │ VOLUMEN VENDIDO              │   │
│ │ 76 operaciones      │ 48 operaciones      │ 12.450,00 créditos  │ 8.920,00 créditos            │   │
│ │ 231 tokens          │ 109 tokens          │                     │                              │   │
│ └─────────────────────┴─────────────────────┴─────────────────────┴──────────────────────────────┘   │
│ ┌────────────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ [ Buscar jugador…        ] [ Todas ▾ ] [ Desde ] [ Hasta ] [ Más recientes ▾ ] [ Limpiar ]    │ │
│ ├────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ JUGADOR                      OPERACIÓN   CANTIDAD   PRECIO UNIT.   TOTAL          FECHA          │ │
│ ├────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ [img] Lionel Messi           [COMPRA]    5 tokens   1,25 créditos  6,25 créditos  10/11 14:30   │ │
│ │       Inter Miami · MLS · Transacción #92                                                        │ │
│ ├────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ [img] Julián Álvarez         [VENTA]     2 tokens   3,40 créditos  6,80 créditos  08/11 09:12   │ │
│ │       Atlético Madrid · LaLiga · Transacción #87                                                 │ │
│ │                                           ⋮                                                       │ │
│ ├────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ Mostrando 1–20 de 124                            [‹] [1] [2] [3] […] [7] [›]                    │ │
│ └────────────────────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Box Model y altura

- Navbar compartido: 108 px; área restante de referencia: 805 px.
- Página: `padding: 8px`, flujo vertical por contenido y separaciones máximas de 8 px.
- Encabezado, métricas y filtros conservan su altura natural.
- El panel de datos puede ocupar el espacio disponible; la región de filas desplaza verticalmente
  cuando hay datos suficientes.
- La región del historial ocupa el espacio vertical restante y mantiene el scroll interno de la
  tabla. Los estados sin filas centran una composición compacta dentro de esa misma región.
- Las filas nunca se estiran para completar la región y la paginación permanece alineada al borde
  inferior cuando corresponde.
- No se define adaptación específica para tablet o móvil.

## Columnas

```text
Jugador 31% | Operación 13% | Cantidad 12% | Precio 15% | Total 15% | Fecha 14%
```

Jugador agrupa `PlayerPortrait`, nombre, equipo, liga y transacción. Números usan cifras tabulares.
Compra usa acento verde; venta usa dorado, nunca rojo. Todos los colores proceden de tokens.

## Estados

```text
CARGA:          Consultando tu historial… + filas skeleton compactas
ERROR:          No pudimos cargar tu historial. [Reintentar]
VACÍO:          Todavía no tenés transacciones. [Ir al mercado]
SIN RESULTADOS: No hay operaciones que coincidan. [Limpiar filtros]
```

Carga usa `role="status"`/`aria-busy`; error `role="alert"`; vacíos son anunciables. Los controles
compartidos conservan foco visible.

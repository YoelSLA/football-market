# Wireframe: Mis órdenes de venta en el mercado

**Estado**: diseño propuesto para la funcionalidad SPEC-030.  
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
│ ┌────┐  MIS ÓRDENES DE VENTA                                                            24 órdenes registradas         │
│ │ 🧩 │  Gestioná tus publicaciones en el mercado secundario y cancelá ofertas activas.                                 │
│ └────┘                                                                                                                 │
│ ┌──────────────────────┬──────────────────────┬──────────────────────┬─────────────────────────────────────────────┐   │
│ │ ÓRDENES ACTIVAS      │ VALOR EN VENTA       │ ÓRDENES VENDIDAS     │ CERRADAS / EXPIRADAS                        │   │
│ │ 5 activas            │ 4.850,00 créditos    │ 14 vendidas          │ 5 cerradas                                  │   │
│ │ 32 tokens en venta   │ Importe en oferta    │ 84 tokens liquidados │ 3 canceladas · 2 expiradas                  │   │
│ └──────────────────────┴──────────────────────┴──────────────────────┴─────────────────────────────────────────────┘   │
│                                                                                                                        │
│ ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐   │
│ │ [ Buscar por jugador o club…     ]  [ Estado: Todas ▾ ]  [ Ordenar por: Más recientes ▾ ]  [ Limpiar filtros ]   │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ ORDEN / FECHA        JUGADOR                    CANTIDAD    PRECIO UNIT.   TOTAL IMPORTE   ESTADO       ACCIÓN   │   │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ #104                 [img] Lionel Messi         5 tokens    1,50 créditos  7,50 créditos   [ ACTIVA ]   [✕ Cancelar] │
│ │ 19/11/2026 14:10           Inter Miami · MLS                                               Vence en 22h              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ #98                  [img] Julián Álvarez       10 tokens   2,20 créditos  22,00 créditos  [ ACTIVA ]   [✕ Cancelar] │
│ │ 19/11/2026 10:30           Atlético Madrid · LaLiga                                        Vence en 18h              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ #89                  [img] Erling Haaland       4 tokens    3,10 créditos  12,40 créditos  [ VENDIDA ]  [—]          │
│ │ 18/11/2026 18:45           Manchester City · Premier                                       Vendida 18/11             │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ #76                  [img] Vinicius Jr.         2 tokens    2,80 créditos  5,60 créditos   [CANCELADA]  [—]          │
│ │ 17/11/2026 09:15           Real Madrid · LaLiga                                            Cancelada                 │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ #62                  [img] Kylian Mbappé        8 tokens    3,50 créditos  28,00 créditos  [ EXPIRADA ] [—]          │
│ │ 14/11/2026 12:00           Real Madrid · LaLiga                                            Expiró 15/11              │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   │
│ │ Mostrando 1–5 de 24 órdenes                                                   [‹] [1] [2] [3] [4] [5] [›]        │   │
│ └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Modal de confirmación de cancelación (`CancelListingModal`)

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
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Especificaciones dimensionales y Box Model

- **Viewport desktop**: `1920 px × 913 px` (fijo; sin scroll en la ventana global del navegador).
- **Navbar principal + Subnavegación**: `60 px + 48 px = 108 px`.
- **Área útil de pantalla**: `1920 px × 805 px`.
- **Contenedor de página (`PageLayout`)**: `padding: 8px`, espaciado vertical de `8 px` entre secciones.
- **Tarjetas de métricas**: Altura fija de `76 px`, 4 columnas distribuidas proporcionalmente con borde `1px solid var(--border-subtle)` y fondo semántico.
- **Barra de filtros**: Altura fija de `56 px`, con inputs alineados en línea (búsqueda de texto, selector de estado, selector de orden y botón limpiar).
- **Tabla de órdenes**: Ocupa el espacio vertical restante (`flex: 1`, `min-height: 0`), con encabezado sticky y scroll vertical interno cuando la lista de filas excede el alto del contenedor.
- **Paginación inferior**: `36 px` fija al pie del contenedor de la tabla.

---

## 4. Distribución de columnas de la tabla

| Columna | Ancho | Alineación | Descripción |
|---|---:|:---:|---|
| **Orden / Fecha** | 16% | Izquierda | Identificador `#listingId` y fecha/hora legible (`DD/MM/YYYY HH:mm`). |
| **Jugador** | 28% | Izquierda | Avatar/foto miniatura (36×36 px), nombre, equipo y liga. No interactivo. |
| **Cantidad** | 10% | Derecha | Cantidad de tokens ofertados (ej. `5 tokens`). |
| **Precio unitario** | 12% | Derecha | Precio por token expresado en créditos con dos decimales. |
| **Total importe** | 12% | Derecha | Importe total congelado (`unitPrice * quantity / 100`). |
| **Estado** | 12% | Centro | Badge semántico con texto explícito y fecha/instante de vencimiento o cierre. |
| **Acción** | 10% | Centro | Botón secundario/peligro `Cancelar` para `ACTIVE`; guion `—` para estados cerrados. |

---

## 5. Estados de la interfaz

1. **Estado de Carga (`Loading`)**: Skeletons en las 4 tarjetas de métricas y filas placeholder en la tabla durante el fetch de `GET /api/listings/mine`.
2. **Estado Vacío Absoluto (`Empty`)**: Si el usuario no tiene ninguna publicación registrada:
   - Se muestra contenedor centrado con icono de caja vacía, texto: *"Aún no tenés órdenes de venta publicadas"*, y botón de acción *"Publicar tokens"* que redirige a `/market/sell`.
3. **Estado Sin Resultados (`No Matches`)**: Si los filtros activos (búsqueda o estado) no producen coincidencias:
   - Mensaje centrado: *"No se encontraron órdenes que coincidan con los criterios seleccionados"*, con botón *"Limpiar filtros"*.
4. **Estado de Error (`Error`)**: Mensaje accesible de error de conexión con botón de reintento (`Reintentar consulta`).
5. **Estado de Mutación / Cancelación (`Cancelling`)**:
   - En el modal de cancelación, el botón de confirmación pasa a estado ocupado (`isBusy`, spinner o texto *"Cancelando..."*).
   - Al resolverse favorablemente, se dispara toast de notificación (`"Publicación #X cancelada con éxito. Tokens devueltos a tu tenencia"`), se cierra el modal y se invalidan las queries correspondientes (`marketQueryKeys.myListings`, `portfolioQueryKeys.allHoldings`).

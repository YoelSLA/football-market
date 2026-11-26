# Research: Tablero de tendencias de mercado y visualización de demanda comunitaria (SPEC-029)

**Feature**: `specs/029-frontend-market-trends-and-sentiment`  
**Fecha**: 2026-11-24  
**Objetivo**: Consolidar decisiones arquitectónicas, patrones de UI y estrategias de integración para el tablero de tendencias, badges de demanda en catálogos y desglose pedagógico en detalle de jugador.

---

## D-01: Hook y Gestión de Caché para Tendencias (`GET /api/market/trends`)

### Decisión
Crear el hook `useMarketTrendsQuery` en `frontend/src/features/market/hooks/queries/useMarketTrendsQuery.ts`, utilizando TanStack Query v5 con la clave `marketQueryKeys.trends()`.
- **Configuración de intervalos:**
  - `refetchInterval: 60000` (sondeo automático cada 60 segundos).
  - `refetchIntervalInBackground: false` (pausa el sondeo cuando la pestaña de la aplicación no está activa o visible).
  - `staleTime: 30000` (los datos se consideran frescos por 30 segundos, evitando peticiones duplicadas ante remonta de componentes).
- **Control manual:**
  - El hook expone `refetch` y `isFetching`, conectados directamente al botón de acción *"Actualizar"* en el encabezado de `MarketTrendsPage`.
- **Mapeo y normalización:**
  - Transformación estricta de importes en centésimas a créditos decimales (`buyVolume / 100`, `totalTradedCredits / 100`).

### Justificación
Cumple la clarificación formal acordada en la sesión `2026-11-24` (Opción A). Ofrece experiencia en tiempo real sin saturar el servidor ni consumir recursos en pestañas en segundo plano.

### Alternativas Descartadas
- *Solo manual (sin sondeo):* No entrega dinamismo para un tablero financiero de sentimiento.
- *WebSockets / Server-Sent Events:* Requiere infraestructura backend adicional innecesaria y compleja para la frecuencia de actualización de la plataforma.

---

## D-02: Navegación y Rutas del Tablero de Tendencias

### Decisión
- **Ruta:** `/market/trends` protegida bajo `AppLayout` en `AppRouter.tsx`.
- **Pestaña en Navbar:** Incorporar la pestaña `"Tendencias"` en la sección `market` de `frontend/src/app/navigation/sections.ts` con icono representativo (`TrendingUp` o `BarChart3`).
- **Ubicación:** Pestaña contigua a `"Mercado Oficial"`, `"Mercado P2P"`, `"Publicar venta"` y `"Mis órdenes"`.

### Justificación
Preserva la coherencia arquitectónica de `NAV_SECTIONS` donde cada pestaña de Mercado vive bajo `/market/*` y cuenta con persistencia de layout global.

---

## D-03: Renderizado y Semántica de la Barra de Sentimiento de Mercado

### Decisión
Diseñar el componente `MarketSentimentBar` como una barra visual horizontal estilizada mediante CSS Modules (`MarketSentimentBar.module.scss`) y SVG complementario:
- **División proporcional:** Dos segmentos dinámicos:
  - Verde Esmeralda (`#10b981` / token `$color-accent-green`): porcentaje de compras (`buyTransactionsPercentage`).
  - Rojo Rubí (`#ef4444` / token `$color-accent-red`): porcentaje de ventas (`sellTransactionsPercentage`).
- **Indicador de sesgo:**
  - Rótulo central o superior con chip semántico: `Alcista (BULLISH)`, `Bajista (BEARISH)` o `Neutral (NEUTRAL)`.
- **Accesibilidad:**
  - Elemento con `role="meter"` o `role="group"` y `aria-label="Sentimiento del mercado: X% compras, Y% ventas, estado Alcista/Bajista/Neutral"`.
- **Empty State:**
  - Si los porcentajes son nulos (ausencia de operaciones en 24h), se muestra una barra única en gris neutro con texto central *"Mercado en calma / Sin operaciones en las últimas 24 horas"*.

### Justificación
Evita dependencias de librerías de gráficos pesadas para una barra de dos segmentos y garantiza diseño ligero, responsivo y de accesibilidad accesible estándar (WCAG AA).

---

## D-04: Presentación de Activos Más Demandados (*Hot Assets*)

### Decisión
Diseñar el componente `MarketHotAssets` compuesto por una cuadrícula responsiva de tarjetas `HotAssetCard`:
- **Datos expuestos:** Puesto en ranking (#1 a #5), retrato/silueta del futbolista, nombre completo, club, cantidad de tokens adquiridos en la ventana y volumen acumulado en créditos (`¢`).
- **Interactividad:** La tarjeta completa funciona como enlace accesible (`<Link to={`/players/${playerId}`}>`) hacia el perfil individual del jugador.
- **Grilla:** En desktop 5 columnas (o grilla fluida `minmax(200px, 1fr)`), en tablet 2-3 columnas y en mobile 1 columna apilada.

### Justificación
Alineado con el lenguaje visual de `MarketPlayerCard` y `SorarePlayerCard` del design system, maximizando la familiaridad del usuario.

---

## D-05: Estrategia de Insignias *"🔥 Alta Demanda"* en Catálogos Oficial y P2P

### Decisión
- **Caché compartida:** Crear el selector/hook `useHotPlayerIds()` que consume `useMarketTrendsQuery` con `staleTime: 60000` y extrae un `Set<number>` con los `playerId` de `topHotPlayers`.
- **Componente insignia:** `HotDemandBadge` con diseño compacto: fuego naranja/oro (`🔥 Alta Demanda`) y tooltip explicativo al hover/focus.
- **Inyección no intrusiva:**
  - En `MarketPlayerCard.tsx` (catálogo oficial): renderiza `HotDemandBadge` sobre el contenedor de media o header de la tarjeta.
  - En `CommunityListingCard.tsx` (mercado P2P): renderiza `HotDemandBadge` junto al nombre o precio del activo.
- **Resiliencia ante fallos:** Si la consulta de tendencias falla o tarda, `hotPlayerIds` se evalúa como conjunto vacío y los catálogos continúan funcionando normalmente sin insignias.

### Justificación
Zero acoplamiento de red en los catálogos: consumen los datos ya cacheados en TanStack Query sin disparar peticiones redundantes por cada tarjeta renderizada.

---

## D-06: Desglose de Cotización en la Ficha del Jugador (`PlayerDetailPage`)

### Decisión
- **Enriquecimiento del modelo `PlayerQuote`:**
  - Mapear desde `QuoteResponseDTO` del backend los campos: `sportMultiplier`, `marketDemandFactor`, `marketMultiplier`, `marketNetDemandTokens` y `origin`.
- **Cálculo de componentes porcentuales:**
  - *Aporte Deportivo:* $(sportMultiplier - 1) \times 100$ cuando no es nulo, o $0.00\%$ si el origen es `MARKET_DEMAND`.
  - *Presión de Mercado:* $marketDemandFactor \times 100$ cuando está presente.
- **Componente UI:** `PlayerQuoteBreakdownPopover` en `PlayerOverviewQuote.tsx`:
  - Botón interactivo con icono informativo y etiqueta accesible *"Desglose"*.
  - Despliegue de popover/tooltip accesible que detalla:
    1. Aporte Deportivo (WhoScored): `+X.XX %` (con badge verde/rojo según signo).
    2. Presión de Mercado (Demanda): `+Y.YY %` (con badge verde/rojo según signo).
    3. Explicación pedagógica de la fórmula de cotización.

### Justificación
Proporciona la transparencia financiera requerida por el Sprint 2 y 3, reutilizando la infraestructura existente de `PlayerOverviewQuote` sin romper su estructura visual compacta.


# Research: UI de venta inmediata oficial y selector de precio flexible en P2P (SPEC-027)

Este documento consolida las investigaciones técnicas, decisiones de arquitectura y patrones de diseño adoptados para la implementación frontend de la **SPEC-027**.

---

## 1. Selector de Modalidad en el Modal de Venta (`SellTokenModal`)

### Contexto y Problema
Actualmente, `SellTokenModal` solo permite publicar órdenes en el mercado comunitario P2P (`POST /api/listings`). Con la introducción de la venta oficial al superusuario (buyback oficial con spread del 10%), el usuario debe poder elegir cómodamente entre vender al instante a la casa o publicar en la comunidad.

### Alternativas Evaluadas
1. **Dos modales separados**: Un modal específico para Venta Oficial y otro para Publicación P2P, disparados por botones independientes en la tarjeta del jugador.
   - *Desventaja*: Duplica lógica de presentación, modales, estado de holdings y cotizaciones.
2. **Segmented Control / Tab Switcher integrado en `SellTokenModal`** (*Elegida*): Un conmutador visual integrado en el encabezado del modal que alterna dinámicamente el cuerpo entre "Venta Inmediata Oficial" y "Publicación en Comunidad P2P".
   - *Ventaja*: Mantiene un único punto de entrada unificado, reutiliza el resumen del futbolista (`MarketPlayerSummary`), la consulta de cotización y tenencias (`SellModalHoldingInfo`), alternando únicamente el desglose financiero y el control de precio.

### Decisión
Implementar un control segmentado accesible (`SellModeSelector`) con `role="tablist"` y botones semánticos (`role="tab"`). Por defecto, el modal se inicializa en modalidad `"OFFICIAL"` (Venta Inmediata Oficial) conforme al valor prioritario de liquidez instantánea.

---

## 2. Control de Ajuste de Precio Flexible en P2P (Banda $\pm 20\%$)

### Contexto y Problema
El requerimiento exige permitir al usuario ajustar el precio unitario en P2P dentro de una banda de $[-20\%, +20\%]$ sobre la cotización oficial vigente, con recálculo en tiempo real del delta porcentual e importe total.

### Alternativas Evaluadas
1. **Librería de Slider externa (e.g. rc-slider, Radix UI Slider)**:
   - *Desventaja*: Introduce dependencias externas innecesarias, violando el Principio I de la Constitución (Simplicidad y calidad) y alterando el inventario tecnológico.
2. **Input numérico simple sin slider**:
   - *Desventaja*: Menor usabilidad táctil y dificultad para visualizar la posición relativa dentro del rango permitido.
3. **Control compuesto nativo HTML5 sincronizado (`<input type="range">` + `<input type="number">` + botones rápidos)** (*Elegida*):
   - *Ventaja*: Cero dependencias adicionales. Utiliza elementos HTML5 estándar con atributos `min`, `max` y `step`.
   - Incorpora botones de salto rápido: `-20%`, `-10%`, `0%`, `+10%`, `+20%`.
   - Inicializa en `-10%` (decisión acordada en CLARIFY) para estimular transacciones rápidas.
   - Actualización bidireccional inmediata de la cantidad en centésimas y créditos.

### Decisión
Diseñar el componente `P2PPriceSelector` en `frontend/src/features/market/pages/MarketSellPage/components/SellTokenModal/components/` utilizando `<input type="range">` sincronizado con un input numérico y badges de variación.

---

## 3. Badges de Oportunidad y Arbitraje en el Catálogo P2P

### Contexto y Problema
En `MarketP2PPage`, los compradores deben identificar de un vistazo las órdenes publicadas con descuento frente al precio oficial (`🏷️ -X% Oferta`) y aquellas con sobreprecio (`🔥 +X% Premium`).

### Alternativas Evaluadas
1. **Llamadas HTTP adicionales para cada listing**:
   - *Desventaja*: Efecto cascada $N+1$ de peticiones hacia `/players/{id}/quotes/current`.
2. **Cálculo en cliente a partir del cache de cotizaciones (`useMarketQuotesQuery`)** (*Elegida*):
   - *Ventaja*: Las cotizaciones oficiales vigentes ya son consultadas en lote por `useMarketQuotesQuery` o vienen anexadas en los datos del mercado.
   - Algoritmo de comparación en memoria:
     $$\Delta\% = \text{round}\left(\frac{P_{\text{p2p}} - P_{\text{oficial}}}{P_{\text{oficial}}} \times 100\right)$$
   - Si $\Delta\% < 0$: Badge verde esmeralda `🏷️ -X% Oferta`.
   - Si $\Delta\% > 0$: Badge naranja/rojo `🔥 +X% Premium`.
   - Si $\Delta\% = 0$ o no hay cotización: No se dibuja badge.

### Decisión
Crear el componente utilitario `P2POpportunityBadge` con estilos encapsulados en SCSS Modules, utilizando colores semánticos del design system (`$color-success` y `$color-warning` / `$color-danger`).

---

## 4. Patrón de Confirmación y Feedback Post-Venta

### Contexto y Problema
Al completar una venta inmediata oficial, los créditos se acreditan inmediatamente en la cuenta del usuario. La experiencia debe brindar certeza total del importe transferido sin forzar una recarga ni cerrar abruptamente la interacción.

### Decisión (Alineada con Clarifications)
- Mantener el diálogo abierto presentando el comprobante en `SellModalSuccess` con el detalle de la venta oficial (créditos acreditados, tokens vendidos y nuevo saldo).
- Emitir simultáneamente una notificación flotante global mediante `useMarketToast` (`notifySellSuccess`).
- Invalidar las queries de TanStack Query:
  - `marketQueryKeys.all` (para refrescar catálogo y listados).
  - `portfolioQueryKeys.all` (para actualizar tenencias y resumen patrimonial).
  - Balance de créditos del usuario (`useAuthStore` / headers).

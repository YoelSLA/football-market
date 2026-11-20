# Research: Dashboard interactivo y resumen ejecutivo de mercado (SPEC-031)

**Feature Branch**: `feat/031-home-dashboard`
**Fecha**: 2026-11-20
**Spec de referencia**: [`spec.md`](spec.md)

---

## 1. Decisiones Técnicas y Arquitectónicas

### Decisión 1: Endpoint Agregador Backend (`GET /api/dashboard/overview`) vs. Agregación en Frontend
- **Decisión adoptada**: Implementar un servicio y endpoint agregador dedicado en el backend (`footballmarket.features.dashboard`), que compile y entregue el resumen ejecutivo completo en una sola solicitud HTTP autenticada.
- **Justificación**:
  - *Rendimiento y latencia de red*: Sin este endpoint, el frontend requeriría realizar al menos 5 peticiones concurrentes desacopladas (`/api/portfolio/me`, `/api/users/me/transactions`, `/api/rankings/quote`, `/api/rankings/score`, `/api/listings`). En redes móviles o con latencia, esto provoca cascada de peticiones y renderizados descoordinados.
  - *Integridad matemática*: El cálculo del Patrimonio Neto (`netWorth`), los Top Movers (con sus deltas exactos) y las ofertas de arbitraje P2P se calculan directamente en la base de datos PostgreSQL mediante proyecciones optimizadas, evitando duplicar fórmulas financieras complejas en TypeScript.
  - *Consistencia de snapshot*: Todas las métricas del dashboard corresponden al mismo instante temporal de consulta.
- **Alternativas consideradas**:
  - *Opción B (Frontend-only composition)*: Descartada porque sobrecargaría el navegador con filtrados pesados de colecciones grandes (catálogo completo de cotizaciones para calcular deltas de gainers/losers y escaneo de todo el libro de órdenes P2P para detectar descuentos).

---

### Decisión 2: Reconstrucción de la Serie Temporal de Rendimiento (`GET /api/dashboard/performance`)
- **Decisión adoptada**: Reconstrucción determinista basada en el historial de transacciones (`market_transactions`) cruzado con el histórico de cotizaciones (`player_quotes`).
- **Justificación**:
  - La plataforma recalcula cotizaciones periódicamente (fechas efectivas registradas en `player_quotes`).
  - No existe una tabla de "snapshots diarios de saldo de usuario" precalculada. Reconstruir la tenencia neta acumulada en cada fecha pasada $\left(\sum \text{compras} - \sum \text{ventas}\right)$ y multiplicarla por la cotización de esa fecha permite graficar la evolución real del capital sin alterar el esquema de base de datos ni requerir un job batch histórico.
- **Alternativas consideradas**:
  - *Backtesting estático*: Multiplicar las tenencias actuales por el historial pasado. Descartada en la fase de Clarify porque distorsionaría la realidad de un usuario que vendió o rotó activos en el pasado.

---

### Decisión 3: Construcción del Benchmark "Football Market Index" (FMI)
- **Decisión adoptada**: Calcular el FMI como el promedio ponderado normalizado de la cotización de los jugadores del catálogo en cada fecha efectiva de recálculo:
  $$\text{FMI}_t = \frac{\sum_{j=1}^{N} P_{j,t}}{N}$$
  Normalizado a una base 100 para permitir comparar el crecimiento relativo de la cartera del usuario frente al mercado general.
- **Justificación**:
  - Emula los índices de referencia financieros tradicionales (S&P 500, MSCI World).
  - Permite al usuario saber si sus inversiones están "batiendo al mercado" o rindiendo por debajo del promedio.
- **Alternativas consideradas**:
  - *Benchmark contra el Top Holding del usuario*: Descartada porque no representa la salud del mercado en su conjunto.

---

### Decisión 4: Visualizaciones Gráficas en SVG Nativo vs. Librería Externa (Recharts / Chart.js)
- **Decisión adoptada**: Implementar los componentes gráficos (Donut de asignación, Área de benchmark temporal y Sparklines de tendencia) en **SVG nativo puro dentro de React 19**, siguiendo el patrón exitoso establecido en [`PlayerQuoteEvolutionChart.tsx`](../../../frontend/src/features/players/pages/PlayerDetailPage/components/PlayerQuoteEvolutionChart/PlayerQuoteEvolutionChart.tsx).
- **Justificación**:
  - *Compatibilidad y estabilidad*: React 19 y Vite 8 presentan incompatibilidades y warnings con varias librerías de gráficos basadas en versiones anteriores de React.
  - *Cero dependencias externas*: Cumple estrictamente con el **Principio I de la Constitución (Simplicidad y calidad)**: no agregar dependencias si las capacidades existentes bastan.
  - *Rendimiento y peso del bundle*: El código SVG nativo compila sin sobrecarga de runtime, logrando tiempos de renderizado inferiores a 16 ms (60 fps).
  - *Accesibilidad total (a11y)*: Control granular sobre atributos semánticos (`role="img"`, `aria-label`, `<desc>`) y exposición de tablas accesibles embebidas para lectores de pantalla.
- **Alternativas consideradas**:
  - *Instalar Chart.js o Recharts*: Descartada por incorporar decenas de dependencias transitivas y potenciales conflictos de tipado con React 19.

---

### Decisión 5: Política de Actualización y Caché (TanStack Query)
- **Decisión adoptada**: Utilizar TanStack Query con `staleTime: 30000` (30 segundos), `gcTime: 300000` (5 minutos) y `refetchOnWindowFocus: true`.
- **Justificación**:
  - Protege al servidor de ráfagas de consultas innecesarias mientras el usuario navega entre pestañas.
  - Asegura que si el usuario regresa de operar en el mercado oficial o P2P, los datos se sincronicen de inmediato al recuperar el foco de la ventana.
  - Elimina la necesidad de loops de polling continuo en segundo plano.


# Quickstart & Interactive Validation: SPEC-029

**Feature**: `specs/029-frontend-market-trends-and-sentiment`  
**Fecha**: 2026-11-24  
**Objetivo**: Guía de verificación interactiva y escenarios de validación funcional en el frontend.

---

## 1. Prerrequisitos y Arranque

1. Servidor backend de Football Market en ejecución en `http://localhost:8080`.
2. Servidor de desarrollo frontend en ejecución:
   ```bash
   cd frontend
   npm run dev
   ```
3. Usuario autenticado con sesión iniciada en `http://localhost:5173`.

---

## 2. Escenarios de Validación Funcional

### Escenario 1: Navegación y Visualización del Tablero de Tendencias
- **Pasos:**
  1. Abrir `http://localhost:5173`.
  2. En el Navbar, seleccionar la sección `"Mercado"` y hacer clic en la pestaña `"Tendencias"` (`/market/trends`).
- **Resultados esperados:**
  - La pantalla carga exhibiendo skeletons mientras resuelve la query.
  - La barra de sentimiento muestra visualmente la proporción verde (compras) vs rojo (ventas) y el chip de sesgo (`Alcista`, `Bajista` o `Neutral`).
  - Las tarjetas de métricas cuantitativas exhiben el volumen total en créditos (`¢`) con 2 decimales y el total de transacciones.
  - La grilla de Hot Assets despliega hasta 5 futbolistas con nombre, club, tokens comprados y volumen.
  - El botón manual *"Actualizar"* muestra el estado de carga (`isFetching`) al pulsarse y refresca los datos.

### Escenario 2: Empty State de Mercado en Calma
- **Pasos:**
  1. Si no hay operaciones registradas en las últimas 24 horas (`topHotPlayers: []`, `totalTransactions: 0`), acceder a `/market/trends`.
- **Resultados esperados:**
  - Se visualiza la tarjeta de `MarketTrendsEmptyState` con mensaje amigable: *"Mercado en calma: sin operaciones registradas en las últimas 24 horas"*.
  - La barra de sentimiento se renderiza en tono gris neutro sin fallar ni arrojar `NaN%`.

### Escenario 3: Badges de Alta Demanda en Catálogos
- **Pasos:**
  1. Identificar en la pestaña Tendencias cuáles futbolistas figuran en el Top 5 de más demandados.
  2. Navegar al Catálogo Oficial (`/market/buy`).
  3. Navegar al Mercado P2P (`/market/p2p`).
- **Resultados esperados:**
  - Las tarjetas correspondientes a dichos futbolistas muestran la insignia distintiva *"🔥 Alta Demanda"*.
  - Los futbolistas que no figuren en dicho Top 5 no muestran la insignia.
  - Al posar el cursor o enfocar la insignia, se despliega el tooltip contextual informativo.

### Escenario 4: Desglose de Cotización en Detalle de Jugador
- **Pasos:**
  1. En el ranking de Hot Assets o en el catálogo, hacer clic en la tarjeta de un jugador para ir a `/players/:id`.
  2. En la sección de cotización de `PlayerOverview`, localizar el botón de información o "Desglose".
  3. Hacer clic para desplegar el popover.
- **Resultados esperados:**
  - Se despliega el detalle de variación porcentual desglosando:
    - *Aporte Deportivo (WhoScored):* `+X.XX%`
    - *Presión de Mercado (Demanda):* `+Y.YY%`
    - *Variación Total:* `+Z.ZZ%`
  - Si la cotización fue por recálculo de mercado exclusivo (`MARKET_DEMAND`), el aporte deportivo muestra `0.00%` y la variación responde al componente de demanda.

---

## 3. Verificación de Compilación Frontend

Conforme a las reglas del repositorio, el agente no ejecuta tests de aplicación. El control ejecutable permitido es la verificación del build estático de frontend:

```bash
cd frontend && npm run build
```
*(Debe compilar limpiamente con 0 errores TypeScript y 0 fallos de empaquetado).*


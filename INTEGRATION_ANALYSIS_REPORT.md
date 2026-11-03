# Informe de Análisis de Integración Frontend-Backend - Football Market

## Resumen Ejecutivo

**Estado General: PARCIALMENTE INTEGRADO**

La aplicación presenta una integración funcional en autenticación, catálogo de jugadores y portfolio, pero tiene **problemas críticos de configuración de URLs** (falta prefijo `/api` en llamadas frontend) y **datos mockeados extensivos** en estadísticas de jugadores y cotizaciones. Además, faltan funcionalidades de mercado (listings, venta, órdenes) que el backend ya expone.

---

## 1. Problema Crítico: Configuración de Base URL

### Hallazgo
Todas las llamadas HTTP del frontend usan rutas relativas **sin el prefijo `/api`**, pero el backend expone todos sus endpoints bajo `/api/*`.

| Frontend (actual) | Backend (esperado) |
|-------------------|-------------------|
| `/auth/register` | `/api/auth/register` |
| `/auth/login` | `/api/auth/login` |
| `/auth/me` | `/api/auth/me` |
| `/players` | `/api/players` |
| `/players/{id}` | `/api/players/{id}` |
| `/portfolio/me` | `/api/portfolio/me` |
| `/players/{id}/quotes/current` | `/api/players/{id}/quotes/current` |
| `/orders/buy` | `/api/orders/buy` |

### Causa Raíz
- `frontend/src/infrastructure/http/http.ts`: `baseURL: import.meta.env.VITE_API_URL || undefined`
- README.md indica: `VITE_API_URL=http://localhost:8080` (SIN `/api`)
- Specs (005, 009, 004) indican: `VITE_API_URL=http://localhost:8080/api` (CON `/api`)

### Impacto
**Todas las llamadas fallan con 404** si `VITE_API_URL` no incluye `/api`. La aplicación no funciona contra el backend real sin configuración correcta.

### Archivos Afectados
- `frontend/src/infrastructure/http/http.ts` (línea 4)
- `frontend/README.md` (línea 44)
- `frontend/src/features/auth/auth.service.ts` (líneas 18, 23, 30)
- `frontend/src/features/players/players.service.ts` (líneas 10, 20)
- `frontend/src/features/portfolio/portfolio.service.ts` (línea 13)
- `frontend/src/features/market/market.service.ts` (líneas 22, 58)

### Prioridad: **CRÍTICA** - Bloquea toda la integración

---

## 2. Análisis por Funcionalidad

### 2.1 Autenticación (Login/Register/Current User)

| Aspecto | Estado |
|---------|--------|
| **Endpoints Backend** | ✅ Completos: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| **Servicio Frontend** | ✅ `auth.service.ts` implementado |
| **Hooks** | ✅ `useCurrentUserQuery`, mutations login/register |
| **Integración Real** | ⚠️ **Parcial** - Requiere fix de URL (`/api` prefix) |
| **DTOs/Contratos** | ✅ Coinciden (`LoginRequestDTO`, `RegisterRequestDTO`, `CurrentUserResponseDTO`, `LoginResponseDTO`) |

**Problemas:**
- Falta prefijo `/api` en todas las llamadas (ver sección 1)
- No hay manejo de refresh token (stateless JWT only)

---

### 2.2 Catálogo de Jugadores (Players Page)

| Aspecto | Estado |
|---------|--------|
| **Endpoints Backend** | ✅ `GET /api/players` (paginado), `GET /api/players/{id}` |
| **Servicio Frontend** | ✅ `players.service.ts` → `getPage()`, `getById()` |
| **Hooks** | ✅ `usePlayersQuery`, `usePlayerDetailQuery` |
| **Mapper** | ✅ `players.mapper.ts` → `toPlayersPage`, `toPlayerDetail` |
| **Integración Real** | ⚠️ **Parcial** - Requiere fix de URL |
| **DTOs/Contratos** | ✅ Coinciden (`PlayersPageResponseDTO`, `PlayerResponseDTO`) |

**Problemas:**
- Falta prefijo `/api` en llamadas (ver sección 1)
- Paginación: frontend usa `size=12` fijo, backend permite 1-100 (default 20) - **OK, compatible**

---

### 2.3 Detalle de Jugador (Player Detail Page)

| Aspecto | Estado |
|---------|--------|
| **Endpoint Backend - Básico** | ✅ `GET /api/players/{id}` |
| **Endpoint Backend - Cotizaciones** | ✅ `GET /api/players/{id}/quotes/current`, `GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=` |
| **Endpoint Backend - Estadísticas** | ❌ **NO EXISTE** endpoint para obtener estadísticas de un jugador específico |
| **Servicio Frontend - Básico** | ✅ `players.service.ts` → `getById()` |
| **Hooks Frontend - Demo Stats** | ❌ **MOCKEADO** - `usePlayerSampleSeasonQuery` usa `getPlayerSampleSeason()` (datos generados) |
| **Hooks Frontend - Demo Quotes** | ❌ **MOCKEADO** - `usePlayerTokenQuoteQuery` usa `getTokenQuoteSeries()` (datos generados) |
| **Componentes** | ❌ `PlayerQuoteEvolutionChart`, `PlayerQuotePanel`, `PlayerSportsSummary` consumen **solo datos demo** |

**Datos Mockeados Detectados:**
- `frontend/src/features/players/demo/playerSampleSeason.ts` - Genera estadísticas ficticias (partidos, goles, asistencias) basadas en hash determinista del playerId
- `frontend/src/features/players/demo/tokenQuoteSeries.ts` - Genera serie de 30 cotizaciones ficticias (40-180 créditos) basadas en hash determinista
- `frontend/src/features/players/hooks/queries/usePlayerSampleSeasonQuery.ts` - Consume demo
- `frontend/src/features/players/hooks/queries/usePlayerTokenQuoteQuery.ts` - Consume demo
- `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx` - Pasa `sampleSeason` y `tokenQuote` (demo) a componentes

**Endpoints Backend Disponibles NO Usados:**
- `GET /api/players/{id}/quotes/current` → Cotización vigente real
- `GET /api/players/{id}/quotes` → Historial completo de cotizaciones
- `GET /api/players/{id}/quotes?date=YYYY-MM-DD` → Cotización a fecha específica
- `GET /api/players/ranking` → Ranking por cotización vigente
- `GET /api/players/ranking/score` → Ranking por score

**Endpoints Backend Faltantes:**
- **No hay endpoint para estadísticas de rendimiento de un jugador específico** (matches, goals, assists, minutes). Solo existe `/api/players/statistics/whoscored/coverage` (cobertura de sync) y endpoints de sincronización.

**Prioridad: ALTA** - El detalle de jugador muestra **solo datos ficticios** para estadísticas y cotizaciones

---

### 2.4 Portfolio (Holdings & Summary)

| Aspecto | Estado |
|---------|--------|
| **Endpoints Backend** | ✅ `GET /api/portfolio/me` (con paginación, filtro status) |
| **Servicio Frontend** | ✅ `portfolio.service.ts` → `getPortfolio()` |
| **Hooks** | ✅ `usePortfolioHoldingsQuery`, `usePortfolioSummaryQuery` |
| **Mapper** | ✅ `portfolio.mapper.ts` → `toPortfolio` |
| **Integración Real** | ⚠️ **Parcial** - Requiere fix de URL |
| **DTOs/Contratos** | ✅ Coinciden (`PortfolioResponseDTO`, `PortfolioQueryParams`) |

**Problemas:**
- Falta prefijo `/api` (ver sección 1)
- El summary usa `getPortfolio({page:0, size:1})` - **ineficiente** pero funcional

---

### 2.5 Mercado - Compra de Tokens (Market Buy)

| Aspecto | Estado |
|---------|--------|
| **Endpoints Backend - Listings** | ✅ `GET /api/listings` (market), `GET /api/listings/mine`, `GET /api/listings/{id}`, `POST /api/listings/{id}/cancel` |
| **Endpoints Backend - Orders** | ✅ `POST /api/orders/buy` (compra directa y via listing), `POST /api/orders/sell` (publicar venta) |
| **Endpoints Backend - Quotes** | ✅ `GET /api/players/{id}/quotes/current` |
| **Servicio Frontend - Quotes** | ✅ `market.service.ts` → `getCurrentQuote()` llama `/players/{id}/quotes/current` |
| **Servicio Frontend - Buy** | ✅ `market.service.ts` → `buyTokens()` llama `/orders/buy` |
| **Hooks** | ✅ `useMarketQuotesQuery`, `useBuyTokensMutation` |
| **Integración Real** | ⚠️ **Parcial** - Requiere fix de URL en ambas llamadas |
| **DTOs/Contratos** | ✅ Coinciden (`OrderBuyRequestDTO`, `OrderBuyResponseDTO`, `QuoteResponseDTO`) |

**Problemas:**
- Falta prefijo `/api` en `getCurrentQuote` (`/players/...`) y `buyTokens` (`/orders/buy`)
- **No usa listings del mercado** - Solo compra directa al superusuario. El backend soporta compra via `listingId` pero frontend no lo implementa
- Filtros visuales en `MarketFilters` no conectados a backend (solo UI)

---

### 2.6 Mercado - Funcionalidades Faltantes (Backend Sí, Frontend No)

| Funcionalidad Backend | Endpoint | Estado Frontend |
|----------------------|----------|-----------------|
| **Ver listings del mercado** | `GET /api/listings` | ❌ No implementado |
| **Ver mis listings** | `GET /api/listings/mine` | ❌ No implementado |
| **Detalle de listing** | `GET /api/listings/{id}` | ❌ No implementado |
| **Cancelar mi listing** | `POST /api/listings/{id}/cancel` | ❌ No implementado |
| **Publicar venta (sell)** | `POST /api/orders/sell` | ❌ No implementado (nav tiene "market-sell" con path=null) |
| **Mis órdenes/historial** | `GET /api/users/me/transactions` | ❌ No implementado (nav tiene "market-orders", "market-history" con path=null) |

**Evidencia en navegación:** `frontend/src/app/navigation/sections.ts` líneas 57-60:
```typescript
{ key: "market-sell", label: "Vender tokens", icon: Tag, path: null },
{ key: "market-orders", label: "Mis órdenes", icon: Puzzle, path: null },
{ key: "market-history", ... path: null },
```

---

### 2.7 Ranking y Score

| Aspecto | Estado |
|---------|--------|
| **Endpoint Backend - Price Ranking** | ✅ `GET /api/players/ranking` |
| **Endpoint Backend - Score Ranking** | ✅ `GET /api/players/ranking/score` |
| **Frontend** | ❌ **No implementado** - No hay página ni componente de ranking |

---

### 2.8 Sincronización y Administración (Solo Backend)

| Funcionalidad | Endpoints Backend | Frontend |
|---------------|-------------------|----------|
| Sync Players | `POST /api/players/sync` | ❌ No UI |
| Sync Statistics (Scraping) | `POST /api/players/statistics/whoscored/sync/scraping` | ❌ No UI |
| Sync Statistics (JSON) | `POST /api/players/statistics/whoscored/sync/json` | ❌ No UI |
| Statistics Coverage | `GET /api/players/statistics/whoscored/coverage` | ❌ No UI |
| Initialize Quotes | `POST /api/quotes/initialize` | ❌ No UI |
| Recalculate Quotes | `POST /api/quotes/recalculate` | ❌ No UI |
| Quote Executions | `GET /api/quotes/executions` | ❌ No UI |
| Evaluate Valuations | `POST /api/valuations/evaluate` | ❌ No UI |
| Sync Images | `POST /api/players/images/sync` | ❌ No UI |
| Image Audit | `GET /api/players/images/sync-runs...` | ❌ No UI |

*Nota: Estas son funcionalidades de administración/sync que pueden no requerir UI de usuario final.*

---

## 3. Datos Estáticos Legítimos vs Mocks

### ✅ Datos Estáticos Legítimos (Configuración/Catálogos)
| Archivo | Propósito | Justificación |
|---------|-----------|---------------|
| `frontend/src/features/players/assets/teams/teams.json` | Logos de equipos mapeados por ID | Catálogo visual estático, no viene de API |
| `frontend/src/features/players/assets/teams/registry.json` | Registro de equipos con metadatos | Configuración visual, no datos dinámicos |
| `frontend/src/features/players/utils/getNationalityFlag.ts` | Banderas por código país | Utilidad visual estática |
| `frontend/src/features/players/utils/getTeamLogo.ts` | Resolución de logo por teamId | Mapeo estático de assets |
| `frontend/src/features/players/constants/playerClassifications.ts` | Clasificación liga/posición | Lógica de presentación |
| `frontend/src/features/portfolio/utils/format*.ts` | Formateadores (credits, %, datetime) | Utilidades de presentación |
| `frontend/src/features/players/utils/demoHash.ts` | Hash determinista para demo | **Es para demo, ver abajo** |

### ❌ Mocks que Deben Reemplazarse por Datos Reales
| Archivo/Lugar | Qué Simula | Endpoint Real Disponible |
|---------------|------------|--------------------------|
| `demo/playerSampleSeason.ts` | Estadísticas jugador (matches, goals, assists) | **NO EXISTE** endpoint individual |
| `demo/tokenQuoteSeries.ts` | Historial 30 días cotizaciones | `GET /api/players/{id}/quotes` |
| `usePlayerSampleSeasonQuery` | Hook que consume demo stats | N/A - Requiere endpoint nuevo |
| `usePlayerTokenQuoteQuery` | Hook que consume demo quotes | `GET /api/players/{id}/quotes/current` + `GET /api/players/{id}/quotes` |
| `PlayerQuoteEvolutionChart` | Gráfico evolución cotizaciones | Consumir `quotes` history real |
| `PlayerQuotePanel` | Panel cotización actual + variación | Consumir `currentQuote` real |
| `PlayerSportsSummary` | Resumen deportivo | Requiere endpoint stats real |

---

## 4. Estados de Carga, Error y Vacíos - Análisis

### Patrones Correctos (Integrados)
- `usePlayersQuery` / `usePlayerDetailQuery` - Usan `isPending`, `isError`, `isSuccess` de TanStack Query correctamente
- `usePortfolioHoldingsQuery` / `usePortfolioSummaryQuery` - Manejo correcto de estados
- `useMarketQuotesQuery` - Mapea estados granulares: `loading`, `available`, `missing`, `authentication-error`, `forbidden`, `network-error`, `http-error`, `invalid-response`

### Problemas en Estados (Demo/Mocks Ocultan Errores Reales)
| Comportamiento | Problema |
|----------------|----------|
| `usePlayerSampleSeasonQuery` - `staleTime: Infinity`, `gcTime: Infinity`, `retry: false` | **Nunca reintenta**, datos demo siempre "exitosos" - oculta que no hay endpoint real |
| `usePlayerTokenQuoteQuery` - `staleTime: Infinity`, `gcTime: Infinity`, `retry: false` | **Nunca reintenta**, datos demo siempre "exitosos" - oculta que endpoint real no se usa |
| `PlayerQuoteEvolutionChart` - Muestra "Datos de demostración" hardcoded | Usuario no sabe que son datos ficticios |
| `PlayerQuotePanel` - Muestra "Preparando cotización de demostración..." | Loading state ficticio, no llamada real |
| `DEMO_DATA_LABEL = "Datos de demostración"` en componentes | Label visible pero integración no existe |

### Fallbacks que Ocultan Problemas
- `useMarketQuotesQuery` línea 35-38: Retorna `null` en 404 `QUOTE_NOT_FOUND` en lugar de error - **Correcto** (jugador sin cotización)
- `market.service.ts` líneas 33-39: Maneja 404 como `null` correctamente
- PERO: `usePlayerTokenQuoteQuery` **nunca llama al backend**, así que nunca ve 404 real

---

## 5. Contratos HTTP - Verificación de Coincidencia

### ✅ Contratos que Coinciden
| Endpoint | Backend DTO | Frontend DTO/Mapper | Estado |
|----------|-------------|---------------------|--------|
| `GET /api/players` | `PlayersPageResponseDTO` | `PlayersPage` + `toPlayersPage` | ✅ |
| `GET /api/players/{id}` | `PlayerResponseDTO` | `PlayerDetail` + `toPlayerDetail` | ✅ |
| `GET /api/portfolio/me` | `PortfolioResponseDTO` | `Portfolio` + `toPortfolio` | ✅ |
| `POST /api/auth/login` | `LoginResponseDTO` | `AuthSession` + `toAuthSession` | ✅ |
| `GET /api/auth/me` | `CurrentUserResponseDTO` | `CurrentUser` + `toCurrentUser` | ✅ |
| `POST /api/orders/buy` | `OrderBuyResponseDTO` | `BuyResult` + `toBuyResult` | ✅ |
| `GET /api/players/{id}/quotes/current` | `QuoteResponseDTO` | `MarketQuote` + `toMarketQuote` | ✅ |

### ⚠️ Contratos Parciales / Diferencias
| Endpoint | Diferencia |
|----------|------------|
| `GET /api/players/{id}/quotes` | Backend devuelve `QuoteHistoryResponseDTO` con array; Frontend espera `TokenQuoteSeries` con `points[]` y `snapshot` - **Estructura distinta** |
| `GET /api/players/ranking` | Backend: `PlayerRankingResponseDTO`; Frontend: No implementado |

---

## 6. Plan de Corrección Priorizado

### 🔴 PRIORIDAD CRÍTICA (Bloqueadores de Integración)

| # | Tarea | Archivos | Esfuerzo |
|---|-------|----------|----------|
| 1 | **Fix VITE_API_URL** - Actualizar README y configuración para incluir `/api` | `README.md`, `frontend/.env.example` (crear), docs specs | 30 min |
| 2 | **Agregar prefijo `/api` a todas las llamadas frontend** O usar `VITE_API_URL=http://localhost:8080/api` | `http.ts` (baseURL), o `.env.local` correcto | 30 min |

> **Nota:** La solución más limpia es configurar `VITE_API_URL=http://localhost:8080/api` y mantener rutas relativas sin `/api` en services. Verificar que CORS en backend permite `/api/**` (ya está en `SecurityConfig.java` línea 138).

---

### 🟠 PRIORIDAD ALTA (Datos Mockeados en Funcionalidades Core)

| # | Tarea | Archivos | Esfuerzo |
|---|-------|----------|----------|
| 3 | **Reemplazar `usePlayerTokenQuoteQuery` demo por llamadas reales** | `usePlayerTokenQuoteQuery.ts`, `market.service.ts` (añadir `getQuoteHistory`), `PlayerQuoteEvolutionChart`, `PlayerQuotePanel` | 2-3h |
| 4 | **Implementar endpoint backend para estadísticas de jugador** O decidir no mostrar stats | Backend: nuevo endpoint en `PlayerController` o `PlayerStatisticsController`; Frontend: `usePlayerSampleSeasonQuery` | 4-6h (backend) + 2h (frontend) |
| 5 | **Si no hay endpoint stats backend: eliminar UI de stats o marcar claramente como "no disponible"** | `PlayerSportsSummary`, `PlayerOverview`, `usePlayerSampleSeasonQuery` | 1-2h |

---

### 🟡 PRIORIDAD MEDIA (Funcionalidades de Mercado Faltantes)

| # | Tarea | Archivos | Esfuerzo |
|---|-------|----------|----------|
| 6 | **Implementar página "Mis Listings" (Vender/Cancelar)** | Nuevo: `MarketSellPage`, `MarketListingsPage`, hooks, services para `/api/listings/*`, `/api/orders/sell` | 4-6h |
| 7 | **Implementar compra via Listing (no solo directa)** | `market.service.ts` (añadir `buyListing`), `BuyTokenModal`, `useBuyTokensMutation` | 2-3h |
| 8 | **Implementar Historial de Operaciones** | Nueva página, hook para `GET /api/users/me/transactions` | 2-3h |
| 9 | **Conectar filtros MarketFilters a backend** (si backend soporta) | `MarketFilters`, `MarketBuyRoute`, `usePlayersQuery` params | 1-2h |

---

### 🟢 PRIORIDAD BAJA (Nice to Have)

| # | Tarea | Esfuerzo |
|---|-------|----------|
| 10 | Página de Ranking (Price + Score) | 2-3h |
| 11 | UI de sincronización (admin) | 3-4h |
| 12 | Documentar decisión sobre stats de jugador (endpoint nuevo vs quitar UI) | 30 min |

---

## 7. Matriz de Estado de Integración por Funcionalidad

| Funcionalidad | Estado | % Real Backend | Mocks | Fix URL Requerido |
|---------------|--------|----------------|-------|-------------------|
| **Auth - Register** | Parcial | 100% | No | ✅ Sí |
| **Auth - Login** | Parcial | 100% | No | ✅ Sí |
| **Auth - Current User** | Parcial | 100% | No | ✅ Sí |
| **Players - List** | Parcial | 100% | No | ✅ Sí |
| **Players - Detail (básico)** | Parcial | 100% | No | ✅ Sí |
| **Players - Detail (stats)** | **Mockeado** | 0% | ✅ Sí (demo) | ✅ Sí |
| **Players - Detail (quotes)** | **Mockeado** | 0% | ✅ Sí (demo) | ✅ Sí |
| **Portfolio - Summary** | Parcial | 100% | No | ✅ Sí |
| **Portfolio - Holdings** | Parcial | 100% | No | ✅ Sí |
| **Market - Buy (direct)** | Parcial | 100% | No | ✅ Sí |
| **Market - Quotes (real-time)** | Parcial | 100% | No | ✅ Sí |
| **Market - Listings (market)** | **Faltante** | 0% | N/A | ✅ Sí |
| **Market - My Listings** | **Faltante** | 0% | N/A | ✅ Sí |
| **Market - Sell** | **Faltante** | 0% | N/A | ✅ Sí |
| **Market - History** | **Faltante** | 0% | N/A | ✅ Sí |
| **Ranking - Price** | **Faltante** | 0% | N/A | ✅ Sí |
| **Ranking - Score** | **Faltante** | 0% | N/A | ✅ Sí |

---

## 8. Conclusiones y Recomendaciones

### Conclusión Principal
La aplicación **parece funcional en desarrollo** porque los mocks de demo (`playerSampleSeason`, `tokenQuoteSeries`) generan datos deterministas y visualmente completos. **Pero no consume datos reales del backend** para:
1. Estadísticas de rendimiento del jugador
2. Historial y cotización vigente de tokens (en detalle de jugador)
3. Listings del mercado, venta, historial de operaciones

### Recomendación Inmediata
1. **Configurar `VITE_API_URL=http://localhost:8080/api`** en `.env.local` (o crear `.env.example`)
2. **Verificar que todas las llamadas funcionan** contra backend real
3. **Decidir estrategia para estadísticas de jugador**: 
   - Opción A: Crear endpoint `GET /api/players/{id}/statistics` en backend
   - Opción B: Quitar UI de stats y mostrar "No disponible"
4. **Reemplazar hooks demo por llamadas reales** a `/api/players/{id}/quotes/*`

### Deuda Técnica Identificada
- `docs/TECHNICAL_DEBT.md` debería registrar: "Falta endpoint estadísticas jugador individual", "Mocks en PlayerDetail ocultan falta de integración real", "Funcionalidades de mercado (sell, listings, history) no implementadas en frontend"

---

## Apéndice: Archivos Clave para Referencia

### Frontend Services (Puntos de Llamada HTTP)
- `frontend/src/features/auth/auth.service.ts`
- `frontend/src/features/players/players.service.ts`
- `frontend/src/features/portfolio/portfolio.service.ts`
- `frontend/src/features/market/market.service.ts`

### Frontend Hooks Demo (Mockeados)
- `frontend/src/features/players/hooks/queries/usePlayerSampleSeasonQuery.ts`
- `frontend/src/features/players/hooks/queries/usePlayerTokenQuoteQuery.ts`
- `frontend/src/features/players/demo/playerSampleSeason.ts`
- `frontend/src/features/players/demo/tokenQuoteSeries.ts`

### Backend Controllers (Endpoints Reales)
- `backend/src/main/java/footballmarket/controllers/AuthenticationController.java`
- `backend/src/main/java/footballmarket/controllers/PlayerController.java`
- `backend/src/main/java/footballmarket/controllers/PortfolioController.java`
- `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java`
- `backend/src/main/java/footballmarket/controllers/ListingController.java`
- `backend/src/main/java/footballmarket/controllers/OrderController.java`
- `backend/src/main/java/footballmarket/controllers/OperationHistoryController.java`
- `backend/src/main/java/footballmarket/controllers/ScoreRankingController.java`

### Configuración
- `frontend/vite.config.ts` (sin proxy)
- `frontend/src/infrastructure/http/http.ts` (baseURL)
- `backend/src/main/java/footballmarket/config/SecurityConfig.java` (CORS `/api/**`)
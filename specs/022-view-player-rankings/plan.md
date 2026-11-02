# Implementation Plan: Visualizar ranking de jugadores (SPEC-022)

**Branch**: `feat/022-view-player-rankings` | **Date**: 2026-10-27 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `specs/022-view-player-rankings/spec.md`

## Summary

SPEC-022 habilita la vista de **Ranking de jugadores** en el frontend, accesible mediante la ruta `/players/ranking` y la pestaña *Ranking* de la barra secundaria de la sección Jugadores. La funcionalidad permite al usuario alternar entre dos modalidades visuales oficiales extraídas de los mockups normativos:
1. **Por rendimiento**: consume `GET /api/players/ranking/score`, exhibiendo la estrategia activa de valuación, la fecha de corrida y la clasificación de jugadores por score a 6 decimales.
2. **Por cotización**: consume `GET /api/players/ranking`, exhibiendo el precio vigente del token en el mercado, su vigencia y badge de origen (`RECOMPUTED`/`CARRIED_FORWARD`).

La arquitectura visual implementa un **Podio Top 3** superior con laureles jerárquicos (Oro #1 centro, Plata #2 izquierda, Bronce #3 derecha) y una **Tabla detallada paginada** (10 filas por defecto) con barra de filtrado reactivo del lado cliente (búsqueda por nombre, selector de liga y selector de posición deportiva), garantizando la conservación de la numeración oficial del ranking en todo momento. Los datos identificatorios del jugador (foto, escudo, bandera de liga, demarcación) se enriquecen instantáneamente cruzando con el catálogo cacheado de `useAllPlayersQuery()`.

---

## Technical Context

**Language/Version**: TypeScript 7.0.2 en modo estricto; React 19.3.0; Node 24.19.0 (`.nvmrc`).

**Primary Dependencies**: React 19.3.0 · React Router DOM 7.18.4 · TanStack Query 5.103.1 · Axios 1.20.0 · Sass 1.104.1 · Lucide React 1.59.0 · Vite 8.3.0 · Biome 2.5.14. **Cero dependencias nuevas**: reutilización integral del stack existente.

**Storage**: No aplica almacenamiento local persistente propio. La sesión reside en `infrastructure/storage` y el estado remoto se almacena y deduplica en la memoria de TanStack Query (`staleTime: 30000`).

**Testing**: **No aplica.** El frontend se encuentra temporalmente exento de testing según la Constitución (*Verificación y testing*) y `frontend/AGENTS.md` («Por el momento no crear tests de frontend ni incorporar infraestructura para ellos»). La verificación técnica se realiza mediante `npm run build` en `frontend/` y la validación manual guiada por [`quickstart.md`](quickstart.md).

**Target Platform**: Navegador web moderno (escritorio y móvil), con adaptabilidad responsiva verificada entre 320 px y 1920 px de ancho.

**Project Type**: Single Page Application (SPA) en React/TypeScript consumiendo la API REST de Spring Boot.

**Performance Goals**: Una sola consulta HTTP por modalidad (`score` o `ranking`) con caché de 30 s; la alternancia no repite tráfico innecesario. Hidratación en memoria en $O(1)$ usando mapa indexado del catálogo de jugadores. DOM ultraligero (< 25 nodos principales en pantalla) mediante paginación cliente de 10 elementos.

**Constraints**: Sin librerías externas de UI o componentes de tablas. Estilos exclusivamente con SCSS Modules y roles semánticos de `styles/abstracts/themes/_default.scss`. Trazabilidad estricta sin alterar contratos del backend ni ejecutar tests prohibidos.

**Scale/Scope**: 1 página principal (`PlayerRankingsPage`), 5 subcomponentes de presentación (`RankingsHeader`, `RankingsPodium`, `RankingsFilters`, `RankingsTable`, `RankingsPagination`), 2 funciones de servicio, 2 mappers de transformación, 2 query hooks y 1 hook de página (`usePlayerRankingsPage`), 1 ruta nueva y activación de 1 pestaña en navegación.

---

## Constitution Check

*GATE: Evaluado antes del diseño y re-evaluado post-diseño.*

| Principio | Verificación | Resultado |
| --- | --- | --- |
| **§I Simplicidad y calidad** | Cero dependencias añadidas. Sin sobre-ingeniería: componentes atómicos enfocados con responsabilidades únicas y limpias. No se crea un store global redundante ni librerías pesadas. | ✅ |
| **§II Proporcionalidad del alcance** | El alcance responde exactamente a `spec.md` y a los mockups normativos. El menú de tres puntos ('...') fue evaluado y excluido formalmente para evitar alcance innecesario. | ✅ |
| **§III Seguridad e integridad** | La autenticación se delega al token JWT administrado por la infraestructura del cliente. No se manejan datos sensibles en local storage. | ✅ |
| **§IV Coherencia de contratos** | Se consumen fielmente los contratos de SPEC-018 (`score` y `ranking`) documentados en [`contracts/player-rankings-consumption.md`](contracts/player-rankings-consumption.md) sin alteraciones ni suposiciones no documentadas. | ✅ |
| **§V Trazabilidad y fuente de verdad** | SPEC-022 ↔ US1-US3 ↔ Pull Request [#76](https://github.com/YoelSLA/football-market/pull/76) ↔ Rama `feat/022-view-player-rankings`. Los estados del proceso se conservan en artefactos. | ✅ |
| **Verificación y testing** | El agente no ejecuta tests. Frontend exento: no se crean tests automáticos. Verificación mediante `npm run build` y validación de `quickstart.md`. | ✅ |
| **Idioma** | Código, identificadores, DTOs y tipos en inglés. Documentación, comentarios y textos de interfaz en español con tuteo amigable. | ✅ |
| **Autorizaciones humanas** | Este plan prepara la arquitectura técnica; no autoriza implementación inmediata. La implementación requiere Checklists, Tasks, Analyze y autorización humana explícita. | ✅ |

**Complexity Tracking**: Cero violaciones normativas. No requiere registro de excepciones.

---

## Project Structure

### Documentation (this feature)

```text
specs/022-view-player-rankings/
├── plan.md              # Este documento (/speckit.plan)
├── spec.md              # Especificación funcional aprobada (Specify + Clarify)
├── research.md          # Fase 0: Decisiones técnicas e investigación
├── data-model.md        # Fase 1: Estructuras de datos, DTOs y estado
├── quickstart.md        # Fase 1: Guía de validación de extremo a extremo
├── contracts/
│   └── player-rankings-consumption.md  # Contrato HTTP de consumo
└── checklists/
    └── requirements.md  # Checklist de calidad (16/16 aprobado)
```

### Source Code Planned Layout

```text
frontend/src/
├── app/
│   ├── navigation/
│   │   └── sections.ts             # Activa path: "/players/ranking" para la pestaña 'players-ranking'
│   └── router/
│       └── AppRouter/
│           └── AppRouter.tsx        # Registra <Route path="/players/ranking" element={<PlayerRankingsPage />} />
│
└── features/players/
    ├── pages/
    │   ├── PlayerRankingsPage/
    │   │   ├── PlayerRankingsPage.tsx                 # Contenedor principal de la página
    │   │   ├── PlayerRankingsPage.module.scss         # Layout y estilos generales
    │   │   ├── index.ts
    │   │   └── components/
    │   │       ├── RankingsHeader/
    │   │       │   ├── RankingsHeader.tsx             # Título, toggle de modo y tarjetas de metadatos
    │   │       │   ├── RankingsHeader.module.scss
    │   │       │   └── index.ts
    │   │       ├── RankingsPodium/
    │   │       │   ├── RankingsPodium.tsx             # Tarjetas destacadas Top 3 (#1 centro, #2 izq, #3 der)
    │   │       │   ├── RankingsPodium.module.scss
    │   │       │   └── index.ts
    │   │       ├── RankingsFilters/
    │   │       │   ├── RankingsFilters.tsx            # Buscador, selectores de liga y posición, limpiar
    │   │       │   ├── RankingsFilters.module.scss
    │   │       │   └── index.ts
    │   │       ├── RankingsTable/
    │   │       │   ├── RankingsTable.tsx              # Tabla detallada clasificada
    │   │       │   ├── RankingsTable.module.scss
    │   │       │   └── index.ts
    │   │       └── RankingsPagination/
    │   │           ├── RankingsPagination.tsx         # Paginador numérico y selector de filas
    │   │           ├── RankingsPagination.module.scss
    │   │           └── index.ts
    │   └── index.ts                                   # Exporta PlayerRankingsPage
    │
    ├── hooks/
    │   ├── pages/
    │   │   ├── usePlayerRankingsPage.ts               # Hook de coordinación de estado, modo y filtros
    │   │   └── index.ts
    │   └── queries/
    │       ├── usePlayerScoreRankingQuery.ts          # Consulta TanStack Query para /api/players/ranking/score
    │       ├── usePlayerQuoteRankingQuery.ts          # Consulta TanStack Query para /api/players/ranking
    │       └── index.ts
    │
    ├── types/
    │   ├── dtos.ts                                    # ScoreRankingResponseDTO, QuoteRankingResponseDTO
    │   └── models.ts                                  # PlayerRankingItem, RankingMode, RankingMetadata
    │
    ├── players.service.ts                             # Métodos getScoreRanking() y getQuoteRanking()
    ├── players.mapper.ts                              # toScoreRankingItem() y toQuoteRankingItem()
    └── index.ts                                       # API pública de la feature
```

---

## Plan de Ejecución por Componentes

1. **Tipos y Servicios**:
   - Incorporar `ScoreRankingResponseDTO`, `QuoteRankingResponseDTO` y entidades en `types/`.
   - Incorporar llamadas HTTP en `players.service.ts` con manejo de señales `AbortSignal`.
   - Implementar mappers para transformar y formatear los datos del backend y fusionarlos con el catálogo de jugadores.
2. **Hooks de Consulta y Página**:
   - `usePlayerScoreRankingQuery` y `usePlayerQuoteRankingQuery` con caching estándar.
   - `usePlayerRankingsPage` para coordinar el modo activo, filtros de cliente, paginación y ordenamiento inalterable.
3. **Componentes Visuales**:
   - `RankingsHeader`: Toggle de modo (*Por rendimiento* / *Por cotización*) y métricas de cabecera.
   - `RankingsPodium`: Podio #1, #2, #3 con laureles, avatares, marcas de agua y valores.
   - `RankingsFilters`: Barra reactiva de búsqueda, selectores y botón de limpieza.
   - `RankingsTable`: Tabla con columnas `#`, `Jugador`, `Equipo`, `Liga`, `Posición`, `Score/Cotización`, `Fecha/Vigencia` y enlace a detalle `/players/:id`.
   - `RankingsPagination`: Controles accesibles de página.
4. **Integración de Enrutamiento y Navegación**:
   - Activar la ruta `/players/ranking` en `sections.ts` y en `AppRouter.tsx`.

---
description: "Lista de tareas de implementación para SPEC-022: Visualizar ranking de jugadores"
---

# Tasks: Visualizar ranking de jugadores (SPEC-022)

**Input**: Documentos de diseño desde `/specs/022-view-player-rankings/` (`plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`).

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/player-rankings-consumption.md`.

**Tests**: El frontend se encuentra temporalmente exento de testing según la Constitución (*Verificación y testing*) y `frontend/AGENTS.md`. MUST NOT crear tests ni infraestructura de testing frontend. La validación técnica es `npm run build` desde `frontend/` y la verificación manual guiada por `quickstart.md`.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Las tareas nacen del SDD y viven exclusivamente en este archivo; no se exportan como issues individuales ni se duplica su estado en el seguimiento operativo. Generar Tasks no completa la fase ni habilita Ready o Implement: requiere Analyze satisfactorio y autorización humana explícita.

**Organization**: Tareas agrupadas por fases y por historias de usuario para posibilitar entrega incremental e independiente.

**Revisión de alcance 2026-11-12**: Los T001–T034 y sus estados se conservan como registro del trabajo anterior. Sus descripciones relativas a paginación/filtros cliente, hidratación desde catálogo y contratos/rutas anteriores están supersedidas para el alcance vigente por las tareas T035–T048 y T050. No tomar esos detalles históricos como requisitos. T030 y T031 conservan sus asignaciones originales en este registro; ninguna tarea del delta reutiliza esos identificadores. La implementación debe coordinarse con los contratos de SPEC-018 y SPEC-016; SPEC-022 no modifica backend. Las checklists son propiedad del revisor y no se alteran en esta fase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Ejecutable en paralelo (archivos independientes, sin dependencias bloqueantes sin completar).
- **[Story]**: Historia de usuario a la que pertenece (`[US1]`, `[US2]`, `[US3]`). Tareas de Setup, Foundational y Polish no llevan etiqueta de historia.
- Todas las tareas incluyen rutas de archivo exactas.

---

## Phase 1: Setup (Estructuras de Datos y Contratos de Servicio)

**Purpose**: Definición de interfaces TypeScript, contratos DTO y servicios de transporte HTTP para el ranking.

- [x] T001 [P] Definir DTOs `ScoreRankingResponseDTO` y `QuoteRankingResponseDTO` en `frontend/src/features/players/types/dtos.ts`
- [x] T002 [P] Definir modelos de dominio `PlayerRankingItem`, `RankingMode` y `RankingMetadata` en `frontend/src/features/players/types/models.ts`
- [x] T003 [P] Implementar llamadas a endpoints `getScoreRanking` y `getQuoteRanking` en `frontend/src/features/players/players.service.ts`
- [x] T004 Implementar funciones de transformación y mapeo en `frontend/src/features/players/players.mapper.ts`

---

## Phase 2: Foundational (Hooks de Consulta, Navegación y Rutas)

**Purpose**: Infraestructura transversal de fetching y enrutamiento previa a la construcción de componentes.

- [x] T005 [P] Registrar query keys de ranking en `frontend/src/features/players/constants/queryKeys.ts`
- [x] T006 [P] Implementar hook TanStack Query `usePlayerScoreRankingQuery` en `frontend/src/features/players/hooks/queries/usePlayerScoreRankingQuery.ts`
- [x] T007 [P] Implementar hook TanStack Query `usePlayerQuoteRankingQuery` en `frontend/src/features/players/hooks/queries/usePlayerQuoteRankingQuery.ts`
- [x] T008 [P] Exportar nuevos query hooks en `frontend/src/features/players/hooks/queries/index.ts`
- [x] T009 Activar la ruta `/players/ranking` para la pestaña `players-ranking` en `frontend/src/app/navigation/sections.ts`
- [x] T010 Registrar la ruta `<Route path="/players/ranking" ... />` en `frontend/src/app/router/AppRouter/AppRouter.tsx`

**Checkpoint**: Base de datos, fetching y rutas lista. Puede comenzar el desarrollo visual de User Story 1.

---

## Phase 3: User Story 1 - Consultar la tabla de posiciones del ranking activo (Priority: P1) 🎯 MVP

**Goal**: Presentar la clasificación de jugadores según la modalidad activa (*Por rendimiento* o *Por cotización*), exhibiendo el Podio Top 3 (#1 centro en escala aumentada, #2 izquierda, #3 derecha) y la tabla con orden consecutivo (1 a N), score con 6 decimales o cotización en créditos, y metadatos de recálculo.

**Independent Test**: Navegar a `/players/ranking`, verificar la alternancia entre *Por rendimiento* y *Por cotización*, las tarjetas de metadatos de cabecera, el Podio con laureles y la tabla con numeración ordinal consecutiva.

- [x] T011 [P] [US1] Crear componente `RankingsHeader` con selector toggle de modo y métricas de cabecera en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsHeader/RankingsHeader.tsx`
- [x] T012 [P] [US1] Crear módulo SCSS para cabecera en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsHeader/RankingsHeader.module.scss`
- [x] T013 [P] [US1] Crear componente `RankingsPodium` con tarjetas de los 3 líderes, laureles y marcas de agua de clubes en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPodium/RankingsPodium.tsx`
- [x] T014 [P] [US1] Crear módulo SCSS para el podio en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPodium/RankingsPodium.module.scss`
- [x] T015 [P] [US1] Crear componente `RankingsTable` con columnas `#`, `Jugador`, `Equipo`, `Liga`, `Posición`, `Score`/`Cotización`, `Fecha`/`Vigencia` en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsTable/RankingsTable.tsx`
- [x] T016 [P] [US1] Crear módulo SCSS para la tabla en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsTable/RankingsTable.module.scss`
- [x] T017 [P] [US1] Crear componente `RankingsPagination` con selector de página y filas por página en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPagination/RankingsPagination.tsx`
- [x] T018 [P] [US1] Crear módulo SCSS para paginación en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPagination/RankingsPagination.module.scss`
- [x] T019 [US1] Implementar hook de coordinación `usePlayerRankingsPage` (modo activo, paginación, hidratación con catálogo) en `frontend/src/features/players/hooks/pages/usePlayerRankingsPage.ts`
- [x] T020 [US1] Ensamblar contenedor de página `PlayerRankingsPage` en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T021 [US1] Crear módulo SCSS principal de la página en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.module.scss`

**Checkpoint**: MVP funcional. La página de ranking muestra el podio, la cabecera interactiva y la tabla paginada en ambos modos.

---

## Phase 4: User Story 2 - Comprender los estados de carga, vacío y fallos de consulta (Priority: P2)

**Goal**: Informar explícitamente al usuario el estado de carga mediante indicadores accesibles (`aria-busy`), mensaje comprensible cuando no existan clasificaciones vigentes (`ranking: []`), y banner de error con reintento.

**Independent Test**: Verificar la respuesta visual ante estado pendiente, respuesta vacía (componente de estado vacío sin tabla rota) y fallo de servidor (alerta con botón de reintento).

- [x] T022 [US2] Integrar estado de carga accesible con indicador visual y atributo `aria-busy` en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T023 [US2] Integrar presentación de estado vacío (`EmptyState`) cuando no haya jugadores clasificados en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T024 [US2] Implementar alerta accesible de error con disparador de reintento en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T025 [P] [US2] Añadir estilos SCSS para estados de carga, vacío y error en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.module.scss`

**Checkpoint**: Resiliencia y feedback asíncrono completo. La interfaz responde adecuadamente a cualquier estado de red.

---

## Phase 5: User Story 3 - Navegar al detalle individual y filtros reactivos (Priority: P3)

**Goal**: Permitir la navegación fluida hacia la ficha del jugador (`/players/:id`) al hacer clic en filas y tarjetas, e incorporar la barra de filtros en cliente (búsqueda por nombre, liga y posición táctica) preservando inalterable el Podio Top 3 y la posición `#` original.

**Independent Test**: Clic en cualquier jugador para verificar navegación a `/players/:id`, filtrar por nombre/liga/posición verificando que la tabla se actualiza mientras el podio permanece fijo con los 3 mejores globales, y validar adaptabilidad móvil.

- [x] T026 [P] [US3] Agregar enlaces accesibles hacia `/players/:id` en filas de `RankingsTable.tsx` y tarjetas de `RankingsPodium.tsx`
- [x] T027 [P] [US3] Crear componente `RankingsFilters` con buscador, selectores de liga y posición, y botón limpiar en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsFilters/RankingsFilters.tsx`
- [x] T028 [P] [US3] Crear módulo SCSS para la barra de filtros en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsFilters/RankingsFilters.module.scss`
- [x] T029 [US3] Integrar lógica de filtrado reactivo de cliente en `usePlayerRankingsPage.ts` manteniendo fijo el podio en el Top 3 absoluto
- [x] T030 [US3] Integrar `RankingsFilters` en `PlayerRankingsPage.tsx`
- [x] T031 [P] [US3] Implementar estilos responsivos (breakpoints de 320px, 768px, 1024px, 1440px) en `PlayerRankingsPage.module.scss`, `RankingsPodium.module.scss` y `RankingsTable.module.scss`

**Checkpoint**: Alcance completo de la feature implementado según los mockups normativos.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Integración de exportaciones públicas, verificación de compilación y validación manual.

- [x] T032 [P] Exportar `PlayerRankingsPage` en `frontend/src/features/players/pages/index.ts` y en `frontend/src/features/players/index.ts`
- [x] T033 Verificar compilación TypeScript y formato Biome ejecutando `npm run build` desde `frontend/`
- [ ] T034 Validar escenarios de extremo a extremo según la guía `specs/022-view-player-rankings/quickstart.md`

---

## Dependencies & Execution Order

### Dependencias entre Fases

1. **Setup (Fase 1)**: Sin dependencias, arranca inmediatamente.
2. **Foundational (Fase 2)**: Depende de Fase 1 (tipos y servicio). Habilita el desarrollo de User Stories.
3. **User Story 1 (Fase 3 - MVP)**: Depende de Fase 2. Construye el flujo nuclear del ranking.
4. **User Story 2 (Fase 4)**: Depende de Fase 3. Añade resiliencia asíncrona.
5. **User Story 3 (Fase 5)**: Depende de Fase 3. Añade navegación a detalle, filtros y responsividad móvil.
6. **Polish (Fase 6)**: Depende de la finalización de las historias de usuario implementadas.

### Oportunidades de Paralelismo ([P])

- **Fase 1**: T001 (DTOs), T002 (Models) y T003 (Service) pueden realizarse en paralelo.
- **Fase 2**: T005 (QueryKeys), T006 (ScoreHook), T007 (QuoteHook) y T009 (Navigation) pueden realizarse en paralelo.
- **Fase 3**: Los componentes atómicos (`RankingsHeader`, `RankingsPodium`, `RankingsTable`, `RankingsPagination`) y sus SCSS respectivos (T011 a T018) pueden desarrollarse en paralelo antes de ensamblarlos en T019 y T020.
- **Fase 5**: T026 (Enlaces a detalle) y T027/T028 (Barra de filtros) pueden desarrollarse en paralelo.

---

## Implementation Strategy

> **Vigencia**: la estrategia siguiente corresponde al alcance histórico anterior al delta paginado. Para el delta vigente, aplicar exclusivamente la estrategia de implementación de Phase 7; no descargar ni enriquecer desde catálogo.

### Enfoque MVP Primero (User Story 1)

1. Completar Fase 1 (Setup) y Fase 2 (Foundational).
2. Implementar Fase 3 (User Story 1): Podio, cabecera con toggle y tabla paginada con hidratación del catálogo.
3. Validar de forma independiente: la pantalla ya es completamente operable y aporta valor inmediato al usuario.
4. Continuar incrementalmente con Fase 4 (estados de error/vacío) y Fase 5 (filtros y responsividad).

> [!IMPORTANT]
> El agente no ejecuta tests de aplicación. Cada fase y alcance completado se somete a revisión humana (`CODE REVIEW`) y comprobación manual (`TESTING`) antes de avanzar al cierre.

---

## Phase 7: Adopción de contratos paginados y alineación de la interfaz (delta vigente)

**Purpose**: Actualizar la implementación histórica para que consuma las páginas remotas de score/quote, haga la consulta global independiente del podio y use la identidad incluida en cada respuesta, conservando IDs y avance históricos.

### User Story 1 — Consultar rankings paginados y podio global (P1)

**Goal**: Consultar los endpoints y parámetros vigentes, mantener orden/posiciones globales y representar por separado la página filtrada y el Top 3 sin filtros.

**Independent Test**: Con respuestas contractuales disponibles, alternar score/quote; comprobar endpoint, parámetros de tabla y podio (`page=0&size=3` sin filtros), filas de la página, posiciones globales, metadatos y datos `player` sin petición al catálogo.

- [x] T035 [US1] Actualizar DTOs y tipos de respuestas paginadas, `content`, metadatos y `filterOptions` según `specs/022-view-player-rankings/data-model.md` y `specs/022-view-player-rankings/contracts/player-rankings-consumption.md` en `frontend/src/features/players/types/dtos.ts` y `frontend/src/features/players/types/models.ts`
- [x] T036 [US1] Adaptar `getScoreRanking` y `getQuoteRanking` para `page`, `size`, `name`, `leagueId` y `position`, usando `GET /api/rankings/score` y `GET /api/rankings/quote` conforme a los contratos referenciados en `frontend/src/features/players/players.service.ts`
- [x] T037 [US1] Mapear `content` de score y quote, identidad `player`, fechas, origen, metadatos y opciones globales sin catálogo, reordenamiento ni renumeración en `frontend/src/features/players/players.mapper.ts`
- [x] T038 [P] [US1] Incluir modo, página, tamaño y filtros efectivos en query keys de tabla y una clave diferenciada para consulta de podio en `frontend/src/features/players/constants/playersQueryKeys.ts`
- [x] T039 [P] [US1] Adaptar el hook de consulta score a los parámetros paginados contractuales en `frontend/src/features/players/hooks/queries/usePlayerScoreRankingQuery.ts`
- [x] T040 [P] [US1] Adaptar el hook de consulta quote a los parámetros paginados contractuales en `frontend/src/features/players/hooks/queries/usePlayerQuoteRankingQuery.ts`
- [x] T041 [US1] Coordinar modo, filtros, página base cero, tamaño, respuestas tardías y consultas independientes de tabla y podio (`page=0`, `size=3`, sin filtros) en `frontend/src/features/players/hooks/pages/usePlayerRankingsPage.ts`
- [x] T042 [US1] Actualizar el ensamblado de la página para presentar contenido paginado y metadatos del servidor, sin descargar el catálogo para enriquecer filas, en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T043 [US1] Adaptar la tabla para consumir las filas recibidas y mantener las posiciones globales y total filtrado en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsTable/RankingsTable.tsx`
- [x] T044 [US1] Conectar filtros de nombre, liga y posición a los parámetros remotos, reiniciar página ante cambio de filtro y limpiar criterios en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsFilters/RankingsFilters.tsx`
- [x] T045 [US1] Conectar controles de paginación/tamaño con los metadatos y parámetros del backend, conservando criterios al cambiar página, en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPagination/RankingsPagination.tsx`
- [x] T046 [US1] Adaptar presentación del podio para usar solo el resultado independiente global y permitir menos de tres entradas sin rellenarlas en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPodium/RankingsPodium.tsx`
- [x] T047 [US1] Presentar encabezado y opciones globales; integrar carga accesible, errores con reintento y estados vacíos que distingan ausencia global de datos y filtros sin coincidencias, sin alterar el manejo HTTP/autenticación común, en `frontend/src/features/players/pages/PlayerRankingsPage/PlayerRankingsPage.tsx`
- [x] T048 [US1] Actualizar escenarios manuales para verificar endpoints, filtros/paginación remotos, podio independiente, identidad en respuesta, orden y estados según el contrato vigente en `specs/022-view-player-rankings/quickstart.md`

> **T049 (registro histórico; consolidada en T047)**: La tarea anterior de estados de carga, error/reintento y vacío para `PlayerRankingsPage.tsx` no tiene trabajo independiente. La implementación está consolidada en T047 y su validación manual en T048.

### User Story 3 — Navegación e identidad del jugador (P3)

**Goal**: Mantener navegación a detalle usando `playerId` y datos `player` del contrato, con fallbacks solo para campos opcionales.

**Independent Test**: Desde filas y tarjetas navegar a `/players/:id`; comprobar nombre/datos disponibles desde la respuesta y fallbacks para campos opcionales nulos, sin consulta de catálogo.

- [x] T050 [US3] Verificar que enlaces de filas y tarjetas usan el identificador de respuesta y conservan navegación al detalle sin dependencia del catálogo en `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsTable/RankingsTable.tsx` y `frontend/src/features/players/pages/PlayerRankingsPage/components/RankingsPodium/RankingsPodium.tsx`

### Dependencias y paralelismo del delta

- T035 precede T036–T037; T036–T037 preceden T039–T042. T038 puede avanzar en paralelo con T035; T039 y T040 son independientes entre sí tras T035–T036. T043–T047 dependen de los modelos y coordinación T037–T042. T050 depende de los modelos/mappeo de T037. T048 se actualiza junto con los escenarios cubiertos y puede redactarse en paralelo a la implementación tras revisar contratos.
- US1 es el MVP del delta. US2 depende de los estados coordinados de US1; US3 depende de la identidad mapeada de US1. La ruta web existente se conserva; no hay cambios backend ni tests frontend.

### Estrategia de implementación del delta

1. Alinear DTOs, servicios, mapper y consultas con los contratos autoritativos.
2. Conectar estado de tabla y consulta independiente del podio; presentar filas y opciones remotas sin enriquecimiento desde catálogo.
3. Completar estados vacíos/error, navegación y guía manual.
4. La verificación manual queda a cargo del usuario; no crear ni ejecutar tests frontend. No marcar tareas por anticipado ni interpretar esta descomposición como autorización de implementación.

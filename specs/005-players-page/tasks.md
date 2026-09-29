---

description: "Tareas de implementación de la página de catálogo de jugadores"
---

# Tasks: Página de catálogo de jugadores

**Input**: Documentos de diseño de `/specs/005-players-page/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/players-catalog.md`, `quickstart.md`

**Tests**: No se crean ni ejecutan tests frontend por norma del proyecto. La verificación automatizada permitida es el build; los escenarios funcionales de `quickstart.md` son manuales.

**Organization**: Las tareas se agrupan por historia de usuario para permitir implementación y validación incremental.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo porque afecta archivos distintos y no depende de una tarea incompleta.
- **[Story]**: Historia de usuario a la que pertenece la tarea (`US1`, `US2`, `US3`, `US4`).
- Cada descripción incluye rutas exactas de archivos.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar los recursos estáticos runtime específicos de la feature sin añadir dependencias.

- [X] T001 Copiar `referencias-imagenes/Player Generic-Icon.png`, `Premier League-League.png`, `Bundesliga-League.png`, `Primera Division-League.png`, `Serie A-League.png`, `Ligue 1-League.png`, `Goalkeeper-Player.png`, `Defence-Player.png`, `Midfield-Player.png` y `Offence-Player.png` a `frontend/src/features/players/assets/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear la frontera tipada y de consulta compartida que bloquea todas las historias.

**CRITICAL**: Ninguna historia de usuario puede comenzar hasta completar esta fase.

- [X] T002 [P] Definir `PlayerResponseDTO` y `PlayersPageResponseDTO` en `frontend/src/features/players/types/dtos.ts`, `Player`, `PlayersPage` y `PlayerSampleStatistics` en `frontend/src/features/players/types/models.ts`, y sus exports internos en `frontend/src/features/players/types/index.ts`
- [X] T003 [P] Definir la factory de query keys parametrizada por página y tamaño en `frontend/src/features/players/constants/playersQueryKeys.ts` y exportarla desde `frontend/src/features/players/constants/index.ts`
- [X] T004 Implementar la transformación `PlayerResponseDTO -> Player` y `PlayersPageResponseDTO -> PlayersPage`, incluida la inferencia `active: true`, en `frontend/src/features/players/players.mapper.ts`
- [X] T005 Implementar `playersService.getPage(page, size, signal)` con `GET /players`, `authenticated: true` y parámetros `page`/`size` en `frontend/src/features/players/players.service.ts`
- [X] T006 Implementar la query paginada con tamaño fijo 12, query key por selección y propagación de `AbortSignal` en `frontend/src/features/players/hooks/queries/usePlayersQuery.ts`, exportándola desde `frontend/src/features/players/hooks/queries/index.ts` y `frontend/src/features/players/hooks/index.ts`

**Checkpoint**: La feature puede obtener una página autenticada y exponer Models sin filtrar DTO ni HTTP a la UI.

---

## Phase 3: User Story 1 - Consultar jugadores como usuario autenticado (Priority: P1) MVP

**Goal**: Mostrar la primera página privada del catálogo con hasta 12 tarjetas y feedback de carga, error y vacío.

**Independent Test**: Iniciar sesión, abrir `/players` con jugadores y comprobar hasta 12 tarjetas con nombre, icono genérico, un bloque central con el equipo real, `24` partidos, `8` goles y `5` asistencias, además de liga y posición; abrir la misma URL sin sesión y comprobar la redirección a `/login`.

### Implementation for User Story 1

- [X] T007 [P] [US1] Definir las estadísticas comunes `24` partidos, `8` goles y `5` asistencias con sus etiquetas en `frontend/src/features/players/constants/playerSampleStatistics.ts` y exportarlas desde `frontend/src/features/players/constants/index.ts`
- [X] T008 [US1] Crear la tarjeta base con icono genérico, nombre largo contenido, un bloque central con el equipo real y las tres estadísticas etiquetadas, y liga y posición textuales; asegurar que los textos contrasten con el fondo en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx`, `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` y `frontend/src/features/players/components/PlayerCard/index.ts`
- [X] T009 [US1] Crear la página con consulta inicial, cuadrícula de cuatro columnas, máximo 12 tarjetas y estados accesibles de carga, error con reintento y catálogo vacío en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y `frontend/src/features/players/pages/PlayersPage/index.ts`
- [X] T010 [US1] Exponer `PlayerCard` y `PlayersPage` mediante `frontend/src/features/players/components/index.ts`, `frontend/src/features/players/pages/index.ts` y `frontend/src/features/players/index.ts`, y sustituir la ruta provisional por `/players` dentro del `AuthGuard` privado en `frontend/src/app/router/AppRouter.tsx`

**Checkpoint**: US1 funciona de forma independiente como catálogo privado de primera página, incluidos carga, error, reintento y vacío.

---

## Phase 4: User Story 2 - Navegar entre páginas del catálogo (Priority: P1)

**Goal**: Recorrer el catálogo mediante una ventana paginada y conservar como resultado final la selección más reciente.

**Independent Test**: Con al menos ocho páginas, navegar a primera, última y páginas de la ventana de tres anteriores/posteriores; verificar límites, selección actual, cambios rápidos y corrección a la última página válida cuando disminuye el total.

### Implementation for User Story 2

- [X] T011 [P] [US2] Implementar el cálculo puro de destinos visibles con página actual, hasta tres anteriores/posteriores y extremos sin duplicados en `frontend/src/features/players/utils/getVisiblePlayerPages.ts` y exportarlo desde `frontend/src/features/players/utils/index.ts`
- [X] T012 [P] [US2] Crear el paginador accesible con numeración visual desde uno, estado actual y controles primera/última deshabilitados en límites en `frontend/src/features/players/components/PlayersPagination/PlayersPagination.tsx`, `frontend/src/features/players/components/PlayersPagination/PlayersPagination.module.scss` y `frontend/src/features/players/components/PlayersPagination/index.ts`
- [X] T013 [US2] Crear el Page Hook que coordina `selectedPage`, la query y la corrección a `totalPages - 1` para selecciones que dejan de existir en `frontend/src/features/players/hooks/pages/usePlayersPage.ts`, exportándolo desde `frontend/src/features/players/hooks/pages/index.ts` y `frontend/src/features/players/hooks/index.ts`
- [X] T014 [US2] Integrar `usePlayersPage` y `PlayersPagination` en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, actualizar sus estilos en `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y exportar el componente desde `frontend/src/features/players/components/index.ts`

**Checkpoint**: US2 permite recorrer cualquier página ofrecida y mantiene alineados catálogo, carga y selección más reciente.

---

## Phase 5: User Story 3 - Reconocer estado y clasificación visual (Priority: P2)

**Goal**: Diferenciar liga, posición y estado activo mediante recursos visuales, colores suaves y texto accesible.

**Independent Test**: Revisar tarjetas de las cinco ligas y cuatro posiciones conocidas, además de valores desconocidos, y comprobar recursos, texto, contraste, fallback neutro y entrada lateral de `Activo` solo durante hover.

### Implementation for User Story 3

- [X] T015 [P] [US3] Definir mappings abiertos para recursos y variantes visuales de las cinco ligas y cuatro posiciones, con fallback neutro, en `frontend/src/features/players/constants/playerClassifications.ts` y exportarlos desde `frontend/src/features/players/constants/index.ts`
- [X] T016 [US3] Incorporar imágenes con alternativa textual, variantes de liga/posición y el indicador `Activo` animado desde la izquierda sin reflujo en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss`

**Checkpoint**: US3 comunica todas las clasificaciones y el estado sin depender únicamente de imágenes o color.

---

## Phase 6: User Story 4 - Visualizar controles de filtrado (Priority: P3)

**Goal**: Mostrar controles interactivos de búsqueda, liga y posición que no alteren consulta, resultados ni página.

**Independent Test**: Escribir y seleccionar opciones en los tres controles y comprobar que conservan su estado visual sin cambiar tarjetas, orden, paginación, query key ni solicitudes HTTP.

### Implementation for User Story 4

- [X] T017 [P] [US4] Crear los controles identificados de búsqueda, liga y posición como inputs controlados en `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx`, `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.module.scss` y `frontend/src/features/players/components/PlayerCatalogControls/index.ts`
- [X] T018 [US4] Ampliar el estado local de `usePlayersPage` con búsqueda, liga y posición sin incorporarlos a query keys, parámetros HTTP ni cambios de página en `frontend/src/features/players/hooks/pages/usePlayersPage.ts`
- [X] T019 [US4] Integrar `PlayerCatalogControls` sobre la cuadrícula en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, ajustar la composición en `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y exportarlo desde `frontend/src/features/players/components/index.ts`

**Checkpoint**: US4 presenta los tres controles previstos sin modificar el catálogo.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar coherencia transversal, accesibilidad y criterios operativos de la feature completa.

- [X] T020 Revisar semántica, foco visible, `aria-busy`, `role="status"`, `role="alert"`, textos alternativos y ausencia de scroll horizontal a 1280x720 en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss`, `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx`, `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx` y `frontend/src/features/players/components/PlayersPagination/PlayersPagination.tsx`
- [ ] T021 Ejecutar manualmente los escenarios de acceso, tarjetas, paginación, controles, estados y fallbacks descritos en `specs/005-players-page/quickstart.md`
- [ ] T022 Ejecutar `npm run build` desde `frontend/` usando el script definido en `frontend/package.json` y confirmar que no se generaron cambios versionables en `frontend/dist/` ni archivos `frontend/*.tsbuildinfo`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; puede comenzar inmediatamente.
- **Foundational (Phase 2)**: Depende de Setup y bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Foundational y constituye el MVP.
- **US2 (Phase 4)**: Depende de US1 porque amplía la consulta y la página con navegación.
- **US3 (Phase 5)**: Depende de US1; puede desarrollarse en paralelo con US2 porque modifica la tarjeta mientras US2 modifica paginación y coordinación de página.
- **US4 (Phase 6)**: Depende de US1; su integración final debe coordinarse con US2 porque ambas historias modifican `usePlayersPage.ts` y `PlayersPage.tsx`.
- **Polish (Phase 7)**: Depende de todas las historias incluidas en la entrega.

### User Story Dependencies

- **US1 (P1)**: Comienza tras Foundational y no depende de otra historia.
- **US2 (P1)**: Reutiliza y amplía la página entregada por US1.
- **US3 (P2)**: Reutiliza la tarjeta de US1, sin depender de US2 ni US4.
- **US4 (P3)**: Reutiliza la página de US1; no depende funcionalmente de US2 o US3, aunque comparte archivos de integración con US2.

### Dependency Graph

```text
Setup -> Foundational -> US1 -> US2 -> Polish
                           |----> US3 --|
                           |----> US4 --|
```

### Within Each User Story

- Crear primero constantes, utilidades o estado requeridos por los componentes consumidores.
- Implementar componentes antes de integrarlos en `PlayersPage`.
- Actualizar los barrels en la misma tarea que introduce el consumidor externo correspondiente.
- Completar el checkpoint de la historia antes de avanzar secuencialmente a la siguiente prioridad.

### Parallel Opportunities

- T002 y T003 pueden ejecutarse en paralelo después de T001.
- T007 puede avanzar en paralelo con trabajo que no dependa todavía de las estadísticas.
- T011 y T012 pueden ejecutarse en paralelo después de US1.
- US2 y US3 pueden avanzar en paralelo tras US1, coordinando únicamente la exportación final de componentes.
- T015 puede ejecutarse en paralelo con las tareas de US2.
- T017 puede prepararse en paralelo con US2 y US3; T018-T019 deben integrar sobre la versión vigente del Page Hook y la Page.

---

## Parallel Example: User Story 1

```text
Task T007: Definir estadísticas de muestra en frontend/src/features/players/constants/playerSampleStatistics.ts
Task preparatoria independiente: revisar la composición y estados existentes de frontend/src/features/players/pages/PlayersPage/
```

## Parallel Example: User Story 2

```text
Task T011: Implementar frontend/src/features/players/utils/getVisiblePlayerPages.ts
Task T012: Crear frontend/src/features/players/components/PlayersPagination/
```

## Parallel Example: User Story 3

```text
Task T015: Definir mappings en frontend/src/features/players/constants/playerClassifications.ts
Task paralela de otra historia: implementar T011-T013 de paginación sin modificar PlayerCard
```

## Parallel Example: User Story 4

```text
Task T017: Crear frontend/src/features/players/components/PlayerCatalogControls/
Task paralela de otra historia: completar T015-T016 de clasificación visual sin modificar el Page Hook
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational.
3. Completar Phase 3: US1.
4. Detenerse y validar US1 de forma independiente: acceso privado, primera página, tarjetas y estados.
5. Ejecutar el build permitido antes de demostrar el MVP.

### Incremental Delivery

1. Entregar Setup + Foundational como base técnica interna.
2. Añadir US1 para obtener el catálogo privado visible como MVP.
3. Añadir US2 para recorrer el catálogo completo.
4. Añadir US3 para completar clasificación y estado visual.
5. Añadir US4 para completar la composición de controles sin filtrado funcional.
6. Ejecutar Polish y la validación manual completa sin atribuir cobertura de tests al build.

### Parallel Team Strategy

1. El equipo completa Setup y Foundational en orden.
2. Una persona completa US1 para estabilizar la Page y PlayerCard base.
3. Tras US1, una persona trabaja US2, otra US3 y otra prepara T017 de US4.
4. La integración de T018-T019 se realiza sobre el Page Hook y la Page ya actualizados por US2.

---

## Notes

- Las tareas `[P]` afectan archivos diferentes y no dependen de tareas aún incompletas.
- No añadir parámetros de búsqueda, liga, posición u orden a `playersService` ni a `playersQueryKeys`.
- No importar DTO, Service ni HTTP desde Pages o Components.
- No añadir dependencias ni infraestructura de tests frontend.
- El build permitido no ejecuta ni acredita tests; T021 sigue siendo una validación manual.

## Phase 8: Convergence

- [X] T023 CRITICAL Validar en `frontend/src/features/players/players.service.ts` y `frontend/src/features/players/players.mapper.ts` que la respuesta externa contiene los campos requeridos, metadatos de paginación coherentes y como máximo 12 jugadores, rechazando datos inválidos como error recuperable antes de exponer Models per Constitution §3 y FR-002/FR-006 (partial)
- [X] T024 Reubicar o reservar un área para el indicador `Activo` en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` para que su entrada lateral por hover no oculte el icono, nombre ni otros datos esenciales y no produzca reflujo per FR-010 y US3/AC3 (contradicts)
- [X] T025 Reconocer `Primera División` en `frontend/src/features/players/constants/playerClassifications.ts` y `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx`, conservando un alias explícito para `Primera Division` si ambos valores forman parte del contrato, para aplicar el recurso y color propios per FR-007/FR-008 y US3/AC1-2 (partial)
- [X] T026 Coordinar en `frontend/src/features/players/hooks/pages/usePlayersPage.ts` y `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx` las tarjetas, el total y el paginador como un único estado de consulta, ocultando o deshabilitando navegación obsoleta durante carga o error per FR-016/FR-017 y SC-005/SC-008 (partial)
- [X] T027 Hacer legibles los nombres largos en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` mediante ajuste controlado o una alternativa accesible por teclado, sin invadir estadísticas ni clasificaciones per Edge case: long names y T008 (partial)

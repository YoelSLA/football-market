---

description: "Task list template for feature implementation"
---

# Tasks: Visualizar portfolio personal (SPEC-017)

**Input**: Design documents from `specs/017-view-personal-portfolio-ui/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: El frontend está exento de testing de forma temporal (Constitution, *Verificación y testing*; `frontend/AGENTS.md`: «Por el momento no crear tests de frontend ni incorporar infraestructura para ellos»). **Esta feature no crea tests ni infraestructura de testing.** La verificación se compone del control ejecutable permitido —`npm run build` desde `frontend/`, a cargo del usuario— y de los escenarios manuales de `specs/017-view-personal-portfolio-ui/quickstart.md`.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/`, `frontend/src/`
- Paths shown below follow the frontend structure of `plan.md` §Project Structure

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Estructura de la feature y los cambios compartidos que el resto de las tareas consume

- [X] T001 Crear la estructura de directorios y los `index.ts` de API pública de `frontend/src/features/portfolio/` según `plan.md` §Project Structure, con las carpetas `components/`, `constants/`, `hooks/pages/`, `hooks/queries/`, `pages/`, `types/` y `utils/`
- [X] T002 [P] Reubicar el ícono genérico de jugador a `frontend/src/shared/assets/Player Generic-Icon.png` y actualizar los imports en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerPortrait/PlayerPortrait.tsx`, consumiéndolo con el alias `@/shared/assets/Player Generic-Icon.png`. La carpeta de recursos no lleva `index.ts`: no es un módulo de código, siguiendo el precedente de `features/players/assets/`
- [X] T003 [P] Declarar en `frontend/src/styles/abstracts/tokens/_colors.scss` los cinco primitivos de estado sobre superficie oscura —`$color-positive-on-dark` `#4ade80`, `$color-negative-on-dark` `#f87171`, `$color-caution-on-dark` `#fbbf24`, `$color-error-on-dark` `#fda4af` y `$color-border-dark` `#64748b`— con su función, uso, restricción y contraste medido según `plan.md` §12
- [X] T004 Declarar en `frontend/src/styles/abstracts/themes/_default.scss` los cinco roles semánticos —`$state-positive`, `$state-negative`, `$state-caution`, `$state-error-on-dark` y `$border-surface-dark`— que consumen los componentes de la sección (depende de T003)
- [X] T005 [P] Actualizar el inventario de recursos gráficos locales en `docs/frontend/technologies.md` con la nueva ubicación del ícono genérico, según `research.md` R-11

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Base compartida por las tres historias: contrato, modelo, transformación, acceso a datos y formato de presentación

**⚠️ CRITICAL**: Ninguna historia puede comenzar hasta que esta fase esté completa

- [X] T006 Declarar el contrato HTTP de `GET /api/portfolio/me` en `frontend/src/features/portfolio/types/dtos.ts`: `PortfolioResponseDTO`, `PortfolioSummaryResponseDTO`, `PortfolioPositionResponseDTO` y `PortfolioPaginationResponseDTO`, con la nulidad exacta de `data-model.md` §3
- [X] T007 Declarar los modelos de dominio en `frontend/src/features/portfolio/types/models.ts`: `PortfolioSummary`, `PortfolioPosition`, `PortfolioPagination` y `Portfolio`, con las uniones literales `PositionStatus`, `ValuationStatus` y `PositionFilter`, y los importes en centésimas (depende de T006)
- [X] T008 Exponer la API de tipos en `frontend/src/features/portfolio/types/index.ts` (depende de T006, T007)
- [X] T009 [P] Declarar las claves de caché en `frontend/src/features/portfolio/constants/portfolioQueryKeys.ts`: `["portfolio","summary"]` y `["portfolio","holdings",status,page]`
- [X] T010 [P] Declarar los rótulos y textos de la sección en `frontend/src/features/portfolio/constants/portfolioLabels.ts`, con el registro de tuteo fijado en `spec.md` DC-10
- [X] T011 [P] Implementar el formateo de importes en `frontend/src/features/portfolio/utils/formatCredits.ts`: centésimas a créditos con dos decimales, signo conservado y guion para la ausencia
- [X] T012 [P] Implementar el formateo de porcentajes en `frontend/src/features/portfolio/utils/formatPercentage.ts`: dos decimales y guion para la ausencia
- [X] T013 [P] Implementar el formateo de fechas y horas en `frontend/src/features/portfolio/utils/formatDateTime.ts`: ISO 8601 a día, mes, año, hora y minutos, y guion para la ausencia
- [X] T014 Exponer las utilidades en `frontend/src/features/portfolio/utils/index.ts` (depende de T011, T012, T013)
- [X] T015 Exponer las constantes en `frontend/src/features/portfolio/constants/index.ts` (depende de T009, T010)
- [X] T016 Implementar el Mapper en `frontend/src/features/portfolio/portfolio.mapper.ts`: transformación explícita de `PortfolioResponseDTO` a `Portfolio`, preservando campos, nulos, importes y orden, sin type guards, validaciones runtime ni comprobaciones o cálculos financieros, según `data-model.md` §5 (depende de T008)
- [X] T017 Implementar el Service en `frontend/src/features/portfolio/portfolio.service.ts`: respuesta `http.get<PortfolioResponseDTO>` de `GET /api/portfolio/me`, con `authenticated: true`, señal de aborto y parámetros `page`, `size` y `status` opcional; transformar el DTO mediante el Mapper dentro de la frontera del Service y devolver Model, sin validaciones runtime ad hoc (depende de T016)
- [X] T018 Implementar el Query Hook del resumen en `frontend/src/features/portfolio/hooks/queries/usePortfolioSummaryQuery.ts`: clave propia y consulta con `page=0` y `size=1` (depende de T017)
- [X] T019 Implementar el Query Hook de tenencias en `frontend/src/features/portfolio/hooks/queries/usePortfolioHoldingsQuery.ts`: clave con filtro y página, y consulta con `size=6` (depende de T017)
- [X] T020 [P] Implementar la familia de la sección de valores en `frontend/src/features/portfolio/components/`: `PortfolioValueSection/`, `PortfolioValueHeader/`, `PortfolioValueRows/` y `PortfolioValueRow/`, cada uno con su `.module.scss` de bloque BEM en `kebab-case`, y el tipo de UI compartido de la fila en `frontend/src/features/portfolio/types/ui.ts`, expuesto por el barrel de `types/` (depende de T008)

**Checkpoint**: Base lista — las historias pueden implementarse
- [X] T021 Implementar el componente de retrato en `frontend/src/features/portfolio/components/PortfolioPortrait/PortfolioPortrait.tsx` y su `PortfolioPortrait.module.scss`, con la cadena `imageUrl` → ícono genérico de `shared/assets/` y el texto alternativo que no depende de la imagen para identificar al jugador (depende de T002)

---

## Phase 3: User Story 1 - Conocer el resumen financiero de mi portfolio (Priority: P1) 🎯 MVP

**Goal**: La sección Cartera muestra la pestaña *Mi portfolio* con los trece indicadores del resumen del portfolio, su agrupación y los estados de carga, vacío y error. La acción que conduce a *Mis tenencias* se entrega con US2 (T031), cuando su destino esté disponible, conforme a A1.

**Independent Test**: Con un usuario autenticado y un portfolio real o vacío, se verifica que la pantalla presenta los trece indicadores con sus valores correctos y que el indicador de posiciones está rotulado como abiertas (escenarios 1 a 3, 6 a 9 y 29 a 32 de `quickstart.md`). La acción hacia *Mis tenencias* no forma parte de este incremento: se entrega con la segunda pestaña, en el incremento de US2, y se verifica en la feature completa.

### Validación de User Story 1 (sin tests de frontend)

> El frontend está exento de testing. Esta historia **no crea tests ni infraestructura de testing**. La verificación comprende el control ejecutable permitido —`npm run build` desde `frontend/`, a cargo del usuario— y los escenarios de `specs/017-view-personal-portfolio-ui/quickstart.md` §3.1, §3.2 y §3.6.

### Implementation for User Story 1

- [X] T022 [P] [US1] Implementar el componente de indicador destacado en `frontend/src/features/portfolio/components/PortfolioMetricCard/PortfolioMetricCard.tsx` y su `PortfolioMetricCard.module.scss`, con rótulo, valor y texto auxiliar, y con la distinción de resultado positivo, negativo y ausente
- [X] T023 [US1] Implementar el Page Hook del resumen en `frontend/src/features/portfolio/hooks/pages/usePortfolioSummaryPage.ts`: consulta, estado de carga, mensaje de error con `getErrorMessage` y reintento (depende de T018)
- [X] T024 [US1] Implementar la Page del resumen en `frontend/src/features/portfolio/pages/PortfolioSummaryPage/PortfolioSummaryPage.tsx` y su `PortfolioSummaryPage.module.scss`: cabecera con título y subtítulo, cuatro indicadores destacados, panel de tenencias y panel de resultados financieros. La acción hacia *Mis tenencias* no se incluye en este incremento: se entrega con la segunda pestaña, en T031 (depende de T020, T022, T023)
- [X] T025 [US1] Exponer la Page del resumen en `frontend/src/features/portfolio/index.ts` y registrar la ruta `/portfolio` en `frontend/src/app/router/AppRouter.tsx` (depende de T024)
- [X] T026 [US1] Habilitar en `frontend/src/app/navigation/sections.ts` la sección `portfolio` con la ruta `/portfolio` y la pestaña `portfolio-overview`, dejando `portfolio-holdings` con `path: null` hasta que exista la segunda pestaña (depende de T025)

**Checkpoint**: User Story 1 es funcional y verificable por sí sola

---

## Phase 4: User Story 2 - Recorrer mis posiciones y filtrar por estado (Priority: P2)

**Goal**: La pestaña *Mis tenencias* muestra seis tarjetas por página con foto, datos financieros principales, el filtro *Todas* / *Abiertas* / *Cerradas* y la paginación, todo alimentado por la consulta del backend.

**Independent Test**: Con un usuario que tiene posiciones abiertas y cerradas, se verifica la grilla, el contenido de cada tarjeta, el filtro, la paginación, el reinicio a la primera página al cambiar el filtro y la acción que conduce desde *Mi portfolio* a esta pestaña (escenarios 4, 10 a 22 de `quickstart.md`).

### Validación de User Story 2 (sin tests de frontend)

> El frontend está exento de testing. Esta historia **no crea tests ni infraestructura de testing**. La verificación comprende el control ejecutable permitido —`npm run build` desde `frontend/`, a cargo del usuario— y los escenarios de `specs/017-view-personal-portfolio-ui/quickstart.md` §3.3, §3.4 y §3.7.

### Implementation for User Story 2

- [X] T027 [P] [US2] Implementar la tarjeta de posición en `frontend/src/features/portfolio/components/PortfolioPositionCard/PortfolioPositionCard.tsx` y su `PortfolioPositionCard.module.scss`: retrato con degradación al ícono genérico, identidad, estado, tenencia, valoración, resultado con su signo y la acción *Ver detalles*
- [X] T028 [P] [US2] Implementar el filtro de estado en `frontend/src/features/portfolio/components/PortfolioStatusFilter/PortfolioStatusFilter.tsx` y su `PortfolioStatusFilter.module.scss`, con *Todas*, *Abiertas* y *Cerradas* y `aria-pressed` en el estado activo
- [X] T029 [US2] Implementar el Page Hook de tenencias en `frontend/src/features/portfolio/hooks/pages/usePortfolioHoldingsPage.ts`: estado de página y de filtro, consulta con ambos, reinicio a la primera página al cambiar el filtro, recorte de página fuera de rango, estado de carga, error y reintento (depende de T019)
- [X] T030 [US2] Implementar la Page de tenencias en `frontend/src/features/portfolio/pages/PortfolioHoldingsPage/PortfolioHoldingsPage.tsx` y su `PortfolioHoldingsPage.module.scss`: cabecera con título y total, bloque de filtros, retícula de seis tarjetas en tres columnas por dos filas y paginación mediante `PaginatedPageLayout`. Las tarjetas se presentan en el orden que entrega el backend, sin reordenar por criterio propio (depende de T027, T028, T029)
- [X] T031 [US2] Registrar la ruta `/portfolio/holdings` en `frontend/src/app/router/AppRouter.tsx`, habilitar la pestaña `portfolio-holdings` en `frontend/src/app/navigation/sections.ts` y añadir en `frontend/src/features/portfolio/pages/PortfolioSummaryPage/PortfolioSummaryPage.tsx` la acción que conduce a *Mis tenencias* (depende de T030)

**Checkpoint**: User Stories 1 y 2 son funcionales de forma independiente

---

## Phase 5: User Story 3 - Revisar el detalle completo de una posición (Priority: P3)

**Goal**: La acción *Ver detalles* abre un panel lateral con toda la información disponible de la posición, sin consulta adicional, accesible con teclado y con la semántica de diálogo modal en cualquier ancho.

**Independent Test**: Con un usuario que tiene posiciones, se verifica que *Ver detalles* abre el panel con las cinco secciones de información, que el panel se cierra restituyendo la grilla y que su comportamiento en pantalla estrecha mantiene la semántica de foco, cierre y contenido inerte (escenarios 23 a 28c de `quickstart.md`).

### Validación de User Story 3 (sin tests de frontend)

> El frontend está exento de testing. Esta historia **no crea tests ni infraestructura de testing**. La verificación comprende el control ejecutable permitido —`npm run build` desde `frontend/`, a cargo del usuario— y los escenarios de `specs/017-view-personal-portfolio-ui/quickstart.md` §3.5.

### Implementation for User Story 3

- [X] T032 [US3] Implementar el panel lateral de detalle en `frontend/src/features/portfolio/components/PortfolioPositionDetail/PortfolioPositionDetail.tsx` y su `PortfolioPositionDetail.module.scss`: cabecera fija con título y control de cierre, `role="dialog"` y `aria-modal`, foco confinado y devuelto al cerrar, cierre con `Escape`, contenido de fondo inerte, y ancho completo por debajo de 900 px con desplazamiento propio del cuerpo (depende de T020)
- [X] T033 [US3] Añadir el estado de la posición abierta y las acciones de abrir y cerrar el panel en `frontend/src/features/portfolio/hooks/pages/usePortfolioHoldingsPage.ts`, conservando el filtro, la página y el orden al cerrarse (depende de T029)
- [X] T034 [US3] Integrar el panel en la Page de tenencias en `frontend/src/features/portfolio/pages/PortfolioHoldingsPage/PortfolioHoldingsPage.tsx`, con la acción *Ver detalles* de cada tarjeta y la marca de inactividad del contenido de la pestaña mientras el panel está abierto (depende de T030, T032, T033)

**Checkpoint**: Las tres historias son funcionales de forma independiente

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia transversal, documentación y verificación de la feature

- [X] T035 [P] Actualizar la lista de rutas privadas y la sección de navegación en `docs/frontend/architecture.md` §1.12 con `/portfolio` y `/portfolio/holdings`
- [X] T036 [P] Comprobar en `frontend/src/styles/abstracts/` que los cinco roles nuevos alcanzan el contraste registrado en `plan.md` §12 sobre las tres superficies oscuras donde se usan: tarjeta `#1e293b`, fondo `#111b40` y panel `#0f172a`
- [ ] T037 Ejecutar el control ejecutable permitido `npm run build` desde `frontend/` y registrar el resultado; la ejecución corresponde al usuario
- [ ] T038 Recorrer los escenarios de `specs/017-view-personal-portfolio-ui/quickstart.md` y registrar la evidencia de cada uno, incluidos los de límites de alcance —§3.8—, consignando como no ejecutados los que no puedan aplicarse por falta de datos
- [X] T039 Revisar el diff final, `git diff --check` y `git status --short`, y comprobar que ninguna tarea del proyecto ajeno a esta feature se ha alterado

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - El panel se integra en la Page de tenencias, de modo que T034 requiere T030; el componente del panel (T032) y el estado del Page Hook (T033) no dependen de la Page

### Within Each User Story

- Sin tests de frontend: la verificación es el build permitido y los escenarios de `quickstart.md`
- Models and DTOs before Mapper and Service
- Mapper before Service, Service before Query Hooks, Query Hooks before Page Hooks
- Page Hooks before Pages, Pages before route registration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- T018 y T019, los dos Query Hooks, pueden ejecutarse en paralelo entre sí una vez completada T017
- T021, el componente de retrato, requiere T002 completada; a partir de ahí corre en paralelo con T020
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members, con la salvedad de T034

---

## Parallel Example: User Story 1

```bash
# Componentes y Page Hook en paralelo, sin tests de frontend:
Task: "Implementar el componente de indicador destacado en frontend/src/features/portfolio/components/PortfolioMetricCard/"
Task: "Implementar el Page Hook del resumen en frontend/src/features/portfolio/hooks/pages/usePortfolioSummaryPage.ts"

# Verificación de la historia, a cargo del usuario:
Task: "npm run build desde frontend/ y escenarios 1 a 3, 6 a 9 y 29 a 32 de quickstart.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3, salvo T034, que requiere la Page de tenencias de US2
3. Stories complete and integrate independently

---

## Notes

### Registro de IMPLEMENT — 2026-10-21

- Autorización: el usuario autorizó implementar las 39 tareas y reiteró Service DTO tipado,
  Mapper de transformación, nulabilidad de SPEC-014, A1 y prohibición de builds/tests/commits/push/merges.
- T001–T036 y T039 implementadas o revisadas por lectura de código, imports, contratos y diff.
  Los marcadores no acreditan compilación ni aceptación funcional. Los checkpoints de US1, US2 y
  US3 tienen revisión estática; su comprobación ejecutable y visual permanece pendiente del usuario.
- A1 respetada: la ruta y acción hacia tenencias se añadieron durante US2, después del resumen.
- El panel usa `dialog.showModal()` para la modalidad nativa (fondo inerte y foco confinado), con
  Escape, restitución de foco y bloqueo de scroll restaurado al desmontar. No requiere paquete nuevo.
- T036: los valores declarados en tokens y roles coinciden con los contrastes documentados durante
  PLAN: positivo 8,40:1, negativo 5,29:1, advertencia 8,76:1, error 7,74:1 y borde 3,07:1
  sobre tarjeta; sobre fondo/panel los contrastes son mayores. No se ejecutó medición en navegador.
- T037 y T038 quedan sin marcar: no se ejecutaron build, tests, aplicación ni escenarios manuales.

### Registro de fidelidad visual — 2026-10-21

- Las dos pantallas se reconstruyeron a partir de `referencias-imagenes/01-mi-portfolio.svg` y
  `02-mis-tenencias.svg`, que pasan a ser la fuente de verdad visual: estructura, distribución,
  proporciones, colores, tipografía, íconos, bordes, radios y espaciados.
- La paleta de la referencia se incorporó como primitivos y roles semánticos propios de Cartera en
  `styles/abstracts/tokens/_colors.scss` y `styles/abstracts/themes/_default.scss`; ningún
  componente escribe hexadecimales.
- Se añadió `PortfolioTokenDistribution` para la barra de distribución de tokens, y se registró la
  sección *Actividad* con el total de operaciones, como agregado de presentación sobre contadores del
  backend y no como regla financiera.
- La comparación se hizo con bancos visuales en `/tmp`, renderizados a 1635 px contra las
  referencias, fuera del repositorio y sin tocar el proyecto.
- Diferencias que persisten y su causa: la etiqueta «Posiciones abiertas» en lugar de «Posiciones»,
  exigida por DC-09; la ausencia de la insignia «Datos de ejemplo» y de la coletilla «Datos
  ficticios», que son artefactos de maqueta con datos reales; el avatar genérico de la aplicación en
  lugar de la silueta de la maqueta, conforme a DC-06; los iconos de métrica de la tarjeta como
  iconos lucide semánticos, porque la maqueta reutiliza un glifo genérico para dos métricas; y la
  paginación como cápsula compartida de la aplicación, porque `shared/components/Pagination` no
  puede modificarse sin afectar al catálogo de jugadores.
- T038 permanece sin marcar hasta la aprobación visual del responsable del proyecto.

### Corrección de compilación — 2026-10-21
- Error reportado por el usuario al levantar el frontend: `ds.$surface-portfolio-page-gradient.from`
  devolvía `Expected digit`. Sass no admite acceso a mapas con notación de punto.
- Sustitución: los degradados se expresan con roles escalares —`$surface-portfolio-page-gradient-from`
  y `-to`, `$surface-portfolio-card-gradient-from` y `-to`— en lugar de mapas con `map.get`.
- En la misma revisión se detectó que faltaba definir el rol `$surface-portfolio-track-fill`, se
  eliminaron los `!important` que contrarían las convenciones de SCSS, trasladando el modificador
  de estado a la fila que lo contiene, y se ajustó la tarjeta de indicador a la geometría verificada
  contra la referencia.
- Verificación estática: los 70 roles usados por la feature están definidos, el balanceo de llaves y
  paréntesis es correcto en los 9 archivos SCSS y en los 40 archivos TS/TSX de la feature, y no hay
  imports sin uso. No se ejecutó el build: la comprobación corresponde al usuario.
- `openedAt` conserva el tipo no nulo del DTO HTTP real; nombre, equipo y posición admiten nulos.
- Cambios previos de infraestructura SDD y referencias SVG conservados sin incorporarlos a commits.
- Informe y orden de revisión: `implementation-report.md`. CODE REVIEW, TESTING y cierre pendientes.

### Alineación con las reglas de arquitectura y convenciones — 2026-10-21

- Se documentaron en `docs/frontend/architecture.md` y `docs/frontend/conventions.md` las reglas que
  faltaban: tipos de props locales y tipos de UI compartidos (§1.1 y §1.4), props declaradas por el
  propio Component y encapsulación de estilos (§1.4), nombre de la interfaz local de props y bloque
  BEM derivado del nombre del componente (convenciones §React y §SCSS). La centralización de íconos
  ya estaba documentada desde el turno anterior. La Constitución no se modificó: ya contiene los
  principios generales correspondientes.
- Coherencia del código con esas reglas, tras el refactor de la sección de valores:
  - Los bloques BEM de `PortfolioValueHeader`, `PortfolioValueRow`, `PortfolioValueRows` y
    `PortfolioValueSection` estaban en `camelCase` y ahora siguen la convención `kebab-case` del
    repositorio, con el nombre del componente.
  - Los modificadores `&_positive`, `&_negative` y `&__body_withAside` pasaron a modificadores BEM
    `&--positive`, `&--negative` y `&__body--with-aside`; el elemento `&__bodyText` pasó a
    `&__body-text`.
  - La forma de la fila estaba declarada tres veces —`types/ui.ts`, las props de `PortfolioValueRows` y
    las de `PortfolioValueRow`— y ahora se declara una sola vez en `types/ui.ts`, se expone por el
    barrel de `types/` y se consume desde allí: se eliminó el deep import a `types/ui`.
  - `PortfolioValueRow.module.scss` importaba `styles//abstracts/tokens/colors` con una barra doble y
    por el archivo de tokens en lugar del índice de abstracts; sus roles de tema no existían en ese
    ámbito. Ahora usa el índice de abstracts, como el resto de la feature.
  - Se eliminó el selector de elemento desnudo `svg` de la tarjeta de posición y la regla muerta
    `span` del filtro, en favor de clases BEM de elemento.
- El plan (`plan.md` §7 y §Project Structure) y esta tarea T020 se actualizaron para reflejar la
  estructura real de la feature: diez componentes, un tipo de UI compartido en `types/ui.ts` y la
  separación de la sección, su cabecera, su lista de filas y su fila. Se conservaron los identificadores
  de las tareas: ninguna estaba iniciada y renumerar rompería la traza del avance ya registrado.
- T037 y T038 siguen sin marcar: la comprobación de compilación y la aprobación visual corresponden al
  usuario.

### Corrección del desajuste entre TSX y SCSS — 2026-10-21

- **Síntoma**: en `/portfolio` los cuatro indicadores superiores perdieron el contenedor de tarjeta
  —sin borde, sin fondo, sin alto fijo— y el mosaico de icono dejó de estar posicionado en absoluto,
  de modo que el icono se apiló sobre la etiqueta en lugar de sitting a su izquierda.
- **Causa**: `PortfolioMetricCard.tsx` pedía las clases con notación de punto sobre nombres
  `camelCase` —`styles.portfolioMetric`, `styles.portfolioMetric__label`— mientras que su
  `PortfolioMetricCard.module.scss` las definía en `kebab-case` —`.portfolio-metric`,
  `.portfolio-metric__label`—. Como el módulo CSS exporta la clave local de cada clase, ninguna
  consulta resolvía: el acceso devolvía `undefined`, el `<article>` quedaba sin clase y toda la
  tarjeta perdía sus estilos. Los modificadores de estado fallaban del mismo modo: el SCSS usaba
  `&--current` y el TSX pedía `__value_current`.
- **Corrección**: se aplicó la convención de acceso por notación de punto en toda la feature —clases
  en `camelCase`, `__` para elementos y `_` para estados—, de modo que las 82 referencias de clase
  resuelven y no queda ningún acceso con corchete. Las clases dinámicas se sustituyeron por mapas de
  clases estáticas por variante, que evitan interpolar el nombre.
- La convención quedó documentada en `docs/frontend/conventions.md` §SCSS, con la regla de que el
  objeto de estilos se accede siempre con notación de punto y, por tanto, el nombre de la clase debe
  ser un identificador válido.
- **Pendiente de decisión**: el resto de la aplicación —los componentes de `features/players` y de
  `shared/components`— sigue con clases en `kebab-case` y acceso por corchete. Migrarlos a la misma
  convención es una tarea aparte que toca funcionalidades existentes y no se ejecuta en esta.

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- No se crean tests de frontend ni infraestructura de testing: la exención temporal de la Constitución sigue vigente
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- **Renumeración**: la incorporación del componente `PortfolioPortrait` como T021 desplazó los
  identificadores anteriores. La numeración sigue siendo consecutiva y sin huecos —T001 a T039— y
  **ninguna tarea estaba iniciada**, de modo que no se perdió avance. Las referencias de dependencia
  internas se actualizaron en su totalidad.
- **Entrega diferida de la acción hacia *Mis tenencias***: se entrega en el incremento de US2 (T031),
  junto con la ruta y la pestaña que son su destino. El requisito FR-011 no cambia; el criterio de
  prueba independiente de US1 se ajustó en consecuencia y la decisión quedó registrada en la sesión de
  Clarify de `spec.md`.

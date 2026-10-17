---

description: "Task list template for feature implementation"
---

# Tasks: Detalle individual del jugador

**Input**: Design documents from `/specs/009-player-detail/`
**Branch**: `feat/009-player-detail`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Se crean tests backend de Controller, Service y persistencia conforme a `docs/backend/testing.md` §8; su ejecución corresponde al usuario. El frontend está exento de tests por norma del proyecto: MUST NOT crear tests ni infraestructura de testing frontend.

**Workflow local**: Este archivo es el registro de Tasks del ciclo SDD. Las tareas no se convierten automáticamente en tarjetas de Trello. Generar Tasks no habilita READY ni Implement: requiere Analyze sin blockers y autorización humana de implementación.

**Constitution**: v7.0.0 vigente. Principios aplicados: reutilización sobre dependencia nueva (el gráfico es SVG propio, sin librería), un único endpoint nuevo (`GET /api/players/{id}`), autenticación con el sistema vigente, sin exposición de datos de jugadores no visibles, código y endpoints en inglés con documentación e interfaz en español, y separación estricta entre identidad real (backend) y datos de demostración (frontend).

**Decisiones de Clarify que las tareas deben preservar**:

1. Solo jugadores activos visibles; inactivos responden 404 sin revelar identidad.
2. Temporada de demostración fija `2025/2026`, sin selector ni cambio automático.
3. Treinta cotizaciones, una por día consecutively durante un período financiero fijo, independiente del reloj del navegador.
4. Tarjeta compacta «Rendimiento deportivo» en lugar del radar, sin percentiles ni barras normalizadas.
5. Navbar horizontal superior con breadcrumb, aplicada solo al detalle; sin adoptar en otras páginas.
   **Punto 5 DEROGADO por la decisión de producto del 2026-10-13**: la navbar se aplica ahora a todas las
   rutas privadas (`/home`, `/players`, `/players/:id`), conforme a FR-018 y a la tarea `SPEC-009/T032`,
   que sustituye la restricción de aplicarla únicamente a `/players/:id`. El breadcrumb sigue siendo
   exclusivo del detalle. Los puntos 1 a 4 permanecen vigentes.

**Límites transversales**: no modificar SPEC-008 ni su trabajo paralelo de scraping; no implementar `GET /api/players/{id}/quotes` ni endpoints estadísticos o financieros; no introducir datos de demostración en el backend ni persistirlos; no tocar código de SPEC-005 más allá del valor compartido de las estadísticas de muestra y su etiqueta.

## Format: `[ID] [P?] [Story] Description`

- **[ID]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- **Alcance de los identificadores**: `T001`…`T047` son **locales a esta SPEC**: un `Txxx` desnudo de este
  archivo se lee siempre como `SPEC-009/Txxx`. Cuando un documento de cualquier SPEC cite una Task externa,
  debe usar el identificador completo `SPEC-XXX/TYYY`; en particular `SPEC-005/T031` y `SPEC-005/T047`
  (verificaciones visuales pendientes del catálogo). `SPEC-005/T007` y `SPEC-005/T011` conservan alcance
  derogado dentro de SPEC-005 y no son antecedente de las tareas de esta SPEC.
- Rutas exactas incluidas en cada tarea. Los archivos nuevos se crean en el repositorio existente; no se generan(features) nuevas.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Piezas deterministas y constantes compartidas por las fuentes de demostración del detalle y del catálogo.

- [x] T001 [P] Crear la función hash determinista de enteros en `frontend/src/features/players/utils/demoHash.ts` y exportarla desde `frontend/src/features/players/utils/index.ts`; sin `Math.random()`, sin `Date.now()` y sin dependencia regional
- [x] T002 [P] Crear las constantes de demostración en `frontend/src/features/players/constants/demoData.ts`: temporada `2025/2026`, período financiero fijo de treinta fechas consecutivas en formato `yyyy-MM-dd` (constante, no relativa al reloj), etiqueta «Datos de demostración», unidad «Créditos» y mensajes de rótulo; exportarlas desde `frontend/src/features/players/constants/index.ts`
- [x] T003 [P] Declarar en `frontend/src/features/players/types/models.ts` el tipo `PlayerDetail` con los campos de `PlayerDetail` de `specs/009-player-detail/data-model.md` (los mismos once campos de `Player` más `active: true` ya presente, sin duplicar el contrato del catálogo) y los tipos `PlayerSampleSeason`, `TokenQuotePoint`, `TokenQuoteSnapshot` y `TokenQuoteSeries` con sus invariantes (incluidos `isDemo`, `null`/`unknown` para serie vacía y punto único); exportarlos desde `frontend/src/features/players/types/index.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Fuente única de datos de demostración compartida por catálogo y detalle. Bloquea a US2 y US3.

- [x] T004 [P] Crear el generador de estadísticas de demostración en `frontend/src/features/players/demo/playerSampleSeason.ts`: función pura `playerId` + temporada → `PlayerSampleSeason` con `matches = 0` implica goles, asistencias y minutos `0`, enteros no negativos, sin unicidad matemática por métrica
- [x] T005 [P] Crear el generador de cotizaciones de demostración en `frontend/src/features/players/demo/tokenQuoteSeries.ts`: serie de treinta puntos diarios del período fijo de `demoData.ts`, ordenados cronológicamente, y `TokenQuoteSnapshot` derivado del último y penúltimo punto (`null` y `unknown` con serie vacía o de un punto, `percentageChange` `null` con precio anterior `0`)
- [x] T006 Migrar el valor compartido de estadísticas de muestra del catálogo en `frontend/src/features/players/constants/playerSampleStatistics.ts` y `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` para consumir el generador de T004 en lugar de los valores fijos comunes 24/8/5, conservando las etiquetas Partidos, Goles y Asistencias y sin alterar la estructura del catálogo; crear en este mismo paso el barrel `frontend/src/features/players/demo/index.ts` que reexporta únicamente los generadores de T004 y T005, de modo que el módulo de demostración quede único y las importaciones posteriores no editen este archivo

**Checkpoint**: la frontera de datos simulados está centralizada y es sustituible sin tocar componentes (FR-012).

---

## Phase 3: User Story 1 - Consultar la identidad real del jugador (Priority: P1) 🎯 MVP

**Goal**: Entregar el endpoint autenticado `GET /api/players/{id}` con visibilidad de catálogo y la pantalla privada `/players/:id` que muestra la identidad real con sus estados distinguibles.

**Independent Test**: Con sesión válida, abrir un jugador activo desde el catálogo y por URL directa, recargar y comparar la identidad con el catálogo; repetir con visitante, id con formato inválido, id inexistente, jugador inactivo y fallo de comunicación.

### Tests for User Story 1 (backend)

> Los tests se crean; su ejecución y la evidencia thereof corresponden al usuario.

- [x] T007 [US1] Extender `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` con los casos de `GET /api/players/{id}`: `200` con todos los campos y nulos, `400` por formato inválido, `401` sin JWT, `404` para inexistente y para inactivo con el mismo cuerpo, y ausencia de interacción con `PlayerCatalogService` en los rechazos previos. Responsabilidad: assertions de status, estructura y contenido; sin documentación REST Docs
- [x] T008 [US1] Sobre los mismos casos ya afirmados en T007, documentar con Spring REST Docs en `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` los snippets de éxito, `400`, `401` y `404`, y añadir las secciones correspondientes en `backend/src/docs/asciidoc/index.adoc` según `docs/backend/testing.md` §8; no duplicar snippets ni reescribir assertions. Depende de T007 porque comparte archivo
- [x] T009 [P] [US1] Extender `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` con el caso de jugador activo visible, inexistente e inactivo, afirmando el resultado y la excepción de aplicación sin acceder a Repository

### Backend for User Story 1

- [x] T010 [P] [US1] Crear `backend/src/main/java/footballmarket/services/exceptions/PlayerNotFoundException.java` extendiendo `footballmarket.exceptions.DomainException`, como excepción de aplicación propia para la ausencia de un jugador visible, con mensaje en español y sin datos del jugador
- [x] T011 [US1] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` la consulta individual con `@Query` JPQL y `@EntityGraph(attributePaths = {"team", "team.league"})` que replique exactamente el predicado de visibilidad del catálogo `findByActiveTrue` (`p.active = true`, `join p.team t`, `t.current = true`), devolver `Optional<Player>` y cargar `team` y `team.league`; no introducir un método derivado que omita las asociaciones ni altere la consulta vigente del catálogo
- [x] T012 [US1] Declarar en `backend/src/main/java/footballmarket/services/PlayerCatalogService.java` y implementar en `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` la consulta individual que delega en T011 y lanza `PlayerNotFoundException` cuando no hay coincidencia, con las asociaciones `team` y `team.league` ya cargadas por el `EntityGraph` y sin lógica de visibilidad en el Controller
- [x] T013 [US1] Registrar en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` el manejador de `PlayerNotFoundException` con `404`, `ErrorResponseDTO` y código estable `PLAYER_NOT_FOUND`, sin tocar los `502` existentes de proveedores externos
- [x] T014 [US1] Añadir en `backend/src/main/java/footballmarket/controllers/PlayerController.java` el endpoint `GET /api/players/{id}` con `id` como `Long`, reutilizando `PlayerMapper`, la anotación de seguridad vigente y anotaciones OpenAPI para `200`, `400`, `401` y `404`

### Frontend for User Story 1

- [x] T015 [P] [US1] Añadir en `frontend/src/features/players/players.service.ts` el método `getById` con `authenticated: true` y ruta relativa `/players/{id}`, y en `frontend/src/features/players/players.mapper.ts` la validación y conversión `PlayerResponseDTO → PlayerDetail` reutilizando `isPlayerResponseDTO` y fijando `active: true`; `PlayerDetail` es el tipo declarado en T003 y no el modelo `Player` de la lista paginada
- [x] T016 [P] [US1] Añadir la clave de detalle en `frontend/src/features/players/constants/playersQueryKeys.ts` y crear `frontend/src/features/players/hooks/queries/usePlayerDetailQuery.ts` con TanStack Query, `signal` y clave por `id`; exportar el hook desde `frontend/src/features/players/hooks/queries/index.ts` y `frontend/src/features/players/hooks/index.ts`
- [x] T017 [P] [US1] Crear `frontend/src/features/players/components/PlayerPortrait/PlayerPortrait.tsx`, su `PlayerPortrait.module.scss`, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`, con el orden retrato principal → alternativo → icono genérico y `alt` textual
- [x] T018 [P] [US1] Crear `frontend/src/features/players/components/PlayerBreadcrumb/PlayerBreadcrumb.tsx`, su módulo SCSS, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`, con «Jugadores > nombre real», retorno funcional a `/players` y sin nombre en carga, error o 404
- [x] T019 [P] [US1] Crear `frontend/src/features/players/components/PlayerOverview/PlayerOverview.tsx`, su módulo SCSS, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`, con la ficha azul dominante, la franja de indicadores y «No disponible» para opcionales ausentes
- [x] T020 [US1] Crear `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx`, `PlayerDetailPage.module.scss` e `index.ts`, con `usePlayerDetailPage` en `frontend/src/features/players/hooks/pages/usePlayerDetailPage.ts` distinguiendo `loading`, `error` con reintento, `invalid-id` (400 o id no numérico), `not-found` (404) y `ready`; mostrar las secciones de demostración solo en `ready` y añadir `PlayerDetailPage` al barrel `frontend/src/features/players/pages/index.ts` y `frontend/src/features/players/index.ts`. Debe ignorar toda respuesta cuya consulta ya no sea la vigente (id distinto del actual): ninguna identidad, estadística ni precio de un identificador anterior puede renderizarse, y la clave de T016 por `id` debe impedir además la reutilización de caché cruzada
- [x] T021 [US1] Registrar en `frontend/src/app/router/AppRouter.tsx` la ruta privada `/players/:id` con el `AuthGuard` vigente, y en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` el enlace navegable a `/players/{id}` manteniendo el diseño y los comportamientos actuales de la tarjeta. Esta es la primera edición de `PlayerCard.tsx` dentro de US1 y sigue a T006; T025 edita el mismo archivo en US2

**Checkpoint**: US1 es funcional y verificable por sí sola, sin datos de demostración obligatorios.

---

## Phase 4: User Story 2 - Consultar estadísticas de demostración coherentes (Priority: P2)

**Goal**: Mostrar Partidos, Goles, Asistencias y Minutos de 2025/2026 en la tarjeta «Rendimiento deportivo» y en los resúmenes inferiores, con valores deterministas y coincidentes con el catálogo.

**Independent Test**: Anotar las métricas de al menos dos jugadores, recargar tres veces, cambiar la fecha del navegador y contrastar los valores con las tarjetas del catálogo.

- [x] T022 [US2] Crear `frontend/src/features/players/hooks/queries/usePlayerSampleSeasonQuery.ts` que recibe solo un `playerId` ya resuelto, invoca la función centralizada de `features/players/demo` creada en T004 y devuelve `PlayerSampleSeason` con `enabled` ligado a la identidad resuelta; exportarlo desde `frontend/src/features/players/hooks/queries/index.ts`
- [x] T023 [US2] Crear `frontend/src/features/players/components/PlayerSportsSummary/PlayerSportsSummary.tsx`, su módulo SCSS, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`: título «Rendimiento deportivo», Partidos, Goles, Asistencias, Minutos, temporada `2025/2026` y rótulo «Datos de demostración», sin radar, percentiles ni barras
- [x] T024 [US2] Componer en `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx` la tarjeta de T023 en la zona superior derecha y las estadísticas inferiores derechas con los mismos valores que la franja de indicadores, sin selector de temporada y sin recalcular cifras en el componente
- [x] T025 [US2] Marcar en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` las métricas como datos de demostración, conservando el resto del catálogo sin filtros nuevos ni rediseño (FR-011 y FR-015). Tercera y última edición de `PlayerCard.tsx`; se ejecuta después de T006 (Foundational) y T021 (US1) sin editar el archivo en paralelo

**Checkpoint**: US2 funciona de forma independiente y comparte valores con el catálogo.

---

## Phase 5: User Story 3 - Consultar cotización y evolución de demostración (Priority: P2)

**Goal**: Mostrar la cotización actual del token en créditos con su variación y la evolución de treinta cotizaciones diarias, con estados excepcionales explícitos.

**Independent Test**: Revisar fechas, precios, último punto y variación de dos jugadores; recargar y cambiar la fecha del navegador; provocar serie vacía, punto único y base cero.

- [x] T026 [P] [US3] Crear `frontend/src/features/players/hooks/queries/usePlayerTokenQuoteQuery.ts` que invoca la función centralizada de `features/players/demo` creada en T005 y devuelve `TokenQuoteSeries` con su `snapshot`, ligado a la identidad resuelta; exportarlo desde `frontend/src/features/players/hooks/queries/index.ts`
- [x] T027 [US3] Crear `frontend/src/features/players/components/PlayerQuotePanel/PlayerQuotePanel.tsx`, su módulo SCSS, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`: precio del token en créditos, fecha, variación absoluta y porcentual con signo y etiqueta textual, rótulo «Datos de demostración» y mensajes para serie vacía, punto único y variación no disponible
- [x] T028 [P] [US3] Crear `frontend/src/features/players/components/PlayerQuoteEvolutionChart/PlayerQuoteEvolutionChart.tsx`, su módulo SCSS, su `index.ts` y su exportación en `frontend/src/features/players/components/index.ts`: gráfico de línea en SVG sin dependencia nueva, puntos en orden cronológico y representación textual o tabla equivalente con todas las fechas y precios
- [x] T029 [US3] Componer en `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx` el panel de cotización y el gráfico de evolución en la zona inferior izquierda, dejando visible que el último punto coincide con la cotización actual y que la variación procede de las dos últimas cotizaciones

**Checkpoint**: US3 funciona de forma independiente y no inventa datos en los estados excepcionales.

---

## Phase 6: User Story 4 - Utilizar un dashboard adaptable con navegación real (Priority: P2)

**Goal**: Presentar la jerarquía oscura de la referencia con navbar horizontal superior y breadcrumb, menú desplegable accesible y ausencia de desbordamiento horizontal a 360, 768 y 1280 píxeles.

**Independent Test**: Comparar la estructura con `referencias-imagenes/football_market_referencia_dashboard.png` en escritorio y recorrer la pantalla a 1280, 768 y 360 píxeles usando teclado y puntero.

- [x] T030 [P] [US4] Crear el componente presentativo de navegación reutilizable en `frontend/src/shared/components/Navbar/` (`Navbar.tsx`, `Navbar.module.scss`, `index.ts` y `navItems.ts`): marca a la izquierda, enlaces `/home` y `/players` mediante `NavLink`, sector de usuario y menú desplegable de tablet y móvil con apertura y cierre reales, operable por teclado, foco visible y estado abierto/cerrado perceptible. La decisión de producto del 2026-10-13 sustituye el nombre previsto `AppNavbar` y confirma que la sección activa se deriva del enrutado en lugar de recibir `activeSection` como propiedad
- [x] T031 [US4] Crear `frontend/src/app/layouts/AppLayout/` que compone la navegación con la identidad de sesión mediante la API pública de `features/auth` (`useCurrentUserQuery` y `useLogout`), exportando `useLogout` desde el barrel de auth sin alterar su comportamiento. La decisión de producto del 2026-10-13 sustituye el nombre previsto `AppNavbarLayout`
- [x] T032 [US4] Aplicar el layout de aplicación en `frontend/src/app/router/AppRouter.tsx` a **todas las rutas privadas** (`/home`, `/players`, `/players/:id`). La decisión de producto del 2026-10-13 sustituye la restricción de aplicarla únicamente a `/players/:id` y actualiza FR-018
- [x] T033 [US4] Implementar en `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.module.scss` el tema oscuro, la ficha azul dominante, la tarjeta «Rendimiento deportivo» superior derecha, la evolución inferior izquierda, las estadísticas inferiores derechas y la adaptación sin desplazamiento horizontal a 360, 768 y 1280 píxeles, sin barra lateral
- [x] T034 [US4] Aplicar en `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx` y en los componentes de T017 a T030 los ajustes de accesibilidad de FR-020 hasta cumplir estas condiciones observables: (1) enlaces Inicio/Jugadores, retorno del breadcrumb, cierre de sesión y apertura/cierre del menú se activan con teclado; (2) cada elemento interactivo muestra foco visible; (3) la imagen del jugador tiene alternativa textual; (4) los estados de carga, error y «no encontrado» se anuncian como texto; (5) el gráfico expone fechas y precios como texto o tabla, sin depender solo del color; (6) nombres largos se recortan o ajustan sin invadir tarjetas vecinas. El criterio de aceptación es observable en el navegador y se registra en la evidencia de T037

**Checkpoint**: US4 es funcional y verificable por sí sola.

> Nota de ejecución 2026-10-12: T034 queda implementada en código. Su aceptación es observable en el
> navegador y se confirma con la evidencia humana de T037, que permanece pendiente.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T035 [P] Documentar en `docs/frontend/architecture.md` el componente transversal `AppNavbar` y su composición desde `app`; consolidar en `specs/009-player-detail/contracts/player-detail.md` el anexo de integración pendiente de cotizaciones y estadísticas ya existente como no implementado, sin presentar contratos como APIs disponibles (FR-021); y registrar de forma explícita en `specs/009-player-detail/tasks.md` y en la entrega que el cumplimiento de REQ-S2-12 es **parcial**: esta versión no consume historial real ni implementa `GET /api/players/{id}/quotes`, sin afirmar que la demostración satisfaga el requisito académico completo (FR-022)
- [ ] T036 Ejecutar los únicos controles ejecutables permitidos tras los cambios de código: `./gradlew build -x test` desde `backend/` y `npm run build` desde `frontend/`; MUST NOT ejecutar tests. **Pendiente de evidencia**: el build se declara Realizado en el árbol de trabajo, pero la rama no contiene commits ni registro que lo acredite. Desmarcado el 2026-10-13; no se acepta como cumplido sin salida verificable
- [ ] T037 [P] Solicitar al usuario la evidencia de ejecución de la suite backend (`./gradlew test` desde `backend/`) y de los escenarios manuales de `specs/009-player-detail/quickstart.md`; registrar los resultados observados y dejar los pendientes sin marcar como cumplidos. **Continúa pendiente**

### Phase 8: Sincronización documental posterior al rediseño (2026-10-13)

**Purpose**: Registrar la decisión de producto que conserva el rediseño manual del frontend y sincronizar la documentación SDD sin tocar código.

- [x] T038 [P] Registrar en `specs/009-player-detail/spec.md` y `specs/005-players-page/spec.md` la sesión de decisiones del 2026-10-13: navegación global en todas las rutas privadas, catálogo responsive, rediseño de la tarjeta, paginación ±1, layouts y componentes compartidos, capa de estilos y tokens, escudos y banderas, estadísticas deterministas y diseño del detalle
- [x] T039 [P] Sustituir en ambas specs los requisitos afectados conservando los no afectados, y documentar cada requisito derogado con su reemplazo y con la divergencia preexistente corregida respecto de `tp/enunciado.md` §11
- [x] T040 [P] Registrar los requisitos de SPEC-005 cuya verificación funcional o visual sigue pendiente (FR-009, FR-010, FR-005/006/008/022 y sus criterios), sin derivar ni falsificar su cumplimiento
- [x] T041 [P] Sincronizar `plan.md`, `research.md` (R-11 a R-14), `data-model.md`, `contracts/`, `quickstart.md`, `docs/frontend/architecture.md` y `docs/frontend/technologies.md` con la arquitectura real de componentes, layouts, rutas, estilos, tokens y recursos gráficos
- [ ] T042 [P] Registrar en `specs/trello.md` y en las tarjetas US de SPEC-009 y SPEC-005 la ampliación de alcance aprobada el 2026-10-13. **Pendiente**: requiere autorización operativa de Trello, que no forma parte de esta operación

**Revisión documental de T042 — 2026-10-13 (conciliación posterior a la sincronización)**: el material
de T042 se ha producido en dos piezas y ambas están verificadas por lectura posterior.
(a) `specs/trello.md`: sección «Sincronización SDD ↔ Trello de plantillas, Épicas y User Stories —
2026-10-13», con las diez tarjetas, sus URLs, estados conservados, Tasks por US y gates pendientes.
(b) Tarjetas de Trello: las ocho US y las dos épicas de SPEC-005 y SPEC-009 incorporan en su descripción la
ampliación de alcance del 2026-10-13; el registro consta en el comentario `[SDD SYNC EVIDENCE]
TEMPLATES SDD ↔ TRELLO — 2026-10-13` de ambas épicas.

**Evidencia que falta para cerrar T042**: (1) autorización humana explícita del responsable para dar por
cerrada la sincronización operativa de Trello de este alcance, y (2) confirmación de esa autorización por
el usuario mediante un comentario `[SDD GATE]` en la épica de SPEC-009. Hasta entonces T042 permanece
`[ ]`. Marcarla completada sin esa autorización atribuiría al agente una validación humana que no existe, y
T042 no constituye ni sustituye CODE REVIEW, TESTING ni Converge.

**Nota de alcance — conciliación documental del 2026-10-13**: esa conciliación se ejecutó con Trello bajo
restricción de no modificación, de modo que **no** se escribió ningún comentario de gate en la épica. El
estado de T042 se registra únicamente en este archivo y en `specs/trello.md`. La persistencia del
comentario `[SDD GATE]` requiere una operación de Trello autorizada por el usuario.

**Nota de trazabilidad**: marcar una tarea como completada documenta el estado del árbol de trabajo; no acredita Code Review, Testing, autorización de gates ni existencia de commits en la rama.

### CODE REVIEW — autorización humana registrada — 2026-10-13

**Gate**: CODE REVIEW de SPEC-009.
**Declaración humana del responsable del proyecto**: «Apruebo el code review».
**Interpretación acotada**: la declaración autoriza el inicio y la aprobación humana del CODE REVIEW de
SPEC-009. **No** es en sí misma evidencia técnica ni sustituye la revisión done.
**Revisión técnica realizada en la misma fecha**: informe de revisión de código de SPEC-009, con
requisitos revisados, evidencia por requisito, defectos y severidad, riesgos y recomendación de cierre.
**Resultado de esa revisión**: el gate **NO se declara cerrado**. Existen defectos MEDIUM y HIGH que
requieren corrección en IMPLEMENT antes de que la aprobación humana pueda considerarse efectiva.
**Estado vigente**: US01–US04 permanecen en **CODE REVIEW**; ninguna pasa a TESTING.
**Pendiente de persistencia**: el comentario `[SDD GATE] CODE REVIEW / APPROVED` en la épica y en cada US
de SPEC-009. Esa escritura no se realizó en esta operación porque se ejecutó con Trello bajo restricción
explícita de no modificación. Constitution §8 exige que la evidencia del gate esté persistida; sin ella,
ninguna transición dependiente queda habilitada.

Restricciones observadas en esta operación: sin código, sin SPEC-008, sin Constitution, sin tests ni
builds, sin commits, sin push, sin cambio de rama, sin aprobar TESTING, sin Converge, sin mover tarjetas y
sin escritura en Trello.

### Corrección de hallazgos de Code Review — 2026-10-13

Operación de corrección tras el informe de Code Review. Sin aprobación de gate, sin Converge, sin escritura
en Trello, sin commits, push, cambio de rama, tests ni builds. El rediseño manual del frontend se conserva
íntegro; ninguna corrección altera una decisión de producto.

| Hallazgo | Severidad | Causa | Corrección | Archivo | Evidencia | Verificación pendiente |
| --- | --- | --- | --- | --- | --- | --- |
| H1 recursos gráficos ausentes en el detalle | HIGH → **reclasificado como desviación documental** | FR-023 está redactado como habilitación («puede acompañarse»); ninguna US de SPEC-009 los exige. La contradicción estaba en `quickstart.md`, que exigía escudo y bandera en `/players/:id`, y en la matriz de trazabilidad que atribuía `009 FR-023` a `getNationalityFlag.ts` | Corrección **solo documental**: escenario 8 de `quickstart.md` reencuadrado a la tarjeta del catálogo, con nota de revisión; matriz de trazabilidad anotada como habilitación y no display. **No se implementa**: mostrarlos exigiría una decisión de producto sobre el diseño aprobado de la ficha | `specs/009-player-detail/quickstart.md`, `specs/009-player-detail/tasks.md` | Lectura de FR-023, AC de US01 y del escenario original | Mostrar escudo y bandera en el detalle requeriría autorización de producto expresa |
| M1 fallback de retrato no reiniciado | MEDIUM → **corregido** | `PlayerPortrait` mantenía `useState` del índice de fallback y ni `PlayerDetailPage` ni `PlayerOverview` le pasaban `key`, por lo que el router reutilizaba la instancia al cambiar de jugador | Se añade `key={detail.player.id}` a `PlayerOverview`, que remonta el subárbol y reinicia el índice. Mismo patrón ya usado en `PlayerCard.tsx:108` | `frontend/src/features/players/pages/PlayerDetailPage/PlayerDetailPage.tsx` | Corrección verificable por lectura | Navegar de un jugador sin imágenes válidas a otro con retrato válido y comprobar que este muestra su propia imagen |
| M2 formato de `AppLayout` | MEDIUM → **corregido** | 19 líneas con indentación de espacios frente a `indentStyle: "tab"` de `frontend/biome.jsonc` | Archivo reescrito con tabulación; contenido funcional idéntico | `frontend/src/app/layouts/AppLayout/AppLayout.tsx` | `grep` de indentación: 0 líneas con espacios, 19 con tabulador | `npm run biome:check` (T036), a cargo del usuario o de agente con autorización de código |
| M3 base cero no reproducible | MEDIUM → **documentado, sin cambio de código** | `tokenQuoteSeries.ts` fija `MIN_PRICE = 40`, de modo que la rama `previous.price === 0` de `buildSnapshot` es defensiva y no observable en la interfaz | **No se altera el generador determinista aprobado.** Se documenta en `quickstart.md` la imposibilidad y una verificación controlada de la rama, aislada y a cargo del usuario | `specs/009-player-detail/quickstart.md` | Lectura de `tokenQuoteSeries.ts:9,50-51` | Verificación controlada de `buildSnapshot` con penúltimo precio `0` |
| M4 duplicación de logout | MEDIUM → **descartado** | `Navbar.tsx` renderiza dos botones «Cerrar sesión», pero `Navbar.module.scss:226-228` deja `__mobileLogout` en `display: none` y `:283-285` oculta `__logout` por debajo de `$bp-tablet`. Ambos son excluyentes: nunca coexisten visibles | **Sin corrección**: no existe duplicación real | — | Lectura de `Navbar.module.scss` y de los breakpoints | Confirmar en navegador a 1280, 768 y 360 px que solo hay un control visible |

### Observaciones LOW: corrección frente a deuda técnica

| Observación | Decisión | Motivo |
| --- | --- | --- |
| L1 prop `loadingQuotes` muerta en `PlayerOverview` | **Deuda técnica** | Eliminar una prop no usada es cosmético; no afecta comportamiento ni accesibilidad. Anotada para limpieza futura |
| L2 una petición a `/api/users/me` por navegación | **Deuda técnica aceptada** | Es diseño deliberado: detecta sesión vencida en cada cambio de ruta, escenario de `quickstart.md`. Optimizarlo exigiría revisar esa garantía |
| L3 menú desplegable sin cierre con `Escape` ni clic exterior | **Deuda técnica** | `aria-expanded` y `aria-controls` están presentes y el conmutador es operable por teclado, lo que satisface AC04. La mejora es de usabilidad, no de corrección |
| L4 avatar con la inicial del usuario | **Deuda técnica / a confirmar** | Es `aria-hidden` y decorativo, sin función de perfil. No se toca el rediseño; queda anotado por si el Out of Scope de SPEC-009 se interpreta de forma restrictiva |
| L5 `minutesPlayed = partidos × 90` exacto | **Deuda técnica** | No contradice ningún requisito; es verosimilitud del dato simulado |
| L6 `getVisiblePlayerById` sin `@Transactional(readOnly = true)` | **Deuda técnica** | Funciona por el `@EntityGraph`; es asimetría con `getActivePlayers`. Corregirlo exigiría tocar backend y build en el mismo ciclo |

### Procedimiento de retorno a IMPLEMENT

Constitution §8 y workflow §7.10: el rechazo de CODE REVIEW devuelve las US afectadas a **IN PROGRESS**
y exige una nueva revisión antes de cualquier transición. No se ha movido ninguna tarjeta porque esta
operación tenía prohibido escribir en Trello; el estado de las US en el tablero permanece CODE REVIEW y
**queda desincronizado con esta corrección**, lo que el usuario debe resolver en una operación autorizada.

### Verificación técnica posterior a las correcciones — 2026-10-13

Ejecutada por el agente dentro del control que Constitution §6 y `frontend/AGENTS.md` permiten. Sin tests,
sin Converge, sin aprobación de gates y sin escritura en Trello. **No se corrigió ningún error reportado.**

| Comando | Área | Resultado | Evidencia |
| --- | --- | --- | --- |
| `npm run biome:check` (desde `frontend/`) | Frontend | **FALLA — 30 errores en 7 archivos** | `Checked 162 files in 166ms. No fixes applied. Found 30 errors.` Diagnósticos de formato y de `assist/source/organizeImports`, ninguno de lint |
| `npm run build` (desde `frontend/`) | Frontend | **CORRECTO — EXIT=0** | `tsc -b && vite build`; `✓ built in 2.16s`; `dist/assets/index-DaOJOGxU.js 521.26 kB`. Única advertencia: chunks > 500 kB, sin efecto sobre el resultado |

Archivos con diagnóstico en `biome:check`: `src/app/layouts/AppLayout/AppLayout.tsx`,
`src/app/layouts/PageLayout/PageLayout.tsx`, `src/app/layouts/PaginatedPageLayout/PaginatedPageLayout.tsx`,
`src/shared/components/Pagination/Pagination.tsx`, `src/features/players/assets/teams/index.ts` (además
`assist/source/organizeImports`), `src/features/players/assets/teams/teams.json` y
`src/features/players/assets/teams/review.json`.

**Estado real de M2**: la indentación de `AppLayout.tsx` ya es de tabulación, pero el formateador de Biome
pide además colapsar dos llamadas multilínea que caben en 80 columnas
(`useCurrentUserQuery(true, revision, location.pathname)` y `<Navbar ... />`). M2 queda **parcialmente
corregido** y `biome:check` sigue en rojo. La corrección automática (`biome check --write`) no se ejecutó por
restricción; queda a decisión del responsable.

**Hallazgo nuevo (no detectado en el Code Review)**: el Code Review早已经 M2 como si fuera el único
problema de formato. La ejecución demuestra que hay seis archivos más con desviaciones de formato y de
imports, cuatro de ellos del rediseño manual conservado. `SPEC-005/T022` («confirmar que no se generaron
cambios versionables») también requiere atención: `frontend/tsconfig.tsbuildinfo` está versionado y el build
lo modifica; `frontend/dist/` está en `.gitignore` y no genera cambios versionables.

**Impacto sobre T036**: sigue `[ ]`. El build frontend tiene ahora salida verificable, pero el build del
backend (`./gradlew build -x test`) no se ejecutó en esta operación porque ningún cambio de este ciclo toca
código backend, y §6 limita el control del agente al área cuyo código cambió. La **suite backend
(`./gradlew test`) no se ejecuta nunca por el agente**: corresponde al usuario.

### Corrección controlada de Biome y revisión de `tsconfig.tsbuildinfo` — 2026-10-13

Operación autorizada y acotada a correcciones de formato y organización de imports. Sin tests, sin
builds adicionales, sin refactorizaciones, sin SPEC-008, sin Constitution, sin Trello, sin commits, push
ni cambio de rama. Ningún gate aprobado.

**Inventario inicial incompleto**: el informe de verificación señalaba siete archivos, pero la ejecución
reveló once. Los cuatro adicionales eran también exclusivamente de formato y se corrigieron por pertenecer
a la misma categoría y por cumplir el objetivo de la operación. Ninguno de los once.MODIFICADOS
introduce cambio de comportamiento.

| Archivo | Corrección aplicada |
| --- | --- |
| `app/layouts/AppLayout/AppLayout.tsx` | Colapso de dos llamadas multilínea que caben en 80 columnas. **Cierra M2** |
| `app/layouts/PageLayout/PageLayout.tsx` | Salto de línea en el ternario de `className` |
| `app/layouts/PaginatedPageLayout/PaginatedPageLayout.tsx` | Espacios → tabulaciones, sin cambio funcional |
| `shared/components/Pagination/Pagination.tsx` | Espacios → tabulaciones, sin cambio funcional |
| `features/players/assets/teams/index.ts` | `assist/source/organizeImports` y desglose de los arrays |
| `features/players/assets/teams/teams.json` | 2 espacios → tabulaciones. **Contenido semánticamente idéntico**, verificado por comparación de JSON |
| `features/players/assets/teams/review.json` | Ídem. Procedencia y metadatos conservados |
| `features/players/assets/teams/registry.json` | Ídem |
x de línea en JSX y ternario, sin cambio funcional |
| `features/players/pages/PlayersPage/PlayersPage.tsx` | Espacios → tabulaciones, sin cambio funcional |
| `features/players/utils/getNationalityFlag.ts` | Salto de línea en la cadena de códigos y en la cadena de normalización |

**Resultado de Biome**: `npm run biome:check` pasa de **30 errores** (7 archivos) a **18 errores**, y los 18
restantes **no están en código propio**:

| Origen | Cantidad | Naturaleza | ¿Autorizado? |
| --- | --- | --- | --- |
| `features/players/assets/teams/**/*.svg` | 16 | `lint/a11y/noSvgWithoutTitle` y `lint/correctness/noUnknownProperty` sobre **recursos de marca de terceros** | **No**: modificarlos alteraría recursos gráficos preservados |
| `scripts/import-team-logos.mjs` | 2 | `format` y `assist/source/organizeImports` | **No aplicado**: queda fuera del inventario autorizado y su corrección roza la lógica de generación |

Ningún archivo `.scss`, `.png` ni `.svg` fue tocado por esta operación: el rediseño manual y los recursos
gráficos quedan intactos.

**Causa raíz detectada del conflicto de formato en JSON**: `scripts/import-team-logos.mjs:80` escribe con
`JSON.stringify(value, null, 2)`, es decir con dos espacios, mientras Biome exige tabulaciones. Cada
reejecución del importador reintroduce los tres errores de formato. La corrección coherente exige alinear
el serializador del script con Biome, lo que **no** se ha hecho por quedar fuera del alcance autorizado.

**Estado de `frontend/tsconfig.tsbuildinfo`**: está **versionado** y el build lo modifica, pero la causa no es
una regla de ignorado ausente.

- `frontend/.gitignore:6` **sí** declara `*.tsbuildinfo`; verificado con `git check-ignore --no-index`.
- El archivo está en el índice porque se commiteó en `812d82a` («initialize frontend foundation»), es decir
  **antes** de que la regla se añadiera en `1b759f6`. Las reglas de `.gitignore` no se aplican a archivos ya
  versionados, de ahí el ` M` permanente en cada build.
- Contenido: caché de `tsc -b` (JSON de 10,8 kB, `version: 7.0.2`). El diff del build es de **una línea**:
  la lista `root` pasa de los tres archivos de entrada iniciales a incluir `app/layouts/*` y el resto del
  árbol real. Es una reconstrucción de caché, no un cambio de código.
- El archivo **no** se restauró ni se desversionó: ambas acciones requieren autorización.

**Solución recomendada, pendiente de autorización**: `git rm --cached frontend/tsconfig.tsbuildinfo`, que lo
retira del índice y lo deja cubierto por la regla existente. No se ejecutó porque prepara un commit.

### Ejecución de Implement — 2026-10-12

T001–T036 ejecutadas bajo IMPLEMENT AUTHORIZATION persistida (`6acd6322480dbfac1219bbfb` en la
épica y `6acd6323d…`, `6acd63230…`, `6acd63234…`, `6acd6324d…` en US01–US04). T037 permanece
pendiente: la ejecución de la suite backend y de los escenarios manuales corresponde al usuario,
y el agente no puede registrarla como cumplida sin resultados aportados.

Controles ejecutables ejecutados por el agente: `./gradlew build -x test` (con `spotlessApply`),
`npm run build` y `npm run biome:check`, los tres sin fallos. No se ejecutó ningún test.

Cumplimiento parcial de REQ-S2-12 (FR-022): esta versión no consume historial real ni implementa
`GET /api/players/{id}/quotes`; la demostración no satisface el requisito académico completo.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; bloquea US2 y US3.
- **US1 (Phase 3)**: depende de Setup solo por tipos compartidos; es el MVP y no depende de US2, US3 ni US4.
- **US2 (Phase 4)**: depende de Foundational (T004–T006) y de la identidad resuelta por US1.
- **US3 (Phase 5)**: depende de Foundational (T005) y de la identidad resuelta por US1.
- **US4 (Phase 6)**: depende de la composición de la pantalla en US1; su maquetación consume los componentes de US2 y US3 cuando están disponibles.
- **Polish (Phase 7)**: depende de las historias deseadas.

### User Story Dependencies

- **US1 (P1)**: sin dependencias de otras historias.
- **US2 (P2)**: integra con US1 mediante la identidad real; su fuente de datos es Foundational.
- **US3 (P2)**: integra con US1 mediante la identidad real; su fuente de datos es Foundational.
- **US4 (P2)**: integra con US1, US2 y US3 para la composición visual, sin depender de sus datos.

### Critical Path

```text
T001–T003 (Setup)
  → T004, T005 (Foundational) → T006 (barrel demo + PlayerCard: estadísticas)  [edición 1 de PlayerCard]
T010–T014 (backend US1) → T015, T016 (servicio y hook) → T017–T020 (pantalla) → T021 (ruta + PlayerCard)  [edición 2]
T004 → T022–T024 → T025 (PlayerCard: etiqueta demo)  [edición 3]
T005 → T026–T029 (US3)
T021 → T030–T034 (US4)
todas → T035–T037 (Polish)
```

Orden obligatorio sobre `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx`:
T006 → T021 → T025, siempre secuencial; ninguna se marca `[P]` respecto de otra.

### Within Each User Story

- Los tests backend se escriben antes o junto a la implementación y los ejecuta el usuario.
- Repository antes que Service; Service antes que Controller.
- Ninguna tarea marcada `[P]` comparte archivo con otra `[P]` del mismo momento; `PlayerCard.tsx` se edita en T006 → T021 → T025 de forma secuencial.
- Contrato y Mapper antes que Hook; Hook antes que componente; componente antes que composición de Page.
- Story completa antes de pasar a la siguiente prioridad.

### Parallel Opportunities

- T001, T002 y T003 en paralelo.
- T004 y T005 en paralelo (el barrel `demo/index.ts` lo crea T006, no ambas); T009 en paralelo con T007; T008 después de T007 por compartir archivo; T010 en paralelo con T007 y T009.
- T015, T016, T017, T018 y T019 en paralelo tras el contrato backend definido.
- T026, T027 y T028 en paralelo.
- T030 puede ejecutarse en paralelo con US2 y US3.

---

## Parallel Example: User Story 1

```bash
# Escribir los tests backend de US1; no ejecutarlos desde el agente:
Task: "Extender backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java con los casos de GET /api/players/{id} (T007)"
Task: "Documentar los snippets REST Docs de esos mismos casos (T008, después de T007)"
Task: "Extender backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java con los casos de visible, inexistente e inactivo (T009, en paralelo con T007)"

# Componentes de frontend de US1 en paralelo:
Task: "Crear PlayerBreadcrumb en frontend/src/features/players/components/PlayerBreadcrumb/PlayerBreadcrumb.tsx"
Task: "Crear PlayerPortrait en frontend/src/features/players/components/PlayerPortrait/PlayerPortrait.tsx"
Task: "Crear PlayerOverview en frontend/src/features/players/components/PlayerOverview/PlayerOverview.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational
3. Completar Phase 3: User Story 1
4. **STOP y validar**: `quickstart.md` escenarios de acceso, identidad y errores; build permitido; evidencia de tests a cargo del usuario
5. Continuar con US2, US3 y US4 solo con autorización vigente

### Incremental Delivery

1. Setup + Foundational → frontera de demostración lista
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. US4 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE

Cada historia aporta valor sin romper las anteriores. Implementar una combinación requiere autorización explícita; la autorización no inicia IMPLEMENT por sí sola.

### Parallel Team Strategy

1. Tras Setup y Foundational, el backend de US1 puede avanzar en paralelo con la parte puramente presentacional de US4.
2. US2 y US3 pueden repartirse una vez resuelta la identidad real de US1.
3. No repartir trabajo con SPEC-008 ni con otras features: son alcances independientes.

---

## Notes

- `[P]` = archivos distintos y sin dependencias de tareas incompletas.
- `[Story]` mapea cada tarea a su historia para trazabilidad con `spec.md`.
- No se crean tests frontend ni infraestructura de testing frontend.
- No se crean tareas de ejecución de tests: la ejecución es del usuario.
- El agente no ejecuta tests; el build sin tests es el único control ejecutable permitido.
- Commit solo con hashtags de SPEC/US/Tasks cuando exista hash real y sin push no autorizado.
- Detenerse en cada checkpoint para validar la historia de forma independiente.
- Evitar: tareas vagas, conflictos de archivo, dependencias entre historias que rompan la independencia y cualquier tarea que modifique SPEC-008, otros contratos del catálogo o el código de scraping.
---

## Matriz de trazabilidad de la decisión del 2026-10-13

| Decisión de producto | SPEC | FR | US | SC | Archivo de implementación |
| --- | --- | --- | --- | --- | --- |
| Navbar horizontal global en rutas privadas | 009 / 005 | 009 FR-018, FR-024; 005 FR-021 | 009 US4; 005 US1 | 009 SC-009, SC-011; 005 SC-009 | `frontend/src/shared/components/Navbar/Navbar.tsx`, `navItems.ts`, `frontend/src/app/layouts/AppLayout/AppLayout.tsx`, `frontend/src/app/router/AppRouter.tsx` |
| Catálogo de jugadores responsive | 005 | FR-006 | US1 | SC-003 | `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss`, `frontend/src/styles/abstracts/_breakpoints.scss` |
| Nuevo diseño de PlayerCard | 005 | FR-004, FR-005, FR-007, FR-009, FR-010 | US1, US3 | SC-002, SC-011 | `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `.module.scss` |
| Paginación con ventana ±1 | 005 | FR-014, FR-015 | US2 | SC-004 | `frontend/src/shared/components/Pagination/Pagination.tsx`, `frontend/src/app/layouts/PaginatedPageLayout/` |
| Nuevos layouts y componentes compartidos | 009 / 005 | 009 FR-024; 005 FR-021 | US4 | SC-009 | `frontend/src/app/layouts/` (`AppLayout`, `PageLayout`, `PaginatedPageLayout`, `AuthLayout`, `AuthGuard`), `frontend/src/shared/components/index.ts` |
| Arquitectura de estilos y tokens | 009 | FR-024 | US4 | SC-008 | `frontend/src/styles/index.scss`, `styles/base/_reset.scss`, `styles/abstracts/` |
| Escudos de equipos | 005 | FR-022 | US1 | SC-007 | `frontend/src/features/players/utils/getTeamLogo.ts`, `features/players/assets/teams/`, `frontend/scripts/import-team-logos.mjs` |
| Banderas de nacionalidad | 005 / 009 | 005 FR-022; 009 FR-023 (**habilitación, no display**) | 005 US1 | SC-007 | `frontend/src/features/players/utils/getNationalityFlag.ts`. **Nota de Code Review 2026-10-13**: el recurso se representa en la tarjeta del catálogo; ninguna US de SPEC-009 lo muestra en la ficha principal |
| Estadísticas deterministas por jugador | 009 / 005 | 009 FR-009–FR-012; 005 FR-005 | US2; US1 | SC-004, SC-005; SC-010 | `frontend/src/features/players/demo/playerSampleSeason.ts`, `utils/demoHash.ts`, `constants/playerSampleStatistics.ts`, `hooks/queries/usePlayerSampleSeasonQuery.ts` |
| Diseño del detalle individual | 009 | FR-013–FR-020 | US3, US4 | SC-005–SC-009 | `frontend/src/features/players/pages/PlayerDetailPage/`, `components/Player{Overview,SportsSummary,QuotePanel,QuoteEvolutionChart,Breadcrumb,Portrait}/`, `demo/tokenQuoteSeries.ts`, `hooks/queries/usePlayerTokenQuoteQuery.ts` |

### Requisitos pendientes de verificación funcional o visual

| Requisito | Qué falta | Quién lo verifica |
| --- | --- | --- |
| 005 FR-009 (etiqueta textual de posición) | Legibilidad del distintivo sobre los cinco fondos de liga | Usuario (navegador) |
| 005 FR-010 (elevación y refuerzo de brillo) | Ausencia de desplazamiento de la cuadrícula y de pérdida de foco con teclado | Usuario (navegador) |
| 005 SC-011 (sin dependencia del hover) | Recorrido completo por teclado y en pantalla táctil | Usuario (navegador) |
| 005 FR-006 / SC-003 | Comportamiento responsive a 768 y 360 px | Usuario (navegador) |
| 005 FR-005 / SC-010 | Valores distintos por jugador y etiqueta de demostración | Usuario (navegador) |
| 005 FR-008 / FR-022 | Recurso de liga, escudo y degradación de la bandera | Usuario (navegador) |
| 009 FR-020 / SC-009 | Foco visible y teclado en todos los controles tras el rediseño | Usuario (navegador) |
| 009 FR-014 / SC-006 | Serie de treinta cotizaciones y coherencia del último punto | Usuario (navegador) |
| 009 T036 | Salida del build sin tests | Usuario o agente con autorización de código |
| 009 T037 | Suite backend y escenarios manuales completos | Usuario |

### Cierre de sincronización documental de SPEC-005 (2026-10-13)

- [X] T043 Registrar la decisión humana que sustituye SPEC-005 FR-009 (recurso gráfico de posición → etiqueta textual) y FR-010 (indicador lateral → elevación y refuerzo de brillo), sin reintroducir los elementos visuales anteriores
- [X] T044 Actualizar en SPEC-005 los escenarios de aceptación de US3 y los criterios SC-007 y SC-011, y añadir SC-011 y el caso límite de dispositivo sin hover
- [X] T045 Conservar en SPEC-005 la redacción original de ambos requisitos derogados, con su sustituto y su justificación, en la sección de historial de sustitución de requisitos
- [X] T046 Alinear `plan.md`, `tasks.md`, `data-model.md`, `research.md` y `quickstart.md` de SPEC-005 con las decisiones aprobadas, y actualizar aquí la matriz de trazabilidad y la tabla de requisitos pendientes
- [ ] T047 Verificar en el navegador la legibilidad de la etiqueta de posición, el efecto de elevación y la ausencia de dependencia del hover. **Pendiente**: requiere ejecución manual del usuario

**Trazabilidad SPEC-005 ↔ SPEC-009 tras el cierre**: FR-011 de SPEC-009 exige que las estadísticas
compartidas entre catálogo y detalle coincidan y se identifiquen como demostración en ambas
pantallas; FR-005 de SPEC-005 cumple esa condición mediante el mismo generador determinista. La
sustitución de FR-009 y FR-010 de SPEC-005 **no afecta a ningún requisito de SPEC-009**: el detalle
no muestra el recurso de posición ni el indicador lateral de tarjeta. SPEC-009 FR-005 y FR-006
mantienen intacta la exigencia de nombre, equipo, liga, posición, nacionalidad y fecha de nacimiento,
que el detalle sigue presentando como dato textual. No se detectan contradicciones entre ambas
especificaciones.

---

## Retiro de herramientas de documentación (2026-10-14)

**Decisión del proyecto.** Se retiraron del repositorio la colección Postman y las integraciones de
Spring REST Docs y Asciidoctor.

**Efecto sobre este archivo.** Ninguna tarea cambia de estado ni de texto. `T007` y `T008` permanecen
`[x]` con su redacción original, como registro histórico de lo ejecutado en su momento: `T008` generó
snippets de Spring REST Docs sobre los casos ya afirmados por `T007`, y ambos siguen siendo la
cobertura funcional de la respuesta HTTP. La cobertura de aserciones no se vio afectada por el retiro;
solo se retiraron las integraciones que generaban la documentación derivada de esas mismas aserciones.

**Documentos actualizados.** Solo este registro. **Documentos conservados sin reescribir por contener
decisiones o estados históricos:** `plan.md`, `research.md`, `spec.md`, `data-model.md` y
`contracts/player-detail.md`. El contrato funcional del endpoint no se modificó.

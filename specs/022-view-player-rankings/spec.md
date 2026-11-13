# Feature Specification: Visualizar ranking de jugadores (REQ-S2-13)

## Revisión vigente — 2026-11-12 (aclaraciones incorporadas)

**Status vigente del cambio**: Draft. Identidad SPEC-022 preservada. IMPLEMENT AUTHORIZATION registrada en [`sdd-gates.md`](sdd-gates.md) el 2026-11-12 para las tareas T035–T050; su alcance no incluye CODE REVIEW, TESTING, CONVERGE ni cierre de la Feature.

**Alcance visual del delta**: FR-010 y SC-004 continúan describiendo el requisito de accesibilidad y adaptación responsive de la Feature. Por decisión del revisor registrada en [`sdd-gates.md`](sdd-gates.md), este delta no incluye cambios de maquetado fino, SCSS ni aceptación de fidelidad visual; la inspección visual en navegador queda a cargo del usuario. Esta exclusión no elimina el requisito general ni autoriza a considerar verificada su aceptación.

- **FR-002 / FR-011**: Consumir `GET /api/rankings/score` y `GET /api/rankings/quote` para rendimiento y cotización, respectivamente. No cambiar la ruta de navegación web `/players/ranking`.
- **FR-005 / FR-007**: Búsqueda por nombre, liga, posición y paginación se solicitan al backend para ambos modos. No descargar el ranking completo para filtrar o paginar en cliente. Un cambio de filtro, tamaño o modo reinicia a página cero; navegar conserva los criterios efectivos y descarta respuestas tardías de una selección anterior. Para las filas se usan los datos `player` incluidos en cada respuesta, sin consultar el catálogo para enriquecerlas.
- **FR-012**: Consultar el Top 3 global del modo activo mediante una solicitud sin filtros con `page=0&size=3`, independiente de la solicitud paginada de la tabla. El podio muestra, en el orden recibido, las entradas que devuelva esa consulta; si devuelve menos de tres, muestra las disponibles, sin completar el podio ni derivar entradas de la página filtrada. Las dos respuestas no garantizan una instantánea común; bajo datos estables coinciden en las posiciones globales. Cada respuesta conserva el orden y las posiciones globales asignadas por el backend.
- La tabla respeta las posiciones oficiales globales recibidas sin renumerarlas. El total de tabla corresponde al conjunto filtrado; no se confunde con el total global de clasificados.

**Aceptación US1–US3**: un jugador de una página posterior se encuentra con filtros enviados al backend; filtros y paginación no cambian los líderes solicitados por separado para el podio; cambiar de modo actualiza endpoint, metadatos, podio y tabla; carga, vacío y error siguen distinguiéndose y se preservan navegación al detalle, accesibilidad y JWT. Sin ordenamiento cliente que altere posiciones.

Este bloque sustituye el filtrado cliente de las aclaraciones y FR anteriores. No autoriza nuevas métricas ni filtros estadísticos. SPEC-018 y SPEC-016 son responsables de los contratos backend; SPEC-022 consume sus páginas, consulta el Top 3 por separado y presenta los datos identificatorios incluidos en cada respuesta.

**Impacto SDD**: plan, research, modelo, contrato de consumo, quickstart, tasks y checklists revisados para el delta. El ANALYZE anterior a las aclaraciones documentales de FR-004 y SC-001 se conserva como evidencia histórica; el análisis más reciente encontró discrepancias documentales de estado y trazabilidad, que se registran y corrigen fuera de ANALYZE. Debe repetirse ANALYZE después de estas correcciones para confirmar el estado vigente. La autorización registrada en `sdd-gates.md` se limita a T035–T050; CODE REVIEW, TESTING y cierre siguen pendientes, y se conserva la exención de tests frontend sin aprobar N/A en nombre del usuario.

**Registro histórico de SPECIFY**: el consumo backend, la invariancia del podio y la aceptación estaban definidos; el contrato de página/Top 3/enriquecimiento quedaba pendiente de CLARIFY/PLAN en ese punto del proceso. No se marcaron checklists del revisor. Issue #25 CLOSED e item Done fueron observados para el alcance histórico, no para este delta. Seguimiento del cambio PENDING, sin escritura remota.

**Evidencia histórica de materialización**: script real `--reuse`, `FEATURE_NUM=022`, `SPEC_ACTION=reused`, `BRANCH_STATUS=skipped-dirty-tree`; sin rama ni spec nueva. El estado «CLARIFY pendiente; preparación global incompleta» corresponde a esa instantánea anterior y no describe por sí solo el estado vigente.

---

## Base anterior — conservada para trazabilidad

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/022-view-player-rankings` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `022-view-player-rankings`, sin el prefijo de tipo. La convención de ramas es normativa; ver la constitución §7. El `<scope>` va en inglés, con independencia del idioma de la descripción de origen.

**Created**: 2026-10-27

**Status**: Ready for planning

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Visualizar ranking de jugadores desde la interfaz web (pestaña Ranking en la sección de Jugadores) consumiendo GET /api/players/ranking/score, comunicando la posición relativa según la estrategia activa de valuación, identificación del jugador y gestionando estados de carga, vacío y error. Issue #25 (REQ-S2-13)."

## Trazabilidad y fuentes de verdad

- **Issue de origen**: [#25 — REQ-S2-13 \| Visualizar ranking de jugadores](https://github.com/YoelSLA/football-market/issues/25).
- **Dependencia previa**: [SPEC-018 — Consultar rankings de jugadores por precio y por score (#19)](https://github.com/YoelSLA/football-market/issues/19) (`CLOSED`), contrato [`specs/018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md`](../018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md).
- **Asignación explícita**: Registrada en [`specs/assignments.md`](../assignments.md) como `SPEC-022`.
- **Referencias visuales / Diseños normativos**:
  - [`docs/design/mockups/players/raking/perfomance-raking.png`](../../docs/design/mockups/players/raking/perfomance-raking.png) — Vista oficial: Ranking por rendimiento (`GET /api/players/ranking/score`).
  - [`docs/design/mockups/players/raking/quote-raking.png`](../../docs/design/mockups/players/raking/quote-raking.png) — Vista complementaria: Ranking por cotización de mercado (`GET /api/players/ranking`).
- **Área afectada**: Frontend (`frontend/src/features/players/` y `frontend/src/app/navigation/`).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar la tabla de posiciones del ranking activo (Priority: P1)

Como usuario de Football Market, quiero acceder a la pestaña de Ranking en la sección de Jugadores para ver la clasificación ordenada de los jugadores según la estrategia de valuación activa del sistema, conociendo su posición relativa, su puntaje y los datos de referencia del recálculo.

**Why this priority**: Es el valor nuclear del requerimiento. Permite a los usuarios conocer qué jugadores lideran la clasificación según la estrategia vigente sin tener que inspeccionar jugador por jugador.

**Independent Test**: Con un usuario autenticado y datos de ranking disponibles, navegar a `/players/ranking`, alternar ambos modos y verificar que la tabla muestra la página y los filtros solicitados al backend, preserva las posiciones globales, y presenta los datos `player` incluidos en la respuesta.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado en la plataforma, **When** navega a la sección de Jugadores y hace clic en la pestaña *Ranking* (o accede directamente a `/players/ranking`), **Then** la vista muestra el listado de jugadores clasificados respetando exactamente el orden y las posiciones provistas por el backend.
2. **Given** una respuesta paginada con jugadores clasificados, **When** se visualiza la tabla o el podio, **Then** cada entrada conserva la posición global del backend y muestra el nombre, los datos visuales disponibles del jugador, el valor del modo activo y la fecha correspondiente, sin enriquecerla desde el catálogo.
3. **Given** una respuesta exitosa del backend con metadatos de recálculo, **When** carga la vista, **Then** se exhibe un encabezado informativo con el nombre legible de la estrategia activa (ej. *Desempeño por posición* para `POSITIONAL_PERFORMANCE` o *Atributos generales* para `GENERAL_ATTRIBUTES`) y la fecha de cotización de referencia (`quoteDate`).

---

### User Story 2 - Comprender los estados de carga, vacío y fallos de consulta (Priority: P2)

Como usuario de la aplicación, quiero que la interfaz me informe claramente si los datos del ranking se están cargando, si todavía no existen clasificaciones calculadas o si ocurrió un problema de red o de servidor, para evitar confusiones y poder reintentar la operación.

**Why this priority**: Asegura la robustez operativa y la usabilidad de la pantalla frente a condiciones asíncronas o indisponibilidad temporal del backend.

**Independent Test**: Se simulan solicitudes pendientes, páginas vacías (`content: []`) y respuesta HTTP 500; verificar respectivamente el indicador de progreso, el estado vacío correspondiente y la alerta de error con botón de reintento.

**Acceptance Scenarios**:

1. **Given** que la petición de consulta del ranking está en curso, **When** el usuario visualiza la vista, **Then** se muestra un indicador visual accesible de carga (`aria-busy="true"`).
2. **Given** que el backend retorna una página vacía por ausencia de datos o coincidencias, **When** finaliza la carga, **Then** se presenta el estado vacío adecuado, distinguiendo ausencia global de datos de ausencia de coincidencias en la tabla.
3. **Given** que la consulta al backend falla por desconexión o error interno (500), **When** se detecta el error, **Then** se muestra un mensaje de advertencia claro informando el problema y ofreciendo una acción explícita para reintentar la consulta.

---

### User Story 3 - Navegar al detalle individual desde la clasificación (Priority: P3)

Como usuario que analiza las posiciones del ranking, quiero poder hacer clic en cualquier jugador clasificado para acceder a su ficha detallada e investigar sus estadísticas completas.

**Why this priority**: Conecta el ranking con el ecosistema de la aplicación, facilitando la toma de decisiones informadas para el mercado de tokens.

**Independent Test**: Al hacer clic en un jugador dentro de la vista de ranking, la aplicación navega a `/players/:id` cargando correctamente la pantalla de detalle del jugador seleccionado.

**Acceptance Scenarios**:

1. **Given** un jugador visible en el ranking, **When** el usuario hace clic sobre su fila, nombre o ficha, **Then** el sistema navega a la ruta de detalle individual `/players/:id`.
2. **Given** un dispositivo con pantalla estrecha o móvil, **When** se accede al ranking, **Then** los elementos de la clasificación adaptan su diseño de manera legible y accesible sin rotura visual ni desbordamientos horizontales.

---

## Edge Cases

- **Ausencia de ejecuciones previas de cotización**: Si no hay ejecuciones, el backend devuelve encabezado nulo, contenido vacío y totales cero; la interfaz muestra el estado vacío indicando que todavía no hay una clasificación disponible.
- **Empates en puntuación**: Si dos jugadores comparten el mismo score, el backend ya resolvió el desempate por identificador ascendente asignando posiciones ordinales consecutivas distintas. El frontend MUST respetar esas posiciones y no agruparlas ni reasignarlas.
- **Datos identificatorios incompletos**: Los contratos requieren el nombre del jugador y permiten que otros datos de presentación sean nulos cuando no existan; la interfaz presenta los campos disponibles y sus fallbacks visuales, sin consultar el catálogo para completarlos.
- **Tokens o cotizaciones faltantes**: El contrato backend garantiza que solo ingresan jugadores con cotización y valuación vigentes; el frontend no debe intentar filtrar ni descartar elementos recibidos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La interfaz MUST habilitar la ruta `/players/ranking` y activar la pestaña *Ranking* en la barra de navegación interna de la sección Jugadores (`players-ranking`).
- **FR-002**: La vista MUST consultar `GET /api/rankings/score` para el modo por rendimiento y `GET /api/rankings/quote` para el modo por cotización, con JWT válido y conforme a los contratos backend vigentes. La ruta web `/players/ranking` no cambia.
- **FR-003**: La cabecera de la página MUST incluir el título "Ranking de jugadores", el selector de modo (*Por rendimiento* / *Por cotización*) y las tarjetas de metadatos correspondientes al modo activo (estrategia activa, fecha de referencia y total de jugadores clasificados).
- **FR-004**: La interfaz MUST renderizar una sección destacada de Podio (Top 3) con las entradas disponibles de la consulta global independiente, hasta un máximo de tres jugadores. Si la consulta devuelve menos de tres entradas, MUST mostrar solo las recibidas y no crear puestos vacíos ni completar el podio desde la tabla:
  - Puesto #1 en posición central, de mayor escala visual, con laureles dorados.
  - Puesto #2 en posición izquierda con laureles plateados.
  - Puesto #3 en posición derecha con laureles bronceados.
  - Cada tarjeta del podio incluye foto grande del jugador, escudo del club de fondo, nombre, badge de posición deportiva, club, liga y el valor correspondiente (score o cotización).
- **FR-005**: La interfaz MUST incluir una barra de herramientas cuyos filtros se envían al backend para explorar el ranking sin alterar la numeración de las posiciones oficiales:
  - Campo de búsqueda por texto para filtrar por nombre de jugador ("Buscar jugador...").
  - Selector desplegable de Liga ("Todas las ligas").
  - Selector desplegable de Posición ("Todas las posiciones").
  - Botón de reinicio ("Limpiar filtros").
- **FR-006**: La interfaz MUST presentar la tabla detallada de posiciones con las columnas:
  - `#`: Posición numérica (con distintivos gráficos para las posiciones 1, 2 y 3).
  - `Jugador`: Avatar/fotografía del jugador y nombre completo (enlazable a `/players/:id`).
  - `Equipo`: Escudo del club y nombre del equipo.
  - `Liga`: Bandera del país y nombre de la liga.
  - `Posición`: Badge tipo pill coloreado según la demarcación táctica (`DEL`, `MED`, `DEF`, `POR`).
  - `Score` / `Cotización`: Valor numérico con formato correspondiente (score a 6 decimales con coma decimal; o cotización en créditos).
  - `Fecha de valuación` / `Vigencia`: Fecha formateada (`dd MMM. yyyy`). En modo cotización incluye badge de origen (`RECOMPUTED` / `CARRIED_FORWARD`).
- **FR-007**: La tabla MUST incluir controles de paginación en el pie: indicador de elementos visibles respecto al total filtrado ("Mostrando X de Y jugadores"), selector de páginas y selector de filas por página (10 por defecto). La paginación se solicita al backend; la respuesta contiene solo la página pedida y los metadatos de total correspondientes.
- **FR-008**: Cada fila y tarjeta del ranking MUST permitir la navegación a la vista de detalle individual del jugador (`/players/:id`).
- **FR-009**: El sistema MUST manejar de forma explícita los tres estados de consulta: carga (`loading` con estado `aria-busy`), ausencia de clasificaciones (`empty`) y error de comunicación (`error`), este último con opción de reintento.
- **FR-010**: La vista MUST ser completamente accesible y responsiva, adaptándose a resoluciones desde 320px de ancho.
- **FR-011**: La interfaz MUST implementar ambos modos de visualización (*Por rendimiento* y *Por cotización*), permitiendo alternar entre ellos mediante el selector de modo de la cabecera, actualizando reactivamente los datos, metadatos y columnas de la tabla acordes a cada endpoint (`GET /api/rankings/score` y `GET /api/rankings/quote`).
- **FR-012**: El podio Top 3 de la cabecera MUST mostrar las entradas globales devueltas por una solicitud independiente sin filtros con `page=0&size=3`, respetando el orden y las posiciones globales asignadas por el backend. La tabla se consulta por separado con sus filtros y página; los filtros no alteran la solicitud del podio. No se garantiza que ambas respuestas compartan una misma instantánea de datos. Si la consulta del podio devuelve menos de tres entradas, se muestran las disponibles sin completarlas ni derivarlas de la tabla.

---

## Clarifications

### Session 2026-11-12

- Q: ¿Cómo debe obtenerse el podio respecto de la tabla paginada si las solicitudes no comparten una instantánea de datos? → A: Consultar el Top 3 global por separado, sin filtros y con `page=0&size=3`; mantener una solicitud independiente para la tabla. No se garantiza una instantánea común y, bajo datos estables, ambas respuestas coinciden en las posiciones globales.
- Q: ¿De qué fuente debe tomar la interfaz el nombre, la posición, el equipo y la liga de cada jugador del ranking? → A: De los datos `player` incluidos en la respuesta del ranking; no consultar el catálogo para enriquecer esas filas.

### Session 2026-10-27

- **Q1 (Alcance de modos en el ranking)**: Se acordó implementar ambos modos contemplados en los mockups: *Por rendimiento* (consumiendo `GET /api/players/ranking/score`, con estrategia activa y fecha de valuación) y *Por cotización* (consumiendo `GET /api/players/ranking`, con precio de cotización, vigencia y badge de origen). El usuario puede alternar fluidamente entre ambas vistas mediante el selector de la cabecera.
- **Q2 (Comportamiento del Podio Top 3 frente a filtros)**: El podio superior de los 3 mejores jugadores se mantiene fijo mostrando siempre los 3 líderes absolutos del ranking correspondiente (rendimiento o cotización). Los filtros cliente (búsqueda por texto, liga y posición) operan exclusivamente sobre la tabla detallada inferior, sin recalcular ni alterar los integrantes del podio.
- **Q3 (Menú de acciones de la tabla)**: La columna de menú contextual con botón de tres puntos ('...') visible en uno de los mockups queda expresamente descartada y fuera de alcance. La exploración del jugador se realiza mediante el clic directo sobre la fila, el nombre o la tarjeta hacia `/players/:id`.

---

### Key Entities

- **PlayerRankingEntry**: Representa una entrada de clasificación recibida del backend, con posición global, identificador del jugador, valor y fecha del modo activo, y los datos `player` incluidos en la respuesta.
- **PlayerRankingResponse**: Respuesta paginada del modo activo, con contenido, metadatos de página y totales filtrados; en el modo de rendimiento incluye además estrategia y fecha de cotización, y ambos modos incluyen las opciones de filtros acordadas en sus contratos.
- **Datos `player` del ranking**: Datos identificatorios y visuales que acompañan cada entrada de la respuesta del backend (nombre, posición deportiva, equipo y liga); los campos permitidos como nulos se presentan con fallbacks visuales.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las entradas recibidas en cada respuesta paginada de tabla aparecen en la interfaz en el mismo orden y con las posiciones globales recibidas, sin exigir cargar otras páginas. El podio presenta, en el orden visual correspondiente, las entradas disponibles de su consulta independiente.
- **SC-002**: El acceso a la vista de ranking desde la sección Jugadores se realiza con 1 solo clic sobre la pestaña correspondiente.
- **SC-004**: La interfaz es completamente legible y operable sin barras de desplazamiento horizontal anómalas en dispositivos con anchos de pantalla entre 320px y 1920px.
- **SC-005**: El clic en cualquier jugador del ranking redirige a `/players/:id` en el 100% de los casos verificados.

---

## Assumptions

- El usuario que accede a la vista cuenta con una sesión válida (JWT) requerida por ambos endpoints de ranking.
- Los datos `player` de cada respuesta de ranking son la fuente para presentar nombre, posición, equipo y liga; no se consulta el catálogo ni se enriquecen las filas mediante `playerId`.
- La fecha de cotización y de valuación se formatea según las convenciones vigentes de la interfaz web (`dd/MM/yyyy`).
- La estrategia recibida (`POSITIONAL_PERFORMANCE` o `GENERAL_ATTRIBUTES`) se mapea a un texto legible en español para la persona usuaria.

---

## Out of Scope

- Implementar lógica de cálculo o recálculo de puntuaciones o precios en el frontend.
- Modificar o seleccionar estrategias de valuación alternativas (la vista refleja estrictamente la estrategia activa en la última corrida).
- Permitir ordenamiento por columnas (por ejemplo, ordenar por nombre o por fecha), ya que alteraría el ranking oficial provisto por el backend.
- Incorporar filtros de búsqueda deportiva dentro del ranking que modifiquen la asignación de posiciones.
- Implementar menú contextual de tres puntos ('...') con acciones adicionales en las filas de la tabla.
- Modificar contratos o implementar endpoints en el backend (ya provistos por SPEC-018).
- Implementar tests automatizados en el frontend (exención temporal según Constitución).

---

## Cobertura del requerimiento de origen (Issue #25)

| Requisito / Criterio de Issue #25 | Cobertura en esta especificación | Estado |
| --- | --- | --- |
| **RF-01 — Acceso al ranking** | FR-001, US1 (Escenario 1) | Cubierto |
| **RF-02 — Orden vigente** | FR-003, US1 (Escenario 1) | Cubierto |
| **RF-03 — Identificación** | FR-004, US1 (Escenario 2), Key Entities | Cubierto |
| **RF-04 — Posición en el ranking** | FR-004, US1 (Escenario 2) | Cubierto |
| **RF-05 — Información de referencia** | FR-005, US1 (Escenario 3) | Cubierto |
| **RF-06 — Consistencia** | FR-003, FR-009, Out of Scope | Cubierto |
| **RF-07 — Estados de consulta** | FR-007, US2 (Escenarios 1, 2 y 3) | Cubierto |
| **RF-08 — Adaptabilidad** | FR-008, US3 (Escenario 2), SC-004 | Cubierto |
| **CA-01 a CA-08** | Mapeados 1:1 en las User Stories y Functional Requirements | Cubierto |

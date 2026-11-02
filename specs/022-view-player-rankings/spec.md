# Feature Specification: Visualizar ranking de jugadores (REQ-S2-13)

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

**Independent Test**: Con un usuario autenticado y datos de ranking disponibles en el backend, se navega a `/players/ranking` y se verifica que la pantalla presenta la lista ordenada de jugadores de menor a mayor posición numérica (1, 2, 3...), con su score original y la fecha de valuación correspondiente.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado en la plataforma, **When** navega a la sección de Jugadores y hace clic en la pestaña *Ranking* (o accede directamente a `/players/ranking`), **Then** la vista muestra el listado de jugadores clasificados respetando exactamente el orden y las posiciones provistas por el backend.
2. **Given** un ranking con jugadores clasificados, **When** se visualiza la tabla o lista, **Then** cada posición muestra claramente el número ordinal (ej. #1, #2), el nombre del jugador, su imagen o avatar de fallback, su club o posición deportiva, el puntaje (`score`) y la fecha de valuación (`valuationDate`).
3. **Given** una respuesta exitosa del backend con metadatos de recálculo, **When** carga la vista, **Then** se exhibe un encabezado informativo con el nombre legible de la estrategia activa (ej. *Desempeño por posición* para `POSITIONAL_PERFORMANCE` o *Atributos generales* para `GENERAL_ATTRIBUTES`) y la fecha de cotización de referencia (`quoteDate`).

---

### User Story 2 - Comprender los estados de carga, vacío y fallos de consulta (Priority: P2)

Como usuario de la aplicación, quiero que la interfaz me informe claramente si los datos del ranking se están cargando, si todavía no existen clasificaciones calculadas o si ocurrió un problema de red o de servidor, para evitar confusiones y poder reintentar la operación.

**Why this priority**: Asegura la robustez operativa y la usabilidad de la pantalla frente a condiciones asíncronas, catálogos iniciales o indisponibilidad temporal del backend.

**Independent Test**: Se simulan las tres respuestas posibles del backend (estado pendiente, arreglo vacío `ranking: []`, y respuesta HTTP 500) y se verifica que la interfaz renderiza respectivamente el indicador de progreso, el mensaje de estado vacío y la alerta de error con botón de reintento.

**Acceptance Scenarios**:

1. **Given** que la petición de consulta del ranking está en curso, **When** el usuario visualiza la vista, **Then** se muestra un indicador visual accesible de carga (`aria-busy="true"`).
2. **Given** que el backend retorna un conjunto sin jugadores (`ranking: []` por ausencia de cotizaciones o valuaciones), **When** finaliza la carga, **Then** se presenta un mensaje comprensible indicando que no hay clasificaciones disponibles en este momento, sin tablas vacías ni errores no controlados.
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

- **Ausencia de ejecuciones previas de cotización**: Si el backend responde `200` con `strategy: null`, `quoteDate: null` y `ranking: []`, la interfaz debe mostrar el estado vacío indicando que aún no se ha ejecutado una cotización en el mercado.
- **Empates en puntuación**: Si dos jugadores comparten el mismo score, el backend ya resolvió el desempate por identificador ascendente asignando posiciones ordinales consecutivas distintas. El frontend MUST respetar esas posiciones y no agruparlas ni reasignarlas.
- **Latencia o falla en datos del catálogo**: Si la consulta del catálogo de jugadores tarda más que el ranking o experimenta demoras, la vista debe mostrar el identificador o esqueleto de carga para los datos visuales complementarios sin bloquear la visualización de la posición y el score.
- **Tokens o cotizaciones faltantes**: El contrato backend garantiza que solo ingresan jugadores con cotización y valuación vigentes; el frontend no debe intentar filtrar ni descartar elementos recibidos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La interfaz MUST habilitar la ruta `/players/ranking` y activar la pestaña *Ranking* en la barra de navegación interna de la sección Jugadores (`players-ranking`).
- **FR-002**: La vista MUST consultar el ranking de jugadores consumiendo los endpoints de ranking del backend: `GET /api/players/ranking/score` para el modo por rendimiento y `GET /api/players/ranking` para el modo por cotización.
- **FR-003**: La cabecera de la página MUST incluir el título "Ranking de jugadores", el selector de modo (*Por rendimiento* / *Por cotización*) y las tarjetas de metadatos correspondientes al modo activo (estrategia activa, fecha de referencia y total de jugadores clasificados).
- **FR-004**: La interfaz MUST renderizar una sección destacada de Podio (Top 3) con los tres mejores jugadores clasificados:
  - Puesto #1 en posición central, de mayor escala visual, con laureles dorados.
  - Puesto #2 en posición izquierda con laureles plateados.
  - Puesto #3 en posición derecha con laureles bronceados.
  - Cada tarjeta del podio incluye foto grande del jugador, escudo del club de fondo, nombre, badge de posición deportiva, club, liga y el valor correspondiente (score o cotización).
- **FR-005**: La interfaz MUST incluir una barra de herramientas de filtrado cliente para explorar el ranking sin alterar la numeración de las posiciones oficiales:
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
- **FR-007**: La tabla MUST incluir controles de paginación en el pie: indicador de elementos visibles respecto al total ("Mostrando X de Y jugadores"), selector de páginas y selector de filas por página (10 por defecto).
- **FR-008**: Cada fila y tarjeta del ranking MUST permitir la navegación a la vista de detalle individual del jugador (`/players/:id`).
- **FR-009**: El sistema MUST manejar de forma explícita los tres estados de consulta: carga (`loading` con estado `aria-busy`), ausencia de clasificaciones (`empty`) y error de comunicación (`error`), este último con opción de reintento.
- **FR-010**: La vista MUST ser completamente accesible y responsiva, adaptándose a resoluciones desde 320px de ancho.
- **FR-011**: La interfaz MUST implementar ambos modos de visualización (*Por rendimiento* y *Por cotización*), permitiendo alternar entre ellos mediante el selector de modo de la cabecera, actualizando reactivamente los datos, metadatos y columnas de la tabla acordes a cada endpoint (`GET /api/players/ranking/score` y `GET /api/players/ranking`).
- **FR-012**: El podio Top 3 de la cabecera MUST mostrar permanentemente a los 3 mejores jugadores absolutos del ranking del modo activo. Los filtros de la barra de herramientas (texto, liga, posición) aplican de forma exclusiva a la tabla de posiciones paginada, sin alterar los integrantes del podio.

---

## Clarifications

### Session 2026-10-27

- **Q1 (Alcance de modos en el ranking)**: Se acordó implementar ambos modos contemplados en los mockups: *Por rendimiento* (consumiendo `GET /api/players/ranking/score`, con estrategia activa y fecha de valuación) y *Por cotización* (consumiendo `GET /api/players/ranking`, con precio de cotización, vigencia y badge de origen). El usuario puede alternar fluidamente entre ambas vistas mediante el selector de la cabecera.
- **Q2 (Comportamiento del Podio Top 3 frente a filtros)**: El podio superior de los 3 mejores jugadores se mantiene fijo mostrando siempre los 3 líderes absolutos del ranking correspondiente (rendimiento o cotización). Los filtros cliente (búsqueda por texto, liga y posición) operan exclusivamente sobre la tabla detallada inferior, sin recalcular ni alterar los integrantes del podio.
- **Q3 (Menú de acciones de la tabla)**: La columna de menú contextual con botón de tres puntos ('...') visible en uno de los mockups queda expresamente descartada y fuera de alcance. La exploración del jugador se realiza mediante el clic directo sobre la fila, el nombre o la tarjeta hacia `/players/:id`.

---

### Key Entities

- **PlayerRankingEntry**: Representa una fila de clasificación recibida del backend. Contiene `position` (entero positivo), `playerId` (identificador único del jugador), `score` (número decimal de valuación) y `valuationDate` (fecha ISO `yyyy-MM-dd`).
- **PlayerRankingResponse**: Contrato raíz que agrupa la estrategia activa (`strategy`), la fecha de cotización de referencia (`quoteDate`) y la lista ordenada de `ranking`.
- **PlayerInfo**: Entidad del catálogo existente utilizada para enriquecer la entrada visual del ranking (nombre completo, imagen/foto, equipo y posición táctica en el campo).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los jugadores provistos por el backend aparecen en la interfaz en la misma posición relativa y con el mismo orden recibido.
- **SC-002**: El acceso a la vista de ranking desde la sección Jugadores se realiza con 1 solo clic sobre la pestaña correspondiente.
- **SC-003**: Ante estados de respuesta vacía o error del servidor, la interfaz presenta el mensaje explicativo correspondiente en menos de 500 ms tras la resolución de la petición HTTP.
- **SC-004**: La interfaz es completamente legible y operable sin barras de desplazamiento horizontal anómalas en dispositivos con anchos de pantalla entre 320px y 1920px.
- **SC-005**: El clic en cualquier jugador del ranking redirige a `/players/:id` en el 100% de los casos verificados.

---

## Assumptions

- El usuario que accede a la vista cuenta con una sesión válida (JWT) requerida por el backend para `/api/players/ranking/score`.
- Los datos enriquecidos del jugador (nombre, fotografía, equipo) se obtienen aprovechando las consultas existentes del catálogo de jugadores (`players.service` / `useAllPlayersQuery`) asociando mediante `playerId`.
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

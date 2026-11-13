# Feature Specification: Consultar ranking de jugadores por score

## Revisión vigente — 2026-11-12 (CLARIFY)

**Status vigente del cambio**: Draft. Se reutiliza SPEC-018 y se preservan las evidencias históricas. La autorización de implementación registrada y su base se consultan en `sdd-gates.md`; su vigencia se evalúa por impacto de los cambios posteriores, no se deduce de este estado.

- **FR-001**: El ranking de score se consulta en `GET /api/rankings/score`. Su paginación y filtros se resuelven en backend; no se descarga el ranking completo para filtrar, contar, ordenar ni paginar.
- **FR-002**: La «última ejecución persistida» es la ejecución **finalizada** con mayor `startedAt` y, a igualdad, mayor `id`. Los contextos sin cierre no cuentan. Esa ejecución determina únicamente la estrategia y la fecha de cotización que encabezan la respuesta.
- **FR-003**: La población del ranking incluye exclusivamente jugadores con cotización vigente y con valuación de la estrategia seleccionada. No se añaden otros criterios poblacionales, salvo la condición de métricas definida por FR-022.
- **FR-004**: Para cada jugador elegible, usar la valuación más reciente de la estrategia seleccionada y su score final persistido (`finalScore` o `finalGrl`). No recalcular ni alterar scores, precios o valuaciones al consultar.
- **FR-005**: Ordenar globalmente por score descendente y `playerId` ascendente. Calcular las posiciones sobre el ranking global antes de aplicar filtros o paginación; no renumerar resultados filtrados.
- **FR-006**: La respuesta paginada incluye `strategy`, `quoteDate`, `content`, `page`, `size`, `totalElements`, `totalPages` y `filterOptions`, conforme a FR-019 y al contrato vigente. Las entradas incluyen los campos permitidos por FR-020.
- **FR-007**: La consulta es de solo lectura, requiere JWT válido, conserva respuestas de error seguras y no expone identificadores internos ni detalles técnicos.

- **FR-018 (contrato paginado)**: `GET /api/rankings/score` acepta `page` (base 0, por defecto 0), `size` (entre 1 y 100, por defecto 20), `name`, `leagueId` y `position`; no admite ordenamiento ni selección de estrategia o fecha. Un valor inválido de `page` o `size` responde `400 INVALID_REQUEST`; una página fuera de rango responde `200` con `content` vacío y los totales del conjunto filtrado. `name` y `position` se recortan en los extremos y se comparan sin distinguir mayúsculas; `name` coincide parcialmente y `%` y `_` son literales; `leagueId` exige igualdad y un ID positivo desconocido da cero coincidencias. Los filtros se combinan con AND.
- **FR-019 (sobre de respuesta)**: La respuesta contiene `strategy`, `quoteDate`, `content`, `page`, `size`, `totalElements`, `totalPages` y `filterOptions`. `totalElements` y `totalPages` corresponden al conjunto filtrado; `filterOptions` contiene las ligas (`id`, `name`) y las posiciones únicas del universo elegible, sin opciones sin jugadores elegibles. Sin ejecución finalizada, totales y opciones son vacíos/cero; con ejecución finalizada pero sin valuaciones aplicables, el encabezado se conserva y totales y opciones son vacíos/cero.
- **FR-020 (entrada)**: Cada entrada contiene `position`, `playerId`, `score`, `valuationDate` y `player` con `name`, `position`, `imageUrl`, `team` (`id`, `name`, `crestUrl`) y `league` (`id`, `name`). `player` y `name` son obligatorios; los demás datos son nulos cuando faltan en lo persistido y no se inventan. No se añaden identificadores de valuación, versión de estrategia ni detalles internos.
- **FR-021 (podio y total global)**: Los tres líderes globales se obtienen de la misma ruta sin filtros con `page=0&size=3`; sus posiciones 1–3 y el `totalElements` de esa solicitud constituyen el podio y el total global, independientes de la página y de los filtros de la tabla. No se añade un campo de podio ni una ruta adicional, y no se promete un snapshot compartido entre solicitudes independientes.
- **FR-022 (elegibilidad delta)**: Además de cotización vigente y valuación aplicable, MUST existir al menos una fila del jugador en `player_statistics` con `present_metrics > 0` y `extraction_date` igual a la fecha de extracción más reciente de ese jugador. Si hay varias fuentes en esa fecha, basta una fila con métricas; una fila sin métricas no excluye al jugador si otra de la misma fecha sí las tiene. Es el criterio del ranking por cotización de SPEC-016, acordado por el usuario.
- **Respuestas vacías obligatorias**: Sin ejecución finalizada, responder `200` con `strategy` y `quoteDate` nulos, `content: []`, `totalElements: 0`, `totalPages: 0` y opciones vacías; con ejecución finalizada pero sin valuaciones aplicables, mantener encabezado no nulo y responder página vacía con totales en cero y opciones vacías; sin coincidencias de filtro, responder `200` con contenido y totales filtrados en cero pero opciones globales; una página válida fuera de rango conserva los totales filtrados reales y opciones globales.
- **Otros casos obligatorios**: Ignorar ejecuciones sin cierre; `page` negativo o `size` fuera de 1–100 produce `400`; el podio sin filtros conserva las posiciones 1–3 aunque la tabla filtrada muestre saltos.

**Escenarios de aceptación vigentes US1–US3**:

1. **US1 — Página y filtros**: dado un universo elegible con más de una página, cuando se consulta una página y/o filtros válidos, entonces filtros se combinan con AND en backend, se devuelven solo las filas de esa página, los totales corresponden al conjunto filtrado y las opciones al universo elegible global (FR-001, FR-006, FR-018–FR-020; SC-011, SC-014; T033–T040, T047).
2. **US1 — Selección y presentación**: dada una ejecución finalizada y valuaciones aplicables, la respuesta expone el encabezado de esa ejecución y cada fila usa la valuación más reciente, los campos permitidos y el score final persistido (FR-002–FR-004, FR-006, FR-020, FR-022; SC-013–SC-014; T032–T039).
3. **US2 — Orden y posiciones globales**: dados scores distintos y empatados, se ordena por score descendente e ID ascendente; las posiciones se calculan globalmente antes de filtros/página y no se renumeran. Sin filtros, `page=0&size=3` devuelve los líderes disponibles y el total global (FR-005, FR-018, FR-021; SC-012, SC-016; T041–T043).
4. **US3 — Sin datos o coincidencias**: sin ejecución finalizada, con ejecución pero sin valuaciones aplicables, sin coincidencias y con página fuera de rango, se devuelve `200` con la forma de contenido, encabezado, totales y opciones fijada en FR-018–FR-019 (SC-015; T044–T046).
5. **Contrato y seguridad transversales**: parámetros de página/tamaño inválidos dan `400 INVALID_REQUEST`, JWT ausente/inválido da `401` y los fallos técnicos dan `500 INTERNAL_ERROR` sin parcial ni detalles internos; la lectura no altera datos (FR-007, FR-018; SC-017; T035, T045–T046).

Este bloque y los requisitos delta FR-018–FR-021 sustituyen, para el alcance vigente, las prohibiciones anteriores de paginación/filtros, la garantía de una ejecución por fecha, la entrada limitada a `playerId` y la exclusión de filtros/paginación de «Fuera de alcance». La propiedad del ranking por cotización sigue en SPEC-016; SPEC-018 define score y coordina coherencia del contrato compartido con SPEC-022.

**Impacto SDD**: plan, research, modelo, contrato, quickstart, tasks y checklists del delta están materializados y se consideran en este ANALYZE. Este análisis no acredita autorización de implementación. Las autorizaciones históricas no habilitan el delta; CODE REVIEW, TESTING y cierre permanecen pendientes para el alcance nuevo.

**Validación de SPECIFY/CLARIFY**: endpoints, alcance, criterios y decisiones de paginación, filtros y selección de la última ejecución quedaron definidos el 2026-11-12. La revisión de `checklists/minimum-scope.md` está registrada en ese artefacto; no equivale a aprobación de CODE REVIEW, TESTING o cierre. Como observación histórica, la issue #19 figuraba CLOSED y el item Done; el estado posterior del seguimiento se verifica en GitHub, no se deduce de esta nota.

**Evidencia**: script real `--reuse`, `FEATURE_NUM=018`, `SPEC_ACTION=reused`, `BRANCH_STATUS=skipped-dirty-tree`. CLARIFY ejecutado el 2026-11-12 con cinco decisiones integradas; artefactos del delta revisados documentalmente. El resultado de ANALYZE se informa por separado y no acredita implementación ni autorización.

---

## Base anterior — histórica, sustituida por la revisión vigente

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/018-consultar-rankings-de-jugadores`

**Created**: 2026-10-23

**Status**: Draft

**Input**: User description: "Consultar un ranking de jugadores ordenado por el score final de la estrategia usada en la última ejecución de cotización persistida, a partir de la información ya persistida y sin recalcular precios, scores ni valuaciones."

**Nota de identidad**: SPEC-018 se materializó con asignación explícita registrada en `specs/assignments.md` (issue de origen #19, requisito `REQ-S2-08`, rama `feat/018-consultar-rankings-de-jugadores`). La carpeta, el identificador y la rama existentes se conservan.

**Trazabilidad**: cubre la parte faltante de `REQ-S2-08` (ranking de jugadores). SPEC-016 ya entregó, de forma parcial, el ranking por precio vigente; esta especificación cubre únicamente el ranking por score de la estrategia registrada en la última ejecución de cotización persistida. El ranking por precio queda fuera del alcance de esta feature.

## Clarifications

### Session 2026-11-12

- Q: Cuando existen varias ejecuciones de cotización en el mismo día, ¿cómo se determina de forma determinista cuál es la «última ejecución persistida» que fija la estrategia y la fecha del ranking? → A: La ejecución finalizada con mayor `startedAt` y, a igualdad, mayor `id`; los contextos sin cierre se ignoran.
- Q: ¿Qué forma de respuesta paginada y qué parámetros debe exponer `GET /api/rankings/score` para ser coherente con `GET /api/rankings/quote`? → A: Replicar el contrato de cotización: `content`, `page`, `size`, `totalElements`, `totalPages` y `filterOptions`; `page` base 0 con default 0, `size` entre 1 y 100 con default 20, filtros `name`, `leagueId` y `position`; `400 INVALID_REQUEST` ante valores inválidos y `200` con página vacía fuera de rango.
- Q: ¿Cada entrada del ranking por score debe incluir los datos identificatorios del jugador o conservar únicamente `playerId`? → A: Incluir `player` con nombre, posición, imagen, equipo y liga, con campos nulos cuando falten datos persistidos; se conservan `position`, `playerId`, `score` y `valuationDate`. Sustituye la respuesta previa que limitaba la entrada a `playerId`.
- Q: ¿Cómo debe obtener la interfaz el total global de jugadores clasificados, distinto del total del conjunto filtrado que muestra la tabla? → A: `totalElements` de la tabla es el total filtrado; el total global es el `totalElements` de la solicitud sin filtros `page=0&size=3` usada para el podio. No se añade otro campo.
- Q: ¿Cómo debe obtener SPEC-022 los tres líderes globales del ranking por score sin filtros, manteniéndolos independientes de la página y de los filtros de tabla? → A: De la misma ruta paginada sin filtros con `page=0&size=3`; las posiciones 1–3 son las globales y no se añade un campo de podio ni una ruta adicional.

### Session 2026-10-23

- Q: ¿Qué valor persistido de la valuación debe usar el ranking como «score» cuando la última ejecución de cotización usó la estrategia `GENERAL_ATTRIBUTES`? → A: El resultado final de cada estrategia: `finalGrl` (GRL) para `GENERAL_ATTRIBUTES` y `finalScore` para `POSITIONAL_PERFORMANCE`.
- Q: ¿Qué datos debe incluir cada entrada del ranking para identificar al jugador? → A: Solo `playerId`, igual que el ranking por precio de SPEC-016.
- Q: ¿La valuación mostrada debe ser la que utilizó la última ejecución de cotización? → A: No. La última ejecución determina únicamente la estrategia del ranking; cada jugador muestra la valuación más reciente actualmente persistida de esa estrategia, aunque sea posterior o distinta de la utilizada para calcular la cotización.
- Q: ¿Cómo se determina la valuación más reciente si el esquema admite una sola valuación por jugador, fecha, estrategia y versión? → A: Exclusivamente por la mayor fecha de valuación. Una nueva evaluación de la misma fecha reemplaza la fila anterior, por lo que no existen empates persistidos que resolver por instante de cálculo o identificador.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el ranking por score de la última ejecución (Priority: P1)

**Estado**: historia base del alcance anterior; sus principios funcionales compatibles se conservan en FR-001–FR-007 vigentes y sus tareas delta están en `tasks.md`.

Como usuario del mercado, quiero consultar un ranking de jugadores ordenado por el score más reciente de la estrategia registrada en la última ejecución de cotización persistida, para conocer qué jugadores obtuvieron los mejores resultados según la valoración vigente del mercado.

**Why this priority**: Es la funcionalidad central y la parte de `REQ-S2-08` que aún falta. Sin ella no existe una clasificación por score basada en la información ya persistida.

**Independent Test**: Puede verificarse con datos persistidos de una ejecución finalizada, jugadores elegibles y varias páginas; comprobar encabezado, filtros backend, orden y posiciones globales, datos de entrada y metadatos paginados.

**Acceptance Scenarios históricos**: el contrato detallado y vigente de la consulta paginada está definido en FR-018–FR-021 y en `contracts/score-ranking-http.md`.

1. **Given** una última ejecución de cotización persistida con estrategia y fecha de cotización, y jugadores con cotización vigente y valuaciones de esa estrategia, **When** se consulta el ranking por score, **Then** la respuesta expone la estrategia y la fecha de cotización de esa ejecución, y devuelve una entrada por jugador elegible con su posición, su score y la fecha de la valuación utilizada.
2. **Given** un conjunto de jugadores elegibles con scores distintos, **When** se consulta el ranking, **Then** las entradas se ordenan por score descendente y las posiciones van de 1 a n sin posiciones compartidas.
3. **Given** dos jugadores elegibles con el mismo score, **When** se consulta el ranking, **Then** el jugador con el identificador menor aparece primero y ambos reciben posiciones consecutivas distintas.
4. **Given** un jugador con cotización vigente y varias valuaciones de la estrategia usada en distintas fechas, **When** se consulta el ranking, **Then** se utiliza el score de su valuación más reciente y se expone la fecha de esa valuación.
5. **Given** una consulta de ranking, **When** la operación finaliza, **Then** no se escriben filas ni se alteran cotizaciones, precios o valuaciones.

---

### User Story 2 - Profundizar la resolución de empates de score (Priority: P2)

**Estado**: escenarios históricos de empate; aplican solo en cuanto sean compatibles con el orden global y la paginación de FR-005/FR-018.

Como usuario del mercado, quiero que un empate de score se resuelva de forma completa y estable aun cuando varios jugadores compartan el mismo valor, para que la clasificación no dependa del orden en que se recuperan los datos y pueda compararse entre consultas.

**Why this priority**: La US1 cubre el flujo principal y el desempate básico entre dos jugadores. Esta historia profundiza ese mismo caso para empates múltiples y consultas repetidas, de modo que el orden sea reproducible y auditable.

**Independent Test**: Se verifica con los casos de empate y posición global definidos en FR-005 y sus tareas asociadas.

**Acceptance Scenarios históricos**: los criterios aplicables al delta se conservan en FR-005 y FR-018–FR-021.

1. **Given** tres o más jugadores elegibles con el mismo score, **When** se consulta el ranking, **Then** aparecen ordenados por identificador de jugador ascendente y reciben posiciones consecutivas distintas.
2. **Given** varios grupos de score, cada uno con empates internos, **When** se consulta el ranking, **Then** los grupos se ordenan por score descendente, dentro de cada grupo desempata el identificador de jugador ascendente y las posiciones van de 1 a n sin compartirse.
3. **Given** los mismos datos consultados más de una vez, **When** se comparan las respuestas, **Then** el orden, las posiciones y la valuación elegida de cada jugador son idénticos en todas las consultas.

---

### User Story 3 - Consultar el ranking cuando no hay información disponible (Priority: P3)

**Estado**: escenarios base conservados como antecedente; las formas HTTP vigentes de respuestas vacías se definen en FR-018–FR-019.

Como usuario del mercado, quiero obtener una respuesta válida cuando todavía no existen ejecuciones de cotización o cuando la última ejecución no produjo valuaciones para su estrategia, para distinguir la ausencia de datos de un error.

**Why this priority**: La respuesta debe ser predecible y no inventar posiciones ni puntuaciones cuando no hay información que clasificar.

**Independent Test**: La forma completa de las respuestas vacías se verifica conforme a FR-018–FR-019 y T035/T044/T045.

**Acceptance Scenarios históricos**: las respuestas vacías vigentes se especifican en FR-018–FR-019.

1. **Given** un sistema sin ejecuciones de cotización persistidas, **When** se consulta el ranking por score, **Then** la respuesta devuelve estrategia y fecha nulas, junto con un arreglo de entradas vacío.
2. **Given** una ejecución de cotización persistida cuya estrategia no tiene ninguna valuación, **When** se consulta el ranking, **Then** la respuesta devuelve el encabezado con estrategia y fecha, y un arreglo de entradas vacío.
3. **Given** una ejecución en la que todos los jugadores fueron arrastrados por no tener valuación de su estrategia, **When** se consulta el ranking, **Then** el encabezado no es nulo y el arreglo de entradas está vacío.

### Edge Cases históricos

Todos los casos límite de esta sección son **obligatorios**: están exigidos por el requisito `REQ-S2-08` y por los criterios de aceptación acordados para esta feature.

- **Jugador sin cotización vigente**: no aparece en el ranking, aunque tenga valuaciones de la estrategia.
- **Jugador sin valuación de la estrategia usada en la última ejecución**: no aparece en el ranking, aunque tenga cotización vigente.
- **Jugadores con fechas de valuación distintas entre sí**: cada uno aparece con su propia fecha de valuación, sin normalizarlas a una fecha común.
- **Empate de score entre jugadores**: se resuelve por identificador de jugador ascendente y las posiciones siguen siendo consecutivas.
- **Ejecución con todos los jugadores arrastrados**: la respuesta conserva el encabezado de la ejecución y devuelve un arreglo vacío.
- **Error que impide recuperar la clasificación**: se comunica de forma adecuada, sin devolver una clasificación parcial como si fuera completa.

## Requirements *(mandatory)*

### Functional Requirements históricos — no normativos para el delta vigente

- **FR-001**: El sistema MUST permitir consultar un ranking de jugadores ordenado por el score de la estrategia registrada en la última ejecución de cotización persistida. La consulta MUST NOT declarar ni utilizar parámetros de estrategia, fecha, ordenamiento, paginación o filtros. Los query parameters no declarados se ignoran y MUST NOT alterar la estrategia, la población, el orden ni el contenido del ranking. **Histórico: sustituido por FR-018–FR-021 de la revisión vigente.**
- **FR-002**: La última ejecución de cotización persistida MUST determinar únicamente la estrategia del ranking y la fecha de cotización que encabezan la respuesta. Se garantiza una única ejecución de cotización por fecha. **Histórico: la selección vigente está definida en la revisión CLARIFY y el contrato vigente.**
- **FR-003**: La población del ranking MUST estar formada exclusivamente por los jugadores que tienen cotización vigente y al menos una valuación persistida de la estrategia de la última ejecución.
- **FR-004**: El score de cada jugador MUST ser el de su valuación más reciente actualmente persistida de esa estrategia, entendida como la de mayor fecha de valuación. El esquema admite una sola valuación por jugador, fecha, estrategia y versión; una nueva evaluación de la misma fecha reemplaza la fila anterior. La valuación elegida no tiene que haber sido utilizada por la última ejecución de cotización ni queda limitada por su fecha; por ello, su score puede ser distinto del empleado para calcular la última cotización. El score MUST corresponder al resultado final de la estrategia: `finalScore` para `POSITIONAL_PERFORMANCE` y `finalGrl` (GRL) para `GENERAL_ATTRIBUTES`. El score bruto MUST NOT utilizarse.
- **FR-006**: El orden del ranking MUST ser por score descendente. A igualdad de score, MUST desempatar el identificador de jugador ascendente.
- **FR-007**: Las posiciones MUST ir de 1 a n, en correspondencia con el orden establecido, sin posiciones compartidas ni saltos.
- **FR-008**: Cada entrada del ranking MUST identificar al jugador mediante `playerId`, e incluir su posición, su score y la fecha de la valuación utilizada. La entrada MUST NOT incluir nombre ni otros datos del jugador. **Histórico: el contenido vigente de la entrada está definido por FR-020.**
- **FR-009**: La respuesta MUST exponer la estrategia y la fecha de cotización de la última ejecución persistida. La versión de la estrategia MUST NOT formar parte de la respuesta ni utilizarse para filtrar resultados.
- **FR-010**: Si no existe ninguna ejecución de cotización persistida, la respuesta MUST devolver estrategia y fecha nulas, y un arreglo de entradas vacío.
- **FR-011**: Si existe una ejecución de cotización persistida pero no hay ninguna valuación de su estrategia, la respuesta MUST devolver el encabezado con estrategia y fecha, y un arreglo de entradas vacío.
- **FR-012**: El score MUST devolverse tal como está persistido, sin redondeo, recálculo ni transformación. La consulta MUST NOT recalcular precios, scores ni valuaciones.
- **FR-013**: Consultar el ranking MUST ser una operación de solo lectura: MUST NOT escribir filas ni alterar cotizaciones, precios o valuaciones.
- **FR-014**: Los errores que impidan recuperar la clasificación MUST comunicarse de forma adecuada, sin sustituir la clasificación por datos inventados ni presentar un resultado parcial como completo.
- **FR-016**: La consulta MUST exigir un JWT válido y MUST NOT introducir roles ni permisos nuevos: cualquier usuario autenticado obtiene el mismo ranking. Una solicitud sin JWT o con JWT inválido MUST responder `401` sin devolver datos del ranking.
- **FR-017**: Las respuestas de la consulta, incluidas las de error, MUST NOT exponer identificadores internos de valuación, la versión de la estrategia, trazas, mensajes técnicos ni otros detalles de implementación.

### Key Entities históricas *(include if feature involves data)*

- **Ejecución de cotización**: registro persistido de una corrida del mercado; aporta la estrategia registrada y la fecha de cotización que encabezan el ranking, pero no identifica la valuación mostrada. Existe como máximo una por fecha de cotización.
- **Estrategia**: criterio de valoración usado en la ejecución; identifica la estrategia que encabeza el ranking. Su versión no forma parte de la respuesta ni interviene en la selección de jugadores.
- **Cotización vigente**: última cotización persistida de un jugador; determina si el jugador participa del ranking, sin aportar su score.
- **Valuación**: resultado persistido del cálculo de una estrategia para un jugador, único por jugador, fecha, estrategia y versión, con fecha de valuación y resultado final (`finalScore` para `POSITIONAL_PERFORMANCE`, `finalGrl` para `GENERAL_ATTRIBUTES`). La de mayor fecha de la estrategia registrada aporta el score y la fecha que se muestran en la entrada, aunque no sea la utilizada para calcular la cotización.
- **Entrada del ranking**: representación de un jugador elegible dentro de la clasificación; contiene `playerId`, posición, score y fecha de la valuación utilizada.

## Success Criteria *(mandatory)*

### Measurable Outcomes vigentes

- **SC-011**: Las consultas válidas devuelven únicamente la página solicitada después de aplicar en backend todos los filtros con AND; los totales reflejan el conjunto filtrado y las opciones corresponden al universo elegible sin filtros.
- **SC-012**: En todas las páginas y filtros, cada jugador conserva su posición en el ranking global ordenado por score descendente e ID ascendente; no se renumeran los resultados filtrados.
- **SC-013**: El 100 % de las entradas corresponde a jugadores con cotización vigente, alguna estadística de la fecha de extracción más reciente con `present_metrics > 0` y valuación aplicable; cada score es el valor final persistido de la última valuación elegida.
- **SC-014**: Cada respuesta `200` contiene el sobre paginado definido en FR-019 y entradas conformes a FR-020; la ejecución finalizada más reciente determina `strategy` y `quoteDate`.
- **SC-015**: Sin ejecución o sin valuaciones aplicables, la respuesta `200` presenta encabezado según FR-019, contenido vacío, totales cero y opciones vacías; sin coincidencias presenta totales filtrados cero y opciones globales; una página fuera de rango conserva los totales filtrados reales.
- **SC-016**: El podio consultado sin filtros con `page=0&size=3` contiene las posiciones globales 1–3 disponibles y reporta el total global en `totalElements`.
- **SC-017**: Los parámetros inválidos de página/tamaño reciben `400 INVALID_REQUEST`; las solicitudes sin JWT válido reciben `401`; errores técnicos no presentan datos parciales ni detalles internos.

### Measurable Outcomes históricos — alcance anterior

- **SC-001**: El 100 % de las entradas del ranking se presenta en orden de score descendente, con desempate por identificador de jugador ascendente cuando los scores coinciden.
- **SC-002**: Las posiciones del ranking son siempre consecutivas de 1 a n, sin valores repetidos ni compartidos, para cualquier población de resultados.
- **SC-003**: El 100 % de los jugadores incluidos tiene cotización vigente y al menos una valuación de la estrategia de la última ejecución; ningún jugador que carezca de alguno de esos dos requisitos aparece en el ranking.
- **SC-004**: El encabezado de la respuesta coincide en el 100 % de los casos con la estrategia y la fecha de cotización de la última ejecución persistida.
- **SC-005**: Cada entrada muestra un score idéntico al resultado final persistido de la estrategia —`finalScore` o `finalGrl` según corresponda— y la fecha de la valuación efectivamente utilizada, sin redondeos ni recálculos observables.
- **SC-006**: Ante la ausencia de ejecuciones o de valuaciones de la estrategia, el 100 % de las consultas devuelve una respuesta válida y vacía, sin posiciones ni puntuaciones inventadas.
- **SC-007**: Consultar el ranking no produce ningún cambio observable en cotizaciones, precios ni valuaciones, verificado comparando el estado antes y después de la consulta.
- **SC-009**: El 100 % de las solicitudes sin JWT válido recibe `401` y ninguna de ellas devuelve datos del ranking.
- **SC-010**: El 100 % de las respuestas, incluidas las de error, contiene únicamente los campos declarados en el contrato y ningún identificador interno de valuación, versión de estrategia ni detalle técnico.

## Fuera de alcance histórico

- Configurar, crear o seleccionar estrategias de valuación.
- Persistir una estrategia activa independiente de la última ejecución.
- Elegir la estrategia por parámetro de consulta.
- Recalcular cotizaciones o valuaciones.
- Implementar la interfaz web de visualización.
- Agregar filtros o paginación. **Histórico: filtros y paginación forman parte del delta aprobado y definido en FR-018–FR-021.**
- Modificar el ranking por precio o su contrato.

## Assumptions históricas

- La versión de la estrategia no forma parte de la respuesta ni interviene en la clasificación.
- El score se devuelve tal como está persistido, sin redondeo ni recálculo.
- La consulta no declara ni utiliza parámetros de estrategia ni filtros adicionales; los query parameters no declarados se ignoran sin alterar el resultado.
- La autenticación es la heredada del resto del sistema (JWT válido, sin roles nuevos); esta feature no define un mecanismo propio.
- La interfaz de usuario de rankings queda fuera de alcance.
- La información de cotizaciones, estrategias y valuaciones ya está persistida por las funcionalidades existentes; esta feature solo la consulta.
- El esquema garantiza una única ejecución de cotización por fecha, por lo que la última ejecución queda determinada sin ambigüedad.
- El flujo operativo habitual obtiene métricas, calcula valuaciones y luego calcula cotizaciones una vez por semana. La consulta puede ejecutarse entre esas etapas o después de un fallo parcial, por lo que `quoteDate` no identifica la fecha del score ni garantiza que este haya sido utilizado para calcular la cotización; `valuationDate` identifica la fecha efectiva del score mostrado.

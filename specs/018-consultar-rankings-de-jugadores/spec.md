# Feature Specification: Consultar ranking de jugadores por score

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

### Session 2026-10-23

- Q: ¿Qué valor persistido de la valuación debe usar el ranking como «score» cuando la última ejecución de cotización usó la estrategia `GENERAL_ATTRIBUTES`? → A: El resultado final de cada estrategia: `finalGrl` (GRL) para `GENERAL_ATTRIBUTES` y `finalScore` para `POSITIONAL_PERFORMANCE`.
- Q: ¿Qué datos debe incluir cada entrada del ranking para identificar al jugador? → A: Solo `playerId`, igual que el ranking por precio de SPEC-016.
- Q: ¿La valuación mostrada debe ser la que utilizó la última ejecución de cotización? → A: No. La última ejecución determina únicamente la estrategia del ranking; cada jugador muestra la valuación más reciente actualmente persistida de esa estrategia, aunque sea posterior o distinta de la utilizada para calcular la cotización.
- Q: ¿Cómo se determina la valuación más reciente si el esquema admite una sola valuación por jugador, fecha, estrategia y versión? → A: Exclusivamente por la mayor fecha de valuación. Una nueva evaluación de la misma fecha reemplaza la fila anterior, por lo que no existen empates persistidos que resolver por instante de cálculo o identificador.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el ranking por score de la última ejecución (Priority: P1)

Como usuario del mercado, quiero consultar un ranking de jugadores ordenado por el score más reciente de la estrategia registrada en la última ejecución de cotización persistida, para conocer qué jugadores obtuvieron los mejores resultados según la valoración vigente del mercado.

**Why this priority**: Es la funcionalidad central y la parte de `REQ-S2-08` que aún falta. Sin ella no existe una clasificación por score basada en la información ya persistida.

**Independent Test**: Puede verificarse con datos persistidos de una última ejecución de cotización, jugadores con cotización vigente y valuaciones de la estrategia usada, comprobando el encabezado de la respuesta, la población incluida, el score de cada entrada, su fecha de valuación y el orden completo con posiciones consecutivas.

**Acceptance Scenarios**:

1. **Given** una última ejecución de cotización persistida con estrategia y fecha de cotización, y jugadores con cotización vigente y valuaciones de esa estrategia, **When** se consulta el ranking por score, **Then** la respuesta expone la estrategia y la fecha de cotización de esa ejecución, y devuelve una entrada por jugador elegible con su posición, su score y la fecha de la valuación utilizada.
2. **Given** un conjunto de jugadores elegibles con scores distintos, **When** se consulta el ranking, **Then** las entradas se ordenan por score descendente y las posiciones van de 1 a n sin posiciones compartidas.
3. **Given** dos jugadores elegibles con el mismo score, **When** se consulta el ranking, **Then** el jugador con el identificador menor aparece primero y ambos reciben posiciones consecutivas distintas.
4. **Given** un jugador con cotización vigente y varias valuaciones de la estrategia usada en distintas fechas, **When** se consulta el ranking, **Then** se utiliza el score de su valuación más reciente y se expone la fecha de esa valuación.
5. **Given** una consulta de ranking, **When** la operación finaliza, **Then** no se escriben filas ni se alteran cotizaciones, precios o valuaciones.

---

### User Story 2 - Profundizar la resolución de empates de score (Priority: P2)

Como usuario del mercado, quiero que un empate de score se resuelva de forma completa y estable aun cuando varios jugadores compartan el mismo valor, para que la clasificación no dependa del orden en que se recuperan los datos y pueda compararse entre consultas.

**Why this priority**: La US1 cubre el flujo principal y el desempate básico entre dos jugadores. Esta historia profundiza ese mismo caso para empates múltiples y consultas repetidas, de modo que el orden sea reproducible y auditable.

**Independent Test**: Puede verificarse con datos que incluyan al menos tres jugadores con el mismo score y otro grupo de empate con un score distinto, comprobando el orden dentro de cada grupo, el orden entre grupos, la continuidad de las posiciones y la estabilidad de la respuesta en consultas repetidas.

**Acceptance Scenarios**:

1. **Given** tres o más jugadores elegibles con el mismo score, **When** se consulta el ranking, **Then** aparecen ordenados por identificador de jugador ascendente y reciben posiciones consecutivas distintas.
2. **Given** varios grupos de score, cada uno con empates internos, **When** se consulta el ranking, **Then** los grupos se ordenan por score descendente, dentro de cada grupo desempata el identificador de jugador ascendente y las posiciones van de 1 a n sin compartirse.
3. **Given** los mismos datos consultados más de una vez, **When** se comparan las respuestas, **Then** el orden, las posiciones y la valuación elegida de cada jugador son idénticos en todas las consultas.

---

### User Story 3 - Consultar el ranking cuando no hay información disponible (Priority: P3)

Como usuario del mercado, quiero obtener una respuesta válida cuando todavía no existen ejecuciones de cotización o cuando la última ejecución no produjo valuaciones para su estrategia, para distinguir la ausencia de datos de un error.

**Why this priority**: La respuesta debe ser predecible y no inventar posiciones ni puntuaciones cuando no hay información que clasificar.

**Independent Test**: Puede verificarse primero sin ejecuciones de cotización y luego con una ejecución cuya estrategia no tiene valuaciones persistidas, comprobando en ambos casos que la respuesta es válida y que el arreglo de entradas está vacío.

**Acceptance Scenarios**:

1. **Given** un sistema sin ejecuciones de cotización persistidas, **When** se consulta el ranking por score, **Then** la respuesta devuelve estrategia y fecha nulas, junto con un arreglo de entradas vacío.
2. **Given** una ejecución de cotización persistida cuya estrategia no tiene ninguna valuación, **When** se consulta el ranking, **Then** la respuesta devuelve el encabezado con estrategia y fecha, y un arreglo de entradas vacío.
3. **Given** una ejecución en la que todos los jugadores fueron arrastrados por no tener valuación de su estrategia, **When** se consulta el ranking, **Then** el encabezado no es nulo y el arreglo de entradas está vacío.

### Edge Cases

Todos los casos límite de esta sección son **obligatorios**: están exigidos por el requisito `REQ-S2-08` y por los criterios de aceptación acordados para esta feature.

- **Jugador sin cotización vigente**: no aparece en el ranking, aunque tenga valuaciones de la estrategia.
- **Jugador sin valuación de la estrategia usada en la última ejecución**: no aparece en el ranking, aunque tenga cotización vigente.
- **Jugadores con fechas de valuación distintas entre sí**: cada uno aparece con su propia fecha de valuación, sin normalizarlas a una fecha común.
- **Empate de score entre jugadores**: se resuelve por identificador de jugador ascendente y las posiciones siguen siendo consecutivas.
- **Ejecución con todos los jugadores arrastrados**: la respuesta conserva el encabezado de la ejecución y devuelve un arreglo vacío.
- **Error que impide recuperar la clasificación**: se comunica de forma adecuada, sin devolver una clasificación parcial como si fuera completa.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir consultar un ranking de jugadores ordenado por el score de la estrategia registrada en la última ejecución de cotización persistida. La consulta MUST NOT declarar ni utilizar parámetros de estrategia, fecha, ordenamiento, paginación o filtros. Los query parameters no declarados se ignoran y MUST NOT alterar la estrategia, la población, el orden ni el contenido del ranking.
- **FR-002**: La última ejecución de cotización persistida MUST determinar únicamente la estrategia del ranking y la fecha de cotización que encabezan la respuesta. Se garantiza una única ejecución de cotización por fecha.
- **FR-003**: La población del ranking MUST estar formada exclusivamente por los jugadores que tienen cotización vigente y al menos una valuación persistida de la estrategia de la última ejecución.
- **FR-004**: El score de cada jugador MUST ser el de su valuación más reciente actualmente persistida de esa estrategia, entendida como la de mayor fecha de valuación. El esquema admite una sola valuación por jugador, fecha, estrategia y versión; una nueva evaluación de la misma fecha reemplaza la fila anterior. La valuación elegida no tiene que haber sido utilizada por la última ejecución de cotización ni queda limitada por su fecha; por ello, su score puede ser distinto del empleado para calcular la última cotización. El score MUST corresponder al resultado final de la estrategia: `finalScore` para `POSITIONAL_PERFORMANCE` y `finalGrl` (GRL) para `GENERAL_ATTRIBUTES`. El score bruto MUST NOT utilizarse.
- **FR-006**: El orden del ranking MUST ser por score descendente. A igualdad de score, MUST desempatar el identificador de jugador ascendente.
- **FR-007**: Las posiciones MUST ir de 1 a n, en correspondencia con el orden establecido, sin posiciones compartidas ni saltos.
- **FR-008**: Cada entrada del ranking MUST identificar al jugador mediante `playerId`, e incluir su posición, su score y la fecha de la valuación utilizada. La entrada MUST NOT incluir nombre ni otros datos del jugador.
- **FR-009**: La respuesta MUST exponer la estrategia y la fecha de cotización de la última ejecución persistida. La versión de la estrategia MUST NOT formar parte de la respuesta ni utilizarse para filtrar resultados.
- **FR-010**: Si no existe ninguna ejecución de cotización persistida, la respuesta MUST devolver estrategia y fecha nulas, y un arreglo de entradas vacío.
- **FR-011**: Si existe una ejecución de cotización persistida pero no hay ninguna valuación de su estrategia, la respuesta MUST devolver el encabezado con estrategia y fecha, y un arreglo de entradas vacío.
- **FR-012**: El score MUST devolverse tal como está persistido, sin redondeo, recálculo ni transformación. La consulta MUST NOT recalcular precios, scores ni valuaciones.
- **FR-013**: Consultar el ranking MUST ser una operación de solo lectura: MUST NOT escribir filas ni alterar cotizaciones, precios o valuaciones.
- **FR-014**: Los errores que impidan recuperar la clasificación MUST comunicarse de forma adecuada, sin sustituir la clasificación por datos inventados ni presentar un resultado parcial como completo.
- **FR-016**: La consulta MUST exigir un JWT válido y MUST NOT introducir roles ni permisos nuevos: cualquier usuario autenticado obtiene el mismo ranking. Una solicitud sin JWT o con JWT inválido MUST responder `401` sin devolver datos del ranking.
- **FR-017**: Las respuestas de la consulta, incluidas las de error, MUST NOT exponer identificadores internos de valuación, la versión de la estrategia, trazas, mensajes técnicos ni otros detalles de implementación.

### Key Entities *(include if feature involves data)*

- **Ejecución de cotización**: registro persistido de una corrida del mercado; aporta la estrategia registrada y la fecha de cotización que encabezan el ranking, pero no identifica la valuación mostrada. Existe como máximo una por fecha de cotización.
- **Estrategia**: criterio de valoración usado en la ejecución; identifica la estrategia que encabeza el ranking. Su versión no forma parte de la respuesta ni interviene en la selección de jugadores.
- **Cotización vigente**: última cotización persistida de un jugador; determina si el jugador participa del ranking, sin aportar su score.
- **Valuación**: resultado persistido del cálculo de una estrategia para un jugador, único por jugador, fecha, estrategia y versión, con fecha de valuación y resultado final (`finalScore` para `POSITIONAL_PERFORMANCE`, `finalGrl` para `GENERAL_ATTRIBUTES`). La de mayor fecha de la estrategia registrada aporta el score y la fecha que se muestran en la entrada, aunque no sea la utilizada para calcular la cotización.
- **Entrada del ranking**: representación de un jugador elegible dentro de la clasificación; contiene `playerId`, posición, score y fecha de la valuación utilizada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las entradas del ranking se presenta en orden de score descendente, con desempate por identificador de jugador ascendente cuando los scores coinciden.
- **SC-002**: Las posiciones del ranking son siempre consecutivas de 1 a n, sin valores repetidos ni compartidos, para cualquier población de resultados.
- **SC-003**: El 100 % de los jugadores incluidos tiene cotización vigente y al menos una valuación de la estrategia de la última ejecución; ningún jugador que carezca de alguno de esos dos requisitos aparece en el ranking.
- **SC-004**: El encabezado de la respuesta coincide en el 100 % de los casos con la estrategia y la fecha de cotización de la última ejecución persistida.
- **SC-005**: Cada entrada muestra un score idéntico al resultado final persistido de la estrategia —`finalScore` o `finalGrl` según corresponda— y la fecha de la valuación efectivamente utilizada, sin redondeos ni recálculos observables.
- **SC-006**: Ante la ausencia de ejecuciones o de valuaciones de la estrategia, el 100 % de las consultas devuelve una respuesta válida y vacía, sin posiciones ni puntuaciones inventadas.
- **SC-007**: Consultar el ranking no produce ningún cambio observable en cotizaciones, precios ni valuaciones, verificado comparando el estado antes y después de la consulta.
- **SC-009**: El 100 % de las solicitudes sin JWT válido recibe `401` y ninguna de ellas devuelve datos del ranking.
- **SC-010**: El 100 % de las respuestas, incluidas las de error, contiene únicamente los campos declarados en el contrato y ningún identificador interno de valuación, versión de estrategia ni detalle técnico.

## Fuera de alcance

- Configurar, crear o seleccionar estrategias de valuación.
- Persistir una estrategia activa independiente de la última ejecución.
- Elegir la estrategia por parámetro de consulta.
- Recalcular cotizaciones o valuaciones.
- Implementar la interfaz web de visualización.
- Agregar filtros, paginación, caché u optimizaciones no solicitadas.
- Modificar el ranking por precio o su contrato.

## Assumptions

- La versión de la estrategia no forma parte de la respuesta ni interviene en la clasificación.
- El score se devuelve tal como está persistido, sin redondeo ni recálculo.
- La consulta no declara ni utiliza parámetros de estrategia ni filtros adicionales; los query parameters no declarados se ignoran sin alterar el resultado.
- La autenticación es la heredada del resto del sistema (JWT válido, sin roles nuevos); esta feature no define un mecanismo propio.
- La interfaz de usuario de rankings queda fuera de alcance.
- La información de cotizaciones, estrategias y valuaciones ya está persistida por las funcionalidades existentes; esta feature solo la consulta.
- El esquema garantiza una única ejecución de cotización por fecha, por lo que la última ejecución queda determinada sin ambigüedad.
- El flujo operativo habitual obtiene métricas, calcula valuaciones y luego calcula cotizaciones una vez por semana. La consulta puede ejecutarse entre esas etapas o después de un fallo parcial, por lo que `quoteDate` no identifica la fecha del score ni garantiza que este haya sido utilizado para calcular la cotización; `valuationDate` identifica la fecha efectiva del score mostrado.

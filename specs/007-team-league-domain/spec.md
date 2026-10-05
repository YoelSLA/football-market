# Feature Specification: Dominio de equipos y ligas

**Feature Branch**: `[007-team-league-domain]`

**Created**: 2026-10-02

**Status**: Draft

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Incorporar `Team` y `League` como conceptos propios del dominio de FootballMarket, con identidad interna independiente de los proveedores. El jugador pasa a estar asociado a un único equipo interno y su liga se determina a través de ese equipo; `Player.team` y `Player.league` dejan de ser la fuente de verdad. Esta feature prepara el dominio para que otras features, en particular `006-player-images`, puedan usar las referencias externas de `Team`, sin incluir comportamiento de sincronización de imágenes.

## Clarifications

### Pendientes de clarificación (bloquean el paso a `Ready for planning`)

- **P-001 — Contrato del catálogo de jugadores**: la regla funcional 27 establece que la consulta debe exponer conceptualmente `teamId`, `teamName`, `leagueId` y `leagueName`, y la regla 18 exige que los valores textuales actuales dejen de ser una segunda fuente de verdad. No está definido funcionalmente si el contrato conserva los campos `team` y `league` actuales como valores derivados, o si se reemplazan por los nuevos. Es una decisión de contrato observable, no técnica, y debe resolverse antes de planificar porque afecta a `002-player-catalog` (RF-004) y a `006-player-images` (FR-042, FR-043), cuyos filtros `teamName` y `leagueName` hoy se comparan contra el texto de `Player.team` y `Player.league`.
- **P-002 — Criterios de resolución de la identidad TheSportsDB de un `Team`**: la regla 21 exige intentar resolver la identidad TheSportsDB de un equipo y conservarla "solo cuando la coincidencia sea confiable", sin definir funcionalmente qué hace confiable una coincidencia ni qué reglas de comparación se aplican a nombres de equipo. No se inventa aquí esa definición.
- **P-003 — Tratamiento de los jugadores migrados sin equipo inequívoco**: la regla 17 exige que esos casos no se resuelvan por aproximación y queden identificados para revisión. No está definido si su ejecución bloquea la finalización de la transición (regla 18), ni cómo se comunica esa revisión.

### Session 2026-10-02

Se registran las decisiones funcionales ya resueltas por el usuario:

- Q: ¿`Team` y `League` tienen identidad propia? → A: Sí, con identificador interno propio, independiente de cualquier proveedor.
- Q: ¿El dominio queda limitado a cinco ligas? → A: No es estructuralmente genérico; inicialmente solo se utilizan las cinco ligas actuales.
- Q: ¿`Player` mantiene una asociación directa con `League`? → A: No; su liga se determina a través de su `Team`.
- Q: ¿Cuántas ligas tiene un `Team`? → A: Actualmente una sola.
- Q: ¿Un cambio de nombre del proveedor crea una entidad nueva? → A: No; se conserva la identidad interna y se actualiza el nombre.
- Q: ¿Qué ocurre con un equipo que cambia de liga? → A: Se conserva la identidad de equipo y se actualiza su liga actual.
- Q: ¿Qué pasa si no se puede resolver el equipo de un jugador durante la sincronización? → A: El jugador no se incorpora ni se actualiza.
- Q: ¿Qué pasa si un equipo no puede asociarse a una liga? → A: El equipo no se incorpora ni se actualiza.
- Q: ¿Debe `Team` obtener referencia TheSportsDB en esta feature? → A: Sí, pero su ausencia nunca bloquea el catálogo; para `League` queda fuera de alcance.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconocer al equipo y a la liga del jugador (Priority: P1)

Como consumidor del catálogo, quiero que el equipo y la liga de cada jugador estén identificados por la identidad propia de FootballMarket y no por textos libres, de modo que el catálogo sea coherente y reutilizable.

**Why this priority**: Es el resultado central de la feature: sin identidad propia de `Team` y `League`, las stories siguientes no tienen sentido.

**Independent Test**: Consultar el catálogo de jugadores y comprobar que cada jugador expone la identidad interna de su equipo y de su liga junto con sus nombres, sin depender de Football-Data.org en tiempo de consulta.

**Acceptance Scenarios**:

1. **Given** un jugador activo asociado a un equipo, **When** se consulta el catálogo de jugadores, **Then** la respuesta expone la identidad interna de ese equipo y de la liga del equipo, junto con el nombre del equipo y el nombre de la liga.
2. **Given** un jugador activo cuyo equipo pertenece a una liga conocida, **When** se consulta el catálogo, **Then** su liga coincide con la liga de su equipo y el jugador no expone una liga independiente de ese equipo.
3. **Given** un proveedor externo no disponible, **When** se consulta el catálogo, **Then** la identidad del equipo y de la liga se obtiene del catálogo local y la consulta responde con normalidad.
4. **Given** un jugador del catálogo, **When** se consulta su información, **Then** no se exponen las referencias externas de su equipo ni de su liga.

---

### User Story 2 - Sincronizar el catálogo reconocyendo ligas y equipos (Priority: P1)

Como usuario autenticado, quiero que la sincronización del catálogo reconozca y mantenga primero las ligas y los equipos, y solo después asocie y actualice los jugadores, sin crear identidades duplicadas.

**Why this priority**: Es el mecanismo por el que las identidades internas se crean y se mantienen coherentes.

**Independent Test**: Sincronizar con Football-Data.org las ligas cubiertas y comprobar que ligas y equipos quedan reconocidos con su propia identidad, que los jugadores se asocian al equipo correspondiente y que sincronizaciones sucesivas no duplican entidades.

**Acceptance Scenarios**:

1. **Given** una sincronización completa con Football-Data.org, **When** se procesa la información de las ligas cubiertas, **Then** las ligas necesarias quedan reconocidas o incorporadas con identidad interna propia antes de asociar o actualizar jugadores.
2. **Given** una sincronización completa con Football-Data.org, **When** se procesa la información de un equipo de una liga cubierta, **Then** ese equipo queda reconocido o incorporado con identidad interna propia, asociado a su liga, antes de asociar o actualizar los jugadores de ese equipo.
3. **Given** un jugador recibido cuyo equipo no puede resolverse, **When** se procesa la sincronización, **Then** el jugador no se incorpora ni se actualiza y el caso queda registrado, sin dejar un jugador sin equipo.
4. **Given** un equipo recibido cuya liga no puede resolverse, **When** se procesa la sincronización, **Then** el equipo no se incorpora ni se actualiza y el caso queda registrado.
5. **Given** las mismas ligas y equipos informados en sincronizaciones sucesivas, **When** se procesa cada sincronización, **Then** el catálogo conserva una única identidad interna por liga y por equipo, sin duplicados.
6. **Given** un jugador ya existente, **When** se procesa la sincronización, **Then** se resuelve por su referencia externa de origen y se actualiza manteniendo su identidad interna y la de su equipo.

---

### User Story 3 - Conservar la identidad del equipo ante cambios del proveedor (Priority: P1)

Como usuario del catálogo, quiero que un cambio de nombre de equipo o un cambio de liga no genere una identidad nueva, para que la historia del jugador no se rompa.

**Why this priority**: Es la garantía de continuidad de identidad que hace útil el modelo.

**Independent Test**: Sincronizar dos veces con un nombre de equipo distinto y con el equipo en una liga distinta, y comprobar que la identidad interna del equipo no cambia.

**Acceptance Scenarios**:

1. **Given** un equipo existente, **When** el proveedor informa un nombre distinto para ese equipo, **Then** se conserva la misma identidad interna de equipo y se actualiza su nombre actual.
2. **Given** un equipo existente, **When** el proveedor informa que ese mismo equipo pertenece a otra liga, **Then** se conserva la misma identidad interna de equipo y se actualiza su liga actual, y los jugadores mantienen su asociación al mismo equipo.
3. **Given** una liga existente, **When** el proveedor informa un nombre distinto para esa liga, **Then** se conserva la misma identidad interna de liga y se actualiza su nombre actual.

---

### User Story 4 - Resolver la identidad externa del equipo sin bloquear el catálogo (Priority: P2)

Como usuario del catálogo, quiero que el equipo pueda contar con una identidad externa propia en TheSportsDB cuando la coincidencia sea confiable, y que su ausencia nunca impida operar el catálogo.

**Why this priority**: Prepara el dominio para `006-player-images`, pero no debe poner en riesgo la disponibilidad del catálogo.

**Independent Test**: Sincronizar con equipos que sí y que no permiten resolver su identidad TheSportsDB, y comprobar que en ambos casos el equipo queda válido y consultable.

**Acceptance Scenarios**:

1. **Given** un equipo cuya identidad TheSportsDB puede resolverse con información disponible del equipo, **When** se procesa la sincronización, **Then** el equipo conserva esa referencia externa y su catálogo no se ve afectado.
2. **Given** un equipo cuya identidad TheSportsDB no puede resolverse o la coincidencia no es confiable, **When** se procesa la sincronización, **Then** el equipo queda igualmente válido con su identidad interna y su referencia externa de origen, y no se le asigna una referencia incorrecta.
3. **Given** un equipo que ya tiene una identidad TheSportsDB, **When** se procesa una sincronización posterior, **Then** esa identidad se conserva y no se reasigna a otro equipo.

---

### User Story 5 - Mantener en el catálogo a los equipos y jugadores que lo abandonan (Priority: P2)

Como usuario del catálogo, quiero que la salida de un equipo de las ligas cubiertas no borre su historia, para que los datos históricos conserven sentido.

**Why this priority**: Es una consecuencia necesaria del modelo de identidad, pero no es el resultado principal.

**Independent Test**: Sincronizar un conjunto de ligas donde un equipo deja de aparecer y comprobar que el equipo y sus jugadores se conservan pero dejan de considerarse parte del catálogo vigente.

**Acceptance Scenarios**:

1. **Given** un equipo que deja de formar parte de las ligas actualmente cubiertas, **When** finaliza una sincronización completa y exitosa, **Then** el equipo conserva su identidad y sus referencias externas y deja de considerarse parte del catálogo actual.
2. **Given** un equipo que deja de formar parte del catálogo actual, **When** finaliza esa sincronización, **Then** sus jugadores se conservan y dejan de considerarse activos dentro del catálogo vigente.
3. **Given** un equipo que vuelve a aparecer en una sincronización completa y exitosa, **When** finaliza, **Then** vuelve a considerarse parte del catálogo actual y sus jugadores vuelven a estar activos.

---

### User Story 6 - Migrar el catálogo existente sin perder jugadores (Priority: P1)

Como usuario del catálogo, quiero que los jugadores existentes queden asociados a su equipo correspondiente sin que se pierdan ni se dupliquen, y que los casos dudosos queden identificados.

**Why this priority**: Sin la transición, la feature no aporta valor al catálogo existente.

**Independent Test**: Partir de un catálogo con textos de equipo y liga y comprobar que los jugadores quedan asociados a un equipo interno, que los casos inequívocos se resuelven y que los ambiguos no se fuerzan.

**Acceptance Scenarios**:

1. **Given** jugadores existentes con equipo y liga textuales, **When** se completa la transición al nuevo modelo, **Then** cada jugador queda asociado a un único equipo interno correspondiente a su equipo textual y su liga resulta de la de ese equipo.
2. **Given** un jugador existente cuyo equipo puede asociarse de forma inequívoca a un equipo del nuevo modelo, **When** se completa la transición, **Then** ese jugador queda asociado a ese equipo y no se genera ningún equipo adicional por aproximación.
3. **Given** un jugador existente que no puede asociarse de forma inequívoca a un equipo, **When** se completa la transición, **Then** no se le asigna un equipo por coincidencia aproximada ni por selección de la opción más probable, y el caso queda identificado para revisión.
4. **Given** un catálogo ya migrado, **When** se consulta el catálogo o se ejecuta una sincronización completa y exitosa, **Then** no se pierden jugadores, no se duplican y no se generan equipos ni ligas duplicados.

## Edge Cases

- Un equipo cambia de nombre en el proveedor: se conserva su identidad interna y se actualiza el nombre; los jugadores asociados no cambian de equipo.
- Una liga cambia de nombre en el proveedor: se conserva su identidad interna y se actualiza el nombre.
- Un equipo cambia de liga entre dos de las ligas cubiertas: conserva su identidad interna, cambia su liga actual y sus jugadores permanecen asociados a él.
- Un equipo pasa a una liga ajena a las cinco ligas actualmente cubiertas: deja de considerarse parte del catálogo actual, conservando identidad y referencias externas.
- Un equipo vuelve a una liga cubierta: vuelve al catálogo actual y sus jugadores vuelven a estar activos.
- Un equipo que ya no aparece en la fuente no se elimina físicamente, igual que los jugadores que ya no están presentes.
- Un jugador recibido sin equipo resoluble: no se incorpora ni se actualiza, y no queda ningún jugador sin equipo.
- Un jugador recibido cuya liga no puede resolverse pero cuyo equipo sí: la liga no se incorpora ni se actualiza y el caso queda registrado.
- Un equipo recibido sin liga resoluble: no se incorpora ni se actualiza.
- Una identidad externa de proveedor que ya pertenece a otro equipo interno no se reasigna: el caso se registra y se continúa con los demás.
- Dos equipos internos distintos no pueden compartir una misma identidad externa concreta de un proveedor.
- Un equipo sin identidad TheSportsDB sigue siendo válido y consultable, con su identidad interna y su referencia de origen.
- Un equipo con identidad TheSportsDB conocida no se vuelve a resolver por otro equipo ni se reasigna.
- La ausencia de la referencia TheSportsDB de un equipo nunca provoca que su jugador se descarte, siempre que su equipo y liga sí se resuelvan.
- Una migración de jugador con equipo textual que corresponde a varios equipos posibles, o a ninguno, no se resuelve por aproximación ni por probabilidad.
- Un cambio de nombre en el proveedor que afecta simultáneamente a la liga y al equipo no crea entidades adicionales.
- Una sincronización incompleta o fallida no debe provocar la salida del catálogo actual de equipos o jugadores que en realidad siguen presentes, ni cambios de liga no confirmados.
- Los valores textuales de equipo y liga no se conservan como fuente de verdad adicional una vez completada la transición; cualquier valor que se exponga en el catálogo se deriva de la identidad interna.
- Las referencias externas de equipos y ligas no se exponen en la consulta normal del catálogo de jugadores.
- Las temporadas, el historial por temporadas y los ascensos y descensos no forman parte del modelo ni del comportamiento de esta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **RF-001**: El dominio debe incorporar `Team` y `League` como conceptos propios de FootballMarket, cada uno con una identidad interna propia, gestionada y asignada por FootballMarket e independiente de cualquier proveedor externo.
- **RF-002**: El modelo de dominio no debe quedar estructuralmente limitado a un número fijo de ligas ni a las cinco ligas actuales. Ningún comportamiento del dominio puede dar por supuesto que existen exactamente cinco ligas.
- **RF-003**: La configuración efectiva de la sincronización continúa determinando qué ligas se consultan, y en el estado actual son Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`). Esta restricción es de alcance de la sincronización, no del modelo de dominio.
- **RF-004**: Cada `Player` debe estar asociado a un único `Team` interno de FootballMarket. Un jugador no asociado a ningún equipo no es un resultado válido de esta feature.
- **RF-005**: `Player` no mantiene una asociación directa e independiente con `League`. La liga de un jugador se determina exclusivamente a través de su `Team`.
- **RF-006**: `Player.team` y `Player.league`, en su forma actual de texto libre, dejan de ser la fuente de verdad del catálogo. Ninguna regla funcional, validación ni sincronización puede depender de esos valores textuales como criterio de identidad o de asociación una vez completada la transición.
- **RF-007**: Cada `Team` pertenece actualmente a una única `League`. Si en el futuro se requiere pertenecer a varias simultáneas, esa posibilidad queda fuera del alcance de esta feature.
- **RF-008**: Temporadas, historial por temporadas e historial de ascensos y descensos no forman parte del modelo ni del comportamiento de esta feature.
- **RF-009**: Si un `Team` cambia actualmente de liga, debe conservar su identidad interna y actualizar su liga actual. Los `Player` asociados conservan su asociación al mismo `Team`.
- **RF-010**: `Player`, `Team` y `League` deben poder conservar referencias externas independientes, una por proveedor, con el identificador que ese proveedor les asigna. La referencia externa no forma parte de la identidad interna de la entidad.
- **RF-011**: Para `Team` y `League`, la referencia externa `FOOTBALL_DATA` es la referencia de origen requerida para toda entidad incorporada mediante la sincronización del catálogo. Una entidad incorporada por esa vía nace junto con esa referencia.
- **RF-012**: Una referencia externa, identificada por la combinación de proveedor e identificador externo, debe identificar de forma unívoca a una sola entidad interna del mismo tipo. Ningún identificador externo puede sustituir al identificador interno de FootballMarket ni parte de la identidad pública de la entidad.
- **RF-013**: Un cambio de nombre informado por un proveedor no crea una entidad nueva. El sistema debe conservar la identidad interna existente y actualizar el nombre actual de esa `Team` o `League`.
- **RF-014**: Durante la sincronización del catálogo desde Football-Data.org, el sistema debe reconocer o incorporar las `League` y los `Team` necesarios antes de asociar o actualizar cualquier `Player` afectado.
- **RF-015**: Si no puede resolverse correctamente el `Team` de un jugador, el jugador no debe incorporarse ni actualizarse. El caso debe quedar registrado y no debe producir un jugador sin equipo ni una asociación aproximada.
- **RF-016**: Si no puede asociarse correctamente un `Team` a una `League`, ese equipo no debe incorporarse ni actualizarse. El caso debe quedar registrado.
- **RF-017**: Los `Player` existentes deben migrar funcionalmente al nuevo modelo, quedando cada uno asociado a su `Team` correspondiente. La migración no debe perder jugadores, duplicarlos ni alterar su estado.
- **RF-018**: Si un jugador existente no puede asociarse de forma inequívoca con un `Team`, no debe forzarse una coincidencia aproximada ni elegirse la opción más probable. El caso debe quedar identificado para revisión. El tratamiento exacto de estos casos pendientes está sujeto a la aclaración P-003.
- **RF-019**: Una vez completada la transición, los valores textuales actuales de equipo y liga no deben permanecer como una segunda fuente de verdad en el modelo del jugador. Cualquier dato de equipo o liga expuesto debe derivarse de la identidad interna asociada. El contrato concreto de la consulta está sujeto a la aclaración P-001.
- **RF-020**: En esta feature, `Team` puede obtener y conservar una referencia externa `THE_SPORTS_DB`. La ausencia de esa referencia no invalida al equipo ni impide incorporarlo o actualizarlo, siempre que su identidad interna y su referencia `FOOTBALL_DATA` estén resueltas.
- **RF-021**: FootballMarket debe poder intentar resolver la identidad TheSportsDB de un `Team` a partir de la información disponible del equipo, y conservarla solo cuando la coincidencia sea confiable. Los criterios de confiabilidad están sujetos a la aclaración P-002.
- **RF-022**: Si no puede resolverse la referencia `THE_SPORTS_DB` de un `Team`, el equipo sigue siendo válido con su identidad interna y su referencia `FOOTBALL_DATA`. La falta de TheSportsDB no bloquea el catálogo ni a sus jugadores.
- **RF-023**: Una referencia externa `THE_SPORTS_DB` existente de un `Team` no debe reasignarse a otro `Team` ni sustituirse por el resultado de un nuevo intento de resolución sobre otro equipo.
- **RF-024**: Si una identidad externa recibida ya pertenece a otro `Team`, no debe reasignarse automáticamente. El caso debe registrarse como conflicto de identidad y el procesamiento debe continuar con los demás casos válidos.
- **RF-025**: Si un `Team` deja de formar parte de las ligas actualmente cubiertas por FootballMarket, su identidad y sus referencias externas se conservan y deja de considerarse parte del catálogo actual. No debe eliminarse físicamente.
- **RF-026**: Los `Player` asociados a un `Team` que deja de considerarse parte del catálogo actual también se conservan y dejan de considerarse activos dentro del catálogo vigente. No deben eliminarse físicamente.
- **RF-027**: Si un `Team` o un `Player` vuelve a aparecer en una sincronización completa y exitosa tras haber salido del catálogo actual, debe volver a considerarse parte del catálogo vigente, conservando su identidad interna y sus referencias externas.
- **RF-028**: La consulta del catálogo de jugadores debe exponer, para cada jugador, la identidad interna de su equipo y de su liga junto con el nombre actual del equipo y de la liga, con información conceptualmente equivalente a `teamId`, `teamName`, `leagueId` y `leagueName`. `leagueId` y `leagueName` deben ser los del equipo del jugador.
- **RF-029**: La consulta del catálogo de jugadores no debe exponer las referencias externas de `Team` ni de `League`, ni los proveedores que las originan. Esta restricción se aplica a la consulta normal del catálogo.
- **RF-030**: Esta feature no debe obtener, resolver ni modificar imágenes de jugadores, ni consulta TheSportsDB con el propósito de obtener imágenes. La referencia `THE_SPORTS_DB` de un `Team` se limita a identificar al equipo y no implica comportamiento de imagen alguno. El comportamiento de imágenes corresponde a `006-player-images`.
- **RF-031**: Los casos no resolubles y los conflictos de identidad derivados de las reglas anteriores deben quedar registrados e identificados de forma distinguible, sin incluir credenciales ni información sensible, para permitir la revisión funcional de los casos pendientes.
- **RF-032**: Una sincronización incompleta o fallida no debe alterar la pertenencia al catálogo actual de equipos y jugadores, ni la liga actual de un equipo, más allá de lo confirmado por los datos obtenidos.

### Key Entities

- **Equipo (`Team`)**: organización deportiva del catálogo con identidad interna propia de FootballMarket, nombre actual, una `League` actual, referencias externas propias por proveedor y la condición de formar o no parte del catálogo actual. Su identificador en fuentes externas no forma parte de su identidad.
- **Liga (`League`)**: competición a la que pertenece un equipo, con identidad interna propia de FootballMarket, nombre actual y referencias externas propias por proveedor. No tiene temporada ni historial asociado en esta feature.
- **Jugador (`Player`)**: persona del catálogo con identidad interna propia, que pertenece a un único `Team`. Su liga se deriva del `Team` al que pertenece. No mantiene una liga independiente ni valores textuales de equipo o liga como fuente de verdad.
- **Referencia externa de equipo**: asocia un `Team` con un proveedor externo y con el identificador que ese proveedor le asigna. Es el mecanismo por el que el catálogo reconoce a un equipo ante un proveedor concreto, y nunca sustituye su identidad interna.
- **Referencia externa de liga**: asocia una `League` con un proveedor externo y con el identificador que ese proveedor le asigna, con la misma finalidad y restricciones que la referencia externa de equipo.
- **Proveedor**: identifica la fuente externa de una referencia externa. Admite Football-Data.org y TheSportsDB.
- **Caso pendiente de revisión**: registro funcional de un jugador o un equipo que no pudo asociarse de forma inequívoca, o de un conflicto de identidad externa, para su tratamiento posterior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cada jugador del catálogo está asociado a exactamente un equipo interno, y su liga es la de ese equipo; ningún jugador aparece sin equipo ni con una liga distinta de la de su equipo.
- **SC-002**: Cada liga y cada equipo del catálogo tienen una identidad interna propia, y una misma entidad aparece con una única identidad interna tras sincronizaciones sucesivas, incluso cuando el proveedor cambia su nombre o su liga.
- **SC-003**: Ante un cambio de nombre de equipo o de liga informado por un proveedor, el número de entidades internas del catálogo no aumenta y la entidad afectada conserva su identificador interno.
- **SC-004**: Ante un cambio de liga de un equipo, ese equipo conserva su identificador interno y sus jugadores conservan su asociación al mismo equipo.
- **SC-005**: En una sincronización completa y exitosa, ningún equipo queda incorporado o actualizado sin una liga asociada, y ningún jugador queda incorporado o actualizado sin un equipo resuelto.
- **SC-006**: Ninguna entidad interna del catálogo comparte una identidad externa concreta con otra entidad interna del mismo tipo; los casos de conflicto quedan registrados y no producen reasignaciones.
- **SC-007**: Un equipo sin identidad `THE_SPORTS_DB` aparece en el catálogo igual que uno que sí la tiene, y su disponibilidad no depende de esa resolución.
- **SC-008**: Ningún equipo pierde su identidad interna ni sus referencias externas al salir de las ligas cubiertas, y sus jugadores se conservan dejando de estar activos en el catálogo vigente.
- **SC-009**: Tras la transición, la cantidad de jugadores del catálogo es la misma que antes de ella o superior, sin pérdidas ni duplicados, y ningún jugador conserva una asociación de equipo ambigua.
- **SC-010**: Los jugadores que no pudieron asociarse de forma inequívoca durante la transición son identificables uno a uno en la revisión funcional, y ninguno recibió un equipo por coincidencia aproximada.
- **SC-011**: La consulta del catálogo expone la identidad interna y el nombre del equipo y de la liga de cada jugador, y no expone referencias externas de equipos ni de ligas.
- **SC-012**: Ninguna operación de esta feature obtiene, resuelve ni modifica imágenes de jugadores, ni consulta TheSportsDB con propósito de imagen.

## Assumptions

- Football-Data.org proporciona un identificador estable por equipo y por liga que permite reconocerlos entre sincronizaciones.
- Cada equipo de las ligas consultadas pertenece a una única de esas ligas.
- La información disponible del equipo es suficiente para intentar resolver su identidad TheSportsDB, aunque la resolución puede no ser posible o no ser confiable; esa resolución es siempre opcional para el catálogo.
- Los jugadores existentes que hay que migrar tienen un equipo textual que corresponde a un equipo reconocible del nuevo modelo, salvo los casos que queden identificados para revisión.
- La migración es una transición única; una vez completada, la operación del catálogo se apoya en la identidad interna de `Team` y `League`.
- Esta feature no redefine la identidad ni la semántica general de `Player` ni de `PlayerExternalReference` establecidas en `002-player-catalog`.
- `006-player-images`, así como cualquier otra feature posterior, podrá utilizar las referencias externas de `Team` una vez que esta feature exista; esta feature no asume ni implementa su comportamiento.

## Out of Scope

- Temporadas y cualquier noción de temporada.
- Historial por temporadas de jugadores, equipos y ligas.
- Historial de ascensos y descensos.
- Pertenencia simultánea de un equipo a varias ligas.
- Resolución de referencias `THE_SPORTS_DB` para `League`.
- Obtención, resolución, almacenamiento o actualización de imágenes de jugadores, y cualquier comportamiento propio de `006-player-images`.
- Matching difuso o aproximado de nombres para identificar entidades.
- Selección automática de la coincidencia más probable ante casos ambiguos.
- Eliminación física de equipos, ligas o jugadores históricos cuando dejan de formar parte del catálogo actual.
- Exposición de las referencias externas de `Team` y `League` en la consulta normal del catálogo de jugadores.
- Cambios en el origen y la cadencia de la sincronización del catálogo: esta feature conserva la sincronización manual con Football-Data.org de `002-player-catalog`.
- Nuevas fuentes de datos distintas de Football-Data.org y TheSportsDB.
- Cualquier detalle de implementación: arquitectura, persistencia, migraciones físicas, índices, contratos internos de datos o estructura de paquetes.
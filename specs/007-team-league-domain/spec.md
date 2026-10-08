# Feature Specification: Dominio de equipos y ligas

**Feature Branch**: `[007-team-league-domain]`

**Created**: 2026-10-02

**Status**: Ready for implementation

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Incorporar `Team` y `League` como conceptos propios del dominio de FootballMarket, con identidad interna independiente de los proveedores. El jugador pasa a estar asociado a un único equipo interno y su liga se determina a través de ese equipo; `Player.team` y `Player.league` dejan de ser la fuente de verdad. Esta feature prepara el dominio para que otras features, en particular `006-player-images`, puedan usar las referencias externas de `Team`, sin incluir comportamiento de sincronización de imágenes.

## Clarifications

### Session 2026-10-07 — B-V5-05 opcionales entre referencias distintas

- Valor obligatorio y valor opcional se consolidan por separado. Un conflicto entre valores opcionales válidos de referencias distintas nunca convierte al Player completo en INVALID_SUBJECT_DATA: Team/name/position siguen rigiendo B-V5-03.
- `dateOfBirth` y `nationality` se evalúan por separado sobre el conjunto completo de observaciones válidas del mismo Player. Sin valor válido ninguno no hay valor nuevo: el Player existente conserva el persistido y el nuevo persiste null. Con un único valor válido ese se usa, y un ausente o inválido nunca compite contra uno válido. Con el mismo valor válido repetido se usa ese valor.
- Si hay dos o más valores válidos incompatibles, el conflicto es del atributo opcional: el Player existente conserva su valor persistido y el nuevo persiste null para ese atributo, sin elegir por orden, first/last wins, externalId ni orden de colección. El conflicto se registra y reconcilia con el mecanismo de casos ya definido, sin congelar ni descartar al Player.
- Cada atributo se resuelve con independencia: un conflicto en uno no impide consolidar el otro. El resultado persiste exactamente ese estado y GET lo expone sin normalizar. El enriquecimiento independiente de Team no se ve afectado y B-V5-05 no introduce prioridades entre proveedores.

### Session 2026-10-07 — B-V5-04 y selección canónica

- Valor semántico y presentación son responsabilidades distintas: normalización estricta existente solo compara; persistencia y GET conservan texto original. Un Player existente conserva name/position persistidos cuando son equivalentes a los entrantes; no se reemplazan por una forma cosméticamente mejor. GET devuelve exactamente esos valores persistidos, sin normalizar al leer.
- Sin representación persistida equivalente, las observaciones semánticamente coherentes seleccionan una representación original por ranking determinista: primero mayor preservación de letras con diacríticos, luego casing natural frente a todo mayúsculas/minúsculas, finalmente comparación lexicográfica del texto original. La elección se aplica al conjunto completo de representaciones equivalentes y es independiente del orden. Una representación única se conserva exactamente. Position usa el mismo mecanismo sin introducir equivalencias nuevas. La selección nunca resuelve contradicciones semánticas, que siguen B-V5-03 y protegen integralmente al Player.
- B-V5-04 y su subdecisión de alta quedan resueltos. Las aclaraciones pendientes anteriores son antecedentes reemplazados por esta decisión humana.

### Session 2026-10-07 — B-V5-03

- Q: ¿Qué ocurre si distintas referencias válidas del mismo Player informan name o position incompatibles en la misma foto, incluso con Team coherente? → A: La contradicción del estado del sujeto (Team, name o position) produce INVALID_SUBJECT_DATA para el Player completo. Conservar asociación, actividad, referencias, name, position y espejo legacy; no actualizar parcialmente, transferir, retirar ni reactivar. Otros Players válidos continúan. La protección solo corresponde a esa foto; una posterior coherente permite procesamiento normal sin bloqueo por el caso histórico. Múltiples referencias coherentes no son un error. Para detectar contradicciones textuales se reutilizan trim, colapso de espacios, comparación independiente de mayúsculas y diacríticos ya definidos por SPEC-007, sin reglas nuevas, aliases ni fuzzy. Team se compara por identidad interna resuelta.
- Antecedente B-V5-04 resuelto por la sesión posterior: conservar representación persistida equivalente; sin ella, ranking determinista de originales. No seleccionar por orden ni utilizar la clave de comparación como presentación.

### Session 2026-10-07 — B-V5-02

- Q: ¿Qué ocurre si referencias válidas distintas del mismo Player resuelven Teams válidos diferentes en una misma foto? → A: El sujeto Player es inconsistente para esa sincronización: `INVALID_SUBJECT_DATA`. Conserva todas sus referencias, asociación, actividad y datos persistidos derivados de la asociación; no se transfiere, retira ni reactiva ni recibe fallback. No se elige una observación por orden o identificador. Los demás Players válidos continúan. La protección se calcula nuevamente por foto: una foto posterior coherente permite procesamiento normal, aunque permanezca el caso histórico sin cierre automático. Referencias que resuelven al mismo Team se procesan normalmente según las reglas existentes.

### Session 2026-10-07 — B-V5-01

- Q: ¿V5 puede rechazar o reducir múltiples referencias legacy válidas del mismo proveedor para un Player? → A: No. Debe conservar todas, sin selección, eliminación, sobrescritura ni regularización externa previa. Player conserva la cardinalidad de SPEC-002: varias referencias por proveedor con identidades externas distintas. Team y League mantienen una por proveedor. Para los tres tipos, una identidad `(provider, externalId)` pertenece a un único propietario del mismo tipo; esta unicidad de identidad no limita cuántas identidades puede conservar un Player.

Las decisiones de migración, transacciones, revisión, entrega y alcance visual se consolidan en la Session 2026-10-06. La clasificación visual no exige continuidad histórica ante renombrados. Las sesiones posteriores registran las aclaraciones sobre múltiples referencias y sus decisiones pendientes.

### Session 2026-10-06

Q: ¿El contrato del catálogo conserva `team` y `league` actuales como valores derivados, o se reemplaza? → A: Se reemplazan. El contrato expone únicamente `teamId`, `teamName`, `leagueId` y `leagueName`, derivados de la identidad interna. `team` y `league` no se exponen, ni siquiera deprecados. El ajuste de consumidores, incluido el frontend, es alcance de 007 sin modificar la spec 005. La clasificación mantiene su criterio por nombre y fallback neutral; no promete conservar estilo ante renombrados.

Q: ¿Qué hace confiable una coincidencia TheSportsDB? → A: Exactamente una identidad coincide con el nombre principal bajo comparación estricta y puede acreditarse la completitud relevante de la búsqueda. Un resultado recibido no demuestra unicidad. Sin completitud acreditable no se asigna referencia. No se presume acceso Premium ni se toleran sufijos o matching aproximado.

Q: ¿Los jugadores existentes que no pueden asociarse de forma inequívoca bloquean la finalización de la transición? → A: No. La migración se completa igualmente y esos jugadores se conservan sin equipo, identificados como pendientes de revisión. La asociación a un `Team` es obligatoria en el estado normal del catálogo; la ausencia de equipo solo es admisible en esa situación transitoria y delimitada.

Q: ¿Cómo se comunica la revisión? → A: Registro persistido y deduplicado, con causa estable, sujeto, evidencia y primera/última detección; sin `status` ni cierre automático. Un operador autorizado lo consulta mediante SQL documentado con permisos de solo lectura. No hay endpoint, frontend ni comando adicional.

Q: ¿Qué determina la vigencia del Team? → A: Indicador explícito recalculado al confirmar foto completa: ausente no protegido sale, presente válido entra, protegido conserva estado previo. League no tiene indicador; configuración no cambia vigencia por sí sola.

Q: ¿Qué dato determina la asociación inicial? → A: Presente válido: referencias FOOTBALL_DATA del jugador y del equipo en la foto. Ausente y aún sin equipo: backfill estricto por texto legacy contra equipos vigentes. Presente inválido: conservar estado, registrar y proteger de ausencia, sin fallback textual. RF-034 protege el backfill, no prioriza historia sobre identidad actual.

Q: ¿Qué consume la oportunidad de resolución externa? → A: Solo una evaluación técnicamente válida. Los fallos técnicos permiten intentar nuevamente en otra sincronización sin cambio de nombre. Como máximo una tentativa lógica por equipo y ejecución; se respeta Retry-After. TheSportsDB se procesa después del commit principal, con persistencia independiente por equipo.

Q: ¿Qué información conserva el registro de un jugador que quedó pendiente de revisión durante la migración? → A: El registro del caso pendiente conserva el texto de equipo que tenía el jugador y los equipos con los que no pudo decidirse, y el jugador queda sin equipo. Ese texto no vuelve al modelo del jugador, de modo que no se reintroduce la segunda fuente de verdad que RF-006 prohíbe.

- Q: ¿Qué garantiza la aplicación de la foto? → A: Obtención completa previa y un único commit local; fallo técnico revierte todo. Presencia identificable inválida se protege; identidad insuficiente que compromete presencias o respuesta requerida incompleta impide aplicar la foto.
- Q: ¿Cómo se entrega la transición? → A: Dos releases es una decisión técnica aprobada, no una consecuencia de RF-011. Se prepara, sincroniza y verifica antes de eliminar textos. Tras su eliminación no se garantiza downgrade directo ni recuperación exacta de textos sin restauración explícita.
- Q: ¿Debe conservarse automáticamente la clasificación visual tras renombrar una League? → A: No. Se aprueba la alternativa C: leagueId conserva identidad interna independiente de proveedores y leagueName informa el nombre actual. El frontend sigue clasificando por leagueName, con fallback neutral existente si no reconoce el nombre. Un rename puede cambiar temporalmente al estilo neutral; no se agregan mapas por leagueId ni campos de clasificación al contrato.

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
3. **Given** un jugador identificable recibido cuyo equipo no puede resolverse, **When** se procesa la sincronización, **Then** no se incorpora ni se actualiza, se registra el caso, se conserva su asociación y actividad previas y no se desactiva por ausencia; un preexistente aún sin equipo permanece fuera del catálogo.
4. **Given** un equipo recibido cuya liga no puede resolverse, **When** se procesa la sincronización, **Then** el equipo no se incorpora ni se actualiza y el caso queda registrado.
5. **Given** las mismas ligas y equipos informados en sincronizaciones sucesivas, **When** se procesa cada sincronización, **Then** el catálogo conserva una única identidad interna por liga y por equipo, sin duplicados.
6. **Given** un jugador existente válido, **When** se procesa la sincronización, **Then** se reconoce por su referencia externa y conserva su identidad interna, asociándose al equipo actual reconocido por su propia referencia; una transferencia no exige conservar el equipo anterior.
7. **Given** varias referencias válidas de un mismo Player que resuelven al mismo Team, **When** se aplica la foto, **Then** se procesa normalmente y conserva todas las referencias.
8. **Given** varias referencias válidas del mismo Player que resuelven Teams válidos distintos, **When** se aplica la foto en cualquier orden de referencias, **Then** se registra `INVALID_SUBJECT_DATA` para ese Player y se conservan su asociación, actividad, referencias y datos persistidos derivados de la asociación, sin transferencia, retirada ni reactivación; los demás Players válidos se procesan normalmente.
9. **Given** un Player con ese caso histórico, **When** una foto posterior produce una asociación coherente, **Then** se procesa normalmente conforme a las reglas existentes; la existencia del caso no impone invalidez permanente.

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
2. **Given** un equipo que deja de formar parte del catálogo actual, **When** se confirma la foto, **Then** sus jugadores se conservan y se desactivan salvo protecciones RF-039; el GET siempre exige Team vigente incluso para un Player protegido que conserva active.
3. **Given** un equipo que reaparece válido en una foto completa, **When** se confirma, **Then** vuelve al catálogo y sus jugadores válidos presentes se reactivan conforme RF-027; jugadores presentes inválidos conservan estado y no se activan por inferencia.

---

### User Story 6 - Migrar el catálogo existente sin perder jugadores (Priority: P1)

Como usuario del catálogo, quiero que los jugadores existentes queden asociados a su equipo correspondiente sin que se pierdan ni se dupliquen, y que los casos dudosos queden identificados.

**Why this priority**: Sin la transición, la feature no aporta valor al catálogo existente.

**Independent Test**: Partir de un catálogo con textos de equipo y liga y comprobar que los jugadores quedan asociados a un equipo interno, que los casos inequívocos se resuelven y que los ambiguos no se fuerzan.

**Acceptance Scenarios**:

1. **Given** un jugador existente presente válido en la foto, **When** se sincroniza, **Then** se asocia al Team actual por referencias FOOTBALL_DATA aunque contradiga el texto legacy, conservando su identidad interna.
2. **Given** un jugador ausente de una foto completa y aún sin equipo, **When** se realiza el backfill, **Then** una única coincidencia estricta con un equipo vigente permite asociarlo sin modificar por sí sola su actividad; cero o varias generan un caso con evidencia original.
3. **Given** un jugador presente con equipo no resoluble, **When** se aplica la foto, **Then** no hay fallback textual, conserva asociación y active previos, se registra y se protege de inactivación por ausencia; si carecía de equipo sigue fuera del catálogo.
4. **Given** un catálogo ya migrado, **When** se consulta el catálogo o se ejecuta una sincronización completa y exitosa, **Then** no se pierden jugadores, no se duplican y no se generan equipos ni ligas duplicados.

## Edge Cases

- Referencias distintas de un mismo Player con Teams válidos contradictorios en la misma foto: invalidez del sujeto `INVALID_SUBJECT_DATA`, no de toda la foto; protección de asociación/actividad/datos derivados y conservación de todas las referencias, independiente del orden. Una foto posterior coherente elimina la protección de esa ejecución, no el caso histórico. Si aún no tenía Team, permanece sin él y sin fallback, con evidencia original para la transición.

- Un equipo cambia de nombre en el proveedor: se conserva su identidad interna y se actualiza el nombre; los jugadores asociados no cambian de equipo.
- Una liga cambia de nombre en el proveedor: se conserva su identidad interna y se actualiza el nombre.
- Un equipo cambia de liga entre dos de las ligas cubiertas: conserva su identidad interna, cambia su liga actual y sus jugadores permanecen asociados a él.
- Un equipo pasa a una liga ajena a las cinco ligas actualmente cubiertas: deja de considerarse parte del catálogo actual, conservando identidad y referencias externas.
- Un equipo reaparece válido en una liga cubierta: vuelve al catálogo; actividad de cada jugador respeta presencia válida y protecciones, no se activa todo el historial automáticamente.
- Un equipo que ya no aparece en la fuente no se elimina físicamente, igual que los jugadores que ya no están presentes.
- Un jugador identificable recibido sin equipo resoluble conserva estado previo; si aún no tenía asociación permanece sin equipo, registrado y fuera del catálogo, sin fallback legacy.
- La liga del jugador nunca se resuelve independientemente: una asociación Team–League inválida impide procesar al equipo y protege las dependencias cuya presencia no puede determinarse.
- Un equipo recibido sin liga resoluble: no se incorpora ni se actualiza.
- Una identidad externa de proveedor que ya pertenece a otro equipo interno no se reasigna: el caso se registra y se continúa con los demás.
- Dos equipos internos distintos no pueden compartir una misma identidad externa concreta de un proveedor.
- Un equipo sin identidad TheSportsDB sigue siendo válido y consultable, con su identidad interna y su referencia de origen.
- Un equipo con identidad TheSportsDB conocida no se vuelve a resolver por otro equipo ni se reasigna.
- La ausencia de la referencia TheSportsDB de un equipo nunca provoca que su jugador se descarte, siempre que su equipo y liga sí se resuelvan.
- El backfill de un ausente sin asociación usa texto estricto: `Arsenal` no coincide con `Arsenal FC`; no se usa liga textual para desambiguar. Cero o varias coincidencias dejan evidencia para revisión y no bloquean el cierre.
- Dos identidades externas diferentes con nombre normalizado igual son ambiguas independientemente del orden. Duplicados de la misma identidad no representan candidatos distintos. Resultados sin completitud acreditable no permiten asignar referencia.
- Un cambio de nombre en el proveedor que afecta simultáneamente a la liga y al equipo no crea entidades adicionales.
- Una sincronización incompleta o fallida no debe provocar la salida del catálogo actual de equipos o jugadores que en realidad siguen presentes, ni cambios de liga no confirmados.
- Los valores textuales de equipo y liga no se conservan como fuente de verdad adicional una vez completada la transición, y no se exponen en el contrato de la consulta: cualquier valor que se exponga se deriva de la identidad interna.
- Las referencias externas de equipos y ligas no se exponen en la consulta normal del catálogo de jugadores.
- Las temporadas, el historial por temporadas y los ascensos y descensos no forman parte del modelo ni del comportamiento de esta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **RF-001**: El dominio debe incorporar `Team` y `League` como conceptos propios de FootballMarket, cada uno con una identidad interna propia, gestionada y asignada por FootballMarket e independiente de cualquier proveedor externo.
- **RF-002**: El modelo de dominio no debe quedar estructuralmente limitado a un número fijo de ligas ni a las cinco ligas actuales. Ningún comportamiento del dominio puede dar por supuesto que existen exactamente cinco ligas.
- **RF-003**: La configuración efectiva de la sincronización continúa determinando qué ligas se consultan, y en el estado actual son Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`). Esta restricción es de alcance de la sincronización, no del modelo de dominio.
- **RF-004**: Cada `Player` debe estar asociado a un único `Team` interno de FootballMarket. Un jugador sin equipo no es un resultado válido del catálogo, salvo el estado transitorio y delimitado de jugador pendiente de revisión definido en RF-018, que no se considera parte del catálogo vigente.
- **RF-005**: `Player` no mantiene una asociación directa e independiente con `League`. La liga de un jugador se determina exclusivamente a través de su `Team`.
- **RF-006**: Los textos legacy dejan de ser fuente de verdad. Durante la transición solo sirven para backfill de ausentes aún sin asociación y evidencia de revisión. Una asociación interna resuelta prevalece; las escrituras temporales de compatibilidad no gobiernan identidad ni consulta. Completada la transición se retiran del jugador conforme a RF-019 y RF-036.
- **RF-007**: Cada `Team` pertenece actualmente a una única `League`. Si en el futuro se requiere pertenecer a varias simultáneas, esa posibilidad queda fuera del alcance de esta feature.
- **RF-008**: Temporadas, historial por temporadas e historial de ascensos y descensos no forman parte del modelo ni del comportamiento de esta feature.
- **RF-009**: Si un `Team` cambia actualmente de liga, debe conservar su identidad interna y actualizar su liga actual. Los `Player` asociados conservan su asociación al mismo `Team`.
- **RF-010**: `Player`, `Team` y `League` conservan referencias externas independientes de su identidad interna. `Player` puede conservar varias referencias de un mismo proveedor si sus identificadores externos son distintos, manteniendo la cardinalidad de SPEC-002 y todas las referencias legacy válidas. `Team` y `League` conservan como máximo una referencia por proveedor. Para los tres tipos se exige RF-012: una misma identidad concreta de proveedor no puede pertenecer a múltiples propietarios del mismo tipo. No se limita a una referencia por proveedor para Player ni se eliminan, seleccionan o sobrescriben referencias históricas para migrarlo.
- **RF-011**: Para `Team` y `League`, la referencia externa `FOOTBALL_DATA` es la referencia de origen requerida para toda entidad incorporada mediante la sincronización del catálogo. Una entidad incorporada por esa vía nace junto con esa referencia.
- **RF-012**: Una referencia externa, identificada por la combinación de proveedor e identificador externo, debe identificar de forma unívoca a una sola entidad interna del mismo tipo. Ningún identificador externo puede sustituir al identificador interno de FootballMarket ni parte de la identidad pública de la entidad.
- **RF-013**: Un cambio de nombre informado por un proveedor no crea una entidad nueva. El sistema debe conservar la identidad interna existente y actualizar el nombre actual de esa `Team` o `League`.
- **RF-014**: Durante la sincronización del catálogo desde Football-Data.org, el sistema debe reconocer o incorporar las `League` y los `Team` necesarios antes de asociar o actualizar cualquier `Player` afectado.
- **RF-015**: Un jugador identificable presente con Team no resoluble no se incorpora ni actualiza ni recibe fallback legacy. Conserva asociación y active previos y se protege de ausencia. Un preexistente sin asociación permanece sin ella y fuera del catálogo; se registra el caso. No se crea un jugador nuevo sin equipo.
- **RF-016**: Si no puede asociarse correctamente un `Team` a una `League`, ese equipo no debe incorporarse ni actualizarse. El caso debe quedar registrado.
- **RF-017**: La transición conserva identidad interna, referencias y jugadores sin pérdidas ni duplicados. Los presentes válidos se asocian por sus referencias FOOTBALL_DATA y las del Team de la foto como sincronización normal; los ausentes aún sin asociación usan RF-034; los presentes inválidos usan RF-015. La transición por sí sola preserva active; los cambios normales de actividad se rigen por la sincronización y sus protecciones.
- **RF-018**: No se fuerzan asociaciones. Un preexistente sin equipo y no resoluble se conserva fuera del catálogo con caso y evidencia; ello no bloquea finalizar la transición. Un caso sobre un jugador ya asociado no elimina su asociación ni lo excluye por su sola existencia. No hay resolución manual ni cierre automático de casos en esta feature.
- **RF-019**: Una vez completada la transición, los valores textuales actuales de equipo y liga no deben permanecer en el modelo del jugador ni como segunda fuente de verdad. Cualquier dato de equipo o liga expuesto debe derivarse de la identidad interna asociada. El contrato de la consulta expone únicamente `teamId`, `teamName`, `leagueId` y `leagueName`; los campos `team` y `league` se retiran del contrato y no se mantienen como valores derivados ni deprecados. Los consumidores del catálogo deben ajustarse a ese contrato dentro del alcance de esta feature.
- **RF-020**: En esta feature, `Team` puede obtener y conservar una referencia externa `THE_SPORTS_DB`. La ausencia de esa referencia no invalida al equipo ni impide incorporarlo o actualizarlo, siempre que su identidad interna y su referencia `FOOTBALL_DATA` estén resueltas.
- **RF-021**: Se intenta resolver Team en TheSportsDB según RF-035. Solo se asigna referencia si exactamente una identidad de equipo de fútbol coincide con su nombre principal y se acredita que los resultados relevantes no están truncados ni restringidos de modo que oculten coincidencias. La comparación ignora mayúsculas y diacríticos, hace trim y colapsa espacios, sin quitar sufijos ni fuzzy: `Arsenal` coincide con `ARSENAL` pero no con `Arsenal FC`. Se cuentan identidades distintas, no filas ni orden. Cero coincidencias, múltiples coincidencias y completitud no acreditable son resultados distintos sin asignación. No se presume acceso Premium ni completitud por recibir una sola fila.
- **RF-022**: Si no puede resolverse la referencia `THE_SPORTS_DB` de un `Team`, el equipo sigue siendo válido con su identidad interna y su referencia `FOOTBALL_DATA`. La falta de TheSportsDB no bloquea el catálogo ni a sus jugadores.
- **RF-023**: Una referencia externa `THE_SPORTS_DB` existente de un `Team` no debe reasignarse a otro `Team` ni sustituirse por el resultado de un nuevo intento de resolución sobre otro equipo.
- **RF-024**: Si se pretende asignar una identidad externa a una entidad distinta de su propietario, no debe reasignarse automáticamente. Se registra conflicto para Player, Team o League y se continúa con casos válidos según RF-037. Encontrar la referencia para actualizar a su propietario es el camino normal, no conflicto.
- **RF-025**: Si un `Team` deja de formar parte de las ligas actualmente cubiertas por FootballMarket, su identidad y sus referencias externas se conservan y deja de considerarse parte del catálogo actual. No debe eliminarse físicamente. La pertenencia al catálogo vigente se rige por el indicador explícito de RF-033.
- **RF-026**: Los Players de un Team que sale del catálogo se conservan y se desactivan conforme a las protecciones RF-039; un Player protegido conserva active y no aparece si su Team no es vigente (RF-028). No se eliminan físicamente.
- **RF-027**: Un Team o Player que reaparece con datos válidos en una foto completa vuelve al catálogo conservando identidad y referencias. Una presencia inválida conserva el estado previo, no provoca reactivación por sí sola.
- **RF-028**: La consulta solo incluye jugadores activos, con Team asociado vigente y League válida. Expone obligatoriamente teamId, teamName, leagueId y leagueName derivados de esas entidades. La existencia de casos de revisión no produce error ni excluye por sí sola a un jugador válido.
- **RF-029**: La consulta del catálogo de jugadores no debe exponer las referencias externas de `Team` ni de `League`, ni los proveedores que las originan. Esta restricción se aplica a la consulta normal del catálogo.
- **RF-030**: Esta feature no debe obtener, resolver ni modificar imágenes de jugadores, ni consulta TheSportsDB con el propósito de obtener imágenes. La referencia `THE_SPORTS_DB` de un `Team` se limita a identificar al equipo y no implica comportamiento de imagen alguno. El comportamiento de imágenes corresponde a `006-player-images`.
- **RF-031**: Los problemas individuales identificables y conflictos se registran persistentemente con categoría, código estable de causa, sujeto, primera/última detección y evidencia necesaria, sin secretos. El mismo problema estable actualiza su registro; no se usa descripción libre como clave. Sujeto existente: tipo e id interno; no creado: tipo, proveedor e id externo; sin identidad suficiente: observación por ejecución/contexto, nunca identidad inventada por nombre. Categorías: asociación legacy (cero/múltiples coincidencias), jugador con Team no resoluble, Team con League no resoluble, conflicto externo (tipo, proveedor, id externo y sujeto pretendido; evidencia de ambos propietarios), e invalidez individual identificable no cubierta por las anteriores. No hay status, resolución ni cierre automático: el registro no certifica vigencia futura del problema. Un operador autorizado revisa mediante SQL documentado con solo lectura; no se agrega contrato HTTP ni interfaz de revisión.
- **RF-032**: Una foto incompleta no se aplica. Completa significa obtener todas las respuestas requeridas para las ligas configuradas y poder determinar presencias/ausencias confiables; exitosa significa confirmar íntegramente su aplicación local. Un fallo técnico de persistencia revierte todos los cambios del intento, incluidos casos y actividad. Los errores externos de obtención se diagnostican sin generar casos dentro de una aplicación que no ocurrió.
- **RF-033**: Team tiene indicador explícito de vigencia, no derivado de configuración; League no lo tiene. Al confirmar una foto completa los Teams válidos presentes son vigentes y los ausentes dejan de serlo; los protegidos conservan indicador previo. Cambiar configuración no modifica por sí solo la vigencia.
- **RF-034**: Solo un Player preexistente ausente de una foto completa y aún sin equipo recibe backfill legacy contra Teams vigentes. La comparación exacta ignora mayúsculas/diacríticos, hace trim y colapsa espacios, sin sufijos, fuzzy ni liga textual para desambiguar. Una coincidencia asocia; cero/varias registran evidencia. No prevalece sobre una relación actual válida por identidad externa ni se aplica a presentes inválidos. Tras finalizar la transición no se vuelve a usar texto de casos como fallback automático.
- **RF-035**: Después del commit principal, un Team sin referencia puede intentarse si no tiene evaluación válida previa para su nombre actual. Solo evaluación técnicamente válida consume la oportunidad: coincidencia, cero, múltiples o completitud no acreditable con respuesta estructuralmente válida. Timeout, transporte, 429, 5xx, 4xx inválido para la operación y estructura inválida no consumen y permiten otra sincronización sin cambio de nombre. Máximo una tentativa lógica por equipo y ejecución; cualquier retry interno debe ser explícitamente acotado y respetar Retry-After. Se distinguen última llamada/resultado técnico y última evaluación válida/nombre. Una referencia resuelta no se reevalúa.
- **RF-036**: Antes de retirar textos legacy, cada Player debe tener asociación válida o caso permitido con evidencia original suficiente. Casos de backfill conservan texto original y candidatos observados; presentes no resolubles aún sin asociación conservan texto y datos recibidos. La evidencia original nunca se sobrescribe por detecciones posteriores. No se pierden ni duplican Players; retirar textos no promete recuperar exactamente todos los valores originales mediante nombres actuales.
- **RF-037**: La foto de Football-Data se obtiene completa e inmutable antes de la escritura. Su aplicación tiene un único commit que incluye League/Team/Player, referencias FOOTBALL_DATA, asociaciones, backfill, casos y cambios de vigencia/active. Las llamadas externas no mantienen escritura abierta. Conflictos reconocidos pueden revertir y reaplicar la misma foto excluyendo el caso conflictivo y registrándolo en un intento válido; no se continúa dentro de una transacción fallida.
- **RF-038**: TheSportsDB se procesa solo después del commit principal. Cada equipo se consulta sin escritura abierta y persiste intento y eventual referencia atómicamente en una transacción independiente. Sus fallos no revierten ni convierten en fallida la sincronización principal confirmada; enriquecimiento parcial entre equipos es válido.
- **RF-039**: La foto distingue presentes, procesables y protegidos. Un sujeto identificable inválido conserva estado, registra caso y permite continuar; su presencia se conserva aunque no se actualice. Team inválido conserva vigencia y protege a jugadores previos si no puede determinarse su plantel; Player inválido conserva asociación y active y no se desactiva por ausencia. Identidad insuficiente que impide determinar presencia/ausencia, respuesta requerida faltante/incompleta o fallo de obtención hacen incompleta toda la foto, sin cambios locales ni casos de esa aplicación. Se registra diagnóstico técnico, no se aproxima identidad; opcionales ausentes no invalidan por sí solos.

### Key Entities

B-V5-03 generaliza la protección RF-039/B-V5-02 al estado del Player: Team interno, name y position deben ser coherentes entre observaciones válidas del mismo propietario en una foto. Una incompatibilidad en cualquiera de esos atributos clasifica al sujeto completo una sola vez como INVALID_SUBJECT_DATA, independientemente del orden y del número de atributos contradictorios. Antes de comparar name/position aplicar únicamente la normalización estricta existente de SPEC-007; equivalentes no son conflictos. Conservar integralmente estado persistido, referencias y espejo, sin actualización parcial; otros Players continúan y una foto posterior coherente vuelve al procesamiento normal. La representación textual coherente sigue B-V5-04 y su ranking aprobado; no confundir clave de comparación con valor de presentación.

B-V5-02 concreta RF-039: varias referencias válidas del mismo Player con Teams válidos distintos en una misma foto constituyen `INVALID_SUBJECT_DATA` del sujeto Player. Conservar referencias, asociación persistida (incluida ausencia transitoria), actividad y datos persistidos derivados; no transferir, retirar, reactivar ni elegir por orden o externalId. Registrar evidencia de las asociaciones contradictorias y continuar los demás Players válidos. La clasificación se recalcula por foto; referencias coherentes permiten procesamiento normal posterior y el caso histórico no lo bloquea. Referencias distintas que resuelven al mismo Team no son este conflicto. Un preexistente aún sin Team conserva evidencia original y queda pendiente conforme RF-018/RF-036, sin backfill por presencia inválida.

- **Equipo (`Team`)**: organización deportiva del catálogo con identidad interna propia de FootballMarket, nombre actual, una `League` actual, referencias externas propias por proveedor y un indicador explícito de formar o no parte del catálogo vigente. Su identificador en fuentes externas no forma parte de su identidad.
- **Liga (`League`)**: competición a la que pertenece un equipo, con identidad interna propia de FootballMarket, nombre actual y referencias externas propias por proveedor. No tiene temporada ni historial asociado en esta feature, ni indicador propio de formar parte del catálogo vigente.
- **Jugador (`Player`)**: persona con identidad interna y un Team del que deriva su liga. Durante preparación puede estar aún sin asociación; al finalizar solo los casos permitidos pueden seguir sin ella, fuera del catálogo. Un caso sobre Player asociado no elimina asociación. No mantiene liga independiente ni texto como verdad tras transición.
- **Referencia externa de equipo**: asocia un `Team` con un proveedor externo y con el identificador que ese proveedor le asigna. Es el mecanismo por el que el catálogo reconoce a un equipo ante un proveedor concreto, y nunca sustituye su identidad interna.
- **Referencia externa de liga**: asocia una `League` con un proveedor externo y con el identificador que ese proveedor le asigna, con la misma finalidad y restricciones que la referencia externa de equipo.
- **Proveedor**: identifica la fuente externa de una referencia externa. Admite Football-Data.org y TheSportsDB.
- **Caso pendiente de revisión**: problema registrado según RF-031 con sujeto, causa estable, evidencia y primera/última detección. No tiene status ni ciclo de resolución. Puede referirse a una entidad no creada identificada externamente; observaciones sin identidad suficiente se distinguen por contexto cuando puedan registrarse sin invalidar la foto. Se consulta por SQL autorizado, no por un Repository como interfaz humana.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cada jugador del catálogo vigente está asociado a exactamente un equipo interno, y su liga es la de ese equipo; ningún jugador del catálogo vigente aparece sin equipo ni con una liga distinta de la de su equipo.
- **SC-002**: Cada liga y cada equipo del catálogo tienen una identidad interna propia, y una misma entidad aparece con una única identidad interna tras sincronizaciones sucesivas, incluso cuando el proveedor cambia su nombre o su liga.
- **SC-003**: Ante un cambio de nombre de equipo o de liga informado por un proveedor, el número de entidades internas del catálogo no aumenta y la entidad afectada conserva su identificador interno.
- **SC-004**: Ante un cambio de liga de un equipo, ese equipo conserva su identificador interno y sus jugadores conservan su asociación al mismo equipo.
- **SC-005**: En una sincronización completa y exitosa, ningún equipo queda incorporado o actualizado sin una liga asociada, y ningún jugador queda incorporado o actualizado sin un equipo resuelto.
- **SC-006**: Ninguna entidad interna del catálogo comparte una identidad externa concreta con otra entidad interna del mismo tipo; los casos de conflicto quedan registrados y no producen reasignaciones.
- **SC-007**: Un equipo sin identidad `THE_SPORTS_DB` aparece en el catálogo igual que uno que sí la tiene, y su disponibilidad no depende de esa resolución.
- **SC-008**: Ningún equipo pierde identidad ni referencias al salir; sus jugadores se conservan fuera del catálogo vigente. Los no protegidos se desactivan; los protegidos conservan active pero el filtro de Team vigente impide su exposición.
- **SC-009**: Tras la transición, la cantidad de jugadores del catálogo es la misma que antes de ella o superior, sin pérdidas ni duplicados, y ningún jugador vigente conserva una asociación de equipo ambigua.
- **SC-010**: Los jugadores que no pudieron asociarse de forma inequívoca durante la transición son identificables uno a uno en el registro persistido de casos pendientes, cada registro incluye el texto de equipo con el que no pudo decidirse, ninguno recibió un equipo por coincidencia aproximada, y su existencia no impidió completar la transición.
- **SC-011**: La consulta del catálogo expone `teamId`, `teamName`, `leagueId` y `leagueName` de cada jugador, no expone los campos `team` y `league`, y no expone referencias externas de equipos ni de ligas.
- **SC-012**: Ninguna operación de esta feature obtiene, resuelve ni modifica imágenes de jugadores, ni consulta TheSportsDB con propósito de imagen.

## Assumptions

- Football-Data.org proporciona un identificador estable por equipo y por liga que permite reconocerlos entre sincronizaciones.
- Cada equipo de las ligas consultadas pertenece a una única de esas ligas.
- La información disponible del equipo es suficiente para intentar resolver su identidad TheSportsDB, aunque la resolución puede no ser posible o no ser confiable; esa resolución es siempre opcional para el catálogo.
- Los textos legacy pueden estar desactualizados o ser ambiguos; no se presume que todo caso registrado sea resoluble manualmente.
- V5 conserva todas las referencias legacy válidas de Player, incluso varias del mismo proveedor; no exige regularización externa previa por esa cardinalidad. La unicidad de identidad externa RF-012 ya exigida por SPEC-002 se mantiene.
- Free/Premium y límites de TheSportsDB son dependencias operacionales. No se presume nivel de la clave ni garantía de completitud de searchteams.php; incapacidad de acreditarla produce abstención conforme a RF-021/RF-022.
- La migración es una transición única; una vez completada, la operación del catálogo se apoya en la identidad interna de `Team` y `League`. Los casos pendientes que resten de esa transición no se resuelven dentro de esta feature.
- Esta feature no redefine la identidad ni la semántica general de `Player` ni de `PlayerExternalReference` establecidas en `002-player-catalog`.
- `006-player-images`, así como cualquier otra feature posterior, podrá utilizar las referencias externas de `Team` una vez que esta feature exista; esta feature no asume ni implementa su comportamiento. El comportamiento observable de los filtros `teamName` y `leagueName` de esa feature no cambia por esta transición, porque siguen siendo filtros de texto; lo que cambia es que el texto comparado procede del nombre del `Team` y de la `League` asociados y no de un campo de texto libre del jugador.
- El código del frontend se adapta en 007 para leer teamName/leagueName en lugar de team/league, sin modificar la spec 005. El cambio de campos no introduce una regla visual nueva: clasificación por nombre y fallback existentes se conservan, con el límite de renombrados explicitado a continuación.
- La clasificación visual del frontend sigue dependiendo de leagueName, no de leagueId. Un nombre no reconocido usa el fallback neutral existente. Un rename puede cambiar temporalmente la clasificación hasta que el frontend conozca el nombre nuevo; esta feature no garantiza continuidad visual histórica. leagueId conserva identidad interna estable e independiente de proveedores; no se agregan mapas por ID ni campos de clasificación.

## Out of Scope

- Temporadas y cualquier noción de temporada.
- Historial por temporadas de jugadores, equipos y ligas.
- Historial de ascensos y descensos.
- Pertenencia simultánea de un equipo a varias ligas.
- Resolución posterior de los casos pendientes de revisión que resten de la transición: esta feature los registra y los hace identificables, no los resuelve.
- Estado/cierre automático de casos, endpoint administrativo, frontend de revisión, comando adicional y contador HTTP de revisión.
- Continuidad visual histórica automática tras renombrar una League, mapas de presentación por leagueId y campos nuevos como leagueCode, leagueSlug o visualKey para clasificación.
- Resolución de referencias `THE_SPORTS_DB` para `League`.
- Obtención, resolución, almacenamiento o actualización de imágenes de jugadores, y cualquier comportamiento propio de `006-player-images`.
- Matching difuso o aproximado de nombres para identificar entidades, incluida la tolerancia a sufijos como `FC`, `CF`, `AC` o `SC` en la resolución de la identidad TheSportsDB de un equipo.
- Selección automática de la coincidencia más probable ante casos ambiguos.
- Mapas o correspondencias curadas manualmente entre equipos y proveedores externos: la resolución se rige únicamente por la normalización estricta de RF-021.
- Mantener en el contrato de la consulta los campos textuales `team` y `league`, incluso como valores derivados o deprecados.
- Eliminación física de equipos, ligas o jugadores históricos cuando dejan de formar parte del catálogo actual.
- Exposición de las referencias externas de `Team` y `League` en la consulta normal del catálogo de jugadores.
- Cambios en el origen y la cadencia de la sincronización del catálogo: esta feature conserva la sincronización manual con Football-Data.org de `002-player-catalog`.
- Nuevas fuentes de datos distintas de Football-Data.org y TheSportsDB.
- Cualquier detalle de implementación: arquitectura, persistencia, migraciones físicas, índices, contratos internos de datos o estructura de paquetes.

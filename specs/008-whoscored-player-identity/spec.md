# Feature Specification: Identidad externa de jugadores en WhoScored

**Feature Branch**: `feat/008-whoscored-player-identity` — rama creada y seleccionada el 2026-10-14 desde el estado integrado de `origin/dev` (`8b3b292`), sin stash ni descarte de cambios locales. La rama anterior `feat/008-whoscored-player-statistics` corresponde al alcance estadístico abandonado y queda obsoleta.

**Created**: 2026-10-13

**Status**: DONE - cerrada por autorización humana el 2026-10-15. US01 y US02 en DONE con CODE REVIEW y TESTING aprobados; Polish T046–T050 y remediaciones de Convergence T051–T057 entregadas. CONVERGE final no se ejecutó por decisión humana explícita; la evidencia revisada es la fase de Convergence ya implementada y los resultados de tests aportados por el usuario

**Input**: Registrar la identidad externa de WhoScored de los jugadores persistidos en el catálogo local de las cinco ligas principales, reutilizando sus referencias externas. Descubrir el catálogo actual, resolver coincidencias, conservar pendientes y conflictos, ofrecer un pase histórico complementario y auditar ambas ejecuciones en la respuesta de las propias operaciones de ejecución. Sin interfaz de consulta de ejecuciones, cotizaciones, estadísticas de rendimiento ni programación periódica.

**Trazabilidad de alcance**: El usuario autorizó reutilizar SPEC-008 tras dividir el alcance anterior. Sus historias, decisiones, fases y gates no se trasladan a esta reformulación. Antecedente en [índice Trello](../trello.md). Esta feature prepara la identificación necesaria para una futura extracción estadística; no implementa esa extracción ni completa por sí sola REQ-S2-02.

## Clarifications

### Session 2026-10-14

- Q: Si un jugador local coincide por nombre y equipo con dos identificadores WhoScored distintos encontrados en ligas diferentes, ¿debe quedar sin asociar aunque una de esas ligas se haya procesado primero? → A: Sí. Comparar globalmente los candidatos de todas las ligas descubiertas correctamente antes de confirmar referencias; dos identificadores que cumplen nombre y equipo implican ambigüedad, sin asociación.
- Q: Si el nombre o el equipo de un jugador local cambia mientras se consulta WhoScored, ¿qué debe hacer la ejecución antes de guardar su referencia? → A: Usar los datos locales capturados al inicio y guardar la referencia aunque nombre o equipo hayan cambiado durante la ejecución, manteniendo las reglas de unicidad y protección de referencias confirmadas.
- Q: ¿Qué superficie HTTP expone esta feature si el objetivo es únicamente sincronizar y persistir los identificadores de WhoScored? → A: Exponer solo los dos endpoints de ejecución —pase actual y pase histórico—. La revisión de resultados y evidencia mediante API queda fuera de esta feature; la auditoría se conserva en persistencia y el resumen de la ejecución se devuelve en la respuesta de las propias operaciones de ejecución. Por ello se retiran la User Story 3 y el requisito de consulta.
- Q: Si una de las cinco ligas falla su descubrimiento pero todas las demás se completan y todas las coincidencias posibles se confirman, ¿debe el resumen declarar PARTIAL o COMPLETED? → A: PARTIAL. El estado global refleja tanto el resultado de los jugadores como la completitud del descubrimiento; un fallo de liga impide declararlo completo aunque no quede ningún pendiente. El operador identifica la liga en `leagues[]` y reintenta.
- Q: Un jugador que termina como NO_MATCH o AMBIGUOUS, ¿impide que la ejecución se declare COMPLETED? → A: No. Son resultados válidos de un recorrido terminado, no trabajo a medias: los pendientes se informan en los contadores por resultado. COMPLETED significa que el trabajo previsto se completó, no que todos los jugadores fueran identificados.
- Q: Si el operador relanza el pase actual tras una desconexión, ¿la segunda ejecución debe ser una ejecución nueva con su propio identificador o debe retomar la anterior? → A: Siempre una ejecución nueva con identificador propio. Cada invocación de POST registra su propia auditoría; la idempotencia de las referencias, y no la reutilización de la ejecución, es lo que garantiza que relanzar sea seguro. Así el histórico conserva un pase actual de origen explícito y el operador puede distinguir en la persistencia cuántos intentos se hicieron.

## User Scenarios & Testing *(mandatory)*

### Decisiones de revisión de checklist — 2026-10-14

Confirmadas por el usuario para ambos pases: datos locales inválidos generan error
propio y no son pendientes históricos; filas externas inválidas se descartan
individualmente y se auditan; una referencia WhoScored existente jamás se reemplaza
ni se acompaña de otro ID; inconsistencias preexistentes generan CONFLICT. Se
precisaron normalización, contextos de identidad y colisiones de nombres de equipo.
La tabla actual fue validada por el usuario para los equipos persistidos del TP y
permanece fija, sin versionado dinámico. Ningún umbral de cobertura condiciona el
éxito. Requisitos y escenarios siguientes incorporan estas decisiones.

### User Story 1 - Identificar jugadores desde el catálogo actual (Priority: P1)

Como operador del catálogo, quiero iniciar manualmente la identificación en WhoScored de los jugadores locales para poder consultar ese proveedor en una feature futura sin confundir sus identificadores con los de otras fuentes.

**Why this priority**: Entrega la referencia externa reutilizable que hoy falta y protege la identidad local ya establecida.

**Independent Test**: Con un catálogo local y respuestas controladas del proveedor que incluyan nombres únicos, duplicados, homónimos y conflictos, iniciar el pase actual y comprobar referencias, pendientes y conservación del catálogo original.

**Acceptance Scenarios**:

1. **Given** jugadores locales sin referencia y cinco ligas configuradas, **When** el operador inicia el pase actual, **Then** se recorren todas las páginas válidas de cada liga y cada identificador externo se considera una sola vez aunque aparezca repetido.
2. **Given** un jugador y un equipo cuyos nombres difieren solo por diacríticos, mayúsculas, espacios, guiones o una equivalencia explícita del catálogo normativo, **When** ambas identidades normalizadas coinciden con un único candidato, **Then** se registra su referencia WhoScored.
3. **Given** uno o varios candidatos con el mismo nombre normalizado, **When** ninguno coincide con el equipo local mediante normalización o correspondencia explícita, **Then** se registra falta de coincidencia sin asociar por nombre solamente; solo varios IDs que cumplen nombre y equipo generan ambigüedad.
4. **Given** un identificador WhoScored asociado a otro jugador local, **When** aparece una nueva coincidencia con ese identificador, **Then** se registra el conflicto y la asociación existente permanece intacta.
5. **Given** jugadores ya referenciados o sin coincidencia única, **When** finaliza el pase, **Then** los primeros conservan sus referencias y los segundos quedan pendientes sin impedir la resolución del resto.
6. **Given** una liga cuya respuesta es bloqueada, no exitosa, no JSON o de estructura inválida, **When** ocurre el error en cualquiera de sus páginas, **Then** no se crean referencias a partir del descubrimiento incompleto de esa liga, se registra el error y pueden confirmarse las demás ligas válidas.
7. **Given** un error al confirmar la referencia de un jugador, **When** continúa la ejecución, **Then** el jugador no queda parcialmente asociado, su error es auditable y los resultados ya confirmados de otros jugadores permanecen válidos.
8. **Given** una ejecución terminada, **When** se repite con los mismos datos, **Then** no se duplican referencias ni se reemplazan asociaciones confirmadas.
9. **Given** dos identificadores WhoScored distintos encontrados en ligas válidas diferentes que coinciden con el nombre y equipo de un jugador local, **When** se evalúa su identidad, **Then** queda sin referencia y se registra ambigüedad, independientemente del orden de recorrido de las ligas.
10. **Given** una coincidencia única con el nombre y equipo locales capturados al inicio, **When** esos atributos cambian durante la ejecución, **Then** se confirma la referencia usando los datos iniciales, siempre que no exista un conflicto de referencias, y la auditoría conserva los datos utilizados para decidir.
11. **Given** un jugador sin nombre/equipo válido, **When** se evalúa el snapshot, **Then** se registra error de datos locales con motivo, sin referencia ni NO_MATCH, y se continúa con los demás; el histórico no lo selecciona.
12. **Given** una página estructuralmente válida con una fila de `playerId` inválido o nombre de jugador/equipo vacío, **When** se adapta, **Then** solo esa fila se descarta y audita; las demás filas siguen utilizables si la liga completa su recorrido.
13. **Given** un candidato con nombre de equipo válido sin `teamId`, **When** coinciden nombre de jugador y equipo normalizados o correspondencia explícita, **Then** puede asociarse sin inventar un ID de equipo.
14. **Given** varias referencias WhoScored distintas preexistentes de un jugador, **When** comienza el pase, **Then** registra CONFLICT, conserva todas sin rematching y continúa con los demás.
15. **Given** un ID externo con variantes de nombre/equipo, **When** un contexto completo coincide con un jugador, **Then** puede resolverlo; si coincide con varios jugadores locales, registra CONFLICT para los involucrados sin referencia nueva.
16. **Given** una tabla con un nombre WhoScored normalizado relacionado con distintos nombres locales normalizados, **When** se valida antes del pase, **Then** informa error de configuración sin solicitudes externas ni asociaciones.

---

### User Story 2 - Resolver pendientes mediante el catálogo histórico (Priority: P2)

Como operador, quiero iniciar por separado un pase histórico sobre los jugadores pendientes del pase actual para ampliar la cobertura sin revisar otra vez las identidades confirmadas.

**Why this priority**: El catálogo actual tiene cobertura parcial aceptada; el histórico puede identificar jugadores ausentes de las plantillas actuales.

**Independent Test**: Partir de una ejecución actual registrada con pendientes y referencias confirmadas; ejecutar el pase histórico con candidatos adicionales y comprobar que únicamente los pendientes elegibles puedan recibir referencias nuevas.

**Acceptance Scenarios**:

1. **Given** un pase actual registrado con pendientes sin coincidencia o ambiguos, **When** se solicita el pase histórico vinculado a ese pase, **Then** solo se intenta resolver su conjunto pendiente que siga sin referencia al iniciar el pase complementario.
2. **Given** un pendiente con coincidencia única en el histórico, **When** se aplican las mismas reglas de identidad y conflicto, **Then** recibe su referencia y deja de estar pendiente para nuevas resoluciones, conservándose la evidencia de la ejecución anterior.
3. **Given** un jugador ya resuelto, incluso si se resolvió después del pase actual, **When** se inicia el complementario, **Then** no se reevalúa ni se modifica su referencia.
4. **Given** varias filas históricas del mismo identificador con distintos equipos, **When** se consolidan los candidatos, **Then** no se convierten en jugadores distintos y se conserva el contexto observado necesario para desambiguación y revisión.
5. **Given** un histórico que no elimina la ambigüedad, contiene un conflicto o falla para una liga, **When** se ejecuta el pase, **Then** se conserva el pendiente o conflicto correspondiente y las asociaciones confirmadas permanecen intactas.
6. **Given** que no existe un pase actual elegible o no quedan pendientes, **When** se solicita el complementario, **Then** se informa respectivamente la ausencia del prerrequisito o un resultado sin jugadores por resolver; no se realiza una identificación general del catálogo.

---

### Edge Cases

- Catálogo local o liga sin jugadores: resultado sin asociaciones, sin crear jugadores locales nuevos.
- Primera página vacía válida: liga sin candidatos; una respuesta de bloqueo no se interpreta como página vacía.
- Página intermedia inválida: la liga no se considera descubierta por completo y no genera asociaciones con datos parciales.
- Proveedor que repite páginas sin progreso o contradice su paginación: error auditable, sin recorrido infinito ni confirmación de esa liga.
- Nombre de jugador/equipo externo ausente o inválido o identificador de jugador inválido: descartar y auditar solo esa fila, sin invalidar la liga. Un fallo estructural de página/paginación sigue invalidando la liga completa.
- Nombre/equipo local nulo, vacío tras normalización o sin letras ni dígitos (solo signos): error de datos locales, sin asociación ni elegibilidad histórica; no borrar puntuación para forzar coincidencias.
- Nombre coincidente cuyo equipo no puede relacionarse inequívocamente: falta de coincidencia o ambigüedad, sin asociar por nombre solamente ni inferir equivalencias por edad, posición o región.
- Dos identificadores externos distintos que señalan al mismo jugador local: ambigüedad/conflicto, sin registrar ambos como identidades confirmadas.
- Referencia local existente distinta de la candidata: se preserva y se registra el conflicto; no se reemplaza.
- Transferencias y equipos históricos: cada candidato conserva todos sus contextos observados, pero solo puede asociarse cuando al menos uno corresponde inequívocamente al equipo local mediante normalización o alias explícito.
- Ejecuciones repetidas o concurrentes: ninguna puede duplicar una referencia ni apropiarse de un identificador ya asociado.
- Cambios de nombre o equipo local durante la ejecución: la resolución utiliza los datos capturados al inicio y puede confirmar la referencia sin revalidar esos atributos; la protección de referencias confirmadas y la unicidad siguen siendo obligatorias.
- Interrupción de una ejecución: lo confirmado permanece; el ámbito sin descubrimiento completo no produce asociaciones ni se presenta como ausencia de coincidencias.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir iniciar manualmente y de forma independiente un pase actual y un pase histórico complementario para las cinco ligas configuradas: Premier League, Bundesliga, LaLiga, Serie A y Ligue 1.
- **FR-002**: El pase actual MUST descubrir el catálogo de plantilla actual de WhoScored, recorriendo su paginación completa por competición antes de confirmar asociaciones de esa liga. La última página MUST determinarse con los metadatos `currentPage` y `totalPages`; una página con menos filas que el máximo configurado MUST NOT considerarse final por ese solo hecho.
- **FR-003**: El descubrimiento MUST deduplicar por identificador WhoScored en toda la ejecución y conservar los contextos de equipo y competición relevantes de las filas repetidas, sin duplicar asociaciones ni conteos de descubiertos.
- **FR-004**: El sistema MUST identificar únicamente jugadores ya persistidos del catálogo local; no crear jugadores, cambiar su identidad interna ni modificar sus atributos de catálogo a partir de WhoScored.
- **FR-005**: El sistema MUST comparar nombres de jugadores y equipos normalizados eliminando diacríticos, unificando mayúsculas/minúsculas y convirtiendo secuencias de espacios y guiones en un único espacio, sin espacios en los extremos. No se incluyen coincidencias aproximadas, abreviaciones inferidas ni transliteraciones adicionales.

  Regla reproducible: descomponer con Unicode NFD; eliminar marcas Mn/Mc/Me únicamente asociadas a una letra base del script LATIN, conservándolas en escrituras no latinas; convertir a minúsculas sin depender del locale del entorno. Sustituir caracteres Unicode de categoría Pd (incluidos `-`, `‐`, `‑`, `–`, `—`) y espacios Unicode White_Space (incluidas tabulaciones/saltos de línea) por U+0020, colapsar secuencias mixtas a un espacio y quitar extremos. Conservar otra puntuación, como apóstrofes y puntos. No convertir `ø` en `o` ni `ß` en `ss`. Aplicar el mismo pipeline a nombres de ambos proveedores y a la tabla.
- **FR-006**: Una identificación MUST requerir simultáneamente la coincidencia del nombre normalizado del jugador y la coincidencia de su equipo mediante nombre normalizado o una equivalencia explícita del catálogo normativo de alias. Un nombre de jugador único MUST NOT bastar por sí solo para registrar la referencia.
- **FR-007**: El sistema MUST evaluar globalmente los candidatos de todas las ligas descubiertas correctamente en la ejecución antes de confirmar referencias, sin limitar la comparación a la liga vigente del jugador local. MUST confirmar solo una correspondencia inequívoca que cumpla FR-006. Si ningún candidato coincide también por equipo, MUST registrar falta de coincidencia; si varios identificadores distintos la cumplen, incluso en ligas diferentes, MUST registrar ambigüedad sin asociación. MUST NOT seleccionar por orden de recorrido, edad, posición o región ni inferir alias ausentes del catálogo normativo.
- **FR-008**: Cada asociación MUST reutilizar el mecanismo de referencias externas del jugador, distinguiendo WhoScored de Football-Data y TheSportsDB y preservando las referencias de esos proveedores.
- **FR-009**: El sistema MUST preservar todas las referencias WhoScored confirmadas: un identificador externo no puede quedar asociado a jugadores locales diferentes y un jugador no puede adquirir identidades WhoScored contradictorias. Los conflictos MUST quedar registrados para revisión sin sobrescritura.

  Como máximo una referencia WhoScored por jugador, incluso ante concurrencia. Una existente MUST conservarse sin rematching ni insertar otro ID. Varias distintas preexistentes MUST generar CONFLICT sin borrado/elección y excluir al jugador del matching y del histórico.
- **FR-010**: Los jugadores evaluados sin coincidencia única MUST registrarse como pendientes por falta de coincidencia o por ambigüedad. Un fallo de descubrimiento MUST distinguirse de ausencia de coincidencias; conflictos y errores MUST conservar motivos propios.
- **FR-011**: Los casos pendientes y conflictos MUST conservar la evidencia disponible de candidatos: identificador WhoScored, nombre devuelto, equipo e identificador externo de equipo, competición e identificador de liga. Cuando no haya candidato, MUST conservar el jugador local y el ámbito consultado, sin inventar datos externos.
- **FR-012**: El pase histórico MUST recorrer el catálogo histórico y resolver únicamente los jugadores pendientes del pase actual de origen que continúen sin referencia. MUST excluir resueltos y conflictos registrados como tales; no reevaluar ni modificar identidades confirmadas. Si no queda ningún jugador elegible, el pase histórico MUST registrar una ejecución vacía completada sin solicitudes al proveedor, y sus ámbitos MUST quedar en estado `SKIPPED` sin descubrir.
- **FR-013**: El pase histórico MUST aplicar las mismas reglas de normalización, desambiguación, unicidad y protección de referencias, conservando los contextos históricos de cada identificador deduplicado.
- **FR-014**: Cada ejecución MUST ser reentrante e idempotente respecto de las referencias: repetirla no produce duplicados ni altera asociaciones confirmadas, incluso ante concurrencia.
- **FR-015**: Respuestas de bloqueo, estados no exitosos, cuerpos no interpretables como JSON y estructuras sin el listado o los metadatos de paginación esperados MUST registrarse como errores de integración. El `Content-Type` no determina por sí solo la validez porque el proveedor puede entregar un cuerpo JSON válido como `text/html`; la estructura parseada MUST contener `playerTableStats` y `paging` coherente. Ningún descubrimiento incompleto o inválido de una liga puede generar asociaciones; la auditoría del error sí debe conservarse.
- **FR-016**: Un fallo de una liga o al confirmar un jugador MUST NOT invalidar resultados ya confirmados de los demás. Cada asociación fallida MUST quedar sin cambios parciales y con motivo auditable; la ejecución MUST distinguir éxito, resultado parcial y fallo. El estado global MUST declarar PARTIAL si alguna liga no completó su descubrimiento, aunque todas las coincidencias alcanzables se hayan confirmado; solo es COMPLETED cuando todas las ligas configuradas completaron su recorrido y cada jugador evaluado obtuvo un resultado final distinto de error de confirmación; una ejecución cuyo trabajo previsto estaba vacío, sin jugadores seleccionados, MUST poder declararse COMPLETED sin descubrimiento realizado.
- **FR-017**: El sistema MUST regular el ritmo de solicitudes con una configuración explícita y respetar la indisponibilidad del proveedor, sin reintentos por defecto. MUST impedir recorridos sin progreso y mantener identificable el ámbito incompleto. Si `totalPages` falta, es inválido o contradice `currentPage`, `totalResults`, `resultsPerPage` o los índices informados, el recorrido MUST fallar como respuesta inválida; MUST NOT inferir el final por una página corta o vacía fuera de los límites declarados.
- **FR-018**: El descubrimiento y la resolución de coincidencias MUST ocurrir fuera de una transacción de persistencia; MUST NOT mantenerse una transacción abierta durante llamadas de red.
- **FR-019**: Cada ejecución MUST registrar tipo de pase, ámbito, vínculo con el pase actual cuando aplique, inicio, fin y resultado, además de conteos de descubiertos únicos, coincidencias resueltas, ya referenciados, conflictos, pendientes sin coincidencia, pendientes ambiguos y errores del proveedor. Los errores al confirmar jugadores MUST ser distinguibles adicionalmente.

  MUST distinguir además jugadores con error de datos locales y filas externas descartadas. Los primeros cuentan una vez como resultado final local; las segundas cuentan filas, no jugadores ni ligas fallidas. Errores de datos locales resueltos como decisión final y descartes individuales auditados no implican por sí solos trabajo interrumpido ni degradan COMPLETED: el estado sigue las condiciones de finalización de FR-016/021, no la cobertura.

  Reconciliación, conforme al modelo de datos: cada jugador MUST contarse una sola vez, en un único resultado final local, y los resultados MUST ser excluyentes. Al cierre, `selectedPlayers` MUST igualar la suma de resultados resueltos, ya referenciados, en conflicto, sin coincidencia, ambiguos, con error de datos locales, con error de descubrimiento, con error de persistencia e interrumpidos. `pendingPlayers` es un contador derivado que solo reconcilia mientras la ejecución no está terminada y MUST NOT contabilizarse como un resultado adicional. Los errores de descubrimiento, persistencia e interrupción se contabilizan por item sin computarse como jugadores sin coincidencia, y los errores del proveedor describen los ámbitos fallidos: `providerErrors` cuenta esos ámbitos y MUST NOT contabilizarse como jugadores. Las filas descartadas se contabilizan aparte y MUST NOT sumarse a `selectedPlayers`. Las unidades de cada conteo se declaran en FR-020, que fija también la reconciliación sin doble contabilización.
- **FR-020**: El resumen MUST declarar la unidad y ámbito de cada conteo: descubiertos son identificadores externos únicos; resultados locales cuentan jugadores una sola vez en su resultado final; errores del proveedor describen fallos de descubrimiento y no se suman como jugadores sin coincidencia. La reconciliación de cierre se define en FR-019. Detalles y resumen MUST reconciliarse sin doble contabilización.
- **FR-021**: Cada operación de ejecución MUST responder con el resumen de la ejecución que ha provocado —pase, inicio, fin, resultado, conteos y estado por liga—, de modo que el operador MAY obtenerlo sin una operación de consulta adicional. El resumen MUST corresponder a una ejecución terminada; el estado no registrado `RUNNING` es interno y MUST NOT aparecer en la respuesta de una operación de ejecución. El estado MUST describir el grado de finalización del trabajo y no el porcentaje de jugadores identificados, conforme a FR-016. La evidencia por jugador MUST quedar persistida para trazabilidad interna, pero esta feature MUST NOT exponer una interfaz de consulta ni permitir editar, reasignar o aprobar asociaciones.
- **FR-022**: Las operaciones de ejecución MUST respetar la autenticación y autorización vigentes para las sincronizaciones del catálogo. Credenciales y contenido sensible MUST NOT aparecer en logs, respuestas ni errores.
- **FR-023**: Los endpoints de ejecución MUST cumplir el [contrato HTTP oficial](../../docs/backend/architecture.md#17-contrato-http-del-backend): prefijo `/api/`, datos exitosos mediante DTO de respuesta y errores controlados mediante `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`. El código de error MUST ser estable; OpenAPI MUST documentar respuestas, seguridad, parámetros y errores según las [convenciones oficiales](../../docs/backend/conventions.md#documentación-de-endpoints-y-javadoc). Las rutas y DTO específicos se diseñarán en Plan.
- **FR-024**: Esta feature MUST NOT persistir ni exponer estadísticas de rendimiento o cotizaciones, modificar el flujo Football-Data/TheSportsDB ni introducir ejecución periódica programada o una interfaz de corrección manual de asociaciones.
- **FR-025**: Cada pase MUST resolver coincidencias utilizando el nombre y equipo locales capturados al inicio de la ejecución. Un cambio posterior de esos atributos MUST NOT impedir por sí solo confirmar una coincidencia única ni provocar su reevaluación con los atributos nuevos. La auditoría MUST conservar los datos locales utilizados para la decisión; al confirmar la referencia MUST seguir respetándose la unicidad y la protección de asociaciones existentes de FR-009 y FR-014.
- **FR-026**: Las operaciones de ejecución MUST ser reintentables por el operador, aplicando la idempotencia de referencias de FR-014: una ejecución interrumpida por desconexión o fallo de transporte MAY relanzarse sin necesidad de comprobar antes su resultado. Cada invocación MUST registrar una ejecución nueva con identificador propio y MUST NOT depender de reutilizar una ejecución previa para su seguridad.
- **FR-027**: Un jugador local sin nombre/equipo válido (nulo, vacío tras FR-005 o sin letras ni dígitos) MUST recibir error de datos locales con motivo específico, sin referencia ni NO_MATCH; MUST NOT ser elegible para el histórico de ese origen. Corregir los datos permite evaluarlo en un nuevo pase actual. El procesamiento de los demás continúa.
- **FR-028**: `playerId` MUST ser entero positivo, numérico o cadena de dígitos decimales; ceros iniciales MUST canonicalizarse. Ausente, vacío, cero, negativo, decimal o no numérico MUST descartar solo la fila con motivo auditable. Nombre de jugador/equipo externo inválido según FR-027 MUST tener igual tratamiento. `teamId` ausente MUST NOT impedir matching por nombre válido. Un equipo fuera de la tabla MAY coincidir directamente por normalización, sin inferir equivalencias. Descartes MUST NOT invalidar por sí solos la liga ni confundirse con errores de página/paginación; MUST conservar ejecución, liga, página, posición y motivo, sin cuerpos crudos.
- **FR-029**: Un ID WhoScored con nombres distintos MUST conservar sus variantes como contextos de una identidad. Solo un contexto que cumpla simultáneamente nombre y equipo habilita asociación. Si un ID cumple para varios jugadores locales distintos sin referencia, MUST registrar CONFLICT para todos antes de escribir, sin elegir propietario por orden.

### Catálogo normativo de alias de equipos

Las siguientes correspondencias forman parte de las reglas funcionales de identidad. El nombre local es el persistido actualmente desde Football-Data y el nombre WhoScored es el observado para la misma entidad externa. Una equivalencia no listada MUST NOT inferirse ni agregarse silenciosamente durante una ejecución.

La tabla es fija para todo el alcance del TP, sin cambios en runtime ni entre pases
y sin versionado dinámico. Su referencia única es «SPEC-008 — Catálogo normativo
de alias de equipos»; Plan, tareas y tests MUST referenciar esta sección. Validada
por el usuario el 2026-10-14 para equipos actualmente persistidos de las cinco
ligas, no para plantillas completas de una temporada externa. Se compara por
nombre normalizado; la tabla explicita equivalencias de nombres distintos. Un
nombre WhoScored normalizado MUST NOT corresponder a distintos nombres locales
normalizados: detectar esa colisión antes del pase y devolver error de
configuración, sin red ni asociaciones. Varios nombres externos para un mismo
nombre local están permitidos si no hay esa colisión.

#### Premier League

| Equipo local | Equipo WhoScored |
| --- | --- |
| Arsenal FC | Arsenal |
| Aston Villa FC | Aston Villa |
| Chelsea FC | Chelsea |
| Everton FC | Everton |
| Fulham FC | Fulham |
| Liverpool FC | Liverpool |
| Manchester City FC | Man City |
| Manchester United FC | Man Utd |
| Newcastle United FC | Newcastle |
| Sunderland AFC | Sunderland |
| Tottenham Hotspur FC | Tottenham |
| Hull City AFC | Hull |
| Leeds United FC | Leeds |
| Ipswich Town FC | Ipswich |
| Nottingham Forest FC | Nottingham Forest |
| Crystal Palace FC | Crystal Palace |
| Brighton & Hove Albion FC | Brighton |
| Brentford FC | Brentford |
| AFC Bournemouth | Bournemouth |
| Coventry City FC | Coventry |

#### Bundesliga

| Equipo local | Equipo WhoScored |
| --- | --- |
| 1. FC Köln | FC Koln |
| TSG 1899 Hoffenheim | Hoffenheim |
| Bayer 04 Leverkusen | Leverkusen |
| Borussia Dortmund | Borussia Dortmund |
| FC Bayern München | Bayern |
| FC Schalke 04 | Schalke |
| Hamburger SV | Hamburg |
| VfB Stuttgart | Stuttgart |
| SV Werder Bremen | Werder Bremen |
| 1. FSV Mainz 05 | Mainz |
| FC Augsburg | Augsburg |
| SC Freiburg | Freiburg |
| Borussia Mönchengladbach | Borussia M.Gladbach |
| Eintracht Frankfurt | Eintracht Frankfurt |
| 1. FC Union Berlin | Union Berlin |
| SC Paderborn 07 | Paderborn |
| SV 07 Elversberg | Elversberg |
| RB Leipzig | RBL |

#### LaLiga

| Equipo local | Equipo WhoScored |
| --- | --- |
| Athletic Club | Athletic Club |
| Club Atlético de Madrid | Atletico |
| CA Osasuna | Osasuna |
| RCD Espanyol de Barcelona | Espanyol |
| FC Barcelona | Barcelona |
| Getafe CF | Getafe |
| Málaga CF | Malaga |
| Real Madrid CF | Real Madrid |
| Rayo Vallecano de Madrid | Rayo Vallecano |
| Levante UD | Levante |
| Real Betis Balompié | Real Betis |
| Real Sociedad de Fútbol | Real Sociedad |
| Villarreal CF | Villarreal |
| Valencia CF | Valencia |
| Deportivo Alavés | Deportivo Alaves |
| Elche CF | Elche |
| RC Celta de Vigo | Celta Vigo |
| Sevilla FC | Sevilla |
| RC Deportivo La Coruña | Deportivo |
| Real Racing Club de Santander | Racing Santander |

#### Serie A

| Equipo local | Equipo WhoScored |
| --- | --- |
| AC Milan | AC Milan |
| ACF Fiorentina | Fiorentina |
| AS Roma | Roma |
| Atalanta BC | Atalanta |
| Bologna FC 1909 | Bologna |
| Cagliari Calcio | Cagliari |
| Genoa CFC | Genoa |
| FC Internazionale Milano | Inter |
| Juventus FC | Juventus |
| SS Lazio | Lazio |
| Parma Calcio 1913 | Parma Calcio 1913 |
| SSC Napoli | Napoli |
| Udinese Calcio | Udinese |
| Venezia FC | Venezia |
| Frosinone Calcio | Frosinone |
| US Sassuolo Calcio | Sassuolo |
| Torino FC | Torino |
| US Lecce | Lecce |
| AC Monza | Monza |
| Como 1907 | Como |

#### Ligue 1

| Equipo local | Equipo WhoScored |
| --- | --- |
| Toulouse FC | Toulouse |
| Stade Brestois 29 | Brest |
| Olympique de Marseille | Marseille |
| AJ Auxerre | Auxerre |
| Lille OSC | Lille |
| OGC Nice | Nice |
| Olympique Lyonnais | Lyon |
| Paris Saint-Germain FC | PSG |
| FC Lorient | Lorient |
| Stade Rennais FC 1901 | Rennes |
| ES Troyes AC | Troyes |
| Angers SCO | Angers |
| Le Havre AC | Le Havre |
| Le Mans FC | Le Mans |
| Racing Club de Lens | Lens |
| AS Monaco FC | Monaco |
| RC Strasbourg Alsace | Strasbourg |
| Paris FC | Paris FC |

### Key Entities *(include if feature involves data)*

- **Jugador local**: Identidad interna persistida, nombre y relación vigente con equipo/liga; destinatario de una referencia nueva, sin reemplazo de sus atributos.
- **Referencia externa de jugador**: Asociación entre jugador local, proveedor e identificador externo, con unicidad y protección de la identidad confirmada.
- **Candidato WhoScored**: Identificador y nombre externo, con contextos observados de equipo y competición; las filas repetidas representan un mismo candidato, no nuevas identidades.
- **Ejecución de identificación**: Pase actual o histórico, ámbito de ligas, ejecución de origen, tiempos, resultado y conteos auditables.
- **Detalle de resolución**: Jugador local cuando corresponda, nombre y equipo locales capturados al inicio y utilizados para decidir, candidatos, decisión, motivo y evidencia de la ejecución. Entidad de auditoría interna: se persiste para trazabilidad y para el pase histórico, y no se expone mediante API en esta feature.
- **Caso de revisión**: Pendiente sin coincidencia, ambiguo o conflicto, vinculado al jugador y a su evidencia. Se contabiliza en los contadores del resumen de la ejecución; su examen detallado y cualquier modificación manual quedan fuera de esta feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un conjunto controlado de las cinco ligas, se identifica el 100% de las coincidencias únicas válidas por nombre de jugador y equipo, incluidas las denominaciones distintas cubiertas por el catálogo normativo, y se confirman cero coincidencias por nombre solamente o con equipo ambiguo.
- **SC-002**: En cada ejecución terminada, los contadores permiten calcular la cobertura real como `(resolved + alreadyReferenced) / selectedPlayers`, con denominador cero informado como no aplicable, y reconciliar todos los resultados locales. Los IDs externos descubiertos se informan por separado. El éxito MUST NOT depender de ningún umbral de porcentaje; el 83% es solo contexto histórico, no cobertura asociada acreditada ni criterio de aceptación.
- **SC-003**: Repetir un pase sobre los mismos datos genera cero referencias duplicadas, cero sustituciones de asociaciones confirmadas y cero modificaciones de referencias Football-Data/TheSportsDB.
- **SC-004**: El pase histórico modifica cero jugadores ya resueltos y el 100% de sus nuevas asociaciones corresponde a pendientes elegibles del pase actual vinculado.
- **SC-005**: El 100% de los jugadores procesados y de los errores de descubrimiento puede rastrearse a una ejecución y un motivo en la auditoría persistida, y los contadores devueltos en cada operación de ejecución se reconcilian con los detalles registrados sin doble contabilización.
- **SC-006**: Ante un fallo en una de las cinco ligas, se conservan el 100% de los resultados ya confirmados y se generan cero asociaciones desde su descubrimiento incompleto o inválido.
- **SC-007**: El resumen devuelto por cada operación de ejecución permite distinguir en el 100% de los casos ausencia de coincidencia, ambigüedad, conflicto y error de proveedor mediante los contadores por resultado y el estado por liga, sin necesidad de repetir el descubrimiento.
- **SC-008**: Ambas ejecuciones producen cero cotizaciones y cero registros o respuestas de estadísticas de rendimiento.

## Assumptions

- El catálogo de entrada es el catálogo local vigente de las cinco ligas, ya persistido por Football-Data y enriquecido por TheSportsDB. SPEC-002 y SPEC-007 son dependencias funcionales, no aprobaciones acreditadas aquí.
- Las ejecuciones se realizan por operadores con los permisos vigentes de sincronización del catálogo; no se introduce un nuevo rol ni acceso público.
- El pase histórico se vincula a una ejecución actual explícita y registrada, incluso si fue parcial. Solo toma sus pendientes genuinos por falta de coincidencia o ambigüedad; errores de descubrimiento no acreditan esos estados. El usuario puede volver a ejecutar el pase actual tras resolver indisponibilidad.
- La identificación utiliza nombres de equipos normalizados y el catálogo normativo explícito de esta spec. No se infieren equivalencias nuevas entre clubes; si el equipo no coincide por normalización o alias, el nombre del jugador no basta para asociar y el caso permanece pendiente.
- La unidad de protección frente a un fallo de descubrimiento es la liga completa: se valida su recorrido y se excluyen sus candidatos si falla. Tras terminar el descubrimiento de todas las ligas, la resolución compara globalmente los candidatos de las ligas válidas antes de confirmar referencias. Fallos al confirmar jugadores se aíslan por jugador. Esto concilia no escribir asociaciones desde datos parciales con conservar el trabajo de otras ligas y evita que el orden de recorrido resuelva ambigüedades.
- Esta feature no expone una interfaz de consulta de ejecuciones, jugadores ni evidencia. El resumen y los contadores de cada ejecución se obtienen en la respuesta de la propia operación que la produce; el examen detallado de casos pendientes y de la evidencia por jugador, así como editar, reasignar o aprobar asociaciones manualmente, quedan fuera de alcance. En consecuencia, relanzar una ejecución interrumpida es seguro por FR-026 y no requiere una consulta previa para decidir si procede repetirla.
- La estimación del 83% surge de una observación de referencia de 2165 identificadores actuales frente a aproximadamente 2600 jugadores locales. Depende de la composición y fecha de ambos catálogos; no constituye una garantía de cobertura ni fija una duración total sin evidencia de disponibilidad y límites del proveedor.
- Los siguientes hechos de integración fueron comprobados durante la investigación de la feature: endpoint interno `GET https://es.whoscored.com/statisticsfeed/1/getplayerstatistics`; `playerId` vacío entrega catálogo paginado; `page` comienza en 1 y 0 devuelve vacío; `tournamentOptions` filtra competición, mientras `tournamentId` y `stageId` se ignoran; `numberOfPlayersToPick=100` entrega hasta 100 filas por página; `isCurrent=true` entrega plantilla actual y `false` el histórico.
- Correspondencias de competición comprobadas: Premier League `2`, Bundesliga `3`, LaLiga `4`, Serie A `5`, Ligue 1 `22`. Se conservará la distinción entre identificador de competición del proveedor e identidad de liga local.
- Datos de identidad disponibles según el usuario: `playerId`, `name`, `firstName`, `lastName`, `teamId`, `teamName`, `positionText`, `age`, `regionCode`. No hay fecha de nacimiento ni nacionalidad; no se inventarán ni se exigirán para identificar. El histórico supera 5900 jugadores y puede repetir equipos anteriores para un mismo jugador.
- Los nombres locales fueron aportados por el usuario desde el estado persistido y los externos contrastados durante la investigación. El usuario validó la tabla el 2026-10-14 para los equipos persistidos del TP; permanece fija durante ese alcance, sin revalidación por pase ni actualización/versionado dinámico.
- El contrato observado de solicitud, headers, cuerpos actual/histórico y cierre de paginación se conserva en [evidencia del catálogo WhoScored](evidence/whoscored-catalog-contract.md). Son muestras fechadas y pueden variar con el proveedor; las reglas funcionales de validación permanecen vigentes.
- Dependencias y fuentes: [SPEC-002](../002-player-catalog/spec.md), [SPEC-007](../007-team-league-domain/spec.md), [enunciado §1.1, §3.1, §6 y §8](../../tp/enunciado.md) y [entregas Sprint 2](../../tp/entregas.md). Esta preparación no dispensa los requisitos académicos futuros de estadísticas, scraping, caché, scheduler o cotizaciones, ni selecciona esta feature como la adicional exigida por la entrega.

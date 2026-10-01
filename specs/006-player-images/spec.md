# Feature Specification: Imágenes de jugadores

**Feature Branch**: `006-player-images`

**Created**: 2026-09-30

**Status**: Ready for planning

**Input**: Incorporar retratos de jugadores de TheSportsDB al catálogo existente, respetando el límite gratuito de peticiones y mostrando la imagen genérica cuando no haya una foto disponible. Uso local por el momento.

## Clarifications

### Session 2026-09-30

- Q: ¿Cuándo querés que empiece o se reanude la búsqueda de fotos pendientes? → A: Solo cuando un usuario inicia manualmente la búsqueda mediante un endpoint de sincronización de imágenes.
- Q: Si alguien vuelve a llamar al endpoint mientras todavía se están buscando fotos, ¿qué debería pasar? → A: Rechazar la segunda solicitud indicando que ya hay una sincronización de imágenes en curso.
- Q: Como el endpoint responde antes de que termine la búsqueda, ¿cómo querés saber cuándo finalizó y cuántas fotos encontró? → A: Esperar a que termine la sincronización manual de imágenes y devolver las cantidades en la respuesta de esa solicitud.
- Q: Si TheSportsDB falla después de que ya se guardaron algunas fotos, ¿qué resultado debería devolver la sincronización de imágenes? → A: Devolver un error de ejecución incompleta y conservar las fotos ya guardadas para continuar en la próxima invocación.
- Q: ¿Cuándo consideramos que un resultado de TheSportsDB corresponde al jugador correcto para asignarle la foto? → A: Solo cuando coinciden nombre y equipo; si hay duda, no se asigna foto.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconocer al jugador por su imagen (Priority: P1)

Como usuario autenticado, quiero ver el retrato del jugador en su tarjeta cuando esté disponible para identificarlo visualmente sin perder la información existente.

**Why this priority**: Es el objetivo principal de la feature.

**Independent Test**: Consultar una página del catálogo con jugadores con y sin retrato registrado y comprobar que cada tarjeta muestra la foto correspondiente o el ícono genérico.

**Acceptance Scenarios**:

1. **Given** un jugador activo con un retrato asociado, **When** se muestra su tarjeta, **Then** aparece ese retrato en lugar del ícono genérico y el nombre sigue visible.
2. **Given** un jugador activo sin retrato asociado, **When** se muestra su tarjeta, **Then** aparece el ícono genérico actual.
3. **Given** un retrato asociado cuya imagen no puede cargarse, **When** falla su visualización, **Then** la tarjeta muestra el ícono genérico sin romper su contenido.

---

### User Story 2 - Completar imágenes sin agotar la cuota (Priority: P1)

Como usuario autenticado, quiero iniciar manualmente la sincronización de imágenes de TheSportsDB sin que la consulta de las tarjetas dependa de peticiones a esa API ni exceda su límite gratuito.

**Why this priority**: El catálogo contiene muchos jugadores y la cuota gratuita no permite consultar al proveedor por cada visita.

**Independent Test**: Con jugadores disponibles, iniciar explícitamente la sincronización de imágenes y comprobar que las consultas al catálogo siguen disponibles mientras se resuelven imágenes pendientes, enviando como máximo una solicitud a TheSportsDB cada 2 segundos y sin superar 30 en cualquier intervalo de 60 segundos.

**Acceptance Scenarios**:

1. **Given** jugadores nuevos o sin imagen en el catálogo local, **When** un usuario autenticado inicia la sincronización manual de imágenes, **Then** se guardan las imágenes encontradas de forma progresiva y la solicitud responde al terminar con las cantidades procesadas y encontradas.
2. **Given** una página del catálogo abierta mientras quedan imágenes pendientes, **When** el usuario consulta o cambia de página, **Then** obtiene los datos locales disponibles y las imágenes ya resueltas, sin generar una búsqueda externa por cada tarjeta.
3. **Given** que se vuelve a sincronizar un jugador que ya tiene imagen, **When** sus datos siguen correspondiendo al mismo jugador, **Then** se conserva la imagen y no se repite innecesariamente su búsqueda.
4. **Given** una sincronización de imágenes en curso, **When** un usuario autenticado intenta iniciar otra, **Then** recibe un conflicto que indica que ya hay una ejecución activa y no se inicia una segunda.

---

### User Story 3 - Continuar tras resultados incompletos o límite de cuota (Priority: P2)

Como usuario, quiero seguir viendo el catálogo aunque TheSportsDB no encuentre una foto o responda con un error, y que el trabajo pendiente pueda continuar más adelante.

**Why this priority**: La disponibilidad y exactitud del catálogo no deben depender de un proveedor opcional de imágenes.

**Independent Test**: Simular ausencia de coincidencia, error temporal y respuesta 429; comprobar que las tarjetas conservan el respaldo y que las búsquedas pendientes pueden retomarse sin afectar los datos del jugador.

**Acceptance Scenarios**:

1. **Given** una búsqueda sin coincidencia de nombre y equipo o sin imagen, **When** se procesa el resultado, **Then** el jugador mantiene su ícono genérico y no recibe la imagen de otro jugador.
2. **Given** un error de comunicación con TheSportsDB, **When** se intenta resolver una imagen, **Then** la consulta y sincronización del catálogo siguen funcionando y el intento queda disponible para una ejecución posterior.
3. **Given** una respuesta `429 Too Many Requests`, **When** se procesa, **Then** no se insiste inmediatamente: se respeta `Retry-After` si está presente y es válido; en otro caso, se espera al menos 60 segundos antes de volver a intentar, sin repetir indefinidamente la misma solicitud.
4. **Given** una ejecución que ya guardó algunas imágenes, **When** una falla del proveedor impide completar las búsquedas pendientes, **Then** la solicitud devuelve un error de ejecución incompleta, conserva las imágenes guardadas y permite retomar lo pendiente con una nueva invocación.

### Edge Cases

- Nombres iguales, variaciones ortográficas y traspasos de equipo no deben provocar una asignación por coincidencia débil: si no coinciden nombre y equipo de forma inequívoca, no se asocia imagen.
- Un registro de TheSportsDB sin URL de retrato utilizable no constituye una imagen encontrada.
- Una nueva sincronización que inactiva jugadores no debe eliminar imágenes de los jugadores que permanecen en el catálogo; la consulta sigue exponiendo solo activos.
- Una sincronización de Football-Data.org, completa o fallida, no debe disparar automáticamente búsquedas de imágenes.
- Si se interrumpe el enriquecimiento, debe ser posible retomarlo sin perder imágenes ya resueltas ni realizar búsquedas masivas cada vez que se abre la página.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El catálogo debe admitir una URL opcional de retrato por jugador, asociada a su identificador existente de Football-Data.org. Los identificadores de TheSportsDB no deben reemplazar la identidad del catálogo.
- **FR-002**: La respuesta existente de `GET /api/players` debe incluir `imageUrl` opcional para cada jugador; los demás campos y la paginación mantienen su significado. `imageUrl` es `null` cuando no hay un retrato asociado.
- **FR-003**: La página de jugadores debe usar `imageUrl` cuando exista y volver al ícono genérico si falta la URL o falla la carga de la imagen, sin perder el nombre ni el resto de la tarjeta.
- **FR-004**: La obtención de imágenes debe usar la API v1 gratuita de TheSportsDB para buscar jugadores y aprovechar únicamente su imagen de jugador, sin sustituir los datos de nombre, equipo, liga o posición procedentes de Football-Data.org.
- **FR-005**: Antes de asociar una imagen, el sistema debe comprobar coincidencia inequívoca tanto de nombre como de equipo entre el jugador del catálogo y un único candidato de TheSportsDB. Si falta o difiere cualquiera de los dos datos, o hay más de un candidato válido, no se asocia imagen.
- **FR-006**: El sistema debe ofrecer `POST /api/players/images/sync` para que un usuario autenticado inicie explícitamente la búsqueda de imágenes pendientes. Esta solicitud debe permanecer abierta hasta que finalice la ejecución y devolver las cantidades procesadas y encontradas. La búsqueda no debe efectuarse en `GET /api/players` ni retrasar `POST /api/players/sync`; una nueva invocación debe poder reanudar el trabajo pendiente tras una interrupción. Ni la sincronización de jugadores ni el inicio de la aplicación deben iniciar búsquedas de imágenes automáticamente.
- **FR-007**: Se deben conservar los retratos ya asociados durante nuevas sincronizaciones de los mismos jugadores y evitar consultas repetidas innecesarias, incluidas búsquedas ya resueltas sin imagen; los pendientes o fallos transitorios deben poder reintentarse posteriormente.
- **FR-008**: El conjunto de solicitudes hacia TheSportsDB debe espaciarse al menos 2 segundos entre inicios de peticiones consecutivas y limitarse a un máximo de 30 en cualquier intervalo de 60 segundos dentro de la única instancia prevista para esta feature; la concurrencia de trabajos no debe aumentar esos máximos. Los reintentos también cuentan como peticiones.
- **FR-009**: Ante `429`, el sistema debe usar un `Retry-After` válido si lo recibe y, en su ausencia, esperar al menos 60 segundos; los reintentos de una misma búsqueda deben tener un límite y no bloquear el catálogo.
- **FR-010**: La falta de imagen, el error o la indisponibilidad de TheSportsDB no deben alterar los campos obligatorios, estados ni resultado de la sincronización del catálogo desde Football-Data.org.
- **FR-011**: La imagen que se asocie debe provenir de una URL de imagen de TheSportsDB validada; no deben exponerse al cliente URLs arbitrarias recibidas del proveedor.
- **FR-012**: `POST /api/players/images/sync` debe permitir una sola ejecución activa por instancia. Si ya existe una sincronización de imágenes en curso, una nueva invocación debe responder `409 Conflict` sin iniciar otra ejecución ni duplicar peticiones a TheSportsDB.
- **FR-013**: Si un fallo de TheSportsDB impide completar una ejecución de `POST /api/players/images/sync`, la solicitud debe responder con un error de ejecución incompleta; las imágenes obtenidas antes del fallo se conservan y los jugadores pendientes se intentan nuevamente en la siguiente invocación manual.

### Key Entities

- **Jugador del catálogo**: Identificado por su ID de Football-Data.org; conserva sus datos y estado, y puede tener un retrato opcional.
- **Resolución de imagen**: Estado de la búsqueda de retrato de un jugador, con resultado encontrado, sin coincidencia o pendiente de reintento, para evitar consultas innecesarias y permitir continuidad.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En una muestra que incluya jugadores con foto, sin coincidencia y con imagen inaccesible, el 100 % de las tarjetas muestra el retrato correcto o el ícono genérico sin perder los datos del jugador.
- **SC-002**: En una ejecución con más de 30 imágenes pendientes, transcurren al menos 2 segundos entre inicios de solicitudes consecutivas, no se envían más de 30 solicitudes a TheSportsDB en ningún intervalo de 60 segundos y las imágenes disponibles aparecen progresivamente.
- **SC-003**: El 100 % de las consultas a `GET /api/players` se atiende desde el catálogo local sin solicitar una imagen a TheSportsDB en ese momento.
- **SC-004**: Tras repetir una sincronización sin cambios de identidad, las imágenes ya resueltas permanecen asociadas al jugador correcto y no se vuelven a buscar masivamente.
- **SC-005**: Ante `429`, ausencia de imágenes o indisponibilidad de TheSportsDB, el catálogo sigue siendo consultable y la sincronización de jugadores no pierde ni inactiva registros por fallos del proveedor de imágenes.
- **SC-006**: En una muestra de homónimos y equipos diferentes, el 100 % de las imágenes asociadas corresponde a una coincidencia inequívoca de nombre y equipo; los demás jugadores conservan el ícono genérico.

## Assumptions

- El uso inicial es local y no se desplegará públicamente en esta etapa. Antes de publicar las fotos se revisarán los términos vigentes y los derechos de las imágenes concretas.
- Se utiliza el plan gratuito de TheSportsDB, con clave pública `123` y límite documentado de 30 solicitudes por minuto. Su documentación indica esperar otro minuto ante `429`, sin garantizar una cabecera `Retry-After`.
- Se mantiene el despliegue de una sola instancia asumido por el catálogo actual.
- Esta feature amplía el contrato de jugador de `002-player-catalog` y reemplaza el uso incondicional del ícono genérico especificado en `005-players-page` por el retrato opcional con respaldo genérico.

## Out of Scope

- Reemplazar Football-Data.org como fuente del catálogo o agregar estadísticas de jugadores.
- Garantizar una foto para todos los jugadores o aceptar coincidencias ambiguas para aumentar la cobertura.
- Publicación pública, distribución en tiendas de aplicaciones y gestión de licencias individuales de fotografías.
- Cambiar el funcionamiento de filtros, paginación o autenticación de la página de jugadores.

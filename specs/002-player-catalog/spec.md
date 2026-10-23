# Feature Specification: Catálogo de jugadores

**Feature Branch**: `[002-player-catalog]`

**Created**: 2026-09-15

**Status**: Ready for implementation

**Input**: Catálogo local de jugadores de fútbol con identidad propia de FootballMarket, consultable por una API paginada y actualizado exclusivamente mediante una sincronización manual con Football-Data.org. Las identidades de proveedores externos se modelan como referencias externas independientes del jugador.

## Clarifications

### Session 2026-09-30

- Q: Al migrar los jugadores existentes cuyo identificador actual proviene de Football-Data.org, ¿el nuevo identificador interno debe conservar el valor numérico actual? → A: No; el identificador interno se reasigna a un valor nuevo e independiente, y el valor anterior se conserva únicamente como `externalId` de la referencia `FOOTBALL_DATA`.
- Q: Cuando Football-Data.org omite, vacía o entrega con formato no reconocible `dateOfBirth` o `nationality`, ¿se borra el valor almacenado? → A: No; se interpreta como "dato no informado", no borra ni sobrescribe un valor previo válido, y nunca descarta el jugador.
- Q: Al crear un jugador nuevo durante la sincronización, ¿el alta de su referencia `FOOTBALL_DATA` es independiente? → A: No; ambas altas forman una única operación lógica y no se considera completada si la referencia no pudo persistirse.
- Q: Si un `externalId` de Football-Data.org entra en conflicto con la identidad de otro jugador, ¿qué debe hacer la sincronización? → A: Descartar el registro por conflicto de identidad, registrar el motivo y continuar; nunca reasignar la referencia ni alterar jugadores, y el conflicto no falla por sí solo la sincronización.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el catálogo de jugadores (Priority: P1)

Como consumidor de la API, quiero consultar el catálogo de jugadores de fútbol en páginas para conocer la información disponible sin depender de la disponibilidad actual de la fuente externa.

**Why this priority**: Es la funcionalidad principal del catálogo y debe permanecer disponible aun cuando Football-Data.org no lo esté.

**Independent Test**: Con jugadores activos en el catálogo, solicitar `GET /api/players?page=0&size=20` y comprobar que se recibe una página de jugadores con los campos definidos. Ante una indisponibilidad de Football-Data.org, comprobar que la consulta continúa respondiendo con la información local.

**Acceptance Scenarios**:

1. **Given** que existen jugadores activos en el catálogo local, **When** un consumidor solicita `GET /api/players?page=0&size=20`, **Then** recibe `200 OK` con la página de jugadores activos correspondiente y la información necesaria para navegar el catálogo.
2. **Given** que el catálogo local no contiene jugadores activos, **When** un consumidor solicita una página válida de `GET /api/players`, **Then** recibe `200 OK` con una página vacía y su información de paginación.
3. **Given** que Football-Data.org no está disponible, **When** un consumidor solicita `GET /api/players`, **Then** recibe `200 OK` con los jugadores activos disponibles localmente.
4. **Given** que se solicita un número de página o tamaño de página inválido, **When** se llama a `GET /api/players`, **Then** el sistema responde `400 Bad Request`.
5. **Given** que un consumidor no informa `page` ni `size`, **When** solicita `GET /api/players`, **Then** el sistema utiliza `page=0` y `size=20`.
6. **Given** que un jugador tiene datos de origen opcionales ausentes, **When** se lo expone por `GET /api/players`, **Then** esos datos aparecen como ausentes (`null`) y el resto del jugador se expone normalmente.
7. **Given** que existen jugadores con referencias a Football-Data.org, **When** se consulta `GET /api/players`, **Then** el `id` expuesto es el identificador interno de FootballMarket y el jugador no expone el proveedor ni su identificador externo.

### User Story 2 - Sincronizar manualmente el catálogo (Priority: P1)

Como usuario autenticado de la aplicación, quiero iniciar explícitamente una sincronización del catálogo para incorporar los jugadores disponibles en Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`), sin que la identidad interna de los jugadores dependa de Football-Data.org.

**Why this priority**: Permite mantener el catálogo actualizado sin comprometer la disponibilidad de las consultas locales, y es el único mecanismo que resuelve la identidad de los jugadores respecto de la fuente externa.

**Independent Test**: Con datos disponibles en Football-Data.org para `PL`, `BL1`, `PD`, `SA` y `FL1`, invocar `POST /api/players/sync` y comprobar que se consultan exactamente esas cinco ligas y que sus jugadores se incorporan, actualizan, activan o inactivan según corresponda, junto con el resultado informado de la sincronización.

**Acceptance Scenarios**:

1. **Given** que Football-Data.org está disponible para `PL`, `BL1`, `PD`, `SA` y `FL1`, **When** un usuario autenticado invoca `POST /api/players/sync`, **Then** el sistema consulta exactamente esas cinco ligas, incorpora o actualiza sus jugadores válidos, actualiza sus estados cuando corresponda y devuelve una respuesta exitosa que informa el resultado de la sincronización.
2. **Given** que un jugador recibido no existía previamente en el catálogo, **When** finaliza una sincronización completa y exitosa, **Then** el jugador se incorpora al catálogo con un identificador interno propio, se crea su referencia externa `FOOTBALL_DATA` con el identificador recibido y queda activo.
3. **Given** que un jugador recibido ya existía en el catálogo, **When** finaliza una sincronización completa y exitosa, **Then** se localiza por su referencia externa `FOOTBALL_DATA` e identificador externo, se actualizan sus datos, permanece o vuelve a quedar activo y no se genera un duplicado.
4. **Given** que el mismo jugador de Football-Data.org aparece en sincronizaciones sucesivas, **When** se procesa cada sincronización, **Then** el catálogo conserva un único jugador interno con una única referencia `FOOTBALL_DATA` y el mismo identificador interno.
5. **Given** que un jugador activo ya no está presente en una sincronización completa y exitosa, **When** esta finaliza, **Then** el jugador se conserva en el catálogo y queda inactivo.
6. **Given** que un jugador inactivo vuelve a estar presente en una sincronización completa y exitosa, **When** esta finaliza, **Then** sus datos se actualizan y vuelve a quedar activo.
7. **Given** que la fuente externa no puede completarse, **When** un usuario autenticado invoca `POST /api/players/sync`, **Then** el sistema responde con un error, registra el error y conserva el estado correcto de los jugadores existentes.
8. **Given** que un registro recibido no contiene la información obligatoria, **When** se procesa la sincronización, **Then** el registro se descarta, se registra el motivo y los demás registros válidos continúan procesándose.
9. **Given** que el identificador recibido ya está asociado a otro jugador del catálogo, **When** se procesa la sincronización, **Then** el registro se descarta por conflicto de identidad, la referencia existente y ambos jugadores quedan sin cambios, se registra el motivo y la sincronización continúa con los demás registros.
10. **Given** que un registro recibido contiene identificador, nombre, equipo, liga y posición pero no fecha de nacimiento ni nacionalidad, **When** se procesa la sincronización, **Then** el jugador se incorpora o actualiza igualmente y esos atributos quedan sin valor.
11. **Given** que un jugador existente sincronizado previamente tiene fecha de nacimiento o nacionalidad y la nueva respuesta las omite, **When** se procesa la sincronización, **Then** el jugador se conserva con los valores anteriores en esos atributos.
12. **Given** que el catálogo contiene jugadores migrados desde el modelo anterior, **When** se consulta `GET /api/players` o se ejecuta una sincronización completa y exitosa, **Then** cada jugador conserva su correspondencia con su referencia `FOOTBALL_DATA` y no se generan jugadores duplicados.
13. **Given** que un jugador tiene un `imageUrl` con valor o sin él, **When** se procesa una sincronización con Football-Data.org, **Then** el `imageUrl` no se modifica ni se borra, puede permanecer sin valor y la sincronización no obtiene imágenes de ninguna fuente.
14. **Given** que una solicitud a `POST /api/players/sync` no presenta una autenticación válida, **When** se procesa la solicitud, **Then** el sistema responde `401 Unauthorized` sin iniciar la sincronización.

### Edge Cases

- Todo jugador creado por la sincronización nace junto con su referencia `FOOTBALL_DATA`; un jugador proveniente de Football-Data.org sin esa referencia no es un resultado válido de esta feature, aunque más adelante puedan existir jugadores con otro origen y sin ella.
- Si un jugador aparece en más de una de las cinco ligas, debe figurar una sola vez en el catálogo, identificado por su referencia externa `FOOTBALL_DATA` y su identificador externo. Se expone como `league` la primera liga en el orden configurado entre `PL`, `BL1`, `PD`, `SA`, `FL1`.
- Si un mismo identificador externo de Football-Data.org se recibe con datos de nombre, equipo, liga o posición distintos entre sincronizaciones, el jugador se resuelve siempre por su referencia externa y se actualiza; el identificador interno no cambia.
- Si un identificador externo de Football-Data.org se recibe sin nombre, equipo, liga o posición, el registro se descarta por datos obligatorios ausentes y no se crea ni se altera ningún jugador ni referencia externa.
- Si un registro recibido no informa fecha de nacimiento ni nacionalidad, el jugador no se descarta por ello y esos atributos quedan sin valor.
- Si un jugador existente tiene fecha de nacimiento o nacionalidad y la fuente los omite, los deja vacíos o los devuelve con un formato no reconocible, los valores previos válidos se conservan; un formato no reconocible no descarta el jugador.
- Si un jugador existente tiene un valor previo de fecha de nacimiento o nacionalidad y la fuente informa un valor válido, el valor almacenado se actualiza.
- Si un jugador tiene un `imageUrl` con valor o sin él, la sincronización desde Football-Data.org no lo modifica ni lo borra. Esta feature nunca escribe ese atributo y puede dejarlo sin valor.
- Si un registro recibido entra en conflicto de identidad porque su referencia `(FOOTBALL_DATA, externalId)` ya está asociada a otro jugador, el registro se considera inválido: no se reasigna ni se modifica la referencia existente, no se altera ningún jugador implicado, se registra el motivo y el procesamiento continúa con los demás registros válidos. Un conflicto de este tipo no falla por sí solo la sincronización completa.
- Si un jugador interno no tiene ninguna referencia externa, no puede resolverse por sincronización y solo puede incorporarse por otras vías, fuera del alcance de esta feature.
- Si no puede obtenerse por completo cualquiera de las cinco ligas, la sincronización completa debe considerarse fallida.
- Una sincronización incompleta o fallida no representa una actualización válida del catálogo y no debe causar inactivaciones incorrectas.
- Los registros inválidos se descartan y no se consideran jugadores disponibles en una sincronización completa.
- Solo los jugadores activos forman parte de la respuesta de `GET /api/players`; los inactivos se conservan para futuras sincronizaciones y no se exponen en esta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **RF-001**: El sistema debe exponer `GET /api/players` para consultar exclusivamente el catálogo local. La consulta no debe depender de la disponibilidad de Football-Data.org en ese momento.
- **RF-002**: `GET /api/players` debe aceptar los parámetros de paginación `page` y `size`. Si no se informa `page`, debe utilizarse el valor 0; si no se informa `size`, debe utilizarse el valor 20. El valor mínimo permitido para `page` es 0. El valor mínimo permitido para `size` es 1 y el máximo es 100. Si `page` es menor que 0, o si `size` es menor que 1 o mayor que 100, el sistema debe responder `400 Bad Request`.
- **RF-003**: La respuesta exitosa de `GET /api/players` debe contener la lista de jugadores de la página, la página actual, el tamaño de página, la cantidad total de elementos y la cantidad total de páginas. No debe ser posible obtener todo el catálogo en una única respuesta sin paginación.
- **RF-004**: Cada jugador expuesto por el catálogo debe contener exactamente los campos `id`, `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality` e `imageUrl`. `id` debe ser el identificador interno de FootballMarket del jugador y nunca el identificador proporcionado por un proveedor externo. `imageUrl` debe ser opcional y puede presentarse sin valor. La respuesta no debe exponer las referencias externas ni el proveedor que las originó.
- **RF-005**: El contrato HTTP del catálogo debe utilizar representaciones de datos específicas para la API y no debe exponer directamente la entidad de persistencia.
- **RF-006**: El sistema debe conservar localmente los jugadores obtenidos desde Football-Data.org, la única fuente externa utilizada por esta feature, junto con la referencia externa que identifica a cada jugador en esa fuente.
- **RF-007**: La sincronización debe obtener desde Football-Data.org los jugadores correspondientes exclusivamente a estas cinco ligas, en el orden configurado entre Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`). No debe admitir ligas adicionales ni omitir ninguna de las cinco. No se debe establecer una cantidad fija de jugadores para el catálogo.
- **RF-008**: La credencial utilizada para acceder a Football-Data.org debe mantenerse segura: no debe exponerse por la API, quedar hardcodeada, versionarse ni registrarse en logs.
- **RF-009**: El sistema debe exponer `POST /api/players/sync` para iniciar manualmente la sincronización. Esta feature no debe realizar sincronización automática.
- **RF-010**: Antes de incorporar o actualizar un jugador, el sistema debe verificar que estén presentes el identificador de Football-Data.org, `name`, `team`, `league` y `position`. Si falta alguno, debe descartar el registro y registrar el motivo.
- **RF-011**: El jugador del catálogo debe tener un identificador interno propio, asignado y gestionado por FootballMarket. El identificador recibido desde Football-Data.org no debe utilizarse como identificador del jugador.
- **RF-012**: Cada jugador debe poder tener una o más referencias externas, cada una con un proveedor y el identificador que ese proveedor asigna al jugador.
- **RF-013**: Un jugador no puede tener dos referencias al mismo proveedor para el mismo identificador externo. La combinación de proveedor e identificador externo debe identificar de forma unívoca una referencia externa y no puede asociarse a más de un jugador. Si un registro recibido entra en conflicto de identidad porque su referencia `(FOOTBALL_DATA, externalId)` ya pertenece a otro jugador, debe descartarse como inválido, registrarse el motivo y continuar con los demás registros; no debe reasignarse la referencia existente ni modificar algún jugador, y por sí solo no hace fallar la sincronización completa.
- **RF-014**: El modelo de proveedores debe soportar al menos Football-Data.org y TheSportsDB, de modo que el mismo jugador pueda referenciarse en más de un proveedor sin cambiar su identidad interna.
- **RF-015**: Durante una sincronización con Football-Data.org, el sistema debe localizar cada jugador recibido mediante su referencia externa de proveedor `FOOTBALL_DATA` y el identificador externo recibido. Si existe esa referencia, debe actualizar el jugador asociado; si no existe, debe crear un nuevo jugador con identidad interna propia y, en el mismo alta lógico, su correspondiente referencia externa `FOOTBALL_DATA`.
- **RF-016**: El alta de un jugador procedente de Football-Data.org y la alta de su referencia externa `FOOTBALL_DATA` forman una única operación lógica: no se considera completada si la referencia no pudo persistirse. La sincronización no puede dejar como resultado un jugador proveniente de Football-Data.org sin su referencia `FOOTBALL_DATA`.
- **RF-017**: Sincronizaciones sucesivas del mismo jugador procedente de Football-Data.org no deben crear jugadores internos adicionales ni referencias externas duplicadas para ese jugador.
- **RF-018**: `dateOfBirth` y `nationality` forman parte del modelo del jugador y son opcionales. Cuando la fuente externa los informe con un valor válido, el sistema debe almacenarlo o actualizar el valor existente. La ausencia del campo, un valor vacío y un formato no reconocible se interpretan como "dato no informado por la fuente": no deben borrar ni sobrescribir un valor previo válido, y en un jugador nuevo el atributo queda sin valor. Un problema únicamente en `dateOfBirth` o `nationality` no debe descartar el jugador.
- **RF-019**: `imageUrl` forma parte del modelo del jugador como dato opcional y puede presentarse sin valor. Esta feature no debe obtenerlo, resolverlo ni modificarlo a partir de proveedores distintos de Football-Data.org, y la sincronización con Football-Data.org no debe introducir dependencias operacionales con otros proveedores.
- **RF-020**: Tras una sincronización completa y exitosa, los jugadores válidos recibidos deben quedar activos. Los jugadores que estaban activos y no aparecen en la fuente, determinado por sus referencias de Football-Data.org, deben conservarse y pasar a estar inactivos, sin eliminación física. Un jugador interno sin referencia a Football-Data.org no debe ser inactivado por esta feature.
- **RF-021**: Si un jugador inactivo vuelve a aparecer en una sincronización completa y exitosa, debe actualizarse y volver a estar activo.
- **RF-022**: Si la sincronización falla o no se completa, el sistema debe registrar el error y no debe eliminar jugadores existentes, crear referencias inconsistentes ni marcarlos incorrectamente como inactivos. `GET /api/players` debe continuar disponible con el catálogo local, incluso cuando esté vacío.
- **RF-023**: El resultado de `POST /api/players/sync` debe comunicar el resultado de la operación mediante una respuesta exitosa cuando se complete y una respuesta de error ante la indisponibilidad de la fuente externa. La respuesta no debe revelar credenciales ni información sensible.
- **RF-024**: Los errores de sincronización, los registros inválidos descartados y el inicio y resultado de cada sincronización deben quedar registrados. Los registros deben incluir las cantidades de jugadores obtenidos, incorporados, actualizados y marcados como inactivos, sin incluir credenciales ni datos sensibles.
- **RF-025**: OpenAPI/Swagger debe documentar `GET /api/players` y `POST /api/players/sync`, los parámetros y límites de paginación, la estructura de las respuestas exitosas, la opcionalidad de los atributos del jugador y los códigos de error aplicables.
- **RF-026**: `POST /api/players/sync` debe estar disponible para cualquier usuario autenticado mediante el mecanismo vigente del proyecto, sin exigir un rol o permiso adicional. Esta feature no debe incorporar mecanismos de autenticación ni roles nuevos.
- **RF-027**: Ante una respuesta `429 Too Many Requests` de Football-Data.org, el sistema debe respetar `Retry-After` y reintentar como máximo tres veces. Otros fallos HTTP o de comunicación no deben reintentarse.
- **RF-028**: La configuración efectiva de la sincronización debe representar exactamente los códigos `PL,BL1,PD,SA,FL1`. Si falta un código, existe uno adicional o se duplica uno, el sistema debe rechazar la configuración y no iniciar una sincronización parcial.
- **RF-029**: Los jugadores ya existentes en el catálogo deben migrar al modelo de identidad propio conservando su correspondencia: cada jugador conserva exactamente una referencia `FOOTBALL_DATA` cuyo `externalId` es el valor que ese identificador tenía como identificador del jugador. El identificador interno del jugador puede reasignarse a un valor nuevo e independiente, igual que el de cualquier jugador creado posteriormente. No es un requisito preservar el valor numérico del identificador expuesto actualmente por `GET /api/players`. La migración no debe perder jugadores, duplicarlos ni alterar su estado.

### Key Entities *(include if feature involves data)*

- **Jugador**: representa a un jugador de fútbol del catálogo. Tiene un identificador interno propio de FootballMarket, nombre, equipo, liga, posición, estado activo o inactivo, y los atributos opcionales fecha de nacimiento, nacionalidad e imagen. Sus identificadores en fuentes externas no forman parte de su identidad.
- **Referencia externa de jugador**: asocia un jugador con un proveedor externo y con el identificador que ese proveedor le asigna. Es el mecanismo por el que el catálogo reconoce a un jugador procedente de un proveedor concreto.
- **Proveedor de jugadores**: identifica la fuente externa de una referencia externa. Admite Football-Data.org y TheSportsDB.
- **Página del catálogo**: representa una sección navegable del catálogo e informa los jugadores de la página, la página actual, el tamaño de página y los totales necesarios para navegarlo.
- **Resultado de sincronización**: comunica si una sincronización manual se completó o falló y resume los cambios efectuados sobre el catálogo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una solicitud válida a `GET /api/players` devuelve `200 OK` con una página del catálogo local, tanto si hay jugadores activos como si el catálogo está vacío.
- **SC-002**: Cada jugador devuelto por el catálogo contiene `id`, `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality` e `imageUrl`, no expone el estado interno, las referencias externas ni la entidad de persistencia, y su `id` es el identificador interno de FootballMarket.
- **SC-003**: `dateOfBirth` y `nationality` aparecen en la respuesta del catálogo, con valor o ausentes según corresponda; `imageUrl` aparece como valor opcional y puede venir ausente sin invalidar la respuesta.
- **SC-004**: Las solicitudes paginadas permiten navegar el catálogo sin recibirlo completo en una sola respuesta. Cuando se omiten, se aplican `page=0` y `size=20`; `page < 0`, `size < 1` y `size > 100` devuelven `400 Bad Request`.
- **SC-005**: Después de una sincronización completa y exitosa, los nuevos jugadores válidos están activos con identidad interna propia, los existentes se resuelven por su referencia `FOOTBALL_DATA` y se actualizan sin duplicarse, y los que ya no están disponibles quedan inactivos sin eliminarse.
- **SC-006**: La consulta del catálogo no necesita ningún dato de Football-Data.org en tiempo de consulta y sigue respondiendo cuando la fuente externa no está disponible.
- **SC-007**: Si Football-Data.org no está disponible, una sincronización informa el error sin alterar incorrectamente los jugadores existentes, mientras que `GET /api/players` continúa devolviendo el catálogo local disponible.
- **SC-008**: La documentación OpenAPI permite identificar ambos endpoints, la paginación, las estructuras de respuesta y los códigos de respuesta aplicables.
- **SC-009**: Cada sincronización iniciada consulta exactamente `PL`, `BL1`, `PD`, `SA` y `FL1`, sin exigir un orden; una configuración distinta es rechazada antes de consultar Football-Data.org.
- **SC-010**: Ninguna operación de esta feature realiza consultas a proveedores distintos de Football-Data.org.

## Assumptions

- Football-Data.org proporciona un identificador estable por jugador que permite reconocerlo entre sincronizaciones y asociarlo a un único jugador del catálogo.
- El proveedor y el identificador externo no forman parte de la identidad interna del jugador ni de su representación pública.
- El catálogo siempre incluye únicamente Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`), sin exigir un orden.
- Football-Data.org puede omitir la fecha de nacimiento y la nacionalidad de un jugador sin que ello invalide el registro.
- Las credenciales necesarias para acceder a Football-Data.org están disponibles de forma segura en el entorno de ejecución.
- La aplicación se despliega en una sola instancia; la serialización de sincronizaciones ocurre dentro de esa instancia.

## Out of Scope

- Sincronización automática.
- Filtros, búsqueda, ordenamiento, `GET /api/players/{id}` o la exposición de jugadores inactivos.
- Consulta, resolución o integración HTTP con TheSportsDB.
- Búsqueda de jugadores en TheSportsDB y matching entre Football-Data.org y TheSportsDB.
- Obtención o actualización de imágenes desde TheSportsDB u otros proveedores; `imageUrl` solo forma parte del modelo del jugador.
- Cualquier estrategia automática de enriquecimiento con proveedores distintos de Football-Data.org.
- Exposición de las referencias externas (proveedor e identificador externo) en el contrato del catálogo.
- Compras, ventas, tokens, portfolio y transacciones.
- Frontend.
- Fuentes externas adicionales o scraping.
- Autenticación o roles nuevos para esta feature.
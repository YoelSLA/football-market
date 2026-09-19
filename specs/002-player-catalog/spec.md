# Feature Specification: Catálogo de jugadores

**Feature Branch**: `[002-player-catalog]`

**Created**: 2026-09-15

**Status**: Ready for implementation

**Input**: Catálogo local de jugadores de fútbol, consultable por una API paginada y actualizado exclusivamente mediante una sincronización manual con Football-Data.org.

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

### User Story 2 - Sincronizar manualmente el catálogo (Priority: P1)

Como usuario autenticado de la aplicación, quiero iniciar explícitamente una sincronización del catálogo para incorporar los jugadores disponibles en las competiciones configuradas.

**Why this priority**: Permite mantener el catálogo actualizado sin comprometer la disponibilidad de las consultas locales.

**Independent Test**: Con datos disponibles en Football-Data.org para las competiciones configuradas, invocar `POST /api/players/sync` y comprobar la incorporación, actualización, activación e inactivación de jugadores, junto con el resultado informado de la sincronización.

**Acceptance Scenarios**:

1. **Given** que Football-Data.org está disponible para todas las competiciones configuradas, **When** un usuario autenticado invoca `POST /api/players/sync`, **Then** el sistema incorpora o actualiza los jugadores válidos, actualiza sus estados cuando corresponda y devuelve una respuesta exitosa que informa el resultado de la sincronización.
2. **Given** que un jugador recibido no existía previamente en el catálogo, **When** finaliza una sincronización completa y exitosa, **Then** el jugador se incorpora al catálogo como activo.
3. **Given** que un jugador recibido ya existía en el catálogo, **When** finaliza una sincronización completa y exitosa, **Then** sus datos se actualizan, permanece o vuelve a quedar activo y no se genera un duplicado.
4. **Given** que un jugador activo ya no está presente en una sincronización completa y exitosa, **When** esta finaliza, **Then** el jugador se conserva en el catálogo y queda inactivo.
5. **Given** que un jugador inactivo vuelve a estar presente en una sincronización completa y exitosa, **When** esta finaliza, **Then** sus datos se actualizan y vuelve a quedar activo.
6. **Given** que la fuente externa no puede completarse, **When** un usuario autenticado invoca `POST /api/players/sync`, **Then** el sistema responde con un error, registra el error y conserva el estado correcto de los jugadores existentes.
7. **Given** que un registro recibido no contiene la información obligatoria, **When** se procesa la sincronización, **Then** el registro se descarta, se registra el motivo y los demás registros válidos continúan procesándose.
8. **Given** que una solicitud a `POST /api/players/sync` no presenta una autenticación válida, **When** se procesa la solicitud, **Then** el sistema responde `401 Unauthorized` sin iniciar la sincronización.

### Edge Cases

- Si un jugador aparece en más de una competición configurada, debe figurar una sola vez en el catálogo, identificado por su ID de Football-Data.org. Se expone como `league` la primera competición en el orden configurado para el catálogo.
- Una sincronización incompleta o fallida no representa una actualización válida del catálogo y no debe causar inactivaciones incorrectas.
- Los registros inválidos se descartan y no se consideran jugadores disponibles en una sincronización completa.
- Solo los jugadores activos forman parte de la respuesta de `GET /api/players`; los inactivos se conservan para futuras sincronizaciones y no se exponen en esta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **RF-001**: El sistema debe exponer `GET /api/players` para consultar exclusivamente el catálogo local. La consulta no debe depender de la disponibilidad de Football-Data.org en ese momento.
- **RF-002**: `GET /api/players` debe aceptar los parámetros de paginación `page` y `size`. Si no se informa `page`, debe utilizarse el valor 0; si no se informa `size`, debe utilizarse el valor 20. El valor mínimo permitido para `page` es 0. El valor mínimo permitido para `size` es 1 y el máximo es 100. Si `page` es menor que 0, o si `size` es menor que 1 o mayor que 100, el sistema debe responder `400 Bad Request`.
- **RF-003**: La respuesta exitosa de `GET /api/players` debe contener la lista de jugadores de la página, la página actual, el tamaño de página, la cantidad total de elementos y la cantidad total de páginas. No debe ser posible obtener todo el catálogo en una única respuesta sin paginación.
- **RF-004**: Cada jugador expuesto por el catálogo debe contener exactamente los campos `id`, `name`, `team`, `league` y `position`.
- **RF-005**: El contrato HTTP del catálogo debe utilizar representaciones de datos específicas para la API y no debe exponer directamente la entidad de persistencia.
- **RF-006**: El sistema debe conservar localmente los jugadores obtenidos desde Football-Data.org, la fuente externa seleccionada para esta feature.
- **RF-007**: La sincronización debe obtener desde Football-Data.org los jugadores correspondientes a las competiciones configuradas para el proyecto. No se debe establecer una cantidad fija de jugadores para el catálogo.
- **RF-008**: La credencial utilizada para acceder a Football-Data.org debe mantenerse segura: no debe exponerse por la API, quedar hardcodeada, versionarse ni registrarse en logs.
- **RF-009**: El sistema debe exponer `POST /api/players/sync` para iniciar manualmente la sincronización. Esta feature no debe realizar sincronización automática.
- **RF-010**: Antes de incorporar o actualizar un jugador, el sistema debe verificar que estén presentes `id`, `name`, `team`, `league` y `position`. Si falta alguno, debe descartar el registro y registrar el motivo.
- **RF-011**: El ID proporcionado por Football-Data.org debe identificar de forma estable a cada jugador entre sincronizaciones. Debe utilizarse para evitar duplicados y para actualizar al jugador correspondiente cuando ya exista.
- **RF-012**: Tras una sincronización completa y exitosa, los jugadores válidos recibidos deben quedar activos. Los jugadores que estaban activos y ya no aparecen en la fuente deben conservarse y pasar a estar inactivos, sin eliminación física.
- **RF-013**: Si un jugador inactivo vuelve a aparecer en una sincronización completa y exitosa, debe actualizarse y volver a estar activo.
- **RF-014**: Si la sincronización falla o no se completa, el sistema debe registrar el error y no debe eliminar jugadores existentes ni marcarlos incorrectamente como inactivos. `GET /api/players` debe continuar disponible con el catálogo local, incluso cuando esté vacío.
- **RF-015**: El resultado de `POST /api/players/sync` debe comunicar el resultado de la operación mediante una respuesta exitosa cuando se complete y una respuesta de error ante la indisponibilidad de la fuente externa. La respuesta no debe revelar credenciales ni información sensible.
- **RF-016**: Los errores de sincronización, los registros inválidos descartados y el inicio y resultado de cada sincronización deben quedar registrados. Los registros deben incluir las cantidades de jugadores obtenidos, incorporados, actualizados y marcados como inactivos, sin incluir credenciales ni datos sensibles.
- **RF-017**: OpenAPI/Swagger debe documentar `GET /api/players` y `POST /api/players/sync`, los parámetros y límites de paginación, la estructura de las respuestas exitosas y los códigos de error aplicables.
- **RF-018**: `POST /api/players/sync` debe estar disponible para cualquier usuario autenticado mediante el mecanismo vigente del proyecto, sin exigir un rol o permiso adicional. Esta feature no debe incorporar mecanismos de autenticación ni roles nuevos.
- **RF-019**: `GET /api/players` y `POST /api/players/sync` deben incorporarse como solicitudes en la colección Postman versionada del proyecto. Cada solicitud debe reflejar su método, ruta, parámetros, headers, autenticación y body cuando corresponda, con ejemplos utilizables sin secretos ni credenciales reales.
- **RF-020**: Ante una respuesta `429 Too Many Requests` de Football-Data.org, el sistema debe respetar `Retry-After` y reintentar como máximo tres veces. Otros fallos HTTP o de comunicación no deben reintentarse.

### Key Entities *(include if feature involves data)*

- **Jugador**: representa a un jugador de fútbol del catálogo. Tiene un identificador externo estable, nombre, equipo, liga, posición y estado activo o inactivo.
- **Página del catálogo**: representa una sección navegable del catálogo e informa los jugadores de la página, la página actual, el tamaño de página y los totales necesarios para navegarlo.
- **Resultado de sincronización**: comunica si una sincronización manual se completó o falló y resume los cambios efectuados sobre el catálogo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una solicitud válida a `GET /api/players` devuelve `200 OK` con una página del catálogo local, tanto si hay jugadores activos como si el catálogo está vacío.
- **SC-002**: Cada jugador devuelto por el catálogo contiene `id`, `name`, `team`, `league` y `position`, y no expone el estado interno ni la entidad de persistencia.
- **SC-003**: Las solicitudes paginadas permiten navegar el catálogo sin recibirlo completo en una sola respuesta. Cuando se omiten, se aplican `page=0` y `size=20`; `page < 0`, `size < 1` y `size > 100` devuelven `400 Bad Request`.
- **SC-004**: Después de una sincronización completa y exitosa, los nuevos jugadores válidos están activos, los existentes se actualizan sin duplicarse y los que ya no están disponibles quedan inactivos sin eliminarse.
- **SC-005**: Si Football-Data.org no está disponible, una sincronización informa el error sin alterar incorrectamente los jugadores existentes, mientras que `GET /api/players` continúa devolviendo el catálogo local disponible.
- **SC-006**: La documentación OpenAPI permite identificar ambos endpoints, la paginación, las estructuras de respuesta y los códigos de respuesta aplicables.
- **SC-007**: La colección Postman versionada contiene solicitudes utilizables para ambos endpoints, coherentes con sus contratos y sin secretos ni credenciales reales.
- **SC-008**: Cualquier usuario autenticado puede iniciar `POST /api/players/sync` sin un rol adicional; una solicitud sin autenticación válida recibe `401 Unauthorized` y no inicia la sincronización.

## Assumptions

- Football-Data.org proporciona una identidad estable que permite reconocer a un mismo jugador entre sincronizaciones.
- Las competiciones incluidas en el catálogo están definidas en la configuración del proyecto.
- Las credenciales necesarias para acceder a Football-Data.org están disponibles de forma segura en el entorno de ejecución.
- La aplicación se despliega en una sola instancia; la serialización de sincronizaciones ocurre dentro de esa instancia.

## Out of Scope

- Sincronización automática.
- Filtros, búsqueda, ordenamiento, `GET /api/players/{id}` o la exposición de jugadores inactivos.
- Compras, ventas, tokens, portfolio y transacciones.
- Frontend.
- Fuentes externas adicionales o scraping.
- Autenticación o roles nuevos para esta feature.

# Research Report: Catálogo de jugadores

## Technical Decisions

### Football-Data.org source and flow

- **Decisión**: Usar Football-Data.org API v4 a través de una Integration dedicada.
- **Recorrido**: por cada competición configurada, consultar la competición, obtener sus equipos y consultar el plantel de cada equipo.
- **Motivo**: La API no ofrece un recurso global de catálogo de jugadores. La documentación oficial expone `GET /competitions/{code}`, `GET /competitions/{code}/teams` y `GET /teams/{id}`; el último contiene `squad` con los jugadores. [Competition](https://docs.football-data.org/general/v4/competition.html), [Team](https://docs.football-data.org/general/v4/team.html).
- **Mapeo**: `competition.name` → `league`; `team.name` → `team`; `squad[].id`, `squad[].name` y `squad[].position` → `id`, `name` y `position`.

### Competition configuration

- **Decisión**: Añadir una lista ordenada `football-data.competitions` a `FootballDataProperties`.
- **Motivo**: La spec establece que las competiciones pertenecen a la configuración del proyecto y que la primera en dicho orden determina `league` ante jugadores repetidos. La lista no tendrá valores hardcodeados.

### HTTP client and credentials

- **Decisión**: Configurar un `RestClient` dedicado, basado en las dependencias de Spring Web ya presentes, con URL base, HTTPS, timeouts y el encabezado de autenticación requerido por Football-Data.org.
- **Motivo**: No agrega dependencias, centraliza la configuración del proveedor y permite aislar las respuestas y errores externos. La API documenta `X-Auth-Token` para las solicitudes autenticadas. [Ejemplo Java oficial](https://docs.football-data.org/general/v4/coding/java.html).
- **Seguridad**: `apiKey` se obtiene de `FOOTBALL_DATA_API_KEY` a través de la propiedad existente; no se copia a DTOs, logs, excepciones ni archivos versionados.

### Synchronization consistency

- **Decisión**: Construir y validar la foto completa del proveedor antes de iniciar la transacción que modifica jugadores locales.
- **Motivo**: Si falla cualquier competición, equipo o plantel, no existe una foto fiable para decidir inactivaciones. Al no ejecutar la fase transaccional, se preserva el catálogo anterior. Si la foto se obtiene correctamente, una transacción única hace atómicos los upserts y las inactivaciones.

### Duplicates

- **Decisión**: Usar el ID de Football-Data.org como PK local y consolidar los candidatos en memoria por ID antes de persistirlos.
- **Motivo**: Previene duplicados tanto frente al proveedor como en la base. Al iterar las competiciones en su orden configurado, el primer candidato conserva la regla funcional de `league`.

### Availability and retries

- **Decisión**: Configurar timeouts y no implementar reintentos automáticos.
- **Motivo**: La Constitution los exige cuando la tecnología lo permite y prohíbe reintentos por defecto. Football-Data.org publica límites de solicitud, por lo que los reintentos aumentarían tráfico sin una necesidad funcional definida. [Políticas de API](https://docs.football-data.org/general/v4/policies.html).

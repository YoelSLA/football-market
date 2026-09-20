# Informe de investigación: Catálogo de jugadores

## Decisiones técnicas

### Fuente y recorrido de Football-Data.org

- **Decisión**: Usar Football-Data.org API v4 a través de una Integration dedicada.
- **Recorrido**: consultar Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`); para cada una, obtener sus equipos y consultar el plantel de cada equipo.
- **Motivo**: La API no ofrece un recurso global de catálogo de jugadores. La documentación oficial expone `GET /competitions/{code}`, `GET /competitions/{code}/teams` y `GET /teams/{id}`; el último contiene `squad` con los jugadores. [Competition](https://docs.football-data.org/general/v4/competition.html), [Team](https://docs.football-data.org/general/v4/team.html).
- **Mapeo**: `competition.name` → `league`; `team.name` → `team`; `squad[].id`, `squad[].name` y `squad[].position` → `id`, `name` y `position`.

### Configuración de competiciones

- **Decisión**: Añadir `football-data.competitions` a `FootballDataProperties` y aceptar cualquier orden del conjunto `PL,BL1,PD,SA,FL1`.
- **Motivo**: El catálogo está limitado siempre a esas cinco ligas. Validar faltantes, adicionales, duplicados durante el arranque evita sincronizaciones parciales y mantiene determinista la selección de `league` ante jugadores repetidos.

### Cliente HTTP y credenciales

- **Decisión**: Configurar un `RestClient` dedicado, basado en las dependencias de Spring Web ya presentes, con URL base, HTTPS, timeouts y el encabezado de autenticación requerido por Football-Data.org.
- **Motivo**: No agrega dependencias, centraliza la configuración del proveedor y permite aislar las respuestas y errores externos. La API documenta `X-Auth-Token` para las solicitudes autenticadas. [Ejemplo Java oficial](https://docs.football-data.org/general/v4/coding/java.html).
- **Seguridad**: `apiKey` se obtiene de `FOOTBALL_DATA_API_KEY` a través de la propiedad existente; no se copia a DTOs, logs, excepciones ni archivos versionados.

### Consistencia de la sincronización

- **Decisión**: Construir y validar la foto completa del proveedor antes de iniciar la transacción que modifica jugadores locales.
- **Motivo**: Si falla cualquier competición, equipo o plantel, no existe una foto fiable para decidir inactivaciones. Al no ejecutar la fase transaccional, se preserva el catálogo anterior. Si la foto se obtiene correctamente, una transacción única hace atómicos los upserts y las inactivaciones.

### Duplicados

- **Decisión**: Usar el ID de Football-Data.org como PK local y consolidar los candidatos en memoria por ID antes de persistirlos.
- **Motivo**: Previene duplicados tanto frente al proveedor como en la base. Al iterar las ligas en el orden configurado para las cinco ligas, el primer candidato conserva la regla funcional de `league`.

### Disponibilidad y reintentos

- **Decisión**: Configurar timeouts y no implementar reintentos automáticos.
- **Motivo**: La Constitution los exige cuando la tecnología lo permite y prohíbe reintentos por defecto. Football-Data.org publica límites de solicitud, por lo que los reintentos aumentarían tráfico sin una necesidad funcional definida. [Políticas de API](https://docs.football-data.org/general/v4/policies.html).

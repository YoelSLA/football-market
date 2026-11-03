# Informe de investigación: Catálogo de jugadores

## Decisiones técnicas

### Identidad del jugador y referencias externas

- **Decisión**: El jugador del catálogo tiene un identificador interno propio, generado localmente por la base. El identificador que asigna Football-Data.org no se usa como clave primaria local, sino como `externalId` de una referencia externa.
- **Modelo**: `Player` es la entidad de dominio; `PlayerExternalReference` representa la identidad que un proveedor externo le asigna a un jugador, con `provider` y `externalId`. La combinación `(provider, externalId)` es única y garantiza la identidad unívoca de la referencia.
- **Motivo**: Acoplar la identidad del dominio a la de un proveedor obliga a redefinir el catálogo cada vez que cambia la fuente. Con identidad interna propia, el mismo jugador puede referenciarse en más de un proveedor sin que su identificador cambie, y las decisiones de la feature no dependen de la numeración de Football-Data.org.
- **Proveedores**: `PlayerProvider` admite `FOOTBALL_DATA` y `THE_SPORTS_DB`. En esta feature solo `FOOTBALL_DATA` tiene operación: TheSportsDB no se consulta, no se busca, no se empareja y no aporta imágenes.

### Migración de la identidad existente

- **Decisión**: Una migración Flyway nueva sustituye `players.id` por una columna generada localmente y crea `player_external_references`, insertando una fila `FOOTBALL_DATA` por jugador existente cuyo `external_id` es el valor que tenía como identificador.
- **Motivo**: La migración ya aplicada no puede modificarse, de modo que la transición debe ser aditiva y reversible por rollback de la propia migración. El identificador numérico anterior no se conserva como identificador interno: solo sobrevive como `externalId`, que es la única información de valor que aporta para el catálogo.
- **Restricción**: La unicidad de `(provider, external_id)` no puede fallar en este paso, porque los valores de origen eran clave primaria y por tanto ya eran únicos.

### Fuente y recorrido de Football-Data.org

- **Decisión**: Usar Football-Data.org API v4 a través de una Integration dedicada.
- **Recorrido**: consultar Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`); para cada una, obtener sus equipos y consultar el plantel de cada equipo.
- **Motivo**: La API no ofrece un recurso global de catálogo de jugadores. La documentación oficial expone `GET /competitions/{code}`, `GET /competitions/{code}/teams` y `GET /teams/{id}`; el último contiene `squad` con los jugadores. [Competition](https://docs.football-data.org/general/v4/competition.html), [Team](https://docs.football-data.org/general/v4/team.html).
- **Mapeo**: `competition.name` → `league`; `team.name` → `team`; `squad[].id` → `externalId` de la referencia `FOOTBALL_DATA`; `squad[].name` y `squad[].position` → `name` y `position`. `squad[].dateOfBirth` y `squad[].nationality` se leen cuando el recurso los informa y se tratan como opcionales.

### Atributos opcionales del jugador

- **Decisión**: `dateOfBirth`, `nationality` e `imageUrl` son atributos opcionales y admiten ausencia de valor. La ausencia, el vacío y el formato no reconocido se interpretan como "dato no informado por la fuente".
- **Motivo**: Las respuestas incompletas o degradadas del proveedor no deben destruir información válida ya conocida ni descartar un jugador por un campo opcional. Un dato no informado nunca sobrescribe un valor previo ni invalida el registro.
- **`imageUrl`**: forma parte del modelo y del contrato público, pero esta feature no lo escribe ni lo resuelve. Su obtención pertenece a otra feature.

### Resolución durante la sincronización

- **Decisión**: Cada candidato se resuelve por su referencia `(FOOTBALL_DATA, externalId)` antes de escribir. Si existe, se actualiza el jugador asociado; si no, se crean conjuntamente el jugador y su referencia dentro de la misma operación lógica.
- **Motivo**: Resolver por referencia evita duplicados entre sincronizaciones y desacopla la persistencia de la numeración del proveedor. El alta conjunta garantiza que no quede un jugador procedente de Football-Data.org sin su referencia.
- **Conflicto de identidad**: si la referencia ya pertenece a otro jugador, el registro se descarta, se registra el motivo y se continúa. No se reasigna la referencia ni se altera ningún jugador.
- **Restricción única**: la base mantiene la constraint inmediata `uk_player_external_references_provider_external_id` sobre `(provider, external_id)`. Solo SQLSTATE `23505` con esa constraint, tabla `player_external_references` y un candidato identificado constituye el conflicto funcional. Otras violaciones de integridad y errores no identificables son fallos técnicos.
- **Aislamiento y recuperación**: ante ese conflicto se revierte el intento completo de aplicación y se reaplica la foto inmutable en una transacción y contexto JPA nuevos, excluyendo la clave conflictiva. La captura ocurre fuera del callback transaccional y después del rollback. No hay commits por jugador ni continuación en una transacción abortada. Cada repetición excluye al menos una clave nueva; no repite consultas HTTP y está acotada por el número de candidatos consolidados.
- **Preservación**: los jugadores implicados se protegen también de la inactivación final, resolviendo de nuevo los propietarios de las claves excluidas. Los contadores de escritura proceden solo del intento confirmado; cada conflicto se registra y cuenta una vez. Los fallos técnicos no se reintentan ni se contabilizan como descartes.

### Configuración de competiciones

- **Decisión**: Añadir `football-data.competitions` a `FootballDataProperties` y aceptar cualquier orden del conjunto `PL,BL1,PD,SA,FL1`.
- **Motivo**: El catálogo está limitado siempre a esas cinco ligas. Validar faltantes, adicionales, duplicados durante el arranque evita sincronizaciones parciales y mantiene determinista la selección de `league` ante jugadores repetidos.

### Cliente HTTP y credenciales

- **Decisión**: Configurar un `RestClient` dedicado, basado en las dependencias de Spring Web ya presentes, con URL base, HTTPS, timeouts y el encabezado de autenticación requerido por Football-Data.org.
- **Motivo**: No agrega dependencias, centraliza la configuración del proveedor y permite aislar las respuestas y errores externos. La API documenta `X-Auth-Token` para las solicitudes autenticadas. [Ejemplo Java oficial](https://docs.football-data.org/general/v4/coding/java.html).
- **Seguridad**: `apiKey` se obtiene de `FOOTBALL_DATA_API_KEY` a través de la propiedad existente; no se copia a DTOs, logs, excepciones ni archivos versionados.

### Consistencia de la sincronización

- **Decisión**: Construir y validar la foto completa del proveedor antes de iniciar la transacción que modifica jugadores locales.
- **Motivo**: Si falla cualquier competición, equipo o plantel, no existe una foto fiable para decidir inactivaciones y no se inicia ninguna escritura local. La aplicación tiene un único commit exitoso: los intentos fallidos se revierten completamente y solo el intento final confirma las altas conjuntas, actualizaciones e inactivaciones. Se elige reaplicar escrituras no confirmadas frente a commits independientes por jugador, que romperían el rollback global. El coste adicional solo aparece ante conflictos de persistencia; los huecos de secuencias tras rollback son admisibles.

### Duplicados

- **Decisión**: Consolidar los candidatos en memoria por identificador externo de Football-Data.org antes de persistirlos, y resolverlos en la base por su referencia externa.
- **Motivo**: La consolidación en memoria evita aplicar el mismo jugador cuando aparece en más de una liga; la resolución por referencia evita duplicar el jugador entre sincronizaciones. Al iterar las ligas en el orden configurado para las cinco ligas, el primer candidato conserva la regla funcional de `league`.

### Disponibilidad y reintentos

- **Decisión**: Configurar timeouts y no reintentar errores HTTP o de comunicación. La única excepción es `429 Too Many Requests`, para la cual se respeta la cabecera `Retry-After` del proveedor y se realizan como máximo tres reintentos; si tras ellos la operación no se completa, se propaga el fallo correspondiente.
- **Motivo**: La Constitution exige reintentos cuando la tecnología lo permite y prohíbe reintentarlos por defecto. El proveedor publica límites de solicitud por lo que reintentar ante `429` es la conducta correcta y acotada, y hace innecesario reintentar cualquier otro fallo. Los reintentos también cuentan como peticiones. [Políticas de API](https://docs.football-data.org/general/v4/policies.html).

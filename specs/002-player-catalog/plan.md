# Implementation Plan: Player Catalog

**Branch**: `002-player-catalog` | **Date**: 2026-09-17 | **Última actualización**: 2026-09-30 | **Spec**: [spec.md](spec.md)

**Input**: Especificación funcional validada y aclarada en `specs/002-player-catalog/spec.md`.

## Ajustes de implementación — 2026-10-01

- La foto transporta `models/records/PlayerCandidate` inmutables (`externalId` y datos), no entidades `Player`; `PlayerSnapshot` conserva esos candidatos entre intentos sin reutilizar estado JPA.
- `Player.id` y la PK de la referencia usan secuencias con `allocationSize=1`, alineadas por V3 para catálogo poblado o vacío. V2 permanece intacta.
- Se desactiva `spring.jpa.open-in-view`, incluido el override de desarrollo, y se fija batching JDBC a cero para delimitar contexto y flush por candidato.
- pgJDBC conserva su versión administrada; pasa de runtime a compilación para consultar los diagnósticos estructurados de `PSQLException`. No se agrega otra librería.
- El preflight toma una proyección escalar de propietarios y resuelve las referencias antes de escribir. Detecta cambios incompatibles de asociación; una referencia existente normal actualiza a su propietario, sin comparar nombres o equipos para hacer matching.
- Los tests técnicos de Flyway/constraints/concurrencia se ubican en `repositories/PlayerMigrationTest.java` y `repositories/PlayerCatalogPersistenceTest.java`, conforme a las categorías vigentes de testing. Los tests funcionales de Service usan exclusivamente su API; el aislamiento técnico de los commits independientes se prepara en `support/ResetPlayerCatalogListener.java`.
- El código ya incluía tres reintentos de 429 y fallback de 60 segundos. Se conserva esa política y se completa soporte de fecha HTTP y control de reloj/espera para tests deterministas.
- Implementación y fuentes de tests preparadas; ninguna ejecución de tests, build, formato ni generación de snippets realizada por el agente. Controles T036, T041 y T065 pendientes del usuario.

## Summary

Implementar el catálogo local de jugadores y sus dos contratos HTTP: `GET /api/players`, que consulta exclusivamente PostgreSQL con paginación, y `POST /api/players/sync`, que inicia una sincronización manual con Football-Data.org API v4.

El jugador del catálogo tiene identidad interna propia de FootballMarket, independiente de cualquier proveedor. Las identidades de fuentes externas se modelan en una entidad separada `PlayerExternalReference` (jugador + proveedor + identificador externo), con unicidad de `(provider, externalId)`. Football-Data.org es el único proveedor que participa de la sincronización en esta feature; el modelo admite `THE_SPORTS_DB` sin ninguna integración operativa.

La solución incorpora la funcionalidad en las capas existentes: Controller → DTO/Mapper → Service u Orchestrator → Model → Repository, y encapsula Football-Data.org en una `Integration`. La sincronización resuelve cada jugador por su referencia `FOOTBALL_DATA`, crea el jugador y su referencia de forma conjunta cuando no existe, y primero construye y valida una foto completa desde el proveedor para después aplicarla dentro de una transacción. Así, un fallo parcial del proveedor no puede inactivar ni eliminar datos locales.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web, Spring Data JPA, Spring Security, Jakarta Validation, Flyway, SpringDoc OpenAPI y PostgreSQL JDBC Driver.

**Storage**: PostgreSQL administrado mediante Flyway; Hibernate valida el esquema existente.

**External system**: Football-Data.org API v4, con URL base y clave ya enlazadas a `FootballDataProperties` desde configuración.

**Testing**: JUnit Jupiter, Spring Boot Test, MockMvc, Mockito, AssertJ y Testcontainers/PostgreSQL. Los tests nuevos usarán explícitamente el perfil `test`.

**Scope**: Solo catálogo de jugadores, identidad interna y sus referencias externas, consulta paginada y sincronización manual con Football-Data.org. No se crearán jobs, filtros, búsquedas, ordenamiento solicitado por cliente, detalle individual, frontend ni integraciones con otros proveedores. TheSportsDB permanece fuera del alcance operativo: se modela como proveedor admitido sin llamadas, búsqueda, matching ni resolución de imágenes.

## Constitution Check

*GATE: aprobado antes de investigación y confirmado después del diseño.*

- **Arquitectura por capas**: PASS — `PlayerController` solo atiende HTTP; `PlayerSynchronizationOrchestrator` coordina los dos servicios; los Services contienen la lógica de aplicación; `PlayerRepository` concentra la persistencia; `FootballDataIntegration` concentra la comunicación externa.
- **DTO y Mapper**: PASS — los DTO HTTP serán `record`; `PlayerMapper` será `final`, sin estado, con constructor privado y métodos `static`. Las entidades no se expondrán directamente.
- **Integración externa**: PASS — las rutas, respuestas, autenticación y fallos de Football-Data.org quedan aislados en `Integration`; el Controller y el Model no conocerán HTTP externo.
- **Persistencia e invariantes**: PASS — el estado del jugador será encapsulado por el Model; el Controller no accederá al Repository. Flyway añadirá el esquema requerido.
- **Seguridad**: PASS — la clave se leerá de configuración, no se registrará ni se incluirá en errores. Los endpoints conservarán la autenticación vigente; cualquier usuario autenticado podrá invocar `POST /api/players/sync` sin un rol adicional.
- **Contratos y documentación**: PASS — las respuestas exitosas usarán DTOs y los errores gestionados por la aplicación usarán `ErrorResponseDTO`; los contratos se documentarán en OpenAPI.
- **Testing**: PASS — habrá pruebas por responsabilidad y pruebas de integración con PostgreSQL; los proveedores externos serán aislados en tests. Los tests de Controller documentarán con Spring REST Docs los endpoints y los casos de respuesta verificados.

## Project Structure

```text
specs/002-player-catalog/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── api.md

backend/src/main/java/footballmarket/
├── config/
│   ├── FootballDataProperties.java              # validar las cinco ligas obligatorias
│   └── FootballDataClientConfig.java            # RestClient del proveedor y timeouts
├── controllers/
│   ├── PlayerController.java
│   ├── dtos/responses/
│   │   ├── PlayerResponseDTO.java
│   │   ├── PlayersPageResponseDTO.java
│   │   └── PlayerSyncResponseDTO.java
│   ├── mappers/PlayerMapper.java
│   └── exceptions/                              # manejo HTTP de paginación
├── integrations/
│   ├── FootballDataIntegration.java             # único punto de salida a Football-Data.org
│   ├── exceptions/                              # FootballDataUnavailable, FootballDataRateLimit, etc.
│   └── [records de respuesta externa privados de la integración]
├── orchestrators/
│   └── PlayerSynchronizationOrchestrator.java
├── models/
│   ├── Player.java
│   ├── PlayerExternalReference.java
│   ├── PlayerProvider.java
│   ├── records/PlayerSynchronizationResult.java
│   ├── exceptions/                              # invariantes de dominio
│   ├── records/PlayerSnapshot.java
│   └── records/PlayerCandidate.java
├── repositories/
│   ├── PlayerRepository.java
│   └── PlayerExternalReferenceRepository.java
├── services/
│   ├── FootballDataPlayerService.java            # contrato del servicio
│   ├── PlayerCatalogService.java                 # contrato del servicio
│   └── impl/                                     # implementaciones de los servicios del catálogo
└── resources/
    └── db/migration/
        ├── V2__create_players_table.sql         # existente, sin modificar
        └── V3__decouple_player_external_identity.sql

backend/src/test/java/footballmarket/
├── models/PlayerExternalReferenceTest.java
├── controllers/PlayerControllerTest.java
├── integrations/FootballDataIntegrationTest.java
├── services/PlayerCatalogServiceTest.java
├── services/FootballDataPlayerServiceTest.java
└── repositories/
    ├── PlayerMigrationTest.java
    └── PlayerCatalogPersistenceTest.java
```

**Structure Decision**: Se conserva la estructura física y las responsabilidades de la Constitution. El Orchestrator está justificado porque coordina una obtención externa y la aplicación transaccional local, sin depender directamente de Repository ni Integration.

## Design

### Persistencia y modelo

`Player` será una entidad JPA en `models` con los siguientes atributos:

| Atributo | Tipo técnico | Regla |
|---|---|---|
| `id` | `Long` | Clave primaria con generación local por secuencia. Es la identidad interna de FootballMarket y no proviene de ningún proveedor. |
| `name` | `String` | Obligatorio y no vacío. |
| `team` | `String` | Obligatorio y no vacío. |
| `league` | `String` | Obligatorio y no vacío. |
| `position` | `String` | Obligatorio y no vacío. |
| `active` | `boolean` | Obligatorio; define la visibilidad en el catálogo. |
| `dateOfBirth` | `LocalDate` | Opcional y nullable. No informa la presencia o ausencia del dato. |
| `nationality` | `String` | Opcional y nullable. |
| `imageUrl` | `String` | Opcional y nullable. Esta feature no lo escribe ni lo resuelve. |

La entidad no usará `Builder` ni `@Setter`. Expondrá operaciones de dominio para actualizar los datos obligatorios desde una fuente válida, aplicar de forma independiente los atributos opcionales cuando la fuente los informe con valor válido, activar y desactivar. La ausencia, el vacío o un formato no reconocible en `dateOfBirth` o `nationality` no constituye motivo de descarte ni operación de escritura sobre esos atributos.

`PlayerExternalReference` será una entidad JPA en `models` con:

| Atributo | Tipo técnico | Regla |
|---|---|---|
| `id` | `Long` | Clave primaria con generación local por secuencia. |
| `player` | `Player` | Obligatorio. Relación `ManyToOne` con la carga diferida. |
| `provider` | `PlayerProvider` | Obligatorio, enumerado y persistido como `VARCHAR`. |
| `externalId` | `String` | Obligatorio y no vacío. Identificador asignado por el proveedor. |

`PlayerProvider` será un `enum` en `models` con `FOOTBALL_DATA` y `THE_SPORTS_DB`, de modo que el mismo jugador pueda referenciarse en más de un proveedor sin cambiar su identidad interna. Esta feature no ejecuta ninguna operación sobre referencias `THE_SPORTS_DB`.

Las relaciones se mapean con `Player.externalReferences` como `OneToMany` en cascada y con eliminación en cascada, de modo que la persistencia de un jugador incluye sus referencias. `PlayerExternalReference` no tendrá operaciones de dominio propias: su ciclo de vida está ligado al del jugador y su unicidad la garantiza la base.

Restricciones de persistencia:

- `player_external_references` tendrá una restricción única sobre `(provider, external_id)`, que garantiza la identidad unívoca de la referencia y su resolución inequívoca por `(provider, externalId)`.
- Un índice sobre `player_id` acompañará a la única referencia para resolver las cargas diferidas y la inactivación por referencia.
- La columna `provider` se persiste como `VARCHAR` en lugar de ordinal, para que agregar proveedores futuros no requiera reescribir filas existentes.
- `external_id` se persiste como `VARCHAR` porque la identidad del proveedor es un dato externo, no un número del dominio. Football-Data.org entrega un identificador numérico que se representa como su representación textual.
- No se presupone índice adicional sobre el campo booleano `active`; su necesidad se evaluará con datos y planes de consulta reales.

`PlayerRepository` extenderá `JpaRepository<Player, Long>` y expondrá consultas para páginas de jugadores activos y para recuperar los jugadores activos que deban inactivarse. `PlayerExternalReferenceRepository` extenderá `JpaRepository<PlayerExternalReference, Long>` y expondrá la búsqueda por `(provider, externalId)` usada para resolver cada jugador durante la sincronización. Ninguno de los dos contendrá reglas de negocio.

### Migración de datos existentes

`V2__create_players_table.sql` ya está aplicada con `players.id` como clave primaria sin generación y con el identificador de Football-Data.org. Como `V2` no puede modificarse sin romper Flyway, la transición se implementa en una migración nueva `V3__decouple_player_external_identity.sql` que ejecuta, en orden:

1. Añadir a `players` las columnas nullable `date_of_birth`, `nationality` e `image_url`.
2. Sustituir `players.id` por una columna `BIGINT` con generación por identidad o secuencia, conservando temporalmente el valor anterior como columna auxiliar `legacy_external_id`.
3. Crear `player_external_references` con su clave primaria, `player_id` no nulo con clave foránea a `players.id` y botón de borrado en cascada, y la restricción única `(provider, external_id)`.
4. Crear el índice sobre `player_id`.
5. Poblar `player_external_references` con una fila por jugador existente: `player_id` igual al identificador interno vigente en ese momento, `provider = 'FOOTBALL_DATA'` y `external_id` tomado de `legacy_external_id`.
6. Eliminar la columna auxiliar `legacy_external_id`.
7. Ajustar la secuencia al valor máximo de `players.id` para que los próximos jugadores creen el flujo generen identificadores válidos.

La migración preserva la correspondencia uno a uno entre cada jugador existente y su referencia `FOOTBALL_DATA`, no crea ni elimina jugadores y reasigna el identificador interno conforme a RF-029: el valor numérico anterior no se conserva como identificador interno, solo como `externalId`. La restricción única `(provider, external_id)` no puede fallar en este paso porque `players.id` era clave primaria y por tanto sus valores ya eran únicos.

Esta migración cambia los valores de la columna `id` que `GET /api/players` expone. Es un cambio de contrato deliberado y aceptado por la spec vigente; no se preserva el valor numérico previo.

### Contratos HTTP y mapeo

- `PlayerResponseDTO`: `id` (identificador interno de FootballMarket), `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality`, `imageUrl`. No expone `active` ni ninguna referencia externa.
- `PlayersPageResponseDTO`: `content`, `page`, `size`, `totalElements`, `totalPages`.
- `PlayerSyncResponseDTO`: resultado de la sincronización y contadores `obtained`, `created`, `updated`, `markedInactive`, `discardedInvalid`.
- `PlayerSynchronizationResult`: Model de aplicación usado entre Services/Orchestrator y convertido al DTO de sincronización por el Mapper.

`PlayerMapper` transformará únicamente `Player` y resultados de sincronización a sus DTOs. El Controller no construirá ni inspeccionará entidades. El mapeo de `Player` a `PlayerResponseDTO` copiará los atributos de datos y los tres opcionales, resueltos a `null` cuando no tienen valor, y omitirá deliberadamente las referencias externas y `active`, de modo que el modelo de proveedor no quede expuesto por el contrato público.

### `GET /api/players`

`PlayerController` recibirá `page` y `size` como parámetros de consulta, con valores por defecto 0 y 20. Validará el contrato estructural: `page >= 0` y `1 <= size <= 100`; ante incumplimiento lanzará una excepción de presentación manejada como `400 Bad Request` con mensaje seguro en español.

El Controller delegará en `PlayerCatalogService.getActivePlayers(page, size)`. El servicio consultará exclusivamente `PlayerRepository` usando paginación y el predicado `active = true`; nunca invocará `FootballDataIntegration`. El Mapper devolverá el DTO paginado. Una página válida sin jugadores devuelve `200 OK` y metadatos con contenido vacío.

### `POST /api/players/sync`

`PlayerController` delegará en `PlayerSynchronizationOrchestrator.synchronize()`. No recibirá cuerpo de petición ni disparará trabajo automático.

El Orchestrator seguirá este flujo:

1. Solicita a `FootballDataPlayerService` una foto validada de jugadores de las cinco ligas obligatorias, según el orden configurado.
2. Si la obtención completa falla, propaga el error de proveedor y no invoca la aplicación local de cambios.
3. Si la foto está completa, delega en `PlayerCatalogService.applySynchronization(...)`.
4. Devuelve el resultado del Service para que el Controller lo convierta al DTO HTTP.

`PlayerCatalogService.applySynchronization(...)` coordinará intentos transaccionales de aplicación de la foto y realizará el upsert por referencia externa `FOOTBALL_DATA`:

- busca la referencia `(FOOTBALL_DATA, externalId)` del candidato recibido;
- si existe, actualiza los datos obligatorios y los opcionales informados con valor válido del jugador asociado, y lo activa, incluso si estaba inactivo;
- si no existe, crea el jugador con identidad interna propia y, en la misma operación lógica, su referencia `FOOTBALL_DATA`. Si la referencia no puede persistirse, el alta completa se revierte y el jugador no queda registrado;
- una vez procesada la foto completa, desactiva a los jugadores que siguen activos cuya referencia `FOOTBALL_DATA` no aparece en ella; un jugador sin referencia `FOOTBALL_DATA` no se inactiva en esta feature;
- nunca elimina jugadores ni referencias externas.

Un candidato cuyo `(FOOTBALL_DATA, externalId)` ya estuviera asociado a otro jugador se descarta por conflicto de identidad: no reasigna la referencia, no altera ninguno de los jugadores implicados, se registra el motivo y aumenta `discardedInvalid`.

La migración de `V3` y las altas de la sincronización comparten la misma invariante: toda fila de `player_external_references` apunta a un jugador existente y el par `(provider, external_id)` es único.

#### Estrategia ante conflictos de identidad

La semántica de la spec exige tratar un conflicto real de `(provider, external_id)` como registro inválido y continuar con los demás. Un flush fallido inutiliza el intento transaccional y su contexto JPA: el error debe salir del callback para completar el rollback antes de clasificarlo. La recuperación se realiza fuera de esa transacción, nunca capturando el error para continuar escribiendo dentro de ella.

La resolución se organiza en tres capas:

1. **Resolución previa.** Cada candidato se resuelve por `(FOOTBALL_DATA, externalId)` antes de escribir, en modo lectura dentro de la misma unidad transaccional. La existencia de la referencia determina el camino de actualización o de alta conjunta.
2. **Detección lógica previa a la persistencia.** El camino de alta solo se toma cuando la consulta del punto 1 confirmó que la referencia no existe. Como la foto se consolidó por identificador externo en la obtención, dos candidatos de la misma ejecución no compiten por la misma referencia, y un conflicto detectado en este punto se registra como descarte del registro sin intentar escribirlo. No existe camino de escritura que pueda producir deliberadamente un conflicto.
3. **Restricción `UNIQUE` en la base como garantía final.** La base mantiene `(provider, external_id)` único e inmutable. Si a pesar de lo anterior la base rechazara una escritura por esa restricción, el error se interpreta exclusivamente como el conflicto funcional de la spec: no reasigna la referencia, no altera los jugadores implicados y no invalida el resto del trabajo válido.

**Aislamiento elegido: rollback del intento y reaplicación de la foto.** Para conservar la atomicidad global con JPA, no se confirman transacciones independientes por jugador. La unidad física de rollback es un intento completo de aplicación; la unidad lógica descartada es el candidato conflictivo. Los cambios válidos de un intento revertido se reaplican desde la foto inmutable y solo el intento final confirma sus escrituras. No se vuelven a realizar llamadas HTTP.

La implementación de `PlayerCatalogService` coordinará un `TransactionTemplate` con propagación `REQUIRES_NEW` por intento, sin una transacción exterior de escritura. Cada intento tendrá su propio contexto de persistencia transaccional; las entidades cargadas o creadas en un intento fallido no se reutilizarán. No se usará un contexto JPA extendido o ligado a la petición para estos intentos. La captura y clasificación del error ocurrirán fuera de `TransactionTemplate.execute`, después de que el gestor transaccional haya completado el rollback. No se usará `noRollbackFor` ni se continuará dentro del callback fallido.

Antes de escribir cada candidato se resuelve su referencia. Una referencia existente es el camino normal de actualización, no evidencia de conflicto; cambios de nombre, equipo o posición tampoco lo son. Si se detecta un intento de asociar esa referencia a un jugador interno distinto, se descarta antes de mutar entidades. Después de aplicar cada candidato se realiza un flush dentro del intento, sin batching que mezcle candidatos: esto permite atribuir una eventual violación a la clave que se estaba procesando. La UNIQUE será inmediata, no diferible, y tendrá el nombre estable `uk_player_external_references_provider_external_id` en V3.

Solo se reconoce el conflicto de persistencia si la cadena de causas confirma PostgreSQL SQLSTATE `23505`, tabla `player_external_references` y constraint `uk_player_external_references_provider_external_id`, y se conoce el candidato cuyo flush falló. No alcanza con `DataIntegrityViolationException`, SQLSTATE aislado ni coincidencias de texto en mensajes. Una violación de otra UNIQUE, PK, FK, NOT NULL o cualquier error no identificable se propaga como fallo técnico, sin incrementar `discardedInvalid`.

Tras el rollback de un conflicto reconocido, se incorpora su clave a un conjunto de exclusiones de la ejecución, se registra el motivo una sola vez y se inicia un nuevo intento sobre la misma foto sin ese candidato. Cada repetición debe excluir al menos una clave nueva: con N candidatos consolidados hay como máximo N reaplicaciones por este motivo. No se reintentan fallos técnicos. Los contadores de escrituras se reinician en cada intento; solo se publican los del commit final, sumando una vez los descartes por conflicto. El resumen exitoso y sus logs se emiten después del commit.

Los jugadores implicados en un conflicto quedan protegidos también frente a la inactivación final. En cada nuevo intento se resuelven las claves excluidas para proteger a su propietario actual; se conservan además los IDs internos implicados en conflictos lógicos. La inactivación excluye ese conjunto y no considera una referencia conflictiva como simplemente ausente. Esa protección es específica de RF-013: los demás registros inválidos siguen las reglas generales de ausencia. No se reasignan referencias ni se sobrescriben cambios de otra transacción.

Esta distinción es deliberada: solo una violación identificable de la invariante única `(provider, external_id)` corresponde al conflicto funcional definido por la spec. Cualquier otra violación de integridad o fallo de persistencia conserva su tratamiento como fallo técnico y revierte la transacción, sin convertirse en registro inválido ni quedar registrado en `discardedInvalid`. No se generaliza la regla de descarte a `DataIntegrityViolationException` ni a errores de base de datos.

La fase de llamadas remotas sucede antes de abrir la transacción de escritura. Cada intento aplica la foto dentro de una única transacción y la ejecución tiene un único commit exitoso. Un fallo técnico revierte todas las altas, referencias, actualizaciones e inactivaciones del intento; los intentos anteriores ya fueron revertidos. Una foto incompleta nunca inicia la aplicación local. Los avances de secuencias no son transaccionales y pueden dejar huecos, sin dejar jugadores ni referencias huérfanos. La serialización de sincronizaciones en la instancia única abarca todos los intentos. No elimina la necesidad de tratar una carrera detectada por la UNIQUE. Un despliegue con varias instancias requiere definir coordinación antes de habilitarlo.

Los contadores tendrán una semántica única para logs y respuesta: `obtained` cuenta los integrantes de planteles leídos antes de validar y consolidar; `discardedInvalid` cuenta los que no cumplen los campos obligatorios ni los que presentan conflicto de identidad; `created` cuenta referencias `FOOTBALL_DATA` inexistentes, con su jugador y su referencia creados conjuntamente; `updated` cuenta referencias ya existentes procesadas, incluidas reactivaciones; y `markedInactive` cuenta únicamente cambios efectivos de activo a inactivo. Un jugador repetido y válido se consolida por identificador externo y no incrementa `created` ni `updated` más de una vez.

### Integración Football-Data.org

`FootballDataIntegration` será el único componente que conozca los recursos y los JSON del proveedor. Usará un `RestClient` configurado con la URL base de `FootballDataProperties`, HTTPS, timeouts de conexión y lectura, y la clave configurada mediante `FOOTBALL_DATA_API_KEY` en el encabezado requerido por Football-Data.org.

Ante una respuesta `429 Too Many Requests` se respetará la cabecera `Retry-After` del proveedor y se realizarán como máximo tres reintentos; si tras ellos la operación no se completa, se propagará el fallo correspondiente como indisponibilidad de la fuente. Ningún otro error HTTP ni de comunicación se reintentará. El reintento se resolverá en la capa `integrations/`, sin dependencias de Spring adicionales ni mecanismos globales de reintento que alteren el resto de la aplicación.

Para cada código de competición, respetando el orden configurado para las cinco ligas, la integración realizará:

1. `GET /competitions/{competitionCode}` para obtener el nombre de la competición, que alimenta `league`.
2. `GET /competitions/{competitionCode}/teams` para obtener sus equipos.
3. Para cada equipo, `GET /teams/{teamId}` para obtener el plantel `squad`.
4. Para cada integrante de `squad`, extraerá `id`, `name`, `position`, `dateOfBirth` y `nationality` cuando el proveedor los informe; combinará esos valores con `team.name` y `competition.name` para producir el candidato de catálogo. `dateOfBirth` y `nationality` ausentes, vacíos o con formato no reconocible se normalizarán a "no informado" en el candidato, sin descartar el registro; ningún campo de imagen se extraerá ni escribirá.

La documentación oficial confirma que el recurso de competición expone `name`, que su subrecurso de equipos existe y que el recurso de equipo expone `name` y `squad`, cuyos integrantes incluyen `id`, `name` y `position`. [Competition](https://docs.football-data.org/general/v4/competition.html), [Team](https://docs.football-data.org/general/v4/team.html). `dateOfBirth` y `nationality` se leerán solo si el recurso de jugador los expone; ante cualquier otra forma, el candidato se resolverá como "no informado" conforme a RF-018.

La configuración ampliará `FootballDataProperties` con una lista obligatoria `competitions`, vinculada a `football-data.competitions`, que debe contener `PL,BL1,PD,SA,FL1` en cualquier orden. La aplicación validará al iniciar que estén exactamente esos cinco códigos, sin faltantes, adicionales, duplicados; se acepta cualquier orden; una configuración inválida impedirá el arranque y, por lo tanto, cualquier sincronización parcial. La configuración actual de `apiKey` y `baseUrl` se conserva.

La Integration validará toda respuesta externa antes de entregarla: ausencia o valor vacío del identificador externo, `name`, `team`, `league` o `position` descarta solo ese candidato, registra un motivo seguro y aumenta `discardedInvalid`. La ausencia o el formato no reconocible de `dateOfBirth` o `nationality` nunca descarta un candidato. Si una de las cinco ligas, un equipo o un plantel no se puede obtener, o el proveedor responde un estado no exitoso o una estructura inválida, se aborta toda la foto y se lanza una excepción técnica de integración. `FootballDataPlayerService` consolidará los candidatos válidos por identificador externo de Football-Data.org usando el primero encontrado: al recorrer las ligas en el orden configurado, esa regla preserva el `league` requerido cuando el mismo jugador aparezca en más de una competición.

### Errores, logs y seguridad

- Se añadirán excepciones de presentación para paginación inválida y de integración para indisponibilidad o respuesta inválida del proveedor. `GlobalExceptionHandler` incorporará manejadores acotados para devolver `400 Bad Request` y `502 Bad Gateway`, respectivamente, mediante `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`; el código HTTP coincidirá con `status` y `path` identificará la ruta solicitada. Los mensajes serán seguros y estarán en español, sin detalles de infraestructura.
- `POST /api/players/sync` devuelve `200 OK` con su resumen cuando se completa y `502 Bad Gateway` ante un fallo de Football-Data.org. El body de error no contendrá URL completa, clave, headers ni respuesta cruda del proveedor. Un `429` agotado tras los reintentos produce el mismo tratamiento de indisponibilidad de la fuente.
- Se usarán logs de Spring para inicio, finalización, contadores de sincronización, descartes con su motivo y fallos de comunicación. No se registra `apiKey`, `X-Auth-Token` ni credenciales.
- `SecurityConfig` no se modificará para esta feature. Los endpoints heredan la autenticación JWT vigente. Cualquier usuario autenticado podrá invocar `POST /api/players/sync` sin un rol o permiso adicional; una solicitud sin autenticación válida recibirá `401 Unauthorized` antes de iniciar la sincronización.

### OpenAPI

`PlayerController` utilizará anotaciones SpringDoc para documentar:

- `GET /api/players`: propósito, `page` y `size`, valores por defecto, límites, DTO de página, autenticación y respuestas `200`/`400`/`401`.
- `POST /api/players/sync`: propósito, ausencia de body, autenticación sin rol adicional, DTO de resultado y respuestas `200`/`401`/`502`.
- Los DTOs: significado, obligatoriedad y ejemplos de cada campo expuesto, indicando que `id` es el identificador interno de FootballMarket y que `dateOfBirth`, `nationality` e `imageUrl` son opcionales y pueden venir ausentes.
- Los errores: formato seguro y códigos aplicables.

## Testing Strategy

| Capa | Pruebas planificadas |
|---|---|
| Model | Invariantes de campos obligatorios, actualización, activación y desactivación de `Player`; aplicación de atributos opcionales solo cuando la fuente informa un valor válido, sin borrar valores previos ante datos ausentes o no reconocidos. |
| Mapper | Conversión de `Player`, página y resultado de sincronización a DTOs, sin exposición de `active` ni de referencias externas, y con los opcionales resueltos a `null` cuando no tienen valor. |
| Integration | Mapeo de competición/equipos/planteles, lectura de `dateOfBirth` y `nationality` como opcionales, normalización de valores no informados, omisión de registros incompletos y conversión de fallos HTTP, timeout o respuesta inválida a error de integración. Las respuestas externas se simularán; no se llamará a Football-Data.org real ni a ningún otro proveedor. |
| Services | Consulta solo de activos, defaults recibidos desde Controller, resolución previa por referencia `FOOTBALL_DATA`, alta conjunta de jugador y referencia, actualización sin duplicado, conservación de opcionales ante fuente degradada, inactivación por ausencia de referencia, reactivación, descarte por conflicto lógico detectado antes de escribir sin reasignar referencias, contadores y ausencia de mutaciones si la foto externa falla. Se verificará que un fallo de integridad ajeno a la invariante `(provider, external_id)` revierte la transacción y no se contabiliza como registro inválido. |
| Controller | `GET /api/players` con página válida, catálogo vacío, defaults y cada límite inválido; `POST /api/players/sync` exitoso, con proveedor no disponible, con cualquier usuario autenticado y sin autenticación válida; contrato JSON, códigos y documentación de los casos verificados mediante Spring REST Docs. |
| Integración | Con Testcontainers/PostgreSQL y Flyway: aplicación de la migración `V3` sobre datos preexistentes conservando la correspondencia jugador-referencia y sin duplicados, generación de identificadores internos tras la migración, restricción única de `(provider, external_id)`, reversión conjunta del alta de jugador y referencia, consulta paginada exclusiva de activos, y aplicación transaccional de una foto completa frente a fallo previo a la aplicación. |

Las pruebas con contexto/persistencia usan el perfil `test` y PostgreSQL Testcontainers con Flyway. Los tests de Service mantienen Repository y DB reales; solo sustituyen Integration cuando corresponde. Las pruebas puras de Model, Orchestrator e Integration autocontenida no usan perfil. Los tests HTTP generan snippets cuando los ejecute el usuario. Las pruebas de persistencia/concurrencia utilizan SQL y barreras exclusivamente sobre el contenedor de testing.

## Files Affected

| Área | Acción planificada |
|---|---|
| `config/FootballDataProperties.java` | Validar el conjunto de ligas `PL,BL1,PD,SA,FL1`. |
| `config/FootballDataClientConfig.java` | Crear el cliente HTTP seguro y con timeouts para la Integration. |
| `models`, `repositories`, `services`, `integrations`, `controllers` | Añadir los componentes descritos, incluyendo `PlayerExternalReference`, `PlayerProvider` y su repositorio, respetando responsabilidades y dependencias de la Constitution. |
| `controllers/exceptions/GlobalExceptionHandler.java` | Añadir manejo seguro de los errores propios del catálogo sin alterar reglas de negocio. |
| `resources/db/migration/V3__decouple_player_external_identity.sql` | Desacoplar la identidad: generar `players.id` localmente, añadir los atributos opcionales y crear `player_external_references` con su restricción única, migrando los identificadores previos como `externalId` `FOOTBALL_DATA`. `V2` no se modifica. |
| `resources/application.properties` y configuración de prueba | Declarar exactamente `PL,BL1,PD,SA,FL1`, sin incorporar credenciales al repositorio. |

| `src/test` | Añadir las pruebas unitarias, de Controller, de Integration y de persistencia definidas. |

## Complexity Tracking

No se introducen nuevas dependencias ni arquitecturas paralelas. El Orchestrator y la Integration responden a responsabilidades explícitas de coordinación y comunicación externa requeridas por la Constitution.

`PlayerExternalReference` no incorpora una capa de servicio propia: su escritura ocurre únicamente dentro del alta y la actualización de jugadores, y su lectura únicamente dentro de la resolución de la sincronización. No se anticipa `PlayerProvider` más allá de los dos proveedores ya decididos, ni se preparan operaciones para TheSportsDB en esta feature.

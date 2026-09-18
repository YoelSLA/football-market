# Implementation Plan: Player Catalog

**Branch**: `002-player-catalog` | **Date**: 2026-09-17 | **Spec**: [spec.md](spec.md)

**Input**: Especificación funcional validada en `specs/002-player-catalog/spec.md`.

## Summary

Implementar el catálogo local de jugadores y sus dos contratos HTTP: `GET /players`, que consulta exclusivamente PostgreSQL con paginación, y `POST /players/sync`, que inicia una sincronización manual con Football-Data.org API v4.

La solución incorpora la funcionalidad en las capas existentes: Controller → DTO/Mapper → Service u Orchestrator → Model → Repository, y encapsula Football-Data.org en una `Integration`. La sincronización primero construye y valida una foto completa desde el proveedor y solo después actualiza la base local dentro de una transacción. Así, un fallo parcial del proveedor no puede inactivar ni eliminar datos locales.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web, Spring Data JPA, Spring Security, Jakarta Validation, Flyway, SpringDoc OpenAPI y PostgreSQL JDBC Driver.

**Storage**: PostgreSQL administrado mediante Flyway; Hibernate valida el esquema existente.

**External system**: Football-Data.org API v4, con URL base y clave ya enlazadas a `FootballDataProperties` desde configuración.

**Testing**: JUnit Jupiter, Spring Boot Test, MockMvc, Mockito, AssertJ y Testcontainers/PostgreSQL. Los tests nuevos usarán explícitamente el perfil `test`.

**Scope**: Solo catálogo de jugadores, consulta paginada y sincronización manual. No se crearán jobs, filtros, búsquedas, ordenamiento solicitado por cliente, detalle individual, frontend ni integraciones adicionales.

## Constitution Check

*GATE: aprobado antes de investigación y confirmado después del diseño.*

- **Arquitectura por capas**: PASS — `PlayerController` solo atiende HTTP; `PlayerSynchronizationOrchestrator` coordina los dos servicios; los Services contienen la lógica de aplicación; `PlayerRepository` concentra la persistencia; `FootballDataIntegration` concentra la comunicación externa.
- **DTO y Mapper**: PASS — los DTO HTTP serán `record`; `PlayerMapper` será `final`, sin estado, con constructor privado y métodos `static`. Las entidades no se expondrán directamente.
- **Integración externa**: PASS — las rutas, respuestas, autenticación y fallos de Football-Data.org quedan aislados en `Integration`; el Controller y el Model no conocerán HTTP externo.
- **Persistencia e invariantes**: PASS — el estado del jugador será encapsulado por el Model; el Controller no accederá al Repository. Flyway añadirá el esquema requerido.
- **Seguridad**: PASS — la clave se leerá de configuración, no se registrará ni se incluirá en errores. Los endpoints conservarán la autenticación vigente; cualquier usuario autenticado podrá invocar `POST /players/sync` sin un rol adicional.
- **Contratos y documentación**: PASS — las respuestas exitosas usarán DTOs y los errores gestionados por la aplicación usarán `ErrorResponseDTO`; los contratos se documentarán en OpenAPI y los dos endpoints se incorporarán a la colección Postman versionada.
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
│   ├── FootballDataProperties.java              # ampliar con competiciones configuradas
│   └── FootballDataClientConfig.java            # RestClient del proveedor y timeouts
├── controllers/
│   ├── PlayerController.java
│   ├── dtos/responses/
│   │   ├── PlayerResponseDTO.java
│   │   ├── PlayersPageResponseDTO.java
│   │   └── PlayerSyncResponseDTO.java
│   ├── mappers/PlayerMapper.java
│   └── exceptions/                              # manejo HTTP de paginación y proveedor
├── integrations/footballdata/
│   ├── FootballDataIntegration.java
│   └── [records de respuesta externa privados de la integración]
├── models/
│   ├── Player.java
│   └── PlayerSynchronizationResult.java
├── repositories/PlayerRepository.java
├── services/
│   ├── FootballDataPlayerService.java
│   ├── PlayerCatalogService.java
│   └── PlayerSynchronizationOrchestrator.java
└── resources/
    └── db/migration/V2__create_players_table.sql

backend/src/test/java/footballmarket/
├── controllers/PlayerControllerTest.java
├── integrations/footballdata/FootballDataIntegrationTest.java
├── services/PlayerCatalogServiceTest.java
├── services/FootballDataPlayerServiceTest.java
└── integration/PlayerCatalogIntegrationTest.java
```

**Structure Decision**: Se conserva la estructura física y las responsabilidades de la Constitution. El Orchestrator está justificado porque coordina una obtención externa y la aplicación transaccional local, sin depender directamente de Repository ni Integration.

## Design

### Persistencia y modelo

`Player` será una entidad JPA en `models` con los siguientes atributos:

| Atributo | Tipo técnico | Regla |
|---|---|---|
| `id` | `Long` | Clave primaria: ID estable de Football-Data.org, sin generación local. Impide duplicados por proveedor. |
| `name` | `String` | Obligatorio y no vacío. |
| `team` | `String` | Obligatorio y no vacío. |
| `league` | `String` | Obligatorio y no vacío. |
| `position` | `String` | Obligatorio y no vacío. |
| `active` | `boolean` | Obligatorio; define la visibilidad en el catálogo. |

La entidad no usará `Builder` ni `@Setter`. Expondrá operaciones de dominio para actualizar los datos desde una fuente válida, activar y desactivar. La migración `V2__create_players_table.sql` creará `players` con `id` como PK, columnas no nulas para los datos públicos y `active` no nulo. No se presupone un índice adicional sobre el campo booleano `active`; su necesidad se evaluará con datos y planes de consulta reales.

`PlayerRepository` extenderá `JpaRepository<Player, Long>` y expondrá consultas para páginas de jugadores activos y para recuperar los jugadores activos que deban inactivarse. No contendrá reglas de negocio.

### Contratos HTTP y mapeo

- `PlayerResponseDTO`: `id`, `name`, `team`, `league`, `position`.
- `PlayersPageResponseDTO`: `content`, `page`, `size`, `totalElements`, `totalPages`.
- `PlayerSyncResponseDTO`: resultado de la sincronización y contadores `obtained`, `created`, `updated`, `markedInactive`, `discardedInvalid`.
- `PlayerSynchronizationResult`: Model de aplicación usado entre Services/Orchestrator y convertido al DTO de sincronización por el Mapper.

`PlayerMapper` transformará únicamente `Player` y resultados de sincronización a sus DTOs. El Controller no construirá ni inspeccionará entidades.

### `GET /players`

`PlayerController` recibirá `page` y `size` como parámetros de consulta, con valores por defecto 0 y 20. Validará el contrato estructural: `page >= 0` y `1 <= size <= 100`; ante incumplimiento lanzará una excepción de presentación manejada como `400 Bad Request` con mensaje seguro en español.

El Controller delegará en `PlayerCatalogService.getActivePlayers(page, size)`. El servicio consultará exclusivamente `PlayerRepository` usando paginación y el predicado `active = true`; nunca invocará `FootballDataIntegration`. El Mapper devolverá el DTO paginado. Una página válida sin jugadores devuelve `200 OK` y metadatos con contenido vacío.

### `POST /players/sync`

`PlayerController` delegará en `PlayerSynchronizationOrchestrator.synchronize()`. No recibirá cuerpo de petición ni disparará trabajo automático.

El Orchestrator seguirá este flujo:

1. Solicita a `FootballDataPlayerService` una foto validada de jugadores de todas las competiciones configuradas.
2. Si la obtención completa falla, propaga el error de proveedor y no invoca la aplicación local de cambios.
3. Si la foto está completa, delega en `PlayerCatalogService.applySynchronization(...)`.
4. Devuelve el resultado del Service para que el Controller lo convierta al DTO HTTP.

`PlayerCatalogService.applySynchronization(...)` será transaccional y realizará el upsert por `Player.id`:

- crea el jugador si no existe;
- actualiza datos y activa al jugador si existe, incluso si estaba inactivo;
- una vez procesada la foto completa, desactiva a los jugadores que siguen activos pero no estén en ella;
- nunca elimina filas.

La fase de llamadas remotas sucede antes de abrir la transacción de escritura. La aplicación de la foto ocurre dentro de una única transacción. Una excepción de persistencia revierte todas las altas, actualizaciones e inactivaciones de esa ejecución. Esta feature presupone un despliegue de una sola instancia. La sincronización manual se serializará dentro de esa instancia para que dos ejecuciones no apliquen fotos simultáneas. Un despliegue con varias instancias requiere definir coordinación entre ellas antes de habilitarlo.

Los contadores tendrán una semántica única para logs y respuesta: `obtained` cuenta los integrantes de planteles leídos antes de validar y consolidar; `discardedInvalid` cuenta los que no cumplen los campos obligatorios; `created` cuenta IDs inexistentes; `updated` cuenta IDs ya existentes procesados, incluidas reactivaciones; y `markedInactive` cuenta únicamente cambios efectivos de activo a inactivo. Un jugador repetido y válido se consolida por ID y no incrementa `created` ni `updated` más de una vez.

### Integración Football-Data.org

`FootballDataIntegration` será el único componente que conozca los recursos y los JSON del proveedor. Usará un `RestClient` configurado con la URL base de `FootballDataProperties`, HTTPS, timeouts de conexión y lectura, y la clave configurada mediante `FOOTBALL_DATA_API_KEY` en el encabezado requerido por Football-Data.org. No se implementarán reintentos automáticos.

Para cada código de competición, respetando su orden configurado, la integración realizará:

1. `GET /competitions/{competitionCode}` para obtener el nombre de la competición, que alimenta `league`.
2. `GET /competitions/{competitionCode}/teams` para obtener sus equipos.
3. Para cada equipo, `GET /teams/{teamId}` para obtener el plantel `squad`.
4. Para cada integrante de `squad`, extraerá `id`, `name` y `position`; combinará esos valores con `team.name` y `competition.name` para producir el candidato de catálogo.

La documentación oficial confirma que el recurso de competición expone `name`, que su subrecurso de equipos existe y que el recurso de equipo expone `name` y `squad`, cuyos integrantes incluyen `id`, `name` y `position`. [Competition](https://docs.football-data.org/general/v4/competition.html), [Team](https://docs.football-data.org/general/v4/team.html).

La configuración ampliará `FootballDataProperties` con una lista ordenada, no vacía y obligatoria `competitions`, vinculada a `football-data.competitions`. No habrá códigos de competición hardcodeados: cada ambiente suministra los códigos a los que la credencial tiene acceso. La configuración actual de `apiKey` y `baseUrl` se conserva.

La Integration validará toda respuesta externa antes de entregarla: ausencia o valor vacío de `id`, `name`, `team`, `league` o `position` descarta solo ese candidato, registra un motivo seguro y aumenta `discardedInvalid`. Si una competición, equipo o plantel no se puede obtener, o el proveedor responde un estado no exitoso o una estructura inválida, se aborta toda la foto y se lanza una excepción técnica de integración. `FootballDataPlayerService` consolidará los candidatos válidos por ID usando el primero encontrado: al recorrer las competiciones en el orden configurado, esa regla preserva el `league` requerido cuando el mismo jugador aparezca en más de una competición.

### Errores, logs y seguridad

- Se añadirán excepciones de presentación para paginación inválida y de integración para indisponibilidad o respuesta inválida del proveedor. `GlobalExceptionHandler` incorporará manejadores acotados para devolver `400 Bad Request` y `502 Bad Gateway`, respectivamente, mediante `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`; el código HTTP coincidirá con `status` y `path` identificará la ruta solicitada. Los mensajes serán seguros y estarán en español, sin detalles de infraestructura.
- `POST /players/sync` devuelve `200 OK` con su resumen cuando se completa y `502 Bad Gateway` ante un fallo de Football-Data.org. El body de error no contendrá URL completa, clave, headers ni respuesta cruda del proveedor.
- Se usarán logs de Spring para inicio, finalización, contadores de sincronización, descartes con su motivo y fallos de comunicación. No se registra `apiKey`, `X-Auth-Token` ni credenciales.
- `SecurityConfig` no se modificará para esta feature. Los endpoints heredan la autenticación JWT vigente. Cualquier usuario autenticado podrá invocar `POST /players/sync` sin un rol o permiso adicional; una solicitud sin autenticación válida recibirá `401 Unauthorized` antes de iniciar la sincronización.

### OpenAPI

`PlayerController` utilizará anotaciones SpringDoc para documentar:

- `GET /players`: propósito, `page` y `size`, valores por defecto, límites, DTO de página, autenticación y respuestas `200`/`400`/`401`.
- `POST /players/sync`: propósito, ausencia de body, autenticación sin rol adicional, DTO de resultado y respuestas `200`/`401`/`502`.
- Los DTOs: significado, obligatoriedad y ejemplos de cada campo expuesto.
- Los errores: formato seguro y códigos aplicables.

### Postman

La colección versionada del proyecto incorporará solicitudes para `GET /players` y `POST /players/sync` con método, ruta, parámetros, headers, autenticación y body según cada contrato. Los ejemplos serán utilizables sin incluir secretos ni credenciales reales.

## Testing Strategy

| Capa | Pruebas planificadas |
|---|---|
| Model | Invariantes de campos obligatorios, actualización, activación y desactivación de `Player`. |
| Mapper | Conversión de `Player`, página y resultado de sincronización a DTOs, sin exposición de `active`. |
| Integration | Mapeo de competición/equipos/planteles, omisión de registros incompletos y conversión de fallos HTTP, timeout o respuesta inválida a error de integración. Las respuestas externas se simularán; no se llamará a Football-Data.org real. |
| Services | Consulta solo de activos, defaults recibidos desde Controller, creación, actualización sin duplicado, inactivación, reactivación, contadores y ausencia de mutaciones si la foto externa falla. |
| Controller | `GET /players` con página válida, catálogo vacío, defaults y cada límite inválido; `POST /players/sync` exitoso, con proveedor no disponible, con cualquier usuario autenticado y sin autenticación válida; contrato JSON, códigos y documentación de los casos verificados mediante Spring REST Docs. |
| Integración | Con Testcontainers/PostgreSQL y Flyway: persistencia de PK externa, consulta paginada exclusiva de activos, y aplicación transaccional de una foto completa frente a fallo previo a la aplicación. |

Todos los tests nuevos usarán `@ActiveProfiles("test")`, serán deterministas y limpiarán los datos que creen. Los tests de Service e Integration aislarán Repository y proveedor con mocks o servidor HTTP de prueba provisto por Spring; los de persistencia usarán PostgreSQL real mediante Testcontainers.

## Files Affected

| Área | Acción planificada |
|---|---|
| `config/FootballDataProperties.java` | Añadir la lista ordenada de competiciones configuradas. |
| `config/FootballDataClientConfig.java` | Crear el cliente HTTP seguro y con timeouts para la Integration. |
| `models`, `repositories`, `services`, `integrations`, `controllers` | Añadir los componentes descritos, respetando responsabilidades y dependencias de la Constitution. |
| `controllers/exceptions/GlobalExceptionHandler.java` | Añadir manejo seguro de los errores propios del catálogo sin alterar reglas de negocio. |
| `resources/db/migration/V2__create_players_table.sql` | Crear la estructura persistente de jugadores. |
| `resources/application.properties` y configuración de prueba | Declarar la lista de competiciones sin incorporar credenciales al repositorio. |
| `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json` | Añadir solicitudes para el catálogo y la sincronización a la colección existente. |
| `src/test` | Añadir las pruebas unitarias, de Controller, de Integration y de persistencia definidas. |

## Complexity Tracking

No se introducen nuevas dependencias ni arquitecturas paralelas. El Orchestrator y la Integration responden a responsabilidades explícitas de coordinación y comunicación externa requeridas por la Constitution.

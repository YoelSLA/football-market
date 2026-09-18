# Tareas: Catálogo de jugadores

**Entrada**: `specs/002-player-catalog/spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/api.md` y `quickstart.md`.

**Organización**: Las tareas se agrupan por historia de usuario. Todos los tests nuevos usan `@ActiveProfiles("test")`; los tests de Controller documentan los casos HTTP verificados con Spring REST Docs. Los errores gestionados usan `ErrorResponseDTO`.

**Formato**: `[ID] [P?] [US?] Descripción con ruta`. `[P]` indica que la tarea puede ejecutarse en paralelo con las otras tareas marcadas en su mismo tramo, siempre que se hayan completado sus dependencias explícitas.

## Fase 1: Preparación

**Objetivo**: Confirmar la infraestructura existente antes de ampliar el backend.

- [ ] T001 Verificar en `backend/build.gradle.kts` que ya están disponibles Spring Web, Spring Data JPA, Spring Security, Validation, Flyway, SpringDoc, REST Docs, Mockito y Testcontainers; añadir solo una dependencia requerida que falte.
- [ ] T002 Verificar la configuración existente de JWT y las reglas de autenticación para `/players` en `backend/src/main/java/footballmarket/config/SecurityConfig.java`, sin añadir roles ni modificarla si la protección actual ya cubre ambas rutas.

## Fase 2: Base compartida

**Objetivo**: Disponer de la persistencia y el modelo de jugador que necesitan ambas historias.

**Bloqueo**: Completar esta fase antes de las historias de usuario.

- [ ] T003 [P] Crear `backend/src/main/resources/db/migration/V2__create_players_table.sql` con `id` externo como PK y columnas `name`, `team`, `league`, `position` y `active` no nulas.
- [ ] T004 [P] Implementar `backend/src/main/java/footballmarket/models/Player.java` con invariantes de campos obligatorios y operaciones de actualización, activación e inactivación, sin `Builder` ni `@Setter`.
- [ ] T005 Crear `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` con consulta paginada de activos y operaciones necesarias para aplicar una foto completa por ID, sin lógica de negocio.
- [ ] T006 Crear `backend/src/test/java/footballmarket/models/PlayerTest.java` para verificar invariantes, actualización y transiciones de estado de `Player` con el perfil `test`.

**Punto de control**: El esquema y las reglas del jugador están listos para las dos historias.

## Fase 3: Historia de usuario 1 — Consultar el catálogo (P1, MVP)

**Objetivo**: Servir páginas de jugadores activos exclusivamente desde PostgreSQL.

**Prueba independiente**: Con y sin jugadores activos, `GET /players` devuelve `200` y metadatos correctos; cuando se omiten los parámetros usa `page=0` y `size=20`, acepta `size` entre 1 y 100, rechaza `page<0`, `size<1` y `size>100` con `400`, y funciona sin acceso a Football-Data.org. Requiere JWT válido según el contrato.

### Tests

- [ ] T007 [P] [US1] Crear `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` para consulta paginada exclusiva de activos, catálogo vacío y ausencia de llamadas externas.
- [ ] T008 [P] [US1] Crear `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para respuesta `200`, campos exactos del jugador, metadatos, defaults, límites `400`, autenticación `401` y snippets REST Docs de `GET /players`.
- [ ] T009 [P] [US1] Crear `backend/src/test/java/footballmarket/integration/PlayerCatalogIntegrationTest.java` con Testcontainers/PostgreSQL y Flyway para comprobar PK externa, paginación y exclusión de inactivos.

### Implementación

- [ ] T010 [P] [US1] Crear los records `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java` y `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayersPageResponseDTO.java` con los campos exactos de `contracts/api.md` y anotaciones de esquema OpenAPI en español.
- [ ] T011 [US1] Implementar la conversión de `Player` y `Page<Player>` a DTO en `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` como clase `final`, sin estado, constructor privado y métodos `static`.
- [ ] T012 [US1] Implementar `getActivePlayers(page, size)` en `backend/src/main/java/footballmarket/services/PlayerCatalogService.java` mediante `PlayerRepository`, sin llamar al proveedor externo.
- [ ] T013 [US1] Implementar `GET /players` en `backend/src/main/java/footballmarket/controllers/PlayerController.java` con defaults 0/20 y validación estructural HTTP de `page >= 0` y `1 <= size <= 100` antes de delegar al Service y Mapper; documentar `200`, `400` y `401` en OpenAPI.
- [ ] T014 [US1] Añadir la excepción de paginación inválida en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPlayerPageException.java` y su manejo `400` seguro en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`.
- [ ] T015 [US1] Añadir la solicitud autenticada `GET {{BASE_URL}}/players?page=0&size=20` a `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`, sin secretos reales.

**Punto de control**: US1 puede validarse sin ejecutar una sincronización ni llamar al proveedor.

## Fase 4: Historia de usuario 2 — Sincronizar manualmente (P1)

**Objetivo**: Construir una foto completa desde Football-Data.org y aplicarla de forma atómica al catálogo local cuando un usuario autenticado invoque `POST /players/sync`.

**Prueba independiente**: Con respuestas externas simuladas, una ejecución completa crea, actualiza, reactiva y desactiva sin duplicados; una lectura externa incompleta o fallida devuelve `502` sin cambios locales; registros inválidos se descartan; sin JWT se devuelve `401` sin iniciar la sincronización.

### Tests

- [ ] T016 [P] [US2] Crear `backend/src/test/java/footballmarket/integrations/footballdata/FootballDataIntegrationTest.java` con respuestas HTTP simuladas para competición, equipos y planteles, descartes por campos faltantes, timeouts, errores HTTP y estructuras incompletas.
- [ ] T017 [P] [US2] Crear `backend/src/test/java/footballmarket/services/FootballDataPlayerServiceTest.java` para consolidación por ID, precedencia de la primera competición configurada y contadores `obtained` y `discardedInvalid`.
- [ ] T018 [US2] Ampliar `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` con creación, actualización, reactivación, inactivación, duplicados, contadores y rollback de la aplicación transaccional.
- [ ] T019 [P] [US2] Crear `backend/src/test/java/footballmarket/services/PlayerSynchronizationOrchestratorTest.java` para verificar que un fallo antes de completar la foto impide la aplicación local y que una foto válida se aplica una sola vez.
- [ ] T020 [US2] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` con `POST /players/sync` exitoso, `502` seguro, `401` sin invocación del Orchestrator, usuario autenticado sin rol adicional, JSON contractual y snippets REST Docs.
- [ ] T021 [US2] Ampliar `backend/src/test/java/footballmarket/integration/PlayerCatalogIntegrationTest.java` para verificar upsert, reactivación, inactivación sin borrado, y ausencia de cambios ante fallo previo o rollback en PostgreSQL.

### Implementación

- [ ] T022 [P] [US2] Ampliar `backend/src/main/java/footballmarket/config/FootballDataProperties.java` con la lista ordenada y no vacía `competitions`; enlazar `football-data.competitions` a una variable de entorno en `backend/src/main/resources/application.properties` y definir códigos de prueba en `backend/src/test/resources/application-test.yml`, conservando `apiKey` y `baseUrl` y sin versionar credenciales.
- [ ] T023 [P] [US2] Crear `backend/src/main/java/footballmarket/config/FootballDataClientConfig.java` con `RestClient` HTTPS, URL y clave de `FootballDataProperties`, encabezado `X-Auth-Token`, timeouts de conexión y lectura, sin reintentos.
- [ ] T024 [P] [US2] Crear `backend/src/main/java/footballmarket/models/PlayerSynchronizationResult.java` con los contadores `obtained`, `created`, `updated`, `markedInactive` y `discardedInvalid` y sus invariantes.
- [ ] T025 [US2] Implementar `backend/src/main/java/footballmarket/integrations/footballdata/FootballDataIntegration.java` con los recursos de competición, equipos y plantel, records externos privados o confinados al paquete, validación de respuestas y candidatos, descarte con motivo seguro, y excepción técnica ante foto incompleta.
- [ ] T026 [US2] Implementar `backend/src/main/java/footballmarket/services/FootballDataPlayerService.java` para recorrer competiciones en orden y consolidar candidatos válidos por ID conservando la primera `league`, junto con los contadores de obtención y descarte.
- [ ] T027 [US2] Añadir `applySynchronization(...)` transaccional a `backend/src/main/java/footballmarket/services/PlayerCatalogService.java` para crear, actualizar y reactivar por ID, desactivar ausentes solo tras foto completa y calcular los contadores sin eliminación física.
- [ ] T028 [US2] Crear `backend/src/main/java/footballmarket/services/PlayerSynchronizationOrchestrator.java` para serializar sincronizaciones dentro de la única instancia prevista para esta feature, obtener toda la foto antes de abrir la transacción de escritura y delegar su aplicación al Service.
- [ ] T029 [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerSyncResponseDTO.java` y ampliar `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` para convertir el resultado a los cinco contadores contractuales, documentados en OpenAPI.
- [ ] T030 [US2] Añadir la excepción técnica `backend/src/main/java/footballmarket/integrations/footballdata/FootballDataUnavailableException.java` y su manejo `502` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` sin exponer URL, headers, credenciales o respuesta cruda.
- [ ] T031 [US2] Añadir `POST /players/sync` sin body en `backend/src/main/java/footballmarket/controllers/PlayerController.java`, delegado al Orchestrator, con respuesta DTO y documentación OpenAPI de `200`, `401` y `502`.
- [ ] T032 [US2] Registrar con el logger habitual de Spring el inicio de cada sincronización y, al completarse, los contadores `obtained`, `created`, `updated`, `markedInactive` y `discardedInvalid` en `backend/src/main/java/footballmarket/services/PlayerSynchronizationOrchestrator.java`; registrar un fallo con causa general segura y cada descarte con el campo obligatorio ausente en `backend/src/main/java/footballmarket/integrations/footballdata/FootballDataIntegration.java`, sin clave, header de autenticación, respuesta cruda ni credenciales.
- [ ] T033 [US2] Añadir la solicitud autenticada `POST {{BASE_URL}}/players/sync` sin body a `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`, con ejemplo de respuesta y sin credenciales reales.

**Punto de control**: US2 puede validarse con proveedor simulado y PostgreSQL local, sin depender de una llamada real para los tests.

## Fase 5: Pulido y controles comunes

**Objetivo**: Mantener contratos, documentación y controles coherentes.

- [ ] T034 [P] Revisar `backend/src/docs/asciidoc/index.adoc` para incluir los snippets REST Docs de ambos endpoints y sus respuestas documentadas.
- [ ] T035 Revisar `specs/002-player-catalog/contracts/api.md` frente a los DTO, OpenAPI y Postman implementados; corregir solo discrepancias de implementación o documentación acordada.
- [ ] T036 Ejecutar `backend/gradlew.bat test spotlessCheck` desde `backend/`, validar el flujo manual de `specs/002-player-catalog/quickstart.md` y comprobar que el CI aprueba `clean build` y que el análisis de SonarQube y su Quality Gate aprueban cuando estén disponibles; registrar los fallos preexistentes y verificablemente ajenos al cambio sin alterar controles para ocultarlos.

## Dependencias y orden de ejecución

```text
Preparación (T001–T002)
  → Base compartida (T003–T006)
    → US1 consulta (T007–T015)
      → US2 sincronización (T016–T033)
        → Pulido (T034–T036)
```

- US1 depende del modelo, la migración y el Repository de la base compartida. Dentro de US1, escribir primero los tests; los DTO preceden al Mapper, el Repository precede al Service y el Service precede al Controller.
- US2 reutiliza `Player`, `PlayerRepository`, `PlayerCatalogService`, `PlayerMapper` y `PlayerController` de US1. La foto externa completa debe existir antes de aplicar cambios locales. Los tests de cada responsabilidad se escriben antes de su implementación y deben fallar por la conducta faltante.
- El catálogo de US1 puede entregarse como MVP con datos locales de prueba; US2 incorpora la actualización manual sin afectar la independencia de lectura.

## Ejemplos de trabajo paralelo

- **US1**: T007, T008 y T009 se pueden preparar en archivos distintos; T010 puede avanzar en paralelo. Tras ellos, T011–T015 siguen las dependencias de Mapper, Service y Controller.
- **US2**: T016, T017 y T019 se pueden preparar en archivos distintos; T022, T023 y T024 también avanzan en archivos distintos. T018, T020 y T021 modifican tests compartidos y se coordinan con sus tareas previas en esos archivos.

## Estrategia de implementación

1. Completar preparación y base compartida.
2. Implementar y validar US1 como MVP: catálogo local, paginado y autenticado, incluso si Football-Data.org no está disponible.
3. Implementar US2: obtener foto completa, aplicar cambios en transacción y exponer el resultado manual.
4. Ejecutar controles, verificar OpenAPI y Postman, y seguir la guía de validación.

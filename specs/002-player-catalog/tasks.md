# Tareas: Catálogo de jugadores

**Entrada**: `specs/002-player-catalog/spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/api.md` y `quickstart.md`.

**Organización**: Las tareas se agrupan por historia de usuario. Todos los tests nuevos usan `@ActiveProfiles("test")`; los tests de Controller documentan los casos HTTP verificados con Spring REST Docs. Los errores gestionados usan `ErrorResponseDTO`.

**Formato**: `[ID] [P?] [US?] Descripción con ruta`. `[P]` indica que la tarea puede ejecutarse en paralelo con las otras tareas marcadas en su mismo tramo, siempre que se hayan completado sus dependencias explícitas.

**Alcance excluido**: Postman y toda integración, consulta, matching o descarga de imágenes desde TheSportsDB. `imageUrl` forma parte del modelo y del contrato público, pero esta feature no lo obtiene ni lo resuelve.

**Sobre la ejecución de tests**: conforme a la Constitution, el agente no ejecuta tests. Las tareas de creación y modificación de tests pertenecen al agente; su ejecución y la verificación de resultado quedan a cargo del usuario.

## Fase 1: Preparación

**Objetivo**: Confirmar la infraestructura existente antes de ampliar el backend.

- [X] T001 Verificar en `backend/build.gradle.kts` que ya están disponibles Spring Web, Spring Data JPA, Spring Security, Validation, Flyway, SpringDoc, REST Docs, Mockito y Testcontainers; añadir solo una dependencia requerida que falte.
- [X] T002 Verificar la configuración existente de JWT y las reglas de autenticación para `/api/players` en `backend/src/main/java/footballmarket/config/SecurityConfig.java`, sin añadir roles ni modificarla si la protección actual ya cubre ambas rutas.

## Fase 2: Base compartida

**Objetivo**: Disponer de la persistencia y el modelo de jugador que necesitan ambas historias.

**Estado**: La tabla y la entidad `Player` ya existen sobre el modelo anterior, en el que `players.id` era el identificador de Football-Data.org. Esa base no se rehace: `V2` no se modifica y el desacoplamiento de identidad se realiza en la Fase 7 mediante una migración nueva.

**Bloqueo**: Completar esta fase antes de las historias de usuario.

- [X] T003 [P] Crear `backend/src/main/resources/db/migration/V2__create_players_table.sql` con `id` externo como PK y columnas `name`, `team`, `league`, `position` y `active` no nulas. La migración queda inmutable; toda evolución del esquema se resuelve en `V3` o posterior.
- [X] T004 [P] Implementar `backend/src/main/java/footballmarket/models/Player.java` con invariantes de campos obligatorios y operaciones de actualización, activación e inactivación, sin `Builder` ni `@Setter`.
- [X] T005 Crear `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` con consulta paginada de activos y las operaciones necesarias para aplicar una foto completa, sin lógica de negocio.
- [X] T006 Crear `backend/src/test/java/footballmarket/models/PlayerTest.java` para verificar invariantes, actualización y transiciones de estado de `Player` con el perfil `test`.

**Punto de control**: El esquema y las reglas del jugador están listos para las dos historias.

## Fase 3: Historia de usuario 1 — Consultar el catálogo (P1, MVP)

**Objetivo**: Servir páginas de jugadores activos exclusivamente desde PostgreSQL.

**Prueba independiente**: Con y sin jugadores activos, `GET /api/players` devuelve `200` y metadatos correctos; cuando se omiten los parámetros usa `page=0` y `size=20`, acepta `size` entre 1 y 100, rechaza `page<0`, `size<1` y `size>100` con `400`, y funciona sin acceso a Football-Data.org. Requiere JWT válido según el contrato.

**Estado**: Las tareas de esta fase se completaron sobre el contrato vigente de `spec.md`. La ampliación del contrato con los atributos opcionales y el `id` interno corresponde a la Fase 7.

### Tests

- [X] T007 [P] [US1] Crear `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` para consulta paginada exclusiva de activos, catálogo vacío y ausencia de llamadas externas.
- [X] T008 [P] [US1] Crear `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para respuesta `200`, campos contractuales del jugador, metadatos, defaults, límites `400`, autenticación `401` y snippets REST Docs de `GET /api/players`.
- [X] T009 [P] [US1] Crear `backend/src/test/java/footballmarket/integration/PlayerCatalogIntegrationTest.java` con Testcontainers/PostgreSQL y Flyway para comprobar la persistencia del jugador, la paginación y la exclusión de inactivos.

### Implementación

- [X] T010 [P] [US1] Crear los records `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java` y `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayersPageResponseDTO.java` con los campos definidos en `contracts/api.md` y anotaciones de esquema OpenAPI en español.
- [X] T011 [US1] Implementar la conversión de `Player` y `Page<Player>` a DTO en `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` como clase `final`, sin estado, constructor privado y métodos `static`.
- [X] T012 [US1] Implementar `getActivePlayers(page, size)` en `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` mediante `PlayerRepository`, sin llamar al proveedor externo.
- [X] T013 [US1] Implementar `GET /api/players` en `backend/src/main/java/footballmarket/controllers/PlayerController.java` con defaults 0/20 y validación estructural HTTP de `page >= 0` y `1 <= size <= 100` antes de delegar al Service y Mapper; documentar `200`, `400` y `401` en OpenAPI.
- [X] T014 [US1] Añadir la excepción de paginación inválida en `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPlayerPageException.java` y su manejo `400` seguro en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`.

**Punto de control**: US1 puede validarse sin ejecutar una sincronización ni llamar al proveedor.

## Fase 4: Historia de usuario 2 — Sincronizar manualmente (P1)

**Objetivo**: Construir una foto completa desde Football-Data.org y aplicarla de forma atómica al catálogo local cuando un usuario autenticado invoque `POST /api/players/sync`.

**Prueba independiente**: Con respuestas externas simuladas, una ejecución completa crea, actualiza, reactiva y desactiva sin duplicados; una lectura externa incompleta o fallida devuelve `502` sin cambios locales; registros inválidos se descartan; sin JWT se devuelve `401` sin iniciar la sincronización.

**Estado**: Completada sobre el modelo anterior, en el que el jugador se resolví por su identificador de Football-Data.org. La resolución por referencia externa delega en la Fase 7.

### Tests

- [X] T016 [P] [US2] Crear `backend/src/test/java/footballmarket/integrations/FootballDataIntegrationTest.java` con respuestas HTTP simuladas para competición, equipos y planteles, descartes por campos faltantes, timeouts, errores HTTP y estructuras incompletas.
- [X] T017 [P] [US2] Crear `backend/src/test/java/footballmarket/services/FootballDataPlayerServiceTest.java` para la consolidación de candidatos válidos y la precedencia de la primera competición recorrida, junto con los contadores de obtención y descarte.
- [X] T018 [US2] Ampliar `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` con creación, actualización, reactivación, inactivación, duplicados, contadores y rollback de la aplicación transaccional.
- [X] T019 [P] [US2] Crear `backend/src/test/java/footballmarket/orchestrators/PlayerSynchronizationOrchestratorTest.java` para verificar que un fallo antes de completar la foto impide la aplicación local y que una foto válida se aplica una sola vez.
- [X] T020 [US2] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` con `POST /api/players/sync` exitoso, `502` seguro, `401` sin invocación del Orchestrator, usuario autenticado sin rol adicional, JSON contractual y snippets REST Docs.
- [X] T021 [US2] Ampliar `backend/src/test/java/footballmarket/integration/PlayerCatalogIntegrationTest.java` para verificar la aplicación de la foto, la reactivación, la inactivación sin borrado, y la ausencia de cambios ante fallo previo o rollback en PostgreSQL.

### Implementación

- [X] T022 [P] [US2] Ampliar `backend/src/main/java/footballmarket/config/FootballDataProperties.java` con la lista ordenada y no vacía `competitions`; enlazar `football-data.competitions` a una variable de entorno en `backend/src/main/resources/application.properties` y definir códigos de prueba en `backend/src/test/resources/application-test.yml`, conservando `apiKey` y `baseUrl` y sin versionar credenciales.
- [X] T023 [P] [US2] Crear `backend/src/main/java/footballmarket/config/FootballDataClientConfig.java` con `RestClient` HTTPS, URL y clave de `FootballDataProperties`, encabezado `X-Auth-Token`, timeouts de conexión y lectura, sin reintentos.
- [X] T024 [P] [US2] Crear `backend/src/main/java/footballmarket/models/records/PlayerSynchronizationResult.java` con los contadores `obtained`, `created`, `updated`, `markedInactive` y `discardedInvalid` y sus invariantes.
- [X] T025 [P] [US2] Implementar `backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java` con los recursos de competición, equipos y plantel, records externos privados o confinados al paquete, validación de respuestas y candidatos, descarte con motivo seguro, y excepción técnica ante foto incompleta.
- [X] T026 [US2] Implementar `backend/src/main/java/footballmarket/services/FootballDataPlayerService.java` para recorrer competiciones en orden y consolidar los candidatos válidos conservando la primera `league` vista, junto con los contadores de obtención y descarte.
- [X] T027 [US2] Añadir `applySynchronization(...)` transaccional a `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` para crear, actualizar y reactivar jugadores, desactivar los ausentes solo tras foto completa y calcular los contadores sin eliminación física.
- [X] T028 [US2] Crear `backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java` para serializar sincronizaciones dentro de la única instancia prevista para esta feature, obtener toda la foto antes de abrir la transacción de escritura y delegar su aplicación al Service.
- [X] T029 [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerSyncResponseDTO.java` y ampliar `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` para convertir el resultado a los cinco contadores contractuales, documentados en OpenAPI.
- [X] T030 [US2] Añadir la excepción técnica `backend/src/main/java/footballmarket/integrations/exceptions/FootballDataUnavailableException.java` y su manejo `502` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` sin exponer URL, headers, credenciales ni respuesta cruda.
- [X] T031 [US2] Añadir `POST /api/players/sync` sin body en `backend/src/main/java/footballmarket/controllers/PlayerController.java`, delegado al Orchestrator, con respuesta DTO y documentación OpenAPI de `200`, `401` y `502`.
- [X] T032 [US2] Registrar con el logger habitual de Spring el inicio de cada sincronización y, al completarse, los contadores `obtained`, `created`, `updated`, `markedInactive` y `discardedInvalid` en `backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java`; registrar un fallo con causa general segura y cada descarte con su motivo en `backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java`, sin clave, header de autenticación, respuesta cruda ni credenciales.

**Punto de control**: US2 puede validarse con proveedor simulado y PostgreSQL local, sin depender de una llamada real para los tests.

## Fase 5: Pulido y controles comunes

**Objetivo**: Mantener contratos, documentación y controles coherentes.

- [X] T034 [P] Revisar `backend/src/docs/asciidoc/index.adoc` para incluir los snippets REST Docs de ambos endpoints y sus respuestas documentadas.
- [X] T035 Revisar `specs/002-player-catalog/contracts/api.md` frente a los DTO y la documentación OpenAPI implementados; corregir solo discrepancias de implementación o documentación acordada.
- [ ] T036 Control a cargo del usuario. Preparar el material de verificación (casos de prueba y pasos del flujo manual de `specs/002-player-catalog/quickstart.md`) queda en las tareas de tests de cada fase; la ejecución de `backend/gradlew.bat test spotlessCheck`, la validación del flujo manual, la comprobación de que el CI aprueba `clean build` y la del análisis de SonarQube con su Quality Gate, cuando estén disponibles, corresponden exclusivamente al usuario conforme a la Constitution. Los fallos preexistentes y verificablemente ajenos al cambio se registrarán sin alterar controles para ocultarlos.

## Fase 6: Restringir el catálogo a las cinco ligas obligatorias

**Objetivo**: Alinear la implementación con el conjunto obligatorio de ligas `PL`, `BL1`, `PD`, `SA`, `FL1`.

- [X] T037 [P] [US2] Ampliar las pruebas de configuración para aceptar únicamente `PL,BL1,PD,SA,FL1` y rechazar listas con faltantes, códigos adicionales, duplicados; aceptar cualquier orden antes de iniciar la aplicación.
- [X] T038 [P] [US2] Ampliar las pruebas de sincronización para verificar que se consultan exactamente las cinco ligas según el orden configurado, y que el fallo de cualquiera impide aplicar una foto parcial.
- [X] T039 [US2] Ajustar `FootballDataProperties` y la configuración de los ambientes para imponer el conjunto exacto de ligas `PL,BL1,PD,SA,FL1`.
- [X] T040 [US2] Actualizar OpenAPI, REST Docs y los ejemplos afectados para comunicar que la sincronización siempre abarca las cinco ligas obligatorias.
- [ ] T041 Control a cargo del usuario. La ejecución de los controles de T036 y la validación del flujo manual con acceso autorizado a las cinco ligas corresponden exclusivamente al usuario; el agente no las ejecuta.

## Fase 7: Identidad interna y referencias externas

**Objetivo**: Desacoplar la identidad del jugador del identificador de Football-Data.org, sin modificar `V2` y preservando la correspondencia de los jugadores existentes.

**Nota de alcance**: El único proveedor con operación es Football-Data.org. `THE_SPORTS_DB` se modela como proveedor admitido, sin ninguna integración, consulta, matching ni resolución de imágenes. `imageUrl` se incorpora al modelo y al contrato público, pero no se escribe desde ningún proveedor en esta feature.

### Modelo y persistencia

- [X] T042 Crear `backend/src/main/java/footballmarket/models/PlayerProvider.java` con `FOOTBALL_DATA` y `THE_SPORTS_DB`, sin operación con otros proveedores.
- [X] T043 Crear `backend/src/main/java/footballmarket/models/PlayerExternalReference.java` con ID generado, jugador/proveedor/externalId obligatorios y asociación inmutable.
- [X] T044 Adaptar `backend/src/main/java/footballmarket/models/Player.java` a ID interno generado por secuencia, referencias y atributos opcionales. Actualizar solo opcionales informados y no modificar `imageUrl`. **Realizada después de crear V3 en T045.**
- [X] T045 Crear `backend/src/main/resources/db/migration/V3__decouple_player_external_identity.sql` sin modificar V2: reasignar IDs, conservar temporalmente `legacy_external_id`, crear referencias con FK/UNIQUE/índice, hacer backfill `FOOTBALL_DATA`, retirar columna auxiliar y alinear ambas secuencias, incluida base vacía. **Precede a T044.**
- [X] T046 Crear `backend/src/main/java/footballmarket/repositories/PlayerExternalReferenceRepository.java` con resolución por `(provider, externalId)`, proyección escalar de propietarios y consulta de referencias del proveedor.
- [X] T047 Preparar aserciones de esquema, UNIQUE e índice en `backend/src/test/java/footballmarket/repositories/PlayerMigrationTest.java`; `PlayerCatalogPersistenceTest.java` inicia Hibernate con `ddl-auto=validate`. Solo preparación: ejecución en T065.

### Contrato y mapeo

**Restricción de T045 para T054**: la UNIQUE `(provider, external_id)` debe ser inmediata (`NOT DEFERRABLE`) y llamarse `uk_player_external_references_provider_external_id`; ese nombre y la tabla forman parte de la clasificación exacta del conflicto.

- [X] T048 Ampliar `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java` con ID interno y los tres opcionales nullable, siempre presentes en JSON, documentados en OpenAPI.
- [X] T049 Adaptar `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` a los ocho campos públicos sin exponer `active` ni referencias externas.

### Sincronización

- [X] T050 Adaptar `backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java` a `PlayerCandidate` inmutable, externalId y opcionales normalizados sin descartar por tipos/formatos inválidos. No extraer imágenes.
- [X] T051 Adaptar `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` a resolución previa por referencia, alta conjunta y actualización/reactivación conservando opcionales. `PlayerSnapshot` ya no transporta entidades JPA.
- [X] T052 Inactivar por referencias `FOOTBALL_DATA` ausentes, preservando otros orígenes, jugadores presentes por otro alias y jugadores protegidos por conflictos; sin borrados.
- [X] T053 Detectar en el preflight de `PlayerCatalogServiceImpl` una asociación incompatible con el propietario previamente resuelto, antes de mutar entidades; descartar, registrar y proteger los IDs implicados. Una referencia existente normal sigue siendo actualización, sin matching por nombre/equipo.
- [X] T054 Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` `TransactionTemplate`/`REQUIRES_NEW`, contexto nuevo por intento, flush atribuible por candidato y clasificación fuera del callback tras rollback. Excluir y reaplicar solo para SQLSTATE `23505`, tabla y UNIQUE esperadas con candidato identificado; otros errores son técnicos. Reaplicaciones acotadas por claves excluidas, sin HTTP, sin reutilizar entidades y con protección de propietarios ante inactivación. Desactivar OSIV y batching. Dependencias T045, T051 y T053.
- [X] T055 Reiniciar contadores por intento, contar descartes una vez y publicar únicamente el resultado confirmado en `PlayerCatalogServiceImpl`; conservar invariantes de `models/records/PlayerSynchronizationResult.java`.

### Proveedores externos

**Coordinación de T051–T055**: T051 proporciona la aplicación de un intento; T054 delimita los intentos y su recuperación. T052 debe excluir de la inactivación los jugadores protegidos por conflictos, sin cambiar las reglas de otros registros inválidos. T055 depende de T054: reiniciar los contadores de escritura por intento, sumar cada descarte una sola vez y publicar únicamente el resultado confirmado. El flush por candidato permite atribuir una violación; no constituye un commit por jugador.

- [X] T066 Completar en `backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java` el manejo existente de 429: Retry-After en segundos o fecha HTTP, máximo tres reintentos y 502 al agotarlos, sin reintentos para otros errores. Mantener fallback existente de 60 segundos y permitir controlar reloj/espera en tests sin dependencias adicionales.

### Documentación

- [X] T056 Actualizar OpenAPI del Controller y DTO con ID interno, opcionales, ausencia de imágenes externas, 429 y errores de aplicación local.
- [X] T057 Actualizar `backend/src/docs/asciidoc/index.adoc` y las fuentes REST Docs en `PlayerControllerTest.java`. No editar ni generar snippets: su generación queda en T065.

### Tests

- [X] T058 Crear `backend/src/test/java/footballmarket/models/PlayerExternalReferenceTest.java` y adaptar `PlayerTest.java` y `PlayerSynchronizationResultTest.java` a identidades separadas, opcionales y foto inmutable.
- [X] T059 Ampliar `backend/src/test/java/footballmarket/integrations/FootballDataIntegrationTest.java` con opcionales válidos, ausentes, vacíos, tipos incorrectos y fechas inválidas, sin descarte del jugador ni lectura de imágenes.
- [X] T060 Adaptar `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` a resolución externa, ID estable, altas, actualización, reactivación, inactivación, duplicados y opcionales. Las pruebas técnicas de rollback y conflictos están en `repositories/PlayerCatalogPersistenceTest.java` para respetar la frontera de tests de Service.
- [X] T061 Adaptar `backend/src/test/java/footballmarket/services/FootballDataPlayerServiceTest.java` a consolidación por externalId y primera liga, usando candidatos inmutables.
- [X] T062 Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` con JSON exacto de ocho campos, opcionales nulos/conocidos, ID interno distinto de externalId, errores seguros y fuentes de snippets REST Docs.
- [X] T063 Crear `backend/src/test/java/footballmarket/repositories/PlayerMigrationTest.java` y `PlayerCatalogPersistenceTest.java`: Flyway V2→V3 poblada/vacía, correspondencia y estados, secuencias, UNIQUE, índice, alta conjunta, otros orígenes e imágenes conservadas.
- [X] T064 Preparar rollback técnico en `services/PlayerCatalogServiceTest.java` y pruebas de otra UNIQUE y fallo al commit sin candidato atribuible en `repositories/PlayerCatalogPersistenceTest.java`. Ejecución exclusivamente en T065.
- [X] T067 Preparar casos deterministas de Retry-After numérico/fecha/fallback, tope de reintentos y otros errores sin reintento en `integrations/FootballDataIntegrationTest.java`.
- [X] T068 Precisar la verificación de colaboradores y ausencia de llamadas adicionales en `orchestrators/PlayerSynchronizationOrchestratorTest.java`; las expectativas HTTP de `integrations/FootballDataIntegrationTest.java` limitan las solicitudes al proveedor Football-Data. Sin clientes ni configuración de TheSportsDB. Ejecución en T065.
- [X] T069 Crear en `repositories/PlayerCatalogPersistenceTest.java` casos de conflicto lógico previo a escritura y UNIQUE real con transacciones concurrentes: rollback, reaplicación, ausencia de huérfanos, propietarios intactos y protegidos, log/descartes únicos, contadores finales y fallo técnico posterior sin escrituras parciales. Infraestructura de barreras en `support/PlayerReferenceReadGate.java`. Dependencias T054 y T063; ejecución en T065.
- [X] T065 Control a cargo del usuario. Ejecución de `backend/gradlew test`, completada el 2026-10-01 con `BUILD SUCCESSFUL` por el usuario: la suite compila y termina sin fallos. Cubre los casos preparados en T058–T064 y T067–T069, incluidos migración `V3`, constraints y concurrencia de PostgreSQL. Evidencia registrada en [validation.md](validation.md).
- [ ] T070 Control a cargo del usuario. Cumplido el 2026-10-01 por ejecución del usuario: `backend/gradlew spotlessCheck` finalizó correctamente. Sigue pendiente la validación manual de los pasos de `specs/002-player-catalog/quickstart.md` para esta fase, por lo que la tarea permanece abierta. El agente no ejecuta estos controles.

## Dependencias y orden de ejecución

**Estado de validación**: los controles pertenecen al usuario. El 2026-10-01 el usuario ejecutó `gradlew test` (`BUILD SUCCESSFUL`, T065 completada) y `gradlew spotlessCheck` (correcto, registrado en T070). Siguen pendientes la validación manual de `quickstart.md`, el `clean build` de CI y el análisis de SonarQube con su Quality Gate, por lo que T036 y T041 permanecen abiertas y T070 aún no se cierra. El agente prepara los casos de verificación pero no ejecuta la suite. Evidencia en [validation.md](validation.md).

```text
Preparación (T001–T002)
  → Base compartida (T003–T006)
    → US1 consulta (T007–T014)
      → US2 sincronización (T016–T032)
        → Pulido (T034–T036)
          → Cinco ligas obligatorias (T037–T041)
            → Identidad interna y referencias externas (T042–T065, T066–T070)
```

- US1 depende del modelo, la migración y el Repository de la base compartida. Dentro de US1, escribir primero los tests; los DTO preceden al Mapper, el Repository precede al Service y el Service precede al Controller.
- US2 reutiliza `Player`, `PlayerRepository`, `PlayerCatalogService`, `PlayerMapper` y `PlayerController` de US1. La foto externa completa debe existir antes de aplicar cambios locales. Los tests de cada responsabilidad se escriben antes de su implementación y deben fallar por la conducta faltante.
- El catálogo de US1 puede entregarse como MVP con datos locales de prueba; US2 incorpora la actualización manual sin afectar la independencia de lectura.
- La Fase 7 es posterior a todo lo anterior porque reaplica el modelo de identidad sobre el código y el esquema ya en uso.
- **Orden obligatorio dentro de la Fase 7: T045 → T044.** La migración `V3` debe existir antes de que `Player` declare `id` generado localmente, porque Hibernate valida el esquema al iniciar y fallaría contra el esquema anterior. Los IDs se conservan por trazabilidad, pero el orden de ejecución es T045, luego T044.
- Recuperación transaccional: T045, T051 y T053 → T054 → T055; T054 y T063 → T069 → T065. T052 debe incorporar la protección de jugadores conflictivos antes de verificar T069. Las tareas de creación de tests pueden prepararse antes de la implementación; su ejecución corresponde al usuario.
- T066 (tratamiento de `429`) y T068 (ausencia de consultas a otros proveedores) pueden ejecutarse en cualquier momento de la Fase 7: T066 depende solo de T050 y T068 solo de T028.
- T070 depende de T065: el formato y la validación manual se verifican sobre el mismo código cuya suite ya pasó.

## Ejemplos de trabajo paralelo

- **US1**: T007, T008 y T009 se pueden preparar en archivos distintos; T010 puede avanzar en paralelo. Tras ellos, T011–T014 siguen las dependencias de Mapper, Service y Controller.
- **US2**: T016, T017 y T019 se pueden preparar en archivos distintos; T022, T023 y T024 también avanzan en archivos distintos. T018, T020 y T021 modifican tests compartidos y se coordinan con sus tareas previas en esos archivos.
- **Fase 7**: T042, T043 y T046 se pueden preparar en archivos distintos. T045 precede a T044. T048 precede a T049, T051 y T058. T066 y T068 son independientes del resto. Las tareas de tests T058–T064 y T067–T068 pueden escribirse antes que su implementación y deben fallar por la conducta faltante.

## Estrategia de implementación

1. Completar preparación y base compartida.
2. Implementar y validar US1 como MVP: catálogo local, paginado y autenticado, incluso si Football-Data.org no está disponible.
3. Implementar US2: obtener foto completa, aplicar cambios en transacción y exponer el resultado manual.
4. Desacoplar la identidad del jugador: modelo y esquema primero, después contrato y mapeo, luego la resolución por referencia externa en la sincronización, y finalmente la documentación y los tests.
5. Aplicar el tratamiento de `429` con `Retry-After` y máximo de tres reintentos, y verificar que la feature no consulta proveedores distintos de Football-Data.org.
6. Ejecutar controles, verificar OpenAPI y seguir la guía de validación. La ejecución de los tests queda a cargo del usuario.

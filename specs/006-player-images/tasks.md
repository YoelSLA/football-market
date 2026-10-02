# Tareas: Imágenes de jugadores

**Entrada**: `specs/006-player-images/spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/api.md`, `quickstart.md` y `checklists/requirements-quality.md`.

**Organización**: Las tareas se agrupan por historia de usuario, en el orden de prioridad de `spec.md`. Los tests nuevos usan `@ActiveProfiles("test")`, agrupan todos sus casos en clases `@Nested` y evitan `this`, conforme a `docs/backend/testing.md`. Los tests de Controller documentan con Spring REST Docs los casos HTTP verificados y los errores gestionados usan `ErrorResponseDTO`. Las pruebas técnicas de persistencia, migración, paginación, retención y concurrencia van en `repositories/`, no en `services/`.

**Formato**: `[ID] [P?] [US?] Descripción con ruta`. `[P]` indica que la tarea puede avanzar en paralelo con otras del mismo tramo, siempre completas sus dependencias explícitas. No se marca `[P]` cuando dos tareas tocan el mismo archivo.

**Sobre la ejecución de tests**: conforme a la Constitución, el agente no ejecuta tests. Las tareas de creación y modificación de tests pertenecen al agente; su ejecución y la verificación de resultados quedan a cargo del usuario. No existen tareas para ejecutar tests, build, Spotless, CI ni SonarQube. El frontend queda exento de testing: no se crean tareas de tests de frontend ni infraestructura para ellos.

**Alcance excluido**: no se modifica `V1`, `V2` ni `V3`; el provider `THE_SPORTS_DB` ya existe en `backend/src/main/java/footballmarket/models/enums/PlayerProvider.java` y **no se vuelve a agregar**. No hay tarea para crear ese enum ni para añadir `THE_SPORTS_DB` a `PlayerProvider`.

**Sin Orchestrator**: no se crea `PlayerImageSynchronizationOrchestrator` ni ninguna otra capa `footballmarket.orchestrators` en esta feature, porque solo delegaría. El flujo principal es `DTO → Mapper → Model → Service → Model → Mapper → DTO` y la recuperación del arranque se invoca desde la configuración contra `PlayerImageRunRecoveryService`. Los contadores y la clasificación `COMPLETED`/`PARTIAL`/`FAILED` viven en `PlayerImageSyncRun`, `PlayerImageSyncCounters` y `PlayerImageSynchronizationServiceImpl`.

**Semántica a preservar sin reinterpretar**: separación identidad/imágenes; referencia `THE_SPORTS_DB` persistente incluso sin imagen; `imageUrl` y `fallbackImageUrl` con prioridad `strCutout` → `strThumb`; validación HTTPS y hostname; sin lookup adicional tras una búsqueda que ya resolvió identidad; sin rematching nunca, incluso con `force=true`; matching por nombre principal/alternativo, `Soccer` obligatorio, equipo coincidente o equipo distinto con fecha y nacionalidad exactas; sin fuzzy ni `relevance`; pertenencia al run determinada por `active` al comenzar la evaluación individual; `processed` solo con intento real; un único control global de intervalo ≥2500 ms con default 2500 ms; `429` con `Retry-After` válido o espera ≥60 s; tres reintentos tras el inicial; contadores `evaluated`/`processed`/`found`/`notFound`/`retryableErrors`/`failed`/`conflicts`/`skippedFound`/`skippedRetryWindow`/`skippedFailed`/`interrupted`; `NOT_FOUND` no degrada, conflicto sí; `FAILED` de run solo por fallo global; recuperación `INTERRUPTED_BY_RESTART`; una ejecución activa por instancia con `409`; paginación de auditoría con `page=0`, `size=20`, máximo 100 como decisión de diseño.

---

## Fase 1: Preparación

**Objetivo**: Confirmar la base existente antes de ampliar backend y frontend.

- [X] T001 Verificar en `backend/build.gradle.kts` que están disponibles Spring Web, Spring Data JPA, Spring Security, Validation, Flyway, SpringDoc, REST Docs, Mockito y Testcontainers; no añadir dependencias salvo que falte una realmente requerida, justificándolo.
- [X] T002 Confirmar en `backend/src/main/java/footballmarket/config/SecurityConfig.java` que la autenticación vigente cubre `/api/players/images/sync` y `/api/players/images/sync-runs/**` con JWT válido y sin exigir rol adicional; no añadir roles.
- [X] T003 Confirmar que `backend/src/main/java/footballmarket/models/enums/PlayerProvider.java` ya contiene `THE_SPORTS_DB` y que `PlayerExternalReference` ya lo admite; no modificar el enum en esta feature.
- [X] T004 Revisar `backend/src/main/resources/application.yml`, `application-dev.yml` y `backend/src/test/resources/application-test.yml` para ubicar el espacio de propiedades de `TheSportsDB` y de imágenes sin versionar credenciales; solo lectura en esta fase.

**Punto de control**: no hay trabajo estructural pendiente en el repositorio; la ampliación es aditiva.

---

## Fase 2: Base compartida

**Objetivo**: Persistencia, modelo, configuración, datos de matching e integración con TheSportsDB que requieren todas las historias.

**Bloqueo**: Completar esta fase antes de las historias de usuario.

### Modelo y persistencia

- [X] T005 Crear `backend/src/main/resources/db/migration/V4__player_image_enrichment.sql` sin modificar `V1`–`V3`: añadir `players.fallback_image_url`, crear `player_image_resolutions` 1:1 con `players` y `last_attempt_at`, crear `player_image_sync_runs` con estado, `force`, `failure_reason` y los once contadores, crear `player_image_sync_run_items` con FK a run y jugador, `UNIQUE (run_id, player_id)`, resultado, motivo de omisión, flags y `finished_at`, e índices de elegibilidad e historial.
- [X] T006 Ampliar `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` con el recorrido de **todos** los jugadores por identificador ascendente, sin `Pageable` ni paginación offset, para que la sincronización pueda evaluar cada jugador activo en su turno individual sin instantánea global; no añadir lógica de negocio.
- [X] T007 Crear `backend/src/main/java/footballmarket/models/enums/PlayerImageResolutionStatus.java` con `PENDING`, `FOUND`, `NOT_FOUND`, `RETRYABLE_ERROR` y `FAILED`.
- [X] T008 [P] Crear `backend/src/main/java/footballmarket/models/enums/PlayerImageSyncRunStatus.java` con `RUNNING`, `COMPLETED`, `PARTIAL` y `FAILED`.
- [X] T009 [P] Crear `backend/src/main/java/footballmarket/models/enums/PlayerImageSyncRunItemResult.java` con `FOUND`, `NOT_FOUND`, `RETRYABLE_ERROR`, `FAILED`, `SKIPPED` e `INTERRUPTED`.
- [X] T010 [P] Crear `backend/src/main/java/footballmarket/models/enums/PlayerImageSkipReason.java` con `FOUND`, `RETRY_WINDOW` y `FAILED`.
- [X] T011 Crear `backend/src/main/java/footballmarket/models/PlayerImageResolution.java` con invariantes de estado, `lastAttemptAt` y transiciones sin `Builder` ni `@Setter`.
- [X] T012 Crear `backend/src/main/java/footballmarket/models/PlayerImageSyncRun.java` con invariantes de estados, contadores y `failureReason`, exponiendo la duración derivable.
- [X] T013 Crear `backend/src/main/java/footballmarket/models/PlayerImageSyncRunItem.java` con invariantes de resultado, motivo de omisión y relación con run y jugador.
- [X] T014 [P] Crear `backend/src/main/java/footballmarket/models/records/PlayerImageSyncCounters.java` con los once contadores y su aritmética, garantizando que `conflicts` es subconjunto causal de `failed` y que `interrupted` no suma a `failed`.
- [X] T015 [P] Crear `backend/src/main/java/footballmarket/models/records/PlayerImageSyncSummary.java` con identificador, `force`, timestamps, `durationMillis`, `failureReason`, estado y contadores.
- [X] T016 [P] Crear `backend/src/main/java/footballmarket/models/records/PlayerImageSyncRunItemRecord.java` con el detalle por jugador que el contrato expone.
- [X] T017 Ampliar `backend/src/main/java/footballmarket/models/Player.java` con `fallbackImageUrl` nullable y operaciones de dominio para fijar ambas URLs: prioridad principal/secundaria, promoción de la secundaria cuando solo ella es válida, no duplicado si son iguales y conservación de URLs previas válidas ante respuesta sin sustitución.
- [X] T018 Crear `backend/src/main/java/footballmarket/repositories/PlayerImageResolutionRepository.java` con lectura de resolución, creación de `PENDING` para altas, listado de resoluciones elegibles por estado y antigüedad de `NOT_FOUND`, y registro de `lastAttemptAt`.
- [X] T019 [P] Crear `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunRepository.java` con creación, cierre por estado, contadores incrementales, runs `RUNNING` huérfanos, historial paginado estable y retención.
- [X] T020 [P] Crear `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunItemRepository.java` con items por run paginados y orden estable, items inconclusos para recuperación y borrado conjunto con el run.

**Restricción de orden**: T005 precede a T006–T013 y a T017, porque Hibernate valida el esquema al iniciar con `ddl-auto=validate`.

### Datos controlados de matching

- [X] T021 [P] Definir el conjunto controlado de sufijos de equipo y alias de nacionalidad en una fuente de datos única y revisable —por ejemplo `backend/src/main/resources/footballmarket/matching/player-identity-aliases.yml` junto con su record en `backend/src/main/java/footballmarket/models/records/PlayerIdentityAliases.java`— con sufijos Soccer conocidos como `FC`, `CF`, `AC` y `SC` y una lista pequeña y explícita de alias de países equivalentes. No incluir mapeos arbitrarios entre clubes ni expansión automática, y no permitir la relevance del proveedor.

### Configuración e integración externa

- [X] T022 Crear `backend/src/main/java/footballmarket/config/TheSportsDbProperties.java` con `apiKey`, `baseUrl`, `requestIntervalMs` con default y mínimo 2500 ms, `maxRetries` con default 3 reintentos posteriores al inicial, `imageRetryDays` con default 30 y `auditRetentionDays` configurable sin default funcional, validando valores en el arranque.
- [X] T023 Crear `backend/src/main/java/footballmarket/config/TheSportsDbClientConfig.java` con `RestClient` sobre HTTPS, timeouts de conexión y lectura, base validada y clave de la API v1 gratuita en el segmento de ruta exigido por TheSportsDB; no usar `X-API-KEY` (exclusivo de v2/premium), no registrar ni exponer la clave en logs, errores o respuestas y no configurar reintentos automáticos.
- [X] T024 Crear `backend/src/main/java/footballmarket/integrations/TheSportsDbRequestPacer.java` como control global único por instancia, compartido por búsqueda, lookup y reintentos: ningún inicio de solicitud antes del intervalo configurado, rechazo de valores inferiores a 2500 ms y espera adicional por `Retry-After` válido o ≥60 s ante `429`, sin ventana móvil adicional y con reloj y espera controlables en tests.
- [X] T025 Crear `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` con búsqueda de candidatos solo cuando falta la referencia, lookup directo por `externalId` cuando existe, reutilización de identidad e imágenes de la misma respuesta de búsqueda sin lookup adicional, adaptación de las raíces `player` y `players`, raíz nula como respuesta válida sin resultados, validación de respuestas y traducción de errores técnicos.
- [X] T026 Crear `backend/src/main/java/footballmarket/integrations/exceptions/TheSportsDbUnavailableException.java`, `TheSportsDbRateLimitException.java` e `InvalidTheSportsDbConfigurationException.java` como `IntegrationException` del proyecto.
- [X] T027 Añadir a `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` la validación de URLs de imagen: esquema `https` y hostname exactamente `thesportsdb.com` o subdominio real suyo, sin coincidencias por substring, sin credenciales embebidas y sin petición adicional para comprobar el recurso.
- [X] T028 Registrar en `backend/src/main/resources/application.yml` y `application-dev.yml` las propiedades de TheSportsDB e imágenes mediante variables de entorno, con valores de prueba en `backend/src/test/resources/application-test.yml` y sin credenciales versionadas.
- [X] T029 [P] Crear `backend/src/main/java/footballmarket/services/exceptions/PlayerImageSynchronizationException.java` y `PlayerImageRunNotFoundException.java` como `ApplicationException` del proyecto.
- [X] T030 [P] Crear `backend/src/main/java/footballmarket/services/exceptions/PlayerImageResolutionPersistenceException.java` como `PersistenceException` para fallos de escritura de auditoría y datos del jugador.

**Punto de control**: esquema, modelo, repositorios, datos de matching, configuración e integración disponibles para las cuatro historias.

---

## Fase 3: Historia de usuario 1 — Reconocer al jugador por su imagen (P1, MVP)

**Objetivo**: Mostrar el retrato persistido en la tarjeta con alternativa e ícono genérico, sin consultar el proveedor desde el frontend.

**Prueba independiente**: Con jugadores con imagen principal, con solo alternativa, sin imagen y con URL que no carga, la tarjeta muestra retrato, alternativa o ícono genérico en ese orden sin perder el nombre, y ninguna llamada del frontend apunta a TheSportsDB. Requiere solo `GET /api/players` con JWT válido.

### Tests

- [X] T031 [P] [US1] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para comprobar que `GET /api/players` expone `imageUrl` y `fallbackImageUrl` siempre presentes, nullable, sin referencias externas, resoluciones ni auditoría, y actualizar las fuentes de snippets REST Docs.
- [X] T032 [P] [US1] Crear `backend/src/test/java/footballmarket/models/PlayerImageTest.java` para la política de dos URLs: prioridad, promoción de la secundaria, no duplicado, ausencia y conservación de URLs válidas previas.

### Implementación

- [X] T033 [P] [US1] Ampliar `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java` con `fallbackImageUrl` nullable y anotaciones OpenAPI en español, y adaptar `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` a los nueve campos públicos.
- [X] T034 [US1] Ampliar `frontend/src/features/players/types/dtos.ts` y `frontend/src/features/players/types/models.ts` con `fallbackImageUrl` y adaptar `frontend/src/features/players/players.mapper.ts`.
- [X] T035 [US1] Implementar en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` la cadena `imageUrl` → `fallbackImageUrl` al fallar la carga real en el navegador → ícono genérico de `frontend/src/features/players/assets/Player Generic-Icon.png`, sin llamadas al proveedor.
- [X] T036 [P] [US1] Ajustar `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` para que el retrato sea responsive y se muestre completo, proporcional y sin recorte destructivo.

**Punto de control**: US1 es entregable con datos ya persistidos, sin ninguna ejecución de sincronización.

---

## Fase 4: Historia de usuario 2 — Resolver identidad y enriquecer imágenes manualmente (P1)

**Objetivo**: Persistir identidad e imágenes mediante una sincronización manual autenticada, con matching conservador y sin rematching.

**Prueba independiente**: Con proveedor simulado, jugadores con y sin referencia `THE_SPORTS_DB`, con y sin imágenes y con `force=true`, se comprueban referencias, estados, URLs y solicitudes; no hay llamadas al proveedor para inactivos al comenzar su evaluación.

### Tests

- [X] T037 [P] [US2] Crear `backend/src/test/java/footballmarket/models/PlayerIdentityMatcherTest.java` como test unitario del helper puro de dominio, con nombre principal y alternativo equivalentes, normalización de nombres y equipos, sufijos y alias del conjunto controlado de T021, fecha exacta, `Soccer` obligatorio, rechazo por relevancia, equipo coincidente con validadores opcionales, equipo distinto con fecha y nacionalidad obligatorias, y evaluación de cero, uno y varios candidatos válidos.
- [X] T038 [P] [US2] Crear `backend/src/test/java/footballmarket/integrations/TheSportsDbIntegrationTest.java` con respuestas simuladas de búsqueda, lookup, raíces `null`, candidato único sin imagen, respuestas permanentemente inválidas, timeouts, errores de red y `5xx`, y con la validación de URLs: rechazo de esquemas no HTTPS, de otros dominios y de coincidencias por substring, y ausencia de petición adicional para comprobar el recurso. El tratamiento de `429`, `Retry-After`, reintentos e intervalo pertenece a US3.
- [X] T039 [P] [US2] Crear `backend/src/test/java/footballmarket/services/PlayerImageSynchronizationServiceTest.java` para exclusión no bloqueante con rechazo de una segunda invocación, elegibilidad, pertenencia por `active` al comenzar la evaluación individual, recorrido sin instantánea global, exclusión sin item ni contadores, `evaluated` y `processed` observables en el resumen, identidad persistida sin imagen, lookup vacío que deja `NOT_FOUND` conservando referencia e imágenes, identidad esperada sin imágenes nuevas que conserva `FOUND` con imágenes previas, refresh sin borrado de URLs válidas, identidad consultada con datos presentes incompatibles que deja `FAILED` conservando referencia e imágenes, ausencia de datos opcionales que no implica contradicción, clasificación del estado final del run y conservación de resultados ya confirmados ante fallo global. Preparar estados funcionales solo mediante APIs públicas de Services, sin acceso directo a Repository/DB. El item único y la reparación de `Player` sin resolución se verifican en T041.
- [X] T041 [P] [US2] Crear `backend/src/test/java/footballmarket/repositories/PlayerImagePersistenceTest.java` con Testcontainers/PostgreSQL y Flyway para el recorrido de todos los jugadores por identificador ascendente sin offset, alta conjunta de resolución, conservación de imágenes, `UNIQUE (run_id, player_id)`, referencia externa persistente sin imagen, incrementalidad por jugador y ausencia de items huérfanos. Preparar directamente en esta prueba de persistencia un jugador activo sin `PlayerImageResolution`, ejecutar el flujo público de sincronización con proveedor simulado y comprobar que se crea la resolución `PENDING` antes del intento y queda persistida con su resultado, sin añadir APIs productivas para preparar el caso.
- [X] T042 [P] [US2] Crear `backend/src/test/java/footballmarket/repositories/PlayerImageMigrationTest.java` para V3→V4 con base poblada y vacía, columnas, restricciones, índices y backfill de resoluciones `PENDING`.

### Implementación

- [X] T043 [P] [US2] Crear `backend/src/main/java/footballmarket/models/PlayerIdentityMatcher.java` como helper de dominio reutilizable para las reglas puras de matching, con los alias y sufijos controlados de T021 recibidos como datos, sin acceso a Repository, Integration, Service ni infraestructura. Evaluar todos los candidatos y aceptar exactamente uno válido; no crear `TheSportsDbPlayerMatchingService`.
- [X] T044 [US2] Crear `backend/src/main/java/footballmarket/services/PlayerImageSynchronizationService.java` y `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` con exclusión no bloqueante por instancia antes de crear el run (segunda invocación rechazada lanzando `PlayerImageSyncInProgressException` de T050, traducida a `409` solo en la frontera HTTP) y recorrido individual sobre el acceso de T006: lectura de `active` al comenzar la evaluación, apertura de un único item para activos, elegibilidad según estado y ventana, exclusión de inactivos sin item ni contadores ni solicitud, y cierre del item con motivo explícito de omisión.
- [X] T045 [US2] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` la resolución de identidad aplicando directamente las reglas puras de `PlayerIdentityMatcher` (T043): búsqueda sin referencia, lookup por `externalId` con referencia, validación de la identidad consultada contra el jugador local sin buscar candidatos alternativos ni repetir matching, persistencia de la referencia aunque falte imagen, y tratamiento de colisión de unicidad `(provider, external_id)` como `FAILED` individual con `conflicts` sin abortar el recorrido.
- [X] T046 [US2] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` la aplicación de imágenes con la política de `strCutout` y `strThumb`, validación de URL, conservación de URLs válidas previas y distinción entre respuesta válida sin resultados, identidad esperada sin imágenes nuevas y refresco con sustitución.
- [X] T047 [US2] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` el cierre del run y la clasificación de su estado final en `COMPLETED`, `PARTIAL` o `FAILED` según los resultados individuales o el fallo global, apoyándose en las invariantes de `backend/src/main/java/footballmarket/models/PlayerImageSyncRun.java` (T012) y en los contadores de `backend/src/main/java/footballmarket/models/records/PlayerImageSyncCounters.java` (T014), devolviendo directamente `PlayerImageSyncSummary` a la frontera HTTP. `PlayerImageSyncSummary` es una proyección de auditoría construida por el Service y devuelta hacia `PlayerImageMapper`, no una entidad persistida ni un DTO; el Controller solo la transporta hacia el Mapper. La exclusión no bloqueante por instancia corresponde a este Service (T044) y ninguna capa intermedia decide estados ni contadores. No mantener una transacción abierta durante llamadas o esperas remotas.
- [X] T048 [US2] Crear `backend/src/main/java/footballmarket/controllers/PlayerImageController.java` con `POST /api/players/images/sync` sin body, parámetro `force` con default `false`, respuesta DTO del resumen y documentación OpenAPI completa de `200`, `401`, `409` y error de ejecución, delegando directamente en `PlayerImageSynchronizationService` con el flujo `DTO → Mapper → Model → Service → Model → Mapper → DTO`, sin Orchestrator. Esta tarea es la fuente de la documentación de este endpoint.
- [X] T049 [P] [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerImageSyncRunResponseDTO.java` y `PlayerImageSyncCountersResponseDTO.java`, y `backend/src/main/java/footballmarket/controllers/mappers/PlayerImageMapper.java` como `final`, sin estado, constructor privado y métodos `static`.
- [X] T050 [P] [US2] Crear `backend/src/main/java/footballmarket/services/exceptions/PlayerImageSyncInProgressException.java` como `ApplicationException` lanzable por `PlayerImageSynchronizationServiceImpl` (T044), con código HTTP estable `PLAYER_IMAGE_SYNC_IN_PROGRESS` asignado únicamente en su manejo `409` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`; el Service no depende del Controller.
- [X] T051 [US2] Extender en `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` el alta de jugadores de Football-Data.org para crear la resolución `PENDING` en la misma operación, sin consultar TheSportsDB y sin sobrescribir `imageUrl` ni `fallbackImageUrl`.
- [X] T052 [P] [US2] Registrar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` y `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` el inicio, el resultado con contadores, los descartes con motivo y los fallos con causa general segura, sin clave, encabezados de autenticación, URLs completas ni respuesta cruda.
- [X] T053 [US2] Crear `backend/src/test/java/footballmarket/controllers/PlayerImageControllerTest.java`, único archivo de tests del Controller de imágenes, con `POST /api/players/images/sync` en `200` para `COMPLETED` y `PARTIAL`, `409` sin segunda ejecución, `401` sin autenticación, error de ejecución sin pérdida de resultados previos, JSON exacto del resumen y fuentes de snippets REST Docs. T063 ampliará este mismo archivo, por lo que ambas tareas son secuenciales.

**Punto de control**: US2 puede validarse con proveedor simulado sin consumir cuota real.

---

## Fase 5: Historia de usuario 3 — Reintentar sin perder resultados (P2)

**Objetivo**: Reintentos, errores transitorios y permanentes, ritmo de peticiones y conservación de resultados válidos.

**Prueba independiente**: Simulan `429`, `5xx`, timeout, error permanente, conflicto y fallo global; se comprueban intentos, esperas, estados `RETRYABLE_ERROR` y `FAILED`, elegibilidad posterior y conservación de datos, sin esperas reales.

### Tests

- [X] T054 [P] [US3] Ampliar `backend/src/test/java/footballmarket/services/PlayerImageSynchronizationServiceTest.java`, sin crear una clase de test nueva, con tres reintentos tras el intento inicial por defecto, tope configurable, `RETRYABLE_ERROR` elegible de inmediato en la siguiente ejecución normal, errores permanentes no reintentables, `FAILED` reintentable solo con `force`, separación entre la ventana de `NOT_FOUND` y los retries dentro de una ejecución, conservación de datos válidos ante `RETRYABLE_ERROR`, respuestas válidas sin resultados que no son error técnico ni disparan reintentos, e invalidez de imagen que no equivale a error permanente.
- [X] T055 [P] [US3] Crear `backend/src/test/java/footballmarket/integrations/TheSportsDbRequestPacerTest.java` para separación mínima de 2500 ms entre inicios, rechazo de valores inferiores, espera por `Retry-After` válido y por el máximo entre `Retry-After` e intervalo, espera ≥60 s sin cabecera utilizable y aplicación del mismo control a búsquedas, lookups, reintentos y refrescos.

### Implementación

- [X] T056 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` la política de reintentos dentro de la ejecución: máximo configurable de reintentos posteriores al inicial, clasificación de errores transitorios y permanentes, actualización de `lastAttemptAt` por intento real y registro de `RETRYABLE_ERROR` o `FAILED` sin borrar referencias ni URLs válidas.
- [X] T057 [US3] Aplicar en `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` el tratamiento de `429` con `Retry-After` en segundos o fecha HTTP mediante el pacer, y asegurar que ningún reintento evita el intervalo mínimo ni el control global.
- [X] T058 [US3] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java` la conservación de resultados y auditoría ante fallo global, con el run en `FAILED` y la respuesta HTTP de error correspondiente.
- [X] T059 [P] [US3] Añadir el manejo del error técnico del proveedor con código estable en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`, sin exponer URL, encabezados ni respuesta cruda.

**Punto de control**: US3 puede validarse sin esperas reales mediante reloj y espera controlados.

---

## Fase 6: Historia de usuario 4 — Consultar la auditoría de imágenes (P2)

**Objetivo**: Historial, resumen y detalle paginados, y recuperación de ejecuciones huérfanas tras reinicio.

**Prueba independiente**: Con ejecuciones `COMPLETED`, `PARTIAL` y `FAILED`, una recuperación por reinicio y una retención aplicada en entorno aislado, el historial, el resumen y el detalle son coherentes con los contadores y no quedan items huérfanos.

### Tests

- [X] T060 [P] [US4] Crear `backend/src/test/java/footballmarket/services/PlayerImageAuditServiceTest.java` con comportamiento de Service: resumen de una ejecución sin items embebidos, ausencia de run como `ApplicationException`, coherencia de contadores con los items entregados por los colaboradores y defaults de paginación expuestos al repositorio.
- [X] T061 [P] [US4] Crear `backend/src/test/java/footballmarket/services/PlayerImageRunRecoveryServiceTest.java` para afirmar solo lo observable mediante `recover()` (éxito e idempotencia, incluso ante un run abierto preparado mediante Services públicos); sin `PlayerImageAuditService`, Repository ni DB en ese test. Verificar en `backend/src/test/java/footballmarket/repositories/PlayerImagePersistenceTest.java`, donde se puede preparar y consultar directamente el estado, que los runs `RUNNING` cierran como `FAILED` con `INTERRUPTED_BY_RESTART`, los items inconclusos como `INTERRUPTED`, los terminados quedan intactos, `interrupted` no suma a `failed` y una segunda recuperación no modifica lo persistido. Mantener por separado `backend/src/test/java/footballmarket/config/PlayerImageStartupConfigurationTest.java` para comprobar el disparador al arrancar y el fallo de startup si la recuperación falla. No añadir API productiva ni atajos de test.
- [X] T062 [P] [US4] Ampliar `backend/src/test/java/footballmarket/repositories/PlayerImagePersistenceTest.java` con la paginación técnica de historial y detalle con `page=0` y `size=20` por defecto, `size` entre 1 y 100, orden estable, y con la retención configurable que elimina runs e items conjuntamente sin tocar resoluciones ni referencias.
- [X] T063 [US4] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerImageControllerTest.java`, creado en T053, con los tres GET de auditoría: `200` con metadatos de paginación, `401` sin autenticación, `400` por paginación inválida, `404` de run inexistente, separación entre resumen y detalle y fuentes de snippets REST Docs.

### Implementación

- [X] T064 [P] [US4] Crear `backend/src/main/java/footballmarket/services/PlayerImageAuditService.java` y `backend/src/main/java/footballmarket/services/impl/PlayerImageAuditServiceImpl.java` con historial paginado, resumen de una ejecución y detalle paginado por jugador, con orden estable y sin items en el resumen.
- [X] T065 [US4] Crear `backend/src/main/java/footballmarket/services/PlayerImageRunRecoveryService.java` y `backend/src/main/java/footballmarket/services/impl/PlayerImageRunRecoveryServiceImpl.java` con la recuperación de runs `RUNNING` al arrancar, el cierre `INTERRUPTED` de items inconclusos y la conservación de los terminados. Si la recuperación no puede completarse, debe fallar propagando `PlayerImageResolutionPersistenceException` (T030) para que el arranque no continúe con runs `RUNNING` inconsistentes, sin tragarse la excepción ni dejar el fallo registrado como arranque correcto.
- [X] T066 [US4] Añadir en `backend/src/main/java/footballmarket/config/PlayerImageStartupConfiguration.java` el disparador de arranque y la invocación de `PlayerImageRunRecoveryService`, sin Orchestrator intermedio, sin llamadas al proveedor y sin lógica de negocio en la configuración. Si la recuperación de runs huérfanos falla, el arranque debe fallar propagando el error, ya envuelto en `PlayerImageResolutionPersistenceException` (T030), sin registrar el fallo y continuar en silencio con runs `RUNNING` inconsistentes. El mecanismo concreto que invoca la recuperación al iniciar el contexto es decisión de implementación de esta tarea y no altera este comportamiento.
- [X] T067 [US4] Cablear en `backend/src/main/java/footballmarket/config/PlayerImageStartupConfiguration.java` únicamente los beans, properties y repositorios que necesita la recuperación, sin el disparador de arranque de T066, sin lógica de dominio ni de coordinación. Al no ser `Config` un rol arquitectónico, esta dependencia se rige por `docs/architecture.md` §1 y no por la matriz de roles de `docs/backend/architecture.md` §1.3.
- [X] T068 [US4] Añadir en `backend/src/main/java/footballmarket/controllers/PlayerImageController.java` los endpoints `GET /api/players/images/sync-runs`, `GET /api/players/images/sync-runs/{id}` y `GET /api/players/images/sync-runs/{id}/items` con validación estructural de paginación y documentación OpenAPI completa de cada respuesta. Esta tarea es la fuente de la documentación de estos endpoints.
- [X] T069 [P] [US4] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerImageSyncRunPageResponseDTO.java`, `PlayerImageSyncRunItemPageResponseDTO.java` y `PlayerImageSyncRunItemResponseDTO.java`, y ampliar `backend/src/main/java/footballmarket/controllers/mappers/PlayerImageMapper.java` para el detalle por jugador.
- [X] T070 [P] [US4] Añadir `PlayerImageRunNotFoundException` con código estable `PLAYER_IMAGE_SYNC_RUN_NOT_FOUND` y su manejo `404` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`.
- [X] T071 [US4] Implementar en `backend/src/main/java/footballmarket/services/impl/PlayerImageAuditServiceImpl.java` la retención configurable de runs, eliminando también sus items y sin afectar resoluciones, referencias ni imágenes.
- [X] T072 [US4] Añadir el manejo HTTP de `InvalidTheSportsDbConfigurationException` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` para el error de configuración que impide admitir una ejecución.

**Punto de control**: US4 puede validarse con ejecuciones reales de las fases previas, sin llamadas al proveedor.

---

## Fase 7: Pulido y controles comunes

**Objetivo**: Mantener contratos, documentación y límites coherentes.

- [X] T073 Revisar la coherencia entre la documentación OpenAPI de `backend/src/main/java/footballmarket/controllers/PlayerImageController.java` y `contracts/api.md`, sin reescribir lo ya documentado en T048 y T068; corregir solo discrepancias.
- [X] T074 [P] Revisar `backend/src/docs/asciidoc/index.adoc` para incluir los snippets REST Docs de los endpoints nuevos. No editar ni generar snippets: su generación ocurre con la ejecución de los tests a cargo del usuario.
- [X] T075 [P] Revisar `docs/backend/technologies.md` y confirmar que no se incorporate ninguna dependencia nueva; si algún inventario cambia, reflejarlo en la misma tarea conforme a la Constitución.
- [ ] T076 Solicitar al usuario la salida de los tests del backend definidos en las fases anteriores y de los controles que le corresponden (Spotless, CI, SonarQube con su Quality Gate) y registrar los fallos preexistentes y verificablemente ajenos al cambio. Esta tarea no incluye ejecutar esos controles: conforme a la Constitución, el agente no ejecuta tests y solo podría ejecutar el build sin tests del área modificada.

**Seguimiento T076 (2026-10-02)**: Pendientes las salidas del usuario de tests backend, builds, Spotless, CI y SonarQube/Quality Gate. Aún no se recibieron resultados; no se clasifican fallos sin evidencia. Mantener T076 sin marcar hasta recibirlos y registrarlos.

---

## Dependencias y orden de ejecución

```text
Preparación (T001–T004)
  → Base compartida: migración T005 → repositorio T006 → modelo T007–T017
      repositorios T018–T020 → datos de matching T021
      configuración e integración T022–T030
    → US1 retrato y fallback (T031–T036)
      → US2 identidad e imágenes (T037–T039, T041–T053)
        → US3 reintentos y ritmo (T054–T059)
          → US4 auditoría y recuperación (T060–T072)
            → Pulido y controles (T073–T076)
```

- US1 depende de la base compartida porque el contrato de `GET /api/players` depende de `fallbackImageUrl` en el modelo y de la migración T005.
- US2 depende de US1 solo por compartir `Player` y el contrato del catálogo; la Matching Service, la Integration y la Synchronization Service no dependen de la UI.
- US3 reutiliza la Integration, el pacer y la Synchronization Service de US2; no introduce contratos nuevos. T054 amplía el archivo creado en T039, por lo que es secuencial.
- US4 depende de los runs e items de US2 y del tratamiento de errores de US3, porque el detalle expone `finalState`, `result` e `interrupted`. T063 amplía el archivo creado en T053 y T062 el creado en T041: ambas son secuenciales.
- **Orden obligatorio**: T005 antes de T006–T013 y T017, porque Hibernate valida el esquema al iniciar con `ddl-auto=validate`; `V1`–`V3` no se modifican.
- T006 precede a T044, porque el recorrido individual se apoya en ese acceso sin paginación offset.
- T021 precede a T037 y T043. T022 precede a T023, T024, T025 y T028. T024 precede a cualquier envío real al proveedor.
- T043 precede a T045; T050 (excepción de Service) precede a T044, mientras que el manejo HTTP de T050 se completa con T048. T044 precede a T045–T047. T012, T014 y T044–T047 definen en Model/Service la exclusión, los contadores y la clasificación final; T045 aplica directamente el helper de Model de T043, sin Service de matching ni Orchestrator. T047 precede a T048–T049. T051 es independiente de T043–T050 y puede avanzar en paralelo.
- El caso de reparación de T041 depende de T044–T046; el resto de T041 puede prepararse junto con los demás tests de US2. T039 no prepara estados mediante Repository/DB.
- T053 precede a T063, y T055 precede a T057.
- T064 precede a T068, T069, T071 y T072. T065 precede a las pruebas de recuperación de T061 y a T067, que precede a T066: el cableado de beans debe existir antes de que el disparador de arranque los use. T066 y T067 son secuenciales porque escriben el mismo archivo `PlayerImageStartupConfiguration.java` y por ello T067 no se marca `[P]`. La recuperación del arranque se invoca desde esa configuración directamente contra `PlayerImageRunRecoveryService`, sin capa intermedia, y su fallo hace fallar el arranque. La prueba de Config de T061 se escribe junto con T066 y T067; las comprobaciones persistidas de T061 se escriben en el archivo de T062 sin cruzar la frontera de la prueba de Service.
- T076 depende de que todos los tests de las fases anteriores estén escritos; su ejecución corresponde al usuario.

## Ejemplos de trabajo paralelo

- **Base compartida**: T008, T009, T010, T014, T015, T016, T021, T029 y T030 se escriben en archivos distintos y avanzan en paralelo. T007 bloquea a T011–T013 solo por el enum de estados.
- **US1**: T031, T032 y T033 se preparan en archivos distintos. T034 precede a T035; T036 es independiente del resto del código de UI.
- **US2**: T037–T039, T041 (salvo reparación) y T042 se preparan en archivos distintos. La reparación de T041 requiere T044–T046. T043, T048, T049, T050, T051 y T052 avanzan en paralelo una vez listas sus dependencias. T044–T047 son secuenciales.
- **US3**: T054 y T055 se preparan en archivos distintos; T056–T058 son secuenciales. T059 es independiente de T056–T058.
- **US4**: T060 y la paginación de T062 se preparan en archivos distintos; las pruebas de recuperación de T061 requieren T065 y amplían también el archivo de T062. T064 y T065 se implementan en paralelo; T069 es independiente. La prueba de Config de T061 se escribe junto con T066 y T067, sin mezclarla con la prueba de Service; T065 → T067 → T066 es secuencial por dependencia y por compartir archivo. T068, T070, T071 y T072 dependen de T064. T070 y T072 tocan `GlobalExceptionHandler.java`, por lo que T072 es secuencial respecto de T070.

## Estrategia de implementación

1. Completar preparación y base compartida: migración, acceso de recorrido, modelo, repositorios, datos de matching, configuración e integración.
2. Implementar y validar US1 como MVP: retrato persistido con alternativa e ícono genérico.
3. Implementar US2: sincronización manual, identidad durable, imágenes y endpoint de ejecución.
4. Implementar US3: reintentos, clasificación de errores y control global de ritmo.
5. Implementar US4: auditoría consultable y recuperación tras reinicio.
6. Cerrar contratos y documentación, y entregar los controles al usuario.

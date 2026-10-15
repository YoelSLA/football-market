---
description: "Tareas de implementación de identidad externa WhoScored — SPEC-008"
---

# Tasks: Identidad externa de jugadores en WhoScored

**Entrada**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [HTTP](contracts/http.md), [WhoScored](contracts/whoscored.md), [quickstart.md](quickstart.md) y [evaluación de Checklists](checklists/evaluation.md).

**Fecha**: 2026-10-14. Primera generación para `008-whoscored-player-identity`: no existe avance anterior en este archivo que reemplazar. Los IDs del alcance estadístico abandonado son antecedentes y no se trasladan a estas tareas.

**Preparación**: Specify, Clarify, Plan y Checklists acreditados; general 16/16 e identidad 40/40. Tasks generado. Analyze re-ejecutado el 2026-10-14 tras las correcciones de redacción y de reconciliación de contadores: sin CRITICAL ni HIGH, solo LOW editoriales. Spec `Ready for implementation`; READY vigente. IMPLEMENT AUTHORIZATION humana de US01 y T001–T011 recuperada del comentario `6acfcdb97ffd8424435e091e` de https://trello.com/c/DqhO6kpg. IMPLEMENT de T001–T035 iniciado y completado técnicamente en esta ejecución; CODE REVIEW y TESTING humanos de US01 aprobados el 2026-10-14. T036–T045 autorizadas por IMPLEMENT AUTHORIZATION humana de US02 (comentario `6acff0797ad21c03f5af047e`) e implementadas técnicamente el 2026-10-14; CODE REVIEW y TESTING humanos aprobados el 2026-10-14 (comentarios `6ad0377ce9a05542a8dd2df1` y `6ad038c6199650ee939dfc4d` de https://trello.com/c/HGkmY1Le); US02 **DONE**. T046–T050 autorizadas por IMPLEMENT AUTHORIZATION humana (`6ad0395bf67af41c389cb7c6`) e implementadas el 2026-10-14, con tests de T046 pendientes de ejecución humana y CONVERGE no ejecutado. Seguir [workflow SDD](../../docs/development/sdd-workflow.md).

**Identidad Git**: rama `feat/008-whoscored-player-identity` creada y seleccionada el 2026-10-14 desde `origin/dev` (`8b3b292`), sin stash ni descarte de cambios locales. La rama anterior `feat/008-whoscored-player-statistics` corresponde al alcance estadístico abandonado. Conservar los cambios locales existentes, incluido `PlayerRepository.java`.

**Tests**: creación/modificación obligatoria de tests backend conforme a [testing](../../docs/backend/testing.md). Su ejecución corresponde exclusivamente al usuario; no ejecutar tests ni hooks que los disparen. No hay frontend en este alcance. Todos los métodos de prueba estarán en grupos `@Nested`, con `@DisplayName` español, tipos explícitos, Arrange/Act/Assert/Verify pertinentes y sin `this` ni `var`.

**Organización**: Setup y Fundacional habilitan US1 y US2 y requieren autorización compartida explícita al implementar. US1 corresponde a `SPEC-008 | US01`; US2 a `SPEC-008 | US02`. US03 fue retirada y no recibe fase ni tareas. Las Tasks viven aquí y no se convierten automáticamente en tarjetas Trello.

## Formato y convenciones de rutas

- `- [ ] Tnnn [P?] [USn?] Descripción con ruta exacta`.
- `[P]` identifica escritura paralela en archivos disjuntos, una vez satisfechos los prerrequisitos del lote; no autoriza subagentes ni ejecución de tests.
- Rutas desde la raíz: producción `backend/src/main/`, tests `backend/src/test/`, documentación HTTP fuente `backend/src/docs/asciidoc/index.adoc`.
- Las clases nuevas nombradas son destinos previstos, no capacidades ya existentes. Mantener paquetes por rol, DTO records, Mapper estático y Services con interfaz/implementación. Model contiene invariantes/matching; Integration encapsula protocolo; Repository no decide identidades.
- Referencia funcional única de equivalencias: **SPEC-008 — Catálogo normativo de alias de equipos**, en [spec.md](spec.md#catálogo-normativo-de-alias-de-equipos). Tabla fija, sin inferencias ni versiones dinámicas.

## Phase 1: Setup — configuración y fixtures compartidas

**Propósito**: reutilizar Java 21, Spring Boot, RestClient/JDK, Jackson, PostgreSQL/Flyway y tooling de tests existentes. No inicializar otro proyecto ni incorporar dependencias.

- [X] T001 [P] Crear fixtures deterministas actual/final/vacía/histórica y respuestas inválidas en `backend/src/test/resources/whoscored/identity/catalog-current.json`, `backend/src/test/resources/whoscored/identity/catalog-current-final.json`, `backend/src/test/resources/whoscored/identity/catalog-empty.json`, `backend/src/test/resources/whoscored/identity/catalog-historical.json` y `backend/src/test/resources/whoscored/identity/catalog-invalid.json`, derivadas de `specs/008-whoscored-player-identity/evidence/whoscored-catalog-contract.md`; distinguir la muestra fuera de rango de una primera página vacía válida, conservar paging coherente y no copiar secretos ni depender de Internet.
- [X] T002 [P] Crear `backend/src/main/java/footballmarket/config/WhoScoredProperties.java` y configurar `backend/src/main/resources/application.yml` y `backend/src/test/resources/application-test.yml` con base HTTPS, User-Agent observado configurable sin CR/LF, page-size 1–100, intervalo 2500 ms, conexión 5000 ms, lectura 20000 ms, máximo 10000 páginas y 5242880 bytes; definir cinco bindings de IDs internos distintos a tournament IDs 2/3/4/5/22, sin inventar IDs de la DB ni confundirlos con Football-Data, con valores de testing aislados y validación antes de red.
- [X] T003 Crear `backend/src/main/java/footballmarket/config/WhoScoredClientConfig.java` con RestClient dedicado y cliente JDK, timeouts configurados, redirecciones deshabilitadas y User-Agent explícito; habilitar properties de T002, permitir proveedor controlado exclusivamente en tests y no reenviar JWT ni modificar converters/clientes globales.

## Phase 2: Fundacional — persistencia e invariantes compartidas

**Propósito**: representación durable necesaria para los dos pases. Bloquea el trabajo funcional de ambas historias; no incluye scheduler, métricas deportivas ni consultas HTTP de auditoría.

- [X] T004 Agregar `WHOSCORED` en `backend/src/main/java/footballmarket/models/enums/ExternalProvider.java` y crear `backend/src/main/java/footballmarket/models/enums/PlayerIdentityPassType.java`, `backend/src/main/java/footballmarket/models/enums/PlayerIdentityRunStatus.java`, `backend/src/main/java/footballmarket/models/enums/PlayerIdentityLeagueStatus.java` y `backend/src/main/java/footballmarket/models/enums/PlayerIdentityItemResult.java` con estados de `data-model.md`, incluidos LOCAL_DATA_ERROR, DISCOVERY_ERROR, PERSISTENCE_ERROR e INTERRUPTED.
- [X] T005 [P] Proteger `Player.addExternalReference()` en `backend/src/main/java/footballmarket/models/Player.java` y validar identidad WhoScored canónica en `backend/src/main/java/footballmarket/models/PlayerExternalReference.java`: misma referencia idempotente, otra WhoScored rechazada sin cambios parciales, sin restringir la multiplicidad de otros proveedores; reutilizar `backend/src/main/java/footballmarket/models/exceptions/InvalidPlayerException.java` para el rechazo de dominio.
- [X] T006 [P] Crear `backend/src/main/java/footballmarket/models/PlayerIdentitySyncRun.java` y `backend/src/main/java/footballmarket/models/records/PlayerIdentitySyncCounters.java` con origen CURRENT/HISTORICAL, tiempos, estados, cantidades no negativas e invariantes de cierre de `data-model.md` §2/6; derivar finalización del trabajo y nunca de un umbral de cobertura.
- [X] T007 [P] Crear `backend/src/main/java/footballmarket/models/PlayerIdentityLeagueDiscovery.java` y `backend/src/main/java/footballmarket/models/PlayerIdentityDiscardedRow.java` con ámbito local/externo capturado, transiciones, paging, diagnóstico sanitizado y auditoría de descarte por página/posición/motivo; no confundir descartes con candidatos, jugadores o ligas fallidas.
- [X] T008 [P] Crear `backend/src/main/java/footballmarket/models/WhoScoredIdentityCandidate.java`, `backend/src/main/java/footballmarket/models/PlayerIdentitySyncItem.java` y `backend/src/main/java/footballmarket/models/records/WhoScoredIdentityContext.java` con contextos tipados, snapshot local nullable para defectos, referencias iniciales múltiples y decisiones de `data-model.md` §4/5; mapear JSONB mediante tipo JDBC JSON de Hibernate, validar pertenencia de candidatos al run y evitar cascade desde catálogo mutable.
- [X] T009 Crear `backend/src/main/resources/db/migration/V6__create_player_identity_audit.sql` con tablas/secuencias de T006–T008, FKs a run/origen, índices y unicidades por run/liga, run/player y run/externalId; agregar `uk_player_external_references_whoscored_player` parcial para WHOSCORED manteniendo `(provider, external_id)`, sin ampliar cardinalidad de otros proveedores ni introducir compatibilidad para datos de desarrollo. **Antes de escribir**: revalidar que V6 sigue libre, porque el trabajo en paralelo puede haberla ocupado desde la última verificación (V1–V5 al 2026-10-14); si aparece ocupada, renumerar esta migración y actualizar las referencias de schema de T006–T008 tras el merge con `dev`, sin alterar requisitos.
- [X] T010 Crear `backend/src/main/java/footballmarket/repositories/PlayerIdentitySyncRunRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerIdentityLeagueDiscoveryRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerIdentitySyncItemRepository.java`, `backend/src/main/java/footballmarket/repositories/WhoScoredIdentityCandidateRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerIdentityDiscardedRowRepository.java`; agregar en `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerExternalReferenceRepository.java` consultas de snapshot por cinco leagueId internos con team/league/referencias materializables, locking breve de Player y propietario por proveedor/ID, sin filtros active/current nuevos ni modificar consultas ajenas.
- [X] T011 Crear excepciones propias en `backend/src/main/java/footballmarket/services/exceptions/PlayerIdentitySynchronizationException.java`, `backend/src/main/java/footballmarket/services/exceptions/PlayerIdentityAuditUnavailableException.java`, `backend/src/main/java/footballmarket/services/exceptions/PlayerIdentityRunNotFoundException.java`, `backend/src/main/java/footballmarket/services/exceptions/PlayerIdentitySourceNotEligibleException.java`, `backend/src/main/java/footballmarket/integrations/exceptions/WhoScoredIntegrationException.java` y `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPlayerIdentityIdException.java`, heredando la categoría correspondiente de FootballMarketException y transportando códigos/metadatos sanitizados sin body, JWT ni causas sensibles en respuestas.

**Checkpoint**: esquema y contratos internos preparados para US1; su evidencia de comportamiento se escribe en las tareas siguientes. No acredita build, ejecución de tests ni autorización de IMPLEMENT.

## Phase 3: User Story 1 — Identificar jugadores desde el catálogo actual (P1) · MVP

**Objetivo**: POST actual autenticado que recorra las cinco ligas, compare globalmente nombres/equipos y registre referencias seguras, auditoría y resumen sin alterar el catálogo.

**Criterio independiente**: con catálogo y proveedor controlados, comprobar los 16 escenarios de US1: coincidencias únicas por nombre+equipo/alias, cero asociaciones por nombre solo, ambigüedad entre ligas independiente del orden, conflictos sin sobrescritura, filas descartadas, snapshot inicial, aislamiento de fallos y reintento/concurrencia idempotentes. Resumen y estado por liga se reconcilian con auditoría; cero estadísticas/cotizaciones.

### Tests backend de US1 — escribir antes de implementar comportamiento

Estas tareas son escritura de casos contract-first usando el diseño y las fixtures disponibles. No requieren ejecución ni suite verde antes de implementación; no ejecutar tests. Los detalles no expuestos por API pública de Service se demuestran en Model/Repository, sin añadir APIs productivas solo para tests.

- [X] T012 [P] [US1] Escribir `backend/src/test/java/footballmarket/models/WhoScoredIdentityMatcherTest.java` para FR-005–007/027/029 y la tabla normativa: NFD/marcas solo de base LATIN, conservación no latina/ø/ß/apóstrofes/puntos, Pd y White_Space, vacíos/signos, alias cerrados y colisiones inversas, nombre+equipo del mismo contexto, homónimos, varios IDs globales, deduplicación y varios propietarios locales sin elección por orden; unitarios puros sin Spring/DB.
- [X] T013 [P] [US1] Ampliar `backend/src/test/java/footballmarket/models/PlayerExternalReferenceTest.java` con idempotencia WhoScored, rechazo de segundo ID sin mutación, canonicalización y regresión de varias referencias permitidas para Football-Data/TheSportsDB; no probar getters o CRUD trivial.
- [X] T014 [P] [US1] Escribir `backend/src/test/java/footballmarket/models/PlayerIdentityAuditModelTest.java` para origen, transiciones y tiempos, pertenencia de contextos/items, resultados locales exclusivos, referencias iniciales múltiples y estados COMPLETED/PARTIAL/FAILED/vacío y RUNNING como estado interno no serializado; NO_MATCH/AMBIGUOUS/CONFLICT/LOCAL_DATA_ERROR y descartes no degradan por sí solos COMPLETED, fallo de liga sí.
- [X] T015 [P] [US1] Escribir `backend/src/test/java/footballmarket/config/WhoScoredConfigurationTest.java` con ApplicationContextRunner/contexto mínimo para binding/startup, límites inválidos, User-Agent vacío/CRLF, cinco competiciones/bindings distintos, HTTPS/redirects/timeouts y cliente dedicado; no DB ni perfil test cuando las properties sean explícitas.
- [X] T016 [P] [US1] Escribir `backend/src/test/java/footballmarket/integrations/WhoScoredIntegrationTest.java` con cliente real y servidor controlado para path/query exactos, playerId vacío, isCurrent=true, User-Agent y ausencia de JWT, JSON text/html, fin por totalPages, primera página vacía válida, índices/totales estables, filas originales vs descartadas, IDs numéricos/cadenas/ceros y teamId opcional; cubrir HTML, 403/429/redirects/timeouts, límites, sin progreso, errores sanitizados y cero retries.
- [X] T017 [P] [US1] Escribir `backend/src/test/java/footballmarket/integrations/WhoScoredRequestPacerTest.java` con tiempo/espera controlados para intervalo entre inicios, permiso único compartido por instancia, concurrencia, interrupción y Retry-After sin repetir la solicitud fallida ni sleeps reales dependientes del reloj.
- [X] T018 [P] [US1] Escribir `backend/src/test/java/footballmarket/repositories/PlayerIdentityPersistenceTest.java` con PostgreSQL Testcontainers/Flyway para snapshot coherente y consultas/locks/constraints propios, JSONB tipado, auditoría durable sin cascada al catálogo, agregados por resultado, contextos solo de ligas completas, descartes y referencias múltiples preexistentes; probar carreras de mismo/distinto ID y rollback referencia+item en categoría Repository, sin trasladar acceso DB a tests Service.
- [X] T019 [P] [US1] Escribir `backend/src/test/java/footballmarket/services/PlayerIdentitySynchronizationServiceTest.java` con @SpringBootTest/perfil test, Service/DB reales e Integration mock: preparar catálogo vía PlayerCatalogService y sus contratos Model productivos, obtener IDs internos por su API y configurar bindings de testing, actuar/afirmar solo por la interfaz del Service bajo prueba; cubrir actual, globalidad, fallos de liga, vacíos, LOCAL_DATA_ERROR, contadores, repetición y nuevo run, verificando ausencia de transacción durante Integration sin acceso Repository/JPA/SQL ni métodos productivos exclusivos para tests.
- [X] T020 [P] [US1] Escribir `backend/src/test/java/footballmarket/services/PlayerIdentityAuditServiceTest.java` con Service/DB reales y perfil test para resumen interno durable, unidades y contadores reconciliados según FR-019 y `pendingPlayers` en cero al cierre, sin red; preparar runs por la API de sincronización en Arrange y actuar/afirmar solo por PlayerIdentityAuditService, sin leer detalles vía Repository ni inventar endpoints de consulta.
- [X] T021 [P] [US1] Escribir `backend/src/test/java/footballmarket/controllers/PlayerIdentityControllerTest.java` con @WebMvcTest, cadena Security real y Service mock para POST actual, DTO exacto COMPLETED/PARTIAL/FAILED/vacío, errores 500 SYNC_FAILED/AUDIT_UNAVAILABLE y 401 sin JWT/expirado/firma inválida; assertions antes de REST Docs para cada respuesta contractual, sin acceso a Repository ni asumir ErrorResponseDTO en el filtro JWT.
- [X] T022 [P] [US1] Escribir el journey actual en `backend/src/test/java/footballmarket/e2e/PlayerIdentityE2ETest.java` con servidor RANDOM_PORT, perfil test/PostgreSQL Testcontainers y proveedor controlado atravesando Integration real; autenticar por flujo existente, usar RestClient y DTO productivos, comprobar resumen actual/repetición con nuevo ID y conservación observable del catálogo, sin JSON manual ni Internet.

### Implementación de US1

- [X] T023 [US1] Crear `backend/src/main/java/footballmarket/models/WhoScoredIdentityNormalizer.java` y `backend/src/main/java/footballmarket/models/WhoScoredTeamAliases.java` con pipeline exacto FR-005, validación de nombres y pares de la tabla **SPEC-008 — Catálogo normativo de alias de equipos**; validar colisiones normalizadas antes de run/red, sin modificar TeamNameNormalizer ni inferir equivalencias o versiones.
- [X] T024 [US1] Crear `backend/src/main/java/footballmarket/models/WhoScoredIdentityMatcher.java` con decisión global sobre snapshots y candidatos de ligas completas: priorizar referencias iniciales únicas/múltiples antes de atributos, LOCAL_DATA_ERROR para inválidos sin referencia, conservar candidatos por nombre, exigir contexto completo nombre+equipo, clasificar NO_MATCH/AMBIGUOUS/CONFLICT y detectar varios propietarios antes de escribir; sin edad/posición/región, liga local restrictiva ni orden como desempate.
- [X] T025 [US1] Crear `backend/src/main/java/footballmarket/integrations/WhoScoredRequestPacer.java` con ritmo compartido por instancia, tiempo/espera controlables y Retry-After para posteriores requests, un permiso por request e interrupción controlada; no reintentar ni mantener transacciones/locks de persistencia durante espera.
- [X] T026 [US1] Crear `backend/src/main/java/footballmarket/integrations/WhoScoredIntegration.java` y contratos separados en `backend/src/main/java/footballmarket/integrations/records/WhoScoredCatalogPage.java`, `backend/src/main/java/footballmarket/integrations/records/WhoScoredPlayerIdentity.java`, `backend/src/main/java/footballmarket/integrations/records/WhoScoredDiscardedIdentityRow.java` y `backend/src/main/java/footballmarket/integrations/records/WhoScoredPaging.java`; usar RestClient/pacer, leer bytes acotados y parsear JSON sin depender de MIME, validar páginas y cada fila según contrato, entregar identidades/descartes/metadatos sin métricas ni Model/DTO compartidos, y traducir fallos sin retries ni datos sensibles.
- [X] T027 [US1] Crear captura inicial interna en `backend/src/main/java/footballmarket/services/internal/PlayerIdentitySnapshotCapture.java`: validar existencia/coherencia de cinco bindings y alias antes de red, registrar run CURRENT nuevo, ámbitos y todos los snapshots locales elegidos en transacción REPEATABLE_READ breve; incluir persistidos sin filtros active/current, materializar valores/referencias sin lazy posterior y no guardar entidades detached por merge.
- [X] T028 [US1] Crear recorrido interno en `backend/src/main/java/footballmarket/services/internal/PlayerIdentityLeagueDiscoveryProcessor.java`: recorrer secuencialmente las cinco ligas fuera de transacción usando Integration, validar continuidad/totales/huellas/límites antes de publicar una liga completa, persistir metadatos y descartes con transacciones breves y deduplicar candidatos/contextos globalmente; liga fallida no publica parciales, y espera/indisponibilidad que impida continuar marca restantes incompletas sin solicitudes ni falsos NO_MATCH.
- [X] T029 [US1] Crear resolución interna en `backend/src/main/java/footballmarket/services/internal/PlayerIdentityResolutionProcessor.java` tras finalizar descubrimiento de todas las ligas: adaptar datos a Model, aplicar matcher fuera de transacción, distinguir ausencia en liga local completa de DISCOVERY_ERROR en fallida, conservar evidencia de candidatos por nombre y resolver conflictos de múltiples locales antes de invocar confirmación; jamás asociar desde una liga incompleta ni sobrescribir snapshot.
- [X] T030 [US1] Crear confirmación interna en `backend/src/main/java/footballmarket/services/internal/PlayerIdentityReferenceConfirmation.java` con REQUIRES_NEW por jugador y lock breve: recargar referencias/propietario, guardar referencia+RESOLVED atómicamente, clasificar misma concurrente como ALREADY_REFERENCED y distinta/otro dueño como CONFLICT; tras rollback leer/auditar en otra transacción, discriminar SQLSTATE 23505 y constraint estructurada de fallos técnicos, registrar PERSISTENCE_ERROR y continuar otros commits, sin revalidar nombre/equipo ni usar merge del snapshot.
- [X] T031 [US1] Implementar agregación interna en `backend/src/main/java/footballmarket/services/PlayerIdentityAuditService.java`, `backend/src/main/java/footballmarket/services/impl/PlayerIdentityAuditServiceImpl.java` y `backend/src/main/java/footballmarket/models/records/PlayerIdentitySyncSummary.java` usando agregados durables de los Repositories de T010: selectedPlayers reconcilia resultados exclusivos, discoveredUnique cuenta IDs de ligas completas, providerErrors cuenta ligas y discardedRows cuenta filas; exponer resumen interno sin API HTTP, red ni fallback al proveedor, y lanzar AUDIT_UNAVAILABLE ante fallo de lectura.
- [X] T032 [US1] Implementar fachada CURRENT en `backend/src/main/java/footballmarket/services/PlayerIdentitySynchronizationService.java` y `backend/src/main/java/footballmarket/services/impl/PlayerIdentitySynchronizationServiceImpl.java` componiendo T027–T030 y Model/Repositories para cierre durable y retorno de resumen; no consumir PlayerIdentityAuditService ni otro Service, no @Transactional alrededor de red/matching, generar ID nuevo por invocación, COMPLETED vacío sin red, preservar commits previos, marcar pendientes interrumpidos en interrupción controlada y abortar con error si auditoría no persiste, sin scheduler/recuperación de runs de otra instancia.
- [X] T033 [US1] Crear records de respuesta en `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerIdentitySyncRunResponseDTO.java`, `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerIdentitySyncCountersResponseDTO.java`, `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerIdentityLeagueDiscoveryResponseDTO.java` y `backend/src/main/java/footballmarket/controllers/mappers/PlayerIdentityMapper.java` estático DTO↔Model; reflejar todos los campos/unidades/nulos de HTTP, sin serializar `RUNNING` ni calcular finish, con `finishedAt` siempre presente, sin cálculos de dominio, métricas ni detalle de auditoría por jugador.
- [X] T034 [US1] Crear `backend/src/main/java/footballmarket/controllers/PlayerIdentityController.java` con solo POST `/api/players/identities/whoscored/sync` en este incremento, invocación directa Service→Mapper, seguridad JWT persistida existente y OpenAPI español completo; ampliar `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` para errores propios 500/códigos contractuales, preservando timestamp/casos ajenos y sin agregar rol, body, selección arbitraria de ligas ni GET de auditoría.
- [X] T035 [US1] Documentar contrato actual mediante includes de REST Docs en `backend/src/docs/asciidoc/index.adoc`, incluyendo respuestas 200 y errores de T021, campos/counters/unidades y finalización independiente de cobertura; editar solo AsciiDoc fuente, no snippets ni `index.html` generado.

**Checkpoint MVP**: actual funcional y verificable sin histórico, con auditoría y resumen propios. US1 completa técnicamente solo después del build permitido y coherencia de sus tests escritos; CODE REVIEW y TESTING humanos siguen siendo gates separados.

## Phase 4: User Story 2 — Resolver pendientes mediante el catálogo histórico (P2)

**Objetivo**: segundo POST vinculado a un actual terminado que procese solo NO_MATCH/AMBIGUOUS aún sin referencia, con snapshot nuevo y reglas de identidad compartidas.

**Criterio independiente**: preparar un actual registrado mediante US1 con pendientes, conflictos, errores y ya referenciados; histórico solo modifica pendientes elegibles, deduplica contextos históricos y conserva auditoría actual. Origen inexistente produce 404; HISTORICAL/RUNNING/FAILED o actual sin fin, 409; cero elegibles produce 200 COMPLETED vacío sin red. Una ejecución actual PARTIAL con fin es válida.

### Tests backend de US2 — ampliación sobre archivos de US1

- [X] T036 [P] [US2] Ampliar `backend/src/test/java/footballmarket/integrations/WhoScoredIntegrationTest.java` para isCurrent=false, repetición de ID con equipos/temporadas/variantes distintos, deduplicación canónica y ausencia de progreso real frente a contextos históricos nuevos; mantener reglas de paging y no exigir seasonName/temporada vigente ni estadísticas.
- [X] T037 [P] [US2] Ampliar `backend/src/test/java/footballmarket/services/PlayerIdentitySynchronizationServiceTest.java` con los seis escenarios US2, origen CURRENT COMPLETED/PARTIAL, nuevo snapshot al histórico, exclusión de resueltos después del actual/CONFLICT/LOCAL_DATA_ERROR/errores y resultado vacío sin Integration; preparar mediante Services públicos, afirmar retorno del Service probado y conservar frontera sin DB funcional directa.
- [X] T038 [P] [US2] Ampliar `backend/src/test/java/footballmarket/repositories/PlayerIdentityPersistenceTest.java` para consulta de elegibles NO_MATCH/AMBIGUOUS aún existentes y sin referencia, exclusión de múltiples referencias, origen/fin, preservación exacta del run/items/candidatos actual, nuevo snapshot histórico y carreras antes de confirmación; demostrar estado durable y constraints con DB real solo en esta categoría.
- [X] T039 [P] [US2] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerIdentityControllerTest.java` para POST histórico con resumen/sourceRunId, 200 vacío/parcial/fallido, 400 tipo/ID no positivo, 404 origen ausente, 409 no elegible, 500 y 401; verificar rechazos previos sin Service donde corresponda, documentar todas las respuestas contractuales por REST Docs tras assertions y sin GET adicional.
- [X] T040 [P] [US2] Ampliar `backend/src/test/java/footballmarket/e2e/PlayerIdentityE2ETest.java` con journey actual→histórico→repetición, usando IDs devueltos, RestClient/DTO productivos, DB e Integration reales/proveedor controlado; afirmar solo pendientes resueltos, sourceRunId, resumen reconciliado y ausencia de duplicados/modificación observable del catálogo, sin duplicar exhaustivamente pruebas de capa.

### Implementación de US2

- [X] T041 [US2] Agregar selección histórica por sourceRunId en `backend/src/main/java/footballmarket/repositories/PlayerIdentitySyncItemRepository.java`, `backend/src/main/java/footballmarket/repositories/PlayerIdentitySyncRunRepository.java` y `backend/src/main/java/footballmarket/repositories/PlayerRepository.java`: resultados de origen exclusivamente NO_MATCH/AMBIGUOUS, jugadores todavía existentes y sin ninguna referencia WhoScored; mantener consultas actuales y no incluir errores/conflictos por filtros aproximados.
- [X] T042 [US2] Ampliar `backend/src/main/java/footballmarket/services/internal/PlayerIdentitySnapshotCapture.java` para validar origen CURRENT con finishedAt y status COMPLETED/PARTIAL, fallar 404/409 antes de red, capturar nuevo run HISTORICAL/sourceRunId y atributos/referencias locales en REPEATABLE_READ; no modificar snapshots ni resultados del origen, y crear cero items para excluidos.
- [X] T043 [US2] Ampliar `backend/src/main/java/footballmarket/services/PlayerIdentitySynchronizationService.java` y `backend/src/main/java/footballmarket/services/impl/PlayerIdentitySynchronizationServiceImpl.java` con pase histórico, reutilizando procesadores y matcher de US1 con isCurrent=false; cero elegibles completa run vacío/ámbitos `SKIPPED` sin red conforme a FR-012/FR-016, cada invocación crea ID propio y protege referencias concurrentes/contextos históricos sin rematching de confirmados ni identificación general.
- [X] T044 [US2] Agregar solo POST `/api/players/identities/whoscored/sync-runs/{id}/historical` en `backend/src/main/java/footballmarket/controllers/PlayerIdentityController.java`, validar Long positivo y documentar OpenAPI, sourceRunId y errores; ampliar `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` con INVALID_PLAYER_IDENTITY_ID, PLAYER_IDENTITY_RUN_NOT_FOUND y PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE, reutilizando INVALID_PARAMETER_TYPE y respuestas DTO sin afectar seguridad/códigos ajenos.
- [X] T045 [US2] Incorporar includes REST Docs del histórico y sus respuestas/errores de T039 en `backend/src/docs/asciidoc/index.adoc`, indicando origen parcial elegible, exclusiones, vacío sin red y resumen devuelto por la propia ejecución, sin fuentes para consultas fuera de alcance ni edición de salidas generadas.

**Checkpoint**: ambas historias técnicamente completas y verificables por separado con prerrequisito US1; no acredita sus gates ni DONE de Feature.

## Phase 5: Polish — coherencia y entrega transversal

**Propósito**: cerrar exclusivamente elementos afectados por esta feature después de US1/US2; no añadir funcionalidad académica futura.

- [X] T046 Revisar y ampliar regresiones afectadas en `backend/src/test/java/footballmarket/models/PlayerExternalReferenceTest.java`, `backend/src/test/java/footballmarket/repositories/PlayerCatalogPersistenceTest.java` y `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` para mantener multiplicidad de otros proveedores y sincronización/presentación del catálogo, sin duplicar cobertura WhoScored ni corregir tests ajenos fuera del impacto.
- [X] T047 [P] Actualizar `specs/008-whoscored-player-identity/quickstart.md` con clases/casos implementados y comandos exactos de tests para el usuario —incluidos Config, Model, Repository y pacer—, configuración de cinco bindings internos obtenidos del catálogo y estados esperados; conservar escenarios SC-001–008, no afirmar ejecución ni carga operativa y no cambiar criterios funcionales de la Spec.
- [X] T048 Revisar y eliminar código muerto introducido en `backend/src/main/java/footballmarket/services/impl/PlayerIdentitySynchronizationServiceImpl.java`, `backend/src/main/java/footballmarket/integrations/WhoScoredIntegration.java` y `backend/src/main/java/footballmarket/controllers/PlayerIdentityController.java`; comprobar Javadoc/idioma, fronteras arquitectónicas, dos POST únicos, ausencia de métricas/secretos/scheduler y coherencia de implementación con `specs/008-whoscored-player-identity/contracts/http.md` y `specs/008-whoscored-player-identity/contracts/whoscored.md`, sin redefinir contratos para justificar discrepancias.
- [X] T049 [P] Actualizar `docs/backend/technologies.md` para reflejar el uso de RestClient/JDK y persistencia JSONB por la integración de identidad WhoScored, sin inventar versiones ni agregar dependencias; mantener sus fuentes efectivas y el resto del inventario tecnológico.
- [X] T050 Verificar entrega de cambios de código backend con `./gradlew build -x test` desde `backend/`, revisar diff final/`git diff --check`/`git status --short` y registrar evidencia de build y tests escritos pendientes del usuario en `specs/008-whoscored-player-identity/tasks.md`; no ejecutar tests, Asciidoctor con tests, Sonar ni hooks incompatibles, ni convertir este control técnico en aprobación de gates.

## Dependencias y orden de ejecución

### Grafo de fases e historias

```text
Catálogo SPEC-002/007 + preparación SDD vigente
                 │
                 ▼
Setup T001–T003 → Fundacional T004–T011
                 │
                 ▼
US1 P1 T012–T035 (MVP actual con auditoría/resumen)
                 │
                 ▼
US2 P2 T036–T045 (histórico sobre origen real)
                 │
                 ▼
Polish T046–T050
```

- US1 no depende de US2. US2 depende de datos/contratos/persistencia de US1; no trabajar sus modificaciones compartidas en paralelo con US1. Tests de US2 preparan el origen mediante el actual ya disponible, no por SQL en tests Service/E2E.
- SPEC-002/007 proveen catálogo/referencias/identidades internas. Son dependencias técnicas, no aceptación acreditada aquí. Bindings concretos de desarrollo son configuración operativa pendiente con validación definida; no inventar valores ni ejecutar carga/arranque durante Tasks.
- Implementación futura exige un Analyze vigente sin blockers y autorizaciones de US/prerrequisitos compartidos. La rama `feat/008-whoscored-player-identity` ya está creada y seleccionada; generar Tasks no la crea ni la cambia.

### Dependencias técnicas dentro de fases

| Lote / tarea | Prerrequisitos y restricciones |
| --- | --- |
| T001 + T002 | Independientes; T003 espera T002. |
| T004 | Setup completo. |
| T005–T008 | T004 completo; archivos disjuntos entre sí. |
| T009 | T005–T008; versión de migración libre y esquema alineado. |
| T010 | T009 y Models completos; T011 después de T010 en orden base. |
| T012–T022 | Fundacional y diseño/fixtures listos; escritura contract-first independiente por archivo, no ejecución de tests. |
| T023 → T024 | Pipeline/tabla antes de matching. |
| T025 → T026 | Cliente/properties y pacer antes de integración completa. |
| T027 → T028 → T029 → T030 | Snapshot antes de descubrimiento; todas las ligas finalizan antes del matching global; decisiones globales antes de confirmar. T028 requiere T026 y T029 requiere T024. |
| T031 → T032 | Agregación durable/Model y procesadores disponibles; fachada no depende del otro Service. Ambos usan Repository/Model sin Service→Service. |
| T033 → T034 → T035 | Resumen antes de DTO/Mapper y frontera HTTP; documentación fuente sobre casos contractuales escritos. |
| T036–T040 | US1 técnico disponible, archivos disjuntos; nunca solapar con T016/T018/T019/T021/T022. |
| T041 → T042 → T043 → T044 → T045 | Elegibles/origen antes de nuevo snapshot, caso de uso y endpoint/documentación. |
| T046–T049 → T050 | Todas las historias implementadas; T047/T049 pueden escribirse en paralelo por rutas disjuntas; control técnico después del último cambio. |

No solapar T005/T013/T046 en sus archivos compartidos, T010/T041 en Repositories, T027/T042 en snapshot, T032/T043/T048 en fachada, T034/T044/T048 en Controller/handler ni T035/T045 en AsciiDoc. Los marcadores `[P]` solo aplican dentro de los lotes explicitados.

## Ejemplos de trabajo paralelo por historia

### US1

Tras T011, escribir en paralelo T012 (Model/matching), T016 (Integration), T019 (Service), T021 (HTTP/REST Docs) y T022 (E2E) en sus cinco archivos disjuntos. El mismo lote admite T013–T015/T017–T018/T020: no hay dependencias de escritura entre ellos. Usar diseño fijo y contratos; no ejecutarlos para adelantar implementación.

### US2

Tras completar US1, escribir en paralelo T036 (protocolo histórico), T037 (caso de uso), T038 (persistencia), T039 (HTTP) y T040 (journey). Son cinco archivos distintos y todas sus versiones de US1 ya están terminadas. Integrar implementación T041–T045 secuencialmente para evitar solapar los responsables compartidos.

### Resumen de oportunidades

24 tareas con `[P]`: Setup 2; Fundacional 4; escritura de tests US1 11; ampliaciones de tests US2 5; documentación transversal 2. Paralelismo técnico de escritura, no delegación automática ni ejecución de suites. Total: 50 tareas; 35 marcadas por implementación/escritura revisada y 15 abiertas de US2/Polish. Distribución original: Setup 3, Fundacional 8, US1 24, US2 10 y Polish 5. Los marcadores de tests no acreditan ejecución.

## Cobertura y verificabilidad

### Requisitos funcionales

| Requisito(s) | Implementación principal | Tests previstos |
| --- | --- | --- |
| FR-001 | T002/T027/T032/T042–T044: dos pases/cinco ligas | T019/T021/T037/T039/T040 |
| FR-002 | T026/T028: paginación completa | T016 |
| FR-003 | T008/T026/T028: IDs/contextos globales | T012/T016/T018/T036 |
| FR-004 | T010/T027/T030: catálogo preservado, sin altas | T018/T019/T022/T046 |
| FR-005–007 | T023/T024/T029: normalización/alias/globalidad | T012/T019 |
| FR-008–009 | T004/T005/T009/T024/T030: referencias protegidas | T013/T018/T019/T046 |
| FR-010–011 | T007/T008/T024/T028/T029: resultados y evidencia | T014/T018/T019 |
| FR-012–013 | T041–T043: elegibilidad histórica/reglas/contextos | T036–T040 |
| FR-014 | T005/T009/T030/T032/T043: idempotencia/concurrencia | T013/T018/T019/T038/T040 |
| FR-015 | T026/T028: respuesta inválida/aislamiento por liga | T016/T018/T019 |
| FR-016 | T006/T028–T032: aislamiento/estado parcial | T014/T018/T019/T021/T037 |
| FR-017 | T002/T003/T025/T026/T028: pacing/límites/sin retries | T015–T017 |
| FR-018 | T027–T030/T032/T042/T043: límites transaccionales | T018/T019/T037 |
| FR-019–020 | T006–T010/T028/T031/T032: auditoría/reconciliación | T014/T018–T021/T038/T039 |
| FR-021 | T031–T035/T043–T045: resumen en los dos POST | T020–T022/T039/T040 |
| FR-022–023 | T003/T011/T026/T033/T034/T044: seguridad/errores/DTO/OpenAPI | T015/T016/T021/T022/T039/T040 |
| FR-024 | T026/T033/T034/T044/T048: solo identidad/dos POST | T016/T021/T022/T039/T040 |
| FR-025 | T008/T027/T029/T030/T042: snapshot inmutable por pase | T018/T019/T038 |
| FR-026 | T027/T032/T042/T043: nuevo run al repetir/interrupción | T018/T019/T022/T040 |
| FR-027 | T023/T024/T027/T029/T041: datos locales inválidos | T012/T014/T018/T019/T037/T038 |
| FR-028 | T007/T026/T028: validación por fila/descartes | T016/T018/T019/T021 |
| FR-029 | T008/T024/T028/T029: variantes/contexto completo/propietarios | T012/T018/T019/T036 |

### Historias y criterios de éxito

| Alcance | Evidencia prevista |
| --- | --- |
| US1 escenarios 1–5/8/9/13/15/16 | T012/T013/T016/T018/T019/T021/T022: nombres/equipos, alias, globalidad, deduplicación, idempotencia y conflictos. |
| US1 escenarios 6/7/10–12/14 | T014/T016/T018/T019: aislamiento de liga/jugador, snapshot, validación local/externa e inconsistencias iniciales. |
| US2 escenarios 1–6 | T036–T040: elegibilidad, referencias/contextos, conservación de origen, errores y vacío. |
| SC-001 | T012/T019: conjunto controlado, 100% de coincidencias únicas válidas y cero por nombre solo/equipo ambiguo. |
| SC-002 | T014/T018/T020/T021: unidades/exclusividad, cobertura informativa sin umbral, denominador cero no aplicable. |
| SC-003 | T013/T018/T019/T022/T040/T046: cero duplicados/sustituciones y otros proveedores preservados. |
| SC-004 | T037/T038/T040: nuevas referencias únicamente del conjunto pendiente elegible. |
| SC-005 | T014/T018/T020/T038: detalle durable y contadores reconciliados; diagnóstico y trazabilidad sin doble conteo. |
| SC-006 | T016/T018/T019: cero asociaciones desde liga inválida y commits previos conservados. |
| SC-007 | T020/T021/T039: resumen por resultado y ámbito sin rediscovery. |
| SC-008 | T016/T021/T022/T039/T040/T048: sin métricas/cotizaciones en persistencia adaptada/respuestas. |

Las pruebas Repository demuestran detalle/constraints/carreras/atomicidad no observables por el retorno Service. Los tests Service usan exclusivamente sus contratos públicos productivos y auxiliares solo en Arrange; no se agrega lectura HTTP ni API de preparación exclusiva de tests. Helpers técnicos de aislamiento reutilizan Testcontainers/perfil test y nunca preparan datos funcionales eludiendo Service. No se prueba Mapper aislado.

## Estrategia de implementación

### MVP primero

1. Un Analyze autorizado posterior debe contrastar estos artefactos y confirmar la reconciliación de contadores antes de READY.
2. Con preparación válida y autorización explícita de US1 más T001–T011, ejecutar Setup → Fundacional → US1 (T012–T035).
3. Mantener configuración, documentación y control técnico necesarios para entregar el incremento; el build backend sin tests corresponde después de cambios de código, incluso al entregar solo MVP.
4. Presentar Code Review Report humano y posteriormente Testing Report con comandos del usuario. Tests escritos/build no acreditan TESTING ni DONE.

### Entrega incremental

US1 aporta actual/auditoría/resumen; US2 agrega histórico sobre pendientes y conserva el incremento anterior. Implementar US2 solo cuando esté autorizada y los prerrequisitos técnicos estén disponibles; no interpretar dependencia como aprobación automática de gates. Trabajo compartido/Polish se autoriza explícitamente según el incremento afectado. Todas las US requeridas DONE y trabajo necesario completo habilitan CONVERGE final; cierre requiere autorización humana final persistida.

### Pendientes humanos y de entorno

- Ejecución de tests backend y sus resultados, revisión de código, aprobación de TESTING y cierre final quedan pendientes del usuario y **no son Tasks técnicas de este archivo**.
- Configuración de IDs internos reales/entorno para una demo y proveedor real no está acreditada. Fixtures controladas satisfacen verificación determinista; el 83% observado no es aceptación ni objetivo obligatorio.
- REQ-S2-02 y requisitos académicos restantes de `tp/enunciado.md`/`tp/entregas.md` —estadísticas, scraping futuro, cotizaciones, caché/scheduler y perfiles unitarios/E2E— conservan alcance fuera de esta feature. No se selecciona aquí una feature adicional del TP.
- Si hay discrepancia funcional o normativa durante implementación, detener y resolver upstream; no modificar Spec/checklists para acomodar código ni ejecutar Analyze/Implement automáticamente desde Tasks.

## Evidencia de esta generación

### IMPLEMENT de US01 — 2026-10-14

- Alcance autorizado T001–T011 → T012–T035 implementado en el diff local de
  `feat/008-whoscored-player-identity`. Sin US02 ni endpoint histórico; T036–T050
  conservan sus marcadores abiertos. No se crearon Tasks nuevas ni se redefinieron requisitos.
- Prerrequisitos reales y autorización verificados antes de escribir; toma en
  comentario `6acfcf9ccbcc87a582a15a6c` de US01, releído. Checklists 16/16 y 40/40
  preservados. `.specify/extensions.yml` ausente antes y después; hooks inspeccionados,
  ninguno ejecutado. `pre-push` dispara tests y no se invocó.
- Build permitido final: `./gradlew build -x test`, ejecutado con Wrapper de Windows
  `.\gradlew.bat build -x test` desde `backend/`: **BUILD SUCCESSFUL**, diez tareas,
  seis ejecutadas y cuatro up-to-date. El intento inicial desde Bash no encontró
  Java; entorno Windows/JDK 21 resuelto. Los fallos intermedios de formato Spotless
  del cambio quedaron corregidos. Gradle informa APIs deprecadas para Gradle 10;
  advertencia del build existente, sin cambiar tooling.
- Tests backend escritos/modificados y revisados estáticamente, **no ejecutados**.
  El build excluye `test` y no acredita compilación/ejecución de la suite. Migración,
  carreras, timeouts, REST Docs y E2E requieren ejecución humana posterior.
- No se consultó al proveedor real ni se ejecutaron cargas/arranque de desarrollo.
  Bindings de desarrollo siguen pendientes del operador; IDs de fixtures/defaults
  test son sintéticos, y las suites funcionales obtienen sus bindings por APIs reales.
- Cambios previos staged/unstaged de PlayerRepository, opencode.json, índice y
  artefactos SDD preservados. Sin commits, push ni edición de salidas generadas.
- Revisión de diff e interfaces afectadas, `git diff --check` y
  `git diff --cached --check` sin diagnósticos en la verificación de entrega.
- La actualización puntual de `docs/backend/technologies.md` cumple Constitution §4
  respecto del uso efectivo del stack; no marca T049 ni ejecuta el resto de Polish.
- [Code Review Report completo de US01](code-review-report-us01.md): inventario,
  funcionamiento, tests, decisiones, orden recomendado y checklist humano.
  Siguiente gate: CODE REVIEW, sin aprobación automática ni habilitación de TESTING.

### CODE REVIEW y TESTING de US01 — 2026-10-14

- Tres defectos detectados por el usuario al ejecutar la suite y al levantar la
  aplicación, corregidos sobre fuentes antes de la revisión: se restauró el
  `@EntityGraph({"team","team.league"})` y el filtro `t.current = true` de
  `PlayerRepository.findByActiveTrue` (500 `LazyInitializationException` en
  `GET /api/players`), se dieron a los cinco `league-id` de `application.yml` el
  default igual a la PK local confirmada (1 Premier, 2 Bundesliga, 3 LaLiga,
  4 Serie A, 5 Ligue 1) manteniendo los `tournament-id` 2/3/4/5/22, y se extrajo
  el `leagueId` con tipo explícito en `PlayerIdentityPersistenceTest`. El comentario
  de `WhoScoredProperties` se ajustó a ese enlace local/provider.
- `.\gradlew.bat build -x test` desde `backend/`: **BUILD SUCCESSFUL** tras las
  correcciones, incluido el ajuste de formato exigido sobre las fuentes. El build
  no acredita la suite.
- CODE REVIEW de US01 **APROBADO** por el revisor humano y persistido como
  `[SDD GATE]` en https://trello.com/c/DqhO6kpg, comentario `6acfedb50ddc77f30a128225`,
  con el addendum de correcciones incorporado al alcance aprobado. La tarjeta de
  US01 pasó de CODE REVIEW a TESTING.
- [Testing Report completo de US01](testing-report-us01.md): inventario de tests,
  cobertura por criterio, dobles y límites, riesgos de falso positivo, orden de
  revisión y comandos exactos para el usuario.
- La suite fue ejecutada por el usuario durante la corrección de los defectos; el
  gate de TESTING sigue **PENDING HUMAN VALIDATION** hasta que el usuario marque su
  checklist del Testing Report. Sin US01 DONE, sin TESTING aprobado y sin CONVERGE.

### Cierre de US01 y habilitación de US02 — 2026-10-14

- TESTING de US01 **APROBADO** por el usuario tras revisar el checklist del
  [Testing Report](testing-report-us01.md), y persistido como `[SDD GATE]` en
  https://trello.com/c/DqhO6kpg, comentario `6acff06965e11eb7bfcc3ad5`. US01 pasa a
  **DONE** (tarjeta movida a DONE y descripción actualizada). La aprobación corresponde
  al diff local revisado; cualquier cambio posterior la invalida.
- IMPLEMENT AUTHORIZATION de **US02** para T036–T045 persistida en
  https://trello.com/c/HGkmY1Le, comentario `6acff0797ad21c03f5af047e`, habilitada por
  el usuario al cerrar US01. La tarjeta permanece en **READY**: la autorización no
  inicia IMPLEMENT y no se registró toma.
- Alcance autorizado de US02, sin implementación en esta sesión: T036–T040 tests
  ampliados (Integration, Service, Repository, Controller y E2E), T041 consulta de
  elegibles NO_MATCH/AMBIGUOUS por `sourceRunId`, T042 captura del pase histórico con
  validación de origen, T043 pase HISTORICAL con `isCurrent=false` reutilizando los
  procesadores de US01, T044 único POST
  `/api/players/identities/whoscored/sync-runs/{id}/historical` y T045 includes REST
  Docs del histórico.
- T046–T050 (Polish, revisión de código muerto, tecnologías y control de entrega)
  siguen sin autorizar. Implement agregado y CONVERGE de la Feature permanecen
  incompletos; FEATURE COMPLETION AUTHORIZATION: PENDING.
- La tarjeta [EPIC] https://trello.com/c/hhBUA2WO conserva IN PROGRESS con el nuevo
  reparto: US01 DONE, US02 READY y autorizada, US03 retirada.

### IMPLEMENT de US02 — 2026-10-14

- Alcance autorizado T036–T045 implementado en el diff local de
  `feat/008-whoscored-player-identity`, con toma persistida y releída en
  `6acff11e49c7738908b62948` de https://trello.com/c/HGkmY1Le y transición
  READY → IN PROGRESS de esa US al comenzar realmente el IMPLEMENT. La Feature
  permanece IN PROGRESS; las aprobaciones de US01 no se modificaron.
- Decisiones de diseño del incremento: la elegibilidad se resuelve en
  `PlayerIdentitySyncItemRepository.findHistoricalPendingPlayerIds` (solo
  `NO_MATCH`/`AMBIGUOUS` del origen) y la existencia real sin ninguna referencia
  WhoScored se filtra en `PlayerRepository.findHistoricalIdentitySnapshot`, sin
  filtros aproximados ni uso de `findIdentitySnapshot`; la captura histórica de
  `PlayerIdentitySnapshotCapture` valida origen `CURRENT` terminado
  `COMPLETED`/`PARTIAL` antes de cualquier red y propaga 404/409 sin envolverlas en
  `PlayerIdentitySynchronizationException`; `WhoScoredIntegration.historicalPage`
  reutiliza request, validación, pacing y límites con `isCurrent=false`; y
  `PlayerIdentityLeagueDiscoveryProcessor` selecciona el método de Integration según
  el tipo de pase, conservando el cierre `SKIPPED` sin red cuando no hay elegibles.
  El Matcher y los procesadores de resolución y confirmación de US01 se reutilizan
  sin duplicación.
- Superficie HTTP: único `POST /api/players/identities/whoscored/sync-runs/{id}/historical`
  con validación de `Long` positivo y `GlobalExceptionHandler` ampliado con
  `INVALID_PLAYER_IDENTITY_ID`, `PLAYER_IDENTITY_RUN_NOT_FOUND` y
  `PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE`, reutilizando `INVALID_PARAMETER_TYPE` y
  sin tocar seguridad ni códigos ajenos.
- Tests escritos y revisados estáticamente, **no ejecutados**: T036 Integration
  (`isCurrent=false`, contextos/temporas y huella), T037 Service (seis escenarios
  US2, origen parcial, snapshot nuevo, exclusión de resueltos y vacío sin red),
  T038 Repository (selección de elegibles, exclusión por referencias múltiples,
  preservación del origen y carrera antes de confirmar), T039 Controller (resumen
  con `sourceRunId`, estados terminados, 400/404/409/500/401 con REST Docs) y
  T040 E2E (journey actual → histórico → repetición con IDs devueltos). El build
  excluye `test` y no acredita compilación ni ejecución de la suite.
- Build permitido `.\gradlew.bat build -x test` desde `backend/`: **BUILD SUCCESSFUL**.
  Un fallo intermedio de formato Spotless sobre el Repository de runs quedó
  corregido en la fuente; Gradle informa APIs deprecadas para Gradle 10,
  advertencia preexistente del build.
- Cambios locales previos de US01 preservados. Sin commits, push, consultas al
  proveedor real, arranque de desarrollo ni ejecución de tests. La reparación de
  codificación de este `tasks.md` tras un `Set-Content` con lectura ANSI quedó
  restituida a UTF-8 sin pérdida de contenido.
- T046–T050 siguen sin autorizar. Implement agregado y CONVERGE permanecen
  incompletos; FEATURE COMPLETION AUTHORIZATION: PENDING.
- [Code Review Report completo de US02](code-review-report-us02.md). Siguiente
  gate: CODE REVIEW de US02, sin aprobación automática ni habilitación de TESTING.

### CODE REVIEW de US02 aprobado y entrada a TESTING — 2026-10-14

- CODE REVIEW de US02 **APROBADO** por el usuario el 2026-10-14 sobre el diff local de
  `feat/008-whoscored-player-identity` con el alcance T036–T045, sus tests incluidos y el
  checklist completo de [code-review-report-us02.md](code-review-report-us02.md).
  Evidencia persistida en https://trello.com/c/HGkmY1Le, comentario
  `6ad0377ce9a05542a8dd2df1`. La tarjeta de US02 pasó de CODE REVIEW a TESTING y su
  descripción refleja el estado vigente.
- [Testing Report completo de US02](testing-report-us02.md): inventario de tests
  (T036–T040 sobre los cinco archivos de US01), cobertura por criterio, dobles y
  límites, riesgos de falso positivo, orden de revisión y comandos exactos para el
  usuario. Estado `PENDING HUMAN VALIDATION`; el agente no ejecutó tests ni build en
  esta operación.
- TESTING de US02 sigue pendiente de que el usuario revise el checklist del Testing
  Report y ejecute la suite. Sin ese gate, US02 no pasa a DONE.
- T046, T047, T048 y T050 permanecen abiertas y sin autorizar; la **Feature no está
  completa**, por lo que no se ejecutó CONVERGE ni se solicitó FEATURE COMPLETION
  AUTHORIZATION. US01 DONE y sus gates no se modifican.
- Índice de trazabilidad actualizado en [specs/trello.md](../trello.md) con el estado
  verificado de la Feature, US01 y US02.

### Cierre de US02 — 2026-10-14

- TESTING de US02 **APROBADO** por el usuario el 2026-10-14: revisión previa de los
  tests y ejecución de la suite con resultado satisfactorio aportado por él. Persistido
  como `[SDD GATE]` en https://trello.com/c/HGkmY1Le, comentario
  `6ad038c6199650ee939dfc4d`, sobre el
  [Testing Report](testing-report-us02.md) ya marcado por el revisor. Con CODE REVIEW
  `6ad0377ce9a05542a8dd2df1` vigente, US02 pasa a **DONE** y su tarjeta quedó en la
  columna DONE con la descripción actualizada.
- El agente no ejecutó tests en esta operación: la evidencia de ejecución es del
  usuario. El build permitido `.\gradlew.bat build -x test` de la fase IMPLEMENT ya
  figuraba como **BUILD SUCCESSFUL** y no se repetió.
- T046, T047, T048 y T050 siguen abiertas y sin autorizar. **La Feature no está
  completa**: no corresponde ejecutar CONVERGE ni solicitar FEATURE COMPLETION
  AUTHORIZATION. US01 DONE y sus gates no se modifican.
- Índice de trazabilidad actualizado en [specs/trello.md](../trello.md) con el estado
  verificado de la Feature, US01 y US02.

### IMPLEMENT de Polish T046–T050 — 2026-10-14

- Alcance autorizado por el usuario el 2026-10-14, persistido como
  `[SDD GATE] IMPLEMENT AUTHORIZATION APPROVED / HUMAN` en
  https://trello.com/c/hhBUA2WO, comentario `6ad0395bf67af41c389cb7c6`. T049 ya estaba
  cerrada por T057 y no se reimplementó.
- **T046**: tres regresiones ampliadas, todas Write-Only y pendientes de ejecución
  humana. `PlayerExternalReferenceTest.preservesOtherProviderCardinalityAlongsideWhoScored`
  comprueba que la identidad WhoScored no restringe la multiplicidad de Football-Data y
  TheSportsDB ni la idempotencia de la WhoScored;
  `PlayerCatalogPersistenceTest.conservaIdentidadWhoScoredYOtrosProveedores` comprueba con
  PostgreSQL real que la resincronización del catálogo conserva la referencia WhoScored,
  la de TheSportsDB y la de Football-Data sin duplicarlas, sin crear otro jugador y sin
  que el índice parcial de `V6` afecte a los demás proveedores;
  `PlayerCatalogServiceTest.unaResincronizacionIdenticaNoAfectaLaPresentacion` comprueba
  por la API del Service que la resincronización idéntica no crea, descarta, inactiva ni
  duplica la presentación. No se corrigieron tests ajenos al impacto.
- **T047**: `quickstart.md` actualizado con las clases realmente implementadas, la tabla
  de casos por clase de test, los comandos exactos en Windows, los fixtures reales de
  `backend/src/test/resources/whoscored/identity/`, la sección de bindings
  `league-id` 1–5 contra `tournament-id` 2/3/4/5/22 con sus variables de entorno, y los
  estados esperados de run y ámbitos. Se retiró la referencia a
  `PlayerIdentityAuditServiceTest`, eliminada por T051. Se conservan los escenarios
  SC-001–008 y no se afirma ninguna ejecución ni carga operativa.
- **T048**: no se encontró código muerto en los tres archivos del alcance. La revisión sí
  corrigió idioma e importacion: `PlayerIdentitySynchronizationServiceImpl` deja de usar
  el nombre completamente cualificado de `PlayerIdentitySourceNotEligibleException` y
  renombra la variable local de interrupción, `WhoScoredIntegration` importa
  `java.util.stream.Collectors` en lugar de usarlo inline, y `GlobalExceptionHandler`
  importa `PlayerIdentityRunNotFoundException` y `PlayerIdentitySourceNotEligibleException`
  en lugar de referenciarlas de forma calificada. Se verificaron los dos POST únicos,
  la ausencia de métricas, secretos y scheduler, y la coherencia de los cuatro códigos de
  error con `contracts/http.md` y del protocolo con `contracts/whoscored.md`. La doble
  validación del ID de origen (Controller y Service) se conserva como defensa en
  profundidad: la prueba de Controller exige el 400 sin invocar el Service.
- **T050**: `.\gradlew.bat build -x test` desde `backend/`: primer intento **BUILD
  FAILED** solo por formato Spotless en las regresiones nuevas; corregido con
  `.\gradlew.bat spotlessApply` sobre las fuentes y segundo intento **BUILD SUCCESSFUL**
  (10 tareas, 5 ejecutadas y 5 up-to-date). Gradle informa APIs deprecadas para Gradle
  10: advertencia preexistente del build. `git diff --check` y
  `git diff --cached --check` sin diagnósticos.
- Tests de T046 **escritos y no ejecutados**: el build excluye `test` y no compila las
  fuentes de prueba, de modo que los tres archivos modificados requieren compilación y
  ejecución por el usuario con `.\gradlew.bat test`. El build no acredita la suite.
- Sin requests al proveedor real, arranque de desarrollo, Sonar ni hooks; sin commits ni
  push; salidas generadas sin editar. Con T001–T050 cerradas, el trabajo técnico
  requerido queda completo y corresponde CONVERGE; FEATURE COMPLETION AUTHORIZATION:
  PENDING.

### Antecedente: generación de Tasks

`setup-tasks.sh --json` real resolvió FEATURE_DIR absoluto `/mnt/c/Users/Vaqui/IdeaProjects/football-market/specs/008-whoscored-player-identity`, AVAILABLE_DOCS `research.md`, `data-model.md`, `contracts/`, `quickstart.md` y TASKS_TEMPLATE absoluto `.specify/templates/tasks-template.md` bajo la raíz resuelta. Se utilizó TASKS_TEMPLATE_CONTENT compuesto. Revisión documental de rutas previstas, formato, cobertura FR-001–029/SC-001–008, dependencias y dos historias vigentes. No acredita Analyze, código ni tests ejecutados.

## Phase 6: Convergence

Hallazgos de `/speckit.converge` sobre el estado del código tras T001–T045. No sustituyen las tareas Polish T046–T050, que siguen abiertas y sin autorizar. No se ejecutaron tests ni build durante esta evaluación.

- [X] T051 Conectar `PlayerIdentityAuditService` a la agregación del resumen de ambos POST en `backend/src/main/java/footballmarket/services/impl/PlayerIdentitySynchronizationServiceImpl.java`, o bien retirar el servicio, el código `PLAYER_IDENTITY_AUDIT_UNAVAILABLE` de `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` y su documentación en `backend/src/docs/asciidoc/index.adoc`, dejando una única fuente de agregación coherente con el contrato HTTP (partial)
- [ ] T052 ~~Interrumpir el recorrido de ligas y registrar como incompletas las ligas restantes sin nuevas solicitudes cuando la respuesta del proveedor sea `PROVIDER_RATE_LIMITED`, `PROVIDER_UNAVAILABLE` o `PROVIDER_BLOCKED`~~ — **CANCELADA el 2026-10-14 por contradicción con US1/AC6**: `spec.md:52` exige que tras una liga bloqueada «pueden confirmarse las demás ligas válidas» y `contracts/whoscored.md:86` solo exige registrar ámbitos restantes sin nuevas solicitudes «si la indisponibilidad o Retry-After impide continuar». Como el pacer respeta `Retry-After`, un bloqueo por liga nunca impide continuar. Se mantiene el comportamiento previo: aislamiento por liga, recorrido de las demás, y corte solo por interrupción de hilo (EXECUTION_INTERRUPTED) o ejecución vacía (SKIPPED sin red)
- [X] T053 Unificar el predicado de validez de nombres externos y locales sobre la misma forma normalizada en `backend/src/main/java/footballmarket/models/WhoScoredIdentityNormalizer.java` y `backend/src/main/java/footballmarket/integrations/WhoScoredIntegration.java`, ajustando los tests de Normalizer e Integration (partial)
- [X] T054 Adjuntar competición y página también a los diagnósticos generados por el pacer en `backend/src/main/java/footballmarket/integrations/exceptions/WhoScoredIntegrationException.java` y `backend/src/main/java/footballmarket/services/internal/PlayerIdentityLeagueDiscoveryProcessor.java`, sin cuerpos crudos (partial)
- [X] T055 Relajar la validación de `backend/src/main/java/footballmarket/config/WhoScoredProperties.java` para exigir cinco identificadores de competición distintos en lugar del conjunto literal `{2,3,4,5,22}`, ajustando los tests de configuración (unrequested)
- [X] T056 Excluir `pendingPlayers` de la reconciliación de cierre de `selectedPlayers` en `backend/src/main/java/footballmarket/models/records/PlayerIdentitySyncCounters.java` y `backend/src/main/java/footballmarket/models/PlayerIdentitySyncRun.java`, conservando su uso exclusivo mientras la ejecución está en curso (partial)
- [X] T057 Reconciliar el marcador de T049 con el contenido ya presente en `docs/backend/technologies.md` sobre RestClient/JDK y persistencia JSONB de la auditoría WhoScored, marcando la tarea o retirando el texto no justificado (partial)

### IMPLEMENT de la fase Convergence — 2026-10-14

- Alcance autorizado por el usuario: T051–T057. No se ejecutaron tareas Polish, ni
  scheduler, ni consultas al proveedor real, ni arranque de desarrollo.
- **T051**: se retiró el servicio de auditoría en lugar de cablearlo, porque `plan.md`
  y T032 prohíben dependencia Service→Service y la agregación ya vive en el cierre
  transaccional de `PlayerIdentitySynchronizationServiceImpl.finish`. Eliminados
  `PlayerIdentityAuditService`, su `impl`, `PlayerIdentityAuditUnavailableException` y
  `PlayerIdentityAuditServiceTest`; retirados el handler `PLAYER_IDENTITY_AUDIT_UNAVAILABLE`,
  sus snippets `identity-current-audit-error`/`identity-historical-audit-error`, sus
  referencias en `index.adoc` y OpenAPI, y la fila equivalente de
  `specs/008-whoscored-player-identity/contracts/http.md`. El único código global 500 de
  ambos POST es `PLAYER_IDENTITY_SYNC_FAILED`. La cobertura de contadores permanece en
  `PlayerIdentitySynchronizationServiceTest` y `PlayerIdentityAuditModelTest`.
- **T052 (cancelada)**: la primera implementación cortaba el recorrido de ligas al primer
  `PROVIDER_RATE_LIMITED`/`PROVIDER_UNAVAILABLE`/`PROVIDER_BLOCKED`. El usuario ejecutó la
  suite y `preservesUnresolvedOutcomesAndFailures` y `skipsRemainingLeaguesWhenProviderBlocks`
  fallaron (`discoveryErrors` 4 en lugar de 1). La revisión del hallazgo mostró que
  ese comportamiento contradice US1/AC6 y `contracts/whoscored.md:86`: un bloqueo aislado
  por liga debe permitir procesar las demás válidas, y el corte solo aplica cuando la
  indisponibilidad o el `Retry-After` impiden continuar, algo que el pacer ya resuelve
  diferando. Se revirtió el corte, se eliminó el test que lo fijaba y se restauró
  `isolatesLeagueFailure` con `PROVIDER_BLOCKED`, añadiendo verificación de que las ligas
  5 y 22 sí se solicitan. El resto de T052 (interrupción de hilo y SKIPPED sin red) queda
  como estaba.
- **T053**: `WhoScoredIntegration.validName` delega en
  `WhoScoredIdentityNormalizer.validName`, de modo que externo y local comparten el
  criterio sobre la forma normalizada de FR-005/FR-027.
- **T054**: `WhoScoredIntegrationException` transporta `competitionId` y `page`, y
  `WhoScoredIntegration.catalogPage` los añade a cualquier fallo del cliente o del
  pacer mediante `withContext`; el procesador persiste la página real del diagnóstico.
  Sin cuerpos ni cabeceras en las respuestas.
- **T055**: `WhoScoredProperties` exige cinco `leagueId` y cinco `tournamentId` positivos
  y distintos, sin fijar el conjunto `{2,3,4,5,22}`; se añadió `tournament-id=0` como
  caso inválido y un test que acepta cinco competiciones distintas.
- **T056**: la reconciliación de `selectedPlayers` excluye `pendingPlayers` y
  `PlayerIdentitySyncCounters.from` rechaza resultados pendientes, de modo que una
  ejecución no puede construirse cerrada con trabajo pendiente; `PlayerIdentitySyncRun.finish`
  mantiene su guardia. Test de modelo actualizado al nuevo punto de rechazo.
- **T057**: T049 marcada como completada; su contenido en `docs/backend/technologies.md`
  (RestClient/JDK y JSONB de la auditoría) estaba ya aplicado y es correcto.
- Build permitido `.\gradlew.bat build -x test` desde `backend/`: **BUILD SUCCESSFUL**
  tras un fallo intermedio de formato Spotless corregido con `spotlessApply` sobre las
  fuentes. Gradle informa APIs deprecadas para Gradle 10: advertencia preexistente.
- Tests backend modificados y **no ejecutados**: el build excluye `test` y no compila
  las fuentes de prueba, por lo que `PlayerIdentityControllerTest`,
  `PlayerIdentitySynchronizationServiceTest`, `PlayerIdentityAuditModelTest`,
  `WhoScoredConfigurationTest` y `WhoScoredIntegrationTest` requieren compilación y
  ejecución por el usuario. Comandos sugeridos: `.\gradlew.bat compileTestJava` y
  `.\gradlew.bat test --tests "footballmarket.controllers.PlayerIdentityControllerTest"`.
- Cambios locales previos preservados. Sin commits, push ni edición de salidas
  generadas. US02 mantiene CODE REVIEW y TESTING pendientes; FEATURE COMPLETION
AUTHORIZATION: PENDING.

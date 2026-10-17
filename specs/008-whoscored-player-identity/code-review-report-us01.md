# Code Review Report — SPEC-008 | US01

## Estado y alcance revisable

Rama: `feat/008-whoscored-player-identity`. Base de revisión: **diff local**, incluidos
archivos nuevos sin seguimiento; no hay commit ni publicación de este incremento.
Alcance: T001–T011 compartidas y T012–T035 de US01. US02 y T036–T050 permanecen
fuera del incremento; sus marcadores no acreditan avance.

Autorización humana recuperada y verificada en https://trello.com/c/DqhO6kpg,
comentario `6acfcdb97ffd8424435e091e`. Toma persistida y releída en
`6acfcf9ccbcc87a582a15a6c`. Feature: https://trello.com/c/hhBUA2WO.
Checklists de requisitos: 16/16 y 40/40, sin cambios de marcadores del revisor.
El script real `check-prerequisites.sh --json --require-tasks --include-tasks`
resolvió `specs/008-whoscored-player-identity` y sus artefactos.

**CODE REVIEW: aprobado por el revisor humano sobre este diff local el 2026-10-14,
incluidas las correcciones del addendum final. TESTING: pendiente de ejecución y
validación humanas.** El addendum final forma parte del alcance aprobado; el
presente reporte no aprueba el gate de TESTING.

## Addendum: correcciones posteriores al reporte (2026-10-14)

Tres defectos detectados al ejecutar la suite y al levantar la aplicación, ambos
corregidos sobre fuentes antes de esta aprobación:

1. `backend/src/main/java/footballmarket/repositories/PlayerRepository.java`:
   `findByActiveTrue` había perdido su `@EntityGraph({"team","team.league"})` y su
   `join p.team t join t.league l ... and t.current = true`. Sin materialización,
   `PlayerMapper.toResponse` desreferenciaba el proxy `Team` fuera de la transacción
   y `GET /api/players` respondía 500 por `LazyInitializationException`. Se restauró
   la declaración previa a este incremento; no cambia el alcance normativo de
   `contracts/http.md`.
2. `backend/src/main/resources/application.yml`: los cinco `league-id` usaban
   `${WHOSCORED_*_LEAGUE_ID}` sin valor por defecto, por lo que el arranque fallaba
   al enlazar un literal a `Long`. Ahora cada `league-id` tiene como default la PK
   local confirmada por el operador (1 Premier League, 2 Bundesliga, 3 LaLiga,
   4 Serie A, 5 Ligue 1) y sigue siendo sobrescribible por entorno; los
   `tournament-id` de WhoScored (2/3/4/5/22) no cambian. Se ajustó el comentario
   de `WhoScoredProperties`, que describía un enlace explícito por variables.
3. `backend/src/test/java/footballmarket/repositories/PlayerIdentityPersistenceTest.java`:
   `transaction().execute(...)` anidado dentro de `List.of(...)` infería `Object` en
   `serializesConcurrentSameReference()`. Se extrajo a una variable `Long leagueId`
   explícita. No cambia el comportamiento-spectrum del test.

`./gradlew build -x test` desde `backend/`:**BUILD SUCCESSFUL** tras estas correcciones
(inclusive el ajuste de formato que Spotless exige sobre las fuentes). Los tests los ejecutó el usuario antes de esta aprobación; el build no los acredita.

## Qué se implementó y cómo funciona

Únicamente `POST /api/players/identities/whoscored/sync`, síncrono, autenticado por
la cadena JWT existente, sin body, selección de ligas ni rol nuevo. Devuelve el
resumen de su propia ejecución terminada, incluso PARTIAL o FAILED si el resultado
quedó registrado. No se añadió GET de auditoría, endpoint histórico, scheduler,
estadísticas, cotizaciones ni corrección manual de referencias.

1. `PlayerIdentitySnapshotCapture.current()` valida bindings existentes y alias,
   captura cinco ámbitos y todos los jugadores de sus ligas en REPEATABLE_READ.
   No filtra por activo ni por equipo actual. Materializa equipo, liga y referencias;
   conserva valores del inicio y no reutiliza entidades detached para guardar catálogo.
2. `PlayerIdentityLeagueDiscoveryProcessor.discover()` consulta secuencialmente
   todas las páginas fuera de transacción. Verifica continuidad, totales y progreso.
   Las páginas válidas registran metadatos/descartes en commits breves. Los candidatos
   de una liga solo se publican en el commit de su finalización correcta.
3. `WhoScoredIdentityMatcher.match()` decide sobre la unión global de las ligas
   completas. Prioriza referencias iniciales, valida datos locales y exige nombre
   y equipo en el mismo contexto. Conserva candidatos por nombre, distingue ausencia,
   ambigüedad y descubrimiento incompleto y detecta múltiples propietarios locales.
4. `PlayerIdentityReferenceConfirmation.confirm()` recarga el jugador con lock breve
   en REQUIRES_NEW. Referencia y RESOLVED se confirman en un mismo commit. La misma
   referencia concurrente es ALREADY_REFERENCED; otra referencia u otro propietario,
   CONFLICT. Después de rollback, la clasificación y auditoría ocurren en otra transacción.
5. La fachada cierra mediante agregados persistidos y devuelve DTO de resumen.
   El Service de auditoría recalcula internamente esos agregados sin red y sin
   dependencia Service→Service. Cada invocación tiene ID propio. Cero seleccionados
   completa con ámbitos SKIPPED y sin solicitudes externas.

La cobertura de identidades no determina COMPLETED. Los resultados locales son
exclusivos; errores del proveedor cuentan ligas, y descartes cuentan filas.
`pendingPlayers` debe ser cero al cierre. La interrupción controlada conserva los
commits previos, registra ámbitos/items pendientes y restaura la señal del hilo.
Un corte abrupto puede dejar RUNNING interno; no se añadió recuperación de otra instancia.

## Inventario completo de archivos

Rutas de código relativas a `backend/src/main/java/footballmarket/`, salvo indicación.

### Configuración y protocolo externo — creados

- `config/WhoScoredProperties.java`: bindings, HTTPS, User-Agent y límites validados.
- `config/WhoScoredClientConfig.java`: RestClient/JDK dedicado, timeouts y sin redirects.
- `integrations/WhoScoredRequestPacer.java`: permiso compartido por instancia, intervalo,
  Retry-After e infraestructura de tiempo/espera controlable en tests.
- `integrations/WhoScoredIntegration.java`: path/query actuales, lectura acotada, JSON
  independiente de MIME, validación de paging/filas, canonicalización y errores seguros.
- `integrations/records/WhoScoredCatalogPage.java`: página adaptada y huellas de progreso.
- `integrations/records/WhoScoredPlayerIdentity.java`: identidad/contexto externo, sin métricas.
- `integrations/records/WhoScoredDiscardedIdentityRow.java`: posición/motivo de descarte.
- `integrations/records/WhoScoredPaging.java`: metadatos de filas originales.
- `integrations/exceptions/WhoScoredIntegrationException.java`: diagnóstico sin body ni causa sensible.

### Dominio y auditoría — creados

- `models/WhoScoredIdentityNormalizer.java`: NFD, marcas latinas, Locale.ROOT, Pd y White_Space.
- `models/WhoScoredTeamAliases.java`: los 96 pares de la tabla normativa fija y colisiones inversas.
- `models/WhoScoredIdentityMatcher.java`: decisiones globales por contexto y conflictos de propietarios.
- `models/PlayerIdentitySyncRun.java`: origen, tiempos, cierre y estado independiente de cobertura.
- `models/PlayerIdentityLeagueDiscovery.java`: ámbito capturado, transiciones y metadatos.
- `models/PlayerIdentityDiscardedRow.java`: auditoría de descarte sin identidad inventada.
- `models/WhoScoredIdentityCandidate.java`: un ID por run y contextos tipados deduplicados.
- `models/PlayerIdentitySyncItem.java`: snapshot, referencias iniciales, evidencia y decisión exclusiva.
- `models/enums/PlayerIdentityPassType.java`: tipos de pase compartidos autorizados.
- `models/enums/PlayerIdentityRunStatus.java`: RUNNING/COMPLETED/PARTIAL/FAILED.
- `models/enums/PlayerIdentityLeagueStatus.java`: lifecycle del descubrimiento.
- `models/enums/PlayerIdentityItemResult.java`: decisiones y errores locales exclusivos.
- `models/records/WhoScoredIdentityContext.java`: contexto íntegro observado.
- `models/records/PlayerIdentitySyncCounters.java`: invariantes y conversión de agregados por resultado.
- `models/records/PlayerIdentitySyncSummary.java`: contrato interno de resumen.

### Persistencia y caso de uso — creados

- `repositories/PlayerIdentitySyncRunRepository.java`: ejecuciones.
- `repositories/PlayerIdentityLeagueDiscoveryRepository.java`: ámbitos por ejecución.
- `repositories/PlayerIdentitySyncItemRepository.java`: snapshots y agregados por resultado.
- `repositories/WhoScoredIdentityCandidateRepository.java`: candidatos por run y conteo deduplicado.
- `repositories/PlayerIdentityDiscardedRowRepository.java`: conteo de descartes externos.
- `services/PlayerIdentitySynchronizationService.java` y
  `services/impl/PlayerIdentitySynchronizationServiceImpl.java`: fachada CURRENT y cierre durable.
- `services/PlayerIdentityAuditService.java` y `services/impl/PlayerIdentityAuditServiceImpl.java`:
  lectura interna coherente; no expone consulta HTTP ni consume Integration.
- `services/internal/PlayerIdentitySnapshotCapture.java`: selección/captura inicial.
- `services/internal/PlayerIdentityLeagueDiscoveryProcessor.java`: recorrido/publicación por liga.
- `services/internal/PlayerIdentityResolutionProcessor.java`: matching fuera de transacción y orden de confirmaciones.
- `services/internal/PlayerIdentityReferenceConfirmation.java`: locking, atomicidad y clasificación estructurada de carreras.
- `services/exceptions/PlayerIdentitySynchronizationException.java`: fallo global seguro.
- `services/exceptions/PlayerIdentityAuditUnavailableException.java`: auditoría indisponible.
- `services/exceptions/PlayerIdentityRunNotFoundException.java` y
  `services/exceptions/PlayerIdentitySourceNotEligibleException.java`: contratos compartidos de T011;
  no habilitan la operación histórica.
- `backend/src/main/resources/db/migration/V6__create_player_identity_audit.sql`: cinco tablas,
  secuencias/FKs/índices y unicidad inversa parcial WHOSCORED. V6 estaba libre al iniciar.

### HTTP — creados

- `controllers/PlayerIdentityController.java`: único POST actual, delegación directa y OpenAPI español.
- `controllers/mappers/PlayerIdentityMapper.java`: transformación estructural estática, sin cálculos de negocio.
- `controllers/dtos/responses/PlayerIdentitySyncRunResponseDTO.java`: resumen terminado.
- `controllers/dtos/responses/PlayerIdentitySyncCountersResponseDTO.java`: contadores y unidades.
- `controllers/dtos/responses/PlayerIdentityLeagueDiscoveryResponseDTO.java`: ámbitos y diagnósticos.
- `controllers/exceptions/InvalidPlayerIdentityIdException.java`: prerrequisito compartido T011;
  no se añadió una ruta con `{id}` en este incremento.

### Archivos existentes modificados

- `models/enums/ExternalProvider.java`: WHOSCORED sin alterar otros proveedores.
- `models/Player.java`: defensa de cardinalidad específica WhoScored e idempotencia canónica.
- `models/PlayerExternalReference.java`: ID decimal positivo canónico exclusivamente para WhoScored.
- `repositories/PlayerRepository.java`: query propia de snapshot y lock de confirmación;
  se preservó el cambio previamente staged de `findByActiveTrue`. Se reutiliza la consulta
  de propietario ya existente en `PlayerExternalReferenceRepository`, sin modificar su contrato.
- `controllers/exceptions/GlobalExceptionHandler.java`: dos errores propios 500; timestamp y casos ajenos conservados.
- `backend/src/main/resources/application.yml`: propiedades operativas y variables de los cinco IDs locales.
- `backend/src/test/resources/application-test.yml`: configuración aislada y bindings sintéticos de testing.
- `backend/src/docs/asciidoc/index.adoc`: contrato actual, unidades e includes REST Docs fuente.
- `docs/backend/technologies.md`: uso efectivo de RestClient/JDK y JSONB, requerido por Constitution §4;
  esta coherencia documental no marca T049 ni consume Polish.
- `specs/008-whoscored-player-identity/tasks.md`: avance real y verificaciones humanas pendientes.
- `specs/trello.md`: estado operativo y evidencia de este incremento, preservando antecedentes locales.
- Este reporte es un artefacto nuevo del incremento. `spec.md`, `plan.md`, contratos y checklists
  mantienen su contenido funcional. `opencode.json` conserva íntegramente sus cambios previos.

## Tests escritos — ejecución pendiente

Rutas relativas a `backend/src/test/java/footballmarket/`.

| Archivo | Responsabilidad y casos relevantes |
| --- | --- |
| `models/WhoScoredIdentityMatcherTest.java` | Pipeline Unicode, marcas no latinas, puntuación, alias y colisiones; contexto íntegro, homónimos, variantes, referencias iniciales, ambigüedad global y múltiples propietarios. |
| `models/PlayerExternalReferenceTest.java` (modificado) | Idempotencia canónica WhoScored, segundo ID sin mutación, IDs inválidos y regresión de multiplicidad de otros proveedores. |
| `models/PlayerIdentityAuditModelTest.java` | Origen, cierre, tiempos, resultados exclusivos, vacío, COMPLETED/PARTIAL/FAILED/RUNNING interno, transiciones de ámbito y candidatos ajenos al run. |
| `config/WhoScoredConfigurationTest.java` | ApplicationContextRunner; binding/startup y límites, User-Agent/CRLF, HTTPS y bindings distintos. |
| `integrations/WhoScoredIntegrationTest.java` | RestClient y cliente JDK productivo con servidor local; MIME incorrecto, query/header sin JWT, IDs canónicos/descartes, fin/vacío/estructura/competición, status HTTP, redirects, timeout y límite de cuerpo, sin retries. |
| `integrations/WhoScoredRequestPacerTest.java` | Tiempo/espera controlados; separación entre inicios, dos hilos, Retry-After numérico/HTTP date e interrupción, sin sleeps de pacing reales. |
| `repositories/PlayerIdentityPersistenceTest.java` | PostgreSQL/Flyway, snapshot sin filtros extra, JSONB, agregados, constraints en ambos sentidos, datos heredados, carreras mismo/distinto propietario, rollback técnico, conservación del catálogo y snapshot. |
| `services/PlayerIdentitySynchronizationServiceTest.java` | Service/DB reales e Integration mock; bindings obtenidos por API de catálogo, resolución/repetición/nuevo run, ausencia de transacción en red, globalidad, fallos/páginas incompletas/sin progreso, datos locales inválidos y ámbito vacío. |
| `services/PlayerIdentityAuditServiceTest.java` | Arrange mediante sincronización, Act/Assert por auditoría; durabilidad, reconciliación, unidades separadas y ausencia de red. |
| `controllers/PlayerIdentityControllerTest.java` | MVC/Security reales y Service mock; resúmenes, contadores diferenciados, errores 500, JWT ausente/expirado/firma ajena; assertions antes de REST Docs. |
| `e2e/PlayerIdentityE2ETest.java` | RANDOM_PORT, Testcontainers y proveedores HTTP controlados; registro/login, carga y lectura por HTTP, IDs obtenidos del DTO, pase actual/repetición y catálogo inalterado. |

Infraestructura nueva:

- `support/IdentityCatalogueFixture.java`: solo llama a PlayerCatalogService; materializa IDs en
  Arrange dentro de una transacción de lectura/preparación. Para probar el ámbito vacío mueve
  jugadores a otra liga mediante el Service: no los borra por DB ni inventa IDs internos.
- `support/IdentityControlledProvider.java`: HTTP externo controlado para el journey, separado
  del contrato HTTP propio. No utiliza JSON manual para las peticiones/respuestas de la API propia.
- Cinco fixtures sintéticas en `backend/src/test/resources/whoscored/identity/`:
  `catalog-current.json`, `catalog-current-final.json`, `catalog-empty.json`,
  `catalog-historical.json` y `catalog-invalid.json`. La última es fuera de rango,
  no una página vacía válida. IDs de fixtures y defaults test son sintéticos, no datos de desarrollo.

Las pruebas Service no acceden a Repository/JPA/SQL. Sus properties son un spy de configuración
para enlazar IDs reales del catálogo de testing tras Arrange; solo se sustituye Integration como
frontera funcional. El E2E conserva las Integrations reales y sustituye únicamente destinos de
clientes por RestClient reales hacia el proveedor controlado. Los tests de persistencia son el
único lugar con preparación/observación SQL funcional; el fixture heredado retira el índice
solo en una transacción de testing que se revierte. El trigger de fallo técnico se elimina en finally.

## Decisiones y puntos delicados para revisar

1. **Snapshot y transacciones:** comprobar REPEATABLE_READ y materialización antes de red;
   después solo se recargan entidades administradas. Las llamadas externas y matching no
   están cubiertos por una transacción de la fachada.
2. **Protección concurrente:** inspeccionar el índice parcial de V6 y `confirm/apply/classifyCurrent`.
   Solo SQLSTATE 23505 de constraints reconocidas habilita reclasificación de carrera; un fallo
   técnico distinto queda como PERSISTENCE_ERROR, con auditoría posterior al rollback.
3. **Integridad de descubrimiento:** publicación de candidatos y COMPLETED de liga son atómicos;
   descartes se conservan aun si una página posterior falla. Nunca se usan candidatos parciales.
4. **Unicode/alias:** contrastar los 96 pares con la tabla normativa; no se modificó TeamNameNormalizer,
   no hay transliteración adicional ni selección por edad/posición/región/orden.
5. **Reconciliación:** los agregados parten de resultados exclusivos durables; no hay incrementos
   de error seguidos de un segundo conteo. El cierre no acepta PENDING ni umbrales de cobertura.
6. **Precisión temporal:** el fin generado por la fachada se limita a microsegundos para que el
   resumen retornado coincida con la precisión durable de PostgreSQL.
7. **Verificación pendiente:** el build no ejecuta ni acredita tests. Revisar especialmente races,
   timeouts, fixtures de DB, REST Docs y journey; las fuentes de tests no tienen resultado de ejecución.
8. **Operación real:** configurar los cinco IDs internos existentes antes de startup. No se cargó
   DB de desarrollo ni se consultó al proveedor real. La cobertura observada históricamente no es aceptación.

## Verificaciones de entrega

El build permitido es `./gradlew build -x test`; en Windows se invoca el mismo Wrapper como
`.\gradlew.bat build -x test`. El intento Bash inicial no encontró Java; Windows dispone de JDK 21.
Los fallos de formato detectados en iteraciones del build se corrigen sobre fuentes, sin ejecutar
SpotlessApply ni tests. La evidencia final de build/diff se registra en `tasks.md` y el índice.
No se ejecutó `test`, Asciidoctor, Sonar ni hooks. `.specify/extensions.yml` no estaba presente
en la comprobación inicial; se comprueba nuevamente antes de la entrega.

No existe evidencia de tests ejecutados por el usuario en este incremento. La compilación productiva
no acredita compilación ni ejecución de las fuentes de tests excluidas por `-x test`.

## Orden recomendado de revisión

1. Spec US1/FR aplicables → contrato HTTP/WhoScored → tasks T001–T035.
2. Migración y Models; tests Model/Repository.
3. Properties, cliente, pacer e Integration; tests Config/Integration.
4. Captura → descubrimiento → resolución → confirmación → cierre/agregación; tests Service.
5. DTO/Mapper, Controller/handler, OpenAPI y REST Docs; tests Controller.
6. Journey E2E, fixtures/infraestructura y evidencia final de diff/build.

### Checklist humano obligatorio de CODE REVIEW

- [x] Revisé todos los archivos creados/modificados, incluidos tests y fixtures.
- [x] Entiendo qué cambió y por qué.
- [x] Revisé las decisiones importantes, límites transaccionales y carreras.
- [x] Contrasté normalización, alias, contadores y protección de referencias con la spec.
- [x] No detecté comportamiento fuera de SPEC-008/US01 ni implementación de US02.
- [x] Revisé la cobertura y las fronteras de los tests sin considerarlos ejecutados.
- [x] Apruebo avanzar a TESTING para este diff local de US01.

La aprobación debe ser humana, explícita y referida a este estado revisado. Hasta entonces
US01 no entra a TESTING. La Feature permanece IN PROGRESS; no procede DONE ni CONVERGE.

## Decisión del revisor (2026-10-14)

Los siete puntos del checklist quedaron marcados por el revisor humano y cubren el
diff local con las tres correcciones del addendum final. CODE REVIEW de
SPEC-008 | US01 queda **APROBADO**. La Feature sigue IN PROGRESS: corresponde
TESTING con ejecución y validación humanas, sin DONE ni CONVERGE.</oldString>

# Code Review Report — sincronización única de identidad

Feature: SPEC-008. Rama: `refactor/008-whoscored-player-identity`. Alcance: T058–T066 y decisiones humanas de 2026-11-01. Estado: **implementación técnica entregada; CODE REVIEW pendiente**. Sin commit ni publicación; la evidencia revisable es el diff local. Seguimiento remoto no verificado ni modificado.

**Actualización posterior — revisión y cierre**: el usuario confirmó que CODE REVIEW y TESTING están aprobados y que todos los tests pasan. La convergencia posterior no encontró pendientes. Se conserva el contenido del reporte de entrega como evidencia de lo revisado; el estado vigente y autorizaciones se registran en [convergencia y cierre](convergence-sync-simplification.md). Los marcadores del checklist humano no se rellenan en nombre del revisor.

## 1. Resultado y funcionamiento

`POST /api/players/identities/whoscored/sync` ejecuta actual y luego histórico sobre NO_MATCH/AMBIGUOUS aún elegibles. Actual FAILED omite el complemento. El estado global conserva prioridad FAILED > PARTIAL > COMPLETED, aunque ya existan referencias confirmadas por el primer pase. Sin pendientes no se consulta el histórico; si todos ya tienen referencia tampoco se descubre de nuevo el catálogo.

La respuesta tiene exactamente cinco campos: `status`, `selectedPlayers`, `referencedPlayers`, `noMatchPlayers` y `unresolvedPlayers`. Las tres categorías finales suman seleccionados; el resultado del histórico sustituye al provisional por ID local. Referencias preexistentes válidas cuentan con referencia; contradicciones, ambigüedad y errores cuentan sin resolver. Cero seleccionados: cuatro ceros y COMPLETED.

Se mantienen normalización, alias, matching global de ligas completas, deduplicación, invariantes de referencias, locks y constraints. Los datos de trabajo existen solo durante la invocación. Se mantienen las clases y carpetas de application, adaptando sus cuerpos; no se fusionan procesadores ni se crea otro Service. `PendingReviewRecorder`, `PendingReviewCaseRepository`, modelos/enums de revisión y matcher/alias de imágenes permanecen intactos.

## 2. Preparación y autorización

- SPECIFY/CLARIFY del cambio, Plan y contratos actualizados; scripts reales de selección y setup ejecutados sobre SPEC-008.
- CHK041–048 marcados por el usuario y verificados por lectura; marcadores no modificados por el agente. Reglas de identidad y checklist general preservados; el complemento cubre captura, conteo, elegibilidad, retirada de auditoría y contratos afectados.
- Tasks T058–T066 añadidas sin alterar IDs ni logros históricos. ANALYZE read-only antes de código: 29 FR y ocho SC cubiertos por las nuevas tareas o por comportamiento preservado y sus regresiones; sin bloqueantes, sin tareas nuevas huérfanas. Cobertura por grupos registrada en tasks.md y plan.md. No se ejecutaron tests como parte del análisis.
- El usuario autorizó spec y código y luego indicó explícitamente continuar implementación. La autorización se registra en spec/clarificación, sin inferir aprobación de revisión, tests ni cierre.

## 3. Inventario productivo

Rutas relativas a `backend/src/main/java/footballmarket/`:

| Archivo(s) | Cambio y responsabilidad |
| --- | --- |
| `features/identity/application/PlayerIdentitySynchronizationService.java` | Una API `synchronize()` para el caso de uso completo. |
| `features/identity/application/impl/PlayerIdentitySynchronizationServiceImpl.java` | Secuencia de pases, clasificación de estado y unión por ID local; sin repositorios de auditoría ni transacción durante red. |
| `features/identity/application/internal/PlayerIdentitySnapshotCapture.java` | Captura materializada por pase, transacción breve; selección histórica de IDs del universo inicial, incluyendo referencias concurrentes. |
| `features/identity/application/internal/PlayerIdentityLeagueDiscoveryProcessor.java` | Paginación, progreso y publicación solo tras completar una liga; candidatos/completitud en memoria sin guardar ámbitos/descartes. |
| `features/identity/application/internal/PlayerIdentityResolutionProcessor.java` | Matching global y confirmación por jugador; resultados en memoria e interrupción controlada. |
| `features/identity/application/internal/PlayerIdentityReferenceConfirmation.java` | Solo persistencia de referencia, lock breve y rollback aislado; relectura tras unicidad conocida sin auditar el fallo. |
| `features/catalog/infraestructure/PlayerRepository.java` | Recaptura por IDs sin excluir referencias concurrentes: se clasifican como ya referenciados, no se pierden del total. Mantiene entity graph y demás consultas ajenas. |
| `features/identity/domain/models/PlayerIdentitySyncItem.java` | Snapshot transitorio mínimo; sin entidad, secuencia, run ni decisión persistida. |
| `features/identity/domain/models/WhoScoredIdentityCandidate.java` | ID externo canónico y contextos deduplicados, sin entidad ni run. |
| `features/identity/domain/records/WhoScoredIdentityContext.java` | Solo nombre/equipo observados juntos; sin metadatos exclusivamente de auditoría. |
| `features/identity/domain/records/PlayerIdentitySyncSummary.java` | Totales finales, validación de cantidades/reconciliación sin overflow y agregación de resultados. |
| `features/identity/domain/enums/PlayerIdentityRunStatus.java` | Tres estados finales, clasificación de finalización de cada pase y regla de combinación en dominio; sin RUNNING persistido. |
| `features/identity/domain/enums/PlayerIdentityItemResult.java` | Retira PENDING, que ya no representa un item persistido. Se conservan clasificaciones de dominio necesarias. |
| `features/identity/presentation/PlayerIdentityController.java` | Único POST, invocación Service → Mapper y OpenAPI actualizado. |
| `features/identity/presentation/PlayerIdentityMapper.java` | Transformación estructural de cinco campos, sin cálculos. |
| `features/identity/presentation/dtos/response/PlayerIdentitySyncRunResponseDTO.java` | Único record de respuesta global con documentación de unidades/categorías. Se conserva el nombre para limitar cambios de consumidores, aunque no representa un run persistido. |
| `common/presentation/GlobalExceptionHandler.java` | Retira exclusivamente handlers/imports de ID de run y origen histórico; conserva SYNC_FAILED y errores ajenos. |

### Archivos retirados

Todos bajo `features/identity/` en producción:

- `infraestructure/PlayerIdentitySyncRunRepository.java`, `PlayerIdentitySyncItemRepository.java`, `PlayerIdentityLeagueDiscoveryRepository.java`, `WhoScoredIdentityCandidateRepository.java`, `PlayerIdentityDiscardedRowRepository.java`: acceso a tablas retiradas.
- `domain/models/PlayerIdentitySyncRun.java`, `PlayerIdentityLeagueDiscovery.java`, `PlayerIdentityDiscardedRow.java`: entidades exclusivamente de auditoría.
- `domain/records/PlayerIdentitySyncCounters.java`, `domain/enums/PlayerIdentityLeagueStatus.java`: contadores/estados detallados sin responsabilidad vigente.
- `presentation/dtos/response/PlayerIdentitySyncCountersResponseDTO.java`, `PlayerIdentityLeagueDiscoveryResponseDTO.java`: desglose retirado del contrato.
- `application/exceptions/PlayerIdentityRunNotFoundException.java`, `PlayerIdentitySourceNotEligibleException.java` y `presentation/exceptions/InvalidPlayerIdentityIdException.java`: errores exclusivos del endpoint histórico retirado.

### Migración nueva

`backend/src/main/resources/db/migration/V20261101000000__remove_player_identity_audit.sql` elimina explícitamente las cinco tablas y cinco secuencias de auditoría, en orden de dependencias, sin CASCADE amplio. Preserva V9, `player_external_references`, índice WhoScored y `pending_review_cases`. **Es destructiva respecto de auditoría histórica** y Flyway la aplicará al iniciar posteriormente la aplicación contra una base que no la tenga. El agente no inició la aplicación, no ejecutó Flyway ni modificó una base.

## 4. Tests creados, modificados y retirados

Rutas relativas a `backend/src/test/java/footballmarket/`:

| Archivo | Cobertura y motivo |
| --- | --- |
| **Nuevo:** `features/identity/domain/records/PlayerIdentitySyncSummaryTest.java` | Categorías, vacío, cantidades inválidas/overflow y nueve combinaciones de estados. Unitario puro. |
| **Retirado:** `features/identity/domain/models/PlayerIdentityAuditModelTest.java` | Contratos de entidades de auditoría desaparecidas. Se reemplazan las reglas aún vigentes por resumen, Service y Repository; no se conserva auditoría artificial para los tests. |
| `features/identity/domain/models/WhoScoredIdentityMatcherTest.java` | Adapta constructores/contextos a valores transitorios; conserva normalización, homónimos, nombre/equipo del mismo contexto, deduplicación, ambigüedad global y conflictos de propietarios. |
| `features/identity/application/PlayerIdentitySynchronizationServiceTest.java` | Service/DB reales, Integration mock. Una llamada actual+histórico, ausencia de red cuando no corresponde, histórico FAILED/PARTIAL, actual parcial, NO_MATCH frente a sin resolver, atributos recapturados, paginación incompleta/sin progreso, interrupción, idempotencia y red sin transacción. Act/Assert por API pública del Service, sin repositorios. |
| `features/identity/infraestructure/PlayerIdentityPersistenceTest.java` | Flyway/Testcontainers reales: ausencia de tablas, índice/revisión conservados, snapshot sin filtros extra, recaptura de referencias concurrentes, constraints, referencias heredadas contradictorias, carreras mismo/distinto propietario, protección de cambios de catálogo, jugador eliminado y rollback técnico con continuidad. Ya no consulta auditoría para afirmar resultados. |
| `features/identity/presentation/PlayerIdentityControllerTest.java` | MVC/seguridad reales y Service mock: cinco campos exactos para tres estados y vacío; error 500 completo, JWT ausente/expirado/firma ajena y ruta histórica retirada sin invocar Service. |
| `features/identity/e2e/PlayerIdentityE2ETest.java` | Autenticación y catálogo por HTTP, Integration real con proveedor controlado; una llamada recorre actual/histórico, cuenta cinco una vez y relanzamiento no duplica referencias ni altera atributos. |
| `features/statistics/application/ExtractionPopulationExecutorTest.java`, `JsonStatisticsUpdateServiceTest.java`, `PlayerStatisticsExtractionServiceTest.java`, `PlayerStatisticsSynchronizationServiceTest.java` | Preparación existente cambia de `synchronizeCurrent()` a `synchronize()`; no se cambia el comportamiento estadístico bajo prueba. |
| `features/statistics/e2e/PlayerStatisticsCrossChannelJourneyE2ETest.java`, `PlayerStatisticsSyncJourneyE2ETest.java` | Ya no afirman un ID de run inexistente; afirman referencias obtenidas en el DTO nuevo. |

Helpers de testing y contratos del cliente WhoScored inspeccionados: no requirieron cambios. Las pruebas Integration/pacer existentes siguen cubriendo protocolo, parsing, límites y ritmo; no fueron ejecutadas. No se añadieron dependencias ni tests frontend.

## 5. Documentación y trazabilidad

- `docs/backend/data-model.md`: retira las tablas del diagrama/esquema actual y explica la migración y referencias conservadas.
- `docs/backend/technologies.md`: elimina el uso de JSONB de auditoría WhoScored sin retirar Hibernate/JSONB de otros dominios.
- `specs/008-whoscored-player-identity/spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/http.md`, `contracts/whoscored.md` y `quickstart.md`: secciones vigentes del alcance único y antecedentes claramente identificados.
- `tasks.md`: T058–T066 conservan seguimiento técnico; historial previo intacto.
- `clarify-sync-simplification.md`: decisiones, autorización, revisión y evidencia de preparación.
- Checklists general/identidad identifican alcance histórico y criterios afectados; `checklists/sync-simplification.md` contiene la evaluación marcada por el usuario, no por el agente.
- Este reporte documenta exclusivamente el nuevo resultado; no modifica aprobaciones históricas ni afirma sincronización remota.

## 6. Verificación observada y pendientes

- Primer `build -x test`: compilación productiva realizada, fallo de formato introducido por el cambio. Corregido con el formateador existente, sin cambiar reglas ni salidas generadas.
- Build final desde backend: `./gradlew build -x test` (Wrapper Windows `.\gradlew.bat build -x test`): **BUILD SUCCESSFUL**. Incluyó compilación productiva, empaquetado, Spotless y clasificación estática de 137 clases de test; ninguna tarea Test ejecutada. No acredita compilación ni ejecución de los tests.
- Advertencia de API obsoleta en `PlayerStatisticsSynchronizationServiceImpl`, archivo productivo ajeno sin cambios en este diff. No se resolvió tooling ni deuda fuera del alcance.
- Búsqueda de consumidores antiguos en `backend/src/`: sin referencias Java a las APIs, repositorios, entidades o excepciones eliminados. Las migraciones históricas permanecen como fuente de su esquema anterior.
- Revisión del diff y `git diff --check` sin diagnósticos al entregar. Sin commits, push, arranque, proveedor real ni aplicación de migraciones.
- Pendientes: revisión humana del diff completo, compilación/ejecución de tests, validación de Flyway y concurrencia en Testcontainers, comportamiento real del proveedor y cierre posterior.

## 7. Orden recomendado de revisión y puntos delicados

1. Contrato HTTP vigente y `PlayerIdentitySyncSummary`: categorías, denominador y ausencia de doble conteo.
2. Fachada e internos de application: orden de pases, FAILED, interrupción y resultado provisional reemplazado, no sumado. La recaptura histórica cuenta referencias concurrentes y no pierde IDs borrados del denominador.
3. Matcher/normalizador/alias preservados y confirmación: comprobar que se mantengan locks, constraints, no sobrescritura, rollback y clasificación conocida de PostgreSQL.
4. Migración: verificar lista exacta de objetos que se retiran y conservación de índices/casos de revisión. No iniciar la aplicación contra una base con auditoría que se quiera conservar sin tomar antes la decisión operativa correspondiente.
5. Controller/Mapper/DTO/handler y consumidores de estadísticas: cambio incompatible intencional, sin API histórica alternativa.
6. Tests de cada capa y comandos siguientes; el build no sustituye sus resultados. En particular, las carreras y el rollback solo quedarán demostrados cuando el usuario los ejecute.

### Comandos para ejecución humana posterior

Desde `backend/`, con Docker disponible para Testcontainers:

```powershell
.\gradlew.bat test --tests "footballmarket.features.identity.*"
.\gradlew.bat test --tests "footballmarket.integrations.whoscored.*"
.\gradlew.bat test --tests "footballmarket.features.statistics.*"
```

Son comandos propuestos, no ejecutados. Tras CODE REVIEW aprobado corresponde el Testing Report y la validación humana separada de cobertura/resultados; no se declara TESTING aprobado anticipadamente.

## 8. Checklist humano de CODE REVIEW

- [x] Revisé los archivos creados, modificados y retirados, incluidos tests y migración.
- [x] Entiendo el endpoint único, las categorías y el conteo final por jugador.
- [x] Revisé decisiones de matching, persistencia, concurrencia y fallos de ambos pases.
- [x] No detecté comportamiento fuera de la spec ni cambios en catálogo/imágenes/revisión ajenos al alcance.
- [x] Apruebo avanzar a TESTING.

La aprobación debe referirse al diff local revisado. La entrega técnica y el build no aprueban este checklist ni autorizan cierre.

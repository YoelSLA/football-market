# Code Review Report — SPEC-012, estructura por canales

**Aprobaciones posteriores — 2026-11-01**: el usuario confirmó que todos los tests pasaron y aprobó expresamente CODE REVIEW y TESTING para T111–T117 y T118–T124. La evaluación conjunta corresponde al estado posterior al retiro de coverage y la corrección del fixture, documentados en [code-review-coverage-removal.md](code-review-coverage-removal.md). No se infieren conteos de tests ni cierre de SPEC-012.

## Alcance y estado

- Fecha: 2026-11-01. Rama: `refactor/012-player-statistics-scraping`.
- Autorización humana: «Autorizo a implementar T111-T117.», registrada en `tasks.md` sobre el diseño analizado sin bloqueantes y el checklist mínimo aprobado.
- Implementación: T111–T117 completas técnicamente; revisión humana de código y testing pendientes. Este reporte no aprueba ni cierra SPEC-012.
- Base de revisión: diff local frente a HEAD y archivos nuevos. La preparación documental ya estaba modificada al iniciar IMPLEMENT y se conservó; no hay commit ni PR nuevos.
- Seguimiento GitHub: no consultado ni actualizado; no se afirma sincronización o estado operativo remoto.

## Resumen y funcionamiento

Se mantiene una sola feature `statistics`, con sus interfaces públicas en `application/`. Los componentes exclusivos se localizan en `application/scraping/` y `application/json/`; la fachada de sincronización, cobertura y composición común se localiza en `application/shared/`. Se conservan las subdivisiones `impl/` e `internal/`.

Scraping continúa en `PlayerStatisticsController.synchronizeScraping` → interfaz de sincronización → `shared/impl/PlayerStatisticsSynchronizationServiceImpl.synchronizeScraping` → batch de extracción y `scraping/internal/ExtractionBatchExecutor` → integración WhoScored → `ScrapingMetricAssembler` → escritor compartido. JSON continúa en la misma fachada, método `synchronizeJson`, su pool y `json/impl/JsonStatisticsUpdateServiceImpl.update` → integración JSON → `JsonMetricAssembler` → escritor compartido. La fachada conserva ambos algoritmos; no se extrajeron clases ni se armonizaron sus políticas.

## Inventario completo de producción

Todas las rutas de esta tabla son relativas a `backend/src/main/java/footballmarket/features/statistics/application/`. Cada fila representa el movimiento del archivo, con igual nombre de clase; cambian exclusivamente paquete e imports necesarios.

| Origen | Destino | Responsabilidad preservada |
| --- | --- | --- |
| `internal/ExtractionRunManager.java` | `shared/internal/ExtractionRunManager.java` | Apertura, recuperación, cierre y resumen de runs. |
| `internal/ExtractionWorkerPool.java` | `shared/internal/ExtractionWorkerPool.java` | Concurrencia y limpieza por hilo. |
| `internal/PlayerStatisticsEligibilitySelector.java` | `shared/internal/PlayerStatisticsEligibilitySelector.java` | Población y pendientes. |
| `internal/PlayerStatisticsWriter.java` | `shared/internal/PlayerStatisticsWriter.java` | Confirmación transaccional por jugador. |
| `internal/ExtractionIncidentRecorder.java` | `shared/internal/ExtractionIncidentRecorder.java` | Auditoría de incidencias. |
| `internal/LeagueContextSelection.java` | `shared/internal/LeagueContextSelection.java` | Elección del contexto de liga. |
| `internal/PlayerMetricsContext.java` | `shared/internal/PlayerMetricsContext.java` | Resultado de ensamblado común. |
| `internal/PlayerStatisticsCoverageQuery.java` | `shared/internal/PlayerStatisticsCoverageQuery.java` | Cobertura persistida sin proveedor. |
| `impl/PlayerStatisticsExtractionServiceImpl.java` | `scraping/impl/PlayerStatisticsExtractionServiceImpl.java` | Sesión y extracción individual web. |
| `internal/ExtractionBatchExecutor.java` | `scraping/internal/ExtractionBatchExecutor.java` | Recorrido de scraping. |
| `internal/HeadedBrowserAvailabilityChecker.java` | `scraping/internal/HeadedBrowserAvailabilityChecker.java` | Apertura headed bajo demanda. |
| `internal/ScrapingMetricAssembler.java` | `scraping/internal/ScrapingMetricAssembler.java` | Traducción del contenido web a métricas. |
| `impl/JsonStatisticsUpdateServiceImpl.java` | `json/impl/JsonStatisticsUpdateServiceImpl.java` | Actualización individual JSON. |
| `internal/JsonMetricAssembler.java` | `json/internal/JsonMetricAssembler.java` | Traducción de respuestas JSON. |
| `impl/PlayerStatisticsSynchronizationServiceImpl.java` | `shared/impl/PlayerStatisticsSynchronizationServiceImpl.java` | Fachada de ambos recorridos. |
| `impl/PlayerStatisticsCoverageServiceImpl.java` | `shared/impl/PlayerStatisticsCoverageServiceImpl.java` | Consulta de cobertura. |

No se modificaron las interfaces públicas, `PlayerStatisticsQueryServiceImpl`, Controllers, DTO, Model, Repository, migraciones, configuración, clientes o integraciones externas. No se cambian algoritmos, nombres de métodos, firmas, excepciones ni límites transaccionales. Los imports antes implícitos por compartir paquete se hicieron explícitos al cruzar los grupos.

## Tests afectados

Rutas relativas a `backend/src/test/java/footballmarket/features/statistics/`:

| Origen | Destino o ajuste | Responsabilidad y relación con historias |
| --- | --- | --- |
| `application/PlayerStatisticsExtractionServiceTest.java` | `application/scraping/PlayerStatisticsExtractionServiceTest.java` | US1: extracción individual y su resultado observable. |
| `application/ExtractionPopulationExecutorTest.java` | `application/scraping/ExtractionPopulationExecutorTest.java` | US2: snapshot, continuidad, fecha y recuperación. |
| `application/HeadedBrowserStartupTest.java` | `application/scraping/HeadedBrowserStartupTest.java` | US1: arranque sin navegador y aislamiento de indisponibilidad. |
| `application/JsonStatisticsUpdateServiceTest.java` | `application/json/JsonStatisticsUpdateServiceTest.java` | US3: actualización JSON, ausencias e independencia de campaña. |
| `application/PlayerStatisticsSynchronizationServiceTest.java` | `application/shared/PlayerStatisticsSynchronizationServiceTest.java` | US2: fachada, pendientes, reanudación y errores. |
| `application/internal/ExtractionWorkerPoolTest.java` | `application/shared/internal/ExtractionWorkerPoolTest.java` | Componente común: propagación y limpieza por worker. |
| `infraestructure/PlayerStatisticsCoverageTest.java` | Misma ruta; imports hacia `shared/internal`. | Elegibilidad y cobertura de la campaña. |
| `infraestructure/PlayerStatisticsPersistenceTest.java` | Misma ruta; imports hacia `shared/internal`. | Fila por jugador/fecha/canal y confirmación atómica. |

Se conservaron cuerpos, casos, tags, fixtures, perfiles, mocks y aserciones: no se agregaron pruebas artificiales por mover paquetes. Los tests de Service mantienen Spring/PostgreSQL y sustituciones de proveedor; el pool sigue siendo unitario. Las suites HTTP, seguridad, dominio y E2E no se movieron. No se modifican deudas históricas de cobertura ni se declaran resueltas.

## Documentación creada o modificada

- Este archivo: inventario, decisiones, evidencia y revisión integral exigida por el workflow.
- `docs/backend/architecture.md`, §3.1: reemplaza la referencia antigua a `services/internal/` por la organización real de estadísticas. Sin alterar roles ni reglas de dependencias.
- `specs/012-player-statistics-scraping/tasks.md`: autorización explícita, evidencia de Analyze previo y avance técnico T111–T117; conserva T001–T110.
- `specs/012-player-statistics-scraping/plan.md`: diseño preparado previamente y nota posterior de implementación, conservando antecedentes.
- `specs/012-player-statistics-scraping/quickstart.md`: ubicaciones actuales y comandos humanos de tests; conserva escenarios y contratos.
- `specs/012-player-statistics-scraping/research.md`: decisión organizativa de la preparación, conservada sin nuevos cambios durante IMPLEMENT.
- `specs/012-player-statistics-scraping/checklists/channel-structure.md`: checklist mínimo preparado y aprobado antes de IMPLEMENT, conservado sin modificaciones en esta fase.

## Evidencia técnica y límites

1. Las 22 clases movidas (16 productivas y seis tests) tienen el mismo cuerpo textual que en HEAD al excluir únicamente declaraciones de paquete/imports y espacios exteriores. Los dos tests que permanecen en persistencia también conservan su cuerpo. Esta es revisión estática del diff, no ejecución de tests.
2. No quedan referencias Java a `footballmarket.features.statistics.application.internal`; `application.impl` conserva únicamente la consulta individual que se excluyó del refactor.
3. Se revisó `backend/build.gradle.kts` antes del build. Desde `backend/` se ejecutó `gradlew.bat build -x test`: **BUILD SUCCESSFUL en 32 s**, sin tareas Test, `compileTestJava` ni `testClasses`. El grafo incluyó compilación productiva, formato y validación estática de clasificación de 137 clases.
4. El compilador informó uso de API deprecated en la fachada. El cuerpo de esa clase no cambió; no se investigó ni corrigió fuera del alcance organizativo.
5. El build no demuestra arranque Spring, compilación de tests, resultados funcionales ni liberación real del navegador. Todos los tests permanecen pendientes del usuario. No se accedió a DB/proveedor ni se inició la aplicación.
6. No se alteran contratos ni se incorporan cambios generados. No hubo commit, push, ejecución de hooks Git ni escrituras remotas.

## Orden recomendado y checklist humano de CODE REVIEW

Revisar primero el mapa de producción, después ambos métodos de la fachada compartida, los extractores/ensambladores exclusivos, los ocho tests afectados y finalmente documentación y evidencia. Comprobar especialmente que los imports nuevos resuelven los mismos tipos y que las interfaces y consumidores externos conservan sus contratos.

- [x] Revisé los archivos creados/modificados, incluidos los tests.
- [x] Entiendo qué cambió y por qué.
- [x] Revisé las decisiones importantes: una feature, agrupación por canal y fachada conservada.
- [x] No detecté comportamiento fuera de la spec ni cambios funcionales en los movimientos.
- [x] Apruebo avanzar a TESTING del alcance T111–T117.

Estas casillas son del usuario; el agente no las marca ni infiere aprobación por el build.

## Preparación de TESTING — pendiente de revisión de código

No hay tests nuevos. Los ocho tests afectados conservan las verificaciones existentes sobre extracción, campañas, ausencias, pendientes, auditoría y limpieza; no cambian mocks ni aserciones. Riesgos de la reorganización: imports/resolución de tipos y descubrimiento de componentes Spring tras mover paquetes. El build productivo no cubre esos riesgos por completo. Las integraciones de proveedor se sustituyen por dobles; no se requiere una campaña real de WhoScored para comprobar estos movimientos.

Después de aprobar CODE REVIEW, revisar las ocho fuentes en el orden de la tabla, y ejecutar personalmente desde `backend/` en PowerShell:

```powershell
.\gradlew.bat unitTest --tests 'footballmarket.features.statistics.application.shared.internal.ExtractionWorkerPoolTest*'
.\gradlew.bat integrationTest --tests 'footballmarket.features.statistics.application.scraping.*' --tests 'footballmarket.features.statistics.application.json.*' --tests 'footballmarket.features.statistics.application.shared.*' --tests 'footballmarket.features.statistics.infraestructure.PlayerStatisticsCoverageTest*' --tests 'footballmarket.features.statistics.infraestructure.PlayerStatisticsPersistenceTest*'
```

El segundo comando requiere Docker/PostgreSQL de Testcontainers; no usa el proveedor real. Los filtros terminados en `*` incluyen los grupos anidados. No confundir clasificación estática, build ni resultados previos de otras ubicaciones con la ejecución de estas suites. Si fallan, aportar la salida para clasificar el fallo; si pasan, confirmar el resultado sin necesidad de pegar todo el log. Esta preparación no acredita TESTING ni cierre.

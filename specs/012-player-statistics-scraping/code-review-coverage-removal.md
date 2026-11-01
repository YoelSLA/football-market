# Code Review Report — retiro de coverage, T118–T124

## Alcance y estado

**Aprobaciones humanas posteriores — 2026-11-01**: tras la corrección del fixture, el usuario informó «Ya pasaron todos los tests y revise el code review.» y respondió «Si lo apruebo.» a la solicitud explícita de aprobar CODE REVIEW y TESTING de T111–T117 y T118–T124. Se registra su aprobación para ambos alcances y la ejecución satisfactoria comunicada por él; no se dispone de conteos ni logs completos y no se atribuye ejecución al agente. Esta aprobación no autoriza ni acredita cierre de SPEC-012. El reporte y diff local anteriores son la base evaluada; no se modificó código desde la aprobación.

**Revisión posterior del fixture — 2026-11-01**: el usuario aportó fallo por rating null en `PlayerStatisticsExtractionServiceTest.Extraction.confirmaJugadorYContextos` y comunicó que el resto pasa, sin inferir qué suites ejecutó. Se corrigió solo `scraped(...)`: el rating se publica en `WhoScoredStructuredDataBlock.Entry` con la misma competición, temporada y equipo de la fila, en vez de una celda inicial que producción descarta. El valor y la assertion decimal exacta se conservan. No hubo cambios productivos ni ejecución de tests por el agente. Esta revisión actualiza la base de revisión del test; CODE REVIEW y TESTING siguen pendientes, sin aprobar ni invalidar aprobaciones humanas inexistentes.

2026-11-01, SPEC-012, rama `refactor/012-player-statistics-scraping`. Autorización: «Autorizo implementar T118-T124.», registrada en `tasks.md` después del checklist humano y Analyze sin bloqueantes. Se retira exclusivamente la consulta GET y sus piezas sin uso, conservando los contratos de sincronización y ajustando pruebas. Implementación técnica completa; CODE REVIEW, TESTING y cierre pendientes. Seguimiento remoto no consultado ni actualizado.

Los cambios del refactor T111–T117 ya existían en el árbol e índice y se preservaron. El reporte de aquel refactor es un antecedente de esa entrega: sus afirmaciones de cuerpos intactos no describen este retiro funcional posterior. No hay commit ni PR nuevos ni aprobación de ninguno de los dos alcances inferida de esta entrega.

## Funcionamiento y decisiones

`PlayerStatisticsController` solo expone los dos POST. El antiguo GET ya no tiene handler; JWT y seguridad global permanecen. No se agrega handler de 404, redirección, alias ni contrato de errores para visitantes de rutas inexistentes.

`PlayerStatisticsCoverageQuery.coverage` conserva el cálculo `eligible`, `catalog - eligible`, `max(0, eligible - completed)` y `pending == 0`. El record interno solo contiene los cuatro datos que consume `ExtractionRunManager.finish`; ese cierre no requiere modificaciones. No se consultan métricas completas, último run o fallidos del antiguo GET para producir el resumen. Se conservan selección, snapshots, fecha/canal, no-data, reanudación, estados, conteos del run, atomicidad y respuesta de los POST. No cambian DTO de sincronización, reglas numéricas, esquema, configuración del proveedor ni integraciones.

## Inventario completo de producción

Rutas relativas a `backend/src/main/java/`:

| Archivo | Cambio y razón |
| --- | --- |
| `footballmarket/features/statistics/presentation/PlayerStatisticsController.java` | Retira GET, constructor/inyección de cobertura, validación de filtros e imports exclusivos; conserva ambos POST/OpenAPI. |
| `footballmarket/features/statistics/presentation/PlayerStatisticsCoverageMapper.java` | Eliminado: solo convertía la respuesta retirada. |
| `footballmarket/features/statistics/presentation/dtos/response/PlayerStatisticsCoverageResponseDTO.java` | Eliminado: ya no existe ese contrato HTTP. |
| `footballmarket/features/statistics/application/PlayerStatisticsCoverageService.java` | Eliminado: caso de consulta retirado. |
| `footballmarket/features/statistics/application/shared/impl/PlayerStatisticsCoverageServiceImpl.java` | Eliminado: implementación del caso retirado. |
| `footballmarket/features/statistics/application/shared/internal/PlayerStatisticsCoverageQuery.java` | Reduce dependencias a población/items; elimina lectura masiva de métricas, diagnóstico y último run no utilizados por los POST. |
| `footballmarket/features/statistics/domain/records/PlayerStatisticsCoverage.java` | Conserva solo elegibles, excluidos, pendientes y cobertura completa; resultado interno, no DTO público. |
| `footballmarket/features/statistics/infraestructure/PlayerStatisticsRunItemRepository.java` | Retira `countDistinctPlayersWithoutConfirmation`, sin consumidores; no representa correctamente pendientes tras reconfirmación y ya no alimenta el GET. |
| `footballmarket/features/statistics/infraestructure/PlayerStatisticsRepository.java` | Solo actualiza comentario de `findMetricsByCampaign`: se conserva porque lo usan tests de persistencia. |
| `footballmarket/common/presentation/GlobalExceptionHandler.java` | Retira ramas de filtros exclusivas de coverage; conserva tratamiento de cotizaciones, errores de POST y otras features. |
| `footballmarket/security/SecurityConfig.java` | Retira matcher redundante de ruta inexistente. `anyRequest().authenticated()` permanece, sin relajar seguridad. |

Se revisó y conservó `PlayerStatisticsExtractionRunRepository.findByExtractionDateAndSourceOrderByStartedAtDescIdDesc`, utilizado en `support/QuoteTestData`. No se eliminaron repositorios completos ni consultas de otras features. No hay nuevas clases productivas, endpoints o APIs exclusivas para tests.

## Inventario completo de tests y garantías

Rutas relativas a `backend/src/test/java/footballmarket/features/statistics/`:

| Archivo | Cambios y cobertura |
| --- | --- |
| `presentation/PlayerStatisticsControllerTest.java` | Retira mocks y ocho casos exclusivos de GET exitoso/filtros/seguridad; agrega regresión parametrizada de 404 con JWT y filtros ausentes, válidos o inválidos, sin interacción con sincronización. Conserva casos de ambos POST, fecha y errores. |
| `presentation/PlayerStatisticsContractFoundationTest.java` | Solo corrige comentario de fixture para no prometer ruta GET vigente. Casos de advice conservados. |
| `security/PlayerStatisticsSecurityIntegrationTest.java` | Quita mock de Service eliminado; sustituye caso exclusivo de GET sin token por 404 con token válido, sin sincronización. Mantiene rechazo de acceso inválido a POST y cadena real. |
| `application/shared/PlayerStatisticsSynchronizationServiceTest.java` | Quita Service eliminado; comprueba cuatro completados iniciales y uno en reanudación, pendientes 1 → 0 y `coverageComplete`, sin consulta posterior. |
| `application/scraping/PlayerStatisticsExtractionServiceTest.java` | Observa conteos por cierre de run y métricas por `PlayerStatisticsQueryService`: rating decimal exacto, gol ausente y NO_DATA tras fallo. Sin consultas directas a Repository. |
| `application/json/JsonStatisticsUpdateServiceTest.java` | Sustituye CoverageQuery y Repository en assertions por cierre y lectura individual: confirmado/pending, temporada, rating, ausencias y conteos 2/55; mantiene rechazo de duplicado y reconfirmación. Una campaña anterior no oculta la fecha más reciente consultable. |
| `infraestructure/PlayerStatisticsCoverageTest.java` | Adapta assertions al record interno: elegibles, excluidos y pendientes. Conserva pruebas de selección y completitud; no exige campos exclusivos eliminados. |
| `infraestructure/PlayerStatisticsPersistenceTest.java` | Añade `preservesRowsAcrossJsonDates`: ambas fechas conservan una fila y distinto rating, total dos filas. Conserva unicidad/reconfirmación y dos canales ya comprobados aquí, en frontera Repository válida. |
| `e2e/PlayerStatisticsSyncJourneyE2ETest.java` | Quita GET y DTO retirado; observa fecha, elegibles, pending, run y resultado en respuesta POST. |
| `e2e/PlayerStatisticsCrossChannelJourneyE2ETest.java` | Quita helpers GET; comprueba contratos de ambas respuestas, omisión de completados y fecha inicial. Agrega 404 HTTP autenticado y `verifyNoInteractions` con las integraciones de ambos canales. |

No se eliminan garantías de persistencia por quitar una observación indebida desde Service: la unicidad/reconfirmación ya se comprueban en `PlayerStatisticsPersistenceTest`, y se añade allí la comprobación de dos fechas antes observada por conteo de Repository en el test JSON. No se crean clases de test ni nuevas etiquetas. Los nuevos casos permanecen dentro de `@Nested` y los tags/perfiles existentes se conservan.

## Artefactos documentales

- `docs/backend/architecture.md` §3: inventario y contrato sin GET, cálculo interno conservado.
- `docs/TECHNICAL_DEBT.md`, TD-032: actualiza el retiro del consumidor de su fórmula defectuosa, sin declarar resolución. TD-033 se conserva.
- `specs/012-player-statistics-scraping/spec.md`, `plan.md`, `data-model.md`, `contracts/http.md`, `quickstart.md`: conservan decisión, antecedentes e implementación/validación pendientes; guía incluye comandos humanos actuales.
- `specs/012-player-statistics-scraping/tasks.md`: autorización, siete tareas marcadas por escritura/revisión y evidencia real, no por tests.
- Este reporte: inventario, verificación y revisión humana. `research.md` y checklists se conservan como preparación previa; no se cambian sus aprobaciones durante IMPLEMENT.

## Verificaciones y riesgos pendientes

- Búsqueda Java: sin referencias al Service/Mapper/DTO retirados, `systemicallyAbsentMetrics`, `INVALID_STATISTICS_SOURCE` ni consulta eliminada. `/coverage` aparece únicamente en tres regresiones de ruta inexistente.
- Primer `gradlew.bat build -x test`: compilación productiva realizada, control de formato fallido por cambios de esta tarea. Se corrigieron fuentes mediante patch, sin editar salidas generadas ni ejecutar formatter independiente.
- Segundo build idéntico: **BUILD SUCCESSFUL en 10 s**; validación estática de clasificación de 137 clases. Sin `test`, suites parciales, `compileTestJava` ni `testClasses`; no compila ni ejecuta tests. Aviso deprecated de la fachada preexistente, sin modificar ese cuerpo.
- Diff y referencias revisados; no migraciones, DB, navegador/proveedor real, commit, push ni cambios de índice efectuados por esta tarea. Los movimientos pendientes del refactor anterior no se revierten.
- Pendiente verificar por ejecución humana el 404, la resolución Spring y los tests actualizados. No se afirma ningún resultado funcional ejecutado por el agente.
- TD-033 preexistente: journeys con mocks de Integration no cubren la conexión al adaptador productivo. Se conservan, sin presentarlos como E2E normativamente completos ni cerrar la feature por su resultado.

## Orden y checklist de CODE REVIEW

Revisar Controller y piezas eliminadas → cálculo/record interno frente a `ExtractionRunManager.finish` → advice/seguridad/repositorios → diez tests → contrato y documentación. Inspeccionar especialmente fórmulas de pendientes, validación UTC preservada, ausencia distinta de cero, aserciones de reanudación y fronteras de observación.

- [x] Revisé todos los archivos creados/modificados/eliminados, incluidos tests.
- [x] Entiendo qué cambió y por qué.
- [x] Revisé las decisiones de contrato y las dependencias internas conservadas.
- [x] No detecté comportamiento fuera de SPEC-012 ni pérdida de garantías de los POST.
- [x] Apruebo avanzar a TESTING de T118–T124.

Solo el usuario aprueba estas casillas. El reporte anterior T111–T117 no sustituye esta revisión ni recibe aprobación implícita.

## Testing Report — preparado, pendiente de CODE REVIEW y ejecución humana

**Tests nuevos**: casos dentro de clases existentes, no nuevas clases: 404 parametrizado, 404 con JWT/proveedores sin interacción y dos fechas JSON persistidas. **Tests modificados**: los diez de la tabla; foundation solo documentación. **Criterios**: aceptación 1 en Controller/seguridad/journey; 2 en contratos POST y Services; 3 en lecturas y persistencia; 4 en ausencia de dependencias retiradas y documentación.

**Dobles/aserciones**: MVC mantiene Service y autenticación doblados con cadena real; Services mantienen PostgreSQL y Integration controlada, rating `6.9033333333333333` exacto y null distinto de cero. Repository conserva DB real. Los journeys conservan HTTP real e Integration doblada, con límite TD-033. Riesgos de falso positivo: inspección de resúmenes no basta para unicidad persistida (cubierta en Repository); build no compila tests; el caso heredado de medianoche no simula por sí solo todas las carreras del proveedor.

Después de CODE REVIEW, revisar los tests en el orden de la tabla y ejecutar personalmente desde `backend/` en PowerShell:

```powershell
.\gradlew.bat lightweightTest --tests '*PlayerStatisticsControllerTest*' --tests '*PlayerStatisticsContractFoundationTest*' --tests '*PlayerStatisticsSecurityIntegrationTest*'
.\gradlew.bat integrationTest --tests '*PlayerStatisticsExtractionServiceTest*' --tests '*JsonStatisticsUpdateServiceTest*' --tests '*PlayerStatisticsSynchronizationServiceTest*' --tests '*PlayerStatisticsCoverageTest*' --tests '*PlayerStatisticsPersistenceTest*'
.\gradlew.bat e2eTest --tests '*PlayerStatisticsSyncJourneyE2ETest*' --tests '*PlayerStatisticsCrossChannelJourneyE2ETest*'
```

Integración y journeys requieren Docker/PostgreSQL de Testcontainers. No requieren WhoScored ni navegador real. Los filtros incluyen clases anidadas. Confirmar resultados al terminar; ante fallos aportar salida para clasificar antes de corregir. Tests y gates siguen pendientes; no hay cierre de SPEC-012 ni publicación autorizados por este reporte.

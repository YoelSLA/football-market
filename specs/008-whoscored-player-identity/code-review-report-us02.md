# Code Review Report — SPEC-008 | US02

**Feature**: [008-whoscored-player-identity](spec.md) · **User Story**: US02 — Resolver
pendientes mediante el catálogo histórico · **Tasks**: T036–T045
**Fecha**: 2026-10-14 · **Rama**: `feat/008-whoscored-player-identity`
**Autorización**: IMPLEMENT AUTHORIZATION humana `6acff0797ad21c03f5af047e`
**Toma de IMPLEMENT**: `6acff11e49c7738908b62948` (US02 pasó READY → IN PROGRESS)

**Estado**: pendiente de revisión humana. Este informe no aprueba CODE REVIEW ni
habilita TESTING.

## 1. Resumen y funcionamiento

El pase histórico es un segundo POST del mismo caso de uso, no un caso nuevo. Recibe
el `id` de un pase actual terminado, valida ese prerrequisito y vuelve a descubrir el
catálogo histórico (`isCurrent=false`) para intentar resolver **solo** los jugadores
que el actual dejó pendientes sin coincidencia o ambiguos y que, al iniciarse este
pase, siguen existiendo y sin ninguna identidad WhoScored.

Flujo efectivo:

1. `PlayerIdentityController.synchronizeHistorical` valida que `{id}` sea un `Long`
   positivo y delega sin lógica de aplicación.
2. `PlayerIdentitySynchronizationServiceImpl.synchronizeHistorical` rechaza IDs
   inválidos y pide el snapshot histórico.
3. `PlayerIdentitySnapshotCapture.capture` abre una transacción REPEATABLE_READ breve
   donde valida el origen, crea un run `HISTORICAL` con `sourceRunId`, materializa
   los atributos locales vigentes de los elegibles y registra cinco ámbitos. No
   modifica snapshots ni resultados del origen y no crea items para excluidos.
4. `PlayerIdentityLeagueDiscoveryProcessor` recorre las cinco ligas fuera de
   transacción con `historicalPage`, publica candidatos solo tras el recorrido íntegro
   y marca `SKIPPED` sin red cuando no hay elegibles.
5. `PlayerIdentityResolutionProcessor` y `PlayerIdentityReferenceConfirmation` —los
   mismos de US01— resuelven globalmente y confirman con `REQUIRES_NEW` y lock breve.
6. El cierre deriva los contadores de los agregados durables y devuelve el resumen de
   la ejecución que la propia operación provocó.

Cambios de comportamiento observables: un endpoint nuevo; errores 404/409 propios de
este prerrequisito; un nuevo código 400. No se altera `POST /sync`, ni el catálogo,
ni los contratos de Football-Data/TheSportsDB, ni se expone consulta de auditoría.

## 2. Archivos creados

Ninguno: la ampliación reutiliza el Matcher, los modelos, la migración `V6` y el
proveedor controlado ya existentes de US01.

## 3. Archivos modificados y responsabilidad de cada cambio

| Archivo | Cambio | Razón |
| --- | --- | --- |
| `repositories/PlayerIdentitySyncItemRepository.java` | `findHistoricalPendingPlayerIds(sourceRunId)` con JPQL explícito sobre `NO_MATCH`/`AMBIGUOUS` | T041: filtrado exacto de pendientes del origen, sin aproximaciones |
| `repositories/PlayerRepository.java` | `findHistoricalIdentitySnapshot(playerIds)` con `@EntityGraph` y `not exists` sobre `PlayerExternalReference` de `WHOSCORED` | T041: existencia real y ausencia de cualquier identidad WhoScored, con materialización de team/league/referencias |
| `services/internal/PlayerIdentitySnapshotCapture.java` | `Capture` gana `passType`; `current()` y `historical(sourceRunId)` sobre un `capture` privado; validación de origen y selección por pase | T042: origen elegible antes de red, 404/409 sin envolver, snapshot nuevo del histórico |
| `integrations/WhoScoredIntegration.java` | `historicalPage` reutilizando request, validación de página, pacing, límites y traducción de fallos, con `isCurrent=false` | T043: mismo contrato y ritmo que el actual |
| `services/internal/PlayerIdentityLeagueDiscoveryProcessor.java` | Elección del método de Integration según `capture.passType()` | T043: reutiliza el recorrido secuencial y el `SKIPPED` de los ámbitos |
| `services/PlayerIdentitySynchronizationService.java` | Nuevo método `synchronizeHistorical(Long)` | T043: contrato del caso de uso |
| `services/impl/PlayerIdentitySynchronizationServiceImpl.java` | Ambos pases delegan en un `synchronize(Capture)` común; validación del ID de origen | T043: cada invocación crea su propio run y comparte cierre |
| `controllers/PlayerIdentityController.java` | Nuevo `POST /sync-runs/{id}/historical` con validación de ID y OpenAPI | T044: única superficie nueva |
| `controllers/exceptions/GlobalExceptionHandler.java` | Tres handlers nuevos: `INVALID_PLAYER_IDENTITY_ID` 400, `PLAYER_IDENTITY_RUN_NOT_FOUND` 404, `PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE` 409 | T044: códigos estables del contrato; reutiliza `INVALID_PARAMETER_TYPE` |
| `docs/asciidoc/index.adoc` | Sección del pase histórico con sus includes | T045: documentación fuente; no se editaron salidas generadas |
| `test/.../WhoScoredIntegrationTest.java` | Grupo `Historical` | T036 |
| `test/.../PlayerIdentitySynchronizationServiceTest.java` | Grupo `Historical` con seis escenarios | T037 |
| `test/.../PlayerIdentityPersistenceTest.java` | Grupo `Historical` e inyección de la captura | T038 |
| `test/.../PlayerIdentityControllerTest.java` | Grupo `Historical` con REST Docs | T039 |
| `test/.../PlayerIdentityE2ETest.java` | `HistoricalJourney`, reset de catálogo y escenario histórico del proveedor | T040 |
| `test/.../support/IdentityControlledProvider.java` | Conmutable `historicalScenario` y contador de requests históricos | T040 |
| `specs/.../tasks.md` | T036–T045 marcadas y evidencia de fase | Seguimiento |

## 4. Decisiones de diseño

1. **Dos consultas y no una.** La selección de pendientes y la de jugadores elegibles
   están separadas: los items del origen declaran el resultado, el catálogo mutable
   declara existencia y referencias. Un filtro único aproximado podría dar por
   elegible a un jugador ya resuelto o excluido por error.
2. **404/409 antes de envolver.** La captura distingue sus propias excepciones de
   dominio del fallo técnico global, para que el Controller pueda traducirlas y no
   convertirlas en 500.
3. **Un solo camino de descubrimiento.** El procesador elige el método de Integration
   según el tipo de pase en lugar de duplicar el recorrido; así se conservan
   huelling de progreso, aislamiento por liga, `SKIPPED` y dedupe global.
4. **Exclusión por `not exists` de cualquier referencia.** Un jugador con varias
   referencias WhoScored heredadas queda excluido, coherente con `MULTIPLE_INITIAL_REFERENCES`.
5. **`sourceRunId` yAMBIGUOUS presentes en el DTO** sin nuevo record: el Mapper ya
   los transportaba y el contrato los exige.

## 5. Tests

| Tarea | Archivo | Qué demuestra |
| --- | --- | --- |
| T036 | `WhoScoredIntegrationTest.Historical` | `isCurrent=false`, seasons opcionales, `00101` canonicalizado a `101`, contextos repetidos con equipos distintos, huella estable sin progreso ante repetición idéntica y progreso ante contexto nuevo |
| T037 | `PlayerIdentitySynchronizationServiceTest.Historical` | Elegibles y repetición vacía sin red; origen `PARTIAL` con errores de descubrimiento y datos locales excluidos; exclusión de resueltos por un actual posterior; snapshot con atributos nuevos; ambigüedad y liga fallida aisladas; origen ausente y no elegible antes de red; ausencia de transacción durante la red histórica |
| T038 | `PlayerIdentityPersistenceTest.Historical` | Selección exacta por resultado, exclusión de referencias multiples y de jugadores inexistentes, materialización del snapshot nuevo, y conservación íntegra del origen frente a una carrera de confirmación concurrente |
| T039 | `PlayerIdentityControllerTest.Historical` | Resumen `HISTORICAL` con `sourceRunId` para `COMPLETED`/`PARTIAL`/`FAILED`, ejecución vacía con ámbitos `SKIPPED`, 400 por tipo y por ID no positivo sin invocar el Service, 404/409/500 y 401, con snippets REST Docs de cada caso |
| T040 | `PlayerIdentityE2ETest.HistoricalJourney` | Journey actual → histórico → repetición: solo el pendiente se resuelve, `sourceRunId` correcto, segunda invocación sin requests al proveedor y catálogo idéntico antes y después |

Riesgos y límites conocidos:

- Los tests de Service, Repository, E2E y REST Docs **requieren ejecución humana**;
  el build no los ejecutó. El snippet de REST Docs se genera al correr la suite.
- `PlayerIdentityE2ETest` comparte la instancia estática del proveedor controlado con
  el journey actual; el estado `historicalScenario` se restablece en `finally`.
- El test de Repository que cubre la carrera usa `NOT_SUPPORTED` y confirmaciones
  reales, igual que los casos de US01.

## 6. Verificaciones realizadas

- `.\gradlew.bat build -x test` desde `backend/`: **BUILD SUCCESSFUL** (10 tareas,
  6 ejecutadas y 4 up-to-date en la primera ejecución). Un fallo intermedio de
  formato Spotless en `PlayerIdentitySyncRunRepository` se corrigió sobre la fuente.
- Gradle informa APIs deprecadas para Gradle 10: advertencia preexistente del build.
- `git diff --check` y revisión de `git status --short`: sin diagnósticos de
  whitespace; los cambios previos de US01 permanecen intactos.
- `.specify/extensions.yml` ausente antes y después: sin hooks.
- No se ejecutaron tests, Asciidoctor con tests, Sonar, arranque de desarrollo ni
  requests al proveedor real. No hubo commits ni push.

## 7. Orden de revisión sugerido

1. `services/internal/PlayerIdentitySnapshotCapture.java` — origen elegible y selección.
2. `repositories/PlayerIdentitySyncItemRepository.java` y `repositories/PlayerRepository.java` — filtros exactos.
3. `services/impl/PlayerIdentitySynchronizationServiceImpl.java` y `services/internal/PlayerIdentityLeagueDiscoveryProcessor.java` — reutilización y `SKIPPED`.
4. `integrations/WhoScoredIntegration.java` — `historicalPage`.
5. `controllers/PlayerIdentityController.java` y `controllers/exceptions/GlobalExceptionHandler.java`.
6. `docs/asciidoc/index.adoc`.
7. Los cinco archivos de test, en el orden T036 → T040.

## 8. Checklist humano de CODE REVIEW

- [x] La elegibilidad histórica excluye resueltos, conflictos, errores y jugadores con referencias, sin filtros aproximados.
- [x] El origen no se modifica y 404/409 se producen antes de cualquier red.
- [x] El snapshot histórico captura atributos nuevos y no reevalúa identidades confirmadas.
- [x] `isCurrent=false` reutiliza validación, pacing, límites y traducción de errores del actual.
- [x] Cero elegibles completa sin red con ámbitos `SKIPPED`.
- [x] El endpoint es el único nuevo y su contrato coincide con `contracts/http.md`.
- [x] Los tres códigos nuevos son estables y no afectan errores ni seguridad ajenos.
- [x] Los tests escritos cubren los escenarios de US2 sin violar las fronteras de categoría.
- [x] Los includes AsciiDoc corresponden a snippets que la suite generará.
- [x] Nada de T046–T050 se consumió.

**Aprobación**: el revisor humano confirma o rechaza el checklist completo. El agente
no aprueba CODE REVIEW, TESTING ni DONE, y no habilita gates posteriores.
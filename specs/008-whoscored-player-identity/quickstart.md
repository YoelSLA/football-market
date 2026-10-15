# Guía de validación — Identidad WhoScored

Guía para **después de implementar** [Plan](plan.md). Los endpoints, properties y clases de tests aquí nombrados son los implementados en `feat/008-whoscored-player-identity`; su ejecución sigue correspondiendo al usuario.

Referencias: [modelo](data-model.md), [HTTP](contracts/http.md), [proveedor](contracts/whoscored.md), [evidencia](evidence/whoscored-catalog-contract.md).

## Prerrequisitos

- JDK 21 y Gradle Wrapper del backend. Docker disponible para PostgreSQL 18 de Testcontainers al ejecutar tests; no usar DB de desarrollo para pruebas automatizadas.
- Para una demostración operativa: PostgreSQL de desarrollo preparado, usuario/JWT válidos, catálogo de SPEC-002/007 persistido y cinco bindings internos correctos.
- Variables existentes JWT/Football-Data/TheSportsDB según configuración efectiva; mantener secretos fuera del repositorio. Configuración futura `whoscored` según contrato, incluido User-Agent observado y defaults de límites/ritmo.
- Fixtures controladas del proveedor para escenarios deterministas: `backend/src/test/resources/whoscored/identity/` con `catalog-current.json`, `catalog-current-final.json`, `catalog-current-empty-page.json`, `catalog-current-page-1.json`, `catalog-current-last-page.json`, `catalog-historical-page-1.json` y `catalog-historical.json`. Los JSON de [evidence](evidence/whoscored-catalog-contract.md) son ejemplos del contrato, no la suite. Los tests fijan `whoscored.page-size=3` donde reproducen las muestras y usan `support/IdentityControlledProvider` e `support/IdentityCatalogueFixture`.

## Clases implementadas

- Configuración: `config/WhoScoredProperties`, `config/WhoScoredClientConfig`, `integrations/WhoScoredRequestPacer`, `integrations/WhoScoredIntegration`.
- Modelo e identidad: `models/WhoScoredIdentityNormalizer`, `models/WhoScoredTeamAliases`, `models/WhoScoredIdentityMatcher`, `models/PlayerIdentitySyncRun`, `models/PlayerIdentityLeagueDiscovery`, `models/PlayerIdentitySyncItem`, `models/WhoScoredIdentityCandidate`, `models/PlayerIdentityDiscardedRow` y migración `V9__create_player_identity_audit.sql`.
- Servicios: `services/PlayerIdentitySynchronizationService` y su implementación, con `services/internal/` para captura de snapshot, recorrido de ligas, resolución y confirmación.
- HTTP: `controllers/PlayerIdentityController`, `controllers/mappers/PlayerIdentityMapper`, los DTO de `controllers/dtos/responses/` y los handlers propios en `controllers/exceptions/GlobalExceptionHandler`.

## Bindings de competición

`backend/src/main/resources/application.yml` declara `whoscored.competitions` con los `league-id` internos por defecto **1** Premier League, **2** Bundesliga, **3** LaLiga, **4** Serie A y **5** Ligue 1, mapeados a los `tournament-id` de WhoScored **2**, **3**, **4**, **5** y **22**. Deben coincidir con las PK reales de la tabla `leagues` del entorno; se ajustan con `WHOSCORED_PREMIER_LEAGUE_ID`, `WHOSCORED_BUNDESLIGA_ID`, `WHOSCORED_LALIGA_ID`, `WHOSCORED_SERIE_A_ID` y `WHOSCORED_LIGUE_1_ID`. `whoscored.page-size` (1–100) y `whoscored.max-pages-per-league` deben ser coherentes con el tamaño de página real del proveedor.

## Comandos para el usuario

Desde `backend/`, en Windows:

```powershell
# Build sin tests (control técnico del agente, no ejecuta la suite)
.\gradlew.bat build -x test

# Suite completa
.\gradlew.bat test

# Categorías de esta feature
.\gradlew.bat test --tests "footballmarket.models.WhoScoredIdentityMatcherTest"
.\gradlew.bat test --tests "footballmarket.models.PlayerIdentityAuditModelTest"
.\gradlew.bat test --tests "footballmarket.config.WhoScoredConfigurationTest"
.\gradlew.bat test --tests "footballmarket.integrations.WhoScoredIntegrationTest"
.\gradlew.bat test --tests "footballmarket.integrations.WhoScoredRequestPacerTest"
.\gradlew.bat test --tests "footballmarket.repositories.PlayerIdentityPersistenceTest"
.\gradlew.bat test --tests "footballmarket.services.PlayerIdentitySynchronizationServiceTest"
.\gradlew.bat test --tests "footballmarket.controllers.PlayerIdentityControllerTest"
.\gradlew.bat test --tests "footballmarket.e2e.PlayerIdentityE2ETest"

# Demostración operativa opcional con entorno dev configurado
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

No existe `PlayerIdentityAuditServiceTest`: la agregación del resumen vive en el cierre transaccional de `PlayerIdentitySynchronizationServiceImpl` y su cobertura está en `PlayerIdentitySynchronizationServiceTest` y `PlayerIdentityAuditModelTest`. No se crean perfiles alternativos ni se declara cubierto el requisito académico de separar unitarios/e2e.

## Casos implementados por clase

| Clase | Casos implementados |
| --- | --- |
| `WhoScoredIdentityMatcherTest` | Normalización NFD solo LATIN, marcas no latinas, ø/ß/apóstrofos/puntos, Pd y `White_Space`, vacíos y signos, tabla de alias y colisiones inversas, nombre+equipo del mismo contexto, homónimos, varios IDs globales, deduplicación y varios propietarios locales sin elección por orden. |
| `PlayerExternalReferenceTest` | Canonicalización e idempotencia WhoScored, rechazo de un segundo ID sin mutación parcial, IDs no representables, y multiplicidad de Football-Data/TheSportsDB preservada incluso con identidad WhoScored presente. |
| `PlayerIdentityAuditModelTest` | Origen, transiciones, tiempos, pertenencia de contextos e items, referencias iniciales múltiples y estados `COMPLETED`/`PARTIAL`/`FAILED`/vacío, con `RUNNING` como estado interno. |
| `WhoScoredConfigurationTest` | Binding y arranque, límites inválidos, User-Agent vacío o con CRLF, cinco competiciones distintas, HTTPS, redirecciones, timeouts y cliente dedicado. |
| `WhoScoredIntegrationTest` | Path y query exactos, `isCurrent=true`/`isCurrent=false`, fin por `totalPages`, primera página vacía válida, filas originales frente a descartadas, IDs numéricos/cadena/ceros, `teamId` opcional, contextos históricos repetidos, huella estable sin progreso real y errores sanitizados sin retries. |
| `WhoScoredRequestPacerTest` | Intervalo entre inicios, permiso único por instancia, concurrencia, interrupción y `Retry-After` sin repetir la solicitud fallida. |
| `PlayerIdentityPersistenceTest` | Snapshot sin filtros adicionales, JSONB tipado, auditoría durable sin cascada al catálogo, agregados por resultado, descartes, carreras, rollback referencia+item, elegibles históricos y conservación íntegra del origen. |
| `PlayerIdentitySynchronizationServiceTest` | Actual, globalidad, fallos de liga, vacíos, `LOCAL_DATA_ERROR`, contadores, repetición con run nuevo, ausencia de transacción durante la red, y los seis escenarios de US02. |
| `PlayerIdentityControllerTest` | Resumen `COMPLETED`/`PARTIAL`/`FAILED`/vacío de ambos POST, 400/401/404/409/500 con snippets REST Docs. |
| `PlayerIdentityE2ETest` | Journey actual → histórico → repetición con IDs devueltos, `sourceRunId`, sin requests al proveedor en la repetición y catálogo observablemente idéntico. |

Estados esperados al ejecutar con proveedor controlado: run `CURRENT` y luego `HISTORICAL` con `sourceRunId` al origen, `status` `COMPLETED` cuando todas las ligas son válidas, `PARTIAL` cuando alguna falla y `FAILED` solo ante fallo global; ámbitos `SKIPPED` sin red cuando no hay elegibles; `pendingPlayers` en cero al cerrar.

## Recorrido HTTP

Ejemplos Bash para operador, token obtenido por flujo de autenticación existente. Son los dos únicos endpoints de la feature. `RUN_ID` es el `id` del resumen devuelto por el primer POST; no un número inventado:

```bash
API='http://localhost:8080/api/players/identities/whoscored'
curl --request POST "$API/sync" --header "Authorization: Bearer $TOKEN"
curl --request POST "$API/sync-runs/$RUN_ID/historical" --header "Authorization: Bearer $TOKEN"
```

Cada respuesta 200 incluye `status`, `counters` con el desglose por resultado y `leagues[]` con el estado de cada ámbito, que es el mecanismo de revisión de la ejecución. Para el detalle por jugador y los candidatos observados, consultar la auditoría persistida.

El User-Agent configurado pertenece al cliente **backend → WhoScored**; no hay que copiarlo a los requests del operador a Football Market. Requests reales al proveedor son demostración variable, no sustituyen pruebas controladas.

## Escenarios y resultados esperados

| Escenario controlado | Verificación observable | Cobertura |
| --- | --- | --- |
| Cinco ligas completas; nombres normalizados y alias normativos, equipo incorrecto | Solo coincidencias únicas nombre+equipo reciben referencia; no cambia catálogo ni otros proveedores. | US01, SC-001/003/008. |
| Mismo ID repetido en páginas/ligas y equipos históricos | Un descubierto por ID y todos sus contextos relevantes, sin duplicar resultado local. | FR-003/013/020, SC-005. |
| Dos IDs válidos en ligas diferentes, invertir orden de respuesta | AMBIGUOUS en ambos recorridos, sin elegir primero. | US01 escenario 9, SC-001. |
| Página final corta y siguiente vacía; JSON servido como text/html | Termina en currentPage==totalPages sin pedir siguiente; acepta JSON estructuralmente válido con MIME incorrecto. | FR-002/015/017. |
| Página intermedia inválida, metadatos cambiantes, repetición, 403/429/timeout o límite | Liga FAILED, cero referencias desde ella; otras completas conservan resultados; errores distintos de NO_MATCH. | US01 escenarios 6–7, SC-006/007. |
| Catálogo local vacío; primera página de liga vacía válida | Run sin asociaciones; no crear jugadores. Vacío de liga según validación definida, no por HTML/403. | Edge cases, FR-004. |
| Dos ejecuciones concurrentes, misma identidad y conflicto de propietario | Cero duplicados/sustituciones; mismo ID idempotente, distinto dueño CONFLICT. | FR-009/014, SC-003. |
| Cambiar nombre/equipo local durante descubrimiento controlado | Resolver con snapshot inicial; auditoría conserva valores usados y atributos nuevos del catálogo no se sobrescriben. | US01 escenario 10, FR-025. |
| Error de persistencia al confirmar un jugador | Rollback de referencia/item exitoso, motivo PERSISTENCE_ERROR separado, otros commits conservados. | FR-016/018, SC-005/006. |
| Histórico sobre actual parcial con pendientes, conflictos y jugadores resueltos después | Solo NO_MATCH/AMBIGUOUS todavía sin referencia; nuevo snapshot del histórico y sourceRunId; evidencia actual intacta. | US02, SC-004. |
| Histórico sin pendientes / origen inexistente o no elegible | 200 vacío sin proveedor / 404 o 409 con code estable. | US02 escenario 6. |
| Revisar el resumen devuelto sin volver a consultar el proveedor | `status`, `counters` por resultado y `leagues[]` distinguen ausencia de coincidencia, ambigüedad, conflicto y error de liga. | FR-021, SC-005/007. |
| Relanzar un pase tras interrupción, sin consulta previa | Cero duplicados ni sustituciones de referencias confirmadas; nueva ejecución con id propio. | FR-014/026, SC-003. |
| Una liga falla y el resto se completa con pendientes | `status` PARTIAL, `leagues[]` identifica la liga fallida, pendientes NO_MATCH/AMBIGUOUS no degradan por sí solos el estado. | FR-016/021, SC-006/007. |
| Sin JWT e `{id}` no numérico, no positivo o de origen no elegible | 401/400/404/409 según contrato, sin requests externas al proveedor. | FR-022/023. |
| Nombre/equipo local nulo, vacío o solo signos | LOCAL_DATA_ERROR con motivo; no referencia ni NO_MATCH ni elegibilidad histórica; demás jugadores continúan. | FR-027, CHK002. |
| playerId inválido y nombres externos inválidos entre filas válidas | Solo filas defectuosas descartadas/auditadas; paging validado sobre originales; liga puede completar y asociar candidatos válidos. IDs `123`/`"00123"` se deduplican. | FR-028, CHK003/004. |
| teamId ausente con nombres válidos / equipo fuera de tabla | Matching por nombre y equipo normalizados o correspondencia explícita, sin inventar ID ni alias. | FR-006/028. |
| Referencias múltiples preexistentes / referencia única inicial | CONFLICT conservando todas / ALREADY_REFERENCED sin insertar otra. No reparación automática. | FR-009, CHK007. |
| Diacríticos latinos, marcas no latinas, guiones Pd, White_Space y apóstrofes | Pipeline exacto FR-005; conserva marcas no latinas y puntuación ajena a Pd; no convierte ø/ß. | CHK009/010/011/032. |
| Colisión de nombres en tabla / variantes de nombre de un ID | Error de configuración sin red / contextos completos, CONFLICT si varios propietarios locales. | FR-029, CHK013/027. |

Model unitario demuestra reglas puras; Integration prueba protocolo con servidor controlado; Service demuestra persistencia real por su API pública con Integration mock, sin acceso funcional directo a Repository/DB. Controller prueba HTTP/seguridad y genera REST Docs; pocos E2E cubren actual → histórico usando componentes reales y proveedor simulado. No testear Mapper aislado ni introducir APIs productivas solo para preparar tests.

Para revisar cobertura real, comparar `resolved + alreadyReferenced` con `selectedPlayers` y declarar ámbitos válidos/fecha; cero seleccionados significa cobertura no aplicable. Ningún umbral de porcentaje determina éxito. El 83% es contexto histórico, no cobertura acreditada ni valor a alcanzar. No equiparar filas externas con IDs únicos ni filas descartadas con jugadores locales. Cero métricas/cotizaciones en todas las respuestas. La tabla fija se valida contra equipos persistidos del TP; no exigir plantillas de una temporada externa ni versión dinámica.

## Evidencia de validación

Usuario revisa tests/cobertura y ejecuta los casos aplicables; si fallan, aporta salida relevante. Build no demuestra comportamiento. El 2026-10-14 el usuario aprobó CODE REVIEW y TESTING de US01 y US02 con la suite ejecutada; T046–T048 (regresiones y guía) quedan pendientes de ejecución hasta el build de T050 y su validación posterior. Registrar resumen de resultados y estado exacto evaluado durante los gates posteriores, sin atribuirles aceptación en Plan.

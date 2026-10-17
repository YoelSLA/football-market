# Testing Report — SPEC-008 | US01

## Estado

TESTING: **PENDING HUMAN VALIDATION**. Este reporte no aprueba el gate; describe qué
se escribió, qué cubre y qué debe ejecutar el usuario. CODE REVIEW de US01 quedó
aprobado el 2026-10-14 sobre el diff local descrito en
[code-review-report-us01.md](code-review-report-us01.md), incluidas sus tres
correcciones. US02 y T036-T050 siguen fuera del incremento.

## Tests escritos o modificados

| Ubicación | Enfoque | Qué cubre |
| --- | --- | --- |
| `models/WhoScoredIdentityMatcherTest.java` | Unitario puro, sin Spring/DB | FR-005/006/007/027/029 y la tabla normativa: NFD solo LATIN, conservación de no latina/acentos/apóstrofes/puntos, Pd y White_Space, vacíos y signos, alias cerrados, colisiones inversas, nombre+equipo del mismo contexto, homónimos, varios IDs globales, deduplicación y varios propietarios locales sin elección por orden. |
| `models/PlayerExternalReferenceTest.java` | Unitario | Idempotencia WhoScored, rechazo de un segundo ID sin mutación parcial, canonicalización y regresión de multiplicidad permitida para Football-Data y TheSportsDB. |
| `models/PlayerIdentityAuditModelTest.java` | Unitario | Origen, transiciones y tiempos, pertenencia de contextos e items, resultados locales exclusivos, referencias iniciales múltiples y estados COMPLETED/PARTIAL/FAILED/vacío, con RUNNING como estado interno no serializado. |
| `config/WhoScoredConfigurationTest.java` | Contexto mínimo, sin DB | Binding y arranque, límites inválidos, User-Agent vacío o con CRLF, cinco bindings distintos, HTTPS, redirecciones, timeouts y cliente dedicado. |
| `integrations/WhoScoredIntegrationTest.java` | Cliente real contra servidor controlado | Path y query exactos, `playerId` vacío, `isCurrent=true`, User-Agent y ausencia de JWT, JSON de texto/HTML, fin por `totalPages`, primera página vacía válida, índices y totales estables, filas originales frente a descartadas, IDs numéricos/cadena/ceros, `teamId` opcional, y errores 403/429/redirecciones/timeouts sanitizados sin retries. |
| `integrations/WhoScoredRequestPacerTest.java` | Tiempo y espera controlados | Intervalo entre inicios, permiso único compartido por instancia, concurrencia, interrupción y `Retry-After` sin repetir la solicitud fallida, sin dormir contra el reloj real. |
| `repositories/PlayerIdentityPersistenceTest.java` | PostgreSQL con Testcontainers y Flyway | Snapshot sin filtros adicionales, JSONB tipado, auditoría durable sin cascada al catálogo, agregados por resultado, contextos solo de ligas completas, descartes, referencias múltiples preexistentes, carreras de mismo y distinto propietario, rollback de referencia más item y conservación del catálogo. |
| `services/PlayerIdentitySynchronizationServiceTest.java` | `@SpringBootTest`, perfil test, DB real e Integration mock | Actual, globalidad, fallos de liga, vacíos, LOCAL_DATA_ERROR, contadores, repetición con run nuevo, y ausencia de transacción durante la Integration. |
| `services/PlayerIdentityAuditServiceTest.java` | `@SpringBootTest`, perfil test | Resumen interno durable, unidades y contadores reconciliados según FR-019, con `pendingPlayers` en cero al cierre y sin red. |
| `controllers/PlayerIdentityControllerTest.java` | `@WebMvcTest` con cadena Security real y Service mock | POST actual, DTO exacto para COMPLETED/PARTIAL/FAILED/vacío, 500 SYNC_FAILED/AUDIT_UNAVAILABLE y 401 sin JWT, expirado o con firma inválida. |
| `e2e/PlayerIdentityE2ETest.java` | RANDOM_PORT, perfil test, Testcontainers, proveedor controlado | Journey completo por HTTP con autenticación por el flujo existente, RestClient y DTO productivos: resumen actual, repetición con ID nuevo y conservación observable del catálogo. |
| `support/IdentityControlledProvider.java`, `support/IdentityCatalogueFixture.java` | Soporte | Proveedor controlado y catálogo aislado; los IDs internos de liga se obtienen de las APIs productivas en el Arrange, nunca se escriben a mano. |
| `test/resources/whoscored/identity/*.json`, `evidence/whoscored-catalog-contract.md` | Fixtures | Catálogo actual, última página, página vacía válida, histórico e inválido fuera de rango, derivados del contrato observado. |

## Cobertura por criterio de aceptación

- FR-001/002 (exposición única): cubiertos por `PlayerIdentityControllerTest` y el
  journey E2E; no existe GET de auditoría.
- FR-003/004 (origen, cierre y exclusividad): `PlayerIdentityAuditModelTest` y
  `PlayerIdentityAuditServiceTest`.
- FR-005/006/007 (normalización, alias y decisión global): `WhoScoredIdentityMatcherTest`
  y `PlayerIdentitySynchronizationServiceTest`.
- FR-008/009/010 (lectura del proveedor y paceo): `WhoScoredIntegrationTest` y
  `WhoScoredRequestPacerTest`.
- FR-011 a FR-018 (captura, scopios, resolución, confirmación y carreras):
  `PlayerIdentityPersistenceTest` y `PlayerIdentitySynchronizationServiceTest`.
- FR-019 (agregados y reconciliación): `PlayerIdentityAuditServiceTest`.
- FR-020 a FR-024 (persistencia de auditoría): `PlayerIdentityPersistenceTest`.
- FR-025 a FR-029 (normalización, alias y catálogo normativo):
  `WhoScoredIdentityMatcherTest` y `WhoScoredIdentityNormalizer`/`WhoScoredTeamAliases`.
- SC-001 a SC-008: extremo a extremo en `PlayerIdentityE2ETest`; el resto se cubre por capa.
- REQ-S2-02 (estadísticas, scraping futuro, cotizaciones, caché o scheduler y perfiles
  unitarios/E2E adicionales) permanece fuera de alcance y sin implementar.

## Mocks, dobles y límites

- La red nunca sale a Internet: `WhoScoredIntegrationTest` y el journey usan un
  servidor o proveedor controlado; el resto sustituye la Integration.
- No se usan mocks de repositorio en tests de servicio: el acceso a datos va por las
  APIs productivas (`PlayerCatalogService` para armar el catálogo).
- No existen métodos productivos exclusivos para testing, ni atajos de test en la
  persistencia, ni lectura de detalles de auditoría por repositorio desde la API.
- El fixture de fallo técnico se crea con un trigger de PostgreSQL dentro de una
  transacción de testing y se elimina en `finally`.

## Riesgos y falsos positivos posibles

1. **Carreras reales.** Los tests de `PlayerIdentityPersistenceTest` dependen del
   planificador y del bloqueo de PostgreSQL; pueden ser sensibles a la carga de la
   máquina. Un fallo de timeout de `Future.get` no prueba un defecto de la
   implementación.
2. **Pacer.** `WhoScoredRequestPacerTest` usa tiempo y espera controlables, pero un
   reloj del sistema alterado puede volverlo flake-sensitive.
3. **REST Docs.** `PlayerIdentityControllerTest` documenta las respuestas; los
   snippets solo se regeneran con la tarea de Asciidoctor, que el agente no ejecuta.
4. **E2E.** El journey depende de un contenedor PostgreSQL sano y de que los IDs de
   liga de testing se resuelvan por la API; un fallo de arranque del contenedor es
   causa de entorno, no de la implementación.
5. **Cobertura de código.** Un porcentaje alto no acredita los escenarios de negocio;
   los criterios se juzgan por las aserciones, no por el número de líneas.

## Orden de revisión recomendado

1. Modelos de auditoría y `PlayerExternalReference`.
2. Normalización, alias y matcher.
3. Properties, cliente, pacer e Integration.
4. Repositorios y migración V6.
5. Captura, scopios, resolución, confirmación y cierre.
6. DTO, mapper, controller, handler y OpenAPI.
7. Tests de servicio, controller y journey E2E.

## Comandos exactos para el usuario

Desde `backend/`, en Windows:

```powershell
.\gradlew.bat test
```

Solo una categoría (más rápido para iterar):

```powershell
.\gradlew.bat test --tests "footballmarket.models.WhoScoredIdentityMatcherTest"
.\gradlew.bat test --tests "footballmarket.config.WhoScoredConfigurationTest"
.\gradlew.bat test --tests "footballmarket.integrations.WhoScoredIntegrationTest"
.\gradlew.bat test --tests "footballmarket.integrations.WhoScoredRequestPacerTest"
.\gradlew.bat test --tests "footballmarket.repositories.PlayerIdentityPersistenceTest"
.\gradlew.bat test --tests "footballmarket.services.PlayerIdentitySynchronizationServiceTest"
.\gradlew.bat test --tests "footballmarket.services.PlayerIdentityAuditServiceTest"
.\gradlew.bat test --tests "footballmarket.controllers.PlayerIdentityControllerTest"
.\gradlew.bat test --tests "footballmarket.e2e.PlayerIdentityE2ETest"
```

Control técnico permitido del agente, distinto de la ejecución de tests:

```powershell
.\gradlew.bat build -x test
```

Informe de fallos: `backend/build/reports/tests/test/index.html`. Si falla alguna
categoría, compártí la salida relevante del reporte o del log de Gradle.

## Checklist humano obligatorio de TESTING

- [x] Ejecuté la suite y revisé el resultado de todas las categorías.
- [x] Confirmé que los criterios de aceptación y SC-001 a SC-008 quedan cubiertos
      por aserciones, no solo por cobertura de líneas.
- [x] Revisé que los dobles no oculten comportamiento productivo (red real
      sustituida por proveedor controlado, sin atajos de test en la implementación).
- [x] Verifiqué que los fixtures de PostgreSQL, carreras y REST Docs se comportaron
      como se espera.
- [x] Acepto o rechazo el resultado y autorizo explícitamente el cierre de US01.

Sin los cinco puntos marcados, US01 no pasa a DONE. La Feature permanece
IN PROGRESS y no procede CONVERGE ni cierre.

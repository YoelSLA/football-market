# Testing Report — SPEC-008 | US02

## Estado

TESTING: **APPROVED** por el usuario el 2026-10-14, tras la revisión previa de los tests
y la ejecución de la suite con resultado satisfactorio aportado por él. CODE REVIEW de
US02 quedó aprobado el mismo día sobre el diff local descrito en
[code-review-report-us02.md](code-review-report-us02.md). Evidencia persistida en
https://trello.com/c/HGkmY1Le, comentarios `6ad0377ce9a05542a8dd2df1` y
`6ad038c6199650ee939dfc4d`. US02 pasa a **DONE**. T046–T050 y las historias retiradas
siguen fuera del incremento.

## Tests escritos o modificados

| Ubicación | Tarea | Enfoque | Qué cubre |
| --- | --- | --- | --- |
| `integrations/WhoScoredIntegrationTest.java` (grupo `Historical`) | T036 | Cliente real contra servidor controlado | `isCurrent=false`, temporadas y `seasonName` opcionales, canonicalización de `playerId` (`00101` → `101`), ID repetido con equipos/temporadas/variantes distintos, deduplicación canónica y ausencia de progreso real ante repetición idéntica frente a progreso ante contexto histórico nuevo. Mantiene paging, límites y traducción de errores del actual. |
| `services/PlayerIdentitySynchronizationServiceTest.java` (grupo `Historical`) | T037 | `@SpringBootTest`, perfil test, DB real e Integration mock | Los seis escenarios de US2: elegibles resueltos y repetición vacía sin red; origen `PARTIAL` con errores de descubrimiento y datos locales defectuosos; exclusión de jugadores ya resueltos por un actual posterior; snapshot histórico con atributos nuevos; ambigüedad y liga fallida aisladas; origen ausente y no elegible rechazados antes de red; ausencia de transacción durante la red histórica. |
| `repositories/PlayerIdentityPersistenceTest.java` (grupo `Historical`) | T038 | PostgreSQL con Testcontainers y Flyway | Consulta de elegibles `NO_MATCH`/`AMBIGUOUS` aún existentes, exclusión de referencias múltiples y de jugadores inexistentes, snapshot histórico nuevo y conservación íntegra de run/items/candidatos del origen frente a una carrera de confirmación concurrente. |
| `controllers/PlayerIdentityControllerTest.java` (grupo `Historical`) | T039 | `@WebMvcTest` con cadena Security real y Service mock | Resumen `HISTORICAL` con `sourceRunId` para `COMPLETED`/`PARTIAL`/`FAILED`, ejecución vacía con ámbitos `SKIPPED`, 400 por tipo y por ID no positivo sin invocar el Service, 404 de origen ausente, 409 no elegible, 500 y 401, con snippets REST Docs de cada respuesta contractual. |
| `e2e/PlayerIdentityE2ETest.java` (`HistoricalJourney`) + `support/IdentityControlledProvider.java` | T040 | RANDOM_PORT, perfil test, Testcontainers, proveedor controlado | Journey actual → histórico → repetición usando los IDs devueltos: solo el pendiente se resuelve, `sourceRunId` correcto, segunda invocación sin requests al proveedor y catálogo idéntico antes y después. |

No se crearon archivos de test nuevos: US02 amplía los cinco archivos de US01, como
exige `tasks.md`.

## Relación con la US y criterios cubiertos

- FR-001 (dos pases y cinco ligas): grupo `Historical` de Integration y de Service.
- FR-003 (IDs y contextos globales, incluidos históricos): T036.
- FR-012 (elegibilidad por resultado y existencia real): T038, T037.
- FR-013 (reglas compartidas y deduplicación de contextos): T036, T037.
- FR-014 (idempotencia y carreras): T038, T040.
- FR-016 (aislamiento y estado parcial): T037, T039.
- FR-021 (resumen devuelto por la propia ejecución): T039, T040.
- FR-022/023 (seguridad, errores y contrato HTTP): T039.
- FR-025 (snapshot inmutable por pase): T037, T038.
- FR-026 (run nuevo al repetir): T037, T040.
- SC-004 (referencias nuevas solo del conjunto pendiente elegible): T037, T038, T040.
- SC-005 y SC-007 (auditoría conservada y resumen reconciliado): T038, T039, T040.

Escenarios de US2 de `spec.md` (1–6) quedan mapeados uno a uno en la tabla anterior
y en la sección 5 del Code Review Report.

## Mocks, dobles y límites

- La red nunca sale a Internet: `WhoScoredIntegrationTest` usa servidor controlado y
  el journey usa el proveedor controlado con escenario y contador de requests
  históricos conmutables, restablecidos en `finally`.
- El Arrange del Service y del journey prepara el origen mediante las APIs
  productivas, en particular `PlayerCatalogService`; no hay preparación por SQL en
  tests de Service o E2E.
- Sin mocks de repositorio en tests de servicio, sin atajos de test en la
  implementación y sin métodos productivos exclusivos para testing.
- El test de carrera de `PlayerIdentityPersistenceTest` usa `NOT_SUPPORTED` y
  confirmaciones reales, igual que los casos de US01.

## Assertions importantes

- Los candidatos del pase histórico se acumulan aunque repitan `playerId`: la
  deduplicación es canónica por ID y los contextos se conservan.
- La huella de página se mantiene estable ante repetición idéntica y cambia ante un
  contexto histórico nuevo (prueba indirecta de ausencia de progreso real).
- Cero elegibles ⇒ run `HISTORICAL` cerrado `COMPLETED` con ámbitos `SKIPPED` y
  **ningún** request al proveedor.
- El origen no muta: run, items, candidatos y contadores se releen idénticos tras el
  pase histórico y ante una carrera concurrente.
- `{id}` no positivo o de tipo inválido se rechaza sin invocar el Service.

## Posibles falsos positivos o puntos delicados

1. **Carreras reales**: los casos de `PlayerIdentityPersistenceTest` dependen del
   bloqueo de PostgreSQL y del planificador; un timeout de `Future.get` no prueba un
   defecto de la implementación.
2. **REST Docs**: los snippets del grupo `Historical` solo existen si corre la suite
   y luego la tarea de Asciidoctor, que el agente no ejecuta. Un `build -x test`
   verde no valida los includes de `index.adoc`.
3. **E2E**: el journey depende de un PostgreSQL sano y de que los IDs de liga de
   testing se resuelvan por API; un fallo de arranque del contenedor es causa de
   entorno.
4. **Estado compartido del proveedor controlado**: `historicalScenario` es compartido
   con el journey actual; conviene revisar el `finally` si se agregan escenarios.
5. **Cobertura**: un porcentaje alto no acredita los seis escenarios de US2; se juzga
   por aserciones.

## Orden recomendado de revisión

1. `WhoScoredIntegrationTest.Historical` (protocolo y contextos).
2. `PlayerIdentityPersistenceTest.Historical` (elegibilidad y conservación del origen).
3. `PlayerIdentitySynchronizationServiceTest.Historical` (escenarios de US2).
4. `PlayerIdentityControllerTest.Historical` (contrato HTTP y REST Docs).
5. `PlayerIdentityE2ETest.HistoricalJourney` (journey completo).

## Comandos exactos para el usuario

Desde `backend/`, en Windows:

```powershell
.\gradlew.bat test
```

Solo las categorías del incremento:

```powershell
.\gradlew.bat test --tests "footballmarket.integrations.WhoScoredIntegrationTest"
.\gradlew.bat test --tests "footballmarket.repositories.PlayerIdentityPersistenceTest"
.\gradlew.bat test --tests "footballmarket.services.PlayerIdentitySynchronizationServiceTest"
.\gradlew.bat test --tests "footballmarket.controllers.PlayerIdentityControllerTest"
.\gradlew.bat test --tests "footballmarket.e2e.PlayerIdentityE2ETest"
```

Control técnico permitido del agente, distinto de la ejecución de tests:

```powershell
.\gradlew.bat build -x test
```

Informe de fallos: `backend/build/reports/tests/test/index.html`. Si falla alguna
categoría, compartí la salida relevante del reporte o del log de Gradle.

## Estado del gate

`APPROVED` por el usuario el 2026-10-14, con resultado de suite satisfactorio aportado
por él. Con CODE REVIEW (`6ad0377ce9a05542a8dd2df1`) y TESTING
(`6ad038c6199650ee939dfc4d`) vigentes y persistidos, la transición de US02 a DONE es
mecánica. La Feature permanece IN PROGRESS: T046–T050 sin autorizar, sin CONVERGE y
sin FEATURE COMPLETION AUTHORIZATION.

## Checklist humano obligatorio de TESTING

- [x] Ejecuté la suite y revisé el resultado de las cinco categorías del incremento.
- [x] Confirmé que los seis escenarios de US2 y FR-012/013 quedan cubiertos por
      aserciones, no solo por cobertura de líneas.
- [x] Revisé que los dobles no oculten comportamiento productivo (red sustituida por
      proveedor controlado, sin atajos de test en la implementación).
- [x] Verifiqué que el origen auditado se conserva y que el proveedor no recibe
      requests en la repetición ni con cero elegibles.
- [x] Acepto o rechazo el resultado y autorizo explícitamente el cierre de US02.
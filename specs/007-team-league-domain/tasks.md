# Tasks — simplificación definitiva SPEC-007

Decisión humana 2026-10-10: actualización SDD e implementación autorizadas, sin operaciones PostgreSQL/tests/commits. Los IDs y el avance anteriores a esa decisión no se reinterpretan: los Tasks enumerados abajo son el seguimiento vigente.

## Impacto en tareas anteriores

T024–028, T039/040/052 se RETIRAN por transición legacy. P09–P12 y preparación/cableado canónico se RETIRAN. T001–023, T029–038, T041–051 conservan responsabilidad funcional cuando aplicable; su validación anterior no acredita el diff nuevo. Nuevos IDs no reutilizan los retirados.

## Preparación y foundation

- [x] T053 Incorporar decisión humana a spec/plan/research/data-model/quickstart/contratos/checklist y seguimiento; preservar antecedentes y sincronizar trazabilidad sin aprobar gates.
- [x] T054 Analizar coherencia del alcance nuevo y Constitución, cobertura US01–US05 y ausencia de dependencias legacy; registrar resultado fuera del análisis.
- [x] T055 Adaptar V5 al esquema definitivo conservando V1–V4; retirar V5.1/V6; NOT NULL/FK `players.team_id` y `teams.league_id`; mantener unicidades y cardinalidad (RF-001–014).
- [x] T056 Retirar CatalogTransition y familia canónica Model/records/enums/Repository/Service/Orchestrator y consumidores; quitar `current_revision_id`/causas legacy, sin eliminar casos funcionales (RF-031).

## Implementación funcional

- [x] T057 [US01] Mantener query GET, contrato y consumidores con cuatro campos derivados y filtros; Team obligatorio en Model/JPA/SQL (RF-004–006/019/028/029).
- [x] T058 [US02] Retirar `completeTransition`/backfill/espejo/captura canónica/serializador artesanal del catálogo; conservar snapshot completo, transacciones, casos/protección/replay y B-V5-01–05 (RF-014–016/024/031/032/037/039).
- [x] T059 [US03] Conservar rename, transferencia, cambio de liga, propietario inmutable y regresiones de imágenes (RF-009–013/030).
- [x] T060 [US04] Conservar TeamEnrichmentService, matcher estricto, intentos, Persistable y Retry-After/pacing (RF-020–023/035/038).
- [x] T061 [US05] Conservar current/active, protección y retirada/regreso sin borrado (RF-025–027/033/039).

## Tests y entrega

- [x] T062 Reemplazar tests exclusivamente legacy/canónicos por tests de esquema limpio/FKs/NOT NULL/UNICIDAD e instalación V1–V5 en PostgreSQL Testcontainers; adaptar migraciones Player/imágenes, sin DB local.
- [x] T063 Adaptar fixtures/query tests PlayerCatalogPersistenceTest y PlayerSnapshotConsistencyPersistenceTest; retirar solo casos sin Team y marcador, conservar contradicciones, cardinalidad y opcionales (US01–US03/US05).
- [x] T064 Conservar suites funcionales Service/Integration/Controller/Orchestrator y regresiones imágenes; casos deduplicados y rollback técnico por Repository; no reflexión de privados ni API exclusiva para tests (US01–US05).
- [x] T065 Documentar comandos humanos de tests/build y recreación DEV acotada, verificar diff/índice; sin tests, commits ni gates aprobados.

Orden: T053 → T054 → T055/T056 → T057–061 → T062–064 → T065. No delegación implícita ni paralelismo sobre archivos compartidos. Tests los ejecuta usuario exclusivamente; build backend permitido tras cambios, no acredita TESTING.

## Estado

ANALYZE 2026-10-10, read-only sobre artefactos actualizados: sin hallazgos HIGH/CRITICAL de diseño. 36 RF vigentes cubiertos por T055–T064; RF-017/034/036 retirados, US06/SC-009/010 no requieren cobertura. US01–US05 y SC-001–008/011–012 conservados. T053/054/065 transversales tienen responsabilidad SDD/entrega, no funcionalidad inventada. Constitución/matriz/testing compatibles. Observación LOW: checklist humano nuevo pendiente no acredita evaluación anterior; historial está separado y no gobierna implementación. Prerequisites resueltos mediante bash (script sin permiso ejecutable), sin hooks configurados. Este registro es posterior al análisis, no modificación dentro de él. READY técnico para el alcance autorizado, no CODE REVIEW/TESTING ni cierre.

Implementación pendiente de verificación del nuevo alcance. US06 retirada, no DONE. Recreación PostgreSQL no autorizada; no ejecutar migraciones sobre la base anterior. CODE REVIEW/TESTING/cierre requieren evidencias humanas nuevas según impacto.

### Implementación 2026-10-11

T053–T065 marcados: el código y la documentación existen en el árbol. Un build sin tests (`./gradlew build -x test`, control ejecutable permitido al agente) finaliza **BUILD SUCCESSFUL**, lo que acredita compilación de main y test, formato Spotless y empaquetado.

**Evidencia de TESTING 2026-10-11.** El usuario ejecutó manualmente `./gradlew test --rerun-tasks` con resultado **BUILD SUCCESSFUL**, sobre el árbol que incluye el refactor de `PlayerCatalogServiceImpl` y los colaboradores de `services/internal`. Con esto queda verificada funcionalmente la composición interna, incluido el reordenamiento de escrituras, la atomicidad y el replay. Registro de verificación humana; el agente no ejecuta tests.

**Clasificación arquitectónica resuelta 2026-10-11.** No existía contradicción normativa sino ambigüedad de criterio: `docs/backend/architecture.md` §1.1 define el rol Service por una condición acumulativa (lógica de aplicación **más** interfaz/implementación separadas con consumidores dependientes de la interfaz), mientras §1.4 asigna al rol Service la lógica que requiere Repository. Los colaboradores de `services/internal` cumplen la lógica pero no la condición de interfaz, por lo que **no son Services independientes**: son composición interna del rol Service, consumida solo por `PlayerCatalogServiceImpl`, que sigue siendo la fachada del caso de uso y el propietario del límite transaccional único (RF-032). La prohibición Service → Service permanece intacta: no se extiende a componentes que nunca fueron Service, y no se habilita reutilización transversal ni ciclos. Se añadió el párrafo aclaratorio en §1.4; §1.1, §1.3 y la constitución no se modificaron.

**Hallazgo HIGH previo invalidado.** El Code Review del refactor había reportado como ALTO una violación de Service → Service. Ese hallazgo queda **INVALIDADO por clasificación incorrecta**: se aplicó el criterio de §1.4 (usa Repository) sin verificar antes la definición de §1.1, que es el criterio de clasificación. No existía conflicto entre roles regulados. El análisis corregido y su evidencia quedan registrados aquí; no se conserva como pendiente ni como riesgo vigente.

**Enmienda constitucional 6.1.0 ratificada 2026-10-11.** El usuario ratificó explícitamente la enmienda 6.0.0 → 6.1.0 y el nuevo §2.1 sobre gestión de datos en desarrollo, aprobando el incremento MINOR y su contenido. Se conserva la fecha de ratificación de la norma base (2026-08-31). La procedencia editorial previa de esa modificación en el worktree queda como **no determinada**: no se atribuye autoría y lo que queda autorizado es su contenido a partir de esta ratificación. La ratificación **no altera §6, §8 ni §9**, no autoriza por sí sola operaciones destructivas sobre la base de datos y no exige compatibilidad retroactiva para datos descartables de DEV. No modifica ninguna decisión de SPEC-007: la confirma. Sin cambios derivados requeridos en `AGENTS.md`, `docs/backend/architecture.md`, `docs/backend/conventions.md` ni `docs/backend/testing.md`, porque ninguno de ellos regula compatibilidad retroactiva de datos, orden de migraciones ni reutilización de estructuras de desarrollo; §2.1 se limita a datos y deja intactos arquitectura, contratos, convenciones y estrategia de testing.

## Cierre formal 2026-10-11 — SPEC-007 DONE

Autorizaciones humanas explícitas del usuario, otorgadas sobre la evidencia acreditada:

| Gate | Estado | Evidencia |
| --- | --- | --- |
| CODE REVIEW | **APPROVED** | Aprobado explícitamente por el usuario el 2026-10-11 sobre el árbol vigente. |
| TESTING | **APPROVED** | Evidencia: `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**, suite completa verde. Ejecución manual del usuario; el agente no ejecuta tests. |
| FEATURE COMPLETION AUTHORIZATION | **APPROVED** | Otorgada explícitamente por el usuario el 2026-10-11. |

**Estado**: 13/13 tareas de implementación completadas. SPEC-007 **DONE**.

Evidencia de soporte: compilación de main y test correcta y Spotless OK mediante `./gradlew build -x test` (control ejecutable permitido al agente); suite completa verde sobre el árbol actual; `PlayerCatalogServiceImpl` refactorizado de 519 a 191 líneas con siete colaboradores cohesionados en `services/internal`; ambigüedad de clasificación arquitectónica resuelta en `docs/backend/architecture.md` §1.4; Constitution **6.1.0** vigente con §2.1 ratificado; esquema definitivo en V5 sin V5.1 ni V6.

**Hallazgos y bloqueos retirados del estado vigente.** El hallazgo HIGH previo sobre Service → Service queda invalidado por clasificación incorrecta. T028, P12, CR-02 y los bloqueos de RELEASE 1/2 gehören a un alcance retirado por decisión humana del 2026-10-10 y no gobiernan el cierre. V5.1 y V6 se eliminaron físicamente y no son trabajo pendiente. La reversibilidad y la migración de datos DEV son descartables por Constitution 6.1.0 §2.1.

**US06**: retirada del alcance, **no entregada ni DONE**. Conserva identificador y tarjeta como trazabilidad histórica.

**Acción operativa posterior, no bloqueante**: recrear PostgreSQL DEV según [quickstart.md](quickstart.md) §3 para poder arrancar el backend local contra el esquema definitivo de V5. Constitution 6.1.0 §2.1 habilita descartar datos de desarrollo descartables; su ejecución respeta §6 y §8 y requiere autorización específica. No bloquea el cierre funcional.

### Corrección de dominio 2026-10-14 — `V8__converge_team_league_domain`

**Hallazgo.** La acción operativa anterior no era necesaria para desbloquear el arranque, y su premisa era incorrecta. `V5__create_team_league_domain.sql` se aplicó en dos formas sucesivas en el historial: `f7be382` la creó aditiva (con `players.team`, `players.league`, `team_id` nullable y la tabla de control `catalog_transition`) y `dd6fda3` la reescribió a la forma definitiva, tal como registra `T055` al decidir retirar `V5.1` y `V6`. Toda base que aplicó `V5` antes de `dd6fda3` conserva el checksum de la forma aditiva, y Flyway aborta el arranque con *Migration checksum mismatch for migration version 5*. La base de desarrollo era ese caso: 8 migraciones en el repositorio contra `V5` registrada con el checksum de `f7be382`.

**Decisión humana 2026-10-14.** En lugar de recrear la base y descartar los 2634 jugadores del catálogo, el usuario autorizó conservar los datos. Se desvía de la previsión registrada arriba, que queda superada solo en su parte de recreación: la habilitación de Constitution 6.1.0 §2.1 sigue vigente y no se ejerce.

**Solución aplicada.** Se agregó `V8__converge_team_league_domain.sql`, migración correctiva e idempotente: cuando `players` expone los textos legacy, deriva las ligas y equipos desde el catálogo existente, asigna `players.team_id`, retira `team` y `league`, vuelve `team_id` obligatorio, elimina `catalog_transition` y endurece el CHECK de categorías de `pending_review_cases`. Si `players` ya está en la forma definitiva, el bloque no hace nada, de modo que una instalación limpia y una base ya convergida quedan en el mismo estado. Antes de `V8` se realineó el checksum registrado de `V5` con `flyway repair`; la reparación solo actualiza metadatos y no ejecuta migraciones ni altera datos. `V6` y `V7` de la ampliación de SPEC-001 son migraciones distintas y no guardan relación con el `V6` que esta spec retiró físicamente.

**Efecto en el dominio.** Los 2634 jugadores se conservan y quedan asociados a 96 equipos derivados de sus textos legacy, repartidos en las 5 ligas del catálogo. La resolución de identidad de `TeamIdentityMatcher` es por nombre normalizado, por lo que el próximo catálogo reutiliza esos equipos y los enriquece con su referencia externa en lugar de duplicarlos.

**Cobertura.** `TeamLeagueConvergenceMigrationTest` reconstruye la forma aditiva sobre una base migrada y verifica la convergencia, la preservación de jugadores, la derivación de ligas y equipos, la retirada de textos legacy, la obligatoriedad de la asociación, la eliminación de la tabla de control, el CHECK de categorías y la no modificación de una base ya definitiva. Su ejecución corresponde al usuario.

# Plan de implementación: Identidad externa de jugadores en WhoScored

## Diseño vigente — simplificación 2026-11-01

Rama: `refactor/008-whoscored-player-identity`. Fuente: [spec.md](spec.md), sesión 2026-11-01 y [clarificación](clarify-sync-simplification.md). Este apartado sustituye el diseño de auditoría y endpoints separados que se conserva abajo como antecedente histórico. Preparación completada tras revisión del checklist por el usuario, Tasks y ANALYZE; implementación y build sin tests entregados en [reporte de revisión](code-review-sync-simplification.md), pendientes de revisión y testing humanos.

**Estado posterior**: revisión y testing aprobados por el usuario, tests satisfactorios informados por él y convergencia final sin pendientes. Cambio cerrado por autorización humana; [evidencia de cierre](convergence-sync-simplification.md). La rama general de integración es `refactor/feature-implementation-cleanup`; el alcance de esta spec no se extiende a otras features por compartir esa rama.

### Resumen y contexto técnico

Una sola operación `POST /api/players/identities/whoscored/sync` obtiene referencias actuales y después históricas, devolviendo totales finales de jugadores locales. Se reutilizan Java 21, Spring Boot 4.1.1, JPA/PostgreSQL, TransactionTemplate, RestClient y Flyway efectivos en el backend. No se agregan dependencias ni se modifica la configuración del proveedor. La red y el matching permanecen fuera de transacciones.

### Constitution Check anterior al diseño

- Simplicidad y alcance: retirar persistencia exclusivamente de auditoría; no eliminar reglas de identidad, unicidad ni protección frente a consultas incompletas.
- Contratos: cambio incompatible explícito de dos operaciones a una; actualizar Controller, Mapper, DTO, handler, consumidores y tests afectados, contrato y guía en el mismo alcance.
- Autorización: el usuario autorizó spec y código; mantener estructura y clases de `application`, adaptando sus responsabilidades. Casos de revisión de catálogo y matcher de imágenes quedan intactos.
- Arquitectura: interfaz/implementación de Service obligatorias; componentes internos componen ese caso de uso, sin introducir Service → Service ni un Orchestrator innecesario. DTO y Mapper se limitan a presentación.
- Verificación: escribir o adaptar tests backend, sin ejecutarlos. Solo build de backend sin tests después de cambios de código; no build durante esta fase documental.
- Requisitos externos: preservar catálogo, referencias y observabilidad existentes. La retirada de auditoría WhoScored no afecta auditoría financiera del enunciado ni requisitos futuros de auditoría de servicios.

### Diseño del flujo y responsabilidades conservadas

1. `PlayerIdentitySynchronizationService` ofrece una operación `synchronize()`; su implementación coordina el flujo completo y calcula el estado global. Eliminar de su API la ejecución independiente de pases y su selección por ID de origen; adaptar consumidores internos de tests de estadísticas.
2. `PlayerIdentitySnapshotCapture` captura en transacción corta REPEATABLE_READ jugadores locales de las cinco ligas y referencias iniciales. No crea runs, items ni ámbitos persistidos. La selección inicial fija el universo contabilizado una sola vez. El histórico recaptura solo IDs elegibles de ese universo para respetar FR-025.
3. `PlayerIdentityLeagueDiscoveryProcessor` consulta páginas secuencialmente. Valida paginación, estabilidad y progreso como antes, publica en memoria solo candidatos de ligas completas y descarta datos parciales de ligas fallidas. Mantiene internamente la completitud necesaria para clasificar errores y el estado del pase, no tiempos, contadores de auditoría o resultados por liga para HTTP. Se conserva el ritmo y los límites del cliente existente.
4. `PlayerIdentityResolutionProcessor` invoca el matcher sobre candidatos de todas las ligas válidas, fuera de transacción, antes de confirmar referencias. Mantiene decisiones finales por ID local en memoria. Conserva desambiguación y detección de varios propietarios antes de escribir.
5. `PlayerIdentityReferenceConfirmation` confirma solo referencias de catálogo, en REQUIRES_NEW por jugador con lock breve. Relee referencias actuales y propietario externo; misma identidad concurrente es idempotente, otra identidad es conflicto. Clasifica violaciones conocidas por SQLSTATE/constraint después del rollback en transacción nueva; errores técnicos dejan al jugador sin resolver, sin escritura de auditoría. No guarda snapshots detached ni sobrescribe atributos del catálogo.
6. Actual FAILED omite histórico. En otro caso el histórico recibe solo NO_MATCH/AMBIGUOUS aún sin referencia. Cero elegibles completa sin red. Un elegible resuelto concurrentemente conserva su referencia y se cuenta con referencia válida; borrado durante el proceso queda sin resolver, sin desaparecer del denominador. Referencias contradictorias quedan como conflicto. El resultado histórico sustituye al provisional del actual por ID local; nunca se suman totales de pases.
7. El resumen de dominio contiene estado global, `selectedPlayers`, `referencedPlayers`, `noMatchPlayers` y `unresolvedPlayers`; debe reconciliar `selectedPlayers = referencedPlayers + noMatchPlayers + unresolvedPlayers`. Los mismos campos pasan a un único Response DTO por Mapper estructural.
8. Estado de cada pase conserva la semántica de FR-016: vacío o trabajo completo sin errores técnicos → COMPLETED; trabajo incompleto con alguna liga completada o resultado no técnico → PARTIAL; resto → FAILED. Global: FAILED > PARTIAL > COMPLETED. Interrupción conserva referencias confirmadas, clasifica lo pendiente como sin resolver, no inicia nuevas llamadas y restaura el indicador de interrupción al salir.

`PendingReviewRecorder`, `PendingReviewCaseRepository` y sus tipos permanecen sin modificación funcional. También `PlayerIdentityMatcher` y `PlayerIdentityAliases`, utilizados por imágenes. Se mantienen nombres/ubicaciones de componentes de `application`; se elimina únicamente código que deja de tener responsabilidad (incluidas excepciones del antiguo origen histórico).

### Modelo, persistencia y retirada de código

- Mantener `PlayerExternalReference` y sus constraints en ambos sentidos para WhoScored.
- Retirar cinco repositorios de auditoría WhoScored: ejecuciones, items, candidatos, descubrimiento por liga y descartes. Retirar `PlayerIdentitySyncRun` y `PlayerIdentityDiscardedRow`; reemplazar las representaciones de items/candidatos/descubrimiento por valores transitorios mínimos allí donde el matching todavía los necesita. El nombre exacto del tipo no justifica una abstracción adicional.
- Reducir contextos de candidato a los datos efectivamente utilizados en la comparación, sin mezclar nombre/equipo de filas distintas. No reutilizar contratos externos como Model.
- Mantener resultados internos necesarios para elegibilidad, conflicto y agregación; simplificar enums sin uso y los records de contadores/resumen de auditoría. Eliminar DTO por liga y contadores detallados.
- Retirar handlers/excepciones de ID de ejecución, origen inexistente/no elegible; conservar error controlado de fallo de preparación que impida producir un resumen coherente.
- Crear una migración aditiva posterior a las existentes, sin editar V9 ni ejecutar la base: retirar exclusivamente cinco tablas de auditoría y sus secuencias, en orden de dependencias, sin CASCADE amplio y preservando el índice WhoScored. La versión concreta debe revalidarse en implementación. Esta eliminación expresa el alcance autorizado, no una migración de compatibilidad o conservación de datos de desarrollo.

### Cobertura prevista y pruebas

| Alcance | Responsabilidad y verificación |
| --- | --- |
| FR-001/012/016/021, SC-004/007 | Service real con Integration mock: secuencia automática, actual FAILED, histórico FAILED/PARTIAL, vacío y cero elegibles. Controller: único endpoint, DTO exacto y todos los resultados contractuales. |
| FR-003–010/013–014/025/027–029, SC-001/003 | Preservar pruebas de normalización/alias/matching; adaptar fixtures de snapshots/candidatos transitorios y confirmación de referencias. |
| FR-011, SC-005 | Retirada de entidades/repositorios; pruebas Repository de esquema y constraints con Flyway real; preservar tests de revisión de catálogo. |
| FR-019–020, SC-002 | Agregación por jugador local, referencia preexistente, resolución en histórico, NO_MATCH final, ambigüedad/conflictos/errores, universo vacío, sin doble conteo. |
| FR-002/015/017–018, SC-006 | Mantener pruebas de Integration/pacer; Service comprueba ausencia de transacción durante red y conservación de commits ante fallos. |
| FR-022–024/026, SC-008 | Controller/seguridad, E2E de una llamada y relanzamiento; sin fugas, estadísticas, cotizaciones ni cambios de otros proveedores. |

Tests afectados: `backend/src/test/java/footballmarket/features/identity/` (domain, application, infraestructure, presentation, e2e), helpers de `support` que limpian auditoría y consumidores de estadísticas. Sustituir cobertura de auditoría retirada por la del nuevo contrato, preservando regresiones de unicidad, concurrencia y atomicidad relevantes. No tests dedicados de Mapper, no APIs productivas exclusivas de tests.

### Constitution Check posterior al diseño

No se propone excepción ni nueva dependencia. Se preservan invariantes, seguridad y aislamiento; solo desaparece persistencia sin función en el nuevo objetivo. Modelo, HTTP, proveedor y quickstart siguen siendo aplicables y se actualizan por impacto. Checklists seleccionados: general existente, identidad existente por invariantes preservadas y complemento de API/datos/estados del cambio; no checklist frontend/UX ni roles nuevos.

**Evidencia posterior**: [checklist del cambio](checklists/sync-simplification.md) marcado por el usuario y releído; Tasks T058–T066 y ANALYZE sin blockers realizados antes de implementar. La revisión de requisitos no acredita CODE REVIEW ni TESTING del resultado implementado.

## Antecedente histórico de Plan — 2026-10-14 (no vigente para este cambio)

**Rama actual**: `feat/008-whoscored-player-identity`, creada y seleccionada el 2026-10-14 desde `origin/dev` (`8b3b292`) | **Fecha**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Identidad Git resuelta**: la rama asociada prevista ya existe y está seleccionada; la anterior `feat/008-whoscored-player-statistics` pertenecía al alcance estadístico abandonado y queda obsoleta. La selección se hizo sin stash ni descarte de los cambios locales preexistentes.

**Estado de Plan**: OK documental; Phase 0 y Phase 1 completas. Evidencia del bloqueo inicial y su resolución en [plan-evidence.md](plan-evidence.md). Rama de la spec creada y seleccionada. Analyze vigente sin blockers el 2026-10-14; READY OK, IMPLEMENT pendiente de autorización humana por US.

**Actualización documental 2026-10-14**: decisiones humanas de checklist incorporadas a Spec y diseño; identity 40/40; luego correcciones de redacción de FR-012/FR-016/FR-019/FR-020/FR-021/FR-026 y del contrato HTTP tras un Analyze sin CRITICAL. Plan no se repite y no acredita Analyze vigente ni READY. Falta persistir esta actualización en Trello cuando el usuario retome su sincronización.

## Resumen

Identificar jugadores persistidos mediante un pase actual y un histórico sobre pendientes del primero, con referencias protegidas y auditoría durable cuyo resumen se devuelve en la propia respuesta de cada operación. No incluye endpoints de consulta de ejecuciones. Descubrir todas las ligas válidas antes de resolver globalmente contra los nombres y equipos locales capturados al inicio. No incluye estadísticas, cotizaciones ni scheduler. No acredita el cumplimiento completo de REQ-S2-02 ni la feature adicional de [Sprint 2](../../tp/entregas.md).

## Contexto técnico

- **Lenguaje y versiones**: Java 21, Spring Boot 4.1.1, Gradle Wrapper 9.6.0; fuentes efectivas `backend/build.gradle.kts`, `backend/gradle/libs.versions.toml` y Wrapper.
- **Dependencias principales**: Spring Web MVC/RestClient, Data JPA, Security/OAuth2 Resource Server, Validation, Flyway y pgJDBC existentes. Cliente JDK; no se propone incorporar dependencias.
- **Almacenamiento**: PostgreSQL; reutilizar `PlayerExternalReference`, agregar `WHOSCORED` y auditoría específica de ejecuciones/ligas/decisiones.
- **Testing previsto**: Model unitario, Service con persistencia real e Integration mock, Integration con servidor controlado, Controller/REST Docs y journeys E2E críticos, según [testing](../../docs/backend/testing.md). Ejecución a cargo del usuario.
- **Plataforma/tipo**: backend JVM, API REST existente; sin cambios frontend.
- **Escala**: aproximadamente 2600 jugadores locales, 2165 IDs actuales de referencia e histórico superior a 5900. Cobertura aproximada 83% orientativa, no umbral obligatorio.
- **Objetivos y restricciones**: normalización exacta más alias cerrados, comparación global, sin transacciones durante red, aislamiento por liga y por confirmación, sin reintentos por defecto. Intervalo inicial 2500 ms, conexión 5 s, lectura 20 s, páginas de 100, límites de 10000 páginas por liga y 5 MiB por respuesta, todos configurables. Son defaults de diseño, no límites oficiales ni SLA de duración.
- **Contrato externo y fin de paginación**: resuelto mediante [evidencia](evidence/whoscored-catalog-contract.md) y [contrato](contracts/whoscored.md). `playerTableStats` y `paging`; terminar por `currentPage == totalPages`. JSON válido puede llegar como `text/html`; parsing explícito del cuerpo y validación estructural. `User-Agent` configurable mediante RestClient, sin nueva dependencia.

## Constitution Check

### Antes de Phase 0

Revisados Constitution 6.1.0, AGENTS raíz/backend, workflow, normas backend y fuentes académicas. Alcance identificable y Clarify vigente persistido; investigación autorizada. Sin violaciones normativas justificadas ni excepción propuesta.

| Control aplicable | Evaluación inicial |
| --- | --- |
| Simplicidad y stack existente (§2) | RestClient existente; separación por responsabilidades reales. |
| Datos de desarrollo (§2.1) | Sin capas preservadoras ni migraciones de compatibilidad propuestas. |
| Seguridad (§3) | Acceso JWT vigente; no existe rol administrativo específico para sync. Sanitizar errores y no guardar cuerpos crudos. |
| Contratos (§4) | Interfaces propias en contracts/http.md y contracts/whoscored.md; Integration separada de DTO HTTP y Model. No cambiar flujos de otros proveedores. |
| Idioma (§5) | Documentación española, futuros identificadores y campos ingleses. |
| Verificación (§6) | Solo revisión documental en Plan; no tests ni build. |
| Git/trazabilidad (§7–8) | Rama `feat/008-whoscored-player-identity` creada y seleccionada; conservar trabajo local. Plan no acredita READY ni autorización. |
| Arquitectura | Service con interfaz/implementación; sin Service→Service, Controller→Repository ni lógica de dominio en Mapper. |

### Gate de investigación y posterior al diseño

**Phase 0: OK**. Evidencia incorporada resuelve el contrato JSON, headers y finalización. No quedan decisiones técnicas sin resolver para Plan.

**Constitution Check posterior a Phase 1: OK documental**, sin violaciones propuestas:

- [Modelo](data-model.md) protege referencias por constraints en ambos sentidos para WhoScored, snapshot inicial e aislamiento por jugador; sin alterar cardinalidad de otros proveedores.
- [HTTP](contracts/http.md) define los dos endpoints de ejecución, DTO de resumen, errores controlados y seguridad vigente; no expone Model, no añade consultas de auditoría ni corrección manual.
- [Integración](contracts/whoscored.md) valida cuerpo/metadatos y liga completa, pacing y límites explícitos, sin retries ni métricas persistidas.
- [Quickstart](quickstart.md) define validación posterior para el usuario y cobertura suficiente de comportamiento, sin ejecutar pruebas en Plan.
- Arquitectura y stack existentes bastan. Requisitos académicos restantes conservan su alcance en tp/ y no se declaran satisfechos.

Esta evaluación de diseño no acredita Checklists, Analyze, READY ni gates humanos.

## Diseño del caso de uso

1. Validar cinco bindings `leagueId` interno → competición WhoScored y origen del histórico cuando corresponda. Snapshot coherente en transacción corta REPEATABLE_READ de run, ámbitos e items locales; materializar nombres/equipo/liga y referencias iniciales. No depender de relaciones lazy fuera de ella.
2. Pase actual incluye jugadores persistidos de las cinco ligas. Histórico incluye exclusivamente pendientes NO_MATCH/AMBIGUOUS del actual terminado, todavía sin referencia; captura atributos locales al iniciar este nuevo pase. Si no quedan elegibles, registrar éxito vacío sin red.
3. Descubrir ligas secuencialmente fuera de transacción; solo publicar candidatos de cada liga tras validar todo el recorrido. Persistir ámbitos y candidatos de identidad, no cuerpos crudos ni métricas.
   Descartar solo filas de identidad inválida y auditar posición/motivo; paging y rowsRead usan filas originales. Descartes no invalidan la liga; fallos de página/paginación sí. Incorporar LOCAL_DATA_ERROR y sus contadores sin convertirlo en pendiente histórico ni fallo técnico. Clasificar referencias iniciales múltiples como CONFLICT antes de matching. Validar colisiones de la tabla fija antes del pase; no versionarla ni actualizarla.
4. Consolidar candidatos por ID en toda la ejecución conservando contextos. Model normaliza y evalúa nombre/equipo contra snapshot y alias normativos, con independencia del orden de ligas. Las referencias iniciales no se rematchean.
5. Confirmar por jugador en REQUIRES_NEW con lock breve de Player y constraints. Referencia y resultado RESOLVED se guardan atómicamente. Referencia idéntica concurrente → ALREADY_REFERENCED; distinta o ID apropiado → CONFLICT. No revalidar nombre/equipo ni actualizar atributos del catálogo.
6. Capturar fallos fuera de la transacción revertida y auditar en otra; continuar otros jugadores. Clasificar violaciones por SQLSTATE/constraint, sin interpretar mensajes sensibles. Si no puede persistirse auditoría, detener sin presentar éxito y conservar lo ya confirmado.
7. Finalizar derivando contadores de los items y candidatos persistidos y devolverlos, junto con el estado por liga, en la respuesta 200 de la operación. El actual no se modifica al terminar su histórico. No incrementar dos veces resultados ni purgar evidencia.

Una interrupción controlada marca ámbitos/items pendientes como interrumpidos sin nuevas referencias. Una terminación abrupta del proceso puede dejar RUNNING: significa finalización no registrada, no acredita actividad ni habilita histórico. Al no existir consulta de ejecuciones, relanzar el pase es el mecanismo de recuperación y es seguro por la idempotencia de las referencias (FR-014, FR-026). No se agrega scheduler ni recuperación que pueda declarar huérfana una ejecución de otra instancia.

### Responsabilidades y componentes previstos

| Rol / componente | Responsabilidad |
| --- | --- |
| `PlayerIdentityController`, DTO records y `PlayerIdentityMapper` | Frontera HTTP, validación estructural y transformación DTO↔Model. |
| `PlayerIdentitySynchronizationService` / `impl` | Captura, selección, descubrimiento, persistencia aislada y cierre. Sin dependencia de otros Services. |
| `PlayerIdentityAuditService` / `impl` | Agregación interna de la auditoría: contadores por resultado y estado por liga que alimentan el DTO de resumen y la elegibilidad del histórico. No consume Integration ni expone consultas HTTP. |
| `WhoScoredIntegration` | HTTP, parsing JSON independiente del MIME, validación de páginas y datos adaptados; propiedad del contrato externo. |
| `WhoScoredClientConfig`, `WhoScoredProperties`, pacer | Cliente dedicado, User-Agent, límites y ritmo fuera de transacción. |
| Model de identidad/ejecución | Normalización, alias, matching, invariantes y estados del modelo documentado. |
| Repositories | Snapshot, locking, auditoría, filtros y agregación de resultados; sin decisiones de identidad. |

No hace falta Orchestrator: las dos fachadas son casos de uso independientes y el Controller no coordina lógica de aplicación. Helpers internos, si son necesarios, componen el único caso de uso y no exponen servicios reutilizables.

### Cobertura de diseño

| Requisitos | Artefacto / responsabilidad |
| --- | --- |
| FR-001–003, FR-015, FR-017 | Contrato WhoScored: solicitud, cinco bindings, paginación, validación, deduplicación y ritmo. |
| FR-004–009, FR-013–014, FR-025 | Modelo: snapshot, normalización/alias, matching global y protección de referencias. |
| FR-010–012, FR-016, FR-018 | Modelo y caso de uso: elegibilidad, fallos por ámbito/jugador y transacciones. |
| FR-019–021, FR-026 | Modelo y HTTP: auditoría durable y reconciliación; DTO de resumen devuelto por cada operación de ejecución, sin superficie de consulta. |
| FR-022–024 | HTTP y límites del diseño: JWT vigente, DTO/errores/OpenAPI y solo identidad. |
| FR-027–029 | Modelo, integración y HTTP: validación local, descartes individuales auditados, variantes por contexto y conflicto de propietarios. |
| SC-001–008 | Quickstart: resultados esperados y escenarios de validación. |

## Estructura del proyecto

### Documentación de la feature

```text
specs/008-whoscored-player-identity/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   ├── http.md
│   └── whoscored.md
├── quickstart.md
├── plan-evidence.md
├── specify-evidence.md
├── clarify-evidence.md
├── evidence/               # Contrato observado y cuatro JSON existentes
└── checklists/              # requirements.md, identity.md y evaluation.md
```

Modelo, contratos y guía son aplicables y están generados. `tasks.md` pertenece a una operación posterior autorizada.

### Código previsto, sin modificar en esta fase

```text
backend/src/main/java/footballmarket/
├── controllers/            # HTTP, DTO y Mapper
├── services/               # Fachada de identificación y agregación de auditoría
│   └── impl/               # Casos de uso y transacciones cortas
├── integrations/           # Adaptador WhoScored y contratos externos
├── models/                 # Referencias, identidad y auditoría
├── repositories/           # Persistencia y consultas
└── config/                 # Cliente y propiedades
backend/src/main/resources/db/migration/
backend/src/test/java/footballmarket/
backend/src/docs/asciidoc/
```

Mantener la estructura por roles existente; incluir el diseño de esquema en la siguiente migración disponible al implementar, actualmente V6, sin fuentes SQL en Plan ni capas de compatibilidad para datos de desarrollo. Decisiones en [research.md](research.md).

## Complexity Tracking

Ninguna violación propuesta ni excepción necesaria. La evidencia resolvió el bloqueo inicial.

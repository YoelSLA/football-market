# Plan de implementación: Identidad externa de jugadores en WhoScored

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

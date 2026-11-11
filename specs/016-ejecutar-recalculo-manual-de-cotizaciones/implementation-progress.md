# IMPLEMENT — delta T055–T080

## Reanudación vigente

El usuario indicó después de la primera detención: «Okey, pero ese problema es para
otro bloque el cual nos vamos a preocupar». Se continúa con la autorización original,
sin tratar la dependencia de SPEC-018 como impedimento para el trabajo independiente.
No se ha aprobado ni implementado un selector distinto para su estrategia activa.
Su riesgo queda registrado en TD-039 y pendiente del bloque responsable.

Se implementaron el núcleo diario, contexto y cierre auditados, lecturas individuales,
pruebas backend y ranking SQL paginado con su endpoint `/api/rankings/quote`.
Se reutiliza `PlayerRepository.lockIdentityById` en inicialización y publicación,
sin añadir otro mecanismo de lock ni un bloqueo global.

T077 conserva un pendiente acotado: retirar `/api/players/ranking` y su respuesta
histórica solo conjuntamente con la adaptación de `frontend/src/features/players/players.service.ts`
bajo SPEC-022. La ruta existente no se convierte en alias ni se crea otro endpoint
de descarga completa; la ruta nueva solo pagina en SQL. Se mantiene separada su
respuesta `QuoteRankingPageResponseDTO` mientras el DTO histórico conserva el contrato
de ese consumidor. No se implementa UI ni se cambia ninguna de esas otras specs.

Esta reanudación supera la detención inicial descrita abajo. El estado de tareas
vigente reside en `tasks.md`; no se deduce cierre de Feature ni revisión/testing
aprobados de este avance. La migración no se aplicó a bases existentes y puede
rechazar datos incompatibles, sin limpieza o backfill automáticos.

Se ejecuta únicamente el build permitido, sin tests. La salida final y las limitaciones
se registran en `quickstart.md`. Los informes de implementación y pruebas están en
[daily-implementation-review.md](daily-implementation-review.md). Ningún gate humano
se considera aprobado y no se ejecutan fases posteriores, commit ni push.

**Resultado observado de la reanudación:** 25 tareas del delta marcadas por trabajo
implementado y revisión estática; T077 queda parcial. Build final sin tests satisfactorio
(12 tareas accionables, 4 ejecutadas y 8 actualizadas). Se revisaron consumidores de
constructores, selectores y excepción retirada; la búsqueda de referencias a
`ExecutionAlreadyExistsException` y selectores retirados no dejó consumidores de código.
`git diff --check` no informó errores y el estado Git conserva los cambios documentales
y archivos sin seguimiento preexistentes. No se aprobó revisión, testing ni cierre;
seguimiento remoto sin verificar ni modificar.

## Primer intento — evidencia histórica

### Autorización humana recibida

El usuario de esta sesión solicitó ejecutar exclusivamente IMPLEMENT y declaró:
«autorizo a implementar las T055–T080». Después de regularizar los marcadores de
las checklists, indicó: «Listo, ya podes continuar, estan tachados aunque los ignores».
La autorización corresponde al delta local descrito en `analysis-report.md`,
`spec.md`, `plan.md` y `tasks.md`, sobre la base `48095fdc93b0981c1c98276e7c4cce749e2e93f2`.
No autoriza fases posteriores, cambios de alcance ni implementación de SPEC-018/022.
Las menciones históricas a autorización NONE se conservan como evidencia anterior.

### Comprobaciones y trabajo observado

- Prerrequisitos reales: `check-prerequisites.sh --json --require-tasks --include-tasks`
  resolvió el directorio de SPEC-016 y sus artefactos.
- Checklists: requirements 16/16, cotizacion 31/31, api 10/10 y data 10/10;
  cero marcadores pendientes. Los criterios tachados conservan su condición de
  retirados, no de requisitos aprobados.
- Sin `.specify/extensions.yml`: no hay hooks de extensión que despachar.
- Se inspeccionó `createMigration`: genera un archivo local, sin dependencias
  de tests, conexiones ni aplicación de Flyway.
- Ejecutado desde `backend/`: `./gradlew.bat createMigration -PmigrationDescription=daily_quote_recalculation`.
  Resultado observado: BUILD SUCCESSFUL, una tarea ejecutada.
- Archivo generado y completado:
  `backend/src/main/resources/db/migration/V20261110235437__daily_quote_recalculation.sql`.
  No se aplicó a ninguna base ni se modificaron migraciones integradas.

### Detención por decisión funcional pendiente

Antes de cambiar cardinalidad y selectores de ejecución se comprobó el consumidor
`ScoreRankingQueryServiceImpl.findScoreRanking`, que invoca
`QuoteExecutionRepository.findTopByOrderByQuoteDateDesc`.
Con múltiples corridas de estrategias distintas en una fecha, ese selector puede
elegir cualquiera; incluir contextos sin cierre también altera la estrategia visible.

`plan.md` §Coordinación contractual y orden técnico reserva expresamente esa
semántica a SPEC-018. La revisión vigente de `specs/018-consultar-rankings-de-jugadores/spec.md`,
FR-002, todavía exige CLARIFY para fijar el desempate. No se interpreta la
dependencia entre features como blocker por sí sola: lo pendiente es la decisión
observable que T057 exige no alterar silenciosamente. La propuesta técnica de
SPEC-016 es seleccionar solo finalizadas con orden
`quoteDate DESC, startedAt DESC, id DESC`; no se la impone al consumidor.

T057 y la integración de múltiples corridas requieren resolver esa decisión en
su fuente de requisitos y validar por impacto los artefactos afectados conforme
al workflow. Se detiene esta ejecución sin ejecutar otras fases. La autorización
T055–T080 se conserva para el alcance no afectado, sin ampliarla a otras specs.

El frontend todavía consume `/players/ranking` en `players.service.ts`; T077/T078
mantienen además su dependencia de adopción conjunta con SPEC-022. No se retira
la ruta ni se modifica frontend en esta ejecución.

### Estado de entrega parcial del primer intento

Solo T055 completada a nivel de escritura y revisión estática. T056–T080 pendientes.
La migración es un cambio parcial: no desplegarla como implementación terminada
del delta, pues Model y consumidores todavía no están adaptados. La aplicación de
constraints sobre datos existentes queda sin verificar; no se autoriza ni ejecuta
limpieza, backfill o borrado.

CODE REVIEW, TESTING y cierre del delta pendientes. Se inspeccionaron las dependencias
de build y se ejecutó `./gradlew.bat build -x test` desde `backend/`: BUILD SUCCESSFUL,
12 tareas accionables (11 ejecutadas y una actualizada), sin tareas Test en la salida.
La validación de layout informó 29 migraciones válidas. Esto no aplica Flyway a una
base ni verifica la compatibilidad funcional del delta parcial. No se ejecutaron
tests, commit, push ni fases posteriores. TD-024 continúa pendiente.
Seguimiento remoto PENDING: no se consultó ni modificó su estado y no se afirma
sincronización ni transición remota.
# Alineación documental para integración con SPEC-023 — 2026-11-11

- **Autorización**: usuario de la sesión activa: «alinees ambas spec para luego procegir al analyze, tenes mi autorizacion para modificar la spec016». No se interpreta como autorización de ejecutar ANALYZE en esta misma operación ni de completar/publicar el merge.
- **Decisión aplicada**: último cálculo válido en una fila jugador-fecha y referencia de ejecución actualizada; núcleo diario/ranking de SPEC-016 más cron 12:00 UTC y cuota automática de SPEC-023. Fórmula sobre precio vigente de cada cálculo; no guardar precios intermedios ni limitar a un ajuste diario.
- **Artefactos alineados**: spec, plan, research, data-model, contratos, quickstart y tareas de ambas specs. Diseño y cobertura compartidos en `../023-scheduler-quote-recalculation/integration-baseline.md`. T081–T086 y T043–T046 son remediaciones pendientes; IDs y avance anteriores preservados.
- **Estado**: preparación documental para nuevo ANALYZE de ambas specs sobre una sola base, sin cambios entre ejecuciones. Los análisis anteriores siguen como historia, no acreditan la integración. No se aprueban checklists ni gates humanos.
- **Límites observados**: no se modificaron código, tests, SQL ni marcadores de checklist; sin tests/build, commits, push ni conclusión del merge. Conflictos productivos y de pruebas permanecen; Flyway humano sigue pendiente de diagnóstico.

---

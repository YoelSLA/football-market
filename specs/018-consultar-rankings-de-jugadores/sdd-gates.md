# Evidencia local de gates SDD

El sistema de seguimiento operativo permanece sin configurar. Este archivo conserva únicamente la
evidencia local exigida por la contingencia de `docs/development/sdd-workflow.md` §12; no afirma
sincronización remota ni sustituye los artefactos SDD.

## 2026-10-23 — ANALYZE

- Alcance evaluado: SPEC-018 completa (`spec.md`, `plan.md`, `tasks.md`, artefactos de diseño y
  checklists).
- Resultado: preparación completa y vigente, sin blockers CRITICAL, HIGH ni MEDIUM.
- Hallazgo no bloqueante: `tasks.md` conserva una nota histórica sobre criterios pendientes de
  reevaluación, aunque los checklists actuales tienen 31/31 criterios marcados por el revisor.
- Cobertura: los requisitos funcionales y criterios verificables están cubiertos por las Tasks; las
  dependencias de US01 están identificadas como T001–T007 y se resuelven por orden técnico.

## 2026-10-23 — IMPLEMENT AUTHORIZATION

- Autor: usuario de la sesión activa.
- Evidencia textual: `autorizo a implementar US01`.
- Alcance autorizado: US01 de SPEC-018 y sus prerrequisitos compartidos necesarios T001–T007; Tasks
  ejecutables T001–T010 y T012–T016.
- Fuera de alcance: US02, US03, pulido global, CODE REVIEW, TESTING, CONVERGE y cierre de Feature.
- Vigencia: vigente al inicio de IMPLEMENT; no existen cambios posteriores en requisitos, plan o
  Tasks que afecten el alcance evaluado.
- Seguimiento remoto: PENDING; no configurado según el workflow vigente.

## 2026-10-23 — INVALIDACIÓN DE ANALYZE E IMPLEMENT

- Motivo: al verificar T001 contra la migración efectiva
  `backend/src/main/resources/db/migration/V20261019120000__create_player_valuations.sql`, se observó
  que ambas tablas de valuaciones imponen unicidad por jugador, fecha, estrategia y versión.
- Contradicción: FR-004/FR-005, `data-model.md` §2.3 y T006–T008 requieren seleccionar entre varias
  valuaciones del mismo jugador y estrategia en una misma fecha mediante `calculatedAt` e `id`, pero
  el esquema vigente impide persistir ese estado.
- Impacto: ANALYZE deja de estar vigente y la IMPLEMENT AUTHORIZATION queda suspendida para US01 y
  sus prerrequisitos hasta corregir los artefactos afectados y completar un nuevo ANALYZE sin
  blockers.
- Trabajo funcional realizado: ninguno; IMPLEMENT se detuvo durante T001 antes de modificar product
  code o tests.
- Decisión requerida: autorizar una corrección upstream que preserve el caso y añada el cambio de
  esquema y sus Tasks, o retirar de la spec el desempate imposible y actualizar sus derivados.

## 2026-10-23 — CORRECCIÓN UPSTREAM AUTORIZADA

- Autor: usuario de la sesión activa.
- Evidencia textual: `FR-005 se borrar y 004 se modifica. Hace los cambios en toda la spec porfavor`.
- Cambios: FR-005 fue retirado sin reutilizar su identificador; FR-004 selecciona la valuación de
  mayor fecha conforme a la unicidad vigente. Se actualizaron spec, plan, research, data model,
  contrato, quickstart y Tasks sin cambiar el esquema ni crear migraciones.

## 2026-10-23 — NUEVO ANALYZE

- Alcance evaluado: SPEC-018 completa después de la corrección upstream.
- Resultado: preparación completa y coherente, sin blockers CRITICAL, HIGH ni MEDIUM.
- Cobertura: FR-004 y sus escenarios están cubiertos por T006–T009; no quedan Tasks ni referencias
  ejecutables asociadas al FR-005 retirado.
- IMPLEMENT AUTHORIZATION anterior: INVALIDATED por el cambio de requisitos evaluados. La
  implementación permanece detenida hasta una nueva autorización humana explícita para el alcance
  corregido.

## 2026-10-23 — IMPLEMENT AUTHORIZATION RENOVADA

- Autor: usuario de la sesión activa.
- Evidencia textual: `autorizo a implementar US01.`
- Alcance autorizado: US01 corregida de SPEC-018 y sus prerrequisitos compartidos necesarios
  T001–T007; Tasks ejecutables T001–T010 y T012–T016.
- Base evaluada: FR-004 selecciona por mayor `valuationDate`; FR-005 está retirado.
- Fuera de alcance: US02, US03, pulido global, CODE REVIEW, TESTING, CONVERGE y cierre de Feature.
- Seguimiento remoto: PENDING; no configurado según el workflow vigente.

## 2026-10-23 — IMPLEMENT AUTHORIZATION AMPLIADA

- Autor: usuario de la sesión activa.
- Evidencia textual: `autorizo a implementar US02, US03 y Polish.`
- Alcance autorizado: US02, US03 y Polish de SPEC-018; Tasks ejecutables T017–T024 y T026–T028.
- Prerrequisito verificado: US01 y sus prerrequisitos T001–T010 y T012–T016 están implementados y
  marcados completos; el ANALYZE renovado permanece vigente y sin blockers.
- Fuera de alcance: CODE REVIEW, TESTING, CONVERGE y cierre de Feature.
- Inicio: el trabajo comienza con esta ejecución; el seguimiento remoto permanece PENDING porque no
  está configurado conforme al workflow vigente.

## 2026-10-23 — AJUSTE DE REMEDIACIONES DE CONVERGE

- Autor: usuario de la sesión activa.
- Evidencia textual: `T031... esta spec esta enfocada en valuacion, por ende borrala porfavor`.
- Cambio: se retira la remediación T031 del bloque `Phase 7: Convergence` de `tasks.md` por
  decisión de alcance del usuario. No se reutiliza su identificador ni se renumera T029/T030.
- Cobertura preservada: US3/AC3 sigue verificada por el escenario de encabezado no nulo con
  ranking vacío; deja de distinguirse el caso «arrastrados» de «sin población elegible».
- Fuera de alcance: código, tests, `spec.md`, `plan.md` y las remediaciones T029/T030.

## 2026-10-23 — AJUSTE Y AUTORIZACIÓN SOBRE REMEDIACIONES DE CONVERGE

- Autor: usuario de la sesión activa.
- Evidencia textual: `entonces tambien se elimina T030 y aplicar el test para la cobertura y cumplimiento de T029`.
- Cambio: se retira T030 de `Phase 7: Convergence` por decisión de alcance del usuario. El riesgo
  que cubría —traducción de `DataAccessException` a `500 INTERNAL_ERROR` sin ranking parcial— ya
  está verificado por el escenario existente, y la forma completa de `ErrorResponseDTO` es
  responsabilidad del `GlobalExceptionHandler` compartido, cubierto por los tests de otros
  Controllers. No se reutiliza el identificador T030.
- IMPLEMENT AUTHORIZATION ampliada a T029 exclusivamente: se añadió el escenario
  `selectsLatestExecutionByQuoteDate` en `ScoreRankingQueryTest`, que persiste tres ejecuciones en
  fechas distintas y verifica que `findTopByOrderByQuoteDateDesc()` devuelve la mayor `quoteDate`.
- Fuera de alcance: `spec.md`, `plan.md`, código productivo, CODE REVIEW, TESTING, CONVERGE y
  cierre de Feature.
- Verificación: `backend/gradlew.bat build -x test` como único control permitido; los tests quedan a
  cargo del usuario.

## 2026-10-23 — CODE REVIEW (informe completo entregado)

- Alcance revisado: T001–T029 de `tasks.md` (US1, US2, US3, Polish y remediación T029) sobre el árbol
  de trabajo de `feat/018-consultar-rankings-de-jugadores`. La rama no tiene commits propios de
  SPEC-018: el alcance está en 12 archivos Java nuevos sin seguimiento, 3 repositorios modificados y 1
  fila de `specs/assignments.md`, además de los artefactos de la feature.
- Informe completo: `specs/018-consultar-rankings-de-jugadores/code-review-report.md`. Incluye resumen
  y funcionamiento, tabla de archivos creados y modificados con los tests, decisiones de
  implementación, hallazgos con severidad y evidencia, cobertura de requisitos, revisión de la suite,
  orden de revisión y checklist humano de verificación.
- Control ejecutado: `backend/gradlew.bat build -x test`: BUILD SUCCESSFUL, con compilación productiva,
  Spotless y `validateTestClassification` («Clasificación válida: 124 clases de test»). El control no
  ejecuta ni compila tests: la compilación y la ejecución quedan a cargo del usuario.
- Hallazgos: ninguno de severidad Alta. CR-01 (Media): cobertura de tests; la implementación sí devuelve
  entradas de `GENERAL_ATTRIBUTES`, pero ningún test de Service ejercita esa rama con valuaciones ni
  afirma el score persistido. CR-02: descartado por decisión del responsable, porque el catálogo de
  estrategias es cerrado y no se ampliará. CR-03 a CR-07 (Baja): falta de copia defensiva en
  `ScoreRanking`, construcción duplicada de entradas, escala del score no discriminada en el test de
  contrato, riesgo heredado de TD-025 y alcance sin commits.
- Deuda técnica registrada: TD-030 (cobertura de tests incompleta para el ranking por score), en estado
  `Pendiente`. No se modifica TD-025.
- Naturaleza: operación exclusivamente documental de revisión. No se modificó código, tests,
  migraciones ni contratos, y no se marcó ni desmarcó ninguna tarea de `tasks.md`.

## 2026-10-23 — REMEDIACIÓN DE CR-01 (test de cobertura)

- Autor: usuario de la sesión activa.
- Evidencia textual: `okey, entonces como es un test, aplica el mismo donde testee este caso.`
- Alcance autorizado: añadir cobertura de test para CR-01 y ajustar el fixture de test de SPEC-018.
  Sin cambios en código productivo, contrato, migraciones ni `tasks.md`.
- Cambios:
  - `ScoreRankingQueryServiceTest`: nuevo `@Nested AttributeRanking` con
    `exposesAttributeFinalScore`, que ejercita la rama `GENERAL_ATTRIBUTES` con dos jugadores y score
    bruto distinto del final; afirma estrategia, `quoteDate`, orden, posiciones, `score` y
    `valuationDate`.
  - `ScoreRankingQueryServiceTest.ordersEntriesAndAssignsPositions`: afirmaciones de score añadidas.
  - `ScoreRankingTestData`: sobrecarga de `prepareAttributePlayer` con bruto y final separados.
- Decisión asociada: CR-02 sigue descartado por catálogo de estrategias cerrado, conforme a la
  evidencia registrada en el informe.
- Control ejecutado: `backend/gradlew.bat build -x test` → BUILD SUCCESSFUL (Spotless y
  `validateTestClassification` con 124 clases). No compila ni ejecuta tests.
- Estado del hallazgo: CR-01 **corregido en código de test, pendiente de ejecución**; TD-030 pasa a
  `En curso` y no se cierra hasta que el usuario ejecute la suite con resultado satisfactorio.
- CODE REVIEW: **no aprobado**; la aprobación humana del checklist sigue pendiente.

### Estado de autorizaciones

- **IMPLEMENT**: sin cambios; el alcance implementado es el autorizado en las entradas anteriores.
- **CODE REVIEW**: informe entregado y persistido. **Pendiente de aprobación humana del checklist de la
  sección 10 del informe**; el agente no aprueba el gate.
- **TESTING**: pendiente; los tests deben ejecutarlos el usuario. El build no acredita tests.
- **CONVERGE y FEATURE COMPLETION**: pendientes y fuera de esta operación.

### Siguiente acción permitida

- Aprobación humana de CODE REVIEW con el checklist de la sección 10 del informe y, tras ella, la fase
  de TESTING con ejecución de tests a cargo del usuario.

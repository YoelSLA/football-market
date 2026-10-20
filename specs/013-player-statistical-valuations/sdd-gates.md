# Registro local de gates SDD — SPEC-013

Este archivo aplica la contingencia local de `docs/development/sdd-workflow.md` §12 mientras GitHub Projects no está configurado. Registra evidencia histórica sin acreditar por sí mismo revisión, testing ni cierre.

## ANALYZE — 2026-10-19

- **Feature**: `013-player-statistical-valuations`
- **Artefactos evaluados**: `spec.md`, `plan.md`, `tasks.md`, `research.md`, `data-model.md`, `contracts/http.md`, `quickstart.md` y checklists aplicables.
- **Resultado**: preparación completa; ANALYZE vigente sin blockers.
- **Hallazgos**: ninguno. Cero issues CRITICAL, HIGH, MEDIUM o LOW.
- **Cobertura**: `FR-001`–`FR-066` y `SC-001`–`SC-011` cubiertos por `T001`–`T036`; sin Tasks ajenas al alcance.
- **Constitution alignment**: sin conflictos detectados.
- **Tests y builds**: no ejecutados.

## IMPLEMENT AUTHORIZATION — 2026-10-19

- **Autorizante**: usuario de la sesión de OpenCode.
- **Evidencia textual**: «autorizo a implementar las US 01, 02 y 03 junto con las fases correspodientes.»
- **Alcance autorizado**: US1, US2 y US3 de SPEC-013, incluidos Setup, Foundational y Polish, junto con los prerrequisitos compartidos y las Tasks `T001`–`T036` que materializan exclusivamente ese alcance.
- **Límites**: no autoriza ampliar requisitos, ejecutar tests, aprobar CODE REVIEW, aprobar TESTING, ejecutar CONVERGE final ni cerrar la Feature.
- **Vigencia**: vigente respecto de los artefactos evaluados por ANALYZE el 2026-10-19.
- **Inicio de IMPLEMENT**: 2026-10-19; comenzó el trabajo autorizado sobre Setup y prerrequisitos compartidos.

## Estado de autorizaciones

- **IMPLEMENT AUTHORIZATION**: vigente para US1, US2, US3 y sus fases/prerrequisitos compartidos.
- **CODE REVIEW**: pendiente.
- **TESTING**: pendiente.
- **FEATURE COMPLETION AUTHORIZATION**: pendiente.

## IMPLEMENT — 2026-10-19

- **Alcance implementado**: Setup, Foundational, US1, US2, US3 y Polish de SPEC-013 (`T001`–`T036`).
- **Resultado técnico**: dos estrategias v1, persistencia reconstruible, selección de campaña confirmada, evaluación poblacional, publicación coherente por jugador, resumen HTTP y seguridad Bearer.
- **Build permitido**: `backend/gradlew.bat build -x test` completado satisfactoriamente.
- **Tests**: creados; no ejecutados por el agente. Ejecución y validación pendientes del usuario.
- **CODE REVIEW**: pendiente de autorización humana sobre el reporte completo.
- **TESTING**: pendiente de ejecución y aprobación humana.
- **Seguimiento operativo**: no sincronizado; GitHub Projects continúa sin configurar.

## IMPLEMENT (enmienda) — 2026-10-20

- **Autorizante**: usuario de la sesión de OpenCode, que eligió relajar el criterio de confirmación de campaña y aplicarlo en la spec.
- **Alcance implementado**: `T037`–`T043`. Enmienda de FR-004, FR-058 y FR-009, criterio de confirmación por campaña terminada, exclusión de los elegibles sin snapshot, traducción de posiciones del catálogo, materialización del borrado previo a la republicación y eliminación del código muerto de la feature.
- **Defectos encontrados y corregidos**: inyección ambigua de `Clock`, 409 permanente por cobertura total exigida, posiciones persistidas no contempladas (`Defence`, `Midfield`, `Offence`) y violación de unicidad al republicar en la misma fecha.
- **Resultado técnico**: la evaluación poblacional corre sobre la campaña JSON vigente y publica un par de valoraciones por jugador evaluado.
- **Build permitido**: `backend/gradlew.bat build -x test` completado satisfactoriamente.
- **Verificación manual**: `POST /api/valuations/evaluate` con JWT en entorno de desarrollo, dos ejecuciones consecutivas, 1860 evaluados, 0 fallidos, 232 `UNRESOLVED_LEAGUE_CONTEXT`, 12 `MISSING_OR_UNCONFIRMED_SNAPSHOT`, sin duplicados en base de datos.
- **Tests**: no ejecutados por el agente. Los tests existentes de SPEC-013 pueden requerir actualización respecto del criterio de confirmación y de las posiciones; ejecución y validación pendientes del usuario.
- **CODE REVIEW**: pendiente de autorización humana sobre el reporte completo.
- **TESTING**: pendiente de ejecución y aprobación humana.
- **Seguimiento operativo**: no sincronizado; GitHub Projects continúa sin configurar.

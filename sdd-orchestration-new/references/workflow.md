# Workflow — fases y estados

## Principios

La **fase SDD** representa la operación actual; el **estado Trello** representa el progreso observable. No confundirlos. Usar `trello-traceability` para sincronizar tarjetas; no duplicar su implementación. Las seis columnas son: `BACKLOG`, `SPECIFYING`, `READY`, `IN PROGRESS`, `CODE REVIEW`, `DONE`. No existe columna `TESTING` ni `CONVERGE`.

## Preparación SDD

| Fase | Operación delegada | Condición para avanzar |
| --- | --- | --- |
| SPECIFY | `/speckit.specify` | `spec.md` identificable y alcance suficientemente explícito. |
| CLARIFY | `/speckit.clarify` | Ambigüedades funcionales relevantes resueltas y reflejadas en la spec. |
| PLAN | `/speckit.plan` | Plan y artefactos técnicos coherentes con spec y Constitución. |
| CHECKLIST | `/speckit.checklist` | Comprobaciones de calidad sin bloqueos pendientes. |
| TASKS | `/speckit.tasks` | `tasks.md` cubre US, criterios y dependencias; sin tareas fuera de alcance. |
| ANALYZE | `/speckit.analyze` | Sin blockers; hallazgos no bloqueantes comunicados. |
| READY | Gate de preparación | Consultar `quality-gates.md`; pedir autorización explícita de IMPLEMENT. |

Los nombres anteriores identifican operaciones de Spec Kit esperadas en este proyecto: comprobar su disponibilidad real antes de invocarlas. No simular operaciones ausentes. Durante preparación, el estado de Feature puede ser `SPECIFYING`; al cumplir READY, pasa a `READY`. Las US existentes permanecen en READY una vez preparadas, hasta que comience trabajo real.

## IMPLEMENT de Feature completa

Una autorización válida habilita el alcance completo de la Feature, no autoriza cambios materiales de alcance ni tests ejecutados por agentes. Respetar dependencias de `tasks.md`; se puede trabajar en paralelo si es coherente con ellas.

| Evento verificado | Feature | US afectada |
| --- | --- | --- |
| Preparación completa; sin trabajo de implementación | READY | READY |
| Comienza implementación real de la primera US | IN PROGRESS | IN PROGRESS |
| Termina implementación de una US | IN PROGRESS | CODE REVIEW (pendiente de revisión integral) |
| Comienza otra US | IN PROGRESS | IN PROGRESS |
| Todas las US terminan implementación | CODE REVIEW | Todas CODE REVIEW |
| Gate DONE satisfecho | DONE | Todas DONE |

Una US en `CODE REVIEW` antes del final de IMPLEMENT significa **implementada, pendiente de validación integral**; no significa aprobación individual. Tareas transversales no provocan por sí solas que todas las US pasen a `IN PROGRESS`. Actualizar estados según evidencia real, no según intención o mera autorización.

## CODE REVIEW y cierre

1. Con todas las US implementadas, mover Feature a `CODE REVIEW` y ejecutar automáticamente `/speckit.converge` (si existe), sin ejecutar tests ni operaciones indirectas que los lancen.
2. Si hay pendientes, seguir `recovery.md`: diagnóstico, impacto y propuesta; esperar autorización de corrección. Reabrir solo US afectadas; Feature vuelve a `IN PROGRESS` cuando corresponda realizar implementación adicional.
3. Si CONVERGE es satisfactorio, preparar reporte integral: código, archivos, criterios de aceptación, tests escritos y comandos sugeridos **para el usuario**.
4. Usuario revisa, ejecuta tests, confirma resultados y aprueba expresamente. Verificar `quality-gates.md` antes de mover Feature y US a `DONE`.

## Estado de sesión y sincronización

Antes de reanudar, identificar Feature, artefactos, código, autorizaciones y estados Trello. Evitar duplicar tarjetas, comentarios o transiciones; verificar si la operación ya se completó. Un estado Trello nunca reemplaza la evidencia de autorización. Ante discrepancia material, seguir `recovery.md`.

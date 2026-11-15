# Daily Scheduling & Quote Integrity Checklist: Programar recálculo automático de cotizaciones

**Purpose**: Evaluar la claridad, completitud, consistencia y verificabilidad de los requisitos modificados para la programación diaria, las ejecuciones múltiples y la cotización única por jugador-fecha.
**Created**: 2026-11-11
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado evalúa la calidad de los requisitos, no su implementación.
**Review Ownership**: Artefacto de revisión propiedad del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Claridad y completitud de la programación diaria

- [x] CHK001 ¿Se expresa de forma consistente que la programación predeterminada es diaria a las 12:00 UTC y que la cadencia sigue siendo configurable? [Consistency, Completeness, Spec §Clarifications, §FR-001]
- [x] CHK002 ¿Queda claro que la zona UTC determina tanto el momento del disparo como la fecha UTC usada para distinguir ejecuciones y cotizaciones? [Clarity, Consistency, Spec §FR-001, §FR-006, §FR-014]

## Reglas de ejecuciones múltiples

- [x] CHK003 ¿Está claramente diferenciada la cantidad permitida de ejecuciones manuales (ilimitada por fecha UTC) del máximo de una ejecución automática por fecha? [Clarity, Spec §FR-006, §FR-015]
- [x] CHK004 ¿Se especifica el resultado requerido para un disparo automático adicional en una fecha que ya tiene una ejecución automática, incluyendo la constancia operativa y la ausencia de una ejecución adicional? [Completeness, Measurability, Spec §US1, §FR-006, §SC-007]
- [x] CHK005 ¿Son consistentes las reglas de múltiples ejecuciones con la consulta por fecha, el registro independiente de cada corrida y el comportamiento manual heredado? [Consistency, Coverage, Spec §US2, §US3, §FR-004–§FR-006, §FR-015]

## Integridad y criterios observables de cotización diaria

- [x] CHK006 ¿Se define sin ambigüedad qué significa actualizar la cotización existente del jugador en la misma fecha UTC, incluyendo que el último cálculo prevalece y no se crea una fila duplicada? [Clarity, Completeness, Spec §FR-014, §SC-009]
- [x] CHK007 ¿Los escenarios y criterios de aceptación cubren una ejecución posterior del mismo día y permiten distinguir el resultado esperado para corridas manuales y automáticas? [Coverage, Measurability, Spec §US1, §US3, §SC-004, §SC-009, §SC-010]
- [x] CHK008 ¿Se mantiene explícito que estas reglas no cambian las fórmulas de valuación ni transfieren al scheduler responsabilidades de persistencia, compensación o recuperación? [Consistency, Scope, Spec §FR-003, §FR-013, §FR-014]

## Notes

- Este checklist se generó tras las clarificaciones del 2026-11-11 sobre cadencia diaria, corridas múltiples y cotización única por jugador-fecha.
- Los ocho ítems están marcados `[x]` como satisfechos en calidad de requisitos; esto no acredita implementación ni aceptación funcional.
- La generación de este checklist no equivale a su evaluación ni habilita por sí sola la fase TASKS.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.

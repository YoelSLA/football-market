# API Checklist: Auditoría de peticiones a servicios web

**Purpose**: Revisar la precisión y cobertura de los requisitos de auditoría en la frontera HTTP.
**Created**: 2026-11-26
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado revisa la calidad de los requisitos; no verifica la implementación.
**Review Ownership**: El checklist pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad del requisito está satisfecho.
**Marker Semantics**: `[x]` expresa satisfacción del criterio de calidad del requisito, no finalización de implementación.

## Calidad de requisitos de API

- [x] CHK001 ¿La ruta u operación que identifica cada invocación está definida sin ambigüedad respecto de los parámetros de consulta? [Clarity, Spec §FR-002]
- [x] CHK002 ¿El criterio para determinar qué parámetros de consulta reconoce un endpoint es suficientemente claro y trazable al alcance del Issue #99, sin exigir una lista de campos que esa fuente no define? [Clarity, Assumption, Spec §FR-002]
- [x] CHK003 ¿Se especifica de forma consistente que se registran solo parámetros de consulta reconocidos y se omiten parámetros no reconocidos? [Consistency, Spec §FR-002, Edge Cases]
- [x] CHK004 ¿Los requisitos cubren consultas sin parámetros relevantes y permiten representar su ausencia sin inventar valores? [Coverage, Edge Case, Spec §Edge Cases]
- [x] CHK005 ¿La inclusión de peticiones que terminan con error está definida para todo el alcance REST, no solo para respuestas exitosas? [Coverage, Exception Flow, Spec §FR-001, US1]
- [x] CHK006 ¿La duración en milisegundos tiene un significado verificable y consistente entre la especificación y el contrato del registro? [Measurability, Consistency, Spec §FR-002]

## Notas

- El Issue #99 y la investigación de la Feature indican que no hay una lista cerrada de parámetros relevantes; no se inventan nombres de parámetros.
- Los ítems marcados reflejan la evaluación solicitada; los ítems sin marcar siguen pendientes de resolución o evaluación.
- `/speckit.implement` lee el estado de los checklists y no debe modificar sus marcadores.

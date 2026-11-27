# General Checklist: Auditoría de peticiones a servicios web

**Purpose**: Revisar la completitud, claridad, consistencia y verificabilidad de los requisitos de auditoría.
**Created**: 2026-11-26
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado revisa la calidad de los requisitos; no verifica la implementación.
**Review Ownership**: El checklist pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad del requisito está satisfecho.
**Marker Semantics**: `[x]` expresa satisfacción del criterio de calidad del requisito, no finalización de implementación.

## Completitud y claridad de requisitos

- [x] CHK001 ¿El alcance de «todas las invocaciones» identifica con claridad qué servicios REST publicados quedan incluidos y mantiene excluidas las llamadas salientes a terceros? [Completeness, Clarity, Spec §FR-001, §FR-007]
- [x] CHK002 ¿Los datos obligatorios de cada registro —timestamp, usuario, método HTTP, ruta u operación, parámetros relevantes y duración en milisegundos— están definidos de forma consistente? [Completeness, Spec §FR-002]
- [x] CHK003 ¿La distinción entre identidad autenticada y `anonymous` está especificada para los casos contemplados, sin dejar una alternativa de identidad implícita? [Clarity, Spec §FR-003–FR-004]
- [x] CHK004 ¿Está claro que las peticiones que terminan con error también deben producir un registro y que este requisito coincide en todos los escenarios y criterios aplicables? [Consistency, Coverage, Spec §FR-001, US1]
- [x] CHK005 ¿Las exclusiones de persistencia en tabla, interfaz gráfica y llamadas salientes están delimitadas de manera coherente entre requisitos, supuestos y alcance? [Consistency, Spec §FR-006–FR-007]

## Notas

- Los ítems marcados reflejan la evaluación solicitada; los ítems sin marcar siguen pendientes de resolución o evaluación.
- Este artefacto evalúa la redacción de requisitos, no el comportamiento del código.
- `/speckit.implement` lee el estado de los checklists y no debe modificar sus marcadores.

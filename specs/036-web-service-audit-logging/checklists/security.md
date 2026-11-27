# Security Checklist: Auditoría de peticiones a servicios web

**Purpose**: Revisar la completitud y precisión de los requisitos de privacidad y protección de credenciales en los registros.
**Created**: 2026-11-26
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado revisa la calidad de los requisitos; no verifica la implementación.
**Review Ownership**: El checklist pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad del requisito está satisfecho.
**Marker Semantics**: `[x]` expresa satisfacción del criterio de calidad del requisito, no finalización de implementación.

## Calidad de requisitos de seguridad

- [x] CHK001 ¿La exclusión de identificadores de consulta, credenciales y contraseñas es consistente en los requisitos y cubre los campos de auditoría permitidos? [Completeness, Consistency, Spec §FR-002, §FR-005]
- [x] CHK002 ¿Está claramente delimitado qué datos de la petición quedan fuera del registro para evitar incluir información no requerida? [Clarity, Spec §FR-005]
- [x] CHK003 ¿Los requisitos distinguen con claridad la identidad obtenida para una petición autenticada de la identidad literal `anonymous` para una petición no autenticada? [Clarity, Coverage, Spec §FR-003–FR-005]
- [x] CHK004 ¿Se excluyen los identificadores de consulta mientras pueden registrarse los demás parámetros reconocidos, sin registrar credenciales ni contraseñas? [Consistency, Spec §FR-002, §FR-005]
- [x] CHK005 ¿El criterio de protección permite evaluar de forma objetiva que los registros no expongan credenciales ni contraseñas en texto plano, sin introducir obligaciones de seguridad no acordadas? [Acceptance Criteria, Measurability, Spec §FR-005, §SC-003]

## Notas

- No convertir categorías hipotéticas de datos sensibles en requisitos nuevos sin decisión humana.
- Los ítems marcados reflejan la evaluación solicitada; los ítems sin marcar siguen pendientes de resolución o evaluación.
- `/speckit.implement` lee el estado de los checklists y no debe modificar sus marcadores.

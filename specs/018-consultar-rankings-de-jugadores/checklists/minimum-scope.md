# Alcance mínimo Checklist: Consultar ranking de jugadores por score

**Purpose**: Revisar que los requisitos del delta incluyan lo indispensable para cumplirlo y no eleven ampliaciones opcionales a obligaciones.
**Created**: 2026-11-12
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [research.md](../research.md) · [contracts/score-ranking-http.md](../contracts/score-ranking-http.md)

**Note**: Este checklist personalizado evalúa calidad y proporcionalidad de requisitos, no implementación.
**Review Ownership**: This checklist is a reviewer-owned requirements-quality review artifact. Mark an item `[x]` only when the reviewer determines the requirements-quality criterion is satisfied.
**Marker Semantics**: `[x]` means the criterion has been reviewed and satisfied for requirements quality. It does not mean implementation work is complete.

## Requerimientos indispensables

- [x] CHK001 ¿La especificación limita el cambio a paginación, filtros y datos identificatorios requeridos para el ranking por score, sin reabrir el ranking por precio de SPEC-016? [Completeness, Spec §FR-001, §FR-018–§FR-021]
- [x] CHK002 ¿La elegibilidad añadida por métricas se justifica explícitamente por el contrato funcional aplicable de SPEC-002/SPEC-016, en vez de constituir una población nueva? [Assumption, Plan vigente §Contexto técnico]
- [x] CHK003 ¿Se distingue con claridad qué fija la última ejecución finalizada (estrategia y encabezado) de qué selección de valuación proporciona el score, sin imponer una fecha de valuación innecesaria? [Clarity, Spec §FR-002–§FR-004; Research §R3–R4]

## Exclusiones y límites de requisito

- [x] CHK004 ¿Quedan expresamente fuera rutas o campos adicionales de podio, selección por estrategia/fecha/orden, snapshots compartidos entre solicitudes y recálculo/escritura? [Completeness, Spec §FR-018, §FR-021; Contract §Podio y totales]
- [x] CHK005 ¿Los requisitos de rendimiento, consultas, transacción y estructura se mantienen como decisiones de diseño necesarias, sin convertir optimizaciones, índices, caché o dependencias no justificadas en alcance obligatorio? [Scope, Plan vigente §Contexto técnico; Research §R7]
- [x] CHK006 ¿La política de nulabilidad se limita a datos de catálogo realmente ausentes y evita inventar fallbacks o campos adicionales? [Clarity, Spec §FR-020; Contract §GET /api/rankings/score]

## Criterios verificables y coherencia

- [x] CHK007 ¿Los criterios de aceptación cubren las obligaciones del delta —filtros AND, ranking global antes de filtrar, totales filtrados, opciones globales y podio por la misma ruta— sin exigir comportamiento ajeno? [Coverage, Spec §FR-018–§FR-021, Aceptación US1–US3]
- [x] CHK008 ¿Los casos obligatorios de ejecución abierta, ausencia de valuaciones, página fuera de rango, filtro vacío y parámetros inválidos están definidos con respuestas observables, diferenciándolos de recomendaciones? [Measurability, Spec §Casos límite del delta]
- [x] CHK009 ¿Spec, contrato y modelo vigente concuerdan en nombres y semántica de campos paginados, entrada `player`, restricciones de consulta y orden global, sin que el antecedente histórico parezca normativa vigente? [Consistency, Spec §FR-018–§FR-021; Contract vigente; Data model vigente]

## Notes

- Revisión asistida solicitada por el revisor: CHK001–CHK009 cubiertos. El usuario confirmó excluir jugadores cuya última fila de estadísticas tenga `present_metrics = 0`, condicionado a que no complique la implementación; se acepta conforme al plan vigente, que la justifica por la elegibilidad de SPEC-002/SPEC-016.
- Aclaración posterior del usuario: prevalece el criterio de SPEC-016 concretado en FR-022; si varias fuentes comparten la fecha más reciente, basta una fila con métricas. Se conserva la nota anterior como evidencia histórica, sin alterar marcadores del revisor.
- `/speckit.implement` lee el estado de los marcadores y no debe modificarlos.
- `checklists/requirements.md` conserva su ciclo separado, mantenido por `/speckit.specify` y `/speckit.clarify`.

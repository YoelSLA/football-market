# Checklist de contrato y seguridad — delta de SPEC-018

**Propósito**: revisión humana de la calidad de los requisitos del contrato paginado vigente, no de su implementación. Complementa el checklist histórico `api-security.md` sin alterar su evaluación.

**Fuentes**: [`spec.md`](../spec.md), [`contracts/score-ranking-http.md`](../contracts/score-ranking-http.md) y [`plan.md`](../plan.md). Los marcadores pertenecen al revisor y permanecen pendientes hasta su evaluación explícita.

- [x] CHK001 ¿Están definidos la ruta, el método, los parámetros admitidos, sus valores por defecto y las reglas de validación de página, tamaño y filtros? [Spec FR-001, FR-018; contrato §GET]
- [x] CHK002 ¿Están definidas las comparaciones de filtros combinados, el recorte de espacios y el tratamiento literal de `%` y `_`? [Spec FR-018; contrato §GET]
- [x] CHK003 ¿La respuesta paginada define los campos, la nulabilidad y la semántica de entradas, totales filtrados y opciones globales, incluidos los estados vacíos? [Spec FR-019–FR-020; contrato §GET y §Casos y errores]
- [x] CHK004 ¿Están definidos el orden y las posiciones globales antes de filtros y página, y la consulta de podio sin filtros? [Spec FR-005, FR-021; contrato §Podio y totales]
- [x] CHK005 ¿Se exige JWT válido y se distinguen `400`, `401` y `500` seguro sin datos parciales ni detalles internos? [Spec FR-007, FR-018, SC-017; contrato §Casos y errores]
- [x] CHK006 ¿La elegibilidad por métricas y su tratamiento para fuentes de la misma fecha están definidos sin cambiar el contrato del ranking por cotización? [Spec FR-022; plan vigente]

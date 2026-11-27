# Checklist de calidad de integración: SPEC-009

**Feature**: [spec.md](../spec.md), [plan.md](../plan.md), [player-statistics.md](../contracts/player-statistics.md) y [player-detail.md](../contracts/player-detail.md)
**Propósito**: Evaluar completitud, consistencia y robustez de los contratos de consulta y del modelo de integración del backend.

---

## 1. Completitud y claridad contractual

- [x] CHK001 ¿Está delimitada la consulta individual respecto de identidad, sincronización y valoración poblacional? [Completeness, Spec §FR-001/FR-017, Plan §Backend]
- [x] CHK002 ¿Está definida la temporada explícita y el criterio de selección cuando no se aporta parámetro? [Clarity, Spec §FR-017, Contrato §player-statistics]
- [x] CHK003 ¿La definición de extracción confirmada distingue fila persistida, ítem confirmado y contexto resuelto? [Clarity, Contrato §player-statistics, Selección punto 2]
- [x] CHK004 ¿Están documentados todos los criterios de orden y desempate del snapshot estadístico? [Completeness, Contrato §player-statistics, Selección punto 3]
- [x] CHK005 ¿La prioridad de fuentes (anclaje JSON) y el contexto requerido para respaldo con SCRAPING son inequívocos? [Clarity, Spec §FR-017, Contrato §player-statistics]
- [x] CHK006 ¿La conservación del cero y la diferencia entre nulo y error son consistentes entre spec y contrato? [Consistency, Spec §FR-018, Contrato §player-statistics]

---

## 2. Cobertura de escenarios

- [x] CHK007 ¿Se contempla el caso de SCRAPING como única fuente confirmada sin exigir JSON previo? [Coverage, Contrato §player-statistics, Selección punto 4]
- [x] CHK008 ¿Se excluye explícitamente el respaldo o fusión de datos de otra fecha, temporada o competición? [Coverage, Spec §FR-017, Contrato §player-statistics]
- [x] CHK009 ¿La ausencia de snapshot (`NO_DATA`) y la disponibilidad parcial (`PARTIAL`) devuelven respuestas HTTP 200 coherentes? [Completeness, Contrato §player-statistics, Modelo §SnapshotStatus]
- [x] CHK010 ¿La consulta concurrente con una extracción tiene una garantía de coherencia mediante transacciones de lectura? [Coverage, Plan §Backend, Modelo §PlayerStatisticsSnapshot]

---

## 3. Consistencia y dependencias

- [x] CHK011 ¿El contrato estadístico delimita 60 claves, unidades, nulabilidad y procedencia efectiva sin mezclar operaciones financieras? [Consistency, Spec §FR-013/FR-017, Contrato §player-statistics]
- [x] CHK012 ¿Están delimitadas las responsabilidades de lectura, composición y transformación sin contradicciones arquitectónicas? [Consistency, Plan §Backend, Modelo §PlayerStatisticsSnapshot]
- [x] CHK013 ¿Está delimitada la preservación de consumidores existentes sin efectos colaterales en el catálogo? [Consistency, Plan §Frontend, Research §Decisiones]
- [x] CHK014 ¿Está definida la resolución de competición legible mediante correspondencia local fiable y fallback seguro («Competición no disponible»)? [Completeness, Spec §FR-006, Contrato §player-statistics]

---

## 4. Seguridad y manejo de errores

- [x] CHK015 ¿Son explícitos los requisitos de autenticación (JWT Bearer), visibilidad de catálogo y ausencia de mutaciones en las consultas? [Completeness, Spec §FR-002/FR-003, Contrato §player-statistics]
- [x] CHK016 ¿La exigencia de identificador entero positivo está alineada entre todas las consultas individuales (HTTP 400 ante parámetros inválidos, HTTP 404 ante inexistente o no visible)? [Consistency, Spec §FR-004, Contratos §player-detail / §player-statistics]

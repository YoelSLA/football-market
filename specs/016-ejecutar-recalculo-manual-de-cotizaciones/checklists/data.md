# Checklist de datos e integridad: delta diario de SPEC-016

**Propósito**: revisar precisión, consistencia y cobertura de los requisitos de unicidad diaria, concurrencia, auditoría y corridas parciales.
**Creado**: 2026-11-10
**Feature**: [spec.md](../spec.md) · [modelo de datos](../data-model.md) · [research.md](../research.md)
**Profundidad y destinatario**: estándar; revisor humano tras PLAN, antes de TASKS.

**Nota**: checklist generado por `/speckit.checklist`, propiedad del revisor. `[x]` expresa conformidad del revisor con la calidad de requisitos, no trabajo implementado. Los ítems nuevos quedan sin marcar.

## Completitud de identidad y ciclo de vida

- [x] CHK001 - ¿La unicidad está definida por jugador y día para todos los orígenes, incluida la inicialización, sin coexistencia permitida de filas recalculadas y arrastradas del mismo día? [Claridad, Spec §FR-024/FR-026, Data model §Modelo vigente del delta]
- [x] CHK002 - ¿La independencia entre identidad de corrida y día está explicitada para ejecuciones manuales, automáticas y concurrentes con estrategias distintas? [Completitud, Spec §FR-060, §Aceptación del cambio — escenario 3]
- [x] CHK003 - ¿Los requisitos de vinculación entre cotización y parámetros de su corrida distinguen identidad única de fecha o instante compartidos y preservan el significado del origen INITIAL? [Trazabilidad, Spec §FR-029, Research §R2, Data model §Modelo vigente del delta]
- [x] CHK004 - ~~¿El ciclo de vida de contexto en curso, resumen finalizado y cierre interrumpido está definido sin presentar un registro incompleto como éxito ni ampliar unilateralmente las obligaciones de recuperación?~~ Retirado como pendiente sobre visibilidad de resúmenes incompletos por decisión del usuario. [Consistencia, Research §R2, Contrato §Ejecución y consultas de cotización, TD-024]

## Integridad y cobertura de escenarios

- [x] CHK005 - ¿Los requisitos concurrentes distinguen evitar un segundo ajuste, evitar sobrescritura y permitir completar pendientes, con resultados observables suficientes para evaluar cada garantía? [Medibilidad, Spec §FR-060, §Aceptación del cambio — escenario 3]
- [x] CHK006 - ¿La regla de precio anterior y vigente sigue siendo coherente con la aplicación diaria del mismo score y con la fecha original de valuación conservada para auditoría? [Consistencia, Spec §FR-005/FR-027, §FR-028, §Aceptación del cambio — escenario 1]
- [x] CHK007 - ¿Los límites temporales de una corrida que cruza medianoche y de una valuación recibida después de cotizar están expresados de forma compatible con la unicidad diaria? [Cobertura, Spec §FR-024/FR-060 — cambio de día, §Aceptación del cambio — escenario 4]
- [x] CHK008 - ¿La población evaluada y las categorías de recalculados, arrastrados, omitidos y fallidos están definidas sin solapamientos ni confusión entre valor cero y ausencia de score? [Claridad, Spec §FR-003, §FR-008, §FR-061, Data model §Modelo vigente del delta]

## Restricciones y dependencias

- [x] CHK009 - ¿El diseño de cambio de esquema explicita inmutabilidad de migraciones integradas, datos descartables y autorización separada de operaciones destructivas, sin exigir conservación de datos no acordada? [Consistencia, Data model §Esquema y despliegue, Constitution §Gestión de datos en desarrollo]
- [x] CHK010 - ¿La limitación TD-024 y su impacto en reconstrucción de parámetros y resúmenes están documentados sin declararlos resueltos por la nueva cardinalidad de corridas ni trasladar su solución al scheduler? [Trazabilidad, Spec §FR-029/FR-061, Research §R2, TD-024]

## Notas

- Revisión humana: **PENDIENTE**; no hay evaluación ni aceptación automática de estos 10 ítems.
- No es checklist de pruebas, migraciones ejecutadas ni verificación de código; se evalúa la calidad del requisito documentado.
- `/speckit.implement` lee los marcadores sin modificarlos. `requirements.md` conserva su ciclo independiente.
- No se incorporan mecanismos de compensación o recuperación no autorizados. Un hallazgo bloquea solo si impide cumplir un requisito, criterio de aceptación u obligación vigente; recomendaciones no se convierten en gates.

**Evaluación asistida solicitada por el usuario — 2026-11-10**: 9/10 criterios contemplados y marcados según fuentes referenciadas. Sustituye la nota anterior de ausencia de evaluación; no aprueba implementación ni resuelve deudas por declaración.

**Pendiente CHK004**: el ciclo está descrito, pero excluir de la consulta un contexto sin cierre es una decisión observable añadida en PLAN, aún sin acuerdo explícito en la spec. Se requiere alinear ese límite, no implementar recuperación. Mismo hallazgo de cotizacion.md CHK024 y api.md CHK004.

**Decisión posterior del usuario — 2026-11-10**: retirado ese pendiente del delta diario. La consulta de contextos incompletos no constituye un criterio obligatorio; no se convierte el registro incompleto en éxito ni se elimina la trazabilidad de cotizaciones. TD-024 continúa abierta, no resuelta.

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

- [x] CHK005 - ¿Se distingue la unicidad de fila de la posibilidad de nuevos ajustes del mismo día, definiendo el reemplazo atómico del último cálculo válido y su ejecución productora sin duplicar cotizaciones? [Medibilidad, Spec §Decisión vigente de integración, §FR-026/FR-060, §Aceptación del cambio — escenario 3; Data model §Modelo vigente; Research §R1/R2]
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

## Corrección C1 y contraste documental de integración — 2026-11-11

Solicitada por el usuario tras ANALYZE: «Mas que reevaluacion, quiero que lo corrigas. Por L1
tambien podes corregirlo». No se cambia ningún marcador ni se atribuye aprobación humana nueva.
Los `[x]` conservan su procedencia histórica; el contraste siguiente documenta las fuentes del
alcance actual, no ejecución de tests ni aprobación de IMPLEMENT, CODE REVIEW, TESTING o cierre.

**Antecedente conservado de CHK005**: preguntaba por «evitar un segundo ajuste, evitar sobrescritura
y permitir completar pendientes». Esa expectativa queda sustituida por la decisión humana de
último cálculo válido; no es una garantía vigente. CHK004 continúa retirado, no se reactiva.

| Criterios afectados | Contraste con la integración vigente |
| --- | --- |
| CHK001–CHK003 | `spec.md` §Decisión vigente de integración: una fila diaria, corridas independientes, FK de la última ejecución y actualización de INITIAL sin duplicar ni perder inicialización. `data-model.md` §Modelo vigente desarrolla columnas e invariantes. |
| CHK005–CHK006 | FR-026/027/028/060 y `research.md` R1/R2 permiten varios cálculos sobre precio vigente; reemplazo íntegro bajo lock de jugador, mismo ID y nueva referencia de ejecución. No hay límite de un ajuste diario ni obligación de conservar precios intermedios. |
| CHK007 | FR-024/060 y aceptación 4/6: día UTC fijado al inicio, nueva valuación utilizable en otra manual del día, omisión solo si la fecha vigente es posterior. |
| CHK008 | FR-061 y contrato HTTP vigente: reemplazo cuenta como recalculado/arrastrado; solo fecha posterior es omisión; fallos separados y contadores disjuntos. |
| CHK009–CHK010 | `integration-baseline.md` §2 y T084: migraciones sin DROP duplicado, checksums inmutables protegidos, colisiones sin limpieza automática; contexto confirmado antes de publicar y TD-024 pendiente. |

Fuentes compartidas: [base de integración](../../023-scheduler-quote-recalculation/integration-baseline.md),
[modelo](../data-model.md), [research](../research.md), [contrato HTTP](../contracts/quotes-http.md).
Los textos anteriores de evaluación pendiente/resuelta describen 2026-11-10, no contradicen este
evento de corrección. El resultado de un nuevo ANALYZE queda pendiente; no se declara superado aquí.

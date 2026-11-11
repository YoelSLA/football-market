# Concurrency & Integrity Checklist: Programar recálculo automático de cotizaciones

**Purpose**: Evaluar la calidad de los requisitos de unicidad, exclusión de solapamientos, atomicidad y conservación de datos de la feature, sin evaluar su implementación.
**Created**: 2026-11-03
**Feature**: [spec.md](../spec.md)
**Depth**: Standard · **Audience**: Revisor (PR)

**Note**: Checklist especializado generado por `/speckit.checklist` a partir de los riesgos de integridad del recálculo financiero.
**Review Ownership**: Artefacto de revisión propiedad del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Unicidad por fecha

- [x] CHK001 ¿Se define con precisión qué fecha UTC identifica la unicidad y cómo se obtiene respecto de la fecha de cotización? [Clarity, Spec §FR-006] — [data-model §1](../data-model.md) y [research §9](../research.md) fijan `quote_date` como la fecha UTC del día en que corre.
- [x] CHK002 ¿Se diferencia el límite de ejecuciones automáticas de la admisión de ejecuciones manuales múltiples en una misma fecha? [Completeness, Spec §FR-006, §FR-015] — FR-006 y FR-015 permiten múltiples ejecuciones manuales y limitan a una las automáticas por fecha UTC.
- [x] CHK003 ¿Se define el resultado observable de un disparo duplicado: ausencia de escrituras, ausencia de segunda ejecución y constancia operativa del intento? [Completeness, Spec §FR-006, §SC-007] — FR-006 y SC-007 lo fijan; el [contrato §4](contracts/scheduling-configuration.md), caso 3, lo detalla.
- [x] CHK004 ¿Se declara el alcance real de la garantía entre instancias o procesos concurrentes, o se supone explícitamente una instancia única? [Assumption, Gap, Spec §FR-006] — [research §3](../research.md) registra el supuesto de instancia única y el alcance real de cada capa.

## Exclusión de solapamientos

- [x] CHK005 ¿Se define el rechazo por fecha ya registrada? [Clarity, Spec §FR-006] — El catálogo operativo registra `EXECUTION_ALREADY_EXISTS`; no se añade un motivo de lock global.
- [x] CHK006 ¿Se define el alcance de la unicidad? [Coverage, Spec §FR-006, §FR-015] — La unicidad por fecha aplica solo a corridas AUTOMATIC; no se limita la cantidad de corridas MANUAL ni se añade exclusión mutua global.
- [x] CHK007 ¿Se define el alcance frente a una corrida bloqueada? [Recovery, Gap, Spec §FR-006] — La recuperación y el bloqueo global no forman parte de esta feature.

## Atomicidad e integridad de datos

- [x] CHK008 ¿Se definen «cotizaciones parciales», «historial incompleto» y la unidad de atomicidad (corrida completa o jugador) con criterios verificables? [Clarity, Spec §FR-013] — [research §16](../research.md) define la atomicidad por jugador y la fila completa o inexistente. La expresión «historial incompleto» se retiró de SC-003 el 2026-11-04: la feature no compensa las cotizaciones ya confirmadas por jugadores individuales (TD-024), porque esa responsabilidad excede su alcance.
- [x] CHK009 ¿Se distingue el efecto de un fallo completo del de un fallo individual de jugador sobre la atomicidad y el resultado registrado? [Consistency, Spec §FR-013] — [contrato §4](contracts/scheduling-configuration.md), casos 6 y 7, y las transiciones de [data-model §3](../data-model.md).
- [x] CHK010 ¿Se identifica la responsabilidad ante cotizaciones confirmadas y fallos? [Completeness, Spec §FR-013] — El scheduler registra el error y no compensa cotizaciones; la integridad y persistencia corresponden al recálculo existente (TD-024).
- [x] CHK011 ¿Se aclara cuándo puede reintentarse manualmente el mismo día? [Completeness, Spec §FR-006] — Si no hay ejecución registrada, rige el reintento de SPEC-016; una ejecución registrada, incluso con fallos individuales, consume la fecha.
- [x] CHK012 ¿Se define el comportamiento si falla la propia reserva o el registro persistido de la ejecución? [Recovery, Gap, Spec §FR-004, §FR-013] — [research §7](../research.md): excepción no controlada → rollback sin fila y evento `failed`; duplicado → omisión controlada.

## Rechazos y registro operativo

- [x] CHK013 ¿Se enumeran de forma cerrada los motivos de omisión, rechazo y fallo, con la información mínima (fecha y motivo) que permite al operador distinguirlos? [Completeness, Gap, Spec §FR-006, §FR-012, §FR-013] — [contrato §5](contracts/scheduling-configuration.md) fija nombres, niveles y campos.
- [x] CHK014 ¿Se define dónde queda cada desenlace (registro persistido de ejecuciones o registro operativo), sin duplicarlo y sin alterar cotizaciones existentes al omitir o rechazar? [Consistency, Spec §FR-004, §FR-006, §FR-012, §FR-013] — [contrato §4–§5](contracts/scheduling-configuration.md) y [data-model §5](../data-model.md).
- [x] CHK015 ¿Se declaran explícitamente fuera de alcance los reintentos automáticos, la compensación de escrituras y la purga de registros? [Consistency, Spec §Assumptions] — [research §7](../research.md), [§12](../research.md) y [contrato §6](contracts/scheduling-configuration.md).

## Notes

- Decisiones del revisor (2026-11-03): recuperación ante corrida bloqueada fuera de alcance; FR-007 eliminada y atomicidad por jugador conservada por SPEC-016.
- La expectativa operativa es que no se inicien dos recálculos simultáneamente ni se dispare el scheduler mientras otro recálculo está en curso. No se exige una garantía técnica de exclusión mutua en esta feature; T026 conserva una prueba histórica, no un requisito vigente.
- Marcar `[x]` solo tras revisar y confirmar que el criterio de calidad de requisitos se cumple.
- Los elementos se dejan sin marcar mientras requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.
- `checklists/requirements.md` tiene un ciclo de vida propio mantenido por `/speckit.specify` y `/speckit.clarify`.

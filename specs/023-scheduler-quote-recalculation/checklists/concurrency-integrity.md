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
- [x] CHK003 ¿Se define el resultado observable de un disparo automático duplicado: ausencia de nuevas publicaciones, ausencia de segunda ejecución automática y constancia operativa? [Completeness, Spec §FR-006, §SC-007] — FR-006/SC-007 y [contrato integrado](../contracts/scheduling-configuration.md); las manuales siguen permitidas.
- [x] CHK004 ¿Se declara el alcance real de la garantía entre instancias o procesos concurrentes, o se supone explícitamente una instancia única? [Assumption, Gap, Spec §FR-006] — [research §3](../research.md) registra el supuesto de instancia única y el alcance real de cada capa.

## Exclusión de solapamientos

- [x] CHK005 ¿Se define el rechazo por fecha ya registrada? [Clarity, Spec §FR-006] — El catálogo operativo registra `EXECUTION_ALREADY_EXISTS`; no se añade un motivo de lock global.
- [x] CHK006 ¿Se define el alcance de la unicidad? [Coverage, Spec §FR-006, §FR-015] — La unicidad por fecha aplica solo a corridas AUTOMATIC; no se limita la cantidad de corridas MANUAL ni se añade exclusión mutua global.
- [x] CHK007 ¿Se define el alcance frente a una corrida bloqueada? [Recovery, Gap, Spec §FR-006] — La recuperación y el bloqueo global no forman parte de esta feature.

## Atomicidad e integridad de datos

- [x] CHK008 ¿Se define la unidad de atomicidad y el efecto de un fallo al insertar o reemplazar una cotización? [Clarity, Spec §FR-013/014] — [Base integrada §2](../integration-baseline.md): cálculo completo por jugador; un reemplazo fallido conserva el último cálculo confirmado, sin cambios parciales. No compensar publicaciones confirmadas si falla el resumen (TD-024).
- [x] CHK009 ¿Se distinguen fallo global, fallo individual y contexto sin cierre respecto de atomicidad y resultados consultables? [Consistency, Spec §FR-005/013] — [Contrato integrado](../contracts/scheduling-configuration.md) y [modelo vigente](../data-model.md): contexto puede persistir sin fin y cotizaciones pueden quedar confirmadas; solo finalizadas en GET, nunca contexto abierto como éxito.
- [x] CHK010 ¿Se identifica la responsabilidad ante cotizaciones confirmadas y fallos? [Completeness, Spec §FR-013] — El scheduler registra el error y no compensa cotizaciones; la integridad y persistencia corresponden al recálculo existente (TD-024).
- [x] CHK011 ¿Se aclara que las manuales pueden repetirse el mismo día independientemente de corridas previas y sus fallos? [Completeness, Spec §FR-006/015] — [Base común §1–2](../integration-baseline.md): manuales ilimitadas tras validaciones; solo contexto AUTOMATIC registrado consume cuota automática, incluso sin cierre.
- [x] CHK012 ¿Se distingue fallo al reservar de fallo al cerrar un contexto ya confirmado y de conflicto automático? [Recovery, Spec §FR-004/013] — [Diseño integrado](../integration-baseline.md): reserva fallida no confirma contexto; fallo posterior puede dejar contexto sin fin y cotizaciones confirmadas; solo conflicto del índice AUTOMATIC se traduce a `skipped`, sin confundir otros errores SQL.

## Rechazos y registro operativo

- [x] CHK013 ¿Se enumeran de forma cerrada los motivos de omisión, rechazo y fallo, con la información mínima (fecha y motivo) que permite al operador distinguirlos? [Completeness, Spec §FR-006/012/013] — [Contrato integrado y catálogo operativo](../contracts/scheduling-configuration.md) conservan nombres, niveles y campos.
- [x] CHK014 ¿Se distingue registro de contexto, resumen finalizado y evento operativo sin alterar cotizaciones al omitir o rechazar? [Consistency, Spec §FR-004/005/006/012/013] — [Contrato integrado](../contracts/scheduling-configuration.md) y [modelo vigente](../data-model.md); solo el último cálculo confirmado referencia su ejecución productora.
- [x] CHK015 ¿Se declaran fuera de alcance reintentos automáticos, compensación y purga? [Consistency, Spec §Assumptions] — [Research integrado](../research.md) y [contrato vigente](../contracts/scheduling-configuration.md); TD-024 sigue pendiente.

## Notes

- Decisiones del revisor (2026-11-03): recuperación ante corrida bloqueada fuera de alcance; FR-007 eliminada y atomicidad por jugador conservada por SPEC-016.
- La expectativa operativa es que no se inicien dos recálculos simultáneamente ni se dispare el scheduler mientras otro recálculo está en curso. No se exige una garantía técnica de exclusión mutua en esta feature; T026 conserva una prueba histórica, no un requisito vigente.
- Marcar `[x]` solo tras revisar y confirmar que el criterio de calidad de requisitos se cumple.
- Los elementos se dejan sin marcar mientras requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.
- `checklists/requirements.md` tiene un ciclo de vida propio mantenido por `/speckit.specify` y `/speckit.clarify`.

## Corrección C1/L1 — 2026-11-11

Solicitada por el usuario tras ANALYZE; conserva IDs, marcadores y decisiones históricas sin
conceder nueva aprobación humana. El contraste documental actual está en las fuentes vigentes
enlazadas en CHK003/008/009/011–015: cuota automática, manuales ilimitadas, reemplazo atómico,
contexto confirmado y cierre independiente. No se interpretan `[x]` históricos como tests ni gates.

**Antecedentes conservados**: CHK011 afirmaba que una ejecución registrada «consume la fecha»;
CHK012 refería «rollback sin fila» ante fallo no controlado. La integración sustituye esas
justificaciones: cuota solo automática y posible contexto persistido sin cierre. CHK008 hablaba
de fila «completa o inexistente»; el reemplazo fallido conserva el cálculo previo íntegro.

L1: enlaces contractuales corregidos a `../contracts/`. Nuevo ANALYZE pendiente; no ejecutado aquí.

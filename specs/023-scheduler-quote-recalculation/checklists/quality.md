# Requirements Quality Checklist: Programar recálculo automático de cotizaciones

**Purpose**: Evaluar la calidad, claridad, consistencia, medibilidad y cobertura de los requisitos de la feature, sin evaluar su implementación.
**Created**: 2026-11-03
**Feature**: [spec.md](../spec.md)
**Depth**: Standard · **Audience**: Revisor (PR)

**Note**: Checklist de calidad de requisitos generado por `/speckit.checklist`. No verifica comportamiento del sistema ni sustituye la revisión humana.
**Review Ownership**: Artefacto de revisión propiedad del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Completitud de requisitos

- [x] CHK001 ¿Está delimitado qué comprende exactamente el «alcance procesado» que FR-004 exige registrar (población evaluada, filas recompuestas, filas arrastradas o jugadores no recotizados)? [Clarity, Spec §FR-004] — Delimitado en [data-model §1](../data-model.md) y en los contadores del [contrato HTTP §2](contracts/quote-executions-http.md).
- [x] CHK002 ¿Está especificado el resultado esperado cuando no hay valuaciones disponibles o actualizadas al momento del disparo, caso listado en Edge Cases pero sin requisito asociado? [Coverage, Gap, Spec §Edge Cases] — Cubierto por herencia de SPEC-016 (§FR-008, §FR-061): sin valuación se arrastra el precio (`CARRIED_FORWARD`); sin valuación posterior a la referencia no se reprecia.
- [x] CHK003 ¿Existe un criterio de éxito medible que cubra el rechazo controlado por mercado no inicializado y su constancia operativa (FR-012), ausente en SC-001 a SC-008? [Coverage, Gap, Spec §FR-012, §SC] — CA-04 aceptado por el revisor como criterio medible (decisión 2026-11-03).
- [x] CHK004 ¿Están definidos el medio, el contenido mínimo, la forma de consulta y la retención del «registro operativo» al que remiten FR-006, FR-012 y FR-013, aclarando si la retención indefinida de FR-005 le aplica? [Completeness, Clarity, Gap, Spec §FR-005, §Key Entities] — Medio, contenido y consulta definidos; retención declarada fuera de alcance en [Spec §Assumptions](../spec.md).

## Claridad

- [x] CHK005 ¿Está acotado qué canales abarca el «resultado expuesto» de FR-010 (respuesta HTTP, registro operativo u otros) y qué se considera «traza», «mensaje técnico» e «identificador interno»? [Ambiguity, Spec §FR-010] — El [contrato HTTP §3–§5](contracts/quote-executions-http.md) limita la respuesta a un catálogo cerrado y separa los logs como evidencia del operador.

## Consistencia

- [x] CHK006 ¿Quedó resuelta la ambigüedad entre «ejecución fallida» y una corrida con fallos individuales que sí se persiste con resultado `FAILED`, y está definido si esta última habilita el reintento del mismo día? [Conflict, Ambiguity, Spec §FR-013, §SC-003] — FR-013 exige registrar el error sin compensar. Según SPEC-016, si el fallo completo no deja registro, se puede reintentar manualmente; una corrida con fallos individuales sí se persiste con `result = FAILED` y consume la fecha. El usuario aceptó ese riesgo.
- [x] CHK007 ¿Queda una sola formulación de la restricción de unicidad heredada de SPEC-016, sin duplicarla ni contradecirla? [Consistency, Spec §FR-006] — Resuelto por consolidación (2026-11-04): FR-008 se integró en FR-006, que declara la restricción y su aplicación también a la vía automática.
- [x] CHK008 ¿Son consistentes los motivos de omisión, rechazo y fallo de US1, US2 y US3 con las entidades del modelo conceptual y con los requisitos que los originan? [Consistency, Spec §US1, §US2, §US3] — El [catálogo del contrato §5](contracts/scheduling-configuration.md) cubre cada escenario con un motivo estable.

## Criterios de aceptación y medición

- [x] CHK009 ¿Cada requisito funcional tiene al menos un escenario de aceptación o criterio de éxito observable que lo verifique, incluidos FR-009, FR-010 y FR-012? [Traceability, Spec §Requirements, §Success Criteria] — CA-04 aceptado por el revisor como criterio medible para FR-012 (decisión 2026-11-03).
- [x] CHK010 ¿Se define cómo se comprueba la equivalencia entre una ejecución automática y una manual que exigen US1 y FR-002? [Measurability, Spec §US1, §FR-002] — US1 (Independent Test) y [quickstart §3](../quickstart.md) fijan la comparación de contadores y precios con los mismos insumos.
- [x] CHK011 ¿Se declara que la feature no añade superficie HTTP nueva ni modifica la autorización de las rutas existentes? [Completeness, Spec §FR-005, §FR-010] — Declarado en [plan §Technical Context](../plan.md) y [contrato HTTP §1](contracts/quote-executions-http.md).

## Dependencias, supuestos y trazabilidad

- [x] CHK012 ¿Se validan SPEC-013 y SPEC-016 como dependencias cerradas y operativas, y se delimita #41 fuera de alcance sin ambigüedad? [Assumption, Spec §Assumptions] — [Spec §Assumptions](../spec.md) y [§Trazabilidad](../spec.md) lo registran explícitamente.
- [x] CHK013 ¿El esquema de identificadores RF, CA, FR y SC permite auditar la cobertura de cada requisito de origen? [Traceability, Spec §Trazabilidad] — El revisor considera suficiente la trazabilidad RF/CA→FR; no se exige mapeo FR→SC (decisión 2026-11-03).

## Notes

- Decisiones del revisor (2026-11-03): CA-04 cubre FR-012; retención del registro operativo y recuperación ante corrida bloqueada fuera de alcance; FR-007 eliminada; no se exige mapeo FR→SC.
- Marcar `[x]` solo tras revisar y confirmar que el criterio de calidad de requisitos se cumple.
- Los elementos se dejan sin marcar mientras requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.
- `checklists/requirements.md` tiene un ciclo de vida propio mantenido por `/speckit.specify` y `/speckit.clarify`.

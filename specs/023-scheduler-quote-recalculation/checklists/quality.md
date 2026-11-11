# Requirements Quality Checklist: Programar recálculo automático de cotizaciones

**Purpose**: Evaluar la calidad, claridad, consistencia, medibilidad y cobertura de los requisitos de la feature, sin evaluar su implementación.
**Created**: 2026-11-03
**Feature**: [spec.md](../spec.md)
**Depth**: Standard · **Audience**: Revisor (PR)

**Note**: Checklist de calidad de requisitos generado por `/speckit.checklist`. No verifica comportamiento del sistema ni sustituye la revisión humana.
**Review Ownership**: Artefacto de revisión propiedad del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Completitud de requisitos

- [x] CHK001 ¿Está delimitado qué comprende exactamente el «alcance procesado» que FR-004 exige registrar (población evaluada, filas recompuestas, filas arrastradas o jugadores no recotizados)? [Clarity, Spec §FR-004] — Delimitado en [modelo integrado](../data-model.md) y [contrato HTTP vigente](../contracts/quote-executions-http.md): reemplazos recalculados/arrastrados, omisiones por fecha posterior y fallos son categorías disjuntas.
- [x] CHK002 ¿Se diferencia la ausencia de score de su antigüedad, sin impedir el cálculo diario por usar un score antiguo? [Coverage, Spec §Edge Cases, §FR-014] — SPEC-016 FR-008/027 y [base integrada §1](../integration-baseline.md): sin score se arrastra; score antiguo se aplica y la igualdad de fecha permite actualizar la fila diaria.
- [x] CHK003 ¿Existe un criterio de éxito medible que cubra el rechazo controlado por mercado no inicializado y su constancia operativa (FR-012), ausente en SC-001 a SC-008? [Coverage, Gap, Spec §FR-012, §SC] — CA-04 aceptado por el revisor como criterio medible (decisión 2026-11-03).
- [x] CHK004 ¿Están definidos el medio, el contenido mínimo, la forma de consulta y la retención del «registro operativo» al que remiten FR-006, FR-012 y FR-013, aclarando si la retención indefinida de FR-005 le aplica? [Completeness, Clarity, Gap, Spec §FR-005, §Key Entities] — Medio, contenido y consulta definidos; retención declarada fuera de alcance en [Spec §Assumptions](../spec.md).

## Claridad

- [x] CHK005 ¿Está acotado qué canales abarca el «resultado expuesto» de FR-010 y qué se considera detalle técnico no contractual? [Clarity, Spec §FR-010] — El [contrato HTTP vigente](../contracts/quote-executions-http.md) permite `executionId` público y motivos cerrados, excluye trazas/IDs ajenos al contrato y separa logs operativos.

## Consistencia

- [x] CHK006 ¿Se distinguen fallo individual, contexto sin cierre y resultado finalizado, y su efecto sobre manuales y cuota automática? [Consistency, Spec §FR-006/013/015] — [Contrato integrado](../contracts/scheduling-configuration.md): manuales siempre permitidas tras las validaciones del caso de uso; contexto AUTOMATIC registrado consume solo la cuota automática aunque no cierre. El scheduler registra errores sin compensar; TD-024 sigue pendiente.
- [x] CHK007 ¿Queda una sola formulación de la restricción de unicidad heredada de SPEC-016, sin duplicarla ni contradecirla? [Consistency, Spec §FR-006] — Resuelto por consolidación (2026-11-04): FR-008 se integró en FR-006, que declara la restricción y su aplicación también a la vía automática.
- [x] CHK008 ¿Son consistentes los motivos de omisión, rechazo y fallo de US1, US2 y US3 con las entidades del modelo conceptual y con los requisitos que los originan? [Consistency, Spec §US1, §US2, §US3] — El [contrato integrado y catálogo operativo](../contracts/scheduling-configuration.md) conserva motivos estables: duplicado automático, mercado no inicializado y fallo; no rechaza manuales por fecha.

## Criterios de aceptación y medición

- [x] CHK009 ¿Cada requisito funcional tiene al menos un escenario de aceptación o criterio de éxito observable que lo verifique, incluidos FR-009, FR-010 y FR-012? [Traceability, Spec §Requirements, §Success Criteria] — CA-04 aceptado por el revisor como criterio medible para FR-012 (decisión 2026-11-03).
- [x] CHK010 ¿Se define cómo se comprueba la equivalencia entre una ejecución automática y una manual que exigen US1 y FR-002? [Measurability, Spec §US1, §FR-002] — US1 (Independent Test) y [quickstart §3](../quickstart.md) fijan la comparación de contadores y precios con los mismos insumos.
- [x] CHK011 ¿Se declara que la feature no añade superficie HTTP de scheduler ni modifica la autorización de las rutas existentes? [Completeness, Spec §FR-005, §FR-010] — Declarado en [plan integrado](../plan.md) y [contrato HTTP vigente](../contracts/quote-executions-http.md); el ranking de SPEC-016 mantiene su adopción coordinada aparte.

## Dependencias, supuestos y trazabilidad

- [x] CHK012 ¿Se identifican las dependencias y se distingue su cierre histórico de la integración todavía pendiente, manteniendo #41 fuera de alcance? [Traceability, Spec §Assumptions] — [Spec vigente](../spec.md) y [base común](../integration-baseline.md) no presumen correcta la combinación por cierres anteriores; las tareas compartidas mantienen responsables distintos.
- [x] CHK013 ¿El esquema de identificadores RF, CA, FR y SC permite auditar la cobertura de cada requisito de origen? [Traceability, Spec §Trazabilidad] — El revisor considera suficiente la trazabilidad RF/CA→FR; no se exige mapeo FR→SC (decisión 2026-11-03).

## Notes

- Decisiones del revisor (2026-11-03): CA-04 cubre FR-012; retención del registro operativo y recuperación ante corrida bloqueada fuera de alcance; FR-007 eliminada; no se exige mapeo FR→SC.
- Marcar `[x]` solo tras revisar y confirmar que el criterio de calidad de requisitos se cumple.
- Los elementos se dejan sin marcar mientras requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.
- `checklists/requirements.md` tiene un ciclo de vida propio mantenido por `/speckit.specify` y `/speckit.clarify`.

## Corrección C1/L1 — 2026-11-11

Corrección documental solicitada expresamente por el usuario tras ANALYZE. Marcadores e IDs
conservados; no se registra aprobación humana nueva ni se acredita implementación o testing.

**Evidencia histórica conservada**: CHK002 indicaba «sin valuación posterior a la referencia no se
reprecia»; CHK006 indicaba que una corrida con fallos individuales «consume la fecha». Esas
justificaciones se aplicaban al alcance anterior y están sustituidas por la decisión integrada.

**Contraste actual de C1**: CHK001/002/005/006/008/011/012 se contrastan con los encabezados
vigentes de spec, modelo, contratos y base común enlazados en cada criterio. Están documentadas la
aplicación de score antiguo, actualización diaria, partición de contadores, ID público y cuota
solo automática. No se trasladan responsabilidades de compensación al scheduler.

**L1**: enlaces contractuales resueltos desde `checklists/` mediante `../contracts/`.
Nuevo ANALYZE pendiente; esta corrección no lo ejecuta ni declara preparación superada.

# Checklist de requisitos — sincronización única sin auditoría

Fecha: 2026-11-01. Feature: [SPEC-008](../spec.md). Alcance: API, datos y estados del cambio autorizado. Complementa el checklist general y las reglas de identidad existentes; no añade requisitos de frontend, rendimiento hipotético ni roles nuevos.

Este checklist pertenece al revisor. Los ítems se generan sin marcar: su existencia no equivale a evaluación, implementación ni aprobación de gates. La revisión histórica de identidad no acredita automáticamente las partes afectadas por este cambio.

- [x] CHK041 ¿FR-001/012 y US2 precisan una única operación actual → histórico, elegibilidad, omisión ante actual FAILED y vacío sin solicitudes?
- [x] CHK042 ¿FR-016 y SC-007 definen sin contradicciones el estado de cada pase y la prioridad global FAILED > PARTIAL > COMPLETED, independientemente de cobertura?
- [x] CHK043 ¿FR-019/020 y SC-002 definen el universo de jugadores locales, categorías finales excluyentes, referencia preexistente y reemplazo del resultado provisional sin doble conteo?
- [x] CHK044 ¿FR-011/021 y SC-005 distinguen persistencia de referencias de auditoría retirada y preservan explícitamente casos de revisión del catálogo y reglas de identidad?
- [x] CHK045 ¿El contrato HTTP vigente precisa una sola respuesta, cuatro totales, estados finales, seguridad, errores y retirada incompatible de la operación histórica sin exigir ID previo?
- [x] CHK046 ¿El diseño conserva completitud interna de ligas y paginación sin convertir ese control en auditoría o desglose HTTP, distinguiendo fallo de ausencia de coincidencias?
- [x] CHK047 ¿FR-009/014/018/025/026 y el diseño conservan unicidad, protección de referencias, captura por pase, ausencia de transacción durante red y reintento seguro sin registros de ejecución?
- [x] CHK048 ¿Los escenarios y exclusiones cubren catálogo vacío, histórico vacío, errores de ambos pases, conflictos, referencias concurrentes y conservación de resultados confirmados sin ampliar alcance?

## Referencias para la revisión

- [Spec vigente y decisiones humanas](../spec.md#session-2026-11-01--alcance-vigente).
- [Diseño vigente](../plan.md#diseño-vigente--simplificación-2026-11-01).
- [Modelo vigente](../data-model.md#modelo-vigente--2026-11-01).
- [Contrato HTTP vigente](../contracts/http.md#contrato-vigente-del-cambio--2026-11-01).
- [Clarificación y evidencia de autorización](../clarify-sync-simplification.md).

Antes de Tasks se requiere revisión de estos criterios y de las partes afectadas del checklist general y de identidad. No se reclama evaluación ni se alteran marcadores históricos.

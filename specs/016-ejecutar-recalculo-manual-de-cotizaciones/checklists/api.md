# Checklist de API y seguridad: delta diario de SPEC-016

**Propósito**: revisar calidad y coherencia de los requisitos del ranking paginado y los resúmenes de múltiples corridas, incluidos autenticación y errores seguros.
**Creado**: 2026-11-10
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [contrato HTTP](../contracts/quotes-http.md)
**Profundidad y destinatario**: estándar; revisor humano tras PLAN, antes de TASKS.

**Nota**: checklist generado por `/speckit.checklist`, propiedad del revisor. `[x]` significa que el revisor encontró satisfecho un criterio de calidad de requisitos; no significa implementación completa. Todos los ítems se generan sin marcar.

## Completitud y claridad del contrato

- [x] CHK001 - ¿Los requisitos del ranking definen parámetros, valores por defecto, límites, filtros omitidos y coincidencia por nombre/liga/posición sin dejar decisiones funcionales implícitas? [Completitud, Spec §FR-039 — filtros, Contrato §GET /api/rankings/quote]
- [x] CHK002 - ¿La respuesta paginada distingue contenido, posición global, total filtrado y opciones globales, y define campos obligatorios, ausencias y nulabilidad de la identificación del jugador? [Claridad, Contrato §GET /api/rankings/quote]
- [x] CHK003 - ¿Los requisitos de vacío, ausencia de coincidencias y página fuera de rango especifican resultados distinguibles y compatibles con los totales documentados? [Cobertura, Contrato §GET /api/rankings/quote]
- [x] CHK004 - ~~¿La consulta de corridas define fecha UTC, identidad única, alcance de la lista sin paginación y orden total cuando varias corridas comparten instante de inicio?~~ Retirado como pendiente sobre visibilidad de resúmenes incompletos por decisión del usuario. [Claridad, Spec §FR-061, Contrato §Ejecución y consultas de cotización]

## Consistencia y compatibilidad

- [x] CHK005 - ¿El nuevo significado de `notRepricedPlayers` está declarado coherentemente en requisitos, resumen y contadores, sin conservar como significado vigente la ausencia de score nuevo? [Consistencia, Spec §FR-061, Contrato §Ejecución y consultas de cotización, Data model §Modelo vigente del delta]
- [x] CHK006 - ¿Los cambios de ruta, cardinalidad y estructura de respuesta identifican consumidores y condición de adopción conjunta sin alterar la navegación web ni añadir una descarga completa alternativa? [Compatibilidad, Spec §FR-039, Contrato §Compatibilidad, Plan §Coordinación contractual y orden técnico]
- [x] CHK007 - ¿El contrato del Top 3 expresa independencia de filtros, tratamiento de menos de tres clasificados y límites de consistencia entre peticiones independientes sin prometer un snapshot compartido no aprobado? [Consistencia, Spec §FR-039, Contrato §Podio, Data model §Modelo vigente del delta]

## Autenticación, autorización y errores

- [x] CHK008 - ¿Las obligaciones de JWT y autorización vigente están especificadas de forma uniforme para ejecución y consultas, sin introducir roles nuevos ni omitir controles por tratarse de lecturas? [Seguridad, Spec §FR-059, Contrato §Contrato vigente del delta, Constitution §III]
- [x] CHK009 - ¿Están definidos el formato seguro de error, los parámetros inválidos, el rechazo de autenticación y la precedencia de errores que permanece vigente tras retirar el rechazo por ejecución diaria previa? [Completitud, Spec §FR-060, Contrato §Ejecución y consultas de cotización, §GET /api/rankings/quote]
- [x] CHK010 - ¿Los requisitos de historial, vigente y fecha conservan sus ausencias y datos de auditoría, distinguiendo fecha diaria y fecha del score sin exponer detalles internos de fallos? [Consistencia, Spec §FR-024, §FR-041, §FR-062, Contrato §Ejecución y consultas de cotización]

## Notas

- Revisión humana: **PENDIENTE**; no hay evaluación ni aprobación automática de estos 10 ítems.
- Se revisa lo escrito, no respuestas de una implementación ni resultados de pruebas.
- `/speckit.implement` lee el estado de los checkboxes y no modifica marcadores. `requirements.md` conserva su ciclo separado de SPECIFY/CLARIFY.
- No se amplía el alcance a roles, controles de tasa, nuevos protocolos ni requisitos de seguridad hipotéticos. Registrar observaciones del revisor junto al ID afectado.

**Evaluación asistida solicitada por el usuario — 2026-11-10**: 9/10 criterios contemplados y marcados con las referencias de cada ítem; sustituye la nota anterior de ausencia de evaluación. No acredita implementación ni gates humanos.

**Pendiente CHK004**: identidad, orden y lista sin paginar están definidos; falta justificar o retirar la restricción del contrato a resúmenes finalizados, no acordada expresamente en FR-061/CLARIFY. Comparar «todos los resúmenes» con «todos los resúmenes finalizados». Mismo hallazgo que cotizacion.md CHK024 y data.md CHK004; no se solicita un endpoint ni mecanismo de recuperación adicional.

**Decisión posterior del usuario — 2026-11-10**: retirado el pendiente de visibilidad de corridas incompletas; no condiciona la ejecución diaria. Ítem tachado, no aprobado. No se elimina la auditoría ni se declara resuelta TD-024.

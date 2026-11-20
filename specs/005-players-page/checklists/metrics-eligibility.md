# Checklist de calidad de requisitos: catálogo con métricas — SPEC-005

**Propósito**: revisar únicamente el delta compartido con SPEC-002 y su efecto en la página y consumidores.
**Fecha**: 2026-11-11  
**Feature**: [SPEC-005](../spec.md), [SPEC-002](../../002-player-catalog/spec.md), [plan](../plan.md) y [contrato](../contracts/players-catalog.md).

**Propiedad de revisión**: pertenece al revisor. Una casilla `[x]` expresa que el revisor considera satisfecho el criterio de calidad de requisitos; no acredita implementación ni pruebas. Los ítems nuevos se generan sin marcar.

## Población y presentación

- [x] CHK001 ¿La población de la página remite sin contradicciones al criterio de fecha y fuentes de SPEC-002, sin imponer score ni cotización? [Consistencia, Spec §Revisión vigente; SPEC-002 §Revisión vigente]
- [x] CHK002 ¿Se distingue claramente la elegibilidad general de la ausencia parcial de partidos, goles o asistencias, incluido el significado de cero y «Sin datos»? [Claridad, Spec §Revisión vigente; Modelo §Modelo del delta]
- [x] CHK003 ¿Los requisitos de búsqueda, opciones globales, conteos y paginación describen el mismo universo elegible, sin depender de la página actual? [Completitud, Spec §Revisión vigente; Contrato §Delta de elegibilidad]
- [x] CHK004 ¿Está definido el resultado de un acceso directo al detalle no elegible y su relación con el tratamiento existente de jugador no encontrado? [Cobertura, Spec §Revisión vigente; Plan §Plan del delta]

## Integración y coherencia

- [x] CHK005 ¿Los requisitos de mercado y rankings concuerdan con SPEC-002 y distinguen exclusión actual de conservación de datos históricos? [Consistencia, Spec §Revisión vigente; SPEC-002 §Revisión vigente]
- [x] CHK006 ¿La conservación visible de tenencias e historial y la imposibilidad de comprar o vender mientras no sea elegible están especificadas sin depender del detalle filtrado? [Cobertura, Spec §Revisión vigente; Plan §Plan del delta]
- [x] CHK007 ¿Las afirmaciones históricas de visibilidad sin métricas y compatibilidad de contenido están identificadas como sustituidas para este delta, sin borrar su trazabilidad? [Consistencia, Spec §Revisión vigente; Modelo §Modelo del delta; SPEC-002 Contrato §Delta de elegibilidad]
- [x] CHK008 ¿Los criterios de aceptación permiten distinguir ausencia de métricas, cero, datos parciales y resultados globales, conservando autenticación y estados de vacío/error existentes? [Medibilidad, Spec §Revisión vigente y FR-023/025/028/034]

## Notas

Se combinan calidad general, UX y contrato/API en esta lista mínima para evitar duplicar los checklists históricos. La evaluación corresponde al revisor antes de TASKS. `/speckit.implement` no modifica los marcadores. `requirements.md` conserva su ciclo separado. No se modifica la aceptación histórica de los checklists anteriores.

Evaluación documental solicitada por el usuario: CHK001–008 cuentan con cobertura en las referencias indicadas. Estos marcadores no acreditan ejecución ni implementación. El punto de concurrencia específico se retiró de SPEC-002 CHK007 por decisión del usuario.

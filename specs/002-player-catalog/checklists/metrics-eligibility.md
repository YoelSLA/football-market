# Checklist de calidad de requisitos: elegibilidad por métricas — SPEC-002

**Propósito**: revisar únicamente el delta de elegibilidad y sus contratos y consumidores afectados.
**Fecha**: 2026-11-11  
**Feature**: [SPEC-002](../spec.md), coordinada con [SPEC-005](../../005-players-page/spec.md), [plan](../plan.md) y [contrato](../contracts/catalog-query.md).

**Propiedad de revisión**: pertenece al revisor. Una casilla `[x]` expresa que el revisor considera satisfecho el criterio de calidad de requisitos; no acredita implementación ni pruebas. Los ítems nuevos se generan sin marcar.

## Definición y cobertura

- [x] CHK001 ¿La elegibilidad define inequívocamente la fecha más reciente, las fuentes de esa fecha, la ausencia de registros, las filas vacías y el cero informado? [Claridad, Spec §Revisión vigente; Clarifications §Session 2026-11-11]
- [x] CHK002 ¿Queda explícito que catálogo no exige score ni cotización y que la elegibilidad es distinta de la disponibilidad de cada valor de tarjeta? [Completitud, Spec §Revisión vigente; SPEC-005 §Revisión vigente]
- [x] CHK003 ¿El mismo universo elegible está definido para listado, conteos y opciones globales antes de aplicar filtros y paginación, sin excluir criterios de visibilidad preexistentes? [Consistencia, Spec §Revisión vigente; Contrato §Delta de elegibilidad]
- [x] CHK004 ¿La respuesta para detalle no elegible distingue un ID válido de entradas inválidas y conserva las reglas de autenticación y visibilidad? [Cobertura, Spec §Revisión vigente; Contrato §Delta de elegibilidad]

## Consumidores y preservación

- [x] CHK005 ¿Los requisitos distinguen la exclusión del mercado y de ambos rankings de la conservación de cotizaciones y valoraciones históricas? [Completitud, Spec §Revisión vigente; Plan §Diseño y límites]
- [x] CHK006 ¿Están definidos el bloqueo de compras y ventas y la visibilidad de tenencias e historial existentes, sin eliminación de datos? [Cobertura, Spec §Revisión vigente; Clarifications §Session 2026-11-11]
- [x] CHK008 ¿Los cambios observables de población y las dependencias de detalle, estadísticas, mercado, rankings y portfolio están identificados sin afirmar compatibilidad idéntica de contenido? [Consistencia, Contrato §Delta de elegibilidad; Plan §Diseño y límites]

## Notas

Se combinan calidad general, contrato/API y seguridad en esta lista mínima porque el delta es una única regla transversal; no se añaden listas especializadas redundantes ni requisitos no acordados. La evaluación corresponde al revisor antes de TASKS. `/speckit.implement` no modifica los marcadores. `requirements.md` conserva su ciclo separado.

Evaluación documental solicitada por el usuario: CHK001–006 y CHK008 cuentan con cobertura en las referencias indicadas. CHK007 se retiró por decisión del usuario: la concurrencia específica entre actualización de métricas y operación de mercado no forma parte de este alcance. Los marcadores no acreditan ejecución ni implementación.

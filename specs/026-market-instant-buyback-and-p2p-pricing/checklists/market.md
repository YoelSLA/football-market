# Calidad de requisitos del mercado: Venta oficial y precio P2P

**Purpose**: Revisar claridad, cobertura y consistencia de los requisitos de venta oficial y precio P2P, sin ampliar el alcance.
**Created**: 2026-11-22
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado revisa la calidad de los requisitos; no verifica la implementación.
**Review Ownership**: Este checklist pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad del requisito está satisfecho.
**Marker Semantics**: `[x]` significa que se revisó y satisfizo la calidad del requisito; no significa que el trabajo de implementación esté completo.

## Completitud y alcance

- [x] CHK001 ¿Los criterios describen con claridad los resultados económicos de la venta oficial —transferencia de tokens, pago y registro— como una sola operación? [Completeness, Spec §FR-005]
- [x] CHK002 ¿Queda inequívoco cómo se determina la cantidad disponible para vender cuando existen tokens reservados en publicaciones activas? [Clarity, Spec §FR-004]
- [x] CHK003 ¿La especificación mantiene explícitamente fuera de alcance la interfaz frontend y los cambios a la compra oficial existente? [Scope, Spec §FR-014–FR-015]
- [x] CHK004 ¿Se distinguen claramente los rechazos de venta/publicación de una operación confirmada y de sus efectos sobre recursos y registros? [Consistency, Spec §Edge Cases]

## Claridad y criterios medibles

- [x] CHK005 ¿Las reglas de cantidad, descuento configurable y redondeo especifican límites inclusivos y el resultado de los valores fuera de rango? [Clarity, Spec §FR-002–FR-003, §FR-017–FR-018]
- [x] CHK006 ¿La banda P2P del 80% al 120%, el uso de la cotización al crear y el precio predeterminado quedan definidos sin ambigüedad? [Measurability, Spec §FR-008–FR-010]
- [x] CHK007 ¿Los requisitos distinguen el precio pactado al publicar del precio usado al comprar, incluso si cambia la cotización? [Consistency, Spec §FR-011–FR-012]
- [x] CHK008 ¿Los criterios de éxito permiten determinar objetivamente cuándo un rechazo deja intactos saldos, tenencias, reservas, publicaciones y registros? [Acceptance Criteria, Spec §SC-002, §SC-005]

## Contratos, integridad y escenarios

- [x] CHK009 ¿El comprobante exigido identifica todas las condiciones económicas y las partes indicadas en los requisitos? [Completeness, Spec §FR-006, §FR-016, §SC-006]
- [x] CHK010 ¿La especificación define el resultado para cotización ausente o no positiva en ambos flujos, sin introducir un valor sustituto? [Coverage, Spec §FR-013]
- [x] CHK011 ¿Los requisitos de autenticación y de prohibición de venta iniciada por el superusuario están claramente diferenciados? [Clarity, Spec §FR-001, §FR-007]
- [x] CHK012 ¿Los escenarios de aceptación cubren los flujos principales y los rechazos materiales descritos, sin exigir funcionalidades fuera del alcance aprobado? [Coverage, Spec §User Stories 1–3, §Out of Scope]

## Cambios de comisión y precio neto

- [x] CHK013 ¿La comisión se define consistentemente como porcentaje configurable de 10% a 99%, inclusive, con valor predeterminado de 10%, en los requisitos y el diseño? [Consistency, Spec §Clarifications, §FR-018, §Assumptions; Plan §Decisiones de diseño]
- [x] CHK014 ¿El rechazo sin efectos cuando el redondeo produce precio neto cero está expresado de forma coherente en el escenario de aceptación, el requisito funcional y el diseño? [Coverage, Spec §User Story 1, §FR-003, §Edge Cases; Plan §Decisiones de diseño]
- [x] CHK015 ¿El diseño de auditoría y configuración usa términos y límites compatibles con la comisión vigente, sin conservar referencias contradictorias a descuento o al rango 0%–100%? [Consistency, Spec §FR-006, §FR-018; Plan §Contexto técnico, §Decisiones de diseño, §Límites de alcance]

## Notes

- Los ítems evalúan la redacción y cobertura de requisitos; no son pruebas de código, procedimientos de QA ni autorización para ampliar alcance.
- La persona revisora confirmó que evaluó los criterios marcados `[x]`.
- `/speckit.implement` lee el estado de checklist como gate y no debe modificar sus marcadores.
- `checklists/requirements.md` tiene un ciclo separado, mantenido por `/speckit.specify` y `/speckit.clarify`.
- Este checklist evalúa la calidad de los requisitos; no crea ni sustituye tareas de `tasks.md`.

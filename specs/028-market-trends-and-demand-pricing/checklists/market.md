# Market Requirements Checklist: Métricas de tendencias y ajuste de oferta-demanda

**Purpose**: Revisar la calidad, claridad y cobertura de los requisitos de tendencias y recálculo de mercado.
**Created**: 2026-11-23
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado se genera mediante `/speckit.checklist` a partir del contexto y los requisitos de la feature.
**Review Ownership**: Este artefacto de revisión de calidad de requisitos pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio se revisó y satisface la calidad de requisitos; no significa que el trabajo de implementación esté completo.

## Requirement Completeness

- [x] CHK001 ¿Quedan especificados los datos, el orden y el límite de los jugadores incluidos en `topHotPlayers`, así como qué significa el volumen de compra? [Completeness, Spec §FR-002; contrato `market-trends-api.md`]
- [x] CHK002 ¿Están definidas sin solapamientos las reglas para clasificar cada transacción como compra o venta, incluida la liquidación P2P, y para atribuir la demanda al jugador? [Clarity, Spec §FR-003, Clarification 2026-11-23; contrato `market-demand-recalculation-api.md`]
- [x] CHK003 ¿Se distingue de forma completa la ausencia de transacciones de una ventana con actividad cuya demanda neta sea cero, tanto en tendencias como en ambos recálculos? [Coverage, Spec §FR-003, FR-007]

## Requirement Clarity

- [x] CHK004 ¿La ventana configurable, su valor predeterminado y el instante de referencia están definidos de forma inequívoca para consultas y recálculos? [Clarity, Spec §FR-003]
- [x] CHK005 ¿La definición de porcentajes y `marketBias` deja claro que se basa en el conteo de transacciones, mientras que el ajuste de cotización se basa en tokens netos? [Clarity, Spec §FR-002, FR-013]
- [x] CHK006 ¿La fórmula, el rango de saturación y la unidad de la demanda neta determinan un factor de mercado inequívoco, incluidos los valores límite? [Measurability, Spec §FR-004, FR-007]

## Requirement Consistency

- [x] CHK007 ¿Son consistentes entre spec, plan y contratos la fórmula combinada del recálculo habitual, el orden de aplicación de límite y precio mínimo, y el comportamiento cuando no hay score? [Consistency, Spec §FR-004, FR-007; Plan §Diseño y decisiones; contratos de recálculo]
- [x] CHK008 ¿Se mantiene consistente que el recálculo exclusivo de mercado procesa solo jugadores elegibles y no consulta ni aplica scores deportivos? [Consistency, Spec §FR-006, FR-010; contrato `market-demand-recalculation-api.md`]

## Acceptance Criteria Quality

- [x] CHK009 ¿Los criterios permiten determinar objetivamente qué resultados de cotización respetan el límite de variación vigente y el precio mínimo en ambos modos de recálculo? [Measurability, Spec §SC-004]
- [x] CHK010 ¿Los criterios de auditoría identifican con suficiente precisión el precio anterior y resultante, cada impacto aplicable, los límites y la ausencia de impacto deportivo en el modo exclusivo de mercado? [Completeness, Spec §FR-005, FR-008; Spec §SC-002–SC-003]

## Scenario and Edge Case Coverage

- [x] CHK011 ¿Están especificados los resultados para transacciones inexistentes, demanda neta cero, jugadores no elegibles y fallos individuales, sin dejar tratamientos incompatibles entre los recálculos? [Coverage, Spec §Edge Cases; contrato SPEC-016 `quotes-http.md`]
- [x] CHK012 ¿Está definido qué información y resultado de ejecución se exponen cuando el recálculo poblacional termina o encuentra un fallo, sin permitir cotizaciones con impactos parciales? [Exception Flow, Spec §FR-010; contratos `market-demand-recalculation-api.md` y SPEC-016 `quotes-http.md`]

## Non-Functional Requirements and Dependencies

- [x] CHK013 ¿Los requisitos de acceso identifican con claridad la autenticación y autorización aplicables a cada endpoint, sin implicar roles nuevos ni exponer métricas protegidas? [Completeness, Spec §FR-001, FR-006, FR-009]
- [x] CHK014 ¿Las dependencias funcionales de transacciones y recálculo existente, así como la exclusión de frontend y de cambios a transacciones confirmadas, están delimitadas de forma coherente? [Scope, Spec §FR-011–FR-012; Spec §Out of Scope]

## Notes

- Los ítems evalúan la calidad de lo especificado, no la implementación ni su funcionamiento.
- Marcar `[x]` solo tras la revisión del criterio por parte del revisor; los ítems nuevos quedan sin marcar.
- `/speckit.implement` lee el estado del checklist como gate y no debe modificar sus marcadores.
- `checklists/requirements.md` tiene un ciclo separado mantenido por `/speckit.specify` y `/speckit.clarify`.
- Registrar comentarios o hallazgos junto al ítem pertinente y enlazar recursos relevantes.

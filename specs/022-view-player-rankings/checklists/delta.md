# Requirements Delta Checklist: Visualizar ranking de jugadores (SPEC-022)

**Purpose**: Revisar la calidad de los requisitos nuevos sobre consultas paginadas, filtros remotos, podio independiente e identidad incluida en las respuestas.
**Created**: 2026-11-12
**Feature**: [spec.md](../spec.md)

**Note**: Checklist personalizado generado para los requisitos nuevos del delta vigente de SPEC-022.
**Review Ownership**: Este checklist es un artefacto de revisión de calidad de requisitos propiedad del revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Completitud de requisitos

- [x] CHK001 ¿Se especifica que ambos modos consultan el endpoint correspondiente y que la tabla envía al backend página, tamaño y filtros efectivos? [Completeness, Spec §FR-002, FR-005, FR-007]
- [x] CHK002 ¿Está definido que la tabla presenta solo la página recibida y que su total corresponde al conjunto filtrado, sin confundirlo con el total global? [Completeness, Spec §FR-007, revisión vigente §10]
- [x] CHK003 ¿Está especificado que el podio obtiene el Top 3 global con una solicitud independiente sin filtros, incluso cuando la tabla tiene filtros o está en otra página? [Completeness, Spec §FR-012]

## Claridad y criterios de aceptación

- [x] CHK004 ¿Están definidos los parámetros concretos de la consulta independiente del podio (`page=0&size=3`) y el comportamiento cuando hay menos de tres jugadores elegibles? [Clarity, Spec §FR-012]
- [x] CHK005 ¿Se aclara qué criterios permanecen al navegar páginas y que cambiar filtro, tamaño o modo reinicia la página a cero? [Clarity, Spec §FR-005, revisión vigente §8]

## Consistencia y cobertura de escenarios

- [x] CHK006 ¿Son consistentes las reglas de filtros solicitados al backend, respuesta tardía de una selección anterior y ausencia de filtrado/paginación cliente entre la especificación y el contrato de consumo? [Consistency, Spec §FR-005, contrato §1]
- [x] CHK007 ¿Se describe el alcance de la consistencia entre las respuestas separadas de tabla y podio, incluida la ausencia de garantía de instantánea común y la coincidencia esperada bajo datos estables? [Scenario Coverage, Spec §FR-012]
- [x] CHK008 ¿Se especifica que cada fila usa los datos `player` incluidos en su respuesta, sin enriquecimiento desde catálogo, y qué fallback corresponde a los datos opcionales ausentes? [Completeness, Spec §US1 escenario 2, §Edge Cases]

## Trazabilidad y alcance

- [x] CHK009 ¿Se conserva la responsabilidad de SPEC-018/SPEC-016 sobre los contratos backend y se limita SPEC-022 a consumirlos, sin introducir nuevas métricas ni filtros estadísticos? [Scope, Spec §revisión vigente §14, Out of Scope]

## Notes

- Marcar ítems `[x]` solo después de que el revisor confirme que el criterio de calidad de requisitos está satisfecho.
- Evaluación solicitada por el revisor: los nueve criterios están cubiertos por la revisión vigente de `spec.md` y `contracts/player-rankings-consumption.md`; su marcado no acredita implementación.
- `/speckit.implement` lee el estado de checklist como gate y no debe modificar sus marcadores.
- Añadir comentarios o hallazgos junto al ítem pertinente.

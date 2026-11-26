# Requisitos generales Checklist: Caché de consultas frecuentes del backend

**Purpose**: Auto-revisión ligera de la calidad y cobertura de los requisitos vigentes de SPEC-034.
**Created**: 2026-11-25
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado revisa requisitos, no el funcionamiento de la implementación.
**Review Ownership**: Este checklist es un artefacto de revisión de calidad de requisitos. Marcar `[x]` solo cuando quien revisa determine que el criterio está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio se revisó y satisface la calidad de requisitos; no significa que la implementación esté completa.

## Completitud y cobertura

- [x] CHK001 ¿Quedan identificadas sin omisiones las seis lecturas incluidas y distinguidas las exclusiones de alcance? [Completeness, Spec §FR-001, §FR-009–010]
- [x] CHK002 ¿Los requisitos cubren las entradas efectivas que determinan cada respuesta —filtros, orden, paginación, valores por defecto e ID de detalle—? [Coverage, Spec §FR-002]
- [x] CHK003 ¿Está especificado que las consultas P2P separen resultados por la identidad autenticada sin confiar en una identidad elegida por el cliente? [Completeness, Spec §FR-003]
- [x] CHK004 ¿Se definen duración y capacidad por tipo de consulta, incluido el límite de 10 segundos para P2P? [Completeness, Spec §FR-004]
- [x] CHK005 ¿Están cubiertas las invalidaciones pertinentes tras recálculo, sincronización y mutaciones exitosas, sin extender el alcance a endpoints transaccionales? [Coverage, Spec §FR-005–007]
- [x] CHK006 ¿Se especifica el resultado esperado ante ausencia, expiración o reinicio de caché y la preservación del contrato HTTP vigente? [Coverage, Spec §FR-008, §FR-010]

## Claridad y verificabilidad

- [x] CHK007 ¿Se distingue claramente entre vencimiento e invalidación de caché —el TTL solo permite reutilizar la respuesta durante un tiempo y una nueva consulta a PostgreSQL ocurre cuando llega otra solicitud tras vencer o invalidarse—, y se indica qué mutaciones exitosas de mercado invalidan cada tipo de resultado? [Clarity, Spec §FR-005–008]
- [x] CHK008 ¿Los criterios de éxito permiten determinar objetivamente la separación de claves, la vigencia/capacidad y la ausencia de resultados anteriores tras actualizaciones? [Measurability, Spec §SC-001–005]

## Notes

- Mantener todos los ítems sin marcar hasta la revisión del autor/revisor; la generación no constituye evaluación ni aceptación.
- `/speckit.implement` lee el estado de los checklists y no debe modificar sus marcadores.
- `checklists/requirements.md` tiene un ciclo de vida separado, mantenido por `/speckit.specify` y `/speckit.clarify`.

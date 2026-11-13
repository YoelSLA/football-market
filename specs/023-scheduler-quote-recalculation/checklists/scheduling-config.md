# Scheduling & Configuration Checklist: Programar recálculo automático de cotizaciones

**Purpose**: Evaluar la calidad de los requisitos de programación, zona horaria, configuración y reutilización de la infraestructura batch, sin evaluar su implementación.
**Created**: 2026-11-03
**Feature**: [spec.md](../spec.md)
**Depth**: Standard · **Audience**: Revisor (PR)

**Note**: Checklist especializado generado por `/speckit.checklist` a partir de los riesgos de configuración y evaluación temporal de la feature.
**Review Ownership**: Artefacto de revisión propiedad del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho en cuanto a calidad de requisitos; no significa que la implementación esté completa.

## Programación y zona horaria

- [x] CHK001 ¿Se especifican la cadencia por defecto, su carácter configurable y la zona UTC de evaluación como requisitos verificables? [Completeness, Spec §FR-001] — FR-001 y SC-001.
- [x] CHK002 ¿Se define el mecanismo y el formato de ajuste de la cadencia con el detalle suficiente para aceptarlo o rechazarlo objetivamente, incluido si el cambio exige reinicio? [Clarity, Spec §FR-001] — [contrato §1](contracts/scheduling-configuration.md): cron de 6 campos y reinicio requerido.
- [x] CHK003 ¿Se aclara que la evaluación en UTC es independiente de la zona del servidor y de la configuración de la aplicación, y se declara la ausencia de horario estacional? [Clarity, Completeness, Spec §FR-001, §Edge Cases] — [contrato §1](contracts/scheduling-configuration.md) y [research §9](../research.md).
- [x] CHK004 ¿Está definido el comportamiento esperado ante una expresión de cadencia inválida: rechazo al arrancar o degradación controlada? [Edge Case, Gap, Spec §FR-001] — [contrato §1](contracts/scheduling-configuration.md): impide el arranque.

## Configuración de la ejecución

- [x] CHK005 ¿Está definido el comportamiento ante una estrategia de valuación configurada fuera del catálogo? [Edge Case, Gap, Spec §FR-002] — [contrato §1](contracts/scheduling-configuration.md): falla el arranque.
- [x] CHK006 ¿Se especifica que la estrategia y los parámetros de la ejecución automática provienen de la misma configuración del mercado y coinciden con los de la ejecución manual? [Consistency, Spec §FR-002] — FR-002, [contrato §3](contracts/scheduling-configuration.md) y [research §4](../research.md).
- [x] CHK007 ¿Es verificable la prohibición de definir, duplicar o modificar fórmulas de valuación que fija FR-003? [Measurability, Spec §FR-003] — Verificable por el recorrido compartido y la ausencia de cambios en la lógica de valuación ([research §15–§16](../research.md)).
- [x] CHK008 ¿Se define el estado habilitado o deshabilitado de la programación como requisito funcional o como configuración operativa justificada? [Ambiguity, Spec §FR-001] — [research §4](../research.md) lo declara configuración operativa, no requisito funcional.

## Observabilidad y contrato operativo

- [x] CHK009 ¿Se especifican los eventos operativos mínimos y sus campos para disparo, omisión, rechazo y fallo, coherentes con FR-004, FR-006, FR-012 y FR-013? [Completeness, Consistency, Spec §FR-004, §FR-006, §FR-012, §FR-013] — [contrato §5](contracts/scheduling-configuration.md).
- [x] CHK010 ¿Se define que un fallo de una corrida no impide los disparos futuros? [Recovery, Gap, Spec §FR-001, §FR-013] — [contrato §2](contracts/scheduling-configuration.md): los errores no se propagan.
- [x] CHK011 ¿Se aclara que los registros operativos son evidencia para el operador y no forman parte del contrato HTTP ni del resultado expuesto de FR-010? [Clarity, Spec §FR-010] — [contrato §5](contracts/scheduling-configuration.md) y [data-model §5](../data-model.md).
- [x] CHK012 ¿Existe un criterio objetivo para comprobar que la infraestructura de programación queda reutilizable por otro proceso batch, sin implementar #41 ni añadir dependencias o componentes de coordinación no exigidos? [Measurability, Consistency, Spec §FR-011, §Assumptions] — [quickstart §8](../quickstart.md), [research §2](../research.md) y [contrato §2](contracts/scheduling-configuration.md).

## Notes

- Evaluación asistida solicitada por el revisor (2026-11-03): los ítems marcados se consideran cubiertos por los artefactos citados; los no marcados se reportan como no cubiertos.
- Marcar `[x]` solo tras revisar y confirmar que el criterio de calidad de requisitos se cumple.
- Los elementos se dejan sin marcar mientras requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado de los checkboxes como puerta y no debe modificar los marcadores.
- `checklists/requirements.md` tiene un ciclo de vida propio mantenido por `/speckit.specify` y `/speckit.clarify`.

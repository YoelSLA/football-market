# Registro de asignaciones explícitas de SPEC

Registro append-only de las asignaciones explícitas de identificador de SPEC.

Existe para satisfacer la trazabilidad exigida por la Constitución §8 y por
`docs/development/sdd-workflow.md` §5: la numeración automática es la predeterminada y toda
numeración explícita requiere una distribución previa documentada de funcionalidades entre
integrantes que trabajan en paralelo, junto con un **registro trazable del número, la
funcionalidad, el responsable y el motivo**, registrado antes de crear la SPEC.

Una fila se agrega al materializar una SPEC con identificador asignado explícitamente, antes de
cualquier escritura sobre el issue de origen. El registro no sustituye los artefactos de la SPEC,
sus gates SDD ni el estado de seguimiento en GitHub Issues y GitHub Projects.

## Convenciones

- **Identificador**: `SPEC-0XX`, con el mismo número que el de la carpeta de la spec.
- **Funcionalidad**: nombre de la funcionalidad asignada, tal como figura en el issue de origen.
- **Responsable**: responsable de la asignación, tomado de los asignados del issue de origen.
- **Issue de origen**: issue de GitHub que motiva la asignación.
- **Requisito**: clave `REQ-` rastreada en el issue de origen, cuando la correspondencia esté
  confirmada.
- **Rama**: rama asociada, con el formato `<type>/<spec-id>-<scope>`.
- **Fecha**: fecha de la asignación.

Las filas no se editan ni se eliminan. Una asignación que cambia de responsable o de funcionalidad
se corrige con una fila nueva que la referencie.

## Asignaciones

| Identificador | Funcionalidad | Responsable | Issue de origen | Requisito | Rama | Fecha |
| --- | --- | --- | --- | --- | --- | --- |
| SPEC-014 | Consultar portfolio personal | YoelSLA | #20 | REQ-S2-09 | feat/014-consultar-portfolio-personal | 2026-10-19 |
| SPEC-015 | Definir reglas contables de costo promedio y resultados financieros | YoelSLA | https://github.com/YoelSLA/football-market/issues/63 | REQ-S2-20 | feat/015-define-average-cost-and-financial-results-rules | 2026-10-19 |
| SPEC-016 | Calcular y persistir la cotización de tokens de jugadores con historial trazable (corrige la fila anterior del 2026-10-20, que quedaba limitada al disparo manual del recálculo) | Vaqui26 | https://github.com/YoelSLA/football-market/issues/39 | REQ-S2-11-A, REQ-S2-03, REQ-S2-04, REQ-S2-05, REQ-S2-08 | feat/016-ejecutar-recalculo-manual-de-cotizaciones | 2026-10-20 |
| SPEC-017 | Visualizar portfolio personal | YoelSLA | #26 | REQ-S2-15 | feat/017-view-personal-portfolio-ui | 2026-10-21 |

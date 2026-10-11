# Quality gates — autorizaciones y cierre

## Autoridad y evidencia

Constitución primero. Las autorizaciones humanas son **explícitas, específicas para la Feature y vinculadas al alcance revisado**. Un mensaje ambiguo («ok», «dale», «seguí») no sustituye un gate pendiente. Una tarjeta `IN PROGRESS` tampoco demuestra autorización. Registrar la decisión mediante `trello-traceability`, con identificador, tipo de gate, decisión, fecha y referencia verificable del alcance/resultado aprobado. Si no se puede verificar evidencia obligatoria, no declarar la transición completada.

## Gate READY

Antes de declarar READY:

- Feature identificada sin ambigüedad y Constitución consultada.
- `spec.md`, `plan.md`, `tasks.md` y artefactos de checklist pertinentes existen y son coherentes.
- Ambigüedades funcionales relevantes resueltas; criterios de aceptación cubiertos por tareas.
- ANALYZE completado y sin blockers pendientes; hallazgos no bloqueantes comunicados.
- No se introdujeron requisitos nuevos sin decisión humana.

Si no cumple, detener o regresar a la fase afectada, conforme a `recovery.md`.

## Gate IMPLEMENT — autorización previa

Presentar: Feature/identificador, alcance y US incluidas, resumen de plan, tareas/dependencias, resultado ANALYZE y riesgos relevantes. Solicitar confirmación explícita para **implementar la Feature completa**. Solo entonces permitir cambios de implementación dentro del alcance autorizado.

Ejemplo de confirmación suficiente: «Autorizo IMPLEMENT de la Feature 007 con el alcance presentado». La autorización puede recuperarse en otra sesión si hay evidencia persistida verificable y no fue invalidada por cambios materiales. No pedir autorizaciones por US. No inferir autorización por ejecutar un comando genérico de continuación.

## Gate CODE REVIEW — una aprobación integral

CONVERGE se ejecuta automáticamente al finalizar IMPLEMENT, **no** es un gate humano separado. Debe diagnosticar cobertura/consistencia sin ejecutar tests, ni directa ni indirectamente. Si encuentra problemas: detener y solicitar autorización para el plan de corrección, según `recovery.md`.

Con CONVERGE satisfactorio, entregar un reporte que incluya:

- Feature, alcance y US implementadas; relación con criterios de aceptación.
- Cambios de código y archivos relevantes; decisiones y riesgos técnicos.
- Tests escritos o modificados, qué validan, qué queda sin verificar y comandos **para que el usuario los ejecute**.
- Evidencia de CONVERGE y cualquier limitación conocida.
- Solicitud de revisión y confirmación humana de resultados de tests.

El agente **nunca ejecuta tests**, ni invoca herramientas o comandos que los ejecuten indirectamente. No presentar tests como pasados si no hay resultados aportados por el usuario.

## Gate DONE — condiciones acumulativas

Solo declarar DONE si:

1. Todas las US y tareas obligatorias están implementadas con evidencia consistente.
2. CONVERGE satisfactorio y vigente respecto del código y artefactos revisados.
3. Reporte integral disponible; usuario revisó la implementación.
4. Usuario confirmó ejecución y resultados satisfactorios de los tests requeridos, o aprobó expresamente una excepción justificada que permita la Constitución.
5. Usuario aprobó explícitamente **el Code Review integral de la Feature**.
6. Evidencia de aprobación registrada y transiciones Trello verificadas.

Una aprobación integral cumple también la autorización de cierre; no solicitar una tercera aprobación. Si hay cambios relevantes tras la revisión, invalidar el resultado afectado, actualizar CONVERGE/reporte y solicitar nueva aprobación. Cambios editoriales inocuos no invalidan automáticamente un gate.

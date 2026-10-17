# Recovery — bloqueos, cambios y reanudación

## Regla de recuperación

**Revalidar por impacto, no reiniciar la Feature completa.** Conservar artefactos, código, tareas, US y aprobaciones que sigan siendo válidos.

Nunca ocultar blockers, fabricar evidencias, ejecutar tests ni aplicar correcciones sin autorización.

## Clasificación y respuesta

| Situación | Respuesta |
| --- | --- |
| Ambigüedad funcional | Detener; explicar alternativas e impacto; esperar decisión; actualizar spec y derivados afectados. |
| Cambio material de alcance o arquitectura | Consultar antes de actuar; revisar artefactos y vigencia de la autorización. |
| Blocker de ANALYZE | Detener; citar hallazgo, ubicación, severidad e impacto; proponer corrección; esperar decisión. |
| CONVERGE con remediaciones | El comando ya anexó las tareas; **no implementarlas**. Presentar plan acotado y esperar autorización específica. |
| CODE REVIEW rechazado | Registrar `REJECTED`; volver a implementación de lo afectado; repetir CODE REVIEW y, luego, TESTING. |
| TESTING fallido con cambio de código | US a `IN PROGRESS`; corregir; recorrer CODE REVIEW y TESTING de nuevo. |
| Bloqueo externo sin cambios de código | La US permanece en su columna de gate con `Status: BLOCKED` y `Reason`. |
| Operación Spec Kit fallida o ausente | No simular éxito; informar qué falta y pedir resolución o alternativa aceptada. |
| Trello no sincroniza | Verificar si la operación ocurrió; reintentar solo si es seguro e idempotente; no declarar transición registrada. |
| Evidencia de autorización ausente o inválida | Detener las acciones que dependan del gate y solicitar autorización contextual. |
| Conflicto entre fuentes obligatorias | Detener la transición, citar fuentes y aplicar Constitución §9. No inventar excepciones. |

## Protocolo de CONVERGE controlado

CONVERGE es append-only. Distinguir con precisión qué hace el comando y qué ocurre después:

**Dentro del comando**, sin autorización adicional:

- diagnosticar cobertura y consistencia;
- anexar a `tasks.md` las tareas de remediación, con referencia a su origen.

**Fuera del comando**, y solo con autorización:

1. Conservar la salida de CONVERGE y **separar problemas confirmados de hipótesis**.
2. Para cada problema confirmado: requisito o criterio, US, archivos o tareas, severidad, corrección propuesta y efecto sobre otras US.
3. Presentar el conjunto de correcciones y **esperar autorización humana**. No modificar código, spec, plan o tasks antes de recibirla.
4. Con la autorización, reabrir en `IN PROGRESS` solo las US afectadas y la Feature cuando corresponda. Mantener las demás US donde estaban, pendientes de revisión global.
5. Crear o reabrir tareas en `tasks.md` **sin inventar comportamiento**. Si el comportamiento no está definido por la spec, es STOP con decisión humana upstream: actualizar artefactos afectados, ANALYZE y READY, y solicitar nueva autorización cuando el alcance autorizado deje de cubrir el trabajo.
6. Aplicar los cambios autorizados, actualizar trazabilidad y **volver a ejecutar CONVERGE** al completar de nuevo la implementación. Repetir hasta resultado satisfactorio o nuevo bloqueo.

No modificar product code, tests ni documentación desde CONVERGE. Una corrección documental mecánica inequívoca MAY ocurrir **fuera** del comando, en operación separada, seguida de nueva ejecución; no repetir gates no afectados.

Toda modificación relevante invalida la revisión final anterior: preparar reporte actualizado y obtener aprobación integral nuevamente.

## Reconstrucción entre sesiones

1. Identificar la Feature inequívocamente desde solicitud, rama y artefactos. Si hay varias candidatas, consultar.
2. Verificar Constitución, workflow aplicable, artefactos SDD, implementación real, tareas pendientes, evidencia de gates y estados de Trello.
3. **Distinguir hechos comprobados de estados declarados:** una casilla marcada o una tarjeta movida no prueba implementación.
4. Evaluar vigencia de resultados de ANALYZE, CONVERGE y autorizaciones respecto de los cambios posteriores. No repetir una operación cuyo resultado verificable sigue vigente; no reutilizar resultados obsoletos.
5. Si todo concuerda, retomar desde el siguiente paso permitido **sin volver a pedir autorizaciones válidas**.
6. Si hay discrepancias materiales o evidencia insuficiente, detenerse y presentar diagnóstico con pregunta concreta.

## Verificación y entrega

El agente no ejecuta tests ni controles adicionales, directa o indirectamente.

Antes de entregar: revisar el diff final, `git diff --check` y `git status --short`. Distinguir fallos nuevos, fallos preexistentes demostrablemente ajenos y problemas de entorno. Si la operación dispara tests o controles no permitidos, entregar el cambio al usuario para que lo ejecute. No afirmar publicación ni pruebas no observadas.

## Formato mínimo de bloqueo

Comunicar:

- Feature y fase;
- qué falló y con qué evidencia;
- alcance y US afectadas;
- qué permanece válido;
- propuesta y riesgos;
- autorización o decisión exacta requerida;
- estado actual de Trello y siguiente paso tras la respuesta.

No transformar una consulta de diagnóstico en una autorización de corrección.
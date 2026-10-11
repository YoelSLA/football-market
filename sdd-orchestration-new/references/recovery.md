# Recovery — bloqueos, cambios y reanudación

## Regla de recuperación

**Revalidar por impacto, no reiniciar toda la Feature.** Conservar artefactos, código, tareas y US que sigan siendo válidos. Nunca ocultar blockers, fabricar evidencias ni ejecutar tests. No aplicar correcciones de CONVERGE sin autorización humana específica.

## Clasificación y respuesta

| Situación | Respuesta |
| --- | --- |
| Ambigüedad funcional | Detener; explicar alternativas e impacto; esperar decisión; actualizar spec y derivados afectados. |
| Cambio material de alcance/arquitectura | Consultar antes de actuar; revisar artefactos y vigencia de autorización IMPLEMENT. |
| Blocker de ANALYZE | Detener; citar hallazgo, ubicación, severidad e impacto; proponer corrección; esperar decisión antes de corregir. |
| CONVERGE con pendientes | Detener; identificar US/tareas/criterios afectados; presentar plan acotado; esperar autorización específica. |
| CODE REVIEW rechazado | Registrar observaciones; volver a implementación de partes afectadas; repetir validaciones relevantes y pedir nueva aprobación integral. |
| Operación Spec Kit fallida/ausente | No simular éxito; informar qué falta y pedir resolución o alternativa explícitamente aceptada. |
| Trello no sincroniza | Verificar si operación ocurrió; reintentar solo si es seguro e idempotente; no declarar transición registrada sin evidencia. |
| Evidencia de autorización ausente/inválida | Detener acciones que dependan del gate y solicitar autorización contextual. |

## Protocolo de CONVERGE controlado

1. Conservar salida de CONVERGE y separar problemas confirmados de hipótesis.
2. Para cada problema: requisito/criterio, US, archivos/tareas, severidad, corrección propuesta y efecto sobre otras US.
3. Presentar el conjunto de correcciones y **esperar autorización humana**. No modificar código, spec, plan o tasks en respuesta a esos problemas antes de recibirla.
4. Tras autorización, reabrir en `IN PROGRESS` solo US afectadas y Feature cuando corresponda. Mantener las demás US en `CODE REVIEW` como pendientes de revisión global.
5. Aplicar cambios autorizados, actualizar trazabilidad y ejecutar CONVERGE automáticamente al completar nuevamente IMPLEMENT; repetir hasta resultado satisfactorio o nuevo bloqueo.
6. Toda modificación relevante invalida la revisión final anterior; preparar reporte actualizado y obtener aprobación integral nuevamente.

Si el problema implica cambiar requisitos, volver a la fase SDD afectada y pedir nueva autorización de IMPLEMENT cuando el alcance previamente autorizado deje de cubrir el trabajo.

## Reconstrucción automática entre sesiones

1. Identificar Feature inequívocamente desde solicitud, rama y artefactos. Si hay varias candidatas, consultar.
2. Verificar Constitución, artefactos SDD, implementación real, tareas pendientes, evidencia de gates y estados Trello.
3. Distinguir **hechos comprobados** de estados declarados: una casilla marcada o tarjeta movida no basta para probar implementación.
4. Evaluar vigencia de resultados de ANALYZE, CONVERGE y autorizaciones respecto de cambios posteriores. No repetir operaciones si el resultado verificable sigue vigente; no reutilizar resultados obsoletos.
5. Si todo concuerda, retomar desde el siguiente paso permitido sin volver a pedir autorizaciones válidas. Si hay discrepancias materiales o evidencia insuficiente, detenerse y presentar diagnóstico y pregunta concreta.

## Formato mínimo de bloqueo

Comunicar: Feature y fase; qué falló y evidencia; alcance/US afectadas; qué permanece válido; propuesta y riesgos; autorización o decisión exacta requerida; estado actual de Trello y siguiente paso tras la respuesta. No transformar una consulta de diagnóstico en una autorización de corrección.

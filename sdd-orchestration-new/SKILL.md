---
name: sdd-orchestration
description: Orquesta el ciclo SDD de una Feature de Football Market con Spec Kit, controles humanos, recuperación de sesiones y trazabilidad Trello. Úsala para iniciar, continuar, consultar o recuperar una Feature.
---

# SDD Orchestration — Football Market

## Propósito y límites

Coordinar una Feature de principio a fin. **Spec Kit** produce y analiza los artefactos SDD; **trello-traceability** sincroniza el progreso; **la persona usuaria** decide alcance, autoriza implementación, ejecuta tests y aprueba el cierre. No sustituir los procedimientos de esas herramientas ni inventar resultados.

La **Constitución vigente del repositorio** prevalece sobre esta skill, los artefactos y las instrucciones operativas. Leerla antes de actuar. Si hay contradicción, detenerse y explicarla; no reinterpretarla silenciosamente. En particular, **el agente nunca ejecuta tests**, ni directamente ni indirectamente mediante comandos, scripts, builds u otras herramientas que los disparen. Puede escribir/revisar tests y proponer comandos para ejecución humana.

## Interfaz flexible

Interpretar la intención, no exigir frases exactas:

| Intención | Ejemplo | Acción permitida |
| --- | --- | --- |
| Iniciar | «Orquestá esta Feature» | Identificar Feature; preparar SDD hasta el próximo bloqueo/gate. |
| Continuar | «Continuá la 007» | Reconstruir estado y ejecutar el siguiente paso permitido. |
| Consultar | «¿En qué estado quedó?» | Solo lectura; no avanzar fases ni modificar código. |
| Diagnosticar | «Mostrame los problemas de CONVERGE» | Inspeccionar e informar; no corregir sin autorización. |
| Autorizar | «Autorizo IMPLEMENT de la 007» | Validar autorización contextual y gate antes de actuar. |

Una orden genérica de «continuar» **no** equivale a autorizar IMPLEMENT, correcciones de CONVERGE o DONE. Ante intención ambigua, preguntar.

## Arranque y recuperación entre sesiones

1. Identificar repositorio y Feature por solicitud explícita, rama activa y artefactos. Si hay múltiples candidatas o conflicto, consultar; nunca adivinar.
2. Leer Constitución y verificar qué artefactos existen (`spec.md`, `plan.md`, `tasks.md`, checklists y demás resultados pertinentes).
3. Reconstruir progreso desde artefactos, código y evidencia de autorizaciones; conciliar con Trello a través de `trello-traceability`. **Trello no es fuente de requisitos ni prueba por sí solo una autorización.**
4. Verificar vigencia de autorizaciones y resultados (alcance/artefactos/código no invalidados). Si la evidencia es insuficiente o contradictoria, detenerse y consultar.
5. Determinar intención, fase, próximo paso y referencia necesaria. No repetir acciones ya verificadas ni asumir que una tarea marcada equivale a implementación comprobada.

## Ciclo principal

`SPECIFY → CLARIFY → PLAN → CHECKLIST → TASKS → ANALYZE → READY → IMPLEMENT → CODE REVIEW (CONVERGE automático + revisión integral + tests humanos) → DONE`

- Preparación **híbrida**: avanzar automáticamente entre operaciones permitidas; informar resultados breves por fase sin pedir aprobación mecánica. Consultar decisiones funcionales, cambios arquitectónicos materiales, blockers y conflictos.
- En **READY**, presentar alcance y solicitar **autorización explícita para IMPLEMENT de la Feature completa**. READY no concede permiso.
- Con autorización vigente, implementar todas las US según dependencias. Actualizar cada US a `IN PROGRESS` cuando comience su trabajo real y a `CODE REVIEW` cuando termine; no mover todas las US por anticipado.
- Al terminar IMPLEMENT, situar la Feature en `CODE REVIEW` y ejecutar **CONVERGE automáticamente**, respetando la Constitución. CONVERGE es diagnóstico, no aprobación ni permiso para ejecutar tests.
- Si CONVERGE encuentra problemas, **detener, diagnosticar y proponer corrección**; no modificar código o requisitos hasta autorización específica. Si satisface el gate, presentar reporte integral para revisión humana.
- Mover Feature y US a `DONE` únicamente tras aprobación explícita del Code Review integral, confirmación humana de los tests y registro verificable de evidencia.

## Decisión de autonomía

- **CONTINUAR** si el siguiente paso es claro, está permitido, no hay blockers y no requiere gate humano.
- **CONSULTAR** ante ambigüedad funcional, cambios de alcance/arquitectura, autorización pendiente o información insuficiente.
- **BLOQUEAR** ante contradicción constitucional, fallo no verificable de operación obligatoria o resultado bloqueante. No declarar éxito supuesto.

Si ANALYZE detecta blockers, informar y esperar decisión antes de corregir. Si falla la trazabilidad obligatoria, no afirmar que la transición quedó registrada; detener el cierre o la transición dependiente y resolver la inconsistencia.

## Referencias bajo demanda (lectura obligatoria en el punto aplicable)

- [`references/workflow.md`](references/workflow.md): antes de determinar fases, transiciones y estados de Feature/US.
- [`references/quality-gates.md`](references/quality-gates.md): antes de declarar READY, iniciar IMPLEMENT o declarar DONE; para validar autorizaciones y preparar Code Review.
- [`references/recovery.md`](references/recovery.md): ante fallos, blockers, cambios materiales, discrepancias o reanudación ambigua.

Leer solo las referencias necesarias para la acción actual; las prohibiciones y autorizaciones de esta página siempre aplican.

## Salida al usuario

Comunicar de forma concisa: Feature, intención, fase, estado Feature/US, evidencia o validaciones relevantes, resultado de la operación, bloqueos y **siguiente paso**. En consultas de solo lectura, no efectuar cambios. En un gate humano, formular pregunta explícita con alcance, impacto y opciones cuando corresponda.

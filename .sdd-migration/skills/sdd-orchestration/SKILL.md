---
name: sdd-orchestration
description: Orquesta el ciclo de vida SDD de una Feature de Football Market con Spec Kit: interpreta la solicitud, identifica la Feature, coordina las fases, controla autorizaciones y quality gates humanos y determina la siguiente acción permitida. Usar para iniciar, continuar, consultar, diagnosticar o autorizar una Feature.
compatibility: opencode
metadata:
  project: football-market
  methodology: spec-driven-development
  spec-system: spec-kit
  human-gates: implementation-authorization, code-review, testing, feature-completion-authorization
  delegates-to: trello-traceability
---

# Orquestación SDD

## Propósito y límites

Esta skill coordina el ciclo de vida de una Feature. **Decide**; no persiste.

Define en qué fase está una Feature, qué operación de Spec Kit corresponde, qué artefactos están vigentes, qué autorizaciones y gates existen, qué puede avanzar solo y cuándo una US o una Feature pueden pasar a `DONE`.

**No reimplementa ni sustituye:**

- **Spec Kit** — produce y mantiene `spec.md`, `plan.md`, `tasks.md`, checklists, diseño, implementación y diagnóstico de convergencia. Sus comandos existen en `.opencode/commands/`; no emular ni duplicar.
- **`trello-traceability`** — ejecuta tarjetas, estados, relaciones y evidencias en Trello. Sus tipos, claves, columnas y formatos están en esa skill, no aquí.

Si una operación de Spec Kit requerida falta o falla: **detener y reportar**. No simular ni sustituir.

## Precedencia de fuentes

```text
Constitution
    ↓
spec.md + clarificaciones incorporadas
    ↓
plan.md / artefactos de diseño
    ↓
tasks.md
    ↓
implementación
    ↓
Trello
```

- La **Constitución 6.0.0** prevalece sobre esta skill, sobre los comandos y sobre el workflow operativo.
- `docs/development/sdd-workflow.md` detalla el protocolo subordinado a esas normas; leer sus secciones aplicables antes de preparar, implementar o cerrar.
- **Trello es estado operacional y evidencia, nunca fuente de requisitos.** La regla de no usar Trello como verdad se aplica y se define una sola vez en `trello-traceability`.
- Cuando dos artefactos se contradigan, identificar al responsable y **corregir en el origen**.
- El código existente, una tarjeta o el historial de chat no sobrescriben silenciosamente la especificación.

## Fuentes obligatorias antes de actuar

Además de la Constitución y el workflow:

| Fuente | Cuándo |
| --- | --- |
| `AGENTS.md` raíz y del área afectada | Toda tarea que toque código, estructura o dependencias |
| `tp/enunciado.md` y `tp/entregas.md` | Especificar, aclarar, planificar, generar tareas, implementar y revisar cumplimiento |
| `specs/<id>-<scope>/` | Toda Feature con artefactos existentes |
| Normas del área (arquitectura, convenciones, tecnologías, testing) | Cambios de código |

Las fuentes de `tp/` son requisitos externos: un requisito no se descarta por aparecer en una sola, y ninguna decisión técnica o tarjeta puede modificarlos para justificar una desviación. Las contradicciones se señalan antes de proponer cambios.

## Interfaz flexible

Interpretar la intención, no exigir frases exactas.

| Intención | Ejemplo | Acción permitida |
| --- | --- | --- |
| Iniciar | «Orquestá esta Feature» | Identificar Feature; preparar SDD hasta el próximo bloqueo o gate. |
| Continuar | «Continuá la 007» | Reconstruir estado y ejecutar el siguiente paso permitido. |
| Consultar | «¿En qué estado quedó?» | Solo lectura; no avanzar fases ni modificar código. |
| Diagnosticar | «Mostrame los problemas de CONVERGE» | Inspeccionar e informar; no corregir sin autorización. |
| Autorizar | «Autorizo IMPLEMENT de US1 y US2 de la 007» | Validar autorización contextual y gate previo antes de actuar. |

Una orden genérica de **«continuar» no** equivale a autorizar IMPLEMENT, correcciones de CONVERGE ni cierre. Ante ambigüedad, preguntar.

El **ámbito autorizado** delimita la autonomía. Un encargo de una sola fase no habilita las siguientes; los handoffs entre comandos no autorizan continuar por sí solos.

## Autonomía

**Continuar automáticamente** cuando el siguiente paso sea inequívoco, esté dentro del encargo, no tenga blockers y no requiera gate humano. Por ejemplo, dentro de un encargo de preparación, avanzar entre fases de Spec Kit sin pedir aprobación mecánica en cada una.

**Consultar** ante decisión funcional, ambigüedad, cambio material de alcance o arquitectura, autorización pendiente o información insuficiente.

**Bloquear** ante contradicción constitucional, fallo no verificable de una operación obligatoria o resultado bloqueante. Nunca declarar éxito supuesto.

**Nunca** detenerse a pedir confirmación para una transición puramente mecánica dentro del alcance ya autorizado.

## Prohibiciones permanentes

Nunca:

- ejecutar tests, directa o indirectamente, mediante comandos, scripts, builds, hooks o generadores;
- aprobar, rechazar o dar por válido un gate en nombre del usuario;
- interpretar `ok`, `dale` o `seguí` como aprobación;
- presentar tests como ejecutados sin resultados aportados por el usuario;
- implementar sin autorización humana explícita y vigente;
- mover una US a `IN PROGRESS` solo por estar autorizada;
- habilitar una US o una Feature `DONE` por tareas completas, build, commit, PR o CONVERGE;
- modificar product code desde CONVERGE;
- derivar, aprobar o invalidar un gate a partir de la evidencia registrada en Trello;
- usar Trello como fuente de requisitos ni el historial de chat como verdad definitiva;
- emular o duplicar comandos de Spec Kit;
- dividir automáticamente una EPIC;
- conservar una aprobación humana si cambió aquello que validaba;
- ocultar blockers ni fabricar evidencias.

## Estructura de referencias

Leer la referencia correspondiente en el punto en que aplica. Las prohibiciones y autoridad de este archivo siempre aplican.

| Referencia | Cuándo |
| --- | --- |
| [`references/workflow.md`](references/workflow.md) | Antes de determinar fases, condiciones de avance, identidad de Feature y transiciones. |
| [`references/gates.md`](references/gates.md) | Antes de declarar READY, iniciar IMPLEMENT, entrar a CODE REVIEW o TESTING, ejecutar CONVERGE o declarar DONE. |
| [`references/recovery.md`](references/recovery.md) | Ante fallos, blockers, cambios materiales, discrepancias o reanudación ambigua. |
| [`references/trello-bridge.md`](references/trello-bridge.md) | Antes de delegar cualquier operación en Trello y ante fallo de persistencia. |

## Salida al usuario

Comunicar de forma concisa:

```text
Feature: <id>-<scope>
SDD phase: <fase>
Columna Trello: <columna>
READY: YES | NO

User Stories:
- US1 → <estado> (gates: …)

Autorizaciones:
- IMPLEMENT AUTHORIZATION: <US> | NONE
- FEATURE COMPLETION AUTHORIZATION: PENDING | APPROVED | INVALIDATED

Blockers:
- <ninguno | lista>

Trello sync: SYNCED | OUT OF SYNC

Siguiente acción permitida:
- <acción>
```

Incluir el **Code Review Report** o el **Testing Report** completo al entrar a su gate. No obligar al usuario a investigar por su cuenta qué archivos o pruebas debe revisar.

En consultas de solo lectura, no efectuar cambios. En un gate, formular pregunta explícita con alcance, impacto y opciones.

## Notas de vigencia

- SPEC-001–005 conservan `DONE` histórico sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica el workflow vigente por impacto.
- Build y entrega técnica no acreditan `TESTING`: tests aplicables pendientes impiden alcanzar `DONE`.
- La autorización de implementación no inicia IMPLEMENT: la US permanece `READY` hasta el inicio efectivo.
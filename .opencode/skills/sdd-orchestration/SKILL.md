---
name: sdd-orchestration
description: Orquesta el ciclo de vida SDD de una Feature de Football Market con Spec Kit: interpreta la solicitud, identifica la Feature, coordina las fases, controla autorizaciones humanas y determina la siguiente acción permitida. Usar para iniciar, continuar, consultar, diagnosticar o autorizar una Feature.
compatibility: opencode
metadata:
  project: football-market
  methodology: spec-driven-development
  spec-system: spec-kit
  tracker: github-issues-projects
  human-authorizations: implement-authorization, code-review, testing, feature-completion-authorization
---

# Orquestación SDD

## Propósito y límites

Esta skill coordina el ciclo de vida de una Feature. **Decide**; no persiste.

Define en qué fase está una Feature, qué operación de Spec Kit corresponde, qué artefactos están vigentes, qué autorizaciones existen, qué puede avanzar solo y cuándo una funcionalidad puede cerrarse.

**No reimplementa ni sustituye:**

- **Spec Kit** — produce y mantiene `spec.md`, `plan.md`, `tasks.md`, checklists, diseño, implementación y diagnóstico de convergencia. Sus comandos existen en `.opencode/commands/`; no emular ni duplicar.
- **GitHub Issues y GitHub Projects** — registran el estado operativo de seguimiento. Sus estados están definidos en el workflow operativo; no se inventan otros.

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
seguimiento operativo (GitHub Issues / Projects)
```

- La **Constitución** prevalece sobre esta skill, sobre los comandos y sobre el workflow operativo.
- `docs/development/sdd-workflow.md` detalla el protocolo subordinado a esas normas; leer sus secciones aplicables antes de preparar, implementar o cerrar.
- **El seguimiento operativo es estado y contexto, nunca fuente de requisitos.** Un comentario, una etiqueta o una columna no reemplaza `spec.md`, `plan.md`, `tasks.md` ni la documentación técnica. Si contradicen un artefacto, prevalece el artefacto.
- Conforme al principio de trazabilidad y fuente de verdad, los artefactos SDD son la fuente de verdad del **estado SDD** y GitHub Issues y Projects son la fuente de verdad del **estado operativo**. Cada una gobierna su ámbito; **no hay precedencia universal** entre ambas, y una discrepancia es una inconsistencia que se corrige en el origen que la evidencia contradiga.
- **El sistema de seguimiento es tablero operativo, no documento técnico.** Al actualizar, entregar solo lo accionable: entidad, estado, bloqueo vigente y próxima acción. El detalle técnico, los escenarios y el análisis permanecen en los artefactos SDD. No duplicar en el seguimiento lo que ya está en `tasks.md`.
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

Las fuentes de `tp/` son requisitos externos: un requisito no se descarta por aparecer en una sola, y ninguna decisión técnica o registro de seguimiento puede modificarlos para justificar una desviación. Las contradicciones se señalan antes de proponer cambios.

## Interfaz flexible

Interpretar la intención, no exigir frases exactas.

| Intención | Ejemplo | Acción permitida |
| --- | --- | --- |
| Iniciar | «Orquestá esta Feature» | Identificar Feature; preparar SDD hasta el próximo bloqueo o autorización. |
| Continuar | «Continuá la 007» | Reconstruir estado y ejecutar el siguiente paso permitido. |
| Consultar | «¿En qué estado quedó?» | Solo lectura; no avanzar fases ni modificar código. |
| Diagnosticar | «Mostrame los problemas de CONVERGE» | Inspeccionar e informar; no corregir sin autorización. |
| Autorizar | «Autorizo IMPLEMENT de la 007» | Validar autorización contextual y previa antes de actuar. |

Una orden genérica de **«continuar» no** equivale a autorizar IMPLEMENT, correcciones de CONVERGE ni cierre. Ante ambigüedad, preguntar.

El **ámbito autorizado** delimita la autonomía. Un encargo de una sola fase no habilita las siguientes; los handoffs entre comandos no autorizan continuar por sí solos.

## Autonomía

**Continuar automáticamente** cuando el siguiente paso sea inequívoco, esté dentro del encargo, no tenga blockers y no requiera autorización humana. Por ejemplo, dentro de un encargo de preparación, avanzar entre fases de Spec Kit sin pedir aprobación mecánica en cada una.

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
- habilitar una funcionalidad `Done` por tareas completas, build, commit, PR o CONVERGE;
- modificar product code desde CONVERGE;
- derivar, aprobar o invalidar un gate a partir de la evidencia registrada en el seguimiento;
- usar el seguimiento como fuente de requisitos ni el historial de chat como verdad definitiva;
- inventar hashes, resultados de pruebas ni aprobaciones;
- emular o duplicar comandos de Spec Kit;
- conservar una aprobación humana si cambió aquello que validaba;
- ocultar blockers ni fabricar evidencias.

## Estructura de referencias

Leer la referencia correspondiente en el punto en que aplica. Las prohibiciones y autoridad de este archivo siempre aplican.

| Referencia | Cuándo |
| --- | --- |
| [`references/workflow.md`](references/workflow.md) | Antes de determinar fases, condiciones de avance, identidad de Feature y transiciones. |
| [`references/gates.md`](references/gates.md) | Antes de solicitar o declarar una autorización: IMPLEMENT, CODE REVIEW, TESTING o cierre. |
| [`references/recovery.md`](references/recovery.md) | Ante fallos, blockers, cambios materiales, discrepancias o reanudación ambigua. |

## Salida al usuario

Comunicar de forma concisa:

```text
Feature: <id>-<scope>
SDD phase: <fase>
Status de spec: <Status de spec.md>
Estado de seguimiento: <Backlog | In Progress | In Review | Done>

Autorizaciones:
- IMPLEMENT AUTHORIZATION: <alcance> | NONE
- CODE REVIEW: PENDING | APPROVED | INVALIDATED
- TESTING: PENDING | APPROVED | N/A VALIDATED | INVALIDATED
- FEATURE COMPLETION AUTHORIZATION: PENDING | APPROVED | INVALIDATED

Blockers:
- <ninguno | lista>

Seguimiento: SYNCED | OUT OF SYNC | PENDING

Siguiente acción permitida:
- <acción>
```

Incluir el **Code Review Report** o el **Testing Report** completo al entrar a su autorización. No obligar al usuario a investigar por su cuenta qué archivos o pruebas debe revisar.

En consultas de solo lectura, no efectuar cambios. En una autorización, formular pregunta explícita con alcance, impacto y opciones.

## Notas de vigencia

- Las User Stories se verifican en conjunto, con cobertura trazable desde `spec.md` hacia `tasks.md`. **No** son unidades de autorización ni requieren seguimiento individual.
- **No existe un gate READY.** La condición que READY evaluaba —preparación global válida y ANALYZE vigente sin blockers— es la que ANALYZE determina, conforme a `references/workflow.md`.
- Build y entrega técnica no acreditan `TESTING`: los tests aplicables pendientes impiden el cierre.
- La autorización de implementación no inicia IMPLEMENT: habilita el trabajo, que comienza cuando la operación autorizada empieza realmente.
- SPEC-001–005 conservan su cierre histórico sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica el workflow vigente por impacto.
- El tablero anterior fue retirado como sistema de seguimiento. Hasta que GitHub Projects esté configurado y las funcionalidades migradas, no se afirma sincronización de ninguna funcionalidad y no se declaran funcionalidades migradas sin verificarlas. La evidencia histórica existente conserva su validez y puede acreditar autorizaciones.
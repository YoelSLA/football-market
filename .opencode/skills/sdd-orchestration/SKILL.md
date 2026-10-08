---
name: sdd-orchestration
description: Orquestador principal del ciclo de vida de una Feature en Spec-Driven Development. Usar esta skill para determinar la fase actual de una feature, validar gates, obtener autorizaciones humanas de implementación, coordinar el Code Review y el Testing obligatorios, decidir el avance entre fases y sincronizar operacionalmente con Trello mediante trello-traceability.
compatibility: opencode
metadata:
  project: football-market
  methodology: spec-driven-development
  spec-system: spec-kit
  human-gates: implementation-authorization, code-review, testing, feature-completion-authorization
  related-skill: trello-traceability
---

# Orquestación SDD

## Propósito

Esta skill es el **orquestador principal del ciclo de vida de una Feature** en Football Market.

Define:

- en qué fase del lifecycle SDD se encuentra una Feature;
- qué operación de Spec Kit corresponde ejecutar;
- qué artefactos están vigentes y cuáles quedaron obsoletos;
- qué User Stories están `READY` y cuáles están autorizadas para implementación;
- qué puede avanzar automáticamente y qué debe detenerse;
- qué gates humanos están pendientes y qué evidencia debe generarse;
- cuándo una User Story y cuándo una Feature pueden pasar a `DONE`.

## Límites de responsabilidad

Esta skill **no reemplaza** ni reimplementa:

- **Spec Kit** — sigue siendo responsable de generar y mantener `spec.md`, `plan.md`, `tasks.md`, checklists, artefactos de diseño, implementación y análisis de convergencia.
- **`trello-traceability`** — sigue siendo responsable de la estructura de Trello, nombres de columnas, labels, links y las actualizaciones mecánicas.

Esta skill **coordina** esas operaciones y decide **qué** debe ocurrir y **cuándo**. Nunca emula manualmente una operación de Spec Kit.

Si una operación de Spec Kit requerida no está disponible o falla: **detener el workflow y reportarlo**. No intentar sustituirla.

---

# 1. Precedencia de fuentes

Usar este orden para determinar qué requiere una Feature:

```text
Constitution
    ↓
spec.md
    ↓
clarifications incorporadas a spec
    ↓
plan.md / design artifacts
    ↓
tasks.md
    ↓
implementation
    ↓
Trello
```

Trello es **tracking operacional y evidencia del workflow**, nunca fuente normativa de requisitos.

El código existente, una tarjeta vieja o el historial de chat nunca sobrescriben silenciosamente lo definido por la especificación.

Cuando dos artefactos se contradigan, identificar cuál es responsable de esa decisión y corregir la inconsistencia **en el origen**.

---

# 2. Lifecycle SDD

```text
IDEA / EPIC
    ↓
SPECIFY
    ↓
CLARIFY
    ↓
PLAN
    ↓
CHECKLIST
    ↓
TASKS
    ↓
ANALYZE
    ↓
READY
    ↓
IMPLEMENT
    ↓
CODE REVIEW
    ↓
TESTING
    ↓
DONE
```

Después de que **todas** las User Stories estén `DONE`:

```text
Feature → TESTING
    ↓
CONVERGE
    ↓
FEATURE COMPLETION AUTHORIZATION (humana, explícita y persistida)
    ↓
Feature → DONE
```

## Operaciones de Spec Kit

```text
/speckit.specify
/speckit.clarify
/speckit.plan
/speckit.checklist
/speckit.tasks
/speckit.analyze
/speckit.implement
/speckit.converge
```

Estas operaciones son responsabilidad de Spec Kit. Esta skill las coordina, no las reimplementa.

## CODE REVIEW y TESTING

`CODE REVIEW` y `TESTING` son **gates humanos obligatorios** definidos por esta skill. No tienen comando propio de Spec Kit: son pasos de orquestación que requieren la aprobación explícita del usuario.

`CONVERGE` es la operación `/speckit.converge` aplicada al nivel Feature.

## Máquina de estados

```text
IDEA
 │
 ▼
SPECIFY → CLARIFY → PLAN → CHECKLIST → TASKS → ANALYZE
                                                │
                                     ┌──────────┴──────────┐
                                 blockers              no blockers
                                     │                      │
                                     ▼                      ▼
                              corregir origen           READY ──┐
                                     │                      │      │
                                     └── ANALYZE           ▼      │
                                                   AUTORIZACIÓN    │
                                                   HUMANA          │
                                                         │          │
                                                         ▼          ▼
                                                    IMPLEMENT ─────┘
                                                         │
                                        ┌────────────────┴────────────────┐
                                        ▼                                 ▼
                                  CODE REVIEW                        ambiguidad
                                        │                            funcional
                            ┌───────────┴───────────┐                    │
                        rechazado              aprobado                ▼
                            │                       │             STOP IMPLEMENT
                            ▼                       ▼             → upstream
                       IN PROGRESS              TESTING          → ANALYZE
                            │                       │             → READY
                            └── correction ────────┘
                                    │
                                    ▼
                              CODE REVIEW
                                    │
                            ┌───────┴────────┐
                            ▼                ▼
                     cambio de código   sin cambios
                            │                │
                            ▼                ▼
                       CODE REVIEW         DONE
                                        (aprobación
                                       TESTING humana)
```

Feature IN PROGRESS: existe al menos una US cuyo IMPLEMENT comenzó y la Feature todavía no cumple condiciones para TESTING final. CODE REVIEW y TESTING de las US conservan significado propio. Todas las US requeridas DONE y trabajo necesario completo habilitan TESTING final; CONVERGE exitoso prepara cierre, pero DONE exige FEATURE COMPLETION AUTHORIZATION humana explícita, vigente y persistida.

Los retrocesos son válidos y esperables. Nunca preservar artificialmente el avance de una Feature a costa de la consistencia de los artefactos.

---

# 3. Trello

## Columnas

El modelo canónico es:

```text
BACKLOG
SPECIFYING
READY
IN PROGRESS
CODE REVIEW
TESTING
DONE
```

No agregar columnas nuevas.

## `SPECIFYING` agrupa internamente

```text
SPECIFY
CLARIFY
PLAN
CHECKLIST
TASKS
ANALYZE
```

Conservar internamente la fase SDD detallada aunque Trello solo muestre `SPECIFYING`. La columna es una proyección; la fase SDD es el estado real.

## Semántica de `TESTING`

La doble semántica de `TESTING` es **intencional**:

| Tipo de tarjeta | Significado de `TESTING` |
| --- | --- |
| Feature | `CONVERGE` final |
| User Story | revisión humana de tests + ejecución humana de tests |

No crear una columna `CONVERGE`.

## Tarjetas

- La Feature tiene su propia tarjeta.
- Las User Stories tienen tarjetas independientes.
- Las tarjetas de User Stories se crean **durante `SPECIFYING`**, tan pronto como las historias estén suficientemente identificadas. Si en ese momento una US todavía no es identificable con certeza, esperar a `CLARIFY` y crearla entonces; no crear US ficticias.

## Sincronización

`sdd-orchestration` **decide** las transiciones. `trello-traceability` **ejecuta** las actualizaciones mecánicas.

---

# 4. SPECIFY y pre-clarificación

## Objetivo

Definir **qué** debe construirse y **por qué**, sin introducir prematuramente detalles de implementación.

## Pre-clarificación mínima

Si todavía **no existe** una spec y la idea inicial es demasiado amplia o ambigua, realizar primero una pre-clarificación mínima con el usuario. Su objetivo es obtener suficiente intención funcional para poder ejecutar `SPECIFY`.

Esta pre-clarificación **NO reemplaza** `CLARIFY`, que siempre es obligatorio después de `SPECIFY`.

## EPICs demasiado amplias

Si la entrada representa una EPIC demasiado amplia, el agente **puede proponer** una división en varias Specs, pero el usuario **debe aprobar los límites** antes de continuar.

**Nunca dividir automáticamente una EPIC.**

## Gate de salida

No avanzar si:

- existen requisitos materialmente ambiguos;
- faltan User Stories relevantes;
- faltan criterios o escenarios de aceptación;
- los requisitos se contradicen;
- existen edge cases importantes sin definir;
- detalles técnicos están reemplazando decisiones de negocio o comportamiento.

## Transición

```text
SPECIFY → CLARIFY
```

---

# 5. CLARIFY

## Objetivo

Resolver ambigüedades antes del diseño técnico. Las decisiones importantes deben quedar **incorporadas en la especificación**, no solamente en el historial del chat.

## Gate de salida

Puede avanzarse cuando no existan ambigüedades relevantes que puedan modificar:

- comportamiento;
- alcance;
- reglas de dominio;
- validaciones;
- permisos;
- errores;
- persistencia;
- contratos de API;
- comportamiento visible para el usuario.

## Retroceso

```text
CLARIFY
   ↓
actualizar spec.md
   ↓
CLARIFY nuevamente si es necesario
```

## Transición

```text
CLARIFY → PLAN
```

---

# 6. PLAN

## Objetivo

Transformar la especificación aprobada en un diseño técnico coherente. Los detalles de arquitectura y decisiones de implementación pertenecen principalmente a esta fase.

## Artefactos esperados

Como mínimo:

```text
plan.md
```

Según la Feature, también pueden existir:

```text
research.md
data-model.md
contracts/
quickstart.md
```

## Gate de salida

No avanzar si:

- el plan no cubre la especificación;
- la arquitectura contradice la Constitución;
- faltan relaciones importantes del dominio;
- faltan contratos necesarios;
- hay decisiones técnicas sin resolver que impiden descomponer el trabajo.

## Retroceso

```text
PLAN → CLARIFY / SPECIFY
```

Corregir primero el requisito. No ocultar cambios funcionales dentro de `plan.md`.

## Transición

```text
PLAN → CHECKLIST
```

---

# 7. CHECKLIST

## Obligatoriedad

`CHECKLIST` es **obligatorio**.

Debe existir:

1. un **checklist general de calidad de requisitos**;
2. **checklists específicos** cuando correspondan al tipo de Feature.

Especializados posibles:

```text
security
API
UX
data
authentication
authorization
```

El agente decide qué checklists especializados aplican, según la naturaleza de la Feature, y debe justificar brevemente su selección.

## Naturaleza del gate

Las checklists son **gates de calidad**, no listas de tareas de desarrollo.

## Regla

Si la checklist detecta un problema:

```text
CHECKLIST
     ↓
SPECIFY / CLARIFY
     ↓
PLAN si fue afectado
     ↓
CHECKLIST
```

No marcar elementos como completos solamente para poder continuar.

## Transición

```text
CHECKLIST → TASKS
```

---

# 8. TASKS

## Objetivo

Crear una descomposición de implementación ejecutable, ordenada y consciente de dependencias.

## Artefacto esperado

```text
tasks.md
```

Las tareas deben derivarse de la especificación y del plan aprobados.

## Gate de salida

No avanzar si:

- una User Story no tiene tareas suficientes para implementarse;
- las tareas agregan requisitos que no existen en `spec.md`;
- las tareas contradicen `plan.md`;
- faltan dependencias importantes;
- las tareas son demasiado vagas;
- el orden de ejecución viola dependencias técnicas.

## Distinción User Story / Task

Una **User Story** describe comportamiento esperado.
Una **Task** describe trabajo técnico de implementación.

No tratarlas como equivalentes.

## Qué puede contener `tasks.md`

Puede contener tareas técnicas para escribir tests, por ejemplo:

```text
Create AuthService unit tests
```

## Qué NO puede contener `tasks.md`

No debe contener gates humanos del workflow:

```text
User performs Code Review
User runs tests
User approves Testing
```

`CODE REVIEW` y `TESTING` son gates del workflow, no implementation tasks.

## Significado de completar `tasks.md`

```text
tasks.md complete  =  implementation complete
```

Y **NO**:

```text
tasks.md complete  ≠  Feature DONE
```

## Transición

```text
TASKS → ANALYZE
```

---

# 9. ANALYZE

## Obligatoriedad

`ANALYZE` es **obligatorio antes de READY**.

## Consistencia a comprobar

```text
Constitution
spec
clarifications
plan/design
tasks
```

## Blockers

Son blockers, entre otros:

- contradicciones constitucionales;
- ambigüedades funcionales;
- contradicciones funcionales;
- requisitos sin cobertura en plan/tasks;
- cambios de alcance no aprobados;
- imposibilidad de verificar un criterio obligatorio.

## Findings non-blocking

Problemas puramente editoriales, de naming o claridad que **no** afecten comportamiento, alcance, arquitectura, Constitución, cobertura ni verificabilidad.

Los findings non-blocking **se reportan pero no impiden READY**.

## Corrección automática única

Si `ANALYZE` descubre una inconsistencia **mecánica** cuya corrección es **inequívoca según la fuente de verdad**, el agente puede realizar **UNA** corrección automática.

`/speckit.analyze` permanece estrictamente read-only. La corrección única ocurre fuera del comando, en operación separada del orquestador. Después informar exactamente lo corregido y **STOP**; nuevo ANALYZE en operación posterior. No corregir durante el comando.

## Prohibido el loop autónomo

```text
analyze → fix → analyze → fix → ...
```

Ese loop debe ser dirigido por el usuario.

## Resultado

Solo un `ANALYZE` sin blockers permite `READY`.

Una User Story no se considera `READY` solamente porque existe en `tasks.md`.

---

# 10. READY

## Alcance

`READY` es un estado **Feature-level**. `READY` es un estado del workflow, no un artefacto de Spec Kit.

La Feature solo alcanza `READY` si `ANALYZE` no contiene blockers.

**Un blocker en cualquier User Story impide READY para toda la Feature.**

**Dependency ≠ Blocker.** READY exige preparación global válida sin blockers de especificación, planificación o consistencia. Dependencias técnicas conocidas e identificadas no impiden READY si pueden resolverse por orden técnico y autorizaciones correspondientes.

## Efecto

Al alcanzar `READY`, las User Stories válidas pasan a `READY`.

## Prohibición crítica

`READY` **no autoriza automáticamente** modificar código.

Antes de `IMPLEMENT` debe existir **autorización humana** explícita.

---

# 11. Autorización de implementación

Cuando una Feature alcanza `READY`, el agente debe:

1. listar **todas** las User Stories disponibles;
2. mostrar **dependencias**;
3. indicar **cuáles pueden implementarse independientemente**;
4. pedir al usuario **cuáles autoriza implementar**.

## División de responsabilidad

```text
Usuario  → decide QUÉ User Stories autorizar
Agente   → decide el ORDEN técnico correcto respetando dependencias
```

Si el usuario selecciona una combinación inconsistente, **advertirlo antes de implementar**.

## Persistencia de la autorización

La autorización es evidencia operacional y debe persistirse en Trello mediante `trello-traceability`, como comentario estructurado **en cada US autorizada**:

```text
[SDD GATE]
Gate: IMPLEMENT AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

Perseguir la autorización individual por US para que la trazabilidad de cada tarjeta sea autosuficiente.

## Transiciones

Una US autorizada **NO** pasa inmediatamente a `IN PROGRESS`.

```text
US1 autorizada → IN PROGRESS cuando comienza
US2 autorizada → READY mientras espera
US3 autorizada → READY mientras espera
```

La Feature pasa a `IN PROGRESS` cuando comienza la primera US y permanece allí durante el desarrollo, review y testing de las US.

---

# 12. IMPLEMENT

## Alcance

`IMPLEMENT` es la **única fase que modifica product code**.

`CONVERGE` **nunca** debe modificar product code.

## Reglas

- respetar `constitution → spec.md → plan.md → tasks.md`;
- no modificar silenciosamente requisitos porque el código existente los haga difíciles de implementar;
- no inventar alcance nuevo;
- no marcar una tarea como terminada si su implementación o validación no está completa;
- respetar dependencias entre tareas;
- el agente **puede crear o modificar tests** durante `IMPLEMENT`;
- el agente **NO puede ejecutar los tests** cuando la Constitución lo prohíba (en Football Market está prohibido, incluida la ejecución indirecta). Para obtener resultados, solicitar los casos concretos y su salida al usuario.

## Varias User Stories autorizadas

El agente puede implementar varias US previamente autorizadas respetando dependencias.

Puede continuar con otras US autorizadas e independientes mientras una US espera `CODE REVIEW` o `TESTING`.

Si el feedback de una US puede afectar a otra US actualmente en desarrollo, **evaluar el impacto antes de continuar**.

## Ambigüedad funcional durante IMPLEMENT

```text
STOP IMPLEMENT
    ↓
volver upstream
    ↓
Feature pierde READY
    ↓
resolver mediante CLARIFY / spec según corresponda
    ↓
actualizar artefactos afectados
    ↓
ANALYZE
    ↓
recuperar READY
    ↓
continuar según autorizaciones vigentes
```

No parchear alrededor de una especificación incorrecta.

---

# 13. Invalidación por impacto

Los cambios upstream **no deben regenerar todo indiscriminadamente**.

Evaluar impacto y actualizar **únicamente** artefactos y User Stories afectados.

## Cambios en la Constitución durante una Feature

```text
no afecta la Feature  → continuar
afecta la Feature     → invalidar READY y actualizar lo necesario
```

## Autorizaciones

Las autorizaciones también se invalidan por impacto.

- Una US previamente autorizada y **no afectada** conserva su autorización.
- Una US **afectada** necesita **nueva autorización** después de recuperar `READY`.

Cuando se determine una invalidación, delegar en `trello-traceability` el registro de un **nuevo** comentario:

```text
[SDD GATE]
Gate: IMPLEMENT AUTHORIZATION
Result: INVALIDATED
Reason: upstream change affected this User Story
User Story: US2
```

Nunca borrar la evidencia histórica: el historial debe mostrar la autorización previa, su invalidación y la nueva autorización.

## Reglas de invalidación

```text
cambia spec.md              → revisar plan.md → revisar/regenerar tasks.md → ANALYZE
cambia plan.md              → revisar/regenerar tasks.md → ANALYZE
cambia materialmente tasks.md → ANALYZE
```

No asumir que un `ANALYZE` anterior sigue siendo válido después de cambios relevantes.

---

# 14. CODE REVIEW (gate humano obligatorio)

## Transición

Después de completar la implementación de una US:

```text
IN PROGRESS
    ↓
CODE REVIEW
```

`CODE REVIEW` es un **gate humano obligatorio**. El agente **NO puede aprobarlo**.

## Code Review Report

Cada vez que una US entra a `CODE REVIEW` debe generarse un `Code Review Report` completo con, como mínimo:

1. resumen de lo implementado;
2. cómo funciona;
3. archivos creados;
4. por qué fue creado cada archivo;
5. responsabilidad de cada archivo;
6. archivos modificados;
7. qué cambió en cada uno;
8. por qué fue necesario;
9. decisiones importantes de implementación;
10. puntos concretos que conviene inspeccionar;
11. funciones / secciones / líneas relevantes cuando sea práctico;
12. orden recomendado de revisión;
13. checklist humano obligatorio.

**Objetivo:** que el usuario pueda revisar directamente los archivos relevantes sin tener que investigar qué cambió.

El `CODE REVIEW` debe cubrir **TODO** el código de la US, **incluidos los tests**.

---

# 15. Aprobación de CODE REVIEW

## Aprobación inequívoca

No interpretar expresiones ambiguas como:

```text
ok
dale
seguí
```

como aprobación.

La aprobación debe ser inequívoca y referirse al checklist completo.

Ejemplo válido:

```text
Checklist revisado. Apruebo el Code Review.
```

## Checklist obligatorio

El `Code Review Report` debe incluir un checklist como:

```text
[ ] Revisé los archivos creados/modificados.
[ ] Entiendo qué cambió y por qué.
[ ] Revisé las decisiones importantes.
[ ] No detecté comportamiento fuera de la spec.
[ ] Apruebo avanzar a TESTING.
```

## Rechazo

Si el usuario rechaza el review:

```text
CODE REVIEW
    ↓
IN PROGRESS
    ↓
corrección
    ↓
nuevo Code Review Report
    ↓
CODE REVIEW nuevamente
```

Nunca saltar el segundo review.

---

# 16. Invalidación del CODE REVIEW

El `CODE REVIEW` valida el **estado final del código**, no quién realizó los cambios.

La invalidación se determina por impacto sobre el alcance aprobado, no por la mera existencia de cambios. Código o tests que afecten ese alcance invalidan los gates correspondientes; cambios sin impacto conservan aprobaciones, independientemente de si fueron realizados por:

- el agente;
- el usuario;
- otro mecanismo.

**Los tests también cuentan como código.**

```text
cambia código con impacto en el alcance aprobado
    → nuevo CODE REVIEW obligatorio
```

Los cambios puramente documentales que no afectan código ni comportamiento **no** invalidan automáticamente el review.

---

# 17. TESTING (gate humano obligatorio)

## Transición

```text
CODE REVIEW aprobado
    ↓
TESTING
```

`TESTING` es otro **gate humano obligatorio**.

## Propósito

```text
revisar los tests
+
verificar que representan correctamente el comportamiento requerido
+
ejecutarlos
+
confirmar que pasan
```

No convertir `TESTING` en un gate genérico de testing manual si eso no está definido por la Feature.

## Límites

- El agente **puede crear/modificar tests** durante `IMPLEMENT`.
- El agente **NO puede ejecutar los tests** cuando la Constitución lo prohíba.
- **Nunca** marcar tests como ejecutados o aprobados sin evidencia humana.

---

# 18. Testing Report

Cada vez que una US entra en `TESTING`, generar un `Testing Report` completo con:

1. tests creados;
2. tests modificados;
3. relación entre tests y User Story;
4. criterios de aceptación cubiertos;
5. casos relevantes;
6. mocks utilizados;
7. assertions importantes;
8. posibles falsos positivos o puntos delicados;
9. qué debería revisar el usuario;
10. orden recomendado de revisión;
11. comandos exactos que el usuario debe ejecutar;
12. estado:

```text
PENDING HUMAN VALIDATION
```

---

# 19. Aprobación de TESTING

`TESTING` requiere **aprobación explícita**. No asumir aprobación por expresiones ambiguas.

La confirmación debe indicar inequívocamente que el usuario:

- revisó los tests;
- verificó su cobertura;
- los ejecutó;
- obtuvo resultado satisfactorio.

Ejemplo válido:

```text
Testing revisado y aprobado. Revisé los tests y todos pasan.
```

No es obligatorio pegar toda la salida cuando pasan.

Si fallan, solicitar el output relevante necesario para diagnosticar.

---

# 20. Fallos en TESTING

Si `TESTING` falla, **clasificar primero la causa**.

## Requiere cambiar production code o test code

```text
TESTING
    ↓
IN PROGRESS
    ↓
corrección
    ↓
CODE REVIEW
    ↓
TESTING
```

Recordar: cambiar test code también invalida el CODE REVIEW (§16).

## Problema exclusivamente externo (environment / configuration)

Si no requiere modificar código:

```text
US permanece TESTING
status = BLOCKED
```

Cuando se resuelve el bloqueo puede continuarse **sin repetir CODE REVIEW** si el código no cambió.

---

# 21. Dependencias y TESTING

Una US puede entrar a `CODE REVIEW` aunque todavía no pueda probarse end-to-end por una dependencia.

Después de aprobar `CODE REVIEW` puede pasar a `TESTING` y quedar:

```text
TESTING — BLOCKED BY DEPENDENCY
```

No repetir `CODE REVIEW` cuando se resuelva la dependencia si el código de la US no cambió.

---

# 22. TESTING N/A

`TESTING` sigue siendo un **gate universal**.

Si razonablemente no existen tests automatizados aplicables, **no crear tests artificiales de poco valor**.

El agente debe justificar:

```text
Automated tests: N/A
Reason: ...
```

El usuario debe **validar explícitamente esa justificación** antes de superar `TESTING`.

---

# 23. User Story DONE

Una US solo puede alcanzar `DONE` después de:

```text
IMPLEMENT complete
+
CODE REVIEW aprobado por humano
+
TESTING aprobado por humano
```

Cuando existe aprobación humana explícita de `TESTING`, el agente puede realizar mecánicamente:

```text
TESTING → DONE
```

y registrar la evidencia correspondiente en Trello.

---

# 24. Feature y CONVERGE

## Durante el desarrollo

```text
Feature = IN PROGRESS
```

## Cuando todas las US están DONE

```text
Feature: IN PROGRESS → TESTING
```

Para la tarjeta Feature, `TESTING` significa `CONVERGE` final.

---

# 25. CONVERGE

## Naturaleza

`/speckit.converge` es **diagnóstico/append-only**: solo puede anexar tareas de remediación según su definición. No corrige documentos durante el comando. Una corrección documental inequívoca ocurre fuera de CONVERGE, como operación separada del orquestador, seguida por otra ejecución.

Nunca debe:

- modificar product code;
- ejecutar tests;
- reemplazar `IMPLEMENT`;
- reemplazar evidencia humana.

Utiliza la evidencia humana previamente registrada.

## Contraste

```text
Constitution
→ spec
→ plan/design
→ tasks
→ implementation
→ Code Review evidence
→ Testing evidence
```

No basta con que el código compile ni con que las tareas iniciales estén marcadas.

## Falta implementación requerida por la spec

```text
Feature → IN PROGRESS
affected US → IN PROGRESS
create/reopen missing implementation task
→ IMPLEMENT
→ CODE REVIEW
→ TESTING
→ DONE
→ Feature TESTING
→ CONVERGE again
```

## Trabajo requerido por una spec existente pero ausente en `tasks.md`

Puede crear automáticamente la tarea faltante **si no inventa comportamiento nuevo**.

## Comportamiento no definido por la spec

```text
STOP
→ ask human
→ resolve upstream
→ update affected artifacts
→ ANALYZE
→ READY
→ continue
```

## Inconsistencia documental mecánica e inequívoca

```text
outside command: auto-correct affected artifact in separate operation
→ rerun CONVERGE
```

No repetir `IMPLEMENT` / `CODE REVIEW` / `TESTING` si no cambió código ni comportamiento.

## Resultado exitoso

```text
Feature TESTING → DONE
```

solo después de FEATURE COMPLETION AUTHORIZATION humana explícita, vigente y persistida. CONVERGE exitoso no autoriza cierre automático: Feature permanece TESTING mientras espera esa decisión.

## FEATURE COMPLETION AUTHORIZATION

Presentar resultado final/CONVERGE y solicitar autorización inequívoca referida a la Feature y alcance evaluado; ok/dale/seguí no bastan. El usuario decide cierre; el orquestador determina preparación y trello-traceability persiste en tarjeta Feature:

```text
[SDD GATE]
Gate: FEATURE COMPLETION AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
Feature: SPEC-007
```

Solo tras persistencia verificada actualizar Status: Approved y mover Feature DONE. Si un cambio impacta la base de cierre, el orquestador invalida por impacto, registra nuevo evento Result: INVALIDATED con Reason y Feature (sin aprobación humana ficticia), repite fases afectadas y solicita nueva autorización. Conservar historial y gates no afectados.

---

# 26. Definition of DONE (Feature)

La Feature solo está `DONE` cuando se cumplen **TODAS** estas condiciones:

```text
✓ implementation tasks complete
✓ every US passed CODE REVIEW
✓ every US passed TESTING
✓ every US is DONE
✓ final CONVERGE succeeded
✓ FEATURE COMPLETION AUTHORIZATION approved by human, current and persisted
✓ mandatory gate evidence persisted
```

La regla `tasks.md complete = Feature DONE` queda **explícitamente reemplazada** por esta definición.

---

# 27. Trello y evidencia humana

## Reparto

```text
sdd-orchestration      → decide las transiciones y los gates
trello-traceability    → persiste la evidencia y ejecuta las actualizaciones
```

## Evidencia operacional a persistir en Trello

Cuando un gate se resuelve, delegar en `trello-traceability` la persistencia de la evidencia como **comentario estructurado** en la tarjeta correspondiente:

- `IMPLEMENT AUTHORIZATION` aprobada;
- `CODE REVIEW` aprobado o rechazado;
- `TESTING` aprobado o `N/A`;
- `FEATURE COMPLETION AUTHORIZATION` aprobada y sus invalidaciones;
- invalidaciones de los gates afectados, decididas por impacto;
- bloqueos y dependencias bloqueantes;
- cualquier otra validación humana.

Formato canónico (detalle en `trello-traceability` §21):

```text
[SDD GATE]
Gate: <CODE REVIEW | TESTING | IMPLEMENT AUTHORIZATION>
Result: <resultado válido para el gate>
Validated by: HUMAN
User Story: US2
```

FEATURE COMPLETION AUTHORIZATION usa Feature en lugar de User Story. Invalidaciones usan Result: INVALIDATED y Reason, sin Validated by: HUMAN salvo que exista una validación humana real; resultados admitidos se detallan en trello-traceability.

No modificar la descripción principal de la tarjeta para registrar aprobaciones, y no copiar los reports completos: eso corresponde a `trello-traceability`.

No guardar estas aprobaciones como tareas técnicas en `tasks.md`.

## Fallo mecánico de sincronización

Si falla una actualización mecánica de Trello, no necesariamente bloquea el trabajo técnico.

Reportar:

```text
TRELLO OUT OF SYNC
```

y reintentar / sincronizar cuando sea posible.

## Imposibilidad de persistir evidencia humana obligatoria

Si no puede almacenarse evidencia obligatoria de IMPLEMENT AUTHORIZATION, CODE REVIEW, TESTING, FEATURE COMPLETION AUTHORIZATION o sus invalidaciones, reportar GATE EVIDENCE NOT PERSISTED y no habilitar la transición dependiente hasta resolverla. Trello registra, no decide gates.

---

# 28. Autonomía

## El agente puede actuar automáticamente

Operaciones mecánicas cuando el resultado sea inequívoco:

- crear / actualizar tarjetas de US durante `SPECIFYING`;
- mover US y Feature entre columnas según las transiciones decididas;
- marcar una US `IN PROGRESS` cuando empieza a trabajar en ella;
- delegar la persistencia de la evidencia de `IMPLEMENT AUTHORIZATION`, `CODE REVIEW` y `TESTING` como comentarios estructurados;
- `TESTING → DONE` tras aprobación humana explícita;
- `Feature TESTING → DONE` tras CONVERGE exitoso y autorización final humana vigente/persistida;
- sincronización de Trello vía `trello-traceability`.

No pedir confirmación para cada transición mecánica.

## El agente debe detenerse

Ante:

- decisiones funcionales;
- ambigüedades;
- cambios de alcance;
- comportamiento nuevo no definido;
- decisiones que requieran criterio humano.

---

# 29. Detección de Feature / branch

Antes de actuar:

1. determinar la **branch activa**;
2. identificar la **Feature activa**;
3. inspeccionar los **artefactos existentes**;
4. determinar la **fase actual**;
5. detectar **artefactos downstream posiblemente stale**.

No reiniciar desde `SPECIFY` si ya existen artefactos válidos. Continuar desde la primera fase incompleta o inválida.

Si la branch **no permite identificar inequívocamente** la Feature, detenerse y preguntarle al usuario.

**Nunca seleccionar silenciosamente una Feature dudosa.**

---

# 30. Estado obligatorio informado al usuario

El agente debe mostrar un estado estructurado y suficientemente completo:

```text
Feature: <name>
SDD Phase: <phase>
Feature Trello: <column>
READY: YES | NO

User Stories:
- US1 → <state>
- US2 → <state>
- US3 → <state>

Human gates:
- CODE REVIEW US1: PENDING / APPROVED / INVALIDATED
- TESTING US1: PENDING / APPROVED / BLOCKED
- IMPLEMENT AUTHORIZATION: <list> / NONE
- FEATURE COMPLETION AUTHORIZATION: PENDING | APPROVED | INVALIDATED

Authorized:
- ...

Blockers:
- ...

Trello sync:
- SYNCED | OUT OF SYNC

Next transition:
- ...
```

Cuando corresponda, incluir además el `Code Review Report` o el `Testing Report`.

**No obligar al usuario a investigar por su cuenta qué archivos o tests debe revisar.**

---

# 31. Cambios de User Stories durante SPECIFYING

Las tarjetas de las US existen desde `SPECIFYING`.

Si una modificación aprobada de la spec implica inequívocamente **renombrar, agregar, eliminar, dividir o ajustar** una US, mantener Trello sincronizado mediante `trello-traceability`.

Si los **límites funcionales** de la US requieren una decisión humana, detenerse y preguntar antes de modificar la estructura funcional.

---

# 32. Principio general de invalidación

Aplicar **invalidación por impacto**.

No reiniciar fases, reviews, testing, autorizaciones o artefactos que demostrablemente no fueron afectados.

Pero nunca conservar una aprobación humana si cambió aquello que esa aprobación validaba.

```text
code changed with impact on approved scope
→ affected CODE REVIEW invalidated
```

```text
tests changed with impact on approved scope
→ affected CODE REVIEW invalidated
→ affected TESTING invalidated
```

```text
requirements changed
→ evaluate affected US/artifacts
→ invalidate only affected downstream state
```

---

# 33. Comportamientos prohibidos

Nunca:

- ignorar findings bloqueantes de `ANALYZE`;
- considerar una US `READY` solamente porque existen tareas;
- implementar una US insuficientemente especificada;
- inventar requisitos desde títulos de tarjetas de Trello;
- usar Trello como fuente de verdad de requisitos;
- cambiar requisitos silenciosamente durante implementación;
- mantener requisitos contradictorios entre artefactos;
- duplicar o emular manualmente comandos de Spec Kit;
- usar el historial de chat como fuente definitiva de requisitos;
- **declarar una Feature terminada antes del CONVERGE final**;
- **dar por hecho que `tasks.md` completo implica Feature DONE**;
- marcar checklists como completas sin validarlas;
- avanzar cuando un artefacto upstream conocido está obsoleto;
- **pasar una US de `IN PROGRESS` a `TESTING` sin `CODE REVIEW` aprobado**;
- **cerrar una Feature sin FEATURE COMPLETION AUTHORIZATION humana vigente y persistida**;
- **aprobar `CODE REVIEW` o `TESTING` en nombre del usuario**;
- **interpretar `ok` / `dale` / `seguí` como aprobación**;
- **ejecutar los tests** cuando la Constitución lo prohíba;
- **marcar tests como ejecutados o aprobados sin evidencia humana**;
- **modificar product code desde CONVERGE**;
- **ejecutar tests desde CONVERGE**;
- **implementar sin autorización humana explícita**;
- **derivar,aprobar o invalidar un gate a partir de la evidencia registrada en Trello** (la evidencia se persiste, no se reinterpreta);
- **mover una US a `IN PROGRESS` solo por haber sido autorizada**;
- **dividir automáticamente una EPIC**;
- **crear un loop autónomo `analyze → fix → analyze → fix`**;
- crear columnas de Trello nuevas.

---

# 34. Algoritmo de decisión

```text
1.  Identificar la Feature activa (branch + artefactos).
    Si es ambigua → STOP y preguntar.
2.  Inspeccionar artefactos y determinar fase actual.
3.  ¿Existe spec? No → pre-clarificar si hace falta → /speckit.specify.
4.  ¿CLARIFY resuelto? No → /speckit.clarify.
5.  ¿plan.md vigente? No → /speckit.plan.
6.  ¿CHECKLIST obligatoria satisfecha? No → /speckit.checklist.
7.  ¿tasks.md vigente? No → /speckit.tasks.
8.  /speckit.analyze.
9.  ¿Blockers? Sí → corregir origen (1 corrección mecánica, luego STOP)
                    → volver a 8.
    ¿Blockers? No → READY (Feature-level).
10. Pedir autorización humana de US a implementar.
11. Por cada US autorizada, en orden técnico respetando dependencias:
      IN PROGRESS → IMPLEMENT
                  → CODE REVIEW (+ Code Review Report) → gate humano
                  → TESTING (+ Testing Report) → gate humano
                  → DONE
12. Si aparece ambigüedad funcional → STOP IMPLEMENT → upstream → ANALYZE → READY.
13. Todas las US DONE → Feature IN PROGRESS → TESTING (= CONVERGE).
14. /speckit.converge.
      Faltantes  → crear tarea / US afectada → IMPLEMENT → CODE REVIEW
                   → TESTING → DONE → volver a 13.
      No definido por spec → STOP → ask human → upstream → ANALYZE → READY.
      Documental mecánica → corregir fuera del comando en operación separada → nuevo CONVERGE.
      Exitoso   → solicitar FEATURE COMPLETION AUTHORIZATION → persistir → Approved/Feature DONE.
15. Reportar estado estructurado (§30).
```

---

# 35. Comportamiento esperado del agente

Cuando esta skill esté activa, informar el estado actual con el formato de §30.

Ejemplo:

```text
Feature: 007-team-league-domain
SDD Phase: ANALYZE
Feature Trello: SPECIFYING
READY: NO

User Stories:
- US1 → NOT READY
- US2 → NOT READY

Human gates:
- IMPLEMENT AUTHORIZATION: NONE

Blockers:
- tasks.md no cubre el criterio de aceptación US1.AC3

Trello sync:
- SYNCED

Next transition:
- corregir tasks.md → re-ejecutar ANALYZE
```

O:

```text
Feature: 007-team-league-domain
SDD Phase: IMPLEMENT
Feature Trello: IN PROGRESS
READY: YES

User Stories:
- US1 → IN PROGRESS
- US2 → READY
- US3 → READY

Human gates:
- IMPLEMENT AUTHORIZATION: US1, US2
- CODE REVIEW US1: PENDING
- TESTING US1: PENDING

Blockers:
- ninguno

Trello sync:
- SYNCED

Next transition:
- completar implementación US1 → generar Code Review Report
```

El objetivo no es ejecutar mecánicamente todas las fases.

SPEC-001–005 conservan DONE legacy bajo el workflow anterior sin gates retroactivos. Reapertura material aplica el workflow vigente solo por impacto. Build/entrega no acreditan TESTING: tests aplicables pendientes impiden DONE. Autorización de implementación no inicia IMPLEMENT; US permanece READY hasta inicio efectivo.

El objetivo es que cada Feature avance por el lifecycle SDD con artefactos consistentes, trazables, **sin saltear gates de calidad ni gates humanos**.

---
name: sdd-orchestration
description: Orquesta el ciclo de Spec-Driven Development de Football Market usando Spec Kit. Usar esta skill para determinar la fase actual de una feature, validar gates, avanzar entre fases, detectar inconsistencias y decidir cuándo una User Story está lista para implementación.
compatibility: opencode
metadata:
  project: football-market
  methodology: spec-driven-development
  spec-system: spec-kit
---

# Orquestación SDD

## Propósito

Orquestar el ciclo de desarrollo guiado por especificaciones de Football Market.

Esta skill define:

- en qué fase se encuentra una feature;
- qué operación de Spec Kit corresponde ejecutar;
- qué artefactos deben existir antes de avanzar;
- qué condiciones deben cumplirse para pasar de fase;
- cuándo una User Story puede considerarse `READY`;
- cuándo hay que volver a una fase anterior;
- cuándo una implementación puede considerarse realmente terminada.

Esta skill **no reemplaza Spec Kit**.

Spec Kit sigue siendo responsable de generar y mantener:

- `spec.md`
- `plan.md`
- `tasks.md`
- checklists
- artefactos de diseño
- implementación
- análisis de convergencia

Esta skill es responsable de **orquestar el flujo entre esas operaciones**.

---

# Fuente de verdad

Usar el siguiente orden de precedencia para determinar qué requiere una feature:

1. Constitución del proyecto
2. `spec.md` de la feature actual
3. Clarificaciones incorporadas a la especificación
4. `plan.md` y artefactos de diseño asociados
5. `tasks.md`
6. Implementación actual
7. Sistemas externos de seguimiento, como Trello

Trello, el código existente o una tarea vieja nunca deben sobrescribir silenciosamente lo definido por la especificación.

Cuando dos artefactos se contradigan, identificar cuál de ellos es responsable de esa decisión y corregir la inconsistencia en el origen.

---

# Ciclo de vida canónico

El flujo estándar es:

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
CONVERGE
    ├── queda trabajo → IMPLEMENT
    └── convergió → DONE
```

Operaciones correspondientes de Spec Kit:

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

No crear versiones alternativas de estas operaciones dentro de esta skill.

---

# Regla principal de orquestación

Antes de decidir qué hacer, determinar siempre la **fase actual** de la feature.

No reiniciar el flujo desde `SPECIFY` si la feature ya tiene artefactos válidos.

Inspeccionar los artefactos existentes y continuar desde la primera fase incompleta o inválida.

Ejemplo:

```text
spec.md       válido
plan.md       válido
tasks.md      existe
analyze       detecta inconsistencias
```

En este caso la feature todavía no está `READY`.

Debe corregirse el artefacto responsable del problema y luego volver a ejecutar `ANALYZE`.

---

# 1. SPECIFY

## Objetivo

Definir **qué** debe construirse y **por qué**, evitando introducir prematuramente detalles de implementación.

## Artefacto esperado

```text
spec.md
```

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

# 2. CLARIFY

## Objetivo

Resolver ambigüedades antes del diseño técnico.

Las decisiones importantes deben quedar incorporadas en la especificación.

No dejar requisitos relevantes solamente en el historial del chat.

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

Si durante la clarificación se detecta que falta un requisito o que uno es incorrecto:

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

# 3. PLAN

## Objetivo

Transformar la especificación aprobada en un diseño técnico coherente.

Los detalles de arquitectura y decisiones de implementación pertenecen principalmente a esta fase.

## Artefactos esperados

Como mínimo:

```text
plan.md
```

Dependiendo de la feature, también pueden existir:

```text
research.md
data-model.md
contracts/
quickstart.md
```

## Gate de salida

No avanzar si:

- el plan no cubre la especificación;
- la arquitectura contradice la constitución del proyecto;
- faltan relaciones importantes del dominio;
- faltan contratos necesarios;
- hay decisiones técnicas sin resolver que impiden descomponer el trabajo.

## Retroceso

Si durante `PLAN` aparece un problema de requisitos:

```text
PLAN → CLARIFY / SPECIFY
```

Corregir primero el requisito.

No ocultar cambios funcionales dentro de `plan.md`.

## Transición

```text
PLAN → CHECKLIST
```

---

# 4. CHECKLIST

## Objetivo

Validar la calidad de la especificación antes de convertirla en trabajo de implementación.

Las checklists son gates de calidad, no listas de tareas de desarrollo.

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

# 5. TASKS

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

## Distinción importante

Una User Story describe comportamiento esperado.

Una Task describe trabajo técnico de implementación.

No tratarlas como equivalentes.

## Transición

```text
TASKS → ANALYZE
```

---

# 6. ANALYZE

## Gate crítico de calidad

`ANALYZE` es el gate obligatorio antes de considerar trabajo como listo para implementación.

Debe validar consistencia al menos entre:

```text
constitution
spec.md
plan.md
tasks.md
```

y cualquier otro artefacto relevante de la feature.

## Regla

Una User Story no debe considerarse `READY` solamente porque existe en `tasks.md`.

Debe considerarse `READY` únicamente cuando `ANALYZE` no detecta inconsistencias bloqueantes.

## Si se detectan problemas

Corregir el artefacto responsable:

```text
problema de requisitos
    → SPECIFY / CLARIFY

problema de diseño
    → PLAN

problema de descomposición
    → TASKS
```

Después actualizar o regenerar los artefactos dependientes y volver a ejecutar `ANALYZE`.

## Loop obligatorio

```text
ANALYZE
   ↓
¿hay problemas?
 ┌─────┴─────┐
sí           no
 ↓            ↓
corregir     READY
 ↓
ANALYZE
```

Nunca continuar con implementación si existen findings bloqueantes conocidos.

---

# 7. READY

`READY` es un estado del workflow, no un artefacto de Spec Kit.

Una User Story está `READY` cuando:

- existe en una especificación aprobada;
- sus criterios de aceptación son suficientemente claros;
- sus ambigüedades relevantes fueron resueltas;
- el enfoque técnico correspondiente está representado en `plan.md`;
- existen las tareas necesarias;
- las dependencias están identificadas;
- las checklists aplicables están satisfechas;
- `ANALYZE` no reporta inconsistencias bloqueantes.

Solo después de cumplir estas condiciones puede considerarse lista para implementación.

`READY` es el principal punto de handoff entre especificación e implementación.

---

# 8. IMPLEMENT

## Objetivo

Ejecutar el trabajo definido en `tasks.md`.

La implementación debe respetar:

```text
constitution
    ↓
spec.md
    ↓
plan.md
    ↓
tasks.md
```

## Reglas

No modificar silenciosamente requisitos porque el código existente haga difícil implementarlos.

No inventar alcance nuevo durante implementación.

No marcar una tarea como terminada si su implementación o validación correspondiente no está completa.

Respetar dependencias entre tareas.

Ejecutar las pruebas y validaciones adecuadas del proyecto.

## Problemas descubiertos durante implementación

Si la implementación revela que una suposición anterior era incorrecta:

```text
descubrimiento en implementación
       ↓
¿problema de SPEC / PLAN / TASK?
       ↓
corregir artefacto origen
       ↓
actualizar artefactos dependientes
       ↓
ANALYZE
       ↓
IMPLEMENT
```

No parchear alrededor de una especificación incorrecta.

---

# 9. CONVERGE

## Objetivo

Comprobar que la implementación realmente satisface la especificación, el plan y las tareas.

Convergencia no significa solamente:

```text
el código compila
```

ni:

```text
las tareas iniciales están marcadas
```

Debe evaluarse el resultado completo contra el conjunto de artefactos de la feature.

## Resultado: todavía queda trabajo

Si se detectan faltantes:

```text
CONVERGE
   ↓
agregar o actualizar tareas
   ↓
IMPLEMENT
   ↓
CONVERGE
```

Repetir hasta que no quede trabajo material pendiente.

## Resultado: convergencia completa

Solo entonces:

```text
CONVERGE → DONE
```

---

# Máquina de estados

```text
IDEA
 │
 ▼
SPECIFY
 │
 ▼
CLARIFY
 │
 ▼
PLAN
 │
 ▼
CHECKLIST
 │
 ▼
TASKS
 │
 ▼
ANALYZE
 │
 ├──────── problemas ────────┐
 │                           │
 ▼                           │
READY                        │
 │                           │
 ▼                           │
IMPLEMENT                    │
 │                           │
 ▼                           │
CONVERGE                     │
 │                           │
 ├── queda trabajo ──────────┤
 │                           │
 ▼                           │
DONE                         │
                             │
         corregir upstream ──┘
```

Los retrocesos son válidos y esperables.

Nunca preservar artificialmente el avance de una feature a costa de la consistencia de los artefactos.

---

# Invalidación de artefactos

Cambios en artefactos upstream pueden invalidar artefactos downstream.

Usar esta regla:

```text
cambia spec.md
    ↓
revisar plan.md
    ↓
revisar o regenerar tasks.md
    ↓
ANALYZE nuevamente
```

```text
cambia plan.md
    ↓
revisar o regenerar tasks.md
    ↓
ANALYZE nuevamente
```

```text
cambia materialmente tasks.md
    ↓
ANALYZE nuevamente
```

No asumir que un `ANALYZE` anterior sigue siendo válido después de cambios relevantes.

---

# Detección de feature

Antes de actuar:

1. determinar la branch activa;
2. identificar la feature correspondiente de Spec Kit;
3. inspeccionar los artefactos existentes;
4. determinar la fase actual;
5. detectar artefactos downstream obsoletos o inconsistentes;
6. ejecutar o recomendar solamente la siguiente transición válida.

No crear una segunda especificación si la feature activa ya representa el trabajo solicitado.

---

# Relación con EPIC

Una EPIC representa un objetivo amplio de producto o dominio.

Una Spec representa una unidad coherente e implementable.

Ejemplo:

```text
EPIC
 ├── SPEC A
 │    ├── User Story 1
 │    └── User Story 2
 │
 ├── SPEC B
 │    └── User Story 3
 │
 └── SPEC C
```

No forzar una EPIC demasiado grande dentro de una única Spec si eso vuelve difícil razonar sobre el ciclo SDD.

Cuando una feature sea demasiado grande, dividirla en unidades independientes preservando la relación con la EPIC padre.

---

# Límite con Trello

Trello representa el estado operativo del trabajo, pero no define requisitos.

Esta skill puede determinar eventos como:

```text
Spec creada
Spec clarificada
Plan completado
Tasks generadas
Analyze aprobado
User Story READY
Implementación iniciada
Implementación terminada
Feature convergida
```

La estructura concreta de Trello, sus columnas, labels, links y reglas de sincronización pertenecen a una skill separada: `trello-traceability`.

No duplicar esas reglas aquí.

---

# Algoritmo de decisión

Cuando se solicite continuar una feature:

```text
1. Identificar la feature activa.
2. Inspeccionar sus artefactos.
3. Determinar la fase actual.
4. Validar el gate de salida de esa fase.
5. Si el gate falla:
      corregir el artefacto responsable.
6. Si el gate pasa:
      avanzar a la siguiente fase válida.
7. Después de TASKS:
      ejecutar ANALYZE.
8. Solo un ANALYZE exitoso permite READY.
9. Desde READY:
      IMPLEMENT.
10. Después de implementar:
      CONVERGE.
11. Si CONVERGE encuentra trabajo pendiente:
      volver a IMPLEMENT.
12. Finalizar solamente cuando la feature converja.
```

---

# Comportamientos prohibidos

Nunca:

- ignorar findings bloqueantes de `ANALYZE`;
- considerar una User Story `READY` solamente porque existen tareas;
- implementar una User Story insuficientemente especificada;
- inventar requisitos desde títulos de tarjetas de Trello;
- cambiar requisitos silenciosamente durante implementación;
- mantener requisitos contradictorios entre artefactos;
- duplicar comandos de Spec Kit dentro de esta skill;
- usar el historial de chat como fuente definitiva de requisitos;
- declarar una feature terminada antes de `CONVERGE`;
- marcar checklists como completas sin validarlas;
- avanzar cuando un artefacto upstream conocido está obsoleto.

---

# Comportamiento esperado del agente

Cuando esta skill esté activa, informar de forma breve el estado actual.

Ejemplo:

```text
Feature actual: 007-team-league-domain
Fase actual: ANALYZE
Findings bloqueantes: 2
Lista para implementación: NO
Próxima transición: corregir plan/tasks → ANALYZE
```

O:

```text
Feature actual: 007-team-league-domain
Fase actual: READY
Gate ANALYZE: PASSED
Lista para implementación: SÍ
Próxima transición: IMPLEMENT
```

El objetivo no es ejecutar mecánicamente todas las fases.

El objetivo es que cada feature avance por el ciclo SDD con artefactos consistentes, trazables y sin saltear gates de calidad.

---
name: trello-traceability
description: Mantiene la trazabilidad entre EPICs, Specs, User Stories, Tasks y estados de Trello en Football Market. Usar esta skill para crear, actualizar, mover o validar tarjetas según el estado real del flujo SDD definido por Spec Kit.
compatibility: opencode
metadata:
  project: football-market
  tracker: trello
  methodology: spec-driven-development
  depends-on: sdd-orchestration
---

# Trazabilidad Trello

## Propósito

Mantener Trello sincronizado con el estado real del ciclo de Spec-Driven Development de Football Market.

Esta skill define:

- qué representa cada tipo de tarjeta;
- cómo relacionar EPICs, Specs y User Stories;
- cuándo crear tarjetas;
- cuándo moverlas entre columnas;
- cuándo una User Story puede pasar a `READY`;
- cómo reflejar `IMPLEMENT`, `CONVERGE` y `DONE`;
- qué vínculos deben mantenerse para conservar trazabilidad;
- qué información pertenece a Trello y qué información debe permanecer en Spec Kit.

Esta skill **no define requisitos funcionales**.

La fuente de verdad para requisitos y diseño continúa siendo:

1. Constitución del proyecto
2. `spec.md`
3. `plan.md`
4. `tasks.md`
5. Artefactos asociados de Spec Kit

Trello representa el estado operativo del trabajo.

---

# Principio fundamental

Nunca usar Trello como fuente primaria de requisitos.

Una tarjeta puede resumir una necesidad o reflejar estado, pero no debe reemplazar:

- `spec.md`
- criterios de aceptación;
- decisiones de clarificación;
- `plan.md`;
- `tasks.md`;
- contratos;
- documentación técnica.

Si una tarjeta contradice un artefacto de Spec Kit, prevalece el artefacto correspondiente.

La tarjeta debe corregirse para reflejar la fuente de verdad.

---

# Modelo de trazabilidad

Usar esta relación conceptual:

```text
EPIC
  ↓
SPEC
  ↓
USER STORY
  ↓
TASK
  ↓
IMPLEMENTACIÓN
  ↓
COMMIT / PR
```

La trazabilidad mínima debe permitir responder:

```text
¿A qué EPIC pertenece esta Spec?
¿Qué User Stories pertenecen a esta Spec?
¿Qué Tasks implementan cada User Story?
¿Qué cambio de código implementó ese trabajo?
¿En qué estado real está actualmente?
```

---

# EPIC

## Definición

Una EPIC representa un objetivo amplio de producto, dominio o capacidad.

Ejemplo:

```text
EPIC: Gestión de equipos y ligas
```

Una EPIC puede contener varias Specs.

```text
EPIC
 ├── SPEC 007 - Team / League Domain
 ├── SPEC 008 - Team Synchronization
 └── SPEC 009 - League Navigation
```

## Regla

No convertir automáticamente una EPIC en una Spec.

La EPIC puede existir antes de que sus Specs estén definidas.

## Contenido recomendado

Una tarjeta EPIC debe contener como mínimo:

- identificador o nombre;
- objetivo general;
- estado general;
- Specs relacionadas;
- enlaces relevantes.

No copiar dentro de la EPIC todos los requisitos detallados de cada Spec.

---

# SPEC

## Definición

Una tarjeta SPEC representa una feature concreta gestionada por Spec Kit.

Debe corresponder a una carpeta real de feature, por ejemplo:

```text
specs/007-team-league-domain/
```

## Relación obligatoria

Toda tarjeta SPEC debe apuntar a:

- su EPIC padre, si existe;
- su directorio o identificador de Spec Kit;
- sus User Stories.

Ejemplo:

```text
EPIC: Gestión de equipos y ligas
└── SPEC: 007-team-league-domain
```

## Cuándo crearla

Crear una tarjeta SPEC cuando la feature ya tenga identidad propia dentro del flujo de Spec Kit.

No crear una SPEC ejecutable solamente porque existe una idea vaga en backlog.

---

# USER STORY

## Definición

Una User Story representa una unidad de comportamiento definida dentro de `spec.md`.

Debe derivar de una Spec existente.

Ejemplo:

```text
SPEC 007-team-league-domain
 ├── US1 - Consultar equipo
 ├── US2 - Consultar liga
 └── US3 - Relacionar jugador con equipo
```

## Regla crítica

Nunca crear una User Story operativa en Trello si no existe una User Story equivalente o una definición funcional suficientemente clara dentro de la Spec correspondiente.

Trello puede contener ideas futuras, pero deben identificarse como ideas o backlog no especificado.

No tratarlas como trabajo listo.

---

# TASK

## Definición

Las Tasks son unidades técnicas de implementación provenientes de `tasks.md`.

Por defecto, no es obligatorio crear una tarjeta Trello por cada Task técnica.

La granularidad de `tasks.md` puede ser demasiado fina para el tablero.

## Regla recomendada

Usar Trello principalmente para:

```text
EPIC
SPEC
USER STORY
```

Mantener las Tasks técnicas en:

```text
tasks.md
```

Crear tarjetas de Task en Trello solamente cuando exista una necesidad operativa real, por ejemplo:

- coordinación entre personas;
- dependencia externa;
- trabajo bloqueado;
- seguimiento independiente;
- tarea de larga duración;
- actividad no representada adecuadamente por una User Story.

Evitar duplicar innecesariamente todo `tasks.md` en Trello.

---

# Estados y columnas

Trello debe reflejar el estado real del SDD.

El modelo recomendado es:

```text
BACKLOG
    ↓
SPECIFICATION
    ↓
READY
    ↓
IN PROGRESS
    ↓
REVIEW / CONVERGE
    ↓
DONE
```

Los nombres exactos de las columnas pueden variar, pero la semántica debe mantenerse.

---

# BACKLOG

## Significado

Trabajo identificado pero todavía no preparado para ejecución.

Puede contener:

- EPICs futuras;
- ideas;
- Specs todavía no iniciadas;
- posibles features;
- trabajo aún no especificado.

## Regla

Una tarjeta en `BACKLOG` no implica que exista una Spec.

No crear User Stories ficticias para anticipar una Spec todavía inexistente.

Una EPIC puede existir en backlog mientras sus Specs todavía no fueron creadas.

---

# SPECIFICATION

## Significado

La Spec está transitando alguna de estas fases:

```text
SPECIFY
CLARIFY
PLAN
CHECKLIST
TASKS
ANALYZE
```

Mientras `ANALYZE` no haya pasado satisfactoriamente, el trabajo no es `READY`.

## Regla

Las User Stories pueden existir en Trello durante esta etapa para aportar visibilidad, pero deben mantenerse claramente como:

```text
NOT READY
```

No deben interpretarse como ejecutables.

---

# READY

## Significado

`READY` es el handoff entre especificación e implementación.

Una User Story puede pasar a `READY` solamente cuando la skill `sdd-orchestration` determine que cumple el gate correspondiente.

Condiciones mínimas:

- existe en `spec.md`;
- tiene aceptación suficientemente definida;
- no tiene ambigüedades bloqueantes;
- existe un plan técnico compatible;
- tiene Tasks suficientes;
- dependencias conocidas;
- checklist aplicable satisfecha;
- `ANALYZE` aprobado sin findings bloqueantes.

## Regla crítica

```text
TASKS generadas ≠ READY
```

La transición correcta es:

```text
TASKS
  ↓
ANALYZE
  ↓
PASSED
  ↓
READY
```

Nunca mover una User Story a `READY` antes de ese gate.

---

# IN PROGRESS

## Significado

La implementación de la User Story ha comenzado realmente.

## Transición

```text
READY → IN PROGRESS
```

Mover a esta columna cuando exista trabajo activo de implementación.

No mover una tarjeta únicamente porque alguien la abrió o la inspeccionó.

## Relación con Spec Kit

Corresponde a:

```text
IMPLEMENT
```

---

# REVIEW / CONVERGE

## Significado

La implementación principal existe, pero todavía debe validarse contra la especificación completa.

Corresponde a:

```text
CONVERGE
```

Esta columna puede utilizarse para:

- verificación final;
- revisión funcional;
- revisión de consistencia;
- validación de tests;
- detección de trabajo faltante.

## Resultado posible

Si `CONVERGE` detecta trabajo pendiente:

```text
REVIEW / CONVERGE
        ↓
IN PROGRESS
```

No mover a `DONE`.

---

# DONE

## Significado

Una User Story está terminada solamente cuando su implementación ha convergido con los artefactos que la definen.

No alcanza con:

- compilar;
- haber hecho commit;
- haber abierto PR;
- haber completado las Tasks iniciales;
- haber escrito tests parciales.

Debe cumplir el resultado esperado y no tener trabajo material pendiente detectado por `CONVERGE`.

## Transición

```text
CONVERGE PASSED → DONE
```

---

# Flujo completo

```text
IDEA / EPIC
     ↓
BACKLOG
     ↓
SPEC creada
     ↓
SPECIFICATION
     │
     ├── SPECIFY
     ├── CLARIFY
     ├── PLAN
     ├── CHECKLIST
     ├── TASKS
     └── ANALYZE
           ↓
      ANALYZE PASSED
           ↓
         READY
           ↓
       IMPLEMENT
           ↓
      IN PROGRESS
           ↓
       CONVERGE
           ↓
   REVIEW / CONVERGE
      ┌────┴────┐
 falta trabajo  convergió
      ↓             ↓
 IN PROGRESS       DONE
```

---

# Relaciones obligatorias

## SPEC → EPIC

Cuando una Spec pertenece a una EPIC, registrar la relación de forma explícita.

Ejemplo:

```text
Parent EPIC: Gestión de equipos y ligas
```

## USER STORY → SPEC

Toda User Story debe permitir identificar su Spec.

Ejemplo:

```text
Spec: 007-team-league-domain
User Story: US2
```

## IMPLEMENTACIÓN → USER STORY

Cuando el trabajo se implemente, mantener referencia suficiente para identificar qué código corresponde a la User Story.

Cuando aplique, vincular:

- branch;
- commit;
- pull request.

---

# Identificadores recomendados

Usar identificadores estables.

Ejemplos:

```text
[EPIC] Gestión de equipos y ligas
[SPEC-007] Team / League Domain
[SPEC-007][US1] Consultar equipos
[SPEC-007][US2] Consultar ligas
```

Evitar depender únicamente del nombre textual.

El identificador de Spec debe coincidir con el utilizado por Spec Kit siempre que sea posible.

---

# Contenido recomendado de una tarjeta SPEC

Una tarjeta de Spec debería incluir:

```text
Tipo: SPEC
ID: 007
Feature: team-league-domain
EPIC: <epic relacionada>
Estado SDD: <fase actual>
Branch: <branch correspondiente>
Ruta: specs/007-team-league-domain/
```

Opcionalmente:

```text
User Stories:
- US1
- US2
- US3
```

No duplicar el contenido completo de `spec.md`.

---

# Contenido recomendado de una tarjeta User Story

Una tarjeta User Story debería incluir:

```text
Tipo: USER STORY
Spec: 007-team-league-domain
ID: US1
Estado SDD: READY
```

Puede incluir un resumen breve de comportamiento.

Los criterios detallados continúan viviendo en `spec.md`.

Cuando corresponda:

```text
Branch:
Commit:
PR:
Bloqueos:
```

---

# Sincronización por eventos

Actualizar Trello cuando ocurran eventos relevantes del SDD.

## Spec creada

Cuando se crea una nueva feature:

```text
crear o actualizar tarjeta SPEC
estado → SPECIFICATION
```

## SPECIFY completado

Actualizar:

```text
Estado SDD: SPECIFY COMPLETED
```

No pasar a `READY`.

## CLARIFY completado

Actualizar:

```text
Estado SDD: CLARIFY COMPLETED
```

## PLAN completado

Actualizar:

```text
Estado SDD: PLAN COMPLETED
```

## CHECKLIST completado

Actualizar:

```text
Estado SDD: CHECKLIST COMPLETED
```

## TASKS generadas

Actualizar:

```text
Estado SDD: TASKS GENERATED
```

No mover a `READY`.

## ANALYZE con findings

Mantener en:

```text
SPECIFICATION
```

Registrar solamente los bloqueos relevantes.

No copiar todo el reporte si no aporta valor operativo.

## ANALYZE aprobado

Mover las User Stories alcanzadas por el análisis a:

```text
READY
```

Actualizar la Spec:

```text
Estado SDD: ANALYZE PASSED
```

## IMPLEMENT iniciado

Mover la User Story a:

```text
IN PROGRESS
```

## CONVERGE iniciado

Mover o mantener la User Story en:

```text
REVIEW / CONVERGE
```

## CONVERGE detecta faltantes

Mover nuevamente a:

```text
IN PROGRESS
```

si requiere más implementación.

## CONVERGE aprobado

Mover a:

```text
DONE
```

---

# Estado de la SPEC

La tarjeta SPEC puede tener un estado diferente de sus User Stories.

Ejemplo:

```text
SPEC-007
├── US1 → DONE
├── US2 → IN PROGRESS
└── US3 → READY
```

La SPEC completa no está `DONE`.

## Regla

Una SPEC puede pasar a `DONE` cuando:

- todas las User Stories requeridas están `DONE`;
- `CONVERGE` de la feature no detecta faltantes materiales;
- no quedan Tasks necesarias para cumplir la Spec.

---

# Estado de la EPIC

Una EPIC puede contener múltiples Specs.

Ejemplo:

```text
EPIC
├── SPEC-007 → DONE
├── SPEC-008 → IN PROGRESS
└── SPEC-009 → BACKLOG
```

La EPIC no está terminada.

## Regla

Una EPIC puede considerarse `DONE` solamente cuando todas las Specs que forman parte de su alcance final están completas.

Si el alcance de la EPIC cambia, actualizar explícitamente su composición.

---

# Manejo de cambios de Spec

Si `spec.md` cambia de forma material después de haber llegado a `READY`:

```text
READY
  ↓
SPECIFICATION
```

cuando el cambio invalide el análisis previo.

Luego:

```text
revisar PLAN
revisar TASKS
ANALYZE nuevamente
```

No mantener una tarjeta en `READY` cuando el gate que justificaba ese estado dejó de ser válido.

---

# Manejo de cambios de Plan

Si `plan.md` cambia materialmente y afecta las Tasks:

```text
READY / IN PROGRESS
        ↓
evaluar impacto
        ↓
TASKS
        ↓
ANALYZE
```

Si el análisis previo quedó invalidado, actualizar Trello acorde.

---

# Trabajo descubierto durante implementación

Si durante `IMPLEMENT` aparece nuevo trabajo necesario para satisfacer la Spec:

1. no crear automáticamente una nueva User Story si el comportamiento ya pertenece a una existente;
2. actualizar `tasks.md` cuando sea trabajo técnico;
3. actualizar `spec.md` si realmente apareció un requisito nuevo;
4. ejecutar nuevamente los gates necesarios;
5. actualizar Trello para reflejar el retroceso.

---

# Nuevas ideas durante implementación

Si aparece una idea que no pertenece al alcance actual:

```text
NO agregar silenciosamente a la Spec actual
```

Registrar como:

```text
BACKLOG / futura EPIC / futura SPEC
```

según corresponda.

Evitar scope creep.

---

# Bloqueos

Un bloqueo operativo puede registrarse en Trello.

Ejemplos:

- dependencia externa;
- decisión pendiente;
- servicio no disponible;
- acceso pendiente;
- PR bloqueado.

El bloqueo no debe reemplazar una clarificación funcional.

Si el problema es de requisitos, resolverlo mediante el flujo SDD correspondiente.

---

# Branches, commits y PRs

Cuando existan, mantener trazabilidad con el trabajo implementado.

Ejemplo recomendado:

```text
Spec: 007-team-league-domain
Branch: feat/007-team-league-domain
PR: <referencia>
```

Los commits individuales pueden vincularse cuando aporten valor.

No es obligatorio copiar cada commit en Trello si la branch o PR ya ofrece trazabilidad suficiente.

---

# Evitar duplicación

No copiar indiscriminadamente en Trello:

- todo `spec.md`;
- todo `plan.md`;
- todo `tasks.md`;
- todas las checklist;
- todos los findings de análisis;
- todos los commits.

Trello debe ofrecer navegación y estado.

Spec Kit debe conservar el detalle de especificación.

Git debe conservar el historial de implementación.

---

# Regla de consistencia

El estado de Trello debe derivar del estado real del proyecto.

Nunca hacer:

```text
mover tarjeta primero
→ asumir que el proyecto está en ese estado
```

Hacer:

```text
estado real cambia
→ validar gate
→ actualizar Trello
```

---

# Detección de inconsistencias

Cuando se inspeccione el tablero, detectar casos como:

```text
US en READY
pero ANALYZE tiene findings bloqueantes
```

Resultado:

```text
Trello está desactualizado.
Mover la US fuera de READY.
```

Otro caso:

```text
US en DONE
pero CONVERGE detecta tareas pendientes
```

Resultado:

```text
La US no está terminada.
Restaurar estado operativo correcto.
```

Otro caso:

```text
US existe en Trello
pero no existe en ninguna Spec
```

Resultado:

```text
No tratarla como User Story ejecutable.
Clasificarla como idea/backlog o crear la Spec correspondiente primero.
```

---

# Algoritmo de sincronización

Cuando se solicite sincronizar Trello:

```text
1. Identificar la EPIC, Spec o User Story afectada.
2. Leer el estado real de Spec Kit.
3. Determinar la fase SDD usando sdd-orchestration.
4. Comparar el estado real con Trello.
5. Detectar tarjetas faltantes, sobrantes o desactualizadas.
6. Corregir relaciones.
7. Corregir estados.
8. Mantener enlaces de trazabilidad.
9. No modificar requisitos desde Trello.
10. Informar brevemente qué cambió.
```

---

# Matriz SDD → Trello

Usar como guía:

| Estado SDD | Estado Trello |
|---|---|
| IDEA / EPIC | BACKLOG |
| SPECIFY | SPECIFICATION |
| CLARIFY | SPECIFICATION |
| PLAN | SPECIFICATION |
| CHECKLIST | SPECIFICATION |
| TASKS | SPECIFICATION |
| ANALYZE con findings | SPECIFICATION |
| ANALYZE aprobado | READY |
| IMPLEMENT | IN PROGRESS |
| CONVERGE | REVIEW / CONVERGE |
| CONVERGE con faltantes | IN PROGRESS |
| CONVERGE aprobado | DONE |

---

# Comportamientos prohibidos

Nunca:

- crear User Stories ejecutables sin una Spec que las defina;
- mover una User Story a `READY` antes de `ANALYZE`;
- usar Trello como fuente primaria de requisitos;
- marcar una User Story `DONE` antes de `CONVERGE`;
- duplicar todo `tasks.md` como tarjetas sin una necesidad real;
- mantener una tarjeta en un estado que contradiga Spec Kit;
- crear requisitos nuevos únicamente desde comentarios de Trello;
- asumir que una EPIC completa equivale a una única Spec;
- marcar una Spec como terminada mientras tenga User Stories pendientes;
- ocultar retrocesos de estado cuando un artefacto upstream cambió.

---

# Comportamiento esperado del agente

Cuando esta skill esté activa, informar de forma concisa la trazabilidad relevante.

Ejemplo:

```text
SPEC: 007-team-league-domain
Fase SDD: ANALYZE
Analyze: PASSED

Trello:
- SPEC-007 → SPECIFICATION → READY
- US1 → READY
- US2 → READY
- US3 → READY
```

Ejemplo con inconsistencia:

```text
US2 figura READY en Trello.
ANALYZE reporta un finding bloqueante para US2.

Estado correcto: SPECIFICATION
Acción: retirar US2 de READY hasta resolver el finding y repetir ANALYZE.
```

El objetivo es que Trello sea una representación confiable, liviana y trazable del estado real del desarrollo, sin convertirse en una segunda fuente de verdad paralela a Spec Kit.

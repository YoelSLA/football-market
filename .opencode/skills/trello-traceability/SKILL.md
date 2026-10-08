---
name: trello-traceability
description: Mantiene la trazabilidad operacional en Trello entre EPICs, Features/Specs y User Stories de Football Market. Usar esta skill para crear, actualizar, mover y relacionar tarjetas, persistir evidencia de gates humanos, detectar desincronización y reportar errores de sincronización, siguiendo las transiciones decididas por sdd-orchestration.
compatibility: opencode
metadata:
  project: football-market
  tracker: trello
  methodology: spec-driven-development
  depends-on: sdd-orchestration
  role: operational
---

# Trazabilidad Trello

## Propósito

Mantener Trello sincronizado con el estado real del ciclo de Spec-Driven Development de Football Market.

Esta skill es **operacional**. Define:

- qué representa cada tipo de tarjeta;
- cómo se relacionan EPICs, Features/Specs y User Stories;
- qué columnas canónicas existen y qué significa cada una;
- cómo crear, actualizar y mover tarjetas;
- cómo persistir evidencia operacional de gates humanos;
- cómo detectar desincronización;
- cómo reportar errores de sincronización.

Esta skill **no define requisitos funcionales** ni **no redefine el lifecycle SDD**.

---

# 1. Separación de responsabilidades

## `sdd-orchestration` es la autoridad del workflow

Decide:

- fase SDD actual;
- gates;
- `READY`;
- autorizaciones de implementación;
- invalidaciones;
- `CODE REVIEW`;
- `TESTING`;
- `DONE` de User Stories;
- `CONVERGE` final;
- `DONE` de Feature;
- retrocesos de estado;
- impacto de cambios upstream.

## `trello-traceability` es la capa operacional

```text
sdd-orchestration
        ↓
decide transición
        ↓
trello-traceability
        ↓
crea / actualiza / mueve / relaciona
        ↓
Trello
```

Debe:

- representar el estado decidido por `sdd-orchestration`;
- crear y actualizar tarjetas;
- mover tarjetas;
- mantener relaciones;
- persistir evidencia operacional;
- detectar desincronización;
- reportar errores de sincronización.

## Regla de conflicto

Si existe conflicto entre ambas skills respecto del lifecycle, **prevalece `sdd-orchestration`**.

Nunca redefinir aquí las condiciones para alcanzar `READY`, la validez de un gate ni el criterio de `DONE`.

---

# 2. Precedencia de fuentes

Alineada con `sdd-orchestration`:

```text
Constitution
    ↓
spec.md
    ↓
clarifications incorporated into spec
    ↓
plan / design artifacts
    ↓
tasks.md
    ↓
implementation
    ↓
Trello
```

Trello representa:

```text
estado operacional
+
trazabilidad
+
evidencia de gates
```

**No requisitos.**

Si una tarjeta contradice un artefacto de Spec Kit, prevalece el artefacto y la tarjeta se corrige.

## Principio fundamental

Nunca usar Trello como fuente primaria de requisitos. Una tarjeta puede resumir una necesidad o reflejar estado, pero no reemplaza `spec.md`, criterios de aceptación, decisiones de clarificación, `plan.md`, `tasks.md`, contratos ni documentación técnica.

Si una tarjeta contradice un artefacto de Spec Kit, prevalece el artefacto correspondiente.

La tarjeta debe corregirse para reflejar la fuente de verdad.

---

# 3. Modelo de trazabilidad

```text
EPIC
  ↓
FEATURE / SPEC
  ↓
USER STORY
  ↓
TASK  (principalmente en tasks.md)
  ↓
IMPLEMENTACIÓN
  ↓
COMMIT / PR
```

La trazabilidad mínima debe permitir responder:

```text
¿A qué EPIC pertenece esta Feature?
¿Qué User Stories pertenecen a esta Feature?
¿Qué Tasks implementan cada User Story?
¿Qué cambio de código implementó ese trabajo?
¿Qué gates humanos se aprobaron y cuál fue su evidencia?
¿En qué estado real está actualmente?
```

---

# 4. EPIC

## Definición

Una EPIC representa un objetivo amplio de producto, dominio o capacidad.

```text
EPIC: Gestión de equipos y ligas
```

Una EPIC puede contener varias Features/Specs:

```text
EPIC
  ├── SPEC 007 - Team / League Domain
  ├── SPEC 008 - Team Synchronization
  └── SPEC 009 - League Navigation
```

## Reglas

- **No convertir automáticamente una EPIC en Spec.** La EPIC puede existir antes de que sus Specs estén definidas.
- Una EPIC puede vivir en `BACKLOG` mientras sus Specs no fueron creadas.
- **No marcar automáticamente una EPIC DONE** por completar una sola Feature.

Una EPIC puede considerarse `DONE` solamente cuando todas las Features que forman parte de su alcance final están `DONE`.

Si el alcance de la EPIC cambia, actualizar explícitamente su composición.

## Contenido mínimo

- identificador o nombre;
- objetivo general;
- estado general;
- Features relacionadas;
- enlaces relevantes.

No copiar dentro de la EPIC todos los requisitos detallados de cada Spec.

---

# 5. FEATURE / SPEC

## Definición

Una tarjeta de Feature/Spec representa una feature concreta gestionada por Spec Kit.

Debe corresponder a una carpeta real de feature, por ejemplo:

```text
specs/007-team-league-domain/
```

## Relación obligatoria

Toda tarjeta de Feature/Spec debe apuntar a:

- su EPIC padre, si existe;
- su directorio o identificador de Spec Kit;
- sus User Stories.

## Cuándo crearla

Crear la tarjeta cuando la feature ya tenga identidad propia dentro del flujo de Spec Kit.

No crear una Spec ejecutable solamente porque existe una idea vaga en backlog.

## Contenido recomendado

```text
Tipo: SPEC
ID: 007
Feature: team-league-domain
EPIC: <epic relacionada>
SDD phase: PLAN
Trello column: SPECIFYING
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

# 6. USER STORY

## Definición

Una User Story representa una unidad de comportamiento definida dentro de `spec.md`.

Debe derivar de una Spec existente.

```text
SPEC 007-team-league-domain
 ├── US1 - Consultar equipo
 ├── US2 - Consultar liga
 └── US3 - Relacionar jugador con equipo
```

## Creación durante SPECIFYING

Las tarjetas de User Stories se crean **durante `SPECIFYING`**, tan pronto como las historias estén suficientemente identificadas.

Pueden existir en Trello para aportar visibilidad, pero deben mantenerse claramente como **no ejecutables** hasta que la Feature alcance `READY`.

Si en ese momento una US todavía no es identificable con certeza, esperar a `CLARIFY` y crearla entonces. No crear US ficticias.

## Regla crítica

Nunca crear una User Story operativa en Trello si no existe una User Story equivalente o una definición funcional suficientemente clara dentro de la Spec correspondiente.

Trello puede contener ideas futuras, pero deben identificarse como ideas o backlog no especificado. No tratarlas como trabajo listo.

## Contenido recomendado

```text
Tipo: USER STORY
Spec: 007-team-league-domain
ID: US1
SDD phase: IMPLEMENT
Trello column: IN PROGRESS
```

Puede incluir un resumen breve de comportamiento. Los criterios detallados continúan viviendo en `spec.md`.

Cuando corresponda:

```text
Branch:
Commit:
PR:
Bloqueos:
```

---

# 7. TASK

## Dónde viven las Tasks

Las Tasks técnicas viven principalmente en:

```text
tasks.md
```

Por defecto **no** crear una tarjeta Trello por cada Task técnica: la granularidad de `tasks.md` puede ser demasiado fina para el tablero.

Crear tarjetas de Task en Trello solamente cuando exista una necesidad operativa real, por ejemplo:

- coordinación entre personas;
- dependencia externa;
- trabajo bloqueado;
- seguimiento independiente;
- tarea de larga duración;
- actividad no representada adecuadamente por una User Story.

## Lo que Trello nunca representa

No crear tarjetas para gates humanos del workflow:

```text
User performs Code Review
User runs tests
User approves Testing
```

`CODE REVIEW` y `TESTING` son gates administrados por `sdd-orchestration`, no Tasks técnicas.

## Evitar duplicación

No duplicar indiscriminadamente en Trello:

- todo `spec.md`;
- todo `plan.md`;
- todo `tasks.md`;
- todas las checklists;
- todos los findings de análisis;
- todos los commits;
- el `Code Review Report` completo ni el `Testing Report` completo.

Trello debe ofrecer navegación y estado. Spec Kit conserva el detalle. Git conserva el historial.

---

# 8. Columnas canónicas

Las columnas reales y únicas del workflow son:

```text
BACKLOG
SPECIFYING
READY
IN PROGRESS
CODE REVIEW
TESTING
DONE
```

Eliminar del modelo anterior:

```text
SPECIFICATION
REVIEW / CONVERGE
```

**No** crear una columna `CONVERGE`.

**No** asumir que los nombres pueden variar: estas siete columnas son canónicas.

---

# 9. BACKLOG

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

---

# 10. SPECIFYING

## Significado

`SPECIFYING` representa en Trello **todas** estas fases internas:

```text
SPECIFY
CLARIFY
PLAN
CHECKLIST
TASKS
ANALYZE
```

Todas pertenecen operacionalmente a `SPECIFYING`.

## Fase detallada como metadata

La fase SDD detallada puede persistirse como metadata / campo `SDD phase` de la tarjeta, pero la columna permanece `SPECIFYING`:

```text
Trello column: SPECIFYING
SDD phase: PLAN
```

La columna es una **proyección**; la fase SDD es el estado real.

## Reglas

- Las US creadas en esta etapa **no son ejecutables**.
- `TASKS generadas ≠ READY`.
- No mover a `READY` desde esta columna por decisión propia: solo cuando `sdd-orchestration` determine que la Feature alcanzó `READY`.
- Al alcanzar `READY`: `Feature → READY` y `User Stories válidas → READY`.

---

# 11. READY

## Significado

`READY` es un gate a nivel **Feature** decidido por `sdd-orchestration`.

Esta skill **no** redefine las condiciones necesarias para alcanzar `READY`. Solo las refleja.

## Aplicación

Cuando `sdd-orchestration` determine que la Feature alcanzó `READY`:

```text
Feature → READY
User Stories válidas → READY
```

## `READY ≠ autorización para implementar`

Una User Story en `READY` todavía puede **no** estar autorizada para implementación.

```text
READY  ≠  autorización para implementar
```

La autorización humana es administrada por `sdd-orchestration`.

Una US autorizada **permanece en `READY`** hasta que realmente comience su implementación.

## `IMPLEMENT AUTHORIZATION`

Cuando el usuario autorice una o varias User Stories, `sdd-orchestration` lo determina y esta skill persiste la evidencia como comentario estructurado **en cada tarjeta de US autorizada**, para que la trazabilidad de cada US sea autosuficiente:

```text
[SDD GATE]
Gate: IMPLEMENT AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

```text
IMPLEMENT AUTHORIZATION APPROVED  ≠  IN PROGRESS
```

La autorización solo significa que la US **puede** ser implementada. La US permanece en `READY` hasta que `IMPLEMENT` realmente comience; solo entonces `READY → IN PROGRESS`.

## Invalidación de la autorización

Cuando `sdd-orchestration` determine que una autorización quedó invalidada por impacto de un cambio upstream, registrar un **nuevo comentario**:

```text
[SDD GATE]
Gate: IMPLEMENT AUTHORIZATION
Result: INVALIDATED
Reason: upstream change affected this User Story
User Story: US2
```

- **No borrar** la evidencia histórica anterior: el historial debe mostrar la autorización previa y luego la nueva.
- Una US autorizada **no afectada** conserva su autorización y no requiere comentario nuevo.
- Solo registrar `INVALIDATED` cuando `sdd-orchestration` lo haya determinado. Esta skill **no decide** la invalidación.

---

# 12. IN PROGRESS

## User Story

```text
READY → IN PROGRESS
```

cuando `sdd-orchestration` indique que comenzó realmente `IMPLEMENT`.

No mover una US a `IN PROGRESS` solo porque fue autorizada, abierta o inspeccionada.

## Feature

Feature IN PROGRESS significa que existe al menos una US cuyo IMPLEMENT comenzó y la Feature todavía no cumple las condiciones para TESTING final. CODE REVIEW y TESTING de sus US mantienen significado propio.

No intentar forzar el mismo estado para Feature y User Stories.

## Estado distribuido válido

```text
Feature: IN PROGRESS

US1 → DONE
US2 → TESTING
US3 → CODE REVIEW
US4 → IN PROGRESS
US5 → READY
```

---

# 13. CODE REVIEW

## Significado

`CODE REVIEW` es un **gate humano obligatorio** administrado por `sdd-orchestration`.

## Transiciones

```text
IN PROGRESS → CODE REVIEW     (implementación de la US completa)
CODE REVIEW → IN PROGRESS     (review rechazado)
```

## Evidencia operacional

Al entrar a `CODE REVIEW`, la US queda esperando revisión humana: la columna ya lo refleja, no hace falta un evento previo.

Al resolverse el gate, persistir comentario estructurado (§21), sin copiar el `Code Review Report` completo:

```text
[SDD GATE]
Gate: CODE REVIEW
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

Si fue rechazado:

```text
[SDD GATE]
Gate: CODE REVIEW
Result: REJECTED
Validated by: HUMAN
User Story: US2
```

y mover la US a `IN PROGRESS`.

## Invalidación

Si el código o los tests de la US cambian después de una aprobación, `sdd-orchestration` invalida el gate y esta skill refleja la invalidación correspondiente, moviendo la US de vuelta a `CODE REVIEW` para un nuevo gate. No borrar el comentario de aprobación anterior: el historial conserva la secuencia.

---

# 14. TESTING (User Story)

## Significado

Para una **tarjeta User Story**, `TESTING` significa:

```text
human review of tests
+
human execution of tests
+
human approval
```

**No** significa `CONVERGE`.

## Transición

```text
CODE REVIEW aprobado → TESTING
```

## Evidencia operacional

Comentario estructurado (§21):

```text
[SDD GATE]
Gate: TESTING
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

## TESTING N/A

```text
[SDD GATE]
Gate: TESTING
Result: N/A
Justification: <resumen>
Validated by: HUMAN
User Story: US2
```

## Bloqueos en TESTING

La US **permanece en `TESTING`** y se registra el bloqueo:

```text
Status: BLOCKED BY DEPENDENCY
Blocked by: <US / Feature / servicio externo>
```

Para cualquier otro bloqueo:

```text
Status: BLOCKED
Reason: <environment | configuration | external service | access | ...>
```

Nunca convertir cada causa posible en un estado nuevo.

Si `TESTING` requiere cambio de código:

```text
TESTING → IN PROGRESS
```

y la US vuelve a recorrer `IN PROGRESS → CODE REVIEW → TESTING`.

---

# 15. DONE

## User Story

Una User Story llega a `DONE` cuando `sdd-orchestration` determina que sus gates fueron satisfechos:

```text
IMPLEMENT complete
        ↓
CODE REVIEW aprobado
        ↓
TESTING aprobado
        ↓
DONE
```

`trello-traceability` **ejecuta** la transición. **No decide** por sí misma si los gates están satisfechos.

Regla eliminada: `CONVERGE PASSED → DONE` **no aplica** a User Stories. `CONVERGE` no es un estado propio de una US.

## Feature / SPEC

Una Feature pasa:

```text
TESTING → DONE
```

únicamente cuando el orquestador determine preparación tras CONVERGE final exitoso y el usuario otorgue FEATURE COMPLETION AUTHORIZATION explícita, vigente y persistida. CONVERGE no autoriza DONE por sí mismo; Feature permanece TESTING mientras espera cierre.

`trello-traceability` **no** define por sí misma `CONVERGE PASSED = DONE` como regla general: recibe la transición desde el orquestador y la persiste.

## Insuficiente para `DONE`

No alcanza con:

- compilar;
- haber hecho commit;
- haber abierto PR;
- haber completado las Tasks iniciales;
- tener `tasks.md` completo;
- haber escrito tests parciales.

---

# 16. TESTING con doble semántica

La columna `TESTING` tiene **doble semántica según el tipo de tarjeta**. Esto es **intencional**:

| Tipo de tarjeta | Significado de `TESTING` |
| --- | --- |
| Feature / SPEC | `CONVERGE` final / validación final |
| User Story | revisión humana de tests + ejecución humana de tests |

Cuando todas las User Stories estén `DONE` y `sdd-orchestration` lo determine:

```text
Feature: IN PROGRESS → TESTING
```

En ese momento `sdd-orchestration` ejecuta el `CONVERGE` final.

No crear una columna `CONVERGE`.

---

# 17. CONVERGE

`CONVERGE` pertenece **exclusivamente** a la validación final de **Feature**.

**No existe** como columna de Trello.

Si `CONVERGE` detecta trabajo faltante y `sdd-orchestration` decide:

```text
Feature → IN PROGRESS
US afectadas → IN PROGRESS
```

esta skill refleja esas transiciones.

Después esas US volverán a recorrer:

```text
IN PROGRESS → CODE REVIEW → TESTING → DONE
```

según indique `sdd-orchestration`.

Si CONVERGE final es exitoso, el orquestador solicita FEATURE COMPLETION AUTHORIZATION; solo tras aprobación humana persistida ordena Feature TESTING → DONE. La corrección documental, si corresponde, ocurre fuera del comando diagnóstico/append-only, nunca por esta skill.

---

# 18. Lifecycle Trello

```text
IDEA / EPIC
      ↓
BACKLOG
      ↓
Feature/Spec creada
      ↓
SPECIFYING
      ├── SPECIFY
      ├── CLARIFY
      ├── PLAN
      ├── CHECKLIST
      ├── TASKS
      └── ANALYZE
            ↓
       ANALYZE aprobado + Feature READY
            ↓
    Feature → READY
    US válidas → READY
            ↓
    IMPLEMENT iniciado para US
            ↓
    US → IN PROGRESS
    Feature → IN PROGRESS
            ↓
    IMPLEMENT completado
            ↓
    US → CODE REVIEW
            ↓
    CODE REVIEW aprobado  → persistir evidencia → US → TESTING
    CODE REVIEW rechazado → persistir resultado → US → IN PROGRESS
            ↓
    TESTING aprobado → persistir evidencia → US → DONE
            ↓
    Todas las US DONE → Feature → TESTING  (= CONVERGE final)
            ↓
    CONVERGE exitoso → FEATURE COMPLETION AUTHORIZATION humana → persistencia → Feature DONE
```

## Ejemplo de estado distribuido válido

```text
Feature: IN PROGRESS

US1 → DONE
US2 → TESTING
US3 → CODE REVIEW
US4 → IN PROGRESS
US5 → READY
```

---

# 19. Matriz SDD → Trello

| Situación | Trello |
|---|---|
| IDEA / EPIC | BACKLOG |
| SPECIFY | SPECIFYING |
| CLARIFY | SPECIFYING |
| PLAN | SPECIFYING |
| CHECKLIST | SPECIFYING |
| TASKS | SPECIFYING |
| ANALYZE | SPECIFYING |
| Feature READY | READY |
| US esperando implementación | READY |
| US IMPLEMENT | IN PROGRESS |
| US esperando revisión humana | CODE REVIEW |
| US Code Review aprobado | TESTING |
| US TESTING bloqueado | TESTING |
| US Testing aprobado | DONE |
| Feature con US activas | IN PROGRESS |
| Feature final CONVERGE | TESTING |
| Feature CONVERGE exitoso, autorización final pendiente | TESTING |
| Feature autorización final aprobada y persistida tras CONVERGE | DONE |

**No volver a introducir `REVIEW / CONVERGE`.**

---

# 20. Sincronización por eventos

Actualizar Trello cuando ocurran los eventos del SDD, **siguiendo siempre las decisiones de `sdd-orchestration`**.

| Evento | Acción operacional |
|---|---|
| Feature/Spec creada | crear o actualizar tarjeta SPEC → `SPECIFYING` |
| US identificada durante `SPECIFYING` | crear / actualizar tarjeta US en `SPECIFYING` |
| US renombrada, agregada, eliminada, dividida o ajustada | sincronizar la tarjeta correspondiente |
| `ANALYZE` aprobado + Feature `READY` | `Feature → READY`, `US válidas → READY` |
| US autorizada para implementación | persistir `[SDD GATE] IMPLEMENT AUTHORIZATION / APPROVED` en cada US; **la US permanece en `READY`** |
| Autorización de US invalidada por impacto | persistir `[SDD GATE] IMPLEMENT AUTHORIZATION / INVALIDATED` como comentario nuevo, sin borrar el historial |
| `IMPLEMENT` iniciado para US | `US → IN PROGRESS`, `Feature → IN PROGRESS` |
| `IMPLEMENT` completado | `US → CODE REVIEW` |
| `CODE REVIEW` aprobado | persistir evidencia, `US → TESTING` |
| `CODE REVIEW` rechazado | persistir resultado, `US → IN PROGRESS` |
| `TESTING` aprobado | persistir evidencia, `US → DONE` |
| `TESTING` con dependencia pendiente | `US` permanece `TESTING`, registrar `BLOCKED BY DEPENDENCY` |
| `TESTING` requiere cambio de código | `US → IN PROGRESS` |
| `TESTING N/A` aprobado | persistir evidencia `N/A` |
| Todas las US `DONE` | `Feature → TESTING` |
| `CONVERGE` final exitoso | Feature permanece TESTING, espera autorización final |
| FEATURE COMPLETION AUTHORIZATION aprobada | comentario estructurado en Feature; mover DONE solo por decisión del orquestador |
| Autorización final invalidada por impacto | nuevo comentario INVALIDATED; reflejar estado ordenado, conservar historial |
| `CONVERGE` detecta implementación faltante | `Feature → IN PROGRESS`, `US afectadas → IN PROGRESS` |
| Cambio upstream invalida `READY` | reflejar retrocesos indicados por el orquestador |

## Transiciones dentro de SPECIFYING

Al avanzar de fase interna, actualizar el campo `SDD phase` de la tarjeta. La columna permanece `SPECIFYING`:

```text
SPECIFY  → SDD phase: SPECIFY  | column: SPECIFYING
CLARIFY  → SDD phase: CLARIFY  | column: SPECIFYING
PLAN     → SDD phase: PLAN     | column: SPECIFYING
TASKS    → SDD phase: TASKS    | column: SPECIFYING
ANALYZE  → SDD phase: ANALYZE  | column: SPECIFYING
```

---

# 21. Evidencia humana

## Qué se persiste

Persistir evidencia operacional de:

- `IMPLEMENT AUTHORIZATION` (aprobada / invalidada);
- `FEATURE COMPLETION AUTHORIZATION` (aprobada / invalidada);
- `CODE REVIEW` aprobado o rechazado;
- `TESTING` aprobado;
- `TESTING` `N/A`;
- bloqueos y dependencias bloqueantes;
- cualquier otra validación humana que `sdd-orchestration` delegue.

## Mecanismo: comentario estructurado

La evidencia se persiste como **comentario estructurado en la tarjeta correspondiente**.

- **No** usar Custom Fields como requisito.
- **No** modificar la descripción principal de la tarjeta cada vez que se aprueba un gate.

Se usan comentarios porque se necesita conservar el historial de eventos y aprobaciones.

## Formato canónico

Todos los eventos de gate usan el prefijo `[SDD GATE]` y los mismos campos.

```text
[SDD GATE]
Gate: CODE REVIEW
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

```text
[SDD GATE]
Gate: TESTING
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

```text
[SDD GATE]
Gate: TESTING
Result: N/A
Justification: <resumen>
Validated by: HUMAN
User Story: US2
```

```text
[SDD GATE]
Gate: IMPLEMENT AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
User Story: US2
```

Resultados admitidos por gate:

| Gate | `Result` válidos |
| --- | --- |
| `IMPLEMENT AUTHORIZATION` | `APPROVED`, `INVALIDATED` |
| `FEATURE COMPLETION AUTHORIZATION` | `APPROVED`, `INVALIDATED` |
| `CODE REVIEW` | `APPROVED`, `REJECTED` |
| `TESTING` | `APPROVED`, `N/A` |

CODE REVIEW y TESTING también admiten eventos INVALIDATED con Reason cuando el orquestador determine impacto sobre alcance aprobado. Son eventos nuevos, no aprobaciones humanas: no añadir Validated by: HUMAN ficticio ni borrar historial. Feature/US afectados se identifican explícitamente.

## Límite de contenido

Autorización final se registra en tarjeta Feature:

```text
[SDD GATE]
Gate: FEATURE COMPLETION AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
Feature: SPEC-007
```

Una invalidación determinada por el orquestador usa nuevo comentario con mismo Gate, Result: INVALIDATED, Reason y Feature; no incluye aprobación humana ficticia. No borrar antecedentes ni inferir decisiones desde el comentario. Los gates de US usan User Story; el gate final usa Feature.

No copiar el `Code Review Report` ni el `Testing Report` completos al comentario: eso genera duplicación innecesaria.

La evidencia debe ser **suficiente para reconstruir qué gate fue validado y cuál fue su resultado**.

## Bloqueos

Los bloqueos no son gates. Se registran con el vocabulario canónico pequeño:

```text
Status: BLOCKED BY DEPENDENCY
Blocked by: <US / Feature / servicio externo>
```

o, para cualquier otra causa:

```text
Status: BLOCKED
Reason: <environment | configuration | external service | access | ...>
```

```text
estado  = clasificación estable
reason  = causa concreta
```

Nunca convertir cada causa posible en un estado nuevo.

## Regla

No **interpretar** esa evidencia para inventar decisiones nuevas.

```text
El orquestador decide el gate.
Trello registra el resultado.
```

No derivar de la evidencia una transición que `sdd-orchestration` no haya ordenado. En particular, un comentario `[SDD GATE]` en Trello **no autoriza**, **no invalida** y **no crea** ningún gate por sí mismo.

---

# 22. Cambios upstream e invalidaciones

Esta skill **no decide** qué queda invalidado: refleja la decisión de `sdd-orchestration`.

## Cambio de Spec

Si `spec.md` cambia materialmente, `sdd-orchestration` puede invalidar `READY`. Esta skill refleja el retroceso indicado:

```text
Feature → SPECIFYING (si corresponde)
US afectadas → SPECIFYING
```

## Cambio de Plan / Tasks

Si `plan.md` o `tasks.md` cambian materialmente, reflejar la re-evaluación de gates indicada por el orquestador.

## Cambio de Constitución

Si la Constitución cambia y afecta la Feature, `sdd-orchestration` invalida `READY`. Reflejar los retrocesos indicados.

## Invalidación de gates

| Cambio | Efecto |
|---|---|
| código cambiado con impacto sobre alcance aprobado | Reflejar invalidación de CODE REVIEW y transición decididas por el orquestador |
| tests cambiados con impacto sobre alcance aprobado | Reflejar invalidación de CODE REVIEW/TESTING afectadas |
| requisitos cambiados | evaluar US / artefactos afectados, invalidar solo el estado downstream afectado |

## Regla de alcance

Aplicar las transiciones **solamente sobre las entidades afectadas**.

**No resetear indiscriminadamente todo el tablero.**

Una US no afectada conserva su estado, incluidas sus aprobaciones persistidas.

Cambios sin impacto sobre el alcance evaluado conservan gates vigentes: no invalidar por mera existencia de cambios. Dependency ≠ Blocker: dependencia técnica identificada no invalida READY por sí misma.

---

# 23. Feature y User Story son entidades diferentes

La tarjeta Feature puede tener un estado diferente de sus User Stories. Esta distinción es explícita y debe reforzarse.

Ejemplo válido:

```text
Feature: IN PROGRESS

US1 → DONE
US2 → TESTING
US3 → CODE REVIEW
US4 → IN PROGRESS
US5 → READY
```

## Regla

**No inferir el estado de la Feature únicamente mirando una US individual**, ni forzar el mismo estado a todas las US de una Feature.

El estado de la Feature se actualiza por los eventos que lo gobiernan:

```text
Feature → READY         cuando la Feature alcanza READY
Feature → IN PROGRESS   cuando comienza la primera US
Feature → TESTING       cuando todas las US están DONE
Feature → DONE          tras CONVERGE y autorización final humana vigente/persistida
```

---

# 24. Relaciones obligatorias

## FEATURE / SPEC → EPIC

```text
Parent EPIC: Gestión de equipos y ligas
```

## USER STORY → SPEC

```text
Spec: 007-team-league-domain
User Story: US2
```

## IMPLEMENTACIÓN → USER STORY

Mantener referencia suficiente para identificar qué código corresponde a la US. Cuando aplique, vincular:

- branch;
- commit;
- pull request.

No es obligatorio copiar cada commit si la branch o el PR ya ofrece trazabilidad suficiente.

---

# 25. Identificadores recomendados

```text
[EPIC] Gestión de equipos y ligas
[SPEC-007] Team / League Domain
[SPEC-007][US1] Consultar equipos
[SPEC-007][US2] Consultar ligas
```

Evitar depender únicamente del nombre textual. El identificador de Spec debe coincidir con el utilizado por Spec Kit siempre que sea posible.

---

# 26. Trabajo descubierto e ideas nuevas

## Trabajo nuevo durante implementación

Si durante `IMPLEMENT` aparece trabajo necesario para satisfacer la Spec:

1. no crear automáticamente una nueva US si el comportamiento ya pertenece a una existente;
2. crear o reabrir la Task técnica en `tasks.md` cuando corresponda;
3. no modificar `spec.md` desde Trello;
4. reflejar el retroceso indicado por `sdd-orchestration`.

## Ideas fuera de alcance

```text
NO agregar silenciosamente a la Spec actual
```

Registrar como:

```text
BACKLOG / futura EPIC / futura SPEC
```

según corresponda. Evitar scope creep.

---

# 27. Bloqueos

Un bloqueo operativo puede registrarse en Trello. Causas frecuentes:

- dependencia externa;
- decisión pendiente;
- servicio no disponible;
- acceso pendiente;
- PR bloqueado.

Un bloqueo **no reemplaza** una clarificación funcional. Si el problema es de requisitos, resolverlo mediante el flujo SDD correspondiente.

## Vocabulario canónico

El estado de bloqueo usa **solo** estos valores:

```text
BLOCKED BY DEPENDENCY     existe específicamente una dependencia pendiente
BLOCKED                   cualquier otra causa
```

Nunca introducir estados nuevos por causa. La causa concreta va en `Reason` / `Blocked by`.

## Bloqueos en gates

Los bloqueos que impiden superar un gate se registran **sin mover la tarjeta fuera de la columna del gate**:

```text
US en TESTING    + Status: BLOCKED BY DEPENDENCY, Blocked by: US4
US en TESTING    + Status: BLOCKED, Reason: environment
US en CODE REVIEW + Status: BLOCKED, Reason: decision pending
```

El bloqueo no es un `[SDD GATE]` y no implica aprobación ni invalidación de gate alguno.

---

# 28. Regla de consistencia

El estado de Trello debe **derivar** del estado real del proyecto.

Nunca hacer:

```text
mover tarjeta primero
→ asumir que el proyecto está en ese estado
```

Hacer:

```text
estado real cambia (decidido por sdd-orchestration)
→ actualizar Trello
```

Nunca interpretar un cambio de Trello como cambio del estado real del proyecto.

---

# 29. Detección de inconsistencias

Al inspeccionar el tablero, contrastar contra el estado decidido por `sdd-orchestration`. Casos a detectar:

```text
US en DONE
pero falta evidencia de TESTING aprobada
```

```text
US en TESTING
pero CODE REVIEW no fue aprobado
```

```text
US en CODE REVIEW
pero implementación todavía figura incompleta
```

```text
Feature en DONE
pero alguna US no está DONE
```

```text
Feature en DONE
pero no existe evidencia de CONVERGE final exitoso
```

```text
Trello dice READY
pero sdd-orchestration indica READY invalidado
```

```text
US en IN PROGRESS
pero no existe comentario [SDD GATE] IMPLEMENT AUTHORIZATION / APPROVED
```

```text
US en IN PROGRESS
pero su autorización figura INVALIDATED sin una nueva autorización posterior
```

```text
US existe en Trello
pero no existe en ninguna Spec
```

```text
tarjeta en BACKLOG
pero ya existe Spec con artefactos vigentes
```

## Corrección

La corrección sigue el estado decidido por `sdd-orchestration`.

Si la corrección requiere una decisión que no puede derivarse de `sdd-orchestration`, **detenerse y reportar**. No resolver silenciosamente.

```text
US en DONE
sin evidencia de TESTING
    → NO asumir DONE válido
    → consultar estado real con sdd-orchestration
    → reflejar la transición que indique
```

---

# 30. Errores de sincronización

Distinguir dos casos.

## 1. Error mecánico

Si no se puede mover o actualizar una tarjeta:

```text
TRELLO OUT OF SYNC
```

Reportar:

- tarjeta afectada;
- estado esperado;
- estado conocido;
- operación fallida.

**No fingir que la sincronización ocurrió.**

Este error no necesariamente bloquea el trabajo técnico: se reintenta o sincroniza cuando sea posible.

## 2. Error persistiendo evidencia humana

Si no se puede persistir evidencia obligatoria de IMPLEMENT AUTHORIZATION, CODE REVIEW, TESTING, FEATURE COMPLETION AUTHORIZATION o sus invalidaciones, reportarlo explícitamente y no habilitar transición dependiente hasta resolverlo.

```text
GATE EVIDENCE NOT PERSISTED
Tarjeta: <id>
Gate: IMPLEMENT AUTHORIZATION | CODE REVIEW | TESTING | FEATURE COMPLETION AUTHORIZATION
```

`sdd-orchestration` decidirá si el gate puede considerarse completamente cerrado.

`trello-traceability` **no debe asumirlo** ni marcar el gate como cerrado.

---

# 31. Algoritmo de sincronización

```text
1. Identificar la EPIC, Feature o User Story afectada.
2. Obtener el estado decidido por sdd-orchestration.
3. Comparar ese estado con Trello.
4. Detectar tarjetas faltantes, sobrantes o desactualizadas.
5. Aplicar las transiciones indicadas, solo sobre las entidades afectadas.
6. Corregir relaciones.
7. Persistir evidencia operacional si corresponde, como comentario estructurado `[SDD GATE]`.
8. Mantener enlaces de trazabilidad.
9. No modificar requisitos desde Trello.
10. Reportar qué cambió y el estado de sincronización.
```

---

# 32. Comportamientos prohibidos

Nunca:

- usar Trello como fuente de requisitos;
- decidir `READY` independientemente de `sdd-orchestration`;
- redefinir las condiciones de `READY`, de los gates o de `DONE`;
- asumir que `READY` autoriza implementación;
- **aprobar, invalidar o derivar un gate por sí mismo** a partir de un comentario `[SDD GATE]` existente;
- mover una US a `IN PROGRESS` solo por haber sido autorizada;
- saltar `CODE REVIEW`;
- saltar `TESTING`;
- persistir evidencia humana inventada;
- usar Custom Fields como requisito para la evidencia de gates;
- reescribir la descripción principal de la tarjeta en cada aprobación de gate;
- borrar evidencia histórica de gates o autorizaciones;
- registrar `IMPLEMENT AUTHORIZATION / INVALIDATED` sin decisión de `sdd-orchestration`;
- introducir estados de bloqueo distintos de `BLOCKED` y `BLOCKED BY DEPENDENCY`;
- interpretar evidencia persistida para derivar decisiones nuevas;
- marcar una US `DONE` solamente porque `tasks.md` está completo;
- marcar una US `DONE` por `CONVERGE`;
- ejecutar `CONVERGE` como estado propio de una US;
- usar `REVIEW / CONVERGE`;
- usar `SPECIFICATION`;
- crear una columna `CONVERGE`;
- asumir que los nombres de columna pueden variar;
- interpretar un cambio de Trello como cambio del estado real del proyecto;
- forzar el mismo estado para Feature y User Stories;
- inferir el estado de la Feature a partir de una sola US;
- duplicar indiscriminadamente `tasks.md`;
- representar `CODE REVIEW` o `TESTING` como Tasks técnicas;
- ocultar desincronizaciones;
- fingir que una sincronización fallida tuvo éxito;
- mover una Feature a `DONE` antes del `CONVERGE` final exitoso;
- mover una Feature a DONE sin FEATURE COMPLETION AUTHORIZATION humana vigente y persistida;
- resetear indiscriminadamente todo el tablero ante un cambio upstream.

---

# 33. Comportamiento esperado del agente

Cuando esta skill esté activa, informar de forma concisa la trazabilidad relevante.

Ejemplo:

```text
Feature: 007-team-league-domain
SDD phase: ANALYZE

Trello:
- SPEC-007 → SPECIFYING
- US1 → SPECIFYING (NOT READY)
- US2 → SPECIFYING (NOT READY)
- US3 → SPECIFYING (NOT READY)

Trello sync: SYNCED
```

Ejemplo con gates:

```text
Feature: 007-team-league-domain
SDD phase: IMPLEMENT

Trello:
- SPEC-007 → IN PROGRESS
- US1 → DONE        [SDD GATE] TESTING APPROVED
- US2 → TESTING     [SDD GATE] CODE REVIEW APPROVED — Status: BLOCKED BY DEPENDENCY (US4)
- US3 → CODE REVIEW (awaiting human review)
- US4 → IN PROGRESS [SDD GATE] IMPLEMENT AUTHORIZATION APPROVED
- US5 → READY       [SDD GATE] IMPLEMENT AUTHORIZATION APPROVED

Trello sync: SYNCED
```

Ejemplo con inconsistencia:

```text
US2 figura DONE en Trello.
No existe evidencia de TESTING aprobada.

Acción: consultar el estado real con sdd-orchestration
        y reflejar la transición que indique.
No asumir DONE válido.
Trello sync: OUT OF SYNC
```

El objetivo es que Trello sea una representación confiable, liviana y trazable del estado real del desarrollo, sin convertirse en una segunda fuente de verdad paralela a Spec Kit.

SPEC-001–005 conservan DONE histórico sin gates retroactivos ni evidencia inventada. Reaperturas materiales siguen workflow vigente por impacto. Tests aplicables pendientes no permiten DONE nuevo; build/entrega no son TESTING. READY/autorización no inician IMPLEMENT.

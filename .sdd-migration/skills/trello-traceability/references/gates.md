# Evidencia — comentarios estructurados, invalidaciones y bloqueos

## Qué se persiste

Cuando `sdd-orchestration` decide que un gate se resolvió, delegar la persistencia como **comentario estructurado** en la tarjeta correspondiente:

- `IMPLEMENT AUTHORIZATION` aprobada o invalidada;
- `CODE REVIEW` aprobado o rechazado, e invalidado cuando corresponda;
- `TESTING` aprobado o `N/A`, e invalidado cuando corresponda;
- `FEATURE COMPLETION AUTHORIZATION` aprobada o invalidada;
- bloqueos y dependencias bloqueantes;
- cualquier otra validación humana que el orquestador delegue.

**No** decidir, derivar ni interpretar estos eventos: se persisten.

## Mecanismo

- Comentario estructurado, **no** Custom Fields obligatorios.
- **No** reescribir la descripción principal de la tarjeta para registrar aprobaciones.
- **No** copiar los reports completos: eso genera duplicación y desactualización.
- Los comentarios se usan porque conservan el historial de eventos y aprobaciones.

## Formato canónico

Prefijo `[SDD GATE]` y los mismos campos:

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

```text
[SDD GATE]
Gate: FEATURE COMPLETION AUTHORIZATION
Result: APPROVED
Validated by: HUMAN
Feature: SPEC-014
```

Reglas de campos:

- Los gates de US usan `User Story`; el gate final usa `Feature`.
- Referenciar el alcance o estado evaluado mediante commit, diff local o report disponible.
- **No inventar** hashes, resultados ni aprobaciones.
- `N/A` siempre incluye `Justification`.
- La evidencia debe ser suficiente para reconstruir qué gate se validó y cuál fue su resultado.

## Resultados admitidos

| Gate | `Result` válidos |
| --- | --- |
| `IMPLEMENT AUTHORIZATION` | `APPROVED`, `INVALIDATED` |
| `FEATURE COMPLETION AUTHORIZATION` | `APPROVED`, `INVALIDATED` |
| `CODE REVIEW` | `APPROVED`, `REJECTED` |
| `TESTING` | `APPROVED`, `N/A` |

`CODE REVIEW` y `TESTING` admiten además `INVALIDATED` con `Reason` **cuando el orquestador lo determine por impacto**.

## Invalidaciones

Una invalidación es un **evento nuevo**, nunca una aprobación humana ni una reescritura:

```text
[SDD GATE]
Gate: CODE REVIEW
Result: INVALIDATED
Reason: tests cambiados con impacto en el alcance aprobado
User Story: US2
```

- **No** incluir `Validated by: HUMAN` salvo que exista una validación humana real.
- **No** borrar el comentario anterior: el historial conserva la secuencia aprobación → invalidación → nueva aprobación.
- Registrar `INVALIDATED` **solo** cuando `sdd-orchestration` lo haya determinado. Esta skill no decide qué queda invalidado.
- Una US no afectada **conserva** su estado y sus aprobaciones. Aplicar las transiciones únicamente sobre las entidades afectadas; **no** resetear el tablero.

## Autorización que no inicia IMPLEMENT

```text
IMPLEMENT AUTHORIZATION APPROVED  ≠  IN PROGRESS
```

La US autorizada **permanece en `READY`** hasta que el orquestador indique que el IMPLEMENT realmente comenzó. No moverla solo por estar autorizada.

## Estado distribuido

La Feature y sus US pueden estar en estados distintos:

```text
Feature: IN PROGRESS

US1 → DONE
US2 → TESTING
US3 → CODE REVIEW
US4 → IN PROGRESS
US5 → READY
```

No forzar uniformidad ni inferir el estado de la Feature de una sola US.

## Bloqueos

Los bloqueos **no son gates**. Se registran **sin mover la tarjeta** fuera de la columna de su gate, con vocabulario fijo:

```text
Status: BLOCKED BY DEPENDENCY
Blocked by: <US / Feature / servicio externo>
```

o, para cualquier otra causa:

```text
Status: BLOCKED
Reason: <environment | configuration | external service | access | ...>
```

Nunca introducir estados nuevos por causa. Un bloqueo no implica aprobación ni invalidación de gate alguno.

## Trabajo nuevo durante la implementación

Si aparece trabajo necesario para satisfacer la Spec:

1. no crear automáticamente una US si el comportamiento ya pertenece a una existente;
2. crear o reabrir la tarea técnica en `tasks.md` cuando corresponda;
3. no modificar `spec.md` desde Trello;
4. reflejar el retroceso indicado por `sdd-orchestration`.

Las ideas fuera de alcance se registran en `BACKLOG` o como futura EPIC/SPEC según corresponda, para evitar scope creep.

## Conservación del historial

Al escribir en Trello: releer las tarjetas afectadas, verificar su identidad y **preservar** miembros, comentarios, adjuntos, checklists, etiquetas pertinentes e historial. Reutilizar tarjetas por clave o correspondencia confirmada.

No borrar, archivar ni cerrar hallazgos para ocultar discrepancias. No cambiar alcance o entrega sin acuerdo y antecedente trazable.
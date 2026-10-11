---
name: trello-traceability
description: Gestiona la trazabilidad operacional en Trello de Football Market: identifica y crea tarjetas, sincroniza estados entre las siete columnas, persiste autorizaciones y evidencias de gates, previene duplicados y conserva el historial. Usar cuando haya que crear, actualizar, mover o relacionar tarjetas, registrar evidencia o detectar desincronización, siguiendo las transiciones decididas por sdd-orchestration.
compatibility: opencode
metadata:
  project: football-market
  tracker: trello
  methodology: spec-driven-development
  role: operational
  receives-from: sdd-orchestration
---

# Trazabilidad Trello

## Propósito y límites

Esta skill ejecuta en Trello lo que `sdd-orchestration` decidió: tarjetas, relaciones, estados y evidencias.

**No decide.** No define requisitos funcionales, ni las condiciones de `READY`, ni la validez de un gate, ni cuándo una US o Feature alcanzan `DONE`, ni qué se invalida.

Si el tablero y la decisión del orquestador discrepan, prevalece la decisión del orquestador y se corrige la tarjeta. Si la discrepancia es material y no puede resolverse desde los artefactos, detenerse y reportar.

## Fuentes de autoridad

| Fuente | Qué gobierna |
| --- | --- |
| [Constitución §8](../../.specify/memory/constitution.md) | Columnas, claves de tarjeta, épicas, persistencia de evidencia e indexación |
| [`docs/development/sdd-workflow.md`](../../docs/development/sdd-workflow.md) | Sincronización, idempotencia, verificación e informe de fallos |
| `sdd-orchestration` | Fase, transiciones, gates, autorizaciones e invalidaciones |

La Constitución prevalece. Este archivo **no** redefine el lifecycle.

## Regla de alcance

**Trello es estado operacional y evidencia. Nunca es fuente de requisitos.** Una tarjeta puede resumir una necesidad o reflejar progreso, pero no reemplaza `spec.md`, criterios de aceptación, clarificaciones, `plan.md`, `tasks.md` ni documentación técnica. Si una tarjeta contradice un artefacto, prevalece el artefacto.

Si existen varias skills sobre el lifecycle, prevalece `sdd-orchestration`.

## Modelo de trazabilidad

```text
EPIC → SPEC → User Story → Task (principalmente tasks.md) → Código → Commit/PR
```

Este modelo, la precedencia de fuentes y la regla de no usar Trello como verdad se definen **una sola vez**, en `sdd-orchestration`. Aquí se aplican.

## Columnas

Las siete columnas canónicas, en orden:

```text
BACKLOG → SPECIFYING → READY → IN PROGRESS → CODE REVIEW → TESTING → DONE
```

| Columna | Feature | User Story |
| --- | --- | --- |
| `BACKLOG` | Intención todavía no preparada. | Historia todavía no preparada. |
| `SPECIFYING` | SPECIFY, CLARIFY, PLAN, CHECKLIST, TASKS o ANALYZE. | Preparación afectada pendiente. |
| `READY` | Preparación validada; espera inicio autorizado. | Preparada; autorizada o no. |
| `IN PROGRESS` | Al menos una US está en IMPLEMENT y la Feature aún no cumple condiciones para su TESTING final. | IMPLEMENT realmente iniciado. |
| `CODE REVIEW` | **No corresponde al flujo normal de Feature.** | Revisión humana del código completo, incluidos tests. |
| `TESTING` | CONVERGE final o autorización de cierre pendiente. | Revisión y ejecución humana de tests, o N/A validado. |
| `DONE` | CONVERGE y autorización final humana satisfechos. | Implementación, CODE REVIEW y TESTING satisfechos. |

Reglas:

- **No crear columnas nuevas.** No usar `CONVERGE`, `REVIEW` ni `SPECIFICATION`.
- Comprobar el tablero real; los cambios estructurales requieren autorización.
- Mantener la fase SDD detallada en el campo `SDD phase`, como proyección dentro de `SPECIFYING`.
- No forzar el mismo estado en Feature y US, ni inferir el estado de la Feature de una sola US.

## Operaciones

Esta skill **debe**:

- identificar y crear tarjetas con su clave canónica;
- relacionar EPIC, SPEC, US y tareas;
- mover tarjetas según la transición decidida;
- persistir evidencia de gates como comentarios estructurados;
- registrar invalidaciones y reaperturas sin borrar historia;
- prevenir duplicados;
- mantener el índice bidireccional;
- verificar las escrituras realizadas y reportar desincronización.

Esta skill **no debe**:

- decidir ni derivar un gate a partir de la evidencia registrada;
- interpretar un cambio de Trello como cambio del estado real del proyecto;
- mover una US a `IN PROGRESS` solo por estar autorizada;
- saltar `CODE REVIEW` o `TESTING`;
- marcar una US o Feature `DONE` por tareas completas, build, commit, PR o CONVERGE;
- persistir evidencia humana inventada;
- borrar evidencia histórica;
- introducir estados de bloqueo distintos de los canónicos;
- ocultar una desincronización o fingir una escritura exitosa;
- resetear el tablero ante un cambio upstream;
- mover tarjetas primero y asumir después que el proyecto está en ese estado.

## Referencias

| Referencia | Cuándo |
| --- | --- |
| [`references/cards.md`](references/cards.md) | Antes de crear, identificar o relacionar cualquier tarjeta. |
| [`references/gates.md`](references/gates.md) | Antes de persistir evidencia, invalidación o bloqueo. |
| [`references/sync.md`](references/sync.md) | En cada ciclo de sincronización. |
| [`references/consistency.md`](references/consistency.md) | Al detectar o corregir discrepancias entre tablero y decisión. |

## Salida

Comunicar de forma concisa:

```text
Feature: <id>-<scope>
SDD phase: <fase>

Trello:
- SPEC-<id> → <columna>
- US1 → <columna> (gates: …)

Trello sync: SYNCED | OUT OF SYNC
Pendientes registrados en el índice: <lista | ninguna>
```

Si una escritura obligatoria falla, informarlo con el código correspondiente (`TRELLO OUT OF SYNC` o `GATE EVIDENCE NOT PERSISTED`) y no declarar la transición completada. El cierre del gate corresponde a `sdd-orchestration`.

## Notas de vigencia

SPEC-001–005 conservan `DONE` histórico sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica el workflow vigente por impacto. Tests aplicables pendientes impiden un `DONE` nuevo.
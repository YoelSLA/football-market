# Contrato de delegación con `trello-traceability`

## Principio

```text
sdd-orchestration  →  decide el estado, la transición y la evidencia
trello-traceability →  la persiste en Trello
```

**Trello registra; no autoriza.** Un comentario de gate en Trello no crea, aprueba ni invalida ningún gate por sí mismo.

## Reparto

| Decisión o contenido | Dueño |
| --- | --- |
| Fase SDD, siguiente acción permitida | `sdd-orchestration` |
| Condiciones de READY y de cada gate | `sdd-orchestration` |
| Validez y vigencia de una autorización | `sdd-orchestration` |
| Invalidación por impacto y reaperturas | `sdd-orchestration` |
| Tipos de tarjeta, claves y contenido | `trello-traceability` |
| Nombres y orden de columnas, y transiciones de columna | `trello-traceability` |
| Formato `[SDD GATE]`, valores de `Result` y bloqueos | `trello-traceability` |
| Idempotencia, deduplicación, historial e índice | `trello-traceability` |
| Comandos de Spec Kit | Spec Kit |

Esta skill **no** inventa nombres de columna ni formatos de tarjeta. `trello-traceability` **no** redefine condiciones de gate.

## Qué delega esta skill

Antes de cada operación en Trello, decidir explícitamente:

1. **Entidad afectada**: Feature, US o ambas.
2. **Estado decidido**: fase, columna y gates vigentes.
3. **Evidencia a persistir**: gate, resultado, quién validó, entidad y referencia verificable del alcance evaluado.
4. **Si es invalidación**: incluir `Reason`; no atribuirla a una aprobación humana.
5. **Si es un bloqueo**: usar el vocabulario fijo, sin mover la tarjeta fuera de la columna del gate.

Verificar en `trello-traceability` el formato canónico antes de delegar. Delegar **una** operación por vez, sin agrupar escrituras de entidades distintas.

## Fallo de persistencia

| Situación | Respuesta |
| --- | --- |
| Operación mecánica fallida | Reportar `TRELLO OUT OF SYNC` con entidad, estado esperado, estado conocido y operación fallida. No bloquea el trabajo técnico; reintentar cuando sea seguro. |
| Evidencia obligatoria no persistida | Reportar `GATE EVIDENCE NOT PERSISTED` con tarjeta y gate. **No habilitar la transición dependiente** hasta resolverlo. |
| Trello no disponible o sin permisos | No afirmar sincronización. Registrar las operaciones pendientes en el índice, informar el bloqueo y pedir acceso o acuerdo sobre cómo completarlas. |

Nunca fingir que una sincronización fallida tuvo éxito. Nunca marcar un gate como cerrado por asumir que el comentario se persistió: **verificar con lectura posterior**.

## Uso de la evidencia registrada

- La evidencia persistida **no se reinterpreta**: no autoriza, no invalida, no crea gates.
- Sirve para **recuperar** una autorización vigente entre sesiones cuando la evidencia es verificable y no fue invalidada.
- Si Trello contradice el estado decidido, prevalece la decisión del orquestador y la tarjeta se corrige.
- Si la discrepancia es material y no puede resolverse desde artefactos, detener y consultar. Trello contradictorio **no redefine requisitos**.

## Secuencia de cierre de Feature

La secuencia y sus prerrequisitos están en [`gates.md`](gates.md). En términos de delegación:

1. Todas las US requeridas en `DONE`, cada una con su evidencia propia.
2. Decidir la transición de la Feature a su columna de CONVERGE y delegarla.
3. Ejecutar CONVERGE conforme al comando local.
4. Con resultado satisfactorio, solicitar la autorización final y delegar su persistencia.
5. Verificar la persistencia; solo entonces delegar la transición final y actualizar el estado de la spec.
# Sincronización — algoritmo, idempotencia y fallos

## Principio

```text
estado real cambia (decidido por sdd-orchestration)
        ↓
actualizar Trello
```

Nunca lo inverso: **no** mover la tarjeta primero y asumir después que el proyecto está en ese estado. Nunca interpretar un cambio de Trello como cambio del estado real.

## Algoritmo

1. Recibir del orquestador: entidad afectada, estado decidido, gates vigentes y evidencia a persistir.
2. Leer el tablero **una vez** y reconciliar en una sola pasada: tarjetas faltantes, sobrantes o desactualizadas.
3. Crear las tarjetas necesarias con su clave canónica, sin duplicar.
4. Mover las tarjetas según la transición decidida, únicamente sobre las entidades afectadas.
5. Corregir relaciones y referencias.
6. Persistir la evidencia operacional, si corresponde, como comentario `[SDD GATE]`.
7. **Verificar por lectura posterior** las entidades escritas: clave única, asociación correcta, transición aplicada y evidencia presente.
8. Actualizar el índice bidireccional con las URL y relaciones.
9. No modificar requisitos desde Trello.
10. Reportar qué cambió y el estado de sincronización, separando cambios del repositorio y del tablero.

## Idempotencia

- Reutilizar tarjetas por clave o correspondencia confirmada; nunca crear una segunda tarjeta para la misma entidad.
- Antes de escribir, releer la tarjeta afectada para confirmar que la operación no ocurrió ya.
- Actualizar `SDD phase` **solo cuando cambia la fase**, no en cada sincronización.
- Ante escritura parcial, releer antes de reintentar.
- Delegar una operación por entidad: no agrupar escrituras de tarjetas distintas en una sola operación sin verificación individual.

## Verificación posterior

Tras cada ciclo, comprobar sobre **las entidades escritas**:

- la clave es única y corresponde a una spec real;
- todas las US afectadas tienen tarjeta;
- la columna refleja la decisión;
- la evidencia obligatoria quedó persistida;
- el índice refleja los cambios.

No es necesario releer el tablero completo. Registrar el resultado en el índice cuando una operación quede pendiente.

## Desincronización

Distinguir dos casos.

### 1. Error mecánico

Si no se puede mover o actualizar una tarjeta:

```text
TRELLO OUT OF SYNC
Tarjeta: <id>
Estado esperado: <columna / contenido>
Estado conocido: <lo observado>
Operación fallida: <qué se intentó>
```

No fingir que la sincronización ocurrió. **No** bloquea necesariamente el trabajo técnico: se reintenta o se sincroniza cuando sea seguro.

### 2. Error persistiendo evidencia obligatoria

Si no se puede persistir evidencia de `IMPLEMENT AUTHORIZATION`, `CODE REVIEW`, `TESTING`, `FEATURE COMPLETION AUTHORIZATION` o sus invalidaciones:

```text
GATE EVIDENCE NOT PERSISTED
Tarjeta: <id>
Gate: <gate>
```

**No habilitar la transición dependiente** hasta resolverlo. No marcar el gate como cerrado. `sdd-orchestration` decidirá si puede considerarse completo.

### 3. Trello no disponible o sin permisos

No afirmar sincronización ni descartar cambios locales. Registrar en el índice las operaciones pendientes, informar el bloqueo y pedir al usuario que habilite el acceso o acuerde cómo completar la sincronización. La creación de la spec queda explícitamente pendiente de sincronización; al reanudar, comprobar lo ya aplicado antes de reintentar.

## Cambios upstream

Esta skill **no decide** qué queda invalidado: refleja lo que decida `sdd-orchestration`.

| Cambio | Efecto |
| --- | --- |
| `spec.md` cambia materialmente | Reflejar el retroceso indicado en la Feature y US afectadas; una US nueva definida por ese cambio recibe su tarjeta al volver a cerrar READY, no antes |
| `plan.md` o `tasks.md` cambian materialmente | Reflejar la re-evaluación de gates indicada |
| La Constitución cambia y afecta a la Feature | Reflejar la invalidación de `READY` indicada |
| Código o tests cambian con impacto en alcance aprobado | Reflejar la invalidación de CODE REVIEW y TESTING afectadas |

**Regla de alcance:** aplicar las transiciones solo sobre las entidades afectadas. Una US no afectada conserva su estado y sus aprobaciones. Cambios sin impacto sobre el alcance evaluado conservan gates vigentes: no invalidar por la mera existencia de cambios.

Nunca resetear indiscriminadamente todo el tablero.

## Persistencia de evidencia de fases

El checklist `SDD Lifecycle` de la tarjeta registra las operaciones de Specify, Clarify, Plan, Checklists, Tasks, Analyze, Implement y Converge con evidencia explícita. **No** sustituye los gates humanos.

Registrar en la tarjeta Feature, por fase: fecha, responsable, alcance, resultado (`OK` / `FAIL` / `PENDING` / `INVALIDATED`), entradas evaluadas, referencias, blockers y siguiente acción.

Marcar una fase solo con evidencia vigente. El Implement agregado se marca solo cuando **todo** su alcance requerido está terminado.

Nunca marcar fases por la mera existencia de artefactos, ni por tareas completas, merges o builds.

## Información de la tarjeta Feature

Actualizar, cuando corresponda:

- `SDD phase` al cambiar de fase;
- alcance, límites y dependencias;
- estado de sincronización y operaciones pendientes;
- evidencia de gates por referencia a sus comentarios, no copiando los reports.
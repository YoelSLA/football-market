# Tarjetas — tipos, claves y relaciones

## Claves canónicas

Las claves siguen la Constitución §8. Son **la única forma válida**; no usar variantes improvisadas.

| Tipo | Clave | Ejemplo |
| --- | --- | --- |
| Requisito sin SPEC | `[EPIC] REQ-<id> - <nombre>` | `[EPIC] REQ-S2-01 - Valuar jugadores con estrategias configurables` |
| Épica materializada | `[EPIC] SPEC-<spec-id> - <nombre>` | `[EPIC] SPEC-014 - Autenticación` |
| User Story | `SPEC-<spec-id> \| US<nn> - <título>` | `SPEC-014 \| US01 - Registro de usuario` |
| Tarea técnica de seguimiento | `SPEC-<spec-id> \| TSK<nn> - <título>` | `SPEC-014 \| TSK01 - Migración de tabla de usuarios` |

Reglas de identificadores:

- `<spec-id>` es el identificador de una spec existente.
- `US<nn>` usa el número local de la historia dentro de esa spec, con al menos dos dígitos. `SPEC-001 \| US01` y `SPEC-014 \| US01` son claves distintas.
- `TSK<nn>` tiene numeración local propia y **no** es equivalente a los identificadores `Tnnn` de `tasks.md`; cuando corresponda a una tarea de ese archivo, referenciarla explícitamente.
- No renumerar historias existentes por cambiar su orden o prioridad.
- Dividir, fusionar o retirar historias conserva la trazabilidad de identificadores y tarjetas anteriores.
- No inventar US ni Tasks, ni usar `REQ-* | USxx` o `REQ-* | TSKxx` como convención previa a la materialización de una SPEC.
- Las US formales derivan **únicamente** de `spec.md` de una SPEC existente.
- No predecir ni reservar números de SPEC.

## EPIC

Representa un objetivo amplio de producto, dominio o capacidad. Puede contener varias Features.

- No convertir automáticamente una EPIC en Spec ni dividirla sin aprobación humana.
- Puede permanecer en `BACKLOG` mientras sus Specs no se materialicen.
- No marcarla `DONE` por completar una sola Feature: requiere que todas las Features de su alcance final estén `DONE`.
- Si su alcance cambia, actualizar explícitamente su composición.
- No copiar en la EPIC los requisitos detallados de cada Spec.

## Feature / SPEC

Representa una Feature gestionada por Spec Kit. Debe corresponder a una carpeta real, por ejemplo `specs/014-autenticacion/`.

Contenido mínimo:

```text
Tipo: SPEC
EPIC: <épica relacionada>
Ruta: specs/014-autenticacion/
SDD phase: <fase actual>
```

Relaciones obligatorias: su EPIC padre si existe, su directorio o identificador de Spec Kit, y sus US.

Crear la tarjeta cuando la Feature tenga identidad propia en el flujo de Spec Kit. **No** crear una Spec ejecutable por una idea vaga en backlog.

## User Story

Representa una unidad de comportamiento definida en `spec.md`. Debe derivar de una Spec existente.

Contenido mínimo:

```text
Tipo: USER STORY
Spec: 014-autenticacion
User Story: US1
SDD phase: <fase actual>
```

Opcional: resumen breve de comportamiento, rama, commit, PR, bloqueos.

Reglas:

- Se crean **durante `SPECIFYING`**, tan pronto como estén suficientemente identificadas.
- Si en ese momento una US no es identificable con certeza, esperar a CLARIFY y crearla entonces.
- **Nunca** crear una US operativa sin equivalente en la Spec.
- En `SPECIFYING` pueden existir para dar visibilidad, pero no son ejecutables hasta que la Feature alcance `READY`.
- Los criterios de aceptación detallados continúan viviendo en `spec.md`.

## Tareas

Las tareas técnicas viven **principalmente** en `tasks.md`. Por defecto **no** crear una tarjeta por tarea: la granularidad puede exceder lo útil en el tablero.

Crear una tarjeta `TSK<nn>` solo con necesidad operativa real: coordinación entre personas, dependencia externa, trabajo bloqueado, seguimiento independiente, tarea larga o actividad no representada por ninguna US.

Trabajo compartido o fundacional **MAY** no tener etiqueta US, pero **MUST** indicar a quién habilita y estar autorizado expresamente.

## Lo que Trello nunca representa

- Los **gates humanos** del workflow: `CODE REVIEW`, `TESTING` y autorizaciones no son Tasks técnicas.
- El detalle de `spec.md`, `plan.md`, `tasks.md`, checklists, hallazgos de análisis, commits completos, `Code Review Report` ni `Testing Report`.
- Cartas ficticias para anticipar una SPEC inexistente.

## Relaciones obligatorias

```text
SPEC → su EPIC
US   → su SPEC
código → su US, mediante rama, commit o PR cuando aplique
```

No es obligatorio copiar cada commit si la rama o el PR ya ofrecen trazabilidad suficiente. Cada tarjeta identifica su ruta y, cuando corresponde, la US o tarea; una épica `REQ-` identifica el requisito futuro **sin inventar una ruta de SPEC**.

## Índice bidireccional

El índice se mantiene en [`specs/trello.md`](../../../../specs/trello.md).

- Registra las URL de las tarjetas y las relaciones provisionales.
- Registra las operaciones pendientes cuando Trello no está disponible o faltan permisos.
- Conserva las claves heredadas como evidencia hasta una migración operativa autorizada; no acreditan US formales ni elegibilidad.

## Fuentes y dependencias en la tarjeta

Cada tarjeta asociada a una SPEC indica las fuentes aplicables de `tp/` y la entrega acordada, y sus dependencias mediante claves o enlaces reales. Los enlaces a GitHub no acreditan que cambios locales ya estén publicados.
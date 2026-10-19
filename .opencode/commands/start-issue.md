---
description: "Iniciar el trabajo de un issue de GitHub: verificar dependencias, materializar la SPEC con el identificador provisto, crear y publicar la rama desde origin/dev, asociarla al issue, actualizar el seguimiento y preparar el flujo SDD."
handoffs:
  - label: Especificar la SPEC
    agent: speckit.specify
    prompt: Redactar la especificación de la feature a partir del issue de origen y de la carpeta de SPEC ya materializada.
    send: false
---

## User Input

$ARGUMENTS

## Reglas locales obligatorias

MUST leer `AGENTS.md`, `.specify/memory/constitution.md`, `docs/development/sdd-workflow.md` y
`docs/development/git-workflow.md` antes de cualquier escritura.

MUST NOT generar, inferir ni asignar el identificador de la SPEC: lo provee la persona que invoca
el comando. Si falta, el comando se detiene y lo pide; no se elige el siguiente número libre ni se
propone un sustituto por cuenta propia.

MUST NOT reutilizar un identificador ya asignado a otra funcionalidad, ni aceptar que el script de
creación lo ajuste. Si el script devuelve un número distinto del pedido, el comando se detiene y
reporta.

MUST NOT modificar código funcional, tests, SPEC existentes, requerimientos, `tp/` ni `docs/`. Este
comando prepara contexto operativo; no implementa.

MUST preservar íntegramente el body del issue —incluidos RF, CA, dependencias y referencias—, los
labels, el milestone, las asignaciones y las relaciones con otros issues.

MUST NOT hacer `commit`. La publicación de la rama es un `push` de la referencia ya creada, sin
commits nuevos.

MUST reportar el resultado real de cada paso leyendo la salida efectiva, sin asumir éxito por la
ausencia de error.

## Fase 0 — Preflight (solo lectura, sin escrituras)

**0.1 — Argumentos.** `$1` = número de issue. `$2` = identificador SPEC. `$3` = scope opcional en
`kebab-case`.
- `$1` MUST ser un entero positivo. Si no lo es, detener y pedir un número válido.
- `$2` MUST cumplir `^SPEC-[0-9]{2,3}$` para issues `REQ-` o `SPEC-`. Para issues `TSK-` es opcional
  y se ignora.
- `$3`, si viene, se usa verbatim como scope tras validarlo. Si no viene, el scope se deriva según
  0.7.

**0.2 — Leer el issue.**

```bash
gh issue view <n> --json number,title,body,state,labels,milestone,assignees,url,projectItems
```

- Si `state` no es `OPEN`, detener y reportar.
- Clasificar por el prefijo del título: `TSK-` = tarea; `REQ-` = requerimiento; `SPEC-` = registro
  ya materializado.
- Guardar el estado previo de `body`, `labels`, `milestone` y `assignees` para la verificación de la
  Fase 3.

**0.3 — Idempotencia.** Si el título ya empieza con `SPEC-<nnn> | `:
- Si `$2` difiere del identificador ya presente, detener: es un conflicto de identidad.
- Si coincide, continuar solo con los pasos faltantes, verificados uno a uno, sin reasignar ni duplicar.

**0.4 — Verificar dependencias** (issues `REQ-` y `SPEC-`).
- Extraer del body los enlaces `(#NN)` de las secciones de dependencias.
- Clasificar cada dependencia por su encabezado: "Requisito previo", "Requisitos previos" y
  equivalentes = **bloqueante**; "Relacionada", "Consumidora", "Funcionalidad relacionada" =
  **no bloqueante**.
- Consultar cada una: `gh issue view <dep> --json number,title,state,closedAt`.
- Una dependencia bloqueante abierta **no detiene** el comando: la doctrina del workflow es
  `Dependency ≠ Blocker`. Se informa de forma destacada en el reporte final y se deja constancia en
  la salida.

**0.5 — Verificar que el identificador SPEC no esté usado.** Obligatorio antes de cualquier escritura.
Cruzar cuatro fuentes y registrar el resultado de cada una:
- Specs locales: `ls specs/ | grep -E '^<num>-'` → cualquier coincidencia es conflicto.
- Ramas remotas: `git ls-remote --heads origin | grep -E '/<num>-'` → conflicto.
- Issues de GitHub: `gh issue list --state all --search "SPEC-<num> in:title"` → cualquier issue
  distinto del actual es conflicto.
- Items del proyecto: cualquier título que empiece con `SPEC-<num> |` es conflicto.

Si hay cualquier conflicto: **detener**, reportar la fuente y el conflicting, y no escribir nada. No
se reasigna ni se corrige el número.

**0.6 — Estado de Git.**
```bash
git fetch origin
git status --short
```
- Si hay cambios rastreados pendientes, **detener**: no se descarta, no se hace `stash`, el árbol
  queda intacto.
- `git checkout dev` y `git merge --ff-only origin/dev` para dejar `HEAD` en `origin/dev`. Si el
  `ff-only` falla, detener y reportar.
- Guardar `ORIGIN_DEV_SHA=$(git rev-parse origin/dev)`.

**0.7 — Scope.** El scope forma parte de la identidad de la SPEC (`<num>-<scope>`) y de la rama
(`<type>/<num>-<scope>`), por lo que su resolución es siempre explícita y auditable.
- Si `$3` viene: se usa verbatim como scope, tras validar `^[a-z0-9]+(-[a-z0-9]+)*$`.
- Si `$3` no viene: derivarlo del título del issue. Quitar el código de prefijo (`REQ-...`, `TSK-...`,
  `SPEC-...`) y el separador `|`, conservar el resto literal, pasar a minúsculas, convertir toda
  secuencia no alfanumérica en `-`, colapsar guiones repetidos y quitar guiones de los extremos.
  Validar el resultado con la misma expresión.
- **Reportar siempre el scope resultante**, indicate si fue provisto o derivado. No se descarta un
  scope inválido sin avisar: se detiene y se pide uno válido.

**0.8 — Tipo de rama.** `feat` por defecto. El conjunto cerrado es `feat fix refactor test docs chore
perf ci` de `docs/development/git-workflow.md`. Si el trabajo no es una funcionalidad nueva, elegir el
tipo que corresponda y reportarlo.

## Fase 1 — Materializar la SPEC (solo issues `REQ-` y `SPEC-`)

**1.1 — Mecanismo sancionado.** Ejecutar desde la raíz, una sola vez:

```bash
.specify/scripts/bash/create-new-feature.sh --json --number <num> --short-name '<scope>' --type <type> '<título del issue>'
```

**1.2 — Verificar la salida.** Leer `SPEC_NAME`, `SPEC_FILE`, `FEATURE_NUM`, `BRANCH_NAME`,
`SPEC_ACTION`, `BRANCH_STATUS`.
- `FEATURE_NUM` distinto del número pedido → **detener y reportar**; el script ajustó el número y ese
  ajuste no se acepta.
- `SPEC_ACTION` distinto de `created` → reportar; no continuar como si fuera una SPEC nueva.
- `BRANCH_STATUS` que no acredite rama disponible → reportar el estado real, no asumir.

**1.3 — Verificar el materializado.** Que exista `specs/<SPEC_NAME>/` con `spec.md` de plantilla. **No
escribir el contenido de la especificación**: corresponde a SPECIFY.

**1.4 — Issues `TSK-`.** No hay SPEC. La rama se crea directamente como `<type>/<scope>` (sin
identificador de SPEC), según `docs/development/git-workflow.md` para cambios que no justifican spec
propia. Materializar la carpeta `specs/<scope>/` queda fuera del alcance de este comando.

## Fase 2 — Publicar la rama y asociarla al issue

**2.1 — Publicar.**
```bash
git push -u origin <BRANCH_NAME>
git ls-remote --heads origin <BRANCH_NAME>
```
Verificar que la referencia remota existe y apunta al commit esperado.

**2.2 — Asociar al issue.** Usar la mutación GraphQL `createLinkedBranch`, que crea la referencia
remota en el commit dado y la vincula al issue en una sola operación:
- `issueId`: `gh api repos/YoelSLA/football-market/issues/<n> --jq .node_id`
- `oid`: `<ORIGIN_DEV_SHA>`
- `name`: `<BRANCH_NAME>`

Si la rama ya está vinculada, verificar con `gh issue develop --list <n>` y no duplicar la vinculación.
Si la mutación falla, reportar el error literal; no simular la asociación ni dejar el issue
supuestamente vinculado.

## Fase 3 — Issue y seguimiento operativo

**3.1 — Título.** Solo para issues `REQ-` y `SPEC-`: `SPEC-<num> | <título original sin el prefijo de
código>`, conservando literal el resto del título. Para issues `TSK-`, **no cambiar el título**. Usar
`gh issue edit <n> --title "..."` únicamente, sin `--body`, `--label`, `--milestone` ni `--assignee`.

**3.2 — Proyecto.** Resolver el proyecto por título (`Football Market — Development`) y, dentro de
él, el campo `Status` y la opción `🔨 IN PROGRESS`, obteniendo sus identificadores en el momento:
```bash
gh project list --owner @me --format json
gh project field-list <project-number> --owner @me --format json
```
- Si el issue no está en el proyecto, agregarlo con `gh project item-add`.
- Fijar el estado con `gh project item-edit --id <item-id> --project-id <id> --field-id <status-field-id>
  --single-select-option-id <option-id>`.

**3.3 — Verificar la conservación.** Releer el issue y comparar `body`, `labels`, `milestone` y
`assignees` contra el estado guardado en 0.2. Cualquier diferencia se reporta como fallo del comando;
no se corrige silenciosamente.

## Fase 4 — Cierre y reporte

**4.1 — Reportar**, con valores leídos de las salidas efectivas:
- Issue y URL, título anterior y título nuevo.
- SPEC: identificador, directorio, archivo, `SPEC_ACTION`, `FEATURE_NUM`, `BRANCH_STATUS`.
- Rama: nombre, base de creación, commit, estado local y remoto, vinculación al issue.
- Seguimiento: proyecto, item y estado final.
- Dependencias verificadas: cuáles cerradas, cuáles abiertas y su carácter bloqueante o no.
- Fuentes de colisión consultadas y resultado de cada una.
- Estado de Git antes y después.

**4.2 — Siguiente paso del flujo SDD.** El comando deja el contexto preparado. La continuación es
`/speckit.specify` sobre la SPEC materializada. **Este comando no ejecuta SPECIFY, PLAN, TASKS,
ANALYZE ni IMPLEMENT**, no redacta la especificación y no autoriza implementación.

**4.3 — Registro de asignación.** El número de SPEC fue asignado de forma explícita por la persona, no
por numeración automática. Registrar el identificador, la funcionalidad, el responsable, el issue de
origen, el requisito `REQ-` rastreado, la rama y la fecha en `specs/assignments.md`, archivo append-only
que el comando crea si no existe, conservando su encabezado y su tabla. Este registro satisface la
trazabilidad de la asignación explícita exigida por la Constitución §8; no sustituye los artefactos de
la SPEC ni los gates SDD.

## Notas de divergencia a reportar

- `docs/development/sdd-workflow.md` §5 describe el título del registro de seguimiento como
  `[SPEC-0XX] <nombre>`, con corchetes. La práctica vigente del repositorio y los registros existentes
  en GitHub Projects usan `SPEC-0XX | <nombre>`, con barra vertical. Este comando sigue la práctica
  vigente y deja constancia de la divergencia documental para su corrección en el origen.
- `docs/development/sdd-workflow.md` §4 fija el paso a `In Progress` cuando el trabajo comienza. Este
  comando lo hace al crear y publicar la rama, que es el inicio del trabajo sobre el issue, y no en el
  momento de una autorización.

# Guía de uso de OpenCode en Football Market

OpenCode es el agente que acompaña el desarrollo de este repositorio. Lee las normas del
proyecto por su cuenta —`AGENTS.md`, la Constitución, el workflow SDD— y ejecuta los comandos
que están definidos en `.opencode/commands/`. Esta guía explica, para alguien que se acaba de
incorporar al equipo, qué comandos hay, cómo se invocan y en qué orden se usan.

Todo lo que sigue está tomado de los archivos reales del repositorio. Si un comando hace algo,
lo hace porque su definición en `.opencode/commands/` lo dice; no hay comandos ocultos ni
parámetros inventados.

## Índice

1. [Requisitos previos](#1-requisitos-previos)
2. [Cómo iniciar OpenCode](#2-cómo-iniciar-opencode)
3. [Comandos disponibles](#3-comandos-disponibles)
4. [Sintaxis y parámetros](#4-sintaxis-y-parámetros)
5. [Ejemplos de uso](#5-ejemplos-de-uso)
6. [Flujo recomendado: del issue al Pull Request](#6-flujo-recomendado-del-issue-al-pull-request)
7. [Trabajar con un REQ, una SPEC o una TSK](#7-trabajar-con-un-req-una-spec-o-una-tsk)
8. [Cuando un comando falla](#8-cuando-un-comando-falla)
9. [Buenas prácticas y errores frecuentes](#9-buenas-prácticas-y-errores-frecuentes)
10. [Dónde está cada cosa](#10-dónde-está-cada-cosa)

---

## 1. Requisitos previos

### Imprescindible

| Herramienta | Para qué | Cómo verificar |
| --- | --- | --- |
| **OpenCode CLI** | Ejecutar el agente. Sin él no hay comandos. | `opencode --version` |
| **Git** | Ramas, commits, PRs. El repositorio exige las convenciones de `docs/development/git-workflow.md`. | `git --version` |
| **GitHub CLI (`gh`)** | Lo usa `/start-issue` para publicar la rama, vincularla al issue y actualizar GitHub Projects. | `gh auth status` |

El token de `gh` necesita permiso sobre el repositorio y sobre el proyecto. En una instalación
correcta `gh auth status` muestra los alcances `repo` (ramas e issues) y `project` (GitHub
Projects). Sin ellos, `/start-issue` se detiene en la fase de publicación.

### Para compilar y probar (los ejecuta la persona, no el agente)

| Herramienta | Versión | Dónde se define |
| --- | --- | --- |
| **JDK** | 21 | `backend/build.gradle.kts` y los workflows de CI |
| **Node.js + npm** | 24 o superior (`.nvmrc` fija `24.19.0`) | `.nvmrc`, `frontend/package.json` |
| **PostgreSQL** | en `localhost:5432` | `backend/src/main/resources/application-dev.yml` |

El agente **no ejecuta tests**, ni de forma directa ni a través de hooks. Escribir los tests es
parte del trabajo; ejecutarlos y confirmar el resultado es tarea de la persona.

### Configuración del proyecto

- `opencode.json` en la raíz fija el modelo por defecto que usa el agente.
- Los comandos viven en `.opencode/commands/` y las skills en `.opencode/skills/`. Son de alcance
  de proyecto: se aplican a este repositorio y solo a este.
- `AGENTS.md` en la raíz es el manual operativo del agente. Se carga automáticamente, pero
  conviene leerlo: es la fuente de las reglas que el agente aplica.

---

## 2. Cómo iniciar OpenCode

Desde la raíz del repositorio:

```bash
cd football-market
opencode
```

Eso abre la interfaz interactiva. Escribí `/` seguido del nombre del comando para ejecutarlo, por
ejemplo `/start-issue 42 SPEC-013`; escribiéndolo a secas se listan los comandos disponibles.

Otras formas de invocarlo:

```bash
opencode                       # interfaz interactiva (TUI)
opencode run "revisá el diff"  # una sola ejecución, sin interfaz
opencode --continue            # retoma la última sesión
opencode --session <id>        # retoma o crea una sesión concreta
opencode /otro/directorio      # arranca en otro directorio
opencode service status        # estado del servicio en segundo plano
```

Dos comportamientos útiles que conviene conocer:

- **Los comandos se recargan solos.** Si alguien edita o agrega un archivo en
  `.opencode/commands/`, el comando nuevo queda disponible sin reiniciar OpenCode.
- **Los argumentos se expanden al invocarlos.** Si escribís `/start-issue 43 SPEC-014`, el
  agente recibe ese texto ya, con los valores puestos en los lugares correspondientes de la
  plantilla del comando.

---

## 3. Comandos disponibles

Hay **diez comandos** en `.opencode/commands/` y **una skill** de orquestación.

### Los comandos del ciclo SDD

| Comando | Qué hace, en una frase | Cuándo se usa |
| --- | --- | --- |
| `/speckit.constitution` | Crea o actualiza la Constitución del proyecto, que fija los principios que ninguna funcionalidad puede contradecir. | Solo cuando cambian las reglas de gobernanza del proyecto. |
| `/speckit.specify` | Redacta `spec.md`: historia, requisitos funcionales, criterios de aceptación y límites de la funcionalidad. | Es el primer paso de una funcionalidad nueva. |
| `/speckit.clarify` | Hace hasta 5 preguntas concretas sobre lo que quedó ambiguo en la spec y graba las respuestas dentro de `spec.md`. | Siempre después de `/speckit.specify`, antes de planificar. |
| `/speckit.plan` | Genera `plan.md` y los artefactos de diseño: estructura, contratos, modelo de datos, estrategia de verificación. | Con la spec ya clara. |
| `/speckit.checklist` | Genera checklists de calidad de requisitos, uno por dominio: `ux.md`, `api.md`, `performance.md`, `security.md`. | Obligatorio, después de `/speckit.plan`. |
| `/speckit.tasks` | Genera `tasks.md` con las tareas técnicas ordenadas por dependencia. | Con plan y checklists ya revisados. |
| `/speckit.analyze` | Analiza, sin escribir nada, si `spec.md`, `plan.md` y `tasks.md` son coherentes entre sí. | Después de `/speckit.tasks`. |
| `/speckit.implement` | Ejecuta el alcance autorizado, trabajando desde `tasks.md`. | Solo con IMPLEMENT AUTHORIZATION humana vigente. |
| `/speckit.converge` | Compara el código con la spec, el plan y las tareas, y anexa a `tasks.md` el trabajo que quedó sin construir. | Al final, con la implementación completa. |

### El comando de arranque operativo

| Comando | Qué hace, en una frase | Cuándo se usa |
| --- | --- | --- |
| `/start-issue` | Prepara el arranque de un issue de GitHub: verifica dependencias, materializa la SPEC, crea y publica la rama desde `origin/dev`, la vincula al issue y actualiza el estado en GitHub Projects. | Al recibir la asignación de un issue. |

### La skill de orquestación

No es un comando: es una capacidad que el agente ofrece bajo demanda.

| Skill | Qué hace, en una frase |
| --- | --- |
| `sdd-orchestration` | Determina en qué fase está una funcionalidad, qué operación corresponde, qué autorizaciones existen y cuál es el siguiente paso permitido. |

Se activa con una intención, no con una frase exacta. La propia skill define el mapeo:

| Intención | Ejemplo | Qué hace |
| --- | --- | --- |
| Iniciar | «Orquestá esta Feature» | Identifica la funcionalidad y prepara el SDD hasta el próximo bloqueo o autorización. |
| Continuar | «Continuá la 007» | Reconstruye el estado y ejecuta el siguiente paso permitido. |
| Consultar | «¿En qué estado quedó?» | Solo lectura: no avanza fases ni modifica código. |
| Diagnosticar | «Mostrame los problemas de CONVERGE» | Inspecciona e informa; no corrige sin autorización. |
| Autorizar | «Autorizo IMPLEMENT de la 007» | Valida la autorización contextual y previa antes de actuar. |

---

## 4. Sintaxis y parámetros

### Comandos `speckit.*`

```text
/speckit.<fase> [texto libre opcional]
```

- **Parámetro obligatorio:** ninguno.
- **Parámetro opcional:** texto libre que se pasa al agente como `$ARGUMENTS`. Sirve para
  orientar el comando, no para que el agente adivine: «análisis enfocado en el contrato
  HTTP» o «checklist de seguridad para la consulta de portfolio».
- El contexto de la funcionalidad no se pasa por parámetro: el agente lo resuelve desde
  `.specify/feature.json` y desde la carpeta `specs/<id>-<scope>/` activa.

Ejemplos de la forma real:

```text
/speckit.specify
/speckit.specify portfolio personal con posición, precio promedio y ganancia por jugador
/speckit.clarify
/speckit.checklist security
```

### `/start-issue`

```text
/start-issue <issue_number> <spec_id> [scope]
```

| Posición | Obligatorio | Qué es | Validación |
| --- | --- | --- | --- |
| `$1` | **Sí** | Número del issue de GitHub. | Entero positivo. |
| `$2` | **Sí** para issues `REQ-` y `SPEC-`; **no** para `TSK-` | Identificador de la SPEC, con formato `SPEC-0NN`. | `^SPEC-[0-9]{2,3}$` |
| `$3` | No | *Scope* de la SPEC, en `kebab-case`. | `^[a-z0-9]+(-[a-z0-9]+)*$` |

Dos reglas que conviene interiorizar:

- **El identificador de la SPEC lo provee siempre la persona.** El comando no genera números, no
  infiere el siguiente libre y no propone sustitutos. Si falta, se detiene y lo pide.
- **Si no pasás `$3`, el scope se deriva del título del issue** y el comando lo reporta
  explícitamente. El scope forma parte del nombre de la carpeta de la spec y de la rama, así que
  conviene elegirlo a mano cuando importe.

Los tres ejemplos reales:

```text
/start-issue 43 SPEC-014           # issue REQ, spec nueva
/start-issue 43 SPEC-014 buy-tokens  # mismo caso, scope en inglés explícito
/start-issue 61                   # issue TSK: sin spec, sin cambio de título
```

### Handoffs entre comandos

Los comandos `speckit.*` declaran en su frontmatter un bloque `handoffs` con el siguiente paso de
la fase. Cuando el comando termina, ese paso queda propuesto. En `speckit.specify`,
`speckit.tasks` y `start-issue` el handoff lleva `send: false`: la transición **no** se dispara
sola y hay que invocarla a mano.

Los handoffs **no autorizan** avanzar. La fase siguiente requiere su propia autorización cuando
el workflow la exige.

---

## 5. Ejemplos de uso

### Arrancar con un issue asignado

```text
/start-issue 42 SPEC-013
```

El comando verifica el issue y sus dependencias, comprueba que `SPEC-013` no esté en uso, crea la
carpeta `specs/013-<scope>/`, crea la rama `feat/013-<scope>` desde `origin/dev`, la publica, la
vincula al issue, renombra el issue a `SPEC-013 | …` y lo mueve a `🔨 IN PROGRESS` en el proyecto
`Football Market — Development`. El body, los labels, el milestone y las asignaciones quedan
intactos.

### Reanudar una SPEC que ya existe

```text
/speckit.specify
```

Cuando la funcionalidad ya está materializada, `/speckit.specify` la **reutiliza** en lugar de
crear una nueva. Nunca se genera un identificador nuevo para continuar trabajo de una SPEC
existente: eso es una regla del workflow, no una recomendación.

### Aclarar una spec ambigua

```text
/speckit.clarify
```

El agente escanea `spec.md`, identifica las zonas no especificadas y hace hasta 5 preguntas
concretas. Las respuestas se graban en la spec, no quedan en el chat.

### Planificar y generar tareas

```text
/speckit.plan
/speckit.checklist
/speckit.tasks
```

En ese orden. El checklist lo revisa la persona: el agente genera los ítems, pero la evaluación
de que un criterio está satisfecho es del revisor y se marca en el archivo de checklist.

### Analizar la coherencia antes de implementar

```text
/speckit.analyze
```

Es estrictamente de solo lectura. Contrasta Constitución, spec, clarificaciones, diseño, tareas y
checklists. Si encuentra un hallazgo `CRITICAL` o `HIGH`, la implementación queda bloqueada.

### Implementar con autorización

```text
/speckit.implement
```

Solo arranca si hay IMPLEMENT AUTHORIZATION humana explícita, vigente y persistida. Si algún
checklist tiene ítems sin marcar, el comando se detiene y los reporta.

### Pedir el estado de una funcionalidad

```text
Orquestá la SPEC 012 y decime en qué estado quedó
```

La skill de orquestación reconstruye el estado, dice en qué fase está y cuál es el siguiente paso
permitido. No escribe nada.

### Un TSK: trabajo que no necesita una SPEC

```text
/start-issue 61
```

No materializa SPEC, no cambia el título y crea la rama como `feat/<scope>`. Los criterios viven
en el body del issue, y la implementación se hace contra esos criterios.

---

## 6. Flujo recomendado: del issue al Pull Request

### Los cuatro gates humanos

Ningún gate se salta, y ninguno se aprueba con un «dale». Están definidos en
`docs/development/sdd-workflow.md` §3.

| Autorización | Habilita | No habilita |
| --- | --- | --- |
| IMPLEMENT AUTHORIZATION | Escribir código del alcance autorizado | El inicio por sí solo, CODE REVIEW, TESTING ni el cierre |
| CODE REVIEW | Pasar a la revisión de tests | TESTING ni el cierre |
| TESTING | Cerrar el ciclo de verificación | El cierre de la funcionalidad |
| FEATURE COMPLETION AUTHORIZATION | El cierre de la funcionalidad | Nada posterior |

### Los estados del seguimiento

`Backlog` → `In Progress` → `In Review` → `Done`. En GitHub Projects se ven como `📋 BACKLOG`,
`🔨 IN PROGRESS`, `🔍 IN REVIEW` y `✅ DONE`. El registro pasa a `In Progress` **cuando el trabajo
empieza**, no cuando se autoriza.

### El recorrido completo

1. **Asignación.** Te llega un issue. Verificá que el repositorio esté sincronizado:
   `git fetch origin` y comprobá `git status --short`.
2. **Arranque operativo.** `/start-issue <n> <spec_id> [scope]`. Deja la rama creada, publicada y
   vinculada, y el seguimiento en `In Progress`.
3. **SPECIFY** (solo `REQ-` y `SPEC-`): `/speckit.specify` con la descripción de la
   funcionalidad.
4. **CLARIFY**: `/speckit.clarify`. Siempre obligatorio después de SPECIFY. Cero preguntas es un
   resultado válido si el escaneo acredita claridad; omitir la fase no lo es. Al concluir, se
   confirman y publican los cambios de la especificación y se crea siempre el Pull Request hacia
   `dev` vinculado al issue si no existía.
5. **PLAN**: `/speckit.plan`.
6. **CHECKLIST**: `/speckit.checklist`. Obligatorio. El agente genera; la persona revisa y marca.
7. **TASKS**: `/speckit.tasks`.
8. **ANALYZE**: `/speckit.analyze`. Solo lectura. Sin hallazgos bloqueantes, la preparación queda
   completa; no existe un gate READY adicional.
9. **IMPLEMENT AUTHORIZATION**: la persona presenta el alcance y lo autoriza de forma explícita.
   Solo entonces `/speckit.implement`.
10. **CODE REVIEW** y **TESTING**: dos autorizaciones separadas. La persona revisa el reporte
    completo y ejecuta los tests; el agente no los ejecuta.
11. **CONVERGE**: `/speckit.converge`, con el trabajo técnico completo. Es diagnóstico y
    append-only: anexa tareas faltantes, nunca escribe código.
12. **FEATURE COMPLETION AUTHORIZATION**: la persona autoriza el cierre sobre el resultado
    presentado. Un CONVERGE exitoso prepara el cierre, no lo autoriza.
13. **Commit y Pull Request.** Según `docs/development/git-workflow.md`: rama
    `<type>/<spec-id>-<scope>`, mensajes `<type>(<scope>): <descripción>` en inglés, y el PR
    dirigido a `dev` referenciando la spec, la rama, la evidencia y los pendientes.

### Ciclo de corrección

Si CODE REVIEW o TESTING detectan algo que exige código o tests, se vuelve a implementación y se
repite CODE REVIEW y TESTING. Un bloqueo externo que no exige cambios deja la funcionalidad en
revisión, con la causa registrada. Una revisión o un test que afecta el alcance aprobado
invalida la autorización correspondiente.

---

## 7. Trabajar con un REQ, una SPEC o una TSK

Las tres cosas se distinguen por el **prefijo del título del issue** y determinan qué hace
`/start-issue`.

| | **REQ** | **SPEC** | **TSK** |
| --- | --- | --- | --- |
| Ejemplo de título | `REQ-S2-14-A \| Comprar tokens desde el frontend` | `SPEC-013 \| …` | `TSK-001 \| Rediseñar Navbar…` |
| Qué es | Un requerimiento de `tp/` que todavía no tiene especificación. | Una funcionalidad ya materializada, con su carpeta y sus artefactos. | Una tarea acotada que no justifica una spec propia. |
| ¿Materializa SPEC? | Sí | No: reutiliza la existente | No |
| ¿Cambia el título? | Sí, a `SPEC-0NN \| <nombre de la funcionalidad>` | No: ya está en ese formato | **No** |
| Identificador en `/start-issue` | **Obligatorio** | **Obligatorio** | No hace falta |
| Rama | `<type>/<spec-id>-<scope>` | `<type>/<spec-id>-<scope>` | `<type>/<scope>` |
| Dónde viven los requisitos | En la spec que se va a redactar | En `specs/<id>-<scope>/` | En el body del issue |
| Siguiente paso | `/speckit.specify` | Continuar la fase en la que esté | Implementar contra los criterios del issue |

### El detalle que más confunde

- **`REQ-` y `SPEC-` comparten camino.** En ambos hace falta el identificador. La diferencia es
  que el `REQ-` todavía no tiene carpeta: el comando la crea y renombra el issue. El `SPEC-` ya
  la tiene: el comando verifica que el identificador coincida y no vuelve a crear nada.
- **Un `TSK-` no genera SPEC.** Los criterios de aceptación del issue son el contrato del
  trabajo, y la implementación se hace contra ellos. Si el issue fuera una funcionalidad
  grande, correspondería abrir un `REQ-` en su lugar.
- **El identificador nunca se inventa.** Si ejecutás `/start-issue 42` sin `SPEC-0NN`, el
  comando se detiene y te pide el identificador. No elige el siguiente número libre.

---

## 8. Cuando un comando falla

### La regla general

Leé el error literal y reportalo. El workflow es explícito: el resultado real de cada paso se
reporta leyendo la salida efectiva, sin asumir éxito por la ausencia de error. Si una operación
obligatoria falta o falla, se detiene y se informa: no se simula ni se sustituye.

### Fallos previstos y qué hacer

| Síntoma | Causa habitual | Qué hacer |
| --- | --- | --- |
| El comando se detiene pidiendo un identificador de SPEC | Falta `$2` | Pasá `SPEC-0NN`. El número lo elegís vos. |
| El comando se detiene por conflicto de identificador | `SPEC-0NN` ya está usado por otra funcionalidad | Verificá en `specs/`, en las ramas remotas y en los issues. No reutilizar ni renumerar por cuenta propia. |
| El script reporta que ajustó el número | El prefijo ya existe en `specs/` | Es una condición de diseño: el comando se detiene. Resolvé el conflicto antes de continuar. |
| `/speckit.implement` se detiene con un cuadro de checklists | Hay ítems sin marcar en `checklists/` | La revisión es tuya. Evaluá los criterios y marcá lo que corresponda; el agente no debe ofrecer excepciones genéricas. |
| `/speckit.analyze` reporta `CRITICAL` o `HIGH` | Inconsistencia entre spec, plan y tareas | La implementación queda bloqueada. Corregí los artefactos en otro paso autorizado y repetí el análisis. |
| El issue no cambia de estado en el proyecto | El item no está en `Football Market — Development` o el `gh` no tiene el alcance `project` | Verificá `gh auth status` y que el item exista en el proyecto. |
| La rama se publica pero no queda vinculada al issue | Falló la mutación de vinculación | El comando lo reporta; no asumas que quedó vinculada. Verificá con `gh issue develop --list <n>`. |
| El comando se detiene con el árbol de trabajo sucio | Hay cambios rastreados pendientes | El comando no descarta nada ni hace `stash`. Resolvé el estado del árbol y volvé a invocarlo. |
| El pre-commit rechaza el commit | Formato en backend o frontend | `cd backend && ./gradlew spotlessApply` o `cd frontend && npm run biome:fix`, y reintentá. |
| El pre-push ejecuta `fastTest` | El hook valida cualquier rama nueva | El hook corre la validación rápida del backend. Si el trabajo es solo de documentación, el hook la omite. |
| Una fase parece saltarse | El handoff es una propuesta, no una autorización | Invocá la fase siguiente explícitamente. |

### Cuándo escalar

- Contradicción entre la Constitución y una decisión de la funcionalidad.
- Falta una autorización humana o no está vigente.
- Un comando de Spec Kit que debería existir y no está.
- Un resultado que no se puede verificar.

En esos casos el flujo es detenerse, informar y pedir decisión humana. La skill de orquestación
tiene material específico para esto en `.opencode/skills/sdd-orchestration/references/recovery.md`.

---

## 9. Buenas prácticas y errores frecuentes

### Buenas prácticas

1. **Leé `AGENTS.md` y el workflow antes de la primera sesión.** Son cortos y explican el
   encuadre completo.
2. **Una fase por comando.** No encadenes fases en un solo pedido: cada una tiene su condición de
   entrada y su gate.
3. **«dale», «ok» y «seguí» no son aprobaciones.** Una autorización tiene que ser explícita y
   referida al alcance evaluado.
4. **El agente no aprueba gates en tu lugar.** Si te pide autorización, la das vos.
5. **El agente no ejecuta tests.** Escribí los tests si corresponde, y corrélos vos.
6. **Asigná vos el número de SPEC.** Es tu decisión y queda registrada.
7. **Reanudá con reutilización, nunca con una spec nueva.** Una SPEC existente se continúa con
   `/speckit.specify`; no se abre un identificador nuevo para el mismo trabajo.
8. **Revisá `git status --short` antes y después de cada paso.** Preservá los cambios previos.
9. **No edites los checklists para avanzar.** Son artefactos del revisor.
10. **Dejá constancia en los artefactos, no solo en el chat.** Las respuestas de CLARIFY van en
    `spec.md`; el avance, en `tasks.md`.
11. **Pedí el estado con la skill antes de improvisar.** «¿En qué estado quedó la 012?» es más
    seguro que asumirlo.

### Errores frecuentes

| Error | Consecuencia | Cómo evitarlo |
| --- | --- | --- |
| Ejecutar `/speckit.implement` sin IMPLEMENT AUTHORIZATION | El comando se detiene | Autorizá el alcance primero, de forma explícita. |
| Confiar en que un handoff autorizó la fase siguiente | Se avanza sin autorización | El handoff propone; vos autorizás. |
| Pedirle al agente que elija el número de SPEC | Se incumple la Constitución §8 | Pasá `SPEC-0NN` vos. |
| Crear la rama a mano y después correr `/start-issue` | Ramas duplicadas o conflictos | Dejá la creación de la rama al comando. |
| Usar `/speckit.specify` para continuar una SPEC ya creada | Se crea una SPEC nueva con otro identificador | `/speckit.specify` reutiliza la existente; verificá el resultado. |
| Marcar checklists sin evaluarlos | Se salta un gate de calidad | La evaluación es tuya. |
| Tratar `tasks.md` completas como funcionalidad terminada | Se confunde implementación con cierre | Falta CODE REVIEW, TESTING, CONVERGE y la autorización final. |
| Mover el estado a `In Review` sin que el trabajo esté completo | El seguimiento miente | El estado lo mueve el flujo, no la intención. |
| Ejecutar tests desde el agente | Incumple la norma del proyecto | Correlos vos. |
| Tomar el resultado de un build o un commit como aprobación | Ningún gate se concede por eso | Las autorizaciones son explícitas y verificables. |

---

## 10. Dónde está cada cosa

### Comandos y configuración del agente

| Ruta | Qué contiene |
| --- | --- |
| `.opencode/commands/` | Los diez comandos de este proyecto. |
| `.opencode/skills/sdd-orchestration/` | La skill de orquestación, con sus referencias de workflow, gates y recuperación. |
| `opencode.json` | Configuración del proyecto: modelo por defecto. |

### Infraestructura de Spec Kit

| Ruta | Qué contiene |
| --- | --- |
| `.specify/memory/constitution.md` | La Constitución: principios que ninguna funcionalidad puede contradecir. |
| `.specify/scripts/bash/create-new-feature.sh` | Materializa una SPEC: numeración, carpeta, plantilla, selección y rama. |
| `.specify/scripts/bash/check-prerequisites.sh` | Resuelve la funcionalidad activa y qué artefactos hay disponibles. |
| `.specify/scripts/bash/setup-plan.sh` | Genera `plan.md` desde la plantilla. |
| `.specify/scripts/bash/setup-tasks.sh` | Genera `tasks.md` desde la plantilla. |
| `.specify/templates/` | Plantillas de spec, plan, tasks, checklist y constitution. |
| `.specify/feature.json` | Cuál es la funcionalidad activa. |

### Artefactos y normas del proyecto

| Ruta | Qué contiene |
| --- | --- |
| `specs/<id>-<scope>/` | Los artefactos de una funcionalidad: `spec.md`, `plan.md`, `tasks.md`, `research.md`, `data-model.md`, `contracts/`, `checklists/`. |
| `specs/assignments.md` | Registro append-only de las asignaciones explícitas de identificador de SPEC. |
| `AGENTS.md` | El manual operativo del agente. |
| `docs/development/sdd-workflow.md` | El workflow normativo: fases, estados, gates y transiciones. |
| `docs/development/git-workflow.md` | Convenciones de rama, commit y Pull Request. |
| `docs/TECHNICAL_DEBT.md` | Registro centralizado de deuda técnica. |
| `tp/enunciado.md` y `tp/entregas.md` | Los requisitos externos del trabajo práctico. Son de lectura obligatoria al especificar, aclarar, planificar, implementar y revisar. |

### Seguimiento operativo

| Dónde | Qué contiene |
| --- | --- |
| GitHub Issues | La intención, el estado vigente, el bloqueo y la próxima acción de cada funcionalidad. |
| GitHub Projects (`Football Market — Development`) | El estado operativo: `📋 BACKLOG`, `🔨 IN PROGRESS`, `🔍 IN REVIEW`, `✅ DONE`. |

Las fuentes de verdad no se mezclan: los artefactos SDD mandan sobre el estado SDD, y GitHub
Issues y Projects mandan sobre el estado operativo. Una discrepancia entre ellas es una
inconsistencia que se corrige en el origen que la evidencia contradiga.

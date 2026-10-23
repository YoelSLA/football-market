# Workflow operativo SDD + Git

## 1. Alcance, autoridad y lenguaje normativo

**MUST** expresa obligación; **MUST NOT**, prohibición; **SHOULD**, recomendación cuya excepción requiere justificación; **MAY**, permiso. Ejecutar únicamente el encargo autorizado. Handoffs, artefactos y estados no autorizan operaciones por sí mismos.

MUST leer AGENTS raíz, Constitution y contexto aplicable de cada área. Constitution tiene máxima jerarquía; arquitectura, convenciones y testing siguen siendo obligatorios. La Spec define intención funcional; plan, diseño, Tasks y checklists la desarrollan. Ante conflicto obligatorio MUST detenerse y aplicar la sección Governance.

Precedencia funcional: Constitution → spec.md → clarificaciones incorporadas → plan/diseño → tasks.md → implementación → seguimiento operativo. La configuración efectiva gobierna versiones y dependencias. Consultar `tp/enunciado.md` y `tp/entregas.md` al ejecutar fases relevantes; ninguna tarjeta ni Issue sustituye requisitos externos.

El seguimiento operativo se realiza en **GitHub Issues** y **GitHub Projects**, conforme al principio de trazabilidad y fuente de verdad. `sdd-orchestration` coordina el workflow subordinado a estas normas; decide fases, autorizaciones, impacto, invalidaciones y transiciones. Spec Kit realiza sus operaciones reales: no emular comandos faltantes. Si falta o falla un comando necesario, detener y reportar.

Infraestructura efectiva: `.opencode/commands/`, `.specify/scripts/bash/`, `.specify/templates/` y `.specify/workflows/speckit/workflow.yml`. El YAML es asistido, no desatendido; no escribe en sistemas de seguimiento ni acredita autorizaciones por sí mismo. Preservar adaptaciones locales al actualizar Spec Kit.

## 2. Responsabilidades y trazabilidad

Cada funcionalidad tiene una carpeta real en `specs/`, una `spec.md` que define sus User Stories y un `tasks.md` que es la fuente única de sus tareas técnicas.

- Las **User Stories** se identifican, definen, refinan y verifican en `spec.md`. No requieren Issue propia ni tarjeta individual.
- Las **tareas `Txxx`** viven exclusivamente en `tasks.md`. MUST NOT convertirse en Issues, tarjetas ni checklists de seguimiento, ni duplicar su estado.
- Los **identificadores `REQ-<entrega>-<nn>`** de `tp/` se conservan como trazabilidad de requisito. No habilitan por sí solos ninguna unidad ejecutable.
- La **Issue** de la funcionalidad registra intención, estado vigente, bloqueo y próxima acción, y referencia `spec.md`, `plan.md` y `tasks.md`. MUST NOT replicar detalle técnico, escenarios, criterios ni avance tarea por tarea.

Conforme al principio de trazabilidad y fuente de verdad, los artefactos SDD son la fuente de verdad del estado SDD y GitHub Issues y Projects son la fuente de verdad del estado operativo. Cada fuente gobierna solo su ámbito; una discrepancia entre ambas es una inconsistencia que se corrige en el origen que la evidencia contradiga, y no se resuelve aplicando precedencia de una sobre otra. Ninguna de las dos se deduce de la existencia de archivos, tareas marcadas, builds, commits, merges o Pull Requests abiertos. Git registra la implementación observada, no la aceptación.

## 3. Registro, autorización y verificación

Las autorizaciones humanas son explícitas y verificables, y se consolidan **por alcance explícito**, no por unidad de historia. Son cuatro, conforme a la sección de autorizaciones humanas del flujo de desarrollo de la Constitución:


| Autorización | Habilita | No habilita |
| --- | --- | --- |
| IMPLEMENT AUTHORIZATION | Escribir código del alcance autorizado | El inicio por sí solo, CODE REVIEW, TESTING ni el cierre |
| CODE REVIEW | Pasar a la revisión de tests | TESTING ni el cierre |
| TESTING | Cerrar el ciclo de verificación | El cierre de la funcionalidad |
| FEATURE COMPLETION AUTHORIZATION | El cierre de la funcionalidad | Nada posterior |

Reglas:

- Ninguna autorización se concede, renueva ni infiere por otra, ni se deriva de tareas completas, build, commit, Pull Request o CONVERGE.
- `ok`, `dale` o `seguí` no son aprobación. El agente MUST NOT aprobar, rechazar ni dar por válido un gate en nombre del usuario.
- Implementar sin autorización humana explícita y vigente está prohibido.

### 3.1 Registro de la autorización y evidencia

La autorización MUST ser persistente en un soporte del proyecto y debe permitir identificar quién autorizó, qué alcance y con qué evidencia se compara. **No se prescribe un formato canónico obligatorio ni un sistema específico**: el agente elige el soporte disponible y explícito —hilo de la Issue, Pull Request, sección del reporte correspondiente o el propio artefacto—.

- La evidencia persistida bajo el workflow anterior se conserva y **puede reutilizarse** cuando sea suficiente para acreditar una autorización vigente. No se exige reemitirla ni duplicarla.
- Las invalidaciones se registran como eventos nuevos, con motivo y alcance, sin borrar evidencia histórica y sin atribuirse a una aprobación humana.
- Referenciar el alcance evaluado mediante commit, diff local o reporte disponible. No inventar hashes, resultados ni aprobaciones.
- El estado del seguimiento MUST NOT afirmarse como persistido sin verificación por lectura posterior. Un sistema de seguimiento inaccesible no autoriza a afirmar sincronización, ni habilita la transición dependiente, ni habilita descartar cambios locales: se informa el bloqueo y se registra la operación pendiente.

No se mantienen índices operativos manuales, checklists de fase ni checklists que repliquen `tasks.md`. Esa información reside en los artefactos SDD y en el sistema de seguimiento.

## 4. Estados y transiciones

Estados canónicos, en orden: **`Backlog` → `In Progress` → `In Review` → `Done`**. No crear estados ni columnas adicionales; los cambios estructurales requieren autorización humana.

| Estado | Condición de entrada |
| --- | --- |
| `Backlog` | Funcionalidad identificada, sin trabajo autorizado. |
| `In Progress` | Existe IMPLEMENT AUTHORIZATION vigente para el alcance y el trabajo está en curso. |
| `In Review` | Implementación completa; revisión humana de código, testing o cierre pendientes. |
| `Done` | Implementación, CODE REVIEW, TESTING y FEATURE COMPLETION AUTHORIZATION, todo vigente y satisfecho. |

Reglas:

- Un cambio de estado requiere evidencia o confirmación explícita del usuario. Completar tareas, un build, un commit, un merge o un Pull Request no habilitan `In Review` ni `Done`.
- `Done` no sustituye al `Status` de `spec.md` ni acredita tests.
- Conservar y explicar los estados heredados. Los hallazgos técnicos siguen abiertos hasta su resolución comprobada.

## 5. Inicio y materialización

MUST comprobar si corresponde reutilizar una SPEC. Antes de SPECIFY, si la intención es demasiado ambigua y no hay spec, pre-clarificar lo mínimo; no sustituye CLARIFY. Dividir el alcance de una funcionalidad requiere aprobación humana.

Mecanismo real, desde raíz, solo en Specify autorizado:

```bash
.specify/scripts/bash/create-new-feature.sh --json --short-name '<scope>' '<requisito autorizado>'
.specify/scripts/bash/create-new-feature.sh --json --reuse --short-name '<directorio-o-scope-existente>' '<trabajo autorizado>'
```

No ejecutar placeholders. Revisar scripts/hooks antes de usarlos. Reanudación usa siempre `--reuse`; no asignar nueva identidad para continuar una existente. Con `--reuse`, `--number` desambigua/valida la identidad existente; `--allow-existing-branch` no sustituye reuse. Leer SPEC_NAME, SPEC_FILE, FEATURE_NUM, BRANCH_NAME, SPEC_ACTION y BRANCH_STATUS; reportar campos faltantes y fallos, no asumir rama disponible. Preservar cambios locales: sin stash/reset automático. Verificar directorio resuelto por SPECIFY_FEATURE_DIRECTORY / `.specify/feature.json` antes de fases; la última spec no determina tarea.

La numeración automática es predeterminada. Excepcionalmente, para una SPEC nueva MAY añadirse `--number <id-asignado>` cuando se cumplan las condiciones de asignación explícita de la Constitución: distribución previa documentada de funcionalidades entre integrantes que trabajan en paralelo y registro trazable del número, funcionalidad, responsable y motivo antes de crearla. Antes de toda creación MUST verificar colisiones con specs locales, remotas accesibles y asignaciones conocidas por el equipo, registrando fuentes y resultado; si no pueden descartarse, detenerse y solicitar confirmación. MUST NOT reutilizar identificadores asignados a otra funcionalidad. Si el script ajusta el número, reportarlo antes de continuar la redacción.

Al materializarse la SPEC, se crea o reutiliza su Issue de seguimiento con el título `SPEC-0XX | Nombre de la funcionalidad` y se referencia el requisito `REQ-` correspondiente, preservando el antecedente cuando la correspondencia esté confirmada. No inventar US ni Tasks, ni convertir ideas en unidades ejecutables.

El `<scope>` del directorio de la SPEC y el de su rama van en **inglés**, conforme a `docs/development/git-workflow.md`, con independencia del idioma del issue, del título de la funcionalidad o de la descripción de origen.

### 5.1 Inicio de un issue desde GitHub

El comando `/start-issue <issue_number> <spec_id> [<scope>]` automatiza el arranque operativo de un issue en GitHub: verifica el issue y sus dependencias, materializa la SPEC, crea y publica la rama desde `origin/dev`, la vincula al issue, actualiza el seguimiento en GitHub Projects y deja el contexto preparado para el flujo SDD. Su definición es `.opencode/commands/start-issue.md`.

Reglas de la operación:

- El identificador de la SPEC lo provee la persona que invoca el comando. MUST NOT generarse, inferirse ni asignarse automáticamente; si falta, el comando se detiene y lo pide.
- Antes de modificar el issue, MUST verificarse que el identificador no esté en uso, cruzando specs locales, ramas remotas, issues de GitHub e items del proyecto, y registrando el resultado de cada fuente. Ante cualquier coincidencia el comando se detiene y reporta; MUST NOT reasignarse ni corregirse el número.
- MUST preservarse íntegramente el body del issue —incluidos RF, CA, dependencias y referencias—, los labels, el milestone, las asignaciones y las relaciones con otros issues. La conservación se verifica comparando el estado previo con el resultante, no se presume.
- La rama se crea desde `origin/dev` y se publica; su referencia remota se vincula al issue en una sola operación.
- El registro de seguimiento pasa a `In Progress` al crear y publicar la rama, que es el inicio del trabajo sobre el issue, conforme a §4.
- Las issues `TSK-` no exigen identificador de SPEC y su título no se modifica; su rama sigue el formato `<type>/<scope>` de las convenciones de Git.
- La asignación explícita de numeración se registra en `specs/assignments.md`, conforme a la Constitución §8, antes de crear la SPEC.
- El comando MUST NOT implementar código funcional, redactar la especificación ni autorizar implementación. La continuación es SPECIFY.

El título del registro de seguimiento que este comando establece es el formato de §5: `SPEC-0XX | Nombre de la funcionalidad`.

## 6. Protocolo de preparación e implementación

Leer la definición del comando correspondiente y ejecutar solo la operación autorizada. Inspeccionar hooks; nunca activar ejecución directa o indirecta de tests.

- **SPECIFY:** materializar intención, US, criterios, escenarios, requisitos y límites; evaluar calidad. Artefacto no equivale a fase completada. Ambigüedades pendientes se explicitan para CLARIFY.
- **CLARIFY:** siempre obligatorio después de SPECIFY. Resolver decisiones funcionales y registrarlas en spec, no solo chat. Cero preguntas es válido si el escaneo acredita claridad; omitir no equivale a completar.
- **PLAN:** diseño implementable, Constitution Check antes y después, contratos y consumidores. Mantener plan, research, data-model, contracts y quickstart pertinentes, justificando no aplicabilidad. Corregir requisitos upstream, no ocultarlos en diseño. La documentación del contrato HTTP de una feature se sostiene en sus `contracts/`, en las anotaciones OpenAPI de Controllers y DTO y en los tests funcionales de Controller; ninguna tarea de entrega puede depender de una herramienta de documentación retirada sin sustituirla por esas fuentes.
- **CHECKLIST:** obligatorio; general de calidad más especializados aplicables, selección justificada. Generación no es evaluación; respetar propiedad del revisor, no marcar para avanzar.
- **TASKS:** preservar IDs y avance; cobertura, dependencias, rutas, tareas compartidas y verificabilidad. Incluir creación o modificación de tests backend cuando las normas aplicables lo exijan; no tests frontend ni tareas humanas de autorización. Tareas completas significan implementación completa, no cierre.
- **ANALYZE:** `/speckit.analyze` estrictamente read-only. Contrastar Constitution, spec y clarificaciones, diseño, Tasks y checklists. Contradicciones normativas o funcionales, alcance no aprobado, cobertura insuficiente o criterio obligatorio no verificable son blockers. Hallazgos editoriales sin impacto material se reportan y no impiden avanzar. `Dependency ≠ Blocker`: dependencias técnicas conocidas, identificadas y resolubles mediante orden técnico y autorizaciones no son blockers.

Si ANALYZE detecta una corrección mecánica inequívoca, el orquestador MAY realizar una única corrección **fuera del comando**, en operación separada. Informar exactamente lo corregido y detener. No hay loops autónomos de análisis y corrección.

Si ANALYZE no reporta blockers, la preparación queda completa. **No existe un gate READY adicional**: la condición que READY evaluaba —preparación global válida y sin blockers de especificación, planificación o consistencia— es la que ANALYZE determina. READY no autoriza implementar.

**IMPLEMENT AUTHORIZATION:** presentar el alcance a implementar, sus dependencias y su independencia. El usuario autoriza explícitamente el alcance; el agente decide el orden técnico. Advertir combinaciones inconsistentes antes de implementar. La autorización habilita IMPLEMENT y **no** lo inicia: el trabajo se inicia cuando la operación autorizada comienza realmente. Autorizaciones afectadas por cambios upstream requieren nueva aprobación; las no afectadas se conservan.

**CONVERGE:** cierre de la funcionalidad, cuando el trabajo técnico necesario esté completo. `/speckit.converge` es diagnóstico y append-only: únicamente anexa remediaciones conforme a su comando; nunca código, tests ni correcciones documentales dentro del comando. Complementar fuera del comando la revisión de evidencias, documentación, checklists, Git y trazabilidad.

Faltantes de implementación requerida: reabrir el trabajo afectado y repetir CODE REVIEW y TESTING según impacto. Reimplementar el alcance autorizado y repetir solo los pasos afectados antes de otro CONVERGE. Comportamiento no definido: detener, decisión humana upstream, artefactos afectados y nuevo ANALYZE. Corrección documental mecánica inequívoca MAY ocurrir **fuera de CONVERGE** como operación separada, seguida por nueva ejecución.

**FEATURE COMPLETION AUTHORIZATION:** CONVERGE exitoso prepara el cierre, no lo autoriza. Presentar el resultado final y pedir autorización humana inequívoca referida a la funcionalidad y al resultado evaluado. Una modificación con impacto en la base del cierre invalida la autorización afectada; reevaluar, repetir los pasos necesarios y obtener nueva autorización. No aceptar `ok`, `dale` o `seguí` como cierre.

## 7. Ejecución del trabajo autorizado

1. Identificar la SPEC real, su carpeta, la rama y el alcance autorizado. Requisitos sin SPEC, `Backlog` sin autorización y tareas técnicas no son entradas ejecutables.
2. Leer normas, fuentes externas, artefactos, implementación, consumidores, tests y configuración afectados. Revisar git status y diff, preservar trabajo previo.
3. Implementar únicamente el alcance autorizado, en orden técnico. `[P]` no autoriza delegación ni solapar archivos.
4. Actualizar `tasks.md` solo por trabajo realizado o verificado. Escribir tests no acredita ejecutarlos. Separar pendientes humanos. No cambiar requisitos ni checklists del revisor para justificar desviaciones.
5. Ambigüedad funcional: detener la implementación, resolver upstream, actualizar los artefactos afectados y volver a ANALYZE. La implementación completa habilita CODE REVIEW con report completo, no el cierre: CODE REVIEW y TESTING son autorizaciones separadas.
6. CODE REVIEW: **Code Review Report completo**, con resumen y funcionamiento, todos los archivos creados o modificados incluidos tests, responsabilidad, cambios y razones, decisiones, referencias concretas, orden de revisión y checklist humano. El usuario aprueba explícitamente el checklist completo; el agente no aprueba. Rechazo → corrección → nuevo report y nueva revisión obligatorios.
7. TESTING: **Testing Report completo**, con tests escritos, cobertura de criterios y casos, mocks y assertions, riesgos, revisión recomendada y comandos exactos para el usuario. El usuario revisa cobertura, ejecuta y confirma resultado satisfactorio. No exigir salida completa si pasan; solicitarla si fallan.
8. Tests automatizados razonablemente no aplicables: justificar la no aplicabilidad y solicitar validación humana explícita; no tests artificiales ni testing manual genérico no definido.
9. Fallo que exige código o tests → volver a implementación → CODE REVIEW → TESTING. Bloqueo externo sin cambios → permanecer en revisión, registrar el bloqueo con causa concreta; dependencia pendiente → registrada como tal. No crear estados por cada causa.
10. Revisión o tests que afecten el alcance aprobado invalidan las autorizaciones correspondientes, independientemente del autor del cambio; los cambios sin impacto no invalidan. Nunca cerrar por tareas, build, commit, Pull Request o CONVERGE.

## 8. Verificación y entrega

La sección de verificación y testing de la Constitución y los `AGENTS.md` gobiernan: el agente no ejecuta tests de aplicación, directa ni indirectamente, ni controles adicionales salvo las validaciones controladas de infraestructura de desarrollo expresamente autorizadas conforme a esa misma sección. Esta excepción no habilita tests de aplicación ni amplía los builds permitidos. Backend: crear o modificar tests necesarios conforme a normas de testing, el usuario los ejecuta. Frontend exento: no crear ni ejecutar tests ni infraestructura.

| Cambio de código | Control permitido |
| --- | --- |
| Backend | `./gradlew build -x test` desde backend, según AGENTS del área. |
| Frontend | `npm run build` desde frontend, según AGENTS del área. |
| Ambos | Ambos builds sin tests. |
| Solo documentación | Coherencia, rutas, enlaces y diff; sin build. |

Revisar diff final, `git diff --check` y `git status --short`. Distinguir fallos nuevos, preexistentes demostrablemente ajenos y de entorno. Fallo nuevo de build bloquea CODE REVIEW; fallo preexistente ajeno puede permitir entrega conforme a la Constitución, reportando evidencia. Entorno sin clasificación deja validación pendiente. El build permite entrega técnica, no aprueba TESTING: los tests aplicables pendientes impiden el cierre. Sonar y controles opcionales del usuario no son gates obligatorios.

CI y hooks existentes no acreditan ejecución. Inspeccionar antes de commit o push; no disparar ni deshabilitar controles prohibidos. Si una operación dispara tests o controles no permitidos, entregar los cambios al usuario para que la ejecute. No afirmar publicación ni pruebas no observadas.

## 9. Git y evidencia de implementación

Las convenciones de ramas, commits y Pull Requests están en
[`git-workflow.md`](git-workflow.md), que es su fuente única. Aquí solo se fija la relación que
el flujo SDD necesita.

Mantener la trazabilidad SPEC ↔ User Stories ↔ Tasks ↔ rama ↔ commits ↔ código ↔ Pull Request. No
imponer rama por US ni commit por tarea. Reutilizar la identidad de la spec: no renumerar, no
reset y no hacer stash de cambios ajenos.

Declarar en el commit la spec, las User Stories o las tareas correspondientes y el alcance
compartido, con hashes reales cuando existan. Vincular el Pull Request a la funcionalidad y a su
registro de seguimiento; vincularlo no acredita implementación, revisión ni cierre.

No inventar URL ni hash de Pull Request. El diff local revisable basta para revisión; aprobar el
estado exacto no implica merge ni publicación. No hacer commit, crear ramas ni push sin encargo.

## 10. Fallos, recuperación y cierre

Inconsistencia que impide determinar el comportamiento correcto: detener la transición afectada, reportar fuente, evidencia, impacto y decisión necesaria. Conservar trabajo e historial; reportar no autoriza corrección fuera del encargo. La falta de spec, una rama incompatible, un ANALYZE bloqueante o artefactos insuficientes impiden implementar. Una discrepancia en el seguimiento operativo no redefine requisitos. El trabajo parcial se conserva; no marcar validación inexistente.

Definición de cierre de una funcionalidad: trabajo técnico necesario completo, implementación completa, CODE REVIEW vigente, TESTING aprobado o no aplicabilidad validada, CONVERGE final satisfactorio vigente, FEATURE COMPLETION AUTHORIZATION humana explícita vigente y evidencia persistida conforme a §3.1. No equivale a `tasks.md` completo.

Informar estado estructurado conforme a `sdd-orchestration`: funcionalidad, fase SDD, `Status` de la spec, estado de seguimiento, autorizaciones vigentes, blockers, sincronización pendiente y siguiente transición permitida; incluir los reports correspondientes al entrar a sus gates.

## 11. Disposición transitoria

Este documento es el protocolo subordinado a la Constitución. La historia de sus enmiendas está en [`.specify/memory/history/constitution-changelog.md`](../../.specify/memory/history/constitution-changelog.md). La coherencia con las skills, los comandos y la guía humana es una sincronización subordinada pendiente.

Hasta que esa sincronización se complete:

- El principio de trazabilidad y fuente de verdad de la Constitución prevalece sobre cualquier texto subordinado que aún describa columnas, tarjetas, índices manuales, checklists de fase o el formato `[SDD GATE]` como obligatorios.

## 12. Estado del sistema de seguimiento

**Configuración efectiva.** El sistema de seguimiento operativo del proyecto es GitHub Issues con
GitHub Projects. El tablero es `Football Market — Development` (proyecto de usuario `YoelSLA`,
número 4) y está configurado y en uso. Este documento describe el estado observado; la
configuración efectiva se verifica antes de cada operación y prevalece sobre lo aquí escrito.

**Estado verificado el 2026-10-21.** 28 items en el tablero y 13 campos, con el campo `Status` y
sus cuatro opciones canónicas `📋 BACKLOG`, `🔨 IN PROGRESS`, `🔍 IN REVIEW` y `✅ DONE`.
Distribución observada: 12 items en `✅ DONE`, 14 en `📋 BACKLOG` y 2 en `🔨 IN PROGRESS`. El
tablero sostiene **una Issue por funcionalidad**: 12 items con identificador `SPEC-0XX`, 1 item
`TSK-` y 15 items `REQ-` de funcionalidades todavía no materializadas como SPEC.

- La migración no es uniforme: hay specs materializadas sin item en el tablero y specs registradas
  en el tablero cuyo artefacto local todavía no está en `dev`. La cobertura se verifica item por
  item; no se presume ni se afirma sincronización de nada que no se haya observado.
- Cada Issue migrada referencia sus artefactos SDD y refleja su estado real. Crear el item o
  registrar un estado **no autoriza** implementar, revisar, validar tests ni cerrar.
- Las funcionalidades en curso conservan su estado real y su evidencia histórica. La evidencia ya
  persistida mantiene su validez y puede acreditar autorizaciones conforme a §3.1; no se exige
  reemitirla ni duplicarla.
- No se inventa ningún estado operativo, no se declaran funcionalidades migradas sin verificarlas
  y no se afirma sincronización de nada que no se haya observado.
- Ninguna funcionalidad se considera cerrada por el solo hecho de escribir en el seguimiento.
- El tablero anterior queda **retirado**: no se le crean, actualiza ni consulta entradas, y su
  evidencia se conserva únicamente como archivo histórico.

[specs/trello.md](../../specs/trello.md) se conserva como **archivo histórico congelado**, sin
mantenimiento. Sus secciones son evidencia y referencias históricas, no un índice vigente, y sus
enlaces a documentos o anclas que cambiaron describen el estado de aquel momento.

Las aprobaciones anteriores, los informes de revisión y testing, los registros de las
especificaciones históricas y los Sync Impact Reports **no se eliminan ni se reescriben**. Cuando
correspondan al alcance aprobado, mantienen su validez como evidencia.

Esta sección no ejecuta el lifecycle, no modifica sistemas de seguimiento y no acredita fases
históricas. Los hallazgos H1–H4 y el conflicto de tests de `tasks.md` de SPEC-007 permanecen fuera
de alcance.
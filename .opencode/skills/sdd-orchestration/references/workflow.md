# Workflow — fases, identidad y transiciones

## Principios

- La **fase SDD** es la operación actual del ciclo de desarrollo; el **estado operativo de seguimiento** es el progreso observable en el sistema de seguimiento. No confundirlos.
- Los estados de seguimiento son cuatro y están definidos en el workflow operativo: `Backlog → In Progress → In Review → Done`. Aquí no se inventan otros.
- Conforme al principio de trazabilidad y fuente de verdad, los artefactos SDD son la fuente de verdad del estado SDD y el sistema de seguimiento es la fuente de verdad del estado operativo. **No hay precedencia universal** entre ambas: una columna no describe la fase SDD y un `Status` no describe el avance de seguimiento. Ante discrepancia, corregir el origen que la evidencia contradiga.
- Las	User Stories viven en `spec.md` y las tareas `Txxx` exclusivamente en `tasks.md`. **Ninguna de las dos se replica en el seguimiento**, y ninguna es una entrada ejecutable por sí misma.

## Fases de preparación

Cada operación se ejecuta mediante el comando local de `.opencode/commands/` correspondiente. Ejecutar solo la fase autorizada.

| Fase | Operación | Condición para avanzar |
| --- | --- | --- |
| SPECIFY | `/speckit.specify` | `spec.md` identificable, con alcance, US, criterios de aceptación y límites explícitos. |
| CLARIFY | `/speckit.clarify` | Ambigüedades funcionales relevantes resueltas y **reflejadas en la spec**, no solo en el chat. |
| PLAN | `/speckit.plan` | Diseño implementable, coherente con spec y Constitución, con Constitution Check antes y después. Mantener `plan.md`, `research.md`, `data-model.md`, `contracts/` y `quickstart.md` pertinentes, **justificando** los que no apliquen. |
| CHECKLIST | `/speckit.checklist` | Checklists aplicables **evaluados por el revisor**; generación no equivale a aceptación. |
| TASKS | `/speckit.tasks` | `tasks.md` cubre US, criterios, dependencias y prerrequisitos; sin alcance nuevo. |
| ANALYZE | `/speckit.analyze` | Sin blockers; hallazgos no bloqueantes comunicados. |

Reglas de fase:

- **CLARIFY siempre es obligatorio** después de SPECIFY. Un escaneo sin ambigüedades puede cerrarlo con evidencia; omitir el escaneo no equivale a completarlo.
- **CHECKLIST es obligatorio**: un checklist general de calidad de requisitos más los especializados aplicables (security, API, UX, data, authentication, authorization). El agente selecciona y **justifica** la selección.
- Las checklists son propiedad del revisor: no marcar ítems para avanzar ni modificar sus marcadores.
- **ANALYZE es read-only** y obligatorio antes de solicitar autorización de implementación. Contradicciones normativas o funcionales, alcance no aprobado, cobertura insuficiente o criterio obligatorio no verificable son **blockers**. Los hallazgos editoriales sin impacto material se reportan y no impiden avanzar.
- `Dependency ≠ Blocker`: dependencias técnicas conocidas, identificadas y resolubles mediante orden técnico y autorizaciones no son blockers, conforme a los principios de proporcionalidad del alcance y de trazabilidad.

### Corrección única fuera del comando

Si ANALYZE detecta una inconsistencia mecánica cuya corrección es inequívoca según la fuente de verdad, el orquestador MAY realizar **una** corrección **fuera del comando**, en operación separada, e informar exactamente lo corregido, y **detenerse**. El nuevo ANALYZE ocurre en una operación posterior.

Prohibido el loop autónomo `analyze → fix → analyze → fix`; ese ciclo lo dirige el usuario.

### Requisitos no ejecutables

Una idea o requisito sin SPEC puede pre-clarificarse mínimamente antes de SPECIFY. Dividir el alcance de una funcionalidad requiere **aprobación humana**: proponer la división y esperar confirmación de los límites. Una épica o un agrupamiento solo se crean cuando exista una necesidad real de coordinación.

## Identidad de la Feature

Determinar la Feature antes de actuar:

1. Identificar la rama activa.
2. Identificar el directorio de spec y sus artefactos.
3. Determinar la fase actual y detectar artefactos downstream posiblemente obsoletos.
4. Si no hay identificación inequívoca, detenerse y preguntar. **Nunca** seleccionar una Feature dudosa en silencio.

La numeración más alta no identifica la tarea. No reiniciar desde SPECIFY si existen artefactos válidos: continuar desde la primera fase incompleta o inválida.

Para materializar o retomar una SPEC se usa el mecanismo real del repositorio:

```bash
# nueva feature
.specify/scripts/bash/create-new-feature.sh --json --short-name '<scope>' '<requisito autorizado>'

# retomar una spec existente
.specify/scripts/bash/create-new-feature.sh --json --reuse --short-name '<directorio-o-scope-existente>' '<trabajo autorizado>'
```

- **Reanudar trabajo de una SPEC existente siempre con `--reuse`**, para no crear una identidad nueva. Sin `--reuse` el flujo crea otra spec y, por defecto, asigna el siguiente número; la asignación explícita excepcional no autoriza duplicar una SPEC existente.
- `--reuse` reutiliza `spec-id`, nombre y directorio; no sobrescribe `spec.md` ni los demás artefactos. Resuelve por coincidencia exacta; sin coincidencia o con varias, falla y las lista.
- Con `--reuse`, `--number` desambigua o valida la identidad existente; falla si contradice la spec encontrada. Para una SPEC nueva, MAY utilizarse `--number` únicamente bajo la excepción de trabajo paralelo de la Constitución, manteniendo la numeración automática como procedimiento predeterminado.
- `--allow-existing-branch` **no** sustituye `--reuse`: el primero permite que la creación caiga sobre un directorio existente.
- Ninguno descarta, sobrescribe ni hace `stash`. Con cambios rastreados pendientes o `HEAD` separado, el paso de rama se aborta y la spec se crea o reutiliza igual.
- Leer `SPEC_ACTION` y `BRANCH_STATUS` para comprobar el resultado, además de `SPEC_NAME`, `SPEC_FILE`, `FEATURE_NUM` y `BRANCH_NAME`. **No asumir** que la rama está disponible por el texto.
- Verificar el directorio efectivo por `SPECIFY_FEATURE_DIRECTORY` / `.specify/feature.json` y overrides de entorno antes de editar.
- No predecir, reservar ni asignar números fuera de esa excepción. Esta exige distribución previa documentada de funcionalidades entre integrantes que trabajan en paralelo y registro trazable del número, funcionalidad, responsable y motivo antes de crear la SPEC. Al materializarla, referenciar el registro y mantener trazabilidad con requisito, rama y seguimiento.
- Antes de toda creación, verificar colisiones con specs locales, remotas accesibles y asignaciones conocidas del equipo; registrar fuentes y resultado. Si no pueden descartarse, detenerse y solicitar confirmación. No reutilizar identificadores asignados a otras funcionalidades. Si el script ajusta el número explícito, reportarlo antes de continuar la redacción: no acredita la asignación acordada.
- Una asignación explícita no acredita existencia, fases, autorizaciones, implementación o integración. Una carpeta provisional o `spec.md` vacío no acredita una SPEC validada ni es requisito para asignar otra identidad. La excepción no permite sobrescribir specs, renumerarlas unilateralmente, omitir autorizaciones ni afectar cambios locales preexistentes.

Al materializarse la SPEC, se crea o reutiliza su registro de seguimiento con el título `SPEC-0XX | Nombre de la funcionalidad`, referenciando el requisito `REQ-` correspondiente y preservando el antecedente cuando la correspondencia esté confirmada. Ese registro **no** contiene tareas, criterios ni detalle técnico.

## Preparación completa

**No existe un gate READY.** Lo que READY evaluaba se evalúa aquí y en ANALYZE:

- Preparación global válida y coherente entre `spec.md`, `plan.md`, `tasks.md` y checklists.
- ANALYZE vigente **sin blockers**.
- Ambigüedades funcionales relevantes resueltas.
- Criterios de aceptación cubiertos por tareas.
- Sin requisitos nuevos introducidos sin decisión humana.

Un blocker en cualquier US impide la preparación global, porque ninguna funcionalidad se implementa parcialmente sin cobertura completa de sus criterios.

Cuando estas condiciones se cumplen, la preparación está completa y corresponde **solicitar la autorización de implementación**. No se pide una confirmación humana adicional de "READY", y no hay ninguna sincronización pendiente que la acompañe.

## IMPLEMENT

Única fase que modifica product code. Nunca modificar requisitos silenciosamente porque el código existente los complique, y nunca inventar alcance.

- Respetar `Constitution → spec.md → plan.md → tasks.md`.
- El usuario autoriza el **alcance** a implementar; el agente decide el **orden técnico** respetando dependencias.
- Advertir antes de implementar una combinación inconsistente de alcances o prerrequisitos.
- Marcar una tarea solo con trabajo realizado y verificado; **escribir tests no acredita ejecutarlos**.
- Registrar los pendientes de verificación humana sin darlos por cumplidos.
- Un requisito sin SPEC, un registro en `Backlog` sin autorización y una tarea técnica aislada no son entradas ejecutables. No consumir todas las tareas por defecto.

### Durante la implementación

- El registro de seguimiento pasa a `In Progress` **cuando el trabajo comienza**, no al autorizarlo.
- Puede continuarse con trabajo autorizado e independiente mientras una parte espera verificación humana; **evaluar el impacto** del feedback recibido antes de continuar trabajo relacionado que dependa de él.
- El agente puede crear o modificar tests durante IMPLEMENT; su ejecución corresponde al usuario.
- El frontend está **exento de testing**: no crear tests ni incorporar infraestructura para ellos.

### Ambigüedad funcional durante IMPLEMENT

```text
STOP IMPLEMENT → resolver upstream → actualizar artefactos afectados
→ ANALYZE → continuar con las autorizaciones vigentes
```

Las autorizaciones no afectadas se conservan; las afectadas requieren nueva autorización. No parchear alrededor de una especificación incorrecta.

## Revisión y testing

Cuando la implementación del alcance autorizado está completa, el registro de seguimiento pasa a `In Review` **pendiente de revisión integral**. Eso no implica que la revisión esté aprobada.

- **CODE REVIEW** y **TESTING** son autorizaciones humanas separadas, sobre el alcance implementado completo, **incluidos los tests**. Revisar y aprobar código no sustituye revisar y aprobar pruebas.
- Las condiciones, el contenido de los reports y el checklist obligatorio están en [`gates.md`](gates.md).
- Los tests aplicables pendientes impiden el cierre, conforme a la sección de verificación y testing.

## CONVERGE y cierre

- Cuando el trabajo técnico necesario está completo, corresponde ejecutar `/speckit.converge`. **No ejecutar CONVERGE antes**: es un prerrequisito del comando.
- CONVERGE es diagnóstico y **append-only**: anexa remediaciones a `tasks.md` y no modifica código, tests ni otros documentos. Detalles en [`gates.md`](gates.md) y [`recovery.md`](recovery.md).
- La corrección documental inequívoca ocurre **fuera del comando**, en operación separada, seguida de nueva ejecución. No repetir autorizaciones no afectadas.
- CONVERGE exitoso **prepara** el cierre, no lo autoriza.
- La transición a `Done` y el estado `Approved` de `spec.md` exigen autorización final humana explícita, vigente y persistida.

## Git

- Las convenciones de rama, commit y Pull Request están en `docs/development/git-workflow.md`, que es su fuente única.
- Antes de crear una rama, determinar si el cambio pertenece a una spec existente. No asociar artificialmente ni crear spec nueva para un cambio ya asociado.
- Mantener trazabilidad spec ↔ User Stories ↔ tasks ↔ rama ↔ commits ↔ código ↔ Pull Request. No imponer rama por US ni commit por tarea.
- Declara en el commit la SPEC, US o Tasks correspondientes y el alcance compartido, con hashes reales cuando existan.
- Vincular el Pull Request a la funcionalidad y a su registro de seguimiento. Vincularlo no acredita implementación, revisión ni cierre.
- No inventar URL ni hash de Pull Request. No hacer commit, crear ramas ni push sin encargo.

## Verificación y entrega

El agente no ejecuta tests. El único control ejecutable permitido es el build **sin tests** del área cuyo código cambió, con los comandos de su `AGENTS.md`. Los cambios solo documentales no requieren build.

| Cambio | Control permitido |
| --- | --- |
| Backend | `./gradlew build -x test` desde `backend` |
| Frontend | `npm run build` desde `frontend` |
| Ambos | Ambos builds sin tests |
| Solo documentación | Coherencia, rutas, enlaces y diff; sin build |

SonarQube, Quality Gates y otros controles opcionales quedan a cargo del usuario y no condicionan la finalización. Un fallo preexistente verificablemente ajeno al cambio se reporta y no bloquea la entrega.

## Disposición transitoria

La migración al tablero de GitHub Projects está en curso y no es completa:

- El tablero anterior fue retirado. GitHub Projects está configurado (`Football Market — Development`) y las funcionalidades migradas tienen su estado real en el tablero: no se afirma sincronización ni se declaran funcionalidades migradas sin verificarlas. La evidencia histórica existente se reutiliza cuando es suficiente para acreditar una autorización.
- La migración no es uniforme: hay funcionalidades sin item en el tablero y specs registradas en el tablero cuyo artefacto local todavía no está en `dev`. La cobertura se verifica item por item.
- SPEC-009 y SPEC-010 conservan su estado real y **no se migran ni se modifican** en esta fase.
- Ninguna instrucción de este archivo autoriza operaciones de escritura sobre el sistema de seguimiento sin autorización operativa explícita.
# Workflow — fases, identidad y transiciones

## Principios

- La **fase SDD** es la operación actual; el **estado Trello** es el progreso observable. No confundirlos.
- Las columnas, su orden y las claves de tarjeta están definidos en `trello-traceability`. Aquí no se enumeran.
- `SPECIFYING` agrupa internamente SPECIFY, CLARIFY, PLAN, CHECKLIST, TASKS y ANALYZE; conservar la fase detallada aunque la columna las agrupe.
- `TESTING` tiene doble semántica: para una **US**, validación humana de pruebas; para una **Feature**, CONVERGE final. La transición normal de la Feature **no** utiliza la columna `CODE REVIEW`, que es propia de US.

## Fases de preparación

Cada operación se ejecuta mediante el comando local de `.opencode/commands/` correspondant. Ejecutar solo la fase autorizada.

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
- **ANALYZE es read-only** y obligatorio antes de READY. Contradicciones normativas o funcionales, alcance no aprobado, cobertura insuficiente o criterio obligatorio no verificable son **blockers**. Los hallazgos editoriales sin impacto material se reportan y no impiden READY.

### Corrección única fuera del comando

Si ANALYZE detecta una inconsistencia mecánica cuya corrección es inequívoca según la fuente de verdad, el orquestador MAY realizar **una** corrección **fuera del comando**, en operación separada, e informar exactamente lo corregido, y **detenerse**. El nuevo ANALYZE ocurre en una operación posterior.

Prohibido el loop autónomo `analyze → fix → analyze → fix`; ese ciclo lo dirige el usuario.

### Requisitos no ejecutables

Una idea o requisito sin SPEC puede pre-clarificarse mínimamente antes de SPECIFY. **Nunca** dividir automáticamente una EPIC: proponer la división y esperar aprobación humana de los límites.

## Identidad de la Feature

Determinar la Feature antes de actuar:

1. Identificar la rama activa.
2. Identificar el directorio de spec y su artefactos.
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
- Con `--reuse`, `--number` desambigua o valida la identidad existente; falla si contradice la spec encontrada. Para una SPEC nueva, MAY utilizarse `--number` únicamente bajo la excepción de trabajo paralelo de Constitution §8, manteniendo la numeración automática como procedimiento predeterminado.
- `--allow-existing-branch` **no** sustituye `--reuse`: el primero permite que la creación caiga sobre un directorio existente.
- Ninguno descarta, sobrescribe ni hace `stash`. Con cambios rastreados pendientes o `HEAD` separado, el paso de rama se aborta y la spec se crea o reutiliza igual.
- Leer `SPEC_ACTION` y `BRANCH_STATUS` para comprobar el resultado, además de `SPEC_NAME`, `SPEC_FILE`, `FEATURE_NUM` y `BRANCH_NAME`. **No asumir** que la rama está disponible por el texto.
- Verificar el directorio efectivo por `SPECIFY_FEATURE_DIRECTORY` / `.specify/feature.json` y overrides de entorno antes de editar.
- No predecir, reservar ni asignar números fuera de la excepción de Constitution §8. Esta exige distribución previa documentada de funcionalidades entre integrantes que trabajan en paralelo y registro trazable del número, funcionalidad, responsable y motivo antes de crear la SPEC. Al materializarla, referenciar el registro y mantener trazabilidad con requisito, rama y seguimiento.
- Antes de toda creación, verificar colisiones con specs locales, remotas accesibles y asignaciones conocidas por el equipo; registrar fuentes y resultado. Si no pueden descartarse, detenerse y solicitar confirmación. No reutilizar identificadores asignados a otras funcionalidades. Si el script ajusta el número explícito, reportarlo antes de continuar la redacción: no acredita la asignación acordada.
- Una asignación explícita no acredita existencia, fases, gates, aprobación, implementación o integración. Una carpeta provisional o `spec.md` vacío no acredita una SPEC validada ni es requisito para asignar otra identidad. La excepción no permite sobrescribir specs, renumerarlas unilateralmente, omitir sincronización o gates ni afectar cambios locales preexistentes.

## READY

`READY` es estado de **Feature**, no artefacto de Spec Kit, y no es una columna propia de Spec Kit.

Condiciones:

- Preparación global válida y coherente entre spec, plan, tasks y checklists.
- ANALYZE vigente sin blockers.
- Ambigüedades funcionales relevantes resueltas.
- Criterios de aceptación cubiertos por tareas.
- Sincronización de tarjetas US completada en este cierre: exactamente una tarjeta por US formal de `spec.md`, creada o reutilizada, con claves canónicas, vínculo con la épica, referencias a `spec.md` y `tasks.md`, columna `READY` salvo estado previo válido, índice `specs/trello.md` actualizado y evidencia persistida en la épica. Si falla o queda incompleta, READY no se declara completado.

**Un blocker en cualquier US impide READY global.**

**Dependency ≠ Blocker:** dependencias técnicas conocidas, identificadas y resolubles mediante orden técnico y autorizaciones no impiden READY.

Efecto: la Feature y las US válidas alcanzan `READY`. Las tarjetas US **no** se crean durante SPECIFYING: su alta o reutilización es parte de este cierre y se delega en `trello-traceability`.

**READY no autoriza implementar.** Requiere autorización humana explícita.

## IMPLEMENT

Única fase que modifica product code. Nunca modificar requisitos silenciosamente porque el código existente los complique, y nunca inventar alcance.

- Respetar `Constitution → spec.md → plan.md → tasks.md`.
- El usuario elige **qué US y qué prerrequisitos compartidos** autoriza; el agente decide el **orden técnico** respetando dependencias.
- Advertir antes de implementar una combinación inconsistente de US.
- Si la combinación lo requiere, advertir qué prerrequisitos habilitan a cuáles.
- Marcar una tarea solo con trabajo realizado y verificado; **escribir tests no acredita ejecutarlos**.
- Registrar los pendientes de verificación humana sin darlos por cumplidos.
- EPIC, `BACKLOG` y tarjetas técnicas no son entradas ejecutables. No consumir todas las tareas por defecto.

### Durante la implementación

- Una US pasa a `IN PROGRESS` **cuando su IMPLEMENT comienza**, no al autorizarla.
- La Feature pasa a `IN PROGRESS` cuando comienza la primera US y permanece allí mientras sus US recorran sus gates.
- Puede continuar con otras US autorizadas e independientes mientras una espera gate humano; **evaluar el impacto** del feedback de una US antes de continuar trabajo relacionado que dependa de ella.
- El agente puede crear o modificar tests durante IMPLEMENT; su ejecución corresponde al usuario.
- El frontend está **exento de testing**: no crear tests ni incorporar infraestructura para ellos.

### Ambigüedad funcional durante IMPLEMENT

```text
STOP IMPLEMENT → resolver upstream → actualizar artefactos afectados
→ ANALYZE → recuperar READY → continuar con autorizaciones vigentes
```

Las autorizaciones no afectadas se conservan; las afectadas requieren nueva autorización. No parchear alrededor de una especificación incorrecta.

## Recorrido de una US

```text
READY → IN PROGRESS → CODE REVIEW → TESTING → DONE
```

Cada US recorre sus propios gates humanos. El detalle de condiciones y reports está en [`gates.md`](gates.md).

Cuando una US termina su implementación pasa a `CODE REVIEW` **pendiente de revisión integral**; eso no implica aprobación individual de esa US. Las tareas transversales no mueven por sí solas todas las US a `IN PROGRESS`.

## CONVERGE y cierre de Feature

- Cuando **todas las US requeridas están `DONE`** y el trabajo técnico necesario está completo, la Feature pasa a `TESTING`.
- En ese momento corresponde ejecutar `/speckit.converge`. **No ejecutar CONVERGE antes**: es un prerrequisito del comando.
- CONVERGE es diagnóstico y **append-only**: anexa remediaciones a `tasks.md` y no modifica código, tests ni otros documentos. Detalles en [`gates.md`](gates.md) y [`recovery.md`](recovery.md).
- La corrección documental inequívoca ocurre **fuera del comando**, en operación separada, seguida de nueva ejecución. No repetir gates no afectados.
- CONVERGE exitoso **prepara** el cierre, no lo autoriza: la Feature permanece `TESTING` hasta la autorización final.
- La transición a `DONE` y el estado `Approved` de la spec exigen autorización final humana explícita, vigente y persistida.

## Git

- Nombre de rama según Constitución §7: `<type>/<spec-id>-<scope>` con spec relacionada, `<type>/<scope>` independiente; el directorio de spec no lleva el prefijo `<type>/`.
- Antes de crear una rama, determinar si el cambio pertenece a una spec existente. No asociar artificialmente ni crear spec nueva para un cambio ya asociado.
- Mantener trazabilidad spec ↔ rama ↔ commits ↔ código ↔ Pull Request. No imponer rama por US ni commit por tarea.
- Declara en el commit la SPEC, US o Tasks correspondientes y el alcance compartido, con hashes reales cuando existan.
- No inventar URL ni hash de PR. La revisión del diff local basta para aprobar; aprobar el estado exacto no implica merge ni publicación.
- No hacer commit, branch ni push sin encargo.

## Verificación y entrega

El agente no ejecuta tests. El único control ejecutable permitido es el build **sin tests** del área cuyo código cambió, con los comandos de su `AGENTS.md`. Los cambios solo documentales no requieren build.

| Cambio | Control permitido |
| --- | --- |
| Backend | `./gradlew build -x test` desde `backend` |
| Frontend | `npm run build` desde `frontend` |
| Ambos | Ambos builds sin tests |
| Solo documentación | Coherencia, rutas, enlaces y diff; sin build |

SonarQube, Quality Gates y otros controles opcionales quedan a cargo del usuario y no condicionan la finalización. Un fallo preexistente verificablemente ajeno al cambio se reporta y no bloquea la entrega.

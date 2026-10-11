# Workflow operativo SDD + Trello + Git

## 1. Alcance, autoridad y lenguaje normativo

**MUST** expresa obligación; **MUST NOT**, prohibición; **SHOULD**, recomendación cuya excepción requiere justificación; **MAY**, permiso. Ejecutar únicamente el encargo autorizado. Handoffs, artefactos y estados no autorizan operaciones por sí mismos.

MUST leer AGENTS raíz, Constitution y contexto aplicable de cada área. Constitution tiene máxima jerarquía; arquitectura, convenciones y testing siguen siendo obligatorios. La Spec define intención funcional; plan, diseño, Tasks y checklists la desarrollan. Ante conflicto obligatorio MUST detenerse y aplicar Constitution §9.

Precedencia funcional: Constitution → spec.md → clarificaciones incorporadas → plan/diseño → tasks.md → implementación → Trello. La configuración efectiva gobierna versiones y dependencias. Consultar `tp/enunciado.md` y `tp/entregas.md` al ejecutar fases relevantes; ninguna tarjeta sustituye requisitos externos.

`sdd-orchestration` coordina el workflow subordinado a estas normas; decide fases, gates, READY, autorizaciones, impacto, invalidaciones y transiciones. `trello-traceability` crea, actualiza, mueve, relaciona y persiste resultados; no toma decisiones de gates. Spec Kit realiza sus operaciones reales: no emular comandos faltantes. Si falta o falla un comando necesario, detener y reportar.

Infraestructura efectiva: `.opencode/commands/`, `.specify/scripts/bash/`, `.specify/templates/` y `.specify/workflows/speckit/workflow.yml`. El YAML es asistido, no desatendido; no escribe Trello ni acredita gates por sí mismo. Preservar adaptaciones locales al actualizar Spec Kit.

## 2. Responsabilidades y trazabilidad

EPIC → FEATURE/SPEC → User Story → Task → Code. La EPIC puede agrupar varias Features; no convertirla ni dividirla automáticamente. El usuario aprueba límites funcionales. Cada SPEC tiene carpeta real y tarjeta propia; cada US deriva de `spec.md` y tiene tarjeta independiente desde SPECIFYING cuando se identifica suficientemente.

`[US1]` corresponde a `SPEC-nnn | US01`, con numeración local estable. Tasks `Txxx` viven principalmente en `tasks.md`; tarjetas `TSKnn` son seguimiento excepcional y no numeración equivalente. Trabajo compartido/fundacional MAY no tener etiqueta US, pero MUST indicar a quién habilita y estar autorizado expresamente. CODE REVIEW, TESTING y autorizaciones no son Tasks técnicas.

Requisitos sin SPEC usan `[EPIC] REQ-<id> - <nombre>`; no inventar US/Tasks ni reservar números. Mantener relaciones bidireccionales reales, referencias a fuentes externas y el índice `specs/trello.md`. Git registra implementación observada, no aceptación. Artefactos existentes, checkboxes, merges y builds no acreditan fases.

## 3. Registro explícito, persistencia e idempotencia

El checklist `SDD Lifecycle` conserva operaciones: Specify, Clarify, Plan, Checklists, Tasks, Analyze, Implement, Converge. No sustituye gates humanos. Registrar evidencia de fases en la tarjeta Feature: fecha, responsable, alcance, resultado OK/FAIL/PENDING/INVALIDATED, entradas evaluadas, referencias, blockers y siguiente acción. Marcar fases solo con evidencia vigente; Implement agregado solo cuando todo su alcance requerido está terminado.

Gates se persisten como comentarios estructurados `[SDD GATE]` en la tarjeta correspondiente; no exigir Custom Fields, reescribir descripciones por aprobaciones ni copiar reports completos. Incluir Gate, Result, Validated by: HUMAN para validaciones humanas y User Story o Feature. IMPLEMENT AUTHORIZATION, CODE REVIEW y TESTING se registran en cada US; FEATURE COMPLETION AUTHORIZATION en la Feature. TESTING N/A incluye Justification. Invalidaciones determinadas por el orquestador llevan Result: INVALIDATED y Reason, sin atribuirlas a una aprobación humana ficticia.

Referenciar el alcance/estado evaluado mediante commit, diff local o report disponible. No inventar hashes, resultados ni aprobaciones. Registrar nuevas decisiones e invalidaciones como eventos nuevos, sin borrar evidencia histórica. Cambios de código/tests que afecten el alcance aprobado invalidan gates correspondientes; cambios sin impacto conservan aprobaciones. No resetear indiscriminadamente toda la Feature.

Antes de escribir MUST releer tarjetas, verificar identidad y preservar ID, URL, miembros, comentarios, adjuntos, etiquetas y checklists. Después verificar lectura, unicidad, relaciones e índice. Ante escritura parcial releer antes de reintentar. Fallo mecánico: TRELLO OUT OF SYNC, con entidad, estado esperado/conocido y operación fallida; no fingir éxito. Si falla persistencia obligatoria de cualquiera de los gates o sus invalidaciones, reportar GATE EVIDENCE NOT PERSISTED: no habilitar la transición dependiente hasta resolverlo. Registrar operaciones pendientes conforme a Constitution §8.

## 4. Columnas y lifecycle

Columnas únicas, en orden: **BACKLOG → SPECIFYING → READY → IN PROGRESS → CODE REVIEW → TESTING → DONE**. Comprobar tablero real; cambios estructurales requieren autorización. No usar Review ni crear CONVERGE.

| Columna | Feature | User Story |
| --- | --- | --- |
| BACKLOG | Intención todavía no preparada. | Historia todavía no preparada. |
| SPECIFYING | SPECIFY, CLARIFY, PLAN, CHECKLIST, TASKS o ANALYZE. | Preparación afectada pendiente. |
| READY | Preparación validada; espera inicio autorizado. | Preparada; autorizada o no. |
| IN PROGRESS | Existe al menos una US cuyo IMPLEMENT comenzó y la Feature todavía no cumple las condiciones para TESTING final. | IMPLEMENT realmente iniciado. |
| CODE REVIEW | No corresponde al flujo normal de Feature. | Revisión humana del código completo, incluidos tests. |
| TESTING | CONVERGE final o autorización final de cierre pendiente. | Revisión, ejecución y aprobación humana de tests; o N/A validado. |
| DONE | CONVERGE y autorización final humana satisfechos. | Implementación, CODE REVIEW y TESTING satisfechos. |

CODE REVIEW y TESTING de las US tienen significado propio mientras la Feature permanece IN PROGRESS. No forzar estados iguales ni inferir estado de Feature desde una sola US. Mantener fase SDD detallada aunque la columna agrupe pasos. `Status` de spec sigue `specs/README.md`, no el estado de una US.

SPEC-001–005 conservan DONE histórico sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica workflow vigente por impacto al alcance afectado.

## 5. Inicio y materialización

MUST comprobar si corresponde reutilizar una SPEC. Antes de SPECIFY, si la intención es demasiado ambigua y no hay spec, pre-clarificar lo mínimo; no sustituye CLARIFY. División de EPIC requiere aprobación humana.

Mecanismo real, desde raíz, solo en Specify autorizado:

```bash
.specify/scripts/bash/create-new-feature.sh --json --short-name '<scope>' '<requisito autorizado>'
.specify/scripts/bash/create-new-feature.sh --json --reuse --short-name '<directorio-o-scope-existente>' '<trabajo autorizado>'
```

No ejecutar placeholders. Revisar scripts/hooks antes de usarlos. Reanudación usa siempre `--reuse`; no asignar nueva identidad para continuar una existente. `--number` solo desambigua/valida identidad existente con reuse; `--allow-existing-branch` no sustituye reuse. Leer SPEC_NAME, SPEC_FILE, FEATURE_NUM, BRANCH_NAME, SPEC_ACTION y BRANCH_STATUS; reportar campos faltantes y fallos, no asumir rama disponible. Preservar cambios locales: sin stash/reset automático. Verificar directorio resuelto por SPECIFY_FEATURE_DIRECTORY / `.specify/feature.json` antes de fases; la última spec no determina tarea.

Asociar EPIC REQ a SPEC real preservando antecedentes cuando la correspondencia esté confirmada. No duplicar tarjetas ni convertir ideas en US ficticias.

## 6. Protocolo de preparación y cierre

Leer definición del comando correspondiente y ejecutar solo operación autorizada. Inspeccionar hooks; nunca activar ejecución directa/indirecta de tests. Aplicar registro de §3.

- **SPECIFY:** materializar intención, US, criterios, escenarios, requisitos y límites; evaluar calidad. Artefacto no equivale a fase completada. Ambigüedades pendientes se explicitan para CLARIFY; sincronizar US identificadas durante SPECIFYING.
- **CLARIFY:** siempre obligatorio después de SPECIFY. Resolver decisiones funcionales y registrarlas en spec, no solo chat. Cero preguntas es válido si el escaneo acredita claridad; omitir no equivale a completar.
- **PLAN:** diseño implementable, Constitution Check antes/después, contratos y consumidores. Mantener plan, research, data-model, contracts y quickstart pertinentes, justificando no aplicabilidad. Corregir requisitos upstream, no ocultarlos en diseño.
- **CHECKLIST:** obligatorio; general de calidad más especializados aplicables, selección justificada. Generación no es evaluación; respetar propiedad del revisor, no marcar para avanzar.
- **TASKS:** preservar IDs/avance; cobertura, dependencias, rutas, tareas compartidas y verificabilidad. Incluir creación/modificación de tests backend cuando normas aplicables lo exijan; no tests frontend ni tareas humanas de gates. Tareas completas significan implementación completa, no DONE.
- **ANALYZE:** `/speckit.analyze` estrictamente read-only. Contrastar Constitution, spec/clarificaciones, diseño, Tasks y checklists. Contradicciones normativas/funcionales, alcance no aprobado, cobertura insuficiente o criterio obligatorio no verificable son blockers. Hallazgos editoriales sin impacto material se reportan y no impiden READY.

Si ANALYZE detecta corrección mecánica inequívoca, el orquestador MAY realizar una única corrección **fuera del comando**, en operación separada. Informar exactamente lo corregido y STOP. Nuevo ANALYZE en operación posterior; no loops autónomos analyze/fix.

**READY:** preparación global válida y ANALYZE vigente sin blockers de especificación, planificación o consistencia. Un blocker en cualquier US impide READY global. **Dependency ≠ Blocker:** dependencias técnicas conocidas, identificadas y resolubles mediante orden y autorizaciones no impiden READY. Feature y US válidas pasan READY.

**IMPLEMENT AUTHORIZATION:** listar US, dependencias e independencia; usuario elige QUÉ US/prerrequisitos autoriza, agente decide orden técnico. Advertir combinación inconsistente antes de implementar. Persistir autorización por US. **IMPLEMENT AUTHORIZATION ≠ inicio de IMPLEMENT:** US autorizada permanece READY hasta inicio efectivo. Autorizaciones afectadas por cambios upstream requieren nueva aprobación; no afectadas se conservan.

**CONVERGE:** final de Feature, solo cuando todas las US requeridas están DONE y trabajo técnico necesario completo. Feature pasa TESTING. `/speckit.converge` es diagnóstico/append-only: únicamente anexa remediaciones conforme a su comando; nunca código, tests ni correcciones documentales dentro del comando. Complementar fuera del comando revisión de evidencias, documentación, checklists, Git y trazabilidad.

Faltantes de implementación requerida: Feature y US afectadas vuelven IN PROGRESS según decisión del orquestador; Tasks nuevas/reabiertas no inventan comportamiento. Reimplementar alcance autorizado y repetir CODE REVIEW/TESTING afectados antes de otro CONVERGE. Comportamiento no definido: STOP, decisión humana upstream, artefactos afectados, ANALYZE y READY. Corrección documental mecánica inequívoca MAY ocurrir **fuera de CONVERGE** como operación separada, seguida por nueva ejecución; no repetir gates no afectados.

**FEATURE COMPLETION AUTHORIZATION:** CONVERGE exitoso prepara el cierre, no lo autoriza. Feature permanece TESTING; presentar resultado final y pedir autorización humana inequívoca referida a la Feature y resultado evaluado. Persistir comentario en Feature y verificarlo antes de `Status: Approved` y Feature DONE. Una modificación con impacto en la base del cierre invalida autorización afectada; reevaluar/repetir pasos necesarios y obtener nueva autorización. No aceptar ok/dale/seguí como cierre.

## 7. Ejecución de una User Story

1. Identificar SPEC/US reales, carpeta, rama, READY vigente y alcance autorizado. EPIC, Backlog y TSK no son entradas ejecutables.
2. Leer normas, fuentes externas, artefactos, implementación/consumidores/tests/configuración afectados. Revisar git status/diff, preservar trabajo previo.
3. Registrar toma y mover READY → IN PROGRESS solo cuando IMPLEMENT comienza; actualizar Feature por estado agregado. Si evidencia de toma no puede persistirse, detener inicio hasta resolver.
4. Implementar únicamente Tasks/prerrequisitos autorizados en orden. `[P]` no autoriza delegación ni solapar archivos. Continuar otras US autorizadas e independientes mientras esperan gates humanos; evaluar impacto de feedback antes de continuar trabajo relacionado.
5. Actualizar tasks.md solo por trabajo realizado/verificado; escribir tests no acredita ejecutarlos. Separar pendientes humanos. No cambiar requisitos ni checklists del revisor para justificar desviaciones.
6. Ambigüedad funcional: STOP IMPLEMENT, resolver upstream, invalidar por impacto, actualizar artefactos afectados, ANALYZE → READY → autorizaciones vigentes/nuevas según impacto.
7. Implementación completa → CODE REVIEW con **Code Review Report completo**, conforme a sdd-orchestration: resumen/funcionamiento, todos los archivos creados/modificados incluidos tests, responsabilidad/cambios/razones, decisiones, referencias concretas, orden de revisión y checklist humano. Usuario aprueba explícitamente el checklist completo; agente no aprueba. Rechazo → corrección en IMPLEMENT → nuevo report/review obligatorio.
8. CODE REVIEW aprobado y persistido → TESTING con **Testing Report completo** conforme a sdd-orchestration: tests/cambios, cobertura US/criterios/casos, mocks/assertions, riesgos/falsos positivos, revisión recomendada y comandos exactos para usuario; PENDING HUMAN VALIDATION. Usuario revisa tests/cobertura, ejecuta y confirma resultado satisfactorio. No exigir output completo si pasan; solicitar salida relevante si fallan.
9. Tests automatizados razonablemente no aplicables: justificar N/A y solicitar validación humana explícita; no tests artificiales ni testing manual genérico no definido. N/A no es ausencia de implementación ni gate omitido.
10. Fallo que exige código/tests → IN PROGRESS → CODE REVIEW → TESTING. Bloqueo externo sin cambios → permanecer TESTING, Status BLOCKED y Reason concreta; dependencia pendiente → BLOCKED BY DEPENDENCY. No crear estados por cada causa. Dependencia resuelta sin impacto de código no obliga a repetir review.
11. TESTING aprobado/N/A validado, vigente y persistido → US DONE. Review/tests que afecten alcance aprobado invalidan gates correspondientes independientemente de autor del cambio; cambios sin impacto no invalidan. Nunca DONE por tasks/build/merge ni aceptación ambigua.

## 8. Verificación y entrega

Constitution §6 y AGENTS gobiernan: agente no ejecuta tests ni controles adicionales, directa/indirectamente. Backend: crear/modificar tests necesarios conforme a normas de testing, usuario los ejecuta. Frontend exento: no crear/ejecutar tests ni infraestructura.

| Cambio de código | Control permitido |
| --- | --- |
| Backend | `./gradlew build -x test` desde backend, según AGENTS del área. |
| Frontend | `npm run build` desde frontend, según AGENTS del área. |
| Ambos | Ambos builds sin tests. |
| Solo documentación | Coherencia/rutas/enlaces/diff; sin build. |

Revisar diff final, git diff --check y git status --short. Distinguir fallos nuevos, preexistentes demostrablemente ajenos y entorno. Fallo nuevo de build bloquea CODE REVIEW; fallo preexistente ajeno puede permitir entrega conforme a Constitution, reportando evidencia. Entorno sin clasificación deja validación pendiente. Build permite entrega técnica, no aprueba TESTING: tests aplicables pendientes impiden US/Feature DONE. Sonar y controles opcionales del usuario no son gates obligatorios.

CI/hooks existentes no acreditan ejecución. Inspeccionar antes de commit/push; no disparar ni deshabilitar controles prohibidos. Si operación dispara tests/controles no permitidos, entregar cambios al usuario para ejecutarla. No afirmar publicación/pruebas no observadas.

## 9. Git y evidencia de implementación

Mantener SPEC ↔ US ↔ Tasks ↔ rama ↔ commits ↔ código ↔ PR. No imponer rama por US/commit por Task. Constitución §7: `<type>/<spec-id>-<scope>` para spec relacionada; `<type>/<scope>` independiente; directorio sin prefijo type. Reutilizar identidad, no renumerar spec ni reset/stash cambios ajenos.

Commits según hook efectivo: type(scope): description o type: description, en inglés. Declarar SPEC/US/Tasks y alcance compartido con hashes reales cuando existan. PR referencia spec/rama, evidencia y pendientes; no inventar URL/hash. Diff local revisable basta para review; aprobación del estado exacto no implica merge/publicación. No commit/branch/push sin encargo.

## 10. Failure / Recovery y cierre

Inconsistencia que impide determinar comportamiento correcto: STOP transición afectada, reportar fuente/evidencia/impacto y decisión necesaria. Conservar trabajo/historial; reportar no autoriza corrección fuera del encargo. Falta de Spec/US, rama incompatible, ANALYZE bloqueante o artefactos insuficientes impiden IMPLEMENT. Trello contradictorio no redefine requisitos. Trabajo parcial se conserva; no marcar validación inexistente.

Definition of DONE de US: implementación completa + CODE REVIEW humano vigente + TESTING humano aprobado o N/A validado + evidencia obligatoria persistida.

Definition of DONE de Feature: trabajo técnico necesario completo + todas las US requeridas DONE + CONVERGE final exitoso vigente + FEATURE COMPLETION AUTHORIZATION humana explícita vigente + evidencia persistida. No tasks.md complete = Feature DONE.

Informar estado estructurado conforme a sdd-orchestration: Feature, fase SDD, columna, READY, US, autorizaciones, gates, blockers, sync y siguiente transición; incluir reports al entrar a sus gates.

Esta enmienda no ejecuta lifecycle, modifica Trello ni acredita fases históricas. H1–H4 y conflicto de tests de tasks.md de SPEC-007 permanecen fuera de alcance.

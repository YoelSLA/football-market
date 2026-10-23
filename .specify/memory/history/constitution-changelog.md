# Historial de enmiendas de la Constitución

Registro íntegro y cronológico de los **Sync Impact Reports** de la Constitución de Football
Market. Cada informe documenta la versión anterior y la nueva, las secciones añadidas,
modificadas y eliminadas, las reglas retiradas y adoptadas, las garantías preservadas, las
dependencias afectadas y la autorización humana de la enmienda.

La Constitución vigente está en [../constitution.md](../constitution.md). Este archivo es su
historial documental: no forma parte de las reglas vigentes y no se aplica como norma.

El contenido se conserva **íntegro y sin reescribir**, tal como fue registrado en cada
momento. Las referencias a documentos, scripts o rutas que han cambiado desde entonces
conservan su valor como registro de aquel estado.

Los informes se registran aquí desde la enmienda **9.0.0**. Los seis informes anteriores se
trasladaron desde `constitution.md` sin alteración de su texto, de su orden ni de su
cronología, conservados en el mismo orden inverso al de las fechas.

---
<!--
Sync Impact Report — Enmienda 2026-10-18 (segunda, misma fecha) — continuación
Versión: se mantiene 9.0.0. Esta entrada amplía el alcance de la enmienda 9.0.0, que aún no fue
integrada y cuyo número no se incrementa por esta vía porque no modifica el texto de la
Constitución. Alcance: retirada definitiva del tablero Trello como sistema de seguimiento
operativo, y registro del estado pendiente de GitHub Projects. No elimina ni debilita ninguna
garantía.
Autorización: el usuario retiró explícitamente la condición que obligaba a mantener Trello
activo hasta configurar GitHub Projects y migrar las funcionalidades. La decisión sustituye a
esa condición previa.
Cambios normativos en el archivo de la Constitución: ninguno. La Constitución de 9.0.0, escrita
la misma fecha con la estructura del template oficial, ya no contenía ninguna mención a Trello ni
ninguna condición transitoria. La presente enmienda por lo tanto opera sobre documentos subordinados
y no modifica ni el texto ni las garantías de la Constitución.
Reglas retiradas, en documentos subordinados:
- .opencode/skills/trello-traceability/ eliminada por completo (SKILL.md y sus cinco
  referencias: cards, card-format, consistency, gates, sync).
- references/trello-bridge.md eliminada en la fase anterior.
- opencode.json: retirado el servidor MCP de Trello. Ese servidor era el único declarado, porque el bloque mcp queda vacío y se elimina. No se modificó ninguna otra configuración.
- Condición transitoria «Trello y su conector continúan operativos, condicionada a la
  configuración de GitHub Projects y a la migración de las funcionalidades activas».
- Obligación de crear tarjetas Trello, sincronizar sus columnas, crear una tarjeta por User
  Story, registrar tareas técnicas como tarjetas y persistir comentarios con formato
  [SDD GATE] en los comandos de Spec Kit.
Reglas adoptadas o confirmadas:
- El seguimiento operativo es GitHub Issues con GitHub Projects, con una Issue por
  funcionalidad o unidad de trabajo independiente.
- Las User Stories permanecen en spec.md. Las tareas técnicas permanecen exclusivamente en
  tasks.md y no se convierten en Issues ni duplican su estado.
- Estados de seguimiento: Backlog, In Progress, In Review, Done.
- La configuración del tablero de Projects y la migración de funcionalidades NO están hechas.
  Se registran como pendientes. No se declara ninguna funcionalidad migrada sin verificación.
- La evidencia histórica conserva su validez y puede acreditar autorizaciones vigentes; no se
  exige reemitirla ni duplicarla.
Documentos actualizados en este trabajo:
- opencode.json
- .opencode/commands/speckit.specify.md, speckit.implement.md, speckit.converge.md,
  speckit.tasks.md, speckit.analyze.md
- .opencode/commands/speckit.taskstoissues.md: ELIMINADO. Creaba una Issue por tarea técnica, lo
  que contradice directamente el modelo vigente. El usuario decidió eliminarlo por completo,
  sin conservar el contenido original y sin crear un comando sustituto. Ninguna skill, comando
  ni documento depende de ese archivo.
- .specify/integrations/opencode.manifest.json: NO se editó. Es un registro de hashes gestionado
  por Spec Kit y ya está desactualizado para todos los comandos modificados en este trabajo; se
  regenerará en la próxima actualización de la herramienta.
- .opencode/skills/sdd-orchestration/SKILL.md y references/gates.md y workflow.md
- docs/development/sdd-workflow.md: nueva sección «Estado del sistema de seguimiento» con el
  estado pendiente, y retirada de la condición transitoria de Trello.
- specs/trello.md: encabezado de archivo histórico congelado. Su contenido no se modificó.
Historial preservado, sin reescritura: specs/trello.md, los Sync Impact Reports completos, los
Code Review Reports, las evidencias de testing y los registros de las especificaciones históricas.
Restricciones respetadas: sin cambios en código funcional, sin modificar SPEC-009 ni SPEC-010,
sin operaciones sobre servicios externos de Trello, sin crear Issues ni Projects, sin tests ni
builds, sin operaciones Git de escritura, sin merge ni rebase.
Pendientes declarados: configurar el tablero de GitHub Projects; migrar las funcionalidades en
curso; configurar el tablero de GitHub Projects y migrar las funcionalidades en curso;
docs/development/GUIA-HUMANA-SDD.md, que sigue describiendo el tablero retirado.
-->


<!--
Sync Impact Report — Enmienda 2026-10-18
Versión: 8.1.0 → 9.0.0 (MAJOR: se elimina y redefine el principio §8 de trazabilidad y
sincronización con Trello, sustituyéndolo por seguimiento operativo en GitHub Issues y GitHub
Projects).
Sección reemplazada: §8 Trazabilidad y sincronización con Trello → §8 Seguimiento operativo y
trazabilidad, con ocho subsecciones: §8.1 unidad de seguimiento, §8.2 identificadores, §8.3 alcance
de cada fuente, §8.4 estados de seguimiento, §8.5 autorizaciones humanas, §8.6 registro de la
autorización, §8.7 invalidación por impacto y §8.8 vigencia y disposición transitoria.
Secciones modificadas: §1, únicamente las dos menciones por las que Trello aparecía como fuente de
precedencia o como medio de alterar requisitos académicos; §6, únicamente la enumeración de
sistemas externos cuya modificación queda prohibida dentro de la excepción acotada de
infraestructura de desarrollo. Ninguna regla de seguridad, verificación, entrega o prohibición de
ejecución de tests cambia. §2, §2.1, §2.2, §3, §4, §5, §7 y §9 quedan sin cambios.
Secciones eliminadas: ninguna. Ninguna sección renumerada.
Reglas retiradas por esta enmienda, todas dentro de §8: la trazabilidad obligatoria
`EPIC → SPEC → User Story → Task → Code` como cadena de seguimiento; la obligación de una tarjeta
propia por cada User Story formal; la obligación de las siete columnas; el gate READY como
formalidad con sincronización obligatoria de tarjetas; el registro de fase mediante checklist
`SDD Lifecycle`; el checklist `Implementation Tasks` que replica `tasks.md` en el tablero; el
formato obligatorio de evidencia `[SDD GATE]`; el índice bidireccional manual `specs/trello.md`; y
las claves canónicas `[EPIC] SPEC-<id>`, `SPEC-<id> | US<nn>` y `SPEC-<id> | TSK<nn>` como
convención vigente.
Reglas adoptadas: una Issue por funcionalidad o unidad de trabajo independiente, con las User
Stories en `spec.md` y las tareas técnicas exclusivamente en `tasks.md`; cuatro estados operativos
`Backlog → In Progress → In Review → Done`; cuatro autorizaciones humanas explícitas y
verificables, consolidadas por alcance explícito, en lugar de gates repetidos por historia;
registro de la autorización sin formato canónico obligatorio ni sistema prescrito, reutilizando la
evidencia existente cuando sea suficiente; y separación entre el estado SDD, que corresponde a los
artefactos del repositorio, y el estado operativo, que corresponde al sistema de seguimiento.
Decisión de gobernanza sobre precedencia: NO se establece precedencia universal entre los
artefactos SDD y el sistema de seguimiento. Cada fuente gobierna únicamente su ámbito y una
discrepancia entre ambas es una inconsistencia que se corrige en el origen que la evidencia
contradiga, no un conflicto que se resuelva aplicando una fuente sobre la otra.
Garantías preservadas sin cambio: el agente no ejecuta tests, directa ni indirectamente; la
actualización en el mismo cambio de Spec, implementación, tests, DTO, documentación, OpenAPI y
configuración; la obligatoriedad de autorización humana previa a implementar; la prohibición de
aprobar, rechazar o dar por válido un gate en nombre del usuario; la prohibición de interpretar
`ok`, `dale` o `seguí` como aprobación; la invalidación de autorizaciones por impacto sobre el
alcance evaluado sin resetear la funcionalidad; la prohibición de habilitar cierre por tareas
completas, build, commit, Pull Request o CONVERGE; y las condiciones de entrega de §6.
Disposición transitoria: el sistema de seguimiento ya operativo continúa vigente y su evidencia
conserva su validez y puede reutilizarse para acreditar autorizaciones. specs/trello.md queda
congelado como archivo histórico, sin mantenimiento. `trello-traceability` y el conector MCP de
Trello NO se desactivan por esta enmienda; su retirada queda condicionada a que GitHub Projects
esté configurado y las funcionalidades activas migradas. SPEC-009 y SPEC-010 no se modifican ni
migran en esta enmienda y conservan su estado real.
Ratificación: se conserva la fecha de ratificación de la norma base, 2026-08-31.
Autorización: solicitud explícita del usuario el 2026-10-18, tras auditoría del workflow
existente y revisión del diseño de simplificación.
Base de aplicación y advertencia de integración: la presente enmienda se aplicó sobre 8.1.0. El
contenido de §2.2 fue portado desde el commit c327a08 porque la rama de trabajo parte de dev,
donde la enmienda 8.1.0 aún no está integrada. Al integrar posteriormente el trabajo de SPEC-011,
que también contiene la enmienda 8.1.0, el encabezado de este archivo requiere una resolución
explícita del conflicto entre las dos enmiendas, sin mezclar ni reescribir el historial de Sync
Impact Reports.
Dependencias y sincronización subordinada: conforme al Scope Guard de speckit.constitution, esta
enmienda escribe la Constitución y el workflow operativo SDD, su único documento normativo
subordinado inmediato. Quedan pendientes de sincronización en fases posteriores, sin aplicar en
este trabajo:
- .opencode/skills/sdd-orchestration/SKILL.md y references/workflow.md, gates.md, recovery.md;
  eliminación de references/trello-bridge.md. ESTA SINCRONIZACIÓN SE REALIZÓ el 2026-10-18:
  los cuatro archivos se adaptaron al modelo de §8 y trello-bridge.md se eliminó tras verificar
  que sus nueve responsabilidades quedaron absorbidas —separación decidir/persistir, tablero
  operativo no documental, no duplicar tareas en el seguimiento, fallo de persistencia de
  evidencia, reutilización de evidencia previa, no declarar sincronización no observada, la
  evidencia registrada no autoriza ni invalida un gate, secuencia de cierre y estados canónicos—
  sin dejar referencias colgantes. SKILL.md corregía además la referencia obsoleta a
  «Constitution 6.0.0».
- Desactivación y posterior retirada de .opencode/skills/trello-traceability/, NO iniciada:
  condicionada a la configuración de GitHub Projects y a la migración de las funcionalidades
  activas. Su contenido se mantiene sin modificar durante esta fase.
- .opencode/commands/speckit.specify.md, speckit.implement.md, speckit.converge.md y
  speckit.taskstoissues.md.
- docs/development/GUIA-HUMANA-SDD.md.
- AGENTS.md, sección de contexto mínimo.
La Constitución es la fuente de verdad; ante contradicción aplicar §1 y §9, sin asumir que esta
sincronización ya ocurrió.
Sin cambios en scripts, plantillas versionadas, código funcional, specs funcionales, Trello,
GitHub, ramas, remotos ni estados SDD. Sin gates humanos aprobados por esta enmienda y sin
operaciones Git de escritura.
-->

---

<!--
Sync Impact Report — Enmienda 2026-10-15
Versión: 8.0.0 → 8.1.0 (MINOR: nuevo principio §2.2 sobre proporcionalidad del alcance y
gestión de casos límite; amplía §2 sin modificar ni eliminar ninguna regla preexistente).
Sección añadida: §2.2 Proporcionalidad del alcance y gestión de casos límite, con las reglas de
alcance mínimo suficiente, clasificación OBLIGATORIO/RECOMENDADO/OPCIONAL, evaluación de
aplicabilidad, documentación de lo no obligatorio, criterios de bloqueo, autoridad del equipo,
aplicación a CHECKLIST y TASKS y simplicidad.
Principios modificados: §2 Calidad y alcance; única modificación una remisión a §2.2 en su
primera regla, sin alterar su contenido normativo. Ninguna sección renumerada.
Secciones eliminadas: ninguna.
Regla adoptada: las SPEC se dimensionan al alcance del enunciado del TP y de las entregas. Los casos
límite se clasifican antes de convertirse en requisitos; solo los OBLIGATORIOS bloquean el avance
a Tasks y las mejoras RECOMENDADAS u OPCIONALES se documentan como observaciones, riesgos
conocidos o mejoras futuras, sin entrar por sí solas en requisitos funcionales, criterios de
aceptación ni tareas obligatorias. Los agentes recomiendan soluciones y no amplían unilateralmente
el alcance aprobado.
Compatible con §2 (simplicidad y anti-sobreingeniería), §2.1, §3 (las garantías de seguridad,
integridad y consistencia se preservan y no se reducen por proporcionalidad), §4 (no altera la
exigencia de actualizar en el mismo cambio Spec, implementación, tests, contratos y documentación),
§6 (no habilita controles ejecutables, no reduce la prohibición de tests y los tests aplicables
pendientes siguen impidiendo el cierre de una funcionalidad), §7 (sin cambios), §8 (los blockers
continúan exigiendo requisitos obligatorios, contradicciones relevantes o riesgos críticos, conforme
al workflow SDD; las dependencias resolubles no son blockers) y §9 (no habilita reinterpretar la
norma; toda ampliación de alcance requiere decisión humana explícita).
Ratificación: se conserva la fecha de ratificación de la norma base, 2026-08-31. El contenido de
§2.2 fue ratificado explícitamente por el usuario el 2026-10-15.
Autorización: solicitud explícita del usuario el 2026-10-15.
Nota de portabilidad: este contenido se incorporate a la rama refactor/simplify-sdd-workflow el
2026-10-18 para evitar que la presente enmienda MAJOR 9.0.0 se aplicara sobre una base 8.0.0. La
fuente es el commit c327a08; no huborite decisiones nuevas ni merge ni reescritura de historia.
Sin cambios en scripts, plantillas versionadas, código, specs funcionales, Trello, ramas ni
estados SDD. Ninguna SPEC se modifica ni se revisa en esta operación.
-->

---

<!--
Sync Impact Report — Enmienda 2026-10-14
Versión: 7.1.0 → 8.0.0 (MAJOR: redefine la exclusividad de controles ejecutables de §6
con una excepción acotada para infraestructura de desarrollo).
Principio modificado: §6 Verificación; nueva subsección de validaciones controladas de
integración de agentes, captura de evidencias y registro de conversaciones de OpenCode.
Autorización: solicitud humana explícita de implementar esta enmienda el 2026-10-14.
Dependencias sincronizadas: docs/development/sdd-workflow.md §8 y AGENTS.md,
sección Verificación y entrega, únicamente sus reglas de controles ejecutables.
Se conservan las prohibiciones de tests de aplicación, las condiciones de builds,
los gates humanos, la ratificación original y el historial de enmiendas.
La excepción exige autorización por ejecución y alcance, protección de datos,
registro de resultados observados, reversibilidad y limpieza limitada.
Esta enmienda no ejecuta la Etapa A ni validaciones, crea plugins, modifica el extractor,
configuración, código funcional, SPEC-005, SPEC-008, tareas, Trello ni estados SDD;
no realiza operaciones Git de escritura ni aprueba gates.
-->

---

<!--
Sync Impact Report — Enmienda 2026-10-12 (segunda, misma fecha)
Versión: 7.0.0 → 7.1.0 (MINOR: reorganization temporal compatible de la obligación de tarjetas US en §8;
la obligación se conserva íntegra y pasa de SPECIFYING al cierre de READY, con requisitos de sincronización nuevos).
Principios modificados: §8 Unidad de seguimiento y numeración (momento de alta de las tarjetas US), §8
Sincronización obligatoria (punto 1), §8 bloque READY y punto 6 de verificación.
Regla adoptada: las US se identifican y documentan durante SPECIFYING, pero sus tarjetas individuales se
crean o reutilizan cuando la épica alcanza READY, después de aprobar el gate humano de Analyze. Esa
sincronización forma parte del cierre de READY. Ninguna US puede comenzar IMPLEMENT sin tarjeta creada,
vinculada y sincronizada.
Compatibilidad: no elimina la obligación de una tarjeta propia por US ni altera sus claves canónicas,
la numeración local, las relaciones obligatorias ni la trazabilidad SPEC → US → Tasks. Las tareas Txxx
siguen sin convertirse en tarjetas. ANALYZE permanece estrictamente read-only. IMPLEMENT AUTHORIZATION
sigue siendo gate humano independiente. Las tarjetas US preexistentes de cualquier SPEC se conservan y
se consideran válidas; la regla se aplica a nuevas SPEC y a sincronizaciones pendientes, sin revertir
estados válidos ni alterar trabajo en curso.
Secciones añadidas o eliminadas: ninguna.
Autorización: solicitud explícita del responsable del proyecto el 2026-10-12.
Dependencias y documentos sincronizados en el mismo trabajo: docs/development/sdd-workflow.md §1, §2, §3,
§4, §5 y §6; .opencode/skills/trello-traceability/references/cards.md (User Story) y SKILL.md;
.opencode/skills/trello-traceability/references/sync.md; .opencode/skills/sdd-orchestration/references/
workflow.md y gates.md.
Sin cambios en scripts, plantillas, código, specs funcionales, Trello ni ramas. SPEC-008 y SPEC-009 no
modificadas; las tarjetas US de SPEC-009 siguen sin crearse y su sincronización queda pendiente para una
operación posterior y autorizada. Sin gates humanos aprobados por esta enmienda.
-->

---

<!--
Sync Impact Report — Enmienda 2026-10-12
Versión: 6.1.0 → 7.0.0 (MAJOR: redefine la prohibición absoluta de asignación manual en §8).
Principios modificados: §8, Unidad de seguimiento y numeración; excepción documentada para
distribución previa de funcionalidades entre integrantes que trabajan en paralelo.
Secciones añadidas o eliminadas: ninguna. Se preservan las demás reglas y la ratificación original.
Autorización: solicitud explícita del usuario de actualizar esta norma el 2026-10-12.
Impacto: numeración automática predeterminada; --number permitido excepcionalmente con registro,
verificación de colisiones y trazabilidad, sin acreditar existencia, integración ni gates de SPEC.
Pendientes de sincronización documental (no modificados por el alcance de /speckit.constitution):
- docs/development/sdd-workflow.md §2 y §5: prohibición absoluta de reservas y --number solo con reuse.
- .opencode/commands/speckit.specify.md: mecanismo de creación y prohibición absoluta de reservas.
- .opencode/skills/sdd-orchestration/references/workflow.md: límites de --number y reservas.
- .opencode/skills/trello-traceability/references/cards.md: prohibición absoluta de reservas.
Estos textos requieren actualización subordinada a §8; ante contradicción, aplicar §1 y §9,
sin ejecutar una creación incompatible ni asumir que la sincronización ya ocurrió.
AGENTS.md describe --number con reuse sin prohibir otros usos; no requiere cambio obligatorio.
El script existente admite --number y puede corregirlo ante colisión: verificar siempre su salida.
Sin cambios en scripts, plantillas, código, specs funcionales, Trello ni ramas; SPEC-009 no creada.
Antecedente de la enmienda anterior conservado a continuación.
-->

---

<!--
Sync Impact Report — Enmienda 2026-10-11
Versión: 6.0.0 → 6.1.0 (MINOR: principio nuevo §2.1 sobre gestión de datos en desarrollo).
Principios modificados: §2 incorpora la subsección 2.1; sin modificar ni eliminar reglas preexistentes.
Dependencias actualizadas: ninguna. Los documentos normativos de §1 no requieren cambios: la
regla se aplica a la base de datos de desarrollo y no altera arquitectura, contratos ni stack.
Compatible con §2 (simplicidad y anti-sobreingeniería), §3, §4 (compatibilidad de contratos),
§6 (restricciones de ejecución), §8 (autorizaciones humanas) y §9 (gobernanza).
Ratificación: se conserva la fecha de ratificación de la norma base, 2026-08-31. La enmienda 6.1.0
y el contenido de §2.1 fueron ratificados explícitamente por el usuario el 2026-10-11. La
procedencia editorial previa queda como no determinada y no se atribuye autoría. Esta ratificación
no modifica §6, §8 ni §9 ni autoriza por sí sola operaciones destructivas.
Sin cambios en Trello, código, specs funcionales ni docs/.
-->

---

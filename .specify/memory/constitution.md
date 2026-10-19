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
pendientes siguen impidiendo US o Feature DONE), §7 (sin cambios), §8 (los blockers continúan
exigiendo requisitos obligatorios, contradicciones relevantes o riesgos críticos, conforme al
workflow SDD; las dependencias resolubles no son blockers) y §9 (no habilita reinterpretar la
norma; toda ampliación de alcance requiere decisión humana explícita).
Ratificación: se conserva la fecha de ratificación de la norma base, 2026-08-31. El contenido de
§2.2 fue ratificado explícitamente por el usuario el 2026-10-15.
Autorización: solicitud explícita del usuario el 2026-10-15.
Dependencias y sincronización: conforme al Scope Guard de speckit.constitution, esta enmienda
escribe únicamente la Constitución. Quedan pendientes de sincronización documental subordinada a
§2.2, sin aplicar en este trabajo:
- docs/development/sdd-workflow.md §1 (vocabulario normativo) y §6 (SPECIFY, CLARIFY, PLAN,
  CHECKLIST, TASKS, ANALYZE y bloqueos de READY).
- .opencode/commands/speckit.specify.md, speckit.clarify.md, speckit.plan.md,
  speckit.checklist.md y speckit.tasks.md.
- .opencode/skills/sdd-orchestration/references/workflow.md y gates.md.
La Constitución es la fuente de verdad; ante contradicción aplicar §1 y §9, sin asumir que esta
sincronización ya ocurrió.
Sin cambios en scripts, plantillas versionadas, código, specs funcionales, Trello, ramas ni
estados SDD. Ninguna SPEC se modifica ni se revisa en esta etapa: SPEC-010 conserva su estado, su
alcance y sus decisiones pendientes en el equipo. Sin gates humanos aprobados por esta enmienda y
sin operaciones Git de escritura.
-->
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
<!--
Sync Impact Report — Enmienda 2026-10-16
Versión: 8.0.0 → 8.1.0 (MINOR: alternativa local acotada de persistencia en §8).
Autorización humana: confirmación explícita del usuario el 2026-10-16 para modificar
Constitution y workflow y permitir READY y IMPLEMENT AUTHORIZATION locales cuando
Trello no esté disponible, con sincronización posterior obligatoria.
Documentos sincronizados: docs/development/sdd-workflow.md y AGENTS.md.
La excepción tiene precedencia sobre las exigencias de tarjeta y persistencia remota
únicamente para READY e inicio de IMPLEMENT; no elimina la evaluación de preparación,
autoriza implementación por sí misma ni altera CODE REVIEW, TESTING o cierre de Feature.
Esta operación normativa no declara READY, no persiste autorizaciones de US ni inicia
IMPLEMENT; no modifica artefactos funcionales, tarjetas, código o tareas.
-->
# Constitución de Football Market

## 1. Documentos normativos

Esta constitución contiene las reglas globales. [Arquitectura común](../../docs/architecture.md), [arquitectura del backend](../../docs/backend/architecture.md) y [arquitectura del frontend](../../docs/frontend/architecture.md) definen roles y dependencias; las [convenciones del backend](../../docs/backend/conventions.md) y las [convenciones del frontend](../../docs/frontend/conventions.md) definen sus reglas de implementación; [tecnologías del backend](../../docs/backend/technologies.md) y [tecnologías del frontend](../../docs/frontend/technologies.md) documentan el stack y su propósito; [testing](../../docs/backend/testing.md) define las pruebas del backend. Todos tienen la misma obligatoriedad cuando resulten aplicables. La configuración efectiva del proyecto conserva precedencia como fuente de verdad de dependencias y versiones.

Consultar solo las secciones pertinentes y sus reglas generales, siguiendo las guías de lectura de cada documento. Las instrucciones operativas están en [AGENTS.md](../../AGENTS.md). Specs y Plans no deben repetir restricciones globales salvo consideraciones específicas de la feature.

Constitution tiene máxima jerarquía normativa. Los documentos normativos de área mantienen su obligatoriedad aplicable; el [workflow SDD](../../docs/development/sdd-workflow.md) detalla el protocolo subordinado a esas normas, y AGENTS y comandos lo ejecutan dentro del alcance autorizado. La Spec define verdad funcional de la feature; plan, tareas y checklists la desarrollan sin contradecirla. Trello registra estado operativo y Git implementación observada, sin autoridad para redefinir intención. La configuración efectiva sigue siendo la fuente de verdad de versiones y dependencias. Ante conflicto entre fuentes obligatorias, detener la transición y aplicar §9, no inventar una excepción.

[Enunciado del TP](../../tp/enunciado.md) y [entregas del TP](../../tp/entregas.md) son fuentes complementarias de requisitos académicos externos: el primero define el alcance general y el segundo agrega condiciones por entrega y sus actualizaciones docentes. Deben consultarse al especificar, aclarar, planificar, generar tareas, implementar y revisar cumplimiento o entregas. Un requisito no se descarta por aparecer solo en uno de ellos. Las decisiones técnicas, Specs o tarjetas de Trello no pueden modificar esos requisitos para justificar una desviación; las contradicciones se señalan antes de proponer cambios.

## 2. Calidad y alcance

- Preferir la solución válida más simple, con responsabilidades coherentes y detalles encapsulados mediante contratos. No crear capas, interfaces, dependencias o abstracciones por costumbre o necesidades hipotéticas; una duplicación no exige por sí sola abstraer. La proporción entre alcance y complejidad, y el tratamiento de casos límite, se rigen por [§2.2](#22-proporcionalidad-del-alcance-y-casos-límite).
- No agregar dependencias si las capacidades existentes bastan. Las tecnologías nuevas requieren justificación técnica y aprobación; las versiones administradas por Spring Boot no se sobrescriben sin necesidad justificada.
- Limitar los cambios a la tarea y a la coherencia de sus elementos afectados. Refactorizar solo con beneficio concreto, preservando comportamiento ajeno; revisar consumidores antes de eliminar o renombrar elementos.
- Eliminar código muerto dentro del alcance. Mantener comentarios útiles y actualizados, sin repetir lo evidente.
- Reportar violaciones o fallos preexistentes ajenos al cambio sin ocultarlos ni ampliar el alcance para corregirlos.

### 2.1. Gestión de datos en desarrollo

Mientras el proyecto no esté en producción, los datos de desarrollo son descartables y su conservación no es un requisito. Aplicar estos principios únicamente a la base de datos y al estado de desarrollo; no constituyen una excepción a §3, §4, §6 ni a las demás secciones.

- Preferir el esquema y la representación de datos más simples que satisfagan los requisitos actuales. Se permite eliminar, recrear o reinicializar datos, tablas y estructuras de base de datos cuando eso simplifique la implementación.
- No implementar migraciones, mecanismos de compatibilidad retroactiva, columnas o rutas de escritura preservadoras ni capas de traducción cuyo único propósito sea conservar datos o estructuras de desarrollo existentes.
- No anticipar requisitos de producción no definidos. Un diseño correcto y coherente prevalece sobre la conservación de estructuras o registros preexistentes.
- Esta permisividad se limita a los datos. No habilita alterar contratos, eliminar operaciones o respuestas documentadas ni evadir las reglas de compatibilidad de §4.
- Toda operación destructiva —incluidas las que afecten la base de datos— respeta las restricciones de ejecución de §6 y las autorizaciones humanas exigidas por §8, y no se ejecuta por iniciativa propia del agente conforme a §9.

Esta sección se revisa cuando el proyecto avance hacia producción y deje de ser aplicable sin una enmienda posterior.

### 2.2. Proporcionalidad del alcance y casos límite

Esta sección gobierna el alcance de las SPEC, no la calidad de su implementación. Obliga a no ampliar lo aprobado y a no bloquear por mejoras no obligatorias; no habilita reducir la cobertura de un requisito obligatorio ni degradar las garantías esenciales de §3, §4 y §6.

- **Alcance mínimo suficiente.** Cada SPEC se concentra en implementar correctamente la funcionalidad solicitada. No agregar requisitos por constituir buenas prácticas generales ni porque un escenario sea técnicamente posible.
- **Clasificación.** Todo caso límite identificado se clasifica antes de incorporarse a la SPEC como OBLIGATORIO —exigido por el [enunciado del TP](../../tp/enunciado.md) o las entregas, aprobado explícitamente por el equipo, o necesario para preservar una garantía esencial—, RECOMENDADO —mejora razonable de robustez, mantenibilidad o experiencia de uso, sujeta a evaluación— u OPCIONAL —escenario excepcional, hipotético o de baja relevancia práctica cuya implementación no resulta indispensable—. Un caso sin clasificar no es requisito.
- **Evaluación de aplicabilidad.** Antes de elevar un caso límite a OBLIGATORIO, evaluar si puede ocurrir en los flujos reales de la aplicación, si puede reproducirse o verificarse razonablemente, su probabilidad e impacto, su relación con los requisitos originales, la complejidad de implementarlo y si existe una solución más sencilla que lo resuelva. La dificultad de reproducción no justifica descartar ni posponer riesgos críticos de seguridad o integridad.
- **Documentación de lo no obligatorio.** Los casos RECOMENDADO y OPCIONAL se registran, cuando aportan valor, como observaciones, riesgos conocidos o mejoras futuras en los artefactos de la SPEC. No se incorporan por sí solos a requisitos funcionales, criterios de aceptación ni tareas obligatorias.
- **Criterios de bloqueo.** Una SPEC no queda bloqueada para avanzar a Tasks por mejoras RECOMENDADAS u OPCIONALES pendientes. Bloquean únicamente requisitos obligatorios incumplidos o no verificables, contradicciones relevantes, riesgos críticos e invariantes esenciales comprometidas, conforme a §8 y al workflow SDD. El análisis distingue de forma explícita bloqueos de especificación, dependencias de implementación y mejoras futuras no bloqueantes.
- **Autoridad del equipo.** Los agentes identifican problemas y proponen soluciones; no convierten unilateralmente una recomendación en requisito obligatorio ni amplían el alcance aprobado. Toda decisión relevante de alcance requiere aprobación del equipo y queda registrada en el artefacto afectado, conforme a §9.
- **Aplicación a CHECKLIST y TASKS.** Los checklists evalúan la calidad de los requisitos obligatorios, no exigen exhaustividad ilimitada y no registran como FAIL bloqueante una observación opcional no implementada. TASKS genera tareas obligatorias para los requisitos aprobados y separa de forma visible cualquier mejora opcional, sin convertirla en tarea bloqueante.
- **Simplicidad.** Entre alternativas técnicamente válidas, elegir la más sencilla que satisfaga los requisitos obligatorios y preserve las garantías esenciales de §3, §4 y §6.

## 3. Seguridad

- Validar entradas externas no confiables conforme al contrato y las reglas funcionales. No deshabilitar seguridad para simplificar una implementación.
- Usar la autenticación del proyecto y comprobar autorización de forma independiente, incluidos roles, permisos y pertenencia al recurso cuando corresponda.
- Usar parametrización, binding o escaping seguro; no concatenar entrada externa cuando exista un mecanismo seguro equivalente. No implementar criptografía propia.
- Mantener secretos, credenciales y tokens fuera del código y del repositorio; no filtrar información sensible en respuestas, logs, errores o configuraciones versionadas.
- Aplicar mínimo privilegio, valores seguros por defecto y comunicaciones seguras, incluido HTTPS cuando corresponda. Corregir vulnerabilidades relevantes introducidas o agravadas por el cambio.

## 4. Contratos y sincronización

Los consumidores dependen de contratos, no de detalles internos. Todo cambio observable es un cambio de contrato: evaluar compatibilidad, incluidos campos, obligatoriedad, significado y errores. No introducir incompatibilidades silenciosas; tratar contratos con consumidores desconocidos como potencialmente utilizados.

Actualizar en el mismo cambio los elementos afectados: Spec, implementación, tests del backend, DTO, documentación, OpenAPI y configuración. Resolver discrepancias entre Spec y código según el comportamiento acordado; nunca cambiar la Spec para justificar una desviación no acordada ni alterar tests o controles para ocultar fallos.

Todo cambio que afecte el inventario tecnológico —incorporación, eliminación, sustitución o actualización de una tecnología o dependencia, o cambio de su versión o propósito sustancial de uso— debe reflejarse en la misma tarea en el `docs/backend/technologies.md` o `docs/frontend/technologies.md` correspondiente. La configuración efectiva conserva precedencia como fuente de verdad.

El contrato HTTP del backend se define en [arquitectura del backend](../../docs/backend/architecture.md) §1.7; sus reglas de documentación, en [convenciones del backend](../../docs/backend/conventions.md).

## 5. Idioma

Código nuevo o modificado en inglés: identificadores, paquetes, endpoints y campos JSON. Los nombres impuestos por contratos externos conservan su forma. Los nombres descriptivos de tests deben estar en español; el resto de su código sigue la regla general.

Documentación propia, Specs, Javadoc, OpenAPI y mensajes de validación en español; documentación externa puede conservar su idioma. Los nombres de ramas se rigen por [§7](#7-nombres-de-ramas-y-trazabilidad); los de commits, Pull Requests e Issues, en inglés. No traducir código existente fuera del alcance: aplicar estas convenciones a los elementos nuevos o modificados.

## 6. Verificación

El agente tiene prohibido ejecutar tests, incluso indirectamente. Los tests del backend los ejecuta el usuario; si se necesitan resultados, solicitar los casos concretos y su salida. El frontend queda temporalmente exento de testing: no crear ni ejecutar tests ni incorporar infraestructura para ellos.

El único control ejecutable por el agente es el build sin tests del área cuyo código cambió, con los comandos de AGENTS.md. Los cambios exclusivamente documentales no requieren build.

### Excepción acotada para infraestructura de desarrollo

Como única excepción a la exclusividad de controles ejecutables anterior, los agentes MAY ejecutar validaciones controladas de infraestructura de desarrollo exclusivamente cuando sean necesarias para comprobar mecanismos de integración de agentes, captura de evidencias y registro de conversaciones de OpenCode, incluidos los mecanismos para automatizar ese registro. Cada validación MUST contar con autorización humana explícita para una ejecución y un alcance concretos; esta norma no concede permisos implícitos.

- Antes de ejecutarla, el agente MUST identificar operaciones previstas, archivos y configuraciones afectados, datos consultados, resultados esperados, mecanismo de registro y procedimiento de reversión y limpieza.
- El agente MUST preservar el trabajo existente, proteger información sensible y registrar únicamente resultados realmente observados, con sus limitaciones.
- La excepción MUST NOT utilizarse para ejecutar tests frontend/backend ni tests de aplicación indirectamente, ampliar los builds autorizados, modificar comportamiento funcional, aprobar gates humanos, cambiar estados SDD o de tareas, modificar Trello, ejecutar operaciones Git de escritura ni acceder o modificar SPEC-008.
- La creación o modificación de instrumentación, plugins, configuraciones, servicios o sesiones requiere autorización humana específica. El inicio de servicios y el reinicio o interrupción de sesiones también requieren autorización específica.
- Las validaciones MUST ser reversibles. La limpieza MUST limitarse a elementos creados o modificados bajo autorización, sin eliminar archivos preexistentes ni resultados registrados.
- Ante riesgo de exposición de información sensible, pérdida de trabajo o efectos secundarios no autorizados, el agente MUST detenerse y solicitar decisión humana.
- Una validación satisfactoria de infraestructura no implica aceptación funcional de la aplicación ni aprobación de gates SDD.

### Condiciones de entrega

Se puede entregar con el build aprobado y los tests del backend explícitamente pendientes del usuario. Un fallo preexistente verificablemente ajeno al cambio no bloquea la entrega, pero debe reportarse. SonarQube, Quality Gates y otros controles opcionales quedan a cargo del usuario y no condicionan la finalización. El cambio debe dejar coherentes todos sus elementos afectados; toda ampliación del alcance requiere justificación explícita.

La entrega técnica y un build satisfactorio no acreditan CODE REVIEW, TESTING ni aceptación final de Feature. Los tests aplicables pendientes impiden superar TESTING y alcanzar DONE bajo el workflow vigente. La no aplicabilidad de tests automatizados requiere justificación y validación humana explícita. Se conserva la prohibición de ejecución directa e indirecta por el agente.

## 7. Nombres de ramas y trazabilidad

Esta convención es normativa y obligatoria. Aplica a toda rama creada en el repositorio, con independencia de quién la cree.

### Formato

```
<type>/<spec-id?>-<scope>
```

El segmento `<spec-id>` es opcional y solo se incluye cuando el cambio corresponde a una spec existente.

- `<type>` indica la naturaleza del cambio y se toma de esta lista cerrada: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`.
- `<spec-id>` es el identificador de la spec a la que pertenece el cambio.
- `<scope>` describe brevemente el cambio en `kebab-case`.

Si existe una spec relacionada:

```
<type>/<spec-id>-<scope>
```

Si el cambio es pequeño, técnico o independiente y no justifica una spec propia:

```
<type>/<scope>
```

### Reglas obligatorias

1. Antes de crear una rama, el agente debe revisar las specs existentes y determinar si el cambio pertenece a alguna. No debe crear nombres de rama sin esa determinación previa.
2. Un cambio que implemente, modifique o complete una spec existente debe reutilizar su identificador. Reutilizar el identificador no autoriza crear una spec nueva para un cambio que ya se asocia correctamente a una existente.
3. No asociar artificialmente un cambio a una spec cuando no exista relación funcional clara.
4. `<scope>` siempre en `kebab-case`. No se admiten `snake_case`, `camelCase` ni `PascalCase`.
5. No se admiten nombres personales ni descripciones genéricas o ambiguas. El nombre debe ser breve, descriptivo y representar con claridad el cambio.
6. Cuando la rama incluye un identificador de spec, el trabajo se vincula con el directorio correspondiente dentro de `specs/`, cuyos artefactos son la fuente principal de requisitos y contexto. La rama no sustituye ni reemplaza a los artefactos de la spec.
7. Se mantiene la trazabilidad entre spec, rama, commits y Pull Request. El Pull Request referencia la spec y la rama; los commits de la rama se asocian a esa spec.

Las herramientas de creación de features del proyecto pueden definir su propia nomenclatura interna para directorios de spec. Cuando su nomenclatura difiera del formato de rama de esta sección, esta sección prevalece para el nombre de la rama y la divergencia de la herramienta se reporta en lugar de resuelverse por cuenta propia.

## 8. Trazabilidad y sincronización con Trello

El tablero de seguimiento es [Football Market](https://trello.com/b/LdQo0xDW/football-market), con identificador `6abc6f718adba84bb0f09184`. Trello refleja las fuentes del repositorio; no sustituye el enunciado, las entregas, las Specs ni `tasks.md`. El índice bidireccional se mantiene en [specs/trello.md](../../specs/trello.md).

### Unidad de seguimiento y numeración

- La trazabilidad sigue `EPIC → SPEC → User Story → Task → Code`. Cada SPEC real se asocia a una épica `[EPIC] SPEC-<spec-id> - <nombre>`. La épica es contexto y agregación, no unidad ejecutable.
- Las operaciones de Spec Kit son Specify, Clarify, Plan, Checklist, Tasks, Analyze, Implement y Converge. El checklist `SDD Lifecycle` registra esas operaciones con evidencia explícita, no mera existencia de artefactos; no sustituye gates humanos. Las columnas operacionales únicas son `BACKLOG → SPECIFYING → READY → IN PROGRESS → CODE REVIEW → TESTING → DONE`.
- `sdd-orchestration` coordina y decide preparación, transiciones, dependencias e invalidaciones, subordinado a Constitution y workflow normativo. `trello-traceability` sincroniza y persiste evidencia; no aprueba, invalida ni deriva gates por sí misma.
- READY es preparación global de Feature válida y sin blockers de especificación, planificación o consistencia. `Dependency ≠ Blocker`: dependencias técnicas conocidas y correctamente identificadas no impiden READY si se resuelven mediante orden técnico y autorizaciones. READY no autoriza implementar; IMPLEMENT AUTHORIZATION humana explícita selecciona US y prerrequisitos compartidos. `IMPLEMENT AUTHORIZATION ≠ inicio de IMPLEMENT`: una US autorizada permanece READY hasta el inicio efectivo.
- Alcanzar READY exige, además, que la sincronización de tarjetas US esté completa: el cierre de READY incluye crear o reutilizar exactamente una tarjeta por cada US formal de `spec.md`, vinculadas a su épica y registradas en el índice. Evidencia de esa sincronización no persistida impide declarar READY. Una US sin tarjeta creada, vinculada y sincronizada no puede comenzar IMPLEMENT.
- Cada US recorre IMPLEMENT → CODE REVIEW humano → TESTING humano → DONE. Completar tareas no aprueba gates. Feature IN PROGRESS significa que existe al menos una US cuyo IMPLEMENT comenzó y la Feature todavía no cumple las condiciones para TESTING final; CODE REVIEW y TESTING de sus US conservan su significado propio.
- Todas las US requeridas DONE y trabajo técnico necesario completo habilitan Feature TESTING para CONVERGE final. CONVERGE satisfactorio prepara el cierre, no lo autoriza: Feature DONE requiere FEATURE COMPLETION AUTHORIZATION humana explícita, vigente y persistida.
- Decisiones e invalidaciones se persisten como comentarios estructurados `[SDD GATE]`, conservando historial. Evidencia obligatoria no persistida impide la transición dependiente. Invalidar solo por impacto sobre el alcance evaluado: código/tests que afecten ese alcance invalidan gates correspondientes; cambios sin impacto conservan aprobaciones. No resetear toda la Feature.
- SPEC-001–005 conservan DONE histórico bajo el workflow anterior, sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica el workflow vigente al alcance afectado.

- Cada user story de una spec tiene una tarjeta propia, no una tarjeta que agrupe todas las historias bajo una sola US. El título usa `SPEC-<spec-id> | US<nn> - <título de la historia>`; por ejemplo, `SPEC-007 | US01 - Reconocer al equipo y a la liga del jugador`.
- Las US se identifican, definen y refinan en `spec.md` durante SPECIFYING, CLARIFY, PLAN, CHECKLISTS y TASKS sin que eso obligue a crear sus tarjetas. El alta o reutilización de las tarjetas individuales de US ocurre al cerrar READY, después del gate humano de Analyze. Antes de ese momento las US existen como fuente documental con trazabilidad en `spec.md`; su ausencia de tarjeta no equivale a una US inexistente ni habilita inventar unidades ejecutables.
- `<spec-id>` es el identificador de una spec existente. `US<nn>` corresponde al número local de la historia dentro de esa spec, con al menos dos dígitos. La numeración de US empieza en cada spec; `SPEC-001 | US01` y `SPEC-007 | US01` son claves diferentes.
- No renumerar historias existentes solo por cambiar su orden o prioridad. Las historias nuevas reciben un identificador local libre; las divisiones, fusiones o retiros conservan trazabilidad de los identificadores y tarjetas anteriores.
- Las tareas técnicas de seguimiento se distinguen mediante `SPEC-<spec-id> | TSK<nn> - <título>`, con numeración local propia. No se presentan como US de producto ni se confunden con los identificadores `Tnnn` de `tasks.md`; cuando correspondan a tareas de ese archivo, deben referenciarlas explícitamente.
- Antes de existir una SPEC, el requisito futuro MUST representarse únicamente como `[EPIC] REQ-<id> - <nombre>`; por ejemplo, `[EPIC] REQ-S2-01 - Valuar jugadores con estrategias configurables`. Para requisitos generales se conserva el identificador `REQ-TP-<nn>` y para otras entregas el sprint correspondiente. Representa intención, no una unidad ejecutable, y MAY permanecer en Backlog hasta ser seleccionado para especificación. MUST NOT inventar US ni Tasks, ni usar como convención formal previa a SPEC claves `REQ-* | USxx` o `REQ-* | TSKxx`.
- Las US formales MUST derivar únicamente de `spec.md` de una SPEC existente. Las Tasks `Txxx` MUST derivar del SDD correspondiente y registrarse principalmente en `tasks.md`; MUST NOT convertirse automáticamente en tarjetas Trello.
- Specify MUST comprobar primero si corresponde reutilizar una SPEC existente y utilizar el mecanismo real del repositorio para crear/reanudar su identidad. La numeración automática de Spec Kit MUST ser el procedimiento predeterminado. MUST NOT predecir, reservar ni asignar manualmente números fuera de la excepción de trabajo paralelo definida a continuación. Al materializarse la SPEC, la épica REQ se asocia a `[EPIC] SPEC-<spec-id> - <nombre>`, preservando ID, URL, historial y antecedente cuando la correspondencia esté confirmada, sin duplicaciones innecesarias. Las claves heredadas se conservan como evidencia hasta una migración operativa explícitamente autorizada; no acreditan US formales ni elegibilidad.
  - Excepcionalmente, MAY asignarse explícitamente un identificador cuando exista una distribución previa y documentada de funcionalidades entre integrantes del equipo que trabajan en paralelo, incluso si sus especificaciones todavía no están integradas en la rama compartida.
  - Antes de crear la SPEC, MUST documentarse en un registro trazable del proyecto el número asignado, la funcionalidad, el integrante o equipo responsable y el motivo de la asignación explícita. Al materializarla, MUST referenciarse ese registro en la SPEC y mantenerse la relación con su requisito, rama y seguimiento. El registro de asignación no constituye una SPEC ni autoriza US o Tasks formales antes de existir `spec.md`.
  - MUST NOT reutilizarse un identificador ya asignado a otra funcionalidad, aunque su SPEC todavía no esté disponible localmente o integrada. La reanudación de la misma SPEC conserva su identidad y el mecanismo `--reuse`.
  - Antes de toda creación, automática o explícita, MUST verificarse la ausencia de colisiones con las especificaciones locales, remotas accesibles y las asignaciones conocidas por el equipo. MUST registrarse las fuentes consultadas y el resultado; si el acceso o la información no permiten descartar una colisión, MUST detenerse la creación y solicitar confirmación al equipo.
  - Cuando se cumplan estas condiciones, MAY utilizarse `--number` del script oficial de creación de Spec Kit sin modificarlo ni sustituir el flujo normal. MUST comprobarse el identificador efectivamente devuelto, la acción y el estado de rama; un ajuste automático del número no acredita la asignación acordada y MUST reportarse antes de continuar la redacción de la SPEC.
  - La asignación explícita no implica que la SPEC exista, esté implementada, aprobada o integrada, ni acredita fases, gates o autorizaciones de implementación. Una carpeta provisional o un `spec.md` vacío MUST identificarse como tal y MUST NOT presentarse como una especificación validada del equipo; no son requisito para asignar el número de otra funcionalidad.
  - La excepción MUST preservar la trazabilidad y consistencia del proceso SDD, las autorizaciones correspondientes y los cambios locales preexistentes. No permite sobrescribir specs, renumerarlas unilateralmente, fabricar evidencia ni omitir sincronización o gates.

### Sincronización obligatoria

1. Al crear o reutilizar una spec, revisar primero el tablero y el índice para localizar tarjetas relacionadas y crear o reutilizar la tarjeta de la épica o Feature correspondiente. Las tarjetas individuales de cada US no se crean en SPECIFYING: se crean o reutilizan al cerrar READY, tras superar el gate humano de Analyze, exactamente una por US formal definida en `spec.md`, con sus claves canónicas, vínculo con la épica, referencias a `spec.md` y `tasks.md` y ubicación en la columna READY salvo que exista un estado previo válido que deba preservarse. La lectura posterior debe verificar que toda US afectada tiene tarjeta, que no hay duplicados, que las asociaciones son correctas y que el índice está sincronizado. Si esa sincronización falla, se registra el problema y READY no se declara completado.
2. Al aclarar o modificar una spec, sincronizar las tarjetas afectadas: título, propósito, criterios o referencias precisas a sus escenarios, prioridad, dependencias, límites de alcance y decisiones pendientes. Los cambios en plan, tareas, avance o condiciones de entrega también actualizan el seguimiento afectado. No crear Specs ni ejecutar otros pasos de Spec Kit por el mero hecho de ordenar Trello.
3. Cada tarjeta asociada a una SPEC identificará su ruta y la US o tarea cuando corresponda; una épica REQ identificará el requisito futuro sin inventar una ruta de SPEC. Todas indicarán las fuentes aplicables de `tp/`, la entrega cuando esté acordada y las dependencias mediante claves o enlaces reales. El índice registrará las URL de las tarjetas y las relaciones provisionales. Los enlaces a GitHub no acreditan que cambios locales ya estén publicados.
4. Los cambios de estado requieren evidencia o confirmación explícita del usuario. Existencia de código, un plan, tareas marcadas o una reorganización no implica aprobación ni verificación. `Done` no sustituye `Status` de la spec ni acredita tests. Conservar y explicar estados heredados; los hallazgos técnicos siguen abiertos hasta su resolución comprobada. Los tests permanecen sujetos a §6.
5. Antes de escribir en Trello, releer las tarjetas afectadas y preservar miembros, comentarios, adjuntos, checklists, etiquetas pertinentes e historial. Reutilizar tarjetas por clave o correspondencia confirmada. No borrar, archivar, cerrar hallazgos ni cambiar alcance o entrega para ocultar discrepancias; cualquier retiro funcional necesita acuerdo y antecedente trazable.
6. Verificar mediante lectura posterior que las claves sean únicas, las asociaciones apunten a specs reales, todas las US afectadas tengan tarjeta y los cambios estén reflejados en el índice. Informar por separado cambios del repositorio y del tablero.
7. Si Trello no está disponible o faltan permisos, no afirmar que se sincronizó ni descartar cambios locales. Registrar en el índice las operaciones pendientes, informar el bloqueo y pedir al usuario que habilite el acceso o acuerde cómo completar la sincronización. La creación de la spec queda explícitamente pendiente de sincronización; al reanudar, comprobar lo ya aplicado antes de reintentar.

### Contingencia temporal por indisponibilidad de Trello

Cuando Trello no sea accesible, MAY completarse READY y persistirse IMPLEMENT AUTHORIZATION
en el repositorio mediante el procedimiento de contingencia del workflow SDD. Esta excepción
prevalece sobre los requisitos de sincronización remota, existencia de tarjetas US y persistencia
en Trello de esta sección exclusivamente para esos dos gates y el inicio de IMPLEMENT autorizado.
No permite ignorar la preparación global ni requisitos funcionales o normativos.

- MUST registrar en `specs/trello.md` la indisponibilidad y el alcance de la contingencia. La
  evidencia local por Feature se conserva en `specs/<spec-id>-<scope>/sdd-gates.md`, como eventos
  append-only identificados por fecha, entidad, gate, resultado, responsable, fuentes y alcance
  evaluado. No atribuir al usuario aprobaciones que no haya expresado.
- READY local exige los mismos artefactos, checklists evaluados, Analyze vigente y ausencia de
  blockers que READY ordinario. MUST identificar todas las US formales, su relación con la
  Feature y el estado local de cada una; no inventar IDs o URLs de tarjetas.
- IMPLEMENT AUTHORIZATION local MUST reproducir o referenciar inequívocamente la autorización
  humana explícita, sus US y prerrequisitos compartidos. MUST comprobar su vigencia y releer
  la evidencia persistida antes de iniciar. La autorización no equivale al inicio: registrar
  por separado la toma y el estado local IN PROGRESS al comenzar realmente IMPLEMENT.
- La contingencia termina cuando se restablece el acceso. MUST releer primero el tablero,
  reutilizar o crear las tarjetas faltantes sin duplicar, trasladar las evidencias con su fecha
  y procedencia originales, verificar por lectura posterior y actualizar el índice. Conservar
  el historial local y marcar la sincronización efectivamente realizada; no fabricar acciones
  remotas ni afirmar SYNCED mientras queden operaciones pendientes.
- CODE REVIEW y TESTING humanos, sus evidencias y transiciones, CONVERGE y FEATURE COMPLETION
  AUTHORIZATION conservan sus reglas ordinarias. Esta excepción no habilita su aprobación ni
  persistencia sustitutiva, no ejecuta tests y no autoriza cambios de alcance.

## 9. Gobernanza

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution ni los documentos normativos aplicables para justificar o facilitar una implementación incompatible. Solo pueden modificarlos cuando el usuario solicite explícitamente una modificación normativa.

Ante un conflicto necesario, proponer la enmienda antes del cambio incompatible. Si los documentos, la Spec y el código no resuelven una decisión normativa relevante, solicitar aclaración sin inventar reglas o excepciones.

Las enmiendas usan versionado semántico: MAJOR para eliminación o redefinición incompatible de gobernanza/principios; MINOR para principios nuevos o ampliación material compatible; PATCH para aclaraciones sin cambio semántico. Deben justificar el incremento, registrar impacto y dependencias, conservar la fecha de ratificación y actualizar la fecha de última enmienda.


**Versión**: 8.1.0 | **Ratificación**: 2026-08-31 | **Última enmienda**: 2026-10-16

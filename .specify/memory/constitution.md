<!--
Sync Impact Report — Enmienda 2026-10-07
Versión: 5.0.0 → 6.0.0 (MAJOR: condiciones obligatorias de autorización, gates y cierre redefinidas).
Principios modificados: §6 distingue entrega técnica de DONE; §8 consolida lifecycle y evidencia.
Sin secciones eliminadas. Dependencias actualizadas: workflow SDD, implement, plantilla tasks,
workflow YAML, estados de specs y skills sdd-orchestration/trello-traceability.
AGENTS y comandos analyze/converge conservan prohibición de tests y contratos de lectura/append-only.
Sin cambios en Trello, código, specs funcionales ni tasks.md; H1–H4 y tests de SPEC-007 fuera de alcance.
-->
# Constitución de Football Market

## 1. Documentos normativos

Esta constitución contiene las reglas globales. [Arquitectura común](../../docs/architecture.md), [arquitectura del backend](../../docs/backend/architecture.md) y [arquitectura del frontend](../../docs/frontend/architecture.md) definen roles y dependencias; las [convenciones del backend](../../docs/backend/conventions.md) y las [convenciones del frontend](../../docs/frontend/conventions.md) definen sus reglas de implementación; [tecnologías del backend](../../docs/backend/technologies.md) y [tecnologías del frontend](../../docs/frontend/technologies.md) documentan el stack y su propósito; [testing](../../docs/backend/testing.md) define las pruebas del backend. Todos tienen la misma obligatoriedad cuando resulten aplicables. La configuración efectiva del proyecto conserva precedencia como fuente de verdad de dependencias y versiones.

Consultar solo las secciones pertinentes y sus reglas generales, siguiendo las guías de lectura de cada documento. Las instrucciones operativas están en [AGENTS.md](../../AGENTS.md). Specs y Plans no deben repetir restricciones globales salvo consideraciones específicas de la feature.

Constitution tiene máxima jerarquía normativa. Los documentos normativos de área mantienen su obligatoriedad aplicable; el [workflow SDD](../../docs/development/sdd-workflow.md) detalla el protocolo subordinado a esas normas, y AGENTS y comandos lo ejecutan dentro del alcance autorizado. La Spec define verdad funcional de la feature; plan, tareas y checklists la desarrollan sin contradecirla. Trello registra estado operativo y Git implementación observada, sin autoridad para redefinir intención. La configuración efectiva sigue siendo la fuente de verdad de versiones y dependencias. Ante conflicto entre fuentes obligatorias, detener la transición y aplicar §9, no inventar una excepción.

[Enunciado del TP](../../tp/enunciado.md) y [entregas del TP](../../tp/entregas.md) son fuentes complementarias de requisitos académicos externos: el primero define el alcance general y el segundo agrega condiciones por entrega y sus actualizaciones docentes. Deben consultarse al especificar, aclarar, planificar, generar tareas, implementar y revisar cumplimiento o entregas. Un requisito no se descarta por aparecer solo en uno de ellos. Las decisiones técnicas, Specs o tarjetas de Trello no pueden modificar esos requisitos para justificar una desviación; las contradicciones se señalan antes de proponer cambios.

## 2. Calidad y alcance

- Preferir la solución válida más simple, con responsabilidades coherentes y detalles encapsulados mediante contratos. No crear capas, interfaces, dependencias o abstracciones por costumbre o necesidades hipotéticas; una duplicación no exige por sí sola abstraer.
- No agregar dependencias si las capacidades existentes bastan. Las tecnologías nuevas requieren justificación técnica y aprobación; las versiones administradas por Spring Boot no se sobrescriben sin necesidad justificada.
- Limitar los cambios a la tarea y a la coherencia de sus elementos afectados. Refactorizar solo con beneficio concreto, preservando comportamiento ajeno; revisar consumidores antes de eliminar o renombrar elementos.
- Eliminar código muerto dentro del alcance. Mantener comentarios útiles y actualizados, sin repetir lo evidente.
- Reportar violaciones o fallos preexistentes ajenos al cambio sin ocultarlos ni ampliar el alcance para corregirlos.

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
- Cada US recorre IMPLEMENT → CODE REVIEW humano → TESTING humano → DONE. Completar tareas no aprueba gates. Feature IN PROGRESS significa que existe al menos una US cuyo IMPLEMENT comenzó y la Feature todavía no cumple las condiciones para TESTING final; CODE REVIEW y TESTING de sus US conservan su significado propio.
- Todas las US requeridas DONE y trabajo técnico necesario completo habilitan Feature TESTING para CONVERGE final. CONVERGE satisfactorio prepara el cierre, no lo autoriza: Feature DONE requiere FEATURE COMPLETION AUTHORIZATION humana explícita, vigente y persistida.
- Decisiones e invalidaciones se persisten como comentarios estructurados `[SDD GATE]`, conservando historial. Evidencia obligatoria no persistida impide la transición dependiente. Invalidar solo por impacto sobre el alcance evaluado: código/tests que afecten ese alcance invalidan gates correspondientes; cambios sin impacto conservan aprobaciones. No resetear toda la Feature.
- SPEC-001–005 conservan DONE histórico bajo el workflow anterior, sin gates retroactivos ni evidencia fabricada. Una reapertura material aplica el workflow vigente al alcance afectado.

- Cada user story de una spec tiene una tarjeta propia, no una tarjeta que agrupe todas las historias bajo una sola US. El título usa `SPEC-<spec-id> | US<nn> - <título de la historia>`; por ejemplo, `SPEC-007 | US01 - Reconocer al equipo y a la liga del jugador`.
- `<spec-id>` es el identificador de una spec existente. `US<nn>` corresponde al número local de la historia dentro de esa spec, con al menos dos dígitos. La numeración de US empieza en cada spec; `SPEC-001 | US01` y `SPEC-007 | US01` son claves diferentes.
- No renumerar historias existentes solo por cambiar su orden o prioridad. Las historias nuevas reciben un identificador local libre; las divisiones, fusiones o retiros conservan trazabilidad de los identificadores y tarjetas anteriores.
- Las tareas técnicas de seguimiento se distinguen mediante `SPEC-<spec-id> | TSK<nn> - <título>`, con numeración local propia. No se presentan como US de producto ni se confunden con los identificadores `Tnnn` de `tasks.md`; cuando correspondan a tareas de ese archivo, deben referenciarlas explícitamente.
- Antes de existir una SPEC, el requisito futuro MUST representarse únicamente como `[EPIC] REQ-<id> - <nombre>`; por ejemplo, `[EPIC] REQ-S2-01 - Valuar jugadores con estrategias configurables`. Para requisitos generales se conserva el identificador `REQ-TP-<nn>` y para otras entregas el sprint correspondiente. Representa intención, no una unidad ejecutable, y MAY permanecer en Backlog hasta ser seleccionado para especificación. MUST NOT inventar US ni Tasks, ni usar como convención formal previa a SPEC claves `REQ-* | USxx` o `REQ-* | TSKxx`.
- Las US formales MUST derivar únicamente de `spec.md` de una SPEC existente. Las Tasks `Txxx` MUST derivar del SDD correspondiente y registrarse principalmente en `tasks.md`; MUST NOT convertirse automáticamente en tarjetas Trello.
- Specify MUST comprobar primero si corresponde reutilizar una SPEC existente y utilizar el mecanismo real del repositorio para crear/reanudar su identidad. MUST NOT predecir ni reservar manualmente números de SPEC. Al materializarse la SPEC, la épica REQ se asocia a `[EPIC] SPEC-<spec-id> - <nombre>`, preservando ID, URL, historial y antecedente cuando la correspondencia esté confirmada, sin duplicaciones innecesarias. Las claves heredadas se conservan como evidencia hasta una migración operativa explícitamente autorizada; no acreditan US formales ni elegibilidad.

### Sincronización obligatoria

1. Al crear o reutilizar una spec, revisar primero el tablero y el índice para localizar tarjetas relacionadas. Crear o actualizar una tarjeta por cada US de la spec dentro del mismo trabajo, sin duplicar tarjetas ni inventar historias para completar una numeración.
2. Al aclarar o modificar una spec, sincronizar las tarjetas afectadas: título, propósito, criterios o referencias precisas a sus escenarios, prioridad, dependencias, límites de alcance y decisiones pendientes. Los cambios en plan, tareas, avance o condiciones de entrega también actualizan el seguimiento afectado. No crear Specs ni ejecutar otros pasos de Spec Kit por el mero hecho de ordenar Trello.
3. Cada tarjeta asociada a una SPEC identificará su ruta y la US o tarea cuando corresponda; una épica REQ identificará el requisito futuro sin inventar una ruta de SPEC. Todas indicarán las fuentes aplicables de `tp/`, la entrega cuando esté acordada y las dependencias mediante claves o enlaces reales. El índice registrará las URL de las tarjetas y las relaciones provisionales. Los enlaces a GitHub no acreditan que cambios locales ya estén publicados.
4. Los cambios de estado requieren evidencia o confirmación explícita del usuario. Existencia de código, un plan, tareas marcadas o una reorganización no implica aprobación ni verificación. `Done` no sustituye `Status` de la spec ni acredita tests. Conservar y explicar estados heredados; los hallazgos técnicos siguen abiertos hasta su resolución comprobada. Los tests permanecen sujetos a §6.
5. Antes de escribir en Trello, releer las tarjetas afectadas y preservar miembros, comentarios, adjuntos, checklists, etiquetas pertinentes e historial. Reutilizar tarjetas por clave o correspondencia confirmada. No borrar, archivar, cerrar hallazgos ni cambiar alcance o entrega para ocultar discrepancias; cualquier retiro funcional necesita acuerdo y antecedente trazable.
6. Verificar mediante lectura posterior que las claves sean únicas, las asociaciones apunten a specs reales, todas las US afectadas tengan tarjeta y los cambios estén reflejados en el índice. Informar por separado cambios del repositorio y del tablero.
7. Si Trello no está disponible o faltan permisos, no afirmar que se sincronizó ni descartar cambios locales. Registrar en el índice las operaciones pendientes, informar el bloqueo y pedir al usuario que habilite el acceso o acuerde cómo completar la sincronización. La creación de la spec queda explícitamente pendiente de sincronización; al reanudar, comprobar lo ya aplicado antes de reintentar.

## 9. Gobernanza

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution ni los documentos normativos aplicables para justificar o facilitar una implementación incompatible. Solo pueden modificarlos cuando el usuario solicite explícitamente una modificación normativa.

Ante un conflicto necesario, proponer la enmienda antes del cambio incompatible. Si los documentos, la Spec y el código no resuelven una decisión normativa relevante, solicitar aclaración sin inventar reglas o excepciones.

Las enmiendas usan versionado semántico: MAJOR para eliminación o redefinición incompatible de gobernanza/principios; MINOR para principios nuevos o ampliación material compatible; PATCH para aclaraciones sin cambio semántico. Deben justificar el incremento, registrar impacto y dependencias, conservar la fecha de ratificación y actualizar la fecha de última enmienda.

**Versión**: 6.0.0 | **Ratificación**: 2026-08-31 | **Última enmienda**: 2026-10-07

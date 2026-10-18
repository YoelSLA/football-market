<!--
Football Market Constitution — Constitution vigente.

Las enmiendas y su historialSync Impact Report completo se conservan en
.specify/memory/history/constitution-changelog.md. Este archivo contiene únicamente los
principios y garantías vigentes.

Documentos subordinados y su responsabilidad única:

| Responsabilidad                          | Fuente                                          |
| ---------------------------------------- | ----------------------------------------------- |
| Principios y garantías                    | Este archivo                                    |
| Roles y dependencias                     | `docs/architecture.md` y la arquitectura de cada área |
| Convenciones de implementación           | Convenciones de cada área                       |
| Stack, versiones y dependencias           | Tecnologías de cada área y la configuración efectiva |
| Requisitos académicos                    | `tp/enunciado.md` y `tp/entregas.md`           |
| Procedimiento de desarrollo SDD          | `docs/development/sdd-workflow.md`             |
| Ramas, commits y Pull Requests            | `docs/development/git-workflow.md`             |
| Deuda técnica                             | `docs/TECHNICAL_DEBT.md`                       |
| Instrucciones de trabajo del agente       | `AGENTS.md` y los `AGENTS.md` de cada área      |
| Estado de las especificaciones            | `specs/README.md`                               |
-->
# Football Market Constitution

## Core Principles

### I. Simplicidad y calidad

Preferir la solución válida más simple, con responsabilidades coherentes y detalles
encapsulados mediante contratos. No crear capas, interfaces, dependencias o abstracciones por
costumbre o por necesidades hipotéticas; una duplicación no exige por sí sola abstraer. No
agregar dependencias si las capacidades existentes bastan; las tecnologías nuevas requieren
justificación técnica y aprobación, y las versiones administradas por la plataforma no se
sobrescriben sin necesidad justificada.

Limitar los cambios a la tarea y a la coherencia de sus elementos afectados. Refactorizar solo
con beneficio concreto, preservando el comportamiento ajeno y revisando consumidores antes de
eliminar o renombrar elementos. Eliminar el código muerto dentro del alcance y mantener
comentarios útiles y actualizados, sin repetir lo evidente.

Entre alternativas técnicamente válidas, elegir la más sencilla que satisfaga los requisitos y
preserve las garantías de este documento.

### II. Proporcionalidad del alcance

Dimensionar cada especificación al alcance solicitado. No agregar requisitos por constituir
buenas prácticas generales ni porque un escenario sea técnicamente posible.

Todo caso límite se clasifica **antes** de convertirse en requisito:

- **Obligatorio**: exigido por los requisitos académicos, aprobado expresamente por el equipo o
  necesario para preservar una garantía esencial.
- **Recomendado**: mejora razonable de robustez, mantenibilidad o experiencia de uso, sujeta a
  evaluación.
- **Opcional**: escenario excepcional, hipotético o de baja relevancia práctica.

Un caso sin clasificar no es requisito. Solo los obligatorios bloquean el avance; los
recomendados y opcionales se documentan, cuando aportan valor, como observaciones, riesgos
conocidos o mejoras futuras, y no entran por sí solos en requisitos funcionales, criterios de
aceptación ni tareas obligatorias.

Antes de elevar un caso a obligatorio, evaluar si puede ocurrir en los flujos reales de la
aplicación, si puede reproducirse o verificarse razonablemente, su probabilidad e impacto, su
relación con los requisitos originales, la complejidad de implementarlo y si existe una solución
más sencilla que lo resuelva. La dificultad de reproducción no justifica descartar ni posponer
riesgos críticos de seguridad o integridad.

Los agentes identifican problemas y proponen soluciones: no convierten unilateralmente una
recomendación en requisito obligatorio ni amplían el alcance aprobado. Toda decisión relevante
de alcance requiere aprobación del equipo y queda registrada en el artefacto afectado.

### III. Seguridad e integridad

Validar las entradas externas no confiables conforme al contrato y a las reglas funcionales. No
deshabilitar seguridad para simplificar una implementación.

Usar la autenticación del proyecto y comprobar la autorización de forma independiente,
incluidos roles, permisos y pertenencia al recurso cuando corresponda.

Usar parametrización, binding o escapado seguro; no concatenar entrada externa cuando exista un
mecanismo seguro equivalente. No implementar criptografía propia.

Mantener secretos, credenciales y tokens fuera del código y del repositorio, y no filtrar
información sensible en respuestas, registros, errores ni configuraciones versionadas.

Aplicar mínimo privilegio, valores seguros por defecto y comunicaciones seguras. Corregir las
vulnerabilidades relevantes introducidas o agravadas por el cambio.

### IV. Coherencia de contratos

Los consumidores dependen de contratos, no de detalles internos. Todo cambio observable es un
cambio de contrato: evaluar su compatibilidad —campos, obligatoriedad, significado y errores— y
tratar los contratos con consumidores desconocidos como potencialmente utilizados. No
introducir incompatibilidades silenciosas.

Actualizar **en el mismo cambio** todos los elementos afectados: especificación, implementación,
tests, contratos, documentación y configuración. Resolver las discrepancias entre especificación
y código según el comportamiento acordado. Nunca cambiar la especificación para justificar una
desviación no acordada, ni alterar tests o controles para ocultar fallos.

Todo cambio que afecte al inventario tecnológico —incorporación, eliminación, sustitución o
actualización de una tecnología o de una dependencia, o cambio de su versión o de su propósito
sustancial de uso— debe reflejarse en la misma tarea en el documento de tecnologías del área
correspondiente. La configuración efectiva conserva precedencia como fuente de verdad.

### V. Trazabilidad y fuente de verdad

El seguimiento operativo de una funcionalidad se realiza en el sistema de seguimiento del
proyecto. Su configuración, sus estados y sus transiciones están definidos en
[`docs/development/sdd-workflow.md`](../../docs/development/sdd-workflow.md).

El seguimiento operativo **no es fuente de requisitos**. Una tarjeta, una etiqueta, una columna
o un comentario no sustituye a la especificación, al plan, a las tareas ni a la documentación
técnica. Si una tarjeta contradice un artefacto, prevalece el artefacto.

El estado de una funcionalidad tiene dos ámbitos con fuentes distintas:

- El **estado del proceso**, que viven en los artefactos de la especificación y se rige por
  [`specs/README.md`](../../specs/README.md).
- El **estado del seguimiento**, que vive en el sistema de seguimiento.

Cada fuente gobierna únicamente su ámbito y **no existe precedencia universal entre ambas**: el
estado del proceso no describe el avance del seguimiento, ni el estado del seguimiento describe
la fase del proceso. Si discrepan, es una inconsistencia que se corrige en el origen que la
evidencia contradija, no un conflicto que se resuelva aplicando una fuente sobre la otra.

Cada cambio debe ser trazable a su origen: la funcionalidad a su requisito, el trabajo a su
especificación y el resultado a su rama, sus commits y su Pull Request. Las convenciones de
ramas, commits y Pull Requests están en
[`docs/development/git-workflow.md`](../../docs/development/git-workflow.md).

Ningún estado se deduce de la existencia de archivos, de tareas marcadas, de controles
técnicos, de commits, de merges ni de Pull Requests abiertos. Los registros que contienen
información histórica conservan su validez como evidencia y no se reescriben para reflejar un
estado posterior.

## Restricciones adicionales

### Idioma

El código nuevo o modificado se escribe en inglés: identificadores, paquetes, endpoints y
campos de serialización. Los nombres impuestos por contratos externos conservan su forma. Los
nombres descriptivos de tests se escriben en español; el resto de su código sigue la regla
general.

La documentación propia, las especificaciones, la documentación de API y los mensajes de
validación se escriben en español; la documentación externa puede conservar su idioma. Los
nombres de commits, ramas, Issues y Pull Requests se rigen por las convenciones del proyecto.
No traducir código existente fuera del alcance.

### Gestión de datos en desarrollo

Mientras el proyecto no esté en producción, los datos son descartables y su conservación no es
un requisito. Aplicar estas reglas únicamente a la base de datos y al estado de desarrollo; no
constituyen una excepción a los principios anteriores.

Preferir el esquema y la representación de datos más simples que satisfagan los requisitos
actuales. Se permite eliminar, recrear o reinicializar datos, tablas y estructuras de base de
datos cuando eso simplifique la implementación.

No implementar migraciones, mecanismos de compatibilidad retroactiva, columnas o rutas de
escritura preservadoras ni capas de traducción cuyo único propósito sea conservar datos o
estructuras de desarrollo existentes. No anticipar requisitos de producción no definidos.

Esta permisividad se limita a los datos: no habilita alterar contratos, eliminar operaciones o
respuestas documentadas, ni evadir el principio de coherencia de contratos. Toda operación
destructiva respeta las restricciones de verificación y las autorizaciones humanas, y no se
ejecuta por iniciativa propia del agente.

Esta restricción se revisa cuando el proyecto avance hacia producción y deje de ser aplicable
sin una enmienda posterior.

## Flujo de desarrollo

### Metodología

El desarrollo se rige por Spec-Driven Development: una funcionalidad se especifica, se planifica,
se descompone en tareas, se implementa y se valida antes de cerrarse. Los procedimientos, las
condiciones de avance entre fases y el uso de los comandos de la herramienta de especificación
están en [`docs/development/sdd-workflow.md`](../../docs/development/sdd-workflow.md), que es
el único documento que los detalla.

El agente no emula ni duplica operaciones de la herramienta de especificación. Si una
operación requerida falta o falla, se detiene el trabajo y se reporta.

### Verificación y testing

**El agente tiene prohibido ejecutar tests**, directa e indirectamente, mediante comandos,
scripts, builds, hooks o generadores. Los tests los ejecuta el usuario. El agente tampoco puede
presentar tests como ejecutados sin resultados aportados por el usuario.

El único control ejecutable por el agente es el que el área afectada autorice en su `AGENTS.md`,
y nunca un control que incluya la ejecución de tests. Los cambios exclusivamente documentales no
requieren control ejecutable.

**Exención temporal de testing del frontend.** El frontend queda exento de testing: no se crean
ni se ejecutan tests del frontend, ni se incorpora infraestructura para ellos. Esta exención es
temporal y se revisa cuando el proyecto lo permita.

**Excepción acotada para infraestructura de desarrollo.** Como única excepción a la exclusividad
anterior, el agente **MAY** ejecutar validaciones controladas de infraestructura de desarrollo,
exclusivamente cuando sean necesarias para comprobar mecanismos de integración de agentes,
captura de evidencias y registro de conversaciones. Cada validación **MUST** contar con
autorización humana explícita para una ejecución y un alcance concretos; esta norma no concede
permisos implícitos.

Esa excepción **MUST NOT** utilizarse para ejecutar tests de aplicación, directa o
indirectamente, ampliar los controles autorizados, modificar comportamiento funcional, aprobar o
invalidar autorizaciones, cambiar estados del proceso o del seguimiento, modificar el tablero o
las entradas de seguimiento, ejecutar operaciones de escritura de historial ni acceder o
modificar SPEC-008. La creación o modificación de instrumentación, plugins, configuraciones,
servicios o sesiones requiere autorización humana específica, y el inicio de servicios o la
interrupción de sesiones también.

Las validaciones **MUST** ser reversibles, preservar el trabajo existente, proteger la
información sensible y registrar únicamente resultados realmente observados, con sus
limitaciones. La limpieza **MUST** limitarse a elementos creados o modificados bajo
autorización. Ante riesgo de exposición de información sensible, pérdida de trabajo o efectos
secundarios no autorizados, el agente **MUST** detenerse y solicitar decisión humana. Una
validación satisfactoria de infraestructura no implica aceptación funcional de la aplicación ni
aprobación de ninguna autorización.

### Condiciones de entrega

Se puede entregar con el control aprobado y los tests explícitamente pendientes del usuario. Un
fallo preexistente verificablemente ajeno al cambio no bloquea la entrega, pero debe reportarse.
Los controles opcionales a cargo del usuario, como análisis estático o puertas de calidad, no
condicionan la finalización.

**La entrega técnica y un control satisfactorio no acreditan revisión, testing ni cierre.** Los
tests aplicables pendientes impiden superar el testing y alcanzar el cierre. La no aplicabilidad
de tests automatizados requiere justificación y validación humana explícita.

### Autorizaciones humanas

Las personas deciden; el agente prepara, implementa y documenta.

**Ninguna implementación se escribe sin autorización humana explícita y vigente.** Ninguna
autorización se concede, renueva, infiere ni sustituye por otra, ni se deriva de tareas
completas, de un control técnico, de un commit, de un Pull Request ni de un diagnóstico
favorable. Una expresión ambigua no constituye aprobación, y el agente no puede aprobar,
rechazar ni dar por válido un gate en nombre del usuario.

Las autorizaciones requeridas son cuatro y se consolidan **por alcance explícito**, no por
unidad de historia:

1. **Implementación** — habilita escribir el código del alcance autorizado y no habilita la
   revisión, el testing ni el cierre.
2. **Revisión de código** — sobre el informe de revisión completo, incluidos los tests. Revisar
   y aprobar código no sustituye revisar y aprobar pruebas.
3. **Testing** — sobre el resultado ejecutado y aportado por el usuario, o sobre una no
   aplicabilidad justificada y validada explícitamente.
4. **Cierre de la funcionalidad** — referida a la funcionalidad concreta y al resultado
   evaluado y presentado. Un diagnóstico favorable prepara el cierre, no lo autoriza.

Toda autorización debe ser explícita, verificable y persistente en un soporte del proyecto, de
modo que se pueda identificar quién autorizó, qué autorizó y con qué evidencia se compara. No
se prescribe un formato obligatorio ni un sistema específico, y la evidencia existente es
reutilizable cuando sea suficiente para acreditar una autorización vigente.

Las autorizaciones se **invalidan solo por impacto sobre el alcance evaluado**: los cambios de
código, tests o requisitos que afecten a lo evaluado invalidan las autorizaciones
correspondientes, y los cambios sin impacto conservan las aprobaciones no afectadas. Las
invalidaciones se registran como eventos nuevos, con su motivo y su alcance, sin borrar la
evidencia histórica y sin atribuirse a una aprobación humana. No se reinicia el trabajo completo
por un cambio acotado.

Las condiciones, los informes y las listas de verificación de cada autorización están en
[`docs/development/sdd-workflow.md`](../../docs/development/sdd-workflow.md).

### Definición de cierre

Una funcionalidad se cierra únicamente con: trabajo técnico necesario completo, implementación
completa del alcance autorizado, revisión de código vigente, testing aprobado o no aplicabilidad
validada, diagnóstico final satisfactorio, autorización de cierre humana explícita, vigente y
persistida, y coherencia de todos los artefactos afectados. `tasks.md` completo, un build
satisfactorio, un commit, un merge o un Pull Request abierto no equivalen a cierre.

## Governance

Esta Constitución tiene la máxima autoridad normativa del proyecto. Ninguna decisión técnica,
especificación, artefacto o registro operativo puede justificarse debilitándola, reinterpretándola
o ignorándola. Los agentes no pueden modificarla, ignorarla ni reinterpretarla para facilitar una
implementación incompatible; solo pueden hacerlo cuando el usuario lo solicite expresamente.

Los documentos normativos de cada área mantienen su obligatoriedad cuando resulten aplicables.
El procedimiento de desarrollo se ejecuta dentro del alcance autorizado. Ante un conflicto entre
fuentes obligatorias, se detiene la transición y se aplican estas reglas de gobernanza; no se
inventan excepciones. Ninguna especificación o registro operativo puede alterar los requisitos
académicos para justificar una desviación: las contradicciones se señalan antes de proponer
cambios.

Las enmiendas requieren documentación, aprobación humana y, cuando existan, un plan de
migración. Una enmienda **MUST** preservar la estructura vigente y las secciones ajenas.

El versionado es semántico:

- **MAJOR** al eliminar o redefinir de forma incompatible un principio o una garantía.
- **MINOR** al añadir un principio o ampliar materialmente uno existente de forma compatible.
- **PATCH** al aclarar sin cambio semántico.

Toda enmienda justifica su incremento, registra el impacto y las dependencias, conserva la fecha
de ratificación y actualiza la fecha de la última enmienda. Cada enmienda produce un informe de
impacto que se conserva **íntegro** en
[`.specify/memory/history/constitution-changelog.md`](history/constitution-changelog.md), junto
a la Constitución y no dentro de ella.

**Version**: 9.0.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-10-18
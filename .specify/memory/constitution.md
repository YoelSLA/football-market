<!--
Sync Impact Report:
- Version change: 1.1.0 -> 1.2.0
- List of modified principles:
  - None
- Added sections:
  - None
- Modified sections:
  - Política de Idioma
- Removed sections:
  - None
- Follow-up TODOs: None
-->

# Football Market Constitution

## Core Principles

### 1. Separación de responsabilidades y modularidad

Cada componente, módulo o parte del sistema debe tener una responsabilidad claramente definida y debe encargarse únicamente de las responsabilidades que le correspondan. Se debe evitar la concentración de responsabilidades, la mezcla de diferentes tipos de lógica y las dependencias innecesarias entre componentes. El sistema debe organizarse de manera modular, favoreciendo la cohesión y el bajo acoplamiento, de forma que las funcionalidades puedan evolucionar sin generar impactos innecesarios en otras partes del sistema. La organización de módulos, componentes y recursos debe responder a responsabilidades claramente identificables y permitir que la arquitectura evolucione conforme cambien las necesidades del proyecto.

### 2. Lógica de negocio

La lógica de negocio debe mantenerse dentro de las responsabilidades propias del dominio y separada de los mecanismos de presentación, persistencia e infraestructura. Las reglas que puedan resolverse utilizando únicamente el estado y comportamiento propio de una entidad o concepto del dominio deben permanecer asociadas a dicho concepto. Las reglas que requieran coordinar información, recursos o dependencias externas deben ubicarse en el componente responsable de dicha coordinación.

### 3. Integración y confiabilidad de fuentes externas

Las APIs, servicios y fuentes de información externas deben mantenerse aisladas de la lógica interna de la aplicación y sus particularidades no deben propagarse innecesariamente por el sistema. La información proveniente de estas fuentes debe considerarse no confiable y validarse antes de ser incorporada. Las integraciones deben contemplar errores, respuestas inválidas, indisponibilidad y cambios inesperados, evitando que estos problemas comprometan el funcionamiento o la integridad de la aplicación. El sistema debe poder modificar o reemplazar una dependencia externa sin generar cambios innecesarios en el resto de la aplicación.

### 4. Testing obligatorio

Todo comportamiento relevante del sistema, ya sea nuevo o modificado, debe estar respaldado por pruebas automatizadas que verifiquen su funcionamiento esperado y los escenarios de error relevantes. Las pruebas deben formar parte del desarrollo de cada funcionalidad y una funcionalidad no se considera terminada mientras las pruebas correspondientes no sean satisfactorias.

### 5. Calidad y estabilidad de las pruebas

Las pruebas deben verificar comportamientos y reglas relevantes del sistema, contemplando escenarios esperados, errores, límites y condiciones relevantes, y deben proporcionar una verificación real y reproducible del comportamiento esperado. No deben implementarse únicamente para aumentar métricas de cobertura. Las pruebas existentes deben mantenerse para proteger los comportamientos previamente validados y evitar regresiones. Cuando un requisito cambie intencionalmente, las pruebas correspondientes podrán actualizarse para reflejar el nuevo comportamiento, pero no deben modificarse para ocultar errores de implementación. Las pruebas de integraciones externas deben poder ejecutarse de forma controlada sin depender de la disponibilidad de terceros cuando dicha disponibilidad no sea parte de lo que se pretende verificar.

### 6. Calidad de código

El código debe ser claro, legible, consistente y mantenible, además de cumplir correctamente con los requisitos funcionales. Se debe evitar la duplicación innecesaria de lógica y mantener criterios consistentes en todo el sistema, evitando implementar de forma independiente las mismas reglas cuando esto pueda generar comportamientos inconsistentes. Las nuevas funcionalidades y modificaciones no deben degradar injustificadamente la calidad existente ni introducir prácticas que dificulten innecesariamente la comprensión y evolución del código.

### 7. Control de calidad automatizado

La calidad del código debe evaluarse de forma continua durante el desarrollo mediante las validaciones establecidas por el proyecto. Los cambios deben superar los controles correspondientes antes de considerarse terminados. Las métricas y herramientas de calidad deben utilizarse para detectar y prevenir problemas, pero no deben convertirse en un objetivo por sí mismas cuando hacerlo implique sacrificar la corrección, claridad o mantenibilidad del código.

### 8. Seguridad

La seguridad debe considerarse un requisito transversal del sistema desde su diseño y durante todo su desarrollo. Los datos, entradas, recursos y operaciones deben tratarse de forma segura, sin asumir como confiable la información proveniente de usuarios, clientes o fuentes externas. Las validaciones o restricciones aplicadas en el cliente no deben considerarse suficientes para garantizar la seguridad. El acceso a funcionalidades y recursos debe estar protegido mediante mecanismos adecuados de autenticación y autorización, verificando que cada usuario tenga los permisos necesarios para realizar una operación o acceder a un recurso. Los secretos y la información sensible deben mantenerse protegidos y no deben exponerse mediante el código, repositorio, logs, respuestas o mensajes de error. El sistema debe aplicar controles adecuados para evitar accesos no autorizados, manipulación de solicitudes y exposición o modificación indebida de información.

### 9. Contratos entre componentes

La comunicación entre las distintas partes del sistema debe basarse en contratos claros, explícitos y consistentes. Los consumidores deben utilizar dichos contratos sin depender de comportamientos implícitos o suposiciones sobre la implementación interna. Los cambios en un contrato deben considerar su compatibilidad e impacto sobre los componentes que dependan de él, evitando modificaciones que introduzcan incompatibilidades o regresiones de forma innecesaria.

### 10. Documentación de la API

Todos los endpoints expuestos por el backend deben estar documentados mediante OpenAPI/Swagger. La documentación debe describir de forma clara el contrato de cada endpoint, incluyendo sus solicitudes, respuestas, parámetros, errores y requisitos de acceso cuando correspondan. La documentación debe mantenerse sincronizada con la implementación y actualizarse cuando cambie el comportamiento de la API.

### 11. Simplicidad antes que sobreingeniería

Las soluciones deben ser proporcionales a las necesidades reales del sistema. No se debe introducir complejidad, abstracciones, patrones, dependencias o tecnologías que no aporten un beneficio concreto. Se debe evitar diseñar para necesidades hipotéticas o futuras, permitiendo que la arquitectura evolucione cuando exista una necesidad real.

### 12. Cambios controlados

Todo cambio debe estar justificado por una necesidad concreta y limitarse al alcance necesario para resolverla. No deben modificarse arbitrariamente componentes o funcionalidades que no estén relacionados con el objetivo del cambio. Los cambios deben considerar su impacto sobre el sistema existente y preservar el comportamiento previamente validados. Las herramientas automatizadas y los agentes de inteligencia artificial están sujetos a estas mismas reglas y no deben realizar modificaciones fuera del alcance de la tarea sin una justificación explícita.

## Reglas Generales de Aplicación

Estos criterios rigen la aplicación de los principios constitucionales y deben ser respetados por todos los colaboradores del proyecto, incluyendo agentes de inteligencia artificial.

- Los principios deben aplicarse tanto al código desarrollado manualmente como al código generado o modificado mediante agentes de inteligencia artificial.

- La Constitución debe ser clara, precisa y suficientemente explícita para que pueda ser utilizada como criterio obligatorio durante el desarrollo.

- No deben introducirse decisiones tecnológicas, arquitectónicas o de implementación que no estén expresamente definidas en esta Constitución.

- Las decisiones concretas de arquitectura e implementación deberán definirse posteriormente durante las etapas de especificación y planificación.

- **Política de Idioma**: El idioma principal de trabajo del proyecto es el español.

  - **Documentación y artefactos de Spec Kit**: Todo el contenido específico del proyecto debe redactarse en español. Esto incluye specifications, clarifications, plans, tasks, análisis, checklists y cualquier otro artefacto generado o mantenido mediante Spec Kit.

  - **Estructura y terminología de Spec Kit**: La estructura, encabezados, palabras clave, identificadores y convenciones propias de los templates de Spec Kit deben conservarse en inglés y en su formato original. Esto incluye, entre otros, términos como `Feature Specification`, `User Story`, `Priority`, `Why this priority`, `Independent Test`, `Acceptance Scenarios`, `Given`, `When`, `Then`, `Edge Cases`, `Requirements`, `Functional Requirements`, `Key Entities`, `Success Criteria`, `Measurable Outcomes`, `Assumptions`, `Out of Scope`, `Implementation Plan`, `Technical Context`, `Constitution Check`, `Project Structure`, `Tasks`, `Phase`, `Goal`, `Checkpoint`, `Dependencies & Execution Order`, `Implementation Strategy`, `Notes` y `Checklist`.

  - **Convenciones normativas de los templates**: Las palabras clave y expresiones normativas propias de los templates deben conservarse en inglés. Esto incluye expresiones como `System MUST`, `MUST`, `MUST NOT`, `Given`, `When` y `Then`. El contenido específico que acompaña estas expresiones debe redactarse en español.

  - **Identificadores de Spec Kit**: Los identificadores y convenciones como `FR-001`, `FR-002`, `SC-001`, `SC-002`, `T001`, `T002`, `US1`, `US2`, `P1`, `P2` y `[P]` deben conservarse en su formato original.

  - **Código fuente**: Los elementos definidos por el equipo, incluyendo clases, métodos, variables, atributos, archivos, paquetes o módulos, entidades, servicios, repositorios, controladores y otros identificadores propios del proyecto deben utilizar español, salvo que exista una restricción técnica o de compatibilidad que requiera mantener la denominación original.

  - **Endpoints**: Los endpoints propios de la API del proyecto deben utilizar español en sus recursos y rutas, salvo que exista una restricción técnica o de compatibilidad que requiera mantener una denominación original.

  - **Pruebas y comentarios**: Las pruebas propias del proyecto, sus nombres, descripciones, datos descriptivos y comentarios deben redactarse en español. Las convenciones, APIs y elementos propios de los frameworks o herramientas de testing deben conservar su denominación original.

  - **Mensajes y commits**: Los mensajes propios de la aplicación y los commits deben redactarse en español.

  - **Excepciones técnicas**: Los nombres oficiales de tecnologías, lenguajes de programación, frameworks, librerías, APIs, servicios externos, herramientas, comandos, protocolos, estándares, productos, marcas y términos técnicos oficiales deben conservarse en su denominación original cuando corresponda. Por ejemplo: Java, Gradle, Spring Boot, Spring Security, JUnit, Mockito, PostgreSQL, OpenAPI, Swagger, Git, GitHub, HTTPS/TLS, BCrypt y Argon2.

  - **Agentes de IA**: Todos los agentes de inteligencia artificial utilizados en el proyecto deben respetar esta política al generar, modificar, revisar o analizar cualquier artefacto. Los agentes deben mantener en inglés la estructura, keywords, identificadores y convenciones requeridas por Spec Kit o por las tecnologías utilizadas, y redactar en español el contenido específico del proyecto.

## Evolución y Conflictos

La Constitución establece reglas generales y permanentes. Su evolución debe ser un proceso controlado para garantizar la estabilidad del proyecto.

- Si una futura especificación o decisión entra en conflicto con un principio de esta Constitución, el conflicto debe identificarse explícitamente y la Constitución deberá ser revisada de forma controlada antes de ignorar o incumplir dicho principio.

- Los agentes de inteligencia artificial deben respetar estos principios y no deben modificarlos por iniciativa propia.

## Governance

La Constitución es el documento de máxima jerarquía técnica del proyecto y rige todas las decisiones posteriores.

- **Procedimiento de Enmienda**: Cualquier cambio en esta Constitución debe estar justificado por una necesidad concreta, documentado mediante un incremento de versión y seguir el proceso de revisión del equipo.

- **Cumplimiento de IA**: Los agentes de IA tienen estrictamente prohibido modificar o ignorar estos principios por iniciativa propia. Deben validar cada propuesta contra esta Constitución.

- **Política de Versionado**: Se utiliza Versionado Semántico (SemVer). Los cambios mayores (MAJOR) implican cambios en la gobernanza o eliminación de principios. Los cambios menores (MINOR) añaden o expanden principios. Los parches (PATCH) son clarificaciones no semánticas.

**Version**: 1.2.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-01
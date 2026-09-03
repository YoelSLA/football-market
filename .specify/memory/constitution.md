<!--
Sync Impact Report:
- Version change: 1.2.0 -> 1.3.0
- List of modified principles:
  - None
- Added sections:
  - Politica de Idioma
- Modified sections:
  - None
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

### 13. Idioma y convenciones lingüísticas

El idioma principal y obligatorio de trabajo del proyecto es el español. Todo contenido específico del proyecto generado, modificado, revisado o analizado por personas o agentes de inteligencia artificial debe redactarse en español, salvo las excepciones establecidas explícitamente en este principio.

Los artefactos de Spec Kit deben conservar en inglés únicamente la estructura, los encabezados, las palabras clave, los identificadores y las convenciones que formen parte de la especificación original de Spec Kit. El contenido específico del proyecto que acompañe dichos elementos debe redactarse en español.

Las expresiones normativas y estructuras propias de Spec Kit, como `System MUST`, `MUST`, `MUST NOT`, `Given`, `When` y `Then`, deben conservarse en inglés cuando formen parte del formato requerido por Spec Kit. El texto descriptivo asociado debe redactarse en español.

Los identificadores propios de Spec Kit, como `FR-001`, `SC-001`, `T001`, `US1`, `P1` y `[P]`, deben conservarse en su formato original.

Los elementos definidos específicamente para el proyecto, incluyendo clases, métodos, variables, atributos, archivos, paquetes, módulos, entidades, servicios, repositorios, controladores, pruebas, comentarios, mensajes de la aplicación, endpoints y commits, deben utilizar español, salvo que exista una restricción técnica, de compatibilidad o una convención oficial que requiera mantener otra denominación.

Los nombres oficiales de lenguajes, frameworks, librerías, APIs, servicios externos, herramientas, comandos, protocolos, estándares, productos, marcas y tecnologías deben conservarse en su denominación oficial. No deben traducirse ni modificarse para cumplir esta política.

Los agentes de inteligencia artificial deben aplicar este principio a todos los artefactos que generen o modifiquen. Ante cualquier conflicto entre una convención lingüística del proyecto y una convención obligatoria de una tecnología o de Spec Kit, debe conservarse la denominación oficial de la tecnología o convención y redactarse en español el contenido específico del proyecto.

## Reglas Generales de Aplicación

Estos criterios rigen la aplicación de los principios constitucionales y deben ser respetados por todos los colaboradores del proyecto, incluyendo agentes de inteligencia artificial.

- Los principios deben aplicarse tanto al código desarrollado manualmente como al código generado o modificado mediante agentes de inteligencia artificial.

- La Constitución debe ser clara, precisa y suficientemente explícita para que pueda ser utilizada como criterio obligatorio durante el desarrollo.

- No deben introducirse decisiones tecnológicas, arquitectónicas o de implementación que no estén expresamente definidas en esta Constitución.

- Las decisiones concretas de arquitectura e implementación deberán definirse posteriormente durante las etapas de especificación y planificación.

## Stack Tecnológico

El proyecto utiliza el siguiente stack tecnológico. Las implementaciones y decisiones de desarrollo deben respetar estas tecnologías y versiones, salvo que una modificación del stack sea aprobada mediante el procedimiento de enmienda correspondiente o mediante la decisión de arquitectura establecida por el proyecto.

| **#** |      **Tecnología / Dependencia**      |          **Versión**         | **Uso**                                  |
| :---: | :------------------------------------: | :--------------------------: | :--------------------------------------- |
|   1   |                 Gradle                 |             8.14+            | Build y gestión de dependencias          |
|   2   |                  Java                  |              21              | Lenguaje / JDK                           |
|   3   |               Spring Boot              |             4.1.1            | Framework principal                      |
|   4   |               Spring Web               | Managed by Spring Boot 4.1.1 | Desarrollo de API REST                   |
|   5   |             Spring Security            | Managed by Spring Boot 4.1.1 | Autenticación y autorización             |
|   6   | Spring Security OAuth2 Resource Server | Managed by Spring Boot 4.1.1 | Validación de JWT                        |
|   7   |             Spring Data JPA            | Managed by Spring Boot 4.1.1 | Persistencia mediante JPA                |
|   8   |         Spring Boot Validation         | Managed by Spring Boot 4.1.1 | Validación de DTOs y requests            |
|   9   |           Jakarta Validation           |             3.1.1            | API de validaciones                      |
|   10  |               PostgreSQL               |             18.x             | Base de datos                            |
|   11  |         PostgreSQL JDBC Driver         | Managed by Spring Boot 4.1.1 | Conexión Java ↔ PostgreSQL               |
|   12  |                 Flyway                 | Managed by Spring Boot 4.1.1 | Migraciones y versionado de BD           |
|   13  |            Flyway PostgreSQL           | Managed by Spring Boot 4.1.1 | Soporte de Flyway para PostgreSQL        |
|   14  |     SpringDoc OpenAPI / Swagger UI     |             3.1.0            | Documentación de APIs REST               |
|   15  |            Spring Boot Test            |             4.1.1            | Testing de la aplicación                 |
|   16  |              JUnit Jupiter             | Managed by Spring Boot 4.1.1 | Tests unitarios                          |
|   17  |                 Mockito                | Managed by Spring Boot 4.1.1 | Mocking                                  |
|   18  |                 AssertJ                | Managed by Spring Boot 4.1.1 | Assertions                               |
|   19  |       Spring Boot Testcontainers       |             4.1.1            | Integración Spring Boot + Testcontainers |
|   20  |      Testcontainers JUnit Jupiter      |             2.0.3            | Integración Testcontainers + JUnit       |
|   21  |        Testcontainers PostgreSQL       |             2.0.3            | PostgreSQL para tests de integración     |
|   22  |         SonarQube Gradle Plugin        |          6.0.1.5171          | Análisis estático / calidad de código    |

Las versiones indicadas deben considerarse parte del contexto tecnológico vigente del proyecto. Las dependencias administradas por Spring Boot deben utilizar las versiones gestionadas por la versión de Spring Boot establecida, evitando definir versiones manuales cuando no exista una necesidad técnica justificada.


## Evolución y Conflictos

La Constitución establece reglas generales y permanentes. Su evolución debe ser un proceso controlado para garantizar la estabilidad del proyecto.

- Si una futura especificación o decisión entra en conflicto con un principio de esta Constitución, el conflicto debe identificarse explícitamente y la Constitución deberá ser revisada de forma controlada antes de ignorar o incumplir dicho principio.

- Los agentes de inteligencia artificial deben respetar estos principios y no deben modificarlos por iniciativa propia.

## Governance

La Constitución es el documento de máxima jerarquía técnica del proyecto y rige todas las decisiones posteriores.

- **Procedimiento de Enmienda**: Cualquier cambio en esta Constitución debe estar justificado por una necesidad concreta, documentado mediante un incremento de versión y seguir el proceso de revisión del equipo.

- **Cumplimiento de IA**: Los agentes de IA tienen estrictamente prohibido modificar o ignorar estos principios por iniciativa propia. Deben validar cada propuesta contra esta Constitución.

- **Política de Versionado**: Se utiliza Versionado Semántico (SemVer). Los cambios mayores (MAJOR) implican cambios en la gobernanza o eliminación de principios. Los cambios menores (MINOR) añaden o expanden principios. Los parches (PATCH) son clarificaciones no semánticas.

**Version**: 1.3.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-03
Quiero establecer la Constitución del proyecto Football Market utilizando los siguientes 12 principios como base.

Estos principios fueron definidos y revisados previamente y representan las reglas fundamentales que deben respetarse durante todo el desarrollo del proyecto.

La Constitución debe mantener exactamente estos 12 principios. No agregues nuevos principios, no elimines ninguno y no cambies su intención.

Criterios para la Constitución:

- Cada principio debe mantener una única responsabilidad clara.
- No deben duplicarse conceptos innecesariamente entre principios.
- Los principios deben establecer reglas generales y permanentes del proyecto.
- No deben introducirse decisiones tecnológicas, arquitectónicas o de implementación que no estén expresamente definidas.
- No deben agregarse frameworks, librerías, patrones de diseño, estructuras de carpetas, tecnologías o mecanismos de implementación por iniciativa propia.
- Las decisiones concretas de arquitectura e implementación deberán definirse posteriormente durante las etapas de especificación y planificación.
- La Constitución debe ser clara, precisa y suficientemente explícita para que pueda ser utilizada como criterio obligatorio durante el desarrollo.
- Los principios deben aplicarse tanto al código desarrollado manualmente como al código generado o modificado mediante agentes de inteligencia artificial.
- Los agentes de inteligencia artificial deben respetar estos principios y no deben modificarlos por iniciativa propia.
- Si una futura especificación o decisión entra en conflicto con un principio de esta Constitución, el conflicto debe identificarse explícitamente y la Constitución deberá ser revisada de forma controlada antes de ignorar o incumplir dicho principio.
- No conviertas detalles de implementación en reglas constitucionales.
- No agregues reglas basadas en suposiciones sobre necesidades futuras del proyecto.

Los 12 principios son los siguientes:

### PRINCIPIO 1 — Separación de responsabilidades y modularidad

Cada componente, módulo o parte del sistema debe tener una responsabilidad claramente definida y debe encargarse únicamente de las responsabilidades que le correspondan. Se debe evitar la concentración de responsabilidades, la mezcla de diferentes tipos de lógica y las dependencias innecesarias entre componentes. El sistema debe organizarse de manera modular, favoreciendo la cohesión y el bajo acoplamiento, de forma que las funcionalidades puedan evolucionar sin generar impactos innecesarios en otras partes del sistema. La organización de módulos, componentes y recursos debe responder a responsabilidades claramente identificables y permitir que la arquitectura evolucione conforme cambien las necesidades del proyecto.

### PRINCIPIO 2 — Lógica de negocio

La lógica de negocio debe mantenerse dentro de las responsabilidades propias del dominio y separada de los mecanismos de presentación, persistencia e infraestructura. Las reglas que puedan resolverse utilizando únicamente el estado y comportamiento propio de una entidad o concepto del dominio deben permanecer asociadas a dicho concepto. Las reglas que requieran coordinar información, recursos o dependencias externas deben ubicarse en el componente responsable de dicha coordinación.

### PRINCIPIO 3 — Integración y confiabilidad de fuentes externas

Las APIs, servicios y fuentes de información externas deben mantenerse aisladas de la lógica interna de la aplicación y sus particularidades no deben propagarse innecesariamente por el sistema. La información proveniente de estas fuentes debe considerarse no confiable y validarse antes de ser incorporada. Las integraciones deben contemplar errores, respuestas inválidas, indisponibilidad y cambios inesperados, evitando que estos problemas comprometan el funcionamiento o la integridad de la aplicación. El sistema debe poder modificar o reemplazar una dependencia externa sin generar cambios innecesarios en el resto de la aplicación.

### PRINCIPIO 4 — Testing obligatorio

Todo comportamiento relevante del sistema, ya sea nuevo o modificado, debe estar respaldado por pruebas automatizadas que verifiquen su funcionamiento esperado y los escenarios de error relevantes. Las pruebas deben formar parte del desarrollo de cada funcionalidad y una funcionalidad no se considera terminada mientras las pruebas correspondientes no sean satisfactorias.

### PRINCIPIO 5 — Calidad y estabilidad de las pruebas

Las pruebas deben verificar comportamientos y reglas relevantes del sistema, contemplando escenarios esperados, errores, límites y condiciones relevantes, y deben proporcionar una verificación real y reproducible del comportamiento esperado. No deben implementarse únicamente para aumentar métricas de cobertura. Las pruebas existentes deben mantenerse para proteger los comportamientos previamente validados y evitar regresiones. Cuando un requisito cambie intencionalmente, las pruebas correspondientes podrán actualizarse para reflejar el nuevo comportamiento, pero no deben modificarse para ocultar errores de implementación. Las pruebas de integraciones externas deben poder ejecutarse de forma controlada sin depender de la disponibilidad de terceros cuando dicha disponibilidad no sea parte de lo que se pretende verificar.

### PRINCIPIO 6 — Calidad de código

El código debe ser claro, legible, consistente y mantenible, además de cumplir correctamente con los requisitos funcionales. Se debe evitar la duplicación innecesaria de lógica y mantener criterios consistentes en todo el sistema, evitando implementar de forma independiente las mismas reglas cuando esto pueda generar comportamientos inconsistentes. Las nuevas funcionalidades y modificaciones no deben degradar injustificadamente la calidad existente ni introducir prácticas que dificulten innecesariamente la comprensión y evolución del código.

### PRINCIPIO 7 — Control de calidad automatizado

La calidad del código debe evaluarse de forma continua durante el desarrollo mediante las validaciones establecidas por el proyecto. Los cambios deben superar los controles correspondientes antes de considerarse terminados. Las métricas y herramientas de calidad deben utilizarse para detectar y prevenir problemas, pero no deben convertirse en un objetivo por sí mismas cuando hacerlo implique sacrificar la corrección, claridad o mantenibilidad del código.

### PRINCIPIO 8 — Seguridad

La seguridad debe considerarse un requisito transversal del sistema desde su diseño y durante todo su desarrollo. Los datos, entradas, recursos y operaciones deben tratarse de forma segura, sin asumir como confiable la información proveniente de usuarios, clientes o fuentes externas. Las validaciones o restricciones aplicadas en el cliente no deben considerarse suficientes para garantizar la seguridad. El acceso a funcionalidades y recursos debe estar protegido mediante mecanismos adecuados de autenticación y autorización, verificando que cada usuario tenga los permisos necesarios para realizar una operación o acceder a un recurso. Los secretos y la información sensible deben mantenerse protegidos y no deben exponerse mediante el código, repositorio, logs, respuestas o mensajes de error. El sistema debe aplicar controles adecuados para evitar accesos no autorizados, manipulación de solicitudes y exposición o modificación indebida de información.

### PRINCIPIO 9 — Contratos entre componentes

La comunicación entre las distintas partes del sistema debe basarse en contratos claros, explícitos y consistentes. Los consumidores deben utilizar dichos contratos sin depender de comportamientos implícitos o suposiciones sobre la implementación interna. Los cambios en un contrato deben considerar su compatibilidad e impacto sobre los componentes que dependan de él, evitando modificaciones que introduzcan incompatibilidades o regresiones de forma innecesaria.

### PRINCIPIO 10 — Documentación de la API

Todos los endpoints expuestos por el backend deben estar documentados mediante OpenAPI/Swagger. La documentación debe describir de forma clara el contrato de cada endpoint, incluyendo sus solicitudes, respuestas, parámetros, errores y requisitos de acceso cuando correspondan. La documentación debe mantenerse sincronizada con la implementación y actualizarse cuando cambie el comportamiento de la API.

### PRINCIPIO 11 — Simplicidad antes que sobreingeniería

Las soluciones deben ser proporcionales a las necesidades reales del sistema. No se debe introducir complejidad, abstracciones, patrones, dependencias o tecnologías que no aporten un beneficio concreto. Se debe evitar diseñar para necesidades hipotéticas o futuras, permitiendo que la arquitectura evolucione cuando exista una necesidad real.

### PRINCIPIO 12 — Cambios controlados

Todo cambio debe estar justificado por una necesidad concreta y limitarse al alcance necesario para resolverla. No deben modificarse arbitrariamente componentes o funcionalidades que no estén relacionados con el objetivo del cambio. Los cambios deben considerar su impacto sobre el sistema existente y preservar el comportamiento previamente validado. Las herramientas automatizadas y los agentes de inteligencia artificial están sujetos a estas mismas reglas y no deben realizar modificaciones fuera del alcance de la tarea sin una justificación explícita.

Genera o actualiza el archivo de Constitución correspondiente siguiendo las convenciones de Spec Kit.

Antes de finalizar, verifica que:

1. Los 12 principios estén presentes.
2. No hayas agregado principios adicionales.
3. No hayas eliminado ningún principio.
4. No hayas introducido decisiones tecnológicas o arquitectónicas que no estén definidas en los principios.
5. No existan contradicciones entre los principios.
6. La redacción sea clara y consistente.
7. Los principios mantengan su responsabilidad individual y no se mezclen innecesariamente.


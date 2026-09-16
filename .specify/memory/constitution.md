<!--
Sync Impact Report:

- Version change: 1.10.0 -> 1.11.0

- List of modified principles:
  - IV. Testing y control de calidad automatizado
  - VII. Contratos entre componentes
  - X. Cambios controlados

- Modified sections:
  - Core Principles

- Follow-up TODOs:
  - None
-->

## 1. Arquitectura

### 1.1. Fundamentos generales

Cada componente debe tener una responsabilidad arquitectónica clara y mantener alta cohesión.

Los componentes deben encapsular sus detalles de implementación y exponer únicamente lo necesario mediante contratos.

Un componente debe dividirse únicamente cuando exista una separación real de responsabilidades, falta de cohesión o una necesidad arquitectónica explícita. El tamaño o cantidad de código no justifica por sí solo una división.

El nuevo comportamiento debe incorporarse al componente cuya responsabilidad corresponda.

### 1.2. Arquitectura del backend

El backend se organiza en:

- Presentación: Controller.
- Aplicación: Orchestrator y Service.
- Dominio: Model.
- Persistence: Repository.
- Integración externa: Integration.
- Soporte HTTP: DTO y Mapper.

#### 1.2.1. Controller

Representa la frontera HTTP.

Puede utilizar DTO, Mapper, Service y Orchestrator.

No debe contener lógica de negocio ni acceder directamente a Model, Repository o Integration.

Puede pasar directamente a Service u Orchestrator el Model devuelto por un Mapper, y pasar directamente a un Mapper el Model devuelto por Service u Orchestrator. En ambos casos no debe construirlo, declararlo, modificarlo, inspeccionarlo ni aplicar lógica sobre él.

Cuando una operación HTTP intercambie datos de dominio, el flujo permitido es: DTO → Mapper → Model → Service u Orchestrator → Model → Mapper → DTO.

Se asume que Service u Orchestrator no devuelven `null` cuando el contrato de la operación espera una respuesta.

#### 1.2.2. Orchestrator

Coordina operaciones de aplicación cuando exista una responsabilidad de coordinación que no corresponda a Controller o a un único Service.

Puede utilizar Service y Model.

No debe contener lógica de dominio.

No puede depender de Repository, Integration ni otro Orchestrator.

El uso de múltiples Services no obliga por sí solo a crear un Orchestrator.

#### 1.2.3. Service

Implementa la lógica de aplicación.

Puede utilizar Model, Repository e Integration.

No puede depender de Controller, Orchestrator ni otros Services.

#### 1.2.4. Model

Representa conceptos, estado, comportamiento e invariantes del dominio.

Las reglas que puedan resolverse exclusivamente con información del dominio deben implementarse en Model.

No puede depender de Service, Repository ni Integration.

#### 1.2.5. Repository

Gestiona el acceso a datos persistidos.

No debe contener lógica de negocio.

Puede utilizar Model.

No puede depender de Service ni DTO.

#### 1.2.6. Integration

Puede ser utilizada por Service.

No puede ser utilizada directamente por Controller, Orchestrator, Model ni Repository.

### 1.3. DTO y Mapper

#### 1.3.1. DTO

Los DTO representan los contratos HTTP de entrada y salida.

Service, Model y Repository no deben depender de DTO.

#### 1.3.2. Mapper

Los Mapper transforman exclusivamente entre DTO y Model:

DTO → Model
Model → DTO

No deben contener lógica de negocio, validaciones de negocio, cálculos ni decisiones semánticas.

No pueden depender de Service ni Repository.

### 1.4. Dependencias arquitectónicas

Las dependencias directas permitidas son:

Controller → DTO, Mapper, Service, Orchestrator
Orchestrator → Service, Model
Service → Model, Repository, Integration
Repository → Model

Las relaciones de DTO y Mapper se rigen por la sección 1.3.

Toda dependencia no listada está prohibida.

Las dependencias transitivas no autorizan dependencias directas.

No deben existir dependencias circulares ni utilizarse intermediarios para evadir una restricción arquitectónica.

### 1.5. Reglas de dominio y aplicación

Las reglas que puedan resolverse exclusivamente con información y comportamiento del dominio deben implementarse en Model.

Las reglas que requieran Repository, Integration u otras capacidades externas al dominio deben implementarse en Service.

Las reglas que involucren múltiples Model pueden permanecer en el dominio cuando puedan resolverse completamente mediante comportamiento de dominio.

### 1.6. Evolución y nuevas áreas

La arquitectura definida por esta Constitution prevalece sobre estructuras existentes.

La existencia de clases, paquetes, interfaces o convenciones previas no obliga a conservarlas cuando contradigan la arquitectura.

Las refactorizaciones necesarias para cumplir la arquitectura están permitidas.

Toda nueva área debe definir explícitamente sus responsabilidades, componentes y dependencias.

### 1.7. Resolución arquitectónica

Las dependencias y responsabilidades no deben inferirse por conveniencia técnica.

Cuando una situación relevante no pueda resolverse mediante las reglas existentes, la Spec o el código aplicable, el agente debe solicitar aclaración en lugar de inventar una regla arquitectónica.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.

### 2.2. DTO

Los DTO HTTP deben implementarse como `record`.

Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales.

Los mensajes de validación propios de la aplicación deben estar en español.

La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.

### 2.3. Model

Los Model deben preservar sus invariantes y encapsular su estado.

No deben utilizar `Builder`.

No deben utilizarse `@Setter` en los Model.

Pueden utilizarse mecanismos de Persistence necesarios para representar el Model.

### 2.4. Mapper

Los Mapper deben ser `final`, sin estado mutable, con constructor privado y métodos `static`.

No deben utilizarse instancias de Mapper.

Un Mapper que pueda ser invocado fuera de un endpoint HTTP validado con Jakarta Validation y reciba `null` debe lanzar la excepción de mapeo definida por el proyecto.

No se requiere una validación de `null` específica cuando el Mapper sólo sea alcanzable desde un endpoint HTTP cuya validación estructural ya impida ese caso.

No debe convertir `null` en valores por defecto.

### 2.5. Service

Las interfaces para Service no son obligatorias.

Solo deben utilizarse cuando exista una necesidad técnica o arquitectónica real.

No debe crearse una interfaz únicamente por seguir una convención genérica.

### 2.6. Framework y tecnología

Las tecnologías y frameworks establecidos por el proyecto deben utilizarse respetando la arquitectura.

Las anotaciones, clases base e interfaces proporcionadas por los frameworks no deben utilizarse para introducir responsabilidades o dependencias prohibidas.

## 3. Integraciones externas

### 3.1. Encapsulamiento

Toda comunicación con sistemas externos debe realizarse mediante `Integration`.

`Integration` encapsula:
- comunicación y configuración del proveedor;
- requests y responses externos;
- transformación de datos externos;
- errores técnicos de integración.

`Model` no debe contener detalles de HTTP, clientes, proveedores ni protocolos externos.

### 3.2. Respuestas y errores

Las respuestas externas deben validarse antes de ser utilizadas.

Los errores técnicos de integración deben mantenerse diferenciados de los errores de dominio.

No se deben inventar datos ni modificar silenciosamente el comportamiento funcional ante respuestas inválidas o fallos externos.

### 3.3. Timeouts y reintentos

Las comunicaciones externas deben utilizar timeouts cuando la tecnología lo permita.

Los reintentos no deben implementarse por defecto.

Solo pueden utilizarse cuando la operación sea segura para reintentar y exista un límite explícito que evite efectos duplicados o cargas innecesarias.

### 3.4. Disponibilidad

La indisponibilidad de un sistema externo debe manejarse según los requisitos de la funcionalidad.

No se deben introducir automáticamente cachés, degradaciones ni comportamientos alternativos sin una justificación funcional o técnica.

### 3.5. Contratos y seguridad

Los contratos externos deben mantenerse aislados de los contratos HTTP y de los Model internos.

Los secretos, tokens y credenciales no deben almacenarse en el código ni exponerse en logs o respuestas.

Las comunicaciones externas deben utilizar mecanismos seguros y no deben degradarse para facilitar la implementación.

## 4. Testing

### 4.1. Cobertura de comportamiento

Todo código nuevo o modificado debe contar con tests automatizados suficientes para verificar el comportamiento afectado.

Los tests deben cubrir, cuando corresponda:
- comportamiento esperado;
- errores y validaciones;
- límites y estados inválidos;
- regresiones relevantes.

Los tests deben verificar comportamiento, no únicamente que se invoquen métodos.

### 4.2. Tipo de test

La elección del test debe corresponder a la responsabilidad del componente:

- `Model` → reglas e invariantes de dominio.
- `Service` → lógica de aplicación y uso de Repository/Integration.
- `Orchestrator` → coordinación.
- `Controller` → contrato HTTP.
- `Mapper` → transformaciones DTO ↔ Model y manejo de errores, cuando su comportamiento no esté cubierto por el flujo HTTP validado.
- `Repository` → persistencia real.
- `Integration` → comunicación externa, respuestas y errores.

Los tests unitarios deben aislar dependencias externas.

Los tests de integración deben utilizarse cuando sea necesario verificar persistencia, comunicación externa, serialización, configuración o contratos reales.

### 4.3. Calidad de los tests

Los tests deben ser:
- deterministas;
- independientes entre sí;
- reproducibles;
- descriptivos respecto del comportamiento probado.

Los nombres de los tests pueden estar en español, de acuerdo con el Principio 9.

### 4.4. Regresiones y controles

Las correcciones de defectos deben incorporar un test de regresión cuando sea razonable.

Antes de finalizar un cambio deben ejecutarse los controles aplicables del proyecto, incluyendo tests, compilación, análisis estático y Quality Gates cuando correspondan.

Los controles aplicables son los disponibles o configurados en el proyecto y aquellos requeridos por el área modificada. Para considerar correcto un cambio, todos los controles aplicables deben aprobarse.

No se deben modificar tests ni controles únicamente para ocultar un fallo introducido por el cambio.

Si un control falla por una causa preexistente y verificablemente ajena al cambio, debe documentarse y reportarse. Ese fallo no bloquea la finalización del cambio ni habilita ampliar su alcance para corregirlo.

## 5. Calidad y mantenibilidad

### 5.1. Simplicidad

El código debe ser claro, mantenible y simple.

Debe preferirse la solución válida más simple, evitando complejidad, dependencias y abstracciones innecesarias.

No deben introducirse patrones, interfaces o capas únicamente por seguir convenciones genéricas.

### 5.2. Responsabilidades

Cada clase y método debe mantener una responsabilidad coherente.

No se debe fragmentar código trivial sin una razón arquitectónica o de mantenibilidad.

La duplicación debe evaluarse antes de introducir una abstracción. No toda duplicación requiere ser eliminada.

### 5.3. Dependencias

No debe agregarse una dependencia externa cuando las capacidades existentes del proyecto sean suficientes.

Las nuevas dependencias deben tener una justificación técnica real.

### 5.4. Código innecesario

Debe eliminarse código muerto, obsoleto o innecesario cuando forme parte del alcance del cambio.

Los comentarios deben aportar información que no resulte evidente del código y mantenerse actualizados.

### 5.5. Refactorización

El código existente no debe modificarse sin una razón concreta.

Las refactorizaciones deben:
- aportar un beneficio técnico, arquitectónico o de mantenibilidad;
- mantenerse dentro de un alcance razonable;
- preservar el comportamiento no relacionado con el cambio.

Las necesidades hipotéticas futuras no justifican complejidad adicional.

## 6. Seguridad

### 6.1. Principios generales

La seguridad es obligatoria en todo cambio.

Toda entrada externa debe considerarse no confiable y validarse según el contrato, las reglas de seguridad y los requisitos funcionales.

No se deben deshabilitar mecanismos de seguridad para simplificar una implementación.

### 6.2. Autenticación y autorización

La autenticación debe utilizar el mecanismo definido por el proyecto.

La autorización debe verificarse de forma explícita e independiente de la autenticación.

Cuando corresponda, debe verificarse el rol, permiso o pertenencia del usuario al recurso solicitado.

### 6.3. Datos y ataques

Las consultas y operaciones que involucren datos externos deben utilizar mecanismos seguros de parametrización, binding o escaping.

No se debe concatenar directamente entrada externa en consultas u operaciones cuando exista un mecanismo seguro equivalente.

No se debe implementar criptografía propia. Deben utilizarse algoritmos y librerías estándar y seguros.

### 6.4. Secretos y datos sensibles

Los secretos, credenciales y tokens deben mantenerse fuera del código fuente y del repositorio.

No deben exponerse datos sensibles en:
- respuestas HTTP;
- logs;
- mensajes de error;
- configuraciones versionadas.

### 6.5. Comunicaciones y dependencias

Las comunicaciones externas deben utilizar mecanismos seguros, como HTTPS cuando corresponda.

Las dependencias con vulnerabilidades relevantes deben corregirse cuando sean introducidas o agravadas por el cambio.

Debe aplicarse el principio de mínimo privilegio y utilizar configuraciones seguras por defecto.

### 6.6. Testing

Los cambios que afecten seguridad deben incluir tests que verifiquen los comportamientos de seguridad relevantes.

## 7. Contratos

### 7.1. Contratos explícitos

Los componentes deben interactuar mediante contratos definidos.

Los consumidores deben depender del contrato y no de detalles internos de implementación.

Las modificaciones que alteren el comportamiento observable deben considerarse cambios de contrato.

### 7.2. Contrato HTTP

El contrato HTTP comprende, cuando corresponda:

- métodos y rutas;
- parámetros y headers;
- Request y Response DTO;
- códigos de estado;
- validaciones;
- estructura de errores.

Los DTO representan el contrato HTTP y no deben exponer directamente los Model internos.

Todo endpoint debe devolver un código de estado HTTP. El body es opcional; si una respuesta exitosa devuelve datos en el body, debe utilizar un Response DTO.

### 7.3. Compatibilidad

Antes de modificar un contrato debe evaluarse su impacto sobre los consumidores existentes.

No deben introducirse cambios incompatibles de forma silenciosa.

Agregar, eliminar, modificar obligatoriedad, cambiar significado o alterar errores puede constituir un cambio incompatible.

### 7.4. Validación

La validación debe realizarse en el componente responsable:

- Validación estructural HTTP → Controller / DTO.
- Validación de negocio → Service / Model.
- Validación de contratos externos → Integration.

Las excepciones de validación estructural HTTP lanzadas por Jakarta Validation deben capturarse desde el manejo de errores de presentación y devolverse mediante una respuesta HTTP uniforme cuyo body contenga únicamente el mensaje `String` definido por la excepción.

Cuando existan múltiples errores de validación, debe utilizarse el primer mensaje disponible como body de la respuesta.

Los contratos externos deben mantenerse aislados de los contratos HTTP e internos.

### 7.5. Evolución

Los contratos relevantes deben contar con tests que permitan detectar cambios incompatibles.

Todo cambio de contrato debe actualizar los elementos afectados, como implementación, tests, DTO, documentación y OpenAPI cuando corresponda.

Si no se conocen los consumidores de un contrato existente, debe tratarse como potencialmente utilizado.

## 8. Documentación

### 8.1. API HTTP

Los endpoints HTTP funcionales deben estar documentados mediante OpenAPI.

La documentación debe reflejar fielmente:
- operación y propósito;
- parámetros;
- Request y Response DTO;
- validaciones relevantes;
- códigos de respuesta contractuales;
- errores esperados;
- seguridad cuando corresponda.

Las anotaciones deben utilizarse de forma consistente con las convenciones definidas por el proyecto.

### 8.2. DTO

Los campos de los DTO que formen parte del contrato deben documentar su significado y, cuando sea relevante, ejemplos y restricciones.

La documentación debe mantenerse consistente con el comportamiento real de la API.

### 8.3. Javadoc

Los métodos y constructores públicos deben tener Javadoc cuando aporten información relevante sobre su contrato o comportamiento.

Los métodos sobrescritos no requieren repetir documentación heredada salvo que agreguen comportamiento relevante.

La documentación debe explicar comportamiento, condiciones, parámetros, retornos o excepciones que no sean evidentes del código.

### 8.4. Consistencia

La documentación del proyecto debe estar escrita en español.

OpenAPI, Javadoc e implementación deben mantenerse coherentes entre sí y con la Spec correspondiente.

## 9. Idioma

### 9.1. Código

Todo código nuevo o modificado debe utilizar inglés para:
- clases;
- métodos;
- variables;
- atributos;
- paquetes;
- endpoints;
- campos JSON.

Los nombres impuestos por frameworks, librerías o sistemas externos pueden mantenerse según su contrato.

### 9.2. Tests

Los nombres descriptivos de los tests pueden estar en español.

El resto del código de los tests debe respetar las convenciones generales del proyecto.

### 9.3. Documentación

La documentación propia del proyecto debe estar en español.

Esto incluye:
- Constitution;
- Specs;
- documentación técnica;
- Javadoc;
- OpenAPI;
- mensajes de validación.

La documentación externa puede mantenerse en su idioma original.

### 9.4. Git

Los nombres de ramas, commits, Pull Requests e Issues deben estar en inglés.

### 9.5. Código existente

El código existente en español no debe modificarse únicamente para traducirlo.

Cuando un archivo existente sea modificado como parte del alcance, los elementos nuevos o modificados deben respetar las convenciones de idioma establecidas.

## 10. Cambios controlados

### 10.1. Alcance

Los cambios deben limitarse a lo necesario para:
- completar la tarea;
- cumplir la Constitution;
- mantener la coherencia de los elementos directamente afectados.

No deben realizarse refactorizaciones, mejoras u optimizaciones no relacionadas.

### 10.2. Archivos y componentes

Se pueden crear, modificar, eliminar o renombrar archivos y componentes cuando sean necesarios para el cambio.

Los elementos no relacionados deben permanecer sin modificaciones.

Antes de eliminar o renombrar elementos deben considerarse sus referencias y consumidores.

### 10.3. Comportamiento existente

Debe preservarse el comportamiento no relacionado con la tarea.

Si el cambio modifica comportamiento funcional, deben actualizarse los elementos afectados, incluyendo cuando corresponda:
- implementación;
- tests;
- contratos;
- DTO;
- OpenAPI;
- documentación;
- configuración.

### 10.4. Tests existentes

No deben modificarse tests únicamente para hacer que pasen.

Si cambia el comportamiento esperado, los tests afectados deben actualizarse para representar el nuevo contrato.

### 10.5. Problemas preexistentes

Las violaciones o fallos preexistentes y no relacionados con el cambio no requieren una corrección fuera de alcance.

No deben ocultarse, ignorarse ni utilizarse para justificar modificaciones innecesarias.

### 10.6. Constitution

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution durante una tarea.

Si una modificación necesaria entra en conflicto con la Constitution, debe proponerse una enmienda antes de realizar el cambio incompatible.

### 10.7. Finalización

Un cambio se considera terminado cuando los elementos directamente afectados son coherentes y los controles aplicables definidos en la sección 4.4 se ejecutan y aprueban, salvo la excepción de fallo preexistente definida en esa misma sección.

Cualquier modificación adicional fuera del alcance inicial debe estar justificada explícitamente.

## Technology Constraints

El proyecto utiliza:

- Java 21.
- Gradle 8.14+.
- Spring Boot 4.1.1.
- Spring Web.
- Spring Security.
- OAuth2 Resource Server.
- Spring Data JPA.
- PostgreSQL 18.x.
- JDBC.
- Flyway.
- Jakarta Validation 3.1.1.
- SpringDoc OpenAPI 3.1.0.
- Spring Boot Test.
- JUnit.
- Mockito.
- AssertJ.
- Testcontainers.
- SonarQube.

Las versiones administradas por Spring Boot deben utilizarse sin sobrescribirlas salvo necesidad técnica justificada.

No deben incorporarse nuevas tecnologías o dependencias fuera de este stack sin justificación y aprobación mediante el proceso de cambios correspondiente.

Las Specs y Plans no deben repetir estas restricciones globales salvo que una funcionalidad requiera una consideración técnica específica.

**Version**: 1.11.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-10

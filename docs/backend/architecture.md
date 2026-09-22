# Arquitectura del backend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md). Leer §1 y las convenciones pertinentes de §2; para integraciones externas, también §3. Para conocer o modificar el stack, las versiones o las dependencias del backend, consultar [tecnologías del backend](technologies.md). No es necesario leer ese documento ni el área ajena cuando la tarea no los afecte.

## 1. Arquitectura

### 1.1. Roles del backend

| Rol | Responsabilidad y restricciones propias |
| --- | --- |
| Controller | Frontera HTTP, sin lógica de negocio. Respecto de Model, solo puede transportar directamente el resultado de un Mapper hacia el rol permitido por la matriz y el resultado correspondiente hacia un Mapper; no puede declarar, inspeccionar, modificar ni aplicar lógica sobre Model. |
| Orchestrator | Coordinación entre Services, sin lógica de dominio ni lógica de aplicación propia que corresponda a un Service. Se crea únicamente cuando existe una responsabilidad real de coordinación; utilizar múltiples Services no obliga a crearlo. Puede transportar de forma opaca un Model producido por un Service hacia otro Service y devolver directamente un Model producido por un Service como resultado de la coordinación. No puede construir, modificar, transformar ni inspeccionar el Model, usarlo para tomar decisiones o realizar cálculos, ni ejecutar comportamiento de dominio sobre él. Tampoco depende de Repository, Integration u otro Orchestrator. Ubicación exclusiva: `footballmarket.orchestrators`, nunca dentro de features u otros roles. |
| Service | Lógica de aplicación. Interfaz e implementación separadas obligatorias incluso con una sola implementación; consumidores dependen de la interfaz. Esta regla específica prevalece sobre evitar interfaces innecesarias. |
| Model | Conceptos, estado, comportamiento e invariantes del dominio. |
| Repository | Acceso a persistencia, sin negocio. Utiliza normalmente interfaces Spring Data que trabajan con Model persistibles. `@Repository` se permite solo en interfaces Spring Data, nunca en puertos, implementaciones manuales o repositorios de test. |
| Integration | Adaptación de sistemas externos, detallada en §3; solo Service puede consumirla entre los roles enumerados. |

En HTTP con datos de dominio, el flujo sin Orchestrator es `DTO → Mapper → Model → Service → Model → Mapper → DTO`. Cuando la coordinación requiere un Orchestrator, este puede insertar pasos de coordinación entre Services: recibe de un Service un Model, lo transporta directamente a otro Service y puede devolver al Controller el Model producido por un Service para que el Mapper lo convierta a DTO. El Controller se limita al transporte directo entre los pasos permitidos y no trabaja con Model. Ni Service ni Orchestrator devuelven `null` cuando el contrato espera respuesta.

### 1.2. DTO y Mapper del backend

DTO representa exclusivamente entrada/salida HTTP; Service, Model y Repository no dependen de DTO. Mapper transforma únicamente `DTO ↔ Model`, sin negocio, validaciones de negocio, cálculos ni decisiones semánticas; no depende de Service ni Repository. Los contratos de sistemas externos se rigen por §3.

### 1.3. Dependencias del backend

```text
Controller → DTO, Mapper, Service, Orchestrator
Orchestrator → Service, Model
Service → Model, Repository, Integration
Repository → Model
```

DTO y Mapper se rigen por §1.2. Model no depende de Service, Repository ni Integration. Controller no depende de Model, Repository o Integration. La dependencia `Orchestrator → Model` se permite exclusivamente para transportar de forma opaca entre Services un Model producido por uno de ellos o para devolver un Model producido por un Service como resultado de la coordinación; no autoriza ninguna lógica sobre el Model. Orchestrator no depende de Repository, Integration ni otro Orchestrator. Integration solo puede ser consumida por Service entre los roles enumerados.

Estas matrices tienen el alcance definido en la [arquitectura común](../architecture.md) §1: regulan relaciones entre roles arquitectónicos, no todos los tipos que un componente puede utilizar. Ante una decisión arquitectónica no definida por la documentación vigente, aplicar su regla de vacíos normativos y la gobernanza de la constitución antes de introducirla.

### 1.4. Dominio y aplicación del backend

Las reglas resolubles exclusivamente con información y comportamiento de dominio pertenecen a Model, incluso si involucran varios Model. Las que requieren Repository, Integration u otras capacidades externas al dominio pertenecen a Service.

## 2. Convenciones

### 2.1. DTO del backend

Todos los DTO del contrato HTTP, tanto Request como Response, deben implementarse como `record`.

Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales.

La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.

### 2.2. Model del backend

Los Model deben preservar sus invariantes y encapsular su estado.

No se impone mutabilidad ni inmutabilidad general a los Model. Cuando necesiten modificar su estado como parte del comportamiento de dominio, deben hacerlo mediante operaciones explícitas que preserven sus invariantes.

No deben utilizar `Builder`.

No deben utilizarse `@Setter` en los Model.

Pueden utilizar directamente los mecanismos de persistencia/JPA necesarios para su representación; no se exige separar entidades JPA del Model de dominio.

### 2.3. Mapper del backend

Los Mapper deben ser `final`, sin estado mutable, con constructor privado y métodos `static`.

No deben ser componentes Spring ni utilizarse mediante instancias.

Un Mapper que pueda ser invocado fuera de un endpoint HTTP validado con Jakarta Validation y reciba `null` debe lanzar la excepción de mapeo definida por el proyecto.

No se requiere una validación de `null` específica cuando el Mapper sólo sea alcanzable desde un endpoint HTTP cuya validación estructural ya impida ese caso.

No debe convertir `null` en valores por defecto.

### 2.4. Excepciones del backend

El código de la aplicación no debe lanzar directamente excepciones genéricas provistas por Java, como `RuntimeException`, `IllegalArgumentException` o `IllegalStateException`, para representar errores funcionales, de aplicación o de dominio.

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante `DomainException`, `ApplicationException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe heredar de la categoría correspondiente. `ApplicationException` representa errores controlados propios de la aplicación o de sus casos de uso que no correspondan al dominio, la integración o la persistencia.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta regla. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el funcionamiento interno de esas tecnologías.

### 2.5. Referencias a miembros de instancia en Java

Toda clase productiva debe utilizar `this` al acceder a sus atributos de instancia y al invocar sus propios métodos de instancia, incluso cuando no exista ambigüedad. La regla se aplica también a métodos triviales y al código de los `record` definido explícitamente por el proyecto.

En el código de tests está prohibido usar `this`, incluidas las referencias calificadas como `OuterTest.this`; acceder a los miembros directamente, según [testing](testing.md) §16.

No aplica a miembros `static`, variables locales, parámetros que no sean miembros de la instancia ni código generado automáticamente por Java, Lombok, frameworks u otras herramientas.

### 2.6. Contrato HTTP del backend

- Toda ruta de Controller comienza con el prefijo exacto `/api/`; `/api` y prefijos como `/apiv2` no son válidos.
- El body es opcional. Si una respuesta exitosa contiene datos, utilizar Response DTO, sin exponer directamente Model internos.
- Validación estructural HTTP en Controller/DTO; negocio en Service/Model; contratos externos en Integration.
- Capturar errores estructurales de Jakarta Validation en presentación. Todo error HTTP controlado por la aplicación utiliza `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`; `status` coincide con el código HTTP y `path` identifica la ruta solicitada. Ante múltiples errores de validación, `message` contiene el primer mensaje disponible.

### 2.7. Documentación del backend

Todo endpoint funcional nuevo o modificado debe documentarse en OpenAPI e incorporarse o actualizarse en la colección Postman versionada dentro de la misma tarea. Ambos deben reflejar propósito, método, ruta, parámetros, headers, autenticación, DTO, validaciones, códigos y errores aplicables. Los ejemplos deben ser utilizables sin credenciales reales. Documentar significado de campos DTO y, cuando aporte valor, ejemplos y restricciones. Spring REST Docs se rige por [testing](testing.md) §8.

Javadoc obligatorio en clases públicas. En métodos productivos, incluidos los `public` y `protected`, y en constructores explícitos, es obligatorio cuando sea necesario para documentar propósito, contrato, comportamiento, parámetros, retorno, excepciones, precondiciones o cualquier aspecto no evidente por sí mismo. No se exige en métodos triviales o evidentes ni debe repetir redundantemente el código. Los métodos sobrescritos conservan documentación accesible; `{@inheritDoc}` basta si cubre el contrato completo.

El Javadoc asociado a una declaración debe ubicarse siempre antes de todas las anotaciones de esa declaración.

En tests, métodos de prueba y ciclo de vida no requieren Javadoc; helpers solo cuando necesitan explicación. Modificar el cuerpo de un método exceptuado no lo obliga a tener Javadoc.

## 3. Integraciones externas del backend

Toda comunicación externa pasa por Integration, que encapsula configuración, comunicación, contratos externos, transformación y errores técnicos. Model no conoce proveedores, clientes, HTTP ni protocolos. Los contratos externos permanecen separados de los DTO HTTP propios y de los Model internos; estos tipos no se reutilizan como contratos externos aunque coincidan estructuralmente.

Validar respuestas externas y distinguir errores técnicos de integración de errores de dominio. No inventar datos ni alterar silenciosamente el comportamiento ante fallos. Usar timeouts cuando la tecnología lo permita; retries solo con operación segura y límite explícito, nunca por defecto.

Resolver indisponibilidad según requisitos funcionales; no introducir automáticamente cachés, degradaciones o alternativas sin justificación funcional o técnica. Aplican las reglas globales de seguridad de la constitución.

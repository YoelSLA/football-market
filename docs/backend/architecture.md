# Arquitectura del backend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md). Leer §1 y, para integraciones externas, §2. Las reglas de naming, formato y estilo de implementación están en [convenciones del backend](conventions.md); el stack, versiones y dependencias, en [tecnologías del backend](technologies.md). Consultar estos documentos según el área afectada.

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

| Origen | Destinos permitidos |
| --- | --- |
| Controller | DTO, Mapper, Service, Orchestrator. |
| Orchestrator | Service, Model. |
| Service | Model, Repository, Integration. |
| Repository | Model. |

DTO y Mapper se rigen por §1.2. Model no depende de Service, Repository ni Integration. Controller no depende de Model, Repository o Integration. La dependencia `Orchestrator → Model` se permite exclusivamente para transportar de forma opaca entre Services un Model producido por uno de ellos o para devolver un Model producido por un Service como resultado de la coordinación; no autoriza ninguna lógica sobre el Model. Orchestrator no depende de Repository, Integration ni otro Orchestrator. Integration solo puede ser consumida por Service entre los roles enumerados.

Estas matrices tienen el alcance definido en la [arquitectura común](../architecture.md) §1: regulan relaciones entre roles arquitectónicos, no todos los tipos que un componente puede utilizar. Ante una decisión arquitectónica no definida por la documentación vigente, aplicar su regla de vacíos normativos y la gobernanza de la constitución antes de introducirla.

### 1.4. Dominio y aplicación del backend

Las reglas resolubles exclusivamente con información y comportamiento de dominio pertenecen a Model, incluso si involucran varios Model. Las que requieren Repository, Integration u otras capacidades externas al dominio pertenecen a Service.

### 1.5. Model del backend

Los Model deben preservar sus invariantes y encapsular su estado.

No se impone mutabilidad ni inmutabilidad general a los Model. Cuando necesiten modificar su estado como parte del comportamiento de dominio, deben hacerlo mediante operaciones explícitas que preserven sus invariantes.

Pueden utilizar directamente los mecanismos de persistencia/JPA necesarios para su representación; no se exige separar entidades JPA del Model de dominio.

### 1.6. Excepciones del backend

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante `DomainException`, `ApplicationException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe heredar de la categoría correspondiente. `ApplicationException` representa errores controlados propios de la aplicación o de sus casos de uso que no correspondan al dominio, la integración o la persistencia.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta jerarquía. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el funcionamiento interno de esas tecnologías.

### 1.7. Contrato HTTP del backend

- Toda ruta de Controller comienza con el prefijo exacto `/api/`; `/api` y prefijos como `/apiv2` no son válidos.
- El body es opcional. Si una respuesta exitosa contiene datos, utilizar Response DTO, sin exponer directamente Model internos.
- Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales. La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.
- Validación estructural HTTP en Controller/DTO; negocio en Service/Model; contratos externos en Integration.
- Capturar errores estructurales de Jakarta Validation en presentación. Todo error HTTP controlado por la aplicación utiliza `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`; `status` coincide con el código HTTP, `code` identifica establemente el tipo de error y `path` identifica la ruta solicitada. `message` es legible y no sirve como identificador programático. Ante múltiples errores de validación, `message` contiene el primer mensaje disponible.

## 2. Integraciones externas del backend

Toda comunicación externa pasa por Integration, que encapsula configuración, comunicación, contratos externos, transformación y errores técnicos. Model no conoce proveedores, clientes, HTTP ni protocolos. Los contratos externos permanecen separados de los DTO HTTP propios y de los Model internos; estos tipos no se reutilizan como contratos externos aunque coincidan estructuralmente.

Validar respuestas externas y distinguir errores técnicos de integración de errores de dominio. No inventar datos ni alterar silenciosamente el comportamiento ante fallos. Usar timeouts cuando la tecnología lo permita; retries solo con operación segura y límite explícito, nunca por defecto.

Resolver indisponibilidad según requisitos funcionales; no introducir automáticamente cachés, degradaciones o alternativas sin justificación funcional o técnica. Aplican las reglas globales de seguridad de la constitución.

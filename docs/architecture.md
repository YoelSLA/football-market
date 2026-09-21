# Arquitectura

## Guía de lectura

Consultar §1.1 en toda tarea arquitectónica. Para backend: §1.2–1.5 y las convenciones pertinentes de §2; para integraciones externas, también §3. Para frontend: §1.6. El stack de cada área está en §4. No es necesario leer el área ajena a la tarea.

## 1. Arquitectura

### 1.1. Reglas comunes y alcance de matrices

Crear o dividir componentes solo por responsabilidades reales, cohesión o necesidad arquitectónica, nunca por cantidad de líneas o archivos. Las estructuras enumeradas no obligan a crear carpetas o roles vacíos. Incorporar comportamiento al responsable correspondiente y exponer solo contratos necesarios.

Las matrices regulan dependencias entre roles arquitectónicos del proyecto, no todos los tipos utilizados. Java/TypeScript, colecciones, value types y colaboradores técnicos como `List`, `Clock`, `PasswordEncoder`, `JwtEncoder` o configuración pueden utilizarse cuando sean necesarios y compatibles con la responsabilidad del componente. Su ausencia en la matriz no justifica prohibirlos o eliminarlos.

Entre los roles regulados, toda dependencia no autorizada está prohibida. Ni colaboradores técnicos, frameworks, dependencias transitivas ni intermediarios permiten eludir restricciones expresas. No se permiten ciclos.

La arquitectura prevalece sobre estructuras existentes: refactorizar incompatibilidades dentro del alcance, sin conservarlas por costumbre. Toda nueva área debe definir responsabilidades, componentes y dependencias. Los vacíos normativos se resuelven según la gobernanza de la constitución.

### 1.2. Roles del backend

| Rol | Responsabilidad y restricciones propias |
| --- | --- |
| Controller | Frontera HTTP, sin lógica de negocio. Solo puede pasar directamente el Model de un Mapper a Service/Orchestrator, o el de estos a Mapper; no puede construirlo, declararlo, inspeccionarlo, modificarlo ni aplicar lógica sobre él. |
| Orchestrator | Coordinación que no corresponde al Controller o a un único Service, sin lógica de dominio. Usar múltiples Services no obliga a crearlo. Ubicación exclusiva: `footballmarket.orchestrators`, nunca dentro de features u otros roles. |
| Service | Lógica de aplicación. Interfaz e implementación separadas obligatorias incluso con una sola implementación; consumidores dependen de la interfaz. Esta regla específica prevalece sobre evitar interfaces innecesarias. |
| Model | Conceptos, estado, comportamiento e invariantes del dominio. |
| Repository | Acceso a persistencia, sin negocio. `@Repository` se permite solo en interfaces Spring Data, nunca en puertos, implementaciones manuales o repositorios de test. |
| Integration | Adaptación de sistemas externos, detallada en §3; solo Service puede consumirla entre los roles enumerados. |

En HTTP con datos de dominio: `DTO → Mapper → Model → Service/Orchestrator → Model → Mapper → DTO`. Service/Orchestrator no devuelven `null` cuando el contrato espera respuesta.

### 1.3. DTO y Mapper del backend

DTO representa exclusivamente entrada/salida HTTP; Service, Model y Repository no dependen de DTO. Mapper transforma únicamente `DTO ↔ Model`, sin negocio, validaciones de negocio, cálculos ni decisiones semánticas; no depende de Service ni Repository.

### 1.4. Dependencias del backend

```text
Controller → DTO, Mapper, Service, Orchestrator
Orchestrator → Service, Model
Service → Model, Repository, Integration
Repository → Model
```

DTO y Mapper se rigen por §1.3. Model no depende de Service, Repository ni Integration. En particular, Controller no accede directamente a Model, Repository o Integration, y Orchestrator no depende de Repository, Integration ni otro Orchestrator.

### 1.5. Dominio y aplicación del backend

Las reglas resolubles exclusivamente con información y comportamiento de dominio pertenecen a Model, incluso si involucran varios Model. Las que requieren Repository, Integration u otras capacidades externas al dominio pertenecen a Service.

### 1.6. Arquitectura del frontend

La unidad principal de organización funcional del frontend debe ser la feature. La arquitectura debe mantener alta cohesión, encapsulación y separación por responsabilidad.

#### 1.6.1. Organización y estructura conceptual

La organización conceptual del frontend debe contemplar:

```text
src/
├── app/
├── features/
├── infrastructure/
├── shared/
├── styles/
├── App.tsx
└── main.tsx
```

Dentro de una feature pueden existir, según su responsabilidad, `components`, `constants`, `form`, `hooks`, `mappers`, `models`, `pages`, `services`, `store`, `types`, `utils` e `index.ts`.

`app` se reserva para composición y configuración global: router, layouts y providers globales, store global, configuración global de TanStack Query y composición entre features. No debe contener lógica específica de una feature. `main.tsx` debe ser un bootstrap mínimo. Configuraciones globales como `queryClient` deben pertenecer conceptualmente a `app`, no a la raíz de `src` como contenedor arbitrario.

`features` contiene las unidades funcionales. Cada feature debe encapsular sus Pages, Components, Hooks, Services, Models, Mappers, formularios y demás elementos específicos que realmente necesite.

`shared` contiene únicamente elementos genuinamente reutilizables entre features. No puede recibir conceptos específicos de una feature para permitir que otra dependa de ellos ni utilizarse para evadir restricciones entre features.

`infrastructure` contiene adaptadores técnicos globales hacia el entorno, como almacenamiento del navegador. No debe contener lógica de negocio ni funcionar como contenedor residual. El acceso a mecanismos concretos como `localStorage` o `sessionStorage` debe encapsularse cuando corresponda y no puede quedar disperso en Components, Pages, Hooks o Services.

Los estilos específicos de una Page o Component deben permanecer colocalizados con ellos mediante CSS Modules o SCSS Modules cuando corresponda. `src/styles` queda reservado para estilos globales, como base, resets, abstracts, variables y mixins.

#### 1.6.2. Aislamiento y API pública de features

Cada feature debe exponer explícitamente su API pública mediante `index.ts`. Un consumidor externo solo puede importar desde esa API y no desde archivos internos. Los módulos internos de la misma feature sí pueden utilizar imports internos.

Una feature no puede depender directa ni indirectamente de otra feature, incluso mediante la API pública de esta última. La composición de múltiples features debe realizarse desde `app` utilizando sus APIs públicas. `app` puede componerlas, pero no absorber su lógica interna.

Los submódulos de `shared` deben exponer APIs públicas cuando corresponda, de modo que sus consumidores no conozcan arbitrariamente su estructura interna, por ejemplo `@/shared/components`, `@/shared/http` o `@/shared/utils`.

#### 1.6.3. Page

Una Page representa una pantalla asociada a una ruta o a la composición de una funcionalidad. Su responsabilidad principal es componer Components y Hooks.

Puede utilizar Components, Hooks, Models y lógica estrictamente de presentación o renderizado, incluidas decisiones visuales basadas en loading, colecciones vacías u otros estados de UI. No debe contener lógica de negocio ni acceso externo. No puede utilizar directamente Services, HTTP ni DTO.

No es obligatorio crear un Page Hook. Debe existir únicamente cuando haya suficiente estado o coordinación de UI para justificar esa extracción; de lo contrario, la Page puede utilizar directamente los Hooks correspondientes.

#### 1.6.4. Component

Un Component representa UI y comportamiento local. Puede utilizar Hooks, Models, estado local y lógica de presentación, incluidos Hooks de su propia feature.

No puede utilizar directamente Services, realizar HTTP ni comunicarse directamente con el backend bajo ninguna circunstancia.

#### 1.6.5. Hooks

Los Hooks conectan UI React y aplicación; puede subdividirse en `pages`, `queries` y `mutations` según responsabilidades.

Los Page Hooks coordinan estado y comportamiento de una Page cuando la complejidad justifica la extracción. Los Query Hooks encapsulan lecturas de estado remoto mediante TanStack Query. Los Mutation Hooks encapsulan modificaciones de estado remoto mediante TanStack Query.

Queries, mutations, invalidaciones y comportamiento asociado al estado remoto de TanStack Query deben permanecer encapsulados en Hooks. Service no puede depender de TanStack Query. Pages y Components no pueden utilizar Services como sustituto de Hooks. El flujo de UI debe ser `Page / Component → Hook → Service`.

#### 1.6.6. Service

Service representa las operaciones externas de una feature y su frontera funcional con el backend.

Los endpoints y URLs propios de la feature deben permanecer en sus Services. Pages, Components y Hooks no pueden definir ni utilizar directamente endpoints HTTP.

Service puede utilizar el cliente HTTP compartido. No debe convertirse en un contenedor genérico de lógica. La lógica de dominio pertenece a Model; la coordinación de UI, a Hook; la transformación, a Mapper; y el acceso externo, a Service.

Un Service puede depender de otro Service de la misma feature únicamente si existe una separación real de responsabilidades. No deben dividirse Services artificialmente para encadenarlos ni pueden existir dependencias circulares.

#### 1.6.7. HTTP compartido

`shared/http` contiene el cliente HTTP común. Puede resolver base URL, configuración del cliente, headers comunes, interceptores, incorporación técnica de credenciales, normalización de errores de transporte y comportamiento HTTP técnico transversal.

No debe contener lógica funcional de una feature, mensajes funcionales de UI ni operaciones de negocio como login o compra de jugadores. Puede normalizar timeout, network errors, status HTTP y otros errores técnicos de transporte. Cada feature debe interpretar sus errores funcionales, como `PLAYER_NOT_AVAILABLE`, `INSUFFICIENT_BALANCE` o `EMAIL_ALREADY_REGISTERED`, y determinar el comportamiento de UI correspondiente. Los errores técnicos y funcionales deben permanecer separados.

#### 1.6.8. DTO, Model y Mapper

DTO representa exclusivamente el contrato HTTP de entrada o salida del backend y debe quedar encapsulado en la frontera de integración de la feature. Pages, Components y Hooks de UI no pueden trabajar con DTO; Service encapsula esa frontera.

Model representa conceptos, estado y comportamiento de dominio utilizados por el frontend. Puede implementarse mediante `type`, `interface`, clase u otra construcción TypeScript sin alterar su responsabilidad. Las reglas resolubles exclusivamente con información del dominio deben implementarse en Model. Model no puede depender de React, Hooks, Services, HTTP, DTO ni infraestructura técnica. Pages, Components y Hooks pueden trabajar con Model.

Mapper realiza exclusivamente transformaciones entre DTO, Model y Form Model. Debe ser puro y no puede realizar HTTP, utilizar Services o Hooks, mantener estado, contener lógica de negocio ni coordinar operaciones. Cada feature debe poseer sus Mappers cuando sean necesarios. Un Mapper compartido solo puede existir para una transformación genuinamente transversal y reutilizable.

Lecturas: `Backend → shared/http → Service → DTO → Mapper → Model → Service → Hook → Page / Component`.

Service debe exponer Model, no DTO, hacia Hooks.

Escrituras: `Component / Form → Form Model → Hook → Service → Mapper → DTO → shared/http → Backend`.

Hook no debe conocer el DTO.

#### 1.6.9. Formularios y validación

Form Model representa el estado y las necesidades de UI de un formulario; DTO representa el contrato HTTP. Deben permanecer separados aunque inicialmente tengan los mismos campos.

`form` puede contener Hooks de formulario, schemas de validación y responsabilidades estrictamente relacionadas con formularios. Los componentes visuales continúan siendo Components de la feature.

Un Form Hook no es obligatorio y solo debe crearse cuando encapsule una responsabilidad real, como configuración de React Hook Form, valores iniciales, schema o comportamiento interno complejo.

Las validaciones de entrada y UX, como formato de email o campo obligatorio, pueden pertenecer al schema del formulario. Las reglas genuinas del dominio deben pertenecer a Model cuando puedan resolverse con información de dominio; no pueden desplazarse a un schema solo porque el dato provenga de un formulario.

La validación frontend mejora UX y consistencia del cliente, pero nunca sustituye al backend como autoridad de validación y protección de reglas. Las validaciones genéricas y genuinamente reutilizables entre features pueden pertenecer a `shared/validation`.

#### 1.6.10. Estado remoto y stores

TanStack Query es responsable del estado remoto y caché proveniente del backend. Ese estado no debe duplicarse innecesariamente en stores globales o de feature.

`app/store` queda reservado para estado cliente verdaderamente transversal. El hecho de que un dato sea estado no justifica ubicarlo allí. El estado local, como apertura de modales, debe permanecer en el Component, Hook o feature correspondiente cuando no sea global.

Una feature puede definir su propio store cuando exista estado cliente compartido entre múltiples elementos de esa feature sin ser global a la aplicación. El estado específico de una feature no puede trasladarse a `app/store` solo para centralizarlo.

#### 1.6.11. Autenticación

La lógica funcional de autenticación pertenece a la feature de autenticación: login, registro, logout y estado o comportamiento funcional del usuario autenticado.

`shared/http` solo debe resolver el mecanismo HTTP transversal, como incorporar técnicamente el token o tratar respuestas técnicas. El almacenamiento concreto de credenciales o tokens debe encapsularse mediante `infrastructure` cuando corresponda. Components, Pages, Hooks y Services no pueden acceder de forma dispersa a `localStorage`, `sessionStorage` u otros mecanismos concretos.

#### 1.6.12. Router, layouts y providers

La configuración global del router pertenece exclusivamente a `app/router`. Las Pages concretas pertenecen a sus features y su asociación con rutas globales se realiza desde `app/router`. Una feature puede navegar cuando lo necesite, pero no ensamblar ni poseer el router global.

`app/layouts` contiene exclusivamente layouts globales o transversales, como layouts públicos, autenticados o de aplicación. Una estructura visual específica de una feature debe permanecer dentro de ella aunque visualmente se considere un layout.

Los providers globales deben configurarse y componerse desde `app`, incluidos TanStack Query, router, tema, store global y cualquier otro provider realmente global.

#### 1.6.13. Utils, constants y query keys

`utils` contiene exclusivamente funciones auxiliares puras que no correspondan mejor a otro rol.

No debe funcionar como contenedor residual. Si una operación representa comportamiento de Model, debe permanecer en Model.

`constants` contiene constantes propias de la feature cuando exista una responsabilidad real. No deben extraerse valores allí únicamente para reducir el tamaño visual de otro archivo.

Las query keys de TanStack Query deben centralizarse por feature cuando esta utilice TanStack Query.

No deben dispersarse strings de query keys si existe una definición central correspondiente.

#### 1.6.14. Dependencias frontend

Las dependencias directas permitidas dentro de una feature son:

```text
Page → Component de la misma feature o shared, Hook de la misma feature, Model
Component → Component de la misma feature o shared, Hook de la misma feature, Model
Hook → Service de la misma feature, Model, Form Model, Hook de formulario, Feature store
Service → Mapper, Model, DTO, Form Model, shared/http, adaptador público de infrastructure,
          Service de la misma feature
Mapper → Model, DTO, Form Model
Hook de formulario → Model, Form Model, validación de la misma feature, shared/validation
Form Model o schema → Model, validación de la misma feature, shared/validation
Feature store → Model
Model → ninguna capa técnica o de aplicación
```

La dependencia `Service → Service` solo está permitida dentro de la misma feature bajo §1.6.6. La dependencia `Service → infrastructure` solo permite utilizar la API pública de un adaptador técnico necesario y no autoriza accesos directos al mecanismo concreto. Un Hook de formulario no puede depender de Query Hooks o Mutation Hooks para evadir sus fronteras.

Una dependencia hacia una API pública de `shared` solo está permitida si su responsabilidad ya está autorizada para el consumidor. Por ejemplo, Page y Component pueden utilizar Components compartidos; Service puede utilizar `shared/http`; Form puede utilizar `shared/validation`; y los roles pueden utilizar utilidades puras compartidas que no introduzcan una dependencia prohibida.

Las dependencias globales permitidas son:

```text
app → APIs públicas de features, APIs públicas de shared, infrastructure
feature → APIs públicas de shared compatibles con su rol
Service de feature → API pública de infrastructure según la regla anterior
shared/http → infrastructure técnica cuando sea necesaria
shared → ningún módulo específico de feature
infrastructure → ningún módulo específico de feature ni lógica de negocio
```

Aplican las reglas comunes de §1.1. Restricciones expresas:

-   Page o Component no puede depender de Service, HTTP ni DTO.

-   Hook no puede depender de DTO ni definir endpoints HTTP.

-   Service no puede depender de React, Component, Page, Hook ni
    TanStack Query.

-   Model no puede depender de React, Hook, Service, HTTP, DTO ni
    infrastructure.

-   Mapper no puede depender de Service, Hook, HTTP, React ni
    infrastructure.

-   una feature no puede depender de otra feature;

-   `shared` e `infrastructure` no pueden depender de features;

-   ningún módulo puede utilizar `app`, barrels o intermediarios para
    evadir estas reglas.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.

### 2.2. DTO del backend

Los DTO HTTP deben implementarse como `record`.

Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales.

La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.

### 2.3. Model del backend

Los Model deben preservar sus invariantes y encapsular su estado.

No deben utilizar `Builder`.

No deben utilizarse `@Setter` en los Model.

Pueden utilizarse mecanismos de Persistence necesarios para representar el Model.

### 2.4. Mapper del backend

Los Mapper deben ser `final`, sin estado mutable, con constructor privado y métodos `static`.

No deben utilizarse instancias de Mapper.

Un Mapper que pueda ser invocado fuera de un endpoint HTTP validado con Jakarta Validation y reciba `null` debe lanzar la excepción de mapeo definida por el proyecto.

No se requiere una validación de `null` específica cuando el Mapper sólo sea alcanzable desde un endpoint HTTP cuya validación estructural ya impida ese caso.

No debe convertir `null` en valores por defecto.

### 2.5. Excepciones del backend

El código de la aplicación no debe lanzar directamente excepciones genéricas provistas por Java, como `RuntimeException`, `IllegalArgumentException` o `IllegalStateException`, para representar errores funcionales, de aplicación o de dominio.

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante `DomainException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe heredar de la categoría correspondiente.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta regla. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el funcionamiento interno de esas tecnologías.

### 2.6. Referencias a miembros de instancia en Java

Toda clase productiva debe utilizar `this` al acceder a sus atributos de instancia y al invocar sus propios métodos de instancia, incluso cuando no exista ambigüedad. La regla se aplica también a métodos triviales y al código de los `record` definido explícitamente por el proyecto.

En el código de tests está prohibido usar `this`, incluidas las referencias calificadas como `OuterTest.this`; acceder a los miembros directamente, según testing §16.

No aplica a miembros `static`, variables locales, parámetros que no sean miembros de la instancia ni código generado automáticamente por Java, Lombok, frameworks u otras herramientas.

### 2.7. Contrato HTTP del backend

- Toda ruta de Controller comienza con el prefijo exacto `/api/`; `/api` y prefijos como `/apiv2` no son válidos.
- El body es opcional. Si una respuesta exitosa contiene datos, utilizar Response DTO, sin exponer directamente Model internos.
- Validación estructural HTTP en Controller/DTO; negocio en Service/Model; contratos externos en Integration.
- Capturar errores estructurales de Jakarta Validation en presentación. Todo error HTTP gestionado por la aplicación utiliza `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`; `status` coincide con el código HTTP y `path` identifica la ruta solicitada. Ante múltiples errores de validación, `message` contiene el primer mensaje disponible.

### 2.8. Documentación del backend

Todo endpoint funcional debe documentarse en OpenAPI e incorporarse a la colección Postman versionada. Actualizar ambos ante cambios de contrato, reflejando propósito, método, ruta, parámetros, headers, autenticación, DTO, validaciones, códigos y errores aplicables. Los ejemplos deben ser utilizables sin credenciales reales. Documentar significado de campos DTO y, cuando aporte valor, ejemplos y restricciones. Spring REST Docs se rige por testing §8.

Javadoc obligatorio en clases públicas y métodos `public`/`protected` productivos. En métodos privados y constructores explícitos, solo si la lógica, propósito, parámetros o precondiciones requieren explicación; quedan exceptuados los triviales y constructores implícitos o generados. Los métodos sobrescritos conservan documentación accesible; `{@inheritDoc}` basta si cubre el contrato completo.

En tests, métodos de prueba y ciclo de vida no requieren Javadoc; helpers solo cuando necesitan explicación. Modificar el cuerpo de un método exceptuado no lo obliga a tener Javadoc.

## 3. Integraciones externas del backend

Toda comunicación externa pasa por Integration, que encapsula configuración, comunicación, contratos externos, transformación y errores técnicos. Model no conoce proveedores, clientes, HTTP ni protocolos; los contratos externos permanecen aislados de los HTTP y Model internos.

Validar respuestas externas y distinguir errores técnicos de integración de errores de dominio. No inventar datos ni alterar silenciosamente el comportamiento ante fallos. Usar timeouts cuando la tecnología lo permita; retries solo con operación segura y límite explícito, nunca por defecto.

Resolver indisponibilidad según requisitos funcionales; no introducir automáticamente cachés, degradaciones o alternativas sin justificación funcional o técnica. Aplican las reglas globales de seguridad de la constitución.

## 4. Stack y versiones

Las versiones efectivas se consultan en Gradle, su catálogo/wrapper y `frontend/package.json`. Las restricciones aprobadas siguientes no autorizan actualizar dependencias fuera de la tarea; cualquier discrepancia se resuelve según la constitución.

### 4.1. Backend

Java 21, Gradle 8.14+, Spring Boot 4.1.1, Spring Web, Spring Security, OAuth2 Resource Server, Spring Data JPA, PostgreSQL 18.x, JDBC, Flyway, Jakarta Validation 3.1.1 y SpringDoc OpenAPI 3.1.0. Testing: Spring Boot Test, Spring REST Docs, JUnit, Mockito, AssertJ y Testcontainers. SonarQube es opcional según la constitución.

### 4.2. Frontend

TypeScript, React, TanStack Query para estado remoto y CSS Modules o SCSS Modules para estilos colocalizados cuando corresponda.

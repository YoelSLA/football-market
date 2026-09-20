<!--
Sync Impact Report:

- Version change: 2.3.0 -> 2.4.0

- List of modified principles:
  - 1. Arquitectura
  - 2. Convenciones
  - 3. Integraciones externas del backend
  - 4. Testing
  - 8. Documentación
  - Technology Constraints

- Renamed sections:
  - 1.3. DTO y Mapper -> 1.3. DTO y Mapper del backend
  - 1.4. Dependencias arquitectónicas -> 1.4. Dependencias arquitectónicas del backend
  - 1.5. Reglas de dominio y aplicación -> 1.5. Reglas de dominio y aplicación del backend
  - 1.6. Evolución y nuevas áreas -> 1.7. Evolución y nuevas áreas
  - 1.7. Resolución arquitectónica -> 1.8. Resolución arquitectónica
  - 2.2-2.6 delimitadas explícitamente como convenciones del backend/Java
  - 3. Integraciones externas -> 3. Integraciones externas del backend
  - 4.2. Clasificación y alcance -> 4.2. Clasificación y alcance del backend
  - 8.4. Javadoc -> 8.4. Javadoc del backend

- Added sections:
  - 1.6. Arquitectura del frontend
  - 1.6.1. Organización y estructura conceptual
  - 1.6.2. Aislamiento y API pública de features
  - 1.6.3. Page
  - 1.6.4. Component
  - 1.6.5. Hooks
  - 1.6.6. Service
  - 1.6.7. HTTP compartido
  - 1.6.8. DTO, Model y Mapper
  - 1.6.9. Formularios y validación
  - 1.6.10. Estado remoto y stores
  - 1.6.11. Autenticación
  - 1.6.12. Router, layouts y providers
  - 1.6.13. Utils, constants y query keys
  - 1.6.14. Dependencias frontend
  - 1.6.15. Crecimiento y resolución arquitectónica

- Removed sections:
  - None

- Added requirements:
  - Arquitectura frontend organizada principalmente por feature.
  - Encapsulación mediante APIs públicas y prohibición de dependencias entre features.
  - Fronteras explícitas para UI, Hooks, Services, HTTP, DTO, Model y Mapper.
  - Matriz normativa de dependencias frontend permitidas y prohibidas.

- Removed requirements:
  - None

- Corrected references:
  - None

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

Todo Orchestrator debe ubicarse en el paquete global `footballmarket.orchestrators`. No deben
existir paquetes de Orchestrator dentro de cada feature ni ubicarse Orchestrator dentro de paquetes
correspondientes a Controller, Service, Repository u otro rol.

#### 1.2.3. Service

Implementa la lógica de aplicación.

Todo Service debe definir una interfaz y una implementación separadas. Los consumidores deben
depender de la interfaz; la implementación concreta no debe utilizarse como contrato. Esta regla
es obligatoria incluso cuando exista una única implementación y, por ser una regla arquitectónica
específica, prevalece sobre la prohibición general de crear interfaces innecesarias.

#### 1.2.4. Model

Representa conceptos, estado, comportamiento e invariantes del dominio.

Las reglas que puedan resolverse exclusivamente con información del dominio deben implementarse en Model.

No puede depender de Service, Repository ni Integration.

#### 1.2.5. Repository

Gestiona el acceso a datos persistidos.

No debe contener lógica de negocio.

Puede utilizar Model.

No puede depender de Service ni DTO.

La anotación `@Repository` debe utilizarse únicamente en interfaces de Repository de Spring Data,
como aquellas que extiendan `JpaRepository`. No debe utilizarse en puertos de persistencia,
implementaciones manuales, repositorios de test ni otras clases o interfaces que cumplan
conceptualmente un rol de persistencia.

#### 1.2.6. Integration

Puede ser utilizada por Service.

No puede ser utilizada directamente por Controller, Orchestrator, Model ni Repository.

### 1.3. DTO y Mapper del backend

#### 1.3.1. DTO

Los DTO representan los contratos HTTP de entrada y salida.

Service, Model y Repository no deben depender de DTO.

#### 1.3.2. Mapper

Los Mapper transforman exclusivamente entre DTO y Model:

DTO → Model
Model → DTO

No deben contener lógica de negocio, validaciones de negocio, cálculos ni decisiones semánticas.

No pueden depender de Service ni Repository.

### 1.4. Dependencias arquitectónicas del backend

Las dependencias directas permitidas son:

Controller → DTO, Mapper, Service, Orchestrator
Orchestrator → Service, Model
Service → Model, Repository, Integration
Repository → Model

Las relaciones de DTO y Mapper se rigen por la sección 1.3.

Toda dependencia no listada está prohibida.

Las dependencias transitivas no autorizan dependencias directas.

No deben existir dependencias circulares ni utilizarse intermediarios para evadir una restricción arquitectónica.

### 1.5. Reglas de dominio y aplicación del backend

Las reglas que puedan resolverse exclusivamente con información y comportamiento del dominio deben implementarse en Model.

Las reglas que requieran Repository, Integration u otras capacidades externas al dominio deben implementarse en Service.

Las reglas que involucren múltiples Model pueden permanecer en el dominio cuando puedan resolverse completamente mediante comportamiento de dominio.

### 1.6. Arquitectura del frontend

La unidad principal de organización funcional del frontend debe ser la feature. La arquitectura
debe mantener alta cohesión, encapsulación y separación por responsabilidad.

Una feature o componente arquitectónico solo debe dividirse cuando exista una responsabilidad real
que lo justifique. La cantidad de archivos, líneas o código no autoriza por sí sola una división.
Ninguna feature está obligada a contener todos los roles posibles.

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

Esta estructura define fronteras globales, no obliga a crear carpetas sin contenido ni
responsabilidad. Dentro de una feature pueden existir, cuando sean necesarias, `components`,
`constants`, `form`, `hooks`, `mappers`, `models`, `pages`, `services`, `store`, `types`, `utils` e
`index.ts`. Esta enumeración describe roles posibles y no constituye una plantilla obligatoria.

`app` se reserva para composición y configuración global: router, layouts y providers globales,
store global, configuración global de TanStack Query y composición entre features. No debe contener
lógica específica de una feature. `main.tsx` debe ser un bootstrap mínimo. Configuraciones globales
como `queryClient` deben pertenecer conceptualmente a `app`, no a la raíz de `src` como contenedor
arbitrario.

`features` contiene las unidades funcionales. Cada feature debe encapsular sus Pages, Components,
Hooks, Services, Models, Mappers, formularios y demás elementos específicos que realmente necesite.

`shared` contiene únicamente elementos genuinamente reutilizables entre features. No puede recibir
conceptos específicos de una feature para permitir que otra dependa de ellos ni utilizarse para
evadir restricciones entre features.

`infrastructure` contiene adaptadores técnicos globales hacia el entorno, como almacenamiento del
navegador. No debe contener lógica de negocio ni funcionar como contenedor residual. El acceso a
mecanismos concretos como `localStorage` o `sessionStorage` debe encapsularse cuando corresponda y
no puede quedar disperso en Components, Pages, Hooks o Services.

Los estilos específicos de una Page o Component deben permanecer colocalizados con ellos mediante
CSS Modules o SCSS Modules cuando corresponda. `src/styles` queda reservado para estilos globales,
como base, resets, abstracts, variables y mixins.

#### 1.6.2. Aislamiento y API pública de features

Cada feature debe exponer explícitamente su API pública mediante `index.ts`. Un consumidor externo
solo puede importar desde esa API y no desde archivos internos. Por ejemplo, desde afuera de la
feature se permite `import { PlayersPage } from "@/features/players"` y se prohíbe importar desde
`@/features/players/pages/PlayersPage`. Los módulos internos de la misma feature sí pueden utilizar
imports internos.

Una feature no puede depender directa ni indirectamente de otra feature, incluso mediante la API
pública de esta última. La composición de múltiples features debe realizarse desde `app` utilizando
sus APIs públicas. `app` puede componerlas, pero no absorber su lógica interna.

No se permite trasladar código específico de una feature a `shared` para eludir esta prohibición.
Los submódulos de `shared` deben exponer APIs públicas cuando corresponda, de modo que sus
consumidores no conozcan arbitrariamente su estructura interna, por ejemplo
`@/shared/components`, `@/shared/http` o `@/shared/utils`.

#### 1.6.3. Page

Una Page representa una pantalla asociada a una ruta o a la composición de una funcionalidad. Su
responsabilidad principal es componer Components y Hooks.

Puede utilizar Components, Hooks, Models y lógica estrictamente de presentación o renderizado,
incluidas decisiones visuales basadas en loading, colecciones vacías u otros estados de UI. No debe
contener lógica de negocio ni acceso externo. No puede utilizar directamente Services, HTTP ni DTO.

No es obligatorio crear un Page Hook. Debe existir únicamente cuando haya suficiente estado o
coordinación de UI para justificar esa extracción; de lo contrario, la Page puede utilizar
directamente los Hooks correspondientes.

#### 1.6.4. Component

Un Component representa UI y comportamiento local. Puede utilizar Hooks, Models, estado local y
lógica de presentación, incluidos Hooks de su propia feature.

No puede utilizar directamente Services, realizar HTTP ni comunicarse directamente con el backend
bajo ninguna circunstancia.

#### 1.6.5. Hooks

Los Hooks constituyen la frontera entre la UI React y el comportamiento de aplicación. Una feature
puede subdividirlos en `pages`, `queries` y `mutations` cuando existan esas responsabilidades; la
subdivisión no es obligatoria.

Los Page Hooks coordinan estado y comportamiento de una Page cuando la complejidad justifica la
extracción. Los Query Hooks encapsulan lecturas de estado remoto mediante TanStack Query. Los
Mutation Hooks encapsulan modificaciones de estado remoto mediante TanStack Query.

Queries, mutations, invalidaciones y comportamiento asociado al estado remoto de TanStack Query
deben permanecer encapsulados en Hooks. Service no puede depender de TanStack Query. Pages y
Components no pueden utilizar Services como sustituto de Hooks. El flujo de UI debe ser
`Page / Component → Hook → Service`.

#### 1.6.6. Service

Service representa las operaciones externas de una feature y su frontera funcional con el backend.
Los endpoints y URLs propios de la feature deben permanecer en sus Services. Pages, Components y
Hooks no pueden definir ni utilizar directamente endpoints HTTP.

Service puede utilizar el cliente HTTP compartido. No debe convertirse en un contenedor genérico
de lógica. La lógica de dominio pertenece a Model; la coordinación de UI, a Hook; la transformación,
a Mapper; y el acceso externo, a Service.

Un Service puede depender de otro Service de la misma feature únicamente si existe una separación
real de responsabilidades. No deben dividirse Services artificialmente para encadenarlos ni pueden
existir dependencias circulares.

#### 1.6.7. HTTP compartido

`shared/http` contiene el cliente HTTP común. Puede resolver base URL, configuración del cliente,
headers comunes, interceptores, incorporación técnica de credenciales, normalización de errores de
transporte y comportamiento HTTP técnico transversal.

No debe contener lógica funcional de una feature, mensajes funcionales de UI ni operaciones de
negocio como login o compra de jugadores. Puede normalizar timeout, network errors, status HTTP y
otros errores técnicos de transporte. Cada feature debe interpretar sus errores funcionales, como
`PLAYER_NOT_AVAILABLE`, `INSUFFICIENT_BALANCE` o `EMAIL_ALREADY_REGISTERED`, y determinar el
comportamiento de UI correspondiente. Los errores técnicos y funcionales deben permanecer separados.

#### 1.6.8. DTO, Model y Mapper

DTO representa exclusivamente el contrato HTTP de entrada o salida del backend y debe quedar
encapsulado en la frontera de integración de la feature. Pages, Components y Hooks de UI no pueden
trabajar con DTO cuando Service pueda encapsular esa frontera.

Model representa conceptos, estado y comportamiento de dominio utilizados por el frontend. Puede
implementarse mediante `type`, `interface`, clase u otra construcción TypeScript sin alterar su
responsabilidad. Las reglas resolubles exclusivamente con información del dominio deben
implementarse en Model. Model no puede depender de React, Hooks, Services, HTTP, DTO ni
infraestructura técnica. Pages, Components y Hooks pueden trabajar con Model.

Mapper realiza exclusivamente transformaciones entre DTO, Model y Form Model. Debe ser puro y no
puede realizar HTTP, utilizar Services o Hooks, mantener estado, contener lógica de negocio ni
coordinar operaciones. Cada feature debe poseer sus Mappers cuando sean necesarios. Un Mapper
compartido solo puede existir para una transformación genuinamente transversal y reutilizable.

En lecturas, el flujo debe ser
`Backend → shared/http → Service → DTO → Mapper → Model → Service → Hook → Page / Component`.
Service debe exponer Model, no DTO, hacia Hooks.

En escrituras, el flujo debe ser
`Component / Form → Form Model → Hook → Service → Mapper → DTO → shared/http → Backend`.
Hook no debe conocer el DTO.

#### 1.6.9. Formularios y validación

Form Model representa el estado y las necesidades de UI de un formulario; DTO representa el
contrato HTTP. Deben permanecer separados aunque inicialmente tengan los mismos campos.

`form` puede contener Hooks de formulario, schemas de validación y responsabilidades estrictamente
relacionadas con formularios. Los componentes visuales continúan siendo Components de la feature.
Un Form Hook no es obligatorio y solo debe crearse cuando encapsule una responsabilidad real, como
configuración de React Hook Form, valores iniciales, schema o comportamiento interno complejo.

Las validaciones de entrada y UX, como formato de email o campo obligatorio, pueden pertenecer al
schema del formulario. Las reglas genuinas del dominio deben pertenecer a Model cuando puedan
resolverse con información de dominio; no pueden desplazarse a un schema solo porque el dato
provenga de un formulario.

La validación frontend mejora UX y consistencia del cliente, pero nunca sustituye al backend como
autoridad de validación y protección de reglas. Las validaciones genéricas y genuinamente
reutilizables entre features pueden pertenecer a `shared/validation`.

#### 1.6.10. Estado remoto y stores

TanStack Query es responsable del estado remoto y caché proveniente del backend. Ese estado no debe
duplicarse innecesariamente en stores globales o de feature.

`app/store` queda reservado para estado cliente verdaderamente transversal. El hecho de que un dato
sea estado no justifica ubicarlo allí. El estado local, como apertura de modales, debe permanecer en
el Component, Hook o feature correspondiente cuando no sea global.

Una feature puede definir su propio store cuando exista estado cliente compartido entre múltiples
elementos de esa feature sin ser global a la aplicación. El estado específico de una feature no
puede trasladarse a `app/store` solo para centralizarlo.

#### 1.6.11. Autenticación

La lógica funcional de autenticación pertenece a la feature de autenticación: login, registro,
logout y estado o comportamiento funcional del usuario autenticado.

`shared/http` solo debe resolver el mecanismo HTTP transversal, como incorporar técnicamente el
token o tratar respuestas técnicas. El almacenamiento concreto de credenciales o tokens debe
encapsularse mediante `infrastructure` cuando corresponda. Components, Pages, Hooks y Services no
pueden acceder de forma dispersa a `localStorage`, `sessionStorage` u otros mecanismos concretos.

#### 1.6.12. Router, layouts y providers

La configuración global del router pertenece exclusivamente a `app/router`. Las Pages concretas
pertenecen a sus features y su asociación con rutas globales se realiza desde `app/router`. Una
feature puede navegar cuando lo necesite, pero no ensamblar ni poseer el router global.

`app/layouts` contiene exclusivamente layouts globales o transversales, como layouts públicos,
autenticados o de aplicación. Una estructura visual específica de una feature debe permanecer
dentro de ella aunque visualmente se considere un layout.

Los providers globales deben configurarse y componerse desde `app`, incluidos TanStack Query,
router, tema, store global y cualquier otro provider realmente global.

#### 1.6.13. Utils, constants y query keys

`utils` contiene exclusivamente funciones auxiliares puras que no correspondan mejor a otro rol.
No debe funcionar como contenedor residual. Si una operación representa comportamiento de Model,
debe permanecer en Model.

`constants` contiene constantes propias de la feature cuando exista una responsabilidad real. No
deben extraerse valores allí únicamente para reducir el tamaño visual de otro archivo.

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

La dependencia `Service → Service` solo está permitida dentro de la misma feature y bajo las
restricciones de 1.6.6. La dependencia `Service → infrastructure` solo permite utilizar la API
pública de un adaptador técnico necesario y no autoriza accesos directos al mecanismo concreto. Un
Hook de formulario no puede depender de Query Hooks o Mutation Hooks para evadir sus fronteras.

Una dependencia hacia una API pública de `shared` solo está permitida si su responsabilidad ya está
autorizada para el consumidor. Por ejemplo, Page y Component pueden utilizar Components
compartidos; Service puede utilizar `shared/http`; Form puede utilizar `shared/validation`; y los
roles pueden utilizar utilidades puras compartidas que no introduzcan una dependencia prohibida.

Las dependencias globales permitidas son:

```text
app → APIs públicas de features, APIs públicas de shared, infrastructure
feature → APIs públicas de shared compatibles con su rol
Service de feature → API pública de infrastructure según la regla anterior
shared/http → infrastructure técnica cuando sea necesaria
shared → ningún módulo específico de feature
infrastructure → ningún módulo específico de feature ni lógica de negocio
```

Toda dependencia directa no autorizada está prohibida. En particular:

- Page o Component no puede depender de Service, HTTP ni DTO.
- Hook no puede depender de DTO ni definir endpoints HTTP.
- Service no puede depender de React, Component, Page, Hook ni TanStack Query.
- Model no puede depender de React, Hook, Service, HTTP, DTO ni infrastructure.
- Mapper no puede depender de Service, Hook, HTTP, React ni infrastructure.
- una feature no puede depender de otra feature;
- `shared` e `infrastructure` no pueden depender de features;
- ningún módulo puede utilizar `app`, barrels o intermediarios para evadir estas reglas.

Las dependencias transitivas no autorizan dependencias directas. No pueden existir dependencias
circulares en ninguna parte de la arquitectura frontend.

#### 1.6.15. Crecimiento y resolución arquitectónica

La estructura existente del frontend no prevalece sobre esta Constitution. El código, carpetas o
convenciones incompatibles deben refactorizarse cuando formen parte del alcance aplicable.

No debe crearse una carpeta, Hook, Service, Mapper, Model, Store u otro componente únicamente porque
aparezca en esta arquitectura. Toda creación y subdivisión debe responder a una responsabilidad o
agrupación conceptual real, nunca solo a tamaño, líneas o cantidad de archivos.

Las responsabilidades no deben asignarse por conveniencia técnica. Si una situación relevante no
puede resolverse mediante esta Constitution, la Spec o el código aplicable, el agente debe solicitar
aclaración. No puede inventar una capa, mover código a `shared`, introducir dependencias entre
features ni crear excepciones arquitectónicas por conveniencia.

### 1.7. Evolución y nuevas áreas

La arquitectura definida por esta Constitution prevalece sobre estructuras existentes.

La existencia de clases, paquetes, interfaces o convenciones previas no obliga a conservarlas cuando contradigan la arquitectura.

Las refactorizaciones necesarias para cumplir la arquitectura están permitidas.

Toda nueva área debe definir explícitamente sus responsabilidades, componentes y dependencias.

### 1.8. Resolución arquitectónica

Las dependencias y responsabilidades no deben inferirse por conveniencia técnica.

Cuando una situación relevante no pueda resolverse mediante las reglas existentes, la Spec o el código aplicable, el agente debe solicitar aclaración en lugar de inventar una regla arquitectónica.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.

### 2.2. DTO del backend

Los DTO HTTP deben implementarse como `record`.

Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales.

Los mensajes de validación propios de la aplicación deben estar en español.

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

El código de la aplicación no debe lanzar directamente excepciones genéricas provistas por Java,
como `RuntimeException`, `IllegalArgumentException` o `IllegalStateException`, para representar
errores funcionales, de aplicación o de dominio.

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o
indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante
`DomainException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe
heredar de la categoría correspondiente.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta
regla. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el
funcionamiento interno de esas tecnologías.

### 2.6. Referencias a miembros de instancia en Java

Toda clase debe utilizar `this` al acceder a sus atributos de instancia y al invocar sus propios
métodos de instancia, incluso cuando no exista ambigüedad. La regla se aplica también a clases de
test, métodos triviales y al código de los `record` definido explícitamente por el proyecto.

No aplica a miembros `static`, variables locales, parámetros que no sean miembros de la instancia
ni código generado automáticamente por Java, Lombok, frameworks u otras herramientas.

### 2.7. Framework y tecnología

Las tecnologías y frameworks establecidos por el proyecto deben utilizarse respetando la arquitectura.

Las anotaciones, clases base e interfaces proporcionadas por los frameworks no deben utilizarse para introducir responsabilidades o dependencias prohibidas.

## 3. Integraciones externas del backend

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

### 4.1. Principios generales

La suite debe organizarse alrededor de responsabilidades arquitectónicas y comportamientos
observables, no de alcanzar cobertura por cobertura ni de crear obligatoriamente un test por cada
clase. Cada test debe tener un propósito concreto y demostrar una responsabilidad de la capa o
componente bajo prueba.

Los tests deben priorizar comportamiento observable, resultado, estado final, contratos e
invariantes sobre detalles internos. No deben duplicar sin necesidad una responsabilidad ya
demostrada en una capa inferior o más específica. Ningún tipo de test sustituye universalmente a
los demás: la suite debe combinar tests unitarios precisos, tests de capa, integraciones con
infraestructura real controlada y pocos E2E de flujos críticos.

La existencia de mocks no convierte por sí sola una prueba en unitaria. El uso de
`@SpringBootTest` no convierte por sí solo una prueba en E2E. La clasificación depende del alcance
real verificado y de qué fronteras permanecen reales o son sustituidas. Debe utilizarse la mínima
infraestructura necesaria; `@SpringBootTest` no debe utilizarse por defecto cuando sean suficientes
JUnit puro, un slice de Spring o un contexto reducido.

Los tests deben ser deterministas, repetibles, reproducibles e independientes. No deben depender
de Internet, servicios externos reales, API keys productivas, datos externos variables, orden de
ejecución, estado dejado por otros tests, un reloj externo controlable ni aleatoriedad innecesaria.
Cada test debe poder ejecutarse de manera aislada.

### 4.2. Clasificación y alcance del backend

Cada área debe demostrar exclusivamente su responsabilidad principal:

- `Model`: invariantes y comportamiento de dominio.
- `Repository`: comportamiento de persistencia propio que requiera prueba independiente.
- `Service`: casos de uso, reglas de aplicación y estado persistido observable.
- `Orchestrator`: coordinación entre casos de uso.
- `Controller`: contrato HTTP y seguridad observable desde HTTP.
- `Integration`: adaptación técnica frente a sistemas externos.
- `Security`: funcionamiento conjunto de autenticación y autorización.
- `Config`: binding, validación y construcción de infraestructura.
- `E2E`: flujos críticos completos desde la frontera HTTP.

Los Mapper no deben tener tests directos dedicados. Sus transformaciones y errores observables
deben quedar verificados a través de los tests de Controller y del contrato HTTP que soportan.

### 4.3. Tests de Model

Los tests ubicados en `src/test/java/footballmarket/models` deben ser unitarios puros. Deben
demostrar que el dominio protege invariantes, reglas de validez, transiciones de estado,
comportamiento propio, modificaciones de estado y atomicidad cuando corresponda.

Deben cubrir, según el comportamiento existente, construcción válida e inválida, estado inicial,
activación y desactivación, actualizaciones permitidas, excepciones de dominio y ausencia de
modificaciones parciales tras una operación rechazada.

No deben levantar Spring, utilizar `@SpringBootTest`, base de datos, Repository, Integration,
perfil `test` ni mocks, salvo una necesidad excepcional justificada por el diseño del dominio.
No deben utilizar `@ActiveProfiles("test")`.

No deben existir tests dedicados únicamente a comportamiento trivial provisto por Java, `record` o
Lombok, como getters o setters sin lógica. Esos miembros solo deben quedar cubiertos como parte de
una comprobación significativa del dominio.

### 4.4. Tests de Repository

No es obligatorio crear un test por cada Repository. Las operaciones estándar heredadas de Spring
Data JPA, como `save`, `findById`, `findAll`, `delete` y `deleteAll`, no deben probarse aisladamente
para demostrar el funcionamiento del framework. La persistencia estándar debe ejercitarse
principalmente de forma indirecta mediante tests de Service.

Deben existir tests específicos cuando haya comportamiento de persistencia propio que necesite
verificación independiente: `@Query`, JPQL, SQL nativo, filtros complejos, joins, proyecciones,
ordenamientos relevantes, queries derivadas complejas o constraints no demostradas adecuadamente
desde Service.

Estos tests deben usar la infraestructura real de persistencia de test y comprobar resultados
observables. Nunca deben mockear el Repository bajo prueba.

### 4.5. Tests de Service

Los tests ubicados en `src/test/java/footballmarket/services` deben comprobar casos de uso, reglas
de negocio y cambios de estado. Son deliberadamente tests de integración con Service, Repository,
JPA/Hibernate y base de datos de test reales; las Integrations externas deben ser mocks y los
servicios externos reales no deben utilizarse.

Deben utilizar `@SpringBootTest` y `@ActiveProfiles("test")`. El Service bajo prueba debe declararse
mediante su interfaz e inyectarse con `@Autowired`; Spring debe resolver su implementación real.

El Act y el Assert funcional solo pueden interactuar con la API pública del Service bajo prueba.
Durante el Arrange, el test puede inyectar e invocar otros Services reales exclusivamente para
crear precondiciones pertenecientes a sus responsabilidades. Esos Services auxiliares no pueden
convertirse en objeto de prueba, utilizarse durante el Act ni emplearse para verificar el resultado
del caso de uso principal.

El código del test no puede declarar, inyectar, acceder, invocar, mockear ni verificar directamente
ningún Repository, ni siquiera para Arrange, limpieza o assertions. Tampoco puede acceder
directamente a JPA, Hibernate, `EntityManager`, JDBC ni la base de datos. Repository, JPA y base de
datos quedan ejercitados indirectamente y exclusivamente a través de Services reales.

Todo Act y Assert funcional debe realizarse mediante la API pública productiva del Service bajo
prueba. El Arrange debe utilizar su API o, únicamente para precondiciones externas a su
responsabilidad, las APIs públicas productivas de Services auxiliares. No deben agregarse métodos
productivos a ningún Service únicamente para facilitar los tests. Cuando el contrato público del
Service bajo prueba no permita observar un detalle interno de persistencia, el test debe afirmar
únicamente el comportamiento observable que dicho contrato exponga; no debe vulnerar la frontera
para obtener acceso al detalle.

Los tests deben favorecer el resultado retornado y el estado final observable mediante el Service:
altas, modificaciones, reactivaciones, bajas lógicas, reglas de negocio, paginación, filtrado,
entidades existentes o inexistentes, snapshots o entradas completas y consistencia posterior.
`verify(repository...)` está prohibido y nunca sustituye la comprobación del resultado observable.

El test puede declarar y configurar un mock de Integration mediante mecanismos como
`@MockitoBean` y `when(...)`. No puede invocar la Integration para ejecutar el comportamiento
funcional ni probar su implementación. La ejecución debe comenzar siempre mediante el Service; el
protocolo de la Integration se prueba en sus propios tests.

### 4.6. Preparación y limpieza de persistencia

Cada test de Service debe partir de un estado conocido, independiente y determinista. No puede
depender de datos creados por otro test ni del orden de ejecución.

La preparación y limpieza deben respetar la frontera absoluta del Service definida en 4.5. No se
permite usar Repository, JPA, JDBC, SQL ni acceso directo a la base de datos desde el test. Debe
utilizarse aislamiento transaccional o un mecanismo de infraestructura de testing que restaure un
estado conocido sin exponer esas dependencias al código del test. Si se necesitan datos
funcionales, deben crearse mediante operaciones públicas productivas del Service bajo prueba o de
un Service auxiliar cuando la precondición pertenezca a la responsabilidad de este último.

Ninguna estrategia de preparación o limpieza puede afectar una base que no sea exclusiva de
testing ni puede motivar la incorporación de métodos productivos destinados solo a tests.

### 4.7. Tests de Orchestrator

Los tests ubicados en `src/test/java/footballmarket/orchestrators` deben ser tests unitarios de
coordinación. El Orchestrator debe ser real y sus Services u otros colaboradores permitidos deben
ser mocks.

Su objetivo exclusivo es comprobar qué colaboradores se invocan, qué datos se transfieren, qué
resultado se propaga o compone y cuándo una falla impide invocaciones posteriores. Se permite
`verify(...)`, `verifyNoInteractions(...)` y verificar orden cuando la interacción o el orden sean
parte del comportamiento de coordinación.

No deben levantar `@SpringBootTest`, utilizar base de datos, repetir lógica interna de los Services
ni usar `@ActiveProfiles("test")` cuando sean unitarios puros.

### 4.8. Tests de Controller y Spring REST Docs

Los tests ubicados en `src/test/java/footballmarket/controllers` deben verificar el contrato HTTP:
rutas, métodos, parámetros, path variables, bodies, DTO, serialización, códigos, estructura exacta
de respuestas contractuales, validaciones, errores, `GlobalExceptionHandler` y seguridad visible
desde HTTP.

Deben preferir `@WebMvcTest(...)`: Spring MVC, Controller, configuración web relevante y Security
Filter Chain deben ser reales; Service y Orchestrator deben ser mocks; Repository no debe
utilizarse directamente y la base de datos no es necesaria salvo justificación excepcional. No
deben volver a probar la lógica interna del Service.

Las entradas inválidas deben rechazarse con el código y contrato de error correspondientes. Cuando
el rechazo deba ocurrir antes del caso de uso, debe comprobarse la ausencia de interacción con
Service u Orchestrator si esto aporta valor, por ejemplo ante paginación inválida o JWT ausente o
inválido.

Todo endpoint y todo caso de respuesta contractual verificado por tests de Controller debe generar
su documentación mediante Spring REST Docs. No existe un subconjunto opcional. Cada caso
contractual distinto de la API debe quedar representado, sin duplicar snippets por cada ejecución
parametrizada ni por tests adicionales de propiedades internas cuando otro test ya documente el
mismo caso HTTP.

REST Docs no sustituye assertions. El test debe comprobar primero status, estructura y contenido
contractual, y la documentación debe derivarse de ese comportamiento comprobado. No puede
documentarse una respuesta no verificada. Snippets y descripciones deben permanecer sincronizados;
todo cambio de request, response, status o campos documentados debe actualizar tests y
documentación. Los Controller tests son la fuente principal de REST Docs; los E2E no deben
duplicarla obligatoriamente.

### 4.9. Tests de Integration externa

Los tests ubicados en `src/test/java/footballmarket/integrations` deben verificar los adaptadores
frente a proveedores externos. La Integration y su cliente o protocolo HTTP deben ser reales; el
proveedor externo debe ser un servidor controlado o una herramienta equivalente, como
`MockRestServiceServer`. Nunca deben realizar requests a Football-Data.org ni a otro proveedor real.

Deben comprobar, según corresponda, URL, path, query parameters, headers, autenticación técnica,
serialización, deserialización, mapeo externo a interno, datos incompletos, respuestas inválidas,
códigos externos, timeouts, rate limiting, política y límite de retries, y traducción de errores a
excepciones propias de Integration. Las capas superiores no deben conocer excepciones específicas
del cliente HTTP. Cuando sea parte del contrato de seguridad, deben verificar que URLs, respuestas
o excepciones no filtren información sensible.

Los tests autocontenidos que construyan manualmente su configuración y no consuman el contexto de
Spring no deben usar `@ActiveProfiles("test")`.

### 4.10. Tests de Security

Los tests transversales de autenticación y autorización deben ubicarse en
`src/test/java/footballmarket/security`, nunca en `integrations`. Deben demostrar el funcionamiento
conjunto de componentes reales de seguridad y pueden utilizar contexto Spring, Security Filter
Chain, proveedor JWT, autenticación, persistencia real y MockMvc cuando el flujo lo requiera.

Deben verificar, según corresponda, rechazo sin token, aceptación con token válido, rechazo de
firma inválida o token expirado y flujos como registro, login, JWT y acceso protegido. Puede crearse
un endpoint mínimo exclusivo del test cuando el objetivo sea la infraestructura de seguridad y no
un Controller productivo. Deben usar `@ActiveProfiles("test")` cuando dependan de la base, secreto
JWT u otras properties del perfil.

### 4.11. Tests de configuración

Los tests ubicados en `src/test/java/footballmarket/config` deben verificar configuración propia:
`@ConfigurationProperties`, binding, validación, construcción de beans, requisitos de startup,
clientes seguros, rechazo de valores inválidos y protección de secretos en representaciones como
`toString()`.

Deben usar la menor cantidad posible de contexto. Cuando alcance, deben instanciar directamente las
clases; para binding o startup deben preferir `ApplicationContextRunner` a `@SpringBootTest`. Los
tests autocontenidos que proporcionen todas sus properties no deben usar innecesariamente
`@ActiveProfiles("test")`. Si una property inválida debe impedir el startup, el test debe demostrar
el fallo del contexto.

### 4.12. Perfil `test` y base de datos

`@ActiveProfiles("test")` solo debe utilizarse cuando el test consuma `application-test.yml` o
levante un contexto que necesite su infraestructura. Debe utilizarse normalmente en Service,
Security con contexto, contexto completo, E2E y slices web que requieran beans del perfil. No debe
utilizarse en Model, tests unitarios de Orchestrator, dependencias instanciadas manualmente,
Integrations autocontenidas ni Config con properties explícitas. Estar bajo `src/test` no lo
justifica.

Todo test automatizado que requiera persistencia debe utilizar PostgreSQL real provisionado
obligatoriamente mediante Testcontainers. Esto incluye tests de Repository, tests de integración
de Service y Security, E2E y cualquier otra categoría que requiera persistencia.

Testcontainers debe levantar una instancia PostgreSQL aislada y desechable para la suite. Flyway
debe aplicar sobre ella las migraciones reales del proyecto antes de ejecutar los tests que la
utilicen. Está prohibido sustituir PostgreSQL por H2 u otra base y depender de una instancia
PostgreSQL previamente instalada, configurada en `localhost` o administrada externamente.

La suite debe provisionar por sí misma la base necesaria cuando Docker esté disponible. No debe
depender de la existencia previa de `football_market_test` ni de otra base local. Está prohibido
usar o modificar bases de desarrollo o producción. El esquema probado debe ser el generado por las
migraciones del proyecto, nunca uno creado manualmente de manera divergente.

### 4.13. Tests E2E

Los tests ubicados en `src/test/java/footballmarket/e2e` deben recorrer flujos críticos completos
desde HTTP. Deben levantar un servidor real mediante
`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`, usar el perfil
`test` y realizar requests como un consumidor externo.

El cliente HTTP de los tests E2E debe ser `org.springframework.web.client.RestClient`. No deben
utilizarse `java.net.http.HttpClient`, `MockMvc`, `TestRestTemplate`, `WebTestClient` ni otro cliente
para ejecutar el journey E2E.

Los cuerpos de request deben construirse mediante los Request DTO productivos y entregarse a
`RestClient` para su serialización. Los cuerpos de response deben deserializarse mediante
`RestClient` al Response DTO productivo correspondiente, incluido `ErrorResponseDTO` para errores
contractuales. Las respuestas sin body deben comprobarse como tales sin crear un DTO artificial.
Los tests E2E no deben construir, concatenar ni interpretar JSON manualmente, ni utilizar
`ObjectMapper` como sustituto de los DTO para interactuar con la API.

Servidor Spring, MVC, Security, Controllers, Orchestrators, Services, Repositories, JPA/Hibernate,
base de test e Integrations productivas deben ser reales. Los sistemas externos deben permanecer
controlados: la `base-url` de la Integration real debe apuntar a un servidor simulado y el tráfico
debe atravesar el cliente HTTP real. Nunca deben depender de un proveedor externo real.

Invocar directamente un Service no es E2E; usar `@SpringBootTest` por sí solo tampoco. Un test de
Controller con MockMvc y Service mockeado es de Controller. Los E2E deben proteger pocos journeys
críticos, como registro, login, JWT y acceso protegido, o sincronización y consulta de catálogo, sin
duplicar exhaustivamente combinaciones ya cubiertas en capas específicas.

### 4.14. Mocks y test doubles

Los mocks deben utilizarse solo en fronteras que el alcance del test sustituya deliberadamente, no
por comodidad. Controller usa Service y Orchestrator mock; Service usa Integration mock y
persistencia real; Orchestrator usa Services mock; Integration usa adaptador y cliente reales con
proveedor controlado; E2E usa componentes internos reales con proveedor externo controlado.

Nunca debe mockearse el componente principal bajo prueba ni una dependencia real necesaria para
demostrar el comportamiento objetivo. No debe abusarse de `verify(...)` cuando exista un resultado
observable más estable. Su uso corresponde cuando la interacción sea el comportamiento relevante,
como coordinación o rechazo antes de iniciar un caso de uso.

### 4.15. Assertions y selección de casos

Las assertions deben comprobar resultados significativos: retorno, estado final, excepción, código
y body HTTP, persistencia observable mediante la frontera permitida, invariantes y efectos
observables. No deben acoplarse innecesariamente a detalles privados. Una operación atómica debe
probar el rechazo y la ausencia de cambios parciales. Si el contrato exige campos exactos, debe
verificarse el contrato exacto y no solo una presencia parcial.

Para cada comportamiento deben considerarse, según corresponda, happy path, entradas inválidas,
ausencia de datos, límites, estado inexistente o previo, excepciones, errores externos, vacíos,
duplicados y transiciones. Debe usarse `@ParameterizedTest` cuando múltiples inputs expresen la
misma regla y expectativa; no deben repetirse tests casi idénticos si la parametrización comunica
mejor la intención.

### 4.16. Nombres, legibilidad y estructura

El nombre del test debe expresar condición y resultado esperado. El cuerpo debe conservar una
estructura conceptual Arrange, Act, Assert sin comentarios artificiales. Los helpers pueden reducir
ruido técnico, pero no ocultar la conducta principal; la claridad prevalece sobre abstracciones
excesivas. Los nombres descriptivos pueden estar en español conforme a la sección 9.

La estructura base es:

```text
src/test/java/footballmarket/
├── config/
├── controllers/
├── e2e/
├── integrations/
├── models/
├── orchestrators/
├── repositories/     # solo para comportamiento propio justificado
├── security/
└── services/
```

La ubicación debe representar la responsabilidad bajo prueba. `integrations/` significa
adaptadores externos, no cualquier prueba técnicamente considerada de integración.

Esta estructura no es una lista cerrada. Pueden agregarse paquetes auxiliares como `fixtures/`,
`builders/` y `support/` cuando exista reutilización real que lo justifique. Solo pueden contener
construcción de datos de prueba, fixtures, builders o infraestructura técnica compartida, como el
soporte común de Testcontainers. No constituyen nuevas categorías de tests.

Los helpers compartidos y privados no deben ocultar el comportamiento principal, contener
assertions ni encapsular flujos de negocio que vuelvan ilegible el test. Tampoco pueden eludir las
restricciones de una capa. En particular, un helper utilizado por un test de Service no puede
acceder directa ni indirectamente a Repository, JPA, `EntityManager`, JDBC, SQL o la base de datos.

### 4.17. Criterio para cambios futuros

Ante todo comportamiento nuevo o modificado, el agente debe identificar primero su responsabilidad
arquitectónica y agregar o modificar el test en el nivel más específico capaz de demostrarla. No
debe elegir `@SpringBootTest` automáticamente, crear E2E para reemplazar tests de Model, Service o
Controller, ni agregar tests redundantes solo para elevar cobertura.

Una validación de dominio o negocio corresponde a Model o Service según su responsabilidad; un
status HTTP a Controller; una query compleja a Repository y, si afecta el caso de uso, a Service;
un formato externo a Integration; un cambio JWT a Security y eventualmente al E2E crítico. Un
cambio transversal puede requerir pruebas en más de un nivel.

La suite debe proporcionar simultáneamente localización —un fallo de una responsabilidad debe
señalar un área específica— e integración —los componentes aislados deben probarse también en sus
conexiones relevantes—.

### 4.18. Regresiones y controles

Todo código nuevo o modificado debe contar con tests automatizados suficientes para el comportamiento
afectado. Las correcciones de defectos deben incorporar un test de regresión cuando sea razonable.

Antes de finalizar un cambio deben ejecutarse los tests y la compilación requeridos por el área
modificada. Para considerar correcto un cambio, estos controles obligatorios deben aprobarse.

SonarQube, los Quality Gates y otros controles opcionales pueden ejecutarse como herramientas
complementarias de análisis y calidad, pero sus resultados no condicionan la finalización de un
cambio aunque estén configurados en el proyecto.

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

La ruta de todo endpoint expuesto por un Controller debe comenzar con el prefijo exacto `/api/`.
La ruta `/api` sin barra final no es válida, y tampoco lo son rutas que solo coincidan textualmente
con el comienzo, como `/apiv2`, `/apiary` o `/apifootball`.

### 7.3. Compatibilidad

Antes de modificar un contrato debe evaluarse su impacto sobre los consumidores existentes.

No deben introducirse cambios incompatibles de forma silenciosa.

Agregar, eliminar, modificar obligatoriedad, cambiar significado o alterar errores puede constituir un cambio incompatible.

### 7.4. Validación

La validación debe realizarse en el componente responsable:

- Validación estructural HTTP → Controller / DTO.
- Validación de negocio → Service / Model.
- Validación de contratos externos → Integration.

Las excepciones de validación estructural HTTP lanzadas por Jakarta Validation deben capturarse
desde el manejo de errores de presentación y devolverse mediante `ErrorResponseDTO`.

Las respuestas HTTP de error gestionadas por la aplicación deben utilizar `ErrorResponseDTO`,
con `timestamp`, `status`, `error`, `message` y `path`. El código HTTP de la respuesta y
`status` deben coincidir; `path` debe identificar la ruta solicitada.

Cuando existan múltiples errores de validación, `message` debe contener el primer mensaje
disponible de la validación.

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

### 8.2. Postman

Todo endpoint HTTP funcional nuevo debe incorporarse como solicitud en la colección Postman
versionada del proyecto. Si cambia el contrato de un endpoint existente, su solicitud en Postman
debe actualizarse como parte del mismo cambio.

Cada solicitud debe reflejar el método, la ruta, los parámetros, los headers, la autenticación
y el body que correspondan al contrato. Los ejemplos deben ser utilizables sin incluir secretos
ni credenciales reales.

### 8.3. DTO

Los campos de los DTO que formen parte del contrato deben documentar su significado y, cuando sea relevante, ejemplos y restricciones.

La documentación debe mantenerse consistente con el comportamiento real de la API.

### 8.4. Javadoc del backend

En código productivo del backend, las clases públicas y los métodos `public` y `protected` deben tener Javadoc.
Los métodos `private` deben tenerlo únicamente cuando contengan lógica relevante o su propósito no
sea evidente; los métodos privados triviales quedan exceptuados.

Los constructores declarados explícitamente en código productivo deben tener Javadoc cuando su
comportamiento, parámetros, precondiciones o propósito requieran explicación. Los constructores
triviales, implícitos o generados automáticamente, por ejemplo mediante Lombok, quedan exceptuados.

En código de test, los métodos anotados con `@Test`, `@BeforeEach`, `@AfterEach` u otras anotaciones
de ciclo de vida no requieren Javadoc. Los métodos auxiliares de test solo lo requieren cuando su
propósito o comportamiento necesite una explicación adicional. Modificar únicamente el cuerpo de
un método no obliga a agregar Javadoc si el método pertenece a una categoría exceptuada.

Los métodos sobrescritos deben conservar documentación accesible mediante Javadoc; puede
utilizarse `{@inheritDoc}` cuando el contrato heredado describa completamente el comportamiento.

### 8.5. Consistencia

La documentación del proyecto debe estar escrita en español.

OpenAPI, Postman, Javadoc e implementación deben mantenerse coherentes entre sí y con la Spec correspondiente.

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

### 10.6. Sincronización entre Spec y código

La Spec y el código deben mantenerse sincronizados como parte del mismo cambio.

Si se modifica el comportamiento especificado, deben actualizarse la Spec, la implementación
y los tests afectados para que describan y verifiquen el mismo comportamiento.

Si la implementación revela una diferencia respecto de la Spec, debe resolverse la discrepancia
antes de finalizar el cambio: corregir el código o actualizar la Spec según el comportamiento
acordado. La Spec no debe actualizarse únicamente para justificar una desviación no acordada.

### 10.7. Constitution

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution durante una tarea.

Si una modificación necesaria entra en conflicto con la Constitution, debe proponerse una enmienda antes de realizar el cambio incompatible.

### 10.8. Finalización

Un cambio se considera terminado cuando los elementos directamente afectados son coherentes y los
controles aplicables definidos en la sección 4.18 se ejecutan y aprueban, salvo la excepción de
fallo preexistente definida en esa misma sección.

Cualquier modificación adicional fuera del alcance inicial debe estar justificada explícitamente.

## Technology Constraints

El proyecto utiliza:

Frontend:

- TypeScript.
- React.
- TanStack Query para estado remoto.
- CSS Modules o SCSS Modules para estilos colocalizados cuando corresponda.

Backend:

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
- Spring REST Docs.
- JUnit.
- Mockito.
- AssertJ.
- Testcontainers.
- SonarQube.

Las versiones administradas por Spring Boot deben utilizarse sin sobrescribirlas salvo necesidad técnica justificada.

No deben incorporarse nuevas tecnologías o dependencias fuera de este stack sin justificación y aprobación mediante el proceso de cambios correspondiente.

Las Specs y Plans no deben repetir estas restricciones globales salvo que una funcionalidad requiera una consideración técnica específica.

**Version**: 2.4.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-20

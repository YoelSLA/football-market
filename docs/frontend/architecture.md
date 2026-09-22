# Arquitectura del frontend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md) y luego las secciones pertinentes de este documento. Para conocer o modificar el stack, las versiones o las dependencias del frontend, consultar [tecnologías del frontend](technologies.md). No es necesario leer ese documento ni el área ajena cuando la tarea no los afecte.

## 1. Arquitectura

La unidad principal de organización funcional del frontend debe ser la feature. La arquitectura debe mantener alta cohesión, encapsulación y separación por responsabilidad.

### 1.1. Organización y estructura conceptual

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

Dentro de una feature pueden existir, según su responsabilidad, `components`, `constants`, `form`, `hooks`, `mappers`, `models`, `pages`, `services`, `store`, `types` y `utils`. Su `index.ts` obligatorio se rige por §1.2.

Estas carpetas representan posibilidades, no una estructura obligatoria. No deben crearse carpetas, archivos, roles o abstracciones vacías para uniformar features. La creación o división de Components y demás elementos se rige por las [reglas arquitectónicas comunes](../architecture.md) §1: responde a responsabilidades, cohesión, reutilización o necesidad arquitectónica, no a cantidad de líneas ni tamaño visual.

`app` se reserva para composición y configuración global: router, layouts y providers globales, store global, configuración global de TanStack Query y composición entre features. No debe contener lógica específica de una feature. `main.tsx` debe ser un bootstrap mínimo. Configuraciones globales como `queryClient` deben pertenecer conceptualmente a `app`, no a la raíz de `src` como contenedor arbitrario.

`features` contiene las unidades funcionales. Cada feature debe encapsular sus Pages, Components, Hooks, Services, Models, Mappers, formularios y demás elementos específicos que realmente necesite.

`shared` contiene únicamente elementos genuinamente reutilizables entre features. No deben moverse elementos allí preventivamente por una posible reutilización futura. No puede recibir conceptos específicos de una feature para permitir que otra dependa de ellos ni utilizarse para evadir restricciones entre features.

`infrastructure` contiene adaptadores técnicos globales hacia el entorno, como almacenamiento del navegador. No debe contener lógica funcional o de negocio ni funcionar como contenedor residual. Components, Pages, Hooks y Services no pueden acceder directamente a mecanismos concretos como `localStorage` o `sessionStorage`; todo acceso debe quedar encapsulado por la API pública del adaptador correspondiente de `infrastructure` y sus consumidores deben respetar la matriz de §1.14.

Los estilos específicos de una Page o Component deben permanecer colocalizados con ellos mediante CSS Modules o SCSS Modules cuando corresponda. `src/styles` queda reservado para estilos globales, como base, resets, abstracts, variables y mixins.

### 1.2. Aislamiento y API pública de features

Cada feature debe exponer explícitamente su API pública mediante `index.ts`, aunque actualmente no tenga consumidores externos. Un consumidor externo solo puede importar desde esa API y no desde archivos internos. La API pública debe exportar únicamente lo que corresponda exponer; no obliga a publicar elementos internos. Los módulos internos de la misma feature sí pueden utilizar imports internos.

Una feature no puede depender directa ni indirectamente de otra feature, incluso mediante la API pública de esta última. La composición de múltiples features debe realizarse desde `app` utilizando sus APIs públicas. `app` puede componerlas, pero no absorber su lógica interna.

Se permiten barrels internos, como `index.ts` en `components`, `hooks` u otros submódulos, cuando resulten útiles. No sustituyen la API pública obligatoria de la feature ni pueden utilizarse para eludir restricciones arquitectónicas.

Los imports entre módulos arquitectónicos deben utilizar los aliases del proyecto, por ejemplo `@/features/...`, `@/shared/...`, `@/infrastructure/...` o `@/app/...`. Dentro del mismo módulo o feature pueden utilizarse imports relativos. Ninguna forma de import puede evadir las dependencias permitidas.

Los submódulos de `shared` deben exponer APIs públicas cuando corresponda, de modo que sus consumidores no conozcan arbitrariamente su estructura interna, por ejemplo `@/shared/components`, `@/shared/http` o `@/shared/utils`.

### 1.3. Page

Una Page representa una pantalla asociada a una ruta o a la composición de una funcionalidad. Su responsabilidad principal es componer Components y Hooks.

Puede utilizar Components, Hooks, Models y lógica estrictamente de presentación o renderizado, incluidas decisiones visuales basadas en loading, colecciones vacías u otros estados de UI. No debe contener lógica de negocio ni acceso externo. No puede utilizar directamente Services, HTTP ni DTO: toda operación de aplicación o backend sigue el flujo `Page → Hook → Service`.

No es obligatorio crear un Page Hook específico. Debe existir únicamente cuando haya estado, coordinación o comportamiento de Page suficiente para justificarlo; no debe crearse si solo delegaría en otros Hooks sin aportar una responsabilidad real. En su ausencia, la Page utiliza directamente los Hooks correspondientes sin evitar la frontera con Service.

### 1.4. Component

Un Component representa UI y comportamiento local. Puede utilizar Hooks, Models, estado local y lógica de presentación, incluidos Hooks de su propia feature.

No puede utilizar directamente Services, realizar HTTP ni comunicarse directamente con el backend bajo ninguna circunstancia. Toda operación de aplicación o backend sigue el flujo `Component → Hook → Service`.

### 1.5. Hooks

Hook constituye la frontera obligatoria entre la UI React y Service. Los Hooks pueden subdividirse en `pages`, `queries` y `mutations` según responsabilidades.

Los Page Hooks coordinan estado y comportamiento de una Page cuando la complejidad justifica la extracción. Los Query Hooks encapsulan lecturas de estado remoto mediante TanStack Query. Los Mutation Hooks encapsulan modificaciones de estado remoto mediante TanStack Query.

Queries, mutations, invalidaciones y todo comportamiento asociado a TanStack Query pertenecen exclusivamente a Hooks. Service no puede depender de TanStack Query. Pages y Components no pueden evitar esta frontera utilizando Services directamente. El flujo obligatorio de UI es `Page / Component → Hook → Service`.

### 1.6. Service

Service representa las operaciones externas de una feature y su frontera funcional con el backend. No debe utilizarse como contenedor genérico de lógica.

Los endpoints y URLs propios de la feature deben permanecer en sus Services. Pages, Components y Hooks no pueden definir ni utilizar directamente endpoints HTTP.

Service puede utilizar el cliente HTTP compartido. La lógica de dominio pertenece a Model; la coordinación de UI, a Hook; la transformación, a Mapper; y el acceso externo, a Service.

Un Service puede depender de otro Service de la misma feature únicamente si existe una separación real de responsabilidades. No deben dividirse Services artificialmente para encadenarlos ni pueden existir dependencias circulares.

### 1.7. HTTP compartido

`shared/http` se limita al comportamiento HTTP técnico transversal. Puede resolver base URL, configuración del cliente, headers comunes, interceptores, incorporación técnica de credenciales, normalización de errores de transporte y responsabilidades equivalentes.

No debe contener lógica funcional de una feature, mensajes funcionales de UI ni operaciones de negocio como login o compra de jugadores. Puede normalizar timeout, network errors, status HTTP y otros errores técnicos de transporte. Cada feature debe interpretar sus errores funcionales, como `PLAYER_NOT_AVAILABLE`, `INSUFFICIENT_BALANCE` o `EMAIL_ALREADY_REGISTERED`, y determinar el comportamiento de UI correspondiente. Los errores técnicos y funcionales deben permanecer separados.

### 1.8. DTO, Model y Mapper

DTO representa exclusivamente el contrato HTTP de entrada o salida del backend y debe quedar encapsulado por Service. Pages, Components y Hooks no pueden trabajar con DTO. Service puede utilizar DTO internamente, pero hacia Hooks debe exponer Model, nunca DTO.

DTO y Model deben permanecer conceptual y tipadamente separados aunque tengan exactamente los mismos campos. No puede reutilizarse un DTO como Model ni un Model como DTO para evitar una transformación aparentemente redundante.

Model representa conceptos, estado y comportamiento de dominio utilizados por el frontend. Puede implementarse mediante `type`, `interface`, clase u otra construcción TypeScript sin alterar su responsabilidad. Las reglas resolubles exclusivamente con información del dominio deben implementarse en Model. Model no puede depender de React, Hooks, Services, HTTP, DTO ni infraestructura técnica. Pages, Components y Hooks pueden trabajar con Model.

Mapper realiza exclusivamente transformaciones entre DTO, Model y Form Model. Debe ser puro y no puede realizar HTTP, utilizar Services o Hooks, mantener estado, contener lógica de negocio ni coordinar operaciones. La frontera de Mapper se mantiene aunque origen y destino tengan exactamente la misma estructura: toda transformación `DTO ↔ Model` o `Form Model ↔ DTO` debe pasar por Mapper, incluso cuando sea 1:1. Cada feature debe poseer sus Mappers cuando sean necesarios. Un Mapper compartido solo puede existir para una transformación genuinamente transversal y reutilizable.

Lecturas: `Backend → shared/http → Service → (DTO → Mapper → Model) → Hook → Page / Component`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; DTO no se expone a Hook.

Escrituras: `Component / Form → Form Hook → Form Model → Hook → Service → (Mapper → DTO) → shared/http → Backend`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; ni el Form Hook ni los demás Hooks conocen DTO.

### 1.9. Formularios y validación

Form Model representa el estado y las necesidades de UI de un formulario; DTO representa el contrato HTTP. Deben permanecer conceptual y tipadamente separados aunque tengan exactamente los mismos campos.

`form` puede contener Hooks de formulario, schemas de validación y responsabilidades estrictamente relacionadas con formularios. Los componentes visuales continúan siendo Components de la feature.

Todo formulario debe encapsular su configuración y comportamiento de formulario mediante un Form Hook. Este constituye la frontera propia del formulario para React Hook Form, valores iniciales, schema y comportamiento relacionado cuando corresponda. Su obligatoriedad no justifica crear otros Hooks sin responsabilidad real.

Las validaciones de entrada y UX, como formato de email o campo obligatorio, pueden pertenecer al schema del formulario. Las reglas genuinas del dominio deben pertenecer a Model cuando puedan resolverse con información de dominio; no pueden desplazarse a un schema solo porque el dato provenga de un formulario.

La validación frontend mejora UX y consistencia del cliente, pero nunca sustituye al backend como autoridad de validación y protección de reglas. Las validaciones genéricas y genuinamente reutilizables entre features pueden pertenecer a `shared/validation`.

### 1.10. Estado remoto y stores

TanStack Query es responsable del estado remoto y caché proveniente del backend. Ese estado no debe duplicarse innecesariamente en stores globales o de feature.

`app/store` queda reservado para estado cliente verdaderamente transversal. El hecho de que un dato sea estado no justifica ubicarlo allí. El estado local, como apertura de modales, debe permanecer en el Component, Hook o feature correspondiente cuando no sea global.

Una feature puede definir su propio store cuando exista estado cliente compartido entre múltiples elementos de esa feature sin ser global a la aplicación. El estado específico de una feature no puede trasladarse a `app/store` solo para centralizarlo.

### 1.11. Autenticación

La lógica funcional de autenticación pertenece a la feature de autenticación: login, registro, logout y estado o comportamiento funcional del usuario autenticado.

`shared/http` solo debe resolver el mecanismo HTTP transversal, como incorporar técnicamente el token o tratar respuestas técnicas. El almacenamiento concreto de credenciales o tokens debe encapsularse mediante `infrastructure`. Components, Pages, Hooks y Services no pueden acceder directamente a `localStorage`, `sessionStorage` u otros mecanismos concretos.

### 1.12. Router, layouts y providers

La configuración global del router pertenece exclusivamente a `app/router`. Las Pages concretas pertenecen a sus features y su asociación con rutas globales se realiza desde `app/router`. Una feature puede navegar cuando lo necesite, pero no ensamblar ni poseer el router global.

`app/layouts` contiene exclusivamente layouts globales o transversales, como layouts públicos, autenticados o de aplicación. Una estructura visual específica de una feature debe permanecer dentro de ella aunque visualmente se considere un layout.

Los providers globales deben configurarse y componerse desde `app`, incluidos TanStack Query, router, tema, store global y cualquier otro provider realmente global.

### 1.13. Utils, constants y query keys

`utils` contiene exclusivamente funciones auxiliares puras que no correspondan mejor a otro rol.

No debe funcionar como contenedor residual. Si una operación representa comportamiento de Model, debe permanecer en Model.

`constants` contiene constantes propias de la feature cuando exista una responsabilidad real. No deben extraerse valores allí únicamente para reducir el tamaño visual de otro archivo. Los endpoints y URLs de una feature pertenecen exclusivamente a sus Services y no deben centralizarse en `constants` ni en un catálogo global.

Las query keys de TanStack Query deben centralizarse por feature cuando esta utilice TanStack Query.

No deben dispersarse strings de query keys si existe una definición central correspondiente.

### 1.14. Dependencias frontend

Las dependencias directas permitidas dentro de una feature son:

```text
Page → Component de la misma feature o shared, Hook de la misma feature, Model
Component → Component de la misma feature o shared, Hook de la misma feature, Form Hook, Model,
            Form Model
Hook → Service de la misma feature, Model, Form Model, Form Hook, Feature store
Service → Mapper, Model, DTO, Form Model, shared/http, adaptador público de infrastructure,
          Service de la misma feature
Mapper → Model, DTO, Form Model
Form Hook → Model, Form Model, schema o validación de la misma feature, shared/validation
Form Model o schema → Model, validación de la misma feature, shared/validation
Feature store → Model
Model → ninguna capa técnica o de aplicación
```

Todo formulario debe depender de su Form Hook conforme a §1.9. La fila específica de Form Hook define sus dependencias permitidas y prevalece sobre la fila general de Hook. La dependencia `Service → Service` solo está permitida dentro de la misma feature bajo §1.6. La dependencia `Service → infrastructure` solo permite utilizar la API pública de un adaptador técnico necesario y no autoriza accesos directos al mecanismo concreto. Un Form Hook no puede depender de Query Hooks o Mutation Hooks para evadir sus fronteras; el Hook de aplicación correspondiente recibe el Form Model y coordina la operación con Service.

Una dependencia hacia una API pública de `shared` solo está permitida si su responsabilidad ya está autorizada para el consumidor. Por ejemplo, Page y Component pueden utilizar Components compartidos; Service puede utilizar `shared/http`; Form puede utilizar `shared/validation`; y los roles pueden utilizar utilidades puras compartidas que no introduzcan una dependencia prohibida.

Las dependencias globales permitidas son:

```text
app → APIs públicas de features, APIs públicas de shared, API pública de infrastructure
feature → APIs públicas de shared compatibles con su rol
Service de feature → API pública de infrastructure según la regla anterior
shared/http → API pública de infrastructure técnica cuando sea necesaria
shared → ningún módulo específico de feature
infrastructure → ningún módulo específico de feature ni lógica de negocio
```

Aplican las [reglas arquitectónicas comunes](../architecture.md). Restricciones expresas:

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

La matriz regula dependencias entre roles arquitectónicos y se interpreta con el alcance definido por las [reglas arquitectónicas comunes](../architecture.md) §1. Tipos del lenguaje, APIs estándar y dependencias técnicas compatibles con la responsabilidad del componente pueden utilizarse aunque no aparezcan enumerados.

En el código TypeScript propio del proyecto está prohibido utilizar explícitamente `any`. Deben utilizarse tipos concretos, `unknown` con narrowing u otras alternativas tipadas. Esta regla no exige modificar código generado ni declaraciones externas.

Cuando la documentación vigente no defina una decisión arquitectónica necesaria, se aplica la gobernanza de la constitución: consultar al usuario antes de introducir una regla, capa, patrón, abstracción o ubicación nueva.

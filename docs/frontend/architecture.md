# Arquitectura del frontend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md) y luego las secciones pertinentes de este documento. Las reglas de naming, formato y estilo de implementación están en [convenciones del frontend](conventions.md); las tecnologías, versiones y dependencias, en [tecnologías del frontend](technologies.md). Estos documentos se consultan según el área afectada.

## 1. Arquitectura

La unidad principal de organización funcional del frontend debe ser la feature. La arquitectura debe mantener alta cohesión, encapsulación y separación por responsabilidad.

### 1.1. Organización y estructura conceptual

La organización conceptual de `src/` comprende `app/`, `features/`, `infrastructure/`, `shared/`, `styles/`, `App.tsx` y `main.tsx`.

Dentro de una feature pueden existir, según su responsabilidad, `components/`, `constants/`, `form/`, `hooks/`, `pages/`, `types/` y `utils/`, así como archivos de Service y Mapper en la raíz de la feature. `form/` separa `hooks/` y `schemas/`; `hooks/` separa `mutations/`, `navigation/`, `pages/` y `queries/` según las responsabilidades existentes. Las APIs de las carpetas modulares se rigen por §1.2. Los Models siguen siendo un rol arquitectónico, pero sus definiciones TypeScript se agrupan en `types/models.ts`, sin carpeta física `models/`; `types/` separa `dtos.ts` de `models.ts`.

Estas carpetas representan posibilidades, no una estructura obligatoria. No deben crearse carpetas, archivos, roles o abstracciones vacías para uniformar features. La creación o división de Components y demás elementos se rige por las [reglas arquitectónicas comunes](../architecture.md) §1: responde a responsabilidades, cohesión, reutilización o necesidad arquitectónica, no a cantidad de líneas ni tamaño visual.

`app` se reserva para composición y configuración global: router, layouts y providers globales, store global, configuración global de TanStack Query y composición entre features. No debe contener lógica específica de una feature. `main.tsx` debe ser un bootstrap mínimo. Configuraciones globales como `queryClient` deben pertenecer conceptualmente a `app`, no a la raíz de `src` como contenedor arbitrario.

`features` contiene las unidades funcionales. Cada feature debe encapsular sus Pages, Components, Hooks, Services, Models, Mappers, formularios y demás elementos específicos que realmente necesite.

`shared` contiene únicamente elementos genuinamente reutilizables entre features. No deben moverse elementos allí preventivamente por una posible reutilización futura. No puede recibir conceptos específicos de una feature para permitir que otra dependa de ellos ni utilizarse para evadir restricciones entre features.

`infrastructure` contiene el cliente y los helpers HTTP transversales (`http/`), así como el adaptador del navegador, la persistencia y el estado de sesión (`storage/`). Components, Pages, Hooks y Services no deben acceder directamente a mecanismos concretos como `localStorage` o `sessionStorage`; el acceso queda encapsulado por el adaptador de almacenamiento. Los consumidores respetan la matriz de §1.14.

Los estilos específicos de una Page o Component deben permanecer colocalizados con ellos mediante CSS Modules o SCSS Modules cuando corresponda. `src/styles` queda reservado para estilos globales, como base, resets, abstracts, variables y mixins.

Cada Page se organiza como módulo autocontenido bajo `pages/<Page>/`, con su implementación, estilos exclusivos y `index.ts`. `pages/index.ts` agrega únicamente las APIs de las Pages que corresponde exponer. Los Components independientes siguen el mismo criterio bajo `components/<Component>/`, colocalizando sus archivos propios. Para otras unidades independientes con archivos asociados se aplica la misma organización cuando corresponda, sin crear carpetas por mera uniformidad ni separar archivos que forman una misma responsabilidad. Los recursos compartidos entre varias unidades permanecen en una ubicación común apropiada dentro de su feature, sin duplicarlos ni atribuirlos artificialmente a una de ellas.

### 1.2. Aislamiento y API pública de features

Cada feature debe exponer explícitamente su API pública mediante `index.ts`, aunque actualmente no tenga consumidores externos. Un consumidor externo solo puede importar desde esa API y no desde archivos internos. La API pública debe exportar únicamente lo que corresponda exponer; no obliga a publicar elementos internos.

Una feature no puede depender directa ni indirectamente de otra feature, incluso mediante la API pública de esta última. La composición de múltiples features debe realizarse desde `app` utilizando sus APIs públicas. `app` puede componerlas, pero no absorber su lógica interna.

Cada carpeta modular con código del frontend debe tener un `index.ts` que defina su API para los demás módulos autorizados. Cualquier consumidor situado fuera de esa carpeta importa sus elementos exclusivamente mediante ese `index.ts`, sin deep imports a archivos internos. Dentro de la misma carpeta pueden utilizarse imports directos entre archivos, especialmente para evitar auto-imports y ciclos a través del propio barrel. Esta regla concierne a módulos de código: recursos como hojas de estilo colocalizadas se importan por su ruta. Los directorios contenedores de módulos sin API propia no requieren un barrel vacío.

El `index.ts` de una carpeta interna de una feature expone solo lo necesario a otros módulos permitidos de esa feature; no convierte sus elementos en parte de la API pública externa. El `index.ts` raíz de la feature reexporta desde esas APIs internas únicamente aquello que necesitan consumidores externos autorizados. Ningún barrel permite eludir la matriz de dependencias ni introducir ciclos.

Ninguna forma de import puede evadir las dependencias permitidas. La elección entre aliases e imports relativos se rige por [convenciones](conventions.md).

Los submódulos de `shared` deben exponer APIs públicas cuando corresponda, de modo que sus consumidores no conozcan arbitrariamente su estructura interna.

### 1.3. Page

Una Page representa una pantalla asociada a una ruta o a la composición de una funcionalidad. Su responsabilidad principal es componer Components y Hooks.

Puede utilizar Components, Hooks, Models y lógica estrictamente de presentación o renderizado, incluidas decisiones visuales basadas en loading, colecciones vacías u otros estados de UI. No debe contener lógica de negocio ni acceso externo. No puede utilizar directamente Services, HTTP ni DTO: toda operación de aplicación o backend sigue el flujo `Page → Hook → Service`.

No es obligatorio crear un Page Hook específico. Debe existir únicamente cuando haya estado, coordinación o comportamiento de Page suficiente para justificarlo; no debe crearse si solo delegaría en otros Hooks sin aportar una responsabilidad real. En su ausencia, la Page utiliza directamente los Hooks correspondientes sin evitar la frontera con Service.

### 1.4. Component

Un Component representa UI y comportamiento local. Puede utilizar Hooks, Models, estado local y lógica de presentación, incluidos Hooks de su propia feature.

No puede utilizar directamente Services, realizar HTTP ni comunicarse directamente con el backend bajo ninguna circunstancia. Toda operación de aplicación o backend sigue el flujo `Component → Hook → Service`.

### 1.5. Hooks

Hook constituye la frontera obligatoria entre la UI React y Service. Los Hooks funcionales se organizan, cuando existan esas responsabilidades, en `hooks/pages/`, `hooks/queries/`, `hooks/mutations/`, `hooks/navigation/`. Los Form Hooks permanecen separados en `form/hooks/`. Las query keys pertenecen a `constants/`, no a `hooks/`.

Los Page Hooks coordinan estado y comportamiento de una Page cuando la complejidad justifica la extracción: pueden componer Form Hooks, Query Hooks, Mutation Hooks y APIs de navegación según corresponda. Interpretan los errores funcionales del flujo de UI y entregan estado y acciones a la Page, sin HTTP directo ni DTO. Los Query Hooks encapsulan `useQuery` y la lectura remota mediante Service, sin coordinación de Page. Los Mutation Hooks encapsulan `useMutation` y la operación remota mediante Service, sin formularios, navegación, mensajes ni coordinación de UI.

Los Navigation Hooks coordinan acciones de la feature que incluyen navegación sin representar por ello una operación HTTP. Cada carpeta expone solo los Hooks o elementos realmente necesarios mediante su `index.ts`, sin elevar preventivamente detalles a la API pública de la feature.

Queries, mutations e invalidaciones ordinarias pertenecen a Hooks. Service no puede depender de TanStack Query. Pages y Components no pueden evitar la frontera de Hooks utilizando Services directamente. El flujo obligatorio de UI es `Page / Component → Hook → Service`.

### 1.6. Service

Service representa las operaciones externas de una feature y su frontera funcional con el backend. No debe utilizarse como contenedor genérico de lógica.

Los endpoints y URLs propios de la feature deben permanecer en sus Services. Pages, Components y Hooks no pueden definir ni utilizar directamente endpoints HTTP.

Service puede utilizar el cliente HTTP y las APIs públicas de `infrastructure` cuando necesite mecanismos técnicos externos. La lógica de dominio pertenece a Model; la coordinación de UI, a Hook; la transformación, a Mapper; y el acceso externo, a Service. Por defecto deja propagar los errores de sus dependencias: solo los captura si la operación exige una acción propia de su responsabilidad, nunca para relanzarlos sin cambios ni para traducir HTTP a mensajes de UI.

Un Service puede depender de otro Service de la misma feature únicamente si existe una separación real de responsabilidades. No deben dividirse Services artificialmente para encadenarlos ni pueden existir dependencias circulares.

### 1.7. HTTP de infraestructura

`infrastructure/http` se limita al comportamiento HTTP técnico transversal: base URL, configuración del cliente, headers comunes, interceptores, incorporación técnica de credenciales e inspección genérica de errores. Conserva el `AxiosError` original, sin envolverlo en un `HttpError` propio.

Su `ApiError` representa el contrato global `timestamp`, `status`, `error`, `code`, `message`, `path` del backend. Sus helpers inspeccionan el error sin reemplazarlo ni conocer códigos de features: `code` es identificador estable y `message` es texto presentable, nunca identificador programático. Los Hooks interpretan códigos funcionales de su feature y deciden el comportamiento de UI, con fallback cuando no exista un `ApiError` válido. Cada feature define solo los códigos que necesita en sus propias constantes.

`authenticated: true` marca una solicitud que requiere token. `infrastructure/http` obtiene la credencial mediante callbacks configurados desde `app`, no conoce auth ni su persistencia. Una respuesta 401 de una solicitud autenticada notifica `onUnauthorized` únicamente si la credencial utilizada sigue siendo la actual; la consecuencia corresponde a auth. Sin token, la solicitud no se envía: se notifica y se rechaza con un `Error` técnico local, sin inventar respuesta ni `AxiosError`.

### 1.8. DTO, Model y Mapper

DTO representa exclusivamente el contrato HTTP de entrada o salida del backend y debe quedar encapsulado por Service. Pages, Components y Hooks no pueden trabajar con DTO. Service puede utilizar DTO internamente, pero hacia Hooks debe exponer Model, nunca DTO.

Los contratos HTTP de cada feature se agrupan en `types/dtos.ts`. Las interfaces y los tipos de Model y Form Model se agrupan en `types/models.ts`, sin funciones en ese archivo. `types/index.ts` expone selectivamente ambos contratos a los módulos autorizados; compartir carpeta no equivale a reutilizar un DTO como Model. Por defecto se confía en el contrato tipado; no se replica manualmente su interfaz en validaciones runtime ad hoc de cada Service. Una necesidad real de validación runtime exige una estrategia arquitectónica explícita y consistente. El sufijo de los tipos DTO se rige por [convenciones](conventions.md).

DTO y Model deben permanecer conceptual y tipadamente separados aunque tengan exactamente los mismos campos. No puede reutilizarse un DTO como Model ni un Model como DTO para evitar una transformación aparentemente redundante.

Model representa conceptos y estado de dominio utilizados por el frontend, definidos mediante tipos/interfaces en `types/models.ts`. La ubicación física no altera su rol ni autoriza dependencias de Model hacia React, Hooks, Services, HTTP, DTO o infraestructura técnica. Las operaciones puras sobre un Model que no sean definiciones de tipo pueden vivir en `utils/` de la feature; las reglas de dominio deben permanecer asociadas al concepto Model y no desplazarse a Hooks, Services o DTO. Pages, Components y Hooks pueden trabajar con Model.

Mapper transforma entre DTO, Model y Form Model sin HTTP ni estado. En la implementación actual, `auth.mapper.ts` obtiene la expiración del token mediante una utilidad de `infrastructure/storage` al construir la sesión. La frontera de Mapper se mantiene aunque origen y destino tengan exactamente la misma estructura: toda transformación `DTO ↔ Model` o `Form Model ↔ DTO` debe pasar por Mapper, incluso cuando sea 1:1. Service invoca Mapper dentro de su frontera; no constituye una capa posterior a Service. Cada feature debe poseer sus Mappers cuando sean necesarios. Un Mapper compartido solo puede existir para una transformación genuinamente transversal y reutilizable.

Los Mappers consumen DTO y Model mediante `types/index.ts`, preservando la distinción conceptual entre `types/dtos.ts` y `types/models.ts`.

Lecturas: `Backend → infrastructure/http → Service → (DTO → Mapper → Model) → Hook → Page / Component`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; DTO no se expone a Hook.

Escrituras: `Component / Form → Form Hook → Form Model → Hook → Service → (Mapper → DTO) → infrastructure/http → Backend`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; ni el Form Hook ni los demás Hooks conocen DTO.

### 1.9. Formularios y validación

Form Model representa el estado y las necesidades de UI de un formulario; DTO representa el contrato HTTP. Deben permanecer conceptual y tipadamente separados aunque tengan exactamente los mismos campos.

`form/hooks/` contiene los Form Hooks, uno por formulario; `form/schemas/` contiene los schemas correspondientes. No agrupar formularios distintos en archivos genéricos de Hooks o schemas de toda la feature. `form/hooks/index.ts` expone los Form Hooks, y `form/schemas/index.ts` expone los schemas que consumen esos Hooks. `form/index.ts` publica para el resto de la feature solo lo que necesita, normalmente los Form Hooks, sin reexportar schemas internos por defecto. Los componentes visuales continúan siendo Components de la feature.

Todo formulario debe encapsular su configuración y comportamiento de formulario mediante un Form Hook en `form/hooks/`: `useForm`, integración con `zodResolver`, valores iniciales, estado y manejo de submit propio del formulario cuando corresponda. Su obligatoriedad no justifica crear otros Hooks sin responsabilidad real. Los Hooks funcionales de la feature permanecen en `hooks/` y no asumen esta responsabilidad.

Los schemas de `form/schemas/` definen validaciones de entrada y UX, como formato de email, campo obligatorio y coherencia entre campos cuando corresponda al formulario. Las reglas genuinas del dominio deben pertenecer a Model cuando puedan resolverse con información de dominio; no pueden desplazarse a un schema solo porque el dato provenga de un formulario. Los Form Hooks importan los schemas mediante `form/schemas/index.ts`; los consumidores fuera de `form` importan los Form Hooks mediante `form/index.ts`, sin acceder a sus carpetas internas.

La validación frontend mejora UX y consistencia del cliente, pero nunca sustituye al backend como autoridad de validación y protección de reglas. Las validaciones genéricas y genuinamente reutilizables entre features pueden pertenecer a `shared/validation`.

### 1.10. Estado remoto y stores

TanStack Query es responsable del estado remoto y caché proveniente del backend. Ese estado no debe duplicarse innecesariamente en stores globales o de feature.

Al restaurar, iniciar o finalizar una sesión, el store cancela las consultas pendientes y limpia la Query Cache mediante el QueryClient configurado desde `app`, para que no sobrevivan datos remotos de otra identidad.

`app/store` queda reservado para estado cliente verdaderamente transversal. El hecho de que un dato sea estado no justifica ubicarlo allí. El estado local, como apertura de modales, debe permanecer en el Component, Hook o feature correspondiente cuando no sea global.

Una feature puede definir su propio store cuando exista estado cliente compartido entre múltiples elementos de esa feature sin ser global a la aplicación. El estado específico de una feature no puede trasladarse a `app/store` solo para centralizarlo.

### 1.11. Autenticación

La feature de autenticación implementa login, registro, consulta del usuario actual y navegación de logout. El estado y la persistencia de la sesión se encuentran actualmente en `infrastructure/storage`.

`infrastructure/http` resuelve el mecanismo HTTP transversal, como incorporar técnicamente el token o tratar respuestas técnicas. El almacenamiento concreto de credenciales o tokens debe encapsularse mediante `infrastructure`. Components, Pages, Hooks y Services no pueden acceder directamente a `localStorage`, `sessionStorage` u otros mecanismos concretos.

La sesión conserva el token en memoria y en `localStorage` mediante `infrastructure/storage/authSessionStorage`; la expiración se obtiene del JWT. `infrastructure/storage/useAuthStore` usa Zustand para las transiciones `UNKNOWN → CHECKING → AUTHENTICATED` o `ANONYMOUS` y para el error de verificación. Una sesión restaurada o iniciada permanece en `CHECKING` hasta que `/auth/me` confirme el usuario. `app/providers/SessionLifecycle` restaura la sesión, coordina el Query Hook de auth y comprueba la expiración mediante temporizador, foco y cambio de visibilidad; el usuario actual remoto no se duplica en el store. `app/providers/HttpCredentials` conecta el token y el fin de sesión con `infrastructure/http`; `app/router/AuthGuard` consulta el estado para controlar rutas.

### 1.12. Router, layouts y providers

La configuración global del router pertenece exclusivamente a `app/router`. Las Pages concretas pertenecen a sus features y su asociación con rutas globales se realiza desde `app/router`. Una feature puede navegar cuando lo necesite, pero no ensamblar ni poseer el router global.

`app/layouts` contiene exclusivamente layouts globales o transversales, como layouts públicos, autenticados o de aplicación. Una estructura visual específica de una feature debe permanecer dentro de ella aunque visualmente se considere un layout.

Los providers globales deben configurarse y componerse desde `app`, incluidos TanStack Query, router, tema, store global y cualquier otro provider realmente global.

### 1.13. Utils, constants y query keys

`utils` contiene exclusivamente funciones auxiliares puras que no correspondan mejor a otro rol.

No debe funcionar como contenedor residual. Las operaciones puras sobre los Models definidos como tipos en `types/models.ts` pueden ubicarse aquí cuando corresponda, separadas de las utilidades de procesamiento técnico según su responsabilidad. Los módulos externos a `utils/` las consumen mediante `utils/index.ts`.

`constants` contiene constantes propias de la feature cuando exista una responsabilidad real. No deben extraerse valores allí únicamente para reducir el tamaño visual de otro archivo. Los endpoints y URLs de una feature pertenecen exclusivamente a sus Services y no deben centralizarse en `constants` ni en un catálogo global.

Las query keys de TanStack Query deben centralizarse por feature en `constants/` cuando esta utilice TanStack Query.

No deben dispersarse strings de query keys si existe una definición central correspondiente.

### 1.14. Dependencias frontend

Las dependencias directas permitidas dentro de una feature son:

| Origen | Destinos permitidos |
| --- | --- |
| Page | Component de la misma feature o shared, Page Hook o Navigation Hook de la misma feature, Model. |
| Component | Component de la misma feature o shared, Hook de la misma feature, Form Hook, Model, Form Model, estado de sesión de `infrastructure/storage` para la pantalla de verificación. |
| Page Hook | Form Hook, Query Hook, Mutation Hook, Navigation Hook de la misma feature, Model, Form Model, estado de sesión de `infrastructure/storage`, `infrastructure/http` para inspección genérica de errores. |
| Query Hook | Service de la misma feature, Model. |
| Mutation Hook | Service de la misma feature, Model, Form Model. |
| Navigation Hook | Hook de la misma feature, Model, estado de sesión de `infrastructure/storage`. |
| Service | Mapper, Model, DTO, Form Model, `infrastructure/http`, adaptador público de `infrastructure`, Service de la misma feature. |
| Mapper | Model, DTO, Form Model, utilidad de expiración de `infrastructure/storage` para la sesión. |
| Form Hook | Model, Form Model, schema o validación de la misma feature, `shared/validation`. |
| Form Model o schema | Model, validación de la misma feature, `shared/validation`. |
| Store de sesión de infraestructura | Persistencia de sesión de `infrastructure/storage`. |
| Model | Ninguna capa técnica o de aplicación. |

Todo formulario debe depender de su Form Hook conforme a §1.9. La fila específica de Form Hook, ubicado en `form/hooks/`, define sus dependencias permitidas; `form/schemas/` cumple el rol de schema de la matriz. La dependencia `Service → Service` solo está permitida dentro de la misma feature bajo §1.6. La dependencia `Service → infrastructure` solo permite utilizar la API pública de un adaptador técnico necesario y no autoriza accesos directos al mecanismo concreto. Un Form Hook no puede depender de Query Hooks o Mutation Hooks para evadir sus fronteras; el Page Hook correspondiente recibe el Form Model y coordina la operación con el Mutation Hook.

`Hook → Hook` dentro de la misma feature se permite por composición o coordinación real, no simplemente para reutilizar una request. Form Hook conserva su responsabilidad específica y no encadena arbitrariamente Query/Mutation Hooks.

Una dependencia hacia una API pública de `shared` solo está permitida si su responsabilidad ya está autorizada para el consumidor. Page y Component pueden utilizar Components compartidos; Service puede utilizar `infrastructure/http`; Form puede utilizar `shared/validation`; y los roles pueden utilizar utilidades puras compartidas que no introduzcan una dependencia prohibida.

Las dependencias globales permitidas son:

| Origen | Destinos permitidos |
| --- | --- |
| `app` | APIs públicas de features y `shared`, API pública de `infrastructure`. |
| Feature | APIs públicas de `shared` compatibles con su rol y de `infrastructure` conforme a la matriz anterior. |
| Service de feature | API pública de `infrastructure` según la regla anterior. |
| `infrastructure/http` | API pública de `infrastructure/storage` cuando sea necesaria. |
| `shared` | Ningún módulo específico de feature. |
| `infrastructure` | Ningún módulo específico de feature; el store de sesión utiliza el adaptador de almacenamiento del mismo módulo. |

Aplican las [reglas arquitectónicas comunes](../architecture.md). Restricciones expresas:

-   Page o Component no puede depender de Service, HTTP ni DTO.

-   Hook no puede depender de DTO ni definir endpoints HTTP.

-   Service no puede depender de React, Component, Page, Hook ni
    TanStack Query.

-   Model no puede depender de React, Hook, Service, HTTP, DTO ni
    infrastructure.

-   Mapper no puede depender de Service, Hook, HTTP ni React; el mapper de sesión utiliza la utilidad de expiración de `infrastructure/storage`.

-   una feature no puede depender de otra feature;

-   `shared` e `infrastructure` no pueden depender de features;

-   ningún módulo puede utilizar `app`, barrels o intermediarios para
    evadir estas reglas.

La matriz regula dependencias entre roles arquitectónicos y se interpreta con el alcance definido por las [reglas arquitectónicas comunes](../architecture.md) §1. Tipos del lenguaje, APIs estándar y dependencias técnicas compatibles con la responsabilidad del componente pueden utilizarse aunque no aparezcan enumerados.

En esta matriz `Model`, `Form Model` y `DTO` son roles conceptuales distintos aunque sus definiciones se expongan mediante `types/index.ts`: `types/models.ts` contiene los primeros y `types/dtos.ts` los contratos HTTP. Los imports y reexports no autorizan a ningún consumidor a acceder a DTO fuera del límite de Service y Mapper.

Cuando la documentación vigente no defina una decisión arquitectónica necesaria, se aplica la gobernanza de la constitución: consultar al usuario antes de introducir una regla, capa, patrón, abstracción o ubicación nueva.

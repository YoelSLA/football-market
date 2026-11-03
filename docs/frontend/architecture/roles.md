# Roles y flujos del frontend

Responsabilidades de UI, Hooks, acceso externo, contratos de datos, formularios y auxiliares de feature.

Consultar el [punto de entrada](../architecture.md), la [interpretación normativa única](../architecture.md#interpretacion-normativa) y las [observaciones de consistencia](../architecture.md#consistencia). Las matrices de [dependencias](dependencies.md) se consultan junto con el rol afectado.

## Índice

- [Artículo 10.º — Responsabilidades de Page](#articulo-10)
- [Artículo 11.º — Responsabilidades de Component](#articulo-11)
- [Artículo 12.º — Organización y frontera de Hooks](#articulo-12)
- [Artículo 13.º — Especialización de Hooks](#articulo-13)
- [Artículo 14.º — Responsabilidades de Service](#articulo-14)
- [Artículo 15.º — HTTP transversal y errores](#articulo-15)
- [Artículo 16.º — Credenciales de solicitudes autenticadas](#articulo-16)
- [Artículo 17.º — DTO y contratos tipados](#articulo-17)
- [Artículo 18.º — Separación y responsabilidades de Model](#articulo-18)
- [Artículo 19.º — Transformaciones mediante Mapper](#articulo-19)
- [Artículo 20.º — Form Model y estructura de formularios](#articulo-20)
- [Artículo 21.º — Configuración mediante Form Hook](#articulo-21)
- [Artículo 22.º — Schemas y autoridad de validación](#articulo-22)
- [Artículo 38.º — Funciones auxiliares puras](#articulo-38)
- [Artículo 39.º — Fuentes de datos simulados](#articulo-39)
- [Artículo 40.º — Constants y query keys](#articulo-40)

## CAPÍTULO PRIMERO — UI y Hooks

<a id="articulo-10"></a>
### Artículo 10.º — Responsabilidades de Page

Una Page representa una pantalla asociada a una ruta o a la composición de una funcionalidad. Su responsabilidad principal es componer Components y Hooks.

**MAY:** Una Page utiliza Components, Hooks, Models y lógica estrictamente de presentación o renderizado, incluidas decisiones visuales basadas en loading, colecciones vacías u otros estados de UI.

**MUST NOT:** Una Page contiene lógica de negocio o acceso externo, o utiliza directamente Services, HTTP o DTO.

**MUST:** Toda operación de aplicación o backend sigue el flujo `Page → Hook → Service`.

No es obligatorio crear un Page Hook específico.

**MUST:** Un Page Hook existe únicamente cuando hay estado, coordinación o comportamiento de Page suficiente para justificarlo. En su ausencia, la Page utiliza directamente los Hooks correspondientes sin evitar la frontera con Service.

**MUST NOT:** Se crea un Page Hook si solo delegaría en otros Hooks sin aportar una responsabilidad real.

<a id="articulo-11"></a>
### Artículo 11.º — Responsabilidades de Component

Un Component representa UI y comportamiento local.

**MAY:** Un Component utiliza Hooks, Models, estado local y lógica de presentación, incluidos Hooks de su propia feature.

**MUST NOT:** Un Component utiliza directamente Services, realiza HTTP o se comunica directamente con el backend bajo ninguna circunstancia.

**MUST:** Toda operación de aplicación o backend sigue el flujo `Component → Hook → Service`.

**MUST:** Cada Component declara sus propias props en su archivo y expone solo lo que necesita. Sus estilos se encapsulan en su SCSS Module y alcanzan únicamente sus propios internos.

**MUST NOT:** Un Component recibe por props responsabilidades que le pertenecen a otro rol o estiliza la estructura interna de otro Component, que se estiliza desde su propio módulo.

<a id="articulo-12"></a>
### Artículo 12.º — Organización y frontera de Hooks

**MUST:** Hook constituye la frontera entre la UI React y Service. Los Hooks funcionales se organizan, cuando existan esas responsabilidades, en `hooks/pages/`, `hooks/queries/`, `hooks/mutations/`, `hooks/navigation/`. Los Form Hooks permanecen separados en `form/hooks/`. Las query keys pertenecen a `constants/`, no a `hooks/`.

<a id="articulo-13"></a>
### Artículo 13.º — Especialización de Hooks

Los Page Hooks coordinan estado y comportamiento de una Page cuando la complejidad justifica la extracción.

**MAY:** Los Page Hooks componen Form Hooks, Query Hooks, Mutation Hooks y APIs de navegación según corresponda.

**MUST:** Los Page Hooks interpretan los errores funcionales del flujo de UI y entregan estado y acciones a la Page, sin HTTP directo ni DTO. Los Query Hooks encapsulan `useQuery` y la lectura remota mediante Service, sin coordinación de Page. Los Mutation Hooks encapsulan `useMutation` y la operación remota mediante Service, sin formularios, navegación, mensajes ni coordinación de UI.

**MUST:** Los Navigation Hooks coordinan acciones de la feature que incluyen navegación sin representar por ello una operación HTTP. Cada carpeta expone solo los Hooks o elementos realmente necesarios mediante su `index.ts`, sin elevar preventivamente detalles a la API pública de la feature.

**MUST:** Queries, mutations e invalidaciones ordinarias pertenecen a Hooks. El flujo de UI es `Page / Component → Hook → Service`.

**MUST NOT:** Service depende de TanStack Query. Pages y Components evitan la frontera de Hooks utilizando Services directamente.

## CAPÍTULO SEGUNDO — Acceso externo y contratos

<a id="articulo-14"></a>
### Artículo 14.º — Responsabilidades de Service

Service representa las operaciones externas de una feature y su frontera funcional con el backend.

**MUST NOT:** Service se utiliza como contenedor genérico de lógica.

**MUST:** Los endpoints y URLs propios de la feature permanecen en sus Services.

**MUST NOT:** Pages, Components y Hooks definen o utilizan directamente endpoints HTTP.

**MAY:** Service utiliza el cliente HTTP y las APIs públicas de `infrastructure` cuando necesita mecanismos técnicos externos.

**MUST:** La lógica de dominio pertenece a Model; la coordinación de UI, a Hook; la transformación, a Mapper; y el acceso externo, a Service. Service deja propagar por defecto los errores de sus dependencias: solo los captura si la operación exige una acción propia de su responsabilidad.

**MUST NOT:** Service captura errores para relanzarlos sin cambios o para traducir HTTP a mensajes de UI.

**MAY:** Un Service depende de otro Service de la misma feature únicamente si existe una separación real de responsabilidades.

**MUST NOT:** Se dividen Services artificialmente para encadenarlos o se introducen dependencias circulares.

<a id="articulo-15"></a>
### Artículo 15.º — HTTP transversal y errores

**MUST:** `infrastructure/http` se limita al comportamiento HTTP técnico transversal: base URL, configuración del cliente, headers comunes, interceptores, incorporación técnica de credenciales e inspección genérica de errores. Conserva el `AxiosError` original, sin envolverlo en un `HttpError` propio.

Su `ApiError` representa el contrato global `timestamp`, `status`, `error`, `code`, `message`, `path` del backend.

**MUST:** Sus helpers inspeccionan el error sin reemplazarlo ni conocer códigos de features: `code` es identificador estable y `message` es texto presentable, nunca identificador programático. Los Hooks interpretan códigos funcionales de su feature y deciden el comportamiento de UI, con fallback cuando no exista un `ApiError` válido. Cada feature define solo los códigos que necesita en sus propias constantes.

<a id="articulo-16"></a>
### Artículo 16.º — Credenciales de solicitudes autenticadas

`authenticated: true` marca una solicitud que requiere token.

**MUST:** `infrastructure/http` obtiene la credencial mediante callbacks configurados desde `app`, sin conocer auth ni su persistencia. Una respuesta 401 de una solicitud autenticada notifica `onUnauthorized` únicamente si la credencial utilizada sigue siendo la actual; la consecuencia corresponde a auth. Sin token, la solicitud no se envía: se notifica y se rechaza con un `Error` técnico local, sin inventar respuesta ni `AxiosError`.

<a id="articulo-17"></a>
### Artículo 17.º — DTO y contratos tipados

DTO representa exclusivamente el contrato HTTP de entrada o salida del backend.

**MUST:** DTO queda encapsulado por Service. Service expone Model hacia Hooks, nunca DTO.

**MUST NOT:** Pages, Components y Hooks trabajan con DTO.

**MAY:** Service utiliza DTO internamente.

**MUST:** Los contratos HTTP de cada feature se agrupan en `types/dtos.ts`. Las interfaces y los tipos de Model y Form Model se agrupan en `types/models.ts`, sin funciones en ese archivo. `types/index.ts` expone selectivamente ambos contratos a los módulos autorizados. Por defecto se confía en el contrato tipado. Una necesidad real de validación runtime exige una estrategia arquitectónica explícita y consistente.

**MUST NOT:** Se replica manualmente su interfaz en validaciones runtime ad hoc de cada Service.

Compartir carpeta no equivale a reutilizar un DTO como Model. El sufijo de los tipos DTO se rige por [convenciones](../conventions.md).

<a id="articulo-18"></a>
### Artículo 18.º — Separación y responsabilidades de Model

**MUST:** DTO y Model permanecen conceptual y tipadamente separados aunque tengan exactamente los mismos campos.

**MUST NOT:** Se reutiliza un DTO como Model o un Model como DTO para evitar una transformación aparentemente redundante.

Model representa conceptos y estado de dominio utilizados por el frontend, definidos mediante tipos/interfaces en `types/models.ts`. La ubicación física no altera su rol ni autoriza dependencias de Model hacia React, Hooks, Services, HTTP, DTO o infraestructura técnica.

**MAY:** Las operaciones puras sobre un Model que no sean definiciones de tipo viven en `utils/` de la feature. Pages, Components y Hooks trabajan con Model.

**MUST:** Las reglas de dominio permanecen asociadas al concepto Model y no se desplazan a Hooks, Services o DTO.

<a id="articulo-19"></a>
### Artículo 19.º — Transformaciones mediante Mapper

**MUST:** Mapper transforma entre DTO, Model y Form Model sin HTTP ni estado.

Descripción de la implementación de sesión: véase [Mapper de sesión](implementation.md#mapper-de-sesion).

**MUST:** Toda transformación `DTO ↔ Model` o `Form Model ↔ DTO` pasa por Mapper, incluso cuando sea 1:1: la frontera se mantiene aunque origen y destino tengan exactamente la misma estructura. Service invoca Mapper dentro de su frontera; no constituye una capa posterior a Service. Cada feature posee sus Mappers cuando sean necesarios.

**MAY:** Un Mapper compartido existe solo para una transformación genuinamente transversal y reutilizable.

**MUST:** Los Mappers consumen DTO y Model mediante `types/index.ts`, preservando la distinción conceptual entre `types/dtos.ts` y `types/models.ts`.

Lecturas: `Backend → infrastructure/http → Service → (DTO → Mapper → Model) → Hook → Page / Component`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; DTO no se expone a Hook.

Escrituras: `Component / Form → Form Hook → Form Model → Hook → Service → (Mapper → DTO) → infrastructure/http → Backend`.

La transformación entre paréntesis ocurre dentro de la frontera de Service; ni el Form Hook ni los demás Hooks conocen DTO.

## CAPÍTULO TERCERO — Formularios y validación

<a id="articulo-20"></a>
### Artículo 20.º — Form Model y estructura de formularios

Form Model representa el estado y las necesidades de UI de un formulario; DTO representa el contrato HTTP.

**MUST:** Form Model y DTO permanecen conceptual y tipadamente separados aunque tengan exactamente los mismos campos.

**MUST:** `form/hooks/` contiene los Form Hooks, uno por formulario; `form/schemas/` contiene los schemas correspondientes. `form/hooks/index.ts` expone los Form Hooks, y `form/schemas/index.ts` expone los schemas que consumen esos Hooks. `form/index.ts` publica para el resto de la feature solo lo que necesita, normalmente los Form Hooks, sin reexportar schemas internos por defecto. Los componentes visuales continúan siendo Components de la feature.

**MUST NOT:** Se agrupan formularios distintos en archivos genéricos de Hooks o schemas de toda la feature.

<a id="articulo-21"></a>
### Artículo 21.º — Configuración mediante Form Hook

**MUST:** Todo formulario encapsula su configuración y comportamiento de formulario mediante un Form Hook en `form/hooks/`: `useForm`, integración con `zodResolver`, valores iniciales, estado y manejo de submit propio del formulario cuando corresponda. Los Hooks funcionales de la feature permanecen en `hooks/` y no asumen esta responsabilidad.

Su obligatoriedad no justifica crear otros Hooks sin responsabilidad real.

<a id="articulo-22"></a>
### Artículo 22.º — Schemas y autoridad de validación

**MUST:** Los schemas de `form/schemas/` definen validaciones de entrada y UX, como formato de email, campo obligatorio y coherencia entre campos cuando corresponda al formulario. Las reglas genuinas del dominio pertenecen a Model cuando puedan resolverse con información de dominio. Los Form Hooks importan los schemas mediante `form/schemas/index.ts`; los consumidores fuera de `form` importan los Form Hooks mediante `form/index.ts`, sin acceder a sus carpetas internas.

**MUST NOT:** Las reglas genuinas del dominio se desplazan a un schema solo porque el dato proviene de un formulario.

La validación frontend mejora UX y consistencia del cliente.

**MUST NOT:** La validación frontend sustituye al backend como autoridad de validación y protección de reglas.

**MAY:** Las validaciones genéricas y genuinamente reutilizables entre features pertenecen a `shared/validation`.

## CAPÍTULO CUARTO — Utilidades y constantes

<a id="articulo-38"></a>
### Artículo 38.º — Funciones auxiliares puras

**MUST:** `utils` contiene exclusivamente funciones auxiliares puras que no correspondan mejor a otro rol.

**MUST NOT:** `utils` funciona como contenedor residual.

**MAY:** Las operaciones puras sobre los Models definidos como tipos en `types/models.ts` se ubican aquí cuando corresponda, separadas de las utilidades de procesamiento técnico según su responsabilidad.

**MUST:** Los módulos externos a `utils/` las consumen mediante `utils/index.ts`.

<a id="articulo-39"></a>
### Artículo 39.º — Fuentes de datos simulados

**MUST:** Las fuentes de datos simulados de una feature viven en `demo/`, con funciones puras deterministas que no dependen del reloj ni de generadores aleatorios, y se consumen desde la capa de Hooks igual que cualquier otra lectura.

**MUST NOT:** Los datos simulados se transportan por la API o se persisten.

Su sustitución futura solo cambia esa capa.

<a id="articulo-40"></a>
### Artículo 40.º — Constants y query keys

**MUST:** `constants` contiene constantes propias de la feature cuando existe una responsabilidad real. Los endpoints y URLs de una feature pertenecen exclusivamente a sus Services.

**MUST NOT:** Se extraen valores a `constants` únicamente para reducir el tamaño visual de otro archivo. Se centralizan endpoints y URLs de una feature en `constants` o en un catálogo global.

**MUST:** Las query keys de TanStack Query se centralizan por feature en `constants/` cuando esta utiliza TanStack Query.

**MUST NOT:** Se dispersan strings de query keys si existe una definición central correspondiente.

# Estado y composición global del frontend

Contratos de estado, almacenamiento, router, providers, layouts y navegación global. Las descripciones concretas se separan en implementación actual.

Consultar el [punto de entrada](../architecture.md), la [interpretación normativa única](../architecture.md#interpretacion-normativa) y las [observaciones de consistencia](../architecture.md#consistencia). Las matrices de [dependencias](dependencies.md) se consultan junto con el rol afectado.

## Índice

- [Artículo 23.º — Estado remoto y limpieza de caché](#articulo-23)
- [Artículo 24.º — Alcance de los stores y estado local](#articulo-24)
- [Artículo 25.º — Autenticación y almacenamiento](#articulo-25)
- [Artículo 26.º — Router global y navegación de features](#articulo-26)
- [Artículo 27.º — Layouts y providers globales](#articulo-27)
- [Artículo 29.º — Composición de navegación global](#articulo-29)
- [Artículo 30.º — Catálogo de layouts y Components compartidos](#articulo-30)

## CAPÍTULO PRIMERO — Estado y autenticación

<a id="articulo-23"></a>
### Artículo 23.º — Estado remoto y limpieza de caché

**MUST:** TanStack Query es responsable del estado remoto y caché proveniente del backend.

**MUST NOT:** Ese estado se duplica innecesariamente en stores globales o de feature.

**MUST:** Al restaurar una sesión válida, iniciar una sesión o finalizarla, el store cancela las consultas pendientes y limpia la Query Cache mediante el QueryClient configurado desde `app`, para que no sobrevivan datos remotos de otra identidad. Una restauración sin sesión válida pasa directamente a estado anónimo y no ejecuta esa limpieza.

<a id="articulo-24"></a>
### Artículo 24.º — Alcance de los stores y estado local

**MUST:** `app/store` queda reservado para estado cliente verdaderamente transversal. El estado local, como apertura de modales, permanece en el Component, Hook o feature correspondiente cuando no sea global.

El hecho de que un dato sea estado no justifica ubicarlo allí.

**MAY:** Una feature define su propio store cuando existe estado cliente compartido entre múltiples elementos de esa feature sin ser global a la aplicación.

**MUST NOT:** El estado específico de una feature se traslada a `app/store` solo para centralizarlo.

<a id="articulo-25"></a>
### Artículo 25.º — Autenticación y almacenamiento

`infrastructure/http` resuelve el mecanismo HTTP transversal, como incorporar técnicamente el token o tratar respuestas técnicas.

**MUST:** El almacenamiento concreto de credenciales o tokens se encapsula mediante `infrastructure`.

**MUST NOT:** Components, Pages, Hooks y Services acceden directamente a `localStorage`, `sessionStorage` u otros mecanismos concretos.

Descripción de la implementación: véase [Sesión y autenticación](implementation.md#sesion-y-autenticacion).

## CAPÍTULO SEGUNDO — Router, layouts y navegación

<a id="articulo-26"></a>
### Artículo 26.º — Router global y navegación de features

**MUST:** La configuración global del router pertenece exclusivamente a `app/router`. Las Pages concretas pertenecen a sus features y su asociación con rutas globales se realiza desde `app/router`.

**MAY:** Una feature navega cuando lo necesita.

**MUST NOT:** Una feature ensambla o posee el router global.

<a id="articulo-27"></a>
### Artículo 27.º — Layouts y providers globales

**MUST:** `app/layouts` contiene exclusivamente layouts globales o transversales, como layouts públicos, autenticados o de aplicación. Una estructura visual específica de una feature permanece dentro de ella aunque visualmente se considere un layout.

**MUST:** Los providers globales se configuran y componen desde `app`, incluidos TanStack Query, router, tema, store global y cualquier otro provider realmente global.

<a id="articulo-29"></a>
### Artículo 29.º — Composición de navegación global

**MUST:** La navegación global se compone exclusivamente desde `app/layouts/AppLayout`, que resuelve la identidad de sesión y el cierre de sesión ya existentes en `features/auth` mediante sus APIs públicas y los entrega a `shared/components/Navbar`. La configuración declarativa vive en `app/navigation/sections.ts`; `AppLayout` entrega las secciones, `location`, `userName`, `onLogout` y el resolver `resolveActiveNavigation`, cuya implementación permanece en `app/navigation/activeNavigation.ts`. `Navbar` invoca ese resolver con `location.pathname` para derivar la sección y pestaña activas, sin estado duplicado ni imports hacia `app` o features. El contrato `NavigationResolver` y su resultado `ActiveNavigation` se exponen desde la API pública del Navbar.

Rutas que utilizan esta composición: véase [Navegación actual](implementation.md#navegacion-actual).

<a id="articulo-30"></a>
### Artículo 30.º — Catálogo de layouts y Components compartidos

El catálogo de layouts y sus responsabilidades es el siguiente; su ubicación actual se documenta en [Ubicación de layouts](implementation.md#ubicacion-de-layouts):

| Layout | Responsabilidad |
| --- | --- |
| `AuthLayout` | Composición visual de las páginas públicas de autenticación. |
| `AuthGuard` | Guarda de rutas públicas y privadas según el estado de sesión, con `SessionChecking` mientras la sesión se resuelve. |
| `PageLayout` | Contenedor de página con cabecera opcional, área de contenido y `aria-busy` durante la carga. |
| `PaginatedPageLayout` | Especialización de `PageLayout` que compone `shared/components/Pagination` cuando existe paginación, reutilizando el componente compartido. |
| `AppLayout` | Layout de las rutas privadas: navbar global y `Outlet`. |

`shared/components` contiene los componentes reutilizables entre layouts y features: `Button`, `Field`, `Navbar` y `Pagination`. `Pagination` expone una ventana configurable mediante su propiedad `variant` y recibe `page`, `totalPages` y `onChange`; no conoce ninguna feature ni consulta datos.

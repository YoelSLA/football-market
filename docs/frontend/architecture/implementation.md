# Implementación actual del frontend

Documento informativo: conserva descripciones concretas, no ejemplos genéricos ni nuevas disposiciones. Los contratos aplicables se consultan desde el [punto de entrada](../architecture.md), con su [interpretación normativa](../architecture.md#interpretacion-normativa). Las [observaciones de consistencia](../architecture.md#consistencia) distinguen correcciones documentales y conflictos pendientes.

## Índice

- [Mapper de sesión](#mapper-de-sesion)
- [Sesión y autenticación](#sesion-y-autenticacion)
- [Artículo 28.º — Rutas y comportamiento actual del guard](#articulo-28)
- [Navegación actual](#navegacion-actual)
- [Ubicación de layouts](#ubicacion-de-layouts)
- [Estilos de la implementación](#estilos-de-la-implementacion)
- [Recursos de la implementación](#recursos-de-la-implementacion)

<a id="mapper-de-sesion"></a>
## Mapper de sesión

En la implementación actual, `auth.mapper.ts` obtiene la expiración del token mediante una utilidad de `infrastructure/storage` al construir la sesión.

Contrato relacionado: [Artículo 19.º](roles.md#articulo-19).

<a id="sesion-y-autenticacion"></a>
## Sesión y autenticación

La feature de autenticación implementa login, registro, consulta del usuario actual y navegación de logout. El estado y la persistencia de la sesión se encuentran actualmente en `infrastructure/storage`.

La sesión conserva el token en memoria y en `localStorage` mediante `infrastructure/storage/authSessionStorage`; la expiración se obtiene del JWT. `infrastructure/storage/useAuthStore` usa Zustand para las transiciones `UNKNOWN → CHECKING → AUTHENTICATED` o `ANONYMOUS` y conserva el error de verificación. Una sesión restaurada o iniciada permanece en `CHECKING` hasta que `/auth/me` confirme el usuario. Una sesión ya autenticada vuelve temporalmente a `CHECKING` mientras se revalida después de una navegación. Si la verificación falla sin finalizar la sesión, el store conserva `CHECKING` y publica `verificationError`, que muestra `SessionChecking`. `app/providers/SessionLifecycle` restaura la sesión, coordina el Query Hook de auth, revalida `/auth/me` después de cada navegación y comprueba la expiración mediante un temporizador, al recuperar el foco y ante cambios de visibilidad. El usuario actual remoto no se duplica en el store. `app/providers/HttpCredentials` conecta el token y el fin de sesión con `infrastructure/http`; `app/layouts/AuthGuard` consulta el estado para controlar rutas.

Contratos relacionados: [Artículo 23.º](composition.md#articulo-23) y [Artículo 25.º](composition.md#articulo-25).

<a id="articulo-28"></a>
## Artículo 28.º — Rutas y comportamiento actual del guard

La configuración actual define `/login` y `/register` como rutas públicas de autenticación, `/home` como ruta privada y `/players` y `/players/:id` como rutas privadas del catálogo y su detalle. `/` redirige a `/login`. El guard envía a `/login` a una identidad anónima que intenta acceder a una ruta privada y a `/home` a una identidad autenticada que intenta acceder a las rutas públicas de autenticación; mientras el estado sea `UNKNOWN` o `CHECKING`, muestra `SessionChecking`. La ruta comodín redirige a `/players`.

<a id="navegacion-actual"></a>
## Navegación actual

`AppLayout` envuelve **todas las rutas privadas** (`/home`, `/players`, `/players/:id`), por lo que la navegación es una capacidad transversal y no una consecuencia de una feature concreta.

Contrato de composición: [Artículo 29.º](composition.md#articulo-29).

<a id="ubicacion-de-layouts"></a>
## Ubicación de layouts

`AuthGuard` pertenece a `app/layouts/AuthGuard` y se exporta por `app/layouts/index.ts`; `app/router/AppRouter/AppRouter.tsx` lo consume desde layouts. Coexisten dos implementaciones llamadas `AuthLayout`: la global recuperada en `app/layouts/AuthLayout`, un contenedor de `Outlet` exportado por layouts pero sin uso en el router actual, y la específica en `features/auth/components/AuthLayout`, compuesta por login y registro y responsable de título, descripción, contenido y panel visual. `AppLayout`, `PageLayout` y `PaginatedPageLayout` permanecen en `app/layouts`. Esta descripción no conecta el layout recuperado a rutas ni cambia las responsabilidades del [Artículo 30.º](composition.md#articulo-30) o la regla de layouts específicos del [Artículo 27.º](composition.md#articulo-27).

<a id="estilos-de-la-implementacion"></a>
## Estilos de la implementación

Escribir colores literales en los módulos de una Page o Component es una divergencia pendiente de corrección, registrada en la sección de inconsistencias de `specs/005-players-page/spec.md`.

La referencia anterior se conserva como descripción previa. Su problema de trazabilidad ya está registrado en [TD-018](../../TECHNICAL_DEBT.md#td-018--colores-literales-en-los-módulos-de-estilo-de-componentes); esta migración no declara resuelta esa deuda. Regla aplicable: [Artículo 36.º](presentation.md#articulo-36).

<a id="recursos-de-la-implementacion"></a>
## Recursos de la implementación

**Implementación actual (descripción informativa):** Los escudos de equipo en `features/players/assets/teams/` son recursos locales generados por `frontend/scripts/import-team-logos.mjs`. Las banderas de nacionalidad en `getNationalityFlag` se obtienen de un servicio externo en runtime. Estas referencias documentan procedencias reales, no ejemplos genéricos.

Disposición relacionada: [Artículo 37.º](presentation.md#articulo-37). Inventario tecnológico: [recursos gráficos](../technologies.md#recursos-gráficos-y-dependencias-que-no-son-paquetes).

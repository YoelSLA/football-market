# Tecnologías del frontend

## Guía de lectura

Consultar este documento para conocer el stack, las tecnologías aprobadas, sus versiones o su propósito, y en toda tarea que agregue, elimine, sustituya o actualice una tecnología o dependencia del frontend. Para estructura, responsabilidades y dependencias arquitectónicas, consultar la [arquitectura del frontend](architecture.md).

Las versiones efectivas se obtienen de `frontend/package.json`, `frontend/package-lock.json`, `.nvmrc` y los archivos de configuración del frontend. Esas fuentes tienen precedencia sobre este documento. La tabla usa las versiones exactas resueltas por el lockfile cuando están disponibles.

| Tecnología | Versión | Uso en el proyecto |
| ---------- | ------- | ------------------ |
| Node.js | 24.19.0 (`.nvmrc`); requisito `>=24` | Runtime de las herramientas de desarrollo, verificación de tipos y build del frontend. |
| npm | No fijada; lockfile v3 | Gestión de dependencias y scripts del frontend mediante `package.json` y `package-lock.json`. |
| TypeScript | 7.0.2 | Tipado estricto del código React; comprueba el proyecto sin emitir JavaScript antes del build de Vite. |
| React y React DOM | 19.3.0 | Renderizado de la aplicación y composición de la UI en el navegador. |
| Lucide React | 1.59.0 | Iconos de navegación y de interfaz. `shared/icons` es el punto único de exportación: ningún módulo importa de la biblioteca directamente, y el inventario de iconos usados queda declarado en ese módulo. `NavIcon` centraliza el mapeo de secciones, el tamaño y la semántica decorativa; los iconos heredan `currentColor`. Sustituye los SVG manuales del Navbar. |
| Vite y plugin React | Vite 8.3.0; plugin React 6.1.1 | Servidor de desarrollo, preview y generación del build, con procesamiento de JSX/TSX y transformaciones de React mediante su plugin. |
| TanStack Query | 5.103.1 | Configuración global del estado remoto, caché y políticas de queries; base para queries, mutations e invalidaciones de features. |
| React Router DOM | 7.18.4 | Router del navegador, asociación de rutas globales y redirecciones de la aplicación. |
| Axios | 1.20.0 | Cliente HTTP en `infrastructure/http`, incorporación de credenciales Bearer en solicitudes protegidas y conservación de sus errores nativos para inspección genérica; utilizado por el servicio de autenticación. |
| Zustand | 5.0.15 | Store de sesión en `infrastructure/storage/useAuthStore`: estado de autenticación, inicio y cierre de sesión, conectado con la persistencia del token. |
| React Hook Form y resolvers | React Hook Form 7.88.0; resolvers 5.9.1 | Estado y comportamiento de los formularios de login y registro mediante Form Hooks, con integración de schemas Zod. |
| Zod | 4.6.5 | Schemas de validación de email, contraseña y confirmación de los formularios de autenticación. |
| Sass | 1.104.1 | Compilación de los estilos SCSS globales y de los SCSS Modules colocalizados con Pages y Components, incluida la capa global `styles/base` y `styles/abstracts` (breakpoints, tokens y temas). |
| Biome | 2.5.14 | Linting, formateo, organización de imports y controles recomendados sobre el frontend. |

## Recursos gráficos y dependencias que no son paquetes

Esta sección registra dependencias de recursos que no aparecen en `package.json` porque no son paquetes instalables. El principio de coherencia de contratos de la Constitución exige que todo cambio del inventario tecnológico se refleje aquí en la misma tarea.

| Recurso | Procedencia | Uso | Tolerancia a fallos |
| --- | --- | --- | --- |
| Escudos de equipos (`features/players/assets/teams/`) | **Local**. Importados en tiempo de build desde Wikimedia Commons mediante `frontend/scripts/import-team-logos.mjs`, que genera el registro de la feature y su documentación en `frontend/scripts/TEAM-LOGOS.md`. | Identificación visual del equipo en la tarjeta del catálogo y en el detalle. No se cargan en runtime. | Sin coincidencia por nombre normalizado, la tarjeta y el detalle muestran un marcador de posición accesible. |
| Banderas de nacionalidad (`getNationalityFlag`) | **Servicio externo en runtime** (`flagcdn.com`). | Identificación visual de la nacionalidad en la tarjeta del catálogo. | Marcador de posición accesible cuando la bandera no carga; la nacionalidad nunca depende de la imagen para ser comprensible. |
| Recursos de liga y posición y capturas de referencia | **Local**. `features/players/assets/` y `referencias-imagenes/`. | Identificación visual de liga y posición y referencia de diseño. | Se conserva siempre la identificación textual del dato. |
| Retrato genérico de jugador | **Local**. `shared/assets/Player Generic-Icon.png`, compartido por catálogo, detalle de jugador y Cartera. | Alternativa cuando el retrato no existe o no carga; sin copiar el recurso entre features. | Se conserva el nombre cuando existe y se comunica su ausencia cuando no es recuperable. |

`getTeamLogo` resuelve exclusivamente por nombre normalizado y devuelve `null` ante colisiones o ausencias; los alias conocidos se aplican solo en la importación y nunca en tiempo de ejecución.

El proyecto **no incorporate una librería de gráficos**: la evolución de cotizaciones se representa con SVG propio para evitar una dependencia adicional y conservar accesibilidad.

El frontend no declara actualmente una librería ni infraestructura de testing propia.

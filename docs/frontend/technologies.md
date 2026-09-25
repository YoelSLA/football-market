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
| Vite y plugin React | Vite 8.3.0; plugin React 6.1.1 | Servidor de desarrollo, preview y generación del build, con procesamiento de JSX/TSX y transformaciones de React mediante su plugin. |
| TanStack Query | 5.103.1 | Configuración global del estado remoto, caché y políticas de queries; base para queries, mutations e invalidaciones de features. |
| React Router DOM | 7.18.4 | Router del navegador, asociación de rutas globales y redirecciones de la aplicación. |
| Axios | 1.20.0 | Cliente HTTP en `infrastructure/http`, incorporación de credenciales Bearer en solicitudes protegidas y conservación de sus errores nativos para inspección genérica; utilizado por el servicio de autenticación. |
| Zustand | 5.0.15 | Store de sesión en `infrastructure/storage/useAuthStore`: estado de autenticación, inicio y cierre de sesión, conectado con la persistencia del token. |
| React Hook Form y resolvers | React Hook Form 7.88.0; resolvers 5.9.1 | Estado y comportamiento de los formularios de login y registro mediante Form Hooks, con integración de schemas Zod. |
| Zod | 4.6.5 | Schemas de validación de email, contraseña y confirmación de los formularios de autenticación. |
| Sass | 1.104.1 | Compilación de los estilos SCSS globales y soporte para los módulos SCSS previstos por la arquitectura. |
| Biome | 2.5.14 | Linting, formateo, organización de imports y controles recomendados sobre el frontend. |

El frontend no declara actualmente una librería ni infraestructura de testing propia.

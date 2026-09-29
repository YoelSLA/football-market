# Plan de implementación: Autenticación de usuarios en el frontend

**Branch**: `004-frontend-user-auth` | **Fecha**: 2026-09-23 | **Spec**: [spec.md](./spec.md)

## Resumen

Implementar registro, inicio de sesión, persistencia de JWT en `localStorage`, validación de sesión mediante `/api/auth/me`, protección de rutas, home temporal y cierre de sesión en una feature de autenticación aislada. La UI utilize React Hook Form, Zod, TanStack Query, React Router y el cliente HTTP compartido, respetando la separación `Page/Component → Hook → Service` y el adaptador de infraestructura.

## Contexto técnico

**Lenguaje/versión**: TypeScript 7.0.2; Node.js >=24; navegador moderno
**Dependencias principales**: React 19.3.0, React Router DOM 7.18.4, TanStack Query 5.103.1, React Hook Form 7.88.0, Zod 4.6.5, `@hookform/resolvers` 5.9.1, Axios 1.20.0, Sass 1.104.1
**Almacenamiento**: `localStorage` para el JWT mediante adaptador público de infraestructura
**Testing**: No se crean ni ejecutan tests de frontend; el build permitido es `npm run build`
**Plataforma objetivo**: Aplicación web SPA de escritorio y móvil
**Tipo de proyecto**: frontend web con backend HTTP
**Objetivos de rendimiento**: Validación de sesión antes de exponer contenido privado; no se fija una métrica adicional
**Restricciones**: El backend es autoridad de autenticación; no guardar secretos de firma; sin desplazamiento horizontal; evitar envíos simultáneos
**Alcance**: Tres rutas (`/login`, `/register`, `/home`), cinco stories P1/P2 y flujos de error especificados

## Constitution Check

- **Calidad y alcance**: PASS. Se utiliza una única feature con responsabilidades cohesivas y dependencias existentes.
- **Seguridad**: PASS. El token se maneja mediante adaptador de almacenamiento y `Authorization: Bearer`; el backend valida la sesión y el frontend no interpreta ni modifica firmas JWT.
- **Contratos y sincronización**: PASS. Se documentan los endpoints existentes y se respetan DTO/Model/Mapper.
- **Idioma**: PASS. El diseño y los nombres nuevos de código están en inglés; la documentación y mensajes de UI serán español.
- **Verificación**: PASS. No se crean tests frontend; el build sin tests queda como control permitido.
- **Gobernanza**: PASS. No requiere modificar la constitución ni introducir dependencias.

**Gate previo**: PASS.

## Estructura del proyecto

```text
frontend/src/
├── app/
│   ├── query/queryClient.ts
│   └── router/AppRouter.tsx
├── features/auth/
│   ├── components/
│   ├── form/
│   ├── hooks/
│   ├── mappers/
│   ├── models/
│   ├── pages/
│   ├── services/
│   ├── types/
│   └── index.ts
├── infrastructure/
│   └── storage/
├── shared/http/
├── styles/
├── App.tsx
└── main.tsx
```

**Decisión estructural**: la feature `auth` encapsula páginas, componentes, formularios, hooks, modelos, DTOs/mappers y servicios. `app/router` compone las páginas mediante la API pública de la feature. `shared/http` contiene solo comportamiento técnico transversal y `infrastructure/storage` encapsula el acceso a `localStorage`. No se crean carpetas vacías ni abstracciones no necesarias.

## Fase 0: Investigación

La investigación y sus decisiones están en [research.md](./research.md). Se resolvieron persistencia, cliente HTTP, validación, estado remoto, contratos y separación arquitectónica sin `NEEDS CLARIFICATION`.

## Fase 1: Diseño y contratos

- Modelo de entidades, validaciones y transiciones: [data-model.md](./data-model.md).
- Contrato HTTP: [contracts/auth.http.md](./contracts/auth.http.md).
- Guía de validación: [quickstart.md](./quickstart.md).

## Reevaluación de Constitution Check

- PASS: el diseño mantiene la feature aislada y las dependencias permitidas.
- PASS: el acceso externo queda en Services, las transformaciones en Mappers y la UI en Pages/Components mediante Hooks.
- PASS: el JWT no se expone a la UI directamente fuera del servicio de autenticación y se conserva en infraestructura.
- PASS: no se agregan dependencias ni se introducen tests de frontend.

**Gate posterior**: PASS.

## Fuera de alcance

Recuperación de contraseña, verificación de email, proveedores externos, perfil, funciones del mercado y comportamiento de `/` o rutas no declaradas.

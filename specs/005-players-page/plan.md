# Implementation Plan: Página de catálogo de jugadores

**Branch**: `005-players-page` | **Date**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-players-page/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Crear la página privada `/players` para consultar en tarjetas el catálogo paginado existente, con 12 jugadores por página, navegación, estados de carga/error/vacío, controles de filtrado exclusivamente visuales y recursos gráficos accesibles para liga, posición y estado activo. La implementación se encapsulará en una nueva feature frontend que consumirá `GET /api/players` mediante Axios y TanStack Query, transformará DTO a Model, reutilizará el guard y el ciclo de sesión actuales y no modificará backend, persistencia ni dependencias.

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0 y Node.js 24.19.0

**Primary Dependencies**: React Router DOM 7.18.4, TanStack Query 5.103.1, Axios 1.20.0 y Sass 1.104.1

**Storage**: N/A para la feature; reutiliza la sesión existente en memoria y `localStorage` encapsulada por `infrastructure/storage`

**Testing**: Sin infraestructura de tests frontend por norma del proyecto; validación manual descrita en `quickstart.md` y build permitido con `npm run build`

**Target Platform**: Navegadores de escritorio con viewport mínimo de 1280×720

**Project Type**: Aplicación web con frontend React y backend Spring Boot existente

**Performance Goals**: Mostrar feedback de catálogo vacío o error en menos de un segundo desde que se conoce el resultado y mantener transiciones visuales fluidas sin reflujo del catálogo

**Constraints**: Página privada; 12 elementos por consulta; cuadrícula fija de cuatro columnas y hasta tres filas sin desplazamiento horizontal desde 1280×720; filtros sin efecto funcional; estado mediante hover únicamente; no permitir que respuestas anteriores sustituyan la selección más reciente; sin dependencias nuevas

**Scale/Scope**: Una ruta y una feature frontend; cinco ligas, cuatro posiciones, un único estado visible y paginación sobre cualquier cantidad de jugadores activos

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Evaluación previa a la investigación

- **Simplicidad y alcance**: PASS. Se reutilizan router, guard, cliente HTTP, sesión, TanStack Query y componentes compartidos existentes; no se añaden capas ni dependencias ajenas a las responsabilidades definidas.
- **Seguridad**: PASS. `/players` pasará al `AuthGuard` privado y la consulta usará `authenticated: true`; el interceptor actual conserva el token fuera de la feature y finaliza la sesión ante `401`.
- **Contratos y sincronización**: PASS. La feature consume sin alterar el contrato vigente de `GET /api/players`; DTO, Model y Mapper permanecen separados y el contrato consumido queda documentado en `contracts/players-catalog.md`.
- **Arquitectura frontend**: PASS. El flujo previsto es `Page/Component → Hook → Service → infrastructure/http`, con transformación `DTO → Mapper → Model` dentro del Service, API pública de feature y estilos SCSS Modules colocalizados.
- **Idioma**: PASS. Código y endpoints conservarán inglés; los artefactos de planificación, textos de interfaz y mensajes serán españoles.
- **Verificación**: PASS. No se crearán ni ejecutarán tests frontend; la implementación posterior se verificará únicamente con el build permitido y los escenarios manuales de `quickstart.md`.

No existen `NEEDS CLARIFICATION` ni violaciones que requieran excepción antes de Phase 0.

### Reevaluación posterior al diseño

- **Resultado**: PASS. `research.md`, `data-model.md`, `contracts/players-catalog.md` y `quickstart.md` mantienen el alcance exclusivamente frontend, no introducen dependencias, cambios backend ni acceso directo desde UI a HTTP o DTO.
- **Seguridad y contrato**: PASS. El diseño conserva autenticación Bearer mediante infraestructura, tratamiento global del `401`, paginación desde cero y respuesta de error vigente sin inventar campos o endpoints.
- **Accesibilidad y estados**: PASS. El diseño exige alternativas textuales, foco visible, semántica de estado/error y fallbacks neutros; el indicador hover no oculta datos esenciales.
- **Verificación**: PASS. La guía separa el build permitido de la validación manual y no atribuye cobertura de tests al build.

## Project Structure

### Documentation (this feature)

```text
specs/005-players-page/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/
│   └── players-catalog.md # Contrato HTTP existente consumido por la feature
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
```text
frontend/
├── src/
│   ├── app/
│   │   └── router/
│   │       └── AppRouter.tsx
│   └── features/
│       └── players/
│           ├── assets/
│           ├── components/
│           │   ├── PlayerCard/
│           │   ├── PlayerCatalogControls/
│           │   └── PlayersPagination/
│           ├── constants/
│           ├── hooks/
│           │   ├── pages/
│           │   └── queries/
│           ├── pages/
│           │   └── PlayersPage/
│           ├── types/
│           ├── utils/
│           ├── players.mapper.ts
│           ├── players.service.ts
│           └── index.ts
└── package.json
```

**Structure Decision**: Se amplía la aplicación frontend existente con `features/players`, encapsulando su contrato, transformación, query, coordinación de página, componentes y recursos. `app/router/AppRouter.tsx` será el único punto global modificado para componer `PlayersPage` como ruta privada. Las carpetas internas se crearán solo cuando contengan la responsabilidad mostrada; el Page Hook se justifica por coordinar selección, corrección de página fuera de rango y estado de controles visuales.

## Complexity Tracking

No hay violaciones constitucionales que justificar.

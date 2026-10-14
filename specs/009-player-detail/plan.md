# Implementation Plan: Detalle individual del jugador

**Branch**: `feat/009-player-detail` | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/009-player-detail/spec.md`

## Summary

Implementar la pantalla privada de detalle de jugador en `/players/:id`: un endpoint backend nuevo `GET /api/players/{id}` que devuelve la identidad real aplicando el mismo criterio de visibilidad del catálogo (inactivos responden 404), una ruta y Page frontend que consumen ese endpoint, estadísticas y cotización del token de demostración generadas de forma determinista y centralizada en el frontend, un gráfico de evolución en SVG sin dependencias nuevas, y una navbar horizontal global reutilizable con breadcrumb y sector de usuario autenticado. La solución reutiliza arquitectura, convenciones, componentes y contratos vigentes; no añade endpoints estadísticos ni financieros, no implementa `GET /api/players/{id}/quotes` y deja documentada la integración futura.

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0 y Node.js 24.19.0 (frontend); Java y Spring Boot vigentes (backend)

**Primary Dependencies**: React Router DOM 7.18.4, TanStack Query 5.103.1, Axios 1.20.0, Sass 1.104.1, Biome 2.5.14; Spring Boot, Spring Web, Spring Data JPA y OpenAPI v3 (backend)

**Storage**: Sin almacenamiento nuevo. Se reutiliza la sesión encapsulada en `infrastructure/storage` y el catálogo persistido en PostgreSQL del backend

**Testing**: Frontend exento de tests por norma del proyecto; backend con tests de Controller y Spring REST Docs conforme a `docs/backend/testing.md` §8, ejecutados por el usuario

**Target Platform**: Navegadores de escritorio, tablet y móvil (360, 768 y 1280 px de ancho sin desbordamiento horizontal)

**Project Type**: Aplicación web con frontend React y backend Spring Boot existentes

**Performance Goals**: Feedback de carga, error o «no encontrado» visible en menos de un segundo desde que se conoce el resultado; navegación y apertura de la pantalla sin reflujo perceptible

**Constraints**: Un único endpoint nuevo; reutilizar credenciales y `404`/`400`/`401` existentes; datos de demostración deterministas, estables y etiquetados; treinta cotizaciones (una por día) con período fijo; gráfico en SVG sin dependencia de paquete nueva; navegación global compuesta desde `app` y aplicada a todas las rutas privadas; recursos gráficos decorativos locales o externos con marcador de posición accesible; sin implementaciones de scraping, estadísticas reales, cotizaciones reales ni compra/venta

**Scale/Scope**: Una ruta, una Page y un endpoint; catálogo completo paginado; un jugador por consulta

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Evaluación previa a la investigación

- **Simplicidad y alcance (§2)**: PASS. Se reutilizan router, guard, cliente HTTP, sesión, QueryClient, servicios, hooks y componentes existentes. No se crean capas ni dependencias nuevas; el gráfico se resuelve con SVG propio y la navbar como componente transversal real, no una abstracción preventiva.
- **Seguridad (§3)**: PASS. La consulta marcada como autenticada usa Bearer mediante `infrastructure/http`; el `401` conserva el tratamiento global y no se expone información de jugadores inactivos ni referencias de proveedores.
- **Contratos y sincronización (§4)**: PASS. `GET /api/players/{id}` se documenta en OpenAPI y REST Docs en la misma tarea; el DTO reutiliza el contrato de identidad del catálogo sin alterarlo; el contrato nuevo queda en `contracts/player-detail.md`.
- **Verificación (§6)**: PASS. No se crean ni ejecutan tests de frontend; los tests backend se crean y los ejecuta el usuario; la validación se apoya en el build permitido y los escenarios manuales de `quickstart.md`.
- **Idioma (§5)**: PASS. Código, endpoints y campos en inglés; documentación, Spec y textos de interfaz en español.
- **Gobernanza (§8, §9)**: PASS. Identidad asignada y registrada previamente conforme a la excepción de trabajo paralelo; sin reutilización de identificadores; la operación no habilita implementación.

No existen `NEEDS CLARIFICATION` ni violaciones sin justificar antes de Phase 0.

### Reevaluación posterior al diseño

- **Resultado**: PASS. `research.md` resuelve las decisiones técnicas diferidas; `data-model.md`, `contracts/player-detail.md` y `quickstart.md` son coherentes con `spec.md` y no introducen requisitos nuevos.
- **Arquitectura frontend**: PASS. Flujo `Page/Component → Hook → Service → infrastructure/http`, transformación `DTO → Mapper → Model` dentro del Service, APIs públicas por `index.ts`, estilos SCSS Modules colocalizados y navbar compuesta desde `app` para no crear dependencia entre features.
- **Arquitectura backend**: PASS. `Controller → Service → Repository → Model` con `PlayerMapper` y reutilización del `GlobalExceptionHandler`; sin lógica de negocio en Controller ni cambios de contrato existentes.
- **Dependencias**: PASS. Sin dependencias de paquete nuevas; los recursos gráficos añadidos el 2026-10-13 (escudos locales y banderas externas) quedan registrados en `docs/frontend/technologies.md` conforme a §4.
- **Alcance**: PASS. Se conservan todos los límites de `spec.md`: sin scraping, sin estadísticas reales, sin cotizaciones reales, sin `GET /api/players/{id}/quotes`, sin compra/venta y sin radar. La adopción de la navbar en el resto de rutas privadas y el rediseño del catálogo son decisiones de producto posteriores registradas en `spec.md` (FR-018, FR-024) y en SPEC-005.

## Project Structure

### Documentation (this feature)

```text
specs/009-player-detail/
├── plan.md                 # This file (/speckit.plan command output)
├── spec.md                 # Especificación con clarificaciones de 2026-10-12
├── research.md             # Phase 0 output (/speckit.plan command)
├── data-model.md           # Phase 1 output (/speckit.plan command)
├── quickstart.md           # Phase 1 output (/speckit.plan command)
├── contracts/
│   ├── player-detail.md    # Contrato HTTP nuevo y anexo de integración futura
│   └── demo-data.md        # Fuente de demostración y su sustitución futura
├── checklists/
│   └── requirements.md     # Evaluación documental de Specify/Clarify
└── tasks.md                # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── controllers/
│   ├── PlayerController.java              # Nuevo GET /{id} con OpenAPI
│   ├── mappers/PlayerMapper.java          # Reutilizado para la respuesta individual
│   └── exceptions/GlobalExceptionHandler.java
├── controllers/exceptions/                # Excepción de id no encontrado, si aplica
├── models/exceptions/                     # Excepción de dominio correspondiente
├── repositories/PlayerRepository.java     # Consulta individual con visibilidad de catálogo
├── services/PlayerCatalogService.java     # Contrato de consulta individual
└── services/impl/PlayerCatalogServiceImpl.java

backend/src/test/java/footballmarket/
├── controllers/                           # Test de Controller + REST Docs del endpoint
└── support/                               # Fixtures existentes reutilizadas

backend/src/docs/asciidoc/                 # Snippets derivados de REST Docs

frontend/src/
├── app/
│   ├── router/AppRouter.tsx               # Tabla de rutas: /, /login, /register, /home, /players, /players/:id
│   └── layouts/                           # Layouts globales
│       ├── AppLayout/                     # Navbar global + Outlet; envuelve todas las rutas privadas
│       ├── PageLayout/                    # Cabecera opcional, contenido y estado de carga
│       ├── PaginatedPageLayout/           # PageLayout + Pagination compartida
│       ├── AuthLayout/                    # Composición de las páginas públicas de autenticación
│       └── AuthGuard/                     # Guarda de rutas según el estado de sesión
├── features/players/
│   ├── pages/PlayerDetailPage/            # Page, estilos y barrel de la pantalla
│   ├── components/
│   │   ├── PlayerPortrait/                # Reutilizable con el fallback existente
│   │   ├── PlayerOverview/                # Ficha azul principal e indicadores
│   │   ├── PlayerSportsSummary/           # Tarjeta «Rendimiento deportivo»
│   │   ├── PlayerQuotePanel/              # Cotización actual y variación
│   │   ├── PlayerQuoteEvolutionChart/     # Gráfico SVG accesible
│   │   ├── PlayerBreadcrumb/              # Breadcrumb «Jugadores > nombre»
│   │   └── PlayerCard/                    # Enlace al detalle y estadísticas compartidas
│   ├── hooks/
│   │   ├── pages/usePlayerDetailPage.ts
│   │   └── queries/                       # Identidad real y lecturas de demostración
│   ├── constants/                         # Query keys, temporada y período de demostración
│   ├── demo/                              # Fuente centralizada de datos simulados
│   ├── utils/                             # demoHash y resolución de recursos gráficos
│   ├── assets/teams/                      # Escudos locales importados en build + registro generado
│   ├── types/dtos.ts, models.ts
│   ├── players.service.ts, players.mapper.ts
│   └── index.ts
├── shared/components/
│   ├── Navbar/                            # Navbar reutilizable (desktop y móvil) + navItems
│   └── Pagination/                        # Paginación compartida con ventana ±1 y elipsis
└── styles/
    ├── index.scss                         # Único punto de entrada global
    ├── base/                              # Reset y estilos elementales
    └── abstracts/                         # Breakpoints, tokens y temas

frontend/scripts/                          # Importación de escudos en build (no es código de runtime)
```

**Structure Decision**: La feature amplía `features/players`, que ya posee catálogo, modelos,
servicio y componentes, evitando una feature nueva y cualquier dependencia entre features. La
navbar vive como componente compartido presentativo (`shared/components/Navbar`) y se compone desde
`app` mediante `AppLayout`, porque requiere la identidad de sesión y debe ser global; el detalle la
consume a través de la composición de `app`, no importándola desde su propia feature. El backend
solo añade un método de lectura y reutiliza controller, mapper y manejo de errores existentes.

**Revisión 2026-10-13**: el diseño inicial preveía `AppNavbar` y `AppNavbarLayout` aplicados solo al
detalle. La decisión de producto conservó la implementación real, que usa `shared/components/Navbar`
sobre `AppLayout` y los aplica a todas las rutas privadas, y añadió el layout paginado compartido.
Este documento se ajusta a la estructura real; la autoridad funcional reside en `spec.md`.

**Dependencias de recursos añadidas el 2026-10-13**: escudos de equipo locales importados en build
y banderas de nacionalidad servidas por un proveedor externo en runtime. Ninguna introduce una
dependencia de paquete y ambas se documentan en `docs/frontend/technologies.md`.

## Complexity Tracking

No hay violaciones constitucionales que justificar.
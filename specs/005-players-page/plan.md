# Implementation Plan: Página de catálogo de jugadores

## Plan vigente — preparación del rediseño con datos reales

**Fecha**: 2026-10-25 · **Spec**: bloque vigente de [spec.md](spec.md). El plan anterior se conserva desde «Antecedente histórico». No modifica código, SPEC-009 ni Tasks históricas.

### Summary y Technical Context

Extender catálogo existente, no crear feature ni framework nuevos. Frontend React/TypeScript, TanStack Query/Axios/Sass e iconos/layouts del proyecto; backend Java 21/Spring Boot/Data JPA/PostgreSQL vigentes, versiones gobernadas por configuración. Sin tablas nuevas ni cambio de identidad. La búsqueda sin acentos requiere función SQL pura e índice GIN de expresión con pg_trgm, planificados para migración futura; no se instalan en esta operación. Diseñar una lectura paginada global y una lectura de opciones; enriquecimiento opcional con tres métricas del lote de página. DTO y Model separados, datos remotos solo en Query Cache. Tests backend escritos en implementación futura y ejecutados por usuario; sin tests frontend. Preparación documental sin builds.

### Constitution Check — antes y después de diseño

| Garantía | Antes de investigación | Después de diseño |
| --- | --- | --- |
| Simplicidad/proporcionalidad | Reutilizar feature/controlador/repositorios | Dos lecturas, enriquecimiento opcional; sin favoritos, scores o índices preventivos |
| Seguridad/integridad | Misma autenticación/elegibilidad | Parámetros tipados, sort whitelist, filtros parametrizados, lectura sin efectos |
| Contratos | Preservar consumidores existentes | Sin flags nuevos, identidad/envelope/orden ID legado idénticos; enriquecimiento solicitado explícitamente |
| Arquitectura | Matrices frontend/backend aplicables | Service sin DTO ni Service→Service; Mapper sin negocio; UI→Hook→Service→Mapper |
| Trazabilidad | Reutilizar 005 y no alterar 009 | Contrato canónico de lectura en 002, requisitos en 005; historia preservada |
| Autorizaciones/verificación | Solo preparación | Checklists requieren revisión humana antes de Tasks; Analyze read-only; sin implementación ni tests |

Sin excepciones nuevas ni violaciones identificadas en diseño. No acredita aceptación humana ni ejecución funcional.

### Investigación y decisiones

Fuentes inspeccionadas: `PlayerController` acepta solo page/size para listado; `PlayerCatalogServiceImpl` usa PageRequest con sort ID; `PlayerRepository.findByActiveTrue` exige activo/equipo vigente/liga; DTO actual tiene IDs/nombres internos, nacionalidad e imágenes opcionales; Page Hook del frontend guarda filtros sin enviarlos y PlayerCard consume muestra determinista. Detalle actual contiene lectura individual, pero reutilizar ese Service desde catálogo violaría la frontera Service→Service y produciría N+1: leer por lote desde Repository y componer Model. Detalles y alternativas en research vigente.

### Backend propuesto

- `controllers/PlayerController.java`: ampliar listado con Request DTO de query mediante binding/validación estructural; conservar método/firma antigua de Service como delegación con criterios vacíos si hay consumidores; nuevo GET literal /filters con respuestas dedicadas. OpenAPI, errores y autenticación existentes.
- `controllers/dtos/requests/PlayerCatalogQueryRequestDTO.java` record de transporte; Mapper convierte a `models/records/PlayerCatalogCriteria.java`, sin validaciones semánticas dispersas. sort restringido al conjunto del contrato; sin concatenar entrada en SQL/ORDER BY.
- `services/PlayerCatalogService.java` y `impl/PlayerCatalogServiceImpl.java`: ampliar lectura de catálogo y opciones. Model de resultado `PlayerCatalogPage`/entry con resumen opcional; no cambiar operaciones de sincronización. Transacción de lectura REPEATABLE_READ engloba página, conteo y métricas; DTO solo en Controller/Mapper.
- `repositories/PlayerRepository.java`: mantener findByActiveTrue y findVisibleById. Añadir JpaSpecificationExecutor y consulta visible compuesta por criterios desde método de Repository; count y selección comparten predicados, AND antes de ordenar/paginar. Nombre usa una única función SQL trim/NFD/eliminación de marcas U+0300–U+036F/lower sobre columna y parámetro, patrón literal escapado y binding según contrato. Los criterios son Model, no DTO. Orden nombre lowercase e ID sin alterar la semántica de sort por la normalización de búsqueda; ID legado si sort ausente. Opciones por consultas DISTINCT globales sobre visibles con asociaciones cargadas, sin filtrar solo la página.
- `repositories/PlayerStatisticsRepository.java`: consulta de lote por IDs de la página (máximo 100), confirmación por item asociado, selección de ancla y compañero por contexto conforme contrato. Usar SQL PostgreSQL con ventana/CTE o lateral en una consulta del lote para no materializar todas las campañas ni iterar consultas por jugador. Nunca cambiar queries de sincronización/valuación ni invocar sus Services. SQL e índices sujetos a evidencia de consulta, no migraciones preventivas.
- Composición en Model reutiliza catálogo canónico y los valores persistidos sin calcular tasas. Los snapshots pueden tener fechas/temporadas diferentes por jugador, que se informan por tarjeta. No exigir correspondencia de equipo actual con histórico para inventar contexto.
- Response DTO con enriquecimiento controlado: sin includeStatistics no emitir propiedad nueva, manteniendo estructura estricta de identidad. Con flag true, `PlayerCatalogEntryResponseDTO` contiene misma identidad más statistics; mapping explícito y record, nullable/cero/fuente preservados. No modificar PlayerResponseDTO individual ni sus consumidores del detalle.
- Migración futura en `backend/src/main/resources/db/migration/`: función de normalización pura/STRICT/IMMUTABLE e índice GIN de la misma expresión con pg_trgm; preservar columna name original y no añadir columna de identidad normalizada. Comprobar permisos de extensión y UTF-8 en el entorno objetivo, reflejar pg_trgm en tecnologías backend cuando se implemente. No usar funciones de reglas mutables declaradas inmutables por conveniencia. Referencia exacta: SPEC-002 contracts/catalog-query.md §Normalización. Sin ejecutar migración, SQL o índices en esta preparación.

### Frontend propuesto

- Extender `features/players/types/{dtos,models}.ts`, mapper y `players.service.ts` para criterios, página enriquecida y opciones. Tipos separados aun con estructura igual. getPage antiguo delega o recibe criterios opcionales compatibles; URL solo en Service, signal y authenticated:true.
- `constants/playersQueryKeys.ts`: clave por criterios efectivos/page/size; opciones separadas cacheadas, no petición de opciones en cada cambio de página.
- `hooks/queries/usePlayersQuery.ts` y nueva query de opciones; `hooks/pages/usePlayersPage.ts` coordina input debounced300ms, criterios/página en una transición y reset inmediato de página al cambiar criterios efectivos. Limpiar cancela debounce pendiente. La cancelación ignora resultados viejos y la key impide mezclar criterios. Una página fuera de rango se corrige una vez según totalPages; no bucle en cero.
- Frontend preserva la entrada original, con trim del criterio efectivo; no elimina acentos ni filtra nombres localmente. El backend normaliza ambos operandos con la misma función, evitando discrepancias entre implementaciones JS/SQL. Mantener key/300ms/reset de página sin crear nuevos filtros.
- `pages/PlayersPage/PlayersPage.tsx` compone componentes locales actuales `PlayerCatalogControls/`, `PlayerCard/`, `PlayersPagination/`; conservar AppLayout y PaginatedPageLayout/Pagination públicos. Estado y callbacks del Page Hook; hijos presentativos sin HTTP o hooks conectados. Verificar APIs/barrels antes de modificar ubicación física.
- Tarjetas dejan de consumir `getPlayerSampleStatistics` y DEMO_DATA_LABEL. No borrar demo/utilidades/tipos mientras existan consumidores ajenos; no modificar detalle. Foto y fallback existente, club/liga/nacionalidad reales, posición con etiqueta, tres valores nullable y contexto legible. Sin favorito decorativo ni contador del mockup constante.
- Referencia 6 columnas×2 filas a1920×913 con12 resultados; reducir columnas por breakpoints existentes y permitir scrollvertical, sin altura fija ni alterar navbar104px. Tokens/SCSS Modules e iconos por shared/icons. Controles etiquetados, foco, aria-busy/status/alert, conteo real y estados diferenciados.

### Verificación futura y riesgos

- Repository PostgreSQL/Testcontainers: AND antes de page/count, nombres repetidos, sort ambos sentidos/ID legado; «José»/«jose»/«JOSÉ» y Unicode compuesto/descompuesto equivalentes, «Álvarez»/«alvarez» equivalentes, `%`/`_`/`!` literales y entrada vacía tras normalización; opciones globales y lote estadístico con nulos/cero, fechas/contextos incompatibles y confirmación. Migración e índice reales en entorno de test aislado, sin pruebas de frontend.
- Service real @SpringBootTest, helper de Arrange únicamente, Act/Assert API pública; Controller slice con mocks de Service comprueba contratos completos, flags, filtros y errores, incluyendo ausencia de interacción en rechazos. No ejecutar pruebas por agente ni crear tests frontend.
- Quickstart cubre diseño, teclado, red lenta/respuestas tardías, limpiar durante debounce, ceroresultados, IDs distintos y compatibilidad. Builds solo después de cambios futuros de código según AGENTS.
- Riesgos: contrato histórico de002 todavía usa team/league legacy; no trasladarlo a los DTO actuales. Evitar N+1 deportivo y filtros en cliente; exactitud de count con joins. GIN pg_trgm solo ayuda cuando hay selectividad/trigramas suficientes; términos cortos siguen siendo válidos aunque requieran escaneo. Revisar colación/UTF-8 y privilegios de extensión; si cambia función/colación, reconstruir índice. No se imponen nuevos límites de longitud mínima ni índices preventivos. Nacionalidades nulas no se convierten en países. No afirmar que opciones/doc de preparación estén implementados.

### Estado de fase

setup-plan.sh --json ejecutado con FEATURE_DIRECTORY explícito a005: plan existente conservado. Research/modelo/contratos/quickstart actualizados documentalmente. El gate de checklists inicialmente pendiente queda satisfecho documentalmente tras la reevaluación y autorización condicional humana registrada en catalog.md y api.md. Este encargo no genera Tasks, ejecuta Analyze ni implementa; esas operaciones requieren un nuevo encargo. No se modifican Tasks históricas.

---

## Antecedente histórico del plan anterior

**Branch**: `005-players-page` | **Date**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-players-page/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Crear la página privada `/players` para consultar en tarjetas el catálogo paginado existente, con 12 jugadores por página, navegación, estados de carga/error/vacío, controles de filtrado exclusivamente visuales y recursos gráficos accesibles para liga, posición y estado activo. La implementación se encapsulará en una feature frontend que consumirá la ruta relativa `GET /players` mediante Axios y TanStack Query; `VITE_API_URL` ya incluye `/api`, por lo que la URL HTTP efectiva será `GET /api/players`. Transformará DTO a Model, reutilizará el guard y el ciclo de sesión actuales y no modificará backend, persistencia ni dependencias.

**Revisión 2026-10-13**: la decisión de producto conserva el rediseño manual del frontend y sustituye tres decisiones de este plan. (1) Las estadísticas de muestra dejan de ser valores fijos `24`/`8`/`5` y pasan a derivarse de forma determinista del identificador del jugador, compartidas con el detalle. (2) La página deja de ser exclusivamente de escritorio y se adapta a tablet y móvil. (3) El paginador propio de la feature se sustituye por el componente compartido `Pagination`, componido desde `app/layouts/PaginatedPageLayout`, con ventana ±1. Además, la navegación global pasa a componerse desde `app/layouts/AppLayout`. Ninguna de estas sustituciones modifica el backend, la persistencia ni el contrato HTTP del catálogo.

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0 y Node.js 24.19.0

**Primary Dependencies**: React Router DOM 7.18.4, TanStack Query 5.103.1, Axios 1.20.0 y Sass 1.104.1

**Storage**: N/A para la feature; reutiliza la sesión existente en memoria y `localStorage` encapsulada por `infrastructure/storage`

**Testing**: Sin infraestructura de tests frontend por norma del proyecto; validación manual descrita en `quickstart.md` y build permitido con `npm run build`

**Target Platform**: Navegadores de escritorio, tablet y móvil; 1280×720 es la referencia de composición en cuatro columnas, no un mínimo exclusivo

**Project Type**: Aplicación web con frontend React y backend Spring Boot existente

**Performance Goals**: Mostrar feedback de catálogo vacío o error en menos de un segundo desde que se conoce el resultado y mantener transiciones visuales fluidas sin reflujo del catálogo

**Constraints**: Página privada; 12 elementos por consulta; cuadrícula de cuatro columnas y hasta tres filas desde 1280×720 que se reorganiza progresivamente hasta una columna, sin desplazamiento horizontal; filtros sin efecto funcional; no permitir que respuestas anteriores sustituyan la selección más reciente; sin dependencias de paquete nuevas; recursos gráficos decorativos con marcador de posición accesible

**Scale/Scope**: Una ruta y una feature frontend; cinco ligas, cuatro posiciones, un único estado visible y paginación sobre cualquier cantidad de jugadores activos

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Evaluación previa a la investigación

- **Simplicidad y alcance**: PASS. Se reutilizan router, guard, cliente HTTP, sesión, TanStack Query y componentes compartidos existentes; no se añaden capas ni dependencias ajenas a las responsabilidades definidas.
- **Seguridad**: PASS. `/players` pasará al `AuthGuard` privado y la consulta usará `authenticated: true`; el interceptor actual conserva el token fuera de la feature y finaliza la sesión ante `401`.
- **Contratos y sincronización**: PASS. La feature consume sin alterar el contrato vigente de `GET /api/players` mediante la ruta relativa `/players` y el `VITE_API_URL` que ya incluye `/api`; DTO, Model y Mapper permanecen separados y el contrato consumido queda documentado en `contracts/players-catalog.md`.
- **Arquitectura frontend**: PASS. El flujo previsto es `Page/Component → Hook → Service → infrastructure/http`, con transformación `DTO → Mapper → Model` dentro del Service, API pública de feature y estilos SCSS Modules colocalizados.
- **Idioma**: PASS. Código y endpoints conservarán inglés; los artefactos de planificación, textos de interfaz y mensajes serán españoles.
- **Verificación**: PASS. No se crearán ni ejecutarán tests frontend; la implementación posterior se verificará únicamente con el build permitido y los escenarios manuales de `quickstart.md`.

No existen `NEEDS CLARIFICATION` ni violaciones que requieran excepción antes de Phase 0.

### Reevaluación posterior al diseño

- **Resultado**: PASS. `research.md`, `data-model.md`, `contracts/players-catalog.md` y `quickstart.md` mantienen el alcance exclusivamente frontend, no introducen dependencias, cambios backend ni acceso directo desde UI a HTTP o DTO.
- **Seguridad y contrato**: PASS. El diseño conserva autenticación Bearer mediante infraestructura, tratamiento global del `401`, paginación desde cero y respuesta de error vigente sin inventar campos o endpoints.
- **Accesibilidad y estados**: PASS. El diseño exige alternativas textuales, foco visible, semántica de estado/error y fallbacks neutros. Desde el 2026-10-13 la información esencial de la tarjeta no depende del hover: la posición se identifica por etiqueta textual (FR-009) y el hover solo eleva y refuerza el brillo (FR-010).
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
│   │   ├── router/
│   │   │   └── AppRouter.tsx
│   │   └── layouts/                    # AppLayout con navbar y PaginatedPageLayout (2026-10-13)
│   ├── shared/components/
│   │   ├── Navbar/
│   │   └── Pagination/
│   └── features/
│       └── players/
│           ├── assets/
│           │   └── teams/               # Escudos locales importados en build (2026-10-13)
│           ├── components/
│           │   ├── PlayerCard/
│           │   └── PlayerCatalogControls/
│           ├── constants/
│           ├── demo/                    # Generador determinista compartido con el detalle (2026-10-13)
│           ├── hooks/
│           │   ├── pages/
│           │   └── queries/
│           ├── pages/
│           │   └── PlayersPage/
│           ├── types/
│           ├── utils/                   # Resolución de escudo y bandera (2026-10-13)
│           ├── players.mapper.ts
│           ├── players.service.ts
│           └── index.ts
└── package.json
```

**Structure Decision**: Se amplía la aplicación frontend existente con `features/players`, encapsulando su contrato, transformación, query, coordinación de página, componentes y recursos. `app/router/AppRouter.tsx` será el único punto global modificado para componer `PlayersPage` como ruta privada. Las carpetas internas se crearán solo cuando contengan la responsabilidad mostrada; el Page Hook se justifica por coordinar selección, corrección de página fuera de rango y estado de controles visuales.

## Complexity Tracking

No hay violaciones constitucionales que justificar.

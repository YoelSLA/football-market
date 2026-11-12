# Implementation Plan: Detalle individual del jugador

## Plan vigente — rediseño visual 2026-11-12

**Rama**: `feat/009-player-statistics-layout` · **Spec**: [spec.md](spec.md) · **Referencia visual canónica**: [docs/wireframes/player-detail.md](../../docs/wireframes/player-detail.md).

### Summary

Adaptar la presentación del detalle del jugador según el wireframe canónico y los requerimientos aprobados. Se consolidan tres pestañas ordenadas («Cotizaciones» predeterminada, «Resumen deportivo» y «Todas las estadísticas»). La cabecera se compone en tres zonas horizontales (identidad a la izquierda con avatar 90×90 px, tres micro-tarjetas titulares centrales de 94 px y caja de cotización oficial de 94 px a la derecha con precio en créditos, badges semánticos de variación y cambio absoluto en créditos). El Resumen deportivo se estructura en 4 tarjetas KPI principales superiores (`SummaryKpiCard`) y exactamente 2 paneles temáticos específicos por posición (`SummaryPanel`) con filas de altura acotada y espaciado armónico, eliminando definitivamente las barras de progreso y el bloque de estadísticas destacadas. Se aplica una paleta semántica de colores sólidos basada en design tokens, garantizando legibilidad y ausencia total de scroll vertical y horizontal en escritorio (1920×1080, 1920×913, 1366×768 y 1280×800) y scroll documental natural en tablet y móvil. No modifica backend, rutas, persistencia ni dependencias globales.

### Technical Context

- **Stack efectivo**: React 19.3.0, TypeScript 7.0.2, Sass Modules, TanStack Query y Lucide mediante `shared/icons`; sin dependencias nuevas.
- **Área afectada**: `frontend/src/features/players/pages/PlayerDetailPage/` y sus subcomponentes, Page Hooks de controles locales (`usePlayerDetailPage`, `usePlayerDetailControls`), queries de cotización (`usePlayerQuotesQuery`, `usePlayerCurrentQuoteQuery`), utilidades de formateo (`playerQuotes.ts`, `playerStatisticsView.ts`, `formatDate.ts`) y design tokens en `_colors.scss`.
- **Datos**: se consumen `PlayerStatistics` (60 métricas) y `PlayerQuotes` (cotización oficial y evolución); semántica `null` ≠ `0` estrictamente conservada; sin migraciones ni contratos backend nuevos.
- **Estado**: TanStack Query continúa como autoridad remota. Pestaña activa (`'quotes' | 'summary' | 'catalog'`), búsqueda y categoría pertenecen al estado local por jugador coordinado por los Page Hooks.
- **Responsive**: Viewports escritorio 1920×1080, 1920×913, 1366×768 y 1280×800 con cero scroll de documento y cero scroll de contenedor en Resumen deportivo; tablet (768 px) y móvil (360 px) con desplazamiento vertical documental natural sin overflow horizontal.
- **Testing y validación**: Conforme a la Constitución, no se ejecutan ni crean tests frontend ni backend. Validaciones estáticas (`npm run biome:check`, `npm run scss:check-imports`), verificación de build (`npm run build`) y verificación automatizada de scroll con Playwright en Chrome DevTools.
- **Unknowns**: ninguno. Perfiles posicionales, prioridades, fallbacks deportivos, badges de cotización y tokens semánticos cerrados y verificados.

### Constitution Check

| Garantía | Antes del diseño | Después del diseño |
| --- | --- | --- |
| Simplicidad y proporcionalidad | Reutilizar la implementación existente | Conforme: componentes específicos sin sobrecomponentización ni abstracciones innecesarias |
| Coherencia contractual | Mantener 60 métricas y endpoints oficiales de cotización | Conforme: no cambia HTTP ni DTOs remotos; compatibilidad total de datos |
| Arquitectura frontend | Page compone; subcomponentes locales presentan; Hooks coordinan | Conforme: separación estricta de responsabilidades sin duplicar estado remoto |
| Sistema de diseño | Reutilizar design tokens semánticos y convenciones SCSS | Conforme: colores sólidos mediante `background-color`, sin gradientes ni sombras |
| Accesibilidad | Tabs accesibles, foco visible, semántica ARIA | Conforme: badges, estados y métricas comprensibles sin depender exclusivamente del color |
| Verificación | No ejecutar tests | Conforme: cero tests ejecutados; validación estática, build y verificación visual de scroll |

### Phase 0 — Research & Decisiones

1. **Cabecera compacta de 3 zonas**:
   - Izquierda (`PlayerOverviewIdentity`): Avatar enmarcado (90×90 px), nombre, dorsal/club, liga, posición, país y fecha de nacimiento.
   - Centro (`PlayerOverviewHeadlineMetrics`): 3 micro-tarjetas titulares específicas por posición (94 px de alto, `$surface-accent-deep`, borde `$border-panel`), iconos 18px, etiquetas 10.5px, valores 22px bold.
   - Derecha (`PlayerOverviewQuote`): Caja de cotización oficial de 94 px de alto (`$surface-accent-deep`, borde `$border-panel`) con precio en créditos (`22px bold`, icono moneda FM 22px `#f5c518`), badges semánticos (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`), fecha oficial y variación absoluta en créditos (`· +0,05 créditos vs. anterior`).
2. **Resumen deportivo de 4 KPI + 2 paneles temáticos**:
   - Eliminación definitiva de `PlayerStatisticsBars` (por falta de escala válida) y de `PlayerSportsSummary` (bloque de destacadas que duplicaba información).
   - 4 tarjetas KPI principales (`SummaryKpiCard`): Iconos ampliados a 24px (en contenedor de 48×48 px), etiquetas 12px, valores 28px bold, subtítulos 12px con `line-height: 1.35`. Sin unidades redundantes que repitan el título/subtítulo. Se eliminan recortes por `text-overflow: ellipsis` evitando que se corten los descenders (`g`, `p`, `y`).
   - 2 paneles temáticos específicos (`SummaryPanel`): Lista de 4 métricas con iconos de 20px (encabezado) y 18px (filas), títulos 16px, etiquetas 14px, valores tabulares 16px. Filas acotadas entre `min-height: 44px` y `max-height: 58px` con `justify-content: space-evenly`, evitando el estiramiento vertical desmedido en pantallas altas.
3. **Pestañas del detalle**:
   - Pestaña 1: «Cotizaciones» (`PlayerQuotesTab`, predeterminada), con gráfico histórico de evolución.
   - Pestaña 2: «Resumen deportivo» (`PlayerSummaryTab` -> `PlayerStatisticsSummary`).
   - Pestaña 3: «Todas las estadísticas» (`PlayerCatalogTab` -> `PlayerStatisticsCatalog`).
4. **Paleta de tokens de colores sólidos**:
   - `$surface-page-deep`: `#011522` (fondo de la página de detalle)
   - `$surface-card-dark`: `#092337` (tarjetas KPI)
   - `$surface-panel-to`: `#071a30` / `$surface-panel-from`: `#0c233e` (paneles temáticos y catálogo)
   - `$surface-accent-deep`: `#011f14` (micro-tarjetas de cabecera y caja de cotización)
   - `$surface-accent-dark`: `#003d2a` (marco de avatar en cabecera)
   - `$accent-sports-green`: `#00e676` (acento deportivo en iconos, bordes activos y variaciones positivas)
   - `$border-panel`: `#2b5378` (bordes estructurales)
   - Cero gradientes, cero sombras decorativas, cero efectos de iluminación.

### Phase 1 — Diseño de Composición y Componentes

#### Árbol de componentes definitivo

```text
PlayerDetailPage
├── PageLayout
├── PlayerDetailNav (breadcrumb hacia catálogo con retorno seguro)
├── PlayerDetailStatus (estados de carga, error y no encontrado de identidad)
├── PlayerOverview (cabecera compacta de 3 zonas y 125 px de altura neta)
│   ├── PlayerOverviewIdentity (avatar 90×90, metadatos del jugador)
│   ├── PlayerOverviewHeadlineMetrics (3 micro-tarjetas titulares de 94 px)
│   └── PlayerOverviewQuote (caja de cotización de 94 px con badges y variación absoluta)
└── PlayerStatisticsSection (gestión de pestañas y estados estadísticos)
    ├── PlayerStatisticsTabs (3 pestañas ordenadas: Cotizaciones, Resumen deportivo, Todas las estadísticas)
    ├── PlayerQuotesTab (gráfico SVG interactivo de evolución de cotización)
    ├── PlayerSummaryTab -> PlayerStatisticsSummary (resumen deportivo compacto)
    │   ├── 4x SummaryKpiCard (tarjetas KPI con iconos de 24px y valores de 28px)
    │   └── 2x SummaryPanel (paneles temáticos específicos con filas de altura acotada)
    ├── PlayerCatalogTab -> PlayerStatisticsCatalog (8 categorías, búsqueda y 60 métricas)
    └── PlayerStatisticsNotice (advertencia accesible de datos parciales o consulta previa)
```

#### Retiro de componentes obsoletos
- `PlayerBreadcrumb`: retirado y reemplazado por `PlayerDetailNav`.
- `PlayerStatisticsBars`: eliminado definitivamente (barras sin escala eliminadas del Resumen).
- `PlayerSportsSummary`: eliminado definitivamente (bloque de destacadas retirado para evitar redundancia).

#### Layout y gestión de scroll
- **Desktop (≥1280 px)**: Layout vertical flexbox acotado (`display: flex; flex-direction: column; height: 100%`). Cabecera fija (125 px) + navegación (32 px) + tabs (40 px) = 209 px superior. La pestaña «Resumen deportivo» ajusta sus KPI y paneles temáticos sin desbordar el contenedor en 1920×1080, 1920×913, 1366×768 y 1280×800 (`hasVerticalScroll: false`, `hasHorizontalScroll: false`). La pestaña «Todas las estadísticas» dispone de scroll vertical interno confinado exclusivamente a su lista de métricas.
- **Tablet y móvil (<1280 px)**: Columnas colapsan ordenadamente (KPIs en 2 cols a 768 px y 1 col a 360 px; paneles en 1 col); desplazamiento vertical natural de página sin desbordamiento horizontal.

### Phase 2 — Implementación y Validación

- **Calidad de código y estilo**: `npm run biome:check` (0 errores, 0 advertencias), `npm run scss:check-imports` (satisfactorio).
- **Compilación**: `npm run build` exitoso sin errores de tipos TypeScript ni empaquetado.
- **Verificación visual y de scroll**: Comprobada mediante Playwright en Chrome DevTools en las 4 resoluciones de escritorio normativas con sesión autenticada. Cero scroll vertical y cero scroll horizontal en Resumen deportivo. Scroll natural documental verificado en anchos móviles.

---

## Antecedente de planificación — dashboard real 2026-10-25

**Plan anterior** — rama `feat/009-player-detail-whoscored-stats-dashboard`. Se conserva como evidencia del diseño funcional ya implementado. El plan visual vigente anterior gobierna cualquier contradicción de composición.

Las secciones vigentes terminan antes de «Antecedente histórico». Se preservan los planes y decisiones anteriores como evidencia, no como requisitos del nuevo dashboard. `tasks.md` no se modifica ni se considera vigente para este alcance por existir.

## Summary

Extender el detalle existente con dos pestañas y estadísticas reales mediante una lectura backend independiente de identidad. Seleccionar un snapshot individual confirmado y componer JSON/scraping sin mezclar contextos. Mantener las 60 claves, metadatos de presentación y cuatro perfiles aprobados; sustituir las llamadas de demostración del detalle sin alterar los consumidores del catálogo. Sin datos financieros, paquetes nuevos, radar, percentiles ni indicadores deportivos derivados.

## Technical Context

- **Lenguajes y dependencias**: TypeScript/React, TanStack Query, Axios, Sass y router existentes; Java/Spring Web/Data JPA y PostgreSQL existentes. Versiones efectivas: configuraciones de cada área; no se modifican dependencias.
- **Persistencia**: reutilizar `PlayerStatistics`, `MetricCatalog`, item de extracción y repositorios. Sin tablas nuevas ni migraciones iniciales. Índice existente `(player_id, extraction_date DESC)`; no anticipar índices sin evidencia.
- **Plataforma**: navegador autenticado; anchos 360/768/1280 y alturas 640/800 de SC-017.
- **Escala**: un jugador/contexto por lectura y 60 valores; selección sin cargar toda la población ni todas las campañas en memoria. Evidencia histórica: 950 filas, no una garantía de volumen.
- **Rendimiento**: consulta indexable acotada, sin N+1 ni proveedores externos; no agregar umbrales de latencia no aprobados.
- **Testing**: escribir tests backend en implementación autorizada, ejecutados por el usuario. Sin tests ni infraestructura frontend. En este PLAN documental no se ejecutan builds ni tests.

## Constitution Check

| Garantía | Antes de research | Después del diseño |
| --- | --- | --- |
| Simplicidad y proporcionalidad | Conforme: ampliar flujos existentes | Conforme: una lectura, sin tablas, capas o dependencias preventivas |
| Seguridad e integridad | Autenticación y elegibilidad existentes | Misma visibilidad para identidad/estadísticas; sin llamadas externas ni escritura al consultar |
| Coherencia contractual | Identidad/catálogo compatibles | Endpoint estadístico aditivo; contrato, mappers, OpenAPI y tests backend previstos en el mismo cambio futuro |
| Fuente de verdad y trazabilidad | Reutilizar 009 y preservar historia | Perfiles/60 claves de spec; snapshots de SPEC-012; no selección poblacional de SPEC-013 |
| Verificación y autorizaciones | Solo PLAN autorizado | Sin código, tests ejecutados, commit, push ni cambio de seguimiento; no autoriza implementar |
| Idioma y límites arquitectónicos | Documentación española | Identificadores ingleses; Controller→Service→Repository/Model y UI→Hook→Service→Mapper |

No se detectan violaciones sin justificar ni decisiones funcionales abiertas. Las discrepancias históricas de layouts no se resuelven cambiando normas globales: conservar comportamiento y limitar el cambio a esta Page.

## Phase 0 — Research

Decisiones y alternativas en [research.md](research.md), sección vigente. Investigación estática sobre implementación y consumidores de ambas áreas. No se emulan operaciones de Spec Kit. `setup-plan.sh --json`, invocado mediante Bash, conservó el plan existente y resolvió esta carpeta; su `BRANCH=009-player-detail` es la selección lógica, no la rama Git real.

## Phase 1 — Diseño

### Backend

1. `PlayerController` añade `GET /api/players/{id}/statistics`, sin alterar DTO, ruta o visibilidad de `GET /{id}` ni listado. Alinear validación de ID positivo de ambas lecturas según FR-046: 400 para formato/rango inválido, cero o negativo; 404 para ID válido inexistente/no visible. Es corrección futura acotada de identidad, hoy sin validación positiva explícita, no cambio ya implementado. Temporada opcional, OpenAPI y errores globales; no permitir que la nueva subruta quede bajo operaciones de sincronización.
2. Nuevo contrato `PlayerStatisticsQueryService` y `impl/PlayerStatisticsQueryServiceImpl`: orquestación transaccional read-only consistente, consulta de visibilidad y selección individual. No invocar otros Services ni `ValuationSnapshotSelector`.
3. `PlayerStatisticsRepository`: candidatos resueltos con item referenciado `COMPLETED` y coincidencia de jugador/fecha/canal; elegir última fecha confirmada con filtro `seasonKey` si se suministra. Obtener solo ancla y filas compañeras de esa fecha, no historia completa. Sin parámetro de temporada, usar el último contexto confirmado del propio jugador y devolver la temporada real; no selector visual nuevo.
4. Anclar contexto en esa fecha: JSON antes que scraping; ante varios candidatos del canal, `observedAt DESC, id DESC`. El ancla fija temporada y competición. La unicidad vigente por jugador/fecha/canal evita múltiples contextos del mismo canal; el orden defensivo no autoriza su fusión. Buscar respaldo exclusivamente de igual jugador, temporada, fecha y competición. Datos de otra competición, incluso más recientes dentro de esa fecha, no completan el ancla.
5. Model puro de lectura `PlayerStatisticsSnapshot`/valor métrico aplica composición por `MetricCatalog`: JSON no nulo gana, nulo admite scraping coincidente, cero nunca activa respaldo; scraping solo también es válido. Iterar siempre 60 claves. No depender del resultado global del run ni de snapshots de otros jugadores.
6. Mapper estático de Controller transforma Model a DTO record; nunca decide fuentes. DTO con contexto, fecha y 60 valores nullable/procedencia. El Service enriquece el nombre de competición solo mediante correspondencia local fiable de la clave persistida; sin nombre verificable entrega null, nunca la liga actual por asociación del jugador. No requiere nuevo almacenamiento ni proveedor externo. Contrato completo en [contracts/player-statistics.md](contracts/player-statistics.md).

### Frontend

- `playersService.getStatistics` usa `http` autenticado y signal; mapper convierte DTO tipado en Model. No introducir validación runtime ad hoc ni tipos HTTP en componentes/hooks.
- Nuevo `usePlayerStatisticsQuery` con key `playersQueryKeys.statistics(id, requestedSeasonKey ?? null)`, `retry: false` consistente con identidad. Activar solo tras identidad vigente; cancelación y comprobación de ID/contexto, sin placeholder de otro jugador. Backend entrega temporada resuelta; no duplicar caché bajo un segundo estado global.
- `usePlayerDetailPage`: derivar ID directamente de params, sin conservar un render del anterior mediante efecto. Coordinar identidad y estadísticas con estados independientes, reintento separado y pestaña/filtro/categoría locales. Resetear controles al cambiar jugador; cambiar pestaña no inicia otra consulta del mismo snapshot.
- `PlayerDetailPage`: conservar breadcrumb, PageLayout y navbar global; quitar consumo de season/quote demo. `PlayerOverview` queda para identidad y recursos existentes, sin precio ni indicadores simulados.
- Componentes locales bajo `pages/PlayerDetailPage/`: `PlayerStatisticsTabs`, `PlayerStatisticsSummary`, `PlayerStatisticsCatalog` y gráfico de barras privado del resumen con TSX/SCSS/barrel. Hijos reciben modelos/callbacks, no hooks. Separación por responsabilidad, no por número de líneas.
- `constants/playerStatisticsCatalog.ts`: nombres españoles, unidades visibles y categorías exactas de [contracts/metric-glossary.md](contracts/metric-glossary.md), orden y prioridades de la matriz; `playerStatisticsProfiles.ts`: tarjetas, series y adicionales por posición. Tipos en `types/models.ts`, DTO en `types/dtos.ts`; transformaciones en mapper, agrupación/filtrado/orden puro sobre Model en `utils/`. Barrels exponen solo lo necesario.
- Catálogo: ocho categorías; buscar sin mayúsculas/acentos por clave o nombre, intersección con categoría. Disponibles P→S→B; ausentes al final, orden estable de spec. Sin paginación inicial necesaria para 60 filas: lista agrupada accesible con scroll de PageLayout; no agregar páginas, selector o totales derivados del mockup como requisito.
- Resumen: exactamente cuatro tarjetas de cada perfil y gráficos/adicionales definidos en spec. Barras en unidades originales con valores textuales; falta un valor → aviso y sin barra; cero → 0 visible; si todos son cero no se divide por cero. No generar proporciones de zonas, medias ni totales desde tasas.
- Responsive: implementar el orden y composición del [wireframe canónico](../../docs/wireframes/player-detail.md), conservar tokens y flex/min-width:0/min-height:0, sin alturas fijas. PageLayout posee scroll desktop; móvil conserva scroll natural vigente. Columnas se apilan; filas largas se adaptan sin overflow de documento. No tocar layouts compartidos para corregir esta Page.
- Accesibilidad: tablist/tabpanel, aria-selected/controls, roving tabindex y flechas/Home/End; buscador etiquetado, categorías con selección perceptible, feedback role=status y errores role=alert, foco y reintento. Tabla/listado mantiene nombre/unidad/valor en móvil, gráfico con alternativa textual; no depender del color.

### Archivos afectados durante implementación futura

Rutas backend relativas a `backend/src/main/java/footballmarket/`:

| Acción | Archivos/responsabilidad |
| --- | --- |
| Modificar | `controllers/PlayerController.java`, `repositories/PlayerStatisticsRepository.java` |
| Crear | `services/PlayerStatisticsQueryService.java`, `services/impl/PlayerStatisticsQueryServiceImpl.java` |
| Crear | `models/PlayerStatisticsSnapshot.java`, `controllers/dtos/responses/PlayerStatisticsResponseDTO.java`, `controllers/mappers/PlayerStatisticsMapper.java` (valores relacionados colocalizados cuando corresponda) |
| Tests backend futuros | `controllers/PlayerControllerTest.java`; tests de consulta Service, composición Model y selección Repository bajo paquetes existentes |

Frontend: modificar `features/players/players.service.ts`, `players.mapper.ts`, `types/{dtos,models,index}.ts`, `constants/playersQueryKeys.ts`, `hooks/pages/usePlayerDetailPage.ts`, `pages/PlayerDetailPage/{PlayerDetailPage.tsx,PlayerDetailPage.module.scss}`, `PlayerOverview/` y barrels necesarios. Añadir query hook, metadatos/perfiles, operaciones puras y componentes locales descritos arriba. Inspeccionar consumidores antes de retirar `PlayerSportsSummary/`, `PlayerQuotePanel/`, `PlayerQuoteEvolutionChart/` o hooks de demo; el catálogo comparte `playerSampleSeason`, no borrar esa fuente ni modificar su comportamiento sin alcance adicional.

No modificar router, sesión, navbar, sincronización, valuaciones o finanzas. Los cambios de testing/documentación OpenAPI necesarios acompañarán el código en implementación autorizada; no se generan ahora.

## Riesgos y verificación

- Filas migradas sin item no acreditan confirmación; `NO_LEAGUE_CONTEXT`/`NO_DATA` no son snapshot válido. No ocultar fallo técnico como ausencia.
- `observedAt` de fila refleja confirmación en el writer: no presentarlo como fecha de partido ni como observación individual de cada tabla. La fecha visible es `extractionDate`.
- Transferencias: contexto estadístico se obtiene de la fila, no se reescribe con equipo/liga actuales. Identidad vigente y contexto histórico se etiquetan separadamente.
- La consulta debe mantener consistencia entre ancla y respaldo (transacción read-only con aislamiento repeatable-read para consultas separadas); ninguna dependencia del estado poblacional.
- Contrato antiguo menciona REST Docs y demos: antecedente sustituido por contrato nuevo y OpenAPI/tests de Controller, sin reintroducir herramientas retiradas.
- IDs cero/negativos: divergencia heredada identificada y resolución documental aprobada en FR-046. Ambas lecturas deben cumplir 400 para ID inválido; conservar 404 de válido inexistente/no visible y 200 NO_DATA de ausencia estadística. Implementación futura incluye alineación acotada y casos de contrato de identidad, sin afirmar que ya esté corregida.
- Checklist y quickstart verifican casos de snapshot, precedencia, claves, perfiles, estados, accesibilidad y viewports. Builds solo en una futura modificación de código y bajo AGENTS de cada área; tests backend siempre a cargo del usuario.

## Evidencia y siguiente paso

Revisión de criterios parciales: I-01 resuelto con `competitionName` opcional/fallback fiable; I-02 con FR-046 y alineación futura explícita de identidad; D-01 con glosario de 60 claves; D-02 con FR-047 y conservación acotada de respuesta previa. El Page Hook obtiene la última respuesta compatible de Query Cache, no duplica estadísticas en un store ni usa placeholder de otro contexto. Error de refetch es estado de UI adicional a disponibilidad de la última respuesta, con fecha original y advertencia; una nueva respuesta NO_DATA reemplaza la anterior.

Artefactos vigentes: este plan, research, data-model, contracts/player-statistics y quickstart; encabezados de contratos anteriores delimitan su vigencia sin borrar evidencia. Constitution Check antes/después conforme documentalmente. No se modifica `tasks.md`, aprobaciones históricas ni seguimiento. No se ejecutaron tests, builds o hooks; no existe `.specify/extensions.yml`. Siguiente paso: generar y evaluar checklists de calidad y especializados aplicables bajo autorización, nunca TASKS automáticamente.

## Complexity Tracking

Sin excepciones arquitectónicas nuevas. Diseño acotado a lectura, composición de fuentes y presentación aprobada.

---

## Antecedente histórico del plan anterior

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

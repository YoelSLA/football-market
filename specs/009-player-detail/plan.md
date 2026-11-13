# Implementation Plan: Detalle individual, estadísticas reales y cotizaciones del jugador

**Feature**: `009-player-detail`
**Spec**: [spec.md](spec.md) · **Referencia visual canónica**: [docs/wireframes/player-detail.md](../../docs/wireframes/player-detail.md)
**Status**: Implementado y verificado en `dev`

## Summary

La página de detalle del jugador (`/players/:id`) organiza de forma integral la identidad, el rendimiento deportivo y el estado financiero del jugador en Football Market.

Se compone de una cabecera compacta de tres zonas (identidad a la izquierda con avatar 90×90 px, tres micro-tarjetas titulares de 94 px en el centro y caja de cotización oficial de 94 px a la derecha con badges semánticos) y tres pestañas ordenadas: «Cotizaciones» (activa por defecto, con gráfico de evolución histórica), «Resumen deportivo» (4 tarjetas KPI superiores y 2 paneles temáticos específicos por posición con filas de altura acotada, sin barras de progreso ni bloque de destacadas) y «Todas las estadísticas» (catálogo de 60 métricas canónicas con búsqueda y filtros por categoría).

El diseño aplica una paleta semántica de colores sólidos basada en design tokens, garantizando legibilidad y ausencia total de scroll vertical y horizontal de ventana en escritorio (1920×1080, 1920×913, 1366×768 y 1280×800) y scroll documental natural en tablet y móvil.

## Technical Stack & Architecture

- **Frontend**:
  - React 19, TypeScript, Sass Modules con tokens semánticos sólidos (`_colors.scss`).
  - TanStack Query como autoridad remota para caché, deduplicación y ciclo de vida de peticiones.
  - Lucide icons mediante `shared/icons`.
  - Roles arquitectónicos: `Page/Component → Page Hook → Service → infrastructure/http`.
- **Backend**:
  - Spring Boot 3, Java 21, Spring Web, Spring Data JPA, PostgreSQL.
  - Roles arquitectónicos: `Controller → Service → Repository → Model`.
- **Endpoints consumidos**:
  - Identidad básica: `GET /api/players/{id}`
  - Estadísticas deportivas: `GET /api/players/{id}/statistics`
  - Cotizaciones del token: `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current`

## Árbol de Componentes Frontend

```text
PlayerDetailPage
├── PageLayout
├── PlayerDetailNav (breadcrumb hacia catálogo con retorno seguro a /players)
├── PlayerDetailStatus (estados de carga, error y no encontrado de identidad)
├── PlayerOverview (cabecera compacta de 3 zonas y 125 px de altura neta en desktop)
│   ├── PlayerOverviewIdentity (avatar 90×90 enmarcado, nombre, club, liga, posición, país y fecha)
│   ├── PlayerOverviewHeadlineMetrics (3 micro-tarjetas titulares de 94 px según posición)
│   └── PlayerOverviewQuote (caja de cotización de 94 px con badges y variación absoluta)
└── PlayerStatisticsSection (gestión de pestañas y estados estadísticos)
    ├── PlayerStatisticsTabs (3 pestañas ordenadas: Cotizaciones, Resumen deportivo, Todas las estadísticas)
    ├── PlayerQuotesTab (panel de cotización y gráfico interactivo de evolución)
    ├── PlayerSummaryTab -> PlayerStatisticsSummary (resumen deportivo compacto)
    │   ├── 4x SummaryKpiCard (tarjetas KPI con iconos de 24 px y valores de 28 px bold)
    │   └── 2x SummaryPanel (paneles temáticos específicos con filas de altura acotada)
    ├── PlayerCatalogTab -> PlayerStatisticsCatalog (8 categorías, búsqueda y 60 métricas)
    └── PlayerStatisticsNotice (advertencia accesible de datos parciales o consulta previa)
```

## Arquitectura de Datos y Estado

### Frontend
- **Consultas remotas coordinadas**:
  - `usePlayerDetailQuery`: consulta de identidad del jugador. Si responde 404 (`PLAYER_NOT_FOUND`), la vista entra en estado no encontrado sin exponer datos.
  - `usePlayerStatisticsQuery`: consulta del snapshot estadístico confirmando temporada y 60 métricas. Se activa únicamente cuando la identidad fue resuelta con éxito.
  - `usePlayerQuotesQuery`: consulta del historial y cotización vigente.
- **Estado local por jugador (`usePlayerDetailControls`)**:
  - `activeTab`: `'quotes' | 'summary' | 'catalog'` (`'quotes'` predeterminada al ingresar).
  - `search`: filtro de texto para el catálogo.
  - `category`: categoría activa (`'all'` o una de las 8 categorías canónicas).
  - Al cambiar de jugador, los filtros y la pestaña activa se reinicializan automáticamente.
- **Resiliencia ante errores**:
  - Si la actualización de estadísticas falla pero existe una respuesta compatible previa en caché, se mantiene visible la información anterior con un banner informativo («Mostrando la última consulta disponible») y botón de reintento.

### Backend
- **Resolución del snapshot estadístico (`PlayerStatisticsQueryService`)**:
  - Consulta acotada y transaccional (`readOnly = true`) por jugador y temporada opcional.
  - Selección de la última extracción confirmada (`extractionDate DESC`).
  - Precedencia de fuentes: JSON tiene prioridad; scraping únicamente completa valores nulos para las tres claves exclusivas (`starts_total`, `shots_on_target_total`, `second_yellow_total`) dentro de la misma fecha y contexto. Un cero en JSON es dato válido y no se sustituye.
  - Manejo de estados: 60 valores presentes → `AVAILABLE`; 1 a 59 presentes → `PARTIAL`; 0 presentes → `NO_DATA`.
  - Validación de identificador: ID no entero, cero o negativo responde 400 (`INVALID_PARAMETER_TYPE`). ID válido no encontrado responde 404 (`PLAYER_NOT_FOUND`). Jugador visible sin estadísticas responde 200 con estado `NO_DATA`.

## Layout, Viewport y Responsividad

- **Desktop (≥1280 px)**:
  - Altura superior fija de 209 px (breadcrumb 32 px + cabecera 125 px + margen 12 px + pestañas 40 px).
  - Resumen deportivo y Cotizaciones encajan de forma armónica aprovechando la altura disponible con **cero scroll de documento y cero scroll de contenedor** en resoluciones estándar (1920×1080, 1920×913, 1366×768 y 1280×800).
  - En «Todas las estadísticas», el desplazamiento vertical pertenece con exclusividad al contenedor interno de la lista de métricas.
- **Tablet y móvil (<1280 px)**:
  - Cabecera: identidad y cotización se apilan; en móvil las métricas titulares se disponen en una fila compacta simétrica.
  - Resumen deportivo: las tarjetas KPI colapsan a 2 columnas en tablet y 1 columna en móvil; los paneles temáticos se apilan verticalmente.
  - Catálogo: controles superiores envolventes con scroll horizontal interno para categorías si es necesario.
  - Desplazamiento documental natural vertical, con **ausencia total de scroll horizontal** en la página.

## Constitution Check

| Principio | Estado | Justificación |
| --- | :---: | --- |
| Simplicidad y calidad | Conforme | Solución directa sin dependencias adicionales de gráficos pesados; uso de SVG nativo y CSS estructurado. |
| Coherencia de contratos | Conforme | Endpoints `GET /api/players/{id}`, `/statistics` y `/quotes` alineados con OpenAPI, DTOs y glosario canónico. |
| Seguridad e integridad | Conforme | Rutas privadas protegidas por Bearer JWT. Identificadores inválidos devuelven 400; inactivos e inexistentes 404 indistinguible sin revelar información. |
| Mínimo alcance necesario | Conforme | Se reutilizan entidades existentes; no se agregaron tablas, migraciones, campos financieros hipotéticos ni indicadores derivados no autorizados. |
| Idioma | Conforme | Código y contratos en inglés; textos visibles y documentación en español. |

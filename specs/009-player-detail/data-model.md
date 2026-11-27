# Modelo de datos: Detalle individual del jugador

**Estado**: Modelo vigente del sistema.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md), [player-statistics.md](contracts/player-statistics.md) y [metric-glossary.md](contracts/metric-glossary.md).

---

## 1. Entidades y modelos de lectura del backend

### Entidades persistidas reutilizadas

| Entidad | Campos principales | Invariantes de consulta |
| --- | --- | --- |
| `Player` | `id`, `name`, `teamId`, `teamName`, `leagueId`, `leagueName`, `position`, `dateOfBirth`, `nationality`, `imageUrl`, `fallbackImageUrl`, `active` | Visibilidad restringida por `findVisibleById`: jugador activo, equipo vigente y liga válida. Inexistente o no visible responden indistintamente 404. |
| `PlayerStatistics` | `id`, `playerId`, `extractionDate`, `source`, `seasonKey`, `seasonLabel`, `competitionKey`, `observedAt`, `contextStatus`, 60 valores métricos (`BigDecimal` nullable) | Solo filas con contexto `RESOLVED` e ítem de extracción `COMPLETED`. Unicidad por `(extraction_date, source, player_id)`. |
| `MetricCatalog` | `key`, `category`, `unit`, `canonicalOrder`, `coverage` | Catálogo canónico de exactamente 60 métricas de WhoScored. |

### Modelo de lectura backend (`PlayerStatisticsSnapshot`)

Representa la composición determinista del snapshot estadístico para un jugador:

- `playerId` (`Long`): identificador del jugador.
- `requestedSeasonKey` (`String` o `null`): temporada solicitada en la petición.
- `status` (`SnapshotStatus`):
  - `AVAILABLE`: 60 métricas con valor no nulo.
  - `PARTIAL`: 1 a 59 métricas con valor no nulo.
  - `NO_DATA`: 0 métricas con valor (jugador sin extracciones confirmadas).
- `seasonKey` (`String` o `null`): temporada efectiva del snapshot resuelto.
- `seasonLabel` (`String` o `null`): etiqueta legible de la temporada.
- `extractionDate` (`LocalDate` o `null`): fecha efectiva de la extracción.
- `competitionKey` (`String` o `null`): clave identificadora de la competición persistida.
- `competitionName` (`String` o `null`): nombre de la competición si existe correspondencia local fiable; `null` en caso contrario.
- `metrics` (`List<SnapshotMetricValue>`): lista de exactamente 60 elementos en orden canónico. Cada elemento contiene:
  - `key` (`String`): clave canónica WhoScored.
  - `unit` (`MetricUnit`): `SEASON_TOTAL`, `PER_MATCH`, `PERCENTAGE` o `AVERAGE_RATING`.
  - `value` (`BigDecimal` o `null`): valor numérico o nulo si no hay registro.
  - `source` (`MetricSource` o `null`): procedencia (`JSON`, `SCRAPING` o `null`).

**Reglas de selección de snapshot**:
1. Filtrar por jugador y temporada (`seasonKey` si se especifica; en su defecto, última fecha disponible del jugador).
2. Ordenar por fecha descendente (`extractionDate DESC`).
3. Anclar contexto en la fuente `JSON`; desempatar por `observedAt DESC` e `id DESC`.
4. Si hay valores nulos en el ancla JSON, completar exclusivamente con registros `SCRAPING` de la misma fecha, jugador, temporada y competición.
5. Si no existe registro JSON, anclar en la extracción de `SCRAPING` confirmada.

---

## 2. Modelos y DTOs del frontend

### Identidad del jugador (`PlayerDetail`)

Mapeado desde `PlayerDetailResponseDTO` (`GET /api/players/{id}`):

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `number` | Identificador interno positivo. |
| `name` | `string` | Nombre del jugador. |
| `teamId` / `teamName` | `number` / `string` | Identificador y nombre del equipo actual. |
| `leagueId` / `leagueName` | `number` / `string` | Identificador y nombre de la liga. |
| `position` | `string` | Posición textual (`Goalkeeper`, `Defender`, `Midfielder`, `Forward`). |
| `dateOfBirth` | `string \| null` | Fecha ISO (`yyyy-MM-dd`) o `null` («No disponible»). |
| `nationality` | `string \| null` | País o `null` («No disponible»). |
| `imageUrl` / `fallbackImageUrl` | `string \| null` | URLs de imágenes con degradación a icono genérico. |
| `active` | `boolean` | Siempre `true` en respuestas exitosas de detalle. |

### Estadísticas del jugador (`PlayerStatistics`)

Mapeado desde `PlayerStatisticsResponseDTO` (`GET /api/players/{id}/statistics`):

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `playerId` | `number` | Identificador del jugador. |
| `status` | `'AVAILABLE' \| 'PARTIAL' \| 'NO_DATA'` | Disponibilidad del snapshot. |
| `seasonKey` / `seasonLabel` | `string \| null` | Temporada resuelta. |
| `extractionDate` | `string \| null` | Fecha ISO de extracción. |
| `competitionKey` / `competitionName` | `string \| null` | Contexto de competición o fallback accesible. |
| `metrics` | `PlayerMetric[]` | Array de exactamente 60 elementos canónicos. |

Cada `PlayerMetric` contiene:
- `key`: clave canónica según [metric-glossary.md](contracts/metric-glossary.md).
- `unit`: unidad técnica del backend (`SEASON_TOTAL`, `PER_MATCH`, `PERCENTAGE`, `AVERAGE_RATING`).
- `value`: número o `null`.
- `source`: procedencia (`JSON`, `SCRAPING` o `null`).

### Cotizaciones del jugador (`PlayerQuote`)

Integrado desde `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current`:

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `currentPrice` | `number \| null` | Precio oficial actual en créditos. |
| `previousPrice` | `number \| null` | Precio de la sesión anterior o referencia base. |
| `quoteDate` | `string \| null` | Fecha ISO de la cotización oficial. |
| `absoluteChange` | `number \| null` | Diferencia en créditos (`currentPrice - previousPrice`). |
| `percentageChange` | `number \| null` | Variación porcentual calculada. |
| `quoteHistory` | `TokenQuotePoint[]` | Serie cronológica de cotizaciones para el gráfico interactivo. |

---

## 3. Estado local y de interacción de la página

El hook `usePlayerDetailPage` centraliza el estado local y la coordinación de consultas:

| Campo | Tipo / Valores | Comportamiento |
| --- | --- | --- |
| `playerId` | `number` | Identificador del jugador obtenido de la ruta activa. |
| `activeTab` | `'quotes'` \| `'summary'` \| `'catalog'` | Pestaña activa. Predeterminada inicial: `'quotes'`. |
| `search` | `string` | Texto de búsqueda local para filtrar métricas en el catálogo. |
| `category` | `'all'` \| categoría canónica | Filtro de categoría en el catálogo. |

### Invariantes de estado y caché

1. **Aislamiento por jugador**: Al cambiar de `playerId`, se reinician `search=""`, `category="all"` y la pestaña vuelve a `'quotes'`. Se cancelan peticiones pendientes del jugador anterior mediante `AbortSignal`.
2. **Conservación de respuesta compatible**: Si una actualización en segundo plano (refetch) falla por error de red, se mantiene visible la última respuesta exitosa en caché del mismo `playerId` con advertencia de desactualización y opción de reintento.
3. **Semántica de nulo frente a cero**: Un valor `0` es un dato válido medido y confirmado; un valor `null` indica ausencia de datos y se representa con un guion (`-`), sin equivalencias entre ambos.

---

## 4. Modelos derivados de presentación

### Tarjetas KPI principales (`SummaryKpiCard`)

Cuatro tarjetas fijas por posición:
- `title`: nombre de la métrica (12 px).
- `value`: valor formateado (28 px bold).
- `icon`: icono representativo (24 px en caja de 48×48 px).
- `context`: información contextual o subtítulo sin redundancia de unidades.

### Paneles temáticos de resumen (`SummaryPanel`)

Dos paneles específicos según la posición del jugador:
- Arquero: *Atajadas por zona* y *Distribución y juego con pies*.
- Defensor: *Acciones defensivas* y *Duelos y disciplina*.
- Mediocampista: *Construcción y distribución* y *Posesión y equilibrio*.
- Delantero: *Definición por zona* y *Juego ofensivo y generación*.

Cada fila de panel cumple:
- `min-height: 44px; max-height: 58px;`
- Espaciado armónico vertical (`justify-content: space-evenly`).

### Grupos del catálogo (`MetricCategoryGroup`)

Estructura de agrupación para las 60 métricas canónicas:
- General (5 métricas)
- Ataque (19 métricas)
- Pases (10 métricas)
- Regates (5 métricas)
- Duelos (3 métricas)
- Defensa (10 métricas)
- Portería (4 métricas)
- Disciplina (4 métricas)

Orden de presentación: Disponibles por prioridad (Principal → Secundaria → Base) y al final las métricas ausentes (`value === null`).

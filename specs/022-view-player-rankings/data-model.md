# Modelo de datos y estado: Visualizar ranking de jugadores

**Feature**: `022-view-player-rankings` · **Contrato de consumo**: [`contracts/player-rankings-consumption.md`](contracts/player-rankings-consumption.md)

Este documento describe DTOs y modelos de presentación del frontend. Las rutas y semánticas normativas permanecen en los contratos backend referenciados.

## DTOs de transporte

### Elementos compartidos

```typescript
interface RankingPlayerDTO {
  name: string;
  position: string | null;
  imageUrl: string | null;
  team: { id: number; name: string; crestUrl: string | null } | null;
  league: { id: number; name: string } | null;
}

interface RankingFilterOptionsDTO {
  leagues: Array<{ id: number; name: string }>;
  positions: string[];
}
```

Las formas de `content` son específicas por modo: score aporta `position`, `playerId`, `score`, `valuationDate`, `player`; quote aporta `position`, `playerId`, `price`, `effectiveDate`, `origin`, `calculatedAt`, `player`. En ambos casos el sobre incluye `content`, `page`, `size`, `totalElements`, `totalPages` y `filterOptions`; score agrega `strategy` y `quoteDate`. La nulabilidad y valores permitidos se definen en los contratos.

## Modelos de presentación

- **RankingMode**: `"score" | "quote"` (o unión equivalente coherente con convenciones de código).
- **RankingFilters**: `name`, `leagueId`, `position`; ausencia representa filtro no aplicado.
- **RankingQueryState**: modo, página base cero, tamaño y filtros efectivos. El contrato fija defaults del backend; la UI muestra 10 filas por defecto y traduce su control de página a base cero.
- **ScoreRankingEntryView / QuoteRankingEntryView**: entrada normalizada para presentación sin perder `position` global, identidad, valor y fechas del modo. No inferir identidad desde catálogo.
- **RankingPageView**: filas, página/tamaño efectivos, total filtrado, total de páginas y opciones globales; la variante score añade estrategia y fecha de cotización.

## Flujo de datos

```text
estado de modo/filtros/paginación ──┬──> query tabla (endpoint + parámetros efectivos)
                                   └──> query podio (mismo endpoint, page=0, size=3, sin filtros)
                                            ↓
                                      DTOs paginados
                                            ↓
                                      Mapper feature
                                      ├── cabecera/opciones
                                      ├── tabla: content y posiciones globales
                                      └── podio: content independiente
```

No hay enriquecimiento de catálogo ni filtrado/paginación cliente. Los filtros modificados o el tamaño/mode reinician la página; navegar páginas conserva filtros. Las dos respuestas del podio y la tabla son solicitudes independientes y pueden observar estados de datos distintos.

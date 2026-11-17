# Modelo de datos: Comprar tokens en el mercado

## Mercado Oficial

### DTOs HTTP

#### `OfficialMarketPageResponseDTO`

| Campo | Tipo | Regla |
| --- | --- | --- |
| `content` | `OfficialMarketPlayerResponseDTO[]` | Colección requerida; longitud no mayor que `size`. |
| `page` | `number` | Entero seguro `>= 0`. |
| `size` | `number` | Entero seguro entre 1 y 100. |
| `totalElements` | `number` | Entero seguro `>= 0`. |
| `totalPages` | `number` | Entero seguro `>= 0`, coherente con total y tamaño. |

#### `OfficialMarketPlayerResponseDTO`

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | `number` | Entero seguro positivo. |
| `name` | `string` | Requerido y no vacío. |
| `position` | `string` | Requerido; valor normalizado del backend. |
| `nationality` | `string \| null` | Nullable. |
| `teamId` | `number \| null` | Nullable; entero positivo cuando existe. |
| `teamName` | `string \| null` | Nullable. |
| `teamBadgeUrl` | `string \| null` | Nullable. |
| `leagueId` | `number \| null` | Nullable; entero positivo cuando existe. |
| `leagueName` | `string \| null` | Nullable. |
| `imageUrl` | `string \| null` | Nullable. |
| `quote` | `OfficialMarketQuoteResponseDTO \| null` | `null` significa ausencia de cotización vigente. |

#### `OfficialMarketQuoteResponseDTO`

| Campo | Tipo | Regla |
| --- | --- | --- |
| `price` | `number` | Entero seguro positivo, en centésimas. |
| `previousPrice` | `number \| null` | Nullable; entero seguro no negativo cuando existe. |
| `variationPercentage` | `number \| null` | Nullable; finito y distinguible de `0`. |
| `performanceScore` | `number` | Finito, validado pero no transferido al Model visual. |
| `multiplier` | `number` | Finito, validado pero no transferido al Model visual. |
| `calculationOrigin` | `string` | Requerido, validado pero no transferido al Model visual. |
| `effectiveDate` | `string` | Fecha ISO `YYYY-MM-DD`. |
| `calculatedAt` | `string` | Instante ISO válido, validado pero no transferido al Model visual. |

### Models

#### `OfficialMarketQuote`

```text
price: number
previousPrice: number | null
variationPercentage: number | null
effectiveDate: string
```

Representa exclusivamente la información financiera aprobada para cards y modal. Variación `0` es neutra; `null` significa que no existe referencia previa calculable.

#### `OfficialMarketPlayer`

```text
id: number
name: string
position: string
nationality: string | null
teamId: number | null
teamName: string | null
teamBadgeUrl: string | null
leagueId: number | null
leagueName: string | null
imageUrl: string | null
quote: OfficialMarketQuote | null
```

No contiene fallback remoto inventado ni campos técnicos del cálculo. Los componentes shared resuelven recursos locales y marcadores accesibles cuando corresponda.

#### `OfficialMarketPage`

```text
players: OfficialMarketPlayer[]
page: number
size: number
totalElements: number
totalPages: number
```

Es el resultado del Mapper y la unidad cacheada por TanStack Query.

#### `OfficialMarketCriteria`

```text
name: string
leagueId: number | null
teamId: number | null
position: string | null
nationality: string | null
hasQuote: boolean | null
minPrice: number | null
maxPrice: number | null
sort: OfficialMarketSort
page: number
size: 12 | 24 | 48
```

`minPrice` y `maxPrice` se conservan internamente en centésimas. `OfficialMarketSort` admite `price`, `variation`, `name` e `id` con las direcciones contractuales. La representación URL usa exactamente las claves de la spec.

#### `OfficialMarketFilterDraft`

Mantiene el texto editable de búsqueda y de precios en créditos. Se transforma en criterios solo cuando cada valor es representable; un rango invertido produce error local y no consulta.

### Estados

```text
URL válida ──parse──> criterios ──query──> pending | success | error
                           │                   │
                           └─ rango inválido ──┘ (query pausada)

modal: closed -> editing -> submitting -> success -> closed
                              └───────> error -> editing
```

- `quote === null`: jugador visible, compra deshabilitada.
- `variationPercentage === null`: ausencia de referencia, no variación cero.
- Durante `fetching` con datos previos, la UI anuncia actualización; la selección transaccional se reinicia si deja de pertenecer al resultado vigente.

### Query keys e invalidación

```text
marketQueryKeys.officialAll() = ["market", "official"]
marketQueryKeys.official(criteria) = ["market", "official", normalizedCriteria]
```

Éxito oficial invalida:

1. `marketQueryKeys.officialAll()`;
2. `authQueryKeys.all`;
3. `portfolioQueryKeys.summary`;
4. prefijo de `portfolioQueryKeys.holdings(...)`.

## Compra Oficial

### `OrderBuyRequestDTO`

`playerId: number` y `quantity: number` entero entre 1 y 100. No incluye `listingId`, precio ni usuario.

### `OrderBuyResponseDTO` / `BuyResult`

`transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `executedAt` y `listingId?`. El Mapper valida enteros positivos, `totalAmount === quantity * unitPrice`, instante ISO y `listingId` cuando exista. El resultado oficial no usa `listingId`.

### `BuyQuantityForm` / `BuyEstimation`

- Formulario: `quantity` textual, inicial `"1"`, válido solo para enteros 1–100.
- Estimación: cantidad validada × precio actual, siempre en centésimas.
- Accesos rápidos reemplazan el valor por 1, 5, 10, 50 o 100; `−/+` cambian una unidad.

## Mercado P2P

### DTOs y Models

- `TokenListingsDTO`: envoltura con `listings` requerida.
- `TokenListingDTO` / `MarketListing`: `listingId`, `sellerId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `status`, `createdAt` y `expiresAt`. El Mapper exige IDs e importes positivos seguros, `status === "ACTIVE"`, total consistente y fechas ISO válidas. `sellerId` no se presenta.
- `CommunityMarketPlayer`: identidad deportiva, publicaciones ordenadas, `listingCount`, suma segura `availableQuantity` y `minimumUnitPrice`.
- `BuyListingRequestDTO`: contiene exclusivamente `listingId`.
- `BuyResult`: reutiliza el resultado de compra y exige que `listingId` coincida con la publicación solicitada.

### Estado navegable

```text
search: string
league: string | "all"
position: string | "all"
sort: "price" | "quantity" | "name"
page: number
player: number | null
```

La URL usa exactamente `search`, `league`, `position`, `sort`, `page` y `player`. Los defaults pueden omitirse. `search` se publica después de 300 ms de debounce; `league`, `position` y `sort` se publican inmediatamente; cambiar cualquier filtro u orden fija `page=0`; abrir o cerrar detalle modifica únicamente `player`.

### Derivación y estados

```text
listings válidos + jugadores resolubles
  -> agrupar por playerId
  -> calcular conteo, cantidad total y precio mínimo
  -> filtrar por search/league/position
  -> ordenar con desempate determinista
  -> paginar localmente de a 12
```

El detalle selecciona el `CommunityMarketPlayer` identificado por `player` y reutiliza sus listings ya ordenados por precio unitario, vencimiento e ID ascendentes. Un jugador ausente o sin publicaciones produce estado vacío de detalle, no datos inventados.

```text
modal: closed -> confirming -> submitting -> success -> closed
                                └───────> error -> confirming
```

- Éxito: permanece visible hasta cierre explícito.
- Conflicto de disponibilidad: invalida listings y actualiza catálogo/detalle.
- Fallo de red incierto: no reintenta automáticamente ni afirma resultado.
- `marketQueryKeys.listings()` permanece independiente del prefijo oficial.

Éxito P2P invalida `listings()`, `authQueryKeys.all`, `portfolioQueryKeys.summary` y el prefijo de holdings.

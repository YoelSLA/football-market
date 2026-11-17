# Contrato de consumo: Mercado Oficial

**Fuente autoritativa**: [SPEC-010](../../010-buy-tokens/contracts/official-catalog.md). Este documento define la frontera consumidora de SPEC-019 sin redefinir el backend.

## Catálogo consolidado

`GET /api/market/official`, autenticado.

### Parámetros

| Parámetro | Tipo | Default backend | Uso frontend |
| --- | --- | --- | --- |
| `page` | entero `>= 0` | `0` | URL base 0. |
| `size` | entero `1..100` | `20` | UI limitada a `12`, `24`, `48`; default UI `12`. |
| `sort` | string | `id,asc` | Un criterio: `price`, `variation`, `name`, `id`; dirección `asc` o `desc`. |
| `name` | string | omitido | Nombre/apellido; trim, parcial, insensible a mayúsculas y acentos. |
| `leagueId` | entero | omitido | Identificador de liga. |
| `teamId` | entero | omitido | Identificador de equipo. |
| `position` | string | omitido | Posición normalizada, por ejemplo `POR`, `DEF`, `MED`, `DEL`. |
| `nationality` | string | omitido | Nacionalidad. |
| `hasQuote` | boolean | omitido | `true`, `false` o sin restricción. |
| `minPrice` | entero `>= 0` | omitido | Centésimas, inclusivo. |
| `maxPrice` | entero `>= 0` | omitido | Centésimas, inclusivo; debe ser `>= minPrice`. |

La URL del navegador usa exactamente estas claves. El frontend no envía strings vacíos, nulos ni valores fuera de contrato. Todo filtro, orden o tamaño cambia `page` a `0`; la búsqueda se publica con debounce.

### Respuesta 200

```ts
interface OfficialMarketPageResponseDTO {
  content: OfficialMarketPlayerResponseDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface OfficialMarketPlayerResponseDTO {
  id: number;
  name: string;
  position: string;
  nationality: string | null;
  teamId: number | null;
  teamName: string | null;
  teamBadgeUrl: string | null;
  leagueId: number | null;
  leagueName: string | null;
  imageUrl: string | null;
  quote: OfficialMarketQuoteResponseDTO | null;
}

interface OfficialMarketQuoteResponseDTO {
  price: number;
  previousPrice: number | null;
  variationPercentage: number | null;
  performanceScore: number;
  multiplier: number;
  calculationOrigin: string;
  effectiveDate: string;
  calculatedAt: string;
}
```

El Mapper valida la respuesta completa. El Model de UI conserva únicamente identidad deportiva, `price`, `previousPrice`, `variationPercentage` y `effectiveDate`; no conserva para presentación `performanceScore`, `multiplier`, `calculationOrigin` ni `calculatedAt`.

`quote: null` es éxito contractual: el jugador se muestra como «Sin cotización» y no se compra. `previousPrice: null` o `variationPercentage: null` muestran ausencia neutral de referencia; `variationPercentage: 0` muestra variación neutra real.

### Errores

| HTTP / código | Comportamiento consumidor |
| --- | --- |
| `400 INVALID_PRICE_RANGE` | No debe producirse desde controles válidos; si llega, mostrar error de consulta y permitir corregir/reintentar. |
| `400 INVALID_PAGINATION` | Normalizar URL inválida; si llega, mostrar error seguro. |
| `400 INVALID_PAGE_SIZE` | Normalizar a `12`; si llega, mostrar error seguro. |
| `400 INVALID_SORT_PARAMETER` | Normalizar al default; si llega, mostrar error seguro. |
| `400 INVALID_PARAMETER_TYPE` | Normalizar valores de URL; si llega, mostrar error seguro. |
| `401` | Gestión global de sesión; no restaurar compra. |
| `500 INTERNAL_ERROR`, red | Estado de error con reintento; no presentar datos inválidos como catálogo completo. |
| Respuesta 200 inválida | Error de respuesta; no exponer DTO parcial ni habilitar compra. |

### Query y caché

```text
marketQueryKeys.officialAll() -> ["market", "official"]
marketQueryKeys.official(criteria) -> ["market", "official", normalizedCriteria]
```

Una consulta reemplazada se cancela mediante `AbortSignal`. Abrir el modal reutiliza el Model de la página y no dispara otra lectura. Una compra oficial exitosa invalida el prefijo `officialAll()`.

## Opciones deportivas

`GET /api/players/filters`, autenticado, consumido mediante la API pública existente de `players`.

```ts
interface PlayerCatalogFiltersDTO {
  leagues: { id: number; name: string }[];
  teams: { id: number; name: string; leagueId: number }[];
  positions: string[];
  nationalities: string[];
}
```

`market` no duplica este Service, Mapper ni query key. El equipo se restringe por `leagueId`; una liga ausente admite todas las opciones y una liga cambiada elimina un equipo incompatible.

## Sigilo y límites

- El catálogo no contiene stock, inventario, saldo ni tenencias del emisor oficial.
- La presencia del jugador no garantiza stock; el POST decide disponibilidad.
- La UI no compone múltiples criterios `sort` ni calcula orden global localmente.
- La modalidad P2P no consume este endpoint ni comparte su query key.

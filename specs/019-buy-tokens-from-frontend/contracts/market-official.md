# Contrato de consumo: Catálogo del Mercado Oficial (SPEC-019 / SPEC-010)

**Fuentes autoritativas:** [Issue #90](https://github.com/YoelSLA/football-market/issues/90) y [SPEC-010](../../010-buy-tokens/spec.md). Este documento define el contrato de lectura autenticada que consume el frontend en `/market/buy`. [Spec](../spec.md) · [Plan](../plan.md) · [Tasks](../tasks.md).

---

## 1. Endpoint HTTP

`GET /api/market/official`

- **Seguridad:** Requiere encabezado `Authorization: Bearer <JWT>`. Sin sesión válida o token vencido responde `401 Unauthorized`.
- **Propósito:** Devuelve el catálogo paginado de jugadores activos y visibles con su cotización vigente integrada en una única respuesta consolidada, aplicando filtros y ordenamiento en el servidor.
- **Sigilo de inventario:** La respuesta no incluye existencias, stock remanente, balance del superusuario ni tenencias de emisión.

---

## 2. Parámetros de consulta (Query Parameters)

Todos los parámetros son opcionales y se envían como query params en la URL:

| Parámetro | Tipo | Descripción | Validación / Valores |
|---|---|---|---|
| `page` | `number` | Índice de página (base 0). | Entero ≥ 0. Predeterminado: `0`. |
| `size` | `number` | Cantidad de elementos por página. | Entero entre 1 y 100. Predeterminado: `20` (en UI: `12`). |
| `name` | `string` | Búsqueda por nombre de jugador. | Texto normalizado insensible a mayúsculas y acentos. |
| `leagueId` | `number` | Identificador de liga. | Entero positivo (Long). |
| `teamId` | `number` | Identificador de equipo. | Entero positivo (Long). |
| `position` | `string` | Posición deportiva del jugador. | Enum de posición (`Goalkeeper`, `Defence`, `Midfield`, `Offence`). |
| `nationality` | `string` | Nacionalidad del jugador. | Texto normalizado. |
| `hasQuote` | `boolean` | Filtro por disponibilidad de cotización vigente. | `true`: solo con cotización.<br>`false`: solo sin cotización.<br>Ausente: ambos. |
| `minPrice` | `number` | Precio unitario mínimo en centésimas. | Entero ≥ 0. |
| `maxPrice` | `number` | Precio unitario máximo en centésimas. | Entero ≥ 0. Si `minPrice > maxPrice` → `400 Bad Request`. |
| `sort` | `string` | Criterio y dirección de ordenamiento. | `price,asc` \| `price,desc`<br>`variation,asc` \| `variation,desc`<br>`name,asc` \| `name,desc`<br>`id,asc` (orden natural predeterminado). |

### Regla de ordenamiento `NULLS LAST`
Para ordenamientos financieros (`price` y `variation`), los valores nulos (activos sin cotización o cotizaciones sin precio anterior válido) se posicionan obligatoriamente **al final de los resultados** (`NULLS LAST`), tanto en sentido ascendente como descendente.

---

## 3. Esquema de respuesta exitosa (200 OK)

```ts
export interface OfficialMarketPageDTO {
  content: OfficialMarketPlayerDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface OfficialMarketPlayerDTO {
  id: number;
  name: string;
  teamId: number;
  teamName: string;
  leagueId: number;
  leagueName: string;
  position: string;
  dateOfBirth: string | null;
  nationality: string | null;
  imageUrl: string | null;
  fallbackImageUrl: string | null;
  quote: OfficialMarketQuoteDTO | null;
}

export interface OfficialMarketQuoteDTO {
  unitPrice: number;                 // En centésimas de crédito
  previousPrice: number | null;      // En centésimas; null en cotización inicial
  variationPercentage: number | null;// Porcentaje de variación; null si previousPrice <= 0
  performanceScore: number | null;   // Score de rendimiento técnico
  multiplier: number;                // Multiplicador aplicado
  calculationOrigin: string | null;  // INITIAL, RECOMPUTED, CARRIED_FORWARD
  effectiveDate: string;             // YYYY-MM-DD
  calculatedAt: string;              // ISO-8601 Instant
}
```

---

## 4. Códigos de error y respuestas HTTP

| Código | Error Code | Causa | Manejo en Frontend |
|---|---|---|---|
| `400 Bad Request` | `INVALID_PRICE_RANGE` | `minPrice > maxPrice`. | Validación preventiva en cliente antes de emitir la consulta; muestra error inline. |
| `400 Bad Request` | `INVALID_PARAMETER_TYPE` | Parámetro numérico no parseable. | Prevención por tipado estricto en el hook y serializador. |
| `401 Unauthorized` | — | Sesión no autenticada o token JWT expirado. | Delegación al interceptor global de sesión para redirección a login. |
| `500 Internal Error` | `INTERNAL_ERROR` | Error interno del servidor. | Mensaje seguro de reintento en el estado de error del catálogo. |

---

## 5. Selectores dinámicos en cascada (`GET /api/players/filters`)

Las opciones de los filtros deportivos se obtienen consumiendo `GET /api/players/filters` a través de la API pública de `features/players`:

- **Ligas:** Se listan todas las ligas disponibles (`leagueId`, `leagueName`).
- **Equipos (en cascada):** Si se selecciona una liga (`leagueId`), el selector de equipos se restringe exclusivamente a los equipos pertenecientes a esa liga.
- Limpiar o cambiar la liga seleccionada reinicia automáticamente la selección del equipo.

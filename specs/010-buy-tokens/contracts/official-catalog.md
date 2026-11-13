# Contrato HTTP: Catálogo del Mercado Oficial con cotizaciones integradas

Endpoint: `GET /api/market/official`

Permite a un usuario autenticado consultar el catálogo paginado de jugadores elegibles para emisión primaria con su cotización vigente consolidada en una única respuesta.

---

## Autenticación y Autorización

- **Método**: `GET`
- **Autenticación**: `Bearer <JWT>`
- **Roles autorizados**: `USER`, `ADMIN`
- **Rechazo no autenticado**: `401 Unauthorized`

---

## Request

```http
GET /api/market/official?page=0&size=20&sort=price,desc&hasQuote=true&leagueId=12&minPrice=1000&maxPrice=50000
Authorization: Bearer <JWT>
```

### Parámetros de Consulta (Query Parameters)

| Parámetro | Tipo | Requerido | Valor por defecto | Descripción y Reglas de Validación |
|---|---|---|---|---|
| `page` | integer | No | `0` | Índice de página basado en 0. Debe ser `>= 0`. Valores negativos responden `400 Bad Request` (`INVALID_PAGINATION`). |
| `size` | integer | No | `20` | Cantidad de elementos por página. Debe estar entre `1` y `100` inclusive. Valores fuera de rango responden `400 Bad Request` (`INVALID_PAGE_SIZE`). |
| `sort` | string | No | `id,asc` | Criterio y dirección de ordenamiento con formato `propiedad,direccion`. Solo se admite un **único criterio principal explícito** por solicitud. Propiedades permitidas: `price`, `variation`, `name`, `id`. Direcciones permitidas: `asc`, `desc`. En ordenamientos por `price` o `variation`, los valores nulos siempre se ubican al final (`NULLS LAST`). Los empates se resuelven internamente por `id ASC`. Múltiples parámetros `sort`, criterios combinados o propiedades/direcciones inválidas responden `400 Bad Request` (`INVALID_SORT_PARAMETER`). |
| `name` | string | No | — | Filtro por nombre o apellido del jugador. Búsqueda parcial insensible a mayúsculas, minúsculas y acentos. Se aplica normalización eliminando espacios en los extremos (`trim`); si el valor resultante queda vacío, el filtro se ignora y no restringe la búsqueda. |
| `leagueId` | integer (Long) | No | — | Filtro por identificador de la liga deportiva del jugador. |
| `teamId` | integer (Long) | No | — | Filtro por identificador del club o equipo deportivo. |
| `position` | string | No | — | Filtro por posición deportiva del jugador (ej. `DEL`, `MED`, `DEF`, `POR`). |
| `nationality` | string | No | — | Filtro por país de nacionalidad del jugador. |
| `hasQuote` | boolean | No | — | Filtro por disponibilidad de cotización vigente: `true` (solo con cotización), `false` (solo sin cotización). Si se omite, incluye jugadores con y sin cotización. |
| `minPrice` | integer (Long) | No | — | Precio mínimo de cotización vigente en **centésimas de crédito** (inclusivo). Debe ser un valor no negativo (`>= 0`). Si `minPrice < 0`, responde `400 Bad Request` (`INVALID_PRICE_RANGE`). |
| `maxPrice` | integer (Long) | No | — | Precio máximo de cotización vigente en **centésimas de crédito** (inclusivo). Debe ser un valor no negativo (`>= 0`). Si `maxPrice < 0` o `minPrice > maxPrice`, responde `400 Bad Request` (`INVALID_PRICE_RANGE`). |

---

## Respuestas

### 200 OK — Consulta Exitosa

```json
{
  "content": [
    {
      "id": 7821,
      "name": "Lionel Messi",
      "position": "DEL",
      "nationality": "Argentina",
      "teamId": 45,
      "teamName": "Inter Miami CF",
      "teamBadgeUrl": "https://images.footballmarket.com/teams/45.png",
      "leagueId": 12,
      "leagueName": "Major League Soccer",
      "imageUrl": "https://images.footballmarket.com/players/7821.png",
      "quote": {
        "price": 25000,
        "previousPrice": 24000,
        "variationPercentage": 4.17,
        "performanceScore": 8.92,
        "multiplier": 1.25,
        "calculationOrigin": "MANUAL_RECALCULATION",
        "effectiveDate": "2026-11-12",
        "calculatedAt": "2026-11-12T15:30:00Z"
      }
    },
    {
      "id": 9102,
      "name": "Julián Álvarez",
      "position": "DEL",
      "nationality": "Argentina",
      "teamId": 23,
      "teamName": "Atlético de Madrid",
      "teamBadgeUrl": "https://images.footballmarket.com/teams/23.png",
      "leagueId": 8,
      "leagueName": "La Liga",
      "imageUrl": "https://images.footballmarket.com/players/9102.png",
      "quote": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

#### Estructura del Item de Jugador

| Campo | Tipo | Nulabilidad | Descripción |
|---|---|---|---|
| `id` | integer (Long) | No | Identificador único del jugador. |
| `name` | string | No | Nombre completo del jugador. |
| `position` | string | No | Posición deportiva normalizada. |
| `nationality` | string | Sí | País de nacionalidad del jugador. |
| `teamId` | integer (Long) | Sí | Identificador del club deportivo actual. |
| `teamName` | string | Sí | Nombre del club deportivo actual. |
| `teamBadgeUrl` | string | Sí | URL del escudo oficial del club. |
| `leagueId` | integer (Long) | Sí | Identificador de la liga de procedencia. |
| `leagueName` | string | Sí | Nombre de la liga de procedencia. |
| `imageUrl` | string | Sí | URL de la fotografía del jugador. |
| `quote` | object | Sí | Bloque de cotización vigente consolidada. Es `null` si el jugador no registra cotización vigente en el sistema. |

#### Estructura del Objeto `quote`

| Campo | Tipo | Nulabilidad | Descripción |
|---|---|---|---|
| `price` | integer (Long) | No | Precio unitario vigente en **centésimas de crédito** (ej. 25000 = 250.00 créditos). |
| `previousPrice` | integer (Long) | Sí | Precio unitario de la cotización inmediatamente anterior en centésimas. Puede ser `null` en cotizaciones iniciales. |
| `variationPercentage` | number (Float) | Sí | Variación porcentual redondeada a 2 decimales calculada como `((price - previousPrice) / previousPrice) * 100`. Es estrictamente `null` si `previousPrice` es nulo o `<= 0`. Variación cero se expresa como `0.0` (no `null`). |
| `performanceScore` | number (Float) | No | Score técnico de rendimiento que originó la valuación. |
| `multiplier` | number (Float) | No | Multiplicador de valuación aplicado. |
| `calculationOrigin` | string | No | Origen o disparador del cálculo (ej. `MANUAL_RECALCULATION`, `SCHEDULED_TASK`, `INITIAL_PROVISIONING`). |
| `effectiveDate` | string (ISO Date) | No | Fecha representativa de vigencia (`YYYY-MM-DD`). |
| `calculatedAt` | string (ISO Timestamp) | No | Instante exacto UTC en que se persistió el cálculo. |

#### Metadatos de Paginación

| Campo | Tipo | Descripción |
|---|---|---|
| `page` | integer | Índice de página actual (base 0). |
| `size` | integer | Tamaño de página solicitado o efectivo. |
| `totalElements` | integer (Long) | Total de jugadores que coinciden con todos los filtros sobre la base de datos completa. |
| `totalPages` | integer | Total de páginas disponibles según `totalElements` y `size`. |

---

## Reglas de Negocio y Sigilo

1. **Sigilo de Inventario**: La respuesta no contiene campos de stock disponible, balance ni inventario remanente del superusuario.
2. **Sin Afirmación de Compra Inmediata**: La presencia de un jugador en el catálogo oficial refleja su elegibilidad y visibilidad general, pero no garantiza la existencia de stock para compra. La validación de inventario se ejecuta estrictamente al confirmar `POST /api/orders/buy`.
3. **Filtros Mutuamente Excluyentes**: Solicitudes con combinaciones de filtros válidos pero sin resultados coincidentes (ej. `hasQuote=false` y `minPrice=1000`) responden `200 OK` con `content: []`, `totalElements: 0` y `totalPages: 0`.
4. **Desempate Determinista**: Si múltiples registros tienen el mismo valor en el criterio de ordenamiento principal, el desempate determinista se realiza siempre de forma automática por `id ASC`.
5. **Criterio de Ordenamiento Único**: No se permite componer múltiples parámetros de ordenación en la query (`sort=price,desc&sort=name,asc`). Toda solicitud que contenga más de un criterio responde `400 Bad Request` (`INVALID_SORT_PARAMETER`).
6. **Normalización de Texto**: El filtro `name` se procesa aplicando recorte de espacios en blanco en los extremos. Si tras recortar el valor queda vacío, se omite el filtrado de nombre sin emitir error.

---

## Errores Controlados

| Código HTTP | Error Code | Causa |
|---|---|---|
| `400 Bad Request` | `INVALID_PRICE_RANGE` | `minPrice < 0`, `maxPrice < 0` o `minPrice > maxPrice`. |
| `400 Bad Request` | `INVALID_PAGINATION` | `page` es menor que 0. |
| `400 Bad Request` | `INVALID_PAGE_SIZE` | `size` es menor que 1 o mayor que 100. |
| `400 Bad Request` | `INVALID_SORT_PARAMETER` | Propiedad o dirección no permitida, formato malformado o más de un criterio de ordenamiento en la solicitud. |
| `400 Bad Request` | `INVALID_PARAMETER_TYPE` | Parámetro con tipo de dato no representable (ej. texto en campos numéricos). |
| `401 Unauthorized` | — | Sesión ausente, inválida o expirada. |
| `500 Internal Server Error` | `INTERNAL_ERROR` | Error imprevisto al procesar la consulta. |

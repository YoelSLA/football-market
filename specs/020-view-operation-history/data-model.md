# Modelo de datos: Historial personal de operaciones (Backend)

**Estado**: Modelo vigente del backend.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md) y [contracts/operation-history-api.md](contracts/operation-history-api.md).

---

## 1. Entidades persistidas y relaciones existentes

La funcionalidad es de solo lectura y no introduce entidades ni migraciones de base de datos. Consume el esquema existente de `backend/src/main/resources/db/migration/`:

### `MarketTransaction` (Fuente de verdad)

Registro confirmado e inmutable en base de datos (`market_transactions`):

| Campo | Tipo | Significado |
| --- | --- | --- |
| `id` | `Long` | Identificador único de la transacción; criterio de desempate descendente. |
| `buyer` | `User` | Usuario comprador; determina el tipo `BUY` cuando coincide con la sesión. |
| `seller` | `User` | Usuario vendedor; determina el tipo `SELL` cuando coincide con la sesión. |
| `player` | `Player` | Jugador cuyos tokens fueron negociados; aporta el perfil actual del catálogo. |
| `quantity` | `int` | Cantidad de tokens negociados (`> 0`). |
| `unitPrice` | `long` | Precio unitario histórico en centésimas de crédito (`> 0`). |
| `totalAmount` | `long` | Importe total histórico en centésimas de crédito (`quantity × unitPrice`). |
| `executedAt` | `OffsetDateTime` | Instante de confirmación de la operación en UTC. |

### Relaciones asociadas al jugador

- `Player` → `Team` (`team_id`): Equipo actual del jugador.
- `Team` → `League` (`league_id`): Liga actual del equipo.
- `imageUrl` y `fallbackImageUrl`: URLs de retratos del jugador (`null` si no están informadas).

---

## 2. Tipos y modelos de dominio del backend

Records inmutables desacoplados de la capa de transporte:

### `OperationHistoryEntry`

Representa una transacción proyectada desde la perspectiva del usuario autenticado:

- `transactionId` (`Long`): identificador de la transacción.
- `type` (`OperationType`): enum con valores `BUY` o `SELL`.
- `player` (`PlayerView`): record con `id`, `name`, `imageUrl`, `fallbackImageUrl`, `team` (`id`, `name`) y `league` (`id`, `name`).
- `quantity` (`int`): cantidad de tokens.
- `unitPrice` (`long`): precio unitario en centésimas.
- `totalAmount` (`long`): importe total en centésimas.
- `executedAt` (`OffsetDateTime`): instante de confirmación.

### `OperationHistoryCriteria`

Encapsula los criterios de filtrado, orden y paginación validados:

- `page` (`int`): índice de página (`>= 0`).
- `size` (`int`): tamaño de página (`1 <= size <= 100`).
- `search` (`String` opcional): texto normalizado para búsqueda sobre el nombre del jugador.
- `type` (`String` opcional): filtro por tipo `BUY` o `SELL`.
- `executedFrom` (`OffsetDateTime` opcional): límite temporal inferior inclusivo.
- `executedBefore` (`OffsetDateTime` opcional): límite temporal superior exclusivo.
- `sort` (`String`): ordenamiento cerrado (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`).

### `OperationHistoryAggregate`

Encapsula las métricas acumuladas calculadas en base de datos para todo el conjunto filtrado:

- `totalTransactions` (`long`): total de operaciones que cumplen los filtros.
- `buyTransactions` (`long`): cantidad de compras.
- `sellTransactions` (`long`): cantidad de ventas.
- `boughtTokens` (`long`): suma total de tokens comprados.
- `soldTokens` (`long`): suma total de tokens vendidos.
- `purchasedVolume` (`long`): volumen total de compras en centésimas de crédito.
- `soldVolume` (`long`): volumen total de ventas en centésimas de crédito.

### `OperationHistoryView`

Agrupa el resultado completo del servicio:

- `entries` (`List<OperationHistoryEntry>`): transacciones de la página actual.
- `aggregate` (`OperationHistoryAggregate`): métricas agregadas del conjunto filtrado.
- `page` (`int`): página actual.
- `size` (`int`): tamaño de página.
- `totalElements` (`long`): total de transacciones coincidentes.
- `totalPages` (`int`): total de páginas disponibles.

---

## 3. Modelo de transporte (`OperationHistoryResponseDTO`)

Estructura de respuesta HTTP documentada para OpenAPI:

```json
{
  "summary": {
    "totalTransactions": 124,
    "buyTransactions": 76,
    "sellTransactions": 48,
    "boughtTokens": 231,
    "soldTokens": 109,
    "purchasedVolume": 1245000,
    "soldVolume": 892000
  },
  "transactions": [
    {
      "transactionId": 92,
      "type": "BUY",
      "player": {
        "id": 41,
        "name": "Lionel Messi",
        "imageUrl": "https://example.test/player-cutout.png",
        "fallbackImageUrl": "https://example.test/player-thumbnail.png",
        "team": { "id": 17, "name": "Inter Miami CF" },
        "league": { "id": 8, "name": "Major League Soccer" }
      },
      "quantity": 5,
      "unitPrice": 125,
      "totalAmount": 625,
      "executedAt": "2026-11-10T14:30:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 124,
    "totalPages": 7
  }
}
```

---

## 4. Invariantes de semántica

1. **Datos históricos**: `transactionId`, `type`, `quantity`, `unitPrice`, `totalAmount` y `executedAt` son estrictamente inmutables y reproducen el registro confirmado.
2. **Datos actuales**: `player.name`, `imageUrl`, `fallbackImageUrl`, `team` y `league` se obtienen de las entidades vigentes del catálogo y no representan una fotografía histórica.
3. **Perspectiva del usuario**: Una misma transacción física se presenta como `BUY` para el comprador y como `SELL` para el vendedor.

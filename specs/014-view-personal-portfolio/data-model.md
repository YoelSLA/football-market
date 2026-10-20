# Modelo de datos — SPEC-014: consulta del portfolio personal

**Feature**: SPEC-014 · **Rama**: `feat/014-consultar-portfolio-personal` · **Fecha**: 2026-10-20
**Plan**: [`../plan.md`](../plan.md) · **Research**: [`../research.md`](../research.md) ·
**Contrato**: [`../contracts/portfolio-api.md`](../contracts/portfolio-api.md)

Este documento describe la forma de la respuesta, su origen campo por campo y las reglas de
validez. La consulta **no persiste nada**: no hay tablas, columnas, vistas ni cachés nuevas.

---

## 1. Persistencia: qué se lee y qué no

| Tabla | Uso en la consulta | Cambia |
| --- | --- | --- |
| `player_tokens` | Tenencia por propietario: cantidad poseída de cada jugador | No |
| `token_listings` | Reservas: publicaciones del propietario efectivamente activas y vigentes, agrupadas por jugador | No |
| `players`, `teams` | Datos descriptivos del jugador: nombre, equipo, posición deportiva, imagen y estado activo | No |
| `market_transactions`, `player_token_emissions` | **No se consultan**: su lectura y su interpretación contable pertenecen al componente de SPEC-015 | No |

### 1.1 Única migración: un índice

`player_tokens` tiene `UNIQUE (player_id, owner_user_id)`, con `player_id` como columna líder: el
índice no sirve para listar las tenencias de un propietario. El índice por propietario fue diferido
de forma explícita porque "no existe consulta de portfolio ni historial"
(`specs/010-buy-tokens/data-model.md:82`). Esta feature es esa consulta, de modo que la migración
añade:

```sql
CREATE INDEX idx_player_tokens_owner ON player_tokens (owner_user_id, player_id);
```

Es un índice, no una fuente de verdad: no almacena verdad contable ni altera ninguna invariante. El
índice es compuesto porque sirve tanto al listado por propietario como a la resolución de una tenencia
concreta.

Sobre `token_listings` **no se crea ningún índice nuevo**: el vigente
`idx_token_listings_seller (seller_user_id, created_at DESC)` cubre el filtro por vendedor, y la
agrupación por jugador es un agregado sobre ese conjunto.

### 1.2 Consultas nuevas

| Repositorio | Consulta | Propósito |
| --- | --- | --- |
| `PlayerTokenRepository` | Tenencias del propietario, con su jugador y su cantidad | Cantidad poseída por jugador |
| `TokenListingRepository` | Reservas del propietario agrupadas por jugador, entre publicaciones `ACTIVE` y no vencidas | Cantidad reservada por jugador |
| `PlayerRepository` | Jugadores de la página por identificadores, con su equipo | Datos descriptivos |

Ninguna consulta se resuelve por posición.

---

## 2. Componentes consumidos y regla de propiedad

La regla de propiedad de las cifras es la frontera entre ambas features y no admite excepción:

| Dato | Dueño | Origen |
| --- | --- | --- |
| Costo de adquisición, precio medio derivado, resultado realizado, valor actual, resultado no realizado, resultado total | **SPEC-015** | `PlayerFinancialPosition` |
| Porcentaje global, agregados contables, `valuationComplete` | **SPEC-015** | Agregación de `FinancialPositionCalculator` |
| Instante de la cotización, ciclo de tenencia, recuentos por rol, última operación | **SPEC-015** | Hechos del mismo plegado |
| Cantidad poseída, reservada y disponible | **SPEC-014** | `player_tokens` y `token_listings` |
| Porcentajes por posición | **SPEC-014** | Fórmulas propias |

**Reglas de consumo que esta feature respeta** (contrato de SPEC-015 §3.2): el costo de adquisición es
la cifra autoritativa y el precio medio nunca lo reconstruye; una cifra nula es indefinida y nunca un
cero; el resultado realizado llega ya acumulado; el porcentaje global y los agregados contables no se
recalculan y se aplican sobre el **conjunto completo** de posiciones, nunca sobre una página; y los
porcentajes por posición no forman parte del contrato contable.

---

## 3. Forma de la respuesta

### 3.1 `summary` — indicadores globales

| Campo | Tipo | Origen | Indefinido cuando |
| --- | --- | --- | --- |
| `totalPositions` | `int` | Conteo de posiciones **abiertas** del conjunto completo | Nunca |
| `totalTokens` | `int` | Σ de la cantidad de la **tenencia persistida** del usuario, lectura propia de esta feature; coincide con la cantidad reconstruida por el componente contable mientras la comprobación de divergencia no detecte inconsistencia | Nunca |
| `totalReservedTokens` | `int` | Σ de reservas del propietario | Nunca |
| `totalAvailableTokens` | `int` | `totalTokens − totalReservedTokens` | Nunca |
| `totalInvested` | `long` | `investedCost` de la agregación contable | Nunca |
| `totalCurrentValue` | `long` | `currentValue` de la agregación contable | Alguna posición abierta sin cotización |
| `totalRealizedProfitLoss` | `long` | `realizedProfitLoss` de la agregación contable | Nunca |
| `totalUnrealizedProfitLoss` | `long` | `unrealizedProfitLoss` de la agregación contable | Alguna posición abierta sin cotización |
| `totalProfitLoss` | `long` | `totalProfitLoss` de la agregación contable | Alguna posición abierta sin cotización |
| `totalProfitLossPercentage` | `BigDecimal` | `profitLossPercentage` de la agregación contable | Denominador cero, o alguna posición abierta sin cotización |
| `totalBuyTransactions` | `int` | Σ de `buyOperationCount` de todas las posiciones | Nunca |
| `totalSellTransactions` | `int` | Σ de `sellOperationCount` de todas las posiciones | Nunca |
| `valuationComplete` | `boolean` | `valuationComplete` de la agregación contable | Nunca |

**Invariante**: `totalTokens = totalReservedTokens + totalAvailableTokens` (SC-002).

**Invariante de agregación**: `totalInvested` es el costo de adquisición de las posiciones
**abiertas**; el denominador del porcentaje global es el **costo histórico total pagado**, que
incluye los ciclos cerrados. Son importes distintos y esta feature no los confunde.

### 3.2 `positions` — detalle por posición

| Campo | Tipo | Origen | Indefinido cuando |
| --- | --- | --- | --- |
| `playerId` | `Long` | Componente contable | Nunca |
| `playerName` | `String` | Catálogo | Jugador sin dato recuperable |
| `teamName` | `String` | Catálogo | Ídem |
| `position` | `String` | Catálogo | Ídem |
| `imageUrl` | `String` | Catálogo | Sin imagen persistida |
| `active` | `boolean` | Catálogo | Nunca |
| `status` | `String` | `OPEN` o `CLOSED`, derivado de la cantidad abierta | Nunca |
| `valuationStatus` | `String` | `VALUED`, `UNVALUED` o `NOT_APPLICABLE` | Nunca |
| `quantity` | `int` | Tenencia del propietario | Nunca |
| `reservedQuantity` | `int` | Publicaciones del propietario activas y vigentes | Nunca |
| `availableQuantity` | `int` | `quantity − reservedQuantity` | Nunca |
| `acquisitionCost` | `long` | Componente contable | Nunca |
| `averageUnitPrice` | `long` | Componente contable | Posición sin cantidad abierta |
| `currentUnitPrice` | `Long` | Componente contable | Sin cotización, o posición cerrada |
| `priceUpdatedAt` | `OffsetDateTime` | Componente contable | Ídem |
| `currentValue` | `Long` | Componente contable | Sin cotización, o posición cerrada |
| `realizedProfitLoss` | `long` | Componente contable | Nunca |
| `unrealizedProfitLoss` | `Long` | Componente contable | Sin cotización, o posición cerrada |
| `totalProfitLoss` | `Long` | Componente contable | Sin cotización; en posición cerrada, el realizado acumulado |
| `unrealizedProfitLossPercentage` | `BigDecimal` | Fórmula de esta feature | Sin cotización, o costo de adquisición cero |
| `portfolioWeightPercentage` | `BigDecimal` | Fórmula de esta feature | Sin cotización, posición cerrada, o valor total cero o indefinido |
| `openedAt` | `OffsetDateTime` | Componente contable | Nunca |
| `closedAt` | `OffsetDateTime` | Componente contable | Posición abierta |
| `lastTransactionAt` | `OffsetDateTime` | Componente contable | Posición sin operaciones confirmadas |
| `totalBuyTransactions` | `int` | Componente contable | Nunca |
| `totalSellTransactions` | `int` | Componente contable | Nunca |

**Invariantes por posición**: `quantity = reservedQuantity + availableQuantity` (SC-002);
`valuationStatus` es `NOT_APPLICABLE` si y solo si `status` es `CLOSED` (D31).

**Normalización de la posición cerrada**: una posición cerrada no tiene valor actual ni resultado no
realizado; esos campos se informan nulos con independencia de lo que entregue el componente
(`research.md` R-05). Una posición abierta sin cotización conserva cantidad, reserva, disponibilidad
y resultado realizado, y sus campos dependientes de cotización son nulos (D03, AC06).

### 3.3 `pagination` — metadatos de la página

| Campo | Tipo | Significado |
| --- | --- | --- |
| `page` | `int` | Número de página desde cero |
| `size` | `int` | Tamaño solicitado |
| `totalElements` | `long` | Cantidad de posiciones del conjunto **filtrado** |
| `totalPages` | `int` | Cantidad de páginas del conjunto filtrado |

Los metadatos cuentan el conjunto filtrado, no el completo (D23, AC15): con `status=CLOSED`,
`totalElements` es la cantidad de posiciones cerradas. El resumen, en cambio, siempre representa el
portfolio completo.

### 3.4 Fórmulas de esta feature

| Cifra | Fórmula | Redondeo |
| --- | --- | --- |
| `unrealizedProfitLossPercentage` | `unrealizedProfitLoss × 100 / acquisitionCost` | `HALF_UP`, dos decimales |
| `portfolioWeightPercentage` | `currentValue × 100 / totalCurrentValue` | `HALF_UP`, dos decimales |
| `totalAvailableTokens` | `totalTokens − totalReservedTokens` | Entero exacto |

El denominador del peso es el valor total de **todas** las posiciones abiertas, no el de la página
consultada (D24). Ninguna fórmula contable —costo de adquisición, resultados, porcentaje global—
se calcula en esta feature: llega del componente contable.

---

## 4. Estado y transiciones de la posición

| Estado | Condición | Significado |
| --- | --- | --- |
| `OPEN` | Cantidad abierta mayor que cero | Posición abierta: tiene costo de adquisición y puede valorarse |
| `CLOSED` | Cantidad abierta cero con operaciones históricas | Posición cerrada: conserva el resultado realizado acumulado; sin valor actual ni resultado no realizado |

El estado **no se persiste**: se deriva de la cantidad. Su aplicación como filtro, su orden y su
paginación pertenecen a esta feature (D06, D07, D33).

**Transiciones observables** (AC25): una venta confirmada que lleva la tenencia a cero cambia el
estado de `OPEN` a `CLOSED`; una compra posterior lo devuelve a `OPEN` con un ciclo nuevo y
`closedAt` en nulo. Los resultados y los metadatos se recalculan en la siguiente solicitud: no
existe una instantánea compartida entre peticiones distintas (D32).

---

## 5. Coherencia entre la cantidad reconstruida y la tenencia

La cantidad del componente contable es una **reconstrucción** a partir de operaciones y emisión; la
cantidad de la posición es la **tenencia persistida**. Ambas se leen bajo la misma instantánea, de
modo que son comparables.

Si divergen, el estado del mercado es inconsistente y las cifras no corresponderían a ningún estado
real: la consulta responde con un **error de integridad** (500), sin elegir silenciosamente una de las
dos fuentes (`research.md` R-04, FR-040, AC13).

Esta comprobación no entra en conflicto con la regla de jugador eliminado: el borrado físico de un
jugador con operaciones confirmadas está bloqueado por la integridad referencial de
`market_transactions`, y un jugador sin operaciones no produce posición. El caso vigente es la
desactivación lógica, en la que los datos descriptivos se conservan; si alguna vez no fueran
recuperables, se informan nulos sin inventar datos (FR-040).

---

## 6. Referencias

- Especificación funcional: [`../spec.md`](../spec.md)
- Contrato HTTP: [`../contracts/portfolio-api.md`](../contracts/portfolio-api.md)
- Decisiones de diseño: [`../research.md`](../research.md)
- Componente contable consumido: `specs/015-define-average-cost-and-financial-results-rules/contracts/financial-position-service.md`
- Esquema vigente: `backend/src/main/resources/db/migration/V10__create_market_domain.sql`,
  `V20261017233439__create_token_listings.sql`

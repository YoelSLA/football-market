# Modelo de datos: Mercado Oficial — Compra de tokens y catálogo con cotizaciones integradas (SPEC-010)

Persistencia PostgreSQL y proyecciones de lectura del dominio de mercado. Precisión, restricciones y modelos según la consolidación del 2026-10-16 y la evolución del 2026-11-12 (Issue #90).

---

## 1. Tablas transaccionales existentes (Compra de tokens)

### user_credits — Saldo de créditos por usuario
```sql
CREATE TABLE user_credits (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    available_credits BIGINT NOT NULL DEFAULT 0 CHECK (available_credits >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### player_token_holdings — Tenencia de tokens por usuario y jugador
```sql
CREATE TABLE player_token_holdings (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_token_holdings_user_player UNIQUE (user_id, player_id)
);
```

### player_token_emissions — Registro de emisión inicial
```sql
CREATE TABLE player_token_emissions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    initial_supply INT NOT NULL DEFAULT 100 CHECK (initial_supply = 100),
    emitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### market_transactions — Asiento de auditoría financiera inmutable
```sql
CREATE TABLE market_transactions (
    id BIGINT PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users(id),
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount > 0),
    buyer_credits_before BIGINT NOT NULL,
    buyer_credits_after BIGINT NOT NULL,
    seller_credits_before BIGINT NOT NULL,
    seller_credits_after BIGINT NOT NULL,
    buyer_tokens_before INT NOT NULL,
    buyer_tokens_after INT NOT NULL,
    seller_tokens_before INT NOT NULL,
    seller_tokens_after INT NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 2. Persistencia de lectura del Catálogo Oficial (Issue #90)

### Sin migraciones DDL
La evolución del catálogo oficial no requiere la creación de nuevas tablas, vistas permanentes ni migraciones Flyway. Toda la resolución se realiza mediante consultas SQL nativas con `LEFT JOIN LATERAL` encapsuladas en `OfficialMarketRepository`.

### Subconsulta LATERAL de Cotización Vigente
Resuelve la cotización más reciente de cada jugador aplicando los criterios canónicos de ordenamiento:

```sql
LEFT JOIN LATERAL (
    SELECT
        pq.id AS quote_id,
        pq.price,
        pq.previous_price,
        CASE
            WHEN pq.previous_price IS NOT NULL AND pq.previous_price > 0
            THEN ROUND(((pq.price - pq.previous_price)::numeric / pq.previous_price::numeric) * 100, 2)
            ELSE NULL
        END AS variation_percentage,
        pq.score,
        pq.multiplier,
        pq.origin,
        pq.effective_date,
        pq.calculated_at
    FROM player_quotes pq
    WHERE pq.player_id = p.id
    ORDER BY
        pq.effective_date DESC,
        (CASE WHEN pq.origin = 'RECOMPUTED' THEN 0 ELSE 1 END),
        pq.calculated_at DESC,
        pq.id DESC
    LIMIT 1
) q ON true
```

---

## 3. Proyección y Modelo de Aplicación (`features/market`)

### OfficialMarketPlayerProjection (Spring Data Interface / Proyección SQL)
Modela el resultado tabular de la consulta SQL unificada (`players` + `teams` + `leagues` + `LEFT JOIN LATERAL`):

```java
public interface OfficialMarketPlayerProjection {
  Long getId();
  String getName();
  String getPosition();
  String getNationality();
  Long getTeamId();
  String getTeamName();
  String getTeamBadgeUrl();
  Long getLeagueId();
  String getLeagueName();
  String getImageUrl();

  // Quote embedded fields (nullable if player has no quote)
  Long getQuoteId();
  Long getPrice();
  Long getPreviousPrice();
  Double getVariationPercentage();
  Double getScore();
  Double getMultiplier();
  String getOrigin();
  LocalDate getEffectiveDate();
  OffsetDateTime getCalculatedAt();
}
```

### OfficialMarketPlayer (Modelo de Dominio / Aplicación)
Representa la entidad inmutable del catálogo oficial consumida por el servicio:

```java
public record OfficialMarketPlayer(
    Long id,
    String name,
    String position,
    String nationality,
    Long teamId,
    String teamName,
    String teamBadgeUrl,
    Long leagueId,
    String leagueName,
    String imageUrl,
    OfficialMarketQuote quote
) {}

public record OfficialMarketQuote(
    long price,
    Long previousPrice,
    Double variationPercentage,
    double performanceScore,
    double multiplier,
    String calculationOrigin,
    LocalDate effectiveDate,
    OffsetDateTime calculatedAt
) {}
```

---

## 4. Mapeo de Atributos: Proyección SQL → Dominio → DTO de Respuesta

El componente `OfficialMarketMapper` realiza la transformación determinista entre las capas, resolviendo los nombres técnicos de columnas y las reglas de nulabilidad:

| Atributo Proyección SQL (`OfficialMarketPlayerProjection`) | Atributo Dominio (`OfficialMarketPlayer` / `OfficialMarketQuote`) | Atributo DTO / JSON (`contracts/official-catalog.md`) | Regla de Transformación / Nulabilidad |
|---|---|---|---|
| `getId()` | `id` | `id` | Idéntico (Long, no nulo). |
| `getName()` | `name` | `name` | Idéntico (String, no nulo). |
| `getPosition()` | `position` | `position` | Idéntico (String, no nulo). |
| `getNationality()` | `nationality` | `nationality` | Idéntico (String, nullable). |
| `getTeamId()` | `teamId` | `teamId` | Idéntico (Long, nullable). |
| `getTeamName()` | `teamName` | `teamName` | Idéntico (String, nullable). |
| `getTeamBadgeUrl()` | `teamBadgeUrl` | `teamBadgeUrl` | Idéntico (String, nullable). |
| `getLeagueId()` | `leagueId` | `leagueId` | Idéntico (Long, nullable). |
| `getLeagueName()` | `leagueName` | `leagueName` | Idéntico (String, nullable). |
| `getImageUrl()` | `imageUrl` | `imageUrl` | Idéntico (String, nullable). |
| `getQuoteId()` | — | — | Si es `null`, el objeto `quote` completo es `null`. |
| `getPrice()` | `quote.price` | `quote.price` | Idéntico (centésimas de crédito, no nulo si `quote != null`). |
| `getPreviousPrice()` | `quote.previousPrice` | `quote.previousPrice` | Nullable (centésimas de crédito). |
| `getVariationPercentage()` | `quote.variationPercentage` | `quote.variationPercentage` | Estrictamente `null` si `previousPrice <= 0` o nulo; `0.0` si precios idénticos. |
| `getScore()` | **`quote.performanceScore`** | **`quote.performanceScore`** | **Mapeo explícito**: el getter SQL abreviado `getScore()` se proyecta al nombre canónico `performanceScore`. |
| `getMultiplier()` | `quote.multiplier` | `quote.multiplier` | Idéntico (Double, no nulo si `quote != null`). |
| `getOrigin()` | **`quote.calculationOrigin`** | **`quote.calculationOrigin`** | **Mapeo explícito**: el getter SQL abreviado `getOrigin()` se proyecta al nombre canónico `calculationOrigin`. |
| `getEffectiveDate()` | `quote.effectiveDate` | `quote.effectiveDate` | Fecha ISO (`YYYY-MM-DD`). |
| `getCalculatedAt()` | `quote.calculatedAt` | `quote.calculatedAt` | Timestamp UTC ISO-8601. |

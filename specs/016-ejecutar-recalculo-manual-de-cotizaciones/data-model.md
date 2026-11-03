# Data Model: Cotización de tokens de jugadores con historial trazable

**Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`
**Spec**: [`spec.md`](spec.md) · **Research**: [`research.md`](research.md)

El historial de cotizaciones es una **única tabla** (FR-022). No existe tabla de cotizaciones
actuales: la cotización vigente de un jugador es su fila de fecha de vigencia máxima.

---

## 1. `player_quotes` — Cotización (historial)

Una fila es un punto de la serie de precios de un jugador.

| Columna | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `id` | `BIGINT` PK (`player_quotes_id_seq`) | no | Identificador interno |
| `player_id` | `BIGINT` → `players(id)` | no | Jugador cotizable (FR-001) |
| `effective_date` | `DATE` | no | Fecha de vigencia: fecha de la valuación utilizada o fecha de cotización en filas arrastradas (FR-024) |
| `price` | `BIGINT` | no | Precio **resultante** de un token, en centésimas de crédito; siempre > 0 (FR-013, SC-001) |
| `previous_price` | `BIGINT` | sí | Precio anterior en centésimas; nulo **solo** en filas `INITIAL` (FR-023, FR-028) |
| `multiplier` | `NUMERIC(18,6)` | no | Multiplicador aplicado, redondeado a seis decimales (FR-016) |
| `score` | `NUMERIC(18,6)` | sí | Score utilizado; nulo en `INITIAL` y en `CARRIED_FORWARD` (FR-008, FR-021) |
| `strategy_name` | `VARCHAR(64)` | sí | Estrategia que produjo el precio; nula en `INITIAL` y en una fila arrastrada heredada de la fila inicial (FR-019, FR-020) |
| `strategy_version` | `VARCHAR(16)` | sí | Versión de la estrategia (FR-019) |
| `source_valuation_date` | `DATE` | sí | Fecha de la valuación de origen; nula en `INITIAL` y en `CARRIED_FORWARD` (FR-024, FR-041) |
| `origin` | `VARCHAR(24)` | no | `INITIAL`, `RECOMPUTED` o `CARRIED_FORWARD` (FR-021) |
| `calculated_at` | `TIMESTAMPTZ` | no | Instante de cálculo, distinto de la fecha de vigencia (FR-025) |

### Restricciones

| Nombre | Definición |
| --- | --- |
| `uq_player_quote` | `UNIQUE (player_id, effective_date, origin)` — como máximo una cotización por jugador, fecha de vigencia y origen (FR-026). Permite que una fila `RECOMPUTED` y una `CARRIED_FORWARD` del mismo jugador compartan fecha de vigencia; en ese caso prevalece la `RECOMPUTED` |
| `ck_player_quote_price` | `CHECK (price > 0)` — ningún precio nulo, cero o negativo (FR-013) |
| `ck_player_quote_previous_price` | `CHECK ((origin = 'INITIAL' AND previous_price IS NULL AND multiplier = 1 AND score IS NULL) OR (origin <> 'INITIAL' AND previous_price IS NOT NULL AND previous_price > 0))` |
| `ck_player_quote_origin` | `CHECK (origin IN ('INITIAL', 'RECOMPUTED', 'CARRIED_FORWARD'))` (FR-021) |
| `ck_player_quote_initial` | `CHECK ((origin = 'INITIAL' AND strategy_name IS NULL AND strategy_version IS NULL AND source_valuation_date IS NULL) OR origin <> 'INITIAL')` |
| `ck_player_quote_carried` | `CHECK (origin <> 'CARRIED_FORWARD' OR (score IS NULL AND source_valuation_date IS NULL AND multiplier = 1))` — una fila arrastrada no tiene score ni fecha de valuación y su multiplicador es 1 (FR-008, FR-021) |
| `ck_player_quote_recomputed` | `CHECK (origin <> 'RECOMPUTED' OR (score IS NOT NULL AND strategy_name IS NOT NULL AND strategy_version IS NOT NULL AND source_valuation_date IS NOT NULL))` — una fila recalculada tiene score, estrategia, versión y fecha de valuación de origen (FR-019, FR-021) |

### Índices

| Nombre | Definición | Uso |
| --- | --- | --- |
| `ix_player_quotes_player_date` | `(player_id, effective_date DESC, calculated_at DESC)` | Historial, cotización vigente y cotización por fecha (FR-036, FR-037, FR-038) |
| `ix_player_quotes_effective_price` | `(effective_date DESC, price DESC)` | Ranking por precio vigente y detección de mercado inicializado (FR-039, FR-044) |

### Invariantes de dominio

- `price > 0` siempre; el piso mínimo se aplica en el cálculo, no solo en la validación.
- `previous_price` de una fila no inicial es el precio de la cotización vigente del jugador
  inmediatamente antes de escribir esa fila (FR-028).
- **Regla de vigencia**: la cotización vigente de un jugador es su fila de `effective_date` máxima y,
  a igualdad de fecha, la fila `RECOMPUTED` (escrita después que la arrastrada). El orden de lectura
  es `effective_date DESC, calculated_at DESC`, cerrado por `id DESC` como último criterio para que
  el orden sea total y una fila nunca dependa del orden arbitrario de la base (FR-026, FR-037).
- **Fecha de vigencia de referencia**: la de la última fila `INITIAL` o `RECOMPUTED` del jugador.
  Las filas `CARRIED_FORWARD` no la modifican, de modo que una fila arrastrada nunca bloquea un
  reprecio posterior (FR-027).
- Una fila `RECOMPUTED` con `score` exactamente 0,5 deja el precio sin cambios y **no** es
  `CARRIED_FORWARD` (FR-021).
- Una fila `CARRIED_FORWARD` tiene `price` igual al precio de la cotización vigente que repite, y
  su estrategia (si existe) es la de esa cotización.
- Los parámetros de la ejecución —factor de ajuste, tope de variación y piso mínimo— no se duplican
  en la fila: se consultan en `quote_executions`, que es su única fuente de verdad (FR-012,
  FR-029, FR-061).

---

## 2. `quote_executions` — Ejecución de cotización

Registro append-only de cada corrida del recálculo (FR-061, FR-064).

| Columna | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `id` | `BIGINT` PK (`quote_executions_id_seq`) | no | Identificador interno |
| `quote_date` | `DATE` | no | Fecha de cotización: la fecha UTC del día en que corre (FR-060) |
| `strategy_name` | `VARCHAR(64)` | no | Estrategia de la ejecución (FR-017, FR-061) |
| `strategy_version` | `VARCHAR(16)` | no | Versión de la estrategia (FR-019) |
| `adjustment_factor` | `NUMERIC(18,6)` | no | Factor de ajuste `k` aplicado (FR-011, FR-061) |
| `variation_cap` | `NUMERIC(18,6)` | no | Tope de variación por período aplicado (FR-012, FR-061) |
| `min_price_floor` | `BIGINT` | no | Piso mínimo de precio aplicado, en centésimas (FR-013, FR-061) |
| `priced_players` | `INTEGER` | no | Población cotizable evaluada en la ejecución, incluidos los jugadores no repreciados (FR-061) |
| `recomputed_rows` | `INTEGER` | no | Filas `RECOMPUTED` escritas (FR-061) |
| `carried_forward_rows` | `INTEGER` | no | Filas `CARRIED_FORWARD` escritas (FR-061) |
| `not_repriced_players` | `INTEGER` | no | Jugadores no repreciados por no tener valuación posterior a su fecha de vigencia de referencia; es un subconjunto de `priced_players` (FR-027, FR-061) |
| `failed_players` | `INTEGER` | no | Jugadores que fallaron de forma individual (FR-062) |
| `started_at` | `TIMESTAMPTZ` | no | Inicio de la corrida |
| `finished_at` | `TIMESTAMPTZ` | no | Fin de la corrida |

### Restricciones

| Nombre | Definición |
| --- | --- |
| `ck_quote_execution_factor` | `CHECK (adjustment_factor BETWEEN 0.10 AND 1.00)` (FR-011) |
| `ck_quote_execution_cap` | `CHECK (variation_cap > 0 AND variation_cap <= 1)` — fracción del precio vigente en el rango (0; 1], es decir de 0 % a 100 % (FR-012) |
| `ck_quote_execution_floor` | `CHECK (min_price_floor > 0)` (FR-013) |
| `ck_quote_execution_counters` | `CHECK (priced_players >= 0 AND recomputed_rows >= 0 AND carried_forward_rows >= 0 AND not_repriced_players >= 0 AND failed_players >= 0)` |
| `uq_quote_executions_date` | `UNIQUE (quote_date)` — como máximo una ejecución de cotización por fecha de cotización (FR-060, FR-066). Una segunda corrida del mismo día se rechaza antes de escribir |

Una ejecución rechazada (estrategia inválida, mercado no inicializado, parámetro fuera de rango o
ejecución ya existente para la fecha) no persiste fila en `quote_executions`.

### Índices

| Nombre | Definición | Uso |
| --- | --- | --- |
| `ix_quote_executions_date` | `(quote_date DESC, started_at DESC)` | Consulta del resumen por fecha de cotización (FR-061) |

Hay unicidad sobre `quote_date`: cada fecha de cotización admite como máximo una ejecución
([research.md §7](research.md)), de modo que el resumen de una fecha es único e inequívoco.

---

## 3. Entidades y repositorios

| Elemento | Nombre | Ubicación | Responsabilidad |
| --- | --- | --- | --- |
| Model | `PlayerQuote` | `footballmarket.models` | Fila del historial, con invariantes de origen, precio y trazabilidad |
| Model | `QuoteOrigin` | `footballmarket.models.enums` | `INITIAL`, `RECOMPUTED`, `CARRIED_FORWARD` |
| Model | `QuoteExecution` | `footballmarket.models` | Corrida persistida con sus parámetros y contadores |
| Model | `ValuationStrategy` (existente, SPEC-013) | `footballmarket.models.enums` | Catálogo cerrado de estrategias de cotización y su versión vigente. No se crea un enum nuevo: reutilizar el de SPEC-013 (FR-018, FR-055) |
| Repository | `PlayerQuoteRepository` | `footballmarket.repositories` | Última fila por jugador, historial, fila por fecha y ranking |
| Repository | `QuoteExecutionRepository` | `footballmarket.repositories` | Persistencia de corridas y consulta por fecha de cotización |

Consultas de lectura requeridas:

- Cotización vigente: última fila por `player_id` ordenada por `effective_date DESC,
  calculated_at DESC, id DESC`; a igualdad de fecha prevalece la fila `RECOMPUTED` (FR-037).
- Historial: todas las filas de un jugador ordenadas por `effective_date DESC, calculated_at DESC,
  id DESC`.
- Cotización a una fecha: última fila con `effective_date <= fecha`, con el mismo desempate.
- Ranking: última fila por jugador reordenada por `price DESC`; el desempate total garantiza una
  sola entrada por jugador (FR-039).
- Mercado inicializado: existencia de al menos una fila con `origin = 'INITIAL'`.
- Población cotizable: `player_id` distintos de `player_statistics` (SPEC-012) cuyo último
  registro tenga al menos una métrica informada.

---

## 4. Structura de la estimación

`player_quote` reproduce el cálculo:

```text
precioResultado = max(pisoMínimo, redondeoHalfUp(precioAnterior × multiplicador))
multiplicador   = acotar(1 + k × (score − 0,5), 1 − tope, 1 + tope)
```

`precioAnterior` es el precio de la cotización vigente del jugador inmediatamente antes de escribir
la fila (FR-028). `tope` es la fracción en el rango (0; 1] (FR-012).

El score es el valor final persistido por SPEC-013: `final_score` (`POSITIONAL_PERFORMANCE`) o
`final_grl` (`GENERAL_ATTRIBUTES`). No se recalcula ninguna valuación (FR-010).

---

## 5. Migraciones

Una única migración nueva, generada con `./gradlew createMigration -PmigrationDescription=...`,
que crea las dos tablas, sus secuencias, restricciones e índices. No se modifica ninguna migración
previa (arquitectura del backend §4). Conforme a la Constitución, los datos de desarrollo son
descartables y no se diseñan mecanismos de compatibilidad retroactiva.

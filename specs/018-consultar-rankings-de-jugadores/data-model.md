# Data Model: Consultar ranking de jugadores por score

## Modelo vigente del delta — 2026-11-12

Este bloque sustituye las reglas históricas incompatibles. El diseño es de solo lectura y no modifica
el esquema persistido.

### Fuentes y selección

| Fuente | Uso vigente |
| --- | --- |
| `quote_executions` | Cabecera de la última ejecución finalizada por `started_at DESC, id DESC`; ignorar `finished_at IS NULL`. |
| `player_quotes` | Elegibilidad: existe cotización vigente del jugador. |
| `player_statistics` | Elegibilidad: al menos una fila con `present_metrics > 0` en la fecha de extracción más reciente del jugador, según SPEC-002/SPEC-016. |
| `player_positional_valuations` | Estrategia `POSITIONAL_PERFORMANCE`; valor `final_score`. |
| `player_attribute_valuations` | Estrategia `GENERAL_ATTRIBUTES`; valor `final_grl`. |
| Catálogo de `players`, equipos y ligas | Identidad y valores globales de filtros de jugador. |

La ejecución finalizada determina solo `strategy` y `quoteDate`; abiertos se ignoran. La valuación más
reciente por jugador en la tabla de esa estrategia aporta el score y `valuationDate`. El universo
requiere cotización y métrica elegibles, además de valuación. No se filtra la valuación por
`quoteDate`. El esquema no cambia: no se crean tablas, columnas, índices o migraciones.

### Modelo derivado

**Consulta**: `page` (entero ≥0, default 0), `size` (1–100, default 20), `name`, `leagueId` positivo y
`position`. Para `name` y `position`, recortar espacios en los extremos y tratar el valor vacío como
filtro omitido; ambas comparaciones ignoran mayúsculas. En `name`, `%` y `_` son literales. Los filtros
se combinan con AND.

**Página del ranking**: `strategy` y `quoteDate` nullable; `content` no nulo; `page`, `size`,
`totalElements` filtrado, `totalPages` y `filterOptions` globales. `filterOptions` incluye ligas
`{id,name}` y posiciones únicas del universo elegible.

**Entrada**: posición `row_number()` global antes de filtrar; `playerId`, score final persistido y
`valuationDate`; `player` con `name`, `position`, `imageUrl`, `team{id,name,crestUrl}` y
`league{id,name}`. `player` y `name` son obligatorios; datos de catálogo ausentes se representan como
nulos donde el contrato lo permite.

El ranking se ordena por score DESC y `playerId` ASC. `totalPages` es el techo de total filtrado entre
size. Una página válida fuera de rango o filtro sin coincidencias mantiene totales reales y contenido
vacío. La misma ruta con filtros omitidos, `page=0&size=3` entrega podio y total global mediante
`totalElements`; no existe snapshot entre requests.

### Consulta de datos

Usar consultas SQL nativas y proyecciones en los repositorios existentes de valuación para resolver
selección de última valuación, población elegible, `row_number()` global, filtros, página, conteo y
opciones sin materializar el ranking entero. Mantener consultas distintas para las dos estrategias.
La especificación de los filtros y su normalización es `research.md` §R2; la transacción es
`readOnly`/`REPEATABLE_READ` por respuesta.

---

## Antecedente histórico

El diseño siguiente describe el contrato anterior al delta de 2026-11-12 y queda subordinado al
modelo vigente al inicio del documento. Se conserva como trazabilidad, no como modelo implementable.

**Feature**: `018-consultar-rankings-de-jugadores`
**Spec**: [`spec.md`](spec.md) · **Research**: [`research.md`](research.md)

La feature es de **solo lectura** y no modifica el esquema. El modelo de datos describe las fuentes
persistidas que se leen, las reglas de selección y los objetos derivados que se devuelven. No se crean
tablas, columnas, secuencias ni migraciones.

---

## 1. Fuentes persistidas existentes

| Tabla | Origen | Uso en esta feature |
| --- | --- | --- |
| `quote_executions` | SPEC-016 | Encabezado: estrategia y fecha de cotización de la última ejecución persistida |
| `player_quotes` | SPEC-016 | Elegibilidad: existencia de cotización vigente del jugador |
| `player_positional_valuations` | SPEC-013 | Score (`final_score`) y fecha de valuación cuando la estrategia es `POSITIONAL_PERFORMANCE` |
| `player_attribute_valuations` | SPEC-013 | Score (`final_grl`) y fecha de valuación cuando la estrategia es `GENERAL_ATTRIBUTES` |

### 1.1. `quote_executions` — Ejecución de cotización

Registro append-only con una única ejecución por fecha de cotización (`uq_quote_executions_date`).

| Columna | Tipo | Uso |
| --- | --- | --- |
| `quote_date` | `DATE` | Criterio de última ejecución (`MAX(quote_date)`) y campo `quoteDate` de la respuesta |
| `strategy_name` | `VARCHAR(64)` | Estrategia que encabeza la respuesta y determina la tabla de valuaciones |
| `strategy_version` | `VARCHAR(16)` | **No se usa**: ni en la respuesta ni en la selección (FR-009) |
| Demás columnas | — | No se leen en esta feature |

### 1.2. `player_quotes` — Cotización

Solo se comprueba la existencia de al menos una fila del jugador. La fila vigente es la de
`effective_date` máxima y, a igualdad, la `RECOMPUTED`; a los efectos de esta feature basta con saber
que el jugador tiene una cotización vigente, porque el score no proviene de esta tabla (FR-003).

### 1.3. `player_positional_valuations` — Valuación posicional

| Columna | Tipo | Uso |
| --- | --- | --- |
| `player_id` | `BIGINT` | Jugador valuado |
| `valuation_date` | `DATE` | Primera clave de selección (más reciente) y campo `valuationDate` |
| `calculated_at` | `TIMESTAMPTZ` | Trazabilidad del cálculo; no participa en la selección |
| `id` | `BIGINT` | Identificador persistente; no participa en la selección |
| `final_score` | `NUMERIC(18,6)` | Score expuesto, sin transformación (FR-004, FR-012) |
| `strategy_name`, `strategy_version` | `VARCHAR` | Constantes por restricción (`POSITIONAL_PERFORMANCE`, `v1`); no se filtran ni se exponen |

### 1.4. `player_attribute_valuations` — Valuación por atributos

Misma estructura de selección, con `final_grl` como score y las constantes
(`GENERAL_ATTRIBUTES`, `v1`). Se aplican las mismas reglas de desempate.

---

## 2. Reglas de derivación

### 2.1. Última ejecución

`quoteDate` y `strategy` son la `quote_date` y la `strategy_name` de la fila de `quote_executions` con
mayor `quote_date`. Si no existe ninguna, ambos son nulos y la lista de entradas está vacía (FR-010).
La unicidad por fecha hace innecesario cualquier desempate adicional. La ejecución determina
únicamente la estrategia y el encabezado; no identifica la valuación usada para calcular la
cotización.

### 2.2. Población elegible

Un jugador entra al ranking si y solo si:

1. existe al menos una fila en `player_quotes` con su `player_id`, y
2. existe al menos una fila de valuación de la estrategia de la última ejecución.

En otro caso queda excluido, aunque tenga valuaciones (caso «Jugador sin cotización vigente») o
aunque tenga cotización (caso «Jugador sin valuación de la estrategia»).

### 2.3. Valuación elegida por jugador

Entre las valuaciones del jugador en la tabla de la estrategia vigente, se elige la de mayor
`valuation_date`. La restricción única por jugador, fecha, estrategia y versión garantiza que no
existan dos filas candidatas para la misma fecha; una reevaluación de esa fecha reemplaza la fila
anterior.

El score es `final_score` para `POSITIONAL_PERFORMANCE` y `final_grl` para `GENERAL_ATTRIBUTES`. El
score bruto no participa. La selección no se limita por `quoteDate`: la valuación puede ser posterior
o distinta de la utilizada para calcular la última cotización. `valuationDate` identifica la fecha
efectiva del score mostrado.

### 2.4. Orden y posiciones

Las entradas se ordenan por `score DESC, playerId ASC`. La posición es el índice de la entrada en ese
orden, desde 1, sin compartirse ni saltarse valores. Dos consultas sobre los mismos datos producen
idéntico resultado.

---

## 3. Objetos derivados (Model y DTO)

### 3.1. `ScoreRanking` (Model)

| Campo | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `strategy` | `ValuationStrategy` | sí | Estrategia de la última ejecución; nula si no hay ejecuciones |
| `quoteDate` | `LocalDate` | sí | Fecha de cotización de la última ejecución; nula si no hay ejecuciones |
| `entries` | `List<ScoreRankingEntry>` | no | Entradas en el orden del ranking; vacía si no hay entradas |

`ScoreRanking` rechaza `entries == null` como estado interno inválido. El Service construye
explícitamente una lista vacía cuando no existe ejecución o no hay valuaciones; el Mapper copia la
lista válida y no normaliza `null`.

### 3.2. `ScoreRankingEntry` (Model)

| Campo | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `position` | `Integer` | no | Posición consecutiva desde 1, asignada por el Service según el orden recibido |
| `playerId` | `Long` | no | Identificador interno del jugador |
| `score` | `BigDecimal` | no | Resultado final persistido de la estrategia |
| `valuationDate` | `LocalDate` | no | Fecha de la valuación usada |

La posición forma parte del resultado del caso de uso. El Service la asigna como índice + 1 sobre el
orden de Repository y el Mapper la copia al DTO sin calcularla ni reordenar.

### 3.3. DTO de respuesta

El contrato HTTP completo, con nombres de campos, tipos y errores, está en
[`contracts/score-ranking-http.md`](contracts/score-ranking-http.md). El mapeo Model → DTO es
estructural: `strategy` pasa a `String` con el nombre del enum, y la posición se copia desde el Model.

---

## 4. Consultas requeridas

| Elemento | Consulta | Resultado |
| --- | --- | --- |
| `QuoteExecutionRepository` | `findTopByOrderByQuoteDateDesc()` | Última ejecución persistida, o vacío |
| `PositionalValuationRepository` | `findScoreRanking()` | Última valuación posicional por jugador con cotización, ordenada por `finalScore DESC, player.id ASC` |
| `AttributeValuationRepository` | `findScoreRanking()` | Última valuación por atributos por jugador con cotización, ordenada por `finalGrl DESC, player.id ASC` |

Las consultas de valuaciones se expresan en JPQL con `exists` sobre `PlayerQuote` y un `not exists`
de una valuación con fecha posterior, siguiendo el patrón determinista que ya usa
`PlayerQuoteRepository.findRanking`. No se usan proyecciones: se recuperan los Models persistibles
necesarios (`player.id`, score y `valuationDate`), como hace el ranking por precio.

---

## 5. Invariantes

- La respuesta nunca inventa posiciones ni scores: sin datos, el arreglo está vacío (FR-010,
  FR-011).
- `ScoreRanking.entries` nunca es nulo; la ausencia válida de entradas se representa con una lista
  vacía y no mediante un fallback del Mapper.
- `score` es exactamente el valor persistido; no se redondea, recalcula ni transforma (FR-012).
- Cada jugador aparece como máximo una vez (FR-003, FR-006).
- Las posiciones son consecutivas de 1 a n (FR-007).
- La operación no escribe filas ni altera cotizaciones, precios o valuaciones (FR-013).
- `strategy` y `quoteDate` son ambos nulos o ambos no nulos, porque provienen de la misma fila de
  `quote_executions`.
- La estrategia de la respuesta determina la tabla consultada; no se mezclan valuaciones de
  estrategias distintas.

---

## 6. Migraciones

Ninguna. El esquema y los índices vigentes cubren las lecturas:

- `ix_quote_executions_date` (`quote_date DESC, started_at DESC`) para la última ejecución.
- `ix_positional_player_date` y `ix_attribute_player_date` para acotar por jugador y fecha.
- `ix_positional_final_score` y `ix_attribute_final_grl` para el orden por score.
- La búsqueda de cotización vigente usa los índices existentes de `player_quotes`.

No se modifican `docs/backend/data-model.md` ni migraciones previas, porque no cambia el modelo
persistido.

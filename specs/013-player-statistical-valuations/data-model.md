# Data Model: Valoraciones estadísticas de jugadores

## Principios

- SPEC-012 permanece como fuente única de entradas; las valoraciones no copian sus 60 métricas.
- `valuationDate` es la fecha UTC fijada al inicio de la solicitud.
- `sourceExtractionDate` identifica una única campaña JSON confirmada y su eventual respaldo SCRAPING del mismo día.
- Ambas estrategias se publican o retiran juntas para cada jugador y fecha.
- Todos los decimales persistidos usan escala 6 y `HALF_UP`, después del `clamp` cuando corresponda.

## Entradas existentes

### PlayerStatistics

Se reutiliza la entidad de SPEC-012 identificada por `(player, extractionDate, source)`. La fila JSON es obligatoria; la fila SCRAPING es respaldo opcional. `seasonKey`, el equipo y su liga resuelven la población. `contextStatus = RESOLVED` es obligatorio para evaluar.

### MetricCatalog

Catálogo inmutable de 60 claves. Determina si una métrica admite JSON y SCRAPING. No se agregan claves ni se persisten definiciones nuevas.

## GeneralPosition

Enum de dominio:

| Valor | Regla |
| --- | --- |
| `GOALKEEPER` | Posición persistida `Goalkeeper`. |
| `DEFENDER` | Posición persistida `Defender`. |
| `MIDFIELDER` | Posición persistida `Midfielder`. |
| `FORWARD` | Posición persistida `Forward`. |

La posición persistida del jugador es la única fuente de la posición general. El conjunto es cerrado y no admite variantes ni subposiciones: una posición persistida fuera de estos cuatro valores constituye un fallo individual controlado y no crea una quinta categoría ni una nueva causa funcional de exclusión.

## MetricSourceSummary

Enum persistido: `JSON`, `SCRAPING`, `MIXED`. Resume únicamente las entradas efectivamente usadas. Las imputaciones no constituyen otro canal.

## PositionalValuation

Tabla propuesta: `player_positional_valuations`.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id` | Long | Secuencia propia. |
| `player` | Player | FK obligatoria. |
| `valuationDate` | LocalDate | Fecha UTC de inicio. |
| `strategyName` | String | Constante `POSITIONAL_PERFORMANCE`. |
| `strategyVersion` | String | Constante `v1`. |
| `generalPosition` | GeneralPosition | Una de cuatro posiciones. |
| `sourceSummary` | MetricSourceSummary | Procedencia agregada. |
| `sourceExtractionDate` | LocalDate | Campaña confirmada usada. |
| `positiveScore` | Decimal(?,6) | Obligatorio, 0..1. |
| `performancePenalty` | Decimal(?,6)? | Nulo solo para arquero. |
| `performanceAdjustment` | Decimal(?,6)? | Nulo solo para arquero. |
| `disciplinePenalty` | Decimal(?,6) | Obligatorio, 0..1. |
| `disciplineAdjustment` | Decimal(?,6) | Obligatorio. |
| `rawScore` | Decimal(?,6) | Componente previo a confianza. |
| `confidence` | Decimal(?,6) | 0..1. |
| `finalScore` | Decimal(?,6) | 0..1 después de clamp. |
| `imputedMetrics` | String[] | Claves derivadas imputadas, únicas y ordenadas. |
| `calculatedAt` | Instant | Momento de cálculo. |

Restricciones e índices:

- UNIQUE `(player_id, valuation_date, strategy_name, strategy_version)`.
- CHECK de nombre/versión constantes, posición válida, fuente válida y rangos 0..1 aplicables.
- Índices `(valuation_date DESC)`, `(final_score DESC)` y `(player_id, valuation_date DESC)`.

## AttributeValuation

Tabla propuesta: `player_attribute_valuations`.

| Campo | Tipo | Regla |
| --- | --- | --- |
| `id`, `player`, `valuationDate` | — | Mismas reglas de identidad que la valoración posicional. |
| `strategyName` | String | Constante `GENERAL_ATTRIBUTES`. |
| `strategyVersion` | String | Constante `v1`. |
| `generalPosition`, `sourceSummary`, `sourceExtractionDate` | — | Mismas reglas comunes. |
| `shooting`, `passing`, `dribbling`, `defending`, `physical` | Decimal(?,6)? | Obligatorios para campo y nulos para arquero. Corresponden a SHO/PAS/DRI/DEF/PHY. |
| `saving`, `distribution`, `activity` | Decimal(?,6)? | Obligatorios para arquero y nulos para campo. Corresponden a SAV/DIS/ACT. |
| `baseGrl` | Decimal(?,6) | 0..1. |
| `disciplineAdjustment` | Decimal(?,6) | Obligatorio. |
| `rawGrl` | Decimal(?,6) | Previo a confianza. |
| `confidence` | Decimal(?,6) | 0..1. |
| `finalGrl` | Decimal(?,6) | 0..1 después de clamp. |
| `imputedMetrics` | String[] | Claves derivadas imputadas, únicas y ordenadas. |
| `calculatedAt` | Instant | Momento de cálculo. |

Restricciones e índices:

- UNIQUE `(player_id, valuation_date, strategy_name, strategy_version)`.
- CHECK de nulabilidad exclusiva campo/arquero y rangos 0..1 de atributos, confianza y GRL finales.
- Índices `(valuation_date DESC)`, `(final_grl DESC)` y `(player_id, valuation_date DESC)`.

## EvaluationSummary

Modelo de retorno no persistido:

| Campo | Tipo | Regla |
| --- | --- | --- |
| `valuationDate` | LocalDate | Fecha UTC fijada al inicio. |
| `evaluatedPlayers` | long | Jugadores con las dos estrategias publicadas. |
| `failedPlayers` | long | Fallos inesperados, no exclusiones. |
| `excludedPlayers` | Map<ExclusionReason,long> | Incluye todos los motivos contractuales, aun con cero. Un jugador excluido se cuenta en un único motivo, conforme al orden de severidad de FR-065. |
| `imputedMetricCounts` | Map<String,long> | Conteo por clave derivada imputada. |

`ExclusionReason` y su orden de evaluación: `UNRESOLVED_LEAGUE_CONTEXT`, `MISSING_OR_UNCONFIRMED_SNAPSHOT`, `MISSING_REQUIRED_METRICS`, `NO_APPEARANCES`. Un jugador se contabiliza solo en el primer motivo que satisface.

## Relaciones

```text
Player 1 ── N PlayerStatistics
Player 1 ── N PositionalValuation
Player 1 ── N AttributeValuation

PlayerStatistics(JSON, fecha)  ─┐
PlayerStatistics(SCRAPING, fecha)├─ entrada reconstruible de ambas valoraciones
MetricCatalog                    ─┘
```

No existe FK desde una valoración a una fila concreta de estadísticas porque el origen lógico puede combinar dos filas; `(player, sourceExtractionDate, source)` y el catálogo reconstruyen el conjunto.

## Flujo y transiciones

```text
sin snapshot confirmado → rechazo global, sin escrituras

candidato → EXCLUDED
          → FAILED (ambas filas ausentes)
          → EVALUATED (ambas filas publicadas)

EVALUATED en repetición → reemplazado por nuevo par
EVALUATED que deja de ser válido → ambas filas retiradas
```

La evaluación no persiste un estado de run. El resumen se construye durante el recorrido. Un fallo individual se registra mediante logging estructurado, sin exponer causa en HTTP.

## Reglas de cálculo compartidas

- Campaña vigente: última fecha JSON confirmada según FR-004 (items `COMPLETED` y filas JSON de la fecha que cubren el catálogo elegible).
- Métricas requeridas: conjunto cerrado por posición de FR-066 (26 o 27 métricas base para campo, 13 para arquero); su ausencia tras el respaldo produce `MISSING_REQUIRED_METRICS`.
- Orden de etapas: respaldo → derivación → imputación → normalización → ponderación y ajustes → confianza → clamp → redondeo (FR-064).
- Fallback: JSON no nulo > SCRAPING no nulo para claves compartidas > ausencia.
- Grupo: posición general + liga local + temporada.
- Percentil: `(menores + 0.5 × iguales) / total`.
- Confianza: `min(minutesPlayed / minimumMinutes, 1)`.
- Publicación: atributos/componentes y resultados redondeados a seis decimales; entradas fuente permanecen inalteradas.

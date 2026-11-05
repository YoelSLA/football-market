# Contrato HTTP: ejecuciones de recálculo con origen y resultado

**Feature**: `023-scheduler-quote-recalculation`
**Spec**: [`spec.md`](../spec.md) · **Data model**: [`../data-model.md`](../data-model.md)

Este documento fija el **delta** que esta feature introduce en el contrato HTTP del recálculo de
cotizaciones. El contrato funcional y normativo completo de la API de cotizaciones sigue siendo
[`specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`](../../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md),
que **debe actualizarse de forma aditiva en el mismo cambio** (§IV de la Constitución). Ante
discrepancia entre ambos documentos, prevalece el de SPEC-016 actualizado.

## 1. Rutas

**No se crea, modifica ni retira ninguna ruta.** No hay endpoint de disparo manual del proceso
automático ni consulta histórica nueva (FR-005).

| Método y ruta | Cambio en esta feature |
| --- | --- |
| `POST /api/quotes/recalculate` | Sin cambios en el request. El response gana tres campos aditivos. `origin` siempre `MANUAL` |
| `GET /api/quotes/executions?date=` | Sin cambios en la query. Cada elemento gana los mismos tres campos aditivos |
| `POST /api/quotes/initialize` | Sin cambios |
| `GET /api/players/{id}/quotes`, `.../quotes/current`, `/api/players/ranking` | Sin cambios |

Todas las rutas conservan la exigencia de JWT válido y el formato de error `ErrorResponseDTO`.

## 2. Objeto de ejecución ampliado

Se añaden tres campos al objeto `QuoteExecutionResponse`:

| Campo | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `origin` | `string` | no | Origen de la corrida: `MANUAL` o `AUTOMATIC` (FR-004) |
| `result` | `string` | no | Desenlace: `SUCCESS` cuando `failedPlayers = 0`; `FAILED` en caso contrario (FR-004) |
| `failureReason` | `string` | sí | Nulo con `SUCCESS`; `INDIVIDUAL_PLAYER_FAILURES` con `FAILED`. Catálogo cerrado, apto para exposición (FR-010) |

Los campos existentes conservan nombre, tipo y significado. El orden de serialización no es parte del
contrato.

### Ejemplo — `GET /api/quotes/executions?date=2026-11-02`

```json
{
  "executions": [
    {
      "quoteDate": "2026-11-02",
      "origin": "AUTOMATIC",
      "result": "SUCCESS",
      "failureReason": null,
      "strategy": "POSITIONAL_PERFORMANCE",
      "strategyVersion": "v1",
      "adjustmentFactor": 0.10,
      "variationCap": 0.10,
      "minPriceFloor": 1,
      "pricedPlayers": 42,
      "recomputedRows": 30,
      "carriedForwardRows": 12,
      "notRepricedPlayers": 3,
      "failedPlayers": 0,
      "startedAt": "2026-11-02T00:00:00Z",
      "finishedAt": "2026-11-02T00:00:12Z"
    }
  ]
}
```

### Ejemplo — `POST /api/quotes/recalculate` (sin cambios en el request)

```json
{ "strategy": "POSITIONAL_PERFORMANCE" }
```

Response `200`:

```json
{
  "quoteDate": "2026-11-03",
  "origin": "MANUAL",
  "result": "FAILED",
  "failureReason": "INDIVIDUAL_PLAYER_FAILURES",
  "strategy": "POSITIONAL_PERFORMANCE",
  "strategyVersion": "v1",
  "adjustmentFactor": 0.10,
  "variationCap": 0.10,
  "minPriceFloor": 1,
  "pricedPlayers": 42,
  "recomputedRows": 29,
  "carriedForwardRows": 12,
  "notRepricedPlayers": 3,
  "failedPlayers": 1,
  "startedAt": "2026-11-03T10:00:00Z",
  "finishedAt": "2026-11-03T10:00:09Z"
}
```

## 3. Comportamiento en la consulta por fecha

- Como máximo hay una ejecución por fecha UTC, de modo que la lista contiene cero o un elemento.
- Una fecha sin ejecución devuelve `{"executions": []}`. Si el disparo se omite porque ya existe
  una ejecución para esa fecha, se devuelve la ejecución existente. El motivo del rechazo o fallo
  no aparece en la respuesta: está en el registro operativo; la persistencia depende del caso de uso existente.
- El origen no es un filtro: no se añade query parameter ni se cambia el índice.

## 4. Errores

Sin códigos nuevos ni cambios de código HTTP. Se mantienen los de SPEC-016:

| Código | HTTP | Causa |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | Body o query ausentes o malformados |
| `UNKNOWN_QUOTE_STRATEGY` | 400 | Estrategia fuera del catálogo |
| `INVALID_ADJUSTMENT_FACTOR`, `INVALID_VARIATION_CAP`, `INVALID_MIN_PRICE_FLOOR` | 400 | Parámetros fuera de rango |
| `MARKET_NOT_INITIALIZED` | 409 | Mercado no inicializado |
| `EXECUTION_ALREADY_EXISTS` | 409 | Ya existe una ejecución para la fecha o hay una corrida en curso |
| `401` | — | JWT ausente o inválido |
| `INTERNAL_ERROR` | 500 | Fallo general |

El orden de precedencia de SPEC-016 se conserva. La vía automática **no** usa este contrato: no
responde HTTP y traduce los rechazos a eventos operativos.

## 5. Compatibilidad

- Los campos nuevos son aditivos: ningún campo existente cambia de nombre, tipo, obligatoriedad ni
  significado.
- Un consumidor que ignore los campos nuevos sigue funcionando; el frontend de esta entrega no los
  consume. No se requieren cambios de frontend (FR-005, fuera de alcance).
- La obligación de coherencia incluye, en el mismo cambio: las anotaciones OpenAPI de
  `QuoteExecutionResponseDTO` y la descripción de `GET /api/quotes/executions` en `QuoteController`,
  el contrato de SPEC-016 y los tests funcionales de Controller afectados.

## 6. Trazabilidad

| Requisito | Cobertura en este contrato |
| --- | --- |
| FR-004 | Campos `origin`, `result` y `failureReason` del objeto de ejecución |
| FR-005 | Mismo endpoint y misma query; sin consulta histórica nueva |
| FR-010 | `failureReason` limitado a códigos cerrados, sin trazas ni identificadores internos |
| FR-006, FR-012, FR-013 | Consulta de la ejecución existente, si la hay; los errores se registran operativamente |

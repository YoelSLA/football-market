# Contrato HTTP: Consulta individual de estadísticas

**Endpoint**: `GET /api/players/{id}/statistics`
**Estado**: Contrato vigente del sistema.
**Referencias normativas**: [spec.md](../spec.md), [plan.md](../plan.md), [data-model.md](../data-model.md) y [metric-glossary.md](metric-glossary.md).

**Alineación 2026-11-11**: identidad y estadísticas aplican la elegibilidad de [SPEC-002](../../002-player-catalog/spec.md): al menos una métrica informada en alguna fila de la fecha de extracción más reciente, además de la visibilidad previa. Un jugador no elegible recibe 404 en ambas lecturas. Si el jugador es elegible pero no hay snapshot confirmado para la temporada o contexto consultados, la respuesta estadística sigue siendo 200 `NO_DATA`, con 60 ausencias. Esto no cambia la selección del snapshot ni acredita implementación.

## Solicitud

## 1. Solicitud

```http
GET /api/players/7821/statistics?seasonKey=2025-2026
Authorization: Bearer <JWT>
```

### Parámetros

| Parámetro | Ubicación | Tipo | Reglas |
| --- | --- | --- | --- |
| `id` | path | integer (`Long`) | Identificador interno del jugador; requerido, número entero positivo (`id > 0`). Inválido o menor a 1 produce HTTP 400. |
| `seasonKey` | query | string | Clave canónica de la temporada; opcional, longitud máxima 255 caracteres. Comparación exacta con el valor persistido. |

**Comportamiento de selección de temporada**:
- **Sin `seasonKey`**: Selecciona la última extracción confirmada disponible del jugador y devuelve su contexto real.
- **Con `seasonKey`**: Filtra por la temporada solicitada antes de seleccionar la fecha más reciente. Si el jugador no registra extracciones en esa temporada, responde HTTP 200 con estado `NO_DATA` y métricas en `null`, nunca HTTP 404.

---

## 2. Respuesta 200 — Éxito

```json
{
  "playerId": 7821,
  "requestedSeasonKey": "2025-2026",
  "status": "PARTIAL",
  "seasonKey": "2025-2026",
  "seasonLabel": "Temporada 2025/2026",
  "extractionDate": "2026-10-18",
  "competitionKey": "12",
  "competitionName": "Primera Division",
  "metrics": [
    {
      "key": "minutes_played_total",
      "unit": "SEASON_TOTAL",
      "value": 1890,
      "source": "JSON"
    },
    {
      "key": "appearances_total",
      "unit": "SEASON_TOTAL",
      "value": 21,
      "source": "JSON"
    },
    {
      "key": "whoscored_rating_average",
      "unit": "AVERAGE_RATING",
      "value": 6.84,
      "source": "JSON"
    },
    {
      "key": "pass_completion_percentage",
      "unit": "PERCENTAGE",
      "value": 78.5,
      "source": "SCRAPING"
    },
    {
      "key": "goals_total",
      "unit": "SEASON_TOTAL",
      "value": 0,
      "source": "JSON"
    },
    {
      "key": "cross_assists_per_match",
      "unit": "PER_MATCH",
      "value": null,
      "source": null
    }
  ]
}
```

### Semántica de campos principales

| Campo | Tipo | Significado |
| --- | --- | --- |
| `playerId` | integer (`Long`) | Identificador del jugador consultado. |
| `requestedSeasonKey` | string o `null` | Parámetro explícito de la solicitud o `null` si no se especificó. |
| `status` | string | `AVAILABLE` (60 valores presentes), `PARTIAL` (1 a 59 valores) o `NO_DATA` (0 valores). |
| `seasonKey` | string o `null` | Clave canónica de la temporada resuelta; `null` si no hay snapshot. |
| `seasonLabel` | string o `null` | Etiqueta descriptiva persistida; `null` si no hay snapshot. |
| `extractionDate` | string ISO date o `null` | Fecha efectiva de la extracción confirmada; `null` si no hay snapshot. |
| `competitionKey` | string o `null` | Identificador persistido de la competición en el proveedor. |
| `competitionName` | string o `null` | Nombre legible resuelto por correspondencia local; `null` («Competición no disponible») si no se puede determinar fehacientemente. |
| `metrics` | array | Lista de exactamente 60 elementos únicos en orden canónico según [metric-glossary.md](metric-glossary.md). |

### Elementos de `metrics`

Cada elemento del array contiene:
- `key`: clave canónica WhoScored (string).
- `unit`: unidad técnica (`SEASON_TOTAL`, `PER_MATCH`, `PERCENTAGE` o `AVERAGE_RATING`).
- `value`: valor numérico (`BigDecimal`) o `null` si la métrica no estuvo presente en la extracción. El número `0` se conserva íntegramente como dato válido.
- `source`: origen del valor (`JSON`, `SCRAPING` o `null` si el valor es nulo).

---

## 3. Reglas de selección y composición del snapshot

1. Comprobar elegibilidad igual que identidad: jugador activo, equipo y liga visibles y métricas según la alineación anterior; inexistente o no elegible → mismo 404 PLAYER_NOT_FOUND, sin exponer estadísticas.
2. Solo filas con contexto RESOLVED e item referenciado COMPLETED, coherentes en jugador/fecha/canal. No exigir confirmación poblacional ni run global COMPLETED. Ignorar filas huérfanas o de contexto sin resolver.
3. Elegir fecha mayor del jugador y temporada pedida; si no se pide, última fecha del jugador. Dentro de fecha, JSON ancla antes de scraping; luego observedAt e ID mayores como desempate determinista. El ancla determina temporada y competición; no es una elección de la temporada más numerosa ni del equipo actual.
4. Completar desde scraping solo con jugador, temporada, fecha y competición exactamente iguales al ancla JSON. Cada valor JSON no nulo se conserva, incluido cero. Clave ausente en ambos → value/source null. Las tres claves exclusivas scraping también requieren coincidencia de contexto. Sin JSON en esa fecha usar scraping confirmado, nunca JSON de una fecha anterior.
5. Emitir 60 entradas incluso sin snapshot. Sin candidato: contexto null, valores/source null, status NO_DATA. Fallos de consulta no se convierten en esta respuesta.

---

## 4. Errores y seguridad

La política de FR-046 se aplica a ambas lecturas: ID decimal positivo dentro de Long; formato inválido/cero/negativo/fuera de rango → 400, válido inexistente/no elegible → 404; jugador elegible sin snapshot confirmado para la consulta → 200 NO_DATA. El endpoint de identidad existente no tiene validación positiva explícita: requiere alineación futura acotada para cero/negativos, conservando DTO y rutas. Este documento no afirma que ese cambio esté implementado.

### Retención de respuesta compatible en frontend

Si una actualización periódica o manual falla por desconexión:
- Si existe una lectura previa exitosa del mismo `playerId`, se conserva visible en pantalla indicando: *«Mostrando la última consulta disponible; no se pudo actualizar»* y se ofrece botón de reintento.
- Una nueva respuesta exitosa (incluso si es `NO_DATA`) reemplaza completamente la lectura anterior.
- Navegar a otro jugador reinicia el estado y no retiene datos del jugador previo.

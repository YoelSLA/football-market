# Contrato HTTP: Consulta individual de estadísticas

**Endpoint**: `GET /api/players/{id}/statistics`
**Estado**: Contrato vigente del sistema.
**Referencias normativas**: [spec.md](../spec.md), [plan.md](../plan.md), [data-model.md](../data-model.md) y [metric-glossary.md](metric-glossary.md).

---

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

1. **Elegibilidad y visibilidad**: La consulta aplica la misma política que la identidad (`findVisibleById`). Jugadores inexistentes o no visibles responden HTTP 404 (`PLAYER_NOT_FOUND`).
2. **Confirmación estricta**: Solo se seleccionan filas con contexto `RESOLVED` y cuyo ítem de ejecución asociado esté en estado `COMPLETED`. Se ignoran filas huérfanas o con contexto no resuelto.
3. **Anclaje en JSON**: Se busca la fecha más reciente del jugador. Si coinciden extracciones de distintas fuentes para esa fecha, la fuente `JSON` ancla el contexto; se desempata por `observedAt DESC` e `id DESC`.
4. **Respaldo determinista con SCRAPING**: Si la fila JSON ancla contiene métricas nulas, solo se completan desde registros `SCRAPING` cuya fecha, jugador, temporada y competición coincidan exactamente con el ancla. Claves ausentes en ambas fuentes permanecen con `value: null` y `source: null`.
5. **Garantía de 60 entradas**: La respuesta siempre entrega el catálogo completo de 60 métricas. Si el jugador no registra extracciones confirmadas, el estado es `NO_DATA`, los metadatos de contexto son `null` y las 60 métricas contienen `value: null` y `source: null`.

---

## 4. Errores y seguridad

| Código HTTP | Condición | Comportamiento |
| --- | --- | --- |
| **400 Bad Request** | Formato de `id` inválido, no numérico o menor/igual a cero (`id <= 0`). | Devuelve `INVALID_PARAMETER_TYPE`. |
| **401 Unauthorized** | Credencial JWT ausente o expirada. | Redirección a `/login`. |
| **404 Not Found** | Jugador inexistente o inactivo/no visible. | Devuelve `PLAYER_NOT_FOUND` idéntico al de identidad. |
| **500 Server Error** | Fallo interno al leer la base de datos. | La interfaz retiene la identidad ya cargada, comunica el error y ofrece botón de reintento. |

### Retención de respuesta compatible en frontend

Si una actualización periódica o manual falla por desconexión:
- Si existe una lectura previa exitosa del mismo `playerId`, se conserva visible en pantalla indicando: *«Mostrando la última consulta disponible; no se pudo actualizar»* y se ofrece botón de reintento.
- Una nueva respuesta exitosa (incluso si es `NO_DATA`) reemplaza completamente la lectura anterior.
- Navegar a otro jugador reinicia el estado y no retiene datos del jugador previo.

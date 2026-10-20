# Contrato HTTP: evaluación de valoraciones estadísticas

## Reglas comunes

- Base: `/api/valuations`.
- JWT Bearer vigente y usuario persistido, con las mismas reglas que las sincronizaciones de SPEC-012; no se introduce rol adicional.
- Fechas ISO-8601 y timestamps de errores según el contrato común del backend.
- Errores controlados mediante `ErrorResponseDTO`.
- La operación no llama proveedores externos ni expone métricas, causas internas o resultados individuales.

## POST `/evaluate`

Evalúa síncronamente toda la población de la última campaña JSON confirmada con cobertura completa y persiste ambas estrategias.

### Request

```http
POST /api/valuations/evaluate
Authorization: Bearer <token>
```

- Sin body, query parameters ni path variables.
- No acepta fecha, jugador, estrategia, pesos ni umbrales.
- Cualquier body no vacío o query parameter se rechaza con `400 INVALID_REQUEST` antes de invocar el Service.

### Response `200 OK`

```json
{
  "valuationDate": "2026-10-19",
  "evaluatedPlayers": 1840,
  "failedPlayers": 2,
  "excludedPlayers": {
    "UNRESOLVED_LEAGUE_CONTEXT": 10,
    "MISSING_OR_UNCONFIRMED_SNAPSHOT": 3,
    "MISSING_REQUIRED_METRICS": 240,
    "NO_APPEARANCES": 5
  },
  "imputedMetricCounts": {
    "goalsPer90": 4,
    "dribbleSuccess": 2
  }
}
```

Reglas:

- `valuationDate` se fija en UTC al inicio y no cambia si la petición cruza medianoche.
- `evaluatedPlayers` cuenta solo jugadores con las dos estrategias publicadas.
- `failedPlayers` contabiliza fallos inesperados ocurridos después de comenzar el recorrido; no incluye exclusiones funcionales.
- `excludedPlayers` incluye siempre los cuatro motivos contractuales, aunque su conteo sea cero; cada jugador excluido se contabiliza en un único motivo según el orden de severidad de la spec, por lo que la suma no excede el número de jugadores excluidos.
- `imputedMetricCounts` contiene únicamente claves derivadas efectivamente imputadas; las claves ausentes tienen conteo cero implícito.
- Una vez completado el recorrido, la respuesta sigue siendo `200` aunque uno o todos los jugadores hayan fallado.
- Repetir la operación en la misma fecha reemplaza pares reevaluados y retira pares obsoletos, sin duplicados.

### Errores

| HTTP | `code` | Condición |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Body no vacío o cualquier query parameter. |
| 401 | gestionado por seguridad | JWT ausente, expirado, inválido o usuario no persistido. |
| 409 | `VALUATION_SNAPSHOT_UNAVAILABLE` | No existe campaña JSON confirmada con cobertura poblacional completa según el criterio de confirmación de la spec. No se modifica ningún resultado. |
| 500 | `VALUATION_EVALUATION_FAILED` | Fallo global controlado antes de completar el recorrido o construir el resumen. |

No se define error de concurrencia: el contrato operativo asume una única ejecución y las solicitudes concurrentes quedan fuera de alcance.

## OpenAPI y consumidores

El Controller y el Response DTO deben documentar propósito, autenticación, ausencia de entrada, campos, `200`, `400`, `401`, `409` y `500`. Los tests de Controller deben afirmar método, ruta, rechazo de entrada, estructura exacta y traducción de errores. No existen endpoints de lectura en esta feature.

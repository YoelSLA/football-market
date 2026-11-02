# Contrato de Consumo: Ranking de Jugadores (Frontend)

**Feature**: `022-view-player-rankings`
**Spec**: [`../spec.md`](../spec.md) · **Data Model**: [`../data-model.md`](../data-model.md)

Este documento describe el contrato de consumo de los servicios backend utilizados por la interfaz de usuario de Football Market para el ranking de jugadores.

---

## 1. `GET /api/players/ranking/score` (Modo Rendimiento)

Recupera la clasificación ordenada de jugadores según el score persistido de la estrategia activa en la última ejecución de cotización.

### 1.1. Petición

- **Método**: `GET`
- **Ruta**: `/api/players/ranking/score`
- **Cabeceras obligatorias**: `Authorization: Bearer <jwt_token>`
- **Parámetros**: Ninguno (sin query params ni body).

### 1.2. Respuesta Exitosa (`200 OK`)

```json
{
  "strategy": "POSITIONAL_PERFORMANCE",
  "quoteDate": "2026-10-23",
  "ranking": [
    {
      "position": 1,
      "playerId": 7821,
      "score": 0.913400,
      "valuationDate": "2026-10-21"
    },
    {
      "position": 2,
      "playerId": 7804,
      "score": 0.895000,
      "valuationDate": "2026-10-22"
    }
  ]
}
```

### 1.3. Semántica de Campos

| Campo | Tipo | Nulabilidad | Descripción |
| --- | --- | --- | --- |
| `strategy` | string | Nulable | Nombre de la estrategia activa (`POSITIONAL_PERFORMANCE` o `GENERAL_ATTRIBUTES`). Nulo si no hubo corridas previas. |
| `quoteDate` | string (ISO `yyyy-MM-dd`) | Nulable | Fecha de cotización de referencia de la corrida. Nula si no hubo ejecuciones. |
| `ranking` | array | No nulo | Lista ordenada de jugadores. Vacío (`[]`) si no hay jugadores con cotización y valuación activa. |
| `ranking[].position` | integer | No nulo | Posición ordinal en el ranking (1, 2, 3...), consecutiva y única. |
| `ranking[].playerId` | integer (long) | No nulo | Identificador único del jugador para cruzar con el catálogo. |
| `ranking[].score` | number (decimal) | No nulo | Puntuación obtenida en la estrategia (hasta 6 decimales de precisión). |
| `ranking[].valuationDate` | string (ISO `yyyy-MM-dd`) | No nulo | Fecha de la valuación concreta utilizada para calcular el score. |

---

## 2. `GET /api/players/ranking` (Modo Cotización)

Recupera la clasificación de jugadores ordenada por la cotización de mercado vigente de sus tokens.

### 2.1. Petición

- **Método**: `GET`
- **Ruta**: `/api/players/ranking`
- **Cabeceras obligatorias**: `Authorization: Bearer <jwt_token>`
- **Parámetros**: Ninguno.

### 2.2. Respuesta Exitosa (`200 OK`)

```json
{
  "ranking": [
    {
      "position": 1,
      "playerId": 7821,
      "price": 3.50,
      "effectiveDate": "2026-10-27",
      "origin": "RECOMPUTED",
      "calculatedAt": "2026-10-27T14:30:00Z"
    },
    {
      "position": 2,
      "playerId": 7804,
      "price": 3.20,
      "effectiveDate": "2026-10-27",
      "origin": "CARRIED_FORWARD",
      "calculatedAt": "2026-10-27T14:30:00Z"
    }
  ]
}
```

### 2.3. Semántica de Campos

| Campo | Tipo | Nulabilidad | Descripción |
| --- | --- | --- | --- |
| `ranking` | array | No nulo | Lista ordenada por precio descendente. Vacío si el mercado aún no tiene cotizaciones. |
| `ranking[].position` | integer | No nulo | Posición ordinal en el ranking de precios (1, 2, 3...). |
| `ranking[].playerId` | integer (long) | No nulo | Identificador único del jugador. |
| `ranking[].price` | number | No nulo | Cotización vigente del token expresada en créditos. |
| `ranking[].effectiveDate` | string (ISO `yyyy-MM-dd`) | No nulo | Fecha de vigencia de la cotización. |
| `ranking[].origin` | string | No nulo | Origen de la cotización: `RECOMPUTED` (recalculada) o `CARRIED_FORWARD` (arrastrada). |
| `ranking[].calculatedAt` | string (ISO DateTime) | No nulo | Marca temporal exacta de cálculo. |

---

## 3. Manejo de Respuestas y Errores en la Interfaz

| Código HTTP | Escenario | Comportamiento del Frontend |
| --- | --- | --- |
| `200 OK` (con datos) | Consulta exitosa | Renderiza el Podio Top 3, los metadatos de cabecera y la tabla paginada. |
| `200 OK` (`ranking: []`) | Sin datos calculados | Presenta componente `EmptyState` informando que no hay clasificaciones vigentes aún. |
| `401 Unauthorized` | Sesión expirada o ausente | Redirige automáticamente al usuario a `/login` preservando la intención. |
| `500 Internal Error` | Fallo en base de datos o servidor | Presenta banner/alerta accesible de error con botón "Reintentar consulta". |

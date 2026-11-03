# Contrato HTTP: Ranking de jugadores por score

**Feature**: `018-consultar-rankings-de-jugadores`
**Spec**: [`spec.md`](../spec.md) · **Data model**: [`../data-model.md`](../data-model.md)

Este documento es el contrato funcional y normativo de la consulta de ranking por score. El detalle
OpenAPI del Controller y de los DTO lo complementa; ante discrepancia, prevalece este documento.

Todas las rutas empiezan por `/api/`. Las respuestas de error usan `ErrorResponseDTO` (`timestamp`,
`status`, `error`, `code`, `message`, `path`). La ruta exige JWT válido, conforme a
`anyRequest().authenticated()` de `SecurityConfig`.

---

## 1. `GET /api/players/ranking/score`

Devuelve el ranking de jugadores ordenado por el score final más reciente de la estrategia registrada en la última
ejecución de cotización persistida (FR-001, FR-002).

### 1.1. Request

Sin body, sin query parameters declarados y sin path variables. No se admite seleccionar estrategia,
fecha, ordenamiento, paginación ni filtros (FR-001). Cualquier query parameter enviado se ignora por
Spring MVC: no forma parte del contrato y no altera la estrategia, la población, el orden ni el
contenido de la respuesta.

### 1.2. Response 200

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
      "score": 0.871250,
      "valuationDate": "2026-10-22"
    }
  ]
}
```

| Campo | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `strategy` | `string` | sí | Estrategia de la última ejecución: `POSITIONAL_PERFORMANCE` o `GENERAL_ATTRIBUTES`. Nula si no hay ejecuciones |
| `quoteDate` | `date` ISO (`yyyy-MM-dd`) | sí | Fecha de cotización de la última ejecución, sin hora ni zona. Nula si no hay ejecuciones |
| `ranking` | `array` | no | Entradas ordenadas por score descendente y, a igualdad, por `playerId` ascendente. Vacío si no hay entradas |

La respuesta 200 contiene exclusivamente los campos declarados en §1.2 y §1.2.1; no se definen campos adicionales. Cualquier campo nuevo constituye un cambio de contrato. La conformidad se evalúa comparando el JSON con los campos y tipos declarados: un campo obligatorio ausente, un tipo distinto o un campo no declarado es una desviación del contrato.

#### 1.2.1. Entrada del ranking

| Campo | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `position` | `integer` | no | Posición en el ranking, desde 1, consecutiva y no compartida |
| `playerId` | `long` | no | Identificador interno del jugador. Es el único dato identificatorio de la entrada |
| `score` | `decimal` | no | Resultado final persistido de la estrategia: `finalScore` para `POSITIONAL_PERFORMANCE` y `finalGrl` para `GENERAL_ATTRIBUTES`. Se devuelve a la escala persistida (seis decimales), sin redondeo ni transformación |
| `valuationDate` | `date` ISO (`yyyy-MM-dd`) | no | Fecha de la valuación utilizada para obtener el score, sin hora ni zona |

La entrada no incluye nombre ni otros datos del jugador (FR-008). La versión de la estrategia no
forma parte de la respuesta (FR-009) y tampoco se expone el identificador de la valuación utilizada
(FR-017).

### 1.3. Comportamiento

| Situación | Resultado |
| --- | --- |
| Existe una última ejecución con estrategia y valuaciones de esa estrategia | `200` con encabezado no nulo y una entrada por jugador elegible |
| Varios jugadores con scores distintos | Orden por score descendente; posiciones de 1 a n |
| Dos o más jugadores con el mismo score | Desempata `playerId` ascendente; posiciones consecutivas distintas |
| Un jugador tiene varias valuaciones de la estrategia | Se usa la de mayor `valuationDate`. El esquema admite una sola por jugador, fecha, estrategia y versión. Se expone la `valuationDate` de esa fila |
| La valuación más reciente es posterior o distinta de la usada para la última cotización | Se utiliza igualmente: la ejecución determina la estrategia y el encabezado, no la valuación concreta; `valuationDate` identifica la fecha del score mostrado |
| Un jugador tiene cotización vigente pero no valuación de la estrategia | No aparece |
| Un jugador tiene valuación de la estrategia pero no cotización vigente | No aparece |
| No existe ninguna ejecución de cotización persistida | `200` con `strategy: null`, `quoteDate: null` y `ranking: []` |
| Existe ejecución pero su estrategia no tiene ninguna valuación | `200` con `strategy` y `quoteDate` no nulos y `ranking: []` |
| Todos los jugadores fueron arrastrados por no tener valuación | `200` con encabezado no nulo y `ranking: []` |
| Los mismos datos se consultan más de una vez | Orden, posiciones y valuación elegida idénticos |
| Se consulta el ranking | No se escriben filas ni se alteran cotizaciones, precios o valuaciones |

### 1.4. Errores

| Código | HTTP | Causa |
| --- | --- | --- |
| `401` | 401 | JWT ausente o inválido |
| `INTERNAL_ERROR` | 500 | Fallo técnico que impide recuperar la clasificación; no se devuelve un resultado parcial |

No se definen códigos de error nuevos. La ausencia de ejecuciones o de valuaciones no es un error: se
responde `200` con el arreglo vacío. Las respuestas de error usan exclusivamente los campos de
`ErrorResponseDTO` y no incluyen trazas, identificadores internos ni mensajes técnicos (FR-017).

---

## 2. Autenticación y autorización

La ruta exige un JWT válido: una solicitud sin JWT o con JWT inválido responde `401` sin devolver
datos del ranking. No requiere roles ni permisos adicionales y no define un mecanismo propio de
autenticación: cualquier usuario autenticado obtiene el mismo ranking (FR-016).

---

## 3. Trazabilidad de requisitos

| Elemento del contrato | Requisitos |
| --- | --- |
| Ruta sin parámetros | FR-001 |
| `strategy` y `quoteDate` | FR-002, FR-009 |
| Población | FR-003 |
| `score` y `valuationDate` | FR-004, FR-008, FR-012 |
| Orden y `position` | FR-006, FR-007 |
| Respuestas vacías | FR-010, FR-011 |
| Solo lectura | FR-013 |
| Errores | FR-014 |
| JWT, roles y autorización | FR-016 |
| No exposición de datos internos | FR-017 |

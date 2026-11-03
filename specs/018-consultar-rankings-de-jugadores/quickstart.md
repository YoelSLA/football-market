# Quickstart: validación del ranking por score

**Feature**: `018-consultar-rankings-de-jugadores`
**Spec**: [`spec.md`](spec.md) · **Contrato**: [`contracts/score-ranking-http.md`](contracts/score-ranking-http.md)

Guía de validación manual sobre la aplicación en ejecución. No contiene código de implementación: las
fuentes persistidas y las reglas de selección están en [`data-model.md`](data-model.md) y las
decisiones de diseño en [`research.md`](research.md).

---

## 1. Requisitos previos

| Requisito | Detalle |
| --- | --- |
| Base de datos | PostgreSQL accesible por la aplicación, con las migraciones aplicadas |
| Datos | Jugadores en el catálogo, cotizaciones en `player_quotes` y valuaciones de SPEC-013 en `player_positional_valuations` o `player_attribute_valuations` |
| Ejecución de cotización | Al menos una fila en `quote_executions` para validar el caso con encabezado |
| Token | JWT de un usuario válido |
| Build | `./gradlew build -x test` desde `backend/` (no ejecuta tests) |

> Los tests automatizados los ejecuta el usuario. Esta guía describe validaciones manuales.

---

## 2. Levantar la aplicación

```bash
cd backend
./gradlew bootRun
```

Comprobar disponibilidad:

```bash
curl -s http://localhost:8080/actuator/health
```

---

## 3. Preparar datos (si la base está vacía)

1. Inicializar el mercado de cotizaciones (SPEC-016).
2. Obtener las métricas semanales aplicables.
3. Ejecutar la valuación estadística (SPEC-013).
4. Ejecutar el recálculo de cotizaciones con la estrategia elegida (SPEC-016).

La estrategia usada por la última ejecución es la que encabezará el ranking. No se pasa por parámetro
al consultar el ranking. Este es el flujo operativo habitual, pero la consulta puede realizarse entre
sus etapas o después de un fallo parcial: usa la valuación más reciente actualmente persistida de esa
estrategia, aunque sea posterior o distinta de la utilizada para calcular la cotización.

---

## 4. Consultar el ranking por score

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/players/ranking/score
```

**Esperado**: `200` con un objeto de tres campos:

- `strategy`: la estrategia de la última ejecución (`POSITIONAL_PERFORMANCE` o
  `GENERAL_ATTRIBUTES`).
- `quoteDate`: la fecha de cotización de esa ejecución.
- `ranking`: entradas ordenadas por `score` descendente y, a igualdad, por `playerId` ascendente, con
  posiciones consecutivas desde 1.

La forma exacta de los campos está en
[`contracts/score-ranking-http.md` §1.2](contracts/score-ranking-http.md).

---

## 5. Casos a comprobar

| Caso | Preparación | Resultado esperado |
| --- | --- | --- |
| Ranking con datos | Última ejecución con valuaciones de su estrategia | Una entrada por jugador elegible, orden por score descendente y posiciones 1..n |
| Empate de dos jugadores | Dos jugadores elegibles con el mismo score | Primero el `playerId` menor; posiciones consecutivas distintas |
| Empate múltiple | Tres o más jugadores con el mismo score y otro grupo con score distinto | Dentro de cada grupo, `playerId` ascendente; grupos por score descendente; posiciones continuas |
| Varias valuaciones del mismo jugador | Valuaciones de la estrategia en fechas distintas | Se usa la más reciente y se muestra su `valuationDate` |
| Valuación posterior a la última cotización | Persistir una valuación de la estrategia después de `quoteDate`, sin recalcular la cotización | Se muestra el score de esa valuación; `quoteDate` conserva la fecha de la ejecución y `valuationDate` identifica el score |
| Jugador sin valuación de la estrategia | Jugador con cotización vigente pero sin fila en la tabla de la estrategia | No aparece en el ranking |
| Jugador sin cotización vigente | Jugador con valuación pero sin fila en `player_quotes` | No aparece en el ranking |
| Sin ejecuciones de cotización | Base sin filas en `quote_executions` | `200` con `strategy: null`, `quoteDate: null`, `ranking: []` |
| Ejecución sin valuaciones | Última ejecución cuya estrategia no tiene valuaciones | `200` con encabezado no nulo y `ranking: []` |
| Todos los jugadores arrastrados | Ejecución sin valuaciones de su estrategia | Encabezado no nulo y arreglo vacío |

---

## 6. Repetibilidad

Ejecutar la misma consulta más de una vez y comparar las respuestas:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/players/ranking/score > ranking-a.json
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/players/ranking/score > ranking-b.json
```

**Esperado**: orden, posiciones y `valuationDate` de cada entrada idénticos en ambas respuestas.

---

## 7. Solo lectura

1. Consultar el historial y la cotización vigente de un jugador incluido en el ranking.
2. Consultar el ranking por score.
3. Repetir las consultas del paso 1.

**Esperado**: mismas filas, mismos precios y mismos `calculatedAt` antes y después. La consulta del
ranking no escribe ni altera cotizaciones, precios ni valuaciones (FR-013, SC-007).

---

## 8. Errores

| Llamada | Respuesta esperada |
| --- | --- |
| Sin header `Authorization` | `401` |
| Con JWT inválido o expirado | `401` |

La ausencia de ejecuciones o de valuaciones no produce error: devuelve `200` con el arreglo vacío.

---

## 9. Verificación local

Build permitido para cambios de backend:

```bash
cd backend
./gradlew build -x test
```

Tests aplicables, a cargo del usuario (no los ejecuta el agente):

```bash
cd backend
./gradlew lightweightTest
./gradlew integrationTest
```

El build no ejecuta tests y no acredita el testing de la funcionalidad. La ejecución y el resultado de
los tests son una autorización humana separada conforme a la Constitución.

# Quickstart: validación del recálculo automático programado

**Feature**: `023-scheduler-quote-recalculation`
**Spec**: [`spec.md`](spec.md) · **Contratos**:
[`contracts/quote-executions-http.md`](contracts/quote-executions-http.md) y
[`contracts/scheduling-configuration.md`](contracts/scheduling-configuration.md)

Guía de validación manual sobre la aplicación en ejecución. No contiene código de implementación: los
detalles de datos están en [`data-model.md`](data-model.md) y las decisiones de diseño en
[`research.md`](research.md).

> El agente no ejecuta tests ni los presenta como ejecutados. Los tests automatizados los ejecuta el
> usuario; esta guía describe comprobaciones observables sobre la aplicación levantada.

---

## 1. Requisitos previos

| Requisito | Detalle |
| --- | --- |
| Base de datos | PostgreSQL accesible, con las migraciones aplicadas (incluida la nueva columna `origin`) |
| Entorno | Variables de `application.yml` y de `application-dev.yml` configuradas (base de datos, `JWT_SECRET`) |
| Datos | Jugadores cotizables con métricas (SPEC-012) y valuaciones (SPEC-013) para que el recálculo tenga insumos |
| Token | JWT de un usuario válido para consultar por HTTP |
| Compilación | `./gradlew build -x test` desde `backend/` (no ejecuta tests) |
| Docker | Solo si se van a ejecutar los tests de integración; no es necesario para esta guía |

---

## 2. Levantar la aplicación con una cadencia de prueba

La cadencia se ajusta por configuración. Para observar el disparo sin esperar al lunes, usar una
expresión de cada minuto y evaluar en UTC:

```powershell
$env:MARKET_QUOTE_SCHEDULE_CRON = "0 */1 * * * *"
$env:MARKET_QUOTE_STRATEGY = "POSITIONAL_PERFORMANCE"
cd backend
./gradlew bootRun
```

Disponibilidad del servicio:

```bash
curl -s http://localhost:8080/actuator/health
```

**Esperado**: `{"status":"UP"}`. Una expresión cron inválida debe impedir el arranque con un error de
configuración; comprobarlo es opcional y requiere reiniciar la aplicación.

---

## 3. Escenario A — el recálculo corre solo (US1)

Sin inicializar el mercado y con la aplicación levantada:

1. Esperar al siguiente disparo (≤ 60 s con la cadencia de prueba).
2. Revisar el log de la aplicación (`logs/football-market.log` o consola).

**Esperado**:

- `quote_automatic_recalculation_rejected reason=MARKET_NOT_INITIALIZED`.
- No hay fila en `quote_executions` para la fecha:

```sql
SELECT quote_date, origin, priced_players, failed_players
FROM quote_executions
ORDER BY quote_date DESC;
```

3. Inicializar el mercado y ejecutar una valuación con los endpoints existentes (SPEC-013 y SPEC-016):

```bash
curl -s -X POST http://localhost:8080/api/quotes/initialize \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"marketDate":"2026-11-02"}'
```

4. Esperar al siguiente disparo.

**Esperado**:

- `quote_automatic_recalculation_triggered quoteDate=<hoy UTC>` y, a continuación, el resumen de la
  ejecución (`quote_execution_started` / `quote_execution_completed` con `origin=AUTOMATIC`).
- Una fila persistida con `origin = 'AUTOMATIC'` para la fecha UTC del día.
- Las cotizaciones resultantes son equivalentes a las de una ejecución manual con los mismos insumos
  (mismos contadores y mismos precios para los mismos jugadores y valuaciones).

---

## 4. Escenario B — disparo duplicado el mismo día (US1, US3)

Con la aplicación corriendo y una ejecución ya registrada para la fecha UTC (resultado del escenario A):

1. Esperar al siguiente disparo.

**Esperado**:

- `quote_automatic_recalculation_skipped reason=EXECUTION_ALREADY_EXISTS`.
- No se crea una segunda fila y no se escriben cotizaciones: el historial de un jugador no gana filas
  nuevas con la fecha de esa corrida.

```sql
SELECT count(*) FROM quote_executions WHERE quote_date = CURRENT_DATE;  -- 1
```

2. Intentar el mismo día una ejecución manual desde HTTP:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/quotes/recalculate \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"strategy":"POSITIONAL_PERFORMANCE"}'
```

**Esperado**: `409`, coherente con la unicidad por fecha ya vigente.

> La unicidad por fecha se verifica también con el test de integración del caso de uso. No se añade
> una garantía de exclusión global entre recálculos de fechas distintas.

---

## 5. Escenario C — trazabilidad por fecha (US2)

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/quotes/executions?date=$(date -u +%Y-%m-%d)"
```

**Esperado**: un único elemento con `origin`, `result`, `failureReason`, `pricedPlayers`,
`recomputedRows`, `carriedForwardRows`, `failedPlayers`, `startedAt` y `finishedAt`.

Comprobaciones:

| Caso | Resultado esperado |
| --- | --- |
| Corrida sin fallos individuales | `result: "SUCCESS"`, `failureReason: null` |
| Corrida con al menos un jugador fallido | `result: "FAILED"`, `failureReason: "INDIVIDUAL_PLAYER_FAILURES"` |
| Ejecución manual (`POST /api/quotes/recalculate`) | `origin: "MANUAL"` |
| Ejecución disparada por el scheduler | `origin: "AUTOMATIC"` |
| Consulta de una fecha sin ejecución | `{"executions": []}` |

Verificación directa del registro persistido:

```sql
SELECT quote_date, origin, strategy_name, priced_players, failed_players, started_at, finished_at
FROM quote_executions
ORDER BY quote_date DESC
LIMIT 5;
```

El contenido expuesto no debe incluir trazas, mensajes técnicos ni identificadores internos (FR-010,
SC-006).

---

## 6. Escenario D — configuración de estrategia (FR-002)

1. Reiniciar con `MARKET_QUOTE_STRATEGY=GENERAL_ATTRIBUTES`.
2. Esperar un disparo en una fecha UTC libre.

**Esperado**: la fila de ejecución registra `strategy_name = 'GENERAL_ATTRIBUTES'` y los parámetros
`adjustment_factor`, `variation_cap` y `min_price_floor` de `market.quote`. Ninguna fórmula de
valuación cambia (FR-003).

---

## 7. Escenario E — fallo completo de la corrida (opcional, invasivo)

FR-013 exige que el scheduler registre el error recibido del recálculo. Provocarlo a mano
requiere cortar el acceso a la base durante la corrida (por ejemplo, detener PostgreSQL con el
disparador en marcha), lo que afecta a toda la aplicación.

Procedimiento, solo en un entorno de desarrollo descartable:

1. Levantar la aplicación con una cadencia de cada minuto y una fecha UTC sin ejecución.
2. Detener PostgreSQL inmediatamente después de que aparezca `quote_automatic_recalculation_triggered`.
3. Revisar el log.

**Esperado**:

- `quote_automatic_recalculation_failed cause=<clase de la excepción>`.
- La persistencia, las cotizaciones y la posibilidad de reintento siguen el comportamiento del
  recálculo existente; el scheduler no compensa ni modifica sus resultados (TD-024).

Este escenario es opcional y no sustituye al test de integración que lo demuestra de forma
determinista.

---

## 8. Escenario F — reutilización por otro proceso batch (FR-011)

**Esperado**: el scheduling está habilitado una sola vez en el arranque y no existe registro central de
jobs, cola ni abstracción de procesos. Un proceso futuro (#41) añade su propio método `@Scheduled` en
el módulo de su feature.

Comprobación estática:

```bash
grep -rn "EnableScheduling" backend/src/main/java
```

**Esperado**: una única declaración, en `FootballMarketApplication`. La programación de #41 **no** debe
existir en este cambio.

---

## 9. Criterios de aceptación verificables

| Criterio | Cómo se comprueba |
| --- | --- |
| SC-001 | El disparo ocurre con la cadencia configurada; por defecto, lunes 00:00 UTC (escenario A) |
| SC-002 | `GET /api/quotes/executions?date=` devuelve `origin`, `result` y el alcance de la corrida (escenario C) |
| SC-003 | Ante un fallo del caso de uso, el scheduler registra el error sin compensar cotizaciones |
| SC-004 | Tras un intento sin ejecución registrada, reintento manual conforme a SPEC-016 |
| SC-005 | La corrida se completa sin ninguna acción de un operador (escenario A) |
| SC-006 | La respuesta no contiene trazas, mensajes técnicos ni identificadores internos (escenario C) |
| SC-007 | El disparo duplicado no crea fila ni escribe cotizaciones y deja el evento `skipped` (escenario B) |
| SC-008 | El scheduler registra el error; persistencia y reintentos siguen el comportamiento existente (escenario E) |

---

## 10. Verificación local

```bash
cd backend
./gradlew build -x test
```

El build no ejecuta tests y no acredita el testing de la funcionalidad: conforme a la Constitución, los
tests los ejecuta el usuario y la validación de tests es una autorización humana separada.

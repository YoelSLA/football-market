# Quickstart: validación de valoraciones estadísticas

> Guía para ejecutar después de implementar SPEC-013. PLAN no ejecutó estos comandos. Los tests y escenarios corresponden al usuario.

## Prerrequisitos

1. Java 21 y Docker disponibles.
2. PostgreSQL de desarrollo configurado y migraciones aplicadas al iniciar el backend.
3. Una campaña JSON de SPEC-012 confirmada según el criterio de FR-004 (run terminado, sin items en fallo estructural y con fila JSON para todo item `COMPLETED`) y, opcionalmente, filas SCRAPING de la misma fecha.
4. Un usuario persistido y un JWT Bearer vigente.
5. Revisar [data-model.md](data-model.md) y [contracts/http.md](contracts/http.md).

## Controles automatizados para el usuario

Desde `backend/`:

```bash
./gradlew test --tests '*Valuation*'
```

La suite esperada debe cubrir:

- fórmulas, percentil de rango medio, inversiones, confianza, clamp y redondeo;
- fallback JSON/SCRAPING, cero válido, ausencias, medianas pares y ausencia de donantes;
- constraints, unicidad, reemplazo, limpieza e índices mediante PostgreSQL/Testcontainers;
- recorrido poblacional, continuidad ante fallos y publicación conjunta;
- contrato HTTP completo y seguridad JWT.

## Iniciar el backend

Con las variables locales requeridas por el proyecto:

```bash
./gradlew bootRun
```

Obtener un JWT mediante el flujo de autenticación existente. No guardar credenciales ni tokens en el repositorio.

## Escenario 1: evaluación completa

```bash
curl -i -X POST http://localhost:8080/api/valuations/evaluate \
  -H "Authorization: Bearer $FOOTBALL_MARKET_TOKEN"
```

Esperado:

- `200 OK`;
- respuesta conforme a [contracts/http.md](contracts/http.md);
- `evaluatedPlayers + failedPlayers + suma(excludedPlayers)` representa la población recorrida según las reglas funcionales;
- cada evaluado tiene exactamente una fila de cada estrategia para `valuationDate`.

## Escenario 2: idempotencia diaria

Repetir la petición anterior el mismo día UTC.

Esperado:

- `200 OK`;
- no aumenta el máximo de una fila por estrategia, jugador y fecha;
- los jugadores reevaluados conservan un par coherente;
- quienes dejaron de ser evaluables o fallaron no conservan resultados viejos de esa fecha.

## Escenario 3: fallback y cero

Preparar datos controlados donde una clave compartida sea nula en JSON y presente en SCRAPING, y otro jugador tenga cero JSON con SCRAPING distinto.

Esperado:

- el primer cálculo usa SCRAPING y refleja procedencia `MIXED` o `SCRAPING` según el resto de entradas;
- el segundo conserva cero JSON;
- ninguna ausencia se convierte en cero.

## Escenario 4: imputación

Preparar jugadores del mismo grupo con denominador cero y donantes válidos, incluida una cantidad par de donantes; preparar otro grupo sin donantes.

Esperado:

- la mediana par promedia los dos valores centrales antes del redondeo;
- las claves aparecen en `imputedMetrics` y en el conteo agregado;
- sin donantes se usa el normalizado neutro 0.5;
- no aparecen divisiones inválidas.

## Escenario 5: ausencia de snapshot confirmado

En un entorno aislado sin campaña JSON confirmada según FR-004:

```bash
curl -i -X POST http://localhost:8080/api/valuations/evaluate \
  -H "Authorization: Bearer $FOOTBALL_MARKET_TOKEN"
```

Esperado: `409` con `code = VALUATION_SNAPSHOT_UNAVAILABLE` y ninguna modificación de resultados.

## Escenario 6: seguridad y entrada prohibida

- Sin token: esperar `401`.
- Con token inválido: esperar `401`.
- Con query o body no vacío: esperar `400 INVALID_REQUEST` sin iniciar la evaluación.

## Límites a comprobar

La revisión final no debe encontrar scheduler, ejecución automática, endpoint de lectura, ranking, selección activa, precio/cotización, cambios de extracción ni llamadas externas introducidas por SPEC-013.

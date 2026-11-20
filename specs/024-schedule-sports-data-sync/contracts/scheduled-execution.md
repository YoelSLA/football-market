# Contrato interno: ejecución deportiva programada

## Disparo

Spring dispara los domingos a las 12:00 de `America/Argentina/Buenos_Aires`, mediante cron `0 0 12 * * SUN`. Se reutiliza el scheduler de Spring existente, sin un segundo `@EnableScheduling`. El perfil `test` no dispara trabajo real. Se asume aplicación activa y ausencia de ejecuciones paralelas; no hay recuperación, retries ni lock nuevos.

## Secuencia y resultados

| Orden | Operación existente | Entrada |
| --- | --- | --- |
| 1 | `PlayerStatisticsSynchronizationService` JSON | Sin parámetros funcionales. |
| 2 | `PlayerStatisticsSynchronizationService` scraping | `PlayerStatisticsSyncRequest(null)`: fecha UTC propia por defecto. |
| 3 | `ValuationEvaluationService.evaluate()` | Sin argumentos; selección y fecha propias de SPEC-013. |

Se espera el retorno completo de cada etapa. La fachada de estadísticas ofrece una vía interna para el disparador que reutiliza el recorrido manual y comunica globalmente excepciones, `FAILED`, `INTERRUPTED` y cortes globales como `PROVIDER_UNAVAILABLE`, aunque el resumen sea `PARTIAL` o `COMPLETED`. La señal contiene únicamente la identidad y causa segura disponible, no métricas ni datos sensibles. La fachada no convierte un `PARTIAL` por fallos individuales en fallo global. Cualquier fallo global de JSON impide scraping y evaluación; uno de scraping impide evaluación; fallo de evaluación termina la secuencia. El disparador registra el fallo sin propagarlo a Spring y mantiene la interrupción del hilo cuando corresponda.

La fachada automática devuelve un indicador operativo simple para distinguir un resultado parcial de sincronización, sin exponer contadores ni exigir al disparador inspeccionar Model. La evaluación mantiene su semántica propia para fallos individuales; un retorno normal se registra como etapa terminada sin reclasificarlo. La secuencia no invoca recálculo de cotizaciones ni operaciones financieras.

## Contratos preservados y registro

No se añade HTTP ni se cambian los POST manuales, autenticación, DTO, respuestas o errores de [SPEC-012](../../012-player-statistics-scraping/contracts/http.md) y [SPEC-013](../../013-player-statistical-valuations/contracts/http.md). Los procesos manuales siguen disponibles tras un fallo automático, sujetos a sus restricciones propias.

El log existente informa inicio y resultado final; un fallo identifica `JSON`, `SCRAPING` o `EVALUATION` y el código/motivo seguro comunicado. Si no existe detalle seguro disponible, se registra la identidad segura de la excepción o estado, sin fabricar causa, texto arbitrario, secretos ni trazas. No se añade persistencia, consulta ni canal nuevos.

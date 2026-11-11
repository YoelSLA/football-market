# Contrato: programación y configuración del recálculo automático

**Feature**: `023-scheduler-quote-recalculation`
**Spec**: [`spec.md`](../spec.md) · **Data model**: [`../data-model.md`](../data-model.md)

Este documento fija los contratos **no HTTP** de la feature: la configuración de la programación, el
componente disparador, la matriz de comportamiento ante cada situación y el catálogo de eventos
operativos. El contrato HTTP está en
[`quote-executions-http.md`](quote-executions-http.md).

---

## 1. Configuración

Bloque `market.quote` de `QuoteProperties` (`@ConfigurationProperties`, validado):

```yaml
market:
  quote:
    strategy: ${MARKET_QUOTE_STRATEGY:POSITIONAL_PERFORMANCE}
    adjustment-factor: ${MARKET_QUOTE_ADJUSTMENT_FACTOR:0.10}
    variation-cap: ${MARKET_QUOTE_VARIATION_CAP:0.10}
    min-price-floor: ${MARKET_QUOTE_MIN_PRICE_FLOOR:1}
    initial-anchor: ${MARKET_QUOTE_INITIAL_ANCHOR:100}
    schedule:
      cron: ${MARKET_QUOTE_SCHEDULE_CRON:0 0 12 * * *}
      enabled: ${MARKET_QUOTE_SCHEDULE_ENABLED:true}
```

| Clave | Tipo | Obligatoria | Por defecto | Regla |
| --- | --- | --- | --- | --- |
| `market.quote.strategy` | `ValuationStrategy` | no | `POSITIONAL_PERFORMANCE` | Estrategia de la ejecución automática (FR-002). Fuera del catálogo → falla el arranque |
| `market.quote.schedule.cron` | `String` | no | `0 0 12 * * *` | Expresión cron de Spring, 6 campos (`segundo minuto hora día mes díaDeSemana`), cada día a las 12:00 UTC por defecto (FR-001) |
| `market.quote.schedule.enabled` | `boolean` | no | `true` | Habilitación del disparador. No es requisito funcional: existe para dejar el disparador inerte en la suite y en entornos controlados |

Reglas de configuración:

- La evaluación es **siempre UTC**, con independencia de la zona del servidor o de la zona Jackson de
  la aplicación. No se expone una zona configurable (ver [`research.md`](../research.md) §9).
- La expresión cron la valida Spring al construir el scheduler; una expresión inválida impide el
  arranque en lugar de degradar a un valor por defecto.
- `adjustment-factor`, `variation-cap` y `min-price-floor` conservan su validación vigente y son los
  parámetros que aplica la ejecución automática (FR-002). La ejecución manual sigue admitiendo los
  suyos en el request.
- Un cambio de cadencia o de estrategia requiere reinicio de la aplicación: la feature no introduce
  recarga en caliente ni operación de administración.

---

## 2. Componente disparador

| Aspecto | Definición |
| --- | --- |
| Ubicación | `backend/src/main/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationScheduler.java` |
| Responsabilidad | Punto de entrada del proceso batch: comprobar la habilitación, invocar el caso de uso y traducir los rechazos controlados a eventos operativos. Sin lógica de negocio ni de dominio |
| Dependencias permitidas | `QuoteRecalculationService` (interfaz) y `QuoteProperties` (solo `schedule.enabled`) |
| Dependencias prohibidas | Repository, Integration, DTO, Mapper, Controller y cualquier Model de dominio usado con lógica |
| Concurrencia | `@Scheduled` sobre el `TaskScheduler` autoconfigurado; el hilo único reduce solapamientos locales, pero la unicidad por fecha automática se impone en base de datos y no es una garantía del hilo |
| Errores | No se propagan: cada desenlace se registra y el método termina. Un fallo no detiene disparos futuros |
| HTTP | No expone ninguna ruta ni participa del contrato HTTP |

Habilitación del scheduling: una sola vez en `FootballMarketApplication` con `@EnableScheduling`. Un
proceso batch futuro (#41) añade su propio método `@Scheduled` en el módulo de su feature sobre la
misma infraestructura, sin configuración central nueva (FR-011).

---

## 3. Punto de entrada del caso de uso

Sobre la interfaz existente `QuoteRecalculationService`:

```java
QuoteExecution recalculate(
    String strategy, BigDecimal adjustmentFactor, BigDecimal variationCap, Long minPriceFloor);

QuoteExecution recalculateAutomatic();
```

| Método | Origen persistido | Estrategia y parámetros | Consumidores |
| --- | --- | --- | --- |
| `recalculate(strategy, factor, cap, floor)` | `MANUAL` | Los del request; los ausentes toman el valor de `market.quote` | `QuoteController` (sin cambios) |
| `recalculateAutomatic()` | `AUTOMATIC` | Todos desde `market.quote`, incluida `market.quote.strategy` | Disparador automático |

Ambos comparten el mismo recorrido: validación de estrategia, verificación de mercado inicializado,
resolución de parámetros, reserva de corrida, publicación por jugador y finalización del resumen.
Las corridas manuales se registran ilimitadamente. Una reserva automática para fecha ya ocupada por
otra automática se omite; una corrida manual en esa fecha no bloquea la automática. La publicación
actualiza la cotización existente del jugador-fecha, cualquiera sea el origen previo. No se añade exclusión global. Los errores
controlados son los mismos:

| Excepción | Significado | Efecto |
| --- | --- | --- |
| `UnknownQuoteStrategyException` | Estrategia fuera del catálogo | Sin escrituras. No alcanzable por configuración validada al arrancar |
| `MarketNotInitializedException` | No existe fila `INITIAL` | Sin escrituras (FR-012) |
| `ExecutionAlreadyExistsException` | Ya existe corrida automática para la fecha | Disparo automático omitido; corridas manuales no generan este conflicto (FR-006) |
| Excepción no controlada | Fallo completo | El scheduler registra el error; el rollback y la persistencia siguen el comportamiento existente |

---

## 4. Matriz de comportamiento del disparo

Los efectos de persistencia descritos en esta matriz corresponden al caso de uso existente, no a garantías adicionales del scheduler. Ante un error el scheduler solo lo registra; no compensa cotizaciones ni gestiona recuperación o reintentos (decisión 2026-11-04).

| # | Situación | Resultado observable | Registro operativo | Ejecución persistida |
| --- | --- | --- | --- | --- |
| 1 | Disparador deshabilitado | No se invoca el caso de uso | Ninguno (silencio deliberado) | Ninguna |
| 2 | Mercado operativo, fecha libre | Recálculo completo con las reglas vigentes (FR-001, FR-002, FR-009) | `triggered` + eventos de ejecución existentes con `origin=AUTOMATIC` | Sí, `origin=AUTOMATIC` |
| 3 | Ya existe corrida automática para la fecha UTC | Se omite; no se escriben cotizaciones | `skipped reason=EXECUTION_ALREADY_EXISTS` | Corrida automática existente; ejecuciones manuales de la fecha permanecen |
| 5 | Mercado no inicializado | Rechazo controlado; no se altera ninguna cotización | `rejected reason=MARKET_NOT_INITIALIZED` | Ninguna para ese intento; ejecuciones manuales posteriores permitidas (FR-012) |
| 6 | Fallo individual de un jugador | La corrida continúa y lo cuenta | Evento de fallo por jugador existente | Sí, `result=FAILED`, `failureReason=INDIVIDUAL_PLAYER_FAILURES` |
| 7 | Fallo completo de la corrida | El scheduler registra el error sin compensación | `failed cause=<clase de la excepción>` | Según el comportamiento existente; si no queda fila, fecha libre para reintento manual (FR-013) |

El caso 7 se produce cuando una excepción no controlada alcanza la transacción externa. Un fallo
individual (caso 6) no revierte la corrida: es el comportamiento fijado por SPEC-016 (FR-062), que esta
feature conserva (FR-003). En el caso 7 las cotizaciones que ya confirmaron jugadores individuales
pueden permanecer: la ejecución automática no las compensa, porque esa responsabilidad excede su alcance
y es heredada de SPEC-016 (TD-024).

---

## 5. Catálogo de eventos operativos

Formato estructurado SLF4J vigente: `nombre_evento campo=valor ...`.

| Evento | Nivel | Campos | Requisito |
| --- | --- | --- | --- |
| `quote_automatic_recalculation_triggered` | `INFO` | `quoteDate` | FR-001 |
| `quote_automatic_recalculation_skipped` | `WARN` | `quoteDate`, `reason` = corrida automática ya registrada | FR-006 |
| `quote_automatic_recalculation_rejected` | `WARN` | `quoteDate`, `reason` = `MARKET_NOT_INITIALIZED` | FR-012 |
| `quote_automatic_recalculation_failed` | `ERROR` | `quoteDate`, `cause` = clase de la excepción | FR-013 |
| `quote_execution_started` / `quote_execution_completed` (existentes) | `INFO` | Se amplían con `origin` | FR-004 |

Reglas:

- El registro operativo **no** sustituye ni duplica `quote_executions`: documenta el intento cuando no
  hay ejecución persistida.
- No se añaden métricas, endpoints de Actuator, trazas distribuidas ni purga de logs (FR-005, §VI).
- Los nombres de evento son estables y se consideran parte del contrato operativo.

---

## 6. Fuera de contrato

No forman parte de esta feature y no deben implementarse como parte de ella: ejecución manual del
proceso automático por HTTP, endpoint de estado del scheduler, recarga de la cadencia en caliente,
reintentos automáticos, purga de ejecuciones, programación de #41 (sincronización de datos deportivos),
métricas o alertas nuevas y cualquier cambio en las fórmulas de valuación o cotización.

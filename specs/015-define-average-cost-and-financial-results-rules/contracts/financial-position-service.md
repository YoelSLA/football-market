# Contrato del componente de cálculo financiero — SPEC-015

**Feature**: SPEC-015 · **Rama**: `feat/015-define-average-cost-and-financial-results-rules` · **Fecha**: 2026-10-20
**Plan**: [`../plan.md`](../plan.md) · **Modelo**: [`../data-model.md`](../data-model.md)

Este documento define el contrato del componente reutilizable que SPEC-015 entrega y que SPEC-014
consume. Es la frontera entre ambas features: SPEC-014 **no** redefine ninguna regla contable.

---

## 1. Naturaleza y ubicación

| Aspecto | Valor |
| --- | --- |
| Rol arquitectónico | **Service**, conforme a `docs/backend/architecture.md` §1.1 |
| Ubicación | Interfaz en `footballmarket.services`; implementación en `footballmarket.services.impl` |
| Consumidor | La consulta de portfolio de **SPEC-014**, que depende de la **interfaz** |
| Carácter | Caso de uso de solo lectura, reutilizable, sin endpoint propio |
| Reglas contables | En **Model**: `PlayerFinancialPosition` y `FinancialPositionCalculator` |
| Dependencias | Model, Repository y el puerto `QuoteSource` (Integration) |

**No es un Service interno.** `docs/backend/architecture.md` §1.4 reserva `services/internal` para
colaboradores que no exponen interfaz de aplicación y no son consumidos por otros Services; este
componente es consumido por otra feature, de modo que es un Service independiente con interfaz e
implementación separadas.

**No expone endpoint HTTP.** SPEC-015 no crea Controller, DTO, Mapper ni ruta: la consulta de
portfolio pertenece a SPEC-014, que ya decidió su ruta y su contrato (CL-10, FR-028).

---

## 2. Interfaz

```java
package footballmarket.services;

/**
 * Calcula las posiciones financieras de un usuario a partir de sus operaciones
 * confirmadas y de la emisión inicial de tokens.
 */
public interface FinancialPositionService {

  /**
   * Devuelve las posiciones financieras del usuario, valoradas con la cotización vigente
   * disponible de cada jugador.
   *
   * @param userId usuario cuyas posiciones se calculan
   * @return posiciones del usuario, ordenadas por identificador de jugador
   */
  List<PlayerFinancialPosition> positionsOf(Long userId);
}
```

**Contrato:**

| Aspecto | Regla |
| --- | --- |
| Identidad | El `userId` la resuelve el consumidor desde la sesión autenticada; el componente no acepta identidades del cliente ni consulta carteras ajenas (FR-024) |
| Efectos | Ninguno. El método es de solo lectura: no escribe operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos (FR-023) |
| Límite transaccional | El método se declara `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`. Operaciones y emisiones se leen de la misma instantánea, de modo que la reconstrucción corresponde a un estado confirmado del mercado (R-15) |
| Coherencia con el consumidor | Si el consumidor invoca el componente **dentro de su propia transacción**, Spring no cambia el aislamiento de una transacción que ya participa: el aislamiento efectivo es el del consumidor. Para que la **respuesta completa** sea coherente —operaciones, emisiones, tenencias, reservas y posiciones de la misma respuesta—, el consumidor debe declarar su consulta con `REPEATABLE READ` (AC34 de SPEC-014) |
| Determinismo | Dos llamadas sobre el mismo estado de datos devuelven el mismo resultado, incluidos costo, resultados y porcentajes, con independencia del reloj de la consulta (FR-020, AC18) |
| Orden | Las posiciones se devuelven ordenadas por `playerId`, para que el resultado sea estable y comparable entre llamadas |
| Posiciones vacías | Un usuario sin operaciones ni emisiones devuelve una lista vacía, sin error (AC03 de SPEC-014, FR-036 de SPEC-014) |
| Ausencia de cotización | La posición existe y se devuelve; su resultado no realizado y su resultado total quedan **sin definir**, nunca en cero (FR-013, CL-05) |
| Fallo técnico de cotizaciones | El método **falla** y propaga el error; no devuelve datos financieros parciales, tampoco el resultado realizado (FR-014, CL-15) |
| Integridad | Si los datos no permiten reconstruir una posición de forma consistente, el método falla con la excepción de integridad del dominio (FR-025) |
| Coherencia precio-instante | `currentUnitPrice` y `quoteCalculatedAt` provienen de la **misma** cotización; sin cotización ambos son nulos. El contrato no exige modificar el puerto `QuoteSource` |
| Eficiencia | El número de consultas no crece con la cantidad de posiciones: dos consultas de conjunto y una adquisición de cotizaciones por jugador distinto, concentrada en un punto sustituible por una llamada masiva (R-17) |

---

## 3. Salida: `PlayerFinancialPosition`

Tipo de Model inmutable. El consumidor **no** recalcula ninguna de sus cifras.

| Figura | Tipo | Significado | Uso en SPEC-014 |
| --- | --- | --- | --- |
| `playerId` | `Long` | Jugador de la posición | Posición por jugador |
| `openQuantity` | `int` | Tokens abiertos poseídos | Cantidad poseída de la posición |
| `acquisitionCost` | `long` | **Costo de adquisición autoritativo**, en centésimas | Costo de adquisición de la posición (FR-013 de SPEC-014) |
| `averageUnitPrice` | `long` | Precio medio **derivado** de `acquisitionCost` y `openQuantity` | Precio promedio de compra (RF-03 del issue #20) |
| `totalPaid` | `long` | Costo pagado histórico acumulado, en centésimas | Denominador del porcentaje global (CL-08) |
| `realizedProfitLoss` | `long` | Resultado realizado acumulado, en centésimas | Resultado realizado de la posición |
| `currentUnitPrice` | `Long` | Cotización vigente por token; nula si no hay cotización | `currentUnitPrice` de SPEC-014 |
| `quoteCalculatedAt` | `OffsetDateTime` | Instante de cálculo de esa misma cotización; nulo si y solo si `currentUnitPrice` es nulo | `priceUpdatedAt` de SPEC-014 (FR-020, AC05) |
| `currentValue` | `Long` | `openQuantity × currentUnitPrice`; nula sin cotización | Valor actual de la posición |
| `unrealizedProfitLoss` | `Long` | `currentValue − acquisitionCost`; nula sin cotización | Resultado no realizado |
| `totalProfitLoss` | `Long` | `realized + unrealized`; nula sin cotización; con posición cerrada es el realizado acumulado | Resultado total (FR-032 de SPEC-015) |
| `isOpen` | `boolean` | `openQuantity > 0` | Estado `OPEN`/`CLOSED` de la posición |
| `openedAt` | `OffsetDateTime` | Instante de la adquisición que abrió el ciclo de tenencia vigente | `openedAt` de SPEC-014 (FR-030, AC26, AC28) |
| `closedAt` | `OffsetDateTime` | Instante de la venta que llevó la cantidad a cero; nula mientras la posición esté abierta | `closedAt` de SPEC-014 (FR-030, AC26, AC28) |
| `lastTransactionAt` | `OffsetDateTime` | Instante de la última operación confirmada del usuario sobre ese jugador | `lastTransactionAt` de SPEC-014 (FR-030, AC28) |
| `buyOperationCount` | `int` | Operaciones confirmadas en las que el usuario es comprador | `totalBuyTransactions` de la posición (FR-031, AC29) |
| `sellOperationCount` | `int` | Operaciones confirmadas en las que el usuario es vendedor | `totalSellTransactions` de la posición (FR-031, AC29) |

### 3.1 Qué debe consumir el consumidor y qué no

El consumidor **no** necesita reinterpretar el historial de operaciones: los hechos del ciclo de
tenencia, los recuentos por rol y el instante de la cotización llegan ya calculados en la posición,
procedentes del mismo recorrido de la historia confirmada y de la misma instantánea transaccional
(R-15, R-18). Tampoco necesita consultar `market_transactions` por su cuenta.

### 3.2 Reglas de consumo obligatorias para el consumidor

1. **El costo de adquisición es `acquisitionCost`**, no el producto del precio medio redondeado por la
   cantidad. El precio medio es informativo (CL-11, FR-006, FR-031).
2. **El precio medio no se usa para reconstruir el costo** en ningún caso.
3. **Una cifra nula no es cero**: la ausencia de cotización se propaga como ausencia, nunca como
   `0` (FR-013, CL-05).
4. **El resultado realizado no se reinicia** por cierre o reapertura de la posición: llega ya
   acumulado (FR-009, CL-16 de SPEC-014).
5. **El porcentaje global y los agregados contables NO se recalculan en el consumidor.** El
   `FinancialPositionCalculator` expone una operación reutilizable sobre el **conjunto completo** de
   posiciones ya calculadas que devuelve `historicalCost`, `investedCost`, `currentValue`,
   `realizedProfitLoss`, `unrealizedProfitLoss`, `totalProfitLoss`, `profitLossPercentage` y
   `valuationComplete` (R-19). El consumidor la aplica sobre el conjunto que recibe; jamás sobre una
   página.
6. **El denominador del porcentaje es `historicalCost`**, el costo pagado histórico acumulado, no
   `investedCost` —el costo de adquisición de las posiciones abiertas—, que es un importe distinto
   (CL-08, FR-027 de SPEC-014, AC31 de SPEC-014).
7. **Un valor indefinido se propaga como indefinido**: `unrealizedProfitLoss`, `totalProfitLoss` y
   `profitLossPercentage` son nulos si alguna posición abierta carece de cotización, y
   `valuationComplete` es `false` en ese caso. Ningún consumidor sustituye un nulo por cero.
8. **La emisión inicial no es una compra**: no incrementa `buyOperationCount` ni `sellOperationCount`,
   aunque aporta el instante de inicio del ciclo cuando el ciclo lo abrió ella (R-18, AC36 de
   SPEC-014).
9. **Los porcentajes por posición** —rendimiento no realizado y peso en el portfolio— **no** forman
   parte de este contrato: sus fórmulas pertenecen a SPEC-014 (FR-028, FR-029) y se calculan con las
   cifras por posición aquí expuestas.

---

## 4. Reglas contables que el componente materializa

El componente implementa `spec.md` sin reinterpretarlo. Las fórmulas con ejemplos verificados están
en [`../data-model.md`](../data-model.md) §3 y las decisiones de diseño en
[`../research.md`](../research.md).

| Regla | Origen |
| --- | --- |
| Costo promedio ponderado móvil por usuario y jugador | CL-01, FR-004 |
| Costo de adquisición autoritativo y precio medio derivado | CL-03, CL-11, FR-006, FR-031 |
| Costo atribuido a la venta con HALF_UP y residuo retenido en la posición abierta | CL-02, FR-007, FR-016, FR-017 |
| Resultado realizado acumulativo por usuario y jugador | CL-04, FR-008, FR-009 |
| Liquidación total a costo cero y reapertura con costo nuevo | FR-010, AC04, AC05 |
| Resultado no realizado y total sobre la cotización vigente | CL-05, CL-14, FR-012, FR-032 |
| Porcentaje global sobre el costo histórico total adquirido | CL-08 |
| Emisión inicial con costo cero y dilución del promedio | CL-07, CL-13, FR-021, FR-022 |
| Orden contable `(executed_at, id)` | CL-06, FR-018 |
| Imputación única por parte y rol | FR-003 |
| Publicación no ejecutada sin efecto contable | FR-011 |

---

## 5. Errores

| Situación | Excepción | Categoría | Comportamiento |
| --- | --- | --- | --- |
| Los datos no permiten reconstruir una posición de forma consistente | `FinancialPositionIntegrityException` | `DomainException` | Falla; no devuelve resultados financieros incorrectos ni incompletos (FR-025) |
| La cantidad vendida excede la cantidad abierta | `FinancialPositionIntegrityException` | `DomainException` | Ídem |
| Cantidad o importe no positivos en una operación | `FinancialPositionIntegrityException` | `DomainException` | Ídem: son invariantes ya establecidos por el dominio y la base de datos (R-16) |
| Comprador y vendedor idénticos en una operación | `FinancialPositionIntegrityException` | `DomainException` | Ídem: evita una doble imputación (FR-003) |
| Cantidad emitida distinta de la constante del dominio | `FinancialPositionIntegrityException` | `DomainException` | Ídem |
| Importe fuera del rango admitido | `FinancialPositionIntegrityException` | `DomainException` | Ídem |
| Fallo técnico de la fuente de cotizaciones | Excepción de integración | `IntegrationException` | Falla y propaga; no devuelve datos parciales (FR-014, CL-15) |

**El mapeo HTTP no pertenece a este contrato.** SPEC-015 no expone endpoint y no modifica
`GlobalExceptionHandler`. `docs/backend/architecture.md` §1.7 exige que todo error controlado use
`ErrorResponseDTO`, y el manejador global no tiene un caso genérico de `FootballMarketException`:
quien exponga estas excepciones en un endpoint debe mapearlas explícitamente. Ese mapeo corresponde
a SPEC-014 (riesgo RA-7 del plan).

---

## 6. Compatibilidad y evolución

- El contrato es **estable**: SPEC-014 consume la interfaz, no su implementación.
- La forma de obtener las cotizaciones es **interna** del componente: cuando REQ-S2-03 aporte el
  repositorio canónico, la adquisición puede pasar de por jugador a masiva **sin cambiar este
  contrato** ni el código del consumidor (R-07).
- El componente no depende de una implementación concreta de cotizaciones: consume el puerto
  `QuoteSource` ya vigente (G-3).
- No se agregan nuevas figuras contables sin actualizar antes `spec.md`: el contrato refleja la
  especificación, no la evolve.

---

## 7. Referencias

- Especificación funcional: [`../spec.md`](../spec.md)
- Fórmulas y ejemplos: [`../data-model.md`](../data-model.md)
- Decisiones de diseño: [`../research.md`](../research.md)
- Contrato del puerto de cotizaciones vigente:
  [`../../010-buy-tokens/contracts/quote-source.md`](../../010-buy-tokens/contracts/quote-source.md)

# Contrato del puerto de lectura de cotización: fuente real

**Feature**: `016-ejecutar-recalculo-manual-de-cotizaciones`
**Spec**: [`spec.md`](../spec.md) · **Research**: [`../research.md`](../research.md)

Este documento define la implementación real del puerto `QuoteSource` y la retirada de la fuente
simulada (FR-042, SC-011). El contrato original del puerto está en
[`specs/010-buy-tokens/contracts/quote-source.md`](../../010-buy-tokens/contracts/quote-source.md)
y se mantiene vigente salvo por la corrección indicada en §4.

---

## 1. Interfaz

Sin cambios de firma:

```java
package footballmarket.integrations;

public interface QuoteSource {
  Optional<CurrentQuote> currentQuote(Long playerId);
}
```

## 2. Implementación real

| Aspecto | Valor |
| --- | --- |
| Rol | `Integration` (adaptador de puerto), en `footballmarket.integrations` |
| Fuente de datos | Repositorio canónico real de cotizaciones: tabla `player_quotes` |
| Selección de la fila | Última fila del jugador por `effective_date DESC` |
| Ausencia | `Optional.empty()` cuando el jugador no tiene ninguna cotización |
| `price` | Precio de la fila en centésimas, siempre > 0 |
| `calculatedAt` | `calculated_at` de la fila: el instante de cálculo de esa cotización, no el de la lectura (FR-046) |
| `strategyVersion` | `strategy_version` de la fila; `null` en la fila inicial, que no tiene estrategia asociada (FR-020) |

La implementación no inventa datos: una cotización ausente se devuelve como ausencia y el mercado
la traduce al error `QUOTE_UNAVAILABLE` ya vigente (FR-045).

### Atribución de la estrategia

`CurrentQuote` transporta hoy la estrategia en un único campo `strategyVersion`. Con la fuente real
disponible, el precio vigente puede proceder de una fila inicial sin estrategia. Se conserva el
contrato del puerto sin ampliarlo: `strategyVersion` admite `null`, y `market_transactions.quote_strategy_version`
es nullable. La ampliación del DTO del puerto, si se requiriera el nombre de la estrategia, sería un
cambio de contrato del puerto y queda fuera de esta feature (FR-058).

## 3. Retirada de la fuente simulada

| Elemento | Acción |
| --- | --- |
| `SimulatedQuoteSource` | Se elimina del sistema (FR-042, SC-011) |
| `QuoteSimulationConfiguration` | Se elimina, incluida la selección condicional y el guard de arranque `quoteSourceAvailabilityGuard` |
| `market.quote.simulator.enabled` / `market.quote.simulator.price` | Se eliminan de `application.yml` |
| `MissingQuoteSourceException` | Se elimina: sin fuente simulada, la fuente real es la única y su ausencia es un fallo de construcción de la aplicación |

La selección de fuente en el arranque deja de existir: ya no hay dos candidatas, sino una
implementación real del puerto.

## 4. Corrección pendiente del contrato de SPEC-010

`specs/010-buy-tokens/contracts/quote-source.md` declara como **único** consumidor a
`BuyOrderServiceImpl`. Desde SPEC-011 también lo consume `TokenListingServiceImpl` para congelar el
precio al publicar una venta.

La descripción de consumidores debe corregirse **en el mismo cambio que introduce esta fuente
real**, conforme al principio de coherencia de contratos (Constitution §IV) y a la `spec.md` de esta
feature. La corrección es exclusivamente documental: no altera firmas, respuestas ni códigos de
error.

## 5. Consumidores

| Consumidor | Uso | Efecto del cambio |
| --- | --- | --- |
| `BuyOrderServiceImpl` | Precio vigente para la compra directa y para la compra de una publicación | Ninguno: la interfaz no cambia |
| `TokenListingServiceImpl` | Precio vigente congelado en la publicación de venta | Ninguno: la interfaz no cambia |

La comprobación de mercado inicializado (FR-043) se incorpora en ambos servicios **antes** de leer
la cotización y antes de cualquier movimiento de recursos.

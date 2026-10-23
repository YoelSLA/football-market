# Contrato de lectura de cotización: QuoteSource

La compra necesita el precio vigente de un token. SPEC-010 **no implementa** el cálculo, el historial ni la persistencia de cotizaciones: esa responsabilidad corresponde a REQ-S2-03.

Este documento define el puerto Integration que consume la compra, cómo se selecciona la fuente y cómo se retira el simulador.

---

## Naturaleza y ubicación

| Aspecto | Valor |
|---|---|
| Rol arquitectónico | **Integration**, conforme a `docs/backend/architecture.md` §1.1 y §1.3 |
| Ubicación | `footballmarket.integrations` |
| Consumidor | `BuyOrderServiceImpl` (compra) y `TokenListingServiceImpl` (publicación de venta, desde SPEC-011) |
| Carácter | **Permanente mientras desacople la compra de la implementación concreta** |
| Integración definitiva | Una implementación real del puerto consulta el repositorio canónico de REQ-S2-03 |

**No es un Service.** No expone un caso de uso propio, no tiene interfaz e implementación obligatorias de aplicación y solo lo consumen Services. Ubicarlo en `services/` lo convertiría en un Service y produciría una dependencia Service → Service, prohibida por §1.3.

Existe por una razón concreta: el repositorio canónico de REQ-S2-03 todavía no existe, y anticipar su tipo y nombre sería inventar un diseño ajeno. El puerto permite verificar la compra de forma aislada sin adivinar el contrato definitivo.

---

## Ciclo de vida

El puerto y el simulador tienen ciclos de vida distintos. Esta distinción evita la ambigüedad de tratar ambos como temporales.

| Componente | Ciclo de vida | Momento de su eliminación |
|---|---|---|
| `QuoteSource` | **Permanente** mientras siga siendo útil para desacoplar la compra | Eventual, mediante refactorización explícita si deja de aportar valor |
| `SimulatedQuoteSource` | **Temporal** | Cuando exista la integración real de REQ-S2-03 |
| Implementación real | Permanente | No se elimina |

### Por qué el puerto es permanente y el simulador no

El puerto cumple una función de **desacoplamiento**: mantiene la compra independiente de cómo se obtiene el precio. Mientras esa separación aporte claridad y estabilidad, se conserva.

El simulador cumple una función de **sustitución temporal**: cubre un hueco de implementación que se cerrará cuando REQ-S2-03 exista. Una vez cubierta, deja de tener propósito.

### Eliminación futura del puerto

Si en el futuro `QuoteSource` resultara redundante —por ejemplo, si la arquitectura del proyecto decidiera que la compra accede directamente al repositorio canónico sin puerto intermedio— su eliminación sería una **refactorización explícita y consciente**, no una consecuencia automática de la llegada de REQ-S2-03.

Esa refactorización no está prevista, aprobada ni planificada en esta SPEC. No se adelanta su diseño.

### Compatibilidad arquitectónica

Mantener el puerto no introduce ninguna dependencia prohibida:

| Dependencia | Regla | Veredicto |
|---|---|---|
| Service → Integration | Permitida por §1.3 | Compatible |
| Service → Service | Prohibida por §1.3 | No ocurre: la compra no invoca ningún Service de cotizaciones |
| Integración directa sin puerto | Permitida | Alternativa válida, no adoptada: el puerto mantiene la compra verificable hoy |

**La frontera pública de `BuyOrderService` no cambia** en ninguno de los dos escenarios.

---

## Contrato mínimo

```java
package footballmarket.integrations;

public interface QuoteSource {

    /**
     * Obtiene la cotización vigente de un jugador.
     *
     * @param playerId identificador interno del jugador
     * @return instantánea de la cotización vigente, o vacío si no existe
     */
    Optional<CurrentQuote> currentQuote(Long playerId);
}
```

La compra depende de este puerto, no de un Service de cotizaciones ni de una implementación concreta.

### Snapshot

| Campo | Tipo | Nulabilidad | Semántica |
|---|---|---|---|
| `playerId` | `Long` | Obligatorio | Jugador al que corresponde la cotización |
| `price` | `long` | Obligatorio | Precio de **un token**, en centésimas de crédito; debe ser mayor que cero |
| `calculatedAt` | `OffsetDateTime` | Obligatorio | Instante en que la cotización fue calculada y persistida, en UTC |
| `strategyVersion` | `String` | **admite `null`** | Estrategia que produjo esa cotización, cuando la fuente la proporcione |

El precio se expresa en centésimas conforme a CL-01: `100` representa 1.00 crédito.

`calculatedAt` es el instante de cálculo de la cotización, no el de la lectura ni el de la respuesta HTTP. Una cotización antigua sigue siendo válida mientras sea la última vigente.

### Nulabilidad de `strategyVersion`

`strategyVersion` admite `null`, y esa es la decisión vigente.

| Criterio | Resultado |
|---|---|
| ¿La SPEC exige versión de estrategia obligatoria? | No. Ningún FR ni AC de SPEC-010 la requiere |
| ¿El enunciado lo exige para esta SPEC? | No. La trazabilidad de estrategias pertenece al dominio de cotizaciones (REQ-S2-01/03) |
| ¿Coincide con la base de datos? | Sí: `quote_strategy_version VARCHAR(64)` es nullable en `market_transactions` |

**No se inventan versiones de estrategia.** Cuando la fuente real no proporcione una, el valor se conserva en `null` y la auditoría lo refleja así. El simulador, en cambio, **sí** completa el campo con una etiqueta que lo identifica como simulado, para que ningún dato de prueba pueda confundirse con una cotización real.

Esta correspondencia entre el `null` del puerto y la columna nullable de la auditoría es deliberada y no requiere migración.

---

## Invariante de correspondencia entre jugador y cotización

`QuoteSource` recibe un `playerId` y devuelve un `CurrentQuote` que también incluye `playerId`. Existe el riesgo de que una implementación devuelva la cotización de otro jugador.

**Invariante obligatoria**: `CurrentQuote.playerId` debe coincidir con el `playerId` solicitado.

| Aspecto | Decisión |
|---|---|
| Dónde se verifica | En `BuyOrderServiceImpl`, inmediatamente después de leer el snapshot |
| Por qué ahí | Es el punto donde ambos identificadores están disponibles y donde un incumplimiento se detecta antes de mover recursos |
| Qué ocurre si no coinciden | La compra falla como **incumplimiento interno del contrato de integración** |
| Respuesta pública | 500 `INTERNAL_ERROR` genérico, sin detalles técnicos |
| Efectos | Reversión completa: sin transferencias, sin saldos modificados y sin auditoría |
| Precio utilizado | **No se utiliza.** La verificación ocurre antes de cualquier cálculo |

No es un error de negocio del solicitante: el cliente pidió un jugador válido y la falla está en un componente interno. Exponer el detalle del incumplimiento revelaría información técnica y no corresponde al contrato público.

No se crea un servicio de validación de cotizaciones. La comprobación es una línea dentro de la fachada, que ya es responsable de validar las condiciones de la compra.

```java
CurrentQuote quote = quoteSource.currentQuote(playerId)
    .orElseThrow(QuoteUnavailableException::new);

if (!playerId.equals(quote.playerId())) {
  throw new QuoteContractViolationException();   // fallo interno, 500 genérico
}

if (quote.price() <= 0) {
  throw new InvalidQuotePriceException();
}
```

El orden importa: primero la correspondencia del jugador y después la validez del precio. Una cotización ajena no debe evaluarse como si fuera válida.

---

## Integración definitiva con REQ-S2-03

Cuando REQ-S2-03 exista, el **repositorio canónico** de cotizaciones alimenta este mismo puerto y `SimulatedQuoteSource` se elimina.

**No se define aquí el esquema de la tabla de cotizaciones.** SPEC-010 no crea tablas de cotizaciones, no inicializa precios y no calcula estrategias. Definir ese esquema duplicaría la responsabilidad de REQ-S2-03 y anticiparía requisitos fuera de alcance.

Requisitos que la integración real debe cumplir:

| Requisito | Motivo |
|---|---|
| Devolver la cotización más reciente del jugador | FR-007 |
| No devolver precio nulo ni cero | FR-007 y AC15 |
| Indicar el instante de cálculo y la estrategia | Trazabilidad de la cotización |
| Permitir distinguir "sin cotización" de "precio no positivo" | AC06 y AC15 son respuestas distintas |

La sustitución **no** requiere modificar el endpoint de compra, su contrato HTTP ni la frontera de la fachada.

---

## Selección de la fuente

La selección ocurre **una sola vez, en el arranque**, y nunca durante una compra.

| Estado del contexto | Fuente activa | Resultado |
|---|---|---|
| Existe fuente real de REQ-S2-03 | La fuente real | La compra usa la cotización vigente |
| No existe fuente real y el simulador está habilitado | Simulador | La compra usa el precio configurado, identificado como simulado |
| No existe fuente real y el simulador está deshabilitado | Ninguna | El arranque falla con un error de configuración explícito |

**Regla obligatoria**: cuando existe una fuente real, el simulador **no se registra**, aunque su propiedad esté habilitada. La selección se resuelve por presencia de la fuente real, no por propiedad.

---

## Simulador temporal

Mientras REQ-S2-03 no exista, `SimulatedQuoteSource` proporciona un precio fijo configurable.

| Propiedad | Defecto | Uso |
|---|---|---|
| `market.quote.simulator.enabled` | `false` | Activa el simulador solo donde se autorice |
| `market.quote.simulator.price` | `100` | Precio por token en centésimas; 100 = 1 crédito, según el valor inicial del enunciado |

### Restricciones obligatorias

| Restricción | Motivo |
|---|---|
| Desactivado por defecto | Evita desplegar precios simulados en un entorno real |
| Exclusivo de desarrollo y pruebas | No es una fuente de verdad funcional |
| No se registra si existe una fuente real | No puede desplazar la integración verdadera |
| No crea tablas ni datos persistentes | SPEC-010 no define el esquema de cotizaciones |
| No sustituye una cotización real ausente | Si hay fuente real y falta la cotización, responde 409 `QUOTE_UNAVAILABLE` |

### Dos ausencias distintas

| Situación | Origen | Resultado observable |
|---|---|---|
| REQ-S2-03 no implementado y simulador deshabilitado | Falta la implementación del dominio de cotizaciones | La aplicación no inicia, con error de configuración explícito |
| Fuente disponible sin cotización para un jugador | Ese jugador no tiene cotización vigente | 409 `QUOTE_UNAVAILABLE` |

El simulador **no** se activa ante ninguna de las dos ausencias. La primera impide el arranque; la segunda produce un rechazo controlado. Ninguna produce un precio inventado.

### Registro del precio utilizado

La auditoría registra el precio efectivamente aplicado en la compra. Cuando el precio proviene del simulador, la versión de estrategia queda identificada como simulada, de modo que ningún dato de prueba pueda confundirse con una cotización real.

---

## Comportamiento esperado en la compra

```java
// El precio se lee una sola vez y ese valor viaja a validación,
// movimientos de créditos y auditoría.
CurrentQuote quote = quoteSource.currentQuote(playerId)
    .orElseThrow(QuoteUnavailableException::new);

if (!playerId.equals(quote.playerId())) {
  throw new QuoteContractViolationException();   // 500 genérico, sin detalles
}

if (quote.price() <= 0) {
  throw new InvalidQuotePriceException();
}

long totalAmount = Math.multiplyExact(quantity, quote.price());
```

Un único valor de precio atraviesa toda la operación, de modo que importe, saldo debitado y registro no puedan divergir.

### Consistencia de los datos

| Regla | Verificación |
|---|---|
| `price` es `long` en centésimas | Consistente con `BIGINT` en la auditoría y con CL-01 |
| `price` es mayor que cero | Validado antes de cualquier cálculo; si no, 409 `INVALID_QUOTE_PRICE` |
| `calculatedAt` es el instante de cálculo | No el de lectura ni el de la respuesta |
| La cotización se lee una sola vez | Un único snapshot por compra; no se relee |
| El importe usa `Math.multiplyExact` | Aritmética exacta; el desbordamiento revierte la compra |
| El precio aplicado se conserva en la auditoría | `unit_price` y `quote_calculated_at` reflejan el snapshot usado |
| El simulador nunca hace de respaldo de la fuente real | La selección ocurre en el arranque; con fuente real el simulador no se registra |

---

## Estados distinguibles

| Estado | Momento | Resultado |
|---|---|---|
| Fuente real disponible | Compra | La compra utiliza la cotización vigente persistida |
| Simulador activo en desarrollo o pruebas | Compra | La compra utiliza el precio configurado, identificado como simulado |
| Sin fuente real y sin simulador | Arranque | La aplicación no inicia, con error de configuración explícito |
| Fuente disponible sin cotización del jugador | Compra | 409 `QUOTE_UNAVAILABLE` |
| Cotización con precio no positivo | Compra | 409 `INVALID_QUOTE_PRICE` |
| Cotización de otro jugador | Compra | 500 `INTERNAL_ERROR` genérico, con reversión completa |

---

## Verificación

| Escenario | Dónde se verifica |
|---|---|
| Correspondencia entre `playerId` solicitado y recibido | Compra, en la fachada |
| Precio mayor que cero | Compra, en la fachada |
| Ausencia de cotización | Compra, en la fachada |
| Registro del precio aplicado | Auditoría persistida |
| Selección de la fuente real por encima del simulador | Arranque del contexto de aplicación |
| Comportamiento del simulador en desarrollo y pruebas | Pruebas de la fuente de cotización |

---

## Responsabilidades

| Componente | Rol | Responsabilidad |
|---|---|---|
| REQ-S2-03 | Repositorio canónico | Calcular y persistir la cotización vigente |
| `QuoteSource` | Integration | Puerto de lectura que consume la compra; desacopla la compra de la implementación concreta |
| `SimulatedQuoteSource` | Integration | Proveer un precio fijo en desarrollo y pruebas; se elimina con REQ-S2-03 |
| `BuyOrderServiceImpl` | Service | Consumir el precio, verificar su correspondencia, aplicarlo y registrarlo en la auditoría |
| `TokenListingServiceImpl` | Service | Consumir el precio vigente y congelarlo en la publicación de venta (desde SPEC-011) |

SPEC-010 no calcula cotizaciones, no las persiste y no las valida. Solo las consume.
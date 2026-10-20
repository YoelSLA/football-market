# Implementation Plan: Definir reglas contables de costo promedio y resultados financieros (SPEC-015)

**Branch**: `feat/015-define-average-cost-and-financial-results-rules` | **Date**: 2026-10-20 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `specs/015-define-average-cost-and-financial-results-rules/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

SPEC-015 define la fuente de verdad contable del mercado de tokens: el costo de adquisición de las
posiciones de un usuario, el resultado realizado y el resultado no realizado, con reglas de
precisión, redondeo, orden determinista y tratamiento de la emisión inicial. La feature **no ejecuta
operaciones, no expone endpoint y no implementa la consulta de portfolio**: entrega las reglas y un
componente de cálculo reutilizable, en el dominio, que SPEC-014 consume sin reescribirlas.

El diseño técnico **reconstruye** el costo y los resultados desde las operaciones confirmadas
(`market_transactions`) y la emisión inicial (`player_token_emissions`), sin introducir una segunda
fuente de verdad persistida. Las reglas viven en **Model** (`PlayerFinancialPosition`,
`FinancialPositionCalculator`), que preserva la invariante de conservación exacta del costo; un
**Service** (`FinancialPositionService`) carga el conjunto de operaciones y emisiones y aplica la
valoración con la cotización vigente del backend. La unificación de la fuente del reloj de las
operaciones de mercado, hoy divergente entre la compra directa y la liquidación de publicaciones,
pertenece a esta feature y no altera ningún contrato público.

**Artefactos de diseño**: [`research.md`](research.md) (decisiones de Fase 0),
[`data-model.md`](data-model.md) (modelo y fórmulas),
[`contracts/financial-position-service.md`](contracts/financial-position-service.md) (contrato del
componente) y [`quickstart.md`](quickstart.md) (guía de validación).

## Technical Context

**Language/Version**: Java 21 (toolchain del backend; `backend/build.gradle.kts:86-87`)

**Primary Dependencies**: Spring Boot 4.1.1 · Spring Data JPA · Spring Security (JWT) · Flyway ·
PostgreSQL · Lombok · Gradle 9.6.0 (Kotlin DSL) · Testcontainers (PostgreSQL 18) · JUnit Jupiter,
Mockito y AssertJ. **Sin dependencias nuevas**: solo `java.math.BigDecimal` de la plataforma, usada
en el interior del Model para la división exacta.

**Storage**: PostgreSQL. Tablas existentes: `market_transactions` (operaciones confirmadas),
`player_token_emissions` (emisión inicial por jugador), `player_tokens`, `token_listings`,
`user_credits`. **Una migración nueva, solo de índice** (R-14). Sin tablas ni columnas de negocio
nuevas.

**Testing**: JUnit Jupiter con `@Nested` obligatorio y una etiqueta literal por clase contenedora
(`unit`, `lightweight`, `integration`, `e2e`), según `docs/backend/testing.md` §2, §16 y la sección de
clasificación por etiquetas. PostgreSQL real por Testcontainers para las categorías Repository y
Service. La ejecución de las pruebas corresponde al usuario.

**Target Platform**: Servidor Linux con JVM 21; base de datos PostgreSQL.

**Project Type**: Backend REST (Spring Boot) con consumidor frontend React.

**Performance Goals**: No se fijan umbrales de latencia sin mediciones reales (SPEC-014 CL-41 y
`tp/enunciado.md` §5.3). El objetivo cualitativo es que el cálculo se resuelva con un número
acotado de consultas de conjunto, sin accesos individuales por posición (FR-027).

**Constraints**: Solo lectura; no se altera el comportamiento observable de SPEC-010 ni de SPEC-011;
no se crean endpoints; los importes siguen siendo enteros en centésimas (CL-01); el usuario no
ejecuta ni una prueba sin su autorización.

**Scale/Scope**: Una feature de backend, tres componentes nuevos (dos de Model y uno de Service),
dos métodos de repositorio nuevos, una línea de código modificada en el servicio de compra, una
migración de índice y las pruebas de las categorías Model, Repository y Service.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / regla | Verificación | Resultado |
| --- | --- | --- |
| **§I Simplicidad y calidad** | Tres componentes nuevos, cada uno con una responsabilidad real; ninguna capa, interfaz ni abstracción por costumbre; sin dependencias nuevas; sin tabla de costo apurado | PASS |
| **§II Proporcionalidad del alcance** | Solo lo que exigen FR-001…FR-032; comisiones, impuestos y spreads excluidos (FR-029, CL-09); los casos **recomendados** y **opcionales** de `spec.md` no generan trabajo obligatorio | PASS |
| **§III Seguridad e integridad** | El cálculo es de solo lectura; opera por usuario y jugador; no expone datos de terceros (FR-024); no escribe saldos, tenencias ni operaciones (FR-023) | PASS |
| **§IV Coherencia de contratos** | No se modifica ningún contrato HTTP; el contrato público de `market_transactions` no cambia; la unificación del reloj preserva el significado de `executed_at` (FR-030); el nuevo contrato del componente queda documentado en `contracts/` | PASS |
| **§V Trazabilidad y fuente de verdad** | Todo importe se reconstruye desde operaciones confirmadas y emisión; no hay segunda fuente de verdad; la orden contable es determinista y auditable (FR-002, FR-018, FR-020) | PASS |
| **Flujo SDD §3 (autorizaciones)** | El plan no implementa, no habilita IMPLEMENT y no ejecuta pruebas; la autorización sigue siendo humana y explícita | PASS |
| **Flujo SDD §6 (Protocolo)** | El plan no redacta la especificación ni las tareas; no se avanzan fases | PASS |
| **AGENTS.md raíz (no tests)** | El plan describe pruebas; su ejecución y los resultados son del usuario | PASS |
| **AGENTS.md backend (build)** | El control permitido es `./gradlew build -x test` desde `backend/`, solo si se modifica código del área | PASS |

**Complejidad**: sin violaciones que justifiquen excepciones. El plan no introduce una cuarta
dependencia ni una capa adicional.

**Post-diseño (re-evaluación tras Fase 1)**: PASS en los mismos principios. Los artefactos de Fase 1
(data-model, contrato y quickstart) no introducen piezas adicionales: el contrato documenta
componentes ya decididos y el quickstart solo describe cómo validarlos.

## Project Structure

### Documentation (this feature)

```text
specs/015-define-average-cost-and-financial-results-rules/
├── spec.md              # Especificación funcional (SPECIFY + CLARIFY, lista para PLAN)
├── plan.md              # Este archivo (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/
│   └── financial-position-service.md   # Phase 1 output: contrato del componente reutilizable
├── checklists/
│   └── requirements.md  # Checklist de calidad de requisitos (16/16)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── models/
│   ├── PlayerFinancialPosition.java        # NUEVO: agregado contable por usuario y jugador
│   └── FinancialPositionCalculator.java     # NUEVO: plegado contable puro y porcentaje global
├── services/
│   ├── FinancialPositionService.java        # NUEVO: interfaz del caso de uso reutilizable
│   └── impl/
│       ├── FinancialPositionServiceImpl.java  # NUEVO: carga de operaciones, emisiones y cotizaciones
│       └── BuyOrderServiceImpl.java           # MODIFICADO: unificación del reloj (R-05), una línea
├── repositories/
│   ├── MarketTransactionRepository.java       # MODIFICADO: consulta de conjunto por usuario
│   └── PlayerTokenEmissionRepository.java     # MODIFICADO: consulta de emisiones por propietario
└── models/exceptions/                        # NUEVO: excepción de integridad contable

backend/src/main/resources/db/migration/
└── V<n>__index_player_token_emissions_owner.sql   # NUEVO: índice por owner_user_id (R-14)

backend/src/test/java/footballmarket/
├── models/
│   ├── PlayerFinancialPositionTest.java       # NUEVO: @Tag("unit")
│   └── FinancialPositionCalculatorTest.java   # NUEVO: @Tag("unit")
├── services/
│   └── FinancialPositionServiceTest.java      # NUEVO: @Tag("integration")
└── repositories/
    └── FinancialPositionPersistenceTest.java  # NUEVO: @Tag("integration")

docs/backend/architecture.md                   # MODIFICADO: §1.3 y nuevo §5 con los componentes de SPEC-015
```

**Sin cambios**: frontend, `controllers/`, `controllers/dtos/`, `controllers/mappers/`,
`integrations/QuoteSource`, `services/exceptions/` (la jerarquía de errores se reutiliza), y los
contratos públicos de SPEC-010 y SPEC-011.

**Structure Decision**: se conserva la estructura física existente del backend, que refleja los roles
arquitectónicos (`docs/architecture.md` §2.1). No se crean paquetes nuevos: los tres componentes nuevos
viven en los paquetes de su rol (`models`, `services`, `services.impl`) y la excepción en
`models/exceptions`, igual que las demás excepciones de dominio (`InvalidTokenQuantityException`,
`InvalidCreditAmountException`).

## Diseño técnico

### 1. Componente de cálculo financiero reutilizable

| Componente | Rol | Responsabilidad | Consumido por |
| --- | --- | --- | --- |
| `PlayerFinancialPosition` | Model | Agregado por usuario y jugador: cantidad abierta, costo de adquisición autoritativo, costo pagado histórico, resultado realizado, valoración opcional y los hechos del ciclo de tenencia, los recuentos por rol y el instante de la cotización (R-18). Preserva las invariantes mediante operaciones explícitas | `FinancialPositionCalculator`, `FinancialPositionService` |
| `FinancialPositionCalculator` | Model | Puro y estático: pliega operaciones confirmadas y emisiones en posiciones aplicando el orden contable; expone la agregación contable reutilizable del usuario sobre el conjunto completo de posiciones (R-19) | `FinancialPositionService` |
| `FinancialPositionService` + `Impl` | Service | Caso de uso reutilizable: resuelve el conjunto de operaciones del usuario, sus emisiones y las cotizaciones vigentes, y devuelve las posiciones valoradas | **SPEC-014** (mediante la interfaz) |

El componente **no expone endpoint propio** (CL-10, FR-028): es una fachada de aplicación, no un
contrato HTTP. Su contrato completo está en
[`contracts/financial-position-service.md`](contracts/financial-position-service.md).

El detalle de por qué el rol Service es el adecuado y qué alternativas se descartaron está en
[`research.md`](research.md) R-01.

### 2. Fuente de verdad contable

- **Operaciones confirmadas**: `market_transactions`, cuyo registro es append-only desde la aplicación
  y existe solo para operaciones confirmadas (`MarketTransaction.java:16-23`).
- **Emisión inicial**: `player_token_emissions`, que aporta la cantidad de tokens de costo cero y
  nunca una compra (R-06).
- **Nada más**: no hay tabla de costo apurado, ni columna de costo en `player_tokens`, ni caché
  contable persistida (R-02). Una eventual estructura apurada, si existiera, tendría que ser
  derivada y reconciliable, nunca una segunda fuente de verdad.

La cantidad abierta de cada posición **se deriva** de operaciones y emisión; el cálculo no consulta
`player_tokens`, con lo que no depende del estado mutable de las tenencias (R-08).

### 3. Modelo de costo promedio

Ciclo de vida de una posición, con `C` = costo de adquisición autoritativo, `q` = cantidad abierta y
`P` = costo pagado histórico:

| Evento | Cantidad abierta | Costo de adquisición | Resultado realizado | Costo pagado |
| --- | --- | --- | --- | --- |
| Emisión inicial (cantidad `n`, costo cero) | `q + n` | `C` | — | `P` |
| Compra confirmada (`qv` tokens, importe `A`) | `q + qv` | `C + A` | — | `P + A` |
| Venta confirmada (`qv` tokens, importe `I`) | `q − qv` | `C − round(C×qv/q)` | `+ I − round(C×qv/q)` | `P` |
| Liquidación total (`qv = q`) | `0` | `0` exacto | `+ I − C` | `P` |
| Compra posterior a una liquidación | `qv` | `A` (nuevo promedio) | se conserva el acumulado | `P + A` |

- **Costo de adquisición autoritativo**: `C` es la cifra que consume SPEC-014 (FR-006, CL-11).
- **Precio medio derivado**: `round(C / q)` con HALF_UP, solo informativo; nunca se recalcula `C`
  a partir de él (FR-031, R-12).
- **Cierre y reapertura**: la liquidación deja `C = 0` exacto y la compra posterior inicia un
  promedio nuevo, sin reiniciar el resultado realizado acumulado (FR-009, FR-010).
- **Tokens emitidos**: aportan costo cero y, si coexistieran con tokens adquiridos comercialmente,
  diluyen el promedio único (FR-022, CL-07, CL-13).

Además, el mismo plegado determina los **hechos del ciclo de tenencia**: `openedAt` (la adquisición
que abrió el ciclo vigente), `closedAt` (la venta que llevó la cantidad a cero, nulo mientras la
posición está abierta), `lastTransactionAt` (la última operación confirmada del usuario sobre ese
jugador) y los recuentos por rol `buyOperationCount` y `sellOperationCount`. La emisión inicial no
cuenta como compra ni como venta, aunque aporta el instante de inicio del ciclo cuando el ciclo lo
abrió ella (R-18). Las reglas de presentación de estas fechas pertenecen a SPEC-014 (FR-030, FR-031).

Las fórmulas completas, con ejemplos numéricos verificados, están en
[`data-model.md`](data-model.md).

### 4. Precisión monetaria

- Importes en **centésimas enteras** (`long`), conforme a CL-01; sin punto flotante en el dominio.
- Toda división se resuelve con `BigDecimal` en escala explícita y `RoundingMode.HALF_UP`, dentro
  del Model (R-03).
- **Conservación exacta**: `costo pagado = Σ costos atribuidos + costo de adquisición`, sin diferencias
  de una centésima (FR-017, SC-002).
- **Residuo retenido**: la diferencia entre el costo atribuido ideal y su valor redondeado queda en
  el costo de la posición abierta; al liquidarse, se incorpora al resultado realizado (CL-02).
- **Integridad aritmética**: la división se calcula en `BigDecimal` para evitar desbordamiento de
  `long`; la conversión de vuelta a `long` se hace de forma exacta y un valor fuera de rango se
  traduce a la excepción de integridad, no a un importe truncado.

### 5. Resultados financieros

| Figura | Definición | Cuando no está definida |
| --- | --- | --- |
| Resultado realizado | `Σ (importe de venta − costo atribuido)` sobre ventas confirmadas, acumulativo por usuario y jugador | Nunca: es siempre definible |
| Valor actual | `cantidad abierta × cotización vigente` | Sin cotización vigente |
| Resultado no realizado | `valor actual − costo de adquisición` | Sin cotización vigente |
| Resultado total | `realizado + no realizado` | Sin cotización vigente; en posición cerrada es el realizado acumulado (FR-032, CL-14) |
| Porcentaje global de resultado | `(Σ realizado + Σ no realizado) / historicalCost`, en puntos porcentuales con dos decimales, donde `historicalCost` es el costo pagado histórico acumulado | Denominador cero o alguna posición abierta sin cotización (CL-08) |

La agregación contable del usuario —`historicalCost`, `investedCost`, `currentValue`,
`realizedProfitLoss`, `unrealizedProfitLoss`, `totalProfitLoss`, `profitLossPercentage` y
`valuationComplete`— es una **operación reutilizable del `FinancialPositionCalculator` sobre el
conjunto completo de posiciones** (R-19). El consumidor la aplica sobre las posiciones que ya recibe:
no recalcula fórmulas contables, no consulta `market_transactions` por su cuenta y jamás la aplica
sobre una página. `historicalCost` y `investedCost` son importes distintos: el primero es el
denominador de CL-08 e incluye los ciclos cerrados; el segundo es el costo de adquisición de las
posiciones abiertas.

- **Ausencia de cotización**: no equivale a cero; deja el resultado no realizado y el total sin
  definir y conserva el resultado realizado (FR-013, CL-05, SC-007).
- **Fallo técnico de la fuente**: se propaga sin devolver ningún dato financiero de la posición,
  tampoco el resultado realizado (FR-014, CL-15).
- **Lectura de solo lectura**: no altera operaciones, tenencias, reservas, publicaciones, cotizaciones
  ni saldos (FR-023).

### 5bis. Coherencia transaccional de la reconstrucción

La reconstrucción **debe ejecutarse** dentro de una única transacción de solo lectura con aislamiento
`REPEATABLE READ` (R-15):

| Aspecto | Decisión |
| --- | --- |
| Límite transaccional | El método del Service es `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)` |
| Instantánea | Fijada en la primera sentencia: operaciones, emisiones y —en el consumidor— tenencias y reservas proceden del mismo estado confirmado del mercado |
| Consistencia con las operaciones de mercado | Una compra o una venta confirma su registro de auditoría y sus movimientos en la misma transacción, así que la reconstrucción siempre corresponde a un estado real, nunca a una combinación imposible |
| Operación concurrente | Una operación confirmada durante el cálculo no aparece en la instantánea: el resultado corresponde al estado previo, que es un estado válido |
| Relación con SPEC-014 | Spring no cambia el aislamiento de una transacción que ya participa: si SPEC-014 invoca el componente dentro de su propia transacción, **debe declarar esa transacción en `REPEATABLE READ`** para que la respuesta completa sea coherente (AC34 de SPEC-014) |
| Interbloqueos | No se usan bloqueos: son la herramienta del orden global de SPEC-011 para escrituras, no para una lectura |

Consecuencia favorable: bajo una instantánea única, la cantidad reconstruida y la cantidad de
`player_tokens` que lee SPEC-014 son comparables, porque provienen del mismo estado (riesgo RA-5
del plan).

### 5ter. Integridad contable

El Model **rechaza** con `FinancialPositionIntegrityException` (`DomainException`) las entradas que
vulneran invariantes ya establecidos, sin añadir reglas de negocio nuevas (R-16):

| Guarda detectada | Origen del invariante |
| --- | --- |
| Cantidad no positiva en una operación | `CHECK (quantity > 0)` de `market_transactions` y `token_listings` |
| Importe no positivo | `CHECK (unit_price > 0)` y `total_amount = quantity * unit_price` |
| Venta por encima de la cantidad abierta | Invariante de la posición: no se puede vender lo que no se posee |
| Comprador y vendedor idénticos en una operación | FR-003: una imputación por parte y rol |
| Cantidad emitida distinta de la constante del dominio | `CHECK (initial_quantity = 100)` y `PlayerTokenEmission.EMITTED_TOKENS` |
| Importe fuera del rango admitido | Invariante de representación de SPEC-010 FR-022 |

Ninguna de estas guardas define qué es una operación válida: **detectan lo que el dominio ya declara
imposible**. El cálculo nunca devuelve una posición ni un resultado con cifras inconsistentes
(FR-025): falla.

### 6. Orden contable determinista y unificación del reloj

- **Orden**: `(executed_at ASC, id ASC)`, impuesto por el Model; la consulta de persistencia ordena
  por las mismas columnas (R-04).
- **Unificación del reloj**: `BuyOrderServiceImpl` registra el `executed_at` de la compra directa con
  el reloj inyectado `marketClock`, como ya hace la liquidación de publicaciones. Es un cambio de
  una línea que **no altera el contrato público** de SPEC-010: `executed_at` sigue siendo el instante
  de confirmación (R-05, FR-019, FR-030).
- **Efecto colateral favorable**: el reloj de mercado queda controlable en pruebas, de modo que el
  orden contable es verificable con `executed_at` congelado.

### 7. Integración con el mercado

Componentes existentes que se reutilizan sin modificación:

| Componente | Uso en SPEC-015 |
| --- | --- |
| `MarketTransaction` | Fuente de las operaciones confirmadas: comprador, vendedor, jugador, cantidad, importe e instante de confirmación |
| `PlayerTokenEmission` | Fuente de la cantidad emitida de costo cero |
| `QuoteSource` / `CurrentQuote` | Fuente de la cotización vigente, sin atarse a su implementación |
| `MarketTransactionRepository`, `PlayerTokenEmissionRepository` | Acceso a las operaciones y emisiones, con dos consultas de conjunto nuevas |
| `GlobalExceptionHandler` | **No se toca**: SPEC-015 no expone endpoint; el mapeo HTTP de sus excepciones corresponde a SPEC-014 (R-09) |
| `MarketTimeConfiguration` | Reloj `marketClock` que la compra directa pasa a usar |

Componentes nuevos: `PlayerFinancialPosition`, `FinancialPositionCalculator`,
`FinancialPositionService` + implementación y `FinancialPositionIntegrityException`.

**Consumo por SPEC-014.** SPEC-014 obtiene de este componente las cifras contables por posición
(costo de adquisición, precio medio, valor actual, resultados, ciclo de tenencia, recuentos por rol
e instante de cotización) y la agregación contable del usuario. No necesita consultar
`market_transactions` ni reinterpretar el historial, y no duplica ninguna regla contable: el
porcentaje global y la semántica de los resultados indefinidos se aplican desde
`FinancialPositionCalculator`. Lo que SPEC-014 conserva son sus datos no contables —tenencias,
reservas, datos descriptivos del jugador— y la composición de su respuesta: campos, formato,
porcentajes por posición, paginación, filtros y orden.

### 8. Estrategia de consultas

| # | Consulta | Cardinalidad | Soporte |
| --- | --- | --- | --- |
| 1 | Operaciones del usuario como comprador **o** vendedor, ordenadas por `(executed_at, id)` | 1 por usuario | Índices existentes `idx_market_transactions_buyer` y `idx_market_transactions_seller`; recorrido inverso para orden ascendente |
| 2 | Emisiones del propietario | 1 por usuario | **Índice nuevo** `player_token_emissions (owner_user_id)` (R-14) |
| 3 | Cotización vigente | 1 por **jugador distinto** | Puerto `QuoteSource`; adquisición concentrada en un punto sustituible por una llamada masiva (R-07) |

**El número de consultas no crece con la cantidad de posiciones.** El Service **determina primero el
conjunto distinto de jugadores** a partir de operaciones y emisiones y solo después adquiere las
cotizaciones de ese conjunto, de modo que la adquisición queda aislada y puede pasar de N llamadas a
una cuando exista el repositorio canónico de REQ-S2-03, sin cambiar el contrato (R-17).

**Orden por jugador.** El orden contable es relevante **por jugador**, que es la granularidad de las
posiciones; no hace falta un orden global entre jugadores distintos. Las consultas de operaciones y
emisiones ya son agregadas: no hay ninguna consulta por posición.

**Sin optimizaciones prematuras.** No se añaden índices sobre `market_transactions` —los actuales
cubren la consulta— ni una API paginada del Service: ningún consumidor la pide y SPEC-014 necesita el
conjunto completo para su resumen global. La cantidad de posiciones está acotada por los jugadores
con operaciones o emisiones; para el superusuario coincide con el tamaño del catálogo emitido, y la
paginación es responsabilidad de SPEC-014 (FR-005, FR-035 de SPEC-014).

Las tenencias y las reservas **no** se consultan: no pertenecen al cálculo contable y las resuelve
SPEC-014 con su propio acceso agregado (FR-027, SPEC-014 AC35).

**El consumidor tampoco consulta `market_transactions`.** Los hechos del ciclo de tenencia, los
recuentos por rol y el instante de la última operación llegan calculados en la posición (R-18), y la
agregación contable se aplica sobre las posiciones devueltas (R-19). Ninguna necesidad de SPEC-014
reintroduce una consulta duplicada ni una segunda interpretación del historial.

### 9. Estrategia de testing

Detalle en [`research.md`](research.md) R-13 y en [`quickstart.md`](quickstart.md). Resumen:

| Categoría | Contenido | Etiqueta |
| --- | --- | --- |
| Model | Promedio ponderado móvil, atribución proporcional, HALF_UP, conservación exacta, residuo retenido, liquidación a cero, reapertura con promedio nuevo, emisión con costo cero y dilución, resultado realizado negativo, valoración indefinida y porcentaje global | `unit` |
| Repository | Consulta de operaciones por usuario en ambos roles y su orden; consulta de emisiones por propietario; presencia del índice nuevo | `integration` |
| Service | Posiciones de un usuario con operaciones y emisión; cotización presente y ausente; propagación del fallo técnico; idempotencia y ausencia de escrituras | `integration` |
| Service (regresión) | La compra directa registra `executed_at` desde el reloj inyectado | `integration` |
| Service (coherencia) | Una operación de mercado confirmada durante el cálculo no altera la instantánea: el resultado corresponde al estado previo | `integration` |
| Service (integridad) | Una venta que excede la cantidad abierta, una cantidad no positiva o un importe incoherente hacen fallar el cálculo | `integration` |
| Model (ciclo) | `openedAt` del ciclo vigente, `closedAt` al liquidar, reapertura con ciclo nuevo y resultado realizado acumulado | `unit` |
| Model (hechos) | Recuentos por rol: la emisión no cuenta como compra ni como venta | `unit` |
| Model (cotización) | Precio e instante de la misma cotización; ambos nulos sin cotización | `unit` |
| Model (agregación) | Agregación sobre el conjunto completo: denominador histórico, nulos por posición sin cotización y porcentaje global | `unit` |

No hay pruebas de Controller: la feature no expone endpoint y la categoría no aplica.

### 10. Riesgos y decisiones arquitectónicas

| Riesgo | Descripción | Mitigación |
| --- | --- | --- |
| **RA-1 Matriz de dependencias** | La relación Service → Service entre SPEC-015 y SPEC-014 no figura en la matriz de `docs/backend/architecture.md` §1.3, aunque el código de SPEC-012 ya la práctica | Documentar la relación en §1.3 y en un nuevo §5 de la arquitectura del backend, siguiendo el precedente de §3 y §4. **Requiere validación humana en la revisión del plan** |
| **RA-2 Fuente de cotizaciones por jugador** | El puerto vigente solo permite consultar un jugador por vez, mientras SPEC-014 AC35 exige un acceso agregado a cotizaciones | El Service encapsula la adquisición; el contrato no cambia cuando REQ-S2-03 aporte el repositorio canónico. El coste actual es nulo con el simulador de precio constante |
| **RA-3 Divergencia de `executed_at` entre vías** | Mientras la unificación no se aplique, el instante de confirmación de la compra directa no es controlable en pruebas | La unificación es parte del alcance (R-05) y tiene regresión propia |
| **RA-4 Precisión de SPEC-014** | El texto de CL-03 de SPEC-014 afirma que las ventas no modifican el precio medio restante; con el residuo retenido eso solo es exacto en aritmética fraccionaria | Corregir en el origen, en la especificación de SPEC-014, que pertenece a otra feature y otra rama (R-12) |
| **RA-5 Divergencia entre reconstruido y tenencias** | La cantidad abierta se deriva de operaciones y emisión; un estado de tenencias inconsistente produciría una divergencia que este cálculo no detecta | Fuera del alcance de SPEC-015: el cálculo no observa `player_tokens`. Bajo una instantánea única (RA-6) la cantidad reconstruida y la de `player_tokens` son comparables, de modo que SPEC-014 puede contrastarlas; se registra la observación en `data-model.md` |
| **RA-6 Coherencia entre lecturas concurrentes** | Con `READ COMMITTED`, operaciones y emisiones podrían leerse de instantáneas distintas y la reconstrucción no correspondería a ningún estado válido | El cálculo se ejecuta en una transacción de solo lectura `REPEATABLE READ` (R-15). **Requisito de integración**: SPEC-014 debe declarar su consulta de portfolio con el mismo aislamiento para que la respuesta completa sea coherente (AC34 de SPEC-014) |
| **RA-7 Excepciones sin mapeo HTTP** | `GlobalExceptionHandler` no tiene manejador genérico de `FootballMarketException` | SPEC-015 no expone endpoint y no toca el manejador; el mapeo de sus excepciones es tarea de SPEC-014 (R-09) |
| **RA-8 Volumen de la reconstrucción** | El plegado es lineal sobre las operaciones del usuario | El coste es proporcional al historial del usuario y no al número de posiciones; sin consultas por posición (R-17) |
| **RA-9 Acceso a cotizaciones por jugador** | El único acceso que crece con la cantidad de posiciones es el de cotizaciones, por la granularidad del puerto vigente | Adquisición concentrada y sustituible por una llamada masiva cuando exista REQ-S2-03, sin cambio de contrato (R-07, R-17) |
| **RA-10 Tamaño del catálogo emitido** | La posición del superusuario abarca un jugador por cada emisión, es decir, el tamaño del catálogo | La cantidad de posiciones es acotada y la paginación es responsabilidad de SPEC-014; el cálculo no añade una API paginada prematura (R-17) |
| **RA-11 Hechos derivados y su regla nominal** | `openedAt`, `closedAt`, `lastTransactionAt` y los recuentos por rol son hechos expuestos por SPEC-015 cuya regla de presentación nominal vive en SPEC-014 (FR-030, FR-031) | Se exponen **hechos**, no reglas de presentación; el contrato delimita expresamente qué pertenece a cada feature. Si SPEC-014 cambiara su definición, el Model debe alinearse: acoplamiento declarado y acotado |
| **RA-12 Agregación aplicada sobre un subconjunto** | Un consumidor podría aplicar la agregación sobre una página y obtener un porcentaje incoherente | El contrato exige la aplicación sobre el **conjunto completo** y prohíbe el cálculo sobre posiciones paginadas (R-19); las pruebas de Model cubren la agregación sobre el conjunto completo |

Dependencias técnicas: SPEC-010 y SPEC-011 (dominio de mercado, registros financieros append-only,
emisión inicial, representación en centésimas) y REQ-S2-03 (fuente definitiva de cotizaciones, hoy
sustituida por el simulador). SPEC-014 es consumidor, no dependencia.

## Archivos que deberán modificarse durante IMPLEMENT

| Archivo | Naturaleza | Alcance |
| --- | --- | --- |
| `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` | Nuevo | Agregado contable con sus invariantes, sus operaciones explícitas, los hechos del ciclo de tenencia, los recuentos por rol y el instante de la cotización (R-18) |
| `backend/src/main/java/footballmarket/models/FinancialPositionCalculator.java` | Nuevo | Plegado contable y agregación contable reutilizable del usuario (R-19) |
| `backend/src/main/java/footballmarket/services/FinancialPositionService.java` | Nuevo | Interfaz del caso de uso |
| `backend/src/main/java/footballmarket/services/impl/FinancialPositionServiceImpl.java` | Nuevo | Implementación: carga de conjunto, valoración y límite transaccional de solo lectura `REPEATABLE READ` |
| `backend/src/main/java/footballmarket/models/exceptions/FinancialPositionIntegrityException.java` | Nuevo | Error de integridad contable (dominio): guardas de R-16 |
| `backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java` | Modificado | Una consulta de conjunto por usuario, ordenada |
| `backend/src/main/java/footballmarket/repositories/PlayerTokenEmissionRepository.java` | Modificado | Una consulta de emisiones por propietario |
| `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` | Modificado | Una línea: `executed_at` de la compra directa desde `marketClock` |
| `backend/src/main/resources/db/migration/V<n>__index_player_token_emissions_owner.sql` | Nuevo | Índice `(owner_user_id)` |
| `docs/backend/architecture.md` | Modificado | §1.3 y nuevo §5 con los componentes de SPEC-015 y la relación autorizada |
| `backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java` | Nuevo | Categoría Model |
| `backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java` | Nuevo | Categoría Model |
| `backend/src/test/java/footballmarket/services/FinancialPositionServiceTest.java` | Nuevo | Categoría Service |
| `backend/src/test/java/footballmarket/repositories/FinancialPositionPersistenceTest.java` | Nuevo | Categoría Repository |
| `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` | Modificado | Regresión de la unificación del reloj |

No se modifican el frontend, los controllers, los DTO, los mappers, el puerto `QuoteSource` ni los
contratos públicos de SPEC-010 y SPEC-011.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones. No hay cuarto proyecto, ni patrón adicional, ni capa de compatibilidad: la feature
se apoya en el dominio de mercado existente y añade solo los tres componentes que la regla contable
exige.

## Verification de alineación con SPEC-015

| Requisito o decisión | Cómo lo cubre el plan |
| --- | --- |
| FR-001…FR-032 | Cada requisito funcional tiene su mecanismo en el diseño: modelo de costo (§3 del diseño), precisión (§4), resultados (§5), orden (§6), fuente de verdad (§2) y consultas (§8) |
| AC01…AC22 | Cubren el comportamiento verificable de las historias 1, 2 y 3; los casos AC16–AC20 (trazabilidad, determinismo, orden y conservación) tienen regresión propia en las categorías Model y Repository |
| CL-01…CL-16 | Sin modificar: el plan implementa la especificación tal como está |
| FR-025 (integridad) | Excepción de integridad del dominio; sin datos financieros incorrectos |
| FR-027 (sin accesos por posición) | Dos consultas de conjunto más las cotizaciones encapsuladas |
| FR-028 (reutilizable) | Service independiente con interfaz, documentado en `contracts/` |
| FR-029 (sin comisiones) | El modelo no contempla comisiones, impuestos ni spreads |
| CL-08 (porcentaje global) | La agregación contable del `FinancialPositionCalculator` lo calcula sobre el conjunto completo, con el denominador histórico y la semántica de indefinidos (R-19) |
| Coherencia con SPEC-014 | La revisión cruzada se resolvió sin modificar ningún requisito: se amplió el contrato de dos tipos de Model ya previstos (R-18, R-19) |
| FR-030 (no altera SPEC-010/011) | La unificación del reloj es interna y preserva el significado de `executed_at` |

## Estado de Git

- Rama: `feat/015-define-average-cost-and-financial-results-rules`, con `origin/dev` integrado
  (0 commits atrás) y el PR documental **#66** abierto hacia `dev`.
- `dev` integrado: el merge de `origin/dev` (PR #65 de SPEC-013) está incorporado en la rama.
- Los artefactos de este plan están **sin commitear**: el plan no hace commit ni push sin
  autorización.

## Siguiente paso

El plan habilita la fase de **Checklists** evaluados y, después, `tasks.md`. No habilita IMPLEMENT:
la autorización de implementación sigue siendo un gate humano explícito y vigente.

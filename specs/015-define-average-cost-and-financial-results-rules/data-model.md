# Modelo de datos — SPEC-015: reglas contables de costo promedio y resultados financieros

**Feature**: SPEC-015 · **Rama**: `feat/015-define-average-cost-and-financial-results-rules` · **Fecha**: 2026-10-20
**Plan**: [`plan.md`](plan.md) · **Research**: [`research.md`](research.md)

Este documento describe el modelo de la regla contable: los tipos de dominio nuevos, las fórmulas
exactas con ejemplos verificados, las consultas de conjunto y qué **no** se persiste.

---

## 1. Persistencia: qué cambia y qué no

### 1.1 Sin modelo de negocio nuevo

No se crea ninguna tabla, columna, vista materializada ni caché contable. La fuente de verdad es
`market_transactions` junto con `player_token_emissions` (R-02).

| Tabla | Uso en el cálculo | Cambia |
| --- | --- | --- |
| `market_transactions` | Operaciones confirmadas: comprador, vendedor, jugador, cantidad, importe unitario, importe total, instante de confirmación e identificador | No |
| `player_token_emissions` | Emisión inicial por jugador: propietario, cantidad emitida e instante de emisión | No |
| `player_tokens` | **No se consulta**: la cantidad abierta se deriva de operaciones y emisión | No |
| `token_listings` | **No se consulta**: las publicaciones no liquidadas no son operaciones | No |
| `user_credits` | **No se consulta**: el saldo no interviene en la regla contable | No |

### 1.2 Única migración: un índice

`player_token_emissions` tiene la clave primaria en `player_id` y `owner_user_id` es clave foránea
**sin índice** (`V10__create_market_domain.sql:54-59`). La consulta de emisiones del propietario lo
recorrería completo, así que la migración añade el índice:

```sql
CREATE INDEX idx_player_token_emissions_owner ON player_token_emissions (owner_user_id);
```

Es un índice, no una fuente de verdad: no almacena verdad contable y no altera ninguna invariante.
Para `market_transactions` **no hace falta índice nuevo**: ya existen
`idx_market_transactions_buyer (buyer_id, executed_at DESC, id DESC)` y
`idx_market_transactions_seller (seller_id, executed_at DESC, id DESC)`, y PostgreSQL puede
recorrerlos en sentido inverso para la ordenación ascendente.

### 1.3 Consultas de conjunto nuevas

| Repositorio | Método | Propósito |
| --- | --- | --- |
| `MarketTransactionRepository` | Operaciones del usuario como comprador **o** vendedor, ordenadas por `(executed_at, id)` ascendente | Reconstrucción del costo y del resultado |
| `PlayerTokenEmissionRepository` | Emisiones del propietario | Cantidad emitida de costo cero |

Ninguna consulta se resuelve por posición.

---

## 2. Tipos de dominio nuevos (Model)

### 2.1 `PlayerFinancialPosition`

Agregado inmutable por **usuario y jugador** que resume la posición financiera. Presenta el estado
contable y ofrece operaciones explícitas que preservan las invariantes
(`docs/backend/architecture.md` §1.5). No utiliza `Builder` ni `@Setter`
(`docs/backend/conventions.md`).

| Atributo | Tipo | Significado | Invariante |
| --- | --- | --- | --- |
| `playerId` | `Long` | Jugador de la posición | No nulo |
| `openQuantity` | `int` | Tokens abiertos poseídos | `≥ 0` |
| `acquisitionCost` | `long` | **Costo de adquisición autoritativo**, en centésimas | `≥ 0` |
| `totalPaid` | `long` | Costo pagado histórico acumulado, en centésimas | `≥ acquisitionCost`; nunca disminuye |
| `realizedProfitLoss` | `long` | Resultado realizado acumulado, en centésimas | Puede ser negativo |
| `currentUnitPrice` | `Long` | Cotización vigente por token, en centésimas | Nulo o `> 0` |
| `quoteCalculatedAt` | `OffsetDateTime` | Instante de cálculo de esa misma cotización | Nulo si y solo si `currentUnitPrice` es nulo |
| `openedAt` | `OffsetDateTime` | Instante de la adquisición que abrió el ciclo de tenencia vigente | No nulo en una posición existente |
| `closedAt` | `OffsetDateTime` | Instante de la venta que llevó la cantidad a cero | Nulo mientras la posición esté abierta |
| `lastTransactionAt` | `OffsetDateTime` | Instante de la última operación confirmada del usuario sobre ese jugador | No nulo en una posición con operaciones |
| `buyOperationCount` | `int` | Operaciones confirmadas en las que el usuario es comprador | `≥ 0`; la emisión no cuenta |
| `sellOperationCount` | `int` | Operaciones confirmadas en las que el usuario es vendedor | `≥ 0` |

Operaciones de dominio:

| Operación | Efecto | Regla |
| --- | --- | --- |
| Emisión (`issued`) | `openQuantity += n`; el costo no cambia | La emisión aporta cantidad y nunca costo (FR-021) |
| Compra (`purchase`) | `openQuantity += qv`; `acquisitionCost += A`; `totalPaid += A` | `A = qv × precio unitario` de la operación confirmada |
| Venta (`sale`) | `openQuantity −= qv`; `acquisitionCost −= atribuido`; `realizedProfitLoss += I − atribuido` | `atribuido = round(C×qv/q)`; exige `qv ≤ q` |
| Valoración (`valuedAt`) | Fija `currentUnitPrice` y `quoteCalculatedAt` **de la misma cotización** | Precio positivo con su instante, o ambos nulos (ausencia) |

Cifras derivadas:

| Cifra | Identificador | Fórmula | Indefinida cuando |
| --- | --- | --- | --- |
| Precio medio de adquisición | `averageUnitPrice` | `round(C / q)` con HALF_UP | `q = 0` |
| Valor actual | `currentValue` | `q × precio` | Sin cotización |
| Resultado no realizado | `unrealizedProfitLoss` | `valor actual − C` | Sin cotización |
| Resultado total | `totalProfitLoss` | `realizado + no realizado` | Sin cotización; con `q = 0` es el realizado acumulado (FR-032) |
| Posición abierta | `isOpen` | `openQuantity > 0` | Nunca |

La **cifra autoritativa** es `acquisitionCost`. El precio medio es informativo y **nunca** se usa
para recalcular el costo (CL-11, FR-006, FR-031).

**Hechos del ciclo de tenencia.** `openedAt`, `closedAt`, `lastTransactionAt` y los recuentos por rol
son hechos del mismo recorrido del historial confirmado, no reglas propias (R-18):

| Hecho | Origen |
| --- | --- |
| `openedAt` | Instante de la compra confirmada que abrió el ciclo vigente; si el ciclo lo abrió la emisión inicial, `emitted_at` |
| `closedAt` | Instante de la venta confirmada que llevó la cantidad a cero; nulo mientras la posición esté abierta |
| `lastTransactionAt` | Instante de la última operación confirmada en la que el usuario es parte |
| `buyOperationCount` / `sellOperationCount` | Recuento de operaciones confirmadas por rol, una vez cada una, con independencia de la cantidad |

La emisión inicial **no** se contabiliza como compra ni incrementa `buyOperationCount`, aunque sí
proporciona el instante de inicio del ciclo cuando el ciclo lo abrió ella (R-18, AC36 de SPEC-014).

### 2.2 `FinancialPositionCalculator`

Componente de Model, puro y estático, sin dependencias hacia fuera del Model.

| Entrada | Origen |
| --- | --- |
| Operaciones confirmadas del usuario, en cualquier orden de entrada | `market_transactions` |
| Emisiones del propietario | `player_token_emissions` |

| Salida | Contenido |
| --- | --- |
| Posiciones | Una por jugador con operaciones o emisión, ordenadas por `playerId` para un resultado estable |
| Agregación contable del usuario | Operación reutilizable sobre el **conjunto completo** de posiciones, que devuelve `historicalCost`, `investedCost`, `currentValue`, `realizedProfitLoss`, `unrealizedProfitLoss`, `totalProfitLoss`, `profitLossPercentage` y `valuationComplete` (R-19) |

Responsabilidades:

1. **Imponer el orden contable** `(executed_at ASC, id ASC)` sobre las operaciones antes de plegarlas
   (FR-018). La consulta ordena por las mismas columnas como optimización; la regla vive aquí
   (R-04).
2. Aplicar cada operación según el rol del usuario: **compra** si es comprador, **venta** si es
   vendedor, una sola imputación por parte (FR-003).
3. Aplicar las emisiones como cantidad de costo cero, antes de las operaciones.
4. Derivar las cifras de cada posición, los hechos del ciclo y el instante de la cotización.
5. Exponer la agregación contable del usuario como operación reutilizable sobre el conjunto completo
   de posiciones: nunca sobre un subconjunto paginado.

### 2.3 Guardas de integridad y excepción de integridad

El Model **rechaza** las entradas que vulneran invariantes ya establecidos por el dominio o por la
base de datos. Son detecciones, no reglas de negocio nuevas: el Model no decide qué es una operación
válida, rechaza lo que el mercado ya declara imposible (R-16).

| Guarda | Invariante de origen |
| --- | --- |
| Cantidad de una operación no positiva | `market_transactions.quantity CHECK (quantity > 0)` y `token_listings.quantity CHECK (quantity > 0)` |
| Importe de una operación no positivo | `unit_price CHECK (unit_price > 0)` y `total_amount = quantity * unit_price` |
| Venta cuya cantidad supera la cantidad abierta | Invariante de la posición: no se puede vender lo que no se posee |
| Comprador y vendedor idénticos en una operación | FR-003: una imputación por parte y por rol |
| Cantidad emitida distinta de la constante del dominio | `player_token_emissions.initial_quantity CHECK (initial_quantity = 100)` y `PlayerTokenEmission.EMITTED_TOKENS` |
| Importe que no cabe en el rango admitido | Invariante de representación de SPEC-010 FR-022 |

`FinancialPositionIntegrityException`, en `footballmarket.models.exceptions`, hereda de
`DomainException` (`docs/backend/architecture.md` §1.6) y es la forma en que el cálculo falla cuando
los datos no permiten reconstruir una posición de forma consistente (FR-025). El cálculo nunca
devuelve una posición ni un resultado con cifras inconsistentes: falla.

El invariante de conservación del costo —`totalPaid = Σatribuido + C`— se preserva **por
construcción** y su verificación corresponde a las pruebas de Model; no se comprueba en caliente en
cada operación, porque sería un coste prematuro sobre una garantía que la propia construcción
asegura.

El código de aplicación no lanza excepciones genéricas de Java para errores funcionales
(`docs/backend/conventions.md`).

---

## 3. Fórmulas exactas

Notación: `C` = costo de adquisición vigente, `q` = cantidad abierta previa, `qv` = cantidad
vendida, `I` = importe de la venta registrada, `A` = importe de la compra registrada.

### 3.1 Atribución del costo de una venta

```text
atribuido = HALF_UP( C × qv / q )            → entero en centésimas
C'        = C − atribuido                    → entero, ≥ 0
resultado realizado += I − atribuido
```

- El redondeo es **HALF_UP** a centésimas (FR-016, CL-03).
- El producto y la división se resuelven en aritmética decimal exacta, sin punto flotante y sin
  desbordamiento de `long` (R-03).
- `atribuido ≤ C` siempre: como `qv ≤ q`, el cociente no excede `C`, y el redondeo de un valor
  entero no lo supera.
- En una liquidación total (`qv = q`): `atribuido = C` y `C' = 0` **exacto** (FR-010).

### 3.2 Conservación del costo

En todo momento:

```text
totalPaid = Σ atribuido + C
```

En centésimas enteras, sin diferencias. El residuo que introduce el redondeo permanece en `C` —la
posición abierta— y se incorpora al resultado realizado cuando la posición se liquida (CL-02,
FR-017, SC-002).

### 3.3 Precio medio derivado

```text
precio medio = HALF_UP( C / q )              → entero en centésimas, informativo
```

`C / q` es una división exacta en aritmética decimal. El precio medio **no** interviene en el
cálculo de `C'`: la atribución usa `C × qv / q`, no `precio medio × qv`. Esa diferencia es
precisamente el residuo retenido (CL-11, AC21).

### 3.4 Porcentaje global de resultado

```text
profitLossPercentage = HALF_UP_2( (Σ realizado + Σ no realizado) / Σ totalPaid ) × 100
```

- Puntos porcentuales con dos decimales (CL-08, SPEC-014 FR-027 y AC31).
- **Nulo** si `Σ totalPaid = 0` o si alguna posición abierta carece de cotización.
- El denominador es el **costo histórico total adquirido**: la suma de `totalPaid` de todas las
  posiciones del usuario, incluidas las de posiciones ya cerradas y reabiertas. No es el costo de
  adquisición de las posiciones abiertas, que es un importe distinto.

### 3.5 Agregación contable del usuario

La agregación se calcula sobre el **conjunto completo** de posiciones, nunca sobre una página
(R-19):

```text
historicalCost        = Σ totalPaid de todas las posiciones
investedCost          = Σ acquisitionCost de las posiciones abiertas
currentValue          = Σ valor actual                     → indefinido si alguna posición abierta no tiene cotización
realizedProfitLoss    = Σ resultado realizado
unrealizedProfitLoss  = Σ resultado no realizado            → indefinido si alguna posición abierta no tiene cotización
totalProfitLoss       = realizedProfitLoss + unrealizedProfitLoss → indefinido si alguna posición abierta no tiene cotización
profitLossPercentage  = HALF_UP_2( totalProfitLoss / historicalCost ) × 100
                         → indefinido si historicalCost = 0 o si alguna posición abierta no tiene cotización
valuationComplete     = false si alguna posición abierta no tiene cotización
```

`historicalCost` y `investedCost` **no son el mismo importe**: el primero acumula todos los importes
pagados, incluidos los de ciclos cerrados, y es el denominador de CL-08; el segundo es el costo de
adquisición de las posiciones actualmente abiertas. Un resultado indefinido se representa con `null`
y nunca con cero (FR-013, CL-05, CL-14).

Los porcentajes **por posición** —rendimiento no realizado y peso en el portfolio— **no** pertenecen a
esta agregación: sus fórmulas están definidas por SPEC-014 (FR-028 y FR-029) y se calculan con las
cifras por posición que este documento define.

### 3.6 Ejemplos verificados

**Ejemplo 1 — dos compras y una venta parcial** (AC01, AC02, AC03, AC11)

| Paso | Operación | `q` | `C` | `totalPaid` | Realizado | Precio medio |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Compra de 10 tokens a 100 | 10 | 1000 | 1000 | 0 | 100 |
| 2 | Compra de 10 tokens a 150 | 20 | 2500 | 2500 | 0 | 125 |
| 3 | Venta de 5 tokens a 200 (importe 1000) | 15 | 1875 | 2500 | 375 | 125 |

Paso 3: `atribuido = HALF_UP(2500 × 5 / 20) = 625`; `C' = 2500 − 625 = 1875`;
`realizado = 1000 − 625 = 375`. Con cotización de 180: valor actual `15 × 180 = 2700`,
resultado no realizado `2700 − 1875 = 825`, resultado total `375 + 825 = 1200`.
Conservación: `2500 = 625 + 1875` ✓.

**Ejemplo 2 — promedio fraccionario y residuo retenido** (AC21, AC22)

| Paso | Operación | `q` | `C` | `totalPaid` | Realizado |
| --- | --- | --- | --- | --- | --- |
| 1 | Compra de 2 tokens a 100 y 1 token a 101 | 3 | 301 | 301 | 0 |
| 2 | Venta de 1 token a 150 (importe 150) | 2 | 201 | 301 | 50 |
| 3 | Venta de 2 tokens a 150 (importe 300) | 0 | 0 | 301 | 149 |

Paso 2: `atribuido = HALF_UP(301 × 1 / 3) = HALF_UP(100,33) = 100`; `C' = 301 − 100 = 201`;
`realizado = 150 − 100 = 50`. El precio medio exacto pasa de `100,33` a `100,5` centésimas por
token: el desplazamiento procede únicamente del residuo retenido.
Paso 3: `atribuido = HALF_UP(201 × 2 / 2) = 201`; `C' = 0` **exacto**;
`realizado += 300 − 201 = 99`. Conservación: `301 = 100 + 201 + 0` ✓.

**Ejemplo 3 — liquidación total y reapertura** (AC04, AC05)

| Paso | Operación | `q` | `C` | Realizado acumulado |
| --- | --- | --- | --- | --- |
| 1 | Compra de 10 tokens a 100 | 10 | 1000 | 0 |
| 2 | Venta de 10 tokens a 150 (importe 1500) | 0 | 0 | 500 |
| 3 | Compra de 4 tokens a 200 (importe 800) | 4 | 800 | 500 |

Paso 2: `atribuido = HALF_UP(1000 × 10 / 10) = 1000`; `C' = 0`; `realizado = 1500 − 1000 = 500`.
Paso 3: la compra inicia un costo nuevo (`C = 800`, precio medio 200) y **no reinicia** el resultado
realizado acumulado (FR-009, FR-010).

**Ejemplo 4 — emisión inicial con costo cero** (AC09, AC10)

| Paso | Operación | `q` | `C` | `totalPaid` | Realizado |
| --- | --- | --- | --- | --- | --- |
| 1 | Emisión inicial de 100 tokens | 100 | 0 | 0 | 0 |
| 2 | Compra de 10 tokens a 100 (importe 1000) | 110 | 1000 | 1000 | 0 |
| 3 | Venta de 10 tokens a 150 (importe 1500) | 100 | 909 | 1000 | 1409 |

Paso 2: el precio medio derivado es `HALF_UP(1000/110) = 9` centésimas: la emisión diluye el
promedio con costo cero (CL-07, CL-13). Paso 3: `atribuido = HALF_UP(1000 × 10/110) = 91`;
`C' = 1000 − 91 = 909`; `realizado = 1500 − 91 = 1409`. La mezcla no puede producirse hoy con el modelo de
mercado vigente; la regla queda definida (R-06, AC10).

---

## 4. Estado y transiciones de la posición

| Estado | Condición | Significado |
| --- | --- | --- |
| `OPEN` | `q > 0` | Posición abierta: tiene costo de adquisición y puede valorarse |
| `CLOSED` | `q = 0` y existen operaciones históricas | Posición cerrada: conserva el resultado realizado acumulado; sin valor actual ni resultado no realizado |

El estado no lo persiste SPEC-015: se deriva de `q` y de la existencia de operaciones. La
persistencia del estado de posición y su paginación pertenecen a SPEC-014 (FR-009 de SPEC-014).

### 4.1 Ciclo de tenencia

El ciclo de tenencia es el intervalo durante el cual la posición ha tenido cantidad abierta. El
plegado lo determina sin reglas adicionales (R-18):

| Situación | `openedAt` | `closedAt` |
| --- | --- | --- |
| Posición emitida por el superusuario, nunca transferida | Instante de la emisión (`emitted_at`) | Nulo |
| Posición abierta por compras | Instante de la primera compra del ciclo | Nulo |
| Posición cerrada por liquidación total | Instante de la compra o emisión que abrió el ciclo | Instante de la venta que llevó la cantidad a cero |
| Posición reabierta tras una liquidación | Instante de la compra que reabre el ciclo | Nulo |

Las compras adicionales dentro de un ciclo **no** alteran `openedAt`; una liquidación completa seguida
de una compra inicia un ciclo nuevo. El resultado realizado **no** se reinicia entre ciclos. Las
reglas de presentación de estas fechas pertenecen a SPEC-014 (FR-030, AC26, AC28).

---

## 5. Observación sobre consistencia con las tenencias

La cantidad abierta del cálculo se deriva de operaciones y emisión, no de `player_tokens`. En
condiciones normales ambos valores coinciden, porque toda tenencia del mercado nace de una emisión o
de una compra y solo se reduce por una venta. Si apareciera una divergencia, su detección natural
corresponde a SPEC-014, que sí consulta `player_tokens` para informar `quantity`, `reservedQuantity` y
`availableQuantity`. SPEC-015 no consulta esa tabla ni introduce una comprobación cruzada: hacerlo
acoplaría la regla contable al estado mutable de las tenencias y añadiría una consulta de conjunto
que FR-027 no exige. Registrado como riesgo RA-5 del plan.

---

## 6. Orden contable

| Aspecto | Definición |
| --- | --- |
| Criterio | `(executed_at ASC, id ASC)` |
| Granularidad | **Por jugador**, que es la granularidad de las posiciones: no hace falta un orden global entre jugadores distintos |
| Dónde vive | En el Model, que ordena las operaciones antes de plegarlas (R-04) |
| Soporte de persistencia | La consulta ordena por las mismas columnas |
| Desempate | `id`, que es único dentro de la tabla |
| Por qué no basta `id` | La secuencia se asigna en el `INSERT`, no en el commit: dos transacciones concurrentes pueden invertir su orden de confirmación |
| Por qué no basta `executed_at` | Admite empates entre operaciones confirmadas en el mismo instante |

La unificación de la fuente del reloj (R-05) hace que ambas vías de confirmación registren el
instante con el mismo reloj inyectado, de modo que el orden es controlable en pruebas y comparable
entre compras directas y liquidaciones de publicaciones.

**Coherencia de la lectura.** Operaciones y emisiones se leen dentro de una **única transacción de
solo lectura con aislamiento `REPEATABLE READ`** (R-15), de modo que ambas proceden de la misma
instantánea y la reconstrucción corresponde siempre a un estado confirmado del mercado. Una operación
confirmada durante el cálculo no aparece en la instantánea: el resultado refleja el estado previo,
que es un estado válido. El aislamiento efectivo es el de la transacción envolvente, por lo que el
consumidor que componga el cálculo dentro de su propia transacción debe declarar ese mismo
aislamiento.

---

## 7. Referencias

- Contrato del componente: [`contracts/financial-position-service.md`](contracts/financial-position-service.md)
- Decisiones de diseño: [`research.md`](research.md)
- Reglas funcionales: [`spec.md`](spec.md) (FR-001…FR-032, CL-01…CL-16)
- Esquema vigente: `backend/src/main/resources/db/migration/V10__create_market_domain.sql`,
  `V20261017233439__create_token_listings.sql`,
  `V20261017233445__add_listing_to_market_transactions.sql`

# Research — SPEC-015: Definir reglas contables de costo promedio y resultados financieros

**Feature**: SPEC-015 · **Rama**: `feat/015-define-average-cost-and-financial-results-rules` · **Fecha**: 2026-10-20
**Entrada**: [`spec.md`](spec.md) · **Plan**: [`plan.md`](plan.md)

Este documento registra las decisiones técnicas de la Fase 0 del plan. Cada decisión sigue el formato
**Decisión / Fundamento / Alternativas descartadas**. Las decisiones funcionales ya están resueltas en
`spec.md` (CL-01 a CL-16); aquí no se reabre ninguna: solo se elige cómo implementarlas sin alterar
el alcance aprobado.

---

## R-01 — Rol del componente reutilizable

**Decisión**: la regla contable vive en **Model** y el componente consumible por SPEC-014 es un
**Service independiente**, con interfaz y implementación separadas, cuyo contrato se documenta en
[`contracts/financial-position-service.md`](contracts/financial-position-service.md).

| Pieza | Rol arquitectónico | Ubicación | Responsabilidad |
| --- | --- | --- | --- |
| `PlayerFinancialPosition` | Model | `footballmarket.models` | Agregado por usuario y jugador con sus invariantes: cantidad abierta, costo de adquisición autoritativo, costo pagado histórico, resultado realizado y valoración opcional. |
| `FinancialPositionCalculator` | Model | `footballmarket.models` | Puro y estático: pliega operaciones confirmadas y emisiones en posiciones y calcula el porcentaje global. Sin E/S. |
| `FinancialPositionService` / `…Impl` | Service | `footballmarket.services` / `…services.impl` | Caso de uso reutilizable: carga las operaciones, las emisiones y las cotizaciones vigentes y devuelve las posiciones valoradas. |

**Fundamento**: `docs/backend/architecture.md` §1.4 establece que un componente **consumido por otros
Services** no es un colaborador interno sino un Service independiente, y §1.1 exige interfaz e
implementación separadas con consumidores dependientes de la interfaz. SPEC-014 necesita las
*posiciones ya calculadas*, no solo las fórmulas: si cada feature pliegara las operaciones por su
cuenta, SPEC-014 quedaría prohibido por su FR-014 («MUST NOT … establecer una implementación contable
paralela») y por su AC38, que exige que el orden contable provenga de REQ-S2-20.

El Service además **encapsula la adquisición de cotizaciones**: SPEC-014 no queda atado a la forma
concreta del puerto `QuoteSource` (hoy por jugador), de modo que la llegada del repositorio canónico
de REQ-S2-03 no obliga a cambiar al consumidor.

**Alternativas descartadas**:

- **Solo Model compartido**, con SPEC-014 repitiendo la carga de operaciones, emisiones y cotizaciones:
  duplica la semántica de valoración —la ausencia de cotización como resultado indefinido— y la
  política de fallo técnico. Contradice el objetivo de no duplicar reglas.
- **Orchestrator que coordine SPEC-015 y SPEC-014**: `docs/backend/architecture.md` §1.1 exige que el
  Orchestrator exista solo ante una responsabilidad real de coordinación y prohíbe que use el Model
  para calcular. La coordinación del portfolio no aporta lógica propia: el Orchestrator sería una
  capa vacía.
- **Service → Service sin documentar**: la matriz de §1.3 no enumera esa relación. El código de
  SPEC-012 ya compone Services entre sí
  (`services/impl/PlayerStatisticsSynchronizationServiceImpl.java:64,68`) y su inventario está
  documentado en §3 de la arquitectura del backend. El plan incluye actualizar
  `docs/backend/architecture.md` con un §5 que documente los componentes de SPEC-015 y haga explícita
  esa relación, con el precedente de §3 y §4.

---

## R-02 — Fuente de verdad contable

**Decisión**: el costo y los resultados se **reconstruyen** desde `market_transactions` (operaciones
confirmadas) y `player_token_emissions` (emisión inicial). **No se crea ninguna tabla de costo
apurado ni columna de costo**: no hay segunda fuente de verdad persistida.

**Fundamento**: FR-002 y FR-021 de `spec.md` fijan la fuente de verdad y el papel de la emisión; CL-02
prohíbe un libro de costo paralelo salvo que sea derivado y reconciliable. La reconstrucción es
además la única compatible con el registro append-only que SPEC-010 y SPEC-011 ya garantizan
(`MarketTransaction.java:16-23`).

**Alternativas descartadas**:

- **Tabla `position_cost_basis` apurada**: segunda fuente de verdad, exige reconciliación y un
  procedimiento de recompensuración; contradice FR-002.
- **Columna de costo en `player_tokens`**: mezcla el estado actual de la tenencia con la auditoría
  histórica y obligaría a reescribir el costo al vender, que es justo lo que la regla de
  conservación prohíbe.

---

## R-03 — Aritmética monetaria y precisión

**Decisión**: los importes son `long` en centésimas de crédito. La atribución proporcional del costo
de una venta y el precio medio se calculan con `BigDecimal` **en el interior del Model**, con escala
explícita y `RoundingMode.HALF_UP`; sus valores definidos se expresan como enteros en centésimas.
El precio medio expuesto es `Long` nullable: con cantidad abierta cero es `null`. El porcentaje
global no es un importe monetario: se expresa como `BigDecimal` nullable a escala 2 y se calcula
como `HALF_UP_2(100 × totalProfitLoss / historicalCost)`. Multiplicar por 100 precede al redondeo,
sin redondeo intermedio; el caso resultado 1 e histórico 3 corresponde a `33,33 %`.

Fórmula de atribución, con `C` = costo de adquisición vigente, `q` = cantidad abierta previa y
`qv` = cantidad vendida:

```text
atribuido = HALF_UP(C × qv / q)          → entero en centésimas
C'        = C − atribuido                 → entero, nunca negativo
coste pagado = Σ atribuido + C'          → conservación exacta
```

**Fundamento**: CL-01 y CL-03 fijan centésimas enteras y HALF_UP; FR-017 exige conservación exacta y
el residuo retenido en la posición abierta. `Math.multiplyExact` ya es el patrón vigente del dominio
(`BuyOrderServiceImpl.java:292-298`) pero no resuelve la división, y `double`/`float` traen
representación binaria imprecisa ya descartada en SPEC-010 (`specs/010-buy-tokens/research.md`).
`BigDecimal` interno no altera CL-01: la representación persistida y expuesta sigue siendo `long`/
`BIGINT`.

**Alternativas descartadas**:

- **`BigDecimal` en toda la cadena**: obligaría a cambiar `market_transactions` y los contratos
  públicos de SPEC-010 y SPEC-011, prohibido por CL-01 y por el alcance de SPEC-015.
- **Fracciones exactas con `BigInteger`**: representa el residuo con exactitud total, pero obliga a
  arrastrar un tipo racional por todo el dominio para un resultado que el redondeo ya define.
- **`double`**: imprecisión binaria en importes; descartado por SPEC-010.

---

## R-04 — Orden contable determinista

**Decisión**: el orden contable es `(executed_at ASC, id ASC)`. Lo impone el **Model**, que ordena
las operaciones antes de plegarlas; la consulta de persistencia usa las mismas columnas como
optimización y para dar estabilidad al conjunto recuperado.

**Fundamento**: FR-018 y AC17 exigen un orden total y determinista; CL-06 lo fija. `id` es
`BIGSERIAL` y se asigna en el `INSERT`, no en el commit (`V10__create_market_domain.sql:81`), de modo
que por sí solo no representa el orden efectivo. `executed_at` por sí solo admite empates. El par
`(executed_at, id)` es total porque ambos son monotónicos dentro de una misma tabla.

**Alternativas descartadas**:

- **Solo `id`**: no representa el orden de confirmación entre transacciones concurrentes.
- **Solo `executed_at`**: deja empates sin regla de desempate y rompe el requisito de determinismo.
- **Nuevo identificador contable secuencial**: exige migración y modificar SPEC-010 y SPEC-011, y
  aporta lo mismo que el par ya existente.

---

## R-05 — Unificación de la fuente del reloj

**Decisión**: la compra directa al superusuario registra su instante de confirmación con el **reloj
inyectado `marketClock`**, igual que la liquidación de publicaciones. El cambio es de una línea en
`BuyOrderServiceImpl` y **no altera el contrato público** de SPEC-010: `executed_at` sigue siendo el
instante de confirmación de la operación.

**Fundamento**: FR-019 lo exige y CL-06 lo declara. Hoy conviven dos fuentes: la liquidación de
publicación toma `OffsetDateTime.now(this.marketClock)` (`BuyOrderServiceImpl.java:136`) y la compra
directa toma `OffsetDateTime.now(ZoneOffset.UTC)` (`BuyOrderServiceImpl.java:424`), es decir, el
reloj de sistema. Ambas son UTC, de modo que el orden es comparable, pero el reloj de sistema no es
controlable en pruebas: congelar `marketClock` no congela el `executed_at` de la compra directa. Sin
unificación, el orden contable no es determinista entre ambas vías y la regla de FR-018 queda
sostenida por una casualidad horaria, no por una garantía.

**Alternativas descartadas**:

- **No unificar**: el orden quedaría apoyado en dos relojes distintos; el determinismo exigido por
  FR-018 dependería de que ambos coincidieran en la misma zona.
- **Cambiar el esquema de `market_transactions`**: prohibido por el alcance; `executed_at` no necesita
  cambios.

---

## R-06 — La emisión inicial como entrada de costo cero

**Decisión**: `player_token_emissions` aporta **cantidad**, nunca costo. Se aplica como una emisión
sin precio a la posición de su propietario, antes de cualquier operación, y no se registra como
compra ni altera los contadores de operaciones.

**Fundamento**: FR-021, FR-022 y CL-07. `PlayerTokenEmission` no tiene comprador, vendedor ni precio
(`PlayerTokenEmission.java:32-43`), y su `initial_quantity` está fijada en 100 por el dominio y por
la base de datos (`V10__create_market_domain.sql:57`). La emisión es la razón por la que la posición
del superusuario existe sin operaciones.

**Alternativas descartadas**:

- **Modelar la emisión como una compra ficticia de precio cero**: prohibido por FR-021 y por SPEC-014
  AC36, que exige que la emisión no incremente `totalBuyTransactions`.
- **Excluir los tokens emitidos del cálculo**: contradice FR-022 y SPEC-014 FR-044.

---

## R-07 — Fuente de la valoración

**Decisión vigente tras la autorización del acceso masivo**: ampliar `QuoteSource` con
`Map<Long, CurrentQuote> currentQuotes(Set<Long> playerIds)` y conservar íntegramente
`Optional<CurrentQuote> currentQuote(Long playerId)`. El Service reconstruye las posiciones y
solicita las cotizaciones de los jugadores distintos con posición abierta en una sola llamada
masiva, sin fallback a llamadas individuales. La ausencia de una clave representa cotización
ausente; un fallo técnico aborta el lote, sin convertirlo en ausencias.

**Fundamento**: G-3 y CL-05 fijan que el valor actual proviene de la cotización vigente que expone el
backend sin atarse a una implementación concreta, y FR-013, FR-014, CL-14 y CL-15 fijan la
distinción entre ausencia y fallo. El puerto actual (`integrations/QuoteSource.java:13-22`) solo
expresa ausencia mediante `Optional` vacío y no tiene canal de fallo técnico declarado: el plan
define la traducción de un fallo no controlado de la fuente a una excepción de la jerarquía del
proyecto. La firma y la conducta del método individual no cambian; la extensión masiva queda
documentada en el contrato de SPEC-015, sin editar artefactos de otras SPEC.

**Compatibilidad**: el método masivo será `default` para que las implementaciones existentes del
puerto sigan siendo compatibles a nivel de fuente y binario. Su implementación por defecto
rechazará explícitamente el acceso masivo mediante `QuoteBatchUnavailableException`, heredera de
`IntegrationException`; nunca recorrerá los identificadores llamando a `currentQuote`. Una fuente
que solo implemente lectura individual podrá seguir atendiendo SPEC-010/011, pero deberá
implementar lectura masiva antes de atender SPEC-015/014. Ese rechazo no significa cotizaciones
ausentes y no autoriza activar el simulador como respaldo.

`SimulatedQuoteSource` implementará el lote construyendo el mapa en memoria con un único instante
de cálculo, sin E/S ni llamadas individuales. La futura fuente persistida deberá obtener el
conjunto en una consulta agrupada, sin búsquedas por jugador. La extensión no incluye crear esa
fuente ni el motor de cotizaciones. Las pruebas de compatibilidad abarcarán la fuente alternativa
de `QuoteSimulationConfigurationTest` y el método individual ya utilizado por el mercado.

**Alternativas descartadas**:

- **Dos consultas contables más N lecturas individuales**: descartado tras la autorización del
  acceso masivo; no satisface FR-027 ni SPEC-014 AC35. La prohibición anterior de extender el puerto
  queda sustituida por la autorización explícita de una extensión aditiva, sin alterar el método
  individual ni las otras SPEC.
- **Método masivo abstracto obligatorio**: rompe la compilación de implementaciones existentes.
- **Método masivo default que itera sobre el individual**: preserva la firma, pero oculta N+1.
- **Valorar en el consumidor**: traslada la regla de ausencia y de fallo a SPEC-014, con riesgo de
  dos implementaciones divergentes.
- **Tratar la ausencia como cero**: contradice FR-004, FR-013 y CL-05.

---

## R-08 — Estrategia de consultas

**Decisión**: el caso de uso resuelve **dos** consultas de conjunto y ninguna por posición:

| Consulta | Origen | Cardinalidad |
| --- | --- | --- |
| Operaciones del usuario como comprador o vendedor, ordenadas por `(executed_at, id)` | `market_transactions` | 1 por usuario |
| Emisiones del usuario | `player_token_emissions` | 1 por usuario |
| Cotizaciones vigentes de posiciones abiertas | `QuoteSource.currentQuotes(Set<Long>)` | Un lote por usuario; ningún acceso individual por jugador |

**Fundamento**: FR-027 exige que el cálculo se resuelva sobre el conjunto completo de operaciones del
usuario sin un acceso individual por posición, y SPEC-014 AC35 exige lo mismo para el portfolio. La
cantidad abierta de cada posición **se deriva de las operaciones y de la emisión**, por lo que el
cálculo **no necesita** consultar `player_tokens`: una consulta menos y ninguna dependencia del
estado de tenencias, que es la proyección mutable del dominio.

`market_transactions` ya tiene índices por comprador y por vendedor
(`V10__create_market_domain.sql:102-103`), pero la combinación «comprador **o** vendedor» degrada a
dos recorridos y unión. `player_token_emissions` no tiene índice por `owner_user_id`, de modo que la
consulta de emisiones requiere **un índice nuevo** en una migración nueva: es un índice, no una
fuente de verdad, y no altera el modelo de datos.

**Alternativas descartadas**:

- **Consultar `player_tokens` para la cantidad**: introduciría el estado mutable de las tenencias en
  la reconstrucción contable y una fuente de verdad adicional.
- **Un acceso por posición a operaciones, emisiones o cotizaciones**: contradice FR-027.

---

## R-09 — Errores y fallos

**Decisión**: el cálculo no expone endpoint propio, de modo que **no** introduce códigos HTTP ni
modifica `GlobalExceptionHandler`. Las excepciones propias respetan la jerarquía de
`docs/backend/architecture.md` §1.6:

| Situación | Excepción | Categoría | Destino |
| --- | --- | --- | --- |
| Los datos no permiten reconstruir una posición de forma consistente (FR-025) | `FinancialPositionIntegrityException` | `DomainException` | El consumidor la traduce; SPEC-014 la mapea a error de integridad. |
| La cantidad vendida excede la cantidad abierta de la posición | `FinancialPositionIntegrityException` | `DomainException` | Ídem. |
| Fallo técnico de la fuente de cotizaciones (FR-014, CL-15) | Excepción de integración | `IntegrationException` | Ídem. |

**Fundamento**: `GlobalExceptionHandler` **no tiene** manejador genérico de `FootballMarketException`
(solo manejadores tipados), de modo que una excepción nueva sin mapeo explícito cae en la respuesta
por defecto de Spring y rompe el contrato `ErrorResponseDTO` de §1.7. Como SPEC-015 no expone
endpoint, el mapeo HTTP corresponde a SPEC-014; queda registrado como dependencia de integración.

**Alternativas descartadas**:

- **Mapear la excepción en `GlobalExceptionHandler` desde SPEC-015**: ampliaría el alcance a un
  endpoint ajeno, que pertenece a SPEC-014.
- **Degradar a resultado indefinido ante un fallo técnico**: contradice FR-014 y CL-15.

---

## R-10 — Alcance técnico

**Decisión**: SPEC-015 **no** introduce DTO, Mapper, Controller, endpoint, migración de modelo de
negocio, dependencia nueva, ni cambios en el contrato público de SPEC-010 y SPEC-011. El único
cambio de comportamiento ajeno a la feature es la unificación del reloj de R-05, que no altera el
significado de ningún dato registrado.

**Fundamento**: FR-029 excluye comisiones, impuestos y spreads; CL-09 lo reitera; el enunciado
(`tp/enunciado.md` §3.4) pide visualizar el portfolio, no crear la contabilidad. La Constitución §I
prohíbe crear capas, interfaces, dependencias o abstracciones por costumbre: cada pieza del plan
tiene una responsabilidad real.

**Alternativas descartadas**:

- **Exponer `GET /api/financial-positions`**: contradice CL-10 y FR-028; la consulta de portfolio es
  de SPEC-014 y su ruta ya fue decidida allí.
- **Crear un endpoint de resultado por jugador**: misma razón.

---

## R-11 — Idioma y nomenclatura

**Decisión**: identificadores, paquetes, métodos y campos en inglés; documentación, Javadoc de
clases públicas, mensajes de error y estos artefactos en español.

**Fundamento**: Constitución, sección *Idioma*; `docs/backend/conventions.md` exige Javadoc en clases
públicas y en todo método cuyo contrato no sea evidente.

---

## R-12 — Frontera de compatibilidad con SPEC-014

**Decisión**: el contrato del componente
([`contracts/financial-position-service.md`](contracts/financial-position-service.md)) es la frontera
entre SPEC-015 y SPEC-014. SPEC-014 consume las posiciones con sus cifras autoritativas y **no**
recalcula el costo a partir del precio medio redondeado.

**Fundamento**: CL-11 y FR-006 de `spec.md` fijan el costo de adquisición como cifra autoritativa y
el precio medio como derivado; SPEC-014 FR-014 delega esas reglas y prohíbe una implementación
contable paralela. La divergencia detectada en la redacción de SPEC-014 (su CL-03 afirma que las
ventas no modifican el precio medio restante, lo que con el residuo retenido solo es exacto en
aritmética fraccionaria) se corrige en el origen, en la especificación de SPEC-014.

---

## R-13 — Estrategia de pruebas

**Decisión**: cobertura en las categorías que `docs/backend/testing.md` §2 asigna, sin pruebas de
Controller porque la feature no expone endpoint:

| Categoría | Qué prueba | Etiqueta |
| --- | --- | --- |
| Model | Reglas contables: promedio ponderado, atribución, redondeo, conservación, cierre y reapertura, emisión con costo cero, resultado negativo, valoración indefinida | `unit` |
| Repository | Consultas de conjunto: operaciones del usuario en ambos roles y ordenadas, emisiones del propietario, índice de `owner_user_id` | `integration` |
| Service | Caso de uso: posiciones de un usuario con operaciones y emisión, cotización presente y ausente, propagación del fallo técnico, ausencia de escrituras | `integration` |
| Service (regresión) | La compra directa registra `executed_at` desde el reloj inyectado | `integration` |

**Fundamento**: `docs/backend/testing.md` §3, §4, §5 y §17 exigen probar cada comportamiento al nivel
más específico que lo demuestra, con `@Nested` obligatorio (§16) y una única etiqueta literal por
clase contenedora (§"Suites de ejecución y clasificación"). La categoría Service exige
`@SpringBootTest` con PostgreSQL real por Testcontainers y la Integration mockeada (§5, §12); el
helper `support/MarketTestData` cubre la preparación de precondiciones en Arrange (§5, excepción de
Arrange).

**Alternativas descartadas**:

- **Pruebas de Controller**: no hay endpoint; la categoría no aplica.
- **Probar solo por la vía HTTP de SPEC-014**: el componente de SPEC-015 debe probarse por su propia
  interfaz, y SPEC-014 todavía no existe como implementación.

---

## R-14 — Índices y migraciones

**Decisión**: una migración nueva, solo con el índice
`player_token_emissions (owner_user_id)`. Ninguna otra tabla ni columna nueva.

**Fundamento**: `player_token_emissions` tiene la clave primaria en `player_id` y la columna
`owner_user_id` es clave foránea **sin índice** (`V10__create_market_domain.sql:54-59`); la consulta de
emisiones del propietario lo recorre completo. Para `market_transactions` no hace falta índice nuevo:
los índices por comprador y por vendedor ya existen y PostgreSQL puede recorrerlos en sentido
inverso para la ordenación ascendente.

**Alternativas descartadas**:

- **Índice compuesto adicional sobre `market_transactions`**: los índices existentes cubren la
  reconstrucción; añadirlo sería complejidad sin evidencia de necesidad.

---

## R-15 — Coherencia transaccional de la reconstrucción

**Decisión**: el cálculo se ejecuta dentro de **una sola transacción de solo lectura con aislamiento
`REPEATABLE READ`**. El método del Service declara `@Transactional(readOnly = true, isolation =
Isolation.REPEATABLE_READ)`. Cuando SPEC-014 lo invoca **desde dentro de su propia transacción**, el
aislamiento efectivo es el de la transacción envolvente —Spring no cambia el aislamiento de una
transacción que ya participa—, por lo que **SPEC-014 debe declarar su consulta de portfolio en
`REPEATABLE READ`** para que la respuesta completa sea coherente (AC34 de SPEC-014).

**Fundamento**: la reconstrucción lee operaciones y emisiones, y SPEC-014 leerá además tenencias y
reservas. Con el aislamiento por defecto (`READ COMMITTED`), cada sentencia ve una instantánea
distinta: una venta confirmada entre ambas lecturas podría quedar fuera de la reconstrucción, de
modo que las cantidades y los resultados no corresponderían a ningún estado válido. Bajo
`REPEATABLE READ`, la instantánea queda fijada en la primera sentencia y todas las lecturas
proceden del **mismo estado confirmado**: una operación de mercado confirma su registro de auditoría
y sus movimientos en una sola transacción, así que la reconstrucción corresponde siempre a un estado
real del mercado. La transacción no escribe, de modo que no genera conflictos de escritura, y
`SERIALIZABLE` se descarta porque puede fallar incluso en transacciones de solo lectura.

**Alternativas descartadas**:

- **`READ COMMITTED`**: dos sentencias, dos instantáneas; la reconstrucción puede mezclar estados.
- **Una sola sentencia SQL que una operaciones y emisiones**: acopla dos fuentes distintas y complica
  el desempate del orden contable sin ganar coherencia.
- **`SERIALIZABLE`**: puede producir fallos de serialización en transacciones de solo lectura.
- **Bloqueos `PESSIMISTIC_WRITE` como los del mercado**: bloquearían las operaciones de mercado
  durante la lectura y abrirían la puerta a interbloqueos; son la herramienta del orden global de
  bloqueo de SPEC-011, no la de una consulta.

---

## R-16 — Guardas de integridad contable

**Decisión**: el Model rechaza, con `FinancialPositionIntegrityException` (`DomainException`), las
entradas que vulneran invariantes **ya establecidos** por el dominio o por la base de datos:

| Guarda | Invariante de origen |
| --- | --- |
| Cantidad de una operación no positiva | `market_transactions.quantity CHECK (quantity > 0)` y `TokenListing.quantity CHECK (quantity > 0)` |
| Importe de una operación no positivo | `unit_price CHECK (unit_price > 0)` y `total_amount = quantity * unit_price` |
| Venta cuya cantidad supera la cantidad abierta | invariante de la posición: no se puede vender lo que no se posee |
| Compra y venta en la misma operación para el mismo usuario | FR-003: una operación se imputa una sola vez por parte |
| Cantidad emitida distinta de la constante del dominio | `player_token_emissions.initial_quantity CHECK (initial_quantity = 100)` y `PlayerTokenEmission.EMITTED_TOKENS` |
| Importe que no cabe en el rango admitido | invariante de representación de SPEC-010 FR-022 |

**Fundamento**: FR-025 exige que el cálculo falle ante datos que no permiten reconstruir una posición
de forma consistente, y el caso límite de integridad de `spec.md` prohíbe devolver resultados
financieros incorrectos. Son **detecciones de invariantes existentes**, no reglas de negocio nuevas:
el Model no decide qué es una operación válida, rechaza lo que el dominio ya declara imposible. Las
guardas del Model son necesarias porque el componente se ejerce también de forma aislada, fuera de
la base de datos, y porque ninguna consulta debe poder fabricar un estado que el mercado no produce.

**Alternativas descartadas**:

- **Confiar solo en los `CHECK` de la base de datos**: protegen el almacenamiento, pero no el
  componente cuando se ejerce aislado, y no permiten distinguir «imposible» de «corrupto».
- **Afirmar la conservación del costo en cada operación**: el invariante se preserva por
  construcción —el costo de adquisición solo disminuye por atribuciones ya descontadas— y su
  verificación corresponde a las pruebas de Model; una comprobación en caliente sería un coste
  prematuro en cada operación.

---

## R-17 — Escalabilidad: agregación y acceso a cotizaciones

**Decisión**: el caso de uso resuelve **un número constante de consultas de conjunto**, que no crece
con la cantidad de posiciones:

| Paso | Consulta | Cardinalidad |
| --- | --- | --- |
| 1 | Operaciones del usuario (comprador o vendedor) | 1 por usuario |
| 2 | Emisiones del propietario | 1 por usuario |
| 3 | Cotizaciones del **conjunto distinto de jugadores con posición abierta** | Un acceso masivo, sin consultas individuales |

El Service reconstruye primero las posiciones y obtiene el conjunto distinto de jugadores con
cantidad abierta positiva. Si el conjunto es vacío, omite la adquisición. En otro caso invoca una
sola vez `currentQuotes`. Una respuesta con claves ajenas, snapshots nulos o identificadores que
no correspondan a su clave es un incumplimiento del contrato, no una cotización ausente.

**Fundamento**: FR-027 y AC35 de SPEC-014 exigen que el cálculo no resuelva una consulta por
posición, incluidas las cotizaciones. Se realizan dos consultas contables y como máximo una
adquisición masiva: el simulador no hace E/S y una fuente persistida deberá resolver el lote con
una consulta de conjunto. Esta garantía no significa tiempo ni memoria constantes: reconstruir
el historial y construir el mapa requiere procesamiento proporcional a su volumen.

El índice sobre `player_token_emissions (owner_user_id)` (R-14) es el único índice nuevo. No se
añaden índices sobre `market_transactions`: los índices por comprador y por vendedor ya existen y
PostgreSQL puede recorrerlos en sentido inverso para la ordenación ascendente. El orden de las
operaciones es **por jugador** —que es la granularidad de las posiciones—, de modo que la
reconstrucción no necesita un orden global entre jugadores distintos.

**Alternativas descartadas**:

- **Índice compuesto adicional sobre `market_transactions`**: los índices actuales cubren la
  consulta; añadirlo es complejidad sin medición que lo justifique.
- **API paginada del Service**: ningún consumidor la pide y SPEC-014 necesita el conjunto completo
  para su resumen global; sería superficie de API prematura.
- **Método de lote que delegue una lectura por jugador**: mantiene N+1 y contradice FR-027 y AC35.

---

## R-18 — Hechos derivados del mismo plegado

**Decisión**: `PlayerFinancialPosition` expone cinco hechos adicionales, obtenidos **durante el mismo
procesamiento del historial confirmado**, sin consultas ni interpretaciones adicionales:

| Hecho | Origen en el historial | Regla que lo gobierna |
| --- | --- | --- |
| `openedAt` | Instante de la adquisición que abrió el ciclo de tenencia vigente: una compra confirmada o, si el ciclo lo abrió la emisión inicial, `emitted_at` | Consecuencia de la evolución de la posición |
| `closedAt` | Instante de la venta confirmada que llevó la cantidad a cero; nulo mientras haya cantidad abierta | Consecuencia de la evolución de la posición |
| `lastTransactionAt` | Instante de la última operación confirmada en la que el usuario es comprador o vendedor | Imputación por rol (FR-003) |
| `buyOperationCount` / `sellOperationCount` | Recuento de operaciones confirmadas por rol, una vez cada una e con independencia de la cantidad | Imputación por rol (FR-003) |
| `quoteCalculatedAt` | `CurrentQuote.calculatedAt` de la **misma** instantánea que aportó el precio | Coherencia precio-instante |

**Fundamento**: los cinco hechos son consecuencia del mismo recorrido de la historia confirmada que
ya produce las cifras financieras. Exponerlos evita que un consumidor reinterprete la misma secuencia
y que consulte `market_transactions` por su cuenta. La imputación por rol ya es regla de SPEC-015
(FR-003), y la emisión conserva su papel de cantidad de costo cero: **no se contabiliza como compra
ni incrementa `buyOperationCount`** (AC36 de SPEC-014). La emisión sí aporta el instante de inicio
del ciclo cuando el ciclo lo abrió ella. El precio y su instante provienen de la misma instantánea de
`CurrentQuote`, de modo que no pueden divergir; sin cotización, ambos son nulos. Las **reglas de
presentación** —qué campos se informan, el formato ISO 8601, el nombre en la respuesta— siguen
perteneciendo a SPEC-014 (FR-020, FR-030, FR-031).

**Alternativas descartadas**:

- **Que SPEC-014 derive ciclo, recuentos e instante**: obliga a una segunda consulta de
  `market_transactions` y a una segunda interpretación de la misma secuencia ordenada. El riesgo es
  concreto: si el ciclo lo calcula un componente y la cantidad y el costo los calcula otro sobre la
  misma secuencia, la posición puede quedar con un ciclo y un costo que no pertenecen al mismo estado.
- **Exponer el historial crudo de operaciones**: filtra la estructura interna del registro financiero
  y deja la re-interpretación en el consumidor, que es justo lo que se quiere evitar.
- **Un método de Service aparte para el ciclo**: segunda llamada y, con ella, segunda instantánea,
  con el riesgo de que el ciclo no coincida con las posiciones.

---

## R-19 — Agregación financiera reutilizable en Model

**Decisión**: `FinancialPositionCalculator` expone una **operación reutilizable sobre el conjunto
completo de posiciones calculadas** que devuelve la agregación contable del usuario:

| Cifra | Definición | Indefinida cuando |
| --- | --- | --- |
| `historicalCost` | Σ del costo pagado histórico de todas las posiciones | Nunca |
| `investedCost` | Σ del costo de adquisición de las posiciones abiertas | Nunca |
| `currentValue` | Σ del valor actual | Alguna posición abierta sin cotización |
| `realizedProfitLoss` | Σ del resultado realizado | Nunca |
| `unrealizedProfitLoss` | Σ del resultado no realizado | Alguna posición abierta sin cotización |
| `totalProfitLoss` | Σ del resultado realizado + Σ del resultado no realizado | Alguna posición abierta sin cotización |
| `profitLossPercentage` | `HALF_UP_2(100 × (realizado + no realizado) / historicalCost)`, como `BigDecimal` nullable a escala 2, sin redondeo intermedio | Denominador cero o alguna posición abierta sin cotización |
| `valuationComplete` | `false` si alguna posición abierta carece de cotización | — |

**La interfaz del Service no cambia**: `positionsOf` sigue devolviendo las posiciones, y la agregación
es una operación de Model que el consumidor aplica sobre el conjunto que ya recibe. La agregación se
calcula **siempre sobre el conjunto completo** de posiciones, nunca sobre una página.

**Fundamento**: CL-08 es una regla de SPEC-015 y su fórmula, junto con la semántica de los
resultados indefinidos, debe vivir en el componente que la posee, no en el consumidor: SPEC-014 FR-027
delega la fórmula exacta y FR-014 prohíbe una implementación contable paralela. La agregación
distingue explícitamente el **costo histórico pagado** —el denominador de CL-08— del **costo de
adquisición de las posiciones abiertas** —`totalInvested` de SPEC-014— porque no son el mismo
importe: el primero incluye los ciclos cerrados. Con una sola llamada y una sola instantánea, el
consumidor obtiene posiciones y agregados mutuamente coherentes, sin consultas duplicadas.

**Alternativas descartadas**:

- **Un método de Service aparte para el porcentaje**: segunda llamada y segunda instantánea, con el
  riesgo de que el porcentaje no corresponda a las posiciones mostradas.
- **Devolver el agregado dentro del resultado del Service**: funciona, pero modifica la interfaz
  aprobada por RA-1 y acopla el Service a la forma del resumen del portfolio, que es composición de
  SPEC-014.
- **Que SPEC-014 calcule los agregados**: duplicaría la fórmula de CL-08 y la semántica de
  indefinidos de FR-026, que es exactamente lo que la revisión cruzada busca evitar.

---

## Conclusiones de la Fase 0

No queda ningún `NEEDS CLARIFICATION` sin resolver. Las decisiones R-01 a R-19 son técnicas, no
funcionales: ninguna modifica el alcance aprobado en `spec.md`, ninguna altera un requisito, un
criterio de aceptación ni una aclaración, y ninguna exige modificar otra SPEC.

Dos puntos requieren atención humana en la revisión del plan, por ser decisiones que tocan
documentación normativa y no código:

1. **R-01**: la relación Service → Service entre SPEC-015 y SPEC-014 requiere actualizar
   `docs/backend/architecture.md` (§1.3 y un nuevo §5), siguiendo el precedente de §3 y §4. La
   aprobación humana de esa actualización queda registrada; la edición del documento se realiza
   durante IMPLEMENT, no antes.
2. **R-12**: la precisión pendiente de SPEC-014 debe corregirse en su propia especificación, que
   pertenece a otra rama y a otra feature.

La revisión final añadió tres decisiones técnicas —**R-15** coherencia transaccional de la
reconstrucción, **R-16** guardas de integridad contable y **R-17** estrategia de escalabilidad— sin
alterar ninguna decisión funcional ni contrato público.

La revisión cruzada con SPEC-014 añadió dos decisiones más, sobre la base de la aprobación humana:
**R-18** hechos derivados del mismo plegado —ciclo de tenencia, recuentos por rol e instante de
cotización— y **R-19** agregación financiera reutilizable en Model. Ambas amplían el contrato de dos
tipos de Model ya previstos: no introducen componentes, consultas, endpoints ni migraciones, y no
alteran ninguna decisión funcional ni contrato público.

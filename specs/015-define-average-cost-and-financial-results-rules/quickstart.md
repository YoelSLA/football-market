# Quickstart — validación de SPEC-015

**Feature**: SPEC-015 · **Rama**: `feat/015-define-average-cost-and-financial-results-rules` · **Fecha**: 2026-10-20
**Plan**: [`plan.md`](plan.md) · **Modelo**: [`data-model.md`](data-model.md) · **Contrato**: [`contracts/financial-position-service.md`](contracts/financial-position-service.md)

Esta guía describe **cómo validar** la implementación de la regla contable una vez existiendo. No
contiene código de implementación, ni cuerpos de clases, ni migraciones, ni la suite de pruebas
completa: esos artefactos pertenecen a `tasks.md` y a la fase de implementación.

---

## 1. Requisitos previos

| Requisito | Detalle |
| --- | --- |
| JDK | 21 (`backend/build.gradle.kts:86-87`) |
| Docker | Necesario para Testcontainers (PostgreSQL 18) en las categorías Repository y Service |
| Base de datos de desarrollo | No es necesaria: las pruebas de integración levantan su propia base |
| Cotizaciones | El simulador debe estar habilitado en el perfil de pruebas; `market.quote.simulator.enabled` y `market.quote.simulator.price` ya están definidos en `application-test.yml:54-57` |

---

## 2. Comandos de validación

Los comandos se ejecutan desde `backend/`. **La ejecución de las pruebas corresponde a la persona
responsable del proyecto**: el agente no las ejecuta.

| Objetivo | Comando |
| --- | --- |
| Reglas contables puras (categoría Model) | `./gradlew unitTest` |
| Consultas de conjunto y persistencia (categoría Repository) | `./gradlew integrationTest` |
| Caso de uso completo (categoría Service) | `./gradlew integrationTest` |
| Comprobación rápida sin cobertura | `./gradlew fastTest` |
| Clasificación de las suites | `./gradlew validateTestClassification` |
| Control de formato | `./gradlew spotlessCheck` (backend) y `npm run biome:check` (frontend, sin cambios esperados) |
| Build sin pruebas | `./gradlew build -x test` |

Las suites están separadas por etiqueta JUnit (`unit`, `lightweight`, `integration`, `e2e`), según
`docs/backend/testing.md` y la tarea `validateTestClassification` de `backend/build.gradle.kts`.

---

## 3. Escenarios de validación

### 3.1 Categoría Model — la regla contable

Ejecutar `./gradlew unitTest`. Cada escenario debe quedar cubierto por una prueba de la categoría
Model, en una clase contenedora con etiqueta `unit` y grupos `@Nested`.

| Escenario | Qué debe observarse | Referencia |
| --- | --- | --- |
| Dos compras a precios distintos | El precio medio derivado es 125 centésimas y el costo de adquisición, 2500 | AC01, AC02 |
| Venta parcial | El resultado realizado es 375, el costo de adquisición pasa a 1875 y la cantidad abierta a 15 | AC03 |
| Liquidación total | El costo de adquisición llega exactamente a 0 y el resultado realizado se conserva | AC04 |
| Reapertura tras liquidación | La compra posterior inicia un costo nuevo y el resultado realizado acumulado no se reinicia | AC05 |
| Publicación no ejecutada | No altera cantidad abierta, costo ni resultado | AC06 |
| Compra como comprador y como vendedor | Cada parte recibe solo su imputación; ninguna operación se contabiliza dos veces | AC07, AC08 |
| Emisión inicial | Posición abierta de 100 tokens con costo cero y resultado realizado cero | AC09 |
| Mezcla de tokens emitidos y adquiridos | Un único costo de adquisición y un único precio medio, con la emisión diluyendo | AC10 |
| Promedio fraccionario | El costo atribuido es 100 y el costo restante 201; la conservación es exacta | AC21 |
| Liquidación tras el residuo | El costo llega a 0 y el resultado realizado acumulado es 149 | AC22 |
| Venta por debajo del costo | El resultado realizado es negativo y conserva su signo | AC15 |
| Posición sin cotización | El resultado no realizado y el total quedan sin definir; el realizado se conserva | AC12 |
| Porcentaje global | Dos decimales; nulo con denominador cero | CL-08 |
| Reproducibilidad | Dos cálculos sobre el mismo estado de datos devuelven el mismo resultado | AC18 |
| Ciclo de tenencia | `openedAt` es el inicio del ciclo vigente; una compra adicional no lo altera; una liquidación lo cierra y una compra posterior abre un ciclo nuevo | AC04, AC05, R-18 |
| Recuentos por rol | Cada operación confirmada cuenta una vez por rol; la emisión no cuenta como compra ni como venta | R-18, AC36 de SPEC-014 |
| Precio e instante | `currentUnitPrice` y `quoteCalculatedAt` provienen de la misma cotización; sin cotización ambos son nulos | R-18, AC05 de SPEC-014 |

### 3.2 Categoría Repository — las consultas de conjunto

Ejecutar `./gradlew integrationTest`. Verificar en PostgreSQL real, con las migraciones de Flyway
aplicadas.

| Escenario | Qué debe observarse | Referencia |
| --- | --- | --- |
| Operaciones del usuario en ambos roles | La consulta devuelve las compras del usuario **y** las ventas del usuario, sin repetir ninguna | FR-002, FR-003 |
| Orden de las operaciones | El resultado viene ordenado por `(executed_at, id)` ascendente | FR-018, AC17 |
| Operaciones de otro usuario | No aparecen en el resultado | FR-024 |
| Emisiones del propietario | Se devuelven las emisiones con su cantidad y su propietario | FR-021 |
| Índice de emisiones | La migración nueva está aplicada y el plan de consulta lo usa | R-14 |

### 3.3 Categoría Service — el caso de uso

Ejecutar `./gradlew integrationTest`.

| Escenario | Qué debe observarse | Referencia |
| --- | --- | --- |
| Usuario con compras y una venta | Las posiciones reflejan costo autoritativo, precio medio derivado y resultado realizado acumulado | AC01–AC03 |
| Superusuario con emisión | Posición abierta de 100 tokens con costo cero y valoración de la posición | AC09 |
| Cotización ausente | La posición se devuelve con resultado no realizado y total sin definir | AC12 |
| Fallo técnico de cotizaciones | El caso de uso falla y no devuelve ningún dato financiero de la posición | AC13, AC15 |
| Solo lectura | Tras el cálculo, las operaciones, tenencias, reservas, publicaciones y saldos no cambiaron | FR-023 |
| Determinismo | Dos llamadas seguidas devuelven resultados idénticos | AC18, AC20 |
| Error de integridad | Una venta que excede la cantidad abierta hace fallar el cálculo | FR-025 |
| Agregación contable | La agregación sobre el conjunto completo devuelve el costo histórico, el invertido, el valor actual, los resultados y el porcentaje global, con nulos cuando alguna posición abierta carece de cotización | R-19, CL-08 |
| Agregación no paginada | La agregación se calcula siempre sobre el conjunto completo; el porcentaje de un subconjunto no se expone | R-19 |

### 3.4 Coherencia concurrente e integridad

Ejecutar `./gradlew integrationTest`.

| Escenario | Qué debe observarse | Referencia |
| --- | --- | --- |
| Operación de mercado confirmada durante el cálculo | La instantánea del cálculo no la incluye; el resultado corresponde al estado previo, que es un estado válido | R-15, AC34 de SPEC-014 |
| Lecturas de operaciones y emisiones en la misma transacción | Ambas proceden de la misma instantánea; no se combinan estados distintos | R-15 |
| Venta que excede la cantidad abierta | El cálculo falla con la excepción de integridad y no devuelve posiciones | FR-025, R-16 |
| Cantidad o importe no positivos en una operación | El cálculo falla con la excepción de integridad | R-16 |
| Comprador y vendedor idénticos | El cálculo falla con la excepción de integridad | FR-003, R-16 |
| Cantidad emitida distinta de la constante | El cálculo falla con la excepción de integridad | R-16 |
| Número de consultas | El cálculo resuelve un número constante de consultas, independiente de la cantidad de posiciones | FR-027, R-17 |

### 3.5 Regresión de la unificación del reloj

| Escenario | Qué debe observarse | Referencia |
| --- | --- | --- |
| Compra directa al superusuario | El `executed_at` registrado proviene del reloj inyectado `marketClock`, controlable en pruebas | FR-019, R-05 |
| Liquidación de publicación | El `executed_at` registrado conserva su semántica de instante de confirmación y su valor validado | SPEC-011, FR-030 |
| Orden mixto | Una compra directa y una liquidación registradas con el mismo instante se ordenan de forma estable por identificador | AC17 |

---

## 4. Resultado esperado

La feature queda validada cuando:

1. Todas las pruebas de las tres categorías pasan en la suite real, con los resultados aportados por
   la persona responsable.
2. `validateTestClassification` no reporta inconsistencias: cada clase contenedora nueva declara
   exactamente una etiqueta literal.
3. `spotlessCheck` y `biome:check` no reportan observaciones.
4. El build `./gradlew build -x test` es satisfactorio.

**Estos controles no acreditan la revisión ni el testing de la funcionalidad**: el cierre exige
revisión de código, validación de pruebas y autorización de cierre, todas humanas y explícitas
(`docs/development/sdd-workflow.md` §6 y §7).

---

## 5. Comprobaciones manuales de contraste

| Comprobación | Cómo | Qué revela |
| --- | --- | --- |
| La cifra autoritativa es el costo, no el precio medio | Tomar una posición con residuo y comparar `acquisitionCost` con `averageUnitPrice × openQuantity` | La diferencia es el residuo retenido y confirma que el consumidor no debe recalcular el costo |
| El orden contable no depende del reloj de la consulta | Ejecutar el caso de uso dos veces con pausas | Resultados idénticos (AC18) |
| No hay segunda fuente de verdad | Revisar las migraciones aplicadas | Solo existe el índice de `owner_user_id`; ninguna tabla de costo apurado |
| La emisión no es una compra | Revisar el contador de operaciones de la posición del superusuario | La emisión no lo incrementa (AC36 de SPEC-014) |
| La coherencia no depende del reloj de la consulta | Confirmar que el cálculo se ejecuta en una transacción de solo lectura `REPEATABLE READ` | La instantánea es única y estable entre lecturas (R-15) |
| Los hechos derivados pertenecen a la misma instantánea | Comparar `openedAt`, `closedAt`, `lastTransactionAt` y los recuentos con la historia confirmada | Salen del mismo recorrido del historial, no de una segunda consulta (R-18) |

---

## 6. Referencias

- Especificación funcional y criterios de aceptación: [`spec.md`](spec.md)
- Fórmulas con ejemplos verificados: [`data-model.md`](data-model.md) §3 y §4.1 (ciclo de tenencia)
- Contrato del componente: [`contracts/financial-position-service.md`](contracts/financial-position-service.md)
- Decisiones de diseño y alternativas descartadas: [`research.md`](research.md)
- Reglas de pruebas del backend: `docs/backend/testing.md`
- Arquitectura del backend: `docs/backend/architecture.md`

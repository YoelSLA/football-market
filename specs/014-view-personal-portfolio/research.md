# Research: Consultar portfolio personal (SPEC-014)

**Feature**: SPEC-014 · **Rama**: `feat/014-consultar-portfolio-personal` · **Fecha**: 2026-10-20
**Plan**: [`../plan.md`](../plan.md) · **Spec**: [`../spec.md`](../spec.md)

Decisiones técnicas de la fase Plan. No modifican el alcance aprobado en `spec.md`: ninguna altera
un requisito funcional, un criterio de aceptación ni una aclaración, y ninguna exige modificar otra
SPEC. La fuente de verdad contable es REQ-S2-20 (SPEC-015), que esta feature consume como frontera.

---

## R-01 — Rol del caso de uso y de la capa HTTP

**Decisión**: la consulta vive en un **Service** propio (`PortfolioService`), con un **Controller** que
solo valida y transporta, y un **Mapper** que traduce la vista de dominio a los DTOs.

**Fundamento**: `docs/backend/architecture.md` §1.1 prohíbe la lógica de negocio en el Controller, y
§1.2 exige que el Mapper no contenga cálculos. La composición del portfolio —agregación contable,
lectura de tenencias y reservas, filtro, orden, paginación y porcentajes por posición— es un caso de
uso con responsabilidades reales, no un ensamblaje trivial: la interfaz y la implementación separadas
son obligatorias para un Service (conventions §1.1).

**Alternativas descartadas**:

- **Lógica en el Controller**: pondría ordenamiento, filtrado y cálculos en la frontera HTTP, lo que
  §1.1 prohíbe de forma expresa.
- **Un único Service que también exponga el endpoint**: mezclaría la frontera HTTP con el caso de uso y
  ampliaría el alcance de la feature.
- **Un Orchestrator**: `docs/backend/architecture.md` §1.1 solo lo admite ante una coordinación real
  entre casos de uso; aquí la coordinación es de un consumidor con un componente contable, no entre
  dos casos de uso independientes, y un Orchestrator no puede calcular con el Model.

---

## R-02 — Forma de la respuesta HTTP

**Decisión**: una respuesta con **tres objetos de nivel superior** —`summary`, `positions` y
`pagination`—, modelada como `record` inmutables: `PortfolioResponseDTO`,
`PortfolioSummaryResponseDTO`, `PortfolioPositionResponseDTO` y `PortfolioPaginationResponseDTO`.

**Fundamento**: `spec.md` FR-004 y CL-32 fijan esa estructura y prohíben mezclar indicadores
financieros con metadatos de paginación. `docs/backend/conventions.md` exige que los DTOs de contrato
HTTP sean `record`. Los nombres de campo siguen la convención ya vigente en el mercado
(`camelCase`, `long` para importes en centésimas).

**Alternativas descartadas**:

- **Un único record plano**: mezclaría el resumen, el detalle y la paginación en el mismo nivel, en
  contra de FR-004.
- **Heredar la forma de `PlayersPageResponseDTO`** (`content`, `page`, `size`, `totalElements`,
  `totalPages`): el enunciado funcional aprobado exige tres objetos de nivel superior, y esa forma no
  admite el resumen global.

---

## R-03 — Dónde se calculan los porcentajes por posición

**Decisión**: los porcentajes **por posición** —`unrealizedProfitLossPercentage` y
`portfolioWeightPercentage`— se calculan en esta feature; el **porcentaje global** y todos los
agregados contables los calcula el componente de SPEC-015.

| Porcentaje | Fórmula | Indefinido cuando |
| --- | --- | --- |
| `unrealizedProfitLossPercentage` | `HALF_UP_2( unrealizedProfitLoss × 100 / acquisitionCost )` | Costo de adquisición cero, o posición sin cotización |
| `portfolioWeightPercentage` | `HALF_UP_2( currentValue × 100 / totalCurrentValue )` | Valor total cero o indefinido, o posición sin cotización o cerrada |

**Fundamento**: `spec.md` FR-027 delega el porcentaje global en REQ-S2-20 y FR-028/FR-029 ownership
—propios— de esta feature (A6). Los denominadores son explícitos: el costo de adquisición de la
posición para el rendimiento individual y el valor total de **todas** las posiciones abiertas para el
peso (D24), no el de la página consultada. El modo de redondeo `HALF_UP` a dos decimales es el mismo
que usa SPEC-015 en el dominio contable, de modo que ambas features redondean igual.

**Alternativas descartadas**:

- **Calcular el porcentaje global aquí**: duplicaría la fórmula de CL-08 y su semántica de
  indefinidos, exactamente lo que la revisión de compatibilidad rechazó.
- **Pedir al componente un porcentaje por posición**: ampliaría el contrato contable de otra feature
  con una regla que no le pertenece.
- **Usar `double`**: la representación binaria imprecisa ya está descartada en el dominio de mercado.

---

## R-04 — Fuente de la cantidad de la posición y política ante divergencia

**Decisión**: la cantidad que la posición informa es la **tenencia persistida** en `player_tokens`, con
la reserva y la disponibilidad calculadas sobre ella (FR-012, D04). La posición del componente
contable y la tenencia persistida **se comparan** bajo la misma instantánea; si divergen, la
respuesta es un **error de integridad** (500), sin elegir silenciosamente una de las dos fuentes.

**Fundamento**: `spec.md` FR-012 exige `quantity = reservedQuantity + availableQuantity` sobre la
tenencia, y las reservas se derivan de las publicaciones: la cantidad, la reserva y la disponibilidad
pertenecen al mismo estado y deben ser coherentes entre sí. La cantidad del componente contable es una
**reconstrucción** a partir de operaciones y emisión, que el propio plan de SPEC-015 deja de
intención comparables con la tenencia bajo una instantánea única (RA-5). Divergir significa que el
estado del mercado es inconsistente; continuar devolvería cifras que no corresponden a ningún estado
real, en contra de FR-040 y AC13.

Esta comprobación **no** entra en conflicto con la regla de jugador eliminado (D38, FR-040): el borrado
físico de un jugador con operaciones confirmadas está bloqueado por la integridad referencial de
`market_transactions`, y un jugador sin operaciones no produce posición. El caso vigente es la
desactivación lógica, en la que el jugador permanece y sus datos se conservan; si alguna vez faltaran,
se informarían nulos sin inventar datos.

**Alternativas descartadas**:

- **Informar la cantidad reconstruida**: obligaría a derivar la reserva y la disponibilidad sobre una
  cifra que no es el estado de la tenencia, y rompería la identidad `quantity = reservada + disponible`.
- **Informar la cantidad de la tenencia sin comparar**: ocultaría una inconsistencia del dominio y
  produciría resultados financieros incoherentes con las cifras del componente.
- **Advertir la divergencia sin fallar**: una respuesta con cifras inconsistentes es peor que un
  error explícito.

---

## R-05 — Presentación de las posiciones cerradas, de los indefinidos y de los hechos nominales

**Decisión**: la respuesta **normaliza** la presentación de la posición:

| Campo | Posición abierta | Posición cerrada |
| --- | --- | --- |
| `currentUnitPrice`, `priceUpdatedAt` | Valor de la cotización vigente, o nulo si no hay | **Nulo** |
| `currentValue`, `unrealizedProfitLoss` | Valor o nulo si no hay cotización | **Nulo** |
| `averageUnitPrice` | Precio medio derivado de la cantidad abierta | **Nulo**: la cantidad abierta es cero |
| `totalProfitLoss` | Realizado más no realizado, o nulo si no hay cotización | **Resultado realizado acumulado** |
| `unrealizedProfitLossPercentage`, `portfolioWeightPercentage` | Valor o nulo según la regla | **Nulo** |
| `valuationStatus` | `VALUED` o `UNVALUED` | `NOT_APPLICABLE` |
| `lastTransactionAt` | Instante de la última operación confirmada, o **nulo** si la posición no tiene ninguna | Instante de la última operación confirmada |

**Fundamento**: `spec.md` FR-020 exige que las posiciones cerradas no informen cotización vigente,
y FR-022 fija los tres estados de valoración; D06 establece que la posición cerrada conserva su
resultado realizado. La regla de datos del enunciado (tp/enunciado.md §3.4) sitúa el valor actual y el
resultado no realizado en la posición **abierta**, y la fuente de verdad contable coincide: una
posición cerrada no tiene valor actual ni resultado no realizado. `lastTransactionAt` es nulo cuando
no hay ninguna operación confirmada —una posición abierta solo por emisión— y ese nulo se propaga
tal cual, sin sustituirlo por el instante de la emisión.

**Alternativas descartadas**:

- **Propagar sin normalizar**: el componente puede entregar un precio para una posición cerrada o
  valores calculados sobre una cantidad abierta nula; la respuesta debe ser coherente con su
  contrato, no con la aritmética del proveedor.
- **Informar cero en una posición cerrada**: confunde "no aplica" con "vale cero", y el enunciado
  distingue la ausencia de cotización de un valor.

---

## R-06 — Aislamiento transaccional de la consulta

**Decisión**: la consulta completa se ejecuta en **una sola transacción de solo lectura con aislamiento
`REPEATABLE READ`**, declarada en el método del Service.

**Fundamento**: `spec.md` FR-038 exige que tenencias, reservas, transacciones, resumen y posiciones
correspondan a una vista consistente. Con el aislamiento por defecto (`READ COMMITTED`), cada
sentencia ve una instantánea distinta: una venta confirmada entre la lectura de las operaciones y la
de las tenencias podría quedar fuera de la reconstrucción, de modo que las cantidades y los resultados
no corresponderían a ningún estado válido. `REPEATABLE READ` fija la instantánea en la primera
sentencia y todas las lecturas proceden del mismo estado confirmado del mercado, porque una operación
confirma su registro y sus movimientos en una sola transacción. El contrato de SPEC-015 exige
exactamente lo mismo del consumidor (R-15 de SPEC-015) y su plan lo advierte: Spring no cambia el
aislamiento de una transacción que ya participa.

`SERIALIZABLE` se descarta: puede producir fallos de serialización en lecturas. Los bloqueos
pesimistas se descartan: son la herramienta del orden global de escrituras de SPEC-011, no la de una
consulta, y abrirían la puerta a interbloqueos con el mercado.

**Alternativas descartadas**:

- **`READ COMMITTED`**: dos sentencias, dos instantáneas.
- **`SERIALIZABLE`**: fallos de serialización en una operación de solo lectura.
- **Cerrar la divergencia con bloqueos**: bloquearía las operaciones de mercado durante la consulta.

---

## R-07 — Orden de las operaciones de la consulta

**Decisión**: el pipeline es **agregar → filtrar → ordenar → paginar → mapear**, con la lectura del
estado de mercado y el cruce de cantidades antes del filtrado.

**Fundamento**: el porcentaje global y los agregados contables se calculan **siempre sobre el conjunto
completo** de posiciones (R-19 de SPEC-015, y el propio contrato lo exige: *"El consumidor la aplica
sobre el conjunto que recibe; jamás sobre una página"*). Por eso el resumen es idéntico con y sin
filtro y con cualquier página (AC14, AC15, AC23, AC24), y por eso `totalPositions` cuenta las
posiciones **abiertas** del conjunto completo con independencia del filtro aplicado (D05).

**Alternativas descartadas**:

- **Paginar antes de agregar**: produciría un porcentaje incoherente con las posiciones mostradas.
- **Agregar después de filtrar**: el resumen dejaría de representar el portfolio completo.

---

## R-08 — Acceso agregado al estado de mercado

**Decisión**: tres lecturas de conjunto y **ninguna por posición**: tenencias del propietario,
reservas del propietario agrupadas por jugador, y jugadores de la página en una sola consulta por
lote. Se añade el índice `player_tokens (owner_user_id, player_id)`.

**Fundamento**: FR-039 y AC35 prohíben el acceso individual por posición. El índice por propietario
sobre `player_tokens` fue diferido explícitamente porque "no existe consulta de portfolio"
(`specs/010-buy-tokens/data-model.md:82`): esta feature es esa consulta. El índice vigente
`idx_token_listings_seller (seller_user_id, created_at DESC)` cubre el filtro de las reservas del
vendedor, y la agrupación por jugador es un agregado sobre ese conjunto.

**Alternativas descartadas**:

- **Índice parcial adicional sobre `token_listings`**: la agrupación se resuelve con el índice
  vigente; añadirlo sería complejidad sin medición que lo justifique.
- **Consultar cada posición por separado**: contradice FR-039 de forma directa.
- **Reutilizar el cálculo del componente para la cantidad**: el componente no consulta `player_tokens`
  por decisión de SPEC-015 (R-08 de SPEC-015), y la cantidad de la tenencia es el estado que
  sustenta la reserva y la disponibilidad.

---

## R-09 — Mapeo HTTP de los errores

**Decisión**: el manejador global de errores incorpora el mapeo explícito de las excepciones de esta
feature y de las del componente contable:

| Excepción | Respuesta | `code` |
| --- | --- | --- |
| `InvalidPortfolioPageException` | 400 | `INVALID_PARAMETER_TYPE` |
| `InvalidPortfolioStatusException` | 400 | `INVALID_PARAMETER_TYPE` |
| `FinancialPositionIntegrityException` (SPEC-015) | 500 | `INTERNAL_ERROR` |
| `PortfolioIntegrityException` | 500 | `INTERNAL_ERROR` |
| Excepción de integración del componente contable | 503 | `QUOTE_SOURCE_UNAVAILABLE` |

**Fundamento**: `docs/backend/architecture.md` §1.7 exige que todo error controlado use
`ErrorResponseDTO`, y el manejador global **no tiene un caso genérico de `FootballMarketException`**
(RA-7 del plan de SPEC-015): una excepción nueva sin mapeo explícito cae en la respuesta por defecto
de Spring y rompe el contrato de error. Se reutiliza `INVALID_PARAMETER_TYPE`, ya vigente para
parámetros de consulta inválidos, en lugar de inventar un código; el mensaje distingue el caso. Las
excepciones de integridad se traducen a un 500 genérico, sin detalles técnicos, porque revelar la
inconsistencia interna ayudaría a reconstruir datos ajenos.

**Dependencia de coordinación**: el 503 requiere que el fallo técnico de la fuente de cotizaciones sea
distinguible de cualquier otra falla de integración. El contrato de SPEC-015 lo describe como
"excepción de integración"; conviene que su implementación use un tipo propio. Si permanece genérica,
esta feature traduce `IntegrationException` a 503, lo que es coherente porque la única integración
implicada en la consulta es la fuente de cotizaciones.

**Alternativas descartadas**:

- **Modificar `GlobalExceptionHandler` desde SPEC-015**: ampliaría el alcance de una feature que no
  expone endpoint.
- **Nuevo código `INVALID_PORTFOLIO_PAGE`**: no hay una convención aplicable y el proyecto ya usa
  `INVALID_PARAMETER_TYPE` para lo equivalente.
- **Degradar el fallo técnico a valoración incompleta**: confundiría indisponibilidad con ausencia de
  cotización, en contra de D10 y FR-023.

---

## R-10 — Dependencia de implementación del componente contable

**Decisión**: esta feature **consume** `FinancialPositionService` por su interfaz y **no implementa**
ninguna regla contable. La implementación de las cifras financieras queda secuenciada después de la de
SPEC-015.

**Fundamento**: FR-014 y CL-12 prohíben una implementación contable paralela, y el propio contrato de
SPEC-015 declara que su componente es la frontera entre ambas features. La separación es además una
ventaja: todo lo que no es contable —contrato HTTP, forma de la respuesta, lectura de tenencias y
reservas, datos del jugador, orden, paginación y porcentajes por posición— se puede diseñar e
implementar sin la dependencia.

**Estado verificado**: los artefactos de diseño de SPEC-015 están publicados y su implementación
integrada en `dev`, de modo que el contrato ya no es provisional: la interfaz `positionsOf`, la
agregación `FinancialPositionCalculator.aggregate` y los hechos por posición existen con la forma
consumida aquí. Esta feature sigue consumiendo **capacidades** y no firmas concretas, y el
componente contable obtiene las cotizaciones **en lote** mediante `QuoteSource.currentQuotes`, por lo
que el acceso a cotizaciones no crece con la cantidad de jugadores distintos ni se ejecuta en esta
feature.

**Alternativas descartadas**:

- **Reimplementar el cálculo contable en esta feature**: contradice FR-014 de forma directa.
- **Un endpoint contable en SPEC-015 para consultar las posiciones**: ampliaría el alcance de SPEC-015
  con un endpoint que el enunciado asigna a esta feature.

---

## R-11 — Dependencia de la matriz de dependencias de la arquitectura

**Decisión**: esta feature **no introduce** por su cuenta la relación Service → Service: la consume
como dependencia ya autorizada, documentada por SPEC-015 en `docs/backend/architecture.md` §1.3 y §5 e
integrada en `dev`.

**Fundamento**: la matriz de dependencias de la arquitectura enumera hoy Service → Service de forma acotada
—*«otro Service exclusivamente para la integración de portfolio descrita en §5»*— y §5 documenta los
componentes contables, su contrato y la exigencia de `REPEATABLE READ` en el consumidor. La aprobación
humana de esa actualización quedó registrada en SPEC-015 (RA-1 de SPEC-015, `research.md` R-01) y su
edición está integrada en `dev`. El `Constitution Check` de esta feature puede por ello declararse PASS
absoluto: el diseño consume un componente de otra feature por una vía ya contemplada en la matriz.

**Alternativas descartadas**:

- **Documentar la relación en esta feature**: duplicaría la edición de un archivo que otra feature ya
  modifica, con riesgo de conflicto.
- **Evitar el componente contable y calcular aquí**: contradice FR-014.

---

## R-12 — Idioma, nomenclatura y convenciones

**Decisión**: identificadores, paquetes, métodos y campos en inglés; documentación, Javadoc de clases
públicas y mensajes de error en español. Los DTOs siguen el sufijo vigente `ResponseDTO` y los
mapeadores el nombre `Mapper` con constructor privado y métodos `static`.

**Fundamento**: Constitución, sección *Idioma*; `docs/backend/conventions.md` exige Javadoc en clases
públicas y en todo método cuyo contrato no sea evidente, y `this.` para acceder a miembros de
instancia en código productivo.

---

## R-13 — Estrategia de pruebas

**Decisión**: cobertura en las categorías que `docs/backend/testing.md` §2 asigna, con `@Nested`
obligatorio y una única etiqueta literal por clase contenedora.

| Categoría | Qué prueba | Etiqueta |
| --- | --- | --- |
| Controller | Ruta, autenticación, validación de parámetros, forma de la respuesta, códigos de error y no exposición de carteras ajenas | `lightweight` |
| Service | Agregación sobre el conjunto completo, filtro, orden, paginación, porcentajes por posición, divergencia de cantidades, nulos de posiciones cerradas, ausencia de cotización y propagación del fallo técnico | `integration` |
| Repository | Consulta de tenencias del propietario, consulta agrupada de reservas y lectura por lote de jugadores | `integration` |

El test de Service usa el componente contable **real** de SPEC-015 una vez integrado, porque el doble
ocultaría precisamente la semántica de indefinidos y la propagación del fallo técnico que esta feature
consume. Mientras la dependencia esté pendiente, puede aislarse con `@MockitoBean`, y esa condición
**debe quedar registrada** en la evidencia de la fase: una prueba con doble no acredita la regla
contable.

No hay pruebas de frontend: el frontend está exento de testing.

**Alternativas descartadas**:

- **Probar el portfolio solo por HTTP**: la capa Controller no demuestra la composición del caso de
  uso ni las reglas de agregación.
- **Probar el componente contable desde aquí**: pertenece a SPEC-015 y se prueba por su propia interfaz.

---

## R-14 — Estabilidad del contrato propio

**Decisión**: el contrato HTTP de esta feature queda en `contracts/portfolio-api.md` y en las
anotaciones OpenAPI del Controller, y refleja la especificación sin evolucionarla. Los hechos
nominales que llegan del componente contable —`openedAt`, `closedAt`, `lastTransactionAt` y los
recuentos por rol— se consumen con el nombre que esta feature les da, y su formato de presentación es
de esta feature; si esta feature cambiara la definición de alguno, el Model de SPEC-015 debería
alinearse (acoplamiento declarado y acotado).

**Fundamento**: Constitution §4 exige coherencia entre especificación, implementación, tests,
contratos y documentación, y las cuatro fuentes del contrato HTTP del proyecto deben permanecer
coherentes entre sí.

---

## R-15 — Rendimiento sin objetivos arbitrarios

**Decisión**: no se fijan umbrales de latencia. Se exige que la consulta resuelva con un número
acotado de consultas de conjunto y sin accesos por posición, y que la agregación se calcule una sola
vez sobre el conjunto completo. El coste del conjunto recibido es lineal y queda medido, no estimado.

**Fundamento**: `spec.md` CL-41 y `tp/enunciado.md` §5.3. El caso de mayor volumen es el superusuario,
cuyo conjunto de posiciones coincide con el tamaño del catálogo emitido: la medida debe ejecutarse
antes de decidir cualquier optimización adicional, y ninguna latencia se inventa sin medición.

---

## Conclusiones de la Fase 0

No queda ningún `NEEDS CLARIFICATION` sin resolver. Las decisiones R-01 a R-15 son técnicas: ninguna
modifica el alcance aprobado en `spec.md`, ninguna altera un requisito, un criterio de aceptación ni
una aclaración, y ninguna exige modificar otra SPEC.

Los tres puntos que esta feature había dejado abiertos en la revisión del plan están resueltos y
verificados:

1. **R-11**: la relación Service → Service está documentada en `docs/backend/architecture.md` §1.3 y §5 e
   integrada en `dev`. El `Constitution Check` de esta feature la registra como PASS.
2. **R-10**: el contrato del componente contable está publicado e integrado; su interfaz y su agregación
   coinciden con lo previsto. Se sigue consumiendo por capacidades y no por firmas.
3. **R-09**: el 503 por fallo técnico de cotizaciones lo provoca la excepción de integración específica
   que propaga el componente contable —`QuoteBatchUnavailableException`—, de modo que esta feature no
   necesita un handler genérico de `IntegrationException` ni altera el tratamiento de las integraciones de
   otras features.

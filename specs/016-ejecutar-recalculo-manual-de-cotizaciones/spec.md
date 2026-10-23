# Feature Specification: Cotización de tokens de jugadores con historial trazable

**Feature Branch**: `feat/016-ejecutar-recalculo-manual-de-cotizaciones` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `016-ejecutar-recalculo-manual-de-cotizaciones`, sin el prefijo de tipo.

**Created**: 2026-10-20

**Status**: Ready for implementation

**Input**: Calcular y persistir la cotización de los tokens de cada jugador, dejando historial consultable y trazable de la estrategia que produjo cada movimiento.

**Nota de identidad**: esta spec se materializó con `--reuse` sobre la SPEC-016 ya existente (asignación registrada en `specs/assignments.md` y vínculo con la issue de seguimiento de recálculo manual). Se conserva el identificador, el directorio y la rama, y el alcance queda ampliado: la funcionalidad ahora incluye el cálculo y la persistencia de la cotización, la inicialización del mercado, las consultas de historial, vigencia, fecha y ranking, y la integración con la compra y la publicación de venta. La etapa de periodicidad programada y su agenda siguen fuera de alcance.

**Trazabilidad**: cubre `REQ-S2-11-A` (ejecución del recálculo de cotizaciones, de forma parcial: la ejecución y su persistencia, no la agenda), `REQ-S2-03` (consultar la cotización actual), `REQ-S2-04` (consultar la cotización a una fecha dada), `REQ-S2-05` (consultar el historial de cotizaciones) y `REQ-S2-08` (ranking de jugadores, de forma parcial: por precio vigente) de `tp/entregas.md`. Resuelve `tp/enunciado.md` §3.2, §3.3, §4 y §9.

## Clarifications

### Session 2026-10-20

- Q: ¿Qué es exactamente la «fecha de vigencia» de una cotización: la fecha de la valuación utilizada o la fecha del período de cotización? → A: La fecha de vigencia es la fecha de la valuación utilizada. La fecha de la valuación de origen se conserva como dato de auditoría y, cuando el jugador no tiene valuación para la estrategia elegida, la fila arrastrada toma como fecha de vigencia la fecha de la cotización (fecha del período de ejecución).
- Q: ¿Cómo se evita que la reejecución de una misma fecha de cotización duplique o altere el resultado? → A: Existe como máximo una cotización por jugador, fecha de vigencia y origen, y una ejecución solo escribe la fila de un jugador cuando la fecha de vigencia calculada es estrictamente posterior a su fecha de vigencia de referencia —la de su última fila `INITIAL` o `RECOMPUTED`—, regla que aplica tanto a las filas recalculadas como a las arrastradas. Una fecha igual o anterior no reprecia ni reescribe: el precio vigente se conserva sin saltos. Además, cada fecha de cotización admite como máximo una ejecución, de modo que una segunda ejecución el mismo día se rechaza con un error controlado, sin escribir filas ni alterar el historial (FR-060).
- Q: ¿Puede un jugador sin valuación recibir una cotización? → A: Sí, toda fila de ese jugador se marca como precio arrastrado (`CARRIED_FORWARD`): el precio vigente se repite, el multiplicador aplicado es 1 y no existe score.
- Q: ¿Qué distingue al precio arrastrado (`CARRIED_FORWARD`) del precio recalculado en un modelo de nivel absoluto? → A: En el modelo compuesto de esta feature, arrastrar es literalmente ausencia de cambio: no hay valuación, no hay score, no hay multiplicador y el multiplicador implícito es 1, por lo que el precio se mantiene. En un modelo de nivel, un precio arrastrado podría diferir del precio recalculado. Son significados distintos y el marcador de origen de la fila (`CARRIED_FORWARD` frente a `RECOMPUTED`) los distingue sin ambigüedad. Una fila `RECOMPUTED` con score exactamente 0,5 tampoco cambia el precio, pero MUST NOT marcarse como `CARRIED_FORWARD`, porque la ausencia de score y la existencia de un score neutro son hechos distintos y auditables.
- Q: ¿Una fila arrastrada cuenta como cotización vigente para las consultas y el ranking? → A: Sí, es la última cotización persistida del jugador y por tanto su cotización vigente, aunque su origen sea arrastrado.
- Q: ¿Qué ocurre con un jugador cuya valuación de origen es anterior a la fecha de vigencia de referencia de su última cotización repreciada? → A: No se reprecia ni se reescribe su historial: su precio vigente se conserva y la fila no se escribe. El cambio de estrategia, en particular, nunca produce un salto de precio.
- Q: ¿Qué valores por defecto adopta el control de variabilidad? → A: Factor de ajuste `k` = 0,10; tope de variación máxima por período = 0,10 (10 %); piso mínimo de precio = 1 centésima de crédito; ancla inicial = 100 centésimas (1 crédito) por token. Todos son configurables y pueden ajustarse en cada ejecución.
- Q: ¿Cómo se expresa y redondea el precio? → A: En centésimas de crédito, con redondeo half-up al entero de centésimas. El multiplicador se calcula con precisión interna completa y se persiste redondeado a seis decimales.
- Q: ¿Quién puede determinar la estrategia de una ejecución? → A: El operador, de forma explícita en la ejecución. El usuario final nunca selecciona ni percibe la estrategia, y el catálogo de estrategias no se amplía en esta feature.
- Q: ¿Qué se hace con la fuente de cotización simulada? → A: Se retira del sistema; la compra y la publicación de venta consumen el repositorio canónico real de cotizaciones a través del puerto de lectura de cotización ya existente.
- Q: ¿Se reprecia a un jugador que no obtuvo valuación nueva, es decir, cuyas métricas no cambiaron? → A: No. Como la fecha de vigencia es la fecha de la valuación, un jugador sin valuación posterior a su fecha de vigencia de referencia conserva su precio y no genera fila: no se reaplica un score que no cambió. El re-ajuste solo se produce cuando el jugador dispone de una valuación más reciente que esa fecha de vigencia de referencia, y una fila arrastrada previa no la modifica (FR-027).
- Q: ¿Qué ocurre si la valuación nueva de un jugador llega con una fecha igual o anterior a la de su fecha de vigencia de referencia? → A: No se reprecia ni se reescribe el historial. El precio del jugador avanza con la siguiente valuación cuya fecha sea posterior a esa fecha de vigencia de referencia, y la ejecución informa de los jugadores no repreciados.
- Q: ¿Qué nombre de marcador se usa para el origen de cada fila? → A: `INITIAL`, `RECOMPUTED` y `CARRIED_FORWARD`. El marcador `RECOMPUTED` identifica las filas cuyo precio surge de aplicar el multiplicador a un score disponible.
- Q: ¿Qué estrategia registra una fila de precio arrastrado? → A: La que produjo el precio arrastrado, heredada de la cotización vigente cuyo precio se repite. Registrar en su lugar la estrategia de la ejecución haría mentir la trazabilidad, porque esa estrategia no produjo el precio. La estrategia usada en cada corrida queda en el resumen de la ejecución y el instante de cálculo identifica la corrida.
- Q: ¿En qué rango se puede parametrizar el factor de ajuste `k`? → A: Siempre dentro del rango cerrado [0,10; 1,00], con valor por defecto 0,10. Un valor fuera del rango se rechaza con error controlado. El tope de variación por período sigue siendo el control que acota el movimiento real: con `k` por encima del tope, es el tope el que limita el ajuste efectivo.
- Q: ¿Debe la consulta de cotización exponer de dónde viene el score que sustenta el precio? → A: Sí, además de los datos de auditoría debe exponer la fecha de la valuación de origen, para que se pueda conocer la antigüedad del score detrás del precio vigente.
- Q: ¿Cómo se rechaza una operación de mercado cuando el mercado todavía no fue inicializado? → A: Con un chequeo explícito y un error propio, distinguible del error de cotización no disponible, aplicado tanto a la compra como a la publicación de venta y antes de cualquier movimiento de recursos. El mercado se considera inicializado cuando existe al menos una cotización de origen `INITIAL`. Se descarta la alternativa de dejar que el mercado degrade al error de cotización no disponible, porque esa ausencia significa algo distinto y el operador debe poder distinguirlas.
- Q: ¿De dónde sale la «fecha de cotización» de una ejecución? → A: Es siempre la fecha del día en que corre la ejecución; el operador no la fija. La fila arrastrada sí se escribe, con esa fecha de cotización como fecha de vigencia, copiando el precio de la última cotización del jugador, con multiplicador 1, sin score, con origen `CARRIED_FORWARD` y con la estrategia heredada de la cotización cuyo precio se repite. Esa escritura solo procede cuando la fecha de cotización es estrictamente posterior a la fecha de vigencia de referencia del jugador —la de su última fila `INITIAL` o `RECOMPUTED`, que las filas arrastradas no alteran— (FR-027), lo que, junto con la unicidad de una sola ejecución por fecha de cotización (FR-060), impide que la fila arrastrada se duplique. La fecha de vigencia de esa fila no es la de su última cotización, porque no puede coexistir con ella en el historial bajo la unicidad de FR-026.
- Q: ¿El resumen de cada ejecución de cotización se persiste o solo se devuelve en la respuesta? → A: Se persiste. Cada ejecución queda registrada con su fecha de cotización, la estrategia y su versión, el factor de ajuste, el tope y el piso aplicados y los contadores del resultado, y es consultable por fecha de cotización. Así la trazabilidad de FR-019 y la regla de una sola ejecución por fecha de cotización de FR-060 tienen un soporte verificable y no dependen del log de la aplicación.
- Q: Si al procesar un jugador la ejecución falla, ¿la ejecución sigue con los demás jugadores o se aborta? → A: La ejecución continúa con los demás jugadores. La atomicidad es por jugador: la cotización de un jugador se persiste de forma coherente o no se persiste, y el fallo de uno no interrumpe la ejecución ni invalida las cotizaciones de los demás. El resumen de la ejecución informa cuántos jugadores fallaron, sin exponer el detalle interno.
- Q: ¿Puede haber más de una ejecución de cotización en la misma fecha de cotización? → A: No. Cada fecha de cotización admite como máximo una ejecución. Una segunda ejecución en la misma fecha se rechaza con un error controlado (`EXECUTION_ALREADY_EXISTS`), no escribe filas, no altera el historial ni el resumen ya registrado. Las valuaciones nuevas que lleguen durante ese mismo día se reflejarán en la ejecución del día siguiente, cuya fecha de vigencia calculada será posterior a la vigente. Con esta regla, la fila arrastrada no puede duplicarse en la misma fecha y los contadores de cada ejecución son inequívocos.
- Q: ¿Qué error se devuelve cuando una ejecución de cotización es inválida por más de una causa a la vez? → A: El de mayor precedencia, con un orden único y determinista: estrategia fuera del catálogo, después mercado no inicializado, después factor de ajuste fuera de rango y, por último, el resto de parámetros inválidos. La existencia previa de una ejecución para la fecha de cotización se evalúa al final. Un rechazo por cualquiera de estas causas no escribe filas ni registra resumen de ejecución.
- Q: ¿Qué requisitos no funcionales de observabilidad y rendimiento aplican a esta feature? → A: Los que ya están vigentes en el proyecto. La ejecución y las consultas se apoyan en los registros estructurados, la correlación de solicitudes, las métricas y los health checks exigidos por el enunciado, sin reimplementarlos. Esta feature no fija objetivos de rendimiento propios ni introduce mecanismos nuevos de caché u observabilidad, y no reimplementa ni amplía la capa de caché ya vigente en el proyecto.
- Q: ¿Puede ejecutarse un recálculo antes de que el mercado esté inicializado? → A: No. La ejecución se rechaza con un error controlado de mercado no inicializado, el mismo criterio y el mismo error propio que aplica a las operaciones de mercado de FR-043, y no escribe ninguna fila. El ajuste parte del último precio vigente (FR-005), de modo que sin mercado inicializado no hay precio de partida y la ejecución no puede producir resultado.

### Session 2026-10-21 (remediación posterior a `/speckit.analyze`)

- Q: ¿Una fila arrastrada impide un reprecio posterior? → A: No. La fecha de vigencia de referencia de un jugador es la fecha de vigencia de su última fila `INITIAL` o `RECOMPUTED`; las filas `CARRIED_FORWARD` no la modifican. Así, una fila arrastrada escrita el día D no oculta ninguna valuación posterior y el jugador puede repreciarse. Cuando la valuación tiene la misma fecha de vigencia que una fila arrastrada previa del mismo jugador, ambas filas coexisten y la fila `RECOMPUTED` pasa a ser su cotización vigente, por ser la que incorpora el score; la fila arrastrada se conserva como registro histórico del período sin cambio (FR-026, FR-027).
- Q: ¿Qué se registra como precio anterior de una fila? → A: El precio de la cotización vigente del jugador inmediatamente antes de escribir esa fila, y no el de una fila posterior (FR-028).
- Q: ¿Cómo se ordenan dos filas con la misma fecha de vigencia? → A: Por fecha de vigencia descendente y, a igualdad de fecha, por instante de cálculo descendente, de modo que la fila `RECOMPUTED` precede a la arrastrada.
- Q: ¿Dónde queda registrado el tope de variación por período? → A: En el resumen persistido de la ejecución que produjo la fila, no en la fila. La fila conserva el multiplicador ya acotado y los parámetros de la corrida se consultan en su resumen, de modo que exista una única fuente de verdad por parámetro (FR-012, FR-029).
- Q: ¿Cuál es el rango y la unidad del tope de variación por período? → A: Es una fracción del precio vigente, dentro del rango cerrado (0; 1], es decir de 0 % a 100 %, con valor por defecto 0,10 (10 %). Un valor fuera de ese rango se rechaza con el error controlado `INVALID_VARIATION_CAP` (FR-012).
- Q: ¿Qué jugadores son cotizables? → A: Los que tienen al menos una métrica informada en su último registro de métricas persistidas. Un registro de estadísticas sin métricas informadas no hace cotizable al jugador (FR-003).
- Q: ¿Qué ocurre con dos ejecuciones concurrentes de la misma fecha de cotización? → A: Solo una se resuelve como ejecución; la otra se rechaza con el error controlado `EXECUTION_ALREADY_EXISTS`, sin filas adicionales ni cambio del resumen existente (FR-060).
- Q: ¿Qué significa exactamente el número de jugadores cotizados de una ejecución? → A: Es la población cotizable evaluada en la ejecución, incluidos los jugadores que no se repreciaron; el número de jugadores no repreciados es un subconjunto de ese (FR-061).
- Q: ¿En qué zona horaria se determinan la fecha de cotización y la fecha de vigencia de una fila arrastrada? → A: En la fecha UTC del día en que corre la operación (FR-024, FR-060).
- Q: ¿Puede repetirse la inicialización con una fecha de mercado distinta de la vigente? → A: No. Se rechaza con `MARKET_ALREADY_INITIALIZED`, porque escribir una segunda fila inicial por jugador violaría FR-034. Repetir con la misma fecha de mercado sigue siendo idempotente (FR-032, FR-033).
- Q: ¿Con qué criterio se autoriza la ejecución de cotización? → A: Con un JWT válido, el criterio de autorización vigente para las operaciones administrativas del mercado, sin introducir roles nuevos (FR-059).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inicializar el mercado con el ancla de un crédito (Priority: P1)

Como operador del mercado, quiero inicializar el mercado una única vez para que todos los jugadores cotizables tengan un precio de partida de 1 crédito y ninguna operación del mercado se ejecute antes de que el mercado exista.

**Why this priority**: El ancla de 1 crédito es el punto de partida del enunciado y la precondición de todo lo demás: sin mercado inicializado, ni la cotización ni la compra ni la venta tienen un precio válido.

**Independent Test**: Puede verificarse inicializando el mercado sobre un catálogo con métricas, comprobando que cada jugador cotizable obtiene exactamente una fila inicial a 100 centésimas, que la operación es idempotente al repetirla y que una segunda inicialización posterior a cotizaciones más recientes se rechaza.

**Acceptance Scenarios**:

1. **Given** un mercado sin cotizaciones y un catálogo con jugadores que poseen métricas, **When** el operador inicializa el mercado, **Then** cada jugador cotizable queda con una única cotización inicial de 100 centésimas, con fecha de vigencia igual a la primera fecha del mercado, sin estrategia asociada y marcada como inicial.
2. **Given** un mercado ya inicializado en esa misma fecha, **When** se repite la inicialización, **Then** el resultado es el mismo conjunto de cotizaciones, sin duplicados ni diferencias.
3. **Given** un mercado con cotizaciones en fechas de vigencia posteriores a la fecha solicitada, **When** se intenta inicializar, **Then** la operación se rechaza y la historia existente no se altera.
4. **Given** un mercado no inicializado, **When** un usuario intenta comprar tokens o publicar una venta, **Then** la operación se rechaza de forma controlada indicando que el mercado no está inicializado.

---

### User Story 2 - Recalcular y persistir la cotización con re-ajuste compuesto (Priority: P1)

Como operador del mercado, quiero ejecutar un recálculo de cotizaciones con una estrategia y un factor de ajuste elegidos por mí, para que el precio de cada token evolucione con el desempeño de su jugador y quede registrado con la trazabilidad de lo que lo produjo.

**Why this priority**: Es el corazón de la funcionalidad y la fuente de verdad que el mercado consumirá después; sin ella no existe cotización real ni historial auditable.

**Independent Test**: Puede verificarse con un catálogo con métricas y valuaciones de ambas estrategias, ejecutando el recálculo una vez por fecha de cotización y comprobando el precio resultante de cada jugador, la marca de arrastrado de los no valuados, la estrategia y la versión registradas y la ausencia de duplicados, y comprobando que una segunda ejecución en la misma fecha de cotización se rechaza sin escribir filas.

**Acceptance Scenarios**:

1. **Given** un jugador con valuación vigente de la estrategia elegida, **When** se ejecuta el recálculo, **Then** su precio se obtiene multiplicando su precio vigente por el multiplicador `1 + k × (score − 0,5)`, acotado por el tope de variación, y la fila registra precio anterior, multiplicador, score, fecha de la valuación de origen, estrategia y versión.
2. **Given** un mercado inicializado en una fecha anterior y un jugador con métricas pero sin valuación para la estrategia elegida, **When** se ejecuta el recálculo, **Then** su precio vigente se repite sin ajuste, la fila queda marcada como `CARRIED_FORWARD`, el multiplicador es 1, no existe score y su fecha de vigencia es la fecha de cotización de la ejecución.
3. **Given** un jugador con valuación cuya fecha es anterior a la de su fecha de vigencia de referencia, **When** se ejecuta el recálculo, **Then** su precio no se modifica, no se escribe ninguna fila y el motivo queda informado.
4. **Given** dos ejecuciones consecutivas con estrategia distinta, **When** se comparan los precios, **Then** ningún token cambia de precio por el solo cambio de estrategia; la nueva estrategia solo produce el ajuste siguiente, que se materializa en la ejecución posterior en la que exista una valuación posterior a la fecha de vigencia de referencia del jugador (FR-009, FR-027).
5. **Given** una ejecución con un factor de ajuste o un tope de variación distintos, **When** se ejecuta, **Then** la magnitud del movimiento de los precios cambia en consecuencia y cada fila conserva los parámetros aplicados.
6. **Given** un precio que por el ajuste quedaría por debajo del piso mínimo, **When** se ejecuta el recálculo, **Then** el precio persistido es el piso mínimo y nunca es nulo ni negativo.
7. **Given** un mercado no inicializado, **When** el operador ejecuta el recálculo, **Then** la ejecución se rechaza con el error de mercado no inicializado y no escribe ninguna fila.
8. **Given** una ejecución de cotización finalizada, **When** se consulta su resumen por fecha de cotización, **Then** devuelve la fecha de cotización, la estrategia y su versión, el factor de ajuste, el tope y el piso aplicados y los contadores del resultado, incluido el número de jugadores que fallaron.
9. **Given** una ejecución de cotización ya registrada para la fecha de cotización del día, **When** el operador lanza otra ejecución en esa misma fecha, **Then** la segunda ejecución se rechaza con un error controlado, no escribe filas y no altera el resumen ya registrado.
10. **Given** una solicitud de ejecución con más de una causa de rechazo, **When** se envía, **Then** el error devuelto corresponde a la causa de mayor precedencia conforme al orden definido en FR-066.
11. **Given** un jugador cuya fila arrastrada tiene la misma fecha de vigencia que una valuación posterior, **When** se ejecuta el recálculo, **Then** se escribe una fila `RECOMPUTED` con esa fecha de vigencia, coexiste con la fila arrastrada y pasa a ser su cotización vigente, y la fila arrastrada se conserva en el historial (FR-026, FR-027).

---

### User Story 3 - Consultar el historial y la cotización vigente e histórica (Priority: P2)

Como usuario del mercado, quiero consultar el historial de cotizaciones de un jugador, su cotización vigente y su cotización en una fecha concreta, para saber cuánto vale un token ahora y cuánto valía antes.

**Why this priority**: La consulta es el contrato público que consumen el frontend, el ranking y las auditorías; sin ella, la cotización persistida no es observable.

**Independent Test**: Puede verificarse con un catálogo cotizado en varias fechas: el historial devuelve las filas ordenadas por fecha, la cotización vigente devuelve la última, y la consulta por fecha devuelve la última cotización cuya fecha de vigencia no supera la fecha solicitada.

**Acceptance Scenarios**:

1. **Given** un jugador con cotizaciones en varias fechas de vigencia, **When** se consulta su historial, **Then** devuelve todas sus cotizaciones ordenadas de la más reciente a la más antigua, con precio, estrategia, versión, score, precio anterior, multiplicador, origen e instante de cálculo.
2. **Given** un jugador con cotizaciones, **When** se consulta su cotización vigente, **Then** devuelve la última cotización persistida, con el instante de cálculo de esa cotización y no el de la lectura.
3. **Given** un jugador con cotizaciones en varias fechas, **When** se consulta su cotización a una fecha concreta, **Then** devuelve la última cotización cuya fecha de vigencia no supera esa fecha.
4. **Given** un jugador sin cotizaciones, **When** se consulta su cotización vigente, **Then** la consulta indica que no hay cotización disponible.

---

### User Story 4 - Obtener el ranking de jugadores por precio (Priority: P3)

Como usuario del mercado, quiero ver los jugadores ordenados por su precio vigente, de mayor a menor, para identificar los tokens más caros del mercado.

**Why this priority**: Es una lectura derivada de las cotizaciones vigentes, útil pero no imprescindible para operar el mercado; puede apoyarse en el mismo repositorio sin reglas propias.

**Independent Test**: Puede verificarse con un catálogo cotizado con precios distintos, comprobando que el ranking incluye una sola entrada por jugador cotizable, ordenada de mayor a menor, y que el precio de cada entrada coincide con su cotización vigente.

**Acceptance Scenarios**:

1. **Given** un catálogo cotizable con cotizaciones vigentes de precios distintos, **When** se solicita el ranking, **Then** devuelve los jugadores de mayor a menor precio, con una sola entrada por jugador.
2. **Given** jugadores sin cotización, **When** se solicita el ranking, **Then** no aparecen en el ranking porque no son cotizables para el mercado.
3. **Given** un jugador cuyo precio vigente es `CARRIED_FORWARD`, **When** se solicita el ranking, **Then** aparece con su precio repetido, que es su precio vigente.

---

### User Story 5 - Operar el mercado con la cotización persistida (Priority: P2)

Como usuario del mercado, quiero comprar tokens y publicar ventas al precio vigente realmente persistido, para que el mercado use un precio trazable y no un valor simulado.

**Why this priority**: Cierra el circuito entre la cotización y el mercado; sin ella la cotización persistida queda aislada del producto, aunque el resto de la funcionalidad sea valioso por sí mismo.

**Alcance sobre el mercado**: la compra, la publicación de venta, el congelamiento del precio publicado, la auditoría del precio aplicado y los errores de negocio del mercado **ya están implementados por SPEC-010 y SPEC-011**, y esta feature MUST NOT modificar su comportamiento. El alcance de esta historia se limita a tres deltas: (1) que el puerto de lectura de cotización existente se resuelva contra el repositorio canónico real de cotizaciones, en lugar de la fuente simulada; (2) que la fuente simulada sea retirada del sistema; y (3) que las operaciones de mercado se rechacen de forma controlada y distinguible cuando el mercado todavía no fue inicializado.

**Independent Test**: Puede verificarse con un mercado inicializado y cotizado: la compra consume el precio vigente persistido y su instante de cálculo, la publicación de venta congela ese precio en el momento de publicarse, el recálculo posterior no altera saldos, tenencias ni transacciones, y una operación sobre un mercado no inicializado se rechaza con un error propio.

**Acceptance Scenarios**:

1. **Given** un jugador con cotización vigente persistida, **When** un usuario compra tokens, **Then** la operación usa el precio vigente persistido y el instante de cálculo de esa cotización.
2. **Given** un jugador sin cotización, **When** un usuario intenta comprar tokens, **Then** la operación se rechaza con el error de cotización no disponible ya definido para el mercado.
3. **Given** una publicación de venta con precio vigente, **When** se publica y luego se recalculan las cotizaciones, **Then** el precio publicado permanece congelado y la compra posterior no reprecia la publicación.
4. **Given** un mercado cotizado, **When** se ejecuta un recálculo, **Then** los saldos de créditos, las tenencias de tokens y las transacciones no cambian.
5. **Given** un mercado sin inicializar, **When** un usuario intenta comprar tokens o publicar una venta, **Then** la operación se rechaza con un error propio, distinguible del error de cotización no disponible, y sin mover saldos, tenencias ni transacciones.
6. **Given** una fuente de cotización real disponible, **When** arranca la aplicación, **Then** la fuente real es la activa y no existe ninguna fuente simulada en el sistema.

### Edge Cases

- Un jugador con métricas que nunca fue valuado con éxito es cotizable desde el arranque: recibe precio arrastrado, conserva su precio y sigue siendo consultable.
- Un jugador sin métricas no es cotizable: no recibe cotización inicial, no aparece en el ranking y no es operable.
- Un jugador con valuación cuya fecha de vigencia es anterior o igual a la de su fecha de vigencia de referencia no se reprecia; su precio se conserva y la ejecución lo informa sin escribir fila. Es el caso del jugador cuyas métricas no cambiaron: sin valuación nueva no hay score nuevo que aplicar.
- Una valuación que llega con una fecha no posterior a la fecha de vigencia de referencia del jugador (valuación tardía) no reprecia al jugador ni altera su historial: su precio avanza con la siguiente valuación cuya fecha sea posterior a esa referencia.
- Una fila arrastrada no bloquea ningún reprecio posterior: las filas `CARRIED_FORWARD` no alteran la fecha de vigencia de referencia del jugador, de modo que una valuación posterior a esa referencia reprecia con normalidad aunque su fecha coincida con la de la fila arrastrada (FR-026, FR-027).
- Cuando una fila `RECOMPUTED` y una fila `CARRIED_FORWARD` del mismo jugador comparten fecha de vigencia, ambas se conservan en el historial y la `RECOMPUTED` es la cotización vigente del jugador, por ser la que incorpora el score (FR-026, FR-037).
- Un jugador con valuación de una estrategia y no de la otra: con la estrategia elegida sin valuación, su fila es `CARRIED_FORWARD` y hereda la estrategia del precio arrastrado; con la estrategia con valuación, su fila es `RECOMPUTED`.
- Un precio que por el ajuste descendente quedaría por debajo del piso mínimo se eleva al piso; un precio que por el ajuste ascendente superaría el tope de variación se acota.
- Un ajuste exactamente neutro (score igual a 0,5) deja el precio sin cambios y la fila se registra como `RECOMPUTED`, no como `CARRIED_FORWARD`: el precio puede no cambiar por dos motivos distintos y auditables.
- Una segunda ejecución en la misma fecha de cotización se rechaza con un error controlado, no escribe filas y no altera el resumen ya registrado; las valuaciones nuevas llegadas ese mismo día se aplican en la ejecución del día siguiente, cuya fecha de vigencia calculada es posterior a la vigente.
- Una ejecución que aucun jugador puede repreciar (todas las fechas de vigencia son anteriores o iguales) no modifica ninguna fila y su resumen lo informa.
- La inicialización solicitada en una fecha anterior a cotizaciones existentes se rechaza y no altera la historia.
- Una ejecución de recálculo solicitada sobre un mercado no inicializado se rechaza con el error propio de mercado no inicializado y no escribe ninguna fila.
- Un reintento de la inicialización tras un fallo parcial no puede dejar filas iniciales duplicadas para un mismo jugador.
- Una caída durante una ejecución no puede dejar una fila a medio escribir: la cotización de un jugador se persiste de forma coherente o no se persiste. Si el fallo es de un jugador concreto, la ejecución continúa con los demás y el resumen informa cuántos fallaron, sin exponer el detalle interno.
- Un jugador que falló de forma individual en una ejecución no puede repreciarse dentro de esa misma fecha de cotización, porque cada fecha admite una sola ejecución (FR-060); se reprecia en la ejecución de un día posterior, cuando exista una valuación posterior a su fecha de vigencia de referencia.
- El instante de cálculo de una cotización consumida es el de esa cotización, nunca el de la lectura; una cotización antigua sigue siendo válida mientras sea la última vigente.

## Requirements *(mandatory)*

### Functional Requirements

#### Universo de la cotización

- **FR-001**: Solo los jugadores que poseen métricas persistidas son cotizables. Los jugadores del catálogo sin métricas MUST NOT recibir cotización, MUST NOT aparecer en el ranking y MUST NOT ser operables en el mercado.
- **FR-002**: Todo jugador con métricas es cotizable desde el arranque del mercado, aunque nunca haya sido valuado con éxito. La cotizabilidad no depende de la existencia de una valuación.
- **FR-003**: La población cotizable MUST determinarse a partir de las métricas persistidas por SPEC-012 —jugadores con al menos una métrica informada en su último registro de métricas—, MUST NOT depender de la cobertura de las valuaciones y MUST NOT excluir a los jugadores que no fueron valuados. Un registro de métricas sin ninguna métrica informada MUST NOT hacer cotizable al jugador.

#### Modelo de re-ajuste compuesto

- **FR-004**: El precio de una cotización MUST NOT derivarse de un nivel absoluto del score. Cada ejecución MUST partir del último precio vigente del jugador y aplicar un ajuste proporcional a su score. En este modelo compuesto, arrastrar un precio significa literalmente ausencia de cambio: sin score no hay multiplicador, el multiplicador implícito es 1 y el precio se mantiene. En un modelo de nivel absoluto, un precio arrastrado podría diferir del precio recalculado. Son significados distintos y el marcador de origen de la fila MUST distinguirlos sin ambigüedad.
- **FR-005**: La fórmula de ajuste MUST ser `precioNuevo = precioVigente × (1 + k × (score − 0,5))`, donde `k` es el factor de ajuste de la ejecución y `score` es el score de la última valuación disponible del jugador para la estrategia elegida, sea cual sea su fecha. El desfase entre la fecha de las métricas y la fecha de la valuación MUST NOT invalidar la cotización. Que esa valuación sea la última disponible MUST NOT implicar por sí solo que el jugador se re-precie: el re-ajuste solo se produce cuando la valuación es posterior a la fecha de vigencia de referencia del jugador, conforme a FR-027.
- **FR-006**: El score MUST ser el resultado final de la estrategia elegida: el score posicional para `POSITIONAL_PERFORMANCE` y el GRL para `GENERAL_ATTRIBUTES`. El score crudo MUST NOT usarse como factor directo, porque drenaría el mercado.
- **FR-007**: El término `(score − 0,5)` MUST anclar el movimiento en la mediana de la población: el jugador mediano mantiene su precio, los superiores suben y los inferiores bajan.
- **FR-008**: Si un jugador no tiene valuación para la estrategia elegida, MUST NOT existir score ni ajuste: el precio vigente se repite, el multiplicador aplicado es 1 y la fila queda marcada con origen `CARRIED_FORWARD`.
- **FR-009**: Cambiar la estrategia activa MUST NOT re-preciar el mercado. La serie MUST continuar desde el último precio y la nueva estrategia MUST producir el siguiente ajuste. El precio de un token MUST NOT saltar por un cambio de configuración.
- **FR-010**: Una ejecución MUST usar el score persistido por SPEC-013 y MUST NOT recalcular valuaciones, extraer métricas ni realizar llamadas a proveedores externos.

#### Control de la variabilidad

- **FR-011**: El factor de ajuste `k` MUST ser un parámetro de configuración con valor por defecto y MUST poder ajustarse en cada ejecución, para permitir que los precios cambien con mayor o menor grado según el período. MUST parametrizarse dentro del rango cerrado [0,10; 1,00] en todas las ejecuciones; un valor fuera de ese rango MUST rechazarse con un error controlado, sin ejecutar ni modificar cotizaciones. El tope de variación por período MUST seguir siendo el control que acota el movimiento real: cuando `k` permite un movimiento mayor que el tope, es el tope el que limita el ajuste efectivo.
- **FR-012**: La ejecución MUST aplicar un tope de variación máxima por período, de modo que ninguna ejecución pueda producir un salto de precio fuera del rango permitido. El tope MUST acotar el multiplicador aplicado y MUST quedar registrado en el resumen persistido de la ejecución que produjo la fila (FR-061), de modo que el movimiento real de cada fila sea reconstruible a partir de su precio anterior, su multiplicador, su score y los parámetros de la corrida. El tope MUST expresarse como fracción del precio vigente dentro del rango cerrado (0; 1] —de 0 % a 100 %—; un valor fuera de ese rango MUST rechazarse con un error controlado `INVALID_VARIATION_CAP`, sin ejecutar ni modificar cotizaciones.
- **FR-013**: El precio MUST tener un piso mínimo configurable, siempre estrictamente mayor que cero, porque el mercado no admite precios nulos ni negativos. Ninguna cotización persistida MUST tener precio nulo, cero o negativo. El piso mínimo MUST quedar registrado en el resumen persistido de la ejecución que produjo la fila (FR-061).
- **FR-014**: El ancla inicial de todo el mercado MUST ser 1 crédito por token, equivalente a 100 centésimas de crédito.
- **FR-015**: El factor de ajuste, el tope de variación y el piso mínimo MUST tener valores por defecto (0,10; 0,10, equivalente a 10 %; 1 centésima respectivamente) y MUST ser ajustables por el operador en cada ejecución. Los valores por defecto MUST estar disponibles sin configuración explícita.
- **FR-016**: Los precios MUST expresarse en centésimas de crédito, MUST calcularse internamente con precisión completa y MUST persistirse como enteros con redondeo half-up. El multiplicador MUST persistirse redondeado a seis decimales.

#### Estrategia y trazabilidad

- **FR-017**: La estrategia de la ejecución MUST ser determinada por el operador de forma explícita. El usuario final MUST NOT seleccionar ni percibir la estrategia.
- **FR-018**: La estrategia solicitada MUST pertenecer al catálogo de estrategias ya configurado y versionado; una estrategia inexistente MUST rechazarse de forma controlada y MUST NOT crear una estrategia nueva.
- **FR-019**: Cada fila persistida que no sea inicial MUST registrar el nombre de la estrategia y su versión, de modo que quede trazada qué estrategia produjo esa cotización. En una fila `RECOMPUTED` se registra la estrategia de la ejecución, que es la que produjo el ajuste. En una fila `CARRIED_FORWARD` se registra la estrategia que produjo el precio arrastrado, heredada de la cotización vigente cuyo precio se repite, porque la estrategia de la ejecución no produjo ese precio y registrarla haría mentir la trazabilidad. La ausencia de score, de fecha de valuación de origen y de ajuste en una fila arrastrada queda expresada por el propio origen de la fila. La ausencia de estrategia en una fila arrastrada MUST limitarse al caso en que el precio arrastrado provenga de la fila inicial, que por definición no tiene estrategia asociada. La estrategia efectivamente usada en cada ejecución MUST constar en el resumen de esa ejecución y el instante de cálculo MUST identificar la corrida, de modo que la atribución de cada movimiento a una estrategia siga siendo reconstruible.
- **FR-020**: La fila inicial MUST existir sin estrategia asociada y MUST distinguirse del resto por su origen `INITIAL`, de modo que la ausencia de estrategia no se confunda con un dato perdido.
- **FR-021**: El origen de una fila MUST pertenecer al conjunto cerrado `INITIAL`, `RECOMPUTED` y `CARRIED_FORWARD`, con el significado siguiente: `INITIAL` es la fila de ancla de 1 crédito, sin estrategia ni score; `RECOMPUTED` es una fila cuyo precio surge de aplicar el multiplicador a un score disponible; `CARRIED_FORWARD` es una fila sin valuación para la estrategia elegida, sin score, con multiplicador 1 y con precio idéntico al precio vigente anterior. Una fila `RECOMPUTED` cuyo score es exactamente 0,5 deja el precio sin cambios, pero MUST NOT marcarse como `CARRIED_FORWARD`, porque la ausencia de score y la existencia de un score neutro son hechos distintos y auditables.

#### Persistencia e historial

- **FR-022**: La tabla de cotizaciones MUST ser el historial. Cada fila MUST ser un punto de la serie y MUST NOT existir una tabla separada de cotizaciones actuales.
- **FR-023**: Cada fila MUST conservar: jugador, fecha de vigencia, precio en centésimas de crédito, nombre de estrategia y versión, fecha de la valuación de origen, score utilizado, precio anterior, multiplicador aplicado, precio resultante, origen de la fila (`INITIAL`, `RECOMPUTED` o `CARRIED_FORWARD`) e instante de cálculo.
- **FR-024**: La fecha de vigencia MUST ser la fecha de la valuación utilizada. Cuando el jugador no tenga valuación para la estrategia elegida, la fila arrastrada MUST tomar como fecha de vigencia la fecha de cotización de la ejecución, es decir, la fecha UTC del día en que corre, y su fecha de valuación de origen MUST quedar ausente. La fila arrastrada MUST NOT alterar la fecha de vigencia de referencia del jugador, conforme a FR-027.
- **FR-025**: El instante de cálculo MUST conservarse por separado de la fecha de vigencia, porque pueden diferir.
- **FR-026**: MUST existir como máximo una cotización por jugador, fecha de vigencia y origen. Una segunda ejecución en la misma fecha de cotización MUST NOT duplicar filas, conforme a FR-060. Una fila `RECOMPUTED` y una fila `CARRIED_FORWARD` del mismo jugador MAY coexistir con la misma fecha de vigencia cuando la valuación que origina la fila `RECOMPUTED` tenga esa fecha; en ese caso la fila `RECOMPUTED` MUST ser la cotización vigente del jugador, por ser la que incorpora el score, y la fila arrastrada MUST conservarse como registro histórico del período sin cambio. El historial MUST mostrar ambas filas con esa fecha de vigencia, y la fila arrastrada MUST documentar el precio anterior al reprecio.
- **FR-027**: Una ejecución MUST escribir la fila de un jugador solo cuando la fecha de vigencia calculada sea estrictamente posterior a la **fecha de vigencia de referencia** del jugador. La fecha de vigencia de referencia MUST ser la fecha de vigencia de la última fila `INITIAL` o `RECOMPUTED` del jugador; las filas `CARRIED_FORWARD` MUST NOT modificar la fecha de vigencia de referencia, de modo que una fila arrastrada nunca pueda impedir un reprecio posterior. Para las filas arrastradas, la comparación MUST realizarse contra esa fecha de vigencia de referencia. Una fecha igual o anterior MUST NOT reescribir el historial ni repreciar al jugador: su precio vigente se conserva y la ejecución lo informa. Esta regla materializa el caso del jugador cuyas métricas no cambiaron: mientras no exista una valuación posterior a la fecha de vigencia de referencia, su score no se vuelve a aplicar y su precio no se mueve. Esta regla MUST aplicar tanto a las filas recalculadas como a las arrastradas.
- **FR-028**: El precio anterior de una fila MUST ser el precio de la cotización vigente del jugador inmediatamente antes de escribir esa fila, y MUST NOT ser el precio de una fila posterior.
- **FR-029**: Cada fila MUST permitir reconstruir el cálculo: precio anterior, multiplicador, score, estrategia y versión, fecha de la valuación de origen y precio resultante, con los parámetros de la ejecución que la produjo —factor de ajuste, tope de variación y piso mínimo— disponibles en el resumen persistido de esa ejecución (FR-061).
- **FR-030**: La persistencia MUST permitir la consulta del historial por jugador, de la cotización vigente, de la cotización a una fecha dada y del ranking por precio, sin recalcular precios.

#### Inicialización del mercado

- **FR-031**: MUST existir una operación que establezca la primera fecha del mercado y deje a todos los jugadores cotizables en 1 crédito, con la fila inicial identificada como tal y sin estrategia asociada.
- **FR-032**: La inicialización MUST ser idempotente: repetirla MUST NOT duplicar cotizaciones ni producir un conjunto distinto.
- **FR-033**: Si el mercado ya está inicializado —existe al menos una fila `INITIAL` (FR-044)— y la fecha de mercado solicitada es distinta de la fecha de mercado vigente, la inicialización MUST rechazarse de forma controlada con el error `MARKET_ALREADY_INITIALIZED` y MUST NOT alterar la historia existente, porque escribir una segunda fila inicial por jugador violaría FR-034. Si la fecha solicitada es anterior a la fecha de mercado vigente, el rechazo MUST ser el de la existencia de cotizaciones en fechas de vigencia posteriores. Repetir la inicialización con la misma fecha de mercado MUST ser idempotente, conforme a FR-032.
- **FR-034**: La inicialización MUST dejar exactamente una fila inicial por jugador cotizable y MUST NOT crear filas para jugadores sin métricas.
- **FR-035**: El mercado MUST requerir que la inicialización se haya ejecutado antes de la primera operación de mercado y antes de toda ejecución de cotización, de modo que todo jugador cotizable disponga de precio, conforme a FR-043, FR-044 y FR-063. La ausencia de esa inicialización MUST rechazarse de forma controlada.

#### Consultas

- **FR-036**: El sistema MUST permitir consultar el historial de cotizaciones de un jugador, con sus filas ordenadas de la más reciente a la más antigua.
- **FR-037**: El sistema MUST permitir consultar la cotización vigente de un jugador, entendida como la última cotización persistida; cuando dos filas compartan fecha de vigencia, MUST prevalecer la fila `RECOMPUTED` sobre la `CARRIED_FORWARD`, conforme a FR-026.
- **FR-038**: El sistema MUST permitir consultar la cotización de un jugador a una fecha dada: la última cotización cuya fecha de vigencia no supera la fecha consultada, con el mismo criterio de prevalencia de FR-037 a igualdad de fecha de vigencia.
- **FR-039**: El sistema MUST permitir consultar un ranking de jugadores por precio sobre las cotizaciones vigentes, ordenado de mayor a menor, con una sola entrada por jugador cotizable.
- **FR-040**: Una consulta de cotización de un jugador sin cotización MUST informar de la ausencia de cotización disponible.
- **FR-041**: Las consultas MUST exponer el precio, la fecha de vigencia, la estrategia y su versión, la fecha de la valuación de origen, el score, el precio anterior, el multiplicador, el origen y el instante de cálculo, de modo que el resultado sea auditable y MUST permitir conocer la antigüedad del score que sustenta el precio vigente.

#### Integración con el mercado

- **FR-042**: La compra y la publicación de venta MUST consumir la cotización vigente persistida, a través del puerto de lectura de cotización ya existente. La fuente de cotización simulada MUST ser retirada del sistema y MUST NOT seguir existiendo como alternativa.
- **FR-043**: La compra y la publicación de venta MUST rechazar de forma controlada toda operación cuando el mercado no esté inicializado. Ese rechazo MUST ser distinguible del error de cotización no disponible mediante un código de error propio, MUST ocurrir antes de cualquier movimiento de recursos y MUST NOT modificar saldos, tenencias de tokens ni transacciones.
- **FR-044**: El mercado MUST considerarse inicializado cuando exista al menos una cotización de origen `INITIAL`. La inicialización MUST ser la única operación que pueda crear esa fila inicial, de modo que el estado de inicialización del mercado sea verificable y no dependa de una configuración externa.
- **FR-045**: Un jugador sin cotización MUST ser rechazado con el error de cotización no disponible, que es el comportamiento ya definido para el mercado.
- **FR-046**: El instante de cálculo que acompaña a la cotización consumida MUST ser el instante de cálculo de esa cotización, no el instante de la lectura. Una cotización antigua MUST seguir siendo válida mientras sea la última vigente.
- **FR-047**: El precio de una publicación de venta MUST permanecer congelado en el momento de su publicación. La publicación MUST NOT repreciarse al comprar.
- **FR-048**: El recálculo de cotizaciones MUST NOT alterar saldos de créditos, tenencias de tokens ni transacciones. El precio promedio de compra histórico de las posiciones MUST NOT modificarse; la revaluación de las posiciones de los usuarios es un efecto de lectura.
- **FR-049**: La ejecución de la cotización MUST NOT requerir la revaluación de las posiciones de los usuarios ni escribir en las tablas de mercado, tenencias o transacciones.

#### Límites de alcance

- **FR-050**: Esta feature MUST NOT incluir el job programado, su agenda ni la periodicidad semanal; la orquestación periódica corresponde a otra feature.
- **FR-051**: Esta feature MUST NOT incluir la interfaz de usuario ni sus gráficos.
- **FR-052**: Esta feature MUST NOT incluir la política de obsolescencia de los precios arrastrados: un jugador que no puede valuarse mantiene su precio indefinidamente y la detección de ese caso corresponde a una feature posterior.
- **FR-053**: Esta feature MUST NOT incluir regresión a la media de los precios.
- **FR-054**: Esta feature MUST NOT incluir comisiones, spreads ni ningún mecanismo de creación de créditos.
- **FR-055**: Esta feature MUST NOT ampliar el catálogo de estrategias de valoración.
- **FR-056**: Esta feature MUST NOT re-preciar el mercado por cambio de estrategia.
- **FR-057**: Esta feature MUST NOT modificar el congelamiento de precio de las publicaciones de venta.
- **FR-058**: Esta feature MUST NOT reemplazar ni extender el contrato de mercado existente más allá de la sustitución de la fuente de cotización simulada por la fuente real.

#### Ejecución de la cotización

- **FR-059**: La ejecución MUST ser invocable de forma explícita por el operador y MUST requerir autorización, con el mismo criterio de autorización vigente para las operaciones administrativas del mercado: un JWT válido, sin引入cir roles nuevos. Una solicitud no autorizada MUST rechazarse con el error de autenticación vigente, sin escribir filas ni resumen de ejecución.
- **FR-060**: La fecha de cotización de una ejecución MUST ser la fecha UTC del día en que corre y MUST NOT ser fijada por el operador. Cada fecha de cotización MUST admitir como máximo una ejecución: una segunda ejecución en la misma fecha de cotización MUST rechazarse con un error controlado, MUST NOT escribir filas de cotización, MUST NOT alterar el historial, MUST NOT modificar el resumen de la ejecución ya registrada y MUST NOT modificar saldos, tenencias ni transacciones. Dos ejecuciones concurrentes de la misma fecha de cotización MUST resolverse de la misma forma: solo una se resuelve como ejecución y la otra se rechaza con el error controlado `EXECUTION_ALREADY_EXISTS`, sin filas adicionales ni cambio del resumen existente.
- **FR-061**: La ejecución MUST informar su resultado: fecha de cotización, estrategia y versión utilizadas, factor de ajuste, tope y piso aplicados, número de jugadores cotizados —la población cotizable evaluada en la ejecución, incluidos los que no se repreciaron—, número de filas recalculadas, número de filas arrastradas y número de jugadores no repreciados por no disponer de una valuación posterior a su fecha de vigencia de referencia —subconjunto de los jugadores cotizados— y número de jugadores que fallaron. Ese resumen MUST persistirse como registro de la ejecución y MUST ser consultable por fecha de cotización. Una ejecución rechazada MUST NOT persistir resumen.
- **FR-062**: Un fallo individual MUST registrarse internamente con el jugador, la etapa y la causa, y MUST NOT dejar una fila parcial ni una cotización incoherente para ese jugador. La atomicidad MUST ser por jugador: la ejecución MUST continuar con los demás jugadores y el fallo de uno MUST NOT interrumpirla ni invalidar las cotizaciones de los demás. El detalle interno MUST NOT exponerse en la respuesta.
- **FR-063**: La ejecución de cotización MUST rechazarse de forma controlada cuando el mercado no esté inicializado, con el mismo criterio y el mismo error propio que las operaciones de mercado de FR-043, y MUST NOT escribir ninguna fila. La inicialización del mercado MUST preceder a toda ejecución de cotización.
- **FR-066**: Las causas de rechazo de una ejecución de cotización MUST evaluarse con un orden de precedencia único y determinista, de modo que una solicitud con más de una causa de rechazo MUST producir siempre el mismo código de error. El orden MUST ser: (1) estrategia fuera del catálogo (`UNKNOWN_QUOTE_STRATEGY`, FR-018); (2) mercado no inicializado (`MARKET_NOT_INITIALIZED`, FR-043, FR-063); (3) factor de ajuste `k` fuera del rango [0,10; 1,00] (`INVALID_ADJUSTMENT_FACTOR`, FR-011); (4) resto de parámetros de la ejecución inválidos (`INVALID_VARIATION_CAP`, FR-012; `INVALID_MIN_PRICE_FLOOR`, FR-013); (5) existencia previa de una ejecución para la fecha de cotización (`EXECUTION_ALREADY_EXISTS`, FR-060). Las causas (1) a (4) MUST evaluarse antes de cualquier escritura y MUST NOT dejar resumen de ejecución registrado; la causa (5) MUST evaluarse después de las anteriores y MUST NOT alterar el resumen de la ejecución existente.

#### Observabilidad y rendimiento

- **FR-064**: La ejecución de cotización y las consultas de cotización MUST apoyarse en los mecanismos de observabilidad ya vigentes en el proyecto, incluidos los registros estructurados, la correlación de solicitudes y la exposición de métricas y health checks, y MUST NOT reimplementarlos ni sustituirlos. La ejecución MUST quedar registrada con su fecha de cotización, su estrategia y su versión y sus contadores, y los fallos individuales MUST quedar registrados con el jugador, la etapa y la causa.
- **FR-065**: Esta feature MUST NOT fijar objetivos de rendimiento propios ni introducir mecanismos nuevos de caché u observabilidad. Las consultas de cotización MUST resolverse por acceso directo al repositorio de cotizaciones, sin recalcular precios.

### Key Entities *(include if feature involves data)*

- **Cotización**: Punto de la serie de precios de un jugador. Jugador, fecha de vigencia, precio en centésimas, estrategia y versión, fecha de la valuación de origen, score, precio anterior, multiplicador, precio resultante, origen (`INITIAL`, `RECOMPUTED` o `CARRIED_FORWARD`) e instante de cálculo.
- **Jugador cotizable**: Jugador del catálogo que posee métricas persistidas; es la única población con cotización y la única operable en el mercado.
- **Ejecución de cotización**: Corrida persistida de la aplicación de una estrategia, un factor de ajuste, un tope de variación y un piso mínimo sobre una fecha de cotización, con su resumen de resultado (estrategia y versión, parámetros aplicados y contadores) y su consulta por fecha de cotización.
- **Ancla de mercado**: Precio inicial de 1 crédito por token, materializado en la fila inicial de cada jugador cotizable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cotizaciones persistidas tiene un precio estrictamente mayor que cero.
- **SC-002**: El 100% de las filas `RECOMPUTED` conserva el nombre de la estrategia y su versión; el 100% de las filas `CARRIED_FORWARD` conserva la estrategia de la cotización cuyo precio repite y MUST carecer de estrategia solo cuando ese precio provenga de la fila inicial, que por definición no tiene estrategia asociada; el 100% de las filas `INITIAL` se identifica como tal y no tiene estrategia asociada.
- **SC-003**: El 100% de las cotizaciones persistidas es reproducible a partir de su precio anterior, su score, su estrategia y los parámetros de la ejecución que la produjo, disponibles en el resumen persistido de esa ejecución.
- **SC-004**: El 100% de las segundas ejecuciones de cotización de una misma fecha de cotización se rechaza de forma controlada, sin escribir filas de cotización, sin alterar el historial ni el resumen de la ejecución ya registrada.
- **SC-005**: Un jugador nunca evaluado conserva su precio, mantiene una fila consultable y sigue siendo consultable como cotización vigente.
- **SC-006**: Tras la inicialización, el 100% de los jugadores cotizables dispone de precio y ninguno de los jugadores sin métricas aparece en el rango de cotizaciones.
- **SC-007**: El ranking refleja el precio vigente de cada jugador cotizable, con una sola entrada por jugador y el mismo precio que devuelve su consulta de cotización vigente.
- **SC-008**: Ninguna ejecución de recálculo altera saldos de créditos, tenencias de tokens ni transacciones; el 100% de las operaciones de mercado confirmadas conserva su precio y su instante de cálculo de origen.
- **SC-009**: La compra y la publicación de venta consumen el precio persistido de la última cotización del jugador, y el instante de cálculo devuelto coincide con el de esa cotización y no con el de la lectura.
- **SC-010**: Ninguna operación de mercado puede ejecutarse sin que el mercado esté inicializado; el 100% de los intentos previos a la inicialización se rechazan de forma controlada.
- **SC-011**: Una revisión de alcance confirma que no queda ninguna fuente de cotización simulada en el sistema y que el puerto de lectura de cotización vigente resuelve contra el repositorio canónico real.
- **SC-012**: El operador puede ejecutar un recálculo completo y conocer su resultado en una sola interacción, sin seguimiento asíncrono ni llamadas a proveedores externos.
- **SC-013**: El 100% de las ejecuciones de cotización ejecutadas queda registrado con su fecha de cotización, la estrategia y su versión, el factor de ajuste, el tope y el piso aplicados y sus contadores, y es consultable por fecha de cotización.
- **SC-014**: El 100% de las ejecuciones de cotización y de las consultas de cotización quedan cubiertas por los mecanismos de observabilidad vigentes en el proyecto, y el 100% de los fallos individuales quedan registrados con el jugador, la etapa y la causa sin exponerse en la respuesta.
- **SC-015**: El 100% de los intentos de recálculo sobre un mercado no inicializado se rechazan de forma controlada, sin escribir filas de cotización.
- **SC-016**: El 100% de las solicitudes de ejecución con más de una causa de rechazo devuelve el código de error de mayor precedencia conforme al orden de FR-066.

## Out of Scope

- Job programado, configuración de agenda y periodicidad semanal.
- Interfaz de usuario y gráficos.
- Detección y política de obsolescencia de los precios arrastrados.
- Regresión a la media de los precios.
- Comisiones, spreads y cualquier mecanismo de creación de créditos.
- Ampliación del catálogo de estrategias de valoración.
- Cualquier re-precio del mercado por cambio de estrategia.
- Modificación del congelamiento de precio de las publicaciones de venta.
- Ampliación del contrato de mercado existente más allá de sustituir la fuente de cotización simulada por la real.

## Assumptions

- SPEC-013 persiste `POSITIONAL_PERFORMANCE` v1 y `GENERAL_ATTRIBUTES` v1 siempre juntas para un jugador y una fecha, de modo que para cualquier estrategia elegida del catálogo existe un score vigente de todo jugador evaluado. Un jugador evaluado en una estrategia puede no estar evaluado en la otra si la evaluación de esa fecha falló para él; en ese caso su fila para la estrategia elegida es arrastrada.
- SPEC-010 define el puerto de lectura de cotización y el comportamiento de error de cotización no disponible, que esta feature reutiliza sin extenderlo.
- El precio de un token se expresa en centésimas de crédito: 100 centésimas equivalen a 1 crédito, coherente con el contrato de mercado vigente.
- La población cotizable es la de los jugadores con métricas persistidas por SPEC-012 y con al menos una métrica informada en su último registro; el catálogo completo incluye jugadores sin métricas —o con un registro de métricas sin ninguna métrica informada— que no entran al mercado.
- Los valores por defecto del control de variabilidad son 0,10 para el factor de ajuste, 0,10 (10 %) para el tope de variación por período y 1 centésima para el piso mínimo; son configurables y ajustables en cada ejecución, y el factor de ajuste solo admite valores dentro del rango cerrado [0,10; 1,00].
- Una fila `CARRIED_FORWARD` hereda la estrategia y la versión de la cotización vigente cuyo precio repite, porque el precio arrastrado no fue producido por la estrategia de la ejecución; la estrategia de cada corrida queda registrada en el resumen de esa ejecución.
- La compra, la publicación de venta, el congelamiento del precio publicado, la auditoría del precio aplicado y los errores de negocio del mercado ya están implementados por SPEC-010 y SPEC-011 y esta feature no modifica su comportamiento. Su único delta es la fuente de cotización, la retirada del simulador y el rechazo explícito de operar sin mercado inicializado.
- El contrato de lectura de cotización de `specs/010-buy-tokens/contracts/quote-source.md` figura con el consumidor "únicamente `BuyOrderServiceImpl`", pero desde SPEC-011 también lo consume `TokenListingServiceImpl`; esa descripción desactualizada MUST corregirse en el mismo cambio que introduce la fuente real, conforme al principio de coherencia de contratos.
- La fecha de vigencia de una fila arrastrada es la fecha de la cotización, porque el jugador no tiene valuación de la que derivarla; el resto de filas usa la fecha de la valuación utilizada.
- La fecha de valuación de SPEC-013 es la fecha UTC de la evaluación, de modo que obtener una valuación nueva equivale a que el jugador haya sido reevaluado con métricas nuevas. Por eso el avance de la fecha de vigencia coincide con la disponibilidad de una valuación posterior y un jugador sin valuación nueva no se reprecia.
- La fecha de vigencia de referencia de un jugador es la de su última fila `INITIAL` o `RECOMPUTED`; las filas `CARRIED_FORWARD` quedan fuera de ella, de modo que una valuación producida el mismo día que una fila arrastrada sigue siendo aplicable en la ejecución siguiente (FR-026, FR-027).
- Una ejecución escribe la fila de un jugador solo cuando la fecha de vigencia calculada es posterior a su fecha de vigencia de referencia; por eso un jugador con valuación antigua respecto de esa referencia conserva su precio y no genera una fila, y por eso el cambio de estrategia nunca produce saltos.
- El mercado se inicializa antes de la primera valuación que se use para cotizar, de modo que las fechas de vigencia de las valuaciones son posteriores a la primera fecha del mercado.
- Las consultas de historial, cotización vigente, cotización por fecha y ranking se exponen conforme a las APIs obligatorias de `tp/enunciado.md` §4; el contrato HTTP efectivo se define en la fase de planificación.
- La ejecución de la cotización es una operación de administración del mercado y reutiliza las reglas de autenticación y autorización vigentes para las operaciones administrativas, sin introducir roles nuevos.
- Una fecha de cotización admite como máximo una ejecución de cotización; una segunda ejecución el mismo día se rechaza, de modo que las valuaciones nuevas que lleguen durante ese día se reflejarán en la ejecución del día siguiente, cuando la fecha de vigencia calculada sea posterior a la vigente.
- La fecha de cotización de una ejecución es la fecha del día en que corre y no es un parámetro del operador; una ejecución en un día distinto es una corrida distinta con su propia fecha de cotización, y una segunda ejecución en el mismo día se rechaza conforme a FR-060.

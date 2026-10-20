# Feature Specification: Definir reglas contables de costo promedio y resultados financieros (REQ-S2-20)

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/015-define-average-cost-and-financial-results-rules` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `015-define-average-cost-and-financial-results-rules`, sin el prefijo de tipo.

**Created**: 2026-10-20

**Status**: Ready for planning

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Estado operacional (2026-10-20)**: Specify y Clarify completadas. La fase Specify incorporó las decisiones D-1 a D-5 y G-1 a G-3, y la fase Clarify resolvió tres decisiones funcionales nuevas (CL-14, CL-15 y CL-16) y precise las reglas de costo autoritativo, precio medio derivado, unificación del reloj y tratamiento de la emisión inicial. No queda ninguna decisión funcional pendiente ni ningún marcador de aclaración. El alcance, los requisitos y los criterios de aceptación están suficientemente claros para diseñar la solución. Esta especificación **no habilita implementación alguna**: la IMPLEMENT AUTHORIZATION sigue siendo un gate humano explícito y separate.

**Identidad asignada explícitamente**: `SPEC-015`, registrado en [`specs/assignments.md`](../assignments.md) antes de materializar la carpeta. La asignación no acredita fases, gates, aprobación, implementación ni integración.

**Input**: Definir la fuente de verdad contable del mercado de tokens: el costo de adquisición de las posiciones de un usuario, el resultado realizado y el resultado no realizado, con reglas de precisión, redondeo, orden determinista de las operaciones y tratamiento de la emisión inicial de tokens. Esta funcionalidad **no ejecuta compras ni ventas**, no modifica las reglas de ejecución, transferencia ni auditoría de SPEC-010 y SPEC-011, no implementa la consulta de portfolio ni su interfaz, y no implementa el motor de cotizaciones. Entrega las reglas y un componente de cálculo reutilizable que las aplica, para que SPEC-014 (consulta de portfolio) las consuma sin redefinirlas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Conocer el costo de adquisición y el resultado realizado de mi posición (Priority: P1)

Como usuario del mercado, quiero conocer el costo de adquisición de los tokens que poseo de un jugador y el resultado financiero que ya realicé al vender parte de ellos, para saber cuánto invertí, cuánto recuperé y qué me queda en cada posición.

**Why this priority**: Es la fuente de verdad contable que el enunciado académico exige («Precio promedio de compra» y «Ganancia o pérdida», `tp/enunciado.md` §3.4) y la que SPEC-014 debe consumir sin reescribirla. Sin costo de adquisición ni resultado realizado no existe resultado financiero consultable, y el portfolio de la entrega 2 no puede completarse.

**Independent Test**: Con un usuario autenticado que haya realizado compras confirmadas y al menos una venta confirmada de un jugador, calcular su posición y comprobar el precio medio de adquisición, el costo de adquisición de la posición y el resultado realizado acumulado. Puede demostrarse sin consultar cotizaciones, sin consultar portfolio y sin operar en el mercado.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC01 | Usuario que compró 10 tokens de un jugador a 100 centésimas por token | Se calcula su posición | El costo pagado es 1000 centésimas, la cantidad abierta es 10 tokens, el precio medio de adquisición es 100 centésimas por token y el costo de adquisición de la posición es 1000 centésimas. No hay resultado realizado. |
| AC02 | El mismo usuario compra después 10 tokens del mismo jugador a 150 centésimas por token | Se calcula su posición | El costo pagado acumulado es 2500 centésimas, la cantidad abierta es 20 tokens y el precio medio de adquisición es 125 centésimas por token. El resultado realizado sigue en cero. |
| AC03 | El usuario anterior vende 5 tokens de ese jugador a 200 centésimas por token | Se calcula su posición | El resultado realizado es 375 centésimas (1000 de importe de venta menos 625 de costo atribuido), la cantidad abierta es 15 tokens, el costo de adquisición de la posición es 1875 centésimas y el precio medio derivado es 125 centésimas por token. La venta reduce la cantidad y el costo de forma proporcional y no altera el precio medio. |
| AC04 | Usuario que vendió la totalidad de sus tokens de un jugador | Se calcula su posición | El costo de adquisición de la posición es cero, la posición está cerrada y el resultado realizado acumulado se conserva con su importe. |
| AC05 | Usuario con una posición cerrada de un jugador que vuelve a comprar tokens de ese jugador | Se calcula su posición | La compra inicia un costo de adquisición nuevo, igual al importe pagado por ella, la posición vuelve a estar abierta con su costo de adquisición propio y el resultado realizado acumulado del ciclo anterior se conserva sin reiniciarse. |
| AC06 | Usuario que publica tokens en venta y esa publicación no se ejecuta, se cancela o vence | Se calcula su posición | El costo de adquisición, la cantidad abierta y el resultado realizado no cambian. Una publicación no ejecutada no es una operación. |
| AC07 | Usuario que compró tokens al superusuario y otro usuario que compró una publicación | Se calculan sus posiciones | En ambos casos la compra es una adquisición: el comprador suma el importe pagado a su costo de adquisición y su cantidad abierta, y no genera resultado realizado por sí sola. |
| AC08 | Vendedor que confirma una venta de tokens y comprador que la ejecuta | Se calculan ambas posiciones | El vendedor obtiene un resultado realizado igual al importe de la venta menos el costo atribuido a los tokens vendidos; el comprador incorpora esos tokens a su costo de adquisición. Cada operación se imputa una sola vez y en el rol que corresponde a cada parte. |
| AC09 | Superusuario que recibió la emisión inicial de 100 tokens de un jugador | Se calcula su posición | La posición está abierta con 100 tokens, costo de adquisición cero y resultado realizado cero. La emisión inicial no es una compra ni una operación de mercado. |
| AC10 | Posición que combine tokens de emisión inicial y tokens adquiridos comercialmente | Se calcula su posición | Existe un único costo de adquisición y un único precio medio de adquisición por usuario y jugador, y un único resultado: los tokens de emisión inicial aportan costo cero al promedio y las adquisiciones comerciales posteriores lo diluyen. |
| AC21 | Usuario que compró 2 tokens a 100 centésimas y 1 token a 101 centésimas del mismo jugador | Se calcula su posición | El costo pagado es 301 centésimas, la cantidad abierta es 3 tokens y el precio medio derivado es 100 centésimas por token. Vende después 1 token a 150 centésimas | El costo atribuido es 100 centésimas, el resultado realizado es 50 centésimas (150 menos 100), el costo de adquisición restante es 201 centésimas y la cantidad abierta es 2 tokens. La conservación es exacta: 301 = 100 + 201. El precio medio exacto pasa de 100,33 a 100,5 centésimas por token, un movimiento producido únicamente por el residuo de redondeo retenido. |
| AC22 | El usuario de AC21 vende los 2 tokens restantes a 150 centésimas cada uno | Se calcula su posición | El costo atribuido es 201 centésimas, el costo de adquisición llega exactamente a cero y el resultado realizado se incrementa en 99 centésimas (300 menos 201), con un resultado realizado acumulado de 149 centésimas. La conservación es exacta: 301 = 100 + 201 + 0. |

AC10 define una regla vigente aunque el modelo de mercado actual no permita que tokens emitidos y adquiridos comercialmente coexistan en una misma posición: esa coexistencia no es alcanzable hoy y no se modifica el mercado para volverla alcanzable (FR-022). AC21 y AC22 se incorporan en la sesión de Clarify del 2026-10-20; la numeración de los criterios anteriores se conserva.

---

### User Story 2 - Conocer el valor actual y el resultado no realizado de mi posición abierta (Priority: P2)

Como usuario del mercado, quiero conocer cuánto vale hoy mi posición abierta y qué resultado financiero todavía no he realizado, para valorar mi inversión con la cotización vigente y distinguir lo que ya es ganancia o pérdida de lo que todavía depende del mercado.

**Why this priority**: Completa el resultado financiero de la posición abierta. Sin él, el usuario solo conoce lo que ya realizó y no puede valorar su cartera, que es el segundo bloque del portfolio exigido por el enunciado y por SPEC-014.

**Independent Test**: Con un usuario que tiene una posición abierta y un jugador con cotización vigente, calcular el valor actual y el resultado no realizado; y con un jugador sin cotización vigente, comprobar que el resultado no realizado no se inventa.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC11 | Posición abierta de 15 tokens con costo de adquisición de 1875 centésimas y cotización vigente de 180 centésimas por token | Se calcula su posición | El valor actual es 2700 centésimas y el resultado no realizado es 825 centésimas (2700 menos 1875). El resultado total de la posición es la suma del resultado realizado y el no realizado. |
| AC12 | Posición abierta de un jugador sin cotización vigente disponible | Se calcula su posición | El valor actual, el resultado no realizado y el resultado total no están definidos: no se informan como cero ni se sustituyen por un precio histórico. El resultado realizado y las cantidades se conservan. |
| AC13 | Fuente de cotizaciones del backend que falla por causa técnica | Se calcula su posición | El cálculo no puede completarse y el fallo se propaga como tal: no se devuelve ningún dato financiero de la posición, tampoco el resultado realizado. No se devuelve un valor estimado, un precio de respaldo ni un resultado no realizado ficticio, y el fallo se distingue del caso de ausencia de cotización. |
| AC14 | Posición cerrada, sin cantidad abierta | Se calcula su posición | No hay valor actual ni resultado no realizado que informar. El resultado realizado acumulado se conserva, el resultado total es igual a ese resultado realizado acumulado y la posición sigue cerrada. |
| AC15 | Posición abierta cuyo resultado no realizado es negativo | Se calcula su posición | El importe se informa con signo negativo, en enteros de centésimas, sin punto flotante ni pérdida de precisión. |

---

### User Story 3 - Verificar que el resultado que me informs es trazable y reproducible (Priority: P3)

Como usuario del mercado, quiero que el resultado financiero que se me informa se pueda reconstruir a partir de mis operaciones confirmadas y sea siempre el mismo para los mismos datos, para poder confiar en él y detectar cualquier discrepancia.

**Why this priority**: La regla contable es la fuente de verdad del sistema y debe ser auditable y determinista. Es la garantía que sostiene la confianza en el resultado y la que permite a SPEC-014 ofrecer cifras coherentes entre consultas sucesivas.

**Independent Test**: Con un conjunto de operaciones confirmadas de un usuario, calcular su posición dos veces sobre el mismo estado de datos y comprobar que el resultado es idéntico; y comprobar que cada importe resultado se corresponde con operaciones confirmadas identificables.

**Acceptance Scenarios**:

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC16 | Usuario con compras y ventas confirmadas de varios jugadores | Se calculan sus posiciones | Cada resultado se reconstruye a partir de las operaciones confirmadas en las que el usuario es comprador o vendedor. Las publicaciones no ejecutadas, las canceladas, las vencidas y las operaciones fallidas no aportan ni modifican ningún resultado. |
| AC17 | Dos operaciones confirmadas del mismo usuario con el mismo instante de confirmación | Se calculan sus posiciones | Ambas se imputan en un orden estable y único, definido por el instante de confirmación y, ante instantes iguales, por el identificador de la operación. El resultado no depende del orden en que el sistema lea los registros. |
| AC18 | Conjunto de operaciones confirmadas sin cambios entre dos cálculos | Se calcula la posición dos veces | Ambos resultados son idénticos, incluidos el costo de adquisición, el resultado realizado y el resultado no realizado. El resultado no depende del reloj de la consulta ni del orden de recorrido de los datos. |
| AC19 | Operaciones confirmadas por las dos vías de compra del mercado | Se calculan las posiciones | El instante de confirmación de toda operación del mercado se obtiene de una única fuente de reloj, de modo que el orden contable es comparable entre compras directas al superusuario y liquidaciones de publicaciones. |
| AC20 | Posición con varias ventas parciales | Se calcula su posición | El costo pagado se conserva exactamente: el costo atribuido a las ventas más el costo de adquisición de la posición es igual al costo pagado, sin diferencias de una centésima. |

### Edge Cases

Clasificación conforme a Constitution §2.2; las mejoras recomendadas no condicionan la aceptación.

- **OBLIGATORIO — residuo de redondeo:** cuando el costo de adquisición de la posición no se reparte en partes enteras, la parte fraccionaria no puede repartirse. El costo atribuido a una venta se redondea a centésimas enteros y la diferencia se conserva en el costo de adquisición de la posición, de modo que la conservación del costo pagado es exacta. El costo de adquisición es la cifra autoritativa y el producto del precio medio redondeado por la cantidad abierta puede diferir de él en ese residuo retenido. El precio medio derivado puede por ello variar tras una venta, como máximo en el residuo retenido, inferior a una centésima por token; en aritmética exacta la venta no lo altera. Al liquidarse por completo la posición, el costo llega exactamente a cero y el residuo pendiente se incorpora al resultado realizado (AC03, AC20, AC21, AC22).
- **OBLIGATORIO — liquidación total:** al vender la totalidad de una posición, el costo de adquisición de la posición llega a cero de forma exacta y la posición queda cerrada con su resultado realizado acumulado (AC04).
- **OBLIGATORIO — venta por encima del costo:** una venta cuyo importe sea inferior al costo atribuido produce un resultado realizado negativo. Los resultados financieros pueden ser negativos y se informan con su signo (AC15).
- **OBLIGATORIO — posición sin operaciones:** un usuario sin compras confirmadas no tiene posición abierta, su costo de adquisición es cero y su resultado realizado es cero. La consulta de un estado vacío no falla (una posición abierta sin operaciones solo puede provenir de la emisión inicial, caso cubierto en AC09).
- **OBLIGATORIO — ausencia de cotización:** la ausencia de cotización vigente deja el resultado no realizado sin definir y no equivale a cero; el resultado realizado y las cantidades no se ven afectados (AC12).
- **OBLIGATORIO — fallo técnico de la fuente de cotizaciones:** se distingue de la ausencia de cotización, no se sustituye por un valor estimado y no devuelve ningún dato financiero de la posición, tampoco el resultado realizado (AC13).
- **OBLIGATORIO — emisión inicial de tokens:** la emisión no es operación de mercado, no genera resultado realizado, no altera el costo de adquisición por compra y no se cuenta como operación. Los tokens emitidos participan en la valoración y, si coexistieran con tokens adquiridos comercialmente, el precio medio único los diluye (AC09, AC10).
- **OBLIGATORIO — operaciones simultáneas:** el cálculo opera sobre un conjunto de operaciones confirmadas y no puede devolver una combinación de cantidades y resultados que no pertenezca a ningún estado válido. No se garantiza una instantánea compartida entre cálculos concurrentes distintos.
- **OBLIGATORIO — integridad de los datos históricos:** si el registro de operaciones no permite reconstruir la posición de forma consistente, el cálculo falla con un error de integridad y no con resultados financieros incorrectos.
- **OBLIGATORIO — jugador fuera del catálogo:** si un jugador deja de estar en el catálogo, el cálculo histórico de sus posiciones se conserva y sus resultados no se alteran.
- **OBLIGATORIO — venta del superusuario como contraparte del mercado:** el superusuario que entrega tokens al comprador en una compra directa es vendedor de la operación y le aplica la misma regla que a cualquier vendedor. Como sus tokens tienen costo de adquisición cero, el resultado realizado de esa venta es igual al importe de la venta. La regla es uniforme: no distingue al superusuario ni introduce un concepto de actividad de contraparte (AC08, AC09).
- **OBLIGATORIO — privacidad:** el cálculo se realiza por usuario y jugador y no puede revelar información financiera de terceros.
- **OBLIGATORIO — el cálculo no modifica datos:** el cálculo es de solo lectura: no crea, modifica ni elimina operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos.
- **RECOMENDADO — detalle de la posición:** informar además el número de compras y el número de ventas de la posición es una mejora de detalle no bloqueante.
- **RECOMENDADO — persistencia de un libro de costo:** mantener una estructura de costo apurada es una mejora de rendimiento no bloqueante. Si existe, MUST ser derivada y reconciliable con las operaciones confirmadas, nunca una segunda fuente de verdad.
- **RECOMENDADO — granularidad del desglose:** exponer el desglose por operación del resultado realizado es una mejora de detalle no bloqueante.
- **OPCIONAL — reexpresión retroactiva:** recalcular o corregir retroactivamente resultados históricos cuando cambie una regla contable es un escenario excepcional, de baja relevancia práctica en el estado actual del proyecto.

## Requirements *(mandatory)*

### Functional Requirements

Los requisitos siguientes son obligatorios para las historias de esta feature.

- **FR-001**: El sistema MUST calcular, para un usuario y un jugador, el costo de adquisición de su posición abierta, su resultado realizado y su resultado no realizado, a partir de las operaciones confirmadas del mercado.
- **FR-002**: La fuente de verdad del costo y de los resultados MUST ser el registro de operaciones confirmadas del mercado. El cálculo MUST NOT derivar costo ni resultados de ningún otro dato. La única información ajena a las operaciones que el cálculo admite es la emisión inicial de tokens, y solo para aportar la cantidad de tokens de costo cero que una posición puede tener sin operaciones (FR-021).
- **FR-003**: Cada operación confirmada MUST imputarse una sola vez por cada parte y según su rol: como adquisición para el comprador y como venta para el vendedor. El cálculo MUST NOT contabilizar dos veces una misma operación ni imputar a un usuario operaciones en las que no participó.
- **FR-004**: El costo de adquisición de una posición MUST construirse ponderando las compras confirmadas por su cantidad: cada compra MUST incorporarse al costo de la posición por su importe exacto, de modo que la posición refleje el promedio ponderado móvil de sus adquisiciones. Las ventas MUST reducir el costo de la posición de forma proporcional a la cantidad vendida, conforme a la regla de atribución de FR-007.
- **FR-005**: El costo pagado de una posición MUST ser la suma exacta de los importes de las compras confirmadas de esa posición. La cantidad abierta MUST ser la cantidad comprada menos la cantidad vendida.
- **FR-006**: El costo de adquisición de la posición MUST ser la cifra autoritativa del costo: un entero en centésimas de crédito, resultado de la evolución descrita en FR-004 y FR-007. El precio medio de adquisición MUST derivarse de ese costo y de la cantidad abierta (FR-031), MUST NOT sustituirlo y MUST NOT usarse para recalcularlo.
- **FR-007**: El costo atribuido a una venta MUST calcularse sobre el costo de adquisición y la cantidad abierta previos a la venta, MUST redondearse a un entero en centésimas y MUST NOT exceder el costo pendiente de asignar. La atribución MUST preservar la conservación del costo pagado exigida por FR-017.
- **FR-008**: El resultado realizado MUST incrementarse, por cada venta confirmada, con la diferencia entre el importe de esa venta y el costo atribuido a los tokens vendidos. El importe de la venta MUST ser el importe registrado en la operación confirmada.
- **FR-009**: El resultado realizado MUST ser acumulativo por usuario y jugador e incluir todas sus ventas confirmadas, incluso de ciclos de tenencia anteriores. MUST NOT reiniciarse al cerrar o reabrir una posición.
- **FR-010**: Al liquidarse por completo una posición, el costo de adquisición de la posición MUST llegar exactamente a cero y la posición MUST quedar cerrada. Una compra posterior MUST iniciar un costo de adquisición nuevo, y MUST NOT alterar el resultado realizado acumulado.
- **FR-011**: Una publicación de venta no ejecutada, cancelada o vencida MUST NOT modificar la cantidad abierta, el costo de adquisición, el precio medio de adquisición ni el resultado realizado. Los tokens reservados por una publicación activa siguen siendo poseídos y valorados: la reserva MUST NOT alterar el costo ni el resultado. Las operaciones fallidas MUST NOT generar resultado ni registro contable.
- **FR-012**: El resultado no realizado MUST ser la diferencia entre el valor actual de la posición abierta y su costo de adquisición. El valor actual MUST obtenerse de la cotización vigente que expone el backend, por el precio de un token multiplicado por la cantidad abierta.
- **FR-013**: Si no hay cotización vigente disponible, el resultado no realizado MUST quedar sin definir: MUST NOT informarse como cero ni sustituirse por un precio histórico o estimado. El resultado realizado y las cantidades MUST conservarse.
- **FR-014**: Si la fuente de cotizaciones falla por causa técnica, el cálculo MUST fallar y propagar el fallo. MUST NOT devolver ningún dato financiero de la posición, tampoco el resultado realizado, que no depende de la cotización; MUST NOT devolver un resultado parcial, estimado o inventado. El fallo técnico MUST distinguirse de la ausencia de cotización de FR-013.
- **FR-015**: Los importes MUST representarse como enteros en centésimas de crédito, sin punto flotante, y los resultados MUST poder ser negativos con su signo preservado.
- **FR-016**: El cálculo interno MUST conservar la precisión fraccionaria del costo de adquisición y de los importes derivados. Todo redondeo MUST realizarse con un único modo explícito y uniforme —redondeo al medio hacia arriba (HALF_UP) en centésimas de crédito— aplicado a todos los importes derivados.
- **FR-017**: La conservación del costo MUST ser exacta: para toda posición, el costo pagado MUST ser igual a la suma de los costos atribuidos a sus ventas más el costo de adquisición de la posición. El residuo producido por el redondeo MUST conservarse en el costo de adquisición de la posición mientras exista posición abierta, e incorporarse al resultado realizado cuando la posición se liquide por completo.
- **FR-018**: El orden contable de las operaciones MUST ser determinista y total: se define por el instante de confirmación de la operación, y ante instantes iguales por el identificador de la operación. El resultado MUST NOT depender del orden en que el sistema recorra los registros ni del reloj de la consulta.
- **FR-019**: El instante de confirmación de toda operación del mercado MUST obtenerse de una única fuente de reloj. El estado actual del sistema usa dos fuentes distintas según la vía de compra, lo que impide comparar operaciones de procedencias distintas: MUST unificarse para que el orden contable sea comparable en todo el mercado. Esta unificación pertenece al alcance de esta feature, aunque exija modificar código desarrollado en SPEC-010; MUST NOT reabrir SPEC-010 ni alterar su comportamiento observable ni su contrato público.
- **FR-020**: Dos cálculos sobre el mismo estado de datos MUST producir exactamente el mismo resultado, incluidos el costo de adquisición, el resultado realizado y el resultado no realizado.
- **FR-021**: La emisión inicial de tokens MUST NOT tratarse como operación de mercado: no tiene comprador, ni vendedor, ni precio, ni instante de operación. Los tokens emitidos MUST tener costo de adquisición cero y MUST NOT generar resultado realizado ni alterar el costo de adquisición de la posición como si fueran compras. La cantidad emitida MUST computar como tokens poseídos de costo cero en la posición de su propietario.
- **FR-022**: Los tokens de emisión inicial MUST participar en la valoración de la posición. Si una posición combinara tokens emitidos y tokens adquiridos comercialmente, el cálculo MUST informar un único costo de adquisición y un único precio medio de adquisición, en los que los tokens emitidos aportan costo cero y las adquisiciones comerciales posteriores diluyen ese promedio. La regla se define aunque el modelo de mercado vigente no permita hoy esa coexistencia, y MUST NOT motivar modificaciones del mercado para volverla alcanzable.
- **FR-023**: El cálculo MUST ser de solo lectura: MUST NOT crear, modificar ni eliminar operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos, y MUST NOT alterar el resultado de ningún otro usuario.
- **FR-024**: El cálculo MUST habilitarse por usuario y por jugador y MUST NOT revelar información financiera de terceros ni existencia de operaciones ajenas.
- **FR-025**: Si el registro de operaciones no permite reconstruir una posición de forma consistente, el cálculo MUST fallar con un error de integridad y MUST NOT devolver resultados financieros incorrectos ni incompletos.
- **FR-026**: Si un jugador deja de estar en el catálogo, el cálculo histórico de sus posiciones MUST conservarse y sus resultados MUST NOT alterarse.
- **FR-027**: El cálculo MUST poder resolverse sobre el conjunto completo de operaciones de un usuario sin un acceso a los datos individuales de cada posición, para que un agregado de cartera sea obtenible sin consultas por posición.
- **FR-028**: Las reglas contables MUST ser reutilizables por otras funcionalidades del sistema, en particular la consulta de portfolio, sin que estas definan fórmulas contables propias ni implementen reglas paralelas.
- **FR-029**: El modelo contable MUST NOT contemplar comisiones, impuestos, spreads, tasas ni conversión de moneda. El importe de una operación es exactamente el importe registrado en ella, y el resultado se obtiene exclusivamente de ese importe y del costo de adquisición.
- **FR-030**: El cálculo MUST NOT modificar las reglas de ejecución, transferencia ni auditoría de SPEC-010 y SPEC-011, ni su especificación ni el contrato público de sus endpoints. La unificación de la fuente del reloj exigida por FR-019 MUST preservar el comportamiento observable de esas operaciones, incluido el significado del instante de confirmación y de los importes registrados, y MUST NOT requerir modificar la especificación de SPEC-010.
- **FR-031**: El cálculo MUST informar el precio medio de adquisición de la posición, derivado del costo de adquisición y de la cantidad abierta de tokens y redondeado conforme a FR-016, como cifra informativa. Responde a la exigencia de informar el precio promedio de compra del portfolio (`tp/enunciado.md` §3.4). MUST NOT sustituir al costo de adquisición de la posición, que es la cifra autoritativa, y ningún consumidor MUST recalcular el costo a partir del precio medio redondeado.
- **FR-032**: El resultado total de una posición MUST ser la suma de su resultado realizado y su resultado no realizado cuando ambos estén definidos. Si el resultado no realizado no está definido por ausencia de cotización, el resultado total MUST quedar sin definido: MUST NOT informarse como cero ni como el resultado realizado. En una posición cerrada, sin cantidad abierta, el resultado total MUST ser igual al resultado realizado acumulado, porque no existe componente no realizado que sumar.

### Key Entities

- **Posición financiera**: Agregado por usuario y jugador que resume la exposición del usuario sobre un jugador: cantidad abierta, costo pagado, costo de adquisición de la posición —cifra autoritativa—, precio medio de adquisición —cifra derivada—, resultado realizado, resultado no realizado y resultado total. Es lo que se calcula; no es una tenencia, ni una operación, ni una publicación.
- **Operación de mercado confirmada**: Registro financiero inmutable de una compra o de una venta ejecutada, con comprador, vendedor, jugador, cantidad, precio unitario, importe, instante de confirmación e identificador. Es la fuente de verdad de la regla contable.
- **Emisión inicial de tokens**: Hecho propio e inmutable por jugador, con su propietario y su instante de emisión. No es una operación: no tiene comprador, ni vendedor, ni precio, ni instante de operación, y no origina compras.
- **Publicación de venta**: Oferta de tokens con precio congelado y estado efectivo. Publicar no es vender: solo su liquidación produce una venta confirmada y, con ella, resultado realizado.
- **Cotización vigente**: Precio actual de un token de un jugador con su instante de cálculo. Es la única fuente del valor actual de la posición.
- **Costo atribuido**: Parte del costo pagado que una venta consume, en enteros de centésimas. Es la contraparte del resultado realizado de esa venta.
- **Residuo de redondeo**: Diferencia, de magnitud inferior a una centésima, entre el costo atribuible ideal y su valor entero redondeado. Se conserva en el costo de adquisición de la posición mientras exista y puede desplazar el precio medio derivado en menos de una centésima por token.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Toda posición con al menos una compra confirmada informa un precio medio de adquisición conforme al promedio ponderado móvil, y toda posición abierta informa un costo de adquisición entero en centésimas igual a la porción del costo pagado que corresponde a los tokens abiertos.
- **SC-002**: En el 100 % de las posiciones con ventas parciales, el costo pagado es exactamente igual a la suma de los costos atribuidos más el costo de adquisición de la posición, sin diferencias de una centésima.
- **SC-003**: En el 100 % de las posiciones liquidadas por completo, el costo de adquisición de la posición es exactamente cero y el resultado realizado acumulado se conserva.
- **SC-004**: Dos cálculos consecutivos sobre el mismo estado de datos producen resultados idénticos, incluidos el costo de adquisición y los porcentajes derivados, con independencia del reloj de la consulta.
- **SC-005**: Ninguna publicación no ejecutada, cancelada o vencida, ni ninguna operación fallida, modifica la cantidad abierta, el costo de adquisición ni el resultado realizado de ninguna posición.
- **SC-006**: El resultado realizado de una posición es consultable aunque la tenencia actual sea cero, y no se reinicia al reabrir la posición.
- **SC-007**: Toda posición abierta sin cotización vigente informa el resultado no realizado como no definido, nunca como cero, y conserva su resultado realizado intacto.
- **SC-008**: El resultado total de una posición es exactamente la suma de su resultado realizado y su resultado no realizado cuando ambos están definidos; en una posición cerrada es igual al resultado realizado acumulado, y queda sin definido cuando el resultado no realizado no está definido.
- **SC-009**: Todos los importes informados son enteros en centésimas de crédito y los resultados negativos conservan su signo sin pérdida de precisión.
- **SC-010**: Cada importe de resultado informado se corresponde con un conjunto identificable de operaciones confirmadas, y ninguna operación se imputa dos veces a un mismo usuario.
- **SC-011**: El cálculo de la posición de un usuario no expone operaciones, tenencias ni resultados de ningún otro usuario.
- **SC-012**: Ningún cálculo modifica operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos.

## Assumptions

No se requieren supuestos funcionales adicionales a las decisiones aprobadas y las dependencias siguientes.

### Dependencias funcionales

- **SPEC-010**: dominio de mercado ya establecido — saldos de créditos, tenencias de tokens, emisión inicial por jugador, representación monetaria en centésimas, compra directa contra el superusuario y registro financiero append-only de compras confirmadas. Esta feature se apoya en ese dominio y no altera su comportamiento.
- **SPEC-011**: mercado secundario — publicaciones con precio congelado, estados efectivo y derivado, reservas de tokens, liquidación por compra de la publicación y registro financiero de la venta confirmada. Las publicaciones no liquidadas no producen venta ni resultado realizado.
- **REQ-S2-03**: cotización vigente por token. Su cálculo, persistencia y evolución no pertenecen a esta feature; la fuente vigente sigue siendo la que SPEC-010 y SPEC-011 utilizan. La ausencia de cotización se trata como resultado no realizado no definido, no como precio de respaldo.
- **SPEC-014** (REQ-S2-09): consumidor de esta fuente de verdad contable. Consume el costo promedio, el resultado realizado, el resultado no realizado, la precisión, los porcentajes y el orden contable aquí definidos, y MUST NOT redefinirlos. La implementación de sus cálculos financieros depende de que esta feature esté especificada.

Los objetivos de rendimiento generales corresponden al proyecto, conforme a `tp/enunciado.md` §5.3; no se fija aquí un umbral no acordado ni se confunde rapidez con integridad contable.

### Supuestos

- **Fuente de cotización disponible**: se asume que la fuente de cotización vigente puede consultarse al calcular el resultado no realizado. Su ausencia para un jugador concreto es un resultado no definido y su indisponibilidad técnica es un fallo; ninguno de los dos produce un precio inventado.
- **Reloj único del mercado**: se asume que, tras la unificación exigida por FR-019, todas las operaciones del mercado registran su instante de confirmación con la misma fuente de reloj, de modo que el par formado por ese instante y el identificador de la operación es un orden total.
- **Integridad referencial del catálogo**: se asume que la eliminación física de un jugador con operaciones históricas no está disponible, de modo que el caso vigente de un jugador que deja de estar en el catálogo es su desactivación lógica, con sus tenencias y operaciones conservadas.
- **Registro de operaciones completo**: se asume que el registro financiero de operaciones confirmadas se conserva y es consultable, y que a partir de él puede reconstruirse el costo de adquisición y los resultados conforme a las reglas de esta feature.
- **Nivel de aislamiento**: se asume el nivel por defecto de la plataforma y del servidor de base de datos, sin aislamiento reforzado configurado de forma explícita.

## Clarifications

### Decisiones funcionales vigentes

| ID | Decisión |
| --- | --- |
| CL-01 | El costo de adquisición de una posición se construye ponderando las compras confirmadas por su cantidad, de modo que refleja el promedio ponderado móvil de sus adquisiciones. Las ventas reducen la cantidad abierta y el costo de la posición de forma proporcional, con el costo atribuible redondeado a centésimas. El precio medio de adquisición se deriva del costo de adquisición y de la cantidad abierta: en aritmética exacta la venta no lo altera y solo el residuo de redondeo retenido puede desplazarlo. |
| CL-02 | El costo atribuido a una venta se redondea a un entero de centésimas y nunca excede el costo pagado pendiente de asignar. El residuo de redondeo se conserva en el costo de adquisición de la posición mientras exista, y se incorpora al resultado realizado al liquidarse la posición. La conservación del costo pagado es exacta. |
| CL-03 | El cálculo interno conserva la precisión fraccionaria; todos los importes expuestos son enteros en centésimas de crédito con un único modo de redondeo, al medio hacia arriba (HALF_UP). El costo de adquisición de la posición es la cifra autoritativa; el precio medio de adquisición se deriva de él y de la cantidad abierta, y su producto redondeado puede diferir del costo en el residuo retenido. |
| CL-04 | El resultado realizado es acumulativo por usuario y jugador e incluye todas las ventas confirmadas, incluso de ciclos anteriores. No se reinicia al cerrar ni al reabrir una posición. |
| CL-05 | El resultado no realizado es la diferencia entre el valor actual de la posición abierta, valuado con la cotización vigente que expone el backend, y su costo de adquisición. La ausencia de cotización deja el resultado no realizado sin definir; no equivale a cero. |
| CL-06 | El orden contable es el instante de confirmación de la operación y, ante instantes iguales, el identificador de la operación. Ninguna operación del mercado puede registrar su instante con una fuente de reloj distinta: el estado actual usa una en la compra directa y otra en la liquidación de publicaciones, y esa divergencia debe unificarse para que el orden sea comparable. |
| CL-07 | La emisión inicial de tokens no es una operación de mercado: los tokens emitidos tienen costo de adquisición cero, no generan resultado realizado y no modifican el costo medio como si fueran compras. Si una posición combinara tokens emitidos y adquiridos comercialmente, existiría un único costo medio y un único resultado, con los tokens emitidos aportando costo cero y diluyendo el promedio. |
| CL-08 | El porcentaje global de resultado del portfolio se calcula sobre el costo histórico total adquirido por el usuario —la suma de los importes pagados en todas sus compras confirmadas, incluidas las de posiciones ya cerradas—, no sobre el costo de adquisición de las posiciones abiertas ni sobre la división directa entre el resultado total y ese costo. |
| CL-09 | El modelo contable no contempla comisiones, impuestos, spreads, tasas ni conversión de moneda. El importe de una operación es exactamente el importe registrado en ella. |
| CL-10 | Esta feature define las reglas contables y entrega un componente de cálculo reutilizable en el dominio, sin endpoint propio, que la consulta de portfolio consume sin duplicar las reglas. |
| CL-11 | El costo de adquisición de la posición es el costo total remanente autoritativo y el precio medio de adquisición se deriva de ese costo y de la cantidad de tokens. Ningún consumidor recalcula el costo a partir del precio medio redondeado. |
| CL-12 | La unificación de la fuente del reloj de las operaciones pertenece al alcance de esta feature, aunque exija modificar código desarrollado en SPEC-010. No reabre SPEC-010 ni modifica su especificación ni su contrato público. |
| CL-13 | La regla de la mezcla entre tokens emitidos y tokens adquiridos comercialmente se mantiene como regla abstracta. No se modifica el mercado de tokens para volver alcanzable esa coexistencia. |
| CL-14 | El resultado total de una posición queda sin definido cuando su resultado no realizado no está definido por ausencia de cotización. No se informa como cero ni como el resultado realizado. En una posición cerrada el resultado total es igual al resultado realizado acumulado. |
| CL-15 | Ante un fallo técnico de la fuente de cotizaciones el cálculo falla completo y propaga el error, sin devolver ningún dato financiero de la posición, tampoco el resultado realizado. El fallo técnico se distingue de la ausencia de cotización. |
| CL-16 | La regla contable se aplica por igual a todos los vendedores. El superusuario, al vender tokens como contraparte del mercado, realiza el importe de la venta porque su costo de adquisición es cero. No se introduce una categoría especial de actividad de contraparte. |

### Sesión 2026-10-20

- Q: ¿Cómo se representa y redondea el costo promedio ponderado y el resultado financiero, dado que las reglas vigentes fijan los importes en centésimas enteras y hoy no existe ninguna operación de división? → A: El promedio se calcula con precisión fraccionaria exacta; los importes expuestos siguen siendo centésimas enteros, con redondeo explícito y uniforme, y el residuo se conserva en el costo de la posición abierta para que la conservación del costo sea exacta. No se modifica la representación monetaria vigente ni los contratos públicos de compra y venta.
- Q: ¿Qué criterio define el orden contable determinista de las operaciones? → A: El instante de confirmación de la operación y, ante instantes iguales, el identificador de la operación. Se exige además unificar la fuente del reloj de las operaciones de mercado, porque hoy la compra directa y la liquidación de publicaciones registran su instante con fuentes distintas y el orden contable no sería comparable entre ambas.
- Q: ¿Cómo se contabiliza la emisión inicial de tokens si una posición los mezcla con tokens adquiridos comercialmente? → A: Con un único costo medio por usuario y jugador: los tokens emitidos aportan costo cero y las adquisiciones comerciales posteriores diluyen ese costo medio. El resultado sigue siendo único.
- Q: ¿Esta feature define solo las reglas documentadas o también el componente de cálculo que las aplica? → A: Define las reglas y entrega un componente de cálculo reutilizable en el dominio, sin endpoint propio, que la consulta de portfolio consume. Así se desbloquea la implementación del portfolio dentro del Sprint 2.
- Q: ¿Qué fórmula define el porcentaje global de resultado del portfolio? → A: El resultado realizado más el no realizado, dividido por el costo histórico total adquirido por el usuario —la suma de los importes pagados en todas sus compras confirmadas—, en puntos porcentuales con dos decimales; nulo si el denominador es cero o falta una cotización necesaria.
- Q: ¿Se define el tratamiento de la mezcla entre tokens emitidos y adquiridos aunque hoy no pueda ocurrir? → A: Sí. La regla queda definida de forma abstracta, con un único costo medio y un único resultado, para que no quede una ambigüedad contable si el modelo de dominio permitiera esa situación.
- Q: ¿Dónde queda el residuo de redondeo? → A: En el costo de la posición abierta. Cada venta recibe el costo atribuido redondeado, y la diferencia permanece en la posición hasta su liquidación, donde se incorpora al resultado realizado. La conservación del costo pagado es exacta en todo momento.
- Q: ¿De dónde sale el valor actual? → A: De la cotización vigente que expone el backend, sin depender de una implementación concreta. Si no hay cotización, el resultado no realizado no está definido; si la fuente falla, el cálculo falla.
- Q: ¿La unificación del reloj de las operaciones pertenece a esta feature aunque exija modificar código de SPEC-010? → A: Sí. La unificación es alcance de SPEC-015 porque el orden contable no es comparable mientras existan dos fuentes de reloj. No se reabre SPEC-010 ni se modifica su especificación ni su contrato público: solo su implementación, conservando el comportamiento observable.
- Q: ¿Se mantiene la regla de la mezcla entre tokens emitidos y adquiridos aunque hoy no pueda ocurrir? → A: Sí, como regla abstracta con un único costo de adquisición y un único resultado. No se modifica el mercado para volverla alcanzable.
- Q: ¿Cuál es la cifra autoritativa del costo de una posición y cómo se obtiene el precio medio? → A: El costo de adquisición de la posición —el costo total remanente— es la cifra autoritativa. El precio medio de adquisición se deriva de ese costo y de la cantidad de tokens. SPEC-014 debe consumir ambos valores y no puede recalcular el costo desde el precio medio redondeado.
- Q: ¿Qué se informa como resultado total cuando la posición abierta no tiene cotización vigente? → A: El resultado total queda sin definir, igual que el resultado no realizado. No se informa como cero ni como el resultado realizado, para que la ausencia de cotización no se lea como una ganancia no realizada de cero. En una posición cerrada, el resultado total es igual al resultado realizado acumulado.
- Q: ¿Qué hace el cálculo cuando la fuente de cotizaciones falla por causa técnica? → A: Falla completo y propaga el error, sin devolver ningún dato financiero de la posición, tampoco el resultado realizado. Es coherente con la respuesta 503 sin datos parciales de la consulta de portfolio y evita que cada consumidor interprete el fallo de forma distinta.
- Q: ¿La venta del superusuario como contraparte del mercado genera resultado realizado? → A: Sí, con la misma regla que cualquier vendedor: su resultado realizado es igual al importe de la venta, porque su costo de adquisición es cero. Se prefiere una única regla auditable frente a una categoría especial de actividad de contraparte que el modelo vigente no representa.

## Out of Scope

- La consulta de portfolio y su interfaz web, que pertenecen a SPEC-014 y a las funcionalidades de visualización del frontend.
- El motor de cotizaciones, su persistencia y su historial, que pertenecen a REQ-S2-03.
- El historial detallado de transacciones del usuario, que pertenece a REQ-S2-10.
- Cualquier modificación de las reglas de ejecución, transferencia o auditoría de SPEC-010 y SPEC-011, de su especificación o de su contrato público. La unificación de la fuente del reloj exigida por FR-019 sí pertenece a esta feature y no altera el comportamiento observable de esas operaciones.
- La modificación del mercado de tokens —emisión, compra, venta o publicación— para hacer alcanzable la coexistencia de tokens emitidos y tokens adquiridos comercialmente en una misma posición.
- Comisiones, impuestos, spreads, tasas, recargas de créditos, conversión de moneda y cualquier mecanismo de creación de créditos distinto del saldo inicial vigente.
- Un libro de costo persistido, su conciliación y su mantenimiento; el cálculo se reconstruye a partir de las operaciones confirmadas.
- El motor de cotizaciones simulado como fuente de verdad de la valoración, y cualquier precio de respaldo.
- La corrección retroactiva de resultados históricos y la reexpresión de operaciones pasadas.
- Auditoría de invocaciones de servicios web y los entregables de Sprint 3 ajenos a la regla contable.
- La venta directa al superusuario como operación de mercado, hoy no contemplada por el modelo vigente.

## Trazabilidad

- **REQ origen**: REQ-S2-20 — Definir reglas contables de costo promedio y resultados financieros.
- **Issue de origen**: [#63](https://github.com/YoelSLA/football-market/issues/63) — `SPEC-015 | Definir reglas contables de costo promedio y resultados financieros`, en el milestone de Sprint 2.
- **Registro de asignación**: [`specs/assignments.md`](../assignments.md) — asignación explícita de `SPEC-015` con funcionalidad, responsable, issue de origen, requisito, rama y fecha.
- **Fuentes**: `tp/enunciado.md` §3.3, §3.4, §4 y §5.2; `tp/entregas.md`, Sprint 2 (03/11/2026); `specs/010-buy-tokens/`, `specs/011-sell-player-tokens/`, `specs/014-consultar-portfolio-personal/`.
- **Compatibilidad**: esta feature no modifica el comportamiento de SPEC-010 ni de SPEC-011, no reabre SPEC-010, no modifica su especificación, no expone ningún endpoint propio y no altera sus contratos públicos. Se apoya en su dominio de mercado y en sus registros financieros append-only. La unificación de la fuente del reloj exigida por FR-019 afecta a la implementación de las operaciones, no a su comportamiento observable.
- **Dependencia**: SPEC-014 consume esta fuente de verdad contable y no la reescribe; la implementación de sus cálculos financieros queda bloqueada hasta que esta feature esté especificada.
- **Precisión pendiente en el consumidor**: la decisión CL-03 de SPEC-014 afirma que las ventas reducen la cantidad y el costo asociado «sin modificar el costo promedio unitario restante». Con el residuo de redondeo retenido en la posición, esa afirmación es exacta en aritmética fraccionaria y puede desviarse en menos de una centésima por token sobre el precio medio redondeado. La divergencia no contradice la regla de SPEC-014, que delega aquí la definición del precio medio (FR-014 de SPEC-014), pero su texto debe precisarse en el origen, en la especificación de SPEC-014, por quien la mantiene.
- **Estado SDD al cerrar Clarify (2026-10-20)**: fases Specify y Clarify completadas. Las decisiones CL-01 a CL-16 están aprobadas e incorporadas, con tres decisiones nuevas tomadas en la sesión de Clarify: resultado total sin definir cuando no hay cotización, fallo técnico propagado sin datos parciales y regla uniforme para el superusuario como vendedor. No quedan decisiones funcionales pendientes ni marcadores de aclaración. La especificación queda en estado `Ready for planning`. Este documento no habilita Tasks ni implementación alguna.

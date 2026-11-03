# Feature Specification: Visualizar portfolio personal (REQ-S2-15)

<!-- MUST seguir Constitution y docs/development/sdd-workflow.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/017-view-personal-portfolio-ui` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `017-view-personal-portfolio-ui`, sin el prefijo de tipo. La convención de ramas es normativa; ver la constitución §7. El `<scope>` va en inglés, con independencia del idioma de la descripción de origen.

**Created**: 2026-10-21

**Status**: Ready for planning

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Visualizar portfolio personal en la sección Cartera del frontend: una pestaña Mi portfolio con el resumen financiero y una pestaña Mis tenencias con las posiciones del usuario, sobre GET /api/portfolio/me. Issue #26 (REQ-S2-15)."

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
-->

### User Story 1 - Conocer el resumen financiero de mi portfolio (Priority: P1)

Como usuario autenticado, quiero entrar a la sección Cartera y ver de inmediato el estado financiero
completo de mi portfolio —qué valen mis posiciones, cuánto invertí, cuánto gané o perdí y cuántas
tenencias tengo— para saber si mi inversión va bien sin recorrer jugador por jugador.

**Why this priority**: Es la pantalla de entrada de la sección y la que responde la pregunta
principal del usuario. Sin ella, Cartera no aporta información y las otras historias no tienen un
punto de entrada coherente.

**Independent Test**: Se puede probar por completo sin las posiciones ni el detalle: con un usuario
autenticado y un portfolio real o vacío, se verifica que la pantalla muestra todos los indicadores
del resumen con sus valores correctos. La acción que conduce a *Mis tenencias* se entrega junto con
esa segunda pestaña y se verifica en el conjunto de la feature, no en este incremento.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con posiciones valoradas, **When** entra a la sección Cartera,
   **Then** ve la pestaña *Mi portfolio* activa por defecto y el resumen financiero completo.
2. **Given** un usuario autenticado, **When** abre la pestaña *Mi portfolio*, **Then** ve el valor
   actual del portfolio, el capital invertido, el resultado total con su rentabilidad y la cantidad
   de posiciones, junto con el detalle de tenencias y de resultados financieros.
3. **Given** un usuario sin posiciones, **When** abre la pestaña *Mi portfolio*, **Then** ve un
   estado vacío comprensible y los indicadores en cero, nunca datos inventados.

---

### User Story 2 - Recorrer mis posiciones y filtrar por estado (Priority: P2)

Como usuario autenticado, quiero ver mis posiciones en tarjetas, una por jugador, con su foto y sus
datos financieros principales, y poder filtrarlas por abiertas o cerradas, para localizar
rápidamente la posición que me interesa.

**Why this priority**: Es el nivel de detalle que convierte el resumen en información accionable.
Depende de la historia anterior para ser descubrible, pero aporta valor por sí sola: el conjunto
de posiciones ya es consultable sin el panel de detalle.

**Independent Test**: Se puede probar por completo sin el panel de detalle: con un usuario que
tiene posiciones abiertas y cerradas, se verifica la grilla, el contenido de cada tarjeta, el filtro
y la paginación.

**Acceptance Scenarios**:

1. **Given** un usuario con posiciones, **When** abre la pestaña *Mis tenencias*, **Then** ve una
   grilla de tarjetas, una por jugador, con su foto, su nombre, su equipo, su posición deportiva, su
   estado y sus datos financieros principales.
2. **Given** un usuario con posiciones, **When** está en *Mis tenencias*, **Then** el filtro está en
   *Todas* y muestra posiciones abiertas y cerradas.
3. **Given** un usuario con posiciones abiertas y cerradas, **When** selecciona *Abiertas*, **Then**
   ve solo las posiciones abiertas; cuando selecciona *Cerradas*, ve solo las cerradas.
4. **Given** un usuario con más posiciones de las que caben en una página, **When** avanza de página,
   **Then** ve el siguiente grupo de posiciones y la página actual queda identificada.
5. **Given** un usuario con posiciones, **When** aplica un filtro, **Then** la vista vuelve a la
   primera página del conjunto filtrado.
6. **Given** un usuario sin posiciones, **When** abre la pestaña *Mis tenencias*, **Then** ve un
   estado vacío comprensible en lugar de una grilla vacía.
7. **Given** un usuario autenticado en *Mi portfolio*, **When** está en la sección Cartera con la
   pestaña *Mis tenencias* disponible, **Then** puede ir a *Mis tenencias* con una sola acción desde
   la propia pantalla.

---

### User Story 3 - Revisar el detalle completo de una posición (Priority: P3)

Como usuario autenticado, quiero abrir el detalle de una de mis posiciones y ver en un solo lugar
todos los datos disponibles de esa tenencia —identificación, tenencia, inversión, valoración,
actividad y resultados— para estudiar una posición concreta sin salir de la sección.

**Why this priority**: Es el nivel de detalle máximo de una posición. Aporta valor por sí mismo,
pero solo es alcanzable desde las tarjetas, por lo que se ubica después de la lista.

**Independent Test**: Se puede probar por completo sin la navegación entre pestañas: con un usuario
que tiene posiciones, se verifica que *Ver detalles* abre un panel con todos los datos de esa
posición y que el panel se puede cerrar.

**Acceptance Scenarios**:

1. **Given** un usuario en la grilla de *Mis tenencias*, **When** pulsa *Ver detalles* en una tarjeta,
   **Then** se abre un panel lateral con los datos de esa posición.
2. **Given** un panel de detalle abierto, **When** se lo consulta, **Then** muestra la
   identificación de la posición, su tenencia, su inversión y valoración, su actividad y sus
   resultados.
3. **Given** un panel de detalle abierto, **When** se lo cierra, **Then** se oculta y la grilla de
   posiciones queda como estaba.
4. **Given** un usuario, **When** abre el detalle de una posición sin cotización vigente, **Then**
   el panel informa la ausencia de cotización en lugar de mostrar una cifra.

---

### Criterios de aceptación transversales

Estas condiciones atraviesan las historias anteriores; no son historias independientes.

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC01 | Usuario autenticado con una sesión vigente | Consulta su portfolio desde la sección Cartera | La respuesta corresponde únicamente a ese usuario. La interfaz no permite consultar ni exhibiting datos de otro usuario, y no envía ningún identificador de usuario en la consulta. |
| AC02 | Usuario autenticado | Se solicitan `page`, `size` o `status` fuera de rango | La consulta se rechaza, la interfaz informa el error de forma comprensible y no muestra datos financieros parciales. |
| AC03 | Fuente de cotizaciones con falla técnica | El usuario consulta su portfolio | La interfaz informa que el servicio no está disponible y no muestra ningún dato financiero, ni siquiera los que no dependen de la cotización. La ausencia de cotización, que no es un error, se comunica de forma distinta. |
| AC04 | Usuario con una operación de mercado concurrente con la consulta | Se consulta su portfolio | Los importes, cantidades y resultados que aparecen juntos en la misma respuesta pertenecen a un mismo estado de los datos. |
| AC05 | Cualquier valor que el backend no informa | Se presenta el resumen o una tarjeta | La interfaz distingue la ausencia de dato del valor cero y no sustituye una ausencia por un cero. |

---

### Edge Cases

Clasificación conforme a Constitution §2.2; las mejoras recomendadas no condicionan la aceptación.

- **OBLIGATORIO — posición abierta sin cotización vigente:** la tarjeta muestra *Sin cotización* en el
  precio actual, un guion en el valor actual y en el resultado total, y conserva su cantidad, su
  cantidad reservada y su resultado realizado. No se reutiliza un precio histórico ni se muestra
  un cero en lugar del valor ausente.
- **OBLIGATORIO — posición cerrada:** no tiene valor actual ni resultado no realizado: la tarjeta
  muestra guiones en esos campos, su precio medio de adquisición es un guion y su resultado total es
  su resultado realizado acumulado. Se distingue de una posición abierta sin cotización. El costo
  de adquisición de una posición liquidada es un cero real informado por el backend, no una
  ausencia: se muestra como `0,00` créditos y no como guion.
- **OBLIGATORIO — resultado negativo:** una pérdida se presenta con su signo, con tratamiento
  visual distinguible del resultado positivo y del resultado nulo.
- **OBLIGATORIO — denominador contable cero:** un porcentaje que el backend no informa se muestra
  como guion, nunca como 0 %.
- **OBLIGATORIO — filtro sin coincidencias:** un filtro que no deja posiciones muestra un estado
  vacío del conjunto filtrado, distinto del estado vacío de un usuario sin posiciones.
- **OBLIGATORIO — página fuera de rango:** la vista no queda en blanco ni muestra un error: se
  muestra el conjunto válido y la página existente.
- **OBLIGATORIO — fallo de la consulta:** la pantalla comunica el fallo, permite reintentar sin
  recargar la aplicación y no muestra importes incompletos.
- **OBLIGATORIO — fotografía ausente o que no carga:** se muestra el avatar predeterminado sin
  perder el nombre ni ningún dato de la tarjeta.
- **OBLIGATORIO — datos descriptivos no recuperables:** un dato descriptivo que el backend no
  informa se muestra como ausente y no se inventa.
- **OBLIGATORIO — cambio de datos entre consultas:** el backend no garantiza una instantánea
  compartida entre consultas distintas. Si los datos del usuario cambian entre la consulta que
  alimenta el resumen y la que alimenta la lista, las dos pestañas pueden mostrar cifras de
  momentos distintos. Cada pantalla es internamente coherente con su propia consulta y ninguna de
  las dos es incorrecta por ello.
- **RECOMENDADO — orden estable ante cambios concurrentes:** conservar el mismo orden en consultas
  sucesivas es una mejora no bloqueante. El orden que la interfaz presenta es el que entrega el
  backend.
- **RECOMENDADO — granularidad del panel de detalle:** ampliar el detalle más allá de los campos
  aprobados es una mejora no bloqueante.

## Requirements *(mandatory)*

### Functional Requirements

Los requisitos siguientes son obligatorios para las historias de esta feature.

#### Navegación y alcance de la sección

- **FR-001**: La sección Cartera de la navegación principal MUST habilitarse y MUST permitir el
  acceso a la vista de portfolio del usuario autenticado. La habilitación MUST NOT alterar la
  estructura, el orden ni el tratamiento visual de las demás secciones.
- **FR-002**: La sección Cartera MUST presentar dos pestañas navegables: *Mi portfolio*, que es la
  predeterminada y la que se muestra al entrar a la sección, y *Mis tenencias*. Las pestañas
  *Historial de transacciones* y *Estadísticas/gráficos* MUST permanecer deshabilitadas y rotuladas
  como pendientes, sin ruta ni navegación.
- **FR-003**: La interfaz MUST NOT habilitar ninguna operación de mercado, compra, venta ni
  publicación desde la sección Cartera.

#### Fuente de datos y reglas financieras

- **FR-004**: Todos los indicadores y todas las posiciones mostrados MUST obtenerse de la consulta
  del portfolio propio ya disponible en el backend. La interfaz MUST NOT calcular, estimar ni
  redefinir cotizaciones, costos de adquisición, resultados, rentabilidades, saldos ni ningún otro
  indicador financiero, y MUST NOT derivar un valor que el backend no informe.
- **FR-005**: Los indicadores financieros y los porcentajes MUST presentarse tal como los informa
  el backend, con su signo y su valor, sin recomputar su fórmula. En particular, el porcentaje
  global de resultado MUST NOT obtenerse dividiendo el resultado total por el capital invertido.
- **FR-006**: Los importes se reciben en centésimas de crédito y MUST presentarse en créditos con
  dos decimales. Los porcentajes MUST presentarse con dos decimales. Las fechas MUST presentarse en
  formato legible, con día, mes, año, hora y minutos. Un importe negativo MUST conservar su signo.
- **FR-007**: La interfaz MUST NOT enviar ningún identificador de usuario en la consulta: la
  identidad del portfolio consultado MUST obtenerse de la sesión autenticada vigente.

#### Pestaña Mi portfolio

- **FR-008**: La pestaña *Mi portfolio* MUST presentar la totalidad de los indicadores del resumen
  del portfolio: valor actual del portfolio, capital invertido, resultado total y su rentabilidad,
  cantidad de posiciones, total de tokens, tokens disponibles, tokens reservados, compras
  realizadas, ventas realizadas, resultado realizado, resultado no realizado y estado de
  completitud de la valoración.
- **FR-009**: La agrupación y el orden de presentación de los indicadores MUST ser los de la
  referencia visual aprobada: indicadores principales, detalle de tenencias y resultados
  financieros. La interfaz MUST NOT omitir ningún indicador del resumen.
- **FR-010**: Los indicadores del resumen MUST representar el portfolio completo y MUST NOT
  depender del filtro de estado ni de la página consultada. La pestaña *Mi portfolio* MUST NOT
  ofrecer filtros ni paginación. En particular, la cantidad de posiciones MUST corresponder a las
  posiciones **abiertas** y MUST rotularse de forma explícita como tal, de modo que no pueda
  confundirse con la cantidad total de posiciones que la pestaña *Mis tenencias* lista, que
  incluye las cerradas.
- **FR-011**: La pestaña *Mi portfolio* MUST ofrecer una acción que conduzca a la pestaña *Mis
  tenencias*.
- **FR-012**: Cuando el portfolio no tenga posiciones, la pestaña MUST presentar un estado vacío
  comprensible y los indicadores numéricos en cero, sin ocultar la estructura de la pantalla ni
  simular posiciones.

#### Pestaña Mis tenencias

- **FR-013**: La pestaña *Mis tenencias* MUST presentar una tarjeta por posición, con el retrato del
  jugador o el avatar predeterminado, su nombre, su equipo, su posición deportiva, su estado
  abierto o cerrado, su cantidad total de tokens, su cantidad disponible, su cantidad reservada, su
  precio medio de adquisición, su precio actual, su valor actual, su resultado total y una acción
  *Ver detalles*.
- **FR-014**: La grilla MUST presentar seis tarjetas por página, dispuestas en tres columnas por dos
  filas en pantalla ancha.
- **FR-015**: La paginación MUST solicitar al backend el tamaño de página acordado y MUST aprovechar
  los metadatos de paginación que la consulta devuelve. La numeración de páginas MUST
  comenzar en cero, como en el resto de la aplicación. La vista MUST comunicar la cantidad total
  de posiciones del conjunto filtrado y la cantidad mostrada en la página en curso. Los controles de
  paginación MUST estar presentes aunque el conjunto filtrado ocupe una sola página, en cuyo caso
  MUST reflejar que solo hay una.
- **FR-016**: La vista MUST ofrecer un filtro por estado con las opciones *Todas*, *Abiertas* y
  *Cerradas*. El valor inicial MUST ser *Todas*, que corresponde a consultar sin filtro de estado.
  El filtro MUST aplicarse al estado vigente de cada posición y MUST corresponderse con los estados
  que el backend distingue: *Abiertas* corresponde a las posiciones abiertas y *Cerradas* a las
  cerradas. La interfaz MUST NOT ofrecer un estado que el backend no distinga.
- **FR-017**: Al cambiar el filtro, la vista MUST volver a la primera página del conjunto filtrado,
  y el filtro MUST NOT alterar los indicadores del resumen.
- **FR-018**: El orden de las tarjetas MUST ser el que entrega el backend y MUST NOT reordenarse por
  criterio propio.
- **FR-019**: Cuando el conjunto filtrado no tenga posiciones, la vista MUST presentar un estado
  vacío comprehensible que distinga ese caso del de un usuario sin posiciones.

#### Panel de detalle de una posición

- **FR-020**: La acción *Ver detalles* MUST abrir un panel lateral con la información disponible de
  esa posición, sin abandonar la sección Cartera. El panel MUST superponerse a la grilla de
  posiciones, que permanece visible detrás de él y no interactiva mientras el panel está abierto, y
  MUST NOT modificar la ruta activa. Mientras el panel está abierto, el resto del contenido de la
  pestaña —incluidos los controles de filtro y de paginación— MUST permanecer inerte. Al cerrarse,
  la grilla MUST volver a su estado anterior.
- **FR-021**: El panel MUST presentar la identificación de la posición —su identificador de
  jugador, si el jugador está activo, su estado y su estado de valoración—, su tenencia —tokens
  totales, disponibles y reservados—, su inversión y valoración —costo de adquisición, precio
  medio de adquisición, precio actual, valor actual, peso en el portfolio e instante de la última
  cotización—, su actividad —compras, ventas, apertura, cierre y última transacción— y sus
  resultados —realizado, no realizado, total y rentabilidad no realizada—.
- **FR-022**: El panel MUST presentar los datos de la posición que la pantalla ya dispone y MUST
  NOT requerir una consulta adicional al backend, ni para abrirlo ni para mostrarlo.
- **FR-023**: El panel MUST poder cerrarse y, al cerrarse, MUST NOT alterar los datos, el orden, el
  filtro ni la página de la grilla de posiciones.
- **FR-024**: El panel MUST comunicar la ausencia de cotización de la misma forma en que lo hace la
  tarjeta, de modo que la ausencia de dato sea interpretable en ambos lugares.
- **FR-025**: El panel MUST NOT ofrecer acciones de mercado ni operaciones sobre la posición.

#### Estados de la interfaz

- **FR-026**: La pantalla MUST comunicar el estado de carga de la consulta, el estado vacío
  correspondiente y el estado de error de recuperación, incluido el fallo técnico del servicio de
  cotizaciones.
- **FR-027**: Ante un fallo de la consulta, la interfaz MUST ofrecer la posibilidad de reintentar sin
  exigir la recarga de la aplicación y MUST NOT mostrar importes financieros parciales.
- **FR-028**: La interfaz MUST distinguir la ausencia de cotización de una posición abierta —que no
  es un error— de la falla técnica del servicio de cotizaciones —que sí lo es—, y MUST NOT presentar
  en el segundo caso ningún dato financiero.
- **FR-029**: Cuando un valor no esté disponible, la interfaz MUST mostrar una ausencia explícita y
  MUST NOT mostrar un cero ni un guion que se confunda con un valor real.
- **FR-030**: La fotografía de un jugador MUST mostrarse cuando el backend la informe y, cuando no
  exista o no cargue, MUST mostrarse el avatar predeterminado de la aplicación, conservando el
  nombre y el resto de los datos de la tarjeta.

#### Presentación, estilo y accesibilidad

- **FR-031**: La vista MUST conservar el navbar, el tema oscuro y los estilos vigentes de Football
  Market. Los componentes nuevos MUST consumir los roles semánticos de color y los tokens
  existentes y MUST NOT introducir un lenguaje visual propio ni valores cromáticos literales.
- **FR-032**: La sección MUST conservar la disposición y la rotulación de la referencia visual
  aprobada: la barra de pestañas internas bajo el navbar, la retícula de tres columnas, el bloque
  de filtros y el bloque de paginación. El texto visible de las pantallas MUST usar el registro
  verbal de tuteo vigente en la aplicación, con independencia del registro del texto ilustrado en la
  referencia visual.
- **FR-033**: Un resultado positivo y un resultado negativo MUST ser distinguibles entre sí y de un
  resultado ausente, por signo y por tratamiento visual, sin depender únicamente del color.
- **FR-034**: La vista MUST ser legible y utilizable en distintos tamaños de pantalla. El número de
  tarjetas por página MUST permanecer en seis y la retícula MUST readaptar su número de columnas a
  dos o una cuando el ancho disponible no admita tres, de modo que ninguna tarjeta se corte ni se
  solape y ninguna tarjeta quede fuera de la página por esa readaptación.
- **FR-035**: El panel lateral de detalle MUST ser utilizable con teclado y MUST poder cerrarse con
  la tecla de escape, con foco gestionado y con la exposición accesible correspondiente a un
  diálogo.
- **FR-036**: El resultado visual de las consultas de la sección MUST ser coherente con el de las
  pantallas existentes de la aplicación en tema, densidad, jerarquía y tratamiento de los estados.

#### Alcance de los datos mostrados

- **FR-037**: La interfaz MUST NOT presentar datos históricos de operaciones del usuario ni listados
  de transacciones dentro de una posición. Ese historial corresponde al requerimiento de historial
  de transacciones.
- **FR-038**: La interfaz MUST NOT presentar gráficos, series ni indicadores analíticos del
  portfolio. Esos elementos corresponden al requerimiento de datos de usuario con gráficos.
- **FR-039**: La sección MUST NOT presentar datos de demostración ni simular posiciones,
  cotizaciones o resultados. Los importes mostrados son los del portfolio real del usuario. Las
  menciones a datos de ejemplo de la referencia visual son un artefacto de la maqueta y MUST NOT
  aparecer en la interfaz.

### Key Entities *(include if feature involves data)*

- **Posición**: Exposición del usuario sobre un jugador. Se presenta como una tarjeta en la grilla y
  como el contenido del panel de detalle. Es lo que la persona usuaria consulta; no es un jugador,
  ni una tenencia, ni una operación.
- **Resumen del portfolio**: Conjunto de indicadores globales del portfolio del usuario, sobre la
  totalidad de sus posiciones. Es independiente del filtro y de la página. Es lo que presenta la
  pestaña *Mi portfolio*.
- **Indicador financiero**: Cifra del resumen expresada en créditos o en porcentaje, con su
  ausencia explícita cuando el backend no la informa.
- **Estado de la posición**: Condición abierta o cerrada de la tenencia del usuario sobre un
  jugador. Determina qué datos de valoración existen para esa posición.
- **Estado de valoración**: Condición de la valoración de una posición: valorada, sin valoración o
  no aplicable por estar cerrada. Determina si existe precio actual y valor actual.
- **Tenencia**: Cantidad de tokens de un jugador propiedad del usuario, de la que forman parte los
  tokens reservados y los disponibles.
- **Cotización vigente**: Precio actual de un token de un jugador. Su cálculo no pertenece a esta
  feature; su ausencia se comunica como ausencia y no como error.
- **Jugador**: Recurso del catálogo con nombre, equipo, posición deportiva, imagen y estado activo.
  La sección no permite operar sobre él ni consultar su pantalla de detalle.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El usuario autenticado obtiene la totalidad de los indicadores financieros de su
  portfolio en una sola pantalla, con una sola consulta al backend y sin abrir el detalle de
  ninguna posición ni solicitar información adicional por jugador.
- **SC-002**: Los trece indicadores del resumen están presentes en la pantalla al mismo tiempo,
  sin paginación, sin diálogo y sin necesidad de desplazarse dentro de un bloque de indicadores.
- **SC-003**: Todos los importes, porcentajes y conteos mostrados coinciden exactamente con los que
  la consulta del backend devuelve para la misma petición: cada importe visible es el valor
  informado expresado en créditos con dos decimales, cada porcentaje es el informado con dos
  decimales y cada conteo es el informado. Ninguna cifra visible difiere de la informada.
- **SC-004**: Ningún valor que el backend no informa se muestra como cero: cada ausencia se
  presenta de forma distinguible de un valor real, en el resumen, en la tarjeta y en el panel.
- **SC-005**: El usuario alcanza el detalle completo de cualquiera de sus posiciones con una sola
  acción desde su tarjeta, y lo cierra con una sola acción, sin abandonar la sección.
- **SC-006**: Al filtrar por *Abiertas* o por *Cerradas*, la vista muestra exclusivamente
  posiciones del estado seleccionado y ninguna del otro estado.
- **SC-007**: Cambiar de filtro o de página no altera los indicadores del resumen: el resumen
  mostrado es el mismo antes y después de cualquier cambio de filtro o de paginación.
- **SC-008**: Ante un fallo de la consulta, el usuario ve un mensaje comprensible y puede reintentar
  sin recargar la aplicación, y en ningún momento ve importes financieros parciales.
- **SC-009**: La sección es utilizable de extremo a extremo con anchos de 360, 768 y 1280 píxeles:
  ninguna tarjeta queda cortada, ninguna etiqueta queda truncada y el panel de detalle permanece
  accesible.
- **SC-010**: Todas las acciones de la sección son alcanzables con teclado, incluido el cierre del
  panel de detalle.
- **SC-011**: El usuario distingue, sin ayuda externa, la cantidad de posiciones que puede tener
  abiertas de la cantidad total de posiciones que la sección lista, y no interpreta una como la
  otra.

## Assumptions

- El endpoint de consulta del portfolio propio del backend está implementado y disponible, con su
  contrato vigente en `specs/014-view-personal-portfolio/contracts/portfolio-api.md`. Esta feature
  es su consumidor y no su autor.
- Las reglas contables de costo de adquisición, resultados y rentabilidades son las que fija el
  requerimiento de reglas contables, ya especificado en
  `specs/015-define-average-cost-and-financial-results-rules/`. Esta feature no las redefine.
- Los importes llegan en centésimas de crédito y los porcentajes en puntos porcentuales con dos
  decimales, según el contrato vigente.
- El tamaño de página acordado —seis posiciones— se solicita al backend, que por defecto devuelve
  veinte.
- La navegación principal del frontend es declarativa: habilitar la sección Cartera y sus dos
  pestañas es el único cambio de navegación necesario para esta feature.
- El backend no expone la imagen alternativa de un jugador en la consulta del portfolio, de modo que
  la tarjeta y el panel solo pueden contar con la imagen principal y con el avatar predeterminado.
  Ese avatar es el ícono genérico que la aplicación ya usa cuando un jugador no tiene retrato
  utilizable, conforme a lo establecido en el requerimiento de imágenes de jugadores.
- La referencia visual aprobada es `referencias-imagenes/01-mi-portfolio.svg` para la pestaña *Mi
  portfolio*, `referencias-imagenes/02-mis-tenencias.svg` para la grilla de *Mis tenencias* y
  `referencias-imagenes/03-mis-tenencias-detalle-abierto.svg` para el panel de detalle. Esas
  referencias fijan disposición, jerarquía, rotulación y tratamiento de los estados; los valores
  que muestran son ficticios y los determina el backend.
- Cuando la referencia visual y el contrato del backend discrepan, prevalece el contrato. En
  particular, la cantidad de posiciones del resumen corresponde a las posiciones abiertas, mientras
  que la grilla con el filtro *Todas* muestra también las cerradas.
- La sección no incorpora un modo de demostración propio: sus datos provienen del backend.

## Clarifications

### Decisiones funcionales vigentes

Las decisiones siguientes provienen de la aprobación humana de esta feature y rigen sobre el
diseño visual, sin perjuicio de que la referencia visual las ilustre.

| ID | Decisión |
| --- | --- |
| DC-01 | La sección Cartera tiene dos pestañas: *Mi portfolio*, predeterminada, y *Mis tenencias*. |
| DC-02 | *Mi portfolio* muestra todos los indicadores relevantes del resumen financiero. |
| DC-03 | *Mis tenencias* muestra seis tarjetas por página, en tres columnas por dos filas, con foto, datos financieros principales y los filtros *Todas*, *Abiertas* y *Cerradas*. El filtro inicial es *Todas*. |
| DC-04 | Cada tarjeta ofrece *Ver detalles*, que abre un panel lateral con toda la información disponible de la posición. |
| DC-05 | Los datos provienen de la consulta del portfolio propio del backend, sin duplicar cálculos financieros. |
| DC-06 | Se muestra *Sin cotización* cuando corresponde y un avatar predeterminado cuando el jugador no tiene imagen. |
| DC-07 | Se mantienen el navbar, el tema oscuro y los estilos actuales de Football Market. |
| DC-08 | El historial de transacciones y los gráficos quedan fuera de alcance. |
| DC-09 | El indicador de posiciones de *Mi portfolio* cuenta las posiciones **abiertas** y se rotula de forma explícita como tal, para que no se confunda con el total de posiciones listadas en *Mis tenencias*. |
| DC-10 | El texto visible de las pantallas nuevas usa el registro verbal de **tuteo**, vigente en la aplicación, con independencia del texto ilustrado en la referencia visual. |

### Sesión 2026-10-21 — Specify

Las decisiones funcionales pendientes se resolvieron con el responsable del proyecto y quedan
incorporadas a los requisitos correspondientes. Los tres identificadores de aclaración se retiraron
de los requisitos: `Q1` en FR-034, `Q2` en FR-020 y `Q3` en FR-039.

- Q1 — en pantallas estrechas, la retícula de *Mis tenencias* se readapta a menos columnas
  manteniendo seis tarjetas por página, en lugar de reducir el tamaño de página → A: se readapta la
  retícula, a dos o una columnas, y el tamaño de página permanece en seis (FR-034).
- Q2 — el panel de detalle se superpone a la grilla de posiciones, que permanece visible detrás y
  no interactiva, en lugar de sustituir el contenido de la pestaña → A: superposición, sin cambio de
  ruta activa y con restitución de la grilla al cerrar (FR-020).
- Q3 — las menciones a datos de ejemplo de la referencia visual se omiten, por tratarse de un
  artefacto de la maqueta y de datos reales del backend, en lugar de incorporar un modo de
  demostración del portfolio → A: se omiten (FR-039).

### Sesión 2026-10-21 — Clarify

El escaneo de ambigüedades sobre la especificación, el issue #26, las decisiones registradas y las
referencias visuales aprobadas detectó dos cuestiones que la documentación existente no resolvía por
sí sola. Ambas se resolvieron con el responsable del proyecto y quedaron incorporadas a los
requisitos correspondientes, como DC-09 y DC-10.

- Q: ¿Qué debe mostrar y cómo rotularse el indicador de posiciones de *Mi portfolio*, si el resumen
  del backend cuenta solo posiciones abiertas y la lista de *Mis tenencias* muestra abiertas y
  cerradas? → A: el conteo de posiciones abiertas del resumen, con un rótulo explícito que lo
  distinga del total de posiciones listadas (FR-010, DC-09).
- Q: ¿Con qué registro verbal se redacta el texto de las pantallas nuevas, si la referencia visual
  escribe en voseo y la aplicación existente usa tuteo? → A: tuteo, como el resto de la aplicación,
  con independencia del texto ilustrado en la referencia visual (FR-032, DC-10).
- Q: ¿En qué incremento se entrega la acción que conduce de *Mi portfolio* a *Mis tenencias*? → A: junto
  con la segunda pestaña, en el mismo incremento en que esa pestaña queda disponible. La acción no se
  entrega antes de que exista su destino, de modo que cada incremento por separado es funcional y
  verificable por sí solo. El requisito FR-011 no cambia: la acción existe en la feature completa
  (US2, escenario 7).

El escaneo también resolvió por evidencia, sin requerir decisión humana, los siguientes puntos,
que quedaron registrados en la especificación: la correspondencia entre los rótulos del filtro y los
estados que el backend distingue (FR-016); la inactividad de todo el contenido de la pestaña —
incluidos filtro y paginación— mientras el panel de detalle está abierto (FR-020); la presencia de
los controles de paginación aun con una sola página (FR-015); el costo de adquisición de una
posición liquidada como cero real y no como ausencia (casos borde); y la ausencia de una instantánea
compartida entre la consulta del resumen y la consulta de la lista, con la consecuencia de que ambas
puedan mostrar momentos distintos (casos bordes y AC04). Además se normalizó la terminología —«tarjeta»
en lugar de «ficha»— conforme a DC-03, se vinculó el avatar predeterminado con el ícono genérico ya
usado por la aplicación, y se precisaron SC-002, SC-003 y SC-011 para que sean verificables.

No quedan decisiones funcionales pendientes en esta especificación. La fase Clarify queda cerrada
con esta sesión; el diseño técnico sigue requiriendo su propia fase y su propia autorización.

## Out of Scope

- La lógica de backend de cálculo y consulta del portfolio, su endpoint y su contrato.
- Calcular, estimar o redefinir cotizaciones, costos de adquisición, resultados, rentabilidades o
  saldos en la interfaz, y cualquier regla de presentación que altere el significado de las cifras
  informadas por el backend.
- Ejecutar compras, ventas o publicaciones de tokens, y cualquier acción de mercado sobre una
  posición.
- La visualización del historial de transacciones del usuario, que corresponde al requerimiento de
  historial de transacciones, y el detalle de operaciones dentro de una posición.
- Los gráficos, series e indicadores analíticos del portfolio, que corresponden al requerimiento de
  datos de usuario con gráficos.
- La ampliación de la consulta del portfolio con paginación adicional o con filtros que el backend
  no expone.
- La consulta de la tarjeta de un jugador desde la sección Cartera y la navegación hacia ella.
- Un modo de demostración del portfolio y sus datos ficticios.
- La modificación del navbar, de su estructura, de su navegación o del lenguaje visual de la
  aplicación.

## Trazabilidad

- **REQ origen**: `REQ-S2-15` — Visualizar portfolio personal (épica
  `[EPIC] REQ-S2-15 - Visualizar portfolio personal`).
- **Issue de origen**: [#26](https://github.com/YoelSLA/football-market/issues/26) —
  `SPEC-017 | Visualizar portfolio personal`, en el milestone de Sprint 2.
- **Registro de asignación**: [`specs/assignments.md`](../assignments.md) — asignación explícita de
  `SPEC-017` con funcionalidad, responsable, issue de origen, requisito, rama y fecha.
- **Requisito previo**: [#20](https://github.com/YoelSLA/football-market/issues/20) —
  `SPEC-014 | Consultar portfolio personal`, cerrado. Aporta la consulta del portfolio propio y su
  contrato, que esta feature consume sin redefinirlo.
- **Dependencia contable**: [#63](https://github.com/YoelSLA/football-market/issues/63) —
  `SPEC-015 | Definir reglas contables de costo promedio y resultados financieros`, cerrado. Es la
  fuente de verdad de las reglas de costo y resultado que esta feature se limita a presentar.
- **Funcionalidades relacionadas, no bloqueantes**: [#27](https://github.com/YoelSLA/football-market/issues/27)
  — historial de transacciones; [#31](https://github.com/YoelSLA/football-market/issues/31) — datos
  de usuario con gráficos.
- **Fuentes**: `tp/enunciado.md` §3.4 — el portfolio del usuario expone cantidad de tokens por
  jugador, precio promedio de compra, valor actual y ganancia o pérdida; el historial de operaciones
  de esa sección queda fuera de esta feature. `tp/entregas.md`, Sprint 2 (03/11/2026).
- **Referencia visual aprobada**: `referencias-imagenes/01-mi-portfolio.svg`,
  `referencias-imagenes/02-mis-tenencias.svg` y
  `referencias-imagenes/03-mis-tenencias-detalle-abierto.svg`.
- **Compatibilidad**: esta feature es de solo lectura y no altera el comportamiento de la consulta
  del portfolio, de las reglas contables ni de las operaciones de mercado.
- **Estado SDD al cerrar Clarify (2026-10-21)**: fase `Clarify` completada. El escaneo de
  ambigüedades sobre la especificación, el issue de origen, las decisiones registradas y las
  referencias visuales aprobadas no dejó decisiones funcionales pendientes: las dos cuestiones que
  requerían decisión humana quedaron resueltas y registradas como DC-09 y DC-10. El alcance, los
  requisitos y los criterios de aceptación están suficientemente claros para diseñar la solución, y
  la especificación queda en estado `Ready for planning`. La existencia de este documento no
  habilita implementación alguna, y la autorización de implementación sigue requiriendo una
  autorización humana explícita y vigente.

### Cobertura del requerimiento de origen

El issue #26 declara los siguientes requisitos y criterios de aceptación. Cada uno queda cubierto
por los requisitos y las historias de esta especificación.

| Origen | Requisito o criterio del issue | Cubierto por |
| --- | --- | --- |
| RF-01 | Acceso al portfolio | FR-001, FR-002; US1 escenario 1 |
| RF-02 | Posiciones: jugadores y cantidad de tokens | FR-013, FR-018; US2 escenario 1 |
| RF-03 | Información de valoración: precio promedio y cotización actual | FR-013, FR-021; AC05 |
| RF-04 | Valor actual | FR-013, FR-021 |
| RF-05 | Resultado de la inversión, con su significado claro | FR-013, FR-021, FR-033; US1 escenario 2 |
| RF-06 | Consistencia con el backend, sin reglas de cálculo alternativas | FR-004, FR-005; AC05 |
| RF-07 | Privacidad: solo la información autorizada | FR-007; AC01 |
| RF-08 | Estados de consulta: carga, ausencia de posiciones y errores | FR-012, FR-019, FR-026, FR-027, FR-028 |
| RF-09 | Adaptabilidad a distintos tamaños de dispositivo | FR-034, FR-035; SC-009, SC-010 |
| CA-01 | Acceso a la vista del portfolio personal | FR-001, FR-002; US1 escenario 1 |
| CA-02 | Posiciones con jugador y cantidad de tokens | FR-013; US2 escenario 1 |
| CA-03 | Precio promedio y cotización actual cuando el backend los proporciona | FR-013, FR-021; AC05 |
| CA-04 | Valor actual consultable | FR-013, FR-021 |
| CA-05 | Ganancias y pérdidas comprensibles | FR-033; US1 escenario 2 |
| CA-06 | Consistencia con el backend y ausencia de posiciones de terceros | FR-004, FR-007; AC01, AC05 |
| CA-07 | Estado vacío comprensible | FR-012, FR-019; US1 escenario 3, US2 escenario 6 |
| CA-08 | Estados de carga y de error comunicados | FR-026, FR-027, FR-028; SC-008 |
| CA-09 | Legibilidad y accesibilidad en distintos tamaños | FR-034, FR-035; SC-009, SC-010 |

El historial de transacciones, la lista de operaciones dentro de una posición y los gráficos que el
issue declara fuera de alcance quedan excluidos en la sección *Out of Scope*.

# Feature Specification: Visualizar historial personal de transacciones

**Feature Branch**: `feat/025-view-transaction-history`

**Created**: 2026-11-10

**Status**: Ready for planning

**Input**: User description: "Como usuario autenticado de Football Market, quiero visualizar mi
historial de transacciones desde la interfaz web para consultar las compras y ventas que realicé y
comprender mi actividad dentro del mercado."

**Trazabilidad**: SPEC-025 materializa `REQ-S2-16` del issue
[#27](https://github.com/YoelSLA/football-market/issues/27), conforme a la asignación explícita
registrada en [`specs/assignments.md`](../assignments.md). Consume el historial personal provisto
por SPEC-020 y adopta el
[wireframe aprobado](../../docs/wireframes/transaction-history.md) como referencia visual.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar compras y ventas personales (Priority: P1)

Como usuario autenticado, quiero acceder desde mi cartera a un historial cronológico de mis
compras y ventas confirmadas para comprender qué operaciones realicé y con qué valores.

**Why this priority**: Es el valor central del requerimiento y permite consultar la actividad
financiera propia sin generar ni modificar operaciones.

**Independent Test**: Puede verificarse con un usuario que tenga compras y ventas confirmadas: al
abrir el historial desde Cartera, visualiza cada operación propia una sola vez, desde la más
reciente, con tipo, jugador, cantidad, precio, importe, fecha e identidad de transacción.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con compras confirmadas, **When** accede a `Cartera > Historial
   de transacciones`, **Then** cada compra aparece identificada como `COMPRA` con su jugador,
   cantidad, precio unitario histórico, importe total histórico, fecha y hora e identificador de
   transacción.
2. **Given** un usuario autenticado con ventas confirmadas, **When** consulta el historial,
   **Then** cada venta aparece identificada como `VENTA` con los mismos datos financieros
   disponibles.
3. **Given** un usuario con varias operaciones, **When** se muestra el historial, **Then** las
   operaciones conservan el orden de la más reciente a la más antigua y cada transacción aparece
   exactamente una vez.
4. **Given** dos usuarios con historiales distintos, **When** cada uno consulta la pantalla desde
   su sesión, **Then** cada usuario visualiza exclusivamente sus propias operaciones y no dispone
   de un control para seleccionar otra identidad.
5. **Given** una operación mostrada, **When** el usuario observa el nombre del jugador, **Then** el
   nombre se presenta como texto informativo y no actúa como enlace, botón o redirección.

---

### User Story 2 - Comprender el estado de la consulta (Priority: P1)

Como usuario autenticado, quiero recibir indicaciones claras mientras se consulta el historial y
cuando no puede recuperarse, para distinguir una espera, un error y una ausencia real de
operaciones.

**Why this priority**: Sin estados inequívocos, el usuario podría interpretar una demora o un
fallo como si nunca hubiera operado, lo que afecta la confianza en información financiera.

**Independent Test**: Puede verificarse provocando sucesivamente una consulta pendiente y un
error: la pantalla comunica el estado activo, ofrece reintentar tras el error y nunca presenta
datos parciales como si fueran el historial vigente.

**Acceptance Scenarios**:

1. **Given** una consulta en curso, **When** la pantalla espera el resultado, **Then** comunica que
   está cargando el historial y no informa cero operaciones.
2. **Given** una consulta que falla, **When** se presenta el error, **Then** se comunica que el
   historial no pudo cargarse, se aclara que las operaciones no fueron modificadas y se ofrece una
   acción para reintentar.
3. **Given** un error visible, **When** el usuario elige reintentar y la nueva consulta resulta
   exitosa, **Then** el error se reemplaza por el historial o por el estado vacío correspondiente.
4. **Given** una sesión ausente o inválida, **When** se intenta acceder al historial, **Then** se
   aplica la protección de acceso vigente sin exponer operaciones privadas.

---

### User Story 3 - Orientarse cuando todavía no hay operaciones (Priority: P2)

Como usuario autenticado sin transacciones confirmadas, quiero ver un estado vacío comprensible y
poder ir al mercado para saber que la consulta funcionó y cuál es la próxima acción disponible.

**Why this priority**: Completa la experiencia de usuarios nuevos o inactivos sin confundir un
resultado exitoso vacío con un fallo.

**Independent Test**: Puede verificarse con un usuario sin operaciones: la pantalla informa que
todavía no existen transacciones y la acción `Ir al mercado` conduce al flujo de compra existente.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado sin operaciones confirmadas, **When** consulta su historial,
   **Then** ve un estado vacío que indica que sus compras y ventas futuras aparecerán allí.
2. **Given** el estado vacío, **When** el usuario elige `Ir al mercado`, **Then** accede a la
   pantalla existente para comprar tokens.

---

### User Story 4 - Consultar el historial en distintos tamaños de pantalla (Priority: P2)

Como usuario autenticado, quiero leer toda la información de mis transacciones tanto en escritorio
como en tablet y móvil, sin desplazamiento horizontal ni pérdida de datos.

**Why this priority**: La adaptabilidad es un criterio de aceptación explícito del issue y debe
preservar la interpretación de importes y fechas en cualquier tamaño soportado.

**Independent Test**: Puede verificarse con el mismo historial en los tres rangos del wireframe:
tabla en escritorio, cuatro grupos en tablet y tarjetas en móvil, conservando los mismos datos y su
orden.

**Acceptance Scenarios**:

1. **Given** un viewport de referencia de `1920 × 913 px`, **When** el historial contiene más
   operaciones que las visibles, **Then** la navegación y la cabecera permanecen visibles, las
   filas se desplazan dentro de su región y la ventana no presenta scroll global.
2. **Given** un ancho de tablet, **When** se muestra el historial, **Then** cada operación conserva
   todos sus datos agrupados en cuatro zonas legibles y no genera desplazamiento horizontal.
3. **Given** un ancho móvil desde 360 px, **When** se muestra el historial, **Then** cada operación
   se presenta como tarjeta en una sola columna y la página usa desplazamiento vertical natural.

### Edge Cases

- Un usuario sin operaciones recibe un resultado exitoso vacío; la pantalla no lo presenta como
  error.
- Dos operaciones con la misma fecha y hora conservan el orden recibido, cuyo desempate por
  identificador descendente corresponde al historial provisto.
- Los valores cero no se sustituyen por ausencia ni se ocultan; la pantalla conserva los valores
  recibidos y aplica el formato monetario acordado.
- Un nombre de jugador largo puede ocupar hasta dos líneas sin truncar importes, fechas ni provocar
  desplazamiento horizontal.
- Una colección extensa se consulta completa y se desplaza dentro de la región prevista en
  escritorio; no se introduce paginación ni carga incremental.
- La pantalla no muestra datos parciales cuando la consulta falla y no revela información de otra
  persona ante una sesión inválida.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir que un usuario autenticado acceda a su historial personal
  desde la pestaña `Historial de transacciones` de la sección `Cartera`.
- **FR-002**: La pantalla MUST usar la ruta acordada `/portfolio/transactions` y MUST permanecer
  protegida por las mismas reglas de sesión que las demás vistas privadas.
- **FR-003**: El historial MUST incluir exclusivamente las compras y ventas confirmadas que el
  servicio personal vigente entregue para la identidad de la sesión.
- **FR-004**: Cada operación MUST mostrar tipo, identificador de transacción, nombre del jugador,
  cantidad de tokens, precio unitario histórico, importe total histórico y fecha y hora de
  ejecución.
- **FR-005**: El tipo `BUY` MUST presentarse como `COMPRA` y el tipo `SELL` MUST presentarse como
  `VENTA`, sin inferir que una venta representa ganancia o pérdida.
- **FR-006**: El nombre del jugador MUST presentarse como texto sin enlace, botón, redirección ni
  consulta adicional de datos del jugador.
- **FR-007**: Los precios e importes MUST reflejar los valores históricos recibidos, expresarse en
  créditos con dos decimales y MUST NOT recalcularse con una cotización vigente.
- **FR-008**: La pantalla MUST conservar el orden recibido y MUST mostrar cada transacción una
  sola vez.
- **FR-009**: La cabecera MUST informar la cantidad de operaciones usando el tamaño de la
  colección obtenida y MUST aplicar correctamente singular y plural.
- **FR-010**: Mientras la consulta esté pendiente, la pantalla MUST comunicar el estado de carga y
  MUST NOT presentar la ausencia de datos como historial vacío.
- **FR-011**: Si la consulta falla, la pantalla MUST mostrar un error comprensible, indicar que las
  operaciones no fueron modificadas y ofrecer una acción para reintentar.
- **FR-012**: Si el historial está vacío, la pantalla MUST comunicar que todavía no existen
  transacciones y MUST ofrecer `Ir al mercado`, que conduce al flujo de compra existente.
- **FR-013**: En escritorio, el historial MUST usar una presentación tabular con seis columnas; en
  tablet MUST reagrupar los mismos datos en cuatro zonas; en móvil MUST presentar una tarjeta por
  operación en una sola columna.
- **FR-014**: La pantalla MUST preservar todos los datos de cada operación sin desplazamiento
  horizontal en los rangos definidos por el wireframe aprobado.
- **FR-015**: En el viewport de referencia `1920 × 913 px`, la página MUST evitar el scroll global
  y MUST confinar el desplazamiento de una colección extensa a la región de filas.
- **FR-016**: El contenido propio de la página MUST usar una retícula compacta donde el padding
  exterior, la separación, los márgenes y los espacios locales sean de 8 px y ninguno supere ese
  valor, sin alterar el contrato del navbar compartido.
- **FR-017**: La aplicación MUST mantener una sola entrada de navegación para este historial en
  `Cartera` y MUST retirar el placeholder duplicado `Historial de operaciones` de `Mercado`.
- **FR-018**: Los estados de carga y vacío MUST ser anunciables como estado, los errores MUST ser
  anunciables como alerta y las acciones MUST conservar foco visible y nombre accesible.
- **FR-019**: La funcionalidad MUST reutilizar las capacidades visuales y de navegación vigentes
  cuando satisfagan el wireframe, sin duplicar el shell, la navegación ni los patrones compartidos.
- **FR-020**: La pantalla MUST NOT incorporar filtros, búsqueda, ordenamiento controlado por el
  usuario, paginación, exportación, agregaciones, contraparte, saldo, tenencias, rentabilidad,
  cotización vigente, imágenes ni información deportiva no provista por el historial personal.

### Key Entities

- **Operación del historial**: Representación de una transacción confirmada desde la perspectiva
  del usuario autenticado. Incluye identidad, tipo, jugador, cantidad, valores históricos y momento
  de ejecución.
- **Usuario autenticado**: Persona identificada por la sesión vigente, cuya identidad delimita el
  historial privado visible y no puede sustituirse desde la pantalla.
- **Jugador**: Activo negociado identificado visualmente solo por su nombre en esta funcionalidad.
- **Historial personal**: Colección completa y ordenada de operaciones propias que puede estar
  cargando, disponible, vacía o fallida.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de los escenarios con datos, cada operación propia aparece una sola vez y
  presenta los siete datos visibles obligatorios: tipo, identificador, jugador, cantidad, precio
  unitario, importe total y fecha y hora.
- **SC-002**: En el 100% de las verificaciones con sesiones de usuarios distintos, ninguna pantalla
  presenta operaciones ajenas ni ofrece un mecanismo para seleccionar otra identidad.
- **SC-003**: En el 100% de las consultas vacías, la pantalla diferencia la ausencia de operaciones
  de un error y permite acceder al mercado mediante una sola acción.
- **SC-004**: En los rangos de escritorio, tablet y móvil definidos, el 100% de los datos de cada
  operación permanece legible sin desplazamiento horizontal.
- **SC-005**: En un viewport de `1920 × 913 px`, una colección que excede el espacio visible puede
  recorrerse completamente mediante scroll interno sin producir scroll global en la ventana.
- **SC-006**: Los estados de carga, error y vacío se distinguen correctamente en el 100% de sus
  escenarios y el reintento permite iniciar una nueva consulta sin recargar toda la aplicación.
- **SC-007**: Una persona sin conocimiento previo del historial puede identificar correctamente el
  jugador, el tipo y el importe total de una transacción indicada en no más de 10 segundos, medidos
  desde que recibe la consigna hasta que responde, sobre una pantalla ya cargada con un dataset
  representativo de 20 operaciones ficticias.

### Protocolo de evaluación de SC-007

- **Participante**: persona sin conocimiento previo del historial presentado.
- **Estado inicial**: pantalla del historial completamente cargada, sin estado de carga ni error.
- **Dataset**: 20 operaciones ficticias representativas, con compras y ventas y todos los campos
  visibles definidos por FR-004.
- **Consigna**: identificar el jugador, el tipo (`COMPRA` o `VENTA`) y el importe total de una
  transacción indicada por su identificador.
- **Cronometraje**: comienza al comunicar la consigna y finaliza cuando la persona responde los
  tres datos solicitados.
- **Éxito**: respuesta completa y correcta en un tiempo menor o igual a 10 segundos.
- **Momento de ejecución**: validación posterior a la implementación; este protocolo define el
  criterio y no acredita su cumplimiento anticipado.

## Assumptions

- SPEC-020 continúa siendo la fuente disponible del historial personal y entrega exclusivamente
  operaciones confirmadas, completas y ordenadas.
- La autenticación vigente identifica al usuario y protege las rutas privadas; SPEC-025 no redefine
  inicio de sesión, expiración ni autorización backend.
- Los importes enteros recibidos representan centésimas de crédito y pueden presentarse con el
  formato monetario ya usado por Cartera.
- La colección completa sin paginación es suficiente para el volumen esperado en esta entrega.
- El wireframe aprobado en `docs/wireframes/transaction-history.md` gobierna composición, Box
  Model, presupuesto vertical, scroll y responsive de esta pantalla.
- La navegación existente al flujo de compra es el destino de `Ir al mercado`.

## Dependencies

- **SPEC-020 — Consultar historial de operaciones**: provee las compras y ventas personales que
  consume esta interfaz. La dependencia está cerrada y disponible.
- **Sistema de sesión vigente**: identifica a la persona autenticada y protege el acceso privado.
- **Navegación y patrones de Cartera vigentes**: proporcionan el contexto visual y de acceso donde
  se incorpora la pantalla.

## Out of Scope

- Crear, modificar, cancelar o eliminar transacciones.
- Implementar o modificar el servicio backend del historial.
- Mostrar contraparte, saldo posterior, tenencia resultante, comisiones, rentabilidad o
  cotizaciones vigentes.
- Mostrar fotos, equipo, liga, posición, nacionalidad u otros datos del jugador.
- Navegar desde el nombre del jugador a su detalle.
- Incorporar filtros, búsqueda, ordenamiento configurable, paginación, exportaciones, estadísticas
  o informes.
- Calcular saldos, posiciones, costo promedio o resultados financieros.
- Rediseñar el navbar, las pantallas de mercado o las demás vistas de Cartera fuera del cambio
  mínimo para incorporar esta navegación.

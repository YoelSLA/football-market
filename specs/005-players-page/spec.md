# Feature Specification: Página de catálogo de jugadores

**Feature Branch**: `dev`

**Created**: 2026-09-28

**Status**: Draft

**Input**: Página privada de jugadores con catálogo en tarjetas, paginación, filtros visuales, recursos gráficos por liga y posición e indicador lateral de estado al interactuar con cada tarjeta.

## Clarifications

### Session 2026-09-28

- Q: ¿Cómo debe revelarse el estado del jugador cuando no está disponible el hover? → A: La aplicación es solo para escritorio y el indicador se activa únicamente con hover.
- Q: ¿Cuál es la resolución mínima de escritorio en la que deben caber las cuatro columnas y todos los controles sin desplazamiento horizontal? → A: 1280×720.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar jugadores como usuario autenticado (Priority: P1)

Como usuario autenticado, quiero abrir la página de jugadores y ver el catálogo en tarjetas para reconocer rápidamente a cada jugador, su liga y su posición.

**Why this priority**: Es el propósito principal de la página y entrega valor aun sin interacción con filtros ni cambio de página.

**Independent Test**: Iniciar sesión, abrir `/players` con al menos un jugador disponible y comprobar que se muestran hasta 12 tarjetas con nombre, icono genérico, estadísticas numéricas, liga y posición, sin permitir el acceso a un visitante.

**Acceptance Scenarios**:

1. **Given** un usuario con sesión válida y jugadores disponibles, **When** abre `/players`, **Then** ve la primera página del catálogo con hasta 12 tarjetas distribuidas en cuatro columnas y tres filas en una pantalla que permita esa disposición.
2. **Given** una tarjeta de jugador visible, **When** el usuario la observa, **Then** identifica el icono genérico y nombre en la parte superior, las estadísticas en el centro y la liga y posición en la parte inferior.
3. **Given** un visitante sin sesión válida, **When** intenta abrir `/players`, **Then** no ve el catálogo y es dirigido al acceso de usuarios.

---

### User Story 2 - Navegar entre páginas del catálogo (Priority: P1)

Como usuario autenticado, quiero cambiar de página para recorrer un catálogo con más de 12 jugadores.

**Why this priority**: Sin paginación, la mayor parte del catálogo no sería accesible desde la página.

**Independent Test**: Con más de 12 jugadores y varias páginas disponibles, avanzar, retroceder y usar los accesos a la primera y última página, comprobando que el catálogo y el estado del paginador coinciden con la página seleccionada.

**Acceptance Scenarios**:

1. **Given** un catálogo con más de una página, **When** el usuario elige otra página disponible, **Then** ve los jugadores correspondientes y la nueva página queda identificada como actual.
2. **Given** una página intermedia, **When** se muestra el paginador, **Then** ofrece la página actual, hasta tres páginas anteriores, hasta tres posteriores y accesos directos a la primera y última página.
3. **Given** la primera o la última página, **When** el usuario observa el paginador, **Then** las acciones que excederían los límites no se pueden activar.
4. **Given** una página con menos de 12 jugadores, **When** se carga, **Then** solo se muestran los jugadores disponibles sin tarjetas de relleno.

---

### User Story 3 - Reconocer estado y clasificación visual (Priority: P2)

Como usuario autenticado, quiero distinguir visualmente la liga, posición y estado de cada jugador para interpretar la tarjeta con rapidez.

**Why this priority**: Mejora la lectura del catálogo, pero depende de que las tarjetas y sus datos principales ya estén disponibles.

**Independent Test**: Revisar jugadores de las cinco ligas y las cuatro posiciones contempladas, e interactuar con sus tarjetas para comprobar el color suave, los recursos gráficos y el indicador lateral de estado.

**Acceptance Scenarios**:

1. **Given** jugadores de ligas diferentes, **When** se muestran sus tarjetas, **Then** cada liga utiliza un color de fondo suave, consistente y distinguible que mantiene legible toda la información.
2. **Given** un jugador con liga y posición reconocidas, **When** se muestra su tarjeta, **Then** aparecen los recursos visuales correspondientes a ambos valores.
3. **Given** una tarjeta visible, **When** el usuario coloca el puntero sobre ella, **Then** un indicador se desplaza desde el lado izquierdo y comunica el estado disponible del jugador sin ocultar la información principal.

---

### User Story 4 - Visualizar controles de filtrado (Priority: P3)

Como usuario autenticado, quiero reconocer controles de búsqueda, liga y posición para anticipar cómo se podrá explorar el catálogo en una evolución posterior.

**Why this priority**: Los controles forman parte de la composición solicitada, aunque todavía no modifican resultados.

**Independent Test**: Abrir `/players` y comprobar que, sobre las tarjetas, aparecen un campo de búsqueda y dos selectores identificados para liga y posición, y que interactuar con ellos no altera el catálogo.

**Acceptance Scenarios**:

1. **Given** la página de jugadores cargada, **When** el usuario observa el área superior, **Then** encuentra un control de búsqueda, un filtro por liga y un filtro por posición claramente identificados.
2. **Given** cualquiera de los tres controles, **When** el usuario escribe o selecciona una opción, **Then** puede interactuar visualmente con el control sin que cambien los jugadores ni la paginación mostrados.

### Edge Cases

- Si el catálogo no contiene jugadores, se muestra un estado vacío comprensible y no una cuadrícula o paginador engañosos.
- Si no se puede obtener el catálogo, se muestra un mensaje de error y una acción para reintentar sin presentar datos incompletos como definitivos.
- Si la sesión deja de ser válida al consultar otra página, se retira el acceso al catálogo y se dirige al usuario al acceso de usuarios.
- Si la página solicitada deja de existir porque cambia el total del catálogo, se presenta la última página válida disponible.
- Si una liga o posición no dispone de un recurso visual reconocido, se conserva el texto del dato y se utiliza una presentación neutra que mantiene la tarjeta legible.
- Los nombres largos deben permanecer legibles sin invadir las estadísticas ni los datos inferiores de la tarjeta.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La aplicación debe ofrecer `/players` como una página privada accesible únicamente con una sesión autenticada válida.
- **FR-002**: La página debe obtener el catálogo paginado existente y solicitar 12 jugadores por página.
- **FR-003**: La página debe mostrar cada jugador en una tarjeta inspirada en `referencias-imagenes/Card Player.png`, sin exigir una reproducción literal de la referencia.
- **FR-004**: Cada tarjeta debe mostrar `Player Generic-Icon.png` y el nombre del jugador en la parte superior; tres estadísticas numéricas comunes de muestra, etiquetadas como partidos, goles y asistencias, en el centro; y la liga y posición en la parte inferior.
- **FR-005**: Mientras no existan estadísticas individuales, todas las tarjetas deben usar los mismos valores numéricos de muestra y distinguirlos visualmente de los datos reales del jugador.
- **FR-006**: La página debe estar disponible exclusivamente en equipos de escritorio con una resolución mínima de 1280×720; desde esa resolución, el catálogo debe formar una cuadrícula de cuatro columnas y hasta tres filas por página sin desplazamiento horizontal.
- **FR-007**: Cada una de las cinco ligas del catálogo debe tener un color de fondo suave, consistente y diferente para sus tarjetas, con contraste suficiente para leer toda la información.
- **FR-008**: La liga debe representarse con el recurso gráfico correspondiente de `referencias-imagenes/`, además de conservar una identificación textual accesible.
- **FR-009**: Las posiciones `Goalkeeper`, `Defence`, `Midfield` y `Offence` deben representarse con sus recursos gráficos correspondientes de `referencias-imagenes/`, además de conservar una identificación textual accesible.
- **FR-010**: Al pasar el puntero sobre una tarjeta, y únicamente mediante esta interacción, debe aparecer desde su lado izquierdo un indicador que comunique el estado disponible del jugador; la transición no debe desplazar el resto del catálogo ni ocultar datos esenciales.
- **FR-011**: El estado comunicado por las tarjetas debe corresponder a la información disponible en el catálogo consumido. Dado que el catálogo vigente solo expone jugadores activos, todas las tarjetas de este alcance deben indicar `Activo`; mostrar jugadores inactivos requiere una ampliación posterior del catálogo.
- **FR-012**: Sobre la cuadrícula deben mostrarse tres controles identificables: búsqueda por texto, selección de liga y selección de posición.
- **FR-013**: Los controles de búsqueda, liga y posición deben ser únicamente visuales en esta feature: su interacción no debe filtrar, ordenar, volver a consultar ni alterar el catálogo o la página actual.
- **FR-014**: Debajo de las tarjetas debe existir un paginador cuando haya más de una página, con selección directa de la página actual, hasta tres páginas anteriores, hasta tres posteriores y controles para ir a la primera y a la última página.
- **FR-015**: El paginador debe identificar inequívocamente la página actual, impedir destinos fuera de rango y actualizar las tarjetas al seleccionar una página válida.
- **FR-016**: Durante la carga inicial o un cambio de página, la página debe informar que la consulta está en curso y evitar que resultados de solicitudes anteriores sustituyan a los de la selección más reciente.
- **FR-017**: Ante un catálogo vacío, la página debe mostrar un estado vacío; ante un fallo recuperable, debe mostrar un mensaje comprensible y una acción de reintento.
- **FR-018**: Si la consulta del catálogo informa que la sesión ya no es válida, la aplicación debe invalidar el acceso privado y dirigir al usuario al acceso de usuarios.
- **FR-019**: La información comunicada por colores o imágenes debe contar con una alternativa textual.
- **FR-020**: La feature debe limitarse a visualizar y paginar el catálogo. El filtrado real, la búsqueda real, la edición de jugadores, la sincronización del catálogo y el detalle individual quedan fuera de alcance.

### Key Entities

- **Jugador**: Elemento del catálogo con identificador, nombre, equipo, liga y posición. En el catálogo vigente, todo jugador visible se considera activo.
- **Página de jugadores**: Grupo de hasta 12 jugadores acompañado por su número actual, cantidad total de elementos y cantidad total de páginas.
- **Tarjeta de jugador**: Representación visual de un jugador que reúne identidad, estadísticas de muestra, liga, posición y estado.
- **Estado de navegación**: Página seleccionada y destinos válidos que permiten recorrer el catálogo sin exceder sus límites.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de los intentos sin una sesión válida, el catálogo de `/players` permanece oculto y el visitante llega al acceso de usuarios.
- **SC-002**: En una revisión con datos de las cinco ligas y cuatro posiciones contempladas, el 100 % de las tarjetas muestra nombre, icono genérico, tres estadísticas numéricas, liga, posición y estado identificables.
- **SC-003**: En equipos de escritorio con una resolución de 1280×720 o superior, cada página muestra como máximo 12 tarjetas en cuatro columnas y tres filas, y el 100 % del contenido y los controles permanece utilizable sin desplazamiento horizontal.
- **SC-004**: En un catálogo de al menos 8 páginas, el usuario puede llegar a la primera, última y cualquier página ofrecida por la ventana de tres anteriores y tres posteriores en una sola acción del paginador.
- **SC-005**: En el 100 % de los cambios de página exitosos, la página identificada como actual y los jugadores visibles corresponden a la selección más reciente.
- **SC-006**: Los tres controles de filtro son visibles e identificables en la página, y en el 100 % de sus interacciones el conjunto de resultados permanece inalterado durante esta feature.
- **SC-007**: En una revisión de todas las ligas, posiciones y estados contemplados, el 100 % de la información sigue siendo comprensible sin depender únicamente del color o de una imagen.
- **SC-008**: En los escenarios de catálogo vacío y fallo de consulta, el usuario recibe feedback visible en menos de un segundo desde que se conoce el resultado, y puede reintentar después de un fallo recuperable.

## Assumptions

- El catálogo paginado y el sistema de autenticación existentes se reutilizan sin incorporar un mecanismo de acceso nuevo.
- La aplicación está destinada exclusivamente a equipos de escritorio; dispositivos móviles y pantallas táctiles quedan fuera del alcance.
- El catálogo vigente contiene y devuelve únicamente jugadores activos; por ello el indicador solicitado comunica `Activo` para todos los jugadores visibles. La visualización de inactivos depende de un cambio futuro fuera de este alcance.
- Las cinco ligas son Premier League, Bundesliga, Primera División, Serie A y Ligue 1, y sus nombres pueden relacionarse con los recursos disponibles en `referencias-imagenes/`.
- Los valores iniciales de partidos, goles y asistencias son datos de muestra comunes a todos los jugadores y no representan estadísticas reales.
- La ventana del paginador se interpreta como hasta tres páginas anteriores y tres posteriores a la actual, además de los accesos a los extremos.

## Out of Scope

- Aplicar búsqueda o filtros al catálogo.
- Mostrar estadísticas reales o diferentes para cada jugador.
- Exponer o incorporar jugadores inactivos al catálogo existente.
- Editar, comprar, vender, sincronizar o abrir un detalle de jugador.
- Cambiar el contrato o los datos persistidos del catálogo.
- Adaptar la página a dispositivos móviles o pantallas táctiles.

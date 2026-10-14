# Feature Specification: Página de catálogo de jugadores

**Feature Branch**: `005-players-page`

**Created**: 2026-09-28

**Revised**: 2026-10-13 (decisión de producto que conserva el rediseño manual del frontend)

**Status**: Draft

**Input**: Página privada de jugadores con catálogo en tarjetas, paginación, filtros visuales, recursos gráficos por liga, etiqueta textual de posición y efecto de elevación al interactuar con cada tarjeta. El indicador lateral de estado y el recurso gráfico de posición de la redacción original de 2026-09-28 fueron sustituidos por decisión de producto el 2026-10-13; ambos antecedentes se conservan en la sección de requisitos derogados.

## Clarifications

### Session 2026-09-28

- Q: ¿Cómo debe revelarse el estado del jugador cuando no está disponible el hover? → A: La aplicación es solo para escritorio y el indicador se activa únicamente con hover. **Antecedente derogado el 2026-10-13**: el estado se comunica siempre de forma visible por FR-011 y el hover solo produce un realce de interacción.
- Q: ¿Cuál es la resolución mínima de escritorio en la que deben caber las cuatro columnas y todos los controles sin desplazamiento horizontal? → A: 1280×720.

### Session 2026-10-13

Decisiones de producto confirmadas por el responsable del proyecto, tras una auditoría de trazabilidad entre el código y esta SPEC. Corresponden al rediseño manual del frontend conservado por decisión expresa y **modifican** los requisitos que este documento fijaba. Cada sustitución se indica con su reemplazo.

- Q: ¿Se conservan los cambios manuales del frontend? → A: Sí, en su totalidad, sin revertir ni reemplazar: navbar horizontal global, catálogo responsive, nuevo diseño de la tarjeta, paginación con ventana ±1, nuevos layouts y componentes compartidos, arquitectura de estilos y tokens, escudos de equipo, banderas de nacionalidad, estadísticas deterministas por jugador y diseño actual del detalle individual.
- Q: ¿El catálogo pasa a ser responsive? → A: Sí. **Sustituye** la exclusividad de escritorio de la resolución mínima de 1280×720. Este cambio **corrige** una divergencia preexistente con `tp/enunciado.md` §11, que exige que el frontend sea una aplicación web responsiva.
- Q: ¿Qué hace la tarjeta con las estadísticas de muestra? → A: Muestra valores deterministas derivados del identificador del jugador, etiquetados como demostración. **Sustituye** los valores fijos comunes `24`/`8`/`5`. Requisito ya anticipado por SPEC-009 FR-011.
- Q: ¿Qué ventana usa el paginador? → A: La ventana actual, con una página anterior y una posterior además de los accesos a la primera y a la última. **Sustituye** la ventana de tres anteriores y tres posteriores.
- Q: ¿Dónde se aplica la navegación? → A: En todas las rutas privadas mediante el layout de aplicación. **Sustituye** la composición de la página sin navegación transversal.
- Q: ¿Puede la tarjeta incorporar escudo de equipo y bandera de nacionalidad? → A: Sí, como recursos gráficos decorativos con alternativa textual y marcador de posición accesible. No amplían el contrato del catálogo.

### Session 2026-10-13 (cierre de sincronización)

Decisiones expresas del responsable del proyecto que resuelven las dos divergencias que quedaban pendientes tras la sesión anterior. Se conserva el frontend actual, incluido el nuevo diseño de `PlayerCard`, y **no se reintroducen los elementos visuales anteriores**.

- Q: ¿Qué representa la posición del jugador? → A: Una **etiqueta textual visible, clara y accesible**. Se **sustituye** el recurso gráfico de posición. Requisito: FR-009.
- Q: ¿Qué comunica el hover sobre una tarjeta? → A: Un **efecto de elevación y refuerzo de brillo**. Se **sustituye** el indicador lateral que aparecía desde la izquierda. Requisito: FR-010.

Consecuencia de coherencia: como el catálogo es utilizable en pantallas táctiles, la información esencial de la tarjeta no puede depender del hover. FR-010 pasa a ser un realce de interacción y no un mecanismo de comunicación de estado; el estado `Activo` sigue siendo visible de forma permanente por FR-011.

**Requisitos que no se sustituyen**: la autenticación de la página, los controles de filtrado únicamente visuales, la ausencia de búsqueda o filtrado reales, la identificación textual de liga y posición, el estado `Activo`, los estados de carga, vacío y error con reintento, y la coherencia entre página actual y resultados.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar jugadores como usuario autenticado (Priority: P1)

Como usuario autenticado, quiero abrir la página de jugadores y ver el catálogo en tarjetas para reconocer rápidamente a cada jugador, su liga y su posición.

**Why this priority**: Es el propósito principal de la página y entrega valor aun sin interacción con filtros ni cambio de página.

**Independent Test**: Iniciar sesión, abrir `/players` con al menos un jugador disponible y comprobar que se muestran hasta 12 tarjetas con retrato o icono genérico, nombre, equipo, estadísticas de demostración, liga y posición, sin permitir el acceso a un visitante.

**Acceptance Scenarios**:

1. **Given** un usuario con sesión válida y jugadores disponibles, **When** abre `/players`, **Then** ve la primera página del catálogo con hasta 12 tarjetas; en un escritorio de 1280×720 o superior se distribuyen en cuatro columnas y hasta tres filas sin desplazamiento horizontal.
2. **Given** una tarjeta de jugador visible, **When** el usuario la observa, **Then** identifica el retrato o el icono genérico y el nombre del jugador, el equipo real, las estadísticas etiquetadas como demostración y la liga y la posición, con recursos gráficos y alternativa textual.
3. **Given** un visitante sin sesión válida, **When** intenta abrir `/players`, **Then** no ve el catálogo y es dirigido al acceso de usuarios.
4. **Given** una ruta privada autenticada, **When** el usuario navega entre Inicio, Jugadores y el detalle de un jugador, **Then** encuentra la misma navbar horizontal en las tres, con la sección actual identificada.

---

### User Story 2 - Navegar entre páginas del catálogo (Priority: P1)

Como usuario autenticado, quiero cambiar de página para recorrer un catálogo con más de 12 jugadores.

**Why this priority**: Sin paginación, la mayor parte del catálogo no sería accesible desde la página.

**Independent Test**: Con más de 12 jugadores y varias páginas disponibles, avanzar, retroceder y usar los accesos a la primera y última página, comprobando que el catálogo y el estado del paginador coinciden con la página seleccionada.

**Acceptance Scenarios**:

1. **Given** un catálogo con más de una página, **When** el usuario elige otra página disponible, **Then** ve los jugadores correspondientes y la nueva página queda identificada como actual.
2. **Given** una página intermedia, **When** se muestra el paginador, **Then** ofrece la página actual, la anterior y la posterior, y accesos directos a la primera y a la última página.
3. **Given** la primera o la última página, **When** el usuario observa el paginador, **Then** las acciones que excederían los límites no se pueden activar.
4. **Given** una página con menos de 12 jugadores, **When** se carga, **Then** solo se muestran los jugadores disponibles sin tarjetas de relleno.
5. **Given** un catálogo extenso, **When** el total de páginas supera el umbral de la ventana, **Then** las páginas omitidas se comunican con una elipsis y siguen siendo alcanzables mediante los accesos a los extremos.

---

### User Story 3 - Reconocer estado y clasificación visual (Priority: P2)

Como usuario autenticado, quiero distinguir visualmente la liga, posición y estado de cada jugador para interpretar la tarjeta con rapidez.

**Why this priority**: Mejora la lectura del catálogo, pero depende de que las tarjetas y sus datos principales ya estén disponibles.

**Independent Test**: Revisar jugadores de las cinco ligas y las cuatro posiciones contempladas, e interactuar con sus tarjetas para comprobar el color suave, la etiqueta textual de posición y el efecto de elevación y refuerzo de brillo al pasar el cursor.

**Acceptance Scenarios**:

1. **Given** jugadores de ligas diferentes, **When** se muestran sus tarjetas, **Then** cada liga utiliza un color de fondo suave, consistente y distinguible que mantiene legible toda la información.
2. **Given** un jugador con liga y posición reconocidas, **When** se muestra su tarjeta, **Then** la liga aparece con su recurso visual y su texto, y la posición aparece con una etiqueta textual visible, clara y accesible.
3. **Given** una tarjeta visible, **When** el usuario coloca el puntero sobre ella, **Then** la tarjeta se eleva y su brillo se refuerza, sin desplazar el resto del catálogo ni ocultar datos esenciales.
4. **Given** un dispositivo táctil o el uso exclusivo del teclado, **When** el usuario recorre las tarjetas, **Then** toda la información esencial, incluida la liga, la posición y el estado, sigue siendo visible y comprensible sin hover.

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
- Si una liga o posición no dispone de un recurso visual reconocido, se conserva el texto del dato y se utiliza una presentación neutra que mantiene la tarjeta legible. Desde el 2026-10-13 la posición no depende de ningún recurso visual: su etiqueta textual es el mecanismo identificado.
- Los nombres largos deben permanecer legibles sin invadir las estadísticas ni los datos inferiores de la tarjeta.
- Un dispositivo sin hover —táctil, teclado o tecnología de asistencia— debe mostrar igualmente toda la información esencial de la tarjeta; el efecto de elevación es un realce y nunca el único canal de comunicación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La aplicación debe ofrecer `/players` como una página privada accesible únicamente con una sesión autenticada válida.
- **FR-002**: La página debe obtener el catálogo paginado existente y solicitar 12 jugadores por página.
- **FR-003**: La página debe mostrar cada jugador en una tarjeta inspirada en `referencias-imagenes/Card Player.png`, sin exigir una reproducción literal de la referencia.
- **FR-004**: Cada tarjeta debe mostrar el retrato del jugador o, en su defecto, `Player Generic-Icon.png`, junto con su nombre; el equipo real del jugador y tres estadísticas numéricas etiquetadas como partidos, goles y asistencias; y la liga y posición, siempre con su identificación textual.
- **FR-005**: Cada tarjeta debe mostrar estadísticas de demostración **derivadas de forma determinista del identificador del jugador**, etiquetadas como «Datos de demostración», de modo que dos jugadores distintos no compartan necesariamente los mismos valores. Este requisito **sustituye** los valores fijos comunes `24` partidos, `8` goles y `5` asistencias, que quedan derogados por decisión de producto del 2026-10-13 y por el requisito compartido de SPEC-009 FR-011. El equipo y cada estadística deben mostrar su etiqueta, y sus textos deben mantener contraste suficiente respecto del color de fondo de la tarjeta.
- **FR-006**: La página debe estar disponible en escritorio, tablet y móvil. Desde 1280×720 el catálogo forma una cuadrícula de cuatro columnas y hasta tres filas por página; por debajo de ese ancho la cuadrícula se reorganiza progresivamente hasta una sola columna, en 768, 360 y cualquier ancho intermedio, sin desplazamiento horizontal. Este requisito **sustituye** la exclusividad de escritorio y la resolución mínima única, y alinea la página con `tp/enunciado.md` §11.
- **FR-007**: Cada una de las cinco ligas del catálogo debe tener un color de fondo suave, consistente y diferente para sus tarjetas, con contraste suficiente para leer toda la información.
- **FR-008**: La liga debe representarse con el recurso gráfico correspondiente de `referencias-imagenes/`, además de conservar una identificación textual accesible.
- **FR-009**: Las posiciones `Goalkeeper`, `Defence`, `Midfield` y `Offence` deben identificarse mediante una **etiqueta textual visible, clara y accesible**, presentada como distintivo de la tarjeta y legible sin depender del color ni del hover. Este requisito **sustituye**, por decisión de producto del 2026-10-13, la exigencia de representar la posición con su recurso gráfico de `referencias-imagenes/`; el texto original del dato se conserva siempre y la imagen no es necesaria para comprenderlo.
- **FR-010**: Al pasar el puntero sobre una tarjeta debe producirse un **efecto de elevación y refuerzo de brillo** que comunique que la tarjeta es interactiva. Este requisito **sustituye**, por decisión de producto del 2026-10-13, el indicador lateral que aparecía desde el borde izquierdo. El efecto no debe desplazar el resto del catálogo ni ocultar datos esenciales, y la información esencial de la tarjeta no puede depender del hover, porque el catálogo es utilizable también en pantallas táctiles.
- **FR-011**: El estado comunicado por las tarjetas debe corresponder a la información disponible en el catálogo consumido. Dado que el catálogo vigente solo expone jugadores activos, todas las tarjetas de este alcance deben indicar `Activo`; mostrar jugadores inactivos requiere una ampliación posterior del catálogo.
- **FR-012**: Sobre la cuadrícula deben mostrarse tres controles identificables: búsqueda por texto, selección de liga y selección de posición.
- **FR-013**: Los controles de búsqueda, liga y posición deben ser únicamente visuales en esta feature: su interacción no debe filtrar, ordenar, volver a consultar ni alterar el catálogo o la página actual.
- **FR-014**: Debajo de las tarjetas debe existir un paginador cuando haya más de una página, con selección directa de la página actual, la página anterior, la página posterior y controles para ir a la primera y a la última página. Este requisito **sustituye** la ventana de tres páginas anteriores y tres posteriores por la ventana vigente, que muestra además una elipsis cuando existen páginas omitidas.
- **FR-015**: El paginador debe identificar inequívocamente la página actual, impedir destinos fuera de rango y actualizar las tarjetas al seleccionar una página válida.
- **FR-016**: Durante la carga inicial o un cambio de página, la página debe informar que la consulta está en curso y evitar que resultados de solicitudes anteriores sustituyan a los de la selección más reciente.
- **FR-017**: Ante un catálogo vacío, la página debe mostrar un estado vacío; ante un fallo recuperable, debe mostrar un mensaje comprensible y una acción de reintento.
- **FR-018**: Si la consulta del catálogo informa que la sesión ya no es válida, la aplicación debe invalidar el acceso privado y dirigir al usuario al acceso de usuarios.
- **FR-019**: La información comunicada por colores o imágenes debe contar con una alternativa textual.
- **FR-020**: La feature debe limitarse a visualizar y paginar el catálogo. El filtrado real, la búsqueda real, la edición de jugadores y la sincronización del catálogo quedan fuera de alcance. El detalle individual, que en la redacción original también quedaba excluido, se materializó en SPEC-009 y desde 2026-10-13 el catálogo enlaza con `/players/:id` mediante la tarjeta.
- **FR-021**: La página se compone sobre el layout de aplicación y el layout paginado compartidos, de modo que la navegación global y la paginación sean capacidades transversales reutilizables y no elementos propios del catálogo. Este requisito se incorpora por decisión de producto del 2026-10-13.
- **FR-022**: La tarjeta puede incorporar recursos gráficos decorativos para el equipo y la nacionalidad, resueltos desde un registro local de escudos y desde un servicio externo de banderas, con marcador de posición y alternativa textual cuando no exista coincidencia o falle la carga. Estos recursos no forman parte del contrato del catálogo ni modifican los datos del jugador. Este requisito se incorpora por decisión de producto del 2026-10-13.

### Key Entities

- **Jugador**: Elemento del catálogo con identificador, nombre, equipo, liga y posición. En el catálogo vigente, todo jugador visible se considera activo.
- **Página de jugadores**: Grupo de hasta 12 jugadores acompañado por su número actual, cantidad total de elementos y cantidad total de páginas.
- **Tarjeta de jugador**: Representación visual de un jugador que reúne identidad, un bloque central con equipo y estadísticas de muestra, liga, posición y estado.
- **Estado de navegación**: Página seleccionada y destinos válidos que permiten recorrer el catálogo sin exceder sus límites.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de los intentos sin una sesión válida, el catálogo de `/players` permanece oculto y el visitante llega al acceso de usuarios.
- **SC-002**: En una revisión con datos de las cinco ligas y cuatro posiciones contempladas, el 100 % de las tarjetas muestra nombre, retrato o icono genérico, equipo, estadísticas etiquetadas como demostración, liga, posición y estado identificables, con textos legibles respecto del fondo.
- **SC-003**: En equipos de escritorio con una resolución de 1280×720 o superior, cada página muestra como máximo 12 tarjetas en cuatro columnas y tres filas; a 768 y 360 píxeles de ancho el 100 % del contenido y los controles permanece utilizable sin desplazamiento horizontal.
- **SC-004**: En un catálogo de al menos 8 páginas, el usuario puede llegar en una sola acción del paginador a la primera, a la última, a la anterior y a la posterior de la actual, y a cualquier página ofrecida por la ventana vigente.
- **SC-005**: En el 100 % de los cambios de página exitosos, la página identificada como actual y los jugadores visibles corresponden a la selección más reciente.
- **SC-006**: Los tres controles de filtro son visibles e identificables en la página, y en el 100 % de sus interacciones el conjunto de resultados permanece inalterado durante esta feature.
- **SC-007**: En una revisión de todas las ligas, posiciones y estados contemplados, el 100 % de la información sigue siendo comprensible sin depender únicamente del color, de una imagen o del hover.
- **SC-008**: En los escenarios de catálogo vacío y fallo de consulta, el usuario recibe feedback visible en menos de un segundo desde que se conoce el resultado, y puede reintentar después de un fallo recuperable.
- **SC-009**: En las tres rutas privadas de la aplicación, el 100 % presenta la misma navbar horizontal con marca, enlaces funcionales, sección activa y sector de usuario, sin divergencias entre pantallas.
- **SC-010**: Para al menos dos jugadores del catálogo, las tarjetas muestran valores distintos derivados de su identificador y etiquetados como demostración, y no se confunden con datos reales.
- **SC-011**: En una revisión con teclado y en pantalla táctil, el 100 % de las tarjetas permite identificar liga, posición y estado sin ejecutar hover, y el efecto de hover no desplaza la cuadrícula ni oculta datos esenciales.

## Assumptions

- El catálogo paginado y el sistema de autenticación existentes se reutilizan sin incorporar un mecanismo de acceso nuevo.
- La aplicación es responsiva y debe ser utilizable en escritorio, tablet y móvil. Esta expectativa sustituye a la del supuesto anterior, que restringía la aplicación al escritorio.
- El catálogo vigente contiene y devuelve únicamente jugadores activos; por ello todas las tarjetas comunican `Activo` de forma permanente. La visualización de inactivos depende de un cambio futuro fuera de este alcance.
- Las cinco ligas son Premier League, Bundesliga, Primera División, Serie A y Ligue 1, y sus nombres pueden relacionarse con los recursos disponibles en `referencias-imagenes/`.
- Las estadísticas de las tarjetas son datos de demostración y no representan estadísticas reales; se derivan de forma determinista del identificador del jugador y se comparten con el detalle; el equipo, la liga y la posición sí proceden del catálogo.
- La ventana del paginador muestra la página actual, la anterior y la posterior, además de los accesos a los extremos y de una elipsis cuando existen páginas omitidas.
- El escudo del equipo se resuelve desde un registro local importado en tiempo de build y la bandera de nacionalidad desde un servicio externo; ambos son decorativos, tienen marcador de posición accesible y no forman parte del contrato del catálogo.

## Out of Scope

- Aplicar búsqueda o filtros reales al catálogo.
- Mostrar estadísticas deportivas reales. Las estadísticas por jugador siguen siendo de demostración.
- Exponer o incorporar jugadores inactivos al catálogo existente.
- Editar, comprar, vender o sincronizar jugadores.
- Cambiar el contrato o los datos persistidos del catálogo.
- Implementar el detalle individual del jugador, especifico en SPEC-009.

### Requisitos derogados

Los siguientes requisitos de la redacción original de 2026-09-28 quedaron derogados por decisión de producto del 2026-10-13 y están reemplazados por los indicados en su requisito equivalente actual. Se conservan aquí como antecedente:

| Requisito derogado | Sustituto |
| --- | --- |
| FR-005 con valores fijos `24`/`8`/`5` | FR-005 con estadísticas deterministas por jugador y etiqueta de demostración |
| FR-006 con escritorio exclusivo y 1280×720 | FR-006 responsive con referencia de cuatro columnas a 1280×720 |
| FR-014 con ventana de tres anteriores y tres posteriores | FR-014 con ventana de una anterior y una posterior más extremos |
| FR-020 con el detalle individual fuera de alcance | FR-020 actualizado y SPEC-009 |
| «Adaptar la página a dispositivos móviles o pantallas táctiles» (Out of Scope) | FR-006 responsive |

La tabla anterior recoge las sustituciones derivadas del rediseño del catálogo. Las dos sustituciones adicionales se registran a continuación, con la redacción exacta del requisito sustituido conservada como antecedente.

| Requisito derogado (redacción de 2026-09-28) | Sustituto (decisión de 2026-10-13) | Justificación |
| --- | --- | --- |
| **FR-009** — «Las posiciones `Goalkeeper`, `Defence`, `Midfield` y `Offence` deben representarse con sus recursos gráficos correspondientes de `referencias-imagenes/`, además de conservar una identificación textual accesible.» | **FR-009** — «Las posiciones deben identificarse mediante una etiqueta textual visible, clara y accesible, presentada como distintivo de la tarjeta y legible sin depender del color ni del hover.» | El distintivo textual cumple mejor la identificación de la posición en una tarjeta que además muestra liga, escudo, bandera y estadísticas: evita un cuarto recurso gráfico en un espacio reducido y mantiene la legibilidad en pantallas estrechas y con daltonismo. La identificación textual, que el requisito original ya exigía, pasa a ser el mecanismo suficiente y único. No se elimina información: el valor original de la posición se conserva siempre. |
| **FR-010** — «Al pasar el puntero sobre una tarjeta, y únicamente mediante esta interacción, debe aparecer desde su lado izquierdo un indicador que comunique el estado disponible del jugador; la transición no debe desplazar el resto del catálogo ni ocultar datos esenciales.» | **FR-010** — «Al pasar el puntero sobre una tarjeta debe producirse un efecto de elevación y refuerzo de brillo que comunique que la tarjeta es interactiva; el efecto no debe desplazar el resto del catálogo ni ocultar datos esenciales y la información esencial no puede depender del hover.» | Un indicador condicionado exclusivamente al hover es incompatible con el catálogo responsive aprobado el mismo día, porque en pantallas táctiles no se ejecuta y el estado quedaría sin comunicar. El estado `Activo` permanece siempre visible por FR-011, de modo que el indicador era además redundante. El efecto de elevación comunica interactividad sin ocultar información y respeta `prefers-reduced-motion`. |

Ninguno de los dos requisitos derogados se elimina por descuido: ambos conservan su redacción original en esta tabla, conservan su objetivo comunicativo y su sustituto cubre la misma necesidad observable con un mecanismo compatible con el resto del alcance.

### Requisitos pendientes de verificación

Los siguientes requisitos **no han sido derogados** y su cumplimiento en el código actual no ha podido confirmarse documentalmente. Requieren verificación funcional o visual por el usuario antes de CODE REVIEW; no deben marcarse como cumplidos sin evidencia.

| Requisito | Estado observado en el código | Verificación pendiente |
| --- | --- | --- |
| FR-009 (etiqueta textual de posición) | Sustituido formalmente el 2026-10-13. La tarjeta muestra la posición como distintivo textual. | Verificar visualmente que la etiqueta es legible sobre los cinco fondos de liga y que el texto original se conserva. |
| FR-010 (elevación y refuerzo de brillo) | Sustituido formalmente el 2026-10-13. El hover produce elevación y refuerzo de brillo. | Verificar visualmente que no hay desplazamiento del catálogo ni pérdida de foco visible con teclado. |
| FR-005 / SC-010 (estadísticas deterministas) | Implementado mediante el generador compartido con el detalle. | Verificación visual de los valores y de la etiqueta de demostración en el navegador. |
| FR-006 / SC-003 (responsive) | La cuadrícula se reorganiza por breakpoints. | Verificación visual a 1280, 768 y 360 píxeles sin desplazamiento horizontal. |
| FR-022 (recursos gráficos decorativos) | Escudo local y bandera externa con marcador de posición. | Verificación visual del fallo controlado de bandera y de la ausencia de escudo. |
| FR-008 (recurso gráfico de liga) | La tarjeta mantiene imagen de liga y texto; la liga **conserva** su recurso gráfico. | Verificación visual del recurso y del texto en las cinco ligas. |

Los requisitos FR-009 y FR-010 ya no están pendientes de decisión: fueron sustituidos expresamente el 2026-10-13. Lo que queda pendiente es únicamente su verificación visual, no su definición.

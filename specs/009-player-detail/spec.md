# Feature Specification: Detalle individual, estadísticas reales y cotizaciones del jugador

**Feature**: `009-player-detail`
**Status**: Approved / Vigente
**Ámbito**: Frontend (`/players/:id`) y Backend (`/api/players/{id}`, `/api/players/{id}/statistics`, `/api/players/{id}/quotes`)
**Input normativo**: Pantalla privada de detalle del jugador con identidad visual completa, cotización oficial en cabecera y pestaña dedicada, resumen deportivo posicional con 4 KPI y 2 paneles temáticos específicos, y catálogo de exploración de 60 métricas reales extraídas de WhoScored.

**Alineación de elegibilidad con SPEC-002 / SPEC-005 — 2026-11-11**: identidad y estadísticas aplican la misma visibilidad y el criterio de métricas de [SPEC-002](../002-player-catalog/spec.md): al menos una métrica informada en alguna fila de la fecha de extracción más reciente. Un jugador no elegible recibe 404 en ambas consultas. Para un jugador elegible sin snapshot confirmado en la temporada o contexto consultados, estadísticas conserva 200 `NO_DATA` y 60 ausencias.

**Alineación autorizada de cotizaciones — 2026-11-12**: se conserva el diseño del frontend integrado desde `dev`, incluidas la cotización oficial en cabecera y la pestaña dedicada. Ambas se alimentan exclusivamente del historial oficial persistido consultado por endpoint; no se presentan datos de demostración como cotización oficial. Esta actualización alinea el comportamiento vigente y no implica que las verificaciones visuales humanas se hayan realizado.

## Trazabilidad del rediseño

- **Identidad**: SPEC-009 reutilizada para la experiencia completa de detalle del jugador.
- **Fuentes normativas**: [wireframe canónico del detalle del jugador](../../docs/wireframes/player-detail.md), [SPEC-012, §Catálogo de métricas](../012-player-statistics-scraping/spec.md#catálogo-de-métricas) (60 claves canónicas), [SPEC-016 (Cotizaciones oficiales)](../016-ejecutar-recalculo-manual-de-cotizaciones/spec.md) y [glosario canónico de presentación](contracts/metric-glossary.md).
- **Contratos backend**:
  - Identidad básica: `GET /api/players/{id}`
  - Estadísticas deportivas: `GET /api/players/{id}/statistics`
  - Cotizaciones del token: `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current`

## Decisiones funcionales y de diseño

- **Estructura de pestañas**: Tres pestañas ordenadas: «Cotizaciones» (activa por defecto), «Resumen deportivo» y «Todas las estadísticas».
- **Cabecera compacta (125 px en desktop)**: Tres zonas horizontales simétricas: identidad y escudo a la izquierda (con retrato de 90×90 px), tres micro-tarjetas de métricas titulares en el centro según la posición deportiva, e indicador financiero de cotización oficial a la derecha con badges semánticos.
- **Resumen deportivo**: Estructurado exclusivamente en 4 tarjetas KPI principales superiores desduplicadas (sin repetir métricas de cabecera ni duplicar unidades) y exactamente 2 paneles temáticos específicos por posición con filas de altura acotada. Se eliminan las barras de progreso sin escala de referencia válida y el bloque de estadísticas destacadas.
- **Catálogo de 60 métricas**: Exploración completa organizada en 8 categorías temáticas con buscador en tiempo real insensible a mayúsculas/acentos y filtros de categoría con conteo. Las métricas se ordenan deportivamente por relevancia posicional (Principal → Secundaria → Poco relevante → Sin datos).
- **Semántica de datos**: Un cero real es un dato válido persistido y nunca se muestra como ausencia. Una métrica ausente en el snapshot muestra «Sin datos» y nunca se inventa ni se rellena con ceros.
- **Viewport y responsividad**: En desktop (1920×913 y 1920×1080), la zona superior permanece fija (209 px: breadcrumb 32 px, cabecera 125 px, margen 12 px, pestañas 40 px) y el contenido se ajusta de forma flexible con cero scroll en ventana; el scroll vertical queda confinado exclusivamente al contenedor interno del catálogo de estadísticas. En tablet y móvil se utiliza flujo documental natural sin desbordamiento horizontal.
- **Paleta visual**: Colores sólidos basados en tokens semánticos aprobados (`$surface-page-deep`, `$surface-card-dark`, `$surface-panel-to`, `$surface-panel-from`, `$surface-accent-deep`, `$surface-accent-dark`, `$accent-sports-green`, `$border-panel`), sin gradientes, sin efectos de iluminación y sin sombras decorativas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar identidad y navegar (Priority: P1)

Como usuario autenticado, quiero abrir el detalle de un jugador desde el catálogo o mediante URL directa y reconocer de inmediato su identidad en una cabecera compacta de tres zonas con sus tres métricas titulares y cotización oficial, pudiendo regresar al catálogo o navegar entre pestañas.

**Why this priority**: Garantiza la correcta atribución de la identidad y estadísticas al jugador correspondiente y permite el acceso centralizado a su ficha.
**Independent Test**: Abrir desde el catálogo, recargar la página y acceder por URL directa; comprobar la identidad, el retorno a `/players` y el comportamiento ante identificadores inválidos o inexistentes.
**Acceptance Scenarios**:

1. **Given** un jugador visible y una sesión activa, **When** abro su detalle, **Then** veo su cabecera compacta de tres zonas (identidad a la izquierda con foto de 90×90 px, tres métricas titulares en el centro e indicador de cotización a la derecha) y las tres pestañas, con «Cotizaciones» seleccionada inicialmente.
2. **Given** un jugador con campos opcionales o imagen ausentes, **When** se presenta su identidad, **Then** no se inventan atributos personales y se muestra la imagen de fallback existente sin romper el diseño.
3. **Given** un usuario sin autenticar, un identificador con formato inválido o un jugador inactivo/inexistente, **When** intenta acceder a `/players/:id`, **Then** se aplican respectivamente la redirección al login (401), el error de identificador inválido (400) o la pantalla de no encontrado (404), sin revelar información privada.
4. **Given** una navegación rápida entre dos jugadores distintos con respuestas asíncronas lentas, **When** finalizan las consultas, **Then** ningún dato del primer jugador se muestra en la vista del segundo.

### User Story 2 - Comprender rendimiento deportivo según posición (Priority: P1)

Como usuario autenticado, quiero analizar el rendimiento deportivo del jugador mediante cuatro tarjetas KPI principales complementarias y dos paneles temáticos específicos adaptados a su posición, con unidades claras y sin gráficos engañosos.

**Why this priority**: Es el núcleo de comprensión deportiva de la aplicación y sustituye cualquier dato ficticio por estadísticas reales oficiales.
**Independent Test**: Consultar un arquero, un defensor, un mediocampista y un delantero con datos persistidos y contrastar sus métricas con los perfiles posicionales normativos.
**Acceptance Scenarios**:

1. **Given** estadísticas reales disponibles, **When** selecciono la pestaña «Resumen deportivo», **Then** veo las 4 tarjetas KPI complementarias de su posición (desduplicadas contra la cabecera y sin unidades redundantes) y los 2 paneles temáticos específicos con métricas por sector o faceta de juego, sin barras de progreso sin escala, sin radar y sin bloque de destacadas.
2. **Given** un valor cero real y una métrica ausente en el snapshot, **When** se presentan en el resumen, **Then** el cero se muestra explícitamente como número y la métrica ausente muestra «Sin datos».
3. **Given** un jugador con estadísticas disponibles pero sin cotizaciones calculadas o sin valoraciones de mercado, **When** abro su resumen deportivo, **Then** el resumen funciona con normalidad sin fallar ni bloquearse por la falta de datos financieros.
4. **Given** un jugador sin estadísticas confirmadas para la temporada consultada (`NO_DATA`), **When** abro el resumen, **Then** se muestra un aviso explícito de incompletitud sin generar rendimiento ficticio y manteniendo el acceso a las demás pestañas.

### User Story 3 - Consultar cotización oficial y evolución del token (Priority: P1)

Como usuario autenticado, quiero consultar la cotización oficial vigente del token del jugador y su evolución temporal histórica en una pestaña dedicada y en la cabecera, para evaluar su valor financiero en el mercado.

**Why this priority**: Permite al usuario conocer la valoración oficial de mercado del jugador, indispensable para la toma de decisiones económicas.
**Independent Test**: Consultar jugadores con cotización inicial, con variaciones positivas, con variaciones negativas y sin cotizaciones registradas.
**Acceptance Scenarios**:

1. **Given** un jugador con historial de cotizaciones registrado, **When** abro el detalle (pestaña «Cotizaciones» activa por defecto), **Then** veo la cotización vigente destacada en créditos en la cabecera y en la pestaña el panel de cotización con su precio actual, su variación porcentual/absoluta con badges semánticos (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`) y el gráfico cronológico de evolución de precios.
2. **Given** un jugador con una única cotización o con cotización de tipo `INITIAL`, **When** se visualiza su cotización, **Then** se muestra el badge «Cotización inicial» y no se muestra una variación porcentual errónea.
3. **Given** un jugador cuya última cotización calculada no cambió de valor respecto a la anterior, **When** se visualiza su cotización, **Then** se presenta el badge «Sin variación» y variación del 0,00%.
4. **Given** un jugador sin cotizaciones registradas en la base de datos, **When** se consulta su ficha, **Then** la cabecera muestra «Sin cotización» y la pestaña «Cotizaciones» informa que el jugador no dispone de cotizaciones oficiales aún, sin interrumpir la navegación por las demás pestañas.

### User Story 4 - Explorar catálogo completo de estadísticas (Priority: P2)

Como usuario autenticado, quiero consultar el inventario completo de las 60 métricas canónicas del jugador organizadas por categorías temáticas, con búsqueda instantánea y filtros rápidos.

**Why this priority**: Otorga acceso granular y transparente a todas las métricas extraídas de WhoScored sin sobrecargar visualmente el resumen deportivo.
**Independent Test**: Contar las métricas en la vista sin filtros (exactamente 60), buscar por nombre con y sin acentos, y filtrar por cada una de las 8 categorías comprobando el orden de relevancia posicional.
**Acceptance Scenarios**:

1. **Given** la pestaña «Todas las estadísticas» sin filtros activos, **When** la visualizo, **Then** tengo acceso a exactamente las 60 métricas canónicas, cada una en su categoría correspondiente, con su nombre legible, unidad y valor real o «Sin datos».
2. **Given** el catálogo de métricas, **When** introduzco un término de búsqueda y selecciono una categoría, **Then** se muestran únicamente las métricas que cumplen ambos criterios; pulsar «Limpiar» restablece el catálogo completo de 60 métricas.
3. **Given** un jugador con posición definida, **When** consulto cualquier categoría del catálogo, **Then** las métricas disponibles se ordenan por su prioridad deportiva (Principal → Secundaria → Poco relevante), seguidas al final por las métricas sin datos («Sin datos»).
4. **Given** una búsqueda sin coincidencias, **When** se aplica el filtro, **Then** se muestra un estado vacío explícito informando de la ausencia de resultados, permitiendo restablecer los filtros con un solo clic.

### User Story 5 - Adaptabilidad y experiencia en viewport (Priority: P2)

Como usuario, quiero acceder al detalle del jugador desde computadoras de escritorio, tablets o teléfonos móviles de forma accesible y adaptada al viewport, sin scroll innecesario en escritorio y con flujo documental natural en pantallas táctiles.

**Why this priority**: Asegura la usabilidad, legibilidad y cumplimiento de los estándares de accesibilidad visual y motriz en cualquier dispositivo.
**Independent Test**: Probar en resoluciones de escritorio (1920×1080, 1920×913, 1366×768, 1280×800) y móviles/tablets (768 px y 360 px), comprobando la ausencia de scroll de ventana en desktop y la navegación por teclado.
**Acceptance Scenarios**:

1. **Given** una resolución de escritorio estándar (1920×913 px o 1920×1080 px), **When** navego por el detalle y el resumen deportivo, **Then** la zona superior (breadcrumb, cabecera y pestañas: 209 px) permanece fija y el contenido se visualiza íntegro con cero scroll vertical u horizontal de ventana; en «Todas las estadísticas», únicamente el contenedor interno del catálogo realiza scroll vertical.
2. **Given** un dispositivo tablet o móvil (360 px a 768 px de ancho), **When** consulto el detalle, **Then** los bloques y tarjetas se apilan verticalmente con flujo documental natural y sin provocar scroll horizontal en ningún punto de la página.
3. **Given** un usuario que navega mediante teclado o lectores de pantalla, **When** interactúa con las pestañas, filtros y controles, **Then** existe foco visible evidente, roles ARIA apropiados en cada pestaña (`tab`, `tabpanel`) y alternativas textuales a los indicadores gráficos.

### Edge Cases

- Jugador elegible sin snapshot confirmado para la consulta: conservar identidad, pestañas y catálogo con ausencias; no confundirlo con jugador no elegible o inexistente, que recibe 404.
- Error de estadísticas con identidad resuelta: conservar identidad e informar error con reintento, sin rellenar con mocks.
- Carga pendiente: indicar carga, no «sin datos» antes de resolver la consulta.
- Posición desconocida: mostrar métricas generales y avisar que no existe perfil posicional, sin atribuir otra posición.
- Porcentajes, promedios y tasas no implican totales reconstruibles; no multiplicar tasas redondeadas para inventarlos.
- Campañas, ligas, temporadas o canales diferentes no se combinan silenciosamente.
- Nombres largos, búsqueda sin coincidencias y gráficos sin base suficiente no ocultan el retorno ni bloquean las pestañas.
- **Jugador sin estadísticas (`NO_DATA`)**: La identidad, la cotización y las pestañas permanecen disponibles; el catálogo lista las 60 métricas con «Sin datos» sin arrojar error 404 ni confundir con jugador inexistente.
- **Error en la consulta de estadísticas**: Se preserva la identidad y se muestra un banner de error con botón de reintento («Reintentar»), conservando la última respuesta exitosa si existía.
- **Carga asíncrona pendiente**: Los indicadores de carga señalan que la información se está obteniendo, sin catalogar preventivamente los datos como ausentes.
- **Posición desconocida**: Si el jugador no cuenta con una posición mapeable, el resumen deportivo muestra un mensaje informativo de ausencia de perfil posicional y mantiene accesibles las estadísticas generales en el catálogo.
- **Métricas con valor cero**: Los ceros reales se renderizan numéricamente como `0` o `0,0` y se mantienen en su posición de prioridad deportiva correspondiente; nunca se transforman en «Sin datos».
- **Búsqueda sin coincidencias**: Ocurre en el cliente de manera limpia sin bloquear los controles de navegación ni la posibilidad de limpiar los filtros.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-025**: Mantener detalle privado, acceso desde catálogo y URL directa, identidad interna, elegibilidad y diferencias entre ID inválido, sesión inválida, no encontrado y fallo de comunicación existentes (US1).
- **FR-026**: Presentar las pestañas «Cotizaciones», «Resumen deportivo» y «Todas las estadísticas», con «Cotizaciones» inicial; cambiar pestaña conserva jugador y contexto deportivo (US1, US5).
- **FR-027**: Consumir exclusivamente estadísticas reales persistidas por el backend; consultar no inicia scraping, sincronización, valoración o recotización y no requiere resultados financieros (US2).
- **FR-028**: Identificar temporada, liga cuando esté disponible, fecha de extracción y procedencia de los valores consultados; no etiquetar como actual una campaña distinta ni mezclar contextos sin advertencia (US2).
- **FR-029**: Diferenciar carga, error recuperable con reintento, ausencia completa y disponibilidad parcial. Cero es dato válido, ausencia no es cero; ninguna situación se rellena con simulaciones (US2).
- **FR-030**: Mostrar en Resumen identidad, exactamente cuatro tarjetas por posición y los gráficos y estadísticas adicionales especificados en los perfiles siguientes, siempre sujetos a disponibilidad. Una tarjeta sin valor conserva su etiqueta y muestra «Sin datos» (US2).
- **FR-031**: Las visualizaciones conservan unidades, etiquetas y valores textuales. No mezclar magnitudes de distinta unidad en un mismo eje; no convertir tasas en acumulados ni representar categorías como partes de un total sin una regla definida (US2, US4).
- **FR-032**: No incluir radar, percentiles, promedios poblacionales, normalizaciones ni indicadores derivados nuevos. Sustituir la zona del radar del mockup por estadísticas destacadas en unidades originales. No reutilizar scores ni atributos de SPEC-013 como sustitutos de estadísticas. Las tasas ya canónicas y persistidas no son indicadores nuevos (US2).
- **FR-033**: Exponer exactamente las 60 claves canónicas de SPEC-012, con nombre comprensible en español, categoría, unidad original y valor real o «No disponible». No añadir xG, xA ni otras métricas del mockup ajenas al catálogo (US5).
- **FR-034**: Agrupar en General, Ataque, Pases, Regates, Duelos, Defensa, Portería y Disciplina. Cada clave pertenece a una sola categoría; General incluye minutos, apariciones, titularidades y rating; los demás grupos siguen la naturaleza deportiva del catálogo, no el canal de extracción (US5).
- **FR-035**: Buscar por nombre visible o clave canónica sin distinguir mayúsculas ni acentos, aplicar coincidencia parcial y combinar con categoría. Mostrar cantidad de resultados y permitir limpiar filtros; todas las métricas siguen accesibles aunque la presentación se divida en páginas (US5).
- **FR-036**: No inventar acumulados, mapas de eventos, coordenadas ni series temporales desde agregados de temporada. Los gráficos representan exclusivamente desgloses realmente disponibles (US2).
- **FR-037**: Preservar navbar, sesión, retorno al catálogo, identidad e imágenes existentes; no rediseñar otras páginas ni cambiar las rutas de entrada. La compatibilidad con catálogo no implica perpetuar sus mocks ni presentarlos como estadísticas reales o cotizaciones oficiales del detalle (US1, US4).
- **FR-038**: Mantener colores, tipografía y jerarquía actuales del sitio, tomando el mockup como referencia visual. Adaptar ancho y altura disponibles, con desplazamiento vertical natural y sin desbordamiento horizontal de la página en los viewports de US4 (US4).
- **FR-039**: Mantener teclado, foco visible, etiquetas, selección de pestaña perceptible y alternativa textual a toda visualización; no depender solo del color (US4).
- **FR-040**: En «Resumen deportivo» y «Todas las estadísticas» no mostrar cotización ni historial financiero. La cotización oficial solo se muestra en la cabecera y en la pestaña dedicada, consumiendo datos oficiales persistidos; su presentación no autoriza datos simulados ni mutaciones financieras durante la consulta (US2, US3).
- **FR-041**: Consultar la última extracción confirmada disponible del jugador para la temporada seleccionada y mostrar su fecha real. No aplicar la selección poblacional de SPEC-013 ni exigir que otros jugadores tengan datos. Ordenar fechas de extracción de más reciente a más antigua; dentro de un mismo canal y contexto, desempatar por instante de observación más reciente y, si persiste el empate, por identificador de registro mayor. En la fecha y contexto seleccionados, JSON tiene prioridad: scraping solo completa valores JSON nulos, incluidas sus tres claves exclusivas (`starts_total`, `shots_on_target_total`, `second_yellow_total`), cuando coinciden jugador, temporada, fecha de extracción y contexto de competición. Un cero JSON es válido y nunca activa respaldo. No buscar valores en fechas o contextos diferentes. Si únicamente existe scraping confirmado en la fecha seleccionada, usar sus valores disponibles sin exigir JSON. La respuesta distingue ausencia de valor de fallo de consulta y conserva la procedencia efectiva por métrica, sin prescribir rutas nuevas ni almacenamiento.
- **FR-042**: En cada categoría de Todas las estadísticas, ordenar disponibles por Principal → Secundaria → Poco relevante, y después las métricas sin datos. Dentro de cada nivel mantener el orden de la matriz canónica siguiente. Conservar categoría y clasificación de relevancia aun cuando falte el valor; «Sin datos» es disponibilidad, no una reclasificación deportiva permanente (US5).
- **FR-043**: Todas las estadísticas incluye las 60 claves incluso para un jugador elegible sin snapshot confirmado para la consulta. Mostrar «Sin datos» por valor ausente; carga y error de consulta conservan sus estados propios y no se presentan como ausencia confirmada. Una métrica numérica cero sigue siendo disponible (US5).
- **FR-044**: La relevancia de las tres claves actualmente ausentes es fija según la matriz, independiente de su disponibilidad. Al aparecer valores reales, incluido cero, presentar el valor y aplicar automáticamente su prioridad en Todas las estadísticas, sin cambiar tarjetas, gráficos ni adicionales del Resumen (US5).
- **FR-045**: Identificar la competición del snapshot con nombre legible solo cuando exista una correspondencia fiable de su clave histórica con información disponible. En ausencia de nombre mostrar «Competición no disponible», conservando la clave como contexto, sin inventar nombres, consultar proveedores ni usar la liga actual del jugador como sustituto. El nombre de la liga actual de identidad permanece separado del contexto estadístico (US2).
- **FR-046**: En identidad y estadísticas, formato de ID inválido (no entero decimal positivo o fuera del rango Long) recibe 400; ID válido inexistente o no visible, 404 indistinguible. Jugador visible sin estadísticas para la consulta recibe respuesta estadística exitosa con estado NO_DATA y 60 ausencias, no 404. Es contrato requerido para la implementación futura, no afirmación de que la identidad existente ya valide cero/negativos (US1, US5).
- **FR-047**: Ante fallo de actualización, conservar únicamente la última respuesta exitosa del mismo jugador y temporada resuelta, con sus valores, disponibilidad y fecha de extracción originales, mostrando el error y «Mostrando la última consulta disponible; no se pudo actualizar» con reintento. El error no convierte valores en Sin datos ni los etiqueta como recién obtenidos. Si no existe respuesta previa compatible, presentar error sin estadísticas; nunca usar datos de otro jugador o temporada. Si el contexto pedido cambia, retirar la respuesta incompatible. Una respuesta exitosa NO_DATA sustituye la anterior: no conservar valores antiguos para ocultar la ausencia nueva (US2).
- **FR-048**: Las etiquetas, categorías y unidades visibles de las 60 claves son las del [glosario canónico de presentación](contracts/metric-glossary.md). El glosario no añade métricas ni cálculos; PERCENTAGE se presenta como porcentaje ya persistido, no se multiplica por 100. Las prioridades continúan en la matriz posicional (US5).

#### Cotizaciones Oficiales de Tokens
- **FR-005**: Exponer `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current` para obtener el historial y la cotización vigente del jugador. Un jugador sin cotizaciones responde lista vacía o 404 (`QUOTE_NOT_FOUND`) sin provocar errores no controlados en la interfaz (US3).
- **FR-006**: Presentar la cotización oficial en el bloque derecho de la cabecera: precio en créditos con símbolo de moneda FM, badge semántico (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`) y variación absoluta en créditos respecto a la cotización anterior. Si el jugador no tiene cotización oficial, muestra «Sin cotización» (US1, US3).
- **FR-007**: Disponer de la pestaña «Cotizaciones» como la vista interactiva predeterminada al ingresar al detalle, presentando el panel de cotización oficial y el gráfico cronológico de evolución de precios del token (US3).

#### Estadísticas Deportivas Reales
- **FR-008**: Exponer `GET /api/players/{id}/statistics` para consultar el último snapshot individual confirmado del jugador extraído de WhoScored, filtrable opcionalmente por temporada (`seasonKey`). La consulta es estrictamente de lectura local: no dispara scraping ni procesos en segundo plano (US2, US4).
- **FR-009**: Identificar temporada, fecha de extracción y procedencia del snapshot estadístico consultado. No se combinan ni mezclan temporadas o contextos deportivos distintos de forma silenciosa (US2).
- **FR-010**: En la resolución del snapshot en backend, priorizar la extracción JSON sobre scraping para la misma fecha y contexto; el scraping únicamente complementa valores nulos en claves ausentes (`starts_total`, `shots_on_target_total`, `second_yellow_total`). Un cero en JSON es dato válido y no se reemplaza (US2).
- **FR-011**: Diferenciar inequívocamente los estados de carga, error recuperable con reintento, ausencia completa de estadísticas (`NO_DATA`) y disponibilidad parcial. Cero es dato válido; una métrica ausente en el snapshot muestra «Sin datos» y no se sustituye por cero (US2).
- **FR-012**: Ante fallo de red o error al actualizar estadísticas, conservar visible la última respuesta válida en memoria con la advertencia «Mostrando la última consulta disponible» y opción de reintento. Si se navega a otro jugador, los datos en caché incompatible se descartan de inmediato (US1, US2).

#### Cabecera y Resumen Deportivo Posicional
- **FR-013**: Presentar en el bloque central de la cabecera exactamente tres micro-tarjetas con las métricas titulares fijadas por el perfil posicional del jugador (US1, US2).
- **FR-014**: Presentar en la pestaña «Resumen deportivo» exactamente cuatro tarjetas KPI principales superiores desduplicadas (que no repiten ninguna métrica de la cabecera ni duplican unidades en sus títulos) y exactamente dos paneles temáticos específicos por posición (con listas de métricas con iconos y divisores limpios) (US2).
- **FR-015**: Prohibir en el resumen deportivo el uso de barras de progreso sin escala de referencia válida, gráficos de radar, percentiles calculados, promedios poblacionales y bloques de «estadísticas destacadas» que dupliquen información (US2).
- **FR-016**: Si el jugador no posee una posición reconocida con perfil posicional, mostrar un aviso informativo de ausencia de perfil y presentar las estadísticas generales disponibles en el catálogo (US2).

#### Catálogo Completo de 60 Métricas
- **FR-017**: Exponer en la pestaña «Todas las estadísticas» exactamente las 60 claves canónicas de SPEC-012, organizadas en 8 categorías temáticas: General (5), Ataque (19), Pases (10), Regates (5), Duelos (3), Defensa (10), Portería (4) y Disciplina (4) (US4).
- **FR-018**: Proveer controles de exploración integrados en la cabecera del catálogo: campo de búsqueda por nombre o clave (insensible a mayúsculas y acentos), botones de filtro por categoría con contador de métricas y botón «Limpiar» para reiniciar los filtros (US4).
- **FR-019**: Ordenar las métricas dentro de cada categoría según la matriz de relevancia posicional: primero Principal (P), luego Secundaria (S) y finalmente Poco relevante (B). Las métricas ausentes se ubican al final con «Sin datos». Un cero real mantiene su prioridad posicional (US4).
- **FR-020**: Las etiquetas, categorías y unidades de las 60 claves son las establecidas en el glosario canónico (`contracts/metric-glossary.md`). Los porcentajes se muestran formateados tal como están persistidos, sin multiplicaciones adicionales (US4).

#### Layout, Viewport y Accesibilidad
- **FR-021**: En pantallas de escritorio (referencia 1920×913 px y 1920×1080 px), la zona superior fija (209 px: breadcrumb 32 px, cabecera 125 px, separación 12 px y pestañas 40 px) permanece estática; el contenido se adapta de forma flexible aprovechando la altura disponible con cero scroll de ventana en la vista de Resumen deportivo y Cotizaciones. En «Todas las estadísticas», únicamente el contenedor interno del catálogo realiza scroll vertical (US5).
- **FR-022**: En tablets y teléfonos móviles, los componentes colapsan de forma armónica (KPIs en 2 columnas en tablet y 1 en móvil; paneles apilados) con scroll vertical documental natural y ausencia total de scroll horizontal (US5).
- **FR-023**: Utilizar exclusivamente la paleta de tokens semánticos sólidos aprobada (`$surface-page-deep`, `$surface-card-dark`, `$surface-panel-to`, `$surface-panel-from`, `$surface-accent-deep`, `$surface-accent-dark`, `$accent-sports-green`, `$border-panel`), sin gradientes, sin efectos de iluminación y sin sombras decorativas (US5).
- **FR-024**: Garantizar navegación por teclado con foco visible, indicadores de estado de pestaña mediante atributos `role="tab"` y `aria-selected`, y textos accesibles complementarios para distinguir la relevancia posicional más allá del código de color (US5).

---

### Estructura visual aprobada

```text
Cabecera del detalle (fija en desktop, 125 px con box-sizing: border-box)
├── Zona 1 (Izquierda): Identidad (retrato 90×90 px, nombre, dorsal, posición, equipo y liga)
├── Zona 2 (Centro): Tres métricas titulares de cabecera según posición (tarjetas de 94 px)
└── Zona 3 (Derecha): Cotización oficial vigente en créditos, moneda FM y badge de variación

Barra de pestañas (altura 40 px, inmediatamente tras la cabecera)
├── Pestaña 1: Cotizaciones (predeterminada activa)
├── Pestaña 2: Resumen deportivo
└── Pestaña 3: Todas las estadísticas

Pestaña Cotizaciones
├── Panel de cotización oficial destacada (precio, fecha de cálculo, variación absoluta/porcentual)
└── Gráfico interactivo de evolución cronológica de cotizaciones

Pestaña Resumen deportivo (distribución armónica, cero scroll en desktop)
├── Fila de cuatro tarjetas KPI complementarias (desduplicadas contra cabecera)
├── Panel temático posicional A (lista de métricas temáticas con iconos y divisores limpios)
└── Panel temático posicional B (lista de métricas temáticas con iconos y divisores limpios)

Pestaña Todas las estadísticas (contenedor con scroll vertical interno exclusivo)
├── Cabecera de exploración: buscador en tiempo real + botón limpiar
├── Barra de filtros por categoría con conteo de métricas
└── Grupos por categoría ordenados por relevancia posicional (P → S → B → Sin datos)
```

---

### Perfiles posicionales de Cabecera y Resumen

Las tres métricas titulares de cabecera y las cuatro tarjetas KPI de Resumen deportivo son fijas para cada perfil posicional, sin sustitución automática ante ausencia. Las cuatro tarjetas de Resumen son estrictamente complementarias y desduplicadas (no repiten ninguna métrica de la cabecera).

| Posición | Tres métricas titulares de cabecera | Cuatro tarjetas KPI de Resumen | Panel temático A (Métricas originales) | Panel temático B (Métricas originales) |
| :--- | :--- | :--- | :--- | :--- |
| **Arquero** | `saves_per_match` (Atajadas/partido)<br>`pass_completion_percentage` (Precisión pases, %)<br>`successful_long_balls_per_match` (Pases largos/partido) | `whoscored_rating_average` (Calificación media)<br>`appearances_total` (Partidos temporada)<br>`clearances_per_match` (Despejes/partido)<br>`passes_per_match` (Pases/partido) | **Atajadas por sector:**<br>`penalty_area_saves_per_match`<br>`outside_box_saves_per_match`<br>`six_yard_box_saves_per_match` | **Distribución y juego con los pies:**<br>`passes_per_match`<br>`inaccurate_short_passes_per_match`<br>`inaccurate_long_balls_per_match` |
| **Defensor** | `tackles_per_match` (Entradas/partido)<br>`interceptions_per_match` (Intercepciones/partido)<br>`clearances_per_match` (Despejes/partido) | `whoscored_rating_average` (Calificación media)<br>`aerial_duels_won_per_match` (Duelos aéreos ganados/partido)<br>`pass_completion_percentage` (Precisión pases, %)<br>`blocked_shots_per_match` (Tiros bloqueados/partido) | **Contención y duelos:**<br>`attempted_tackles_per_match`<br>`blocked_passes_per_match`<br>`dribbled_past_per_match`<br>`blocked_crosses_per_match` | **Juego aéreo y disciplina:**<br>`aerial_duels_lost_per_match`<br>`fouls_committed_per_match`<br>`yellow_cards_total`<br>`red_cards_total` |
| **Mediocampista** | `passes_per_match` (Pases/partido)<br>`pass_completion_percentage` (Precisión pases, %)<br>`key_passes_per_match` (Pases clave/partido) | `whoscored_rating_average` (Calificación media)<br>`assists_total` (Asistencias temporada)<br>`tackles_per_match` (Entradas/partido)<br>`successful_dribbles_per_match` (Regates exitosos/partido) | **Construcción y distribución:**<br>`accurate_short_passes_per_match`<br>`successful_long_balls_per_match`<br>`short_key_passes_per_match`<br>`successful_crosses_per_match` | **Posesión y equilibrio:**<br>`dispossessed_per_match`<br>`bad_controls_per_match`<br>`aerial_duels_won_per_match`<br>`fouls_committed_per_match` |
| **Delantero** | `goals_total` (Goles temporada)<br>`shots_per_match` (Tiros/partido)<br>`assists_total` (Asistencias temporada) | `whoscored_rating_average` (Calificación media)<br>`shots_on_target_total` (Tiros al arco temporada)<br>`goals_per_match` (Goles/partido)<br>`successful_dribbles_per_match` (Regates exitosos/partido) | **Definición por zona:**<br>`penalty_area_shots_per_match`<br>`six_yard_box_shots_per_match`<br>`outside_box_shots_per_match`<br>`offsides_per_match` | **Juego ofensivo y generación:**<br>`key_passes_per_match`<br>`aerial_duels_won_per_match`<br>`dispossessed_per_match`<br>`successful_crosses_per_match` |

---

### Catálogo completo y prioridad posicional

**P** = Principal; **S** = Secundaria; **B** = Poco relevante.<br>
La prioridad deportiva no depende de la disponibilidad del valor: las métricas ausentes conservan su prioridad en la matriz pero se presentan con «Sin datos» al final de su categoría.

| Categoría | Clave canónica | Arquero | Defensor | Mediocampista | Delantero |
| :--- | :--- | :---: | :---: | :---: | :---: |
| General | `minutes_played_total` | P | P | P | P |
| General | `appearances_total` | P | P | P | P |
| General | `starts_total` | P | P | P | P |
| General | `substitute_appearances_total` | S | S | S | S |
| General | `whoscored_rating_average` | P | P | P | P |
| Ataque | `goals_total` | B | S | S | P |
| Ataque | `assists_total` | B | S | P | P |
| Ataque | `shots_per_match` | B | S | S | P |
| Ataque | `shots_on_target_total` | B | S | S | P |
| Ataque | `goals_per_match` | B | S | S | P |
| Ataque | `assists_per_match` | B | S | P | P |
| Ataque | `six_yard_box_shots_per_match` | B | S | S | P |
| Ataque | `penalty_area_shots_per_match` | B | S | S | P |
| Ataque | `outside_box_shots_per_match` | B | S | S | P |
| Ataque | `six_yard_box_goals_per_match` | B | B | S | P |
| Ataque | `penalty_area_goals_per_match` | B | B | S | P |
| Ataque | `outside_box_goals_per_match` | B | B | S | P |
| Ataque | `cross_assists_per_match` | B | B | S | S |
| Ataque | `corner_assists_per_match` | B | B | S | S |
| Ataque | `through_ball_assists_per_match` | B | B | S | S |
| Ataque | `free_kick_assists_per_match` | B | B | S | S |
| Ataque | `throw_in_assists_per_match` | B | B | B | S |
| Ataque | `other_assists_per_match` | B | B | S | S |
| Ataque | `offsides_per_match` | B | S | S | P |
| Pases | `pass_completion_percentage` | P | P | P | S |
| Pases | `passes_per_match` | S | S | P | S |
| Pases | `successful_long_balls_per_match` | P | S | P | S |
| Pases | `inaccurate_long_balls_per_match` | S | S | S | S |
| Pases | `accurate_short_passes_per_match` | S | S | P | S |
| Pases | `inaccurate_short_passes_per_match` | S | S | S | S |
| Pases | `key_passes_per_match` | S | S | P | P |
| Pases | `long_key_passes_per_match` | S | S | P | S |
| Pases | `short_key_passes_per_match` | B | S | P | S |
| Pases | `successful_crosses_per_match` | B | S | P | S |
| Regates | `successful_dribbles_per_match` | B | S | P | P |
| Regates | `failed_dribbles_per_match` | B | S | S | P |
| Regates | `total_dribbles_per_match` | B | S | S | P |
| Regates | `dispossessed_per_match` | S | S | P | P |
| Regates | `bad_controls_per_match` | S | S | P | P |
| Duelos | `aerial_duels_won_per_match` | S | P | S | P |
| Duelos | `aerial_duels_lost_per_match` | S | S | S | S |
| Duelos | `total_aerial_duels_per_match` | S | S | S | S |
| Defensa | `tackles_per_match` | B | P | P | S |
| Defensa | `interceptions_per_match` | B | P | P | S |
| Defensa | `clearances_per_match` | S | P | S | S |
| Defensa | `blocked_shots_per_match` | B | P | S | S |
| Defensa | `dribbled_past_per_match` | B | P | S | S |
| Defensa | `offsides_won_per_match` | B | S | S | B |
| Defensa | `own_goals_total` | S | S | B | B |
| Defensa | `attempted_tackles_per_match` | B | P | S | S |
| Defensa | `blocked_crosses_per_match` | B | P | S | B |
| Defensa | `blocked_passes_per_match` | B | P | S | S |
| Portería | `saves_per_match` | P | B | B | B |
| Portería | `six_yard_box_saves_per_match` | P | B | B | B |
| Portería | `penalty_area_saves_per_match` | P | B | B | B |
| Portería | `outside_box_saves_per_match` | P | B | B | B |
| Disciplina | `yellow_cards_total` | S | S | S | S |
| Disciplina | `red_cards_total` | S | S | S | S |
| Disciplina | `second_yellow_total` | S | S | S | S |
| Disciplina | `fouls_committed_per_match` | S | S | S | S |

---

### Key Entities

- **Jugador**: Identidad interna local con ID, nombre, posición deportiva, equipo, liga, fecha de nacimiento, nacionalidad y retrato opcional.
- **Cotización oficial del token**: Precio vigente en créditos, tipo de cotización (`INITIAL`, `RECALCULATED`, `CARRIED_OVER`), fecha de vigencia, variación porcentual y variación absoluta.
- **Snapshot estadístico**: Conjunto inmutable de 60 claves canónicas para un jugador, temporada y fecha de extracción resueltos desde WhoScored/JSON.
- **Perfil posicional**: Asignación de 3 métricas de cabecera, 4 KPIs complementarios y 2 paneles temáticos específicos para una de las cuatro posiciones deportivas.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En acceso desde el catálogo, URL directa o recarga, el 100% de los datos visualizados corresponde al jugador solicitado; la alternancia rápida entre dos jugadores distintos no produce solapamiento ni mezcla de datos (FR-001, FR-002, FR-003).
- **SC-002**: Las tres pestañas («Cotizaciones», «Resumen deportivo», «Todas las estadísticas») están operables, con «Cotizaciones» seleccionada de forma predeterminada al ingresar a la pantalla (FR-006, FR-007).
- **SC-003**: En la pestaña «Cotizaciones», se presenta la cotización vigente con su badge semántico y el gráfico de evolución cronológica consumiendo los endpoints oficiales del backend; un jugador sin cotizaciones muestra «Sin cotización» sin bloquear el resto de la interfaz (FR-005, FR-006, FR-007).
- **SC-004**: Para un jugador de cualquiera de las cuatro posiciones, el 100% de las métricas visibles en Resumen coincide con el perfil posicional asignado; los ceros reales se visualizan numéricamente y las ausencias se identifican como «Sin datos» sin generar métricas ficticias ni mostrar barras de progreso sin escala (FR-010, FR-011, FR-013, FR-014, FR-015).
- **SC-005**: La pestaña «Todas las estadísticas» lista exactamente las 60 claves canónicas sin duplicados, agrupadas en sus 8 categorías; la combinación de búsqueda y filtro por categoría devuelve la intersección exacta de resultados y el botón «Limpiar» restablece las 60 métricas (FR-017, FR-018, FR-019).
- **SC-006**: En pantallas de escritorio (1920×1080 px y 1920×913 px), la cabecera superior (209 px) permanece fija y la página se visualiza con cero scroll de ventana vertical u horizontal en el Resumen deportivo; en el catálogo, únicamente la lista interna de métricas realiza scroll (FR-021).
- **SC-007**: En pantallas de tablet (768 px) y móvil (360 px), los elementos se reorganizan en 1 o 2 columnas sin desbordamiento horizontal de página y permitiendo el flujo documental natural (FR-022).
- **SC-008**: Los estados de carga, error recuperable, ausencia de datos (`NO_DATA`) e identificador no encontrado (404) están claramente diferenciados y ofrecen opciones de reintento o retorno sin romper la sesión (FR-002, FR-011, FR-012).

---

## Assumptions

- La sesión de usuario y autenticación JWT están gobernadas por la infraestructura global de la aplicación.
- La persistencia y recálculo de cotizaciones de tokens y la extracción de estadísticas deportivas WhoScored se gestionan mediante sus respectivos servicios de backend y no se ejecutan desde esta interfaz.
- La cobertura estadística depende de la disponibilidad de datos extraídos para el jugador; no se garantiza que el 100% de los jugadores disponga de valores para las 60 métricas.

## Dependencies and Integration

- **Backend**:
  - `PlayerController`: `GET /api/players/{id}` y `GET /api/players/{id}/statistics`
  - `PlayerQuoteController`: `GET /api/players/{id}/quotes` y `GET /api/players/{id}/quotes/current`
- **Frontend**:
  - `PlayerOverview`: Renderizado de identidad, titulares de cabecera y cotización oficial.
  - `PlayerQuotesTab`: Visualización de cotizaciones y gráfico de evolución.
  - `PlayerSummaryTab`: KPIs posicionales y paneles temáticos.
  - `PlayerCatalogTab`: Exploración y búsqueda de 60 métricas.
- **Documentación y contratos**:
  - [Glosario canónico de métricas](contracts/metric-glossary.md)
  - [Wireframe canónico del detalle del jugador](../../docs/wireframes/player-detail.md)

## Out of Scope

- Disparo manual o programado de scraping de estadísticas o sincronizaciones de catálogo desde esta pantalla.
- Ejecución de compra o venta de tokens (gestionadas por las pantallas de Mercado).
- Gráficos de radar, métricas de xG o xA, comparativas poblacionales entre jugadores o indicadores derivados no definidos en el catálogo canónico de WhoScored.

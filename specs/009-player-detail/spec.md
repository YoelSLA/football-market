# Feature Specification: Detalle individual y estadísticas reales del jugador

**Feature Branch vigente**: `feat/009-player-detail-whoscored-stats-dashboard`
**Created**: 2026-10-12 · **Revisión**: 2026-10-25
**Status vigente**: Ready for planning — Clarify del rediseño completado documentalmente; no autoriza ni ejecuta PLAN o implementación.
**Input vigente**: Rediseñar SPEC-009 con exactamente dos pestañas, estadísticas reales persistidas de WhoScored, resumen por posición y catálogo de 60 métricas; sin dependencia de valoraciones o cotizaciones.

## Trazabilidad del rediseño

Esta revisión reutiliza `009-player-detail`. El [issue #72](https://github.com/YoelSLA/football-market/issues/72) mantiene su título y referencia al [issue original #51](https://github.com/YoelSLA/football-market/issues/51). No es una nueva asignación de identidad.

Las secciones anteriores a «Antecedente histórico» son la especificación vigente. Todo el contenido posterior se conserva íntegro como evidencia histórica, incluidos sus IDs, decisiones y aprobaciones; no gobierna el nuevo dashboard cuando contradiga esta revisión. Los nuevos requisitos usan FR-025 en adelante para no reutilizar IDs históricos con otro significado. US1 conserva identidad y navegación, US2 sustituye la demostración por datos reales, US3 queda retirada para este alcance (finanzas) y US4 conserva adaptación y accesibilidad; US5 incorpora el catálogo completo.

La sustitución de mocks y la retirada de los bloques financieros del detalle afectan el alcance evaluado anteriormente. La propuesta inicial de radar queda sustituida por métricas en unidades originales por decisión del usuario. Las aprobaciones históricas no autorizan implementar este rediseño. Plan, contratos, tareas y checklists previos se conservan, pero requieren revisión posterior por impacto; no se presentan como coherentes ni aprobados para este alcance.

Referencia visual: [mockup de estadísticas](../../referencias-imagenes/spec-009-player-detail-statistics-mockup.png). Se conserva su jerarquía de identidad, pestañas, indicadores, gráficos y catálogo, no sus cifras, equipo de ejemplo, xG, xA, percentiles, totales derivados ni controles sin datos confirmados. La zona del radar se dedica a estadísticas destacadas con valores originales, sin radar normalizado ni comparación poblacional. El catálogo canónico es el de [SPEC-012, §Catálogo de métricas](../012-player-statistics-scraping/spec.md#catálogo-de-métricas): 60 claves; 57 cubiertas por JSON, no 60 valores garantizados por jugador.

## Clarifications

### Session 2026-10-25

- Q: ¿El detalle debe mostrar la última extracción confirmada del jugador para su temporada, o usar la última campaña JSON confirmada común a toda la población? → A: Utilizar la última extracción confirmada disponible del jugador para la temporada seleccionada, mostrar la fecha real y resolver empates determinísticamente. No aplicar la selección poblacional de SPEC-013.
- Q: ¿Confirmás que JSON tenga prioridad sobre scraping dentro de la fecha seleccionada, utilizando respaldo únicamente del mismo jugador, temporada y contexto de competición? → A: JSON prioritario; scraping completa únicamente valores nulos y sus tres claves exclusivas cuando coinciden jugador, temporada, fecha de extracción y contexto de competición. Nunca reemplazar ceros ni mezclar fechas o contextos. Si solo existe scraping confirmado en la fecha seleccionada, utilizar sus valores disponibles. La selección permanece individual e independiente de SPEC-013.
- Q: ¿Qué prioridad deben tener las tres métricas actualmente ausentes cuando aparezcan datos reales? → A: Titularidades principal en todas las posiciones; tiros al arco poco relevante en arquero, secundaria en defensor y mediocampista, principal en delantero; segundas amarillas secundaria en todas. Mantener «Sin datos» hasta tener valores reales y aplicar entonces automáticamente la prioridad. Disponibilidad y relevancia independientes; no cambiar las cuatro tarjetas ni los gráficos del Resumen.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar identidad y navegar (Priority: P1)

Como usuario autenticado, quiero abrir un jugador desde el catálogo o directamente y reconocer su identidad sin depender de la navegación anterior.

**Why this priority**: Evita atribuir estadísticas a otra persona y mantiene la entrada existente.
**Independent Test**: Abrir desde catálogo, recargar y abrir directamente; contrastar identidad y retorno.
**Acceptance Scenarios**:

1. **Given** un jugador visible y sesión válida, **When** abro su detalle, **Then** veo su identidad real y las dos pestañas, con Resumen seleccionada inicialmente.
2. **Given** campos opcionales o imagen ausentes, **When** se presenta la identidad, **Then** no se inventan atributos y se mantiene el fallback de imagen existente.
3. **Given** un visitante, ID inválido o jugador inexistente/inactivo, **When** intenta acceder, **Then** se conserva respectivamente el acceso protegido, identificador inválido o no encontrado, sin revelar identidad privada.
4. **Given** navegación de A a B con respuestas tardías, **When** terminan las consultas, **Then** ningún bloque de A se muestra como perteneciente a B.

### User Story 2 - Comprender rendimiento real según posición (Priority: P1)

Como usuario, quiero interpretar cuatro tarjetas, estadísticas destacadas y gráficos relevantes para la posición del jugador con sus unidades y contexto deportivo.

**Why this priority**: Es el valor principal del nuevo detalle y reemplaza los datos de demostración.
**Independent Test**: Revisar un arquero, defensor, mediocampista y delantero con estadísticas persistidas, incluidos datos parciales.
**Acceptance Scenarios**:

1. **Given** estadísticas reales disponibles, **When** abro Resumen, **Then** veo identidad, exactamente las cuatro tarjetas de mi perfil y los gráficos y estadísticas adicionales definidos abajo, con temporada, unidades y procedencia identificables, sin radar ni percentiles.
2. **Given** un cero real y una métrica ausente, **When** se presentan, **Then** el primero conserva su cero y la segunda indica «No disponible», sin sustituirla por cero.
3. **Given** estadísticas presentes pero ninguna valoración ni cotización calculada, **When** consulto el resumen, **Then** funciona sin solicitar recálculos ni mostrar datos financieros.
4. **Given** información insuficiente para un gráfico, **When** se presenta el resumen, **Then** informa la limitación y mantiene visibles los valores disponibles, sin dibujar rendimiento ficticio ni sustituir silenciosamente las métricas de las tarjetas.

### User Story 4 - Usar el dashboard dentro del viewport (Priority: P2)

Como usuario, quiero consultar ambas pestañas con teclado o puntero en escritorio, tablet y móvil, manteniendo el estilo de Football Market.

**Why this priority**: El catálogo y sus gráficos deben ser legibles sin perder navegación.
**Independent Test**: Revisar ambas pestañas a anchos de 360, 768 y 1280 píxeles y alturas de 640 y 800 píxeles.
**Acceptance Scenarios**:

1. **Given** cualquiera de esos viewports, **When** navego por ambas pestañas, **Then** el contenido se adapta al ancho y permite desplazamiento vertical sin recortes ni superposiciones; no exige que las 60 métricas entren simultáneamente en pantalla.
2. **Given** teclado o ausencia de percepción del color, **When** uso pestañas, buscador, categorías y retorno, **Then** hay foco visible, selección perceptible y alternativas textuales para los gráficos.
3. **Given** cualquier estado del detalle, **When** uso la navegación existente, **Then** conservo los destinos y capacidades de sesión del sitio y puedo volver al catálogo.

### User Story 5 - Explorar todas las estadísticas (Priority: P2)

Como usuario, quiero encontrar cualquiera de las 60 métricas por categoría y nombre sin confundir ausencia con rendimiento cero.

**Why this priority**: Da acceso al inventario completo sin sobrecargar el resumen.
**Independent Test**: Contar las claves, aplicar categorías y buscar nombres con y sin acentos; contrastar valores con los registros seleccionados.
**Acceptance Scenarios**:

1. **Given** el catálogo sin filtros, **When** abro Todas las estadísticas, **Then** están accesibles exactamente las 60 claves canónicas una vez cada una, incluso las no disponibles.
2. **Given** una categoría y texto de búsqueda, **When** los aplico juntos, **Then** veo su intersección; limpiar ambos restaura las 60 métricas y una búsqueda sin coincidencias muestra un estado vacío explícito.
3. **Given** una métrica disponible, **When** la consulto en ambas pestañas, **Then** coinciden valor, unidad y contexto; las ausentes no reciben valores inventados.
4. **Given** una posición reconocida, **When** consulto una categoría, **Then** aparecen primero las métricas principales, luego secundarias y finalmente poco relevantes; las ausentes permanecen accesibles al final con «Sin datos», y un cero conserva su posición de relevancia.

### Edge Cases

- Sin estadísticas: conservar identidad, pestañas y catálogo con ausencias; no confundirlo con jugador inexistente.
- Error de estadísticas con identidad resuelta: conservar identidad e informar error con reintento, sin rellenar con mocks.
- Carga pendiente: indicar carga, no «sin datos» antes de resolver la consulta.
- Posición desconocida: mostrar métricas generales y avisar que no existe perfil posicional, sin atribuir otra posición.
- Porcentajes, promedios y tasas no implican totales reconstruibles; no multiplicar tasas redondeadas para inventarlos.
- Campañas, ligas, temporadas o canales diferentes no se combinan silenciosamente.
- Nombres largos, búsqueda sin coincidencias y gráficos sin base suficiente no ocultan el retorno ni bloquean las pestañas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-025**: Mantener detalle privado, acceso desde catálogo y URL directa, identidad interna, elegibilidad y diferencias entre ID inválido, sesión inválida, no encontrado y fallo de comunicación existentes (US1).
- **FR-026**: Presentar únicamente las pestañas «Resumen» y «Todas las estadísticas», con Resumen inicial; cambiar pestaña conserva jugador y contexto deportivo (US1, US5).
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
- **FR-037**: Preservar navbar, sesión, retorno al catálogo, identidad e imágenes existentes; no rediseñar otras páginas ni cambiar las rutas de entrada. La compatibilidad con catálogo no implica perpetuar sus mocks ni presentarlos como estadísticas reales del detalle (US1, US4).
- **FR-038**: Mantener colores, tipografía y jerarquía actuales del sitio, tomando el mockup como referencia visual. Adaptar ancho y altura disponibles, con desplazamiento vertical natural y sin desbordamiento horizontal de la página en los viewports de US4 (US4).
- **FR-039**: Mantener teclado, foco visible, etiquetas, selección de pestaña perceptible y alternativa textual a toda visualización; no depender solo del color (US4).
- **FR-040**: No mostrar cotización ni historial financiero en estas dos pestañas. Su retirada de esta vista no autoriza eliminar capacidades, componentes compartidos ni contratos consumidos fuera de ella (US2).
- **FR-041**: Consultar la última extracción confirmada disponible del jugador para la temporada seleccionada y mostrar su fecha real. No aplicar la selección poblacional de SPEC-013 ni exigir que otros jugadores tengan datos. Ordenar fechas de extracción de más reciente a más antigua; dentro de un mismo canal y contexto, desempatar por instante de observación más reciente y, si persiste el empate, por identificador de registro mayor. En la fecha y contexto seleccionados, JSON tiene prioridad: scraping solo completa valores JSON nulos, incluidas sus tres claves exclusivas (`starts_total`, `shots_on_target_total`, `second_yellow_total`), cuando coinciden jugador, temporada, fecha de extracción y contexto de competición. Un cero JSON es válido y nunca activa respaldo. No buscar valores en fechas o contextos diferentes. Si únicamente existe scraping confirmado en la fecha seleccionada, usar sus valores disponibles sin exigir JSON. La respuesta distingue ausencia de valor de fallo de consulta y conserva la procedencia efectiva por métrica, sin prescribir rutas nuevas ni almacenamiento.
- **FR-042**: En cada categoría de Todas las estadísticas, ordenar disponibles por Principal → Secundaria → Poco relevante, y después las métricas sin datos. Dentro de cada nivel mantener el orden de la matriz canónica siguiente. Conservar categoría y clasificación de relevancia aun cuando falte el valor; «Sin datos» es disponibilidad, no una reclasificación deportiva permanente (US5).
- **FR-043**: Todas las estadísticas incluye las 60 claves incluso para un jugador sin estadísticas. Mostrar «Sin datos» por valor ausente; carga y error de consulta conservan sus estados propios y no se presentan como ausencia confirmada. Una métrica numérica cero sigue siendo disponible (US5).
- **FR-044**: La relevancia de las tres claves actualmente ausentes es fija según la matriz, independiente de su disponibilidad. Al aparecer valores reales, incluido cero, presentar el valor y aplicar automáticamente su prioridad en Todas las estadísticas, sin cambiar tarjetas, gráficos ni adicionales del Resumen (US5).
- **FR-045**: Identificar la competición del snapshot con nombre legible solo cuando exista una correspondencia fiable de su clave histórica con información disponible. En ausencia de nombre mostrar «Competición no disponible», conservando la clave como contexto, sin inventar nombres, consultar proveedores ni usar la liga actual del jugador como sustituto. El nombre de la liga actual de identidad permanece separado del contexto estadístico (US2).
- **FR-046**: En identidad y estadísticas, formato de ID inválido (no entero decimal positivo o fuera del rango Long) recibe 400; ID válido inexistente o no visible, 404 indistinguible. Jugador visible sin estadísticas para la consulta recibe respuesta estadística exitosa con estado NO_DATA y 60 ausencias, no 404. Es contrato requerido para la implementación futura, no afirmación de que la identidad existente ya valide cero/negativos (US1, US5).
- **FR-047**: Ante fallo de actualización, conservar únicamente la última respuesta exitosa del mismo jugador y temporada resuelta, con sus valores, disponibilidad y fecha de extracción originales, mostrando el error y «Mostrando la última consulta disponible; no se pudo actualizar» con reintento. El error no convierte valores en Sin datos ni los etiqueta como recién obtenidos. Si no existe respuesta previa compatible, presentar error sin estadísticas; nunca usar datos de otro jugador o temporada. Si el contexto pedido cambia, retirar la respuesta incompatible. Una respuesta exitosa NO_DATA sustituye la anterior: no conservar valores antiguos para ocultar la ausencia nueva (US2).
- **FR-048**: Las etiquetas, categorías y unidades visibles de las 60 claves son las del [glosario canónico de presentación](contracts/metric-glossary.md). El glosario no añade métricas ni cálculos; PERCENTAGE se presenta como porcentaje ya persistido, no se multiplica por 100. Las prioridades continúan en la matriz posicional (US5).

### Perfiles posicionales del Resumen

Cada lista está ordenada por prioridad de presentación. Las cuatro tarjetas son fijas para el perfil, sin sustitución automática ante ausencia. Las estadísticas adicionales no repiten las tarjetas. Para posición desconocida se conserva el estado informativo existente y se muestran minutos, apariciones y calificación si existen, sin inventar un perfil.

| Posición | Cuatro tarjetas, en orden | Gráficos específicos: barras con valores originales | Estadísticas adicionales, en orden |
| --- | --- | --- | --- |
| Arquero | `saves_per_match` (Atajadas/partido); `pass_completion_percentage` (Precisión de pases, %); `successful_long_balls_per_match` (Pases largos acertados/partido); `whoscored_rating_average` (Calificación media) | Atajadas por zona: `six_yard_box_saves_per_match`, `penalty_area_saves_per_match`, `outside_box_saves_per_match`. Distribución: `accurate_short_passes_per_match`, `inaccurate_short_passes_per_match`, `successful_long_balls_per_match`, `inaccurate_long_balls_per_match` | Principales: `minutes_played_total`, `appearances_total`. Secundarias: `clearances_per_match`, `aerial_duels_won_per_match`, `passes_per_match`, `yellow_cards_total`, `red_cards_total` |
| Defensor | `tackles_per_match` (Entradas/partido); `interceptions_per_match` (Intercepciones/partido); `clearances_per_match` (Despejes/partido); `aerial_duels_won_per_match` (Duelos aéreos ganados/partido) | Acciones defensivas: `tackles_per_match`, `interceptions_per_match`, `clearances_per_match`, `blocked_shots_per_match`, `blocked_crosses_per_match`, `blocked_passes_per_match`. Duelos aéreos: `aerial_duels_won_per_match`, `aerial_duels_lost_per_match` | Principales: `minutes_played_total`, `appearances_total`, `whoscored_rating_average`, `pass_completion_percentage`, `dribbled_past_per_match`, `attempted_tackles_per_match`. Secundarias: `fouls_committed_per_match`, `yellow_cards_total`, `red_cards_total` |
| Mediocampista | `passes_per_match` (Pases/partido); `pass_completion_percentage` (Precisión de pases, %); `key_passes_per_match` (Pases clave/partido); `assists_total` (Asistencias, total temporada) | Distribución: `accurate_short_passes_per_match`, `inaccurate_short_passes_per_match`, `successful_long_balls_per_match`, `inaccurate_long_balls_per_match`. Creación: `long_key_passes_per_match`, `short_key_passes_per_match` | Principales: `minutes_played_total`, `appearances_total`, `whoscored_rating_average`, `assists_per_match`, `successful_dribbles_per_match`, `tackles_per_match`, `interceptions_per_match`, `dispossessed_per_match`, `bad_controls_per_match`. Secundarias: `goals_total`, `fouls_committed_per_match` |
| Delantero | `goals_total` (Goles, total temporada); `assists_total` (Asistencias, total temporada); `shots_per_match` (Tiros/partido); `whoscored_rating_average` (Calificación media) | Tiros por zona: `six_yard_box_shots_per_match`, `penalty_area_shots_per_match`, `outside_box_shots_per_match`. Asistencias por tipo: `cross_assists_per_match`, `corner_assists_per_match`, `through_ball_assists_per_match`, `free_kick_assists_per_match`, `throw_in_assists_per_match`, `other_assists_per_match` | Principales: `minutes_played_total`, `appearances_total`, `goals_per_match`, `assists_per_match`, `key_passes_per_match`, `successful_dribbles_per_match`, `aerial_duels_won_per_match`, `offsides_per_match`. Secundarias: `pass_completion_percentage`, `yellow_cards_total`, `red_cards_total` |

Los gráficos no suman categorías, no generan porcentajes y no comparan contra otras personas. Un desglose por zona no es mapa de eventos. Las atajadas representan actividad, no eficacia ni goles evitados. Datos parciales muestran solo barras conocidas con aviso de incompletitud; si todos los valores conocidos son cero se muestran explícitamente como cero, no como gráfico sin datos.

### Catálogo completo y prioridad posicional

P = Principal; S = Secundaria; B = Poco relevante. La prioridad es deportiva y no depende de la disponibilidad: las tres claves actualmente ausentes conservan su prioridad en esta matriz, pero se presentan como «Sin datos» hasta disponer de valores reales. Los nombres y unidades mantienen el significado del catálogo canónico de SPEC-012: totales de temporada, tasas por partido, porcentaje y calificación media. No se reconstruyen totales desde tasas.

| Categoría | Clave canónica | Arquero | Defensor | Mediocampista | Delantero |
| --- | --- | --- | --- | --- | --- |
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

Disponibilidad observada en la consulta PostgreSQL del análisis previo: 950 registros JSON de 2026-10-23, temporada 2026/2027; 58 arqueros, 332 defensores, 276 mediocampistas y 284 delanteros. Cada registro presenta 57 valores no nulos; `starts_total`, `shots_on_target_total` y `second_yellow_total` están ausentes. Las demás claves incluyen ceros legítimos. Esta evidencia no garantiza cobertura futura ni existencia de estadísticas para todo el catálogo. La interfaz usa la disponibilidad de cada jugador consultado, no estos conteos como constantes.

### Key Entities

- **Jugador**: identidad local y posición; datos personales e imágenes opcionales.
- **Contexto estadístico**: jugador, temporada, liga, extracción y procedencia que delimitan una consulta coherente.
- **Métrica canónica**: clave de SPEC-012, nombre legible, categoría y unidad; valor real opcional.
- **Perfil posicional**: selección de indicadores y visualizaciones relevante a una de las cuatro posiciones existentes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-012**: En selección desde catálogo, acceso directo y recarga, el 100 % de las identidades corresponde al jugador autorizado; alternar dos jugadores con respuestas inversas no mezcla datos (FR-025, FR-037).
- **SC-013**: Ambas pestañas son las únicas disponibles; sin filtros el catálogo contiene 60 claves distintas, sin duplicados, y la búsqueda/categoría devuelve exactamente las coincidencias esperadas (FR-026, FR-033–035).
- **SC-014**: Para un jugador de cada posición con datos, el 100 % de valores visibles coincide con el contexto persistido seleccionado; cero y ausencia se distinguen en todos los casos revisados (FR-027–030, FR-041).
- **SC-015**: Para las cuatro posiciones, cada gráfico dispone de etiquetas, unidades y alternativa textual; datos insuficientes nunca generan puntos, barras o comparativas ficticias (FR-030–032, FR-036, FR-039).
- **SC-016**: Carga, error, sin datos y datos parciales producen cuatro estados distinguibles y mantienen navegación; un reintento no sustituye fallos por mocks (FR-029, FR-037).
- **SC-017**: A 360, 768 y 1280 píxeles de ancho y alturas de 640 y 800, las dos pestañas y sus controles son accesibles sin desbordamiento horizontal de página ni contenido inaccesible por recorte (FR-038–039).
- **SC-018**: Con estadísticas persistidas y sin valoraciones/cotizaciones, se pueden consultar las dos pestañas sin ejecutar procesos de actualización ni mostrar bloques financieros (FR-027, FR-040).
- **SC-019**: Para cada una de las cuatro posiciones, las cuatro tarjetas coinciden exactamente con su perfil; gráficos y adicionales usan solo las claves listadas, sin cálculos nuevos ni valores de relleno (FR-030–032).
- **SC-020**: Las ocho categorías contienen respectivamente 5, 19, 10, 5, 3, 10, 4 y 4 claves únicas, sumando 60. Para cada posición, el orden respeta la matriz; ausencias permanecen visibles al final y los ceros no se desplazan como ausentes (FR-033–035, FR-042–043).
- **SC-021**: Para cada posición, al pasar cualquiera de las tres claves de ausente a valor real (incluido cero), Todas las estadísticas muestra el valor y lo ordena según la matriz sin modificar ninguna tarjeta o gráfico del Resumen (FR-044).
- **SC-022**: Con dos fechas confirmadas del mismo jugador y temporada se selecciona la más reciente, independientemente de las campañas de otros jugadores. Los empates dentro de un canal/contexto respetan observación e identificador; JSON cero no se sustituye, JSON nulo admite respaldo coincidente y scraping de fecha o contexto distinto nunca completa la consulta (FR-041).
- **SC-023**: El contexto estadístico ofrece nombre fiable o «Competición no disponible», sin reemplazo por liga actual; los 60 nombres/categorías/unidades coinciden con el glosario. ID inválido, jugador no encontrado y ausencia estadística están diferenciados en todos los escenarios correspondientes (FR-045/046/048).
- **SC-024**: Ante error de actualización se conserva solo la respuesta previa compatible, con fecha original, advertencia y reintento; cambio de jugador/temporada no muestra esa respuesta, y una consulta exitosa NO_DATA reemplaza los valores anteriores (FR-047).

## Assumptions

- La sesión, navegación e identidad existentes se conservan; no se añaden roles ni gestión de sesión.
- El mockup no autoriza selector de temporadas, percentiles en cada fila ni métricas nuevas. Se presenta el contexto seleccionado; el selector queda fuera de este rediseño salvo decisión posterior.
- No se exige que todos los jugadores tengan las 60 métricas; el catálogo completo es distinto de la cobertura real.
- Los controles de calidad de producto descritos son criterios futuros, no comprobaciones ejecutadas por el agente.

## Dependencies and Pending Integration

- SPEC-012 aporta catálogo y estadísticas persistidas. La consulta individual debe proporcionar contexto, disponibilidad, procedencia y errores distinguibles; su contrato concreto corresponde a PLAN.
- La selección individual de snapshot y la prioridad JSON con respaldo acotado de scraping están confirmadas en FR-041. No se hereda la selección poblacional usada por valoraciones.
- No se requiere población de comparación, normalización ni resultado de SPEC-013 para presentar métricas o gráficos.
- Se mantienen las capacidades de catálogo, identidad, imágenes, equipos y ligas de SPEC-002/005/006/007. Este trabajo no modifica SPEC-008.
- REQ-S2-12 conserva su trazabilidad; excluir finanzas del dashboard no elimina obligaciones externas ni permite declarar satisfecho el historial financiero exigido por el TP.

## Out of Scope

- Implementación durante Specify; PLAN, TASKS y autorizaciones de implementación.
- Sincronización, scraping, cálculo de valoraciones o cotizaciones y cambios en esas features.
- Compra/venta, historial financiero, ranking, favoritos y nuevas pestañas.
- Mapas de eventos, series por partido y métricas ajenas a las 60 canónicas; cifras del mockup como datos reales.
- xG, xA, percentiles, radares, promedios comparativos e indicadores derivados con cálculos no definidos.
- Eliminación automática de contratos o componentes históricos y rediseño del catálogo.

## Evidencia de Specify — rediseño 2026-10-25

Spec Kit ejecutado con `--reuse --number 009 --short-name player-detail`: `SPEC_ACTION=reused`, `FEATURE_NUM=009`, `SPEC_NAME=009-player-detail`, archivo de esta carpeta. `BRANCH_NAME=feat/009-player-detail`, `BRANCH_STATUS=skipped-dirty-tree`: el script no cambió rama por modificaciones previas. Se conserva la rama autorizada `feat/009-player-detail-whoscored-stats-dashboard`; no se afirma que el script la haya creado. `.specify/feature.json` selecciona esta carpeta y no se observaron overrides de entorno. No existe `.specify/extensions.yml`: sin hooks before/after Specify.

Evaluación documental: estructura y escenarios redactados; FR-032 y FR-041 pendientes. No acredita Clarify, aceptación, implementación ni resultados de producto. Los artefactos históricos no acreditan preparación del alcance nuevo. Siguiente fase: CLARIFY obligatorio, no PLAN.

Actualización de Specify tras el análisis completo: FR-032 resuelto por exclusión expresa de radar y cálculos no definidos; cuatro tarjetas por posición, gráficos con claves persistidas y matriz completa de 60 métricas incorporados. FR-041 continúa pendiente. El texto de evaluación anterior se conserva como evidencia del estado previo, no como pendiente vigente del radar. No se ejecutaron fases posteriores ni se modificó el seguimiento remoto.

Evidencia de CLARIFY — 2026-10-25: tres preguntas formuladas y tres respuestas explícitas incorporadas en la sesión vigente. Resueltos FR-041 y prioridad futura de las tres claves; FR-044 y SC-021/022 verifican el comportamiento. «Confirmada» conserva el significado de SPEC-012: fila publicada atómicamente con item de jugador `COMPLETED`, no mera existencia de una fila ni cobertura poblacional. La temporada seleccionada es el contexto de la consulta; no añade selector de temporadas a la interfaz. El contexto es el de la competición de la liga local resuelto por SPEC-012; no se seleccionan competiciones adicionales. La unicidad por jugador/fecha/canal de SPEC-012 limita empates; observación e ID fijan orden estable sin fusionar registros incompatibles.

Escaneo de alcance, dominio/datos, interacción, calidad no funcional aplicable, integración, excepciones, restricciones, terminología, señales de aceptación y placeholders: claro tras estas respuestas. Rendimiento cuantitativo e instrumentación no se agregan como requisitos; diseño de consulta y contratos concretos corresponde a PLAN. No quedan marcadores abiertos en el alcance vigente. Checklist documental vigente 13/16 → 16/16; no implica aceptación del producto ni extiende aprobaciones históricas. Sin hooks: `.specify/extensions.yml` ausente. No se ejecutaron tests, PLAN, TASKS, implementación, commits, push ni cambios del seguimiento. Siguiente paso permitido bajo un nuevo encargo: `/speckit.plan`.

---

## Antecedente histórico — versión anterior, no normativa para el rediseño

**Feature Branch**: `feat/009-player-detail` (rama local activa; creada posteriormente desde el mismo HEAD de `dev`, conservando los cambios locales. El `BRANCH_STATUS=skipped-dirty-tree` original se conserva como antecedente).

**Created**: 2026-10-12

**Status**: Ready for planning

**Input**: REQ-S2-12: pantalla privada de detalle con identidad real consultable directamente, estadísticas deportivas y cotizaciones del token simuladas, evolución histórica y dashboard responsive. Specify ejecutado; consolidación documental y Clarify autorizados, sin fases posteriores.

## Trazabilidad y asignación de identidad

- Identidad real materializada por Spec Kit: `009-player-detail`, `FEATURE_NUM=009`, `SPEC_ACTION=created`.
- Asignación explícita previa conforme a Constitution v7.0.0 §8: número 009; funcionalidad detalle individual del jugador; responsable integrante encargado del frontend y detalle (usuario solicitante); motivo desarrollo paralelo con scraping asignado a otro integrante como 008.
- Registro y comprobación de colisiones: [índice de trazabilidad](../trello.md#asignación-de-identidad-para-trabajo-paralelo--2026-10-12). La carpeta 008 provisional no constituye una SPEC validada y no se reutiliza.
- Fuentes funcionales: [enunciado §4 y §11](../../tp/enunciado.md), [Sprint 2](../../tp/entregas.md), [SPEC-005](../005-players-page/spec.md), [catálogo](../002-player-catalog/spec.md), [imágenes](../006-player-images/spec.md) y [equipos y ligas](../007-team-league-domain/spec.md).
- Esta SPEC amplía el catálogo de SPEC-005 con navegación al detalle y reemplaza, únicamente para las estadísticas de demostración compartidas, sus valores comunes 24/8/5 por valores deterministas individuales. Originalmente no modificaba los requisitos del catálogo; la decisión de producto del **2026-10-13** amplió expresamente ese alcance para incorporar la navbar global, el catálogo responsive, el rediseño de la tarjeta del catálogo y la paginación compartida, documentados ahora en SPEC-005. No habilita filtros reales ni búsqueda real sobre el catálogo.
- La épica [SPEC-009, antecedente REQ-S2-12](https://trello.com/c/KViJvig1) y SDD Lifecycle están disponibles; su actualización se registra en el índice. Las tarjetas de US siguen pendientes, fuera de esta operación. No se acredita READY ni implementación.

## Clarifications

### Session 2026-10-12

Las cinco respuestas fueron aportadas y confirmadas por el usuario antes del escaneo; no se repreguntaron.

- Q: ¿Qué jugadores permite consultar el detalle? → A: Solo jugadores activos siguiendo la visibilidad del catálogo; un jugador inactivo recibe 404 y “Jugador no encontrado”, sin revelar sus datos. Mantener 400, 401, 404 y errores de comunicación diferenciados.
- Q: ¿Qué temporada utilizan las estadísticas simuladas? → A: Temporada fija 2025/2026; valores dependientes del ID interno y temporada, deterministas, estables y compartidos con catálogo; etiqueta “Datos de demostración”, sin selector ni cambio automático.
- Q: ¿Qué historial de demostración presenta el token? → A: Treinta días consecutivos, una cotización diaria en créditos, período fijo independiente del reloj del navegador y valores específicos estables por jugador; precio/fecha actuales iguales al último punto y variación respecto del anterior. Conservar vacío, punto único y base cero; sin endpoints financieros nuevos.
- Q: ¿Qué reemplaza al radar? → A: Tarjeta compacta “Rendimiento deportivo” con partidos, goles, asistencias, minutos, temporada 2025/2026 y “Datos de demostración”, coherente con los demás resúmenes; sin radar, percentiles, barras normalizadas o nuevas métricas.
- Q: ¿Cómo será la navegación? → A: Navbar horizontal superior, marca a la izquierda, Inicio/Jugadores funcionales y sección activa, usuario autenticado a la derecha; breadcrumb debajo con retorno a Jugadores. Menú desplegable accesible en tablet/móvil. Intención global reutilizable, aplicada en esta SPEC solo al detalle, sin rediseñar otras páginas. **Antecedente sustituido el 2026-10-13**: la limitación «aplicada en esta SPEC solo al detalle» queda derogada; la navbar se aplica a todas las rutas privadas según la sesión de decisiones del 2026-10-13 y FR-018 vigente. El resto de la respuesta se conserva.

### Session 2026-10-13

Decisiones de producto confirmadas por el responsable del proyecto tras una auditoría de trazabilidad entre el código y esta SPEC. **Amplían** el alcance acordado el 2026-10-12 y sustituyen las restricciones correspondientes; todo lo demás de esta SPEC se conserva sin cambios.

- Q: ¿Se conserva el rediseño manual del frontend? → A: Sí. Se conservan todos los cambios manuales realizados en el frontend, sin revertir ni reemplazar.
- Q: ¿Dónde se aplica la navbar horizontal? → A: A **todas las rutas privadas** (`/home`, `/players`, `/players/:id`), no solo al detalle. Sustituye la restricción de aplicar la navegación únicamente en esta pantalla.
- Q: ¿El catálogo de jugadores pasa a ser responsive? → A: Sí. Sustituye la restricción de escritorio exclusivo del catálogo.
- Q: ¿Qué decide el rediseño de la tarjeta y la paginación del catálogo? → A: Se conservan el nuevo diseño de la tarjeta y la paginación compartida con ventana ±1, que sustituye la ventana de tres anteriores y tres posteriores.
- Q: ¿Qué recursos gráficos pueden usarse en runtime? → A: Se distinguen **recursos locales** (escudos de equipo importados en tiempo de build) y **servicios externos** (banderas de nacionalidad). Ambos se documentan como dependencias de recursos, no como contratos de datos.

**Efecto sobre requisitos externos obligatorios**: la ampliación del catálogo a responsive **corrige** una divergencia preexistente con `tp/enunciado.md` §11, que exige que el frontend sea «una aplicación web responsiva». La restricción de escritorio exclusivo de SPEC-005 FR-006 era incompatible con ese requisito externo. La decisión no elimina ningún requisito del enunciado.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar la identidad real del jugador (Priority: P1)

Como usuario autenticado, quiero abrir el detalle de un jugador desde el catálogo o su URL para consultar su identidad real y regresar al catálogo.

**Why this priority**: Entrega el detalle básico sin depender de datos deportivos o financieros simulados.

**Independent Test**: Seleccionar un jugador real, abrir su URL en una sesión autenticada nueva, recargar y comparar identidad; repetir con visitante, ID inexistente y fallo de comunicación.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado en el catálogo, **When** selecciona un jugador, **Then** accede a `/players/:id` con su ID interno y ve su identidad obtenida del backend.
2. **Given** la URL de un jugador activo visible según el catálogo y sesión válida, **When** abre directamente o recarga la página sin caché del catálogo, **Then** obtiene la identidad real y no depende del estado de navegación anterior.
3. **Given** información disponible, **When** se presenta el detalle, **Then** muestra nombre, equipo, liga, posición, nacionalidad y fecha de nacimiento disponibles, sin inventar atributos ausentes; intenta imagen principal, alternativa y finalmente icono genérico.
4. **Given** una consulta pendiente o fallida, **When** el usuario observa la página, **Then** ve carga o error comprensible con reintento, sin identidad ficticia ni datos de otro jugador.
5. **Given** un ID válido inexistente o correspondiente a un jugador inactivo, **When** termina la consulta, **Then** recibe 404, ve “Jugador no encontrado” y regreso al catálogo, sin identidad ni bloques simulados atribuidos a ese ID y sin distinguir la existencia de un inactivo.
6. **Given** un visitante o una sesión vencida, **When** intenta consultar el detalle, **Then** no accede a información privada y se aplica el flujo de acceso existente.
7. **Given** que cambia de A a B y A responde tarde, **When** se resuelven las consultas, **Then** solo se presenta información correspondiente a B.
8. **Given** el detalle o sus estados de error, **When** activa “Volver al catálogo”, **Then** llega a `/players` mediante navegación funcional.
9. **Given** un ID inválido, autenticación inválida, un jugador no visible o un fallo de comunicación, **When** se consulta el detalle, **Then** se diferencian 400, 401, 404 y fallo de comunicación según las convenciones existentes; no se sustituye ninguno por identidad simulada.

---

### User Story 2 - Consultar estadísticas de demostración coherentes (Priority: P2)

Como usuario, quiero consultar partidos, goles, asistencias y minutos de la temporada fija 2025/2026, sabiendo que son demostrativos.

**Why this priority**: Permite demostrar el resumen deportivo mientras las estadísticas reales no están disponibles.

**Independent Test**: Comparar al menos dos jugadores y repetir navegación/recarga; contrastar las métricas comunes con sus tarjetas de catálogo.

**Acceptance Scenarios**:

1. **Given** un jugador real activo cargado, **When** consulta la tarjeta compacta “Rendimiento deportivo”, **Then** ve Partidos, Goles, Asistencias y Minutos, temporada 2025/2026 y rótulo “Datos de demostración”, sin radar, percentiles, barras normalizadas ni métricas adicionales.
2. **Given** el mismo ID y temporada, **When** recarga o vuelve a abrir el detalle, **Then** recibe exactamente los mismos valores.
3. **Given** jugadores distintos, **When** compara resúmenes, **Then** las estadísticas dependen de sus IDs y no son un bloque fijo común; la combinación de valores de la muestra comparada difiere.
4. **Given** un jugador del catálogo, **When** compara catálogo y detalle para la misma temporada, **Then** partidos, goles y asistencias coinciden y están identificados como demostración en ambas pantallas.
5. **Given** estadísticas con valor cero de un jugador sin participación simulada, **When** se presentan, **Then** cero no se interpreta como error ni ausencia de identidad.
6. **Given** el mismo jugador, **When** cambia el día o año del navegador o recarga, **Then** mantiene temporada 2025/2026 y los mismos valores; no existe selector de temporadas.
7. **Given** “Rendimiento deportivo” y los demás resúmenes del detalle, **When** compara sus estadísticas, **Then** coinciden exactamente; también coinciden las métricas compartidas con el catálogo.

---

### User Story 3 - Consultar cotización y evolución de demostración (Priority: P2)

Como usuario, quiero ver el precio simulado del token y su evolución para comprender la pantalla financiera sin confundirla con cotizaciones reales o valores de transferencia.

**Why this priority**: Demuestra la evolución requerida, haciendo explícita su limitación de integración.

**Independent Test**: Revisar fechas, precios, último punto y variación para dos jugadores; repetir después de recargar.

**Acceptance Scenarios**:

1. **Given** un jugador real, **When** observa la cotización, **Then** ve precio del token en créditos, fecha, variación y “Datos de demostración”.
2. **Given** el historial normal de demostración, **When** examina el gráfico, **Then** ve treinta cotizaciones, una por cada día consecutivo de un período fijo, ordenadas cronológicamente en créditos; el último punto coincide exactamente con la cotización actual y su fecha.
3. **Given** al menos dos cotizaciones, **When** consulta variación, **Then** su signo y magnitud corresponden a las dos últimas cotizaciones y la comparación está identificada.
4. **Given** el mismo jugador, **When** recarga, vuelve a navegar o cambia el reloj del navegador, **Then** mantiene la cotización de cada uno de los treinta días y su precio, específicos de ese jugador y sin aleatoriedad por renderizado.
5. **Given** una serie vacía o de un solo punto, **When** se presenta, **Then** informa ausencia de historial o muestra el único valor, respectivamente, sin inventar evolución ni variación.
6. **Given** los bloques financieros, **When** los interpreta, **Then** se identifican como token en créditos, nunca valor de transferencia ni precio operativo real.

---

### User Story 4 - Utilizar un dashboard adaptable con navegación real (Priority: P2)

Como usuario, quiero una pantalla oscura cercana a la referencia que preserve la jerarquía del jugador y sea utilizable en escritorio, tablet y móvil.

**Why this priority**: Hace accesible y legible el detalle y sus resúmenes en los dispositivos requeridos.

**Independent Test**: Comparar estructura con la referencia en escritorio y revisar a 1280, 768 y 360 píxeles de ancho; activar todos los controles visibles mediante teclado y puntero.

**Acceptance Scenarios**:

1. **Given** una pantalla de escritorio, **When** abre el detalle, **Then** ve navbar horizontal superior y breadcrumb debajo, ficha principal azul con fotografía, tarjetas oscuras, “Rendimiento deportivo” superior derecho en el lugar del radar, evolución inferior izquierda y estadísticas inferiores derechas, sin barra lateral.
2. **Given** la ficha principal, **When** la observa, **Then** imagen e identidad son dominantes y la franja de indicadores resume partidos, goles, asistencias y cotización sin contradecir los bloques detallados.
3. **Given** la navbar en escritorio, **When** la utiliza, **Then** encuentra marca Football Market a la izquierda, Inicio (`/home`) y Jugadores (`/players`) funcionales, Jugadores identificado como sección activa y sector de usuario autenticado a la derecha, con las capacidades de identidad y cierre de sesión existentes, sin acciones decorativas.
4. **Given** tablet o móvil, **When** utiliza el menú desplegable, **Then** puede abrirlo, navegar y cerrarlo mediante controles reales accesibles por teclado, con estado abierto/cerrado perceptible; navegación, tarjetas y gráfico se reorganizan sin desplazamiento horizontal y conservan identidad, cifras y unidades legibles.
5. **Given** una persona que navega con teclado o no distingue colores, **When** usa el detalle, **Then** dispone de foco visible, textos para los estados y valores históricos accesibles sin depender solo del dibujo.
6. **Given** el jugador cargado, **When** observa el breadcrumb bajo la navbar, **Then** ve “Jugadores > nombre real”, y “Jugadores” regresa a `/players`; si no se resolvió la identidad, no aparece un nombre ficticio o del jugador anterior.
7. **Given** esta navegación concebida como reutilizable, **When** se abre cualquier ruta privada autenticada, **Then** presenta la misma navbar horizontal; `/home`, `/players` y `/players/:id` comparten esa capacidad sin divergir entre sí. Este comportamiento sustituye, por decisión de producto del 2026-10-13, la limitación anterior que la restringía al detalle.

### Edge Cases

- ID no numérico, no entero positivo o fuera del rango admitido: informar identificador inválido, sin consultar ni inventar otro jugador. ID válido no encontrado se distingue de errores de comunicación.
- Jugador persistido inactivo o no elegible según la visibilidad del catálogo: responder 404 y “Jugador no encontrado”, sin exponer identidad, estado de inactividad ni simulaciones. La consulta solo permite jugadores activos visibles según esa política.
- Opcionales nulos: “No disponible” u omisión del dato; nunca convertirlos en datos simulados. Nombres largos no invaden tarjetas vecinas.
- Imágenes ausentes o fallidas: seguir el orden de fallback y mantener identidad textual; no consultar proveedores deportivos desde la pantalla.
- Cambio rápido de ID o respuesta tardía: no mezclar identidad, temporada, estadísticas o precios de jugadores diferentes.
- Error o sesión vencida: no sustituir la identidad real por mocks ni mostrar cotizaciones de demostración como respuesta del backend.
- Historial vacío, punto único y precio anterior cero: sin porcentaje indefinido; indicar variación porcentual no disponible si no puede calcularse.
- Crecimiento positivo, negativo o nulo: signo y etiqueta textual coherentes, no solo color.
- Cambio del reloj del navegador: no modifica la temporada 2025/2026 ni las fechas del período financiero fijo. Las series vacías o de punto único son estados excepcionales, no la muestra normal de treinta días.
- Durante carga, error o 404, el breadcrumb conserva retorno al catálogo sin inventar el nombre; la navegación no presenta acciones de usuario no disponibles.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ofrecer `/players/:id` como pantalla privada bajo el sistema de sesión existente y rechazar consultas backend sin autenticación vigente.
- **FR-002**: Permitir abrir el detalle desde cada tarjeta del catálogo con el ID interno, acceder directamente y recargar sin depender de la caché o estado anterior del catálogo.
- **FR-003**: Incorporar como único endpoint nuevo de este alcance `GET /api/players/{id}` (ruta conceptual `/players/:id`), conservando el prefijo real `/api`, autenticación y contrato global de errores existentes. Solo permite jugadores activos con la misma visibilidad del catálogo. ID válido inexistente, inactivo o no visible según esa política debe responder 404 sin revelar la identidad; ID inválido, 400; ausencia de autenticación válida, 401. Distinguir estos estados de errores de comunicación. No se definen aquí nuevos códigos de error ni payloads futuros no confirmados.
- **FR-004**: Obtener la identidad real del jugador mediante esa consulta individual local, sin iniciar scraping, sincronizaciones o consultas a proveedores externos.
- **FR-005**: Mantener los conceptos del catálogo: ID, nombre, equipo y liga con IDs y nombres internos, posición, fecha de nacimiento, nacionalidad e imágenes opcionales. No exponer identidades de proveedores ni fabricar información personal, física o contractual.
- **FR-006**: Mostrar opcionales solo cuando existan; usar omisión o “No disponible” para ausentes y preservar retrato principal → alternativa → icono genérico ante fallos de carga.
- **FR-007**: Comunicar carga, error recuperable con reintento, ID inválido y jugador no encontrado como estados distintos, permitiendo regresar al catálogo.
- **FR-008**: Ante cambio de ID, impedir presentar datos del jugador anterior o mezclar respuestas tardías; las simulaciones solo se presentan después de resolver una identidad real autorizada.
- **FR-009**: Mostrar partidos, goles, asistencias y minutos simulados asociados a la temporada fija y visible 2025/2026; números enteros no negativos y sin minutos positivos cuando los partidos sean cero. No incluir selector ni cambio automático de temporada.
- **FR-010**: Obtener estadísticas estables a partir del ID y temporada de demostración, sin aleatoriedad por apertura, renderizado o recarga ni valores fijos comunes para todo el catálogo. No exigir unicidad matemática de cada métrica individual.
- **FR-011**: Mantener igualdad de las estadísticas comunes de catálogo y detalle para el mismo ID y temporada, reemplazando los valores comunes de muestra de SPEC-005 y señalando su naturaleza demostrativa también en el catálogo.
- **FR-012**: Centralizar las fuentes de estadísticas y cotizaciones simuladas en el frontend y separar su procedencia de la identidad real, permitiendo sustituirlas posteriormente sin redefinir la presentación funcional; no establecer contratos HTTP ficticios como confirmados.
- **FR-013**: Mostrar el precio actual simulado del token en créditos, fecha y variación respecto del punto anterior; distinguirlo del valor de transferencia y de cotizaciones operativas reales.
- **FR-014**: Mostrar en el caso normal treinta cotizaciones simuladas por jugador, una por día consecutivo, con fechas cronológicamente ordenadas y ancladas a un período de demostración fijo independiente del reloj del navegador. Los precios en créditos, no negativos y específicos del jugador, y las fechas permanecen estables entre recargas. Precio y fecha actuales coinciden con el último punto. La variación absoluta y porcentual se derivan de las dos últimas cotizaciones; sin historial o con un solo punto no hay variación y con precio anterior cero no hay porcentaje definido. Conservar estos estados excepcionales sin inventar datos.
- **FR-015**: Identificar de forma visible cada bloque deportivo y financiero simulado como “Datos de demostración”; no etiquetar la identidad real como simulada ni ocultar procedencia al reorganizar la pantalla.
- **FR-016**: Mantener estructura y jerarquía próximas a `referencias-imagenes/football_market_referencia_dashboard.png`: entorno oscuro, ficha azul dominante, fotografía izquierda, datos principales, franja de indicadores y tarjetas superiores/inferiores. La excepción visual acordada reemplaza la barra lateral por navbar horizontal superior y breadcrumb. Adaptar contenido a Football Market sin copiar funcionalidades ajenas.
- **FR-017**: Conservar el lugar superior derecho del radar con una tarjeta compacta titulada “Rendimiento deportivo”: Partidos, Goles, Asistencias, Minutos, temporada 2025/2026 y “Datos de demostración”. Todos los valores coinciden con los demás resúmenes y métricas compartidas del catálogo. No dibujar radar, percentiles, barras normalizadas ni métricas deportivas adicionales.
- **FR-018**: Incluir navbar horizontal superior en **todas las rutas privadas** (`/home`, `/players`, `/players/:id`): marca o nombre Football Market a la izquierda, Inicio (`/home`) y Jugadores (`/players`) funcionales, indicación accesible de la sección activa y sector de usuario autenticado a la derecha que reutiliza identidad y cierre de sesión existentes sin crear perfil, avatar ni acciones nuevas. En el detalle, debajo de la navbar, breadcrumb “Jugadores > nombre real” con retorno a `/players`, sin nombre ficticio en carga/error. En tablet y móvil utilizar menú desplegable operable por teclado, con apertura/cierre reales y estado perceptible. No mostrar seguimiento, compra, venta, noticias, filtros o selectores sin comportamiento definido. La navegación es una capacidad global reutilizable compuesta desde `app`, conforme a la arquitectura vigente. Sustituye, por decisión de producto del 2026-10-13, la redacción anterior que la limitaba a esta pantalla.
- **FR-019**: Adaptar el detalle a escritorio, tablet y móvil; reorganizar tarjetas y navegación sin desbordamiento horizontal a 360, 768 y 1280 píxeles de ancho, preservando jerarquía, unidades y etiquetas.
- **FR-020**: Permitir navegación por teclado, foco visible, alternativas textuales para imágenes y presentación accesible de fechas/precios del gráfico, sin depender exclusivamente de color.
- **FR-021**: Documentar la integración futura de estadísticas y cotizaciones como pendiente; sus contratos se acordarán con el backend y no se presentan como APIs existentes. El historial conceptual necesita identificar jugador, fechas, precios en créditos y procedencia; parámetros, DTOs, precisión y errores quedan para el diseño acordado.
- **FR-022**: Declarar cumplimiento parcial de REQ-S2-12: este alcance no consume historial real ni implementa `GET /api/players/{id}/quotes`. No declarar satisfecho el requisito académico completo por la demostración o por el cierre de esta SPEC.
- **FR-023**: Permitir que la identidad real se acompañe de recursos gráficos decorativos que no forman parte del contrato: escudo del equipo resuelto desde un registro local de escudos importados en build y bandera de nacionalidad resuelta desde un servicio externo de banderas, ambos con marcador de posición accesible cuando no exista coincidencia o falle la carga. Estos recursos no amplían el contrato HTTP, noAlteran los datos del jugador y no se presentan como información de la fuente de datos.
- **FR-024**: Componer el detalle y el catálogo sobre los layouts y componentes compartidos vigentes: layout de aplicación con navegación, layout de página y layout paginado. Las capacidades transversales nuevas (navegación global, paginación compartida, capa de estilos y tokens) son reutilizables y no pertenecen a esta feature.

### Key Entities

- **Jugador**: Identidad interna real con nombre, posición, equipo y liga actuales; datos personales e imágenes opcionales. No contiene métricas ficticias persistidas como reales.
- **Resumen deportivo de demostración**: ID del jugador, temporada, partidos, goles, asistencias, minutos y procedencia simulada.
- **Cotización del token de demostración**: Jugador, fecha, precio en créditos y procedencia; no equivale a valor de transferencia.
- **Serie histórica de demostración**: Treinta cotizaciones diarias consecutivas del mismo token en un período fijo, que determinan cotización actual y variación; vacío y punto único son estados excepcionales.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de los escenarios de selección, acceso directo y recarga de un jugador existente autorizado, se presenta su identidad real correcta sin necesidad de visitar previamente el catálogo (US1; FR-002–006).
- **SC-002**: En el 100 % de los casos de visitante/sesión vencida, no se revela el detalle privado; IDs inválidos (400), inexistentes/inactivos/no visibles (404), autenticación inválida (401) y errores de comunicación se distinguen sin inventar identidad ni revelar al inactivo (US1; FR-001, FR-003, FR-007).
- **SC-003**: Al alternar rápidamente entre dos IDs y resolver respuestas en orden inverso, ningún bloque visible se atribuye al jugador incorrecto (US1; FR-008).
- **SC-004**: Para al menos dos jugadores de muestra, tres recargas y dos recorridos catálogo–detalle mantienen exactamente las métricas comunes de 2025/2026 y los valores repetidos en “Rendimiento deportivo”; los resúmenes no son iguales para todos los jugadores revisados y ningún cambio del reloj modifica la temporada (US2; FR-009–012, FR-017).
- **SC-005**: El 100 % de los bloques simulados revisados muestra su etiqueta de demostración, temporada deportiva o fecha financiera y unidades pertinentes (US2–3; FR-015).
- **SC-006**: Para dos jugadores, tres recargas y un cambio de fecha del navegador conservan exactamente treinta días consecutivos con una cotización diaria por día, específica de cada jugador; último precio/fecha y variación coinciden con el historial en el 100 % de las comprobaciones, incluidos crecimiento, caída y estabilidad (US3; FR-013–014).
- **SC-007**: Los estados de historial vacío, punto único y base cero no muestran una evolución o porcentaje inexistente (US3; FR-014).
- **SC-008**: A 360, 768 y 1280 píxeles de ancho, todo contenido del detalle es utilizable sin desplazamiento horizontal; a 360 y 768 la navegación usa menú desplegable y en escritorio se reconocen las cuatro zonas principales, navbar superior y breadcrumb; no hay barra lateral (US4; FR-016–019).
- **SC-009**: El 100 % de los controles visibles tiene comportamiento definido y acceso por teclado, incluidos enlaces Inicio/Jugadores, retorno del breadcrumb, cierre de sesión y apertura/cierre del menú; la sección actual aparece activa en cada ruta privada y la evolución puede comprenderse mediante sus fechas y valores textuales (US4; FR-018, FR-020).
- **SC-011**: En la revisión de las tres rutas privadas, el 100 % de ellas presenta la misma navbar horizontal con marca, enlaces funcionales, sección activa y sector de usuario, sin divergencias entre pantallas (US4; FR-018).
- **SC-010**: La documentación identifica REQ-S2-12, la ampliación específica de SPEC-005, las fuentes reales/simuladas y la integración pendiente de cotizaciones, sin atribuir cumplimiento completo del requisito original (FR-021–022).

## Assumptions

- Se reutilizan sesión, catálogo, arquitectura y convenciones vigentes; no se elige nueva librería gráfica ni estructura técnica en Specify.
- La temporada de demostración es fija: 2025/2026, visible y común a catálogo y detalle; los datos no afirman rendimiento real de esa temporada. No hay selector ni actualización automática.
- El período financiero normal contiene una cotización diaria durante treinta días consecutivos con fechas fijas. La elección concreta del inicio del período se documentará en el diseño posterior, no se derivará del reloj del navegador y no se exige que coincida con la temporada deportiva 2025/2026; no agrega una nueva decisión de producto ni bloquea la claridad funcional.
- La variación financiera compara las dos últimas fechas de la serie de demostración, no transferencias ni rentabilidad personal.
- Las simulaciones permanecen estables para el mismo ID y temporada/conjunto de demostración; su modificación futura será explícita, no consecuencia de la fecha del navegador.
- Regresar al catálogo garantiza el destino `/players`; conservar página y filtros previos no es requisito de este alcance.
- La forma concreta de los contratos futuros corresponde al diseño posterior y acuerdo de integración; no se genera Plan en esta operación.
- La navbar es una capacidad global reutilizable, compuesta desde `app` y aplicada a todas las rutas privadas (`/home`, `/players`, `/players/:id`). Su adopción en el resto de las rutas privadas es consecuencia de la decisión de producto del 2026-10-13 y no requiere un alcance adicional; no altera las excepciones de catálogo de FR-002/011, que siguen vigentes.
- Los escudos de equipo se sirven como recursos locales importados en tiempo de build y las banderas de nacionalidad se obtienen de un servicio externo en runtime. Ninguno de los dos constituye un contrato de datos ni amplía el endpoint individual.

## Dependencies and Pending Integration

- Catálogo, autenticación y datos de SPEC-002/005/006/007; la consistencia de identidad no presupone que los datos del backend sean inmutables entre consultas.
- El endpoint individual real es implementación requerida de esta SPEC, no implementación ya existente. La elegibilidad está resuelta: misma visibilidad del catálogo activo; inactivos reciben 404 sin revelar identidad.
- Estadísticas reales y cotizaciones reales pertenecen a integración posterior; `GET /api/players/{id}/quotes` permanece expresamente pendiente.
- Conforme al enunciado §4 y §11, los mocks no satisfacen la consulta backend de evolución: el cumplimiento de REQ-S2-12 es parcial aun cuando se entregue esta versión demostrativa.
- SPEC-008 es trabajo paralelo de scraping, no prerrequisito de esta versión basada en identidad local y muestras frontend. No se asume validada ni integrada.
- Reutilizar las capacidades de identidad autenticada y cierre de sesión existentes. La navegación global se compone en `app` y se aplica a todas las rutas privadas desde la decisión de producto del 2026-10-13.
- Pendientes del flujo: evaluación de checklists por el revisor, Tasks, Analyze y demás gates; tarjetas de US pendientes. Specify, Clarify y Plan evaluados documentalmente, rama disponible; no se habilita implementación ni READY.

## Out of Scope

- Scraping y cambios a SPEC-008.
- Estadísticas deportivas reales o nuevos endpoints deportivos.
- Cotizaciones reales e implementación de `GET /api/players/{id}/quotes`.
- Compra, venta, emisión de tokens, portfolio o ranking.
- Radar, percentiles, barras normalizadas, comparación con otros jugadores o métricas deportivas adicionales.
- Seguimiento/favoritos, eventos de partido, mapas de disparos, noticias y datos físicos/contractuales inexistentes.
- Filtros reales o búsqueda real sobre el catálogo. El rediseño del catálogo y su adaptación responsive quedan fuera de alcance **de esta SPEC** y se autorizaron expresamente, con su especificación propia, en SPEC-005 el 2026-10-13.
- Selector o cambio automático de temporada; historial dependiente del reloj del navegador.
- Barra lateral; rediseño de la identidad visual y del tema de las demás pantallas; nueva funcionalidad de perfil o avatar del usuario.

## Evidencia de Specify

2026-10-12, antecedente: mecanismo real ejecutado una vez con `--number 009` y `--short-name player-detail`; identidad creada, rama inicialmente omitida por cambios rastreados preexistentes y creada luego mediante operación Git autorizada. Plantilla activa resuelta y revisión inicial registrada en `checklists/requirements.md`. La decisión abierta de elegibilidad y el seguimiento inicial fueron pendientes históricos, no el estado vigente.

2026-10-12, consolidación vigente: cinco decisiones confirmadas integradas con escenarios, requisitos, límites y criterios verificables. Specify evaluado documentalmente: OK; sin ambigüedad funcional bloqueante restante. Los checkboxes del revisor no son gates aprobados ni resultados de producto.

Ejecución de Clarify: definición local `.opencode/commands/speckit.clarify.md` seguida; `check-prerequisites.sh --json --paths-only` ejecutado una vez y resuelto a esta SPEC. Escaneo de alcance, datos, UX, seguridad/errores, integración, excepciones, restricciones, terminología y criterios: claro; decisiones de contratos/composición técnica diferidas a Plan. Cinco respuestas preconfirmadas, cero preguntas nuevas, cero marcadores pendientes. Clarify: OK; Status Ready for planning, no READY. Sin hooks de extensiones (no existe `.specify/extensions.yml`). Sin Plan, Tasks, Analyze, Implement, tests, build, commits, push ni cambios de rama.

Ejecución de Plan: definiciones locales de `/speckit.plan` y Constitution Check aplicadas antes y después del diseño; `setup-plan.sh --json` ejecutado una vez y resolvió esta SPEC. Artefactos generados: `plan.md`, `research.md`, `data-model.md`, `contracts/player-detail.md`, `contracts/demo-data.md` y `quickstart.md`. Sin dependencias nuevas, `NEEDS CLARIFICATION` ni violaciones. El Status permanece en preparación y READY NO hasta Checklists evaluados, Tasks y Analyze. Siguiente paso permitido: `/speckit.checklist`.

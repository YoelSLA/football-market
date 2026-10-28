# Feature Specification: Valoraciones estadísticas de jugadores

**Feature Branch**: `feat/013-player-statistical-valuations` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `013-player-statistical-valuations`, sin el prefijo de tipo.

**Created**: 2026-10-19

**Status**: Ready for planning

**Input**: Calcular y persistir dos estrategias de valoración estadística sobre los datos extraídos por SPEC-012 y permitir su evaluación manual para toda la población, sin incluir programación periódica, consultas de resultados, ranking ni conversión a cotización.

**Trazabilidad**: Cubre parcialmente `REQ-S2-02` (métricas para valuación) y `REQ-S2-03` (base de la cotización) de `tp/entregas.md`. La conversión del resultado a cotización y su consulta permanecen fuera de esta feature.

## Clarifications

### Session 2026-10-19

- Q: ¿La evaluación debe usar una única fecha de extracción común para toda la población o el snapshot confirmado más reciente de cada jugador? → A: Usar la última fecha JSON confirmada con cobertura poblacional y evaluar únicamente las filas de esa fecha; scraping respalda en esa misma fecha.
- Q: ¿Quién debe estar autorizado para ejecutar manualmente la evaluación de toda la población? → A: Aplicar las mismas reglas de autenticación y autorización que los endpoints de sincronización de SPEC-012.
- Q: ¿Qué debe ocurrir si se solicita una segunda evaluación mientras otra evaluación poblacional todavía está en curso? → A: Se asume operativamente una única ejecución; las solicitudes concurrentes quedan fuera del contrato de esta feature.
- Q: ¿Cómo debe hacerse visible un fallo individual de cálculo o persistencia sin detener la evaluación completa? → A: Incluir `failedPlayers` en el resumen y registrar internamente el jugador, la etapa y la causa del fallo.
- Q: ¿Cómo debe interpretarse la configurabilidad exigida para las estrategias frente a los pesos fijos solicitados? → A: Conforme a la aclaración del equipo docente aportada por el usuario, los pesos son configurables al definir cada estrategia para permitir ponderaciones distintas incluso cuando compartan métricas; una vez definida y versionada la estrategia, sus pesos permanecen fijos para asegurar trazabilidad y reproducibilidad. Esta feature define dos estrategias materialmente distintas por sus métricas, fórmulas y ponderaciones, calcula y persiste ambas, y deja su selección activa para la futura feature de cotización. Un cambio posterior de pesos requiere una nueva versión de estrategia, no la modificación de `v1` en tiempo de ejecución.
- Q: ¿Deben participar en los percentiles solamente los jugadores que tienen completas todas las métricas exigidas por ambas estrategias, o puede aportar cada jugador a las métricas que sí tenga aunque finalmente sea excluido? → A: Cada métrica usa todos los jugadores de la misma posición, liga y temporada que tengan disponible ese valor, aunque luego sean excluidos del resultado final.
- Q: ¿La mediana usada para imputar un denominador cero debe calcularse dentro de la misma liga y temporada, además de la misma posición? → A: La mediana se calcula dentro de la misma posición, liga y temporada.
- Q: ¿Debe calcularse `minutesPerAppearance` aunque ninguna fórmula, atributo ni resultado persistido de las dos estrategias lo utiliza? → A: `minutesPerAppearance` no se calcula en esta feature.
- Q: ¿Qué debe responder la evaluación si todavía no existe ninguna campaña JSON con cobertura poblacional completa? → A: Rechazar con un error controlado indicando que no existe un snapshot poblacional confirmado.
- Q: ¿Qué debe ocurrir con las métricas por 90 cuando `minutes_played_total` es cero pero el jugador tiene apariciones? → A: Imputar cada tasa por 90 con la mediana de posición, liga y temporada y registrarla en `imputed_metrics`.
- Q: ¿Qué fórmula exacta debe utilizar el percentil de rango medio para que el mismo conjunto de jugadores produzca siempre los mismos valores normalizados? → A: `(cantidadMenores + 0.5 × cantidadIguales) / cantidadTotal`.
- Q: ¿La evaluación debe responder exitosamente cuando uno o más jugadores fallan inesperadamente, siempre que el recorrido completo haya terminado? → A: Responder exitosamente con `failedPlayers`, incluso si todos los jugadores fallaron después de iniciar el recorrido.
- Q: ¿Con qué precisión deben persistirse los componentes, atributos y resultados calculados entre 0 y 1? → A: Persistir seis decimales con redondeo half-up, calculando internamente con mayor precisión.
- Q: ¿Qué nombres y versión constantes deben identificar las dos estrategias persistidas? → A: `POSITIONAL_PERFORMANCE` v1 y `GENERAL_ATTRIBUTES` v1.
- Q: ¿Cómo debe calcularse la mediana de imputación cuando existe una cantidad par de valores donantes? → A: Promediar los dos valores centrales con precisión completa antes del redondeo final.

### Session 2026-10-20

- Q: ¿Qué debe confirmar una campaña JSON para que la evaluación poblacional pueda ejecutarse, dado que muchos jugadores legítimos no tienen datos en el proveedor? → A: La confirmación se define por la terminación de la campaña y la integridad de sus items confirmados, no por la cobertura total de la población: run terminado (`COMPLETED` o `PARTIAL`), sin items en fallo estructural (`INCOMPLETE`, `INTERRUPTED`, `BLOCKED`, `STRUCTURE_CHANGED`) y con fila JSON para todo item `COMPLETED`. Los items `NO_DATA` y `NOT_FOUND` no invalidan la campaña, y los fallos de proveedor sobre un jugador concreto tampoco.
- Q: ¿Qué ocurre con los jugadores elegibles que no tienen snapshot confirmado en la campaña vigente? → A: Se excluyen como `MISSING_OR_UNCONFIRMED_SNAPSHOT` y se contabilizan en el resumen; no se les imputa cero ni se mixed con poblaciones artificiales.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Evaluar el rendimiento por posición (Priority: P1)

Como responsable del mercado, quiero evaluar a cada jugador mediante responsabilidades observables de su posición para obtener un score comparable dentro de su liga, temporada y posición.

**Why this priority**: Proporciona la valoración deportiva principal sobre la que podrán apoyarse futuras cotizaciones sin confundir posiciones ni poblaciones deportivas distintas.

**Independent Test**: Puede verificarse con snapshots confirmados de las cuatro posiciones, incluidos valores cero, ausencias y respaldo por canal, comprobando componentes y score final sin utilizar la segunda estrategia.

**Acceptance Scenarios**:

1. **Given** jugadores elegibles de una misma liga, temporada y posición con métricas completas, **When** se ejecuta la evaluación, **Then** cada uno obtiene un score posicional entre 0 y 1 calculado únicamente contra esa población.
2. **Given** una métrica JSON nula y la misma métrica compartida disponible mediante scraping, **When** se evalúa al jugador, **Then** se utiliza el valor de scraping y la procedencia resultante lo refleja.
3. **Given** una métrica JSON con valor cero y un valor de scraping distinto, **When** se evalúa al jugador, **Then** se conserva el cero JSON y no se activa el respaldo.
4. **Given** un arquero elegible, **When** se calcula su score, **Then** se compara solo con arqueros y no recibe penalización de rendimiento específica.

---

### User Story 2 - Obtener atributos generales y GRL (Priority: P2)

Como responsable del mercado, quiero una segunda valoración que describa dimensiones generales del rendimiento y un GRL para contrastar perfiles con una estrategia distinta.

**Why this priority**: Satisface la necesidad de contar con dos estrategias trazables y ofrece una lectura multidimensional complementaria al rendimiento por posición.

**Independent Test**: Puede verificarse calculando los cinco atributos de un jugador de campo y los tres de un arquero, confirmando la nulabilidad de los atributos que no correspondan y un GRL final entre 0 y 1.

**Acceptance Scenarios**:

1. **Given** un jugador de campo elegible, **When** se ejecuta la estrategia de atributos, **Then** obtiene `SHO`, `PAS`, `DRI`, `DEF` y `PHY`, los atributos de arquero quedan ausentes y se calcula su GRL con los pesos de su posición.
2. **Given** un arquero elegible, **When** se ejecuta la estrategia, **Then** obtiene `SAV`, `DIS` y `ACT`, los cinco atributos de campo quedan ausentes y se calcula su GRL específico.
3. **Given** dos jugadores de distinta liga o temporada, **When** se calculan sus atributos, **Then** ninguno interviene en la normalización del otro.

---

### User Story 3 - Ejecutar y auditar una evaluación poblacional (Priority: P3)

Como operador autorizado, quiero iniciar manualmente una evaluación de toda la población vigente y recibir su cobertura para conocer qué resultados se publicaron, cuáles se excluyeron y qué imputaciones se realizaron.

**Why this priority**: Hace operables y observables ambas estrategias, garantiza persistencia coherente y deja el mismo servicio disponible para que una feature posterior programe su ejecución.

**Independent Test**: Puede verificarse invocando `POST /api/valuations/evaluate` dos veces en la misma fecha UTC y comprobando el resumen, la continuidad ante fallos individuales, la ausencia de duplicados y la retirada de resultados obsoletos.

**Acceptance Scenarios**:

1. **Given** una población con jugadores elegibles y excluibles, **When** el operador inicia la evaluación, **Then** ambas estrategias se calculan para cada elegible y el resumen distingue todas las exclusiones e imputaciones.
2. **Given** un fallo individual durante el cálculo o escritura de un jugador, **When** continúa la ejecución, **Then** los demás jugadores se procesan y el jugador fallido no conserva un resultado inválido de esa fecha.
3. **Given** una evaluación ya realizada en la fecha UTC actual, **When** se repite con el snapshot vigente, **Then** se reemplazan los resultados reevaluados, se retiran los que dejaron de ser evaluables y no aparecen duplicados.
4. **Given** una cobertura baja, **When** finaliza la evaluación, **Then** los resultados válidos se conservan y el resumen informa la cobertura sin bloquear su publicación.

### Edge Cases

- Un cero publicado es un valor válido en métricas, numeradores, minutos y apariciones; nunca activa respaldo ni se convierte en ausencia.
- Si `minutes_played_total` es cero y existen apariciones, cada tasa por 90 se imputa con la mediana de posición, liga y temporada; la confianza cero ajusta el resultado final a 0.5.
- Un jugador con apariciones iguales a cero se excluye como `NO_APPEARANCES`, aunque otras métricas estén presentes.
- Un snapshot sin contexto de liga resuelto se excluye como `UNRESOLVED_LEAGUE_CONTEXT`; no se mezcla con una población artificial.
- Si falta la fila primaria JSON vigente o no está confirmada, el jugador se excluye como `MISSING_OR_UNCONFIRMED_SNAPSHOT`; scraping no reemplaza el snapshot primario completo.
- Si no existe ninguna campaña JSON que cumpla el criterio de confirmación de FR-004, la evaluación se rechaza antes de calcular o modificar resultados; no se publica un resumen vacío.
- Una clave exclusiva de JSON ausente no admite respaldo y, si es requerida por la posición, causa exclusión por `MISSING_REQUIRED_METRICS`.
- Un jugador excluido del resultado final por otra métrica ausente sigue participando en el percentil de cada métrica que sí tenga disponible; ninguna ausencia participa como cero.
- Si ambos canales aportan valores utilizados, la procedencia es `MIXED`; si todos proceden de uno, se registra ese canal.
- Un denominador cero no produce infinito, cero ni una ventaja artificial; activa la imputación definida para la métrica.
- Un cambio de liga mueve al jugador a la población de la liga reflejada por el snapshot vigente desde la siguiente ejecución, sin considerarlo incidencia.
- Una población de una sola persona recibe el percentil neutro 0.5.
- Un fallo individual no puede dejar una sola estrategia vigente para el jugador y fecha: ambas se publican de forma coherente o ambas quedan sin resultado para esa ejecución.

## Requirements *(mandatory)*

### Functional Requirements

#### Fuente, población y procedencia

- **FR-001**: La valuación MUST leer exclusivamente filas persistidas en `player_statistics` por SPEC-012 y MUST NOT ejecutar extracción, scraping ni llamadas externas.
- **FR-002**: El sistema MUST usar el catálogo cerrado vigente de 60 claves de `MetricCatalog`; MUST NOT crear métricas ni utilizar claves ajenas al catálogo.
- **FR-003**: Salvo `whoscored_rating_average`, citada únicamente para declarar su no obligatoriedad en v1, toda clave de entrada citada por esta feature MUST existir como métrica obligatoria en SPEC-012.
- **FR-004**: La población vigente MUST usar una única fecha de extracción común: la última fecha JSON confirmada. Una campaña JSON se considera confirmada cuando cumple las tres condiciones siguientes: (1) su run de extracción está terminado, es decir, en estado `COMPLETED` o `PARTIAL`, sin `RUNNING`, `FAILED` ni `INTERRUPTED`; (2) ningún item de jugador de esa fecha quedó en estado de fallo estructural, entendiendo por estructural `INCOMPLETE`, `INTERRUPTED`, `BLOCKED` y `STRUCTURE_CHANGED`; y (3) todo item en estado `COMPLETED` tiene su fila `player_statistics` de canal JSON para esa fecha. Los items en estado `NO_DATA` o `NOT_FOUND` son terminales sin fallo y no invalidan la campaña, y un fallo de proveedor sobre un jugador concreto tampoco la invalida. La cobertura de la población se resuelve por jugador durante el recorrido: un jugador elegible sin fila JSON confirmada en esa fecha se excluye como `MISSING_OR_UNCONFIRMED_SNAPSHOT` conforme a FR-010 y FR-011, y su ausencia nunca se computa como cero. La mera existencia de una fecha, de un run o de filas parciales no acredita cobertura poblacional. Solo se evalúan filas JSON de esa fecha y el respaldo de scraping MUST pertenecer al mismo jugador y fecha. La temporada y liga local MUST ser las resueltas en ese snapshot; no se seleccionan, comparan ni combinan temporadas o ligas distintas.
- **FR-005**: Para cada jugador y métrica, JSON MUST ser la fuente primaria. Solo si el valor JSON es nulo y la clave pertenece a las 42 compartidas se MUST intentar el valor de scraping de la misma campaña. Las 15 claves exclusivas de JSON no admiten respaldo y son: `key_passes_per_match`, `successful_dribbles_per_match`, `offsides_per_match`, `dispossessed_per_match`, `bad_controls_per_match`, `passes_per_match`, `successful_long_balls_per_match`, `successful_crosses_per_match`, `tackles_per_match`, `interceptions_per_match`, `clearances_per_match`, `blocked_shots_per_match`, `dribbled_past_per_match`, `offsides_won_per_match` y `own_goals_total`. Las 3 claves exclusivas de scraping son `shots_on_target_total`, `second_yellow_total` y `starts_total` y, por tanto, no admiten valor JSON.
- **FR-006**: El respaldo MUST activarse solo ante nulo. Cero MUST conservarse como dato válido y MUST NOT reemplazarse ni activar respaldo.
- **FR-007**: Una ausencia MUST permanecer ausente y MUST NOT completarse con altura, marcador, titularidades, minutos ni otra métrica.
- **FR-008**: Cada resultado MUST registrar `JSON`, `SCRAPING` o `MIXED` según los valores efectivamente utilizados. Su `source_extraction_date`, las filas de SPEC-012 y la regla determinista de respaldo MUST permitir reconstruir la procedencia de cada entrada sin duplicarla en las tablas de valuación.
- **FR-009**: La posición general MUST derivarse de la posición persistida del jugador. El conjunto cerrado reconocido son las cuatro posiciones del catálogo y sus equivalentes en inglés: `Goalkeeper` → `GOALKEEPER`, `Defence` o `Defender` → `DEFENDER`, `Midfield` o `Midfielder` → `MIDFIELDER` y `Offence` o `Forward` → `FORWARD`, sin subposiciones. Una posición persistida fuera de ese conjunto se trata como fallo individual controlado y MUST NOT crear una quinta categoría de posición ni una causa funcional de exclusión nueva.
- **FR-010**: Un jugador MUST ser evaluable cuando tiene snapshot primario vigente confirmado, contexto de liga resuelto, `appearances_total > 0` y todas las métricas requeridas por su posición presentes después del respaldo permitido. Los motivos de exclusión se evalúan en el orden de FR-065.
- **FR-011**: El resumen MUST distinguir al menos `UNRESOLVED_LEAGUE_CONTEXT`, `MISSING_OR_UNCONFIRMED_SNAPSHOT`, `MISSING_REQUIRED_METRICS` y `NO_APPEARANCES`, y cada jugador excluido MUST contabilizarse en un único motivo, conforme al orden de FR-065.

#### Derivación, imputación y normalización

- **FR-012**: El sistema MUST derivar `goalsPer90` a partir de `goals_total`, `assistsPer90` a partir de `assists_total`, `yellowCardsPer90` a partir de `yellow_cards_total` y `redCardsPer90` a partir de `red_cards_total`, cada una como `total / minutesPlayed * 90`. Si `minutes_played_total` es cero y el jugador tiene apariciones, cada tasa por 90 MUST imputarse con la mediana de la misma posición, liga y temporada entre jugadores con minutos mayores que cero y MUST registrarse en `imputed_metrics`; si no existe donante, aplica FR-015. No MUST aplicar esta conversión a porcentajes, métricas por partido ni rating.
- **FR-013**: El sistema MUST derivar `dribbleSuccess = 1 - failed_dribbles_per_match / total_dribbles_per_match`, `aerialWinRate = aerial_duels_won_per_match / total_aerial_duels_per_match`, `saveOutOfBox = outside_box_saves_per_match / saves_per_match` y `blocksPerMatch = blocked_passes_per_match + blocked_crosses_per_match`. `minutesPerAppearance` MUST NOT calcularse porque no interviene en ninguna fórmula ni resultado de esta feature.
- **FR-014**: Si el denominador de una métrica derivada es cero, el sistema MUST imputar la mediana de los jugadores de la misma posición, liga y temporada cuyo denominador sea mayor que cero y MUST registrar la clave derivada en `imputed_metrics`. Con una cantidad par de donantes, la mediana MUST ser el promedio de los dos valores centrales calculado con precisión completa antes del redondeo persistido de FR-052.
- **FR-015**: Si una combinación de posición, liga y temporada no tiene ningún jugador con denominador mayor que cero y, por tanto, no existe mediana donante, la métrica MUST recibir el valor normalizado neutro 0.5 y MUST registrarse igualmente en `imputed_metrics`.
- **FR-016**: Cada métrica MUST normalizarse por percentil contra todos los jugadores de la misma posición general, liga local y temporada vigente que tengan disponible el valor de esa métrica después del respaldo permitido, aunque finalmente sean excluidos por la ausencia de otra métrica requerida. Una ausencia MUST NOT ingresar como cero. La población MUST incluir jugadores con pocos minutos y los arqueros MUST compararse exclusivamente con arqueros.
- **FR-017**: El percentil MUST utilizar rango medio mediante `normalizedMetric = (cantidadMenores + 0.5 * cantidadIguales) / cantidadTotal`, calculado dentro de la población de la métrica definida en FR-016. Todos los valores empatados MUST recibir el mismo resultado y una población unitaria MUST recibir 0.5.
- **FR-018**: Ninguna métrica de las fórmulas de esta feature MUST invertirse. Las únicas métricas cuyo valor menor representa mejor rendimiento son `offsides_per_match`, `dispossessed_per_match`, `bad_controls_per_match`, `dribbled_past_per_match`, `fouls_committed_per_match`, `yellow_cards_total` y `red_cards_total`, y todas participan exclusivamente como penalización, usando el percentil creciente de su comportamiento negativo. MUST NOT usarse `1 - normalizedMetric` en ninguna fórmula.
- **FR-019**: La confianza MUST ser `min(minutesPlayed / minimumMinutes, 1)`, con mínimos configurables iniciales de 270 minutos para campo y 540 para arqueros. MUST ajustar hacia 0.5 y MUST NOT excluir jugadores de la normalización por pocos minutos.
- **FR-020**: `performanceAdjustment` MUST ser `performanceMaximumImpact * (1 - 2 * performancePenalty)` y `disciplineAdjustment` MUST ser `disciplineMaximumImpact * (1 - 2 * disciplinePenalty)`, con impactos configurables iniciales 0.10 y 0.05 respectivamente.
- **FR-021**: La disciplina MUST afectar las cuatro posiciones como `0.20 normalizedFoulsCommittedPerMatch + 0.30 normalizedYellowCardsPer90 + 0.50 normalizedRedCardsPer90`, donde `normalizedFoulsCommittedPerMatch` es el percentil de `fouls_committed_per_match`. Faltas y tarjetas MUST NOT repetirse en la penalización de rendimiento de una misma estrategia.
- **FR-022**: `whoscored_rating_average` MUST ser opcional y MUST NOT excluir jugadores ni participar como requisito o fórmula en v1.

#### Estrategia 1 — rendimiento específico por posición

- **FR-023**: Cada estrategia MUST definirse mediante una combinación trazable de métricas, fórmulas y ponderaciones que permita distinguirla de las demás, incluso cuando compartan métricas. Una vez definida y versionada, esa configuración MUST permanecer fija y no editable por el usuario para asegurar reproducibilidad; cualquier cambio posterior de pesos MUST crear una nueva versión. La estrategia 1 MUST identificarse como `POSITIONAL_PERFORMANCE` versión `v1` y la estrategia 2 como `GENERAL_ATTRIBUTES` versión `v1`. Esta feature calcula y persiste ambas configuraciones, mientras que su selección activa corresponde a la futura feature de cotización.
- **FR-024**: Para `FORWARD`, `positiveScore` MUST ser `0.30 goalsPer90 + 0.20 assistsPer90 + 0.15 shots_per_match + 0.15 key_passes_per_match + 0.10 successful_dribbles_per_match + 0.10 dribbleSuccess`, todo normalizado; `performancePenalty` MUST ser `0.30 offsides_per_match + 0.40 dispossessed_per_match + 0.30 bad_controls_per_match`, todo normalizado.
- **FR-025**: Para `MIDFIELDER`, `positiveScore` MUST ser `0.20 pass_completion_percentage + 0.15 key_passes_per_match + 0.15 assistsPer90 + 0.10 passes_per_match + 0.10 successful_long_balls_per_match + 0.05 successful_crosses_per_match + 0.10 successful_dribbles_per_match + 0.075 tackles_per_match + 0.075 interceptions_per_match`, todo normalizado; `performancePenalty` MUST ser `0.55 dispossessed_per_match + 0.45 bad_controls_per_match`, todo normalizado.
- **FR-026**: Para `DEFENDER`, `positiveScore` MUST ser `0.20 tackles_per_match + 0.20 interceptions_per_match + 0.15 clearances_per_match + 0.15 blocksPerMatch + 0.15 aerialWinRate + 0.15 aerial_duels_won_per_match`, todo normalizado; `performancePenalty` MUST ser `normalizedDribbledPastPerMatch`.
- **FR-027**: Para `GOALKEEPER`, `positiveScore` MUST ser `0.40 saves_per_match + 0.20 saveOutOfBox + 0.20 pass_completion_percentage + 0.20 aerialWinRate`, todo normalizado. `performancePenalty` y `performanceAdjustment` MUST ser nulos y `rawScore` MUST sumar solo `positiveScore + disciplineAdjustment`.
- **FR-028**: Para jugadores de campo, `rawScore` MUST ser `positiveScore + performanceAdjustment + disciplineAdjustment`. Para todos, `finalScore` MUST ser `clamp(confidence * rawScore + (1 - confidence) * 0.5, 0, 1)`.
- **FR-029**: La interpretación del score MUST usar intervalos no solapados: desde 0 inclusive hasta 0.3 exclusivo, bajo; desde 0.3 inclusive hasta 0.5 exclusivo, inferior al promedio; exactamente 0.5, promedio; mayor que 0.5 hasta 0.7 exclusivo, superior al promedio; desde 0.7 hasta 1 inclusive, alto.
- **FR-030**: El score de arquero MUST describirse solo como evaluación relativa de rendimiento general observable, distribución y actividad; MUST NOT presentarse como capacidad de atajada o prevención de goles.

#### Estrategia 2 — atributos generales

- **FR-031**: Todos los jugadores de campo MUST recibir `SHO`, `PAS`, `DRI`, `DEF` y `PHY`; sus atributos `SAV`, `DIS` y `ACT` MUST ser nulos.
- **FR-032**: Para campo, `SHO` MUST ser `0.60 normalizedGoalsPer90 + 0.25 normalizedShotsPerMatch + 0.15 normalizedOutsideBoxGoalsPerMatch`.
- **FR-033**: Para campo, `PAS` MUST ser `0.30 normalizedPassCompletionPercentage + 0.25 normalizedKeyPassesPerMatch + 0.20 normalizedAssistsPer90 + 0.15 normalizedSuccessfulLongBallsPerMatch + 0.10 normalizedPassesPerMatch`.
- **FR-034**: Para campo, `dribblingPositive` MUST ser `0.55 normalizedDribbleSuccess + 0.30 normalizedSuccessfulDribblesPerMatch`, y `dribblingPenalty` MUST ser `0.60 normalizedDispossessedPerMatch + 0.40 normalizedBadControlsPerMatch`.
- **FR-035**: Para campo, `defendingPositive` MUST ser `0.35 normalizedTacklesPerMatch + 0.30 normalizedInterceptionsPerMatch + 0.20 normalizedClearancesPerMatch + 0.15 normalizedBlocksPerMatch`, y `defendingPenalty` MUST ser `normalizedDribbledPastPerMatch`.
- **FR-036**: `DRI` MUST ser `dribblingPositive + attributePenaltyMaximumImpact * (1 - 2 * dribblingPenalty)` y `DEF` MUST ser `defendingPositive + attributePenaltyMaximumImpact * (1 - 2 * defendingPenalty)`. Una penalización 0 produce el ajuste positivo máximo, 0.5 uno neutro y 1 el negativo máximo.
- **FR-037**: `attributePenaltyMaximumImpact` MUST ser configurable con valor inicial 0.10; `DRI` y `DEF` MUST quedar acotados entre 0 y 1 tras aplicar la fórmula aclarada.
- **FR-038**: Para campo, `PHY` MUST ser `0.60 normalizedAerialWinRate + 0.40 normalizedAerialDuelsWonPerMatch`; la altura MUST NOT utilizarse.
- **FR-039**: El `baseGRL` de campo MUST ponderar `SHO/PAS/DRI/DEF/PHY` así: `FORWARD` 0.35/0.20/0.25/0.08/0.12; `MIDFIELDER` 0.16/0.30/0.22/0.20/0.12; `DEFENDER` 0.09/0.18/0.13/0.35/0.25.
- **FR-040**: Los arqueros MUST recibir solo `SAV`, `DIS` y `ACT`; los cinco atributos de campo MUST ser nulos.
- **FR-041**: Para arqueros, `SAV` MUST ser `0.60 normalizedSavesPerMatch + 0.40 normalizedSaveOutOfBox`; `DIS` MUST ser `0.60 normalizedPassCompletionPercentage + 0.25 normalizedSuccessfulLongBallsPerMatch + 0.15 normalizedPassesPerMatch`; `ACT` MUST ser `0.50 normalizedClearancesPerMatch + 0.30 normalizedAerialDuelsWonPerMatch + 0.20 normalizedPassesPerMatch`.
- **FR-042**: El `baseGRL` de arquero MUST ser `0.55 SAV + 0.30 DIS + 0.15 ACT`.
- **FR-043**: Para todos, `rawGRL` MUST ser `baseGRL + disciplineAdjustment` y `finalGRL` MUST ser `clamp(confidence * rawGRL + (1 - confidence) * 0.5, 0, 1)`.
- **FR-044**: Todos los atributos y GRL MUST estar entre 0 y 1. La presentación 1–99 no forma parte de esta feature.
- **FR-045**: Los resultados de arquero MUST describirse como evaluación relativa de rendimiento general, distribución y actividad defensiva observable según estadísticas agregadas. MUST NOT describirse como porcentaje o capacidad de atajada, prevención de goles, rendimiento ante tiros o penales ni evaluación integral de la posición.

#### Persistencia, vigencia y trazabilidad

- **FR-046**: Cada valoración posicional MUST conservar: identidad del jugador, fecha UTC de valuación, nombre y versión constantes de estrategia, posición general, procedencia, fecha de extracción, score positivo, penalización y ajuste de rendimiento cuando correspondan, penalización y ajuste disciplinario, score bruto, confianza, score final, métricas imputadas y momento de cálculo.
- **FR-047**: Cada valoración de atributos MUST conservar: identidad del jugador, fecha UTC de valuación, nombre y versión constantes de estrategia, posición general, procedencia, fecha de extracción, los ocho atributos con la nulabilidad definida por tipo de jugador, GRL base, ajuste disciplinario, GRL bruto, confianza, GRL final, métricas imputadas y momento de cálculo.
- **FR-048**: MUST existir como máximo una valoración de cada estrategia por jugador y fecha. Una segunda ejecución de la fecha MUST sobrescribir a los reevaluados y MUST NOT crear duplicados.
- **FR-049**: Antes de publicar el resultado de una repetición, el sistema MUST retirar ambas valoraciones de esa fecha para todo jugador que haya dejado de ser evaluable o haya fallado, evitando resultados viejos asociados a un snapshot sustituido.
- **FR-050**: Las consultas vigentes posteriores MUST poder obtener, sin recalcular, la última fecha de valoración disponible de cada jugador para cada estrategia.
- **FR-051**: La persistencia MUST permitir búsquedas eficientes por fecha descendente, resultado final y jugador con fecha descendente en cada estrategia.
- **FR-052**: Cada fila MUST permitir reconstruir el cálculo mediante sus componentes, versión, procedencia, fecha de extracción e imputaciones. Las entradas y pesos MUST NOT duplicarse como snapshot ni configuración por ejecución. Todos los componentes, penalizaciones, ajustes, confianza, atributos, scores y GRL decimales MUST calcularse internamente con mayor precisión y persistirse con seis decimales mediante redondeo half-up; el clamp MUST aplicarse antes del redondeo persistido.

#### Evaluación manual y cobertura

- **FR-053**: El sistema MUST exponer `POST /api/valuations/evaluate`, sin identificación de jugador, para evaluar síncronamente toda la población vigente. La ruta efectiva conserva el prefijo `/api/` obligatorio del backend; `/valuations/evaluate` identifica el recurso funcional solicitado. El endpoint MUST aplicar exactamente las mismas reglas de autenticación y autorización que los endpoints de sincronización de estadísticas de SPEC-012 y MUST NOT introducir un rol nuevo.
- **FR-054**: La operación MUST usar el mismo servicio de evaluación poblacional que podrá invocar el job semanal de una feature posterior, sin incluir el job ni su agenda en este alcance.
- **FR-055**: La operación MUST ser idempotente por `valuationDate`, no MUST realizar llamadas externas y no requiere ejecución asíncrona, reanudación por lotes ni registro de runs.
- **FR-056**: Un fallo individual MUST registrarse internamente con el jugador, la etapa y la causa, limpiar cualquier resultado inválido de ese jugador y fecha, y permitir que la ejecución continúe con el resto. El detalle interno MUST NOT exponerse en la respuesta.
- **FR-057**: La respuesta MUST incluir `valuationDate`, `evaluatedPlayers`, `failedPlayers`, conteos de excluidos por cada motivo definido e `imputedMetricCounts` por clave derivada. `failedPlayers` MUST distinguir los fallos inesperados de las exclusiones funcionales. Una vez iniciado y completado el recorrido poblacional, la operación MUST responder exitosamente con su resumen aunque `failedPlayers` sea mayor que cero o todos los jugadores hayan fallado; los errores globales ocurridos antes de completar el recorrido conservan el tratamiento de error controlado aplicable.
- **FR-058**: La cobertura MUST ser informativa. Ningún umbral de cobertura MUST bloquear ni descartar resultados válidos. Si no existe ninguna campaña JSON confirmada según el criterio de confirmación de FR-004, la operación MUST rechazarse con un error controlado que indique la ausencia de un snapshot poblacional confirmado, antes de calcular o modificar resultados; MUST NOT responder con una evaluación vacía ni utilizar una campaña que no cumpla ese criterio. Los jugadores de la población elegible que no encuentren snapshot confirmado en la campaña vigente se excluyen como `MISSING_OR_UNCONFIRMED_SNAPSHOT` y su impacto queda visible en el resumen.

#### Límites de alcance

- **FR-059**: Esta feature MUST NOT incluir job programado, agenda ni ejecución automática.
- **FR-060**: Esta feature MUST NOT incluir endpoints de lectura, historial o detalle de valuaciones.
- **FR-061**: Esta feature MUST NOT incluir ranking de jugadores ni selección de estrategia activa.
- **FR-062**: Esta feature MUST NOT convertir `finalScore` o `finalGRL` en precio, cotización o valor de token.
- **FR-063**: Esta feature MUST NOT modificar extracción, scraping, catálogo de métricas, mercado, portfolio, tokenización ni operaciones.

#### Coherencia transversal del cálculo y de las exclusiones

- **FR-064**: El cálculo MUST seguir un orden único e inequívoco de etapas: selección de la campaña confirmada vigente → resolución de contexto de liga y temporada → respaldo de scraping solo ante valor JSON nulo → derivación de métricas → imputación por denominador cero o ausencia de donantes → normalización por percentil dentro de la población de la métrica → ponderación y ajustes de penalización y disciplina → ajuste por confianza → clamp a `[0,1]` → redondeo persistido a seis decimales con `HALF_UP`. Ninguna etapa MUST ejecutarse fuera de ese orden ni antes de la que la precede.
- **FR-065**: Los motivos de exclusión funcional MUST evaluarse en un orden de severidad fijo: primero `UNRESOLVED_LEAGUE_CONTEXT`, después `MISSING_OR_UNCONFIRMED_SNAPSHOT`, después `MISSING_REQUIRED_METRICS` y por último `NO_APPEARANCES`. Cada jugador excluido MUST contabilizarse únicamente en el primer motivo que satisface, de modo que la suma de exclusiones MUST NOT exceder el número de jugadores excluidos. Un fallo individual inesperado MUST NOT contabilizarse como exclusión funcional y MUST NOT desplazar los conteos de exclusiones.

#### Conjunto de métricas requeridas por posición

- **FR-066**: Las métricas de entrada requeridas por un jugador MUST ser exactamente las de la tabla siguiente, que resulta de la unión de las métricas de ambas estrategias, de la disciplina (FR-021) y de las métricas base de las derivadas (FR-012, FR-013). La ausencia de cualquiera de ellas, después del respaldo permitido, MUST producir la exclusión `MISSING_REQUIRED_METRICS` conforme a FR-010 y FR-065.

  | Grupo | Métricas base requeridas |
  | --- | --- |
  | Común a las cuatro posiciones | `minutes_played_total`, `appearances_total`, `fouls_committed_per_match`, `yellow_cards_total`, `red_cards_total` |
  | Campo (estrategia 2) | `goals_total`, `assists_total`, `shots_per_match`, `outside_box_goals_per_match`, `pass_completion_percentage`, `key_passes_per_match`, `successful_long_balls_per_match`, `passes_per_match`, `failed_dribbles_per_match`, `total_dribbles_per_match`, `successful_dribbles_per_match`, `dispossessed_per_match`, `bad_controls_per_match`, `tackles_per_match`, `interceptions_per_match`, `clearances_per_match`, `blocked_passes_per_match`, `blocked_crosses_per_match`, `dribbled_past_per_match`, `aerial_duels_won_per_match`, `total_aerial_duels_per_match` |
  | `FORWARD` | Común + Campo + `offsides_per_match` |
  | `MIDFIELDER` | Común + Campo + `successful_crosses_per_match` |
  | `DEFENDER` | Común + Campo |
  | `GOALKEEPER` | Común + `saves_per_match`, `outside_box_saves_per_match`, `pass_completion_percentage`, `successful_long_balls_per_match`, `passes_per_match`, `clearances_per_match`, `aerial_duels_won_per_match`, `total_aerial_duels_per_match` |

  Consecuencia asumida: un jugador de campo requiere 26 o 27 métricas base, de las cuales hasta 11 son exclusivas de JSON y no admiten respaldo. `MISSING_REQUIRED_METRICS` será el motivo de exclusión dominante y la cobertura resultante puede ser baja, sin que ello bloquee la publicación de los resultados válidos (FR-058, SC-010).

### Key Entities *(include if feature involves data)*

- **Snapshot estadístico de jugador**: Fila vigente de SPEC-012 para un jugador, fecha de campaña y canal, con contexto de liga y temporada y las 60 claves cerradas.
- **Población de normalización**: Jugadores de una misma posición general, liga local y temporada vigente que intervienen en percentiles aun cuando tengan baja confianza.
- **Valoración posicional**: Resultado trazable de la estrategia 1 para un jugador y fecha, incluidos componentes, ajustes, confianza, procedencia e imputaciones.
- **Valoración de atributos**: Resultado trazable de la estrategia 2 para un jugador y fecha, con atributos aplicables, GRL y componentes comunes.
- **Resumen de evaluación**: Cobertura observable de una ejecución manual, con fecha, evaluados, exclusiones, fallos e imputaciones agregadas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los resultados publicados utiliza únicamente claves existentes en las 60 métricas de SPEC-012 y ninguna ejecución crea una clave nueva.
- **SC-002**: En todos los casos con JSON cero y scraping distinto, el 100% conserva el cero JSON; en todos los casos con JSON nulo y respaldo compartido disponible, el 100% utiliza scraping.
- **SC-003**: El 100% de las normalizaciones se limita a una única combinación de posición general, liga y temporada y usa, para cada métrica, todos los valores disponibles de ese grupo aunque algunos jugadores no obtengan resultado final; ninguna ausencia ingresa como cero y ningún arquero se compara con jugador de campo.
- **SC-004**: El 100% de las métricas con denominador cero sigue la regla de imputación resuelta y registra su clave, sin producir divisiones inválidas ni ventajas por ausencia de actividad.
- **SC-005**: El 100% de scores, atributos y GRL publicados queda entre 0 y 1, y todo jugador con confianza menor que 1 se ajusta hacia 0.5.
- **SC-006**: Para una fecha, cada jugador tiene cero o una fila por estrategia; repetir la evaluación no aumenta ese máximo y elimina el 100% de los resultados que dejaron de ser válidos.
- **SC-007**: Un fallo individual permite continuar con todos los demás candidatos y el resumen contabiliza el 100% de evaluados, excluidos y fallidos observados.
- **SC-008**: El operador puede iniciar la evaluación completa con una sola solicitud y recibe un resumen final en la misma interacción, sin seguimiento asíncrono ni llamadas al proveedor.
- **SC-009**: El 100% de las valoraciones puede reconstruirse a partir de sus componentes, versión, fecha y filas de origen, incluida la procedencia de los valores y las imputaciones.
- **SC-010**: Una cobertura de cualquier porcentaje, incluso inferior al 50%, conserva todos los resultados válidos y siempre informa las causas de no evaluación sin bloquear la publicación.
- **SC-011**: Una revisión de alcance encuentra cero jobs, endpoints de lectura, rankings o conversiones a precio introducidos por esta feature.

## Out of Scope

- Job programado, configuración de agenda y periodicidad semanal.
- Endpoints de lectura de valoración, historial o detalle.
- Ranking de jugadores y selección de estrategia activa.
- Conversión de scores o GRL a precio, cotización o valor de token.
- Escala de presentación 1–99 y cualquier interfaz de frontend.
- Extracción o scraping de datos, ampliación de `MetricCatalog` y comparación histórica de temporadas.
- Tokenización, mercado, portfolio y operaciones de compra o venta.

## Assumptions

- SPEC-012 provee una fila independiente por jugador, fecha de campaña y canal, con cobertura JSON 57, scraping 45 y 42 claves compartidas.
- El snapshot primario vigente es la última campaña JSON confirmada según el criterio de FR-004 y gobierna liga y temporada; el respaldo de scraping se busca para el mismo jugador y esa misma fecha de extracción, sin sustituir el contexto primario.
- `minutes_played_total` es una entrada requerida para todas las posiciones porque interviene en tasas por 90 y confianza; su ausencia causa `MISSING_REQUIRED_METRICS`.
- Las dos estrategias de v1 se configuran al definir sus métricas, fórmulas y ponderaciones diferenciadas; sus nombres, versiones y pesos quedan después fijos. Solo los umbrales e impactos expresamente indicados como configurables admiten configuración operativa, y cualquier cambio posterior de pesos requiere una nueva versión de estrategia.
- La fecha de valuación es la fecha UTC de inicio de la ejecución y se conserva aunque la operación cruce medianoche.
- La publicación de ambas estrategias para un jugador y fecha constituye una unidad coherente: no se considera evaluado si solo una pudo persistirse.
- Esta feature no expone lecturas por HTTP ni deja contratos de lectura sin uso: las consultas de consulta que necesite una feature posterior se incorporan en esa feature.
- El operador garantiza que no iniciará evaluaciones concurrentes. La exclusión mutua, espera o rechazo de una segunda solicitud quedan fuera del contrato de esta feature.
- La posición persistida de cada jugador pertenece al conjunto cerrado de cuatro posiciones de FR-009; un valor fuera de ese conjunto corresponde a un fallo individual controlado, no a una quinta posición.

## Verificaciones de diseño completadas

- Confirmado en esta revisión: el contrato de snapshot confirmado de SPEC-012 queda definido por el criterio de confirmación de FR-004 (run terminado, sin items en fallo estructural y con fila JSON para todo item `COMPLETED`), por lo que la cobertura poblacional completa ya no depende de la mera existencia de una fecha o de un run.
- Confirmado en esta revisión: la ausencia de datos de un jugador (`NO_DATA`, `NOT_FOUND`) y el fallo de proveedor sobre un jugador concreto no invalidan la campaña; esos jugadores se excluyen como `MISSING_OR_UNCONFIRMED_SNAPSHOT` y quedan visibles en el resumen.
- Confirmado en Plan: la reconstrucción de procedencia por métrica usa las filas JSON y SCRAPING de SPEC-012 del mismo jugador y fecha, el catálogo y la regla determinista de respaldo, sin agregar inputs duplicados ni persistir fuente por métrica; la decisión y sus alternativas quedan documentadas en `research.md` §2.

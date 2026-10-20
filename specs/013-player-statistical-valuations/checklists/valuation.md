# Checklist de requisitos de valuación: Valoraciones estadísticas de jugadores

**Purpose**: Evaluar la calidad, claridad, consistencia y cobertura de los requisitos de valuación estadística de SPEC-013 antes de Tasks, sin revisar la futura conversión a cotización o precio de token.
**Created**: 2026-10-19
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado fue generado por `/speckit.checklist` a partir del contexto y los requisitos de la feature.
**Review Ownership**: Este artefacto de revisión de calidad de requisitos pertenece al revisor. Un ítem se marca `[x]` únicamente cuando el revisor determina que el criterio de calidad está satisfecho.
**Marker Semantics**: `[x]` significa que el criterio fue revisado y satisfecho respecto de la calidad de requisitos. No significa que la implementación esté completa.
**Review Pass**: 2026-10-19 — revisión manual del revisor contra `spec.md`, `research.md`, `data-model.md`, `contracts/http.md`, `plan.md` y `backend/src/main/java/footballmarket/models/MetricCatalog.java`.

## Completitud de requisitos

- [x] CHK001 ¿Están enumeradas todas las métricas de entrada requeridas por cada posición y estrategia, de modo que pueda determinarse objetivamente cuándo aplica `MISSING_REQUIRED_METRICS`? [Completeness, Spec §FR-010, §FR-023–FR-045]
  - Evidencia: FR-066 incorpora la tabla cerrada de métricas base requeridas por posición (común + campo + específica de `FORWARD`/`MIDFIELDER`/`DEFENDER`; 13 para `GOALKEEPER`), derivada de FR-021, FR-024–FR-027 y FR-032–FR-042 y verificada contra `MetricCatalog`; `data-model.md` la referencia.
  - Nota de impacto asumida y aceptada por el revisor: un jugador de campo requiere 26 o 27 métricas base, hasta 11 de ellas exclusivas de JSON, por lo que `MISSING_REQUIRED_METRICS` será el motivo dominante y la cobertura puede ser baja sin bloquear la publicación (FR-058, SC-010).
- [x] CHK002 ¿Está definido para cada métrica de las fórmulas si se usa su valor directo, derivado, normalizado, invertido o penalizado? [Completeness, Spec §FR-012–FR-018, §FR-024–FR-043]
  - Evidencia: FR-012 y FR-013 (métricas derivadas y sus claves base canónicas: `goals_total`, `assists_total`, `yellow_cards_total`, `red_cards_total`), FR-024–FR-028 y FR-032–FR-042 («todo normalizado» y nomenclatura `normalizedX`), FR-021 y las fórmulas de penalización, y FR-018 (ninguna métrica se invierte; el conjunto de métricas de peor rendimiento queda enumerado y solo participa como penalización).
- [x] CHK003 ¿Los requisitos identifican de forma exhaustiva cuáles métricas admiten respaldo SCRAPING y cuáles son exclusivas de JSON, sin depender de conocimiento implícito de SPEC-012? [Completeness, Spec §FR-002–FR-005]
  - Evidencia: FR-005 enumera las 15 claves exclusivas de JSON y las 3 exclusivas de scraping, verificadas contra `MetricCatalog` (60 claves, 57 con JSON, 45 con scraping, 42 compartidas).
- [x] CHK004 ¿Están definidos todos los componentes persistidos necesarios para reconstruir ambas estrategias, incluida la nulabilidad específica de arqueros y jugadores de campo? [Completeness, Spec §FR-046–FR-052]
  - Evidencia: FR-046/FR-047 y `data-model.md` (tablas `PositionalValuation` y `AttributeValuation`, con nulabilidad exclusiva `performancePenalty`/`performanceAdjustment` para arquero y de los atributos de campo/arquero).
- [x] CHK005 ¿El resumen de evaluación especifica todos los conteos, motivos de exclusión e imputaciones que debe comunicar, incluida la presencia o ausencia de claves con conteo cero? [Completeness, Spec §FR-011, §FR-057]
  - Evidencia: FR-057, `contracts/http.md` (`excludedPlayers` incluye siempre los cuatro motivos aunque el conteo sea cero; `imputedMetricCounts` solo con claves derivadas imputadas, cero implícito en las ausentes) y `data-model.md` §EvaluationSummary.

## Claridad matemática

- [x] CHK006 ¿La precedencia entre respaldo, derivación, imputación, normalización, ponderación, ajuste de confianza, clamp y redondeo está definida en un orden único e inequívoco? [Clarity, Spec §FR-005–FR-006, §FR-012–FR-019, §FR-052]
  - Evidencia: FR-064 declara la secuencia única (campaña → contexto → respaldo → derivación → imputación → normalización → ponderación y ajustes → confianza → clamp → redondeo), alineada con FR-052 (`clamp` antes del redondeo) y `research.md` §3.
- [x] CHK007 ¿Está claro si los pesos de cada fórmula operan sobre percentiles ya normalizados y no sobre valores estadísticos crudos? [Clarity, Spec §FR-024–FR-043]
  - Evidencia: FR-024–FR-028 («todo normalizado») y nomenclatura `normalizedX` en FR-032–FR-042.
- [x] CHK008 ¿Las reglas de inversión distinguen explícitamente cada métrica donde un valor menor representa mejor rendimiento, en vez de dejar esa clasificación a interpretación? [Ambiguity, Spec §FR-018, §FR-024–FR-043]
  - Evidencia: FR-018 enumera el conjunto de métricas cuyo valor menor es mejor (`offsides_per_match`, `dispossessed_per_match`, `bad_controls_per_match`, `dribbled_past_per_match`, `fouls_committed_per_match`, `yellow_cards_total`, `red_cards_total`), declara que todas participan solo como penalización con percentil creciente y prohíbe el uso de `1 - normalizedMetric`.
- [x] CHK009 ¿Las reglas de mediana definen inequívocamente la población donante, el tratamiento de ausencias, denominadores válidos, cantidades pares y precisión previa al redondeo? [Clarity, Spec §FR-012, §FR-014–FR-015]
  - Evidencia: FR-012, FR-014 (donantes con denominador > 0, misma posición/liga/temporada, promedio de los dos centrales con precisión completa) y FR-015 (sin donantes → 0.5).
- [x] CHK010 ¿La definición de confianza aclara la unidad de `minutesPlayed`, los mínimos aplicables a cada posición y el resultado cuando los minutos son cero? [Clarity, Spec §FR-019, Edge Cases]
  - Evidencia: FR-019 (`min(minutesPlayed / minimumMinutes, 1)`, 270 campo / 540 arquero), FR-012 (la unidad es minutos, no minutos por partido) y Edge Cases (minutos cero → confianza 0 → resultado final 0.5).
- [x] CHK011 ¿Los límites de interpretación del score cubren sin solapamientos ni huecos todo el intervalo `[0,1]`, incluidos exactamente `0.3`, `0.5`, `0.7`, `0` y `1`? [Clarity, Spec §FR-029]
  - Evidencia: FR-029 define los cinco intervalos con extremos inclusivos explícitos y sin solapamientos ni huecos.

## Consistencia interna

- [x] CHK012 ¿La regla de participación por métrica de jugadores finalmente excluidos es consistente entre población, percentiles, criterios de elegibilidad y Success Criteria? [Consistency, Spec §FR-010, §FR-016–FR-017, §SC-003]
  - Evidencia: FR-016, FR-017 y SC-003 exprimen la misma regla; Edge Cases lo reafirma.
- [x] CHK013 ¿La imputación neutra `0.5` sin donantes es consistente con la regla general de que una ausencia no participa como cero y con el registro en `imputed_metrics`? [Consistency, Spec §FR-007, §FR-015–FR-016]
  - Evidencia: FR-015 registra igualmente en `imputed_metrics`; FR-016 y Edge Cases prohíben que una ausencia ingrese como cero; `research.md` §3 precisa que el valor neutro no se mezcla como observación cruda en percentiles.
- [x] CHK014 ¿La ausencia de `performancePenalty` y `performanceAdjustment` para arqueros es consistente en fórmula, persistencia, nulabilidad y criterios de aceptación? [Consistency, Spec §FR-027–FR-028, §FR-046, US1 escenario 4]
  - Evidencia: FR-027, FR-046, `data-model.md` (`Nulo solo para arquero`) y US1 escenario 4.
- [x] CHK015 ¿Las reglas de disciplina son coherentes entre ambas estrategias y evitan inequívocamente duplicar faltas o tarjetas dentro de la penalización de rendimiento? [Consistency, Spec §FR-020–FR-021, §FR-043]
  - Evidencia: FR-021 (disciplina para las cuatro posiciones y prohibición explícita de repetir faltas o tarjetas en la penalización de rendimiento) y FR-043 (misma `disciplineAdjustment` para el GRL).
- [x] CHK016 ¿Los nombres y versiones constantes de estrategia son idénticos en requisitos, entidades clave, persistencia y límites de configurabilidad? [Consistency, Spec §FR-023, §FR-046–FR-047]
  - Evidencia: `POSITIONAL_PERFORMANCE` v1 y `GENERAL_ATTRIBUTES` v1 en FR-023, FR-046, FR-047, `data-model.md` (CHECK de constantes) y `research.md` §9.

## Calidad de criterios de aceptación

- [x] CHK017 ¿Cada fórmula y regla de redondeo tiene un resultado objetivamente calculable a partir de entradas conocidas, sin términos cualitativos no definidos? [Measurability, Spec §FR-012–FR-045, §FR-052]
  - Evidencia: todas las fórmulas son numéricas y cerradas; los ajustes discretos (FR-020, FR-036, FR-037) fijan valores iniciales configurables; «ajustar hacia 0.5» queda definido por la fórmula de `finalScore`/`finalGRL` (FR-028, FR-043).
- [x] CHK018 ¿Los Success Criteria distinguen resultados publicados, jugadores excluidos y fallos inesperados de manera medible y compatible con el resumen? [Acceptance Criteria, Spec §SC-001–SC-010, §FR-057]
  - Evidencia: SC-007, SC-010 y FR-057/contrato HTTP con `evaluatedPlayers`, `excludedPlayers` y `failedPlayers` diferenciados.
- [x] CHK019 ¿El criterio de reconstrucción identifica evidencia suficiente para determinar entradas, procedencia, imputaciones, componentes, versión y fecha sin exigir datos no persistidos? [Measurability, Spec §FR-008, §FR-052, §SC-009]
  - Evidencia: FR-008, FR-052 y SC-009; `data-model.md` documenta que la reconstrucción usa `(player, sourceExtractionDate, source)` y el catálogo, sin FK a filas concretas ni duplicación de entradas.
- [x] CHK020 ¿La idempotencia está expresada con criterios observables para reevaluados, jugadores que dejan de ser elegibles, fallidos y ausencia de duplicados? [Acceptance Criteria, Spec §FR-048–FR-049, §SC-006]
  - Evidencia: FR-048, FR-049, FR-056 (limpieza ante fallo individual) y SC-006.

## Cobertura de escenarios

- [x] CHK021 ¿Los requisitos cubren claramente el flujo primario completo desde la selección del snapshot confirmado hasta la publicación conjunta y el resumen final? [Coverage, Spec §FR-004, §FR-046–FR-058, US3]
  - Evidencia: FR-004, FR-046–FR-058, US3 y `data-model.md` §Flujo y transiciones.
- [x] CHK022 ¿Están cubiertos los flujos alternativos JSON puro, SCRAPING como respaldo y procedencia `MIXED`, incluido el caso de cero JSON válido? [Coverage, Spec §FR-005–FR-008, US1 escenarios 2–3]
  - Evidencia: FR-005–FR-008, Edge Cases (cero conservado, `MIXED`) y FR-058 (ausencia de campaña JSON confirmada → rechazo controlado `409`, no evaluación vacía).
- [x] CHK023 ¿Cada motivo funcional de exclusión tiene condiciones de entrada no solapadas y una prioridad definida cuando un jugador satisface más de un motivo? [Ambiguity, Spec §FR-010–FR-011, Edge Cases]
  - Evidencia: FR-065 fija el orden de severidad (`UNRESOLVED_LEAGUE_CONTEXT` → `MISSING_OR_UNCONFIRMED_SNAPSHOT` → `MISSING_REQUIRED_METRICS` → `NO_APPEARANCES`) y la contabilización en un único motivo; FR-010 y FR-011 lo referencian, y `data-model.md` y `contracts/http.md` lo reflejan.
- [x] CHK024 ¿El fallo individual está especificado para las etapas relevantes de cálculo y persistencia, incluyendo limpieza, continuidad, conteo y no exposición de la causa? [Coverage, Exception Flow, Spec §FR-049, §FR-056–FR-057]
  - Evidencia: FR-056, FR-057, `research.md` §5 (transacción por jugador con retirada de ambas filas) y contrato HTTP (la causa interna no se expone).
- [x] CHK025 ¿Los requisitos distinguen con claridad los fallos individuales recuperables de los fallos globales que impiden completar el recorrido y emitir un resumen exitoso? [Clarity, Exception Flow, Spec §FR-056–FR-058]
  - Evidencia: FR-057 («una vez iniciado y completado el recorrido… responde exitosamente»), FR-058 (rechazo previo) y contrato HTTP con `409`/`500` globales.

## Cobertura de casos límite

- [x] CHK026 ¿Están definidos los resultados para población unitaria, todos los valores empatados, todos los valores cero y ausencia total de donantes dentro de un grupo? [Edge Case, Spec §FR-015, §FR-017, Edge Cases]
  - Evidencia: FR-017 (población unitaria 0.5 y empates con el mismo resultado), Edge Cases (el cero es valor válido) y FR-015 (sin donantes → 0.5).
- [x] CHK027 ¿Está definido qué ocurre cuando liga o temporada están ausentes o no resueltas, sin crear poblaciones artificiales ni mezclar contextos? [Edge Case, Spec §FR-004, §FR-010–FR-011]
  - Evidencia: FR-004 (liga y temporada resueltas en el snapshot, sin mezclar contextos), Edge Cases (`UNRESOLVED_LEAGUE_CONTEXT`) y `data-model.md` (`contextStatus = RESOLVED` obligatorio para evaluar).
- [x] CHK028 ¿La clasificación de todas las posiciones específicas admitidas por el catálogo hacia las cuatro posiciones generales está definida o referenciada sin categorías residuales ambiguas? [Completeness, Spec §FR-009]
  - Evidencia: FR-009 fija el conjunto cerrado `Goalkeeper`/`Defender`/`Midfielder`/`Forward` a partir de la posición persistida del jugador, sin variantes ni subposiciones; `data-model.md` §GeneralPosition replica el criterio.
- [x] CHK029 ¿Está especificado el tratamiento de una posición inesperada sin introducir silenciosamente una quinta posición o un motivo de exclusión no contractual? [Gap, Spec §FR-009, §FR-011]
  - Evidencia: `data-model.md` §GeneralPosition: una posición inesperada es un fallo individual controlado y no crea quinta categoría ni nueva causa funcional de exclusión (coherente con FR-009/FR-011).
- [x] CHK030 ¿La repetición en la misma fecha UTC y el cruce de medianoche tienen una semántica única y consistente para `valuationDate` y `sourceExtractionDate`? [Edge Case, Spec §FR-048–FR-055, Assumptions]
  - Evidencia: Assumptions (`valuationDate` es la fecha UTC de inicio y se conserva aunque la operación cruce medianoche), FR-004 (una única fecha de extracción común) y FR-048–FR-049.

## Requisitos no funcionales y límites

- [x] CHK031 ¿El requisito de búsquedas eficientes identifica patrones y ordenamientos concretos sin prometer un SLA inexistente ni dejar “eficiente” como término subjetivo? [Clarity, Non-Functional, Spec §FR-050–FR-051]
  - Evidencia: FR-051 fija fecha descendente, resultado final y jugador con fecha descendente; `data-model.md` concreta los índices y `plan.md` declara que la spec no fija SLA temporal.
- [x] CHK032 ¿Los requisitos de seguridad definen de forma suficiente que la operación reutiliza autenticación y autorización de SPEC-012, sin introducir roles o excepciones implícitas? [Coverage, Security, Spec §FR-053]
  - Evidencia: FR-053, `contracts/http.md` (JWT Bearer y usuario persistido, sin rol adicional) y `research.md` §8.
- [x] CHK033 ¿La ausencia contractual de concurrencia está delimitada sin insinuar garantías de exclusión, espera o rechazo que la feature no promete? [Scope, Assumption, Spec Clarifications, Assumptions]
  - Evidencia: Clarification Q3, Assumptions (exclusión mutua, espera o rechazo fuera del contrato) y `contracts/http.md` (no se define error de concurrencia).
- [x] CHK034 ¿Los límites excluyen de forma consistente scheduler, consultas, ranking, estrategia activa, cotización, mercado y cambios de extracción en requisitos, Success Criteria y Out of Scope? [Consistency, Scope, Spec §FR-059–FR-063, §SC-011, Out of Scope]
  - Evidencia: FR-059–FR-063, SC-011 y la sección Out of Scope son consistentes entre sí.

## Dependencias y supuestos

- [x] CHK035 ¿La noción de “campaña JSON confirmada con cobertura poblacional completa” está definida mediante un contrato verificable de SPEC-012 y no mediante la mera existencia de una fecha o un run? [Dependency, Spec §FR-004, §FR-058, Verificaciones pendientes]
  - Evidencia: FR-004 define el criterio de confirmación (items `COMPLETED` de la extracción JSON de la fecha y filas `player_statistics` JSON que cubren el catálogo elegible) y descarta fecha, run o filas parciales como acreditación; FR-058 lo referencia, `contracts/http.md` lo aplica al error `409` y la sección Verificaciones pendientes refleja el cierre.
- [x] CHK036 ¿El supuesto de cobertura 57/45/42 y catálogo cerrado de 60 claves está referenciado de forma consistente con SPEC-012 y con todas las métricas citadas por las fórmulas? [Dependency, Consistency, Spec §FR-002–FR-003, Assumptions]
  - Evidencia: FR-002/FR-003 y Assumptions coinciden con `MetricCatalog` (60 claves, 45 con scraping, 57 con JSON, 42 compartidas, verificado en el catálogo de SPEC-012) y todas las métricas citadas por las fórmulas existen en el catálogo (`goals_total`, `assists_total`, `yellow_cards_total`, `red_cards_total`, `minutes_played_total`, `appearances_total`, `shots_per_match`, `pass_completion_percentage`, `passes_per_match`, `key_passes_per_match`, `successful_dribbles_per_match`, `successful_long_balls_per_match`, `successful_crosses_per_match`, `tackles_per_match`, `interceptions_per_match`, `clearances_per_match`, `blocked_passes_per_match`, `blocked_crosses_per_match`, `aerial_duels_won_per_match`, `total_aerial_duels_per_match`, `failed_dribbles_per_match`, `total_dribbles_per_match`, `saves_per_match`, `outside_box_saves_per_match`, `outside_box_goals_per_match`, `dribbled_past_per_match`, `offsides_per_match`, `dispossessed_per_match`, `bad_controls_per_match`, `fouls_committed_per_match`).
- [x] CHK037 ¿Está documentado qué identidad de liga y temporada gobierna cuando los datos JSON y SCRAPING de la misma fecha difieren en contexto? [Clarity, Dependency, Spec §FR-004–FR-005, Assumptions]
  - Evidencia: FR-004 (la liga y temporada locales son las resueltas en el snapshot primario), FR-005 y Assumptions (el respaldo no sustituye el contexto primario).
- [x] CHK038 ¿Los requisitos dejan claro que la valoración estadística de SPEC-013 termina en scores y GRL, sin exigir ni definir conversión a cotización o precio? [Scope, Spec §FR-062, Out of Scope]
  - Evidencia: FR-062, Out of Scope y Clarification Q5 (la selección de estrategia activa corresponde a la feature futura de cotización).

## Ambigüedades y conflictos

- [x] CHK039 ¿La expresión “ambas estrategias se publican de forma coherente” define suficientemente la atomicidad esperada ante fallos de escritura y limpieza? [Ambiguity, Spec Edge Cases, Assumptions]
  - Evidencia: Edge Cases (no puede quedar una sola estrategia vigente), Assumptions (unidad coherente), FR-049/FR-056 y `research.md` §5 (transacción por jugador que publica o retira ambas filas).
- [x] CHK040 ¿Existe una definición única de “resultado vigente” compatible con reemplazo por fecha, última fecha disponible y futuras consultas que no recalculan? [Ambiguity, Spec §FR-048–FR-050]
  - Evidencia: FR-048 (máximo una fila por estrategia, jugador y fecha), FR-049 (retirada de resultados obsoletos) y FR-050 (consulta de la última fecha disponible sin recalcular).
- [x] CHK041 ¿Las claves presentadas en `imputed_metrics` tienen nombres canónicos inequívocos y consistentes con las métricas derivadas descritas en la spec? [Clarity, Spec §FR-012–FR-015, §FR-046–FR-047]
  - Evidencia: las claves derivadas de FR-012/FR-013 (`goalsPer90`, `assistsPer90`, `yellowCardsPer90`, `redCardsPer90`, `dribbleSuccess`, `aerialWinRate`, `saveOutOfBox`, `blocksPerMatch`) se usan de forma uniforme en el spec, en `data-model.md` (`imputedMetrics` con claves únicas y ordenadas) y en el ejemplo del contrato HTTP (`goalsPer90`, `dribbleSuccess`).

## Notes

- Marcar ítems `[x]` solo después de que la revisión confirme que el criterio de calidad de requisitos está satisfecho.
- Dejar sin marcar los ítems que todavía requieran aclaración, corrección o evaluación del revisor.
- `/speckit.implement` lee el estado del checklist como gate y no debe modificar sus marcadores.
- `checklists/requirements.md` tiene un ciclo separado, mantenido por `/speckit.specify` y `/speckit.clarify`.
- Agregar hallazgos o referencias junto al ítem correspondiente.
- Este checklist excluye deliberadamente la definición futura de la fórmula de cotización y precio de token; solo revisa que permanezca fuera del alcance de SPEC-013.
- Resultado de la revisión 2026-10-19 (1ª pasada): 33 ítems marcados `[x]` y 8 pendientes (CHK001, CHK002, CHK003, CHK006, CHK008, CHK023, CHK028, CHK035), todos con hallazgo y recomendación asociados. Los pendientes se concentran en enumeraciones explícitas (métricas por posición, claves JSON-only, métricas invertidas, taxonomía de posiciones) y en el orden de pipeline y la prioridad de exclusiones; ninguno indica contradicción interna entre requisitos.
- Resultado de la revisión 2026-10-19 (2ª pasada, tras decisiones del revisor): se aplicaron al spec los criterios cerrados — claves canónicas del catálogo en las fórmulas (FR-012, FR-021), enumeración de las 15 claves JSON-only y las 3 solo-scraping (FR-005), orden único de etapas (FR-064), prioridad fija de exclusiones con motivo único (FR-065, FR-010, FR-011), taxonomía cerrada de posiciones persistidas (FR-009), definición completa de la cobertura poblacional confirmada (FR-004, FR-058) y tabla cerrada de métricas requeridas por posición (FR-066). Estado final: 41 ítems `[x]`, ninguno pendiente.

# Research: Valoraciones estadísticas de jugadores

## 1. Campaña vigente confirmada

**Decision**: Seleccionar la mayor `extraction_date` de canal `JSON` cuya campaña esté confirmada. La confirmación exige run terminado (`COMPLETED` o `PARTIAL`), ausencia de items en fallo estructural (`INCOMPLETE`, `INTERRUPTED`, `BLOCKED`, `STRUCTURE_CHANGED`) y fila `player_statistics` JSON para todo item `COMPLETED`. Los items `NO_DATA` y `NOT_FOUND` no invalidan la campaña y los fallos de proveedor sobre un jugador concreto tampoco; esos jugadores quedan sin snapshot y se excluyen como `MISSING_OR_UNCONFIRMED_SNAPSHOT` durante el recorrido. Si no existe ninguna campaña confirmada, se rechaza antes de modificar valoraciones.

**Rationale**: SPEC-012 no persiste una tabla de cobertura. La cobertura se deriva del catálogo elegible, los items de la campaña y las filas de campaña. Exigir que todos los items fueran `COMPLETED` hacía la evaluación inaccesible en la práctica: una fracción estructural de la población (jugadores sin ficha en el proveedor) nunca alcanza ese estado. La cobertura se resuelve por jugador, donde ya existe un motivo de exclusión normativo, y así la ausencia de datos nunca se computa como cero ni como población artificial.

**Alternatives considered**: Usar la fecha del último run (rechazada porque un run puede estar en curso o interrumpido); usar la máxima fecha de `player_statistics` (rechazada porque no acredita integridad de la campaña); exigir cobertura total del catálogo elegible (rechazada porque bloquea indefinidamente campañas con `NO_DATA` legítimos); persistir un nuevo snapshot de entrada (rechazada por duplicación).

## 2. Reconstrucción de procedencia

**Decision**: Cargar la fila JSON primaria y, para las 42 claves compartidas de `MetricCatalog`, la fila SCRAPING del mismo jugador y fecha. Elegir JSON cuando no sea nulo, SCRAPING solo ante nulo y conservar un mapa efímero de fuente por valor utilizado. Persistir únicamente la procedencia agregada `JSON`, `SCRAPING` o `MIXED`, la fecha y las imputaciones.

**Rationale**: La clave del catálogo declara canales admitidos y las filas conservan canal, fecha y jugador. Esa combinación reconstruye cada entrada sin copiar 60 valores ni crear auditoría paralela.

**Alternatives considered**: Persistir fuente por métrica (duplicación innecesaria); preferir scraping por actualidad (contradice la primacía JSON); permitir scraping sin fila JSON (contradice el snapshot primario obligatorio).

## 3. Precisión, derivación e imputación

**Decision**: Usar `BigDecimal` en todo el pipeline. Mantener precisión intermedia suficiente para divisiones y medianas; aplicar `clamp` antes de persistir y redondear cada decimal persistido a escala 6 con `RoundingMode.HALF_UP`. Las medianas usan donantes con denominador mayor que cero de igual posición, liga y temporada; sin donantes se inyecta el valor normalizado 0.5.

**Rationale**: Evita error binario, satisface FR-012–FR-017 y hace determinista la reconstrucción. El valor neutro sin donantes no se mezcla como observación cruda en percentiles.

**Alternatives considered**: `double` (rechazado por precisión); redondear cada paso (rechazado por acumulación de error); persistir tasas imputadas como estadísticas fuente (rechazado porque altera SPEC-012).

## 4. Normalización por percentil

**Decision**: Construir una población por `(generalPosition, leagueId, seasonKey, metricKey)` con todos los valores disponibles tras fallback, aunque el jugador sea después excluido. Ordenar valores y calcular rango medio `(menores + 0.5 × iguales) / total`. Las métricas cuyo valor menor representa mejor rendimiento conservan su percentil creciente y participan exclusivamente como penalizaciones; no se calcula `1 - normalizedMetric`.

**Rationale**: Implementa directamente FR-016–FR-018, trata empates de forma estable y desacopla disponibilidad por métrica de elegibilidad final.

**Alternatives considered**: Min-max (sensible a extremos y no solicitado); ranking ordinal (trata empates de forma arbitraria); población solo de elegibles finales (excluye datos válidos contra FR-016).

## 5. Límite transaccional y fallos individuales

**Decision**: Preparar poblaciones en memoria y publicar cada jugador en una transacción independiente que elimina/reemplaza coherentemente sus dos estrategias. Antes del recorrido, limpiar para la `valuationDate` los resultados que no correspondan al snapshot vigente; ante fallo individual, una operación transaccional separada retira ambas filas del jugador y registra un log estructurado con jugador, etapa y causa saneada.

**Rationale**: Permite continuar sin una transacción poblacional larga, impide una sola estrategia vigente y satisface idempotencia y retirada de resultados obsoletos.

**Alternatives considered**: Una transacción global (un fallo revierte toda la población); una transacción por estrategia (permite pares incompletos); tabla de runs (fuera de alcance y no requerida).

## 6. Persistencia e índices

**Decision**: Usar dos tablas, una por forma de resultado, ambas con FK a jugador, fecha de valoración, estrategia/version constantes, fecha fuente, posición, procedencia, componentes reconstruibles e imputaciones como array de claves controladas. Agregar unicidad `(player_id, valuation_date, strategy_name, strategy_version)` e índices para fecha descendente, resultado final y jugador/fecha descendente.

**Rationale**: Las estrategias tienen componentes y nulabilidad distintos. Dos tablas evitan una entidad polimórfica ancha y constraints condicionales complejos, manteniendo consultas futuras directas.

**Alternatives considered**: Una tabla única (muchas columnas semánticamente nulas); JSONB para componentes (debilita esquema e índices); modelo EAV (complejidad y peor contrato).

## 7. Arquitectura del caso de uso

**Decision**: Exponer una interfaz `ValuationEvaluationService` con implementación única. Componer internamente selección, fallback, normalización y publicación en `services/internal/`; mantener fórmulas e invariantes puras en Model. No crear Orchestrator ni Integration.

**Rationale**: Solo existe un caso de uso de aplicación; no coordina Services independientes ni accede a sistemas externos. Respeta la arquitectura backend y permite probar fórmulas sin Spring.

**Alternatives considered**: Un Service por etapa (interfaces sin consumidor y coordinación artificial); Orchestrator (no coordina casos de uso); Integration para SPEC-012 (la fuente es persistencia local, no sistema externo).

## 8. Contrato HTTP y seguridad

**Decision**: `POST /api/valuations/evaluate` sin body, query ni identificación de jugador; JWT Bearer y usuario persistido, igual que SPEC-012. Respuesta `200` con resumen agregado. Ausencia de snapshot confirmado se traduce a `409 VALUATION_SNAPSHOT_UNAVAILABLE`; fallos globales controlados a `500 VALUATION_EVALUATION_FAILED`.

**Rationale**: La operación actúa sobre toda la población y no admite configuración del usuario. `409` expresa que el estado previo requerido aún no existe sin presentar un fallo de validación del request.

**Alternatives considered**: `404` (no falta un recurso direccionado); `422` (la entrada es válida); body con fecha o estrategia (amplía el contrato y contradice el alcance).

## 9. Configuración operativa

**Decision**: Declarar properties validadas para `minimumMinutes.field=270`, `minimumMinutes.goalkeeper=540`, `performanceMaximumImpact=0.10`, `disciplineMaximumImpact=0.05` y `attributePenaltyMaximumImpact=0.10`. Las métricas, fórmulas y ponderaciones se configuran al definir cada estrategia y después permanecen constantes en código para su versión; cambiar pesos requiere una nueva versión.

**Rationale**: La aclaración docente distingue la configuración que define estrategias diferentes de la edición operativa de una estrategia ya versionada. Solo estos umbrales e impactos admiten configuración operativa; externalizar los pesos de `v1` permitiría alterar resultados bajo la misma versión y debilitaría su trazabilidad.

**Alternatives considered**: Hardcodear todo (ignora configurabilidad expresa); configuración completa de estrategias (contradice FR-023); tablas editables (complejidad y administración no solicitadas).

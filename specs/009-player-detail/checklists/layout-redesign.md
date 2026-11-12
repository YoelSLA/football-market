# Checklist de calidad del rediseño: SPEC-009

**Feature**: [spec.md](../spec.md), [plan.md](../plan.md), [player-statistics.md](../contracts/player-statistics.md) y [wireframe canónico](../../../docs/wireframes/player-detail.md)
**Propósito**: Evaluar completitud, claridad, consistencia y medibilidad de los requisitos visuales vigentes.

---

## 1. Completitud del alcance visual

- [x] CHK001 ¿Están determinadas la jerarquía, el contenido y el orden de la cabecera compacta sin sustituir identidad, temporada ni contexto ya existentes? [Completeness, Spec §FR-005–FR-007 y Wireframe §Cabecera]
- [x] CHK002 ¿Está especificado que Resumen contiene exactamente cuatro tarjetas KPI y dos paneles temáticos posicionales sin barras arbitrarias ni duplicados? [Completeness, Spec §FR-010–FR-012 y Wireframe §Resumen]
- [x] CHK003 ¿Las variantes de arquero, defensor, mediocampista y delantero identifican inequívocamente las métricas de tarjetas y los dos paneles correspondientes? [Clarity, Spec §Perfiles posicionales y Wireframe §Variantes]
- [x] CHK004 ¿Está definido que los paneles temáticos desarrollan los indicadores clave de cada posición con espaciado armónico y filas acotadas (44–58 px)? [Clarity, Spec §FR-010/SC-004]
- [x] CHK005 ¿La ausencia de radar, percentiles, finanzas en el resumen deportivo y métricas ajenas queda delimitada sin retirar capacidades compartidas fuera del detalle? [Completeness, Spec §FR-012 y Out of Scope]

---

## 2. Claridad del catálogo completo

- [x] CHK006 ¿Está fijado que Todas las estadísticas conserva exactamente las 60 claves canónicas una vez cada una, incluidas las ausentes? [Clarity, Spec §FR-013 y SC-005]
- [x] CHK007 ¿Las ocho categorías, la pertenencia única de cada clave y el orden por relevancia/disponibilidad están definidos sin ambigüedades? [Completeness, Spec §FR-014/FR-016]
- [x] CHK008 ¿La búsqueda, la intersección con categoría, el conteo dinámico, el estado sin coincidencias y la limpieza de filtros tienen reglas inequívocas? [Clarity, Spec §FR-015 y US5]
- [x] CHK009 ¿Está expresado que búsqueda y categorías viven en el encabezado del catálogo y que no se requiere panel lateral permanente ni filtro de unidad? [Consistency, Spec §FR-015 y Wireframe §Todas las estadísticas]
- [x] CHK010 ¿La unidad visible y el valor ausente permanecen representados por métrica sin introducir valores reconstruidos? [Consistency, Spec §FR-013/FR-016]

---

## 3. Consistencia entre artefactos

- [x] CHK011 ¿SPEC, PLAN, modelo y contrato coinciden en que la presentación reutiliza la consulta, estados, perfiles y catálogo existentes? [Consistency, Spec §Clarifications, Plan §Summary, Contrato §player-statistics]
- [x] CHK012 ¿La referencia al wireframe canónico es uniforme y prevalece sobre antecedentes históricos cuando existe una diferencia estructural? [Consistency, Spec §Trazabilidad, Plan §Frontend, Wireframe §Alcance]
- [x] CHK013 ¿Los artefactos distinguen el inventario canónico de 60 claves de la disponibilidad real por jugador? [Consistency, Spec §FR-013 y contracts/metric-glossary.md]
- [x] CHK014 ¿La regla de cero válido, ausencia, carga, error y respuesta anterior compatible conserva la misma semántica en SPEC, modelo y contratos? [Consistency, Spec §FR-018/FR-019, Modelo §Estado de interacción]
- [x] CHK015 ¿La continuidad del contrato HTTP y la exclusión de cambios backend innecesarios están documentadas con precisión? [Consistency, Plan §Summary, Contrato §player-statistics]

---

## 4. Calidad de criterios de aceptación

- [x] CHK016 ¿SC-004 permite medir la composición de cada variante posicional sin ambigüedades? [Acceptance Criteria, Spec §SC-004]
- [x] CHK017 ¿SC-005 define resultados observables para agrupación, controles superiores, conteos y acceso a las 60 claves? [Acceptance Criteria, Spec §SC-005]
- [x] CHK018 ¿SC-008 permite evaluar los anchos de 360, 768 y 1280+ px y la regla de cero scroll en escritorio? [Measurability, Spec §SC-008]
- [x] CHK019 ¿SC-003 y SC-004 delimitan objetivamente la presentación de cotizaciones oficiales y KPI deportivos? [Acceptance Criteria, Spec §SC-003/SC-004]

---

## 5. Escenarios, estados y adaptación

- [x] CHK020 ¿Los requisitos cubren carga inicial, error recuperable, ausencia total (`NO_DATA`), datos parciales (`PARTIAL`) y refetch con respuesta compatible? [Coverage, Spec §Edge Cases, FR-018/FR-019]
- [x] CHK021 ¿Está determinado qué contenido permanece visible y qué mensaje corresponde cuando un indicador carece de datos suficientes? [Coverage, Spec §US2, FR-010/FR-018]
- [x] CHK022 ¿El comportamiento responsive especifica orden semántico, apilado, ausencia de overflow horizontal y cero scroll en escritorio? [Completeness, Spec §FR-023/FR-024]
- [x] CHK023 ¿Los requisitos de teclado, foco, selección perceptible y alternativas textuales cubren pestañas, búsqueda, categorías y estados? [Coverage, Spec §FR-022 y US4]
- [x] CHK024 ¿Nombres largos, posición desconocida, búsqueda vacía y fallos de imagen están tratados sin ocultar navegación ni contexto? [Edge Case, Spec §Edge Cases]

---

## 6. Dependencias, supuestos y límites

- [x] CHK025 ¿Está documentado que los perfiles posicionales y las 60 claves son dependencias conservadas y normativas? [Dependency, Spec §FR-010/FR-013]
- [x] CHK026 ¿Los artefactos excluyen rutas no autorizadas, scraping en tiempo real o finanzas dentro del resumen deportivo? [Scope, Spec §Out of Scope]
- [x] CHK027 ¿La documentación viva delega el registro de cambios e historial a Git sin arrastrar antecedentes redundantes? [Traceability, Spec §Trazabilidad]

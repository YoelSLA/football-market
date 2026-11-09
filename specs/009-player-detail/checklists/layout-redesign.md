# Checklist de calidad del rediseño: SPEC-009

**Purpose**: Evaluar completitud, claridad, consistencia y medibilidad de los requisitos del rediseño antes de TASKS; no verifica la implementación.
**Created**: 2026-11-09
**Feature**: [spec.md](../spec.md), [plan.md](../plan.md), [contrato de presentación](../contracts/player-statistics.md) y [wireframe canónico](../../../docs/wireframes/player-detail.md)
**Profundidad y destinatario**: estándar; revisor de requisitos en el gate CHECKLIST previo a TASKS.

**Note**: Este checklist fue generado por `/speckit.checklist` a partir del alcance vigente de SPEC-009.
**Review Ownership**: Este artefacto pertenece al revisor. Marcar un criterio `[x]` solo cuando el revisor determine que la calidad de requisitos es satisfactoria.
**Marker Semantics**: `[x]` significa aprobación de calidad documental; no acredita implementación, pruebas ni revisión visual del producto.

## Completitud del alcance visual

- [ ] CHK001 ¿Están determinadas la jerarquía, el contenido y el orden de la cabecera compacta sin sustituir identidad, temporada ni contexto ya existentes? [Completeness, Spec §FR-049 y Wireframe §Cabecera]
- [ ] CHK002 ¿Está especificado que Resumen contiene exactamente cuatro tarjetas, dos paneles posicionales y un único bloque de destacadas para las cuatro posiciones? [Completeness, Spec §FR-030/FR-050 y Wireframe §Resumen]
- [ ] CHK003 ¿Las variantes de arquero, defensor, mediocampista y delantero identifican inequívocamente las métricas de tarjetas y los dos paneles correspondientes? [Clarity, Spec §Perfiles posicionales y Wireframe §Variantes]
- [ ] CHK004 ¿Está definido que las destacadas excluyen claves presentes en tarjetas o paneles, sin prohibir que un panel desarrolle una métrica introducida por una tarjeta? [Clarity, Spec §FR-030/SC-025]
- [ ] CHK005 ¿La ausencia de radar, percentiles, finanzas, paneles genéricos y métricas ajenas queda delimitada sin retirar capacidades compartidas fuera del detalle? [Completeness, Spec §FR-032/FR-037/FR-040]

## Claridad del catálogo completo

- [ ] CHK006 ¿Está fijado que Todas las estadísticas conserva exactamente las 60 claves canónicas una vez cada una, incluidas las ausentes? [Clarity, Spec §FR-033/FR-043 y SC-013]
- [ ] CHK007 ¿Las ocho categorías, la pertenencia única de cada clave y el orden por relevancia/disponibilidad están definidos sin depender del diseño anterior? [Completeness, Spec §FR-034/FR-042]
- [ ] CHK008 ¿La búsqueda, la intersección con categoría, el conteo, el estado sin coincidencias y la limpieza de filtros tienen reglas inequívocas? [Clarity, Spec §FR-035 y US5]
- [ ] CHK009 ¿Está expresado que búsqueda y categorías viven en el encabezado del catálogo y que no se requiere panel lateral permanente ni filtro de unidad? [Consistency, Spec §FR-035/FR-051 y Wireframe §Todas las estadísticas]
- [ ] CHK010 ¿La unidad y el valor ausente permanecen visibles por métrica sin introducir un segundo criterio de filtrado o valores reconstruidos? [Consistency, Spec §FR-031/FR-033/FR-036]

## Consistencia entre artefactos

- [ ] CHK011 ¿SPEC, PLAN, modelo y contrato coinciden en que el cambio reorganiza la presentación y reutiliza consulta, estados, perfiles y catálogo existentes? [Consistency, Spec §Clarifications; Plan §Summary; Contrato §Contrato de presentación]
- [ ] CHK012 ¿La referencia al wireframe canónico es uniforme y prevalece sobre capturas o antecedentes históricos cuando existe una diferencia estructural? [Consistency, Spec §Trazabilidad; Plan §Frontend; Wireframe §Alcance]
- [ ] CHK013 ¿Los artefactos distinguen el inventario canónico de 60 claves de la disponibilidad real por jugador y de las 57 claves actualmente cubiertas por JSON? [Consistency, Spec §Trazabilidad/FR-033/FR-043]
- [ ] CHK014 ¿La regla de cero válido, ausencia, carga, error y respuesta anterior compatible conserva la misma semántica en SPEC, modelo y contrato? [Consistency, Spec §FR-029/FR-047; Modelo §Estado de interacción; Contrato §Contrato de presentación]
- [ ] CHK015 ¿La continuidad del contrato HTTP y la exclusión de cambios backend innecesarios están documentadas sin afirmar que capacidades futuras ya estén implementadas? [Consistency, Plan §Summary; Contrato §encabezado]

## Calidad de criterios de aceptación

- [ ] CHK016 ¿SC-025 permite medir la composición de cada variante y la deduplicación de destacadas sin contradecir las repeticiones tarjeta-panel previstas por el wireframe? [Acceptance Criteria, Spec §SC-025]
- [ ] CHK017 ¿SC-027 define resultados observables para agrupación, controles superiores, conteos y acceso a las 60 claves? [Acceptance Criteria, Spec §SC-027]
- [ ] CHK018 ¿SC-026 permite evaluar los anchos 360, 768 y 1280 sin exigir medidas pixel-perfect no aprobadas? [Measurability, Spec §SC-026]
- [ ] CHK019 ¿SC-028 delimita objetivamente qué paneles repetidos desaparecen y cuándo la calificación no debe tener una segunda presentación? [Acceptance Criteria, Spec §SC-028]

## Escenarios, estados y adaptación

- [ ] CHK020 ¿Los requisitos cubren carga inicial, error recuperable, ausencia total, datos parciales y actualización fallida con respuesta compatible sin confundir sus significados? [Coverage, Spec §Edge Cases/FR-029/FR-047; Wireframe §Estados]
- [ ] CHK021 ¿Está determinado qué contenido permanece visible y qué mensaje corresponde cuando una tarjeta, panel o destacada carece de datos suficientes? [Coverage, Spec §US2/FR-030/FR-031; Wireframe §Datos parciales]
- [ ] CHK022 ¿El comportamiento responsive especifica orden semántico, apilado, scroll vertical y ausencia de overflow horizontal para cabecera, tarjetas, paneles y catálogo? [Completeness, Spec §FR-038/FR-050/FR-052; Wireframe §Responsive]
- [ ] CHK023 ¿Los requisitos de teclado, foco, selección perceptible y alternativas textuales cubren pestañas, búsqueda, categorías, estados y visualizaciones? [Coverage, Spec §FR-039 y US4]
- [ ] CHK024 ¿Los nombres largos, posición desconocida, búsqueda vacía y gráficos con base insuficiente están tratados sin ocultar navegación, contexto o métricas disponibles? [Edge Case, Spec §Edge Cases]

## Dependencias, supuestos y límites

- [ ] CHK025 ¿Está documentado que los perfiles posicionales y las 60 claves son dependencias conservadas y que el rediseño no autoriza modificar su semántica? [Dependency, Spec §FR-030/FR-033; Plan §Datos y transformación]
- [ ] CHK026 ¿Los artefactos excluyen rutas, navegación global, dependencias, persistencia, scraping, sincronización y valoración sin dejar una obligación visual sin responsable? [Scope, Spec §FR-027/FR-037/FR-040; Plan §Scope]
- [ ] CHK027 ¿Las referencias históricas obsoletas están subordinadas al alcance vigente sin atribuir sus aprobaciones al rediseño actual? [Traceability, Spec §Trazabilidad; checklists/dashboard.md]

## Notes

- Revisión asistida del agente, 2026-11-09: se contrastaron SPEC, PLAN, research, modelo, contrato y wireframe. Se corrigió una contradicción en la regla de deduplicación: las destacadas no repiten claves de tarjetas o paneles; los paneles sí pueden desarrollar indicadores introducidos en tarjetas cuando el wireframe lo exige.
- Tras esa corrección no se detectaron otras inconsistencias documentales bloqueantes. Esta evaluación asistida no marca criterios ni sustituye la decisión del revisor.
- Marcar criterios `[x]` únicamente después de la revisión humana de calidad de requisitos.
- Dejar sin marcar cualquier criterio que requiera aclaración o corrección adicional.
- `/speckit.implement` lee el estado de los checklists como gate y no modifica sus marcadores.
- `checklists/requirements.md` conserva el ciclo separado de `/speckit.specify` y `/speckit.clarify`.

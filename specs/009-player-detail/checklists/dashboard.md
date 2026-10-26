# Checklist de calidad del dashboard: SPEC-009

**Purpose**: Evaluar requisitos de catálogo, perfiles, interacción y presentación; no comportamiento de implementación.
**Created**: 2026-10-25
**Feature**: [spec.md](../spec.md), [plan.md](../plan.md), [modelo](../data-model.md), [mockup](../../../referencias-imagenes/spec-009-player-detail-statistics-mockup.png).
**Profundidad y destinatario**: estándar; revisor de requisitos antes de Tasks.

**Review Ownership**: Propiedad del revisor. `[x]` indica aprobación de calidad de requisitos, no implementación completa. Todos los criterios generados quedan sin marcar. La evaluación asistida autorizada se expresa mediante resultados documentales, no aprobaciones.

## Completitud de alcance y contenido

- [x] CHK001 ¿Están delimitadas exactamente las dos pestañas, la selección inicial y la conservación del contexto? [Completeness, Spec §FR-026 y US1]
- [x] CHK002 ¿La matriz contiene exactamente las 60 claves, sin duplicados ni categorías superpuestas? [Completeness, Spec §Catálogo completo y SC-020]
- [x] CHK003 ¿Las cuatro tarjetas de cada posición están identificadas inequívocamente y son compatibles con su prioridad? [Clarity, Spec §Perfiles posicionales y FR-030]
- [x] CHK004 ¿Están definidos los gráficos y adicionales por perfil mediante métricas existentes sin cálculos deportivos nuevos? [Completeness, Spec §Perfiles posicionales y FR-031/032/036]
- [x] CHK005 ¿La prioridad y disponibilidad son independientes, incluyendo los valores futuros de las tres métricas hoy ausentes? [Consistency, Spec §FR-042–044 y SC-021]
- [x] CHK006 ¿Las etiquetas españolas y unidades específicas de las 60 claves están completamente determinadas en artefactos del repositorio? [Clarity, Spec §FR-033 y matriz; Plan §metadatos; Gap]

## Interacción, excepciones y recuperación

- [x] CHK007 ¿La búsqueda, intersección con categoría, orden estable y ausencia de coincidencias están especificados? [Clarity, Spec §US5, FR-035/042]
- [x] CHK008 ¿Las ausencias, ceros, datos parciales, posición desconocida y gráficos sin valores están cubiertos sin simulaciones? [Coverage, Spec §Edge Cases; Plan §Resumen; Modelo §Estado de interacción]
- [x] CHK009 ¿Está inequívocamente definida la presentación de valores anteriores si falla una actualización del mismo snapshot? [Coverage, Modelo §Estado de interacción; Spec §FR-029/043; Gap]
- [x] CHK010 ¿La recuperación y el cambio rápido de jugador tienen requisitos que impiden mezclar identidades, contexto y respuestas tardías? [Coverage, Spec §US1/SC-012; Plan §Frontend]

## Consistencia visual, accesibilidad y dependencias

- [x] CHK011 ¿La referencia visual está acotada por exclusiones expresas, viewports medibles y accesibilidad de los controles y gráficos? [Consistency, Spec §Referencia visual, FR-038/039 y SC-017; Plan §Responsive/Accesibilidad]
- [x] CHK012 ¿El retiro de mocks y finanzas está delimitado sin cambiar catálogo, navegación o consumidores compartidos fuera del alcance? [Consistency, Spec §FR-027/037/040; Plan §Archivos afectados]

## Notes — evaluación asistida solicitada

Evaluación documental del agente, 2026-10-25: **10 conformes, 2 parciales, 0 no aplicables**. Todos los marcadores siguen `[ ]`. Profundidad estándar sobre alcance vigente; la aprobación histórica de otro alcance no se extiende a estos criterios.

| Criterios | Resultado documental | Evidencia |
| --- | --- | --- |
| CHK001–005 | Conforme | Dos tabs; matriz de 60 claves en grupos 5/19/10/5/3/10/4/4; cuatro tarjetas por perfil; gráficos/adicionales cerrados; relevancia futura definida |
| CHK006 | Parcial — D-01 | Matriz vigente lista claves y prioridades; SPEC-012 aporta unidad genérica y el plan promete nombres españoles. No existe todavía una tabla normativa completa clave→etiqueta española→unidad visible para las 60, especialmente acumulados que comparten SEASON_TOTAL |
| CHK007–008 | Conforme | Búsqueda parcial sin acentos/mayúsculas y categoría; prioridad con ausentes al final; cero, posición desconocida y barras incompletas especificados |
| CHK009 | Parcial — D-02 | Modelo dice «Refetch conserva contexto propio y avisa error», pero no explicita si retiene valores anteriores visibles, su identificación como no actualizados, ni la relación con error/ready de estadísticas. Spec exige distinguir error de ausencia, sin fijar esa elección |
| CHK010–012 | Conforme | Protección A→B, recuperación independiente, teclado y viewports; mockup limitado sin radar/xG/xA/percentiles; preservación de navbar y catálogo |

**Correcciones recomendadas, no aplicadas:**

- **D-01 (media, claridad):** persistir el glosario completo de 60 nombres y unidades visibles, con una referencia canónica desde los metadatos de presentación; reutilizar traducciones del análisis sin crear claves ni indicadores. No depender del chat como fuente permanente.
- **D-02 (media, recuperación):** acordar ocultar valores durante el error de actualización o conservarlos con advertencia explícita de última lectura; documentar una sola regla en spec y modelo. Siempre prohibir mostrar datos de otro jugador y confundir error con ausencia confirmada.

La correspondencia con el mockup es de jerarquía, no pixel-perfect ni de cifras: el usuario excluyó radar y cálculos no definidos. No se agregan latencias, nuevas métricas, pruebas de frontend ni requisitos opcionales como blockers por iniciativa del agente.

`/speckit.implement` no debe modificar marcadores. La evaluación no verifica producto, no ejecuta tests ni autoriza Tasks/implementación. `.specify/extensions.yml` ausente: sin hooks before/after checklist. Checklist general y especializado de integración se complementan; no se altera `requirements.md` ni la evidencia previa de `requirements-quality.md`.

## Reevaluación tras decisiones del usuario — 2026-10-25

Operación documental separada y autorizada: **12/12 conformes documentalmente; 0 parciales**. Se mantienen las 12 casillas sin marcar; no equivale a aprobación del revisor.

| Criterio antes parcial | Resolución y evidencia | Resultado |
| --- | --- | --- |
| CHK006 / D-01 | Glosario `contracts/metric-glossary.md`: 60 claves, etiqueta española, categoría y unidad visible; FR-048/SC-023 fijan referencia normativa; distinción de tasa, acumulado, porcentaje y calificación | Conforme |
| CHK009 / D-02 | FR-047/SC-024, modelo y contrato definen conservación de última respuesta exitosa únicamente del mismo jugador/temporada, fecha original, error visible, advertencia y reintento; sin previa compatible hay error sin valores; respuesta NO_DATA exitosa sustituye datos anteriores | Conforme |

CHK001–005, CHK007–008 y CHK010–012 continúan conformes. La caché por ID/temporada de TanStack Query y reintento controlado existentes permiten planificar esta política sin stores nuevos ni placeholder entre jugadores; no se afirma que el nuevo hook ya exista. Sin ampliación de perfiles, tarjetas, gráficos, tabs o cálculos.

Sin decisiones pendientes para estos cuatro hallazgos. La evidencia de evaluación previa se conserva íntegra; los hallazgos se consideran resueltos documentalmente, no técnicamente implementados ni aceptados por el revisor.

## Aceptación documental — 2026-10-25

El usuario solicitante autorizó explícitamente completar los 28 criterios si mantenían conformidad. Se contrastaron CHK001–012 con las cuatro tablas de perfiles, matriz/glosario de 60 claves, FR-025–048, SC-012–024 y contratos/modelo vigentes: 12/12 conformes, sin regresiones. Se aplican las 12 casillas bajo esa decisión humana; la aceptación es exclusivamente documental y permite TASKS, no implementación, pruebas o cierre. Las notas anteriores de marcadores vacíos son antecedentes conservados. Las tarjetas y gráficos permanecen sin cambios.

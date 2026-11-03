# Checklist de calidad de integración: SPEC-009

**Purpose**: Evaluar completitud, claridad y consistencia de requisitos de consulta y contratos del rediseño, no su implementación.
**Created**: 2026-10-25
**Feature**: [spec.md](../spec.md), [plan.md](../plan.md), [contrato](../contracts/player-statistics.md).
**Profundidad y destinatario**: estándar, con énfasis en riesgos de datos; revisor de requisitos antes de Tasks.

**Review Ownership**: Propiedad del revisor. `[x]` significa criterio de calidad aprobado por el revisor, no implementación completa. Los criterios generados permanecen sin marcar; la evaluación asistida solicitada se registra abajo sin sustituir aceptación humana.

## Completitud y claridad

- [x] CHK001 ¿Está delimitada la consulta individual respecto de identidad, sincronización y valoración poblacional? [Completeness, Spec §FR-025–028; Plan §Backend]
- [x] CHK002 ¿Está definida la temporada explícita y el criterio de temporada cuando no se aporta selección? [Clarity, Spec §FR-041 y Evidencia de CLARIFY; Contrato §Solicitud]
- [x] CHK003 ¿La definición de extracción confirmada distingue fila persistida, item confirmado y resultado poblacional? [Clarity, Spec §Evidencia de CLARIFY; Contrato §Selección, punto 2]
- [x] CHK004 ¿Están documentados todos los criterios de orden y desempate del snapshot? [Completeness, Spec §FR-041; Contrato §Selección, punto 3]
- [x] CHK005 ¿La prioridad de fuentes y el contexto requerido para respaldo son inequívocos? [Clarity, Spec §FR-041; Contrato §Selección, punto 4]
- [x] CHK006 ¿La conservación del cero y la diferencia entre nulo y error son consistentes entre spec y contrato? [Consistency, Spec §FR-029/041/043; Contrato §Respuesta y Errores]

## Cobertura de escenarios

- [x] CHK007 ¿Se especifica el caso de scraping como única fuente confirmada sin exigir JSON anterior? [Coverage, Spec §FR-041; Contrato §Selección, punto 4]
- [x] CHK008 ¿Se excluye explícitamente el respaldo de otra fecha, temporada o competición? [Coverage, Spec §FR-041; Contrato §Selección]
- [x] CHK009 ¿La ausencia de snapshot y la disponibilidad parcial tienen respuestas y contexto distinguibles? [Completeness, Contrato §Respuesta y Selección, punto 5; Modelo §Model de lectura]
- [x] CHK010 ¿La consulta concurrente con una extracción tiene una exigencia de coherencia documentada? [Coverage, Plan §Riesgos; Modelo §Model de lectura]

## Consistencia y dependencias

- [x] CHK011 ¿El contrato delimita 60 claves, unidades, nulabilidad y procedencia efectiva sin exponer operaciones financieras? [Consistency, Spec §FR-027/033/040; Contrato §Respuesta]
- [x] CHK012 ¿Están delimitadas las responsabilidades de lectura, composición y transformación sin contradicciones con las matrices arquitectónicas? [Consistency, Plan §Backend/Frontend; Research §Decisiones de backend]
- [x] CHK013 ¿Está delimitado el cambio contractual aditivo y la preservación de consumidores existentes e históricos? [Consistency, Plan §Archivos afectados; Contratos §encabezados de vigencia]
- [x] CHK014 ¿Está definida la identificación legible de liga/competición histórica, separada de la identidad actual, cuando esa información está disponible? [Completeness, Spec §FR-028; Contrato §Respuesta competitionKey; Plan §Riesgos; Gap]

## Seguridad y criterios de aceptación

- [x] CHK015 ¿Son explícitos autenticación, visibilidad, errores y ausencia de efectos de escritura de la consulta? [Completeness, Spec §FR-025/027; Contrato §Errores y seguridad]
- [x] CHK016 ¿La exigencia de identificador inválido está alineada sin decisiones abiertas entre las dos consultas del detalle? [Consistency, Spec §US1 y FR-025; Plan §Riesgos, IDs cero/negativos; Conflict]

## Notes — evaluación asistida solicitada

Evaluación documental del agente, 2026-10-25: **14 conformes, 2 parciales, 0 no aplicables**. Cero casillas marcadas por esta operación. No acredita aceptación humana, funcionamiento ni tests. Se revisaron únicamente secciones vigentes; `tasks.md` describe el diseño de demostración anterior y no acredita cobertura del nuevo alcance.

| Criterios | Resultado documental | Evidencia |
| --- | --- | --- |
| CHK001–003 | Conforme | Separación explícita; temporada opcional con default individual; item COMPLETED y fila RESOLVED, sin gate poblacional |
| CHK004–008 | Conforme | Fecha, prioridad JSON, observación/ID, cero conservado, respaldo coincidente y scraping único explícitos |
| CHK009–010 | Conforme | AVAILABLE/PARTIAL/NO_DATA y null; aislamiento coherente de lectura documentado |
| CHK011–013 | Conforme | 60 entradas/procedencia y capas definidas; identidad y muestras del catálogo preservadas; contratos históricos delimitados |
| CHK014 | Parcial — I-01 | FR-028 exige «liga cuando esté disponible» y el plan distingue contexto histórico; el DTO solo ofrece `competitionKey` opaca y no define cómo obtiene la UI una etiqueta histórica legible ni cuándo debe omitirla |
| CHK015 | Conforme | Bearer, elegibilidad, 400/401/404/500 y lectura sin efectos explícitos |
| CHK016 | Parcial — I-02 | US1 conserva «identificador inválido» y el nuevo contrato fija Long positivo/400, pero el plan registra una posible divergencia 400/404 en identidad y no fija su tratamiento dentro del alcance sin cambiar silenciosamente ese contrato |

**Correcciones recomendadas, no aplicadas:**

- **I-01 (media, integración):** precisar la etiqueta histórica disponible y su procedencia o la omisión explícita cuando no puede resolverse; nunca sustituirla por la liga actual. Alinear spec/contrato/modelo si se agrega un campo, sin crear llamadas externas.
- **I-02 (media, coherencia contractual):** determinar el alcance normativo de ID inválido para identidad y nueva lectura. Contrastar comportamiento existente antes de decidir si basta documentación o exige corrección futura autorizada; no dar por resuelta la advertencia del plan.

No se encontraron contradicciones en las reglas aprobadas de precedencia. Las decisiones técnicas ya descritas en PLAN no equivalen a aprobación humana del contrato por sí mismas.

`/speckit.implement` puede consultar el estado del checklist, pero no modifica sus marcadores. `requirements.md` tiene ciclo propio Specify/Clarify. Las revisiones históricas permanecen intactas. Sin hooks: `.specify/extensions.yml` ausente. No se avanza a Tasks ni se corrigen artefactos funcionales dentro de esta evaluación.

## Reevaluación tras decisiones del usuario — 2026-10-25

Operación documental separada y autorizada: **16/16 conformes documentalmente; 0 parciales**. Los 16 marcadores permanecen sin completar: asistencia de evaluación no sustituye aceptación del revisor.

| Criterio antes parcial | Resolución y evidencia | Resultado |
| --- | --- | --- |
| CHK014 / I-01 | Spec FR-045/SC-023, contrato campo competitionName y modelo: nombre opcional desde correspondencia fiable de la clave persistida; null/fallback explícito sin inventar ni usar liga actual | Conforme |
| CHK016 / I-02 | Spec FR-046/SC-023, contrato y Plan Backend: ambas lecturas requieren ID entero positivo dentro de Long, 400 inválido, 404 válido inexistente/no visible, 200 NO_DATA visible sin estadísticas; alineación futura de identidad documentada expresamente | Conforme |

CHK001–013 y CHK015 continúan conformes; la adición nullable no cambia selección, fuentes, visibilidad ni consumidor de identidad. Compatibilidad estática contrastada: `PlayerController` enlaza Long y `PlayerCatalogServiceImpl.getVisiblePlayerById` delega sin validación positiva; `PlayerStatistics` guarda clave de competición, no nombre. Se informó antes de editar que son límites de implementación, no carencias resueltas por existir documentos. El alcance futuro incluye 400 para cero/negativos y no se afirma ejecución o corrección de código.

Sin decisiones documentales pendientes de estos criterios. La aceptación humana de los checklists y los pasos SDD posteriores no se infieren de esta reevaluación.

## Aceptación documental — 2026-10-25

El usuario solicitante autorizó explícitamente marcar los 28 criterios si mantenían conformidad y continuar con TASKS. Relectura del alcance vigente de spec, plan, modelo y contratos: CHK001–016 mantienen conformidad, incluidas FR-045/046/048 y la política de recuperación FR-047. Se aplican sus 16 marcadores por esa autorización condicional satisfecha, sin atribuir al agente aprobación humana propia. La aceptación cubre calidad documental del rediseño y habilita generar tareas; no implementación, revisión de código, testing, ANALYZE ni cierre. Los conteos y marcadores sin completar descritos arriba son evidencia histórica anterior.

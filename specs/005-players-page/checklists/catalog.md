# Checklist de calidad del catálogo: SPEC-005

**Purpose**: Calidad general, UX y contenido del rediseño; no verifica implementación.
**Created**: 2026-10-25
**Feature**: [spec](../spec.md), [plan](../plan.md), referencia `06-catalogo-jugadores.png`.

**Review Ownership**: Del revisor. Generado por `/speckit.checklist` inicialmente sin marcar; las casillas se completan por la autorización humana condicional registrada abajo, tras reevaluación conforme. `[x]` significa aceptación documental de calidad de requisitos, no trabajo implementado ni aprobación autónoma del agente.

## Completitud y claridad

- [x] CHK001 ¿La sustitución de muestras/filtros visuales está delimitada sin alterar identidad, detalle o sincronización? [Completeness, Spec §Revisión y FR-023/025/032]
- [x] CHK002 ¿Las tres estadísticas y su ausencia/cero/contexto tienen significado definido sin valores de demostración? [Clarity, Spec §FR-025/033 y SC-012]
- [x] CHK003 ¿Los cinco criterios de búsqueda/filtro están definidos y combinados globalmente antes de paginar? [Completeness, Spec §FR-026/027/030 y SC-013]
- [x] CHK004 ¿El orden nombre ascendente de UI y orden ID legado de API están distinguidos, con desempate definido? [Consistency, Spec §FR-029/032 y SC-014]

## Escenarios y aceptación

- [x] CHK005 ¿El cambio de criterios, limpiar durante debounce, respuestas tardías y página fuera de rango están cubiertos? [Coverage, Spec §FR-030/031/034 y SC-015]
- [x] CHK006 ¿Las opciones globales y combinaciones incompatibles no dependen de la página ni de cascadas implícitas? [Clarity, Spec §FR-028 y Clarifications]
- [x] CHK007 ¿Los estados carga/error/vacío y recursos ausentes son distinguibles y conservan recuperación pertinente? [Coverage, Spec §FR-034/035/038 y SC-017]

## Visual, accesibilidad y límites

- [x] CHK008 ¿La correspondencia con mockup tiene viewport/distribución medibles, navbar preservado y adaptación sin datos inventados? [Measurability, Spec §FR-024/036 y SC-016]
- [x] CHK009 ¿El teclado, foco y alternativas textuales abarcan filtros, limpiar, tarjetas y paginación? [Coverage, Spec §FR-037 y SC-019]
- [x] CHK010 ¿Los límites excluyen favoritos, relevancia, métricas nuevas y finanzas sin trasladar decisiones al código? [Consistency, Spec §Entidades, dependencias y límites; Plan §Frontend]

## Notes

Selección: checklist general + UX porque el alcance reemplaza diseño e interacción; api.md cubre integración/datos/seguridad, evitando checklists adicionales redundantes. Los 10 criterios enlazan artefactos y evalúan redacción, no ejecución. Ninguna aprobación histórica de005 se aplica automáticamente a esta ampliación: la aceptación vigente se registra explícitamente abajo.

Atención explícita del revisor: búsqueda insensible a acentos y mayúsculas, con nombres originales preservados; opciones globales no cascadas; orden UI A–Z/Z–A e ID legado; enriquecimiento opcional y resúmenes por jugador de campañas potencialmente diferentes. Son reglas documentadas, no comportamientos ya implementados u observados.

## Reevaluación y aceptación documental — 2026-10-25

El usuario solicitante autorizó expresamente la corrección de acentos y pidió «Si todos resultan conformes, marcá las casillas y registrá la aceptación documental». Reevaluación: **10/10 conformes, 0 parciales, 0 pendientes**. Se aplican los marcadores al cumplirse esa condición, sin atribuir al agente una aprobación humana propia.

| Criterios | Evidencia de conformidad |
| --- | --- |
| CHK001–002 | FR-023/025/032/033 preservan identidad, datos reales y cero/ausencia; no cambios de sincronización o detalle |
| CHK003 | FR-027 y SC-013 ahora incluyen José/jose y Álvarez/alvarez, coherentes con contrato, función normalizadora e índice propuestos |
| CHK004 | FR-029 y contrato conservan default nombre ascendente de UI, ID legado de API y desempate, sin ampliar sort por esta corrección |
| CHK005–007 | Debounce/limpiar/página/respuestas tardías y estados en FR-028/030/031/034–038 siguen sin cambios |
| CHK008–010 | FR-024/036/037 y SC-016/019 delimitan mockup/viewport/accesibilidad; límites funcionales intactos |

La aceptación cubre la calidad documental de estos criterios y esta versión revisada; no acredita producto, tests o autorización de implementación. No se genera TASKS ni se ejecuta ANALYZE en este encargo. La evaluación anterior 9 conformes/1 parcial queda como antecedente de la discrepancia ya resuelta.

`/speckit.implement` no modifica marcadores. `requirements.md` tiene ciclo separado Specify/Clarify. Sin hooks configurados. No tests, builds, Tasks ni Analyze ejecutados por generar este documento.

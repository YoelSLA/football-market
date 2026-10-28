# Checklist de calidad del contrato del catálogo: SPEC-005 / SPEC-002

**Purpose**: Completitud, coherencia y verificabilidad de lectura global, compatibilidad, datos y seguridad; no tests de código.
**Created**: 2026-10-25
**Feature**: [Spec](../spec.md), [contrato canónico](../../002-player-catalog/contracts/catalog-query.md), [modelo](../data-model.md), [plan](../plan.md).

**Review Ownership**: Del revisor. Casillas inicialmente sin marcar por generación; se completan tras reevaluación conforme bajo la autorización humana condicional registrada abajo. `[x]` es aceptación de calidad documental, no aceptación funcional ni testing; el agente no se atribuye aprobación humana propia.

## Contrato y datos

- [x] CHK001 ¿Parámetros, tipos, valores vacíos, errores y orden admitido están especificados de forma inequívoca? [Completeness, Contrato §GET /api/players y Errores]
- [x] CHK002 ¿Count/página/lista comparten filtros y visibilidad, sin filtrar una sola página ni toda la población en cliente? [Consistency, Spec §FR-026/030; Contrato §Selección]
- [x] CHK003 ¿La respuesta sin flags mantiene identidad/estructura/orden legado y la enriquecida está completamente definida? [Completeness, Spec §FR-032; Contrato §Respuesta]
- [x] CHK004 ¿Las opciones de filtros son únicas, globales, reales y omiten opcionales desconocidos sin inventar países? [Clarity, Contrato §GET /filters; Spec §FR-028]
- [x] CHK005 ¿Confirmación, fecha, prioridad de fuentes, contextos y ceros del resumen están delimitados sin requisito poblacional? [Clarity, Spec §FR-033; Contrato §Resumen]
- [x] CHK006 ¿La ausencia deportiva mantiene jugadores y se distingue de fallo técnico de lectura? [Coverage, Spec §FR-025/034/038; Contrato §Errores]

## Arquitectura, seguridad y trazabilidad

- [x] CHK007 ¿El enriquecimiento y opciones evitan dependencias financieras, llamadas externas y consultas por jugador? [Consistency, Spec §FR-031–033; Plan §Backend]
- [x] CHK008 ¿Autenticación, visibilidad, búsqueda literal y sort controlado están definidos sin relajar seguridad? [Coverage, Spec §FR-023/027/038; Contrato §Solicitud]
- [x] CHK009 ¿Las responsabilidades DTO/Model/Mapper/Service/Repository y caché/estado UI respetan las matrices existentes? [Consistency, Plan §Backend/Frontend; Modelo §Estado]
- [x] CHK010 ¿Las verificaciones backend previstas respetan Repository real, Service público y ejecución humana, sin introducir tests frontend? [Coverage, Plan §Verificación; Quickstart §Pruebas]

## Notes

El contrato es diseño propuesto; no afirmar que GET /filters o includeStatistics existan en la implementación actual. El contrato api.md histórico de002 conserva su body como antecedente y dirige al contrato nuevo, sin borrar decisiones ajenas. No se edita SPEC-009 ni se considera aprobada su implementación por reutilizar persistencia.

## Reevaluación y aceptación documental — 2026-10-25

Autorización humana expresa del usuario solicitante: completar casillas si los 20 criterios mantienen conformidad tras la corrección de acentos. Resultado de este checklist: **10/10 conformes, 0 parciales, 0 pendientes**. Marcadores aplicados por esa decisión condicional satisfecha, no por aceptación autónoma del agente.

| Criterios | Evidencia de conformidad |
| --- | --- |
| CHK001 | Contrato name/normalización define equivalencia Unicode, literalidad, binding, límites y vacío; FR-027/SC-013 y plan/research/quickstart alineados |
| CHK002–004 | Predicado global/AND antes de paginar, count coherente, respuesta sin flags inalterada y opciones globales sin cascadas explícitos |
| CHK005–007 | Resumen por lote confirmado individual, respaldo coincidente, cero/ausencia/error diferenciados; sin proveedores o finanzas |
| CHK008 | Patrón ligado como parámetro, escape fijo !, sort whitelist, misma seguridad/visibilidad; pg_trgm no modifica permisos de acceso |
| CHK009 | Normalización única en Repository/PostgreSQL, no filtrado frontend; DTO/Model/Mapper separados y estado remoto en Query Cache |
| CHK010 | Verificación futura de migración/índice y Unicode/comodines en PostgreSQL/Testcontainers; Service público con Arrange acotado; tests solo humanos y sin tests frontend |

Sin decisiones documentales abiertas para estos criterios. Disponibilidad de pg_trgm, privilegios de instalación y comportamiento del plan de consulta son verificaciones futuras de entorno/rendimiento, no resultados observados ni motivo para inventar índices adicionales. No se ejecutaron SQL, migraciones, tests, builds, TASKS o ANALYZE.

Después de evaluación por revisor, TASKS debe cubrir cada FR/SC antes de ANALYZE read-only. No emitir aprobación humana, implementar o modificar el tablero desde esta fase. `/speckit.implement` no cambia marcadores; built-in requirements.md mantiene su ciclo propio.

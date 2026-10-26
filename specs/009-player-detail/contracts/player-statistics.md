# Contrato propuesto: consulta individual de estadísticas

Diseño de PLAN de SPEC-009, 2026-10-25. No acredita endpoint implementado. Complementa la identidad existente sin modificar su respuesta ni el catálogo. Fuente funcional: `spec.md` FR-025–048 y Clarify; FR-045 define competición legible/fallback, FR-046 distingue IDs/no encontrado/ausencia, FR-047 conserva respuesta compatible ante error y FR-048 referencia el glosario.

## Solicitud

`GET /api/players/{id}/statistics?seasonKey=<clave-canónica>` con Bearer y reglas de autenticación existentes. `id` Long positivo obligatorio; inválido, no numérico o fuera de rango → 400. `seasonKey` string opcional no vacío de máximo 255 caracteres, comparación exacta parametrizada con el valor persistido (no usar seasonLabel como identidad ni interpretar rangos a partir de la etiqueta). Temporada válida sin registros → ausencia exitosa, no jugador no encontrado. El detalle no agrega un selector visual.

Sin `seasonKey`, seleccionar la última extracción confirmada disponible del jugador y devolver su contexto real. Con ella, filtrar por esa temporada antes de seleccionar fecha. No hay parámetros de canal, fecha, recálculo, fuente ni población. La consulta no escribe ni llama a WhoScored.

## Respuesta 200

| Campo | Tipo JSON | Semántica |
| --- | --- | --- |
| `playerId` | integer | Identidad interna solicitada y visible |
| `requestedSeasonKey` | string o null | Parámetro explícito o null |
| `status` | string | AVAILABLE: 60 valores; PARTIAL: 1–59; NO_DATA: ninguno |
| `seasonKey` | string o null | Temporada real del snapshot; null sin snapshot |
| `seasonLabel` | string o null | Etiqueta persistida, nunca temporada simulada |
| `extractionDate` | string ISO date o null | Fecha real seleccionada, no reloj del navegador |
| `competitionKey` | string o null | Identidad opaca de competición del snapshot persistido; no liga actual ni instrucción de consulta externa |
| `competitionName` | string o null | Nombre legible solo con correspondencia fiable disponible para la clave del snapshot; null si no puede determinarse sin inventar información |
| `metrics` | array | Exactamente 60 elementos únicos de MetricCatalog, en su orden canónico |

Cada elemento contiene `key` (string canónica), `unit` (SEASON_TOTAL, PER_MATCH, PERCENTAGE o AVERAGE_RATING), `value` (number o null) y `source` (JSON, SCRAPING o null). Son números JSON desde BigDecimal, no strings localizados. La unidad SEASON_TOTAL se presenta en minutos/partidos/goles/etc. según metadatos de la clave; no implica que todos los acumulados sean goles.

Etiquetas y unidades visibles: [metric-glossary.md](metric-glossary.md), 60 claves. `competitionName` es enriquecimiento opcional de lectura, no dato histórico actualmente persistido ni razón para agregar tabla o llamar a proveedores. Solo resolverlo con una correspondencia local explícita e inequívoca de la clave del snapshot con información disponible; la asociación actual jugador→equipo→liga no acredita esa correspondencia. Sin ella, null y UI «Competición no disponible». Sin snapshot, competitionKey/competitionName null.

No se publican números de ejemplo para evitar convertir cifras del mockup en estadísticas reales. La forma se documenta mediante tabla; ejemplos futuros de OpenAPI/tests deben indicar su naturaleza de fixtures, nunca cargarse como fallback del producto.

## Selección y composición normativa del contrato

1. Comprobar elegibilidad igual que identidad: jugador activo, equipo y liga visibles; inexistente o no visible → mismo 404 PLAYER_NOT_FOUND, sin exponer estadísticas.
2. Solo filas con contexto RESOLVED e item referenciado COMPLETED, coherentes en jugador/fecha/canal. No exigir confirmación poblacional ni run global COMPLETED. Ignorar filas huérfanas o de contexto sin resolver.
3. Elegir fecha mayor del jugador y temporada pedida; si no se pide, última fecha del jugador. Dentro de fecha, JSON ancla antes de scraping; luego observedAt e ID mayores como desempate determinista. El ancla determina temporada y competición; no es una elección de la temporada más numerosa ni del equipo actual.
4. Completar desde scraping solo con jugador, temporada, fecha y competición exactamente iguales al ancla JSON. Cada valor JSON no nulo se conserva, incluido cero. Clave ausente en ambos → value/source null. Las tres claves exclusivas scraping también requieren coincidencia de contexto. Sin JSON en esa fecha usar scraping confirmado, nunca JSON de una fecha anterior.
5. Emitir 60 entradas incluso sin snapshot. Sin candidato: contexto null, valores/source null, status NO_DATA. Fallos de consulta no se convierten en esta respuesta.

## Errores y seguridad

- 400: parámetros inválidos, contrato global existente.
- 401: credencial ausente/expirada; conservar flujo global de sesión.
- 404: PLAYER_NOT_FOUND para inexistente/no visible, idéntica política que identidad.
- 500: fallo interno de lectura, contrato global; frontend ofrece reintento y mantiene identidad ya obtenida. No introducir 502 de proveedor: no hay consulta externa.

La política de FR-046 se aplica a ambas lecturas: ID decimal positivo dentro de Long; formato inválido/cero/negativo/fuera de rango → 400, válido inexistente/no visible → 404; jugador visible sin estadísticas → 200 NO_DATA. El endpoint de identidad existente no tiene validación positiva explícita: requiere alineación futura acotada para cero/negativos, conservando DTO, rutas y visibilidad. Este documento no afirma que ese cambio esté implementado.

No requiere roles operativos de sincronización ni amplía permisos existentes. No exponer entidades JPA, IDs de runs/items o información financiera. OpenAPI de Controller/DTO y tests funcionales de Controller deben sostener este contrato en implementación, sin depender de REST Docs retirado.

## Contrato de presentación

Usar los perfiles, matriz y gráficos exactos de `../spec.md`. Etiquetas/categorías/prioridades son metadatos de presentación de la feature, no valores del proveedor. Fuente nullable por métrica mantiene trazabilidad; número cero siempre disponible. La aparición futura de las tres claves no cambia Resumen. Dos tabs accesibles, buscador/categorías locales; sin radar, xG/xA, percentiles, tasas nuevas o finanzas.

Error de actualización: conservar solo la última respuesta exitosa del mismo playerId/seasonKey resuelto, mostrando su fecha original, el error, reintento y «Mostrando la última consulta disponible; no se pudo actualizar». No hacer pasar esa respuesta por una actualización exitosa ni convertirla a NO_DATA. No mezclarla con una selección explícita distinta; sin respuesta compatible presentar error sin valores. Para consulta sin seasonKey, una actualización fallida puede conservar el último contexto resuelto del mismo jugador identificado como anterior; no afirmar conocer la temporada de una respuesta que falló. Una respuesta nueva exitosa sustituye la anterior incluso si es NO_DATA.

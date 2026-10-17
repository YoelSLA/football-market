# Modelo de datos propuesto: imágenes de jugadores

Derivado de [spec.md](spec.md); estructura física propuesta para PostgreSQL/JPA, sujeta a migración durante implementación. No modificar la identidad interna ni el modelo de referencias de `002-player-catalog`.

## Entidades y relaciones

| Entidad | Campos relevantes | Relaciones/restricciones |
| --- | --- | --- |
| `Player` (existente) | `id` interno, `active`, datos de identidad, `imageUrl` nullable (existente), **`fallbackImageUrl` nullable (nuevo)** | Las URLs no contienen estado operativo; la sincronización del catálogo no las sobrescribe. Ampliar `players` con `fallback_image_url`, misma capacidad prevista para `image_url`. |
| `PlayerExternalReference` (existente) | `playerId`, `provider`, `externalId` | Reutilizar el valor `THE_SPORTS_DB` ya existente en el modelo de `002-player-catalog`; referencia persistida aun sin imagen. Conservar unicidad existente `(provider, externalId)` y reglas de unicidad por jugador/proveedor del catálogo; no reasignar en conflictos. Coexiste con `FOOTBALL_DATA`. |
| `PlayerImageResolution` (nueva) | `playerId`, `status`, `lastAttemptAt` nullable | Una por jugador, FK a `Player`; `PENDING` inicial. Estados operativos separados de imágenes y referencias. Indexar estado/fecha si se requieren filtros de elegibilidad. |
| `PlayerImageSyncRun` (nueva) | `id`, `startedAt`, `finishedAt` nullable, `force`, `status`, `failureReason` nullable, `evaluated`, `processed`, `found`, `notFound`, `retryableErrors`, `failed`, `conflicts`, `skippedFound`, `skippedRetryWindow`, `skippedFailed`, `interrupted` | Una por invocación admitida; `durationMillis` derivada, nunca confundida con tiempo del último item. Los contadores se mantienen consistentes con items. Indexar `startedAt DESC, id DESC` para historial. |
| `PlayerImageSyncRunItem` (nueva) | `id`, `runId`, `playerId`, `previousState`, `finalState`, `result` (`FOUND`, `NOT_FOUND`, `RETRYABLE_ERROR`, `FAILED`, `SKIPPED`, `INTERRUPTED` o pendiente interno), `identityResolved`, `imageFoundOrUpdated`, `skipReason` nullable (`FOUND`, `RETRY_WINDOW`, `FAILED`), `conflict`, `errorOccurred`, `outcomeDetail` seguro para exposición, `finishedAt` nullable | FK a run y jugador; único `(runId, playerId)`; sin items de jugadores inactivos al comenzar su evaluación individual. Los pendientes creados pero no cerrados son recuperables como `INTERRUPTED`; los completados nunca se reescriben por recuperación. Eliminación conjunta con run al vencer retención. |

La retención se configura sin fijar un default funcional. No borrar resoluciones/referencias al purgar auditoría. Evitar almacenar respuestas crudas, secretos o URLs de proveedores en el detalle público. Timestamps en UTC; `lastAttemptAt` registra intento real y no omisiones; duration derivada de fechas. La pertenencia al run se determina según `active` al inicio de cada evaluación individual, no al comienzo del run. Si entonces es inactivo, no hay item ni incremento de `evaluated`/`processed`; si es activo, se crea exactamente un item y suma a `evaluated`, aunque luego se omita o cambie `active`. Recorrer con orden estable por id y sin offset sobre estados mutables.

## Invariantes de estados de resolución

| Actual | Evento | Final / próxima elegibilidad |
| --- | --- | --- |
| ausencia de resolución | alta de jugador o reparación durante sync | `PENDING`, intento inmediato si activo |
| `PENDING`, `RETRYABLE_ERROR` | run normal o forzado | procesar de inmediato si activo |
| `NOT_FOUND` | run normal | omitir `RETRY_WINDOW` hasta que transcurran los días configurados desde `lastAttemptAt` (30 por defecto); entonces procesar |
| `FOUND` | run normal | omitir `FOUND` sin solicitud |
| `FAILED` | run normal | omitir `FAILED` sin solicitud |
| cualquiera | run con `force=true` y activo | procesar, pero nunca buscar por nombre si ya existe referencia externa |
| cualquiera | respuesta válida sin resultados (también lookup vacío con referencia e imágenes previas), sin identidad confiable, o identidad esperada devuelta sin imágenes nuevas ni imágenes anteriores | `NOT_FOUND`, sin retry técnico; referencia e imágenes válidas preexistentes conservadas, sin rematching |
| cualquiera | identidad esperada devuelta con imagen válida, o sin imágenes nuevas pero con imagen previa | `FOUND`; conservar URL previa si falta sustitución válida |
| cualquiera | error transitorio agotado | `RETRYABLE_ERROR`, próximo run normal inmediato; preservar datos válidos |
| cualquiera | error permanente, datos presentes de la identidad consultada por ID inequívocamente incompatibles según FR-015, o colisión de referencia | `FAILED`, elegible solo con force; preservar identidad/URLs anteriores; datos opcionales ausentes no demuestran contradicción |

Jugador inactivo al comenzar su evaluación individual: ninguna transición por sync de imágenes, ningún item ni solicitud externa; tras reactivarse se usa el mismo estado y fecha. Si se inactiva después de comenzar una evaluación como activo, el item se termina normalmente, sin cancelar el intento ni revertir resultados previos; en runs posteriores queda excluido mientras permanezca inactivo. Una búsqueda aceptada crea referencia externa incluso cuando la resolución final es `NOT_FOUND`.

## Estados y contabilidad de run

- `RUNNING` al admitir POST, `COMPLETED` al acabar sin `RETRYABLE_ERROR`/`FAILED` individual ni conflictos (puede haber `NOT_FOUND`), `PARTIAL` al acabar con al menos uno de esos problemas, `FAILED` si se interrumpe por fallo global o reinicio. `finishedAt` solo al cerrar; `failureReason` identifica fallo global, especialmente `INTERRUPTED_BY_RESTART`.
- `evaluated` = cantidad de items creados para jugadores activos al inicio de su evaluación individual (incluye skips aunque luego cambie `active`); `processed` = items con intento real de enriquecimiento (incluye resultados fallidos, excluye skips e items interrumpidos antes del intento). `found`, `notFound`, `retryableErrors`, `failed` contabilizan resultados finales individuales correspondientes; `conflicts` es subconjunto causal de `failed`, no una categoría sumable de nuevo a `processed`.
- `skippedFound`, `skippedRetryWindow`, `skippedFailed` son disjuntos, con motivo explícito en item. `interrupted` cuenta exclusivamente items pendientes cerrados por recuperación; no se agrega a `failed`. Un item previamente terminado conserva su resultado tras el reinicio.
- Para garantizar persistencia parcial, confirmar cada resultado de jugador y su item conjuntamente cuando sea posible; iniciar/finalizar run en unidades independientes. En una colisión de unicidad revertir solo el intento de ese jugador, registrar el conflicto en una unidad separada y seguir. No retener transacciones durante red ni espera de cuota. Si falla la propia escritura de auditoría, cerrar el run como `FAILED` cuando la base esté disponible, sin prometer persistencia imposible durante indisponibilidad completa.

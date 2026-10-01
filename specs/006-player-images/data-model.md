# Modelo de datos: imágenes de jugadores

## Jugador existente (`players`)

| Campo | Tipo lógico | Regla |
| --- | --- | --- |
| `id` | ID externo de Football-Data.org | Identidad estable; sin cambios. |
| `name`, `team`, `league`, `position`, `active` | Datos actuales | Sin reemplazo por datos de TheSportsDB. |
| `image_url` | Texto nullable | URL HTTPS validada del retrato (`strThumb`); `null` si no hay imagen asociada. |
| `image_resolution` | Estado no nulo | `PENDING`, `FOUND` o `NO_MATCH`; migración inicial con `PENDING` para jugadores existentes. |

La imagen pertenece al mismo jugador que `id`; no se cambia su clave primaria ni se exige tabla adicional. `image_url` es nullable aun cuando el proveedor haya respondido sin coincidencia. El estado `FOUND` requiere URL; `PENDING` y `NO_MATCH` requieren URL nula. La migración debe preservar los jugadores existentes y ser compatible con la validación de esquema vigente.

## Estados y transiciones

| Estado inicial | Suceso | Estado final | Efecto |
| --- | --- | --- | --- |
| Nuevo | Jugador creado desde Football-Data.org | `PENDING` | Sin URL. |
| `PENDING` | Coincidencia inequívoca e imagen válida | `FOUND` | Guardar URL validada. |
| `PENDING` | Sin coincidencia, ambigua o sin imagen | `NO_MATCH` | Conservar URL nula; no consultar de nuevo en el mismo estado. |
| `PENDING` | 429 agotado, fallo HTTP/técnico o interrupción | `PENDING` | No falsear ausencia; permitir próxima invocación manual. |
| `FOUND` o `NO_MATCH` | Nombre o equipo cambia en una sincronización exitosa | `PENDING` | Quitar URL previa si existe; revalidar identidad en próxima búsqueda manual. |
| Cualquiera | Sincronización exitosa sin cambio de nombre/equipo | Igual | Conservar el estado y la URL. |
| Cualquiera | Inactivación/reactivación del mismo jugador | Igual | Se conserva la imagen; solo jugadores activos son candidatos para nueva búsqueda. |

`NO_MATCH` significa que se completó una búsqueda sin foto asignable, no que el proveedor falló. Revisar periódicamente resultados antiguos o forzar una nueva búsqueda masiva queda fuera de alcance; un cambio de identidad reabre el estado.

## Ejecución de sincronización de imágenes

La exclusión de ejecuciones simultáneas es de la instancia de aplicación, no una entidad de base de datos. Una ejecución enumera jugadores activos con `PENDING` en orden estable de `id`, consulta a TheSportsDB y persiste cada transición individualmente. Tras una interrupción, los que sigan `PENDING` podrán procesarse mediante un nuevo POST autenticado. No se mantienen locks de base de datos durante HTTP ni durante las esperas por cuota.

Antes de guardar el resultado externo se comprueba de nuevo que el registro sigue activo y pendiente y que `name` y `team` siguen siendo los consultados. Si cambió por una sincronización simultánea de jugadores, el resultado obsoleto se descarta y la próxima invocación manual puede buscar la nueva identidad.

El resumen de una ejecución completada incluye `processed` (búsquedas concluidas para jugadores), `found` (con URL asignada) y `withoutImage` (sin coincidencia válida); `processed = found + withoutImage`. Los reintentos no cuentan como jugadores adicionales. Ante ejecución incompleta se utiliza el error estándar y se conservan las transiciones ya persistidas.

## Proyección HTTP y presentación

`GET /api/players` sigue devolviendo exclusivamente activos con sus cinco campos vigentes más `imageUrl`, `null` en `PENDING` y `NO_MATCH`. La tarjeta muestra `imageUrl` si se carga correctamente; de lo contrario usa `Player Generic-Icon.png`. `image_resolution` es interno y nunca se expone como campo del jugador.

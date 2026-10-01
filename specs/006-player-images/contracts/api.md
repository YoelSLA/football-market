# Contratos de API: Imágenes de jugadores

Esta feature amplía el [contrato del catálogo](../../002-player-catalog/contracts/api.md). La documentación OpenAPI del backend se actualizará durante la implementación.

## `GET /api/players`

Conserva autenticación, paginación, errores y los cinco campos de cada jugador activo. Añade `imageUrl`, presente y nullable; el campo indica el retrato de TheSportsDB asociado al jugador. No se consulta a TheSportsDB durante este GET.

### Respuesta `200 OK`

```json
{
  "content": [
    {
      "id": 7821,
      "name": "Joel Robles",
      "team": "Real Betis Balompié",
      "league": "Primera Division",
      "position": "Goalkeeper",
      "imageUrl": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`imageUrl` es `null` si la búsqueda está pendiente o no existe un retrato asignable; nunca expone el estado interno de resolución. Su valor no nulo es una URL HTTPS de retrato validada.

## `POST /api/players/images/sync`

Inicia **manualmente** la búsqueda de imágenes de jugadores activos pendientes. No acepta body. Requiere JWT válido y no exige rol adicional. La respuesta permanece abierta hasta que termina la ejecución; no hay endpoint de progreso. La duración depende de la cantidad de jugadores pendientes y de las esperas por cuota.

### Respuesta `200 OK`

```json
{
  "processed": 12,
  "found": 8,
  "withoutImage": 4
}
```

`processed` es la cantidad de jugadores cuya búsqueda se completó en esta invocación; `found` contabiliza los que recibieron retrato y `withoutImage` los que no tuvieron una coincidencia válida con imagen. `processed = found + withoutImage`. Una ejecución sin pendientes devuelve los tres contadores en cero. Los reintentos HTTP no aumentan `processed`.

### Respuesta `401 Unauthorized`

JWT ausente o inválido. No comienza ninguna búsqueda.

### Respuesta `409 Conflict`

Ya hay una ejecución activa en esta instancia. No se inicia una segunda. Se usa el error estándar `ErrorResponseDTO` con `code: PLAYER_IMAGE_SYNC_IN_PROGRESS`.

### Respuesta `502 Bad Gateway`

Una falla de TheSportsDB impidió completar el trabajo, incluso después de los reintentos aplicables. `ErrorResponseDTO` utiliza `code: PLAYER_IMAGE_SYNC_INCOMPLETE`. Las fotos guardadas anteriormente se conservan; una nueva invocación manual intenta los jugadores pendientes. El error no presenta un resumen de éxito ni datos sensibles del proveedor.

## Formato de errores

Los errores gestionados utilizan `timestamp`, `status`, `error`, `code`, `message` y `path` según el contrato HTTP vigente. `status` coincide con el código HTTP, `code` es estable y `message` es legible en español. Las respuestas `401` siguen el mecanismo de seguridad existente.

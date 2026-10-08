# Contratos HTTP: Dominio de equipos y ligas

## GET /api/players

B-V5-05: dateOfBirth y nationality se exponen exactamente como quedaron persistidos tras la consolidación por atributo, incluido null cuando las referencias válidas entran en conflicto y no había valor previo. Sin campos nuevos, sin normalización y sin efecto sobre teamId/teamName/leagueId/leagueName.

B-V5-04 resuelto: name/position exponen exactamente representación persistida; no normalización ni ranking en Mapper/GET. Actualizaciones equivalentes conservan texto anterior; alta o cambio semántico usan representación original canónica (diacríticos, casing natural, desempate lexicográfico), sin depender del orden. No campos nuevos. Esta decisión sustituye las menciones pendientes anteriores.

Cambio deliberado RF-019/RF-028. JWT vigente; page=0 por defecto, page>=0; size=20 por defecto, 1..100. Orden existente por Player.id creciente.

Solo Player activo con Team asociado current=true y League válida. Team/League se cargan dentro de consulta transaccional de lectura (entity graph/proyección), sin acceso remoto ni lazy loading fuera de contexto. Un caso registrado no genera error ni excluye por sí mismo un jugador válido.

```json
{
  "content": [{
    "id": 7821, "name": "Nombre del jugador",
    "teamId": 41, "teamName": "Arsenal",
    "leagueId": 12, "leagueName": "Premier League",
    "position": "Midfielder", "dateOfBirth": null,
    "nationality": null, "imageUrl": null, "fallbackImageUrl": null
  }],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

id/teamId/leagueId son números internos de FootballMarket, nunca IDs externos. name/teamName/leagueName/position son strings obligatorios. leagueId/leagueName derivan exclusivamente del Team. Los cuatro campos opcionales existentes se conservan presentes y nullable. Se retiran team/league, sin aliases deprecados. No se exponen active, current, referencias ni proveedores.

leagueId permanece estable ante rename y leagueName refleja el nombre actual. El consumidor puede clasificar por leagueName con fallback neutral existente para nombres desconocidos; no se promete continuidad visual histórica. No se agrega leagueCode, leagueSlug, visualKey ni otro campo para clasificación, ni se requiere mapa por leagueId.

Catálogo vacío: content=[], totalElements=0, totalPages=0 y page/size solicitados. Página fuera de rango conserva semántica existente (contenido vacío y totales reales). 400 por paginación inválida, 401 sin JWT válido; ErrorResponseDTO existente (timestamp,status,error,code,message,path). No se agregan errores por pendientes.

## POST /api/players/sync

Contrato de invocación sin cambios: JWT, sin body ni permisos nuevos; 200 tras confirmar aplicación principal, 401 sin autenticación, 502 si Football-Data no permite obtener foto completa. Fallo técnico de persistencia se trata por mecanismo existente, no como descarte funcional ni éxito.

```json
{"obtained":0,"created":0,"updated":0,"markedInactive":0,"discardedInvalid":0}
```

Contadores siguen siendo de jugadores, no de Team/League: obtained integrantes recibidos, created altas de Player, updated Players existentes procesados válidamente, markedInactive cambios efectivos true→false, discardedInvalid descartes individuales de Player. Duplicados válidos se consolidan por referencia con orden configurado existente; solo contadores del commit final. Backfill aislado no cuenta como actualización normal de datos; sus casos se consultan por SQL.

No se agrega contador de revisión ni se modifica DTO por RF-031. Enriquecimiento posterior TheSportsDB no cambia éxito confirmado, contadores ni errores del catálogo principal.

## Otras interfaces

B-V5-03 mantiene HTTP y generaliza la protección de B-V5-02 a name/position incompatibles tras normalización estricta existente: el Player conserva integralmente datos persistidos y referencias, sin error global ni nuevos campos. Múltiples observaciones coherentes no son inválidas; casos históricos no bloquean futuras fotos coherentes. B-V5-04 resuelto determina la representación persistida descrita en GET; no seleccionar mediante orden de entrada ni convertir claves de comparación en presentación.

B-V5-02 no agrega campos ni error global: observaciones de referencias distintas del mismo Player con Teams válidos contradictorios se descartan individualmente conforme discardedInvalid, sin created/updated ni markedInactive para ese Player. Se registra INVALID_SUBJECT_DATA y se conserva su estado/referencias; los demás Players continúan. La comparación se hace por propietario y Team, no por orden de referencias. La consolidación por una misma referencia del párrafo anterior no es una prioridad entre referencias distintas del mismo propietario. Un caso histórico no bloquea una foto coherente posterior.

La resolución de B-V5-01 no modifica HTTP: Player puede conservar varias referencias internas del mismo proveedor con externalId distintos, pero ninguna se expone. Sigue vigente la unicidad de cada `(provider,externalId)` por tipo de entidad; la cardinalidad por propietario no es un campo ni una validación del contrato HTTP.

No endpoint administrativo, frontend de revisión ni comando nuevo. SQL de revisión en [quickstart.md](../quickstart.md). Endpoints de imágenes conservan contrato/comportamiento; adaptaciones internas usan nombre de Team sin resolver/modificar imágenes en 007. OpenAPI y REST Docs deben reflejar únicamente los cambios de GET y aclaración de significado, no un campo adicional del resumen.

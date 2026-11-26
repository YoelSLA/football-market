# Contrato HTTP: consultas cacheadas

La caché es interna y no cambia el contrato de los endpoints existentes. Todas las rutas requieren
la misma autenticación Bearer JWT que tienen actualmente. Las consultas válidas conservan
parámetros, DTO de respuesta, códigos HTTP y errores documentados por sus Controllers/OpenAPI.
Los errores no se almacenan en caché.

| Método y ruta | Entradas que distinguen la consulta | Respuesta existente |
| --- | --- | --- |
| `GET /api/rankings/score` | Filtros, orden, página y tamaño efectivos según `ScoreRankingQueryRequestDTO`; los valores predeterminados explícitos e implícitos equivalentes comparten clave. | `ScoreRankingPageResponseDTO`; errores de parámetros `400`, autenticación `401`, fallo de lectura `500`. |
| `GET /api/players/filters` | Sin parámetros de consulta; clave única. | `PlayerCatalogFiltersResponseDTO`; autenticación `401`, fallo de lectura `500`. |
| `GET /api/players` | `page`, `size`, `name`, `leagueId`, `teamId`, `position`, `nationality`, `sort`, `includeStatistics`, tras aplicar defaults y normalización existentes. | DTO de página actual (`PlayersPageResponseDTO` o `PlayerCatalogPageResponseDTO`); parámetros inválidos `400`, autenticación `401`. |
| `GET /api/players/{id}` | ID interno positivo del jugador. | `PlayerResponseDTO`; ID inválido `400`, autenticación `401`, jugador inexistente/no visible `404`. |
| `GET /api/market/official` | `page`, `size`, `sort` y todos los filtros efectivos admitidos por `OfficialMarketQueryRequestDTO`, incluidas cotización y bandas de precio. | `OfficialMarketPageResponseDTO`; parámetros inválidos `400`, autenticación `401`, fallo de lectura `500`. |
| `GET /api/market/p2p` | `viewerId` resuelto de la sesión autenticada, `page`, `size`, `sort` y todos los filtros efectivos de `P2PMarketQueryRequestDTO`. | `P2PMarketPageResponseDTO`; parámetros inválidos `400`, autenticación `401`. |

Los nombres, rangos y defaults vigentes de parámetros, así como el esquema completo de cada
respuesta, son los declarados por los DTO request/response y las anotaciones OpenAPI de los
Controllers citados. Esta feature no los modifica. En P2P, el `viewerId` es interno y no es un
parámetro HTTP.

La ausencia, expiración o reinicio de una entrada repite la lectura existente desde PostgreSQL y
devuelve el mismo contrato. La caché no agrega headers, endpoints ni códigos de respuesta.

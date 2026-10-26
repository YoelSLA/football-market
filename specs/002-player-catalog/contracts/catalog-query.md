# Ampliación propuesta de lectura del catálogo — SPEC-005

Diseño documental 2026-10-25, no endpoint ya implementado ni autorización de código. Responsable funcional del rediseño: SPEC-005 FR-023–038. Conserva sincronización, visibilidad, identidad y operaciones de SPEC-002. Las referencias a `team`/`league` texto del contrato histórico no sustituyen los campos internos actuales `teamId/teamName`, `leagueId/leagueName`.

## GET /api/players

Bearer obligatorio. Mantener página desde cero, size predeterminado 20 y rango 1–100. El cliente de catálogo solicita size=12. Sin parámetros nuevos: mismos campos actuales de identidad, metadatos y orden ID ascendente. Las consultas son exclusivamente locales y de lectura.

| Parámetro nuevo opcional | Contrato |
| --- | --- |
| `name` | Subcadena literal insensible a mayúsculas y acentos; trim; blanco equivale a ausencia; máximo 255 caracteres sobre entrada original. «José»↔«jose», «Álvarez»↔«alvarez», con equivalencia Unicode compuesta/descompuesta. Escapar `%`, `_` y el carácter de escape después de normalizar: no son comodines de entrada. Preservar nombres originales |
| `leagueId` | Long positivo, igualdad de liga interna del equipo vigente; inexistente devuelve conjunto vacío |
| `teamId` | Long positivo, igualdad de equipo interno vigente; inexistente o incompatible con liga devuelve vacío |
| `position` | Texto exacto persistido, trim, máximo 255; blanco sin criterio; desconocido bien formado devuelve vacío |
| `nationality` | Texto exacto persistido, trim, máximo 255; blanco sin criterio; no convertir null en un país |
| `sort` | Conjunto cerrado `id,asc` (predeterminado legado), `name,asc`, `name,desc`; otros valores → 400 INVALID_REQUEST |
| `includeStatistics` | boolean, predeterminado false; solo true incluye resumen por jugador |

Selección: visibilidad (`active=true`, equipo vigente y liga asociada) → filtros AND → orden → página. `name` ordena por lowercase del nombre persistido con colación PostgreSQL de la base, ID ascendente como desempate en ambos sentidos. No agregar un orden de relevancia o de valores deportivos. La UI envía siempre `sort=name,asc` inicialmente y tras limpiar.

### Normalización de nombre y rendimiento (diseño, no implementación)

Normalizar ambos operandos con la misma función PostgreSQL de búsqueda: trim → Unicode NFD → eliminación de marcas combinantes U+0300–U+036F → lowercase con la misma colación de base. Base UTF-8. Los nombres originales no se actualizan ni se sustituyen por la forma de búsqueda. No se agregan transliteraciones de otros alfabetos, corrección ortográfica o equivalencias fonéticas. El frontend conserva y envía el texto original; no filtra ni intenta reproducir la normalización SQL. Así los clientes HTTP y la UI comparten la misma semántica backend.

Usar una función SQL propia, pura, STRICT e IMMUTABLE con esas operaciones inmutables, sin diccionarios editables o configuración variable. No declarar inmutable un wrapper de `unaccent` dependiente de reglas mutables: no se adopta esa extensión. La implementación debe fijar expresión/colación de normalización de forma única para consulta e índice. Cualquier modificación futura de esa función/colación exige reconstruir el índice correspondiente.

Tras normalizar la entrada, escapar `!` como `!!`, `%` como `!%` y `_` como `!_`, y delimitar el patrón con `%` para la coincidencia parcial solicitada. Binding de patrón como parámetro y `ESCAPE '!'` fijo; nunca interpolar texto de usuario en SQL. Si trim/normalización deja cadena vacía, omitir el predicado de nombre, sin cambiar los otros criterios. Count y selección usan exactamente el mismo predicado normalizado.

Planificar índice GIN de expresión sobre la función normalizada de `players.name`, con `gin_trgm_ops` de `pg_trgm`, instalado por migración controlada futura. Es índice de búsqueda, no nuevo campo de identidad. Su expresión debe coincidir con la usada por el predicado. No sustituir por un B-tree sobre `lower(name)`: no garantiza aceleración de `%subcadena%`. Términos muy cortos o poco selectivos pueden requerir escaneo; no imponer longitud mínima nueva ni prometer uso de índice en cada consulta. Validar plan de consulta y costo con datos representativos en la implementación bajo controles permitidos. No añadir otros índices por anticipación. El ordenamiento nombre/ID ya aprobado no se cambia a nombre sin acentos por esta decisión.

Respuesta conserva `content`, `page`, `size`, `totalElements`, `totalPages`. Conteos del conjunto filtrado, no de la página. Página fuera de rango válida devuelve content vacío y total real; frontend corrige una vez si totalPages>0. Sin resultados, totalElements/totalPages cero.

Identidad de cada elemento: `id`, `name`, `teamId`, `teamName`, `leagueId`, `leagueName`, `position`, `dateOfBirth`, `nationality`, `imageUrl`, `fallbackImageUrl`, con nulabilidad vigente. Sin includeStatistics, no emitir campos nuevos: preservar consumidores con estructura estricta y el endpoint individual de identidad.

Con includeStatistics=true se agrega exclusivamente `statistics`:

| Campo del resumen | Tipo y regla |
| --- | --- |
| `seasonKey`, `seasonLabel` | string o null, contexto real seleccionado |
| `extractionDate` | fecha ISO o null, no fecha de consulta |
| `competitionKey` | string o null, contexto persistido, no liga actual inferida |
| `appearancesTotal`, `goalsTotal`, `assistsTotal` | number o null; mismas claves canónicas appearances_total/goals_total/assists_total, sin derivaciones nuevas |
| `sources` | objeto con appearancesTotal/goalsTotal/assistsTotal, cada valor JSON/SCRAPING/null; procedencia de cada valor, no mostrada en UI |

No exige estadísticas para figurar en catálogo. Sin snapshot el objeto contiene contexto/valores/fuentes null. Snapshot confirmado individual más reciente por jugador: contexto RESOLVED, item referenciado COMPLETED y coherente en jugador/fecha/canal; fecha DESC, JSON prioritario en igual fecha, observedAt DESC e ID DESC. Ancla fija temporada/competición. Respaldo scraping solo ante valor nulo y mismo jugador/temporada/fecha/competición; cero nunca se reemplaza; si solo existe scraping confirmado, usarlo. Sin retroceder fechas para completar valores. No depender de campaña poblacional ni de valuaciones. Selección de página y enriquecimiento comparten snapshot de lectura coherente.

La selección estadística usa el lote de IDs de la página, no HTTP ni una consulta por tarjeta/jugador. El backend no expone todas las campañas al frontend ni carga toda la población para paginar. Filtros/orden no incluyen datos deportivos.

Errores: 400 INVALID_PLAYER_PAGE/INVALID_PARAMETER_TYPE heredados y INVALID_REQUEST para criterios/sort inválidos; ErrorResponseDTO global seguro. 401 sesión inválida. Fallo técnico de base → 500, nunca estadística ausente fabricada ni respuesta parcial silenciosa. No hay 404 por combinación de filtros sin resultados. Sin cambios a permisos de sincronización.

## GET /api/players/filters (propuesto)

Bearer obligatorio, sin argumentos de filtros/página. Lee el mismo conjunto completo visible. Retorna `leagues: [{id,name}]`, `teams: [{id,name,leagueId}]`, `positions: string[]`, `nationalities: string[]`. Valores únicos, IDs reales, orden textual lowercase ascendente e ID como desempate cuando existe; omitir null/vacío en opcionales. No snapshots ni fuentes externas. Lista vacía en un catálogo vacío; 401/500 globales. Ruta literal separada de `/{id}` y de sincronización.

Opciones globales estables durante la sesión/caché de lectura, no facetado en cascada ni de la página cargada. No convertir selecciones incompatibles a otras opciones ni borrar filtros automáticamente. No inventar opción de nacionalidad desconocida. Revalidación normal al recuperar foco según política global; sin invalidaciones por leer otra página.

## Compatibilidad y pruebas previstas

No modificar POST /sync, identidad individual, estadísticas individuales, ranking, precios o sesión. Contrato OpenAPI y casos de Controller deben cubrir respuesta sin enriquecimiento idéntica, respuesta enriquecida nullable/cero/fuentes, todos los criterios y errores. Repository con PostgreSQL/Testcontainers prueba AND antes de paginar, conteo, orden/desempate, equivalencias «José»/«jose» y «Álvarez»/«alvarez», Unicode compuesto/descompuesto, comodines `%`/`_`/`!` literales y lote de snapshots; Service con frontera normativa y helper de Arrange. La migración de función/pg_trgm/GIN y su validación serán parte de implementación futura; aquí no se instalan ni se ejecutan. Ejecución de tests siempre humana; no tests frontend.

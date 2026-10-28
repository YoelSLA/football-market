# Modelo de datos: Detalle individual del jugador

## Modelo vigente — rediseño 2026-10-25

Este bloque gobierna el rediseño; lo posterior a «Antecedente histórico» conserva evidencia anterior. No introduce tablas ni modifica entidades de extracción. Identidad mantiene el contrato existente, separado de estadísticas.

### Entidades persistidas reutilizadas

| Entidad | Campos necesarios | Invariantes de consulta |
| --- | --- | --- |
| `Player` | ID, activo, equipo/liga, posición e identidad existentes | Visibilidad por `findVisibleById`; inexistente e inactivo indistinguibles |
| `PlayerStatistics` | ID, jugador, extractionDate, source, seasonKey/label, providerCompetitionId, providerTeamId, contextStatus, observedAt, runItem y 60 valores nullable | Única por jugador/fecha/canal; solo contexto RESOLVED con item COMPLETED coherente; no deducir confirmación por existencia |
| Item de extracción | ID, jugador, fecha, canal, result | Es el item referenciado por la fila; resultado COMPLETED tras publicación atómica |
| `MetricCatalog` | Clave canónica, unidad, orden y cobertura de canal | Exactamente 60 claves; no añadir métricas ni inferir disponibilidad por cobertura |

`observedAt` en la fila corresponde al instante de confirmación usado por el writer, no a un evento de partido. Mantener `extractionDate` como fecha visible de extracción.

### Model de lectura backend

`PlayerStatisticsSnapshot` (no entidad persistida): `playerId`, `requestedSeasonKey` nullable, `status` (`AVAILABLE`, `PARTIAL`, `NO_DATA`), `seasonKey`, `seasonLabel`, `extractionDate`, `competitionKey`, `competitionName` nullable y `metrics` de 60 entradas. Contexto nulo si no existe snapshot; `requestedSeasonKey` puede conservarse para explicar una búsqueda vacía. `competitionName` solo desde correspondencia local fiable de la clave de competición persistida; sin correspondencia, null y «Competición no disponible», sin inferir desde equipo/liga actuales ni afirmar que el nombre está almacenado en la fila.

Cada valor: `key`, `unit`, `value` (`BigDecimal` o null), `source` (`JSON`, `SCRAPING` o null). Fuente nula si valor nulo; cero conserva su fuente. Procedencia por métrica, no un único origen que oculte composición. `competitionKey` identifica el contexto persistido del proveedor, no el ID de liga local ni una URL; no usar el equipo actual para reetiquetarlo.

Selección: filtrar jugador, temporada si se pide y confirmación; fecha DESC → JSON antes de scraping para anclar contexto → observedAt DESC → ID DESC. Con ancla JSON, completar nulos solo desde scraping de igual jugador/seasonKey/fecha/competitionKey. Si JSON no existe, ancla scraping. No retroceder de fecha para rellenar ausencias. No unir temporadas o competiciones ni depender de estado global de run. Consultas dentro de transacción read-only repeatable-read; límite en Repository antes de materializar historia.

Disponibilidad del snapshot: 60 no nulos → AVAILABLE; 1–59 → PARTIAL; 0 o sin candidato → NO_DATA. Una fila de 57 valores JSON es PARTIAL respecto del catálogo completo, no error de extracción. `NO_DATA` de la respuesta no equivale al resultado interno del item. No imponer presencia de minutos/apariciones para mostrar valores conocidos.

### DTO y Model frontend

Contrato HTTP en [contracts/player-statistics.md](contracts/player-statistics.md), DTO record backend y `PlayerStatisticsResponseDTO` frontend conceptualmente separados de `PlayerStatistics` Model frontend. Service invoca Mapper tipado; UI nunca recibe DTO.

Metadatos frontend: `MetricDefinition` con key, label español, category y orden estable de spec; nombres/categorías/unidades visibles de [contracts/metric-glossary.md](contracts/metric-glossary.md), unidades técnicas contrastadas con backend. `PositionProfile` selecciona exactamente cuatro tarjetas, series y adicionales de spec. Posiciones: Goalkeeper→arquero, Defence/Defender→defensor, Midfield/Midfielder→mediocampista, Offence/Forward→delantero. No crear subposiciones; desconocida presenta generales y aviso.

Relevancia P/S/B fija por posición, independiente de `value !== null`. Titularidades principal en cuatro perfiles; tiros al arco B/S/S/P; segunda amarilla S/S/S/S. No modificar tarjetas o series al recibirlas. Categorías y cantidades: General 5, Ataque 19, Pases 10, Regates 5, Duelos 3, Defensa 10, Portería 4, Disciplina 4.

### Estado de interacción

- Identidad: loading, invalid-id, not-found, error, ready (sesión invalidada por infraestructura existente).
- Estadísticas: idle hasta identidad vigente → loading → error sin respuesta o ready con disponibilidad AVAILABLE/PARTIAL/NO_DATA. Refetch del mismo jugador/temporada conserva la última respuesta exitosa de Query Cache, su disponibilidad y fecha original; error de actualización se comunica además del contenido, con advertencia y reintento, nunca simula ausencia confirmada ni éxito nuevo. Cambio de contexto retira valores incompatibles. Sin temporada explícita, se conserva el último contexto resuelto del mismo jugador como lectura anterior, no se adivina temporada nueva. Una nueva respuesta exitosa NO_DATA reemplaza la anterior. No duplicar respuesta en un store para implementar esta regla.
- Estado cliente local: pestaña inicial Resumen, texto y categoría; reset al cambiar ID. La temporada resuelta no crea selector. Query cache es única autoridad de datos remotos.
- Navegación A→B: no presentar respuesta de A bajo B ni hacer una request para A en el render inicial de B; cancelar mediante signal y validar el ID vigente. Respuesta tardía no modifica controles ni contexto de B.
- Filtros: intersección categoría/texto normalizado; ordenar disponibles P→S→B y luego ausentes, con desempate por orden canónico. Cero participa como disponible. Error y carga no convierten las 60 filas en ausencias confirmadas.

Sin modelos de valoración, precios, percentiles o radar en el diseño vigente.

---

## Antecedente histórico del modelo anterior

## Contratos HTTP

### PlayerDetailResponseDTO (nuevo, `GET /api/players/{id}`)

Reutiliza el mismo conjunto de campos que `PlayerResponseDTO` del catálogo; la respuesta
individual no introduce campos nuevos.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `number` | Identificador interno requerido. |
| `name` | `string` | Nombre requerido. |
| `teamId` | `number` | Identificador interno del equipo actual; requerido. |
| `teamName` | `string` | Nombre del equipo actual; requerido. |
| `leagueId` | `number` | Identificador interno de la liga; requerido. |
| `leagueName` | `string` | Nombre de la liga; requerido. |
| `position` | `string` | Posición persistida; requerida, sin catálogo cerrado. |
| `dateOfBirth` | `string \| null` | Fecha `yyyy-MM-dd` o `null` si la fuente no la informa. |
| `nationality` | `string \| null` | Nacionalidad o `null`. |
| `imageUrl` | `string \| null` | Retrato principal persistido o `null`. |
| `fallbackImageUrl` | `string \| null` | Retrato alternativo o `null`. |

Reglas transversales:

- La respuesta solo contiene jugadores que cumplen la visibilidad del catálogo: `active = true`,
  equipo vigente y liga válida. Un jugador existente pero no visible responde `404` con el
  contrato global de errores, sin revelar identidad ni estado.
- No se exponen identificadores ni referencias de proveedores, ni información de scraping,
  imágenes, sincronizaciones o auditoría.
- No se incluyen estadísticas ni cotizaciones en esta respuesta: pertenecen a la fuente de
  demostración del frontend y a la integración posterior.

### ErrorResponseDTO (contrato global existente, sin cambios)

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `timestamp` | `string` | Momento de la respuesta. |
| `status` | `number` | Código HTTP; en esta consulta, 400, 401 o 404. Los fallos técnicos locales de lectura no tienen código asignado en esta SPEC. |
| `error` | `string` | Razón HTTP. |
| `code` | `string` | Identificador estable del error. |
| `message` | `string` | Texto legible, nunca identificador programático. |
| `path` | `string` | Ruta solicitada. |

Códigos previstos para la consulta individual:

| Situación | HTTP | `code` |
| --- | --- | --- |
| `id` con formato inválido | 400 | `INVALID_PARAMETER_TYPE` (handler existente) |
| Sin sesión válida o vencida | 401 | Sin cuerpo contractual (comportamiento vigente) |
| Jugador inexistente, inactivo o no visible | 404 | Código estable propio de «jugador no encontrado» |
| Fallo técnico local al resolver la identidad | Sin código asignado en esta SPEC | La UI lo trata como error de comunicación con reintento |

## Model frontend

### PlayerDetail

Representación de la identidad real consultada. Reutiliza el concepto `Player` del catálogo
ampliado con los campos que el catálogo ya expone pero la tarjeta no usa.

| Campo | Tipo | Origen y reglas |
| --- | --- | --- |
| `id` | `number` | Del DTO; identifica también la fuente de demostración. |
| `name` | `string` | Del DTO. |
| `teamId` / `teamName` | `number` / `string` | Del DTO. |
| `leagueId` / `leagueName` | `number` / `string` | Del DTO. |
| `position` | `string` | Del DTO. |
| `dateOfBirth` | `string \| null` | Del DTO; `null` se presenta como «No disponible». |
| `nationality` | `string \| null` | Del DTO; mismo tratamiento que la fecha. |
| `imageUrl` / `fallbackImageUrl` | `string \| null` | Del DTO; se prueban en orden antes del icono genérico. |
| `active` | `true` | Inferido por el Mapper: la consulta individual solo devuelve jugadores visibles. |

### PlayerSampleSeason (nuevo, demostración)

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `playerId` | `number` | Dueño de las estadísticas simuladas. |
| `season` | `string` | Temporada fija `2025/2026`. |
| `matches` | `number` | Entero `>= 0`. |
| `goals` | `number` | Entero `>= 0`; `0` cuando `matches = 0`. |
| `assists` | `number` | Entero `>= 0`; `0` cuando `matches = 0`. |
| `minutesPlayed` | `number` | Entero `>= 0`; `0` cuando `matches = 0`. |
| `isDemo` | `true` | Marca de procedencia; alimenta la etiqueta «Datos de demostración». |

Invariantes:

- Los cuatro valores se derivan de una función determinista de (`playerId`, `season`): el mismo
  par produce siempre el mismo resultado.
- No se exige que pares distintos produzcan combinaciones distintas. La spec solo requiere que
  el valor no sea un bloque fijo común a todo el catálogo, sin unicidad matemática por métrica.
- `matches = 0` implica goles, asistencias y minutos `0`.
- No se persiste ni se envía al backend.

### TokenQuotePoint (nuevo, demostración)

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `playerId` | `number` | Dueño de la serie. |
| `date` | `string` | Fecha `yyyy-MM-dd` del período fijo de demostración. |
| `price` | `number` | Precio del token en créditos, `>= 0`. |
| `isDemo` | `true` | Marca de procedencia. |

Invariantes:

- El caso normal genera una cotización diaria durante treinta días consecutivos del período fijo
  de demostración, ordenadas cronológicamente y estables entre recargas.
- El precio y la fecha actuales coinciden exactamente con el último punto.
- No depende del reloj del navegador ni de aleatoriedad por renderizado.

### TokenQuoteSnapshot (nuevo, demostración)

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `playerId` | `number` | Dueño de la serie. |
| `price` | `number \| null` | Último punto de la serie; `null` si la serie está vacía. |
| `date` | `string \| null` | Fecha del último punto; `null` si la serie está vacía. |
| `absoluteChange` | `number \| null` | Último menos anterior; `null` con menos de dos puntos. |
| `percentageChange` | `number \| null` | `null` con un punto, serie vacía o precio anterior `0`. |
| `direction` | `"up" \| "down" \| "flat" \| "unknown"` | Señal derivado del cambio; `unknown` con menos de dos puntos; nunca el único indicador. |
| `isDemo` | `true` | Marca de procedencia. |

Representación de una serie vacía: `price` y `date` son `null`, `absoluteChange` y
`percentageChange` son `null` y `direction` es `unknown`. No se inventa un precio de cero ni una
fecha sustituta; la UI informa que no hay historial disponible.

### TokenQuoteSeries (nuevo, demostración)

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `playerId` | `number` | Dueño de la serie. |
| `points` | `TokenQuotePoint[]` | Ordered cronológicamente; vacío y de un punto son estados válidos. |
| `snapshot` | `TokenQuoteSnapshot` | Derivado de `points`; nunca calculado de forma independiente. |

## Constantes de demostración

| Constante | Valor | Uso |
| --- | --- | --- |
| Temporada deportiva | `2025/2026` | Etiqueta común de estadísticas y resumen deportivo. |
| Período financiero | Treinta días consecutivos con una cotización diaria | Serie de cotización; fechas fijas, no relativas al reloj. No se exige que coincidan con la temporada deportiva 2025/2026. |
| Unidad de cotización | Créditos | Etiqueta de precio del token. |

La elección concreta de las treinta fechas del período se define en la implementación como
constantes fijas del módulo de demostración, independientes del reloj y estables para toda la
demostración. No se exige que ese período coincida con la temporada deportiva 2025/2026, cuya
relación con las cotizaciones no está definida por el enunciado.

## Estados de la pantalla

| Estado | Origen | Contenido permitido |
| --- | --- | --- |
| `loading` | Consulta de identidad en curso | Indicador de carga y navegación; sin datos. |
| `not-found` | `404` | «Jugador no encontrado» y regreso al catálogo; sin simulaciones. |
| `invalid-id` | `400` o id no numérico | Mensaje de identificador inválido y regreso. |
| `unauthenticated` | `401` | Flujo de acceso existente; sin datos privados. |
| `error` | Fallo técnico o de red | Mensaje y reintento; sin identidad ficticia. |
| `ready` | Identidad real resuelta | Identidad, estadísticas y cotización de demostración. |

## Recursos gráficos decorativos (2026-10-13)

No forman parte del modelo: no se persisten, no viajan por la API y no amplían el contrato. Se
resuelven en la capa de presentación a partir de datos ya presentes en la identidad del jugador.

| Recurso | Origen del dato | Procedencia | Resultado |
| --- | --- | --- | --- |
| Escudo del equipo | `teamName` | Registro local de escudos importados en build, con coincidencia por nombre normalizado | URL de recurso o `null` ante ausencia o colisión |
| Bandera de nacionalidad | `nationality` | Servicio externo de banderas consultado en runtime | URL de recurso o `null` ante ausencia |
| Retrato | `imageUrl`, `fallbackImageUrl` | Contrato del backend, con icono genérico local como último recurso | Recurso local del icono genérico |

`null` produce un marcador de posición accesible y nunca oculta el dato textual. La bandera es la
única dependencia de red añadida el 2026-10-13 y su falla no puede impedir comprender el catálogo.

## Relación entre entidades

```text
PlayerDetail (identidad real)
  ├─ informa breadcrumb, ficha principal y navbar activa
  └─ PlayerSampleSeason (por playerId + 2025/2026)
  └─ TokenQuoteSeries (por playerId, 30 puntos fijos)
        └─ TokenQuoteSnapshot (derivado del último y penúltimo punto)
```

Las entidades de demostración dependen solo de `playerId`; nunca se asocian a un ID no resuelto.

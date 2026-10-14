# Modelo de datos: Detalle individual del jugador

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
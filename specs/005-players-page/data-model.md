# Modelo de datos: Página de catálogo de jugadores

## Contratos HTTP

### PlayerResponseDTO

Representa exactamente un elemento de la respuesta de `GET /api/players`.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `id` | `number` | Identificador externo requerido. |
| `name` | `string` | Nombre requerido; la UI debe contener nombres largos sin invadir otras zonas. |
| `team` | `string` | Equipo actual requerido. |
| `league` | `string` | Liga requerida; puede no coincidir con un recurso conocido. |
| `position` | `string` | Posición requerida; puede no coincidir con un recurso conocido. |

### PlayersPageResponseDTO

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `content` | `PlayerResponseDTO[]` | Jugadores activos de la página; puede estar vacío. |
| `page` | `number` | Entero desde cero. |
| `size` | `number` | Entero entre 1 y 100; la feature solicita siempre 12. |
| `totalElements` | `number` | Entero no negativo. |
| `totalPages` | `number` | Entero no negativo. |

## Models frontend

### Player

| Campo | Tipo | Origen y reglas |
| --- | --- | --- |
| `id` | `number` | Copiado del DTO. |
| `name` | `string` | Copiado del DTO. |
| `team` | `string` | Copiado del DTO. |
| `league` | `string` | Copiado sin cast cerrado para permitir fallback visual. |
| `position` | `string` | Copiado sin cast cerrado para permitir fallback visual. |
| `active` | `true` | Inferido por el Mapper; el endpoint solo devuelve jugadores activos. |

### PlayersPage

| Campo | Tipo | Origen y reglas |
| --- | --- | --- |
| `players` | `Player[]` | Transformación de `content`. |
| `page` | `number` | Índice remoto desde cero. |
| `size` | `number` | Tamaño informado por backend. |
| `totalElements` | `number` | Total de jugadores activos. |
| `totalPages` | `number` | Total de páginas disponibles. |

### PlayerSampleStatistics

Datos numéricos de presentación de las tarjetas, derivados de forma determinista del identificador
del jugador y no como respuesta remota. Se muestran en el mismo bloque central que el equipo real de
`Player`, pero este no forma parte de `PlayerSampleStatistics`.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `matches` | `number` | **Derivado de forma determinista del `id` del jugador**, mostrado con la etiqueta `Partidos`. |
| `goals` | `number` | **Derivado de forma determinista del `id` del jugador**, mostrado con la etiqueta `Goles`. |
| `assists` | `number` | **Derivado de forma determinista del `id` del jugador**, mostrado con la etiqueta `Asistencias`. |

**Revisión 2026-10-13**: estos valores dejaron de ser constantes comunes `24`/`8`/`5` y pasaron a
producirse con el mismo generador determinista que usa el detalle del jugador, etiquetados como
«Datos de demostración». La fuente sigue siendo de presentación: no se persisten ni se envían al
backend, y el equipo se muestra con su propia etiqueta y contraste suficiente respecto del fondo.
El concepto completo, incluidas la temporada y la invariante `matches = 0`, se define en
`../009-player-detail/data-model.md` (`PlayerSampleSeason`).

### PlayersPageState

Estado coordinado por el Page Hook.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `selectedPage` | `number` | Índice desde cero, inicialmente 0 y nunca negativo. |
| `searchText` | `string` | Estado visual; no altera query, resultados ni página. |
| `selectedLeague` | `string` | Estado visual; no altera query, resultados ni página. |
| `selectedPosition` | `string` | Estado visual; no altera query, resultados ni página. |

Los estados remotos `loading`, `error`, `empty` y `populated` derivan de TanStack Query y de `PlayersPage`; no se duplican en un store.

## Relaciones

- `PlayersPage` contiene cero o más `Player` y resume su paginación.
- Cada `Player` se presenta mediante una tarjeta cuyo bloque central combina su `team` real con `PlayerSampleStatistics` comunes.
- `PlayersPageState.selectedPage` determina la clave de query y el parámetro `page`; los tres controles visuales quedan deliberadamente fuera de ambos.
- Liga y posición seleccionan recursos mediante mappings de presentación, pero el texto original siempre permanece disponible.

## Transiciones

### Consulta y navegación

1. La ruta privada autoriza al usuario y el estado inicia con `selectedPage = 0`.
2. La query solicita `page=selectedPage&size=12` y la interfaz entra en carga.
3. En éxito con contenido, se presenta `populated`; en éxito con `totalPages = 0`, se presenta `empty` sin paginador.
4. Si el resultado está vacío, `totalPages > 0` y `selectedPage >= totalPages`, el Page Hook cambia a `totalPages - 1` y dispara la query de esa última página válida.
5. Una selección válida distinta cambia `selectedPage`; la clave y la señal de aborto aíslan la solicitud más reciente.
6. Un error recuperable produce `error`; reintentar vuelve a consultar la misma selección.
7. Un `401` finaliza la sesión mediante infraestructura y el guard dirige a `/login`.

### Paginador

- Solo existe cuando `totalPages > 1`.
- La interfaz muestra numeración desde uno aunque el Model conserve índice desde cero.
- Expone la actual, la anterior, la posterior y accesos a primera y última. **Revisión 2026-10-13**: la ventana de tres anteriores y tres posteriores queda sustituida por la ventana vigente ±1, con elipsis cuando existen páginas omitidas y listado completo cuando el rango es pequeño. La implementación es el componente compartido `shared/components/Pagination`, componible desde `app/layouts/PaginatedPageLayout`.
- Primera/última y cualquier destino fuera de `[0, totalPages - 1]` no son activables.

### Tarjeta

- Estado base: toda la información esencial permanece visible.
- Hover: la tarjeta se eleva y su brillo se refuerza, sin reflujo ni ocultación esencial. **Sustitución aprobada el 2026-10-13** (SPEC-005 FR-010): el indicador lateral que entraba desde el borde izquierdo queda derogado.
- El estado `Activo` permanece visible de forma permanente, con o sin hover, conforme a FR-011.
- Fin de hover: la tarjeta recupera su posición y brillo originales.
- Con el alcance responsive del 2026-10-13, la tarjeta debe ser utilizable también con teclado y en pantallas táctiles, sin depender del hover para la información esencial.

## Validaciones y fallbacks

- `Premier League`, `Bundesliga`, `Primera Division`, `Serie A` y `Ligue 1` tienen recurso y color suave propios.
- `Goalkeeper`, `Defence`, `Midfield` y `Offence` se identifican mediante una etiqueta textual visible y accesible. **Sustitución aprobada el 2026-10-13** (SPEC-005 FR-009): el recurso gráfico de posición queda derogado y el texto es el mecanismo único.
- `Premier League`, `Bundesliga`, `Primera Division`, `Serie A` y `Ligue 1` conservan su recurso gráfico; la posición ya no consume recurso gráfico alguno.
- El escudo del equipo se resuelve desde un registro local importado en build; la bandera de nacionalidad, desde un servicio externo. Ambos son adornos con marcador de posición accesible y no forman parte del modelo.
- Imagen y color complementan, pero nunca sustituyen, los textos de liga, posición y estado.
- Los controles admiten interacción, pero sus valores no pueden modificar contenido, query key, parámetros, orden o página.

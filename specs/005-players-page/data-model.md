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

Datos de presentación comunes a todas las tarjetas, definidos como constantes y no como respuesta remota.

| Campo | Tipo | Reglas |
| --- | --- | --- |
| `matches` | `number` | Valor fijo compartido, etiquetado como dato de muestra. |
| `goals` | `number` | Valor fijo compartido, etiquetado como dato de muestra. |
| `assists` | `number` | Valor fijo compartido, etiquetado como dato de muestra. |

Los valores concretos se fijarán en implementación y deberán ser iguales en todas las tarjetas. No se persistirán ni enviarán al backend.

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
- Cada `Player` se presenta mediante una tarjeta con `PlayerSampleStatistics` comunes.
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
- Expone la actual, hasta tres anteriores, hasta tres posteriores y accesos a primera y última.
- Primera/última y cualquier destino fuera de `[0, totalPages - 1]` no son activables.

### Tarjeta

- Estado base: toda la información esencial permanece visible.
- Hover: el indicador `Activo` entra desde el borde izquierdo sin reflujo ni ocultación esencial.
- Fin de hover: el indicador vuelve a su posición oculta.
- No se define transición por click, foco o touch porque el alcance es exclusivamente escritorio y hover.

## Validaciones y fallbacks

- `Premier League`, `Bundesliga`, `Primera Division`, `Serie A` y `Ligue 1` tienen recurso y color suave propios.
- `Goalkeeper`, `Defence`, `Midfield` y `Offence` tienen recurso propio.
- Todo valor no reconocido conserva su texto y usa representación neutra.
- Imagen y color complementan, pero nunca sustituyen, los textos de liga, posición y estado.
- Los controles admiten interacción, pero sus valores no pueden modificar contenido, query key, parámetros, orden o página.

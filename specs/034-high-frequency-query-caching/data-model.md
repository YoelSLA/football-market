# Modelo de datos en memoria: caché de consultas frecuentes

Esta feature no introduce tablas, migraciones ni entidades persistidas. PostgreSQL y los Models
actuales siguen siendo la fuente canónica.

## Consulta cacheable

Una consulta identificada por el nombre de su caché y una clave inmutable formada por todos los
criterios efectivos que determinan el resultado.

- Ranking: consulta global o criterios de filtro, orden y paginación.
- Filtros de catálogo: clave única, independiente de página.
- Listado de jugadores: filtros normalizados, orden, página, tamaño y opciones que cambien el
  contenido, incluido `includeStatistics`.
- Detalle de jugador: ID interno.
- Mercado oficial: filtros, disponibilidad de cotización, rangos, orden y paginación.
- Mercado P2P: `viewerId` autenticado, búsqueda, filtros, rango de precio, orden y paginación.
- Valores ausentes y explícitos equivalentes se normalizan según el contrato actual; la caché no
  cambia semántica de parámetros ni sus valores predeterminados.

## Resultado cacheado

Resultado de aplicación no nulo producido por la consulta existente. Puede representar una lista o
página vacía. No se cachean excepciones ni DTO HTTP, entidades gestionadas por JPA o respuestas de
operaciones de escritura.

## Evento de actualización

Notificación interna emitida tras completar correctamente una operación existente. Nombra el
conjunto afectado para que el listener retire sus entradas:

| Evento de origen | Cachés retiradas |
| --- | --- |
| Recálculo de cotizaciones completado | Ranking por score y mercado oficial |
| Sincronización de jugadores confirmada | Filtros, listado y detalle de catálogo |
| Mutación de mercado completada | Mercado oficial, P2P o ambos según los datos modificados |

En mutaciones transaccionales, el listener aplica la invalidación únicamente después del commit.
Un fallo o rollback no se trata como actualización completada.

## Políticas de ciclo de vida

| Caché | Expiración máxima | Entradas máximas |
| --- | ---: | ---: |
| Ranking por score | 5 minutos | 128 |
| Filtros de catálogo | 60 minutos | 1 |
| Listado de jugadores | 5 minutos | 256 |
| Detalle de jugador | 30 minutos | 1.024 |
| Mercado oficial | 1 minuto | 128 |
| Mercado P2P | 10 segundos | 1.024 |

La expiración se mide desde la escritura. Las entradas pueden retirarse antes por invalidación.
Un reinicio descarta la caché completa y las consultas siguientes recargan desde PostgreSQL.

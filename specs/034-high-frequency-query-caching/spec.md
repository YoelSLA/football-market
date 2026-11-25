# Feature Specification: Caché de consultas frecuentes del backend

**Feature Branch**: `feat/034-high-frequency-query-caching`

**Created**: 2026-11-25

**Status**: Ready for planning

**Input**: REQ-S3-04, issue #101: reducir la latencia de las consultas frecuentes del ranking, el catálogo de jugadores y los mercados oficial y P2P, evitando consultas redundantes y pesadas a PostgreSQL. Definir una caché de backend con Spring Cache y Caffeine en memoria, claves determinísticas que consideren filtros y paginación y, para P2P, el usuario consultante (`viewerId`); TTL y tamaños máximos acordes con la volatilidad, con aproximadamente 10 segundos para P2P; invalidación al terminar el recálculo de cotizaciones y al completar una sincronización de jugadores; consistencia ante mutaciones de mercado incluidas en el alcance; fallback a PostgreSQL ante ausencia, expiración o reinicio, sin errores ni cambios en los contratos. Incluir `GET /api/rankings/score`, `GET /api/players/filters`, `GET /api/players`, `GET /api/players/{id}`, `GET /api/market/official` y `GET /api/market/p2p`. Excluir endpoints transaccionales, consultas personales en tiempo real y caché distribuida con Redis.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar rankings y jugadores (Priority: P1)

Como usuario que consulta el ranking o el catálogo, quiero reutilizar resultados de consultas equivalentes mientras sus datos sigan vigentes, para evitar lecturas repetidas y pesadas sin perder la correspondencia entre los filtros solicitados y los resultados.

**Why this priority**: El ranking y el catálogo son las consultas de lectura identificadas por REQ-S3-04 y constituyen el núcleo del alcance.

**Independent Test**: Consultar repetidamente cada endpoint con la misma selección efectiva y comprobar que la respuesta se conserva y que las variantes de filtros o página no reutilizan resultados ajenos.

**Acceptance Scenarios**:

1. **Given** que no existe un resultado vigente para una consulta, **When** el cliente consulta el ranking, los filtros del catálogo, una página del catálogo o el detalle de un jugador, **Then** el sistema obtiene el resultado desde PostgreSQL y lo hace disponible para consultas equivalentes posteriores.
2. **Given** un resultado vigente de una consulta filtrada y paginada, **When** se repite esa consulta, **Then** se entrega el resultado correspondiente a esa combinación exacta, sin sustituirlo por el de otros filtros, ordenamientos o páginas.
3. **Given** dos consultas cuyos filtros, paginación u otros parámetros que determinan la respuesta difieren, **When** se ejecutan, **Then** cada una se resuelve con su propio resultado correcto.

---

### User Story 2 - Consultar mercados con datos pertinentes al usuario (Priority: P1)

Como usuario del mercado oficial o P2P, quiero recibir resultados vigentes para la consulta solicitada y para mi identidad cuando corresponda, para que la reutilización no muestre información personalizada de otro usuario.

**Why this priority**: Los datos del mercado pueden cambiar y P2P depende del usuario que consulta.

**Independent Test**: Repetir consultas idénticas de ambos mercados y comparar consultas P2P realizadas por distintos `viewerId`, verificando que cada respuesta corresponde a su endpoint y contexto.

**Acceptance Scenarios**:

1. **Given** una consulta P2P con resultado vigente para un `viewerId`, **When** el mismo usuario repite los mismos parámetros, **Then** recibe el resultado asociado a su consulta.
2. **Given** dos usuarios distintos que consultan los mismos parámetros P2P, **When** sus consultas se resuelven, **Then** la identidad de cada usuario distingue los resultados y ninguno recibe el resultado personalizado del otro.
3. **Given** resultados de consultas oficiales o P2P con parámetros diferentes, **When** se repiten las solicitudes, **Then** se devuelve el resultado de la combinación de parámetros correspondiente.

---

### User Story 3 - Mantener lecturas coherentes tras cambios y recuperar datos faltantes (Priority: P1)

Como usuario de la plataforma, quiero que las consultas reflejen los recálculos, sincronizaciones y mutaciones de mercado completados, y que sigan funcionando si la caché está vacía, expirada o reiniciada.

**Why this priority**: La reutilización de resultados no debe sustituir datos actualizados ni impedir que una lectura se atienda desde la fuente persistente.

**Independent Test**: Precargar resultados, completar cada operación de actualización correspondiente y volver a consultar; además, consultar con caché fría, expirada o reiniciada y verificar una respuesta normal de PostgreSQL.

**Acceptance Scenarios**:

1. **Given** resultados cacheados de ranking y mercado oficial, **When** termina correctamente un recálculo de cotizaciones, **Then** las siguientes consultas a dichos endpoints se resuelven con datos posteriores al recálculo.
2. **Given** resultados cacheados del catálogo, **When** termina correctamente una sincronización de jugadores, **Then** las siguientes consultas de filtros, listado y detalle se resuelven con datos posteriores a la sincronización.
3. **Given** resultados cacheados de mercado, **When** se completa una mutación existente que altera los datos expuestos por un endpoint de mercado, **Then** las siguientes consultas afectadas no devuelven el resultado anterior a esa mutación.
4. **Given** que el resultado no está en caché, expiró o se reinició el proceso, **When** se consulta cualquiera de los endpoints incluidos, **Then** se consulta PostgreSQL y se conserva la respuesta y el comportamiento HTTP existentes.

## Edge Cases

- Claves para peticiones con valores por defecto explícitos o implícitos: parámetros equivalentes según el contrato vigente deben identificar la misma consulta efectiva.
- Respuestas de página vacía y resultados de filtros sin opciones deben conservar su respuesta actual y poder volver a obtenerse desde PostgreSQL tras una ausencia, expiración o reinicio.
- Dos solicitudes concurrentes de la misma clave no deben provocar que una respuesta asociada a filtros, página o usuario diferente se entregue al solicitante.
- Un fallo antes de que una operación de recálculo, sincronización o mutación se complete no debe presentarse como una actualización completada; la invalidación de esa operación se produce tras su finalización correcta.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El backend MUST aplicar una caché local en memoria mediante Spring Cache y Caffeine a las lecturas de `GET /api/rankings/score`, `GET /api/players/filters`, `GET /api/players`, `GET /api/players/{id}`, `GET /api/market/official` y `GET /api/market/p2p`.
- **FR-002**: Las claves MUST ser determinísticas y distinguir toda entrada que pueda cambiar el resultado de la respuesta. Para listados y mercados paginados MUST incluir los filtros efectivos, la paginación y los demás parámetros de consulta que alteren el resultado; los filtros del catálogo MUST considerar sus criterios efectivos. La consulta de detalle MUST distinguir el identificador del jugador.
- **FR-003**: La clave P2P MUST incluir el `viewerId` autenticado además de todos los parámetros que determinan el resultado. La identidad MUST obtenerse del contexto autenticado existente, no de un valor de cliente que permita atribuir la respuesta a otro usuario.
- **FR-004**: La política inicial de duración y límite máximo de entradas MUST ser la siguiente, con cachés diferenciadas por tipo de dato:

  | Consulta | Duración máxima | Máximo de entradas |
  | --- | ---: | ---: |
  | Ranking por score | 5 minutos | 128 |
  | Filtros del catálogo | 60 minutos | 1 |
  | Listado de jugadores | 5 minutos | 256 |
  | Detalle de jugador | 30 minutos | 1.024 |
  | Mercado oficial | 1 minuto | 128 |
  | Mercado P2P | 10 segundos | 1.024 |

- **FR-005**: Al completar correctamente un recálculo de cotizaciones, el sistema MUST invalidar los resultados cacheados del ranking por score y del mercado oficial antes de que nuevas consultas puedan reutilizarlos.
- **FR-006**: Al completar correctamente una sincronización de jugadores, el sistema MUST invalidar los resultados cacheados de filtros, listado y detalle del catálogo.
- **FR-007**: Cuando una mutación de mercado existente y exitosa cambie información expuesta por `GET /api/market/official` o `GET /api/market/p2p`, el sistema MUST invalidar el conjunto afectado de resultados de lectura para que las solicitudes posteriores no reutilicen el estado anterior. La invalidación MUST ocurrir después de que la mutación se complete correctamente. Esta exigencia no incorpora endpoints transaccionales al alcance ni altera sus contratos.
- **FR-008**: Ante una ausencia de resultado cacheado, expiración o reinicio del proceso, el sistema MUST resolver la consulta desde PostgreSQL y entregar la respuesta según el contrato HTTP vigente, sin requerir una inicialización o recuperación manual de la caché.
- **FR-009**: La caché MUST ser local al backend; Redis y cualquier caché distribuida quedan fuera del alcance.
- **FR-010**: La incorporación de caché MUST mantener los contratos existentes de los seis endpoints: ruta, parámetros, formato y significado de respuesta, autenticación, códigos y comportamiento de errores. La caché no MUST incorporar consultas personales de datos en tiempo real.

### Key Entities *(include if feature involves data)*

- **Consulta cacheable**: resultado de uno de los seis endpoints junto con los parámetros efectivos que identifican inequívocamente la consulta; para P2P incluye además el `viewerId`.
- **Resultado de catálogo**: opciones de filtro, página de jugadores o detalle individual, con la vigencia acotada por su política y por la finalización de una sincronización.
- **Resultado de mercado**: respuesta oficial o P2P, con vigencia acotada por su política y por las operaciones de actualización que afectan sus datos.
- **Evento de actualización**: finalización exitosa de un recálculo, una sincronización o una mutación de mercado que requiere invalidar resultados de lectura dependientes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Para cada uno de los seis endpoints, una consulta repetida con la misma clave durante su vigencia devuelve el resultado de esa misma combinación; cambiar cualquier criterio que determine la respuesta no devuelve el resultado de la combinación anterior.
- **SC-002**: En verificaciones de ciclo de vida, las consultas posteriores a un recálculo, sincronización o mutación de mercado completados correctamente no muestran los resultados que precedían al evento para los conjuntos de datos afectados.
- **SC-003**: Para cada uno de los seis endpoints, una consulta con caché ausente, expirada o reiniciada obtiene la respuesta desde PostgreSQL y conserva el contrato HTTP vigente.
- **SC-004**: Dos consultas P2P con parámetros iguales y distinto `viewerId` quedan separadas; no se entrega a un usuario el resultado personalizado del otro.
- **SC-005**: Cada caché mantiene sus entradas dentro del límite máximo y duración especificados en FR-004, incluida la duración aproximada de 10 segundos para P2P.

## Assumptions

- Los valores de TTL y límites de FR-004 son valores iniciales funcionales para esta feature, elegidos según la volatilidad relativa de los datos. No representan objetivos cuantitativos de latencia, tasa de aciertos o reducción porcentual de consultas.
- La equivalencia de parámetros se determina por los valores efectivos conforme a los valores por defecto y reglas del contrato existente. No se cambia la interpretación de esos parámetros.
- Las operaciones existentes que recalculan cotizaciones, sincronizan jugadores o mutan el mercado son responsables de notificar la invalidación correspondiente únicamente tras su finalización correcta. La especificación cubre ese efecto sobre las lecturas, no la implementación de dichas operaciones ni sus endpoints transaccionales.
- La expiración es un límite máximo de reutilización; las invalidaciones explícitas pueden retirar resultados antes de dicho límite.
- La caché no es persistente: al iniciar o reiniciar el backend, los resultados se reconstruyen consultando PostgreSQL.

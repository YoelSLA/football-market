# Feature Specification: Historial personal de operaciones (Backend)

**Feature Branch**: `feat/020-view-operation-history`
**Estado**: Especificación viva del backend.
**Referencias normativas**: [contracts/operation-history-api.md](contracts/operation-history-api.md), [data-model.md](data-model.md) y [SPEC-025](../../specs/025-view-transaction-history/spec.md).

---

## Clarifications

- **Identidad de sesión**: La identidad del usuario se obtiene exclusivamente del JWT autenticado. No se admite parámetro de usuario que permita consultar historiales ajenos.
- **Fuente de verdad**: Toda transacción en `MarketTransaction` representa una operación confirmada y append-only; no existen transacciones pendientes en esta fuente.
- **Participantes distintos**: SPEC-010 y SPEC-011 garantizan que comprador y vendedor sean usuarios distintos; esta consulta interpreta cada transacción como `BUY` o `SELL` según el rol del usuario autenticado.
- **Inmutabilidad financiera**: Cantidad, precio unitario, importe total e instante de ejecución reflejan estrictamente los valores confirmados y no se recalculan con cotizaciones actuales.
- **Perfil actual del jugador**: Nombre, imágenes, equipo y liga representan el catálogo actual; no constituyen snapshots históricos.
- **Capacidades de consulta**: El endpoint incluye paginación en base de datos, filtros combinables (`search`, `type`, `executedFrom`, `executedBefore`), orden cerrado determinista y agregados (`summary`) sobre el conjunto filtrado.

---

## User Scenarios & Testing

### User Story 1 - Consultar el historial paginado con filtros y agregados (Priority: P1)

Como usuario autenticado, quiero consultar mis operaciones de compra y venta confirmadas con paginación, filtros y resumen de actividad para revisar mis movimientos financieros en el mercado.

**Independent Test**: Con un usuario con compras y ventas confirmadas, consultar `GET /api/users/me/transactions`: devuelve las transacciones propias interpretadas como `BUY` o `SELL`, con valores históricos, perfil actual del jugador, resumen agregado del conjunto filtrado y metadatos de paginación.

**Acceptance Scenarios**:
1. **Given** un usuario autenticado comprador en transacciones confirmadas, **When** consulta su historial, **Then** cada una aparece como `BUY` con identificador, perfil actual del jugador, cantidad, precio unitario, importe total y fecha/hora de ejecución.
2. **Given** un usuario autenticado vendedor en transacciones confirmadas, **When** consulta su historial, **Then** cada una aparece como `SELL` con los mismos datos históricos.
3. **Given** una consulta con parámetros de paginación (`page`, `size`), **When** se ejecuta, **Then** devuelve únicamente el subconjunto solicitado y metadatos con `totalElements` y `totalPages`.
4. **Given** filtros de búsqueda por nombre, tipo (`BUY`/`SELL`) o rango de fechas, **When** se aplican, **Then** `transactions`, `summary` y `pagination` aplican exactamente los mismos predicados.
5. **Given** un criterio de ordenamiento (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`), **When** se solicita, **Then** el resultado se ordena según el criterio con desempate determinista por ejecución e ID.

---

### User Story 2 - Proteger la privacidad del historial (Priority: P1)

Como usuario autenticado, quiero que mi historial sea estrictamente confidencial para que ningún tercero pueda acceder a mis transacciones privadas.

**Independent Test**: Dos usuarios autenticados distintos consultan el endpoint: cada uno obtiene únicamente sus operaciones y no existe parámetro alguno para solicitar la información de otro usuario.

**Acceptance Scenarios**:
1. **Given** dos usuarios con transacciones distintas, **When** consultan el endpoint, **Then** cada respuesta contiene únicamente operaciones donde el usuario autenticado fue comprador o vendedor.
2. **Given** una solicitud sin token JWT o con token inválido/expirado, **When** consulta el historial, **Then** se rechaza con HTTP 401 sin revelar datos.
3. **Given** una credencial válida cuyo subject no existe en la base de datos, **When** solicita el historial, **Then** se rechaza con HTTP 401.

---

### User Story 3 - Consultar un historial vacío o sin coincidencias (Priority: P2)

Como usuario sin transacciones o con filtros sin coincidencias, quiero recibir una respuesta exitosa con colección vacía e indicadores en cero para distinguir la ausencia de operaciones de un error.

**Independent Test**: Con un usuario nuevo sin operaciones o con filtros que no arrojan resultados, la consulta responde HTTP 200 con `transactions: []`, indicadores en cero y metadatos de paginación coherentes.

**Acceptance Scenarios**:
1. **Given** un usuario sin transacciones confirmadas, **When** consulta su historial, **Then** responde HTTP 200 con `transactions: []`, indicadores de `summary` en cero y `totalElements: 0`.
2. **Given** un usuario con transacciones que aplica filtros sin coincidencias, **When** ejecuta la consulta, **Then** responde HTTP 200 con `transactions: []`, indicadores de `summary` en cero y `totalElements: 0`.

---

### Edge Cases

- **Desempate determinista**: Dos operaciones con igual valor de ordenación se desempatan por fecha de ejecución descendente/ascendente e identificador descendente/ascendente, garantizando resultados reproducibles.
- **Página fuera de rango**: Si `page` excede las páginas disponibles, responde HTTP 200 con `transactions: []` y metadatos correctos de `totalElements` y `totalPages`.
- **Rango de fechas inválido**: Si `executedFrom` es igual o posterior a `executedBefore`, la petición se rechaza con HTTP 400 y código `INVALID_DATE_RANGE`.
- **Imágenes nulas**: Si el jugador carece de `imageUrl` o `fallbackImageUrl`, se devuelven como `null` en el JSON.
- **Transacciones de terceros**: Transacciones donde el usuario no participó como comprador ni vendedor nunca se incluyen, aun si refieren a un jugador que el usuario negoció en otra ocasión.

---

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exponer el endpoint `GET /api/users/me/transactions` para consultar el historial personal de operaciones financieras confirmadas del usuario autenticado.
- **FR-002**: La identidad del usuario MUST obtenerse exclusivamente del subject del JWT y el endpoint MUST NOT aceptar identificadores de usuario en ruta, consulta o cuerpo.
- **FR-003**: El historial MUST incluir exclusivamente transacciones confirmadas donde el usuario autenticado sea comprador o vendedor.
- **FR-004**: Cada transacción MUST aparecer exactamente una vez en la respuesta, tipificada como `BUY` si el usuario es comprador o como `SELL` si es vendedor.
- **FR-005**: Cantidad, precio unitario, importe total e instante de ejecución MUST reflejar los valores históricos confirmados y MUST NOT recalcularse con cotizaciones vigentes.
- **FR-006**: Cada operación MUST incluir el perfil actual del jugador: `id`, `name`, `imageUrl` (nullable), `fallbackImageUrl` (nullable), equipo actual (`id`, `name`) y liga actual (`id`, `name`).
- **FR-007**: El endpoint MUST soportar paginación server-side con parámetros `page` (default 0, `>= 0`) y `size` (default 20, entre 1 y 100), informando `page`, `size`, `totalElements` y `totalPages`.
- **FR-008**: El endpoint MUST admitir filtros opcionales: búsqueda de texto normalizado sobre el nombre del jugador (`search`), tipo de operación (`type: BUY|SELL`) y rango de fechas (`executedFrom` inclusivo, `executedBefore` exclusivo).
- **FR-009**: El endpoint MUST soportar ordenamiento cerrado mediante el parámetro `sort` con valores `NEWEST` (default), `OLDEST`, `AMOUNT_DESC` y `AMOUNT_ASC`, aplicando desempates estables por ejecución e ID.
- **FR-010**: El endpoint MUST devolver en `summary` los agregados calculados sobre todo el conjunto filtrado: `totalTransactions`, `buyTransactions`, `sellTransactions`, `boughtTokens`, `soldTokens`, `purchasedVolume` y `soldVolume` en centésimas de crédito.
- **FR-011**: Cuando no existan operaciones o coincidencias, el sistema MUST responder exitosamente HTTP 200 con `transactions: []`, indicadores en cero y metadatos de paginación válidos.
- **FR-012**: Peticiones con parámetros de tipo inválido MUST responder HTTP 400 con `INVALID_PARAMETER_TYPE`; rangos de fecha inconsistentes (`executedFrom >= executedBefore`) MUST responder HTTP 400 con `INVALID_DATE_RANGE`.
- **FR-013**: Solicitudes sin credenciales válidas o con un subject no asociado a un usuario persistido MUST responder HTTP 401 sin exponer información.
- **FR-014**: La consulta MUST ejecutarse bajo transacción de solo lectura (`@Transactional(readOnly = true)`) y MUST NOT ejecutar escrituras ni alterar datos financieros.
- **FR-015**: La consulta MUST paginar en base de datos y evitar el problema de N+1 mediante precarga de asociaciones to-one (`buyer`, `seller`, `player`, `team`, `league`).

---

## Key Entities

- **Transacción de mercado (`MarketTransaction`)**: Registro confirmado e inmutable en base de datos que constituye la fuente de verdad.
- **Entrada del historial (`OperationHistoryEntry`)**: Proyección de una transacción desde la perspectiva del usuario (`BUY` o `SELL`).
- **Agregados del historial (`OperationHistoryAggregate`)**: Totales e importes calculados sobre el conjunto de operaciones filtradas.
- **Criterios de consulta (`OperationHistoryCriteria`)**: Parámetros validados de paginación, filtros y orden.

---

## Success Criteria

- **SC-001**: En el 100% de las consultas, cada transacción propia aparece una sola vez con los datos financieros históricos intactos y el perfil actual del jugador.
- **SC-002**: En el 100% de las consultas paginadas, el resultado respeta el orden y desempates deterministas sin duplicaciones ni omisiones entre páginas.
- **SC-003**: En el 100% de las verificaciones con identidades distintas, ninguna respuesta expone operaciones ajenas ni admite suplantación de identidad.
- **SC-004**: En el 100% de las consultas sin operaciones, el sistema responde HTTP 200 con `transactions: []` y resumen en cero.

---

## Out of Scope

- Interfaz gráfica de usuario (especificada e implementada en SPEC-025).
- Exportación de transacciones a formatos externos (CSV, PDF, Excel).
- Modificaciones en las reglas contables, tablas de persistencia o migraciones de base de datos.

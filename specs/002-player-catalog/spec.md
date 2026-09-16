# Feature Specification: Player Catalog

**Feature Branch**: `[002-player-catalog]`

**Created**: 2026-09-15

**Status**: Draft

**Input**: User description: "Catálogo de jugadores para la Entrega N.º 1. API REST para consultar jugadores, persistencia local, integración con Football-Data.org con tolerancia a fallas, endpoint público GET /players." // No creo que haga falta mencionar la primer oracion, como tambien lo de la API

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar catálogo de jugadores (Priority: P1)

Como usuario o visitante, quiero poder consultar un catálogo de jugadores de fútbol para conocer información básica de los mismos.

**Why this priority**: Es la funcionalidad central de la Entrega N.º 1; permite que el sistema cumpla con su propósito fundamental de mostrar información de jugadores.

**Independent Test**: Se puede probar realizando una petición GET al endpoint /players y verificando que devuelve una lista de jugadores con los campos esperados (id, name, team, league, position).

**Acceptance Scenarios**:

1. **Given** que el sistema tiene jugadores persistidos localmente, **When** un usuario realiza una petición GET a /players, **Then** el sistema devuelve una lista JSON con los jugadores.
2. **Given** que la API externa Football-Data.org está disponible, **When** el sistema se inicializa o actualiza periódicamente, **Then** los datos se sincronizan y persisten localmente.
3. **Given** que la API externa Football-Data.org falla o está inaccesible, **When** un usuario realiza una petición GET a /players, **Then** el sistema devuelve los datos previamente persistidos sin error.

---

### Edge Cases

- **API externa inaccesible**: ¿Cómo se comporta el sistema si la API externa falla permanentemente? (El sistema debe usar datos de la BD local).
- **Datos incompletos**: ¿Qué sucede si la API externa devuelve jugadores sin alguno de los campos requeridos (id, name, team, league, position)? (El sistema debe omitir o marcar como inválidos esos registros para evitar inconsistencias).
- **BD local vacía**: ¿Qué ocurre si la BD local está vacía y la API externa también falla en el primer arranque? (El sistema debe devolver una lista vacía y registrar el error). // Excelente pregunta aca jaja

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST proporcionar un endpoint público `GET /players` que devuelva el catálogo completo de jugadores.
- **FR-002**: System MUST obtener datos de jugadores desde la API externa Football-Data.org.
- **FR-003**: System MUST persistir los jugadores obtenidos en la base de datos local.
- **FR-004**: System MUST tolerar fallas de la API externa; si falla, debe servir datos desde la base de datos local.
- **FR-005**: System MUST exponer los datos de jugadores mediante DTOs, NO mediante la entidad Player directamente.
- **FR-006**: System MUST documentar el endpoint `GET /players` mediante OpenAPI/Swagger.
- **FR-007**: El catálogo debe contener un mínimo inicial de 100 jugadores.

### Key Entities

- **Player**: Representa a un jugador de fútbol.
  - Attributes: id, name, team, league, position // Ahora que lo pienso esto puede variar dependiendo que datos traiga la api.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El endpoint `GET /players` responde exitosamente en menos de 500ms cuando hay datos persistidos. // no se mide metricas
- **SC-002**: El sistema recupera y persiste exitosamente un mínimo de 100 jugadores desde la API externa.
- **SC-003**: El sistema continúa funcionando y sirviendo datos locales cuando la API externa no está disponible.
- **SC-004**: La documentación OpenAPI para `GET /players` está completa y generada automáticamente.

## Assumptions

- **Formato de datos**: Se asume que los datos recibidos de Football-Data.org pueden ser mapeados al modelo `Player` (id, name, team, league, position).
- **Consistencia**: Se asume que el conjunto de 100 jugadores es suficiente para la Entrega N.º 1, sin necesidad de las cinco grandes ligas completas.
- **Acceso a red**: El entorno de ejecución tiene acceso a internet para consultar Football-Data.org.

## Out of Scope 
 
- **Integraciones adicionales**: WhoScored, scraping, etc.
- **Funcionalidades de usuario**: Autenticación, JWT, roles (USER/SUPERUSER).
- **Funcionalidades de mercado**: Cotizaciones, ranking, compra/venta de tokens, portfolio, transacciones.
- **Frontend**: Implementación de interfaz de usuario.
- **Filtros**: Cualquier parámetro de búsqueda o filtrado en `GET /players`.
- **Detalle de jugador**: Endpoint `GET /players/{id}`.

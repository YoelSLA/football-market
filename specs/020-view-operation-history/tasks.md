# Tareas: Historial personal de operaciones (Backend)

**Estado**: Implementado y verificado en backend.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md) y [contracts/operation-history-api.md](contracts/operation-history-api.md).

---

## Tareas de la funcionalidad vigente

### Fase 1 — Modelos de dominio y DTOs

- [x] **T01** Crear los records inmutables de dominio (`OperationHistoryEntry`, `OperationHistoryCriteria`, `OperationHistoryAggregate`, `OperationHistoryView`) en `backend/src/main/java/footballmarket/features/portfolio/domain/records/`.
- [x] **T02** Declarar el DTO de respuesta `OperationHistoryResponseDTO` con anotaciones OpenAPI, subdivisiones de `SummaryDTO`, `TransactionDTO`, `PlayerDTO`, `RelatedDTO` y `PaginationDTO` en `backend/src/main/java/footballmarket/features/portfolio/presentation/dtos/response/`.
- [x] **T03** Implementar el mapeador estático `OperationHistoryMapper` en `backend/src/main/java/footballmarket/features/portfolio/presentation/` para convertir vistas de dominio a DTOs de salida.
- [x] **T04** Implementar las excepciones de validación `InvalidOperationHistoryDateRangeException` e `InvalidOperationHistoryCriteriaException` y su manejo en `PortfolioExceptionHandler`.

### Fase 2 — Persistencia y consultas optimizadas

- [x] **T05** Implementar en `MarketTransactionRepository` (`backend/src/main/java/footballmarket/features/market/infraestructure/`) las consultas de contenido paginado con fetch joins (`buyer`, `seller`, `player`, `team`, `league`), consulta de conteo y consulta de agregación sobre el conjunto filtrado.

### Fase 3 — Lógica de negocio y frontera HTTP

- [x] **T06** Definir la interfaz `OperationHistoryService` e implementar `OperationHistoryServiceImpl` bajo `@Transactional(readOnly = true)` en `backend/src/main/java/footballmarket/features/portfolio/application/`, gestionando la resolución de identidad, validación de criterios, derivación `BUY`/`SELL` y composición de resultados.
- [x] **T07** Exponer el endpoint `GET /api/users/me/transactions` en `OperationHistoryController` (`backend/src/main/java/footballmarket/features/portfolio/presentation/`) con extracción de credencial JWT, documentación OpenAPI y respuestas HTTP 200, 400, 401 y 500.

### Fase 4 — Cobertura de pruebas

- [x] **T08** Implementar las pruebas de repositorio en `OperationHistoryPersistenceTest` (`backend/src/test/java/footballmarket/features/portfolio/infraestructure/`) con Testcontainers y PostgreSQL real.
- [x] **T09** Implementar las pruebas de servicio en `OperationHistoryServiceTest` (`backend/src/test/java/footballmarket/features/portfolio/application/`) para verificar derivación de tipos, validación de criterios y agregados.
- [x] **T10** Implementar las pruebas de controlador en `OperationHistoryControllerTest` (`backend/src/test/java/footballmarket/features/portfolio/presentation/`) mediante `@WebMvcTest` para validar contratos JSON, parámetros y manejo de errores.

---

## Matriz de trazabilidad

| Requisito | Criterio de éxito | Componentes afectados | Tarea |
| --- | --- | --- | --- |
| **FR-001** (Endpoint dedicado) | **SC-001** | `OperationHistoryController.java` | T07, T10 |
| **FR-002** (Identidad exclusiva JWT) | **SC-003** | `OperationHistoryController.java`, `OperationHistoryServiceImpl.java` | T06, T07 |
| **FR-003** (Transacciones confirmadas propias) | **SC-001** | `MarketTransactionRepository.java` | T05, T08 |
| **FR-004** (Interpretación BUY/SELL) | **SC-001** | `OperationHistoryServiceImpl.java`, `OperationHistoryEntry.java` | T01, T06 |
| **FR-005** (Inmutabilidad financiera) | **SC-001** | `OperationHistoryEntry.java`, `OperationHistoryMapper.java` | T01, T03 |
| **FR-006** (Perfil actual del jugador) | **SC-001** | `MarketTransactionRepository.java`, `OperationHistoryResponseDTO.java` | T02, T05 |
| **FR-007** (Paginación server-side) | **SC-002** | `OperationHistoryCriteria.java`, `MarketTransactionRepository.java` | T01, T05 |
| **FR-008** (Filtros opcionales) | **SC-002** | `OperationHistoryCriteria.java`, `MarketTransactionRepository.java` | T01, T05 |
| **FR-009** (Orden cerrado determinista) | **SC-002** | `OperationHistoryCriteria.java`, `MarketTransactionRepository.java` | T01, T05 |
| **FR-010** (Agregados de summary) | **SC-001** | `OperationHistoryAggregate.java`, `MarketTransactionRepository.java` | T01, T05 |
| **FR-011** (Colección vacía) | **SC-004** | `OperationHistoryServiceImpl.java`, `OperationHistoryMapper.java` | T03, T06 |
| **FR-012** (Respuestas HTTP 400) | **SC-002** | `PortfolioExceptionHandler.java`, `OperationHistoryController.java` | T04, T07 |
| **FR-013** (Seguridad HTTP 401) | **SC-003** | `SecurityConfig.java`, `OperationHistoryController.java` | T07, T10 |
| **FR-014** (Transacción de solo lectura) | **SC-001** | `OperationHistoryServiceImpl.java` | T06, T09 |
| **FR-015** (Consulta sin N+1) | **SC-001** | `MarketTransactionRepository.java` | T05, T08 |

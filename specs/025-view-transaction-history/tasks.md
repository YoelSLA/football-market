# Tareas: Historial personal de transacciones (SPEC-025)

**Estado**: Implementación completada; pendiente de verificación final.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md), [contracts/transaction-history.md](contracts/transaction-history.md) y [`docs/wireframes/transaction-history.md`](../../docs/wireframes/transaction-history.md).

---

## Tareas de la funcionalidad vigente

### Fase 1 — Extensión del backend (Persistencia, Servicio y API)

- [x] **T01** Crear los records backend de criterios, agregados, vista paginada y perfil actual en `backend/src/main/java/footballmarket/features/portfolio/domain/records/` (`OperationHistoryCriteria`, `OperationHistoryAggregate`, `OperationHistoryView`, `OperationHistoryEntry`).
- [x] **T02** Implementar en `MarketTransactionRepository` (`backend/src/main/java/footballmarket/features/market/infraestructure/MarketTransactionRepository.java`) la consulta de contenido paginado con fetch joins to-one (`buyer`, `seller`, `player`, `team`, `league`), consulta de conteo y consulta de agregados SQL sobre el conjunto filtrado.
- [x] **T03** Implementar en `OperationHistoryService` y su implementación (`backend/src/main/java/footballmarket/features/portfolio/application/`) la validación de parámetros, orden determinista (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`) y cálculo de agregados bajo transacción `readOnly`.
- [x] **T04** Actualizar el DTO wrapper `OperationHistoryResponseDTO`, el mapeador `OperationHistoryMapper`, el controlador `OperationHistoryController` con OpenAPI y el manejador de excepciones `PortfolioExceptionHandler` para `INVALID_DATE_RANGE`.
- [x] **T05** Crear la cobertura de pruebas de backend en `OperationHistoryPersistenceTest`, `OperationHistoryServiceTest` y `OperationHistoryControllerTest` en `backend/src/test/java/footballmarket/features/portfolio/`.

### Fase 2 — Frontera de datos del frontend (Tipos, Servicio y Hooks)

- [x] **T06** Actualizar interfaces DTO en `frontend/src/features/portfolio/types/dtos.ts`, modelos de dominio en `models.ts`, mapeo en `portfolio.mapper.ts`, llamada tipada en `portfolio.service.ts` y hook de consulta TanStack Query en `hooks/queries/useTransactionHistoryQuery.ts`.
- [x] **T07** Implementar el hook de coordinación de página `useTransactionHistoryPage.ts` en `frontend/src/features/portfolio/hooks/pages/`, utilizando los search params de la URL como única fuente de verdad y restableciendo la página a cero al modificar filtros u orden.

### Fase 3 — Componentes y presentación visual (UI)

- [x] **T08** Implementar la jerarquía de componentes en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/`: encabezado (`TransactionHistoryHeader`), cuatro indicadores de actividad (`TransactionHistoryMetrics`), barra de filtros (`TransactionHistoryFilters`), tabla de contenido (`TransactionHistoryContent`), filas individuales con retrato actual (`TransactionHistoryEntry`), paginación compartida y estados compactos (`TransactionHistoryState`, `TransactionHistoryEmptyState`).
- [x] **T09** Maquetar los estilos SCSS desktop en `TransactionHistoryPage.module.scss` y sus módulos correspondientes en `components/`, respetando la retícula de 8 px, tokens de color sólido y garantizando la ausencia de scroll horizontal en resolución de 1920×913 px.

### Fase 4 — Verificación y entrega

- [ ] **T10** Ejecutar controles ejecutables permitidos (`npm run biome:check`, `npm run build`, `./gradlew build -x test`), realizar la revisión visual humana en el navegador y registrar la ejecución de los tests backend por parte del usuario.
- [ ] **T11** Ajustar identidad visual con escudos compartidos por ID, altura flexible y botones secundarios; verificar los cuatro escenarios desktop autorizados.
- [x] **T12** Centralizar la concordancia singular/plural de operaciones, tokens y créditos en los formatters de Cartera y aplicarla al historial.

---

## Matriz de trazabilidad

| Requisito | Criterio de éxito | Componentes afectados | Tarea |
| --- | --- | --- | --- |
| **FR-001** (Ruta `/portfolio/transactions`) | **SC-004** | `AppRouter.tsx`, `sections.ts`, `TransactionHistoryPage.tsx` | T08, T09 |
| **FR-002** (Identidad exclusiva JWT) | **SC-001** | `OperationHistoryController.java`, `OperationHistoryServiceImpl.java` | T03, T04 |
| **FR-003** (Wrapper summary/transactions/pagination) | **SC-001** | `OperationHistoryResponseDTO.java`, `portfolio.mapper.ts` | T01, T04, T06 |
| **FR-004** (Campos obligatorios de operación) | **SC-001** | `TransactionDTO`, `TransactionHistoryEntry.tsx` | T01, T04, T08 |
| **FR-005** (Perfil actual de jugador y clubes) | **SC-001** | `MarketTransactionRepository.java`, `TransactionHistoryEntry.tsx` | T02, T08 |
| **FR-006** (Documentación de perfil no histórico) | **SC-001** | `contracts/transaction-history.md`, `spec.md` | T04, T06 |
| **FR-007** (Inmutabilidad financiera) | **SC-001** | `MarketTransactionRepository.java`, `TransactionHistoryEntry.tsx` | T02, T08 |
| **FR-008** (Imágenes nullable y fallback) | **SC-001** | `PlayerPortrait.tsx`, `TransactionHistoryEntry.tsx` | T06, T08 |
| **FR-009** (Filtros opcionales) | **SC-002** | `OperationHistoryCriteria.java`, `TransactionHistoryFilters.tsx` | T01, T02, T08 |
| **FR-010** (Orden cerrado con desempates) | **SC-002** | `OperationHistoryCriteria.java`, `MarketTransactionRepository.java` | T01, T02 |
| **FR-011** (Desempates deterministas) | **SC-003** | `MarketTransactionRepository.java` | T02 |
| **FR-012** (Paginación server-side) | **SC-003** | `MarketTransactionRepository.java`, `Pagination.tsx` | T02, T08 |
| **FR-013** (Summary sobre conjunto filtrado) | **SC-002** | `OperationHistoryAggregate.java`, `TransactionHistoryMetrics.tsx` | T01, T02, T08 |
| **FR-014** (Volúmenes sin rotulación de P&L) | **SC-002** | `TransactionHistoryMetrics.tsx` | T08 |
| **FR-015** (Predicados idénticos en consultas) | **SC-002** | `MarketTransactionRepository.java` | T02 |
| **FR-016** (Consulta sin N+1) | **SC-001** | `MarketTransactionRepository.java` | T02 |
| **FR-017** (Códigos de error HTTP 400/401/500) | **SC-005** | `PortfolioExceptionHandler.java`, `OperationHistoryController.java` | T04 |
| **FR-018** (URL search params como fuente) | **SC-002** | `useTransactionHistoryPage.ts`, `useTransactionHistoryQuery.ts` | T07 |
| **FR-019** (Perfil sin navegación a ficha) | **SC-001** | `TransactionHistoryEntry.tsx` | T08 |
| **FR-020** (Reutilización de componentes y tokens) | **SC-004** | `PlayerPortrait.tsx`, `Pagination.tsx`, `Button.tsx` | T08, T09 |
| **FR-021** (Alcance exclusivamente desktop) | **SC-004** | `TransactionHistoryPage.module.scss` | T09 |
| **FR-022** (Retícula de 8 px y espacios locales) | **SC-004** | Módulos SCSS de componentes | T09 |
| **FR-023** (Estados compactos de altura natural) | **SC-005** | `TransactionHistoryState.tsx`, `TransactionHistoryEmptyState.tsx` | T08, T09 |
| **FR-024** (Acciones de vacío y sin resultados) | **SC-005** | `TransactionHistoryEmptyState.tsx`, `TransactionHistoryState.tsx` | T08 |
| **FR-025** (Sin migraciones ni cambios contables) | **SC-001** | `MarketTransactionRepository.java` | T02 |

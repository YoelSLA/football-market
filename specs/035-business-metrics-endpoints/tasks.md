---

description: "Tareas para implementar los endpoints de métricas de negocio"
---

# Tasks: Endpoints de métricas de negocio

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/http.md` y `quickstart.md` de esta carpeta.

**Prerequisites**: `plan.md` y `spec.md`.

**Tests**: Crear las pruebas backend indicadas; su ejecución queda a cargo del usuario. El agente no ejecuta tests.

**Organización**: Tareas agrupadas por User Story para mantener cobertura trazable e incrementos verificables.

## Fase 1: Setup

No se requieren tareas de inicialización: se reutilizan el backend, la autenticación, PostgreSQL y Micrometer existentes.

## Fase 2: Fundacional

No se requieren prerrequisitos compartidos adicionales; cada User Story puede implementarse sobre la arquitectura y los componentes existentes.

---

## Fase 3: User Story 1 - Consultar totales históricos de negocio (Priority: P1) 🎯 MVP

**Goal**: Exponer los agregados históricos de compras, ventas, publicaciones canceladas, tokens y créditos a partir de las filas persistidas.

**Independent Test**: Con operaciones financieras y publicaciones persistidas, verificar los cinco agregados y `queriedAt`; cubrir datos vacíos, clasificación sin doble conteo, vencimientos excluidos y centésimas de crédito.

### Tests para User Story 1

- [X] T001 [P] [US1] Añadir pruebas de agregación histórica para filas de compra, recompra oficial, liquidación P2P, cancelaciones, vencimientos y ausencia de actividad en `backend/src/test/java/footballmarket/features/market/infraestructure/BusinessMetricsRepositoryTest.java`.
- [X] T002 [P] [US1] Añadir pruebas de integración del servicio histórico para agregados, unidad de créditos, cero actividad e instante UTC en `backend/src/test/java/footballmarket/features/businessmetrics/application/BusinessMetricsServiceTest.java`. El usuario confirmó que la clase pasó.

### Implementación para User Story 1

- [X] T003 [US1] Implementar consultas agregadas históricas de transacciones financieras y publicaciones CANCELLED en `backend/src/main/java/footballmarket/features/market/infraestructure/MarketTransactionRepository.java` y `backend/src/main/java/footballmarket/features/market/infraestructure/TokenListingRepository.java`.
- [X] T004 [US1] Crear el servicio histórico de métricas, el DTO de respuesta común y la ruta `GET /api/business-metrics/historical` en `backend/src/main/java/footballmarket/features/businessmetrics/application/BusinessMetricsService.java`, `backend/src/main/java/footballmarket/features/businessmetrics/application/impl/BusinessMetricsServiceImpl.java`, `backend/src/main/java/footballmarket/features/businessmetrics/presentation/dtos/response/BusinessMetricsResponseDTO.java` y `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java`.

---

## Fase 4: User Story 2 - Consultar métricas de la ejecución actual (Priority: P1)

**Goal**: Consultar agregados Micrometer de las operaciones completadas desde el arranque actual, sin persistirlos ni contar solicitudes HTTP.

**Independent Test**: Verificar incrementos por operación confirmada, ausencia de incrementos ante fallos/rechazos y reinicio de contadores en un nuevo proceso.

### Tests para User Story 2

- [X] T005 [P] [US2] Añadir pruebas de los contadores Micrometer sin tags de identidad, con incrementos por operación, volúmenes agregados y clasificación de compras/ventas definida en la spec en `backend/src/test/java/footballmarket/features/businessmetrics/application/BusinessMetricsServiceTest.java`. El usuario confirmó que la clase pasó.
- [X] T006 [P] [US2] Añadir o ampliar pruebas de servicio para comprobar que las métricas solo aumentan tras completar compras, ventas/recompras y cancelaciones en `backend/src/test/java/footballmarket/features/market/application/BuyOrderServiceTest.java`, `backend/src/test/java/footballmarket/features/market/application/OfficialBuybackServiceTest.java` y `backend/src/test/java/footballmarket/features/market/application/TokenListingServiceTest.java`. El usuario confirmó que las tres clases pasaron.

### Implementación para User Story 2

- [X] T007 [US2] Implementar contadores Micrometer de cardinalidad fija y la lectura `GET /api/business-metrics/current` mediante el servicio y controller de `backend/src/main/java/footballmarket/features/businessmetrics/application/impl/BusinessMetricsServiceImpl.java` y `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java`.
- [X] T008 [US2] Instrumentar cada operación completada, una vez confirmado su resultado de negocio, desde `BuyOrderServiceImpl`, `OfficialBuybackServiceImpl` y `TokenListingServiceImpl`, contabilizando los eventos solo tras el commit de mercado.

---

## Fase 5: User Story 3 - Consultar métricas con acceso y exposición acotados (Priority: P1)

**Goal**: Asegurar que ambos endpoints exijan autenticación, compartan exactamente el contrato definido y no expongan identidades ni dimensiones de alta cardinalidad.

**Independent Test**: Comprobar ambos contratos HTTP completos y equivalentes, respuesta 401 sin autenticación, acceso de cualquier cuenta autenticada y ausencia de identificadores personales en respuestas y métricas.

### Tests para User Story 3

- [X] T009 [P] [US3] Añadir pruebas de Controller para ambas rutas, contrato JSON exacto, OpenAPI y rechazo de solicitudes no autenticadas en `backend/src/test/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsControllerTest.java`. Ejecución pendiente del usuario.

### Implementación para User Story 3

- [X] T010 [US3] Documentar en las anotaciones OpenAPI de `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java` las rutas, autenticación, respuesta compartida, unidades y significado de campos según `contracts/http.md`.

---

## Fase final: Pulido y validación transversal

No se requieren tareas adicionales fuera de las User Stories; contratos, documentación OpenAPI y validación están cubiertos en sus fases.

## Dependencias y orden de ejecución

### Dependencias entre fases

- Setup y Fundacional no introducen trabajo: se reutiliza infraestructura existente.
- US1, US2 y US3 son P1 y técnicamente independientes tras disponer del servicio, DTO y rutas compartidas; las modificaciones coincidentes en controller y servicio deben integrarse coordinadamente.
- El pulido transversal depende de que estén implementadas y documentadas las dos consultas.

### Orden técnico dentro de cada User Story

- US1: pruebas de agregación y servicio → consultas de repositorio → servicio, DTO y endpoint histórico.
- US2: pruebas → contadores y endpoint actual → instrumentación en los puntos de finalización de operaciones.
- US3: pruebas HTTP y seguridad → anotaciones OpenAPI del contrato.

### Oportunidades paralelas

- T001 y T002 pueden prepararse en paralelo; afectan archivos distintos.
- T005 y T006 pueden prepararse en paralelo; afectan pruebas de componentes distintos.

## Estrategia de implementación

### MVP

La US1 es el MVP: entrega la consulta histórica y su contrato común. La US2 añade métricas de la ejecución actual; la US3 verifica los requisitos transversales de seguridad y exposición para ambas rutas.

### Entrega incremental

1. Completar la consulta histórica (US1).
2. Añadir contadores de ejecución e instrumentación por operación completada (US2).
3. Verificar contrato, autenticación, ausencia de identificadores y documentación OpenAPI (US3).
4. Ejecutar las pruebas backend y las comprobaciones manuales a cargo del usuario. No ejecutar tests desde el agente.

Las tareas describen trabajo técnico y no autorizan IMPLEMENT, revisión, testing ni cierre.

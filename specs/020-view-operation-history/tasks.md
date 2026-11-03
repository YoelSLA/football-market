---

description: "Tareas de implementación del historial personal de operaciones"
---

# Tasks: Historial personal de operaciones (SPEC-020)

**Input**: Documentos de diseño de `specs/020-view-operation-history/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/operation-history-api.md`, `quickstart.md`

**Tests**: La implementación backend requiere tests de Repository, Service y Controller. Su ejecución y evidencia corresponden al usuario; el agente no debe ejecutar tests ni hooks que los disparen.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Estas Tasks viven exclusivamente en este archivo, no se convierten en Issues ni duplican su avance en el seguimiento operativo. Completar TASKS no autoriza IMPLEMENT ni acredita revisión, testing o cierre.

**Organization**: Las tareas se agrupan por User Story para permitir implementación y verificación incremental. US1 entrega el flujo principal; US2 completa su garantía inseparable de privacidad; US3 cubre explícitamente el resultado vacío.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede realizarse en paralelo porque trabaja en archivos distintos y no depende de una tarea incompleta.
- **[Story]**: User Story de `spec.md` cubierta por la tarea.
- Todas las tareas indican rutas exactas.

## Phase 1: Setup (Shared Test Infrastructure)

**Purpose**: Preparar fixtures reutilizables para los tests de persistencia y Service sin alterar infraestructura ni dependencias.

- [X] T001 Crear el helper de Arrange para usuarios, jugadores y transacciones confirmadas de compra y venta en `backend/src/test/java/footballmarket/support/OperationHistoryTestData.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Definir los tipos compartidos por el caso de uso y su frontera HTTP.

**⚠️ CRITICAL**: Estas tareas bloquean las fases de User Stories.

- [X] T002 [P] Crear el record de vista con los ocho datos contractuales y el tipo `BUY`/`SELL` en `backend/src/main/java/footballmarket/models/records/OperationHistoryEntry.java`
- [X] T003 [P] Crear el Response DTO documentado con OpenAPI, unidades financieras y fecha con offset en `backend/src/main/java/footballmarket/controllers/dtos/responses/OperationHistoryResponseDTO.java`
- [X] T004 Implementar la traducción sin lógica desde la vista de Model al Response DTO en `backend/src/main/java/footballmarket/controllers/mappers/OperationHistoryMapper.java`

**Checkpoint**: La vista interna y el contrato de salida están disponibles sin agregar persistencia ni dependencias.

---

## Phase 3: User Story 1 - Consultar el historial personal (Priority: P1) 🎯 MVP técnico

**Goal**: Devolver todas las compras y ventas confirmadas del usuario como entradas únicas, con valores históricos y orden descendente estable.

**Independent Test**: Con un usuario que compró y vendió tokens, la API devuelve ambas operaciones una sola vez, con los ocho campos contractuales, `BUY`/`SELL` según su rol y orden `executedAt DESC, transactionId DESC`.

### Tests para User Story 1

> Escribir los tests; su ejecución queda a cargo del usuario.

- [X] T005 [P] [US1] Crear el test de Repository para compras, ventas, exclusión de operaciones ajenas, ausencia de duplicados y orden descendente estable en `backend/src/test/java/footballmarket/repositories/OperationHistoryPersistenceTest.java`
- [X] T006 [P] [US1] Crear el test de Service con PostgreSQL real para resolver la identidad, derivar `BUY`/`SELL` y preservar cantidad, precios, importe e instante históricos en `backend/src/test/java/footballmarket/services/OperationHistoryServiceTest.java`
- [X] T007 [P] [US1] Crear el test MVC del contrato 200 con array y ocho campos exactos, serialización ISO-8601 y propagación del subject JWT en `backend/src/test/java/footballmarket/controllers/OperationHistoryControllerTest.java`

### Implementation for User Story 1

- [X] T008 [US1] Agregar la consulta dedicada con fetch de participantes y jugador, filtro comprador-o-vendedor y orden descendente estable sin modificar `findFinancialHistory` en `backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java`
- [X] T009 [P] [US1] Definir el contrato público de consulta por subject autenticado en `backend/src/main/java/footballmarket/services/OperationHistoryService.java`
- [X] T010 [US1] Implementar el caso de uso `readOnly`, resolución del usuario persistido, interpretación del rol y copia de valores históricos en `backend/src/main/java/footballmarket/services/impl/OperationHistoryServiceImpl.java`
- [X] T011 [US1] Exponer `GET /api/users/me/transactions` con principal JWT, Mapper y documentación OpenAPI coherente con el contrato en `backend/src/main/java/footballmarket/controllers/OperationHistoryController.java`

**Checkpoint**: US1 queda implementada y lista para que el usuario ejecute sus tests; no se modifican compras, ventas ni registros financieros.

---

## Phase 4: User Story 2 - Proteger la privacidad del historial (Priority: P1)

**Goal**: Garantizar que la identidad proceda exclusivamente de la sesión y que una solicitud no pueda seleccionar ni observar el historial de otra persona.

**Independent Test**: Con sesiones de dos usuarios, cada JWT obtiene solo las operaciones de su subject; sin JWT o con JWT inválido se responde `401`, no se invoca el Service y no existe variante de la ruta con identificador de usuario.

### Tests para User Story 2

- [X] T012 [US2] Ampliar los casos MVC para sesiones distintas, ausencia o invalidez del JWT, subject válido sin usuario persistido con respuesta `401` sin datos sensibles, no interacción con el Service cuando la autenticación falla y rechazo de rutas que intenten seleccionar otro usuario en `backend/src/test/java/footballmarket/controllers/OperationHistoryControllerTest.java`
- [X] T013 [US2] Ampliar los casos de Service para excluir transacciones ajenas y rechazar un subject válido sin usuario persistido sin devolver datos parciales en `backend/src/test/java/footballmarket/services/OperationHistoryServiceTest.java`

### Implementation for User Story 2

- [X] T014 [US2] Completar la frontera HTTP para que solo transporte el subject autenticado y no acepte path, query ni body de selección de identidad en `backend/src/main/java/footballmarket/controllers/OperationHistoryController.java`
- [X] T015 [US2] Completar la resolución segura de identidad y la exclusión de operaciones ajenas conservando la transacción de solo lectura en `backend/src/main/java/footballmarket/services/impl/OperationHistoryServiceImpl.java`

**Checkpoint**: US2 demuestra aislamiento por sesión y rechazo previo de solicitudes no autenticadas sin exponer información financiera.

---

## Phase 5: User Story 3 - Consultar un historial vacío (Priority: P2)

**Goal**: Responder exitosamente con una colección vacía para un usuario autenticado sin operaciones.

**Independent Test**: Un usuario persistido que no sea comprador ni vendedor en ninguna transacción recibe `200 OK` con `[]`.

### Tests para User Story 3

- [X] T016 [P] [US3] Agregar el escenario de Service que devuelve una lista vacía para un usuario persistido sin operaciones en `backend/src/test/java/footballmarket/services/OperationHistoryServiceTest.java`
- [X] T017 [P] [US3] Agregar el escenario MVC que serializa la respuesta vacía como `200 OK` con `[]` en `backend/src/test/java/footballmarket/controllers/OperationHistoryControllerTest.java`

### Implementation for User Story 3

- [X] T018 [US3] Garantizar que el caso de uso preserve y devuelva la colección vacía sin traducirla a `null` ni error en `backend/src/main/java/footballmarket/services/impl/OperationHistoryServiceImpl.java`

**Checkpoint**: Las tres User Stories quedan implementadas y verificables con las suites indicadas por `quickstart.md`.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cerrar cobertura transversal sin ampliar el alcance funcional.

- [X] T019 Reforzar la regresión de consulta comprobando que lecturas repetidas sobre el mismo estado devuelven la misma colección ordenada y sin filas duplicadas en `backend/src/test/java/footballmarket/repositories/OperationHistoryPersistenceTest.java`
- [X] T020 Revisar y sincronizar ejemplos, campos, errores y restricciones de identidad del contrato implementado en `specs/020-view-operation-history/contracts/operation-history-api.md`
- [X] T021 Actualizar los escenarios y comandos de validación a cargo del usuario si la implementación final cambia nombres de suites o condiciones verificables en `specs/020-view-operation-history/quickstart.md`
- [X] T022 [US1] Cubrir la respuesta contractual `500 INTERNAL_ERROR` con su `ErrorResponseDTO` completo en `backend/src/test/java/footballmarket/controllers/OperationHistoryControllerTest.java`
- [X] T023 Delimitar compradores y vendedores distintos como garantía externa de SPEC-010 y SPEC-011, eliminando FR-016 del alcance implementable de SPEC-020 en `specs/020-view-operation-history/spec.md`, `specs/020-view-operation-history/data-model.md`, `specs/020-view-operation-history/research.md` y `specs/020-view-operation-history/checklists/api-security.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no depende de otras tareas.
- **Foundational (Phase 2)**: puede avanzar después de T001; T002 y T003 son paralelizables, y T004 depende de ambas.
- **US1 (Phase 3)**: depende de Foundation; sus tests se escriben antes de la implementación correspondiente. T010 depende de T002, T008 y T009; T011 depende de T003, T004, T009 y T010.
- **US2 (Phase 4)**: depende del endpoint y el caso de uso de US1 porque endurece esas mismas fronteras.
- **US3 (Phase 5)**: depende del caso de uso y endpoint de US1, pero su comportamiento vacío no depende de completar los escenarios adicionales de US2.
- **Polish (Phase 6)**: depende de las User Stories incluidas en el alcance autorizado.

### User Story Dependencies

```text
Setup → Foundational → US1 ──→ US2
                          └──→ US3
US2 + US3 → Polish
```

- **US1 (P1)**: incremento funcional base; no depende de otras historias.
- **US2 (P1)**: dependencia técnica de US1 por compartir endpoint y Service; es inseparable para una entrega segura del recurso personal.
- **US3 (P2)**: reutiliza US1 y puede completarse en paralelo con US2 una vez disponible el flujo base.

### Within Each User Story

- Escribir primero los tests backend que expresan el comportamiento; el usuario los ejecuta.
- Definir tipos y contratos antes de los consumidores.
- Implementar Repository antes de Service y Service antes de Controller.
- No modificar `MarketTransaction`, el esquema, índices, compras, ventas ni publicaciones.
- Una Task completada acredita trabajo implementado, no resultados de tests ni autorizaciones humanas.

### Parallel Opportunities

- T002 y T003 trabajan en archivos distintos y pueden desarrollarse en paralelo.
- T005, T006 y T007 pueden escribirse en paralelo después de Foundation y T001.
- T009 puede avanzar en paralelo con T008.
- Después de US1, US2 y US3 pueden avanzar en paralelo, evitando solapar ediciones de los mismos archivos.
- T020 y T021 son documentales y pueden realizarse en paralelo después de estabilizar el contrato implementado.

---

## Parallel Example: User Story 1

```text
Task T005: crear OperationHistoryPersistenceTest.java
Task T006: crear OperationHistoryServiceTest.java
Task T007: crear OperationHistoryControllerTest.java

Task T008: ampliar MarketTransactionRepository.java
Task T009: crear OperationHistoryService.java
```

## Parallel Example: User Stories 2 and 3

```text
Developer A: T012–T015 para privacidad y autenticación de US2
Developer B: T016–T018 para respuesta vacía de US3
```

Las ediciones compartidas de `OperationHistoryServiceTest.java`, `OperationHistoryControllerTest.java` y `OperationHistoryServiceImpl.java` deben coordinarse o realizarse secuencialmente para evitar conflictos.

---

## Implementation Strategy

### MVP First

1. Completar Setup y Foundational.
2. Implementar US1 en orden T005–T011.
3. Completar US2 antes de exponer o entregar el recurso, porque su privacidad es P1 e inseparable del flujo principal.
4. Detenerse para CODE REVIEW y para que el usuario ejecute los tests aplicables; no avanzar gates por inferencia.

**MVP sugerido**: US1 + US2. Aunque US1 contiene el valor funcional principal, US2 es una garantía de seguridad P1 necesaria para entregar el endpoint personal.

### Incremental Delivery

1. Setup + Foundational → tipos y fixtures listos.
2. US1 → consulta funcional completa.
3. US2 → aislamiento de sesión y privacidad completos.
4. US3 → experiencia correcta para usuarios sin actividad.
5. Polish → regresión de solo lectura y coherencia documental.

### Verification Handoff

- Tras implementar, el agente puede ejecutar únicamente `./gradlew build -x test` desde `backend/` si el alcance autorizado lo permite.
- El usuario ejecuta `./gradlew lightweightTest`, `./gradlew integrationTest` y `./gradlew test` según `quickstart.md`.
- Generar Tasks conduce a `/speckit.analyze`; no autoriza ni inicia IMPLEMENT.

---

## Notes

- `[P]` significa archivos distintos y ausencia de dependencias incompletas; no autoriza delegación automática.
- No se crean tests E2E ni tests directos del Mapper porque la cobertura prevista corresponde a Repository, Service y Controller.
- No se agregan migraciones, dependencias, paginación, filtros, estadísticas, frontend ni fuentes financieras paralelas.
- Preservar los IDs y el avance de estas Tasks en futuras reanudaciones.

---

description: "Lista de tareas para la feature: Definir reglas contables de costo promedio y resultados financieros (SPEC-015)"
---

# Tasks: Definir reglas contables de costo promedio y resultados financieros (SPEC-015)

**Input**: Documentos de diseño desde `/specs/015-define-average-cost-and-financial-results-rules/`

**Prerequisites**: plan.md (requerido), spec.md (requerido para las historias), research.md, data-model.md, contracts/

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend. Los ejemplos genéricos siguientes se adaptan a estas reglas.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Organization**: Tasks agrupadas por historia de usuario para permitir implementar y probar cada una de forma independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias)
- **[Story]**: historia de usuario a la que pertenece la tarea (US1, US2, US3)
- Las rutas son exactas y corresponden a la estructura del backend definida en `plan.md`

**Alcance**: esta feature **no expone endpoint**, no crea DTO ni Mapper, no introduce dependencias y no crea tablas de negocio. La única migración es un índice. La documentación arquitectónica de RA-1 se actualiza en la fase final.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificaciones preparatorias y decisiones que condicionan a todas las historias. No son entregables de código.

- [ ] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas: la aritmética exacta usa `java.math.BigDecimal` de la plataforma y no se incorpora ninguna librería; confirmar que no se añade nada al build (R-03, R-11, Constitución §I)
- [ ] T002 [P] Verificar la siguiente versión de migración disponible con `ls backend/src/main/resources/db/migration/` y confirmar que la nueva usará timestamp UTC de 14 dígitos según `docs/development/flyway-migrations.md` (R-14)
- [ ] T003 [P] Revisar `git status --short` y preservar los cambios locales ajenos antes de escribir (preservación de trabajo previo, AGENTS.md raíz)
- [ ] T004 [P] Verificar que el puerto `QuoteSource` de `backend/src/main/java/footballmarket/integrations/QuoteSource.java` y el record `CurrentQuote` de `backend/src/main/java/footballmarket/models/records/CurrentQuote.java` se consumen sin modificación, conforme a R-07 y CL-05
- [ ] T005 [P] Verificar los dos sitios que registran `executed_at` en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` —liquidación de publicación con `marketClock` y compra directa con el reloj de sistema— y el bean `marketClock` de `backend/src/main/java/footballmarket/config/MarketTimeConfiguration.java`, antes de cualquier cambio (FR-019, R-05)
- [ ] T006 [P] Revisar `docs/backend/testing.md` §2, §16 y la sección de clasificación por etiquetas, y verificar la etiqueta `@Tag` de las clases de test del dominio de mercado existentes, para replicar el patrón (R-13, `docs/backend/testing.md`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Piezas que TODAS las historias necesitan. Ninguna historia puede comenzar antes de que esta fase esté completa.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Crear la migración del índice con `./gradlew createMigration -PmigrationDescription=index_player_token_emissions_owner` desde `backend/` y escribir en el archivo generado el `CREATE INDEX` sobre `player_token_emissions (owner_user_id)`, sin tocar ninguna tabla ni columna de negocio (FR-021, R-08, R-14, `data-model.md` §1.2)
- [ ] T008 [P] Añadir en `backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java` la consulta de conjunto que devuelve las operaciones del usuario como comprador **o** vendedor, ordenadas por `(executed_at, id)` ascendente, sin modificar el método existente (FR-002, FR-018, R-08)
- [ ] T009 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerTokenEmissionRepository.java` la consulta de las emisiones cuyo propietario es el usuario, sin modificar los métodos existentes de aprovisionamiento (FR-021, R-08)
- [ ] T010 [P] Crear `FinancialPositionIntegrityException` en `backend/src/main/java/footballmarket/models/exceptions/FinancialPositionIntegrityException.java`, heredando de `DomainException` conforme a `docs/backend/architecture.md` §1.6, con las guardas de integridad de R-16 y R-09 (FR-025)
- [ ] T011 Unificar la fuente del reloj en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java`: registrar el `executed_at` de la compra directa con el reloj inyectado `marketClock`, conservando el significado del campo —instante de confirmación de la operación— y sin alterar el contrato público de SPEC-010 (R-05, FR-019, FR-030, CL-06, CL-12)
- [ ] T012 [P] Escribir el test de regresión de la unificación del reloj en `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java`, con grupo `@Nested`, declarando el mock del reloj de mercado —`@MockitoBean Clock marketClock`, que el archivo actual no declara— y afirme que el `executed_at` de la compra directa procede del reloj inyectado y que el de la liquidación conserva su instante validado (FR-019, AC19, R-05, R-13)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Conocer el costo de adquisición y el resultado realizado de mi posición (Priority: P1) 🎯 MVP

**Goal**: La regla contable que calcula el costo de adquisición autoritativo, el precio medio derivado y el resultado realizado acumulado de una posición, con la atribución proporcional del costo de una venta, la conservación exacta del costo y el tratamiento de la emisión inicial.

**Independent Test**: Con operaciones confirmadas de un usuario, el cálculo de la posición entrega el costo de adquisición, el precio medio, el resultado realizado y los hechos del ciclo esperados, con conservación exacta del costo pagado. No requiere cotizaciones, ni portfolio, ni operar en el mercado.

### Tests para User Story 1 (backend; sin tests frontend)

- [ ] T013 [P] [US1] Escribir tests de Model en `backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java` con grupos `@Nested` que cubran la acumulación de compras, el promedio ponderado móvil, la atribución proporcional de una venta parcial, el redondeo al medio hacia arriba, la conservación exacta del costo, el residuo retenido en la posición abierta, la liquidación total a costo cero, la reapertura con costo nuevo y el resultado realizado acumulado y negativo (FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-015, FR-016, FR-017, AC01, AC02, AC03, AC04, AC05, AC15, AC20, AC21, AC22, SC-001, SC-002, SC-003, SC-006, SC-009, CL-01, CL-02, CL-03, CL-04, R-13, R-16)
- [ ] T014 [P] [US1] Escribir tests de Model en `backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java` con grupos `@Nested` que cubran el plegado del historial ordenado por `(executed_at, id)`, la imputación por rol de comprador y vendedor, la aplicación de la emisión como cantidad de costo cero antes de las operaciones, la dilución del promedio único en la mezcla de orígenes y el desempate ante instantes iguales (FR-002, FR-003, FR-018, FR-021, FR-022, AC07, AC08, AC09, AC10, AC16, AC17, SC-010, CL-07, CL-13, CL-16, R-04, R-06, R-18)

### Implementation for User Story 1

- [ ] T015 [US1] Implementar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con el estado autoritativo —`openQuantity`, `acquisitionCost`, `totalPaid`, `realizedProfitLoss`— y las operaciones de dominio `purchase`, `sale` e `issued`, el precio medio derivado con redondeo al medio hacia arriba y las invariantes de no negatividad y conservación, sin `Builder` ni `@Setter` y con Javadoc en español (FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-015, FR-016, FR-017, CL-01, CL-02, CL-03, CL-11, R-03, R-11, R-16, `data-model.md` §2.1 y §3.1)
- [ ] T016 [US1] Ampliar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con los hechos del ciclo de tenencia —`openedAt`, `closedAt`, `lastTransactionAt`— y los recuentos por rol `buyOperationCount` y `sellOperationCount`, actualizados en el mismo recorrido del historial, con la emisión inicial abriendo el ciclo sin contar como compra ni como venta (FR-009, FR-010, FR-021, FR-022, AC04, AC05, AC09, AC10, SC-005, SC-006, CL-04, CL-07, CL-13, R-06, R-18, `data-model.md` §4.1)
- [ ] T017 [US1] Implementar `FinancialPositionCalculator` en `backend/src/main/java/footballmarket/models/FinancialPositionCalculator.java` como componente de Model puro y estático: ordenar las operaciones por `(executed_at, id)`, aplicarlas según el rol del usuario, aplicar las emisiones como cantidad de costo cero y devolver las posiciones ordenadas por `playerId` (FR-002, FR-003, FR-018, FR-027, AC07, AC08, AC16, AC17, SC-010, CL-06, CL-07, CL-16, R-04, R-06, R-17, R-18, `data-model.md` §2.2)

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Conocer el valor actual y el resultado no realizado de mi posición abierta (Priority: P2)

**Goal**: La valoración de la posición abierta con la cotización vigente, su resultado no realizado, el resultado total con su semántica de indefinición y la agregación contable del usuario con el porcentaje global.

**Independent Test**: Con una posición abierta valorada y otra sin cotización, el cálculo entrega el valor actual, el resultado no realizado y el total según la regla de indefinición, y la agregación sobre el conjunto completo devuelve el costo histórico, los resultados y el porcentaje global.

### Tests para User Story 2 (backend; sin tests frontend)

- [ ] T018 [US2] Ampliar los tests de Model creados en T013 en `backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java` con un grupo `@Nested` de valoración que cubra precio e instante de la misma cotización, ambos nulos sin cotización, valor actual, resultado no realizado, resultado total definido e indefinido, y el total de una posición cerrada igual al resultado realizado acumulado (FR-012, FR-013, FR-031, FR-032, AC11, AC12, AC14, AC15, SC-007, SC-008, CL-05, CL-14, R-18)
- [ ] T019 [US2] Ampliar los tests de Model creados en T014 en `backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java` con un grupo `@Nested` de agregación que cubra la distinción entre el costo histórico pagado y el costo de adquisición de las posiciones abiertas, la propagación de indefinidos cuando una posición abierta carece de cotización, el indicador de valoración completa y el porcentaje global con su denominador histórico y sus casos nulos (FR-032, CL-08, SC-008, R-19)

### Implementation for User Story 2

- [ ] T020 [US2] Ampliar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con el par de valoración `currentUnitPrice` y `quoteCalculatedAt`, tomado de la misma cotización y nulo en bloque cuando no hay cotización, y con las cifras derivadas `currentValue`, `unrealizedProfitLoss` y `totalProfitLoss` conforme a la regla de indefinición (FR-012, FR-013, FR-031, FR-032, AC11, AC12, AC14, AC15, SC-007, SC-008, CL-03, CL-05, CL-14, R-18, `data-model.md` §2.1)
- [ ] T021 [US2] Implementar en `backend/src/main/java/footballmarket/models/FinancialPositionCalculator.java` la agregación contable reutilizable del usuario sobre el **conjunto completo** de posiciones, que devuelva `historicalCost`, `investedCost`, `currentValue`, `realizedProfitLoss`, `unrealizedProfitLoss`, `totalProfitLoss`, `profitLossPercentage` y `valuationComplete`, con el denominador histórico del porcentaje global y la semántica de indefinidos (FR-032, CL-08, SC-008, R-19, `data-model.md` §3.5)

**Checkpoint**: User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Verificar que el resultado que me informan es trazable y reproducible (Priority: P3)

**Goal**: El caso de uso reutilizable que reconstruye la posición desde las operaciones confirmadas y la emisión inicial, con una lectura consistente, la valoración tomada de la fuente vigente y el acceso al contrato que consume SPEC-014.

**Independent Test**: Con un usuario que tiene operaciones confirmadas y emisión, el caso de uso devuelve sus posiciones valoradas desde una única instantánea, falla ante el fallo técnico de la fuente de cotizaciones y no altera ningún dato persistido.

### Tests para User Story 3 (backend; sin tests frontend)

- [ ] T022 [P] [US3] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/FinancialPositionPersistenceTest.java` con grupos `@Nested` que cubran que la consulta de operaciones devuelve las compras y las ventas del usuario sin repetir ninguna y en el orden contable, que la consulta de emisiones devuelve las del propietario y que el índice por propietario está disponible (FR-002, FR-018, AC16, AC17, SC-010, R-08, R-14)
- [ ] T023 [P] [US3] Escribir tests de Service en `backend/src/test/java/footballmarket/services/FinancialPositionServiceTest.java` que afirmen únicamente lo que devuelve o lanza la API pública del caso de uso, sin tocar persistencia, y que cubran las posiciones de un usuario con operaciones y emisión, la cotización presente y ausente, la propagación del fallo técnico sin datos financieros parciales, la ausencia de escrituras, el determinismo entre dos llamadas y el fallo ante una inconsistencia de integridad; que una publicación de venta activa y vigente no altere la cantidad abierta, el costo de adquisición ni el resultado de la posición (FR-011, AC06); que el cálculo exponga únicamente las posiciones del usuario solicitado (FR-024); que una posición cuyo jugador dejó de estar en el catálogo se conserve con sus resultados (FR-026); y que el resultado realizado equivalga exactamente al importe de la venta menos el costo atribuido, sin comisiones ni deducciones (FR-029) (FR-011, FR-014, FR-020, FR-023, FR-024, FR-025, FR-026, FR-029, AC06, AC12, AC13, AC16, AC18, AC20, SC-004, SC-005, SC-010, SC-011, SC-012, CL-09, CL-16, R-15, R-18)
- [ ] T024 [US3] Ampliar en `backend/src/test/java/footballmarket/services/FinancialPositionServiceTest.java` creado en T023 un grupo `@Nested` de coherencia que confirme que una operación de mercado confirmada durante el cálculo no altera la instantánea y que el resultado corresponde al estado previo, que es un estado válido (FR-020, AC18, R-13, R-15, AC34 de SPEC-014)

### Implementation for User Story 3

- [ ] T025 [US3] Implementar la interfaz `FinancialPositionService` en `backend/src/main/java/footballmarket/services/FinancialPositionService.java` con la operación `positionsOf(Long userId)` y el contrato documentado en `contracts/financial-position-service.md` §2, incluidos el límite transaccional, la ausencia de cotización, el fallo técnico y la ausencia de efectos (FR-001, FR-028, CL-10, CL-11, R-18, R-19)
- [ ] T026 [US3] Implementar `FinancialPositionServiceImpl` en `backend/src/main/java/footballmarket/services/impl/FinancialPositionServiceImpl.java` con la transacción de solo lectura y aislamiento `REPEATABLE READ`, las dos consultas de conjunto, la determinación previa del conjunto distinto de jugadores, la adquisición de las cotizaciones por el puerto conservando precio e instante, el plegado mediante el calculador y la traducción de los fallos técnicos y de integridad (FR-002, FR-012, FR-024, FR-027, FR-028, FR-029, AC12, AC13, SC-005, SC-011, SC-012, CL-05, CL-15, R-02, R-07, R-15, R-17, `contracts/financial-position-service.md` §2)
- [ ] T027 [US3] Verificar el contrato de consumo por SPEC-014 contra la implementación en `specs/015-define-average-cost-and-financial-results-rules/contracts/financial-position-service.md`: las nueve reglas de consumo de §3.2, la pareja precio-instante, los hechos del ciclo, la agregación sobre el conjunto completo y la exigencia de aislamiento del consumidor; registrar el resultado y corregir el contrato solo si aparece una discrepancia real, sin modificar SPEC-014 (FR-028, CL-11, R-12, R-18, R-19)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Afectan a varias historias y a la integración con el consumidor

- [ ] T028 [P] Actualizar `docs/backend/architecture.md`: documentar los componentes de SPEC-015 —`PlayerFinancialPosition`, `FinancialPositionCalculator`, `FinancialPositionService` y su implementación— y hacer explícita la relación Service → Service autorizada entre SPEC-015 y SPEC-014, siguiendo el precedente de §3 y §4 (RA-1, R-01)
- [ ] T029 [P] Verificar que la feature no introduce ruta HTTP, DTO ni Mapper en `backend/src/main/java/footballmarket/controllers/`, y que ningún contrato público de SPEC-010, SPEC-011 o SPEC-014 cambia; registrar el resultado (FR-030, R-10)
- [ ] T030 [P] Verificar que no se crea ningún artefacto en `frontend/src/` ni infraestructura de pruebas de frontend, conforme a la exención de testing del frontend en la Constitución (Constitución, sección de verificación y testing)
- [ ] T031 [P] Recorrer los escenarios de validación de `specs/015-define-average-cost-and-financial-results-rules/quickstart.md`, solicitar al usuario la ejecución de las suites y registrar su evidencia; el agente no ejecuta tests (R-13, SC-001 a SC-012)
- [ ] T032 [P] Ejecutar el control de build permitido para el backend, `./gradlew build -x test` desde `backend/`, y registrar el resultado; no acredita TESTING (control de build permitido, AGENTS.md del backend)
- [ ] T033 [P] Actualizar el estado de avance de las tareas en `specs/015-define-average-cost-and-financial-results-rules/tasks.md` conservando los IDs y las marcas existentes, y verificar que ningún estado se replica en issues ni tarjetas de seguimiento (`docs/development/sdd-workflow.md` §2)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - Las tareas de tests de cada historia pueden escribirse en paralelo tras completar la fase fundacional
  - La implementación sigue el orden P1 → P2 → P3: US2 consume el modelo de posición de US1, y US3 consume el modelo de US1 junto con la valoración de US2
- **Polish (Final Phase)**: Depends on all desired user stories being complete; T028 debe completarse antes de que SPEC-014 implemente su consumo del componente

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Se apoya en la posición de US1 pero es verificable de forma independiente
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Consume el modelo de US1 y US2; el plegado y la valoración son verificables de forma independiente

### Within Each User Story

- Escribir tests backend necesarios; su ejecución y evidencia corresponden al usuario
- Una tarea que amplía un archivo creado por otra tarea depende de ella: T018 depende de T013, T019 depende de T014 y T024 depende de T023; por eso las tres pierden la marca `[P]`
- Model before service
- Service before contract verification
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Escribir tests backend de US1; no ejecutarlos desde el agente:
Task: "Escribir tests de Model en backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java"
Task: "Escribir tests de Model en backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java"

# Ejecutar en paralelo las consultas de conjunto y la excepción de integridad:
Task: "Añadir en backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java la consulta de conjunto..."
Task: "Añadir en backend/src/main/java/footballmarket/repositories/PlayerTokenEmissionRepository.java la consulta de emisiones..."
Task: "Crear FinancialPositionIntegrityException en backend/src/main/java/footballmarket/models/exceptions/..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario
5. Demo si está listo

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit tras cada tarea o grupo lógico, solo cuando el responsable del proyecto lo autorice: el agente no realiza commits ni push sin autorización explícita
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Las tareas no incluyen la generación de issues ni de tarjetas: las Tasks viven en este archivo

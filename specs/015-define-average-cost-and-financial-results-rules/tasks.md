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

- [X] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas: la aritmética exacta usa `java.math.BigDecimal` de la plataforma y no se incorpora ninguna librería; confirmar que no se añade nada al build (R-03, R-11, Constitución §I)
- [X] T002 [P] Verificar la siguiente versión de migración disponible con `ls backend/src/main/resources/db/migration/` y confirmar que la nueva usará timestamp UTC de 14 dígitos según `docs/development/flyway-migrations.md` (R-14)
- [X] T003 [P] Revisar `git status --short` y preservar los cambios locales ajenos antes de escribir (preservación de trabajo previo, AGENTS.md raíz)
- [X] T004 [P] Verificar que el puerto `QuoteSource` de `backend/src/main/java/footballmarket/integrations/QuoteSource.java` y el record `CurrentQuote` de `backend/src/main/java/footballmarket/models/records/CurrentQuote.java` se consumen sin modificación, conforme a R-07 y CL-05
- [X] T005 [P] Verificar los dos sitios que registran `executed_at` en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` —liquidación de publicación con `marketClock` y compra directa con el reloj de sistema— y el bean `marketClock` de `backend/src/main/java/footballmarket/config/MarketTimeConfiguration.java`, antes de cualquier cambio (FR-019, R-05)
- [X] T006 [P] Revisar `docs/backend/testing.md` §2, §16 y la sección de clasificación por etiquetas, y verificar la etiqueta `@Tag` de las clases de test del dominio de mercado existentes, para replicar el patrón (R-13, `docs/backend/testing.md`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Piezas que TODAS las historias necesitan. Ninguna historia puede comenzar antes de que esta fase esté completa.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Crear la migración del índice con `./gradlew createMigration -PmigrationDescription=index_player_token_emissions_owner` desde `backend/` y escribir en el archivo generado el `CREATE INDEX` sobre `player_token_emissions (owner_user_id)`, sin tocar ninguna tabla ni columna de negocio (FR-021, R-08, R-14, `data-model.md` §1.2)
- [X] T008 [P] Añadir en `backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java` la consulta de conjunto que devuelve las operaciones del usuario como comprador **o** vendedor, ordenadas por `(executed_at, id)` ascendente, sin modificar el método existente (FR-002, FR-018, R-08)
- [X] T009 [P] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerTokenEmissionRepository.java` la consulta de las emisiones cuyo propietario es el usuario, sin modificar los métodos existentes de aprovisionamiento (FR-021, R-08)
- [X] T010 [P] Crear `FinancialPositionIntegrityException` en `backend/src/main/java/footballmarket/models/exceptions/FinancialPositionIntegrityException.java`, heredando de `DomainException` conforme a `docs/backend/architecture.md` §1.6, con las guardas de integridad de R-16 y R-09 (FR-025)
- [X] T011 Unificar la fuente del reloj en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java`: registrar el `executed_at` de la compra directa con el reloj inyectado `marketClock`, conservando el significado del campo —instante de confirmación de la operación— y sin alterar el contrato público de SPEC-010 (R-05, FR-019, FR-030, CL-06, CL-12)
- [X] T012 [P] Escribir el test de regresión de la unificación del reloj en `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java`, con grupo `@Nested`, declarando el mock del reloj de mercado —`@MockitoBean Clock marketClock`, que el archivo actual no declara— y afirme que el `executed_at` de la compra directa procede del reloj inyectado y que el de la liquidación conserva su instante validado (FR-019, AC19, R-05, R-13)

### Prerrequisitos adicionales autorizados: lectura masiva

Se conservan T001–T033 y su avance. T004 documenta la inspección inicial del puerto, no la
prohibición de la extensión posteriormente autorizada. Los nuevos IDs se añaden sin renumerar.
T034–T035 se realizan antes de T026; T035 fija el contrato antes de implementar T034.

- [X] T034 Implementar la extensión aditiva de `backend/src/main/java/footballmarket/integrations/QuoteSource.java` con `Map<Long, CurrentQuote> currentQuotes(Set<Long>)`, conservando `currentQuote(Long)`; el default debe rechazar la capacidad ausente mediante `backend/src/main/java/footballmarket/integrations/exceptions/QuoteBatchUnavailableException.java` (`IntegrationException`), nunca delegar N lecturas. Implementar el lote directamente en memoria en `backend/src/main/java/footballmarket/integrations/SimulatedQuoteSource.java`, con un instante común, sin E/S por jugador, conforme al contrato §2.1 (FR-027, R-07, R-17). Depende de T035.
- [X] T035 Escribir, sin ejecutar, las pruebas de la extensión masiva en `backend/src/test/java/footballmarket/integrations/SimulatedQuoteSourceTest.java`: conjuntos vacíos y múltiples, ausencia de delegación individual, mapas y snapshots coherentes, y default explícitamente no soportado; conservar pruebas del método individual y comprobar compatibilidad de la fuente alternativa de `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java`. Respetar etiquetas, `@Nested` y fronteras de categoría; no añadir consultas individuales como fallback (FR-013, FR-014, FR-027, R-07, R-13, R-17).

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Conocer el costo de adquisición y el resultado realizado de mi posición (Priority: P1) 🎯 MVP

**Goal**: La regla contable que calcula el costo de adquisición autoritativo, el precio medio derivado y el resultado realizado acumulado de una posición, con la atribución proporcional del costo de una venta, la conservación exacta del costo y el tratamiento de la emisión inicial.

**Independent Test**: Con operaciones confirmadas de un usuario, el cálculo de la posición entrega el costo de adquisición, el precio medio, el resultado realizado y los hechos del ciclo esperados, con conservación exacta del costo pagado. No requiere cotizaciones, ni portfolio, ni operar en el mercado.

### Tests para User Story 1 (backend; sin tests frontend)

**Precisión autorizada de B-02 (T013/T015):** cubrir en la prueba de liquidación total que
`averageUnitPrice` es `Long` nullable y vale `null` cuando `openQuantity = 0`; conservar costo
remanente cero y resultado realizado acumulado. No dividir por cero ni sustituir la ausencia por
un promedio cero. T015 debe exponer ese tipo conforme al contrato corregido.

- [X] T013 [P] [US1] Escribir tests de Model en `backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java` con grupos `@Nested` que cubran la acumulación de compras, el promedio ponderado móvil, la atribución proporcional de una venta parcial, el redondeo al medio hacia arriba, la conservación exacta del costo, el residuo retenido en la posición abierta, la liquidación total a costo cero, la reapertura con costo nuevo y el resultado realizado acumulado y negativo (FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-015, FR-016, FR-017, AC01, AC02, AC03, AC04, AC05, AC15, AC20, AC21, AC22, SC-001, SC-002, SC-003, SC-006, SC-009, CL-01, CL-02, CL-03, CL-04, R-13, R-16)
- [X] T014 [P] [US1] Escribir tests de Model en `backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java` con grupos `@Nested` que cubran el plegado del historial ordenado por `(executed_at, id)`, la imputación por rol de comprador y vendedor, la aplicación de la emisión como cantidad de costo cero antes de las operaciones, la dilución del promedio único en la mezcla de orígenes y el desempate ante instantes iguales (FR-002, FR-003, FR-018, FR-021, FR-022, AC07, AC08, AC09, AC10, AC16, AC17, SC-010, CL-07, CL-13, CL-16, R-04, R-06, R-18)

### Implementation for User Story 1

- [X] T015 [US1] Implementar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con el estado autoritativo —`openQuantity`, `acquisitionCost`, `totalPaid`, `realizedProfitLoss`— y las operaciones de dominio `purchase`, `sale` e `issued`, el precio medio derivado con redondeo al medio hacia arriba y las invariantes de no negatividad y conservación, sin `Builder` ni `@Setter` y con Javadoc en español (FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-015, FR-016, FR-017, CL-01, CL-02, CL-03, CL-11, R-03, R-11, R-16, `data-model.md` §2.1 y §3.1)
- [X] T016 [US1] Ampliar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con los hechos del ciclo de tenencia —`openedAt`, `closedAt`, `lastTransactionAt`— y los recuentos por rol `buyOperationCount` y `sellOperationCount`, actualizados en el mismo recorrido del historial, con la emisión inicial abriendo el ciclo sin contar como compra ni como venta (FR-009, FR-010, FR-021, FR-022, AC04, AC05, AC09, AC10, SC-005, SC-006, CL-04, CL-07, CL-13, R-06, R-18, `data-model.md` §4.1)
- [X] T017 [US1] Implementar `FinancialPositionCalculator` en `backend/src/main/java/footballmarket/models/FinancialPositionCalculator.java` como componente de Model puro y estático: ordenar las operaciones por `(executed_at, id)`, aplicarlas según el rol del usuario, aplicar las emisiones como cantidad de costo cero y devolver las posiciones ordenadas por `playerId` (FR-002, FR-003, FR-018, FR-027, AC07, AC08, AC16, AC17, SC-010, CL-06, CL-07, CL-16, R-04, R-06, R-17, R-18, `data-model.md` §2.2)

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Conocer el valor actual y el resultado no realizado de mi posición abierta (Priority: P2)

**Goal**: La valoración de la posición abierta con la cotización vigente, su resultado no realizado, el resultado total con su semántica de indefinición y la agregación contable del usuario con el porcentaje global.

**Independent Test**: Con una posición abierta valorada y otra sin cotización, el cálculo entrega el valor actual, el resultado no realizado y el total según la regla de indefinición, y la agregación sobre el conjunto completo devuelve el costo histórico, los resultados y el porcentaje global.

### Tests para User Story 2 (backend; sin tests frontend)

**Correcciones autorizadas de B-01/B-02 (T018–T021):** T018 debe cubrir el promedio nulo en una
posición cerrada y definido al reabrirse, coherente con `Long` nullable de T020. T019 debe incluir
la regresión resultado 1 e histórico 3 con expectativa `33,33 %`, además de los casos nulos ya
previstos. T021 debe multiplicar exactamente el numerador por 100 antes de dividir y aplicar
HALF_UP a escala 2, sin redondeos intermedios, devolviendo un porcentaje `BigDecimal` nullable.
Estas precisiones forman parte de las tareas existentes; no se ejecutan las pruebas.

- [X] T018 [US2] Ampliar los tests de Model creados en T013 en `backend/src/test/java/footballmarket/models/PlayerFinancialPositionTest.java` con un grupo `@Nested` de valoración que cubra precio e instante de la misma cotización, ambos nulos sin cotización, valor actual, resultado no realizado, resultado total definido e indefinido, y el total de una posición cerrada igual al resultado realizado acumulado (FR-012, FR-013, FR-031, FR-032, AC11, AC12, AC14, AC15, SC-007, SC-008, CL-05, CL-14, R-18)
- [X] T019 [US2] Ampliar los tests de Model creados en T014 en `backend/src/test/java/footballmarket/models/FinancialPositionCalculatorTest.java` con un grupo `@Nested` de agregación que cubra la distinción entre el costo histórico pagado y el costo de adquisición de las posiciones abiertas, la propagación de indefinidos cuando una posición abierta carece de cotización, el indicador de valoración completa y el porcentaje global con su denominador histórico y sus casos nulos (FR-032, CL-08, SC-008, R-19)

### Implementation for User Story 2

- [X] T020 [US2] Ampliar `PlayerFinancialPosition` en `backend/src/main/java/footballmarket/models/PlayerFinancialPosition.java` con el par de valoración `currentUnitPrice` y `quoteCalculatedAt`, tomado de la misma cotización y nulo en bloque cuando no hay cotización, y con las cifras derivadas `currentValue`, `unrealizedProfitLoss` y `totalProfitLoss` conforme a la regla de indefinición (FR-012, FR-013, FR-031, FR-032, AC11, AC12, AC14, AC15, SC-007, SC-008, CL-03, CL-05, CL-14, R-18, `data-model.md` §2.1)
- [X] T021 [US2] Implementar en `backend/src/main/java/footballmarket/models/FinancialPositionCalculator.java` la agregación contable reutilizable del usuario sobre el **conjunto completo** de posiciones, que devuelva `historicalCost`, `investedCost`, `currentValue`, `realizedProfitLoss`, `unrealizedProfitLoss`, `totalProfitLoss`, `profitLossPercentage` y `valuationComplete`, con el denominador histórico del porcentaje global y la semántica de indefinidos (FR-032, CL-08, SC-008, R-19, `data-model.md` §3.5)

**Checkpoint**: User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Verificar que el resultado que me informan es trazable y reproducible (Priority: P3)

**Goal**: El caso de uso reutilizable que reconstruye la posición desde las operaciones confirmadas y la emisión inicial, con una lectura consistente, la valoración tomada de la fuente vigente y el acceso al contrato que consume SPEC-014.

**Independent Test**: Con un usuario que tiene operaciones confirmadas y emisión, el caso de uso devuelve sus posiciones valoradas desde una única instantánea, falla ante el fallo técnico de la fuente de cotizaciones y no altera ningún dato persistido.

### Tests para User Story 3 (backend; sin tests frontend)

- [X] T022 [P] [US3] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/FinancialPositionPersistenceTest.java` con grupos `@Nested` que cubran que la consulta de operaciones devuelve las compras y las ventas del usuario sin repetir ninguna y en el orden contable, que la consulta de emisiones devuelve las del propietario y que el índice por propietario está disponible (FR-002, FR-018, AC16, AC17, SC-010, R-08, R-14)
- [X] T023 [P] [US3] Escribir tests de Service en `backend/src/test/java/footballmarket/services/FinancialPositionServiceTest.java` que afirmen únicamente lo que devuelve o lanza la API pública del caso de uso, sin tocar persistencia, y que cubran las posiciones de un usuario con operaciones y emisión, la cotización presente y ausente, la propagación del fallo técnico sin datos financieros parciales, la ausencia de escrituras, el determinismo entre dos llamadas y el fallo ante una inconsistencia de integridad; que una publicación de venta activa y vigente no altere la cantidad abierta, el costo de adquisición ni el resultado de la posición (FR-011, AC06); que el cálculo exponga únicamente las posiciones del usuario solicitado (FR-024); que una posición cuyo jugador dejó de estar en el catálogo se conserve con sus resultados (FR-026); y que el resultado realizado equivalga exactamente al importe de la venta menos el costo atribuido, sin comisiones ni deducciones (FR-029) (FR-011, FR-014, FR-020, FR-023, FR-024, FR-025, FR-026, FR-029, AC06, AC12, AC13, AC16, AC18, AC20, SC-004, SC-005, SC-010, SC-011, SC-012, CL-09, CL-16, R-15, R-18)
- [X] T024 [US3] Ampliar en `backend/src/test/java/footballmarket/services/FinancialPositionServiceTest.java` creado en T023 un grupo `@Nested` de coherencia que confirme que una operación de mercado confirmada durante el cálculo no altera la instantánea y que el resultado corresponde al estado previo, que es un estado válido (FR-020, AC18, R-13, R-15, AC34 de SPEC-014)

### Implementation for User Story 3

- [X] T025 [US3] Implementar la interfaz `FinancialPositionService` en `backend/src/main/java/footballmarket/services/FinancialPositionService.java` con la operación `positionsOf(Long userId)` y el contrato documentado en `contracts/financial-position-service.md` §2, incluidos el límite transaccional, la ausencia de cotización, el fallo técnico y la ausencia de efectos (FR-001, FR-028, CL-10, CL-11, R-18, R-19)
- [X] T026 [US3] Implementar `FinancialPositionServiceImpl` en `backend/src/main/java/footballmarket/services/impl/FinancialPositionServiceImpl.java` con la transacción de solo lectura y aislamiento `REPEATABLE READ`, dos consultas contables de conjunto y reconstrucción de posiciones antes de obtener los jugadores distintos con posición abierta; adquirir sus cotizaciones con una sola llamada a `currentQuotes`, omitirla para conjunto vacío y conservar precio e instante. No llamar al método individual ni aceptar mapas incoherentes; propagar fallos técnicos sin convertirlos en cotizaciones ausentes. Depende de T034–T035 y del modelo de US1/US2 (FR-002, FR-012, FR-024, FR-027, FR-028, FR-029, AC12, AC13, SC-005, SC-011, SC-012, CL-05, CL-15, R-02, R-07, R-15, R-17, `contracts/financial-position-service.md` §2 y §2.1).
- [X] T027 [US3] Verificar el contrato de consumo por SPEC-014 contra la implementación en `specs/015-define-average-cost-and-financial-results-rules/contracts/financial-position-service.md`: las nueve reglas de consumo de §3.2, la pareja precio-instante, los hechos del ciclo, la agregación sobre el conjunto completo y la exigencia de aislamiento del consumidor; registrar el resultado y corregir el contrato solo si aparece una discrepancia real, sin modificar SPEC-014 (FR-028, CL-11, R-12, R-18, R-19)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Afectan a varias historias y a la integración con el consumidor

- [X] T028 [P] Actualizar `docs/backend/architecture.md`: documentar los componentes de SPEC-015 —`PlayerFinancialPosition`, `FinancialPositionCalculator`, `FinancialPositionService` y su implementación— y hacer explícita la relación Service → Service autorizada entre SPEC-015 y SPEC-014, siguiendo el precedente de §3 y §4 (RA-1, R-01)
- [X] T029 [P] Verificar que la feature no introduce ruta HTTP, DTO ni Mapper en `backend/src/main/java/footballmarket/controllers/`, y que ningún contrato público de SPEC-010, SPEC-011 o SPEC-014 cambia; registrar el resultado (FR-030, R-10)
- [X] T030 [P] Verificar que no se crea ningún artefacto en `frontend/src/` ni infraestructura de pruebas de frontend, conforme a la exención de testing del frontend en la Constitución (Constitución, sección de verificación y testing)
- [X] T031 [P] Recorrer los escenarios de validación de `specs/015-define-average-cost-and-financial-results-rules/quickstart.md`, solicitar al usuario la ejecución de las suites y registrar su evidencia; el agente no ejecuta tests (R-13, SC-001 a SC-012)
  <!-- Evidencia registrada el 2026-10-20. Primera ejecución completa de la persona responsable: 1170 pruebas y 2 fallos, ambos de esta feature, diagnosticados y corregidos en su causa raíz: `FinancialPositionServiceTest` (emisión valorada a costo cero, por un fixture que registraba auditorías sin la emisión inicial) y `FinancialPositionPersistenceTest` (consultas con N+1, por la resolución de las asociaciones EAGER con una sentencia por fila). La corrección no alteró reglas contables ni aserciones. Ejecución acotada intermedia, por orden expresa del usuario y limitada a esas dos clases: 14 pruebas, 0 fallos, 0 errores. Ejecución completa posterior de la persona responsable sobre el árbol actual: 1172 pruebas en 216 clases, 0 fallos, 0 errores, 0 omitidos, 631 s; las 27 pruebas de SPEC-015 (Model, Repository y Service) pasan. La ampliación del fixture no altera el estado compartido de la base de pruebas de las demás clases. Comandos ejecutados, desde `backend/`: `./gradlew test`. Pendiente: revisión humana de la cobertura y de los criterios antes de la autorización de TESTING. El check de CI aprobado en el PR #66 corresponde a la rama sin el código de implementación y no evidencia esta feature. T031 completada: la persona responsable aprobó la validación de TESTING el 2026-10-20 sobre esa evidencia. -->
- [X] T032 [P] Ejecutar el control de build permitido para el backend, `./gradlew build -x test` desde `backend/`, y registrar el resultado; no acredita TESTING (control de build permitido, AGENTS.md del backend)
- [X] T033 [P] Actualizar el estado de avance de las tareas en `specs/015-define-average-cost-and-financial-results-rules/tasks.md` conservando los IDs y las marcas existentes, y verificar que ningún estado se replica en issues ni tarjetas de seguimiento (`docs/development/sdd-workflow.md` §2)

---

## Dependencies & Execution Order

**Integración masiva:** T035 → T034 → T026; T023–T024 deben usar el mock masivo de `QuoteSource`
y cubrir ausencia de llamadas individuales, lote ausente/error técnico y conjunto vacío. Los
tests del simulador pertenecen a Integration; el conteo SQL y la ausencia de accesos JPA N+1 se
observan en Repository, no accediendo a repositorios desde los tests de Service. T022 debe cubrir
la carga de las relaciones utilizadas durante la reconstrucción para detectar consultas ocultas
por posición. No se ejecutan estos tests desde el agente.

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

## Registro de autorización y bloqueo de IMPLEMENT — 2026-10-20

La persona usuaria autorizó explícitamente en esta conversación: «Autorizo la implementación de las 33 tareas definidas en tasks.md». Alcance: T001–T033, respetando el orden de dependencias y el contrato con SPEC-014; escribir pruebas sin ejecutarlas; sin commits, push, merge ni modificaciones de SPEC-014. Base observada: `61c1bd985ee5e0c2299ea3fa3b7a34c86a633f38`.

**Verificaciones realizadas, no implementación funcional:** checklists `requirements.md` 16/16 y `accounting.md` 39/39; árbol inicialmente limpio, rama y selección de Feature correctas; sin hooks de extensión. T001–T006 completadas por inspección documental y de fuentes:

- T001: Java 21 y aritmética de plataforma disponibles; no hace falta dependencia adicional y no se modificó el build.
- T002: última migración local `V20261019120000__create_player_valuations.sql`; revisados el generador `createMigration` y las reglas de timestamps UTC. No se generó ni aplicó migración.
- T003: estado y diff revisados antes de escribir; sin cambios locales previos que preservar.
- T004: revisados `QuoteSource.currentQuote(Long)` y `CurrentQuote`, que conserva precio e instante; permanecen intactos.
- T005: confirmada la divergencia de reloj en `BuyOrderServiceImpl.java:136,424` y el bean existente en `MarketTimeConfiguration.java:11`.
- T006: leídas las normas de tests y los precedentes `PlayerTokenTest` (`unit`), `MarketDomainPersistenceTest` y `BuyOrderServiceTest` (`integration`), con `@Nested`.

**Bloqueo previo a T007:** el contexto de implementación reveló dos contradicciones materiales que los análisis anteriores no detectaron. No se corrigen silenciosamente ni se cambia el contrato durante la escritura de código:

1. **B-01 — precisión del porcentaje (US2, T019/T021):** `data-model.md` §3.4 y §3.5 redondea el cociente a dos decimales antes de multiplicar por 100. Con resultado 1 e histórico 3 produce 33,00 %, mientras la regla de puntos porcentuales con dos decimales requiere 33,33 %. Propuesta técnica: multiplicar el numerador por 100 antes de dividir y redondear con HALF_UP a escala 2; sincronizar diseño y prueba de regresión. No requiere cambiar CL-08.
2. **B-02 — nulabilidad del promedio (US1/US2, T015/T020/T027):** `data-model.md` §2.1 declara `averageUnitPrice` indefinido cuando `q = 0`, pero `contracts/financial-position-service.md` §3 declara el tipo primitivo `long`. Ese tipo no representa ausencia. Propuesta: acordar y documentar `Long` nullable para posiciones cerradas y su prueba; no inventar un promedio cero para suplir la ausencia.

El diagnóstico anterior de preparación sin bloqueantes no acredita la resolución de estas discrepancias recién identificadas. Se conserva la autorización explícita para el alcance no afectado; la implementación funcional queda pausada hasta autorizar la corrección de los artefactos afectados y repetir ANALYZE sobre esa corrección, conforme al comando de IMPLEMENT y al workflow §7 y §10.

T007–T033 permanecen pendientes: no se escribió código, no se ejecutaron tests ni builds y no se modificaron los requisitos, los checklists ni el seguimiento remoto. T031 seguirá pendiente de evidencia de ejecución humana incluso cuando se hayan escrito las pruebas. Revisión de código, testing y cierre no aprobados.

### Corrección documental autorizada de B-01/B-02 — 2026-10-20

La persona usuaria autorizó corregir ambos puntos y repetir ANALYZE, manteniendo IMPLEMENT en
pausa. Se actualizan `plan.md`, `research.md` R-03, `data-model.md`, el contrato interno y
`quickstart.md`, además de las precisiones de T013/T015/T018–T021 anteriores. No se cambian las
reglas funcionales, no se altera ninguna otra SPEC ni se añaden tareas o componentes.

- **B-01 corregido en el diseño:** `HALF_UP_2(100 × totalProfitLoss / historicalCost)`, con
  multiplicación previa a división y redondeo. Contraste aritmético documental: `100 / 3`,
  redondeado a escala 2 con HALF_UP, es `33,33 %`. No se ejecutó un test de aplicación.
- **B-02 corregido en el contrato:** `averageUnitPrice` es `Long` nullable, `null` cuando
  `openQuantity = 0`. Las regresiones y la construcción del Model quedan pendientes en las
  tareas existentes; la corrección documental no se marca como implementación realizada.

El registro de bloqueo anterior se conserva como evidencia histórica. Esta operación solo corrige
los artefactos indicados y prepara la reevaluación de consistencia: no reanuda IMPLEMENT, no
aprueba revisión de código ni testing y no modifica el avance T001–T006 completadas / T007–T033
pendientes. La autorización previa de implementación no se sustituye por la corrección del diseño.

### Resolución autorizada del acceso masivo — 2026-10-20

La persona usuaria autorizó la extensión compatible de `QuoteSource` conservando el método
individual y prohibiendo consultas por jugador en la lectura masiva. R-07/R-08/R-17, PLAN,
modelo, contrato y quickstart se sincronizan antes de repetir ANALYZE; no se reanuda IMPLEMENT.
La prohibición de modificar el puerto que figura en la inspección inicial T004 y en versiones
anteriores del diseño queda sustituida exclusivamente para esta extensión aditiva.

Se añaden T034–T035 por necesidad técnica autorizada, conservando los 33 IDs originales y todas
sus marcas: total 35 tareas, 6 verificaciones previas completadas y 29 pendientes. El acceso
individual y los contratos HTTP de SPEC-010/011 no cambian; no se modifica SPEC-014 ni otras
SPEC. La futura fuente real deberá implementar el contrato masivo antes de servir portfolios;
no se crea ahora un motor de cotizaciones. No se escribió código, no se ejecutaron pruebas y no
se realizaron commits ni push.

### Registro de implementación — 2026-10-20

Implementación del alcance autorizado, sin ejecutar pruebas. T007–T030 y T032–T035 quedan
completadas; **T031 permanece pendiente** porque exige la evidencia de ejecución de las suites,
aportada por la persona responsable del proyecto.

**Producción**: `PlayerFinancialPosition` y `FinancialPositionCalculator` como Model puros, con
costo remanente autoritativo, promedio nullable, atribución con HALF_UP y conservación exacta;
`FinancialPositionService` y su implementación con lectura `REPEATABLE READ`, dos consultas
contables de conjunto y un único lote de cotizaciones; extensión aditiva de `QuoteSource` con
`currentQuotes(Set<Long>)` y default que rechaza el soporte ausente sin delegar N lecturas;
`SimulatedQuoteSource` con lote en memoria; `FinancialPositionIntegrityException` y
`QuoteBatchUnavailableException`; consultas de conjunto en `MarketTransactionRepository` y
`PlayerTokenEmissionRepository`; índice `player_token_emissions (owner_user_id)`; y la unificación
del reloj de confirmación en la compra directa, que no altera el contrato público.

**Pruebas escritas, no ejecutadas**: `PlayerFinancialPositionTest`, `FinancialPositionCalculatorTest`,
`FinancialPositionServiceTest`, `FinancialPositionPersistenceTest`, la extensión de
`SimulatedQuoteSourceTest`, la compatibilidad de `QuoteSimulationConfigurationTest` y la regresión
del reloj en `BuyOrderServiceTest`. `FinancialTestData` prepara historia en Arrange.

**Control ejecutado**: `./gradlew build -x test` desde `backend/`, BUILD SUCCESSFUL, con
`validateTestClassification` en 110 clases válidas. El control no compila las fuentes de prueba
porque excluye la tarea `test`: su compilación y su ejecución quedan como evidencia pendiente.

**Verificaciones de alcance**: sin rutas HTTP, DTO ni Mapper nuevos; sin cambios en los contratos
públicos de SPEC-010 y SPEC-011; sin artefactos de frontend; SPEC-014 y las demás SPEC sin
modificar. `docs/backend/architecture.md` documenta los componentes y la relación autorizada
Service → Service limitada a la integración del portfolio (RA-1).

**Correcciones posteriores al Code Review (F1–F8)**: la consulta financiera se simplificó a
`select t from MarketTransaction t where t.buyer.id = :userId or t.seller.id = :userId` y la de
emisiones a `where e.owner.id = :userId`, sin `join fetch` del catálogo: el plegado solo necesita
identificadores y las asociaciones perezosas no se inicializan. `QuoteBatchUnavailableException`
admite causa y el servicio la preserva, con un único `catch` que mantiene el tipo de los fallos de
integración. Los mocks de `Clock` quedan acotados al bean `marketClock` con `@Qualifier`. Se
eliminó el efecto lateral oculto de `valuedAt`, que validaba el desbordamiento invocando
`totalProfitLoss()`, y ahora comprueba el producto de forma explícita. La validación del estado
permanece en el constructor canónico, que el lenguaje no permite estrechar en un record público: el
Javadoc pasa a declarar la vía recomendada —las operaciones de dominio— y la validación como barrera
de construcción. La nota del reloj congelado documenta que las expectativas heredadas de
`BuyOrderServiceTest` se escribieron contra el reloj real. La aserción de sentencias pasó de igualdad
a cota superior, por depender del plan de Hibernate.

**Corrección de los dos tests fallidos de la primera ejecución humana**: la causa raíz del fallo de
la emisión valorada a costo cero fue el fixture, que registraba auditorías sin la emisión inicial que
el mercado exige, de modo que el superusuario aparecía vendiendo tokens que nunca había recibido y la
guarda de integridad rechazaba la posición; el fixture ahora aprovisiona la emisión inicial en poder
del superusuario, fechada antes de la primera operación. La causa raíz del fallo de la prueba de
consultas fue que las asociaciones {@code EAGER} se resolvían con una sentencia por fila: las
consultas financieras recuperan ahora sus relaciones en la misma sentencia, sin arrastrar el grafo
perezoso del catálogo. Ninguna regla contable ni aserción se modificó.

**Verificación**: ejecución acotada por orden expresa del usuario, limitada a
`FinancialPositionServiceTest` y `FinancialPositionPersistenceTest`: 14 pruebas, 0 fallos, 0 errores.
Después, la persona responsable ejecutó la suite completa sobre el árbol actual: **1172 pruebas en 216
clases, 0 fallos, 0 errores, 0 omitidos** (631 s), incluidas las 27 pruebas de SPEC-015. La
ejecución completa acredita la ausencia de regresiones; **la autorización de TESTING sigue requiriendo
la revisión humana de la cobertura** (T031). Revisión de código y cierre pendientes. Sin commits, sin
push, sin merge y sin cierre del issue #63.

### Reanudación autorizada — 2026-10-20

La persona usuaria autoriza actualizar y reevaluar CHK033 y, satisfechos los checklists, retomar
IMPLEMENT desde T007 con las 35 tareas y sus dependencias, incluyendo lectura masiva real sin
fallback N+1. La corrección de porcentaje y nulabilidad se conserva. Checklists reevaluados:
`requirements.md` 16/16 y `accounting.md` 39/39; CHK033 queda alineado con R-07/R-17 y contrato
§2.1. No hay criterios pendientes. No se autorizan tests ejecutados, cambios en SPEC-014,
ampliaciones de alcance, commits, push ni merge. La revisión de código y el testing siguen
pendientes de aprobación humana independiente.

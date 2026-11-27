# Tasks: Venta oficial inmediata y precio flexible P2P

**Input**: Design documents from `specs/026-market-instant-buyback-and-p2p-pricing/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Crear o modificar los tests backend indicados por las normas aplicables; su ejecución corresponde al usuario. No crear tests ni infraestructura frontend. No ejecutar tests desde el agente.

**Workflow local**: Las tareas técnicas vigentes viven exclusivamente en este archivo. No representan implementación completa, autorización ni estado de seguimiento.

## Phase 1: Setup

**Purpose**: No requiere inicialización: se reutiliza el módulo de mercado backend existente, sin dependencias ni infraestructura nuevas.

## Phase 2: Foundational

**Purpose**: No hay prerrequisitos compartidos que bloqueen todas las historias; las modificaciones de persistencia y contratos se realizan en la historia que las requiere.

---

## Phase 3: User Story 1 - Vender tokens al superusuario (Priority: P1) 🎯 MVP

**Goal**: Liquidar atómicamente la venta de tokens libres del usuario al superusuario con comisión configurable, comprobante y registro financiero auditable.

**Independent Test**: Una venta válida acredita el importe neto calculado con la comisión y transfiere tokens; los casos rechazados, incluido el precio neto cero, y los fallos de liquidación no dejan cambios parciales ni una venta confirmada.

### Tests para User Story 1

- [X] T001 [P] [US1] Añadir pruebas HTTP del endpoint de venta oficial, autenticación, request/response, errores y documentación contractual en `backend/src/test/java/footballmarket/features/market/presentation/OfficialMarketControllerTest.java`.
- [X] T002 [P] [US1] Añadir pruebas de la liquidación, comisión, redondeo, límites, tenencia libre, saldo del superusuario y rollback observable por el Service en `backend/src/test/java/footballmarket/features/market/application/OfficialBuybackServiceTest.java`.
- [X] T003 [P] [US1] Añadir pruebas de persistencia real de cotización de referencia y comisión en la transacción de mercado en `backend/src/test/java/footballmarket/features/market/infraestructure/MarketTransactionBuybackPersistenceTest.java`.
- [X] T004 [P] [US1] Añadir pruebas unitarias de las invariantes y construcción del registro de buyback en `backend/src/test/java/footballmarket/features/market/domain/models/MarketTransactionTest.java`.

### Implementation for User Story 1

- [X] T005 [US1] Añadir las columnas nullable `quote_price` y `discount_percentage` mediante una migración Flyway nueva en `backend/src/main/resources/db/migration/`.
- [X] T006 [US1] Extender la entidad `MarketTransaction` para guardar y exponer cotización aplicada y descuento sin alterar registros de modalidades existentes en `backend/src/main/java/footballmarket/features/market/domain/models/MarketTransaction.java`.
- [X] T007 [US1] Definir la configuración validada de la comisión, con valor predeterminado 10, en `backend/src/main/java/footballmarket/features/market/infraestructure/config/OfficialBuybackProperties.java`.
- [X] T008 [US1] Implementar el caso de uso de venta oficial con interfaz e implementación separadas en `backend/src/main/java/footballmarket/features/market/application/OfficialBuybackService.java` y `backend/src/main/java/footballmarket/features/market/application/impl/OfficialBuybackServiceImpl.java`.
- [X] T009 [US1] Implementar la liquidación inversa bajo el límite transaccional existente, con bloqueos en el orden definido, revalidación de reservas y saldo, aritmética exacta, registro financiero y rollback total, en `backend/src/main/java/footballmarket/features/market/application/internal/OfficialBuybackExecutor.java`.
- [X] T010 [US1] Añadir los records de dominio para solicitud y comprobante de venta oficial en `backend/src/main/java/footballmarket/features/market/domain/records/OfficialBuybackRequest.java` y `backend/src/main/java/footballmarket/features/market/domain/records/OfficialBuybackReceipt.java`.
- [X] T011 [US1] Añadir Request/Response DTO, Mapper estático y endpoint autenticado `POST /api/orders/sell-official` con anotaciones OpenAPI en `backend/src/main/java/footballmarket/features/market/presentation/dtos/request/OfficialBuybackRequestDTO.java`, `backend/src/main/java/footballmarket/features/market/presentation/dtos/response/OfficialBuybackResponseDTO.java`, `backend/src/main/java/footballmarket/features/market/presentation/OfficialMarketMapper.java` y `backend/src/main/java/footballmarket/features/market/presentation/OfficialMarketController.java`.
- [X] T012 [US1] Traducir los rechazos del caso de uso a los códigos y estados HTTP del contrato reutilizando el manejador de errores de mercado en `backend/src/main/java/footballmarket/features/market/presentation/MarketExceptionHandler.java`.
- [X] T013 [US1] Actualizar el contrato de compra oficial para documentar los campos nullable de auditoría sin cambiar el comportamiento de compra en `specs/010-buy-tokens/contracts/buy-tokens.md`.
- [X] T026 [US1] Mantener sincronizado el contrato HTTP de la venta oficial con el request, comprobante, errores y atomicidad implementados en `specs/026-market-instant-buyback-and-p2p-pricing/contracts/official-buyback.md`.

### Tests para la comisión vigente de User Story 1

- [X] T028 [P] [US1] Actualizar las pruebas del modelo para aceptar comisiones 10 y 99, rechazar valores fuera del rango y rechazar precio neto redondeado a cero en `backend/src/test/java/footballmarket/features/market/domain/models/MarketTransactionTest.java`.
- [X] T029 [P] [US1] Añadir pruebas de configuración que acepten los límites 10 y 99 y rechacen valores inferiores o superiores en `backend/src/test/java/footballmarket/features/market/infraestructure/config/OfficialBuybackPropertiesTest.java`.
- [X] T030 [P] [US1] Añadir una prueba de Service con cotización de una centésima y comisión de 99% que verifique el rechazo por precio neto cero en `backend/src/test/java/footballmarket/features/market/application/OfficialBuybackZeroPriceServiceTest.java`.

### Implementation para la comisión vigente de User Story 1

- [X] T031 [US1] Restringir la configuración de comisión al rango inclusivo 10–99, conservar el default de 10 y alinear su descripción en `backend/src/main/java/footballmarket/features/market/infraestructure/config/OfficialBuybackProperties.java` y `backend/src/main/resources/application.yml`.
- [X] T032 [US1] Alinear las invariantes y mensajes del registro financiero con la comisión permitida de 10–99, manteniendo el rechazo de precio neto no positivo, en `backend/src/main/java/footballmarket/features/market/domain/models/MarketTransaction.java`.
- [X] T033 [US1] Sincronizar la documentación de comisión y rango en el comprobante de dominio, el DTO/OpenAPI y la liquidación en `backend/src/main/java/footballmarket/features/market/domain/records/OfficialBuybackReceipt.java`, `backend/src/main/java/footballmarket/features/market/presentation/dtos/response/OfficialBuybackResponseDTO.java` y `backend/src/main/java/footballmarket/features/market/application/internal/MarketSettlementExecutor.java`.

**Checkpoint**: La venta oficial y sus rechazos pueden verificarse de forma independiente; las pruebas se ejecutan por el usuario.

---

## Phase 4: User Story 2 - Publicar tokens P2P a un precio elegido (Priority: P1)

**Goal**: Admitir un precio unitario P2P opcional dentro del intervalo inclusivo 80%–120% de la cotización al publicar y conservar el precio acordado para la compra posterior.

**Independent Test**: Crear publicaciones con precio omitido, dentro y fuera de banda; confirmar que el precio e importe pactados no cambian al variar la cotización y que la compra usa esos valores.

### Tests para User Story 2

- [X] T014 [P] [US2] Añadir pruebas HTTP para precio P2P omitido, aceptado, inválido y rechazo sin publicación/reserva en `backend/src/test/java/footballmarket/features/market/presentation/OrderControllerTest.java`.
- [X] T015 [P] [US2] Añadir pruebas de publicación y compra P2P con precio predeterminado o explícito congelado en `backend/src/test/java/footballmarket/features/market/application/TokenListingServiceTest.java`.
- [X] T016 [P] [US2] Añadir pruebas de invariantes de precio e importe pactados de la publicación en `backend/src/test/java/footballmarket/features/market/domain/models/TokenListingTest.java`.

### Implementation for User Story 2

- [X] T017 [US2] Extender el Request DTO de publicación con `unitPrice` opcional, representación estricta de entero y validación estructural en `backend/src/main/java/footballmarket/features/market/presentation/dtos/request/OrderSellRequestDTO.java`.
- [X] T018 [US2] Actualizar el record de solicitud de publicación para transportar el precio opcional a la capa de dominio en `backend/src/main/java/footballmarket/features/market/domain/records/SellOrderRequest.java`.
- [X] T019 [US2] Aplicar en la publicación el precio informado o la cotización vigente, validar la banda exacta sin overflow y congelar el precio e importe en `backend/src/main/java/footballmarket/features/market/domain/models/TokenListing.java` y `backend/src/main/java/footballmarket/features/market/application/impl/TokenListingServiceImpl.java`.
- [X] T020 [US2] Mapear el precio opcional entre DTO y solicitud de dominio sin incorporar lógica de negocio en `backend/src/main/java/footballmarket/features/market/presentation/ListingMapper.java`.
- [X] T021 [US2] Traducir el precio fuera de banda al error `INVALID_LISTING_PRICE` conservando los demás errores de publicación en `backend/src/main/java/footballmarket/features/market/presentation/MarketExceptionHandler.java`.
- [X] T022 [US2] Actualizar el contrato vigente de publicaciones para describir precio opcional y precio congelado sin alterar el resto de SPEC-011 en `specs/011-sell-player-tokens/contracts/listings.md`.
- [X] T027 [US2] Mantener sincronizado el contrato HTTP de precio opcional P2P con la validación, respuesta y liquidación implementadas en `specs/026-market-instant-buyback-and-p2p-pricing/contracts/p2p-listing-price.md`.

**Checkpoint**: La publicación acepta o rechaza según la cotización al crearla y la compra mantiene el precio acordado.

---

## Phase 5: User Story 3 - Mantener la compra oficial existente (Priority: P2)

**Goal**: Preservar ruta, reglas, respuesta y efectos financieros de la compra oficial existente.

**Independent Test**: Ejecutar el escenario vigente de compra oficial y comprobar que conserva su contrato y liquidación, sin usar las reglas nuevas de venta.

### Tests para User Story 3

- [X] T023 [US3] Añadir o ajustar una prueba de regresión de compra oficial válida y sus efectos observables en `backend/src/test/java/footballmarket/features/market/application/BuyOrderServiceTest.java`.
- [X] T024 [US3] Añadir o ajustar una prueba HTTP de compatibilidad de la ruta y respuesta de compra oficial en `backend/src/test/java/footballmarket/features/market/presentation/OrderControllerTest.java`.

### Implementation for User Story 3

- [X] T025 [US3] Mantener la compra oficial en el ejecutor y contrato existentes; corregir únicamente una incompatibilidad introducida por las historias anteriores, si la cobertura de regresión la evidencia, en `backend/src/main/java/footballmarket/features/market/application/internal/OfficialBuyOrderExecutor.java` y `specs/010-buy-tokens/contracts/buy-tokens.md`.

**Checkpoint**: La compra oficial conserva el comportamiento previo y sus contratos.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: No se agregan tareas de polish independientes; la sincronización contractual necesaria forma parte de las historias US1 y US2.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin tareas; el módulo y la infraestructura existentes bastan.
- **Foundational (Phase 2)**: Sin tareas compartidas bloqueantes.
- **User Stories (Phase 3+)**: Todas pueden comenzar con la infraestructura existente; completar cada historia respetando sus dependencias técnicas.
- **Polish (Phase 6)**: Sin tareas adicionales; los cambios contractuales imprescindibles están incluidos en US1 y US2.

### User Story Dependencies

- **User Story 1 (P1)**: Independiente; MVP recomendado.
- **User Story 2 (P1)**: Independiente de US1 en funcionalidad, reutiliza cotizaciones y publicación existentes.
- **User Story 3 (P2)**: Regresión de compra oficial existente; verificarla junto a los cambios de mercado. No depende del buyback como comportamiento.

### Within Each User Story

- Escribir y mantener tests backend antes o junto con la implementación; el usuario los ejecuta.
- Dentro de US1, T005–T007 preceden a la liquidación y a su endpoint; contrato HTTP, servicio y persistencia deben quedar sincronizados.
- Dentro de US2, T017–T018 preceden a la regla de publicación y el mapeo; el endpoint y el contrato se verifican con los tests.
- T025 solo se modifica si una regresión concreta demuestra incompatibilidad; no ampliar el flujo de compra.

### Parallel Opportunities

- US1: T001–T004 pueden desarrollarse en paralelo en archivos de test distintos.
- US2: T014–T016 pueden desarrollarse en paralelo en archivos de test distintos.
- Actualización de US1: T028–T030 pueden desarrollarse en paralelo en archivos de test distintos.
- US1 y US2 pueden implementarse en paralelo tras coordinar archivos compartidos como `MarketExceptionHandler.java` y sus contratos comunes.
- Las tareas de una misma historia con archivos compartidos o dependencias explícitas no deben solaparse.

---

## Parallel Example: User Story 1

```text
Task: T001 pruebas HTTP de venta oficial en OfficialMarketControllerTest.java
Task: T002 pruebas del caso de uso en OfficialBuybackServiceTest.java
Task: T003 pruebas de persistencia en MarketTransactionBuybackPersistenceTest.java
Task: T004 pruebas de dominio en MarketTransactionTest.java
Task: T028–T030 pruebas independientes de límites y rechazo por precio neto cero
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar US1, incluido el registro auditable, el comprobante y rollback total.
2. Detenerse para revisión de cobertura y resultados de tests ejecutados por el usuario.
3. US1 no habilita las fases posteriores ni la implementación sin autorización humana vigente.

### Incremental Delivery

1. Implementar y verificar US1 como incremento de venta oficial.
2. Implementar y verificar US2 como incremento independiente de precio P2P.
3. Verificar US3 como regresión de compatibilidad del flujo de compra existente.
4. Alinear contratos afectados y mantener la ejecución de tests a cargo del usuario.

---

## Notes

- Todas las tareas deben seguir el formato `- [ ]` (pendiente) o `- [X]` (completada),
  seguido de `Txxx [P?] [USx?] Descripción con ruta`.
- `[P]` se reserva a tareas sobre archivos distintos y sin dependencia de tareas incompletas.
- No hay tareas de frontend, ejecución de tests, autorización humana ni actualización del seguimiento operativo.
- La fase TASKS no inicia implementación ni ejecuta ANALYZE.

## Phase 7: Convergence

- [X] T034 Actualizar el Javadoc y las anotaciones OpenAPI de `OfficialMarketController` para documentar la comisión 10–99 y el rechazo por precio neto cero según plan: contrato HTTP (partial)

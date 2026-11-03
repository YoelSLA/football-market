---

description: "Task list for SPEC-011 — Vender tokens de un jugador"

---

# Tasks: Vender tokens de un jugador (SPEC-011)

**Input**: Design documents from `/specs/011-sell-player-tokens/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/listings.md, contracts/buy-listing.md, quickstart.md

**Tests**: Se escriben tests backend conforme a `docs/backend/testing.md`. Su ejecución queda a cargo del usuario; el agente MUST NOT ejecutarlos. No se crean tests ni infraestructura frontend. Convención obligatoria: clases `@Nested` con `@DisplayName` en español natural, Arrange/Act/Assert explícito, PostgreSQL real de Testcontainers y sin `this`.

**Frontera de observación por categoría** (`docs/backend/testing.md` §2, §5 y §6 — vinculante por Constitution §1): los tests de Service afirman **solo** lo que devuelve o lanza la API pública de `TokenListingService` y `BuyOrderService`; no acceden a Repository, JPA, `EntityManager`, JDBC, SQL ni DB, ni para preparación ni para assertions. Todo el estado durable —reservas, expiración efectiva, transiciones, `CHECK`, secuencias de bloqueo, concurrencia y reversión— se afirma **exclusivamente** en la categoría Repository. Controller usa `@WebMvcTest` con la cadena de seguridad real; Security ubica sus tests en `security/`. No se simula `Repository` ni se usa `verify(repository...)` en ninguna categoría.

**Workflow local**: Las tareas viven en este archivo y no se exportan automáticamente a tarjetas. Generar Tasks no habilita implementación: requiere `Analyze` vigente y autorización humana.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend único en `backend/src/`. No hay tareas de frontend en esta SPEC.
- Rutas de tests bajo `backend/src/test/java/footballmarket/`, una carpeta por categoría de `testing.md`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificaciones preparatorias y decisiones que condicionan a todas las historias. No son entregables de código.

- [X] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas: `QuoteSource`, `PlayerToken`, `UserCredit` y los descriptores de Jackson ya existen; el generador de migraciones ya está registrado
- [X] T002 [P] Verificar la siguiente versión de migración disponible con `ls backend/src/main/resources/db/migration/` y confirmar que las nuevas usarán timestamp UTC según `docs/development/flyway-migrations.md`
- [X] T003 [P] Revisar `git status --short` y preservar los cambios locales ajenos antes de escribir
- [X] T004 Declarar `market.listing.expiration-duration` con valor por defecto `PT24H` y variable `MARKET_LISTING_EXPIRATION_DURATION` en el bloque `market:` de `backend/src/main/resources/application.yml`, siguiendo el patrón de `market.quote.simulator.*`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Piezas que TODAS las historias necesitan. Ninguna historia puede comenzar antes de que esta fase esté completa.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Crear la migración de la tabla de publicaciones con `./gradlew createMigration -PmigrationDescription=create_token_listings` desde `backend/` y escribir el SQL en el archivo generado con las columnas, restricciones e índices definidos en `data-model.md`, incluidas las dos claves foráneas con `ON DELETE NO ACTION`
- [X] T006 Crear la migración de la columna de referencia con `./gradlew createMigration -PmigrationDescription=add_listing_to_market_transactions` desde `backend/` y añadir la columna nullable con clave foránea a la tabla de publicaciones, sin cascada y sin modificar restricciones ni índices existentes
- [X] T007 [P] Implementar la entidad `TokenListing` en `backend/src/main/java/footballmarket/models/TokenListing.java` con operaciones de dominio `publish`, `markSold`, `markCancelled`, `effectiveStatus(now)` e `isActiveAndValid(now)`, sin setters y preservando invariantes
- [X] T008 [P] Implementar `TokenListingRepository` en `backend/src/main/java/footballmarket/repositories/TokenListingRepository.java` con bloqueo pesimista de la fila por identificador, consulta de suma de reservas activas vigentes y consultas de lectura para mercado, propias y detalle
- [X] T009 [P] Crear las excepciones de aplicación de la feature en `backend/src/main/java/footballmarket/services/exceptions/`: `SuperuserCannotSellException`, `CannotBuyOwnListingException`, `ListingNotFoundException` y `ListingNotAvailableException`, heredando de `ApplicationException` conforme a `docs/backend/architecture.md` §1.6. `ListingNotAvailableException` debe cubrir las cuatro causas aprobadas —publicación `SOLD`, `CANCELLED`, efectivamente `EXPIRED` y ausencia excepcional de la cuenta de créditos del vendedor— mediante una fábrica o un constructor común, de modo que ninguna se reporte como saldo insuficiente del comprador: la ausencia de cuenta del vendedor jamás debe traducirse a `InsufficientCreditsException`, y el rechazo no produce transferencias, no modifica la publicación y conserva el 409 `LISTING_NOT_AVAILABLE`
- [X] T010 [P] Implementar el deserializador estricto de enteros de 64 bits en `backend/src/main/java/footballmarket/controllers/dtos/requests/StrictLongDeserializer.java`, replicando el criterio de `StrictIntegerDeserializer` —rechazar todo nodo JSON que no sea `isIntegralNumber()` o no sea convertible al rango destino— sin reutilizar el existente, que está tipado a `Integer`
- [X] T011 Añadir el campo nullable `listing` y su sobrecarga de fábrica en `backend/src/main/java/footballmarket/models/MarketTransaction.java`, conservando la firma existente de `record` para no alterar el camino de compra directa
- [X] T012 Añadir los handlers de los rechazos nuevos en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`, reutilizando el handler existente de `MethodArgumentTypeMismatchException` para el filtro de estado

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Publicar una venta de tokens (Priority: P1) 🎯 MVP

**Goal**: Un usuario autenticado publica tokens que posee; el sistema congela el precio, reserva la cantidad y no transfiere ni registra venta.

**Independent Test**: Con un usuario habilitado que posee tokens libres de un jugador visible, publicar una cantidad válida y comprobar que la publicación existe con precio congelado, que los tokens siguen del vendedor y que no hay ninguna transacción de venta. No requiere que ningún usuario compre ni consulte el mercado.

### Tests para User Story 1 (backend; sin tests frontend)

- [X] T013 [P] [US1] Escribir tests de Model en `backend/src/test/java/footballmarket/models/TokenListingTest.java` con grupos `@Nested` que cubran transiciones válidas, transición inválida, cálculo del estado efectivo en la frontera `now >= expiresAt`, cálculo del importe y no negatividad (SC-003)
- [X] T014 [P] [US1] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingPersistenceTest.java` con grupos `@Nested` que cubran `CHECK` de cantidad, precio e importe, unicidad efectiva de estado almacenado, cálculo de la suma de reservas con la condición `expires_at > :now`, precio congelado e importe exacto (SC-003), e integridad referencial de ambas claves foráneas: una publicación existente impide eliminar al usuario y al jugador referenciados y ninguna eliminación se propaga en cascada a las publicaciones (SC-001)
- [X] T015 [US1] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingConcurrencyTest.java` con un grupo `@Nested` que libere dos publicaciones simultáneas del mismo vendedor y el mismo jugador desde dentro de las transacciones, sin esperas temporales, y afirme que la suma de reservas efectivas nunca excede la tenencia, que solo se confirma la publicación respaldada por tokens disponibles y que la solicitud rechazada no deja publicación, no altera tenencias ni genera transacción (SC-002, AC13, FR-006)
- [X] T016 [P] [US1] Escribir tests de Service en `backend/src/test/java/footballmarket/services/TokenListingServiceTest.java` que afirmen únicamente lo que devuelve o lanza la API pública de publicación, sin tocar persistencia, y que la publicación creada no altera ningún saldo de créditos (SC-001)
- [X] T017 [P] [US1] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/ListingControllerTest.java` para `POST /api/orders/sell` con cadena de seguridad real, cubriendo cada código de respuesta, la no revelación de tenencias y reservas, la correspondencia exacta con el contrato documentado (SC-012) y el rechazo sin invocar el Service ante cuerpo inválido

### Implementation for User Story 1

- [X] T018 [P] [US1] Crear el record de modelo `SellOrderRequest` en `backend/src/main/java/footballmarket/models/records/SellOrderRequest.java` con el jugador y la cantidad de la intención de publicación
- [X] T019 [US1] Implementar la interfaz `TokenListingService` en `backend/src/main/java/footballmarket/services/TokenListingService.java` con la operación de publicación y su documentación de contrato
- [X] T020 [US1] Implementar `TokenListingServiceImpl` en `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java` con la transacción de publicación: resolver vendedor por el sujeto autenticado, rechazar al superusuario, validar cantidad, comprobar existencia y visibilidad del jugador, leer la cotización por `QuoteSource`, calcular el importe, bloquear la tenencia del vendedor y solo entonces obtener el instante para calcular las reservas vigentes sobre el estado confirmado, revalidar la disponibilidad e insertar la publicación con su vencimiento, conservando el bloqueo hasta commit o rollback; si una compra retiene esa tenencia, esperar y no reutilizar sus tokens antes de finalizar (C1, SC-002)
- [X] T021 [P] [US1] Crear `OrderSellRequestDTO` en `backend/src/main/java/footballmarket/controllers/dtos/requests/OrderSellRequestDTO.java` con el identificador de jugador y la cantidad, aplicando el deserializador estricto y las anotaciones de esquema
- [X] T022 [P] [US1] Crear `OrderSellResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/OrderSellResponseDTO.java` con los datos de la publicación creada y su vencimiento
- [X] T023 [US1] Extender `ListingMapper` en `backend/src/main/java/footballmarket/controllers/mappers/ListingMapper.java` con la conversión entre DTO y modelo de la publicación
- [X] T024 [US1] Exponer `POST /api/orders/sell` en `backend/src/main/java/footballmarket/controllers/OrderController.java` devolviendo `201 Created`, con las anotaciones de OpenAPI para la ruta y sus respuestas
- [X] T025 [US1] Verificar que `specs/011-sell-player-tokens/contracts/listings.md` documenta de forma coherente con `data-model.md` que el estado almacenado nunca incluye `EXPIRED` y el significado de `closedAt` en los cuatro estados efectivos; registrar el resultado de la verificación y modificar el contrato solo si aparece una discrepancia real

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Comprar la publicación de otro usuario (Priority: P1)

**Goal**: Un usuario compra la totalidad de una publicación; tokens y créditos se transfieren atómicamente, la publicación queda vendida y se registra la operación.

**Independent Test**: Con dos usuarios autenticados, uno con una publicación vigente y el otro con créditos suficientes, comprar esa publicación y comprobar transferencias, crédito al vendedor, estado `SOLD` y un único registro financiero asociado. No requiere consultar el mercado ni usar la compra directa.

### Tests para User Story 2 (backend; sin tests frontend)

- [X] T026 [P] [US2] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/OrderControllerTest.java` para la variante secundaria de `POST /api/orders/buy`, cubriendo cada código, la indistinguibilidad de motivos y la no revelación
- [X] T027 [P] [US2] Escribir tests de Service en `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` para la operación de compra de publicación, afirmando solo la API pública
- [X] T028 [P] [US2] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/MarketDomainPersistenceTest.java` con grupos `@Nested` para la reversión completa ante fallo de registro, la referencia `listing_id` presente en ventas y nula en compras directas (SC-010), la transferencia exacta con saldos y tenencias no negativos y el estado `SOLD` (SC-008), la reversión completa sin registro de venta no confirmada (SC-009) y la conservación de la suma de tokens y de créditos
- [X] T029 [P] [US2] Escribir tests de Repository de concurrencia en `backend/src/test/java/footballmarket/repositories/TokenListingConcurrencyTest.java` con dos compradores simultáneos y con compra y cancelación simultáneas, liberando la barrera dentro de las transacciones y sin `Thread.sleep`, afirriendo que se confirma exactamente una compra, que existe un único registro financiero y que la solicitud rechazada no deja movimientos ni registro (SC-007); añadir la regresión determinista C1 de `quickstart.md` 9c en ambos órdenes de adquisición de la tenencia, con reloj controlado y espera efectiva acreditada desde una transacción observadora sobre PostgreSQL real: compra que vence mientras espera y se rechaza sin movimientos frente a reserva nueva válida, y compra ya validada que conserva la tenencia hasta confirmar aunque venza, obligando a la nueva publicación a esperar y respetar la tenencia reducida. Afirmar reservas nunca superiores a tenencias, saldos, estado y auditoría coherentes (SC-002, SC-004, SC-008, SC-009); una barrera de inicio sola no acredita esta carrera
- [X] T030 [P] [US2] Escribir tests de Repository para el vendedor sin cuenta de créditos y para la tenencia insuficiente del vendedor, afirmando en ambos casos que no hay transferencias, ni cambio de saldo, ni registro de transacción, ni marca `SOLD` (SC-009) en `backend/src/test/java/footballmarket/repositories/TokenListingPersistenceTest.java`
- [X] T031 [US2] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingExpirationPersistenceTest.java` con un grupo `@Nested` que verifique ambas ramas de la expiración de forma determinista, fijando el instante en lugar de esperar en el reloj: obtenerlo después de bloquear publicación y ambas tenencias; `now >= expiresAt` en ese punto devuelve 409 `LISTING_NOT_AVAILABLE` sin movimientos y con reversión de toda preparación, mientras una publicación que superó esa única validación completa la liquidación aunque el reloj alcance `expiresAt` antes del commit, sin una segunda validación temporal. Complementar la regresión C1 de T029 y `quickstart.md` 9c, sin sustituir por una barrera inicial la prueba de que el instante decisivo es posterior a adquirir las tenencias (SC-004, FR-011)
- [X] T032 [US2] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/MarketDomainPersistenceTest.java` con un grupo `@Nested` que afirme que la compra de una publicación de jugador no visible responde 404 `LISTING_NOT_FOUND` sin revelar que la publicación existe, sin transferencias, sin transacción y sin cambiar el estado de la publicación, mientras el propietario conserva su acceso (SC-014, FR-019, AC31)

### Implementation for User Story 2

- [X] T033 [P] [US2] Crear el record de modelo `BuyListingRequest` en `backend/src/main/java/footballmarket/models/records/BuyListingRequest.java` con el identificador de publicación
- [X] T034 [US2] Ampliar `BuyOrderService` en `backend/src/main/java/footballmarket/services/BuyOrderService.java` con la operación de compra de publicación
- [X] T035 [US2] Implementar la compra de publicación en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` siguiendo el flujo corregido C1 de `plan.md`: validar el identificador, leer la publicación sin bloqueo para conocer el vendedor, bloquear las cuentas en orden ascendente, bloquear la publicación, revalidar identidad del vendedor, propiedad y estado almacenado sin decidir aún la vigencia, insertar la tenencia del comprador si falta y bloquear ambas tenencias en orden ascendente; solo entonces obtener `validatedAt` y realizar la única validación temporal decisiva. Si venció durante la espera, rechazar sin movimientos y con rollback de toda preparación. Si sigue vigente, revalidar saldo y stock, transferir, marcar `SOLD` con el instante ya validado y registrar la operación, reteniendo cuentas, publicación y tenencias hasta commit o rollback, sin segunda validación temporal aunque venza durante la liquidación
- [X] T036 [US2] Hacer opcional `playerId` y `quantity` y añadir el campo `listingId` con deserializador estricto en `backend/src/main/java/footballmarket/controllers/dtos/requests/OrderBuyRequestDTO.java`, conservando intacta la validación de la modalidad directa
- [X] T037 [P] [US2] Añadir el campo opcional `listingId` en `backend/src/main/java/footballmarket/controllers/dtos/responses/OrderBuyResponseDTO.java`
- [X] T038 [US2] Documentar en `backend/src/main/java/footballmarket/controllers/OrderController.java` con OpenAPI la variante secundaria de `POST /api/orders/buy`, incluyendo los códigos propios de esa modalidad
- [X] T039 [US2] Actualizar `specs/010-buy-tokens/contracts/buy-tokens.md` para dejar constancia de que la obligatoriedad de `playerId` y `quantity` rige para la modalidad directa y que la variante secundaria se rige por su propio contrato
- [X] T040 [P] [US2] Actualizar `specs/010-buy-tokens/data-model.md` con la columna nullable de referencia a publicación y su política sin cascada
- [X] T041 [US2] Ajustar las pruebas existentes de SPEC-010 en `backend/src/test/java/footballmarket/controllers/OrderControllerTest.java` y `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` para el nuevo contrato de solicitud, sin debilitar ninguna expectativa vigente

**Checkpoint**: User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Consultar las publicaciones disponibles (Priority: P2)

**Goal**: El usuario lista el mercado, lista sus propias publicaciones con filtro por estado efectivo y consulta el detalle de una publicación.

**Independent Test**: Con dos usuarios y publicaciones en distintos estados y sobre jugadores visibles e invisibles, listar el mercado, listar las propias con y sin filtro y consultar el detalle. No requiere ninguna compra.

### Tests para User Story 3 (backend; sin tests frontend)

- [X] T042 [P] [US3] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/ListingControllerTest.java` para las tres rutas de consulta, incluyendo el rechazo del filtro con capitalización incorrecta y el `404` de detalle no visible para terceros
- [X] T043 [P] [US3] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingPersistenceTest.java` para las exclusiones del mercado (SC-005), la inclusión de jugadores no visibles en las propias y el filtrado por estado efectivo (SC-006)
- [X] T044 [US3] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingVisibilityPersistenceTest.java` con un grupo `@Nested` que cree una publicación para un jugador visible, desactive luego el jugador y afirme que la publicación desaparece de las consultas públicas, sigue apareciendo en las propias con su estado efectivo, permanece accesible en el detalle del propietario, no se elimina ni cambia de estado automáticamente y no figura entre las accesibles para terceros (SC-013, FR-029)
- [X] T045 [P] [US3] Escribir tests de Service en `backend/src/test/java/footballmarket/services/TokenListingServiceTest.java` para las tres consultas de lectura, afirmando solo la API pública, incluido que un tercero no obtiene detalle de publicaciones que no le son visibles (SC-006)

### Implementation for User Story 3

- [X] T046 [P] [US3] Crear `TokenListingResponseDTO` en `backend/src/main/java/footballmarket/controllers/dtos/responses/TokenListingResponseDTO.java` con el estado efectivo y el momento de cierre
- [X] T047 [US3] Implementar las consultas en `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java`: mercado, propias con filtro por estado efectivo y detalle según identidad del solicitante
- [X] T048 [US3] Exponer `GET /api/listings`, `GET /api/listings/mine` y `GET /api/listings/{id}` en `backend/src/main/java/footballmarket/controllers/ListingController.java` con anotaciones de OpenAPI y validación del valor del filtro
- [X] T049 [US3] Documentar en `specs/011-sell-player-tokens/contracts/listings.md` que el filtro acepta únicamente las cuatro formas exactas y que decide solo por estado efectivo

**Checkpoint**: User Stories 1, 2 AND 3 should now be independently functional

---

## Phase 6: User Story 4 - Cancelar mi publicación (Priority: P2)

**Goal**: El propietario cancela una publicación vigente, que pasa a `CANCELLED` y deja de reservar tokens.

**Independent Test**: Con un usuario que tiene una publicación vigente, cancelarla y comprobar que los tokens se liberan y puede volver a publicarlos. No requiere que ningún usuario consulte ni compre.

### Tests para User Story 4 (backend; sin tests frontend)

- [X] T050 [P] [US4] Escribir tests de Controller en `backend/src/test/java/footballmarket/controllers/ListingControllerTest.java` para la cancelación, incluyendo el rechazo por falta de autorización y el rechazo por estado no vigente
- [X] T051 [P] [US4] Escribir tests de Service en `backend/src/test/java/footballmarket/services/TokenListingServiceTest.java` para la cancelación según propietario y estado
- [X] T052 [P] [US4] Escribir tests de Repository en `backend/src/test/java/footballmarket/repositories/TokenListingPersistenceTest.java` para la liberación efectiva de la reserva que permite republicar (SC-011) y para la conservación del registro histórico

### Implementation for User Story 4

- [X] T053 [US4] Implementar la cancelación en `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java`: bloquear la fila, comprobar propiedad y vigencia, marcar `CANCELLED` con su momento de cierre, sin tocar cuentas ni tenencias
- [X] T054 [US4] Exponer `POST /api/listings/{id}/cancel` en `backend/src/main/java/footballmarket/controllers/ListingController.java` con anotaciones de OpenAPI

**Checkpoint**: All user stories should now be independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T055 [P] Cerrar la revisión de `docs/backend/technologies.md` con evidencia explícita: si SPEC-011 no introduce tecnologías ni dependencias nuevas —esperado según `plan.md`— la tarea se cierra como «sin cambios necesarios» dejando constancia de que no se modificó el archivo; solo si apareciera un cambio real de inventario se documentaría lo que exijan las convenciones del proyecto
- [X] T056 [P] Actualizar `specs/trello.md` con el registro de SPEC-011 y la referencia a sus artefactos, sin crear tarjetas de US antes del cierre de READY
- [X] T057 Revisar que los casos límite clasificados OBLIGATORIO en `spec.md` tengan tarea o escenario de verificación que los cubra, en particular la expiración durante una compra en curso y los dos órdenes de la carrera C1 entre compra, vencimiento y nuevas reservas, trazados a T029/T031 y `quickstart.md` 9c; distinguir cobertura prevista de regresiones escritas y ejecutadas
- [X] T058 Revisar `specs/011-sell-player-tokens/quickstart.md` y dejar anotados los resultados observados por el usuario en cada escenario, sin ejecutarlos desde el agente
- [X] T059 [P] Ejecutar el build permitido del backend, `./gradlew build -x test` desde `backend/`, y registrar el resultado observado
- [X] T060 Solicitar al usuario la ejecución de la suite de tests y registrar la evidencia aportada; los tests aplicables pendientes impiden declarar DONE

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 y US2 son P1; US3 y US4 son P2. US2 depende de US1 y US4 depende de US3, conforme a la tabla de dependencias obligatorias.
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

Las dependencias siguientes son **obligatorias por conflicto de archivos**, no preferencias de
planificación. Ninguna historia puede ejecutarse en paralelo con la que comparte archivos con ella.

| Historia | Depende de | Motivo verificable |
|---|---|---|
| **US1 (P1)** | Solo Foundational | Su fase de tests escribe `TokenListingTest.java`, `TokenListingPersistenceTest.java`, `TokenListingConcurrencyTest.java`, `TokenListingServiceTest.java` y `ListingControllerTest.java` |
| **US2 (P1)** | **US1** | Comparte `OrderController.java`, `OrderBuyRequestDTO.java` y `BuyOrderServiceImpl.java` con US1. Editarlos en paralelo produce conflictos con merge no determinista |
| **US3 (P2)** | Solo Foundational | Su implementación escribe `TokenListingResponseDTO.java` y el camino de lectura de `TokenListingServiceImpl.java` |
| **US4 (P2)** | **US3** | Comparte `TokenListingServiceImpl.java` y `ListingController.java` con US3 |

**Regla de conflicto dentro de una fase**: `[P]` significa que la tarea escribe un archivo distinto al
de cualquier otra tarea `[P]` de la misma fase y no depende de ella. Dos tareas que escriben el mismo
archivo nunca llevan ambas `[P]`. Aplicado en esta revisión: T028 y T032 escriben
`MarketDomainPersistenceTest.java`, y solo T028 conserva `[P]`; del mismo modo T026 y T041 escriben
`OrderControllerTest.java`, y solo T026 conserva `[P]`.

**Compartición de archivo entre historias**: T015 y T029 modifican **el mismo archivo**,
`TokenListingConcurrencyTest.java`. No genera conflicto de paralelismo porque pertenecen a fases
distintas —T015 corresponde a US1 y T029 a US2— y US2 depende obligatoriamente de US1, de modo que
se ejecutan secuencialmente. Lo mismo ocurre con `TokenListingPersistenceTest.java`, compartido por
T014, T030, T043 y T052, y con `TokenListingServiceTest.java`, compartido por T016, T045 y T051: en
todos los casos el orden US1 → US2 y US3 → US4 serializa las tareas que los tocan.

**Regla de conflicto entre fases**: varias fases escriben los mismos archivos de prueba —
`ListingControllerTest.java` (T017, T042 y T050), `TokenListingPersistenceTest.java` (T014, T030,
T043 y T052), `TokenListingServiceTest.java` (T016, T045 y T051) y `TokenListingConcurrencyTest.java`
(T015 y T029). Esa compartición es admisible porque las fases se ejecutan **secuencialmente**, y es
el único caso en que un archivo compartido entre fases no exige corrección.

### Within Each User Story

- Escribir tests backend necesarios; su ejecución y evidencia corresponden al usuario
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel within Phase 2 (T007, T008, T009, T010); escriben archivos distintos
- Las historias **no** se lanzan en paralelo entre sí cuando comparten archivos: US2 espera a US1 y US4 espera a US3
- All tests for a user story marked [P] can run in parallel
- Models and DTOs within a story marked [P] can run in parallel
- US3 y US4 se ejecutan secuencialmente: US4 espera a US3 por los archivos compartidos, sin excepción por reparto de archivos.

---

## Parallel Example: User Story 1

```bash
# Escribir tests backend de US1; no ejecutarlos desde el agente:
Task: "Escribir tests de Model en backend/src/test/java/footballmarket/models/TokenListingTest.java"
Task: "Escribir tests de Repository en backend/src/test/java/footballmarket/repositories/TokenListingPersistenceTest.java"
Task: "Escribir tests de Service en backend/src/test/java/footballmarket/services/TokenListingServiceTest.java"
Task: "Escribir tests de Controller en backend/src/test/java/footballmarket/controllers/ListingControllerTest.java"

# Lanzar en paralelo el modelo y los DTO de US1:
Task: "Crear el record de modelo SellOrderRequest en backend/src/main/java/footballmarket/models/records/SellOrderRequest.java"
Task: "Crear OrderSellRequestDTO en backend/src/main/java/footballmarket/controllers/dtos/requests/OrderSellRequestDTO.java"
Task: "Crear OrderSellResponseDTO en backend/src/main/java/footballmarket/controllers/dtos/responses/OrderSellResponseDTO.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario
5. El MVP ya es demostrable por `quickstart.md` escenarios 1, 2, 3, 7 y 13

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. US4 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
6. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 y después User Story 2 (comparten `OrderController`, `OrderBuyRequestDTO` y `BuyOrderServiceImpl`)
   - Developer B: User Story 3 y después User Story 4 (comparten `TokenListingServiceImpl` y `ListingController`)
   - Developer C: apoyo en tareas de test de archivos exclusivos de cada historia, respetando los órdenes A→US2 y B→US4
3. Stories complete and integrate independently

---

## Notes

### Cierre vigente — SPEC-011 DONE (2026-10-18)

Usuario aprueba el resultado final CONVERGED presentado y sus límites; autorización final
persistida/verificada en épica: `6ad50693400c55a3098e9a48`. Spec Approved, épica y US01–US04 DONE.
SDD Lifecycle 8/8; **60/60 tareas**, incluidas T058/T060. Sin tareas nuevas ni renumeraciones.
Los pendientes de gate de registros inferiores quedan superados, conservando el historial.

### Estado vigente — CONVERGE satisfactorio; gate final pendiente (2026-10-18)

Reanudación con bash autorizada expresamente: prerrequisito exitoso, contexto SPEC-011.
CONVERGE revisó FR-001–033, AC01–48, SC-001–014 y nueve decisiones de plan sin hallazgos
accionables nuevos. No anexó tareas ni fases; siguen **60/60**, incluidas T058/T060.
US01–US04 DONE; CODE REVIEW/TESTING conservados. Épica TESTING, SDD Lifecycle 8/8.
El fallo 126 anterior está superado, no eliminado del historial. Informe y limitaciones:
convergence-report.md. El workflow exige autorización final sobre el resultado presentado;
no se atribuye revisión de este resultado a la autorización anticipada del usuario.
No Feature DONE/Approved todavía. Este registro de seguimiento es externo al comando CONVERGE.

### Estado vigente — TESTING aprobado; CONVERGE bloqueado (2026-10-18)

T058 completada: revisión y registro por escenario en quickstart.md mediante equivalencias
automatizadas expresamente aceptadas por el usuario, con diez coberturas completas y ocho
parciales. No se acredita HTTP manual, reinicio, observador de bloqueo en 13/14 ni historial
V9/V10 de desarrollo. T060 permanece completa. **60/60 tareas**, sin renumeración ni refuerzos.
TESTING aprobado por HUMAN para US01–US04 y persistido/verificado; CODE REVIEW conservado.
Las cuatro US están DONE; Feature TESTING para CONVERGE final, no DONE.
El prerrequisito obligatorio de CONVERGE falló con permiso denegado (126); diagnóstico detenido,
sin hallazgos de implementación evaluados ni tareas de remediación anexadas por ese comando.
Ver `convergence-report.md`. Los estados inferiores son históricos y quedan superados.

### Consolidación TESTING — evidencia humana verificada (estado vigente, 2026-10-18)

T060 completada por confirmación humana de `./gradlew test` satisfactorio y lectura de 159 XML
y resumen HTML: **938 ejecutados/exitosos, 0 fallidos y 0 omitidos**. C1, expiración, CR-01,
SPEC-010 y ListingControllerTest están presentes y exitosos. Historial de la primera ejecución
(938 tests, tres fallos temporales), corrección y ejecución posterior en `testing-report.md`.
Se conservan los 60 IDs: **59 `[X]`, T058 `[ ]`**. T058 requiere resultados por escenario o
aceptación humana explícita de equivalencia automatizada. No se aprueba TESTING por este registro,
no se cambian columnas, CODE REVIEW se conserva, épica IN PROGRESS y CONVERGE no habilitado.
El agente solo leyó reportes: no ejecutó ni compiló tests, Gradle, Flyway o servicios.

### CODE REVIEW aprobado — TESTING humano pendiente (estado vigente, 2026-10-18)

El usuario aceptó CR-01/02/03 y aprobó expresamente CODE REVIEW de US01–US04. Aprobaciones
individuales persistidas/verificadas en Trello, con detalle en `specs/trello.md`. Las cuatro US
están en TESTING; la Feature permanece IN PROGRESS. Esto no acredita tests compilados ni ejecutados
ni aprobación de TESTING. Procedimiento y evidencia solicitada: `testing-report.md`.
Se mantienen **60 IDs, 58 `[X]` y T058/T060 `[ ]`**. No se ejecutan verificaciones en esta operación;
el build histórico precede a CR-01. CONVERGE y cierre siguen sin habilitarse. Se conserva abajo
la evidencia de entrega y revisión previa sin borrarla.

### Reanudación integral — entrega previa al 2026-10-18 (historial)

Autorización humana expresa de US01–US04/T001–T060 después de ANALYZE 7 e I4 editorial;
persistida por US en Trello, con revalidación de READY sin borrar su invalidación anterior.
Se conservaron las 34 tareas completadas. Se cierran 24 adicionales por escritura/revisión en
fuente: T009, T013–T017, T020, T026–T032, T035, T041–T045, T050–T052 y T057.
Resultado: **58 `[X]` / 2 `[ ]`**, sin IDs añadidos, eliminados o renumerados.

- C1 aplicado en `BuyOrderServiceImpl.buyListing`: cuentas → publicación → ambas tenencias →
  único instante decisivo. La publicación conserva la tenencia antes de leer reloj/reservas.
  `@Transactional` mantiene bloqueos hasta commit/rollback; no hay segunda lectura temporal.
- T009: las cuatro causas usan el mismo constructor y 409. T020/T035 revisadas contra el plan
  corregido. Ambas carreras de T029/quickstart 9c escritas con observación de bloqueo real;
  T031 exige frontera exacta, rollback de preparación y una sola consulta al reloj.
- Tests escritos/revisados: **ninguno compilado ni ejecutado**. Sus `[X]` acreditan solo la tarea
  de escritura; la evidencia de comportamiento y cualquier fallo de compilación quedan pendientes
  del usuario. T016 observa la API pública; los créditos durables se afirman en Repository.
- T057: matriz de los ocho casos obligatorios en `implementation-review.md`, con prueba escrita
  y ejecución humana pendiente diferenciadas.
- T059: build final permitido observado **BUILD SUCCESSFUL (6 s)**; intentos previos fallaron por formato
  y se corrigieron las fuentes. El grafo no compila ni ejecuta tests.
- T058/T060 siguen pendientes: no hay resultados del quickstart ni suite aportados por el usuario.
- Los cuatro Code Review Reports están en `implementation-review.md`; CODE REVIEW no aprobado,
  TESTING pendiente y CONVERGE no habilitado. La entrega técnica no declara DONE ninguna US.
- Migraciones intactas y no aplicadas; sin base modificada, commits, push ni Git mutante.

### Ejecución parcial — bloqueo C1 detectado en IMPLEMENT (historial, superado en fuente)

**2026-10-18 — corrección exclusivamente documental autorizada**: C1 queda resuelto en el diseño
mediante la única validación temporal después de adquirir publicación y tenencias. Los escenarios
de regresión están previstos en T029/T031 y `quickstart.md` 9c. No se modificó el código parcial,
que conserva el orden defectuoso; la corrección de implementación y la verificación siguen
pendientes. Se conservan las 60 tareas, sus IDs y dependencias, y exactamente 34 `[X]`/26 `[ ]`.
IMPLEMENT permanece suspendido; no se restaura READY ni se aprueba ningún gate por esta edición
ni por el Analyze posterior. El detalle siguiente conserva la evidencia histórica del bloqueo.

- Evidencia de los `[X]`: fuentes escritas y revisadas y build `./gradlew build -x test`
  satisfactorio (última ejecución: BUILD SUCCESSFUL, 5 s). No ejecuta ni compila tests;
  no acredita comportamiento en PostgreSQL, CODE REVIEW ni TESTING.
- T005/T006: generadas sin conexión a base ni ejecución de Flyway:
  `V20261017233439__create_token_listings.sql` y
  `V20261017233445__add_listing_to_market_transactions.sql`; SQL escrito según el diseño.
- T025/T049: contrato revisado, sin cambios necesarios: `EXPIRED` no se almacena,
  `closedAt` solo se informa al vender/cancelar y el filtro exacto opera por estado efectivo.
- T055: inventario tecnológico revisado, sin dependencias nuevas ni cambios necesarios en
  `docs/backend/technologies.md` ni `backend/build.gradle.kts`.
- T009 queda pendiente por la revisión completa de las causas de rechazo. T020/T035 quedan
  pendientes: el flujo escrito reproduce el diseño aprobado pero presenta el bloqueo C1.
- Tests de T013–T017, T026–T032, T041–T045 y T050–T052 escritos parcial o sustancialmente,
  pero permanecen sin marcar: falta completar/revisar cobertura y compilación; no se ejecutaron.
  T057/T058/T060 también pendientes. No hay resultados funcionales aportados por el usuario.
- **C1 — bloqueante de consistencia/integridad detectado en el flujo anterior**: el diseño de compra
  validaba la vigencia antes de bloquear tenencias (antiguos pasos 7 y 9 del plan). Una compra validada antes de `expiresAt`
  puede esperar esa tenencia mientras una publicación concurrente, ya alcanzado el vencimiento,
  deja de contar su reserva y reserva toda la tenencia. La compra luego descuenta tokens y
  puede dejar reservas vigentes superiores a la tenencia restante (SC-002/FR-006/FR-021).
  Ejemplo analítico, no test ejecutado: tenencia 20; venta antigua de 5 validada; nueva
  publicación reserva 20 tras el vencimiento; venta antigua liquida y deja tenencia 15/reserva 20.
- Propuesta registrada durante el bloqueo: adquirir tenencias antes de la única validación temporal
  decisiva, conservando cuentas → publicación → tenencias y sin segunda lectura del reloj. Fue
  autorizada y aplicada **solo a los documentos** el 2026-10-18, sin modificar decisiones ajenas a C1.
  Requiere Analyze del diseño corregido y revisión humana; no se ha aplicado a la implementación.

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- No task authorizes code changes outside `backend/` and `specs/011-sell-player-tokens/`, salvo la actualización documental de SPEC-010 listada en US2
- Ninguna tarea habilita una regla nueva de negocio: el alcance es el de `spec.md` ya revisado

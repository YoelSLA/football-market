---

description: "Task list for SPEC-010 — Comprar tokens de un jugador"
---

# Tasks: Comprar tokens de un jugador (SPEC-010)

**Input**: Design documents from `/specs/010-buy-tokens/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Se escriben tests backend conforme a `docs/backend/testing.md`. Su ejecución queda a cargo del usuario; el agente MUST NOT ejecutarlos. No se crean tests ni infraestructura frontend.

**Workflow local**: Las tareas viven en este archivo y no se exportan automáticamente a tarjetas. Generar Tasks no habilita implementación: requiere Analyze vigente y autorización humana.

**Convención de tests** (`docs/backend/testing.md` §16, obligatoria): agrupar todos los casos en clases `@Nested` con `@DisplayName`, sin métodos de prueba sueltos en la clase contenedora, sin uso de `this` y con estructura Arrange/Act/Assert explícita. Cada `@DisplayName` va **en español natural** y explica condición y comportamiento observable (Constitution §5). PostgreSQL real de Testcontainers, nunca H2.

**Frontera de observación por categoría** (`docs/backend/testing.md` §2, §5 y §6 — vinculante por Constitution §1): los tests de Service afirman **solo** lo que devuelve o lanza la API pública de `BuyOrderService`; no acceden a Repository, JPA, `EntityManager`, JDBC, SQL ni DB, ni para preparación ni para assertions. Como `BuyOrderService` no expone saldos, tenencias ni auditoría, todo el estado durable (SC-008 a SC-013, idempotencia, concurrencia, reversión) se afirma **exclusivamente** en la categoría Repository. Controller usa `@WebMvcTest`; Security ubica sus tests en `security/`. No se simula `Repository` ni se usa `verify(repository...)` en ninguna categoría.

**Organization**: Tasks grouped by user story. Fase 2 es autocontenida: no depende de artefactos de fases posteriores.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencia de tareas inconclusas)
- **[Story]**: historia de `spec.md` a la que pertenece la tarea
- Rutas exactas incluidas en cada descripción

---

## Fase 1: Setup (verificaciones preparatorias)

**Purpose**: Confirmar la base técnica antes de escribir. Son verificaciones, no entregables de código (G11).

- [X] T001 Verificar que `backend/build.gradle.kts` no requiere dependencias nuevas y que la siguiente migración libre es `V9` (las vigentes son V1–V8)
- [X] T002 [P] Verificar `git status --short` y preservar cambios locales ajenos antes de escribir

---

## Fase 2: Foundational (prerrequisitos bloqueantes)

**Purpose**: Persistencia, modelo de dominio y contrato de cotización. Bloquea el inicio de US1.

**⚠️ CRITICAL**: Ningún trabajo de US1 puede comenzar antes de completar esta fase. Esta fase no depende de ningún artefacto de US1.

### Migración única

- [X] T003 Crear `backend/src/main/resources/db/migration/V9__create_market_domain.sql` con las cuatro tablas de `data-model.md` — `user_credits`, `player_tokens`, `player_token_emissions`, `market_transactions` — con claves foráneas y cascadas según `data-model.md` (cascada `ON DELETE CASCADE` en las tablas del dominio salvo `market_transactions`, que no la lleva) e índices según `data-model.md`; restricciones: `available_credits >= 0`, `quantity >= 0`, `PRIMARY KEY (player_id)` en `player_token_emissions`, `initial_quantity = 100`, `quantity > 0`, `unit_price > 0`, `total_amount = quantity * unit_price`, los ocho estados con `>= 0` y `UNIQUE (player_id, owner_user_id)` en `player_tokens`
- [X] T004 En `V9__create_market_domain.sql`, insertar de forma idempotente las cuentas de créditos de usuarios existentes con `ON CONFLICT DO NOTHING`: 10000 para USER y 0 para SUPERUSER, sin sobrescribir saldos existentes
- [X] T005 En `V9__create_market_domain.sql`, registrar de forma idempotente la emisión inicial de 100 tokens por cada jugador existente con `ON CONFLICT DO NOTHING` y asignar la tenencia al superusuario, solo si existe superusuario

### Modelos

- [X] T006 [P] Crear `backend/src/main/java/footballmarket/models/UserCredit.java` con saldo en centésimas (`long`) y operaciones de débito y crédito que preserven la no negatividad
- [X] T007 [P] Crear `backend/src/main/java/footballmarket/models/PlayerToken.java` con cantidad en `INT` y operaciones de transferencia que preserven la no negatividad
- [X] T008 [P] Crear `backend/src/main/java/footballmarket/models/PlayerTokenEmission.java` sin operaciones de modificación
- [X] T009 [P] Crear `backend/src/main/java/footballmarket/models/MarketTransaction.java` como registro inmutable sin operaciones de modificación, con los 17 campos de `data-model.md`: `buyer_id`, `seller_id`, `player_id`, `quantity`, `unit_price`, `total_amount`, los ocho estados anterior y posterior (`buyer_credits_before/after`, `seller_credits_before/after`, `buyer_tokens_before/after`, `seller_tokens_before/after`), `quote_calculated_at`, `quote_strategy_version` (admite `null`) y `executed_at`

### Repositorios

- [X] T010 Crear `backend/src/main/java/footballmarket/repositories/UserCreditRepository.java` con consulta de bloqueo de cuentas del comprador y vendedor ordenadas por `user_id` ascendente
- [X] T011 Crear `backend/src/main/java/footballmarket/repositories/PlayerTokenRepository.java` con inserción idempotente de la primera tenencia (`ON CONFLICT DO NOTHING`) y consulta de bloqueo de tenencias ordenadas por propietario
- [X] T012 Crear `backend/src/main/java/footballmarket/repositories/PlayerTokenEmissionRepository.java`
- [X] T013 Crear `backend/src/main/java/footballmarket/repositories/MarketTransactionRepository.java`

### Contrato de cotización (Integration)

- [X] T014 [P] Crear `backend/src/main/java/footballmarket/models/records/CurrentQuote.java` con `playerId`, `price` (`long`, centésimas), `calculatedAt` (`OffsetDateTime`, UTC) y `strategyVersion` (`String`, admite `null`)
- [X] T015 Crear `backend/src/main/java/footballmarket/integrations/QuoteSource.java` como puerto Integration con `Optional<CurrentQuote> currentQuote(Long playerId)`; no es un Service
- [X] T016 Crear `backend/src/main/java/footballmarket/integrations/SimulatedQuoteSource.java` como implementación temporal de `QuoteSource`, desactivada por defecto, con precio por token en centésimas leído de `market.quote.simulator.price` (defecto `100`) e identificador de estrategia que la marque como simulada
- [X] T017 Crear `backend/src/main/java/footballmarket/config/QuoteSimulationConfiguration.java` que registre `SimulatedQuoteSource` únicamente cuando no exista otra implementación de `QuoteSource` y la propiedad `market.quote.simulator.enabled` esté activa

**Checkpoint**: base lista. La compra no está disponible hasta completar US1.

---

## Fase 3: User Story 1 — Comprar tokens de un jugador (Priority: P1) 🎯 MVP

**Goal**: Un usuario autenticado y no superusuario compra entre 1 y 100 tokens de un jugador visible contra el superusuario, con transferencia atómica de créditos y tokens y auditoría completa.

**Independent Test**: Con usuario normal de 100 créditos, jugador visible emitido (100 tokens en superusuario) y cotización disponible, `POST /api/orders/buy` devuelve 200 con los seis campos de FR-018, transfiere cantidad e importe exactos y deja exactamente un registro de auditoría. Cada rechazo conserva los estados previos.

### Tests de Model (puros, sin Spring)

- [X] T018 [P] [US1] Tests de dominio de `UserCredit` en `backend/src/test/java/footballmarket/models/UserCreditTest.java`: débito mayor al saldo falla, débito válido descuenta, crédito suma sin quedar negativo
- [X] T019 [P] [US1] Tests de dominio de `PlayerToken` en `backend/src/test/java/footballmarket/models/PlayerTokenTest.java`: transferir más de la tenencia falla, transferir conserva el total entre ambos propietarios

### Tests de Controller (contrato HTTP, Service simulado)

- [X] T020 [US1] Tests de contrato en `backend/src/test/java/footballmarket/controllers/OrderControllerTest.java`: 200 con los seis campos; 400 `INVALID_REQUEST` por body ilegible y por quantity ausente, nula, string, booleano, array, objeto, decimal, exponencial o entero fuera de Integer, sin invocar el Service; 400 `INVALID_QUANTITY` por literal entero representable fuera de 1–100; límites 1 y 100 continúan el caso de uso sin omitir otros rechazos; 403 `SUPERUSER_CANNOT_BUY`; 404 `PLAYER_NOT_FOUND` y `PLAYER_NOT_VISIBLE`; 409 para cada código de negocio; 500 genérico (SC-014: campos, unidades monetarias y errores). H02 resuelto y verificado por el usuario: `./gradlew test --tests '*OrderControllerTest'` (27 s), `./gradlew test --tests '*BuyOrderServiceTest'` (40 s), `./gradlew test` (3 min 39 s) y `./gradlew spotlessCheck` (3 s), todos BUILD SUCCESSFUL. El mock del slice solo acredita transporte y traducción HTTP, no validación real del Service.
- [X] T021 [US1] En `OrderControllerTest.java`, verificar que el cuerpo de error de `INSUFFICIENT_STOCK` e `INSUFFICIENT_CREDITS` no expone stock disponible, saldo disponible, campos equivalentes de existencias ni el importe requerido (D5), conservando `ErrorResponseDTO` y los códigos vigentes (SC-014: ausencia de revelaciones prohibidas)
- [X] T022 [US1] En `OrderControllerTest.java`, verificar que un rechazo propio del Controller no invoca el Service y que el acceso sin sesión responde 401

### Tests del caso de uso — categoría Service (PostgreSQL real, Testcontainers)

**Frontera obligatoria** (`docs/backend/testing.md` §5 y §6): estas pruebas afirman **solo lo que devuelve o lanza la API pública** de `BuyOrderService`. No acceden a repositorios, JPA, `EntityManager`, JDBC, SQL ni base de datos, ni siquiera para preparación o assertions. `BuyOrderService` no expone saldos, tenencias ni listado de auditoría, así que esas afirmaciones **no pueden** hacerse aquí: viven en la categoría Repository.

- [X] T023 [US1] Compra exitosa en `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java`: devuelve los seis campos de FR-018 con `unitPrice`, `totalAmount` y `quantity` exactos, y una segunda llamada idéntica devuelve otro `transactionId` (D1, SC-001, SC-016)
- [X] T024 [US1] En `BuyOrderServiceTest.java`, cada rechazo lanza la excepción propia que corresponde a su código: créditos insuficientes, stock insuficiente, cantidad inválida, elegibilidad y cotización ausente o no positiva, comprobando el tipo de excepción sin observar la base de datos
- [X] T025 [US1] En `BuyOrderServiceTest.java`, el fallo al registrar la auditoría se propaga como fallo interno y la llamada no devuelve compra confirmada; el estado durable lo asume T055 en `MarketDomainPersistenceTest.java`
- [X] T026 [US1] En `BuyOrderServiceTest.java`, cotización con `playerId` distinto al solicitado lanza la excepción de incumplimiento del contrato de cotización y ninguna compra se completa
- [X] T027 [US1] En `BuyOrderServiceTest.java`, `strategyVersion` ausente se admite sin error y la compra se confirma; la persistencia del `null` la asume T062 en `MarketDomainPersistenceTest.java`
- [X] T028 [US1] En `BuyOrderServiceTest.java`, un usuario persistido como `SUPERUSER` es rechazado por autorización antes de resolver jugador o cotización
- [X] T029 [US1] En `BuyOrderServiceTest.java`, un `playerId` en `0` o negativo lanza `PlayerNotFoundException`, mientras que un jugador existente no visible lanza `PlayerNotVisibleException`: son excepciones distintas para dos 404 distintos (FR-006)

### Tests de persistencia — categoría Repository (PostgreSQL real)

**Por qué aquí y no en la categoría Service**: `docs/backend/testing.md` §5 prohíbe en los tests de Service acceder a Repository, JPA, `EntityManager`, JDBC, SQL o DB, *tampoco para preparación o assertions*, y §6 prohíbe que la infraestructura técnica habilite *assertions eludiendo el Service*. `BuyOrderService` no expone saldos, tenencias ni auditoría, de modo que SC-008 a SC-013 solo son observables desde la categoría Repository (§4: *constraints no demostradas adecuadamente desde Service*). Precedente del proyecto: `PlayerCatalogPersistenceTest` afirma estado durable en la categoría Repository, y `PlayerImageRunRecoveryServiceTest` lo declara en un comentario: *«el estado durable se comprueba exclusivamente en Repository»*.

Todas estas tareas viven en una única clase `backend/src/test/java/footballmarket/repositories/MarketDomainPersistenceTest.java`, con grupos `@Nested` conforme a `testing.md` §16.

- [X] T036 [US1] `@Nested Restricciones` en `MarketDomainPersistenceTest.java`: PostgreSQL rechaza saldo negativo, cantidad negativa, emisión con `initial_quantity` distinta de 100, `total_amount` incoherente con `quantity * unit_price` y tenencia duplicada por `UNIQUE (player_id, owner_user_id)`
- [X] T030 [US1] `@Nested InicializacionIdempotente` en `MarketDomainPersistenceTest.java`, escenario de créditos (AC10, SC-013): usuario normal creado por el Service con 10000 centésimas; consume saldo mediante una compra; al reejecutar la inicialización el saldo no se restaura y no aparece una segunda fila en `user_credits`. **Aclaración de trazabilidad, 2026-10-17**: la redacción original afirma que el usuario se crea «por el Service», pero en esta prueba el comprador lo aporta `MarketTestData.registerBuyer()`, que invoca `UserCredit.forUser` directamente. La creación por el camino productivo y la atomicidad de AC18 se cubren en `@Nested AltaDeUsuarioYSaldoInicial` (T076); la inicialización y la no restauración de saldos, que son el objeto real de esta tarea, mantienen sus aserciones. Pendiente de validación humana.
- [X] T031 [US1] Emisión productiva (AC17, SC-012) en `backend/src/test/java/footballmarket/repositories/PlayerTokenEmissionPersistenceTest.java`: `PlayerCatalogService` real crea el jugador y emite 100 tokens al SUPERUSER; repetir el snapshot conserva identidad, emisión y tenencia inicial. Tras comprar 10 tokens mediante `BuyOrderService` real, resolver la misma identidad con nacionalidad actualizada conserva emisión, inventario 90/10, cuentas y auditoría mediante comparación JDBC de filas completas y timestamps. H05 resuelto y verificado por el usuario para estos escenarios: `./gradlew test --tests '*PlayerTokenEmissionPersistenceTest'` BUILD SUCCESSFUL (42 s), `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (35 s) y `./gradlew test` BUILD SUCCESSFUL (3 min 26 s); `spotlessCheck` inicial detectó una diferencia de formato en Javadoc, corregida mediante `./gradlew spotlessApply` BUILD SUCCESSFUL (1 s), y `./gradlew spotlessCheck` final BUILD SUCCESSFUL (1 s). Sustituye `reejecutarEmisionNoReponeInventario()` de `MarketDomainPersistenceTest`, que solo repetía el fixture; se conserva la prueba independiente del backfill. No acredita cobertura concurrente ni recuperación de emisión omitida.
- [X] T032 [US1] En `@Nested InicializacionIdempotente`, reejecutar las sentencias de backfill de `V9` sobre saldos e inventarios existentes no los sobrescribe. No reejecutar la migración versionada mediante Flyway
- [X] T033 [US1] `@Nested Concurrencia` en `MarketDomainPersistenceTest.java`, compras simultáneas sobre el último token (AC08, SC-011): exactamente una compra confirma; no se venden más tokens de los disponibles; la suma de `player_tokens` del jugador permanece en 100; ningún saldo queda negativo; el total de créditos transferidos se conserva; cada compra confirmada deja exactamente una fila en `market_transactions`; los rechazos no generan fila; y los estados antes/después auditados corresponden a operaciones efectivamente confirmadas. Conexiones y transacciones independientes, orden determinista de locks, sin reintentos y sin exigir ausencia de deadlocks. Reabierta el 2026-10-17 por el hallazgo H08: la cobertura prometida no estaba completa. La barrera de liberación simultánea y la clasificación por tipo de excepción se implementan en T067; la conservación de créditos y la correspondencia de la auditoría, en T068. Verificada por el usuario el 2026-10-17: `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (39 s), `./gradlew test` BUILD SUCCESSFUL (5 min 5 s), y cinco ejecuciones forzadas con `./gradlew test --tests '*MarketDomainPersistenceTest' --rerun-tasks` BUILD SUCCESSFUL (51 s, 43 s, 47 s, 47 s, 47 s). `spotlessCheck` detectó una diferencia de formato, corregida con `./gradlew spotlessApply` BUILD SUCCESSFUL (2 s) y verificada de nuevo con `./gradlew spotlessCheck` BUILD SUCCESSFUL (2 s). No se acredita contención efectiva de bloqueos: ver T068.
- [X] T034 [US1] En `@Nested Concurrencia`, doble gasto (AC09): un comprador con saldo para una sola compra sobre dos jugadores distintos; solo una confirma, sin saldo negativo, con las mismas siete verificaciones de SC-011 de T033. Reabierta el 2026-10-17 por el hallazgo H08: el escenario no afirmaba nada sobre `market_transactions` ni sobre el saldo del vendedor. Implementado en T068. Verificada por el usuario el 2026-10-17 con la misma evidencia de ejecución que T033. Durante esa validación la primera corrida detectó que la conservación de SC-008 comparaba la suma final contra el saldo individual del vendedor en lugar de la suma previa de ambas partes; se corrigió la referencia, no la expectativa.
- [X] T035 [US1] En `@Nested Concurrencia`, creación concurrente de la primera tenencia por dos compradores: no aparecen filas duplicadas y la suma de `player_tokens` del jugador permanece en 100
- [X] T055 [US1] `@Nested RejectionsAndRollback` en `MarketDomainPersistenceTest.java`: una compra rechazada por créditos, stock, cantidad, elegibilidad o cotización no modifica `user_credits`, no crea ni modifica `player_tokens` y no inserta fila en `market_transactions`; y un fallo al persistir la auditoría revierte la compra completa dejando saldos, tenencias y ausencia de registro intactos. El estado se comprueba **después** de que la transacción fallida concluye (SC-009, SC-010). Cobertura de auditoría H04 resuelta y verificada por el usuario en `@Nested AuditPersistenceRollback`: escenarios con y sin tenencia previa, rechazo PostgreSQL SQLSTATE 22001 por versión de estrategia de 65 caracteres, comparación JDBC de filas completas después del rollback y control positivo con 64 caracteres. Resultados humanos de esa etapa: `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (43 s); `./gradlew test` BUILD SUCCESSFUL (3 min 38 s); `spotlessApply` BUILD SUCCESSFUL (1 s) y `spotlessCheck` BUILD SUCCESSFUL (1 s). Reabierta el 2026-10-17 por H08 y ampliada el 2026-10-17 con los subcasos de cantidad (0 y 101), elegibilidad, cotización ausente y precio de cotización inválido (AC15), todos con aserciones duraderas de créditos, tenencias, conservación de tokens y ausencia de auditoría. **Verificada por el usuario el 2026-10-17**: `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (46 s) con `@Nested Rechazos y reversión` en 8 tests, 0 failures; `./gradlew test` BUILD SUCCESSFUL (5 min 33 s) con 646 tests, 0 failures, 0 errors, 0 skipped; `spotlessCheck` detectó una diferencia de formato, corregida con `./gradlew spotlessApply` BUILD SUCCESSFUL (2 s) y verificada con `./gradlew spotlessCheck` BUILD SUCCESSFUL (2 s). Dos ajustesSingleton durante esa validación fueron defectos de los propios tests, no del código productivo: el estado previo de la tenencia del comprador se observaba como cantidad cuando la primera compra aún no la había creado, y se corrigió a `Optional<PlayerToken>` para distinguir ausencia de cero; y una contraseña de prueba de 6 caracteres violaba la longitud mínima del dominio y hacía fallar el alta antes de tocar la base.
- [X] T056 [US1] `@Nested VendedorAusente` en `MarketDomainPersistenceTest.java`: sin superusuario persistido, la compra falla con la excepción técnica de vendedor ausente y no deja débitos, tenencias creadas o modificadas ni fila de auditoría (FR-023, AC19, SC-010; integridad transaccional OBLIGATORIA)
- [X] T062 [US1] `@Nested CotizacionPersistida` en `MarketDomainPersistenceTest.java`, cerrando las filas «Registro del precio aplicado» y «versión de estrategia» de `contracts/quote-source.md`: la fila de `market_transactions` guarda `unit_price` y `quote_calculated_at` iguales a los del snapshot aplicado; `quote_strategy_version` conserva la versión recibida cuando la fuente la aporta y queda en `null` cuando la fuente omite la estrategia. Asume el diferimiento de T027 para que no quede verificación colgante

### Implementación: integraciones con componentes existentes

- [X] T037 [US1] Modificar `backend/src/main/java/footballmarket/services/impl/AuthenticationServiceImpl.java` para crear la cuenta de créditos del usuario (10000 para USER) dentro de la misma transacción de registro, con inserción idempotente (FR-015, AC18)
- [X] T038 [P] [US1] Modificar `backend/src/main/java/footballmarket/services/impl/SuperuserProvisioningServiceImpl.java` para crear la cuenta de créditos del superusuario con saldo 0 en su propia transacción, sin sobrescribir una existente (FR-015, D2)
- [X] T039 [US1] Modificar `backend/src/main/java/footballmarket/services/internal/PlayerResolution.java` para registrar la emisión de 100 tokens y crear la tenencia del superusuario en la transacción que crea cada jugador, con inserción idempotente (FR-016, AC17)
- [X] T040 [US1] En `PlayerResolution.java`, verificar que actualizar, reactivar o volver a sincronizar un jugador existente no vuelve a emitir ni repone inventario

### Implementación: excepciones y manejo global

- [X] T041 [US1] Crear en `backend/src/main/java/footballmarket/services/exceptions/` las excepciones de dominio de la compra heredando de la existente `DomainException` (`backend/src/main/java/footballmarket/exceptions/DomainException.java`, no se modifica): superusuario no puede comprar, jugador no visible, cantidad inválida, stock insuficiente, créditos insuficientes, cotización no disponible y precio no positivo. Reutilizar la existente `PlayerNotFoundException` para `PLAYER_NOT_FOUND` y no duplicarla (G4, G10)
- [X] T042 [P] [US1] Crear en `backend/src/main/java/footballmarket/services/exceptions/` las excepciones técnicas de la compra heredando de la existente `ApplicationException` (`backend/src/main/java/footballmarket/exceptions/ApplicationException.java`, no se modifica): `QuoteContractViolationException`, `SellerUnavailableException` y `ArithmeticOverflowException`, todas ellas con respuesta pública 500 `INTERNAL_ERROR` genérico (G4)
- [X] T043 [US1] Extender `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` con los handlers de los códigos nuevos, el de body ilegible y el de fallo interno genérico, sin exponer excepciones, trazas ni identificadores técnicos

### Implementación: fachada de compra

- [X] T044 [US1] Crear `backend/src/main/java/footballmarket/services/BuyOrderService.java` con la interfaz de aplicación del caso de uso de compra
- [X] T045 [US1] Crear `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` con una única transacción: resolver comprador por el sujeto autenticado mediante `UserRepository`, validar rol y cantidad, resolver elegibilidad, leer y validar la cotización, bloquear cuentas y tenencias en orden determinista, revalidar saldo y stock, transferir y registrar auditoría
- [X] T046 [US1] En `BuyOrderServiceImpl`, resolver la elegibilidad directamente con `PlayerRepository` sin invocar `PlayerCatalogService` (G1): `findById(playerId)` para existencia — `PLAYER_NOT_FOUND` si está vacío, incluidos valores `0` y negativos; si existe, `findVisibleById(playerId)` reutilizando el predicado vigente del catálogo — `PLAYER_NOT_VISIBLE` si está vacío; continuar si ambos resuelven. No crear métodos de consulta nuevos ni duplicar el predicado de visibilidad
- [X] T047 [US1] En `BuyOrderServiceImpl`, aplicar la regla de lectura bajo bloqueo: saldo y stock se obtienen exclusivamente de las consultas de bloqueo, sin reutilizar entidades cargadas previamente, y una cuenta de créditos ausente no se crea durante la compra
- [X] T048 [US1] En `BuyOrderServiceImpl`, calcular el importe una sola vez con `Math.multiplyExact` y revertir con 500 genérico ante desbordamiento, sin exponer identificadores técnicos (FR-022, D4)
- [X] T049 [US1] En `BuyOrderServiceImpl`, leer la cotización una sola vez, verificar que `CurrentQuote.playerId` coincide con el solicitado antes de usar el precio, validar que el precio es mayor que cero y conservar en la auditoría el precio aplicado y su procedencia
- [X] T057 [US1] En `BuyOrderServiceImpl`, comprobar la existencia del superusuario con `UserRepository.findByRole(UserRole.SUPERUSER)` tras resolver el comprador y **antes** de cualquier bloqueo o movimiento de recursos; si no resuelve, lanzar `SellerUnavailableException` para que la operación revierta completa y sin compra parcial (FR-023). No invocar `SuperuserProvisioningService`: introduciría una dependencia Service → Service (`plan.md` §Vendedor ausente)

### Tests de seguridad — categoría Security

`testing.md` §10 ubica estos tests en `security/`. El token emitido por `JWTProvider` solo contiene `subject`, `issuedAt` y `expiration`: **no existe claim de rol**, de modo que la autorización solo puede provenir del rol persistido.

- [X] T058 [US1] Verificación del origen de la autorización en `backend/src/test/java/footballmarket/security/BuyOrderAuthorizationTest.java`: con la cadena de seguridad real, un usuario persistido como `SUPERUSER` recibe 403 y uno persistido como `USER` completa la compra, demostrando que la decisión se toma sobre el rol persistido y no sobre información del token (FR-003, Constitution §3, `research.md` R-02)

### Tests de configuración de la fuente de cotización — categoría Config

Cubren las dos filas de la tabla de verificación de `contracts/quote-source.md` que la categoría Repository y Service no pueden afirmar. Reutilizan los patrones ya existentes en `backend/src/test/java/footballmarket/config/`: `ApplicationContextRunner` con `withUserConfiguration(...)`, `withPropertyValues(...)` y `assertThat(started).hasFailed()`, como en `PlayerImageStartupConfigurationTest`. Conforme a `testing.md` §12, **no** usan `@ActiveProfiles("test")`: la categoría Config con propiedades explícitas no lo lleva. Sin base de datos ni Docker.

- [X] T059 [US1] `@Nested SeleccionDeFuente` en `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java`: con una implementación real de `QuoteSource` presente **y** `market.quote.simulator.enabled=true`, el contexto arranca con una sola implementación y esa es la real; el `SimulatedQuoteSource` **no se registra** aunque su propiedad esté habilitada (regla obligatoria del contrato)
- [X] T060 [US1] `@Nested ArranqueSinFuente` en `QuoteSimulationConfigurationTest.java`: sin ninguna implementación de `QuoteSource` y con `market.quote.simulator.enabled=false`, el arranque **falla** con error de configuración explícito; no hay precio de respaldo ni compra posible
- [X] T061 [US1] `@Nested SimuladorHabilitado` en `QuoteSimulationConfigurationTest.java`: sin fuente real y con `market.quote.simulator.enabled=true`, el simulador **se registra** como única implementación de `QuoteSource` y su precio queda **bound** desde `market.quote.simulator.price`. El comportamiento del componente —precio devuelto e identificación de estrategia simulada— lo afirma T063 en la categoría Integration
- [X] T063 [US1] Categoría Integration en `backend/src/test/java/footballmarket/integrations/SimulatedQuoteSourceTest.java`: `SimulatedQuoteSource` devuelve un `CurrentQuote` cuyo `playerId` es el solicitado, cuyo `price` es el configurado en centésimas y cuya estrategia identifica la cotización como simulada, sin crear tablas ni datos persistentes. Test puro de Integration autocontenida: instancia directa, sin Spring, sin `@ActiveProfiles("test")` y sin base de datos, conforme a `testing.md` §2 y §12, reutilizando el patrón de `TheSportsDbRequestPacerTest`

### Implementación: contrato HTTP

- [X] T050 [P] [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/OrderBuyRequestDTO.java` como `record` con `playerId` y `quantity` requeridos
- [X] T051 [P] [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/OrderBuyResponseDTO.java` como `record` con `transactionId`, `playerId`, `quantity`, `unitPrice`, `totalAmount`, `executedAt`
- [X] T052 [US1] Crear `backend/src/main/java/footballmarket/controllers/mappers/OrderMapper.java` como mapper `final` con métodos estáticos, sin negocio ni cálculos
- [X] T053 [US1] Crear `backend/src/main/java/footballmarket/controllers/OrderController.java` con `POST /api/orders/buy`, validación de entrada y anotaciones OpenAPI completas del contrato vigente (FR-020)

- [X] T064 [US1] Remediación H06 autorizada: recuperar emisiones pendientes al aprovisionar SUPERUSER mediante `SuperuserProvisioningServiceImpl` y `PlayerTokenEmissionRepository`, en todas las ramas exitosas. Incluir inactivos; rechazar inconsistencias sin reparación; CTE con RETURNING que crea tenencias solo para emisiones nuevas dentro de la misma transacción, sin reponer inventario vendido ni alterar créditos o auditorías existentes. Implementado con pruebas A–G y rama deshabilitada en `SuperuserMarketRecoveryPersistenceTest`. H06 verificado por el usuario para el flujo secuencial aprobado: `./gradlew test --tests '*SuperuserMarketRecoveryPersistenceTest'` BUILD SUCCESSFUL (1 min 58 s), `./gradlew test --tests '*SuperuserProvisioningServiceTest'` BUILD SUCCESSFUL (54 s), `./gradlew test --tests '*PlayerTokenEmissionPersistenceTest'` BUILD SUCCESSFUL (45 s), `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (37 s) y `./gradlew test` BUILD SUCCESSFUL (5 min 22 s). `spotlessCheck` inicial detectó diferencias de formato, corregidas mediante `./gradlew spotlessApply` BUILD SUCCESSFUL (1 s), y `./gradlew spotlessCheck` final BUILD SUCCESSFUL (1 s). Constraint de prueba acotada en Testcontainers con limpieza `finally`. Precondición: aprovisionamiento antes de sincronización, sin sincronizaciones concurrentes con aprovisionamientos manuales. Esta validación no acredita concurrencia multiinstancia ni sincronización simultánea con aprovisionamiento.

- [X] T065 [US1] Remediación H07 autorizada y verificada: `QuoteSimulationConfiguration` es `@AutoConfiguration` descubierta por `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, sin escaneo prematuro (`@SpringBootApplication` con `AutoConfigurationExcludeFilter`) ni importación como configuración de usuario. Causa confirmada por ejecución humana: alternativa registrada después y simulador habilitado dejaban dos fuentes `QuoteSource`. Conservados `@ConditionalOnMissingBean(QuoteSource.class)`, `@ConditionalOnProperty`, precio por defecto de 100, arranque fallido sin fuente y ausencia de fallback en `BuyOrderServiceImpl`. Regresiones para ambos órdenes y ambas habilitaciones, consumidor con dependencia obligatoria, ausencia de alternativa, discovery real mediante `@EnableAutoConfiguration` y exclusión del escaneo ordinario. H07 verificado por el usuario: `./gradlew test --tests '*QuoteSimulationConfigurationTest'` BUILD SUCCESSFUL (11 s), `./gradlew test --tests '*BuyOrderServiceTest'` BUILD SUCCESSFUL (39 s), `./gradlew test` BUILD SUCCESSFUL (4 min 57 s), `./gradlew spotlessCheck` BUILD SUCCESSFUL (6 s), `./gradlew spotlessApply` BUILD SUCCESSFUL (1 s) y `./gradlew spotlessCheck` final BUILD SUCCESSFUL (1 s). Fuera de alcance: precedencia entre múltiples fuentes reales, proveedores reales inexistentes, restricciones por perfil y cambios en `BuyOrderServiceImpl`.

**Checkpoint**: US1 funcional y verificable de forma independiente. Requiere autorización humana de IMPLEMENT; este archivo no la habilita.

---

## Fase 4: Polish

- [X] T054 Actualizar la documentación de `specs/010-buy-tokens/` si la implementación introduce desviaciones respecto de `plan.md`, `research.md`, `data-model.md` y `contracts/`

---

## Dependencias y orden de ejecución

### Dependencias de fase

- **Fase 1**: sin dependencias
- **Fase 2**: depende de Fase 1; BLOQUEA US1; no depende de US1
- **Fase 3**: depende de Fase 2 completa
- **Fase 4**: depende de Fase 3

### Dependencias internas de Fase 2

- T003 → T004 → T005 (mismo archivo, secuencial)
- T006–T009 entre sí, todas antes de T010–T013
- T010–T013 entre sí, cada una dependiente del modelo que declara (`UserCredit`, `PlayerToken`, `PlayerTokenEmission`, `MarketTransaction`); al depender de tareas pendientes de la misma fase, ninguna lleva `[P]`
- T014 → T015 → T016 → T017 (el puerto referencia el record; la configuración referencia la implementación)

### Dependencias internas de US1

- Modelos y repositorios antes de la fachada
- T041/T042 antes de T043 (el handler mapea las excepciones)
- T039 antes de T040 (mismo archivo, secuencial)
- T044 antes de T045
- T045 antes de T046–T049, T057 (mismo archivo, secuencial)
- T053 después de T050–T052
- T036 antes de T030–T035, T055, T056, T062: todas comparten `MarketDomainPersistenceTest.java`, así que son secuenciales entre sí por escritura
- T023–T029 (`BuyOrderServiceTest.java`), T030–T036, T055, T056 y T062 (`MarketDomainPersistenceTest.java`), T059–T061 (`QuoteSimulationConfigurationTest.java`) y T063 (`SimulatedQuoteSourceTest.java`) viven en archivos distintos y pueden escribirse en paralelo entre sí; T058 va en `security/` y también es independiente de archivo
- T062 asume la verificación de `quote_strategy_version` que T027 difiere; ambos quedan enlazados y ninguno queda colgante
- T059–T061 dependen de T016 y T017: la configuración solo puede probarse cuando `SimulatedQuoteSource` y `QuoteSimulationConfiguration` existen (Foundational)
- T063 depende de T015 y T016: solo puede probarse cuando el puerto y el simulador existen (Foundational). Es la tarea que asume el comportamiento que T061 deja de afirmar
- `SellerUnavailableException` (T042) debe existir antes que T045 la use y antes que T043 la mapee al 500 genérico

### Numeración de T055 a T063

`T055` a `T063` se **añadieron** en remediaciones posteriores a Analyze y conservan esa posición para no renumerar las tareas preexistentes y preservar su trazabilidad. Su ubicación lógica está dentro de su grupo correspondiente; el orden de ejecución es el que indican las dependencias anteriores.

### Oportunidades de paralelismo

- T002, T006–T009, T014, T018, T019, T038, T042, T050 y T051 están marcadas `[P]` (12 en total)
- Ninguna pareja de tareas `[P]` escribe el mismo archivo
- `[P]` significa paralelismo dentro del conjunto marcado: una tarea `[P]` no depende de otra tarea `[P]` inconclusa. Criterio aplicado:
  - **Válido** si solo depende de artefactos de fases anteriores ya completadas (caso de `T018`/`T019` sobre los modelos de Foundational) o de clases preexistentes en el repositorio (caso de `T042` sobre `ApplicationException`).
  - **Inválido** si depende de una tarea pendiente de su misma fase: el repositorio no puede compilar sin la entidad que declara (`T011`–`T013` sobre `T007`–`T009`), y el propio template marca sin `[P]` la tarea que depende de otras dos previas, `T014 … (depends on T012, T013)`.
- No hay historias paralelizables entre sí: solo existe US1

---

## Ejemplo de ejecución paralela

```bash
# Modelos en paralelo (archivos distintos):
Task: "Crear UserCredit en backend/src/main/java/footballmarket/models/UserCredit.java"
Task: "Crear PlayerToken en backend/src/main/java/footballmarket/models/PlayerToken.java"

# Integraciones con componentes existentes en paralelo:
Task: "Modificar AuthenticationServiceImpl para crear la cuenta de créditos"
Task: "Modificar SuperuserProvisioningServiceImpl para crear la cuenta del superusuario"
```

---

## Estrategia de implementación

### MVP (solo US1)

1. Fase 1: verificaciones
2. Fase 2: base (bloquea)
3. Fase 3: US1
4. Detenerse: Analyze vigente, autorización humana, tests a cargo del usuario

### Fuera de alcance de Tasks (no bloquean)

| Elemento | Clasificación | Motivo |
|---|---|---|
| Prueba de compra concurrente con desactivación del jugador | **Recomendada** | D3 opción A: la compra puede completarse si el jugador se desactiva tras validar visibilidad. No es obligatoria ni bloqueante |
| Reparación de corrupción manual | Opcional | Fuera de las operaciones normales |
| Resistencia de auditoría ante operadores privilegiados | Opcional | La garantía aprobada es de comportamiento de aplicación |

---

## Notas

- Conservar IDs y avance al reanudar
- No modificar los marcadores de `checklists/requirements-quality.md`; pertenecen al revisor
- No agregar tareas de ejecución de tests, builds ni migraciones: corresponden al usuario
- Detenerse en cada checkpoint para validar la historia de forma independiente
- Evitar tareas vagas, conflictos de archivo y dependencias entre historias

---

## Fase 5: Convergence

Diagnóstico `/speckit.converge` append-only. No modifica `spec.md` ni `plan.md`, no renumera tareas previas y no ejecuta correcciones. Todas las tareas siguientes requieren autorización humana de IMPLEMENT antes de ejecutarse.

- [X] T066 [US1] Completar los subcasos de rechazo que T055 sigue pendientes: en `@Nested RechazosAndRollback` de `MarketDomainPersistenceTest.java`, demostrar que una compra rechazada por cantidad inválida, por elegibilidad del jugador y por cotización ausente o inválida no modifica `user_credits`, no crea ni modifica `player_tokens` y no inserta fila en `market_transactions`, afirmando el estado después de que la transacción fallida conclude (T055, SC-009, SC-010; partial). Cerrar T055 solo cuando la tarea compuesta quede íntegramente verificada por el usuario. Texto original conservado sin reescribir; solo se corrige el nombre del grupo anidado a `RejectionsAndRollback` conforme a T072. **Cerrada sin implementación propia por duplicación**: se creó durante `/speckit.converge` del 2026-10-17 y solapó el alcance que T055 ya declaraba pendiente sobre sí misma. La autoridad de esa cobertura es T055, que sigue abierta hasta la validación humana. No se afirma que T066 tuviera ejecución, revisión ni verificación propias.

- [X] T067 [US1] Sustituir en `@Nested Concurrency` de `MarketDomainPersistenceTest.java` la clasificación de resultados por `getClass().getSimpleName()` por captura explícita de `InsufficientStockException` e `InsufficientCreditsException`, reintentando cualquier otra excepción, de modo que una excepción homónima distinta no pueda satisfacer la expectativa (AC08, AC09, SC-011, T033, T034; partial). Implementado el 2026-10-17 con un enum `Outcome` declarado en la clase externa, porque un enum no puede ser miembro de una clase interna no estática: `attempt` captura los dos tipos del contrato y propaga cualquier otra excepción sin convertirla en resultado. Sustituye a la comparación por subcadena del nombre. Verificada por el usuario el 2026-10-17 con la misma evidencia de ejecución que T033.

- [X] T068 [US1] Añadir en `@Nested Concurrency` de `MarketDomainPersistenceTest.java` una barrera de inicio que libere ambas transacciones simultáneamente, sin `sleep` ni serialización artificial, junto con las aserciones ausentes de conservación de la suma de créditos de comprador y vendedor y de correspondencia entre las filas de `market_transactions` y la operación confirmada; en el escenario de jugadores distintos añadir además las aserciones de auditoría y de saldo del vendedor (AC08, AC09, SC-008, SC-010, SC-011, plan.md:543, T033, T034; partial). Implementado el 2026-10-17 con `CyclicBarrier` de dos partes esperada dentro de la transacción antes de invocar `buyOrderService.buy`, con esperas acotadas de 15 s en la barrera y 30 s en los futuros, sin `Thread.sleep`. La liberación simultánea no demuestra contención efectiva de bloqueos PostgreSQL y así queda documentado en el Javadoc de `race`; comprobar esa contención exigiría observar el bloqueo desde fuera y queda fuera de alcance. Se añadieron `onlyNewAudit`, `totalCreditsOf` y `auditOfBoth` para afirmar conservación y auditoría. Verificada por el usuario el 2026-10-17 con la misma evidencia de ejecución que T033; el XML resultante declara 4 tests, 0 failures, 0 errors y 0 skipped en `@Nested Concurrency`, sin suposiciones ni ramas que puedan omitir aserciones.

- [X] T069 [US1] Someter a decisión humana el conflicto entre `backend/src/test/java/footballmarket/support/MarketTestData.java`, que expone repositorios de persistencia para preparar compradores, catálogo, inventario y saldos, y `docs/backend/testing.md` §6, que prohíbe habilitar esa preparación funcional desde infraestructura técnica; registrar la decisión en el artefacto afectado sin conceder excepciones a una norma superior (docs/backend/testing.md §6, Constitution §1; contradicts). No ejecutar esta tarea sin autorización explícita del enfoque a adoptar. **Resuelto por enmienda normativa, no por refactor.** El usuario autorizó expresamente el 2026-10-17 elevar `docs/backend/testing.md` a la versión 2.0.0 para admitir que un test de categoría Service prepare sus precondiciones persistentes en `Arrange` mediante un helper compartido que acceda a repositorios. Condiciones acumulativas y obligatorias: Act y Assert usan solo APIs públicas productivas del Service; el helper no ejecuta la operación bajo prueba; el helper no contiene assertions funcionales; no habilita usar repositorios para verificar resultados; Repository y Security conservan íntegra la prohibición. `MarketTestData` se conserva sin cambios y no se refactoriza `BuyOrderServiceTest`. Justificación, impacto y dependencias constan en el Sync Impact Report de `docs/backend/testing.md`; Constitution §2.2 no se invoca porque gobierna el alcance de las SPEC, no la calidad de su implementación. La brecha de evidencia de AC18 que este hallazgo destapó se trata en T076. **Verificado por el usuario el 2026-10-17**: `./gradlew test` BUILD SUCCESSFUL (5 min 33 s), 646 tests sin fallos; el helper `MarketTestData` se conserva sin cambios y `BuyOrderServiceTest` no se refactorizó, conforme a la autorización.
- [X] T070 [US1] Cerrar el invariante de `PlayerToken.transferTo` en `backend/src/main/java/footballmarket/models/PlayerToken.java`: o bien exigir que emisor y receptor correspondan al mismo jugador, o bien corregir el Javadoc que afirma conservar la suma del jugador sin esa condición, conservando la selección actual de filas por el mismo `playerId` en `BuyOrderServiceImpl` (FR-017; partial). **Corregido el invariante**, que era compatible con el dominio: `transferTo` exige ahora que emisor y receptor pertenezcan al mismo jugador y lanza `TokenPlayerMismatchException` antes de mover cantidad. El comportamiento válido de compra no cambia, porque `lockHoldings` resuelve ambas tenencias del mismo `player_id`; dos tenencias cuyo jugador carece de identificador se tratan como del mismo jugador porque el modelo no puede afirmar una contradicción que el persistido aún no permite comprobar. Javadocs corregidos y regresión en `@Nested Transfer` de `PlayerTokenTest`. Implementado el 2026-10-17; pendiente de validación humana.
- [X] T071 [US1] Renombrar el identificador `MENSAJE_CANTIDAD_NO_ENTERA` de `backend/src/main/java/footballmarket/controllers/dtos/requests/StrictIntegerDeserializer.java` a inglés, conservando sin cambios el texto del mensaje de validación, que sigue en español (Constitution §5; contradicts). Renombrado a `NON_INTEGER_QUANTITY_MESSAGE`. El texto de validación y el contrato externo no cambian. Implementado el 2026-10-17; pendiente de validación humana.
- [X] T072 [US1] Alinear `plan.md` y T055 con el nombre real del grupo anidado de persistencia, `RejectionsAndRollback`, o renombrar el grupo para que coincida con la denominación de los artefactos; una sola fuente de verdad para el nombre (plan.md:541, T055; partial). Alineados `plan.md`, T055 y T066 al nombre real `RejectionsAndRollback`; el código no se renombra. Implementado el 2026-10-17; pendiente de validación humana.
- [X] T073 [US1] Reconciliar en `backend/src/test/java/footballmarket/controllers/OrderControllerTest.java` la afirmación del comentario de `assertNoCuantidadRevelada` con lo que la aserción realmente comprueba, o ampliar la aserción a los seis campos exactos del contrato global si esa era la intención (FR-019, SC-014, T021; partial). **Reforzada la aserción**, que era la mejor de las dos vías: el helper lee el JSON real de la respuesta con Jackson y compara el conjunto de campos con los seis declarados en `ErrorResponseDTO`, de modo que un campo nuevo o un campo de más fallen la prueba en lugar de pasar inadvertido. La prohibición de nombres que revelan saldo o cantidad y la ausencia de cifras en el mensaje se conservan, y el comentario ahora describe lo que la aserción demuestra. Implementado el 2026-10-17; pendiente de validación humana.
- [X] T074 [US1] Reconciliar en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` el comentario de `handleInternalOrderFailure`, que afirma conservar el detalle técnico en un registro interno de diagnóstico que el método no produce, con el comportamiento real; registrar el diagnóstico o corregir la afirmación, sin exponer detalle técnico en la respuesta pública (partial). **Corregido el comentario**, sin añadir logging: la causa queda para los registros de la aplicación y el análisis de fallos, no en el cuerpo de la respuesta. Constitution §3 no obliga a registrar, de modo que no se introduce dependencia ni comportamiento nuevo. Implementado el 2026-10-17; pendiente de validación humana.
- [X] T075 [EPIC] Actualizar el campo `Status` de `specs/010-buy-tokens/spec.md`, hoy en `Draft`, para que refleje la fase real de la feature, sin implicar avance de gates ni cierre, y registrar la fase vigente en la tarjeta de la épica conforme a Constitution §8 (Constitution §8; partial). Requiere autorización de trazabilidad, no de implementación. Aplicado el 2026-10-17 en `spec.md`: `Status: Implementada — pendiente de gates CODE REVIEW, TESTING y FEATURE COMPLETION`. La SPEC no se declara finalizada ni cerrada. La sincronización de la tarjeta de la épica en Trello queda pendiente y no se ejecuta en esta intervención. Pendiente de validación humana.
- [X] T076 [US1] `@Nested AltaDeUsuarioYSaldoInicial` en `MarketDomainPersistenceTest.java`, categoría Repository: cubrir AC10 y AC18 por el camino productivo. Registrar el usuario con `AuthenticationService.register` —no con `MarketTestData`— y afirmar que queda persistido con rol USER, que su cuenta de créditos existe con `UserCredit.INITIAL_USER_CREDITS` y que hay exactamente una fila en `user_credits`. Con una restricción `CHECK` temporal (`user_id < 0`) sobre el PostgreSQL desechable de Testcontainers, comprobar que un rechazo real SQLSTATE 23514 al insertar la cuenta revierte el alta completa: sin usuario persistido y sin fila de créditos, afirmado con consultas nuevas en autocommit una vez concluida la transacción fallida. Retirar la restricción en `finally` y comprobar que `pg_constraint` ya no la contiene. No usar mocks de persistencia (AC18, FR-015, SC-013; missing). Implementado el 2026-10-17, con el mismo precedente de restricción temporal y limpieza de `SuperuserMarketRecoveryPersistenceTest`. Cubre el hueco de evidencia que destapó H09. **Verificado por el usuario el 2026-10-17**: `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (46 s) con `@Nested Alta de usuario y saldo inicial` en 2 tests, 0 failures; `./gradlew test` BUILD SUCCESSFUL (5 min 33 s) con 646 tests, 0 failures, 0 errors, 0 skipped; `spotlessApply` BUILD SUCCESSFUL (2 s) y `spotlessCheck` BUILD SUCCESSFUL (2 s). La restricción se declara `NOT VALID` porque la tabla ya contiene saldos válidos: sin esa cláusula el propio `ALTER` fallaría contra las filas existentes, y `NOT VALID` omite validarlas mientras sigue rechazando toda inserción posterior.
- [X] T077 [US1] Corregir la descripción de T030, que afirma que el usuario normal se crea «por el Service»: en `MarketDomainPersistenceTest` el comprador lo obtiene `MarketTestData`. Conservar el texto original y añadir la aclaración de que la creación por `AuthenticationService` y la atomicidad de AC18 se cubren en `@Nested AltaDeUsuarioYSaldoInicial` (T030, T076; partial). Hecho el 2026-10-17: el texto original de T030 se conserva intacto y la aclaración se añadió en la misma línea, remitiendo a T076. Sin reescritura ni renumeración.

---

## Fase 6: Convergence

CONVERGE final del 2026-10-17, ejecutado con US01 en DONE y trabajo técnico completo. Diagnóstico append-only: no modifica `spec.md` ni `plan.md`, no toca código ni tests y no ejecuta pruebas. Resultado: **no exitoso**. Se anexan dos remediaciones documentales; por la definición de DONE de Feature, su existencia impide completar FEATURE COMPLETION hasta resolverlas.

- [ ] T078 [US1] Actualizar `plan.md:515` para que refleje la excepción de Arrange que `docs/backend/testing.md` 2.0.0 introdujo el 2026-10-17. El párrafo afirma hoy la prohibición de §5 y §6 sin mencionarla, mientras `plan.md:221` ya la reconoce, de modo que el documento es internamente inconsistente y desalineado de la norma vigente (Constitution §4; partial). Sustituir las referencias por número de sección o por nombre de apartado, que no se desplazan con ediciones posteriores.

- [ ] T079 [US1] Sustituir en T068 y T072 las referencias de línea `plan.md:541` y `plan.md:543` por referencias a la sección o al apartado. La numeración de líneas de `plan.md` se desplazó al incorporar las remediaciones de H08 a H11, y hoy `plan.md:543` apunta a «Compra correcta: los seis campos de FR-018», no a la lista de grupos de la categoría Repository (partial). Aplica la misma perdurabilidad que T078.

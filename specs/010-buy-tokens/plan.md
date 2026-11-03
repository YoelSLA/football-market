# Implementation Plan: Comprar tokens de un jugador (REQ-S2-06)

**Branch**: `feat/010-buy-tokens` | **Date**: 2026-10-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/010-buy-tokens/spec.md`

---

## Summary

Implementar `POST /api/orders/buy`: un usuario autenticado y no superusuario compra entre 1 y 100 tokens de un jugador visible, pagando con créditos al precio vigente del token. La operación transfiere créditos y tokens de forma atómica y registra la compra en un registro financiero que las operaciones normales del sistema no modifican ni eliminan.

El caso de uso se resuelve con **una única fachada de aplicación** (`BuyOrderService`) que consume Model y Repository directamente, conforme a `docs/backend/architecture.md` §1.3 y §1.4. No se crean servicios financieros intermedios ni orquestadores: la compra es un solo caso de uso transaccional.

**Decisiones vigentes confirmadas en la consolidación del 2026-10-16:**

| Tema | Decisión |
|---|---|
| Unidad monetaria | **Centésimas enteras** (`long` / `BIGINT`), 1 crédito = 100 unidades. Rige CL-01, ratificada por el equipo. |
| Servicios | Solo `BuyOrderService` + `BuyOrderServiceImpl`. Sin `TokenService`, `CreditService` ni `AuditService`. |
| Cotización | Lectura a través del puerto Integration `QuoteSource`. El puerto se mantiene mientras desacople la compra; `SimulatedQuoteSource` es el componente temporal y se elimina cuando exista REQ-S2-03. |
| Emisión de tokens | Integrada en la transacción de creación del jugador dentro de la sincronización del catálogo, más inicialización única por Flyway para jugadores existentes. |
| Créditos iniciales | Creados en la misma transacción de registro del usuario y del aprovisionamiento del superusuario; Flyway inicializa usuarios preexistentes. Sin eventos. |
| Migraciones | **Una única migración `V9`**; no existen migraciones de SPEC-010 aplicadas. |
| Concurrencia | Transacción única con bloqueos pesimistas en orden determinista y revalidación bajo bloqueo. Sin reintentos automáticos. |
| Desactivación concurrente | Se valida visibilidad en el momento de la comprobación; sin bloqueo de la fila del jugador. El orden estricto compra/desactivación es **recomendado y no bloqueante** (D3 opción A). |

---

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.1.1 (backend); sin frontend en esta SPEC

**Primary Dependencies**: Spring Web MVC, Spring Data JPA, Spring Security OAuth2 Resource Server, JJWT 0.13.0, Flyway, PostgreSQL, Jakarta Validation, Lombok. Sin dependencias nuevas.

**Storage**: PostgreSQL exclusivamente. Cuatro tablas nuevas: `user_credits`, `player_tokens`, `player_token_emissions`, `market_transactions`.

**Testing**: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers con PostgreSQL 18 real. Ejecución a cargo del usuario.

**Target Platform**: Servidor Linux, JDK 21, PostgreSQL 18

**Project Type**: Backend REST API (Spring Boot)

**Performance Goals**: valores de referencia **no vinculantes**, sin umbral acordado por el equipo y sin tarea ni escenario de aceptación asociado. Se registran solo como criterio de evaluación cualitativa del equipo, no como resultado medido ni como criterio obligatorio: p95 ≤ 1 s y errores técnicos < 1 % durante 5 minutos a 1 compra/s, con datos locales, JVM caliente y recursos del entorno registrados. No se afirma que compras simultáneas no degraden el rendimiento. Conforme a Constitution §2.2 y a `spec.md`, la integridad concurrente es obligatoria y no depende de este objetivo, que no genera tarea.

**Constraints**:
- Sin dependencias Service → Service (`docs/backend/architecture.md` §1.3).
- Interfaz e implementación separadas para el Service (§1.1); colaboradoras internas sin interfaz de aplicación (§1.4).
- Importes en centésimas enteras con aritmética exacta; `CHECK` de no negatividad en base de datos.
- Sin comisiones, spreads, órdenes pendientes, reservas de créditos ni infraestructura de eventos.
- PostgreSQL es la única persistencia financiera y de auditoría; no hay duplicación de registros, outbox ni event sourcing.
- Migración única `V9__create_market_domain.sql`; no se editan migraciones aplicadas.

**Scale/Scope**: Un endpoint nuevo; cuatro tablas; una migración; un simulador de cotización temporal. Sin venta, portfolio, historial ni frontend.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Evaluación vigente — consolidación 2026-10-16

| Principio | Resultado | Fundamento |
|---|---|---|
| **§2 Calidad y alcance** | **PASS** | Una fachada, un caso de uso. Se eliminan servicios sin responsabilidad propia, eventos, reservas, reintentos y extensibilidad futura. |
| **§2.2 Proporcionalidad** | **PASS** | Casos obligatorios cubiertos; D3 y corrupción manual quedan como limitaciones o mejoras no bloqueantes. |
| **§2.1 Datos de desarrollo** | **PASS** | Sin migraciones de compatibilidad ni columnas preservadoras; la inicialización de datos preexisting es funcionalmente obligatoria (FR-015/016). |
| **§3 Seguridad** | **PASS** | Autenticación y autorización existentes reutilizadas; rol leído de la persistencia; sin datos sensibles en respuestas ni mensajes de rechazo. |
| **§4 Contratos** | **PASS** | Contrato HTTP en `contracts/buy-tokens.md`, anotaciones Springdoc y tests de Controller; códigos de error estables. |
| **§5 Idioma** | **PASS** | Código en inglés; documentación en español. |
| **§6 Verificación** | **PASS** | Tests escritos por el agente y ejecutados por el usuario; esta edición no ejecuta build ni tests. |
| **§8/§9 Gobernanza** | **PASS** | Identidad SPEC-010 registrada; rama y trazabilidad conservadas; sin ampliación de alcance funcional. |
| **§1 Arquitectura del backend** | **PASS tras consolidación** | Sin dependencias entre roles no autorizadas. Ver §1.3/§1.4 abajo. |

### Verificación específica del conflicto arquitectónico

El diseño anterior documentaba `BuyOrderService → TokenService/CreditService/MarketAuditService/AuthenticationService/QuoteService`. Eso infringe §1.3, que permite a un Service depender solo de Model, Repository e Integration, y `docs/architecture.md` §1, que prohíbe eludir dependencias mediante intermediarios.

**Resolución aplicada en esta consolidación:**

| Punto | Resolución |
|---|---|
| Compra | `BuyOrderServiceImpl` opera con Model y Repository. Sin servicios financieros intermedios. |
| Identidad del comprador | `UserRepository` por el sujeto autenticado. No se invoca `AuthenticationService`. |
| Vendedor | `UserRepository.findByRole(SUPERUSER)`. No se invoca `SuperuserProvisioningService`. |
| Cotización | `QuoteSource` como **Integration** (puerto de lectura), no como Service. La integración definitiva es la lectura del repositorio canónico de REQ-S2-03. |
| Auditoría | Modelo histórico escrito por la propia fachada dentro de la misma transacción. Sin servicio de auditoría. |
| Créditos iniciales | `AuthenticationServiceImpl.register` y `SuperuserProvisioningServiceImpl` crean la cuenta mediante `UserCreditRepository`, en su propia transacción. Dependencia Service → Repository, permitida. |
| Emisión de tokens | La propia sincronización del catálogo crea la emisión y la tenencia del superusuario en la transacción que crea al jugador. |

No se crea Orchestrator: no hay coordinación entre casos de uso independientes y un Orchestrator no puede ejecutar lógica financiera (§1.1).

### Decisión final sobre `QuoteSource`

**Determinación**: `QuoteSource` es un **puerto Integration** que desacopla la compra de la implementación concreta de cotizaciones. No es una abstracción de servicio de aplicación.

| Criterio | Evaluación |
|---|---|
| ¿Expone un caso de uso propio? | No. Solo entrega un dato de mercado a un único consumidor. |
| ¿Es consumido por más de un Service? | No. Solo `BuyOrderServiceImpl`. |
| ¿Encaja en un rol definido? | Sí: **Integration**, que §1.1 define como adaptación de sistemas externos y §1.3 permite a un Service. |
| ¿Es reutilización transversal? | No. Nace y muere con esta feature. |
| Carácter | **Permanente** mientras siga desacoplando la compra |

**Ubicación**: `footballmarket.integrations`, fuera de `services`. Su implementación de desarrollo y pruebas es `SimulatedQuoteSource`.

**Ciclo de vida del puerto**: se conserva mientras aporte la separación entre la compra y el origen del precio. Su eliminación, si llegara a ser redundante, sería una refactorización explícita y consciente, no una consecuencia automática de la llegada de REQ-S2-03. Esa refactorización no está prevista ni aprobada.

**Ciclo de vida del simulador**: es estrictamente temporal. Cubre el hueco de implementación y se elimina cuando REQ-S2-03 aporte la fuente real.

**Por qué se conserva el puerto**: sin él, `BuyOrderServiceImpl` no podría compilarse ni probarse hasta que exista el repositorio canónico de REQ-S2-03, cuyo tipo y nombre no están definidos.

**Integración definitiva**: una implementación real del puerto consultará el repositorio canónico de REQ-S2-03. La frontera de la fachada no cambia.

**Regla de dependencia**: `BuyOrderServiceImpl` depende de `QuoteSource` como Integration, nunca de una implementación concreta. El simulador no se inyecta directamente en la fachada.

---

## Project Structure

### Documentation (this feature)

```text
specs/010-buy-tokens/
├── plan.md
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── buy-tokens.md
│   └── quote-source.md
├── checklists/
│   └── requirements-quality.md
└── tasks.md                # Phase 2 — NO generado por /speckit.plan
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── config/
│   └── QuoteSimulationConfiguration.java     # Registra el simulador solo en dev/test
├── controllers/
│   ├── OrderController.java                  # POST /api/orders/buy con OpenAPI
│   ├── dtos/requests/OrderBuyRequestDTO.java
│   ├── dtos/responses/OrderBuyResponseDTO.java
│   ├── mappers/OrderMapper.java
│   └── exceptions/GlobalExceptionHandler.java # Reutilizado, con handlers nuevos
├── models/
│   ├── UserCredit.java                       # Saldo de créditos
│   ├── PlayerToken.java                      # Tenencia por usuario y jugador
│   ├── PlayerTokenEmission.java              # Emisión inicial única por jugador
│   ├── MarketTransaction.java                # Auditoría append-only
│   └── records/
│       ├── CurrentQuote.java                 # Snapshot recibido del puerto Integration
│       └── BuyOrderRequest.java              # Intención de compra expresada en el modelo
├── repositories/
│   ├── UserCreditRepository.java
│   ├── PlayerTokenRepository.java
│   ├── PlayerTokenEmissionRepository.java
│   └── MarketTransactionRepository.java
├── integrations/
│   ├── QuoteSource.java                      # Integration: puerto de lectura de cotización
│   └── SimulatedQuoteSource.java             # Simulador temporal dev/test; se elimina con REQ-S2-03
├── services/
│   ├── BuyOrderService.java                  # Interfaz de aplicación
│   └── exceptions/                           # Excepciones de la compra
│       ├── SuperuserCannotBuyException.java         # 403, rechazo de negocio
│       ├── PlayerNotVisibleException.java           # 404 PLAYER_NOT_VISIBLE
│       ├── InvalidQuantityException.java            # 400 INVALID_QUANTITY
│       ├── InsufficientStockException.java          # 409 INSUFFICIENT_STOCK
│       ├── InsufficientCreditsException.java        # 409 INSUFFICIENT_CREDITS
│       ├── QuoteUnavailableException.java           # 409 QUOTE_UNAVAILABLE
│       ├── InvalidQuotePriceException.java          # 409 INVALID_QUOTE_PRICE
│       ├── QuoteContractViolationException.java     # 500, incumplimiento del puerto Integration
│       ├── SellerUnavailableException.java           # 500, ausencia de superusuario
│       └── ArithmeticOverflowException.java         # 500, importe no representable
├── services/internal/
│   └── PlayerTokenEmissionWriter.java       # Emisión inicial en la transacción que crea al jugador
├── services/impl/
│   └── BuyOrderServiceImpl.java              # Caso de uso completo
└── security/
    └── (reutilizado) PersistedUserJwtAuthenticationConverter.java

backend/src/main/resources/db/migration/
└── V9__create_market_domain.sql             # Única migración de SPEC-010

backend/src/test/java/footballmarket/
├── models/                                   # Categoría Model: reglas de dominio puras
├── controllers/OrderControllerTest.java      # Categoría Controller: contrato HTTP, Service simulado
├── services/BuyOrderServiceTest.java         # Categoría Service: solo API pública del caso de uso
├── repositories/MarketDomainPersistenceTest.java  # Categoría Repository: todo el estado durable
├── security/BuyOrderAuthorizationTest.java   # Categoría Security: autorización por rol persistido
├── config/QuoteSimulationConfigurationTest.java  # Categoría Config: arranque y binding de la fuente
└── integrations/SimulatedQuoteSourceTest.java # Categoría Integration: comportamiento del simulador
```

Cambios mínimos en componentes existentes:

| Componente existente | Cambio | Motivo |
|---|---|---|
| `AuthenticationServiceImpl` | Crear la cuenta de créditos inicial en la misma transacción de registro. | FR-015 y AC18 sin eventos ni servicio adicional. |
| `SuperuserProvisioningServiceImpl` | Crear la cuenta de créditos del superusuario con saldo 0 en su propia transacción. | FR-015, D2 y SC-013. |
| Sincronización del catálogo (`PlayerResolution`) | Crear emisión y tenencia inicial en la transacción que crea cada jugador. | FR-016 y AC17 sin dependencia entre Services. |
| `GlobalExceptionHandler` | Handlers para los rechazos de negocio nuevos, body ilegible y fallo interno. | FR-019, AC04 y AC16. |
| `SecurityConfig` | Sin cambios: `POST /api/orders/buy` queda cubierto por `anyRequest().authenticated()`. | Reutilización de la cadena existente. |

### Desviaciones registradas durante IMPLEMENT

Ninguna cambia decisiones funcionales. Todas responden a la arquitectura del backend o a la
frontera de observación de `docs/backend/testing.md`.

| Desviación | Motivo normativo |
|---|---|
| `models/records/BuyOrderRequest.java` | La matriz de dependencias de `docs/backend/architecture.md` §1.3 prohíbe que el Controller dependa de Repository. El flujo obligatorio `DTO → Mapper → Model → Service → Model → Mapper → DTO` exige un Model que transporte la intención de compra, de modo que la resolución del jugador y su elegibilidad ocurren en el Service. |
| `services/internal/PlayerTokenEmissionWriter.java` | `PlayerResolution` es un `@Component` interno del catálogo. Emitir los tokens exige escribir en `player_token_emissions` y `player_tokens`, por lo que la emisión se delega en un colaborador interno y no en el componente de resolución. No es un Service ni introduce dependencia entre Services. |
| `models/exceptions/*` del dominio de mercado | `UserCredit` y `PlayerToken` preservan sus invariantes en el modelo, y `conventions.md` prohíbe lanzar `IllegalArgumentException` u `IllegalStateException` para representar errores de dominio. |
| `integrations/exceptions/MissingQuoteSourceException.java` | El fallo de arranque sin fuente es un error de la integración de cotización, no un error de negocio. |
| `support/MarketTestData.java` y `models/TestFixtures.java` (tests) | El aprovisionamiento de catálogo no es alcanzable por Services públicos sin un proveedor externo, y `PlayerCatalogService.applySynchronization` aplica una foto completa que desactivaría jugadores creados por otras pruebas. `testing.md` 2.0.0 abre para la categoría Service una excepción de Arrange que permite delegar esas precondiciones en un helper compartido que acceda a repositorios, sin que ese acceso alcance Act, Assert ni Verify. `TestFixtures` no toca la base y sigue siendo construcción pura de entidades. La excepción fue autorizada por el usuario el 2026-10-17 y su impacto consta en el Sync Impact Report de `docs/backend/testing.md`. |

**Structure Decision**: la compra es un caso de uso transaccional único. Su frontera es `BuyOrderService`, con Model y Repository como únicas dependencias. Los cálculos e invariantes viven en Model; el acceso a datos y los bloqueos, en Repository. La emisión inicial se integra donde el jugador nace, y la cuenta de créditos donde se crea el usuario, porque ambas escrituras deben compartir la transacción que las origina.

---

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Ninguna | — | — |

Componentes considerados y sustituidos por una solución más simple:

| Componente considerado | Sustitución adoptada |
|---|---|
| `TokenService`, `CreditService`, `MarketAuditService` | Operaciones internas de la fachada y Model. |
| `UserRegisteredEvent` + listener | Escritura directa desde los Services de registro y aprovisionamiento. |
| Función PL/pgSQL de emisión | Dos escrituras desde la aplicación en una transacción. |
| `reserved_credits` | Eliminado: no hay reservas en esta SPEC. |
| `OrderType` y columna constante | Eliminados: solo existe BUY y la tabla ya lo expresa. |
| Verificación `SUM(quantity)` por compra | Eliminada del camino crítico; la invariante se sostiene por emisión única y transferencia simétrica. |

---

## Architecture and transactional flow

`BuyOrderServiceImpl` ejecuta una única transacción de Spring. Orden:

| Paso | Acción | Resultado |
|---|---|---|
| 1 | Resolver comprador por el sujeto autenticado. | Identidad persistida. |
| 2 | Validar rol: si es superusuario, rechazar. | 403 `SUPERUSER_CANNOT_BUY`. |
| 3 | Validar cantidad entre 1 y 100. | 400 `INVALID_QUANTITY`. |
| 4 | Resolver la elegibilidad del jugador con `PlayerRepository`: `findById` para existencia y `findVisibleById` para visibilidad. Un `playerId` no positivo no identifica a ningún jugador y responde 404 `PLAYER_NOT_FOUND`. | 404 `PLAYER_NOT_FOUND` o `PLAYER_NOT_VISIBLE`. |
| 5 | Leer la cotización vigente y verificar que el `playerId` del snapshot coincide con el solicitado. | Precio por token; si no coincide, fallo interno con reversión. |
| 6 | Calcular el importe con aritmética exacta. | Total único para toda la operación. |
| 7 | Bloquear las cuentas de créditos de comprador y vendedor en orden ascendente de `user_id`. | Serializa saldos. |
| 8 | Insertar la tenencia del comprador con cantidad 0 si no existe (`ON CONFLICT DO NOTHING`) y bloquear las tenencias de comprador y vendedor en orden ascendente de propietario. | Serializa el inventario del jugador. |
| 9 | Revalidar saldo y stock con los valores bloqueados. | Rechazos 409 sin revelar cifras. |
| 10 | Aplicar transferencias de créditos y tokens. | Estados nuevos calculados. |
| 11 | Insertar la auditoría con estados anteriores y posteriores. | Registro completo. |
| 12 | Confirmar. | Un solo commit. |

Cualquier excepción revierte la operación completa: no existen movimientos parciales ni auditoría de compras no confirmadas.

### Resolución de elegibilidad del jugador

FR-006 exige distinguir dos 404 que una consulta de visibilidad por sí sola no puede separar: un identificador inexistente y un jugador existente pero invisible producen el mismo resultado vacío.

| Situación | Mecanismo | Código |
|---|---|---|
| `playerId` en `0` o negativo | Ninguna fila puede identificarse con un identificador no positivo; `findById` no resuelve | 404 `PLAYER_NOT_FOUND` |
| Jugador inexistente | `PlayerRepository.findById` devuelve vacío | 404 `PLAYER_NOT_FOUND` |
| Jugador existente pero no visible | `findById` resuelve y `findVisibleById` devuelve vacío | 404 `PLAYER_NOT_VISIBLE` |
| Jugador visible | Ambas consultas resuelven | Continuar |

`BuyOrderServiceImpl` consulta `PlayerRepository` directamente. No invoca `PlayerCatalogService`: el catálogo expondría `getVisiblePlayerById`, que devuelve vacío indistintamente en los dos primeros casos y por tanto no permite implementar FR-006, y además introduciría una dependencia Service → Service prohibida por `docs/backend/architecture.md` §1.3.

El predicado de visibilidad no se duplica ni se reescribe: se reutiliza el método `findVisibleById` ya existente en `PlayerRepository`, que replica el filtro vigente del catálogo (jugador activo con equipo vigente). `findById` es un método heredado de `JpaRepository`, por lo que **no se requiere ningún método de consulta nuevo** ni cambio en los repositorios existentes.

La comprobación ocurre una sola vez, antes de tomar cualquier bloqueo, y no se repite bajo bloqueo: la fila del jugador no se bloquea, en coherencia con D3 opción A.

### Regla de lectura bajo bloqueo

El saldo y el stock se leen **exclusivamente** desde las consultas de bloqueo de los pasos 7 y 8.

| Regla | Motivo |
|---|---|
| No leer `user_credits` ni `player_tokens` antes de bloquearlas | Una lectura previa queda cacheada en el contexto de persistencia y puede devolver un saldo obsoleto aunque la fila esté bloqueada después |
| No reutilizar una entidad cargada anteriormente para decidir la validación | El valor válido es el leído bajo el bloqueo en esta transacción |
| Una cuenta de créditos ausente no se crea durante la compra | La compra rechaza; la inicialización ocurre donde nace el usuario |

Esta regla evita que la revalidación sea una comprobación sobre valores antiguos, que es la forma más común de introducir sobreventa o doble gasto en una aplicación con caché de primer nivel.

### Secuencia efectiva de bloqueos

La tabla de pasos describe acciones; esta tabla describe **qué bloqueos se toman realmente** y en qué orden. La distinción importa porque algunas operaciones aceptan bloqueos implícitos.

| # | Operación | Bloqueo real adquirido |
|---|---|---|
| 1 | Lectura de comprador, vendedor y jugador | Ninguno de escritura |
| 2 | Lectura de la cotización | Ninguno de escritura sobre el mercado |
| 3 | `SELECT ... FOR UPDATE` sobre `user_credits` del comprador y del vendedor, ordenados por `user_id` ascendente | Bloqueo de fila `FOR UPDATE` sobre las cuentas existentes |
| 4 | `INSERT ... ON CONFLICT DO NOTHING` sobre la tenencia del comprador con cantidad 0 | `FOR KEY SHARE` sobre `users` y `players` referenciados; si hay conflicto con una inserción concurrente sin confirmar, **espera** a esa transacción |
| 5 | `SELECT ... FOR UPDATE` sobre las tenencias del comprador y del vendedor, ordenadas por propietario ascendente | `FOR UPDATE` sobre la fila del vendedor y sobre la fila del comprador si ya existía |
| 6 | Revalidación de saldo y stock | Ninguno adicional: se lee lo ya bloqueado |
| 7 | Transferencias y auditoría | Escrituras sobre filas ya bloqueadas |

**Precisiones técnicas verificadas:**

1. `ON CONFLICT DO NOTHING` **no** bloquea la fila preexistente cuando hay conflicto: descarta la inserción. Por eso el paso 5 es imprescindible y no redundante; es el que aporta el bloqueo sobre la tenencia del comprador cuando esta ya existía.
2. La inserción del paso 4 sí serializa la creación concurrente de la primera tenencia, porque la unicidad `(player_id, owner_user_id)` obliga a resolver el conflicto contra la transacción que insertó primero.
3. El orden total por clave es la garantía real contra ciclos: todas las compras bloquean `user_credits` por `user_id` ascendente antes de tocar `player_tokens`. Dos compras concurrentes no pueden adquirir bloqueos en orden inverso.

**Interacción con la emisión durante la sincronización del catálogo**: la emisión escribe `player_token_emissions` y `player_tokens`, y no toca `user_credits`. Como la compra toma primero las cuentas y después las tenencias, y la emisión solo toma tenencias, no se forma un ciclo entre ambos procesos.

**Interacción con la desactivación del jugador**: la inserción de la tenencia toma `FOR KEY SHARE` sobre la fila de `players`. Una actualización de `players.active` toma `FOR NO KEY UPDATE`, que es compatible con `FOR KEY SHARE`, de modo que la compra no bloquea la sincronización ni la bloquea a ella. Esto confirma de forma técnica la semántica de D3 aprobada. Solo un borrado físico del jugador, que el catálogo no realiza, tomaría un bloqueo incompatible.

**Alcance de la garantía**: el orden total evita ciclos entre compras y entre compras y emisión. No se afirma que ninguna operación pueda esperar: dos compras simultáneas pueden esperar la una a la otra mientras la primera confirma. Esa espera no es un fallo y no activa reintentos automáticos.

**Orden de las consultas**: el orden debe ser explícito y determinista. La forma más inequívoca es ordenar los identificadores en la aplicación y bloquearlos por clave primaria en ese orden, en lugar de confiar en el plan de ejecución de una consulta con `ORDER BY`.

### Semántica de desactivación concurrente

Si el jugador fue desactivado antes de la validación, la compra se rechaza. Si se desactiva después de superarla, la compra en curso se completa. No se garantiza orden estricto de confirmación entre ambos procesos. Es una limitación aceptada y no bloqueante (D3).

---

## Concurrency and integrity

Los mecanismos que sostienen las garantías obligatorias:

| Garantía | Mecanismo |
|---|---|
| Sin sobreventa | Bloqueo de la tenencia del vendedor y revalidación de stock. |
| Sin doble gasto | Bloqueo de la cuenta del comprador y revalidación de saldo. |
| Sin saldos negativos | `CHECK (available_credits >= 0)` y revalidación previa. |
| Sin tenencias negativas | `CHECK (quantity >= 0)`. |
| Conservación de tokens | Emisión única de 100 y transferencia simétrica. |
| Atomicidad | Una única transacción de compra. |
| Primera tenencia | Inserción idempotente y unicidad `(player_id, owner_user_id)`. |

No se implementan reintentos automáticos ni coordinación distribuida. Ante un conflicto de bloqueo, la transacción falla y el cliente reintenta por su cuenta, conforme a D1.

**Riesgo arquitectónico aceptado — H10: contención sobre la fila del vendedor.** Toda compra confirmada bloquea la misma fila de `user_credits` del SUPERUSER, incluso cuando compradores y jugadores son distintos. El coste es real y limitado a esta arquitectura: serializa las compras y fija un techo de concurrencia igual al de las compras de un solo vendedor. Se acepta conscientemente para el alcance académico porque el único vendedor del dominio es, por diseño, el mismo superusuario (SPEC-001/003), y porque la alternativa exigiría distribuir la propiedad del inventario o un modelo de órdenes, ambos fuera de alcance. No se rediseña la estrategia de bloqueo ni se modifica el orden de adquisición: `plan.md` ya fija el orden total por `user_id` ascendente como la garantía real contra ciclos, y ese orden se conserva. La mitigación es de concurrencia, no de diseño: si el volumen lo justificara, la medición del objetivo no vinculante de la sección de Performance Goals sería el punto de partida, y ese objetivo no genera tarea.

**Remediación H08 — verificada por el usuario el 2026-10-17.** El code review detectó que `@Nested Concurrency` enviaba dos tareas a un pool sin ningún punto de sincronización, de modo que una ejecución secuencial satisfacía las mismas aserciones que una carrera real, y que clasificaba el resultado de cada operación comparando `getClass().getSimpleName()` contra `"Stock"` y `"Credits"`. Esa comparación textual aceptaba como previsto cualquier excepción homónima: eliminar la traducción de `InsufficientUserCreditsException` a `InsufficientCreditsException` habría dejado la prueba en verde. Además faltaban verificaciones que `plan.md` y T033/T034 declaraban: conservación de la suma de créditos de las partes, correspondencia entre la fila de auditoría y la operación confirmada, y ausencia total de aserciones de auditoría y de saldo del vendedor en el escenario de doble gasto sobre jugadores distintos.

La corrección no toca código productivo. `attempt` captura `InsufficientStockException` e `InsufficientCreditsException` de forma explícita y propaga cualquier otra excepción, y el resultado es un enum `Outcome` declarado en la clase externa porque un enum no puede ser miembro de una clase interna no estática. `race` crea un `CyclicBarrier(2)` que cada transacción espera **dentro de ella**, antes de invocar `buyOrderService.buy`, con esperas acotadas de 15 s en la barrera y 30 s en los futuros y sin `Thread.sleep`. Los cuatro escenarios afirman además la suma de créditos de todas las partes antes y después, el registro financiero único que crea la carrera, sus campos de comprador, vendedor, jugador, cantidad, precio e importe, los saldos y tenencias anteriores y posteriores, y que la operación rechazada no deja fila ni tenencia.

**Límite de cobertura, explícito.** `CyclicBarrier(2)` coordina que dos transacciones estén abiertas y se liberen a la vez; **no demuestra contención efectiva de bloqueos de PostgreSQL**. Estas pruebas evidencian que, bajo simultaneidad real, no hay sobreventa ni doble gasto y que el total emitido y la suma de créditos se conservan. No evidencian que ambas transacciones compitan por una misma fila bloqueada, ni el orden total de adquisición de bloqueos descrito más arriba. Demostrar esa contención exigiría observar el bloqueo desde fuera, por ejemplo consultando `pg_locks` o `pg_stat_activity` mientras una tercera transacción retiene el bloqueo; queda como mejora futura y no como garantía acreditada. No se registra entrada de deuda técnica por este límite: es una decisión de cobertura acordada, no un compromiso pendiente de pago.

**Validación humana de H08.** `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (39 s), `./gradlew test` BUILD SUCCESSFUL (5 min 5 s), cinco ejecuciones forzadas con `./gradlew test --tests '*MarketDomainPersistenceTest' --rerun-tasks` BUILD SUCCESSFUL (51 s, 43 s, 47 s, 47 s, 47 s), `spotlessApply` BUILD SUCCESSFUL (2 s) y `spotlessCheck` BUILD SUCCESSFUL (2 s) tras corregir una diferencia de formato. El XML resultante declara 4 tests, 0 failures, 0 errors y 0 skipped en `@Nested Concurrency`, y 21 sin fallos en la clase completa. Durante esa validación una primera corrida detectó que la conservación de SC-008 comparaba la suma final de créditos contra el saldo individual del vendedor en vez de la suma previa de ambas partes; se corrigió el valor de referencia, no la expectativa.

**Validación humana de H09, H10 y H11, y de T055 y T076.** `./gradlew test --tests '*PlayerTokenTest'` BUILD SUCCESSFUL (9 s), `./gradlew test --tests '*OrderControllerTest'` BUILD SUCCESSFUL (17 s), `./gradlew test --tests '*MarketDomainPersistenceTest'` BUILD SUCCESSFUL (46 s) y `./gradlew test` BUILD SUCCESSFUL (5 min 33 s), con **646 tests, 0 failures, 0 errors y 0 skipped** en 121 clases. `spotlessCheck` detectó una diferencia de formato, corregida con `./gradlew spotlessApply` BUILD SUCCESSFUL (2 s) y verificada con `./gradlew spotlessCheck` BUILD SUCCESSFUL (2 s). `MarketDomainPersistenceTest` queda en 28 tests repartidos en ocho grupos, incluidos `@Nested Rechazos y reversión` con 8 y `@Nested Alta de usuario y saldo inicial` con 2.

Tres correcciones fueron defectos de los propios tests y no del código productivo, y se documentan para que no se repitan: la tenencia del comprador se observa como `Optional` porque la primera compra la crea el Service; la restricción temporal de AC18 se declara `NOT VALID` porque la tabla ya contiene saldos válidos que un `ADD CONSTRAINT` validaría y rechazaría; y la contraseña del caso de alta debe respetar la longitud mínima del dominio, porque una contraseña más corta hace fallar el alta antes de tocar la base y enmascara la inspección de la restricción.

---

## Persistence

Cuatro tablas, con detalle en [data-model.md](data-model.md):

| Tabla | Contenido |
|---|---|
| `user_credits` | Saldo actual por usuario. |
| `player_tokens` | Tenencia actual por usuario y jugador. |
| `player_token_emissions` | Emisión inicial única por jugador. |
| `market_transactions` | Auditoría de compras. |

Los saldos se leen directamente de las tablas de estado; no se reconstruyen sumando movimientos históricos.

### Migración única

`V9__create_market_domain.sql`, siguiente versión disponible. Crea las cuatro tablas con sus índices y restricciones, inicializa créditos de usuarios existentes y del superusuario, y registra la emisión inicial de 100 tokens por jugador existente asignada al superusuario. No se editan migraciones ya aplicadas.

---

## Dependencies

| Dependencia | Estado | Efecto |
|---|---|---|
| SPEC-001/003: identidad, autenticación y registro | Existente | Se reutiliza sin cambios de contrato. |
| SPEC-002: catálogo y sincronización | Existente | Se extiende la creación de jugadores con la emisión inicial. |
| SPEC-007: dominio de equipo y liga | Existente | El predicado de visibilidad la requiere: `PlayerRepository.findVisibleById` une `team` y `team.league` y exige `team.current = true`. Sin ella no puede distinguirse `PLAYER_NOT_VISIBLE` de `PLAYER_NOT_FOUND` (FR-006). |
| Superusuario único | Existente, aprovisionado en arranque | Debe tener cuenta de créditos con saldo 0. |
| REQ-S2-03: cotización vigente | **No implementado** | Dependencia técnica. Mientras no exista, el simulador cubre desarrollo y pruebas. |

### Integración con cotizaciones

`BuyOrderServiceImpl` obtiene el precio a través de `QuoteSource`, un Integration. La integración definitiva es la lectura del repositorio canónico que entrega REQ-S2-03, cuyo esquema no se define en esta SPEC.

#### Selección de la fuente

**Remediación H07 — verificada por el usuario.** La caracterización humana
confirmó que registrar la configuración ordinaria del simulador antes de una configuración de
usuario con `QuoteSource`, con simulador habilitado, dejaba dos beans: `@ConditionalOnMissingBean`
no se reevalúa después de registrar la alternativa. `QuoteSimulationConfiguration`
es `@AutoConfiguration` y se descubre mediante
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. El filtro de
auto-configuración de `@SpringBootApplication` evita su procesamiento prematuro por escaneo.
No debe importarse ni registrarse directamente como configuración de usuario.

Las regresiones conservan ambos órdenes de incorporación al runner con simulador habilitado o
deshabilitado, utilizando `AutoConfigurations.of(...)` para el simulador y configuraciones de
usuario para la alternativa. Verifican unicidad, identidad, ausencia del simulador e inyección
obligatoria mediante consumidor. Otras pruebas ejercitan el selector real de
`@EnableAutoConfiguration` con los candidatos `.imports` y el filtro de escaneo de Boot. Las
auto-configuraciones ajenas se excluyen únicamente en esa prueba reducida de discovery.
Se conservan propiedades, defaults, fallo sin fuente y ausencia de fallback durante la compra.
Esto no establece precedencia entre varias fuentes reales ni entre auto-configuraciones futuras:
si la fuente definitiva también es auto-configurada, deberá definirse su orden respecto del
simulador cuando exista. No hay cambios en contratos HTTP.

**Validación humana de H07.** `./gradlew test --tests '*QuoteSimulationConfigurationTest'`
BUILD SUCCESSFUL (11 s), `./gradlew test --tests '*BuyOrderServiceTest'` BUILD SUCCESSFUL (39 s),
`./gradlew test` BUILD SUCCESSFUL (4 min 57 s), `./gradlew spotlessCheck` BUILD SUCCESSFUL (6 s),
`./gradlew spotlessApply` BUILD SUCCESSFUL (1 s) y `./gradlew spotlessCheck` final
BUILD SUCCESSFUL (1 s), sin fallos de tests ni de formato. Queda verificado para los escenarios
cubiertos: precedencia del simulador frente a una fuente registrada por configuración de usuario,
unicidad e identidad de la fuente, ausencia del simulador cuando existe fuente alternativa,
resolución por consumidor con dependencia obligatoria, discovery por `.imports` con
`@EnableAutoConfiguration`, exclusión del escaneo ordinario, fallo de arranque sin fuente y precio
por defecto. H07 no cubre la precedencia entre varias fuentes reales, proveedores reales aún
inexistentes, restricciones por perfil ni ningún cambio en `BuyOrderServiceImpl`.

La fuente se resuelve **una sola vez, en el arranque**, y no durante la compra:

| Estado del contexto | Fuente activa | Resultado |
|---|---|---|
| Existe fuente real de REQ-S2-03 | La fuente real | La compra usa la cotización vigente |
| No existe fuente real y el simulador está habilitado | Simulador | La compra usa el precio configurado, identificado como simulado |
| No existe fuente real y el simulador está deshabilitado | Ninguna | El arranque falla con un error de configuración explícito |

**Regla obligatoria**: cuando existe una fuente real, el simulador **no se registra**, aunque su propiedad esté habilitada. El simulador nunca sustituye una cotización real ausente.

#### Propiedades del simulador

El simulador se configura con dos propiedades, ambas definedas en `contracts/quote-source.md`:

| Propiedad | Defecto | Uso |
|---|---|---|
| `market.quote.simulator.enabled` | `false` | Activa el simulador solo en desarrollo y pruebas |
| `market.quote.simulator.price` | `100` | Precio por token en centésimas; `100` equivale a 1 crédito |

Ambas son de lectura y binding en la configuración del simulador; no se registran como propiedades de la compra, que no decide la fuente en ejecución.

#### Ausencia de implementación frente a ausencia de cotización

Son dos situaciones distintas y se comportan de forma distinta:

| Situación | Origen | Resultado observable |
|---|---|---|
| REQ-S2-03 no implementado y simulador deshabilitado | Falta la implementación del dominio de cotizaciones | La aplicación no inicia, con error de configuración explícito |
| Fuente disponible sin cotización para el jugador | El jugador no tiene cotización vigente | 409 `QUOTE_UNAVAILABLE` en `POST /api/orders/buy` |

La segunda situación nunca activa el simulador ni produce un precio inventado.

Detalles en [contracts/quote-source.md](contracts/quote-source.md) y `research.md` R-05.

---

### Vendedor ausente

**Clasificación Constitution §2.2: OBLIGATORIO.** Es una garantía de integridad transaccional: la compra no puede identificar al vendedor porque `UserRepository.findByRole(UserRole.SUPERUSER)` no resuelve.

| Aspecto | Decisión |
|---|---|
| Clasificación | **OBLIGATORIO** (Constitution §2.2), aprobado por el equipo. Requisito `FR-023`, criterio `AC19` |
| Cuándo se comprueba | En la fachada, tras resolver el comprador y **antes** de tomar cualquier bloqueo o mover recursos |
| Por qué es OBLIGATORIO | Un fallo posterior dejaría una compra parcial; `research.md` R-02 ya lo exigía sin tarea que lo verificara |
| Comportamiento | La operación falla con la excepción técnica propia y revierte por completo: sin débitos, sin tenencias creadas o modificadas y sin registro de auditoría |
| Respuesta pública | **500 `INTERNAL_ERROR` genérico**, reutilizando la cláusula ya aprobada en `contracts/buy-tokens.md`. **No se crea un código nuevo** |
| Detalle técnico | Solo en registro interno de diagnóstico; nunca en el cuerpo de la respuesta |
| Verificación | Categoría Repository, grupo `VendedorAusente` |

Reutilizar el 500 genérico evita una decisión contractual nueva. Si el equipo prefiere un código distinto, es un cambio de contrato y requiere su autorización expresa antes de incorporarlo a `spec.md` y `contracts/`.

---

## Testing strategy

### Recuperación de emisiones al aprovisionar SUPERUSER (H06)

`SuperuserProvisioningServiceImpl` completa la recuperación en todas sus ramas exitosas, incluida
la llamada explícita deshabilitada con propietario existente. Reutiliza su transacción REQUIRED;
no invoca otro Service ni abre REQUIRES_NEW. Asegura la cuenta sin sobrescribirla y hace flush antes
de consultar inconsistencias. `SuperuserProvisioningException` rechaza tenencias sin emisión y
emisiones sin tenencia del propietario inicial; cantidad cero es válida y no se repara inventario.

`PlayerTokenEmissionRepository` ejecuta una CTE nativa PostgreSQL: inserta emisiones ausentes para
todos los jugadores (también inactivos), con `ON CONFLICT (player_id) DO NOTHING` y `RETURNING`.
Solo las emisiones retornadas alimentan la inserción de tenencias. Esta segunda inserción no
ignora conflictos: cualquier error revierte usuario, cuenta, emisiones y tenencias del
aprovisionamiento. No modifica V9 ni reconstruye tenencias de emisiones históricas.

El callback `SmartInitializingSingleton` existente ejecuta el aprovisionamiento habilitado antes
de las sincronizaciones HTTP normales. Precondición aprobada: no sincronizar catálogo durante
aprovisionamientos manuales posteriores. No hay garantía global multiinstancia ni coordinación
distribuida; validar y recuperar no constituye un protocolo serializable frente a escritores
concurrentes fuera de esa precondición.

`SuperuserMarketRecoveryPersistenceTest` verifica mediante Services reales y JDBC posterior al
commit/rollback: catálogo anterior al propietario, recuperación de activos e inactivos,
repetición, stock parcialmente vendido o agotado, rama deshabilitada existente, inconsistencias
sin reparar y rollback ante CHECK de prueba (SQLSTATE 23514). La constraint está acotada a un
jugador en Testcontainers aislado y se elimina en `finally`. Ejecución humana pendiente.

Verificación por responsabilidad, con límites reales, conforme a `docs/backend/testing.md`. Las categorías usadas son **las de `testing.md` §2**; no se inventan categorías adicionales.

### Por qué el estado durable no se afirma desde la categoría Service

`testing.md` §5 establece que el test de Service no declara, inyecta ni accede a Repository, JPA/Hibernate, `EntityManager`, JDBC, SQL o DB, *tampoco para preparación, limpieza o assertions*, y que si el contrato del Service no expone un detalle persistido **no se elude la frontera para observarlo**. §6 refuerza que la infraestructura técnica de test no habilita *assertions eludiendo el Service*.

`BuyOrderService` expone una única operación de compra que devuelve el resultado de esa compra. **No expone saldos, tenencias ni listado de auditoría.** Por lo tanto, afirmaciones como «una compra rechazada no modifica créditos» o «el total emitido permanece en 100» no son realizables desde la categoría Service: serían verificables solo mediante acceso a la persistencia, que esa categoría prohíbe.

Es el límite que el propio proyecto ya resolvió de forma explícita en `PlayerImageRunRecoveryServiceTest`: *«el estado durable se comprueba exclusivamente en Repository»*. Esta SPEC adopta la misma frontera.

**Consecuencia de diseño**: los invariantes financieros de esta feature no se afirman duplicándolos en la categoría Service. Se afirman una sola vez, en la categoría que puede observarlos.

### Asignación por categoría

| Categoría (`testing.md` §2) | Ubicación | Componentes reales | Qué afirma |
|---|---|---|---|
| Model | `models/` | Unitario puro, sin Spring | Transferencias que preservan la no negatividad y la conservación del total. |
| Controller | `controllers/` | `@WebMvcTest`, Service simulado, cadena de seguridad real | Contrato HTTP, códigos, estructura de errores, restricciones de entrada, no-revelación, ausencia de invocación del Service y acceso sin sesión. |
| Service | `services/` | `@SpringBootTest`, perfil `test`, PostgreSQL real de Testcontainers, Integration simulada | **Solo lo que devuelve o lanza la API pública**: resultado de la compra y tipo de excepción por cada rechazo. |
| Repository | `repositories/` | PostgreSQL real, repositorios reales | Todo el estado durable: créditos, tenencias, auditoría, constraints, secuencias de bloqueo, concurrencia e idempotencia. |
| Security | `security/` | Cadena de seguridad y componentes reales | Que la autorización se decide por el rol **persistido**. |
| Config | `config/` | Contexto de configuración | Registro del simulador bajo las condiciones aprobadas, binding de sus propiedades y ausencia de conflictos con una fuente real. |
| Integration | `integrations/` | Componente real, instancia directa, sin Spring | Comportamiento del simulador: precio devuelto, identificación de estrategia simulada y cumplimiento del contrato `QuoteSource`. |

«Concurrencia» y «Emisión e inicialización» **no son categorías** de `testing.md`: son grupos `@Nested` dentro de la categoría Repository, porque el estado que afirman solo es observable desde ahí.

### Qué afirma cada categoría

**Controller** — únicamente la frontera HTTP: estado y cuerpo exactos de 200, 400, 403, 404, 409 y 500; estructura de `ErrorResponseDTO`; rechazo por validación propia sin invocar el Service; no-revelación de stock, saldo e importe; ausencia de sesión. No afirma sobre tenencias, saldos ni auditoría: con el Service simulado no puede, y un doble no es evidencia de integridad.

**Service** — únicamente la API pública del caso de uso:

- Compra correcta: los seis campos de FR-018, con el precio e importe esperados.
- Cada rechazo: el tipo de excepción que corresponde a su código, sin observar la base de datos.
- Cotización con `playerId` distinto: fallo interno por incumplimiento del contrato de cotización.
- `strategyVersion` ausente: la compra se confirma y el valor viaja sin estrategia.
- Repetición (D1): dos compras correctas distintas.
- `playerId` en `0` o negativo frente a jugador existente no visible: excepciones distintas.
- Rol no|Superusuario: rechazo por autorización.
- Desbordamiento aritmético: fallo interno genérico.

**Repository** — todo el estado durable, en grupos `@Nested`:

- `Restricciones`: `CHECK` de no negatividad, `total_amount = quantity * unit_price`, `UNIQUE (player_id, owner_user_id)` y `PRIMARY KEY (player_id)` de la emisión.
- `RejectionsAndRollback`: una compra rechazada por stock, créditos, cantidad, elegibilidad, cotización o precio de cotización inválido no modifica `user_credits`, no crea ni modifica `player_tokens` y no genera auditoría; un fallo al registrar la auditoría revierte la compra completa. Los tres rechazos anteriores a los bloqueos se incluyen para fijar ese orden de validación.
- `AltaDeUsuarioYSaldoInicial`: AC10 y AC18. El alta por `AuthenticationService.register` persiste usuario y cuenta de créditos juntos; un rechazo real de PostgreSQL al insertar la cuenta revierte el alta completa, comprobado con una restricción `CHECK` temporal sobre la base desechable de Testcontainers y removida en `finally`.
- `IdempotentInitialization`: reejecutar la inicialización de créditos y de emisión no restaura saldos, no repone inventario, no duplica cuentas ni emisiones y no altera el total de 100.
- `Concurrencia`: las siete verificaciones de SC-011, con conexiones y transacciones independientes y orden determinista de adquisición de bloqueos.
- `VendedorAusente`: sin superusuario persistido, la compra falla sin movimientos parciales ni auditoría.
- `CotizacionPersistida`: la fila de auditoría guarda `unit_price` y `quote_calculated_at` del snapshot aplicado; `quote_strategy_version` conserva la versión recibida y queda en `null` cuando la fuente omite la estrategia.

**Config** — la resolución de la fuente ocurre en el arranque, nunca durante una compra. Los grupos `@Nested` de `QuoteSimulationConfigurationTest` son:

- `SeleccionDeFuente`: con fuente real y simulador habilitado, solo queda registrada la fuente real.
- `ArranqueSinFuente`: sin fuente y con simulador deshabilitado, el arranque falla con error de configuración explícito.
- `SimuladorHabilitado`: sin fuente y con simulador habilitado, el simulador se registra y su precio queda bound desde `market.quote.simulator.price`.

Conforme a `testing.md` §12, esta categoría **no** usa `@ActiveProfiles("test")`: son propiedades explícitas y no hay base de datos.

**Integration** — el comportamiento del puerto vive en la categoría Integration, no en Config. `SimulatedQuoteSourceTest` verifica, con el componente real y sin Spring, que el `CurrentQuote` devuelto lleva el `playerId` solicitado, el precio configurado en centésimas y una estrategia que identifica la cotización como simulada. `testing.md` §2 distingue esta categoría de Config precisamente para no mezclar el arranque con el comportamiento del adaptador.

**Security** — la decisión de autorización proviene del rol persistido, no de un claim. El token emitido por `JWTProvider` solo contiene `subject`, `issuedAt` y `expiration`: **no existe claim de rol**, de modo que la decisión solo puede provenir de la persistencia. La prueba lo verifica con la cadena real: un usuario persistido como `SUPERUSER` recibe 403 y uno persistido como `USER` compra.

### Invariantes y su categoría

| Invariante | Categoría que la afirma |
|---|---|
| SC-001 datos y transferencia exacta de la compra | Service (respuesta) + Repository (saldo y tenencia resultantes) |
| SC-008 total de tokens en 100 y créditos conservados | Repository |
| SC-009 sin créditos ni tenencias negativos | Repository |
| SC-010 exactamente un registro por compra confirmada y ninguno por rechazada | Repository |
| SC-011 compras simultáneas sin sobreventa ni doble gasto | Repository |
| SC-012 emisión única, sin reposición de inventario vendido | Repository |
| SC-013 saldo inicial una sola vez, sin restaurar créditos consumidos | Repository |
| SC-014 respuestas contractuales y ausencia de revelaciones | Controller |
| SC-016 dos solicitudes iguales producen dos compras distintas | Service (respuestas distintas) + Repository (dos registros) |
| FR-017 una transferencia solo mueve tokens entre tenencias del mismo jugador | Model |

**Invariante de dominio — H11/T070.** `PlayerToken.transferTo` exige que emisor y receptor pertenezcan al mismo jugador antes de mover cantidad. Sin esa comprobación, el Javadoc afirmaba una conservación de la suma del jugador que el modelo no garantizaba. La compra nunca produce el caso porque `lockHoldings` resuelve ambas tenencias del mismo `player_id`, de modo que el comportamiento válido de compra no cambia; la comprobación protege el invariante frente a usos futuros. Dos tenencias cuyo jugador aún no tiene identificador se tratan como del mismo jugador, porque el modelo no puede afirmar una contradicción que el persistido todavía no permite comprobar. Cubierto por `@Nested Transfer` en `PlayerTokenTest`.

### Escenarios y capas

Escenarios obligatorios: compra exitosa; créditos insuficientes; stock insuficiente; cantidad inválida; jugador inexistente; jugador no visible; superusuario que intenta comprar; creación concurrente de la primera tenencia; compras simultáneas del stock restante; ausencia de sobreventa y doble gasto; reversión completa ante errores; auditoría consistente; emisión inicial única; idempotencia de la inicialización de créditos y de tokens; vendedor ausente; y autorización por rol persistido.

Escenarios recomendados, no bloqueantes: compra concurrente con desactivación del jugador. D3 opción A solo exige elegibilidad al validar la compra; el orden estricto entre compra y desactivación es una mejora posterior que no condiciona la aceptación de la SPEC.

Escenarios derivados de los contratos:

| Escenario | Categoría | Resultado esperado |
|---|---|---|
| `playerId` en `0` o negativo | Controller y Service | 404 `PLAYER_NOT_FOUND` |
| Cotización con `playerId` distinto al solicitado | Service y Repository | Fallo interno genérico; reversión completa y sin auditoría |
| `strategyVersion` ausente en la fuente | Service y Repository | Compra correcta; `quote_strategy_version` en `null` |
| Vendedor ausente | Service y Repository | Fallo interno genérico; sin movimientos parciales ni auditoría |
| Fuente real disponible y simulador habilitado | Config | La fuente real prevalece; el simulador no se registra |

Las pruebas concurrentes siguen la secuencia de bloqueos definitiva: cada operación usa su propia conexión y transacción, y ninguna se ejecuta dentro de una transacción de prueba que nunca confirma. Los escenarios de reversión comprueban el estado **después** de que la transacción fallida concluye, nunca dentro de ella.

El agente escribe los tests y no los ejecuta; su ejecución corresponde al usuario.

---

## Phase 1 Design — Artefactos generados

- **[research.md](research.md)**: decisiones técnicas vigentes, alternativas descartadas, estrategia de cotización, riesgos.
- **[data-model.md](data-model.md)**: entidades, restricciones, índices, precisión, emisión inicial y auditoría.
- **[contracts/buy-tokens.md](contracts/buy-tokens.md)**: contrato HTTP de `POST /api/orders/buy`.
- **[contracts/quote-source.md](contracts/quote-source.md)**: puerto Integration de lectura de cotización, selección de fuente y comportamiento del simulador.
- **[quickstart.md](quickstart.md)**: pasos de verificación posterior a la implementación.

---

## Phase 2 — Tasks (NO generado por Plan)

`tasks.md` se genera con `/speckit.tasks` tras la aprobación de este plan consolidado. El desglose esperado cubre: migración única, entidades y repositorios, emisión integrada en la sincronización, créditos iniciales, fachada de compra, Controller y contrato, simulador de cotización y estrategia de pruebas.

---

## Riesgos técnicos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| REQ-S2-03 no definido | Puerto Integration con contrato mínimo; el simulador cubre desarrollo y pruebas; el esquema pertenece a REQ-S2-03. |
| El puerto queda más de lo necesario | Su eliminación sería una refactorización explícita; no está prevista ni aprobada. |
| El puerto sobrevive a la integración real | El puerto es permanente por diseño; solo `SimulatedQuoteSource` es temporal. Su eliminación sería una refactorización explícita. |
| Lectura de saldo o stock desde valores cacheados | Regla de lectura bajo bloqueo: solo se leen valores obtenidos bajo bloqueo, nunca antes. |
| Punto de integración de la emisión en la sincronización | Se define explícitamente: creación del jugador y emisión en la misma transacción. |
| Divergencia entre idempotencia de emisión y de compra | Se documentan como independientes (D1). |
| Falsos positivos en pruebas de concurrencia | Se usan conexiones independientes y se verifican invariantes, no tiempos. |
| Modificaciones en el rango del precio | Aritmética exacta y `CHECK` de no negatividad. |

---

## Próximos pasos SDD

Superados: **Specify**, **Clarify**, **Plan**, **Checklist**, **Tasks**, **Analyze** e **Implement** de US01, con 66 de 75 tareas completadas y las remediaciones H01–H11 registradas en `tasks.md`.

Pendientes, en este orden:

1. Validación humana de la suite completa y de `spotlessCheck` tras las remediaciones H09–H11.
2. **CODE REVIEW** de US01: informe completo de revisión de código y aprobación humana del checklist.
3. **TESTING** de US01: informe de pruebas y validación humana de la cobertura.
4. **CONVERGE** final de la Feature: sin hallazgos pendientes que anexar.
5. **FEATURE COMPLETION AUTHORIZATION** humana y persistida.

El cierre formal no está autorizado en este documento: exige los gates anteriores conforme a la Constitución §8 y a `docs/development/sdd-workflow.md` §6.

---

**Bloqueo vigente**: ninguno para la implementación. La integración real con REQ-S2-03 es una dependencia técnica acotada al puerto `QuoteSource`, no un bloqueo. Los bloqueantes del cierre son de gates y de trazabilidad, no de funcionalidad.

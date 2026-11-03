# Implementation Plan: Vender tokens de un jugador (SPEC-011)

**Branch**: `feat/011-sell-player-tokens` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/011-sell-player-tokens/spec.md`

**Nota histórica de fase**: este documento se produjo en `Plan`; por sí solo no habilitó implementación.
Las fases posteriores ya se completaron mediante sus autorizaciones. Estado vigente y límites de
validación: tasks.md, testing-report.md y convergence-report.md.

## Summary

Implementar el mercado secundario de tokens: un usuario autenticado publica tokens que posee
mediante `POST /api/orders/sell`, lo que congela el precio unitario y reserva esa cantidad sin
transferirla; otro usuario compra esa publicación por su totalidad mediante `POST /api/orders/buy`
con identificador de publicación, lo que transfiere tokens y créditos de forma atómica, marca la
publicación como vendida y deja un registro financiero trazable a ella. Se mantienen las consultas
del mercado y del historial propio, la cancelación por el propietario y la compra directa al
superusuario de SPEC-010, que se conserva sin cambios.

El caso de uso se resuelve con dos fachadas de aplicación que dependen únicamente de Model,
Repository y `QuoteSource`, conforme a `docs/backend/architecture.md` §1.3 y §1.4. No se crea
Orchestrator: no hay coordinación entre casos de uso independientes.

## Technical Context

**Language/Version**: Java 21 (backend). Sin frontend en esta SPEC

**Primary Dependencies**: Spring Web MVC, Spring Data JPA, Spring Security OAuth2 Resource Server,
Hibernate, JJWT 0.13.0, Jakarta Validation, Lombok, Flyway, PostgreSQL. Puerto Integration
`QuoteSource` ya existente, reutilizado. **Sin dependencias nuevas.**

**Storage**: PostgreSQL exclusivamente. Una tabla nueva `token_listings` y una columna nullable
`listing_id` en `market_transactions`.

**Testing**: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers con PostgreSQL 18 real. Ejecución a
cargo del usuario; el agente escribe pruebas y no las ejecuta (Constitution §6).

**Target Platform**: Servidor Linux, JDK 21, PostgreSQL 18

**Project Type**: Backend REST API (Spring Boot)

**Performance Goals**: sin objetivo medible acordado por el equipo. Los objetivos de rendimiento
generales corresponden al proyecto (`tp/enunciado.md` §5.3). Conforme a Constitution §2.2, la
integridad concurrente es obligatoria y no depende de este objetivo, que no genera tarea. Se registra
el riesgo de contención heredado de SPEC-010 (H10) como advertencia, no como criterio.

**Constraints**:
- Sin dependencias Service → Service (`docs/backend/architecture.md` §1.3).
- Interfaz e implementación separadas para cada Service (§1.1).
- Importes en centésimas enteras con aritmética exacta; `CHECK` de no negatividad en base de datos.
- Orden total de bloqueos `user_credits` → `token_listings` → `player_tokens` (research.md R-01).
- Sin mercado de órdenes, sin emparejamiento, sin notificaciones, sin scheduler de expiración.
- Migraciones nuevas con timestamp UTC mediante `./gradlew createMigration`
  (`docs/development/flyway-migrations.md`).
- Vigencia de publicación: propiedad `market.listing.expiration-duration`, valor por defecto `PT24H`,
  declarada en `application.yml` y enlazada con `@Value` (research.md R-11).
- No se editan migraciones ya aplicadas.

**Scale/Scope**: cuatro endpoints nuevos (`POST /api/orders/sell`, `POST /api/listings/{id}/cancel`,
`GET /api/listings`, `GET /api/listings/mine`, `GET /api/listings/{id}`) más la variante de compra
existente; una tabla y una columna; dos migraciones; sin frontend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Resultado | Fundamento |
|---|---|---|
| **§1 Documentos normativos** | **PASS** | El diseño no contradice `docs/backend/architecture.md` ni `docs/backend/testing.md`. Toda decisión que los toca se cita en el apartado correspondiente. |
| **§2 Calidad y alcance** | **PASS** | Dos fachadas, dos casos de uso. No se crean servicios financieros intermedios, eventos, reservas persistidas ni extensibilidad futura. |
| **§2.1 Datos de desarrollo** | **PASS** | Sin columnas preservadoras ni capa de traducción. La única ampliación de una tabla existente responde a un requisito aprobado (FR-022), no a conservar datos. |
| **§2.2 Proporcionalidad** | **PASS** | Los casos RECOMENDADO del spec quedan como observaciones en research.md y no generan requisito ni tarea obligatoria. |
| **§3 Seguridad** | **PASS** | Autenticación y autorización existentes reutilizadas; el rol se lee de la persistencia. Los rechazos no revelan tenencias, saldos, reservas ni importes. |
| **§4 Contratos y sincronización** | **PASS tras diseño** | `market_transactions` amplía su esquema (research.md R-08). La misma tarea actualiza contrato, DTO, pruebas de SPEC-010 y documentación, como exige §4. La adaptación del DTO de compra de SPEC-010 es PENDIENTE-2 y se ejecuta dentro de esta feature con autorización expresa. |
| **§5 Idioma** | **PASS** | Código en inglés, documentación en español. |
| **§6 Verificación** | **PASS** | El agente escribe pruebas y no las ejecuta; el build permitido es `./gradlew build -x test` desde `backend/`. |
| **§7 Ramas** | **PASS** | `feat/011-sell-player-tokens`, conforme al formato. |
| **§8/§9 Gobernanza** | **PASS** | Sin ampliación unilateral de alcance funcional. Las decisiones funcionales ya estaban aprobadas en el spec. |

### Verificación específica del conflicto arquitectónico (PENDIENTE-3)

PENDIENTE-3 planteaba dos órdenes incompatibles: uno global que empieza por `user_credits` y otro
que hace que la compra empiece por `token_listings`. **Queda resuelto en research.md R-01 y en D-03**,
con un único orden global y la precisión de que cada operación toma **un subconjunto ordenado** de él:

- La compra secundaria **no** empieza por `token_listings`. Empieza por las cuentas, igual que la
  compra directa de SPEC-010. Para conocer al vendedor, la publicación se lee **sin bloqueo** y todo
  se revalida bajo el bloqueo de la fila; véase el flujo de 13 pasos más abajo.
- Publicar **no** bloquea `user_credits` ni `token_listings`: solo la tenencia del vendedor.
- Cancelar **no** bloquea cuentas ni tenencias: solo su fila de publicación.

Ninguna operación toma una categoría anterior después de haber tomado una posterior. Esa es la
condición que impide los ciclos entre las cuatro operaciones, y su alcance está acotado en R-01.

### Re-evaluación posterior al diseño (Phase 1)

| Principio | Resultado | Cambio respecto de la evaluación inicial |
|---|---|---|
| §4 Contratos | **PASS** | El diseño de Phase 1 confirmó que `market_transactions` admite la columna nullable sin alterar `CHECK`, índices ni cascadas existentes. |
| §1 Arquitectura | **PASS** | `TokenListingService` y `BuyOrderService` cumplen la matriz de dependencias sin excepciones. |
| §2 Calidad y alcance | **PASS** | No apareció ningún componente adicional durante el diseño de datos ni de contratos. |
| Resto de principios | **PASS** | Sin cambios. |

## Project Structure

### Documentation (this feature)

```text
specs/011-sell-player-tokens/
├── spec.md                 # Especificación funcional (Specify + Clarify)
├── plan.md                 # Este archivo
├── research.md             # Decisiones técnicas de Fase 0
├── data-model.md           # Modelo de datos de Fase 1
├── quickstart.md           # Guía de validación de Fase 1
├── contracts/
│   ├── listings.md         # Publicar, cancelar y consultar publicaciones
│   └── buy-listing.md     # Compra secundaria y convivencia con la compra directa
├── checklists/             # Fase CHECKLIST (no generada por Plan)
└── tasks.md                # Fase TASKS — NO generada por /speckit.plan
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── config/
│   └── (sin cambios de clases: SecurityConfig ya cubre las rutas)
├── controllers/
│   ├── ListingController.java                  # GET /api/listings, /mine, /{id}, POST /{id}/cancel
│   ├── OrderController.java                    # + POST /api/orders/sell
│   ├── dtos/requests/
│   │   ├── OrderSellRequestDTO.java            # playerId, quantity
│   │   ├── OrderBuyRequestDTO.java             # MODIFICADO: campo listingId opcional
│   │   ├── StrictIntegerDeserializer.java       # referencia del criterio (Integer); no interviene en listingId
│   │   └── StrictLongDeserializer.java           # T010: deserialización estricta de listingId (Long)
│   ├── dtos/responses/
│   │   ├── OrderSellResponseDTO.java           # publicación creada
│   │   ├── TokenListingResponseDTO.java        # detalle y elementos de listado
│   │   └── OrderBuyResponseDTO.java            # MODIFICADO: listingId opcional
│   ├── mappers/
│   │   ├── OrderMapper.java                    # + conversión de publicación
│   │   └── ListingMapper.java                  # DTO ↔ Model
│   └── exceptions/GlobalExceptionHandler.java   # + handlers nuevos
├── models/
│   ├── TokenListing.java                       # Publicación, con estado efectivo
│   ├── MarketTransaction.java                  # MODIFICADO: campo listing nullable
│   └── records/
│       ├── SellOrderRequest.java               # intención de publicar
│       ├── BuyListingRequest.java              # intención de comprar publicación
│       └── CurrentQuote.java                   # reutilizado sin cambios
├── repositories/
│   └── TokenListingRepository.java             # bloqueos, suma de reservas y consultas
├── services/
│   ├── TokenListingService.java                # interfaz de aplicación
│   ├── BuyOrderService.java                    # + buyListing
│   ├── impl/
│   │   ├── TokenListingServiceImpl.java
│   │   └── BuyOrderServiceImpl.java            # + camino de compra secundaria
│   └── exceptions/                             # excepciones nuevas de esta feature
└── security/
    └── (reutilizado sin cambios)

backend/src/main/resources/db/migration/
├── V<timestamp>__create_token_listings.sql
└── V<timestamp>__add_listing_to_market_transactions.sql

backend/src/test/java/footballmarket/
├── models/TokenListingTest.java
├── controllers/ListingControllerTest.java
├── repositories/TokenListingPersistenceTest.java
├── repositories/TokenListingConcurrencyTest.java
├── repositories/TokenListingExpirationPersistenceTest.java
├── repositories/TokenListingVisibilityPersistenceTest.java
├── services/TokenListingServiceTest.java
└── security/TokenListingAuthorizationTest.java
```

### Componentes existentes que cambian

| Componente | Cambio | Motivo |
|---|---|---|
| `OrderBuyRequestDTO` | Campo `listingId` opcional; `playerId` y `quantity` dejan de ser obligatorios en el caso de compra secundaria | FR-012, FR-013. Requiere la regla de validación que hoy produce `INVALID_REQUEST` en SPEC-010 |
| `OrderBuyResponseDTO` | Campo `listingId` opcional | FR-023 |
| `MarketTransaction` | Campo `listing` nullable y sobrecarga de la fábrica `record` | FR-021, FR-022 |
| `BuyOrderService` / `BuyOrderServiceImpl` | Nueva operación `buyListing`, con su camino transaccional | FR-013 a FR-021 |
| `GlobalExceptionHandler` | Handlers de los rechazos nuevos | FR-019, FR-025, FR-028 y códigos aprobados |
| `build.gradle.kts` | **Sin cambios**: el generador de migraciones ya existe |
| `OrderController` | + `POST /api/orders/sell` y anotaciones OpenAPI de la variante `listingId` | FR-001, FR-031 |

### Documentación OpenAPI (FR-032, Constitution §4)

El backend expone ya un contrato OpenAPI generado por Springdoc, con `SecurityRequirement(name =
"bearerAuth")` y `@ApiResponse` por código en cada operación. Esta feature debe seguir ese patrón, no
inventar otro.

| Endpoint | Descripción OpenAPI requerida |
|---|---|
| `POST /api/orders/sell` | `@Operation` con resumen y descripción que aclare que reserva sin transferir; `@ApiResponse` de 201, 400, 401, 403, 404, 409 y 500 |
| `GET /api/listings` | `@Operation` que aclare las exclusiones del mercado; 200 y 401 |
| `GET /api/listings/mine` | `@Parameter` del parámetro `status` con los cuatro valores exactos y su sensibilidad a mayúsculas; 200, 400 y 401 |
| `GET /api/listings/{id}` | `@Parameter` de `id`; 200, 401 y 404 |
| `POST /api/listings/{id}/cancel` | `@Operation` y `@Parameter` de `id`; 200, 401, 403, 404 y 409 |
| `POST /api/orders/buy` | Documentar la **variante `listingId`**: campo opcional en el request DTO, campo opcional en el response DTO y los códigos 403 `CANNOT_BUY_OWN_LISTING`, 404 `LISTING_NOT_FOUND` y 409 `LISTING_NOT_AVAILABLE` además de los ya documentados |

Los cambios aditivos en el contrato —`listingId` en request y response— se documentan de forma
explícita. Las categorías de error se expresan con `@Schema(implementation = ErrorResponseDTO.class)`
como ya hace `OrderController`. La verificación de que el documento generado coincide con el
comportamiento corresponde a la categoría Controller de `docs/backend/testing.md`.

**Inventario real de rutas del proyecto**: esta feature añade **cinco** rutas nuevas sobre las que
`OrderController` ya expone (`POST /api/orders/buy`). El proyecto expone además las rutas de
autenticación, de catálogo de jugadores, de detalle de jugador, de imágenes y de sincronizaciones. No
se modifican esas.

### Archivos de SPEC-010 a revisar durante la implementación

Constitution §4 exige actualizar en el mismo cambio los elementos afectados. **No se editan ahora**;
se enumeran con sus rutas reales verificadas para que `Tasks` los incluya:

| Ruta | Motivo |
|---|---|
| `specs/010-buy-tokens/contracts/buy-tokens.md` | El contrato establece que `quantity` ausente produce `400 INVALID_REQUEST`. Con `listingId`, esa regla solo sigue vigente en la modalidad directa |
| `specs/010-buy-tokens/data-model.md` | `market_transactions` incorpora la columna `listing_id` |
| `backend/src/main/java/footballmarket/controllers/dtos/requests/OrderBuyRequestDTO.java` | Campo opcional nuevo y relajación de las obligatoriedad |
| `backend/src/main/java/footballmarket/controllers/dtos/responses/OrderBuyResponseDTO.java` | Campo opcional nuevo en la respuesta |
| `backend/src/main/java/footballmarket/models/MarketTransaction.java` | Campo `listing` nullable |
| `backend/src/main/java/footballmarket/services/BuyOrderService.java` | Nueva operación `buyListing` |
| `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` | Camino transaccional nuevo |
| `backend/src/test/java/footballmarket/controllers/OrderControllerTest.java` | Cubre la validación de `quantity` ausente |
| `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` | Cubre la API pública de compra |

### Reglas de validación por modalidad (H-06)

La obligatoriedad de `playerId` y `quantity` **no se elimina**: se vuelve condicional al identificador
de publicación.

| Solicitud | Validación |
|---|---|
| `{"playerId": …, "quantity": …}` sin `listingId` | Modalidad directa. Ambos siguen siendo obligatorios; si falta alguno se mantiene el `400 INVALID_REQUEST` de SPEC-010 |
| `{"listingId": …}` | Modalidad secundaria. `playerId` y `quantity` son opcionales y se ignoran |
| `{"listingId": …, "playerId": …, "quantity": …}` | Modalidad secundaria. Gana `listingId`; los demás no influyen (FR-013) |
| `{}` o cuerpo sin ningún campo identificable | `400 INVALID_REQUEST` |

Ninguna regla nueva debilita la validación existente: la modalidad directa conserva exactamente su
comportamiento vigente, incluido el rechazo de `quantity` nula, decimal, exponencial, string o fuera del
rango de Integer.

### Validación estricta de `listingId` (H-05)

`listingId` se valida con el **mismo criterio estricto** que SPEC-010 aplica a `quantity`.

**Mecanismo verificado**: `StrictIntegerDeserializer` es un `ValueDeserializer<Integer>` que rechaza
todo nodo JSON que no sea `isIntegralNumber()` o que no sea convertible a `int`. **No es aplicable tal
cual a un campo `Long`**, porque su tipo genérico está fijado a `Integer`. Por eso la reutilización
requiere una variante tipada para `Long` —el mismo criterio de rechazo, aplicado al rango de `Long`—,
que es un componente nuevo de esta feature, no una integración existente.

| `listingId` recibido | Resultado |
|---|---|
| Ausente o `null` en una solicitud de compra secundaria | `400 INVALID_REQUEST` |
| `"25"`, `"abc"`, `true`, `false`, `[]`, `{}` | `400 INVALID_REQUEST` |
| `25.0`, `1e1` | `400 INVALID_REQUEST` |
| Entero fuera del rango representable de `Long` | `400 INVALID_REQUEST` |
| Entero `0` o negativo | `404 LISTING_NOT_FOUND`: es un identificador bien formado que no corresponde a ninguna publicación |
| Entero positivo | Continúa el caso de uso |

No hay coerción de tipos, comparación sin distinción de caja ni truncamiento.

**Structure Decision**: dos fachadas de aplicación porque publicar y comprar son casos de uso
independientes con fronteras transaccionales distintas, y comprar comparte camino con SPEC-010. La
estructura es la del backend existente; esta feature no introduce un árbol nuevo.

## Modelo de datos y contratos

El detalle completo está en [data-model.md](data-model.md), [research.md](research.md) y
[contracts/](contracts/). Resumen de decisiones estructurales:

| Elemento | Decisión |
|---|---|
| Tabla | `token_listings`, con `status` limitado a `ACTIVE`, `SOLD`, `CANCELLED` |
| `EXPIRED` | Estado efectivo derivado de `expires_at`, nunca persistido (R-04) |
| Reserva | Derivada de la suma de publicaciones activas y vigentes; sin columna de reserva (R-02, R-03) |
| Auditoría | `market_transactions.listing_id` nullable, sin cascada (R-08) |
| Bloqueos | Orden total `user_credits` → `token_listings` → `player_tokens`; publicar solo toma tenencias (R-01) |
| Migraciones | Dos archivos con timestamp UTC, sin editar migraciones aplicadas (R-05) |

## Flujo transaccional

**Qué significa «serializa» en este documento**: dos operaciones que compiten por el mismo registro
bloqueado se ejecutan **en serie sobre ese registro**. La segunda espera a que la primera confirme o
revierta, y entonces lee el estado ya confirmado. El alcance es el registro bloqueado, nunca el
sistema completo: serializar la fila de una publicación no implica bloquear operaciones sobre otras
publicaciones, y serializar la tenencia de un vendedor no implica bloquear a otros vendedores.

### Publicar (`POST /api/orders/sell`)

| # | Paso | Bloqueo real | Resultado |
|---|---|---|---|
| 1 | Resolver vendedor por el sujeto autenticado | — | Identidad persistida |
| 2 | Validar rol: `SUPERUSER` rechaza | — | 403 `SUPERUSER_CANNOT_SELL` |
| 3 | Validar cantidad entre 1 y 100 | — | 400 `INVALID_QUANTITY` |
| 4 | Existencia y visibilidad del jugador | — | 404 `PLAYER_NOT_FOUND` / `PLAYER_NOT_VISIBLE` |
| 5 | Leer cotización y congelar precio e importe | — | Precio unitario e importe exacto |
| 6 | Bloquear la tenencia del vendedor | `player_tokens` | Serializa publicaciones concurrentes |
| 7 | Calcular reservas vigentes bajo el bloqueo y revalidar disponibilidad | — | 409 `INSUFFICIENT_STOCK` |
| 8 | Insertar la publicación con `expires_at` = ahora más `market.listing.expiration-duration` | — | Sin transferencias ni transacción |
| 9 | Confirmar | — | Un solo commit |

Ninguna escritura de créditos ocurre en este camino (FR-001, SC-001).

### Comprar una publicación (`POST /api/orders/buy` con `listingId`)

La secuencia respeta el orden global `user_credits` → `token_listings` → `player_tokens` (D-03,
research.md R-01). Para conocer al vendedor **antes** de bloquear las cuentas, la publicación se lee
primero **sin bloqueo**; ese dato es únicamente orientativo y **no autoriza nada**: todas las
precondiciones se vuelven a verificar bajo el bloqueo de la fila.

| # | Paso | Bloqueo real | Resultado |
|---|---|---|---|
| 1 | Resolver comprador por el sujeto autenticado; validar rol | — | 403 si es `SUPERUSER` |
| 2 | Validar que `listingId` esté presente y sea un entero JSON válido | — | 400 `INVALID_REQUEST` |
| 3 | **Lectura sin bloqueo** de la publicación para conocer `seller_user_id` | — | 404 `LISTING_NOT_FOUND` si no existe o su jugador no es visible. Solo orienta el siguiente paso |
| 4 | Bloquear las cuentas del comprador y del vendedor en orden ascendente de `user_id` | `user_credits` | 409 `LISTING_NOT_AVAILABLE` si falta la cuenta de cualquiera de las dos partes |
| 5 | Bloquear la publicación por `id` | `token_listings` | Serializa compradores y cancelaciones concurrentes |
| 6 | **Revalidar bajo bloqueo** que el vendedor bloqueado en el paso 4 es el mismo que el de la lectura del paso 3 | — | 409 `LISTING_NOT_AVAILABLE` si cambió; nunca se liquida contra datos obsoletos |
| 7 | Comprobar propiedad y estado almacenado; insertar la tenencia del comprador si falta y bloquear ambas tenencias en orden ascendente de propietario. Aún no se obtiene ni evalúa el instante de vigencia | `player_tokens` | 403 `CANNOT_BUY_OWN_LISTING`, 409 `LISTING_NOT_AVAILABLE` por estado cerrado, o 409 `INSUFFICIENT_STOCK` si falta respaldo |
| 8 | **Con publicación y tenencias ya bloqueadas**, obtener `validatedAt` del reloj y decidir la vigencia una única vez: `validatedAt < expiresAt`. No se reutiliza ningún instante anterior a la espera por las tenencias | — | 409 `LISTING_NOT_AVAILABLE` si `validatedAt >= expiresAt`, con rollback completo |
| 9 | Revalidar saldo y cantidad del vendedor con cuentas y tenencias ya bloqueadas | — | 409 `INSUFFICIENT_CREDITS` / 409 `INSUFFICIENT_STOCK` |
| 10 | Transferir tokens y créditos de forma simétrica | — | Estados nuevos calculados |
| 11 | Marcar la publicación como `SOLD`, utilizando `validatedAt` como instante decisivo ya validado, sin leer nuevamente el reloj para comprobar vencimiento | — | Estado terminal |
| 12 | Insertar el registro financiero con `listing_id` | — | Un registro por venta |
| 13 | Confirmar conservando todos los bloqueos hasta commit o rollback | — | Un solo commit |

**Cómo se evita una liquidación basada en datos obsoletos**: el paso 3 no autoriza nada. El paso 6
comprueba que el vendedor cuya cuenta se bloqueó es el de la publicación bloqueada; si la publicación
cambió de vendedor entre la lectura y el bloqueo, la operación se rechaza. Los pasos 7 a 9 revalidan
estado, vigencia, saldo y stock exclusivamente contra los valores leídos **bajo bloqueo**; en particular,
la vigencia se decide solo en el paso 8, después de que termine cualquier espera por las tenencias.

**Qué se lee sin bloqueo y por qué no compromete la integridad**: solo `seller_user_id`, y únicamente
para seleccionar qué cuentas bloquear. Ninguna decisión de negocio se toma con ese valor.

**Garantía de compra única**: la fila de la publicación se bloquea en el paso 5 y permanece bloqueada
hasta el commit. Dos compradores concurrentes se serializan en ese paso; el segundo revalida en el
paso 7 y observa `SOLD`, por lo que recibe 409 `LISTING_NOT_AVAILABLE`. Una cancelación concurrente
se serializa en el mismo punto y se resuelve del mismo modo.

**Bloqueos adicionales no cubiertos por el orden global**: el paso 7 inserta la primera tenencia del
comprador cuando no existe. Esa inserción toma `FOR KEY SHARE` sobre `users` y `players`, como ya
ocurre en SPEC-010, y no pertenece a las tres categorías del orden global. Es compatible porque ocurre
**después** de haber tomado las categorías superiores y porque ninguna de las cuatro operaciones
adquiere esos bloqueos en sentido inverso. No se afirma una demostración general de ausencia de
interbloqueos; el alcance acotado de la garantía está en research.md R-01.

Cualquier excepción revierte la operación completa: no hay transferencias parciales, ni publicación
marcada, ni registro de una venta no confirmada (SC-009).

### Resolución documental de C1 — 2026-10-18

El diseño anterior decidía la vigencia antes de esperar por las tenencias. Una compra validada podía
quedar esperando mientras, tras el vencimiento, otra publicación reutilizaba esos tokens; liquidar
después esa compra dejaba reservas mayores que la tenencia. Se sustituye ese punto decisivo por el
paso 8 anterior, sin alterar el orden global ni introducir una segunda validación temporal.

La compra conserva el bloqueo de la tenencia del vendedor desde antes de la decisión temporal hasta
el final de la transacción. Publicar también obtiene ese bloqueo y **después** obtiene su propio
instante para calcular reservas y disponibilidad. En el aislamiento por defecto asumido por la Spec,
la lectura posterior al bloqueo utiliza los datos confirmados que dejó la operación precedente:

| Operación que obtiene primero la tenencia | Consecuencia para la otra operación |
|---|---|
| Una publicación conserva la tenencia mientras vence la oferta antigua y crea una nueva reserva | La compra espera; al adquirir las tenencias obtiene un instante nuevo. Si ya venció, rechaza con `LISTING_NOT_AVAILABLE` y revierte toda preparación, sin consumir los tokens de la reserva nueva |
| La compra obtiene las tenencias y supera la validación temporal antes de vencer | La publicación espera incluso si entretanto vence la oferta. Tras el commit de compra, calcula disponibilidad con la tenencia reducida y la oferta `SOLD`; tras rollback, con la tenencia y publicaciones restauradas |

Así se serializan las escrituras relevantes: una publicación no puede calcular disponibilidad usando
tokens que una compra validada aún puede transferir. No se añade reserva persistida, scheduler,
estado adicional ni bloqueo en orden inverso. La cancelación sigue tomando solo la publicación.
Las transferencias, `SOLD` y la auditoría mantienen la misma frontera transaccional.

**Estado histórico de esta corrección documental, superado por la implementación actual**: al
redactar este apartado, el código parcial conservaba el flujo anterior y T020/T035 estaban pendientes.
Después se autorizó la reanudación y se aplicó C1 en fuente; el estado vigente y sus evidencias están
en `tasks.md` e `implementation-review.md`. No se acreditan regresiones ejecutadas ni gates humanos
por la existencia de este apartado.

### Cancelar (`POST /api/listings/{id}/cancel`)

Bloquea únicamente la fila de la publicación, comprueba propietario y vigencia, y la marca como
`CANCELLED`. No toca cuentas ni tenencias: al ser la reserva derivada, la publicación deja de
computar al instante.

## Estrategia de pruebas

Verificación por responsabilidad, con los límites reales de `docs/backend/testing.md`. Las categorías
son las de su §2; no se inventan categorías nuevas. «Concurrencia» y «Reservas» son grupos `@Nested`
dentro de la categoría Repository, porque el estado que afirman solo es observable desde ahí.

| Categoría | Componentes | Qué afirma |
|---|---|---|
| Model | `TokenListing` | Transiciones, estado efectivo, no negativity, invariantes de cantidad e importe |
| Controller | `ListingController`, `OrderController` | Contrato HTTP, códigos, estructura de `ErrorResponseDTO`, no-revelación, validación de entrada, ausencia de sesión |
| Service | `TokenListingService`, `BuyOrderService` | Solo lo que devuelve o lanza la API pública, sin tocar Repository ni la base |
| Repository | `TokenListingPersistenceTest`, `MarketDomainPersistenceTest` | Reservas, expiración efectiva, transiciones, `CHECK`, secuencias de bloqueo, concurrencia, idempotencia de inicialización, reversión |
| Security | `TokenListingAuthorizationTest` | Que la autorización se decide por el rol persistido |

**Casos OBLIGATORIOS que fija el `spec.md`**: publicación y reserva simultáneas (AC14), doble
comprador (AC23), compra y cancelación simultáneas, fallo del registro financiero (AC26), falta de
respaldo del vendedor (AC30), vendedor sin cuenta de créditos (AC31), publicación de jugador no
visible no comprable (AC31 de FR-019), cancelaciones y expiración.

**Límite de cobertura de concurrencia, explícito**: las pruebas coordinan dos transacciones con
`CyclicBarrier` y liberan dentro de ellas. Eso evidencia que bajo simultaneidad real no hay
sobreventa, ni doble gasto, ni doble liquidación, y que la suma de reservas respeta la tenencia. No
evidencia contención efectiva de bloqueos de PostgreSQL ni el orden total de adquisición; demostrarlo
exigiría observar `pg_locks` desde una tercera transacción. Se registra como límite de cobertura
acordado, no como garantía acreditada, en coherencia con el precedente de SPEC-010.

**Cobertura específica adicional de C1 (T029/T031), implementada y ejecutada por el usuario**: las carreras de C1 deben
fijar el reloj y forzar ambos órdenes de adquisición de la tenencia. A diferencia de las carreras
anteriores de mera simultaneidad, la regresión debe acreditar que existe una espera real por la
tenencia antes de liberar al competidor; puede observar `pg_blocking_pids`/`pg_locks` desde una
transacción de test independiente en PostgreSQL de Testcontainers. No sustituir esa evidencia por
una barrera inicial, `Thread.sleep`, ausencia momentánea de resultado o espera al vencimiento real.
Los timeouts técnicos solo acotan un fallo de coordinación. No se añaden hooks productivos ni se
mockean Repository para simular bloqueos. Los dos escenarios y sus assertions completas están en
`quickstart.md`, subescenario 9c; T031 conserva además ambas ramas del instante decisivo y verifica
que no se revalida el vencimiento al final. Esto acredita la carrera concreta, no un orden de
bloqueos universal ni una prueba general de ausencia de interbloqueos.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Ninguna | — | — |

Componentes considerados y sustituidos por una solución más simple:

| Componente considerado | Sustitución adoptada |
|---|---|
| `TokenService`, `CreditService`, `MarketAuditService` | Operaciones internas de la fachada y Model |
| `ListingPurchaseService` separado | Ampliación de `BuyOrderService` con `buyListing` |
| `reserved_quantity` en `player_tokens` | Suma derivada de publicaciones (R-03) |
| Columna `operation_type` en `market_transactions` | Referencia nullable `listing_id`, suficiente para distinguir ambas modalidades (R-08) |
| Persistencia de `EXPIRED` | Estado efectivo derivado (R-04) |
| Tarea programada de expiración | Excluida por decisión funcional |
| Orchestrator | Innecesario: no hay coordinación entre casos de uso independientes |

## Dependencias

| Dependencia | Estado | Efecto |
|---|---|---|
| SPEC-001/003: identidad, autenticación, rol persistido | Existente | Se reutiliza sin cambios de contrato |
| SPEC-002/007: catálogo y predicado de visibilidad | Existente | Publicar exige visibilidad; el mercado y el detalle la replican |
| SPEC-010: dominio de mercado, representación monetaria, compra directa, auditoría | Existente | Se reutiliza; se amplía con la columna nullable y la variante de compra |
| `QuoteSource` (Integration) | Existente | Reutilizado para congelar el precio |
| Generador de migraciones con timestamp | Existente | `docs/development/flyway-migrations.md` y `backend/build.gradle.kts` |

### Advertencia operativa: estado de la base de desarrollo

La corrección histórica V9 → V10 renombró `V9__create_market_domain.sql` a `V10__create_market_domain.sql`
sin cambiar sus bytes. En la base de desarrollo local esa migración ya consta aplicada con versión
9, de modo que V10 figura pendiente y no puede aplicarse sin reconciliación previa. El documento
`docs/development/flyway-migrations.md` lo exige de forma explícita y prohíbe resolverlo
automáticamente. **Esta feature no modifica ninguna base**; la reconciliación corresponde a su
responsable antes de ejecutar las migraciones nuevas sobre un entorno con historial.

## Orden de integración

1. Migraciones, verificadas sobre una base desechable.
2. Modelos y repositorios.
3. Servicios, empezando por el camino de compra secundaria sobre `BuyOrderService`.
4. Contratos HTTP, DTO, mappers y handler de errores.
5. Documentación y trazabilidad.

## Fuera de alcance de esta fase

`tasks.md`, checklists, implementación, migraciones y pruebas se producen en fases posteriores.
Generar este plan no habilita `IMPLEMENT`: sigue requiriendo checklists evaluados, tareas,
`Analyze` y autorización humana expresa.

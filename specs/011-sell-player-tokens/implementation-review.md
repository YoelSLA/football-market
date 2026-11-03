# SPEC-011 — Entrega de IMPLEMENT y Code Review Reports

## Estado vigente — 2026-10-18

**Cierre posterior confirmado:** usuario aprobó el resultado final CONVERGED; autorización final
`6ad50693400c55a3098e9a48` persistida y verificada. Spec Approved; épica y US01–US04 DONE.
SDD 8/8 y 60/60 tareas. La mención inferior al gate final pendiente queda como antecedente superado.

US01–US04 DONE tras aprobación humana expresa de TESTING persistida/verificada, conservando
CODE REVIEW. Suite humana: 938 exitosos, cero fallidos/omitidos; C1/CR-01/SPEC-010 aceptados.
T058/T060 completas; equivalencias y límites en quickstart.md y testing-report.md.
Feature TESTING: CONVERGE satisfactorio tras reanudación autorizada con bash, sin hallazgos
accionables nuevos. El fallo 126 queda como historial. Gate final posterior al resultado pendiente;
no se declara Feature DONE ni Status Approved. Detalle: convergence-report.md.
La sección de gates siguiente y los informes originales se conservan como historia.

## Actualización de gates — 2026-10-18

El usuario aceptó las correcciones CR-01/02/03 y aprobó expresamente CODE REVIEW de US01–US04
después de revisar la evidencia disponible y C1 estático. Aprobación individual persistida y
transición a TESTING verificadas en `specs/trello.md`. **Solo CODE REVIEW aprobado**: no hay tests
compilados/ejecutados acreditados ni TESTING aprobado. T058/T060 continúan pendientes.
El estado y procedimiento vigentes están en [testing-report.md](testing-report.md).

CR-01 añade `controllers/dtos/requests/OrderBuyRequestDeserializer.java`, conecta el deserializador
en OrderBuyRequestDTO y amplía OrderControllerTest; no modifica C1 ni las transferencias.
CR-02 identifica como superado el estado histórico en plan.md; CR-03 corrige los nombres de la
matriz T057. Los informes y checklists siguientes se conservan como evidencia de la entrega
previa: su estado PENDING original no sustituye las nuevas aprobaciones. El build histórico
que citan es anterior a CR-01 y no verifica esa corrección. No se actualizan retrospectivamente
sus resultados de tests ni se aprueba el gate siguiente.

## Informe de entrega anterior (historial conservado)

**Fecha**: 2026-10-18. **Base de revisión**: archivos del árbol local de
`feat/011-sell-player-tokens`, incluidos los no rastreados; no hay commit de esta entrega.
**Estado**: PENDING HUMAN VALIDATION. Este informe no aprueba CODE REVIEW, TESTING ni CONVERGE.

## Resultado y límites de evidencia

- Se conservan los 60 IDs y las dependencias; 58 tareas realizadas y dos pendientes humanos:
  T058 (resultados del quickstart) y T060 (ejecución/evidencia de la suite).
- Los 34 marcadores previos se conservan. Los tests se consideran escritos y revisados en fuente,
  **no compilados ni ejecutados**. Marcar sus tareas de escritura no acredita resultados funcionales.
- C1 está corregido en fuente y tiene dos regresiones escritas; su eficacia en PostgreSQL sigue
  pendiente de ejecución humana. No se detectó otra contradicción funcional durante esta revisión.
- Build final permitido observado: `./gradlew build -x test`, **BUILD SUCCESSFUL, 6 s**.
  Los intentos anteriores fallaron por formato de Spotless, corregido en las fuentes.
  Ese build compila producción y revisa formato, pero no compila ni ejecuta tests.
- Inspección final: IDs T001–T060 únicos y conservados; checklists del revisor 44/44, 38/38 y
  41/41, sin edición. No existe `.specify/extensions.yml`: sin hooks de extensión que ejecutar.
- No se aplicaron migraciones, modificaron bases, iniciaron servicios, hicieron commits o push
  ni operaciones Git mutantes. No se incorporaron dependencias ni cambios frontend.
- Las cuatro US se entregan para CODE REVIEW humano; la Feature continúa IN PROGRESS.
  Los siete LOW previamente aceptados siguen siendo residuales; I4 quedó corregido editorialmente.

## Corrección C1 y decisiones comunes

En `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java`, `buyListing`:

1. Lee solo una proyección de identidad para orientar los bloqueos, sin cargar una publicación
   potencialmente obsoleta en el contexto JPA.
2. Bloquea cuentas por ID ascendente y después la publicación; comprueba identidad, propiedad,
   visibilidad y estado almacenado, sin decidir todavía su vencimiento.
3. Prepara la primera tenencia del comprador y bloquea las tenencias por propietario ascendente.
4. Obtiene el único `validatedAt` **después** de ambas tenencias. Si alcanzó `expiresAt`, lanza
   `ListingNotAvailableException` y revierte también la preparación de la tenencia.
5. Revalida saldo y stock, transfiere, marca SOLD y registra auditoría dentro de la misma
   transacción. El cierre reutiliza el instante validado; no vuelve a consultar el reloj.

El límite `@Transactional` conserva los bloqueos hasta commit/rollback. El orden es
`user_credits → token_listings → player_tokens`. Publicar usa el subconjunto tenencia del vendedor:
espera su bloqueo antes de obtener el instante y sumar reservas efectivas. No bloquea publicaciones
para calcular esa suma, ni puede reutilizar tokens de una compra que todavía retiene la tenencia.
Cancelar solo bloquea la publicación; no transfiere créditos ni tokens.

En `backend/src/test/java/footballmarket/repositories/TokenListingConcurrencyTest.java`, grupo
`ExpirationReservationRace`, los dos escenarios 9c usan reloj controlado, transacciones separadas,
PID de cada participante y un observador JDBC de `pg_blocking_pids`/`pg_stat_activity`. Las señales
tienen timeouts técnicos; no usan `Thread.sleep` ni esperan al vencimiento real:

- `rechazaCompraVencidaDuranteEspera`: publicación retiene tenencia, compra espera realmente,
  reloj alcanza vencimiento, nueva publicación reserva 20 y la compra antigua revierte sin recursos
  ni auditoría secundaria. Comprueba ausencia de la tenencia preparada y reserva 20/tenencia 20.
- `conservaTenenciaHastaCommit`: compra válida retiene transacción tras retornar, reloj avanza,
  publicación de 20 espera realmente y, después del commit, rechaza contra tenencia 15.
  Verifica SOLD, transferencia, suma de 100 tokens y una auditoría; republicar 15 es control positivo.

`TokenListingExpirationPersistenceTest` complementa la frontera exacta y exige una sola consulta
al reloj. Las carreras de dos publicaciones, dos compradores y compra/cancelación permanecen.
Estas pruebas no demuestran ausencia universal de interbloqueos; requieren Docker y PostgreSQL
desechable de Testcontainers cuando las ejecute el usuario.

## Inventario completo de implementación

Las rutas relativas de las tablas siguientes se resuelven desde **`backend/src/main/java/footballmarket/`**.
«Nuevo» significa archivo añadido por la implementación acumulada; varios ya existían sin rastrear
antes de esta reanudación. No se atribuye su creación a la última corrección.

| Archivo | Tipo / US | Responsabilidad y motivo |
| --- | --- | --- |
| `config/MarketTimeConfiguration.java` | Nuevo / común | Reloj UTC inyectable para vencimiento determinista. |
| `models/TokenListing.java` | Nuevo / común | Precio congelado, importe exacto, invariantes, transiciones, estado efectivo y snapshot. |
| `models/enums/TokenListingStatus.java` | Nuevo / común | ACTIVE, SOLD, CANCELLED y EXPIRED efectivo. |
| `models/exceptions/InvalidTokenListingException.java` | Nuevo / común | Error de dominio para invariantes y transiciones inválidas. |
| `models/records/ListingIdentity.java` | Nuevo / US02 | Proyección de vendedor/jugador que orienta bloqueos sin entidad previa. |
| `models/records/SellOrderRequest.java` | Nuevo / US01 | Intención de publicación independiente de HTTP. |
| `models/records/BuyListingRequest.java` | Nuevo / US02 | Intención de compra completa. |
| `models/records/TokenListingView.java` | Nuevo / US03–04 | Snapshot con estado efectivo y cierre. |
| `repositories/TokenListingRepository.java` | Nuevo / común | Bloqueo, suma de reservas, consulta del mercado y propias. |
| `services/TokenListingService.java` | Nuevo / US01,03,04 | Contrato productivo de publicación, lectura y cancelación. |
| `services/impl/TokenListingServiceImpl.java` | Nuevo / US01,03,04 | Transacciones, disponibilidad bajo tenencia, visibilidad, filtro exacto y propietario. |
| `services/BuyOrderService.java` | Modificado / US02 | Añade buyListing sin retirar buy directo. |
| `services/impl/BuyOrderServiceImpl.java` | Modificado / US02 | Liquidación secundaria, C1 y reutilización de transferencias; método buy directo preservado. |
| `services/exceptions/SuperuserCannotSellException.java` | Nuevo / US01 | Prohibición por rol persistido. |
| `services/exceptions/CannotBuyOwnListingException.java` | Nuevo / US02 | Prohibición de compra propia. |
| `services/exceptions/ListingNotFoundException.java` | Nuevo / US02–04 | Ausencia o inaccesibilidad sin revelación. |
| `services/exceptions/ListingNotAvailableException.java` | Nuevo / US02,04 | Constructor común para SOLD/CANCELLED/EXPIRED/cuenta receptora ausente. |
| `services/exceptions/InvalidListingStatusException.java` | Nuevo / US03 | Filtro estricto sin conversión de mayúsculas. |
| `services/exceptions/InvalidOrderRequestException.java` | Nuevo / US02 | Rechazo controlado de solicitud de compra incompleta. |
| `models/MarketTransaction.java` | Modificado / US02 | Referencia nullable y fábrica adicional; firma directa original intacta. |
| `controllers/OrderController.java` | Modificado / US01–02 | POST sell crea; POST buy selecciona modalidad por listingId; OpenAPI de ambas. |
| `controllers/ListingController.java` | Nuevo / US03–04 | Tres lecturas y cancelación; no expone POST de creación en listings. |
| `controllers/dtos/requests/OrderSellRequestDTO.java` | Nuevo / US01 | Estructura y cantidad estricta de publicación. |
| `controllers/dtos/requests/StrictLongDeserializer.java` | Nuevo / US02 | Long JSON integral sin coerción, con rechazo de overflow. |
| `controllers/dtos/requests/OrderBuyRequestDTO.java` | Modificado / US02 | Campos opcionales y obligación condicional; constructor Java directo conservado. |
| `controllers/dtos/responses/OrderSellResponseDTO.java` | Nuevo / US01 | Respuesta 201 sin datos financieros privados. |
| `controllers/dtos/responses/OrderBuyResponseDTO.java` | Modificado / US02 | listingId opcional, omitido en modalidad directa. |
| `controllers/dtos/responses/TokenListingResponseDTO.java` | Nuevo / US03–04 | Diez campos del detalle, incluido closedAt nullable. |
| `controllers/dtos/responses/TokenListingSummaryResponseDTO.java` | Nuevo / US03 | Nueve campos de listado, sin closedAt. |
| `controllers/dtos/responses/TokenListingsResponseDTO.java` | Nuevo / US03 | Sobre listings de las colecciones. |
| `controllers/mappers/ListingMapper.java` | Nuevo / US01,03,04 | Conversión DTO/modelo sin reglas de negocio. |
| `controllers/mappers/OrderMapper.java` | Modificado / US02 | Solicitud secundaria y referencia opcional de respuesta. |
| `controllers/exceptions/GlobalExceptionHandler.java` | Modificado / común | Rechazos nuevos, filtro reutilizando handler global y fallo interno genérico sin detalle SQL. |

Otros archivos productivos:

| Ruta | Tipo / motivo |
| --- | --- |
| `backend/src/main/resources/application.yml` | Modificado: market.listing.expiration-duration=PT24H y variable de entorno. |
| `backend/src/main/resources/db/migration/V20261017233439__create_token_listings.sql` | Nueva: tabla, CHECK, dos FK NO ACTION e índices; no aplicada. |
| `backend/src/main/resources/db/migration/V20261017233445__add_listing_to_market_transactions.sql` | Nueva: listing_id nullable y FK sin cascada; no aplicada. |

### Inventario de tests

Rutas relativas a **`backend/src/test/java/footballmarket/`**. Todos pendientes de compilación y ejecución humanas.

| Archivo | Tipo / cobertura |
| --- | --- |
| `models/TokenListingTest.java` | Nuevo: publicación, importes, overflow, frontera temporal y transiciones terminales. |
| `repositories/ListingRepositorySupport.java` | Nuevo: fixtures compartidos, reloj y transacciones de categoría Repository. |
| `repositories/TokenListingPersistenceTest.java` | Nuevo: reservas sin movimientos, CHECK, FK, cuenta vendedora ausente, stock/saldo insuficientes, estados indistinguibles, filtros y cancelación. |
| `repositories/TokenListingConcurrencyTest.java` | Nuevo: exclusión de publicaciones/compras/cancelación y ambas carreras C1. |
| `repositories/TokenListingExpirationPersistenceTest.java` | Nuevo: frontera, rollback de preparación y una sola lectura del reloj. |
| `repositories/TokenListingVisibilityPersistenceTest.java` | Nuevo: pérdida posterior de visibilidad, acceso propio e historia conservada. |
| `repositories/MarketDomainPersistenceTest.java` | Modificado: liquidación, referencia nullable, sumas, rollback real ante fallo de auditoría y compra no visible. |
| `services/TokenListingServiceTest.java` | Nuevo: API pública de publicación/lectura/cancelación, precio congelado y rechazos. |
| `services/BuyOrderServiceTest.java` | Modificado: compra secundaria, precio congelado, compra propia y compatibilidad directa. |
| `controllers/ListingControllerTest.java` | Nuevo: POST sell, tres lecturas, cancelación, contratos, seguridad HTTP y ausencia de creación alternativa. |
| `controllers/OrderControllerTest.java` | Modificado: selección secundaria, identificador estricto, errores y respuesta directa sin listingId. |
| `security/TokenListingAuthorizationTest.java` | Nuevo: JWT real y rol persistido frente a claims engañosos; SUPERUSER no publica ni compra. |

Los Service usan persistencia real y QuoteSource simulado; MarketTestData solo prepara en Arrange.
Las assertions durables y SQL están en Repository. Controller usa Services simulados y cadena
Security real. Security usa autenticación/servicios reales y Testcontainers, sin Repository directo.
No se simulan repositorios. El reloj se controla donde el vencimiento determina el resultado.

### Documentación afectada

- `specs/010-buy-tokens/contracts/buy-tokens.md` y `specs/010-buy-tokens/data-model.md`:
  ampliación aditiva para modalidad secundaria y listing_id nullable, sin retirar contrato directo.
- `specs/011-sell-player-tokens/contracts/listings.md`: explicita ACCESS_DENIED ya usado por el
  handler global para cancelación ajena; no modifica la regla de autorización.
- `specs/011-sell-player-tokens/tasks.md`, `quickstart.md` y este informe: avance, cobertura y límites
  reales de verificación, diferenciados de escenarios todavía no ejecutados.
- `specs/trello.md`: reanudación, nueva evidencia y sincronización de tareas/estados.
- Se conservan sin reescribir en esta reanudación `spec.md`, `plan.md`, `research.md`,
  `data-model.md`, `contracts/buy-listing.md` y los checklists del revisor. Sus cambios previos y
  archivos no rastreados forman parte de la base revisable y no se descartan.

## Code Review Report — US01: publicar

**Alcance**: T013–T025 y prerrequisitos comunes T001–T012. Fuentes de US01 en el inventario.
POST `/api/orders/sell` resuelve vendedor persistido, valida rol/cantidad/jugador/cotización,
bloquea tenencia y calcula disponibilidad después de cualquier espera. Guarda precio y vencimiento
sin transferencias ni auditoría financiera. T016 afirma retorno/excepciones mediante Service;
la promesa durable de no alterar créditos se verifica en Repository, sin eludir la frontera Service.

**Revisar en orden**: contrato listings → TokenListing → migración y Repository → publish del
Service → DTO/Mapper/OrderController → Model, Persistence, Concurrency.Publication,
Service.Publication, Controller.Publication y Security.PersistedRole.
**Puntos delicados**: ninguna reserva vencida computa; el reloj de publicación se lee bajo tenencia;
cotización inválida no tiene fallback; dos reservas no pueden superar stock.

- [ ] Revisé los archivos creados/modificados de US01 y sus prerrequisitos, incluidos tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes, especialmente la serialización de reservas C1.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar US01 a TESTING.

## Code Review Report — US02: comprar

**Alcance**: T026–T041, con US01 como dependencia técnica. El flujo y la corrección C1 están
explicados arriba. La compra es total; usa el snapshot congelado y no consulta otra cotización.
Los rechazos por cierre, vencimiento y cuenta receptora ausente son indistinguibles. El método
`buy` directo y la fábrica original de MarketTransaction permanecen, con referencia nula/omitida.

**Revisar en orden**: buy-listing/plan → buyListing → MarketTransaction y segunda migración →
DTO/OrderMapper/OrderController/handler → ExpirationReservationRace → ExpirationPersistence →
MarketDomainPersistence.SecondarySettlement y PurchaseRejections → BuyOrderServiceTest y
OrderControllerTest, incluidos los casos directos existentes.
**Puntos delicados**: obtención tardía de validatedAt, retención hasta commit, rollback de primera
tenencia, auditoría atómica, ningún saldo/stock negativo y no exposición de razones privadas.
Las pruebas C1 deben fallar si se vuelve a validar antes de bloquear tenencias.

- [ ] Revisé los archivos creados/modificados de US02, incluidos tests y compatibilidad SPEC-010.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes, C1 y las dos regresiones 9c.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar US02 a TESTING.

## Code Review Report — US03: consultar

**Alcance**: T042–T049. Mercado excluye propias, cerradas, vencidas y jugadores no visibles.
Propias filtra solo por estado efectivo, sin excluir jugadores ocultos; acepta las cuatro formas
exactas. El dueño conserva detalle e historia; terceros reciben 404 indistinguible si no acceden.

**Revisar en orden**: contrato listings → Repository.findMarket/findBySellerId → market/mine/detail
→ TokenListingView/DTO/ListingMapper/ListingController → Persistence.Queries,
VisibilityPersistence, Service.Queries y Controller.Queries.
**Puntos delicados**: frontera exacta, ausencia de persistencia de EXPIRED, filtro independiente
de visibilidad y sobres de nueve/diez campos, respectivamente.

- [ ] Revisé los archivos creados/modificados de US03, incluidos tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes de estado efectivo y visibilidad.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar US03 a TESTING.

## Code Review Report — US04: cancelar

**Alcance**: T050–T054; trabajo secuencial posterior a US03. Cancelar bloquea publicación,
comprueba dueño y vigencia, marca CANCELLED con cierre y conserva historia. La reserva se libera
por derivación, sin movimientos financieros ni de tenencias.

**Revisar en orden**: contrato listings → cancel del Service → Controller.cancel →
Persistence.Cancellation → método `compiteConCancelacion` de Concurrency.Purchases
→ Service.Cancellation y Controller.Cancellation.
**Puntos delicados**: tercero no cancela, cierre terminal no se repite, vencimiento se rechaza,
republicación posible tras cancelar y exclusión con compra concurrente.

- [ ] Revisé los archivos creados/modificados de US04, incluidos tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes de cancelación y concurrencia.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar US04 a TESTING.

## T057 — Cobertura de los ocho casos obligatorios

| Caso obligatorio de spec.md | Tarea / regresión escrita | Ejecución |
| --- | --- | --- |
| Reservas simultáneas | T015 / evitaSobreventa | Pendiente humana |
| Compra contra cancelación | T029 / compiteConCancelacion | Pendiente humana |
| Dos compradores | T029 / liquidaUnaVez | Pendiente humana |
| Fallo de auditoría | T028 / revierteFalloDeAuditoria | Pendiente humana |
| Stock excepcional reducido | T030 / rechazaTenenciaInsuficiente | Pendiente humana |
| Expiración efectiva | T013/T014/T043 / calculaFronteraExacta, expiraReserva, filtraEstadoEfectivo | Pendiente humana |
| Vencimiento durante compra y reserva C1 | T029/T031 / dos casos ExpirationReservationRace, rechazaFrontera, noRevalidaAlFinal; quickstart 9b/9c | Pendiente humana |
| Pérdida de visibilidad | T032/T044 / ocultaJugadorNoVisible, conservaHistorialPropio | Pendiente humana |

No se confunde cobertura prevista (tasks/quickstart) con fuentes escritas (tabla) ni con ejecución
(ninguna aportada). La inspección no sustituye al compilador ni a los resultados de PostgreSQL.

## Siguiente paso humano

Aprobar o rechazar explícitamente el checklist de CODE REVIEW **por cada US**. Todavía no se pide
aprobar TESTING. Tras CODE REVIEW aprobado se entrega el Testing Report y se solicita al usuario
ejecutar, desde `backend/`, la suite `./gradlew test` (Docker disponible, PostgreSQL aislado de
Testcontainers; nunca la base de desarrollo). Ante fallo, aportar la salida relevante.
T058/T060 permanecen pendientes hasta recibir evidencia; CONVERGE y cierre no están habilitados.

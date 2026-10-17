# Research: Comprar tokens de un jugador (REQ-S2-06)

Decisiones técnicas de la consolidación del 2026-10-16. Cada decisión indica los motivos y las alternativas descartadas. Este documento no introduce requisitos funcionales: la verdad funcional es [spec.md](spec.md).

**Contexto verificado en el repositorio**: no existen `QuoteService`, `QuoteSource`, `UserCredit`, `PlayerToken`, `MarketTransaction`, `PlayerTokenEmission` ni un repositorio de cotizaciones. Tampoco existe configuración de JPA Auditing, Envers o Spring Data JPA Auditing en el backend. Las migraciones vigentes son V1–V8, por lo que la siguiente versión disponible es **V9**.

---

## R-01. Arquitectura de una sola fachada

*Origen*: `docs/backend/architecture.md` §1.1, §1.3 y §1.4.

**Decisión**: la compra se implementa con `BuyOrderService` (interfaz) y `BuyOrderServiceImpl` (implementación). La implementación depende de Model y Repository, y comparte un único límite transaccional. No existen `TokenService`, `CreditService` ni `MarketAuditService`.

**Razón**: §1.3 permite a un Service depender de Model, Repository e Integration. La prohibición de dependencias entre Services hace inviable invocar servicios financieros auxiliares desde la fachada. §1.4 permite explícitamente colaboradores internos que componen un caso de uso, no exponen interfaz de aplicación, no son consumidos por otros Services y comparten el límite transaccional.

**Colaboradores internos admisibles**: solo si aislan una responsabilidad concreta con dos o más pasos, por ejemplo la preparación de bloqueos. No se crean por simetría ni por nomenclatura.

**Alternativas descartadas**:

| Alternativa | Motivo del descarte |
|---|---|
| Servicios financieros separados | Dependencia Service → Service, prohibida por §1.3. |
| Orchestrator de la compra | §1.1 le prohíbe ejecutar lógica de dominio, cálculos o decisiones sobre Model. No hay además coordinación entre casos de uso independientes. |
| Servicios auxiliares "puente" que delegan a otros Services | Eludiría la restricción mediante intermediarios, prohibido por `docs/architecture.md` §1. |
| Servicio de auditoría separado | Su única función es escribir una fila dentro de la transacción de la compra. Modelo y repositorio bastan. |

---

## R-02. Identidad, vendedor y precios

*Origen*: FR-002, FR-003, FR-009, FR-013.

**Decisión**:

- El comprador se resuelve por el sujeto autenticado mediante `UserRepository`.
- El vendedor se resuelve con `UserRepository.findByRole(UserRole.SUPERUSER)`.
- El precio se obtiene del puerto Integration `QuoteSource`, cuya integración definitiva es el repositorio canónico de cotizaciones de REQ-S2-03.

**Razón**: la matriz de dependencias permite a la fachada usar Repository e Integration. Invocar `AuthenticationService` o `SuperuserProvisioningService` desde la compra introduciría dependencias entre Services no autorizadas. El rol se lee de la persistencia y no de los claims del token, conforme a §3 de la Constitución y al contrato vigente.

**Consecuencia**: la aprovisionación del superusuario es una dependencia de configuración. Si no existe superusuario, la compra no puede identificar al vendedor y **debe fallar de forma explícita, nunca con una compra parcial**.

Comportamiento vigente, tras la aprobación del equipo (2026-10-16, Constitution §2.2 como caso OBLIGATORIO de integridad transaccional): `BuyOrderServiceImpl` resuelve el vendedor con `UserRepository.findByRole(UserRole.SUPERUSER)` **antes** de tomar cualquier bloqueo o mover recursos. Si no resuelve, lanza `SellerUnavailableException` (excepción técnica) y la operación revierte completa, sin débitos, sin tenencias creadas o modificadas y sin auditoría. La respuesta pública es el **500 `INTERNAL_ERROR` genérico ya definido en `contracts/buy-tokens.md`**; no se crea un código HTTP ni un error propio. Verificado en `MarketDomainPersistenceTest`, grupo `@Nested VendedorAusente` (AC19, FR-023).

La comprobación no puede residir en `SuperuserProvisioningService`, porque invocarlo desde la compra introduciría una dependencia Service → Service prohibida por `docs/backend/architecture.md` §1.3. La resolución directa por `UserRepository` es coherente con el resto de la decisión.

---

## R-03. Unidad monetaria: centésimas enteras

*Origen*: CL-01, FR-008, FR-022.

**Decisión**: todos los importes se representan en centésimas de crédito con `long` en Java y `BIGINT` en PostgreSQL. 1 crédito = 100 centésimas. El precio inicial del token es de 1 crédito, según el enunciado §3.3.

**Razón**: CL-01 fue ratificada por el equipo y aparece en AC01, FR-008 y en el contrato HTTP, donde `unitPrice=100` representa 1.00 crédito. La representación entera elimina el punto flotante, serializa sin sorpresas y hace la aritmética exacta trivial. Se descarta una representación decimal porque obligaría a modificar una aclaración funcional aprobada, su criterio de aceptación y el contrato público.

**Aritmética**: el importe se calcula una sola vez con `Math.multiplyExact(quantity, unitPrice)`, y los saldos resultantes con `Math.subtractExact` y `Math.addExact`. Todo desbordamiento produce una excepción interna que revierte la operación completa y responde 500 genérico (D4, FR-022).

**Alternativas descartadas**: `double` y `float` por representación binaria imprecisa; `BigDecimal` y `NUMERIC(19,2)` por conflicto con CL-01.

---

## R-04. Modelo de dominio y invariantes

*Origen*: FR-012, FR-013, FR-017.

**Decisión**: las operaciones de dominio viven en Model y se invocan sobre entidades ya cargadas bajo bloqueo.

- `UserCredit.debit(amount)` y `UserCredit.credit(amount)`: exigyen saldo suficiente y no negativo.
- `PlayerToken.transferOut(quantity)` y `PlayerToken.receive(quantity)`: exigen cantidad positiva y no negativa.

**Razón**: §1.4 asigna al Model las reglas resolubles con información del dominio. Encapsular las operaciones garantiza que las invariantes se respeten sin depender del orden de llamadas en la fachada.

**Verificación de la invariante**: se sostiene por emisión única de 100 tokens y por transferencia simétrica. No se ejecuta una consulta `SUM(quantity)` en cada compra: no es necesaria para garantizar la invariante y añade una lectura global en el camino crítico.

---

## R-05. Cotización vigente y simulador temporal

*Origen*: FR-007, CL-02, CL-03.

**Decisión**: la compra obtiene el precio a través de `QuoteSource`, un puerto **Integration** que la desacopla de la implementación concreta de cotizaciones.

**Ciclo de vida**: el puerto se conserva mientras siga aportando esa separación. `SimulatedQuoteSource` es el componente estrictamente temporal y se elimina cuando REQ-S2-03 exista. Eliminar el puerto sería una refactorización explícita, no una consecuencia automática de la llegada de REQ-S2-03, y no está prevista.

**Por qué un Integration y no un Service**: `QuoteSource` no expone un caso de uso propio, solo lo consume `BuyOrderServiceImpl` y tiene un único consumidor. Ubicado en `services/` sería un Service y produciría una dependencia Service → Service, prohibida por §1.3. Como Integration, la dependencia está permitida por la misma matriz.

**Contrato mínimo esperado** (documentado en `contracts/quote-source.md`): `playerId`, precio en centésimas, instante de cálculo y versión de estrategia. `strategyVersion` admite `null` porque ninguna exigencia de SPEC-010 la requiere y la fuente real aún no está implementada; se conserva la nulabilidad de `quote_strategy_version` en la auditoría. La estructura de la tabla de cotizaciones pertenece a REQ-S2-03 y **no se define aquí**.

**Invariante de correspondencia**: `CurrentQuote.playerId` debe coincidir con el `playerId` solicitado. `BuyOrderServiceImpl` lo verifica antes de usar el precio; si no coincide, es un incumplimiento interno del contrato de integración y produce 500 genérico con reversión completa, sin exponer detalles técnicos. No se crea un servicio de validación de cotizaciones.

**Restricciones del simulador**:

| Restricción | Motivo |
|---|---|
| Limitado a desarrollo y pruebas | No debe existir en producción. |
| Desactivado por defecto mediante propiedad explícita | Evita desplegar precios simulados inadvertidamente. |
| No se registra si existe una fuente real | No puede desplazar la integración verdadera. |
| No crea tablas de cotizaciones | SPEC-010 no define el esquema de cotizaciones. |
| No sustituye una cotización real ausente | Si hay fuente y falta la cotización, responde 409 `QUOTE_UNAVAILABLE`. |

**Selección de la fuente**: se resuelve una sola vez en el arranque.

| Estado del contexto | Comportamiento |
|---|---|
| Fuente real disponible | La compra usa la cotización vigente |
| Sin fuente real y simulador habilitado | La compra usa el precio configurado, identificado como simulado |
| Sin fuente real y simulador deshabilitado | El arranque falla con error de configuración explícito |

**Dos ausencias que no deben confundirse**:

| Situación | Resultado |
|---|---|
| REQ-S2-03 no implementado y simulador deshabilitado | La aplicación no inicia |
| Fuente disponible sin cotización para un jugador | 409 `QUOTE_UNAVAILABLE` |

El simulador no interviene en ninguno de los dos casos.

**Alternativas descartadas**: simulación por perfil, que podría activarse en un entorno no previsto; precio por defecto silencioso, que produciría compras con datos ficticios; esquema de cotizaciones ficticio en esta SPEC, que duplicaría la responsabilidad de REQ-S2-03; anticipar el tipo del repositorio canónico, que inventaría un contrato ajeno.

---

## R-06. Concurrencia: bloqueos en orden determinista

*Origen*: FR-011, FR-012, FR-013, FR-017, AC08, AC09.

**Decisión**: dentro de una única transacción:

1. Resolver identidad, rol, cantidad, jugador y cotización.
2. Calcular el importe una vez.
3. Bloquear las cuentas de comprador y vendedor ordenadas por `user_id` ascendente.
4. Crear la primera tenencia del comprador con cantidad 0 mediante `INSERT ... ON CONFLICT DO NOTHING`.
5. Bloquear las tenencias de comprador y vendedor ordenadas por propietario ascendente.
6. Revalidar saldo y stock con los valores protegidos.
7. Transferir y registrar la auditoría.

**Razón**: el orden cuentas → tenencias es consistente en todas las compras. Como toda compra toca la cuenta del mismo vendedor, fijar ese orden total por clave evita ciclos entre compradores concurrentes.

**Efectos de bloqueo verificados**:

| Operación | Bloqueo real |
|---|---|
| `SELECT ... FOR UPDATE` sobre `user_credits` | `FOR UPDATE` sobre las cuentas existentes |
| `INSERT ... ON CONFLICT DO NOTHING` sobre la tenencia | `FOR KEY SHARE` sobre `users` y `players`; espera ante inserción concurrente sin confirmar |
| `SELECT ... FOR UPDATE` sobre `player_tokens` | `FOR UPDATE` sobre la fila del vendedor y del comprador |

**Precisión crítica**: `ON CONFLICT DO NOTHING` **no** bloquea la fila preexistente cuando existe conflicto; la inserción se descarta. Por eso el paso 5 no es redundante: aporta el bloqueo de la tenencia del comprador cuando la fila ya existía. Si se eliminara, la revalidación de saldo o stock podría operar sobre una fila no protegida.

**Regla de lectura**: el saldo y el stock se leen únicamente desde las consultas de bloqueo. Una lectura anterior queda cacheada en el contexto de persistencia y puede devolver un valor obsoleto aunque la fila se bloquee después.

**Interacción con la emisión**: la emisión escribe `player_token_emissions` y `player_tokens`, y no toca `user_credits`. Como la compra toma primero las cuentas y después las tenencias, no se forma un ciclo con la sincronización.

**Interacción con la desactivación**: la inserción de la tenencia toma `FOR KEY SHARE` sobre `players`. La actualización de `players.active` toma `FOR NO KEY UPDATE`, compatible con ese bloqueo. La compra no bloquea la sincronización ni queda bloqueada por ella, lo que confirma técnicamente D3.

**Orden de las consultas**: los identificadores se ordenan en la aplicación y se bloquean por clave primaria en ese orden. Una consulta con `IN (...)` y `ORDER BY` no garantiza por sí sola la secuencia de adquisición de bloqueos.

**Alcance**: el orden total evita ciclos. No se afirma ausencia de esperas: dos compras simultáneas pueden esperar la una a la otra mientras la primera confirma. Esa espera no es un fallo.

**Sin reintentos**: ante un conflicto de bloqueo la transacción falla y el cliente reintenta por su cuenta, conforme a D1.

**Alternativas descartadas**: bloqueos por orden de llegada, que producen ciclos; lectura sin bloqueo, que permite sobreventa; bloqueo de la fila del jugador, que bloquearía la sincronización del catálogo sin aportar la garantía aprobada.

---

## R-07. Semántica de desactivación concurrente

*Origen*: FR-006, D3 opción A.

**Decisión**: la compra comprueba la visibilidad del jugador en el momento de la validación. No bloquea la fila del jugador.

**Comportamiento acordado**:

| Situación | Resultado |
|---|---|
| Jugador desactivado antes de la validación | Compra rechazada. |
| Jugador desactivado después de superar la validación | La compra en curso se completa. |

**Razón**: D3 fue aprobado como no bloqueante. Bloquear el jugador impediría sincronizaciones del catálogo sin garantizar un orden estricto de confirmación que la SPEC no exige.

---

## R-08. Emisión inicial de tokens

*Origen*: FR-016, FR-017, AC17.

**Decisión**:

| Población | Mecanismo |
|---|---|
| Jugadores nuevos | La creación del jugador en la sincronización del catálogo emite 100 tokens al superusuario en la misma transacción. Si la emisión falla, se revierte la creación. |
| Jugadores existentes | Inicialización única por la migración `V9`. |

La emisión se registra en `player_token_emissions`, cuya clave primaria es el jugador. Actualizar, reactivar o volver a sincronizar un jugador existente no vuelve a emitir.

**Razón**: emitir dentro de la transacción de creación cumple AC17 y evita que exista un jugador sin inventario. La tabla de emisiones aporta una garantía que sobrevive a la tenencia mutable y no depende de ella.

**Restricciones de base de datos**: `PRIMARY KEY (player_id)` impide emitir dos veces los tokens iniciales de un mismo jugador. `CHECK (initial_quantity = 100)` fija la cantidad exacta de la emisión, que el enunciado y FR-016 determinan y que no es un parámetro.

**Dependencia**: la emisión requiere un superusuario existente. Si no hay superusuario, la emisión no puede asignar propietario y debe fallar explícitamente en lugar de emitir tokens sin dueño.

**Alternativas descartadas**: emisión diferida en la primera compra, que dejaría jugadores sin inventario y mezcla responsabilidades; `ON CONFLICT DO UPDATE SET quantity = 100` sobre la tenencia, que repone inventario vendido; columna de emisión en `players`, que mezcla dominio de catálogo con dominio de mercado.

---

## R-09. Créditos iniciales

*Origen*: FR-015, AC10, AC18, SC-013, D2.

**Decisión**:

| Caso | Mecanismo |
|---|---|
| Usuario normal nuevo | `AuthenticationServiceImpl.register` crea la cuenta de créditos en la misma transacción del registro. |
| Superusuario | `SuperuserProvisioningServiceImpl` crea su cuenta con saldo 0 en su propia transacción. |
| Usuarios preexistentes | La migración `V9` inserta las cuentas de forma idempotente. |

Todos los caminos usan inserción idempotente que nunca sobrescribe una cuenta existente. La compra **no** crea cuentas de créditos.

**Razón**: §1.3 permite a esos Services depender de `UserCreditRepository`. Escribir la cuenta donde nace el usuario comparte la transacción sin eventos, sin servicios auxiliares y sin dependencias prohibidas. El superusuario se aprovisiona después de las migraciones, por lo que su cuenta no puede depender únicamente de la migración.

**Alternativas descartadas**: evento de dominio con listener `BEFORE_COMMIT`, que añade indirección sin necesidad y no evita por sí solo la frontera entre Services; inicialización diferida en la primera compra, prohibida por D2; trigger de base de datos, que traslada lógica de negocio al motor.

---

## R-10. Auditoría de compras

*Origen*: FR-014, TP §5.2.

**Decisión**: cada compra confirmada inserta una fila en `market_transactions` dentro de la misma transacción, con comprador, vendedor, jugador, cantidad, precio unitario, importe total, fecha y hora, y los saldos y tenencias anteriores y posteriores de ambas partes.

**Razón**: el registro conserva el estado real de la operación, que es lo que exige el enunciado §5.2. Se escriben valores observados durante la compra, nunca reconstruidos a posteriori.

**Inmutabilidad proporcionada**: las operaciones normales solo insertan. No hay modificaciones, eliminaciones, cascadas ni retención sobre auditoría histórica. No se incorporan triggers, firmas, hashes ni mecanismos WORM.

**Autoría**: el registro identifica al comprador autenticado y nunca toma la identidad del cuerpo de la solicitud.

**Alternativas descartadas**: servicio de auditoría separado, innecesario dentro de la transacción; JSONB versionado, que añade versionado y transformación sin consumidor que lo exija; triggers de protección, desproporcionados para este alcance.

---

## R-11. Manejo de errores

*Origen*: FR-019, AC02–AC07, AC12–AC16.

**Decisión**: se reutiliza `GlobalExceptionHandler`. Se agregan únicamente los handlers y excepciones propias que falten.

| Situación | HTTP | `code` |
|---|---|---|
| Rol de superusuario | 403 | `SUPERUSER_CANNOT_BUY` |
| Jugador no visible | 404 | `PLAYER_NOT_VISIBLE` |
| Cantidad inválida | 400 | `INVALID_QUANTITY` |
| Stock insuficiente | 409 | `INSUFFICIENT_STOCK` |
| Créditos insuficientes | 409 | `INSUFFICIENT_CREDITS` |
| Cotización no disponible | 409 | `QUOTE_UNAVAILABLE` |
| Precio no positivo | 409 | `INVALID_QUOTE_PRICE` |
| Fallo interno | 500 | `INTERNAL_ERROR` |

**Jugador inexistente frente a no visible**: el catálogo actual ofrece una consulta de visibilidad que no distingue ambos casos. Se requiere una consulta que separe inexistencia y no visibilidad para responder los dos códigos del contrato, sin alterar el comportamiento existente del catálogo.

**Cuerpo ilegible o campo ausente**: se responde 400 con el contrato global de errores, sin ejecutar lógica de negocio.

**Mensajes de rechazo**: no incluyen saldo, stock ni importes requeridos (D5).

---

## R-12. Estrategia de pruebas

*Origen*: AC01–AC18, SC-001 a SC-016.

**Decisión**: verificación por responsabilidad, con límites reales y sin una prueba por clase.

| Nivel | Sustituciones |
|---|---|
| Model | Ninguna; pruebas puras de dominio. |
| Controller | Service simulado; se verifica contrato HTTP y ausencia de efectos. |
| Service | Persistencia real con PostgreSQL de Testcontainers. |
| Repository | Persistencia real para bloqueos, unicidad y restricciones. |
| Concurrencia | Conexiones y transacciones independientes. |

**Concurrencia**: las compras simultáneas no se ejecutan dentro de una transacción de prueba que nunca confirma. Cada operación usa su propia conexión. Los escenarios de reversión comprueban el estado después de que la transacción fallida concluye.

**Escenarios obligatorios**: compra exitosa; créditos insuficientes; stock insuficiente; cantidad inválida; jugador no visible; primera tenencia creada de forma concurrente; compras simultáneas del último token; ausencia de sobreventa y doble gasto; reversión completa; auditoría consistente; emisión única; inicialización de créditos y tokens; desactivación concurrente.

**Límites**: no se agregan pruebas obligatorias para D3 estricto, corrupción manual, venta, carga masiva ni protección ante administradores.

**Riesgo aceptado**: las pruebas de integración requieren Docker. Si no está disponible, quedan pendientes del usuario y no se declaran cubiertas.

---

## R-13. Migración única

*Origen*: decisiones de consolidación; `backend/src/main/resources/db/migration/`.

**Decisión**: una única migración `V9__create_market_domain.sql`.

**Contenido**:

1. Crear `user_credits`, `player_tokens`, `player_token_emissions` y `market_transactions` con claves, índices y restricciones.
2. Inicializar créditos de usuarios existentes: 100 créditos para usuarios normales, 0 para el superusuario.
3. Registrar la emisión inicial de 100 tokens por cada jugador existente y asignar la tenencia al superusuario.

**Razón**: las migraciones vigentes son V1–V8 y ninguna de SPEC-010 está aplicada, por lo que una única migración es suficiente y más simple de revisar. No se editan migraciones aplicadas.

**Precondición operativa**: la emisión de jugadores existentes requiere un superusuario existente. Si no hay superusuario en el momento de aplicar la migración, esa inicialización no puede asignar propietario y debe señalarse en lugar de emitir sin dueño.

**Orden dentro de la migración**: crear tablas, inicializar créditos, inicializar emisiones. Las inserciones usan semántica idempotente para no duplicar.

---

## Resumen de decisiones

| ID | Tema | Decisión |
|---|---|---|
| R-01 | Arquitectura | Una fachada `BuyOrderService` + `Impl`; sin servicios financieros ni orquestador |
| R-02 | Identidad y vendedor | Repositorio de usuarios; sin dependencias entre Services |
| R-03 | Moneda | Centésimas enteras (`long`/`BIGINT`), aritmética exacta |
| R-04 | Dominio | Operaciones de dominio en Model |
| R-05 | Cotización | Puerto Integration permanente que desacopla la compra; simulador temporal; correspondencia de jugador verificada en la fachada |
| R-06 | Concurrencia | Bloqueos en orden determinista, sin reintentos |
| R-07 | Desactivación | Validación de visibilidad sin bloqueo del jugador |
| R-08 | Emisión | Integrada en la creación del jugador; Flyway para existentes |
| R-09 | Créditos | Creados en la transacción de registro y aprovisionamiento; Flyway para preexistentes |
| R-10 | Auditoría | Registro append-only en la misma transacción |
| R-11 | Errores | Manejo global reutilizado con handlers mínimos |
| R-12 | Pruebas | Verificación por responsabilidad con PostgreSQL real |
| R-13 | Migraciones | Una única migración V9 |

---

## Dependencias y límites

| Dependencia | Garantía afectada | Estado | Consecuencia |
|---|---|---|---|
| REQ-S2-03: cotización vigente | FR-007 | No implementado | El simulador cubre desarrollo y pruebas; no se define su esquema aquí. |
| Superusuario aprovisionado | FR-009, FR-013 | Existente, configurable | Sin superusuario no hay vendedor; la compra falla de forma explícita. |
| Compra/desactivación simultáneas | D3 | No serializadas | Limitación aceptada y no bloqueante. |
| Corrupción manual de datos | FR-014, FR-016 | Fuera de alcance | No se exige reparación automática. |

Ninguna de estas dependencias amplía el alcance funcional de SPEC-010.
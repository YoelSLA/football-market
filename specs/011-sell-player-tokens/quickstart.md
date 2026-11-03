# Guía de validación: Vender tokens de un jugador

Escenarios ejecutables que demuestran el funcionamiento de la feature de extremo a extremo. Esta
guía **no** contiene código de implementación, migraciones ni suites de prueba: describe cómo
obtener, ejecutar y observar el resultado.

**Quién ejecuta**: el usuario. El agente no ejecuta pruebas, builds de prueba ni comandos de base de
datos de escritura (Constitution §6 y `AGENTS.md`).

---

## Prerrequisitos

| Requisito | Comprobación |
|---|---|
| JDK 21 | `java -version` |
| Base de datos de desarrollo accesible | Contenedor PostgreSQL 18 en marcha, según `compose.yml` |
| Fuente de cotización disponible | Sin REQ-S2-03, el simulador cubre el hueco: `MARKET_QUOTE_SIMULATOR_ENABLED=true` |
| Migraciones al día | La aplicación arranca y aplica las migraciones pendientes |

### Advertencia previa sobre la base de desarrollo

La corrección histórica V9 → V10 renombró `V9__create_market_domain.sql` a
`V10__create_market_domain.sql` sin cambiar sus bytes. En una base donde el dominio de mercado ya
figura aplicado con versión **9**, la versión **10** queda pendiente y no puede aplicarse sin
reconciliación. `docs/development/flyway-migrations.md` lo prohíbe resolver automáticamente.

Antes de ejecutar los escenarios, comprobar en qué versión consta aplicada esa migración:

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE description ILIKE '%market domain%'
ORDER BY installed_rank;
```

Si aparece con versión `9`, **detener la ejecución** y acordar la reconciliación con el responsable
de la base antes de continuar. Esta guía no prescribe cómo hacerlo.

---

## Preparación del entorno

```bash
cd backend
MARKET_QUOTE_SIMULATOR_ENABLED=true ./gradlew bootRun
```

En otra terminal, obtener un token de un usuario normal y otro del superusuario mediante el
registro o el inicio de sesión existentes (`POST /api/auth/register`, `POST /api/auth/login`).

Variables útiles para los escenarios:

| Variable | Valor |
|---|---|
| `TOKEN_BUYER` | JWT del usuario que compra |
| `TOKEN_SELLER` | JWT del usuario que publica |
| `PLAYER_ID` | Identificador de un jugador visible con tokens disponibles |

---

## Escenario 1 — Publicar una venta

**Objetivo**: verificar que publicar reserva tokens, congela el precio y no mueve nada.

```bash
curl -s -X POST http://localhost:8080/api/orders/sell \
  -H "Authorization: Bearer $TOKEN_SELLER" \
  -H "Content-Type: application/json" \
  -d "{\"playerId\": $PLAYER_ID, \"quantity\": 5}"
```

**Resultado esperado**: 201 con `listingId`, `unitPrice`, `totalAmount`, `status` `ACTIVE`,
`createdAt` y `expiresAt` exactamente 24 horas después.

**Verificaciones complementarias**:

1. El `expiresAt` menos el `createdAt` es 24 horas.
2. `totalAmount` es `quantity * unitPrice`.
3. Consultar el mercado: la publicación aparece para otros usuarios, no para el vendedor.
4. `GET /api/listings/mine` del vendedor la muestra con `status` `ACTIVE`.
5. Ninguna tabla de movimientos registró una venta: la publicación no es una transacción.

---

## Escenario 2 — El precio queda congelado

**Objetivo**: comprobar que un cambio de cotización posterior no altera publicaciones existentes.

1. Publicar y anotar `unitPrice`.
2. Cambiar el valor de `market.quote.simulator.price` y reiniciar la aplicación.
3. Consultar la publicación con `GET /api/listings/{id}`.

**Resultado esperado**: `unitPrice` y `totalAmount` idénticos a los anotados. La cotización vigente
afecta únicamente a publicaciones nuevas.

---

## Escenario 3 — Mercado público y listados propios

**Objetivo**: verificar las reglas de visibilidad de los tres listados.

| Consulta | Resultado esperado |
|---|---|
| `GET /api/listings` con `TOKEN_SELLER` | No incluye la publicación propia |
| `GET /api/listings` con `TOKEN_BUYER` | Incluye la publicación del vendedor |
| `GET /api/listings/mine` sin filtro | Incluye todas las propias, en cualquier estado |
| `GET /api/listings/mine?status=ACTIVE` | Solo las efectivamente activas y vigentes |
| `GET /api/listings/mine?status=active` | Solicitud inválida: solo se aceptan las formas exactas |
| `GET /api/listings/mine?status=VENDIDA` | Solicitud inválida |
| `GET /api/listings/{id}` con `TOKEN_BUYER` | 200 con el detalle |
| `GET /api/listings/{id}` de otro tercero | 200 mientras siga activa y vigente |

---

## Escenario 4 — Compra secundaria completa

**Objetivo**: verificar transferencia, crédito al vendedor y marca de venta.

1. Anotar el saldo del comprador y del vendedor, y las tenencias de ambos para el jugador.
2. `POST /api/orders/buy` con `{"listingId": <id>}` y `TOKEN_BUYER`.
3. Repetir las consultas de saldos y tenencias.

**Resultado esperado**:

| Comprobación | Valor esperado |
|---|---|
| Tenencia del comprador | Aumenta exactamente la cantidad publicada |
| Tenencia del vendedor | Disminuye exactamente esa cantidad |
| Saldo del comprador | Disminuye exactamente el importe total |
| Saldo del vendedor | Aumenta exactamente el importe total |
| Suma de créditos de ambas partes | Sin cambios |
| Suma de tenencias del jugador | Sin cambios: sigue 100 |
| `GET /api/listings/{id}` | `status` `SOLD` y `closedAt` informado |
| `GET /api/listings/mine` del vendedor | La publicación sigue en el historial, con `SOLD` |
| `transactionId` devuelto | Corresponde a un registro con `listing_id` informada |

Ninguna operación es parcial: todo ocurre en una sola transacción.

---

## Escenario 5 — La publicación solo se liquida una vez

**Objetivo**: verificar que repetir la compra no duplica nada.

Repetir el paso 2 del escenario 4 con el mismo `listingId`.

**Resultado esperado**: 409 `LISTING_NOT_AVAILABLE`. No hay segunda transferencia, ni segundo
registro financiero, ni saldo negativo. La publicación sigue `SOLD`.

---

## Escenario 6 — Comprar la publicación propia

`POST /api/orders/buy` con `{"listingId": <id>}` y el token del **vendedor**.

**Resultado esperado**: 403 `CANNOT_BUY_OWN_LISTING`, sin movimientos y con la publicación intacta.

---

## Escenario 7 — Saldos insuficientes

**Objetivo**: verificar que un rechazo no altera nada.

1. Publicar con un vendedor cuyos tokens estén disponibles.
2. Iniciar sesión con un comprador con saldo menor que el importe total e intentar comprar.

**Resultado esperado**: 409 `INSUFFICIENT_CREDITS`. El saldo no se revela, no queda negativo, la
publicación sigue `ACTIVE` y vigente, y no se registra ninguna transacción.

---

### Subescenario 7b — El vendedor no tiene cuenta de créditos válida

**Objetivo**: verificar CL-14, es decir que la compra se rechaza cuando el vendedor no puede recibir
el pago, sin que la respuesta revele el motivo.

**Precondición**: una publicación `ACTIVE` y vigente cuyo vendedor no tenga fila en `user_credits`, o
con una cuenta en estado que impida el crédito.

**Preparación**: esta precondición no se puede provocar desde la API. Es una **fixture o preparación de
prueba futura**: un caso de prueba en la categoría Repository que cree la publicación directamente
sobre una base aislada de Testcontainers, omita deliberadamente la cuenta de créditos del vendedor y
compruebe la respuesta. **No ejecutar SQL sobre datos reales ni alterar la base de desarrollo** para
producir este estado.

**Resultado esperado** al comprar esa publicación:

| Comprobación | Valor esperado |
|---|---|
| Respuesta | 409 `LISTING_NOT_AVAILABLE` |
| Tokens transferidos | Ninguno |
| Créditos movidos | Ninguno |
| Transacción registrada | Ninguna |
| Estado de la publicación | Sigue `ACTIVE` y vigente; no se marca `SOLD` |
| Mensaje | No menciona al vendedor ni revela que la ausencia de su cuenta es el motivo |

La respuesta debe ser indistinguible de la que se recibe al comprar una publicación `SOLD`,
`CANCELLED` o vencida: las cuatro situaciones comparten el mismo código.

---

## Escenario 8 — Cancelación

1. `GET /api/listings/{id}` y anotar cantidad, `status` y `closedAt`.
2. `POST /api/listings/{id}/cancel` con el token del propietario.
3. Repetir la cancelación.
4. Intentar la cancelación con el token de un tercero.

**Resultado esperado**:

| Paso | Resultado |
|---|---|
| 2 | 200, `status` `CANCELLED`, `closedAt` informado |
| 3 | 409 `LISTING_NOT_AVAILABLE`, sin cambios |
| 4 | Rechazo por falta de autorización, sin cambios |
| Tras 2 | `GET /api/listings` ya no la muestra; `GET /api/listings/mine` sí, con `CANCELLED` |

Verificar además que los tokens vuelven a estar disponibles: publicar de nuevo la cantidad liberada
debe aceptarse.

---

## Escenario 9 — Vencimiento a las 24 horas

**Objetivo**: verificar la expiración efectiva sin proceso programado.

1. Publicar y anotar `createdAt` y `expiresAt`.
2. Consultar la publicación inmediatamente: `status` `ACTIVE`, presente en el mercado.
3. Una vez alcanzado el `expiresAt`, volver a consultarla **sin reiniciar ni ejecutar nada**.

**Resultado esperado**:

| Comprobación | Valor esperado |
|---|---|
| `GET /api/listings/{id}` del propietario | `status` `EXPIRED`, sin cambios en la fila persistida |
| `GET /api/listings/{id}` de un tercero | 404 `LISTING_NOT_FOUND` |
| `GET /api/listings` | Ya no la incluye |
| `GET /api/listings/mine?status=EXPIRED` | La incluye, porque el filtro es por estado efectivo |
| Compra | 409 `LISTING_NOT_AVAILABLE` |
| Cancelación | 409 `LISTING_NOT_AVAILABLE` |
| Reserva | Ya no computa: los tokens pueden volver a publicarse |

Que el estado efectivo cambie sin ninguna tarea programada es la comprobación central de este
escenario.

---

### Subescenario 9b — La publicación vence durante una compra en curso

**Objetivo**: verificar que el punto decisivo de la vigencia es la única validación con publicación
y tenencias ya bloqueadas, después de cualquier espera por ellas, y que no existe una segunda
validación de vencimiento al final.

**Preparación**: se requiere control del instante. La forma verificable es una prueba en la categoría
Repository sobre una base aislada de Testcontainers que fije el reloj o use una publicación cuyo
`expiresAt` se alcance durante la transacción. **No ejecutar SQL ni alterar datos reales** para
producir este estado.

**Resultado esperado**:

| Momento | Comportamiento requerido |
|---|---|
| Con la publicación y ambas tenencias bloqueadas, y el instante obtenido entonces `now >= expiresAt` | 409 `LISTING_NOT_AVAILABLE`, sin transferencias, sin transacción y sin marcar `SOLD`; revierte también una primera tenencia preparada para el comprador |
| Con la validación de vigencia ya superada y el reloj alcanzando `expiresAt` durante la liquidación | La compra puede confirmarse de forma atómica, con `SOLD`, transferencias y registro financiero |

Una implementación que revalidara el vencimiento al final rechazaría la segunda fila, lo que
contradice la regla: la vigencia se decide una sola vez, con la publicación y las tenencias
bloqueadas. T031 tiene cobertura escrita en `TokenListingExpirationPersistenceTest`; todavía no
se acredita su resultado: compilación y ejecución pendientes del usuario.

### Subescenario 9c — Compra, vencimiento y nueva reserva (regresión C1)

**Responsabilidad y estado**: cobertura escrita de categoría Repository (T029), complementada por
T031 para las dos ramas temporales. No ejecutar desde el agente ni contra bases de desarrollo;
no hay resultados observados de estos escenarios. No añade criterios ni endpoints: verifica
SC-002/SC-004 y las garantías ya aprobadas de reservas y atomicidad.

**Preparación común**: PostgreSQL real y desechable de Testcontainers con migraciones reales,
Service y Repository productivos, vendedor con tenencia 20, publicación antigua de 5, comprador
financiado y reloj de test controlado. Usar transacciones independientes y señales con timeouts
técnicos acotados; no `Thread.sleep`, esperas al vencimiento real, mocks de Repository ni hooks
productivos. Una transacción observadora puede consultar `pg_blocking_pids`/`pg_locks` para
acreditar qué sesión espera a cuál por la tenencia; los PID se obtienen desde las transacciones
participantes. Un timeout falla el escenario, no demuestra por sí mismo que hubo bloqueo.

**Caso A — La compra espera y vence antes de obtener las tenencias**:

1. La transacción de publicación obtiene la tenencia del vendedor por el repositorio real y la
   retiene antes de publicar. Iniciar la compra en otra transacción, todavía antes del vencimiento.
2. Observar que la compra espera esa tenencia, después de obtener cuentas y publicación. Solo
   entonces avanzar el reloj hasta `expiresAt`; no se presume una validación temporal previa válida.
3. La primera transacción publica 20 tokens mediante el Service real dentro de su transacción y
   confirma: la oferta antigua ya no reserva por vencimiento.
4. La compra adquiere las tenencias, obtiene el nuevo instante y rechaza con
   `409 LISTING_NOT_AVAILABLE`; finaliza su rollback antes de observar el resultado durable.
5. Afirmar tenencia del vendedor 20, reserva nueva 20, antigua almacenada `ACTIVE` y efectivamente
   `EXPIRED`, sin `SOLD`, sin nueva tenencia del comprador si se preparó exclusivamente para esta
   compra fallida, sin cambios de créditos ni auditoría secundaria para la oferta antigua.

Este caso debe detectar el orden defectuoso anterior: validar antes de esperar permitiría que
la compra descontase 5 tras la nueva reserva y dejase 15 de tenencia frente a 20 reservados.

**Caso B — La compra valida antes de vencer y retiene la tenencia**:

1. Ejecutar la compra dentro de una transacción envolvente de test real y esperar su retorno
   exitoso antes de liberar el commit de esa transacción. La oferta aún no venció al validar;
   cuentas, publicación y tenencias permanecen bloqueadas hasta confirmar.
2. Avanzar el reloj hasta `expiresAt` e iniciar en otra transacción una publicación de 20 tokens.
   Acreditar su espera por la tenencia retenida por la compra, aunque la oferta ya haya vencido.
3. Permitir el commit de la compra. La publicación obtiene la tenencia ya reducida a 15 y rechaza
   con `409 INSUFFICIENT_STOCK`, sin insertar una reserva nueva ni alterar las existentes.
4. Afirmar antigua `SOLD`, comprador +5 tokens, vendedor −5 tokens, créditos transferidos por el
   importe congelado, suma de recursos conservada y una única auditoría con la referencia antigua.
   No hay reserva de 20 sobre tenencia 15. Como control positivo posterior, publicar 15 debe reservar
   exactamente 15; las reservas efectivas nunca superan la tenencia.

El reloj adelantado no puede deshacer esa compra ni causar otra validación temporal al commit.
T028 mantiene la cobertura de rollback integral, y T031 debe detectar explícitamente una segunda
lectura temporal decisoria. Estos casos de C1 prueban la exclusión concreta, no ausencia universal
de interbloqueos. La reanudación fue autorizada y ambas regresiones están escritas en el grupo
`ExpirationReservationRace` de `TokenListingConcurrencyTest`. No se compilaron ni ejecutaron;
la comprobación de los resultados sigue pendiente de intervención humana.

---

## Escenario 10 — Jugador no visible

**Objetivo**: verificar las cuatro consecuencias de perder la visibilidad.

1. Publicar con un jugador visible y confirmar 201.
2. Desactivar el jugador mediante la sincronización del catálogo, sin tocar la publicación.
3. Consultar los tres listados y la publicación.

**Resultado esperado**:

| Comprobación | Resultado |
|---|---|
| `POST /api/orders/sell` de otro usuario para ese jugador | 404 `PLAYER_NOT_VISIBLE` |
| `GET /api/listings` | Ya no incluye ninguna publicación de ese jugador |
| `GET /api/listings/{id}` de un tercero | 404 `LISTING_NOT_FOUND` |
| Compra por `listingId` de un tercero | 404 `LISTING_NOT_FOUND` |
| `GET /api/listings/{id}` del propietario | 200, con su estado e importe |
| `GET /api/listings/mine` del propietario | La sigue incluyendo |

La publicación **no** se cancela ni se elimina: conserva su historial.

---

## Escenario 11 — Compatibilidad con la compra directa

**Objetivo**: comprobar que SPEC-010 sigue intacto.

```bash
curl -s -X POST http://localhost:8080/api/orders/buy \
  -H "Authorization: Bearer $TOKEN_BUYER" \
  -H "Content-Type: application/json" \
  -d "{\"playerId\": $PLAYER_ID, \"quantity\": 2}"
```

**Resultado esperado**: compra contra el superusuario, con `listingId` nulo o ausente en la
respuesta y `listing_id` nula en el registro financiero. Los errores de la compra directa se
mantienen idénticos, incluidos `SUPERUSER_CANNOT_BUY`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE` e
`INVALID_QUOTE_PRICE`.

---

## Escenario 12 — Autorización del superusuario

Con el token del superusuario:

| Operación | Resultado esperado |
|---|---|
| `POST /api/orders/sell` | 403 `SUPERUSER_CANNOT_SELL` |
| `POST /api/orders/buy` con `listingId` | 403 `SUPERUSER_CANNOT_BUY` |
| `POST /api/orders/buy` directa | 403 `SUPERUSER_CANNOT_BUY` |

Ninguna de las tres opera aunque el superusuario posea tokens o saldo.

---

## Escenario 13 — Reserva concurrente

**Objetivo**: verificar que las reservas nunca exceden la tenencia.

Con un vendedor que posea exactamente 10 tokens libres del jugador, disparar en paralelo dos
peticiones de publicación de 6 tokens cada una.

**Resultado esperado**: una responde 201 y la otra 409 `INSUFFICIENT_STOCK`. La suma de reservas
resultante es 6 y nunca 12.

---

## Escenario 14 — Dos compradores simultáneos

Con dos compradores con saldo suficiente y una publicación activa:

**Resultado esperado**: una compra confirma con 200 y la otra recibe 409 `LISTING_NOT_AVAILABLE`.
Solo existe un registro financiero y el saldo de nadie queda negativo.

---

## Registro histórico de preparación — anterior a la aceptación de T058

Revisión documental realizada el 2026-10-18: rutas, frontera temporal y escenarios 9b/9c coherentes
con el diseño C1 y las regresiones escritas. No se ejecutó ningún escenario ni se modificó una base.

| Escenario | Resultado observado |
| --- | --- |
| 1 — Publicar | Pendiente del usuario |
| 2 — Precio congelado | Pendiente del usuario |
| 3 — Consultas | Pendiente del usuario |
| 4 — Compra secundaria | Pendiente del usuario |
| 5 — Liquidación única | Pendiente del usuario |
| 6 — Compra propia | Pendiente del usuario |
| 7 y 7b — Saldos y cuenta vendedora | Pendiente del usuario |
| 8 — Cancelación | Pendiente del usuario |
| 9, 9b y 9c A/B — Vencimiento y C1 | Pendiente del usuario |
| 10 — Visibilidad | Pendiente del usuario |
| 11 — SPEC-010 | Pendiente del usuario |
| 12 — Superusuario | Pendiente del usuario |
| 13 — Reserva concurrente | Pendiente del usuario |
| 14 — Compradores concurrentes | Pendiente del usuario |

La tabla anterior conserva el estado previo. No exige ejecutar SQL ni alterar desarrollo: los casos
de persistencia/concurrencia se verifican mediante las pruebas de Repository sobre Testcontainers.

## T058 — equivalencias aceptadas por el usuario (2026-10-18)

El usuario aprobó expresamente TESTING de US01–US04 y aceptó las equivalencias automatizadas,
incluidas sus limitaciones y sin exigir los refuerzos propuestos. T058 exige revisar la guía y
anotar resultados observados por escenario; no exige ejecución HTTP literal ni añadir tests.
Se completa como revisión y registro de la evidencia humana aceptada, **no como ejecución manual**.
Suite aportada por el usuario y corroborada en XML/HTML: 938 exitosos, 0 fallidos, 0 omitidos.
Rutas, conteos por clase y fingerprint: [testing-report.md](testing-report.md).

Los métodos siguientes identifican pruebas existentes; los XML bajo
`backend/build/test-results/test/TEST-footballmarket.*.xml` usan los nombres de clase/grupo
indicados en el informe. No se ejecutaron nuevamente ni se añadieron refuerzos.

| Escenario | Resultado funcional observado en tests existentes | Límite aceptado |
| --- | --- | --- |
| 1 | ListingControllerTest/Publication, TokenListingPersistenceTest.reservaSinTransferir y Service/Queries exitosos: contrato, reserva y consulta | Composición por capas; no HTTP integrado |
| 2 | TokenListingServiceTest.mantienePrecioCongelado y BuyOrderServiceTest.liquidaPublicacion exitosos | Parcial: sin reinicio ni publicación nueva al precio cambiado verificados conjuntamente |
| 3 | Controller/Queries y Persistence.filtraEstadoEfectivo exitosos | Composición; filtros inválidos representativos |
| 4 | MarketDomainPersistenceTest.conservaRecursos, compra secundaria y C1 B exitosos | Parcial: no journey HTTP enlazado con consultas posteriores |
| 5 | Persistence.indistinguibilidadDeCierres("SOLD") exitoso | Parcial: segundo comprador distinto; no repetición literal por el mismo usuario |
| 6 | BuyOrderServiceTest.rechazaCompraPropia y traducción Controller exitosos | Parcial: sin snapshot durable específico de este rechazo |
| 7 | Persistence.rechazaSaldoInsuficiente y contrato/no divulgación Controller exitosos | Composición entre persistencia y HTTP simulado |
| 7b | Persistence.rechazaCuentaAusente, rechazos de cierres y traducción Controller exitosos | Ausencia de cuenta; no estado hipotético de cuenta bloqueada |
| 8 | Service/Cancellation, Persistence.liberaReservaSinTransferir y Controller/Cancellation exitosos | Composición; no recorrido HTTP secuencial |
| 9 | Persistence.expiraReserva, consultas/rechazos y Service.rechazaCancelacionVencida exitosos | Parcial: reloj/fixtures, no observación HTTP durante 24 horas |
| 9b | ExpirationPersistence.rechazaFrontera/noRevalidaAlFinal y regresiones C1 exitosos | Reloj controlado, no espera de reloj real |
| 9c A | Concurrency.rechazaCompraVencidaDuranteEspera exitoso: espera real observada y rollback | Carrera concreta; no garantía universal de ausencia de interbloqueos |
| 9c B | Concurrency.conservaTenenciaHastaCommit exitoso: bloqueo hasta commit y rechazo posterior | Carrera concreta; no garantía universal de ausencia de interbloqueos |
| 10 | VisibilityPersistence.conservaHistorialPropio, rechazo de compra y contratos exitosos | Desactivación en fixture, no sincronización externa real |
| 11 | Regresiones directas de Controller, Service, Persistence y Authorization exitosas | Alcance de las assertions existentes; no demostración universal |
| 12 | TokenListingAuthorizationTest y BuyOrderAuthorizationTest exitosos | Parcial: sin snapshots durables específicos de las tres prohibiciones |
| 13 | Concurrency.evitaSobreventa exitoso: una confirmación y un rechazo | Parcial: barrera, no observador independiente de bloqueo; fixture 20/11+11 equivalente a 10/6+6 |
| 14 | Concurrency.liquidaUnaVez exitoso: liquidación/auditoría únicas y recursos coherentes | Parcial: coordinación sin observador independiente de bloqueo |

Se conservan **10 escenarios funcionalmente completos y 8 parciales**; ninguno sin cobertura.
La aceptación humana no transforma las ocho coberturas parciales en comprobaciones completas.
No se certifican prerrequisitos operativos de desarrollo: no se verificó su historial V9/V10,
no se reconciliaron bases ni se aplicaron migraciones. Testcontainers aislado no demuestra el
estado de una base existente. No se realizó el recorrido HTTP manual completo.

## Referencias

| Documento | Contenido |
|---|---|
| [spec.md](spec.md) | Requisitos, escenarios y criterios |
| [plan.md](plan.md) | Diseño, flujos transaccionales y estrategia de pruebas |
| [data-model.md](data-model.md) | Esquema, índices y bloqueos |
| [contracts/listings.md](contracts/listings.md) | Contrato de publicar, consultar y cancelar |
| [contracts/buy-listing.md](contracts/buy-listing.md) | Contrato de la compra secundaria |
| [research.md](research.md) | Decisiones técnicas y alternativas descartadas |

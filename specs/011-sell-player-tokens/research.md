# Research: Vender tokens de un jugador (SPEC-011)

**Feature**: `011-sell-player-tokens` | **Fecha**: 2026-10-17 | **Spec**: [spec.md](spec.md)

Este documento registra las decisiones técnicas tomadas en la fase Plan. Cada una indica la
decisión adoptada, su fundamento y las alternativas evaluadas. Ninguna decisión funcional de la
especificación se modifica aquí: las decisiones funcionales pertenecen al `spec.md` y a sus
aclaraciones.

---

## R-01 — Orden total de adquisición de bloqueos

**Decisión**: existe un único orden total y explícito de bloqueo:

```text
1. user_credits    (ascendente por user_id)
2. token_listings  (ascendente por id)
3. player_tokens   (ascendente por owner_user_id)
```

Cada operación adquiere **un subconjunto ordenado** de ese orden: las categorías que necesite, siempre
en el orden relativo que el orden global establece. No es necesario tomar las tres, y tomar una
categoría intermedia sin las anteriores está permitido.

| Operación | `user_credits` | `token_listings` | `player_tokens` |
|---|---|---|---|
| Publicar (`POST /api/orders/sell`) | — | — | tenencia del vendedor |
| Comprar publicación | comprador y vendedor, asc | fila de la publicación | ambas tenencias, asc |
| Cancelar publicación | — | fila de la publicación | — |
| Compra directa (SPEC-010) | comprador y superusuario, asc | — | ambas tenencias, asc |

**Condición que sostiene la ausencia de ciclos** (enunciada con precisión): *para las categorías del
orden global que una operación sí adquiere, esa operación nunca toma una categoría anterior después de
haber tomado una posterior.* Publicar toma únicamente la última categoría y no toma ninguna anterior;
cancelar toma únicamente la intermedia y ninguna otra. Ninguna operación adquiere `player_tokens` y
después necesita `token_listings` o `user_credits`.

**Fundamento**: el orden relativo reproduce el que SPEC-010 ya aplica en su compra directa
(`user_credits` ascendente antes que `player_tokens` ascendente), de modo que la extensión no altera
ninguna garantía existente. La emisión inicial de SPEC-010 toma solo `player_tokens`, y la
sincronización del catálogo solo toca `players` y tenencias, por lo que no introducen ninguna
dependencia nueva.

**Alcance de la garantía, con honestidad**: la condición anterior impide los ciclos entre las cuatro
operaciones descritas. No constituye una demostración general de ausencia de interbloqueos, porque
no cubre las operaciones futuras que agregue el proyecto ni los bloqueos que acquisitionen sobre otros
recursos —por ejemplo, `players` durante la sincronización del catálogo o `users` al insertar una
primera tenencia. Si una operación futura atraviesa el orden global en sentido inverso, la garantía
deja de aplicar y debe reevaluarse.

**Alternativas evaluadas**:

- **Adquirir `token_listings` antes que `user_credits` en la compra de publicación.** **Descartada.**
  El orden global obliga a tomar las cuentas primero. Además, dada esa secuencia, la publicación queda
  retenida mientras se esperan las cuentas del comprador y del vendedor, lo que prolonga la ventana
  en que una publicación permanece bloqueada sin necesidad funcional: el vendedor se conoce con una
  lectura sin bloqueo, de modo que bloquear la publicación antes de las cuentas no aporta nada que
  la lectura inicial no aporte. Por eso D-03 exige leer la publicación sin bloqueo, bloquear las
  cuentas, bloquear la publicación y **revalidar bajo bloqueo**.
- **Publicar bloqueando `user_credits`.** **Descartada.** Publicar no mueve créditos (FR-001,
  SC-001). Tomar la cuenta del vendedor añadiría serialización global sobre todos los usuarios que
  publican del mismo vendedor sin aportar ninguna garantía, y es el camino que hace posible el
  interbloqueo descrito abajo entre dos usuarios que publican y compran mutuamente.
- **Ordenar por creación de la publicación en lugar de por identificador.** Descartada por
  debilidad: el identificador es la clave primaria estable y ordenable, y ordenar por él en la
  aplicación antes de bloquear evita depender del plan de ejecución de una consulta con `ORDER BY`.

**Consecuencia aceptada**: la suma de reservas se calcula bajo el bloqueo de la tenencia. No se
bloquean las filas de `token_listings` del vendedor. Véase R-02.

**Precisión C1 (2026-10-18)**: el orden de adquisición no cambia, pero la única validación temporal
decisiva de una compra ocurre **después** de bloquear también las tenencias. El instante se obtiene
en ese punto, no antes de esperar. Cuentas, publicación y tenencias permanecen bloqueadas hasta el
commit o rollback. Si la compra supera esa validación, puede terminar después del vencimiento sin
una segunda comprobación temporal. Publicar espera la misma tenencia y, después de obtenerla,
calcula reservas y disponibilidad con su propio instante. No existe una reserva persistida adicional.

---

## R-02 — Cálculo de reservas y su carácter conservador

**Decisión**: la disponibilidad publicable es

```text
disponible = player_tokens.quantity(seller, player)
             − SUM(token_listings.quantity)  // solo publicaciones efectivamente activas y vigentes
```

y la suma se evalúa **bajo el bloqueo pesimista de la tenencia** del vendedor para ese jugador. Las
filas de `token_listings` se leen sin bloquearlas.

**Fundamento**: la tenencia es el único recurso que la reserva consume, de modo que serializar por
tenencia garantiza que la suma de reservas de un vendedor nunca exceda su tenencia (SC-002). Dos
publicaciones simultáneas del mismo vendedor se serializan en esa fila: la segunda vuelve a calcular
la disponibilidad y ya ve la reserva de la primera (AC14).

**Por qué no se bloquean las filas de publicaciones al sumar reservas**: toda nueva publicación y
toda transferencia de esos tokens se serializan mediante la tenencia del vendedor. En particular,
la compra adquiere esa tenencia **antes** de obtener el instante de su única validación temporal
decisiva (C1). Si espera mientras vence la publicación, la rechaza al obtener la tenencia; si ya
superó la validación, conserva la tenencia hasta commit o rollback y una nueva publicación no puede
reutilizar esos tokens entretanto. En el aislamiento por defecto asumido, publicar obtiene la
tenencia antes de leer el estado confirmado y sumar reservas. Comprar y cancelar pueden disminuir
reservas, no aumentarlas; una cancelación concurrente solo puede producir una lectura conservadora,
nunca una aceptación por encima de la tenencia. La afirmación anterior de seguridad no era
suficiente si la compra validaba vigencia antes de bloquear las tenencias: esa secuencia queda
sustituida documentalmente por la corrección C1, pendiente de implementación y prueba.

**Consecuencia aceptada y documentada**: pueden producirse rechazos de publicación
conservadores bajo concurrencia (una publicación cancelada al mismo tiempo sigue contada). Es una
limitación de disponibilidad, no una violación de integridad. Documentada en `plan.md` y sin tarea
obligatoria, conforme a Constitution §2.2.

**Alternativa evaluada**: bloquear también las publicaciones del vendedor, antes de la tenencia,
para que la suma fuese estable. Esa secuencia `token_listings` → `player_tokens` **sí respeta** el
orden global `user_credits` → `token_listings` → `player_tokens`; no se descarta por invertirlo.
Se descarta porque añade bloqueos sobre publicaciones que no son necesarios para preservar la
disponibilidad: el diseño seleccionado hace que publicar bloquee solo la tenencia y lea las reservas
sin bloquear sus filas, mientras comprar bloquea cuentas, publicación y tenencias antes de su única
validación temporal decisiva (C1). Ambas escrituras quedan serializadas por la tenencia, y una
cancelación concurrente solo puede causar una lectura conservadora. La alternativa añade contención
sin aportar una garantía necesaria adicional al mecanismo seleccionado.

---

## R-03 — Reservas derivadas, sin columna de reserva en `player_tokens`

**Decisión**: no se agrega ninguna columna de reserva a `player_tokens`. La reserva es un valor
derivado de las publicaciones.

**Fundamento**: coherencia directa con SPEC-010, que descartó explícitamente `reserved_credits` por el
mismo motivo (`specs/010-buy-tokens/data-model.md`). Un estado derivado no puede quedar desincronizado
de su fuente. Además, Constitution §2.1 prioriza el esquema más simple que satisfaga los requisitos
actuales.

**Alternativa evaluada**: columna `reserved_quantity` en `player_tokens`, actualizada por cada
publicación. Se descarta porque introduce un segundo dato que debe coincidir siempre con la suma de
las publicaciones, sin aportar capacidad funcional: la fecha de vencimiento de una publicación
seguiría necesitando consultarse para saber si la reserva está vigente.

---

## R-04 — `EXPIRED` como estado efectivo, no persistido

**Decisión**: la columna `status` almacena únicamente `ACTIVE`, `SOLD` y `CANCELLED`. `EXPIRED` se
deriva en lectura de que `expires_at <= now`.

**Fundamento**: FR-010 y CL-06 lo exigen: sin proceso programado ni actualización automática del
estado almacenado. El `CHECK` de la columna refleja exactamente los valores que la aplicación puede
persistir, de modo que la base de datos no admite un estado que el sistema no produce.

**Consecuencia sobre el filtro**: el filtro de `GET /api/listings/mine` debe calcular el estado
efectivo (CL-15, FR-027). Una publicación vencida permanece `ACTIVE` en la fila y deja de computar
reservas porque la condición de vigencia se evalúa junto con el estado.

**Alternativas evaluadas**:

- Persistir `EXPIRED` mediante una tarea programada. Descartada: CL-06 y Out of Scope la excluyen.
- Persistir `EXPIRED` de forma perezosa, escribiéndolo al observarlo. Descartada porque escribir
  desde una operación de lectura mezcla lectura con escritura y, con varias instancias, introduce
  actualizaciones condicionadas sin beneficio funcional alguno frente a derivarlo.

---

## R-05 — Número y formato de las migraciones

**Decisión**: las migraciones de esta feature se crean con el generador del proyecto, producing
`V<timestamp UTC de 14 dígitos>__descripcion.sql`, y se crean **dos** archivos:

1. tabla `token_listings` con sus índices;
2. columna nullable `listing_id` en `market_transactions`.

Ambas se generan desde `backend/` con `./gradlew createMigration -PmigrationDescription=…`.

**Fundamento**: `docs/development/flyway-migrations.md` establece que las migraciones nuevas usan
timestamp UTC y que las versiones históricas V1–V10 no se convierten. Los timestamps ordenan
numéricamente por encima de V10 y reducen colisiones entre ramas paralelas, que es exactamente el
modo de fallo que produjo el conflicto V9/V10 ya corregido.

**Dos migraciones y no una**: la adición de una columna a una tabla existente (`market_transactions`,
creada por SPEC-010) es una dependencia y un riesgo distintos de la creación de una tabla nueva.
Separarlas permite que la primera se aplique y verifique sin depender de la segunda, y hace que un
fallo de validación de la segunda no deje `token_listings` a medio construir en el esquema.

**Advertencia de estado de base**: la corrección histórica V9 → V10 renombró
`V9__create_market_domain.sql` a `V10__create_market_domain.sql` sin modificar sus bytes. En la base
de desarrollo local esa migración ya figura aplicada con versión 9, por lo que V10 quedó pendiente
y Flyway no podrá aplicarla sin reconciliación. `docs/development/flyway-migrations.md` lo exige
explícitamente. **No se modifica ninguna base en esta fase**; queda como advertencia operativa para
el responsable de la base.

---

## R-06 — Servicios y separación de casos de uso

**Decisión**: dos fachadas de aplicación, con interfaz e implementación separadas:

| Servicio | Operaciones |
|---|---|
| `TokenListingService` | publicar, cancelar, listar el mercado, listar las propias, consultar el detalle |
| `BuyOrderService` | compra directa (existente) y compra de publicación (nueva) |

**Fundamento**: `docs/backend/architecture.md` §1.3 permite a un Service depender solo de Model,
Repository e Integration, y prohíbe las dependencias entre Services. Publicar y comprar son casos de
uso con fronteras transaccionales distintas y no comparten paso alguno, por lo que un servicio único
con un parámetro discriminador sería menos claro que dos fachadas. Comprar comparte con SPEC-010
todo el camino de validación y transferencia, de modo que **debe** residir en `BuyOrderService` y no
en un servicio nuevo que lo duplicara.

No se crea Orchestrator: `docs/backend/architecture.md` §1.1 lo reserva para coordinación entre
Services, y aquí no hay coordinación entre casos de uso independientes.

**Alternativa evaluada**: separar también la compra de publicación en `ListingPurchaseService`. Se
descarta porque obligaría a duplicar la validación de rol, el cálculo de importe, los bloqueos de
cuentas y tenencias y la auditoría, ya implementados y probados en `BuyOrderServiceImpl`.

---

## R-07 — Reutilización de `QuoteSource` para el precio congelado

**Decisión**: el precio unitario de la publicación se obtiene del puerto Integration `QuoteSource`,
igual que la compra directa, y se persiste congelado junto con `quote_calculated_at` y
`quote_strategy_version` opcionales.

**Fundamento**: CL-02 lo exige. Reutilizar el puerto existente evita una segunda fuente de precio y
deja SPEC-011 preparada para que REQ-S2-03 sustituya la fuente sin cambios. Persistir la procedencia
del precio mantiene la trazabilidad que SPEC-010 ya documento en su auditoría.

**Consecuencia**: la publicación congela el precio vigente en el momento de publicar; la compra no
vuelve a consultar la cotización (FR-013). Una modificación posterior de la cotización no altera
publicaciones existentes (AC09, SC-003).

---

## R-08 — Referencia de publicación en el registro financiero

**Decisión**: `market_transactions` incorpora una columna nullable `listing_id` con clave foránea a
`token_listings(id)`, sin cascada.

**Fundamento**: FR-022 lo exige y da trazabilidad directa de cada venta a su publicación. La
nulabilidad preserva la compra directa de SPEC-010 sin cambios (AC29, SC-010).

**Ausencia de cascada**: coherente con la decisión documentada en SPEC-010, donde
`market_transactions` no tiene cascadas hacia `users` ni `players`. La auditoría histórica sobrevive
a sus participantes, y la referencia a la publicación debe comportarse igual.

**Interacción con las claves foráneas de `token_listings` (decisión D-02)**: las referencias
`seller_user_id → users.id` y `player_id → players.id` se declaran también con `NO ACTION`. Esa
combinación es deliberada y coherente: al no existir cascadas en ninguna dirección, la eliminación de
un usuario o de un jugador con historial de mercado queda **rechazada por PostgreSQL** en lugar de
eliminar publicaciones ni auditorías. Es el comportamiento que la trazabilidad histórica exige.

En la práctica esa eliminación ya estaba bloqueada antes de esta feature por
`market_transactions.buyer_id` y `seller_id`, que SPEC-010 declaró sin cascada. `token_listings` no
cambia esa situación: la refuerza. La consecuencia a documentar es que la eliminación física de un
usuario con actividad de mercado no es posible sin una decisión explícita y una migración que la
habilite, decisión que no se toma aquí.

**Riesgo aceptado**: SPEC-011 amplía el modelo de datos que SPEC-010 cerró, al añadir una columna a su
tabla. Constitution §4 obliga a actualizar en el mismo cambio los elementos afectados: `spec.md`,
contrato, modelo de datos y pruebas de SPEC-010. Esa actualización corresponde a la modificación ya
diferida (PENDIENTE-2) y se ejecuta en la misma tarea, no como cambio separado.

---

## R-09 — Diferenciación entre publicación inexistente y no visible al solicitante

**Decisión**: tanto `GET /api/listings/{id}` como la compra por identificador responden
`404 LISTING_NOT_FOUND` cuando la publicación no existe **o** no es visible para quien pregunta,
incluido el caso de jugador no visible (CL de la sesión 2026-10-17, FR-019, FR-028).

**Fundamento**: una sola regla evita que la respuesta sirva de oráculo de existencia. Se aparta de la
distinción explícita entre `PLAYER_NOT_FOUND` y `PLAYER_NOT_VISIBLE` que sí existe en el catálogo,
y esa diferencia es deliberada: el jugador se consulta directamente y su no visibilidad es
información del catálogo, mientras que la publicación es un recurso del usuario cuya existencia no se
promete revelar.

---

## R-10 — Autorización sin cambios en `SecurityConfig`

**Decisión**: no se modifica `SecurityConfig`. Todas las rutas nuevas quedan cubiertas por
`anyRequest().authenticated()`, y la autorización por rol y por pertenencia se resuelve en la
fachada leyendo el usuario persistido.

**Fundamento**: SPEC-010 ya estableció que el rol se lee de la persistencia y que el JWT no contiene
claim de rol (`security/BuyOrderAuthorizationTest`). Reutilizar ese mecanismo evita un segundo
punto de decisión de autorización.

---

## R-11 — Duración configurable

**Decisión**: la vigencia de 24 horas se resuelve con **una única propiedad de configuración**,
`market.listing.expiration-duration`, con valor por defecto `PT24H`, declarada en
`backend/src/main/resources/application.yml` dentro del bloque `market:` y sobreescribible por la
variable de entorno `MARKET_LISTING_EXPIRATION_DURATION`.

```yaml
market:
  listing:
    expiration-duration: ${MARKET_LISTING_EXPIRATION_DURATION:PT24H}
```

**Fundamento**: la decisión funcional vigente exige que la duración sea 24 horas y que el valor sea
ajustable en el backend sin tocar código. El nombre sigue la convención ya vigente del bloque
`market.quote.simulator.*` del mismo archivo, con el mismo patrón de valor por defecto inyectado
desde el entorno. **No hay colisión**: `application.yml` no declara hoy ninguna clave bajo
`market.listing`.

**Enlace previsto en la implementación**: el valor es un `java.time.Duration` y se enlaza en la
fachada con `@Value("${market.listing.expiration-duration:PT24H}")`, exactamente igual que
`QuoteSimulationConfiguration` enlaza `market.quote.simulator.price` con `@Value`. Se descarta
`@ConfigurationProperties` porque el proyecto no lo usa en ningún punto y una única propiedad no
justifica una clase de configuración. La ausencia de la propiedad no debe impedir el arranque: el
valor por defecto cubre ese caso.

**Límite**: el valor es de negocio, no de infraestructura. Esta decisión no habilita cambiar la
vigencia de forma individual por publicación; el valor aplica a todas ellas.

---

## R-12 — Idioma del código y de la documentación

**Decisión**: identificadores, paquetes, endpoints y campos JSON en inglés; documentación,
Javadoc, OpenAPI y mensajes de validación en español.

**Fundamento**: Constitution §5.

**Consecuencia sobre los nombres**: el estado persistido usa literales en inglés (`ACTIVE`, `SOLD`,
`CANCELLED`) porque forman parte del contrato de datos; la documentación que los describe va en
español.

---

## Deferred

| Asunto | Motivo |
|---|---|
| Objetivo de rendimiento medible | Constitution §2.2: no hay umbral acordado por el equipo. SPEC-010 registró el suyo como no vinculante y sin tarea; esta feature mantiene la misma postura. La integridad concurrente es obligatoria y no depende de ese objetivo. |
| Índice parcial para publicaciones activas vigentes | Depende del plan de ejecución real y del volumen; se documenta la recomendación en `data-model.md` como observación, no como requisito. |
| Orden de presentación y paginación de los listados | RECOMENDADO en el `spec.md`; mejora de experiencia, no bloqueante. |
| Notificación al vendedor | Fuera de alcance por decisión funcional. |
| Reconciliación de la base local respecto de V10 | Corresponde al responsable de la base, conforme a `docs/development/flyway-migrations.md`. No se ejecuta desde esta feature. |

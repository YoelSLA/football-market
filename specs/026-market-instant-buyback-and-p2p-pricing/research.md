# Investigación técnica — SPEC-026

## Rutas y límites del mercado actual

- `POST /api/orders/sell` está implementado por `OrderController.sell` y `TokenListingService.publish`;
  crea una publicación que reserva tokens y **no** transfiere dinero, tokens ni crea
  `MarketTransaction`. Por eso la venta oficial necesita una ruta distinta.
- `BuyOrderServiceImpl` es una fachada transaccional para compras y `OfficialBuybackServiceImpl` es
  la fachada transaccional de la venta oficial. `OfficialBuybackExecutor` resuelve cotización y
  actor; `MarketSettlementExecutor` centraliza el bloqueo ordenado, transferencia simétrica y
  persistencia de auditoría.
- `UserCreditRepository.lockAccountsInAscendingOrder` obtiene bloqueos pesimistas por usuario
  ascendente; `PlayerTokenRepository.lockHoldingsInAscendingOwnerOrder` hace lo propio por tenencia.
  La compra oficial actual bloquea primero cuentas y después inventario.
- `TokenListingRepository.reservedQuantity` suma publicaciones `ACTIVE` cuyo vencimiento es mayor que
  el instante de operación. `TokenListing.publish` conserva cantidad, precio, importe y datos de la
  cotización.
- `MarketTransaction` persiste comprador, vendedor, jugador, cantidad, precio unitario, total,
  snapshots de saldos/tenencias y procedencia temporal/estrategia de cotización. `unit_price` no puede
  representar a la vez la cotización de referencia y el precio neto del buyback.

## Decisiones

### D-01 — Ruta propia para buyback

**Decisión**: `POST /api/orders/sell-official`, autenticado, separado de `POST /api/orders/sell`.

**Fundamento**: la ruta actual tiene semántica contractual establecida de publicación/reserva; una
venta inmediata crea una operación financiera, por lo que reutilizar esa ruta cambiaría el
significado de clientes existentes. El sufijo diferencia de forma explícita ambos resultados.

**Alternativa descartada**: cambiar el significado de `/sell` o seleccionar operación mediante un
campo discriminador en body: rompería el contrato existente y ampliaría la complejidad de
deserialización.

### D-02 — Settlement existente y orden inverso de partes

**Decisión**: el usuario autenticado es vendedor y el superusuario comprador. Se conserva la
adquisición de bloqueos `user_credits` ascendente y luego `player_tokens` ascendente. Se revalidan
bajo bloqueo el saldo del superusuario, la tenencia del usuario menos reservas activas y la
disponibilidad de la cotización antes de persistir cambios.

**Fundamento**: coincide con el orden de bloqueos del mercado oficial y evita introducir otra
política de concurrencia. Se modela al superusuario como comprador y al usuario como vendedor, por
lo que el método actual `MarketSettlementExecutor.transfer` ya realiza el sentido requerido:
debita al comprador, acredita al vendedor y transfiere los tokens del vendedor al comprador. La
publicación P2P también bloquea la tenencia del vendedor antes de calcular reservas; por ello la
venta oficial obtiene el mismo bloqueo antes de volver a comprobar disponibilidad.

**Alternativa descartada**: transferencias secuenciales fuera de una transacción común, que podrían
dejar dinero y tokens desincronizados.

### D-03 — Auditoría diferenciada sin reescribir historial

**Decisión**: conservar el precio pagado en `unit_price` y agregar `quote_price` y
`discount_percentage` nullable a `market_transactions`; solo el buyback exige guardar ambos valores.
El resto de los registros históricos mantiene `NULL`, sin backfill ficticio.

**Fundamento**: FR-006 exige cotización y descuento además del precio neto, mientras que el schema
vigente solo representa el precio ejecutado. La nulabilidad permite migración aditiva y conserva
los registros históricos sin inventar valores.

**Alternativa descartada**: derivar la cotización desde el precio neto/porcentaje, porque el
redondeo half-up hace que no siempre pueda reconstruirse exactamente.

### D-04 — Precio neto y comisión

**Decisión**: configurar comisión porcentual entera de 10 a 99 inclusive, default 10. Calcular el neto por
token y aplicar half-up al entero de centésimas antes de calcular el importe; toda multiplicación y
acreditación usa aritmética exacta. Rechazar configuración fuera del rango, precio neto redondeado a
cero y liquidaciones que desborden o no puedan financiarse.

**Fundamento**: implementa FR-003, FR-017, FR-018 y las clarificaciones incorporadas en `spec.md`.

**Alternativas descartadas**: redondear el importe total, usar binary floating point o permitir
porcentajes fraccionarios no solicitados.

### D-05 — Precio P2P comparado en unidad exacta

**Decisión**: `unitPrice` es opcional; si está presente debe ser entero JSON positivo en centésimas.
Validar `100 * precio >= 80 * cotización` y `100 * precio <= 120 * cotización` usando aritmética
que no desborde (por ejemplo, comparación exacta con `BigInteger`, si los operandos `long` pueden
exceder el rango de producto). Si se omite, persistir la cotización vigente actual. Se conserva el
precio e importe congelados de la publicación.

**Fundamento**: respeta banda inclusiva, la unidad monetaria y el comportamiento existente de
compras P2P.

**Alternativa descartada**: redondear límites porcentuales a centésimas antes de validar, lo que
podría aceptar o rechazar precios distintos de la banda matemática especificada.

## Riesgos y tratamiento

- **Saldo del superusuario**: la cuenta se inicializa actualmente en cero (`UserCredit`); no se
  añade fondeo. Si el saldo no alcanza, la venta se rechaza sin cambios, conforme a la asunción
  vigente de `spec.md`.
- **Compatibilidad de contratos**: los clientes actuales usan `/api/orders/sell` como publicación.
  La nueva ruta evita colisión y el request existente solo agrega un campo opcional.
- **Historial financiero**: nuevas columnas nullable no rellenan información desconocida de
  operaciones pasadas. La venta incluye la cotización aplicada y el porcentaje explícitamente.
- **Consumidores contables**: el historial de operaciones clasifica BUY cuando el usuario es
  `buyer_id` y SELL cuando es `seller_id`; el reconstruidor de posiciones aplica compra/venta con
  esa misma regla. El buyback conserva los roles naturales comprador-superusuario/vendedor-usuario,
  por lo que no requiere un tipo de operación adicional; sus pruebas deben confirmar el resultado.
- **Traducción de errores**: el handler de mercado existente expone códigos para cantidad,
  superusuario, stock y créditos, pero varios mensajes se redactaron para compra. El contrato del
  nuevo endpoint debe verificar los códigos y una respuesta segura apropiada sin revelar saldo ni
  existencias.
- **Límites aritméticos**: porcentaje, multiplicación por cantidad y suma de créditos deben
  protegerse contra overflow; fallo significa rollback total.

## Estado de clarificaciones

El `spec.md` registra las decisiones funcionales requeridas para PLAN: redondeo de centésimas
(half-up), comisión de 10%-99% y rechazo cuando el neto resulte cero. No quedan marcadores
`NEEDS CLARIFICATION`. La ruta y las columnas son decisiones técnicas de diseño asentadas aquí, no
requisitos funcionales nuevos.

## Alineación de comisión

El diseño vigente requiere una comisión entera de 10–99, con valor predeterminado 10, y el rechazo de
una venta cuyo precio neto redondeado sea cero. `tasks.md` marca T028–T033, destinadas a alinear esos
límites y comportamientos, como completadas. Esto no acredita que los tests se hayan ejecutado ni que
los gates humanos estén aprobados.

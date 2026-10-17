# Modelo de datos: Comprar tokens de un jugador

Persistencia PostgreSQL del dominio de mercado. Precisión, restricciones e índices según la consolidación del 2026-10-16.

## Principios de persistencia

- Los saldos actuales se leen directamente de las tablas de estado. No se reconstruyen sumando movimientos históricos.
- No hay reservas de créditos: no existen órdenes pendientes en esta SPEC.
- No hay event sourcing, outbox ni tabla de cotizaciones. La cotización pertenece a REQ-S2-03.
- Una única migración crea las cuatro tablas e inicializa los datos preexistentes.

---

## Precisión de importes

Rige la aclaración CL-01 aprobada. Se descarta el punto flotante y se descarta la representación decimal, que obligaría a modificar una decisión funcional vigente.

### Aritmética y reversión

| Elemento | Representación |
|---|---|
| Unidad monetaria | Centésimas de crédito; 1 crédito = 100 unidades |
| Java | `long` |
| PostgreSQL | `BIGINT` |
| Cantidades de tokens | `INT` |
| Aritmética | Exactiva; un desbordamiento revierte la compra completa |

El importe total se calcula una sola vez con aritmética exacta y las restricciones de la base de datos fijan la coherencia del resultado registrado. Ninguna operación financiera usa punto flotante.

**Coherencia con PostgreSQL**: `total_amount = quantity * unit_price` es una expresión entera exacta, sin residuo ni redondeo, porque los tres operandos son enteros. La restricción no impone trabajo extra al motor ni introduce imprecisión.

**Reversión ante desbordamiento**: si el cálculo o la actualización de importes excede el rango soportado, la operación se revierte por completo y la respuesta es un error interno genérico, conforme a FR-022 y D4. No se transfieren tokens, no se modifican saldos y no queda registro de compra confirmada.

---

## user_credits — Saldo de créditos por usuario

```sql
CREATE TABLE user_credits (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    available_credits BIGINT NOT NULL DEFAULT 0 CHECK (available_credits >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Reglas**:

| Regla | Mecanismo |
|---|---|
| Una cuenta por usuario | `PRIMARY KEY (user_id)` |
| Saldo nunca negativo, incluido el del superusuario | `CHECK (available_credits >= 0)` |
| No se restaura ni se sobrescribe en reejecuciones | Inserción idempotente que ignora conflictos |
| No existe reserva | No se modela `reserved_credits` |

La presencia de la fila es un hecho persistente, no una inferencia. Una cuenta ausente durante una compra produce rechazo por créditos insuficientes y no se crea en esa operación.

---

## player_tokens — Tenencia por usuario y jugador

```sql
CREATE TABLE player_tokens (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_player_tokens_player_owner UNIQUE (player_id, owner_user_id)
);
CREATE INDEX idx_player_tokens_player ON player_tokens(player_id);
```

**Reglas**:

| Regla | Mecanismo |
|---|---|
| Una tenencia por par usuario-jugador | `UNIQUE (player_id, owner_user_id)` |
| Tenencia nunca negativa | `CHECK (quantity >= 0)` |
| Primera tenencia creada bajo demanda | Inserción con cantidad 0 e ignorado de conflicto |
| Total conservado por jugador | Emisión única más transferencias simétricas |

El índice por `player_id` cubre la agregación de existencias del jugador; el índice por propietario no es necesario en esta SPEC porque no existe consulta de portfolio ni historial.

Las filas con cantidad 0 son válidas: existen para simplificar los bloqueos y para distinguir "nunca compró" de "compró y vendió", sin exigir la segunda semántica hoy.

---

## player_token_emissions — Emisión inicial por jugador

```sql
CREATE TABLE player_token_emissions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    initial_quantity INT NOT NULL CHECK (initial_quantity = 100),
    emitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Reglas**:

| Regla | Mecanismo |
|---|---|
| Emisión única por jugador | `PRIMARY KEY (player_id)` |
| Propietario inicial identificable | `owner_user_id` |
| Emisión de exactamente 100 tokens | `CHECK (initial_quantity = 100)` |
| Sin reposición del inventario vendido | La reejecución no modifica `player_tokens` |

La cantidad de la emisión no es un parámetro: el enunciado fija 100 tokens por jugador y FR-016 lo exige literalmente. La restricción `= 100` hace que PostgreSQL rechace cualquier emisión con otra cantidad, de modo que la invariante no depende solo del código de aplicación.

**Compatibilidad con las vías de inicialización**:

| Vía | Respeto de la restricción |
|---|---|
| Sincronización del catálogo | Inserta `100` de forma explícita al crear el jugador; una emisión con otra cantidad sería rechazada |
| Migración de jugadores existentes | Inserta `100` para cada jugador existente, asignado al superusuario |
| Reejecución | La clave primaria y el conflicto ignorado impiden una segunda emisión |

La restricción no habilita ningún mecanismo nuevo: no permite reponer inventario, emitir por otras vías ni crear reservas. Solo acota el valor de una emisión que ya es única.

El registro sobrevive a la tenencia mutable: no depende de que la fila de inventario exista. Es la garantía de unicidad de la emisión.

---

## market_transactions — Auditoría de compras

```sql
CREATE TABLE market_transactions (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount = quantity * unit_price),
    buyer_credits_before BIGINT NOT NULL CHECK (buyer_credits_before >= 0),
    buyer_credits_after BIGINT NOT NULL CHECK (buyer_credits_after >= 0),
    seller_credits_before BIGINT NOT NULL CHECK (seller_credits_before >= 0),
    seller_credits_after BIGINT NOT NULL CHECK (seller_credits_after >= 0),
    buyer_tokens_before INT NOT NULL CHECK (buyer_tokens_before >= 0),
    buyer_tokens_after INT NOT NULL CHECK (buyer_tokens_after >= 0),
    seller_tokens_before INT NOT NULL CHECK (seller_tokens_before >= 0),
    seller_tokens_after INT NOT NULL CHECK (seller_tokens_after >= 0),
    quote_calculated_at TIMESTAMPTZ NOT NULL,
    quote_strategy_version VARCHAR(64),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_market_transactions_buyer ON market_transactions(buyer_id, executed_at DESC, id DESC);
CREATE INDEX idx_market_transactions_seller ON market_transactions(seller_id, executed_at DESC, id DESC);
```

**Reglas**:

| Regla | Mecanismo |
|---|---|
| Autor y participantes | `buyer_id` y `seller_id`; `buyer_id` es el comprador autenticado |
| Detalle de la operación | Jugador, cantidad, precio unitario e importe |
| Marca de tiempo | `executed_at` en UTC, igual al valor de la respuesta |
| Estado anterior y posterior | Ocho columnas de saldos y tenencias |
| Coherencia del importe | `CHECK (total_amount = quantity * unit_price)` |
| Conservación | Comportamiento de la aplicación: solo inserciones |

Los valores de estado son los observados durante la compra bajo los bloqueos, no reconstruidos después. La coherencia entre estados se sostiene por la operación atómica: la operación resta exactamente lo que suma.

La columna `operation_type` se omite: esta SPEC solo registra compras y la ausencia de esa columna no se compensa con un enum de un único valor.

`quote_strategy_version` es nullable porque ninguna exigencia de SPEC-010 la requiere y el origen de la cotización aún no está implementado. Esta nulabilidad coincide con la del contrato `QuoteSource`, cuyo `strategyVersion` admite `null`: cuando la fuente real no proporcione una versión, la auditoría conserva `null` en lugar de inventar un valor. El simulador, en cambio, completa el campo con una etiqueta que lo identifica como simulado. Cuando REQ-S2-03 entregue la versión de estrategia, se informará en la misma fila.

---

## Alcance de la inmutabilidad de la auditoría

Conviene distinguir dos garantías que suelen confundirse. Esta tabla **no** está protegida contra `UPDATE` o `DELETE` a nivel de motor: no hay triggers, permisos especiales, firmas ni restricciones que lo impidan.

| Garantía | Quién la proporciona | Alcance real |
|---|---|---|
| **Comportamiento de la aplicación** | La compra solo inserta | Se mantiene mientras el sistema se comporte así |
| **Restricción de PostgreSQL** | No existe | Cualquier sentencia SQL con permiso podría modificar o borrar filas |

Comportamiento de la aplicación que sostiene la garantía:

| Comportamiento | Detalle |
|---|---|
| La auditoría se inserta dentro de la compra | Comparte la transacción con las transferencias de créditos y tokens |
| Una compra revertida no deja auditoría | La reversión deshace también la inserción del registro |
| La entidad no expone modificaciones | `MarketTransaction` no tiene operaciones de cambio de estado |
| No hay eliminación de auditoría | Ninguna operación del sistema borra transacciones |
| No hay superficie HTTP de escritura | El único endpoint de mercado es la compra; no existe actualización ni eliminación de transacciones |
| Sin retención automática | Ninguna tarea elimina registros por antigüedad |

**Consecuencia práctica**: la conservación exigida por FR-014 y SC-010 es «durante las operaciones normales del sistema», que es exactamente el alcance de la decisión funcional aprobada. El modelo no promete resistencia ante un operador de base de datos que ejecute un `UPDATE` o un `DELETE` manual.

Endurecer esa garantía exigiría triggers, separación de permisos o evidencia criptográfica, todos descartados por simplicidad y porque el enunciado no los exige. Si en el futuro se exigiera resistencia ante operadores privilegiados, sería una decisión funcional nueva que debería aprobarse de forma explícita.

---

## Inicialización de datos

### Usuarios y superusuario

| Población | Mecanismo |
|---|---|
| Usuario normal nuevo | Creación de la cuenta dentro de la transacción de registro |
| Superusuario | Creación de la cuenta con saldo 0 dentro de la transacción de aprovisionamiento |
| Usuarios preexistentes | Inserción idempotente en la migración |

Todas las inserciones ignoran conflictos y nunca sobrescriben un saldo existente.

### Jugadores y emisión

| Población | Mecanismo |
|---|---|
| Jugadores nuevos | Emisión dentro de la transacción que crea al jugador en la sincronización del catálogo |
| Jugadores existentes | Emisión idempotente en la migración |

Si no existe superusuario, la emisión de jugadores existentes no puede asignar propietario. Esa condición debe señalarse de forma explícita en lugar de emitir tokens sin dueño.

---

## Políticas de eliminación

Las cascadas del modelo se justifican por el comportamiento real del backend, verificado en el código.

### Comportamiento existente

| Entidad | Política real | Evidencia |
|---|---|---|
| Jugador | **Nunca se borra físicamente.** La sincronización lo desactiva | `PlayerResolution.inactivateMissing` invoca `player.deactivate()`; el modelo no tiene eliminación |
| Usuario | Borrado físico disponible pero **sin consumidor** | `UserService.deteleAllUsers()` existe, no lo invoca ningún Controller ni Service |

### Evaluación de las cascadas

| Relación | Cascada | Veredicto |
|---|---|---|
| `user_credits.user_id → users.id` | `ON DELETE CASCADE` | **Compatible.** La cuenta de créditos no tiene sentido sin su usuario. Si un usuario se eliminara, su saldo desaparece con él. |
| `player_tokens.player_id → players.id` | `ON DELETE CASCADE` | **Compatible.** Los jugadores no se borran, por lo que la cascada es una salvaguarda ante un borrado futuro y no un comportamiento activo. |
| `player_tokens.owner_user_id → users.id` | `ON DELETE CASCADE` | **Compatible.** Una tenencia no sobrevive a la eliminación de su propietario. |
| `player_token_emissions.player_id → players.id` | `ON DELETE CASCADE` | **Compatible**, por la misma razón que las tenencias. |
| `market_transactions` → `users`, `players` | **Sin cascada** | **Correcto y deliberado.** La auditoría histórica no se borra al eliminar un usuario o jugador. |

### Consecuencias que conviene conocer

| Situación | Efecto |
|---|---|
| Se eliminara físicamente un usuario con historial | Se borrarían su saldo y sus tenencias, pero **sus auditorías se conservarían** |
| Se eliminara físicamente un jugador | Se borrarían su emisión y sus tenencias, pero **sus auditorías se conservarían** |
| Estado normal del sistema | Ninguna de las dos situaciones ocurre: los jugadores se desactivan y los usuarios no se eliminan |

La combinación es coherente: el saldo y la tenencia son **estado actual** y pertenecen a su entidad, mientras que la auditoría es **histórico** y sobrevive a sus participantes.

### Advertencia de coherencia de auditoría

Si alguna vez se eliminaran físicamente usuarios o jugadores, `market_transactions` conservaría filas cuyo comprador, vendedor o jugador ya no existirían. Hoy esa situación no se produce. Si llegara a producido, requeriría una decisión funcional explícita sobre cómo tratar la auditoría de participantes eliminados; no se resuelve aquí porque no forma parte de esta SPEC.

No se agregan cascadas adicionales ni se modifican las existentes.

---

## Restricciones de integridad

| Tabla | Restricción | Garantía funcional |
|---|---|---|
| `user_credits` | `available_credits >= 0` | Ningún saldo negativo (SC-009) |
| `player_tokens` | `quantity >= 0` | Ninguna tenencia negativa (SC-009) |
| `player_tokens` | `UNIQUE (player_id, owner_user_id)` | Sin tenencias duplicadas ni sobreventa oculta |
| `player_token_emissions` | `PRIMARY KEY (player_id)` | Emisión única (AC17, FR-016) |
| `player_token_emissions` | `initial_quantity = 100` | Cantidad exacta de la emisión (FR-016) |
| `market_transactions` | `quantity > 0`, `unit_price > 0` | Registro de operaciones válidas |
| `market_transactions` | `total_amount = quantity * unit_price` | Coherencia del importe |

Las claves foráneas hacia `users` y `players` heredan la integridad referencial ya existente.

**Efecto de las claves foráneas sobre la concurrencia**: insertar una tenencia toma `FOR KEY SHARE` sobre la fila de `users` y de `players` referenciadas. Una actualización de `players.active` toma `FOR NO KEY UPDATE`, compatible con ese bloqueo, de modo que la compra no bloquea la sincronización del catálogo ni queda bloqueada por ella. Solo un borrado físico del jugador tomaría un bloqueo incompatible, y el catálogo no borra jugadores físicamente: los desactiva.

---

## Consultas de bloqueo requeridas

| # | Consulta | Orden | Bloqueo real | Propósito |
|---|---|---|---|---|
| 1 | Cuentas de comprador y vendedor | `user_id` ascendente | `FOR UPDATE` | Serializar saldos y evitar doble gasto |
| 2 | Inserción de la primera tenencia del comprador con cantidad 0 | — | `FOR KEY SHARE` sobre `users` y `players` | Serializar la creación concurrente de la tenencia |
| 3 | Tenencias de comprador y vendedor de un jugador | Propietario ascendente | `FOR UPDATE` | Serializar el stock y evitar sobreventa |

**El orden debe ser explícito y determinista.** La forma inequívoca es ordenar los identificadores en la aplicación y bloquearlos por clave primaria en ese orden; una cláusula de pertenencia sin orden no garantiza la secuencia de adquisición de bloqueos.

**La inserción no reemplaza el bloqueo de lectura**: `ON CONFLICT DO NOTHING` serializa la creación frente a otra inserción concurrente, pero cuando la fila ya existe la operación se descarta y **no** bloquea esa fila. El bloqueo efectivo de la tenencia del comprador lo aporta la consulta del paso 3.

**Lectura bajo bloqueo**: el saldo y el stock se obtienen exclusivamente de estas consultas. No deben leerse antes de bloquear, porque un valor leído previamente puede quedar cacheado en el contexto de persistencia y no reflejar el estado protegido.

---

## Entidades JPA

Cada tabla se representa con una entidad persistente en `footballmarket.models`, siguiendo el criterio vigente de que los Model pueden usar directamente mecanismos de persistencia. Las entidades de saldo, tenencia y auditoría no tienen setters: sus cambios de estado ocurren mediante operaciones de dominio que preservan las invariantes.

| Entidad | Responsabilidad |
|---|---|
| `UserCredit` | Saldo disponible y operaciones de débito y crédito |
| `PlayerToken` | Tenencia y operaciones de transferencia |
| `PlayerTokenEmission` | Registro de emisión inicial, sin mutaciones |
| `MarketTransaction` | Registro histórico de una compra, sin mutaciones |
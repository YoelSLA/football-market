# Modelo de datos: Vender tokens de un jugador

Persistencia PostgreSQL del mercado secundario. Complementa, sin modificar, el modelo de
[SPEC-010](../010-buy-tokens/data-model.md), del que hereda la representación monetaria y el dominio
de tenencias.

## Principios de persistencia

- Los saldos y las tenencias se leen directamente de las tablas de estado; no se reconstruyen sumando
  movimientos históricos.
- La reserva de tokens es un valor **derivado** de las publicaciones activas y vigentes, no una
  columna (research.md R-02, R-03).
- `EXPIRED` es un estado **efectivo** calculado en lectura; la columna nunca lo almacena (R-04).
- No hay event sourcing, outbox ni tabla de cotizaciones. La cotización pertenece a REQ-S2-03.
- No se editan migraciones aplicadas.

---

## Precisión de importes

Rige CL-01, también aplicable a SPEC-010.

| Elemento | Representación |
|---|---|
| Unidad monetaria | Centésimas de crédito; 1 crédito = 100 unidades |
| Java | `long` |
| PostgreSQL | `BIGINT` |
| Cantidades de tokens | `INT` |
| Aritmética | Exactiva; un desbordamiento revierte la compra completa |

`total_amount = quantity * unit_price` es una expresión entera exacta en PostgreSQL, porque los tres
operandos son enteros. La publicación congela el precio en el momento de crearse y la compra usa ese
mismo valor congelado (FR-013), de modo que el importe registrado en `market_transactions` coincide con
el importe que la publicación fijó.

---

## token_listings — Publicación

```sql
CREATE TABLE token_listings (
    id BIGSERIAL PRIMARY KEY,
    seller_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE NO ACTION,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE NO ACTION,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount = quantity * unit_price),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLD', 'CANCELLED')),
    quote_calculated_at TIMESTAMPTZ NOT NULL,
    quote_strategy_version VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ
);
```

### Reglas

| Regla | Mecanismo |
|---|---|
| Importe coherente con precio y cantidad | `CHECK (total_amount = quantity * unit_price)` |
| Precio y cantidad positivos | `CHECK` de las dos columnas |
| Vendedor persistido | `seller_user_id` con clave foránea a `users` |
| Publicación solo en `ACTIVE`, `SOLD` o `CANCELLED` | `CHECK (status IN (...))` |
| El vencimiento es posterior a la creación | `CHECK (expires_at > created_at)` |
| Una publicación cerrada tiene momento de cierre | Coherencia mantenida por el modelo: `status <> 'ACTIVE'` implica `closed_at` informado |
| Publicación no duplicada | **No hay unicidad**: se permiten varias publicaciones del mismo vendedor y jugador (CL y AC12) |

`EXPIRED` **no** figura en el `CHECK` porque la aplicación nunca lo persiste. Incluirlo
contradiría la base de datos con el comportamiento real y permitiría estados que el sistema no
produce.

### Índices

| Índice | Propósito |
|---|---|
| `idx_token_listings_active ON token_listings (expires_at) WHERE status = 'ACTIVE'` | Resuelve el filtro del mercado público y el cálculo de reservas sobre publicaciones vigentes |
| `idx_token_listings_seller ON token_listings (seller_user_id, created_at DESC)` | Resuelve `GET /api/listings/mine`, que siempre ordena por vendedor |
| Índice único de la clave primaria | Suficiente para el bloqueo de una fila concreta |

El índice parcial es una **recomendación**, no un requisito. El plan de ejecución real y el volumen de
publicaciones determinan su utilidad; si resultara innecesario, un índice completo sobre
`(status, expires_at)` sería equivalente. No se fija ninguno como criterio de aceptación.

### Índices ausentes y por qué

| Índice candidato | Motivo de no incluirlo |
|---|---|
| `player_id` | La publicación no se busca por jugador en esta feature: la compra parte del identificador de publicación y la reserva parte del vendedor |
| Un índice único `(seller_user_id, player_id)` | Contradiría la decisión funcional de permitir varias publicaciones simultáneas |

---

## Cambios en market_transactions

```sql
ALTER TABLE market_transactions
    ADD COLUMN listing_id BIGINT REFERENCES token_listings(id);
```

| Aspecto | Decisión |
|---|---|
| Nulabilidad | `NULL` en compras directas al superusuario; informado en compras de publicación |
| Cascada | **Ninguna**, coherente con la política sin cascadas de SPEC-010 |
| Índices existentes | No se modifican: siguen sirviendo las consultas por comprador y por vendedor |
| Índices nuevos | Ninguno. La consulta del historial de transacciones no está en el alcance de esta feature |

La columna no tiene `CHECK` propio: la nulabilidad ya expresa ambos modos y el tipo de la clave
foránea garantiza que el identificador exista cuando es informado.

**Riesgo reconocido**: esta es la única ampliación del modelo que SPEC-010 cerró. La
misma tarea que añade la columna actualiza `contracts/buy-tokens.md`, las pruebas de SPEC-010 y su
`data-model.md`, conforme a Constitution §4.

---

## Código de generación de migraciones

Las migraciones se crean desde `backend/` con:

```bash
./gradlew createMigration -PmigrationDescription=create_token_listings
./gradlew createMigration -PmigrationDescription=add_listing_to_market_transactions
```

Producen `V<timestamp UTC de 14 dígitos>__<descripción>.sql`. El timestamp numérico es muy superior
a `V10`, de modo que Flyway las ordena después de las versiones históricas sin renombrar ni
convertir ninguna.

Se crean **dos** archivos y no uno porque la creación de una tabla nueva y la modificación de una
tabla existente tienen dependencias y riesgos distintos (research.md R-05).

---

## Cascadas

| Relación | Cascada declarada | Veredicto |
|---|---|---|
| `token_listings.seller_user_id → users.id` | `ON DELETE NO ACTION` | **Deliberada (D-02).** Una publicación es parte del historial del vendedor y no se elimina en cascada. La eliminación física de un usuario con publicaciones queda rechazada por PostgreSQL, preservando la trazabilidad |
| `token_listings.player_id → players.id` | `ON DELETE NO ACTION` | **Deliberada (D-02).** Coherente con `NO ACTION`. El catálogo no borra jugadores físicamente: los desactiva, de modo que esta relación no se activa en la operación normal |
| `market_transactions.listing_id → token_listings(id)` | **Sin cascada**, es decir `NO ACTION` | **Deliberada.** La auditoría histórica sobrevive a sus participantes |

### Consecuencias de la ausencia de cascadas

| Situación | Efecto |
|---|---|
| Se elimina un usuario con publicaciones | **Rechazado** por `seller_user_id`. No se pierde trazabilidad ni se degrada la auditoría |
| Se elimina un usuario con compras o ventas registradas | **Ya estaba rechazado** antes de esta feature por `market_transactions.buyer_id` y `seller_id`, que SPEC-010 declaró sin cascada. `token_listings` no altera esa situación |
| Se elimina un jugador con publicaciones | **Rechazado** por `player_id`. El catálogo desactiva jugadores y no los borra, así que no se activa en la operación normal |
| Se cierra una publicación (`SOLD`, `CANCELLED` o vencida) | La fila se conserva siempre. No existe ninguna operación que borre publicaciones |
| Estado normal del sistema | Ninguna eliminación física ocurre. El catálogo desactiva y las publicaciones se cierran por cambio de estado |

**Lectura conjunta**: la ausencia de cascadas en las tres relaciones convierte cualquier eliminación
física en una operación rechazada por la base de datos, en lugar de una eliminación silenciosa de
historial. Es la postura que sostiene la trazabilidad exigida por `tp/enunciado.md` §5.2 y por FR-021.

**Advertencia operativa**: eliminar un usuario con actividad de mercado requiere una migración que
redefina estas referencias. Es una decisión nueva, ajena a esta feature, y no se plantea aquí.

---

## Entidades JPA

Siguiendo el criterio vigente de `docs/backend/architecture.md` §1.5, las entidades viven en
`footballmarket.models` y preservan sus invariantes mediante operaciones de dominio, sin setters.

| Entidad | Responsabilidad |
|---|---|
| `TokenListing` | Publicación, transiciones de estado y cálculo del estado efectivo |
| `MarketTransaction` | Registro histórico append-only, ahora con referencia opcional a la publicación |

### Operaciones de dominio de `TokenListing`

| Operación | Precondición | Efecto |
|---|---|---|
| `publish(seller, player, quantity, unitPrice, quote, expiresAt)` | Cantidad entre 1 y 100, precio positivo | Publicación `ACTIVE` con importe calculado |
| `markSold(validatedAt)` | `ACTIVE` y vigente en el único instante decisivo obtenido después de bloquear publicación y tenencias | Estado `SOLD` y `closed_at` informado con el instante ya validado, sin consultar otra vez el reloj |
| `markCancelled(closedAt)` | `ACTIVE` y vigente | Estado `CANCELLED` y `closed_at` informado |
| `effectiveStatus(now)` | — | `EXPIRED` si está `ACTIVE` y venció; en otro caso el estado almacenado |
| `isActiveAndValid(now)` | — | `true` solo si está `ACTIVE` y no venció |

La validez se calcula contra un instante recibido, no contra el reloj interno del modelo, para que
el cálculo sea determinista y verificable desde la categoría Model.

### `MarketTransaction`

Se añade el campo `listing` nullable y una sobrecarga de la fábrica `record` que lo recibe. **Se
conserva la firma existente** para no modificar el camino de la compra directa de SPEC-010.

---

## Consultas de bloqueo requeridas

| # | Consulta | Orden | Bloqueo real | Propósito |
|---|---|---|---|---|
| 1 | Cuentas de comprador y vendedor | `user_id` ascendente | `FOR UPDATE` | Serializar saldos y evitar doble gasto |
| 2 | Fila de la publicación | — | `FOR UPDATE` | Serializar compradores y cancelaciones sobre la misma publicación |
| 3 | Tenencias de comprador y vendedor | Propietario ascendente | `FOR UPDATE` | Serializar el inventario del jugador |
| 4 | Tenencia del vendedor (publicar) | — | `FOR UPDATE` | Serializar publicaciones concurrentes del mismo vendedor |

El orden de adquisición global es `user_credits` → `token_listings` → `player_tokens`. Cada operación
adquiere **un subconjunto ordenado** de esas categorías: las que necesite, en ese orden relativo, sin
que sea necesario tomar las tres. La condición que sostiene la ausencia de ciclos es que ninguna
operación tome una categoría anterior después de haber tomado una posterior (research.md R-01).

En la compra secundaria, la consulta 2 se ejecuta **después** de la 1: la publicación se lee primero
sin bloqueo para conocer al vendedor, se bloquean las cuentas y luego se bloquea la fila, revalidando
bajo ese bloqueo que el vendedor no cambió.

**Punto decisivo de C1**: después de la consulta 2 se prepara, si falta, la tenencia del comprador y
se ejecuta la consulta 3. Solo **después de obtener ambas tenencias** se lee el reloj y se valida
`validatedAt < expiresAt`, conservando todos los bloqueos hasta commit o rollback. Una espera por
`player_tokens` no puede ir precedida por una decisión de vigencia que luego siga autorizando la
compra. Si la publicación venció durante esa espera, se rechaza con `LISTING_NOT_AVAILABLE` y se
revierte cualquier preparación, incluida la inserción de una primera tenencia. Si la validación
se supera, no se revalida el vencimiento al marcar `SOLD` ni al confirmar.

No cambia el esquema: las nuevas publicaciones esperan por la misma tenencia y, al obtenerla,
calculan disponibilidad sobre los datos confirmados resultantes y un instante posterior al bloqueo.
Una compra ya validada puede consumir tokens después del vencimiento sin que otra publicación los
reutilice antes de finalizar. La cancelación conserva su bloqueo exclusivo de la publicación,
sin adquirir cuentas ni tenencias.

**El orden debe ser explícito y determinista**: ordenar los identificadores en la aplicación y
bloquearlos por clave primaria en ese orden. Una cláusula de pertenencia sin orden no garantiza la
secuencia de adquisición.

### Inserción de la primera tenencia del comprador

La compra de una publicación puede crear la primera tenencia del comprador para ese jugador. Se
reutiliza la inserción idempotente que SPEC-010 ya define, con su particularidad documentada: cuando
la fila ya existe la operación se descarta y **no** bloquea esa fila, de modo que el bloqueo efectivo
lo aporta la consulta del paso 3.

---

## Suma de reservas

```sql
SELECT COALESCE(SUM(quantity), 0)
FROM token_listings
WHERE seller_user_id = :sellerId
  AND player_id = :playerId
  AND status = 'ACTIVE'
  AND expires_at > :now
```

Se evalúa bajo el bloqueo de la tenencia del vendedor. `EXPIRED` no se persiste, de modo que las
publicaciones vencidas quedan fuera por la condición `expires_at > :now`.

**Frontera de vigencia**: la condición `expires_at > :now` significa que una publicación está vigente
únicamente mientras `now < expiresAt`, y que está vencida en cuanto `now >= expiresAt`. El mismo
criterio se aplica al calcular el estado efectivo y al decidir si una publicación puede comprarse o
cancelarse. En una compra, el instante decisivo se obtiene con publicación **y tenencias** ya
bloqueadas, después de cualquier espera por ellas. En una cancelación se obtiene con la publicación
bloqueada; en una publicación se obtiene después de bloquear la tenencia del vendedor.

Dentro de cada operación se utiliza un único instante para sus decisiones temporales. No se
comparte un instante antiguo entre operaciones concurrentes ni se usa en una compra un instante
obtenido antes de adquirir las tenencias. Una vez superada su validación decisiva, la compra usa
`validatedAt` para la transición y no vuelve a comprobar el reloj, aunque alcance `expiresAt` antes
del commit. La reserva derivada no se prolonga artificialmente: la exclusión frente a una nueva
publicación durante esa compra la aporta el bloqueo de la tenencia, no un nuevo estado ni columna.

---

## Restricciones de integridad

| Tabla | Restricción | Garantía funcional |
|---|---|---|
| `token_listings` | `quantity > 0`, `unit_price > 0` | Solo publicaciones con precio y cantidad válidos |
| `token_listings` | `total_amount = quantity * unit_price` | Coherencia del importe (SC-003) |
| `token_listings` | `status IN ('ACTIVE','SOLD','CANCELLED')` | Estados que el sistema puede persistir |
| `token_listings` | `expires_at > created_at` | Publicaciones siempre vencen después de crearse |
| `market_transactions` | `listing_id` nullable y referenciado | Trazabilidad de la venta a su publicación (SC-010) |

Las claves foráneas hacia `users`, `players` y `token_listings` heredan la integridad referencial ya
existente.

**Efecto sobre la concurrencia**: crear una publicación toma `FOR KEY SHARE` sobre las filas de
`users` y `players` referenciadas. Una actualización de `players.active` toma `FOR NO KEY UPDATE`,
compatible con ese bloqueo. Insertar `listing_id` en `market_transactions` toma `FOR KEY SHARE`
sobre la publicación, compatible con la lectura de una publicación comprada. Ninguna de estas
operaciones introduce un conflicto con la sincronización del catálogo.

---

## Inicialización de datos

Ninguna. La tabla se crea vacía y la columna es nullable: no hay backfill ni datos preexistentes que
requieran inicialización. Los saldos iniciales y la emisión de tokens pertenecen a SPEC-010 y no se
alteran.

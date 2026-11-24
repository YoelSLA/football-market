# Modelo de datos: tendencias y demanda

El diseño reutiliza el ledger de transacciones y las tablas de cotizaciones. Las migraciones PostgreSQL/Flyway son la fuente de verdad del esquema.

## Fuente de actividad

`market_transactions` permanece sin cambios y append-only. La ventana filtra por `executed_at`; se usan `quantity` y `total_amount`, expresados en tokens y centésimas de crédito.

| Categoría derivada | Regla de conteo |
| --- | --- |
| Compra | `buyer.role = USER`; incluye compra directa y liquidación P2P. |
| Venta | `buyer.role = SUPERUSER` y `seller.role = USER`; venta oficial al superusuario. |

Una transacción P2P se considera solo compra. `listing_id` identifica su origen, pero no agrega un evento de venta para tendencias/demanda. Operaciones confirmadas no se reescriben ni etiquetan retroactivamente.

## Cotización por jugador (`player_quotes`)

Conservar unicidad actual por `(player_id, effective_date)`, publicación bajo bloqueo, `previous_price`, `price` y `multiplier` aplicado final. Agregar campos explícitos:

| Campo lógico | Nulable | Significado |
| --- | --- | --- |
| `sport_multiplier` | Sí | Multiplicador deportivo previo a combinar; nulo cuando no aplica score o en registros anteriores. |
| `market_demand_factor` | Sí | Factor de mercado usado; nulo en iniciales y filas previas a la feature, cero cuando el cálculo nuevo evaluó demanda neutra. |
| `market_multiplier` | Sí | `1 + market_demand_factor`; nulo en iniciales y registros históricos no reconstruibles. |
| `market_net_demand_tokens` | Sí | Demanda neta del jugador en la ventana; nula para filas iniciales e históricas no reconstruibles. |

El `multiplier` persistido existente representa el multiplicador final acotado. En recálculo habitual, el valor previo al cap es `sport_multiplier * market_multiplier`; en recálculo solo de mercado es `market_multiplier`. Aplicar reglas existentes de redondeo, cap y piso. No fingir reconstrucción de filas históricas.

El origen distingue `INITIAL`, `RECOMPUTED`, `CARRIED_FORWARD` y `MARKET_DEMAND`. Una ejecución habitual sin score conserva la semántica actual; una ejecución exclusiva de mercado se identifica como `MARKET_DEMAND`, incluso con factor cero.

## Ejecución (`quote_executions`)

Agregar discriminador `recalculation_type` (`SPORT_AND_MARKET` o `MARKET_DEMAND_ONLY`) y duración de ventana. La demanda neta se calcula y persiste por jugador en `player_quotes`, no como un único valor de ejecución. Mantener inicio/fin, origen, límites y contadores existentes.

En `MARKET_DEMAND_ONLY`, estrategia y factor deportivo no representan valores usados y deben ser nulos; la migración permite su nulabilidad para ese modo. `variation_cap` y `min_price_floor` siguen aplicando. En modo habitual se conservan estrategia y factor deportivo. Los contadores por jugador particionan la población elegible conforme al contrato de SPEC-016.

## Esquema y migración

- Migración Flyway aditiva para los campos nuevos.
- Respetar filas históricas: columnas nuevas nullable cuando el dato no pueda reconstruirse.
- `market_transactions` no requiere migración.
- Hibernate mantiene `ddl-auto: validate`; el usuario verifica compatibilidad durante implementación/verificación.

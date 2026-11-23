# Modelo de datos — SPEC-026

## Modelo lógico

No se incorpora una entidad conceptual persistida nueva. Se amplía el registro financiero
`MarketTransaction` que ya conserva las partes, jugador, cantidad, importe, saldos y tenencias antes
y después.

### Venta oficial

| Dato | Representación | Origen |
|---|---|---|
| Comprador | `buyer_id` existente | Superusuario persistido |
| Vendedor | `seller_id` existente | Usuario autenticado persistido |
| Jugador y cantidad | `player_id`, `quantity` existentes | Solicitud y jugador visible |
| Cotización vigente | `quote_price BIGINT NULL` | `QuoteSource` leído para la operación |
| Comisión aplicada | `discount_percentage SMALLINT NULL` | Configuración del sistema; 10–99. El nombre de almacenamiento describe el porcentaje descontado. |
| Precio unitario pagado | `unit_price BIGINT` existente | Neto por token, centésimas |
| Total | `total_amount BIGINT` existente | `quantity * unit_price`, exacto |
| Snapshots financieros | columnas `*_before` / `*_after` existentes | Valores observados bajo bloqueo |
| Procedencia de cotización | `quote_calculated_at`, `quote_strategy_version` existentes | Cotización aplicada |
| Instante | `executed_at` existente | Reloj del mercado |

La aplicación exige `quote_price > 0`, `discount_percentage BETWEEN 10 AND 99`, `unit_price > 0`
y `total_amount = quantity * unit_price` para cada buyback aceptado. Si el redondeo del precio neto
resulta cero, rechaza la operación antes de liquidar. `quote_price` y
`discount_percentage` permanecen `NULL` para transacciones históricas y modalidades que no
representan un descuento.

### Publicación P2P

Se reutiliza `TokenListing`/`token_listings` sin columnas nuevas. El precio seleccionado o
predeterminado se persiste en `unit_price`; `total_amount` se calcula como cantidad por precio.
Los dos valores quedan inmutables durante las compras posteriores. No se almacena el precio
solicitado por separado del precio pactado.

## Migración

Una migración Flyway aditiva añade las dos columnas nullable a `market_transactions`. No modifica
migraciones existentes, no crea backfill y no cambia claves, índices ni relaciones. Los nombres y el
timestamp final se determinarán al implementar con el mecanismo vigente de migraciones; este plan
no genera migraciones.

La migración preserva los registros ya existentes. La nulabilidad distingue registros anteriores y
operaciones sin descuento; la aplicación garantiza que los nuevos buybacks siempre guarden ambos
atributos requeridos.

## Relaciones y consistencia

- Se mantienen las referencias existentes a usuario y jugador.
- No se agrega una referencia nueva: la venta oficial no liquida una publicación P2P.
- La transferencia de `UserCredit`, `PlayerToken` y el insert de `MarketTransaction` ocurre dentro
  de una sola transacción.
- El registro mantiene los snapshots pre/post ya presentes; el precio bruto y el descuento se
  guardan como valores aplicados, no se reconstruyen posteriormente.
- La cantidad disponible para vender es la tenencia bajo bloqueo menos la suma derivada de
  publicaciones `ACTIVE` y no vencidas, leída con el mismo instante de la operación.

## Reglas de cálculo

Para cotización `q` en centésimas y comisión entera `c`:

```text
netUnitPrice = roundHalfUp(q * (100 - c) / 100)
totalAmount = exactMultiply(quantity, netUnitPrice)
```

El caso de mitad se redondea hacia arriba. La implementación debe evitar overflow y rechazar
cantidades, cotizaciones, comisiones fuera de rango, precio neto cero o resultados no válidos antes
de confirmar transferencias.

Para precio P2P indicado `p`, la validación matemática es:

```text
80 * q <= 100 * p <= 120 * q
```

Los extremos son inclusivos; la comparación debe ser exacta y segura frente a overflow. Al omitir
`p`, `unit_price = q`. En ambos casos el total se calcula de forma exacta y se congela en la
publicación.

# Data Model: Historial personal de transacciones

## Contrato de transporte

### `OperationHistoryResponseDTO`

Representa un elemento de la respuesta HTTP existente.

| Campo | Tipo | Obligatorio | Semántica |
| --- | --- | --- | --- |
| `transactionId` | `number` | Sí | Identificador persistido y key estable de UI. |
| `type` | `"BUY" \| "SELL"` | Sí | Rol del usuario autenticado en la operación. |
| `playerId` | `number` | Sí | Identidad interna; no habilita navegación ni consulta adicional. |
| `playerName` | `string` | Sí | Nombre persistido del jugador mostrado como texto. |
| `quantity` | `number` | Sí | Tokens confirmados transferidos. |
| `unitPrice` | `number` | Sí | Precio histórico unitario en centésimas de crédito. |
| `totalAmount` | `number` | Sí | Importe histórico total en centésimas de crédito. |
| `executedAt` | `string` | Sí | Instante ISO-8601 con offset. |

La respuesta completa es `OperationHistoryResponseDTO[]`. No contiene wrapper, paginación,
totales agregados ni metadatos.

## Modelo frontend

### `TransactionHistoryEntry`

Modelo inmutable de lectura que consume la UI después del mapper.

| Campo | Tipo | Regla de presentación |
| --- | --- | --- |
| `transactionId` | `number` | Mostrar como `Transacción #N`; usar como key React. |
| `type` | `"BUY" \| "SELL"` | Mapear a `COMPRA` o `VENTA`; no inferir resultado financiero. |
| `playerId` | `number` | Conservar como dato del modelo; no mostrar ni navegar. |
| `playerName` | `string` | Texto plano, hasta dos líneas según espacio disponible. |
| `quantity` | `number` | Mostrar singular/plural de token sin alterar el valor. |
| `unitPrice` | `number` | Formatear en créditos con dos decimales. |
| `totalAmount` | `number` | Formatear en créditos con dos decimales y mayor jerarquía visual. |
| `executedAt` | `string` | Formatear fecha y hora con el formatter vigente de Cartera. |

DTO y Model mantienen tipos separados aunque la transformación sea 1:1.

## Colección y estados

### `TransactionHistory`

`TransactionHistoryEntry[]` recibida completa y ordenada. Reglas:

- conservar el orden del backend (`executedAt DESC, transactionId DESC`);
- no ordenar, deduplicar, paginar ni filtrar en cliente;
- derivar el contador exclusivamente de `entries.length`;
- `[]` representa éxito vacío, no error;
- no recalcular `totalAmount` desde cantidad y precio.

### Estados de consulta

```text
pending/fetching ──success──> data(entries.length > 0)
       │               └────> empty(entries.length = 0)
       └──error─────────────> error ──retry──> pending/fetching
```

- **loading**: no hay contador cero ni filas visibles.
- **data**: colección completa vigente.
- **empty**: resultado exitoso `[]`, con navegación a `/market/buy`.
- **error**: no se muestran datos parciales u obsoletos; se ofrece reintento.
- **unauthorized**: lo resuelve el ciclo global de sesión, no un estado de dominio nuevo.

## Validaciones y garantías

- El frontend confía en el contrato tipado; no agrega schemas runtime ad hoc.
- Los valores cero son válidos y se muestran.
- `playerId` no se usa para navegación.
- No existen transiciones ni mutaciones de una operación: todas las entradas son confirmadas y de
  solo lectura.
- No se agregan campos derivados persistentes ni estado cliente duplicado.

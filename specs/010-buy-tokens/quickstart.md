# Quickstart: Comprar tokens (SPEC-010)

Guía de verificación **posterior a la implementación**, a ejecutar por el usuario. No acredita capacidades existentes, resultados de pruebas ni gates.

---

## Preparación

- JDK 21 y PostgreSQL.
- Pruebas automatizadas con PostgreSQL 18 de Testcontainers y Docker; nunca contra la base de desarrollo.
- Un superusuario existente: sin él no hay vendedor y la compra no puede confirmarse.
- Cotización vigente disponible. Mientras REQ-S2-03 no exista, el simulador debe habilitarse de forma explícita; sin fuente real y sin simulador, la aplicación **no inicia**:

```bash
MARKET_QUOTE_SIMULATOR_ENABLED=true MARKET_QUOTE_SIMULATOR_PRICE=100 ./gradlew bootRun
```

El precio se expresa en centésimas: `100` equivale a 1 crédito por token, el valor inicial del enunciado. Con una fuente real configurada, el simulador no se registra aunque la propiedad esté habilitada: la ausencia de cotización de un jugador produce 409 `QUOTE_UNAVAILABLE` y nunca un precio simulado.

- Usuarios con saldo inicial y jugadores con emisión inicial completados. Verificar que cada jugador tiene 100 tokens asignados al superusuario y que cada usuario normal tiene 100 créditos.

---

## Compra principal (AC01)

Con un usuario de 100 créditos, el superusuario con 100 tokens del jugador y precio de 1 crédito por token:

```bash
curl -X POST http://localhost:8080/api/orders/buy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 1, "quantity": 10}'
```

Respuesta esperada:

```json
{
  "transactionId": 1,
  "playerId": 1,
  "quantity": 10,
  "unitPrice": 100,
  "totalAmount": 1000,
  "executedAt": "2026-10-16T10:30:00.123Z"
}
```

Efectos esperados: el comprador queda con 90 créditos y 10 tokens; el vendedor, con 10 créditos y 90 tokens. Existe exactamente un registro de auditoría con los estados anteriores y posteriores.

No simular compras alterando saldos o inventario mediante SQL: destruiría las invariantes que se quieren comprobar.

---

## Matriz de verificación

| Riesgo | Casos de `spec.md` | Resultado esperado |
|---|---|---|
| Seguridad y autorización | AC07, AC14 | 403 `SUPERUSER_CANNOT_BUY` para el superusuario; 401 sin sesión válida. Sin efectos financieros. |
| Entradas | AC04 | Literal entero representable como Integer fuera de 1–100 → 400 `INVALID_QUANTITY`. Cantidad ausente, nula, string, booleano, array, objeto, decimal, exponencial o entero fuera de Integer → 400 `INVALID_REQUEST` antes del Service. Sin coerciones, redondeo ni truncamiento. Cantidades válidas continúan las restantes validaciones. |
| Elegibilidad | AC05, AC12 | 404 con el código correspondiente según exista o no sea visible. Un `playerId` en `0` o negativo responde `PLAYER_NOT_FOUND`. Sin transferencias. |
| Cotización | AC06, AC15 | 409 `QUOTE_UNAVAILABLE` sin cotización; 409 `INVALID_QUOTE_PRICE` con precio no positivo. Sin precio inventado. |
| Cotización de otro jugador | Contrato de integración | 500 genérico con reversión completa: sin transferencias, sin saldos modificados y sin auditoría. No expone detalles técnicos. |
| Capacidad | AC02, AC03 | 409 con crédito insuficiente y con stock insuficiente, aislando la otra precondición. Sin compra parcial y sin revelar cifras. |
| Atomicidad y auditoría | AC16, FR-011/014 | Un fallo controlado al registrar la auditoría revierte saldos, tenencias y registro; no queda compra confirmada. |
| Concurrencia por stock | AC08 | Dos compradores y un único token: una compra confirma, la otra recibe 409 `INSUFFICIENT_STOCK`. El total del jugador se conserva. |
| Concurrencia por saldo | AC09 | Un comprador sobre dos jugadores distintos con saldo para una sola: una confirma; no hay doble gasto ni saldo negativo. |
| Primera tenencia concurrente | FR-012 | Dos compradores del mismo jugador sin tenencia previa: cada una obtiene la suya, sin filas duplicadas. |
| Orden de bloqueos | FR-011 | La concurrencia se verifica con las compras reales, no simulando el Service. |
| Compras repetidas | AC11 | Dos solicitudes idénticas financiadas producen dos identificadores y dos registros distintos. |
| Rango monetario | AC13 | Con un precio que exceda el rango soportado, la compra se rechaza con 500 genérico y sin efectos. |
| Desactivación concurrente | D3 | Si el jugador se desactiva antes de la validación, la compra se rechaza; si se desactiva después, la compra en curso se completa. No se exige orden estricto entre ambos procesos. |
| Emisión inicial | AC17, FR-016 | Un jugador nuevo recibe 100 tokens en su creación; reejecutar la inicialización no repone inventario vendido. |
| Créditos iniciales | AC10, AC18, FR-015 | Altas y usuarios preexistentes con saldo inicial una sola vez; el superusuario con 0. |

Las pruebas de integridad y concurrencia requieren conexiones y transacciones independientes. Cada caso concurrente debe usar su propia conexión y confirmar su transacción; una prueba transaccional que nunca confirma no reproduce el problema real.

La distribución de responsabilidades es la siguiente:

| Capa | Verifica |
|---|---|
| Controller | Respuestas HTTP, códigos, estructura de errores, restricciones de entrada y ausencia de invocación del Service en rechazos propios |
| Service con PostgreSQL real | Que una compra rechazada no modifica créditos, no crea tenencias, no genera auditoría y revierte por completo |
| Concurrencia | Secuencia de bloqueos, sobreventa, doble gasto y primera tenencia concurrente |

Un doble de prueba no constituye evidencia de integridad transaccional. Respetar las fronteras de `docs/backend/testing.md`: el caso de uso se verifica con persistencia real, las consultas de bloqueo y restricciones en pruebas de repositorio, y el contrato HTTP en pruebas de controlador.

---

## Comprobación de la auditoría

Sobre la base de desarrollo, con un comprador conocido:

```sql
SELECT id, buyer_id, seller_id, player_id, quantity, unit_price, total_amount, executed_at,
       buyer_credits_before, buyer_credits_after,
       seller_credits_before, seller_credits_after,
       buyer_tokens_before, buyer_tokens_after,
       seller_tokens_before, seller_tokens_after
FROM market_transactions
WHERE buyer_id = :buyer_id
ORDER BY executed_at DESC, id DESC
LIMIT 20;
```

`:buyer_id` es un parámetro a enlazar con el cliente SQL, no texto para ejecutar literalmente.

Verificar que el importe coincide con `quantity * unit_price` y que la diferencia entre los saldos del comprador y del vendedor es exactamente el importe en direcciones opuestas.

No modificar ni borrar auditoría: el registro histórico no admite cambios ni retención.

---

## Límites aceptados

- Compra y desactivación simultáneas no tienen orden estricto garantizado; la elegibilidad se valida en el momento de la comprobación. La compra no bloquea la sincronización del catálogo.
- La corrupción manual de datos no dispara reparación automática.
- El simulador de cotizaciones no acredita la integración real de REQ-S2-03, no debe activarse fuera de desarrollo y pruebas y nunca sustituye una cotización real ausente.
- El puerto `QuoteSource` es permanente mientras desacople la compra; solo `SimulatedQuoteSource` es temporal y se elimina con REQ-S2-03.
- Los objetivos de rendimiento son una propuesta de referencia, no una medición. La integridad concurrente es obligatoria con independencia del rendimiento.

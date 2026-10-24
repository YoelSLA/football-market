# Guía de validación: Historial personal de operaciones (SPEC-020)

**Feature**: SPEC-020 · **Rama**: `feat/020-view-operation-history` · **Fecha**: 2026-10-24

Esta guía describe cómo validar el resultado; no implementa la feature. La ejecución de tests y de
los comandos de prueba corresponde al usuario. Ninguno se ejecutó durante PLAN.

## 1. Prerrequisitos

- Java 21.
- Docker disponible para las pruebas con PostgreSQL/Testcontainers.
- Implementación de SPEC-020 completada y aplicación configurada con JWT.
- Datos aislados con dos usuarios distintos, jugadores y transacciones confirmadas de compra y venta.

## 2. Comandos para el usuario

Desde `backend/`:

```bash
./gradlew lightweightTest
./gradlew integrationTest
./gradlew test
```

El control permitido al agente después de cambios de código backend, que no acredita tests, es:

```bash
./gradlew build -x test
```

## 3. Escenarios automatizados esperados

| Categoría | Escenario | Resultado esperado |
| --- | --- | --- |
| Controller | JWT válido | 200 y array con los ocho campos exactos del contrato |
| Controller | Sin JWT o JWT inválido | 401 y Service no invocado |
| Controller | Respuesta vacía | 200 con `[]` |
| Repository | Usuario comprador y vendedor | Recupera ambas transacciones una vez cada una |
| Repository | Transacciones ajenas | No aparecen |
| Repository | Mismo `executedAt` | Orden descendente estable por `id` |
| Service | Comprador | Tipo `BUY` y valores históricos intactos |
| Service | Vendedor | Tipo `SELL` y valores históricos intactos |
| Service | Usuario sin operaciones | Colección vacía |
| Regresión | Consulta repetida sobre el mismo estado | Devuelve la misma colección ordenada y sin filas duplicadas |

## 4. Validación HTTP manual

Con la aplicación levantada por el usuario y un token de prueba en `TOKEN`:

```bash
curl -i \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/me/transactions
```

Comprobar:

1. Solo aparecen operaciones de la identidad del token.
2. Una compra se presenta como `BUY` y una venta como `SELL`.
3. Cada transacción aparece una sola vez.
4. El primer elemento es el más reciente; los empates se resuelven por id descendente.
5. `unitPrice` y `totalAmount` coinciden con el registro histórico, en centésimas.
6. Un usuario sin operaciones recibe `[]`.

Sin cabecera:

```bash
curl -i http://localhost:8080/api/users/me/transactions
```

Resultado esperado: `401`, sin exposición de operaciones.

## 5. Referencias

- Contrato: [`contracts/operation-history-api.md`](contracts/operation-history-api.md)
- Modelo: [`data-model.md`](data-model.md)
- Decisiones: [`research.md`](research.md)
- Especificación: [`spec.md`](spec.md)

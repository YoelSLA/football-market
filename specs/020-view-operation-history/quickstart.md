# Guía de validación: Historial personal de operaciones (Backend)

**Estado**: Guía de validación backend vigente.
**Referencias normativas**: [spec.md](spec.md), [contracts/operation-history-api.md](contracts/operation-history-api.md) y [plan.md](plan.md).

---

## 1. Verificaciones automatizadas

Comandos ejecutables desde el directorio `backend/` para validar la implementación:

```bash
# Pruebas de integración con PostgreSQL real (Testcontainers)
./gradlew integrationTest --tests '*OperationHistoryPersistenceTest' --tests '*OperationHistoryServiceTest'

# Pruebas unitarias y de capa web (MockMvc)
./gradlew lightweightTest --tests '*OperationHistoryControllerTest'
```

Control de compilación backend sin ejecución de pruebas:

```bash
./gradlew build -x test
```

---

## 2. Escenarios de validación HTTP manual

Con la aplicación en ejecución y un token JWT válido exportado en `TOKEN`:

### Escenario 1: Consulta básica paginada

```bash
curl -i \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/users/me/transactions?page=0&size=20&sort=NEWEST"
```

**Verificaciones esperadas**:
1. Respuesta HTTP 200 con estructura que contiene `summary`, `transactions` y `pagination`.
2. Las transacciones se presentan tipificadas como `BUY` o `SELL` según el rol de la sesión.
3. El primer elemento es el más reciente en el tiempo (`executedAt DESC`).
4. Cada transacción incluye el perfil actual del jugador (`id`, `name`, `team`, `league`) y valores históricos inmutables (`quantity`, `unitPrice`, `totalAmount`).

### Escenario 2: Filtrado por tipo y búsqueda

```bash
curl -i \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/users/me/transactions?type=BUY&search=Messi"
```

**Verificaciones esperadas**:
1. Todas las transacciones devueltas tienen `type: "BUY"`.
2. El resumen `summary` refleja únicamente los totales e importes del subconjunto filtrado.

### Escenario 3: Validación de rango de fechas erróneo (HTTP 400)

```bash
curl -i \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/users/me/transactions?executedFrom=2026-11-10T15:00:00Z&executedBefore=2026-11-10T14:00:00Z"
```

**Verificaciones esperadas**:
1. Respuesta HTTP 400 Bad Request.
2. Cuerpo con código `INVALID_DATE_RANGE`.

### Escenario 4: Protección de autenticación (HTTP 401)

```bash
curl -i http://localhost:8080/api/users/me/transactions
```

**Verificaciones esperadas**:
1. Respuesta HTTP 401 Unauthorized sin revelar datos.

### Escenario 5: Usuario sin transacciones o sin coincidencias

Con un token de un usuario sin operaciones confirmadas:

**Verificaciones esperadas**:
1. Respuesta HTTP 200 OK.
2. `transactions: []`.
3. Todos los campos de `summary` en cero (`totalTransactions: 0`, `purchasedVolume: 0`, etc.).
4. `pagination.totalElements: 0` y `pagination.totalPages: 0`.

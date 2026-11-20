# Quickstart: Mercado Oficial — Compra de tokens y catálogo con cotizaciones integradas (SPEC-010)

Guía de verificación **posterior a la implementación**, a ejecutar por el usuario. No acredita capacidades existentes, resultados de pruebas ni gates.

---

## 1. Verificación de Compra de Tokens (`POST /api/orders/buy`)

### Compra exitosa principal (AC01)
Con un usuario de 100 créditos, el superusuario con 100 tokens del jugador y precio de 1 crédito por token:

```bash
curl -X POST http://localhost:8080/api/orders/buy \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 1, "quantity": 10}'
```

**Respuesta esperada (200 OK):**
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

---

## 2. Verificación del Catálogo del Mercado Oficial (`GET /api/market/official`)

### 2.1. Consulta básica paginada (AC20, AC21, AC23)
```bash
curl -X GET "http://localhost:8080/api/market/official?page=0&size=20" \
  -H "Authorization: Bearer $USER_TOKEN"
```
**Validaciones**:
- Responde `200 OK`.
- Metadatos presentes: `page: 0`, `size: 20`, `totalElements >= 0`, `totalPages >= 0`.
- Cada elemento contiene datos deportivos (`id`, `name`, `position`, `teamName`, etc.).
- Si el jugador tiene cotización vigente, `quote` incluye `price`, `previousPrice`, `variationPercentage`, `performanceScore`, `multiplier`, etc.
- Si el jugador no registra cotización, el campo `quote` es estrictamente `null`.
- **Sigilo**: La respuesta no contiene `stock`, `inventory` ni `availableTokens`.

### 2.2. Filtro de nombre con normalización trim y acentos (AC24)
```bash
# Nombre con espacios en los extremos y sin acento
curl -X GET "http://localhost:8080/api/market/official?name=%20messi%20" \
  -H "Authorization: Bearer $USER_TOKEN"
```
**Validaciones**:
- Responde `200 OK`.
- Devuelve a "Lionel Messi" independientemente de mayúsculas/minúsculas.
- `totalElements` refleja únicamente los jugadores coincidentes.

### 2.3. Filtro por disponibilidad de cotización `hasQuote` (AC25)
```bash
# Solo jugadores con cotización
curl -X GET "http://localhost:8080/api/market/official?hasQuote=true" \
  -H "Authorization: Bearer $USER_TOKEN"

# Solo jugadores sin cotización
curl -X GET "http://localhost:8080/api/market/official?hasQuote=false" \
  -H "Authorization: Bearer $USER_TOKEN"
```
**Validaciones**:
- En `hasQuote=true`, ningún jugador tiene `quote: null`.
- En `hasQuote=false`, el 100% de los jugadores tiene `quote: null`.

### 2.4. Filtros financieros y validación de rango (AC26, AC27, AC28)
```bash
# Rango válido: entre 1000 y 50000 centésimas (10.00 a 500.00 créditos)
curl -X GET "http://localhost:8080/api/market/official?minPrice=1000&maxPrice=50000" \
  -H "Authorization: Bearer $USER_TOKEN"
# -> 200 OK

# Rango inválido (minPrice > maxPrice)
curl -X GET "http://localhost:8080/api/market/official?minPrice=5000&maxPrice=1000" \
  -H "Authorization: Bearer $USER_TOKEN"
# -> 400 Bad Request (code: INVALID_PRICE_RANGE)

# Precios negativos
curl -X GET "http://localhost:8080/api/market/official?minPrice=-100" \
  -H "Authorization: Bearer $USER_TOKEN"
# -> 400 Bad Request (code: INVALID_PRICE_RANGE)

# Filtros mutuamente excluyentes (sin cotización y con rango de precios)
curl -X GET "http://localhost:8080/api/market/official?hasQuote=false&minPrice=1000" \
  -H "Authorization: Bearer $USER_TOKEN"
# -> 200 OK con content: [], totalElements: 0, totalPages: 0
```

### 2.5. Ordenamiento con `NULLS LAST` (AC29, AC30)
```bash
# Orden por precio ascendente
curl -X GET "http://localhost:8080/api/market/official?sort=price,asc" \
  -H "Authorization: Bearer $USER_TOKEN"

# Orden por variación descendente
curl -X GET "http://localhost:8080/api/market/official?sort=variation,desc" \
  -H "Authorization: Bearer $USER_TOKEN"

# Múltiples criterios de ordenamiento (rechazado)
curl -X GET "http://localhost:8080/api/market/official?sort=price,desc&sort=name,asc" \
  -H "Authorization: Bearer $USER_TOKEN"
# -> 400 Bad Request (code: INVALID_SORT_PARAMETER)
```
**Validaciones**:
- En orden por precio o variación, los jugadores sin cotización (o sin variación previa) aparecen al final del catálogo completo.
- Desempate estable: ante precios idénticos, el orden secundario `id ASC` asegura que no se repitan jugadores entre páginas consecutivas.

---

## 3. Matriz de verificación rápida de errores

| Caso | Solicitud | Código HTTP | Error Code |
|---|---|---|---|
| Rango de precio invertido | `?minPrice=500&maxPrice=100` | `400` | `INVALID_PRICE_RANGE` |
| Precio negativo | `?minPrice=-10` | `400` | `INVALID_PRICE_RANGE` |
| Página negativa | `?page=-1` | `400` | `INVALID_PAGINATION` |
| Tamaño de página excedido | `?size=150` | `400` | `INVALID_PAGE_SIZE` |
| Criterio de orden inválido | `?sort=salary,desc` | `400` | `INVALID_SORT_PARAMETER` |
| Múltiples criterios sort | `?sort=price,asc&sort=name,asc` | `400` | `INVALID_SORT_PARAMETER` |
| Sin autenticación | Sin header `Authorization` | `401` | — |


---

## 4. Verificación de equivalencia canónica y plan de ejecución

A ejecutar por la persona usuaria para validar la paridad con `features/quote`:

```bash
./gradlew test --tests "footballmarket.features.market.OfficialMarketQuoteEquivalenceTest"
```

**Comprobaciones de ejecución**:
1. Comprobar que ante diferentes escenarios de cotización (inicial, arrastrada, recalculada), `OfficialMarketRepository` seleccione exactamente la misma cotización que `PlayerQuoteRepository.findLatestByPlayerId`.
2. Inspeccionar el plan de ejecución SQL (`EXPLAIN`) en el entorno de pruebas para verificar que la subconsulta lateral aproveche el índice `ix_player_quotes_player_date` sin incurrir en escaneos secuenciales innecesarios.

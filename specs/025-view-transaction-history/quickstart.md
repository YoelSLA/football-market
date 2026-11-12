# Quickstart: Historial personal paginado

## Contrato

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/users/me/transactions?page=0&size=20&type=BUY&sort=NEWEST"
```

Comprobar `summary`, `transactions`, perfil actual del jugador y `pagination`. Repetir con
búsqueda, fechas y los cuatro órdenes. Un rango inválido debe devolver `INVALID_DATE_RANGE`.

## Backend — ejecución humana

Los tests relevantes son:

```bash
./gradlew integrationTest --tests '*OperationHistoryPersistenceTest' --tests '*OperationHistoryServiceTest'
./gradlew lightweightTest --tests '*OperationHistoryControllerTest'
```

El agente no los ejecuta. El build permitido para el agente es `./gradlew build -x test`.

## Frontend

Comprobar en desktop:

1. URL `/portfolio/transactions` y pestaña activa.
2. Cuatro indicadores coherentes con filtros.
3. Búsqueda, tipo, fechas y orden reflejados en query string.
4. Paginación backend sin duplicados.
5. Imagen primaria, fallback y retrato genérico.
6. Región de altura consistente: carga, error, vacío y sin resultados con contenido compacto centrado; filas naturales y paginación inferior.
7. Nombre, imagen y perfil sin navegación.

Controles permitidos:

```bash
npm run biome:fix
npm run build
```

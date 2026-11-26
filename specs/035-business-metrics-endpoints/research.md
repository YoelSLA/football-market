# Investigación: métricas de negocio

## Decisiones

### Dependencias y memoria de ejecución

- **Decisión:** reutilizar Micrometer y la configuración de Spring Boot ya presentes; registrar contadores de cardinalidad fija, sin tags que identifiquen usuarios, jugadores u operaciones individuales.
- **Fundamento:** la especificación exige contadores Micrometer desde el arranque y el stack ya incluye Micrometer Prometheus Registry. No se requiere nueva dependencia ni exposición por Prometheus.
- **Alternativas consideradas:** agregar otra biblioteca o persistir el estado de ejecución. Ambas opciones duplican capacidades existentes o contradicen el reinicio por proceso y la separación del histórico.

### Fuente histórica

- **Decisión:** agregar las filas confirmadas de `market_transactions` y contar filas de `token_listings` cuyo estado persistido sea `CANCELLED`.
- **Fundamento:** `MarketTransaction` es append-only y conserva cantidades e importes en centésimas; `EXPIRED` es un estado efectivo derivado, mientras que la cancelación se persiste explícitamente.
- **Alternativas consideradas:** reconstruir movimientos desde saldos antes/después o sumar por las dos partes. Se rechazan porque duplican una fila financiera o hacen innecesaria la reconstrucción: cada transacción ya contiene `quantity` y `total_amount`.

### Clasificación histórica compra/venta

- **Decisión:** compra si `buyer.role = USER`; venta si `buyer.role = SUPERUSER` y `seller.role = USER` (recompra oficial). Por tanto, una liquidación P2P con comprador usuario se clasifica una sola vez como compra; no se cuenta también como venta.
- **Fundamento:** la dirección del comprador en la fila financiera distingue el flujo de compra del flujo de venta oficial sin duplicar la fila entre categorías.
- **Alternativa considerada:** contar como venta cada fila donde cualquier usuario sea vendedor. Se rechaza porque duplicaría las liquidaciones P2P entre categorías.

### Contrato HTTP

- **Decisión:** ambas rutas comparten un único contrato JSON documentado en `contracts/http.md`; la API usa DTO `record`, anotaciones OpenAPI y seguridad existente.
- **Fundamento:** la estructura común y la autenticación son requisitos explícitos; el backend documenta contratos HTTP en contratos de spec, Controllers/DTO y pruebas Controller.
- **Alternativas consideradas:** respuestas diferenciadas o incluir detalle por usuario/jugador. No satisfacen la estructura común y exponen dimensiones expresamente excluidas.

## Contexto técnico inspeccionado

- Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security, PostgreSQL y Micrometer ya integrados.
- `MarketTransactionRepository` contiene agregaciones SQL sobre filas financieras y `MarketTransaction` conserva quantity/totalAmount en centésimas.
- `TokenListing.status` persiste `ACTIVE`, `SOLD` o `CANCELLED`; `EXPIRED` se deriva en lectura y no equivale a cancelación.
- No se identifican incógnitas técnicas pendientes para diseñar estos dos endpoints. La clasificación de operación queda explicitada para evitar doble cómputo P2P.

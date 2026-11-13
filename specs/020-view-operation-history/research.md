# Research: Historial personal de operaciones (Backend)

**Estado**: Registro consolidado de decisiones técnicas vigentes.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md) y [contracts/operation-history-api.md](contracts/operation-history-api.md).

---

## 1. Decisiones arquitectónicas y de diseño

| Decisión | Justificación técnica | Alternativas descartadas |
| --- | --- | --- |
| **Recurso personal `/api/users/me/transactions`** | Obtener la identidad directamente del subject del JWT impide cualquier vector de suplantación o fuga de datos privados entre usuarios. | `/api/users/{id}/transactions` (inseguro); `/api/portfolio/me` (acoplamiento indebido de recursos). |
| **Fuente de verdad única en `MarketTransaction`** | La tabla `market_transactions` ya registra cada operación confirmada de forma inmutable. La consulta deriva `BUY` o `SELL` según si el usuario es comprador o vendedor, sin crear registros redundantes. | Crear tabla paralela para historial; duplicar filas por transacción. |
| **Consulta optimizada con fetch joins sin N+1** | La consulta de contenido precarga las asociaciones to-one (`buyer`, `seller`, `player`, `team`, `league`) en una sola sentencia SQL, paginando a nivel de base de datos. | Consultar el repositorio de jugadores por cada fila; paginar en memoria de la JVM. |
| **Agregados calculados directamente en SQL** | La consulta de agregados (`OperationHistoryAggregate`) calcula sumas y conteos condicionales en el motor de base de datos sobre el conjunto filtrado, sin traer colecciones masivas a memoria. | Calcular agregados iterando en memoria; mantener tablas de agregados precalculadas. |
| **Orden cerrado con desempates estables** | Se restringe el orden a cuatro opciones controladas (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`), incorporando siempre desempates por fecha de ejecución e ID de transacción para asegurar paginación reproducible. | Permitir nombres de columna arbitrarios en la URL; omitir desempates deterministas. |
| **Perfil actual del jugador en la lectura** | Nombre, imágenes, equipo y liga se resuelven desde el estado actual del catálogo. Los valores financieros (`quantity`, `unitPrice`, `totalAmount`, `executedAt`) se conservan estrictamente históricos. | Crear snapshots históricos redundantes de datos del jugador; congelar imágenes en la transacción. |
| **Límite transaccional `@Transactional(readOnly = true)`** | Garantiza formalmente que el caso de uso es de solo lectura y permite al driver de PostgreSQL y a Hibernate optimizar las consultas sin sobrecarga de dirty checking. | Ejecutar sin transacción explícita; elevar innecesariamente el aislamiento. |
| **Importes enteros en centésimas de crédito** | Preserva la convención de tipos enteros del sistema financiero del mercado (`long`), evitando errores de precisión asociados a coma flotante. | Usar `double` o `float`; convertir a decimales en el transporte backend. |

# Investigación: Historial personal de operaciones (SPEC-020)

## Decisión 1 — Recurso HTTP personal

**Decision**: Exponer `GET /api/users/me/transactions`, sin path variable, query parameter ni body.

**Rationale**: Conserva la intención académica de `/users/:id/transactions`, reemplaza el selector
inseguro por `me`, respeta el prefijo `/api/` y hace imposible consultar otra identidad desde el
contrato. El JWT entrega el correo y el Service resuelve el usuario persistido.

**Alternatives considered**: `/api/users/{id}/transactions` se rechaza por FR-002; reutilizar
`/api/portfolio/me` mezcla recursos distintos; `/api/transactions/me` es válido pero menos alineado
con el recurso obligatorio de usuario.

## Decisión 2 — Fuente e interpretación

**Decision**: Leer una sola vez `MarketTransaction` por `buyer.id = :userId OR seller.id = :userId` y
derivar `BUY` si el usuario es comprador o `SELL` si es vendedor.

**Rationale**: La tabla ya representa cada operación confirmada una vez y conserva cantidad, precio,
importe e instante históricos. SPEC-010 y SPEC-011 garantizan que comprador y vendedor sean
distintos; esta consulta consume esa garantía para derivar un tipo inequívoco sin duplicar registros
ni introducir otra fuente financiera.

**Alternatives considered**: unir dos consultas puede duplicar o exigir mezcla posterior; persistir
un tipo por usuario duplica información; reutilizar posiciones financieras agrega cálculos ajenos al
historial y pierde la transacción como unidad.

## Decisión 3 — Consulta, relaciones y orden

**Decision**: Añadir al repositorio una consulta dedicada que cargue comprador, vendedor y jugador y
ordene por `executedAt DESC, id DESC` en PostgreSQL.

**Rationale**: Evita N+1, devuelve directamente el orden contractual y aprovecha los índices
existentes por comprador y vendedor con fecha e identificador descendentes. La consulta vigente
`findFinancialHistory` se mantiene porque su orden ascendente sirve a la reconstrucción contable.

**Alternatives considered**: cambiar `findFinancialHistory` rompería a SPEC-015; ordenar en memoria
consume recursos innecesarios; cargar el grafo completo del catálogo no aporta datos al contrato.

## Decisión 4 — Límite transaccional

**Decision**: Ejecutar el Service con `@Transactional(readOnly = true)`.

**Rationale**: Cumple FR-015 y expresa que el caso de uso no escribe. Una única consulta obtiene una
instantánea suficiente; no se requiere elevar el aislamiento ni coordinar varias fuentes.

**Alternatives considered**: `REPEATABLE_READ` no aporta una garantía observable adicional para una
sola lectura; operar sin límite explícito no documenta la garantía requerida.

## Decisión 5 — Forma y unidades del contrato

**Decision**: Responder un array JSON directo. Cada elemento contiene `transactionId`, `type`,
`playerId`, `playerName`, `quantity`, `unitPrice`, `totalAmount` y `executedAt`; importes en centésimas
enteras y fecha ISO-8601 con offset.

**Rationale**: Son exactamente los ocho datos de FR-006, sin metadatos de paginación prohibidos. Los
tipos preservan la representación persistida (`long`, `int`, `OffsetDateTime`) sin recalcular.

**Alternatives considered**: un wrapper agrega estructura sin necesidad; decimales de créditos
cambiarían la convención del mercado; incluir contrapartes expone datos no solicitados.

## Decisión 6 — Cobertura de verificación

**Decision**: Planificar pruebas de Repository para query y orden, Service para identidad/rol/vacío y
Controller para contrato, JWT y serialización. No crear un E2E nuevo salvo que TASKS demuestre un
journey crítico no cubierto.

**Rationale**: Son los niveles más específicos definidos por `docs/backend/testing.md`; cubren query
compleja, caso de uso y frontera HTTP sin duplicación.

**Alternatives considered**: solo Controller no prueba persistencia; solo E2E dificulta localizar
fallos y repite infraestructura; tests directos de Mapper están prohibidos por la norma vigente.

## Clarificaciones técnicas resueltas

No quedan marcadores `NEEDS CLARIFICATION`. La definición y validación de SLA de rendimiento queda
fuera de SPEC-020 y se abordará como requisito transversal en el próximo sprint. El objetivo técnico
de este diseño es una consulta de conjunto indexada, sin N+1.

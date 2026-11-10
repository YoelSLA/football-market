# Research: Historial personal de transacciones

## Decisión 1 — Extender la feature `portfolio`

**Decision**: Ubicar modelos, mapper, Service, Query Hook, Page y componentes en
`features/portfolio`.

**Rationale**: El historial es información financiera personal, comparte navegación, formatters y
frontera HTTP con Cartera, y la arquitectura organiza por feature propietaria del concepto.

**Alternatives considered**:

- Crear `features/transactions`: fragmentaría una capacidad que solo consume Cartera.
- Ubicarla en `market`: mezclaría actividad retrospectiva personal con acciones de compra/venta.
- Crear componentes en `shared`: no existe reutilización real entre features.

## Decisión 2 — Consumir el endpoint existente sin cambios backend

**Decision**: Usar `GET /api/users/me/transactions` mediante el cliente `http` autenticado, sin
parámetros ni body.

**Rationale**: El contrato ya entrega todas las compras y ventas confirmadas de la identidad JWT,
ordenadas de manera descendente y con los campos requeridos.

**Alternatives considered**:

- Agregar paginación o filtros backend: fuera de alcance y contrario al contrato aprobado.
- Consultar jugadores para enriquecer filas: aumenta solicitudes y muestra datos no respaldados.
- Derivar identidad desde parámetros: vulnera el contrato privado basado en sesión.

## Decisión 3 — Mantener DTO y Model separados con mapper explícito

**Decision**: Declarar `OperationHistoryResponseDTO` y `TransactionHistoryEntry`, y mapear cada
elemento 1:1 dentro del Service.

**Rationale**: Los artículos 17–19 de arquitectura exigen que DTO no escape del Service y que toda
transformación DTO → Model pase por Mapper, aun cuando los campos coincidan.

**Alternatives considered**:

- Exponer el DTO al Query Hook: rompe la frontera arquitectónica.
- Validar el contrato con un schema runtime ad hoc: no existe una necesidad aprobada y duplicaría
  el contrato tipado.

## Decisión 4 — Query Hook sin Page Hook adicional

**Decision**: Crear `useTransactionHistoryQuery` y consumirlo directamente desde la Page.

**Rationale**: TanStack Query ya resuelve estado remoto, cancelación y `refetch`; la pantalla no
tiene filtros, selección, paginación ni coordinación compleja que justifique otro Hook.

**Alternatives considered**:

- Crear `useTransactionHistoryPage`: sería una delegación sin responsabilidad propia.
- Guardar el array en Zustand o estado local: duplicaría estado remoto y podría conservar datos de
  otra sesión fuera de Query Cache.

## Decisión 5 — Componentes locales y composición responsive única

**Decision**: Delegar cabecera, contenido/estados y representación de una operación en componentes
locales de la Page; adaptar la misma colección por CSS y markup semántico adecuado al breakpoint.

**Rationale**: La Page presenta múltiples estados y una composición visual específica. La
localidad evita abstraer una tabla genérica y permite respetar el wireframe sin acoplarla a
tenencias o rankings.

**Alternatives considered**:

- Reutilizar tarjetas de tenencias: sus props y semántica incluyen valuación y detalle de jugador.
- Crear una tabla global: no existe un segundo consumidor que justifique el contrato.
- Renderizar tabla y tarjetas duplicadas y ocultar una: duplica contenido accesible y trabajo de
  mantenimiento.

## Decisión 6 — Formato y temporalidad existentes

**Decision**: Reutilizar `portfolio/utils/formatCredits` y `formatDateTime` si cubren importes en
centésimas y timestamps ISO-8601; conservar valores y orden del backend.

**Rationale**: Evita duplicación y mantiene consistencia con Cartera. El frontend solo presenta
datos históricos y no recalcula precio, total ni zona temporal del contrato actual.

**Alternatives considered**:

- Formatter nuevo para la pantalla: duplicación innecesaria.
- Recalcular `quantity × unitPrice`: puede divergir del importe histórico persistido.
- Reordenar por fecha en cliente: altera el contrato ya ordenado y su desempate estable.

## Decisión 7 — Política de error sin datos obsoletos visibles

**Decision**: Ante carga o error activo, no presentar la colección cacheada como historial vigente;
ofrecer `refetch` y delegar 401 al ciclo global de sesión.

**Rationale**: La especificación exige distinguir inequívocamente espera, error y vacío, y no
autoriza una política visual de datos obsoletos.

**Alternatives considered**:

- Mostrar filas previas junto al error: puede interpretarse como estado vigente.
- Manejar 401 localmente: duplicaría la responsabilidad de autenticación global.

## Resultado de investigación

No quedan `NEEDS CLARIFICATION`. Todas las decisiones se resuelven con el contrato backend, la
arquitectura frontend, la especificación y el wireframe aprobados.

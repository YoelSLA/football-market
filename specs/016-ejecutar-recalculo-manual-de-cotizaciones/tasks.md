---

description: "Task list template for feature implementation"
---

# Tasks: Cotización de tokens de jugadores con historial trazable

## Integración vigente con SPEC-023 — 2026-11-11

**Fuentes**: decisiones de integración de ambas `spec.md`, diseño compartido en
[`integration-baseline.md`](../023-scheduler-quote-recalculation/integration-baseline.md),
encabezados vigentes de plan/research/modelo/contratos/quickstart de ambas specs.
**Rama destino**: `feat/023-daily-quote-scheduling`. **Estado**: corrección documental autorizada,
ANALYZE conjunto pendiente; no se implementan estas tareas en esta operación.

Se conservan T001–T080 y sus marcadores como evidencia del trabajo de origen. Los IDs no se
renumeran ni se regeneran; sus marcas no acreditan la combinación ni habilitan consumir todo el
backlog. Las reglas de primera cotización inmutable, omisión por igualdad de fecha, eliminación
total de `ExecutionAlreadyExistsException` y automática ilimitada están sustituidas por las specs
vigentes. El pendiente T077 con SPEC-022 permanece separado, no se declara resuelto.

### Tareas acotadas de integración (pendientes)

**Evidencia posterior (2026-11-11)**: resolución de merge implementada y build sin tests
satisfactorio; [informe compartido](../023-scheduler-quote-recalculation/merge-integration-report.md).
Las marcas amplias conservan pendientes de comprobación y no acreditan testing humano.

**Comprobación posterior al merge `23e175c`**: T082/T084/T085 marcadas por implementación
inspeccionada; T081/T083 conservan faltantes de cobertura explícitos y T086 depende del alcance
restante. Evidencias y limitaciones en el informe compartido, sección «Comprobación de tareas
posterior al merge». Las marcas no acreditan tests ni cierre de la Feature.

- [ ] T081 Escribir/adaptar regresiones backend en `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`, `QuoteMarketInitializationServiceTest.java`, `QuoteAutomaticRecalculationFailureTest.java` y `backend/src/test/java/footballmarket/features/quote/domain/models/PlayerQuoteTest.java` (crear solo si no existe cobertura suficiente): dos manuales del día con una fila y último cálculo válido, misma identidad y última referencia de ejecución; score antiguo diario, cambio de estrategia/score, actualización de INITIAL y repetición idempotente de initialize, fallo sin reemplazo parcial y omisión solo ante fecha posterior. Relojes controlados y bloqueo de jugador; respetar fronteras de tests, fixtures solo Arrange y APIs públicas para Assert. No ejecutar pruebas.
- [X] T082 Reconciliar `backend/src/main/java/footballmarket/features/quote/domain/models/PlayerQuote.java`, `application/internal/QuotePlayerPublisher.java`, `infraestructure/PlayerQuoteRepository.java` y `application/impl/QuoteMarketInitializationServiceImpl.java`: último cálculo sobre precio vigente, actualizar fila igual al día de corrida y todos sus datos incluido `execution_id`, conservando identidad/fecha; insertar si falta, omitir solo fecha vigente posterior. Mantener bloqueo por jugador compartido, `REQUIRES_NEW`, fecha UTC inicial y score disponible aunque antiguo. Conservar mercado inicializado tras reemplazar INITIAL, sin reinicializar ni borrar; revisar consumidores del estado/fecha de mercado antes de cambiar su consulta (depende de T081 y esquema compatible T084).
- [ ] T083 Adaptar en `backend/src/test/java/footballmarket/features/quote/application/QuoteQueryServiceTest.java`, `infraestructure/PlayerQuoteMigrationTest.java`, `infraestructure/QuoteMarketOperationPersistenceTest.java` e `infraestructure/RepositoryQuoteSourceTest.java` la verificación persistida de último cálculo diario, FK actualizada y una fila por fecha, consulta vigente/historial/fecha y mercado/lectura por lote preservados. Añadir assertions de identidad y datos persistidos por la frontera de cada categoría, no solo retorno en memoria. No cambiar firmas de QuoteSource ni precios congelados de operaciones; no ejecutar pruebas (depende de T081–T082).
- [X] T084 Reconciliar el esquema de ambas ramas y pruebas en `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java` y `PlayerQuoteMigrationTest.java`: secuencia compatible de `backend/src/main/resources/db/migration/V20261110235437__daily_quote_recalculation.sql` y `V20261111163841__allow_daily_quote_recalculation.sql`, sin retirar dos veces constraints. Verificar primero si son editables según integración/aplicación; si no, preservar checksums y generar nueva migración con tarea oficial. Conservar unicidad jugador-fecha, FK de ejecución, fin nullable, contadores y orden de finalizadas; añadir unicidad parcial AUTOMATIC de SPEC-023. Prevalidar colisiones/datos incompatibles, detener sin limpieza/backfill automático. Escribir pruebas de secuencia y rechazo de segunda automática, admisión manual y FK del cálculo reemplazado; no ejecutar migraciones ni tests. Esta tarea es la única propietaria de la reconciliación DDL compartida.
- [X] T085 Reconciliar `backend/src/main/java/footballmarket/features/quote/domain/models/QuoteExecution.java`, `application/impl/QuoteRecalculationServiceImpl.java`, `presentation/QuoteController.java`, `QuoteExecutionMapper.java` y `presentation/dtos/response/QuoteExecutionResponseDTO.java` con `contracts/quotes-http.md`: `executionId` contractual y finalizadas por inicio/ID, contadores disjuntos (reemplazo no es omisión), omisión solo fecha posterior, manuales sin rechazo de fecha y sin exposición de detalles técnicos. Adaptar `backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java` a la respuesta integrada y conservar ranking/consumidores. Coordinar con T043 de SPEC-023 sobre reserva de contextos automáticos y excepción; no eliminar esa excepción ni duplicar el recorder. No ejecutar pruebas (depende de T081–T084 y T043 de SPEC-023).
- [ ] T086 Tras la implementación de integración autorizada y T043–T045 de SPEC-023, actualizar informe integral y evidencias de ambas specs sin reescribir históricos. Si cambió código backend, ejecutar solo `gradlew.bat build -x test` desde `backend/`, con dependencias inspeccionadas; revisión final de diff/`git diff --check`/estado. Mantener Flyway pendiente hasta salida humana, tests exclusivamente del usuario, TD-024 pendiente y gates separados. No ejecutar build durante esta alineación documental ni afirmar publicación del merge sin observarla.

**Orden conjunto**: pruebas antes del comportamiento → T084 (esquema compartido) → T082
(publicación/estado) y T043 de SPEC-023 (reserva/disparador, en secuencia si comparten archivo) →
T083/T085 y T044–T045 → T086/T046. T081 no depende del DDL para escribirse; la ejecución humana sí
requiere esquema reconciliado. No paralelizar ediciones del mismo archivo ni crear tareas duplicadas
en ambas specs. T077 no es dependencia para corregir la actualización diaria; su condición de entrega
coordinada con SPEC-022 permanece vigente para el ranking completo.

**Checklists**: marcadores del revisor preservados. Corrección C1 y contraste documental de los
criterios afectados registrados el 2026-11-11 en `checklists/data.md` y en los checklists de
SPEC-023, con fuentes de la base común. No se atribuye aprobación humana nueva ni se reutiliza
la primera cotización inmutable como requisito. Nuevo ANALYZE pendiente para verificar la corrección.

## Desglose de origen — antecedente con IDs y avance preservados

## Delta vigente — cotización diaria y ranking paginado (2026-11-10)

**Fuentes**: revisión vigente de [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contrato HTTP](contracts/quotes-http.md), [puerto de cotización](contracts/quote-source.md) y [quickstart.md](quickstart.md).
**Rama**: `docs/016-daily-quote-recalculation-spec`. **Operación**: TASKS, mediante `setup-tasks.sh --json`; plan y spec existentes reutilizados. IMPLEMENT AUTHORIZATION: NONE. No se ejecutan tareas técnicas ni fases posteriores al generar este desglose.

**Revisión de requisitos previa**: 29 criterios documentales contemplados en los checklists; los tres pendientes sobre visibilidad de contextos incompletos fueron retirados expresamente por el usuario y tachados, no aprobados. La decisión está en la spec. No se añaden requisitos de recuperación ni compensación; TD-024 continúa abierta. Esta evidencia no es aprobación de implementación, código, testing o cierre.

**Preservación**: T001–T054, sus marcadores y textos se conservan como antecedente; no se reabren ni se usan como evidencia de implementación del delta. El trabajo nuevo es T055–T080, todo pendiente. Para el delta prevalecen sus fases, rutas y dependencias siguientes sobre los ejemplos históricos. No existe gate READY ni autorización/cierre por US; las autorizaciones se consolidan por alcance conforme al workflow vigente.

**Ajuste mínimo autorizado tras ANALYZE I1**: T057 y T064 deben consultar/comparar la fecha efectiva vigente bajo el lock ya previsto y omitir si `effectiveDate >= quoteDate`, no solo si existe fila del mismo día. T056/T063/T066/T067 mantienen `notRepricedPlayers` como categoría de esa omisión, incluida fecha posterior, sin fallo ni escritura. Ampliar T062 con el escenario determinista D+1 publicado antes de que D procese al jugador, verificando precio e historial intactos mediante APIs públicas. T078 conserva coherencia con ese significado contractual. Esta precisión sustituye las menciones más restrictivas a «fila diaria existente» de las tareas siguientes; no crea IDs, recuperación ni bloqueos globales adicionales. Todas continúan pendientes; no se implementa código en esta corrección.

### Fase 1 — Preparación del esquema

**Evidencia vigente de IMPLEMENT**: autorización T055–T080, primera detención y reanudación por indicación humana registradas en [implementation-progress.md](implementation-progress.md). Las menciones anteriores a autorización NONE y a todas las tareas pendientes son antecedentes. Backend implementado y revisado estáticamente, build sin tests satisfactorio; las pruebas permanecen sin ejecutar ni compilar por el agente. T077 está parcialmente implementada y conserva pendiente el retiro coordinado de la ruta histórica con SPEC-022. No se declara IMPLEMENT integral completada ni gates aprobados. Informe: [daily-implementation-review.md](daily-implementation-review.md).

**Objetivo**: sustituir restricciones incompatibles sin editar migraciones integradas ni preservar datos descartables por exigencia no acordada.

- [X] T055 Generar una nueva migración desde `backend/` con `./gradlew createMigration -PmigrationDescription=daily_quote_recalculation` y editar solo el archivo generado bajo `backend/src/main/resources/db/migration/`: eliminar unicidad de `quote_executions.quote_date`, sustituir `uq_player_quote` por `(player_id,effective_date)`, añadir `player_quotes.execution_id` con FK y nulabilidad según origen, permitir `finished_at` nulo en contextos en curso y ordenar el índice de ejecuciones por fecha/inicio/ID. Conservar checks financieros y secuencias existentes; no editar V20261107030027 ni V20261107030028. Si los datos impiden constraints, detener y solicitar autorización de limpieza, sin backfill preservador ni borrado automático (modelo vigente). Inspeccionar previamente la tarea Gradle y sus dependencias para asegurar que no dispara tests; registrar la ruta real generada.

### Fase 2 — Fundamentos compartidos

**Dependencia**: T055. **Resultado verificable**: esquema y Model compatibles con varias corridas y una fila diaria por jugador.

- [X] T056 Adaptar `backend/src/main/java/footballmarket/features/quote/domain/models/QuoteExecution.java` y `backend/src/main/java/footballmarket/features/quote/domain/models/PlayerQuote.java`: referencia a ejecución para filas no iniciales, inicio inmutable, cierre explícito con fin nullable antes de completar y contadores disjuntos; `notRepricedPlayers` significa omisión diaria. Mantener invariantes de origen y precio, sin setters ni builders; contexto incompleto no equivale a éxito final (research R2).
- [X] T057 Adaptar `backend/src/main/java/footballmarket/features/quote/infraestructure/QuoteExecutionRepository.java` y `backend/src/main/java/footballmarket/features/quote/infraestructure/PlayerQuoteRepository.java`: consulta de corridas finalizadas por fecha con orden `startedAt DESC,id DESC`, comprobación diaria independiente del origen y selección vigente/histórica sin prevalencia entre orígenes del mismo día. Revisar consumidores de los selectores de ejecución que dejan de ser únicos antes de retirarlos; no alterar silenciosamente la estrategia activa de SPEC-018.
- [X] T058 Escribir/adaptar `backend/src/test/java/footballmarket/features/quote/infraestructure/PlayerQuoteMigrationTest.java` y `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java`: unicidad diaria real para todos los orígenes, varias corridas por fecha, FK de auditoría, orden total de resúmenes y contexto sin cierre excluido de resultados finalizados. PostgreSQL/Testcontainers y migraciones reales; no ejecutar pruebas.

### Fase 3 — US1: inicialización compatible con límite diario (P1)

**Objetivo**: INITIAL cuenta para el día sin cambiar ancla ni semántica de inicialización.
**Criterio independiente**: una inicialización de D seguida de recálculo en D no genera otra fila; repetición de inicialización mantiene idempotencia y fecha distinta conserva rechazo existente.

- [X] T059 [US1] Adaptar `backend/src/test/java/footballmarket/features/quote/application/QuoteMarketInitializationServiceTest.java`: inicialización idempotente y concurrencia con publicación diaria sin duplicados; preparar estado por fixtures permitidos y afirmar mediante APIs públicas. No ejecutar tests.
- [X] T060 [US1] Adaptar `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteMarketInitializationServiceImpl.java` y `backend/src/main/java/footballmarket/features/catalog/infraestructure/PlayerRepository.java`: bloqueo por fila de jugador compartido con publicación y comprobación de existencia antes de insertar INITIAL. Mantener precio 100, fecha de mercado, errores y ausencia de executionId en INITIAL; sin lock global de corridas ni nuevas capas (plan, punto 2).

### Fase 4 — US2: recálculo diario con múltiples corridas (P1)

**Objetivo/MVP del delta**: reutilizar el último score cada día y aceptar reintentos/corridas adicionales sin recotizar jugadores ya procesados.
**Criterio independiente**: con el mismo score en D/D+1 hay un ajuste por día; dos corridas en D, concurrentes o con estrategias distintas, no duplican fila ni ajuste; un fallo individual puede completarse en otra corrida. Fecha UTC fijada al inicio incluso al cruzar medianoche.

- [X] T061 [P] [US2] Adaptar `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteExecutionTest.java` y `backend/src/test/java/footballmarket/features/quote/domain/models/QuoteAdjustmentTest.java` para contexto/cierre y partición de contadores; conservar cobertura ya suficiente de fórmula, límites y redondeo sin duplicarla ni probar getters. No ejecutar tests.
- [X] T062 [P] [US2] Adaptar `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`: mismo score en dos días, score ausente/neutro, varias corridas del día, estrategias distintas, concurrencia, INITIAL diario, valuación llegada después y Clock que cruza medianoche; barreras deterministas sin sleeps. Act/Assert mediante interfaz pública, fixtures solo Arrange y observaciones por API pública. No ejecutar tests.
- [X] T063 [P] [US2] Adaptar `backend/src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java`: POST repetido aceptado, executionId, contadores y errores/precedencia conservados; lista diaria completa de finalizadas, orden total, vacío y JWT. Assertions de status/shape/contenido con MVC real y Service mock; no ejecutar tests.
- [X] T064 [US2] Adaptar `backend/src/main/java/footballmarket/features/quote/application/internal/QuotePlayerPublisher.java`: adquirir lock de jugador de T060, omitir fila diaria existente, eliminar condición de score posterior a referencia, aplicar último score sobre precio vigente o arrastrar sin score; fijar effectiveDate al día inicial, conservar sourceValuationDate y referencia a corrida. Mantener transacción individual; devolver omisión diaria separada del fallo. Conflictos del constraint diario se clasifican fuera de la transacción revertida por constraint/SQLSTATE, no capturando cualquier error como duplicado.
- [X] T065 [US2] Crear `backend/src/main/java/footballmarket/features/quote/application/internal/QuoteExecutionRecorder.java` como componente interno exclusivo de la fachada: confirmar contexto antes de las publicaciones y completar resumen después con transacciones independientes, sin FK hacia una corrida no confirmada. No es Service independiente reutilizable ni añade recuperación/compensación; cierre fallido conserva limitación TD-024 (research R2).
- [X] T066 [US2] Adaptar `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java`: derivar fecha UTC de un único startedAt, eliminar reserva/rechazo por ejecución diaria previa y transacción envolvente incompatible con registro independiente; integrar T064/T065 y contadores disjuntos, preservar entradas manual/automática y precedencia restante de validaciones. Contexto sin fin no se devuelve como resumen finalizado; conservar logs seguros y continuidad tras fallo individual.
- [X] T067 [US2] Adaptar `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteExecutionResponseDTO.java`, `backend/src/main/java/footballmarket/features/quote/presentation/QuoteExecutionMapper.java`, `backend/src/main/java/footballmarket/features/quote/presentation/QuoteController.java` y `backend/src/main/java/footballmarket/features/quote/presentation/QuoteExceptionHandler.java`: exponer executionId, actualizar significado de omitidos y documentación OpenAPI; eliminar respuesta contractual EXECUTION_ALREADY_EXISTS. Retirar `backend/src/main/java/footballmarket/features/quote/application/exceptions/ExecutionAlreadyExistsException.java` solo tras revisar y adaptar todos sus consumidores afectados, incluidos tests, sin eliminar manejadores ajenos.
- [X] T068 [US2] Adaptar `backend/src/test/java/footballmarket/features/quote/application/QuoteAutomaticRecalculationFailureTest.java` y `backend/src/test/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java`: ambas entradas comparten límite por jugador/día; omisiones no se cuentan como fallos; no interpretar contexto persistido como cierre correcto ni afirmar recuperación de TD-024. No cambiar cron ni responsabilidades del scheduler SPEC-023; no ejecutar tests.

### Fase 5 — US3: lecturas históricas con fecha diaria (P2)

**Objetivo**: preservar contratos de lectura y separar fecha de vigencia de antigüedad del score.
**Criterio independiente**: historial en D/D+1 conserva ambas filas con la misma fecha de score; vigente y consulta por fecha devuelven la fila diaria correcta; ausencias conservan respuesta existente.

- [X] T069 [P] [US3] Adaptar `backend/src/test/java/footballmarket/features/quote/application/QuoteQueryServiceTest.java` y casos de historial/vigente/fecha de `backend/src/test/java/footballmarket/features/quote/presentation/PlayerQuoteControllerTest.java`: vigencia diaria, score antiguo auditado, INITIAL/arrastrado y ausencias; quitar expectativas sustituidas de coexistencia de orígenes diarios. No ejecutar tests ni duplicar en esta tarea los casos de ranking de T074.
- [X] T070 [US3] Adaptar las lecturas individuales de `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteQueryServiceImpl.java` y `backend/src/main/java/footballmarket/features/quote/presentation/QuoteMapper.java` a T057: vigente por fecha efectiva máxima y ningún requisito de score nuevo; mapper solo transforma, sin reglas de negocio ni fecha de lectura inventada.
- [X] T071 [US3] Actualizar anotaciones OpenAPI de historial/vigente/fecha en `backend/src/main/java/footballmarket/features/quote/presentation/PlayerQuoteController.java` y `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteResponseDTO.java` para eliminar coexistencia diaria y distinguir effectiveDate de sourceValuationDate, conservando rutas, seguridad y errores existentes.

### Fase 6 — US5: integración de mercado preservada (P2)

**Objetivo**: no cambiar firmas de QuoteSource ni saldo/tenencias o precio congelado de publicaciones.
**Criterio independiente**: fuente individual y por lote reflejan última cotización diaria e instante original; recálculo no cambia recursos financieros ni precios ya publicados.

- [X] T072 [US5] Adaptar regresiones de `backend/src/test/java/footballmarket/features/quote/infraestructure/RepositoryQuoteSourceTest.java` y `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteMarketOperationPersistenceTest.java` para nueva unicidad y referencia de corrida: fuente individual/lote vigente, INITIAL sin estrategia, arrastrada válida, saldos/tenencias y publicaciones congeladas intactos. Conservar `backend/src/main/java/footballmarket/features/quote/infraestructure/RepositoryQuoteSource.java` y firmas de `backend/src/main/java/footballmarket/features/quote/application/QuoteSource.java` salvo ajuste de lectura indispensable por T057; no rediseñar mercado ni ejecutar tests.

### Fase 7 — US4: ranking por precio paginado en backend (P3)

**Objetivo**: filtrar/contar/paginar en backend conservando posiciones globales y podio independiente.
**Criterio independiente**: encontrar por filtro al jugador de una página posterior, empates por ID ascendente, posiciones no renumeradas, totales filtrados y Top 3 sin filtros mediante página de tamaño 3; vacíos y parámetros inválidos según contrato.

- [X] T073 [P] [US4] Crear `backend/src/test/java/footballmarket/features/quote/infraestructure/QuoteRankingPersistenceTest.java` para SQL real de vigente/elegibilidad, numeración global antes de filtros, empate de precio por ID, conteos, opciones globales y paginación; incluir búsqueda parcial con comodines literales, liga/posición combinadas y jugadores sin métricas. PostgreSQL/Testcontainers y datos locales deterministas; no ejecutar tests.
- [X] T074 [P] [US4] Adaptar casos de ranking en `backend/src/test/java/footballmarket/features/quote/presentation/PlayerQuoteControllerTest.java` y de página en `backend/src/test/java/footballmarket/features/quote/application/QuoteQueryServiceTest.java`: ruta nueva, contenido/identificación/nulos, totales/opciones, filtro fuera de primera página, límites/defaults, página fuera de rango, vacío y JWT; cubrir cada respuesta HTTP distinta sin repetir exhaustivamente reglas Repository. No ejecutar tests; coordinar edición de archivos con T069.
- [X] T075 [US4] Definir entrada/salida de dominio del ranking en `backend/src/main/java/footballmarket/features/quote/domain/records/QuoteRankingQuery.java` y `backend/src/main/java/footballmarket/features/quote/domain/records/QuoteRankingPage.java`, adaptando `backend/src/main/java/footballmarket/features/quote/domain/records/PlayerRankingEntry.java`: posición global transportada y datos identificatorios/opciones/metadatos del contrato. Sin DTO HTTP en Service/Repository ni campos calculados por Mapper; usar solo los modelos necesarios.
- [X] T076 [US4] Adaptar `backend/src/main/java/footballmarket/features/quote/infraestructure/PlayerQuoteRepository.java` con consultas/proyecciones de ranking: última fila por jugador elegible, row_number por precio DESC/ID ASC, filtros después de posición y antes de conteo/página, opciones globales por conjunto; joins de identificación sin N+1. No traer todo el ranking o catálogo a Java ni ordenar por fecha como desempate de precio. Mantener consultas individuales de T057.
- [ ] T077 [US4] Adaptar `backend/src/main/java/footballmarket/features/quote/application/QuoteQueryService.java`, `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteQueryServiceImpl.java`, `backend/src/main/java/footballmarket/features/quote/presentation/PlayerQuoteController.java`, `backend/src/main/java/footballmarket/features/quote/presentation/QuoteMapper.java`, `backend/src/main/java/footballmarket/features/quote/presentation/dtos/response/PlayerRankingResponseDTO.java` y crear `backend/src/main/java/footballmarket/features/quote/presentation/dtos/request/QuoteRankingQueryRequestDTO.java`: ruta /api/rankings/quote sin alterar rutas individuales, records HTTP, binding/validación, page=0/size=20/límite 100 y name/leagueId/position, lectura REPEATABLE_READ y OpenAPI completo. Respuesta según contrato vigente, mapper sin renumerar; Top 3 mediante misma ruta sin filtros. Retiro de ruta vieja se entrega conjuntamente con adaptación del consumidor bajo SPEC-022; no introducir alias de descarga completa ni implementar UI desde esta tarea.

### Fase 8 — Coherencia transversal y entrega técnica

- [X] T078 Documentar y contrastar el cambio implementado con `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`, `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quote-source.md`, `specs/016-ejecutar-recalculo-manual-de-cotizaciones/quickstart.md` y `docs/backend/architecture.md` §1.8: límite por jugador/día, no por corrida; coordinar el selector temporal de SPEC-018 y adopción de SPEC-022 sin cambiar sus requisitos ni implementar sus fases. Registrar dependencia concreta si sus artefactos siguen pendientes; no publicar contratos incompatibles con consumidores sin adaptar.
- [X] T079 Tras modificar código backend y revisar que las dependencias del comando no disparan tests, ejecutar únicamente `./gradlew build -x test` desde `backend/` conforme a `backend/AGENTS.md` y registrar resultado y limitaciones en `specs/016-ejecutar-recalculo-manual-de-cotizaciones/quickstart.md`; los tests y validación funcional quedan al usuario. No ejecutar build en esta fase documental ni interpretar build como testing aprobado.
- [X] T080 Revisar coherencia de evidencias, referencias y delta final en `specs/016-ejecutar-recalculo-manual-de-cotizaciones/tasks.md` y `docs/TECHNICAL_DEBT.md`: preservar TD-024 como pendiente salvo evidencia y encargo específico, registrar solo tareas implementadas/verificadas y pendientes humanos de tests; comprobar diff final, `git diff --check` y `git status --short`. No aprobar checklists/gates ni hacer commit/push sin encargo.

### Dependencias, paralelismo y estrategia del delta

Grafo: `T055 → T056 → T057 → T058`; luego `US1 (T059–T060) → US2 (T061–T068) → {US3 (T069–T071), US5 (T072), US4 (T073–T077)} → T078 → T079 → T080`. La escritura de pruebas puede adelantarse una vez definido el contrato; su ejecución siempre es humana. T064 depende del bloqueo de T060; T065 de T056; T066 integra T064/T065; T067 depende de T066. Para ranking, T075 precede T076 y ambos preceden T077. Los tests preceden los cambios de comportamiento de su bloque.

**Ejemplos de trabajo paralelo permitido**, solo archivos distintos y sin dependencias pendientes: en US2 T061/T062/T063; en US3 la parte de pruebas T069 y regresión US5 T072; en US4 T073/T074. T069 y T074 comparten archivos y no se solapan. T070/T071/T077 se secuencian cuando comparten servicio, mapper o controller. `[P]` no autoriza delegación ni implementación ni anula dependencias entre historias.

**MVP sugerido del delta**: fundamentos + US1/US2 para demostrar ejecución diaria segura. Es prioridad técnica, no autorización parcial ni cierre; US3/US5 preservan regresiones y US4 completa el alcance de ranking aprobado. La entrega global necesita todas las tareas aplicables y los pasos normativos posteriores. Scheduler diario, paginación por score y UI pertenecen respectivamente a SPEC-023/018/022 y requieren su propia preparación/autorización; dependencias conocidas no bloquean por sí mismas generar estas tareas.

**Resumen de generación de TASKS (histórico)**: 26 tareas nuevas: 4 compartidas, US1=2, US2=8, US3=3, US5=1, US4=5 y 3 transversales. Todas pendientes; 54 tareas históricas preservadas. Siguiente operación: ANALYZE por encargo separado, no ejecutada aquí.

**Resultado vigente de IMPLEMENT:** 25 tareas marcadas tras escritura/revisión estática y build sin tests; T077 parcial por retiro coordinado con SPEC-022. El backend paginado nuevo está implementado, no se retira el consumidor existente ni se modifica su UI. Pruebas, revisión y cierre pendientes de sus gates humanos; no se ejecutaron fases posteriores.

---

## Antecedente histórico — T001–T054 preservadas, no ejecutar reglas sustituidas

**Input**: Design documents from `/specs/016-ejecutar-recalculo-manual-de-cotizaciones/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/quotes-http.md,
contracts/quote-source.md, quickstart.md

**Tests**: MUST incluir tests backend necesarios según Constitution y normas aplicables; ejecución a cargo del usuario, nunca del agente. MUST NOT crear tests ni infraestructura frontend.

**Workflow local**: MUST seguir `docs/development/sdd-workflow.md`. Tasks nacen del SDD y viven en este archivo; no se exportan automáticamente a tarjetas. Generar Tasks no completa implementación ni habilita Ready: requiere Analyze OK. MUST conservar IDs/avance al reanudar.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend Java: `backend/src/main/java/footballmarket/...`
- Backend tests: `backend/src/test/java/footballmarket/...`
- Migraciones: `backend/src/main/resources/db/migration/`
- Configuración: `backend/src/main/resources/application.yml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Esquema de persistencia y configuración base del control de variabilidad.

- [X] T001 [P] Crear la migración `backend/src/main/resources/db/migration/V<timestamp>__create_player_quotes.sql` con `./gradlew createMigration -PmigrationDescription=create_player_quotes`: tablas `player_quotes` y `quote_executions`, secuencias, restricciones `uq_player_quote` sobre `(player_id, effective_date, origin)` y `uq_quote_executions_date`, CHECK de precio positivo, origen, fila inicial, fila arrastrada, fila recalculada y contadores, e índices `ix_player_quotes_player_date` e `ix_player_quotes_effective_price` (data-model.md §1, §2, §5)
- [X] T002 Crear `backend/src/main/java/footballmarket/config/QuoteProperties.java` con `@ConfigurationProperties("market.quote")`: factor de ajuste por defecto `0.10`, tope de variación por defecto `0.10` (fracción en el rango cerrado (0; 1]), piso mínimo por defecto `1` centésima y ancla inicial `100` centésimas, configurables sin declaración explícita (FR-012, FR-014, FR-015)
- [X] T003 [P] Declarar los valores por defecto `market.quote.adjustment-factor`, `market.quote.variation-cap` y `market.quote.min-price-floor` en `backend/src/main/resources/application.yml` y registrar `@EnableConfigurationProperties(QuoteProperties.class)` en la configuración de Spring que ya activa las propiedades existentes del área (FR-015)

**Checkpoint**: esquema y configuración base disponibles.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelo, repositorios, catálogo de estrategias, errores y componentes compartidos por todas las historias.

**⚠️ CRITICAL**: No puede iniciarse ninguna historia hasta completar esta fase.

- [X] T004 Crear el model `PlayerQuote` en `backend/src/main/java/footballmarket/models/PlayerQuote.java` con los campos de FR-023 (`player`, `effectiveDate`, `price`, `previousPrice`, `multiplier`, `score`, `strategyName`, `strategyVersion`, `sourceValuationDate`, `origin`, `calculatedAt`) y sus invariantes de origen (research.md §2)
- [X] T005 [P] Crear el enum `QuoteOrigin` en `backend/src/main/java/footballmarket/models/enums/QuoteOrigin.java` con `INITIAL`, `RECOMPUTED` y `CARRIED_FORWARD` (FR-021)
- [X] T006 [P] Reutilizar el enum existente `footballmarket.models.enums.ValuationStrategy` de SPEC-013 como catálogo cerrado versionado de estrategias de cotización, sin ampliarlo y sin crear un enum nuevo: sus constantes son exactamente `POSITIONAL_PERFORMANCE` y `GENERAL_ATTRIBUTES` (FR-018, FR-055, Constitution §V)
- [X] T007 Crear el model `QuoteExecution` en `backend/src/main/java/footballmarket/models/QuoteExecution.java` con `quoteDate`, estrategia y versión, factor, tope, piso, contadores (`pricedPlayers`, `recomputedRows`, `carriedForwardRows`, `notRepricedPlayers`, `failedPlayers`), `startedAt` y `finishedAt` (data-model.md §2)
- [X] T008 Crear `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java` con las consultas de última fila por jugador con desempate (`effectiveDate DESC, calculatedAt DESC`, prevalece la fila `RECOMPUTED`), historial ordenado por `effectiveDate DESC`, fila con `effectiveDate <= fecha` y existencia de fila `INITIAL` (mercado inicializado) (FR-026, FR-036 a FR-040, FR-044)
- [X] T009 [P] Crear `backend/src/main/java/footballmarket/repositories/QuoteExecutionRepository.java` con la persistencia de corridas y la consulta por fecha de cotización (FR-061)
- [X] T010 Crear las excepciones de dominio nuevas en `backend/src/main/java/footballmarket/services/exceptions/`: `MarketNotInitializedException`, `MarketAlreadyInitializedException`, `InvalidAdjustmentFactorException`, `InvalidVariationCapException`, `InvalidMinPriceFloorException`, `UnknownQuoteStrategyException`, `QuoteNotFoundException` y `ExecutionAlreadyExistsException` (research.md §15)
- [X] T011 Mapear en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` las ocho excepciones nuevas a sus códigos y estados HTTP (`MARKET_NOT_INITIALIZED` 409, `MARKET_ALREADY_INITIALIZED` 409, `INVALID_ADJUSTMENT_FACTOR` 400, `INVALID_VARIATION_CAP` 400, `INVALID_MIN_PRICE_FLOOR` 400, `UNKNOWN_QUOTE_STRATEGY` 400, `QUOTE_NOT_FOUND` 404, `EXECUTION_ALREADY_EXISTS` 409), conservando `QUOTE_UNAVAILABLE` intacto (FR-043, FR-045, research.md §15)
- [X] T012 [P] Crear `backend/src/main/java/footballmarket/services/internal/QuotePopulationSelector.java` para obtener la población cotizable desde los `player_id` distintos de `player_statistics` cuyo último registro tenga al menos una métrica informada, sin depender de la cobertura de las valuaciones (FR-001 a FR-003, research.md §6)
- [X] T013 [P] Crear `backend/src/main/java/footballmarket/services/internal/QuoteScoreResolver.java` para resolver el score persistido de SPEC-013 (`final_score` para `POSITIONAL_PERFORMANCE`, `final_grl` para `GENERAL_ATTRIBUTES`) usando `ValuationStrategy` como catálogo de estrategias, junto con su fecha de valuación y la estrategia y versión, sin recalcular valuaciones ni llamar a proveedores externos (FR-006, FR-010, T006)

**Checkpoint**: modelo, persistencia, errores y componentes compartidos listos; las historias pueden empezar.

---

## Phase 3: User Story 1 - Inicializar el mercado con el ancla de un crédito (Priority: P1) 🎯 MVP

**Goal**: Poder inicializar el mercado una única vez, dejando una fila `INITIAL` de 100 centésimas por jugador cotizable y rechazando operaciones de mercado sobre un mercado no inicializado.

**Independent Test**: Inicializar sobre un catálogo con métricas, comprobar una única fila inicial de 100 centésimas por jugador cotizable, idempotencia al repetir y rechazo con otra fecha; comprobar que compra y publicación de venta se rechazan con `MARKET_NOT_INITIALIZED` sin mover recursos.

### Tests para User Story 1 (backend según normas; sin tests frontend)

> Escribir tests backend necesarios; resultados y ejecución quedan a cargo del usuario. MUST NOT ejecutar tests desde el agente.

- [X] T014 [P] [US1] Escribir test de servicio de inicialización en `backend/src/test/java/footballmarket/services/QuoteMarketInitializationServiceTest.java`: escritura de una fila `INITIAL` por jugador cotizable, idempotencia en la misma fecha, rechazo con `MARKET_ALREADY_INITIALIZED` ante cualquier otra fecha de mercado, y no creación de filas para jugadores sin métricas ni para jugadores con un registro de métricas sin métricas informadas (FR-031 a FR-035)
- [X] T015 [P] [US1] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java` para `POST /api/quotes/initialize`: respuesta con `marketDate`, `initializedPlayers` y `alreadyInitialized`, y rechazo `409 MARKET_ALREADY_INITIALIZED` (contracts/quotes-http.md §1)
- [X] T016 [P] [US1] Escribir test de persistencia del esquema en `backend/src/test/java/footballmarket/repositories/PlayerQuoteMigrationTest.java` que verifique unicidad `(player_id, effective_date, origin)`, coexistencia de una fila `RECOMPUTED` y una `CARRIED_FORWARD` de la misma fecha de vigencia, CHECK de precio positivo y CHECK de invariantes de origen (data-model.md §1)

### Implementation for User Story 1

- [X] T017 [US1] Crear la interfaz `QuoteMarketInitializationService` en `backend/src/main/java/footballmarket/services/QuoteMarketInitializationService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java`: fecha de mercado derivada de las filas `INITIAL`, escritura de una fila inicial por jugador cotizable con `price = 100`, `multiplier = 1`, `previousPrice` nulo, sin estrategia ni score, idempotencia en la misma fecha de mercado y rechazo controlado `MARKET_ALREADY_INITIALIZED` ante cualquier otra fecha de mercado (FR-031 a FR-035, research.md §8)
- [X] T018 [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/InitializeQuotesRequestDTO.java` con el campo opcional `marketDate` y `backend/src/main/java/footballmarket/controllers/dtos/responses/InitializeMarketResponseDTO.java` con `marketDate`, `initializedPlayers` y `alreadyInitialized` (contracts/quotes-http.md §1)
- [X] T019 [US1] Crear `backend/src/main/java/footballmarket/controllers/mappers/QuoteMapper.java` con el mapeo de la respuesta de inicialización (sin lógica de negocio) (plan.md §Project Structure)
- [X] T020 [US1] Crear `backend/src/main/java/footballmarket/controllers/QuoteController.java` con `POST /api/quotes/initialize`, anotaciones OpenAPI y validación Jakarta del body, delegando en `QuoteMarketInitializationService` (FR-031, contracts/quotes-http.md §1)

**Checkpoint**: User Story 1 es funcional y verificable de forma independiente.

---

## Phase 4: User Story 2 - Recalcular y persistir la cotización con re-ajuste compuesto (Priority: P1)

**Goal**: Ejecutar el recálculo manual con estrategia y factor elegidos por el operador, aplicando `precioNuevo = precioVigente × (1 + k × (score − 0,5))` acotado por el tope y el piso, y persistiendo el historial trazable y el resumen de la corrida.

**Independent Test**: Catálogo con métricas y valuaciones de ambas estrategias; ejecutar el recálculo una vez por fecha de cotización y comprobar precio resultante, marca `CARRIED_FORWARD` en los no valuados, estrategia y versión registradas, ausencia de duplicados y rechazo de una segunda ejecución en la misma fecha.

### Tests para User Story 2 (backend según normas; sin tests frontend)

- [X] T021 [P] [US2] Escribir el test unitario de dominio `backend/src/test/java/footballmarket/models/QuoteAdjustmentTest.java` para la fórmula, redondeo half-up, acotamiento por tope y aplicación del piso mínimo; y el test de servicio de recálculo `backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java` para fila `RECOMPUTED` con score y estrategia, fila `CARRIED_FORWARD` sin score y con estrategia heredada, comparación contra la fecha de vigencia de referencia sin que una fila arrastrada la altere, coexistencia de una fila `RECOMPUTED` con una `CARRIED_FORWARD` de la misma fecha con prevalencia de la repreciada, jugador con valuación no posterior a su fecha de vigencia de referencia no repreciado, precedencia de rechazos FR-066 y rechazo `EXECUTION_ALREADY_EXISTS` en la segunda corrida del día y ante ejecuciones concurrentes de la misma fecha (FR-004 a FR-016, FR-026, FR-027, FR-060, FR-066)
- [X] T022 [P] [US2] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java` para `POST /api/quotes/recalculate` y `GET /api/quotes/executions?date=`: forma de la respuesta con contadores y `startedAt`/`finishedAt`, rechazo con `401` de toda solicitud sin JWT válido, y códigos `INVALID_ADJUSTMENT_FACTOR`, `INVALID_VARIATION_CAP`, `INVALID_MIN_PRICE_FLOOR`, `UNKNOWN_QUOTE_STRATEGY`, `MARKET_NOT_INITIALIZED` y `EXECUTION_ALREADY_EXISTS` (FR-059, contracts/quotes-http.md §2, §3)
- [X] T023 [P] [US2] Escribir test de persistencia en `backend/src/test/java/footballmarket/repositories/QuoteExecutionPersistenceTest.java` que verifique `uq_quote_executions_date`, que una ejecución rechazada no persiste fila y que dos ejecuciones concurrentes de la misma fecha producen una sola ejecución persistida y un `EXECUTION_ALREADY_EXISTS` controlado (FR-060, FR-066)

### Implementation for User Story 2

- [X] T024 [US2] Crear `backend/src/main/java/footballmarket/models/QuoteAdjustment.java` como Model inmutable responsable de calcular el multiplicador acotado y el precio resultante con redondeo half-up y piso mínimo; y crear `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java` para publicar la cotización de un jugador en su propia transacción `REQUIRES_NEW`, consumiendo ese resultado de dominio, aplicando la condición de escritura de FR-027 contra la fecha de vigencia de referencia —la última fila `INITIAL` o `RECOMPUTED` del jugador, que las filas arrastradas no alteran— y permitiendo la coexistencia de una fila `RECOMPUTED` con una `CARRIED_FORWARD` de la misma fecha, y registrando internamente el fallo individual con jugador, etapa y causa sin exponerlo en la respuesta (FR-005, FR-012, FR-013, FR-016, FR-026, FR-027, FR-062, research.md §13)
- [X] T025 [US2] Crear la interfaz `QuoteRecalculationService` en `backend/src/main/java/footballmarket/services/QuoteRecalculationService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java`: fecha de cotización UTC del día, orden de precedencia de rechazos FR-066, aplicación del multiplicador acotado, redondeo half-up en centésimas, piso mínimo, escritura condicional por posterioridad estricta respecto de la fecha de vigencia de referencia (última fila `INITIAL` o `RECOMPUTED`), contadores con la definición de FR-061 y persistencia del resumen de ejecución (FR-005, FR-011 a FR-013, FR-016, FR-027, FR-060, FR-061, FR-063, FR-066)
- [X] T026 [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/requests/RecalculateQuotesRequestDTO.java` con `strategy` obligatorio y `adjustmentFactor`, `variationCap` (fracción en el rango cerrado (0; 1]) y `minPriceFloor` opcionales, y `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteExecutionResponseDTO.java` con la forma del resumen de ejecución (FR-012, contracts/quotes-http.md §2)
- [X] T027 [US2] Crear `backend/src/main/java/footballmarket/controllers/mappers/QuoteExecutionMapper.java` con el mapeo del resumen de ejecución persistido a la respuesta HTTP (plan.md §Project Structure)
- [X] T028 [US2] Completar `backend/src/main/java/footballmarket/controllers/QuoteController.java` con `POST /api/quotes/recalculate` y `GET /api/quotes/executions`, anotaciones OpenAPI y validación de los parámetros por rango antes de ejecutar, garantizando que ambas rutas exigen JWT válido y rechazan con `401` toda solicitud no autorizada (FR-011, FR-059, FR-061, contracts/quotes-http.md §2, §3)

**Checkpoint**: User Stories 1 y 2 son funcionales y verificables de forma independiente.

---

## Phase 5: User Story 3 - Consultar el historial y la cotización vigente e histórica (Priority: P2)

**Goal**: Exponer el historial ordenado, la cotización vigente y la cotización a una fecha dada, con los datos de auditoría completos.

**Independent Test**: Catálogo cotizado en varias fechas: el historial devuelve filas de más reciente a más antigua, la vigente devuelve la última con su instante de cálculo de origen y la consulta por fecha devuelve la última con fecha de vigencia no superior a la solicitada.

### Tests para User Story 3 (backend según normas; sin tests frontend)

- [X] T029 [P] [US3] Escribir test de servicio de consultas en `backend/src/test/java/footballmarket/services/QuoteQueryServiceTest.java`: historial ordenado, cotización vigente, cotización a una fecha dada, ausencia `QUOTE_NOT_FOUND` y ausencia de cotización en la respuesta (FR-036 a FR-040)
- [X] T030 [P] [US3] Escribir test de contrato HTTP en `backend/src/test/java/footballmarket/controllers/PlayerQuoteControllerTest.java` para `GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=` y `GET /api/players/{id}/quotes/current`, incluidos `404 QUOTE_NOT_FOUND` y el `calculatedAt` de la cotización y no el de la lectura (contracts/quotes-http.md §4, §5, §6)

### Implementation for User Story 3

- [X] T031 [US3] Crear la interfaz `QuoteQueryService` en `backend/src/main/java/footballmarket/services/QuoteQueryService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` con las consultas de historial, vigente y por fecha, con el desempate de FR-037 a igualdad de fecha de vigencia, resueltas por acceso directo al repositorio sin recalcular precios (FR-030, FR-036 a FR-038, FR-065)
- [X] T032 [US3] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteResponseDTO.java` con los campos de auditoría de `contracts/quotes-http.md` §8 y `backend/src/main/java/footballmarket/controllers/dtos/responses/QuoteHistoryResponseDTO.java` con `playerId` y `quotes` (FR-041)
- [X] T033 [US3] Completar `backend/src/main/java/footballmarket/controllers/mappers/QuoteMapper.java` con el mapeo de la cotización y de su historial (plan.md §Project Structure)
- [X] T034 [US3] Crear `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java` con `GET /api/players/{id}/quotes`, `GET /api/players/{id}/quotes?date=` y `GET /api/players/{id}/quotes/current`, anotaciones OpenAPI y validación del identificador y de la fecha (FR-036 a FR-038, contracts/quotes-http.md §4, §5, §6)

**Checkpoint**: User Story 3 es funcional y verificable de forma independiente.

---

## Phase 6: User Story 4 - Obtener el ranking de jugadores por precio (Priority: P3)

**Goal**: Exponer el ranking de jugadores cotizables ordenados por su cotización vigente, de mayor a menor.

**Independent Test**: Catálogo cotizado con precios distintos: el ranking incluye una sola entrada por jugador cotizable, ordenada de mayor a menor, con el mismo precio que devuelve su cotización vigente.

### Tests para User Story 4 (backend según normas; sin tests frontend)

- [X] T035 [P] [US4] Escribir test de ranking en `backend/src/test/java/footballmarket/services/QuoteQueryServiceTest.java`: una sola entrada por jugador cotizable, orden descendente por precio, exclusión de jugadores sin métricas y presencia de los jugadores con precio `CARRIED_FORWARD` repetido (FR-039, SC-007)

### Implementation for User Story 4

- [X] T036 [US4] Añadir en `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java` la consulta de ranking: última fila por jugador con el desempate `effectiveDate DESC, calculatedAt DESC` (prevalece la fila `RECOMPUTED`) reordenada por precio descendente, sin recalcular precios (FR-039, FR-065)
- [X] T037 [US4] Añadir el método de ranking en `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` con su interfaz en `backend/src/main/java/footballmarket/services/QuoteQueryService.java` (FR-039)
- [X] T038 [US4] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/RankingResponseDTO.java` con `position`, `playerId`, `price`, `effectiveDate`, `origin` y `calculatedAt`, y exponer `GET /api/players/ranking` en `backend/src/main/java/footballmarket/controllers/PlayerQuoteController.java` con anotaciones OpenAPI (contracts/quotes-http.md §7)

**Checkpoint**: User Story 4 es funcional y verificable de forma independiente.

---

## Phase 7: User Story 5 - Operar el mercado con la cotización persistida (Priority: P2)

**Goal**: Que la compra y la publicación de venta consuman el repositorio canónico real de cotizaciones a través del puerto `QuoteSource`, sin fuente simulada, y que se rechacen de forma controlada cuando el mercado no esté inicializado.

**Independent Test**: Mercado inicializado y cotizado: la compra usa el precio vigente persistido y su instante de cálculo, la publicación congela ese precio, un recálculo posterior no altera saldos, tenencias ni transacciones, y una operación sobre un mercado no inicializado se rechaza con `MARKET_NOT_INITIALIZED`.

### Tests para User Story 5 (backend según normas; sin tests frontend)

- [X] T039 [P] [US5] Escribir test de la fuente real en `backend/src/test/java/footballmarket/integrations/RepositoryQuoteSourceTest.java`: devuelve la última fila del jugador, devuelve vacío si no hay cotización, conserva `calculatedAt` de la cotización y `strategyVersion` nulo en la fila inicial (contracts/quote-source.md §2)
- [X] T040 [P] [US5] Actualizar `backend/src/test/java/footballmarket/services/BuyOrderServiceTest.java` y `backend/src/test/java/footballmarket/services/TokenListingServiceTest.java` con el rechazo `MARKET_NOT_INITIALIZED` antes de cualquier movimiento de recursos y con la comprobación de que un recálculo no altera saldos, tenencias ni transacciones (FR-043, FR-048)
- [X] T041 [P] [US5] Eliminar `backend/src/test/java/footballmarket/integrations/SimulatedQuoteSourceTest.java` y `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java`, y adaptar los tests de arranque y la configuración de test que dependan de la fuente simulada, verificado con la búsqueda `SimulatedQuoteSource|market.quote.simulator` en `backend/src` (quickstart §7) (FR-042, SC-011)

### Implementation for User Story 5

- [X] T042 [US5] Crear `backend/src/main/java/footballmarket/integrations/RepositoryQuoteSource.java` como implementación real del puerto `QuoteSource`, resuelta contra `PlayerQuoteRepository` con la última fila por `effective_date DESC, calculated_at DESC` (prevalece la fila `RECOMPUTED`) y sin cambiar la firma del puerto (FR-042, FR-058, contracts/quote-source.md §1, §2)
- [X] T043 [US5] Eliminar `backend/src/main/java/footballmarket/integrations/SimulatedQuoteSource.java`, `backend/src/main/java/footballmarket/config/QuoteSimulationConfiguration.java` y `backend/src/main/java/footballmarket/integrations/exceptions/MissingQuoteSourceException.java`, y retirar de `backend/src/main/resources/application.yml` y de `backend/src/test/resources/application-test.yml` las propiedades `market.quote.simulator.enabled` y `market.quote.simulator.price` (FR-042, SC-011, contracts/quote-source.md §3)
- [X] T044 [US5] Añadir en `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java` y `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java` la comprobación de mercado inicializado antes de leer la cotización y antes de cualquier movimiento de recursos, con rechazo `MARKET_NOT_INITIALIZED` (FR-043, FR-045)
- [X] T045 [US5] Corregir en `specs/010-buy-tokens/contracts/quote-source.md` la descripción de consumidores del puerto, que declara como único a `BuyOrderServiceImpl` cuando también lo consume `TokenListingServiceImpl`, en el mismo cambio que introduce la fuente real (research.md §10, Constitution §IV)

**Checkpoint**: User Story 5 es funcional y verificable de forma independiente.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia de contratos, documentación y control entregable.

- [X] T046 [P] Revisar y completar las anotaciones OpenAPI de `QuoteController` y `PlayerQuoteController` y de sus DTO en `backend/src/main/java/footballmarket/controllers/`, de modo que el contrato HTTP documentado y el OpenAPI coincidan con `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` (Constitution §IV)
- [X] T047 [P] Actualizar `docs/backend/architecture.md` §1.3 con la dependencia efectiva `Integration → Repository` introducida por `RepositoryQuoteSource` (research.md §9)
- [X] T048 [P] Revisar `docs/backend/technologies.md` y confirmar que no hay cambios en el inventario tecnológico: la feature no incorpora dependencias ni cambia versiones (Constitution §IV)
- [X] T049 Revisar en `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java` y `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java` que la ejecución y las consultas usen los mecanismos de observabilidad vigentes (registros estructurados, correlación de solicitudes, métricas y health checks) sin reimplementarlos, y que el fallo individual quede registrado con jugador, etapa y causa (FR-062, FR-064)
- [X] T050 Ejecutar el control entregable permitido `./gradlew build -x test` desde `backend/` y reportar el resultado al usuario; los tests automatizados los ejecuta el usuario (Constitution, «Verificación y testing»)

---

## Phase 9: Remediación del code review (CR-01)

**Purpose**: Cerrar el desempate no determinista de la lectura de cotizaciones y registrar las decisiones del responsable sobre los hallazgos con decisión abierta.

- [X] T051 Cerrar el orden de lectura en `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java`: agregar el identificador de la fila como último criterio en `findLatestByPlayerId`, `findHistoryByPlayerId`, `findLatestByPlayerIdOnOrBefore` y `findReferenceDatesByPlayerId`, y extender la condición de exclusión de `findRanking` con `q2.id > q.id` ante empate de fecha de vigencia e instante de cálculo (FR-037, FR-039, CR-01, TD-022)
- [X] T052 Escribir en `backend/src/test/java/footballmarket/services/QuoteQueryServiceTest.java` los casos de empate: dos filas del mismo jugador con la misma fecha de vigencia y el mismo instante de cálculo, y convivencia de los tres orígenes; ambos deben afirmar una sola entrada por jugador en el ranking y la misma fila vigente en la consulta de cotización vigente (FR-039, CR-01)
- [X] T053 Actualizar `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` con la corrección aplicada y la verificación en la base de desarrollo, y `docs/TECHNICAL_DEBT.md` con el estado de TD-022 y con la decisión del responsable sobre CR-02, CR-05 y CR-07
- [X] T054 Ejecutar el control entregable permitido `./gradlew build -x test` desde `backend/` y reportar el resultado al usuario; los tests automatizados los ejecuta el usuario (Constitution, "Verificación y testing")

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories; consumes la inicialización solo como precondición de datos
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 5 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories; requiere filas `INITIAL` existentes solo como precondición de datos

### Within Each User Story

- Escribir tests backend necesarios; su ejecución y evidencia corresponden al usuario
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 2

```bash
# Escribir tests backend de US2; no ejecutarlos desde el agente:
Task: "Escribir test de servicio de recálculo en backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java"
Task: "Escribir test de contrato HTTP en backend/src/test/java/footballmarket/controllers/QuoteControllerTest.java"
Task: "Escribir test de persistencia en backend/src/test/java/footballmarket/repositories/QuoteExecutionPersistenceTest.java"

# Launch all foundations for User Story 2 together:
Task: "Crear backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java"
Task: "Crear backend/src/main/java/footballmarket/controllers/dtos/requests/RecalculateQuotesRequestDTO.java"
Task: "Crear backend/src/main/java/footballmarket/controllers/mappers/QuoteExecutionMapper.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP y validar**: gates permitidos y evidencia disponible; tests a cargo del usuario

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. US1 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
3. US2 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
4. US3 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
5. US4 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
6. US5 autorizada → IMPLEMENT efectivo → CODE REVIEW humano → TESTING humano → DONE
7. Each story adds value without breaking previous stories

READY y IMPLEMENT AUTHORIZATION no implican inicio. Gates humanos no son Tasks técnicas. Todas las US requeridas DONE y trabajo necesario completo → Feature TESTING/CONVERGE → FEATURE COMPLETION AUTHORIZATION humana → Feature DONE. Build/entrega no acreditan TESTING; tests aplicables pendientes impiden DONE.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
   - Developer D: User Story 4
   - Developer E: User Story 5
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Solicitar evidencia de tests al usuario cuando sea necesaria; no ejecutarlos
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

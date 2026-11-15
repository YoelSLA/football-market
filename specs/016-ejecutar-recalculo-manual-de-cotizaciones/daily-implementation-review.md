# Informe de implementación y revisión — delta diario SPEC-016

## Estado y alcance

Autorización humana: T055–T080, persistida en [implementation-progress.md](implementation-progress.md).
Se implementó el backend del delta; T077 permanece parcial exclusivamente por el retiro
coordinado de la ruta histórica con SPEC-022. No se declara IMPLEMENT integral terminada,
CODE REVIEW aprobado, TESTING aprobado ni cierre. El selector de estrategia de SPEC-018
se conserva por indicación humana de tratarlo en su bloque; riesgo pendiente TD-039.
El estado vigente es el diff local, sin commit ni publicación de esta implementación.

## Funcionamiento y decisiones

1. Una migración nueva permite varias corridas por fecha, identifica la corrida desde cada
   fila no inicial y garantiza una sola cotización por jugador/día, también para INITIAL.
   No se modificaron migraciones integradas ni se aplicó Flyway a una base existente.
2. La fachada valida estrategia → mercado → factor → tope/piso. Captura un solo instante
   y deriva su día UTC. Confirma el contexto mediante `QuoteExecutionRecorder.start`
   antes de abrir transacciones individuales, evitando FK hacia una corrida no confirmada.
3. `QuotePlayerPublisher.publish` reutiliza `PlayerRepository.lockIdentityById`, compartido
   con inicialización. Bajo ese lock lee vigente y omite si `effectiveDate >= quoteDate`.
   En otro caso aplica el último score aunque sea antiguo, o arrastra si falta. No recalcula
   valuaciones; conserva fórmula, topes, redondeo, piso y fecha original del score.
4. Un conflicto diario se clasifica después del rollback del jugador y solo con SQLSTATE
   23505, tabla `player_quotes` y constraint `uq_player_quote`. Otros fallos incrementan
   `failedPlayers`; se registran identidades seguras, no mensajes arbitrarios de excepciones.
5. El cierre independiente exige contadores disjuntos, fin no anterior al inicio y un único
   cierre. Un contexto abierto no se representa como éxito; la consulta diaria devuelve
   solo finalizadas en orden `startedAt DESC,id DESC`. Un fallo de cierre no se compensa
   ni recupera automáticamente: TD-024 continúa pendiente.
6. El ranking paginado selecciona en PostgreSQL la fila vigente elegible, numera por precio
   DESC/playerId ASC y después filtra, cuenta y pagina. Lee página, total y opciones bajo
   REPEATABLE_READ. No descarga el ranking completo para paginarlo en Java. Los joins
   entregan identificación sin consultas por entrada; el escudo se conserva nulo porque
   el catálogo actual no lo persiste. Los Mapper no numeran posiciones.
7. `/api/rankings/quote` admite defaults 0/20, tamaño máximo 100 y filtros conjuntos.
   El podio se obtiene con tamaño 3 sin filtros. La ruta existente `/api/players/ranking`
   conserva su shape hasta la adopción conjunta de SPEC-022: no se añadió otro alias de
   descarga completa. T077 no se marca terminada por esa conservación pendiente.

## Archivos de producción

Rutas relativas a `backend/src/main/java/footballmarket/features/quote/`:

| Archivo | Cambio y responsabilidad |
| --- | --- |
| `application/QuoteQueryService.java` | Contrato adicional de consulta paginada, sin DTO HTTP. |
| `application/impl/QuoteMarketInitializationServiceImpl.java` | Lock existente de jugador, comprobación previa a INITIAL e idempotencia concurrente. |
| `application/impl/QuoteRecalculationServiceImpl.java` | Fecha única UTC, varias corridas, transacciones separadas, partición de contadores y clasificación estructurada de conflictos. |
| `application/impl/QuoteQueryServiceImpl.java` | Página/total/opciones consistentes; conserva consultas individuales y asigna posiciones fuera del Mapper en la lectura histórica. |
| `application/internal/QuotePlayerPublisher.java` | Lock compartido, omisión por fecha vigente, aplicación diaria del score y referencia a corrida. |
| `application/internal/QuoteExecutionRecorder.java` — nuevo | Confirmación independiente de contexto y cierre, exclusivo de la fachada. |
| `application/exceptions/ExecutionAlreadyExistsException.java` — eliminado | Ya no existe el rechazo funcional por segunda corrida diaria; consumidores revisados y adaptados. |
| `domain/models/QuoteExecution.java` | Contexto abierto, fin nullable, partición de contadores y cierre único. |
| `domain/models/PlayerQuote.java` | Relación con ejecución; INITIAL es el único origen sin referencia. |
| `domain/records/PlayerRankingEntry.java` | Transporta posición e identificación sin cálculo en presentación. |
| `domain/records/QuoteRankingQuery.java` — nuevo | Parámetros normalizados y límites del dominio de consulta. |
| `domain/records/QuoteRankingPage.java` — nuevo | Página y opciones de dominio, metadatos e identificación. |
| `domain/records/QuoteRankingRow.java` — nuevo | Proyección SQL de conjunto, no DTO HTTP. |
| `infraestructure/PlayerQuoteRepository.java` | Unicidad diaria consultable, selectores sin prioridad de origen, SQL paginado y opciones globales; retiro del selector de referencia sin consumidores. |
| `infraestructure/QuoteExecutionRepository.java` | Lista diaria finalizada con orden total; conserva el selector consumido por SPEC-018. |
| `infraestructure/RepositoryQuoteSource.java` | Documentación de selección diaria; firmas y comportamiento individual/lote conservados. |
| `infraestructure/scheduling/AutomaticQuoteRecalculationScheduler.java` | Retiro de captura del rechazo eliminado, sin cambiar cron ni responsabilidades. |
| `presentation/QuoteController.java` | OpenAPI de corridas múltiples y consulta de finalizadas. |
| `presentation/PlayerQuoteController.java` | Ruta paginada validada; rutas individuales e histórica conservadas. |
| `presentation/QuoteExecutionMapper.java` | Expone la identidad persistida de ejecución. |
| `presentation/QuoteMapper.java` | Transformaciones de página y filtros; elimina numeración en presentación. |
| `presentation/QuoteExceptionHandler.java` | Retiro exclusivo del manejador de ejecución diaria duplicada. |
| `presentation/dtos/request/QuoteRankingQueryRequestDTO.java` — nuevo | Binding, defaults y validación HTTP de filtros/página. |
| `presentation/dtos/response/QuoteExecutionResponseDTO.java` | executionId y significado diario de omitidos. |
| `presentation/dtos/response/QuoteResponseDTO.java` | Distingue vigencia diaria de fecha de score auditado. |
| `presentation/dtos/response/RankingResponseDTO.java` | Transporta posición numérica proporcionada por el caso de uso. |
| `presentation/dtos/response/QuoteRankingPageResponseDTO.java` — nuevo | Shape HTTP paginado e identificación/opciones. |

Archivo nuevo de esquema:
`backend/src/main/resources/db/migration/V20261110235437__daily_quote_recalculation.sql`.
Mantiene secuencias y checks financieros, cambia unicidades/FK/nulabilidad e índice de
ejecuciones. Añade checks del orden temporal de cierre y partición de contadores finalizados.
Si los datos existentes incumplen constraints, la migración falla; no hay borrado ni backfill.

Se reutiliza `backend/src/main/java/footballmarket/features/catalog/infraestructure/PlayerRepository.java`
sin modificarlo: ya dispone del lock requerido. No se agregaron dependencias ni versiones.

## Archivos de pruebas y cobertura prevista

Rutas relativas a `backend/src/test/java/footballmarket/features/quote/`:

| Archivo | Comportamiento que afirma; relación con el delta |
| --- | --- |
| `domain/models/QuoteExecutionTest.java` | Contexto no exitoso, cierre único, partición sin mutación parcial; T056/T061. |
| `application/QuoteMarketInitializationServiceTest.java` | Idempotencia, dos inicializaciones y convivencia concurrente con publicación; US1/T059. |
| `application/QuoteRecalculationServiceTest.java` | Misma valuación en días sucesivos, score neutro/ausente, INITIAL, valuación tardía, varias estrategias/corridas, concurrencia, medianoche y precedencia; US2/aceptación 1–4. |
| `application/QuoteAutomaticRecalculationFailureTest.java` | Cierre interrumpido fuera de resultados finalizados, reintento del pendiente y omisiones no fallidas; TD-024 no se declara resuelta. |
| `application/QuoteQueryServiceTest.java` | Historial diario y fecha original del score, precio compuesto observable, corrida D demorada hasta después de D+1 con barreras, vigente/fecha y página filtrada global; US3/US4/aceptación 6. |
| `infraestructure/PlayerQuoteMigrationTest.java` | Constraints reales: unicidad entre orígenes diarios, FK requerida según origen y checks financieros. |
| `infraestructure/QuoteExecutionPersistenceTest.java` | Varias corridas concurrentes, parámetros rechazados, consulta Repository real de finalizadas y desempate por ID. |
| `infraestructure/QuoteRankingPersistenceTest.java` — nuevo | SQL real de vigente/elegibilidad, empate por ID, numeración antes del filtro, comodines literales, filtros conjuntos, total, opciones y página fuera de rango; aceptación 5. |
| `infraestructure/RepositoryQuoteSourceTest.java` | Individual/lote con precio arrastrado e instante persistido; INITIAL y ausencias conservados; US5. |
| `infraestructure/QuoteMarketOperationPersistenceTest.java` | Cotización usada por compra, FK de corrida, saldos/tenencias/transacciones y precio de publicación congelado intactos; US5. |
| `infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java` | Disparos repetidos vuelven a delegar, sin exclusión por fecha; scheduler sin nueva responsabilidad. |
| `presentation/QuoteControllerTest.java` | POST repetidos, identidades, contadores disjuntos, lista múltiple, vacío, errores y JWT. |
| `presentation/PlayerQuoteControllerTest.java` | Contratos individuales conservados; página nueva con filtros, defaults/límites, posiciones, identificación/nulos, vacíos/fuera de rango, JWT y error técnico seguro. |

Además, `backend/src/test/java/footballmarket/support/QuoteTestData.java` prepara FK
de corrida para filas no iniciales, exclusivamente en Arrange. Se conserva sin
duplicar la cobertura existente de fórmula/topes/redondeo en `QuoteAdjustmentTest`.

### Testing Report — pendiente de ejecución humana

- Las pruebas Service utilizan Spring/Repositories/PostgreSQL reales; los fixtures no
  ejecutan el caso de uso ni observan resultados en lugar de sus APIs públicas.
- Las pruebas Repository ejercitan PostgreSQL/Testcontainers y migraciones reales.
  Controller usa MVC y seguridad reales con `QuoteQueryService` o el servicio de
  recálculo mock; el test unitario del scheduler sustituye únicamente su frontera Service.
- Tiempo controlado con Clock; concurrencia con CountDownLatch y límites de espera,
  sin sleeps. Las assertions distinguen omisión, fallo y publicación, precio/fecha/score,
  posiciones globales, totales filtrados, estructura HTTP y referencias de auditoría.
- Puntos delicados: SQL nativo/proyecciones, binding immutable de parámetros, FK y
  aplicación sobre datos existentes, aislamiento entre tests y planificación de hilos.
  Ninguno de esos resultados runtime se infiere del build. El agente no compiló ni
  ejecutó tests; no hay resultados humanos aportados para este delta.
- No se crearon ni ejecutaron tests frontend, ni se añadió infraestructura de testing.

Orden recomendado: Model → migraciones/Repository → Service y concurrencia →
Controller → regresiones QuoteSource/mercado/ranking por score.

Comando para el usuario desde `backend/`, con JDK 21 y Docker disponibles:

```powershell
.\gradlew.bat test --tests 'footballmarket.features.quote.*' --tests 'footballmarket.features.ranking.*' --tests 'footballmarket.features.market.*' --tests 'footballmarket.common.persistence.FlywaySchemaTest*'
```

El usuario ejecuta y confirma los resultados; si falla, compartir la salida del fallo.
La suite de ranking por score es regresión del consumidor existente, no acredita su
adaptación multicorrida ni la resolución de TD-039.

## Documentación modificada y evidencia

- `docs/backend/architecture.md`: límite diario por jugador y responsabilidades del recorder/scheduler.
- `docs/TECHNICAL_DEBT.md`: TD-024 sigue pendiente; TD-039 registra selector diferido.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`:
  estado implementado local y retiro coordinado todavía pendiente.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quote-source.md`:
  firmas conservadas y estado de sus regresiones.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/quickstart.md`: build y límites de verificación.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/tasks.md`: progreso sin marcar T077 terminada.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/implementation-progress.md`:
  autorización, detención histórica y reanudación explícita.
- Este informe: inventario, decisiones, cobertura y checklist humano sin aprobación.

Los cambios documentales previos en spec/plan/research/data-model/checklists y el
informe ANALYZE anterior se preservaron; no se atribuyen a esta implementación.

Verificación ejecutada: generador de migración y `build -x test`, sin tareas Test.
El último build informado en `quickstart.md` es satisfactorio (4 s, 12 tareas accionables:
4 ejecutadas y 8 actualizadas). `git diff --check` no informó errores; el estado Git
se revisó preservando el trabajo previo. No se arrancó la aplicación,
aplicó la migración, ejecutó tests, creó commit/push ni se ejecutaron fases posteriores.
El seguimiento remoto permanece sin verificar/sincronizar por esta ejecución.

## Orden y checklist de revisión humana

### Corrección del primer fallo aportado por el usuario

El usuario informó dos tests fallidos y aportó la salida de
`PlayerQuoteControllerTest.PagedRanking.hidesTechnicalFailure`: el mock lanzaba
`IllegalStateException`, que no está entre las excepciones traducidas por el manejador
global. MockMvc propagaba el fallo en lugar de recibir la respuesta esperada.
Se corrigió el Arrange para simular `DataAccessResourceFailureException`, coherente
con el fallo técnico de persistencia del ranking y con las pruebas existentes del
ranking por score. Se mantienen las assertions de 500, INTERNAL_ERROR y ausencia
del detalle privado. No se añadió una captura genérica al código productivo ni se
relajaron assertions. La reejecución del test corregido queda pendiente del usuario;
en ese momento el segundo fallo todavía no se había detallado. No se aprueba TESTING.
El build permitido posterior a esta corrección terminó satisfactoriamente, sin tests.

### Corrección del segundo fallo informado por el usuario

`backend/src/test/java/footballmarket/common/persistence/FlywaySchemaTest.java`,
`migratesEmptyPostgres`, todavía esperaba 28 migraciones y la versión
`20261107030028`. Se actualizaron ambos conteos a 29, el nombre descriptivo y
`LAST_VERSION` a `20261110235437`, correspondiente a la migración nueva del delta.
La prueba incremental también utiliza esa constante y ahora alcanza la nueva versión.
Se conservaron las comprobaciones de validación Flyway e historial SQL; no se
reemplazaron los valores esperados por el resultado observado de la propia prueba.
Se explicitaron los tipos locales conforme a las normas de tests, sin cambiar su lógica.
La reejecución de ambos tests corregidos permanece a cargo del usuario; no hay
confirmación posterior de éxito ni aprobación de TESTING.
El build sin tests posterior a esta segunda corrección fue satisfactorio (4 s).

### Checklist humano pendiente

Inspeccionar primero migración/Model, luego `QuoteExecutionRecorder` y fachada,
`QuotePlayerPublisher.publish`, inicialización y selectores; después SQL paginado,
Service/Mapper/Controller/DTO y todas las pruebas listadas. Revisar específicamente
la clasificación de conflictos tras rollback, que D no publique tras D+1 y los límites
de T077/TD-024/TD-039. El informe no sustituye revisar los tests ni ejecutarlos.

- [ ] Revisé todos los archivos creados/modificados y la excepción eliminada, incluidos tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones de transacciones, locks, auditoría y ranking SQL.
- [ ] No detecté comportamiento fuera de la spec y entiendo los pendientes de coordinación.
- [ ] Apruebo avanzar a TESTING para el alcance evaluado.

Checklist humano pendiente; el agente no lo marca ni solicita cierre automático.
La fase IMPLEMENT integral conserva el pendiente indicado de T077.

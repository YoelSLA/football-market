# Implementation Plan: Scraping de estadísticas de rendimiento de jugadores

## Diseño objetivo — retiro del endpoint coverage (2026-11-01)

**Implementación autorizada T118–T124**: retiro del GET, simplificación interna y tests adaptados según este diseño; se conserva como antecedente la preparación siguiente. El cierre no requiere editar `ExtractionRunManager` porque su consumo coincide con el record reducido. No hay migraciones ni cambios de contrato POST. Revisión, testing y cierre pendientes; evidencia técnica en el reporte de entrega del retiro.

Este cambio es posterior a T111–T117 y no los reescribe. Rige sobre las referencias anteriores a la consulta HTTP de cobertura; los inventarios del refactor se conservan como evidencia de su alcance. Preparación documental autorizada por el usuario, sin código ni tests modificados por este retiro. Specify y Clarify acotados registrados en `spec.md`; setup de Plan conserva artefactos existentes y resuelve SPEC-012. Rama Git: `refactor/012-player-statistics-scraping`; no hay hooks de extensión configurados.

**Evidencia de resolución**: el script oficial con `--reuse --type refactor --short-name 012-player-statistics-scraping` devolvió `SPEC_ACTION=reused`, `FEATURE_NUM=012` y `BRANCH_STATUS=already-current`, con `SPEC_FILE` en esta carpeta. La invocación exterior terminó con código 1 por interpretar el fragmento descriptivo posterior a un punto y coma como comando (`solo` no encontrado); no se repitió la materialización ni se atribuye éxito al comando completo. La lectura posterior de `.specify/feature.json`, el resolver de Clarify y `setup-plan.sh --json` confirmó SPEC-012 y la conservación del plan. Ningún cambio de rama, descarte de archivos ni nueva spec.

### Phase 0: decisión e investigación

La lectura de Controller, Services, `ExtractionRunManager.finish`, repositorios y tests confirma que el endpoint tiene piezas exclusivas y un cálculo compartido con los POST. La alternativa de borrar todas las clases con nombre Coverage rompería el cierre de runs; la alternativa de conservar su diagnóstico completo mantendría trabajo sin consumidores. La decisión y alternativas quedan en `research.md`. No se necesitan tecnologías, proveedor externo ni decisiones funcionales adicionales.

### Phase 1: diseño mínimo

Rutas relativas a `backend/src/main/java/footballmarket/features/statistics/`:

| Componente actual | Cambio previsto |
| --- | --- |
| `presentation/PlayerStatisticsController.java` | Retirar el método GET, inyección del Service de cobertura, validación de `source` y imports exclusivos. Conservar ambos POST y su documentación OpenAPI. |
| `presentation/PlayerStatisticsCoverageMapper.java` | Eliminar: exclusivo de respuesta GET. |
| `presentation/dtos/response/PlayerStatisticsCoverageResponseDTO.java` | Eliminar: contrato retirado, sin trasladar sus campos a los POST. |
| `application/PlayerStatisticsCoverageService.java` y `application/shared/impl/PlayerStatisticsCoverageServiceImpl.java` | Eliminar; migrar primero sus consumidores de tests a fronteras válidas. |
| `application/shared/internal/PlayerStatisticsCoverageQuery.java` | Conservar como cálculo interno reducido de población y pendientes. Retirar diagnóstico de métricas ausentes y búsqueda del último run, que los POST no usan; no cambiar nombre por una preferencia estética. |
| `domain/records/PlayerStatisticsCoverage.java` | Reducir al resultado interno requerido por el cierre: `eligiblePlayers`, `excludedWithoutReference`, `pendingPlayers` y `coverageComplete`. No es nuevo DTO público ni entidad persistida. |
| `application/shared/internal/ExtractionRunManager.java` | Adaptar únicamente al resultado interno reducido, manteniendo estados, contadores, atomicidad, recuperación y resumen. |
| `infraestructure/*Repository.java` | Revisar todos los consumidores del repositorio. Retirar solo métodos que queden exclusivamente ligados al GET y sin uso; preservar consultas de valuaciones, cotizaciones, catálogo, auditoría y sincronización. |

El cierre conserva las fórmulas actuales de población, pendientes y `coverageComplete`; no se usa el contador de intentos fallidos del GET como sustituto de los fallidos del run. El retiro no modifica selección del snapshot ni comportamiento frente a nuevos elegibles, no-data o reanudación. No hay migraciones ni cambios de entidades/tablas, Clock, propiedades, navegador o integraciones.

### Tests afectados y fronteras

Raíz `backend/src/test/java/footballmarket/features/statistics/`:

- `presentation/PlayerStatisticsControllerTest.java`: retirar fixtures/mocks/casos exitosos de coverage; conservar casos completos de POST y comprobar 404 del GET con JWT válido, sin invocar sincronización.
- `presentation/PlayerStatisticsContractFoundationTest.java` y `security/PlayerStatisticsSecurityIntegrationTest.java`: adaptar wiring si depende del Service eliminado; conservar seguridad de ambos POST. No fijar una respuesta alternativa para visitantes de una ruta inexistente.
- `application/shared/PlayerStatisticsSynchronizationServiceTest.java`: sustituir la lectura del Service retirado por los resúmenes de los POST. No perder pendientes, reanudación, cero trabajo ni fallos.
- `application/scraping/PlayerStatisticsExtractionServiceTest.java` y `application/json/JsonStatisticsUpdateServiceTest.java`: retirar dependencia del contrato de cobertura eliminado. Verificar los resultados existentes por las APIs productivas de extracción/actualización, cierre del run y consulta individual disponibles, conservando Arrange y fronteras de categoría. No añadir endpoint ni API de Service exclusiva para tests; si falta observación pública, cubrir el detalle persistido en categoría Repository ya existente, sin borrar la garantía.
- `infraestructure/PlayerStatisticsCoverageTest.java`: conservar casos de elegibilidad, pendientes e invariantes que sigan vigentes; adaptar el resultado interno reducido y retirar solo diagnóstico exclusivo. `PlayerStatisticsPersistenceTest.java` y consultas relacionadas: actualizar solo consumidores realmente afectados.
- `e2e/PlayerStatisticsSyncJourneyE2ETest.java` y `e2e/PlayerStatisticsCrossChannelJourneyE2ETest.java`: retirar helper/GET de coverage y reemplazar las comprobaciones por resúmenes POST y lectura individual HTTP existente cuando corresponda. Conservar independencia entre canales y fechas, reanudación y métricas, sin usar repositorios para Assert/Verify ni nuevas sincronizaciones solo para observar un dato histórico.
- Buscar dependencias adicionales en `backend/src/test/java/` antes de eliminar piezas; preservar los tests del refactor ya realizado y las demás features.

### Constitution Check pre/post diseño

Alcance mínimo: retiro aprobado por usuario, sin ampliar requisitos. Contrato incompatible explícito: ruta retirada, 404 autenticado y consumidores adaptados; no se agrega deprecación, alias o redirección. Seguridad y secretos sin cambios. Se preservan reglas de dominio, esquema, datos y fuentes académicas: el enunciado exige scraping, no este GET. Las pruebas se ajustan sin bajar sus garantías ni cruzar sus fronteras; ejecución humana. No se ejecutan tests ni build en preparación documental. No hay excepciones arquitectónicas nuevas. La evaluación documental no es Analyze ni aprobación humana de implementación.

`data-model.md` documenta el resultado interno reducido y la ausencia de cambios persistidos. `contracts/http.md` retira el GET conservando el antecedente identificado. `contracts/whoscored.md` no requiere cambios: no se altera comunicación externa. `quickstart.md` deja de usar el GET como instrucción vigente. Siguiente paso: checklist mínimo del retiro evaluado, Tasks y Analyze; no se reutiliza la aceptación del checklist del movimiento para este cambio funcional.

**Branch**: `feat/012-player-statistics-scraping` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-player-statistics-scraping/spec.md`

**Estado del refactor organizativo — 2026-11-01**: PLAN documental completado en `refactor/012-player-statistics-scraping`, sin implementación. Rige la sección «PLAN acotado» y su estructura objetivo. El Planning Status siguiente se conserva como antecedente del alcance del 2026-10-27, no describe la rama activa de esta ejecución.

**Actualización posterior de implementación — 2026-11-01**: T111–T117 implementadas bajo autorización explícita registrada en `tasks.md`; la estructura objetivo se materializó sin cambios de cuerpos funcionales. Build sin tests satisfactorio; CODE REVIEW, TESTING y cierre pendientes. El estado PLAN anterior describe la preparación, no invalida esta actualización. Evidencia y archivos en [code-review-channel-structure.md](code-review-channel-structure.md).

**Planning Status**: Diseño actualizado el 2026-10-27 para la sesión de Clarify de esa fecha; pendiente de Analyze. Es comportamiento objetivo, no implementación acreditada. Se conserva cobertura 45/57/42, quince combinaciones JSON, fila por campaña/canal y navegador por worker. La rama de la spec es `feat/012-player-statistics-scraping`; la rama activa `fix/012-scraping-parallel-workers` contiene un cambio correctivo/refactor relacionado con esta misma SPEC-012, no una feature independiente. Las autorizaciones históricas están en `sdd-gates.md`, sin autorización para implementar este nuevo alcance. Seguimiento remoto no verificado.

## Summary

### PLAN acotado — reorganización por canal (2026-11-01)

**Rama de trabajo**: `refactor/012-player-statistics-scraping`. Esta sección y la estructura objetivo de abajo gobiernan únicamente el refactor organizativo solicitado; el resto del documento conserva las decisiones funcionales y sus antecedentes. No se vuelve a planificar ni a aprobar el alcance histórico.

**Autorización de preparación**: el usuario invocó `/speckit.plan` para actualizar exclusivamente el diseño de SPEC-012, manteniendo una feature, comportamiento y contratos, sin modificar código. Esta ejecución no genera Tasks ni acredita autorización de implementación, CODE REVIEW, TESTING o cierre.

**Objetivo**: localizar los componentes exclusivos de scraping y JSON junto a su canal, y distinguir los componentes usados por ambos. El cambio futuro se limita a paquetes, rutas, imports y referencias afectadas, incluidos tests. No se extraen nuevos ejecutores, no se dividen Services, no se renombran clases ni métodos y no se unifican políticas de los canales.

**Estado de esta preparación**: Phase 0 y Phase 1 documentales completadas para este alcance; checklists por evaluar, Tasks y Analyze posteriores pendientes. Seguimiento remoto no consultado ni actualizado. Las aprobaciones históricas no se transfieren al refactor.

**Setup verificado**: `setup-plan.sh --json`, con `SPECIFY_FEATURE_DIRECTORY=specs/012-player-statistics-scraping`, conservó el plan existente y resolvió esta carpeta. Persistió ese contexto en `.specify/feature.json`, que antes apuntaba a SPEC-008; no se modificó ningún artefacto de SPEC-008. El campo `BRANCH` del script devolvió el nombre de spec sin prefijo; la rama Git real se verifica por separado y es la indicada arriba. No había `.specify/extensions.yml`, por lo que no existen hooks previos/posteriores que despachar en esta ejecución.

Dos vías independientes y síncronas, cada una sobre todos sus pendientes en un POST: scraping con fecha opcional en body y JSON sin parámetros ni body. Sin lotes, `maxPlayers` ni deadline total. Scraping intenta cada jugador una vez, sin retries de navegación, sección o categoría; JSON conserva su retry técnico existente. Playwright headed mantiene el recorrido de 45 métricas y JSON sus quince combinaciones para 57 métricas. Se conserva catálogo cerrado de 60 y persistencia por jugador/fecha/canal. Fallidos y no-data siguen pendientes; no se repiten completados ni se reintenta automáticamente el POST.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC/`RestClient`, Spring Data JPA, Spring Security/OAuth2 Resource Server, Flyway, pgJDBC y Microsoft Playwright para Java 1.63.0. Playwright es una dependencia nueva aprobada para esta feature; su incorporación actualizará `docs/backend/technologies.md` y la configuración efectiva.

**Storage**: PostgreSQL gestionado por Flyway. Catálogo inmutable de 60 métricas en código y persistencia aplanada: una fila por jugador, fecha de campaña y canal con las 60 métricas en columnas, más las tablas de ejecuciones, items e incidencias. Una campaña nueva no sobrescribe las filas anteriores; dentro de la misma campaña la fila se actualiza.

**Decimal precision**: las 60 columnas de métricas son PostgreSQL `NUMERIC` nullable sin precisión ni escala fijas, con mapping JPA `numeric`. `BigDecimal` desde el token original, sin float/double ni redondeo de valores directos. El rating `6.9033333333333333` conserva su valor exacto. Solo la división por `apps` usa seis decimales HALF_UP. No hay tolerancias, diferencias ni precisión observada persistidas por métrica.

**Testing**: JUnit Jupiter, Mockito, MockMvc y Testcontainers con PostgreSQL 18. Los tests se implementarán conforme a `docs/backend/testing.md`; su ejecución corresponde al usuario y nunca utilizará Internet ni WhoScored real.

**Target Platform**: Backend Spring Boot. La vía JSON funciona en el entorno HTTP habitual; la vía de scraping requiere una sesión gráfica capaz de mostrar Chromium. La aplicación arranca aunque Playwright o el entorno gráfico no estén disponibles.

**Project Type**: Servicio web backend en el monolito existente.

**Performance Goals**: SC-012 conserva la estimación histórica informativa de unas 10 horas, sin criterio de aceptación ni deadline. Defaults actuales: cuatro workers por canal e intervalo de 1500 ms. La medición real corresponde al usuario con configuración declarada; no hay garantía de duración ni de cobertura completa si hay fallos/no-data. Una petición puede durar horas: el cliente y cualquier proxy deben admitir esa espera; un timeout de transporte no habilita repetir automáticamente el POST ni implica rollback del trabajo confirmado.

**Constraints**: Scraping headed; cuatro workers por canal y 1500 ms de intervalo inicial. Sin retries de scraping; JSON mantiene su política existente documentada en `contracts/whoscored.md`. Playwright y Browser por worker, creados y cerrados en su hilo, contextos independientes por sección y sin monitor global. Timeouts por operación, no por campaña. Sin transacción durante acceso externo, ejecución al arrancar, propagación de secretos ni cambio de SPEC-008.

**Recorrido completo de scraping**: resolver fecha una vez desde el request o Clock UTC, abrir run y obtener `allPendingPlayerIds` una vez, en orden ascendente de ID local. El pool consume cada ID del snapshot como máximo una vez; no vuelve a consultar pendientes para alimentar ese POST. Sustituir el corte de `ExtractionBatchExecutor` por ese recorrido, retirar media de 20 muestras, capacidad estimada, deadline, `scrapingBatchTimeoutMs` y `maxPlayers` de los consumidores afectados. Los nombres internos pueden conservarse si no justifican un renombrado transversal; no conservar algoritmos muertos. Sin pendientes, cerrar sin navegador ni llamadas. Nuevos elegibles durante el run quedan para otro POST y cuentan como pendientes en cobertura al cerrar.

**Continuidad y recuperación de scraping**: un fallo atribuible a un jugador —incluidos timeout, transporte, 403/404 o respuesta no interpretable— se registra para ese jugador y el recorrido continúa con los restantes, sin reintentar al jugador. Se clasifica como fallo global solo cuando el proveedor o el entorno impide iniciar/continuar operaciones para los jugadores restantes, o cuando no se puede mantener/cerrar de forma segura la auditoría del run. Respetar `Retry-After`: si el pacing permite continuar después de la espera, reanudar el recorrido sin reintentar al jugador afectado; si el aplazamiento impide continuar con los restantes, cerrar el run como fallo global, con pendientes declarados y sin reiniciar automáticamente el POST. Una interrupción explícita o pérdida irrecuperable del entorno de navegador también puede cerrar el run. Nunca recuperar un run solo por su antigüedad: sin deadline podría seguir vivo. En la instancia única del diseño actual, proteger la entrada con exclusión por canal, mantenerla hasta terminar workers y limpieza, y recuperar un run huérfano solo cuando no pertenece a una ejecución viva de esa instancia (incluido reinicio). Mantener el índice único `RUNNING` como respaldo; no introducir lease, heartbeat ni soporte multiinstancia. JSON no cambia su política de recuperación en este alcance.

**No-data y reconfirmación**: scraping sin contexto de liga o sin ningún valor numérico presente termina `NO_DATA`, no `COMPLETED`. Conservar auditoría y, si se guarda fila nula con contexto resuelto, permitir su reconfirmación posterior solo si no existe item `COMPLETED` de esa campaña; una fila con éxito previo permanece protegida. Una ausencia opcional aislada no invalida valores válidos. No cambiar la semántica JSON.

**Recorrido JSON diario**: la fachada expone una entrada sin argumentos funcionales para JSON y obtiene la fecha mediante Clock UTC del backend una sola vez al inicio. Fija la población de referencias confirmadas elegibles al abrir el run y selecciona todos sus pendientes JSON, por ID local ascendente, sin tamaño máximo ni deadline de lote de scraping. Reutiliza selector, pool, pacer, auditoría y confirmación por jugador; no usa el corte de `ExtractionBatchExecutor`. Cada seleccionado se procesa una vez por run más el retry técnico permitido; no se vuelve a seleccionar un fallido dentro del mismo run. Un fallo individual no aborta los demás; bloqueo global, rate limit prolongado o interrupción cierran con el resultado alcanzado y pendientes explícitos, respetando el aplazamiento del proveedor sin insistencia. No hay transacción abierta durante consultas ni nuevo proceso asíncrono. Los timeouts por consulta permanecen activos. Otra llamada el mismo día omite los ya `COMPLETED`; si no hay pendientes, devuelve resumen `COMPLETED`, selección y llamadas cero, sin consultar WhoScored. Cruzar medianoche no cambia la fecha inicial; una petición nueva otro día inicia otra campaña. Los nuevos elegibles después del snapshot se contabilizan como pendientes en cobertura y se seleccionan en una petición posterior, nunca se declara cobertura completa mientras falten elegibles actuales.

**Ritmo y reintentos**: se extiende `WhoScoredRequestPacer`, que ya gobierna ritmo, timeouts y `Retry-After` para el canal de identidad, con los valores por canal del catálogo de estadísticas. No se crea un segundo pacer ni un segundo cliente HTTP: las integraciones de estadísticas reutilizan `WhoScoredClientConfig` y `WhoScoredProperties`, y solo amplían el contrato de dominio que cada una encapsula.

El permiso de scraping regula inicios de navegación de sección por worker sin retry, no cada recurso. El permiso JSON regula cada consulta y sus retries existentes. Se conserva identidad sin serializar los workers estadísticos. El cierre libera primero contextos, después Browser y por último Playwright, en su hilo y una sola vez; el pool no cierra recursos desde otro hilo mientras una operación sigue activa. Un fallo de limpieza no debe ocultar el fallo principal ni perder conteos confirmados. La espera acotada de terminación del pool es de limpieza, no un deadline del recorrido.

## Investigación acotada de fallos observados

### Estado final y limpieza autorizada — 2026-10-30

Decisión posterior: usuario solicita eliminar la suite DOM opt-in. Se elimina su
clase y comandos/variable, conservando tests de protocolo y Model. La implementación
productiva no cambia; la cobertura real de JavaScript deja de estar automatizada.
TD-034 registra la brecha, sin declarar equivalencia entre campañas y regresiones.
Las referencias posteriores a esa suite describen antecedentes, no alcance vigente.

Se conservan correcciones de apertura, modo nativo, ausencia N/A y normalización
por canal; se retiran herramienta diagnóstica, tarea Gradle, Logback dedicado,
hooks de observación, listeners de red/JS y comparación de HTML usada solo para logs.
El observer de render permanece funcional. Se conserva logging operativo/general
con rotación y tests aislados bajo BackendFileLoggingTest. No DB, proveedor ni cambios
JSON. Las secciones diagnósticas siguientes son antecedentes retirados, no diseño vigente;
la evidencia histórica y logs del usuario no se eliminan. Validación posterior a limpieza
pendiente, sin cierre global ni merge implícito.

### Corrección autorizada de apertura inicial — 2026-10-29

El diagnóstico secuencial 7 reprodujo await-detail-filter con panel vacío: clic a
81 ms, panel visible antes del clic, ocultación a 1132 ms, sin petición shots.
Antes del único clic, esperar estado inicial renderizado: resumen seleccionado,
visible, con encabezados/tbody y carga oculta; detalle existente y oculto mediante
visibilidad CSS real. Evidencia estructural: HTML local `RecorridoHTML/PaginaPrincipal.txt`
y `RecorridoHTML/Recorrido-Haaland/Resumen.txt`. Después del clic exigir enlace
seleccionado y panel visible, no solo style.display. Sin sleeps, retries ni nuevos
timeouts: preparación usa navigationTimeoutMs (45000), activación conserva 8000.
No garantiza ausencia global de fallos ni atribuye todos los de run 9 a esta carrera.

### Histórico retirado: diagnóstico secuencial autorizado — 2026-10-29

Una ficha y un worker: Resumen → Detallado, mismo Browser y StatisticsWorker,
contextos separados mediante Navigator. Resumen espera su tabla sin parsear métricas;
detalle conserva captura temprana y una categoría aerial nativa. Red correlacionada
por sección; si resumen falla no continuar a detalle. Sin DB, población, retries,
cambios de clics, esperas ni timeouts. No reproduce aún cuatro workers ni la vida
prolongada del navegador. Tests de protocolo mediante dobles, ejecución humana.

### Histórico retirado: captura temprana de inicialización — 2026-10-29

Diagnóstico individual: instalar observación de red antes de navegar y de DOM en
DOMContentLoaded, antes del clic. Finalizar antes del cierre del contexto incluso si
fallan `open-tab` o `await-detail-filter`. Capturar documento/scripts/xhr/fetch,
errores JS sin mensajes, clic capture/bubble y presencia/visibilidad de panel,
contenedor, filtro y tabla. Sin URLs, contenido, IDs externos ni cambio de navegación,
timeout, retries, métricas o persistencia. El recorrido normal usa callbacks vacíos.
Las mutaciones se limitan a subárboles relevantes; máximo 1000 eventos y contador
de descartados, con estado final independiente. Validación humana individual: 7,
después 803 si resulta necesario; no repetir población para este diagnóstico.

### Corrección de lectura nativa de detalle — autorización del 2026-10-29

- Sustituye para scraping las decisiones anteriores de seleccionar acumulados `2`.
  Navigator valida modo nativo `0` antes y después de elegir cada categoría; no cambia
  ese selector. Conserva espera por render, encabezados/cuerpo/carga oculta y timeout
  de 45 s como máximo, sin sleeps ni espera mínima de 2 s, sin retries. Modo desconocido
  o distinto se rechaza, no se convierte por conjetura.
- Model explicita regla por canal en `MetricDefinition.derivationFor`: las 29 claves
  de detalle son nativas por partido en scraping; goles/asistencias del resumen conservan
  división. El accessor legado y procesamiento JSON se conservan sin cambios.
- Assembler usa esa regla y no exige apps para detalle; conserva normalización exacta,
  ausencias, selección de contexto por competición/temporada y catálogo 60/45/57/42.
  No cambia parser, Writer, claves de DB ni filas existentes.
- Actualizar tests Model de regla por canal, protocolo Integration y DOM opt-in para
  categorías sin cambio de acumulación, incluyendo rechazo de modo inesperado. Mantener
  las comprobaciones de no render/carga pendiente/HTML idéntico y tentativa única.
- Evidencia: traza humana individual (input/change de acumulados sin petición/render)
  y comparación manual de varios jugadores. Captura HAR local previa de modo `0`
  publica duelAerialTotal fraccionario con apps=6; HTML local muestra el valor por partido.
  No se accede al proveedor por el agente. Riesgo numérico del canal JSON identificado
  durante lectura se registra como deuda independiente, sin corregir fuera de alcance.

### Histórico: diagnóstico individual retirado y log general conservado — 2026-10-29

- Añadir `tools/WhoScoredScrapingDiagnostic` y
  `integrations/WhoScoredScrapingDiagnosticIntegration`: entrada manual independiente
  de Spring, sin Repository/Service, POST poblacional, run ni persistencia de métricas.
  El tool no es un rol de aplicación; toda comunicación externa queda en Integration.
  Excepción diagnóstica a la frase anterior «no se añade otro cliente»: no es un canal
  nuevo ni una vía de actualización, solo una captura acotada para T106.
- Una ficha, una navegación de detalle y una llamada a `chooseCategory(aerial)` real,
  sin retry ni cambio de timeout (45 s). IDs provistos por variables de entorno,
  validados antes de abrir navegador; ID externo nunca en logs/argumentos de Gradle.
  El ID local solo sirve para correlacionar y no se resuelve ni escribe en DB.
- Eventos de red con secuencia local, tipo, endpoint clasificado (estadísticas/otro),
  categoría/subcategoría/acumulación de una lista permitida, status y fin/fallo.
  Nunca URL completa, query libre, headers, cookies, bodies o mensajes externos.
- Observer diagnóstico independiente: secuencia temporal de input/change y mutaciones,
  tabla presente, filas/columnas, carga, reemplazo de nodo y cambio de HTML (solo
  booleanos/conteos). Buffer acotado a 1000 eventos con contador explícito de descartados;
  captura final y limpieza aun si el filtro falla. No cambia el observer ni el criterio
  de espera productivos.
- Configurar archivo persistente del backend `logs/football-market.log`, configurable
  por `FOOTBALL_MARKET_LOG_FILE`, además de consola; rotación estándar de Spring Boot.
  Perfil test sin archivo. La herramienta usa logback dedicado, consola y archivo
  append completo `logs/whoscored-diagnostic.log`, configurable por propiedad Gradle.
  Ignorar directorios de logs y documentar conservación de todos los archivos rotados.
- Tests de entradas inválidas del tool sin navegador/red y de configuración logging;
  no tests de texto de mensajes diagnósticos. Ejecución de la herramienta, backend y
  tests exclusivamente humana; preparación no autoriza acceso externo por el agente.

### Corrección autorizada tras run=7 — 2026-10-28

- FR-022 incorpora únicamente el marcador literal `N/A` tras `strip`, como `ABSENT`,
  sin valor ni precisión observada. La regla permanece en `MetricValueNormalizer`;
  sus consumidores, incluido JSON cuando use esa regla, conservan rechazo de otros textos.
- La comparación de `innerHTML` no distingue una tabla vieja de una nueva idéntica.
  Antes de seleccionar se espera la tabla inicial y se instala un `MutationObserver`
  acotado al contenedor de detalle. Se espera incorporación de estructura/contenido de
  tabla, no atributos del selector, spinners ni mera eliminación de filas; al terminar
  se exigen encabezados técnicos, cuerpo, indicador de carga oculto (si existe) y
  valor solicitado. La espera de tabla inicial también exige cuerpo y carga oculta.
  Reemplazar una tabla o
  cuerpo por uno idéntico acredita renderizado; si no se observa, falla sin leer la vieja.
- Observer y handle se liberan en `finally`. No se agregan endpoints, peticiones propias,
  sleeps, reintentos ni cambios al timeout, pacing, persistencia o catálogo.
- Regresiones controladas: ausencias y textos inválidos en Model; protocolo de armado
  antes de selección, espera y limpieza aun en timeout en Integration. La validación
  del DOM real/proveedor queda explícitamente pendiente del usuario, sin atribuir todos
  los timeouts observados a esta limitación del criterio anterior.

**Ampliación de verificación autorizada el 2026-10-28:** añadir una clase de integración
DOM con Playwright/Chromium headed reales contra HTML en memoria (`setContent`), bloqueando
todas las solicitudes del contexto. No requiere servidor, DB, Spring, proveedor real ni
dependencias nuevas. Solo se activa con `FOOTBALL_MARKET_DOM_TESTS=true`; fuera de suites
rápidas y sin descarga automática del navegador. La falta de Chromium/display al activarla
debe fallar visiblemente, no omitir los escenarios. Ejecución exclusivamente humana.
Se ejercita `chooseCategory`/`awaitTable` productivos sin copiar el observer en el test.
Casos: HTML idéntico renderizado; tabla vieja sin render (timeout); tabla renderizada con
carga aún visible (timeout); finalización diferida de carga y datos (éxito).

El run aportado intentó 38 de 2104 elegibles: 27 completados, 3 no-data y 8 fallidos; quedaron 2053 pendientes. Se observaron `PROVIDER_DATA_INCONSISTENT` por valor no decimal, esperas próximas a 45–47 s y `PlaywrightException: Failed to send message` al cerrar workers. Son síntomas, no causas demostradas.

- Seguir celda/token → parser → normalizador → ensamblador para determinar qué valor causa el rechazo. Usar capturas locales existentes o fixtures mínimos; no convertir texto inválido a cero ni ampliar formatos numéricos por conjetura. Preservar precisión, guion/ausencia y rechazo de datos realmente inválidos.
- Contrastar fases de navegación/tabla/categoría con los timeouts y retries actuales. Retirar las recargas/reintentos incompatibles con la tentativa única; esperar estados DOM relevantes, sin fijar un SLA nuevo ni bajar timeouts a ciegas. Falta de tabla obligatoria sigue siendo fallo estructural, no no-data.
- Seguir el orden de cierre, afinidad de hilo y propagación de excepciones. Corregir solo la causa confirmada y añadir regresión controlada del síntoma reproducible, sin tests exclusivos de mensajes de log.
- Si las capturas y el código no bastan para establecer una causa, registrar el punto pendiente y pedir al usuario la evidencia mínima. Esta dependencia de investigación no autoriza campaña ni tests del agente, ni una solución especulativa.

## Cliente operativo del desarrollador y contrato de ejecución

- Dos POST independientes: scraping conserva body obligatorio `{}` o `extractionDate` opcional, sin `maxPlayers`; JSON no recibe query parameters ni body. Controller transporta DTO → Mapper → Model → Service sin negocio. Retirar el máximo es una incompatibilidad explícita: rechazar el campo retirado con `400 INVALID_REQUEST`, incluso null, antes de invocar Service; actualizar DTO, Model de entrada, Mapper, advice, OpenAPI, consumidores y tests juntos al implementar. No ignorarlo silenciosamente.
- `GET /api/players/statistics/whoscored/coverage` conserva `source` obligatorio y fecha opcional. La respuesta de cada POST conserva `source` para auditoría.
- Cliente CLI Java en `backend/src/main/java/footballmarket/tools/PlayerStatisticsSyncClient.java`, fuera del contexto Spring y sin inicio automático. Reutiliza Java 21, `RestClient` y los DTO HTTP propios; no consulta WhoScored ni persistencia directamente y no añade dependencias.
- El cliente CLI existente se simplifica a un solo POST de scraping: `--extraction-date` opcional y `--base-url`, sin `--mode` ni `--max-players`. Rechazar esas opciones retiradas antes de HTTP con error local sin secretos. JWT desde `FOOTBALL_MARKET_TOKEN`; se conserva `statisticsSyncClient`, sin dependencias nuevas. No se añade otro cliente ni scheduler.
- Scraping fija fecha al inicio y la conserva hasta terminar; JSON conserva su entrada actual sin fecha, cantidad ni IDs.
- El CLI muestra el resumen de esa única petición y termina con código 0 solo si `coverageComplete = true`; si quedan pendientes o falla el transporte, salida distinta de cero sin repetir HTTP. Un POST posterior es una nueva acción del operador, no un bucle automático.
- La ejecución es operativa para el desarrollador, sin scheduler, rol API nuevo ni secretos impresos. La prueba E2E del cliente recorre HTTP real contra la aplicación y proveedores controlados, sin acceder al proveedor real.

**Scale/Scope**: Cinco ligas, unos 2100 jugadores elegibles, 60 métricas por fila de campaña; 45 cubiertas por scraping, 57 por JSON, 42 compartidas y tres exclusivas del scraping. Histórico sin política de purga en esta feature.

## Constitution Check

*GATE: evaluado antes de Phase 0 y nuevamente después de Phase 1.*

### Pre-Design Gate

| Regla | Evaluación |
| --- | --- |
| Alcance mínimo y responsabilidades coherentes (§2) | PASS. Se conservan el monolito y los roles existentes; no se agregan scheduler, cola, caché ni Orchestrator. |
| Dependencias nuevas (§2, §4) | PASS con aprobación registrada. Playwright es necesario para el scraping headed exigido; la implementación actualizará el inventario tecnológico. |
| Seguridad (§3) | PASS. JWT solo protege la API propia; URLs externas usan IDs validados y ninguna credencial propia se propaga. |
| Contratos y sincronización (§4) | PASS. Los contratos HTTP y externo se actualizan junto con DTO, OpenAPI y tests planificados. |
| Idioma (§5) | PASS. Código y claves en inglés; documentación, mensajes y OpenAPI en español. |
| Verificación (§6) | PASS. PLAN no ejecuta tests ni builds; los tests backend quedan para ejecución humana. |
| Rama y trazabilidad (§7) | PASS. `feat/012-player-statistics-scraping` reutiliza SPEC-012. |
| SDD y seguimiento | PASS documental. Preparación autorizada hasta Analyze, sin implementación ni escrituras remotas. GitHub Issues/Projects no verificados; no se afirma sincronización. |
| Arquitectura backend | PASS. Controller → Service; Service → Model/Repository/Integration. Los contratos externos no se reutilizan como DTO o Model. |
| Fuentes académicas | PASS. Se conserva el scraping completo exigido por `tp/enunciado.md`; JSON es una segunda vía y no lo sustituye. |

### Post-Design Gate

Reevaluación documental del diseño del 2026-10-27, no aprobación humana ni resultado de Analyze. El diseño conserva estas decisiones:

1. El catálogo permanece en 60 métricas; los 29 acumulados se descartan después de derivar su valor por partido.
2. `fouls_received` queda fuera del catálogo y de FR-030.
3. `foulsPerGame` y `foulCommitted / apps` son alias de `fouls_committed_per_match`, sin entidad de equivalencia.
4. JSON cubre 57 métricas y scraping 45; las 42 compartidas permiten actualización independiente, y cada canal persiste su propia fila de campaña.
5. Cada canal persiste su fila de campaña con la procedencia real; una campaña nueva no sobrescribe las anteriores y una fila con contexto resuelto no se degrada.
6. Las tres métricas no observadas en JSON (`shots_on_target_total`, `second_yellow_total`, `starts_total`) permanecen cubiertas por scraping.

Post-design: sin tecnologías nuevas ni roles/dependencias nuevos; timeouts externos preservados; retiro contractual explícito y consumidores cubiertos; sin tests/builds en preparación. Las autorizaciones históricas no cubren el nuevo alcance. Investigación causal pendiente como trabajo técnico ordenado, no excusa para cambiar reglas numéricas. Próximo control: checklists evaluados y Analyze; no existe gate READY adicional.

## Project Structure

### Estructura objetivo del refactor organizativo — 2026-11-01

Esta propuesta sustituye para este refactor el árbol horizontal histórico de más abajo, no sus decisiones funcionales. Raíz: `backend/src/main/java/footballmarket/features/statistics/`.

```text
statistics/
├── application/
│   ├── PlayerStatisticsSynchronizationService.java
│   ├── PlayerStatisticsExtractionService.java
│   ├── JsonStatisticsUpdateService.java
│   ├── PlayerStatisticsCoverageService.java
│   ├── PlayerStatisticsQueryService.java
│   ├── exceptions/                         # Sin movimientos
│   ├── impl/
│   │   └── PlayerStatisticsQueryServiceImpl.java  # Lectura existente, fuera del refactor
│   ├── scraping/
│   │   ├── impl/
│   │   │   └── PlayerStatisticsExtractionServiceImpl.java
│   │   └── internal/
│   │       ├── ExtractionBatchExecutor.java
│   │       ├── HeadedBrowserAvailabilityChecker.java
│   │       └── ScrapingMetricAssembler.java
│   ├── json/
│   │   ├── impl/
│   │   │   └── JsonStatisticsUpdateServiceImpl.java
│   │   └── internal/
│   │       └── JsonMetricAssembler.java
│   └── shared/
│       ├── impl/
│       │   ├── PlayerStatisticsSynchronizationServiceImpl.java
│       │   └── PlayerStatisticsCoverageServiceImpl.java
│       └── internal/
│           ├── ExtractionRunManager.java
│           ├── ExtractionWorkerPool.java
│           ├── PlayerStatisticsEligibilitySelector.java
│           ├── PlayerStatisticsWriter.java
│           ├── ExtractionIncidentRecorder.java
│           ├── LeagueContextSelection.java
│           ├── PlayerMetricsContext.java
│           └── PlayerStatisticsCoverageQuery.java
├── domain/                                # Sin movimientos ni reglas nuevas
├── infraestructure/                        # Nombre existente preservado
└── presentation/                           # Contratos y rutas sin cambios
```

Las interfaces públicas de aplicación permanecen en `application/` para preservar sus nombres cualificados y los consumidores ajenos al refactor. `impl` e `internal` conservan la distinción existente entre implementación de Service y composición interna; no se crean roles arquitectónicos nuevos. `shared` solo reúne componentes efectivamente compartidos por los canales, no cualquier utilidad de la feature. El dominio y los repositorios ya compartidos permanecen en sus capas actuales, sin duplicarlos bajo `shared`.

#### Mapa de movimientos previsto

Todos los orígenes y destinos siguientes son relativos a `application/`; los nombres de archivo se conservan.

| Origen actual | Destino | Archivos |
| --- | --- | --- |
| `impl/` | `scraping/impl/` | `PlayerStatisticsExtractionServiceImpl.java` |
| `internal/` | `scraping/internal/` | `ExtractionBatchExecutor.java`, `HeadedBrowserAvailabilityChecker.java`, `ScrapingMetricAssembler.java` |
| `impl/` | `json/impl/` | `JsonStatisticsUpdateServiceImpl.java` |
| `internal/` | `json/internal/` | `JsonMetricAssembler.java` |
| `impl/` | `shared/impl/` | `PlayerStatisticsSynchronizationServiceImpl.java`, `PlayerStatisticsCoverageServiceImpl.java` |
| `internal/` | `shared/internal/` | `ExtractionRunManager.java`, `ExtractionWorkerPool.java`, `PlayerStatisticsEligibilitySelector.java`, `PlayerStatisticsWriter.java`, `ExtractionIncidentRecorder.java`, `LeagueContextSelection.java`, `PlayerMetricsContext.java`, `PlayerStatisticsCoverageQuery.java` |

Los tests exclusivos de un componente movido reflejarán su ubicación bajo `backend/src/test/java/footballmarket/features/statistics/`, conservando sus casos. Los tests HTTP, persistencia, seguridad, dominio y E2E permanecen en sus categorías actuales; solo cambian imports/referencias si resulta necesario. No se diseñan escenarios funcionales nuevos por un movimiento de paquetes. No se modifica la estructura de `footballmarket.integrations.whoscored`, compartida con otros alcances, ni el cliente operativo.

#### Orden de lectura del flujo de scraping

1. `presentation/PlayerStatisticsController.synchronizeScraping` recibe la petición y llama a la interfaz pública.
2. `application/shared/impl/PlayerStatisticsSynchronizationServiceImpl.synchronizeScraping` conserva resolución de fecha, exclusión, apertura/cierre del run y gestión de recursos/fallos. Usa `shared/internal/ExtractionRunManager` y `PlayerStatisticsEligibilitySelector`.
3. `scraping/impl/PlayerStatisticsExtractionServiceImpl.openBatch` abre la sesión mediante `scraping/internal/HeadedBrowserAvailabilityChecker`.
4. `scraping/internal/ExtractionBatchExecutor.execute` recorre el snapshot mediante `shared/internal/ExtractionWorkerPool` y llama al batch para cada jugador.
5. `scraping/impl/PlayerStatisticsExtractionServiceImpl` resuelve la referencia y llama a `WhoScoredStatisticsScrapingIntegration`, que sigue fuera de la feature.
6. `scraping/internal/ScrapingMetricAssembler` produce `shared/internal/PlayerMetricsContext` aplicando las reglas existentes del dominio.
7. `shared/internal/PlayerStatisticsWriter` selecciona contexto mediante `LeagueContextSelection` y confirma los datos; `ExtractionIncidentRecorder` registra incidencias cuando corresponde. La fachada cierra recursos y run y devuelve el resumen.

#### Orden de lectura del flujo JSON

1. `presentation/PlayerStatisticsController.synchronizeJson` rechaza entrada no permitida y llama a la interfaz pública.
2. `application/shared/impl/PlayerStatisticsSynchronizationServiceImpl.synchronizeJson` conserva fecha UTC, apertura del run y selección mediante los mismos componentes compartidos.
3. Ese método conserva su bucle concurrente con `shared/internal/ExtractionWorkerPool`, creación de workers de la integración y condiciones de interrupción propias de JSON.
4. `json/impl/JsonStatisticsUpdateServiceImpl.update` resuelve la referencia y llama a `WhoScoredStatisticsIntegration`, sin navegador.
5. `json/internal/JsonMetricAssembler` produce los contextos del jugador usando las reglas existentes del dominio.
6. Se utilizan los mismos `PlayerStatisticsWriter`, `LeagueContextSelection` y `ExtractionIncidentRecorder`; la fachada cierra el run y devuelve el resumen.

**Límite deliberado**: la fachada conserva ambos métodos. Mover paquetes mejora la localización sin prometer separar sus algoritmos. Extraer el recorrido de cada canal a clases nuevas sería otro refactor, no está incluido en este diseño. Se preservan diferencias de bloqueo, recuperación, conteo de llamadas, errores, reintentos y limpieza; no se corrigen ni armonizan aprovechando los movimientos.

#### Constitution Check del alcance acotado (pre y post diseño)

- Simplicidad y proporcionalidad: agrupación por responsabilidad real; ninguna dependencia, interfaz, algoritmo o componente nuevo. No hay `NEEDS CLARIFICATION` pendiente para decidir los destinos.
- Arquitectura: los movimientos no autorizan dependencias nuevas. Se conservan roles y contratos públicos; la composición interna permanece dentro del caso de uso, conforme a arquitectura backend §1.4. No se añaden Services independientes ni Orchestrator.
- Contratos e integridad: no cambian endpoints, DTO, seguridad, reglas numéricas, catálogo 45/57/42, persistencia, transacciones, pacing ni ciclo de vida del navegador. No se modifican requisitos de `spec.md` ni requisitos académicos.
- Artefactos: `research.md` documenta la elección; `quickstart.md` agrega la revisión estructural futura. `data-model.md` y `contracts/` siguen vigentes y no necesitan modificaciones: no hay entidades, estados, esquema ni interfaces externas nuevos.
- Evidencia: evaluación documental antes y después del diseño sin contradicciones identificadas en este alcance, no aprobación humana ni Analyze. No se ejecutaron tests ni build. Las verificaciones de la implementación quedan pendientes; no se acredita resolución de deuda previa.
- Siguiente paso permitido: checklists evaluados para esta reorganización. `tasks.md` no se modifica durante PLAN; tampoco se implementa código ni se cambia el estado operativo remoto.

### Documentation (this feature)

```text
specs/012-player-statistics-scraping/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── http.md
    └── whoscored.md
```

### Source Code (repository root)

**Antecedente histórico conservado**: el árbol siguiente precede a la organización actual por features; para el refactor del 2026-11-01 rige la estructura objetivo anterior.

```text
backend/
├── build.gradle.kts
├── gradle/libs.versions.toml
├── src/main/java/footballmarket/
│   ├── config/
│   ├── controllers/
│   │   ├── dtos/requests/
│   │   ├── dtos/responses/
│   │   ├── exceptions/
│   │   └── mappers/
│   ├── integrations/
│   │   └── records/
│   ├── models/
│   │   ├── enums/
│   │   └── records/
│   ├── repositories/
│   ├── tools/                   # Cliente operativo, no bean de la aplicación
│   └── services/
│       ├── impl/
│       └── internal/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
└── src/test/java/footballmarket/
    ├── config/
    ├── controllers/
    ├── e2e/
    ├── integrations/
    ├── models/
    ├── repositories/
    ├── services/
    └── support/

docs/backend/technologies.md
```

**Structure Decision**: Un único `PlayerStatisticsSynchronizationService` actúa como fachada del caso de uso y selecciona el canal solicitado. Dos Integrations encapsulan Playwright/DOM y `RestClient`/JSON. Model contiene el catálogo y las reglas de normalización de valores, derivación por partido y resolución de alias, calculables exclusivamente con datos de dominio. Los componentes internos del rol Service aplican esas reglas dentro del caso de uso y resuelven selección y persistencia; no son Services independientes ni justifican un Orchestrator.

**Componentes añadidos tras el análisis de consistencia**: `MetricCatalog` y `MetricDefinition` en `models/` materializan el catálogo cerrado de 60 métricas; `MetricValueNormalizer` y `MetricDerivationApplier`, también en `models/`, encapsulan normalización, derivación y alias sin depender de Service, Repository ni Integration. `WhoScoredStructuredDataParser` en `integrations/` cubre el bloque estructurado; `PlayerStatisticsProperties` en `config/` valida defaults. `PlayerStatisticsWriter`, `ExtractionIncidentRecorder`, `ExtractionRunManager`, `ExtractionBatchExecutor`, `ExtractionWorkerPool`, `PlayerStatisticsEligibilitySelector`, `PlayerStatisticsCoverageQuery`, `HeadedBrowserAvailabilityChecker`, `ScrapingMetricAssembler`, `JsonMetricAssembler` y `PlayerMetricsContext` en `services/internal/` componen el caso de uso. El escritor confirma la fila de campaña y su item en una sola transacción por jugador. Los dobles de test viven en `support/`; el cliente operativo vive en `tools/`.

## Phase 0: Research

Las decisiones consolidadas están en [research.md](research.md). La evidencia de métricas ya obtenida se acepta como entrada: PLAN no repite pruebas contra WhoScored ni deja métricas implícitas por descubrir.

## Phase 1: Design & Contracts

- [data-model.md](data-model.md): fila de campaña por jugador, fecha y canal, cobertura por canal, alias y estados.
- [contracts/http.md](contracts/http.md): endpoints separados, scraping sin máximo de jugadores, compatibilidad explícita y cobertura por canal.
- [contracts/whoscored.md](contracts/whoscored.md): recorrido headed y quince combinaciones JSON conocidas.
- [quickstart.md](quickstart.md): validación humana posterior a implementación para ambos canales.

## Complexity Tracking

No hay violaciones constitucionales ni excepciones arquitectónicas que justificar.

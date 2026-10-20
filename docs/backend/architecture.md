# Arquitectura del backend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md). Leer §1 y, para integraciones externas, §2. Las reglas de naming, formato y estilo de implementación están en [convenciones del backend](conventions.md); el stack, versiones y dependencias, en [tecnologías del backend](technologies.md); el modelo de datos persistido y su diagrama de entidades, en [modelo de datos del backend](data-model.md). Consultar estos documentos según el área afectada.

## 1. Arquitectura

### 1.1. Roles del backend

| Rol | Responsabilidad y restricciones propias |
| --- | --- |
| Controller | Frontera HTTP, sin lógica de negocio. Respecto de Model, solo puede transportar directamente el resultado de un Mapper hacia el rol permitido por la matriz y el resultado correspondiente hacia un Mapper; no puede declarar, inspeccionar, modificar ni aplicar lógica sobre Model. |
| Orchestrator | Coordinación entre Services, sin lógica de dominio ni lógica de aplicación propia que corresponda a un Service. Se crea únicamente cuando existe una responsabilidad real de coordinación; utilizar múltiples Services no obliga a crearlo. Puede transportar de forma opaca un Model producido por un Service hacia otro Service y devolver directamente un Model producido por un Service como resultado de la coordinación. No puede construir, modificar, transformar ni inspeccionar el Model, usarlo para tomar decisiones o realizar cálculos, ni ejecutar comportamiento de dominio sobre él. Tampoco depende de Repository, Integration u otro Orchestrator. Ubicación exclusiva: `footballmarket.orchestrators`, nunca dentro de features u otros roles. |
| Service | Lógica de aplicación. Interfaz e implementación separadas obligatorias incluso con una sola implementación; consumidores dependen de la interfaz. Esta regla específica prevalece sobre evitar interfaces innecesarias. |
| Model | Conceptos, estado, comportamiento e invariantes del dominio. |
| Repository | Acceso a persistencia, sin negocio. Utiliza normalmente interfaces Spring Data que trabajan con Model persistibles. `@Repository` se permite solo en interfaces Spring Data, nunca en puertos, implementaciones manuales o repositorios de test. |
| Integration | Adaptación de sistemas externos, detallada en §3; solo Service puede consumirla entre los roles enumerados. |

En HTTP con datos de dominio, el flujo sin Orchestrator es `DTO → Mapper → Model → Service → Model → Mapper → DTO`. Cuando la coordinación requiere un Orchestrator, este puede insertar pasos de coordinación entre Services: recibe de un Service un Model, lo transporta directamente a otro Service y puede devolver al Controller el Model producido por un Service para que el Mapper lo convierta a DTO. El Controller se limita al transporte directo entre los pasos permitidos y no trabaja con Model. Ni Service ni Orchestrator devuelven `null` cuando el contrato espera respuesta.

### 1.2. DTO y Mapper del backend

DTO representa exclusivamente entrada/salida HTTP; Service, Model y Repository no dependen de DTO. Mapper transforma únicamente `DTO ↔ Model`, sin negocio, validaciones de negocio, cálculos ni decisiones semánticas; no depende de Service ni Repository. Los contratos de sistemas externos se rigen por §3.

### 1.3. Dependencias del backend

| Origen | Destinos permitidos |
| --- | --- |
| Controller | DTO, Mapper, Service, Orchestrator. |
| Orchestrator | Service, Model. |
| Service | Model, Repository, Integration; otro Service exclusivamente para la integración de portfolio descrita en §5. |
| Repository | Model. |

DTO y Mapper se rigen por §1.2. Model no depende de Service, Repository ni Integration. Controller no depende de Model, Repository o Integration. La dependencia `Orchestrator → Model` se permite exclusivamente para transportar de forma opaca entre Services un Model producido por uno de ellos o para devolver un Model producido por un Service como resultado de la coordinación; no autoriza ninguna lógica sobre el Model. Orchestrator no depende de Repository, Integration ni otro Orchestrator. Integration solo puede ser consumida por Service entre los roles enumerados.

Estas matrices tienen el alcance definido en la [arquitectura común](../architecture.md) §1: regulan relaciones entre roles arquitectónicos, no todos los tipos que un componente puede utilizar. Ante una decisión arquitectónica no definida por la documentación vigente, aplicar su regla de vacíos normativos y la gobernanza de la constitución antes de introducirla.

### 1.4. Dominio y aplicación del backend

Las reglas resolubles exclusivamente con información y comportamiento de dominio pertenecen a Model, incluso si involucran varios Model. Las que requieren Repository, Integration u otras capacidades externas al dominio pertenecen a Service.

La pertenencia de esa lógica al rol Service se define por lo establecido en §1.1 y no por el uso de Repository: un componente forma parte del rol Service sin constituir un Service independiente cuando compone internamente un caso de uso, no expone interfaz de aplicación, no es consumido por otros Services y comparte el límite transaccional del Service que lo compone. Sus dependencias hacia Model, Repository e Integration son las del propio rol Service. El Service coordinador conserva la condición de §1.1 —interfaz e implementación separadas y consumidores dependientes de la interfaz— y es la fachada del caso de uso. Esta composición interna no habilita dependencias entre Services ni ciclos, no autoriza su reutilización transversal y no sustituye al Orchestrator cuando la coordinación corresponde a casos de uso independientes. La excepción autorizada de §5 corresponde a un Service independiente reutilizable, no a composición interna ni a una habilitación general de dependencias Service → Service.

### 1.5. Model del backend

Los Model deben preservar sus invariantes y encapsular su estado.

No se impone mutabilidad ni inmutabilidad general a los Model. Cuando necesiten modificar su estado como parte del comportamiento de dominio, deben hacerlo mediante operaciones explícitas que preserven sus invariantes.

Pueden utilizar directamente los mecanismos de persistencia/JPA necesarios para su representación; no se exige separar entidades JPA del Model de dominio.

### 1.6. Excepciones del backend

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante `DomainException`, `ApplicationException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe heredar de la categoría correspondiente. `ApplicationException` representa errores controlados propios de la aplicación o de sus casos de uso que no correspondan al dominio, la integración o la persistencia.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta jerarquía. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el funcionamiento interno de esas tecnologías.

### 1.7. Contrato HTTP del backend

- Toda ruta de Controller comienza con el prefijo exacto `/api/`; `/api` y prefijos como `/apiv2` no son válidos.
- El body es opcional. Si una respuesta exitosa contiene datos, utilizar Response DTO, sin exponer directamente Model internos.
- Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales. La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.
- Validación estructural HTTP en Controller/DTO; negocio en Service/Model; contratos externos en Integration.
- Capturar errores estructurales de Jakarta Validation en presentación. Todo error HTTP controlado por la aplicación utiliza `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`; `status` coincide con el código HTTP, `code` identifica establemente el tipo de error y `path` identifica la ruta solicitada. `message` es legible y no sirve como identificador programático. Ante múltiples errores de validación, `message` contiene el primer mensaje disponible.

## 2. Integraciones externas del backend

Toda comunicación externa pasa por Integration, que encapsula configuración, comunicación, contratos externos, transformación y errores técnicos. Model no conoce proveedores, clientes, HTTP ni protocolos. Los contratos externos permanecen separados de los DTO HTTP propios y de los Model internos; estos tipos no se reutilizan como contratos externos aunque coincidan estructuralmente.

Validar respuestas externas y distinguir errores técnicos de integración de errores de dominio. No inventar datos ni alterar silenciosamente el comportamiento ante fallos. Usar timeouts cuando la tecnología lo permita; retries solo con operación segura y límite explícito, nunca por defecto.

Resolver indisponibilidad según requisitos funcionales; no introducir automáticamente cachés, degradaciones o alternativas sin justificación funcional o técnica. Aplican las reglas globales de seguridad de la constitución.

## 3. Estadísticas de rendimiento de jugadores (SPEC-012)

Componentes añadidos por SPEC-012. El inventario de dependencias y versiones vive en
`docs/backend/technologies.md` y no se repite aquí. El contrato funcional completo está en
`specs/012-player-statistics-scraping/`.

### 3.1. Capas

- Controller: `PlayerStatisticsController` expone únicamente `POST /api/players/statistics/whoscored/sync/scraping`,
  `POST /api/players/statistics/whoscored/sync/json` y `GET /api/players/statistics/whoscored/coverage`.
  La ruta determina el canal; el body de scraping solo admite `extractionDate` y `maxPlayers`, y la
  entrada JSON rechaza cualquier parámetro o cuerpo.
- Services: `PlayerStatisticsSynchronizationService` es la fachada que abre y cierra la campaña;
  `PlayerStatisticsExtractionService` ejecuta un jugador de scraping; `JsonStatisticsUpdateService`
  ejecuta un jugador por JSON; `PlayerStatisticsCoverageService` responde la cobertura sin acceder
  al proveedor. Los componentes internos de `services/internal/` resuelven selección, lotes,
  ritmo, ensamblado y confirmación por jugador.
- Model: `MetricCatalog` y `MetricDefinition` materializan el catálogo cerrado de 60 métricas con
  cobertura 45/57/42 por canal; `MetricValueNormalizer` y `MetricDerivationApplier` encapsulan
  normalización, alias y derivación por partido sin dependencias hacia fuera de Model.
- Integrations: `WhoScoredStatisticsIntegration` consulta la interfaz JSON pública sobre el
  `RestClient` seguro ya existente; `WhoScoredStatisticsScrapingIntegration` recorre la ficha
  headed con Playwright. Ambas reutilizan `WhoScoredRequestPacer`, que no se duplica. El navegador se
  inicializa de forma perezosa: el arranque de la aplicación no abre Chromium.
- Persistencia: una fila `player_statistics` por jugador, fecha de campaña y canal, con las 60
  métricas en columnas `NUMERIC` sin precisión ni escala fijas. `player_statistics_extraction_runs`,
  `player_statistics_run_items` y `player_statistics_incidents` conservan la auditoría de la
  campaña. Las entidades de contexto, observación y contraste por canal no forman parte del modelo
  vigente.

### 3.2. Contrato HTTP

- `POST /sync/scraping` acepta `extractionDate` opcional no futura y `maxPlayers` entero positivo
  opcional; el límite es global entre workers y el alcance temporal lo acota el ritmo de campaña.
- `POST /sync/json` no admite query parameters ni body; fija la fecha UTC al iniciar y recorre
  todos los pendientes de esa campaña sin corte temporal ni límite de cantidad.
- Ambos devuelven el mismo resumen: identificación del run, estado, contadores de jugadores, de
  métricas y de llamadas al proveedor, y `coverageComplete`. Las ausencias se informan solo como
  conteo agregado.
- `GET /coverage` exige `source` y admite `extractionDate` opcional; devuelve conteos por fecha y
  canal, `systemicallyAbsentMetrics` y el último run. No accede al proveedor.
- Errores controlados: `INVALID_STATISTICS_SOURCE`, `INVALID_STATISTICS_EXTRACTION_DATE`,
  `INVALID_STATISTICS_MAX_PLAYERS`, `INVALID_REQUEST`, `PLAYER_STATISTICS_SYNC_IN_PROGRESS`,
  `PLAYER_STATISTICS_SYNC_FAILED` y `HEADED_BROWSER_UNAVAILABLE`, este último solo para scraping.

### 3.3. Cliente operativo

`tools/PlayerStatisticsSyncClient` es un cliente CLI de scraping ejecutado por el desarrollador,
fuera del contexto Spring, con modos `batch` y `campaign`. No consulta WhoScored ni la
persistencia directamente, no expone opciones de canal y obtiene el JWT del entorno. JSON se invoca
por POST autenticado sin entrada.

## 4. Valoraciones estadísticas de jugadores (SPEC-013)

- Controller: `ValuationController` expone únicamente `POST /api/valuations/evaluate`, sin body,
  query ni selección de jugador o estrategia. Conserva la autenticación Bearer y devuelve un
  resumen agregado.
- Service: `ValuationEvaluationService` es la fachada del recorrido poblacional. Los componentes
  internos seleccionan la última campaña JSON confirmada, preparan poblaciones y publican cada par
  de resultados en una transacción independiente, permitiendo continuar ante fallos individuales.
- Snapshot: una campaña JSON está confirmada cuando su run está terminado (`COMPLETED` o `PARTIAL`),
  ningún item quedó en fallo estructural (`INCOMPLETE`, `INTERRUPTED`, `BLOCKED`,
  `STRUCTURE_CHANGED`) y todo item `COMPLETED` tiene su fila JSON de esa fecha. Los items sin datos
  del proveedor y los fallos de un jugador concreto no invalidan la campaña: esos jugadores quedan
  sin snapshot y se excluyen como `MISSING_OR_UNCONFIRMED_SNAPSHOT`.
- Model: `ValuationMetricAssembler` y `ValuationMetricPipeline` resuelven fallback, derivaciones,
  imputaciones, percentiles, confianza y precisión. `PositionalValuationCalculator` y
  `AttributeValuationCalculator` contienen las fórmulas versionadas de las dos estrategias.
  `GeneralPosition` traduce la posición persistida por el catálogo (`Goalkeeper`, `Defence`,
  `Midfield`, `Offence` y sus equivalentes en inglés) al conjunto cerrado de cuatro posiciones
  generales.
- Persistencia: `player_positional_valuations` y `player_attribute_valuations` mantienen una fila
  por jugador, fecha, estrategia y versión, con componentes reconstruibles e índices de fecha,
  resultado y jugador/fecha. Ambas filas se publican o retiran juntas. La republicación materializa
  el borrado previo antes del insert para respetar la unicidad por jugador, fecha y estrategia.
- Límites: el caso de uso solo lee estadísticas locales de SPEC-012. No extrae datos, programa
  ejecuciones, expone lecturas, elige estrategia activa ni convierte scores en cotizaciones.

## 5. Contabilidad de posiciones (SPEC-015)

- `PlayerFinancialPosition` conserva costo remanente autoritativo, promedio derivado nullable,
  resultado realizado acumulado, hechos del ciclo, contadores por rol y snapshot de valoración.
  `FinancialPositionCalculator` reconstruye el historial por instante e identidad y expone
  `aggregate` sobre el conjunto completo, nunca sobre una página. Son Model puros sin E/S.
- `FinancialPositionService` y su implementación cargan operaciones y emisiones en dos consultas
  de conjunto; usan el lote `QuoteSource.currentQuotes` para posiciones abiertas, sin fallback
  individual, endpoints ni tablas de costo persistidas. El método anterior `currentQuote(Long)`
  sigue disponible para compras y publicaciones.
- RA-1 autoriza específicamente al Service del portfolio de SPEC-014 a consumir la interfaz
  `FinancialPositionService`. No se permiten ciclos ni llamadas inversas, ni se habilita
  Service → Service para otras relaciones. El consumidor no reinterpreta operaciones ni duplica
  fórmulas: utiliza las posiciones y la agregación de Model.
- La lectura es `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`. El consumidor
  debe mantener ese aislamiento en su transacción envolvente; la propagación REQUIRED no lo
  eleva automáticamente. La garantía local no implica una transacción distribuida con proveedores.
- Una clave omitida en el lote representa ausencia de cotización; un fallo técnico aborta la
  respuesta completa. Una fuente sin soporte masivo rechaza explícitamente esa capacidad.
  Los contratos HTTP y su traducción de errores pertenecen al consumidor, no a SPEC-015.

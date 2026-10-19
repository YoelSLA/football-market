# Implementation Plan: Scraping de estadísticas de rendimiento de jugadores

**Branch**: `feat/012-player-statistics-scraping` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-player-statistics-scraping/spec.md`

**Planning Status**: DISEÑO COMPLETO, READY LOCAL — alineado con la implementación vigente el 2026-10-18: cobertura efectiva 45/57/42, quince combinaciones JSON, persistencia aplanada por campaña y canal, y retirada del contraste entre canales, que conserva sus FR identificadores como retirados. Las correcciones documentales autorizadas el 2026-10-16 (decimales) y los gates históricos se conservan en `sdd-gates.md`. Sincronización Trello pendiente.

## Summary

Incorporar dos vías independientes y síncronas: scraping por lotes y JSON sobre todos los pendientes del día UTC en una petición sin parámetros ni body. Playwright ejecutará Chromium visible y conservará el recorrido de 45 métricas; el `RestClient` dedicado consultará las quince combinaciones JSON para obtener 57. Ambas vías normalizan al catálogo cerrado de 60 claves y persisten una fila de campaña por jugador, fecha y canal, con la procedencia del canal en la propia fila. Ningún canal repite jugadores ya confirmados para su misma fecha y canal.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC/`RestClient`, Spring Data JPA, Spring Security/OAuth2 Resource Server, Flyway, pgJDBC y Microsoft Playwright para Java 1.63.0. Playwright es una dependencia nueva aprobada para esta feature; su incorporación actualizará `docs/backend/technologies.md` y la configuración efectiva.

**Storage**: PostgreSQL gestionado por Flyway. Catálogo inmutable de 60 métricas en código y persistencia aplanada: una fila por jugador, fecha de campaña y canal con las 60 métricas en columnas, más las tablas de ejecuciones, items e incidencias. Una campaña nueva no sobrescribe las filas anteriores; dentro de la misma campaña la fila se actualiza.

**Decimal precision**: columnas `value`, `tolerance` y `difference` de tipo PostgreSQL
`NUMERIC` sin precisión ni escala fijas, con mappings JPA explícitos a `numeric` sin
`precision`/`scale` fijas. Lectura del token original y operaciones mediante `BigDecimal`,
sin conversión intermedia a punto flotante ni redondeo de valores directos. El rating
`6.9033333333333333` conserva sus 16 decimales y su metadato de precisión tras persistirlo.
Solo la derivación por `apps` aplica seis decimales HALF_UP; no se modifica FR-027.
T032/T033/T034/T083 precisan la regresión de lectura/normalización/persistencia. La precisión
observada por métrica y el contraste entre canales no forman parte del modelo vigente.

**Testing**: JUnit Jupiter, Mockito, MockMvc y Testcontainers con PostgreSQL 18. Los tests se implementarán conforme a `docs/backend/testing.md`; su ejecución corresponde al usuario y nunca utilizará Internet ni WhoScored real.

**Target Platform**: Backend Spring Boot. La vía JSON funciona en el entorno HTTP habitual; la vía de scraping requiere una sesión gráfica capaz de mostrar Chromium. La aplicación arranca aunque Playwright o el entorno gráfico no estén disponibles.

**Project Type**: Servicio web backend en el monolito existente.

**Performance Goals**: Objetivo informativo, sin umbral de aceptación, de unas 10 horas para una campaña de scraping de aproximadamente 2100 jugadores con tres workers, pacing de 2500 ms y proveedor disponible, incluido el encadenamiento de lotes. Las 4–5 horas extrapoladas del benchmark de 21–27 s no validan el diseño final. Cada lote de scraping admite hasta 30 minutos. La configuración vigente es de cuatro workers y 1500 ms de intervalo mínimo, por lo que la medición humana debe declarar los valores usados. JSON recorre todos los pendientes de su día UTC en una petición, realiza quince consultas conocidas por jugador y reporta cobertura/duración por separado, sin corte de 30 minutos ni promesa de 10 horas. La evidencia temporal real queda a cargo del usuario conforme a SC-012, que informa el resultado con sus limitaciones y no habilita ni bloquea gates.

**Constraints**: Scraping siempre headed; máximo inicial de cuatro workers por canal; intervalo mínimo inicial de 1500 ms por worker; un reintento máximo por sección o consulta solo ante transporte o timeout; contexto de navegador independiente por sección; sin transacción durante acceso externo; sin reintentos para 403, 404, estructura inválida o datos incoherentes; sin ejecución al arrancar; sin propagar JWT ni secretos al proveedor.

**Regla de corte del lote de scraping**: el alcance estimado deriva del presupuesto temporal restante dividido por el ritmo observado por jugador de la campaña `SCRAPING`, distribuido entre los workers disponibles. El ritmo es la media de las últimas 20 confirmaciones `COMPLETED` de igual fecha/canal, ordenadas por `finishedAt` e `id`; con menos muestras se usan todas. Se reconstruye de los tiempos persistidos en items, sin tabla ni propiedad mutable. Sin muestras se usa un valor técnico conservador de 60 segundos por jugador, no medido. No se selecciona un jugador si la estimación no cabe. `maxPlayers` opcional limita adicionalmente la selección total entre workers, sin multiplicarlo por worker. Operaciones y esperas se acotan al deadline; al agotarlo se interrumpe lo no confirmado y se cierra sin datos parciales válidos. T090 cubre arranque, reconstrucción y límites combinados. JSON no aplica este algoritmo de corte ni `maxPlayers`.

**Recorrido JSON diario**: la fachada expone una entrada sin argumentos funcionales para JSON y obtiene la fecha mediante Clock UTC del backend una sola vez al inicio. Fija la población de referencias confirmadas elegibles al abrir el run y selecciona todos sus pendientes JSON, por ID local ascendente, sin tamaño máximo ni deadline de lote de scraping. Reutiliza selector, pool, pacer, auditoría y confirmación por jugador; no usa el corte de `ExtractionBatchExecutor`. Cada seleccionado se procesa una vez por run más el retry técnico permitido; no se vuelve a seleccionar un fallido dentro del mismo run. Un fallo individual no aborta los demás; bloqueo global, rate limit prolongado o interrupción cierran con el resultado alcanzado y pendientes explícitos, respetando el aplazamiento del proveedor sin insistencia. No hay transacción abierta durante consultas ni nuevo proceso asíncrono. Los timeouts por consulta permanecen activos. Otra llamada el mismo día omite los ya `COMPLETED`; si no hay pendientes, devuelve resumen `COMPLETED`, selección y llamadas cero, sin consultar WhoScored. Cruzar medianoche no cambia la fecha inicial; una petición nueva otro día inicia otra campaña. Los nuevos elegibles después del snapshot se contabilizan como pendientes en cobertura y se seleccionan en una petición posterior, nunca se declara cobertura completa mientras falten elegibles actuales.

**Ritmo y reintentos**: se extiende `WhoScoredRequestPacer`, que ya gobierna ritmo, timeouts y `Retry-After` para el canal de identidad, con los valores por canal del catálogo de estadísticas. No se crea un segundo pacer ni un segundo cliente HTTP: las integraciones de estadísticas reutilizan `WhoScoredClientConfig` y `WhoScoredProperties`, y solo amplían el contrato de dominio que cada una encapsula.

El permiso de scraping regula inicios de navegación de sección por worker, incluidos retries, no cada recurso. El permiso JSON regula cada consulta por worker. Se conserva la política de identidad y se permiten cuatro workers estadísticos sin serializar todo el recorrido bajo un bloqueo único. `Retry-After` se respeta en ambos canales; solo scraping añade el deadline de lote. JSON conserva timeouts por consulta y cierre controlado por indisponibilidad global, sin heredar el corte temporal.

## Cliente operativo del desarrollador y contrato de ejecución

- Dos POST independientes: `/api/players/statistics/whoscored/sync/scraping` recibe el Request DTO con `extractionDate` y `maxPlayers` opcionales; `/api/players/statistics/whoscored/sync/json` no recibe query parameters ni body y llama a la entrada JSON sin argumentos funcionales de la fachada. El Controller limita su trabajo a validación estructural/transporte; para scraping transporta el Model producido por Mapper. No se expone un POST genérico `/sync` ni `source` en body.
- `GET /api/players/statistics/whoscored/coverage` conserva `source` obligatorio y fecha opcional. La respuesta de cada POST conserva `source` para auditoría.
- Cliente CLI Java en `backend/src/main/java/footballmarket/tools/PlayerStatisticsSyncClient.java`, fuera del contexto Spring y sin inicio automático. Reutiliza Java 21, `RestClient` y los DTO HTTP propios; no consulta WhoScored ni persistencia directamente y no añade dependencias.
- El cliente CLI se limita a scraping. El desarrollador elige `--mode=batch|campaign`, `--extraction-date` opcional, `--max-players` opcional y `--base-url`; no necesita opción `--source`. El JWT se obtiene de `FOOTBALL_MARKET_TOKEN`, nunca de un argumento registrado o una URL. La ejecución manual se expondrá mediante la tarea Gradle `statisticsSyncClient`, con `clientArgs` solo para opciones no sensibles. JSON se invoca directamente por POST autenticado sin entrada, sin modos de cliente ni loop de peticiones.
- Para scraping la fecha por defecto se resuelve una vez al iniciar el cliente y se envía en todas las peticiones, siempre a `/sync/scraping`. Para JSON la fecha la fija el backend; el desarrollador no puede pasar fecha, cantidad ni IDs.
- En scraping `batch` hace un POST y muestra el resumen; `campaign` consulta cobertura inicial y repite POST solo si disminuyen los pendientes de esa fecha y `SCRAPING`. Termina con código 0 solo con cobertura completa; sin progreso, `FAILED`/`INTERRUPTED`, error HTTP/transporte o interrupción local termina con código distinto de 0. No reintenta POST inciertos ni cambia fecha/canal para aparentar progreso.
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
| SDD y Trello (§8) | PASS documental. PLAN no habilita Implement ni crea tarjetas US; su evidencia operativa queda pendiente de sincronización. |
| Arquitectura backend | PASS. Controller → Service; Service → Model/Repository/Integration. Los contratos externos no se reutilizan como DTO o Model. |
| Fuentes académicas | PASS. Se conserva el scraping completo exigido por `tp/enunciado.md`; JSON es una segunda vía y no lo sustituye. |

### Post-Design Gate

Evaluación documental contrastada por el último Analyze sin blockers. El diseño conserva estas decisiones:

1. El catálogo permanece en 60 métricas; los 29 acumulados se descartan después de derivar su valor por partido.
2. `fouls_received` queda fuera del catálogo y de FR-030.
3. `foulsPerGame` y `foulCommitted / apps` son alias de `fouls_committed_per_match`, sin entidad de equivalencia.
4. JSON cubre 57 métricas y scraping 45; las 42 compartidas permiten actualización independiente, y cada canal persiste su propia fila de campaña.
5. Cada canal persiste su fila de campaña con la procedencia real; una campaña nueva no sobrescribe las anteriores y una fila con contexto resuelto no se degrada.
6. Las tres métricas no observadas en JSON (`shots_on_target_total`, `second_yellow_total`, `starts_total`) permanecen cubiertas por scraping.

El estado de gate vigente está registrado en `sdd-gates.md`: READY LOCAL fue restablecido tras la confirmación humana del Analyze posterior a la corrección decimal. El checklist conserva las evaluaciones no afectadas; US01 continúa bajo su autorización vigente y las demás historias permanecen sin autorización. Los valores operativos y el algoritmo de ritmo anteriores no alteran el catálogo validado.

## Project Structure

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
- [contracts/http.md](contracts/http.md): endpoints separados de sincronización por scraping y JSON, límite opcional de jugadores y consulta de cobertura por canal.
- [contracts/whoscored.md](contracts/whoscored.md): recorrido headed y quince combinaciones JSON conocidas.
- [quickstart.md](quickstart.md): validación humana posterior a implementación para ambos canales.

## Complexity Tracking

No hay violaciones constitucionales ni excepciones arquitectónicas que justificar.

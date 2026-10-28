# Implementation Plan: Scraping de estadísticas de rendimiento de jugadores

**Branch**: `feat/012-player-statistics-scraping` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-player-statistics-scraping/spec.md`

**Planning Status**: Diseño actualizado el 2026-10-27 para la sesión de Clarify de esa fecha; pendiente de Analyze. Es comportamiento objetivo, no implementación acreditada. Se conserva cobertura 45/57/42, quince combinaciones JSON, fila por campaña/canal y navegador por worker. La rama de la spec es `feat/012-player-statistics-scraping`; la rama activa `fix/012-scraping-parallel-workers` contiene un cambio correctivo/refactor relacionado con esta misma SPEC-012, no una feature independiente. Las autorizaciones históricas están en `sdd-gates.md`, sin autorización para implementar este nuevo alcance. Seguimiento remoto no verificado.

## Summary

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
- [contracts/http.md](contracts/http.md): endpoints separados, scraping sin máximo de jugadores, compatibilidad explícita y cobertura por canal.
- [contracts/whoscored.md](contracts/whoscored.md): recorrido headed y quince combinaciones JSON conocidas.
- [quickstart.md](quickstart.md): validación humana posterior a implementación para ambos canales.

## Complexity Tracking

No hay violaciones constitucionales ni excepciones arquitectónicas que justificar.

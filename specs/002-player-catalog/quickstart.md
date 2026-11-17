# Guía rápida de validación

## Delta de elegibilidad — verificación futura por el usuario

### Inventario definitivo de suites y comandos (2026-11-12)

El repositorio del catálogo aplica `exists` sobre métricas informadas en la fecha máxima del
jugador a listado, conteo, opciones y detalle. Identidad, estadísticas y validaciones de compra y
venta reutilizan ese acceso. Las publicaciones públicas y ambos repositorios de ranking por score
aplican la misma condición. El ranking por precio ya la aplicaba. Las lecturas de portfolio e
historial conservan caminos independientes del detalle filtrado. OpenAPI se actualizó en
`PlayerController`, responsable efectivo de identidad y estadísticas individuales;
`PlayerStatisticsController` mantiene los endpoints de extracción, no el GET individual.

Build backend sin tests observado con resultado satisfactorio durante la implementación. Ese
control no compila ni ejecuta las fuentes de test: la compilación, ejecución y aceptación de las
suites siguientes corresponde exclusivamente al usuario.

Suites del delta y de los consumidores adaptados:

- Catálogo e identidad: `PlayerCatalogQueryRepositoryTest`, `PlayerCatalogQueryServiceTest`,
  `PlayerCatalogServiceTest`, `PlayerCatalogPersistenceTest`, `PlayerControllerTest`,
  `TeamIdentityAndRetirementTest`, `TeamEnrichmentServiceTest`,
  `TeamProtectionPersistenceTest`, `PlayerIdentityE2ETest`.
- Estadísticas y extracción: `PlayerStatisticsQueryServiceTest`,
  `PlayerStatisticsExtractionServiceTest`, `JsonStatisticsUpdateServiceTest`,
  `PlayerSnapshotConsistencyPersistenceTest`, `PlayerStatisticsCrossChannelJourneyE2ETest`,
  `PlayerStatisticsSyncJourneyE2ETest`.
- Mercado, portfolio e historial: `TokenListingVisibilityPersistenceTest`,
  `TokenListingServiceTest`, `BuyOrderServiceTest`, `PortfolioServiceTest`,
  `PortfolioControllerTest`, `OperationHistoryServiceTest`,
  `OperationHistoryControllerTest`, `PlayerTokenEmissionPersistenceTest`,
  `SuperuserMarketRecoveryPersistenceTest`.
- Rankings, cotizaciones e imágenes: `ScoreRankingQueryTest`,
  `ScoreRankingQueryServiceTest`, `QuoteRankingPersistenceTest`, `QuoteQueryServiceTest`,
  `QuoteRecalculationServiceTest`, `QuoteAutomaticRecalculationFailureTest`,
  `QuoteMarketOperationPersistenceTest`, `PlayerImageSynchronizationServiceTest`.

Ejecución **exclusiva del usuario**, desde `backend/`, de las suites afectadas:

```powershell
.\gradlew.bat test --tests '*PlayerCatalogQueryRepositoryTest' --tests '*PlayerCatalogQueryServiceTest' --tests '*PlayerCatalogServiceTest' --tests '*PlayerCatalogPersistenceTest' --tests '*PlayerControllerTest' --tests '*TeamIdentityAndRetirementTest' --tests '*TeamEnrichmentServiceTest' --tests '*TeamProtectionPersistenceTest' --tests '*PlayerStatisticsQueryServiceTest' --tests '*PlayerStatisticsExtractionServiceTest' --tests '*JsonStatisticsUpdateServiceTest' --tests '*PlayerSnapshotConsistencyPersistenceTest' --tests '*TokenListingVisibilityPersistenceTest' --tests '*TokenListingServiceTest' --tests '*BuyOrderServiceTest' --tests '*PortfolioServiceTest' --tests '*PortfolioControllerTest' --tests '*OperationHistoryServiceTest' --tests '*OperationHistoryControllerTest' --tests '*ScoreRankingQueryTest' --tests '*ScoreRankingQueryServiceTest' --tests '*QuoteRankingPersistenceTest' --tests '*QuoteQueryServiceTest' --tests '*QuoteRecalculationServiceTest' --tests '*QuoteAutomaticRecalculationFailureTest' --tests '*QuoteMarketOperationPersistenceTest' --tests '*PlayerImageSynchronizationServiceTest' --tests '*PlayerIdentityE2ETest' --tests '*PlayerStatisticsCrossChannelJourneyE2ETest' --tests '*PlayerStatisticsSyncJourneyE2ETest' --tests '*PlayerTokenEmissionPersistenceTest' --tests '*SuperuserMarketRecoveryPersistenceTest'
```

Para incluir toda la regresión backend, ejecutar la suite completa desde `backend/`:

```powershell
.\gradlew.bat test
```

Escenarios cubiertos por las suites: sin registros; fecha anterior con datos y última vacía; dos
fuentes en la última fecha con una poblada; cero informado; jugador elegible sin score; listado,
búsqueda, conteos, páginas y opciones globales; 404 en identidad y estadísticas por ID no elegible;
200 `NO_DATA` con 60 ausencias para elegible sin snapshot confirmado en el contexto consultado;
exclusión de mercado y ambos rankings aunque existan precios o valuaciones antiguos; compras y
ventas sin movimientos; tenencias e historial de operaciones consultables; sincronización y datos
históricos sin alterar. El agente no ejecuta estos escenarios ni tests, ni declara la suite
aprobada.

## Requisitos previos

- Java 21 y PostgreSQL disponibles.
- Variables de configuración de desarrollo existentes.
- `FOOTBALL_DATA_API_KEY` disponible en el entorno.
- La credencial debe tener acceso a Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`).
- Definir `FOOTBALL_DATA_COMPETITIONS` con las cinco ligas (por ejemplo, `PL,BL1,PD,SA,FL1`), en cualquier orden; una lista incompleta, con duplicados o ligas adicionales debe impedir el arranque de la aplicación.
- Docker disponible para las pruebas de persistencia: `PlayerMigrationTest`,
  `PlayerCatalogPersistenceTest` y las pruebas de Service usan PostgreSQL 18
  aislado mediante Testcontainers y migraciones reales, sin una base local preexistente.
- Un JWT válido de cualquier usuario autenticado, obtenido mediante el login vigente.

## Ejecución y controles

Esta sección corresponde exclusivamente al usuario. El agente preparó los casos y ejecutó solo el
build backend permitido sin tests; no ejecutó tests ni Spotless de forma independiente, y no declara
la suite aprobada.

Desde `backend/`:

```bash
./gradlew test
./gradlew spotlessCheck
```

Para ejecutar el backend con el perfil de desarrollo:

```bash
./gradlew bootRun
```

## Validación manual

1. Invocar `POST /api/players/sync` con un JWT válido de cualquier usuario autenticado; no se requiere un rol adicional.
2. Verificar respuesta exitosa con el resumen de sincronización y logs sin credenciales.
3. Consultar `GET /api/players` y comprobar que devuelve solo jugadores activos, con sus metadatos de página.
4. Verificar que cada jugador expone `id`, `name`, `team`, `league`, `position`, `dateOfBirth`, `nationality` e `imageUrl`, que `id` es el identificador interno de FootballMarket y que no se exponen el estado activo ni las referencias externas.
5. Comprobar los defaults con `GET /api/players` sin parámetros.
6. Comprobar errores con `page=-1`, `size=0` y `size=101`.
7. Ejecutar dos sincronizaciones consecutivas y comprobar que el número de jugadores del catálogo no crece y que cada jugador conserva un único `id` interno, confirmando la resolución por referencia `FOOTBALL_DATA`.
8. Verificar que `dateOfBirth` y `nationality` ausentes en la respuesta del proveedor no borran valores previamente almacenados, y que `imageUrl` no se resuelve por esta feature.
9. Simular la indisponibilidad de Football-Data.org y verificar que `POST /api/players/sync` falla sin modificar jugadores, mientras `GET /api/players` continúa sirviendo el catálogo local.
10. Invocar `POST /api/players/sync` sin un JWT válido y comprobar que responde `401 Unauthorized` sin iniciar la sincronización.
11. Verificar que las respuestas `400` y `502` gestionadas por la aplicación incluyen `timestamp`, `status`, `error`, `code`, `message` y `path`, con `status` igual al código HTTP y `path` igual a la ruta solicitada.
12. Verificar que OpenAPI documenta ambos endpoints, sus parámetros, autenticación, respuestas y ejemplos sin credenciales reales.

13. Ejecutar las pruebas `PlayerMigrationTest` y `PlayerCatalogPersistenceTest`: incluyen
    V2→V3 con datos y base vacía, constraints e índices, conservación de imágenes,
    conflictos concurrentes, recuperación sin duplicar contadores y rollback global
    ante errores técnicos. No aplicar estos escenarios de prueba sobre desarrollo o producción.
14. Ejecutar `FootballDataIntegrationTest`: cubre `Retry-After` numérico/fecha HTTP,
    máximo de tres reintentos y ausencia de reintentos ante otros errores, sin esperas reales.

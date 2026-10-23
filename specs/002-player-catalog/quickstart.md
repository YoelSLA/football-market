# Guía rápida de validación

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

Esta sección corresponde exclusivamente al usuario. El agente preparó los casos,
pero no ejecutó tests, build, Spotless ni generó snippets durante la implementación.

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

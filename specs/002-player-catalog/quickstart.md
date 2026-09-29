# Guía rápida de validación

## Requisitos previos

- Java 21 y PostgreSQL disponibles.
- Variables de configuración de desarrollo existentes.
- `FOOTBALL_DATA_API_KEY` disponible en el entorno.
- La credencial debe tener acceso a Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`).
- Definir `FOOTBALL_DATA_COMPETITIONS` con las cinco ligas (por ejemplo, `PL,BL1,PD,SA,FL1`), en cualquier orden; una lista incompleta, con duplicados o ligas adicionales debe impedir el arranque de la aplicación.
- Docker disponible para los tests de `PlayerCatalogIntegrationTest`, que crean
  PostgreSQL 18 mediante Testcontainers. El resto de los tests de integración existentes
  utiliza la base `football_market_test` configurada en `application-test.yml`.
- En Postman, definir `BASE_URL` y `JWT_TOKEN` con el token obtenido del login.

## Ejecución y controles

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
4. Comprobar los defaults con `GET /api/players` sin parámetros.
5. Comprobar errores con `page=-1`, `size=0` y `size=101`.
6. Simular la indisponibilidad de Football-Data.org y verificar que `POST /api/players/sync` falla sin modificar jugadores, mientras `GET /api/players` continúa sirviendo el catálogo local.
7. Invocar `POST /api/players/sync` sin un JWT válido y comprobar que responde `401 Unauthorized` sin iniciar la sincronización.
8. Verificar que las respuestas `400` y `502` gestionadas por la aplicación incluyen `timestamp`, `status`, `error`, `message` y `path`, con `status` igual al código HTTP y `path` igual a la ruta solicitada.
9. Verificar que OpenAPI y la colección Postman versionada contienen ambos endpoints, sus parámetros, autenticación, respuestas y ejemplos sin credenciales reales.

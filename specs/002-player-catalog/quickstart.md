# Quickstart: Validation Guide

## Prerequisites

- Java 21 y PostgreSQL disponibles.
- Variables de configuración de desarrollo existentes.
- `FOOTBALL_DATA_API_KEY` disponible en el entorno.
- `football-data.competitions` configurada con los códigos de competición autorizados para la clave.

## Execution and Controls

Desde `backend/`:

```bash
./gradlew test
./gradlew spotlessCheck
```

Para ejecutar el backend con el perfil de desarrollo:

```bash
./gradlew bootRun
```

## Manual Validation

1. Invocar `POST /players/sync` usando la autenticación ya requerida por la configuración del backend.
2. Verificar respuesta exitosa con el resumen de sincronización y logs sin credenciales.
3. Consultar `GET /players` y comprobar que devuelve solo jugadores activos, con sus metadatos de página.
4. Comprobar los defaults con `GET /players` sin parámetros.
5. Comprobar errores con `page=-1`, `size=0` y `size=101`.
6. Simular la indisponibilidad de Football-Data.org y verificar que `POST /players/sync` falla sin modificar jugadores, mientras `GET /players` continúa sirviendo el catálogo local.

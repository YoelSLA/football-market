# Quickstart: Validation Guide

## Prerrequisitos
- Java 21 instalado.
- PostgreSQL en ejecución (configurado en `application-dev.yml` / `application-test.yml` según el perfil).

## Validación
Utilizar la colección de Postman versionada:
`postman/auth/User-Authentication.postman_collection.json`

### Pasos
1. Ejecutar el backend con perfil de desarrollo: `./gradlew bootRun --args='--spring.profiles.active=dev'`
2. Configurar la variable de entorno `baseUrl` en Postman (e.g., `http://localhost:8080`).
3. Ejecutar los requests de Registro y Login en la colección.
4. Verificar las respuestas según la documentación de la colección.
5. Capturar el JWT desde el request de Login para futuras peticiones.

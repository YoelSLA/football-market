# Quickstart: Validación de Current Authenticated User

## Prerrequisitos

- Java 21 y Docker disponibles.
- Configuración de desarrollo existente del backend y PostgreSQL accesible para ejecución manual.
- Un cliente HTTP para ejecutar las solicitudes: `curl` o Swagger UI.

El contrato completo está en `specs/003-current-user/contracts/api.md` y el modelo en `specs/003-current-user/data-model.md`.

## Validación automatizada

Desde `backend/`, ejecutar primero las áreas afectadas:

```bash
./gradlew test --tests footballmarket.controllers.AuthenticationControllerTest --tests footballmarket.services.AuthenticationServiceTest --tests footballmarket.security.JwtAuthenticationIntegrationTest --tests footballmarket.e2e.AuthenticationJourneyE2ETest
```

Luego ejecutar los controles completos obligatorios:

```bash
./gradlew test spotlessJavaCheck
```

## Validación ejecutada durante la implementación

- Suite focalizada de autenticación, usuario actual, seguridad y E2E: aprobada.
- Suite completa: 136 tests, sin fallos, errores ni pruebas omitidas.
- `./gradlew test spotlessJavaCheck asciidoctor build --no-configuration-cache`: aprobado.
- Confirmados los contratos de registro exitoso, duplicado y entrada inválida, y de login
  exitoso, credenciales inválidas y entrada inválida.
- Verificados OpenAPI, los snippets `auth-me-success` y `auth-me-unauthorized`, y la solicitud Postman.

> **Nota de retiro 2026-10-14.** El registro anterior conserva los resultados de la implementación de
> esta feature y queda como evidencia histórica: en ese momento la validación incluía la tarea
> `asciidoctor`, la generación de snippets y la solicitud Postman, herramientas retiradas
> posteriormente del proyecto. La ejecución con la caché de configuración habilitada fallaba en la
> serialización de la tarea Asciidoctor con la configuración existente de Gradle 9.6.0 y el plugin
> 4.0.5; esa limitación dejó de aplicar al retirarse el plugin y no requiere ninguna acción.

## Journey exitoso manual

1. Iniciar el backend con el perfil de desarrollo:

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. Registrar un usuario nuevo con un `username` y un email:

   ```bash
   curl -i -X POST "$BASE_URL/api/auth/register" \
     -H 'Content-Type: application/json' \
     -d '{"username":"juan.perez","email":"juan@example.com","password":"password123"}'
   ```

3. Iniciar sesión con las mismas credenciales y guardar el token:

   ```bash
   curl -i -X POST "$BASE_URL/api/auth/login" \
     -H 'Content-Type: application/json' \
     -d '{"email":"juan@example.com","password":"password123"}'
   ```

   Copiar el valor del campo `token` en la variable `JWT_TOKEN`.

4. Consultar la identidad actual con ese token:

   ```bash
   curl -i "$BASE_URL/api/auth/me" -H "Authorization: Bearer $JWT_TOKEN"
   ```

5. Verificar `200 OK` y un JSON con exactamente `id`, `username`, `email` y `role`; el email y el `username` deben coincidir con los valores persistidos del usuario. El `id` es numérico y el `role` vale `USER` para una cuenta creada mediante el registro público.

Alternativamente, los tres pasos se ejecutan desde Swagger UI en `http://localhost:8080/swagger-ui.html`.

## Escenarios de rechazo

Ejecutar la misma solicitud `GET /api/auth/me` en cada condición y verificar `401 Unauthorized` con body vacío:

- sin encabezado Authorization;
- con un Bearer inválido o malformado;
- con un JWT expirado;
- con un JWT criptográficamente válido cuyo usuario ya no existe.

El último caso debe validarse automatizadamente con persistencia controlada; no se deben editar tokens, secretos ni bases de desarrollo manualmente para simularlo.

## Regresión

- `Register` continúa respondiendo `201 Created` sin body para datos válidos.
- `Login` continúa respondiendo `200 OK` con exactamente el campo `token` para credenciales válidas.
- Los errores contractuales existentes de registro y login conservan su formato actual.

## Documentación verificable

- OpenAPI debe mostrar `/api/auth/me` como operación Bearer protegida, con respuestas `200` y `401`.
- `AuthenticationControllerTest` debe afirmar el caso exitoso y cada variante contractual de rechazo, sin afirmar un body inexistente.
- El contrato de `contracts/api.md` y el comportamiento HTTP de `GET /api/auth/me` deben coincidir en los cuatro campos de la respuesta.

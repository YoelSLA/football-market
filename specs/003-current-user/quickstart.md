# Quickstart: Validación de Current Authenticated User

## Prerrequisitos

- Java 21 y Docker disponibles.
- Configuración de desarrollo existente del backend y PostgreSQL accesible para ejecución manual.
- Colección Postman `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`.

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

Generar la documentación, lo que también ejecuta los tests requeridos por la tarea:

```bash
./gradlew asciidoctor
```

## Validación ejecutada durante la implementación

- Suite focalizada de autenticación, usuario actual, seguridad y E2E: aprobada.
- Suite completa: 136 tests, sin fallos, errores ni pruebas omitidas.
- `./gradlew test spotlessJavaCheck asciidoctor build --no-configuration-cache`: aprobado.
- Confirmados los contratos de registro exitoso, duplicado y entrada inválida, y de login
  exitoso, credenciales inválidas y entrada inválida.
- Verificados OpenAPI, los snippets `auth-me-success` y `auth-me-unauthorized`, y la solicitud Postman.

La ejecución con la caché de configuración habilitada falla en la serialización de la tarea
Asciidoctor con la configuración existente de Gradle 9.6.0 y el plugin 4.0.5. Para generar la
documentación, usar `./gradlew asciidoctor --no-configuration-cache`. No se modificaron plugins
ni configuración global del build.

## Journey exitoso manual

1. Iniciar el backend con el perfil de desarrollo:

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. En la carpeta `Auth` de Postman, ejecutar `Register` con un email nuevo.
3. Ejecutar `Login` con las mismas credenciales y guardar el token en `JWT_TOKEN`.
4. Ejecutar `Current user`.
5. Verificar `200 OK` y un JSON con exactamente `id` y `email`; el email debe coincidir con el usuario persistido.

## Escenarios de rechazo

Ejecutar `Current user` en cada condición y verificar `401 Unauthorized` con body vacío:

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
- Spring REST Docs debe incluir el caso exitoso y cada variante contractual de rechazo sin documentar un body inexistente.
- La colección Postman existente debe contener `Current user`; no debe crearse una colección nueva.

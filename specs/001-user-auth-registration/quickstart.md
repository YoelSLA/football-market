# Quickstart: Validation Guide

## Prerrequisitos

- Java 21 instalado.
- PostgreSQL en ejecución y configuración JWT del perfil `dev` disponible.
- Configuración existente del proyecto disponible para el perfil correspondiente.
- Colección de Postman existente ubicada en `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`.

## Validación

La funcionalidad se valida con las solicitudes de registro y login de la colección Postman existente y con los tests automatizados para rutas protegidas.

### Pasos

1. Ejecutar el backend utilizando el perfil de desarrollo:

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. Abrir la colección existente de Postman:

   `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`

3. Configurar la variable `BASE_URL` con la dirección del backend, por ejemplo:

   ```text
   http://localhost:8080
   ```

4. Ejecutar la solicitud `Register` ubicada dentro de la carpeta `Auth` con un email válido y una contraseña de al menos 8 caracteres.

5. Verificar que el registro responda `201 Created` sin body. Un email duplicado debe responder `409 Conflict` con `ErrorResponseDTO`.

6. Ejecutar la solicitud `Login` ubicada dentro de la carpeta `Auth`.

7. Verificar que el login responda `200 OK` con un JWT en el campo `token`.

8. Enviar el JWT como `Authorization: Bearer <token>` a un recurso protegido cuando exista uno. El servidor valida la firma y vigencia del token. Actualmente no hay un endpoint protegido de producción de esta funcionalidad; el flujo se comprueba con una ruta definida solo en `JwtAuthenticationIntegrationTest`.

## Validación de Pruebas

Ejecutar desde `backend/`:

```bash
./gradlew test spotlessJavaCheck
```

`AuthenticationControllerTest` genera snippets con Spring REST Docs. `JwtAuthenticationIntegrationTest` verifica acceso con token válido y rechazo de solicitudes sin token, con firma inválida o con token expirado.

Toda nueva clase de test deberá utilizar explícitamente:

```java
@ActiveProfiles("test")
```

## Restricciones

- No modificar los archivos YAML existentes del proyecto.
- No crear una colección nueva de Postman.
- No incorporar endpoints de producción adicionales a los definidos en la especificación.
- La validación debe cubrir los casos de uso de `spec.md`.

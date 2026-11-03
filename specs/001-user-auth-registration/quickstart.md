# Quickstart: Validation Guide

## Prerrequisitos

- Java 21 instalado.
- PostgreSQL en ejecución y configuración JWT del perfil `dev` disponible.
- Configuración existente del proyecto disponible para el perfil correspondiente.
- Un cliente HTTP para ejecutar las solicitudes: `curl` o Swagger UI.

## Validación

La funcionalidad se valida con las solicitudes de registro y login contra el backend en ejecución y
con los tests automatizados para rutas protegidas. El contrato de referencia es
[contracts/api.md](contracts/api.md), y la especificación interactiva se publica en `/swagger-ui.html`
y `/v3/api-docs` a través de Springdoc OpenAPI.

### Pasos

1. Ejecutar el backend utilizando el perfil de desarrollo:

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. Abrir Swagger UI en `http://localhost:8080/swagger-ui.html`, o definir la variable `BASE_URL` con
   la dirección del backend para usarla en las solicitudes siguientes, por ejemplo:

   ```text
   http://localhost:8080
   ```

4. Ejecutar `POST /api/auth/register` con un nombre de usuario válido, un email válido y una contraseña de al menos 8 caracteres:

   ```bash
   curl -i -X POST "$BASE_URL/api/auth/register" \
     -H 'Content-Type: application/json' \
     -d '{"username":"juan.perez","email":"juan@example.com","password":"password123"}'
   ```

   El nombre de usuario es obligatorio, debe tener entre 3 y 30 caracteres y admitir únicamente
   letras, números, punto, guion y guion bajo. Ejemplo válido: `juan.perez`.

5. Verificar que el registro responda `201 Created` sin body. Un email duplicado debe responder `409 Conflict` con `ErrorResponseDTO` y `code` `EMAIL_ALREADY_REGISTERED`; un nombre de usuario duplicado debe responder `409 Conflict` con `code` `USERNAME_ALREADY_TAKEN`. Un nombre de usuario vacío o con formato inválido debe responder `400 Bad Request`.

6. Ejecutar `POST /api/auth/login`. El inicio de sesión continúa usando **correo electrónico y
   contraseña**. El nombre de usuario no es una credencial: un intento de inicio de sesión con nombre
   de usuario debe ser rechazado con `401 Unauthorized`.

   ```bash
   curl -i -X POST "$BASE_URL/api/auth/login" \
     -H 'Content-Type: application/json' \
     -d '{"email":"juan@example.com","password":"password123"}'
   ```

7. Verificar que el login responda `200 OK` con un JWT en el campo `token`.

8. Enviar el JWT como `Authorization: Bearer <token>` a `GET /api/auth/me`. La respuesta debe
   incluir `id`, `username`, `email` y `role`. El campo `role` debe ser `USER` para un usuario
   registrado mediante el registro público. El `role` es informativo: el servidor nunca toma
   decisiones de autorización a partir de lo que declara el cliente.

9. Enviar el JWT como `Authorization: Bearer <token>` a un recurso protegido cuando exista uno. El servidor valida la firma y vigencia del token. Actualmente no hay un endpoint protegido de producción de esta funcionalidad; el flujo se comprueba con una ruta definida solo en `JwtAuthenticationIntegrationTest`.

## Validación del SUPERUSER

El `SUPERUSER` se inicializa al arrancar la aplicación, y **solo** si se habilita
explicitamente mediante configuración. No se crea por el registro público.

### Variables de entorno

El repositorio no contiene credenciales. Los valores se proporcionan por el entorno de ejecución:

| Propiedad | Variable de entorno | Valor por defecto |
|-----------|---------------------|-------------------|
| `footballmarket.superuser.enabled` | `SUPERUSER_ENABLED` | `false` |
| `footballmarket.superuser.username` | `SUPERUSER_USERNAME` | Sin valor por defecto |
| `footballmarket.superuser.email` | `SUPERUSER_EMAIL` | Sin valor por defecto |
| `footballmarket.superuser.password` | `SUPERUSER_PASSWORD` | Sin valor por defecto |

### Procedimiento

1. Con `SUPERUSER_ENABLED` ausente o en `false`, arrancar la aplicación y comprobar que no se crea
   ningún usuario con rol `SUPERUSER`.
2. Exportar las cuatro variables con valores válidos y arrancar la aplicación. Debe crearse un
   usuario con rol `SUPERUSER`, asociado al email y al nombre de usuario configurados.
3. **Reiniciar la aplicación sin cambiar esas variables.** La inicialización es idempotente: no
   debe crearse un segundo `SUPERUSER` ni modificarse el existente.
4. Arrancar la aplicación con `SUPERUSER_ENABLED=true` pero sin alguna de las variables de
   identidad o contraseña. El arranque debe fallar de forma explícita, sin crear cuentas.
5. Iniciar sesión con el email y la contraseña del `SUPERUSER` configurados. El acceso debe
   concederse por el flujo habitual y `GET /api/auth/me` debe informar el rol `SUPERUSER`.

## Validación de Pruebas

Ejecutar desde `backend/`:

```bash
./gradlew test spotlessJavaCheck
```

`AuthenticationControllerTest` verifica el contrato HTTP de registro, login e identidad actual. `JwtAuthenticationIntegrationTest` verifica acceso con token válido y rechazo de solicitudes sin token, con firma inválida o con token expirado.

Pruebas agregadas por la ampliación del nombre de usuario y los roles:

| Prueba | Verifica |
|--------|----------|
| `UserTest` | Invariantes de nombre de usuario, longitud, formato y rol por defecto. |
| `AuthenticationServiceTest` | Normalización, duplicados, asignación de `USER` y rechazo del login con nombre de usuario. |
| `UserRoleMigrationTest` | Relleno de los usuarios existentes, unicidad del nombre de usuario y rechazo de un segundo `SUPERUSER`. |
| `SuperuserProvisioningServiceTest` | Creación, idempotencia, hasheo de la contraseña y fallo por configuración incompleta. |
| `SuperuserInitializationTest` | Arranque sin creación cuando está deshabilitado, creación idempotente cuando está habilitado y fallo de arranque incompleto. |
| `AuthenticationControllerTest` | Contrato HTTP del registro y de `GET /api/auth/me`. |
| `AuthenticationJourneyE2ETest` | Registro con nombre de usuario, login, identidad actual y recurso protegido. |

Toda nueva clase de test deberá utilizar explícitamente:

```java
@ActiveProfiles("test")
```

## Restricciones

- No modificar los archivos YAML existentes del proyecto que no sean estrictamente necesarios para
  declarar las propiedades del `SUPERUSER`. Las propiedades se declaran sin valores por defecto y
  sin credenciales.
- No crear una colección de peticiones HTTP ni un cliente de línea de comandos persistente en el repositorio.
- No incorporar endpoints de producción adicionales a los definidos en la especificación.
- La validación debe cubrir los casos de uso de `spec.md`, incluidas las historias 3 y 4.

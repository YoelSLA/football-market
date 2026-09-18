# Research Report: User Registration and Authentication

## Decisiones y Rationale

- **Decision**: Utilizar Spring Security para proteger rutas y validar JWT Bearer.

- **Rationale**: Spring Security protege las rutas no públicas y valida los tokens mediante OAuth2 Resource Server. El login comprueba las credenciales en `AuthenticationServiceImpl`.

- **Alternatives considered**: No se incorporarán mecanismos de autenticación externos al stack definido por la Constitution.

- **Decision**: Utilizar Spring Security OAuth2 Resource Server y `NimbusJwtDecoder` para validar los JWT firmados con HS256.

- **Rationale**: Spring Security OAuth2 Resource Server forma parte del stack tecnológico definido y está destinado a la validación de tokens JWT dentro de la aplicación.

- **Alternatives considered**: No se incorporó una solución externa para la validación de JWT.

- **Decision**: Utilizar JJWT para generar JWT con la misma clave HMAC que utiliza el decoder.

- **Rationale**: `JWTProvider` firma los tokens con HS256 y `SecurityConfig` comparte la clave con el decoder. Spring Security comprueba firma y vigencia al recibir `Authorization: Bearer <token>`.

- **Decision**: Utilizar BCrypt para el hashing de contraseñas.

- **Rationale**: Las contraseñas deben almacenarse de forma segura y nunca en texto plano. BCrypt permite almacenar las contraseñas mediante un algoritmo de hashing diseñado para este propósito y dificulta ataques de fuerza bruta.

- **Alternatives considered**: Se consideró utilizar Argon2, pero se mantendrá BCrypt como mecanismo de hashing definido para esta funcionalidad.

- **Decision**: Normalizar el email a minúsculas antes de realizar las operaciones de registro y autenticación.

- **Rationale**: Permite mantener una representación consistente del email y facilita el control de unicidad de las cuentas, de acuerdo con `FR-004`.

- **Alternatives considered**: Mantener el email sin normalización, descartado debido al riesgo de inconsistencias en la comparación y control de unicidad.

- **Decision**: Responder `201 Created` sin body al registrar y usar `ErrorResponseDTO` para errores de aplicación.

- **Rationale**: El registro crea un recurso sin devolver representación. `ErrorResponseDTO` unifica `timestamp`, `status`, `error`, `message` y `path`; el manejo de Jakarta Validation toma el primer mensaje disponible.

## Pendientes Resueltos

- **Validación de JWT**: Se utiliza Spring Security OAuth2 Resource Server para comprobar firma y vigencia de los JWT Bearer.

- **Generación y firma de JWT**: `JWTProvider` utiliza JJWT y HS256. `SecurityConfig` define la clave compartida y `NimbusJwtDecoder` la utiliza para validar los tokens.

- **Hashing de contraseñas**: Se utilizará BCrypt para generar y verificar los hashes de las contraseñas.

- **Migraciones de base de datos**: Las modificaciones necesarias del esquema deberán gestionarse mediante Flyway en `backend/src/main/resources/db/migration/`.

- **Organización de código**: `AuthenticationController` está en `footballmarket.controllers`; los DTO en `footballmarket.controllers.dtos.requests` y `.responses`; `UserMapper` en `footballmarket.controllers.mappers`; `GlobalExceptionHandler` en `footballmarket.controllers.exceptions`.

- **Separación de responsabilidades**: El Controller gestionará únicamente responsabilidades HTTP, el Service concentrará la lógica de negocio y el Repository gestionará el acceso a persistencia.

## Resumen de Investigación

Las decisiones técnicas necesarias para implementar el registro y la autenticación se alinean con el stack tecnológico definido por la Constitution y con la arquitectura existente del backend.

La autenticación utiliza `AuthenticationServiceImpl`; JJWT genera los tokens y Spring Security OAuth2 Resource Server los valida. La clave HMAC se configura en `SecurityConfig` y se comparte con `JWTProvider`.

Las contraseñas serán almacenadas utilizando BCrypt y los emails serán normalizados antes de las operaciones de registro y autenticación para mantener consistencia y controlar la unicidad.

Las modificaciones de persistencia que sean necesarias se realizarán mediante Flyway. La implementación mantendrá la separación entre Controller, Service, Repository y Model/Entity, y utilizará los DTOs y mappers definidos para la funcionalidad.

Las decisiones reflejan la implementación actual y los contratos de la API.

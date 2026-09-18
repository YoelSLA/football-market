# Implementation Plan: User Auth & Registration

**Branch**: `001-user-auth-registration` | **Date**: 2026-09-03 | **Spec**: `specs/001-user-auth-registration/spec.md`

**Input**: Especificación de la funcionalidad ubicada en `specs/001-user-auth-registration/spec.md`

**Nota**: Este documento corresponde al plan de implementación de la funcionalidad y se genera a partir de la especificación, la Constitution y la estructura real del repositorio.

## Summary

Implementar el registro y la autenticación de usuarios para Football Market. El registro crea una cuenta con email y contraseña de al menos 8 caracteres y responde `201 Created`. El login devuelve un JWT que Spring Security valida antes de permitir acceso a recursos protegidos.

La implementación deberá integrarse con la arquitectura existente del backend, respetando la separación de responsabilidades definida por la Constitution. La recepción y validación de las solicitudes corresponderá al Controller, la lógica de negocio al Service y el acceso a la persistencia al Repository. Los datos sensibles deberán manejarse de acuerdo con las reglas de seguridad establecidas por la Constitution.

La funcionalidad incluye los endpoints de registro y login, validaciones, respuestas de error mediante `ErrorResponseDTO`, persistencia, pruebas automatizadas y la colección de Postman existente. No se agrega un endpoint protegido de producción: la validación del JWT se verifica mediante una ruta definida solo para tests.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Security, Spring Data JPA, Spring Boot Validation, Flyway, PostgreSQL JDBC Driver, Spring Security OAuth2 Resource Server, Spring Security OAuth2 JOSE y JJWT para generar JWT.

**Storage**: PostgreSQL

**Testing**: JUnit Jupiter, AssertJ, Spring Boot Test, Spring REST Docs y pruebas de integración HTTP con Spring Security.

**Target Platform**: Backend ejecutado sobre JVM.

**Project Type**: Servicio web backend.

**Performance Goals**: No se define un objetivo de rendimiento específico para esta funcionalidad en `spec.md`.

**Constraints**: El Controller se limita a responsabilidades HTTP y delega la lógica de negocio al Service. El acceso a persistencia se realiza mediante Repository. Las contraseñas se almacenan como hash BCrypt y no se exponen en las respuestas. Los tests utilizan el perfil `test`. Los únicos endpoints de producción de esta funcionalidad son `POST /api/auth/register` y `POST /api/auth/login`; las demás rutas quedan protegidas por Spring Security.

**Scale/Scope**: Registro, login, generación y validación de JWT, errores HTTP, persistencia, tests y colección Postman existentes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Idioma**: PASS — Todo el contenido documental del plan debe redactarse en español. Los identificadores técnicos pueden conservar su denominación original.

- **Stack tecnológico**: PASS — La implementación utilizará exclusivamente las tecnologías y versiones establecidas por la Constitution.

- **Arquitectura por capas**: PASS — Se respetará la separación entre Controller, Service, Repository y Model/Entity definida por la Constitution.

- **Responsabilidad del Controller**: PASS — El Controller se limitará al manejo HTTP, validación de entrada, delegación al Service y construcción de respuestas. No contendrá lógica de negocio, hashing de contraseñas, generación de JWT, acceso directo al Repository ni lógica de persistencia.

- **Lógica de negocio**: PASS — La lógica de negocio corresponderá al Service. Las invariantes propias del dominio permanecerán en el Model/Entity cuando corresponda.

- **Persistencia**: PASS — El acceso a datos se realizará mediante Repository, sin acceso directo desde el Controller.

- **Seguridad**: PASS — Las contraseñas se almacenan mediante BCrypt. Spring Security valida la firma y vigencia del JWT en recursos protegidos.

- **DTOs y mapeo**: PASS — Los DTO de request y response representan los contratos HTTP. `controllers/mappers/UserMapper.java` convierte el registro a `User` sin exponer el modelo en la respuesta.

- **Testing**: PASS — Los tests de Controller documentan los casos HTTP mediante Spring REST Docs. La prueba de integración cubre registro, login y acceso protegido con JWT válido, ausente, inválido o expirado. Las clases de test usan `@ActiveProfiles("test")`.

- **Configuración**: PASS — Se reutiliza la configuración JWT existente de los perfiles `dev` y `test`.

- **Alcance**: PASS — La implementación se limitará a los casos de uso especificados en `spec.md`, sin incorporar funcionalidades adicionales fuera del alcance.

- **Postman**: PASS — La colección existente contiene `Auth / Register` y `Auth / Login`.

**Resultado**: PASS — No se identifican violaciones a la Constitution que requieran justificar complejidad adicional.

## Project Structure

### Documentation (this feature)

```text
specs/001-user-auth-registration/

├── plan.md              # Este archivo
├── research.md          # Investigación técnica
├── data-model.md        # Modelo de datos
├── quickstart.md        # Guía de ejecución y validación
├── contracts/           # Contratos de la API
└── tasks.md             # Tareas de implementación
```

### Source Code

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── footballmarket/
│   │   │       ├── config/SecurityConfig.java
│   │   │       ├── controllers/
│   │   │       │   ├── AuthenticationController.java
│   │   │       │   ├── dtos/requests/{RegisterRequestDTO,LoginRequestDTO}.java
│   │   │       │   ├── dtos/responses/{LoginResponseDTO,ErrorResponseDTO}.java
│   │   │       │   ├── exceptions/GlobalExceptionHandler.java
│   │   │       │   └── mappers/UserMapper.java
│   │   │       ├── models/User.java
│   │   │       ├── repositories/UserRepository.java
│   │   │       ├── security/JWTProvider.java
│   │   │       ├── services/AuthenticationService.java
│   │   │       └── services/impl/AuthenticationServiceImpl.java
│   │   └── resources/db/migration/V1__create_users_table.sql
│   └── test/java/footballmarket/
│       ├── controllers/AuthenticationControllerTest.java
│       ├── integration/JwtAuthenticationIntegrationTest.java
│       ├── models/UserTest.java
│       └── services/AuthenticationServiceTest.java
postman/
└── collections/
    └── 34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json
```

**Structure Decision**: La estructura refleja los paquetes reales. El registro responde sin body, por lo que no existe un `RegisterResponseDTO`. La generación y validación del JWT comparten la clave configurada por `SecurityConfig`.

## Endpoints

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | User registration |
| `POST` | `/api/auth/login` | User authentication |

El registro responde `201 Created`; el login responde `200 OK` con `token`. Ambos son públicos.
Las rutas no públicas requieren un JWT válido en `Authorization: Bearer <token>`.
Los errores de aplicación utilizan `ErrorResponseDTO`.

## Database
- `V1__create_users_table.sql` crea la tabla `users` con email único y contraseña persistida como hash BCrypt.

## Testing Strategy
- **Model**: `UserTest` comprueba invariantes de email y contraseña.
- **Service**: `AuthenticationServiceTest` comprueba registro, normalización, hashing, login y errores.
- **Controller**: `AuthenticationControllerTest` comprueba respuestas HTTP y genera snippets con Spring REST Docs.
- **Integración**: `JwtAuthenticationIntegrationTest` comprueba registro, login y acceso protegido con JWT válido, ausente, alterado o expirado. La ruta protegida existe solo en el test.
- **Perfil**: Las clases de test usan `@ActiveProfiles("test")`.

## Postman Integration
- Modify existing `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`.
- La carpeta existente se llama `Auth` y contiene `Register` y `Login`.
- Las solicitudes usan `{{BASE_URL}}/api/auth/register` y `{{BASE_URL}}/api/auth/login`.

## Complexity Tracking

No se identifican violaciones a la Constitution que requieran justificar complejidad adicional.

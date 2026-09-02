# Implementation Plan: User Registration and Authentication

**Branch**: `001-user-auth-registration` | **Date**: 2026-09-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-user-auth-registration/spec.md`

## Summary

Implementación de un sistema de registro y autenticación de usuarios. Los usuarios podrán crear cuentas proporcionando un email (normalizado a minúsculas) y una contraseña (almacenada mediante hashing seguro). La autenticación se realizará mediante JWT y Spring Security. Se seguirá una arquitectura por capas: Controller -> Service -> Repository -> PostgreSQL, asegurando que los passwords nunca se expongan ni se almacenen en texto plano.

## Technical Context

**Language/Version**: Java 21

**Gradle**: 8.14+

**Primary Dependencies**:
- Spring Boot 4.1.1 (managed)
- Spring Security 7.0.x (managed by SB 4.1.1)
- Spring Data JPA (managed by SB 4.1.1)
- PostgreSQL JDBC Driver (managed by SB 4.1.1)
- Spring Security OAuth2 Resource Server (managed by SB 4.1.1)
- Spring Security OAuth2 JOSE (managed by SB 4.1.1)
- Spring Boot Validation (managed by SB 4.1.1)
- Jakarta Validation 3.1.1
- Flyway (version explicitly defined in build.gradle)

**Database**: PostgreSQL 18.3

**Security**:
- Password Hashing: BCrypt using `BCryptPasswordEncoder` from Spring Security.
- JWT: Nimbus JOSE + JWT (via Spring Security OAuth2 JOSE).

**DTO Validation**:
- Jakarta Validation 3.1.1.
- DTOs implemented as Java records.
- Validation via `jakarta.validation.*` annotations.
- HTTP validation via Spring Boot Validation.
- DTO request validation handles email and password rules (lowercase normalization, etc.).

**Constraints**: El email debe ser único y normalizado (lowercase). El password debe almacenarse como un hash seguro (BCrypt). No exponer passwords ni hashes en responses o logs.

**Scale/Scope**: Sistema inicial de gestión de usuarios; base para futuras funcionalidades protegidas.

## Testing

**Dependencies**:
- JUnit Jupiter 5.14.3
- Mockito 5.20.0
- AssertJ 3.27.6
- Spring Boot Test 4.1.1
- Testcontainers 2.0.3
- PostgreSQL Testcontainers module 2.0.3

**Constraints**:
- Assertions MUST use AssertJ exclusively.
- DO NOT use `org.junit.jupiter.api.Assertions`.
- DO NOT use Hamcrest.
- DO NOT introduce any other assertion library.

**Scope**: Unit tests for Model/Entity, Service, and Controller; Integration tests as defined in the feature.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principio 1 (Separación de responsabilidades)**: Se garantiza mediante el uso de la arquitectura por capas definida (Controller -> Service -> Repository).
- **Principio 2 (Lógica de negocio)**: La lógica de negocio de registro y autenticación pertenece al Service. El Model/Entity puede contener sus propias validaciones relacionadas con su estado e invariantes. Las validaciones propias del Model/Entity no deben trasladarse obligatoriamente al Service si pertenecen naturalmente al estado o invariantes de la entidad. No interpretar esto como que el Entity debe contener toda la validación de entrada HTTP.
- **Principio 4 y 5 (Testing)**: Se definieron tests unitarios para Model/Entity, Service, Controller y tests de integración.
- **Principio 8 (Seguridad)**: Se implementará hashing de contraseñas, JWT para sesiones y Spring Security para protección de recursos. No se expondrán datos sensibles.
- **Principio 10 (Documentación)**: Se utilizará OpenAPI/Swagger para documentar los nuevos endpoints de registro y login.

## Endpoints

Los endpoints de registro y login deben definirse explícitamente antes de la implementación en `footballmarket.controller.auth.AuthController`.

### AuthController (Responsabilidades)
- Implementar `POST /api/auth/register` y `POST /api/auth/login`.
- Recibir requests y validar DTOs (utilizando `jakarta.validation`).
- Delegar la lógica de negocio al `Service`.
- Convertir resultados del `Service` a `Response DTO`.
- Devolver HTTP status correctos.
- **Restricción**: NO contener lógica de negocio, hashing de passwords, generación de JWT ni acceso directo al `Repository`.

### Registration

- **POST /api/auth/register**
- **Tipo**: Público.
- **Request DTO**: `footballmarket.controller.auth.dto.RegisterRequest` (Contiene email y password).
- **Response DTO**: `footballmarket.controller.auth.dto.RegisterResponse` (Información del usuario registrado, sin password/hash).
- **Validaciones**:
    - Email: Formato válido, normalizado a lowercase antes de persistir.
    - Password: Validado según especificación y almacenado como BCrypt hash.
- **Comportamiento**:
    - Email duplicado: Manejo de error específico (e.g., 409 Conflict).
- **HTTP Status**: 201 Created (éxito), 400 Bad Request (validación), 409 Conflict (duplicado).

### Login

- **POST /api/auth/login**
- **Tipo**: Público.
- **Request DTO**: `footballmarket.controller.auth.dto.LoginRequest` (Contiene email y password).
- **Response DTO**: `footballmarket.controller.auth.dto.LoginResponse` (JWT firmado).
- **Validaciones**:
    - Email: Normalizado a lowercase antes de buscar.
    - Password: Verificación mediante `BCryptPasswordEncoder`.
- **Comportamiento**:
    - Credenciales correctas: Genera y devuelve un JWT.
    - Autenticación stateless.
    - Acceso a endpoints protegidos: Esquema Bearer.
    - Nunca devolver password o hash.
- **HTTP Status**: 200 OK (éxito), 401 Unauthorized (credenciales incorrectas).

## Project Structure

### Documentation (this feature)

```text
specs/001-user-auth-registration/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (generated later)
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/footballmarket/
│   │   │   ├── controller/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── RegisterRequest.java
│   │   │   │   │       ├── RegisterResponse.java
│   │   │   │   │       ├── LoginRequest.java
│   │   │   │   │       └── LoginResponse.java
│   │   │   │   └── mapper/          # Mappers (DTO <-> Entity)
│   │   │   ├── service/         # Lógica de negocio y Auth
│   │   │   ├── repository/      # Persistencia (JPA)
│   │   │   ├── model/           # Entities
│   │   │   ├── security/        # Configuración de Spring Security y JWT
│   │   │   └── config/          # Configuraciones generales
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── db/migration/    # Migraciones de base de datos
│   └── test/
│       └── java/footballmarket/
│           ├── controller/
│           │   └── auth/        # AuthController tests
│           ├── service/
│           ├── model/
│           └── integration/
├── postman/
│   └── auth/
│       └── User-Authentication.postman_collection.json
```

**Structure Decision**: Se adopta una estructura de backend Spring Boot estándar, organizando el código por capas (`footballmarket/`). Los DTOs de autenticación se implementan como Java records.

## Postman Collection

Se creará una colección de Postman versionada en el repositorio: `postman/auth/User-Authentication.postman_collection.json`.

### Configuración
- Uso de variable de entorno `{{baseUrl}}` para definir el host (no hardcodear URLs).

### Requests Requeridas
- **Registration**: Registro exitoso, email inválido, password vacío, email duplicado.
- **Login**: Autenticación exitosa, password incorrecto, email no registrado.

### JWT Handling
- Los requests de login permitirán capturar el JWT devuelto.
- La colección configurará automáticamente el uso de este JWT como Bearer Token para futuros requests protegidos.

### Application Configuration

Se requiere configuración separada para desarrollo y testing utilizando perfiles de Spring:

- `application.yml`: Configuración base común.
- `application-dev.yml`: Configuración para desarrollo, base de datos de desarrollo.
- `application-test.yml`: Configuración para testing, base de datos de testing.

**Database Setup**:
- Se utilizará el mismo clúster de PostgreSQL, pero con dos bases de datos independientes.
- Las configuraciones de los perfiles `dev` y `test` garantizarán que los datos no se mezclen.
- **No hardcodear credenciales**: Utilizar variables de entorno para passwords y otros secretos.


## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

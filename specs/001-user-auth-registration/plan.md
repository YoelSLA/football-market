# Implementation Plan: User Auth & Registration

**Branch**: `001-user-auth-registration` | **Date**: 2026-09-03 | **Spec**: `specs/001-user-auth-registration/spec.md`

**Input**: Especificación de la funcionalidad ubicada en `specs/001-user-auth-registration/spec.md`

**Nota**: Este documento corresponde al plan de implementación de la funcionalidad y se genera a partir de la especificación, la Constitution y la estructura real del repositorio.

## Summary

Implementar el registro y la autenticación de usuarios para Football Market. La funcionalidad permitirá que nuevos usuarios creen una cuenta mediante email y contraseña, y que los usuarios registrados puedan autenticarse mediante sus credenciales.

La implementación deberá integrarse con la arquitectura existente del backend, respetando la separación de responsabilidades definida por la Constitution. La recepción y validación de las solicitudes corresponderá al Controller, la lógica de negocio al Service y el acceso a la persistencia al Repository. Los datos sensibles deberán manejarse de acuerdo con las reglas de seguridad establecidas por la Constitution.

La funcionalidad estará limitada a los casos de uso definidos en `spec.md`, incluyendo los endpoints de registro y login, sus validaciones, manejo de errores, persistencia necesaria, pruebas automatizadas y la correspondiente actualización de la colección de Postman existente.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Security, Spring Data JPA, Spring Boot Validation, Flyway, PostgreSQL JDBC Driver, Spring Security OAuth2 Resource Server y Spring Security OAuth2 JOSE.

**Storage**: PostgreSQL

**Testing**: JUnit Jupiter, Mockito, AssertJ, Spring Boot Test y Testcontainers con PostgreSQL.

**Target Platform**: Backend ejecutado sobre JVM.

**Project Type**: Servicio web backend.

**Performance Goals**: No se define un objetivo de rendimiento específico para esta funcionalidad en `spec.md`.

**Constraints**: La implementación debe respetar la arquitectura y las dependencias definidas por la Constitution. El Controller debe limitarse a responsabilidades HTTP y delegar la lógica de negocio al Service. El acceso a persistencia debe realizarse mediante Repository. Las contraseñas deben almacenarse de forma segura y nunca exponerse mediante las respuestas de la API. Los tests nuevos deben utilizar explícitamente el perfil `test`. No se permite modificar los archivos YAML existentes ni crear archivos YAML alternativos para esta funcionalidad. La implementación debe limitarse a los endpoints `POST /api/auth/register` y `POST /api/auth/login` definidos por la especificación.

**Scale/Scope**: La funcionalidad se limita al registro y autenticación de usuarios definidos en `spec.md`, incluyendo las validaciones correspondientes, manejo de errores, persistencia, generación de JWT, pruebas automatizadas y actualización de la colección Postman existente.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Idioma**: PASS — Todo el contenido documental del plan debe redactarse en español. Los identificadores técnicos pueden conservar su denominación original.

- **Stack tecnológico**: PASS — La implementación utilizará exclusivamente las tecnologías y versiones establecidas por la Constitution.

- **Arquitectura por capas**: PASS — Se respetará la separación entre Controller, Service, Repository y Model/Entity definida por la Constitution.

- **Responsabilidad del Controller**: PASS — El Controller se limitará al manejo HTTP, validación de entrada, delegación al Service y construcción de respuestas. No contendrá lógica de negocio, hashing de contraseñas, generación de JWT, acceso directo al Repository ni lógica de persistencia.

- **Lógica de negocio**: PASS — La lógica de negocio corresponderá al Service. Las invariantes propias del dominio permanecerán en el Model/Entity cuando corresponda.

- **Persistencia**: PASS — El acceso a datos se realizará mediante Repository, sin acceso directo desde el Controller.

- **Seguridad**: PASS — Las contraseñas deberán almacenarse mediante hashing seguro y nunca podrán exponerse en las respuestas. La autenticación y generación de JWT deberán utilizar los mecanismos establecidos por la Constitution.

- **DTOs y mapeo**: PASS — Los DTOs de request y response se utilizarán como contratos de la API y el mapeo correspondiente se realizará mediante los componentes ubicados en `controller/mapper`, evitando exponer directamente las entidades.

- **Testing**: PASS — Se implementarán las pruebas correspondientes a las capas afectadas. Cada nueva clase de test deberá utilizar explícitamente `@ActiveProfiles("test")`.

- **Configuración**: PASS — No se modificarán los archivos YAML existentes ni se crearán archivos YAML alternativos para esta funcionalidad.

- **Alcance**: PASS — La implementación se limitará a los casos de uso especificados en `spec.md`, sin incorporar funcionalidades adicionales fuera del alcance.

- **Postman**: PASS — Se modificará la colección existente de Postman para incorporar los endpoints de registro y login, sin crear una colección nueva.

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
│   │   │       ├── controller/
│   │   │       │   ├── auth/
│   │   │       │   │   └── AuthController.java
│   │   │       │   ├── dtos/
│   │   │       │   │   ├── requests/
│   │   │       │   │   │   ├── RegisterRequest.java
│   │   │       │   │   │   └── LoginRequest.java
│   │   │       │   │   └── responses/
│   │   │       │   │       ├── RegisterResponse.java
│   │   │       │   │       └── LoginResponse.java
│   │   │       │   └── mapper/
│   │   │       │       └── UserMapper.java
│   │   │       │
│   │   │       ├── models/
│   │   │       │   ├── [entidades/modelos involucrados]
│   │   │       │   └── exceptions/
│   │   │       │       └── [excepciones necesarias]
│   │   │       │
│   │   │       ├── repositories/
│   │   │       │   └── [repositories involucrados]
│   │   │       │
│   │   │       └── services/
│   │   │           ├── AuthService.java
│   │   │           ├── impl/
│   │   │           │   └── AuthServiceImpl.java
│   │   │           └── exceptions/
│   │   │               └── [excepciones necesarias]
│   │   │
│   │   └── resources/
│   │       └── [archivos de configuración existentes, sin modificación]
│   │
│   └── test/
│       ├── java/
│       │   └── footballmarket/
│       │       ├── controller/
│       │       │   └── auth/
│       │       ├── services/
│       │       ├── models/
│       │       ├── repositories/
│       │       └── integration/
│       │
│       └── resources/
│           └── application-test.yml
│
└── [estructura adicional existente]
    
postman/
└── collections/
    └── 34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json
```

**Structure Decision**: Se utilizará la estructura de backend existente del repositorio. La funcionalidad se implementará respetando la separación de responsabilidades establecida por la Constitution. El Controller de autenticación se ubicará en controller/auth, los DTOs en controller/dtos/requests y controller/dtos/responses, y el mapeo en controller/mapper. La lógica de autenticación se implementará mediante AuthService y AuthServiceImpl, mientras que el acceso a persistencia se realizará mediante los Repositories correspondientes. Las excepciones se ubicarán en los paquetes models/exceptions o services/exceptions según la responsabilidad que representen. Las pruebas se organizarán según las capas involucradas dentro de src/test/java. La colección existente de Postman será modificada para incorporar las solicitudes de registro y login.

## Endpoints

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | User registration |
| `POST` | `/api/auth/login` | User authentication |

## Database
- Flyway migrations are required to add/update user table structure to support storage of hashed passwords if not already present.

## Testing Strategy
- **Unit Tests**: Test Service logic and Mapping logic.
- **Controller Tests**: Test HTTP endpoints with mocked Services.
- **Integration Tests**: Test Registration/Login flows with Testcontainers/PostgreSQL.
- **Test Profile**: Every new test class must include `@ActiveProfiles("test")`.

## Postman Integration
- Modify existing `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`.
- Add `auth` folder.
- Add `register` and `login` requests using `{{baseUrl}}/api/auth/...`.

## Complexity Tracking

No se identifican violaciones a la Constitution que requieran justificar complejidad adicional.

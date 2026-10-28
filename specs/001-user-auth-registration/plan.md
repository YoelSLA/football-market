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

---

# Implementation Plan: Ampliación — Nombre de usuario y roles

**Branch**: `feat/001-user-roles-username` | **Fecha**: 2026-10-14 | **Spec**: `specs/001-user-auth-registration/spec.md`

> Documento de ampliación aprobado el 2026-10-14. Las secciones anteriores describen la implementación
> original y se conservan sin cambios, igual que el estado original de `tasks.md`.

## Summary

Incorporar al agregado `User` un nombre de usuario público y único, y un rol de dominio con dos
valores, `USER` y `SUPERUSER`. El registro público asigna siempre `USER`; el `SUPERUSER` es una
cuenta singular del dominio de mercado que se crea mediante un mecanismo interno de arranque,
idempotente y configurado por el entorno. La ampliación no modifica el JWT ni el inicio de sesión.

## Technical Context

Sin cambios respecto de la sección original. Se reutilizan Java 21, Spring Boot, Spring Security,
Spring Data JPA, Jakarta Validation, Flyway y PostgreSQL. No se incorporan dependencias nuevas.

**Constraints de la ampliación**: la configuración del `SUPERUSER` proviene exclusivamente del
entorno y no admite valores por defecto en el repositorio; el repositorio no contiene credenciales;
la inicialización está deshabilitada por defecto; `username` es obligatorio e inmutable; el rol no
se recibe del cliente; toda autorización futura se evalúa en el backend.

## Constitution Check (ampliación)

- **Idioma**: PASS — Documentación y código en español; identificadores técnicos sin traducir.
- **Stack tecnológico**: PASS — Sin dependencias nuevas.
- **Arquitectura por capas**: PASS — Invariantes de `username` y `role` en el Model; unicidad,
  normalización, hashing e idempotencia en el Service; acceso a datos en el Repository; HTTP en el Controller.
- **Responsabilidad del Controller**: PASS — Sin hashing, sin persistencia, sin lógica de roles.
- **Lógica de negocio**: PASS — La asignación del rol es una invariante del agregado.
- **Persistencia**: PASS — Solo Repository; las migraciones son versionadas y no modifican las históricas.
- **Seguridad**: PASS — Contraseña del `SUPERUSER` hasheada y provista por el entorno; sin secretos en el repositorio; arranque fallido ante configuración incompleta.
- **DTOs y mapeo**: PASS — `RegisterRequestDTO` no contiene rol; `CurrentUserResponseDTO` expone rol y nombre de usuario; `UserMapper` mantiene el mapeo estático sin estado.
- **Testing**: PASS — Cada categoría afectada recibe pruebas propias según [testing del backend](../../../docs/backend/testing.md); perfil `test` y Testcontainers.
- **Configuración**: PASS — Propiedades con validación; sin valores por defecto.
- **Alcance**: PASS — No se implementa mercado, autorización por rol ni modificación del nombre de usuario.
- **Postman**: PASS — Se actualiza la colección existente, no se crea una nueva.

**Resultado**: PASS — Sin violaciones a la Constitution.

## Cambios de estructura

Archivos nuevos:

```text
backend/src/main/java/footballmarket/
├── models/enums/UserRole.java
├── models/exceptions/UsernameEmptyException.java
├── models/exceptions/UsernameInvalidException.java
├── services/exceptions/UsernameAlreadyRegisteredException.java
├── services/SuperuserProvisioningService.java
├── services/impl/SuperuserProvisioningServiceImpl.java
└── config/SuperuserProperties.java

backend/src/main/resources/db/migration/
├── V6__add_username_and_role_to_users.sql
└── V7__restrict_single_superuser.sql

backend/src/test/java/footballmarket/
├── repositories/UserRoleMigrationTest.java
├── services/SuperuserProvisioningServiceTest.java
└── config/SuperuserInitializationTest.java

specs/001-user-auth-registration/checklists/
└── requirements-amendment.md
```

Archivos modificados: `models/User.java`, `repositories/UserRepository.java`,
`controllers/dtos/requests/RegisterRequestDTO.java`, `controllers/dtos/responses/CurrentUserResponseDTO.java`,
`controllers/mappers/UserMapper.java`, `services/AuthenticationService.java`,
`services/impl/AuthenticationServiceImpl.java`, `controllers/AuthenticationController.java`,
`controllers/exceptions/GlobalExceptionHandler.java`, `application.yml`, `application-test.yml`,
la colección Postman y las pruebas existentes de la funcionalidad.

**Estructura sin cambios**: `security/JWTProvider.java`, `config/SecurityConfig.java`,
`security/PersistedUserJwtAuthenticationConverter.java`, `controllers/dtos/requests/LoginRequestDTO.java`,
`controllers/dtos/responses/LoginResponseDTO.java`, `services/UserService.java` y su implementación.
La decisión se sustenta en [research.md](research.md).

## Migraciones

- `V6__add_username_and_role_to_users.sql`: añade `username` y `role`, rellena los usuarios
  existentes con un nombre de usuario determinista derivado de sus datos actuales y con el rol
  `USER`, y declara `NOT NULL`, unicidad e índice. Preserva `id`, `email` y `password`.
- `V7__restrict_single_superuser.sql`: índice único parcial sobre `role = 'SUPERUSER'`.

`V1` a `V5` no se modifican. `ddl-auto: validate` exige que cada columna declarada en el modelo
exista en el esquema, por lo que toda migración de esta ampliación se ejecuta antes del arranque de
la aplicación y las pruebas de integración la validan con migraciones reales sobre Testcontainers.

## Endpoints

| Method | Path | Cambio |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | **Incompatible**: exige `username`. Sigue sin body en `201`. |
| `POST` | `/api/auth/login` | Sin cambios. |
| `GET` | `/api/auth/me` | **Aditivo**: suma `username` y `role`. |

## Testing Strategy

- **Model** (`UserTest`): username vacío, con formato inválido, por debajo y por encima del rango de longitud, válido; rol `USER` por defecto; ausencia de cambios parciales tras un rechazo.
- **Service** (`AuthenticationServiceTest`): normalización a minúsculas, duplicado exacto y duplicado por mayúsculas y minúsculas, asignación de `USER`, rechazo del login con nombre de usuario.
- **Service** (`SuperuserProvisioningServiceTest`): creación cuando no existe, idempotencia cuando ya existe, asignación de `SUPERUSER`, hasheado de la contraseña, fallo ante configuración incompleta.
- **Repository** (`UserRoleMigrationTest`): relleno de `username` y `role` desde el estado previo, unicidad del nombre de usuario, rechazo de un segundo `SUPERUSER` por el índice único parcial.
- **Controller** (`AuthenticationControllerTest`): `201` con `username`, `400` por username vacío o inválido, `409` por username duplicado, `role` enviado en el registro ignorado, `GET /me` con los cuatro campos, snippets REST Docs.
- **Security** (`JwtAuthenticationIntegrationTest`): login y acceso protegido sin cambios; el rol no altera la validación del JWT.
- **E2E** (`AuthenticationJourneyE2ETest`): registro con `username`, login, identidad actual y recurso protegido.
- **Config** (`SuperuserInitializationTest`): arranque sin creación cuando está deshabilitado, creación idempotente cuando está habilitado, fallo de arranque con configuración incompleta.

## Dependencias con funcionalidades futuras

- El mercado de tokens necesita identificar al `SUPERUSER` como contraparte de las compras: el
  mecanismo de provisión entrega esa identificación sin requerir un endpoint de consulta.
- El mercado necesitará autorización por rol. Esta ampliación no la implementa; el rol persistido
  queda disponible para esas reglas cuando existan endpoints que las necesiten.
- La cotización y la distribución inicial de tokens no se incluye aquí. Ningún componente de esta
  ampliación asume su existencia.

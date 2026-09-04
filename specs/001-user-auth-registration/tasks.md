# Tasks: User Registration and Authentication

**Input**: Documentos de diseño ubicados en `/specs/001-user-auth-registration/`

**Prerequisites**: plan.md (obligatorio), spec.md (obligatorio para las historias de usuario), research.md, data-model.md, contracts/

**Tests**: Se requieren pruebas automatizadas con JUnit Jupiter, Mockito, AssertJ, Spring Boot Test y Testcontainers con PostgreSQL, de acuerdo con la estrategia de pruebas definida en plan.md.

**Language**: Todo el contenido descriptivo y documental de este archivo debe redactarse en español. Las keywords de Spec-Kit, identificadores técnicos, nombres de clases, métodos, paquetes, endpoints, tecnologías y otros términos técnicos deben conservar su denominación original.

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: Verificación de las dependencias e infraestructura existente necesarias para implementar la funcionalidad.

- [ ] T001 Verificar que las dependencias requeridas por la funcionalidad ya estén disponibles en backend/build.gradle.kts, sin incorporar dependencias fuera de la Constitution.

---

## Phase 2: Foundational (Prerrequisitos bloqueantes)

**Purpose**: Infraestructura central que MUST estar completa antes de implementar cualquier historia de usuario.

**⚠️ CRITICAL**: No se puede comenzar el trabajo de ninguna historia de usuario hasta que esta fase esté completa.

- [ ] T002 Crear las migraciones de Flyway necesarias para la funcionalidad de usuarios en `backend/src/main/resources/db/migration/`. Los archivos deben seguir el formato `V<numero>__<descripcion>.sql`, donde `V` es obligatorio, `<numero>` corresponde a la versión incremental de la migración y `<descripcion>` debe estar escrita completamente en minúsculas, utilizando `_` como separador entre palabras. Por ejemplo: `V1__init_schema.sql`. Las migraciones deben incluir la estructura requerida para almacenar usuarios y contraseñas hasheadas, sin modificar la configuración existente de PostgreSQL.
- [ ] T003 Implementar la entidad `User` en `backend/src/main/java/footballmarket/models/User.java`, utilizando como referencia y fuente de verdad el modelo de datos definido en `data-model.md`.

- [ ] T004 [P] Configurar el repositorio base para Usuario en backend/src/main/java/footballmarket/repositories/UserRepository.java

- [ ] T005 [P] Configurar Spring Security para el hashing y la autenticación en backend/src/main/java/footballmarket/config/SecurityConfig.java

- [ ] T006 Configurar el manejo de excepciones global en backend/src/main/java/footballmarket/exceptions/GlobalExceptionHandler.java

**Checkpoint**: Base lista - la implementación de las historias de usuario puede comenzar

## Phase 3: User Story 1 - Registro de usuario (Priority: P1) 🎯 MVP

**Goal**: Permitir el registro de nuevos usuarios mediante email y contraseña.

**Independent Test**: Enviar una solicitud de registro con datos válidos y verificar que el usuario sea creado correctamente en la base de datos.

### Tests for User Story 1

- [ ] T007 [US1] Implementar tests unitarios del modelo `User`, validando las restricciones definidas en `data-model.md` para casos válidos e inválidos.

- [ ] T008 [US1] Implementar tests unitarios de `AuthServiceImpl` para el registro, cubriendo casos positivos, negativos.

- [ ] T009 [US1] Implementar tests unitarios de `AuthController` para el endpoint de registro, cubriendo respuestas exitosas, errores de validación y casos negativos.

- [ ] T010 [US1] Implementar el test de integración del registro en `backend/src/test/java/footballmarket/integration/auth/RegistrationIntegrationTest.java`, verificando el flujo completo de registro y la persistencia del usuario en la base de datos.

### Implementation for User Story 1

- [ ] T011 [US1] Crear `RegisterRequest` y `RegisterResponse` en `backend/src/main/java/footballmarket/controller/auth/dtos/requests/RegisterRequest.java` y `backend/src/main/java/footballmarket/controller/auth/dtos/responses/RegisterResponse.java`, utilizando como referencia el contrato de API definido en `contracts/api.md`.

- [ ] T012 [US1] Implementar `UserMapper` en `backend/src/main/java/footballmarket/controller/mapper/UserMapper.java` para realizar, según las responsabilidades definidas en `plan.md`, la transformación entre los DTOs de registro y la entidad `User`, permitiendo convertir del DTO al modelo y del modelo al DTO.

- [ ] T013 [US1] Implementar la interfaz `AuthService` en `backend/src/main/java/footballmarket/services/AuthService.java`, definiendo las operaciones necesarias para el registro de usuarios.

- [ ] T014 [US1] Implementar `AuthServiceImpl` en `backend/src/main/java/footballmarket/services/impl/AuthServiceImpl.java` para gestionar la lógica de registro, incluyendo validación de existencia del email, hashing de la contraseña y persistencia del usuario.

- [ ] T015 [US1] Implementar `AuthController` en `backend/src/main/java/footballmarket/controller/auth/AuthController.java` para exponer el endpoint `POST /api/auth/register`, realizando únicamente la validación de entrada y la construcción de la respuesta, y delegando la lógica de negocio al Service, según las responsabilidades definidas en `plan.md`.

**Checkpoint**: La historia de usuario 1 está totalmente funcional y es testeable de forma independiente.

## Phase 4: User Story 2 - Inicio de sesión de usuario (Priority: P1)

**Goal**: Permitir el inicio de sesión de usuarios registrados mediante email y contraseña.

**Independent Test**: Enviar una solicitud de inicio de sesión con credenciales válidas y verificar la recepción de un JWT válido.

### Tests for User Story 2

- [ ] T016 [US2] Implementar tests unitarios de `AuthServiceImpl` para el inicio de sesión, cubriendo casos positivos, negativos y casos límite.

- [ ] T017 [US2] Implementar tests unitarios de `AuthController` para el endpoint de inicio de sesión, cubriendo respuestas exitosas, errores de validación y casos negativos.

- [ ] T018 [US2] Implementar el test de integración del inicio de sesión en `backend/src/test/java/footballmarket/integration/auth/LoginIntegrationTest.java`, verificando el flujo completo de autenticación y la generación de un JWT válido.

### Implementation for User Story 2

- [ ] T019 [US2] Crear `LoginRequest` y `LoginResponse` en `backend/src/main/java/footballmarket/controller/auth/dtos/requests/LoginRequest.java` y `backend/src/main/java/footballmarket/controller/auth/dtos/responses/LoginResponse.java`, utilizando como referencia el contrato de API definido en `contracts/api.md`.

- [ ] T020 [US2] Extender `AuthService` en `backend/src/main/java/footballmarket/services/AuthService.java` con las operaciones necesarias para el inicio de sesión, según las responsabilidades definidas en `plan.md`.

- [ ] T021 [US2] Extender `AuthServiceImpl` en `backend/src/main/java/footballmarket/services/impl/AuthServiceImpl.java` para gestionar la lógica de inicio de sesión, incluyendo la validación de credenciales y la generación del JWT, según las responsabilidades definidas en `plan.md`.

- [ ] T022 [US2] Extender `AuthController` en `backend/src/main/java/footballmarket/controller/auth/AuthController.java` para exponer el endpoint `POST /api/auth/login`, realizando únicamente la validación de entrada y la construcción de la respuesta, y delegando la lógica de negocio al Service, según las responsabilidades definidas en `plan.md`.

**Checkpoint**: Las historias de usuario 1 y 2 deberían funcionar de forma independiente.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Completar la documentación, los artefactos de prueba y las validaciones finales de la funcionalidad.

- [ ] T023 Actualizar la documentación OpenAPI de los endpoints `POST /api/auth/register` y `POST /api/auth/login` en `backend/src/main/java/footballmarket/controller/auth/AuthController.java`, según los contratos definidos en `contracts/api.md`.

- [ ] T024 Actualizar la colección existente de Postman en `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`, incorporando las solicitudes de registro e inicio de sesión correspondientes a los endpoints implementados.

- [ ] T025 Validar el flujo definido en `quickstart.md`, verificando que las instrucciones permitan ejecutar y probar correctamente la funcionalidad de registro e inicio de sesión.

**Checkpoint**: La funcionalidad de registro e inicio de sesión está documentada, probada mediante la colección de Postman y validada según `quickstart.md`.

## Dependencies & Execution Order

### Phase Dependencies

- Setup -> Foundational -> US1 -> US2 -> Polish

### Parallel Opportunities

- Las tareas de tests unitarios de una misma historia de usuario pueden desarrollarse de forma independiente cuando sus componentes ya estén implementados.
- No se define ejecución paralela entre US1 y US2, ya que la estrategia MVP establece completar y validar US1 antes de comenzar US2.

### MVP Strategy

1. Completar Setup y Foundational.
2. Completar US1 (Registro de usuario).
3. Validar US1 de forma independiente.
4. Completar US2 (Inicio de sesión).
5. Completar Polish.
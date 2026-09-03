# Tasks: User Registration and Authentication

**Input**: Design documents from `/specs/001-user-auth-registration/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests requested (JUnit, Mockito, Testcontainers).

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: Inicialización del proyecto y estructura básica

- [ ] T001 Configurar dependencias del proyecto en backend/build.gradle.kts
- [ ] T002 [P] Configurar herramientas de linting y formateo

---

## Phase 2: Foundational (Prerrequisitos bloqueantes)

**Purpose**: Infraestructura central que MUST be complete antes de implementar CUALQUIER historia de usuario

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Configurar la base de datos PostgreSQL y las migraciones de Flyway en backend/src/main/resources/db/migration/
- [ ] T004 Implementar la entidad Usuario en backend/src/main/java/footballmarket/models/User.java
- [ ] T005 [P] Configurar el repositorio base para Usuario en backend/src/main/java/footballmarket/repositories/UserRepository.java
- [ ] T006 [P] Configurar Spring Security para el hashing y la autenticación en backend/src/main/java/footballmarket/config/SecurityConfig.java
- [ ] T007 Configurar el manejo de excepciones global en backend/src/main/java/footballmarket/exceptions/GlobalExceptionHandler.java

**Checkpoint**: Base lista - la implementación de las historias de usuario puede comenzar

---

## Phase 3: User Story 1 - Registro de usuario (Priority: P1) 🎯 MVP

**Goal**: Permitir el registro de nuevos usuarios mediante email y contraseña.

**Independent Test**: Enviar registro con datos válidos y verificar creación en BD.

### Tests for User Story 1

- [ ] T008 [P] [US1] Integration test for registration in backend/src/test/java/footballmarket/integration/auth/RegistrationIntegrationTest.java

### Implementation for User Story 1

- [ ] T009 [US1] Crear DTOs de registro en backend/src/main/java/footballmarket/controller/auth/dtos/requests/RegisterRequest.java y responses/RegisterResponse.java
- [ ] T010 [US1] Implementar UserMapper en backend/src/main/java/footballmarket/controller/mapper/UserMapper.java
- [ ] T011 [US1] Implementar AuthServiceImpl para el registro en backend/src/main/java/footballmarket/services/impl/AuthServiceImpl.java
- [ ] T012 [US1] Implementar AuthController para el registro en backend/src/main/java/footballmarket/controller/auth/AuthController.java

**Checkpoint**: La historia de usuario 1 está totalmente funcional y es testeable de forma independiente.

---

## Phase 4: User Story 2 - Inicio de sesión de usuario (Priority: P1)

**Goal**: Permitir el inicio de sesión para usuarios registrados mediante credenciales.

**Independent Test**: Iniciar sesión con credenciales válidas y recibir JWT.

### Tests for User Story 2

- [ ] T013 [P] [US2] Integration test for login in backend/src/test/java/footballmarket/integration/auth/LoginIntegrationTest.java

### Implementation for User Story 2

- [ ] T014 [US2] Crear DTOs de inicio de sesión en backend/src/main/java/footballmarket/controller/auth/dtos/requests/LoginRequest.java y responses/LoginResponse.java
- [ ] T015 [US2] Implementar AuthServiceImpl para el login/generación de JWT en backend/src/main/java/footballmarket/services/impl/AuthServiceImpl.java
- [ ] T016 [US2] Implementar AuthController para el login en backend/src/main/java/footballmarket/controller/auth/AuthController.java

**Checkpoint**: Las historias de usuario 1 y 2 deberían funcionar de forma independiente.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan a múltiples historias de usuario

- [ ] T017 Actualización de documentación (OpenAPI) en backend/src/main/java/footballmarket/controller/auth/AuthController.java
- [ ] T018 Actualizar colección de Postman en postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json
- [ ] T019 Ejecutar la validación de quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup -> Foundational -> US1/US2 (Parallel) -> Polish

### Parallel Opportunities

- T002, T005, T006, T008, T013 marked [P] can run in parallel.
- US1 and US2 implementation can be parallelized after Phase 2.

### MVP Strategy

1. Complete Setup and Foundational.
2. Complete US1 (Registration).
3. Validate US1 independently.
4. Complete US2 (Login).
5. Complete Polish.

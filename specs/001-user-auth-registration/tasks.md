# Tasks: User Registration and Authentication

**Input**: Design documents from `/specs/001-user-auth-registration/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/api.md

**Tests**: JUnit 5, Mockito, AssertJ, Testcontainers (Mandatory as per plan.md)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Project initialization and structure

- [ ] T001 Create project structure per implementation plan (package `footballmarket`)
- [ ] T002 Configure `build.gradle` with specified dependencies (Spring Boot 4.1.1, Flyway, etc.)
- [ ] T003 [P] Configure Flyway for DB migrations in `backend/src/main/resources/db/migration/`

---

## Phase 2: Foundational

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Setup Spring Boot application configuration (`application.yml`, `application-dev.yml`, `application-test.yml`)
- [ ] T005 [P] Setup PostgreSQL database for dev and test profiles
- [ ] T006 Implement base security configuration (`SecurityConfig` with `BCryptPasswordEncoder`) in `backend/src/main/java/footballmarket/security/`
- [ ] T007 Create `User` Entity model in `backend/src/main/java/footballmarket/model/`
- [ ] T008 Setup `UserRepository` interface in `backend/src/main/java/footballmarket/repository/`

---

## Phase 3: User Story 1 - Registration (Priority: P1) 🎯 MVP

**Goal**: Implement user registration with email normalization and secure password hashing.

**Independent Test**: Register a new user, verify user in DB, attempt duplicate registration (409).

### Tests for User Story 1

- [ ] T009 [P] [US1] Create unit tests for `User` entity in `backend/src/test/java/footballmarket/model/`
- [ ] T010 [P] [US1] Create unit tests for `AuthService` registration logic in `backend/src/test/java/footballmarket/service/`
- [ ] T011 [P] [US1] Create integration tests for registration in `backend/src/test/java/footballmarket/integration/`

### Implementation for User Story 1

- [ ] T012 [P] [US1] Create `RegisterRequest` record in `backend/src/main/java/footballmarket/controller/auth/dto/`
- [ ] T013 [P] [US1] Create `RegisterResponse` record in `backend/src/main/java/footballmarket/controller/auth/dto/`
- [ ] T014 [US1] Create `UserMapper` for DTO-Entity conversion in `backend/src/main/java/footballmarket/controller/mapper/`
- [ ] T015 [US1] Implement `AuthService` registration logic in `backend/src/main/java/footballmarket/service/`
- [ ] T016 [US1] Implement `AuthController.register` endpoint in `backend/src/main/java/footballmarket/controller/auth/`
- [ ] T017 [US1] Create unit tests for `AuthController` registration endpoint in `backend/src/test/java/footballmarket/controller/auth/`

---

## Phase 4: User Story 2 - Login (Priority: P2)

**Goal**: Implement user login returning a JWT.

**Independent Test**: Login with valid credentials, verify JWT reception. Login with invalid credentials, verify 401.

### Tests for User Story 2

- [ ] T018 [P] [US2] Create unit tests for JWT generation in `AuthService` in `backend/src/test/java/footballmarket/service/`
- [ ] T019 [P] [US2] Create integration tests for login in `backend/src/test/java/footballmarket/integration/`

### Implementation for User Story 2

- [ ] T020 [P] [US2] Create `LoginRequest` record in `backend/src/main/java/footballmarket/controller/auth/dto/`
- [ ] T021 [P] [US2] Create `LoginResponse` record in `backend/src/main/java/footballmarket/controller/auth/dto/`
- [ ] T022 [US2] Implement `AuthService` login and JWT generation logic in `backend/src/main/java/footballmarket/service/`
- [ ] T023 [US2] Implement `AuthController.login` endpoint in `backend/src/main/java/footballmarket/controller/auth/`
- [ ] T024 [US2] Create unit tests for `AuthController` login endpoint in `backend/src/test/java/footballmarket/controller/auth/`

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and tooling.

- [ ] T025 [P] Create Postman collection `postman/auth/User-Authentication.postman_collection.json`
- [ ] T026 [P] Documentation updates and final verification against `quickstart.md`
- [ ] T027 [P] Configure OpenAPI/Swagger documentation using SpringDoc in `backend/src/main/java/footballmarket/config/`

# Tasks: User Registration and Authentication

**Input**: Documentos de diseño ubicados en `/specs/001-user-auth-registration/`

**Prerequisites**: plan.md (obligatorio), spec.md (obligatorio para las historias de usuario), research.md, data-model.md, contracts/

**Tests**: El estado de las tareas refleja las clases de test presentes: `UserTest`, `AuthenticationServiceTest`, `AuthenticationControllerTest` y `JwtAuthenticationIntegrationTest`. La persistencia tiene cobertura indirecta mediante registro seguido de login; no hay una prueba de integración independiente que inspeccione la base de datos.

**Language**: Todo el contenido descriptivo y documental de este archivo debe redactarse en español. Las keywords de Spec-Kit, identificadores técnicos, nombres de clases, métodos, paquetes, endpoints, tecnologías y otros términos técnicos deben conservar su denominación original.

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: Verificación de las dependencias e infraestructura existente necesarias para implementar la funcionalidad.

- [X] T001 Verificar en `backend/build.gradle.kts` las dependencias de Spring Security, OAuth2 Resource Server, JJWT, Spring REST Docs, JUnit y Spring Boot Test.

---

## Phase 2: Foundational (Prerrequisitos bloqueantes)

**Purpose**: Infraestructura central que MUST estar completa antes de implementar cualquier historia de usuario.

**⚠️ CRITICAL**: No se puede comenzar el trabajo de ninguna historia de usuario hasta que esta fase esté completa.

- [X] T002 Crear `backend/src/main/resources/db/migration/V1__create_users_table.sql` con email único y columna para la contraseña hasheada.
- [X] T003 Implementar la entidad `User` en `backend/src/main/java/footballmarket/models/User.java`, utilizando como referencia y fuente de verdad el modelo de datos definido en `data-model.md`.

- [X] T004 [P] Configurar el repositorio base para Usuario en backend/src/main/java/footballmarket/repositories/UserRepository.java

- [X] T005 [P] Configurar BCrypt, rutas públicas y validación de JWT Bearer en `backend/src/main/java/footballmarket/config/SecurityConfig.java`.

- [X] T006 Configurar `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` y `controllers/dtos/responses/ErrorResponseDTO.java` para los errores de aplicación.

**Checkpoint**: Base lista - la implementación de las historias de usuario puede comenzar

## Phase 3: User Story 1 - Registro de usuario (Priority: P1) 🎯 MVP

**Goal**: Permitir el registro de nuevos usuarios mediante email y contraseña.

**Independent Test**: Enviar una solicitud de registro con datos válidos y verificar que el usuario sea creado correctamente en la base de datos.

### Tests for User Story 1

- [X] T007 [US1] Implementar tests unitarios del modelo `User`, validando las restricciones definidas en `data-model.md` para casos válidos e inválidos.

- [X] T008 [US1] Probar registro, normalización, hashing y email duplicado en `AuthenticationServiceTest`.
- [X] T009 [US1] Probar `201 Created`, validaciones y `409 Conflict` en `AuthenticationControllerTest`, con snippets Spring REST Docs.
- [ ] T010 [US1] Agregar una prueba de integración independiente que compruebe explícitamente la persistencia del usuario en la base de datos. No existe hoy una prueba ejecutable con esa verificación directa.

### Implementation for User Story 1

- [X] T011 [US1] Crear `controllers/dtos/requests/RegisterRequestDTO.java`. El registro responde `201 Created` sin body, por lo que no requiere un Response DTO.

- [X] T012 [US1] Implementar `controllers/mappers/UserMapper.java` para convertir `RegisterRequestDTO` a `User`.

- [X] T013 [US1] Definir el registro en `backend/src/main/java/footballmarket/services/AuthenticationService.java`.

- [X] T014 [US1] Implementar el registro en `services/impl/AuthenticationServiceImpl.java`, incluyendo normalización, unicidad, hashing y persistencia.

- [X] T015 [US1] Implementar `POST /api/auth/register` en `controllers/AuthenticationController.java` con respuesta `201 Created` sin body.

**Checkpoint**: La historia de usuario 1 está totalmente funcional y es testeable de forma independiente.

## Phase 4: User Story 2 - Inicio de sesión de usuario (Priority: P1)

**Goal**: Permitir el inicio de sesión de usuarios registrados mediante email y contraseña.

**Independent Test**: Enviar una solicitud de inicio de sesión con credenciales válidas y verificar la recepción de un JWT válido.

### Tests for User Story 2

- [X] T016 [US2] Probar login y errores de credenciales en `AuthenticationServiceTest`.

- [X] T017 [US2] Probar `POST /api/auth/login` en `AuthenticationControllerTest`, con snippets Spring REST Docs.

- [X] T018 [US2] Probar el flujo registro → login → recurso protegido y los JWT ausentes, alterados o expirados en `integration/JwtAuthenticationIntegrationTest.java`.

### Implementation for User Story 2

- [X] T019 [US2] Crear `controllers/dtos/requests/LoginRequestDTO.java` y `controllers/dtos/responses/LoginResponseDTO.java`.

- [X] T020 [US2] Definir login en `services/AuthenticationService.java`.

- [X] T021 [US2] Implementar login en `services/impl/AuthenticationServiceImpl.java` y generación HS256 en `security/JWTProvider.java`.

- [X] T022 [US2] Implementar `POST /api/auth/login` en `controllers/AuthenticationController.java` con respuesta `200 OK` y campo `token`.

**Checkpoint**: Las historias de usuario 1 y 2 deberían funcionar de forma independiente.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Completar la documentación, los artefactos de prueba y las validaciones finales de la funcionalidad.

- [X] T023 Documentar los endpoints con OpenAPI en `controllers/AuthenticationController.java` y con Spring REST Docs en `AuthenticationControllerTest.java`.

- [X] T024 Mantener la colección Postman existente con `Auth / Register` y `Auth / Login`, usando `{{BASE_URL}}`.

- [ ] T025 Ejecutar manualmente el flujo Postman de `quickstart.md` y confirmar sus resultados en un entorno de desarrollo. La guía está actualizada, pero esa ejecución manual no está acreditada en el repositorio.

**Checkpoint**: Los tests automatizados cubren registro, login y validación de JWT. Quedan pendientes la comprobación explícita de persistencia (T010) y la validación manual con Postman (T025).

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

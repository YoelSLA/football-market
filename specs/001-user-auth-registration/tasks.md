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

- [ ] T025 Ejecutar manualmente el flujo de `quickstart.md` contra el backend en desarrollo mediante HTTP directo o Swagger UI, y confirmar sus resultados: registro con `username`, login, identidad actual y acceso protegido. Reformulada el 2026-10-14 al retirar la colección Postman; conserva su identificador y sigue sin acreditar en el repositorio.

**Checkpoint**: Los tests automatizados cubren registro, login y validación de JWT. Quedan pendientes la comprobación explícita de persistencia (T010) y la validación manual del flujo documentado (T025).

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

---

# Phase 6: Ampliación — Nombre de usuario y roles (2026-10-14)

**Purpose**: Incorporar el nombre de usuario y los roles de usuario a la funcionalidad
aprobada, preservando el historial de las fases anteriores.

**Estado previo**: Este bloque se agrega a un `tasks.md` ya aprobado. **No modifica el estado de
ninguna tarea anterior.** Las tareas `T010` y `T025` continúan pendientes y sin creditar. Las
tareas de esta fase comienzan en `- [ ]` y solo se marcarán como completadas con evidencia de
implementación y verificación.

**Rama**: `feat/001-user-roles-username`

**Orden**: `Phase 6` depende de las fases 1 a 5 completadas.

## Phase 6.1: Documentación de la ampliación

- [X] T026 [AMP] Ampliar `spec.md` con las historias de usuario 3 y 4, los requisitos `FR-013` a `FR-031`, los criterios `SC-007` a `SC-012`, el alcance ampliado y la sección `## Amendments`, preservando `FR-001` a `FR-012`.
- [X] T027 [AMP] Ampliar `data-model.md` con los atributos `username` y `role`, las reglas de validación, la restricción de unicidad del `SUPERUSER` y la estrategia de migración de usuarios existentes.
- [X] T028 [AMP] Ampliar `contracts/api.md` con el body de registro actualizado, los nuevos códigos de error y el contrato de `GET /api/auth/me`.
- [X] T029 [AMP] Ampliar `research.md` y `plan.md` con las decisiones, alternativas descartadas, estructura, migraciones, estrategia de pruebas y dependencias futuras.
- [X] T030 [AMP] Crear `checklists/requirements-amendment.md` como checklist independiente, sin modificar `checklists/requirements.md`.
- [X] T031 [AMP] Actualizar `quickstart.md` con el body de registro y la configuración de propiedades del `SUPERUSER` para la validación manual.

## Phase 6.2: Persistencia

- [X] T032 [AMP] Crear `V6__add_username_and_role_to_users.sql` con las columnas `username` y `role`, el relleno determinista de los usuarios existentes y las restricciones de nulabilidad, unicidad e índice.
- [X] T033 [AMP] Crear `V7__restrict_single_superuser.sql` con el índice único parcial sobre `role = 'SUPERUSER'`.
- [X] T034 [AMP] Confirmar que las migraciones históricas `V1` a `V5` no fueron modificadas.

## Phase 6.3: Modelo de dominio

- [X] T035 [AMP] Crear `models/enums/UserRole.java` con los valores `USER` y `SUPERUSER`.
- [X] T036 [AMP] Crear las excepciones de dominio `UsernameEmptyException` y `UsernameInvalidException` siguiendo las categorías de [arquitectura](../../../docs/architecture.md) §1.6.
- [X] T037 [AMP] Ampliar `models/User.java` con `username` normalizado a minúsculas y `role` inicializado en `USER`, conservando las invariantes de email y contraseña.

## Phase 6.4: Persistencia, servicios y contrato HTTP

- [X] T038 [AMP] Ampliar `repositories/UserRepository.java` con `existsByUsername` y la consulta de `SUPERUSER`.
- [X] T039 [AMP] Crear `services/exceptions/UsernameAlreadyRegisteredException.java` y registrarla en `GlobalExceptionHandler` con el código estable `USERNAME_ALREADY_TAKEN`.
- [X] T040 [AMP] Ampliar `AuthenticationService.java` y `AuthenticationServiceImpl.java` con la normalización y la comprobación de unicidad del nombre de usuario, conservando la asignación invariante del rol `USER` y el inicio de sesión por correo electrónico.
- [X] T041 [AMP] Ampliar `RegisterRequestDTO.java` con `username` obligatorio, sus validaciones y su documentación OpenAPI, sin incorporar campo de rol.
- [X] T042 [AMP] Ampliar `CurrentUserResponseDTO.java` con `username` y `role`.
- [X] T043 [AMP] Ampliar `UserMapper.java` para construir el usuario con nombre de usuario y para exponer la identidad actual completa.
- [X] T044 [AMP] Actualizar la documentación OpenAPI de `AuthenticationController.java` para el registro y para `GET /api/auth/me`, incluyendo el nuevo `409`.
- [X] T045 [AMP] Actualizar la colección Postman existente con el campo `username` en la solicitud `Auth / Register`.

## Phase 6.5: Aprovisionamiento del SUPERUSER

- [X] T046 [AMP] Crear `config/SuperuserProperties.java` con validación de configuración completa y sin valores por defecto.
- [X] T047 [AMP] Crear `services/SuperuserProvisioningService.java` y su implementación con provisión idempotente y hasheo BCrypt de la contraseña.
- [X] T048 [AMP] Registrar el componente de inicialización de arranque habilitado solo por propiedad y deshabilitado por defecto.
- [X] T049 [AMP] Declarar las propiedades del `SUPERUSER` en la configuración sin incluir credenciales ni valores por defecto.

## Phase 6.6: Tests

- [X] T050 [AMP] Ampliar `UserTest` con las invariantes de nombre de usuario, el rango de longitud, el formato y el rol por defecto.
- [X] T051 [AMP] Ampliar `AuthenticationServiceTest` con normalización, duplicados exactos y por mayúsculas y minúsculas, asignación de `USER` y rechazo del inicio de sesión con nombre de usuario.
- [X] T052 [AMP] Crear `UserRoleMigrationTest` para el relleno de los usuarios existentes, la unicidad del nombre de usuario y el rechazo de un segundo `SUPERUSER`.
- [X] T053 [AMP] Crear `SuperuserProvisioningServiceTest` para la creación, la idempotencia, el hasheo y el fallo por configuración incompleta.
- [X] T054 [AMP] Crear `SuperuserInitializationTest` para la ausencia de creación cuando está deshabilitado, la creación idempotente cuando está habilitado y el fallo de arranque con configuración incompleta.
- [X] T055 [AMP] Actualizar `AuthenticationControllerTest` con los casos de registro y de identidad actual, incluyendo snippets de Spring REST Docs.
- [X] T056 [AMP] Actualizar `AuthenticationJourneyE2ETest` y `JwtAuthenticationIntegrationTest` para el nuevo body de registro, conservando la verificación de login, JWT y recursos protegidos.

## Phase 6.7: Verificación y entrega

- [X] T057 [AMP] Ejecutar el build permitido `./gradlew build -x test` desde `backend/`. El agente no ejecuta tests ni genera evidencia de comportamiento por sí mismo.
- [X] T058 [AMP] Revisar el diff final, `git diff --check` y `git status --short`.
- [ ] T059 [AMP] Entregar al usuario la lista exacta de tests a ejecutar y la validación manual pendiente de `quickstart.md` contra el backend en desarrollo.

**Checkpoint**: La ampliación está implementada y documentada. Permanece pendiente la verificación
por parte del usuario y la validación manual del flujo documentado.

### Dependencias de Phase 6

- Phase 6.1 no tiene dependencias y puede ejecutarse de forma independiente.
- Phase 6.2 debe ejecutarse antes de Phase 6.3 y 6.4: `ddl-auto: validate` impide arrancar si el
  esquema y el modelo no coinciden.
- Phase 6.5 depende de Phase 6.3 y 6.4.
- Phase 6.6 depende de las fases de implementación que prueba.

---

## Retiro de herramientas de documentación (2026-10-14)

**Decisión del proyecto.** Se retiraron del repositorio la colección Postman y las integraciones de
Spring REST Docs y Asciidoctor. Ninguna de ellas definía el contrato de la API: ese contrato vive en
`contracts/api.md`, en las anotaciones OpenAPI de Controllers y DTO, y en los tests funcionales de
Controller. El retiro no altera ningún requisito funcional, ningún endpoint ni ningún código.

**Efecto sobre las tareas de este archivo.**

- `T024` permanece `[X]` sin cambios, como registro histórico de una tarea cumplida con la colección
  vigente en su momento.
- `T025` conserva su identificador y su estado `- [ ]`, y se reformuló el 2026-10-14 para validar el
  flujo de `quickstart.md` mediante HTTP directo o Swagger UI en lugar de la colección Postman. Sigue
  sin acreditar.
- `T045` permanece `[X]` como registro histórico de la actualización de la colección.

**Documentos actualizados.** `quickstart.md` (prerrequisitos, pasos 2, 4, 6 y 11, y restricción de no
crear colección) y `tasks.md` (`T025` y `T059`). **Documentos conservados sin reescribir por
contener decisiones o estados históricos:** `plan.md`, `research.md`, `spec.md`, `data-model.md`,
`contracts/api.md` y `checklists/requirements.md`. Sus referencias a las herramientas retiradas se
conservan como registro de las decisiones tomadas en su momento.

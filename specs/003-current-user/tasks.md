# Tasks: Current Authenticated User

**Input**: Design documents from `/specs/003-current-user/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/api.md`, `quickstart.md`

**Tests**: Son obligatorios porque la spec exige escenarios verificables y regresión, y la Constitución requiere tests para cambios de seguridad.

**Organization**: US1 implementa la recuperación persistida. US2 garantiza que solo una identidad JWT correspondiente a un usuario actual alcance el endpoint y que todos los rechazos sean `401` vacíos producidos por Security.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo dentro de la ventana indicada porque modifica archivos distintos y no depende de trabajo incompleto.
- **[Story]**: Historia cubierta por la tarea (`US1` o `US2`).
- Todas las tareas incluyen rutas concretas.

## Phase 1: Setup (Shared Infrastructure)

No se requieren cambios de inicialización: JWT, PostgreSQL, Testcontainers, REST Docs, OpenAPI y Postman ya están configurados. No se agregarán dependencias ni migraciones.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Definir el resultado compartido que necesitan el caso de uso y la infraestructura de seguridad.

- [X] T001 Crear y documentar `CurrentUserNotFoundException` dentro de la jerarquía de excepciones del proyecto en `backend/src/main/java/footballmarket/services/exceptions/CurrentUserNotFoundException.java`

**Checkpoint**: El Service puede expresar que el sujeto no corresponde a una identidad persistida sin reutilizar los contratos de login o `404`.

---

## Phase 3: User Story 1 - Recuperar la identidad de la sesión actual (Priority: P1) 🎯

**Goal**: Un cliente con JWT válido asociado a un usuario existente obtiene `200 OK` con exactamente el `id` y `email` persistidos actualmente.

**Independent Test**: Registrar un usuario, iniciar sesión y consultar `GET /api/auth/me` con el token; comprobar un JSON exacto con dos campos cuyos valores proceden del usuario persistido.

### Tests for User Story 1

> Escribir estos tests primero y comprobar que fallan antes de implementar.

- [X] T002 [P] [US1] Agregar tests de Service para recuperar por email sujeto normalizado el `User` persistido actual en `backend/src/test/java/footballmarket/services/AuthenticationServiceTest.java`
- [X] T003 [P] [US1] Agregar el test de Controller para JWT válido, respuesta JSON estricta `{id,email}`, uso del `sub` y snippet REST Docs exitoso en `backend/src/test/java/footballmarket/controllers/AuthenticationControllerTest.java`
- [X] T004 [P] [US1] Extender el journey registro → login → `/api/auth/me` con `RestClient` y el Response DTO productivo en `backend/src/test/java/footballmarket/e2e/AuthenticationJourneyE2ETest.java`

### Implementation for User Story 1

- [X] T005 [P] [US1] Crear y documentar el `record` público `CurrentUserResponseDTO(Long id, String email)` en `backend/src/main/java/footballmarket/controllers/dtos/responses/CurrentUserResponseDTO.java`
- [X] T006 [P] [US1] Incorporar el contrato público documentado `getCurrentUser(String subjectEmail)` en `backend/src/main/java/footballmarket/services/AuthenticationService.java`
- [X] T007 [US1] Implementar `getCurrentUser` con normalización, `UserRepository.findByEmail` y `CurrentUserNotFoundException` para sujeto nulo, vacío o inexistente en `backend/src/main/java/footballmarket/services/impl/AuthenticationServiceImpl.java`
- [X] T008 [US1] Agregar y documentar la transformación pública estática `User` → `CurrentUserResponseDTO` sin password ni campos adicionales en `backend/src/main/java/footballmarket/controllers/mappers/UserMapper.java`
- [X] T009 [US1] Implementar y documentar con Javadoc `GET /api/auth/me`, delegar por `sub`, mapear la respuesta y declarar `200` y seguridad Bearer con OpenAPI en `backend/src/main/java/footballmarket/controllers/AuthenticationController.java`

**Checkpoint**: Con un principal JWT válido cuyo usuario existe, US1 responde `200` con exactamente `id` y `email` persistidos.

---

## Phase 4: User Story 2 - Rechazar solicitudes no autenticadas (Priority: P1)

**Goal**: Security rechaza antes del endpoint todo JWT ausente, inválido, expirado, sin sujeto utilizable o sin usuario persistido mediante `401 Unauthorized` sin cuerpo.

**Independent Test**: Ejecutar cada variante contra `/api/auth/me`, comprobar `401` y body vacío, y verificar que el método del Controller no se alcanza cuando falla la autenticación.

### Tests for User Story 2

> Escribir estos tests primero y comprobar que fallan antes de implementar.

- [X] T010 [P] [US2] Agregar tests de Service para sujeto nulo, vacío e inexistente y afirmar `CurrentUserNotFoundException` en `backend/src/test/java/footballmarket/services/AuthenticationServiceTest.java`
- [X] T011 [P] [US2] Agregar tests de Controller para JWT ausente, malformado, con firma inválida, expirado, sin sujeto y sin usuario persistido; afirmar `401` vacío y generar el snippet REST Docs no autorizado en `backend/src/test/java/footballmarket/controllers/AuthenticationControllerTest.java`
- [X] T012 [P] [US2] Extender la integración de seguridad sobre `/api/auth/me` para JWT válido, ausente, inválido, expirado, sin sujeto y criptográficamente válido sin usuario persistido en `backend/src/test/java/footballmarket/security/JwtAuthenticationIntegrationTest.java`

### Implementation for User Story 2

- [X] T013 [US2] Crear `PersistedUserJwtAuthenticationConverter`, documentar su API pública, validar el sujeto mediante AuthenticationService y traducir `CurrentUserNotFoundException` a fallo OAuth2 de autenticación en `backend/src/main/java/footballmarket/security/PersistedUserJwtAuthenticationConverter.java`
- [X] T014 [US2] Configurar el converter persistido en OAuth2 Resource Server y reemplazar `/api/auth/**` por permisos exactos para `POST /api/auth/register` y `POST /api/auth/login` en `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- [X] T015 [US2] Completar OpenAPI de `GET /api/auth/me` con el `401` sin contenido, sin aplicar seguridad Bearer a register/login, en `backend/src/main/java/footballmarket/controllers/AuthenticationController.java`

**Checkpoint**: US1 y US2 forman el incremento seguro: solo un JWT válido cuyo sujeto identifica a un usuario persistido obtiene `200`; todos los demás casos reciben `401` vacío desde Security.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Sincronizar documentación, cliente manual y controles de regresión.

- [X] T016 [P] Incorporar la sección de usuario actual y los snippets `auth-me-success` y `auth-me-unauthorized` en `backend/src/docs/asciidoc/index.adoc`
- [X] T017 [P] Agregar `Current user` bajo `Auth`, con `GET /api/auth/me`, Bearer `{{JWT_TOKEN}}` y sin body, en `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`
- [X] T018 Ejecutar la validación focalizada indicada en `specs/003-current-user/quickstart.md` y corregir únicamente fallos introducidos por la feature
- [X] T019 Ejecutar `./gradlew test spotlessJavaCheck asciidoctor` desde `backend/`; confirmar explícitamente los casos existentes de register exitoso, duplicado e inválido y login exitoso, con credenciales inválidas y entrada inválida, manteniendo coherencia con `specs/003-current-user/contracts/api.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup**: No requiere cambios.
- **Foundational**: T001 debe completarse antes del Service y del converter.
- **US1**: Depende de T001.
- **US2**: Depende del contrato y comportamiento de Service creados por US1.
- **Polish**: Depende de US1 y US2 completas.

### User Story Dependencies

```text
T001: resultado de identidad inexistente
 └── US1: recuperar usuario persistido y responder 200
      └── US2: autenticar contra persistencia y responder 401 desde Security
           └── Polish: documentación, Postman y regresión
```

- US1 no depende de otra historia, únicamente del prerrequisito T001.
- US2 necesita la operación de consulta de US1 para validar la identidad durante la autenticación.
- Ambas historias son obligatorias para un MVP desplegable.

### Within User Story 1

- T002–T004 pueden escribirse en paralelo antes de implementar.
- T005 y T006 pueden ejecutarse en paralelo.
- T007 depende de T001 y T006.
- T008 depende de T005.
- T009 depende de T005–T008.
- T002–T004 deben aprobar después de T009.

### Within User Story 2

- T010–T012 pueden escribirse en paralelo.
- T013 depende de T001, T006 y T007.
- T014 depende de T013.
- T015 depende de T009.
- T010–T012 deben aprobar después de T013–T015.

### Parallel Opportunities

- T002, T003 y T004 modifican suites distintas.
- T005 y T006 modifican contratos productivos distintos.
- T010, T011 y T012 modifican suites distintas.
- T016 y T017 actualizan canales documentales distintos.

---

## Parallel Example: User Story 1

```text
Task T002: Service test en backend/src/test/java/footballmarket/services/AuthenticationServiceTest.java
Task T003: Controller test en backend/src/test/java/footballmarket/controllers/AuthenticationControllerTest.java
Task T004: E2E en backend/src/test/java/footballmarket/e2e/AuthenticationJourneyE2ETest.java

Task T005: Response DTO en backend/src/main/java/footballmarket/controllers/dtos/responses/CurrentUserResponseDTO.java
Task T006: Service contract en backend/src/main/java/footballmarket/services/AuthenticationService.java
```

## Parallel Example: User Story 2

```text
Task T010: Service rejection tests en backend/src/test/java/footballmarket/services/AuthenticationServiceTest.java
Task T011: HTTP rejection tests en backend/src/test/java/footballmarket/controllers/AuthenticationControllerTest.java
Task T012: JWT integration tests en backend/src/test/java/footballmarket/security/JwtAuthenticationIntegrationTest.java
```

---

## Implementation Strategy

### MVP seguro

1. Completar T001.
2. Completar US1 y validar el caso exitoso.
3. Completar US2 y validar todos los rechazos desde Security.
4. Completar documentación y controles.
5. Entregar US1 + US2 como un único MVP seguro.

### Incremental Delivery

1. **Incremento técnico US1**: consulta persistida y contrato `200`, apto para revisión interna.
2. **Incremento técnico US2**: autenticación respaldada por persistencia y contrato `401`, apto para despliegue junto con US1.
3. **Incremento final**: documentación, Postman y regresión completa.

## Notes

- No agregar handlers vacíos a `GlobalExceptionHandler`; los `401` vacíos pertenecen al entry point de Security.
- No crear migraciones, claims, refresh tokens, logout, revocación ni dependencias.
- No reutilizar `UserNotFoundException` ni `InvalidCredentialsException` porque poseen contratos observables distintos.
- Los tests de Service no pueden acceder a Repository; deben utilizar Service real y Testcontainers.
- No versionar artefactos generados bajo `backend/build/`.

---

## Phase 6: Actualización documental del contrato (2026-10-14)

**Purpose**: Reflejar en esta spec el contrato vigente de `GET /api/auth/me` tras la ampliación de
SPEC-001, que añadió `username` y `role` a la respuesta.

**Alcance**: Exclusivamente documental. La implementación de `username` y `role` pertenece a
`specs/001-user-auth-registration/` (Phase 6) y no se reimplementa aquí.

**Estado de las tareas anteriores**: las 19 tareas de las fases 1 a 5 conservan su estado original
y no se modifican. Ninguna tarea de esta fase se marca como completada sin evidencia de ejecución.
Los *checkpoints* de las fases anteriores describen el contrato vigente en su momento y quedan
superados, en lo que respecta a la forma de la respuesta, por la amendment de `spec.md`.

- [X] T020 Actualizar `spec.md`: escenario 4 nuevo y ajuste de `FR-006`, `FR-007`, `SC-001`, `SC-002`, `Key Entities` y `Out of Scope`, conservando los identificadores.
- [X] T021 Actualizar `data-model.md` con las filas `username` y `role` del User y la tabla de la respuesta actual.
- [X] T022 Actualizar `contracts/api.md` con el cuerpo de cuatro campos y su tabla.
- [X] T023 Actualizar `quickstart.md` para el registro con `username` y la verificación de los cuatro campos.
- [X] T024 Registrar la sección `## Amendments` con la redacción anterior preservada.
- [ ] T025 Verificar mediante la suite del backend que `/api/auth/me` devuelve `id`, `username`, `email` y `role` con la comparación JSON estricta. Sin acreditar en este repositorio.
- [ ] T026 Verificar en un entorno de desarrollo que la respuesta expone el `username` normalizado a minúsculas. Sin acreditar en este repositorio.
- [ ] T027 Confirmar que el contrato documentado, la especificación OpenAPI y el comportamiento HTTP de `GET /api/auth/me` son coherentes entre sí respecto del cuerpo de cuatro campos. Reformulada el 2026-10-14 al retirar la colección Postman; conserva su identificador y sigue sin acreditar en el repositorio.

**Checkpoint**: la documentación de esta spec es coherente con el contrato implementado. Queda
pendiente la verificación por parte del usuario de los controles T025 a T027.

### Dependencias de Phase 6

- Requiere la ampliación de SPEC-001 implementada y documentada.
- No depende de Phase 1 a 5, que permanecen sin cambios.

---

## Retiro de herramientas de documentación (2026-10-14)

**Decisión del proyecto.** Se retiraron del repositorio la colección Postman y las integraciones de
Spring REST Docs y Asciidoctor. El contrato de `GET /api/auth/me` no dependía de ellas: vive en
`contracts/api.md`, en las anotaciones OpenAPI de `AuthenticationController` y `CurrentUserResponseDTO`,
y en los tests funcionales de Controller. El retiro no altera ningún requisito funcional de esta spec
ni el comportamiento del endpoint.

**Efecto sobre las tareas de este archivo.**

- `T017` permanece `[X]` como registro histórico de una tarea cumplida cuando la colección todavía
  formaba parte del proyecto.
- `T019` permanece `[X]`; la tarea `asciidoctor` que incluía se retiró y ya no está disponible.
- `T025` y `T026` conservan su estado `- [ ]` y su redacción, sin cambios.
- `T027` conserva su identificador y su estado `- [ ]`, y se reformuló el 2026-10-14 para verificar
  la coherencia entre contrato documentado, OpenAPI y comportamiento HTTP.

**Documentos actualizados.** `quickstart.md` (prerrequisitos, comandos de validación, journey manual,
escenarios de rechazo y documentación verificable) y `tasks.md` (`T027`).

**Documentos conservados sin reescribir por contener decisiones o estados históricos:** `plan.md`,
`research.md`, `spec.md`, `data-model.md`, `contracts/api.md` y `checklists/requirements.md`. El
registro de "Validación ejecutada durante la implementación" de `quickstart.md` se conserva con una
nota que delimita su alcance histórico. Sus referencias a las herramientas retiradas se mantienen
como registro de las decisiones tomadas en su momento.

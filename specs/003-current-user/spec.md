# Feature Specification: Current Authenticated User

**Feature Branch**: `[003-current-user]`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "Agregar un endpoint protegido GET /api/auth/me que permita a un cliente autenticado recuperar el id y email actuales del usuario persistido asociado al JWT de su sesión."

## Clarifications

### Session 2026-09-20

- Q: ¿Qué cuerpo debe devolver `GET /api/auth/me` cuando responde `401 Unauthorized`? → A: Sin cuerpo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recuperar la identidad de la sesión actual (Priority: P1)

Como cliente con una sesión válida, quiero consultar la identidad del usuario asociado a mi sesión para comprobar que sigo autenticado y recuperar sus datos básicos actuales.

**Why this priority**: Es el objetivo principal de la funcionalidad y permite que clientes como el frontend reconstruyan el contexto del usuario a partir de una sesión existente.

**Independent Test**: Se puede probar con un usuario persistido y un JWT válido cuyo identificador de sujeto sea el email de ese usuario; al consultar `GET /api/auth/me` con `Authorization: Bearer <token>`, la respuesta debe contener exclusivamente el `id`, el `username`, el `email` y el `role` almacenados actualmente para ese usuario.

**Acceptance Scenarios**:

1. **Given** existe un usuario persistido y se dispone de un JWT válido y no expirado cuyo `sub` contiene su email, **When** el cliente solicita `GET /api/auth/me` con ese token, **Then** el sistema responde `200 OK` con el `id`, el `username`, el `email` y el `role` actuales del usuario persistido.
2. **Given** un JWT válido identifica a un usuario persistido, **When** los datos persistidos de ese usuario difieren de cualquier dato adicional que pudiera estar contenido en el token, **Then** la respuesta utiliza exclusivamente el `id`, el `username`, el `email` y el `role` obtenidos del estado persistido actual.
3. **Given** la consulta autenticada es exitosa, **When** el sistema construye la respuesta, **Then** el cuerpo contiene únicamente los campos `id`, `username`, `email` y `role`, sin contraseña ni otros datos del usuario.
4. **Given** un usuario cuyo nombre de usuario se registró escrito en mayúsculas, **When** el cliente solicita `GET /api/auth/me`, **Then** la respuesta expone el nombre de usuario ya normalizado a minúsculas, tal como fue persistido.

---

### User Story 2 - Rechazar solicitudes no autenticadas (Priority: P1)

Como responsable de la seguridad del sistema, quiero que solo una sesión válida y asociada a un usuario existente pueda consultar la identidad actual para evitar la exposición de información de usuarios.

**Why this priority**: La protección del recurso es inseparable de su valor funcional; una respuesta de identidad no debe estar disponible sin autenticación efectiva.

**Independent Test**: Se puede probar consultando el endpoint sin credenciales, con un JWT inválido, con un JWT expirado y con un JWT criptográficamente válido cuyo sujeto ya no corresponde a ningún usuario persistido; en todos los casos debe responder `401 Unauthorized` y no exponer datos de usuario.

**Acceptance Scenarios**:

1. **Given** una solicitud no incluye autenticación, **When** el cliente solicita `GET /api/auth/me`, **Then** el sistema responde `401 Unauthorized` sin cuerpo.
2. **Given** una solicitud incluye un JWT inválido, **When** el cliente solicita `GET /api/auth/me`, **Then** el sistema responde `401 Unauthorized` sin cuerpo.
3. **Given** una solicitud incluye un JWT expirado, **When** el cliente solicita `GET /api/auth/me`, **Then** el sistema responde `401 Unauthorized` sin cuerpo.
4. **Given** una solicitud incluye un JWT criptográficamente válido y vigente cuyo `sub` no corresponde al email de ningún usuario persistido, **When** el cliente solicita `GET /api/auth/me`, **Then** el sistema responde `401 Unauthorized` sin cuerpo.

### Edge Cases

- El encabezado `Authorization` está presente pero no contiene un token Bearer utilizable; la solicitud se considera no autenticada.
- El JWT es válido y vigente, pero su `sub` está ausente, vacío o no permite identificar a un usuario existente; la solicitud se considera no autenticada.
- El usuario identificado por el token deja de existir antes de la consulta; la solicitud responde `401 Unauthorized`, aunque el token conserve una firma válida y no haya expirado.
- El email persistido se obtiene usando las mismas reglas de identidad vigentes en el sistema; la respuesta refleja exactamente el usuario encontrado mediante esas reglas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer `GET /api/auth/me` para consultar la identidad del usuario actualmente autenticado.
- **FR-002**: El endpoint MUST requerir autenticación mediante el JWT empleado actualmente por la aplicación y recibido como `Authorization: Bearer <token>`.
- **FR-003**: El sistema MUST utilizar el email incluido en el `sub` del JWT válido como identidad para buscar al usuario en el estado persistido actual.
- **FR-004**: El sistema MUST obtener la información de la respuesta desde el usuario persistido encontrado y MUST NOT limitarse a devolver datos extraídos directamente del JWT.
- **FR-005**: Cuando el JWT sea válido, esté vigente y su identidad corresponda a un usuario persistido, el sistema MUST responder `200 OK`.
- **FR-006**: La respuesta exitosa MUST contener exclusivamente el identificador del usuario en `id`, su nombre de usuario normalizado en `username`, su correo electrónico en `email` y su rol en `role`.
- **FR-007**: El `id`, el `username`, el `email` y el `role` de la respuesta MUST corresponder al estado actual del mismo usuario persistido identificado por el `sub` del JWT.
- **FR-008**: Si no se proporciona autenticación, el sistema MUST responder `401 Unauthorized`.
- **FR-009**: Si el JWT es inválido o está expirado, el sistema MUST responder `401 Unauthorized`.
- **FR-010**: Si el JWT es criptográficamente válido y vigente, pero su identidad no corresponde a un usuario existente, el sistema MUST considerar la solicitud no autenticada y responder `401 Unauthorized`.
- **FR-011**: Toda respuesta `401 Unauthorized` de este endpoint MUST tener un cuerpo vacío.
- **FR-012**: La incorporación de este endpoint MUST NOT alterar el comportamiento observable de `POST /api/auth/register` ni de `POST /api/auth/login`.
- **FR-013**: Esta funcionalidad MUST reutilizar el mecanismo de autenticación JWT existente sin cambiar la duración de los tokens ni exigir nuevos datos en ellos.

### Key Entities *(include if feature involves data)*

- **Usuario**: Persona registrada y persistida en el sistema. Para esta funcionalidad son relevantes su identificador único, su nombre de usuario, su correo electrónico actual y su rol.
- **Sesión autenticada**: Contexto de una solicitud cuya identidad procede de un JWT válido y vigente. El sujeto del token referencia al usuario mediante su email, pero no sustituye la consulta del estado persistido.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas realizadas con un JWT válido, vigente y asociado a un usuario existente responden `200 OK` con el `id`, `username`, `email` y `role` actuales de ese usuario.
- **SC-002**: El 100% de las respuestas exitosas contienen exactamente los cuatro campos permitidos, `id`, `username`, `email` y `role`, y cero datos adicionales o sensibles.
- **SC-003**: El 100% de las consultas sin autenticación, con JWT inválido, con JWT expirado o con identidad sin usuario persistido responden `401 Unauthorized` y no exponen información de usuario.
- **SC-004**: Un cliente autenticado puede comprobar su sesión y recuperar su identidad básica mediante una única solicitud.
- **SC-005**: Las pruebas de regresión confirman que el 100% de los comportamientos previamente aceptados de registro e inicio de sesión permanecen sin cambios.

## Assumptions

- El sistema ya emite y valida JWT, y el `sub` de cada token emitido contiene el email que identifica al usuario según las reglas vigentes.
- El sistema mantiene usuarios persistidos con un identificador y un email disponibles para consulta.
- Las respuestas `401 Unauthorized` generadas al proteger este endpoint mantienen el contrato vigente de los recursos protegidos: no incluyen cuerpo.
- El tráfico autenticado se realiza sobre un canal seguro, conforme a las condiciones operativas ya asumidas por la autenticación existente.

## Out of Scope

- Modificar el registro o el inicio de sesión existentes.
- Implementar logout, revocación de JWT o refresh tokens.
- Cambiar la duración actual del JWT o agregar datos al token para soportar esta consulta.
- Modificar datos del usuario o incorporar funcionalidades de perfil.
- Ampliar la respuesta con datos distintos de `id`, `username`, `email` y `role`.
- Usar el `role` expuesto para autorizar operaciones. El rol es informativo para el cliente y toda autorización se evalúa en el backend.

## Amendments

### 2026-10-14 — El contrato de `/api/auth/me` incluye `username` y `role`

**Qué cambió.** `GET /api/auth/me` pasó de devolver exclusivamente `id` y `email` a devolver `id`, `username`, `email` y `role`.

**Por qué cambió.** La ampliación de SPEC-001 incorporó el nombre de usuario y los roles al agregado `User`. Esta spec consume el estado persistido del usuario, por lo que su respuesta debía reflejar el agregado vigente sin volver a consultarlo por separado.

**Qué contrato de SPEC-001 lo respalda.** `FR-018` (exponer el nombre de usuario del usuario autenticado) y el nuevo requisito de rol del `Key Entities` de SPEC-001, con el detalle de contrato en [contracts/api.md](../001-user-auth-registration/contracts/api.md) §Identidad actual. El tipo JSON del identificador sigue siendo numérico, tal como ya lo documentaba esta spec.

**Documentos actualizados.** `spec.md` (escenario 4 nuevo; `FR-006`, `FR-007`, `SC-001`, `SC-002`, `Key Entities` y `Out of Scope` ajustados a cuatro campos), `data-model.md`, `contracts/api.md` y `quickstart.md`. `plan.md` y `research.md` conservan su redacción original como registro histórico de la decisión de diseño.

**Redacción anterior preservada.** El enunciado original de los requisitos afectados era: `FR-006` "La respuesta exitosa MUST contener exclusivamente el identificador del usuario en `id` y su correo electrónico en `email`"; `FR-007` "El `id` y el `email` de la respuesta MUST corresponder al estado actual del mismo usuario persistido identificado por el `sub` del JWT"; `SC-002` "El 100% de las respuestas exitosas contienen exactamente los dos campos permitidos, `id` y `email`, y cero datos adicionales o sensibles"; y `Out of Scope` "Ampliar la respuesta con datos distintos de `id` y `email`". No se eliminó ningún requisito: los cuatro identificadores se conservan y su alcance se amplió.

**Lo que no cambió.** `FR-001` a `FR-005` y `FR-008` a `FR-013` permanecen idénticos. La autenticación, el `sub` del JWT, la fuente de verdad persistida, los `401` vacíos y la ausencia de datos sensibles en la respuesta no se modifican. No se agregó ninguna regla de autorización por rol.

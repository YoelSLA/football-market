# Feature Specification: User Registration and Authentication

**Feature Branch**: `[001-user-auth-registration]`

**Created**: 2026-08-31

**Status**: Approved

**Input**: User description: "Necesitamos implementar una funcionalidad de registro y autenticación de usuarios. Un usuario debe poder registrarse proporcionando un email y una contraseña..."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Creación de cuenta de usuario (Priority: P1)

Como nuevo visitante, quiero crear una cuenta utilizando mi correo electrónico y una contraseña para poder acceder a las funciones protegidas del sistema.

**Why this priority**: Primer paso esencial para cualquier funcionalidad específica del usuario; necesario para la identificación y la seguridad.

**Independent Test**: Se puede probar enviando el formulario de registro con datos válidos y verificando que el usuario pueda intentar iniciar sesión posteriormente.

**Acceptance Scenarios**:

1. **Given** un visitante está en la página de registro, **When** envía un correo electrónico válido ("test@example.com") y una contraseña de al menos 8 caracteres, **Then** el sistema crea la cuenta y responde `201 Created` sin body.
2. **Given** un visitante intenta registrarse con un correo electrónico que ya existe en el sistema, **When** envía el registro, **Then** el sistema rechaza el registro y proporciona un mensaje de error claro.
3. **Given** un visitante proporciona un formato de correo electrónico no válido, **When** envía el registro, **Then** el sistema rechaza la solicitud.

---

### User Story 2 - Inicio de sesión de usuario (Priority: P1)

Como usuario registrado, quiero iniciar sesión con mis credenciales y recibir un JWT para
poder acceder a los recursos protegidos mientras el token sea válido.

**Why this priority**: Funcionalidad central para verificar la identidad y otorgar acceso al sistema.

**Independent Test**: Se puede probar iniciando sesión con credenciales correctas y usando el JWT
devuelto para acceder a un recurso protegido. Sin token, con firma inválida o con token expirado,
el acceso debe rechazarse.

**Acceptance Scenarios**:

1. **Given** un usuario registrado con correo electrónico "user@test.com" y contraseña "password123", **When** envía estas credenciales al endpoint de login, **Then** el sistema responde `200 OK` con un JWT en el campo `token`.
2. **Given** un usuario proporciona un correo electrónico correcto pero una contraseña incorrecta, **When** envía, **Then** el sistema niega el acceso.
3. **Given** un usuario proporciona un correo electrónico que no está registrado, **When** envía, **Then** el sistema niega el acceso.
4. **Given** un usuario dispone de un JWT válido emitido por el sistema, **When** lo envía como `Authorization: Bearer <token>` a un recurso protegido, **Then** el sistema valida el token y permite el acceso.
5. **Given** una solicitud a un recurso protegido sin token, con una firma inválida o con un JWT expirado, **When** se procesa la solicitud, **Then** el sistema rechaza el acceso con `401 Unauthorized`.

---

### User Story 3 - Nombre de usuario visible (Priority: P2)

Como usuario registrado, quiero tener un nombre de usuario propio, distinto de mi correo
electrónico, para ser identificado de forma pública en el sistema y en las interfaces.

**Why this priority**: El nombre de usuario es la identidad pública del usuario y es un
prerrequisito de la interfaz que mostrará quién es el propietario de cada operación en el
mercado de tokens.

**Independent Test**: Se puede probar registrando un usuario con un nombre de usuario válido,
comprobando que se persiste y que la identidad actual lo expone, y verificando que el nombre de
usuario no sirve para autenticarse.

**Acceptance Scenarios**:

1. **Given** un visitante se registra con un correo electrónico válido, una contraseña de al menos 8 caracteres y un nombre de usuario válido ("juan.perez"), **When** envía el registro, **Then** el sistema crea la cuenta, responde `201 Created` sin body y persiste el nombre de usuario asociado a esa cuenta.
2. **Given** un visitante se registra sin nombre de usuario o con un nombre de usuario vacío, **When** envía el registro, **Then** el sistema rechaza la solicitud y explica que el nombre de usuario es obligatorio.
3. **Given** un visitante se registra con un nombre de usuario que ya está en uso, **When** envía el registro, **Then** el sistema rechaza la solicitud y explica que el nombre de usuario no está disponible.
4. **Given** un visitante se registra con un nombre de usuario fuera del formato admitido, **When** envía el registro, **Then** el sistema rechaza la solicitud indicando el formato esperado.
5. **Given** existen dos nombres de usuario que solo se diferencian en mayúsculas y minúsculas ("juan" y "JUAN"), **When** se intenta registrar cualquiera de los dos, **Then** el sistema los considera el mismo nombre de usuario y rechaza el segundo por duplicado.
6. **Given** un visitante se registra con un nombre de usuario escrito en mayúsculas y minúsculas ("Juan.Perez"), **When** envía el registro, **Then** el sistema acepta el registro y persiste el nombre de usuario normalizado a minúsculas.
7. **Given** un usuario registrado con nombre de usuario "juan.perez", **When** consulta su identidad actual, **Then** la respuesta incluye ese nombre de usuario junto al identificador y al correo electrónico.
8. **Given** un usuario registrado, **When** intenta iniciar sesión indicando su nombre de usuario en lugar de su correo electrónico, **Then** el sistema rechaza el inicio de sesión, porque solo el correo electrónico y la contraseña son credenciales.

---

### User Story 4 - Roles de usuario y SUPERUSER único (Priority: P2)

Como sistema, quiero que cada usuario tenga exactamente un rol y que exista un único
SUPERUSER, para poder identificar con confiable al dueño inicial de los tokens sin exponer esa
capacidad a los usuarios registrados públicamente.

**Why this priority**: El mercado de tokens opera inicialmente contra un único SUPERUSER. La
identificación de esa cuenta debe ser confiable antes de implementar las operaciones de compra y
venta.

**Independent Test**: Se puede probar registrando varios usuarios públicos, comprobando que todos
reciben el rol USER, verificando que el registro no permite elegir el rol, e inicializando el
SUPERUSER dos veces para comprobar que la inicialización es idempotente y que sigue existiendo un
único SUPERUSER.

**Acceptance Scenarios**:

1. **Given** un visitante completa un registro público válido, **When** el sistema crea la cuenta, **Then** el usuario creado tiene el rol USER.
2. **Given** un visitante intenta registrarse, **When** incluye un rol en los datos enviados, **Then** el sistema no utiliza ese valor y asigna USER, de modo que no es posible convertirse en SUPERUSER desde el registro público.
3. **Given** la aplicación se inicializa con la configuración de SUPERUSER habilitada, **When** arranca, **Then** existe exactamente un usuario con rol SUPERUSER asociado a la configuración, y las inicializaciones posteriores no crean usuarios adicionales.
4. **Given** la aplicación se inicializa sin la configuración de SUPERUSER habilitada, **When** arranca, **Then** no se crea ningún usuario SUPERUSER por el proceso de inicialización.
5. **Given** la aplicación se inicializa con la configuración de SUPERUSER habilitada pero incompleta, **When** arranca, **Then** la inicialización falla de forma explícita en lugar de crear una cuenta incompleta o insegura.
6. **Given** un usuario registrado, **When** inicia sesión con su correo electrónico y su contraseña, **Then** el acceso se concede con normalidad y la identidad actual expone su rol.
7. **Given** un usuario con rol SUPERUSER, **When** la base de datos admite un segundo usuario con ese mismo rol, **Then** la operación es rechazada por la base de datos.
8. **Given** la ampliación de la funcionalidad, **When** un usuario existente sin nombre de usuario ni rol es migrado, **Then** conserva su cuenta, su correo electrónico y su contraseña, y recibe un nombre de usuario derivado de forma determinista y el rol USER.

---

### Edge Cases

- **Case sensitivity**: ¿Cómo maneja el sistema "User@Test.com" vs "user@test.com"? (Los correos electrónicos deben tratarse como insensibles a mayúsculas para la identificación).
- **Case sensitivity del nombre de usuario**: el nombre de usuario no distingue mayúsculas de minúsculas. "Juan.Perez", "JUAN.PEREZ" y "juan.perez" son el mismo nombre de usuario; el sistema conserva la forma normalizada a minúsculas.
- **Special characters**: Las contraseñas pueden contener caracteres especiales, siempre que no estén vacías y tengan al menos 8 caracteres al registrarse.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST permitir a los usuarios registrarse proporcionando un correo electrónico y una contraseña.
- **FR-002**: System MUST validar que el correo electrónico no esté vacío y tenga un formato de correo electrónico válido.
- **FR-003**: System MUST validar que la contraseña de registro no esté vacía y tenga al menos 8 caracteres. La contraseña de login no debe estar vacía.
- **FR-004**: System MUST asegurar que cada correo electrónico sea único; los registros duplicados para el mismo correo electrónico MUST NOT ser aceptados.
- **FR-005**: System MUST permitir a los usuarios autenticarse (iniciar sesión) utilizando su correo electrónico registrado y su contraseña.
- **FR-006**: System MUST rechazar intentos de inicio de sesión con correos electrónicos inexistentes o contraseñas incorrectas.
- **FR-007**: System MUST proteger las credenciales: las contraseñas MUST NOT almacenarse en texto plano.
- **FR-008**: System MUST NOT exponer datos sensibles (contraseñas, credenciales en texto plano) en respuestas, mensajes de error o registros del sistema.
- **FR-009**: Los endpoints de la API para el registro y la autenticación MUST ser documentados de acuerdo con la sección 8 de la Constitución.
- **FR-010**: El registro exitoso MUST responder `201 Created` sin body; un email ya registrado MUST responder `409 Conflict`.
- **FR-011**: Los errores gestionados por la aplicación MUST devolver un `ErrorResponseDTO` con `timestamp`, `status`, `error`, `code`, `message` y `path`. `code` MUST identificar establemente el error; `status` MUST coincidir con el código HTTP y `path` MUST indicar la ruta solicitada. Para errores de Jakarta Validation, `message` MUST contener el primer mensaje de validación disponible.
- **FR-012**: El login exitoso MUST responder `200 OK` con un JWT en el campo `token`. Las solicitudes a recursos protegidos MUST presentar ese token mediante `Authorization: Bearer`; el servidor MUST validar su firma y vigencia antes de permitir el acceso y responder `401 Unauthorized` si el token falta o no es válido.

### Requirements — Ampliación: Nombre de usuario y roles

- **FR-013**: System MUST requerir un nombre de usuario en el registro. El nombre de usuario MUST ser obligatorio y MUST NOT admitir valores vacíos.
- **FR-014**: System MUST validar que el nombre de usuario tenga entre 3 y 30 caracteres y que esté formado únicamente por letras, números, punto, guion y guion bajo. El nombre de usuario MUST NOT contener espacios ni caracteres especiales.
- **FR-015**: System MUST almacenar y comparar el nombre de usuario normalizado a minúsculas, de modo que dos nombres de usuario que solo se diferencien en mayúsculas y minúsculas se consideren el mismo nombre de usuario. La normalización MUST aplicarse antes de persistir y antes de comprobar unicidad, y la respuesta al cliente MUST exponer siempre la forma normalizada.
- **FR-016**: System MUST asegurar que cada nombre de usuario sea único; un registro con un nombre de usuario ya utilizado MUST NOT ser aceptado y MUST responder `409 Conflict` con un código de error estable.
- **FR-017**: El nombre de usuario MUST NOT utilizarse como credencial. El inicio de sesión MUST continuar requiriendo el correo electrónico y la contraseña, y un intento de inicio de sesión con nombre de usuario MUST ser rechazado.
- **FR-018**: System MUST exponer el nombre de usuario del usuario autenticado en la respuesta de identidad actual, junto al identificador, al correo electrónico y al rol.
- **FR-019**: System MUST permitir que el nombre de usuario se Consulte y se muestre al usuario, y MUST NOT permitir su modificación dentro de esta funcionalidad.
- **FR-020**: Cada usuario MUST tener exactamente un rol, y los roles admitidos MUST ser `USER` y `SUPERUSER`.
- **FR-021**: Los usuarios creados mediante el registro público MUST recibir siempre el rol `USER`.
- **FR-022**: El rol MUST NOT recibirse ni elegirse desde los datos de registro. Si el cliente envía un rol, el sistema MUST ignorarlo y asignar `USER`.
- **FR-023**: El rol MUST persistirse de forma segura y MUST NOT ser modificable por el cliente una vez creado el usuario.
- **FR-024**: System MUST garantizar la existencia de, como máximo, un usuario con rol `SUPERUSER` en la base de datos.
- **FR-025**: System MUST disponer de un mecanismo interno, seguro y explícito de inicialización del `SUPERUSER`, cuya ejecución MUST ser idempotente: ejecutarla repetidamente MUST NOT crear usuarios adicionales ni modificar el `SUPERUSER` ya existente.
- **FR-026**: La configuración del `SUPERUSER` MUST provenir del entorno de ejecución. El código y la documentación del repositorio MUST NOT contener credenciales, contraseñas ni valores por defecto de la configuración del `SUPERUSER`.
- **FR-027**: Si la inicialización del `SUPERUSER` está habilitada y su configuración está incompleta, el arranque MUST fallar de forma explícita en lugar de crear una cuenta incompleta.
- **FR-028**: El rol MUST NOT modificar el contenido ni la validación del JWT existente. El sistema MUST continuar resolviendo la identidad y el rol del usuario autenticado desde la persistencia.
- **FR-029**: El rol expuesto al cliente MUST ser únicamente informativo. Toda autorización MUST evaluarse en el backend a partir del rol persistido.
- **FR-030**: La ampliación MUST preservar las cuentas de los usuarios existentes. La migración MUST asignar a cada usuario existente un rol y un nombre de usuario válido derivado de forma determinista de sus datos actuales, y MUST NOT perder información ni invalidar sus credenciales.
- **FR-031**: La ampliación MUST preservar el inicio de sesión por correo electrónico y contraseña, la emisión y validación del JWT y los contratos HTTP existentes, salvo el carácter obligatorio del nombre de usuario en el registro y los nuevos campos de la respuesta de identidad actual.

### Key Entities *(include if feature involves data)*

- **Usuario**: Representa a un individuo registrado en el sistema.
- Attributes: Correo electrónico, Contraseña, Nombre de usuario, Rol

- **Rol de usuario**: Indica la capacidad del usuario dentro del sistema. Existen exactamente dos valores:
  - `USER`: usuario registrado público. Es el rol asignado a todo alta realizada mediante el registro público.
  - `SUPERUSER`: dueño inicial de la totalidad de los tokens de los jugadores. Es un rol singular del dominio de mercado y MUST NOT interpretarse como una administración general del sistema.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden completar con éxito el proceso de registro.
- **SC-002**: El 100% de los intentos de registro con correos electrónicos existentes son identificados y rechazados correctamente.
- **SC-003**: El 100% de las contraseñas almacenadas utilizan hashing criptográfico seguro (sin texto plano).
- **SC-004**: Cero instancias de contraseñas en texto plano aparecen en los registros del sistema o en las respuestas de la API.
- **SC-005**: La documentación de la API está 100% completa y coincide con la implementación para ambos endpoints.
- **SC-006**: Las solicitudes a recursos protegidos con un JWT válido obtienen acceso; sin token, con firma inválida o con token expirado reciben `401 Unauthorized`.
- **SC-007**: El 100% de los registros públicos válidos crean usuarios con nombre de usuario persistido, normalizado y único.
- **SC-008**: El 100% de los registros públicos rechazan nombres de usuario vacíos, con formato inválido o ya utilizados, incluidos los que solo se diferencian en mayúsculas y minúsculas.
- **SC-009**: El 100% de los usuarios creados mediante registro público tienen el rol `USER`; ningún registro público puede crear un usuario con rol `SUPERUSER`.
- **SC-010**: Existe como máximo un usuario con rol `SUPERUSER` en la base de datos, y la inicialización repetida de esa cuenta no produce usuarios adicionales ni modifica la existente.
- **SC-011**: Los usuarios existentes antes de la ampliación conservan su correo electrónico, su contraseña y su capacidad de iniciar sesión después de la migración.
- **SC-012**: El inicio de sesión con nombre de usuario en lugar de correo electrónico es rechazado en el 100% de los casos.

## Assumptions

- **Transporte seguro**: Se asume que todo el tráfico de autenticación se realiza sobre HTTPS/TLS para proteger los datos en tránsito.
- **Normalización del correo electrónico**: Se asume que los correos electrónicos serán normalizados (p. ej., convertidos a minúsculas) antes del almacenamiento y la comparación para evitar problemas de sensibilidad a mayúsculas/minúsculas.
- **Hashing estándar**: Se asume que se utilizará un algoritmo de hashing estándar de la industria (como BCrypt o Argon2), según las prácticas generales de seguridad.

## Out of Scope

- **Recuperación de contraseña**: No hay mecanismo para restablecer o recuperar contraseñas olvidadas.
- **Verificación de correo electrónico**: No hay enlaces o códigos de confirmación por correo electrónico.
- **Bloqueo de cuenta**: No hay bloqueo de cuentas después de múltiples intentos fallidos de inicio de sesión.
- **Multi-factor Authentication (MFA)**: Solo se requiere autenticación de factor único (correo electrónico/contraseña).

## Out of Scope — Ampliación

- **Modificación del nombre de usuario**: El nombre de usuario es inmutable en esta ampliación. No existe endpoint ni caso de uso para cambiarlo.
- **Sistema de permisos y roles granulares**: No se implementan permisos granulares, herencia de roles, múltiples roles por usuario ni administración general del sistema.
- **Autorización basada en el rol**: No se agregan endpoints protegidos por rol ni reglas de autorización. El rol es informativo para el cliente y queda disponible para las funcionalidades posteriores del mercado.
- **Gestión del SUPERUSER**: No existe interfaz HTTP para crear, consultar, modificar ni eliminar el `SUPERUSER`. Su existencia se gestiona por el mecanismo interno de inicialización y por la base de datos.
- **Recuperación o rotación de credenciales del SUPERUSER**: La configuración del `SUPERUSER` no se modifica en caliente ni se expone por la API.

---

## Amendments

### 2026-10-14 — Ampliación: Nombre de usuario y roles de usuario

Esta ampliación amplía una funcionalidad ya aprobada e implementada. **No modifica ni reescribe**
`FR-001` a `FR-012`, `SC-001` a `SC-006`, ni las historias de usuario originales; preserva su
historial y los mantiene vigentes. Los requisitos nuevos se añaden como `FR-013` a `FR-031` y los
criterios nuevos como `SC-007` a `SC-012`.

**Motivo.** El mercado de tokens requiere identificar de forma pública a cada usuario mediante un
nombre de usuario, y requiere identificar con confiable al único `SUPERUSER` dueño inicial de los
tokens. Ambas necesidades pertenecen al mismo agregado `User` y a la misma funcionalidad de
usuarios, por lo que se amplía SPEC-001 en lugar de crear una funcionalidad independiente.

**Cambios de contrato.** Dos cambios, ambos identificados de forma explícita:

1. `POST /api/auth/register` pasa a exigir `username`. Es un cambio incompatible respecto de los clientes que no lo envían, aceptado de forma explícita. Se actualizan los contratos, la colección Postman, la guía de validación y las pruebas correspondientes.
2. `GET /api/auth/me` suma los campos `username` y `role`. Es un cambio aditivo: los campos preexistentes `id` y `email` se conservan sin cambios.

**Lo que no cambia.** El inicio de sesión continúa usando correo electrónico y contraseña. El JWT conserva su sujeto, su firma, su vigencia y su formato. `JWTProvider`, la cadena de filtros de seguridad y el decodificador de tokens no se modifican. El rol se resuelve desde la persistencia en cada solicitud autenticada, igual que ya se resolvía la identidad del usuario.

**Historial de tareas.** Las tareas de `tasks.md` conservan su estado original, incluidas las pendientes `T010` y `T025`. El trabajo de esta ampliación se registra en una fase propia.

**Checklist.** `checklists/requirements.md` no se modifica: es el gate de calidad de la especificación original. La validación de los requisitos nuevos se registra en un checklist independiente.

### 2026-10-14 — Precisión: insensibilidad a mayúsculas del nombre de usuario en el borde HTTP

**Origen.** La verificación ejecutada de la ampliación detectó que `FR-014` y `FR-015` eran mutuamente
inconsistentes tal como estaban redactados. `FR-014` exigía el formato `^[a-z0-9._-]+$`, que rechaza
mayúsculas; `FR-015` exigía tratar como iguales los nombres que solo difieren en caja. La
implementación había resuelto el conflicto aplicando el patrón estricto en el borde HTTP, lo que
hacía la normalización inalcanzable para un cliente y contradecía el escenario de aceptación 5
(«JUAN» debe rechazarse **por duplicado**, no por formato).

**Decisión.** El nombre de usuario **admite mayúsculas y minúsculas en la solicitud** y se normaliza
a minúsculas antes de persistirse y antes de comprobar la unicidad. La forma normalizada es la única
que se almacena y la que se devuelve al cliente.

**Alcance del cambio.** Se ajustan únicamente `FR-014`, `FR-015`, el escenario de aceptación 5 y el
caso límite de sensibilidad, y se añade el escenario 6. `FR-001` a `FR-013` y `FR-016` a `FR-031` no
se modifican. No cambia ningún requisito de roles, de `SUPERUSER`, de autenticación ni de
compatibilidad: el login por correo electrónico y contraseña, el JWT y la inicialización idempotente
del `SUPERUSER` permanecen exactamente como estaban especificados.

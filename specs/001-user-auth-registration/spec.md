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

### Edge Cases

- **Case sensitivity**: ¿Cómo maneja el sistema "User@Test.com" vs "user@test.com"? (Los correos electrónicos deben tratarse como insensibles a mayúsculas para la identificación).
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
- **FR-011**: Los errores gestionados por la aplicación MUST devolver un `ErrorResponseDTO` con `timestamp`, `status`, `error`, `message` y `path`. `status` MUST coincidir con el código HTTP y `path` MUST indicar la ruta solicitada. Para errores de Jakarta Validation, `message` MUST contener el primer mensaje de validación disponible.
- **FR-012**: El login exitoso MUST responder `200 OK` con un JWT en el campo `token`. Las solicitudes a recursos protegidos MUST presentar ese token mediante `Authorization: Bearer`; el servidor MUST validar su firma y vigencia antes de permitir el acceso y responder `401 Unauthorized` si el token falta o no es válido.

### Key Entities *(include if feature involves data)*

- **Usuario**: Representa a un individuo registrado en el sistema.
- Attributes: Correo electrónico, Contraseña

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden completar con éxito el proceso de registro.
- **SC-002**: El 100% de los intentos de registro con correos electrónicos existentes son identificados y rechazados correctamente.
- **SC-003**: El 100% de las contraseñas almacenadas utilizan hashing criptográfico seguro (sin texto plano).
- **SC-004**: Cero instancias de contraseñas en texto plano aparecen en los registros del sistema o en las respuestas de la API.
- **SC-005**: La documentación de la API está 100% completa y coincide con la implementación para ambos endpoints.
- **SC-006**: Las solicitudes a recursos protegidos con un JWT válido obtienen acceso; sin token, con firma inválida o con token expirado reciben `401 Unauthorized`.

## Assumptions

- **Transporte seguro**: Se asume que todo el tráfico de autenticación se realiza sobre HTTPS/TLS para proteger los datos en tránsito.
- **Normalización del correo electrónico**: Se asume que los correos electrónicos serán normalizados (p. ej., convertidos a minúsculas) antes del almacenamiento y la comparación para evitar problemas de sensibilidad a mayúsculas/minúsculas.
- **Hashing estándar**: Se asume que se utilizará un algoritmo de hashing estándar de la industria (como BCrypt o Argon2), según las prácticas generales de seguridad.

## Out of Scope

- **Recuperación de contraseña**: No hay mecanismo para restablecer o recuperar contraseñas olvidadas.
- **Verificación de correo electrónico**: No hay enlaces o códigos de confirmación por correo electrónico.
- **Bloqueo de cuenta**: No hay bloqueo de cuentas después de múltiples intentos fallidos de inicio de sesión.
- **Multi-factor Authentication (MFA)**: Solo se requiere autenticación de factor único (correo electrónico/contraseña).

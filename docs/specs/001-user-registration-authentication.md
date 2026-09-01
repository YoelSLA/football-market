# Feature Specification: Registro y autenticación de usuarios

**Feature Branch**: `001-user-registration-authentication`

**Created**: 2026-08-31

**Status**: Approved

**Input**: User description: "Quiero una API REST para poder registrar un usuario con email y contraseña, donde el mismo una vez registrado se podrá loguearse a la aplicación con el mismo email y contraseña."

## User Scenarios & Testing

### User Story 1 - Registro de usuario (Priority: P1)

Como usuario, quiero registrarme utilizando un email y una contraseña para crear una cuenta en el sistema.

**Why this priority**: El registro es necesario para que una persona pueda crear una cuenta y posteriormente identificarse en el sistema.

**Independent Test**: Puede probarse registrando un usuario con un email que cumpla las condiciones requeridas y una contraseña no vacía, verificando que la cuenta sea creada correctamente. También puede verificarse que no sea posible registrar nuevamente un usuario utilizando un email ya registrado.

**Acceptance Scenarios**:

1. **Given** que no existe un usuario registrado con el email proporcionado, **When** se envía una solicitud de registro con un email que contiene `@` y `.com` y una contraseña no vacía, **Then** el sistema debe crear el usuario correctamente.

2. **Given** que ya existe un usuario registrado con el email proporcionado, **When** se intenta registrar nuevamente utilizando ese email, **Then** el sistema debe rechazar la solicitud.

3. **Given** que el email está vacío, no contiene `@` o no contiene `.com`, o que la contraseña está vacía, **When** se intenta registrar el usuario, **Then** el sistema debe rechazar la solicitud.

---

### User Story 2 - Inicio de sesión (Priority: P1)

Como usuario registrado, quiero iniciar sesión utilizando mi email y contraseña para identificarme en el sistema.

**Why this priority**: El inicio de sesión permite que un usuario registrado se identifique utilizando las credenciales con las que creó su cuenta.

**Independent Test**: Puede probarse utilizando un usuario previamente registrado, iniciando sesión con las credenciales correctas y verificando que la autenticación sea exitosa. También puede probarse utilizando credenciales incorrectas y verificando que la autenticación sea rechazada.

**Acceptance Scenarios**:

1. **Given** que existe un usuario registrado, **When** proporciona el email y la contraseña correspondientes a su cuenta, **Then** el sistema debe autenticar correctamente al usuario.

2. **Given** que existe un usuario registrado, **When** proporciona una contraseña que no corresponde con la de su cuenta, **Then** el sistema debe rechazar la autenticación.

3. **Given** que no existe un usuario registrado con el email proporcionado, **When** intenta iniciar sesión utilizando dicho email, **Then** el sistema debe rechazar la autenticación.

4. **Given** que los datos requeridos para iniciar sesión no fueron proporcionados correctamente, **When** se envía la solicitud, **Then** el sistema debe rechazarla.

---

## Edge Cases

- ¿Qué ocurre cuando se intenta registrar un usuario utilizando un email vacío?
- ¿Qué ocurre cuando se intenta registrar un usuario utilizando un email que no contiene `@`?
- ¿Qué ocurre cuando se intenta registrar un usuario utilizando un email que no contiene `.com`?
- ¿Qué ocurre cuando se intenta registrar un usuario utilizando una contraseña vacía?
- ¿Qué ocurre cuando el email ya se encuentra registrado?
- ¿Qué ocurre cuando se intenta iniciar sesión sin proporcionar email?
- ¿Qué ocurre cuando se intenta iniciar sesión sin proporcionar contraseña?
- ¿Qué ocurre cuando se intenta iniciar sesión con un email inexistente?
- ¿Qué ocurre cuando se intenta iniciar sesión con una contraseña incorrecta?

## Requirements

### Functional Requirements

- **FR-001**: System MUST permitir registrar un usuario mediante una solicitud que contenga un email y una contraseña.

- **FR-002**: System MUST validar que el email proporcionado no sea vacío y contenga `@` y `.com`.

- **FR-003**: System MUST validar que la contraseña proporcionada no sea vacía.

- **FR-004**: System MUST garantizar que el email de un usuario sea único dentro del sistema.

- **FR-005**: System MUST rechazar el registro cuando el email proporcionado ya se encuentre asociado a un usuario existente.

- **FR-006**: System MUST proteger las credenciales del usuario, evitando almacenar o exponer las contraseñas de forma recuperable o accesible de manera no autorizada.

- **FR-007**: System MUST permitir que un usuario registrado inicie sesión utilizando el email y la contraseña con los que fue registrado.

- **FR-008**: System MUST rechazar un intento de inicio de sesión cuando el email proporcionado no corresponda a un usuario registrado o cuando la contraseña proporcionada no coincida con la correspondiente al usuario.

### Key Entities

- **Usuario**: Representa a una persona registrada en el sistema. Se identifica mediante un email y posee las credenciales necesarias para iniciar sesión.

- **Credenciales de acceso**: Representan la información utilizada por un usuario para identificarse durante el inicio de sesión.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Un usuario debe poder registrarse correctamente proporcionando un email no vacío que contenga `@` y `.com`, junto con una contraseña no vacía.

- **SC-002**: Un usuario registrado debe poder iniciar sesión correctamente utilizando el email y la contraseña con los que fue registrado.

- **SC-003**: El sistema debe rechazar todo intento de registro que utilice un email que ya se encuentre registrado.

- **SC-004**: El sistema debe rechazar el inicio de sesión cuando el email no corresponda a un usuario registrado o cuando la contraseña proporcionada no coincida con la asociada a dicho usuario.

- **SC-005**: Las contraseñas de los usuarios no deben quedar almacenadas ni expuestas en texto plano.

## Assumptions

- El email identifica de forma única a cada usuario dentro del sistema.

- El usuario debe registrarse previamente antes de poder iniciar sesión.

- El registro y el inicio de sesión forman parte de la misma funcionalidad general de gestión de usuarios.

- Para esta feature, se considera válido un email no vacío que contenga `@` y `.com`.

- Para esta feature, se considera válida una contraseña no vacía.

- Los mecanismos de recuperación de contraseña, verificación de email, bloqueo de cuentas y límites de intentos de autenticación no forman parte del alcance de esta feature.
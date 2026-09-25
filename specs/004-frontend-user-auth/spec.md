# Feature Specification: Autenticación de usuarios en el frontend

**Feature Branch**: `dev`

**Created**: 2026-09-23

**Status**: Draft

**Input**: Autenticación de usuarios de Football Market mediante páginas independientes de registro e inicio de sesión, sesión persistente, rutas protegidas, home temporal y cierre de sesión.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Crear una cuenta (Priority: P1)

Como visitante, quiero registrarme para poder iniciar sesión con mi nueva cuenta.

**Why this priority**: Permite incorporarse a la plataforma sin depender de una cuenta preexistente.

**Independent Test**: Abrir `/register`, completar datos válidos y comprobar el regreso a `/login` con confirmación, sin sesión iniciada; repetir con email duplicado.

**Acceptance Scenarios**:

1. **Given** un visitante en `/register`, **When** envía un email válido, una contraseña de al menos 8 caracteres y una confirmación coincidente, **Then** llega a `/login`, recibe confirmación de registro y puede iniciar sesión allí.
2. **Given** un visitante en `/register`, **When** el email ya está registrado, **Then** permanece en la página y ve el mensaje devuelto por el servicio.
3. **Given** un visitante en `/register`, **When** faltan campos, el email es inválido, la contraseña tiene menos de 8 caracteres o la confirmación no coincide, **Then** recibe feedback antes del envío.
4. **Given** un visitante en `/register`, **When** usa el enlace para usuarios existentes, **Then** llega a `/login`.

---

### User Story 2 - Iniciar sesión y acceder al home (Priority: P1)

Como usuario con una cuenta, quiero iniciar sesión con email y contraseña para acceder al área privada de Football Market.

**Why this priority**: Es el acceso principal a la aplicación autenticada.

**Independent Test**: Con una cuenta existente, abrir `/login`, enviar credenciales válidas y comprobar que se llega a `/home` con la sesión activa; repetir con credenciales erróneas.

**Acceptance Scenarios**:

1. **Given** un visitante sin sesión en `/login`, **When** envía email válido y contraseña correctos, **Then** queda autenticado y llega a `/home`, donde ve una confirmación de acceso.
2. **Given** un visitante en `/login`, **When** envía credenciales incorrectas, **Then** permanece en la página y recibe un mensaje comprensible.
3. **Given** un visitante en `/login`, **When** intenta enviar campos vacíos o un email mal formado, **Then** recibe feedback de validación antes del envío.
4. **Given** un visitante en `/login`, **When** usa el enlace de creación de cuenta, **Then** llega a la página independiente `/register`.

---

### User Story 3 - Conservar y proteger la sesión (Priority: P1)

Como usuario autenticado, quiero mantener el acceso después de recargar la aplicación y evitar que una sesión vencida habilite páginas privadas.

**Why this priority**: Hace utilizable el acceso privado y evita mostrarlo a visitantes o a sesiones inválidas.

**Independent Test**: Iniciar sesión, recargar y visitar directamente las tres rutas; repetir sin sesión, con sesión expirada, con una sesión local vigente rechazada por `/me` y con una consulta de restauración temporalmente indisponible.

**Acceptance Scenarios**:

1. **Given** un JWT persistido y no expirado asociado a un usuario existente, **When** se recarga la aplicación o se abre `/home`, **Then** se comprueba la sesión mediante `GET /api/auth/me` y, tras recibir `200` con `id` y `email`, el usuario conserva el acceso.
2. **Given** ninguna sesión o una sesión expirada, **When** se intenta abrir `/home`, **Then** se llega a `/login` y el área privada no queda accesible.
3. **Given** una sesión vigente, **When** se intenta abrir `/login` o `/register`, **Then** se llega a `/home`.
4. **Given** una solicitud autenticada a un recurso protegido, **When** el servicio rechaza la autenticación con `401`, **Then** la sesión local se invalida y el usuario llega a `/login`.
5. **Given** un JWT persistido cuya expiración aún es futura, **When** `/me` responde `401`, **Then** se invalida la sesión local y el usuario llega a `/login`.
6. **Given** un JWT persistido cuya expiración aún es futura, **When** falla la comunicación con `/me`, **Then** no se concede acceso privado, se informa que la sesión no pudo comprobarse y se permite reintentar sin cerrar la sesión local por ese fallo.

---

### User Story 4 - Cerrar sesión (Priority: P1)

Como usuario autenticado, quiero cerrar sesión desde `/home` para impedir el acceso posterior con esa sesión.

**Why this priority**: Completa el ciclo de autenticación y permite salir de un dispositivo compartido.

**Independent Test**: Entrar a `/home`, cerrar sesión, recargar y volver a intentar abrir `/home`.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado en `/home`, **When** cierra sesión, **Then** llega a `/login` y ya no puede acceder a `/home` sin autenticarse nuevamente.

---

### User Story 5 - Utilizar páginas de acceso coherentes y adaptables (Priority: P2)

Como visitante, quiero formularios claros y una presentación reconocible de Football Market en escritorio.

**Why this priority**: Facilita completar las operaciones y establece una identidad visual propia.

**Independent Test**: Revisar `/login` y `/register` en pantallas amplias y estrechas, completar los formularios y comprobar sus estados de carga, éxito y error.

**Acceptance Scenarios**:

1. **Given** un escritorio, **When** se abre cualquiera de las dos páginas, **Then** se distinguen una sección visual protagonista relacionada con el mercado de jugadores y una sección independiente de formulario, con espacio en blanco, separación visual clara y una identidad compartida.
2. **Given** una pantalla estrecha, **When** se abre cualquiera de las dos páginas, **Then** todos los campos, mensajes, enlaces y acciones siguen siendo visibles y utilizables sin desplazamiento horizontal.
3. **Given** un envío en curso, **When** el usuario observa o vuelve a activar la acción principal, **Then** ve el estado de carga y no se produce un segundo envío simultáneo.

### Edge Cases

- Una contraseña de menos de 8 caracteres puede enviarse desde Login si se completan sus dos campos y el email es válido; la longitud mínima se exige solo en Register.
- Una confirmación vacía o distinta de la contraseña impide enviar Register. La confirmación no forma parte de los datos remitidos al servicio.
- Si el servicio rechaza datos con `400`, el formulario muestra feedback adecuado y conserva la oportunidad de corregirlos.
- Si hay un error inesperado o falla la comunicación, se muestra un mensaje comprensible y se permite reintentar manualmente sin alterar los datos escritos por el usuario.
- Si la información de sesión persistida está ausente, dañada o no permite determinar una vigencia futura, se trata como sesión no autenticada.
- Mientras se comprueba una sesión persistida mediante `/me`, no se muestra contenido privado ni se decide el destino de las rutas públicas como si el usuario ya estuviera autenticado o no autenticado.
- Si `/me` devuelve `200`, la identidad actual procede de sus campos `id` y `email`; el frontend no presume que los datos actuales del usuario estén contenidos en el JWT.
- La expiración mientras la aplicación está abierta elimina el acceso privado al volver a evaluar la sesión; cualquier `401` de un recurso protegido también la invalida.
- Un `401` por credenciales erróneas en Login muestra el error de acceso y no implica una redirección adicional.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La aplicación MUST ofrecer `/login` y `/register` como páginas y URL independientes, cada una con su propio formulario y enlace visible hacia la otra.
- **FR-002**: Ambas páginas MUST compartir una identidad visual de Football Market. En escritorio MUST distinguirse una sección visual amplia alusiva al mercado de jugadores y una sección de formulario claramente separada; en pantallas pequeñas ambas MUST seguir siendo utilizables sin desplazamiento horizontal. La referencia `referencias-imagenes/login-register.png` orienta la composición y el estilo, sin reproducir marca, textos o gráficos ajenos.
- **FR-003**: Login MUST solicitar email y contraseña obligatorios, validar el formato del email antes de enviar y no exigir longitud mínima a la contraseña.
- **FR-004**: Login MUST enviar únicamente `email` y `password` a `POST /api/auth/login`; ante `200` MUST tomar el `token` recibido, establecer una sesión persistente y dirigir al usuario a `/home`.
- **FR-005**: Login MUST mostrar ante `401`, `400` y fallos inesperados o de comunicación un mensaje de error visible sin obstruir ni desplazar la página completa, que preserve la experiencia de uso y permita corregir o reintentar la operación sin alterar los datos escritos.
- **FR-006**: Register MUST solicitar email, contraseña y confirmación obligatorios; MUST validar el formato del email, exigir al menos 8 caracteres a la contraseña y exigir confirmación coincidente antes de enviar.
- **FR-007**: Register MUST enviar únicamente `email` y `password` a `POST /api/auth/register`; ante `201` sin contenido MUST dirigir al usuario a `/login` con confirmación visible de registro exitoso, sin autenticarlo automáticamente.
- **FR-008**: Register MUST mostrar el mensaje devuelto por el servicio ante `409` por email registrado y, ante `400`, fallos inesperados o de comunicación, un mensaje de error visible sin obstruir ni desplazar la página completa, que preserve la experiencia de uso y permita corregir o reintentar la operación sin alterar los datos escritos.
- **FR-009**: Durante Login y Register MUST indicarse el progreso de la operación y evitar envíos simultáneos duplicados; al terminar MUST quedar claro si hubo éxito o error.
- **FR-010**: La sesión MUST persistir entre recargas. Al iniciar o recargar, el estado de autenticación MUST comenzar en `unknown` hasta determinarlo. Una sesión ausente, expirada o ilegible MUST invalidarse localmente y conducir a `anonymous`; si existe un JWT persistido con expiración futura, la aplicación MUST pasar a `checking` y consultar `GET /api/auth/me` antes de confirmar que el usuario está autenticado. La consulta MUST realizarse antes de acceder o navegar a cualquier ruta o acción protegida.
- **FR-011**: La consulta de `/me` y las demás solicitudes a recursos protegidos MUST presentar el JWT como `Authorization: Bearer <token>`. La respuesta `200` de `/me` MUST confirmar la sesión y aportar el `id` y `email` actuales del usuario. Un `401` de `/me` o de otra solicitud autenticada a un recurso protegido MUST invalidar la sesión local y dirigir a `/login`.
- **FR-012**: Mientras `/me` comprueba una sesión persistida, la aplicación MUST mantener un estado de verificación sin exponer `/home` ni aplicar redirecciones basadas en un estado de autenticación todavía indeterminado. Si falla la comunicación o surge un error inesperado, MUST informar del problema y permitir reintentar manualmente sin considerar validada la sesión ni invalidarla por ese error.
- **FR-013**: `/home` MUST ser privada: visitantes sin sesión vigente MUST llegar a `/login`. Los usuarios autenticados que intenten abrir `/login` o `/register` MUST llegar a `/home`.
- **FR-014**: `/home` MUST mostrar una confirmación de acceso correcto y una acción para cerrar sesión; MUST ser una página temporal sin funciones del mercado ni perfil de usuario.
- **FR-015**: Cerrar sesión MUST invalidar la sesión local, dirigir a `/login` e impedir volver a `/home` hasta un nuevo inicio de sesión; no depende de una operación de logout del servicio.
- **FR-016**: La aplicación MUST tratar al backend como autoridad final sobre la validez de la autenticación y MUST mantener fuera del frontend el secreto de firma de los JWT.
- **FR-017**: La feature MUST limitarse a registro, login, persistencia e invalidación de sesión, protección de rutas y home temporal; recuperación de contraseña, verificación de email, proveedores externos, perfil y funciones del mercado quedan fuera de alcance.

### Key Entities

- **Cuenta de usuario**: Identidad registrada mediante email y contraseña; el frontend no administra el perfil ni datos del mercado.
- **Sesión**: Estado de acceso derivado del JWT recibido en Login, con vigencia declarada de 12 horas y expiración incluida en él; al restaurarse, requiere la confirmación de `/me` y puede estar en verificación, activa o invalidada.
- **Usuario actual**: Identidad básica de la sesión confirmada por `/me`, compuesta por `id` y `email` obtenidos del estado actual del servicio.
- **Datos de formulario**: Email y contraseña para Login; email, contraseña y confirmación para Register. La confirmación solo sirve para validar el formulario.

## Clarificaciones

### Sesión 2026-09-23

- P: ¿Cómo debe almacenarse el JWT recibido de `POST /api/auth/login` en el frontend para persistir la sesión entre recargas del navegador? (FR-010) → R: localStorage
- P: ¿Cómo debe manejar el frontend los errores de la API durante login o registro? → R: Mostrar un mensaje de error en pantalla sin alterar los datos escritos y permitir el reintento manual.
- P: ¿Cómo debe validar el frontend la sesión antes de acceder o navegar a una ruta protegida? → R: Consultar `/me` antes de continuar hacia cualquier ruta o acción protegida; si la comunicación falla, informar y permitir reintento manual.
- P: ¿Qué nivel de accesibilidad WCAG deben cumplir `/login`, `/register` y el flujo de autenticación? → R: Sin requisito formal de accesibilidad (fuera de alcance)
- P: Dado que el JWT se devuelve en el cuerpo JSON (no cookie HttpOnly) y se almacena en localStorage, ¿qué estrategia de protección CSRF debe aplicar el frontend? → R: Ninguna medida adicional en frontend (confiar en header Authorization: Bearer; navegadores no lo envían automáticamente cross-site)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En una revisión de los flujos descritos, el 100 % de los registros válidos termina en `/login` con confirmación visible y sin acceso automático a `/home`.
- **SC-002**: En una revisión de los flujos descritos, el 100 % de los inicios de sesión válidos llega a `/home` y conserva el acceso tras una recarga cuando `/me` confirma que la sesión sigue vigente y asociada a un usuario existente.
- **SC-003**: En los escenarios de acceso directo a las tres rutas, el 100 % de las sesiones ausentes, expiradas o cerradas queda fuera de `/home`, y el 100 % de las sesiones vigentes queda fuera de las páginas públicas de autenticación.
- **SC-004**: En las revisiones de escritorio y móvil de Login y Register, el 100 % de campos, mensajes y acciones principales permanece legible y operable sin desplazamiento horizontal.
- **SC-005**: En los escenarios de error conocidos, carga y pérdida de comunicación descritos, el usuario recibe feedback visible en el 100 % de los casos y no se procesa más de un envío simultáneo por formulario.
- **SC-006**: En los escenarios de restauración descritos, el 100 % de las respuestas `401` de `/me` elimina el acceso privado; el 100 % de los fallos de comunicación mantiene el acceso privado suspendido hasta que una nueva comprobación confirme la sesión.

## Assumptions

- El contrato existente de autenticación mantiene `POST /api/auth/login` con respuesta `200` y campo `token`, y `POST /api/auth/register` con respuesta `201` sin contenido. El error gestionado por registro expone un mensaje en la respuesta.
- `GET /api/auth/me` está disponible para comprobar una sesión persistida y devuelve `200` con el `id` y `email` actuales del usuario; responde `401` sin cuerpo si la sesión no es válida o el usuario ya no existe. El home temporal solo necesita confirmar el acceso, sin incorporar un perfil.
- El comportamiento de la ruta inicial `/` y de rutas ajenas a `/login`, `/register` y `/home` pertenece al enrutamiento general y queda fuera del alcance de esta especificación.

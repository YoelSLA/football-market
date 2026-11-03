# Modelo de datos

> **Actualizado 2026-10-14** conforme a la ampliación de SPEC-001. Ver
> [spec.md](spec.md) §Amendments. El modelo persiste, no se reescribe.

## Entidades

### Cuenta de usuario
- `username`: nombre de usuario público de la cuenta; obligatorio y único sin distinguir mayúsculas de minúsculas.
- `id`: identificador del usuario.
- `email`: correo de la cuenta.
- `password`: credencial mantenida únicamente en el formulario de login/registro; ante un error se conserva para permitir corregir o reintentar según FR-005/FR-008, y se descarta al completar la operación o abandonar el formulario.
- `role`: rol del usuario, `USER` o `SUPERUSER`, recibido como dato informativo desde `/me`.
- La contraseña no se almacena en estado persistente.

### Sesión
- `token`: JWT persistido en `localStorage` mediante adaptador de infraestructura.
- `expiresAt`: expiración futura requerida para intentar restaurar.
- `status`: `unknown` durante la inicialización previa a determinar la restauración; `checking` durante `/me`; `authenticated` tras `200`; `anonymous` sin token restaurable o tras `401`.
- Un fallo de comunicación mantiene `checking` y la restauración queda suspendida; permite reintentar sin invalidar el token.

### Usuario actual
- `id`: identificador numérico devuelto por `GET /api/auth/me`.
- `username`: nombre de usuario actual devuelto por `/me`, ya normalizado a minúsculas por el backend.
- `email`: correo actual devuelto por `/me`.
- `role`: rol actual devuelto por `/me`, con valores `USER` y `SUPERUSER`. Es informativo y no habilita capacidades en el frontend.

### Formularios
- Login: `email`, `password`.
- Register: `username`, `email`, `password`, `passwordConfirmation`; este último solo valida y no se envía.

## Validaciones

- Email obligatorio y con formato válido.
- Login: contraseña obligatoria, sin longitud mínima.
- Register: nombre de usuario obligatorio, de entre 3 y 30 caracteres y formado por letras, números, punto, guion y guion bajo; se aceptan mayúsculas y minúsculas y el valor se envía tal como fue escrito, porque la normalización a minúsculas la realiza el backend. Contraseña de al menos 8 caracteres y confirmación coincidente.
- Todos los formularios muestran progreso, evitan envíos simultáneos y, tras un error, permiten reintento manual sin alterar los datos escritos.

## Estados y transiciones

`unknown → checking → authenticated`: existe JWT con expiración futura y `/me` responde `200`.
`unknown → anonymous`: no existe JWT restaurable o el JWT está expirado o es ilegible.
`checking → anonymous`: `/me` responde `401`.
`checking → checking`: error de comunicación; se informa y se ofrece reintento.
`authenticated → anonymous`: logout local o `401` de recurso protegido.
`authenticated → home`: acceso confirmado.

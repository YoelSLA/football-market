# Modelo de datos

## Entidades

### Cuenta de usuario
- `id`: identificador del usuario.
- `email`: correo de la cuenta.
- `password`: credencial solo durante login/registro; nunca se conserva después de enviar.
- La contraseña no se almacena en estado persistente.

### Sesión
- `token`: JWT persistido en `localStorage` mediante adaptador de infraestructura.
- `expiresAt`: expiración futura requerida para intentar restaurar.
- `status`: `unknown` durante la inicialización previa a determinar la restauración; `checking` durante `/me`; `authenticated` tras `200`; `anonymous` sin token restaurable o tras `401`.
- Un fallo de comunicación mantiene `checking` y la restauración queda suspendida; permite reintentar sin invalidar el token.

### Usuario actual
- `id`: identificador devuelto por `GET /api/auth/me`.
- `email`: correo actual devuelto por `/me`.

### Formularios
- Login: `email`, `password`.
- Register: `email`, `password`, `passwordConfirmation`; este último solo valida y no se envía.

## Validaciones

- Email obligatorio y con formato válido.
- Login: contraseña obligatoria, sin longitud mínima.
- Register: contraseña de al menos 8 caracteres y confirmación coincidente.
- Todos los formularios muestran progreso, evitan envíos simultáneos y, tras un error, permiten reintento manual sin alterar los datos escritos.

## Estados y transiciones

`unknown → checking → authenticated`: existe JWT con expiración futura y `/me` responde `200`.
`unknown → anonymous`: no existe JWT restaurable o el JWT está expirado o es ilegible.
`checking → anonymous`: `/me` responde `401`.
`checking → checking`: error de comunicación; se informa y se ofrece reintento.
`authenticated → anonymous`: logout local o `401` de recurso protegido.
`authenticated → home`: acceso confirmado.

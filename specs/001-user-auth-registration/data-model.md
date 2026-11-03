# Data Model: User Registration and Authentication

## Entidades

### Usuario (User)

Entidad que representa a un usuario registrado en el sistema.

| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| `id` | Long | PK, Auto-increment | ID único generado por la base de datos |
| `email` | String | Único, No vacío, Indexado | Normalizado a minúsculas |
| `password` | String | No nulo | Hash BCrypt; la contraseña de registro debe tener al menos 8 caracteres antes del hashing |

## Reglas de Validación

- **Email**: Debe tener un formato de correo electrónico válido y ser único en la tabla de usuarios.
- **Password**: La contraseña recibida en el registro no debe estar vacía y debe tener al menos 8 caracteres. Antes de persistirla se transforma en un hash BCrypt; la contraseña en texto plano no se devuelve ni se guarda.
- **Normalización**: El campo `email` debe convertirse a minúsculas antes de almacenarse y antes de realizar consultas de login o comprobaciones de duplicados.

## Ampliación: Nombre de usuario y rol

### Atributos añadidos a Usuario (User)

| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| `username` | String | No vacío, Único, Indexado, 3 a 30 caracteres | Normalizado a minúsculas; solo letras, números, punto, guion y guion bajo |
| `role` | UserRole | No nulo | Enum persistido como texto (`EnumType.STRING`) |

### Rol de usuario (UserRole)

Conjunto cerrado de dos valores, sin extensible prevista en esta funcionalidad.

| Valor | Significado |
|-------|-------------|
| `USER` | Usuario registrado público. Rol asignado a toda alta del registro público. |
| `SUPERUSER` | Dueño inicial de la totalidad de los tokens de los jugadores. Rol singular del dominio de mercado; no equivale a una administración general del sistema. |

### Reglas de validación añadidas

- **Username obligatorio**: `User` MUST rechazar un nombre de usuario `null` o vacío al construirse.
- **Formato**: `User` MUST validar que el nombre de usuario tenga entre 3 y 30 caracteres y que esté formado únicamente por letras, números, punto, guion y guion bajo. Los espacios, tildes y demás caracteres no admitidos MUST ser rechazados.
- **Normalización**: el nombre de usuario MUST convertirse a minúsculas antes de almacenarse y antes de comprobar duplicados. La comparación MUST ser insensible a mayúsculas y minúsculas. El valor normalizado es el único que se persiste y el único que se expone al cliente.
- **Unicidad**: garantizada por restricción `UNIQUE` sobre la columna y comprobada en el servicio para devolver un error legible antes de llegar a la base de datos.
- **Rol obligatorio y único**: cada usuario tiene exactamente un rol. `User` MUST inicializar el rol en `USER`, de modo que ninguna alta quede sin rol. El rol no se recibe desde el cliente ni se modifica por el cliente.
- **Inmutabilidad**: dentro de esta funcionalidad el nombre de usuario y el rol no se modifican una vez creado el usuario.

### Restricciones de unicidad del SUPERUSER

- La base de datos MUST impedir la existencia de dos usuarios con rol `SUPERUSER` mediante un índice único parcial sobre la condición `role = 'SUPERUSER'`. Es una red de seguridad a nivel de motor, independiente de la aplicación.
- La aplicación MUST además garantizar la unicidad mediante una inicialización idempotente: antes de crear, consulta si ya existe el `SUPERUSER` y, si existe, no crea otro ni lo modifica.

### Migración de usuarios existentes

Los usuarios previos a la ampliación no tienen `username` ni `role`. La migración:

- añade ambas columnas de forma que la tabla existente siga siendo válida mientras se completa el relleno;
- asigna `USER` a todos los usuarios existentes;
- deriva un nombre de usuario determinista de los datos actuales de cada usuario, sin generar valores aleatorios y sin intervención manual;
- declara las columnas `NOT NULL` una vez completados los rellenos anteriores;
- preserva `id`, `email` y `password`, de modo que los usuarios existentes conservan su acceso.

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

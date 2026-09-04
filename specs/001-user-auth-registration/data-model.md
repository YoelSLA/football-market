# Data Model: User Registration and Authentication

## Entidades

### Usuario (User)

Entidad que representa a un usuario registrado en el sistema.

| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| `id` | Long | PK, Auto-increment | ID único generado por la base de datos |
| `email` | String | Único, No vacío, Indexado | Normalizado a minúsculas |
| `password` | String | No vacío | Hash seguro mediante BCrypt |

## Reglas de Validación

- **Email**: Debe tener un formato de correo electrónico válido y ser único en la tabla de usuarios.
- **Password**: No debe estar vacío y debe almacenarse como hash.
- **Normalización**: El campo `email` debe convertirse a minúsculas antes de almacenarse y antes de realizar consultas de login o comprobaciones de duplicados.
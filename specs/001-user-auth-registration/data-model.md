# Data Model: User Registration and Authentication

## Entidades

### Usuario (User)

Entidad que representa a un usuario registrado en el sistema.

| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| `id` | Long | PK, Auto-increment | ID único generado por la DB |
| `email` | String | Único, No vacío, Indexado | Normalizado a minúsculas |
| `password` | String | No vacío | Hash seguro (BCrypt) |

## Reglas de Validación

- **Email**: Formato de correo electrónico válido; único en la tabla `usuarios`.
- **Password**: No vacío (almacenado como hash).
- **Normalización**: El campo `email` se convertirá a minúsculas antes de guardar y antes de consultar (para login/duplicados).

# Data Model: Current Authenticated User

## User

Entidad persistida existente que representa una cuenta registrada.

| Campo | Tipo conceptual | Restricciones relevantes |
|---|---|---|
| `id` | Entero de 64 bits | Identificador generado, único y no nulo para usuarios persistidos. |
| `email` | Texto | Obligatorio, válido, único y normalizado según las reglas actuales de autenticación. Es la identidad referenciada por el `sub` del JWT. |
| `password` | Texto protegido | Obligatorio y almacenado como hash. No participa en esta operación ni puede aparecer en la respuesta. |

### Relaciones

- El `sub` de una sesión JWT referencia conceptualmente a un User mediante `email`.
- No se persiste una entidad Session ni una relación adicional.

### Reglas de lectura

- El email sujeto se normaliza con la misma regla utilizada por registro e inicio de sesión.
- Debe existir exactamente un User para producir una respuesta exitosa.
- Los valores devueltos de `id` y `email` proceden del User recuperado, no de claims adicionales.
- Si no existe el User, la sesión no es válida para esta operación y el resultado HTTP es `401` vacío.

### Transiciones

Esta feature no crea ni modifica estado. Solo observa el estado persistido actual.

## Authenticated Session

Concepto no persistido que representa una solicitud con JWT validado.

| Dato | Origen | Uso |
|---|---|---|
| `subject` | Claim estándar `sub` | Clave de búsqueda del User persistido. |
| vigencia | Claims temporales validados por seguridad | Determina si la solicitud puede alcanzar el caso de uso. |
| autenticidad | Firma validada por seguridad | Garantiza que el token es aceptado por el sistema. |

### Estados observables

1. **No autenticada**: token ausente, inválido, expirado o sin sujeto utilizable → `401` vacío.
2. **Token válido sin User**: autenticidad criptográfica válida, pero identidad sin registro persistido → `401` vacío.
3. **Autenticada con User**: token válido y User existente → `200` con `id` y `email`.

## Current User Response

Contrato de salida no persistido.

| Campo | Tipo JSON | Fuente | Restricción |
|---|---|---|---|
| `id` | integer (`int64`) | `User.id` | Obligatorio. |
| `email` | string (`email`) | `User.email` | Obligatorio. |

No admite campos adicionales y nunca contiene password, token u otros datos del User.

## Impacto de persistencia

- Migraciones: ninguna.
- Tablas nuevas: ninguna.
- Índices o constraints nuevos: ninguno.
- Repositories nuevos: ninguno.

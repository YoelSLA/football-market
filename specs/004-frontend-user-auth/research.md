# Investigación técnica

## Decisiones resueltas

- **Estado de autenticación**: usar un contexto/hook de autenticación dentro de la feature, con estados `unknown`, `checking`, `authenticated` y `anonymous`; el bootstrap consultará `/api/auth/me` antes de resolver rutas protegidas.
- **Persistencia**: encapsular `localStorage` mediante un adaptador público de `infrastructure`; solo el servicio de autenticación lo utilizará para leer y escribir el JWT.
- **Cliente HTTP**: usar Axios ya instalado mediante `shared/http`; el interceptor de la feature incorporará `Authorization: Bearer <token>` y la invalidación funcional de sesión se maintained en el flujo de autenticación.
- **Validación**: usar React Hook Form, Zod y `@hookform/resolvers` ya instalados; los schemas vivirán en `features/auth/form` y los modelos de formulario no incluirán la confirmación en las operaciones remotas.
- **Estado remoto**: usar TanStack Query para login, registro y `/me`; la persistencia y decisión de navegación serán responsabilidad de hooks de autenticación, no de componentes o servicios.
- **Contrato técnico**: el Service encapsulará DTOs, endpoints y mappers; la UI consumir únicamente modelos.

## Justificación

La arquitectura del frontend exige features aisladas, flujo `Page/Component → Hook → Service`, adaptador de infraestructura para almacenamiento y separación DTO/Model. Las dependencias existentes cubren las necesidades sin introducir librerías.

## Alternativas consideradas

- Mantener el estado únicamente en React Context sin TanStack Query: no permite coordinar de forma consistente las mutaciones y lecturas remotas ya contempladas por el stack.
- Guardar el token directamente desde componentes o servicios: rechazado por la matriz arquitectónica y por acoplamiento al navegador.
- Usar cookies HttpOnly: incompatible con el contrato actual, que devuelve el token en JSON y exige `localStorage`.

## Riesgos y decisiones pendientes

- La respuesta de error del backend para `400`, `409` y otros errores debe normalizarse en la capa HTTP sin convertir errores funcionales en errores técnicos. Los fallos de API muestran un mensaje y permiten reintento manual sin alterar los datos escritos.
- La sesión se valida mediante `/me` antes de acceder o navegar a cualquier ruta o acción protegida. Los fallos de comunicación mantienen el acceso privado suspendido y ofrecen reintento manual; el backend sigue siendo la autoridad.

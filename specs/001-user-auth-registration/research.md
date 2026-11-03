# Research Report: User Registration and Authentication

## Decisiones y Rationale

- **Decision**: Utilizar Spring Security para proteger rutas y validar JWT Bearer.

- **Rationale**: Spring Security protege las rutas no públicas y valida los tokens mediante OAuth2 Resource Server. El login comprueba las credenciales en `AuthenticationServiceImpl`.

- **Alternatives considered**: No se incorporarán mecanismos de autenticación externos al stack definido por la Constitution.

- **Decision**: Utilizar Spring Security OAuth2 Resource Server y `NimbusJwtDecoder` para validar los JWT firmados con HS256.

- **Rationale**: Spring Security OAuth2 Resource Server forma parte del stack tecnológico definido y está destinado a la validación de tokens JWT dentro de la aplicación.

- **Alternatives considered**: No se incorporó una solución externa para la validación de JWT.

- **Decision**: Utilizar JJWT para generar JWT con la misma clave HMAC que utiliza el decoder.

- **Rationale**: `JWTProvider` firma los tokens con HS256 y `SecurityConfig` comparte la clave con el decoder. Spring Security comprueba firma y vigencia al recibir `Authorization: Bearer <token>`.

- **Decision**: Utilizar BCrypt para el hashing de contraseñas.

- **Rationale**: Las contraseñas deben almacenarse de forma segura y nunca en texto plano. BCrypt permite almacenar las contraseñas mediante un algoritmo de hashing diseñado para este propósito y dificulta ataques de fuerza bruta.

- **Alternatives considered**: Se consideró utilizar Argon2, pero se mantendrá BCrypt como mecanismo de hashing definido para esta funcionalidad.

- **Decision**: Normalizar el email a minúsculas antes de realizar las operaciones de registro y autenticación.

- **Rationale**: Permite mantener una representación consistente del email y facilita el control de unicidad de las cuentas, de acuerdo con `FR-004`.

- **Alternatives considered**: Mantener el email sin normalización, descartado debido al riesgo de inconsistencias en la comparación y control de unicidad.

- **Decision**: Responder `201 Created` sin body al registrar y usar `ErrorResponseDTO` para errores de aplicación.

- **Rationale**: El registro crea un recurso sin devolver representación. `ErrorResponseDTO` unifica `timestamp`, `status`, `error`, `message` y `path`; el manejo de Jakarta Validation toma el primer mensaje disponible.

## Pendientes Resueltos

- **Validación de JWT**: Se utiliza Spring Security OAuth2 Resource Server para comprobar firma y vigencia de los JWT Bearer.

- **Generación y firma de JWT**: `JWTProvider` utiliza JJWT y HS256. `SecurityConfig` define la clave compartida y `NimbusJwtDecoder` la utiliza para validar los tokens.

- **Hashing de contraseñas**: Se utilizará BCrypt para generar y verificar los hashes de las contraseñas.

- **Migraciones de base de datos**: Las modificaciones necesarias del esquema deberán gestionarse mediante Flyway en `backend/src/main/resources/db/migration/`.

- **Organización de código**: `AuthenticationController` está en `footballmarket.controllers`; los DTO en `footballmarket.controllers.dtos.requests` y `.responses`; `UserMapper` en `footballmarket.controllers.mappers`; `GlobalExceptionHandler` en `footballmarket.controllers.exceptions`.

- **Separación de responsabilidades**: El Controller gestionará únicamente responsabilidades HTTP, el Service concentrará la lógica de negocio y el Repository gestionará el acceso a persistencia.

## Resumen de Investigación

Las decisiones técnicas necesarias para implementar el registro y la autenticación se alinean con el stack tecnológico definido por la Constitution y con la arquitectura existente del backend.

La autenticación utiliza `AuthenticationServiceImpl`; JJWT genera los tokens y Spring Security OAuth2 Resource Server los valida. La clave HMAC se configura en `SecurityConfig` y se comparte con `JWTProvider`.

Las contraseñas serán almacenadas utilizando BCrypt y los emails serán normalizados antes de las operaciones de registro y autenticación para mantener consistencia y controlar la unicidad.

Las modificaciones de persistencia que sean necesarias se realizarán mediante Flyway. La implementación mantendrá la separación entre Controller, Service, Repository y Model/Entity, y utilizará los DTOs y mappers definidos para la funcionalidad.

---

# Research Report: Ampliación — Nombre de usuario y roles

**Input**: Ampliación de la funcionalidad aprobada el 2026-10-14 (User Stories 3 y 4, `FR-013` a
`FR-031`). No sustituye las decisiones anteriores: se añade a este mismo documento.

## Username

- **Decision**: Almacenar el nombre de usuario normalizado a minúsculas y compararlo siempre en esa
  forma.

- **Rationale**: Consigue insensibilidad a mayúsculas y minúsculas con una única columna y una
  única restricción `UNIQUE`. Evita mantener una columna con el valor mostrado y otra con el valor
  de comparación, o un índice único sobre una expresión, que son soluciones más complejas para un
  beneficio funcional nulo en este dominio: la identidad pública del usuario se muestra siempre en
  minúsculas.

- **Alternatives considered**: Conservar el case original y crear un índice único sobre
  `lower(username)`. Descartado por complejidad de persistencia y por la necesidad de dos
  representaciones del mismo dato.

- **Decision**: Aplicar en el modelo las invariantes de formato y longitud
  (`^[a-z0-9._-]+$`, 3 a 30 caracteres) y en el servicio la unicidad.

- **Rationale**: Mantiene la separación establecida por la Constitution: las invariantes del
  agregado viven en el Model/Entity y la comprobación de duplicados, que consulta la base de
  datos, vive en el Service. Es el mismo reparto que ya usa el correo electrónico.

- **Alternatives considered**: Validar el formato solo con anotaciones de Jakarta en el DTO. Se
  descarta como única validación, porque dejaría las invariantes sin proteger ante otros caminos
  de construcción del agregado.

- **Decision**: Mantener el nombre de usuario inmutable en esta ampliación.

- **Rationale**: Evita un endpoint nuevo, reglas de reasignación y colisión de unicidad, y posibles
  incidencias de autorización en el JWT. Se documenta como mejora futura.

- **Alternatives considered**: Endpoint de cambio de nombre de usuario. Descartado por alcance.

- **Decision**: `username` obligatorio en `RegisterRequestDTO`, aceptando el cambio incompatible.

- **Rationale**: Una cuenta sin nombre de usuario público no cumple el objetivo de la historia.
  Mantenerlo opcional obligaría a modelar un estado intermedio que ninguna otra funcionalidad
  necesita. El coste es acotado: se actualizan la colección Postman, la guía de validación y las
  pruebas.

- **Alternatives considered**: Período de compatibilidad con clientes antiguos. Descartado por
  decisión del solicitante.

## Roles

- **Decision**: Representar el rol con un enum de dominio persistido como texto
  (`EnumType.STRING`).

- **Rationale**: Es el mecanismo ya establecido en el backend para los nueve enums de
  `models/enums`. Persiste valores legibles y evita acoplar el esquema a los ordinales del enum.

- **Alternatives considered**: Tabla de roles con claves foráneas, o colección de roles por
  usuario. Descartados: la funcionalidad requiere exactamente un rol por usuario y dos valores
  cerrados. Sobrediseñarían el dominio.

- **Decision**: Inicializar el rol en `USER` como invariante del modelo, no como decisión del
  servicio ni valor por defecto de la base de datos.

- **Rationale**: Cualquier construcción de `User` produce un usuario con rol, incluidos los
  caminos futuros que agreguen altas. Ubicarlo en el servicio podría omitir la asignación.

- **Alternatives considered**: Que el servicio asigne el rol explícitamente. Funciona, pero deja la
  invariante fuera del agregado. Valor por defecto en la columna: no expresa la regla de dominio.

- **Decision**: No aceptar el rol en los datos de registro y omitirlo de `RegisterRequestDTO`.

- **Rationale**: El cliente no puede solicitar un rol, ni siquiera uno válido. La asignación es
  consecuencia del registro, no un parámetro de la operación.

## SUPERUSER

- **Decision**: Crear el `SUPERUSER` mediante un componente de inicialización de arranque
  idempotente, configurado mediante propiedades del del entorno de ejecución.

- **Rationale**: Permite inicializar una base de datos vacía y también en bases que ya contienen
  usuarios, sin credenciales fijas en el repositorio y sin endorsement de una request HTTP
  administrativa. Es idempotente: busca el usuario antes de crear, por lo que reiniciar la
  aplicación no produce duplicados ni modifica la cuenta existente.

- **Alternatives considered**:
  - *Migración SQL con contraseña hasheada fija*: descartada. Obliga a versionar un hash en el
    repositorio, cuyo texto plano es recuperable, y aplica el mismo secreto a todos los
    entornos.
  - *Endpoint HTTP de administración*: descartada. Introduce superficie pública y otra política de
    autorización en una funcionalidad que no la necesita.
  - *Script o CLI bajo demanda*: descartada como mecanismo principal porque exige un paso manual
    en cada entorno y es fácil de olvidar. Su ventaja, no crear nada sin una orden explícita, se
    conserva mediante el interruptor `enabled`.

- **Decision**: La inicialización está deshabilitada por defecto y, si se habilita con la
  configuración incompleta, el arranque falla de forma explícita.

- **Rationale**: Un ambiente mal configurado no debe arrancar creando una cuenta insegura ni
  continuar sin el `SUPERUSER` que el mercado requiere. Se sigue el criterio de la Constitution §11
  sobre propiedades que deben impedir el startup.

- **Decision**: Reforzar la unicidad con un índice único parcial sobre `role = 'SUPERUSER'` en la
  base de datos.

- **Rationale**: La aplicación garantiza la unicidad por diseño, pero una restricción a nivel de
  motor la vuelve una invariante del dato y no una convención del código.

- **Alternatives considered**: Solo una comprobación en la aplicación. Insuficiente ante inserciones
  directas o errores de despliegue.

- **Decision**: El `SUPERUSER` puede iniciar sesión con el flujo habitual de correo electrónico y
  contraseña.

- **Rationale**: El mercado opera contra esa cuenta, y separar su autenticación agregaría un
  mecanismo nuevo sin requisitos que lo justifiquen.

- **Alternatives considered**: Cuenta de servicio sin login. Descartada por sobreingeniería; se
  revisitará solo si aparece un requisito que la exija.

## Autenticación y compatibilidad

- **Decision**: No modificar el JWT. El rol se resuelve desde la persistencia en cada solicitud
  autenticada.

- **Rationale**: `PersistedUserJwtAuthenticationConverter` ya consulta la base de datos en cada
  petición para validar la identidad del usuario. Incluir el rol en el token aportaría cero
  información sin consulta adicional, pero sí un riesgo de obsolescencia: un cambio de rol no
  invalidaría los tokens emitidos hasta su expiración. La decisión también evita tocar
  `JWTProvider` y `SecurityConfig`.

- **Alternatives considered**: Claim `role` en el JWT. Descartada por la razón anterior.

- **Decision**: No agregar reglas de autorización por rol ni endpoints protegidos por rol.

- **Rationale**: No existe todavía ningún endpoint de mercado que las requiera. Fijar ahora una
  política de autorización sin casos de uso reales sería anticipar decisiones de Sprint 2. El rol
  queda disponible y se documenta la dependencia.

- **Alternatives considered**: `hasRole("SUPERUSER")` sobre un endpoint nuevo. Descartada por
  alcance.

## Migración de usuarios existentes

- **Decision**: Derivar el nombre de usuario de los usuarios existentes de forma determinista a
  partir de sus datos actuales, y asignarles el rol `USER`.

- **Rationale**: Garantiza unicidad sin generar valores aleatorios, sin intervention manual y sin
  depender del volumen de datos. Preserva `id`, `email` y `password`, de modo que las cuentas
  existentes siguen siendo utilizables.

- **Alternatives considered**: Dejar `username` nullable hasta que cada usuario lo elija.
  Descartado porque debilita la invariante de que todo usuario tiene nombre de usuario y traslada
  el problema a cada cliente.

Las decisiones reflejan la implementación actual y los contratos de la API.

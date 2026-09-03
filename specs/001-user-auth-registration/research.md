# Research Report: User Registration and Authentication

## Decisiones y Rationale

- **Decision**: Utilizar Spring Security para implementar la autenticación y autorización de usuarios.

- **Rationale**: Spring Security forma parte del stack tecnológico definido para el backend y proporciona los mecanismos necesarios para integrar la autenticación con la API REST.

- **Alternatives considered**: No se incorporarán mecanismos de autenticación externos al stack definido por la Constitution.

- **Decision**: Utilizar Spring Security OAuth2 Resource Server para la validación de los JWT.

- **Rationale**: Spring Security OAuth2 Resource Server forma parte del stack tecnológico definido y está destinado a la validación de tokens JWT dentro de la aplicación.

- **Alternatives considered**: No se incorporará una solución externa para la validación de JWT, manteniendo esta responsabilidad dentro de Spring Security.

- **Decision**: Utilizar BCrypt para el hashing de contraseñas.

- **Rationale**: Las contraseñas deben almacenarse de forma segura y nunca en texto plano. BCrypt permite almacenar las contraseñas mediante un algoritmo de hashing diseñado para este propósito y dificulta ataques de fuerza bruta.

- **Alternatives considered**: Se consideró utilizar Argon2, pero se mantendrá BCrypt como mecanismo de hashing definido para esta funcionalidad.

- **Decision**: Normalizar el email a minúsculas antes de realizar las operaciones de registro y autenticación.

- **Rationale**: Permite mantener una representación consistente del email y facilita el control de unicidad de las cuentas, de acuerdo con `FR-004`.

- **Alternatives considered**: Mantener el email sin normalización, descartado debido al riesgo de inconsistencias en la comparación y control de unicidad.

## Pendientes Resueltos

- **Validación de JWT**: Se utilizará `Spring Security OAuth2 Resource Server`, dependencia definida en el stack tecnológico del proyecto, para la validación de los tokens JWT.

- **Generación y firma de JWT**: El stack tecnológico proporcionado define `Spring Security OAuth2 Resource Server` para la validación de JWT, pero no especifica explícitamente un componente o dependencia independiente para la generación y firma de los tokens. Esta decisión deberá resolverse durante la implementación respetando las dependencias establecidas por la Constitution y sin incorporar dependencias no autorizadas.

- **Hashing de contraseñas**: Se utilizará BCrypt para generar y verificar los hashes de las contraseñas.

- **Migraciones de base de datos**: Las modificaciones necesarias del esquema deberán gestionarse mediante Flyway en `backend/src/main/resources/db/migration/`.

- **Organización de código**: `AuthController` se ubicará bajo `footballmarket.controller.auth`. Los DTOs de requests y responses se ubicarán bajo `footballmarket.controller.dtos` y los mappers bajo `footballmarket.controller.mapper`.

- **Separación de responsabilidades**: El Controller gestionará únicamente responsabilidades HTTP, el Service concentrará la lógica de negocio y el Repository gestionará el acceso a persistencia.

## Resumen de Investigación

Las decisiones técnicas necesarias para implementar el registro y la autenticación se alinean con el stack tecnológico definido por la Constitution y con la arquitectura existente del backend.

La autenticación utilizará Spring Security y la validación de los JWT se realizará mediante Spring Security OAuth2 Resource Server. El mecanismo concreto para la generación y firma de los JWT no queda definido explícitamente en el stack tecnológico proporcionado y deberá resolverse durante la implementación sin introducir dependencias que contradigan la Constitution.

Las contraseñas serán almacenadas utilizando BCrypt y los emails serán normalizados antes de las operaciones de registro y autenticación para mantener consistencia y controlar la unicidad.

Las modificaciones de persistencia que sean necesarias se realizarán mediante Flyway. La implementación mantendrá la separación entre Controller, Service, Repository y Model/Entity, y utilizará los DTOs y mappers definidos para la funcionalidad.

Con estas decisiones documentadas, la funcionalidad puede avanzar a la fase de diseño del modelo de datos y los contratos de la API.
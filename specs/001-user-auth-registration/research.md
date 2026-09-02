# Research Report: User Registration and Authentication

## Decisiones y Rationale

- **Decision**: Uso de Spring Security OAuth2 (JOSE/JWT) para la autenticación y autorización.
- **Rationale**: Es el estándar de la industria para aplicaciones Spring Boot con Spring Security 7.0, cumpliendo con los requisitos de seguridad transversal (Principio 8). Permite una gestión de sesiones sin estado, ideal para APIs REST.
- **Alternatives considered**: Sesiones basadas en servidor (Stateful) rechazadas por no ser escalables para un API REST. JJWT rechazado a favor de las herramientas integradas en Spring Security.

- **Decision**: Uso de BCrypt para el hashing de contraseñas.
- **Rationale**: Algoritmo robusto y ampliamente soportado que previene ataques de fuerza bruta y Rainbow Tables.
- **Alternatives considered**: Argon2.

- **Decision**: Normalización de email (lowercase) antes de cualquier operación.
- **Rationale**: Garantiza la unicidad y consistencia en la base de datos (Principio 8 y FR-004).

## Pendientes Resueltos

- **Librería JWT**: Se utilizará `Spring Security OAuth2 JOSE` (Nimbus JOSE + JWT).
- **Estructura de migraciones**: Se utilizará Flyway para gestionar las migraciones de base de datos (`backend/src/main/resources/db/migration/`).
- **Organización de Código**: `AuthController` y DTOs específicos bajo `footballmarket.controller.auth`, con `mapper` dentro de `controller`.

## Resumen de Investigación
La arquitectura está clara y las herramientas tecnológicas elegidas cumplen con la Constitución y los requisitos funcionales, incluyendo versiones específicas (Spring Boot 4.1.1, Java 21, etc.). Se procederá con la fase de diseño.

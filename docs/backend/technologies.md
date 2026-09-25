# Tecnologías del backend

## Guía de lectura

Consultar este documento para conocer el stack, las tecnologías aprobadas, sus versiones o su propósito, y en toda tarea que agregue, elimine, sustituya o actualice una tecnología o dependencia del backend. Para estructura, responsabilidades y dependencias arquitectónicas, consultar la [arquitectura del backend](architecture.md).

Las versiones efectivas se obtienen de `backend/build.gradle.kts`, `backend/gradle/libs.versions.toml`, el Gradle Wrapper y la configuración del proyecto. Esas fuentes tienen precedencia sobre este documento. Cuando Spring Boot administra una versión que el proyecto no fija individualmente, la tabla lo indica sin inferir un número.

| Tecnología | Versión | Uso en el proyecto |
| ---------- | ------- | ------------------ |
| Java | 21 | Lenguaje y toolchain del backend; la CI configura JDK 21 con distribución Zulu. |
| Gradle y Kotlin DSL | 9.6.0 (Wrapper) | Construcción, gestión de plugins y dependencias, ejecución de controles y empaquetado del backend mediante scripts Kotlin. |
| Spring Dependency Management Plugin | 1.1.7 | Aplica la gestión de versiones de dependencias provista por Spring Boot. |
| Spring Boot | 4.1.1 | Arranque, configuración y plataforma de versiones de la aplicación backend. |
| Spring Web MVC y `RestClient` | Gestionada por Spring Boot 4.1.1 | Endpoints HTTP, serialización web y cliente HTTP utilizado para Football-Data.org y pruebas E2E. |
| Spring Data JPA | Gestionada por Spring Boot 4.1.1 | Repositories del catálogo de jugadores y usuarios, paginación y acceso a persistencia. Las conexiones utilizan el pool HikariCP, también gestionado por Spring Boot, con UTC configurado en desarrollo y testing. |
| Hibernate ORM | Gestionada por Spring Boot 4.1.1 | Implementación JPA y validación del esquema de entidades contra la base migrada. |
| Spring Security y OAuth2 Resource Server | Gestionada por Spring Boot 4.1.1 | Cadena de seguridad, autenticación Bearer JWT, autorización de endpoints y conversión del principal persistido. |
| JJWT | 0.13.0 | Generación y firma HS256 de tokens JWT utilizados por la autenticación. |
| Jakarta Validation | Gestionada por Spring Boot 4.1.1 | Validación estructural de los DTO de entrada HTTP. |
| PostgreSQL | 18 en tests; no fijada para desarrollo y producción | Base de datos relacional. Testcontainers levanta la imagen `postgres:18`; el proyecto no fija actualmente la versión del servidor utilizada fuera de tests. |
| pgJDBC | Gestionada por Spring Boot 4.1.1 | Driver JDBC utilizado por los perfiles del backend para conectarse a PostgreSQL. |
| Flyway | Gestionada por Spring Boot 4.1.1 | Aplicación de las migraciones SQL versionadas de usuarios y jugadores. |
| SpringDoc OpenAPI | 2.8.13 | Generación de la especificación OpenAPI y UI a partir de los endpoints y sus anotaciones. |
| Lombok | Gestionada por Spring Boot 4.1.1 | Generación en compilación de constructores, getters y constructores sin argumentos usados por componentes y Models. |
| springboot4-dotenv | 5.1.0 | Carga de variables desde archivos `.env` exclusivamente durante desarrollo. |
| Spring Boot Test, JUnit Jupiter, Mockito, AssertJ y MockMvc | Gestionadas por Spring Boot 4.1.1 | Pruebas unitarias, de Controller, seguridad, integración y recorridos E2E; mocks, assertions y ejercicio del contrato MVC. |
| Testcontainers | Gestionada por Spring Boot 4.1.1 | Provisionamiento de PostgreSQL 18 aislado para las pruebas que requieren persistencia real. |
| Spring REST Docs | Gestionada por Spring Boot 4.1.1 | Generación de snippets del contrato HTTP desde las pruebas de Controller. |
| Asciidoctor Gradle Plugin | 4.0.5 | Conversión de la documentación AsciiDoc que incorpora los snippets de Spring REST Docs. |
| Spotless | 7.2.1 | Formato y controles de estilo de Java y archivos auxiliares, con Google Java Format y orden de imports. |
| SonarQube Gradle Plugin | 7.3.1.8318 | Análisis estático del backend ejecutado por el workflow dedicado. |
| GitHub Actions | Servicio sin versión fijada; acciones referenciadas por commit | CI del backend con JDK 21, build y tests, y ejecución separada del análisis de SonarQube. |
| Postman | No fijada por el proyecto | Colección versionada para explorar y verificar manualmente el contrato HTTP documentado. |

El catálogo contiene una entrada `springDotenv = "4.0.0"` asociada a `me.paulschwarz:spring-dotenv`, pero esa alias no es consumida por el build. La dependencia efectiva declarada es `me.paulschwarz:springboot4-dotenv:5.1.0`.

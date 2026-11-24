# Tecnologías del backend

## Guía de lectura

Consultar este documento para conocer el stack, las tecnologías aprobadas, sus versiones o su propósito, y en toda tarea que agregue, elimine, sustituya o actualice una tecnología o dependencia del backend. Para estructura, responsabilidades y dependencias arquitectónicas, consultar la [arquitectura del backend](architecture.md).

Las versiones efectivas se obtienen de `backend/build.gradle.kts`, `backend/gradle/libs.versions.toml`, el Gradle Wrapper y la configuración del proyecto. Esas fuentes tienen precedencia sobre este documento. Cuando Spring Boot administra una versión que el proyecto no fija individualmente, la tabla lo indica sin inferir un número.

| Tecnología | Versión | Uso en el proyecto |
| ---------- | ------- | ------------------ |
| Java | 21 | Lenguaje y toolchain del backend; la CI configura JDK 21 con distribución Zulu. |
| Microsoft Playwright para Java | 1.63.0 | Scraping de estadísticas WhoScored con Chromium visible (SPEC-012), una instancia de Playwright y un navegador headed por worker confinados a su hilo, contextos independientes por sección e inicialización bajo demanda, nunca durante el arranque de Spring. El recorrido no tiene retries ni deadline total. La limpieza del worker termina antes de devolver su resultado, cierra contextos → Browser → Playwright sin interrumpir el cierre normal. Tests de protocolo con dobles; retirada por decisión humana la suite DOM opt-in con navegador local. La ejecución real y los tests permanecen sujetos a validación humana. |
| Gradle y Kotlin DSL | 9.6.0 (Wrapper) | Construcción, dependencias y empaquetado; suites por tags, validación estática de clasificación y `fastTest` sin cobertura. Daemon, configuration cache y build cache locales; con `CI=true` los tests se ejecutan frescos sin deshabilitar la caché de compilación. |
| Spring Dependency Management Plugin | 1.1.7 | Aplica la gestión de versiones de dependencias provista por Spring Boot. |
| Spring Boot | 4.1.1 | Arranque, configuración y plataforma de versiones de la aplicación backend. |
| Spring Web MVC y `RestClient` | Gestionada por Spring Boot 4.1.1 | Endpoints HTTP, serialización web y clientes HTTP para Football-Data.org, TheSportsDB, identidad WhoScored y pruebas E2E. WhoScored usa cliente JDK dedicado sin redirecciones, timeouts y parsing JSON explícito independiente del MIME. |
| Spring Data JPA | Gestionada por Spring Boot 4.1.1 | Repositories del catálogo de jugadores y usuarios, paginación y acceso a persistencia. Las conexiones utilizan el pool HikariCP, también gestionado por Spring Boot, con UTC configurado en desarrollo y testing. |
| Hibernate ORM | Gestionada por Spring Boot 4.1.1 | Implementación JPA y validación del esquema de entidades contra la base migrada; soporte JDBC JSON para estructuras JSONB de los modelos persistidos. La sincronización WhoScored conserva solo referencias de catálogo: candidatos y resultados son transitorios, sin auditoría propia persistida. |
| Spring Security y OAuth2 Resource Server | Gestionada por Spring Boot 4.1.1 | Cadena de seguridad, autenticación Bearer JWT, autorización de endpoints y conversión del principal persistido. |
| JJWT | 0.13.0 | Generación y firma HS256 de tokens JWT utilizados por la autenticación. |
| Jakarta Validation | Gestionada por Spring Boot 4.1.1 | Validación estructural de los DTO de entrada HTTP. |
| PostgreSQL | 18 en tests; no fijada para desarrollo y producción | Base de datos relacional. Testcontainers levanta la imagen `postgres:18`; el proyecto no fija actualmente la versión del servidor utilizada fuera de tests. |
| pgJDBC | Gestionada por Spring Boot 4.1.1 | Driver JDBC y dependencia de compilación para clasificar errores de PostgreSQL por SQLSTATE, tabla y constraint estructurados, sin interpretar mensajes. |
| Flyway | Gestionada por Spring Boot 4.1.1 | Autoridad del esquema PostgreSQL. Historial atómico con timestamps UTC, validación al migrar, orden estricto e inmutabilidad de las migraciones integradas en `dev`; Hibernate valida el resultado. |
| SpringDoc OpenAPI | 2.8.13 | Documentación del contrato HTTP: generación de la especificación OpenAPI y UI a partir de los endpoints y sus anotaciones. |
| Spring Boot Actuator | Gestionada por Spring Boot 4.1.1 | Health, info y métricas del backend mediante HTTP; exposición explícita y restrictiva, nunca el comodín `*`. |
| Micrometer Prometheus Registry | Gestionada por Spring Boot 4.1.1 | Exporta las métricas estándar de CPU, JVM y solicitudes HTTP en formato Prometheus mediante Actuator. |
| Lombok | Gestionada por Spring Boot 4.1.1 | Generación en compilación de constructores, getters y constructores sin argumentos usados por componentes y Models. |
| springboot4-dotenv | 5.1.0 | Carga de variables desde archivos `.env` exclusivamente durante desarrollo. |
| Spring Boot Test, JUnit Jupiter, Mockito, AssertJ y MockMvc | Gestionadas por Spring Boot 4.1.1 | Pruebas de dominio, Controller, seguridad, integración y E2E; tags `unit`, `lightweight`, `integration` y `e2e` según dependencias reales, sin filtrar la tarea completa `test`. |
| JaCoCo | Plugin incluido con Gradle; versión efectiva predeterminada del plugin | Instrumentación de la suite completa `test` y reportes XML/HTML para Sonar y consulta humana. Instrumentación deshabilitada en suites parciales y pre-push. |
| Testcontainers | Gestionada por Spring Boot 4.1.1 | Provisionamiento de PostgreSQL 18 aislado para las pruebas que requieren persistencia real. |
| Spotless | 7.2.1 | Formato y controles de estilo de Java y archivos auxiliares, con Google Java Format y orden de imports. |
| SLF4J y Logback | Gestionadas por Spring Boot 4.1.1 | Logging de consola y archivo UTF-8 del backend (`FOOTBALL_MARKET_LOG_FILE`), con rotación estándar de Boot y archivo desactivado en tests. Retirada la configuración Logback de la herramienta diagnóstica temporal; sin nuevas dependencias. |
| SonarQube Gradle Plugin | 7.3.1.8318 | Análisis estático del backend ejecutado por el workflow dedicado. |
| GitHub Actions | Servicio sin versión fijada; acciones referenciadas por commit | CI con JDK 21, clasificación y suite completa; análisis Sonar separado con cobertura completa. Caché Gradle y publicación de reportes de tests/cobertura con `actions/upload-artifact`. |

El catálogo contiene una entrada `springDotenv = "4.0.0"` asociada a `me.paulschwarz:spring-dotenv`, pero esa alias no es consumida por el build. La dependencia efectiva declarada es `me.paulschwarz:springboot4-dotenv:5.1.0`.

## Monitoreo con Actuator

Spring Boot Actuator expone el monitoreo del backend. No es parte del contrato de dominio ni de
las rutas `/api/**`, y su configuración se resuelve por entorno.

### Endpoints disponibles

| Endpoint | Habilitado | Expuesto por HTTP | Acceso |
| --- | --- | --- | --- |
| `/actuator/prometheus` | Sí | Sí | JWT requerido |
| `/actuator/health` | Sí | Sí | Sin token; devuelve únicamente `{"status":"UP"}` |
| `/actuator/info` | Sí | Sí | Sin token; publica solo nombre y descripción estáticos |
| Raíz `/actuator`, subendpoints y demás endpoints | Según Actuator | **Nunca** | — |

La exposición se declara exactamente mediante `management.endpoints.web.exposure.include` y nunca
con el comodín `*` ni con una variable que amplíe la lista:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
```

### Consideraciones de seguridad

- `management.endpoint.health.show-details` y `show-components` son `never`: el health check no
  revela componentes, rutas, versiones ni datos de conexión. Es la razón por la que puede quedar
  accesible sin token.
- `SecurityConfig` exige JWT para Prometheus, deja públicos solo health e info y deniega la raíz,
  los subendpoints y cualquier otro endpoint de Actuator.
- `info` usa únicamente `info.app.name` y `info.app.description` estáticos mediante el contributor
  `management.info.env`; ambos valores son públicos y no se agregan credenciales ni otros valores
  del entorno.
- Los endpoints de Actuator comparten el puerto de la API. `management.server.port` no está
  configurado.

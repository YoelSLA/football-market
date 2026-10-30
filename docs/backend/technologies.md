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
| Hibernate ORM | Gestionada por Spring Boot 4.1.1 | Implementación JPA y validación del esquema de entidades contra la base migrada; auditoría de identidad WhoScored con estructuras JSONB tipadas mediante soporte JDBC JSON. |
| Spring Security y OAuth2 Resource Server | Gestionada por Spring Boot 4.1.1 | Cadena de seguridad, autenticación Bearer JWT, autorización de endpoints y conversión del principal persistido. |
| JJWT | 0.13.0 | Generación y firma HS256 de tokens JWT utilizados por la autenticación. |
| Jakarta Validation | Gestionada por Spring Boot 4.1.1 | Validación estructural de los DTO de entrada HTTP. |
| PostgreSQL | 18 en tests; no fijada para desarrollo y producción | Base de datos relacional. Testcontainers levanta la imagen `postgres:18`; el proyecto no fija actualmente la versión del servidor utilizada fuera de tests. |
| pgJDBC | Gestionada por Spring Boot 4.1.1 | Driver JDBC y dependencia de compilación para clasificar errores de PostgreSQL por SQLSTATE, tabla y constraint estructurados, sin interpretar mensajes. |
| Flyway | Gestionada por Spring Boot 4.1.1 | Aplicación de las migraciones SQL versionadas de usuarios y jugadores. |
| SpringDoc OpenAPI | 2.8.13 | Documentación del contrato HTTP: generación de la especificación OpenAPI y UI a partir de los endpoints y sus anotaciones. |
| Spring Boot Actuator | Gestionada por Spring Boot 4.1.1 | Monitoreo del backend. La exposición por HTTP es restrictiva y explícita, nunca el comodín `*`. Ver §Monitoreo con Actuator. |
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
| `/actuator` (enlaces) | Sí | Sí | Sin token |
| `/actuator/health` | Sí | Sí | Sin token. Devuelve únicamente `{"status":"UP"}` |
| `/actuator/info` | Sí | Sí | Sin token (temporal, ver la deuda técnica) |
| `/actuator/metrics` y `/actuator/metrics/{name}` | Sí | **Solo con habilitación explícita** | Sin token cuando está expuesto |
| `env`, `heapdump`, `threaddump`, `configprops`, `beans`, `shutdown` | Habilitados por Actuator | **Nunca** | — |

La exposición se declara de forma explícita mediante `management.endpoints.web.exposure.include` y
nunca con el comodín `*`. La lista completa se resuelve por entorno con un valor por defecto que
no incluye `metrics`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "${MANAGEMENT_EXPOSED_ENDPOINTS:health,info}"
```

### Habilitación de las métricas

Las métricas **no** se ligan al perfil `dev`. Ese perfil es el valor por defecto de
`spring.profiles.active` en este repositorio, por lo que asociarle la exposición haría que las
métricas quedaran disponibles en cualquier entorno que no fije el perfil. La habilitación es una
variable de entorno independiente, ausente por defecto:

```bash
# Habilitar, solo para diagnóstico local
MANAGEMENT_EXPOSED_ENDPOINTS=health,info,metrics ./gradlew bootRun

# Restablecer: basta con unset de la variable
```

Sin esa variable, `metrics` no se expone en ningún perfil, incluido producción. Solo `health`,
`info` y `metrics` son endpoints de monitoreo previstos; exponer cualquier otro es una regresión
de seguridad.

La lista se resuelve con un placeholder de Spring Boot y no con un documento de configuración
activado por propiedad: `spring.config.activate.on-property` **no existe en Spring Boot 4.1.1**
(las únicas activaciones admitidas son `on-profile` y `on-cloud-platform`). Además de romper el
arranque por sintaxis YAML inválida, un documento con una activación no reconocida se incluiría
siempre, lo que expondría las métricas en todos los entornos.

### Consideraciones de seguridad

- `management.endpoint.health.show-details` es `never` en todos los perfiles: el health check no
  revela componentes, rutas, versiones ni datos de conexión. Es la razón por la que puede quedar
  accesible sin token.
- `SecurityConfig` lista de forma explícita las rutas de monitoreo accesibles, en lugar de abrir
  `/actuator/**`. Exponer por error un endpoint sensible no lo volvería accesible.
- `info` no expone datos porque no hay propiedades `management.info.*` declaradas.
- Los endpoints de Actuator comparten el puerto de la API. `management.server.port` no está
  configurado.

### PENDIENTE — Seguridad de Actuator

Los endpoints de monitoreo están actualmente accesibles sin autenticación para facilitar el
desarrollo local. **Esa condición es temporal y no debe llegar a un despliegue público.** El
detalle, el riesgo, las alternativas a evaluar y el criterio de cierre se registran en
[deuda técnica](TECHNICAL_DEBT.md) como TD-001, que es la fuente única de ese seguimiento.

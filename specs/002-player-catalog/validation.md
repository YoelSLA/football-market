# Validación de la implementación

Fecha: 2026-09-18.

## Resultado

T001–T035 implementadas. T036 permanece pendiente porque el entorno no dispone
de Docker para ejecutar la suite comprometida de Testcontainers y no se ejecutaron
los controles remotos de CI/SonarQube.

## Controles ejecutados

| Control | Resultado |
|---|---|
| `gradlew.bat test spotlessCheck --continue` | 100 tests aprobados; un error de inicialización de Testcontainers por ausencia de Docker. Formato aprobado. |
| `gradlew.bat clean build spotlessCheck --continue` | Compilación, JAR ejecutable y formato aprobados. El build completo falla por el mismo requisito de Docker. |
| Persistencia sobre PostgreSQL 18.3 local | Cinco pruebas aprobadas: paginación de activos, upsert/reactivación/inactivación, fallo previo sin cambios, rollback y flujo HTTP autenticado. |
| HTTP contra el JAR en puerto temporal | GET 200; límites de paginación 400; sincronización con proveedor inaccesible 502; GET posterior 200; sincronización sin JWT 401. |
| OpenAPI servido por la aplicación | Ambos endpoints, campos exactos del jugador, códigos 200/400/401 y 200/401/502; sincronización sin request body. |
| Postman | JSON válido; ambas solicitudes con Bearer `{{JWT_TOKEN}}`, parámetros y ejemplos contractuales. |
| `gradlew.bat asciidoctor -x test --no-configuration-cache` | Documentación renderizada correctamente usando los snippets ya generados por los tests HTTP. |
| `git diff --check` | Aprobado. |

Las cinco pruebas de persistencia se ejecutaron mediante una copia temporal de
`PlayerCatalogIntegrationTest`, conectada a `football_market_test` en PostgreSQL
local. La copia y su script de Gradle estuvieron bajo `backend/build/` y fueron
eliminados por el posterior `clean`. El test versionado conserva Testcontainers
y falla explícitamente cuando Docker no está disponible; no se agregó un salto
ni una sustitución automática del control.

El flujo HTTP exitoso de sincronización y el fallo posterior se verificaron con
proveedor simulado y persistencia real. No se llamó a Football-Data.org real ni
se utilizaron credenciales reales. El servidor temporal fue detenido.

## Limitaciones y continuación

- Iniciar Docker y volver a ejecutar `gradlew.bat clean build spotlessCheck`
  desde `backend/` para completar el control estándar de persistencia.
- CI y SonarQube no se ejecutaron en este cambio local. No hay `SONAR_TOKEN`
  disponible en la sesión; su Quality Gate no puede darse por aprobado.
- Asciidoctor falla con la configuración de caché predeterminada existente.
  La ejecución con `--no-configuration-cache` aprueba sin modificar la
  configuración global ni las versiones del proyecto.
- No existe `.specify/extensions.yml`; no hay hooks posteriores que ejecutar.

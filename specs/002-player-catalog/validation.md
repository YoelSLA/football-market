# Validación de la implementación

Fecha: 2026-09-18.

## Corrección de orden y arranque — 2026-09-19

- Se aceptan las cinco ligas en cualquier orden, sin faltantes, adicionales ni duplicados.
- Se corrigió `football-data.competitions=${PL,BL1,PD,SA,FL1}` para referenciar
  `${FOOTBALL_DATA_COMPETITIONS}`. El placeholder anterior buscaba una variable
  cuyo nombre era la lista completa.
- La terminal no tenía definida `FOOTBALL_DATA_COMPETITIONS`. Al suministrar
  temporalmente `FL1,SA,PD,BL1,PL`, Spring arrancó con perfil `dev` y PostgreSQL
  en un puerto temporal. No se ejecutó una sincronización real.
- `spotlessApply test build --continue`: aprobado, 114 pruebas sin fallos.
  La suite actual ya no incluye `PlayerCatalogIntegrationTest`, cuya eliminación
  estaba preparada por el usuario antes de este cambio.
- Estos resultados actualizan los informes históricos siguientes; se elimina
  la exigencia previa de un orden fijo.

## Resultado

T001–T035 implementadas. T036 permanece pendiente porque el entorno no dispone
de Docker para ejecutar la suite comprometida de Testcontainers y no se ejecutaron
los controles remotos de CI/SonarQube.

El 2026-09-19 se restringió la especificación a las cinco ligas obligatorias `PL`,
`BL1`, `PD`, `SA` y `FL1`. T037–T040 están implementadas: la configuración rechaza
listas diferentes durante el arranque y las pruebas cubren las cinco ligas en orden
y el fallo de cualquiera de ellas. T041 conserva pendiente la ejecución con Docker
y el flujo manual con el proveedor real.

## Validación del cambio de cinco ligas — 2026-09-19

- `gradlew.bat spotlessApply test build --continue`: 112 pruebas aprobadas y un
  error de inicialización de `PlayerCatalogIntegrationTest` por Docker no disponible.
  Compilación, JAR y formato aprobados; el build global falla por ese requisito
  preexistente, sin omitir ni modificar el test.
- Reejecución de las pruebas de configuración, cliente, servicio de obtención,
  orquestador y Controller: aprobada, junto con `spotlessCheck`.
- Se comprobó primero que los nuevos casos de configuración incorrecta fallaban
  con la implementación anterior y que pasan con la nueva validación.
- OpenAPI, REST Docs y Postman documentan las cinco ligas. Se corrigieron las rutas
  de los snippets del catálogo en AsciiDoc para coincidir con los tests existentes.
- No se llamó al proveedor real ni se ejecutaron controles remotos de CI/SonarQube.
  SonarQube es opcional conforme a la Constitution vigente.

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

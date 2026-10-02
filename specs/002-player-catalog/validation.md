# Validación de la implementación

## Ejecución de la suite por el usuario — 2026-10-01

| Control | Resultado |
|---|---|
| `gradlew test` desde `backend/` | `BUILD SUCCESSFUL`. Compilación y ejecución completa de la suite, sin fallos reportados. |
| `gradlew spotlessCheck` desde `backend/` | Correcto. Formato y orden de imports conformes a la configuración de Spotless. |

- La ejecución es del usuario; el agente no ejecutó la suite ni los controles.
- `test` cubre los casos preparados de `PlayerMigrationTest` y `PlayerCatalogPersistenceTest`,
  incluidos migración `V3`, restricciones, y los conflictos lógico y concurrente de
  referencia externa.
- Pendientes, sin ejecutar: validación manual de `quickstart.md`, `clean build` de CI
  y análisis de SonarQube con su Quality Gate. Por eso T036, T041 y T070 siguen abiertas.
- No se registra conteo de pruebas ni detalle de casos: la salida compartida por el
  usuario fue únicamente el resultado de cada tarea de Gradle.

## Implementación preparada — 2026-10-01

- Se preparó el delta de identidad interna, V3, referencias externas, opcionales,
  contrato HTTP y recuperación transaccional aprobada. V2 no se modificó.
- Se crearon/adaptaron tests de Model, Integration, Service, Controller, Orchestrator,
  migración y concurrencia PostgreSQL. Las pruebas técnicas nuevas están en
  `backend/src/test/java/footballmarket/repositories/PlayerMigrationTest.java` y
  `PlayerCatalogPersistenceTest.java`.
- Los snippets se generan desde las fuentes de Controller al ejecutarse los tests;
  no se editaron archivos generados.
- El agente no ejecutó tests, build, Spotless, CI ni SonarQube. La ejecución de la
  suite figura en el apartado anterior, realizada por el usuario.
- Pendientes del usuario: T036, T041 y T070.
- Ajustes técnicos: candidatos inmutables en lugar de entidades en la foto, OSIV
  desactivado, flush sin batching y pgJDBC disponible en compilación para clasificar
  exactamente SQLSTATE/tabla/constraint. No cambia la semántica funcional aprobada.

Los apartados siguientes conservan evidencia histórica; no acreditan esta implementación.

Fecha: 2026-09-18.

## Estado vigente — 2026-09-30

La spec y el plan fueron actualizados para desacoplar la identidad interna de `Player`
del identificador de Football-Data.org. Este documento conserva el historial de
validaciones anteriores y ya no describe el modelo implementado.

- `Player.id` pasa a ser un identificador interno generado localmente.
- Se incorpora `PlayerExternalReference` con `provider` y `externalId`, única por
  `(provider, externalId)`, junto con el enumerado `PlayerProvider`.
- `Player` incorpora `dateOfBirth`, `nationality` e `imageUrl` como atributos
  opcionales.
- `GET /api/players` expone `dateOfBirth`, `nationality` e `imageUrl`, y su `id` es el
  identificador interno.
- La sincronización resuelve por `(FOOTBALL_DATA, externalId)` y crea el jugador junto
  con su referencia.
- La migración `V3__decouple_player_external_identity.sql` sustituye los identificadores
  previos por referencias `FOOTBALL_DATA`.
- Postman sale del alcance de esta feature.
- ThisSportsDB permanece fuera del alcance operativo.

Las validaciones registradas a continuación corresponden al modelo anterior y deben
repetirse tras la implementación. Ninguna prueba de este documento cubre el modelo de
identidad vigente.

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
- OpenAPI y REST Docs documentan las cinco ligas. Se corrigieron las rutas
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
| OpenAPI servido por la aplicación | Ambos endpoints, campos del jugador según el modelo vigente, códigos 200/400/401 y 200/401/502; sincronización sin request body. |

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

## Continuación pendiente

- Implementar el modelo de identidad vigente y repetir las validaciones de este
  documento: los resultados previos no son aplicables.
- Verificar en un entorno con Docker la migración `V3` sobre datos preexistentes, la
  generación de identificadores internos tras la migración y la restricción única de
  `(provider, external_id)`.
- Verificar que dos sincronizaciones consecutivas no crean duplicados ni alteran el
  `id` interno de los jugadores existentes.
- Verificar que un fallo de integridad ajeno a la invariante `(provider, external_id)`
  revierte la transacción y no se contabiliza como registro inválido.

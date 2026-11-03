# SPEC-011 — Testing Report y procedimiento humano

## Decisión humana vigente — 2026-10-18

**Cierre posterior:** resultado final CONVERGED aprobado expresamente por el usuario; autorización
final persistida/verificada `6ad50693400c55a3098e9a48`. Spec Approved y épica/US01–US04 DONE;
SDD 8/8, 60/60 tareas. Las menciones inferiores a autorización pendiente quedan superadas.
Sin nuevos tests ni modificación de los resultados y límites aceptados.

El usuario aprobó expresamente TESTING para US01–US04 sobre los 938 tests exitosos, C1,
CR-01 y compatibilidad SPEC-010. Aceptó las equivalencias de todos los escenarios de quickstart,
incluidas las ocho coberturas parciales, y no exige los refuerzos de 2/5/6/12 para esta aprobación.
El registro por escenario y sus límites se encuentra en [quickstart.md](quickstart.md#t058--equivalencias-aceptadas-por-el-usuario-2026-10-18).
T058 y T060 completas: 60/60 tareas. No se ejecutó HTTP manual completo, reinicio del escenario 2,
observación independiente de locks en 13/14 ni revisión de historial de desarrollo V9/V10.
Los registros de pendientes inferiores se conservan como historia y no son el estado vigente.
La aprobación de TESTING no sustituye CONVERGE ni la autorización final sobre su resultado.
CONVERGE posterior satisfactorio: ver convergence-report.md. Épica TESTING, US01–US04 DONE;
SDD Lifecycle 8/8. El gate final sobre el resultado presentado permanece pendiente conforme workflow.

## Consolidación de ejecución humana — 2026-10-18 (historial anterior a la aprobación)

**TESTING sigue PENDING HUMAN VALIDATION por US.** La épica permanece IN PROGRESS y las cuatro
historias en TESTING; CODE REVIEW se conserva. Esta consolidación no aprueba gates ni habilita
CONVERGE. No se ejecutaron tests, compilación, Gradle, Flyway ni servicios desde el agente.

### Resultados leídos, no inferidos del build

Se leyeron los **159 XML `TEST-*.xml`** de `backend/build/test-results/test/` y el resumen
`backend/build/reports/tests/test/index.html` (contadores tests/failures/skipped/successful).
El conteo de elementos testcase, sin sumar nuevamente sus grupos anidados, da:

| Métrica | XML | HTML |
| --- | --- | --- |
| Tests ejecutados | 938 | 938 |
| Exitosos | 938 | 100% |
| Fallidos (failure/error) | 0 | 0 failures |
| Omitidos | 0 | 0 skipped |

Timestamps de inicio de suites XML: `2026-10-18T17:01:12.705Z` a
`2026-10-18T17:08:52.138Z`; no representan el fin de toda la ejecución.
Duración agregada HTML: **7m56.09s**. El usuario aportó `./gradlew test`,
**BUILD SUCCESSFUL in 8m 4s**, seis tareas accionables (dos ejecutadas/cuatro up-to-date).
La duración del build y la de los tests son métricas diferentes; no hay contradicción.

Fingerprint del conjunto de XML leído:
`8f266ced003a6384e982065a4045ab98c9488a32c0e963ce1cb374ef76502ce9`.
SHA-256 acumulado por nombre de archivo ordenado + NUL + contenido + NUL. Permite identificar
estos resultados; no es un commit ni demuestra por sí solo el hash de fuentes que ejecutó Gradle.
No se modificaron reportes generados. El snapshot de código aprobado que figura más abajo es
histórico, anterior a la corrección temporal; no se presenta como fingerprint del código actual.

### Evidencia por clase

Prefijo de cada XML: `backend/build/test-results/test/TEST-footballmarket.`; los grupos `@Nested`
aparecen como `Clase$Grupo.xml`. Se agruparon por clase externa:

| Clase | Ejecutados / exitosos | Fallidos | Omitidos |
| --- | --- | --- | --- |
| controllers.ListingControllerTest | 42 / 42 | 0 | 0 |
| repositories.TokenListingConcurrencyTest | 5 / 5 | 0 | 0 |
| repositories.TokenListingExpirationPersistenceTest | 2 / 2 | 0 | 0 |
| controllers.OrderControllerTest | 88 / 88 | 0 | 0 |
| services.BuyOrderServiceTest | 26 / 26 | 0 | 0 |
| repositories.MarketDomainPersistenceTest | 31 / 31 | 0 | 0 |
| security.BuyOrderAuthorizationTest | 4 / 4 | 0 | 0 |
| models.TokenListingTest | 11 / 11 | 0 | 0 |
| repositories.TokenListingPersistenceTest | 18 / 18 | 0 | 0 |
| repositories.TokenListingVisibilityPersistenceTest | 1 / 1 | 0 | 0 |
| services.TokenListingServiceTest | 26 / 26 | 0 | 0 |
| security.TokenListingAuthorizationTest | 3 / 3 | 0 | 0 |

Estas filas son un subconjunto de la suite de 938, no totales que deban volver a sumarse a ella.

### Escenarios confirmados en los XML

- **C1**: `repositories.TokenListingConcurrencyTest$ExpirationReservationRace.xml` contiene dos
  casos exitosos: «Una compra ya validada conserva los tokens hasta confirmar aunque venza y bloquea
  la nueva reserva» y «Una compra que vence esperando la tenencia no consume los tokens de la nueva
  reserva». Corresponden a `conservaTenenciaHastaCommit` y `rechazaCompraVencidaDuranteEspera`.
- **Frontera temporal/T031**: `repositories.TokenListingExpirationPersistenceTest$Expiration.xml`,
  dos casos exitosos: validación vigente sin segunda lectura temporal y rechazo en igualdad con
  expiresAt sin movimientos. La fuente afirma también rollback de la tenencia preparada.
- **CR-01**: `controllers.OrderControllerTest$SecondaryPurchase.xml` incluye los **18** casos de
  compra secundaria con datos opcionales ignorados (overflow, decimal, exponencial, strings,
  booleanos, arrays/objetos, playerId inválido y listingId en último lugar), los **16** rechazos
  estructurales y los **3** IDs no positivos sin fallback a directa. Ocho casos adicionales verifican
  errores secundarios; todos exitosos. InputValidation comprueba la modalidad directa.
- **SPEC-010**: OrderControllerTest, BuyOrderServiceTest, MarketDomainPersistenceTest y
  BuyOrderAuthorizationTest figuran ejecutados con cero fallos/omisiones. Sus grupos incluyen
  SuccessfulPurchase, validaciones/rechazos, auditoría, rollback, SecondaryPurchase y rol persistido.
  La compatibilidad corresponde al alcance de esas assertions, no a una demostración universal.
- **Publicar/consultar/cancelar**: ListingControllerTest$Publication, $Queries y $Cancellation
  pasan; los tres escenarios antes fallidos de publicación, detalle y cancelación están presentes
  y exitosos. TokenListingServiceTest confirma las APIs reales y sus rechazos.
- **Reservas y vencimiento**: TokenListingPersistenceTest$Reservations/$Queries/$Cancellation
  verifican no transferencia, coexistencia/límite de reservas, frontera sin persistir EXPIRED,
  filtro efectivo, liberación y republicación. Sus $Constraints/$PurchaseRejections también pasan.
  TokenListingVisibilityPersistenceTest conserva acceso propio y oculta publicaciones a terceros.
- **Carreras adicionales**: TokenListingConcurrencyTest$Publication y $Purchases contienen los
  otros tres casos exitosos: publicaciones simultáneas, doble comprador y compra/cancelación.

### Historial preservado y procedencia

1. **Primera suite, informada por el usuario**: 938 tests, 935 sin fallos y tres fallidos en
   ListingControllerTest:71/:185/:345. Expected `2026-10-17T12:00:00Z`; actual
   `2026-10-17T09:00:00-03:00`: mismo instante, distinta representación.
2. **Diagnóstico aceptado y corrección autorizada**: Jackson global permanece en Buenos Aires;
   los tests parsean OffsetDateTime y comparan Instant exactos. Se corrigieron nueve assertions
   temporales y descripciones OpenAPI de tres Response DTO, sin cambiar C1 ni SPEC-010.
   Evidencia histórica de TESTING en la épica: comentario `6ad4f8e3d526b6ebd69a4499`.
3. **Ejecuciones focalizadas satisfactorias, informadas por el usuario**: testClasses,
   footballmarket.models.*, concurrencia/expiración y el grupo de compatibilidad SPEC-010.
   No se atribuyen totales independientes a esas ejecuciones: los XML actuales pertenecen al
   conjunto final leído y pueden haber reemplazado los reportes anteriores.
4. **Suite posterior satisfactoria**: confirmación humana del comando/build y corroboración
   mediante XML/HTML actuales: 938 exitosos, cero fallidos/omitidos. Los XML fallidos originales
   no se afirman conservados en el directorio actual; su resultado se preserva aquí como evidencia
   aportada por el usuario, no como una relectura de artefactos fallidos antiguos.

### T060 y siguiente paso

**T060 COMPLETADA**: ejecución solicitada, resultados aportados por el usuario y evidencia
XML/HTML verificable registrada; suite completa sin fallos ni omisiones, con las clases y casos
críticos presentes. El criterio de T060 no exige aprobar TESTING ni completar quickstart.
Quedan **59/60 tareas**; **T058 pendiente**. No se alteran IDs, gates o columnas.

Para T058, usar el procedimiento de §4 y la tabla de resultados de quickstart.md. Por escenario
1–14, 7b, 9b y 9c A/B, anotar método, precondiciones, esperado/observado y referencia al reporte.
Se dispone de cobertura automatizada de apoyo (§4), pero eso no acredita ejecución HTTP manual.
Si se adopta equivalencia automatizada, el usuario debe identificarla y aceptarla explícitamente;
los escenarios no verificados permanecen pendientes. No aplicar migraciones ni alterar datos
para fabricar las fixtures excepcionales. Si falta el entorno HTTP preparado, registrar PENDING
con el motivo en vez de PASS.

Para aprobar TESTING todavía se requiere decisión humana expresa por US sobre pruebas/cobertura,
resultados y limitaciones. La lectura de reportes y el cierre de T060 no sustituyen ese gate.

## Procedimiento preparado inicialmente (historial; resultados actualizados arriba)

**Estado: PENDING HUMAN VALIDATION.** Fecha: 2026-10-18.
CODE REVIEW de US01–US04 aprobado expresamente por el usuario después de aceptar CR-01/02/03.
Esta aprobación no acredita compilación, ejecución, resultados de tests ni TESTING aprobado.
La Feature permanece IN PROGRESS; las cuatro US pasan a TESTING una vez persistido su CODE REVIEW.
T058 y T060 siguen pendientes. CONVERGE y cierre no están habilitados.

## 1. Base evaluada y entorno

Árbol local de `feat/011-sell-player-tokens`, incluidos archivos no rastreados, sin commit de entrega.
Snapshot SHA-256 del backend aprobado:
`6227cde6d792e84aab2ab3a179072d0af8095c8f34da128dc9b459622267b3ec`.
Incluye todos los archivos de `backend/src/`, `backend/build.gradle.kts`,
`backend/gradle/libs.versions.toml` y `backend/gradle/wrapper/gradle-wrapper.properties`.
Se ordenan las rutas relativas a la raíz; para cada una se agrega ruta UTF-8, byte NUL,
contenido y byte NUL al SHA-256. No es un hash de commit ni evidencia de tests.

Requisitos:

- JDK 21 y Gradle Wrapper del repositorio (9.6.0); usar `./gradlew`, no un Gradle del sistema.
- Docker Engine accesible al usuario, recursos disponibles y permiso de descarga de `postgres:18`.
  No hace falta levantar `compose.yml` para tests automatizados.
- Acceso a los repositorios de dependencias si las cachés no están completas. No se requieren
  proveedores deportivos reales, secretos productivos ni una base PostgreSQL externa.
- Perfil `test`: las clases de contexto lo declaran; no forzar datasource de desarrollo mediante
  variables de entorno, argumentos o propiedades. Revisar overrides locales antes de ejecutar.
- Bash para los comandos con `pipefail`; espacio para logs/reportes. Python 3 para el
  fingerprint opcional y extracción de tokens; `curl` y credenciales sintéticas solo para quickstart HTTP.

`TestcontainersConfiguration` utiliza PostgreSQL 18 aislado/desechable; las suites con DB aplican
las migraciones reales mediante Flyway **dentro de ese entorno de testing** cuando las ejecute el
usuario. El agente no provisionó contenedores ni aplicó migraciones. No reutilizar la base de
desarrollo para sustituir Testcontainers ni modificar migraciones para ocultar un fallo.

## 2. Inventario y cobertura para revisar por US

Rutas de tests relativas a `backend/src/test/java/footballmarket/`.
La tabla describe cobertura escrita, no resultados observados ni certificación de exhaustividad.
El inventario productivo completo y los checklists de revisión están en `implementation-review.md`.

| US / criterios | Tests nuevos | Tests modificados / foco |
| --- | --- | --- |
| US01, AC01–AC14 | `models/TokenListingTest.java`; `services/TokenListingServiceTest.java`; `controllers/ListingControllerTest.java`; `repositories/TokenListingPersistenceTest.java`; `repositories/TokenListingConcurrencyTest.java`; `security/TokenListingAuthorizationTest.java` | Publicación, límites/formatos, rol persistido, precio congelado, reserva derivada, CHECK/FK, ausencia de transferencias y publicaciones simultáneas. |
| US02, AC15–AC31 | `repositories/TokenListingExpirationPersistenceTest.java`; grupos de compra/rechazos/C1 en los nuevos tests anteriores | `controllers/OrderControllerTest.java`, `services/BuyOrderServiceTest.java`, `repositories/MarketDomainPersistenceTest.java`: compra total, saldos/tokens, auditoría, rollback, ocultación, compra única, compatibilidad directa y CR-01. |
| US03, AC32–AC42 | `repositories/TokenListingVisibilityPersistenceTest.java`; grupos Queries de Persistence/Service/Controller | Mercado excluye propias/cerradas/vencidas/ocultas; propias filtra por estado efectivo exacto sin perder jugadores ocultos; dueño conserva detalle/historia. |
| US04, AC43–AC48 | Grupos Cancellation de Persistence/Service/Controller y Purchases de Concurrency | Propietario exclusivo, rechazo de estados terminales/vencidos, cierre, reserva liberada, republicación y exclusión compra/cancelación. |

Soporte nuevo: `repositories/ListingRepositorySupport.java` prepara fixtures, reloj y transacciones
reales para Repository. `support/MarketTestData.java` y `support/TestcontainersConfiguration.java`
se reutilizan, no se modificaron en las correcciones CR.

### Mocks, assertions y puntos delicados

- Model: pruebas puras, sin Spring ni DB; cantidad/precio, overflow, transiciones y frontera exacta.
- Service: fachada real con DB real; `QuoteSource` simulado, fixtures solo en Arrange y assertions
  sobre la API pública. Los créditos/reservas durables se observan exclusivamente en Repository.
- Repository: repositorios, Services y PostgreSQL reales; no mocks de Repository. Reloj controlado
  en pruebas temporales. Se verifican saldos, tenencias, estados, reservas, FK/CHECK y auditoría.
- Controller: Services simulados y cadena Security real. Status, campos/códigos y modalidad elegida;
  no demuestra que una transferencia se haya persistido. Esa garantía requiere Repository.
- Security: JWT, autenticación y servicios reales; rol persistido frente a claims engañosos.
- CR-01: revisar `compraPublicacionCompleta`, `rechazaIdentificador`,
  `noDerivaIdentificadorInexistente`, `cantidadNoEnteraRespondePeticionInvalida`,
  `identificadorNoNumericoRespondePeticionInvalida` y `unaSolicitudValidaAlcanzaAlService` en
  OrderControllerTest. Cantidad/jugador inválidos se ignoran solo con listingId válido;
  listingId mal formado no deriva a directa. Ausente/null conserva la selección directa vigente.
- C1: `ExpirationReservationRace` en TokenListingConcurrencyTest tiene dos escenarios:
  `rechazaCompraVencidaDuranteEspera` y `conservaTenenciaHastaCommit`. Revisar que el avance del reloj
  sucede después de observar bloqueo real con PID, `pg_blocking_pids` y `pg_stat_activity`.
  Las señales/timeouts no simulan el tiempo de negocio. El timeout debe fallar, nunca contar como PASS.
- T031: `rechazaFrontera` comprueba rollback de la primera tenencia y ausencia de movimientos;
  `noRevalidaAlFinal` exige una sola consulta al reloj. La carrera B retiene una transacción real
  durante el vencimiento y comprueba nueva publicación bloqueada/rechazada tras el commit.
- C1 no demuestra ausencia universal de deadlocks. Las otras carreras con barrera no prueban por sí
  solas contención efectiva. No presentar un PASS de Model/Controller como prueba de locks PostgreSQL.
- Si no se descubren tests o aparecen SKIPPED/ABORTED, no declarar la cobertura satisfecha.
  Los métodos anidados generan nombres/reportes adicionales: revisar también sus resultados.
- Un fallo de Docker, red o dependencias es un problema de entorno, no un PASS funcional.
  Un fallo de compilación puede impedir arrancar cualquier suite; el build histórico sin tests
  fue anterior a CR-01 y no valida el deserializador nuevo ni sus tests.

## 3. Ejecución automatizada — exclusivamente por el usuario

Orden: compilación → Model → integración focalizada → C1 → SPEC-010 → suite completa.
Ejecutar desde una terminal nueva, en `backend/`, sin servicios productivos ni datasource externo
forzado. Ante un fallo, detener esta secuencia y compartir la salida relevante antes de corregir.
Los filtros usan la tarea real `test`; **no existe una tarea separada integrationTest** en el build.

Preparación (comandos personales; ninguno fue ejecutado por el agente):

```bash
cd /home/yoelsla/Escritorio/football-market/backend
set -o pipefail
mkdir -p build/spec011-evidence
java -version
docker version
docker info
```

### A. Compilación de tests, sin ejecución

```bash
./gradlew testClasses --console=plain 2>&1 | tee build/spec011-evidence/01-test-classes.log
```

`testClasses` compila producción/tests y procesa sus recursos; no inicia tests, aplicación ni DB.
Resultado necesario: comando exitoso. Registrar el código de salida inmediatamente con `echo $?`.

### B. Tests unitarios de Model

```bash
./gradlew test --tests 'footballmarket.models.*' -x jacocoTestReport --rerun-tasks --console=plain 2>&1 | tee build/spec011-evidence/02-model.log
cp -a build/test-results/test build/spec011-evidence/02-model-results
```

### C. Integración de SPEC-011 y contratos HTTP/seguridad

```bash
./gradlew test --tests 'footballmarket.repositories.TokenListing*Test' --tests 'footballmarket.repositories.MarketDomainPersistenceTest' --tests 'footballmarket.services.TokenListingServiceTest' --tests 'footballmarket.services.BuyOrderServiceTest' --tests 'footballmarket.controllers.ListingControllerTest' --tests 'footballmarket.controllers.OrderControllerTest' --tests 'footballmarket.security.TokenListingAuthorizationTest' -x jacocoTestReport --rerun-tasks --console=plain 2>&1 | tee build/spec011-evidence/03-integration.log
cp -a build/test-results/test build/spec011-evidence/03-integration-results
```

Esta selección incluye pruebas con PostgreSQL y slices web: no todas las clases de contexto son
tests E2E. Los fixtures no utilizan proveedores reales.

### D. Regresiones C1 y vencimiento

```bash
./gradlew test --tests 'footballmarket.repositories.TokenListingConcurrencyTest' --tests 'footballmarket.repositories.TokenListingExpirationPersistenceTest' -x jacocoTestReport --rerun-tasks --console=plain 2>&1 | tee build/spec011-evidence/04-c1.log
cp -a build/test-results/test build/spec011-evidence/04-c1-results
```

Se seleccionan clases completas para incluir sus grupos `@Nested` sin depender de filtros de
nombres internos. Confirmar que se ejecutaron **ambas** carreras C1 y ambos casos T031.

### E. Compatibilidad con SPEC-010 y selección CR-01

```bash
./gradlew test --tests 'footballmarket.controllers.OrderControllerTest' --tests 'footballmarket.services.BuyOrderServiceTest' --tests 'footballmarket.repositories.MarketDomainPersistenceTest' --tests 'footballmarket.security.BuyOrderAuthorizationTest' -x jacocoTestReport --rerun-tasks --console=plain 2>&1 | tee build/spec011-evidence/05-spec010.log
cp -a build/test-results/test build/spec011-evidence/05-spec010-results
```

Revisar compra directa, límites/formatos, autorización, auditoría sin listing_id y respuesta sin
listingId, además de las variantes secundarias y el no-fallback de CR-01.

### F. Suite completa

```bash
./gradlew test --rerun-tasks --console=plain 2>&1 | tee build/spec011-evidence/06-full-suite.log
cp -a build/test-results/test build/spec011-evidence/06-full-suite-results
cp -a build/reports/tests/test build/spec011-evidence/06-full-suite-report
```

Los pasos focalizados excluyen el finalizador JaCoCo para separar diagnóstico de cobertura.
La suite completa conserva el finalizador configurado; genera cobertura en
`build/reports/jacoco/test/`. Los HTML de tests están en `build/reports/tests/test/index.html`
y los XML en `build/test-results/test/`. Copiarlos antes de otra ejecución: Gradle los reemplaza.
`--rerun-tasks` evita confundir UP-TO-DATE con una nueva ejecución. No usar `-x test` aquí.
Si se repite una etapa, elegir un directorio de evidencia nuevo; `cp -a` no debe mezclar resultados
viejos con nuevos. Registrar salida/código de cada Gradle antes de los comandos de copia.

## 4. Quickstart — T058

Seguir `quickstart.md` escenarios 1–14, incluidos 7b/9b/9c. La guía conserva los resultados
esperados; registrar los **observados**, no copiar los esperados como si hubieran sucedido.

| Escenario | US / evidencia automatizada de apoyo |
| --- | --- |
| 1, 2 | US01 / publicación sin movimientos y precio congelado en Persistence/Service/Controller |
| 3 | US03 / Queries de Persistence/Service/Controller |
| 4, 5, 6, 7 | US02 / SecondarySettlement, PurchaseRejections, compra propia y repetición |
| 7b | US02 / `rechazaCuentaAusente`; solo fixture aislada, nunca borrar cuentas de desarrollo |
| 8 | US04 / Cancellation, republicación y `compiteConCancelacion` |
| 9 | US02/03/04 / frontera, estado efectivo, filtros y cancelación vencida |
| 9b, 9c A/B | US01/02 / comando D; registrar individualmente los cuatro tests temporales |
| 10 | US01/02/03 / VisibilityPersistence y `ocultaJugadorNoVisible` |
| 11 | US02 / comando E, compra directa y referencia nula/omitida |
| 12 | US01/02 / TokenListingAuthorizationTest y BuyOrderAuthorizationTest |
| 13, 14 | US01/02 / `evitaSobreventa` y `liquidaUnaVez` |

### Si se ejecuta el recorrido HTTP manual

Precondición externa: entorno local **dedicado de validación**, con esquema ya actualizado,
catálogo visible, emisión inicial y superusuario previamente preparados por su responsable.
No reutilizar un entorno ajeno; no aplicar migraciones ni reconciliar V9/V10 como parte de este
procedimiento. Si el esquema no está preparado, registrar el escenario como pendiente por entorno.
Los tests automatizados anteriores no necesitan esta base manual.

Para evitar que el arranque manual aplique migraciones, una vez que el responsable confirme el
esquema, el usuario puede ejecutar en otra terminal desde `backend/`:

```bash
export VALIDATION_JDBC_URL='jdbc:postgresql://localhost:55432/football_market_validation'
read -r -p 'Usuario de la base aislada: ' VALIDATION_DB_USER
read -r -s -p 'Contraseña de la base aislada: ' VALIDATION_DB_PASSWORD
SPRING_DATASOURCE_URL="$VALIDATION_JDBC_URL" SPRING_DATASOURCE_USERNAME="$VALIDATION_DB_USER" SPRING_DATASOURCE_PASSWORD="$VALIDATION_DB_PASSWORD" SPRING_FLYWAY_ENABLED=false SUPERUSER_ENABLED=false MARKET_QUOTE_SIMULATOR_ENABLED=true MARKET_QUOTE_SIMULATOR_PRICE=100 ./gradlew bootRun
```

La URL es un ejemplo reservado para validación; confirmar host/puerto/base antes de usarlo.
No inicia el contenedor ni crea esquema/usuarios. Hibernate conserva `ddl-auto=validate`:
si falta esquema, detenerse, no sustituirlo por create/update. Para el escenario 2, reiniciar
personalmente con otro MARKET_QUOTE_SIMULATOR_PRICE; nunca cambiar el código.

Usar dos usuarios sintéticos registrados/autenticados por las rutas existentes. En una base aislada
de validación, estas identidades/contraseñas de ejemplo son exclusivamente desechables. No volver
a registrar si ya existen: reutilizar su login o elegir identidades sintéticas nuevas y coherentes
en ambos comandos. En la terminal HTTP:

```bash
export BASE_URL='http://localhost:8080'
curl -sS -i -X POST "$BASE_URL/api/auth/register" -H 'Content-Type: application/json' -d '{"username":"spec011.seller","email":"spec011.seller@example.com","password":"Spec011-local-only-123"}'
curl -sS -i -X POST "$BASE_URL/api/auth/register" -H 'Content-Type: application/json' -d '{"username":"spec011.buyer","email":"spec011.buyer@example.com","password":"Spec011-local-only-123"}'
TOKEN_SELLER=$(curl -fsS -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d '{"email":"spec011.seller@example.com","password":"Spec011-local-only-123"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')
TOKEN_BUYER=$(curl -fsS -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d '{"email":"spec011.buyer@example.com","password":"Spec011-local-only-123"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')
```

Confirmar 201 en las altas y éxito de ambos logins sin imprimir los tokens. Preparar una tenencia
del vendedor mediante compra directa si todavía no existe; no repetir la compra preparatoria sin
considerar tenencia/saldo consumidos por ejecuciones anteriores:

```bash
read -r -p 'ID de jugador visible con inventario: ' PLAYER_ID
curl -sS -i -X POST "$BASE_URL/api/orders/buy" -H "Authorization: Bearer $TOKEN_SELLER" -H 'Content-Type: application/json' -d "{\"playerId\":$PLAYER_ID,\"quantity\":20}"
curl -sS -i -X POST "$BASE_URL/api/orders/sell" -H "Authorization: Bearer $TOKEN_SELLER" -H 'Content-Type: application/json' -d "{\"playerId\":$PLAYER_ID,\"quantity\":5}"
```

Esperar respectivamente 200 y 201; anotar listingId. Continuar las solicitudes de `quickstart.md`
con esos tokens/IDs. En el escenario 4, el detalle SOLD se consulta **como vendedor**: el comprador
es tercero y después de comprar recibe 404. Las consultas de saldos/tenencias no tienen endpoint
nuevo en esta spec: usar la evidencia de Repository, no inventar rutas de portfolio.
Los escenarios con fixtures excepcionales o reloj controlado se verifican mediante los tests
aislados indicados; no alterar SQL de la base manual para forzarlos.

No registrar JWT, contraseñas ni headers Authorization en evidencias. Si solo se ejecuta la
cobertura automatizada equivalente, indicarlo explícitamente por escenario y solicitar aceptación
humana de esa equivalencia; no afirmar que se ejecutó el recorrido HTTP. No cerrar T058 por un
PASS global sin resultados por escenario. Los escenarios no realizados permanecen pendientes.

## 5. Evidencia y cierre de pendientes

Para **T060**, aportar:

1. Fecha, entorno (JDK/Docker), rama/estado local y confirmación de que se evaluó el snapshot aprobado.
2. Resultado de compilación A; comandos realmente ejecutados, códigos de salida y resultados B–F.
3. Totales ejecutados/fallidos/omitidos, y confirmación específica de C1, CR-01 y SPEC-010.
4. Logs y XML/HTML disponibles localmente; cuando pasan basta confirmar claramente el resultado
   y su alcance. Ante fallo, compartir tarea/clase/método, causa y salida relevante sin secretos.

Para **T058**, registrar una fila por escenario 1–14 y 7b/9b/9c A/B con:

```text
Escenario | Fecha | Método (HTTP / test y clase#método) | Precondiciones/IDs sintéticos
Esperado | Observado | PASS/FAIL/PENDING | Referencia a log/reporte | Limitaciones
```

La tabla pendiente de `quickstart.md` es el registro de destino; completar solo con evidencia real
aportada por el usuario. Mantener las 60 tareas/IDs. T058/T060 no se marcan en esta entrega.

TESTING requiere también revisión y aprobación humana **por US** de sus pruebas/cobertura y
resultados. La evidencia de compilación o un PASS de suite no aprueban automáticamente ese gate.
Si un fallo exige modificar fuentes/tests, corresponde volver a IMPLEMENT y repetir CODE REVIEW
afectado; si es solo entorno, permanecer en TESTING con bloqueo explicado. No habilitar CONVERGE.

## 6. Revisión humana solicitada

Orden recomendado: revisar los tests por US (§2), las nuevas variantes CR-01, las señales/locks C1,
la compatibilidad directa, y después ejecutar A–F y completar el registro de quickstart.

- [ ] Revisé tests, mocks, assertions y límites de cobertura de cada US.
- [ ] Ejecuté la compilación y las pruebas aplicables; identifiqué omisiones/fallos.
- [ ] Revisé individualmente C1 y compatibilidad SPEC-010.
- [ ] Aporté resultados de quickstart/T058 y suite/T060 con sus limitaciones.
- [ ] Emito por separado mi decisión explícita de TESTING para cada US.

Todo este checklist permanece pendiente. Ningún comando de este documento fue ejecutado por el agente.

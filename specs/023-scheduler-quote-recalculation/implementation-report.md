# Informe de implementación — SPEC-023

## Alcance y estado

Implementadas Phase 1 y 2 y US1–US3: T001–T028 y T031. T029/T030 están retiradas por decisión
humana, no se ejecutaron. Phase 6 (T032/T033) completada bajo autorización posterior explícita del
usuario registrada en `sdd-gates.md`. Base revisable: diff local respecto
de `a1cfb36`, sin commit ni publicación. No se modificaron requisitos ni checklists en esta reanudación.

La autorización procede de `sdd-gates.md` y de la solicitud explícita de continuar US1–US3. Se usa el
ANALYZE posterior a las precisiones de T027, no los eventos históricos anteriores. No se repitió ANALYZE.

## Code Review Report

### Funcionamiento y decisiones

1. Spring habilita scheduling una vez y configura un pool batch de un hilo. La expresión configurable
   usa lunes 00:00 UTC por defecto; el perfil `test` deja el disparador inerte.
2. `AutomaticQuoteRecalculationScheduler.recalculate` solo comprueba habilitación e invoca la interfaz
   del caso de uso. Registra disparo, fecha ya registrada, mercado no inicializado o clase de fallo.
   No propaga los errores de aplicación y no inspecciona el resultado de dominio.
3. `QuoteRecalculationServiceImpl.recalculateAutomatic` resuelve estrategia y parámetros desde
   `QuoteProperties`. Comparte un núcleo privado con la entrada manual, preservando su firma,
   precedencia de validaciones, reserva por fecha, fórmulas y transacciones existentes.
4. La migración agrega únicamente `origin`, rellena filas existentes como `MANUAL`, elimina el default
   y exige catálogo cerrado. El modelo deriva resultado y motivo desde `failedPlayers`; no hay columnas
   duplicadas. El Mapper solo transporta los tres campos aditivos al contrato HTTP existente.
5. No hay lock global, `EXECUTION_IN_PROGRESS`, reintento automático ni compensación. Una ejecución
   registrada, incluso con fallos individuales, consume la fecha. Sin registro, el recálculo existente
   permite reintento manual. TD-024 sigue pendiente: cotizaciones confirmadas por jugador pueden
   permanecer aunque falle el resumen.

### Inventario de código a revisar

Rutas relativas a `backend/`. Se incluye la base preparatoria de la entrega anterior.

| Archivo | Cambio y responsabilidad |
| --- | --- |
| `src/main/java/footballmarket/FootballMarketApplication.java` | Modificado: única habilitación `@EnableScheduling` (T001). |
| `src/main/resources/application.yml` | Modificado: estrategia, cron, habilitación y pool batch (T002). |
| `src/test/resources/application-test.yml` | Modificado: programación inerte en la suite (T003). |
| `src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionOrigin.java` | Nuevo: catálogo persistido MANUAL/AUTOMATIC (T004). |
| `src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionResult.java` | Nuevo: SUCCESS/FAILED derivados (T005). |
| `src/main/java/footballmarket/features/quote/domain/enums/QuoteExecutionFailureReason.java` | Nuevo: motivo cerrado sin detalles internos (T006). |
| `src/main/java/footballmarket/features/quote/domain/models/QuoteExecution.java` | Modificado: origen obligatorio y accesores derivados `result`/`failureReason` (T007). |
| `src/main/java/footballmarket/features/quote/infraestructure/config/QuoteProperties.java` | Modificado: binding de estrategia y programación, defaults y validación anidada (T008). |
| `src/main/resources/db/migration/V20261104144409__add_quote_execution_origin.sql` | Nueva: columna y constraint de origen, sin alterar migraciones previas (T009). No aplicada por el agente. |
| `src/main/java/footballmarket/features/quote/application/QuoteRecalculationService.java` | Modificado: segunda entrada explícita `recalculateAutomatic` (T015). |
| `src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java` | Modificado: núcleo compartido, origen de cada entrada y logs ampliados (T010/T016/T018). |
| `src/main/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationScheduler.java` | Nuevo: entrada batch delgada, cron UTC y traducción de errores operativos (T017/T031). |
| `src/main/java/footballmarket/features/quote/presentation/dtos/response/QuoteExecutionResponseDTO.java` | Modificado: tres campos aditivos y sus esquemas (T022). |
| `src/main/java/footballmarket/features/quote/presentation/QuoteExecutionMapper.java` | Modificado: transporte directo desde el modelo (T023). |
| `src/main/java/footballmarket/features/quote/presentation/QuoteController.java` | Modificado: OpenAPI sin cambiar rutas, firma ni seguridad (T024). |
| `src/test/java/footballmarket/features/quote/infraestructure/scheduling/AutomaticQuoteRecalculationSchedulerTest.java` | Nuevo: mock de Service, habilitación, omisión, rechazo, fallo y continuación (T011/T028). |
| `src/test/java/footballmarket/features/quote/infraestructure/config/QuotePropertiesTest.java` | Nuevo: binding y fallos de startup en contexto mínimo (T012). |
| `src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java` | Modificado: configuración automática, origen releído, equivalencia de contadores, consulta vacía y concurrencia mixta (T013/T021/T026). |
| `src/test/java/footballmarket/features/quote/application/QuoteQueryServiceTest.java` | Modificado: dos entradas con fixtures equivalentes y la misma expectativa de precios, verificadas desde la API pública de lectura (complementa T013). |
| `src/test/java/footballmarket/features/quote/infraestructure/QuoteExecutionPersistenceTest.java` | Modificado: inserciones con origen, rechazo sin origen/default, nulo y catálogo inválido, aceptación de AUTOMATIC (T014). |
| `src/test/java/footballmarket/features/quote/presentation/QuoteControllerTest.java` | Modificado: serialización, campos aditivos, fallo individual sanitizado y fechas sin registro; conserva rechazos y seguridad (T019, adaptación mecánica de T007). |
| `src/test/java/footballmarket/features/quote/domain/models/QuoteExecutionTest.java` | Nuevo: invariantes y resultado derivado al construir y completar (T020). |
| `src/test/java/footballmarket/features/quote/application/QuoteAutomaticRecalculationFailureTest.java` | Nuevo: fallo completo/reintento, mercado no inicializado y fecha consumida con fallo individual usando servicios y DB reales (T027). |
| `src/test/java/footballmarket/support/ScoreRankingTestData.java` | Modificado: fixture existente declara MANUAL en el constructor obligatorio (T007). |

Documentación cambiada por esta implementación: contrato aditivo
`specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` (T025), seguimiento en
`tasks.md`, eventos nuevos en `sdd-gates.md` y este informe. `docs/TECHNICAL_DEBT.md` recibió evidencia
de TD-024 durante la entrega preparatoria; no se declara resuelta. Las modificaciones previas del
usuario en spec, plan, research, modelo, contratos, quickstart y checklists se preservan.

### Orden y puntos de revisión humana

1. Contratos de SPEC-023 y delta aditivo de SPEC-016; comprobar que no se amplía el alcance.
2. `QuoteExecution`, enums y migración: nulabilidad, catálogo y resultado derivado.
3. Configuración y `QuoteProperties`: defaults, validación y test inerte.
4. Service: dos entradas al mismo núcleo; revisar especialmente precedencia y transacciones intactas.
5. Scheduler: UTC fija, ausencia de negocio/compensación y traducción de excepciones.
6. DTO/Mapper/Controller y tests HTTP: contratos, null de `failureReason` y ausencia de detalles internos.
7. Todos los tests nuevos/modificados, especialmente reloj de fallo y carrera entre llamadas.

Checklist humano — pendiente, no aprobado por el agente:

- [ ] Revisé todos los archivos creados/modificados, incluidos los tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes y el límite TD-024.
- [ ] No detecté comportamiento fuera de la spec vigente.
- [ ] Apruebo avanzar a TESTING.

## Tests preparados y pendientes

No se ejecutó ningún test ni se compiló su código mediante el build sin tests. Las casillas de Tasks
acreditan escritura e inspección, no resultados funcionales. Este apartado prepara la revisión de
cobertura; no ejecuta ni aprueba el gate TESTING.

| Suite/archivo | Cobertura y assertions importantes | Dobles y puntos delicados |
| --- | --- | --- |
| `AutomaticQuoteRecalculationSchedulerTest` (`unit`) | US1/US3, SC-001/005/007/008: invocación habilitada, silencio deshabilitado, evento por cada desenlace, no propagación y siguiente disparo tras fallo. | Service mockeado; captura Logback se desconecta al terminar. El disparo se invoca directamente, no espera tiempo real. |
| `QuotePropertiesTest` (`lightweight`) | US1: defaults, overrides, estrategia desconocida y cron inválido impiden startup del contexto correspondiente. | Service mockeado solo en contexto mínimo con `@EnableScheduling`; sin DB. Cron es validado por Spring, no por el record. |
| `QuoteExecutionTest` (`unit`) | US2, SC-002/006: origen obligatorio, SUCCESS/null, FAILED/motivo cerrado al construir y completar. | Sin mocks ni Spring. |
| `QuoteControllerTest` (`lightweight`) | US2: POST MANUAL, consulta MANUAL/AUTOMATIC, resultado, motivo nulo/cerrado, lista vacía, sin id/cause/stackTrace; errores y JWT existentes. | Services mockeados, MVC/seguridad reales. Revisar que los campos nuevos no alteren el contrato anterior. |
| `QuoteRecalculationServiceTest` (`integration`) | US1/US2/US3, SC-002/004/007: propiedades no predeterminadas, origen persistido, contadores equivalentes, resultado releído, fecha vacía, carrera mixta con un único registro. | Services y PostgreSQL reales; clocks fijos. La carrera permite cualquier ganador; timeout evita espera indefinida. |
| `QuoteQueryServiceTest` (`integration`) | US1: ambas entradas con iguales fixtures y parámetros producen filas completas y precios según el mismo cálculo de dominio. | La preparación invoca el recálculo; Act/Assert consultan el Service de lectura. La expectativa reutiliza `QuoteAdjustment`: detecta diferencias de wiring, no pretende reprobar sus fórmulas. |
| `QuoteExecutionPersistenceTest` (`integration`) | US1: NOT NULL, ausencia de default, catálogo cerrado y unicidad existente. | PostgreSQL Testcontainers y Flyway reales; SQLSTATE y constraint estructurados, sin interpretar mensajes. |
| `QuoteAutomaticRecalculationFailureTest` (`integration`) | US3, SC-003/004/008: sin registro tras fallo completo, reintento manual posible, rechazo sin mercado y fecha consumida aun con fallos individuales. | Reloj controlado falla en la tercera lectura (cierre de corrida). La preparación deja un jugador sin ancla para inducir fallo individual real. No sustituye Service/Repository ni afirma compensación (TD-024). |

Todos los tests nuevos usan una sola `@Tag` literal y grupos `@Nested`. No hay tests frontend,
proveedores reales ni tests E2E nuevos. `ScoreRankingTestData` solo adapta una precondición existente.

### Comandos para ejecución humana, desde `backend/`

```powershell
.\gradlew.bat unitTest --tests "footballmarket.features.quote.*"
.\gradlew.bat lightweightTest --tests "footballmarket.features.quote.*"
.\gradlew.bat integrationTest --tests "footballmarket.features.quote.*"
```

El tercero requiere Docker disponible: la suite provisiona PostgreSQL aislado y aplica migraciones
reales. No usar la base de desarrollo. Para regresión completa, el usuario puede ejecutar
`.\gradlew.bat test`. Revisar primero tests de modelo/disparador, luego configuración/HTTP y al final
persistencia/casos de uso. Si falla algún comando, aportar su salida; no hay resultado PASS supuesto.

## Verificación y límites de entrega

- `gradlew.bat build -x test`: satisfactorio; compilación productiva, formato y clasificación estática.
  El primer intento falló por formato introducido en los archivos modificados; el posterior pasó.
- Sin tests ejecutados, migraciones aplicadas a una base ni servicios levantados por el agente.
- TD-024 permanece pendiente y fuera del alcance. Sin dependencias, rutas, tablas ni fórmulas nuevas.
- Sin hooks de extensiones (`.specify/extensions.yml` ausente), CONVERGE, handoffs, commits ni push.
- Scope de IMPLEMENT completo, revisión y testing humanos pendientes. Phase 6 completada por autorización posterior;
  no se afirma cierre de Feature. El seguimiento remoto observado continúa en `In Progress`; no fue
  modificado ni se afirma sincronización de esta entrega con GitHub.

## Ampliación del Code Review Report — Phase 6

- `docs/backend/architecture.md` §1.8 (T032): documenta el punto de entrada de infraestructura batch,
  sus dependencias exclusivas hacia la interfaz del Service y configuración, ausencia de negocio y
  compensación, y reutilización del scheduler de plataforma. Inspeccionar junto con
  `AutomaticQuoteRecalculationScheduler`; no se introduce un rol con permisos transversales nuevos.
- `tasks.md` y `sdd-gates.md`: avance y evidencia de T032/T033, conservando eventos históricos.
  Este informe incorpora el nuevo archivo documental al inventario de revisión humano.
- Último `gradlew.bat build -x test` desde `backend/`: `BUILD SUCCESSFUL` en 1 s, con caché de
  configuración reutilizada; 1 tarea ejecutada y 10 up-to-date. Clasificación estática: 141 clases.
  No se ejecutaron ni compilaron tests; no se aplicó la migración ni se modificó código funcional.
- El checklist humano anterior sigue pendiente, incluida la revisión de esta documentación.
  No se ejecutaron CONVERGE ni fases posteriores. No se concede ningún gate humano.

## Corrección acotada de T012 tras ejecución humana

El usuario aportó el fallo de `QuotePropertiesTest.rejectsInvalidCronAtStartup`: el contexto
rechaza correctamente el cron, pero la assertion buscaba `cron` en minúsculas en la traza cuando
Spring informa `Cron`. Se reemplazó por comprobación de la causa raíz `IllegalStateException` y
del mensaje específico de seis campos. Se conserva la comprobación de que falla el contexto.
No cambia código productivo, dependencias ni configuración de Mockito; su aviso de auto-attach es
independiente de este fallo. Revisión y reejecución del test corregido pendientes del usuario.

## Corrección acotada de T027 tras ejecución humana

El usuario aportó un fallo de arranque en `QuoteAutomaticRecalculationFailureTest`: el reemplazo
`@TestBean` se declaraba con el subtipo `FailureClock`, mientras que el bean `marketClock` expone
`Clock`. Se alinearon campo y fábrica con `Clock`, manteniendo una instancia `FailureClock` para
inyectar el fallo técnico. `enforceOverride = true` exige reemplazar el bean existente en lugar de
intentar crearlo. No se habilita overriding global ni se cambia `MarketTimeConfiguration`.
La corrección afecta el contexto compartido de los tres escenarios; todos requieren reejecución
humana. El fallo aportado ocurre antes del caso de uso y no demuestra resultados de sus assertions.

## Precisión de la expectativa de reintento de T027

Tras corregir el contexto, el usuario informa dos escenarios satisfactorios y aporta el fallo del
tercero: el reintento manual sí se acepta y completa, pero registra cinco fallos individuales por
cotizaciones `CARRIED_FORWARD` ya confirmadas (`uq_player_quote`), efecto de TD-024. La expectativa
`failedPlayers = 0` agregaba una garantía que la spec vigente no exige y contradecía la ausencia de
compensación acordada. Se sustituye por comprobar los cinco jugadores procesados, conservando las
assertions de fecha sin registro tras fallo completo, origen MANUAL y persistencia del reintento.
No se fijan como requisito ni el éxito del reintento ni la reproducción de cinco duplicados; este
test verifica su disponibilidad. La evidencia del defecto heredado se añadió a TD-024, que sigue
pendiente. No se modificó producción. Revisión y reejecución humanas del escenario corregido pendientes.

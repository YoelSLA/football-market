<!--
Sync Impact Report — Enmienda 2026-10-17
Versión: (sin versionado previo) → 2.0.0. MAJOR: amplía de forma material el alcance de la
prohibición de acceso a la persistencia en los tests de categoría Service, acotando la exclusividad
de §5 y §6 con una excepción explícita y delimitada.
Documento modificado: únicamente este archivo.
Secciones modificadas: §5, §6 y §16. Secciones eliminadas: ninguna. Principios renumerados: ninguno.
Regla adoptada: un test de categoría Service puede preparar sus precondiciones persistentes en
Arrange mediante un helper compartido que acceda a repositorios, siempre que ese acceso no alcance
Act, Assert ni Verify, que el helper no ejecute la operación de negocio bajo prueba y que no contenga
assertions funcionales. La prohibición se mantiene íntegra para Verify y para las demás categorías.
Motivo: `MarketTestData` prepara compradores, catálogo, inventario y saldos por repositorio. La
regla anterior lo declaraba no conforme sin ofrecer una vía alternativa, porque §5 solo admite
Services auxiliares y el dominio de mercado no expone ninguno para aprovisionar inventario. La
excepción cierra ese hueco y, al limitarse a Arrange y prohibir assertions en el helper, impide que
se use para eludir la frontera que §5 protege.
Compatibilidad: §2 y §2.2 (no amplía el alcance funcional de ninguna SPEC), §3 y §4 (la excepción
es de arquitectura de tests y no altera contratos ni garantías de integridad), §6 (no habilita
controles ejecutables ni reduce la prohibición de que el agente ejecute tests), §8 (la excepción
requiere autorización humana expresa, otorgada el 2026-10-17) y §9 (enmienda solicitada y autorizada
expresamente, con impacto y dependencias declarados aquí).
Dependencias: en el mismo cambio se actualizan las referencias cruzadas de §5, §6 y §16 de este
documento. No requieren cambios la Constitución §1 (ya declara este documento normativo con la misma
obligatoriedad), `docs/development/sdd-workflow.md` §8, `AGENTS.md` y `backend/AGENTS.md` (remiten
aquí sin describir la excepción), ni `specs/010-buy-tokens/plan.md`.
Sin cambios en código, migraciones, contratos, Constitución, AGENTS.md ni tecnologías. No se
ejecutaron tests ni builds: la verificación queda a cargo del usuario conforme a la Constitución §6.
Autorización: solicitud explícita del usuario el 2026-10-17.
-->
# Testing del backend

La ejecución queda a cargo del usuario: el agente no ejecuta tests. El frontend está temporalmente exento de testing. Las condiciones completas de verificación y entrega están en la constitución §6 (Verificación); los comandos permitidos, en AGENTS.md.

## Guía de lectura

Leer §1–2 y las reglas transversales §12, §14–18 al crear o modificar tests; consultar además la categoría afectada (§3–11 o §13). Para Service, incluir §6. Al modificar tests, consultar también las [convenciones del backend](conventions.md) sobre referencias a miembros de instancia y Javadoc de tests. No cargar categorías ajenas a la tarea.

## 1. Principios generales

Probar responsabilidades y comportamiento observable, no cobertura por cobertura ni un test por clase. Priorizar resultados, estado final, contratos e invariantes; evitar duplicar pruebas ya suficientes en otra capa. Combinar unitarios precisos, pruebas de capa, integración real controlada y pocos E2E críticos.

Clasificar por alcance y fronteras reales, no por presencia de mocks o `@SpringBootTest`. Usar el mínimo contexto necesario; no elegir contexto completo cuando basten JUnit, un slice o contexto reducido, salvo exigencia específica de categoría.

Los tests deben ser deterministas, independientes y ejecutables aisladamente: sin Internet, proveedores reales, claves productivas, datos externos variables, dependencia del orden, estado de otros tests o aleatoriedad innecesaria. Controlar el tiempo cuando afecte al resultado.

## 2. Categorías y componentes reales

| Categoría | Responsabilidad | Componentes y sustituciones |
| --- | --- | --- |
| Model | Invariantes y comportamiento de dominio | Unitario puro; sin Spring, DB, Repository, Integration ni mocks salvo necesidad excepcional justificada del dominio. |
| Repository | Persistencia propia que requiere prueba independiente | Repository y persistencia reales. |
| Service | Casos de uso y estado persistido observable | Service, Repository, JPA/Hibernate y DB reales; Integration mock. `@SpringBootTest` obligatorio. |
| Orchestrator | Coordinación | Orchestrator real y Services/colaboradores permitidos mock; sin Spring ni DB. |
| Controller | Contrato HTTP y seguridad observable | Preferir `@WebMvcTest`; MVC, Controller, configuración web y Security Filter Chain reales; Service/Orchestrator mock. |
| Integration | Adaptación de proveedor externo | Integration y cliente/protocolo reales; proveedor controlado, como `MockRestServiceServer`. |
| Security | Autenticación y autorización conjuntas | Componentes de seguridad reales; Spring, DB y MockMvc según el flujo. |
| Config | Configuración propia, binding y startup | Instancia directa si basta; preferir `ApplicationContextRunner` para contexto. |
| E2E | Journeys críticos completos desde HTTP | Servidor y componentes internos reales, proveedor externo controlado. |

Mapper no tiene tests directos dedicados: sus transformaciones y errores observables se comprueban mediante Controller y el contrato HTTP.

## 3. Model

Cubrir construcción válida/inválida, invariantes, estado inicial, transiciones, actualizaciones, excepciones de dominio y ausencia de cambios parciales tras rechazos, según corresponda. No probar getters, setters, records o comportamiento trivial generado por Java/Lombok de forma aislada.

## 4. Repository

No probar operaciones estándar heredadas de Spring Data para demostrar el framework; ejercitarlas principalmente desde Service. Crear pruebas específicas para `@Query`, JPQL, SQL nativo, filtros, joins, proyecciones, ordenamientos, queries derivadas complejas o constraints no demostradas adecuadamente desde Service. Comprobar resultados reales, sin mockear el Repository bajo prueba.

## 5. Service

Inyectar con `@Autowired` la interfaz del Service; Spring resuelve su implementación real. Usar el perfil `test` según §12.

Act y Assert funcionales utilizan exclusivamente la API pública productiva del Service bajo prueba. En Arrange pueden utilizarse Services auxiliares reales solo para precondiciones de su responsabilidad; no se convierten en objeto de prueba ni intervienen en Act/Assert del caso principal.

El test no declara, inyecta, accede, invoca, mockea ni verifica Repository, JPA/Hibernate, `EntityManager`, JDBC, SQL o DB directamente, tampoco para preparación, limpieza o assertions. Las fronteras y la excepción técnica están en §6.

**Excepción de Arrange, desde la enmienda 2.0.0**: el test de categoría Service puede delegar la preparación de sus precondiciones persistentes en un helper compartido que acceda a repositorios, con estas condiciones acumulativas y todas obligatorias:

- El acceso a la persistencia ocurre exclusivamente en `Arrange`; `Act` y `Assert` usan solo las APIs públicas productivas del Service bajo prueba.
- El helper no ejecuta la operación de negocio que constituye el comportamiento bajo prueba ni ninguna de sus partes.
- El helper no contiene assertions funcionales: afirma únicamente lo que necesita para preparar su siguiente precondición.
- La excepción no habilita usar repositorios para verificar resultados, ni para limpiar estado en `Verify`, ni para observar credenciales, tenencias o auditoría.
- El test no inyecta ni accede al repositorio por sí mismo: el helper encapsulates el acceso.

Esta excepción no altera §2 (la categoría sigue siendo Service), ni las reglas de Repository de §4, ni las de Security de §10, que conservan íntegra la prohibición de eludir su frontera. Un helper que prepare por repositorio puede ser usado por tests de cualquier categoría, pero solo la categoría Service se beneficia de esta excepción.

Afirmar retorno y estado final observable por el Service: altas, cambios, reactivaciones, bajas, paginación, filtros, existencia, snapshots, entradas completas y consistencia posterior según el caso. Si su contrato no expone un detalle persistido, no eludir la frontera para observarlo ni agregar métodos productivos solo para tests. `verify(repository...)` está prohibido.

Se pueden configurar mocks de Integration con `@MockitoBean`/`when(...)`, pero no invocarlos como Act ni probar su implementación: la ejecución funcional comienza en el Service; el protocolo se verifica en Integration.

## 6. Preparación, helpers y persistencia de Service

Cada test parte de un estado conocido e independiente:

- `Test/Helper → Service → Repository → DB` está permitido mediante APIs públicas productivas y respetando Arrange/Act/Assert de §5.
- `Test/Helper → Repository/JPA/Hibernate/EntityManager/JDBC/SQL/DB` está prohibido si elude esa frontera.

La infraestructura técnica puede acceder a la DB exclusivamente de testing para provisionamiento, aislamiento transaccional u otro aislamiento y restauración. No expone dependencias de persistencia al test ni habilita preparar datos funcionales o hacer assertions eludiendo Service.

**Helper funcional de fixtures (enmienda 2.0.0)**: un componente de test compartido puede, además, preparar datos funcionales por repositorio **solo al servicio de la categoría Service y únicamente en `Arrange`**, conforme a las condiciones de la excepción de §5. Ese helper no se considera «infraestructura técnica» a los efectos del párrafo anterior y por eso sí puede exponer su acceso a la persistencia; lo que no puede es cruzar a `Act`, `Assert` o `Verify`, ejecutar el caso de uso bajo prueba ni afirmar resultados funcionales. El helper es reutilizable y su ubicación en `support` es la de §16.

Crear datos funcionales por el Service bajo prueba o auxiliares permitidos. Ninguna estrategia puede afectar bases ajenas a testing ni justificar métodos productivos exclusivos para tests.

## 7. Orchestrator

Verificar colaboradores invocados, datos transferidos, resultados propagados/compuestos y fallos que impiden invocaciones posteriores. Se permiten `verify(...)`, `verifyNoInteractions(...)` y orden cuando la coordinación lo requiera. No repetir lógica interna de Services.

## 8. Controller y contrato HTTP

Comprobar rutas, métodos, parámetros, path variables, bodies, DTO, serialización, status, estructura/contenido exactos del contrato, validaciones, errores, `GlobalExceptionHandler` y seguridad HTTP. No acceder directamente a Repository; DB solo con justificación excepcional. No repetir lógica del Service.

Verificar rechazos de entradas inválidas y ausencia de interacción con Service/Orchestrator cuando aporte valor demostrar rechazo previo al caso de uso, como paginación inválida o JWT ausente/inválido.

Todo endpoint y cada caso distinto de respuesta contractual debe quedar cubierto por tests de Controller que afirmen el comportamiento observable: status, estructura y contenido exactos. No hay subconjunto opcional. No duplicar la afirmación de un mismo caso HTTP por ejecución parametrizada o por pruebas internas cuando otro test ya lo cubre.

El contrato HTTP del backend se documenta y mantiene mediante cuatro fuentes que deben permanecer coherentes entre sí:

- **Springdoc OpenAPI**, que genera la especificación y la UI a partir de los endpoints y sus anotaciones.
- **Las anotaciones OpenAPI de Controllers y DTO**, que son la fuente de la especificación y deben reflejar propósito, ruta, parámetros, headers, autenticación, campos, validaciones, códigos y errores.
- **Los contratos definidos en `specs/*/contracts/`**, que son la referencia funcional y normativa de la API de cada feature.
- **Los tests funcionales de Controller**, que verifican que el comportamiento real coincide con ese contrato.

La documentación OpenAPI no sustituye las assertions: primero se afirma status, estructura y contenido, y la anotación se mantiene sincronizada con lo verificado. Controller es su fuente principal; E2E no está obligado a duplicarla.

Las anotaciones deben poder utilizarse sin credenciales reales, y sus ejemplos no sustituyen a los contratos de `specs/*/contracts/`: cuando un contrato normativo y un ejemplo diverjan, prevalece el contrato.

## 9. Integration externa

No realizar requests a proveedores reales. Verificar según el contrato: URL, path, query, headers, autenticación técnica, serialización/deserialización, mapeo, datos incompletos, respuestas inválidas, códigos, timeouts, rate limiting, política/límite de retries y traducción a excepciones propias de Integration. Las capas superiores no conocen excepciones del cliente HTTP. Comprobar ausencia de filtraciones sensibles en URLs, respuestas y excepciones cuando sea parte del contrato de seguridad.

## 10. Security

Verificar ausencia de token, token válido, firma inválida, expiración y flujos registro/login/JWT/acceso protegido según corresponda. Se permite un endpoint mínimo exclusivo del test para infraestructura de seguridad en lugar de un Controller productivo. Ubicar estos tests en `security`, nunca en `integrations`.

## 11. Configuración

Verificar `@ConfigurationProperties`, binding, validación, beans, startup, clientes seguros, rechazo de valores inválidos y protección de secretos en representaciones como `toString()`. Si una property debe impedir startup, demostrar el fallo del contexto.

## 12. Perfil `test` y DB

`@ActiveProfiles("test")` solo corresponde al consumir `application-test.yml` o su infraestructura:

| Test | Perfil |
| --- | --- |
| Service y E2E | Obligatorio. |
| Security/contexto completo/slice web | Cuando necesite DB, secreto JWT u otros beans/properties del perfil. |
| Model, Orchestrator unitario, instancias manuales, Integration autocontenida, Config con properties explícitas | No utilizar. |

Todo test que necesite persistencia utiliza PostgreSQL real mediante Testcontainers: instancia aislada y desechable, provisionada por la suite con Docker disponible y migraciones reales de Flyway aplicadas antes de probar. Prohibidos H2 u otros sustitutos, PostgreSQL externo/preinstalado/local, dependencia de una base preexistente y esquemas manuales divergentes. Nunca usar bases de desarrollo o producción.

## 13. E2E

Levantar servidor real con `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` y perfil `test`. Ejecutar journeys desde HTTP exclusivamente con `org.springframework.web.client.RestClient`, no MockMvc, TestRestTemplate, WebTestClient u otros clientes.

Serializar Request DTO productivos y deserializar Response DTO productivos, incluido `ErrorResponseDTO`. Comprobar respuestas sin body sin crear DTO artificial. Prohibido construir, concatenar o interpretar JSON manualmente y usar `ObjectMapper` para sustituir DTO.

Spring, MVC, Security, Controllers, Orchestrators, Services, Repositories, JPA/Hibernate, DB e Integrations productivas son reales. Apuntar `base-url` a un proveedor simulado atravesando el cliente HTTP real.

Cubrir pocos flujos críticos, como registro/login/acceso protegido o sincronización/consulta de catálogo, sin repetir exhaustivamente combinaciones de otras capas. Invocar Service directamente o usar `@SpringBootTest` por sí solo no es E2E.

## 14. Mocks

Sustituir solo fronteras deliberadas según §2, nunca el componente bajo prueba ni dependencias reales necesarias para demostrar el objetivo. Usar `verify(...)` cuando la interacción sea comportamiento relevante —coordinación o rechazo previo—, prefiriendo resultados observables estables cuando existan.

## 15. Assertions y casos

Afirmar resultados significativos: retorno, estado, excepciones, HTTP, invariantes y persistencia observable por la frontera permitida, sin acoplarse a detalles privados. Para atomicidad, probar rechazo y ausencia de cambios parciales; para campos exactos, comprobar el contrato completo.

Considerar happy path, entradas inválidas, ausencia, límites, estado previo/inexistente, errores internos/externos, vacíos, duplicados y transiciones según corresponda. Usar `@ParameterizedTest` para inputs de la misma regla y expectativa cuando comunique mejor que pruebas repetidas.

## 16. Organización y legibilidad

Bajo `src/test/java/footballmarket/`, ubicar por responsabilidad: `models`, `repositories`, `services`, `orchestrators`, `controllers`, `integrations`, `security`, `config` o `e2e`. `integrations` significa adaptadores externos, no cualquier prueba técnicamente de integración.

El uso de `@Nested` de JUnit es obligatorio en todos los tests del backend, en todas las categorías de §2, tanto existentes como nuevos. Agrupar los casos en clases anidadas por funcionalidad o caso de uso; todos los métodos de prueba, incluidos los parametrizados, deben pertenecer a un grupo `@Nested`, sin métodos de prueba directamente en la clase contenedora. Esta regla también aplica cuando solo exista una funcionalidad o un caso de prueba. Nombrar cada grupo de forma que identifique la funcionalidad que verifica.

Está prohibido usar `this` en todo el código de tests, incluidos métodos de prueba, ciclo de vida, helpers y clases `@Nested`. Acceder a atributos y métodos de instancia directamente, sin `this` ni referencias calificadas como `OuterTest.this`. Evitar nombres de parámetros o variables locales que oculten miembros necesarios de la instancia. Esta regla es la excepción explícita a la convención de código productivo de [convenciones del backend](conventions.md).

Nombres que expresen condición y resultado; estructura Arrange/Act/Assert/Verify explícita cuando corresponda, sin bloques vacíos. `fixtures`, `builders` y `support` se permiten con reutilización real para datos o infraestructura compartida, no como categorías nuevas.

### Estilo de los escenarios

- Un método prueba un comportamiento o escenario concreto. Separar reglas independientes (por ejemplo, crear, actualizar, reactivar e inactivar) en tests distintos; conservar juntas las comprobaciones que describen el mismo resultado. Un journey E2E puede abarcar pasos sucesivos de un único flujo crítico.
- Usar `// Arrange` para datos, mocks y precondiciones; `// Act` para la operación bajo prueba; `// Assert` para retorno o excepción; `// Verify` para estado persistido observable e interacciones o consecuencias. Omitir los bloques sin contenido. Las operaciones previas que preparan estado, incluso cuando llaman al método bajo prueba, pertenecen a Arrange. Si una excepción se afirma al ejecutar la acción, `// Act / Assert` es válido.
- No utilizar `var` en tests: declarar tipos explícitos, también para páginas, snapshots, resultados, builders y recursos concurrentes.
- Declarar entidades y objetos relevantes antes de incluirlos en `List.of(...)`, snapshots, requests o llamadas a Services: `Player player = new Player(1L, "N", "T", "L", "P"); List<Player> players = List.of(player); PlayerSnapshot snapshot = new PlayerSnapshot(players, 1, 0);`. No crear variables para literales triviales cuando no aportan claridad.
- Los identificadores siguen las convenciones de idioma de la constitución. Dar a cada método un nombre corto y descriptivo, y a cada test un `@DisplayName` en español natural que explique condición y comportamiento observable. Bien: `creaJugadorNuevo` con `@DisplayName("Crea un jugador cuando no existe en el catálogo")`; mal: `testPlayer`, `works` o `creaActualizaReactivaEInactiva`. El nombre visible no debe limitarse a repetir literalmente el método.
- Agrupar mediante `@Nested` según §16 y dar al grupo un `@DisplayName` que describa la funcionalidad compartida (por ejemplo, «Sincronización del catálogo»), sin anidamientos adicionales superfluos.
- Parametrizar únicamente cuando varíen las entradas de una misma regla y expectativa. Preferir `@CsvSource` para combinaciones, `@ValueSource` para un valor y `@NullAndEmptySource` para ausencias; usar otras fuentes cuando aporten claridad. Dar también un `@DisplayName` humano a los casos parametrizados. No juntar reglas conceptualmente distintas para ahorrar líneas.
- Las assertions deben demostrar la promesa del test: retorno, excepción o respuesta directa en Assert; efecto observable y colaboraciones relevantes en Verify. No añadir comprobaciones ajenas para incrementar cobertura ni verificar implementación privada. Respetar la frontera de cada categoría (§2–14).
- Antes de cambiar una expectativa, leer implementación y contrato; no modificar código productivo para hacer pasar un test ni ajustar silenciosamente un valor esperado por un fallo. Registrar discrepancias cuya semántica no esté clara.

Ejemplo de Service (con estado inicial ya conocido):

```java
@Test
@DisplayName("Crea un jugador cuando todavía no existe en el catálogo")
void creaJugadorNuevo() {
  // Arrange
  Player player = new Player(1L, "Name", "Team", "League", "Forward");
  List<Player> players = List.of(player);
  PlayerSnapshot snapshot = new PlayerSnapshot(players, 1, 0);

  // Act
  PlayerSynchronizationResult result = playerCatalogService.applySynchronization(snapshot);

  // Assert
  assertThat(result.created()).isEqualTo(1);

  // Verify
  Page<Player> activePlayers = playerCatalogService.getActivePlayers(0, 20);
  assertThat(activePlayers.getContent()).extracting(Player::getId).containsExactly(1L);
}
```

Helpers privados o compartidos no contienen assertions funcionales ni ocultan comportamiento o flujos de negocio. Respetan la frontera de su categoría, incluida §6 para persistencia de Service y la excepción de Arrange de §5. Un helper que prepara por repositorio puede afirmar lo que necesita para completar su propia precondición —por ejemplo, localizar la fila que acaba de insertar—, y esa comprobación es preparación, no una assertion funcional.

## 17. Cambios y cobertura

Todo comportamiento backend nuevo o modificado requiere tests automatizados suficientes; agregar regresión para defectos cuando sea razonable. Incluir pruebas para cambios de seguridad y contratos relevantes, capaces de detectar incompatibilidades.

Elegir el nivel más específico capaz de demostrar la responsabilidad (§2), sin E2E como sustituto de pruebas de capa. Los cambios transversales pueden requerir varios niveles: por ejemplo, query compleja en Repository y caso de uso en Service. La suite debe localizar fallos y demostrar las conexiones relevantes entre componentes.

## 18. Entrega

Aplicar la constitución §6 (Verificación): el agente escribe o modifica tests del backend cuando corresponda, pero su ejecución queda a cargo del usuario. No modificar controles para ocultar fallos. Informar resultados disponibles y comprobaciones pendientes sin presentar el build como prueba de comportamiento.

**Versión**: 2.0.0 | **Última enmienda**: 2026-10-17 (excepción de Arrange para tests de categoría Service; ver el Sync Impact Report al inicio)

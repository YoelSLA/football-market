# Testing del backend

La ejecución queda a cargo del usuario: el agente no ejecuta tests. El frontend está temporalmente exento de testing. Las condiciones completas de verificación y entrega están en la constitución §6; los comandos permitidos, en AGENTS.md.

## Guía de lectura

Leer §1–2 y las reglas transversales §12, §14–18 al crear o modificar tests; consultar además la categoría afectada (§3–11 o §13). Para Service, incluir §6. Al modificar tests, consultar también arquitectura §2.6 y el párrafo de Javadoc de tests en §2.8. No cargar categorías ajenas a la tarea.

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

Afirmar retorno y estado final observable por el Service: altas, cambios, reactivaciones, bajas, paginación, filtros, existencia, snapshots, entradas completas y consistencia posterior según el caso. Si su contrato no expone un detalle persistido, no eludir la frontera para observarlo ni agregar métodos productivos solo para tests. `verify(repository...)` está prohibido.

Se pueden configurar mocks de Integration con `@MockitoBean`/`when(...)`, pero no invocarlos como Act ni probar su implementación: la ejecución funcional comienza en el Service; el protocolo se verifica en Integration.

## 6. Preparación, helpers y persistencia de Service

Cada test parte de un estado conocido e independiente:

- `Test/Helper → Service → Repository → DB` está permitido mediante APIs públicas productivas y respetando Arrange/Act/Assert de §5.
- `Test/Helper → Repository/JPA/Hibernate/EntityManager/JDBC/SQL/DB` está prohibido si elude esa frontera.

La infraestructura técnica puede acceder a la DB exclusivamente de testing para provisionamiento, aislamiento transaccional u otro aislamiento y restauración. No expone dependencias de persistencia al test o helper funcional ni habilita preparar datos funcionales o hacer assertions eludiendo Service.

Crear datos funcionales por el Service bajo prueba o auxiliares permitidos. Ninguna estrategia puede afectar bases ajenas a testing ni justificar métodos productivos exclusivos para tests.

## 7. Orchestrator

Verificar colaboradores invocados, datos transferidos, resultados propagados/compuestos y fallos que impiden invocaciones posteriores. Se permiten `verify(...)`, `verifyNoInteractions(...)` y orden cuando la coordinación lo requiera. No repetir lógica interna de Services.

## 8. Controller y Spring REST Docs

Comprobar rutas, métodos, parámetros, path variables, bodies, DTO, serialización, status, estructura/contenido exactos del contrato, validaciones, errores, `GlobalExceptionHandler` y seguridad HTTP. No acceder directamente a Repository; DB solo con justificación excepcional. No repetir lógica del Service.

Verificar rechazos de entradas inválidas y ausencia de interacción con Service/Orchestrator cuando aporte valor demostrar rechazo previo al caso de uso, como paginación inválida o JWT ausente/inválido.

Todo endpoint y caso distinto de respuesta contractual debe quedar documentado por tests de Controller mediante Spring REST Docs: no hay subconjunto opcional. No duplicar snippets por ejecución parametrizada o pruebas internas cuando otro test documenta el mismo caso HTTP.

Primero afirmar status, estructura y contenido; REST Docs deriva del comportamiento verificado y no sustituye assertions. Mantener snippets y descripciones sincronizados con request, response, status y campos. Controller es su fuente principal; E2E no está obligado a duplicarlos.

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

Está prohibido usar `this` en todo el código de tests, incluidos métodos de prueba, ciclo de vida, helpers y clases `@Nested`. Acceder a atributos y métodos de instancia directamente, sin `this` ni referencias calificadas como `OuterTest.this`. Evitar nombres de parámetros o variables locales que oculten miembros necesarios de la instancia. Esta regla es la excepción explícita a la convención de código productivo de arquitectura §2.6.

Nombres que expresen condición y resultado; estructura conceptual Arrange/Act/Assert sin comentarios artificiales. `fixtures`, `builders` y `support` se permiten con reutilización real para datos o infraestructura compartida, no como categorías nuevas.

Helpers privados o compartidos no contienen assertions ni ocultan comportamiento o flujos de negocio. Respetan la frontera de su categoría, incluida §6 para persistencia de Service.

## 17. Cambios y cobertura

Todo comportamiento backend nuevo o modificado requiere tests automatizados suficientes; agregar regresión para defectos cuando sea razonable. Incluir pruebas para cambios de seguridad y contratos relevantes, capaces de detectar incompatibilidades.

Elegir el nivel más específico capaz de demostrar la responsabilidad (§2), sin E2E como sustituto de pruebas de capa. Los cambios transversales pueden requerir varios niveles: por ejemplo, query compleja en Repository y caso de uso en Service. La suite debe localizar fallos y demostrar las conexiones relevantes entre componentes.

## 18. Entrega

Aplicar la constitución §6: el agente escribe o modifica tests del backend cuando corresponda, pero su ejecución queda a cargo del usuario. No modificar controles para ocultar fallos. Informar resultados disponibles y comprobaciones pendientes sin presentar el build como prueba de comportamiento.

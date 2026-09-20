<!--
Sync Impact Report:

- Version change: 2.2.1 -> 2.3.0

- List of modified principles:
  - 4. Testing

- Added sections:
  - None

- Removed sections:
  - None

- Added requirements:
  - `RestClient` obligatorio como cliente HTTP en tests E2E.
  - Request y Response DTO obligatorios para cuerpos HTTP en tests E2E.

- Removed requirements:
  - JSON literal manual mediante text blocks en tests E2E.

- Corrected references:
  - None

- Follow-up TODOs:
  - None
-->

## 1. Arquitectura

### 1.1. Fundamentos generales

Cada componente debe tener una responsabilidad arquitectónica clara y mantener alta cohesión.

Los componentes deben encapsular sus detalles de implementación y exponer únicamente lo necesario mediante contratos.

Un componente debe dividirse únicamente cuando exista una separación real de responsabilidades, falta de cohesión o una necesidad arquitectónica explícita. El tamaño o cantidad de código no justifica por sí solo una división.

El nuevo comportamiento debe incorporarse al componente cuya responsabilidad corresponda.

### 1.2. Arquitectura del backend

El backend se organiza en:

- Presentación: Controller.
- Aplicación: Orchestrator y Service.
- Dominio: Model.
- Persistence: Repository.
- Integración externa: Integration.
- Soporte HTTP: DTO y Mapper.

#### 1.2.1. Controller

Representa la frontera HTTP.

Puede utilizar DTO, Mapper, Service y Orchestrator.

No debe contener lógica de negocio ni acceder directamente a Model, Repository o Integration.

Puede pasar directamente a Service u Orchestrator el Model devuelto por un Mapper, y pasar directamente a un Mapper el Model devuelto por Service u Orchestrator. En ambos casos no debe construirlo, declararlo, modificarlo, inspeccionarlo ni aplicar lógica sobre él.

Cuando una operación HTTP intercambie datos de dominio, el flujo permitido es: DTO → Mapper → Model → Service u Orchestrator → Model → Mapper → DTO.

Se asume que Service u Orchestrator no devuelven `null` cuando el contrato de la operación espera una respuesta.

#### 1.2.2. Orchestrator

Coordina operaciones de aplicación cuando exista una responsabilidad de coordinación que no corresponda a Controller o a un único Service.

Puede utilizar Service y Model.

No debe contener lógica de dominio.

No puede depender de Repository, Integration ni otro Orchestrator.

El uso de múltiples Services no obliga por sí solo a crear un Orchestrator.

Todo Orchestrator debe ubicarse en el paquete global `footballmarket.orchestrators`. No deben
existir paquetes de Orchestrator dentro de cada feature ni ubicarse Orchestrator dentro de paquetes
correspondientes a Controller, Service, Repository u otro rol.

#### 1.2.3. Service

Implementa la lógica de aplicación.

Todo Service debe definir una interfaz y una implementación separadas. Los consumidores deben
depender de la interfaz; la implementación concreta no debe utilizarse como contrato. Esta regla
es obligatoria incluso cuando exista una única implementación y, por ser una regla arquitectónica
específica, prevalece sobre la prohibición general de crear interfaces innecesarias.

#### 1.2.4. Model

Representa conceptos, estado, comportamiento e invariantes del dominio.

Las reglas que puedan resolverse exclusivamente con información del dominio deben implementarse en Model.

No puede depender de Service, Repository ni Integration.

#### 1.2.5. Repository

Gestiona el acceso a datos persistidos.

No debe contener lógica de negocio.

Puede utilizar Model.

No puede depender de Service ni DTO.

La anotación `@Repository` debe utilizarse únicamente en interfaces de Repository de Spring Data,
como aquellas que extiendan `JpaRepository`. No debe utilizarse en puertos de persistencia,
implementaciones manuales, repositorios de test ni otras clases o interfaces que cumplan
conceptualmente un rol de persistencia.

#### 1.2.6. Integration

Puede ser utilizada por Service.

No puede ser utilizada directamente por Controller, Orchestrator, Model ni Repository.

### 1.3. DTO y Mapper

#### 1.3.1. DTO

Los DTO representan los contratos HTTP de entrada y salida.

Service, Model y Repository no deben depender de DTO.

#### 1.3.2. Mapper

Los Mapper transforman exclusivamente entre DTO y Model:

DTO → Model
Model → DTO

No deben contener lógica de negocio, validaciones de negocio, cálculos ni decisiones semánticas.

No pueden depender de Service ni Repository.

### 1.4. Dependencias arquitectónicas

Las dependencias directas permitidas son:

Controller → DTO, Mapper, Service, Orchestrator
Orchestrator → Service, Model
Service → Model, Repository, Integration
Repository → Model

Las relaciones de DTO y Mapper se rigen por la sección 1.3.

Toda dependencia no listada está prohibida.

Las dependencias transitivas no autorizan dependencias directas.

No deben existir dependencias circulares ni utilizarse intermediarios para evadir una restricción arquitectónica.

### 1.5. Reglas de dominio y aplicación

Las reglas que puedan resolverse exclusivamente con información y comportamiento del dominio deben implementarse en Model.

Las reglas que requieran Repository, Integration u otras capacidades externas al dominio deben implementarse en Service.

Las reglas que involucren múltiples Model pueden permanecer en el dominio cuando puedan resolverse completamente mediante comportamiento de dominio.

### 1.6. Evolución y nuevas áreas

La arquitectura definida por esta Constitution prevalece sobre estructuras existentes.

La existencia de clases, paquetes, interfaces o convenciones previas no obliga a conservarlas cuando contradigan la arquitectura.

Las refactorizaciones necesarias para cumplir la arquitectura están permitidas.

Toda nueva área debe definir explícitamente sus responsabilidades, componentes y dependencias.

### 1.7. Resolución arquitectónica

Las dependencias y responsabilidades no deben inferirse por conveniencia técnica.

Cuando una situación relevante no pueda resolverse mediante las reglas existentes, la Spec o el código aplicable, el agente debe solicitar aclaración en lugar de inventar una regla arquitectónica.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.

### 2.2. DTO

Los DTO HTTP deben implementarse como `record`.

Los Request DTO pueden utilizar Jakarta Validation cuando las restricciones correspondan al contrato o a requisitos funcionales.

Los mensajes de validación propios de la aplicación deben estar en español.

La obligatoriedad y nulabilidad deben representar correctamente el contrato HTTP.

### 2.3. Model

Los Model deben preservar sus invariantes y encapsular su estado.

No deben utilizar `Builder`.

No deben utilizarse `@Setter` en los Model.

Pueden utilizarse mecanismos de Persistence necesarios para representar el Model.

### 2.4. Mapper

Los Mapper deben ser `final`, sin estado mutable, con constructor privado y métodos `static`.

No deben utilizarse instancias de Mapper.

Un Mapper que pueda ser invocado fuera de un endpoint HTTP validado con Jakarta Validation y reciba `null` debe lanzar la excepción de mapeo definida por el proyecto.

No se requiere una validación de `null` específica cuando el Mapper sólo sea alcanzable desde un endpoint HTTP cuya validación estructural ya impida ese caso.

No debe convertir `null` en valores por defecto.

### 2.5. Excepciones

El código de la aplicación no debe lanzar directamente excepciones genéricas provistas por Java,
como `RuntimeException`, `IllegalArgumentException` o `IllegalStateException`, para representar
errores funcionales, de aplicación o de dominio.

Todas las excepciones propias y controladas creadas por el proyecto deben heredar, directa o
indirectamente, de `FootballMarketException`. Deben clasificarse por su naturaleza mediante
`DomainException`, `IntegrationException` o `PersistenceException`, y cada excepción concreta debe
heredar de la categoría correspondiente.

Las excepciones pertenecientes a Java, Spring o librerías externas no están alcanzadas por esta
regla. Pueden capturarse y traducirse en los límites apropiados; no se exige reemplazar el
funcionamiento interno de esas tecnologías.

### 2.6. Referencias a miembros de instancia

Toda clase debe utilizar `this` al acceder a sus atributos de instancia y al invocar sus propios
métodos de instancia, incluso cuando no exista ambigüedad. La regla se aplica también a clases de
test, métodos triviales y al código de los `record` definido explícitamente por el proyecto.

No aplica a miembros `static`, variables locales, parámetros que no sean miembros de la instancia
ni código generado automáticamente por Java, Lombok, frameworks u otras herramientas.

### 2.7. Framework y tecnología

Las tecnologías y frameworks establecidos por el proyecto deben utilizarse respetando la arquitectura.

Las anotaciones, clases base e interfaces proporcionadas por los frameworks no deben utilizarse para introducir responsabilidades o dependencias prohibidas.

## 3. Integraciones externas

### 3.1. Encapsulamiento

Toda comunicación con sistemas externos debe realizarse mediante `Integration`.

`Integration` encapsula:
- comunicación y configuración del proveedor;
- requests y responses externos;
- transformación de datos externos;
- errores técnicos de integración.

`Model` no debe contener detalles de HTTP, clientes, proveedores ni protocolos externos.

### 3.2. Respuestas y errores

Las respuestas externas deben validarse antes de ser utilizadas.

Los errores técnicos de integración deben mantenerse diferenciados de los errores de dominio.

No se deben inventar datos ni modificar silenciosamente el comportamiento funcional ante respuestas inválidas o fallos externos.

### 3.3. Timeouts y reintentos

Las comunicaciones externas deben utilizar timeouts cuando la tecnología lo permita.

Los reintentos no deben implementarse por defecto.

Solo pueden utilizarse cuando la operación sea segura para reintentar y exista un límite explícito que evite efectos duplicados o cargas innecesarias.

### 3.4. Disponibilidad

La indisponibilidad de un sistema externo debe manejarse según los requisitos de la funcionalidad.

No se deben introducir automáticamente cachés, degradaciones ni comportamientos alternativos sin una justificación funcional o técnica.

### 3.5. Contratos y seguridad

Los contratos externos deben mantenerse aislados de los contratos HTTP y de los Model internos.

Los secretos, tokens y credenciales no deben almacenarse en el código ni exponerse en logs o respuestas.

Las comunicaciones externas deben utilizar mecanismos seguros y no deben degradarse para facilitar la implementación.

## 4. Testing

### 4.1. Principios generales

La suite debe organizarse alrededor de responsabilidades arquitectónicas y comportamientos
observables, no de alcanzar cobertura por cobertura ni de crear obligatoriamente un test por cada
clase. Cada test debe tener un propósito concreto y demostrar una responsabilidad de la capa o
componente bajo prueba.

Los tests deben priorizar comportamiento observable, resultado, estado final, contratos e
invariantes sobre detalles internos. No deben duplicar sin necesidad una responsabilidad ya
demostrada en una capa inferior o más específica. Ningún tipo de test sustituye universalmente a
los demás: la suite debe combinar tests unitarios precisos, tests de capa, integraciones con
infraestructura real controlada y pocos E2E de flujos críticos.

La existencia de mocks no convierte por sí sola una prueba en unitaria. El uso de
`@SpringBootTest` no convierte por sí solo una prueba en E2E. La clasificación depende del alcance
real verificado y de qué fronteras permanecen reales o son sustituidas. Debe utilizarse la mínima
infraestructura necesaria; `@SpringBootTest` no debe utilizarse por defecto cuando sean suficientes
JUnit puro, un slice de Spring o un contexto reducido.

Los tests deben ser deterministas, repetibles, reproducibles e independientes. No deben depender
de Internet, servicios externos reales, API keys productivas, datos externos variables, orden de
ejecución, estado dejado por otros tests, un reloj externo controlable ni aleatoriedad innecesaria.
Cada test debe poder ejecutarse de manera aislada.

### 4.2. Clasificación y alcance

Cada área debe demostrar exclusivamente su responsabilidad principal:

- `Model`: invariantes y comportamiento de dominio.
- `Repository`: comportamiento de persistencia propio que requiera prueba independiente.
- `Service`: casos de uso, reglas de aplicación y estado persistido observable.
- `Orchestrator`: coordinación entre casos de uso.
- `Controller`: contrato HTTP y seguridad observable desde HTTP.
- `Integration`: adaptación técnica frente a sistemas externos.
- `Security`: funcionamiento conjunto de autenticación y autorización.
- `Config`: binding, validación y construcción de infraestructura.
- `E2E`: flujos críticos completos desde la frontera HTTP.

Los Mapper no deben tener tests directos dedicados. Sus transformaciones y errores observables
deben quedar verificados a través de los tests de Controller y del contrato HTTP que soportan.

### 4.3. Tests de Model

Los tests ubicados en `src/test/java/footballmarket/models` deben ser unitarios puros. Deben
demostrar que el dominio protege invariantes, reglas de validez, transiciones de estado,
comportamiento propio, modificaciones de estado y atomicidad cuando corresponda.

Deben cubrir, según el comportamiento existente, construcción válida e inválida, estado inicial,
activación y desactivación, actualizaciones permitidas, excepciones de dominio y ausencia de
modificaciones parciales tras una operación rechazada.

No deben levantar Spring, utilizar `@SpringBootTest`, base de datos, Repository, Integration,
perfil `test` ni mocks, salvo una necesidad excepcional justificada por el diseño del dominio.
No deben utilizar `@ActiveProfiles("test")`.

No deben existir tests dedicados únicamente a comportamiento trivial provisto por Java, `record` o
Lombok, como getters o setters sin lógica. Esos miembros solo deben quedar cubiertos como parte de
una comprobación significativa del dominio.

### 4.4. Tests de Repository

No es obligatorio crear un test por cada Repository. Las operaciones estándar heredadas de Spring
Data JPA, como `save`, `findById`, `findAll`, `delete` y `deleteAll`, no deben probarse aisladamente
para demostrar el funcionamiento del framework. La persistencia estándar debe ejercitarse
principalmente de forma indirecta mediante tests de Service.

Deben existir tests específicos cuando haya comportamiento de persistencia propio que necesite
verificación independiente: `@Query`, JPQL, SQL nativo, filtros complejos, joins, proyecciones,
ordenamientos relevantes, queries derivadas complejas o constraints no demostradas adecuadamente
desde Service.

Estos tests deben usar la infraestructura real de persistencia de test y comprobar resultados
observables. Nunca deben mockear el Repository bajo prueba.

### 4.5. Tests de Service

Los tests ubicados en `src/test/java/footballmarket/services` deben comprobar casos de uso, reglas
de negocio y cambios de estado. Son deliberadamente tests de integración con Service, Repository,
JPA/Hibernate y base de datos de test reales; las Integrations externas deben ser mocks y los
servicios externos reales no deben utilizarse.

Deben utilizar `@SpringBootTest` y `@ActiveProfiles("test")`. El Service bajo prueba debe declararse
mediante su interfaz e inyectarse con `@Autowired`; Spring debe resolver su implementación real.

El Act y el Assert funcional solo pueden interactuar con la API pública del Service bajo prueba.
Durante el Arrange, el test puede inyectar e invocar otros Services reales exclusivamente para
crear precondiciones pertenecientes a sus responsabilidades. Esos Services auxiliares no pueden
convertirse en objeto de prueba, utilizarse durante el Act ni emplearse para verificar el resultado
del caso de uso principal.

El código del test no puede declarar, inyectar, acceder, invocar, mockear ni verificar directamente
ningún Repository, ni siquiera para Arrange, limpieza o assertions. Tampoco puede acceder
directamente a JPA, Hibernate, `EntityManager`, JDBC ni la base de datos. Repository, JPA y base de
datos quedan ejercitados indirectamente y exclusivamente a través de Services reales.

Todo Act y Assert funcional debe realizarse mediante la API pública productiva del Service bajo
prueba. El Arrange debe utilizar su API o, únicamente para precondiciones externas a su
responsabilidad, las APIs públicas productivas de Services auxiliares. No deben agregarse métodos
productivos a ningún Service únicamente para facilitar los tests. Cuando el contrato público del
Service bajo prueba no permita observar un detalle interno de persistencia, el test debe afirmar
únicamente el comportamiento observable que dicho contrato exponga; no debe vulnerar la frontera
para obtener acceso al detalle.

Los tests deben favorecer el resultado retornado y el estado final observable mediante el Service:
altas, modificaciones, reactivaciones, bajas lógicas, reglas de negocio, paginación, filtrado,
entidades existentes o inexistentes, snapshots o entradas completas y consistencia posterior.
`verify(repository...)` está prohibido y nunca sustituye la comprobación del resultado observable.

El test puede declarar y configurar un mock de Integration mediante mecanismos como
`@MockitoBean` y `when(...)`. No puede invocar la Integration para ejecutar el comportamiento
funcional ni probar su implementación. La ejecución debe comenzar siempre mediante el Service; el
protocolo de la Integration se prueba en sus propios tests.

### 4.6. Preparación y limpieza de persistencia

Cada test de Service debe partir de un estado conocido, independiente y determinista. No puede
depender de datos creados por otro test ni del orden de ejecución.

La preparación y limpieza deben respetar la frontera absoluta del Service definida en 4.5. No se
permite usar Repository, JPA, JDBC, SQL ni acceso directo a la base de datos desde el test. Debe
utilizarse aislamiento transaccional o un mecanismo de infraestructura de testing que restaure un
estado conocido sin exponer esas dependencias al código del test. Si se necesitan datos
funcionales, deben crearse mediante operaciones públicas productivas del Service bajo prueba o de
un Service auxiliar cuando la precondición pertenezca a la responsabilidad de este último.

Ninguna estrategia de preparación o limpieza puede afectar una base que no sea exclusiva de
testing ni puede motivar la incorporación de métodos productivos destinados solo a tests.

### 4.7. Tests de Orchestrator

Los tests ubicados en `src/test/java/footballmarket/orchestrators` deben ser tests unitarios de
coordinación. El Orchestrator debe ser real y sus Services u otros colaboradores permitidos deben
ser mocks.

Su objetivo exclusivo es comprobar qué colaboradores se invocan, qué datos se transfieren, qué
resultado se propaga o compone y cuándo una falla impide invocaciones posteriores. Se permite
`verify(...)`, `verifyNoInteractions(...)` y verificar orden cuando la interacción o el orden sean
parte del comportamiento de coordinación.

No deben levantar `@SpringBootTest`, utilizar base de datos, repetir lógica interna de los Services
ni usar `@ActiveProfiles("test")` cuando sean unitarios puros.

### 4.8. Tests de Controller y Spring REST Docs

Los tests ubicados en `src/test/java/footballmarket/controllers` deben verificar el contrato HTTP:
rutas, métodos, parámetros, path variables, bodies, DTO, serialización, códigos, estructura exacta
de respuestas contractuales, validaciones, errores, `GlobalExceptionHandler` y seguridad visible
desde HTTP.

Deben preferir `@WebMvcTest(...)`: Spring MVC, Controller, configuración web relevante y Security
Filter Chain deben ser reales; Service y Orchestrator deben ser mocks; Repository no debe
utilizarse directamente y la base de datos no es necesaria salvo justificación excepcional. No
deben volver a probar la lógica interna del Service.

Las entradas inválidas deben rechazarse con el código y contrato de error correspondientes. Cuando
el rechazo deba ocurrir antes del caso de uso, debe comprobarse la ausencia de interacción con
Service u Orchestrator si esto aporta valor, por ejemplo ante paginación inválida o JWT ausente o
inválido.

Todo endpoint y todo caso de respuesta contractual verificado por tests de Controller debe generar
su documentación mediante Spring REST Docs. No existe un subconjunto opcional. Cada caso
contractual distinto de la API debe quedar representado, sin duplicar snippets por cada ejecución
parametrizada ni por tests adicionales de propiedades internas cuando otro test ya documente el
mismo caso HTTP.

REST Docs no sustituye assertions. El test debe comprobar primero status, estructura y contenido
contractual, y la documentación debe derivarse de ese comportamiento comprobado. No puede
documentarse una respuesta no verificada. Snippets y descripciones deben permanecer sincronizados;
todo cambio de request, response, status o campos documentados debe actualizar tests y
documentación. Los Controller tests son la fuente principal de REST Docs; los E2E no deben
duplicarla obligatoriamente.

### 4.9. Tests de Integration externa

Los tests ubicados en `src/test/java/footballmarket/integrations` deben verificar los adaptadores
frente a proveedores externos. La Integration y su cliente o protocolo HTTP deben ser reales; el
proveedor externo debe ser un servidor controlado o una herramienta equivalente, como
`MockRestServiceServer`. Nunca deben realizar requests a Football-Data.org ni a otro proveedor real.

Deben comprobar, según corresponda, URL, path, query parameters, headers, autenticación técnica,
serialización, deserialización, mapeo externo a interno, datos incompletos, respuestas inválidas,
códigos externos, timeouts, rate limiting, política y límite de retries, y traducción de errores a
excepciones propias de Integration. Las capas superiores no deben conocer excepciones específicas
del cliente HTTP. Cuando sea parte del contrato de seguridad, deben verificar que URLs, respuestas
o excepciones no filtren información sensible.

Los tests autocontenidos que construyan manualmente su configuración y no consuman el contexto de
Spring no deben usar `@ActiveProfiles("test")`.

### 4.10. Tests de Security

Los tests transversales de autenticación y autorización deben ubicarse en
`src/test/java/footballmarket/security`, nunca en `integrations`. Deben demostrar el funcionamiento
conjunto de componentes reales de seguridad y pueden utilizar contexto Spring, Security Filter
Chain, proveedor JWT, autenticación, persistencia real y MockMvc cuando el flujo lo requiera.

Deben verificar, según corresponda, rechazo sin token, aceptación con token válido, rechazo de
firma inválida o token expirado y flujos como registro, login, JWT y acceso protegido. Puede crearse
un endpoint mínimo exclusivo del test cuando el objetivo sea la infraestructura de seguridad y no
un Controller productivo. Deben usar `@ActiveProfiles("test")` cuando dependan de la base, secreto
JWT u otras properties del perfil.

### 4.11. Tests de configuración

Los tests ubicados en `src/test/java/footballmarket/config` deben verificar configuración propia:
`@ConfigurationProperties`, binding, validación, construcción de beans, requisitos de startup,
clientes seguros, rechazo de valores inválidos y protección de secretos en representaciones como
`toString()`.

Deben usar la menor cantidad posible de contexto. Cuando alcance, deben instanciar directamente las
clases; para binding o startup deben preferir `ApplicationContextRunner` a `@SpringBootTest`. Los
tests autocontenidos que proporcionen todas sus properties no deben usar innecesariamente
`@ActiveProfiles("test")`. Si una property inválida debe impedir el startup, el test debe demostrar
el fallo del contexto.

### 4.12. Perfil `test` y base de datos

`@ActiveProfiles("test")` solo debe utilizarse cuando el test consuma `application-test.yml` o
levante un contexto que necesite su infraestructura. Debe utilizarse normalmente en Service,
Security con contexto, contexto completo, E2E y slices web que requieran beans del perfil. No debe
utilizarse en Model, tests unitarios de Orchestrator, dependencias instanciadas manualmente,
Integrations autocontenidas ni Config con properties explícitas. Estar bajo `src/test` no lo
justifica.

Todo test automatizado que requiera persistencia debe utilizar PostgreSQL real provisionado
obligatoriamente mediante Testcontainers. Esto incluye tests de Repository, tests de integración
de Service y Security, E2E y cualquier otra categoría que requiera persistencia.

Testcontainers debe levantar una instancia PostgreSQL aislada y desechable para la suite. Flyway
debe aplicar sobre ella las migraciones reales del proyecto antes de ejecutar los tests que la
utilicen. Está prohibido sustituir PostgreSQL por H2 u otra base y depender de una instancia
PostgreSQL previamente instalada, configurada en `localhost` o administrada externamente.

La suite debe provisionar por sí misma la base necesaria cuando Docker esté disponible. No debe
depender de la existencia previa de `football_market_test` ni de otra base local. Está prohibido
usar o modificar bases de desarrollo o producción. El esquema probado debe ser el generado por las
migraciones del proyecto, nunca uno creado manualmente de manera divergente.

### 4.13. Tests E2E

Los tests ubicados en `src/test/java/footballmarket/e2e` deben recorrer flujos críticos completos
desde HTTP. Deben levantar un servidor real mediante
`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`, usar el perfil
`test` y realizar requests como un consumidor externo.

El cliente HTTP de los tests E2E debe ser `org.springframework.web.client.RestClient`. No deben
utilizarse `java.net.http.HttpClient`, `MockMvc`, `TestRestTemplate`, `WebTestClient` ni otro cliente
para ejecutar el journey E2E.

Los cuerpos de request deben construirse mediante los Request DTO productivos y entregarse a
`RestClient` para su serialización. Los cuerpos de response deben deserializarse mediante
`RestClient` al Response DTO productivo correspondiente, incluido `ErrorResponseDTO` para errores
contractuales. Las respuestas sin body deben comprobarse como tales sin crear un DTO artificial.
Los tests E2E no deben construir, concatenar ni interpretar JSON manualmente, ni utilizar
`ObjectMapper` como sustituto de los DTO para interactuar con la API.

Servidor Spring, MVC, Security, Controllers, Orchestrators, Services, Repositories, JPA/Hibernate,
base de test e Integrations productivas deben ser reales. Los sistemas externos deben permanecer
controlados: la `base-url` de la Integration real debe apuntar a un servidor simulado y el tráfico
debe atravesar el cliente HTTP real. Nunca deben depender de un proveedor externo real.

Invocar directamente un Service no es E2E; usar `@SpringBootTest` por sí solo tampoco. Un test de
Controller con MockMvc y Service mockeado es de Controller. Los E2E deben proteger pocos journeys
críticos, como registro, login, JWT y acceso protegido, o sincronización y consulta de catálogo, sin
duplicar exhaustivamente combinaciones ya cubiertas en capas específicas.

### 4.14. Mocks y test doubles

Los mocks deben utilizarse solo en fronteras que el alcance del test sustituya deliberadamente, no
por comodidad. Controller usa Service y Orchestrator mock; Service usa Integration mock y
persistencia real; Orchestrator usa Services mock; Integration usa adaptador y cliente reales con
proveedor controlado; E2E usa componentes internos reales con proveedor externo controlado.

Nunca debe mockearse el componente principal bajo prueba ni una dependencia real necesaria para
demostrar el comportamiento objetivo. No debe abusarse de `verify(...)` cuando exista un resultado
observable más estable. Su uso corresponde cuando la interacción sea el comportamiento relevante,
como coordinación o rechazo antes de iniciar un caso de uso.

### 4.15. Assertions y selección de casos

Las assertions deben comprobar resultados significativos: retorno, estado final, excepción, código
y body HTTP, persistencia observable mediante la frontera permitida, invariantes y efectos
observables. No deben acoplarse innecesariamente a detalles privados. Una operación atómica debe
probar el rechazo y la ausencia de cambios parciales. Si el contrato exige campos exactos, debe
verificarse el contrato exacto y no solo una presencia parcial.

Para cada comportamiento deben considerarse, según corresponda, happy path, entradas inválidas,
ausencia de datos, límites, estado inexistente o previo, excepciones, errores externos, vacíos,
duplicados y transiciones. Debe usarse `@ParameterizedTest` cuando múltiples inputs expresen la
misma regla y expectativa; no deben repetirse tests casi idénticos si la parametrización comunica
mejor la intención.

### 4.16. Nombres, legibilidad y estructura

El nombre del test debe expresar condición y resultado esperado. El cuerpo debe conservar una
estructura conceptual Arrange, Act, Assert sin comentarios artificiales. Los helpers pueden reducir
ruido técnico, pero no ocultar la conducta principal; la claridad prevalece sobre abstracciones
excesivas. Los nombres descriptivos pueden estar en español conforme a la sección 9.

La estructura base es:

```text
src/test/java/footballmarket/
├── config/
├── controllers/
├── e2e/
├── integrations/
├── models/
├── orchestrators/
├── repositories/     # solo para comportamiento propio justificado
├── security/
└── services/
```

La ubicación debe representar la responsabilidad bajo prueba. `integrations/` significa
adaptadores externos, no cualquier prueba técnicamente considerada de integración.

Esta estructura no es una lista cerrada. Pueden agregarse paquetes auxiliares como `fixtures/`,
`builders/` y `support/` cuando exista reutilización real que lo justifique. Solo pueden contener
construcción de datos de prueba, fixtures, builders o infraestructura técnica compartida, como el
soporte común de Testcontainers. No constituyen nuevas categorías de tests.

Los helpers compartidos y privados no deben ocultar el comportamiento principal, contener
assertions ni encapsular flujos de negocio que vuelvan ilegible el test. Tampoco pueden eludir las
restricciones de una capa. En particular, un helper utilizado por un test de Service no puede
acceder directa ni indirectamente a Repository, JPA, `EntityManager`, JDBC, SQL o la base de datos.

### 4.17. Criterio para cambios futuros

Ante todo comportamiento nuevo o modificado, el agente debe identificar primero su responsabilidad
arquitectónica y agregar o modificar el test en el nivel más específico capaz de demostrarla. No
debe elegir `@SpringBootTest` automáticamente, crear E2E para reemplazar tests de Model, Service o
Controller, ni agregar tests redundantes solo para elevar cobertura.

Una validación de dominio o negocio corresponde a Model o Service según su responsabilidad; un
status HTTP a Controller; una query compleja a Repository y, si afecta el caso de uso, a Service;
un formato externo a Integration; un cambio JWT a Security y eventualmente al E2E crítico. Un
cambio transversal puede requerir pruebas en más de un nivel.

La suite debe proporcionar simultáneamente localización —un fallo de una responsabilidad debe
señalar un área específica— e integración —los componentes aislados deben probarse también en sus
conexiones relevantes—.

### 4.18. Regresiones y controles

Todo código nuevo o modificado debe contar con tests automatizados suficientes para el comportamiento
afectado. Las correcciones de defectos deben incorporar un test de regresión cuando sea razonable.

Antes de finalizar un cambio deben ejecutarse los tests y la compilación requeridos por el área
modificada. Para considerar correcto un cambio, estos controles obligatorios deben aprobarse.

SonarQube, los Quality Gates y otros controles opcionales pueden ejecutarse como herramientas
complementarias de análisis y calidad, pero sus resultados no condicionan la finalización de un
cambio aunque estén configurados en el proyecto.

No se deben modificar tests ni controles únicamente para ocultar un fallo introducido por el cambio.

Si un control falla por una causa preexistente y verificablemente ajena al cambio, debe documentarse y reportarse. Ese fallo no bloquea la finalización del cambio ni habilita ampliar su alcance para corregirlo.

## 5. Calidad y mantenibilidad

### 5.1. Simplicidad

El código debe ser claro, mantenible y simple.

Debe preferirse la solución válida más simple, evitando complejidad, dependencias y abstracciones innecesarias.

No deben introducirse patrones, interfaces o capas únicamente por seguir convenciones genéricas.

### 5.2. Responsabilidades

Cada clase y método debe mantener una responsabilidad coherente.

No se debe fragmentar código trivial sin una razón arquitectónica o de mantenibilidad.

La duplicación debe evaluarse antes de introducir una abstracción. No toda duplicación requiere ser eliminada.

### 5.3. Dependencias

No debe agregarse una dependencia externa cuando las capacidades existentes del proyecto sean suficientes.

Las nuevas dependencias deben tener una justificación técnica real.

### 5.4. Código innecesario

Debe eliminarse código muerto, obsoleto o innecesario cuando forme parte del alcance del cambio.

Los comentarios deben aportar información que no resulte evidente del código y mantenerse actualizados.

### 5.5. Refactorización

El código existente no debe modificarse sin una razón concreta.

Las refactorizaciones deben:
- aportar un beneficio técnico, arquitectónico o de mantenibilidad;
- mantenerse dentro de un alcance razonable;
- preservar el comportamiento no relacionado con el cambio.

Las necesidades hipotéticas futuras no justifican complejidad adicional.

## 6. Seguridad

### 6.1. Principios generales

La seguridad es obligatoria en todo cambio.

Toda entrada externa debe considerarse no confiable y validarse según el contrato, las reglas de seguridad y los requisitos funcionales.

No se deben deshabilitar mecanismos de seguridad para simplificar una implementación.

### 6.2. Autenticación y autorización

La autenticación debe utilizar el mecanismo definido por el proyecto.

La autorización debe verificarse de forma explícita e independiente de la autenticación.

Cuando corresponda, debe verificarse el rol, permiso o pertenencia del usuario al recurso solicitado.

### 6.3. Datos y ataques

Las consultas y operaciones que involucren datos externos deben utilizar mecanismos seguros de parametrización, binding o escaping.

No se debe concatenar directamente entrada externa en consultas u operaciones cuando exista un mecanismo seguro equivalente.

No se debe implementar criptografía propia. Deben utilizarse algoritmos y librerías estándar y seguros.

### 6.4. Secretos y datos sensibles

Los secretos, credenciales y tokens deben mantenerse fuera del código fuente y del repositorio.

No deben exponerse datos sensibles en:
- respuestas HTTP;
- logs;
- mensajes de error;
- configuraciones versionadas.

### 6.5. Comunicaciones y dependencias

Las comunicaciones externas deben utilizar mecanismos seguros, como HTTPS cuando corresponda.

Las dependencias con vulnerabilidades relevantes deben corregirse cuando sean introducidas o agravadas por el cambio.

Debe aplicarse el principio de mínimo privilegio y utilizar configuraciones seguras por defecto.

### 6.6. Testing

Los cambios que afecten seguridad deben incluir tests que verifiquen los comportamientos de seguridad relevantes.

## 7. Contratos

### 7.1. Contratos explícitos

Los componentes deben interactuar mediante contratos definidos.

Los consumidores deben depender del contrato y no de detalles internos de implementación.

Las modificaciones que alteren el comportamiento observable deben considerarse cambios de contrato.

### 7.2. Contrato HTTP

El contrato HTTP comprende, cuando corresponda:

- métodos y rutas;
- parámetros y headers;
- Request y Response DTO;
- códigos de estado;
- validaciones;
- estructura de errores.

Los DTO representan el contrato HTTP y no deben exponer directamente los Model internos.

Todo endpoint debe devolver un código de estado HTTP. El body es opcional; si una respuesta exitosa devuelve datos en el body, debe utilizar un Response DTO.

La ruta de todo endpoint expuesto por un Controller debe comenzar con el prefijo exacto `/api/`.
La ruta `/api` sin barra final no es válida, y tampoco lo son rutas que solo coincidan textualmente
con el comienzo, como `/apiv2`, `/apiary` o `/apifootball`.

### 7.3. Compatibilidad

Antes de modificar un contrato debe evaluarse su impacto sobre los consumidores existentes.

No deben introducirse cambios incompatibles de forma silenciosa.

Agregar, eliminar, modificar obligatoriedad, cambiar significado o alterar errores puede constituir un cambio incompatible.

### 7.4. Validación

La validación debe realizarse en el componente responsable:

- Validación estructural HTTP → Controller / DTO.
- Validación de negocio → Service / Model.
- Validación de contratos externos → Integration.

Las excepciones de validación estructural HTTP lanzadas por Jakarta Validation deben capturarse
desde el manejo de errores de presentación y devolverse mediante `ErrorResponseDTO`.

Las respuestas HTTP de error gestionadas por la aplicación deben utilizar `ErrorResponseDTO`,
con `timestamp`, `status`, `error`, `message` y `path`. El código HTTP de la respuesta y
`status` deben coincidir; `path` debe identificar la ruta solicitada.

Cuando existan múltiples errores de validación, `message` debe contener el primer mensaje
disponible de la validación.

Los contratos externos deben mantenerse aislados de los contratos HTTP e internos.

### 7.5. Evolución

Los contratos relevantes deben contar con tests que permitan detectar cambios incompatibles.

Todo cambio de contrato debe actualizar los elementos afectados, como implementación, tests, DTO, documentación y OpenAPI cuando corresponda.

Si no se conocen los consumidores de un contrato existente, debe tratarse como potencialmente utilizado.

## 8. Documentación

### 8.1. API HTTP

Los endpoints HTTP funcionales deben estar documentados mediante OpenAPI.

La documentación debe reflejar fielmente:
- operación y propósito;
- parámetros;
- Request y Response DTO;
- validaciones relevantes;
- códigos de respuesta contractuales;
- errores esperados;
- seguridad cuando corresponda.

Las anotaciones deben utilizarse de forma consistente con las convenciones definidas por el proyecto.

### 8.2. Postman

Todo endpoint HTTP funcional nuevo debe incorporarse como solicitud en la colección Postman
versionada del proyecto. Si cambia el contrato de un endpoint existente, su solicitud en Postman
debe actualizarse como parte del mismo cambio.

Cada solicitud debe reflejar el método, la ruta, los parámetros, los headers, la autenticación
y el body que correspondan al contrato. Los ejemplos deben ser utilizables sin incluir secretos
ni credenciales reales.

### 8.3. DTO

Los campos de los DTO que formen parte del contrato deben documentar su significado y, cuando sea relevante, ejemplos y restricciones.

La documentación debe mantenerse consistente con el comportamiento real de la API.

### 8.4. Javadoc

En código productivo, las clases públicas y los métodos `public` y `protected` deben tener Javadoc.
Los métodos `private` deben tenerlo únicamente cuando contengan lógica relevante o su propósito no
sea evidente; los métodos privados triviales quedan exceptuados.

Los constructores declarados explícitamente en código productivo deben tener Javadoc cuando su
comportamiento, parámetros, precondiciones o propósito requieran explicación. Los constructores
triviales, implícitos o generados automáticamente, por ejemplo mediante Lombok, quedan exceptuados.

En código de test, los métodos anotados con `@Test`, `@BeforeEach`, `@AfterEach` u otras anotaciones
de ciclo de vida no requieren Javadoc. Los métodos auxiliares de test solo lo requieren cuando su
propósito o comportamiento necesite una explicación adicional. Modificar únicamente el cuerpo de
un método no obliga a agregar Javadoc si el método pertenece a una categoría exceptuada.

Los métodos sobrescritos deben conservar documentación accesible mediante Javadoc; puede
utilizarse `{@inheritDoc}` cuando el contrato heredado describa completamente el comportamiento.

### 8.5. Consistencia

La documentación del proyecto debe estar escrita en español.

OpenAPI, Postman, Javadoc e implementación deben mantenerse coherentes entre sí y con la Spec correspondiente.

## 9. Idioma

### 9.1. Código

Todo código nuevo o modificado debe utilizar inglés para:
- clases;
- métodos;
- variables;
- atributos;
- paquetes;
- endpoints;
- campos JSON.

Los nombres impuestos por frameworks, librerías o sistemas externos pueden mantenerse según su contrato.

### 9.2. Tests

Los nombres descriptivos de los tests pueden estar en español.

El resto del código de los tests debe respetar las convenciones generales del proyecto.

### 9.3. Documentación

La documentación propia del proyecto debe estar en español.

Esto incluye:
- Constitution;
- Specs;
- documentación técnica;
- Javadoc;
- OpenAPI;
- mensajes de validación.

La documentación externa puede mantenerse en su idioma original.

### 9.4. Git

Los nombres de ramas, commits, Pull Requests e Issues deben estar en inglés.

### 9.5. Código existente

El código existente en español no debe modificarse únicamente para traducirlo.

Cuando un archivo existente sea modificado como parte del alcance, los elementos nuevos o modificados deben respetar las convenciones de idioma establecidas.

## 10. Cambios controlados

### 10.1. Alcance

Los cambios deben limitarse a lo necesario para:
- completar la tarea;
- cumplir la Constitution;
- mantener la coherencia de los elementos directamente afectados.

No deben realizarse refactorizaciones, mejoras u optimizaciones no relacionadas.

### 10.2. Archivos y componentes

Se pueden crear, modificar, eliminar o renombrar archivos y componentes cuando sean necesarios para el cambio.

Los elementos no relacionados deben permanecer sin modificaciones.

Antes de eliminar o renombrar elementos deben considerarse sus referencias y consumidores.

### 10.3. Comportamiento existente

Debe preservarse el comportamiento no relacionado con la tarea.

Si el cambio modifica comportamiento funcional, deben actualizarse los elementos afectados, incluyendo cuando corresponda:
- implementación;
- tests;
- contratos;
- DTO;
- OpenAPI;
- documentación;
- configuración.

### 10.4. Tests existentes

No deben modificarse tests únicamente para hacer que pasen.

Si cambia el comportamiento esperado, los tests afectados deben actualizarse para representar el nuevo contrato.

### 10.5. Problemas preexistentes

Las violaciones o fallos preexistentes y no relacionados con el cambio no requieren una corrección fuera de alcance.

No deben ocultarse, ignorarse ni utilizarse para justificar modificaciones innecesarias.

### 10.6. Sincronización entre Spec y código

La Spec y el código deben mantenerse sincronizados como parte del mismo cambio.

Si se modifica el comportamiento especificado, deben actualizarse la Spec, la implementación
y los tests afectados para que describan y verifiquen el mismo comportamiento.

Si la implementación revela una diferencia respecto de la Spec, debe resolverse la discrepancia
antes de finalizar el cambio: corregir el código o actualizar la Spec según el comportamiento
acordado. La Spec no debe actualizarse únicamente para justificar una desviación no acordada.

### 10.7. Constitution

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution durante una tarea.

Si una modificación necesaria entra en conflicto con la Constitution, debe proponerse una enmienda antes de realizar el cambio incompatible.

### 10.8. Finalización

Un cambio se considera terminado cuando los elementos directamente afectados son coherentes y los
controles aplicables definidos en la sección 4.18 se ejecutan y aprueban, salvo la excepción de
fallo preexistente definida en esa misma sección.

Cualquier modificación adicional fuera del alcance inicial debe estar justificada explícitamente.

## Technology Constraints

El proyecto utiliza:

- Java 21.
- Gradle 8.14+.
- Spring Boot 4.1.1.
- Spring Web.
- Spring Security.
- OAuth2 Resource Server.
- Spring Data JPA.
- PostgreSQL 18.x.
- JDBC.
- Flyway.
- Jakarta Validation 3.1.1.
- SpringDoc OpenAPI 3.1.0.
- Spring Boot Test.
- Spring REST Docs.
- JUnit.
- Mockito.
- AssertJ.
- Testcontainers.
- SonarQube.

Las versiones administradas por Spring Boot deben utilizarse sin sobrescribirlas salvo necesidad técnica justificada.

No deben incorporarse nuevas tecnologías o dependencias fuera de este stack sin justificación y aprobación mediante el proceso de cambios correspondiente.

Las Specs y Plans no deben repetir estas restricciones globales salvo que una funcionalidad requiera una consideración técnica específica.

**Version**: 2.3.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-20

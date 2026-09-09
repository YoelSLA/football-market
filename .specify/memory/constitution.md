<!--
Sync Impact Report:

- Version change: 1.4.0 -> 1.5.0
- List of modified principles:
  - I. Arquitectura
  - II. Convenciones
  - III. Integración y confiabilidad de fuentes externas
  - IV. Testing y control de calidad automatizado
  - V. Calidad de código y mantenibilidad
  - VI. Seguridad
  - VII. Contratos entre componentes
  - VIII. Documentación
  - IX. Idioma y convenciones lingüísticas
  - X. Cambios controlados
- Added sections:
  - Technology Constraints
  - Governance
- Modified sections:
  - Core Principles
- Removed sections:
  - None
- Follow-up TODOs: None
-->

# Football Market Constitution

## Core Principles

### 1. Arquitectura

#### 1.1. Fundamentos generales

La arquitectura debe organizarse mediante responsabilidades claramente delimitadas, componentes cohesivos y dependencias explícitas.

Las reglas de este principio se aplican a todas las áreas de la aplicación, salvo que una regla específica establezca un alcance diferente.

Cada área debe definir explícitamente sus propias capas, roles, componentes, responsabilidades y dependencias. Las relaciones entre áreas se definen de forma independiente y no modifican la arquitectura interna de cada área.

###### 1.1.1. Capa

Una **capa** es un nivel arquitectónico que agrupa responsabilidades relacionadas dentro de un área de la aplicación.

Una capa no implica una estructura física determinada de paquetes, directorios o clases.

###### 1.1.2. Rol arquitectónico

Un **rol arquitectónico** define un tipo de responsabilidad dentro de una arquitectura.

Un mismo rol puede ser implementado por múltiples componentes.

###### 1.1.3. Componente

Un **componente** es una unidad arquitectónica concreta que implementa un rol y cumple una responsabilidad determinada.

Un componente puede estar compuesto por una o varias clases.

La cantidad de clases, métodos o líneas de código no determina por sí misma la división de componentes.

###### 1.1.4. Responsabilidad

Cada componente debe implementar una única responsabilidad arquitectónica.

Una responsabilidad puede comprender múltiples comportamientos cuando estos correspondan a dicha responsabilidad.

La responsabilidad arquitectónica determina a qué componente corresponde un comportamiento.

La relación conceptual entre comportamientos no determina por sí misma que deban pertenecer al mismo componente.

###### 1.1.5. Cohesión

Los elementos de un componente deben contribuir a su responsabilidad arquitectónica.

Los elementos que no contribuyan a dicha responsabilidad no deben incorporarse al componente.

###### 1.1.6. Modularidad

Un componente debe dividirse únicamente cuando exista:

- una separación real de responsabilidades;
- una falta de cohesión; o
- una necesidad arquitectónica establecida por esta Constitución.

Una necesidad arquitectónica solo puede considerarse como tal cuando derive de una regla, responsabilidad o restricción establecida por esta Constitución.

El tamaño, la cantidad de clases, métodos o líneas de código no constituye por sí mismo una razón suficiente para dividir un componente.

Debe evitarse la fragmentación innecesaria.

###### 1.1.7. Evaluación de alternativas

Cuando existan varias alternativas arquitectónicamente válidas, el agente debe considerar su impacto sobre la cohesión, el acoplamiento, la testabilidad y la mantenibilidad.

La testabilidad y la mantenibilidad pueden orientar la elección entre alternativas arquitectónicamente válidas, pero no justifican incumplir una regla específica de esta Constitución.

###### 1.1.8. Encapsulamiento

Cada componente debe mantener encapsulados los detalles de implementación que no formen parte de su contrato.

Un componente no debe acceder directamente a los detalles internos de otro componente.

La accesibilidad técnica de un elemento no implica autorización arquitectónica para utilizarlo.

###### 1.1.9. Contrato

El **contrato** de un componente define aquello que otros componentes están arquitectónicamente autorizados a utilizar.

Un componente solo puede utilizar de otro componente aquello que forme parte de su contrato.

###### 1.1.10. Evolución

El nuevo comportamiento debe incorporarse al componente cuya responsabilidad arquitectónica corresponda.

No debe incorporarse comportamiento a un componente únicamente porque ya exista, sea cercano al comportamiento existente o resulte conveniente modificarlo.

---

#### 1.2. Arquitectura del backend

La arquitectura del backend se organiza en las siguientes capas:

- **Presentación:** Controller.
- **Aplicación:** Orchestrator y Service.
- **Dominio:** Model.
- **Persistence:** Repository.

`Integración externa` constituye un rol arquitectónico separado de las capas principales y es responsable de encapsular la comunicación con sistemas externos.

`DTO` y `Mapper` son componentes de soporte asociados a la representación y transformación de los contratos HTTP.

###### 1.2.1. Controller

`Controller` representa la frontera HTTP de la aplicación.

Es responsable de recibir solicitudes, utilizar los DTO y Mapper correspondientes, invocar `Service` u `Orchestrator` y construir las respuestas HTTP.

No debe contener lógica de negocio.

No puede acceder directamente a `Model`, `Repository` ni `Integración externa`.

###### 1.2.2. Orchestrator

`Orchestrator` coordina operaciones de aplicación cuando exista una responsabilidad de coordinación que no corresponda al `Controller` ni a un único `Service`.

La utilización de múltiples `Service` no constituye por sí misma una razón suficiente para utilizar un `Orchestrator`.

Puede utilizar un único `Service` cuando exista una verdadera responsabilidad de coordinación.

Puede utilizar `Model` para transportar, combinar o consultar información necesaria para la coordinación.

No debe implementar lógica de dominio.

No puede acceder directamente a `Repository` ni a `Integración externa`.

Un `Orchestrator` no puede depender de otro `Orchestrator`.

###### 1.2.3. Service

`Service` implementa la lógica de aplicación y coordina las operaciones necesarias para cumplir una funcionalidad.

Puede utilizar `Model`, `Repository` e `Integración externa`.

Los `Service` no pueden depender de `Controller`, `Orchestrator` ni de otros `Service`.

###### 1.2.4. Model

`Model` representa conceptos y estado del dominio y contiene el comportamiento y las reglas correspondientes a su responsabilidad.

Las reglas que puedan resolverse exclusivamente mediante información y comportamiento del dominio deben implementarse en el `Model` correspondiente.

Un `Model` puede relacionarse con otros `Model` cuando dicha relación esté justificada por el dominio.

Los `Model` deben mantener sus invariantes y encapsular el estado cuya modificación esté sujeta a reglas de dominio.

Un `Model` no puede depender de `Service`, `Repository` ni `Integración externa`.

###### 1.2.5. Repository

`Repository` pertenece a la capa de `Persistence` y proporciona acceso y gestión de los datos persistidos.

Los componentes de `Repository` deben limitarse a las operaciones relacionadas con los datos persistidos.

`Repository` no debe contener lógica de negocio.

Puede utilizar `Model` para representar los datos correspondientes.

No puede depender de `Service` ni de `DTO`.

###### 1.2.6. Integración externa

`Integración externa` encapsula la comunicación con sistemas externos.

`Service` puede utilizar componentes de integración cuando la interacción corresponda a la responsabilidad que debe resolver.

`Controller`, `Orchestrator`, `Model` y `Repository` no pueden comunicarse directamente con sistemas externos mediante componentes de integración.

La existencia de una integración externa no constituye por sí misma una razón para utilizar un `Orchestrator`.

---

#### 1.3. DTO y Mapper

###### 1.3.1. DTO

`DTO` representa los contratos HTTP de entrada y salida y constituye una estructura de soporte de la capa de presentación.

`Service`, `Model` y `Repository` no deben depender de `DTO`.

###### 1.3.2. Mapper

`Mapper` es un componente de soporte destinado exclusivamente a transformar entre DTO y Model.

Las transformaciones permitidas son:

```text
DTO → Model
Model → DTO
```

Puede realizar las transformaciones técnicas necesarias para dichas conversiones.

No debe contener lógica de negocio, cálculos de negocio, validaciones de negocio ni decisiones semánticas.

No puede depender de `Service` ni de `Repository`.

---

#### 1.4. Dependencias arquitectónicas

Las dependencias directas permitidas entre los roles principales del backend son:

```text
Controller → Service
Controller → Orchestrator

Orchestrator → Service
Orchestrator → Model

Service → Model
Service → Repository
Service → Integración externa

Repository → Model
```

Las relaciones de `DTO` y `Mapper` se limitan a las responsabilidades establecidas en la sección 1.3.

Toda dependencia directa que no esté expresamente permitida queda prohibida, salvo que esta Constitución establezca una excepción explícita.

La matriz de dependencias es exhaustiva y no constituye una lista de ejemplos.

###### 1.4.1. Dependencias transitivas

Una dependencia indirecta no autoriza una dependencia directa.

Por ejemplo:

```text
Controller → Service → Repository
```

no autoriza:

```text
Controller → Repository
```

###### 1.4.2. Dirección de las dependencias

Las dependencias deben respetar la dirección establecida por la arquitectura.

Una dependencia en sentido inverso constituye una violación arquitectónica.

###### 1.4.3. Dependencias circulares

No deben existir dependencias circulares entre componentes.

###### 1.4.4. Acceso mediante intermediarios

No debe utilizarse un componente intermedio para eludir una restricción arquitectónica.

Una operación no permitida para un componente continúa siendo no permitida aunque pueda alcanzarse indirectamente mediante otro componente.

---

#### 1.5. Reglas de dominio y aplicación

Las reglas de negocio deben implementarse en el componente correspondiente según su naturaleza.

Las reglas que puedan resolverse exclusivamente con información y comportamiento del dominio corresponden al `Model`.

Las reglas que requieran `Repository`, `Integración externa` u otras capacidades propias de la aplicación corresponden a `Service`.

Una regla que involucre múltiples `Model` puede permanecer en el dominio cuando pueda resolverse completamente mediante lógica de dominio.

El `Service` debe obtener mediante `Repository` o `Integración externa` la información que el dominio necesite cuando esta no se encuentre disponible en los `Model`.

---

#### 1.6. Estructuras existentes y evolución arquitectónica

La existencia de paquetes, clases, interfaces, clases `Impl`, nombres o estructuras previas no obliga a conservarlas cuando contradigan esta Constitución.

El agente puede realizar las refactorizaciones necesarias para cumplir la arquitectura.

Los nombres existentes no determinan por sí mismos el rol arquitectónico de un componente.

La estructura física de paquetes, directorios y clases no determina por sí misma la arquitectura.

La arquitectura definida por esta Constitución prevalece sobre las estructuras existentes, salvo que una regla específica establezca una excepción.

---

#### 1.7. Nuevas áreas

Cada nueva área de la aplicación debe definir explícitamente su propia arquitectura, incluyendo las capas, roles, componentes, responsabilidades y dependencias que correspondan.

Las reglas internas de una arquitectura no deben extrapolarse automáticamente a otra área.

Las relaciones entre áreas deben establecerse explícitamente.

---

#### 1.8. Resolución arquitectónica

El agente debe respetar las responsabilidades, contratos, límites y dependencias establecidos por esta Constitución.

Las dependencias no definidas como permitidas no pueden inferirse como válidas por conveniencia técnica.

Una excepción arquitectónica debe estar expresamente establecida.

Cuando una situación no pueda resolverse de forma determinística a partir de las reglas existentes, el agente no debe inventar una nueva regla arquitectónica.

Las reglas arquitectónicas prevalecen sobre las decisiones de implementación técnica.

### 2. Convenciones

#### 2.1. Principios de implementación

Las convenciones establecen cómo deben implementarse técnicamente las decisiones arquitectónicas definidas en el Principio 1.

Una convención no puede modificar, ampliar ni contradecir una regla arquitectónica.

Cuando una convención entre en conflicto con una regla arquitectónica, prevalece la regla arquitectónica.

#### 2.2. Estructura física

La organización de paquetes, directorios, clases y archivos debe seguir las convenciones definidas para el proyecto.

La estructura física debe reflejar la arquitectura cuando las convenciones correspondientes así lo establezcan, pero no debe utilizarse para modificar la clasificación arquitectónica de los componentes.

#### 2.3. DTO

Los DTO estructurados utilizados para entrada y salida HTTP deben implementarse como `record`.

Los Request DTO pueden utilizar mecanismos de validación de Jakarta Validation cuando las restricciones correspondan a requisitos funcionales o al contrato HTTP.

Los mensajes de validación propios de la aplicación deben estar redactados en español.

La obligatoriedad y nulabilidad de los campos deben representar correctamente el contrato HTTP.

#### 2.4. Model

Los mecanismos utilizados para construir y modificar `Model` deben garantizar el cumplimiento de sus invariantes.

Los estados obligatorios para la validez del `Model` deben establecerse mediante mecanismos de construcción apropiados.

Los `Model` no deben utilizar `Builder`.

Los mecanismos de acceso y modificación del estado deben preservar el encapsulamiento definido por la arquitectura.

Cuando corresponda, puede utilizarse `@Getter` para exponer el estado.

No deben utilizarse `@Setter` indiscriminadamente en los `Model`.

Las anotaciones de Persistence necesarias para representar el `Model` pueden utilizarse sin modificar su responsabilidad arquitectónica de dominio.

#### 2.5. Mapper

Los `Mapper` deben implementarse como clases `final`, sin estado mutable, con constructor privado explícito y métodos `static`.

No deben utilizarse instancias de `Mapper`.

Cuando un `Mapper` reciba `null` como entrada, debe lanzar la excepción específica de mapeo definida por el proyecto.

Un `Mapper` no debe convertir silenciosamente `null` en valores por defecto como `""`, `[]`, `0`, `false` u otros valores.

Si la excepción específica de mapeo necesaria para cumplir esta regla no existe, puede crearse respetando las convenciones de excepciones del proyecto.

#### 2.6. Service

La existencia de una interfaz Java para un `Service` no es obligatoria.

La utilización de una interfaz debe responder a una necesidad técnica o arquitectónica real.

La existencia previa de una estructura `Service`/`ServiceImpl` no obliga a crear interfaces para otros `Service`.

#### 2.7. Implementación y framework

Las decisiones de implementación deben respetar las tecnologías y frameworks establecidos por el proyecto.

Las anotaciones, clases base, interfaces de framework y mecanismos técnicos utilizados no deben utilizarse para alterar las responsabilidades o dependencias definidas por el Principio 1.

Las decisiones técnicas deben mantenerse subordinadas a la arquitectura.

### 3. Integración y confiabilidad de fuentes externas

#### 3.1. Encapsulamiento y separación respecto del dominio

Toda comunicación con un sistema o fuente externa debe realizarse mediante un componente de integración específico.

###### 3.1.1. Encapsulamiento

El componente de integración debe encapsular la construcción de solicitudes, la comunicación con el sistema externo, la interpretación de sus respuestas, la transformación de datos externos a representaciones internas, el tratamiento de errores propios de la integración y la configuración necesaria para su funcionamiento.

Los detalles técnicos del proveedor, protocolo, cliente o mecanismo de comunicación no deben exponerse innecesariamente al resto de la aplicación.

###### 3.1.2. Separación respecto del dominio

El dominio no debe depender directamente de detalles técnicos de las integraciones externas.

Los Model no deben contener lógica de comunicación HTTP, clientes externos ni detalles propios de protocolos, proveedores o formatos externos.

Cuando sea necesario utilizar información externa dentro de la aplicación, esta debe transformarse previamente a una representación compatible con las responsabilidades internas correspondientes.

#### 3.2. Validación e información externa

Toda respuesta externa debe validarse antes de ser utilizada, al menos en los aspectos necesarios para interpretarla correctamente.

Debe distinguirse entre respuestas válidas, vacías, incompletas e inválidas cuando dichas condiciones tengan significados diferentes para la operación.

Una respuesta técnicamente válida no implica necesariamente que sus datos sean funcionalmente válidos para la aplicación.

###### 3.2.1. Información insuficiente o inválida

Si una fuente externa no proporciona información suficiente o válida para cumplir correctamente una operación, debe aplicarse el comportamiento definido por los requisitos.

No se deben asumir valores, inventar información ni simular el éxito de una operación cuando la información externa necesaria no se encuentra disponible o no es válida.

La aplicación tampoco debe modificar silenciosamente su comportamiento funcional para adaptarse a un fallo o respuesta inesperada del proveedor.

Cuando los requisitos no definan el comportamiento correspondiente ante una condición externa relevante, debe solicitarse una definición antes de establecer una decisión funcional.

#### 3.3. Errores, tiempos de espera y reintentos

Los errores originados en sistemas o comunicaciones externas deben diferenciarse de los errores propios del dominio.

Deben contemplarse, según corresponda, fallos de conexión, timeouts, respuestas HTTP no exitosas, respuestas con formato inválido, datos incompatibles, indisponibilidad del sistema externo y errores de autenticación o autorización.

Un error técnico de integración no debe convertirse arbitrariamente en una excepción de dominio.

###### 3.3.1. Timeouts

Toda comunicación externa debe disponer de límites de tiempo cuando el mecanismo utilizado lo permita. No debe admitirse una espera indefinida.

Los tiempos establecidos deben ser compatibles con los requisitos de la operación y con las características de la fuente externa.

###### 3.3.2. Reintentos

No deben realizarse reintentos automáticos por defecto.

Un reintento solo debe implementarse cuando esté explícitamente justificado por la operación, sea seguro repetirla y se encuentre limitado.

Las operaciones que puedan producir efectos duplicados no deben reintentarse sin un mecanismo que garantice un comportamiento seguro.

Los reintentos no deben utilizarse para ocultar fallos persistentes de una integración.

#### 3.4. Disponibilidad, degradación y caché

La indisponibilidad de una fuente externa debe tratarse explícitamente.

Cuando una operación dependa obligatoriamente de dicha fuente, debe aplicarse el resultado o error definido por los requisitos.

No debe simularse el éxito de una operación ni sustituirse información faltante por valores arbitrarios.

La degradación funcional solo puede utilizarse cuando esté contemplada por los requisitos.

###### 3.4.1. Datos previamente obtenidos

Si los requisitos permiten utilizar información previamente obtenida, esta debe conservar criterios explícitos de validez y frescura cuando resulten relevantes.

###### 3.4.2. Caché

La caché puede utilizarse cuando exista una justificación basada en los requisitos funcionales o en una necesidad técnica.

Cuando se utilice información almacenada previamente, deben considerarse su frescura y consistencia con la fuente externa cuando estas puedan afectar la corrección de la operación.

Los datos almacenados no deben utilizarse indefinidamente cuando su antigüedad pueda producir resultados incorrectos.

La caché no debe utilizarse como mecanismo permanente para ocultar una integración defectuosa ni para inventar información ante su indisponibilidad.

#### 3.5. Contratos y transformación de datos

Las integraciones deben respetar los contratos de las fuentes externas con las que se comunican.

Cuando el contrato externo sea conocido, deben modelarse explícitamente los datos necesarios para la comunicación.

###### 3.5.1. Cambios e incompatibilidades

Los cambios o incompatibilidades del contrato externo que afecten a la aplicación deben resolverse dentro de la integración mediante una adaptación técnicamente válida, un rechazo de la respuesta u otro comportamiento definido por los requisitos.

No deben realizarse conversiones arbitrarias en Model, Service, Controller u otros componentes para ocultar incompatibilidades del proveedor.

Los datos externos que no sean necesarios para la aplicación no deben incorporarse a sus representaciones internas sin una justificación.

###### 3.5.2. Transformación y aislamiento

Las estructuras y convenciones propias de un proveedor externo no deben propagarse innecesariamente por la aplicación.

Cuando corresponda, los datos externos deben transformarse a representaciones internas antes de ser utilizados por Model, Service, Orchestrator, Controller u otros componentes.

Los contratos externos y los contratos internos de la aplicación deben mantenerse independientes cuando las responsabilidades arquitectónicas lo requieran.

#### 3.6. Autenticación, credenciales y logs

Las credenciales y demás datos de autenticación deben gestionarse mediante los mecanismos de configuración y seguridad establecidos por el proyecto.

No deben almacenarse secretos directamente en el código fuente.

La configuración de autenticación debe mantenerse separada de la lógica funcional de la integración.

###### 3.6.1. Protección de credenciales

Las credenciales, tokens y secretos no deben exponerse en logs, respuestas HTTP ni otros medios no destinados a su protección.

###### 3.6.2. Logs de integración

Los errores de integración que puedan afectar la operación deben registrarse con el nivel de detalle apropiado para permitir identificar el tipo de fallo y su contexto técnico.

Los logs no deben contener credenciales, tokens, secretos ni información sensible innecesaria.

No deben registrarse indiscriminadamente respuestas externas completas cuando su contenido no sea necesario para diagnosticar el problema.

#### 3.7. Aislamiento de fallos y dependencias

El fallo de una integración externa no debe producir efectos secundarios innecesarios sobre componentes u operaciones que no dependan de ella.

Cuando una operación utilice múltiples fuentes externas, el tratamiento de fallos parciales debe responder a los requisitos funcionales y evitar resultados inconsistentes cuando sea posible.

No debe incorporarse una dependencia externa únicamente para simplificar una implementación que pueda resolverse razonablemente con las capacidades existentes del proyecto.

Cuando una integración requiera una librería, cliente o dependencia externa, esta debe respetar las reglas de arquitectura, seguridad y calidad del proyecto.

#### 3.8. Criterio general de las integraciones externas

Las integraciones externas constituyen límites de confiabilidad de la aplicación.

Por lo tanto, deben aislar sus detalles técnicos, validar la información recibida y controlar los fallos previsibles sin permitir que comportamientos inesperados de una fuente externa se conviertan silenciosamente en estados o resultados incorrectos de la aplicación.

Las decisiones relacionadas con disponibilidad, reintentos, caché y degradación deben estar justificadas por los requisitos y no por supuestos arbitrarios.

### 4. Testing y control de calidad automatizado

#### 4.1. Testing obligatorio y selección de pruebas

Todo código nuevo o modificado debe contar con pruebas automatizadas suficientes para verificar los comportamientos afectados por el cambio.
Las pruebas deben contemplar el comportamiento esperado y los casos de error, validación, límites o estados inválidos que resulten relevantes.
Un cambio que modifica un comportamiento sin incorporar o actualizar las pruebas correspondientes se considera incompleto.
Las pruebas deben seleccionarse de acuerdo con el componente y las responsabilidades cuyo comportamiento se modifica.

###### 4.1.1. Tipos de pruebas

Se contemplan, entre otras, las siguientes categorías:

- **Pruebas de Model:** verifican reglas de dominio, invariantes y comportamiento del Model.
- **Pruebas de Service:** verifican lógica de aplicación, uso de Persistence, integraciones y coordinación propia del Service.
- **Pruebas de Orchestrator:** verifican la coordinación entre Services, integraciones y operaciones de aplicación.
- **Pruebas de Controller:** verifican el contrato HTTP, validaciones, respuestas y tratamiento de errores.
- **Pruebas de Mapper:** verifican las transformaciones entre DTO y Model.
- **Pruebas de Persistence:** verifican el comportamiento dependiente de la infraestructura de persistencia.
- **Pruebas de integración:** verifican interacciones reales entre componentes, infraestructura o sistemas externos cuando estas sean parte del comportamiento.
- **Pruebas end-to-end:** verifican flujos completos cuando el proyecto o el cambio requieran validar el comportamiento integral de la aplicación.

No es obligatorio ejecutar todas las categorías ante cada cambio. Deben utilizarse aquellas correspondientes a los comportamientos y responsabilidades afectados.

#### 4.2. Pruebas unitarias e integración

Las pruebas unitarias deben aislar el comportamiento que se pretende verificar de las dependencias externas que no formen parte de dicho comportamiento.
Cuando corresponda, las dependencias pueden sustituirse por dobles de prueba u otros mecanismos equivalentes que permitan controlar sus respuestas.
No debe dependerse innecesariamente de infraestructura externa, bases de datos, comunicaciones HTTP, tiempo real u otros recursos externos para verificar un comportamiento que pueda probarse correctamente de forma aislada.
Las pruebas de integración deben utilizarse cuando el comportamiento dependa de la interacción real entre componentes, infraestructura o sistemas externos.

###### 4.2.1. Criterio de selección

Cuando un comportamiento pueda verificarse correctamente mediante una prueba unitaria aislada, no es necesario convertir esa verificación en una prueba de integración.
Las pruebas de integración no sustituyen a las pruebas unitarias cuando estas permiten verificar correctamente el comportamiento de forma aislada.

###### 4.2.2. Alcance de las pruebas de integración

Las pruebas de integración deben permitir verificar, según corresponda:

- Persistencia real.
- Serialización.
- Integración entre componentes.
- Contratos.
- Configuración.
- Comunicación con sistemas externos.

#### 4.3. Cobertura de comportamiento

Las pruebas deben cubrir, según corresponda al comportamiento modificado:

- Casos exitosos.
- Errores relevantes.
- Validaciones aplicables.
- Límites significativos.
- Estados inválidos.
- Excepciones esperadas.
- Transformaciones relevantes.
- Reglas de negocio modificadas.

La cobertura de código constituye una métrica de calidad, pero no demuestra por sí misma que el comportamiento requerido haya sido correctamente verificado.
La cobertura de comportamiento debe evaluarse en función de los escenarios relevantes, independientemente del porcentaje de cobertura de código obtenido.

#### 4.4. Determinismo, independencia y organización

Las pruebas deben producir resultados deterministas bajo las mismas condiciones.
No deben depender innecesariamente de orden de ejecución, estado compartido, datos modificados por otras pruebas, servicios externos variables, tiempo real, aleatoriedad no controlada o configuración ambiental no controlada.
Cada prueba debe ser independiente y disponer de datos y recursos controlables.
Los recursos utilizados durante una prueba deben limpiarse o controlarse para evitar afectar a otras pruebas.
Los nombres de las pruebas deben describir el comportamiento verificado, sus condiciones relevantes y el resultado esperado.
Las pruebas deben organizarse de manera que exista una correspondencia clara con el componente y comportamiento que verifican.

#### 4.5. Pruebas por componente

###### 4.5.1. Controller

Las pruebas de Controller deben verificar el contrato HTTP correspondiente, incluyendo cuando resulte aplicable:

- Método HTTP.
- Ruta.
- Parámetros.
- Headers o cookies relevantes.
- Cuerpo de la solicitud.
- Validaciones.
- Código de estado.
- Cuerpo de la respuesta.
- Estructura de los DTO.
- Traducción de excepciones a respuestas HTTP.

No deben utilizarse para repetir la verificación de lógica interna de Model, Service u otros componentes que ya puedan verificarse de forma aislada.

###### 4.5.2. Service

Las pruebas de Service deben verificar la lógica de aplicación correspondiente, incluyendo cuando resulte aplicable:

- Utilización de Persistence.
- Utilización de integraciones externas.
- Interpretación y utilización de los datos obtenidos.
- Reglas de coordinación propias del Service.
- Propagación o transformación de excepciones.
- Tratamiento de respuestas vacías, inválidas o excepcionales.

No deben limitarse a verificar únicamente que una dependencia fue invocada cuando el comportamiento funcional también dependa del resultado de dicha invocación.

###### 4.5.3. Orchestrator

Las pruebas de Orchestrator deben verificar la coordinación de la operación de aplicación, incluyendo cuando resulte relevante:

- Orden de las operaciones.
- Utilización de los resultados.
- Condiciones de coordinación.
- Manejo de errores.
- Combinación o encadenamiento de operaciones.
- Resultado final de la coordinación.

No deben duplicar las pruebas de la lógica interna de los Services o integraciones que el Orchestrator coordina.

###### 4.5.4. Model

Las pruebas de Model deben verificar directamente las reglas de dominio e invariantes, incluyendo cuando corresponda:

- Construcción válida.
- Rechazo de estados inválidos.
- Operaciones permitidas y prohibidas.
- Transiciones de estado.
- Excepciones de dominio.
- Comportamiento propio del dominio.

Las reglas de dominio no deben considerarse verificadas únicamente por pruebas de Controller o Service.

###### 4.5.5. Mapper

Las pruebas de Mapper deben verificar las transformaciones correspondientes entre DTO y Model, incluyendo:

- DTO → Model.
- Model → DTO.
- Transformaciones de colecciones.
- Tratamiento de valores `null`.
- Errores de mapeo.

No deben utilizarse para incorporar o verificar lógica de negocio que no corresponda al Mapper.

###### 4.5.6. Persistence

Las pruebas de Persistence deben ser de integración cuando el comportamiento dependa de la infraestructura de persistencia.
Deben verificar, según corresponda:

- Consultas.
- Operaciones de persistencia.
- Mapeos.
- Comportamiento dependiente de la infraestructura.

No debe simularse mediante una prueba unitaria la infraestructura cuyo comportamiento real constituye precisamente lo que se pretende verificar.

###### 4.5.7. Integraciones externas

Las pruebas de integraciones externas deben verificar los comportamientos propios de la integración, incluyendo cuando corresponda:

- Construcción de solicitudes.
- Transformación de datos.
- Interpretación de respuestas.
- Respuestas vacías, incompletas o inválidas.
- Errores externos.
- Timeouts.
- Fallos de conexión.
- Respuestas inesperadas.

Cuando no resulte apropiado utilizar el sistema externo real, deben emplearse dobles de prueba u otros mecanismos que permitan verificar el comportamiento de la integración de forma controlada.

#### 4.6. Calidad automatizada

Todo cambio debe pasar los controles automatizados definidos y aplicables al proyecto.
Estos controles pueden incluir, entre otros:

- Compilación.
- Pruebas automatizadas.
- Análisis estático.
- Cobertura.
- Detección de vulnerabilidades.
- Formateo.
- Linting.
- Quality Gates.

Solo son obligatorios los controles y Quality Gates efectivamente definidos por el proyecto para el cambio correspondiente.

###### 4.6.1. Formateo y análisis estático

El código debe cumplir las reglas de formateo y análisis estático configuradas en el proyecto.
No deben incorporarse excepciones, supresiones ni modificaciones de configuración únicamente para evitar un incumplimiento.
Las excepciones o supresiones solo pueden utilizarse cuando exista una justificación válida y sean compatibles con las reglas del proyecto.

###### 4.6.2. Quality Gates

Los Quality Gates definidos por el proyecto son obligatorios cuando resulten aplicables.
El incumplimiento de un Quality Gate aplicable implica que el cambio no se considera terminado.
No deben deshabilitarse, relajarse ni modificarse Quality Gates para permitir la finalización de un cambio, salvo que la modificación del propio Quality Gate forme parte explícita de un cambio requerido.

#### 4.7. Fallos de controles y regresión

Cuando un control automatizado falle, debe determinarse si el fallo fue provocado por el cambio.
Si el fallo fue introducido por el cambio, debe corregirse antes de considerar terminado el trabajo.
Si se demuestra que el fallo es preexistente y no está relacionado con el cambio, no deben realizarse modificaciones en el código únicamente para ocultar o solucionar dicho fallo dentro del alcance del cambio actual.

###### 4.7.1. Regresión

Cuando se corrija un defecto, debe incorporarse una prueba de regresión que reproduzca el comportamiento defectuoso cuando resulte técnicamente posible.
La prueba de regresión debe conservarse para evitar que el defecto vuelva a introducirse.
Cuando una modificación de comportamiento afecte pruebas existentes, estas deben revisarse y actualizarse de acuerdo con el comportamiento requerido.

#### 4.8. Finalización del cambio

Antes de finalizar un cambio de código deben ejecutarse todos los controles automatizados que resulten aplicables.
Como mínimo, cuando estén definidos por el proyecto, deben ejecutarse la compilación, las pruebas, el análisis estático, el formateo y los Quality Gates correspondientes.
El cambio no debe considerarse terminado mientras alguno de los controles aplicables falle por una causa introducida por el propio cambio.

### 5. Calidad de código y mantenibilidad

#### 5.1. Calidad y simplicidad

El código debe ser claro, legible, mantenible y coherente con la arquitectura definida por esta Constitución.

Las soluciones deben ser tan simples como sea posible sin incumplir los requisitos funcionales, arquitectónicos, de seguridad, calidad o mantenibilidad.

Ante soluciones técnicamente válidas, debe preferirse aquella que presente:

- Menor complejidad.
- Menos dependencias innecesarias.
- Mayor facilidad de comprensión.
- Mayor facilidad de prueba.
- Mayor facilidad de mantenimiento.
- Menor cantidad de código, siempre que esto no perjudique los criterios anteriores.

La simplicidad no debe obtenerse incumpliendo ninguna otra regla de esta Constitución.

#### 5.2. Sobreingeniería, abstracciones y patrones

No deben introducirse abstracciones, patrones, clases, métodos, configuraciones o estructuras que no sean necesarios para cumplir los requisitos o las reglas arquitectónicas.

###### 5.2.1. Abstracciones

Toda abstracción debe tener una responsabilidad claramente definida y una justificación concreta, como separación de responsabilidades, reutilización real, aislamiento necesario, testabilidad o cumplimiento de una restricción arquitectónica.

La posibilidad hipotética de reutilización, extensión o modificación futura no constituye por sí misma una justificación suficiente.

Las abstracciones existentes deben evaluarse antes de introducir nuevas abstracciones equivalentes.

###### 5.2.2. Patrones de diseño

Los patrones de diseño pueden utilizarse cuando resuelvan un problema real del sistema y mejoren su estructura, mantenibilidad o claridad.

No deben utilizarse únicamente para demostrar su utilización ni para adaptar artificialmente el código a una estructura teórica.

La ausencia de un patrón no constituye un defecto cuando una solución más simple resuelve correctamente el problema.

#### 5.3. Duplicación y reutilización

La duplicación de código debe evaluarse según el contexto antes de eliminarse mediante una abstracción.

No toda duplicación requiere una abstracción.

La reutilización debe introducirse cuando exista una responsabilidad común real y suficientemente estable.

Cuando eliminar una duplicación implique introducir una abstracción más compleja que el código duplicado, debe preferirse mantener la duplicación si esta resulta más clara y mantenible.

#### 5.4. Métodos, clases e interfaces

Los métodos deben tener una responsabilidad claramente identificable y las clases deben representar una responsabilidad coherente.

No deben crearse métodos o clases únicamente para fragmentar código trivial, reducir la cantidad de líneas de un método o cumplir una convención sin aportar valor arquitectónico, de mantenimiento, reutilización o claridad.

Una clase o método no debe concentrar responsabilidades pertenecientes a componentes diferentes.

###### 5.4.1. Interfaces

Las interfaces deben utilizarse cuando representen un contrato necesario para la arquitectura, permitan desacoplar componentes cuando dicho desacoplamiento sea necesario o respondan a una necesidad real de sustitución de implementaciones.

No deben crearse interfaces únicamente para cumplir una convención genérica de programar contra interfaces.

La existencia de una interfaz para un Service no es obligatoria salvo que otra regla de esta Constitución o una necesidad concreta del sistema lo requiera.

#### 5.5. Dependencias externas

No deben incorporarse dependencias externas cuando la funcionalidad pueda resolverse razonablemente mediante las capacidades ya disponibles en el proyecto o en las librerías existentes.

Antes de incorporar una dependencia debe verificarse que exista una necesidad real que justifique su utilización.

Toda dependencia incorporada debe respetar las reglas de seguridad, arquitectura, mantenimiento y licenciamiento aplicables al proyecto.

Las dependencias específicas de integraciones externas deben respetar además las reglas establecidas en el Principio 3.

#### 5.6. Código innecesario y comentarios

No debe permanecer código que carezca de una finalidad vigente.

Esto incluye, entre otros:

- Métodos o clases sin uso.
- Variables o imports innecesarios.
- Dependencias o configuraciones sin finalidad vigente.
- Código muerto.
- Código comentado que ya no forme parte de la implementación.
- Comentarios obsoletos.
- Abstracciones que hayan dejado de tener una responsabilidad real.

Cuando un cambio vuelva innecesario un fragmento de código, este debe eliminarse en lugar de conservarse por precaución.

No debe mantenerse código únicamente por una posible reutilización futura cuando no exista actualmente una referencia o requisito que lo justifique.

La eliminación de código innecesario debe realizarse cuando sea identificada y se encuentre dentro del alcance seguro del cambio.

Los comentarios deben utilizarse para explicar decisiones, restricciones o comportamientos que no resulten evidentes a partir del código.

No deben utilizarse para describir literalmente operaciones evidentes ni para justificar código innecesariamente complejo cuando este pueda simplificarse.

Los comentarios deben mantenerse actualizados respecto del comportamiento real del código.

#### 5.7. Código existente y refactorización

La búsqueda de una solución simple no implica modificar innecesariamente código existente.

Cuando una implementación cumpla correctamente su responsabilidad y no contradiga esta Constitución, debe evitarse su modificación sin una razón concreta.

###### 5.7.1. Refactorización

Las refactorizaciones deben realizarse cuando mejoren de forma concreta la calidad, mantenibilidad, claridad o cumplimiento arquitectónico del código.

Puede modificarse código relacionado aunque no sea estrictamente necesario para implementar la funcionalidad cuando la refactorización esté concretamente justificada, tenga un alcance razonable y no modifique comportamiento ajeno al objetivo del cambio.

Una refactorización no debe modificar comportamiento funcional salvo que dicho cambio forme parte explícita del objetivo.

Las refactorizaciones extensas que no sean necesarias deben evitarse.

Cuando una refactorización sea necesaria, debe realizarse de forma verificable mediante los mecanismos de testing y control de calidad definidos por esta Constitución.

#### 5.8. Complejidad y justificación

No debe introducirse complejidad que no sea requerida por el problema que se está resolviendo.

Debe evitarse especialmente:

- Encadenamiento innecesario de abstracciones.
- Jerarquías de clases innecesarias.
- Indirecciones sin propósito.
- Configuración innecesaria.
- Patrones aplicados sin necesidad.
- Conversiones innecesarias entre múltiples representaciones.
- Flujos de ejecución más complejos que los requeridos por la funcionalidad.

Cuando exista una decisión entre una solución simple y una más compleja, la solución compleja debe contar con una justificación concreta relacionada con una necesidad del sistema.

La justificación puede corresponder, entre otros motivos, a:

- Requisito funcional.
- Requisito arquitectónico.
- Seguridad.
- Mantenibilidad.
- Reutilización real.
- Aislamiento necesario.
- Testabilidad.
- Integración con infraestructura o sistemas externos.
- Restricción técnica.

La posibilidad hipotética de una necesidad futura no constituye una justificación suficiente.

#### 5.9. Relación con la arquitectura

La simplicidad no autoriza a incumplir las responsabilidades y dependencias definidas por los Principios 1 y 2.

Una solución aparentemente más simple no debe utilizarse si concentra responsabilidades, introduce dependencias prohibidas o viola cualquier otra regla de esta Constitución.

La solución preferida debe ser la más simple que cumpla simultáneamente los requisitos funcionales y las restricciones aplicables.

#### 5.10. Criterio general

El código debe resolver el problema actual de manera clara y mantenible, evitando anticipar problemas que todavía no existen.

Debe evitarse tanto la complejidad innecesaria como la simplificación que degrade la arquitectura, seguridad, calidad o mantenibilidad.

La calidad del código debe evaluarse por su capacidad de expresar correctamente la responsabilidad que implementa con la menor complejidad razonable.

### 6. Seguridad

#### 6.1. Seguridad como requisito obligatorio

La seguridad es un requisito obligatorio de toda modificación del sistema.

Ningún cambio funcional, arquitectónico o técnico debe introducir deliberadamente una vulnerabilidad, debilitar un mecanismo de seguridad existente o permitir un acceso no autorizado por los requisitos del sistema.

La implementación debe respetar las medidas de seguridad definidas por el proyecto, el framework, las librerías utilizadas y los requisitos funcionales aplicables.

#### 6.2. Autenticación, autorización y control de acceso

Las operaciones que requieran identificar al consumidor deben utilizar el mecanismo de autenticación establecido por el proyecto.

Las operaciones que requieran permisos deben verificar que el consumidor autenticado tenga autorización suficiente para ejecutar la operación sobre el recurso correspondiente.

###### 6.2.1. Autorización

La autenticación no implica autorización automática.

Las comprobaciones de autorización no deben depender únicamente de datos proporcionados por el consumidor.

###### 6.2.2. Acceso a recursos

El acceso a recursos debe restringirse de acuerdo con las reglas de autorización definidas por el sistema.

No debe permitirse acceder, modificar o eliminar un recurso únicamente porque el consumidor conozca o proporcione su identificador.

Cuando el acceso dependa de pertenencia, propiedad, rol, permiso u otra condición del consumidor, dicha condición debe verificarse explícitamente.

La implementación arquitectónica concreta de estos mecanismos no se define en este principio salvo cuando otra regla de la Constitución lo establezca.

#### 6.3. Validación y tratamiento de entradas

Toda entrada proveniente de un consumidor externo debe considerarse no confiable.

Los datos recibidos mediante HTTP, parámetros, headers, cookies, cuerpos de solicitudes u otras interfaces externas deben validarse y procesarse de acuerdo con su contrato y los requisitos de seguridad aplicables.

###### 6.3.1. Separación de validaciones

La validación estructural no sustituye las validaciones de seguridad, autorización ni reglas de negocio.

Un DTO correctamente validado estructuralmente no implica que sus datos estén autorizados, sean seguros o cumplan las reglas de negocio.

Cada tipo de validación debe realizarse en el componente correspondiente a su responsabilidad.

###### 6.3.2. Prevención de inyecciones

Los datos proporcionados por consumidores externos no deben incorporarse directamente en consultas, comandos u otras operaciones susceptibles de inyección.

Deben utilizarse los mecanismos de parametrización, binding, escaping o abstracción proporcionados por las tecnologías utilizadas cuando sean aplicables.

No debe construirse dinámicamente una consulta o comando mediante concatenación directa de datos externos cuando exista un mecanismo seguro para parametrizarlos.

#### 6.4. Secretos y datos sensibles

Los secretos utilizados por la aplicación deben mantenerse fuera del código fuente y obtenerse mediante los mecanismos de configuración y gestión de secretos establecidos por el proyecto.

Se consideran secretos, entre otros:

- Contraseñas.
- Tokens.
- API keys.
- Claves privadas.
- Credenciales de bases de datos.
- Credenciales de sistemas externos.
- Secretos utilizados para firmar o cifrar información.

No deben incorporarse secretos reales al repositorio.

La información sensible debe tratarse de acuerdo con los requisitos de seguridad, privacidad y normativa aplicable al proyecto.

No deben exponerse secretos ni datos sensibles innecesariamente mediante respuestas HTTP, logs, mensajes de error, excepciones, DTO, código fuente o archivos de configuración versionados.

La definición de información sensible debe determinarse según los requisitos de seguridad, privacidad y normativa aplicable.

#### 6.5. Errores, excepciones y logs

Los mensajes de error destinados al consumidor no deben revelar información interna innecesaria.

No deben exponerse, entre otros:

- Stack traces.
- Rutas internas.
- Credenciales.
- Tokens.
- Consultas.
- Detalles de infraestructura.
- Información de configuración.
- Información interna que facilite ataques.

Las excepciones internas no deben exponerse directamente al consumidor HTTP cuando contengan información técnica o interna.

Los errores deben traducirse al contrato HTTP correspondiente mediante los mecanismos establecidos por la arquitectura.

Los logs deben contener únicamente la información necesaria para operación, diagnóstico y auditoría.

No deben registrar deliberadamente contraseñas, tokens completos, API keys, claves privadas, secretos ni datos sensibles innecesarios.

#### 6.6. Criptografía

Cuando los requisitos requieran proteger información mediante cifrado, deben utilizarse mecanismos criptográficos seguros proporcionados por librerías o plataformas confiables y compatibles con los estándares aceptados por el proyecto.

No deben implementarse algoritmos criptográficos propios.

No deben utilizarse algoritmos o configuraciones criptográficas obsoletas cuando exista una alternativa segura compatible con los requisitos.

Las claves criptográficas deben gestionarse como secretos conforme a este principio.

#### 6.7. Comunicaciones externas

Las comunicaciones que transporten información protegida deben utilizar mecanismos de seguridad apropiados al protocolo y al entorno.

Cuando una integración externa requiera comunicación mediante HTTPS u otro mecanismo seguro definido por el proveedor, no debe degradarse deliberadamente la comunicación a un mecanismo inseguro.

La configuración de certificados y mecanismos de autenticación debe respetar las reglas de seguridad de la infraestructura utilizada.

Las reglas específicas sobre integraciones externas se encuentran establecidas en el Principio 3.

#### 6.8. Dependencias y vulnerabilidades

Las dependencias externas deben evaluarse desde el punto de vista de seguridad antes de su incorporación cuando existan mecanismos disponibles para realizar dicha evaluación.

No debe incorporarse una dependencia que introduzca una vulnerabilidad conocida de severidad relevante cuando exista una alternativa razonable que permita cumplir el mismo objetivo.

La severidad de las vulnerabilidades debe determinarse mediante las herramientas y estándares de seguridad adoptados por el proyecto.

Las vulnerabilidades detectadas deben tratarse mediante los mecanismos de seguridad y control de calidad establecidos por el proyecto.

###### 6.8.1. Vulnerabilidades introducidas o agravadas

Las vulnerabilidades críticas o de alta severidad que afecten al código o dependencias modificadas por el cambio deben resolverse antes de considerar terminado el trabajo, salvo que exista una excepción explícita del proyecto.

Toda excepción debe estar explícitamente documentada y justificada.

###### 6.8.2. Vulnerabilidades preexistentes

Una vulnerabilidad existente antes del cambio que no haya sido introducida ni agravada por este debe distinguirse de una vulnerabilidad provocada por el cambio.

Cuando se demuestre que una vulnerabilidad es preexistente y ajena al cambio, puede quedar fuera del alcance del cambio actual, pero debe registrarse o gestionarse mediante los mecanismos establecidos por el proyecto.

No deben realizarse modificaciones sin relación con el cambio únicamente para ocultar un hallazgo de seguridad.

#### 6.9. Mínimo privilegio y configuración segura

Los componentes, usuarios y procesos deben disponer únicamente de los permisos necesarios para cumplir su responsabilidad.

No deben concederse permisos adicionales únicamente por comodidad de implementación.

Las credenciales utilizadas para acceder a recursos deben disponer del nivel de privilegio mínimo compatible con la operación.

Las configuraciones relacionadas con seguridad deben utilizar valores seguros por defecto cuando la tecnología lo permita.

No deben deshabilitarse mecanismos de seguridad únicamente para simplificar el desarrollo, las pruebas o la implementación.

Cuando sea necesario utilizar temporalmente una configuración menos restrictiva en un entorno no productivo, esta debe permanecer aislada del comportamiento y configuración productivos.

#### 6.10. Seguridad y arquitectura

Las medidas de seguridad deben implementarse respetando la separación de responsabilidades establecida por los Principios 1 y 2.

Una medida de seguridad no debe utilizarse como justificación para introducir dependencias prohibidas, trasladar responsabilidades incorrectamente o concentrar lógica de diferentes componentes.

El Principio 6 establece las garantías de seguridad que deben cumplirse, pero no determina por sí mismo el componente arquitectónico en el que deben implementarse, salvo cuando otra regla de la Constitución lo establezca.

#### 6.11. Seguridad e integraciones externas

Los datos provenientes de sistemas externos deben considerarse no confiables hasta que hayan sido procesados y validados según las reglas aplicables.

Las integraciones externas deben respetar las reglas de confiabilidad, validación, errores, credenciales y comunicación establecidas en el Principio 3.

La existencia de una fuente externa confiable no elimina la necesidad de validar sus respuestas cuando la seguridad o corrección de la operación lo requieran.

#### 6.12. Seguridad en testing y cambios de seguridad

Las pruebas no deben incorporar secretos reales.

Las credenciales utilizadas durante los tests deben ser credenciales de prueba, valores ficticios o mecanismos equivalentes controlados por el entorno de pruebas.

Los controles de seguridad relevantes deben ser verificados mediante pruebas cuando formen parte del comportamiento de la funcionalidad modificada.

Las reglas generales de testing se encuentran establecidas en el Principio 4.

Todo cambio que afecte autenticación, autorización, gestión de secretos, exposición de datos, validación de entradas, comunicaciones externas, dependencias o mecanismos criptográficos debe evaluarse específicamente desde el punto de vista de seguridad.

El éxito de los tests funcionales no elimina la necesidad de esta evaluación cuando el cambio afecta directamente a un mecanismo de seguridad.

#### 6.13. Criterio general

La aplicación debe asumir que toda entrada externa puede ser manipulada y que todo mecanismo externo puede fallar o comportarse de manera inesperada.

Los controles de seguridad deben implementarse de forma explícita, verificable y coherente con la arquitectura.

La seguridad no debe depender de suposiciones sobre el comportamiento correcto de consumidores, sistemas externos, configuraciones o datos recibidos.

Cuando un requisito de seguridad no esté definido y la decisión pueda modificar significativamente el nivel de protección del sistema, debe solicitarse aclaración antes de introducir una decisión de seguridad arbitraria.

### 7. Contratos entre componentes

#### 7.1. Definición de contrato

Todo componente que exponga funcionalidad a otro componente debe hacerlo mediante un contrato explícito y claramente definido.

El contrato determina, según corresponda:

- Operaciones disponibles.
- Parámetros de entrada.
- Tipos de datos utilizados.
- Valores de retorno.
- Excepciones esperables.
- Restricciones relevantes.
- Precondiciones y postcondiciones necesarias para utilizar la operación.

Un componente consumidor no debe depender de detalles internos de implementación que no formen parte de su contrato.

#### 7.2. Contratos internos

Los componentes internos deben interactuar mediante las interfaces, clases, métodos o mecanismos definidos por su arquitectura.

El consumidor debe utilizar únicamente las operaciones necesarias para cumplir su responsabilidad.

No debe acceder directamente a atributos, estructuras internas o mecanismos de implementación de otro componente cuando estos no formen parte de su contrato.

Las reglas específicas sobre dependencias permitidas se encuentran establecidas en el Principio 2.

#### 7.3. Contrato HTTP

La API HTTP constituye un contrato externo de la aplicación.

El contrato HTTP incluye, según corresponda:

- Métodos HTTP.
- Rutas.
- Parámetros.
- Headers.
- Cookies.
- Cuerpos de solicitud.
- Códigos de respuesta.
- Estructura de las respuestas.
- DTOs.
- Reglas de validación.
- Errores expuestos al consumidor.

Los cambios sobre estos elementos deben considerarse cambios del contrato HTTP.

#### 7.4. Estabilidad del contrato

Una implementación interna no debe modificar el comportamiento observable de un contrato existente sin una justificación y evaluación del impacto correspondiente.

Los cambios que alteren las precondiciones, postcondiciones, tipos, respuestas, errores o comportamiento observable de una operación deben considerarse potencialmente incompatibles.

No debe asumirse que un cambio es compatible únicamente porque el código siga compilando.

#### 7.5. Compatibilidad de contratos

Un cambio es compatible cuando los consumidores existentes pueden continuar utilizando el contrato sin requerir modificaciones y sin que cambie de forma inesperada el comportamiento que reciben.

###### 7.5.1. Cambios potencialmente incompatibles

Son potencialmente incompatibles, entre otros:

- Eliminar una operación existente.
- Cambiar su firma de forma incompatible.
- Eliminar un campo obligatorio.
- Cambiar el tipo de un campo.
- Modificar el significado de un campo existente.
- Cambiar un código HTTP esperado por los consumidores.
- Cambiar la estructura de una respuesta existente.
- Introducir nuevas precondiciones obligatorias.
- Eliminar una respuesta o error que forme parte del contrato.

#### 7.6. Evolución de contratos

Los contratos deben evolucionar de forma controlada.

Antes de modificar un contrato existente debe determinarse si existen consumidores que dependan de él.

Cuando un cambio incompatible sea necesario, debe aplicarse el mecanismo de evolución definido por el proyecto.

No debe introducirse una modificación incompatible de forma silenciosa.

#### 7.7. Adición de información

La incorporación de nuevos datos a un contrato existente debe evaluarse según el tipo de consumidor y las reglas del contrato.

Agregar información no debe considerarse automáticamente compatible.

Los cambios deben mantener la capacidad de los consumidores existentes para interpretar correctamente el contrato cuando dicha compatibilidad sea un requisito.

#### 7.8. Eliminación de información

No debe eliminarse información que forme parte de un contrato utilizado por consumidores sin evaluar previamente su impacto.

Un campo, parámetro, operación o respuesta existente no debe eliminarse únicamente porque actualmente no sea utilizado por el código de la aplicación.

La ausencia de referencias internas no demuestra que no existan consumidores externos.

#### 7.9. Valores obligatorios y opcionales

Las modificaciones sobre la obligatoriedad de un dato deben considerarse cambios del contrato.

Un dato previamente opcional no debe convertirse en obligatorio sin evaluar el impacto sobre los consumidores existentes.

Un dato previamente obligatorio no debe convertirse en opcional cuando dicho cambio altere las garantías que ofrece el contrato.

La obligatoriedad debe estar determinada por el contrato correspondiente y no únicamente por la implementación técnica.

#### 7.10. Semántica de los datos

Los contratos no deben limitarse a definir tipos de datos.

El significado de cada dato debe mantenerse coherente con el comportamiento definido por la aplicación.

No debe cambiarse el significado de un campo, parámetro, código o estado manteniendo su mismo nombre y tipo con el objetivo de evitar un cambio formal del contrato.

Un cambio semántico debe tratarse como una modificación del contrato.

#### 7.11. Errores como parte del contrato

Cuando una operación exponga errores de forma definida, dichos errores forman parte de su comportamiento observable.

Los cambios en códigos HTTP, tipos de error, estructura de errores o condiciones que producen cada error deben evaluarse como cambios del contrato.

No deben exponerse excepciones internas directamente como mecanismo accidental de definición del contrato.

#### 7.12. Excepciones de contratos internos

Los componentes internos deben utilizar excepciones coherentes con el contrato de la operación que invocan.

Un consumidor no debe depender de detalles accidentales de implementación de excepciones internas.

Las excepciones técnicas que no formen parte del contrato no deben considerarse automáticamente parte del comportamiento garantizado del componente.

#### 7.13. Encapsulamiento de implementación

Un contrato debe exponer únicamente lo necesario para utilizar correctamente el componente.

Los consumidores no deben depender de:

- Estructuras internas.
- Variables internas.
- Orden interno de ejecución cuando no forme parte del comportamiento requerido.
- Implementaciones concretas cuando exista un contrato abstracto.
- Detalles de Persistence.
- Detalles de infraestructura.
- Detalles de proveedores externos.

La modificación de un detalle interno no debe requerir modificar consumidores cuando el contrato permanezca estable.

#### 7.14. DTO como contrato HTTP

Los DTO utilizados por la frontera HTTP representan el contrato de entrada o salida correspondiente.

Los DTO no deben utilizarse para exponer directamente Model o Entity cuando estas representaciones pertenezcan al funcionamiento interno de la aplicación.

Las reglas específicas de construcción y utilización de DTO se encuentran establecidas en el Principio 2.

#### 7.15. Contratos y Model

Los Model no deben diseñarse en función de los requisitos accidentales de un contrato HTTP.

El contrato HTTP debe adaptarse al dominio mediante los mecanismos definidos por la arquitectura.

Un cambio en el contrato HTTP no debe modificar automáticamente la estructura del Model si el dominio no requiere dicho cambio.

Del mismo modo, un cambio interno del Model no debe modificar automáticamente el contrato HTTP.

#### 7.16. Contratos e integraciones externas

Los contratos de sistemas externos deben mantenerse separados de los contratos internos y HTTP de la aplicación cuando la arquitectura así lo requiera.

Un cambio en un proveedor externo no debe propagarse automáticamente al contrato público de la aplicación.

Las integraciones deben encapsular las diferencias entre el contrato externo y las representaciones internas.

Las reglas específicas de integración se encuentran establecidas en el Principio 3.

#### 7.17. Contratos y seguridad

Los contratos no deben permitir operaciones que contradigan las restricciones de seguridad del sistema.

Los datos necesarios para autenticación y autorización deben formar parte del contrato únicamente cuando corresponda al mecanismo de seguridad utilizado.

No debe considerarse válido un contrato que permita acceder o modificar recursos sin las verificaciones de autorización requeridas.

Las reglas generales de seguridad se encuentran establecidas en el Principio 6.

#### 7.18. Validación del contrato

Las condiciones definidas por un contrato deben validarse en el componente responsable de garantizar dicho contrato.

###### 7.18.1. Validación HTTP

La validación de un contrato HTTP debe realizarse en la frontera correspondiente.

###### 7.18.2. Validación de negocio

Las validaciones de negocio deben permanecer en los componentes responsables de las reglas de negocio.

No debe trasladarse una validación a otro componente únicamente para evitar implementar correctamente el contrato.

#### 7.19. Consumidores desconocidos

Cuando un contrato pueda ser consumido por sistemas externos cuyo uso no pueda determinarse completamente, debe asumirse que puede existir dependencia sobre cualquier parte explícitamente documentada del contrato.

No debe eliminarse o modificarse una parte documentada del contrato basándose únicamente en la ausencia de referencias dentro del repositorio.

Cuando el alcance de los consumidores no pueda determinarse y el cambio pueda ser incompatible, debe evaluarse el cambio como potencialmente disruptivo.

#### 7.20. Contratos no documentados

Un comportamiento accidental que no haya sido definido como parte del contrato no debe considerarse automáticamente una garantía que deba mantenerse.

Cuando un comportamiento existente deba preservarse por compatibilidad, debe incorporarse explícitamente al contrato correspondiente cuando resulte necesario.

La ausencia de documentación no autoriza a modificar arbitrariamente un comportamiento que pueda ser utilizado por consumidores existentes.

#### 7.21. Pruebas de contratos

Los contratos críticos deben contar con pruebas que verifiquen su comportamiento observable.

Las pruebas deben cubrir, cuando corresponda:

- Entradas válidas.
- Entradas inválidas.
- Respuestas esperadas.
- Errores definidos.
- Estructura de los datos.
- Códigos HTTP.
- Restricciones relevantes del contrato.

Las reglas generales de testing se encuentran establecidas en el Principio 4.

#### 7.22. Cambios de contrato

Todo cambio que afecte un contrato debe identificarse explícitamente durante el desarrollo.

Antes de realizarlo debe determinarse:

1. Qué contrato se modifica.
2. Qué consumidores pueden verse afectados.
3. Si el cambio es compatible.
4. Qué pruebas deben actualizarse.
5. Qué documentación debe actualizarse.
6. Qué mecanismo de evolución corresponde aplicar cuando el cambio sea incompatible.

#### 7.23. Documentación

Todo contrato externo debe estar documentado mediante los mecanismos de documentación establecidos por el proyecto.

La documentación debe mantenerse sincronizada con el comportamiento real del contrato.

No debe documentarse un comportamiento que la implementación no garantice.

La documentación de la API se encuentra regulada específicamente por el Principio 8.

#### 7.24. Criterio general

Los contratos deben definir límites claros y estables entre componentes.

Los consumidores deben depender del contrato y no de detalles accidentales de implementación.

Todo cambio que pueda modificar el comportamiento observable de un contrato debe evaluarse antes de realizarse y debe ejecutarse de forma compatible con las reglas de evolución establecidas por el proyecto.

Cuando no pueda determinarse si un comportamiento forma parte de un contrato y modificarlo pueda afectar a consumidores existentes, el agente debe solicitar aclaración antes de eliminarlo o modificarlo de forma incompatible.

### 8. Documentación de

#### 8.1. Alcance y criterios generales

###### 8.1.1. Documentación de contratos

Los contratos de la aplicación que requieran documentación deben contar con una representación completa, precisa y coherente mediante el mecanismo de documentación correspondiente.

La documentación debe representar el contrato y comportamiento que los consumidores deben utilizar, sin modificar unilateralmente las reglas establecidas por la Spec, los requisitos funcionales o los contratos definidos por la arquitectura.

###### 8.1.2. Proporcionalidad y evidencia

La documentación debe proporcionar la información necesaria para comprender el contrato, comportamiento, condiciones, efectos y restricciones del elemento documentado.

No es obligatorio documentar información que resulte suficientemente evidente a partir del nombre, firma, tipos, anotaciones, contrato heredado, implementación y contexto, siempre que no aporte información adicional relevante.

La documentación debe ser proporcional a la complejidad del elemento y a la información que sea necesario comunicar.

###### 8.1.3. Contrato e implementación

La documentación debe describir el contrato y comportamiento observable del elemento, no los detalles internos de su implementación.

La documentación debe permanecer válida cuando la implementación interna cambie siempre que el contrato y comportamiento observable se mantengan.

Los detalles de implementación solo deben documentarse cuando sean relevantes para comprender una condición, restricción, efecto o comportamiento que no pueda determinarse razonablemente de otra forma.

###### 8.1.4. Separación respecto de otros principios

Este principio establece cómo deben documentarse los contratos y comportamientos correspondientes.

Las reglas sobre arquitectura, contratos, DTOs, validaciones, seguridad, testing y cambios establecidas en otros principios no deben duplicarse ni reinterpretarse mediante la documentación.

Cuando otro principio establezca requisitos específicos, estos deben cumplirse mediante los mecanismos correspondientes.

#### 8.2. Documentación del contrato HTTP

###### 8.2.1. Endpoints y operaciones

Todos los endpoints funcionales de la API deben estar documentados mediante OpenAPI.

A efectos de este principio, un endpoint es toda operación HTTP funcional expuesta intencionalmente por la aplicación mediante mecanismos de mapeo de Spring.

Los endpoints técnicos generados automáticamente por Spring, Spring Security u otras tecnologías no requieren documentación como endpoints funcionales cuando no formen parte intencional de la API de la aplicación.

Cada endpoint funcional debe documentarse mediante `@Operation`.

La documentación debe indicar como mínimo la finalidad de la operación, el comportamiento funcional relevante y la información necesaria para comprender su utilización.

La descripción no debe limitarse a repetir el nombre del método o endpoint cuando esto no aporte información adicional.

###### 8.2.2. Agrupación de operaciones

Los Controllers deben utilizar `@Tag` para agrupar los endpoints de acuerdo con su responsabilidad funcional.

Las etiquetas deben ser claras, consistentes y representativas del conjunto de operaciones agrupadas.

No deben crearse etiquetas diferentes para operaciones que pertenezcan a una misma responsabilidad funcional sin una justificación concreta.

###### 8.2.3. Parámetros, headers y cookies

Todos los parámetros HTTP que formen parte del contrato deben estar documentados.

Los parámetros correspondientes a `@PathVariable` y `@RequestParam` deben utilizar `@Parameter`.

La documentación debe describir su finalidad y las restricciones relevantes para su utilización.

Los headers y cookies que formen parte del contrato deben documentarse cuando sean relevantes para el comportamiento del endpoint.

Su documentación no implica que deban representarse mediante DTOs.

Los headers y cookies técnicos utilizados internamente por Spring u otras herramientas no requieren documentación cuando no formen parte del contrato de la API.

###### 8.2.4. Request Body

Los cuerpos de entrada de la API deben documentarse mediante los DTO correspondientes.

La documentación debe representar su estructura, campos, restricciones relevantes, obligatoriedad y nulabilidad cuando corresponda.

La documentación debe mantenerse coherente con las validaciones aplicadas al DTO.

###### 8.2.5. Response Body

Los cuerpos de salida de la API deben documentarse mediante los DTO correspondientes.

La documentación debe representar únicamente la información que forme parte del contrato HTTP.

No deben exponerse mediante OpenAPI Model, Entity u otras representaciones internas cuando estas no formen parte del contrato público.

###### 8.2.6. DTOs y campos

Los DTO utilizados como parte del contrato HTTP deben estar documentados mediante OpenAPI.

La documentación debe representar correctamente su estructura y significado.

Los Request DTO y Response DTO que formen parte del contrato HTTP deben utilizar `@Schema` cuando sea necesario para documentar explícitamente su nombre y descripción.

La descripción debe explicar la finalidad del DTO dentro del contrato de la API.

Los campos de los DTO que formen parte del contrato deben contar con descripción y ejemplo.

La descripción debe explicar el significado del campo.

El ejemplo debe representar un valor válido y coherente con el tipo y las restricciones del campo.

Los campos impuestos por frameworks, librerías, tecnologías o contratos externos pueden conservar la documentación requerida por dichas tecnologías cuando no sea posible aplicar directamente las convenciones de este principio.

Los campos que no formen parte del contrato HTTP no requieren documentación OpenAPI.

###### 8.2.7. Obligatoriedad, nulabilidad y validaciones

La documentación OpenAPI debe reflejar correctamente la obligatoriedad y nulabilidad establecidas por el contrato.

Un campo puede ser obligatorio y nullable cuando el contrato distinga entre la ausencia del campo y la presencia explícita del campo con valor `null`.

La anotación `@NotNull` debe utilizarse cuando corresponda según los requisitos funcionales.

OpenAPI no constituye por sí sola la fuente de verdad para determinar la obligatoriedad funcional de un campo.

Las validaciones de los Request DTO deben mantenerse coherentes con la documentación OpenAPI.

Las validaciones deben representar restricciones reales del contrato o de los requisitos funcionales.

No deben agregarse restricciones únicamente para modificar o mejorar la documentación generada.

###### 8.2.8. Ejemplos

Los ejemplos documentados deben representar valores válidos y realistas para el contrato.

No deben utilizarse ejemplos que contradigan las restricciones del DTO, las validaciones o la Spec.

Cuando un campo tenga restricciones específicas de formato, el ejemplo debe respetarlas.

#### 8.3. Respuestas HTTP

###### 8.3.1. Respuestas contractuales

Cada endpoint debe documentar mediante `@ApiResponse` los códigos de estado HTTP que formen parte de su comportamiento contractual.

Cada respuesta debe declararse mediante un `@ApiResponse` independiente.

No debe utilizarse `@ApiResponses`.

No es necesario documentar todos los códigos que técnicamente podría producir Spring, una librería, la infraestructura o una excepción inesperada.

La documentación debe representar las respuestas que el consumidor de la API puede esperar de acuerdo con el contrato y comportamiento definido.

###### 8.3.2. Respuestas de error

Las respuestas de error deben utilizar la estructura de errores definida por el contrato de la aplicación.

La estructura concreta y los campos de dichas respuestas deben estar determinados por la Spec y el contrato OpenAPI correspondiente.

No deben documentarse estructuras de error que la aplicación no garantice.

Las respuestas producidas por el mecanismo global de manejo de excepciones deben documentarse cuando formen parte del comportamiento contractual de los endpoints afectados.

La existencia de un `@ControllerAdvice` global no obliga por sí misma a documentar todas las excepciones que técnicamente pueda procesar.

Cuando una respuesta global pueda producirse como consecuencia de un comportamiento esperado del endpoint, debe documentarse.

###### 8.3.3. Respuestas sin cuerpo

Cuando una respuesta no tenga cuerpo, su `@ApiResponse` no debe declarar `content`.

Cuando una respuesta tenga cuerpo, debe documentarse su representación y el tipo correspondiente.

La documentación debe reflejar el cuerpo que realmente forma parte del contrato.

###### 8.3.4. Respuestas generadas automáticamente

Las respuestas generadas automáticamente por Spring, Spring Security u otros mecanismos deben documentarse únicamente cuando formen parte del comportamiento contractual esperado del endpoint.

No deben documentarse respuestas que sean únicamente consecuencias accidentales de la implementación o de errores inesperados.

La respuesta `500` debe documentarse cuando forme parte del comportamiento contractual previsto para el endpoint.

No debe introducirse una excepción específica ni una respuesta artificial únicamente para justificar la documentación de un `500`.

Una excepción inesperada que técnicamente pueda producir un `500` no constituye por sí misma una respuesta contractual documentable.

###### 8.3.5. Seguridad

Los requisitos de seguridad que formen parte del contrato del endpoint deben documentarse mediante OpenAPI.

Las respuestas `401` y `403` deben documentarse cuando puedan producirse como consecuencia de los mecanismos de autenticación y autorización aplicables al endpoint y formen parte del comportamiento esperado.

No deben documentarse únicamente porque el framework pueda producirlas técnicamente.

#### 8.4. Documentación mediante OpenAPI

###### 8.4.1. Operaciones y respuestas

La documentación de las operaciones, parámetros, cuerpos y respuestas debe utilizar los mecanismos de OpenAPI establecidos para representar cada elemento del contrato HTTP.

Las respuestas deben declarar su código y descripción, y su contenido cuando corresponda.

###### 8.4.2. DTOs y esquemas

Los DTO utilizados en el contrato HTTP deben documentarse mediante los esquemas correspondientes de OpenAPI.

La documentación debe representar únicamente la información que forme parte del contrato público.

Las reglas arquitectónicas sobre construcción y utilización de DTOs se encuentran establecidas en el Principio 2.

###### 8.4.3. Configuración y generación

La configuración utilizada para generar o exponer OpenAPI puede determinarse según las necesidades técnicas del proyecto.

La generación automática de documentación no exime del cumplimiento de las convenciones establecidas por este principio.

Cuando la herramienta genere documentación automáticamente, el resultado debe verificarse y corregirse cuando no represente correctamente el contrato.

###### 8.4.4. Documentación como representación verificable

La documentación OpenAPI debe poder utilizarse como representación verificable del contrato HTTP.

No debe contener información contradictoria, ambigua o incompatible con la Spec, las validaciones, los DTOs o el comportamiento contractual de la API.

#### 8.5. Documentación de métodos y constructores

###### 8.5.1. Métodos y constructores públicos

Todos los métodos y constructores públicos deben contar con Javadoc.

La documentación debe describir el contrato y comportamiento relevante del elemento, proporcionando la información necesaria para comprender su utilización.

###### 8.5.2. Elementos no públicos

Los métodos y constructores no públicos deben contar con Javadoc cuando su propósito, comportamiento, condiciones, efectos o restricciones no sean evidentes a partir del código y su contexto.

Los métodos `private` siguen este mismo criterio.

No es obligatorio documentar mediante Javadoc los elementos cuyo propósito y comportamiento resulten suficientemente claros a partir de su implementación y contexto.

###### 8.5.3. Métodos `protected`

Los métodos `protected` deben documentarse cuando su propósito, comportamiento, condiciones, efectos o restricciones no sean evidentes a partir del código y su contexto.

Cuando un método `protected` forme parte de un contrato utilizado por clases derivadas, su documentación debe describir las condiciones relevantes para su utilización y extensión.

###### 8.5.4. Métodos sobrescritos e implementaciones

Los métodos que sobrescriban métodos de una clase o interfaz no requieren duplicar mediante Javadoc la documentación del contrato heredado cuando esta sea suficiente para comprender su comportamiento.

Las implementaciones de una interfaz no deben duplicar innecesariamente la documentación del contrato cuando el Javadoc heredado sea suficiente.

Cuando la implementación introduzca condiciones, efectos, restricciones o comportamientos adicionales que no estén definidos por el contrato heredado, dicha información debe documentarse.

###### 8.5.5. Métodos requeridos o generados por frameworks

Los métodos generados automáticamente o requeridos por frameworks, librerías, herramientas u otros mecanismos técnicos no requieren Javadoc cuando su comportamiento esté completamente determinado por dicho mecanismo y no exista información adicional relevante que documentar.

Cuando el método contenga comportamiento propio de la aplicación que no esté determinado por el mecanismo técnico, debe documentarse según las reglas aplicables a dicho método.

###### 8.5.6. Métodos abstractos

Los métodos `abstract` deben documentarse cuando definan un contrato que deba ser implementado por otras clases.

La documentación debe describir el comportamiento esperado, las condiciones de utilización, los parámetros, el resultado, las excepciones y demás restricciones relevantes del contrato.

###### 8.5.7. Métodos estáticos

Los métodos `static` deben documentarse siguiendo las mismas reglas aplicables a su nivel de visibilidad y responsabilidad.

Su condición de estáticos no constituye por sí misma una excepción a las obligaciones de documentación.

###### 8.5.8. Métodos de acceso y modificación de estado

Los métodos cuyo único propósito sea exponer directamente un estado, como getters, no requieren una descripción extensa cuando su comportamiento sea completamente evidente por su nombre, tipo de retorno y contexto.

Cuando exista información adicional relevante sobre el significado del valor expuesto, sus restricciones o sus condiciones de utilización, dicha información debe documentarse.

Los métodos que modifiquen el estado de un objeto deben documentarse cuando la modificación implique condiciones, invariantes, efectos secundarios o restricciones que no sean evidentes a partir de su nombre y firma.

Cuando la modificación del estado esté sujeta a reglas de dominio, la documentación debe describir las condiciones relevantes para realizar la operación.

#### 8.6. Contenido del Javadoc

###### 8.6.1. Contrato y comportamiento

Cuando corresponda documentar un método o constructor, el Javadoc debe incluir la información necesaria para comprender, según corresponda, su propósito y comportamiento.

No es obligatorio documentar información que resulte evidente a partir de la firma, el nombre, el contexto o el código y que no aporte información adicional para comprender el contrato.

###### 8.6.2. Parámetros y retorno

Cuando un parámetro no tenga un significado completamente evidente a partir de su nombre, tipo y contexto, debe documentarse mediante `@param`.

La documentación debe explicar el significado del parámetro y las condiciones relevantes para su utilización cuando corresponda.

Cuando el significado o las condiciones del valor de retorno no sean completamente evidentes a partir de su tipo, nombre y contexto, debe documentarse mediante `@return`.

La documentación debe explicar qué representa el resultado y las condiciones relevantes de su obtención cuando corresponda.

###### 8.6.3. Excepciones

Cuando una excepción forme parte relevante del contrato o comportamiento esperado de un método, debe documentarse mediante `@throws` o `@exception`.

La documentación debe indicar la condición que provoca la excepción cuando dicha condición sea relevante para comprender la utilización del método.

No es obligatorio documentar excepciones técnicas cuya aparición no forme parte relevante del contrato del método y cuya documentación no aporte información útil para su utilización.

###### 8.6.4. Efectos secundarios

Cuando un método produzca efectos secundarios relevantes además de su valor de retorno, dichos efectos deben documentarse.

Esto incluye, cuando corresponda:

- Modificación de estado.
- Persistencia de información.
- Emisión de eventos.
- Comunicación con otros componentes.
- Comunicación con sistemas externos.
- Otras modificaciones observables relevantes.

###### 8.6.5. Condiciones y restricciones

Cuando un método requiera condiciones específicas para ser utilizado correctamente, estas deben documentarse.

Las condiciones pueden incluir, entre otras:

- Valores permitidos o no permitidos.
- Estado requerido del objeto.
- Precondiciones.
- Restricciones de utilización.
- Dependencias relevantes.
- Condiciones que provoquen excepciones.

No deben documentarse como condiciones aquellas restricciones que ya sean impuestas y verificadas automáticamente por la firma o mecanismos técnicos correspondientes, salvo que exista información adicional relevante.

###### 8.6.6. Constructores

Los constructores deben documentarse siguiendo las mismas reglas aplicables a los métodos.

Cuando un constructor imponga condiciones, inicialice invariantes, produzca efectos relevantes o requiera determinados valores para construir correctamente el objeto, dicha información debe documentarse.

Los constructores públicos deben contar siempre con Javadoc conforme a la regla de documentación obligatoria de elementos públicos.

###### 8.6.7. Redundancia y detalles de implementación

No deben agregarse comentarios o Javadoc que repitan innecesariamente información que ya resulte evidente a partir del nombre del elemento, sus parámetros, su tipo de retorno, las anotaciones aplicables, el contrato heredado y el contexto.

La documentación debe aportar información adicional relevante para comprender el contrato o comportamiento.

No debe utilizarse Javadoc para compensar código poco claro, responsabilidades mal definidas o una implementación innecesariamente compleja.

#### 8.7. Herencia y contratos de métodos

###### 8.7.1. Documentación de interfaces

Cuando una interfaz defina un contrato interno relevante, su Javadoc debe permitir comprender las responsabilidades, operaciones, entradas, salidas, condiciones y restricciones relevantes del contrato.

El Javadoc de la interfaz debe constituir la documentación principal del contrato cuando dicho contrato sea aplicable a sus implementaciones.

###### 8.7.2. Herencia de documentación

Cuando la tecnología utilizada permita heredar documentación de un contrato superior y dicha documentación sea suficiente para describir el comportamiento del elemento, no debe duplicarse innecesariamente.

Cuando sea necesario agregar información específica de la implementación, debe documentarse únicamente dicha información adicional.

###### 8.7.3. Información específica de las implementaciones

Las implementaciones de interfaces y los métodos que sobrescriban contratos superiores deben documentar las condiciones, efectos, restricciones o comportamientos adicionales que no estén definidos por el contrato heredado.

No debe duplicarse información que ya se encuentre suficientemente documentada en el contrato superior.

#### 8.8. Coherencia y actualización

###### 8.8.1. Coherencia con la Spec

OpenAPI debe mantenerse coherente con la Spec y los requisitos funcionales.

Cuando exista una contradicción entre OpenAPI y la Spec, debe prevalecer la Spec.

La documentación debe actualizarse para representar el comportamiento definido por la fuente de mayor precedencia.

###### 8.8.2. Coherencia con el contrato

OpenAPI debe representar el contrato HTTP de la aplicación.

La documentación no puede utilizarse para modificar unilateralmente el comportamiento requerido por la Spec.

Cuando exista una contradicción entre el contrato definido y la documentación, debe determinarse cuál es la fuente de mayor precedencia antes de modificar la implementación.

###### 8.8.3. Coherencia con la implementación

La implementación debe respetar el contrato documentado cuando OpenAPI sea coherente con la Spec y los requisitos aplicables.

Cuando la implementación contradiga el contrato documentado, debe adaptarse la implementación o modificarse formalmente el contrato de acuerdo con las reglas de precedencia y cambios establecidas por esta Constitución.

No debe modificarse OpenAPI únicamente para ocultar una implementación incorrecta.

###### 8.8.4. Actualización de la documentación

Toda modificación que afecte el contrato o comportamiento documentado de un elemento debe actualizar la documentación correspondiente dentro del mismo cambio.

En el caso del contrato HTTP, esto incluye, cuando corresponda:

- Rutas.
- Métodos HTTP.
- Parámetros.
- Headers.
- Cookies.
- Request DTOs.
- Response DTOs.
- Códigos de estado.
- Respuestas de error.
- Requisitos de seguridad.
- Cuerpos de solicitud.
- Cuerpos de respuesta.

Cuando una modificación cambie el contrato HTTP, la implementación y la documentación deben modificarse de forma coherente.

Las pruebas correspondientes deben actualizarse cuando el cambio afecte el comportamiento verificado, conforme a las reglas establecidas en el Principio 4.

La gestión y delimitación del cambio se rige por el Principio 11.

#### 8.9. Idioma y nomenclatura

La documentación debe respetar las reglas de idioma y nomenclatura establecidas en el Principio 10.

#### 8.10. Verificación de la documentación

###### 8.10.1. Verificación de OpenAPI

La documentación OpenAPI debe verificarse como parte de los controles aplicables al proyecto.

La generación automática de documentación no exime de comprobar que el resultado represente correctamente el contrato.

###### 8.10.2. Verificación automatizada

Cuando exista un mecanismo automatizado razonablemente viable capaz de comprobar una regla obligatoria de documentación establecida por esta Constitución, debe utilizarse.

La ausencia de un mecanismo automatizado no elimina la obligación de cumplir la regla documental correspondiente.

###### 8.10.3. Cumplimiento verificable

La documentación debe poder verificarse respecto de las reglas obligatorias establecidas por este principio.

Los mecanismos de verificación deben detectar, cuando sea técnicamente posible, incumplimientos que puedan comprobarse automáticamente.

#### 8.11. Cambios de contratos y documentación

Cuando una modificación del contrato HTTP sea incompatible con consumidores existentes o potenciales, deben aplicarse las reglas establecidas en el Principio 7.

La documentación OpenAPI debe actualizarse para representar el contrato resultante únicamente después de que el cambio haya sido determinado y aprobado de acuerdo con dichas reglas.

#### 8.12. Criterio general

La documentación debe representar de forma precisa, completa y verificable el contrato y comportamiento que corresponda documentar.

La documentación no debe adelantarse a la definición funcional, modificar unilateralmente el contrato ni utilizarse para ocultar inconsistencias de la implementación.

La documentación debe mantenerse sincronizada con el contrato y comportamiento documentados, respetando las reglas de precedencia y cambios establecidas por esta Constitución.

### 9. Idioma y convenciones lingüísticas

#### 9.1. Idioma del proyecto

El idioma principal y obligatorio de trabajo del proyecto es el español.

Todo contenido específico del proyecto generado, modificado, revisado o analizado por personas o agentes de inteligencia artificial debe redactarse en español, salvo las excepciones establecidas en este principio.

#### 9.2. Alcance

La obligación de utilizar español se aplica al contenido específico del proyecto presente en el repositorio y en los mecanismos de trabajo asociados al proyecto.

Esto incluye, entre otros:

- Código fuente.
- Documentación.
- Especificaciones.
- Archivos de configuración.
- Tests.
- Mensajes generados por la aplicación.
- Nombres de clases, métodos, variables y atributos.
- Ramas.
- Commits.
- Pull Requests.
- Issues.

#### 9.3. Nomenclatura propia del proyecto

Los nombres de clases, métodos, variables y atributos creados para representar conceptos propios del proyecto deben utilizar nomenclatura en español.

Las excepciones establecidas expresamente en este principio deben respetarse aunque el concepto correspondiente pueda expresarse en español.

#### 9.4. Elementos impuestos por tecnologías

Los nombres impuestos por lenguajes, frameworks, librerías, herramientas, interfaces técnicas o mecanismos tecnológicos deben conservarse en su nomenclatura original cuando su modificación afecte su funcionamiento, integración o compatibilidad.

Esto incluye, entre otros:

- Métodos sobrescritos.
- Anotaciones.
- Interfaces técnicas.
- Métodos requeridos por frameworks.
- Elementos definidos por APIs técnicas.
- Identificadores requeridos por herramientas.

#### 9.5. Interfaces externas

Los nombres definidos por APIs, servicios, sistemas, archivos, protocolos u otros contratos externos pueden conservarse en su idioma y nomenclatura original cuando deban respetarse para mantener la compatibilidad con la interfaz externa.

La utilización de una nomenclatura externa no obliga a utilizarla en las representaciones internas cuando exista una representación propia equivalente y no exista una restricción técnica o contractual que lo impida.

#### 9.6. Convenciones técnicas

Los términos técnicos de uso universal o ampliamente establecidos pueden conservarse en inglés cuando constituyan una convención técnica clara del proyecto o de la tecnología utilizada.

El agente no debe traducir automáticamente un término técnico cuando la traducción pueda generar ambigüedad, perder significado técnico o apartarse de una convención establecida.

###### 9.6.1. Determinación de una convención técnica

No es necesario verificar mediante una fuente externa toda convención técnica.

Cuando el término constituya una convención técnica clara y ampliamente establecida, el agente puede utilizar su conocimiento.

Cuando determinar si un término constituye una convención técnica sea relevante para una decisión significativa y exista una duda real, debe verificarse mediante una fuente confiable, dando preferencia a la documentación oficial, código oficial o documentación de la tecnología o librería correspondiente.

#### 9.7. Nomenclatura arquitectónica

Los términos técnicos establecidos por la arquitectura del proyecto pueden conservarse en inglés cuando constituyan nomenclatura técnica de los componentes.

Esto incluye, entre otros:

- DTO.
- Request.
- Response.
- Mapper.
- Controller.
- Service.
- Entity.
- Repository.
- Orchestrator.

La utilización de estos términos no constituye una violación de la regla general de nomenclatura en español.

#### 9.8. Nombres propios del proyecto

Los nombres propios del proyecto, producto, dominio o sistema pueden conservarse independientemente del idioma cuando constituyan su denominación oficial.

Los nombres propios no deben traducirse cuando la traducción altere su denominación oficial.

#### 9.9. Nombres de dominio

Los conceptos propios del dominio deben utilizar nomenclatura en español, salvo que exista una excepción establecida por este principio o una restricción técnica o contractual que requiera conservar otra nomenclatura.

Por ejemplo, `UsuarioService` es una nomenclatura válida para representar un Service relacionado con usuarios.

#### 9.10. Endpoints

Las rutas propias del proyecto deben estar redactadas en español.

Los nombres impuestos por frameworks, tecnologías, APIs externas u otros sistemas pueden conservarse en su nomenclatura original cuando deban respetarse.

La nomenclatura de las rutas propias debe mantenerse consistente en toda la API.

#### 9.11. Campos JSON

Los nombres de los campos JSON propios de los contratos de la aplicación deben estar redactados en español.

Pueden conservarse nombres en inglés cuando sean impuestos por un contrato externo, una tecnología o una restricción técnica que requiera mantener dicha nomenclatura.

#### 9.12. Mensajes de la aplicación

Los mensajes generados por la aplicación deben estar redactados en español.

Los mensajes generados automáticamente por frameworks, librerías u otras herramientas pueden conservarse en su idioma original cuando no sean controlados directamente por la aplicación.

#### 9.13. Datos de ejemplo

Los datos de ejemplo utilizados en documentación, DTOs, OpenAPI, tests u otros artefactos del proyecto deben redactarse en español cuando representen información propia del proyecto.

Pueden utilizarse valores o términos en otros idiomas cuando sean necesarios para representar una interfaz externa, una restricción técnica o un caso específico que requiera conservarlos.

#### 9.14. Git y gestión del trabajo

Las ramas, commits, Pull Requests e Issues deben estar redactados en español.

Pueden conservarse nombres propios, identificadores técnicos o elementos impuestos por herramientas o sistemas externos cuando deban mantenerse en su forma original.

#### 9.15. Tests

Los nombres de variables, métodos y demás elementos creados específicamente para los tests deben utilizar nomenclatura en español, salvo las excepciones establecidas por este principio.

Los nombres impuestos por frameworks, librerías o mecanismos técnicos deben conservarse.

La documentación y los mensajes propios de los tests deben redactarse en español.

#### 9.16. Código preexistente

Las violaciones lingüísticas presentes en código preexistente no deben corregirse automáticamente cuando se encuentren fuera del alcance de la tarea.

Deben modificarse cuando sea necesario para completar la tarea, cumplir una regla aplicable o evitar que el cambio realizado introduzca o mantenga una inconsistencia necesaria para su implementación.

La existencia de una violación preexistente no autoriza a introducir nuevas violaciones lingüísticas en código modificado o creado.

#### 9.17. Aplicación de excepciones

Cuando este principio establezca expresamente una excepción lingüística, el agente debe aplicarla directamente.

No debe solicitar aclaración ni justificar nuevamente una excepción que ya se encuentre definida.

Cuando una excepción no esté expresamente establecida, el agente puede determinar razonablemente si el caso corresponde a una convención técnica o a una restricción necesaria.

Solo debe solicitar aclaración cuando exista una duda real y relevante sobre la aplicación de la excepción y la decisión pueda afectar de forma significativa la consistencia, arquitectura, contratos o mantenimiento del proyecto.

#### 9.18. Términos técnicos no establecidos

Cuando un término en inglés no constituya una excepción expresamente establecida ni una convención técnica clara, debe utilizarse su equivalente en español cuando exista uno adecuado.

Si existen dudas relevantes sobre si el término constituye una convención técnica o si debe conservarse en inglés, el agente debe solicitar aclaración antes de adoptar una decisión significativa.

#### 9.19. Coherencia lingüística

Las excepciones lingüísticas no deben utilizarse para introducir arbitrariamente nomenclatura en inglés.

Cuando exista una nomenclatura propia del proyecto para un concepto, esta debe utilizarse de forma consistente.

La existencia de un término técnico en inglés no autoriza por sí misma a utilizar nombres en inglés para conceptos propios del proyecto que no requieran dicha nomenclatura.

#### 9.20. Spec Kit

La sintaxis, estructura, palabras clave, comandos, identificadores y demás elementos definidos por Spec Kit deben conservarse exactamente según las convenciones oficiales de Spec Kit.

Estos elementos no deben traducirse, reemplazarse ni modificarse para cumplir la regla general de idioma.

El contenido descriptivo y específico del proyecto dentro de los artefactos de Spec Kit debe redactarse en español.

###### 9.20.1. Identificadores de Spec Kit

Los identificadores definidos por Spec Kit deben conservarse exactamente, incluyendo aquellos necesarios para preservar la estructura, referencias o funcionamiento del sistema.

No deben traducirse ni modificarse aunque estén redactados en inglés.

#### 9.21. Documentación externa

La documentación técnica externa puede estar redactada en su idioma original.

Cuando dicha información se incorpore como contenido específico del proyecto, debe adaptarse al español salvo que corresponda conservar nombres, citas, identificadores, terminología técnica o contenido impuesto por la fuente externa.

#### 9.22. Contenido generado por herramientas

El contenido generado automáticamente por herramientas externas puede conservar su idioma original cuando la aplicación o el proyecto no controle directamente dicho contenido.

Cuando el agente tenga control sobre el contenido generado, debe aplicar las reglas lingüísticas de este principio.

#### 9.23. Prioridad de las excepciones

Las excepciones lingüísticas expresamente establecidas por este principio tienen prioridad sobre la regla general de utilización del español.

No deben inferirse nuevas excepciones únicamente por conveniencia, preferencia personal o costumbre.

#### 9.24. Ambigüedad lingüística

Cuando este principio no determine el idioma o nomenclatura aplicable y no exista una convención técnica clara que permita resolver la decisión, el agente debe solicitar aclaración cuando la elección pueda afectar de forma relevante la consistencia, arquitectura, contratos o mantenimiento del proyecto.

No debe solicitar aclaración para decisiones lingüísticas triviales que puedan resolverse razonablemente mediante las reglas de este principio.

### 10. Cambios controlados

#### 10.1. Alcance de los cambios

Toda modificación debe tener un objetivo definido y estar relacionada con el alcance de la tarea.

El agente debe modificar únicamente los elementos necesarios para implementar la funcionalidad, corregir el comportamiento solicitado, cumplir una regla aplicable o mantener la coherencia de los elementos directamente afectados.

###### 10.1.1. Expansión necesaria del alcance

Cuando durante la implementación se determine que es necesario modificar elementos inicialmente no contemplados, el agente puede ampliar el alcance de los cambios cuando exista una relación directa y verificable con la tarea.

La ampliación debe limitarse a los cambios necesarios para alcanzar el objetivo correspondiente.

###### 10.1.2. No expansión arbitraria

El agente no debe ampliar el alcance de una tarea basándose únicamente en:

- Preferencias personales.
- Oportunidades de refactorización no relacionadas.
- Buenas prácticas genéricas.
- Problemas independientes.
- Necesidades futuras no definidas.

La expansión del alcance debe estar respaldada por una necesidad concreta y verificable.

#### 10.2. Archivos y componentes afectados

El alcance de una tarea no se limita a los archivos mencionados explícitamente en su descripción.

El agente puede modificar, crear, eliminar o renombrar archivos adicionales cuando dichos cambios sean necesarios para completar correctamente la tarea o cumplir las reglas aplicables.

###### 10.2.1. Creación de elementos

El agente puede crear los archivos, clases, componentes, configuraciones, pruebas y demás elementos necesarios para completar la tarea.

La creación de nuevos elementos debe responder a una responsabilidad o necesidad concreta y no debe utilizarse para introducir complejidad innecesaria.

###### 10.2.2. Eliminación de elementos

El agente puede eliminar elementos cuando sea necesario para completar la tarea o cuando exista una justificación explícita para su eliminación.

Antes de eliminar un elemento debe considerar sus dependencias, referencias y consumidores conocidos.

No debe eliminarse un elemento cuando su eliminación pueda producir un cambio incompatible no contemplado por la tarea.

###### 10.2.3. Renombrado

El agente puede renombrar clases, métodos, variables, atributos, archivos u otros elementos cuando sea necesario o esté justificado por la tarea.

Los usos, referencias, dependencias y documentación afectada deben actualizarse de forma coherente.

#### 10.3. Refactorizaciones

Las refactorizaciones pueden incluirse cuando sean necesarias para:

- Implementar correctamente la tarea.
- Cumplir esta Constitución.
- Preservar la arquitectura.
- Mantener la calidad del código afectado.
- Mantener la coherencia de los elementos directamente afectados.

Una refactorización necesaria no debe considerarse ajena al alcance únicamente porque modifique archivos o componentes adicionales.

#### 10.4. Cambios no relacionados

No deben realizarse modificaciones que no estén relacionadas con la tarea, aunque representen oportunidades de mejora.

El agente no debe utilizar una tarea como justificación para realizar refactorizaciones generales, mejoras de estilo, reorganizaciones arquitectónicas o correcciones de problemas independientes que no sean necesarias para completar el objetivo.

#### 10.5. Mejoras de calidad relacionadas

Una mejora de calidad puede incluirse cuando esté directamente relacionada con el código afectado y sea necesaria para implementar correctamente la tarea, cumplir esta Constitución o producir una mejora clara y verificable de calidad o mantenibilidad.

Las mejoras que no tengan una relación directa con la tarea deben permanecer fuera del alcance.

#### 10.6. Código preexistente

La existencia de una violación de esta Constitución en código preexistente no obliga al agente a corregirla cuando se encuentre fuera del alcance de la tarea.

El agente debe corregirla cuando sea necesario para completar la tarea, cumplir una regla constitucional aplicable al cambio o evitar que la modificación produzca un incumplimiento.

###### 10.6.1. Controles sobre código preexistente

La prohibición de corregir automáticamente una violación fuera del alcance no impide implementar o ejecutar controles automatizados que la Constitución establezca como obligatorios.

Debe distinguirse entre:

- Detectar o controlar una violación.
- Modificar el código que contiene la violación.

La obligación de implementar o ejecutar un control obligatorio no implica por sí misma la obligación de corregir todo el código que dicho control detecte cuando la corrección se encuentre fuera del alcance de la tarea.

#### 10.7. Preservación del comportamiento

Los cambios deben preservar el comportamiento existente que no forme parte del objetivo de la modificación.

Cuando una modificación altere intencionalmente un comportamiento existente, el cambio debe estar respaldado por la Spec, requisitos, contrato o alcance de la tarea correspondiente.

#### 10.8. Cambios funcionales

Los cambios funcionales deben actualizar todos los elementos afectados por el nuevo comportamiento, incluyendo cuando corresponda:

- Implementación.
- Tests.
- Contratos.
- DTOs.
- OpenAPI.
- Documentación.
- Configuración.
- Controles automatizados.

#### 10.9. Cambios de contratos

Cuando una modificación afecte un contrato, deben considerarse los consumidores afectados y aplicarse las reglas de compatibilidad establecidas por esta Constitución.

No debe introducirse unilateralmente un cambio incompatible cuando no pueda determinarse razonablemente cómo afectará a los consumidores.

#### 10.10. Cambios de configuración y dependencias

Las modificaciones de configuración deben estar directamente relacionadas con la tarea, una necesidad técnica legítima o el cumplimiento de una regla constitucional.

No deben utilizarse para eludir, debilitar, ocultar o evitar controles obligatorios.

Las dependencias pueden agregarse, eliminarse o modificarse cuando sean necesarias para implementar o validar correctamente la tarea.

El cambio debe estar directamente relacionado con una necesidad concreta y respetar las demás reglas aplicables del proyecto.

#### 10.11. Tests durante los cambios

El agente puede y debe crear, modificar o eliminar pruebas cuando sea necesario para reflejar el comportamiento correcto de la funcionalidad modificada.

Las pruebas no deben modificarse únicamente para conseguir resultados exitosos cuando el comportamiento que verifican siga siendo válido.

Las pruebas obsoletas pueden eliminarse cuando el comportamiento que verificaban haya dejado de formar parte del contrato o funcionalidad vigente.

#### 10.12. Documentación durante los cambios

La documentación afectada por una modificación debe actualizarse para mantener su coherencia con el comportamiento y los contratos vigentes.

Cuando una modificación cambie un contrato, deben actualizarse las representaciones documentales correspondientes.

#### 10.13. Reporte de cambios adicionales

Cuando el agente haya debido modificar elementos fuera del alcance inicialmente mencionado, debe informar qué elementos fueron modificados y por qué eran necesarios para completar la tarea o cumplir las reglas aplicables.

#### 10.14. Precedencia de las reglas

El alcance indicado por una tarea no puede utilizarse para justificar el incumplimiento de una regla constitucional obligatoria.

Cuando completar una tarea requiera realizar cambios adicionales para cumplir una regla de mayor precedencia, dichos cambios forman parte de lo necesario para completar correctamente la tarea.

#### 10.15. Modificaciones constitucionales

El agente no puede modificar, eliminar, agregar ni aplicar por sí mismo reglas de esta Constitución.

Puede identificar contradicciones, problemas o necesidades de modificación y proponer una enmienda, pero la modificación constitucional requiere el procedimiento de aprobación establecido por esta Constitución.

#### 10.16. Decisiones no determinadas

Cuando una modificación requiera tomar una decisión relevante que no esté determinada por la Constitución, Spec, contratos, código existente o convenciones aplicables, el agente debe seguir las reglas generales de decisión establecidas por esta Constitución.

Si persiste una ambigüedad relevante con consecuencias importantes, debe solicitar aclaración antes de continuar con dicha decisión.

#### 10.17. Finalización del cambio

Una tarea no debe considerarse completamente finalizada mientras existan elementos directamente afectados por el cambio que se encuentren desactualizados o sean incoherentes con el comportamiento final.

Cuando corresponda, el agente debe verificar la coherencia entre:

- Implementación.
- Tests.
- Contratos.
- DTOs.
- OpenAPI.
- Documentación.
- Configuración.
- Controles automatizados.

La validación de estos elementos debe realizarse de acuerdo con las obligaciones establecidas por los demás principios de esta Constitución.

#### 10.18. Criterio general

Los cambios deben ser suficientemente amplios para completar correctamente la tarea, pero suficientemente limitados para evitar modificaciones innecesarias.

El agente debe priorizar cambios trazables, justificados y coherentes con la tarea, preservando el comportamiento no afectado y evitando expandir arbitrariamente el alcance.

Cuando exista una duda relevante sobre si un cambio adicional es necesario y la decisión pueda producir consecuencias importantes, el agente debe solicitar aclaración antes de realizarlo.

## Reglas Generales de Aplicación

Estos criterios rigen la aplicación de los principios constitucionales y deben ser respetados por todos los colaboradores del proyecto, incluyendo agentes de inteligencia artificial.

- Los principios deben aplicarse tanto al código desarrollado manualmente como al código generado o modificado mediante agentes de inteligencia artificial.
- Las especificaciones, planes y decisiones de implementación deben interpretarse y ejecutarse de forma compatible con esta Constitution.
- Las decisiones concretas de arquitectura e implementación deben resolverse de acuerdo con la Constitution, la especificación correspondiente, los contratos aplicables, el código existente y las demás fuentes de decisión reconocidas por esta Constitution.
- Cuando una decisión no esté determinada por las fuentes anteriores y exista una ambigüedad que impida una decisión segura, debe solicitarse aclaración antes de adoptar una decisión que pueda afectar materialmente la arquitectura, los contratos o el comportamiento del sistema.

## Technology Constraints

### Stack tecnológico

El proyecto utiliza las siguientes tecnologías y versiones:

- **Build:** Gradle 8.14 o superior.
- **Lenguaje:** Java 21.
- **Framework:** Spring Boot 4.1.1.
- **Web:** Spring Web.
- **Seguridad:** Spring Security.
- **Autenticación y autorización:** OAuth2 Resource Server.
- **Persistencia:** Spring Data JPA.
- **Base de datos:** PostgreSQL 18.x.
- **Acceso a base de datos:** JDBC.
- **Migraciones:** Flyway.
- **Validación:** Jakarta Validation 3.1.1.
- **Documentación de API:** SpringDoc OpenAPI 3.1.0.
- **Testing:** Spring Boot Test.
- **Testing unitario:** JUnit.
- **Mocking:** Mockito.
- **Assertions:** AssertJ.
- **Testing de integración:** Testcontainers.
- **Análisis de calidad:** SonarQube.

Las dependencias cuya versión sea gestionada por Spring Boot deben utilizar la versión administrada por Spring Boot.

No deben declararse versiones manuales para dependencias gestionadas por Spring Boot salvo que exista una necesidad técnica justificada.

La incorporación de una tecnología que no forme parte del stack establecido debe estar técnicamente justificada y seguir el procedimiento de cambios establecido por esta Constitution.

Los `spec` y `plan` no deben repetir el stack tecnológico global definido en esta sección, salvo cuando una decisión específica de la feature requiera hacer referencia explícita a una tecnología determinada.

## Governance

La Constitution establece reglas generales y permanentes. Su evolución debe ser un proceso controlado para garantizar la estabilidad del proyecto.

Si una futura especificación o decisión entra en conflicto con un principio de esta Constitution, el conflicto debe identificarse explícitamente y la Constitution debe revisarse de forma controlada antes de adoptar la decisión incompatible.


La Constitution establece las reglas arquitectónicas y de desarrollo que deben respetarse durante la evolución del proyecto.

Las especificaciones, planes, decisiones de implementación y código generado o modificado deben ser compatibles con esta Constitution.

Cuando una especificación o decisión futura entre en conflicto con esta Constitution, el conflicto debe identificarse y resolverse mediante una modificación controlada de la Constitution antes de adoptar la decisión incompatible.

Ningún agente de IA puede modificar, eliminar, ignorar o reinterpretar deliberadamente una regla de esta Constitution por iniciativa propia.

Las modificaciones de la Constitution deben realizarse mediante el procedimiento de aprobación establecido para el proyecto y deben quedar documentadas.

Los cambios constitucionales deben incrementar la versión del documento de acuerdo con su naturaleza.

**Version**: 1.4.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-09-03

# Feature Specification: Tests de arquitectura en el backend

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md. -->

**Feature Branch**: `feat/037-arch-tests`

**Created**: 2026-11-27

**Status**: Ready for planning

**Input**: Issue #98 (`REQ-S3-01 | Crear tests de arquitectura con ArchUnit`), asignado a `YoelSLA`, registrado en `specs/assignments.md`.

## Clarifications

### Session 2026-11-27

- Q: ¿Debe la suite de arquitectura limitarse estrictamente al alcance del Issue #98 o incluir también reglas transversales sobre excepciones y mappers? → A: Ampliar la suite para incluir también la validación de la jerarquía de excepciones (FootballMarketException) y reglas de Mappers estáticos según architecture.md.
- Q: ¿Cómo deben organizarse físicamente las clases de prueba que componen la suite de arquitectura dentro del paquete de tests? → A: Clases especializadas por dimensión (LayerArchitectureTest, NamingAndAnnotationRulesTest, FeatureCyclesTest, ExceptionAndMapperRulesTest) dentro de footballmarket.architecture.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Verificación automática de capas y restricciones de dependencias (Priority: P1)

Como equipo de desarrollo y mantenimiento del backend, queremos que las reglas de capas y la matriz de dependencias arquitectónicas se verifiquen automáticamente mediante tests, para evitar que capas superiores accedan a persistencia directa o que el dominio se acople a frameworks web o infraestructura.

**Why this priority**: Es el objetivo central del requerimiento (RF-02, CA-02, CA-03) para prevenir la erosión arquitectónica del backend y asegurar que se respeten los roles definidos en la arquitectura.

**Independent Test**: Ejecutar la suite de tests de arquitectura sobre el código del backend y verificar que valida que los controladores no invoquen repositorios directamente y que los modelos de dominio no dependan de presentación ni de infraestructura web.

**Acceptance Scenarios**:

1. **Given** las clases ubicadas en paquetes de presentación (`presentation`), **When** se evalúan las dependencias hacia persistencia o repositorios (`persistence`, `infrastructure`, `infraestructure`), **Then** la suite verifica que ninguna clase de presentación invoque o dependa de ellas directamente sin pasar por la capa de aplicación.
2. **Given** las clases y entidades de dominio (`domain`), **When** se evalúan sus dependencias externas, **Then** la suite verifica que no dependan de paquetes de presentación ni de frameworks web (como `org.springframework.web`).
3. **Given** componentes de coordinación (`orchestrators`), **When** se evalúan sus dependencias, **Then** la suite verifica que no dependan de repositorios, integraciones externas ni de otros orquestadores.

---

### User Story 2 - Detección y prevención de dependencias cíclicas entre módulos (Priority: P1)

Como equipo de desarrollo, queremos verificar que no existan ciclos de dependencia entre los distintos paquetes de features del backend, para asegurar la modularidad y el bajo acoplamiento del sistema.

**Why this priority**: Evitar ciclos (RF-04, CA-04) es indispensable para mantener el backend modular, testeable y comprensible a largo plazo.

**Independent Test**: Evaluar los paquetes de features del backend con una regla de análisis de cortes (*slices*) y comprobar que no existan dependencias circulares directas ni indirectas entre ellos.

**Acceptance Scenarios**:

1. **Given** el conjunto de paquetes modulares de features (`footballmarket.features.*`), **When** se analiza el grafo de dependencias entre paquetes hermanos, **Then** la verificación comprueba que el diseño está 100% libre de ciclos.

---

### User Story 3 - Cumplimiento de convenciones de nomenclatura y anotaciones (Priority: P2)

Como equipo de desarrollo, queremos verificar que los componentes de presentación, aplicación y persistencia cumplan las convenciones de nombres y anotaciones de rol arquitectónico establecidas por el proyecto, para mantener la coherencia y predictibilidad del código base.

**Why this priority**: Satisface RF-03 y asegura que componentes con un rol técnico específico (controladores, repositorios, servicios, DTOs) utilicen el patrón de nombres y anotaciones adecuado sin ambigüedades.

**Independent Test**: Evaluar las clases del backend contra las reglas de nomenclatura y anotaciones, comprobando que las clases de presentación HTTP tengan el sufijo y anotación correctos, y que la persistencia delimite el uso de anotaciones según la convención del proyecto.

**Acceptance Scenarios**:

1. **Given** clases en paquetes de presentación que exponen endpoints HTTP, **When** se inspeccionan sus metadatos, **Then** la suite verifica que finalicen con el sufijo `Controller` y estén anotadas con `@RestController`.
2. **Given** clases de transferencia de datos (DTO), **When** se evalúa su definición estructural, **Then** la suite comprueba que estén implementadas como `record`.
3. **Given** la anotación `@Repository`, **When** se examinan sus apariciones en el código base, **Then** la suite comprueba que se utilice exclusivamente en interfaces de persistencia Spring Data y no en clases de servicio ni repositorios manuales.
4. **Given** las clases de excepción propias creadas por el proyecto, **When** se evalúa su jerarquía, **Then** la suite comprueba que todas hereden directa o indirectamente de `FootballMarketException` y que no se lancen directamente excepciones genéricas de Java en dominio o aplicación.
5. **Given** las clases de mapeo (`Mapper`), **When** se analiza su diseño, **Then** la suite comprueba que sean clases `final`, con constructor privado, métodos estáticos y sin anotaciones de componente Spring.

---

### User Story 4 - Fallo explícito ante violaciones arquitectónicas (Priority: P1)

Como equipo de desarrollo, queremos que cualquier transgresión de las reglas de arquitectura provoque el fallo explícito e inmediato de la prueba correspondiente en el entorno local y en la integración continua, para impedir que código no conforme llegue a la rama principal.

**Why this priority**: Satisface RF-05 y CA-05; una prueba de arquitectura solo es efectiva si rompe el build cuando se introduce una violación.

**Independent Test**: Comprobar que la suite de arquitectura se ejecute en el ciclo habitual de pruebas rápidas del backend y que una clase de prueba que simule o contenga una violación arquitectónica provoque el fallo explícito con un mensaje explicativo.

**Acceptance Scenarios**:

1. **Given** la suite de arquitectura integrada al conjunto de pruebas del backend, **When** se ejecuta la suite rápida de tests unitarios, **Then** los tests de arquitectura se ejecutan sin levantar servidor web ni base de datos relacional.
2. **Given** una regla de arquitectura infringida en el código analizado, **When** se ejecuta la prueba, **Then** la prueba falla explícitamente indicando la clase origen, la clase destino y la regla violada.

---

### Edge Cases

- **Variaciones ortográficas en paquetes de infraestructura:** El código base contiene paquetes con grafía `infraestructure` (ej. en catálogo, cotizaciones, estadísticas) y con grafía `infrastructure` (ej. en dashboard). La suite debe contemplar ambas variantes para no dejar clases sin inspección ni emitir falsos negativos.
- **Modelos de dominio con JPA:** La regla arquitectónica del proyecto permite que los modelos de dominio utilicen anotaciones de mapeo JPA estándar (`jakarta.persistence.*`), por lo que no deben confundirse con dependencias web ni de presentación.
- **Exclusión del análisis sobre clases de test:** La suite de arquitectura debe evaluar exclusivamente las clases productivas compiladas (`src/main/java`), evitando falsos positivos derivados de mocks, fixtures o helpers de prueba.
- **Paquetes transversales (`common`, `security`, `integrations`):** Los componentes transversales deben poder ser utilizados según las restricciones de la matriz arquitectónica sin que se generen advertencias espurias.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El backend MUST contar con una suite de tests de arquitectura automatizada utilizando la librería ArchUnit.
- **FR-002**: La suite MUST validar que ninguna clase ubicada en paquetes de presentación (`presentation`) dependa o invoque directamente a clases de infraestructura o repositorios de persistencia sin la intermediación de la capa de aplicación.
- **FR-003**: La suite MUST validar que las entidades y modelos de dominio (`domain`) permanezcan completamente desacoplados de la capa de presentación y de frameworks de infraestructura web (`org.springframework.web.*`).
- **FR-004**: La suite MUST validar la ausencia de dependencias cíclicas directas e indirectas entre los diferentes paquetes de features (`footballmarket.features.*`).
- **FR-005**: La suite MUST validar que las clases de presentación HTTP finalicen con el sufijo `Controller` y posean la anotación `@RestController`.
- **FR-006**: La suite MUST validar que los contratos de entrada y salida HTTP (DTOs) se implementen exclusivamente como `record`.
- **FR-007**: La suite MUST validar que la anotación `@Repository` se utilice únicamente en interfaces de persistencia Spring Data.
- **FR-008**: La suite MUST integrarse en el conjunto de pruebas del backend bajo la categoría de ejecución rápida (`@Tag("unit")`), ejecutándose en memoria a partir del bytecode compilado sin inicializar contextos pesados de Spring Boot, Docker ni PostgreSQL.
- **FR-009**: Cualquier violación a las reglas de arquitectura definidas MUST provocar el fallo explícito de la prueba, describiendo claramente la clase infractora, el elemento consumido y la regla transgredida.
- **FR-010**: La suite MUST validar que todas las clases de excepción propias del backend hereden directa o indirectamente de `FootballMarketException` y que no se lancen directamente excepciones genéricas de Java (`RuntimeException`, `IllegalArgumentException`, `IllegalStateException`) en código de dominio o aplicación.
- **FR-011**: La suite MUST validar que las clases Mapper sean `final`, con constructor privado, métodos estáticos y no sean administradas como beans o componentes de Spring.
- **FR-012**: Las pruebas de arquitectura MUST organizarse en clases especializadas por dimensión (`LayerArchitectureTest`, `NamingAndAnnotationRulesTest`, `FeatureCyclesTest`, `ExceptionAndMapperRulesTest`) dentro del paquete `footballmarket.architecture`.

### Key Entities

- **Regla de arquitectura (ArchRule):** Definición declarativa de una restricción estructural, de nomenclatura o de dependencia aplicable sobre un conjunto de clases compiladas.
- **Violación de arquitectura:** Incumplimiento detectado entre dos componentes que infringe la matriz de dependencias o convenciones establecidas, reportado con traza y detalle de clases infractoras.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las capas principales (presentación, aplicación, dominio, persistencia/infraestructura) y sus dependencias permitidas cuentan con validación automática ejecutable.
- **SC-002**: El 100% de las dependencias cíclicas potenciales entre paquetes de features son detectadas mediante reglas de corte (*slices*).
- **SC-003**: La suite completa de tests de arquitectura se ejecuta en menos de 5 segundos en un entorno estándar, sin requerir contenedores ni bases de datos.
- **SC-004**: Ante cualquier transgresión intencional o accidental de una regla arquitectónica, la prueba correspondiente falla de forma determinista y reproducible en el 100% de los casos.
- **SC-005**: La suite de pruebas de arquitectura clasifica y se ejecuta exitosamente dentro de las tareas de prueba rápida (`unitTest` / `fastTest`) de Gradle.

## Assumptions

- Se utiliza la biblioteca ArchUnit (`com.tngtech.archunit:archunit-junit5`) en su versión compatible con Java 21.
- La suite analiza el código compilado de producción (`src/main/java`) bajo el paquete base `footballmarket`.
- Las clases de prueba de arquitectura se ubican en el paquete de tests del backend (ej. `footballmarket.architecture`), cumpliendo con la clasificación estática de pruebas del proyecto (`validateTestClassification`).
- No se requiere refactorizar código de features existentes salvo que se detecten violaciones estrictas que impidan la aprobación de las reglas de arquitectura establecidas.

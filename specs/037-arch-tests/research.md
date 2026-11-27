# Investigación y Decisiones Técnicas: Tests de arquitectura con ArchUnit

**Feature**: `SPEC-037 | Tests de arquitectura en el backend`
**Fecha**: 2026-11-27
**Rama**: `feat/037-arch-tests`

Este documento consolida las decisiones tecnológicas, patrones de diseño y análisis de alternativas para la implementación de la suite de pruebas de arquitectura en el backend.

---

## Decisión 1: Framework de pruebas de arquitectura

* **Decisión:** Incorporar **ArchUnit JUnit 5** (`com.tngtech.archunit:archunit-junit5:1.4.1`).
* **Razón:**
  * Es la herramienta de facto en el ecosistema Java para verificar arquitectura como código ejecutable.
  * Inspecciona directamente el bytecode compilado (`.class`) mediante ASM sin requerir levantar el contenedor de inversión de control (Spring ApplicationContext), servidores web embebidos ni bases de datos.
  * Se integra de forma nativa con JUnit Jupiter mediante las anotaciones `@AnalyzeClasses` y `@ArchTest`, reportando fallos descriptivos con la traza de la clase origen, clase destino y línea donde se produce la violación.
  * Plena compatibilidad con Java 21 y Spring Boot 4.x.
* **Alternativas evaluadas:**
  * *Checkstyle / PMD / SpotBugs:* Son analizadores de código fuente / AST o bytecode orientados a calidad de código y estilo local (métodos, variables), pero no ofrecen un DSL fluido para modelar grafos de dependencias entre paquetes o matrices de capas.
  * *jQAssistant:* Muy potente para escaneo de grafos, pero requiere una base de datos Neo4j embebida o externa, lo que penaliza fuertemente el tiempo de compilación y ejecución de la suite de pruebas rápidas.

---

## Decisión 2: Integración en Gradle y clasificación de tests

* **Decisión:** Registrar `archunit` en `backend/gradle/libs.versions.toml` y agregarlo como `testImplementation(libs.archunit.junit5)` en `backend/build.gradle.kts`. Clasificar cada clase de prueba con `@Tag("unit")`.
* **Razón:**
  * El repositorio cuenta con la tarea estricta `validateTestClassification` que rechaza clases de prueba sin una `@Tag` literal válida (`unit`, `lightweight`, `integration`, `e2e`).
  * Los tests de arquitectura analizan bytecode en memoria en menos de 2 segundos; por tanto, pertenecen estrictamente a la categoría `unit` (sin Spring, sin base de datos, sin Docker).
  * Al pertenecer a `unit`, se ejecutarán automáticamente en `./gradlew unitTest`, `./gradlew fastTest` y en los hooks pre-push sin penalizar la velocidad de feedback.
* **Alternativas evaluadas:**
  * *`@Tag("lightweight")`:* Descartada porque `lightweight` en este proyecto está reservada para tests que cargan un slice reducido de Spring (`@WebMvcTest`, `ApplicationContextRunner`). ArchUnit no requiere Spring.

---

## Decisión 3: Estructura modular de la suite de pruebas

* **Decisión:** Organizar las pruebas en 4 clases especializadas dentro de `backend/src/test/java/footballmarket/architecture/`:
  1. `LayerArchitectureTest`: Restricciones de dependencias entre capas (Presentación → Persistencia/Infraestructura prohibido; Dominio → Web/Presentación prohibido; Orquestadores delimitados).
  2. `NamingAndAnnotationRulesTest`: Convenciones de nombres (`*Controller`, DTOs como `record`) y uso de `@Repository` restringido a interfaces Spring Data.
  3. `FeatureCyclesTest`: Ausencia de dependencias cíclicas entre paquetes de features (`footballmarket.features.*`).
  4. `ExceptionAndMapperRulesTest`: Herencia obligatoria de `FootballMarketException`, prohibición de excepciones genéricas de Java en dominio/aplicación, y Mappers estáticos `final` no administrados por Spring.
* **Razón:**
  * Facilita el diagnóstico rápido en la salida de CI ante fallos: el desarrollador identifica inmediatamente qué dimensión arquitectónica fue violada.
  * Mantiene clases pequeñas, cohesivas y mantenibles.
* **Alternativas evaluadas:**
  * *Una única clase `BackendArchitectureTest`:* Agruparía decenas de reglas en un solo archivo monolítico de más de 300 líneas, dificultando la lectura y mantenimiento.

---

## Decisión 4: Manejo de anomalías y variantes ortográficas en paquetes

* **Decisión:** Utilizar expresiones de paquete tolerantes o composiciones de predicados:
  * Para paquetes de infraestructura: contemplar tanto `..infraestructure..` como `..infrastructure..` (mediante predicados `resideInAnyPackage("..infraestructure..", "..infrastructure..")` o regex `..infra*structure..`).
* **Razón:**
  * La mayoría de las features (`catalog`, `quote`, `statistics`, etc.) usan históricamente `infraestructure`, mientras que `dashboard` utiliza `infrastructure`. Para no dejar código sin verificar ni provocar falsos positivos, la suite debe cubrir ambas variantes.

---

## Decisión 5: Exclusión de clases de prueba del análisis estático

* **Decisión:** Configurar `@AnalyzeClasses` con `importOptions = {ImportOption.DoNotIncludeTests.class}` en todas las clases de prueba de arquitectura.
* **Razón:**
  * Los tests unitarios y de integración a menudo configuran mocks, llaman a métodos internos o crean fixtures que no deben evaluarse con las restricciones de producción. La suite debe validar exclusivamente el código productivo compilado bajo `src/main/java`.

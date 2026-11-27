# Plan de implementación: Tests de arquitectura en el backend con ArchUnit

**Rama**: `feat/037-arch-tests` | **Fecha**: 2026-11-27 | **Spec**: [spec.md](spec.md)

<!--
MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Documentación viva: describe exclusivamente el diseño técnico y contratos vigentes (Constitución §V).
Forma parte de la tríada permanente junto a spec.md y tasks.md.
Plan no genera Tasks ni autoriza IMPLEMENT: siguen Checklists, Tasks y Analyze.
-->

## Resumen

Incorporar la biblioteca ArchUnit (`archunit-junit5:1.4.1`) al backend de Football Market para verificar automáticamente mediante tests en memoria las reglas de arquitectura y diseño en capas, modularidad sin ciclos entre features, convenciones de nomenclatura y anotaciones, jerarquía de excepciones propias y diseño de mappers estáticos.

La suite se organizará en 4 clases especializadas en `footballmarket.architecture` bajo la categoría de prueba rápida `@Tag("unit")`, ejecutándose sin levantar contextos pesados de Spring Boot ni bases de datos relacionales, integrándose directamente en `validateTestClassification`, `unitTest` y `fastTest`.

## Contexto técnico

* **Lenguaje/versión**: Java 21.
* **Dependencias principales**: ArchUnit JUnit 5 (`com.tngtech.archunit:archunit-junit5:1.4.1`), JUnit Jupiter, Spring Boot 4.1.1 (gestión de dependencias de testing).
* **Persistencia**: N/A (los tests inspeccionan bytecode compilado en memoria; no acceden a base de datos ni ejecutan Flyway).
* **Verificación**: Tests automáticos clasificados con `@Tag("unit")`, ejecutables mediante `./gradlew unitTest` o `./gradlew test --tests "footballmarket.architecture.*"`. El agente no ejecuta tests de aplicación; la ejecución de tests corresponde al usuario.
* **Plataforma**: Subproyecto `backend/` de Gradle.
* **Tipo de proyecto**: Pruebas automatizadas de arquitectura como código (Arch tests).
* **Objetivos de rendimiento**: Ejecución completa de la suite de arquitectura en menos de 5 segundos en memoria.
* **Restricciones**: No levantar Spring ApplicationContext ni Testcontainers; evaluar únicamente el código productivo compilado (`src/main/java`) excluyendo clases de prueba; no alterar código funcional ni refactorizar features existentes salvo corrección de violaciones estrictas detectadas.
* **Alcance**: Capas de presentación, aplicación, dominio, persistencia/infraestructura, orquestadores, features modulares, excepciones y mappers del backend.

## Constitution Check (previo a diseño)

* **Principio I (Simplicidad y proporcionalidad):** PASS. Se utiliza la biblioteca estándar ArchUnit integrada en JUnit 5 existente sin dependencias runtime, bases de datos auxiliares ni herramientas externas pesadas.
* **Principio II (Seguridad e integridad):** PASS. No se introducen vectores de vulnerabilidad ni exposición de datos; el análisis es estático sobre bytecode en tiempo de pruebas.
* **Principio III (Coherencia de contratos):** PASS. Los contratos de reglas y sus aserciones quedan documentados en [contracts/architecture-rules.md](contracts/architecture-rules.md).
* **Principio IV (Trazabilidad y documentación viva):** PASS. La suite enlaza directamente los roles y restricciones de `docs/backend/architecture.md` con tests ejecutables que previenen la degradación del diseño.
* **Principio V (Sincronización continua de 4 vías):** PASS. Se sincronizan especificación (`spec.md`), diseño y contratos (`contracts/`), dependencias de Gradle y suites de pruebas en la misma feature.
* **Principio VI (Verificación y testing):** PASS. Las pruebas se clasifican estrictamente bajo `@Tag("unit")` cumpliendo `validateTestClassification`. El agente no ejecuta tests; el plan deja la guía de comprobación humana en `quickstart.md`.

## Investigación y decisiones

Las alternativas evaluadas y las decisiones de diseño se detallan en [research.md](research.md):
* **Librería seleccionada:** `archunit-junit5:1.4.1` gestionada a través de `backend/gradle/libs.versions.toml`.
* **Clasificación Gradle:** Anotación `@Tag("unit")` a nivel de clase contenedora para integración transparente en suites rápidas.
* **Modularidad:** Separación en 4 clases cohesivas en lugar de un único test monolítico.
* **Resiliencia ante variantes de empaquetado:** Soporte para `infraestructure` e `infrastructure`.
* **Aislamiento:** Uso de `ImportOption.DoNotIncludeTests.class` en todas las clases de análisis.

## Diseño y contratos

* **Modelo conceptual:** [data-model.md](data-model.md) describe las entidades abstractas (RolArquitectónico, ReglaArquitectura, ViolaciónArquitectura).
* **Catálogo de reglas:** [contracts/architecture-rules.md](contracts/architecture-rules.md) formaliza las reglas ejecutables para:
  * Restricciones de capas (`LayerArchitectureTest`): Presentación no accede a persistencia/infraestructura; Dominio no depende de capas web ni presentación; Orquestadores delimitados.
  * Ciclos de paquetes (`FeatureCyclesTest`): Grafo de paquetes `features.*` libre de ciclos.
  * Nomenclatura y anotaciones (`NamingAndAnnotationRulesTest`): Controladores terminan en `Controller` con `@RestController`; DTOs implementados como `record`; `@Repository` restringido a interfaces Spring Data.
  * Jerarquías transversales (`ExceptionAndMapperRulesTest`): Excepciones heredan de `FootballMarketException` sin `RuntimeException` genéricas directas; Mappers estáticos y `final`.
* **Guía de validación:** [quickstart.md](quickstart.md) documenta comandos de ejecución y prueba de inyección de violaciones.

## Comprobación constitucional posterior al diseño

* **Simplicidad y mínimo alcance:** PASS. La suite abarca estrictamente lo definido en el Issue #98 y las clarificaciones acordadas, sin añadir infraestructura redundante.
* **Integridad arquitectónica:** PASS. Las reglas refuerzan la autoridad de `docs/backend/architecture.md` y `docs/backend/conventions.md`.
* **Verificación:** PASS. Respeta la clasificación estática de Gradle y la política de no ejecución de tests por parte de agentes.

No existen violaciones que requieran Complexity Tracking.

## Estructura del proyecto

```text
specs/037-arch-tests/
├── spec.md
├── plan.md
├── checklists/
│   └── requirements.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── architecture-rules.md

backend/
├── gradle/
│   └── libs.versions.toml                     # Entrada para archunit-junit5
├── build.gradle.kts                           # Dependencia testImplementation(libs.archunit.junit5)
└── src/test/java/footballmarket/architecture/
    ├── LayerArchitectureTest.java             # Capas y dependencias de roles
    ├── FeatureCyclesTest.java                 # Ciclos entre features modulares
    ├── NamingAndAnnotationRulesTest.java      # Nombres, DTOs y @Repository
    └── ExceptionAndMapperRulesTest.java       # Excepciones y Mappers
```

**Decisión de estructura:**
Las pruebas se ubican en `backend/src/test/java/footballmarket/architecture/`, un paquete dedicado dentro del árbol de tests unitarios del backend, evitando dispersar reglas de arquitectura dentro de features específicas.

## Límites

Este plan documenta el diseño técnico y no constituye autorización para implementar. Cualquier implementación requiere una autorización humana explícita y la finalización previa de checklists y tareas (`/speckit.tasks`).

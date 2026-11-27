# Tasks: Tests de arquitectura en el backend con ArchUnit

**Input**: Artefactos de diseño de `specs/037-arch-tests/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/architecture-rules.md`, `quickstart.md`).

**Prerequisites**: `spec.md` (requisitos y US priorizadas), `plan.md` (diseño técnico), `checklists/architecture.md` (calidad de requisitos evaluada).

**Tests del proyecto**: La suite completa se compone de pruebas de arquitectura automatizadas en memoria bajo `@Tag("unit")`. La ejecución de tests corresponde al usuario; el agente no ejecuta tests.

**Workflow local**: Sigue `docs/development/sdd-workflow.md`. Las tareas se registran y gestionan exclusivamente en este archivo. La generación de este plan de tareas no autoriza la implementación automática; requiere Analyze OK y autorización humana explícita.

**Organization**: Tareas agrupadas por User Story para habilitar la implementación y verificación independiente de cada incremento.

---

## Phase 1: Setup (Infraestructura compartida de dependencias)

**Purpose**: Incorporar y configurar la biblioteca ArchUnit en el build del backend.

- [X] T001 [P] Declarar la versión y librería ArchUnit (`archunit = "1.4.1"` y `archunit-junit5`) en `backend/gradle/libs.versions.toml`
- [X] T002 Añadir la dependencia `testImplementation(libs.archunit.junit5)` en `backend/build.gradle.kts`

---

## Phase 2: Foundational (Prerrequisitos bloqueantes)

**Purpose**: Establecer el paquete base y la configuración compartida de importación para las pruebas de arquitectura.

**⚠️ CRITICAL**: Ninguna User Story puede comenzar hasta completar esta fase.

- [X] T003 Crear el paquete base de pruebas de arquitectura en `backend/src/test/java/footballmarket/architecture/`
- [X] T004 Definir la configuración de importación excluyendo clases de prueba (`ImportOption.DoNotIncludeTests.class`) y la clasificación estática `@Tag("unit")` requerida por Gradle

**Checkpoint**: Infraestructura base lista. La implementación de User Stories puede comenzar.

---

## Phase 3: User Story 1 - Verificación automática de capas y dependencias (Priority: P1) 🎯 MVP

**Goal**: Validar que los controladores no accedan a persistencia/infraestructura, que el dominio no dependa de frameworks web o presentación, y que los orquestadores no dependan de repositorios, integraciones u otros orquestadores.

**Independent Test**: Ejecutar `LayerArchitectureTest` y comprobar que las restricciones entre Presentation, Application, Domain, Persistence y Orchestrator se verifiquen en memoria.

### Implementation for User Story 1

- [X] T005 [US1] Crear la clase contenedora `LayerArchitectureTest` con anotación literal `@Tag("unit")` y `@AnalyzeClasses` en `backend/src/test/java/footballmarket/architecture/LayerArchitectureTest.java`
- [X] T006 [US1] Implementar la regla ArchUnit para verificar que ninguna clase en `..presentation..` dependa de clases en `..persistence..`, `..infrastructure..` o `..infraestructure..` en `backend/src/test/java/footballmarket/architecture/LayerArchitectureTest.java`
- [X] T007 [US1] Implementar la regla ArchUnit para verificar que las clases en `..domain..` no dependan de clases en `..presentation..` ni de `org.springframework.web..` en `backend/src/test/java/footballmarket/architecture/LayerArchitectureTest.java`
- [X] T008 [US1] Implementar la regla ArchUnit para verificar que las clases en `footballmarket.orchestrators..` no dependan de persistencia, integraciones ni de otros orquestadores en `backend/src/test/java/footballmarket/architecture/LayerArchitectureTest.java`

**Checkpoint**: MVP de verificación de capas completado y funcional de manera autónoma.

---

## Phase 4: User Story 2 - Detección y prevención de ciclos entre módulos (Priority: P1)

**Goal**: Garantizar la modularidad y el desacoplamiento mediante la detección automática de dependencias circulares directas o indirectas entre paquetes de features.

**Independent Test**: Ejecutar `FeatureCyclesTest` y comprobar que el grafo de dependencias entre paquetes `footballmarket.features.*` esté libre de nuevos ciclos y documente acoplamientos históricos en TD-041.

### Implementation for User Story 2

- [X] T009 [US2] Crear la clase contenedora `FeatureCyclesTest` con anotación literal `@Tag("unit")` y `@AnalyzeClasses` en `backend/src/test/java/footballmarket/architecture/FeatureCyclesTest.java`
- [X] T010 [US2] Implementar la regla ArchUnit utilizando `slices().matching("footballmarket.features.(*)..").should().beFreeOfCycles()` aislando dependencias legadas y validando modularidad en `backend/src/test/java/footballmarket/architecture/FeatureCyclesTest.java`

**Checkpoint**: Verificación de ciclos modulares completada y autónoma.

---

## Phase 5: User Story 3 - Cumplimiento de convenciones de nomenclatura, anotaciones y diseño (Priority: P2)

**Goal**: Validar convenciones de nomenclatura de controladores, DTOs como `record`, restricción de `@Repository` a interfaces Spring Data, jerarquía obligatoria bajo `FootballMarketException` y mappers estáticos `final`.

**Independent Test**: Ejecutar `NamingAndAnnotationRulesTest` y `ExceptionAndMapperRulesTest`, comprobando que todas las convenciones técnicas se cumplan sobre el código compilado.

### Implementation for User Story 3

- [X] T011 [P] [US3] Crear la clase contenedora `NamingAndAnnotationRulesTest` con anotación literal `@Tag("unit")` y `@AnalyzeClasses` en `backend/src/test/java/footballmarket/architecture/NamingAndAnnotationRulesTest.java`
- [X] T012 [US3] Implementar la regla ArchUnit para verificar que las clases anotadas con `@RestController` residan en `..presentation..` y terminen en `Controller` en `backend/src/test/java/footballmarket/architecture/NamingAndAnnotationRulesTest.java`
- [X] T013 [US3] Implementar la regla ArchUnit para verificar que las clases en paquetes `..dtos..` sean `record` en `backend/src/test/java/footballmarket/architecture/NamingAndAnnotationRulesTest.java`
- [X] T014 [US3] Implementar la regla ArchUnit para verificar que la anotación `@Repository` solo se aplique sobre interfaces en `backend/src/test/java/footballmarket/architecture/NamingAndAnnotationRulesTest.java`
- [X] T015 [P] [US3] Crear la clase contenedora `ExceptionAndMapperRulesTest` con anotación literal `@Tag("unit")` y `@AnalyzeClasses` en `backend/src/test/java/footballmarket/architecture/ExceptionAndMapperRulesTest.java`
- [X] T016 [US3] Implementar la regla ArchUnit para verificar que toda excepción propia herede de `FootballMarketException` en `backend/src/test/java/footballmarket/architecture/ExceptionAndMapperRulesTest.java`
- [X] T017 [US3] Implementar la regla ArchUnit para verificar que las clases `Mapper` sean `final`, no sean componentes Spring y expongan métodos estáticos en `backend/src/test/java/footballmarket/architecture/ExceptionAndMapperRulesTest.java`

**Checkpoint**: Convenciones de nombres, anotaciones, excepciones y mappers plenamente verificadas.

---

## Phase 6: User Story 4 - Fallo explícito e integración en suite rápida (Priority: P1)

**Goal**: Garantizar que ante una transgresión las pruebas fallen con mensajes de error claros y que la suite completa clasifique y ejecute de forma transparente en el pipeline de validación rápida.

**Independent Test**: Ejecutar `./gradlew validateTestClassification` y `./gradlew unitTest`, verificando que las 4 clases de arquitectura se reconozcan como pruebas unitarias sin levantar infraestructura pesada.

### Implementation for User Story 4

- [X] T018 [US4] Asignar mensajes descriptivos y claros (`as(...)` / `because(...)`) a todas las reglas ArchUnit de las 4 clases de prueba para asegurar reportes explícitos ante fallos en `backend/src/test/java/footballmarket/architecture/`
- [X] T019 [US4] Verificar la conformidad estática de clasificación JUnit contra la tarea `validateTestClassification` de `backend/build.gradle.kts`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Actualización documental y alineación normativa del repositorio.

- [X] T020 [P] Actualizar la tabla de dependencias en `docs/backend/technologies.md` documentando la versión y uso de `ArchUnit JUnit 5`
- [X] T021 [P] Actualizar `docs/backend/testing.md` agregando la descripción de la suite de pruebas de arquitectura bajo la categoría `@Tag("unit")`
- [X] T022 Validar los escenarios y comandos documentados en `specs/037-arch-tests/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 — BLOQUEA el inicio de las User Stories.
- **User Story 1 (Phase 3)**: Depende de Phase 2 — MVP de capas.
- **User Story 2 (Phase 4)**: Depende de Phase 2 — Independiente de US1.
- **User Story 3 (Phase 5)**: Depende de Phase 2 — Independiente de US1 y US2.
- **User Story 4 (Phase 6)**: Depende de la implementación de las reglas en US1, US2 y US3.
- **Polish (Phase 7)**: Depende de la finalización de las historias de usuario.

### Parallel Opportunities

- `T001` y `T002` pueden prepararse secuencialmente o en paralelo en archivos de build.
- `T011` (`NamingAndAnnotationRulesTest`) y `T015` (`ExceptionAndMapperRulesTest`) se ubican en archivos distintos y pueden desarrollarse en paralelo.
- `T020` y `T021` de documentación pueden redactarse en paralelo.

---

## Parallel Example: User Story 3

```bash
# Desarrollo paralelo de archivos independientes en US3:
Task: "T011 [P] [US3] Crear NamingAndAnnotationRulesTest en backend/src/test/java/footballmarket/architecture/NamingAndAnnotationRulesTest.java"
Task: "T015 [P] [US3] Crear ExceptionAndMapperRulesTest en backend/src/test/java/footballmarket/architecture/ExceptionAndMapperRulesTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Completar Phase 1 (Setup: ArchUnit en `libs.versions.toml` y `build.gradle.kts`).
2. Completar Phase 2 (Foundational: Paquete y opciones de exclusión).
3. Completar Phase 3 (US1: Reglas de capas y dependencias de roles en `LayerArchitectureTest.java`).
4. **Validar MVP**: Compilación y ejecución humana de `LayerArchitectureTest`.

### Incremental Delivery

1. Setup + Foundational → Base técnica disponible.
2. US1 → Reglas de capas funcionales (MVP).
3. US2 → Detección de ciclos modulares.
4. US3 → Convenciones de nombres, anotaciones, excepciones y mappers.
5. US4 → Diagnóstico explícito y validación estática de tags.
6. Polish → Sincronización de `docs/backend/technologies.md` y `docs/backend/testing.md`.

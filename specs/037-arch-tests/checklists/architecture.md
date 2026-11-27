# Architecture Checklist: Tests de arquitectura en el backend

**Purpose**: Validar la completitud, claridad, consistencia y calidad de los requisitos de la suite de pruebas de arquitectura con ArchUnit.
**Created**: 2026-11-27
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md)

**Note**: Este checklist personalizado revisa la calidad de los requisitos como «tests unitarios para las especificaciones»; no verifica la implementación en código.
**Review Ownership**: El checklist pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad del requisito está satisfecho.
**Marker Semantics**: `[x]` expresa que el criterio de calidad de la especificación ha sido evaluado y satisfecho; no significa que el código esté implementado.

---

## 1. Completitud y cobertura de capas y dependencias

- [x] CHK001 ¿Están explícitamente delimitadas las capas involucradas (presentación, aplicación, dominio, persistencia/infraestructura) y sus dependencias prohibidas sin omitir roles intermedios? [Completeness, Spec §FR-002, §FR-003, Contracts §1]
- [x] CHK002 ¿Se especifica con precisión cómo manejar las variantes de nomenclatura de paquetes de infraestructura (`infraestructure` vs `infrastructure`) para prevenir lagunas en la inspección? [Clarity, Edge Cases, Spec §Edge Cases, Research §4]
- [x] CHK003 ¿La prohibición de acceso desde `presentation` hacia persistencia o infraestructura define inequívocamente que solo la capa de aplicación (`application`) puede intermediar? [Clarity, Consistency, Spec §FR-002, US1]
- [x] CHK004 ¿El desacoplamiento del dominio respecto de frameworks web (`org.springframework.web.*`) y presentación está claramente acotado sin restringir las anotaciones JPA autorizadas por la arquitectura? [Consistency, Boundaries, Spec §FR-003, §Edge Cases]

## 2. Modularidad y ausencia de ciclos

- [x] CHK005 ¿El alcance de la detección de ciclos entre paquetes de features (`footballmarket.features.*`) define con claridad el nivel de granularidad del corte (*slice*) analizado? [Clarity, Measurability, Spec §FR-004, US2, Contracts §2]
- [x] CHK006 ¿Se prevé el comportamiento y mensaje descriptivo requerido ante la detección de un ciclo directo o transitivo entre features? [Coverage, Spec §FR-004, §FR-009]

## 3. Convenciones de nombres, anotaciones y diseño

- [x] CHK007 ¿Están definidos de forma bidireccional los requisitos de nomenclatura para controladores (sufijo `Controller` y anotación `@RestController`)? [Completeness, Clarity, Spec §FR-005, Contracts §3]
- [x] CHK008 ¿La restricción estructural de DTOs como `record` de Java está especificada sin ambigüedad respecto a los paquetes cubiertos? [Clarity, Spec §FR-006, Contracts §3]
- [x] CHK009 ¿La regla sobre la anotación `@Repository` delimita con precisión que su uso queda restringido exclusivamente a interfaces de Spring Data? [Completeness, Spec §FR-007, Contracts §3]
- [x] CHK010 ¿La jerarquía obligatoria de excepciones propias bajo `FootballMarketException` y la prohibición de lanzar excepciones genéricas de Java están claramente formuladas? [Completeness, Consistency, Spec §FR-010, Clarifications]
- [x] CHK011 ¿Los requisitos de diseño para clases Mapper (`final`, constructor privado, métodos estáticos, sin anotaciones Spring) están formalizados y sin margen de interpretación? [Clarity, Spec §FR-011, Clarifications]

## 4. Organización física, clasificación y rendimiento

- [x] CHK012 ¿La organización de la suite en 4 clases especializadas por dimensión dentro de `footballmarket.architecture` está unívocamente establecida? [Completeness, Spec §FR-012, Clarifications]
- [x] CHK013 ¿La clasificación estática bajo `@Tag("unit")` y la integración en tareas rápidas (`unitTest` / `fastTest`) de Gradle están especificadas conforme a las restricciones del build? [Traceability, Spec §FR-008, SC-005, Research §2]
- [x] CHK014 ¿La exclusión explícita de las clases de prueba (`ImportOption.DoNotIncludeTests.class`) está contemplada para evitar falsos positivos en el análisis estático? [Coverage, Spec §Edge Cases, Research §5]
- [x] CHK015 ¿El umbral de tiempo de ejecución (< 5 segundos) y la independencia de bases de datos y contenedores están especificados con métricas verificables? [Measurability, Spec §SC-003, §FR-008]

---

## Notes

- Los 15 ítems de revisión de calidad de requisitos han sido evaluados y confirmados contra la especificación (`spec.md`), el plan de implementación (`plan.md`) y el catálogo de reglas (`contracts/architecture-rules.md`).
- Este checklist valida la calidad y cobertura de las especificaciones de requisitos antes de proceder a la descomposición de tareas (`/speckit.tasks`).
- `/speckit.implement` lee el estado de los checklists y no modifica sus marcadores.

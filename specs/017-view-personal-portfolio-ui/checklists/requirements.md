# Specification Quality Checklist: Visualizar portfolio personal (REQ-S2-15)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-21
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

### Iteración 1 — 2026-10-21

El único ítem no aprobado fue _No [NEEDS CLARIFICATION] markers remain_: quedaban tres marcadores, el
máximo permitido por el comando, todos por decisiones funcionales con impacto en el alcance o en la
experiencia de uso, no por preferencias técnicas.

| Marcador | Requisito | Cuestión | Opciones presentadas |
| --- | --- | --- | --- |
| Q1 | FR-034 | En pantallas estrechas, ¿la retícula de *Mis tenencias* se readapta a menos columnas manteniendo seis fichas por página, o el tamaño de página se reduce para conservar tres columnas? | A: readaptar columnas, seis por página · B: reducir el tamaño de página |
| Q2 | FR-020 | ¿El panel de detalle se superpone a la grilla manteniéndola visible detrás, o sustituye el contenido de la pestaña? | A: superposición · B: sustitución |
| Q3 | FR-039 | La referencia visual muestra «Datos de ejemplo»; ¿se conserva, lo que exige un modo de demostración del portfolio, o se omite por tratarse de datos reales? | A: omitir · B: conservar con modo de demostración |

### Iteración 2 — 2026-10-21

Las tres decisiones se resolvieron con el responsable del proyecto: **Q1 = A**, **Q2 = A**,
**Q3 = A**. Los marcadores se retiraron de FR-034, FR-020 y FR-039 y las decisiones quedaron
registradas en la sección *Clarifications · Sesión 2026-10-21* de la especificación. Todos los ítems
del checklist quedan aprobados.

### Iteración 3 — 2026-10-21 — Clarify

La re-validación posterior a la fase Clarify no dejó ningún ítem en estado distinto: los dieciséis
ítems siguen aprobados y no se cambió ningún marcador. La especificación recibió dos decisiones
nuevas —DC-09, sobre el indicador de posiciones abiertas, y DC-10, sobre el registro verbal de
tuteo— y precisó SC-002, SC-003 y SC-011, que pasaron a ser verificables. El estado de la
especificación es `Ready for planning`.

El escaneo de Clarify detectó además cinco puntos que se resolvieron por evidencia, sin decisión
humana, y que quedaron registrados en la especificación: la correspondencia entre los rótulos del
filtro y los estados del backend (FR-016); la inactividad de todo el contenido de la pestaña
mientras el panel de detalle está abierto (FR-020); la presencia de los controles de paginación aun
con una sola página (FR-015); el costo de adquisición de una posición liquidada como cero real y
no como ausencia (casos borde); y la ausencia de instantánea compartida entre la consulta del
resumen y la consulta de la lista (casos borde y AC04). Además se normalizó la terminología a
«tarjeta» conforme a DC-03 y se vinculó el avatar predeterminado con el ícono genérico ya usado por
la aplicación.

### Aclaraciones sobre los ítems aprobados

- **Sin detalles de implementación.** Las únicas referencias técnicas del documento son el contrato
  de la consulta del portfolio —exigido por el propio issue #26— y las rutas de los artefactos de
  otras specs en la sección de trazabilidad. No se introduce ninguna decisión de diseño: la
  estructura interna, el enrutado, la biblioteca de estado, la estrategia de fetching y la
  implementación de los componentes quedan para la fase de diseño técnico.
- **Legible para las personas interesadas.** El documento describe qué ve la persona usuaria y qué
  significa cada dato. Los términos técnicos que sobreviven —centésimas de crédito, paginación por
  número de página, filtro de estado— son los del contrato funcional vigente y están definidos en
  la sección de supuestos.
- **Criterios de aceptación de todos los requisitos.** Los FR-003, FR-036, FR-037, FR-038 y FR-039
  son restricciones normativas y no comportamientos observables adicionales; su verificación es por
  revisión del artefacto y por los criterios de aceptación del issue de origen, no por un escenario
  de interacción adicional.
- **Cobertura del origen.** La sección «Cobertura del requerimiento de origen» mapea los nueve
  requisitos funcionales y los nueve criterios de aceptación del issue #26 a los requisitos,
  escenarios y criterios de esta especificación, sin requisitos del issue sin cobertura.
- **Sin duplicación con SPEC-014 ni con SPEC-015.** Esta especificación consume la consulta del
  portfolio y las reglas contables como fuentes externas: no redefine el contrato, no recalcula
  indicadores y no amplía el alcance de cálculo. Las exclusiones mutuas están declaradas en las
  secciones *Out of Scope* de las tres specs.

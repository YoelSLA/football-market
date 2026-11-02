# Checklist mínimo: organización por canales de SPEC-012

**Propósito**: evaluar únicamente la claridad y suficiencia del diseño del movimiento de archivos, antes de generar sus tareas.
**Creado**: 2026-11-01.
**Feature**: [SPEC-012](../spec.md). Diseño: [plan.md](../plan.md#estructura-objetivo-del-refactor-organizativo--2026-11-01).
**Alcance autorizado**: checklist mínimo solicitado por el usuario; sin ampliación funcional ni revisión global de la feature.

**Propiedad de revisión**: las casillas corresponden al revisor humano. `[x]` significa criterio documental revisado y satisfecho, no implementación ni tests completados. Generar el checklist no acredita su aceptación.

## Completitud y claridad

- [x] CHK001 ¿El diseño identifica inequívocamente origen y destino de los componentes exclusivos de scraping, exclusivos de JSON y compartidos, así como los elementos que no se moverán? [Completitud y claridad; Plan §Estructura objetivo y §Mapa de movimientos previsto]
- [x] CHK002 ¿El alcance está limitado explícitamente a movimientos de archivos y ajustes necesarios de paquetes, imports y referencias, preservando comportamiento, contratos y diferencias entre canales, sin extraer clases ni modificar algoritmos? [Consistencia; Plan §PLAN acotado y §Límite deliberado; Spec FR-001, FR-043 y FR-049]

## Verificabilidad

- [x] CHK003 ¿Están documentados los consumidores y tests afectados por los movimientos, el orden de lectura de ambos flujos y las comprobaciones posteriores, distinguiendo build sin tests de testing humano? [Verificabilidad; Plan §Mapa de movimientos previsto y §Orden de lectura de cada flujo; Quickstart §Revisión futura del refactor de estructura]

## Notas

- Selección mínima: calidad general del alcance y organización interna. No se generan checklists especializados de API, seguridad, datos o UX: el diseño excluye cambios a esas responsabilidades y conserva sus contratos.
- Los tres criterios remiten al diseño existente; no agregan requisitos ni escenarios funcionales. No quedan decisiones de alcance pendientes para redactar este checklist.
- Preparación realizada mediante el comando local `speckit.checklist`: prerrequisitos resueltos con `check-prerequisites.sh --json --template checklist-template` para SPEC-012. Sin `.specify/extensions.yml`, no hay hooks previos ni posteriores que despachar.
- Estado inicial: generado, pendiente de evaluación humana. No se modificaron los checklists históricos ni `tasks.md` durante su generación. No se ejecutaron tests ni builds.
- Evaluación humana del 2026-11-01: ante la solicitud explícita de aprobación de CHK001–CHK003, el usuario respondió «Apruebi»; las tres casillas aparecen marcadas por el revisor. La aprobación se limita al diseño documental y no aprueba implementación, tests ni cierre.
- `/speckit.implement` puede leer las casillas, pero no modificarlas. Avanzar a Tasks requiere registrar primero la evaluación de estos criterios.

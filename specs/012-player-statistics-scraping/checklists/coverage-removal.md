# Checklist mínimo: retiro de GET coverage

**Propósito**: revisar únicamente la calidad de requisitos del retiro y la preservación de ambos POST.
**Creado**: 2026-11-01.
**Feature**: [SPEC-012](../spec.md), cambio «retiro de la consulta HTTP de coverage».
**Propiedad de revisión**: casillas del revisor humano. `[x]` significa criterio documental satisfecho, no código implementado ni tests pasados. No reutiliza la aprobación del refactor organizativo.

## Alcance, contrato y verificabilidad

- [x] CHK001 ¿Está definido el retiro del GET, su resultado 404 autenticado y la conservación de seguridad global sin alias ni endpoint sustituto? [Claridad y compatibilidad; Spec §Cambio autorizado y FR-061; Contrato HTTP §Cambio objetivo autorizado]
- [x] CHK002 ¿Se distinguen los componentes exclusivos eliminables de los conteos internos conservados, manteniendo contratos de POST, `coverageComplete`, pendientes, reanudación y auditoría sin cambios? [Completitud y consistencia; Spec FR-014/FR-061/FR-066; Plan §Diseño mínimo; Data Model §Cambio objetivo]
- [x] CHK003 ¿El diseño identifica los tests afectados y cómo conservar sus garantías por fronteras válidas, sin borrar comprobaciones compartidas ni usar el GET retirado como observación? [Verificabilidad; Spec §Aceptación del cambio; Plan §Tests afectados y fronteras; Quickstart §Validación objetivo]

## Evaluación documental asistida (no aceptación)

**Evaluación humana posterior — 2026-11-01**: ante la solicitud explícita de aprobar estos tres criterios para continuar con Tasks y Analyze, el usuario respondió «Ya esta aprobado, podes continuar.». Las casillas ya se encuentran marcadas por el revisor. Se registra su aprobación documental de CHK001–CHK003; no autoriza implementación, CODE REVIEW, TESTING ni cierre.

Los tres criterios tienen soporte en las secciones referidas: contrato incompatible explícito, cuatro campos internos necesarios y separación de casos HTTP/Service/Repository/E2E. No se detectan decisiones funcionales pendientes en este escaneo. La aceptación corresponde al usuario antes de Tasks; no se marcan casillas en su nombre.

Se combina calidad general con compatibilidad de API y conservación de garantías de datos/seguridad en estos tres criterios, sin suites ni requisitos adicionales. No hay cambio UX, tecnológico ni de integraciones externas. Prerrequisitos de `speckit.checklist` resueltos con plantilla oficial; no existe `.specify/extensions.yml`, sin hooks que despachar. No se ejecutan tests ni build. Los checklists y reportes anteriores se conservan como antecedentes de sus respectivos alcances.

# IMPLEMENT AUTHORIZATION — SPEC-034

- **Fecha**: 2026-11-25.
- **Autorización humana**: usuario de la sesión autorizó explícitamente «Phase 1 y 2».
- **Alcance**: `tasks.md` Phase 1 y Phase 2, tareas T001–T005. No autoriza las historias ni fases posteriores, CODE REVIEW, TESTING ni cierre.
- **Base de preparación**: [`analyze-report.md`](analyze-report.md), con ANALYZE sin hallazgos ni blockers; `spec.md`, `plan.md`, `tasks.md` y checklists locales vigentes.
- **Vigencia**: contrastada antes de implementar con los artefactos locales actuales. Los cambios funcionales se limitan a las dependencias y a la infraestructura común de caché e invalidación definida para Phase 1–2.
- **Seguimiento operativo**: la issue #101 está abierta y no tiene elemento de GitHub Projects asociado al consultarla; no se afirma sincronización ni se modifica el seguimiento remoto.

## Autorización adicional — US1

- **Fecha**: 2026-11-25.
- **Autorización humana**: usuario de la sesión autorizó explícitamente «implementar US1».
- **Alcance**: US1, T006–T009 de `tasks.md`: pruebas e implementación de las cachés de ranking por score y de filtros, listado y detalle de jugadores. Incluye únicamente los prerrequisitos técnicos compartidos ya descritos en el registro anterior; no autoriza US2, US3, Phase 6, CODE REVIEW, TESTING ni cierre.
- **Base de preparación**: ANALYZE local vigente sin blockers; checklist `requirements.md` (30/30 marcados) y checklist `general.md` (8/8 marcados); spec, plan, contratos y tareas locales.
- **Vigencia**: autorizada para el estado local de los artefactos de SPEC-034 observado el 2026-11-25. Los cambios funcionales no pueden modificar requisitos ni exceder US1.

## Autorización adicional — US2

- **Fecha**: 2026-11-25.
- **Autorización humana**: usuario de la sesión autorizó explícitamente «implementar US2».
- **Alcance**: US2, T010–T013 de `tasks.md`: pruebas e implementación de las cachés del mercado oficial y P2P, incluyendo aislamiento por `viewerId` autenticado. No autoriza US3, Phase 6, CODE REVIEW, TESTING ni cierre.
- **Base de preparación**: ANALYZE local sin hallazgos ni blockers; `spec.md`, `plan.md`, `tasks.md`, contratos y checklists locales. Las tareas fundacionales T001–T005 están marcadas completas.
- **Vigencia**: autorizada para el estado local de los artefactos de SPEC-034 observado el 2026-11-25. Los cambios funcionales no pueden modificar requisitos ni exceder US2.
- **Seguimiento operativo**: no se modifica el seguimiento remoto en esta autorización.

## Autorización adicional — US3

- **Fecha**: 2026-11-25.
- **Autorización humana**: el usuario de la sesión autorizó explícitamente «implementar US3».
- **Alcance**: US3, tareas T014–T020 de `tasks.md`: pruebas e invalidaciones tras recálculo, sincronización y mutaciones de mercado completadas correctamente, más la comprobación de arranque con cachés vacías. No autoriza T021, CODE REVIEW, TESTING ni cierre.
- **Base de preparación**: ANALYZE local vigente sin hallazgos ni blockers; `spec.md`, `plan.md`, `tasks.md`, contratos y checklists locales. US1 y US2 y sus prerrequisitos compartidos aparecen completados en `tasks.md`.
- **Vigencia**: autorizada para el estado local de los artefactos de SPEC-034 observado el 2026-11-25. No autoriza cambios de requisitos ni alcance ajeno a US3.
- **Seguimiento operativo**: no se modifica el seguimiento remoto en esta autorización.

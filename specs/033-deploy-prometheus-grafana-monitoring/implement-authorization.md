# Autorización de implementación — SPEC-033

- **Fecha**: 2026-11-25.
- **Evidencia**: autorización explícita recibida en la conversación activa: «autorizo T001 y US1».
- **Alcance autorizado**: T001 y User Story 1, incluidos sus prerrequisitos explícitos. En `tasks.md`, esto habilita T001 y T002. No autoriza T003–T007 ni otras historias.
- **Límite**: reutilizar el endpoint existente de SPEC-032; no modificar el backend ni añadir instrumentación.
- **Vigencia**: aplica al alcance descrito en los artefactos vigentes de SPEC-033 para esta ejecución. No autoriza revisión de código, testing ni cierre de la feature.

## Autorización de implementación — US2 — 2026-11-25

- **Autorizó**: usuario, mediante instrucción explícita en la conversación activa.
- **Alcance autorizado**: User Story 2 de SPEC-033, incluidas sus tareas T003, T004 y T005, y sus prerrequisitos explícitos ya implementados. No autoriza US3 ni tareas ajenas a US2.
- **Evidencia comparativa**: instrucción del usuario «autorizo implementar US2»; requisitos de US2 en `spec.md` y tareas T003–T005 en `tasks.md`.
- **Límites**: dashboard y provisioning de Grafana para consultar únicamente métricas existentes de SPEC-032; no modificar instrumentación/backend, no ejecutar tests ni contenedores, y no avanzar a CODE REVIEW, TESTING o cierre.
- **Vigencia**: habilita únicamente IMPLEMENT de US2; no autoriza gates posteriores.

## Autorización de implementación — US3 — 2026-11-25

- **Autorizó**: usuario, mediante instrucción explícita en la conversación activa.
- **Alcance autorizado**: User Story 3 de SPEC-033, limitada a T006 y T007 de `tasks.md`; los prerrequisitos T001–T005 ya están implementados. No autoriza tareas ajenas a US3.
- **Evidencia comparativa**: instrucción del usuario «autorizo a implementar US3»; criterios de US3 en `spec.md`, tareas T006–T007 en `tasks.md` y ANALYZE sin bloqueos registrado en el issue #117 el 2026-11-25.
- **Límites**: bind local de Grafana con autenticación y sin secretos versionados, y actualización del inventario tecnológico; no modificar backend ni instrumentación, no ejecutar tests, builds ni contenedores, y no avanzar a CODE REVIEW, TESTING, CONVERGE o cierre.
- **Vigencia**: habilita únicamente IMPLEMENT de US3 conforme a los artefactos vigentes; no autoriza gates posteriores.

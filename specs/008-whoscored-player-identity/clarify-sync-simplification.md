# Clarificación del cambio de sincronización — 2026-11-01

Feature: [SPEC-008](spec.md). Rama: `refactor/008-whoscored-player-identity`.

## Identidad y operación

- Reanudación real con `create-new-feature.sh --json --reuse --type refactor --short-name 008-whoscored-player-identity`: `SPEC_ACTION=reused`, `BRANCH_STATUS=already-current`, `FEATURE_NUM=008`.
- `.specify/feature.json` releído: `specs/008-whoscored-player-identity`.
- `check-prerequisites.sh --json --paths-only` resuelve FEATURE_DIR y FEATURE_SPEC a SPEC-008. Su campo BRANCH devuelve el nombre normalizado sin prefijo; Git confirma la rama `refactor/008-whoscored-player-identity`.
- No existe `.specify/extensions.yml`; no hay hooks de extensión que ejecutar.

## Decisiones humanas incorporadas

El usuario autorizó modificar spec y código manteniendo `application`. Confirmó un endpoint que ejecute actual y después histórico; omitir histórico ante actual FAILED; prioridad global FAILED > PARTIAL > COMPLETED; eliminar persistencia dedicada a auditoría WhoScored; conservar el repositorio y flujo de casos pendientes del catálogo; devolver totales locales sin desglose por liga y contar cada jugador una sola vez.

Las categorías acordadas son con referencia, sin coincidencia y sin resolver. La referencia válida preexistente cuenta como con referencia; varias referencias contradictorias siguen siendo conflicto. El resultado histórico sustituye al provisional, no se suma como un jugador adicional.

## Escaneo de ambigüedades

| Categoría | Resultado | Base |
| --- | --- | --- |
| Alcance y comportamiento | Claro | FR-001, FR-011–012, FR-019–021; exclusiones FR-024. |
| Dominio y datos | Claro | Referencias permanentes; candidatos y resultados transitorios; unicidad FR-009/014. |
| Interacción | Claro | Una llamada síncrona y una respuesta global; relanzamiento seguro FR-026. |
| Calidad no funcional | Claro para este cambio | Autenticación existente, pacing y ausencia de transacciones durante red se preservan. No se añaden objetivos hipotéticos. |
| Integración | Claro | Mismo proveedor y paginación; actual primero y complemento sobre pendientes elegibles. |
| Errores y vacíos | Claro | FR-012/016; cero jugadores y cero elegibles; errores no se convierten en NO_MATCH. |
| Restricciones | Claro | Preservar estructura de application, revisión de catálogo y lógica ajena de imágenes. |
| Terminología | Claro | Totales de jugadores locales, no filas externas, ligas ni suma de pases. |
| Criterios verificables | Claro | US2 6–8 y SC-002/004/005/007 cubren el cambio. |
| Marcadores pendientes | Ninguno | Sin NEEDS CLARIFICATION ni decisiones funcionales nuevas pendientes. |

Cero preguntas adicionales: las decisiones materiales fueron respondidas en la conversación e incorporadas a spec.md. Rutas, nombres de campos y mecanismos internos se concretan en Plan.

## Impacto y siguientes fases

- Constitución y requisitos académicos consultados. La auditoría financiera del enunciado no se modifica; tampoco la observabilidad de servicios prevista en entregas. No se identifica una obligación externa de persistir estos candidatos o ejecuciones de identidad.
- Se preservan los IDs de US, FR y SC y las decisiones históricas expresamente identificadas. Las revisiones anteriores no acreditan este cambio.
- El checklist general conserva sus marcadores históricos; su aceptación para el nuevo alcance y la evaluación de checklists especializados siguen pendientes del revisor.
- Plan, research, modelo de datos, contratos y quickstart se actualizaron después de esta clarificación mediante sus secciones vigentes, preservando antecedentes históricos. El setup real de Plan resolvió SPEC-008 y conservó el plan existente. Constitution Check previo y posterior documentados en plan.md; sin hooks de extensión configurados.
- Se generó `checklists/sync-simplification.md` mediante la definición local de Checklist y su setup real; CHK041–048 permanecen sin marcar. No se atribuye evaluación en nombre del revisor. Checklists existentes preservan marcadores históricos, identificando su alcance anterior.
- La siguiente transición está detenida por revisión pendiente: antes de Tasks deben evaluarse CHK041–048 y los criterios de identidad afectados CHK004/022/028/029/040. No se ejecutaron Tasks, ANALYZE ni IMPLEMENT, ni se modifica código. La autorización de implementación registrada no sustituye esa revisión.
- Autorización de implementación expresada por el usuario para el alcance descrito, condicionada a la preparación normativa; CODE REVIEW, TESTING y cierre del nuevo alcance pendientes.
- Seguimiento remoto no consultado ni modificado; sincronización pendiente, sin atribuir un estado remoto.
- No se ejecutaron tests ni builds. Esta revisión documental no acredita aceptación funcional.

## Continuación posterior — preparación e implementación

- El usuario marcó CHK041–048 en `checklists/sync-simplification.md` y escribió «Listo, ya podes continuar con la implementacion». Se verificó por lectura el checklist antes de continuar, sin modificar sus marcadores. Se mantienen las reglas de identidad históricas y el complemento cubre sus cambios de captura, persistencia, elegibilidad y totales.
- Setup real de Tasks ejecutado; T058–T066 añadidas conservando T001–T057. ANALYZE se realizó read-only tras `check-prerequisites.sh --json --require-tasks --include-tasks`: spec, diseño vigente, contrato y tareas cubren los 29 FR y ocho SC; cero blockers, cero tareas nuevas sin requisito asociado. No se reutiliza la aprobación del alcance anterior.
- Alcance autorizado: implementación completa del cambio descrito por la sesión 2026-11-01 y sus prerrequisitos T058–T066. Comenzó al escribir los modelos transitorios y procesadores. No se autorizaron ni ejecutaron commit, push, cambios de ramas adicionales o actualización del seguimiento remoto.
- Implementación y verificación técnica documentadas en [reporte de revisión](code-review-sync-simplification.md). Este evento deja atrás el bloqueo de revisión pendiente registrado arriba; no modifica la evidencia histórica de ese bloqueo.
- Estado de seguimiento remoto no verificado ni modificado: sincronización operativa pendiente. CODE REVIEW, TESTING y cierre del cambio actual permanecen pendientes del usuario.

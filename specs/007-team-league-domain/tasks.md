---
description: "Tareas de implementación del dominio de equipos y ligas"
---

# Tasks: Dominio de equipos y ligas

**Input**: `specs/007-team-league-domain/` — spec, plan, research, data-model, contracts/api y quickstart.
**Prerequisites**: leer esos artefactos y las instrucciones de cada área antes de implementarla.
**Tests**: no se agregan tareas de tests: no se solicitó TDD ni suites nuevas explícitamente en la spec. La estrategia de comprobación del plan y las verificaciones del usuario siguen pendientes. El agente nunca ejecuta tests; builds solo según AGENTS del área y tras modificar su código.
**Organization**: historias por prioridad P1 (US1, US2, US3, US6), luego P2 (US4, US5). La numeración de historias conserva la spec.

## Format: `[ID] [P?] [Story] Description`

Todas las tareas comienzan sin completar. `[P]` permite trabajo independiente tras completar sus dependencias; no autoriza agentes paralelos automáticamente. Rutas relativas a la raíz. No crear proyectos, dependencias, endpoints o campos adicionales.

## Phase 1: Setup

- [ ] T001 Revisar instrucciones aplicables, implementaciones y consumidores; registrar alcance de Release 1/2 y verificaciones pendientes en specs/007-team-league-domain/tasks.md, consultando backend/AGENTS.md y frontend/AGENTS.md sin ejecutar tests.
- [ ] T002 Inventariar mappings, migraciones V1–V4 y configuración efectiva en backend/src/main/resources/db/migration/ y backend/src/main/resources/application.yml para preparar V5 sin incluir V6 en Release 1; conservar cambios previos.

## Phase 2: Foundational — prerrequisitos compartidos

- [ ] T003 Renombrar PlayerProvider a ExternalProvider en backend/src/main/java/footballmarket/models/ y actualizar todos sus consumidores, incluidos tests existentes, manteniendo FOOTBALL_DATA/THE_SPORTS_DB y EnumType.STRING sin cambios de valores persistidos o HTTP.
- [ ] T004 Crear V5__create_team_league_domain.sql en backend/src/main/resources/db/migration/ con tablas/secuencias League, Team, referencias, casos, intentos y catalog_transition; añadir players.team_id nullable con FK/índice, preservar legacy NOT NULL y establecer las restricciones de data-model.md; no crear V6 todavía.
- [ ] T005 [P] Implementar League y sus referencias externas en backend/src/main/java/footballmarket/models/ y sus repositorios en backend/src/main/java/footballmarket/repositories/, con nombre obligatorio y propiedad externa inmutable.
- [ ] T006 Implementar Team, referencias, asociación obligatoria a League y current explícito en backend/src/main/java/footballmarket/models/ y repositorios en backend/src/main/java/footballmarket/repositories/, conservando IDs ante cambios.
- [ ] T007 [P] Implementar PendingReviewCase sin status en backend/src/main/java/footballmarket/models/ y acceso interno en backend/src/main/java/footballmarket/repositories/: claves canónicas UNIQUE, categorías/causas tipadas, primera/última detección y evidencia JSONB, nunca nombre o descripción como identidad.
- [ ] T008 [P] Implementar TeamNameNormalizer puro en backend/src/main/java/footballmarket/models/ con trim, diacríticos, case independiente de locale y espacios colapsados, sin quitar sufijos, aliases o fuzzy; no cambiar la tolerancia del matcher de jugadores.
- [ ] T009 Adaptar Player en backend/src/main/java/footballmarket/models/Player.java a Team nullable para transición/casos permitidos y mappings legacy temporales; derivar liga de Team y mantener IDs/referencias/active sin duplicar fuente de verdad.

## Phase 3: US1 — Reconocer equipo y liga (P1)

**Goal**: consulta paginada con entidades internas y consumidores adaptados.
**Independent verification**: con datos válidos persistidos, GET devuelve solo activos con Team vigente/League válida, cuatro campos no nulos, sin team/league; vacío conserva totales cero. Cliente muestra clasificación por nombre o neutral.

- [ ] T010 [US1] Actualizar la consulta y carga de asociaciones en backend/src/main/java/footballmarket/repositories/PlayerRepository.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: active + Team.current + League válida, orden/paginación existentes y sin lazy loading fuera de lectura transaccional.
- [ ] T011 [US1] Sustituir team/league por teamId/teamName/leagueId/leagueName en DTO y mapper del catálogo en backend/src/main/java/footballmarket/controllers/, manteniendo campos opcionales, JWT, errores y contrato paginado existentes.
- [ ] T012 [P] [US1] Adaptar DTO/modelo y mapper de frontend/src/features/players/types/dtos.ts, frontend/src/features/players/types/models.ts y frontend/src/features/players/players.mapper.ts al contrato nuevo, sin campos visuales ni mapas por IDs.
- [ ] T013 [US1] Adaptar frontend/src/features/players/components/PlayerCard/PlayerCard.tsx y frontend/src/features/players/constants/playerClassifications.ts a teamName/leagueName, preservando clasificación por nombre y fallback neutral existente incluso tras rename desconocido.

## Phase 4: US2 — Sincronizar ligas y equipos (P1)

**Goal**: foto completa previa y aplicación local atómica con casos individuales.
**Independent verification**: una foto controlada crea entidades/referencias/asociaciones en un commit; foto incompleta no escribe y fallo técnico revierte todo; presente inválido conserva estado y presencia.

- [ ] T014 [US2] Adaptar configuración de ligas en backend/src/main/java/footballmarket/ y backend/src/main/resources/application.yml de cinco códigos fijos a lista no vacía sin duplicados, manteniendo configuración inicial y validación efectiva.
- [ ] T015 [US2] Ampliar backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java y records de backend/src/main/java/footballmarket/models/ para obtener competición/equipos/planteles requeridos y construir foto inmutable antes de escritura, distinguiendo respuesta vacía válida, datos opcionales y respuestas incompletas.
- [ ] T016 [US2] Implementar clasificación de presentes/procesables/protegidos en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: identidad insuficiente que compromete presencias aborta foto; invalidez identificable registra causa y protege entidad/dependientes sin fallback aproximado.
- [ ] T017 [US2] Implementar upsert funcional de casos en backend/src/main/java/footballmarket/services/impl/ con Repository de casos: categorías del modelo, evidencia de relaciones previas/recibidas y propietarios; preservar original/primera detección, actualizar última/evidencia actual y reconciliar sujeto externo a interno sin duplicar.
- [ ] T018 [US2] Aplicar League/Team/Player y referencias FOOTBALL_DATA mediante un único intento transaccional en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java, resolver presentes válidos por referencias y conservar asociados inválidos; dejar un punto interno para backfill/marcador en el mismo commit, sin HTTP.
- [ ] T019 [US2] Implementar rollback/reaplicación acotada de foto ante conflictos reconocidos en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: clasificar SQLSTATE/constraint, excluir/proteger nuevo conflicto y registrar en intento válido; propagar otros fallos, reiniciar contadores y no reutilizar entidades de transacción fallida.
- [ ] T020 [US2] Adaptar backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java a coordinación opaca sin lógica de dominio, conservando exclusión de ejecuciones existente y los cinco contadores de jugadores; no agregar contador de casos ni convertir fallos técnicos en descartes exitosos.

## Phase 5: US3 — Identidad ante cambios (P1)

**Goal**: cambios de nombres, transferencias y liga sin nuevas identidades.
**Independent verification**: segunda foto renombra/mueve Team y transfiere Player preservando IDs y referencias; una identidad ajena nunca se reasigna. leagueName cambia sin promesa visual histórica.

- [ ] T021 [US3] Completar actualizaciones por referencia de propietario en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: rename de League/Team, cambio Team→League y Player→Team por foto válida; no resolver identidad por texto ni conservar equipo anterior ante transferencia válida.
- [ ] T022 [US3] Aplicar invariantes de propietario inmutable y unicidad en referencias de backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/repositories/, diferenciando actualizar propietario de asignar a otro y registrando evidencia de conflicto sin reasignar.
- [ ] T023 [US3] Adaptar backend/src/main/java/footballmarket/models/PlayerIdentityMatcher.java y consumidores de backend/src/main/java/footballmarket/services/ a Team.name, manteniendo reglas de imágenes y filtrando jugadores sin Team para evitar null dereference sin añadir sincronización de imágenes a 007.

## Phase 6: US6 — Migrar sin perder jugadores (P1)

**Goal**: transición completa y evidencia persistida con convivencia segura.
**Independent verification**: presentes válidos usan foto aunque legacy difiera; ausentes sin asociación usan matching estricto; presentes inválidos no usan fallback. IDs se conservan, casos deduplican y nuevas altas cumplen legacy NOT NULL. Limpieza se verifica solo después del gate de Release 1.

- [ ] T024 [US6] Integrar backfill en la transacción principal de backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java solo para ausentes preexistentes sin Team y transición pendiente; una coincidencia con Team vigente asocia, cero/varias registran original/candidatos sin liga textual y sin cambiar active por el backfill.
- [ ] T025 [US6] Implementar espejo legacy temporal en backend/src/main/java/footballmarket/models/Player.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: altas/asociados escriben nombres derivados para NOT NULL; no asociados conservan original y casos capturan evidencia antes de sobrescribir; GET nunca usa espejo.
- [ ] T026 [US6] Implementar mapping y actualización de catalog_transition en backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: marcar en el mismo commit solo si cada Player tiene asociación válida o caso permitido con evidencia; tras finalización no repetir backfill desde casos.
- [ ] T027 [P] [US6] Ajustar consultas SQL y procedimiento de solo lectura en specs/007-team-league-domain/quickstart.md al esquema implementado, con rol separado autorizado, sin endpoint/comando/status ni credenciales generales de aplicación.
- [ ] T028 [US6] Registrar en specs/007-team-league-domain/tasks.md el gate pendiente del usuario: desplegar Release 1 con solo V5, ejecutar sincronización y verificar IDs/conteos/asociaciones/evidencia/marcador; detener implementación de T039–T040 hasta confirmación explícita, sin marcar este gate satisfecho por existencia de código.

## Phase 7: US4 — Identidad TheSportsDB no bloqueante (P2)

**Goal**: enriquecimiento independiente posterior al commit con confianza y retries correctos.
**Independent verification**: fixtures con completitud acreditada y único match permiten referencia; searchteams.php sin evidencia de completitud no asigna. Fallos técnicos permiten siguiente sync y no revierten principal; evaluación válida consume nombre, referencia resuelta no se reevalúa.

- [ ] T029 [P] [US4] Implementar TeamResolutionAttempt y Repository en backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/repositories/, separando última llamada/resultado técnico de última evaluación válida/nombre y retry_not_before.
- [ ] T030 [US4] Adaptar búsqueda de equipos en backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java: candidatos Soccer/nombre principal, identidad externa y evidencia de completitud; searchteams.php sin prueba retorna completitud no acreditada, sin asumir Premium ni unicidad por cantidad.
- [ ] T031 [US4] Implementar evaluación estricta y Service de enriquecimiento en backend/src/main/java/footballmarket/services/ y backend/src/main/java/footballmarket/services/impl/: deduplicar identidades coherentes, rechazar duplicados contradictorios, distinguir MATCH/NO_MATCH/AMBIGUOUS/COMPLETENESS_UNPROVEN, sin primer resultado ni fuzzy.
- [ ] T032 [US4] Implementar elegibilidad, pacing y Retry-After en el Service de backend/src/main/java/footballmarket/services/impl/: una tentativa lógica por Team/sync sin retry interno nuevo, fallos técnicos no consumen evaluación, respuesta válida consume nombre y referencia resuelta excluye consultas.
- [ ] T033 [US4] Persistir intento y eventual referencia/caso atómicamente por Team en backend/src/main/java/footballmarket/services/impl/, con red fuera de escritura y revalidación de nombre/propietario; descartar resultados obsoletos sin consumir nombre nuevo, rollback corto ante fallo.
- [ ] T034 [US4] Invocar enriquecimiento después del commit principal desde backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java sin inspeccionar entidades ni cambiar respuesta confirmada; diagnosticar fallos independientes y permitir enriquecimiento parcial entre Teams.

## Phase 8: US5 — Retirada y regreso (P2)

**Goal**: vigencia explícita y actividad con protecciones, sin eliminación física.
**Independent verification**: ausentes confirmados salen, protegidos conservan estado y regreso válido conserva identidad/reactiva; foto incompleta no altera estados. Un Player protegido activo con Team no vigente no aparece en GET.

- [ ] T035 [US5] Recalcular Team.current al aplicar foto completa en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: presentes válidos vigentes, ausentes no protegidos retirados y protegidos con estado previo; configuración sola no escribe ni League recibe current.
- [ ] T036 [US5] Completar activación/reactivación/inactivación en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java según presentes/protegidos/ausentes y Team retirado, proteger planteles previos indeterminables, conservar active de inválidos y nunca borrar físicamente entidades/referencias.

## Phase 9: Polish & Cross-Cutting — entrega en dos releases

- [ ] T037 Actualizar anotaciones OpenAPI y fuentes REST Docs/consumidores existentes en backend/src/main/java/footballmarket/controllers/ y backend/src/test/java/footballmarket/ al contrato de specs/007-team-league-domain/contracts/api.md, sin editar snippets generados ni agregar suites nuevas/campos de revisión o clasificación.
- [ ] T038 Revisar Release 1 y documentar comprobaciones pendientes en specs/007-team-league-domain/quickstart.md y specs/007-team-league-domain/tasks.md: escenarios de cada historia, compatibilidad, SQL, controles backend del usuario y builds permitidos tras código; no ejecutar tests ni declarar controles sin evidencia.
- [ ] T039 Solo tras confirmación explícita del gate T028 y para Release 2, crear backend/src/main/resources/db/migration/V6__drop_player_team_league_text.sql transaccional PostgreSQL: validar marcador/integridad/asociación o caso permitido con original, luego eliminar columnas; fallos dejan esquema intacto, sin DDL no transaccional ni V6 en Release 1.
- [ ] T040 Solo junto con V6 en Release 2, retirar mappings/lecturas/escrituras legacy de backend/src/main/java/footballmarket/models/Player.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java; mantener Team nullable para casos permitidos y evidencia en casos, sin fallback desde evidencia histórica.
- [ ] T041 Finalizar procedimiento operativo en specs/007-team-league-domain/quickstart.md: backup consistente, exclusión de escritores antiguos, binario final compatible, manejo de fallos Flyway y forward-only; registrar revisión final de diff/status y controles pendientes en specs/007-team-league-domain/tasks.md sin prometer downgrade o restauración realizada.

## Dependencies & Execution Order

- Setup → Foundational → US1 → US2 → US3 → US6 → US4 → US5 → cierre de Release 1.
- US1 verifica consulta con datos controlados; catálogo real completo requiere US2/US6. No desplegar una historia aislada como Release 1 funcional completo.
- US3/US6 dependen de aplicación US2. US4 y US5 dependen de US2/US3; comparten catálogo/orchestrator con otras tareas y se secuencian para evitar conflictos. US6 backfill usa el conjunto vigente/protegido completado por US5: verificarlo antes del gate.
- T004 antecede mappings/repositorios; T005 antecede T006; T007/T008 pueden hacerse en paralelo con T005. T012 puede adelantarse tras contrato conocido, pero T013 requiere T012 y validación final T011.
- T027 puede hacerse en paralelo con T024–T026 una vez fijado esquema. T029 puede hacerse en paralelo con T030 tras foundation; T031 necesita ambos.
- T028 registra y controla el gate, no ejecuta operación por el usuario. T039/T040 están bloqueadas hasta gate humano confirmado, Release 1 completo y revisión T038. V5 y V6 jamás se distribuyen en el mismo release.

## Parallel Execution Examples per Story

- **US1**: T011 backend y T012 frontend usan contrato común y archivos distintos; T013 espera tipos nuevos.
- **US2**: no paralelizar tareas de Service compartido; inventario de configuración/contratos puede revisarse independientemente, implementación T014–T020 secuencial.
- **US3**: no paralelizar T021/T022: invariantes y aplicación se revisan juntas; T023 después de estabilidad del modelo.
- **US6**: T027 documentación SQL en paralelo con T024–T026 código; no ejecutar gate automáticamente.
- **US4**: T029 persistencia y T030 integración en paralelo; T031–T034 secuenciales.
- **US5**: T035/T036 secuenciales por archivo/transacción compartidos; revisión de escenarios puede hacerse separadamente, sin ejecución de tests por el agente.

## Implementation Strategy

**MVP técnico**: foundation + US1 para consulta con asociaciones controladas. **Primer incremento desplegable**: todas las historias de Release 1 con V5, convivencia, atomicidad/protecciones y transición; TheSportsDB puede abstenerse válidamente por completitud no acreditada.

Completar Release 1 → usuario sincroniza/verifica → aprobación explícita del gate → implementar/publicar Release 2 con V6 y mapping final. No inferir aprobación por checklist de calidad ni build. Cada historia se verifica por sus criterios independientes y escenarios de quickstart; ninguna casilla se completa solo por diseño. Mantener trazabilidad y cambios previos; no renombrar rama automáticamente.

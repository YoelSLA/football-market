---
description: "Tareas de implementación del dominio de equipos y ligas"
---

# Tasks: Dominio de equipos y ligas

## Estado vigente — US01–US06 en TESTING

CODE REVIEW aprobado por humano (checklist completo, implementación y tests) con evidencia
persistida por US. US01–US06 en TESTING con estado `PENDING HUMAN VALIDATION`. Ninguna US es DONE,
por lo que la Feature permanece IN PROGRESS. T039/T040/T052 (V6) siguen sin autorizar ni iniciar;
el gate T028 continúa pendiente del usuario. Un cambio de código o tests con impacto sobre el
alcance aprobado invalida CODE REVIEW y TESTING correspondientes.

## Antecedente — cierre técnico de Release 1 completado

Todas las tasks técnicas autorizadas de Release 1 (T001–T038, T041–T051) están marcadas `[x]`
tras contraste con su definición exacta. Ninguna quedó abierta esperando un gate humano: las
tareas técnicas terminan con fuentes escritas, no con ejecución de tests. T039/T040/T052
pertenecen a V6 y permanecen `[ ]`.

Trabajo de cierre de esta iteración: TEAM/LEAGUE de propiedades inmutables y una referencia por
proveedor con rechazo de sustitución (T005/T006/T022); asociación Player→Team nullable con
espejo legacy y liga derivada (T009/T025); consulta vigente con Team.current y carga de
asociaciones (T010); DTO/Mapper de cuatro campos internos (T011); clasificación integral por
propietario antes de mutar Team/name/position (T016); upsert de casos con reconciliación
externo→interno y preservación de evidencia original (T017); commit principal único con
backfill y marcador (T018/T024/T026); reaplicación acotada por SQLSTATE con exclusiones por
tipo y reconstrucción de candidatos desde la foto original (T019); configuración de competiciones
como lista no vacía sin duplicados (T014); consolidación
de opcionales por atributo B-V5-05; enriquecimiento TheSportsDB con red fuera de escritura,
revalidación, replay acotado y diagnóstica independiente (T029–T034); vigencia, retirada y
reactivación sin borrado físico (T035/T036); OpenAPI y REST Docs alineados (T037); procedimiento
operativo Release 1 documentado (T041).

Cobertura fuente de cierre: `PlayerTest`, `TeamLeagueTest`, `TeamResolutionAttemptTest`,
`PendingReviewCaseTest`, `PlayerPresentationTest`, `PlayerTeamAssociationTest`,
`PlayerIdentityMatcherTest` (models); `TeamLeagueMigrationTest` (V1–V4→V5, unicidades, evidence
JSONB, fechas, intentos), `PlayerCatalogPersistenceTest` (query real, carga fuera de
transacción, orden/paginación, casos históricos, atómicidad y concurrencia),
`PlayerSnapshotConsistencyPersistenceTest` (B-V5-01–05), `TeamCatalogTransitionPersistenceTest`
(backfill, marcador, espejo, precedencia foto/legacy, rollback conjunto); `PlayerCatalogServiceTest`,
`TeamIdentityAndRetirementTest`, `TeamEnrichmentServiceTest` (servicios);
`FootballDataIntegrationTest`, `TheSportsDbIntegrationTest` (adaptadores);
`PlayerSynchronizationOrchestratorTest`; `PlayerControllerTest` (contrato HTTP, JWT, 400,
persistencia exacta de presentación).

Verificación ejecutable pendiente del usuario: tests backend y builds permitidos. No se ejecutó
ninguno en esta iteración; el build histórico no valida el diff actual. Ninguna US se mueve a
CODE REVIEW hasta que el reporte de revisión cubra implementación y tests de esa US.

## Antecedente — B-V5-05 resuelto

**ANALYZE posterior read-only: PASS WITH NON-BLOCKING.** B-V5-05 queda cubierto por T009/T016/T021
y T045/T046/T048; el registro usa la categoría PLAYER_OPTIONAL_CONFLICT con causas estables
CONFLICTING_DATE_OF_BIRTH/CONFLICTING_NATIONALITY y evidencia por atributo, sin alterar
INVALID_SUBJECT_DATA, Team/name/position, HTTP, proveedores ni V5/V6. Sin nueva ambigüedad
detectada; READY recuperado y Status Ready for implementation. Autorizaciones US01–US06
preservadas sin ampliación material. Ningún gate humano aprobado.

Decisión humana incorporada en spec/plan/data-model/contrato/quickstart: los opcionales se
consolidan por atributo e independientemente del estado obligatorio. Sin valor válido no hay
valor nuevo; único valor válido se usa; mismo valor repetido se usa; valores incompatibles
conservan el persistido del Player existente o null en el nuevo, sin first/last wins, externalId
ni orden de colección. Un conflicto no descarta, congela ni invalida al Player, no impide
consolidar el otro atributo y no bloquea el enriquecimiento de Team. Sin nuevas prioridades
entre proveedores.

**IMPLEMENT fuente:** Service agrupa observaciones válidas por propietario y consolida
dateOfBirth y nationality por separado antes de mutar; el caso se registra con clave estable por
sujeto y atributo y se reconcilia con el sujeto interno. V5 admite la nueva categoría en el
check de dominio. Fuentes: regresiones de opcionales en PlayerSnapshotConsistencyPersistenceTest
(valor persistido ante conflicto en ambos órdenes, independencia entre atributos, recuperación
posterior); suites de opcionales válidos/repetidos ausentes ya cubiertas en PlayerCatalogServiceTest
y PlayerCatalogPersistenceTest; consulta GET en PlayerControllerTest. Fuentes escritas, no
compiladas ni ejecutadas. T004 sigue pendiente de validación integral y el cierre de V5 continúa
sin mover ninguna US a CODE REVIEW.

## Antecedente — STOP IMPLEMENT por B-V5-05

Nueva decisión funcional pendiente, no parada por progreso parcial: observaciones de
referencias distintas del mismo Player con Team/name/position coherentes pueden traer
dateOfBirth/nationality válidos diferentes. synchronizeCandidates/updatePlayer aplica
updateOptionalDetails por cada referencia y deja el último valor según orden. SPEC-002
RF-018 define actualización válida/conservación ante ausencia y prohíbe descarte por
problema solo opcional; prioridad entre referencias distintas no está definida. B-V5-03
enumera Team/name/position; B-V5-04/ranking se limitan a presentación de name/position.
No implementar selección, conservación por campo ni protección integral por analogía.
Decisión solicitada: regla para opcionales válidos incompatibles del mismo sujeto en una foto.

READY NO; último ANALYZE PASS WITH NON-BLOCKING antecede este hallazgo y no habilita
continuación mientras esté abierto. No se ejecutó un nuevo ANALYZE ni se afirma un resultado.
Status Draft; Feature y US01–US06 IN PROGRESS. Autorizaciones preservadas, sin ampliación
material establecida; evaluar impacto tras decisión humana. B-V5-01–04 no se reabren; H1–H4
y gates humanos siguen pendientes. Impacto potencial T016/T018/T021/T045–T048/T051.

**Trabajo de esta continuación (fuentes, sin ejecución):** revisión/inventario T001/T002;
revisión de modelos/query/DTO/consumidores y marcado de T005–T007/T009–T011/T013/T014.
Replay reconstruye presentaciones desde foto original y reconoce UNIQUE de League/Team
mediante SQLSTATE/tabla/constraint con exclusión incremental; conflictos Player dejan caso.
Reconocimiento posterior reconcilia sujeto externo de casos. Enrichment reaplica únicamente
conflicto de identidad conocido en transacción nueva sin repetir red. Adaptador valida límites
de texto recibidos y errores no 2xx de searchTeams. OpenAPI refleja GET y POST vigentes.
Fuentes ampliadas: PlayerTest, TeamLeagueTest, TeamLeagueMigrationTest (opcionales legacy,
constraints de casos/intentos), PlayerCatalogPersistenceTest (query/carga/paginación),
PlayerImageSynchronizationServiceTest (fixtures de identidad V5), TeamEnrichmentServiceTest
(evaluación válida, retry técnico, rename semántico y enriquecimiento parcial).
T004 sigue pendiente: no sustituir validación integral de esquema/mappings por existencia
de SQL o build histórico. Ninguna US tiene aún completo todo el alcance técnico/tests;
no se mueve a CODE REVIEW. Tests/builds/migraciones/hooks no ejecutados, V6 no iniciado.

Las secciones de estado que siguen son antecedentes; prevalece esta sección.

**Entrega de esta continuación:** revisión de diff/stat/status y `git diff --check` sin errores
de whitespace. Estado de trabajo conservado, sin commit/push ni cambios normativos nuevos.
Trello: comentario de B-V5-05/avance persistido y verificado en Feature y seis US, listas intactas.
Verificación ejecutable actual de código, tests y migraciones pendiente del usuario; no se usa
el build histórico como evidencia. Siguiente paso humano: decidir opcionales incompatibles;
después actualizar artefactos afectados y ejecutar ANALYZE real, sin reabrir B-V5-01–04.

## Estado vigente — B-V5-04 y ranking resueltos

### Continuación autorizada — revisión de tareas implementadas

T005/T006/T007/T009 y T010/T011 están implementadas en fuentes: entidades/referencias
inmutables, repositorios, JSONB/fechas, asociación nullable/espejo, consulta con joins y DTO/Mapper.
T013 conserva clasificación por nombre/fallback, y T014 elimina la cardinalidad fija de configuración.
Se marcan esas tareas técnicas tras inspección, no como evidencia de ejecución. T004 permanece
pendiente de validación íntegra de esquema/mappings/cobertura T043, no se completa por existencia.
T042–T051 se mantienen independientes de la implementación productiva: solo se marcarán con
su cobertura completa. Builds/tests/migraciones actuales no ejecutados; históricos no validan el diff.
Revisión en curso: replay reconstruye candidatos desde foto original y persiste conflictos de Player;
regresión de query real añadida para filtro/carga fuera de transacción/orden/paginación/casos históricos.

**ANALYZE posterior read-only: PASS WITH NON-BLOCKING.** Política coherente en spec/plan/modelo/
contrato/tasks: T009/T042 cubren representación, T016/T018/T021/T045–T048 clasificación/ranking,
T011/T044 GET persistido. Sin nueva decisión funcional detectada en esta revisión. READY recuperado;
Status Ready for implementation, Feature/US IN PROGRESS; IMPLEMENT AUTHORIZATION preservadas.
Hallazgos/estados bloqueados anteriores se conservan como antecedentes, no preparación vigente.

**IMPLEMENT fuente:** PlayerPresentation separa equivalencia y ranking; Player.update conserva
name/position equivalentes. Consolidación de referencia reúne presentaciones equivalentes con
ranking asociativo independiente del orden; grupos por propietario clasifican Team/name/position
antes de mutar Player y seleccionan la forma canónica solo si todo el grupo es coherente.
CONFLICTING_PLAYER_STATE registra discrepancias textuales; conflictos Team mantienen causa
CONFLICTING_PLAYER_TEAMS y clave existente por sujeto para no duplicar clasificación por atributo.
Marcador/lectura SQL admiten excepción delimitada con causa/texto original/observaciones.
Fuentes PlayerPresentationTest, persistencia de consistencia, Service alta ambos órdenes,
consolidación FootballData y Controller GET exacto agregadas/ampliadas. No compiladas/ejecutadas;
no builds/migraciones ni V6. T004 y resto de cierre integral V5 siguen pendientes; no completar
tasks de mayor alcance solo por esta porción, ni aprobar CODE REVIEW/TESTING/DONE.

Decisión humana incorporada: comparación semántica separada de presentación original;
existente conserva name/position persistidos equivalentes. Alta/significado nuevo selecciona
original por diacríticos preservados, casing natural/mixto y desempate lexicográfico determinista,
nunca orden ni clave normalizada. B-V5-01–04 resueltos; los bloqueos anteriores son históricos.
Impacto dentro de T009/T016/T018/T021 y T042/T044–T048: ranking puro Model, conservación en
Player, clasificación integral antes de mutar propietario, consolidación de formas equivalentes
sin perder observaciones y HTTP devuelve original persistido. Autorizaciones US01–US06 preservadas
por ausencia de ampliación material; sin nueva US/proveedor/HTTP/V6 ni aprobaciones de gates.
Cobertura: única representación, diacríticos, casing, varios equivalentes, permutaciones,
desempate, conservación existente name/position, contradicción semántica y GET exacto.
ANALYZE posterior obligatorio antes de continuar producto; no tests/builds/migraciones ejecutados.

## Estado vigente — B-V5-03 resuelto; B-V5-04 pendiente

**ANALYZE posterior: BLOCKED.** Análisis read-only de Constitution, spec/clarificaciones,
plan/data-model/contratos y tasks: B-V5-03 tiene cobertura en T016/T018/T021/T045–T048/T051;
la conservación transitoria alcanza marcador/lecturas de control T026/T027 sin iniciar V6.
Finding B-V5-04 HIGH: camino coherente no determina presentación de name/position cuando hay
formas distintas equivalentes por normalización. No seleccionar ni alterar textos de presentación
automáticamente. READY NO; no invocar el PASS anterior para continuar. Registro separado del
análisis; no se corrigió producto dentro de ANALYZE ni se ejecutaron controles de código.

Decisión humana B-V5-03 incorporada en spec/plan/data-model/contrato/quickstart: contradicción
de Team/name/position entre observaciones válidas del mismo Player en la misma foto protege
integralmente al sujeto como INVALID_SUBJECT_DATA. Normalización textual estricta existente solo
para comparación, sin aliases/fuzzy/reglas nuevas. Todas las referencias se conservan; sin
actualización parcial ni invalidez permanente; demás Players continúan. B-V5-01/B-V5-02 vigentes.

Cobertura agregada a T045/T046/T047/T048/T051, sin renumeración: (1) Team/name/position coherentes
procesan normal; (2) name contradictorio con mismo Team; (3) position contradictoria con mismo Team;
(4) varios atributos incompatibles producen una clasificación del sujeto; (5) conservación de
asociación/active/name/position/espejo; (6) todas las referencias/propietarios/IDs conservados;
(7) orden inverso conserva clasificación/estado/evidencia canónica; (8) otros Players continúan;
(9) siguiente foto coherente permite normalidad. Añadir equivalencias por trim/espacios/case/
diacríticos sin falsos conflictos. Fuente de regresiones B-V5-02 debe extenderse durante IMPLEMENT;
no se afirma que esos tests nuevos ya estén escritos o ejecutados.

**B-V5-04 (HIGH):** normalización de comparación no define representación persistida/expuesta
para textos equivalentes con formas distintas. Ejemplo: mismo Player/Team/position, observaciones
name='José Pérez' y name='JOSE PEREZ', nombre persistido previo='Nombre anterior'. Ambas son
equivalentes según SPEC-007; no son INVALID_SUBJECT_DATA, pero updatePlayer escribe el último
texto recibido y el GET refleja una presentación distinta según orden. SPEC-007 RF-021/RF-034
definen comparación estricta, no un texto canónico de Player; guardar la clave normalizada,
conservar un valor anterior distinto o elegir una forma recibida necesita política funcional.
Lo mismo aplica a position con distintas formas equivalentes. No resolver automáticamente.

Impacto B-V5-03: clasificación compartida T016/T018/T021 y cobertura existente US02/US03/US06,
dentro de protecciones ya autorizadas, sin ampliar US ni proveedores/HTTP/V5. Autorizaciones
US01–US06 preservadas; evaluar impacto de la decisión pendiente antes de invalidar alguna.
Preparación global sigue bloqueada por B-V5-04, no por B-V5-03 ni progreso parcial. Feature/US
IN PROGRESS; Status Draft; siguiente transición CLARIFY → ANALYZE → READY. Sin producto nuevo,
tests/builds/migraciones ejecutados, V6 ni gates humanos aprobados durante esta corrección.

## Estado operativo vigente — nueva revisión de cierre Release 1

La aceptación humana de B-V5-02 conserva IMPLEMENT AUTHORIZATION US01–US06 y el alcance V5.
Se corrigió mecánicamente Status de spec de Ready for planning a Ready for implementation según
specs/README.md y el ANALYZE vigente; el nuevo hallazgo siguiente vuelve a invalidar READY global.
No se han ejecutado tests, builds ni migraciones ni iniciado V6. No se completa ninguna task
por la corrección editorial ni se aprueban gates. B-V5-01 y B-V5-02 siguen resueltos/aceptados.

**B-V5-03 — decisión funcional pendiente (HIGH):** varias referencias válidas distintas del mismo
Player pueden resolver el mismo Team válido en una misma foto pero informar name o position
obligatorios distintos, todos individualmente válidos. B-V5-02 excluye únicamente Teams distintos;
su camino coherente permite procesar normalmente. SPEC-002 define prioridad/consolidación por
una misma referencia externa, no entre referencias distintas de un mismo propietario; SPEC-007
RF-010/RF-039 y contracts/api.md no establecen selección ni rechazo para esos atributos cuando
Team es coherente. PlayerCatalogServiceImpl.protectConflictingPlayerTeams (distinctTeams < 2)
permite el grupo; synchronizeCandidates actualiza varias veces al mismo Player y updatePlayer
sobrescribe name/position con cada observación. El resultado depende del orden, sin política
funcional aprobada. Hallazgo estático; no se fabricó ni ejecutó un snapshot para acreditarlo.

Ejemplo mínimo: referencia A → mismo Player, Team T, name=N1/position=P1;
referencia B → mismo Player, Team T, name=N2/position=P2. Ambas referencias se conservan.
Falta decidir si el sujeto se protege como INVALID_SUBJECT_DATA ante esta contradicción o si
existe una regla explícita para consolidar sus atributos. No extender B-V5-02 por analogía ni
escoger first/last/min/max ID automáticamente. La preservación de opcionales ya existente no
resuelve qué name/position obligatorios prevalecen.

**SDD:** STOP IMPLEMENT por nueva decisión funcional, no por progreso parcial. READY: NO;
preparación global BLOCKED hasta clarificación upstream y nuevo ANALYZE. Feature/US01–US06
permanecen IN PROGRESS. Autorizaciones existentes preservadas; evaluar materialidad de la
decisión futura antes de invalidar alguna. Impacto potencial US02/US03 y T016/T018/T021/T045/T047,
sin declarar cambios de alcance por conjetura. Resto del trabajo y gates históricos se preservan.

**Input**: `specs/007-team-league-domain/` — spec, plan, research, data-model, contracts/api y quickstart.
**Prerequisites**: leer esos artefactos y las instrucciones de cada área antes de implementarla.
**Tests**: todo comportamiento backend nuevo/modificado requiere cobertura automatizada suficiente conforme a Constitution y docs/backend/testing.md, aunque no exista solicitud explícita ni TDD. Las tareas siguientes crean/adaptan tests durante IMPLEMENT; nunca los ejecuta el agente. CODE REVIEW incluye los tests y el gate humano TESTING revisa cobertura, ejecuta y valida resultados. Sin tests frontend. Builds solo cuando Constitution/AGENTS los permitan; ninguno en esta corrección documental.

**Planificación de cobertura**: se conservan IDs T001–T041; T042–T052 se incorporan en las fases del comportamiento correspondiente, no en orden numérico global. Reutilizar suites suficientes; no un test por clase ni metas porcentuales. Todos los tests siguen categorías/fronteras, perfil, PostgreSQL Testcontainers/Flyway, mocks permitidos, @Nested y convenciones de docs/backend/testing.md. Paths de tests relativos a backend/src/test/java/footballmarket/. Autorización vigente V5 incluye T042–T051; T052 queda pendiente de Release 2. Planificar no acredita tests escritos ni ejecutados.
**Organization**: historias por prioridad P1 (US1, US2, US3, US6), luego P2 (US4, US5). La numeración de historias conserva la spec.

**Alcance vigente del incremento**: Release 1/V5 completo autorizado por el usuario: US01–US06, T001–T038 y T042–T051, más documentación preparatoria V5 de T041. T025 completo es compatibilidad V5; backfill T024 y marcador T026 también son V5. T039/T040/T052 y ejecución operativa posterior al gate quedan excluidos; documentar el procedimiento futuro no autoriza ejecutarlo. T028 registra preparación/gate pendiente, nunca aprobación ni despliegue. Autorizaciones históricas preservadas; nuevas US autorizadas permanecen READY hasta inicio efectivo. Foundation compartido no se asigna exclusivamente a US01.

**Avance**: T003 renombra ExternalProvider y todos sus consumidores productivos/tests sin cambiar constantes ni EnumType.STRING; búsqueda en backend/src sin referencias PlayerProvider. T008 y fuentes de tests de normalización creadas (parte de T042). Build backend `./gradlew build -x test` aprobado tras corregir formato; no ejecuta/compila tests ni aplica migraciones. Fuente V5 creada (T004), pendiente de validación de esquema y cobertura T043; no marcar T004 completa por build. Consumidores frontend adaptados al contrato interno (T012/T013); build frontend `npm run build` aprobado sin tests. T013 espera validación final del contrato backend T011; US01 aún incompleta. Ejecución de tests pendiente del usuario.

**Continuación IMPLEMENT, 2026-10-07 (sin controles ejecutables de código)**: creadas fuentes League/Team/referencias/repositorios, PendingReviewCase/causas/categorías, CatalogTransition y TeamResolutionAttempt. Player tiene asociación y espejo temporal; GET/DTO/Mapper tienen los cuatro campos internos. Fuentes Football-Data conservan identidades externas y presencias inválidas; contratos externos separados de Model. Aplicación de ligas/equipos, asociaciones, vigencia, casos, backfill y marcador incorporada al Service, pendiente de completar clasificación/conflictos/reaplicación y revisión integral. Enriquecimiento independiente y matcher estricto incorporados, con red posterior al commit y abstención por completitud no acreditada. Tests puros de entidades/asociaciones/casos/matcher/intentos y fuentes de migración V5 añadidos; Controller/Integration/Orchestrator y configuración parcialmente adaptados. No acredita cumplimiento completo ni funcionamiento: suites funcionales/persistencia existentes aún requieren adaptación a las fotos con identidades reales y cobertura T044–T051; formato, coherencia contractual y concurrencia también pendientes. Ninguna task adicional se marca completa. El build anterior no verifica estos cambios.

**Blocker de compatibilidad B-V5-01 — pendiente de decisión humana**: V3/V4 admiten varias referencias de un mismo proveedor para un Player (V3 solo UNIQUE(provider,external_id); SPEC-002 RF-012/RF-013 y operación original addExternalReference). SPEC-007 RF-010/data-model exigen una por proveedor, y V5 añade UNIQUE(player_id,provider), mientras RF-017/Assumptions conservan referencias y no redefinen semántica general de PlayerExternalReference. Una entrada válida V4 con esa cardinalidad no puede pasar la nueva constraint conservando todas sus referencias en la misma representación. No hay política aprobada para ese estado: no elegir/eliminar referencias, repartir Players ni relajar constraint automáticamente. Hallazgo estático; no se consultó DB ni se afirma que existan duplicados reales. T004 sigue pendiente; implementación se detiene por esta decisión funcional/compatibilidad, no por progreso parcial. Resolver en fuente funcional/diseño y evaluar impacto antes de ANALYZE/READY. Autorizaciones no afectadas preservadas; no gates humanos aprobados, no V6.

**Estado operativo al detectar B-V5-01**: Feature y US01–US06 IN PROGRESS, porque todas comenzaron comportamiento propio; IMPLEMENT detenido por el blocker anterior. Preparación global pendiente de reevaluación, no usar ANALYZE anterior para continuar mientras exista esta decisión abierta. No US completa para CODE REVIEW. Sin tests/builds ni migración aplicados en esta continuación. H1–H4 permanecen pendientes.

**Frontera y orden técnico V5**: setup/foundation → foto completa/clasificación → casos/aplicación/conflictos → actualizaciones de identidad y espejo legacy → cálculo de vigencia/protecciones → backfill/marcador en el mismo commit principal → enriquecimiento posterior independiente → revisión contractual/operativa V5. El orden técnico satisface la dependencia ya documentada de US6 respecto de US5, sin renumerar/reordenar fases ni cerrar H1–H4. Gate humano de Release 1 separado; ningún build, marcador ni tarea completa lo satisface. US04 incluye el intento con abstención válida por completitud no acreditada: ausencia de referencia no permite omitir su implementación.

## Format: `[ID] [P?] [Story] Description`

### Resolución B-V5-02 — decisión humana 2026-10-07

**ANALYZE posterior (solo lectura): PASS WITH NON-BLOCKING.** Revisión de Constitution, spec/clarificación, plan/data-model/contratos, tasks y checklist: conflicto por propietario cubierto T016–T018/T045–T046; conservación T021/T025–T026/T036/T047–T048/T051; determinismo/otros sujetos/foto posterior cubiertos en esas regresiones. Sin nuevas contradicciones funcionales detectadas. Finding LOW: Status editorial de spec conserva Ready for planning; no decide estado operativo. READY global recuperado para el alcance V5; Feature/US01–US06 continúan IN PROGRESS; autorizaciones preservadas sin ampliación material. H1–H4 no resueltos, ningún gate humano aprobado. Registro separado del análisis, no corrección dentro de ANALYZE.

**Retrabajo IMPLEMENT B-V5-02:** clasificación por propietario y Team resueltos antes de actualizar Players; exclusiones funcionales locales al intento, caso interno/causa estable y evidencia canónica; protección incluye espejo/backfill y marcador reconoce exclusivamente el caso delimitado con original. Fuente PlayerSnapshotConsistencyPersistenceTest cubre cardinalidad histórica, mismo Team, conflicto activo/inactivo/sin asociación, orden inverso/deduplicación, continuidad de otro Player, foto posterior coherente y rollback del caso por fallo técnico. Aislamiento del contenedor restablece nuevas tablas/marcador; suites Catalog Service/Persistence reciben identidades Team/League explícitas. Fuentes escritas, no compiladas ni ejecutadas; resto de T045–T051 y cierre integral V5 aún pendientes, sin marcar tasks completas.

Estado vigente de la decisión: RESOLVED; los párrafos anteriores/posteriores que describen bloqueo son antecedentes. Múltiples referencias válidas del mismo Player con Teams válidos distintos en la misma foto → INVALID_SUBJECT_DATA. Conservar asociación/active/referencias/datos derivados persistidos, sin transferencia/retirada/reactivación/fallback; independiente del orden, otros Players continúan. Foto coherente posterior procesa normalmente, sin invalidez permanente ni cierre automático de casos. B-V5-01 permanece vigente.

Impacto acotado T016/T017/T018/T021/T025/T026/T036 y cobertura T045/T046/T047/T048/T051: clasificación por propietario antes de mutarlo, causa CONFLICTING_PLAYER_TEAMS y evidencia canónica, protección de todas sus observaciones y espejo, caso delimitado con original para preexistente sin Team. No modifica identidad, HTTP, proveedores, US ni límites V5/V6; concreta RF-039 para el estado compatible de RF-010, dentro del alcance previamente autorizado. Autorizaciones US01–US06 preservadas por ausencia de ampliación material. No invalidar trabajo ajeno al comportamiento last-wins sobre ese Player. ANALYZE posterior pendiente; ninguna task/gate completa por documentación.

Cobertura obligatoria B-V5-02 en IDs existentes: (1) referencias múltiples al mismo Team procesan normal; (2) Teams distintos registran INVALID_SUBJECT_DATA/CONFLICTING_PLAYER_TEAMS; (3) asociación/active/referencias/espejo permanecen tanto activo como inactivo y aún sin Team; (4) orden invertido conserva resultado y evidencia canónica; (5) otro Player válido se procesa; (6) siguiente foto coherente permite procesamiento normal, transferencia/retirada/regreso conforme reglas existentes sin consultar casos históricos como bloqueo. Funcional por APIs Service productivas; preparación de cardinalidad legacy y observación de casos/active/espejo en suite técnica PostgreSQL real, no crear API para fixtures.

### Estado vigente tras corregir B-V5-01

B-V5-01 resuelto por decisión humana y retrabajo puntual: retirados UNIQUE(player_id,provider) de la fuente V5 y rechazo por proveedor de Player; regresión de modelo verifica conservación/idempotencia, y fuente de migración V4→V5 conserva tres referencias (dos FOOTBALL_DATA y una THE_SPORTS_DB), IDs y propietarios. UNIQUE(provider,external_id) y constraints de Team/League se preservan. Ningún test/build/migración ejecutado; T004/T042/T043 siguen pendientes de sus restantes criterios. El análisis inicial de los artefactos corregidos fue PASS WITH NON-BLOCKING (Status editorial); el hallazgo posterior B-V5-02 impide usar ese PASS para continuar IMPLEMENT.

**B-V5-02 — ambigüedad funcional pendiente:** varias referencias FOOTBALL_DATA distintas del mismo Player pueden aparecer en la misma foto con Team/datos contradictorios, cada relación individualmente válida. RF-010 permite conservarlas, RF-004 exige un Team único y el contrato solo define consolidación por referencia, no prioridad entre referencias del mismo propietario. El Service actual procesa candidatos por externalId y actualiza el mismo Player varias veces (synchronizeCandidates), dejando el último dato; ese orden de implementación no es fuente funcional. Debe decidirse si se protege al Player sin actualizar y se registra invalidez individual, o si existe una prioridad explícita entre observaciones; en todos los casos se conservan todas las referencias. No implementar prioridad/selección ni diseñar assertions de esa política por cuenta del agente. Impacto potencial T016/T022/T045/T046/T048, US02/US03/US06; evaluar materialidad después de resolver, sin invalidar otras autorizaciones por conjetura.

**ANALYZE vigente: BLOCKED** por B-V5-02 (HIGH, comportamiento no definido para un estado admitido). B-V5-01 no vuelve a abrirse. Feature/US01–US06 permanecen IN PROGRESS; READY global pendiente de aclaración y análisis posterior. Trabajo válido y autorizaciones históricas preservados; ningún CODE REVIEW/TESTING/DONE aprobado, no V6.

### Resolución B-V5-01 — decisión humana 2026-10-07

Conservar todas las referencias legacy válidas de Player, incluso varias del mismo proveedor. Cardinalidad anterior propuesta en 007: una por proveedor para Player/Team/League. Cardinalidad corregida: Player varias identidades externas distintas por proveedor; Team/League una por proveedor. En cada tabla `UNIQUE(provider,external_id)` preserva propietario único por identidad externa; solo Team/League tienen además unicidad por propietario/proveedor. V5 no añade `UNIQUE(player_id,provider)` ni modifica referencias históricas. La descripción previa del blocker se conserva como antecedente, no como estado vigente de la decisión.

Impacto: RF-010/clarificación, data-model, plan, research, contrato (sin cambio HTTP), quickstart y T004/T022/T042/T043/T046/T048. Retrabajo de producto limitado a quitar la constraint nueva de Player y su rechazo por proveedor; sustituir el test que esperaba ese rechazo por conservación/idempotencia y agregar regresión V4→V5 con múltiples referencias. No renumerar ni regenerar tasks; ninguna tarea/gate se completa por la corrección. Mappings y unicidad por identidad externa de Player ya son compatibles; Team/League, asociaciones, backfill, vigencia, enriquecimiento y frontend se preservan. No US nueva ni ampliación material: se restaura la compatibilidad expresamente exigida y la cardinalidad de SPEC-002; autorizaciones vigentes se conservan. ANALYZE posterior obligatorio antes de reanudar producto; tests/builds/migración no ejecutados.

Todas las tareas comienzan sin completar. `[P]` permite trabajo independiente tras completar sus dependencias; no autoriza agentes paralelos automáticamente. Rutas relativas a la raíz. No crear proyectos, dependencias, endpoints o campos adicionales.

## Phase 1: Setup

- [x] T001 Revisar instrucciones aplicables, implementaciones y consumidores; registrar alcance de Release 1/2 y verificaciones pendientes en specs/007-team-league-domain/tasks.md, consultando backend/AGENTS.md y frontend/AGENTS.md sin ejecutar tests.
- [x] T002 Inventariar mappings, migraciones V1–V4 y configuración efectiva en backend/src/main/resources/db/migration/ y backend/src/main/resources/application.yml para preparar V5 sin incluir V6 en Release 1; conservar cambios previos.

## Phase 2: Foundational — prerrequisitos compartidos

- [x] T003 Renombrar PlayerProvider a ExternalProvider en backend/src/main/java/footballmarket/models/ y actualizar todos sus consumidores, incluidos tests existentes, manteniendo FOOTBALL_DATA/THE_SPORTS_DB y EnumType.STRING sin cambios de valores persistidos o HTTP.
- [x] T004 Crear V5__create_team_league_domain.sql en backend/src/main/resources/db/migration/ con tablas/secuencias League, Team, referencias, casos, intentos y catalog_transition; añadir players.team_id nullable con FK/índice, preservar legacy NOT NULL y establecer las restricciones de data-model.md; no crear V6 todavía. B-V5-01: mantener UNIQUE(provider,external_id) de Player, no añadir UNIQUE(player_id,provider) ni modificar referencias legacy; unicidad propietario/proveedor solo para Team/League.
- [x] T005 [P] Implementar League y sus referencias externas en backend/src/main/java/footballmarket/models/ y sus repositorios en backend/src/main/java/footballmarket/repositories/, con nombre obligatorio y propiedad externa inmutable.
- [x] T006 Implementar Team, referencias, asociación obligatoria a League y current explícito en backend/src/main/java/footballmarket/models/ y repositorios en backend/src/main/java/footballmarket/repositories/, conservando IDs ante cambios.
- [x] T007 [P] Implementar PendingReviewCase sin status en backend/src/main/java/footballmarket/models/ y acceso interno en backend/src/main/java/footballmarket/repositories/: claves canónicas UNIQUE, categorías/causas tipadas, primera/última detección y evidencia JSONB, nunca nombre o descripción como identidad.
- [x] T008 [P] Implementar TeamNameNormalizer puro en backend/src/main/java/footballmarket/models/ con trim, diacríticos, case independiente de locale y espacios colapsados, sin quitar sufijos, aliases o fuzzy; no cambiar la tolerancia del matcher de jugadores.
- [x] T009 Adaptar Player en backend/src/main/java/footballmarket/models/Player.java a Team nullable para transición/casos permitidos y mappings legacy temporales; derivar liga de Team y mantener IDs/referencias/active sin duplicar fuente de verdad.
- [x] T042 Crear/adaptar tests compartidos en models/ (PlayerTest, PlayerExternalReferenceTest y suites de League/Team/referencias, PendingReviewCase y TeamNameNormalizer cuando corresponda): construcción e invariantes, propietario inmutable, relación Team–League, cambios sin pérdida de identidad, nullable transitorio/legacy conservado, evidencia original/fechas y normalización estricta con locale/diacríticos/espacios/sufijos. Cubrir T003/T005–T009 sin implementar casos de uso de sincronización o enriquecimiento; tests puros, sin Spring/DB ni getters triviales. Adaptar consumidores existentes del enum sin ampliar comportamiento de imágenes. Foundation habilita US01–US06, no pertenece exclusivamente a una US.
- [x] T043 Crear/adaptar tests compartidos en repositories/ (patrón PlayerMigrationTest/PlayerCatalogPersistenceTest): V1–V4 → V5 con datos legacy preservados, team_id nullable/FK, mappings y unicidad de referencias/case_key, NOT NULL conservados y estructura inicial de intentos/marcador. PostgreSQL Testcontainers y Flyway reales; verificar constraints no demostradas en otras capas, no CRUD estándar. Depende de T004 y mappings T005–T009; excluye guarda V6, backfill y aplicación transaccional de US02–US06.

## Phase 3: US1 — Reconocer equipo y liga (P1)

**Goal**: consulta paginada con entidades internas y consumidores adaptados.
**Independent verification**: con datos válidos persistidos, GET devuelve solo activos con Team vigente/League válida, cuatro campos no nulos, sin team/league; vacío conserva totales cero. Cliente muestra clasificación por nombre o neutral.

- [x] T010 [US1] Actualizar la consulta y carga de asociaciones en backend/src/main/java/footballmarket/repositories/PlayerRepository.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: active + Team.current + League válida, orden/paginación existentes y sin lazy loading fuera de lectura transaccional.
- [x] T011 [US1] Sustituir team/league por teamId/teamName/leagueId/leagueName en DTO y mapper del catálogo en backend/src/main/java/footballmarket/controllers/, manteniendo campos opcionales, JWT, errores y contrato paginado existentes.
- [x] T012 [P] [US1] Adaptar DTO/modelo y mapper de frontend/src/features/players/types/dtos.ts, frontend/src/features/players/types/models.ts y frontend/src/features/players/players.mapper.ts al contrato nuevo, sin campos visuales ni mapas por IDs.
- [x] T013 [US1] Adaptar frontend/src/features/players/components/PlayerCard/PlayerCard.tsx y frontend/src/features/players/constants/playerClassifications.ts a teamName/leagueName, preservando clasificación por nombre y fallback neutral existente incluso tras rename desconocido.
- [x] T044 [US1] Crear/adaptar repositories/PlayerCatalogPersistenceTest y controllers/PlayerControllerTest para T010/T011: query real con activos/inactivos, Team nulo/no vigente y League válida, caso de revisión que no excluye asociado válido, joins/carga sin lazy fuera de lectura, orden/paginación y página vacía/fuera de rango. Controller con Service mock verifica cuatro campos internos obligatorios, ausencia de team/league/referencias/proveedores, opcionales nullable, JWT y 400 existentes; Mapper por HTTP, sin suite dedicada. Actualizar fuentes REST Docs de respuestas GET afectadas y anotaciones OpenAPI correspondientes a T011, sin ejecutar T037 completo ni generar snippets. Usar datos controlados en Repository; pruebas funcionales de Service con preparación por sincronización se completan en T045, no habilitan US02 ahora.

## Phase 4: US2 — Sincronizar ligas y equipos (P1)

**Goal**: foto completa previa y aplicación local atómica con casos individuales.
**Independent verification**: una foto controlada crea entidades/referencias/asociaciones en un commit; foto incompleta no escribe y fallo técnico revierte todo; presente inválido conserva estado y presencia.

- [x] T014 [US2] Adaptar configuración de ligas en backend/src/main/java/footballmarket/ y backend/src/main/resources/application.yml de cinco códigos fijos a lista no vacía sin duplicados, manteniendo configuración inicial y validación efectiva.
- [x] T015 [US2] Ampliar backend/src/main/java/footballmarket/integrations/FootballDataIntegration.java y records de backend/src/main/java/footballmarket/models/ para obtener competición/equipos/planteles requeridos y construir foto inmutable antes de escritura, distinguiendo respuesta vacía válida, datos opcionales y respuestas incompletas.
- [x] T016 [US2] Implementar clasificación de presentes/procesables/protegidos en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: identidad insuficiente que compromete presencias aborta foto; invalidez identificable registra causa y protege entidad/dependientes sin fallback aproximado.
- [x] T017 [US2] Implementar upsert funcional de casos en backend/src/main/java/footballmarket/services/impl/ con Repository de casos: categorías del modelo, evidencia de relaciones previas/recibidas y propietarios; preservar original/primera detección, actualizar última/evidencia actual y reconciliar sujeto externo a interno sin duplicar.
- [x] T018 [US2] Aplicar League/Team/Player y referencias FOOTBALL_DATA mediante un único intento transaccional en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java, resolver presentes válidos por referencias y conservar asociados inválidos; dejar un punto interno para backfill/marcador en el mismo commit, sin HTTP.
- [x] T019 [US2] Implementar rollback/reaplicación acotada de foto ante conflictos reconocidos en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: clasificar SQLSTATE/constraint, excluir/proteger nuevo conflicto y registrar en intento válido; propagar otros fallos, reiniciar contadores y no reutilizar entidades de transacción fallida.
- [x] T020 [US2] Adaptar backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java a coordinación opaca sin lógica de dominio, conservando exclusión de ejecuciones existente y los cinco contadores de jugadores; no agregar contador de casos ni convertir fallos técnicos en descartes exitosos.
- [x] T045 [US2] Crear/adaptar config/FootballDataPropertiesTest, integrations/FootballDataIntegrationTest, services/PlayerCatalogServiceTest, services/FootballDataPlayerServiceTest, orchestrators/PlayerSynchronizationOrchestratorTest y controllers/PlayerControllerTest para T014–T020: lista válida/no vacía/sin duplicados, respuestas completas/vacías válidas/truncadas y opcionales, identidad insuficiente, creación/asociación por referencias e idempotencia, presentes inválidos protegidos, errores sin cambios y cinco contadores HTTP conservados. Service real con @SpringBootTest/perfil test/Integration mock; Arrange/Act/Assert solo por APIs productivas, incluyendo consulta local sin red y paginación de US01 tras preparar por sincronización. Adaptador con proveedor controlado; Orchestrator puro con Services mock; POST/errores contractuales con REST Docs. No probar aún backfill, enriquecimiento ni retirada completa.
- [x] T046 [US2] Crear/adaptar repositories/PlayerCatalogPersistenceTest y suites técnicas de persistencia para T017–T019: claves canónicas/upsert/reconciliación externo→interno sin duplicación, primera detección/original preservados, unicidad/propietarios y concurrencia determinista, rollback total de intento, conflicto reconocido con reaplicación acotada y propagación de fallos ajenos. PostgreSQL Testcontainers real, sin Internet, datos/tiempo controlados; atomicidad técnica aquí sin eludir frontera de tests funcionales de Service.

## Phase 5: US3 — Identidad ante cambios (P1)

**Goal**: cambios de nombres, transferencias y liga sin nuevas identidades.
**Independent verification**: segunda foto renombra/mueve Team y transfiere Player preservando IDs y referencias; una identidad ajena nunca se reasigna. leagueName cambia sin promesa visual histórica.

- [x] T021 [US3] Completar actualizaciones por referencia de propietario en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: rename de League/Team, cambio Team→League y Player→Team por foto válida; no resolver identidad por texto ni conservar equipo anterior ante transferencia válida.
- [x] T022 [US3] Aplicar invariantes de propietario inmutable y unicidad en referencias de backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/repositories/, diferenciando actualizar propietario de asignar a otro y registrando evidencia de conflicto sin reasignar. B-V5-01: Player admite varias referencias del mismo proveedor con externalId distintos; agregar la misma referencia es idempotente, otra no sustituye las anteriores. Team/League conservan una por proveedor.
- [x] T023 [US3] Adaptar backend/src/main/java/footballmarket/models/PlayerIdentityMatcher.java y consumidores de backend/src/main/java/footballmarket/services/ a Team.name, manteniendo reglas de imágenes y filtrando jugadores sin Team para evitar null dereference sin añadir sincronización de imágenes a 007.
- [x] T047 [US3] Crear/adaptar services/PlayerCatalogServiceTest, models/PlayerIdentityMatcherTest y tests afectados de services/PlayerImageSynchronizationServiceTest para T021–T023: dos fotos con rename de Team/League, cambio de liga y transferencia de Player conservan IDs/referencias, identidad ajena no se reasigna, nombres derivan de asociaciones; matcher mantiene tolerancia existente y selección excluye Player sin Team sin null dereference. Reutilizar pruebas de constraints T043/T046, agregar solo gaps; adaptaciones de imágenes son regresión de semántica existente, no nueva sincronización. Depende de aplicación US02; Service por API pública.

## Phase 6: US6 — Migrar sin perder jugadores (P1)

**Goal**: transición completa y evidencia persistida con convivencia segura.
**Independent verification**: presentes válidos usan foto aunque legacy difiera; ausentes sin asociación usan matching estricto; presentes inválidos no usan fallback. IDs se conservan, casos deduplican y nuevas altas cumplen legacy NOT NULL. Limpieza se verifica solo después del gate de Release 1.

- [x] T024 [US6] Integrar backfill en la transacción principal de backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java solo para ausentes preexistentes sin Team y transición pendiente; una coincidencia con Team vigente asocia, cero/varias registran original/candidatos sin liga textual y sin cambiar active por el backfill.
- [x] T025 [US6] Implementar espejo legacy temporal en backend/src/main/java/footballmarket/models/Player.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: altas/asociados escriben nombres derivados para NOT NULL; no asociados conservan original y casos capturan evidencia antes de sobrescribir; GET nunca usa espejo.
- [x] T026 [US6] Implementar mapping y actualización de catalog_transition en backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: marcar en el mismo commit solo si cada Player tiene asociación válida o caso permitido con evidencia; tras finalización no repetir backfill desde casos.
- [x] T027 [P] [US6] Ajustar consultas SQL y procedimiento de solo lectura en specs/007-team-league-domain/quickstart.md al esquema implementado, con rol separado autorizado, sin endpoint/comando/status ni credenciales generales de aplicación.
- [x] T028 [US6] Registrar en specs/007-team-league-domain/tasks.md el gate pendiente del usuario: desplegar Release 1 con solo V5, ejecutar sincronización y verificar IDs/conteos/asociaciones/evidencia/marcador; detener implementación de T039–T040 hasta confirmación explícita, sin marcar este gate satisfecho por existencia de código.
- [x] T048 [US6] Crear/adaptar services/PlayerCatalogServiceTest y pruebas técnicas en repositories/ para T024–T026: presente válido usa foto contra legacy, ausente sin asociación usa matching estricto con cero/una/varias coincidencias sin liga textual, presente inválido no usa fallback, active/IDs/referencias se conservan por transición; repetición no pierde/duplica ni repite backfill finalizado. Verificar casos con original/candidatos, espejo NOT NULL de nuevas altas/asociados, marcador y rollback conjunto. API pública en tests funcionales; evidencia/constraints/marcador y rollback técnicos en persistencia real. La comprobación integral del conjunto vigente/protegido requiere T035/T036 conforme a dependencia existente; no adelantar gate ni V6.

## Phase 7: US4 — Identidad TheSportsDB no bloqueante (P2)

**Goal**: enriquecimiento independiente posterior al commit con confianza y retries correctos.
**Independent verification**: fixtures con completitud acreditada y único match permiten referencia; searchteams.php sin evidencia de completitud no asigna. Fallos técnicos permiten siguiente sync y no revierten principal; evaluación válida consume nombre, referencia resuelta no se reevalúa.

- [x] T029 [P] [US4] Implementar TeamResolutionAttempt y Repository en backend/src/main/java/footballmarket/models/ y backend/src/main/java/footballmarket/repositories/, separando última llamada/resultado técnico de última evaluación válida/nombre y retry_not_before.
- [x] T030 [US4] Adaptar búsqueda de equipos en backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java: candidatos Soccer/nombre principal, identidad externa y evidencia de completitud; searchteams.php sin prueba retorna completitud no acreditada, sin asumir Premium ni unicidad por cantidad.
- [x] T031 [US4] Implementar evaluación estricta y Service de enriquecimiento en backend/src/main/java/footballmarket/services/ y backend/src/main/java/footballmarket/services/impl/: deduplicar identidades coherentes, rechazar duplicados contradictorios, distinguir MATCH/NO_MATCH/AMBIGUOUS/COMPLETENESS_UNPROVEN, sin primer resultado ni fuzzy.
- [x] T032 [US4] Implementar elegibilidad, pacing y Retry-After en el Service de backend/src/main/java/footballmarket/services/impl/: una tentativa lógica por Team/sync sin retry interno nuevo, fallos técnicos no consumen evaluación, respuesta válida consume nombre y referencia resuelta excluye consultas.
- [x] T033 [US4] Persistir intento y eventual referencia/caso atómicamente por Team en backend/src/main/java/footballmarket/services/impl/, con red fuera de escritura y revalidación de nombre/propietario; descartar resultados obsoletos sin consumir nombre nuevo, rollback corto ante fallo.
- [x] T034 [US4] Invocar enriquecimiento después del commit principal desde backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java sin inspeccionar entidades ni cambiar respuesta confirmada; diagnosticar fallos independientes y permitir enriquecimiento parcial entre Teams.
- [x] T049 [US4] Crear/adaptar models/, integrations/TheSportsDbIntegrationTest y services/ para T029–T032: Soccer/nombre principal estricto, identidades deduplicadas/contradictorias, MATCH/NO_MATCH/AMBIGUOUS/COMPLETENESS_UNPROVEN, ausencia de prueba de completitud sin asignación, referencia resuelta excluida, elegibilidad por evaluación válida/nombre, pacing/Retry-After y máximo una tentativa lógica. Timeout/transporte/429/5xx/4xx/estructura inválida no consumen evaluación; respuesta válida sí. Tiempo/proveedor controlados, sin Internet/Premium supuesto ni fuzzy.
- [x] T050 [US4] Crear/adaptar tests en services/, repositories/ y orchestrators/ para T033/T034: red fuera de escritura y después del commit principal, revalidación ante rename/referencia concurrentes, resultado obsoleto descartado, intento+referencia/caso atómicos por Team, rollback corto sin consumir oportunidad, conflictos sin reasignación, fallos sin revertir principal/alterar HTTP y enriquecimiento parcial entre Teams. Service funcional por APIs públicas, atomicidad/concurrencia técnica en PostgreSQL Testcontainers y coordinación opaca con Services mock; fixtures deterministas sin sleeps arbitrarios.

## Phase 8: US5 — Retirada y regreso (P2)

**Goal**: vigencia explícita y actividad con protecciones, sin eliminación física.
**Independent verification**: ausentes confirmados salen, protegidos conservan estado y regreso válido conserva identidad/reactiva; foto incompleta no altera estados. Un Player protegido activo con Team no vigente no aparece en GET.

- [x] T035 [US5] Recalcular Team.current al aplicar foto completa en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java: presentes válidos vigentes, ausentes no protegidos retirados y protegidos con estado previo; configuración sola no escribe ni League recibe current.
- [x] T036 [US5] Completar activación/reactivación/inactivación en backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java según presentes/protegidos/ausentes y Team retirado, proteger planteles previos indeterminables, conservar active de inválidos y nunca borrar físicamente entidades/referencias.
- [x] T051 [US5] Crear/adaptar services/PlayerCatalogServiceTest y pruebas técnicas en repositories/ para T035/T036: ausencia confirmada retira Team/inactiva Player sin borrarlos, regreso válido conserva identidad/reactiva, inválidos/protegidos conservan estado, Team no vigente excluye del GET aun con Player protegido activo, configuración sola y foto incompleta no alteran vigencia. Completar regresión integral de US6 con conjunto vigente/protegido y conservación de todos los IDs, incluidos inactivos; API pública para comportamiento, persistencia para invariantes técnicas no observables por esa API. Depende de US02/US03 y aplicación de backfill para esa regresión, no resuelve ni reordena H1.

## Phase 9: Polish & Cross-Cutting — entrega en dos releases

- [x] T037 Actualizar anotaciones OpenAPI y fuentes REST Docs/consumidores existentes en backend/src/main/java/footballmarket/controllers/ y backend/src/test/java/footballmarket/ al contrato de specs/007-team-league-domain/contracts/api.md, sin editar snippets generados ni agregar campos de revisión o clasificación. Crear/adaptar tests contractuales cuando sean necesarios conforme a Constitution/testing, reutilizando T044/T045 sin duplicar cobertura. T044 incluye la porción GET necesaria para Phase 3; ello no completa ni autoriza T037 como tarea transversal.
- [x] T038 Revisar Release 1 y documentar comprobaciones pendientes en specs/007-team-league-domain/quickstart.md y specs/007-team-league-domain/tasks.md: escenarios de cada historia, compatibilidad, SQL, controles backend del usuario y builds permitidos tras código; no ejecutar tests ni declarar controles sin evidencia.
- [ ] T039 Solo tras confirmación explícita del gate T028 y para Release 2, crear backend/src/main/resources/db/migration/V6__drop_player_team_league_text.sql transaccional PostgreSQL: validar marcador/integridad/asociación o caso permitido con original, luego eliminar columnas; fallos dejan esquema intacto, sin DDL no transaccional ni V6 en Release 1.
- [ ] T040 Solo junto con V6 en Release 2, retirar mappings/lecturas/escrituras legacy de backend/src/main/java/footballmarket/models/Player.java y backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java; mantener Team nullable para casos permitidos y evidencia en casos, sin fallback desde evidencia histórica.
- [x] T041 Finalizar procedimiento operativo en specs/007-team-league-domain/quickstart.md: backup consistente, exclusión de escritores antiguos, binario final compatible, manejo de fallos Flyway y forward-only; registrar revisión final de diff/status y controles pendientes en specs/007-team-league-domain/tasks.md sin prometer downgrade o restauración realizada.
- [ ] T052 Crear/adaptar tests de migración en repositories/ y regresiones afectadas de Model/Service/Controller para T039/T040 (comportamiento de transición US06/RF-036): guarda V6 rechaza marcador pendiente, asociaciones inválidas/casos sin evidencia; éxito elimina columnas y valida mappings finales, Team nullable permitido, original preservado y fallo transaccional deja esquema intacto. PostgreSQL Testcontainers/Flyway reales, sin esquemas divergentes ni DB externas; revisar regresión de GET sin legacy. Implementación de esta tarea solo junto a Release 2 tras gate humano T028 y T038; tests no acreditan ni ejecutan gate/despliegue. No reasignar T039/T040 ni resolver H3.

## Dependencies & Execution Order

- Setup → Foundational → US1 → US2 → US3 → US6 → US4 → US5 → cierre de Release 1.
- US1 verifica consulta con datos controlados; catálogo real completo requiere US2/US6. No desplegar una historia aislada como Release 1 funcional completo.
- US3/US6 dependen de aplicación US2. US4 y US5 dependen de US2/US3; comparten catálogo/orchestrator con otras tareas y se secuencian para evitar conflictos. US6 backfill usa el conjunto vigente/protegido completado por US5: verificarlo antes del gate.
- T004 antecede mappings/repositorios; T005 antecede T006; T007/T008 pueden hacerse en paralelo con T005. T012 puede adelantarse tras contrato conocido, pero T013 requiere T012 y validación final T011.
- T027 puede hacerse en paralelo con T024–T026 una vez fijado esquema. T029 puede hacerse en paralelo con T030 tras foundation; T031 necesita ambos.
- T028 registra y controla el gate, no ejecuta operación por el usuario. T039/T040 están bloqueadas hasta gate humano confirmado, Release 1 completo y revisión T038. V5 y V6 jamás se distribuyen en el mismo release.
- T042 espera los modelos/normalizador de T003/T005–T009; T043 espera T004–T009; T044 espera T010/T011 y foundation. Son trabajo de implementación de tests incluido en Phase 2/3, no ejecución humana. T045/T046 siguen T014–T020; T047 sigue T021–T023; T048 sigue T024–T026 y su validación integral espera T035/T036; T049/T050 siguen las tareas US4 correspondientes; T051 sigue T035/T036 y completa el escenario integral con US6; T052 sigue T039/T040 y su gate. Dependency ≠ Blocker: dependencias identificadas no autorizan fases posteriores ni permiten obviar gates. La cobertura se revisa sin duplicación entre tareas antes de entregar cada alcance.

## Parallel Execution Examples per Story

- **US1**: T011 backend y T012 frontend usan contrato común y archivos distintos; T013 espera tipos nuevos.
- **US2**: no paralelizar tareas de Service compartido; inventario de configuración/contratos puede revisarse independientemente, implementación T014–T020 secuencial.
- **US3**: no paralelizar T021/T022: invariantes y aplicación se revisan juntas; T023 después de estabilidad del modelo.
- **US6**: T027 documentación SQL en paralelo con T024–T026 código; no ejecutar gate automáticamente.
- **US4**: T029 persistencia y T030 integración en paralelo; T031–T034 secuenciales.
- **US5**: T035/T036 secuenciales por archivo/transacción compartidos; revisión de escenarios puede hacerse separadamente, sin ejecución de tests por el agente.

## Implementation Strategy

Cobertura puntual B-V5-01 dentro de IDs existentes: T042 verifica múltiples referencias Player/idempotencia y no reemplazo; T043 verifica V4→V5 con todas las referencias/IDs/propietarios conservados, sin regularización previa, y rechazo de identidad externa duplicada entre propietarios; T046 conserva pruebas de conflictos sin confundir múltiples referencias del mismo propietario con colisión de identidad; T048 verifica conservación de todas las referencias en la transición y presencia por cualquier referencia válida. No debilitar assertions ni usar fixture funcional directo a Repository/SQL fuera de los tests técnicos de persistencia.

**MVP técnico**: foundation + US1 para consulta con asociaciones controladas. **Primer incremento desplegable**: todas las historias de Release 1 con V5, convivencia, atomicidad/protecciones y transición; TheSportsDB puede abstenerse válidamente por completitud no acreditada.

Completar Release 1 → usuario sincroniza/verifica → aprobación explícita del gate → implementar/publicar Release 2 con V6 y mapping final. No inferir aprobación por checklist de calidad ni build. Cada historia se verifica por sus criterios independientes y escenarios de quickstart; ninguna casilla se completa solo por diseño. Mantener trazabilidad y cambios previos; no renombrar rama automáticamente.

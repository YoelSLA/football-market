---

description: "Tareas de implementación de la página de catálogo de jugadores"
---

# Tasks: Página de catálogo de jugadores

## Tareas vigentes del rediseño — 2026-10-26

### Implement authorization — 2026-10-26

El usuario solicitante autorizó expresamente «la implementación de SPEC-005 siguiendo tasks.md (T033–T068)», incluyendo frontend/backend y escritura de pruebas, sin commits/push/merges. Base evaluada: spec/plan/contrato vigentes, checklists20/20 aceptados y ANALYZE previo sin hallazgos. No autoriza tests por el agente, CODE REVIEW, TESTING ni cierre. Se preserva el trabajo previo de SPEC-009 y demás archivos. Reanudación sobre carpeta005 explícita; rama actual no se cambia con el árbol modificado.

Inventario T033: getPage se consume por usePlayersQuery; los mappers de identidad/listado también sostienen el detalle y no deben perder exports. PlayerCard del catálogo es el consumidor efectivo de getPlayerSampleStatistics y etiqueta de demostración; revisar nuevamente sus barrels antes de eliminar fuentes. PlayerCatalogService.getActivePlayers es usado por Controller y tests de Service existentes; conservar firma, DTO legado y comportamiento sin flags. Detalle/estadísticas individuales permanecen independientes.

**Entrada**: bloques vigentes de spec.md, plan.md, research.md, data-model.md, quickstart.md, contracts/players-catalog.md y [contrato canónico de SPEC-002](../002-player-catalog/contracts/catalog-query.md). Checklists catalog.md y api.md: aceptación documental humana registrada, 20/20. La operación autorizada genera tareas y analiza; no implementa ni autoriza implementación.

**Historial**: todas las definiciones, IDs y estados T001–T032 permanecen íntegros a partir de «Antecedente histórico». T036/T037/T047 mencionados allí pertenecen a referencias externas SPEC-009, no son definiciones locales. Las tareas nuevas son SPEC-005/T033–T068; no se hereda avance histórico. Sin cambios de SPEC-009 ni tareas de otro alcance.

**Reglas**: tareas exclusivamente aquí; no se exportan al seguimiento. `[P]` indica archivos distintos tras satisfacer dependencias, no permiso de delegación. Los tests backend se escriben durante implementación autorizada y los ejecuta el usuario; no se ejecutan desde el agente, ni indirectamente por hooks. No crear tests/frontend o infraestructura para ellos. Builds solo tras modificar código del área y con AGENTS. Ninguna tarea técnica sustituye CODE REVIEW, TESTING o cierre humanos.

### Fase 1 — Preparación compartida

- [X] T033 Documentar en `specs/005-players-page/tasks.md` inventario definitivo de consumidores de `frontend/src/features/players/players.service.ts`, `players.mapper.ts`, `constants/playerSampleStatistics.ts` y `backend/src/main/java/footballmarket/services/PlayerCatalogService.java`; identificar firmas/DTO que deben conservarse y usos de demos antes de retirar solo los del catálogo. Dependencias: ninguna; FR-025/032/035, SC-012/014/017.
- [X] T034 Precisar en `specs/005-players-page/quickstart.md` precondiciones de PostgreSQL UTF-8/pg_trgm, rol de migración con permisos de extensión/función/índice y rol lector sin elevación; documentar comprobación humana de disponibilidad y plan de consulta, sin ejecutar migraciones o EXPLAIN desde esta tarea del agente. Dependencias: T033; FR-027/032/038, SC-013/014/019.

### Fase 2 — Fundamentos: normalización, migración y tipos

- [X] T035 Escribir `backend/src/test/java/footballmarket/repositories/PlayerCatalogSearchMigrationTest.java` con PostgreSQL/Testcontainers/Flyway real: función pura STRICT/IMMUTABLE, Unicode compuesto/descompuesto, José/jose/JOSÉ y Álvarez/alvarez, nombre original intacto, GIN sobre la expresión normalizada con gin_trgm_ops. Incluir términos cortos/vacío y `%`/`_`/`!` literales; no exigir que toda consulta use índice ni ejecutar tests por el agente. Dependencias: T034; FR-027, SC-013.
- [X] T036 Crear en implementación futura una migración única bajo `backend/src/main/resources/db/migration/` mediante el mecanismo `createMigration` existente (timestamp real al crearla, no asignado en esta preparación), que habilite pg_trgm, defina función pura trim/NFD/eliminación U+0300–U+036F/lower y GIN de su expresión sobre players.name. Fijar expresión/colación iguales en consulta/índice, sin unaccent ni wrapper de reglas mutables, sin modificar nombre original. No ejecutar migración desde el agente; documentar permisos y reconstrucción ante cambios futuros de función/colación. Dependencias: T035; FR-027, SC-013.
- [X] T037 Definir `backend/src/main/java/footballmarket/models/records/PlayerCatalogCriteria.java` y `controllers/dtos/requests/PlayerCatalogQueryRequestDTO.java`, más transformación en `controllers/mappers/PlayerMapper.java`: filtros opcionales, límites del contrato, sort cerrado y includeStatistics=false por defecto; DTO record sin negocio, Model sin DTO. Mantener page=0/size=20 API, UI size=12; preservar firmas antiguas de lectura mediante delegación compatible. Dependencias: T036; FR-026–032/038, SC-013–015/019.
- [X] T038 Definir criterios, opciones y resumen deportivo nullable en `frontend/src/features/players/types/dtos.ts`, `types/models.ts` y exports de `types/index.ts`, separados tipadamente. No normalizar acentos en cliente ni crear valores de demo; mantener tipos del detalle y consumidores existentes. Dependencias: T037; FR-025/028/032/033, SC-012/014/017.

### Fase 3 — US4 (P1): búsqueda y filtros globales

**Objetivo**: seleccionar jugadores de todo el conjunto visible con nombre/cuatro filtros/orden. **Criterio independiente**: jugador fuera de página inicial encontrado con filtros AND, opciones globales, limpiar y nombre A–Z predeterminado; no requiere aún enriquecimiento deportivo de tarjetas. Se conserva US4 pese al cambio desde filtros visuales; se implementa antes de US1 porque provee la lectura compartida.

#### Pruebas backend — escritura previa

- [X] T039 [P] [US4] Escribir `backend/src/test/java/footballmarket/repositories/PlayerCatalogQueryRepositoryTest.java`: filtros AND globales antes de page/count, visibilidad, búsqueda normalizada/escape parametrizado, orden name asc/desc con ID asc de desempate e ID legado, opciones DISTINCT completas e incompatibilidad liga/equipo sin cascadas. Persistencia real, sin mocks ni ejecución por agente. Dependencias: T038; FR-023/026–030/038, SC-013–015/019.
- [X] T040 [P] [US4] Escribir `backend/src/test/java/footballmarket/services/PlayerCatalogQueryServiceTest.java` con Service real @SpringBootTest/perfil test, helper compartido de Arrange en `backend/src/test/java/footballmarket/support/PlayerCatalogQueryData.java` si necesita fixtures por Repository, y Act/Assert solo API pública productiva: filtros, opciones, visibilidad y errores controlados. No Repository/SQL en el test ni helpers funcionales durante Act/Assert; no ejecutar. Dependencias: T038; FR-023/026–030/034/038, SC-013/015/017/019.
- [X] T041 [P] [US4] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para parámetros/defaults, listado sin flags exactamente compatible, sort inválido, límites de texto/IDs/tipos, sin interacción de Service en rechazo, GET /filters autenticado y errores globales. Usar slice real y Service mock, sin pruebas de Mapper aisladas ni ejecutar tests. Dependencias: T038; FR-023/026–032/038, SC-013/014/019.

#### Backend y frontend

- [X] T042 [US4] Extender `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` con consulta visible compuesta de criterios y opciones globales: mismo predicado para count/content, función normalizada para nombre, LIKE con parámetro escapado y ESCAPE ! fijo, sort whitelist/lower(name)/ID, asociaciones cargadas y paginación SQL. Conservar findByActiveTrue/findVisibleById y sincronización, sin cargar toda población para paginar. Dependencias: T036/T039; FR-023/026–030/038, SC-013–015/019.
- [X] T043 [US4] Ampliar `backend/src/main/java/footballmarket/services/PlayerCatalogService.java` y `services/impl/PlayerCatalogServiceImpl.java` para lectura con criterios/opciones, preservando firmas de lectura existentes y lógica de sincronización. Mantener Model como resultado y consistencia read-only REPEATABLE_READ, sin Service→Service de estadísticas o finanzas. Dependencias: T037/T040/T042; FR-023/026–034/038, SC-013–015/018/019.
- [X] T044 [US4] Integrar query y GET literal /filters en `backend/src/main/java/footballmarket/controllers/PlayerController.java`, records de respuesta en `controllers/dtos/responses/PlayerCatalogFiltersResponseDTO.java` y Mapper en `controllers/mappers/PlayerMapper.java`; validar estructura HTTP, describir OpenAPI/DTO/errores 400/401/500 y preservar GET /{id}, /sync y demás contratos. Dependencias: T041/T043; FR-023/026–032/038, SC-013/014/019.
- [X] T045 [US4] Ampliar `frontend/src/features/players/players.service.ts` y `players.mapper.ts` con criterios no vacíos y opciones globales, DTO→Model dentro del Service, signal/authenticated:true; mantener interfaces usadas por detalle y clientes previos. Enviar nombre original con trim, sin filtrar ni normalizar acentos localmente. Dependencias: T038/T044; FR-026–029/032, SC-013/014.
- [X] T046 [US4] Actualizar `frontend/src/features/players/constants/playersQueryKeys.ts`, `hooks/queries/usePlayersQuery.ts`, crear `hooks/queries/usePlayerCatalogFiltersQuery.ts` y exports de sus barrels; key incluye criterios efectivos/page/size, opciones en key propia cacheada sin petición por cada página, cancelación por signal y sin placeholder de otro criterio. Dependencias: T045; FR-028/030/031/034, SC-015/018.
- [X] T047 [US4] Reorganizar `frontend/src/features/players/hooks/pages/usePlayersPage.ts` como única coordinación de texto editado/efectivo, liga/equipo/posición/nacionalidad, sort y page: debounce300ms, reset página0 en transición de criterio efectivo, limpiar cancela debounce y restaura name,asc/todos/página0, errores/reintento sin resultados obsoletos. Dependencias: T046; FR-026–031/034, SC-013–015/017/018.
- [X] T048 [US4] Adaptar `frontend/src/features/players/pages/PlayersPage/PlayerCatalogControls/PlayerCatalogControls.tsx`, su SCSS/barrel y composición en `pages/PlayersPage/PlayersPage.tsx` a opciones reales y callbacks de Page Hook, cinco criterios, nombre A–Z/Z–A y limpiar; opciones globales sin cascadas ni autolimpieza de combinaciones incompatibles. Hijos locales sin hooks/HTTP; sin favoritos/relevancia. Dependencias: T047; FR-024/026–031/037, SC-013–016/019.

### Fase 4 — US1 (P1): tarjetas con estadísticas reales por lote

**Objetivo**: conservar todos los jugadores visibles, incluso sin estadísticas, y eliminar muestras del catálogo. **Criterio independiente**: identidad real, tres valores/cero/ausencia por jugador con fecha/temporada, acceso al detalle y sin consultas por tarjeta. Reutiliza fundamentos de US4, pero es verificable sin aplicar filtros.

#### Pruebas backend — escritura

- [X] T049 [P] [US1] Escribir pruebas puras en `backend/src/test/java/footballmarket/models/PlayerCatalogStatisticsTest.java` para tres claves canónicas, selección individual/JSON prioritario, respaldo solo mismo contexto/fecha/temporada, cero no reemplazado, scraping único, ausencia y fuente por valor, sin tasas nuevas ni selección poblacional. Dependencias: T048; FR-025/033, SC-012/017/018.
- [X] T050 [P] [US1] Escribir `backend/src/test/java/footballmarket/repositories/PlayerCatalogStatisticsBatchTest.java`: lote de IDs paginados con distintos snapshots, confirmación por item asociado, filas huérfanas/no resueltas excluidas, desempate y ausencia sin excluir jugador. Cubrir presupuesto de consultas constante para lote de 1/12/100 IDs mediante instrumentación de pruebas apropiada de Repository, no consultas por jugador; sin ejecución del agente. Dependencias: T048; FR-025/031–033, SC-012/017/018.

#### Backend, frontend e integración

- [X] T051 [US1] Definir Model de resultado/resumen en `backend/src/main/java/footballmarket/models/PlayerCatalogPage.java` y `models/PlayerCatalogStatistics.java`, incluyendo entradas de identidad y tres valores/contexto/fuentes nullable, composición en Model con datos cargados, no DTO ni DB. Mantener jugadores sin métricas y nombres originales. Dependencias: T049; FR-025/032/033, SC-012/014/017.
- [X] T052 [US1] Añadir consulta SQL de lote al `backend/src/main/java/footballmarket/repositories/PlayerStatisticsRepository.java` con CTE/ventana o lateral según plan para ancla y compañero confirmados por IDs de la página, sin leer toda la historia en Java, sin alterar consultas individuales/poblacionales ni disparar sincronización. Dependencias: T050/T051; FR-025/031–033, SC-012/018.
- [X] T053 [US1] Integrar enriquecimiento opcional por lote en `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` bajo la misma transacción read-only REPEATABLE_READ de página/count; includeStatistics=false no solicita estadísticas. Extender `backend/src/test/java/footballmarket/services/PlayerCatalogQueryServiceTest.java` con respuesta enriquecida/legada/ausencia/cero y fixtures Arrange permitidas, sin tests ejecutados ni Repository en Act/Assert. Dependencias: T043/T051/T052; FR-025/032–034, SC-012/014/017/018.
- [X] T054 [US1] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerCatalogEntryResponseDTO.java` y mapear página/resumen en `controllers/mappers/PlayerMapper.java` y Controller: sin flag conservar exactamente estructura y campos; con flag agregar statistics definido en contrato y procedencia nullable. No cambiar PlayerResponseDTO individual ni contratos ajenos. Dependencias: T044/T053; FR-025/032/033, SC-012/014.
- [X] T055 [US1] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para contratos completos sin flag/false/true, cero/nulos/fuentes/contexto por entrada, ausencia deportiva vs fallo500, página vacía y credencial ausente/inválida. Describir campos/nullable en OpenAPI de `controllers/dtos/responses/PlayerCatalogEntryResponseDTO.java`; sin ejecutar tests ni duplicar pruebas directas del Mapper. Dependencias: T041/T054; FR-023/025/032–034/038, SC-012/014/017/019.
- [X] T056 [US1] Integrar enriquecimiento en `frontend/src/features/players/players.service.ts`, `players.mapper.ts` y tipos de `types/`: UI solicita includeStatistics=true, conserva snapshot/contexto por tarjeta, cero/nulo y fuente solo interna. Mantener rutas/Model del detalle y firmas previas; no duplicar DTO en UI. Dependencias: T045/T054/T055; FR-025/031–033, SC-012/014/018.
- [X] T057 [US1] Cambiar `frontend/src/features/players/pages/PlayersPage/PlayerCard/PlayerCard.tsx` y su SCSS para identidad/fotografía/club/liga/nacionalidad/posición reales y tres estadísticas, fecha/temporada identificables, Sin datos separado de0, vínculo al detalle y fallbacks actuales. Retirar solo uso de muestras/etiquetasdemo en catálogo, sin borrar consumidores compartidos ni mostrar fuentes técnicas. Dependencias: T056; FR-023–025/033/035, SC-012/017.

### Fase 5 — US2 (P1): paginación sincronizada

**Objetivo**: navegar el resultado global filtrado sin perder criterios ni mezclar respuestas. **Criterio independiente**: conjunto de varias páginas, extremos y página fuera de rango, total cero, cambio/limpieza desde una página posterior.

- [X] T058 [US2] Completar `frontend/src/features/players/hooks/pages/usePlayersPage.ts` con corrección única a última página válida si baja totalPages, mantener0 con total cero y sin bucle, estado conjunto de criterios/page/resultados/error; no reutilizar resultados previos como actuales. Dependencias: T047/T057; FR-030/031/034, SC-015/017.
- [X] T059 [US2] Integrar `frontend/src/features/players/pages/PlayersPage/PlayersPagination/PlayersPagination.tsx` y `PlayersPage.tsx` con PaginatedPageLayout/Pagination públicos, totales filtrados, selección/limites/ventana vigente sin modificar otras páginas. Dependencias: T058; FR-030/036/037, SC-015/016/019.
- [X] T060 [US2] Ampliar `backend/src/test/java/footballmarket/repositories/PlayerCatalogQueryRepositoryTest.java` y `controllers/PlayerControllerTest.java` con más de dos páginas filtradas, count consistente, nombre repetido en límites, tamaño12, extremos y fuera de rango válido200; mantener default API size20 y orden ID legado. Sin ejecutar. Dependencias: T039/T055/T059; FR-026/029/030/032/038, SC-013–015/019.

### Fase 6 — US3 (P2): fidelidad visual y accesibilidad

**Objetivo**: reproducir mockup con datos verdaderos y UI responsive. **Criterio independiente**: referencia1920×913, navbar104px intacto, 6×2 para12 y adaptación a1280/768/360; teclado y fallback sin hover obligatorio.

- [X] T061 [US3] Ajustar `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss`, `PlayerCard/PlayerCard.module.scss` y `PlayerCatalogControls/PlayerCatalogControls.module.scss` a `referencias-imagenes/06-catalogo-jugadores.png`: cabecera/conteo/búsqueda, barra filtros, seis columnas/dos filas, tarjetas y paginador. Reutilizar tokens/iconos y layouts vigentes, min-width/min-height:0, scroll vertical permitido, sin alturas de Page fijas ni modificar navbar/submenús. Dependencias: T048/T057/T059; FR-024/035/036, SC-016/017.
- [X] T062 [US3] Completar semántica, etiquetas/foco/teclado/status/alert/busy y controles deshabilitados pertinentes en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, `PlayerCard/PlayerCard.tsx`, `PlayerCatalogControls/PlayerCatalogControls.tsx` y `PlayersPagination/PlayersPagination.tsx`; conservar texto de liga/nacionalidad/posición y fallbacks sin depender de color/hover. Sin tests frontend. Dependencias: T061; FR-023/034–037, SC-016/017/019.
- [X] T063 [US3] Actualizar `specs/005-players-page/quickstart.md` con escenarios visuales/humanos concretos para referencia y tamaños responsive, teclado, nombre largo, imágenes, búsqueda rápida/limpiar, cero/ausencia, regresión del detalle y navbar. No ejecutar escenarios ni usar cifras del mockup como fixtures del producto. Dependencias: T060/T062; FR-023–038, SC-012–019.

### Fase 7 — Integración y entrega técnica

- [X] T064 Retirar solo código muerto del catálogo en `frontend/src/features/players/constants/playerSampleStatistics.ts`, `pages/PlayersPage/`, hooks y barrels afectados después del inventario T033; mantener helpers/tipos de demostración con consumidores ajenos y APIs del detalle. Sin refactorización global, funcionalidades nuevas o alteración de SPEC-009. Dependencias: T063; FR-025/032/035, SC-012/014/017.
- [X] T065 Alinear OpenAPI de `backend/src/main/java/footballmarket/controllers/PlayerController.java`/DTOs con `specs/002-player-catalog/contracts/catalog-query.md` y `specs/005-players-page/contracts/players-catalog.md`, pruebas de Controller y cambios efectivos; si aparece alcance nuevo, detener para decisión upstream, no ajustar requisitos para justificar código. Dependencias: T064; FR-023–038, SC-012–019.
- [X] T066 Ejecutar solo builds permitidos después de cambios de código (`./gradlew build -x test` en `backend/`, `npm run build` en `frontend/`), previa inspección de scripts/hooks para excluir tests; revisar salidas generadas/diff y registrar resultados observados en `specs/005-players-page/quickstart.md`. No correr migraciones, EXPLAIN o tests por este enunciado; informar fallos del cambio/entorno/preexistentes. Dependencias: T065; SC-012–019.
- [X] T067 Preparar en `specs/005-players-page/quickstart.md` y `tasks.md` evidencia técnica por FR/SC, archivos y comandos exactos de clases backend escritas para ejecución humana, más instrucciones de comprobación de permisos/GIN/plan de consulta por usuario. Explicitar pendientes visuales y tests, sin marcar gates humanos o pruebas no observadas. Dependencias: T066; FR-023–038, SC-012–019.
- [X] T068 Registrar pg_trgm y propósito/condiciones de función normalizada e índice en `docs/backend/technologies.md` junto con su implementación futura, sin introducir unaccent o paquetes innecesarios; revisar documentación de desarrollo afectada y preservación del nombre original/orden legado. Dependencias: T036/T067; FR-027/029/032, SC-013/014; Constitution IV.

### Dependencias, paralelismo y entrega incremental

Cadena: preparación T033–034 → fundamentos T035–038 → US4 T039–048 → US1 T049–057 → US2 T058–060 → US3 T061–063 → integración T064–068. Todas las dependencias explícitas apuntan a tareas previas; ninguna aprobación o test humano se convierte en Task ejecutable. Los cambios simultáneos en PlayerMapper, PlayerControllerTest, usePlayersPage, DTO/barrels se serializan.

- Tras T038 pueden escribirse T039/T040/T041 en paralelo, en archivos distintos; no ejecutar tests.
- Tras T048 pueden escribirse T049/T050 en paralelo; Repository usa persistencia real, Model no usa Spring/DB.
- US4 provee lectura común; US1 entrega valor por sí misma sin filtros activos una vez esa base existe. US2 completa navegación, US3 la fidelidad visual. Ningún incremento parcial acredita aceptación final de las cuatro US.
- MVP de datos reales requiere fundamentos+US4+US1; finalización requiere todas las US del alcance. Dependency ≠ Blocker: orden técnico no exige dividir Feature o inventar autorización por historia.

### Matriz de cobertura

| Requisitos | Tareas principales | Criterios |
| --- | --- | --- |
| FR-023 | T039–044/T055/T057/T062 | SC-012/019 |
| FR-024 | T048/T057/T061/T062 | SC-016 |
| FR-025/033 | T049–057/T064 | SC-012/017/018 |
| FR-026/027 | T034–037/T039–048/T060/T068 | SC-013/018/019 |
| FR-028/029 | T039–048/T060 | SC-013/014/018 |
| FR-030/031 | T045–048/T050/T056/T058–060 | SC-015/018 |
| FR-032 | T033/T037/T041/T043–045/T053–056/T065 | SC-014 |
| FR-034/035 | T040/T047/T055/T057/T058/T061–064 | SC-017 |
| FR-036/037 | T048/T059/T061–063 | SC-016/019 |
| FR-038 | T037/T039–044/T055/T060 | SC-017/019 |

### Evidencia de generación y límites

setup-tasks.sh --json ejecutado con selección explícita de carpeta005 y plantilla local disponible. 36 tareas nuevas pendientes; T001–T032 preservadas. Hooks before/after Tasks ausentes porque no existe .specify/extensions.yml. No se escribió código, generó/ejecutó migración, ejecutó test/build ni se cambió seguimiento remoto. La autorización actual habilita ANALYZE read-only a continuación, no implementación. Resultado de ANALYZE se informa sin corregir artefactos automáticamente.

---

## Antecedente histórico — T001–T032 y evidencia anterior

**Input**: Documentos de diseño de `/specs/005-players-page/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/players-catalog.md`, `quickstart.md`

**Tests**: No se crean ni ejecutan tests frontend por norma del proyecto. La verificación automatizada permitida es el build; los escenarios funcionales de `quickstart.md` son manuales.

**Organization**: Las tareas se agrupan por historia de usuario para permitir implementación y validación incremental.

## Format: `[ID] [P?] [Story] Description`

- **[ID]**: `T001`…`T032`. Los identificadores son **locales a esta SPEC**: un `Txxx` desnudo de este
  archivo se lee siempre como `SPEC-005/Txxx`. Cuando un documento de cualquier SPEC cite una Task
  externa, debe usar el identificador completo `SPEC-XXX/TYYY`; por ejemplo `SPEC-009/T006` (migración
  del valor compartido de estadísticas), `SPEC-009/T036` (build sin tests), `SPEC-009/T037` (suite
  backend y escenarios manuales) o `SPEC-009/T047` (verificación visual del distintivo de posición y
  del efecto de elevación).

- **[P]**: Puede ejecutarse en paralelo porque afecta archivos distintos y no depende de una tarea incompleta.
- **[Story]**: Historia de usuario a la que pertenece la tarea (`US1`, `US2`, `US3`, `US4`).
- Cada descripción incluye rutas exactas de archivos.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar los recursos estáticos runtime específicos de la feature sin añadir dependencias.

- [X] T001 Copiar `referencias-imagenes/Player Generic-Icon.png`, `Premier League-League.png`, `Bundesliga-League.png`, `Primera Division-League.png`, `Serie A-League.png`, `Ligue 1-League.png`, `Goalkeeper-Player.png`, `Defence-Player.png`, `Midfield-Player.png` y `Offence-Player.png` a `frontend/src/features/players/assets/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear la frontera tipada y de consulta compartida que bloquea todas las historias.

**CRITICAL**: Ninguna historia de usuario puede comenzar hasta completar esta fase.

- [X] T002 [P] Definir `PlayerResponseDTO` y `PlayersPageResponseDTO` en `frontend/src/features/players/types/dtos.ts`, `Player`, `PlayersPage` y `PlayerSampleStatistics` en `frontend/src/features/players/types/models.ts`, y sus exports internos en `frontend/src/features/players/types/index.ts`
- [X] T003 [P] Definir la factory de query keys parametrizada por página y tamaño en `frontend/src/features/players/constants/playersQueryKeys.ts` y exportarla desde `frontend/src/features/players/constants/index.ts`
- [X] T004 Implementar la transformación `PlayerResponseDTO -> Player` y `PlayersPageResponseDTO -> PlayersPage`, incluida la inferencia `active: true`, en `frontend/src/features/players/players.mapper.ts`
- [X] T005 Implementar `playersService.getPage(page, size, signal)` con `GET /players`, `authenticated: true` y parámetros `page`/`size` en `frontend/src/features/players/players.service.ts`
- [X] T006 Implementar la query paginada con tamaño fijo 12, query key por selección y propagación de `AbortSignal` en `frontend/src/features/players/hooks/queries/usePlayersQuery.ts`, exportándola desde `frontend/src/features/players/hooks/queries/index.ts` y `frontend/src/features/players/hooks/index.ts`

**Checkpoint**: La feature puede obtener una página autenticada y exponer Models sin filtrar DTO ni HTTP a la UI.

---

## Phase 3: User Story 1 - Consultar jugadores como usuario autenticado (Priority: P1) MVP

**Goal**: Mostrar la primera página privada del catálogo con hasta 12 tarjetas y feedback de carga, error y vacío.

**Independent Test**: Iniciar sesión, abrir `/players` con jugadores y comprobar hasta 12 tarjetas con nombre, icono genérico, un bloque central con el equipo real, las estadísticas de demostración vigentes y la liga y la posición; abrir la misma URL sin sesión y comprobar la redirección a `/login`.

**Independent Test — redacción original de 2026-09-28, derogada el 2026-10-13**: la prueba independiente de
esta fase citaba `24` partidos, `8` goles y `5` asistencias. Los valores son ahora deterministas por
identificador de jugador y están etiquetados como demostración (FR-005); se conservan aquí solo como
antecedente.

### Implementation for User Story 1

- [X] T007 [P] [US1] Definir las estadísticas comunes `24` partidos, `8` goles y `5` asistencias con sus etiquetas en `frontend/src/features/players/constants/playerSampleStatistics.ts` y exportarlas desde `frontend/src/features/players/constants/index.ts`

**Nota de reconciliación documental — 2026-10-13**: la redacción de T007 corresponde a la ejecución de la
fase original. Los valores fijos `24`/`8`/`5` quedaron **derogados** por FR-005 y fueron sustituidos por
el generador determinista compartido con el detalle en `SPEC-009/T006`, conservando las etiquetas
Partidos, Goles y Asistencias. El archivo vigente es
`frontend/src/features/players/constants/playerSampleStatistics.ts`. No se reescribe la tarea ejecutada:
se identifica el alcance derogado.
- [X] T008 [US1] Crear la tarjeta base con icono genérico, nombre largo contenido, un bloque central con el equipo real y las tres estadísticas etiquetadas, y liga y posición textuales; asegurar que los textos contrasten con el fondo en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx`, `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` y `frontend/src/features/players/components/PlayerCard/index.ts`
- [X] T009 [US1] Crear la página con consulta inicial, cuadrícula de cuatro columnas, máximo 12 tarjetas y estados accesibles de carga, error con reintento y catálogo vacío en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y `frontend/src/features/players/pages/PlayersPage/index.ts`
- [X] T010 [US1] Exponer `PlayerCard` y `PlayersPage` mediante `frontend/src/features/players/components/index.ts`, `frontend/src/features/players/pages/index.ts` y `frontend/src/features/players/index.ts`, y sustituir la ruta provisional por `/players` dentro del `AuthGuard` privado en `frontend/src/app/router/AppRouter.tsx`

**Checkpoint**: US1 funciona de forma independiente como catálogo privado de primera página, incluidos carga, error, reintento y vacío.

---

## Phase 4: User Story 2 - Navegar entre páginas del catálogo (Priority: P1)

**Goal**: Recorrer el catálogo mediante una ventana paginada y conservar como resultado final la selección más reciente.

**Independent Test**: Con al menos ocho páginas, navegar a primera, última y páginas de la ventana vigente (actual, anterior y posterior, con elipsis); verificar límites, selección actual, cambios rápidos y corrección a la última página válida cuando disminuye el total.

**Independent Test — redacción original, derogada el 2026-10-13**: la prueba citaba la ventana de tres
anteriores y tres posteriores. FR-014 la sustituye por la ventana vigente con elipsis; se conserva aquí
solo como antecedente.

### Implementation for User Story 2

- [X] T011 [P] [US2] Implementar el cálculo puro de destinos visibles con página actual, hasta tres anteriores/posteriores y extremos sin duplicados en `frontend/src/features/players/utils/getVisiblePlayerPages.ts` y exportarlo desde `frontend/src/features/players/utils/index.ts`

**Nota de reconciliación documental — 2026-10-13**: la ventana de tres anteriores y tres posteriores de
T011 quedó **derogada** por FR-014. El cálculo vigente del componente compartido
`shared/components/Pagination` expone la página actual, la anterior y la posterior, los extremos y la
elipsis. Se conserva la redacción original como antecedente de la tarea ejecutada.
- [X] T012 [P] [US2] Crear el paginador accesible con numeración visual desde uno, estado actual y controles primera/última deshabilitados en límites en `frontend/src/features/players/components/PlayersPagination/PlayersPagination.tsx`, `frontend/src/features/players/components/PlayersPagination/PlayersPagination.module.scss` y `frontend/src/features/players/components/PlayersPagination/index.ts`
- [X] T013 [US2] Crear el Page Hook que coordina `selectedPage`, la query y la corrección a `totalPages - 1` para selecciones que dejan de existir en `frontend/src/features/players/hooks/pages/usePlayersPage.ts`, exportándolo desde `frontend/src/features/players/hooks/pages/index.ts` y `frontend/src/features/players/hooks/index.ts`
- [X] T014 [US2] Integrar `usePlayersPage` y `PlayersPagination` en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, actualizar sus estilos en `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y exportar el componente desde `frontend/src/features/players/components/index.ts`

**Checkpoint**: US2 permite recorrer cualquier página ofrecida y mantiene alineados catálogo, carga y selección más reciente.

---

## Phase 5: User Story 3 - Reconocer estado y clasificación visual (Priority: P2)

**Goal**: Diferenciar liga, posición y estado activo mediante recursos visuales, colores suaves y texto accesible.

**Independent Test**: Revisar tarjetas de las cinco ligas y cuatro posiciones conocidas, además de valores desconocidos, y comprobar recursos, texto, contraste y fallback neutro.

**Independent Test — redacción original, derogada el 2026-10-13**: la prueba comprobaba además la entrada
lateral de `Activo` solo durante hover. FR-010 la sustituye por elevación y refuerzo de brillo, y el
estado `Activo` permanece visible de forma permanente por FR-011. La prueba vigente añade el recorrido con
teclado y en pantalla táctil (T031 y `SPEC-009/T047`).

### Implementation for User Story 3

- [X] T015 [P] [US3] Definir mappings abiertos para recursos y variantes visuales de las cinco ligas y cuatro posiciones, con fallback neutro, en `frontend/src/features/players/constants/playerClassifications.ts` y exportarlos desde `frontend/src/features/players/constants/index.ts`
- [X] T016 [US3] Incorporar imágenes con alternativa textual, variantes de liga/posición y el indicador `Activo` animado desde la izquierda sin reflujo en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss`

**Checkpoint**: US3 comunica todas las clasificaciones y el estado sin depender únicamente de imágenes o color.

---

## Phase 6: User Story 4 - Visualizar controles de filtrado (Priority: P3)

**Goal**: Mostrar controles interactivos de búsqueda, liga y posición que no alteren consulta, resultados ni página.

**Independent Test**: Escribir y seleccionar opciones en los tres controles y comprobar que conservan su estado visual sin cambiar tarjetas, orden, paginación, query key ni solicitudes HTTP.

### Implementation for User Story 4

- [X] T017 [P] [US4] Crear los controles identificados de búsqueda, liga y posición como inputs controlados en `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx`, `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.module.scss` y `frontend/src/features/players/components/PlayerCatalogControls/index.ts`
- [X] T018 [US4] Ampliar el estado local de `usePlayersPage` con búsqueda, liga y posición sin incorporarlos a query keys, parámetros HTTP ni cambios de página en `frontend/src/features/players/hooks/pages/usePlayersPage.ts`
- [X] T019 [US4] Integrar `PlayerCatalogControls` sobre la cuadrícula en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, ajustar la composición en `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss` y exportarlo desde `frontend/src/features/players/components/index.ts`

**Checkpoint**: US4 presenta los tres controles previstos sin modificar el catálogo.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificar coherencia transversal, accesibilidad y criterios operativos de la feature completa.

- [X] T020 Revisar semántica, foco visible, `aria-busy`, `role="status"`, `role="alert"`, textos alternativos y ausencia de scroll horizontal a 1280x720 en `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx`, `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss`, `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx`, `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx` y `frontend/src/features/players/components/PlayersPagination/PlayersPagination.tsx`
- [ ] T021 Ejecutar manualmente los escenarios de acceso, tarjetas, paginación, controles, estados y fallbacks descritos en `specs/005-players-page/quickstart.md`
- [ ] T022 Ejecutar `npm run build` desde `frontend/` usando el script definido en `frontend/package.json` y confirmar que no se generaron cambios versionables en `frontend/dist/` ni archivos `frontend/*.tsbuildinfo`

  **Evidencia 2026-10-13 (ejecución del agente, salida verificable)**: `npm run build` → EXIT=0,
  `✓ built in 2.16s`. `frontend/dist/` está cubierto por `frontend/.gitignore` y no produce cambios
  versionables. **Pendiente**: `frontend/tsconfig.tsbuildinfo` está versionado y el build lo modifica, de
  modo que el criterio «no se generaron cambios versionables en `frontend/*.tsbuildinfo`» **no se cumple**.
  T022 permanece `[ ]` hasta que el responsable decida si restaura ese archivo y deja de versionarse.
  Nota adicional: `npm run biome:check` falla en el estado actual del árbol, con desviaciones de formato en
  `app/layouts/AppLayout`, `app/layouts/PageLayout`, `app/layouts/PaginatedPageLayout` y
  `shared/components/Pagination`, más `assist/source/organizeImports` y formato en
  `features/players/assets/teams/`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; puede comenzar inmediatamente.
- **Foundational (Phase 2)**: Depende de Setup y bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Foundational y constituye el MVP.
- **US2 (Phase 4)**: Depende de US1 porque amplía la consulta y la página con navegación.
- **US3 (Phase 5)**: Depende de US1; puede desarrollarse en paralelo con US2 porque modifica la tarjeta mientras US2 modifica paginación y coordinación de página.
- **US4 (Phase 6)**: Depende de US1; su integración final debe coordinarse con US2 porque ambas historias modifican `usePlayersPage.ts` y `PlayersPage.tsx`.
- **Polish (Phase 7)**: Depende de todas las historias incluidas en la entrega.

### User Story Dependencies

- **US1 (P1)**: Comienza tras Foundational y no depende de otra historia.
- **US2 (P1)**: Reutiliza y amplía la página entregada por US1.
- **US3 (P2)**: Reutiliza la tarjeta de US1, sin depender de US2 ni US4.
- **US4 (P3)**: Reutiliza la página de US1; no depende funcionalmente de US2 o US3, aunque comparte archivos de integración con US2.

### Dependency Graph

```text
Setup -> Foundational -> US1 -> US2 -> Polish
                           |----> US3 --|
                           |----> US4 --|
```

### Within Each User Story

- Crear primero constantes, utilidades o estado requeridos por los componentes consumidores.
- Implementar componentes antes de integrarlos en `PlayersPage`.
- Actualizar los barrels en la misma tarea que introduce el consumidor externo correspondiente.
- Completar el checkpoint de la historia antes de avanzar secuencialmente a la siguiente prioridad.

### Parallel Opportunities

- T002 y T003 pueden ejecutarse en paralelo después de T001.
- T007 puede avanzar en paralelo con trabajo que no dependa todavía de las estadísticas.
- T011 y T012 pueden ejecutarse en paralelo después de US1.
- US2 y US3 pueden avanzar en paralelo tras US1, coordinando únicamente la exportación final de componentes.
- T015 puede ejecutarse en paralelo con las tareas de US2.
- T017 puede prepararse en paralelo con US2 y US3; T018-T019 deben integrar sobre la versión vigente del Page Hook y la Page.

---

## Parallel Example: User Story 1

```text
Task T007: Definir estadísticas de muestra en frontend/src/features/players/constants/playerSampleStatistics.ts
Task preparatoria independiente: revisar la composición y estados existentes de frontend/src/features/players/pages/PlayersPage/
```

## Parallel Example: User Story 2

```text
Task T011: Implementar frontend/src/features/players/utils/getVisiblePlayerPages.ts
Task T012: Crear frontend/src/features/players/components/PlayersPagination/
```

## Parallel Example: User Story 3

```text
Task T015: Definir mappings en frontend/src/features/players/constants/playerClassifications.ts
Task paralela de otra historia: implementar T011-T013 de paginación sin modificar PlayerCard
```

## Parallel Example: User Story 4

```text
Task T017: Crear frontend/src/features/players/components/PlayerCatalogControls/
Task paralela de otra historia: completar T015-T016 de clasificación visual sin modificar el Page Hook
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational.
3. Completar Phase 3: US1.
4. Detenerse y validar US1 de forma independiente: acceso privado, primera página, tarjetas y estados.
5. Ejecutar el build permitido antes de demostrar el MVP.

### Incremental Delivery

1. Entregar Setup + Foundational como base técnica interna.
2. Añadir US1 para obtener el catálogo privado visible como MVP.
3. Añadir US2 para recorrer el catálogo completo.
4. Añadir US3 para completar clasificación y estado visual.
5. Añadir US4 para completar la composición de controles sin filtrado funcional.
6. Ejecutar Polish y la validación manual completa sin atribuir cobertura de tests al build.

### Parallel Team Strategy

1. El equipo completa Setup y Foundational en orden.
2. Una persona completa US1 para estabilizar la Page y PlayerCard base.
3. Tras US1, una persona trabaja US2, otra US3 y otra prepara T017 de US4.
4. La integración de T018-T019 se realiza sobre el Page Hook y la Page ya actualizados por US2.

---

## Notes

- Las tareas `[P]` afectan archivos diferentes y no dependen de tareas aún incompletas.
- No añadir parámetros de búsqueda, liga, posición u orden a `playersService` ni a `playersQueryKeys`.
- No importar DTO, Service ni HTTP desde Pages o Components.
- No añadir dependencias ni infraestructura de tests frontend.
- El build permitido no ejecuta ni acredita tests; T021 sigue siendo una validación manual.

## Phase 8: Convergence

- [X] T023 CRITICAL Validar en `frontend/src/features/players/players.service.ts` y `frontend/src/features/players/players.mapper.ts` que la respuesta externa contiene los campos requeridos, metadatos de paginación coherentes y como máximo 12 jugadores, rechazando datos inválidos como error recuperable antes de exponer Models per Constitution §3 y FR-002/FR-006 (partial)
- [X] T024 Reubicar o reservar un área para el indicador `Activo` en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` para que su entrada lateral por hover no oculte el icono, nombre ni otros datos esenciales y no produzca reflujo per FR-010 y US3/AC3 (contradicts)
- [X] T025 Reconocer `Primera División` en `frontend/src/features/players/constants/playerClassifications.ts` y `frontend/src/features/players/components/PlayerCatalogControls/PlayerCatalogControls.tsx`, conservando un alias explícito para `Primera Division` si ambos valores forman parte del contrato, para aplicar el recurso y color propios per FR-007/FR-008 y US3/AC1-2 (partial)
- [X] T026 Coordinar en `frontend/src/features/players/hooks/pages/usePlayersPage.ts` y `frontend/src/features/players/pages/PlayersPage/PlayersPage.tsx` las tarjetas, el total y el paginador como un único estado de consulta, ocultando o deshabilitando navegación obsoleta durante carga o error per FR-016/FR-017 y SC-005/SC-008 (partial)
- [X] T027 Hacer legibles los nombres largos en `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` mediante ajuste controlado o una alternativa accesible por teclado, sin invadir estadísticas ni clasificaciones per Edge case: long names y T008 (partial)

---

## Phase 9: Sincronización documental posterior al rediseño (2026-10-13)

**Purpose**: Registrar la decisión de producto que conserva el rediseño manual del frontend y
alinear los artefactos de esta SPEC con la implementación real. No se modifica código.

- [X] T028 Registrar en `specs/005-players-page/spec.md` la sesión de decisiones del 2026-10-13, los requisitos derogados con su reemplazo y los requisitos que siguen pendientes de verificación
- [X] T029 Sustituir FR-005, FR-006, FR-014 y FR-020, e incorporar FR-021 y FR-022; actualizar US1, US2, SC-002, SC-003, SC-004 y añadir SC-009 y SC-010
- [X] T030 Sincronizar `data-model.md`, `plan.md`, `research.md`, `contracts/players-catalog.md` y `quickstart.md` con el generador determinista compartido, la cuadrícula responsive, la paginación compartida y los recursos gráficos decorativos
- [ ] T031 Verificar en el navegador el comportamiento responsive a 1280, 768 y 360 píxeles, los valores deterministas por jugador, la etiqueta de posición, el efecto de elevación y la degradación de la bandera nacional. **Pendiente**: requiere ejecución manual del usuario
- [X] T032 Registrar la decisión humana del 2026-10-13 que sustituye FR-009 por una etiqueta textual de posición y FR-010 por el efecto de elevación y refuerzo de brillo, sin reintroducir los elementos visuales anteriores. Sustituciones, justificaciones y antecedentes documentados en `specs/005-players-page/spec.md`

**Nota**: T031 y T032 permanecen abiertas. El rediseño conservado no cierra por sí mismo estos
requisitos, y su cumplimiento no puede inferirse de la existencia del código.

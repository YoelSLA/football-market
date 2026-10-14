# Índice de trazabilidad con Trello

## Regularización del estado SDD tras Code Review — SPEC-009 — 2026-10-13

Operación autorizada de trazabilidad. Aplica el retorno a IMPLEMENT exigido por
[workflow SDD §7.7 y §7.10](../docs/development/sdd-workflow.md) tras un CODE REVIEW rechazado. Sin
código, sin SPEC-008, sin Constitution, sin tests ni builds, sin commits, sin push, sin cambio de rama,
sin aprobar gates y sin Converge. Únicas tarjetas afectadas: épica y US01–US04 de SPEC-009.

### Transiciones realizadas

| Tarjeta | Antes | Después | Motivo |
| --- | --- | --- | --- |
| [SPEC-009 US01](https://trello.com/c/lkiI1Wxd) | CODE REVIEW | **IN PROGRESS** | CODE REVIEW rechazado por M1 (INCUMPLIMIENTO de FR-006) |
| [SPEC-009 US04](https://trello.com/c/3A5njlA4) | CODE REVIEW | **IN PROGRESS** | CODE REVIEW rechazado por M2 (no conformidad de formato) |
| [SPEC-009 US02](https://trello.com/c/IRXEIntV) | CODE REVIEW | CODE REVIEW (sin mover) | Evaluación de impacto: ningún defecto dentro de su alcance |
| [SPEC-009 US03](https://trello.com/c/J01xCDGc) | CODE REVIEW | CODE REVIEW (sin mover) | M3 afecta a su fuente pero no es defecto corregible; queda como verificación pendiente |
| [Épica SPEC-009](https://trello.com/c/KViJvig1) | IN PROGRESS | IN PROGRESS (sin mover) | La Feature conserva IN PROGRESS; no habilita TESTING final mientras sus US no estén DONE |

Criterio aplicado: Constitution §8 «invalidar solo por impacto sobre el alcance evaluado; no resetear toda
la Feature». US02 y US03 permanecen en CODE REVIEW, con constancia escrita de que su estado es deliberado
y no un olvido.

### Hallazgos de la revisión y su resolución

| Hallazgo | Severidad inicial | Resolución | Ámbito |
| --- | --- | --- | --- |
| H1 recursos gráficos ausentes en el detalle | HIGH | Reclasificado como **discrepancia documental**: FR-023 habilita, no exige. Corregidos `quickstart.md` y la matriz de trazabilidad | Documental |
| M1 fallback de retrato no reiniciado | MEDIUM | **Corregido**: `key={detail.player.id}` en `PlayerOverview` | US01 |
| M2 formato de `AppLayout` | MEDIUM | **Corregido**: tabulación conforme a `biome.jsonc` | US04 |
| M3 base cero no reproducible | MEDIUM | **Documentado**, generador intacto; verificación controlada propuesta | US03 (verificación) |
| M4 duplicación de logout | MEDIUM | **Descartado**: excluyentes por CSS en `Navbar.module.scss` | US04 (sin defecto) |
| L1–L6 | LOW | Deuda técnica, sin corrección | — |

### Evidencia registrada

| Tarjeta | Comentario |
| --- | --- |
| Épica SPEC-009 | `[SDD GATE] CODE REVIEW / REJECTED — retorno parcial a IMPLEMENT` |
| US01 | `[SDD GATE] CODE REVIEW / REJECTED — retorno a IMPLEMENT` |
| US04 | `[SDD GATE] CODE REVIEW / REJECTED — retorno a IMPLEMENT` |
| US02 | `[SDD SYNC] EVALUACIÓN DE IMPACTO — CODE REVIEW sigue vigente` |
| US03 | `[SDD SYNC] EVALUACIÓN DE IMPACTO — CODE REVIEW sigue vigente` |

La declaración humana «Apruebo el code review» del 2026-10-13 se conserva en el historial de comentarios
como antecedente y queda **superada** por los hallazgos emitidos posteriormente. No se interpreta como
aprobación efectiva. No se aprobó ningún gate.

### Condiciones de reingreso a CODE REVIEW

Conforme a §7.7 (rechazo → corrección en IMPLEMENT → nuevo report/review obligatorio):

1. Verificar en navegador la corrección de M1: navegar de un jugador sin imágenes válidas a otro con
   retrato válido y comprobar que este muestra sus propias imágenes.
2. Obtener salida verificable de `npm run build` y `npm run biome:check` (`SPEC-009/T036`), que ahora
   incluye el archivo corregido en M2.
3. Presentar un Code Review Report nuevo y completo, con el informe de la revisión del 2026-10-13 como
   antecedente.
4. Obtener la aprobación humana explícita del checklist completo.
5. Persistir el `[SDD GATE] CODE REVIEW / APPROVED` en la US y en la épica antes de mover la tarjeta.
6. Para US01 y US04 solo; US02 y US03 no se reingresan sin el informe que las cubra.

### Estado final por US

| US | Estado | Gate CODE REVIEW | Pendientes |
| --- | --- | --- | --- |
| US01 | IN PROGRESS | Rechazado; pendiente de reingreso | M1 verificado en navegador; T036; T037 |
| US02 | CODE REVIEW | Pendiente de aprobación humana | Verificación de FR-009–FR-012, FR-015, FR-017; T036; T037 |
| US03 | CODE REVIEW | Pendiente de aprobación humana | Verificación de FR-013–FR-015, SC-006/SC-007; base cero documentada; T036; T037 |
| US04 | IN PROGRESS | Rechazado; pendiente de reingreso | M2 verificado con `biome:check`; L3/L4 confirmadas en navegador; T036; T037 |

La Feature permanece **IN PROGRESS**: TESTING final, Converge y FEATURE COMPLETION AUTHORIZATION siguen
pendientes. `SPEC-005/T031` y `SPEC-009/T047` continúan pendientes y no forman parte de esta operación.

Las secciones anteriores a esta entrada se conservan íntegras como antecedentes históricos.

## Conciliación documental posterior a la sincronización — 2026-10-13

Operación exclusivamente documental, con **Trello bajo restricción de no modificación**. Se corrigieron
referencias normativas locales que contradecían las decisiones aprobadas el 2026-10-13, identificando como
**derogadas** las redacciones anteriores. Sin cambios funcionales, sin cambios de estados SDD, sin aprobar
gates, sin código, sin SPEC-008, sin Constitution, sin tests, sin builds, sin commits, sin push y sin
cambio de rama. La rama activa sigue siendo `feat/009-player-detail`.

### Referencias normativas corregidas

| Archivo | Referencia obsoleta | Corrección |
| --- | --- | --- |
| `005-players-page/contracts/players-catalog.md` | Estadísticas fijas de muestra `24`/`8`/`5` presentadas como descripción vigente del contrato | Marcadas como **derogadas**; la redacción vigente deriva del identificador del jugador (SPEC-005 FR-005; SPEC-009 FR-011). El contrato HTTP no cambia |
| `005-players-page/tasks.md` — Phase 3 | `Independent Test` de US1 con `24`/`8`/`5` | Reescrito con las estadísticas vigentes; la redacción original se conserva identificada como derogada |
| `005-players-page/tasks.md` — T007 | Descripción de la tarea con valores fijos comunes | Se conserva la tarea ejecutada sin reescribirla y se añade nota de reconciliación que identifica el alcance derogado por FR-005 y por `SPEC-009/T006` |
| `005-players-page/tasks.md` — Phase 4 | `Independent Test` de US2 con ventana de tres anteriores/posteriores | Reescrito con la ventana vigente; la redacción original se conserva como derogada |
| `005-players-page/tasks.md` — T011 | Descripción de la tarea con hasta tres anteriores/posteriores | Nota de reconciliación: la ventana queda derogada por FR-014; el componente vigente es `shared/components/Pagination` |
| `005-players-page/tasks.md` — Phase 5 | `Independent Test` de US3 con entrada lateral de `Activo` solo durante hover | Reescrito; la entrada lateral por hover queda derogada por FR-010 y el estado `Activo` permanece visible por FR-011 |
| `009-player-detail/spec.md` — Clarifications 2026-10-12 | Respuesta «aplicada en esta SPEC solo al detalle» | Marcada como antecedente sustituido el 2026-10-13; el resto de la respuesta se conserva |
| `009-player-detail/tasks.md` — Decisiones de Clarify | Punto 5, navbar aplicada solo al detalle | Marcado **DEROGADO**; vigente FR-018 y `SPEC-009/T032`. Los puntos 1 a 4 permanecen vigentes |

### Referencias cruzadas de Tasks verificadas

Se resolvió la ambigüedad de los identificadores `Txxx`, que son **locales a cada SPEC** y colisionan
frecuentemente entre SPEC-005 y SPEC-009 (`T007`, `T011`, `T024`, `T031`, `T036`, `T037`, `T047` existen
en ambas). Ambos `tasks.md` incorporan ahora la convención `SPEC-XXX/TYYY` para toda referencia externa.

| Tarea | Ambigua entre SPEC-005 y SPEC-009 | Resolución documentada |
| --- | --- | --- |
| `SPEC-005/T007` y `SPEC-009/T007` | Sí | T007 de SPEC-005: constantes de estadísticas de muestra, alcance derogado. T007 de SPEC-009: tests de Controller del endpoint individual |
| `SPEC-005/T011` y `SPEC-009/T011` | Sí | T011 de SPEC-005: cálculo de destinos visibles, ventana derogada. T011 de SPEC-009: consulta individual en el repositorio |
| `SPEC-005/T024` y `SPEC-009/T024` | Sí | T024 de SPEC-005: indicador `Activo` por hover, ya anotado como contradictorio con FR-010. T024 de SPEC-009: composición de la tarjeta de rendimiento |
| `SPEC-005/T031` y `SPEC-009/T031` | Sí | T031 de SPEC-005: verificación visual del catálogo. T031 de SPEC-009: creación del layout de aplicación. Solo la primera está pendiente |
| `SPEC-005/T036`, `T037` y `SPEC-009/T036`, `T037` | Sí | Las de SPEC-009 son build sin tests y suite backend más escenarios manuales, ambas pendientes. Las de SPEC-005 son escenarios manuales de `quickstart.md` y build frontend |
| `SPEC-005/T047` y `SPEC-009/T047` | Sí | Ambas pendientes: legibilidad del distintivo de posición, efecto de elevación y ausencia de dependencia del hover |
| `SPEC-005/T031` → `SPEC-009/T047` | Dependencia cruzada | La verificación visual del catálogo es la misma evidencia que `SPEC-009/T047`; se registra en ambas, sin duplicar estado de ejecución |

### Estado de `SPEC-009/T042`

T042 («registrar en `specs/trello.md` y en las tarjetas US la ampliación de alcance del 2026-10-13»)
**permanece sin marcar**.

- Material ya producido y verificado: la sección de sincronización de este índice y las descripciones de
  las diez tarjetas, con el comentario `[SDD SYNC EVIDENCE] TEMPLATES SDD ↔ TRELLO — 2026-10-13` en
  ambas épicas.
- Evidencia que falta: (1) autorización humana explícita del responsable para cerrar la sincronización
  operativa de Trello, y (2) su registro como comentario `[SDD GATE]` en la épica de SPEC-009. Esa
  escritura no se realizó porque esta conciliación se ejecutó con Trello bajo restricción de no
  modificación.
- Marcarla completada sin esa autorización atribuiría al agente una validación humana inexistente. T042 no
  sustituye CODE REVIEW, TESTING ni Converge.

### Estados documentales: discrepancias verificadas, no corregidas

1. `009-player-detail/spec.md` conserva `Status: Ready for planning` pese a que la preparación global y
   ANALYZE están validados y la épica alcanzó READY. Según `specs/README.md`, el estado que corresponde es
   `Ready for implementation`. **No se modificó**: el cambio de `Status` es una transición documental
   atribuible al avance SDD y requiere decisión explícita del responsable; esta operación solo debía
   corregir referencias contradictorias con las decisiones del 2026-10-13.
2. `005-players-page/spec.md` conserva `Status: Draft` mientras la tarjeta está en DONE histórico y su
   `SDD Lifecycle` está 8/8. El workflow (§4) establece que SPEC-001–005 conservan DONE histórico sin gates
   retroactivos. **No se modificada** por el mismo motivo.
3. `005-players-page/tasks.md` conserva en el Dependency Graph y en la estrategia incremental referencias a
   la ventana de paginación propia de la feature; la decisión del 2026-10-13 la sustituye por el
   componente compartido. El grafo describe orden de ejecución, no comportamiento normativo, por lo que no
   se reescribe.

### Pendientes reales antes de Code Review

- **SPEC-005**: `SPEC-005/T021` (escenarios manuales de `quickstart.md`), `SPEC-005/T022` (build frontend),
  `SPEC-005/T031` y `SPEC-009/T047` (verificación visual en navegador: responsive 1280/768/360, valores
  deterministas, distintivo de posición, elevación y refuerzo de brillo, degradación de bandera), y la
  verificación visual de FR-005, FR-006, FR-008, FR-009, FR-010, FR-022, SC-003, SC-007, SC-010 y SC-011
  declarada en `spec.md`.
- **SPEC-009**: `SPEC-009/T036` (salida verificable del build sin tests), `SPEC-009/T037` (suite backend y
  escenarios manuales de `quickstart.md`, a cargo del usuario), y la verificación en navegador de
  FR-020/SC-009 y FR-014/SC-006. `SPEC-009/T042` requiere autorización humana, no verificación técnica.
- Ambos: sin credenciales de Code Review o Testing registrables; el agente no ejecuta tests ni builds.

Las secciones anteriores a esta entrada se conservan íntegras como antecedentes históricos.

## Sincronización SDD ↔ Trello de plantillas, Épicas y User Stories — 2026-10-13

Operación de documentación y trazabilidad. Se aplicó el template estándar de épica y de User Story a las
diez tarjetas existentes de SPEC-005 y SPEC-009, y se creó el checklist `Implementation Tasks` en las ocho
tarjetas US. **No** se movió ninguna tarjeta, **no** se aprobó ningún gate, **no** se crearon tarjetas,
**no** se modificó código, SPEC-008, Constitution ni comandos SDD, y no se ejecutaron tests, builds,
commits, push ni cambios de rama.

Fuente normativa única de los templates y de las reglas de jerarquía:
[workflow operativo SDD](../docs/development/sdd-workflow.md) §3 y §4. Este índice registra resultados y
evidencia; no redefine reglas.

### Jerarquía y reglas aplicadas

- Jerarquía oficial: **EPIC → User Story → Tasks `Txxx`**.
- Las épicas **no** tienen subtareas técnicas ni checklist `Implementation Tasks`. Conservan únicamente
  el checklist `SDD Lifecycle`.
- Las Tasks se representan como ítems de checklist `Implementation Tasks` dentro de la US
  correspondiente. No se crearon tarjetas para `Txxx`.
- Las tareas transversales (`Setup`, `Foundational`, `Polish`, `Convergence`, sincronización documental)
  se registran como dependencias en las US que habilitan y conservan su seguimiento en `tasks.md`. No se
  duplica su estado de ejecución.
- El estado de un ítem del checklist refleja `tasks.md`; no aprueba la US ni ningún gate.
- Las decisiones funcionales aprobadas el 2026-10-13 y las decisiones anteriores derogadas se conservan
  como historial identificado en cada tarjeta.

### Tarjetas sincronizadas

| SPEC | Tarjeta | Trello ID | URL | Estado anterior | Estado final | Resultado |
| --- | --- | --- | --- | --- | --- | --- |
| 005 | `[EPIC] SPEC-005 - Página de jugadores` | `6ac5a4dc6d628b000cb79f18` | https://trello.com/c/pP8Qfa6M | DONE | DONE (sin mover) | Template de épica aplicado; `SDD Lifecycle` 8/8 conservado; sin subtareas técnicas |
| 005 | `SPEC-005 \| US01 - Consultar jugadores como usuario autenticado` | `6abc70dd336b06cbc2871e3e` | https://trello.com/c/7WWHGU58 | DONE | DONE (sin mover) | Template aplicado; `Implementation Tasks` con T007–T010 |
| 005 | `SPEC-005 \| US02 - Navegar entre páginas del catálogo` | `6ac59708c685f348ab496e1c` | https://trello.com/c/Z2qrRpuC | DONE | DONE (sin mover) | Template aplicado; `Implementation Tasks` con T011–T014 |
| 005 | `SPEC-005 \| US03 - Reconocer estado y clasificación visual` | `6ac59708b3e3503db82bfe29` | https://trello.com/c/XOeC5jC2 | DONE | DONE (sin mover) | Template aplicado; `Implementation Tasks` con T015–T016 |
| 005 | `SPEC-005 \| US04 - Visualizar controles de filtrado` | `6ac59709569bf65fd70b29b2` | https://trello.com/c/UatbQHJp | DONE | DONE (sin mover) | Template aplicado; `Implementation Tasks` con T017–T019 |
| 009 | `[EPIC] SPEC-009 - Player Detail` | `6abc70f3c1ec56812e028155` | https://trello.com/c/KViJvig1 | IN PROGRESS | IN PROGRESS (sin mover) | Template de épica aplicado; `SDD Lifecycle` 6/8 conservado; sin subtareas técnicas |
| 009 | `SPEC-009 \| US01 - Consultar la identidad real del jugador` | `6acd6183a69c1dc6dd4f190b` | https://trello.com/c/lkiI1Wxd | CODE REVIEW | CODE REVIEW (sin mover) | Template aplicado; `Implementation Tasks` con T007–T021 |
| 009 | `SPEC-009 \| US02 - Consultar estadísticas de demostración coherentes` | `6acd6188bff304f344fb583d` | https://trello.com/c/IRXEIntV | CODE REVIEW | CODE REVIEW (sin mover) | Template aplicado; `Implementation Tasks` con T022–T025 |
| 009 | `SPEC-009 \| US03 - Consultar cotización y evolución de demostración` | `6acd61895714dd80e0f4bc36` | https://trello.com/c/J01xCDGc | CODE REVIEW | CODE REVIEW (sin mover) | Template aplicado; `Implementation Tasks` con T026–T029 |
| 009 | `SPEC-009 \| US04 - Utilizar un dashboard adaptable con navegación real` | `6acd618a93021c82ab068eac` | https://trello.com/c/3A5njlA4 | CODE REVIEW | CODE REVIEW (sin mover) | Template aplicado; `Implementation Tasks` con T030–T034 |

### Tasks asociadas por User Story

| SPEC | US | Tasks | Total | Completadas | Pendientes | Transversales referenciadas |
| --- | --- | --- | --- | --- | --- | --- |
| 005 | US01 | `SPEC-005/T007`–`T010` | 4 | 4 | 0 | `T001`; `T002`–`T006`; `T020`–`T032` |
| 005 | US02 | `SPEC-005/T011`–`T014` | 4 | 4 | 0 | `T001`; `T002`–`T006`; `T020`–`T032` |
| 005 | US03 | `SPEC-005/T015`–`T016` | 2 | 2 | 0 | `T001`; `T002`–`T006`; `T020`–`T032`; `SPEC-005/T031`, `SPEC-009/T047` |
| 005 | US04 | `SPEC-005/T017`–`T019` | 3 | 3 | 0 | `T001`; `T002`–`T006`; `T020`–`T032` |
| 009 | US01 | `SPEC-009/T007`–`T021` | 15 | 15 | 0 | `T001`–`T006`; `T035`–`T037`; `T042` |
| 009 | US02 | `SPEC-009/T022`–`T025` | 4 | 4 | 0 | `T001`–`T006`; `T036`; `T037`; `SPEC-005/T047` |
| 009 | US03 | `SPEC-009/T026`–`T029` | 4 | 4 | 0 | `T001`–`T006`; `T036`; `T037` |
| 009 | US04 | `SPEC-009/T030`–`T034` | 5 | 5 | 0 | `T001`–`T006`; `T035`; `T036`; `T037`; `SPEC-005/T031`, `SPEC-009/T047` |

Los identificadores `Txxx` son locales a cada SPEC. Las filas de la columna «Tasks» se leen siempre con el
prefijo de su SPEC; las referencias de la última columna usan el identificador completo `SPEC-XXX/TYYY`
porque son externas a la US.

Ninguna tarea transversal se asignó como tarea exclusiva de una US ni se duplicó en dos checklists.

### Evidencia

- Comentario `[SDD SYNC EVIDENCE] TEMPLATES SDD ↔ TRELLO — 2026-10-13` persistido en las dos tarjetas
  épica (SPEC-005 `6ac5a4dc…` y SPEC-009 `6abc70f3…`).
- Lectura posterior de las diez tarjetas: nombres, etiquetas, listas, checklists y descripciones verificados
  contra lo esperado. Listas conservadas: SPEC-005 en DONE (`6abc70c5f846ad9d38511d2e`), épica SPEC-009 en
  IN PROGRESS (`6abc70c14ade21afbe942c7b`), US de SPEC-009 en CODE REVIEW (`6ac64ce4a5b9e63fa940cb14`).
- Sin operaciones fallidas.

### Gates pendientes tras la sincronización

- SPEC-005: no se abre ningún gate. DONE es estado histórico heredado y **no** constituye aceptación de
  las decisiones del 2026-10-13. Verificaciones pendientes: T021, T022, T031, T047.
- SPEC-009: CODE REVIEW y TESTING por US sin aprobar; Converge y FEATURE COMPLETION AUTHORIZATION
  pendientes. Verificaciones pendientes: T036, T037, T042, T047.
- No se inició Code Review, no se inició Testing y no se ejecutó Converge.

### Discrepancias detectadas (no corregidas por esta operación)

1. SPEC-005: el checklist `SDD Lifecycle` de la épica está 8/8 marcado mientras `spec.md` conserva
   `Status: Draft`. Es un antecedente histórico; no se reconstruyen ni se alteran gates.
2. **SPEC-005**: `tasks.md` describía la prueba independiente y el objetivo de US1 con los valores fijos
   24/8/5 y la ventana de paginación ±3, mientras la decisión del 2026-10-13 los sustituye en `spec.md`.
   **Resuelto en la conciliación posterior del 2026-10-13**: las redacciones normativas se actualizaron y
   las originales quedaron identificadas como derogadas. Las US de Trello reflejan la redacción vigente
   de `spec.md` desde la sincronización.
3. **SPEC-009**: `tasks.md` mantenía en la «Decisiones de Clarify» el punto 5 con la navbar aplicada solo
   al detalle, y `spec.md` conserva Status `Ready for planning` pese a haber superado READY e IMPLEMENT.
   **Parcialmente resuelto en la conciliación posterior**: el punto 5 quedó marcado como derogado; el
   campo `Status` no se modifica (ver «Estados documentales» de la conciliación).
4. T042 («registrar en `specs/trello.md` y en las tarjetas US la ampliación de alcance del 2026-10-13»)
   describe materialmente esta operación, pero se deja **sin marcar** en `tasks.md`: el encargo prohíbe
   marcar tareas sin evidencia suficiente y la comprobación se delega al usuario.

Las secciones anteriores a esta entrada se conservan íntegras como antecedentes históricos.

## Estado operativo vigente — SPEC-009 — 2026-10-12

| Elemento | Resultado verificado |
| --- | --- |
| Spec | [009-player-detail/spec.md](009-player-detail/spec.md), Status: Ready for planning; cinco decisiones confirmadas consolidadas mediante Clarify. |
| Rama local activa | `feat/009-player-detail`, creada desde el mismo HEAD de `dev` mediante operación Git autorizada; sin commits ni push. |
| Épica | [[EPIC] SPEC-009 - Player Detail](https://trello.com/c/KViJvig1) |
| ID de tarjeta | `6abc70f3c1ec56812e028155` |
| Tablero | Football Market, `6abc6f718adba84bb0f09184` |
| Lista | IN PROGRESS, `6abc70c14ade21afbe942c7b` (SPECIFYING → READY al superar el gate → IN PROGRESS al comenzar IMPLEMENT) |
| Fase / preparación | Analyze completada y aprobada por el responsable (0 CRITICAL / 0 HIGH / 0 MEDIUM / 6 LOW no bloqueantes; 22/22 FR y 37/37 tareas); Checklists 36/36; Tasks 37 corregidas; READY: YES con sincronización de tarjetas US completa. Implement y Converge pendientes. |

### Tarjetas de User Stories — 2026-10-12

Sincronización obligatoria del cierre de READY conforme a Constitution v7.1.0 §8. Las cuatro US
formales de `spec.md` tienen exactamente una tarjeta cada una, creada al cerrar READY tras el gate
humano de Analyze. Antes de escribir se consultaron las siete columnas del tablero: no existían
tarjetas `SPEC-009 | USxx`.

| Clave canónica | Trello ID | Enlace | Columna | Prioridad | Tareas de `tasks.md` |
| --- | --- | --- | --- | --- | --- |
| `SPEC-009 \| US01 - Consultar la identidad real del jugador` | `6acd6183a69c1dc6dd4f190b` | https://trello.com/c/lkiI1Wxd | READY | P1 | T007–T021 |
| `SPEC-009 \| US02 - Consultar estadísticas de demostración coherentes` | `6acd6188bff304f344fb583d` | https://trello.com/c/IRXEIntV | READY | P2 | T022–T025 |
| `SPEC-009 \| US03 - Consultar cotización y evolución de demostración` | `6acd61895714dd80e0f4bc36` | https://trello.com/c/J01xCDGc | READY | P2 | T026–T029 |
| `SPEC-009 \| US04 - Utilizar un dashboard adaptable con navegación real` | `6acd618a93021c82ab068eac` | https://trello.com/c/3A5njlA4 | READY | P2 | T030–T034 |

Relaciones: `EPIC [EPIC] SPEC-009 - Player Detail` (KViJvig1) → SPEC-009 (`specs/009-player-detail/`)
→ US01–US04 → tareas T001–T037 en `tasks.md`. Las tareas Txxx no se convirtieron en tarjetas. Las
cuatro describen estado vigente, progreso, bloqueos, próxima acción y referencias a `spec.md`,
`tasks.md`, `tp/enunciado.md` §4 y §11 y `tp/entregas.md`; conservan las etiquetas Frontend,
Prioridad Media y Sprint 2 de la épica.

Verificación posterior: cuatro tarjetas US únicas, correspondencia 1:1 con `spec.md`, todas en READY,
asociación correcta a la épica, índice sincronizado y ninguna tarjeta de otra SPEC movida o modificada.
No se movió ninguna US a IN PROGRESS. Evidencia: comentario `[SDD GATE EVIDENCE — READY SYNC]` en la
épica, ID `6acd618e23a6096111965c7e`, y en cada US. La sincronización de READY queda **completa**.

### IMPLEMENT AUTHORIZATION — 2026-10-12

Decisión humana explícita del responsable de SPEC-009, persistida como evidencia `[SDD GATE]`
(`Result: APPROVED`, `Validated by: HUMAN`) conforme a Constitution §8 y workflow §3. Alcance
autorizado: Setup T001–T003, Foundational T004–T006, US01 T007–T021, US02 T022–T025, US03
T026–T029, US04 T030–T034 y Polish T035–T037, con el orden técnico de `tasks.md`.

Evidencia: épica `6acd6322480dbfac1219bbfb`; US01 `6acd6323d99151e7e898edf4`; US02
`6acd6323003bd4c385e124d1`; US03 `6acd63234a2628b969f1a1ea`; US04 `6acd6324d1e4fb5c23ef1569`.
Verificada por lectura posterior en US01. La autorización no aprueba CODE REVIEW, TESTING ni el
cierre de Feature, y no habilita Converge; cada US permanece en READY hasta el inicio efectivo.

**Pendiente que bloquea el inicio de Implement:** los 16 marcadores del revisor en
`specs/009-player-detail/checklists/requirements.md` siguen sin marcar. Ese archivo es propiedad
del revisor y `/speckit.implement` lo trata como gate de solo lectura. `requirements-quality.md`
está 36/36. Ninguna tarjeta se movió a IN PROGRESS.

### Checklists del revisor e Implement — 2026-10-12

El revisor aprobó las decisiones O1-a y O2 sobre `checklists/requirements.md`, reformuló los
criterios 1, 14 y 15 y autorizó actualizar los 16 marcadores conforme a la evidencia presentada.
Resultado verificado: `requirements.md` 16/16 y `requirements-quality.md` 36/36. El criterio sobre
resultados medibles evaluates la definición y trazabilidad de los Success Criteria; el cumplimiento
de esos resultados por el producto queda pendiente de CODE REVIEW, TESTING y CONVERGE.

Con los gates previos satisfechos, la IMPLEMENT se ejecutó bajo la
autorización persistida: T001–T036 ejecutadas y marcadas, T037 pendiente porque la ejecución de la
suite backend y de los escenarios manuales corresponde al usuario. US01–US04 pasaron a CODE REVIEW y
la épica a IN PROGRESS. Evidencia: `6acd65e6…` en la épica y en cada US. Ningún gate aprobado.

La épica preexistente REQ-S2-12 fue asociada a SPEC-009 conservando ID, URL, miembro e historial,
sin crear una tarjeta duplicada. Se conservaron las etiquetas Frontend, Prioridad Media y Sprint 2;
se retiró únicamente «Pendiente de spec», ya obsoleta. Descripción y lista verificadas mediante
lectura posterior. El antecedente REQ-S2-12 y las antiguas referencias 016/008 se preservan como
historia, no como dependencias vigentes. No se crearon tarjetas de US ni de implementación.

La creación de rama preservó exactamente HEAD, índice, diffs y contenido de todos los archivos
rastreados y no rastreados. Los cambios ajenos siguen pendientes y no pertenecen automáticamente
a SPEC-009. El `BRANCH_STATUS=skipped-dirty-tree` de Specify permanece como evidencia histórica;
no se reejecutó Spec Kit para crear la rama ni se modificó `spec.md`.

**Trello sync:** épica, SDD Lifecycle e índice sincronizados; sincronización global parcial. Pendientes:
tarjetas de US01–US04, fuera del alcance autorizado; Analyze y los gates humanos requieren sus
autorizaciones. No se ejecutaron Analyze ni Implement. Las secciones anteriores a esta operación
se conservan debajo como antecedentes.

**Trello sync:** épica en columna READY, `SDD Lifecycle` y este índice sincronizados; sincronización
global parcial. Pendientes: tarjetas de US01–US04, fuera del alcance autorizado; autorización de
IMPLEMENT por historia y gates posteriores requieren decisiones humanas. No se ejecutó Implement ni
Converge. Las secciones anteriores a esta operación se conservan debajo como antecedentes.

### Gate Analyze y alcance de READY — 2026-10-12

Analyze reejecutado en modo read-only tras las correcciones documentales: **0 CRITICAL, 0 HIGH,
0 MEDIUM y 6 LOW aceptados como no bloqueantes** (L1–L4 del primer informe y N1–N2 editoriales).
Cobertura 22/22 requisitos FR-001–FR-022 y 37/37 tareas T001–T037; sin conflictos `[P]` sobre un
mismo archivo ni dependencias circulares; H1–H4 y M1–M6 verificados como resueltos.

Aprobación humana explícita del responsable de SPEC-009 sobre la consistencia documental y la
preparación para implementar. **No** autoriza Implement, ni el inicio de ninguna US, ni Converge.

Condiciones de READY verificadas: `spec.md`, `plan.md`, `tasks.md` y checklists evaluados y
coherentes; ambigüedades funcionales resueltas en Clarify; criterios de aceptación cubiertos por
tareas (FR-001–FR-022 con SC-001–SC-010); Analyze vigente y sin blockers; sin requisitos nuevos
introducidos sin decisión humana; ningún blocker en US1–US4. **READY: YES**, registrado moviendo la
épica de SPECIFYING a la columna READY. READY no autoriza implementar: la US permanece `READY`
hasta el inicio efectivo de IMPLEMENT, y las tarjetas de US se crean bajo autorización separada.

Evidencia: comentario `[SDD GATE EVIDENCE — ANALYZE]`, ID `6acd5facaeb83226926225f3`, y elemento
`Analyze` del `SDD Lifecycle` marcado, verificados por lectura posterior (6 de 8 elementos
completos). Siguiente gate: IMPLEMENT AUTHORIZATION por historia.

### Corrección documental posterior a Analyze — 2026-10-12

Operación separada y autorizada, posterior al Analyze read-only, que no declara el gate de
Analyze resuelto. Se corrigieron en `specs/009-player-detail/tasks.md` los 4 hallazgos HIGH y los
6 MEDIUM del Specification Analysis Report, sin cambiar alcance funcional, diseño aprobado,
prioridades ni historias.

HIGH: T007 deja de ser paralela y T008 queda secuencial tras T007 por compartir
`PlayerControllerTest.java` (assertions y snippets REST Docs con responsabilidades separadas); el
barrel `features/players/demo/index.ts` lo crea solo T006; T003 declara el tipo `PlayerDetail` de
`data-model.md` y T015 deja de confundirlo con `Player`; T011 exige la consulta JPQL con el
predicado exacto del catálogo (`p.active = true`, `join p.team t`, `t.current = true`) y
`@EntityGraph({"team", "team.league"})` en lugar de un método derivado incompatible.

MEDIUM: T020 y T021 descartan respuestas obsoletas ante cambio rápido de id; T035 documenta el
cumplimiento parcial de REQ-S2-12; T022 y T026 usan la función centralizada de `features/players/demo`;
`PlayerCard.tsx` queda secuenciado T006 → T021 → T025 sin ediciones concurrentes; T034 pasa a seis
condiciones observables de accesibilidad; T010 exige extender `DomainException`.

Validación: 37 tareas y IDs T001–T037 conservados, sin tareas agregadas ni eliminadas; sin
conflictos `[P]` sobre un mismo archivo; cobertura FR-001–FR-022 intacta; sin código, tests,
commits, push ni cambio de rama; `git diff --check` limpio. Blockers: ninguno.

Evidencia persistida en comentario `[SDD ANALYZE FOLLOW-UP]`, ID `6acd5efe8d0daf6222d8d01e`.
Elemento `Analyze` del `SDD Lifecycle` permanece **sin marcar**: debe repetirse `/speckit.analyze`
antes de READY. READY: NO.

### Fase Tasks — 2026-10-12

Tasks **completada**. Generado `specs/009-player-detail/tasks.md` con **37 tareas** y rutas
exactas del repositorio existente: Setup 3, Foundational 3, US1 15, US2 4, US3 4, US4 5 y
Polish 3. MVP = US1.

Orden: Setup → Foundational (frontera única de datos de demostración, bloquea US2 y US3) →
US1 (endpoint y pantalla de identidad real) → US2 y US3 → US4 → Polish. US1, US2, US3 y US4
conservan criterio de prueba independiente; la navbar se aplica solo a `/players/:id`.

Cobertura normativa: las cinco decisiones de Clarify preservadas; tests backend de Controller con
Spring REST Docs y de Service generados como tareas, con ejecución a cargo del usuario; frontend
exento de tests; identidad real en backend separada de los datos simulados en frontend; sin
scraping, sin `GET /api/players/{id}/quotes` y sin tocar SPEC-008.

Sin `.specify/extensions.yml`: no hay hooks `before_tasks` ni `after_tasks`. Sin código, commits,
push ni cambio de rama. Blockers: ninguno. Evidencia persistida en comentario
`[SDD PHASE EVIDENCE]`, ID `6acd5dde2d4db9da6509a051`, y verificada por lectura posterior junto al
elemento `Tasks` marcado en `SDD Lifecycle`.

Siguiente acción permitida: `/speckit.analyze` (read-only). READY sigue en NO; Implement requiere
autorización humana explícita.

### Cierre de fase Checklists — 2026-10-12

Fase Checklists **completada** con aprobación humana explícita del responsable de la revisión de
SPEC-009 sobre `specs/009-player-detail/checklists/requirements-quality.md`: **36/36 criterios
aprobados (CHK001–CHK036)**. Los 36 marcadores pasaron de `[ ]` a `[x]` por decisión del
revisor; contenido, redacción y numeración conservados; no se generaron checklists adicionales.
`checklists/requirements.md` permanece intacto.

El alcance de la aprobación es exclusivamente la calidad documental de los requisitos. No
acredita implementación, pruebas, `ANALYZE` ni `READY: NO` se mantiene. No se aprobaron otros
gates.

Siguiente fase pendiente en el momento del cierre: **Tasks** (ejecutada después, registrada
abajo) y luego Analyze antes de READY. Al cerrar esta fase no se ejecutaron `/speckit.tasks`,
Analyze, Implement ni Converge. Sin commits ni push; sigue la rama `feat/009-player-detail`.

### Fase Plan — 2026-10-12

Plan OK. Artefactos generados en `specs/009-player-detail/`: `plan.md`, `research.md`,
`data-model.md`, `contracts/player-detail.md`, `contracts/demo-data.md` y `quickstart.md`.
Constitution Check PASS antes y después del diseño; sin dependencias nuevas, sin
`NEEDS CLARIFICATION` y sin violaciones que justificar.

Decisiones de diseño relevantes: reutilización del criterio de visibilidad del catálogo con 404
para jugadores no visibles; método de lectura en repositorio y excepción de aplicación propia
mapeada por el `GlobalExceptionHandler`; gráfico de evolución en SVG sin dependencia nueva;
navbar como componente compartido compuesto desde `app` para evitar dependencia entre features;
fuente de demostración determinista por `playerId` con temporada y período fijos.

Bloqueantes: ninguno. Pendientes del lifecycle: evaluación de Checklists por el revisor,
Tasks y Analyze antes de READY. Evidencia persistida en comentario `[SDD PHASE EVIDENCE]`, ID
`6acd57bd4a2628b969dfada5`, verificada por lectura posterior junto al elemento `Plan` marcado.
Spec en preparación; READY NO. Sin implementación ni gates humanos aprobados.

### Fase Checklists — 2026-10-12

Generado `specs/009-player-detail/checklists/requirements-quality.md` con 36 criterios de calidad de
requisitos (CHK001–CHK036), sin marcar: archivo nuevo, ningún marcador del revisor modificado.
Derivado de `spec.md`, `plan.md` y `contracts/`, con foco en completitud, claridad, consistencia,
medibilidad, cobertura de escenarios, requisitos no funcionales, dependencias y trazabilidad.

Profundidad estándar y audiencia: revisor previo a Tasks. Criterios de seguridad, API, UX y datos
quedan cubiertos dentro del checklist general (CHK020, CHK025, CHK028, CHK031); queda pendiente
que el revisor decida si requiere checklists especializados adicionales.
`checklists/requirements.md` (built-in de Specify/Clarify) permanece intacto.

Evidencia persistida en comentario `[SDD PHASE EVIDENCE]`, ID `6acd585eafc6239298f96068`, y
verificada por lectura posterior: el elemento `Checklists` del Lifecycle permanece sin marcar
porque generar no equivale a evaluar. Pendientes: evaluación del revisor, Tasks y Analyze antes
de READY. No se ejecutaron Tasks, Analyze ni Implement.

### Revisión documental de requisitos — 2026-10-12

Revisión conjunta solicitada por el revisor sobre `spec.md`, `plan.md`, `research.md`,
`data-model.md`, `contracts/player-detail.md`, `contracts/demo-data.md` y el checklist de calidad.
Solo documentación; sin código, sin tareas y sin commits.

Correcciones en el checklist: CHK005/CHK006 alineados a los conceptos definidos en la spec;
CHK011 expresado como una cotización diaria durante treinta días consecutivos; CHK027 coherente
con los requisitos de UX y el objetivo ya documentado, sin umbrales nuevos; CHK035/CHK036
reformulados para que `[x]` represente ausencia de contradicciones.

Correcciones en artefactos: `data-model.md` ya no exige combinaciones estadísticas únicas por
jugador y temporada; `TokenQuoteSnapshot` representa la serie vacía con `price`/`date` en `null`,
cambios en `null` y `direction` en `unknown`, sin inventar precio ni fecha; el período financiero
ya no se impone dentro de la temporada 2025/2026. `contracts/player-detail.md` y `data-model.md`
retiran el `502` para fallos técnicos locales de lectura: verificado en el código existente que
los `502` de `GlobalExceptionHandler` corresponden solo a proveedores externos, por lo que se
documentó la inconsistencia sin modificar el backend.

Cobertura verificada: los 22 requisitos FR-001 a FR-022 están referenciados por al menos un
criterio de éxito y ningún SC queda sin requisito. Sin contradicciones con las cinco decisiones de
Clarify.

Evidencia persistida en comentario `[SDD PHASE EVIDENCE]`, ID `6acd5c6fb4360f529d0a7e7d`.
Checklists continúan PENDING EVALUATION con sus 36 marcadores sin marcar; READY NO. Pendiente:
evaluación del revisor y decisión sobre checklists especializados.

### Consolidación Specify/Clarify — 2026-10-12

Cinco decisiones funcionales confirmadas integradas en `spec.md`, sección Clarifications,
historias/escenarios, FR, SC, excepciones, supuestos y límites: solo activos/inactivos 404;
temporada fija 2025/2026; historial fijo de treinta cotizaciones diarias; tarjeta
“Rendimiento deportivo” sin radar; navbar superior y breadcrumb, global reutilizable
pero sin adopción en otras páginas. REQ-S2-12 sigue parcialmente cubierto.

Procedimiento local Clarify seguido: resolución de rutas con `check-prerequisites.sh --json
--paths-only`, escaneo de cobertura y cinco respuestas preconfirmadas; cero preguntas nuevas
y cero marcadores pendientes. Contratos futuros, inicio concreto del período de demostración
y composición técnica de navbar quedan para Plan; no son bloqueos funcionales.
Checklist documental reevaluado sin aprobar marcadores del revisor.

Épica y checklist reutilizados, sin duplicados. Evidencia vigente persistida en comentario
`[SDD PHASE EVIDENCE]`, ID `6acd52a358f3fcc1287c2d5e`, y verificada mediante lectura posterior:
Specify y Clarify completos; Plan, Checklists, Tasks, Analyze, Implement y Converge incompletos.
La descripción de la épica refleja Ready for planning y fase Clarify; permanece en SPECIFYING.
No hay READY, implementación o gates humanos aprobados. Se conserva historial previo,
incluida la inicialización pendiente que sigue debajo como antecedente.

### Antecedente — Inicialización SDD Lifecycle — 2026-10-12

Creado en la épica existente, tras comprobar ausencia de checklist previo:
`SDD Lifecycle`, ID `6acd4d79f29669f6835ecdc2`. Lectura posterior confirma un único
checklist y ocho elementos únicos en el orden canónico:

| Elemento | Estado verificado | Evidencia |
| --- | --- | --- |
| Specify | Pendiente de cierre; no marcado | Ejecutado y redactado previamente; revisión inicial registra aclaración de elegibilidad abierta. |
| Clarify | Pendiente; no marcado | No ejecutado. |
| Plan | Pendiente; no marcado | No ejecutado. |
| Checklists | Pendiente; no marcado | La revisión inicial de Specify no acredita evaluación de esta fase por el revisor. |
| Tasks | Pendiente; no marcado | No ejecutado. |
| Analyze | Pendiente; no marcado | No ejecutado. |
| Implement | Pendiente; no marcado | No iniciado. |
| Converge | Pendiente; no marcado | No ejecutado. |

Evidencia persistida y verificada en comentario `[SDD LIFECYCLE EVIDENCE]`, ID
`6acd4d8a1785f2c95e8c2450`, con fecha, responsable, alcance, fuentes, resultado y próxima acción.
Constitution v7.0.0 es norma vigente, no elemento del lifecycle de Feature. SPEC-009 permanece
Draft, SPECIFYING, READY: NO; sin gates humanos inferidos. No se ejecutaron fases nuevas ni
se crearon tarjetas US. El seguimiento Lifecycle deja de ser una operación pendiente.

## Asignación de identidad para trabajo paralelo — 2026-10-12

Registro previo a Specify conforme a Constitution v7.0.0 §8, autorizado por el usuario:

| Número | Funcionalidad | Responsable | Motivo |
| --- | --- | --- | --- |
| 008 | Scraping de jugadores | Otro integrante del equipo, según confirmación del usuario | Desarrollo paralelo; spec real todavía no integrada. |
| 009 | Detalle individual del jugador (REQ-S2-12) | Integrante encargado del frontend y detalle del jugador (usuario solicitante) | Evitar colisión con la asignación de scraping 008. |

Comprobación previa: carpetas locales `specs/` (001–008), ramas locales y remotas conocidas,
`git ls-remote --heads origin` (solo dev y main), y árboles de `origin/dev` y `origin/main`
inspeccionados mediante `git ls-tree`; no hay SPEC-009 ni rama 009 en esas fuentes.
Las asignaciones conocidas por el equipo son las confirmadas por el usuario arriba.
`specs/008-player-scrapping/spec.md` está vacío: carpeta provisional, no SPEC validada,
implementada ni integrada. No se modifica ni se reutiliza.
La asignación no acredita existencia de SPEC, fases, gates, aprobación o implementación.
Antecedente operacional: [REQ-S2-12](https://trello.com/c/KViJvig1).
La asociación a SPEC-009 y creación de tarjetas de sus US queda pendiente de autorización
operativa para Trello; no se escribirá en el tablero en esta operación.

### Resultado de Specify de SPEC-009 — 2026-10-12

Identidad materializada: [009-player-detail](009-player-detail/spec.md), `SPEC_ACTION=created`,
`FEATURE_NUM=009`, rama prevista `feat/009-player-detail`, `BRANCH_STATUS=skipped-dirty-tree`;
la rama activa permanece `dev`. Se preservan cambios locales. Especificación `Draft` con una
decisión abierta (elegibilidad de jugadores inactivos); revisión inicial en
[checklist](009-player-detail/checklists/requirements.md). No acredita READY ni aprobación humana.
US formales definidas: US01 identidad real, US02 estadísticas de demostración, US03 cotización
y evolución de demostración, US04 dashboard adaptable. Pendiente: asociar la épica REQ-S2-12
preservando su ID/URL y crear una tarjeta por cada US, mediante autorización operativa.
Trello sync: PENDING AUTHORIZATION; no hubo escrituras ni comprobación del tablero en vivo.
La ausencia del historial backend implica cumplimiento parcial de REQ-S2-12.

## Estado vigente — SPEC-007 DONE, US06 retirada del alcance — 2026-10-11

Cierre por autorización humana explícita, persistida en Trello como `[SDD GATE] FEATURE COMPLETION /
APPROVED` en la épica y en cada US.

| Gate | Estado | Evidencia |
| --- | --- | --- |
| CODE REVIEW | **APPROVED** | Aprobación humana explícita del 2026-10-11, con hallazgos HIGH/MEDIUM previamente corregidos. |
| TESTING | **APPROVED** | `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**, suite completa verde. Ejecución manual del usuario; el agente no ejecuta tests. |
| FEATURE COMPLETION AUTHORIZATION | **APPROVED** | Otorgada explícitamente el 2026-10-11 y persistida. |

- `[EPIC] SPEC-007 - Dominio de equipos y ligas` → **DONE**.
- **US01–US05 → DONE** (5/5). Cada tarjeta registra su gate de cierre y entrega en la descripción vigente.
- **US06 → RETIRADA del alcance**, no entregada ni DONE. Conserva identificador y tarjeta como
  trazabilidad histórica conforme a Constitution §8; permanece en `BACKLOG` porque `DONE` significaría
  entrega. Su tarjeta documenta el motivo y la sustitución funcional.
- **13/13 tareas de implementación completadas** (T053–T065), conforme a `tasks.md`.
- Esquema definitivo en **V5** (`Player → Team → League`, `players.team_id` NOT NULL). **V5.1 y V6
  eliminados físicamente**; no son trabajo pendiente.
- T028, P12 y CR-02 pertenecen al alcance de transición legacy **retirado** por decisión humana del
  2026-10-10 y no gobiernan el cierre. Constitution **6.1.0** §2.1 vigente (ratificada el 2026-10-11).
- **Acción operativa posterior, no bloqueante**: recrear PostgreSQL DEV según `quickstart.md` §3 para
  arrancar el backend local. Requiere autorización específica según §6 y §8.

Esta sección sustituye como vigente la reconciliación del 2026-10-07 y los estados posteriores, que se
conservan íntegros como antecedentes históricos. No se movieron tarjetas, no se alteraron gates,
comentarios ni decisiones de alcance: la actualización refleja el tablero ya cerrado.

## Antecedente — Reconciliación del protocolo — 2026-10-07

Tablero inspeccionado: 63 tarjetas activas, 7 épicas SPEC, 25 US formales,
23 épicas REQ y 8 controles/observaciones técnicas. Sin tarjetas nuevas, renombres ni movimientos.
Se actualizaron descripciones de las siete épicas y 25 US: cierre vigente, relación padre,
Tasks en `tasks.md` y ausencia de autorización de implementación. Historial y metadatos preservados.

| SPEC / carpeta | Épica | US | Kanban preservado | Fases acreditadas |
| --- | --- | --- | --- | --- |
| 001-user-auth-registration | [SPEC-001](https://trello.com/c/Qge1nG5l) | US01–02 | Done histórico | Ninguna |
| 002-player-catalog | [SPEC-002](https://trello.com/c/3UuHczuT) | US01–02 | Done histórico | Ninguna |
| 003-current-user | [SPEC-003](https://trello.com/c/VlRBcTsW) | US01–02 | Done histórico | Ninguna |
| 004-frontend-user-auth | [SPEC-004](https://trello.com/c/a9hopQWh) | US01–05 | Done histórico | Ninguna |
| 005-players-page | [SPEC-005](https://trello.com/c/pP8Qfa6M) | US01–04 | Done histórico | Ninguna |
| 006-player-images | [SPEC-006](https://trello.com/c/ieUyRF1V) | US01–04 | Backlog | Ninguna |
| 007-team-league-domain | [SPEC-007](https://trello.com/c/dyBW2MBE) | US01–06 | En progreso heredado | Solo Checklists |

Cada épica tiene un único checklist nuevo `SDD Lifecycle`, con las ocho fases en orden.
Solo Checklists de SPEC-007 fue marcado: `007-team-league-domain/checklists/requirements-quality.md`
registra evaluación individual solicitada por el usuario el 2026-10-06, 30 criterios satisfechos,
0 abiertos y alternativa C aprobada. Evidencia copiada/referenciada en comentario de la épica;
es evidencia local sin commit, no ejecución nueva ni prueba de Analyze OK.
Las demás fases quedan sin marcar por falta de cierre demostrable, no como afirmación de que nunca ocurrieron.

Las 23 épicas REQ ya tenían formato vigente; permanecen Backlog, no ejecutables, sin US/Tasks
formales ni números SPEC reservados. Las siete tarjetas `SPEC-nnn | TSK01` son observaciones
técnicas históricas, no US ni Tasks `Txxx` generadas ahora; se preservan sin archivar.

Divergencias pendientes: Done histórico no acredita lifecycle actual; SPEC-003–005 conservan
`Status: Draft`. SPEC-006 tiene commits de implementación (`59e482d`, merge `2c4cbc5`) pero
sigue Backlog: no basta para determinar Review/Done. SPEC-007 conserva En progreso con preparación
local y Tasks pendientes, sin evidencia de Analyze OK/toma vigente suficiente para reconstruir su estado.
Las 25 US corresponden a `spec.md`; diferencias ortográficas no cambian identidad funcional.

Decisión humana pendiente: aportar evidencia histórica de gates/aceptaciones o autorizar por separado
la revisión/fase correspondiente antes de regularizar estados. No se ejecutó SDD, tests ni builds,
no se cambió código ni alcance de specs. Esta migración no habilita implementación.
Los registros anteriores se conservan como antecedentes; esta sección distingue estado histórico
de lifecycle verificable conforme a Constitution 5.0.0 y al workflow SDD vigente.

## Tablero y fuentes

#### Estado vigente — US01–US06 en TESTING

CODE REVIEW de US01–US06 aprobado por humano tras revisar el checklist completo, con evidencia
`[SDD GATE] CODE REVIEW / APPROVED` persistida y verificada en cada US. US01–US06 movidas de
CODE REVIEW a TESTING con lectura posterior verificada. TESTING = `PENDING HUMAN VALIDATION`: los
tests backend y los builds permitidos los ejecuta el usuario; el agente no ejecutó ninguno y el
build histórico no valida el diff actual. Ninguna US es DONE todavía, por lo que la Feature
permanece IN PROGRESS. Release 2 (V6: T039/T040/T052) sigue sin autorizar ni iniciar, y el gate
T028 (despliegue con solo V5, sincronización y verificación de IDs/conteos/asociaciones/evidencia/
marcador) continúa pendiente del usuario. Un cambio de código o tests que afecte el alcance aprobado
invalida CODE REVIEW y TESTING según impacto.

### Antecedente — cierre técnico de Release 1 completo; US01–US06 en CODE REVIEW

Todas las tasks técnicas autorizadas (T001–T038, T041–T051) están implementadas como fuentes y
marcadas `[x]`; T039/T040/T052 siguen pendientes por pertenecer a V6. Code Review Report entregado
en sesión para US01–US06, cubriendo implementación y tests. US01–US06 movidas de IN PROGRESS a
CODE REVIEW con lectura posterior verificada; Feature permanece IN PROGRESS. Sin tests, builds ni
migraciones ejecutados y sin iniciar V6. Evidencia de avance persistida como comentario en la
tarjeta Feature y en cada US. TESTING y DONE requieren aprobación humana explícita; el gate de
Release 1 (despliegue con solo V5, sincronización y verificación) sigue pendiente del usuario.

### Antecedente — B-V5-05 resuelto; IMPLEMENT Release 1 continúa

Decisión humana: dateOfBirth y nationality se consolidan por atributo e independientemente del
estado obligatorio; sin valor válido no hay valor nuevo, con valor único se usa, y con valores
válidos incompatibles el Player existente conserva su persistido y el nuevo persiste null. El
conflicto no genera INVALID_SUBJECT_DATA, no congela ni descarta al Player, no impide consolidar
el otro atributo y no bloquea el enriquecimiento; sin selección por orden/externalId. Se registra
como PLAYER_OPTIONAL_CONFLICT con causas CONFLICTING_DATE_OF_BIRTH/CONFLICTING_NATIONALITY.
ANALYZE posterior read-only PASS WITH NON-BLOCKING; READY recuperado; Status Ready for
implementation; Feature y US01–US06 IN PROGRESS. Autorizaciones preservadas sin ampliación
material. B-V5-01–05 resueltos; el estado pendiente de B-V5-05 más abajo es antecedente.
Sin CODE REVIEW/TESTING/DONE aprobados, sin V6, sin tests/builds/migraciones ejecutados.
Sincronización de este evento y avance pendiente de lectura/escritura/verificación en Feature y
seis US.

### Antecedente — B-V5-05 pendiente

STOP IMPLEMENT por decisión de opcionales válidos incompatibles entre referencias distintas
del mismo Player, con Team/name/position coherentes. SPEC-002 RF-018 impide descartar solo por
opcionales; B-V5-03/B-V5-04/ranking no definen su consolidación. No inventar prioridad por
orden, preservación por campo ni extender protección integral. Evidencia y avance en
specs/007-team-league-domain/tasks.md; Status Draft, READY NO, Feature/US01–US06 IN PROGRESS.
Autorizaciones preservadas; sin CODE REVIEW/TESTING/DONE aprobados, ni tests/build/migraciones
ejecutados, ni V6. B-V5-01–04 resueltos; estados READY recuperado siguientes son históricos.
Sincronización de este evento completada: lectura previa, comentario nuevo y lectura posterior
verificaron persistencia en Feature y seis US con listas IN PROGRESS intactas; Trello SYNCED.
No se alteraron miembros, historial ni gates.

### Estado vigente — B-V5-04 y selección canónica resueltos

Decisión humana incorporada: comparación por normalización existente separada de presentación;
ranking de originales equivalentes por diacríticos/casing natural/desempate lexicográfico;
existente conserva texto persistido equivalente y GET no normaliza. B-V5-01–04 resueltos.
ANALYZE posterior read-only PASS WITH NON-BLOCKING, READY recuperado; autorizaciones preservadas
sin ampliación material. Feature/US01–US06 IN PROGRESS; Status Ready for implementation.
Resolución y avance de implementación persistidos y verificados en Feature y seis US.
Detalle de código/cobertura fuente en tasks.md; sin ejecución tests/builds/migración, V6 o gates
humanos aprobados. Bloqueos de las secciones anteriores/siguientes son antecedentes superados.

### Estado vigente — B-V5-03 resuelto; B-V5-04 pendiente

Decisión humana B-V5-03 incorporada en SPEC-007 y diseño/contrato/quickstart/tasks: contradicciones
de Team/name/position por propietario en una foto → INVALID_SUBJECT_DATA y conservación integral,
independiente del orden; referencias coherentes son válidas, fotos posteriores pueden procesar.
Normalización estricta existente para comparación, sin normas nuevas. Cobertura de nueve
escenarios y equivalencias planificada en tasks existentes, no tests nuevos ejecutados/escritos.
ANALYZE posterior read-only BLOCKED por B-V5-04: no se define forma textual persistida/GET para
name/position equivalentes con representaciones originales distintas. No elegir por orden ni
guardar automáticamente clave de comparación. READY NO, SDD CLARIFY, Feature/US IN PROGRESS.
B-V5-01/B-V5-02/B-V5-03 resueltos; autorizaciones preservadas, evaluar solo impacto de la decisión
futura. Sin producto nuevo, tests/builds/migraciones ni V6/gates aprobados en esta continuación.

### Estado vigente — B-V5-03 pendiente

Tras aceptación humana de B-V5-02 se conservaron las autorizaciones de implementación V5 y se
retomó revisión de cierre. Nuevo hallazgo funcional: referencias distintas del mismo Player con
Team coherente pero name/position obligatorios contradictorios no tienen política de consolidación
entre propietarios definida; el Service actual sobrescribe por orden. B-V5-03 HIGH, STOP IMPLEMENT
por decisión funcional nueva, READY global NO. Status de spec vuelve a Draft por esta aclaración
pendiente, según specs/README.md; el estado anterior Ready for implementation fue válido antes
del hallazgo. B-V5-01/B-V5-02 siguen resueltos. Feature/US IN PROGRESS; autorizaciones preservadas
hasta evaluar impacto de la decisión futura. Sin tests/builds/migraciones, sin V6 ni gates nuevos.

### Estado vigente — resolución B-V5-02 (2026-10-07)

Decisión humana incorporada a SPEC-007: múltiples referencias válidas del mismo Player con Teams
válidos contradictorios en una foto → INVALID_SUBJECT_DATA, protección del estado/asociación/espejo
y conservación de todas las referencias; independiente del orden y limitado a esa foto. Otros
Players continúan y una foto posterior coherente procesa normalmente. B-V5-01 preservado.
ANALYZE posterior read-only: PASS WITH NON-BLOCKING (Status editorial LOW), READY recuperado,
autorizaciones US01–US06 preservadas por ausencia de ampliación material. Feature/US IN PROGRESS.
Resolución registrada y leída posteriormente en Feature y las seis US; bloqueos anteriores son
antecedentes, no estado vigente. Sin gates de review/testing/cierre aprobados ni V6.
Retrabajo puntual y cobertura fuente en tasks.md; no tests/builds/migraciones ejecutados.

### Resolución funcional posterior — B-V5-01 / B-V5-02

Decisión humana: conservar todas las referencias legacy válidas de Player. B-V5-01 resuelto;
artefactos corregidos, UNIQUE(player_id,provider) y rechazo productivo por proveedor retirados,
regresiones fuente adaptadas sin ejecución. UNIQUE(provider,external_id) se preserva para todos;
unicidad propietario/proveedor solo para Team/League. Sin ampliación material de US por B-V5-01.
El PASS WITH NON-BLOCKING inicial no habilita continuación tras descubrir B-V5-02: falta política
para referencias distintas del mismo Player que llegan con Team/datos contradictorios en la misma
foto. ANALYZE vigente BLOCKED; READY global pendiente. El blocker anterior queda como historia,
no como decisión abierta. Feature/US IN PROGRESS, autorizaciones preservadas, sin gates aprobados.

### Actualización operacional SPEC-007 — continuación IMPLEMENT, 2026-10-07

El registro histórico anterior se conserva. El ANALYZE posterior y las autorizaciones V5 fueron
persistidos en Trello; no sustituyen gates humanos. Durante esta continuación US02–US06 comenzaron
comportamiento propio y pasaron READY → IN PROGRESS; US01 y la Feature permanecen IN PROGRESS.
IMPLEMENT detenido por B-V5-01, descrito en [tasks.md](007-team-league-domain/tasks.md): política de
conservación de múltiples referencias legacy del mismo proveedor frente a la nueva unicidad por
propietario. No se conoce la existencia de esos datos en una DB real: hallazgo estático del contrato
de transición. Preparación global pendiente de resolver y reevaluar; autorizaciones históricas
preservadas, sin aprobar CODE REVIEW/TESTING, completar Feature ni iniciar V6.

- [Football Market](https://trello.com/b/LdQo0xDW/football-market), ID `6abc6f718adba84bb0f09184`.
- Reglas de sincronización: [Constitución §8](../.specify/memory/constitution.md#8-trazabilidad-y-sincronización-con-trello).
- Fuentes académicas complementarias: [enunciado](../tp/enunciado.md) y [entregas](../tp/entregas.md).
- [Control de cobertura y presentación de Sprint 2](https://trello.com/c/7Ziy5ws3).

Este índice registra relaciones, no acredita implementación, aprobación ni pruebas. Las fuentes son los archivos locales vigentes; los enlaces a GitHub presentes en tarjetas no garantizan que cambios locales sin publicar estén disponibles en remoto.

## Convención

- Historia de una spec existente: `SPEC-007 | US01 - Título de la historia`. El número de US es local a cada spec.
- Tarea técnica de seguimiento: `SPEC-007 | TSK01 - Título de la tarea`. Si corresponde a una tarea de `tasks.md`, se referencia además su identificador `Tnnn`; no son numeraciones equivalentes.
- Requisito sin spec: `[EPIC] REQ-<id> - <nombre>`, conforme a Constitution §8; no reserva número de SPEC ni define US/Tasks formales.
- Al asociar un requisito provisional a una spec real, actualizar esta tabla y la tarjeta conservando ID, URL y antecedente; no crear una copia de la misma historia.

## Historias de las specs existentes

Las 25 historias se corresponden con los títulos, prioridades y escenarios de las specs locales. Las tarjetas contienen el propósito, un resumen de criterios y la referencia a los escenarios completos; la spec conserva autoridad sobre su alcance. No se modificaron las specs para acomodarlas al tablero.

| Spec | Historia | Tarjeta | Estado de seguimiento |
| --- | --- | --- | --- |
| [001](001-user-auth-registration/spec.md) | US01 — Creación de cuenta de usuario | [Trello](https://trello.com/c/rzaE8d4e) | Done heredado |
| [001](001-user-auth-registration/spec.md) | US02 — Inicio de sesión de usuario | [Trello](https://trello.com/c/61gzxJeb) | Done heredado |
| [002](002-player-catalog/spec.md) | US01 — Consultar el catálogo de jugadores | [Trello](https://trello.com/c/XhiITvZI) | Done heredado |
| [002](002-player-catalog/spec.md) | US02 — Sincronizar manualmente el catálogo | [Trello](https://trello.com/c/lo9qZscQ) | Done heredado |
| [003](003-current-user/spec.md) | US01 — Recuperar la identidad de la sesión actual | [Trello](https://trello.com/c/o9XpSE87) | Done heredado |
| [003](003-current-user/spec.md) | US02 — Rechazar solicitudes no autenticadas | [Trello](https://trello.com/c/7orcvX2D) | Done heredado |
| [004](004-frontend-user-auth/spec.md) | US01 — Crear una cuenta | [Trello](https://trello.com/c/YanLjlBb) | Done heredado |
| [004](004-frontend-user-auth/spec.md) | US02 — Iniciar sesión y acceder al home | [Trello](https://trello.com/c/XxG0CwRj) | Done heredado |
| [004](004-frontend-user-auth/spec.md) | US03 — Conservar y proteger la sesión | [Trello](https://trello.com/c/13cjAWOb) | Done heredado |
| [004](004-frontend-user-auth/spec.md) | US04 — Cerrar sesión | [Trello](https://trello.com/c/uE1UJGKu) | Done heredado |
| [004](004-frontend-user-auth/spec.md) | US05 — Utilizar páginas de acceso coherentes y adaptables | [Trello](https://trello.com/c/S3Eqfn14) | Done heredado |
| [005](005-players-page/spec.md) | US01 — Consultar jugadores como usuario autenticado | [Trello](https://trello.com/c/7WWHGU58) | Done heredado |
| [005](005-players-page/spec.md) | US02 — Navegar entre páginas del catálogo | [Trello](https://trello.com/c/Z2qrRpuC) | Done heredado |
| [005](005-players-page/spec.md) | US03 — Reconocer estado y clasificación visual | [Trello](https://trello.com/c/XOeC5jC2) | Done heredado |
| [005](005-players-page/spec.md) | US04 — Visualizar controles de filtrado | [Trello](https://trello.com/c/UatbQHJp) | Done heredado |
| [006](006-player-images/spec.md) | US01 — Reconocer al jugador por su imagen | [Trello](https://trello.com/c/HJJzqJNN) | Backlog |
| [006](006-player-images/spec.md) | US02 — Resolver identidad y enriquecer imágenes manualmente | [Trello](https://trello.com/c/J2g08Tw5) | Backlog |
| [006](006-player-images/spec.md) | US03 — Reintentar sin perder resultados | [Trello](https://trello.com/c/yjs63hrK) | Backlog |
| [006](006-player-images/spec.md) | US04 — Consultar la auditoría de imágenes | [Trello](https://trello.com/c/wuVsZbX8) | Backlog |
| [007](007-team-league-domain/spec.md) | US01 — Reconocer al equipo y a la liga del jugador | [Trello](https://trello.com/c/FWEz7xjP) | DONE |
| [007](007-team-league-domain/spec.md) | US02 — Sincronizar el catálogo reconociendo ligas y equipos | [Trello](https://trello.com/c/Q2Yg1P1l) | DONE |
| [007](007-team-league-domain/spec.md) | US03 — Conservar la identidad del equipo ante cambios del proveedor | [Trello](https://trello.com/c/k7iw3LKH) | DONE |
| [007](007-team-league-domain/spec.md) | US04 — Resolver la identidad externa del equipo sin bloquear el catálogo | [Trello](https://trello.com/c/rHQkeh3k) | DONE |
| [007](007-team-league-domain/spec.md) | US05 — Mantener en el catálogo a los equipos y jugadores que lo abandonan | [Trello](https://trello.com/c/6n04meCE) | DONE |
| [007](007-team-league-domain/spec.md) | US06 — Migrar el catálogo existente sin perder jugadores | [Trello](https://trello.com/c/94ktF8ch) | Retirada del alcance; no DONE. Antecedente histórico. |

**Done heredado:** el usuario autorizó reorganizar las tarjetas conservando sus estados. Las cinco tarjetas agrupadas de 001–005 estaban en Done; se repartieron en sus 15 historias conservando ese seguimiento y sin una validación nueva. No se cambió el `Status` de las specs ni se cerraron hallazgos técnicos. Las US de 006 quedan en Backlog y sin asignación de sprint confirmada. Las US de 007 fueron concluidas por gates humanos el 2026-10-11 (ver el estado vigente al inicio de este índice), con US06 retirada del alcance y conservada como antecedente.

## Hallazgos técnicos preservados

Todas estas tareas permanecen en Backlog con sus etiquetas y antecedentes. No se reinspeccionó código ni se ejecutaron pruebas para resolverlas.

| Clave | Propósito | Tarjeta |
| --- | --- | --- |
| SPEC-001 \| TSK01 | Alinear contrato de registro y autenticación | [Trello](https://trello.com/c/m0rQ8Bvf) |
| SPEC-002 \| TSK01 | Revisar desalineaciones del contrato del catálogo | [Trello](https://trello.com/c/QaZz43pC) |
| SPEC-003 \| TSK01 | Revisar estado y contrato del usuario actual | [Trello](https://trello.com/c/nVN93AW6) |
| SPEC-004 \| TSK01 | Revisar base HTTP, errores y reintento de sesión | [Trello](https://trello.com/c/UJnej3Iw) |
| SPEC-005 \| TSK01 | Revisar DTO, contrato e imágenes de tarjetas | [Trello](https://trello.com/c/RYreZ5lK) |
| SPEC-006 \| TSK01 | Revisar divergencias entre aclaraciones y artefactos de imágenes | [Trello](https://trello.com/c/mNG4uTpQ) |
| SPEC-007 \| TSK01 | Revalidar observaciones contra aclaraciones actuales | [Trello](https://trello.com/c/qoAKAABl) |

En 007 se señaló que el Draft y los bloqueantes P-001/P-002/P-003 del análisis anterior ya no describen la spec local, que contiene las aclaraciones de 2026-10-06 y declara `Ready for planning`. Esto no acredita implementación ni resuelve automáticamente los demás impactos. En 006 no se eligió revertir aclaraciones ni abrir una spec duplicada para justificar divergencias.

## Cobertura del Sprint 2

**Antecedentes, no convención vigente:** las claves REQ con sufijos US/TSK de las tablas siguientes registran la identificación heredada del tablero. No son US/Tasks formales ni unidades ejecutables. Se preservan ID, URL y contenido como evidencia; su migración a épicas requiere una tarea operativa posterior autorizada. Esta enmienda documental no renombra ni sincroniza tarjetas.

**Presentación y entrega:** martes 03/11/2026. No se fija una hora no informada por el docente. Las tarjetas tienen la etiqueta `Sprint 2`; la fecha se conserva en sus descripciones, sin inventar una hora para el campo de vencimiento.

Todos los requisitos específicos de la entrega tienen seguimiento. Los requisitos generales que soportan esos flujos se distinguen de los puntos expresamente pedidos en el sprint; esta organización no agrega exigencias académicas. No existen aún specs de estas funcionalidades: las asociaciones numéricas anticipadas del tablero anterior quedan únicamente como antecedente histórico, no como dependencias vigentes.

| Clave provisional | Historia o tarea | Tarjeta | Fuente y relación |
| --- | --- | --- | --- |
| REQ-S2-01 \| US01 | Valuar jugadores con estrategias configurables | [Trello](https://trello.com/c/rDk6ml49) | Cálculo según estrategia; enunciado §3.2 |
| REQ-S2-02 \| US01 | Obtener métricas para la valuación de jugadores | [Trello](https://trello.com/c/hhBUA2WO) | Soporte de valuación; fuentes del enunciado §1.1 y §3.2 |
| REQ-S2-03 \| US01 | Consultar la cotización actual del jugador | [Trello](https://trello.com/c/3UZQeRnF) | Cotización; enunciado §1.2 y §3.2 |
| REQ-S2-04 \| US01 | Consultar la cotización a una fecha dada | [Trello](https://trello.com/c/b2JH99so) | Funcionalidad explícita del sprint |
| REQ-S2-05 \| US01 | Consultar el historial de cotizaciones | [Trello](https://trello.com/c/LtLmpdrJ) | Soporte de cotización por fecha; enunciado §3.2 y §4 |
| REQ-S2-06 \| US01 | Comprar tokens de un jugador | [Trello](https://trello.com/c/aJe4VVNt) | Compra/venta del sprint; enunciado §3.3 |
| REQ-S2-07 \| US01 | Vender tokens de un jugador | [Trello](https://trello.com/c/sZFzbryR) | Compra/venta del sprint; enunciado §3.3 |
| REQ-S2-08 \| US01 | Consultar ranking según estrategia activa | [Trello](https://trello.com/c/ZGBTFbHZ) | Ranking explícito del sprint |
| REQ-S2-09 \| US01 | Consultar portfolio personal | [Trello](https://trello.com/c/43T7hVOh) | Soporte del mercado; enunciado §3.4 |
| REQ-S2-10 \| US01 | Consultar historial de operaciones | [Trello](https://trello.com/c/BLFLKDlL) | Historial explícito del sprint; auditoría financiera del TP |
| REQ-S2-11 \| TSK01 | Programar y ejecutar recálculo de cotizaciones | [Trello](https://trello.com/c/fxWc1znJ) | Soporte de cotización; scheduler del enunciado §5.4 |
| REQ-S2-12 \| US01 | Ver detalle y evolución de un jugador | [Trello](https://trello.com/c/KViJvig1) | Frontend de soporte; enunciado §11 |
| REQ-S2-13 \| US01 | Visualizar ranking de jugadores | [Trello](https://trello.com/c/cIvVwJBf) | Frontend de soporte; enunciado §11 |
| REQ-S2-14 \| US01 | Operar compra y venta desde el frontend | [Trello](https://trello.com/c/ybsdrqJu) | Frontend autenticado; enunciado §11 |
| REQ-S2-15 \| US01 | Visualizar portfolio personal | [Trello](https://trello.com/c/s4CtKyVM) | Frontend de soporte; enunciado §11 |
| REQ-S2-16 \| US01 | Visualizar historial de transacciones | [Trello](https://trello.com/c/VR5BnZAq) | Frontend de soporte del historial |
| REQ-S2-17 \| US01 | Visualizar datos de usuario con gráficos | [Trello](https://trello.com/c/B7VgjMih) | Nuevo requisito del comunicado |
| REQ-S2-18 \| US01 | Definir y entregar una feature adicional | [Trello](https://trello.com/c/De0cmT1N) | Nuevo requisito y condiciones de presentación |
| REQ-S2-19 \| TSK01 | Separar profiles de testing unitario y e2e | [Trello](https://trello.com/c/pta8t6vP) | Único Core pendiente según comunicado |

### Pendientes funcionales y de evaluación

- Elegir la feature adicional; los ejemplos docentes no son elecciones ni requisitos obligatorios. Su tarjeta incluye ideación, planificación, implementación y presentación con justificación de producto/técnica, demo y explicación del código.
- Aclarar los datos y gráficos concretos del usuario antes de especificar su visualización.
- Conservar las ambigüedades del [enunciado](../tp/enunciado.md#notas-editoriales-ambigüedades-y-aspectos-pendientes-de-aclaración): fuentes, periodicidad, versión de estrategia, valor jugador/token, contraparte de venta, saldo inicial y forma de consulta de cotizaciones, entre otras.
- El control de entrega conserva los tres escenarios mínimos de evaluación: construir la base desde fuentes externas; un jugador de cada liga en distintas fechas; cuatro usuarios y compra de cinco jugadores con valoración actual. No se inventa si los cinco jugadores corresponden a cada usuario o al total ni cuántos tokens se compran.
- Build en Verde: el comunicado no aclara expresamente su vigencia. Tiempo de presentación: pendiente de comunicación docente.
- HSQLDB/H2, datos al arranque, configuración Swagger y JOB Coverage ya no son pendientes Core autónomos de Sprint 2. Esto no acredita implementación ni elimina la documentación de endpoints nuevos. Se mantuvieron los antecedentes de las tarjetas actualizadas.

## Requisitos generales sin sprint confirmado

Estas tarjetas no se asignan automáticamente a Sprint 2 ni sustituyen los requisitos de Sprint 3.

| Clave | Propósito | Tarjeta | Observación |
| --- | --- | --- | --- |
| REQ-TP-01 \| TSK01 | Mantener documentación OpenAPI de las APIs | [Trello](https://trello.com/c/G1MM7v7n) | Obligación continua; no repetir configuración como Core pendiente de Sprint 2 |
| REQ-TP-02 \| US01 | Filtrar realmente el catálogo de jugadores | [Trello](https://trello.com/c/Locmfeqz) | Enunciado exige filtros; 002 los excluye y 005 solo tiene controles visuales |
| REQ-TP-03 \| TSK01 | Completar alcance responsivo del frontend | [Trello](https://trello.com/c/u3j3USq1) | Enunciado exige web responsiva; 005 excluye móvil |
| REQ-TP-04 \| TSK01 | Incorporar caché y tolerancia a fallas externas | [Trello](https://trello.com/c/sFJeiQmn) | Caché obligatoria; Redis/in-memory son ejemplos; SLAs e índices sin valores inventados |

## Estado de sincronización

La reorganización incluye todas las tarjetas previamente activas del tablero y conserva sus URL, comentarios, adjuntos, miembros y checklists. Las descripciones anteriores se mantienen como antecedentes cuando cambia la identificación o el alcance agrupado. Las tarjetas nuevas desglosan historias existentes o requisitos documentados; no se crearon specs futuras ni se implementaron funcionalidades.

La verificación final compara las 25 claves de US contra las specs locales y comprueba la unicidad y presencia de las claves de requisitos y tareas en el tablero. Los estados del índice deben actualizarse con el seguimiento posterior, no interpretarse como resultados de tests.

**Operaciones pendientes de sincronización:** ninguna tras la comprobación de esta reorganización. Permanecen los pendientes funcionales, técnicos y de evaluación descritos arriba; no son fallos de conexión con Trello.

---

## Retiro de herramientas de documentación (2026-10-14)

**Decisión del proyecto.** Se retiraron del repositorio la colección Postman y las integraciones de
Spring REST Docs y Asciidoctor.

**Efecto sobre este documento.** Ninguna tarjeta, clave de historia, clave de requisito, clave de
tarea, prioridad ni registro de análisis cambia. El documento conserva su historial de planificación
íntegro, incluidas las entradas que mencionan REST Docs al describir la separación de
responsabilidades entre assertions y snippets dentro de una misma clase de test, y las que registran
tareas de generación de documentación como tareas de Service.

**Alcance practical.** Esas referencias describen decisiones y tareas de un estado del proyecto ya
superado. No habilitan ninguna acción vigente sobre herramientas que ya no forman parte del
repositorio. La cobertura funcional de las clases de Controller no se vio afectada: el retiro eliminó
las integraciones que generaban documentación derivada de las aserciones, no las aserciones.

**Trazabilidad preservada.** La separación de responsabilidades entre las aserciones de un test de
Controller y la documentación derivada sigue siendo la práctica vigente: las aserciones permanecen en
las clases de test y el contrato se sostiene en `specs/*/contracts/`, en las anotaciones OpenAPI y en
esas mismas pruebas.

---

## Deuda técnica pendiente — Seguridad de Actuator (2026-10-14)

El registro centralizado y único de deuda técnica del proyecto es
[docs/TECHNICAL_DEBT.md](../docs/TECHNICAL_DEBT.md). La deuda sobre los endpoints de monitoreo de
Spring Boot Actuator está registrada allí como **TD-001**, con prioridad alta y estado `Pendiente`.

Esta anotación se conserva por trazabilidad del trabajo realizado en esta fecha; no debe
actualizarse de forma independiente del registro.

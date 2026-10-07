# Índice de trazabilidad con Trello

## Reconciliación del protocolo vigente — 2026-10-07

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
| [007](007-team-league-domain/spec.md) | US01 — Reconocer al equipo y a la liga del jugador | [Trello](https://trello.com/c/FWEz7xjP) | Backlog |
| [007](007-team-league-domain/spec.md) | US02 — Sincronizar el catálogo reconociendo ligas y equipos | [Trello](https://trello.com/c/Q2Yg1P1l) | Backlog |
| [007](007-team-league-domain/spec.md) | US03 — Conservar la identidad del equipo ante cambios del proveedor | [Trello](https://trello.com/c/k7iw3LKH) | Backlog |
| [007](007-team-league-domain/spec.md) | US04 — Resolver la identidad externa del equipo sin bloquear el catálogo | [Trello](https://trello.com/c/rHQkeh3k) | Backlog |
| [007](007-team-league-domain/spec.md) | US05 — Mantener en el catálogo a los equipos y jugadores que lo abandonan | [Trello](https://trello.com/c/6n04meCE) | Backlog |
| [007](007-team-league-domain/spec.md) | US06 — Migrar el catálogo existente sin perder jugadores | [Trello](https://trello.com/c/94ktF8ch) | Backlog |

**Done heredado:** el usuario autorizó reorganizar las tarjetas conservando sus estados. Las cinco tarjetas agrupadas de 001–005 estaban en Done; se repartieron en sus 15 historias conservando ese seguimiento y sin una validación nueva. No se cambió el `Status` de las specs ni se cerraron hallazgos técnicos. Las US de 006 y 007 quedan en Backlog y sin asignación de sprint confirmada.

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

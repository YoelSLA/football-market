# Registro de deuda técnica

## 1. Introducción y propósito

Este documento es el registro único y centralizado de la deuda técnica del proyecto Football
Market. Su propósito es que todo problema real detectado durante una implementación, revisión o
refactorización quede escrito, trazado y con criterio de cierre explícito, en lugar de depender de
la memoria, de un chat o de una anotación dispersa en los artefactos de una feature.

El registro no sustituye la documentación normativa ni las especificaciones. Las specs siguen
siendo la verdad funcional de cada feature y la deuda técnica no puede relajar sus requisitos: una
deuda describe trabajo diferido, nunca comportamiento deseado omitido.

## 2. Convenciones de registro

- Cada deuda tiene un identificador único con el formato `TD-NNN`, numerado de forma correlativa.
- **Los identificadores no se reutilizan**, tampoco cuando una deuda se resuelve.
- **No se eliminan las deudas resueltas**: su estado cambia a `Resuelta` y se conserva la entrada
  completa, incluida la fecha de resolución.
- **No se modifica la fecha de creación original** de una deuda, aunque luego se actualice.
- **No se registran deudas especulativas.** Solo problemas reales, comprobados y descritos con
  evidencia.
- **No se duplican deudas.** Si un problema ya está registrado, se actualiza su entrada en lugar de
  crear otra.
- **Toda actualización indica fecha, hora y descripción del cambio** en el historial de la entrada.
- **No se declara una deuda resuelta sin evidencia verificable**, en [testing](backend/testing.md)
  o en la verificación que la deuda misma define.
- Las fechas y horas usan el formato `YYYY-MM-DD HH:MM:SS` en la zona `America/Argentina/Buenos_Aires`,
  obtenidas del reloj del sistema. No se inventan fechas históricas: cuando una deuda se identificó
  antes de existir este registro, la fecha registrada es la de su incorporación al documento.
- Cuando una deuda requiere una decisión del responsable del proyecto, su estado es `Pendiente`
  hasta que esa decisión se tome y se registre.

## 3. Estados posibles

| Estado | Significado |
| --- | --- |
| `Pendiente` | Identificada, sin decisión tomada o pendiente de autorización para resolverla. |
| `En curso` | Con decisión tomada y trabajo autorizado, todavía sin verificar. |
| `Resuelta` | Implementada y verificada con la evidencia que la propia entrada define. |
| `Descartada` | Se evaluó y se descartó, con la justificación registrada en su historial. |

## 4. Prioridades

| Prioridad | Criterio |
| --- | --- |
| `Alta` | Afecta la seguridad, la integridad de los datos o la corrección de un contrato; puede exponer información o permitir un uso indebido. |
| `Media` | Degrada la mantenibilidad, el rendimiento o la consistencia interna sin impacto directo en el usuario. |
| `Baja` | Mejora de legibilidad, limpieza o consistencia sin efecto observable. |

## 5. Índice de deudas técnicas

| ID | Título | Área | Prioridad | Estado | Registrada |
| --- | --- | --- | --- | --- | --- |
| [TD-001](#td-001--protección-de-los-endpoints-de-monitoreo-de-spring-boot-actuator) | Protección de los endpoints de monitoreo de Spring Boot Actuator | Backend / Seguridad | Alta | Pendiente | 2026-10-14 16:29:48 |
| [TD-002](#td-002--autorización-por-rol-inexistente-prometida-en-el-contrato-público) | Autorización por rol inexistente, prometida en el contrato público | Backend / Seguridad | Alta | Pendiente | 2026-10-14 17:48:44 |
| [TD-003](#td-003--sincronizaciones-del-catálogo-abiertas-a-cualquier-usuario-autenticado) | Sincronizaciones del catálogo abiertas a cualquier usuario autenticado | Backend / Seguridad | Alta | Pendiente | 2026-10-14 17:48:44 |
| [TD-004](#td-004--migración-v5-reescrita-tras-su-publicación-rompe-la-validación-de-flyway) | Migración V5 reescrita tras su publicación rompe la validación de Flyway | Backend / Datos | Alta | Pendiente | 2026-10-14 17:48:44 |
| [TD-005](#td-005--perfil-dev-activo-por-defecto-con-credenciales-de-base-de-datos-versionadas) | Perfil `dev` activo por defecto con credenciales de base de datos versionadas | Backend / Configuración | Alta | Pendiente | 2026-10-14 17:48:44 |
| [TD-006](#td-006--peso-de-los-gifs-de-la-pantalla-de-acceso) | Peso de los GIFs de la pantalla de acceso | Frontend / Rendimiento | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-007](#td-007--dos-peticiones-de-sesión-actual-por-navegación) | Dos peticiones de sesión actual por navegación | Frontend / Rendimiento | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-008](#td-008--índice-ausente-en-pending_review_cases) | Índice ausente en `pending_review_cases` | Backend / Rendimiento | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-009](#td-009--carga-completa-de-jugadores-por-cada-equipo-protegido) | Carga completa de jugadores por cada equipo protegido | Backend / Rendimiento | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-010](#td-010--el-historial-de-sincronización-de-imágenes-ejecuta-borrado-en-una-lectura) | El historial de sincronización de imágenes ejecuta borrado en una lectura | Backend / Datos | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-011](#td-011--cobertura-incompleta-del-manejador-global-de-excepciones) | Cobertura incompleta del manejador global de excepciones | Backend / Contrato | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-012](#td-012--integración-continua-sin-verificación-del-frontend-ni-análisis-de-dependencias) | Integración continua sin verificación del frontend ni análisis de dependencias | Transversal / Infraestructura | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-013](#td-013--divergencia-en-la-convención-de-zona-horaria-entre-perfiles) | Divergencia en la convención de zona horaria entre perfiles | Backend / Datos | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-014](#td-014--contratos-y-documentación-desalineados-del-código) | Contratos y documentación desalineados del código | Transversal / Documentación | Media | Pendiente | 2026-10-14 17:48:44 |
| [TD-015](#td-015--lectura-de-detalle-de-jugador-fuera-de-una-transacción-de-lectura) | Lectura de detalle de jugador fuera de una transacción de lectura | Backend / Datos | Baja | Pendiente | 2026-10-14 17:48:44 |
| [TD-016](#td-016--artefacto-de-compilación-incremental-versionado-en-git) | Artefacto de compilación incremental versionado en Git | Frontend / Configuración | Baja | Pendiente | 2026-10-14 17:48:44 |
| [TD-017](#td-017--conflicto-de-formato-entre-el-importador-de-escudos-y-biome) | Conflicto de formato entre el importador de escudos y Biome | Frontend / Herramientas | Baja | Pendiente | 2026-10-14 17:48:44 |
| [TD-018](#td-018--colores-literales-en-los-módulos-de-estilo-de-componentes) | Colores literales en los módulos de estilo de componentes | Frontend / Convenciones | Baja | Pendiente | 2026-10-14 17:48:44 |
| [TD-019](#td-019--precisión-del-rating-incompatible-con-el-diseño-de-persistencia-de-spec-011) | Precisión del rating incompatible con el diseño de persistencia de SPEC-012 | Backend / Datos | Alta | Pendiente | 2026-10-16 02:49:30 |
| [TD-020](#td-020--contraste-entre-canales-retirado-en-spec-011) | Contraste entre canales retirado en SPEC-012 | Backend / Datos | Media | Pendiente | 2026-10-18 00:00:00 |
| [TD-021](#td-021--re-evaluación-con-una-campaña-de-métricas-ya-usada-reprecia-con-datos-estancados) | Re-evaluación con una campaña de métricas ya usada reprecia con datos estancados | Backend / Datos | Media | Pendiente | 2026-10-21 16:20:55 |
| [TD-022](#td-022--ranking-de-cotizaciones-sin-desempate-deterministica-un-jugador-puede-aparecer-dos-veces) | Ranking de cotizaciones sin desempate determinista: un jugador puede aparecer dos veces | Backend / Datos | Media | En curso | 2026-10-21 22:20:53 |
| [TD-023](#td-023--jugador-cotizable-sin-fila-initial-falla-en-cada-corrida-de-recálculo) | Jugador cotizable sin fila `INITIAL` falla en cada corrida de recálculo | Backend / Datos | Media | Pendiente | 2026-10-21 22:20:53 |
| [TD-024](#td-024--cotizaciones-confirmadas-sin-resumen-de-ejecución-persistido) | Cotizaciones confirmadas sin resumen de ejecución persistido | Backend / Datos | Media | Pendiente | 2026-10-21 22:20:53 |
| [TD-025](#td-025--quote_executions-sin-estado-una-ejecución-en-curso-es-indistinguible-de-una-terminada) | `quote_executions` sin estado: una ejecución en curso es indistinguible de una terminada | Backend / Datos | Media | Pendiente | 2026-10-21 22:20:53 |
| [TD-026](#td-026--inicialización-concurrente-del-mercado-responde-500-en-lugar-de-409) | Inicialización concurrente del mercado responde 500 en lugar de 409 | Backend / Contrato | Media | Descartada | 2026-10-21 22:20:53 |
| [TD-027](#td-027--índice-ausente-sobre-player_quotesorigin-en-el-camino-caliente-de-compra-y-publicación) | Índice ausente sobre `player_quotes.origin` en el camino caliente de compra y publicación | Backend / Rendimiento | Media | Pendiente | 2026-10-21 22:20:53 |
| [TD-028](#td-028--operaciones-de-cotización-del-mercado-sin-control-de-rol) | Operaciones de cotización del mercado sin control de rol | Backend / Seguridad | Media | Descartada | 2026-10-21 22:20:53 |
| [TD-029](#td-029--cobertura-de-tests-incompleta-para-el-recálculo-de-cotizaciones) | Cobertura de tests incompleta para el recálculo de cotizaciones | Backend / Testing | Media | Pendiente | 2026-10-21 22:20:53 |
| [TD-030](#td-030--cobertura-parcial-del-control-de-imports-scss) | Cobertura parcial del control de imports SCSS | Frontend / Herramientas | Media | Pendiente | 2026-10-22 14:32:17 |

## 6. Detalle de las deudas técnicas

### TD-022 — Ranking de cotizaciones sin desempate determinista: un jugador puede aparecer dos veces

| Campo | Valor |
| --- | --- |
| **ID** | TD-022 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `En curso` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Verificación en la base de desarrollo (2026-10-21).** El ranking ejecutado sobre la base de
desarrollo con el predicado exacto de `findRanking` devuelve **1.860 filas para 1.860 jugadores
distintos**: no hay duplicados en los datos actuales. La tabla contiene 1.860 filas `INITIAL` del
2026-10-19, 1.860 filas `RECOMPUTED` del 2026-10-19 y 1.860 filas `RECOMPUTED` del 2026-10-20, es
decir, tres filas de historial por jugador, de las cuales el ranking expone solo la vigente. El
defecto es latente y no se manifiesta con estos datos: se reproduce únicamente cuando un mismo
jugador tiene dos filas con la **misma fecha de vigencia y el mismo instante de cálculo**, con
orígenes de la familia `INITIAL`/`CARRIED_FORWARD`, condición que solo se alcanza si el instante de
la fila inicial coincide con el de una fila arrastrada.

**Descripción.** Las consultas que resuelven la cotización vigente de cada jugador no tienen un
desempate total. Ordenan por `effective_date`, origen y `calculated_at`, pero no por `id`, de modo
que dos filas del mismo jugador con la misma fecha de vigencia y el mismo instante de cálculo
pueden devolver en cualquier orden. El ranking, que se construye con una condición `not exists`
correlacionada que solo excluye la fila peor cuando el `calculated_at` es estrictamente mayor,
puede devolver **dos filas del mismo jugador** cuando hay empate, contra FR-039, que exige una sola
entrada por jugador cotizable.

**Evidencia.**

- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:26-31` — "última fila
  por jugador": `order by effective_date desc, case origin ..., calculated_at desc`, sin desempate por
  `id`.
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:41-43` y `:55-57` —
  historial y fila con `effective_date <= fecha` con el mismo criterio incompleto.
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:72-88` — ranking: la
  condición `not exists` excluye la fila peor solo con `q2.calculated_at > q.calculated_at`, de modo
  que un empate no excluye ninguna fila.
- `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java:73-83` — construye el
  ranking sin deduplicar por jugador.
- `backend/src/main/java/footballmarket/integrations/RepositoryQuoteSource.java:63-68` — la fuente real
  sí deduplica con `putIfAbsent`, de modo que el mercado queda protegido y la API HTTP de ranking no.

**Causa técnica.** El desempate escrito en `data-model.md §1` y `research.md §2` no se trasladó a un
criterio total en el SQL: falta el identificador de la fila como último criterio de orden y como
condición de exclusión en el ranking.

**Impacto técnico.** Con los datos actuales el ranking es correcto: una sola entrada por jugador,
con su cotización vigente. El riesgo es latente: si un jugador llegara a tener dos filas de la misma
fecha de vigencia y del mismo instante de cálculo, el ranking lo devolvería dos veces, contra
FR-039. El mercado no se ve afectado porque la fuente real deduplica con `putIfAbsent`. El defecto
es observable por HTTP y contradice FR-039 solo bajo esa condición de empate.

**Solución propuesta.** Añadir `id` como criterio final de orden en las tres consultas y extender la
condición de exclusión del ranking con `q2.id > q.id`, o reemplazar el `not exists` por un
`distinct on (player_id)` ordenado por fecha, origen e instante, como ya contempla `research.md`.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Ninguna consulta de vigente, historial, fila por fecha ni ranking devuelva dos filas del mismo
   jugador, incluso ante empate de fecha de vigencia e instante de cálculo.
2. Existan tests backend que cubran el empate de fecha e instante entre las filas `RECOMPUTED` y
   `CARRIED_FORWARD` y que afirmen una sola entrada por jugador en el ranking.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-026, FR-037, FR-039)
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md`
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java`
- `backend/src/main/java/footballmarket/services/impl/QuoteQueryServiceImpl.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-01)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |
| 2026-10-21 22:20:53 | Verificación empírica en la base de desarrollo: el ranking devuelve 1.860 filas para 1.860 jugadores distintos, sin duplicados. El defecto se relega a latente y la prioridad baja de Alta a Media. Se documenta la condición exacta que lo reproduce y la prueba en transacción revertida. No se modificó código, migraciones ni contratos. |
| 2026-10-21 23:05:00 | Corrección aplicada por decisión del responsable del proyecto: el orden de lectura de `PlayerQuoteRepository` cierra con el identificador de la fila y la condición de exclusión del ranking añade `q2.id > q.id` ante empate de fecha de vigencia e instante de cálculo. Se escribieron los casos de empate en `QuoteQueryServiceTest`. El estado pasa a `En curso`: la corrección está implementada y compilada, pero la ejecución de los tests corresponde al usuario y la deuda no se declara resuelta sin esa evidencia. |

### TD-023 — Jugador cotizable sin fila `INITIAL` falla en cada corrida de recálculo

| Campo | Valor |
| --- | --- |
| **ID** | TD-023 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** La población cotizable se resuelve por métricas informadas, pero la fila `INITIAL`
solo la crea la inicialización del mercado, que es idempotente y no completa filas faltantes. Un
jugador que obtiene métricas después de la inicialización es cotizable y a la vez carece de fila
inicial. En cada corrida, el publicador toma la primera fila de la lista de cotizaciones vigentes y
de la lista de fechas de referencia del jugador: al estar vacías, lanza `NoSuchElementException`, que
se contabiliza como fallo del jugador. El jugador queda sin precio, no aparece en el ranking y la
corrida muestra un contador `failed_players` permanente.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java:44` y `:47` —
  `getFirst()` sobre el resultado de la última fila vigente y de las fechas de referencia.
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:26-31` y `:99-110` —
  consultas que devuelven listas vacías cuando el jugador no tiene ninguna fila.
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:115-122` —
  el fallo individual se captura, se registra y se cuenta en `failed_players`.
- `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java:49-57` —
  la idempotencia no crea las filas `INITIAL` que falten.

**Causa técnica.** FR-001 y FR-002 declaran cotizable a todo jugador con métricas informadas, pero la
población cotizable y la población con fila inicial se resuelven con criterios distintos y no hay
cota que los garantice.

**Impacto técnico.** Un jugador incorporable al catálogo después de inicializar el mercado queda
permanentemente sin cotización y desplaza el contador de fallos de cada corrida, sin que exista una
señal que distinga ese caso de un fallo real.

**Solución propuesta.** Tratar la ausencia de fila inicial como una condición de "no operable" y no
como un fallo, y permitir que la inicialización idempotente complete las filas `INITIAL` faltantes de
los jugadores cotizables actuales, o bien derivar la fila inicial de la fecha de mercado vigente
cuando el jugador se vuelve cotizable.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Ningún jugador cotizable sin fila `INITIAL` provoque un fallo ni un contador de fallos en la
   corrida.
2. Existan tests backend que cubran un jugador que obtiene métricas después de la inicialización.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-001, FR-002, FR-034)
- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java`
- `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-02)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |
| 2026-10-21 23:05:00 | Decisión del responsable del proyecto: se conserva como deuda registrada. El caso es muy borde —un jugador que obtiene métricas después de la inicialización del mercado— y no se modifica el código. La entrada permanece `Pendiente` con su evidencia, su causa y su regla de resolución propuesta. |

### TD-024 — Cotizaciones confirmadas sin resumen de ejecución persistido

| Campo | Valor |
| --- | --- |
| **ID** | TD-024 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** La corrida completa se ejecuta dentro de una transacción, pero la publicación de
cada jugador usa `Propagation.REQUIRES_NEW` y confirma su fila por separado. Si la transacción
global revierte después del bucle —por ejemplo, ante un fallo al guardar el resumen de la
ejecución— las filas `RECOMPUTED` y `CARRIED_FORWARD` quedan confirmadas sin que exista la fila de
`quote_executions` que las documenta. El precio se movió sin corrida registrada, y la fecha de
cotización queda libre para una segunda ejecución que ya no puede reproducir el salto.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java:34` —
  `REQUIRES_NEW` en la publicación por jugador.
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:32` —
  transacción de la corrida.
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:133-139` —
  actualización del resumen al final de la corrida.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/research.md §13` — justificación del
  `REQUIRES_NEW` por atomicidad individual.

**Causa técnica.** La atomicidad por jugador y la atomicidad de la corrida se implementan con
alcances transaccionales distintos, sin una decisión sobre qué ocurre cuando el segundo nivel falla
después de que el primero confirmó.

**Impacto técnico.** Historial de cotizaciones sin resumen de ejecución, que es lo que FR-061 exige
consultar para reconstruir el cálculo. La trazabilidad se degrada sin que el sistema lo detecte.

**Solución propuesta.** Definir y aplicar una de las dos alternativas: ejecutar toda la corrida en
una única transacción con.Savepoint por jugador, o registrar la ejecución con un estado que
distinga "en curso" de "terminada" y permita reconciliar las filas confirmadas con su resumen.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Ninguna fila de `player_quotes` pueda quedar confirmada sin su resumen de ejecución, o exista un
   estado que haga detectable esa situación.
2. Existan tests backend que simulate un fallo al persistir el resumen y afirmen el estado
   resultante.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-061, FR-062)
- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java`
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-03)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |

### TD-025 — `quote_executions` sin estado: una ejecución en curso es indistinguible de una terminada

| Campo | Valor |
| --- | --- |
| **ID** | TD-025 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** La fila de `quote_executions` se inserta como reserva al inicio de la corrida, con
los cinco contadores en cero y `finished_at` igual a `started_at`, y se actualiza al final con los
contadores reales. La tabla no tiene columna de estado, de modo que un lector concurrente que
consulte el resumen de ejecuciones puede leer una corrida todavía en curso como si estuviera
terminada, con contadores en cero. Además, la tabla se califica como append-only en el diseño de
persistencia y su comentario de migración, pero admite actualización.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:95-108` —
  inserción de la fila de reserva.
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:133-139` —
  actualización final del resumen.
- `backend/src/main/java/footballmarket/models/QuoteExecution.java:157-176` — `complete()` actualiza
  la entidad; `finished_at` es actualizable y `started_at` no.
- `backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql:30` — comentario
  que describe la tabla como append-only.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md §2` — misma calificación.

**Causa técnica.** La reserva anticipada de la fecha de cotización resuelve la unicidad de FR-060,
pero el modelo no incorpora un estado que distinga la corrida en curso de la terminada, y la
documentación declara un carácter append-only que la implementación no respeta.

**Impacto técnico.** Un consumidor del resumen de ejecuciones puede interpretar una corrida en
curso como una corrida terminada y sin efectos, y el desajuste entre documentación e
implementación impide afirmar la trazabilidad de la tabla.

**Solución propuesta.** Añadir una columna de estado (`IN_PROGRESS` / `COMPLETED`) con su
restricción, o persistir la fila de ejecución solo al final y proteger la unicidad por fecha con un
mecanismo distinto, como una fila de reserva con marca temporal.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Un lector del resumen pueda distinguir una ejecución en curso de una terminada.
2. `data-model.md` y el comentario de la migración reflejen el carácter real de la tabla.
3. Existan tests backend que consulten el resumen durante una ejecución en curso.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-060, FR-061)
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md`
- `backend/src/main/java/footballmarket/models/QuoteExecution.java`
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-04)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |

### TD-026 — Inicialización concurrente del mercado responde 500 en lugar de 409

| Campo | Valor |
| --- | --- |
| **ID** | TD-026 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Descartada` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Contrato |
| **Fecha de resolución** | - |

**Decision del responsable del proyecto.** La carrera de inicializacion concurrente se descarta: no
ocurre en el uso previsto del mercado, donde la inicializacion se ejecuta una sola vez y de forma
manual. La entrada se conserva con su evidencia por trazabilidad, y no se modifica el codigo.

**Descripción.** El camino secuencial de la inicialización detecta el mercado ya inicializado y
responde `409 MARKET_ALREADY_INITIALIZED`. Dos inicializaciones concurrentes con fechas distintas no
discurren por ese camino: la perdedora falla por violación de unicidad o de restricción y la
excepción de integridad se traduce al manejador genérico, que responde `500 INTERNAL_ERROR`. El
contrato HTTP de la feature exige `409` para esa situación.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java:48-57`
  — detección secuencial del mercado ya inicializado.
- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java:185-197` —
  traducción de la violación de integridad a error interno.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md §1` — código
  `MARKET_ALREADY_INITIALIZED` con estado 409.
- `backend/src/main/java/footballmarket/services/impl/QuoteRecalculationServiceImpl.java:95-99` —
  el análogo de la corrida de recálculo sí traduce la violación de unicidad al código controlado.

**Causa técnica.** La traducción de la violación de integridad al código de negocio existe en el
servicio de recálculo y no en el de inicialización.

**Impacto técnico.** Contrato incumplido en concurrencia: un cliente concurrente recibe un 500 en
lugar del 409 documentado, y el error interno degrada el diagnóstico de la causa real.

**Solución propuesta.** Traducir la violación de integridad de la inicialización al mismo
`MarketAlreadyInitializedException` que usa el camino secuencial.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Dos inicializaciones concurrentes con fechas distintas produzcan una respuesta `409
   MARKET_ALREADY_INITIALIZED` y ninguna `500`.
2. Existan tests backend que cubran la carrera.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md §1`
- `backend/src/main/java/footballmarket/services/impl/QuoteMarketInitializationServiceImpl.java`
- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-05)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |
| 2026-10-21 23:05:00 | Descartada por decisión del responsable del proyecto: la inicialización se ejecuta una sola vez y de forma manual, de modo que la carrera no se considera alcanzable. Se conserva la entrada con su evidencia y no se modifica código, migraciones ni contratos. |

### TD-027 — Índice ausente sobre `player_quotes.origin` en el camino caliente de compra y publicación

| Campo | Valor |
| --- | --- |
| **ID** | TD-027 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Rendimiento |
| **Fecha de resolución** | — |

**Descripción.** La tabla `player_quotes` no tiene ningún índice sobre la columna `origin`. La
existencia de una fila `INITIAL`, que es el único criterio de mercado inicializado, se consulta en
cada compra y en cada publicación de venta, y además se cuenta al inicializar el mercado. Sin
índice, cada consulta es un recorrido secuencial de la tabla completa.

**Evidencia.**

- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:123` —
  `existsByOrigin`.
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java:131` y `:117` —
  `countByOrigin` y `findMarketDates`.
- `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java:95` y `:200` — guardas
  de mercado no inicializado en la compra.
- `backend/src/main/java/footballmarket/services/impl/TokenListingServiceImpl.java:66` — guarda en la
  publicación de venta.
- `backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql:27-28` — únicos
  índices creados: `ix_player_quotes_player_date` e `ix_player_quotes_effective_price`.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md §1` — el diseño de persistencia
  no prevé índice sobre `origin`.

**Causa técnica.** El diseño de índices no contempló que `origin` participara en el camino caliente
de las operaciones de mercado.

**Impacto técnico.** Degradación de rendimiento de cada operación de mercado a medida que crezca el
historial de cotizaciones. No afecta la corrección funcional.

**Solución propuesta.** Crear una migración con un índice parcial sobre `origin` para la fila
`INITIAL`, o un índice compuesto que cubra las consultas de mercado, y actualizar `data-model.md`.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. El plan de ejecución de la consulta de mercado inicializado use un índice.
2. Exista una migración que lo cree y el `data-model.md` la refleje.

**Referencias.**

- `backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql`
- `backend/src/main/java/footballmarket/repositories/PlayerQuoteRepository.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-06)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |

### TD-028 — Operaciones de cotización del mercado sin control de rol

| Campo | Valor |
| --- | --- |
| **ID** | TD-028 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Descartada` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Seguridad |
| **Fecha de resolución** | - |

**Decision del responsable del proyecto.** La ausencia de control de rol para inicializar el mercado
y ejecutar recálculos se descarta: el acceso a esas operaciones es un uso interno y deliberado del
equipo, y la spec no introduce roles nuevos (FR-059). La ausencia de autorizacion por rol del
proyecto ya esta registrada en [TD-002](#td-002--autorización-por-rol-inexistente-prometida-en-el-contrato-público),
que sigue vigente; esta entrada se conserva con su evidencia por trazabilidad y no se modifica el
codigo.

**Descripción.** La inicialización del mercado y el recálculo de cotizaciones son mutaciones globales
del mercado: la primera fija la fecha de mercado y el ancla de precio de todo el catálogo, y la
segunda reescribe precios y consume la unicidad de la fecha de cotización, de modo que puede bloquear
la corrida legítima del día. Ambas exigen solamente un JWT válido, sin ninguna distinción de rol,
porque la feature no introduce roles nuevos (FR-059) y el modelo de autorización del proyecto es
"autenticado o no autenticado".

**Evidencia.**

- `backend/src/main/java/footballmarket/config/SecurityConfig.java:112-113` — `anyRequest()
  .authenticated()` como única regla.
- `backend/src/main/java/footballmarket/controllers/QuoteController.java:50` y `:84` — sin
  `@PreAuthorize` ni verificación adicional.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-059) — la feature no introduce
  roles nuevos.
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` — todas las rutas
  exigen JWT válido.

**Causa técnica.** La ausencia de autorización por rol es una decisión de alcance ya registrada en
[TD-002](#td-002--autorización-por-rol-inexistente-prometida-en-el-contrato-público), que el
proyecto decidió de forma consciente. Lo que no estaba evaluado es el efecto de esa decisión sobre las
nuevas mutaciones globales del mercado.

**Impacto técnico.** Cualquier usuario registrado puede fijar la fecha y el ancla del mercado y
disparar un recálculo global. No hay exposición de información ni bypass de autenticación.

**Solución propuesta.** Decidir con el responsable del proyecto si las dos operaciones exigen un rol
administrativo o superusuario, o si la superficie se acepta y se documenta. La decisión debe
incorporarse al modelo de autorización que TD-002 ya tiene pendiente.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Exista una decisión registrada sobre el control de rol de estas dos operaciones.
2. Si se decide protegerlas, las rutas apliquen una regla explícita y existen tests backend que lo
   afirmen.

**Referencias.**

- `docs/TECHNICAL_DEBT.md` (TD-002, TD-003)
- `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- `backend/src/main/java/footballmarket/controllers/QuoteController.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-07)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |
| 2026-10-21 23:05:00 | Descartada por decisión del responsable del proyecto: el acceso a la inicialización y al recálculo es un uso interno y deliberado del equipo, y la spec no introduce roles nuevos (FR-059). La ausencia general de autorización por rol permanece registrada en TD-002. Se conserva la entrada con su evidencia y no se modifica código, migraciones ni contratos. |

### TD-029 — Cobertura de tests incompleta para el recálculo de cotizaciones

| Campo | Valor |
| --- | --- |
| **ID** | TD-029 |
| **Fecha y hora de registro** | 2026-10-21 22:20:53 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Testing |
| **Fecha de resolución** | — |

**Descripción.** La suite escrita para la feature no cubre varios comportamientos exigidos por la
spec. El más sensible es el fallo individual de un jugador (FR-062): no existe ninguna forma de
provocarlo en los tests, de modo que una regresión que aborte la corrida entera al primer fallo
seguiría en verde. Tampoco están cubiertos la rama `GENERAL_ATTRIBUTES` (FR-006, FR-009, FR-019), el
puerto de cotizaciones por lotes `currentQuotes(Set)`, que es la ruta de revaluación del portfolio y
perdió su test con la retirada del simulador, el modo de redondeo `HALF_UP` y la escala de seis
decimales del multiplicador (FR-016), y la ausencia de escritura de filas y de resumen tras cada
rechazo (FR-060, FR-063).

**Evidencia.**

- `backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java` — ningún caso
  provoca un fallo individual; los contadores `carriedForwardRows`, `notRepricedPlayers` y
  `failedPlayers` no se afirman sobre el resumen persistido; las validaciones de tope y piso solo se
  ejercitan con objetos simulados en el controlador; la estrategia `GENERAL_ATTRIBUTES` nunca se usa.
- `backend/src/test/java/footballmarket/models/QuoteAdjustmentTest.java:19-26` — el caso de fórmula
  no distingue el modo de redondeo ni la escala del multiplicador.
- `backend/src/test/java/footballmarket/integrations/RepositoryQuoteSourceTest.java` — sin caso de
  lote para `currentQuotes(Set)`.
- `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java` (eliminado) —
  la retirada del simulador no tiene un test sustituto que acredite que el puerto resuelve contra el
  repositorio canónico (SC-011).

**Causa técnica.** La suite se construyó caso por caso sobre los criterios de aceptación, sin una
fase de mutación ni una revisión de qué ramas del código quedan sin ejercitar.

**Impacto técnico.** Regresiones en FR-062, FR-006, FR-009, FR-016, FR-019, FR-060 y FR-063 no se
detectarían. La ejecución y la evidencia de los tests quedan a cargo del usuario.

**Solución propuesta.** Ampliar la suite con un caso de fallo individual por jugador, con los
contadores completos sobre el resumen persistido, con la rama `GENERAL_ATTRIBUTES`, con el lote del
puerto, con un caso de redondeo discriminante y con aserciones de ausencia de efectos tras cada
rechazo.

**Criterios de resolución.** La deuda no se considera resuelta hasta que existan tests backend que
cubran los comportamientos enumerados y el usuario los ejecute con resultado satisfactorio.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-006, FR-009, FR-016, FR-019,
  FR-060, FR-062, FR-063)
- `backend/src/test/java/footballmarket/services/QuoteRecalculationServiceTest.java`
- `backend/src/test/java/footballmarket/models/QuoteAdjustmentTest.java`
- `backend/src/test/java/footballmarket/integrations/RepositoryQuoteSourceTest.java`
- `docs/backend/testing.md`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-10)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 22:20:53 | Registro inicial tras el code review de SPEC-016. No se modificó código, migraciones ni contratos. |

### TD-021 — Re-evaluación con una campaña de métricas ya usada reprecia con datos estancados

| Campo | Valor |
| --- | --- |
| **ID** | TD-021 |
| **Fecha y hora de registro** | 2026-10-21 16:20:55 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | - |

**Descripción.** Cuando la campaña JSON de métricas vigente no resulta utilizable, la capa de valuación
reutiliza en silencio la campaña confirmada anterior y sella esas valuaciones con la fecha del día de
la evaluación, no con la fecha de extracción de la campaña. El recálculo de ese día ve entonces una
valuación posterior a la fecha de vigencia de referencia de cada jugador y vuelve a repreciar a todos
con el mismo score, sobre métricas que no cambiaron. El tope de variación acota cada paso, pero no la
cantidad de pasos: mientras la campaña siga sin ser utilizable, cada corrida semanal añade un paso al
precio de todos los jugadores.

No forma parte de esta deuda el comportamiento que el responsable del proyecto confirmó como deseado:
un jugador cuyo item de campaña falló con `NO_DATA`, `NOT_FOUND` o `PROVIDER_ERROR` conserva su
precio, no genera fila ese día y se reprecia cuando una campaña posterior aporte sus métricas.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/internal/ValuationSnapshotSelector.java:50-57` — itera
  las fechas de campaña en orden descendente y devuelve la primera confirmada, de modo que una campaña
  no utilizable se descarta y se reutiliza la anterior, sin señal alguna en la respuesta.
- `backend/src/main/java/footballmarket/services/internal/ValuationSnapshotSelector.java:60-70` —
  `isConfirmed` rechaza la campaña completa cuando un único item JSON queda `INCOMPLETE`,
  `INTERRUPTED`, `BLOCKED` o `STRUCTURE_CHANGED`.
- `backend/src/main/java/footballmarket/services/impl/ValuationEvaluationServiceImpl.java:52` —
  `valuationDate` es la fecha UTC de la evaluación, con independencia de la fecha de extracción de la
  campaña usada.
- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java:48` — la fila se
  escribe como `RECOMPUTED` cuando `valuationDate.isAfter(referenceDate)`, condición que satisface una
  valuación re-sellada con el día en curso.
- `backend/src/main/java/footballmarket/services/impl/JsonStatisticsUpdateServiceImpl.java:79` — un
  jugador bloqueado por el proveedor queda registrado como `BLOCKED` en su campaña.
- `backend/src/main/java/footballmarket/services/impl/PlayerStatisticsSynchronizationServiceImpl.java:58,188`
  — `PROVIDER_BLOCKED` es un código de parada global del recorrido.
- `backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java:20-23` y
  `backend/src/main/java/footballmarket/repositories/AttributeValuationRepository.java:20-23` — la
  resolución toma la valuación más reciente por `valuation_date`, de modo que la re-sellada prevalece
  sobre la anterior.
- `backend/src/main/java/footballmarket/controllers/dtos/responses/ValuationEvaluationResponseDTO.java` —
  el resumen de la evaluación no expone la fecha de extracción de la campaña utilizada, por lo que la
  reutilización de una campaña anterior no es observable por HTTP.

**Causa técnica.** La frescura del dato se pierde en la frontera entre la capa de valuación y la de
cotización: la fecha de valuación es el día de la evaluación y no la fecha de extracción de la campaña,
de modo que "valuación nueva" es indistinguible de "el mismo dato evaluado otra vez". La información
necesaria para distinguir ambos casos sí está persistida (`source_extraction_date` en
`player_positional_valuations` y `player_attribute_valuations`) y no la usa la capa de cotización.

**Impacto técnico.** El precio de todos los jugadores se desplaza sobre datos estancados, un paso
acotado por campaña no utilizable (con los parámetros por defecto, hasta el `variationCap` del 10 % por
paso), y los pasos se acumulan mientras la campaña siga sin ser utilizable; un jugador bloqueado de
forma permanente mantiene su campaña inutilizable durante ese lapso. La traza en `player_quotes`
muestra un `effective_date` que avanza sin que avance el `source_extraction_date` de las valuaciones, y
`POST /api/valuations/evaluate` responde 200 sin indicar que reutilizó una campaña anterior. No afecta
al congelamiento por jugador, que es el comportamiento aceptado.

**Solución propuesta.** Aplicar la regla en el pipeline de la capa de valuación y no como un gate por
jugador en la capa de cotización: no evaluar cuando la fecha de extracción de la campaña seleccionada no
sea más reciente que la fecha de extracción de la última evaluación. La información ya existe, la
frontera permanece limpia (el recálculo sigue sin enterarse de la frescura) y ningún jugador sano queda
bloqueado cuando la campaña es nueva y solo está parcialmente cubierta. La misma regla puede aplicarse
sin cambios de código: no evaluar ni recalcular mientras la campaña de la semana no esté confirmada, y
relanzar la extracción (`POST /api/players/statistics/whoscored/sync/json` o `/sync/scraping` con el
mismo `extractionDate` y `maxPlayers: 1`) hasta que la campaña esté sana o sea parcialmente utilizable.

**Criterios de resolución.** La deuda no se considera resuelta hasta que:

1. Ninguna evaluación selle una valuación de una campaña ya usada por una evaluación anterior.
2. La consulta de auditoría no muestre avance de `effective_date` en `player_quotes` sin avance
   correlativo de `source_extraction_date` en las valuaciones.
3. La regla operativa quede documentada en el quickstart de SPEC-016.
4. Existan tests backend que demuestren el comportamiento con una campaña no utilizable y con una
   campaña parcialmente cubierta.

La ejecución y la evidencia de los tests quedan a cargo del usuario; no se declara resuelta por build ni
por una corrección documental aislada.

**Referencias.**

- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md` (FR-027, FR-060)
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md`
- `backend/src/main/java/footballmarket/services/internal/ValuationSnapshotSelector.java`
- `backend/src/main/java/footballmarket/services/impl/ValuationEvaluationServiceImpl.java`
- `backend/src/main/java/footballmarket/services/internal/QuotePlayerPublisher.java`
- `backend/src/main/java/footballmarket/models/QuoteAdjustment.java`
- `backend/src/main/resources/db/migration/V20261019120000__create_player_valuations.sql`
- `backend/src/main/resources/db/migration/V20261021042325__create_player_quotes.sql`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-21 16:20:55 | Registro inicial tras la revisión funcional de SPEC-016, en la que el responsable del proyecto decidió no modificar el código y conservar el congelamiento por jugador. No se modificó código, migraciones ni contratos. |
<a id="td-021--cobertura-parcial-del-control-de-imports-scss"></a>
### TD-030 — Cobertura parcial del control de imports SCSS

- **Registrada**: 2026-10-22 14:32:17 (`America/Argentina/Buenos_Aires`, reloj del sistema).
- **Estado**: Pendiente; **prioridad**: Media; **área**: Frontend / Herramientas.
- **Evidencia**: `frontend/scripts/scss-check-imports.js` utiliza un patrón con `^@use` sin modo multilinea. Solo inspecciona la posición inicial del texto y no todas las directivas. El mensaje de éxito no acredita la regla completa. Hallazgo por lectura, sin ejecución.
- **Impacto**: un import relativo situado después de otra directiva o comentario puede pasar inadvertido.
- **Solución propuesta**: lectura de directivas SCSS que distinga comentarios y strings, cubra el archivo completo y valide la forma canónica, sin agregar dependencias innecesarias. No aplicar detección React mediante regex.
- **Criterio de cierre**: implementación revisada y evidencia aportada por el usuario de detección de directivas válidas e inválidas en distintas posiciones; respetar las restricciones de ejecución vigentes. No se declara resuelta por el build.
- **Historial**:
  - 2026-10-22 14:32:17 — registrada durante la reorganización global del frontend; no se amplía el alcance para resolverla.
  - 2026-10-23 14:24:46 — al integrar dev en SPEC-017, se reasigna el identificador local TD-021 a TD-030 para resolver la colisión con la deuda de backend «Re-evaluación con una campaña de métricas ya usada reprecia con datos estancados». Se conservan fecha de registro, estado, evidencia y criterio de cierre. Las menciones históricas a TD-021 en el informe de SPEC-017 y en la auditoría de componentes del frontend corresponden a esta deuda; se conserva también el ancla anterior para no romper enlaces.

### TD-019 — Precisión del rating incompatible con el diseño de persistencia de SPEC-012

- **Registrada**: 2026-10-16 02:49:30 (`America/Argentina/Buenos_Aires`), reloj del sistema.
- **Estado**: Pendiente; **prioridad**: Alta; **área**: Backend / Datos.
- **Evidencia**: `RecorridoHTML/PaginaPrincipal.txt`, línea 1219, publica en el bloque
  estructurado `Rating: 6.9033333333333333`. La captura es local e ignorada; no se publica
  su contenido completo ni se accede al proveedor para este hallazgo.
- **Problema**: FR-029 de SPEC-012 exige conservar la precisión publicada sin redondeo,
  mientras `specs/012-player-statistics-scraping/data-model.md` y `research.md` definen
  NUMERIC(18,6). Aplicarlo redondearía un valor real. Rechazarlo por su precisión tampoco
  implementaría el requisito. El conflicto está en el diseño, no en una migración desplegada.
- **Motivo del aplazamiento**: requiere corrección de diseño autorizada y nuevo Analyze;
  IMPLEMENT de US01 se suspende conforme al workflow. La migración y el modelo incompletos
  creados en esta ejecución se retiraron antes de entrega; no se altera una migración existente.
- **Criterio de cierre**: diseño y Tasks coherentes con FR-029, Analyze sin blocker de precisión,
  almacenamiento e implementación que preserven el decimal publicado y tests backend que
  demuestren ida/vuelta exacta del rating con PostgreSQL. Ejecución/evidencia de tests a cargo
  del usuario; no declarar resuelta por build ni por corrección documental aislada.
- **Historial**: 2026-10-16 02:49:30 — registrada durante IMPLEMENT autorizado de US01;
  solución y validación pendientes. No autoriza ampliar alcance ni modificar FR-029.
- **Actualización**: 2026-10-16 02:52:09 (`America/Argentina/Buenos_Aires`, reloj del sistema)
  — el usuario autorizó la corrección documental mediante «lo autorizo» ante la propuesta
  explícita de NUMERIC sin escala fija y sincronización de artefactos/Tasks conservando avance.
  Diseño actualizado: `value`, `tolerance` y `difference` NUMERIC sin precisión/escala fijas;
  BigDecimal desde el token original, sin conversión binaria; solo derivadas a seis decimales
  HALF_UP. Se precisaron regresiones en Tasks existentes, sin implementarlas ni ejecutarlas.
  Estado conservado `Pendiente`: faltan Analyze, implementación y evidencia de tests; no se
  declara resuelta por esta edición. Autorización e historial en `sdd-gates.md` de SPEC-012.
- **Avance de implementación — 2026-10-16**: V10 y los mappings JPA usan `numeric` sin
  precisión/escala fijas. T032–T034 incorporan regresiones del rating exacto de 16 decimales,
  derivadas de seis decimales, metadatos del esquema, rollback y persistencia histórica.
  Las pruebas están escritas pero no ejecutadas por el agente. Se conserva `Pendiente`:
  faltan la extracción del token original en el recorrido funcional y la evidencia humana
  satisfactoria de ida/vuelta con PostgreSQL; el build no acredita estos resultados.
- **Avance del recorrido — 2026-10-16**: `WhoScoredStructuredDataParser` incorpora lectura
  decimal del HTML original, sin ejecución de JavaScript ni conversión binaria; su regresión
  conserva `6.9033333333333333` y ceros finales publicados. La extracción y el ensamblado
  están implementados; T036 y el cierre de US01 dependen de la infraestructura de US02
  por decisión humana de mantener el diseño. TD-019 continúa pendiente de ejecución y
  evidencia de los tests, no resuelta por existencia de código.

### TD-020 — Contraste entre canales retirado en SPEC-012

| Campo | Valor |
| --- | --- |
| **ID** | TD-020 |
| **Fecha y hora de registro** | 2026-10-18 |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | - |

**Descripción.** SPEC-012 exigía contrastar por métrica compartida la coincidencia, el redondeo
publicado o la discrepancia entre el scraping y la interfaz JSON (FR-044, FR-047, FR-048, SC-006 y
SC-013). El modelo entregado no lo implementa: `V20261018191335__flatten_player_statistics.sql` elimina la tabla
`player_metric_comparisons` y no existen `MetricComparisonRule`, `MetricComparisonRecorder` ni sus
tests. La alineación documental del 2026-10-18 retiró esos requisitos conservando sus identificadores
y desmarcando las tareas T061, T062, T069 y T070.

**Evidencia.**

- `backend/src/main/resources/db/migration/V20261018191335__flatten_player_statistics.sql:384` - `DROP TABLE
  player_metric_comparisons`; la secuencia `player_metric_comparisons_id_seq` queda huérfana.
- `backend/src/main/java/footballmarket/services/JsonStatisticsUpdateService.java:9` y
  `backend/src/main/java/footballmarket/services/impl/JsonStatisticsUpdateServiceImpl.java:28` -
  el propio código declara que no consulta ni contrasta observaciones de otros canales.
- `backend/src/main/java/footballmarket/models/records/PlayerStatisticsSyncCounters.java` - no
  expone contadores de coincidencia, redondeo ni discrepancia.
- `specs/012-player-statistics-scraping/tasks.md` - T061, T062, T069 y T070 estaban marcadas como
  completadas sin artefacto correspondiente en el repositorio.

**Causa técnica.** El diseño normalizado con observaciones y auditoría de contraste fue reemplazado
por una fila aplanada por campaña y canal sin que la decisión quedara registrada como gate ni como
tarea del workflow.

**Impacto técnico.** Una divergencia de valor entre ambos canales de la misma métrica pasa
desdetectada: solo se detecta comparando manualmente las dos filas de campaña del jugador. Se pierde
también la distinción entre una métrica ausente por el proveedor y una derivada no derivable, que
comparten columna nula.

**Solución propuesta.** Si el contraste se considera necesario, reinstate el requisito mediante una
spec que lo verifique y defina la forma de persistirlo sobre el modelo aplanado actual. Requiere
autorización humana; esta entrada no autoriza cambios de código.

**Criterios de resolución.** La deuda no se considera resuelta hasta que exista una decisión humana
explícita sobre el requisito y, si se mantiene, exista implementación y evidencia humana de tests
que distingan coincidencia, redondeo publicado y discrepancia por métrica compartida. No se declara
resuelta por la alineación documental ni por build.

**Referencias.**

- `specs/012-player-statistics-scraping/spec.md` (FR-044, FR-047, FR-048, SC-006, SC-013)
- `specs/012-player-statistics-scraping/data-model.md`
- `backend/src/main/resources/db/migration/V20261018191335__flatten_player_statistics.sql`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-18 | Registro inicial tras la alineación documental de SPEC-012 autorizada por el usuario. No modifica código ni migraciones. |

### TD-001 — Protección de los endpoints de monitoreo de Spring Boot Actuator

| Campo | Valor |
| --- | --- |
| **ID** | TD-001 |
| **Fecha y hora de registro** | 2026-10-14 16:29:48 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Seguridad |
| **Fecha de resolución** | — |

**Descripción.** Durante el desarrollo se decidió permitir temporalmente el acceso sin
autenticación a los endpoints de monitoreo de Spring Boot Actuator, para poder consultarlos desde
Postman durante el diagnóstico local. La configuración habilita `/actuator`, `/actuator/health`,
`/actuator/info` y `/actuator/metrics`.

La exposición por HTTP de las métricas es doblemente restrictiva: requiere la habilitación
explícita por entorno `MANAGEMENT_EXPOSED_ENDPOINTS=health,info,metrics`, que el repositorio no
define por defecto. Aun así, la autorización es abierta para las rutas de monitoreo.

**Motivo del aplazamiento.** La medida responde a una decisión consciente y temporal del
responsable del proyecto: el monitoreo es una herramienta de diagnóstico local y no está prevista
para ningún despliegue público. Implementar la protección antes de que exista un despliegue real
sumaría complejidad arquitectónica —autorización por roles o un puerto de gestión independiente—
que el proyecto todavía no necesita.

**Impacto técnico.** Una exposición accidental en producción podría revelar información operativa:
consumo de memoria de la JVM, uso de CPU del sistema y del proceso, número de hilos, tiempo de
actividad y métricas de las solicitudes HTTP servidas. `/actuator/info` quedaría accesible en todos
los perfiles; actualmente no expone datos porque no hay propiedades `management.info.*` declaradas,
pero esa condición depende de la configuración y no de un control de acceso. `/actuator/health` no
es parte del riesgo: `show-details` es `never` y solo informa el estado general.

**Solución propuesta.** Implementar controles de acceso adecuados para los endpoints de monitoreo.
Las alternativas a evaluar son:

- **Autorización administrativa.** Requiere un rol con autoridad real, que el sistema todavía no
  implementa: hoy `role` es informativo y no existe autorización por rol.
- **Restricciones de red.** Puerto de gestión independiente ligado a la loopback. Es la única
  variante que no depende del cliente, pero tiene un coste conocido: al compartir puerto los
  endpoints, `/actuator/health` pasaría también a quedar ligado a la loopback y dejaría de ser
  alcanzable desde fuera.
- **Separación del puerto de gestión.** Puede combinarse con la anterior y permite tratar la
  seguridad de ese contexto de forma independiente.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber implementado el control de acceso elegido para `/actuator/info` y `/actuator/metrics/**`.
2. Haber verificado el comportamiento efectivo de esa restricción en ejecución.
3. Mantener `/actuator/health` accesible sin información interna sensible.
4. No volver a exponer por error `env`, `heapdump`, `threaddump`, `configprops`, `beans` ni
   `shutdown`, cuya exposición está hoy vedada en todos los perfiles.

**Referencias.**

- `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- `backend/src/main/resources/application.yml`
- `docs/backend/technologies.md`
- `specs/trello.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 16:29:48 | Registro inicial en `docs/TECHNICAL_DEBT.md`. La anotación previa existía en `specs/trello.md` y en `docs/backend/technologies.md`; la fecha de creación original de esas anotaciones no se conoce, por lo que se registra la de incorporación a este documento. |
| 2026-10-14 16:52:29 | Actualización de la referencia al mecanismo de habilitación: `FOOTBALLMARKET_MONITORING_LOCAL_METRICS` se reemplaza por `MANAGEMENT_EXPOSED_ENDPOINTS=health,info,metrics`. La corrección del arranque no altera el alcance, la prioridad ni el estado de la deuda, que continúa `Pendiente`. Sin cambios funcionales: la exposición sigue siendo abierta para las rutas de monitoreo. |

### TD-002 — Autorización por rol inexistente, prometida en el contrato público

| Campo | Valor |
| --- | --- |
| **ID** | TD-002 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Seguridad |
| **Fecha de resolución** | — |

**Descripción.** El backend no implementa ninguna autorización por rol. El campo `role` de la
entidad `User` es informativo y nunca participa de una decisión de acceso. La superficie de
autorización completa es «autenticado o no autenticado», sin ninguna forma de distinguir entre un
usuario registrado y el superusuario.

El contrato público afirma lo contrario. La descripción OpenAPI de `GET /api/auth/me` declara que
«el backend evalúa toda autorización a partir del rol persistido», frase que no se corresponde con
la implementación.

**Evidencia.**

- `backend/src/main/java/footballmarket/config/SecurityConfig.java:86-105` — la superficie de
  autorización completa: `permitAll()` para `POST /api/auth/register` y `POST /api/auth/login`
  (`:88-89`), para Swagger y OpenAPI (`:90-91`) y para siete rutas de monitoreo (`:95-103`), y
  `.anyRequest().authenticated()` (`:104-105`). No hay ninguna otra regla.
- Búsqueda de `hasRole`, `hasAuthority`, `@PreAuthorize`, `@PostAuthorize`, `@Secured`,
  `EnableMethodSecurity`, `@RolesAllowed` y `ROLE_` en `backend/src/main/java`: **0 resultados**.
- `backend/src/main/java/footballmarket/security/JWTProvider.java:33-38` — el token se emite con
  `subject`, `issuedAt` y `expiration`. No incluye `scope`, `scp` ni `roles`.
- `backend/src/main/java/footballmarket/security/PersistedUserJwtAuthenticationConverter.java:28-35`
  — resuelve el usuario persistido para revalidar que la sesión siga existiendo y **descarta el
  objeto devuelto**. Las autoridades provienen del `JwtAuthenticationConverter` por defecto, cuyo
  `JwtGrantedAuthoritiesConverter` solo lee `scope` y `scp`, que nunca se emiten. La colección de
  autoridades de todo principal autenticado es, por tanto, siempre vacía.
- `backend/src/main/java/footballmarket/controllers/AuthenticationController.java:44-46` — el OpenAPI
  afirma que el backend evalúa toda autorización a partir del rol persistido.

**Motivo del origen.** No es un descuido. `specs/001-user-auth-registration/spec.md:192` declara
que no se agregan endpoints protegidos por rol ni reglas de autorización, y `role` queda
disponible para las funcionalidades posteriores del mercado. La ausencia es una decisión de alcance
consciente; lo que sí es un defecto es que el contrato público describa un control que no existe.

**Impacto técnico.** Dos consecuencias. La primera es de seguridad: los endpoints
administrativos del catálogo solo exigen `.authenticated()` (ver [TD-003](#td-003--sincronizaciones-del-catálogo-abiertas-a-cualquier-usuario-autenticado)),
de modo que cualquier usuario registrado puede ejecutarlos. La segunda es de contrato: un
consumidor del OpenAPI puede suponer que existe separación de privilegios entre `USER` y
`SUPERUSER` y construir sobre una garantía inexistente. Además, añadir `@PreAuthorize` sin
corregir primero el convertidor de autoridades no bastaría: la regla fallaría para todos los
usuarios porque la colección de autoridades está vacía.

**Solución propuesta.** Decidir el modelo de autorización. Las dos alternativas razonables son
emitir el rol como claim y configurar `JwtGrantedAuthoritiesConverter` con el prefijo `ROLE_`, o
construir un `Converter` propio, que ya dispone de punto de extensión natural en
`PersistedUserJwtAuthenticationConverter`. Después, activar la seguridad a nivel de método para
expresar las restricciones sobre rutas que la cadena de filtros no puede expresar. También
requiere corregir el texto del contrato en `AuthenticationController.java:44-46` para que deje de
prometer un control ausente.

La elección entre modelo es una decisión funcional y no puede tomarse por cuenta propia.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber decidido y documentado el modelo de autorización con su contrato.
2. Haber hecho que las autoridades derivadas de una sesión reflejen el rol persistido, verificado
   en ejecución.
3. Haber protegido con una regla de autorización explícita cada endpoint que no deba estar
   disponible para cualquier usuario autenticado.
4. Haber corregido la descripción OpenAPI de `GET /api/auth/me` para que refleje el modelo
   vigente.
5. Tener pruebas que comprueben el rechazo de un usuario sin el rol requerido.

**Relación con otras deudas.** [TD-001](#td-001--protección-de-los-endpoints-de-monitoreo-de-spring-boot-actuator)
identifica esta misma ausencia como el motivo por el que su variante de autorización
administrativa no es aplicable hoy. No es una duplicación: TD-001 registra la protección de las
rutas de monitoreo y cita esta ausencia como dependencia; esta entrada registra la ausencia por
sí misma. Resolver TD-002 es condición previa para resolver TD-001 por esa vía.

**Referencias.**

- `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- `backend/src/main/java/footballmarket/security/JWTProvider.java`
- `backend/src/main/java/footballmarket/security/PersistedUserJwtAuthenticationConverter.java`
- `backend/src/main/java/footballmarket/controllers/AuthenticationController.java`
- `backend/src/main/java/footballmarket/models/enums/UserRole.java`
- `specs/001-user-auth-registration/spec.md`
- `docs/backend/architecture.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La ausencia se identificó durante el desarrollo de SPEC-001 y se produjo el 2026-10-14, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-003 — Sincronizaciones del catálogo abiertas a cualquier usuario autenticado

| Campo | Valor |
| --- | --- |
| **ID** | TD-003 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Seguridad |
| **Fecha de resolución** | — |

**Descripción.** Los dos endpoints que disparan sincronizaciones completas contra proveedores
externos solo exigen estar autenticado. Cualquier cuenta registrada puede invocarlos
repetidamente. La creación de cuentas es pública y no tiene ningún límite, por lo que la cadena
de abuso es completa y accesible para un visitante anónimo.

**Evidencia.**

- `backend/src/main/java/footballmarket/controllers/PlayerController.java:39-53` —
  `POST /api/players/sync` no exige rol adicional; el método no recibe ningún parámetro ni
  comprueba la identidad del llamador.
- `backend/src/main/java/footballmarket/controllers/PlayerImageController.java:41-46` — el OpenAPI
  de `POST /api/players/images/sync` lo declara literalmente: «Ejecución síncrona sin body,
  disponible para cualquier usuario autenticado».
- `backend/src/main/java/footballmarket/config/SecurityConfig.java:88-89` — el registro es
  `permitAll`, de modo que obtener una cuenta no requiere ningún privilegio.
- `backend/src/main/java/footballmarket/config/FootballDataClientConfig.java:34` y
  `backend/src/main/java/footballmarket/config/TheSportsDbClientConfig.java:30` — las llamadas
  salientes se autentican con las claves de API del operador, que son las que se consumen.
- `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java:96-99`
  y `backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java:33` —
  las únicas barreras son un `AtomicBoolean` y un `synchronized`, ambas locales a una única JVM.
  Evitan concurrencia, no abuso secuencial.

**Impacto técnico.** Cada invocación consume cuota de las APIs de Football-Data.org y TheSportsDB
usando las claves del operador. Un atacante puede agotarla con cuentas gratuitas creadas
mediante el registro público, lo que degrada la sincronización legítima y genera coste para el
proyecto. Las barreras actuales no mitigan el escenario, que es secuencial por naturaleza. La
ausencia de separación de privilegios agrava el impacto: no existe ninguna forma de restringir la
operación a un perfil o rol administrativo mientras TD-002 siga abierto.

**Solución propuesta.** Decidir de forma explícita si estos endpoints son administrativos. Si lo
son, su protección depende de resolver [TD-002](#td-002--autorización-por-rol-inexistente-prometida-en-el-contrato-público).
Independientemente de esa decisión, corresponde evaluar un límite de frecuencia o una restricción
por perfil que evite que existan fuera de un entorno de diagnóstico local.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber decidido y documentado si estos endpoints son administrativos o de diagnóstico local.
2. Haber aplicado el control de acceso correspondiente a esa decisión.
3. Haber verificado en ejecución que un usuario sin el privilegio requerido no puede invocarlos.
4. No haber introducido una protección que dependa únicamente de estado en memoria de una JVM si
   el proyecto puede desplegarse en más de una instancia.

**Referencias.**

- `backend/src/main/java/footballmarket/controllers/PlayerController.java`
- `backend/src/main/java/footballmarket/controllers/PlayerImageController.java`
- `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- `backend/src/main/java/footballmarket/services/impl/PlayerImageSynchronizationServiceImpl.java`
- `backend/src/main/java/footballmarket/orchestrators/PlayerSynchronizationOrchestrator.java`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La exposición se introdujo al implementar SPEC-002 y SPEC-006 y no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-004 — Migración V5 reescrita tras su publicación rompe la validación de Flyway

| Campo | Valor |
| --- | --- |
| **ID** | TD-004 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** La migración `V5__create_team_league_domain.sql` fue modificada después de haber
sido publicada, en un commit que ya está integrated en `dev`. Flyway registra el checksum de cada
migración al aplicarla y lo valida en cada arranque posterior. Cualquier base de datos que haya
aplicado la versión original de V5 fallará al iniciar.

**Evidencia.**

- `git log --follow -- backend/src/main/resources/db/migration/V5__create_team_league_domain.sql`
  devuelve dos commits: `f7be382` (forma original) y `dd6fda3` (reescritura).
- La forma original, `git show f7be382:…/V5__create_team_league_domain.sql`, empieza con el
  comentario «Release 1: esquema aditivo. La retirada de textos requiere Release 2 y gate humano»,
  **no contiene** ninguna sentencia `DROP COLUMN` y declara `team_id` como
  `BIGINT REFERENCES teams(id)`, nullable.
- La forma actual, `backend/src/main/resources/db/migration/V5__create_team_league_domain.sql:3-4`,
  ejecuta `ALTER TABLE players DROP COLUMN IF EXISTS "team"` y `… "league"`.
- `dd6fda3` es ancestro de `HEAD` y de `origin/dev`. No es ancestro de `main`.
- `spring.flyway.validate-on-migrate` no está configurado en ningún archivo del backend, por lo que
  aplica el valor por defecto de Flyway, que es `true`. Tampoco existe `spring.flyway.repair` ni
  `ignoreMigrationPatterns` en ninguna parte del repositorio.

**Causa técnica.** Flyway prohíbe por diseño la edición de una migración ya aplicada. La
reescritura se hizo sin el mecanismo de reparación que la acompaña, y sin una migración nueva que
absorba el trabajo destructivo.

**Matiz sobre el alcance.** La Constitución §2.1 autoriza expresamente a destruir datos de
desarrollo cuando la operación simplifica la implementación, y la reescritura encaja en ese
marco. El problema que se registra aquí no es la decisión de reescribir, sino su ejecución sin el
procedimiento de recuperación correspondiente. El esquema resultante es correcto y la convergencia
de datos de V8 es adecuada; el defecto es la ausencia de un camino de recuperación para las bases
ya migradas.

**Impacto técnico.** Indisponibilidad de la aplicación en todo entorno cuya base de datos se haya
migrado antes de `dd6fda3`, con un error de checksum que no señala la causa real. Como el
comando pertenece al conjunto del equipo y ya está integrado en `dev`, es probable que existan
bases afectadas. La prueba automatizada existente no cubre el hueco: siembra la forma legacy con
SQL directo sobre una base ya migrada, por lo que nunca ejercita la ruta de validación de
checksum.

**Solución propuesta.** Documentar el procedimiento de recuperación de una sola vez para los
entornos afectados, o restituir V5 a su forma original y trasladar todo lo que la reescritura
introduce a una migración posterior. Añadir además una prueba que valide el checksum partiendo de
una base migrada con la versión previa.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber documentado el procedimiento aplicable a una base migrada con la versión original de V5.
2. Haber verificado que una base en ese estado arranca correctamente tras aplicar el
   procedimiento.
3. No volver a modificar una migración publicada sin una migración posterior que absorba el
   cambio.
4. Tener una prueba que cubra la ruta de validación de checksum.

**Referencias.**

- `backend/src/main/resources/db/migration/V5__create_team_league_domain.sql`
- `backend/src/main/resources/db/migration/V8__converge_team_league_domain.sql`
- `backend/src/test/java/footballmarket/repositories/TeamLeagueConvergenceMigrationTest.java`
- `.specify/memory/constitution.md` §2.1

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La reescritura se produjo en el commit `dd6fda3` al cerrar SPEC-007 y no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-005 — Perfil `dev` activo por defecto con credenciales de base de datos versionadas

| Campo | Valor |
| --- | --- |
| **ID** | TD-005 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Configuración |
| **Fecha de resolución** | — |

**Descripción.** El archivo de configuración base fija el perfil `dev` como activo, de modo que
ese perfil es el efectivo en cualquier entorno que no establezca `SPRING_PROFILES_ACTIVE`. Ese
perfil contiene la URL, el usuario y la contraseña de la base de datos de desarrollo versionados en
el repositorio. Además, la configuración de secreto JWT solo existe en los perfiles `dev` y `test`:
no hay ningún perfil de producción en el repositorio.

**Evidencia.**

- `backend/src/main/resources/application.yml:4-6` — `spring.profiles.active: dev` en el archivo
  base, que aplica a todos los perfiles.
- `backend/src/main/resources/application-dev.yml:3-5` — `url: jdbc:postgresql://localhost:5432/football_market_dev`,
  `username: postgres`, `password: root`.
- `compose.yml:7-11` — replica el mismo par de credenciales y publica el puerto `5432` al host.
- `backend/src/main/resources/application.yml:18-19` — `spring.jackson.time-zone` está en el
  archivo base; `jwt` solo aparece en `application-dev.yml:23-25` y en
  `backend/src/test/resources/application-test.yml:29-31`. Los únicos perfiles existentes en
  `backend/src/main/resources` son `application.yml`, `application-dev.yml` y
  `application.properties`. No hay perfil de producción ni de preproducción.

**Impacto técnico.** Dos efectos. El primero es que un despliegue que olvide fijar el perfil no
falla de forma ruidosa: arranca contra `localhost:5432` con `postgres/root`. El segundo es que
`postgres/root` es una credencial débil publicada en el repositorio, con el agravante de que el
puerto se publica al host en `compose.yml`. La ausencia de un perfil endurecido implica que el
repositorio no ofrece hoy ninguna ruta de configuración apta para un despliegue real.

Lo que sí está correcto y conviene preservar: los secretos que importan están externalizados sin
valor por defecto, de modo que la aplicación falla rápido si faltan; `backend/.env` está excluido
por `.gitignore` y no está versionado; y el enmascarado de credenciales en `toString()` está
implementado de forma consistente en las clases de propiedades.

**Solución propuesta.** Retirar el perfil activo del archivo base para que se fije únicamente por
entorno, eliminar la contraseña del archivo de configuración o sustituirla por una variable de
entorno, y documentar que `compose.yml` es de uso exclusivamente local.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. No haber un perfil activo declarado en la configuración base.
2. No existir credenciales de base de datos versionadas en el repositorio.
3. Haber verificado en ejecución que la aplicación falla de forma explícita cuando falta la
   configuración del perfil que le corresponde.
4. Haber documentado qué perfiles existen y cuál corresponde a cada entorno.

**Referencias.**

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `compose.yml`
- `backend/.gitignore`
- `.gitignore`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La configuración existe desde la implementación inicial del backend y no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-006 — Peso de los GIFs de la pantalla de acceso

| Campo | Valor |
| --- | --- |
| **ID** | TD-006 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Frontend / Rendimiento |
| **Fecha de resolución** | — |

**Descripción.** La pantalla de autenticación descarga los cinco GIFs animados de la pantalla de
bienvenida, por un total de unos 15,85 MiB, antes de que el usuario pueda escribir su correo. Los
cinco se importan de forma estática y se renderizan simultáneamente; los inactivos se ocultan con
opacidad, por lo que se descargan igualmente.

**Evidencia.**

- `frontend/src/features/auth/utils/footballVisuals.ts:1-13` — import estático de los cinco GIF.
- `frontend/src/features/auth/components/AuthVisualPanel/AuthVisualPanel.tsx:19-30` — renderiza
  los cinco a la vez.
- `frontend/src/features/auth/components/AuthVisualPanel/AuthVisualPanel.module.scss:29` y `:41` —
  los elementos inactivos usan `opacity: 0` sobre `position: absolute`, por lo que permanecen en
  el documento y se descargan.
- Tamaños verificados en el repositorio: `football-04.gif` 9,49 MiB, `football-02.gif` 3,66 MiB,
  `football-01.gif` 1,41 MiB, `football-03.gif` 0,78 MiB y `football-05.gif` 0,51 MiB; total
  16.611.104 bytes.
- Búsqueda de `React.lazy`, `lazy(`, `Suspense` e `import()` dinámico en `frontend/src`: **0
  resultados**.

**Contraste interno.** El mismo proyecto aplicó optimizaciones de carga de imagen en el catálogo
de jugadores, con carga diferida, decodificación asíncrona y cadena de repliegue de tres niveles.
La pantalla de autenticación es la excepción, no la regla.

**Impacto técnico.** Cada visita a la ruta de acceso paga la transferencia completa antes de poder
interactuar. En redes lentas o móviles esto se traduce en una espera larga y una tasa de abandono
precoz. El coste crece con cada recurso añadido a la pantalla.

**Solución propuesta.** Las alternativas van de menor a mayor esfuerzo: renderizar únicamente el
elemento activo y precargar el siguiente; convertir los recursos a un formato moderno y
redimensionarlos a las medidas del panel; o cargar el panel de forma diferida para que la ruta de
autenticación no incluya estos recursos en su camino crítico.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber medido el peso transferido en la ruta de acceso después del cambio.
2. No mantener recursos animados completos descargados de forma anticipada.
3. No haber introducido una regresión de la animación ni de su alternativa accesible cuando el
   movimiento esté reducido o desactivado.

**Referencias.**

- `frontend/src/features/auth/utils/footballVisuals.ts`
- `frontend/src/features/auth/components/AuthVisualPanel/AuthVisualPanel.tsx`
- `frontend/src/features/auth/components/AuthVisualPanel/AuthVisualPanel.module.scss`
- `frontend/src/features/auth/assets/`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. Los recursos se introdujeron con SPEC-004 y no fueron registrados en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-007 — Dos peticiones de sesión actual por navegación

| Campo | Valor |
| --- | --- |
| **ID** | TD-007 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Frontend / Rendimiento |
| **Fecha de resolución** | — |
| **Decisión pendiente** | Confirmar con el responsable del proyecto si la revalidación en cada cambio de ruta es una garantía que deba preservarse. |

**Descripción.** La clave de la consulta de sesión actual incluye un valor distinto según quién la
solicite. Como consecuencia, el gestor de consultas trata ambas como consultas sin relación y
emite dos peticiones al backend en cada navegación de una ruta privada. La configuración de
revalidación impede además que ninguna de las dos entradas de caché pueda reutilizarse.

**Evidencia.**

- `frontend/src/features/auth/constants/authQueryKeys.ts:3-4` — la clave es
  `["auth", "currentUser", revision, navigation]`, donde `navigation` es un parámetro supplied por
  el llamador.
- `frontend/src/app/providers/SessionLifecycle.tsx:24-28` — la invoca con `location.key`.
- `frontend/src/app/layouts/AppLayout/AppLayout.tsx:15` — la invoca con `location.pathname`.
- `frontend/src/features/auth/hooks/queries/useCurrentUserQuery.ts:16-18` — `staleTime: 0` y
  `refetchOnMount: "always"`.

**Causa técnica.** `location.key` y `location.pathname` no coinciden nunca, de modo que las dos
claves son distintas. La revalidación permanente hace que ninguna entrada pueda servir de caché.

**Impacto técnico.** Dos peticiones en lugar de una por navegación, más una por el servidor. El
costo individual es bajo porque la respuesta es ligera, pero es un desperdicio evitable y se
acumula con cada navegación.

**Salvedad importante sobre la solución.** La propia especificación de la funcionalidad describe
esta revalidación como deliberada: detecta una sesión vencida en cada cambio de ruta y cubre un
escenario declarado. Optimizarlo sin revisar esa garantía eliminaría una comprobación de sesión
que hoy existe. Cualquier corrección debe conservar la revalidación y eliminar únicamente la
duplicación, por ejemplo con una clave estable más revalidación en montaje y en foco de ventana.

**Referencias.**

- `frontend/src/features/auth/constants/authQueryKeys.ts`
- `frontend/src/features/auth/hooks/queries/useCurrentUserQuery.ts`
- `frontend/src/app/providers/SessionLifecycle.tsx`
- `frontend/src/app/layouts/AppLayout/AppLayout.tsx`
- `specs/004-frontend-user-auth/quickstart.md`
- `specs/009-player-detail/tasks.md` (observación L2, que describe esta misma situación)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La situación ya estaba anotada como observación L2 en `specs/009-player-detail/tasks.md` sin incorporarse a este registro, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-008 — Índice ausente en `pending_review_cases`

| Campo | Valor |
| --- | --- |
| **ID** | TD-008 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Rendimiento |
| **Fecha de resolución** | — |

**Descripción.** El repositorio consulta los casos de revisión pendientes por la terna del sujeto
externo. El índice disponible no cubre esa terna, por lo que la consulta se resuelve como un
recorrido sobre el índice parcial más un filtrado sobre el almacenamiento. Como la consulta se
ejecuta una vez por cada entidad reconciliada durante la sincronización del catálogo, el costo se
repite por cada elemento procesado.

**Evidencia.**

- `backend/src/main/java/footballmarket/repositories/PendingReviewCaseRepository.java:16-17` —
  `findBySubjectTypeAndSubjectProviderAndSubjectExternalId`, cuyo predicado es la terna
  `subject_type`, `subject_provider`, `subject_external_id`.
- `backend/src/main/resources/db/migration/V5__create_team_league_domain.sql:74` — el único
  índice sobre el sujeto es `idx_pending_review_cases_subject ON pending_review_cases(subject_type, subject_id)`.
  `subject_type` es la columna líder, por lo que PostgreSQL puede recorrerlo, pero debe filtrar
  `subject_provider` y `subject_external_id` desde el almacenamiento.
- `backend/src/main/resources/db/migration/V5__create_team_league_domain.sql:54` — el `CHECK`
  admite `PLAYER`, `TEAM` y `LEAGUE`, de modo que la tabla mezcla los tres tipos de sujeto.
- `backend/src/main/java/footballmarket/services/internal/PendingReviewRecorder.java` — la consulta
  se emite en cada reconciliación de jugador, equipo y liga.

**Causa técnica.** El índice se diseñó para recuperar casos por el identificador interno del
sujeto, mientras que la consulta de producción discrimina por la identidad externa del proveedor.

**Impacto técnico.** El costo por consulta crece con el número de casos registrados y se
multiplica por el número de entidades reconciliadas en cada sincronización. Con un catálogo
histórico amplio el comportamiento cuadrático deja de ser visible y pasa a ser el costo dominante
de la ejecución. No afecta la corrección del resultado: la consulta devuelve lo correcto.

**Solución propuesta.** Añadir un índice que cubra la terna de la consulta
`(subject_type, subject_provider, subject_external_id)`, en una migración nueva. Conviene
evaluar junto con él si el índice sobre el sujeto interno sigue teniendo consultas que lo
justifiquen.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber añadido el índice que cubre el predicado real de la consulta.
2. Haber verificado en ejecución el plan de ejecución de la consulta sobre un volumen
   representativo.
3. No haber introducido una desviación de esquema que invalide la validación de arranque.

**Referencias.**

- `backend/src/main/java/footballmarket/repositories/PendingReviewCaseRepository.java`
- `backend/src/main/java/footballmarket/services/internal/PendingReviewRecorder.java`
- `backend/src/main/resources/db/migration/V5__create_team_league_domain.sql`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El índice se creó al implementar SPEC-007 y la consulta fue ajustada en el mismo trabajo; la divergencia no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-009 — Carga completa de jugadores por cada equipo protegido

| Campo | Valor |
| --- | --- |
| **ID** | TD-009 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Rendimiento |
| **Fecha de resolución** | — |

**Descripción.** Para proteger a los jugadores asociados a un equipo, el servicio recorre la tabla
completa de jugadores. Ese recorrido se ejecuta una vez por cada equipo que requiere protección,
y a su vez se invoca desde un bucle sobre los equipos observados en la respuesta del proveedor.
El costo total crece por tanto con el producto entre el número de equipos y el número de jugadores,
todo ello dentro de la transacción que aplica la instantánea completa.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/internal/TeamResolution.java:125-134` —
  `private void protect(Team team, Set<Long> protectedTeams, Set<Long> protectedPlayers)` recorre
  `this.players.findAll()` en la línea 130 y filtra por equipo en la 131.
- `backend/src/main/java/footballmarket/services/internal/TeamResolution.java:83` y `:91` — el
  método se invoca desde un bucle sobre los equipos observados en cada candidato de jugador.
- `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java:76-83` — la
  aplicación completa del catálogo se ejecuta dentro de un bucle con reintentos acotado por el
  tamaño del catálogo.
- `backend/src/main/resources/application.properties:5` — `hibernate.jdbc.batch_size=0`, sin
  agrupación de escrituras.

**Causa técnica.** La protección necesita el conjunto de jugadores de un equipo concreto, pero se
obtiene materializando la tabla completa y filtrando en memoria, en lugar de consultando
directamente por equipo.

**Impacto técnico.** El costo de la sincronización crece de forma cuadrática con el tamaño del
catálogo. Como la operación se ejecuta dentro de la transacción que aplica la instantánea
completa, el efecto no es solo deCPU: se prolonga la duración de la transacción y, con ella, la
retención de bloqueos sobre `teams`, `leagues` y `pending_review_cases`. Con un catálogo de las
cinco competiciones configuradas el efecto ya es apreciable y crecerá con el volumen.

**Solución propuesta.** Obtener los jugadores protegidos mediante una consulta acotada por equipo,
de modo que el conjunto disponible esté disponible una sola vez por transacción, en lugar de
recorrerlo repetidamente.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. No existir un recorrido de la tabla completa de jugadores dentro de un bucle.
2. Haber medido la duración de la aplicación de la instantánea antes y después del cambio sobre un
   catálogo representativo.
3. Conservar exactamente el mismo comportamiento de protección de jugadores.

**Referencias.**

- `backend/src/main/java/footballmarket/services/internal/TeamResolution.java`
- `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java`
- `backend/src/main/resources/application.properties`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La lógica de protección se introdujo al implementar SPEC-007 y no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-010 — El historial de sincronización de imágenes ejecuta borrado en una lectura

| Campo | Valor |
| --- | --- |
| **ID** | TD-010 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** El listado del historial de ejecuciones aplica, antes de consultar, la purga por
retención configurada. La purga borrada filas, se ejecuta dentro de una transacción que no es de
solo lectura, y se apoya en una consulta por fecha de finalización que no tiene índice. El borrado
de los elementos asociados se realiza mediante un método derivado, que carga las entidades antes
de eliminarlas una a una en lugar de emitir una única sentencia.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/impl/PlayerImageAuditServiceImpl.java:39-44` —
  `getRuns` está anotado con `@Transactional` sin `readOnly` y llama a `purgeExpired()` en la
  línea 41. Los otros dos métodos de lectura de la misma clase sí usan
  `@Transactional(readOnly = true)` en las líneas 48 y 54.
- `backend/src/main/java/footballmarket/services/impl/PlayerImageAuditServiceImpl.java:64-74` —
  la purga recorre las ejecuciones anteriores al corte y, para cada una, borra sus elementos y
  luego la ejecución.
- `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunRepository.java:19` —
  `findByFinishedAtBefore` no tiene ningún índice que la respalde.
- `backend/src/main/resources/db/migration/V4__player_image_enrichment.sql:34-36` — los únicos
  índices de esa tabla son `(started_at DESC, id DESC)` y `(status)`. Ninguno cubre
  `finished_at`.
- `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunItemRepository.java:18` —
  `deleteByRunId` es un borrado derivado, que en Spring Data se implementa seleccionando las
  entidades y eliminándolas individualmente. No hay ninguna declaración `@Modifying` con consulta
  de borrado masivo en el repositorio.
- `backend/src/main/resources/application.yml:35` — `audit-retention-days` está vacío por defecto,
  por lo que la purga no se ejecuta en la configuración de referencia.

**Condición de activación.** El comportamiento descrito no se manifiesta con la configuración de
referencia: al no estar definida la retención, la purga retorna de inmediato. Se activa en cuanto
se define `THE_SPORTS_DB_AUDIT_RETENTION_DAYS`. Es un problema latente, no un defecto observable
hoy.

**Causa técnica.** La retención se resuelve de forma diferida y oportunista, en el momento de leer,
en lugar de en un momento de mantenimiento explícito. La consulta por fecha de finalización nunca
recibió el índice que su frecuencia de ejecución requiere.

**Impacto técnico.** Al activar la retención, cada consulta del historial recorre la tabla de
ejecuciones sin índice y abre una transacción de escritura sobre una petición de lectura. Con
muchas ejecuciones expiradas, el borrado derivado añade un número de viajes por elemento. El
problema escala con el volumen de ejecuciones retenidas, no con el de consultas.

**Solución propuesta.** Evaluar tres cambios independientes y aplicables por separado: mover la
purga a un momento de mantenimiento explícito, dotar con un índice la consulta por fecha de
finalización y sustituir el borrado derivado por una operación de borrado masivo.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. No ejecutarse escritura alguna al atender una lectura del historial, o hacerlo desde un punto
   explícito y verificable.
2. Tener un índice que cubra la consulta por fecha de finalización.
3. No ejecutarse el borrado de elementos mediante un recorrido entidad por entidad.
4. Haber verificado en ejecución el comportamiento con una retención configurada.

**Relación con futuras funcionalidades.** El enunciado del proyecto exige un registro inmutable de
las operaciones financieras. Esta purga demuestra un patrón de borrado de auditoría por
retención que convendría no replicar en un registro de ese tipo.

**Referencias.**

- `backend/src/main/java/footballmarket/services/impl/PlayerImageAuditServiceImpl.java`
- `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunRepository.java`
- `backend/src/main/java/footballmarket/repositories/PlayerImageSyncRunItemRepository.java`
- `backend/src/main/resources/db/migration/V4__player_image_enrichment.sql`
- `backend/src/main/resources/application.yml`
- `tp/enunciado.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La purga por retención se introdujo al implementar SPEC-006 y no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-011 — Cobertura incompleta del manejador global de excepciones

| Campo | Valor |
| --- | --- |
| **ID** | TD-011 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Contrato |
| **Fecha de resolución** | — |

**Descripción.** El manejador global de excepciones cubre las excepciones de dominio que el
proyecto decisionó exponer, pero no cubre dos situaciones. La primera es que no existe un manejador
de respaldo: los errores que produce el propio marco de trabajo, como un cuerpo JSON malformado,
un tipo de contenido no soportado o una ruta inexistente, se responden con la estructura de error
del marco y no con la del proyecto. La segunda es que tres excepciones propias del proyecto no
tienen manejador y terminan como error interno del servidor.

**Evidencia.**

- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` —
  contiene 15 manejadores `@ExceptionHandler`. No hay ninguna declaración
  `@ExceptionHandler(Exception.class)` ni una subclase de `ResponseEntityExceptionHandler`, lo
  que se verificó con 0 resultados.
- El contrato de error del proyecto es
  `backend/src/main/java/footballmarket/controllers/dtos/responses/ErrorResponseDTO.java`, que
  declara seis campos. Los errores del marco de trabajo no incluyen el campo `code`, de modo que
  un consumidor que lea ese campo obtiene vacío en esos casos.
- Excepciones propias sin manejador, verificado con 0 resultados cada una:
  `CurrentUserNotFoundException`, `InvalidPlayerException` e
  `InvalidPlayerSnapshotException`.
- `CurrentUserNotFoundException` es alcanzable desde
  `backend/src/main/java/footballmarket/controllers/AuthenticationController.java:54-57`. Existe un
  manejador para `UserNotFoundException` en la línea 159, pero es un tipo distinto y no lo cubre.
- `InvalidPlayerException` e `InvalidPlayerSnapshotException` se construyen en
  `backend/src/main/java/footballmarket/services/impl/FootballDataPlayerServiceImpl.java:67-69` y
  `:81-82`, fuera de cualquier captura.
- Tampoco hay manejador para `DataIntegrityViolationException`, ni para excepciones de bloqueo de
  filas.
- A partir de SPEC-016 se suma una excepción propia sin manejador:
  `backend/src/main/java/footballmarket/models/exceptions/InvalidQuoteException.java`, que se
  construye en `backend/src/main/java/footballmarket/models/PlayerQuote.java:114,117,156,162,166,175`,
  en `backend/src/main/java/footballmarket/models/QuoteExecution.java` y en
  `backend/src/main/java/footballmarket/models/QuoteAdjustment.java:32`. Hoy está contenida dentro de
  la corrida de recálculo, pero si escapara al controlador la respuesta 500 no usaría
  `ErrorResponseDTO` ni el código `INTERNAL_ERROR` del contrato de la API.

**Impacto técnico.** El impacto es de consistencia del contrato, no de exposición de información:
la configuración de errores del servidor no habilita la inclusión de trazas ni de mensajes, de modo
que ninguna de estas rutas filtra detalles internos. El efecto observable es que la API responde
con dos estructuras de error distintas según el origen del fallo, y que un cliente que dependa del
campo `code` recibe un valor vacío en errores del marco de trabajo. Las tres excepciones sin
mapeo convierten situaciones controlables en errores internos del servidor, lo que degrada el
diagnóstico y oculta la causa real.

**Solución propuesta.** Añadir los tres manejadores ausentes con el código de error que
corresponda a cada caso, y añadir una prueba dedicada al manejador global que cubra tanto las
excepciones propias como los errores del marco de trabajo. Las excepciones de integridad y de
bloqueo de filas, que hoy no aplican a ningún flujo existente, conviene cubrirlas cuando exista un
flujo que las pueda producir.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Ninguna excepción propia alcanzable desde un controlador termine como error interno del
   servidor.
2. Tener pruebas que cubran los quince manejadores existentes y los que se añadan.
3. Tener una prueba que verifique que los errores originados en el marco de trabajo respetan el
   contrato de error del proyecto, o una decisión documentada que justifique la diferencia.

**Referencias.**

- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`
- `backend/src/main/java/footballmarket/controllers/dtos/responses/ErrorResponseDTO.java`
- `backend/src/main/java/footballmarket/controllers/AuthenticationController.java`
- `backend/src/main/java/footballmarket/services/impl/FootballDataPlayerServiceImpl.java`
- `backend/src/main/java/footballmarket/models/exceptions/InvalidQuoteException.java`
- `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md` (CR-08)
- `docs/backend/architecture.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El manejador se introdujo con la primera funcionalidad del backend y las ausencias no fueron registradas en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |
| 2026-10-21 22:20:53 | Se agrega `InvalidQuoteException` a las excepciones propias sin manejador, detectada en el code review de SPEC-016. No se modificó código, migraciones ni contratos. |

### TD-012 — Integración continua sin verificación del frontend ni análisis de dependencias

| Campo | Valor |
| --- | --- |
| **ID** | TD-012 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Transversal / Infraestructura |
| **Fecha de resolución** | — |

**Descripción.** El flujo de integración continua ejecuta únicamente la construcción del backend.
No hay ninguna tarea que compile o verifique el frontend, pese a que el repositorio define los
comandos correspondientes y el hook local de confirmación previa sí los aplica. Tampoco existe
análisis de secretos ni de vulnerabilidades de dependencias, ni una puerta de cobertura mínima.

**Evidencia.**

- `.github/workflows/ci.yml:43-45` — el único paso de construcción es
  `./gradlew clean build` con `working-directory: backend`. La búsqueda del término `frontend` en el
  flujo devuelve **0 resultados**.
- `frontend/package.json:8` y `:10` — los comandos `build` y `biome:check` existen.
- `.githooks/pre-commit:33` — el hook local sí invoca Biome, de modo que la verificación existe
  pero depende de que cada integración la ejecute.
- Búsqueda de `gitleaks`, `trufflehog`, `dependency-check` y `npm audit` en `.github/workflows/`:
  **0 archivos**.
- `backend/build.gradle.kts:78-90` — la tarea de informe de cobertura está definida y se entrega al
  análisis estático, pero no hay ninguna tarea de verificación de cobertura ni reglas de umbral:
  la búsqueda de `jacocoTestCoverageVerification` y `violationRules` devuelve **0 resultados**.

**Lo que está correcto.** Las acciones del flujo están fijadas por identificador de commit en lugar
de por etiqueta, lo que es una buena práctica de aprovisionamiento. Merece preservarse.

**Impacto técnico.** El frontend llega a integración continua sin compilar ni analizar, de modo
que un error de compilación o de formato solo se detecta si quien integra ejecutó el hook. La
ausencia de análisis de secretos limita la detección preventiva de credenciales, y la ausencia de
análisis de dependencias deja sin detectar vulnerabilidades conocidas. La cobertura se produce y
se reporta, pero ninguna puerta falla por ella, de modo que no ejerce presión sobre la calidad.

**Solución propuesta.** Añadir al flujo una tarea que ejecute la verificación del frontend
apoyándose en los comandos ya definidos en `package.json`. Evaluar además la incorporación de
análisis de secretos y de dependencias, y una puerta de cobertura mínima, ambas con la
consideración de que el análisis estático queda a cargo del responsable del proyecto según la
Constitución §6.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Verificar en ejecución que el flujo falla ante un error de compilación del frontend.
2. Tener una decisión documentada sobre análisis de secretos y de dependencias.
3. Tener una decisión documentada sobre si la cobertura se convierte en puerta, y si lo es, con
   qué umbral.

**Referencias.**

- `.github/workflows/ci.yml`
- `.github/workflows/sonarqube.yml`
- `.githooks/pre-commit`
- `frontend/package.json`
- `backend/build.gradle.kts`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El flujo se creó al iniciar el backend y su alcance nunca se amplió al frontend, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-013 — Divergencia en la convención de zona horaria entre perfiles

| Campo | Valor |
| --- | --- |
| **ID** | TD-013 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** El proyecto declara la zona horaria en cuatro lugares y esos cuatro no coinciden.
El archivo base, que aplica a todos los perfiles, fija la zona de serialización JSON. El perfil de
desarrollo, que es el activo por defecto, fija la zona de la sesión de base de datos pero no la de
enlace de parámetros. El perfil de prueba fija la zona de enlace y no la de serialización. A esto se
suma que la marca de tiempo de la respuesta de error no lleva zona.

**Evidencia.**

- `backend/src/main/resources/application.yml:19` —
  `spring.jackson.time-zone: America/Argentina/Buenos_Aires`, en el archivo base.
- `backend/src/main/resources/application-dev.yml:9` — `TimeZone: UTC` para la sesión de base de
  datos. Búsqueda de `time_zone` en ese archivo: **0 resultados**.
- `backend/src/test/resources/application-test.yml` — declara `time_zone` y no declara
  `time-zone`, verificado con 0 resultados para ese segundo término.
- `backend/src/main/resources/application.yml:6` — el perfil activo por defecto es `dev`.
- `backend/src/main/java/footballmarket/controllers/dtos/responses/ErrorResponseDTO.java:3` —
  `LocalDateTime timestamp`.
- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java:114` —
  se produce con `LocalDateTime.now()`.

**Causa técnica.** Cada perfil declara lo que necesita sin que exista una convención única
declarada que rija el proyecto. `LocalDateTime` no representa un instante, sino un reloj de pared
en la zona por defecto de la JVM, de modo que la misma respuesta de error produce una hora
distinta según dónde se ejecute el proceso. El resto de la API sí usa `Instant`, que sí representa
un instante absoluto, por lo que la API mezcla dos convenciones temporales.

**Impacto técnico.** Hoy el impacto es acotado porque las marcas de tiempo de la API operativa son
instantes absolutos y las columnas de la base son marcas de tiempo con zona. El impacto real
aparece cuando una funcionalidad necesita razonar sobre fechas de negocio, como una cotización de
un jugador para una fecha dada: con una zona de visualización y una zona de sesión que difieren en
tres horas, el concepto de día vigente puede quedar desplazado. La marca de tiempo de error, sin
zona, es además ambigua para cualquier consumidor que intente correlacionar eventos.

**Solución propuesta.** Declarar una convención única de zona horaria para el proyecto y aplicarla
de forma explícita en cada perfil, incluida la zona de enlace de parámetros en el perfil de
desarrollo. Evaluar cambiar la marca de tiempo de la respuesta de error a un instante absoluto.
Definir, antes de implementar funcionalidades con fecha de negocio, en qué zona se cierra el día.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Existir una convención de zona horaria declarada para el proyecto.
2. Todos los perfiles declarar de forma explícita y coherente los valores que les aplican.
3. Haber definido y documentado la zona de negocio para las fechas con significado funcional.
4. Ninguna respuesta de la API mezclar dos convenciones temporales.

**Referencias.**

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/test/resources/application-test.yml`
- `backend/src/main/java/footballmarket/controllers/dtos/responses/ErrorResponseDTO.java`
- `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java`
- `tp/enunciado.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. Las declaraciones se acumularon al crear el backend y al activar los perfiles de desarrollo y prueba; la divergencia no fue registrada en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-014 — Contratos y documentación desalineados del código

| Campo | Valor |
| --- | --- |
| **ID** | TD-014 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Transversal / Documentación |
| **Fecha de resolución** | — |

**Descripción.** Varios documentos normativos y contratos de API describen un comportamiento que
el código no implementa. El caso más relevante es una contradicción interna de una misma
especificación: su documento de contrato y su documento de especificación describen contratos
distintos para el mismo endpoint. También hay contratos de funcionalidades anteriores que siguen
describiendo campos que fueron retirados, y documentos de proceso que apuntan a rutas que ya no
existen.

**Evidencia.**

- `specs/006-player-images/spec.md:47`, `:217` y `:251` — la especificación establece que el
  parámetro booleano `force` se elimina del contrato público y se reemplaza por `mode` con los
  valores `NORMAL`, `FORCE` y `RETRY_UNRESOLVED`, más los parámetros `limit`, `teamName` y
  `leagueName`, y que el indicador booleano deja de formar parte de la entidad de ejecución.
- `specs/006-player-images/contracts/api.md:17` y `:25` — el contrato de la misma funcionalidad
  documenta un parámetro booleano `force` opcional e incluye `"force": false` en el ejemplo de
  respuesta.
- `backend/src/main/java/footballmarket/controllers/PlayerImageController.java:64-65` — la
  implementación acepta únicamente `boolean force`. No existen `mode`, `limit`, `teamName` ni
  `leagueName`.
- `specs/002-player-catalog/contracts/api.md:22-23`, `:39` y `:45-46` — describen los campos
  `team` y `league` como texto.
- `specs/006-player-images/contracts/api.md:10` — ejemplo de respuesta con `"team"` y `"league"`.
- `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java:24-39` —
  la respuesta real expone `teamId`, `teamName`, `leagueId` y `leagueName`, sin `team` ni
  `league`.
- `specs/002-player-catalog/quickstart.md:38` — repite la lista de campos retirados.
- `specs/002-player-catalog/quickstart.md:9` — afirma que una lista de competiciones con ligas
  adicionales debe impedir el arranque, mientras que
  `backend/src/main/java/footballmarket/config/FootballDataProperties.java:15` declara lo
  contrario en su propia documentación.
- `specs/trello.md:548` — remite a un directorio de especificación provisional que ya no existe
  en el repositorio.
- `specs/009-player-detail/contracts/player-detail.md:107` — exige cobertura con una herramienta
  de documentación de API que no figura en `docs/backend/testing.md:76-85` ni en las dependencias
  del backend.
- Enlaces relativos rotos verificados: `docs/development/sdd-workflow.md:69`,
  `docs/backend/technologies.md:103`, `specs/001-user-auth-registration/tasks.md:157` y
  `specs/001-user-auth-registration/plan.md:182`. El segundo resuelve a una ruta de este mismo
  registro que no existe.

**Lo que está correcto.** Los tipos que el frontend declara para los contratos de autenticación y
de jugadores coinciden campo por campo con los registros del backend, incluidas las nulidades. Los
códigos de error que el frontend reconoce existen todos en el manejador global de excepciones. Las
reglas de validación de los formularios del frontend reproducen las del backend, incluido el patrón
del nombre de usuario. No hay ninguna divergencia de contrato entre frontend y backend en lo que
está implementado: las diferencias son de documentación, no de comportamiento.

**Impacto técnico.** La Constitución §4 exige actualizar en el mismo cambio la especificación, la
implementación, los contratos y la documentación. El incumplimiento no altera el comportamiento en
ejecución, pero vuelve poco fiable la documentación como fuente de verdad, expone a un consumidor
del contrato público a asumir un parámetro inexistente y lleva a un integrador a diagnosticar
contra una especificación que describe otra funcionalidad. La contradicción interna de la
especificación de imágenes es el caso más costoso, porque dos documentos de la misma funcionalidad
se contradicen dentro del mismo repositorio.

**Solución propuesta.** Reconciliar los contratos y las especificaciones con la implementación
vigente, decidiendo en cada caso si la corrección corresponde al documento o al código. Cerrar la
contradicción interna de la especificación de imágenes, que requiere una decisión explícita porque
el contrato y la especificación discrepan sobre cuál es el comportamiento deseado. Reparar los
cuatro enlaces rotos.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Cada contrato de API del repositorio coincidir con la implementación vigente.
2. No existir contradicción entre el documento de especificación y el documento de contrato de una
   misma funcionalidad.
3. No existir referencias a rutas de documentación que no existan.
4. Haber revisado los enlaces relativos de toda la documentación del proyecto.

**Referencias.**

- `specs/006-player-images/spec.md`
- `specs/006-player-images/contracts/api.md`
- `specs/002-player-catalog/contracts/api.md`
- `specs/002-player-catalog/quickstart.md`
- `specs/009-player-detail/contracts/player-detail.md`
- `specs/trello.md`
- `docs/backend/technologies.md`
- `docs/development/sdd-workflow.md`
- `docs/backend/testing.md`
- `backend/src/main/java/footballmarket/controllers/PlayerImageController.java`
- `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java`
- `.specify/memory/constitution.md` §4

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. Las divergencias se produjeron al retirar los textos heredados en SPEC-007 y al implementar la sincronización de imágenes en SPEC-006; no fueron registradas en su momento, por lo que la fecha registrada es la de su incorporación a este documento. |

### TD-015 — Lectura de detalle de jugador fuera de una transacción de lectura

| Campo | Valor |
| --- | --- |
| **ID** | TD-015 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Baja |
| **Área afectada** | Backend / Datos |
| **Fecha de resolución** | — |

**Descripción.** El método que recupera un jugador por identificador no está anotado como
transacción de solo lectura, a diferencia del método que lista jugadores de la misma clase. El
mapeo a la respuesta se realiza en el controlador, fuera de cualquier transacción. El resultado es
correcto únicamente porque la consulta que alimenta el método carga de forma anticipada las
relaciones que el mapeo necesita. Esa dependencia implícita no es evidente al leer el código.

**Evidencia.**

- `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java:114` —
  `getVisiblePlayerById` no tiene anotación transaccional.
- `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java:107` —
  `getActivePlayers` sí está anotado con `@Transactional(readOnly = true)`.
- `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java:23-36` — el mapeo
  accede a `player.getTeam()` y a `player.getLeague()`, y se ejecuta desde el controlador, fuera de
  la transacción.
- `backend/src/main/java/footballmarket/repositories/PlayerRepository.java:30` y `:34` —
  `findVisibleById` declara `@EntityGraph(attributePaths = {"team", "team.league"})`, que es lo que
  hace que el mapeo funcione sin inicialización diferida.
- `backend/src/main/resources/application.properties:4` — la vista abierta está desactivada, de
  modo que no hay una sesión que mantenga las relaciones disponibles fuera de la transacción.

**Causa técnica.** El método depende de una garantía aportada por el repositorio y no por la
frontera transaccional del servicio. La dependencia no está declarada ni documentada, de modo que
cualquier cambio que agregue una relación al mapeo, o que modifique el grafo de la consulta, puede
introducir un fallo de inicialización diferida que no se manifiesta hasta la ejecución.

**Impacto técnico.** No hay impacto observable en el comportamiento actual. El riesgo es de
fragilidad futura y se manifiesta como un fallo en tiempo de ejecución en un cambio posterior.

**Solución propuesta.** Declarar la frontera transaccional de lectura en el método, alineándola con
el método de listado de la misma clase, o bien documentar explícitamente en el mapeo la dependencia
que tiene sobre el grafo de la consulta.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. El método tener una frontera transaccional de lectura declarada, o la dependencia sobre el
   grafo de la consulta estar documentada donde se cumple.
2. Conservar exactamente el mismo comportamiento observable.

**Referencias.**

- `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java`
- `backend/src/main/java/footballmarket/repositories/PlayerRepository.java`
- `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java`
- `specs/009-player-detail/tasks.md` (observación L6, que describe esta misma situación)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El método se introdujo al implementar el detalle de jugador en SPEC-009 y la observación ya existía en las tareas de esa funcionalidad sin incorporarse a este registro; la fecha registrada es la de su incorporación a este documento. |

### TD-016 — Artefacto de compilación incremental versionado en Git

| Campo | Valor |
| --- | --- |
| **ID** | TD-016 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Baja |
| **Área afectada** | Frontend / Configuración |
| **Fecha de resolución** | — |

**Descripción.** El archivo de estado de compilación incremental del frontend está versionado en
Git pese a estar excluido por el archivo de exclusiones del propio frontend y pese a que las
instrucciones del área lo declaran una salida generada.

**Evidencia.**

- `git ls-files frontend` devuelve `frontend/tsconfig.tsbuildinfo`, es decir, el archivo está
  versionado.
- `frontend/.gitignore:6` — `*.tsbuildinfo`. La exclusión está declarada y no surte efecto
  justamente porque el archivo ya está seguido.
- `frontend/AGENTS.md:13` — declara el patrón como salida generada que no debe editarse.

**Causa técnica.** El archivo se añadió al repositorio antes de que existiera la regla de
exclusión, o sin que la regla se aplicara al añadirlo.

**Impacto técnico.** El archivo cambia en cada compilación, de modo que introduce ruido en los
diffs y en el historial sin aportar información. Su contenido es específico del entorno local que
lo generó, por lo que versionarlo no aporta valor reproducible para otras máquinas.

**Solución propuesta.** Dejar de seguir el archivo en el repositorio, manteniendo la exclusión
declarada.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. El archivo no estar versionado.
2. La exclusión del patrón permanecer declarada.
3. No quedar ninguna otra salida generada versionada.

**Referencias.**

- `frontend/tsconfig.tsbuildinfo`
- `frontend/.gitignore`
- `frontend/AGENTS.md`
- `specs/009-player-detail/tasks.md` (que ya identificó el archivo)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El archivo fue identificado al revisar el detalle de jugador en SPEC-009, donde ya se dejó anotado sin incorporarse a este registro; la fecha registrada es la de su incorporación a este documento. |

### TD-017 — Conflicto de formato entre el importador de escudos y Biome

| Campo | Valor |
| --- | --- |
| **ID** | TD-017 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Baja |
| **Área afectada** | Frontend / Herramientas |
| **Fecha de resolución** | — |

**Descripción.** La herramienta que genera los archivos de registro de escudos escribe JSON con dos
espacios de indentación, mientras que la configuración del formateador exige tabulaciones. Cada
ejecución del importador reintroduce la discrepancia, de modo que la verificación de formato falla
de forma reproducible tras regenerar esos archivos.

**Evidencia.**

- `frontend/scripts/import-team-logos.mjs:109` — escribe los archivos con
  `JSON.stringify(value, null, 2)`.
- `frontend/biome.jsonc:29` — `"indentStyle": "tab"`.
- `frontend/package.json:10` — el comando de verificación de formato está definido y se invoca
  desde el hook local de confirmación previa, en `.githooks/pre-commit:33`.

**Causa técnica.** El generador y el formateador no comparten una convención de indentación y
ninguno de los dos cede.

**Impacto técnico.** La verificación de formato falla de forma sistemática después de cada
regeneración, lo que obliga a reformatear archivos generados y reintroduce un ciclo de escritura
que no aporta valor. También genera falsos positivos que dificultan distinguir un problema real de
formato de este efecto conocido.

**Solución propuesta.** Alinear el generador con la convención del formateador, o declarar en la
configuración del formateador una exclusión acotada para los archivos que el importador produce, si
se considera que su formato es legítimo.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Regenerar los archivos del importador y que la verificación de formato pase sin reformateo
   manual.
2. No quedar ningún archivo generado en conflicto con la configuración del formateador.

**Referencias.**

- `frontend/scripts/import-team-logos.mjs`
- `frontend/biome.jsonc`
- `frontend/package.json`
- `.githooks/pre-commit`
- `specs/009-player-detail/tasks.md` (que ya identificó el conflicto)

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. El conflicto fue identificado al trabajar los recursos gráficos del catálogo en SPEC-009 y ya estaba anotado sin incorporarse a este registro; la fecha registrada es la de su incorporación a este documento. |

### TD-018 — Colores literales en los módulos de estilo de componentes

| Campo | Valor |
| --- | --- |
| **ID** | TD-018 |
| **Fecha y hora de registro** | 2026-10-14 17:48:44 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Baja |
| **Área afectada** | Frontend / Convenciones |
| **Fecha de resolución** | — |

**Descripción.** La documentación de arquitectura del frontend declara que los componentes no
deben escribir colores literales y que deben usar los tokens del proyecto. Los módulos de estilo
incumplen esa regla de forma generalizada. La propia documentación reconoce la divergencia como
pendiente de corrección y afirma que está registrada en la sección de inconsistencias de una
especificación que ya no contiene esa sección, de modo que la deuda perdió su ancla de trazabilidad.

**Evidencia.**

- `docs/frontend/architecture.md:163` — declara los colores literales como «una divergencia
  pendiente de corrección» y afirma que está registrada en una sección de
  `specs/005-players-page/spec.md`. La búsqueda del término «inconsistencias» en ese archivo de
  especificación devuelve **0 resultados**, de modo que el ancla citada no existe.
- `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss` — 53 colores
  literales, verificado por recuento.
- Otros módulos con literales: `PlayersPage.module.scss` (7),
  `PlayerQuoteEvolutionChart.module.scss` (7), `AuthVisualPanel.module.scss` (6),
  `PlayerDetailPage.module.scss` (5), y `PlayerSportsSummary.module.scss` y
  `PlayerQuotePanel.module.scss` (2 cada uno).
- `frontend/src/styles/abstracts/tokens/_colors.scss` — los tokens existentes documentan su razón
  de contraste y señalan explícitamente los usos que no cumplen, de modo que la fuente de verdad
  para el color está bien establecida y es completa.

**Causa técnica.** Los estilos de componentes se escribieron aplicando valores directamente en
lugar de derivarlos de los tokens. La divergencia se agravó porque el seguimiento documental que
la registraba se perdió.

**Impacto técnico.** No hay efecto observable en el comportamiento ni en la accesibilidad. El
impacto es de mantenibilidad: un cambio de tema exige editar cada módulo en lugar de modificar un
token, y la documentación de contraste existente deja de reflejar lo que el código realmente usa.

**Solución propuesta.** Sustituir los colores literales por los tokens definidos, empezando por el
módulo con mayor cantidad de literales. Restablecer o corregir el ancla de trazabilidad en la
documentación de arquitectura para que la deuda vuelva a tener seguimiento.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Ningún módulo de estilo de componente declarar colores literales.
2. El contraste resultante ser equivalente al actual, sin pérdidas de accesibilidad.
3. La documentación de arquitectura citar un ancla de trazabilidad que exista.

**Referencias.**

- [Artículo 36.º — Capas globales de estilos](frontend/architecture/presentation.md#articulo-36) y [antecedente de implementación](frontend/architecture/implementation.md#estilos-de-la-implementacion).
- `frontend/src/styles/abstracts/tokens/_colors.scss`
- `frontend/src/features/players/components/PlayerCard/PlayerCard.module.scss`
- `frontend/src/features/players/pages/PlayersPage/PlayersPage.module.scss`
- `frontend/src/features/players/components/PlayerQuoteEvolutionChart/PlayerQuoteEvolutionChart.module.scss`
- `frontend/src/features/auth/components/AuthVisualPanel/AuthVisualPanel.module.scss`
- `specs/005-players-page/spec.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 17:48:44 | Registro inicial en `docs/TECHNICAL_DEBT.md`, tras auditoría técnica de solo lectura del 2026-10-14. La divergencia ya estaba declarada en la documentación de arquitectura con un ancla que ya no existe, por lo que la fecha registrada es la de su incorporación a este documento. |

## 7. Historial de actualizaciones del registro

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 16:29:48 | Creación del registro centralizado con TD-001. Se consolidaron como entrada las referencias dispersas que existían en `specs/trello.md` y `docs/backend/technologies.md`, conservando la traza hacia ellas. |
| 2026-10-14 17:48:44 | Incorporación de TD-002 a TD-018 tras auditoría técnica integral de solo lectura del 2026-10-14. Cuatro entradas de prioridad Alta (TD-002 a TD-005), nueve de prioridad Media (TD-006 a TD-014) y cuatro de prioridad Baja (TD-015 a TD-018). TD-001 no se modificó: su contenido se verificó contra el código y sigue siendo exacto. Se incorporaron además tres deudas ya declaradas en `specs/009-player-detail/tasks.md` que nunca se habían centralizado (TD-015, TD-016, TD-017) y una divergencia declarada en `docs/frontend/architecture.md:163` cuya ancla de trazabilidad ya no existía (TD-018). No se registraron los hallazgos descartados por no cumplir los criterios del §2 ni los clasificados como funcionalidad pendiente o mejora opcional. Ninguna entrada modifica código, migraciones, contratos ni otra documentación. |
| 2026-10-18 | Incorporación de TD-020 tras la alineación documental de SPEC-012 autorizada por el usuario, que retiró el contraste entre canales, fijó la cobertura efectiva 45/57/42 y recorrió quince combinaciones JSON. No se modificó código, migraciones ni contratos; la entrada requiere decisión humana y evidencia de tests para cerrarse. |
| 2026-10-21 16:20:55 | Incorporación de TD-021 tras la revisión funcional de SPEC-016, en la que se verificó que la capa de valuación reutiliza en silencio la campaña de métricas anterior cuando la campaña vigente no está confirmada y sella esas valuaciones con el día de la evaluación, con lo que el recálculo reprecia a todos los jugadores sobre datos que no cambiaron. El responsable del proyecto decidió no modificar el código; la entrada registra el comportamiento real y su regla de resolución propuesta. No se modificó código, migraciones ni contratos. |
| 2026-10-21 22:20:53 | Incorporación de TD-022 a TD-029 tras el code review de SPEC-016: una deuda de prioridad Alta (TD-022, ranking sin desempate determinista) y siete de prioridad Media (TD-023 a TD-029). TD-011 se actualizó con `InvalidQuoteException` entre las excepciones propias sin manejador global. El code review completo, con los hallazgos, el orden de revisión y el checklist humano, quedó registrado en `specs/016-ejecutar-recalculo-manual-de-cotizaciones/code-review-report.md`. No se modificó código, migraciones ni contratos. |
| 2026-10-21 23:05:00 | Remediación del code review de SPEC-016 por decisión del responsable del proyecto: se cierra el desempate no determinista de la lectura de cotizaciones (CR-01, TD-022, T051 y T052) y se registran las decisiones sobre CR-02 (TD-023 se conserva como deuda), CR-05 (TD-026 descartada) y CR-07 (TD-028 descartada). TD-022 pasa a `En curso` porque la ejecución de los tests corresponde al usuario. Se modificó el repositorio de cotizaciones y la suite de pruebas de consultas; no se modificaron migraciones, contratos HTTP ni el esquema. |

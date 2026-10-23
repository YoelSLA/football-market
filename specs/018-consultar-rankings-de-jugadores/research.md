# Research: Consultar ranking de jugadores por score

**Feature**: `018-consultar-rankings-de-jugadores`
**Spec**: [`spec.md`](spec.md)
**Plan**: [`plan.md`](plan.md)
**Fecha**: 2026-10-23

Este documento resuelve las decisiones técnicas de la fase de planificación. No introduce
requisitos: su autoridad es subordinada a la Constitución y a `spec.md`. No quedan marcadores
`NEEDS CLARIFICATION` en el [Technical Context](plan.md#technical-context).

---

## 1. Superficie HTTP

**Decision**: Una única ruta de solo lectura `GET /api/players/ranking/score`, sin query parameters
declarados ni body. Los query parameters no declarados se ignoran y no alteran la estrategia, la
población, el orden ni el contenido. El ranking por precio queda fuera del alcance.

**Rationale**: FR-001 prohíbe declarar o utilizar parámetros de estrategia y filtros adicionales.
Ignorar parámetros no declarados coincide con el comportamiento estándar de Spring MVC sin convertirlos
en parte del contrato. Una ruta propia evita reinterpretar el recurso existente o ampliarlo con un
discriminador. `/api/` es obligatorio por la arquitectura del backend §1.7. La
coexistencia con `GET /api/players/{id}` y `GET /api/players/{id}/quotes` no genera ambigüedad: Spring
resuelve el segmento literal antes que la variable de path.

**Alternatives considered**:
- *`GET /api/players/ranking?by=score`*: rechazada; convertiría el criterio en un parámetro contractual
  y acoplaría el recurso nuevo al ranking existente.
- *`GET /api/rankings/score`*: rechazada; separa artificialmente un recurso del resto de las consultas
  de jugadores y no aporta ninguna ventaja.
- *Reutilizar `GET /api/players/ranking` devolviendo ambos rankings*: rechazada; cambia la forma de la
  respuesta existente y duplica datos no solicitados.

## 2. Última ejecución de cotización persistida

**Decision**: El encabezado se obtiene de la fila de `quote_executions` con mayor `quote_date`
(`findTopByOrderByQuoteDateDesc()`). De ella se toman `strategy_name` como `strategy` y `quote_date`
como `quoteDate`. La versión de la estrategia no se lee para la respuesta (FR-009) ni se usa para
filtrar (FR-002). La ejecución determina únicamente la estrategia y el encabezado; no identifica la
valuación concreta utilizada para calcular las cotizaciones.

**Rationale**: FR-002 y el supuesto de la spec garantizan una única ejecución por fecha de cotización
(`uq_quote_executions_date`), por lo que la última ejecución es inequívoca y no requiere desempate por
`started_at`. `quote_executions` es la fuente persistida de la estrategia efectivamente aplicada por
la corrida; derivarla de las filas de cotización sería imposible para jugadores arrastrados sin
estrategia (fila `INITIAL`).

**Alternatives considered**:
- *Deducir la estrategia de `player_quotes.strategy_name`*: rechazada; las filas arrastradas pueden no
  tener estrategia y varias filas vigentes pueden provenir de estrategias distintas.
- *Usar `started_at` como criterio de última ejecución*: innecesario; la unicidad por fecha ya
  determina la última corrida y `started_at` no cambia el resultado.
- *Recibir la estrategia por parámetro*: prohibido por FR-001.

## 3. Población elegible

**Decision**: Es el conjunto de jugadores que cumplen simultáneamente:
1. tienen al menos una fila en `player_quotes` (por lo tanto, una cotización vigente), y
2. tienen al menos una valuación de la estrategia de la última ejecución.

La condición (1) se expresa con un `exists` sobre `PlayerQuote`; la condición (2) queda implícita al
consultar la tabla de valuaciones de esa estrategia.

**Rationale**: Es la formulación literal de FR-003 y de la entidad «Cotización vigente» de la spec.
La población no se limita a los jugadores repreciados en la última ejecución ni a los cotizables con
métricas: un jugador puede conservar una cotización vigente anterior y seguir siendo elegible si tiene
valuación de la estrategia. Exigir la fecha de cotización de la última ejecución añadiría una
restricción que la spec no define.

**Alternatives considered**:
- *Población cotizable de `QuotePopulationSelector`*: rechazada; depende de métricas, criterio que
  FR-003 no usa, y excluiría jugadores ya cotizados que conservan cotización vigente.
- *Jugadores con fila de cotización en la fecha de la última ejecución*: rechazada; excluye jugadores
  con cotización vigente anterior y añade un requisito no especificado.
- *Jugadores con valuación, aunque no tengan cotización*: rechazada; contradice FR-003 y el caso límite
  «Jugador sin cotización vigente».

## 4. Selección de la valuación y del score

**Decision**: Para cada estrategia se consulta su tabla de valuaciones
(`player_positional_valuations` para `POSITIONAL_PERFORMANCE`, `player_attribute_valuations` para
`GENERAL_ATTRIBUTES`). De cada jugador se elige la fila con mayor `valuation_date`. La unicidad por
jugador, fecha, estrategia y versión impide que existan dos candidatas para la misma fecha; una nueva
evaluación de esa fecha reemplaza la fila anterior. El score es `final_score` para la estrategia posicional y
`final_grl` para la de atributos. El score bruto (`raw_score`, `raw_grl`) no se usa. La fecha expuesta
es la `valuation_date` de la fila elegida (FR-004, FR-012). La selección no se limita por
`quote_date`: una valuación puede ser posterior o distinta de la usada por la última cotización.

**Rationale**: `final_score` y `final_grl` son los resultados finales persistidos por SPEC-013, ya
normalizados y acotados al construir la entidad. La restricción única del esquema hace inequívoca la
fila de cada fecha. La versión de la estrategia no interviene: cada tabla impone por `CHECK` una única
estrategia y versión (`v1`), y la spec excluye la versión de la respuesta y del filtrado. El flujo
operativo habitual métricas → valuaciones → cotizaciones se ejecuta semanalmente, pero puede observarse
entre etapas o después de un fallo parcial; por eso `valuationDate`, y no `quoteDate`, identifica la
fecha del score mostrado.

**Alternatives considered**:
- *Usar `raw_score`/`raw_grl`*: rechazada; FR-004 exige el resultado final.
- *Recalcular el score desde las métricas*: rechazada; FR-012 prohíbe transformaciones y recálculos.
- *Unificar ambas tablas con una vista o `union`*: rechazada; introduce una estructura nueva sin
  necesidad y complica el tipado, cuando la estrategia ya determina la tabla.

## 5. Orden, desempate y posiciones

**Decision**: El orden se resuelve en la consulta JPQL: `score DESC, player.id ASC`. El Service
preserva ese orden y asigna la posición como índice + 1 al construir cada `ScoreRankingEntry`. El
Mapper copia la posición al DTO sin calcularla ni volver a ordenar.

**Rationale**: FR-006 y FR-007 exigen un orden total y posiciones consecutivas sin compartirse.
Fijar el orden en una única fuente evita duplicar el criterio y garantiza que dos consultas con los
mismos datos devuelvan la misma respuesta (US2). El Service materializa la posición como parte del
resultado del caso de uso, mientras que el Mapper permanece estructural conforme a la arquitectura.
El desempate por `playerId` ascendente ya es determinista por sí solo; no se necesita otro criterio.

**Alternatives considered**:
- *Ordenar en el Service con `Comparator`*: rechazada; duplica el criterio de la consulta y crea dos
  fuentes de verdad para el mismo orden.
- *Calcular la posición en el Mapper*: rechazada; introduciría un cálculo semántico en una
  transformación que debe ser estructural.
- *Posiciones compartidas para empates*: rechazada por FR-007.

## 6. Descomposición en roles

**Decision**: Se añade un Service `ScoreRankingQueryService` con interfaz en
`footballmarket.services` e implementación en `footballmarket.services.impl`, un Controller
`ScoreRankingController`, un Mapper `ScoreRankingMapper` y dos records de Model
(`ScoreRanking`, `ScoreRankingEntry`). No se crea Orchestrator.

**Rationale**: La consulta combina el resumen de ejecuciones, el historial de cotizaciones y las
valuaciones; es un caso de uso de lectura independiente del de `QuoteQueryService`. Un Service propio
respeta la responsabilidad única y evita que `QuoteQueryServiceImpl` dependa de las tablas de
valuaciones. No hay coordinación entre Services que justifique un Orchestrator. El Mapper transforma
Model a DTO sin negocio, conforme a la arquitectura §1.2.

**Alternatives considered**:
- *Ampliar `QuoteQueryService`*: rechazada; mezcla la consulta de cotizaciones con la de valuaciones y
  obliga a modificar la implementación del ranking por precio, que debe permanecer intacta.
- *Añadir la ruta a `PlayerQuoteController`*: rechazada; haría que el Controller de cotizaciones
  dependiera de un caso de uso de valuaciones y cambiaría su Tag y su descripción OpenAPI.
- *Orchestrator*: rechazado; no coordina Services, sino lecturas de Repositories dentro de un mismo
  caso de uso.

## 7. Forma del contrato y tipos

**Decision**:

```json
{
  "strategy": "POSITIONAL_PERFORMANCE",
  "quoteDate": "2026-10-23",
  "ranking": [
    {
      "position": 1,
      "playerId": 7821,
      "score": 0.913400,
      "valuationDate": "2026-10-21"
    }
  ]
}
```

- `strategy`: nombre del enum (`String` en el DTO, `ValuationStrategy` en Model), nulo si no hay
  ejecuciones.
- `quoteDate`: `LocalDate`, nulo si no hay ejecuciones.
- `ranking`: siempre presente, vacío si no hay entradas.
- `score`: `BigDecimal` serializado como número, con la escala persistida (seis decimales), sin
  redondeo (FR-012).
- `valuationDate`: `LocalDate` de la valuación usada.
- La entrada no incluye nombre ni otros datos del jugador (FR-008).

**Rationale**: Es la forma mínima que satisface FR-008, FR-009, FR-010 y FR-011. Mantener el enum en
Model y `String` en DTO replica el patrón vigente de `QuoteResponseDTO`. Devolver un objeto con campos
nulos —y no `null` como respuesta— respeta la regla de arquitectura §1.1 y permite distinguir «sin
ejecuciones» de «error».

**Alternatives considered**:
- *Omitir `strategy` y `quoteDate` cuando son nulos*: rechazada; FR-010 exige devolverlos nulos, no
  ausentes, y la forma del objeto debe ser estable.
- *Incluir `strategyVersion`*: rechazada explícitamente por FR-009.
- *Devolver `score` como `String`*: rechazada; el valor es numérico y el resto del contrato usa números.

## 8. Consistencia transaccional

**Decision**: El Service se ejecuta con `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`.
La lectura de la última ejecución y la de las valuaciones comparten la misma instantánea.

**Rationale**: El encabezado y las entradas deben describir la misma ejecución. Con el aislamiento por
defecto, una ejecución de cotización concurrente podría confirmarse entre ambas lecturas y producir un
encabezado de una corrida con datos de otra. `REPEATABLE_READ` de solo lectura es el patrón ya usado
por las consultas de portfolio (arquitectura del backend §5 y §6) y no añade escrituras.

**Alternatives considered**:
- *Aislamiento por defecto (`READ_COMMITTED`)*: rechazado; permite mezclar dos ejecuciones.
- *Una sola consulta con `join` entre ejecución y valuaciones*: rechazada; el encabezado debe existir
  aunque no haya valuaciones, y la consulta quedaría artificialmente acoplada a dos tablas de
  valuaciones distintas según la estrategia.

## 9. Manejo de errores

**Decision**: No se añaden códigos de error nuevos. Un fallo técnico de persistencia se traduce por el
manejador vigente en `500 INTERNAL_ERROR`, sin devolver una clasificación parcial. La ausencia de
ejecuciones o de valuaciones no es un error: produce la respuesta vacía de FR-010/FR-011.

**Rationale**: FR-014 exige comunicar el error sin inventar datos ni presentar un resultado parcial.
La operación es una lectura única dentro de una transacción; si falla, no hay respuesta parcial que
ocultar. El proyecto ya traduce `DataAccessException` y `TransactionException` a `INTERNAL_ERROR`
(`GlobalExceptionHandler`), y no se requiere un código propio para diferenciar causas que el cliente
no puede resolver.

**Alternatives considered**:
- *Código nuevo `SCORE_RANKING_UNAVAILABLE`*: rechazado; no aporta información accionable y amplía el
  catálogo sin requisito.
- *Devolver una lista vacía ante un fallo*: rechazado; confundiría ausencia de datos con un error
  (FR-014).

## 10. Persistencia, índices y migraciones

**Decision**: No se crean tablas, columnas, índices ni migraciones. Se reutilizan
`ix_positional_player_date`, `ix_attribute_player_date` para la selección por jugador y
`ix_positional_final_score`, `ix_attribute_final_grl` para el orden por score. La consulta de la
última ejecución usa `ix_quote_executions_date`. La condición de cotización vigente se resuelve con un
`exists` sobre `player_quotes`.

**Rationale**: El esquema vigente ya contiene todo lo necesario y los índices cubren los criterios de
selección y orden. Añadir estructuras sería una optimización no solicitada (Principio I).

**Alternatives considered**:
- *Vista materializada o tabla de ranking*: rechazada; duplica estado y exige sincronización.
- *Índice compuesto nuevo para la selección*: rechazado; el índice por jugador y fecha y la unicidad
  vigente ya cubren la búsqueda y no hay evidencia de necesidad.

## 11. Separación respecto del ranking por precio

**Decision**: El ranking por precio no forma parte del alcance. El nuevo recurso se implementa en
componentes separados y no requiere modificar `PlayerQuoteController`, `QuoteQueryService`,
`QuoteQueryServiceImpl`, las consultas `findRanking` de `PlayerQuoteRepository`,
`PlayerRankingResponseDTO`, `RankingResponseDTO` ni `QuoteMapper.toRankingResponse`.

**Rationale**: La feature añade un caso de uso distinto y no necesita reespecificar ni verificar el
ranking existente. Separar sus componentes evita ampliar responsabilidades. Los tests existentes del
ranking por precio permanecen fuera de las tareas de esta feature.

**Alternatives considered**:
- *Generalizar el ranking existente para aceptar el criterio*: rechazada; cambiaría el contrato y el
  comportamiento del recurso ya entregado, que la spec declara estable.

## 12. Estrategia de tests

**Decision**: En la fase de implementación se prevén, como mínimo:

| Categoría | Cobertura |
| --- | --- |
| Repository (`integration`) | `@Query` de la última ejecución; consulta de la valuación de mayor fecha por jugador para cada estrategia; exclusión de jugadores sin cotización; orden por score y `playerId` |
| Service (`integration`) | Encabezado con estrategia y fecha de la última ejecución; población elegible; selección de la valuación más reciente aunque sea posterior a `quoteDate`; empates múltiples; respuesta vacía sin ejecuciones y con ejecución sin valuaciones; orden, posiciones y repetibilidad |
| Controller (`lightweight`) | Contrato exacto de la respuesta para caso con datos y caso vacío; `401` sin JWT; parámetros no declarados ignorados sin alterar la respuesta |

Los tests se ubican bajo `backend/src/test/java/footballmarket/{repositories,services,controllers}`
con `@Nested`, `@DisplayName` en español y una única `@Tag` literal por clase contenedora, conforme a
`docs/backend/testing.md`. El agente escribe los tests; su ejecución queda a cargo del usuario.
La ausencia de escrituras se revisa estructuralmente sobre el Service y las consultas, y la comparación
integral del estado persistido antes y después queda en la validación manual de `quickstart.md` §7:
la frontera de los tests de Service no permite consultar Repositories en Assert o Verify.

**Rationale**: `docs/backend/testing.md` §4 exige probar consultas `@Query` específicas en Repository,
§5–§6 el caso de uso en Service con persistencia real, y §8 el contrato HTTP completo en Controller. No
se requieren tests de Model (los records no tienen comportamiento propio), de Frontend (exento) ni E2E
(la ruta no añade un journey crítico distinto del contrato ya cubierto por Controller).

**Alternatives considered**:
- *Solo tests de Controller*: rechazado; no demostraría la selección de la última valuación ni el
  orden efectivo en la base.
- *Tests E2E adicionales*: rechazados; duplicarían el contrato de Controller sin aportar cobertura
  nueva, contra §2 y §13.

## 13. Fuera de alcance confirmado

Se mantiene fuera de alcance, sin ampliación: configurar o seleccionar estrategias, persistir una
estrategia activa, elegir la estrategia por parámetro, recalcular cotizaciones o valuaciones,
interfaz web, filtros, paginación y caché (sección «Fuera de alcance» de `spec.md`), y cualquier
  modificación o verificación adicional del ranking por precio.

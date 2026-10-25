# SPEC-018 — Code Review Report

**Fecha de la revisión:** 2026-10-23
**Rama revisada:** `feat/018-consultar-rankings-de-jugadores`
**Base del alcance:** árbol de trabajo sobre `HEAD` (`0dcda00`). La rama **no tiene commits propios** de
SPEC-018: el alcance revisado está compuesto por 12 archivos Java nuevos sin seguimiento, 3 repositorios
modificados y 1 línea de `specs/assignments.md`.
**Alcance revisado:** T001–T029 de `tasks.md` (US1, US2, US3, Polish y la remediación T029), 8 archivos
productivos (265 líneas), 4 archivos de test/fixture (660 líneas) y 3 repositorios modificados
(+34 líneas), más los artefactos de la spec.
**Estado de este informe:** entregado para revisión humana. **El agente no aprueba CODE REVIEW.**

---

## 1. Resumen y funcionamiento

La feature expone una consulta de solo lectura del ranking de jugadores ordenado por el score final de
la estrategia registrada en la última ejecución de cotización persistida. No crea tablas, migraciones,
dependencias, códigos de error ni roles, y no modifica el ranking por precio de SPEC-016.

### 1.1. Flujo de la consulta

`ScoreRankingQueryServiceImpl.findScoreRanking()` ejecuta dos lecturas dentro de la misma transacción
(`backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java:41-55`):

1. `QuoteExecutionRepository.findTopByOrderByQuoteDateDesc()` obtiene el encabezado: estrategia y
   `quoteDate` de la ejecución de mayor fecha.
2. Si no hay ejecución, devuelve `new ScoreRanking(null, null, List.of())` (FR-010).
3. Si hay ejecución, despacha a la consulta de valuaciones de su estrategia (FR-004):
   `PositionalValuationRepository.findScoreRanking()` para `POSITIONAL_PERFORMANCE` o
   `AttributeValuationRepository.findScoreRanking()` para `GENERAL_ATTRIBUTES`.
4. Asigna `position = índice + 1` sobre el orden devuelto por la consulta y construye las entradas con
   `playerId`, score final persistido y `valuationDate`. No reordena, recalcula ni redondea (FR-012).

### 1.2. Selección de la valuación y población

Las dos consultas JPQL
(`backend/src/main/java/footballmarket/repositories/PositionalValuationRepository.java:31-36` y
`AttributeValuationRepository.java:31-36`) aplican el mismo patrón:

- **Población:** `exists (select q.id from PlayerQuote q where q.player.id = v.player.id)`. Basta una
  fila de cotización, porque una fila implica una cotización vigente y el score no proviene de esa
  tabla (FR-003, `data-model.md` §1.2 y §2.2).
- **Valuación elegida:** `not exists` de una valuación del mismo jugador con `valuationDate` posterior.
  La unicidad `(player_id, valuation_date, strategy_name, strategy_version)` y los `CHECK` que fijan
  una única estrategia y versión (`v1`) por tabla hacen que exista como máximo una fila por jugador y
  fecha. Por ello el desempate por `calculatedAt` o `id` no es necesario (FR-004, `research.md` §4).
- **Score:** `finalScore` para posicional y `finalGrl` para atributos; no se usa `rawScore`/`rawGrl`.
- **Orden:** `score DESC, player.id ASC`, total y determinista (FR-006, US2).

La fecha de la valuación no se limita por `quoteDate`: puede ser posterior o distinta de la usada para
calcular la última cotización, tal como exige la clarificación de la spec.

### 1.3. Capa HTTP

| Elemento | Comportamiento |
| --- | --- |
| `GET /api/players/ranking/score` | Sin body, sin path variables y sin query parameters declarados; los no declarados se ignoran (FR-001) |
| Respuesta `200` | `strategy`, `quoteDate` y `ranking`; `strategy`/`quoteDate` nulos sin ejecuciones; `ranking` siempre presente (FR-008 a FR-011) |
| Entrada | `position`, `playerId`, `score` y `valuationDate`; sin nombre, versión ni id de valuación (FR-008, FR-009, FR-017) |
| `401` | Sin JWT o JWT inválido, por `anyRequest().authenticated()` (FR-016) |
| `500 INTERNAL_ERROR` | Fallo técnico de persistencia, sin clasificación parcial (FR-014) |

`ScoreRankingController` delega en el Service y entrega el Model al Mapper sin lógica
(`ScoreRankingController.java:30,46`). `ScoreRankingMapper` es una copia estructural:
`ValuationStrategy` → nombre y lista ordenada → DTO (`ScoreRankingMapper.java:15-24`).

### 1.4. Consistencia y solo lectura

`@Transactional(readOnly = true, isolation = REPEATABLE_READ)` garantiza que el encabezado y las
entradas describan la misma instantánea y que no existan escrituras (`research.md` §8). No hay
`save`, `delete` ni `flush` en el Service ni en las consultas.

---

## 2. Verificación ejecutada por el agente

| Control | Resultado |
| --- | --- |
| `./gradlew build -x test` desde `backend/` | **BUILD SUCCESSFUL** — ejecutado de nuevo tras la remediación de CR-01: `compileJava` UP-TO-DATE, Spotless y `validateTestClassification` ("Clasificación válida: 124 clases de test") |
| Compilación de tests | **No verificada.** `-x test` excluye la tarea `test` y el `build` no invocó `compileTestJava`; la compilación de las tres clases nuevas queda pendiente de la ejecución del usuario. |
| Tests de aplicación | **No ejecutados.** Reservados al usuario por la Constitución y `backend/AGENTS.md`. |
| Revisión estática del código, de los tests y de los artefactos de la spec | Realizada (este informe) |

El build acredita compilación productiva, formato y clasificación estática de tests; **no acredita
compilación de tests, ejecución de tests ni aprobación de TESTING**.

---

## 3. Archivos creados y modificados

Responsabilidad y razón de cada cambio. `A` = creado, `M` = modificado.

### 3.1. Modelo y Service

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../models/records/ScoreRanking.java` (A) | Backend | Encabezado y lista de entradas; rechaza `entries == null` | FR-002, FR-009 a FR-011; `data-model.md` §3.1 |
| `backend/.../models/records/ScoreRankingEntry.java` (A) | Backend | `position`, `playerId`, `score`, `valuationDate` | FR-008; `data-model.md` §3.2 |
| `backend/.../services/ScoreRankingQueryService.java` (A) | Backend | Interfaz del caso de uso de solo lectura | Arquitectura §1.1 (interfaz/implementación separadas) |
| `backend/.../services/impl/ScoreRankingQueryServiceImpl.java` (A) | Backend | Lectura del encabezado, despacho por estrategia, posiciones | FR-001 a FR-014; `research.md` §2, §4, §5, §8 |

### 3.2. Persistencia

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../repositories/QuoteExecutionRepository.java` (M) | Backend | `findTopByOrderByQuoteDateDesc()` | FR-002, FR-010 |
| `backend/.../repositories/PositionalValuationRepository.java` (M) | Backend | JPQL `findScoreRanking()` por `finalScore` | FR-003, FR-004, FR-006, FR-007 |
| `backend/.../repositories/AttributeValuationRepository.java` (M) | Backend | JPQL `findScoreRanking()` por `finalGrl` | FR-003, FR-004, FR-006, FR-007 |

No se modifica el esquema ni se añaden migraciones: los índices vigentes
(`ix_player_quotes_player_date`, `ix_positional_player_date`, `ix_attribute_player_date`,
`ix_positional_final_score`, `ix_attribute_final_grl`, `ix_quote_executions_date`) cubren las lecturas.

### 3.3. Capa HTTP

| Archivo | Resp. | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/.../controllers/ScoreRankingController.java` (A) | Backend | Ruta, OpenAPI y delegación | FR-001, FR-016; contrato §1–§2 |
| `backend/.../controllers/mappers/ScoreRankingMapper.java` (A) | Backend | Model → DTO sin cálculos | FR-008; arquitectura §1.2 |
| `backend/.../dtos/responses/ScoreRankingResponseDTO.java` (A) | Backend | `strategy`, `quoteDate`, `ranking` | Contrato §1.2 |
| `backend/.../dtos/responses/ScoreRankingEntryResponseDTO.java` (A) | Backend | `position`, `playerId`, `score`, `valuationDate` | Contrato §1.2.1 |

### 3.4. Tests

| Archivo | Categoría / tag | Cambio | Razón |
| --- | --- | --- | --- |
| `backend/src/test/.../repositories/ScoreRankingQueryTest.java` (A) | Repository / `integration` | Última ejecución, última valuación, población, orden y empates de ambas estrategias | T008, T018, T029 |
| `backend/src/test/.../services/ScoreRankingQueryServiceTest.java` (A) | Service / `integration` | Encabezado, orden, posiciones, score final en ambas estrategias, repetibilidad y respuestas vacías | T009, T017, T020; CR-01 |
| `backend/src/test/.../controllers/ScoreRankingControllerTest.java` (A) | Controller / `lightweight` | Contrato exacto, `401`, parámetros ignorados, `500` | T010, T021, T022 |
| `backend/src/test/.../support/ScoreRankingTestData.java` (A) | Fixture | Persistencia de cotizaciones, valuaciones (bruto/final separables) y ejecuciones en Arrange | T008–T010; CR-01 |

### 3.5. Documentación

| Archivo | Cambio | Razón |
| --- | --- | --- |
| `specs/018-consultar-rankings-de-jugadores/` (A) | Artefactos de la feature (`spec`, `plan`, `research`, `data-model`, contrato, `quickstart`, `tasks`, checklists, `sdd-gates`) | Ciclo de vida SDD |
| `specs/assignments.md` (M) | Fila de SPEC-018 | Trazabilidad issue → spec → rama |

---

## 4. Decisiones de implementación

1. **Una ruta propia en lugar de ampliar el ranking por precio.** Evita cambiar el contrato de
   SPEC-016 y separa los casos de uso (`research.md` §1, §11).
2. **El encabezado se deriva de `quote_executions`, no de las cotizaciones.** Las filas arrastradas o
   `INITIAL` no tienen estrategia; la ejecución es la fuente persistida de la estrategia aplicada
   (`research.md` §2).
3. **La población usa existencia de cotización, no la población cotizable.** FR-003 se refiere a
   cotización vigente, no a métricas disponibles (`research.md` §3).
4. **La selección es solo por fecha de valuación.** El esquema impide dos filas del mismo jugador,
   fecha, estrategia y versión; no se necesitan desempates adicionales (`research.md` §4).
5. **El orden vive en JPQL y la posición en el Service.** Una sola fuente del orden y un Mapper
   estrictamente estructural (`research.md` §5).
6. **`REPEATABLE_READ` de solo lectura.** Encabezado y entradas comparten instantánea sin escrituras
   (`research.md` §8).
7. **Sin códigos de error nuevos.** `DataAccessException`/`TransactionException` ya se traducen a
   `INTERNAL_ERROR`; la ausencia de datos no es error (`research.md` §9).

---

## 5. Hallazgos de la revisión

Severidad: **Alta** = corrección, contrato o disponibilidad; **Media** = riesgo de regresión o
degradación consistente; **Baja** = mantenibilidad, legibilidad o limpieza.

| ID | Severidad | Hallazgo | Evidencia | Recomendación |
| --- | --- | --- | --- | --- |
| CR-01 | Media — corregido (test añadido, pendiente de ejecución) | **Cobertura de tests: el score persistido no se afirmaba en Service/Repository y la rama de atributos no se ejercitaba con datos.** La implementación **sí** devuelve entradas de `GENERAL_ATTRIBUTES` (el `else` despacha a `AttributeValuationRepository.findScoreRanking()` y mapea `finalGrl`); lo que no existía era un test de Service que recorriera esa rama con valuaciones persistidas. Tampoco ningún test de Service afirmaba `ScoreRankingEntry::score`. Una regresión que expusiera `rawGrl` en lugar de `finalGrl` no se detectaba en el caso de uso, pese a FR-004/FR-012/SC-005. | `backend/src/test/java/footballmarket/services/ScoreRankingQueryServiceTest.java:71-74,110-154`; `backend/src/test/java/footballmarket/support/ScoreRankingTestData.java:50-87,157-180` | Corregido: se añadió el caso de Service con ejecución `GENERAL_ATTRIBUTES`, score bruto distinto del final y afirmaciones de `score` y `valuationDate`; además se afirman los scores en el test posicional. Pendiente de ejecución por el usuario. Registrado como TD-031. |
| CR-02 | Descartado | **Despacho por defecto: sin hallazgo.** El responsable del proyecto confirmó que el catálogo de estrategias es cerrado: siempre serán `POSITIONAL_PERFORMANCE` y `GENERAL_ATTRIBUTES`, y no se sumará otra. El `else` es, por tanto, correcto y no requiere cambio. | `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java:57-81`; `ValuationStrategy`; `V20261019120000__create_player_valuations.sql:16,38` | Sin acción. Se conserva el identificador con la decisión registrada. |
| CR-03 | Baja | **`ScoreRanking` no copia defensivamente la lista.** Solo rechaza `null`; si un llamador construyera el record con una lista mutable, el resultado quedaría modificable. `ScoreRankingEntry` tampoco valida `position`, `playerId`, `score` ni `valuationDate`, aunque `data-model.md` §3.2 los declara no nulos. | `backend/src/main/java/footballmarket/models/records/ScoreRanking.java:15-21`; `ScoreRankingEntry.java:13-14` | `List.copyOf(entries)` en el constructor compacto y, si se desea, validación de las invariantes declaradas. |
| CR-04 | Baja | **Construcción de entradas duplicada** para las dos estrategias: dos bucles casi idénticos y `Integer` boxeado para una posición derivada de un índice. | `backend/src/main/java/footballmarket/services/impl/ScoreRankingQueryServiceImpl.java:59-81` | Extraer un helper genérico o `IntStream.range(...).mapToObj(...)` por estrategia. |
| CR-05 | Baja | **El test de contrato no verifica la escala del score.** `$.ranking[0].score` se afirma con `closeTo(0.9134, …)`, que aceptaría `0.9134`, `0.913400` o un redondeo distinto. El contrato exige seis decimales sin transformación (SC-005). | `backend/src/test/java/footballmarket/controllers/ScoreRankingControllerTest.java:94` | Afirmar el texto JSON exacto del campo o comparar la representación serializada con `0.913400`. |
| CR-06 | Baja | **Riesgo heredado, no nuevo: encabezado de una ejecución parcial.** `findTopByOrderByQuoteDateDesc()` no distingue una corrida terminada de una reservada. Si SPEC-016 deja una fila `quote_executions` de una corrida fallida o en curso, el ranking podría encabezarse con esa estrategia y fecha. No contradice el contrato («última ejecución persistida»), pero conviene tenerlo presente al resolver TD-025. | `backend/src/main/java/footballmarket/repositories/QuoteExecutionRepository.java:24-29`; TD-025 | Sin cambio en esta feature. Resolver con el estado de ejecución de SPEC-016. |
| CR-07 | Baja | **Alcance sin commits.** Todo el código de SPEC-018 está en el árbol de trabajo; `origin/dev...HEAD` no muestra los cambios y la trazabilidad spec → rama → commits → PR aún no existe para esta feature. | `git status`; `HEAD = 0dcda00` | Confirmar con el usuario el agrupamiento de commits antes del PR; no es un defecto de código. |

**Decisiones del revisor (2026-10-23).**

- **CR-01 se mantiene** como hallazgo de **cobertura de tests**, no de comportamiento: el endpoint
  devuelve entradas de `GENERAL_ATTRIBUTES` correctamente; faltaba el test de Service que lo demuestre.
  Con autorización del responsable se añadió ese test (`AttributeRanking.exposesAttributeFinalScore`) y
  las afirmaciones de score del caso posicional. Queda **pendiente de ejecución**; el build permitido no
  compila ni ejecuta los tests.
- **CR-02 se descarta** por decisión del responsable: el catálogo de estrategias es cerrado —siempre
  serán `POSITIONAL_PERFORMANCE` y `GENERAL_ATTRIBUTES`— y no se ampliará a una tercera. Se conserva el
  identificador y la justificación.

No hay hallazgos de severidad Alta. La implementación es correcta respecto de la spec y del contrato
revisados.

### 5.1. Verificaciones sin hallazgo

- **Contrato HTTP:** la ruta, el verbo, la ausencia de body/query declarados, los tres campos de
  respuesta, los cuatro de la entrada, los nulos de la respuesta vacía y la ausencia de campos extra
  coinciden con `contracts/score-ranking-http.md` §1.2–§1.2.1.
- **Población y selección:** el `exists`/`not exists` equivale a las reglas de `data-model.md` §2.2 y
  §2.3; la unicidad vigente impide duplicados por jugador y el orden es total.
- **Esquema:** no hay migraciones ni cambios de modelo; las consultas usan índices existentes.
- **Seguridad:** `anyRequest().authenticated()` cubre la ruta, no se añaden roles y los errores no
  exponen ids de valuación, versión de estrategia ni trazas (FR-016, FR-017).
- **Solo lectura:** no hay operaciones de escritura en Service ni en las consultas; `readOnly = true`
  y `REPEATABLE_READ` son coherentes con `research.md` §8.
- **Capas:** Controller sin lógica, Mapper estructural, Service con interfaz e implementación separadas
  y Repository sin negocio, conforme a `docs/backend/architecture.md` §1.1–§1.2.
- **Clasificación de tests:** una única `@Tag` literal por clase contenedora, `@Nested` y
  `@DisplayName` en español; `validateTestClassification` acepta las 124 clases.
- **OpenAPI:** ruta, `200`, `401`, `500`, nulabilidad y ejemplos declarados en las anotaciones y en los
  DTO, sin `strategyVersion` (T024).

---

## 6. Cobertura de requisitos

| Grupo | FR / SC | Estado de la revisión |
| --- | --- | --- |
| Consulta y parámetros | FR-001 | Cubierto: ruta propia sin parámetros declarados; el test verifica que un query param no altera la respuesta |
| Encabezado | FR-002, FR-009 | Cubierto: estrategia y fecha de la última ejecución; versión excluida |
| Población | FR-003 | Cubierto: `exists` sobre `player_quotes`; exclusión sin cotización y sin valuación |
| Score y selección | FR-004, FR-012 | Cubierto en código y en test de Service tras CR-01 (score final y fecha por entrada, pendiente de ejecución) |
| Orden y posiciones | FR-006, FR-007 | Cubierto: `score DESC, playerId ASC`, posiciones 1..n, empates múltiples y repetibilidad |
| Entrada | FR-008 | Cubierto: solo `position`, `playerId`, `score`, `valuationDate` |
| Respuestas vacías | FR-010, FR-011 | Cubierto en Service y Controller |
| Solo lectura | FR-013, SC-007 | Cubierto estructuralmente; la comparación integral es validación manual (`quickstart.md` §7) |
| Errores | FR-014 | Cubierto: `500 INTERNAL_ERROR` sin clasificación parcial |
| Autenticación | FR-016, SC-009 | Cubierto: `401` sin JWT y con JWT inválido, sin invocar el Service |
| No exposición | FR-017, SC-010 | Cubierto: contrato cerrado y error con `ErrorResponseDTO` |

---

## 7. Revisión de la suite de tests

**Cobertura correcta.** Última ejecución por fecha; última valuación por jugador incluso posterior a
`quoteDate`; exclusión de jugadores sin cotización; orden y empates múltiples para ambas estrategias en
Repository; encabezado, orden, posiciones consecutivas, repetibilidad y tres estados vacíos en Service;
contrato exacto con datos y sin datos, `401` sin/con JWT inválido, query parameters ignorados y `500`
sin datos parciales en Controller; fixture con cotizaciones `INITIAL` válidas para los `CHECK`.

**CR-01 corregido en revisión.** El test posicional afirma ambos scores y el nuevo
`AttributeRanking.exposesAttributeFinalScore` ejercita la rama `GENERAL_ATTRIBUTES` con dos jugadores,
score bruto distinto del final, orden por score final y `valuationDate` por entrada; así una regresión
`rawGrl` → `finalGrl` sí se detecta. El fixture `ScoreRankingTestData` admite ahora bruto y final
separados. **Pendiente de compilación y ejecución por el usuario**: `build -x test` no ejecuta
`compileTestJava` ni la suite. **Deficiencia restante:** CR-05, el test de contrato no discrimina la
escala de seis decimales.

**Clasificación.** `ScoreRankingQueryTest` y `ScoreRankingQueryServiceTest` usan `@SpringBootTest`,
Testcontainers y `@Tag("integration")`; `ScoreRankingControllerTest` usa `@WebMvcTest`,
`@Tag("lightweight")` y `MockMvc` con la cadena de seguridad real, siguiendo el patrón de los
Controllers existentes. `validateTestClassification` informa «Clasificación válida: 124 clases de
test» dentro del build permitido.

**Ejecución.** Sin evidencia de ejecución: la suite y la compilación de tests quedan a cargo del
usuario. El build no acredita ninguna de las dos.

---

## 8. Deuda técnica registrada en esta revisión

| ID | Título | Prioridad |
| --- | --- | --- |
| TD-031 | Cobertura de tests incompleta para el ranking por score (valor del score y rama `GENERAL_ATTRIBUTES` en el Service) | Media — `En curso` (test añadido, pendiente de ejecución) |

No se declara resuelta ninguna deuda existente: TD-031 pasa a `En curso` y solo se cerrará con la
ejecución de los tests. TD-025 se referencia como riesgo heredado (CR-06) sin modificar su estado.

---

## 9. Orden de revisión sugerido

1. **Contrato y spec** — `spec.md` (FR-001 a FR-017), `contracts/score-ranking-http.md` §1–§2 y
   `data-model.md` §2.
2. **Consultas** — `QuoteExecutionRepository.findTopByOrderByQuoteDateDesc()`,
   `PositionalValuationRepository.findScoreRanking()`, `AttributeValuationRepository.findScoreRanking()`.
3. **Service** — `ScoreRankingQueryServiceImpl` (CR-01, CR-03 y CR-04), con `research.md` §2, §4, §5 y
   §8.
4. **Model** — `ScoreRanking` y `ScoreRankingEntry` (CR-03).
5. **HTTP** — `ScoreRankingController`, `ScoreRankingMapper` y los dos DTO.
6. **Tests** — `ScoreRankingQueryTest`, `ScoreRankingQueryServiceTest`,
   `ScoreRankingControllerTest` y `ScoreRankingTestData` (CR-01, CR-05).
7. **Documentación** — `plan.md`, `research.md`, `quickstart.md`, checklists y `tasks.md`.
8. **Git** — agrupamiento de commits de la feature (CR-07) antes del PR.

---

## 10. Checklist humano de verificación

El usuario revisa y marca cada ítem. **El agente no aprueba este checklist.**

### A. Corrección funcional

- [ ] A1. La estrategia y `quoteDate` provienen de la ejecución de mayor fecha y no de las cotizaciones.
- [ ] A2. Sin ejecuciones se devuelve `strategy: null`, `quoteDate: null` y `ranking: []`.
- [ ] A3. Con ejecución sin valuaciones se conserva el encabezado y el ranking queda vacío.
- [ ] A4. Solo aparecen jugadores con cotización y con valuación de la estrategia de la ejecución.
- [ ] A5. Se usa la valuación de mayor `valuationDate`, aunque sea posterior a `quoteDate`.
- [ ] A6. El score es `finalScore` o `finalGrl` según la estrategia, sin redondeo ni recálculo.
- [ ] A7. El orden es `score DESC, playerId ASC` con posiciones consecutivas de 1 a n.
- [ ] A8. La consulta no escribe ni altera cotizaciones, precios ni valuaciones.

### B. Contrato HTTP

- [ ] B1. La respuesta contiene solo `strategy`, `quoteDate` y `ranking`, sin campos extra.
- [ ] B2. Cada entrada contiene solo `position`, `playerId`, `score` y `valuationDate`.
- [ ] B3. `score` conserva la escala persistida de seis decimales.
- [ ] B4. Sin JWT o con JWT inválido se responde `401` sin datos; un fallo técnico responde `500`
      `INTERNAL_ERROR` sin clasificación parcial.
- [ ] B5. La ruta ignora query parameters no declarados sin alterar el resultado.

### C. Persistencia y rendimiento

- [ ] C1. No hay migraciones ni cambios de esquema.
- [ ] C2. Las consultas usan los índices existentes y no generan productos cartesianos.
- [ ] C3. El criterio «cotización vigente» es la existencia de fila en `player_quotes`, conforme a
      `data-model.md` §1.2.
- [ ] C4. La transacción es de solo lectura y con aislamiento `REPEATABLE_READ`.

### D. Tests

- [ ] D1. Los tests compilan (`./gradlew compileTestJava` o la ejecución completa) y pasan
      (`./gradlew lightweightTest` y `./gradlew integrationTest`).
- [ ] D2. Los tests de persistencia corren contra PostgreSQL real (Testcontainers), no embebido.
- [ ] D3. Se comprueba el caso de CR-01 añadido (`AttributeRanking.exposesAttributeFinalScore`) y su
      ejecución antes de aprobar TESTING.
- [ ] D4. La clasificación de tests sigue siendo válida (`validateTestClassification`).

### E. Riesgo y deuda

- [ ] E1. Se revisa el test añadido para CR-01 y la entrada TD-031 (`En curso`; cierre tras ejecución).
- [ ] E2. Se revisan CR-03 a CR-05 (severidad Baja) y se decide si se corrigen o se aceptan. CR-02
      queda descartado por decisión registrada.
- [ ] E3. Se revisa CR-06 como riesgo heredado de TD-025, sin cambio en esta feature.
- [ ] E4. Se revisa CR-07 (agrupamiento de commits) antes de abrir el PR.

### F. Git

- [ ] F1. Los archivos del alcance son exactamente los listados en la sección 3.
- [ ] F2. Los commits referencian SPEC-018, las US y las tareas.
- [ ] F3. No se incorporan artefactos generados, `backend/build/` ni cambios ajenos.

---

## 11. Estado y siguiente acción

- **CODE REVIEW**: informe entregado, **pendiente de aprobación humana del checklist de §10**. CR-01
  tiene el test añadido, pero CODE REVIEW no se aprueba hasta revisarlo.
- **TESTING**: pendiente; la ejecución y la compilación de los tests quedan a cargo del usuario. El
  build no acredita tests. Tests concretos sugeridos: `./gradlew lightweightTest` y
  `./gradlew integrationTest` desde `backend/`.
- **CONVERGE** y **FEATURE COMPLETION**: no habilitados.
- **Deuda técnica**: TD-031 `En curso` (test añadido, cierre tras ejecución); TD-025 se referencia como
  riesgo heredado sin cambio de estado.
- **Siguiente acción permitida**: ejecución de los tests por el usuario y aprobación humana de CODE
  REVIEW con el checklist de §10; después, la fase de TESTING.

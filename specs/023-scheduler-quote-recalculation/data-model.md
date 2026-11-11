# Data Model: Recálculo automático programado de cotizaciones

## Modelo vigente de integración — 2026-11-11

Modelo compartido: [SPEC-016](../016-ejecutar-recalculo-manual-de-cotizaciones/data-model.md) y
[base común §2](integration-baseline.md). No hay nuevas tablas ni versiones intradiarias.

| Elemento | Invariante integrada |
| --- | --- |
| `quote_executions.quote_date` | Fecha UTC de inicio; múltiples manuales y una automática como máximo |
| `uq_quote_executions_automatic_date` | Índice parcial por fecha `WHERE origin='AUTOMATIC'`; incluye contexto aún sin cierre |
| `quote_executions.finished_at` | Nulo en contexto abierto, informado al cerrar; solo finalizadas en GET |
| Contadores | Partición disjunta al cerrar; reemplazos cuentan en recalculados/arrastrados, omisión solo por fecha posterior |
| `player_quotes` | UNIQUE jugador/fecha efectiva; fecha = día UTC fijado al iniciar corrida |
| Reemplazo | Conservar ID/jugador/fecha; reemplazar cálculo completo y `execution_id` atómicamente |
| `execution_id` | FK nula en INITIAL, obligatoria en otros orígenes; referencia la última corrida productora |
| Score fuente | `source_valuation_date` separado de fecha efectiva; score antiguo sigue utilizable |
| Inicialización | Sigue vigente tras cambiar origen de la fila; no reinicializar ni sobrescribir al repetir initialize |
| Lectura HTTP | `executionId` contractual; orden de finalizadas por inicio/ID descendente |

No repetir DDL incompatible entre las dos migraciones de las ramas ni cambiar checksums de
migraciones inmutables; guardar colisiones para decisión humana sin eliminación automática. El
antecedente siguiente se conserva, pero sus reglas incompatibles no son diseño vigente.

## Modelo de origen — antecedente, no aplicar reglas sustituidas

**Feature**: `023-scheduler-quote-recalculation`
**Spec**: [`spec.md`](spec.md) · **Research**: [`research.md`](research.md)

Esta feature **no crea tablas ni entidades nuevas**. Amplía el registro persistido de ejecuciones ya
existente (`quote_executions`, SPEC-016) con el origen de la corrida y deriva de sus contadores el
resultado y el motivo de fallo. La programación es configuración, no estado persistido, y el registro
operativo es evidencia en logs, no una tabla.

---

## 1. `quote_executions` — Ejecución de cotización (ampliada)

Registro append-only: admite cualquier cantidad de filas `MANUAL` por fecha UTC y como máximo una
fila `AUTOMATIC` por fecha. La columna `origin` es la única incorporación de esta feature al modelo.

| Columna | Tipo | Nulable | Semántica |
| --- | --- | --- | --- |
| `id` | `BIGINT` PK (`quote_executions_id_seq`) | no | Identificador único de la ejecución (FR-004) |
| `quote_date` | `DATE` | no | Fecha de cotización: la fecha UTC del día en que corre (FR-001, FR-006) |
| `origin` | `VARCHAR(16)` | no | **Nuevo.** `MANUAL` o `AUTOMATIC` (FR-004) |
| `strategy_name` | `VARCHAR(64)` | no | Estrategia aplicada; en la vía automática proviene de `market.quote.strategy` (FR-002) |
| `strategy_version` | `VARCHAR(16)` | no | Versión de la estrategia aplicada |
| `adjustment_factor` | `NUMERIC(18,6)` | no | Factor de ajuste aplicado (FR-002) |
| `variation_cap` | `NUMERIC(18,6)` | no | Tope de variación aplicado (FR-002) |
| `min_price_floor` | `BIGINT` | no | Piso mínimo aplicado, en centésimas (FR-002) |
| `priced_players` | `INTEGER` | no | Población evaluada: alcance procesado de la corrida (FR-004) |
| `recomputed_rows` | `INTEGER` | no | Filas `RECOMPUTED` escritas |
| `carried_forward_rows` | `INTEGER` | no | Filas `CARRIED_FORWARD` escritas |
| `not_repriced_players` | `INTEGER` | no | Jugadores sin valuación posterior a su fecha de referencia |
| `failed_players` | `INTEGER` | no | Jugadores con fallo individual; base del resultado derivado (FR-004) |
| `started_at` | `TIMESTAMPTZ` | no | Inicio de la corrida |
| `finished_at` | `TIMESTAMPTZ` | no | Fin de la corrida |

### Restricciones

| Nombre | Definición | Origen |
| --- | --- | --- |
| `uq_quote_executions_automatic_date` | `UNIQUE (quote_date) WHERE origin = 'AUTOMATIC'` (índice único parcial) | FR-006: como máximo una corrida automática por fecha UTC; corridas manuales ilimitadas |
| `ck_quote_execution_origin` | `CHECK (origin IN ('MANUAL', 'AUTOMATIC'))` | **Nueva.** Catálogo cerrado del origen (FR-004) |
| `ck_quote_execution_factor`, `ck_quote_execution_cap`, `ck_quote_execution_floor`, `ck_quote_execution_counters` | Sin cambios | SPEC-016 |

No se añade un valor por defecto permanente a `origin`: una inserción sin origen debe fallar, no
quedar clasificada en silencio como `MANUAL`.

### Campos derivados (no persistidos)

| Campo expuesto | Derivación | Motivo |
| --- | --- | --- |
| `result` | `failed_players = 0` → `SUCCESS`; `failed_players > 0` → `FAILED` | FR-004: el resultado ya es determinable de un contador persistido; una columna adicional sería estado duplicado y podría divergir |
| `failureReason` | `failed_players = 0` → `null`; `failed_players > 0` → `INDIVIDUAL_PLAYER_FAILURES` | FR-004 y FR-010: motivo expresado como código cerrado, sin trazas ni mensajes técnicos |

Los campos se calculan en el Model a partir de su propio estado; el Mapper solo los transporta. El
registro persistido contiene así el resultado y el motivo de forma derivada y determinista, sin columnas
que puedan divergir (FR-004). Una corrida que falla por completo **no deja fila**, de modo que su motivo
no forma parte de esta tabla: vive únicamente en el registro operativo (FR-013).

### Índices

| Nombre | Definición | Uso |
| --- | --- | --- |
| `ix_quote_executions_date` | `(quote_date DESC, started_at DESC)` | Consulta existente por fecha retorna todas las corridas (FR-005) |

## 1.1. `player_quotes` — Cotización diaria de jugador

La tabla existente no agrega entidad ni columnas. Su restricción actual `uq_player_quote
(player_id, effective_date, origin)` debe cambiarse por unicidad `(player_id, effective_date)`, sin
distinción de origen. Una ejecución posterior actualiza cualquier fila diaria existente con el último
cálculo, incluyendo precio, estrategia, origen y hora de cálculo.

| Invariante | Regla |
| --- | --- |
| Cotización diaria única | A lo sumo una fila por `(player_id, effective_date)`, cualquiera sea `origin` |
| Actualización repetida | Recalcular mismo jugador y fecha actualiza precio, estrategia, `origin` y `calculated_at` en la fila existente |
| Historial | Se conserva la fila y su identidad; no se borra ni inserta una segunda fila diaria |

La migración nueva sustituye la unicidad anterior sin modificar migraciones ya aplicadas. Debe
comprobar colisiones existentes de jugador-fecha antes de imponer la nueva restricción y documentar
su tratamiento para el entorno de desarrollo.

---

## 2. Enums de dominio

| Enum | Valores | Ubicación | Responsabilidad |
| --- | --- | --- | --- |
| `QuoteExecutionOrigin` | `MANUAL`, `AUTOMATIC` | `features/quote/domain/enums/` | Origen de la corrida; persistido en `quote_executions.origin` (FR-004) |
| `QuoteExecutionResult` | `SUCCESS`, `FAILED` | `features/quote/domain/enums/` | Desenlace derivado de la corrida (FR-004) |
| `QuoteExecutionFailureReason` | `INDIVIDUAL_PLAYER_FAILURES` | `features/quote/domain/enums/` | Motivo apto para exposición externa (FR-004, FR-010) |

`QuoteOrigin` (`INITIAL`, `RECOMPUTED`, `CARRIED_FORWARD`) es un enum distinto y preexistente: describe
el origen de una fila de `player_quotes`, no el de una ejecución. No se reutiliza ni se modifica.

---

## 3. Model `QuoteExecution`

Cambios sobre el Model existente:

- Nueva propiedad `origin`, obligatoria y no nula, validada en el constructor: un origen ausente es un
  contexto incompleto y lanza `InvalidQuoteException`, igual que el resto de campos obligatorios.
- Accesores derivados `result()` y `failureReason()` coherentes con `failed_players`. No se
  persisten ni se pueden fijar desde fuera.
- `complete(...)` no cambia: sigue fijando los contadores y el fin de la corrida, y por lo tanto
  determina el resultado derivado.

### Transiciones de estado de una ejecución

| Estado | Cómo se alcanza | Qué queda persistido |
| --- | --- | --- |
| Reservada | Inserción con `saveAndFlush` al inicio de la corrida, dentro de la transacción externa | Fila con parámetros, origen y contadores en cero |
| Completada con éxito | Fin del recorrido sin fallos individuales | Fila con contadores definitivos; `result = SUCCESS` |
| Completada con fallos individuales | Fin del recorrido con `failed_players > 0` | Fila con contadores definitivos; `result = FAILED`, motivo `INDIVIDUAL_PLAYER_FAILURES` |
| Descartada | Excepción no controlada que revierte la transacción externa existente | Ninguna fila de ejecución para la fecha; el intento queda en el registro operativo (FR-013). Las cotizaciones ya confirmadas pueden permanecer; compensarlas está fuera del alcance del scheduler (TD-024) |

El comportamiento existente no persiste un estado terminal para el fallo completo. Un resultado
`FAILED` por fallos individuales corresponde a una ejecución completada y registrada. Solo una corrida
`AUTOMATIC` registrada consume la cuota automática de esa fecha; las corridas manuales no consumen
esa cuota.

---

## 4. Configuración de la programación (no persistida)

| Propiedad | Tipo | Obligatoria | Valor por defecto | Regla |
| --- | --- | --- | --- | --- |
| `market.quote.strategy` | enum `ValuationStrategy` | no | `POSITIONAL_PERFORMANCE` | Estrategia aplicada por la ejecución automática (FR-002) |
| `market.quote.schedule.cron` | `String` | no | `0 0 12 * * *` | Expresión cron de 6 campos de Spring, diaria a las 12:00 UTC por defecto (FR-001) |
| `market.quote.schedule.enabled` | `boolean` | no | `true` | Habilita el disparador; no es un requisito funcional (ver [`research.md`](research.md) §4) |

`market.quote.{adjustment-factor, variation-cap, min-price-floor, initial-anchor}` conservan su
semántica actual: son los parámetros que la ejecución automática toma de la misma configuración del
mercado (FR-002). Una estrategia fuera del catálogo impide el arranque al validarse el binding de
`QuoteProperties`; una expresión cron inválida también lo impide, porque Spring valida su sintaxis al
construir el disparador (`@Scheduled`).

---

## 5. Registro operativo (no persistido)

Evidencia estructurada de los intentos que **no** producen ejecución persistida. No es una tabla ni
sustituye ni duplica `quote_executions`:

| Situación | Evento | Datos mínimos |
| --- | --- | --- |
| Disparo automático adicional en fecha con corrida automática | `quote_automatic_recalculation_skipped` | fecha UTC, motivo `EXECUTION_ALREADY_EXISTS` |
| Rechazo por mercado no inicializado | `quote_automatic_recalculation_rejected` | fecha UTC, motivo `MARKET_NOT_INITIALIZED` |
| Fallo completo de la corrida | `quote_automatic_recalculation_failed` | fecha UTC, clase de la causa |
| Disparo aceptado | `quote_automatic_recalculation_triggered` | fecha UTC |

Los eventos usan SLF4J y el formato estructurado vigente. No se registran trazas ni identificadores
internos en la respuesta HTTP; los logs operativos son evidencia para el operador, no contrato HTTP.

---

## 6. Migración

Una única migración Flyway nueva, creada con `./gradlew createMigration` para respetar el versionado
estrictamente creciente del repositorio:

1. `ALTER TABLE quote_executions ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'MANUAL'` — relleno
   único de las filas existentes.
2. `ALTER TABLE quote_executions ALTER COLUMN origin DROP DEFAULT` — sin valor por defecto permanente.
3. `ALTER TABLE quote_executions ADD CONSTRAINT ck_quote_execution_origin CHECK (origin IN ('MANUAL', 'AUTOMATIC'))`.

No se modifica ninguna migración previa, no se recrea la tabla y no se añaden columnas preservadoras.
Los datos de desarrollo son descartables, pero la migración debe aplicarse sin reescribir historial.

---

## 7. Invariantes verificables

Las transiciones y resultados de persistencia describen el recálculo existente, no responsabilidades ni garantías nuevas del scheduler. Ante errores, este se limita a registrarlos; TD-024 permanece pendiente.

| Invariante | Requisito | Verificación observable |
| --- | --- | --- |
| Manuales ilimitadas por fecha y como máximo una automática | FR-006, FR-015 | Índice único parcial sobre fecha para `AUTOMATIC`; inserciones manuales distintas admitidas |
| Una cotización por jugador-fecha | FR-014 | Restricción única por jugador/fecha sin distinción de origen; corrida posterior actualiza la fila |
| Toda ejecución persistida tiene origen | FR-004 | `origin NOT NULL` + `CK` del catálogo cerrado |
| El resultado es coherente con los contadores | FR-004 | Derivación en el Model; no existe columna alternativa que pueda divergir |
| Error de invocación registrado | FR-013, SC-008 | El scheduler registra el error; el rollback y la disponibilidad de reintento dependen del recálculo existente. Las cotizaciones ya confirmadas no se compensan (TD-024) |
| Los registros de ejecución se conservan indefinidamente | FR-005 | Sin purga ni retención configurable |

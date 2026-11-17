# Base documental compartida — integración SPEC-016 / SPEC-023

**Fecha**: 2026-11-11. **Rama destino**: `feat/023-daily-quote-scheduling`.
**Origen del merge**: `docs/016-daily-quote-recalculation-spec`, commit `ce794c2`.
**Base publicada de SPEC-023**: `7fe1d4b`.

Este documento reúne el diseño y la trazabilidad de la decisión humana incorporada en ambas
`spec.md`; no crea una tercera spec, no sustituye los requisitos de esas specs ni acredita ANALYZE.
Los encabezados de integración de sus artefactos prevalecen sobre sus antecedentes incompatibles.

## 1. Comportamiento acordado

| Aspecto | Regla única para ambas specs | Responsable |
| --- | --- | --- |
| Cadencia | Diario, `0 0 12 * * *`, UTC fija, configurable | SPEC-023 |
| Ejecuciones | Manuales ilimitadas; como máximo una automática registrada por día UTC; las manuales no consumen la cuota automática | SPEC-023 sobre persistencia de SPEC-016 |
| Fecha | `quoteDate` y `effectiveDate` se fijan con la fecha UTC de inicio de la corrida, aunque cruce medianoche | SPEC-016 |
| Score | Último score persistido de la estrategia, aunque sea antiguo; no invocar proveedores ni recalcular scores | SPEC-016 |
| Precio | Fórmula existente sobre el precio vigente leído antes de cada cálculo; mismo día también puede recalcularse; no limitar a un único ajuste diario | SPEC-016 |
| Sin score | Precio vigente arrastrado, multiplicador 1 y score/fecha fuente nulos | SPEC-016 |
| Cardinalidad | Una sola fila por jugador y fecha, sin distinguir origen/estrategia; conservar identidad al reemplazar todos los datos del cálculo | Ambas |
| Auditoría | Actualizar también `execution_id`, estrategia/versión, fecha fuente y `calculated_at`; parámetros en el resumen de la ejecución referenciada | SPEC-016 |
| Lecturas | Vigente = máxima fecha efectiva; consulta diaria = su única fila; historial = un precio por fecha, sin versiones intermedias intradiarias | SPEC-016 |
| Corrida atrasada | Omitir solo si la fecha vigente es posterior a `quoteDate`; igualdad permite actualizar | SPEC-016 |
| Inicialización | No insertar otro INITIAL ni reinicializar el mercado cuando la fila inicial del día haya cambiado de origen por un recálculo | SPEC-016 |
| Ranking | Conservar paginación, filtros y posiciones globales de SPEC-016; no modificar UI ni selección de score de otras specs | SPEC-016 |
| Fallos | Atomicidad por jugador; sin compensación, recuperación ni lock global nuevos; TD-024 sigue pendiente | Ambas |

Una segunda corrida aplica la misma fórmula sobre el precio entonces vigente. No es una
operación idempotente de negocio: conservar una única fila evita duplicados, no impide nuevos
ajustes. Si falla la publicación de un jugador, su último cálculo confirmado permanece intacto.
El último cálculo válido es el último confirmado para ese jugador-fecha; no se elige por ID de
ejecución ni se permite que una corrida atrasada altere una fecha posterior.

## 2. Diseño integrado

### Publicación e inicialización

Conservar el bloqueo por identidad de jugador ya diseñado en SPEC-016, compartido entre
inicialización y publicación. Dentro de `REQUIRES_NEW`, leer vigente y fila del día bajo ese
bloqueo. Omitir únicamente una fecha posterior; calcular usando la fórmula existente y luego
insertar si falta fila diaria o reemplazar íntegramente el cálculo si existe. Mantener inmutables
ID, jugador y fecha; actualizar precio, precio anterior, multiplicador, score, estrategia, versión,
fecha de score, origen, instante y referencia a ejecución de forma atómica.

Conservar el contexto de corrida confirmado antes de publicar jugadores, mediante
`QuoteExecutionRecorder`, y el cierre independiente. El contexto sin fin no es un resumen
finalizado. Actualizar `execution_id` al mismo tiempo que el precio evita atribuir el último cálculo
a los parámetros de otra corrida. Un reemplazo cuenta en `recomputedRows` o `carriedForwardRows`,
no en `notRepricedPlayers`. Este último cuenta omisiones por fecha vigente posterior.

La inicialización no debe deducirse exclusivamente de `origin=INITIAL`, porque el reemplazo puede
cambiar ese origen sin desinicializar el mercado. Usar existencia de cotizaciones y la primera
fecha efectiva persistida para conservar la fecha de mercado; esa fila no se borra ni cambia de
fecha. Repetir initialize no debe sobrescribir un cálculo ni crear otra fila. Se conserva su ancla
100 y sus rechazos por fecha de mercado incompatible.

### Corrida automática

Reservar el contexto automático mediante inserción confirmada y protegida por índice único parcial
`uq_quote_executions_automatic_date ON quote_executions(quote_date) WHERE origin='AUTOMATIC'`.
Traducir solo ese conflicto estructurado (SQLSTATE `23505` y constraint correspondiente) a
`ExecutionAlreadyExistsException`; el scheduler lo registra como `skipped`. No eliminar esa clase
ni convertir cualquier fallo SQL en ese motivo. Una manual no genera este rechazo por fecha.
Un contexto automático ya registrado consume la cuota aun si no llegó a cerrarse; no se añade
recuperación automática de contextos interrumpidos. Las manuales siguen permitidas.

### Persistencia y migraciones

- Mantener `uq_player_quote UNIQUE(player_id,effective_date)` y FK `execution_id` nula en INITIAL,
  obligatoria en RECOMPUTED/CARRIED_FORWARD. Resultado/motivo de corrida continúan derivados.
- Mantener `finished_at` nullable en contexto abierto, partición de contadores al cerrar e índice
  `(quote_date DESC,started_at DESC,id DESC)` para resúmenes finalizados.
- Reconciliar la secuencia de `V20261110235437__daily_quote_recalculation.sql` (SPEC-016) y
  `V20261111163841__allow_daily_quote_recalculation.sql` (SPEC-023): solo la primera retira la
  unicidad global de ejecución y cambia la unicidad de cotización; la segunda comprueba el estado
  compatible y añade la unicidad automática parcial, sin repetir DROP de constraints ya retiradas.
- Antes de aplicar nuevas restricciones, comprobar colisiones y datos incompatibles con la FK.
  No borrar, elegir ni rellenar filas automáticamente. Detener y solicitar resolución humana si
  los datos son incompatibles.
- No modificar migraciones integradas en `dev`. Antes de editar las dos migraciones de las ramas,
  verificar su estado de integración/aplicación; si alguna ya debe ser inmutable, usar una nueva
  migración generada por la tarea oficial y preservar su checksum. El fallo humano de Flyway sigue
  sin diagnóstico; no se presume que provenga únicamente de este solapamiento.

### Contrato HTTP

Conservar `executionId` público como identificador contractual de corrida, no como filtración
accidental de detalles internos. POST manual repetido devuelve 200 con resumen independiente;
GET por fecha devuelve todos los resúmenes finalizados, orden inicio/ID descendente. No exponer
contextos incompletos como éxitos. Sin trazas, mensajes técnicos ni IDs internos no contractuales.
Los objetos de cotización mantienen su shape y muestran el último cálculo diario. Ranking paginado
de SPEC-016 y dependencias pendientes de SPEC-018/022 se conservan sin ampliar alcance.

## 3. Cobertura y tareas de integración

Las tareas históricas conservan IDs, avance y evidencia. Su marca no acredita el comportamiento
combinado. Las remediaciones están exclusivamente en los `tasks.md` de sus responsables:

| Criterio verificable integrado | SPEC-016 | SPEC-023 |
| --- | --- | --- |
| Dos manuales del día, IDs distintos, una fila con último precio y ejecución; releer historial y vigente | T081–T083 | T043–T044 |
| Mismo score antiguo en D y D+1; nueva valuación o estrategia en otra manual de D; fórmula sin cambios | T081–T083 | T044 |
| Actualizar INITIAL/arrastrada sin duplicar ni perder inicialización; repetir initialize sin sobreescribir | T081–T083 | — |
| Cuota automática no consumida por manual; segundo disparo omitido incluso ante contexto automático sin cierre | T084 | T043–T044 |
| Fecha UTC fija, omisión solo si vigente posterior, serialización por jugador y fallo sin reemplazo parcial | T081–T083 | T044 |
| Migraciones consecutivas sin DROP duplicado; constraints/FK reales y colisiones detectadas sin limpieza | T084 | T045 |
| `executionId` y resúmenes finalizados, HTTP sin rechazo diario manual; ranking y mercado preservados | T083, T085 | T044 |
| Entrega técnica y evidencias de integración; ejecución de pruebas reservada al usuario | T086 | T046 |

No se marcan checklists del revisor ni se atribuye aprobación nueva. Al revisar su aplicabilidad,
los criterios antiguos de «primera cotización inmutable», omisión por igualdad de fecha, ausencia
de ID contractual y automática ilimitada deben evaluarse contra esta base y las specs vigentes,
no reutilizarse como requisitos activos. T077 de SPEC-016 conserva su pendiente con SPEC-022.

**Corrección C1/L1 autorizada tras ANALYZE — 2026-11-11**: criterios y justificaciones de
`checklists/data.md` de SPEC-016 y `checklists/quality.md`/`concurrency-integrity.md` de SPEC-023
corregidos contra esta base, con antecedente y contraste documental explícitos. Marcadores e IDs
preservados, sin aprobación humana nueva. Enlaces de contratos de los checklists de SPEC-023
corregidos a `../contracts/`, incluido `scheduling-config.md`. La corrección no equivale a un
nuevo ANALYZE satisfactorio; debe verificarse en una ejecución posterior.

## 4. Uso en los dos ANALYZE

Ambas operaciones leen esta base y las dos carpetas completas. Analizar primero SPEC-016 y después
SPEC-023 en la misma sesión, sin editar documentos entre ellas. Usar el override de directorio
admitido por el script local, sin crear ni renumerar features ni cambiar de rama. Los hallazgos se
referencian cruzadamente; no corregir automáticamente una spec entre los dos diagnósticos.

**Estado**: alineación documental, no informe de ANALYZE ni validación de código. Merge pendiente
con conflictos productivos y de tests. No ejecutar build por este cambio exclusivamente documental.
Los resultados humanos anteriores de tests no acreditan el resultado combinado; Flyway sigue
pendiente de salida. CODE REVIEW, TESTING y cierre no aprobados.

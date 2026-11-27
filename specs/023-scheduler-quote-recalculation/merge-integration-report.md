# Integración SPEC-016 / SPEC-023 — 2026-11-11

Merge autorizado de `ce794c2` (`docs/016-daily-quote-recalculation-spec`) hacia
`feat/023-daily-quote-scheduling`, base publicada `7fe1d4b`.
Contrato integrado: [integration-baseline.md](integration-baseline.md).

## Cambios

- Una fila diaria conserva identidad, jugador y fecha; reemplaza cálculo completo y FK de ejecución.
  Solo se omite fecha posterior. Cada cálculo aplica la fórmula al precio vigente.
- Conservados bloqueo por jugador, contexto confirmado, cierre independiente, ranking paginado,
  `executionId` y consulta exclusiva de resúmenes finalizados.
- Manuales ilimitadas; una automática registrada por fecha UTC, incluso sin cierre. Traducción
  restringida a SQLSTATE/tabla/constraint automáticos y omisión segura del scheduler.
- Inicialización por existencia y primera fecha de cotizaciones, sin depender de origen mutable.
- Migraciones consecutivas sin DROP duplicado; FK e índice automático parcial preservados.
  Ambas estaban ausentes de `origin/dev` local al comprobarse. No se aplicaron a una base.
- Conflictos de tests resueltos; regresiones adaptadas a reemplazos, score antiguo y cuota.
  Persistencia cubre misma identidad, fórmula sobre precio anterior, nueva FK e initialize repetido.
- Arquitectura combinada; TD-024 sigue pendiente. Se conservan los demás cambios de la rama origen.

## Evidencia y límites

Build permitido `gradlew.bat build -x test` desde `backend/` satisfactorio tras corregir con
parches de fuentes los errores de formato iniciales. Validación estática de 30 migraciones y
144 clases de tests. No se ejecutaron ni compilaron tests; el build no valida funcionalidad ni SQL.

Las tareas amplias T081–T086 y T043–T046 conservan sus marcas pendientes hasta terminar la
comprobación de cobertura/evidencia exigida. T045 conserva pendiente la salida humana de Flyway:
actualizar versión/conteo no demuestra la causa del fallo. T077 de SPEC-016 permanece fuera de
este merge. Este informe complementa la evidencia histórica de ambas specs.

El usuario debe ejecutar `./gradlew test` desde `backend/`, especialmente `FlywaySchemaTest`,
`PlayerQuoteMigrationTest`, `QuoteExecutionPersistenceTest`, `QuoteMarketOperationPersistenceTest`,
`QuoteRecalculationServiceTest`, `QuoteAutomaticRecalculationFailureTest`, `QuoteQueryServiceTest`,
`QuoteMarketInitializationServiceTest`, `AutomaticQuoteRecalculationSchedulerTest` y
`QuoteControllerTest`. Si falla, aportar salida del caso afectado. Resultados previos al merge
no validan esta base integrada.

IMPLEMENT autorizado; CODE REVIEW, TESTING y cierre pendientes. Sin nueva fase ANALYZE/CONVERGE,
sin cambios de seguimiento remoto ni push del resultado. La finalización Git se verifica aparte.

## Comprobación de tareas posterior al merge — 2026-11-11

Por solicitud del usuario se contrastaron las tareas con el commit de merge `23e175c` y se
marcaron únicamente alcances técnicos comprobables, sin atribuir ejecución ni aceptación humana:

| Tarea | Evidencia inspeccionada |
| --- | --- |
| SPEC-016 T082 | `PlayerQuote.replaceCalculation`, publicación con bloqueo y `REQUIRES_NEW`, búsqueda diaria, omisión solo de fecha posterior; inicialización y guard de mercado independientes de INITIAL. |
| SPEC-016 T084 | DDL consecutivo sin DROP repetido; constraints/FK, pruebas de unicidad automática/manual, contextos sin cierre y consulta ordenada; prueba de actualización de FK en `QuoteMarketOperationPersistenceTest`. La aplicación real sigue sin ejecutar. |
| SPEC-016 T085 | Fachada/recorder, modelo y mapper de ejecución; `QuoteControllerTest.acceptsRepeatedHttpRuns`, IDs en GET y ausencia de detalles internos. |
| SPEC-023 T043 | Reserva independiente, traducción específica del índice automático, scheduler y `skipsAlreadyRegisteredAutomaticExecution`. El orden histórico de escritura prueba/código no se presenta como satisfecho: la regresión del scheduler se añadió después de la resolución productiva. |
| SPEC-023 T044 | `acceptsRepeatedRun`, rechazo automático tras cierre fallido y reintento manual; IDs HTTP y comprobación persistida de última FK. |

Pendientes conservados, sin duplicar su estado fuera de los respectivos `tasks.md`:

- T081: falta completar la comprobación/cobertura explícita de fallo sin reemplazo parcial y de
  cambio de score/estrategia en la misma fecha; los escenarios existentes no bastan para declarar
  todo ese alcance cumplido.
- T083: falta cerrar el contraste de lecturas vigente/historial/fecha y fuente por lote después
  del reemplazo del mismo día. La prueba persistida añadida no demuestra por sí sola esas APIs.
- T045: versión/conteo actualizados; falta salida del fallo anterior de Flyway para diagnosticarlo.
- T086/T046: entrega parcial documentada y build observado, pero sus prerrequisitos amplios siguen
  abiertos. T077 conserva su coordinación pendiente con SPEC-022.

Se pueden ejecutar ahora los tests existentes con Docker disponible desde `backend/`:
`./gradlew.bat test`. Se solicita salida de los fallos, especialmente Flyway. Un resultado verde
no sustituye los escenarios pendientes de cobertura ni autoriza cierre. Esta operación solo
actualiza documentación: no nuevo build, tests, commit, push ni cambio de seguimiento remoto.

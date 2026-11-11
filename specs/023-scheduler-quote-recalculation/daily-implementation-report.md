# Informe de IMPLEMENT — delta diario de SPEC-023

## Resultado y límites

T034–T042 implementadas bajo autorización explícita del usuario, incluida su confirmación del ajuste
del test diario (T034). T042 es la última tarea funcional/documental del alcance. No se ejecutaron
tests, fases posteriores, handoffs, commits, push ni escrituras en GitHub. El informe histórico
`implementation-report.md` se conserva sin reescribir su evidencia.

Base revisable: diff local de los archivos enumerados abajo, sobre la rama
`feat/023-daily-quote-scheduling`; autorización persistida en `sdd-gates.md`. Los cambios SDD previos
en spec, plan, diseño, contratos y checklists ya estaban presentes y se preservaron. `spec.md`
conserva `Ready for implementation`; no se declara cierre.

## Code Review Report

### Funcionamiento

- El cron por defecto es `0 0 12 * * *`, diario a las 12:00 UTC; sigue configurable.
- La reserva de corrida conserva `saveAndFlush`. La migración retira la unicidad global por fecha y
  crea `uq_quote_executions_automatic_date` solo para `AUTOMATIC`: las manuales no consumen esa cuota.
- El publisher calcula como antes y busca por jugador-fecha sin origen. Si encuentra una fila,
  `replaceCalculation` sustituye todos los datos del cálculo, preservando ID, jugador y fecha.
  La restricción `uq_player_quote` garantiza cardinalidad diaria sin distinguir origen.
- Se conserva la omisión de reprecio sin valuación posterior. No se cambian fórmulas ni fecha de
  vigencia de una cotización recalculada, que sigue siendo la fecha de valuación existente.
- La consulta existente por fecha devuelve todas las corridas; DTO, Mapper, seguridad y rutas
  mantienen su forma. No se exponen IDs internos nuevos.

### Inventario completo del cambio de esta ejecución

Rutas relativas al repositorio. En las dos primeras tablas, el prefijo común es
`backend/src/main/java/footballmarket/features/quote/` o
`backend/src/test/java/footballmarket/features/quote/`, respectivamente.

| Archivo productivo modificado | Responsabilidad y motivo |
| --- | --- |
| `infraestructure/config/QuoteProperties.java` | Default diario en `Schedule` |
| `application/internal/QuotePlayerPublisher.java` | `publish`: localizar y actualizar fila existente antes de flush |
| `domain/models/PlayerQuote.java` | `replaceCalculation`: validar jugador/fecha y sustituir cálculo completo; habilitar actualización JPA de esos campos |
| `infraestructure/PlayerQuoteRepository.java` | Consulta `findByPlayerIdAndEffectiveDate` sin filtro de origen |
| `presentation/QuoteController.java` | OpenAPI: manuales repetibles y consulta múltiple; retirar rechazo diario del POST manual |
| `domain/models/QuoteExecution.java` | Javadoc coherente con cardinalidad por origen, sin cambios de ejecución |
| `application/impl/QuoteRecalculationServiceImpl.java` | Javadoc coherente; el recorrido transaccional no cambia |

| Test modificado | Responsabilidad y motivo |
| --- | --- |
| `infraestructure/config/QuotePropertiesTest.java` | Exigir cron diario; conservar override y cron inválido |
| `presentation/QuoteControllerTest.java` | Lista de dos manuales y una automática; retirar expectativa HTTP obsoleta de fecha consumida |
| `application/QuoteRecalculationServiceTest.java` | Identidades independientes de corridas manuales; coexistencia y rechazo automático; reemplazo de arrastrada conservando ID y datos del último cálculo |
| `infraestructure/QuoteExecutionPersistenceTest.java` | Manuales múltiples y unicidad automática, también entre inserciones concurrentes |
| `infraestructure/PlayerQuoteMigrationTest.java` | Unicidad jugador-fecha independiente de origen; admisión en otra fecha |
| `application/QuoteAutomaticRecalculationFailureTest.java` | Coherencia de regresión: una automática con fallos admite manual y rechaza otra automática |
| `application/QuoteQueryServiceTest.java` | Historial sin duplicado diario y último instante; fixture de ranking compatible con nueva unicidad |

| Otro archivo | Cambio y motivo |
| --- | --- |
| `backend/src/main/resources/application.yml` | Default diario configurable |
| `backend/src/main/resources/db/migration/V20261111163841__allow_daily_quote_recalculation.sql` (nuevo) | Generado con `gradlew.bat createMigration`; guardia de colisiones, índice automático parcial y unicidad jugador-fecha |
| `specs/016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md` | Contrato normativo alineado con manuales múltiples y fila diaria única |
| `docs/backend/architecture.md` §1.8 | T042: cardinalidad por origen, actualización diaria, cron y ausencia de lock global |
| `docs/TECHNICAL_DEBT.md` TD-024 | Registrar impacto acotado sin declarar resuelta la deuda transaccional |
| `specs/023-scheduler-quote-recalculation/tasks.md` | Marcar implementación y separar verificación humana pendiente |
| `specs/023-scheduler-quote-recalculation/sdd-gates.md` | Persistir ampliación autorizada, inicio y resultado observado |
| `specs/023-scheduler-quote-recalculation/daily-implementation-report.md` (nuevo) | Este informe completo y checklist humano |

### Decisiones, riesgos y puntos de inspección

1. Revisar primero el SQL: no modifica migraciones anteriores ni elimina filas. Antes de cambiar
   restricciones, detecta colisiones heredadas y aborta. No se ejecutó contra datos reales. Ante
   colisiones, el usuario debe decidir su resolución; no se selecciona automáticamente una fila.
2. Revisar `PlayerQuote.replaceCalculation` y sus columnas JPA: ID/jugador/fecha no cambian; el
   argumento es otra cotización validada por su constructor. Se copian precio anterior, multiplicador,
   score, estrategia, versión, fecha fuente, origen e instante junto con el precio nuevo.
3. Revisar `QuotePlayerPublisher.publish`: cálculo anterior, búsqueda diaria y actualización están
   dentro de la atomicidad por jugador ya existente (`REQUIRES_NEW`). Sin nueva exclusión global.
4. Revisar contrato/OpenAPI y tests HTTP: la respuesta conserva el contrato actual sin agregar IDs
   internos. El límite diario ahora solo corresponde a la vía automática.
5. Revisar todos los tests de las tablas, especialmente las expectativas históricas reemplazadas.
   El uso operativo evita recálculos simultáneos; no se promete concurrencia general manual/automática.
6. TD-024 sigue pendiente: las cotizaciones confirmadas por jugador no se compensan si falla el
   resumen. El reemplazo diario no convierte esas transacciones en una operación atómica global.

### Verificación observada

- Prerrequisitos: script real invocado mediante `bash`, con FEATURE_DIR de SPEC-023 y tareas presentes.
- Cinco checklists: 64 ítems marcados y 0 pendientes; no se modificaron sus marcadores.
- `.specify/extensions.yml` no existe: sin hooks pre/post que despachar.
- Build permitido: `gradlew.bat build -x test`, satisfactorio. Incluye compilación productiva,
  formato, validación de nombres/contenido de migraciones y clasificación estática de tests; no
  demuestra ejecución funcional ni aplicación del SQL. Fallos intermedios de formato corregidos.
- Tests no ejecutados ni compilados por el agente. No se levantaron servicios ni se accedió a DB.
- Revisión de ignore existente: cubre `.gradle/`, `build/`, clases, jars, logs y secretos; no se
  introdujo empaquetado Docker, publicación npm ni nuevas herramientas que requieran ignores.

### Checklist humano pendiente

- [ ] Revisé los archivos creados/modificados, incluidos todos los tests.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé las decisiones importantes y el tratamiento de colisiones de la migración.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar a TESTING.

Este checklist no está aprobado. Su presentación documenta la entrega de IMPLEMENT; no inicia
otra fase ni concede un handoff.

## Pruebas escritas y pendientes de ejecución humana

| Cobertura | Tests / assertions relevantes |
| --- | --- |
| FR-001, SC-001 / US1 | `QuotePropertiesTest.bindsDefaults`: default diario; casos existentes de override y arranque inválido conservados |
| FR-005 / US2 | `QuoteControllerTest.returnsMultipleDailyExecutions`: tres elementos, orígenes, fecha, resultado, motivo cerrado y ausencia de ID interno |
| FR-006, FR-015, SC-004, SC-010 / US3 | `persistsRepeatedManualRuns`, `limitsOnlyAutomaticExecution`: IDs distintos, consulta de todas las corridas y rechazo de segunda automática |
| FR-014, SC-009 / US3 | `recomputedQuoteUpdatesCarriedForward`: mismo ID, precio/multiplicador calculados y último instante/estrategia; `QuoteQueryServiceTest` relee historial con una sola fila diaria |
| Restricciones de persistencia | `QuoteExecutionPersistenceTest`: manuales antes/después de automática; segunda automática rechazada y concurrencia automática; `PlayerQuoteMigrationTest`: rechaza mismo jugador-fecha con otro origen |
| Fallos y reintentos | `QuoteAutomaticRecalculationFailureTest`: manual aceptada tras automática con fallos; resto del comportamiento previo conservado |

Mocks: Controller sustituye Services y validador de identidad; mantiene MVC y cadena de seguridad.
Config usa `ApplicationContextRunner`; el test de cron inválido sustituye solo el Service para el
disparador. Service y persistencia usan PostgreSQL real de Testcontainers y sus fixtures existentes,
con relojes controlados; no se sustituyó el repositorio bajo prueba.

Puntos delicados: la assertion del precio usa la fórmula existente, por lo que es una comprobación
de transporte del cálculo, no una validación independiente de la fórmula financiera. La cardinalidad
persistida se comprueba adicionalmente por consulta y constraint. No hay garantía de ejecución de
estos casos hasta recibir resultados humanos, ni prueba de colisiones sobre la base del usuario.

Orden recomendado de ejecución humana desde `backend/` (PowerShell; Docker disponible para integración):

```powershell
.\gradlew.bat lightweightTest --tests 'footballmarket.features.quote.infraestructure.config.QuotePropertiesTest*' --tests 'footballmarket.features.quote.presentation.QuoteControllerTest*'
.\gradlew.bat integrationTest --tests 'footballmarket.features.quote.application.QuoteRecalculationServiceTest*' --tests 'footballmarket.features.quote.application.QuoteAutomaticRecalculationFailureTest*' --tests 'footballmarket.features.quote.application.QuoteQueryServiceTest*' --tests 'footballmarket.features.quote.infraestructure.QuoteExecutionPersistenceTest*' --tests 'footballmarket.features.quote.infraestructure.PlayerQuoteMigrationTest*'
```

Estado: **pendiente de validación humana**, no TESTING aprobado. La comprobación y aplicación real de
la migración también están pendientes; no ejecutar limpiezas automáticas ante colisiones.

## Estado SDD y seguimiento

- Feature: SPEC-023; fase entregada: IMPLEMENT, alcance T034–T042.
- Status de spec: `Ready for implementation` (sin declarar `Approved`).
- IMPLEMENT AUTHORIZATION: explícita y persistida para el alcance.
- CODE REVIEW, TESTING y FEATURE COMPLETION AUTHORIZATION: pendientes.
- Seguimiento remoto: no verificado ni modificado; sincronización PENDING. No se afirma transición
  operativa a `In Review` ni cierre.
- Pendientes: revisión humana del diff/informe y ejecución humana de las pruebas. Ninguna fase
  posterior se ejecutó en esta sesión.

# Resultado parcial de IMPLEMENT — SPEC-024, fases 1 y 2

## Estado

Fase 1 comprobada sin inicialización. T001–T002 completadas; T003–T004 parciales. IMPLEMENT se detiene por los pendientes siguientes; no se declara el alcance listo para CODE REVIEW ni se ejecutan fases posteriores. La spec permanece Draft; el seguimiento observado es In Progress, sin escrituras remotas. Autorización recibida y persistida en [implement-preparation.md](implement-preparation.md); CODE REVIEW, TESTING y cierre pendientes.

## Cambios y funcionamiento

Las rutas de los archivos Java siguientes parten de `backend/src/`:

| Archivo | Responsabilidad y cambio |
| --- | --- |
| `main/java/footballmarket/features/statistics/application/PlayerStatisticsSynchronizationService.java` | Añade `synchronizeJsonScheduled` y `synchronizeScrapingScheduled`; retornan un booleano de parcialidad, sin contadores. Corrige el Javadoc obsoleto de límites manuales. Conserva firmas manuales. |
| `main/java/footballmarket/features/statistics/application/exceptions/ScheduledStatisticsSyncException.java` (nuevo) | Excepción de aplicación con `Reason` cerrado: proveedor no disponible, navegador no disponible, interrupción o estado fallido/interrumpido. No incorpora mensajes arbitrarios ni secretos. |
| `main/java/footballmarket/features/statistics/application/shared/impl/PlayerStatisticsSynchronizationServiceImpl.java` | Manual y automático comparten recorridos privados. El modo automático conserva la causa de interrupción JSON y usa `closeRun` para seleccionar el cierre automático; no incorpora scheduler ni cambia fechas, persistencia o contadores manuales. |
| `main/java/footballmarket/features/statistics/application/shared/internal/ExtractionRunManager.java` | `closeScheduled` reutiliza el cierre transaccional y solo después lanza la señal segura. Prioriza el código global sobre el estado; por ello un parcial no oculta `PROVIDER_UNAVAILABLE`. `FAILED` e `INTERRUPTED` se traducen cuando no hay código conocido. |
| `test/java/footballmarket/features/statistics/application/shared/PlayerStatisticsSynchronizationServiceTest.java` | Once escenarios nuevos en `ScheduledSynchronization`, con Service y persistencia reales, `@SpringBootTest`, perfil test y PostgreSQL/Testcontainers existentes. JSON usa un worker para orden determinista y respuestas de Integration controladas. |

También se actualizan `tasks.md` (avance y pendientes), `implement-preparation.md` (autorización recibida) y `docs/TECHNICAL_DEBT.md` (TD-037). Los otros artefactos locales y el diff previo de `spec.md` se conservan; no son implementación de esta ejecución. No hay cambios HTTP, dependencias, migraciones, frontend ni programación semanal. Los ignores existentes cubren los artefactos efectivos; no se requieren nuevos.

## Tests escritos y límites

- JSON completo y scraping completo: retorno sin parcialidad, confirmaciones conservadas y posterior entrada manual sin pendientes.
- Fallo global JSON antes de confirmar (429) y tras una confirmación (403): motivo seguro `PROVIDER_UNAVAILABLE`.
- Entrada manual JSON: mantiene resumen `PARTIAL`, contadores y pendientes ante el mismo corte global.
- JSON y scraping parciales por fallo individual: continúan sin convertirlo en fallo global.
- JSON y scraping sin éxitos: traducción a `FAILED`.
- Interrupción JSON tras confirmar y scraping interrumpido: motivo `EXECUTION_INTERRUPTED`.

Se sustituyen exclusivamente Integrations externas y configuración de workers; no se sustituyen Services, repositorios ni componentes internos del recorrido. Assertions principales: motivo exacto de excepción, booleano de parcialidad y resumen manual de relanzamiento. No se afirma haber ejecutado estos escenarios. El build no compiló las fuentes de test; compilación y ejecución quedan pendientes.

Cobertura trazada: FR-003 y FR-004 (señales), FR-006 y FR-007 (preservación manual y confirmaciones observables), FR-009 (sin umbral ni contadores automáticos). No se acredita cobertura completa de estos requisitos ni de las US: faltan las fases posteriores.

## Bloqueantes y siguiente decisión

1. **TD-037:** excepción inesperada JSON puede dejar un run `RUNNING` y obstaculizar la entrada manual durante el presupuesto de 24 horas. Evidencia estática preexistente y propuesta registradas en el documento de deuda; sin corrección autorizada específica ni prueba ejecutada. Hace falta acordar el tratamiento y actualizar el desglose afectado, seguido de ANALYZE antes de reanudar lo afectado. No se modifica silenciosamente el contrato manual.
2. **T004 pendiente:** falta demostrar `PROVIDER_UNAVAILABLE` con `COMPLETED` desde la frontera permitida. `JsonStatisticsUpdateServiceImpl` registra un item fallido antes de relanzar el error de Integration, por lo que los Arrange escritos producen `PARTIAL` o `INTERRUPTED`. No se concluye imposibilidad universal, no se elimina el caso del contrato ni se declara cubierto. Hace falta aclarar un Arrange válido o revisar upstream el criterio, con ANALYZE si cambia materialmente.

### Actualización posterior de preparación — 2026-11-05

- Las observaciones anteriores describen el estado histórico de la implementación parcial. El plan y T003–T004 incorporan ahora el tratamiento de TD-037 en el recorrido automático y la regresión de disponibilidad manual. **TD-037 y T003–T004 siguen pendientes de implementación y verificación**; no se declara corregida la deuda.
- Por decisión del usuario, T004 ya no exige fabricar una prueba de `PROVIDER_UNAVAILABLE` con `COMPLETED` para el recorrido que registra un item fallido. Se conservan la protección contractual ante fallo global y las pruebas deterministas reproducibles desde la fachada; no se afirma que esa combinación sea universalmente imposible ni que haya sido probada.
- El ANALYZE posterior a esos ajustes reportó **sin bloqueos de preparación** y una observación no bloqueante sobre esa limitación de verificación. Esto permite evaluar la reanudación de IMPLEMENT, no acredita que los fundamentos estén terminados. Antes de reanudar, comprobar la vigencia de las autorizaciones persistidas frente al plan y las tareas modificados, y resolver cualquier autorización afectada; no inferir aprobación de revisión, testing ni cierre.

## Verificación observada

- Prerrequisito real: `check-prerequisites.sh --json --require-tasks --include-tasks`, correcto; directorio absoluto de SPEC-024.
- Checklists leídas sin modificaciones: requirements 16/16, general 8/8, contracts-security 4/4.
- Sin `.specify/extensions.yml`: hooks previos y posteriores no aplicables.
- `gradlew.bat build -x test`: primer intento con fallo de formato introducido, corregido manualmente; segundo intento `BUILD SUCCESSFUL`. Compilación Java y clasificación estática de 141 clases correctas; advertencia de API obsoleta en el archivo de sincronización, que ya usaba `Thread.getId` antes del cambio.
- Tests no ejecutados; sin commit, push, CONVERGE ni aprobación humana inferida.

Para que el usuario compile y ejecute los tests escritos, desde `backend/`, con JDK 21 y Docker disponibles:

```powershell
.\gradlew.bat integrationTest --tests 'footballmarket.features.statistics.application.shared.PlayerStatisticsSynchronizationServiceTest*'
```

## Orden de inspección y checklist humano (sin gate solicitado)

Revisar primero el contrato de fachada y `Reason`, después `closeScheduled`, los recorridos compartidos y finalmente los once tests. Inspeccionar especialmente el commit transaccional anterior a la señal, la prioridad código/estado, el alcance exclusivo del modo automático y los pendientes anteriores.

- [ ] Revisé todos los archivos creados/modificados, incluidos los tests.
- [ ] Entiendo los cambios y sus motivos.
- [ ] Revisé las decisiones y los límites de cobertura.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Una vez resueltos los pendientes y emitido el informe completo, evaluaré la aprobación para avanzar a TESTING.

Esta lista no acredita revisión ni solicita pasar de fase con implementación incompleta.

## Entrega posterior de IMPLEMENT — 2026-11-05

Las secciones anteriores conservan el estado histórico. Tras la corrección upstream del plan y T003–T004, ANALYZE posterior sin bloqueantes y solicitud contextual de reanudación, se completan los fundamentos pendientes, US1 y US2 y su coherencia arquitectónica. Los pendientes de implementación descritos arriba no son el estado actual: la limitación de T004 quedó acordada upstream y el tratamiento automático de TD-037 está implementado, con tests escritos y aún no ejecutados. La deuda permanece En curso, sin resolución declarada.

Resultado actual: T001–T012 implementadas y revisadas estáticamente; build sin tests satisfactorio, spec Draft y gates humanos pendientes. GitHub Projects observado In Progress; no se actualiza el remoto y queda pendiente su sincronización a revisión. No hay cierre, CONVERGE ni handoff. El informe completo vigente está en [code-review-report.md](code-review-report.md) y la cobertura y comandos humanos en [testing-report.md](testing-report.md).

### Corrección P2 — 2026-11-06

El usuario identificó y autorizó corregir el doble fallo scraping/cierre oculto en los logs. Se corrige únicamente la entrada automática y se añaden dos regresiones de Service y una de logs del scheduler. El contrato manual se conserva. Detalle vigente en los anexos P2 de los informes de revisión/testing y en TD-038 (En curso, no resuelta sin evidencia humana). No se ejecutan tests ni fases posteriores.

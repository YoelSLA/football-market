# Informe de IMPLEMENT — Endpoints de métricas de negocio

- **Feature:** `035-business-metrics-endpoints`
- **Rama observada:** `feat/035-business-metrics-endpoints`
- **Fecha:** 2026-11-26
- **Alcance ejecutado:** exclusivamente US2, «Consultar métricas de la ejecución actual» (T005–T008).
- **Estado de spec:** `Ready for implementation`; no se ejecutó cierre ni fase posterior.

## Autorización IMPLEMENT

- **Autorizó:** usuario solicitante en esta conversación.
- **Alcance autorizado:** US1 (T001–T004), US2 (T005–T008) y, mediante autorización explícita posterior, US3 (T009–T010).
- **Evidencia:** autorización de US2 del 2026-11-26 registrada en este informe; autorización explícita de US3 «autorizo a implementar US3», recibida en esta conversación el 2026-11-26 y persistida aquí antes de implementar US3. La autorización anterior de US1 permanece en `analyze-report.md`.
- **Límites:** la autorización actual cubre exclusivamente US3 y sus tareas T009–T010; no autoriza CODE REVIEW, TESTING o CONVERGE.
- **Preparación US3:** `analyze-report.md` informa ANALYZE sin blockers; ambas checklists están completas.

## Resultado

- Añadido `GET /api/business-metrics/current`, que devuelve el DTO común con los acumulados Micrometer y el instante UTC de consulta.
- Definidos contadores de cardinalidad fija, sin tags de identidad, para compras, ventas, publicaciones canceladas, tokens y créditos.
- Las fachadas de compra, liquidación P2P, recompra oficial y cancelación publican eventos internos tras completar la operación. El listener los aplica a Micrometer únicamente en `AFTER_COMMIT`, por lo que rechazos y transacciones revertidas no incrementan los contadores.
- La clasificación de ejecución distingue compras (compra oficial y P2P) de ventas (recompra oficial); cada operación agrega una vez sus tokens e importe.
- Los contadores son locales al proceso y Micrometer los inicializa de nuevo al arrancar una nueva ejecución.
- T005–T008 quedaron marcadas como completadas en `tasks.md`. El usuario informó posteriormente que pasaron las cuatro clases de prueba solicitadas para ejecución; la autorización de TESTING sigue pendiente.
- T009–T010 de US3 se completaron en esta fase; la ejecución de la nueva clase de pruebas queda pendiente del usuario.

## Resultado US3

- Se documentaron ambos endpoints OpenAPI con autenticación Bearer, acceso para cualquier cuenta autenticada, fuente de los agregados, unidades, instante UTC, contrato de respuesta compartido y exclusión de identidades individuales.
- Se ampliaron las descripciones OpenAPI del DTO para definir los campos, tipos/volúmenes, centésimas de crédito y formato UTC.
- Se añadió `BusinessMetricsControllerTest`, que cubre ambos contratos JSON exactos y equivalentes, el rechazo 401 sin credenciales, acceso de cuenta regular y superusuario, y las rutas protegidas/campo de unidad en OpenAPI.
- T009 y T010 quedan completadas en `tasks.md`. Los tests no se ejecutaron; `BusinessMetricsControllerTest` queda pendiente para ejecución del usuario.
- No se cambió la especificación, contrato ni checklists, ni se alteró el avance fuera de US3.

## Archivos de US3

- `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java` — anotaciones OpenAPI de ambas rutas.
- `backend/src/main/java/footballmarket/features/businessmetrics/presentation/dtos/response/BusinessMetricsResponseDTO.java` — significado de campos y unidades en OpenAPI.
- `backend/src/test/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsControllerTest.java` — contrato, autenticación y documentación OpenAPI.
- `specs/035-business-metrics-endpoints/tasks.md` — tareas T009–T010.
- `specs/035-business-metrics-endpoints/implementation-report.md` — autorización y resultado de US3.

## Code Review Report — US3, pendiente de aprobación humana

**Resumen funcional:** US3 asegura las dos rutas autenticadas, el contrato común de seis campos y la exposición exclusivamente agregada. El endpoint permite cuenta regular y superusuario; sin credenciales devuelve 401.

| Archivo | Responsabilidad y cambio | Motivo |
|---|---|---|
| `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java` | Añade detalle de propósito, autenticación, acceso, contrato, unidades y límites a las dos operaciones OpenAPI. | Cumple FR-008 y documenta uniformemente ambas rutas. |
| `backend/src/main/java/footballmarket/features/businessmetrics/presentation/dtos/response/BusinessMetricsResponseDTO.java` | Explica cada campo, incluidos conteos, volúmenes, centésimas e instante UTC. | Hace explícito el significado del contrato común. |
| `backend/src/test/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsControllerTest.java` | Verifica esquema exacto/equivalencia, datos agregados, 401, dos clases de cuenta y OpenAPI. | Aporta la cobertura de Controller requerida para US3. |
| `specs/035-business-metrics-endpoints/tasks.md` | Marca T009–T010 realizadas, dejando pendiente la ejecución humana de tests. | Mantiene el avance de tareas alineado con el trabajo realizado. |
| `specs/035-business-metrics-endpoints/implementation-report.md` | Persiste la autorización y el resultado de US3. | Evidencia durable de alcance y entrega de IMPLEMENT. |

**Orden de revisión:** Controller y DTO/OpenAPI; después el test de contrato/autenticación/documentación; finalmente T009–T010 y la evidencia de autorización.

**Checklist humana:**
- [ ] Revisé todos los archivos de US3 enumerados arriba.
- [ ] Confirmo el contrato JSON exacto y común en ambas rutas.
- [ ] Confirmo que ambas rutas exigen autenticación y no requieren rol adicional.
- [ ] Confirmo que OpenAPI documenta autenticación, rutas, estructura y significado de campos.
- [ ] Confirmo ausencia de identidades individuales en la respuesta y documentación.
- [ ] No detecté comportamiento fuera de la especificación.
- [ ] Apruebo avanzar a TESTING.

No se solicita ni registra aprobación en nombre del usuario.

## Verificación de US3

- **Tests:** no ejecutados por el agente. Solicitar al usuario ejecutar `./gradlew lightweightTest --tests footballmarket.features.businessmetrics.presentation.BusinessMetricsControllerTest` desde `backend/`.
- **Build backend:** `./gradlew build -x test` desde `backend/` — SUCCESS. No ejecutó tests; sí ejecutó validación de migraciones, formato, clasificación estática, compilación de producción y empaquetado.
- **Diff:** `git diff --check` — sin errores.
- **CODE REVIEW:** pendiente de aprobación humana.
- **TESTING:** pendiente.

### Corrección tras resultado de prueba aportado por el usuario

- El usuario reportó que `BusinessMetricsControllerTest` fallaba al preparar el test porque `@WebMvcTest` no proveía un bean `ObjectMapper`.
- Se eliminó la inyección de ese bean y el test ahora instancia un `ObjectMapper` local, utilizado solo para analizar los bodies recibidos. No cambia código productivo ni el alcance de US3.
- La clase corregida no fue ejecutada por el agente. El usuario debe volver a ejecutar el comando indicado arriba; la corrección no se registra como verificada y TESTING continúa pendiente.
- En la ejecución humana posterior, solo falló `documentsBothProtectedRoutes`: `/v3/api-docs` no está registrado en el contexto `@WebMvcTest` y responde 404. El test ahora inspecciona directamente las anotaciones `@Operation`, `@SecurityRequirement` y `@Schema` de Controller/DTO, sin asumir que el slice inicia Springdoc.
- Tras esta corrección, `./gradlew build -x test` terminó `SUCCESS`; no ejecuta tests y no verifica la prueba corregida. El test sigue pendiente de ejecución del usuario.
- `BusinessMetricsServiceTest` usa `ResetPlayerCatalogListener` antes de cada método para aislar las consultas históricas con el mismo reset de la base Testcontainers que ya utilizan otras pruebas del backend. Esto evita que la compra confirmada por un método altere los totales esperados por otro.
- El usuario informó el 2026-11-26 que pasaron las cuatro clases de prueba indicadas para ejecutar (la clase de métricas y las de compra, recompra y publicaciones). Esta evidencia se registra como resultado aportado por el usuario; el agente no ejecutó tests.

## Archivos incorporados o modificados por IMPLEMENT

### Producción

- `backend/src/main/java/footballmarket/features/businessmetrics/application/BusinessMetricsService.java` — incorpora la consulta de métricas actuales.
- `backend/src/main/java/footballmarket/features/businessmetrics/application/BusinessMetricTransactionEvent.java` — comunica datos agregados de una operación financiera, sin identidad.
- `backend/src/main/java/footballmarket/features/businessmetrics/application/BusinessMetricListingCancelledEvent.java` — señala una cancelación efectiva.
- `backend/src/main/java/footballmarket/features/businessmetrics/application/impl/BusinessMetricsServiceImpl.java` — define los nombres fijos de métricas y lee los contadores Micrometer.
- `backend/src/main/java/footballmarket/features/businessmetrics/application/impl/BusinessMetricsRecorder.java` — incrementa contadores tras el commit.
- `backend/src/main/java/footballmarket/features/businessmetrics/presentation/BusinessMetricsController.java` — expone la ruta actual junto a la histórica ya existente.
- `backend/src/main/java/footballmarket/features/market/application/impl/BuyOrderServiceImpl.java` — publica eventos de compras directas y P2P completadas.
- `backend/src/main/java/footballmarket/features/market/application/impl/OfficialBuybackServiceImpl.java` — publica eventos de recompras oficiales completadas.
- `backend/src/main/java/footballmarket/features/market/application/impl/TokenListingServiceImpl.java` — publica el evento de cancelación efectiva.

### Pruebas backend añadidas o ampliadas

- `backend/src/test/java/footballmarket/features/businessmetrics/application/BusinessMetricsServiceTest.java` — agregados actuales, clasificación, volúmenes y rechazo sin incremento.
- `backend/src/test/java/footballmarket/features/market/application/BuyOrderServiceTest.java` — incremento por compra y volúmenes.
- `backend/src/test/java/footballmarket/features/market/application/OfficialBuybackServiceTest.java` — incremento por venta oficial y volúmenes.
- `backend/src/test/java/footballmarket/features/market/application/TokenListingServiceTest.java` — incremento por cancelación efectiva.

### Artefacto SDD

- `specs/035-business-metrics-endpoints/tasks.md` — refleja el trabajo completado de US2, sin cambiar el avance de US1 o US3.
- `specs/035-business-metrics-endpoints/implementation-report.md` — persiste la autorización de US2 y este resultado.
- `bruno/collection/business-metrics/folder.bru` y sus solicitudes `get-historical-metrics.bru` / `get-current-metrics.bru` — agregadas a pedido del usuario para ejecutar ambos endpoints manualmente.

El árbol de trabajo ya contenía cambios de SPEC-035 antes de IMPLEMENT (incluidos archivos de US1 y artefactos de planificación/análisis). Se preservaron; no se atribuyen a esta ejecución ni se revirtieron.

## Decisiones y orden recomendado de revisión

1. Revisar primero `BusinessMetricTransactionEvent`, `BusinessMetricListingCancelledEvent` y `BusinessMetricsRecorder`, especialmente el registro `AFTER_COMMIT` y la ausencia de tags.
2. Revisar las publicaciones de eventos en las tres fachadas de mercado y la lectura de contadores en `BusinessMetricsServiceImpl`.
3. Revisar el nuevo mapping y su DTO común en `BusinessMetricsController`.
4. Revisar los casos de métricas en `BusinessMetricsServiceTest` y las ampliaciones de pruebas de compra, recompra y cancelación.
5. Revisar T005–T008 y la persistencia de esta autorización en el presente informe.

## Code Review Report — pendiente de aprobación humana

Esta entrega contiene el alcance completo de US2 y las pruebas correspondientes. La revisión humana debe comprobar:

- [ ] Revisé los archivos creados/modificados del alcance US2.
- [ ] Entiendo qué cambió y por qué.
- [ ] Revisé el límite transaccional: solo se cuentan operaciones confirmadas.
- [ ] Revisé que ambas compras se clasifiquen como compra y la recompra oficial como venta.
- [ ] Revisé la ausencia de dimensiones personales o tags de alta cardinalidad.
- [ ] No detecté comportamiento fuera de la spec.
- [ ] Apruebo avanzar a TESTING.

No se solicita ni se registra aprobación en nombre del usuario.

## Verificación y pendientes

- **Build backend:** `./gradlew build -x test` desde `backend/` — SUCCESS. Ejecutó compilación de producción, formato, validación de clasificación estática y validación de migraciones; no ejecutó tests.
- **Tests de aplicación:** el agente no los ejecutó. El usuario confirmó que pasaron `BusinessMetricsServiceTest`, `BuyOrderServiceTest`, `OfficialBuybackServiceTest` y `TokenListingServiceTest`; no se aportó confirmación sobre la clase repository `BusinessMetricsRepositoryTest`.
- **CODE REVIEW:** pendiente.
- **TESTING:** pendiente.
- **Seguimiento operativo:** no verificado ni modificado; no se afirma sincronización.
- No se ejecutaron fases posteriores ni handoffs.

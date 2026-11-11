# Feature Specification: Programar recálculo automático de cotizaciones

**Feature Branch**: `feat/023-daily-quote-scheduling`

## Decisión vigente de integración con SPEC-016 — 2026-11-11

**Autor**: usuario de la sesión activa, tras comparar ambas implementaciones: «vayamos con la regla de la spec023» y «a mi solo me importa conservar un precio por fecha», autorizando combinar ambas especificaciones.

Esta decisión sustituye las reglas incompatibles de los bloques anteriores y de la revisión de
SPEC-016 del 2026-11-10, sin eliminar su evidencia histórica:

- **FR-001 / SC-001**: programación diaria configurable a las **12:00 UTC** por defecto.
- **FR-006 / FR-015**: corridas manuales ilimitadas y como máximo una automática registrada por
  fecha UTC. Una manual no consume la cuota automática. Otro disparo automático de esa fecha se
  omite con registro operativo; se conserva `EXECUTION_ALREADY_EXISTS` para ese conflicto automático.
- **FR-002 / FR-003**: ambas entradas reutilizan las reglas financieras diarias de SPEC-016:
  último score persistido disponible de la estrategia, aunque no sea nuevo; sin score se arrastra
  el precio. No se recalculan scores ni se duplican fórmulas en el scheduler.
- **FR-014 / SC-009**: una sola fila por jugador y fecha UTC de cotización, sin distinguir origen
  ni estrategia. Si una ejecución posterior vuelve a calcular ese jugador y fecha, actualiza la
  fila existente con el último cálculo válido, preservando identidad, jugador y fecha. La mera
  existencia de la fila diaria no causa omisión. No se conserva cada precio intermedio intradiario.
- La fecha de vigencia es la fecha UTC fijada al iniciar la corrida; la fecha del score se conserva
  aparte como auditoría. Una corrida de una fecha anterior a la cotización vigente no sobrescribe
  una fecha posterior; la igualdad de fecha sí permite actualización.
- La fila diaria referencia la ejecución que produjo su último cálculo. Al actualizar precio,
  estrategia, origen e instante, se actualiza también esa referencia. Cada corrida conserva su
  propio resumen y parámetros; esto no exige almacenar versiones de cada precio intradiario.
- SPEC-016 conserva el cálculo, persistencia, consultas y ranking paginado; SPEC-023 conserva la
  programación y el límite automático. No se crea otra spec ni se fusionan sus identidades.
- El scheduler sigue sin compensar cotizaciones ni recuperar transacciones; no se declara resuelta
  TD-024 ni se añade exclusión global de corridas como requisito.

**Aceptación integrada**: dos manuales de la misma fecha tienen resúmenes independientes y una sola
cotización diaria del jugador con el último cálculo válido y su ejecución de origen; manuales antes
y después de una automática siguen permitidas; una segunda automática se omite sin cotizar. Las
consultas diaria y vigente no deben elegir entre filas duplicadas. Se conserva el ajuste diario con
score antiguo y el ranking de SPEC-016.

**Impacto SDD**: deben reconciliarse planes, modelos, contratos, research, quickstarts, tareas y
cobertura de ambas specs con esta decisión, y repetirse ANALYZE sobre la base combinada antes de
resolver funcionalmente el código del merge. Los análisis y resultados de tests anteriores no
acreditan la integración; no se aprueban CODE REVIEW, TESTING ni cierre. El usuario informa tests
satisfactorios salvo Flyway, cuyo diagnóstico sigue pendiente de salida. La autorización de combinar
no se interpreta como aceptación funcional ni como autorización de cierre.

Base conjunta de diseño y cobertura para ambos ANALYZE: [`integration-baseline.md`](integration-baseline.md).
Los apartados funcionales siguientes reflejan esta integración; la revisión del 2026-11-10 se
conserva expresamente como antecedente, no como requisito activo.

## Revisión de la rama SPEC-016 — 2026-11-10 (antecedente superado por la decisión de integración)

**Status vigente del cambio**: Draft. Solo preparación documental; IMPLEMENT AUTHORIZATION: NONE. Este bloque sustituye las reglas anteriores incompatibles y conserva el resto como antecedente.

- **FR-001 / SC-001**: Programación diaria en UTC, configurable, en lugar de semanal. Se conserva como supuesto la hora 00:00 UTC; CLARIFY debe confirmar que no se requiere cambiarla.
- **FR-002 / FR-003**: Reutilizar el núcleo de SPEC-016 actualizado: aplicar diariamente el último score disponible aunque no sea nuevo; mantener el precio si no existe score para la estrategia. El scheduler no calcula scores ni duplica reglas financieras.
- **FR-006**: Admitir varias ejecuciones por fecha, manuales o automáticas, sin omitir el disparo únicamente porque exista una ejecución previa. La unicidad es de cotización por jugador y día UTC, garantizada por SPEC-016, no de ejecución por fecha. No se incorpora exclusión global adicional.
- **FR-004 / FR-005**: Cada corrida conserva identidad, origen, fecha y resultado diferenciables; consultar por fecha permite distinguir las múltiples ejecuciones. Los errores siguen registrándose sin compensación a cargo del scheduler.

**Aceptación US1–US3**: dos días consecutivos con el mismo score producen el ajuste diario; sin score se conserva el precio; una ejecución manual posterior a la automática el mismo día es admitida pero no recotiza jugadores ya procesados; los pendientes pueden completarse sin duplicados. Se conservan mercado inicializado, seguridad, trazabilidad y responsabilidades de recuperación existentes.

**Impacto SDD**: sustituidas las aclaraciones, escenarios, supuestos y criterios que exigen frecuencia semanal, unicidad de ejecución diaria o rechazo de reintento con resumen previo. Plan, configuración contractual, modelo, research, quickstart, tasks y checklists afectados pendientes de revisión; ANALYZE previo no vigente para el delta. Autorizaciones históricas no cubren el cambio; CODE REVIEW, TESTING y cierre pendientes para este alcance, sin borrar su evidencia.

**Validación de SPECIFY**: alcance, escenarios y límites definidos; hora diaria pendiente de confirmar en CLARIFY. Los checklists históricos del revisor no se marcan para el delta. Issue #40 CLOSED e item Done observados para el alcance anterior; seguimiento del cambio PENDING, sin escritura remota.

**Evidencia**: script real con `--reuse`, `FEATURE_NUM=023`, `SPEC_ACTION=reused`, `BRANCH_STATUS=skipped-dirty-tree`; no cambio de rama ni descarte de trabajo. CLARIFY pendiente; no se acredita preparación completa.

---

## Base anterior — conservada para trazabilidad

**Feature Branch histórica**: `feat/023-scheduler-quote-recalculation`

**Created**: 2026-11-03

**Status**: Draft

**Input**: Programar recálculo automático de cotizaciones (issue #40, requisito REQ-S2-11-B). El sistema ya calcula y persiste cotizaciones con historial trazable mediante ejecución manual (SPEC-016) sobre valuaciones estadísticas (SPEC-013). Esta feature añade la ejecución periódica y desatendida del recálculo, sin modificar la lógica financiera existente.

## Clarifications

### Session 2026-11-03

- Q: ¿Con qué cadencia y en qué zona horaria debe dispararse el recálculo automático? → A: Semanal, lunes a las 00:00 UTC, con expresión configurable. Se evalúa siempre en UTC, de modo que no requiere tratamiento especial de horario estacional.
- Q: ¿De dónde debe tomar la ejecución automática la estrategia de valuación y los parámetros de ajuste? → A: La estrategia se lee de una variable de configuración del mercado; si el administrador no la modifica, se mantiene la estrategia allí definida. El resto de parámetros se toman de la misma configuración del mercado.
- Q: ¿Qué debe hacer un disparo automático cuando ya existe una ejecución para esa fecha UTC? → A: El disparo duplicado se omite y deja constancia en el registro operativo. Ante un fallo se registra el error; los efectos de persistencia y reintento corresponden al recálculo existente.
- Q: Cuando la programación dispara y el mercado no está inicializado, ¿dónde debe quedar registrado el intento? → A: Rechazo controlado, sin alterar cotizaciones ni persistir una ejecución para la fecha; el intento queda en el registro operativo con su motivo y la fecha queda libre para un reintento manual tras la inicialización.
- Q: ¿Dónde debe registrarse y consultarse el resultado de una ejecución automática, y cuánto tiempo debe conservarse? → A: En el registro persistido de ejecuciones existente, ampliado con el origen automático o manual, consultable por fecha mediante el mecanismo de consulta de ejecuciones ya existente. La retención es indefinida, junto con el historial de cotizaciones.

### Session 2026-11-04

- Q: ¿Qué responsabilidad tiene el scheduler frente a errores o efectos del recálculo? → A: Solo dispara el caso de uso de recálculo existente y registra el error si la invocación falla. La gestión de transacciones, persistencia, integridad, recuperación y reintentos corresponde al recálculo existente, no al scheduler. La ejecución automática reutiliza las mismas reglas que la manual; no se modifica el comportamiento del endpoint ni se exige que el scheduler lo invoque por HTTP.
- Q: ¿Puede el administrador ejecutar manualmente después de un intento automático? → A: Si el intento automático finalizó sin dejar una ejecución registrada, el administrador puede reintentar manualmente el mismo día UTC, conforme a SPEC-016. Si ya quedó una ejecución registrada, incluso con fallos individuales, se rechaza otra para esa fecha; el equipo acepta este riesgo. No se establece un máximo de dos intentos sin registro. El scheduler no compensa cotizaciones ni recupera fallos.
- Q: ¿Se exige exclusión global de recálculos en paralelo? → A: No. Se retira esa exigencia adicional y se conserva únicamente la restricción por fecha existente en SPEC-016; no se añade un bloqueo global.
- Q: Cuando una corrida automática falla por completo después de que algunos jugadores ya confirmaron su cotización, ¿debe el disparador automático revertir o compensar esas cotizaciones? → A: No. La ejecución automática no es responsable de las cotizaciones que ya quedaron confirmadas por jugadores individuales: compensarlas excede su alcance. Ese comportamiento es heredado de SPEC-016 y está registrado como TD-024; esta feature no lo modifica, no lo compensa y no lo declara resuelto.

### Session 2026-11-11

- Q: Si una ejecución vuelve a calcular la cotización de un jugador que ya tiene una cotización registrada para ese mismo día UTC, ¿qué debe ocurrir con esa cotización? → A: Actualizar la fila existente con el último cálculo, sin crear otra.
- Q: ¿A qué hora y en qué zona horaria debe ejecutarse el recálculo diario por defecto? → A: Todos los días a las 12:00 UTC (mediodía UTC), manteniendo la programación configurable.
- Q: Si ya hay una ejecución registrada para una fecha UTC, ¿debe permitirse otra ejecución manual o automática en esa misma fecha? → A: Permitir cualquier cantidad de ejecuciones manuales en una fecha; el scheduler automático se ejecuta como máximo una vez por fecha.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recálculo periódico sin intervención (Priority: P1)

Como responsable del mercado, quiero que las cotizaciones se recalculen automáticamente según una programación definida, para que sus valores se mantengan actualizados sin que nadie tenga que disparar el proceso a mano.

**Why this priority**: es el valor central del issue. Sin ejecución programada no existe la feature; el resto de historias protegen o hacen observable esa ejecución.

**Independent Test**: se puede verificar configurando una programación de prueba, ejecutando más de un recálculo en una fecha UTC y comprobando que cada corrida permitida queda registrada, que cada jugador mantiene una sola cotización para ese día y que esta refleja el último cálculo, sin intervención para iniciar la corrida automática.

**Acceptance Scenarios**:

1. **Given** una programación configurada (por defecto, todos los días a las 12:00 UTC) y el mercado operativo, **When** llega el momento programado, **Then** el recálculo se inicia automáticamente y produce cotizaciones equivalentes a las de una ejecución manual con los mismos insumos.
2. **Given** una programación configurada, **When** la ejecución termina, **Then** no se requirió ninguna acción de un operador para iniciarla ni para completarla.
3. **Given** una fecha UTC con una ejecución automática ya registrada, **When** el scheduler vuelve a disparar para esa misma fecha, **Then** el disparo automático se omite sin crear otra ejecución, y queda constancia operativa del intento.
4. **Given** el mecanismo de programación en funcionamiento, **When** debe programarse otro proceso batch, **Then** puede reutilizar el mismo mecanismo sin duplicar la infraestructura de programación.

---

### User Story 2 - Trazabilidad de cada ejecución (Priority: P2)

Como responsable del mercado, quiero poder identificar cada ejecución automática y su resultado, para saber qué ocurrió en cada corrida y detectar desvíos sin revisar detalles internos.

**Why this priority**: sin registro identificable no se puede operar ni auditar el proceso; el issue lo exige explícitamente (RF-03, RF-05).

**Independent Test**: se puede verificar provocando ejecuciones con resultados distintos y comprobando que cada una deja un registro diferenciable con origen, fecha, resultado y alcance.

**Acceptance Scenarios**:

1. **Given** una ejecución automática completada, **When** el responsable consulta su registro por los medios operativos existentes, **Then** identifica que fue automática, cuándo ocurrió, qué alcance procesó y si terminó con éxito o fallo.
2. **Given** una ejecución automática fallida, **When** el responsable consulta su registro, **Then** el registro permite distinguir el fallo sin exponer trazas ni detalles internos.
3. **Given** una ejecución automática que falla, **When** el scheduler recibe el error, **Then** el error queda en el registro operativo; el comportamiento del recálculo y sus reintentos permanece a cargo del caso de uso existente.

---

### User Story 3 - Errores y rechazos del recálculo (Priority: P3)

Como responsable del mercado, quiero conocer los errores o rechazos del recálculo automático, sin que el scheduler asuma responsabilidades propias del recálculo existente.

**Why this priority**: permite detectar problemas de la ejecución desatendida.

**Independent Test**: se puede verificar haciendo que el caso de uso existente rechace o falle y comprobando que el scheduler registre el resultado, sin aplicar compensación ni modificar las reglas del recálculo.

**Acceptance Scenarios**:

1. **Given** que el caso de uso de recálculo falla, **When** el scheduler recibe el error, **Then** deja constancia del fallo en el registro operativo, sin modificar ni compensar las cotizaciones.
2. **Given** una fecha UTC con ejecuciones previas, **When** el administrador solicita otro recálculo manual ese mismo día, **Then** la ejecución manual se permite y queda registrada de forma independiente.
3. **Given** el mercado no inicializado, **When** la programación dispara, **Then** no se altera ninguna cotización existente, no se persiste una ejecución para ese intento y el intento queda registrado en el registro operativo con su motivo.

---

### Edge Cases

- El scheduler automático ya se ejecutó en la fecha UTC: cualquier nuevo disparo automático de esa fecha se omite y deja constancia operativa; las solicitudes manuales adicionales sí se ejecutan.
- Una ejecución posterior vuelve a calcular un jugador cotizado ese mismo día UTC: se actualiza la fila diaria existente, sin duplicarla.
- El mercado no está inicializado al momento del disparo: el intento queda en el registro operativo y la fecha sigue disponible para reintento manual.
- Una ejecución falla: el scheduler registra el error y no altera los efectos ni la recuperación del recálculo existente.
- La programación se evalúa en UTC fija, por lo que no hay cambio de horario estacional que desplace el disparo.
- No hay score para la estrategia: arrastrar el precio. Si existe un score antiguo, aplicar el cálculo diario de SPEC-016; su antigüedad no impide publicar ni actualizar la fila del día. El scheduler no añade reglas financieras.

## Requirements *(mandatory)*

### Functional Requirements

Los identificadores FR-007 y FR-008 se retiraron durante la revisión; no se reutilizan para preservar trazabilidad.

- **FR-001**: El sistema MUST ejecutar el recálculo de cotizaciones automáticamente conforme a una programación configurable, sin intervención de un operador. La cadencia por defecto MUST ser diaria a las 12:00 UTC y MUST poder ajustarse por configuración. La programación MUST evaluarse en UTC.
- **FR-002**: El sistema MUST aplicar a la ejecución automática las mismas reglas de valuación, estrategia y parámetros que la ejecución manual. La estrategia MUST leerse de una variable de configuración del mercado; mientras el administrador no la modifique, MUST usarse la estrategia allí definida. Los parámetros de ajuste MUST provenir de la misma configuración del mercado.
- **FR-003**: El sistema MUST NOT definir, duplicar ni modificar fórmulas de valuación; la lógica financiera existente es la única fuente de verdad.
- **FR-004**: El sistema MUST registrar cada ejecución automática completada en el registro persistido de ejecuciones existente, con, al menos, identificador único, fecha y hora en UTC, origen automático o manual, resultado (éxito o fallo), alcance procesado y motivo de fallo cuando corresponda. Un fallo completo se registra operativamente conforme a FR-013. El resultado y el motivo de fallo MUST poder derivarse de forma determinista de los datos persistidos de la ejecución, sin depender de columnas duplicadas.
- **FR-005**: El sistema MUST permitir identificar y consultar por fecha todos los resúmenes finalizados con `executionId`, por inicio e ID descendentes, mediante la consulta existente y sin ruta histórica nueva. Un contexto sin cierre no se presenta como éxito ni como resumen finalizado. La retención de registros MUST ser indefinida junto con las cotizaciones diarias.
- **FR-006**: El sistema MUST permitir cualquier cantidad de ejecuciones manuales en una fecha UTC, registrando cada una independientemente. El scheduler MUST ejecutar como máximo una corrida automática por fecha UTC y MUST omitir y registrar operativamente cualquier disparo automático adicional de esa fecha, incluso si también hubo ejecuciones manuales.
- **FR-009**: La ejecución automática MUST completarse sin requerir ninguna acción de un operador.
- **FR-010**: El resultado MUST NOT incluir trazas, mensajes técnicos ni identificadores internos ajenos al contrato. `executionId` es la identidad pública contractual de la corrida, compartida con SPEC-016, no un detalle interno accidental.
- **FR-011**: La infraestructura de programación MUST quedar planteada para reutilizarse en otros procesos batch, en particular #41, sin implementar #41 en esta feature.
- **FR-012**: Cuando el mercado no esté inicializado, el sistema MUST rechazar la ejecución de forma controlada, MUST NOT alterar cotizaciones existentes ni persistir una ejecución para ese intento, y MUST registrar el intento en el registro operativo con su motivo. Las ejecuciones manuales posteriores MUST seguir permitidas tras la inicialización.
- **FR-013**: Si la invocación automática del recálculo falla, el scheduler MUST registrar el error en el registro operativo. La persistencia, integridad, compensación, recuperación y disponibilidad de reintentos corresponden al caso de uso existente de SPEC-016 y no son responsabilidad adicional del scheduler; TD-024 permanece pendiente.
- **FR-014**: Otra ejecución del mismo jugador y fecha UTC MUST actualizar la única fila con el último cálculo válido y su referencia de ejecución, precio anterior, multiplicador, score, estrategia/versión, fecha fuente, origen e instante; MUST preservar ID, jugador y fecha y MUST NOT crear duplicados ni versiones intradiarias. La fecha de vigencia es el día UTC fijado al iniciar corrida, no la fecha del score. Sin score se arrastra; score antiguo no impide calcular. Si la fecha vigente es posterior a la de la corrida, omitir sin alterar ese historial; igualdad permite reemplazar.
- **FR-015**: El registro de ejecuciones MUST admitir múltiples registros para una misma fecha UTC e identificar cada corrida de forma independiente, sin limitar las ejecuciones manuales por fecha.

### Key Entities *(include if feature involves data)*

- **Programación de recálculo**: define cuándo debe ejecutarse el proceso. Atributos conceptuales: cadencia diaria por defecto, a las 12:00 UTC, ajustable por configuración; zona de evaluación UTC; estado habilitado o deshabilitado; estrategia de valuación configurada.
- **Ejecución de recálculo**: representa una corrida concreta. Atributos conceptuales: identificador, origen automático o manual, fecha UTC, inicio, fin, resultado y alcance procesado.
- **Resultado de ejecución**: describe el desenlace de una corrida. Atributos conceptuales: éxito o fallo, cantidad de cotizaciones procesadas y motivo de fallo apto para exposición externa. Se conserva de forma indefinida en el registro de ejecuciones.
- **Registro operativo**: evidencia estructurada de disparos omitidos, rechazos por mercado no inicializado y fallos completos de una corrida. Documenta el intento, pero no sustituye ni duplica el registro persistido de ejecuciones.
- **Cotización**: valor publicado para un jugador. Para cada jugador y fecha UTC se conserva una sola fila; una ejecución posterior del mismo día actualiza esa fila con el último cálculo, sin modificar las reglas de valuación.
- **Valuación estadística**: insumo del recálculo. Entidad existente; esta feature no modifica su obtención ni sus fórmulas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

Los criterios se verifican por resultado observable (cumple o no cumple) en los escenarios definidos. No se imponen tasas, porcentajes ni umbrales estadísticos.

- **SC-001**: Una ejecución programada se inicia conforme a la programación configurada; por defecto, todos los días a las 12:00 UTC.
- **SC-002**: Cada ejecución automática completada deja un registro persistido identificable con origen, fecha UTC, resultado y alcance procesado; un fallo completo se registra operativamente.
- **SC-003**: Si el caso de uso falla durante una invocación automática, el scheduler registra el error sin compensar ni modificar por su cuenta las cotizaciones.
- **SC-004**: El administrador puede solicitar y registrar cualquier cantidad de ejecuciones manuales en la misma fecha UTC, aunque ya existan ejecuciones automáticas o manuales.
- **SC-005**: La ejecución programada se completa de principio a fin sin intervención de un operador.
- **SC-006**: Los resultados no contienen trazas, mensajes técnicos ni IDs internos no contractuales; cada resumen finalizado expone su `executionId` contractual.
- **SC-007**: Un disparo automático adicional omitido porque ya hubo una ejecución automática en esa fecha deja constancia operativa.
- **SC-008**: Ante un fallo del caso de uso, la ejecución automática deja constancia operativa del error; la persistencia y los reintentos conservan el comportamiento existente del recálculo.
- **SC-009**: Tras dos o más ejecuciones en una fecha UTC, cada jugador recalculado tiene como máximo una fila de cotización para ese día, cuyo valor corresponde al último cálculo realizado.
- **SC-010**: El sistema registra todas las ejecuciones manuales solicitadas en una misma fecha UTC; de las corridas automáticas registra como máximo una por fecha UTC y deja constancia operativa de disparos automáticos adicionales omitidos.

## Assumptions

- SPEC-013 (#38) y SPEC-016 (#39) están cerradas y su comportamiento está disponible, según confirmación del equipo del 2026-11-04; la actualización de su campo `Status` para reflejar ese cierre queda pendiente en los artefactos correspondientes.
- La integración usa el núcleo diario de SPEC-016 y debe verificar su compatibilidad con la entrada automática; no se presume correcta una combinación de ramas sin revisar ni probar. Fórmulas y ranking permanecen bajo responsabilidad de SPEC-016.
- Se permiten ejecuciones manuales ilimitadas en una fecha UTC. El scheduler automático realiza como máximo una ejecución por fecha UTC; disparos automáticos adicionales se omiten y registran operativamente.
- La cadencia es diaria a las 12:00 UTC y es ajustable por configuración; al evaluarse en UTC no hay horario estacional que desplazar.
- La estrategia y los parámetros de la ejecución automática se definen por configuración y el administrador puede cambiarlos sin modificar la lógica de negocio ni las fórmulas de valuación.
- Existe un entorno de ejecución desatendida disponible; su naturaleza técnica corresponde a PLAN.
- El registro de ejecuciones se consulta mediante el mecanismo existente por fecha; no se crea una interfaz ni una consulta histórica nueva.
- Los registros de ejecución se conservan de forma indefinida junto con el historial de cotizaciones; no se define una purga.
- La sincronización automática de datos deportivos (#41) queda fuera de alcance.
- La retención del registro operativo sigue la configuración vigente de logs; no se exige retención indefinida para esos registros.
- No se añade exclusión global entre recálculos concurrentes.
- El uso operativo no inicia dos recálculos a la vez ni dispara el scheduler mientras otro recálculo está en curso; esta expectativa operativa no constituye una garantía técnica de exclusión mutua de SPEC-023.
- La compensación de las cotizaciones ya confirmadas por jugadores individuales cuando falla el guardado del resumen queda fuera del alcance de la ejecución automática. Es un límite heredado de SPEC-016, registrado como TD-024, y esta feature no lo resuelve ni lo declara resuelto.

## Decisiones de CLARIFY

Las decisiones abiertas del issue #40 se registran en [Clarifications](#clarifications). La sesión 2026-11-04 acota la responsabilidad del scheduler ante fallos (TD-024 permanece pendiente). La sesión 2026-11-11 establece una programación diaria a las 12:00 UTC, permite ejecuciones manuales ilimitadas y una corrida automática como máximo por fecha, y actualiza la fila de cotización del jugador existente con el último cálculo del día. Estas decisiones reemplazan las decisiones anteriores incompatibles sobre cadencia y unicidad de ejecuciones; el plan y el análisis previos requieren revisión.

## Trazabilidad

- **Issue**: #40 — `SPEC-023 | Programar recálculo automático de cotizaciones`
- **Requerimiento**: `REQ-S2-11-B`
- **Dependencias cerradas**: SPEC-013 (#38, valuaciones estadísticas) y SPEC-016 (#39, recálculo manual de cotizaciones).
- **Fuera de alcance**: sincronización automática de datos deportivos (#41), fórmulas de valuación, consultas históricas nuevas, reglas de compra/venta, comportamiento del mercado e interfaz de usuario.

| Requisito del issue | Requisitos funcionales |
| --- | --- |
| RF-01 | FR-001, FR-011 |
| RF-02 | FR-002, FR-003 |
| RF-03 | FR-002, FR-004, FR-006 |
| RF-04 | FR-006, FR-012, FR-013 |
| RF-05 | FR-004, FR-005, FR-010 |
| RF-06 | FR-004 y supuesto de ejecución manual operativa (SPEC-016) |
| Bloque 2: corridas múltiples y cotización diaria única | FR-006, FR-014, FR-015 |

| Criterio de aceptación | Requisitos funcionales |
| --- | --- |
| CA-01 | FR-001, FR-011 |
| CA-02 | FR-002, FR-003 |
| CA-03 | FR-002, FR-004, FR-006 |
| CA-04 | FR-006, FR-012, FR-013 |
| CA-05 | FR-004, FR-005, FR-010 |
| CA-06 | Supuesto: ejecución manual operativa (SPEC-016), fuera de verificación |

# Feature Specification: Programar recálculo automático de cotizaciones

**Feature Branch**: `feat/023-daily-quote-scheduling`

**Created**: 2026-11-03

**Status**: Ready for implementation

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
- No hay valuaciones disponibles o actualizadas al momento del disparo: se conserva el comportamiento existente de SPEC-016 (arrastrar el precio cuando no hay valuación y no repreciar cuando no hay valuación posterior a la referencia). Este caso no añade un requisito nuevo a SPEC-023.

## Requirements *(mandatory)*

### Functional Requirements

Los identificadores FR-007 y FR-008 se retiraron durante la revisión; no se reutilizan para preservar trazabilidad.

- **FR-001**: El sistema MUST ejecutar el recálculo de cotizaciones automáticamente conforme a una programación configurable, sin intervención de un operador. La cadencia por defecto MUST ser diaria a las 12:00 UTC y MUST poder ajustarse por configuración. La programación MUST evaluarse en UTC.
- **FR-002**: El sistema MUST aplicar a la ejecución automática las mismas reglas de valuación, estrategia y parámetros que la ejecución manual. La estrategia MUST leerse de una variable de configuración del mercado; mientras el administrador no la modifique, MUST usarse la estrategia allí definida. Los parámetros de ajuste MUST provenir de la misma configuración del mercado.
- **FR-003**: El sistema MUST NOT definir, duplicar ni modificar fórmulas de valuación; la lógica financiera existente es la única fuente de verdad.
- **FR-004**: El sistema MUST registrar cada ejecución automática completada en el registro persistido de ejecuciones existente, con, al menos, identificador único, fecha y hora en UTC, origen automático o manual, resultado (éxito o fallo), alcance procesado y motivo de fallo cuando corresponda. Un fallo completo se registra operativamente conforme a FR-013. El resultado y el motivo de fallo MUST poder derivarse de forma determinista de los datos persistidos de la ejecución, sin depender de columnas duplicadas.
- **FR-005**: El sistema MUST permitir identificar y consultar el registro de cada ejecución por fecha mediante el mecanismo de consulta de ejecuciones ya existente, sin introducir una consulta histórica nueva. La retención de esos registros MUST ser indefinida, junto con el historial de cotizaciones.
- **FR-006**: El sistema MUST permitir cualquier cantidad de ejecuciones manuales en una fecha UTC, registrando cada una independientemente. El scheduler MUST ejecutar como máximo una corrida automática por fecha UTC y MUST omitir y registrar operativamente cualquier disparo automático adicional de esa fecha, incluso si también hubo ejecuciones manuales.
- **FR-009**: La ejecución automática MUST completarse sin requerir ninguna acción de un operador.
- **FR-010**: El resultado expuesto de una ejecución MUST NOT incluir trazas, mensajes técnicos ni identificadores internos.
- **FR-011**: La infraestructura de programación MUST quedar planteada para reutilizarse en otros procesos batch, en particular #41, sin implementar #41 en esta feature.
- **FR-012**: Cuando el mercado no esté inicializado, el sistema MUST rechazar la ejecución de forma controlada, MUST NOT alterar cotizaciones existentes ni persistir una ejecución para ese intento, y MUST registrar el intento en el registro operativo con su motivo. Las ejecuciones manuales posteriores MUST seguir permitidas tras la inicialización.
- **FR-013**: Si la invocación automática del recálculo falla, el scheduler MUST registrar el error en el registro operativo. La persistencia, integridad, compensación, recuperación y disponibilidad de reintentos corresponden al caso de uso existente de SPEC-016 y no son responsabilidad adicional del scheduler; TD-024 permanece pendiente.
- **FR-014**: Cuando otra ejecución recalcule la cotización de un jugador que ya tiene una cotización para la misma fecha UTC, el sistema MUST actualizar la fila existente con el último cálculo y MUST NOT crear una fila duplicada para ese jugador y día.
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
- **SC-006**: Los resultados expuestos no contienen trazas, mensajes técnicos ni identificadores internos.
- **SC-007**: Un disparo automático adicional omitido porque ya hubo una ejecución automática en esa fecha deja constancia operativa.
- **SC-008**: Ante un fallo del caso de uso, la ejecución automática deja constancia operativa del error; la persistencia y los reintentos conservan el comportamiento existente del recálculo.
- **SC-009**: Tras dos o más ejecuciones en una fecha UTC, cada jugador recalculado tiene como máximo una fila de cotización para ese día, cuyo valor corresponde al último cálculo realizado.
- **SC-010**: El sistema registra todas las ejecuciones manuales solicitadas en una misma fecha UTC; de las corridas automáticas registra como máximo una por fecha UTC y deja constancia operativa de disparos automáticos adicionales omitidos.

## Assumptions

- SPEC-013 (#38) y SPEC-016 (#39) están cerradas y su comportamiento está disponible, según confirmación del equipo del 2026-11-04; la actualización de su campo `Status` para reflejar ese cierre queda pendiente en los artefactos correspondientes.
- La ejecución manual de recálculo (SPEC-016) funciona correctamente y se asume operativa; esta especificación no la modifica ni la verifica.
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

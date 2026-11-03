# Feature Specification: Programar recálculo automático de cotizaciones

**Feature Branch**: `feat/023-scheduler-quote-recalculation`

**Created**: 2026-11-03

**Status**: Ready for planning

**Input**: Programar recálculo automático de cotizaciones (issue #40, requisito REQ-S2-11-B). El sistema ya calcula y persiste cotizaciones con historial trazable mediante ejecución manual (SPEC-016) sobre valuaciones estadísticas (SPEC-013). Esta feature añade la ejecución periódica y desatendida del recálculo, sin modificar la lógica financiera existente.

## Clarifications

### Session 2026-11-03

- Q: ¿Con qué cadencia y en qué zona horaria debe dispararse el recálculo automático? → A: Semanal, lunes a las 00:00 UTC, con expresión configurable. Se evalúa siempre en UTC, de modo que no requiere tratamiento especial de horario estacional.
- Q: ¿De dónde debe tomar la ejecución automática la estrategia de valuación y los parámetros de ajuste? → A: La estrategia se lee de una variable de configuración del mercado; si el administrador no la modifica, se mantiene la estrategia allí definida. El resto de parámetros se toman de la misma configuración del mercado.
- Q: ¿Qué debe hacer un disparo automático cuando ya existe una ejecución para esa fecha UTC o cuando otro disparo sigue en curso? → A: Solo puede existir una ejecución por fecha y nunca dos en paralelo; el disparo duplicado o solapado se omite sin escribir cotizaciones ni una segunda ejecución, dejando constancia en el registro operativo. Si la corrida falla por completo, no se persiste ejecución para esa fecha, se registra el error operativo y el administrador puede reejecutar manualmente ese mismo día.
- Q: Cuando la programación dispara y el mercado no está inicializado, ¿dónde debe quedar registrado el intento? → A: Rechazo controlado, sin alterar cotizaciones ni persistir una ejecución para la fecha; el intento queda en el registro operativo con su motivo y la fecha queda libre para un reintento manual tras la inicialización.
- Q: ¿Dónde debe registrarse y consultarse el resultado de una ejecución automática, y cuánto tiempo debe conservarse? → A: En el registro persistido de ejecuciones existente, ampliado con el origen automático o manual, consultable por fecha mediante el mecanismo de consulta de ejecuciones ya existente. La retención es indefinida, junto con el historial de cotizaciones.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recálculo periódico sin intervención (Priority: P1)

Como responsable del mercado, quiero que las cotizaciones se recalculen automáticamente según una programación definida, para que sus valores se mantengan actualizados sin que nadie tenga que disparar el proceso a mano.

**Why this priority**: es el valor central del issue. Sin ejecución programada no existe la feature; el resto de historias protegen o hacen observable esa ejecución.

**Independent Test**: se puede verificar configurando una programación de prueba, esperando su disparo y comprobando que se generaron cotizaciones nuevas con las mismas reglas que la ejecución manual, sin intervención de un operador.

**Acceptance Scenarios**:

1. **Given** una programación configurada (por defecto, lunes a las 00:00 UTC) y el mercado operativo, **When** llega el momento programado, **Then** el recálculo se inicia automáticamente y produce cotizaciones equivalentes a las de una ejecución manual con los mismos insumos.
2. **Given** una programación configurada, **When** la ejecución termina, **Then** no se requirió ninguna acción de un operador para iniciarla ni para completarla.
3. **Given** una fecha UTC con una ejecución de recálculo registrada, **When** la programación vuelve a disparar para esa misma fecha, **Then** el disparo se omite sin crear una segunda ejecución ni escribir cotizaciones, y queda constancia operativa del intento.
4. **Given** el mecanismo de programación en funcionamiento, **When** debe programarse otro proceso batch, **Then** puede reutilizar el mismo mecanismo sin duplicar la infraestructura de programación.

---

### User Story 2 - Trazabilidad de cada ejecución (Priority: P2)

Como responsable del mercado, quiero poder identificar cada ejecución automática y su resultado, para saber qué ocurrió en cada corrida y detectar desvíos sin revisar detalles internos.

**Why this priority**: sin registro identificable no se puede operar ni auditar el proceso; el issue lo exige explícitamente (RF-03, RF-05).

**Independent Test**: se puede verificar provocando ejecuciones con resultados distintos y comprobando que cada una deja un registro diferenciable con origen, fecha, resultado y alcance.

**Acceptance Scenarios**:

1. **Given** una ejecución automática completada, **When** el responsable consulta su registro por los medios operativos existentes, **Then** identifica que fue automática, cuándo ocurrió, qué alcance procesó y si terminó con éxito o fallo.
2. **Given** una ejecución automática fallida, **When** el responsable consulta su registro, **Then** el registro permite distinguir el fallo sin exponer trazas ni detalles internos.
3. **Given** una ejecución automática que falla por completo, **When** el proceso termina, **Then** no queda una ejecución persistida para esa fecha, el error queda en el registro operativo y el administrador puede reintentar manualmente ese mismo día.

---

### User Story 3 - Integridad ante fallos y solapamientos (Priority: P3)

Como responsable del mercado, quiero que una falla o dos ejecuciones simultáneas no dejen cotizaciones a medias, para poder confiar en los valores publicados y en el historial.

**Why this priority**: protege la integridad del dato financiero; es condición para que la automatización sea adoptable.

**Independent Test**: se puede verificar induciendo un fallo a mitad de ejecución y disparando dos ejecuciones concurrentes, comprobando que no quedan cotizaciones parciales ni historial incompleto.

**Acceptance Scenarios**:

1. **Given** una ejecución que falla antes de completar, **When** el proceso termina, **Then** las cotizaciones válidas previas se conservan y no quedan cotizaciones ni historial parciales.
2. **Given** dos disparos coincidentes, **When** ambos intentan ejecutarse, **Then** no se produce una ejecución superpuesta ni un resultado inconsistente.
3. **Given** el mercado no inicializado, **When** la programación dispara, **Then** no se altera ninguna cotización existente, no se persiste una ejecución para la fecha y el intento queda registrado en el registro operativo con su motivo.

---

### Edge Cases

- La fecha UTC programada ya tiene una ejecución registrada: el disparo se omite sin escribir cotizaciones ni una segunda ejecución.
- El mercado no está inicializado al momento del disparo: el intento queda en el registro operativo y la fecha sigue disponible para reintento manual.
- Una ejecución falla después de haber escrito parte de su alcance.
- Una ejecución falla por completo antes de completar: no queda ejecución persistida para la fecha y el administrador puede reintentar manualmente ese mismo día.
- La programación se evalúa en UTC fija, por lo que no hay cambio de horario estacional que desplace el disparo.
- No hay valuaciones disponibles o actualizadas al momento del disparo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST ejecutar el recálculo de cotizaciones automáticamente conforme a una programación configurable, sin intervención de un operador. La cadencia por defecto MUST ser semanal, lunes a las 00:00 UTC, y MUST poder ajustarse por configuración. La programación MUST evaluarse en UTC.
- **FR-002**: El sistema MUST aplicar a la ejecución automática las mismas reglas de valuación, estrategia y parámetros que la ejecución manual. La estrategia MUST leerse de una variable de configuración del mercado; mientras el administrador no la modifique, MUST usarse la estrategia allí definida. Los parámetros de ajuste MUST provenir de la misma configuración del mercado.
- **FR-003**: El sistema MUST NOT definir, duplicar ni modificar fórmulas de valuación; la lógica financiera existente es la única fuente de verdad.
- **FR-004**: El sistema MUST registrar cada ejecución automática en el registro persistido de ejecuciones existente, con, al menos, identificador único, fecha y hora en UTC, origen automático o manual, resultado (éxito o fallo), alcance procesado y motivo de fallo cuando corresponda.
- **FR-005**: El sistema MUST permitir identificar y consultar el registro de cada ejecución por fecha mediante el mecanismo de consulta de ejecuciones ya existente, sin introducir una consulta histórica nueva. La retención de esos registros MUST ser indefinida, junto con el historial de cotizaciones.
- **FR-006**: El sistema MUST garantizar que exista como máximo una ejecución por fecha UTC y que nunca haya dos ejecuciones en paralelo. Un disparo automático que encuentre una ejecución ya registrada para la fecha o una ejecución en curso MUST omitirse sin escribir cotizaciones ni una segunda ejecución, y MUST dejar constancia del intento en el registro operativo.
- **FR-007**: El sistema MUST garantizar que una ejecución fallida no deje cotizaciones parciales ni historial incompleto, conservando las cotizaciones válidas previas.
- **FR-008**: El sistema MUST respetar la restricción existente de como máximo una ejecución de recálculo por fecha UTC, definida en SPEC-016, y aplicarla también a la ejecución automática.
- **FR-009**: La ejecución automática MUST completarse sin requerir ninguna acción de un operador.
- **FR-010**: El resultado expuesto de una ejecución MUST NOT incluir trazas, mensajes técnicos ni identificadores internos.
- **FR-011**: La infraestructura de programación MUST quedar planteada para reutilizarse en otros procesos batch, en particular #41, sin implementar #41 en esta feature.
- **FR-012**: Cuando el mercado no esté inicializado, el sistema MUST rechazar la ejecución de forma controlada, MUST NOT alterar cotizaciones existentes ni persistir una ejecución para la fecha, y MUST registrar el intento en el registro operativo con su motivo. La fecha MUST quedar disponible para un reintento manual tras la inicialización.
- **FR-013**: Una corrida automática que falla por completo MUST NOT persistir una ejecución para la fecha, MUST registrar el error en el registro operativo, MUST conservar las cotizaciones válidas previas y MUST dejar la fecha disponible para un reintento manual ese mismo día.

### Key Entities *(include if feature involves data)*

- **Programación de recálculo**: define cuándo debe ejecutarse el proceso. Atributos conceptuales: cadencia semanal por defecto, lunes a las 00:00 UTC, ajustable por configuración; zona de evaluación UTC; estado habilitado o deshabilitado; estrategia de valuación configurada.
- **Ejecución de recálculo**: representa una corrida concreta. Atributos conceptuales: identificador, origen automático o manual, fecha UTC, inicio, fin, resultado y alcance procesado.
- **Resultado de ejecución**: describe el desenlace de una corrida. Atributos conceptuales: éxito o fallo, cantidad de cotizaciones procesadas y motivo de fallo apto para exposición externa. Se conserva de forma indefinida en el registro de ejecuciones.
- **Registro operativo**: evidencia estructurada de disparos omitidos, rechazos por mercado no inicializado y fallos completos de una corrida. Documenta el intento, pero no sustituye ni duplica el registro persistido de ejecuciones.
- **Cotización**: valor publicado para un jugador. Entidad existente; esta feature no modifica sus reglas de cálculo.
- **Valuación estadística**: insumo del recálculo. Entidad existente; esta feature no modifica su obtención ni sus fórmulas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

Los criterios se verifican por resultado observable (cumple o no cumple) en los escenarios definidos. No se imponen tasas, porcentajes ni umbrales estadísticos.

- **SC-001**: Una ejecución programada se inicia conforme a la programación configurada; por defecto, el lunes a las 00:00 UTC.
- **SC-002**: Cada ejecución automática deja un registro identificable con origen, fecha UTC, resultado y alcance procesado.
- **SC-003**: Una ejecución fallida no deja cotizaciones parciales ni historial incompleto, y conserva las cotizaciones válidas previas.
- **SC-004**: Dos disparos concurrentes no producen ejecuciones superpuestas ni resultados inconsistentes.
- **SC-005**: La ejecución programada se completa de principio a fin sin intervención de un operador.
- **SC-006**: Los resultados expuestos no contienen trazas, mensajes técnicos ni identificadores internos.
- **SC-007**: Un disparo duplicado o solapado no crea una segunda ejecución ni modifica cotizaciones, y deja constancia operativa del intento.
- **SC-008**: Una corrida automática fallida por completo no deja una ejecución persistida para la fecha y permite el reintento manual ese mismo día.

## Assumptions

- SPEC-013 (#38) y SPEC-016 (#39) están cerradas y su comportamiento está disponible.
- La ejecución manual de recálculo (SPEC-016) funciona correctamente y se asume operativa; esta especificación no la modifica ni la verifica.
- La restricción de como máximo una ejecución por fecha UTC sigue vigente y aplica también a la ejecución automática.
- La cadencia es semanal, lunes a las 00:00 UTC, y es ajustable por configuración; al evaluarse en UTC no hay horario estacional que desplazar.
- La estrategia y los parámetros de la ejecución automática se definen por configuración y el administrador puede cambiarlos sin modificar la lógica de negocio ni las fórmulas de valuación.
- Existe un entorno de ejecución desatendida disponible; su naturaleza técnica corresponde a PLAN.
- El registro de ejecuciones se consulta mediante el mecanismo existente por fecha; no se crea una interfaz ni una consulta histórica nueva.
- Los registros de ejecución se conservan de forma indefinida junto con el historial de cotizaciones; no se define una purga.
- La sincronización automática de datos deportivos (#41) queda fuera de alcance.

## Decisiones de CLARIFY

Las cinco decisiones abiertas del issue #40 quedaron resueltas en la sesión 2026-11-03 registrada en [Clarifications](#clarifications). No queda ninguna decisión pendiente para esta fase.

## Trazabilidad

- **Issue**: #40 — `SPEC-023 | Programar recálculo automático de cotizaciones`
- **Requerimiento**: `REQ-S2-11-B`
- **Dependencias cerradas**: SPEC-013 (#38, valuaciones estadísticas) y SPEC-016 (#39, recálculo manual de cotizaciones).
- **Fuera de alcance**: sincronización automática de datos deportivos (#41), fórmulas de valuación, consultas históricas nuevas, reglas de compra/venta, comportamiento del mercado e interfaz de usuario.

| Requisito del issue | Requisitos funcionales |
| --- | --- |
| RF-01 | FR-001, FR-011 |
| RF-02 | FR-002, FR-003 |
| RF-03 | FR-002, FR-004, FR-008 |
| RF-04 | FR-006, FR-007, FR-012, FR-013 |
| RF-05 | FR-004, FR-005, FR-010 |
| RF-06 | FR-004 y supuesto de ejecución manual operativa (SPEC-016) |

| Criterio de aceptación | Requisitos funcionales |
| --- | --- |
| CA-01 | FR-001, FR-011 |
| CA-02 | FR-002, FR-003 |
| CA-03 | FR-002, FR-004, FR-008 |
| CA-04 | FR-006, FR-007, FR-012, FR-013 |
| CA-05 | FR-004, FR-005, FR-010 |
| CA-06 | Supuesto: ejecución manual operativa (SPEC-016), fuera de verificación |

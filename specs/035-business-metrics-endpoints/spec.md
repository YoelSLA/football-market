# Feature Specification: Endpoints de métricas de negocio

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git. Forma parte de la tríada permanente junto a
plan.md y tasks.md. Las US formales nacen aquí; la existencia de este archivo no completa
Specify ni habilita la implementación. -->

**Feature Branch**: `feat/035-business-metrics-endpoints`

**Created**: 2026-11-26

**Status**: Draft

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: Issue #102, `SPEC-035 | Endpoints de métricas de negocio con Micrometer`, vinculada a `REQ-S3-05`.

**Trazabilidad**: `REQ-S3-05` (issue #102).

## Clarifications

### Session 2026-11-26

- Q: ¿Qué cuentas autenticadas pueden consultar los dos endpoints de métricas de negocio? → A: Cualquier cuenta autenticada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar totales históricos de negocio (Priority: P1)

Como integrante autenticado del equipo responsable del sistema, quiero consultar los totales históricos de compras, ventas, publicaciones canceladas, tokens transferidos y créditos negociados, para conocer la actividad de negocio registrada hasta el instante de consulta.

**Why this priority**: Proporciona los totales exactos derivados de la actividad persistida y no depende del tiempo que lleve activo el proceso actual.

**Independent Test**: Preparar operaciones completadas y publicaciones canceladas persistidas, consultar el endpoint histórico autenticado y comparar cada total con los registros de negocio hasta el instante informado.

**Acceptance Scenarios**:

1. **Given** registros financieros y publicaciones con estados persistidos, **When** una persona autenticada consulta `GET /api/business-metrics/historical`, **Then** recibe los totales históricos y el instante de consulta en la estructura definida en FR-003.
2. **Given** un registro financiero incluido en el historial, **When** se calculan tokens transferidos y créditos negociados, **Then** la cantidad de tokens y el importe de ese registro se incorporan una sola vez.
3. **Given** una publicación que vence y queda `EXPIRED`, **When** se consultan los totales históricos, **Then** no se incluye en publicaciones canceladas ni se contabilizan movimientos financieros por ese vencimiento.
4. **Given** que no hay actividad persistida aplicable, **When** se consulta el endpoint, **Then** los contadores y volúmenes son cero y el instante de consulta sigue presente.

---

### User Story 2 - Consultar métricas de la ejecución actual (Priority: P1)

Como integrante autenticado del equipo responsable del sistema, quiero consultar las operaciones de negocio completadas desde el último arranque de la aplicación, para conocer la actividad acumulada durante esa ejecución.

**Why this priority**: Ofrece una lectura de actividad en tiempo de ejecución complementaria al historial persistido, sin reemplazarlo.

**Independent Test**: Completar operaciones de negocio durante una ejecución, consultar el endpoint autenticado y verificar los contadores; comprobar que operaciones fallidas o rechazadas no los aumentan y que un nuevo arranque inicia una acumulación nueva.

**Acceptance Scenarios**:

1. **Given** que la aplicación lleva registradas operaciones de negocio completadas desde su arranque actual, **When** una persona autenticada consulta `GET /api/business-metrics/current`, **Then** recibe los acumulados de esa ejecución y el instante de consulta en la estructura definida en FR-003.
2. **Given** una solicitud HTTP que completa más de una operación de negocio, **When** se actualizan las métricas de la ejecución, **Then** se contabiliza cada operación completada, no la solicitud.
3. **Given** una operación fallida o rechazada, **When** se actualizan las métricas de la ejecución, **Then** la operación no incrementa los contadores de eventos completados.
4. **Given** que la aplicación se inicia en un nuevo proceso, **When** se consultan las métricas de la ejecución, **Then** estas reflejan únicamente las operaciones completadas desde ese arranque y no sustituyen los totales históricos.

---

### User Story 3 - Consultar métricas con acceso y exposición acotados (Priority: P1)

Como consumidor autenticado de las métricas de negocio, quiero que ambos endpoints tengan el mismo contrato y solo expongan agregados, para consultar actividad sin revelar datos personales o identificadores individuales.

**Why this priority**: La autenticación, la uniformidad de los contratos y la ausencia de datos personales aplican a ambas formas de consulta.

**Independent Test**: Comparar los nombres y tipos de campos de ambas respuestas, probar el acceso autenticado y no autenticado y revisar que las respuestas y las etiquetas de métricas no contengan datos personales ni identificadores individuales.

**Acceptance Scenarios**:

1. **Given** una solicitud sin autenticación, **When** se consulta cualquiera de las dos rutas, **Then** recibe `401 Unauthorized`.
2. **Given** una solicitud autenticada a cualquiera de las dos rutas, **When** se devuelve el resultado, **Then** contiene exactamente los mismos campos y tipos que la otra ruta.
3. **Given** una respuesta HTTP o una métrica de negocio, **When** se inspeccionan sus datos, **Then** no contiene datos personales ni identificadores de usuario, jugador u otros identificadores individuales de alta cardinalidad.
4. **Given** cualquier cuenta autenticada mediante el mecanismo existente, **When** consulta cualquiera de las dos rutas, **Then** puede acceder al endpoint sin requerir un rol adicional.

## Edge Cases

- Un conteo sin operaciones o cancelaciones aplicables se representa con cero; los volúmenes acumulados también se representan con cero.
- El instante de consulta refleja cuándo se produjo la lectura y se expresa como fecha y hora ISO 8601 en UTC.
- Una operación que no llega a completarse no aporta eventos, tokens ni créditos a las métricas de la ejecución.
- El registro financiero ya persistido es la unidad de contribución para volumen de tokens y créditos; no se vuelve a sumar por la presencia de comprador y vendedor ni por la solicitud que lo originó.
- Una publicación solo contribuye al contador de cancelaciones cuando su estado pasa efectivamente a `CANCELLED`. `EXPIRED` no es una cancelación.
- La cancelación de una publicación por sí sola no genera un movimiento de tokens ni créditos.
- El reinicio del proceso inicia nuevos acumulados de ejecución; los totales históricos continúan calculándose desde los registros persistidos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(issue RF-02)*: El sistema MUST exponer dos endpoints REST autenticados para consultar métricas de negocio, separados de `/actuator/prometheus`:
  - `GET /api/business-metrics/historical`: totales históricos calculados desde los registros persistidos hasta el instante de consulta.
  - `GET /api/business-metrics/current`: contadores de negocio acumulados desde el último arranque de la aplicación.
- **FR-002** *(issue RF-01, RF-04, RF-05)*: Las métricas MUST contar operaciones de negocio completadas, no solicitudes HTTP. Una solicitud que complete varias operaciones MUST contabilizar cada una; operaciones fallidas o rechazadas MUST NOT incrementar los contadores de eventos completados. El endpoint histórico MUST consultar los registros persistidos; el endpoint de ejecución MUST informar los contadores Micrometer acumulados desde el arranque actual para compras, ventas, publicaciones canceladas, tokens transferidos y créditos negociados, reiniciarse al iniciar un nuevo proceso y MUST NOT reemplazar el historial persistido.
- **FR-003** *(issue RF-03)*: Ambos endpoints MUST devolver la misma estructura JSON, con exactamente estos campos y tipos:

  ```json
  {
    "purchases": 0,
    "sales": 0,
    "cancelledListings": 0,
    "tokensTransferred": 0,
    "creditsTradedInCents": 0,
    "queriedAt": "2026-11-26T12:00:00Z"
  }
  ```

  `purchases`, `sales` y `cancelledListings` MUST ser conteos enteros; `tokensTransferred` MUST ser un volumen numérico acumulado; `creditsTradedInCents` MUST ser el volumen entero de créditos expresado en centésimas de crédito; `queriedAt` MUST ser el instante de consulta en formato ISO 8601 UTC. Cuando no haya actividad, los conteos y volúmenes MUST ser cero.
- **FR-004** *(issue RF-04, RF-06)*: Para cada registro financiero, el volumen de tokens y el importe MUST contribuir una sola vez a los agregados correspondientes. El registro MUST contabilizarse como una compra o venta según la operación de negocio registrada, sin contar la misma operación en ambas categorías. El importe negociado MUST expresarse en centésimas de crédito.
- **FR-005** *(issue RF-01, RF-07)*: Una publicación MUST contribuir a `cancelledListings` únicamente cuando pase efectivamente a estado `CANCELLED`. Las publicaciones en estado `EXPIRED` MUST NOT contar como cancelaciones. Cancelar una publicación MUST NOT contabilizarse como movimiento de tenencias, tokens ni créditos.
- **FR-006** *(issue RF-02)*: Las solicitudes no autenticadas a cualquiera de los dos endpoints MUST recibir `401 Unauthorized`. Cualquier cuenta autenticada mediante el mecanismo existente MUST poder consultarlos, sin requerir un rol adicional.
- **FR-007** *(issue RF-09)*: Las respuestas y las etiquetas de métricas MUST NOT exponer datos personales ni identificadores individuales de usuario, jugador u otras dimensiones de alta cardinalidad.
- **FR-008** *(issue RF-08)*: Ambos contratos HTTP MUST estar documentados en OpenAPI, con sus rutas, autenticación, estructura de respuesta y significado de los campos.
- **FR-009** *(issue #102, límites explícitos)*: La funcionalidad MUST NOT reemplazar el historial persistido; MUST NOT incluir métricas técnicas cubiertas por issue #100; MUST NOT incluir el despliegue de Prometheus/Grafana cubierto por issue #117; y MUST NOT ofrecer desgloses por usuario o jugador.

### Key Entities *(include if data involved)*

- **Métrica histórica de negocio**: agregado de compras, ventas, publicaciones canceladas, tokens transferidos y créditos negociados calculado desde los registros persistidos hasta un instante de consulta.
- **Métrica de ejecución de negocio**: agregado de operaciones completadas durante el proceso de aplicación activo, desde su último arranque.
- **Registro financiero**: operación de compra o venta completada que aporta una sola vez sus tokens e importe a los volúmenes correspondientes.
- **Publicación cancelada**: publicación cuyo estado pasa efectivamente a `CANCELLED`; una publicación `EXPIRED` no pertenece a esta entidad para el conteo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En cada endpoint autenticado, los cinco conteos y volúmenes devueltos coinciden con sus fuentes definidas: registros persistidos para el histórico y operaciones completadas desde el arranque para la ejecución.
- **SC-002**: Ambas respuestas incluyen exactamente `purchases`, `sales`, `cancelledListings`, `tokensTransferred`, `creditsTradedInCents` y `queriedAt`, con los tipos y formatos establecidos en FR-003.
- **SC-003**: Cada registro financiero aporta una sola vez sus tokens e importe; las operaciones fallidas o rechazadas, las publicaciones `EXPIRED` y las cancelaciones sin movimiento financiero no incrementan los agregados que no les corresponden.
- **SC-004**: Las solicitudes no autenticadas a ambas rutas reciben `401 Unauthorized`, y ninguna respuesta ni etiqueta de métrica expone datos personales o identificadores individuales de alta cardinalidad.
- **SC-005**: Los dos contratos están documentados en OpenAPI y se mantienen separados de las métricas técnicas de `/actuator/prometheus`.

## Assumptions

- Las rutas se alojan bajo `/api` y se denominan `/business-metrics/historical` y `/business-metrics/current`; distinguen explícitamente el alcance histórico del alcance de la ejecución actual y no sustituyen `/actuator/prometheus`.
- Las respuestas usan JSON. Los nombres y tipos del ejemplo de FR-003 son el contrato común requerido para ambos endpoints.
- `tokensTransferred` es un volumen numérico porque agrega cantidades de tokens; `creditsTradedInCents` es un entero que representa centésimas de crédito.
- Se reutiliza el mecanismo de autenticación existente del backend; cualquier cuenta autenticada puede consultar los endpoints y esta especificación no define roles nuevos.
- Las compras, ventas y cancelaciones corresponden a operaciones de negocio completadas, no a solicitudes ni intentos.
- Micrometer proporciona los contadores de negocio acumulados de la ejecución actual; su reinicio con el proceso es el límite temporal solicitado, no una retención persistente.
- Los issues #100 y #117 mantienen sus alcances respectivos de métricas técnicas y despliegue de Prometheus/Grafana; esta feature solo define métricas de negocio y sus dos endpoints.

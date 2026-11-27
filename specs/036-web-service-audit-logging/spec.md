# Feature Specification: Auditoría de peticiones a servicios web

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md. -->

**Feature Branch**: `feat/036-web-service-audit-logging`

**Created**: 2026-11-26

**Status**: Ready for implementation

**Input**: User description: "Especificá la auditoría de las peticiones a los servicios REST publicados con Spring, conforme a la issue #99. La auditoría debe registrar, mediante el logging existente con SLF4J/Logback, timestamp, usuario, método HTTP y ruta, parámetros relevantes y duración en milisegundos. El usuario debe obtenerse del contexto de seguridad/JWT o registrarse como anonymous si no hay autenticación. No registrar credenciales sensibles ni contraseñas en texto plano. La auditoría no requiere persistencia en una tabla ni una interfaz gráfica, y no incluye llamadas a APIs externas de terceros. Conservá el alcance de la issue y expresá cualquier ambigüedad que impida especificar estos requisitos; no agregues requisitos por conveniencia. Para los parámetros, respetar los parámetros relevantes de la consulta indicados en la issue #99."

## Clarifications

### Session 2026-11-26

- Q: ¿Qué parámetros de la consulta deben incluirse en el registro de auditoría? → A: Solo los parámetros de consulta reconocidos por el endpoint, excepto identificadores; excluir siempre credenciales y contraseñas.
- Q: ¿Qué parámetros de consulta se consideran sensibles? → A: Los identificadores recibidos como parámetros de consulta son el único dato sensible dentro de esos parámetros y deben excluirse del registro. Los demás parámetros de consulta reconocidos por el endpoint no se consideran sensibles y pueden registrarse. Las credenciales y contraseñas siguen excluidas en cualquier caso.

## User Scenarios & Testing

### User Story 1 - Consultar auditoría de peticiones REST (Priority: P1)

Una persona que opera o mantiene el sistema puede consultar los registros de las peticiones recibidas por los servicios REST publicados y reconocer cuándo ocurrió cada petición, quién la realizó, qué operación atendió, qué parámetros relevantes se usaron y cuánto duró.

**Why this priority**: Es el objetivo central de la issue #99: disponer de trazabilidad de las peticiones entrantes.

**Independent Test**: Enviar peticiones autenticadas y no autenticadas, exitosas y fallidas, a servicios REST publicados y comprobar que sus registros reflejan los datos requeridos, sin exponer credenciales sensibles ni contraseñas.

**Acceptance Scenarios**:

1. **Given** una petición a un servicio REST publicado con un usuario autenticado, **When** la petición termina, **Then** se registra timestamp, usuario autenticado, método HTTP, ruta, parámetros relevantes y duración en milisegundos.
2. **Given** una petición a un servicio REST publicado sin autenticación, **When** la petición termina, **Then** se registra la identidad como `anonymous`, junto con los demás datos de auditoría requeridos.
3. **Given** una petición cuyos datos incluyen credenciales sensibles o contraseñas, **When** se registra la auditoría, **Then** esos valores no aparecen en texto plano en el registro.
4. **Given** una petición a un servicio REST publicado que termina con error, **When** se procesa, **Then** también se genera su registro de auditoría.

### Edge Cases

- Petición sin parámetros relevantes: se registra la auditoría sin inventar valores de parámetros.
- Petición que termina con error: también se audita, conforme al alcance de todas las invocaciones a servicios web indicado en la issue.
- Solo se registran los parámetros de consulta reconocidos por el endpoint que no sean identificadores; se excluyen los identificadores, aunque estén reconocidos, y no se incluyen otros datos de la petición ni credenciales o contraseñas.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST registrar todas las invocaciones a los servicios REST publicados con Spring, incluidas las que terminan con error, utilizando el logging existente con SLF4J/Logback.
- **FR-002**: Cada registro MUST incluir timestamp, usuario, método HTTP, ruta u operación invocada, parámetros de consulta reconocidos por el endpoint que no sean identificadores y duración de la petición en milisegundos, conforme a RF-02 y RF-04 de la issue #99. MUST NOT incluir identificadores recibidos como parámetros de consulta, credenciales ni contraseñas.
- **FR-003**: Para una petición autenticada, el usuario registrado MUST obtenerse del contexto de seguridad/JWT.
- **FR-004**: Para una petición sin autenticación, el usuario registrado MUST ser `anonymous`.
- **FR-005**: Los registros MUST NOT exponer credenciales, contraseñas ni otra información sensible.
- **FR-006**: La auditoría MUST NOT requerir persistencia en una tabla ni una interfaz gráfica.
- **FR-007**: La auditoría MUST NOT abarcar llamadas salientes a APIs externas de terceros.

### Key Entities

- **Registro de auditoría de petición**: representación en el logging existente de los datos de auditoría de una petición entrante: timestamp, usuario, método HTTP, ruta, parámetros relevantes y duración en milisegundos.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cada invocación a los servicios REST publicados, incluidas las que terminan con error, produce un registro que permite identificar el timestamp, el usuario o `anonymous`, el método HTTP, la ruta u operación y la duración en milisegundos.
- **SC-002**: La verificación de los registros de peticiones no autenticadas confirma que la identidad queda indicada como `anonymous`.
- **SC-003**: La inspección de los registros confirma que no contienen credenciales sensibles ni contraseñas en texto plano.
- **SC-004**: Los registros incluyen los parámetros de consulta reconocidos por el endpoint que no sean identificadores; no incluyen identificadores, credenciales ni contraseñas.

## Assumptions

- El mecanismo de autenticación y el logging existentes se reutilizan; esta feature no introduce persistencia de auditoría ni una interfaz gráfica.
- La duración se informa en milisegundos y corresponde al tiempo de atención de la petición.
- El alcance es la auditoría de peticiones entrantes a los servicios REST publicados por la aplicación; no incluye invocaciones salientes a terceros.
- Los parámetros de consulta reconocidos distintos de identificadores no se consideran sensibles y pueden registrarse. Los identificadores recibidos como parámetros de consulta se consideran sensibles y se excluyen; las credenciales y contraseñas también se excluyen siempre.

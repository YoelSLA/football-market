# Feature Specification: Métricas Prometheus y monitoreo seguro

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `[feat/032-prometheus-actuator-monitoring]` — rama Git de la feature. El directorio de esta spec usa el nombre `032-prometheus-actuator-monitoring`.

**Created**: 2026-11-24

**Status**: Ready for planning

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Completar la especificación de SPEC-032 a partir de REQ-S3-03 (issue #100). El equipo de operaciones necesita consultar métricas del backend compatibles con Prometheus para monitorear su estado, recursos y rendimiento. Incluye el endpoint /actuator/prometheus y el acceso seguro a los endpoints de monitoreo, sin exponer información sensible. Reutiliza Actuator, que ya existe. No incluyas desplegar un servidor Prometheus ni crear dashboards de Grafana."

**Trazabilidad**: REQ-S3-03 (issue #100).

## Clarifications

### Session 2026-11-24

- Q: ¿Qué endpoints de Actuator deben quedar accesibles por HTTP y con qué protección? → A: Exponer `/actuator/prometheus` públicamente, sin JWT, para cualquier cliente que alcance el backend; mantener `/actuator/health` público con estado mínimo; exponer `/actuator/info` públicamente sin datos sensibles; y bloquear los demás endpoints de Actuator.
- Q: ¿Cómo debe quedar el acceso HTTP a `/actuator/info`? → A: Exponerlo públicamente, solo con datos no sensibles.
- Q: ¿Qué métricas cubren el estado, los recursos y el rendimiento sin agregar instrumentación propia? → A: Reutilizar las métricas estándar de Spring Boot/Micrometer: CPU del sistema y del proceso; memoria, hilos y pausas de GC de la JVM; y recuento y duración de solicitudes HTTP. El estado general se consulta por separado en `/actuator/health`; la tasa de errores se obtiene de los resultados de las solicitudes HTTP. No se requieren métricas de negocio, métricas personalizadas ni percentiles de latencia.
- Q: ¿Cambia la respuesta pública de `/actuator/prometheus` si la solicitud incluye un JWT? → A: No. La presencia, ausencia o validez del JWT no afecta el acceso ni la respuesta; el endpoint no requiere ni utiliza ese token.
- Q: ¿Qué información operativa se considera sensible en las respuestas de monitoreo? → A: Secretos, credenciales, tokens y valores de configuración sensibles no deben exponerse en ningún endpoint. Las métricas estándar de CPU, JVM y HTTP y el estado mínimo de health, así como información técnica no sensible del programa o de la máquina, pueden mostrarse y no se consideran sensibles por sí mismos. `/actuator/info` puede incluir información de programa o máquina únicamente si no pertenece a las categorías sensibles indicadas.
- Q: ¿Qué autenticación permite consultar `/actuator/prometheus`? → A: No requiere autenticación ni JWT; cualquier cliente que alcance el backend puede consultarlo, sin roles ni permisos adicionales.
- Q: ¿Qué información estática debe mostrar `/actuator/info`? → A: Debe mostrar el nombre público `Football Market` y la descripción `API de mercado de jugadores de fútbol`. No debe exponer otros valores del entorno ni configuración sensible.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar métricas del backend (Priority: P1)

El equipo de operaciones y cualquier otro cliente que alcance el backend consulta las métricas del backend en un formato compatible con Prometheus para observar sus recursos y rendimiento, sin presentar un JWT.

**Why this priority**: La consulta de métricas constituye el valor central requerido para monitorear la aplicación.

**Independent Test**: Solicitar las métricas sin JWT y verificar que la respuesta sea legible por un recolector compatible con Prometheus y contenga las métricas estándar de recursos y rendimiento. Consultar por separado el estado general en `/actuator/health`.

**Acceptance Scenarios**:

1. **Given** que el backend está disponible y un cliente puede alcanzarlo, **When** consulta `/actuator/prometheus` sin JWT, **Then** recibe métricas del backend en un formato compatible con Prometheus.
2. **Given** que el backend publica sus métricas de monitoreo, **When** cualquier cliente inspecciona `/actuator/prometheus` y `/actuator/health`, **Then** puede identificar las métricas estándar de recursos y rendimiento y el estado general del backend, sin consultar endpoints de dominio ni presentar un JWT.

---

### User Story 2 - Limitar la información y los endpoints de monitoreo (Priority: P1)

Los clientes que alcanzan el backend pueden consultar las métricas sin autenticación, mientras que la información sensible y los endpoints de Actuator no permitidos permanecen protegidos.

**Why this priority**: El acceso público deliberado a las métricas no debe exponer información sensible ni habilitar otros endpoints de Actuator.

**Independent Test**: Consultar `/actuator/prometheus` sin JWT y verificar que devuelve métricas sin información sensible; comprobar además que los endpoints de Actuator fuera de la lista permitida no son accesibles por HTTP.

**Acceptance Scenarios**:

1. **Given** un cliente que alcanza el backend sin JWT, **When** consulta `/actuator/prometheus`, **Then** recibe las métricas permitidas sin una autenticación, rol o permiso de aplicación.
2. **Given** una respuesta de `/actuator/prometheus`, **When** cualquier cliente la consulta, **Then** no contiene secretos, credenciales, tokens ni valores de configuración sensibles.
3. **Given** un endpoint de Actuator que no forma parte de los endpoints de monitoreo permitidos, **When** cualquier solicitante intenta acceder por HTTP, **Then** el endpoint no queda expuesto.
4. **Given** un solicitante anónimo, **When** consulta `/actuator/info`, **Then** recibe el nombre y la descripción pública del backend, sin información sensible.
5. **Given** una solicitud a `/actuator/prometheus` con un JWT ausente, válido, vencido o inválido, **When** un cliente alcanza el backend, **Then** recibe el mismo estado HTTP y las mismas métricas; el JWT no se requiere ni se utiliza.

## Edge Cases

- `/actuator/prometheus` no requiere ni utiliza JWT; su presencia, ausencia o validez no cambia el acceso ni la respuesta.
- Si la aplicación no puede producir temporalmente una métrica particular, el endpoint no debe sustituir la ausencia de esa métrica con información sensible ni habilitar endpoints adicionales.
- `/actuator/health` permanece público solo con un estado mínimo; sus subendpoints y los demás endpoints de Actuator no quedan expuestos por HTTP.
- `/actuator/info` puede consultarse sin autenticación, pero su respuesta no debe incluir información sensible.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El backend MUST exponer `/actuator/prometheus` para que herramientas compatibles con Prometheus consulten métricas del backend.
- **FR-002**: `/actuator/prometheus` MUST exponer las métricas estándar de Spring Boot/Micrometer para monitorear recursos y rendimiento: uso de CPU del sistema y del proceso; memoria, hilos y pausas de garbage collection de la JVM cuando se produzcan eventos de GC; y recuento y duración de solicitudes HTTP. El estado general MUST consultarse por separado mediante `/actuator/health`. La tasa de errores MUST poder obtenerse de los resultados de las solicitudes HTTP; no se requiere una métrica de tasa de errores independiente.
- **FR-003**: `/actuator/prometheus` MUST ser accesible sin autenticación ni JWT para cualquier cliente que alcance el backend y MUST NOT requerir roles ni permisos de aplicación. La presencia o validez de un JWT suministrado por el solicitante MUST NOT afectar el acceso.
- **FR-004**: El backend MUST responder con HTTP `200` y las métricas en formato compatible con Prometheus a solicitudes a `/actuator/prometheus`, independientemente de que incluyan un JWT válido, vencido o inválido; el estado HTTP y las métricas devueltas MUST ser los mismos que para una solicitud sin JWT.
- **FR-005**: La información expuesta por los endpoints de monitoreo MUST NOT incluir secretos, credenciales, tokens ni valores de configuración sensibles.
- **FR-006**: La exposición HTTP de Actuator MUST limitarse a `/actuator/prometheus`, `/actuator/health` y `/actuator/info`; los demás endpoints de Actuator, incluidos sus subendpoints, MUST permanecer inaccesibles por HTTP.
- **FR-007**: `/actuator/health` MUST permanecer accesible sin autenticación y limitar su respuesta a un estado mínimo, sin revelar detalles de componentes, dependencias o configuración.
- **FR-008**: Esta funcionalidad MUST reutilizar Actuator existente y MUST NOT incluir el despliegue de un servidor Prometheus ni la creación de dashboards de Grafana.
- **FR-009**: `/actuator/info` MUST permanecer accesible sin autenticación, MUST incluir el nombre público `Football Market` y la descripción `API de mercado de jugadores de fútbol`, y MUST NOT exponer otros valores del entorno ni información sensible.

### Key Entities *(include if data involved)*

- **Métrica de monitoreo**: Medición del estado, los recursos o el rendimiento del backend presentada para su consulta por el equipo de operaciones.
- **Cliente solicitante**: Cualquier cliente que pueda alcanzar el backend; no necesita autenticación ni JWT para consultar `/actuator/prometheus`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `/actuator/prometheus` devuelve en formato compatible las métricas estándar de CPU, JVM y solicitudes HTTP indicadas en FR-002, y `/actuator/health` informa el estado general mínimo del backend.
- **SC-002**: Las solicitudes a `/actuator/prometheus` sin JWT reciben HTTP `200` y las métricas en formato compatible con Prometheus; la consulta no requiere roles ni permisos de aplicación.
- **SC-003**: Ninguna respuesta HTTP de monitoreo contiene secretos, credenciales, tokens ni valores de configuración sensibles.
- **SC-004**: Por HTTP, `/actuator/prometheus` es accesible sin JWT a cualquier cliente que alcance el backend, `/actuator/health` devuelve únicamente un estado mínimo, `/actuator/info` muestra el nombre y la descripción públicos establecidos sin exponer información sensible, y ningún otro endpoint de Actuator es accesible.

## Assumptions

- El backend ya cuenta con Actuator y con mecanismos de autenticación y autorización para los endpoints que los requieren; esta feature no añade autenticación a `/actuator/prometheus`.
- Las métricas estándar de Spring Boot/Micrometer indicadas en FR-002 son suficientes para este alcance; no se agregan métricas personalizadas, métricas de negocio ni requisitos de percentiles de latencia.
- `/actuator/prometheus` es público para cualquier cliente que alcance el backend; la consulta no requiere JWT, roles ni permisos de aplicación.
- Las métricas se consultan desde el backend por herramientas compatibles; instalar, desplegar u operar un servidor Prometheus queda fuera del alcance.
- Los endpoints de salud que deban seguir siendo públicos solo entregan un estado mínimo, sin detalles internos.

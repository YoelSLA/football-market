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

**Status**: Ready for implementation

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Completar la especificación de SPEC-032 a partir de REQ-S3-03 (issue #100). El equipo de operaciones necesita consultar métricas del backend compatibles con Prometheus para monitorear su estado, recursos y rendimiento. Incluye el endpoint /actuator/prometheus y el acceso seguro a los endpoints de monitoreo, sin exponer información sensible. Reutiliza Actuator, que ya existe. No incluyas desplegar un servidor Prometheus ni crear dashboards de Grafana."

**Trazabilidad**: REQ-S3-03 (issue #100).

## Clarifications

### Session 2026-11-24

- Q: ¿Qué endpoints de Actuator deben quedar accesibles por HTTP y con qué protección? → A: Exponer `/actuator/prometheus` a cualquier identidad con un JWT válido aceptado por la autenticación existente, mantener `/actuator/health` público con estado mínimo, exponer `/actuator/info` públicamente sin datos sensibles y bloquear los demás endpoints de Actuator.
- Q: ¿Cómo debe quedar el acceso HTTP a `/actuator/info`? → A: Exponerlo públicamente, solo con datos no sensibles.
- Q: ¿Qué métricas cubren el estado, los recursos y el rendimiento sin agregar instrumentación propia? → A: Reutilizar las métricas estándar de Spring Boot/Micrometer: CPU del sistema y del proceso; memoria, hilos y pausas de GC de la JVM; y recuento y duración de solicitudes HTTP. El estado general se consulta por separado en `/actuator/health`; la tasa de errores se obtiene de los resultados de las solicitudes HTTP. No se requieren métricas de negocio, métricas personalizadas ni percentiles de latencia.
- Q: ¿Qué información operativa se considera sensible en las respuestas de monitoreo? → A: Secretos, credenciales, tokens y valores de configuración sensibles no deben exponerse en ningún endpoint. Las métricas estándar de CPU, JVM y HTTP y el estado mínimo de health, así como información técnica no sensible del programa o de la máquina, pueden mostrarse y no se consideran sensibles por sí mismos. `/actuator/info` puede incluir información de programa o máquina únicamente si no pertenece a las categorías sensibles indicadas.
- Q: ¿Qué autenticación permite consultar `/actuator/prometheus`? → A: Basta con presentar un JWT válido aceptado por el mecanismo de autenticación existente. No hay roles ni permisos adicionales para este endpoint; todas las identidades autenticadas pueden consultarlo. Las solicitudes anónimas o con JWT inválido se rechazan con `401`.
- Q: ¿Qué información estática debe mostrar `/actuator/info`? → A: Debe mostrar el nombre público `Football Market` y la descripción `API de mercado de jugadores de fútbol`. No debe exponer otros valores del entorno ni configuración sensible.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar métricas del backend (Priority: P1)

El equipo de operaciones con una identidad autenticada consulta las métricas del backend en un formato compatible con Prometheus para observar sus recursos y rendimiento.

**Why this priority**: La consulta de métricas constituye el valor central requerido para monitorear la aplicación.

**Independent Test**: Solicitar las métricas con un JWT válido y verificar que la respuesta sea legible por un recolector compatible con Prometheus y contenga las métricas estándar de recursos y rendimiento. Consultar por separado el estado general en `/actuator/health`.

**Acceptance Scenarios**:

1. **Given** que el backend está disponible y el solicitante presenta un JWT válido, **When** consulta `/actuator/prometheus`, **Then** recibe métricas del backend en un formato compatible con Prometheus, sin que se requiera un rol o permiso adicional.
2. **Given** que el backend publica sus métricas de monitoreo, **When** operaciones inspecciona `/actuator/prometheus` y `/actuator/health`, **Then** puede identificar las métricas estándar de recursos y rendimiento y el estado general del backend, sin consultar endpoints de dominio.

---

### User Story 2 - Proteger el acceso y la información de monitoreo (Priority: P1)

El equipo de operaciones consulta las métricas con una identidad autenticada, mientras que las solicitudes sin autenticación válida y la información sensible quedan protegidas.

**Why this priority**: La disponibilidad de métricas no debe crear una vía de acceso público ni exponer secretos o detalles internos sensibles.

**Independent Test**: Comparar solicitudes anónimas, con JWT inválido y con JWT válido a `/actuator/prometheus`; verificar que solo las dos primeras se rechazan y que las respuestas no contienen la información sensible definida.

**Acceptance Scenarios**:

1. **Given** un solicitante anónimo o con JWT inválido, **When** intenta consultar `/actuator/prometheus`, **Then** recibe `401` y no recibe métricas.
2. **Given** un solicitante con un JWT válido aceptado por la autenticación existente, **When** consulta `/actuator/prometheus`, **Then** recibe las métricas permitidas sin requerir roles ni permisos adicionales.
3. **Given** un endpoint de Actuator que no forma parte de los endpoints de monitoreo permitidos, **When** cualquier solicitante intenta acceder por HTTP, **Then** el endpoint no queda expuesto.
4. **Given** un solicitante anónimo, **When** consulta `/actuator/info`, **Then** recibe el nombre y la descripción pública del backend, sin información sensible.

## Edge Cases

- Una solicitud anónima o con credenciales JWT inválidas a `/actuator/prometheus` se rechaza sin revelar el contenido protegido. Una identidad con JWT válido no requiere un rol o permiso adicional.
- Si la aplicación no puede producir temporalmente una métrica particular, el endpoint no debe sustituir la ausencia de esa métrica con información sensible ni habilitar endpoints adicionales.
- `/actuator/health` permanece público solo con un estado mínimo; sus subendpoints y los demás endpoints de Actuator no quedan expuestos por HTTP.
- `/actuator/info` puede consultarse sin autenticación, pero su respuesta no debe incluir información sensible.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El backend MUST exponer `/actuator/prometheus` para que herramientas compatibles con Prometheus consulten métricas del backend.
- **FR-002**: `/actuator/prometheus` MUST exponer las métricas estándar de Spring Boot/Micrometer para monitorear recursos y rendimiento: uso de CPU del sistema y del proceso; memoria, hilos y pausas de garbage collection de la JVM cuando se produzcan eventos de GC; y recuento y duración de solicitudes HTTP. El estado general MUST consultarse por separado mediante `/actuator/health`. La tasa de errores MUST poder obtenerse de los resultados de las solicitudes HTTP; no se requiere una métrica de tasa de errores independiente.
- **FR-003**: El acceso a `/actuator/prometheus` MUST requerir un JWT válido aceptado por los mecanismos de autenticación existentes; todas las identidades autenticadas pueden acceder sin una comprobación adicional de roles o permisos.
- **FR-004**: El backend MUST denegar con `401` las solicitudes anónimas o con JWT inválido a `/actuator/prometheus`, sin devolver su contenido.
- **FR-005**: La información expuesta por los endpoints de monitoreo MUST NOT incluir secretos, credenciales, tokens ni valores de configuración sensibles.
- **FR-006**: La exposición HTTP de Actuator MUST limitarse a `/actuator/prometheus`, `/actuator/health` y `/actuator/info`; los demás endpoints de Actuator, incluidos sus subendpoints, MUST permanecer inaccesibles por HTTP.
- **FR-007**: `/actuator/health` MUST permanecer accesible sin autenticación y limitar su respuesta a un estado mínimo, sin revelar detalles de componentes, dependencias o configuración.
- **FR-008**: Esta funcionalidad MUST reutilizar Actuator existente y MUST NOT incluir el despliegue de un servidor Prometheus ni la creación de dashboards de Grafana.
- **FR-009**: `/actuator/info` MUST permanecer accesible sin autenticación, MUST incluir el nombre público `Football Market` y la descripción `API de mercado de jugadores de fútbol`, y MUST NOT exponer otros valores del entorno ni información sensible.

### Key Entities *(include if data involved)*

- **Métrica de monitoreo**: Medición del estado, los recursos o el rendimiento del backend presentada para su consulta por el equipo de operaciones.
- **Identidad operativa autenticada**: Identidad cuyo JWT válido es aceptado por la autenticación existente y que, por ello, puede consultar `/actuator/prometheus` sin roles ni permisos adicionales.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `/actuator/prometheus` devuelve en formato compatible las métricas estándar de CPU, JVM y solicitudes HTTP indicadas en FR-002, y `/actuator/health` informa el estado general mínimo del backend.
- **SC-002**: El 100 % de las solicitudes anónimas o con JWT inválido a `/actuator/prometheus` reciben `401` y no reciben el contenido protegido; cualquier identidad con JWT válido puede acceder sin una comprobación adicional de roles o permisos.
- **SC-003**: Ninguna respuesta HTTP de monitoreo contiene secretos, credenciales, tokens ni valores de configuración sensibles.
- **SC-004**: Por HTTP, `/actuator/prometheus` es accesible a cualquier identidad autenticada con JWT válido, `/actuator/health` devuelve únicamente un estado mínimo, `/actuator/info` muestra el nombre y la descripción públicos establecidos sin exponer información sensible, y ningún otro endpoint de Actuator es accesible.

## Assumptions

- El backend ya cuenta con Actuator y con mecanismos de autenticación y autorización que deben reutilizarse.
- Las métricas estándar de Spring Boot/Micrometer indicadas en FR-002 son suficientes para este alcance; no se agregan métricas personalizadas, métricas de negocio ni requisitos de percentiles de latencia.
- Un JWT válido aceptado por la autenticación existente basta para acceder a Prometheus; no existen roles ni permisos adicionales para este endpoint y esta feature no introduce ninguno.
- Las métricas se consultan desde el backend por herramientas compatibles; instalar, desplegar u operar un servidor Prometheus queda fuera del alcance.
- Los endpoints de salud que deban seguir siendo públicos solo entregan un estado mínimo, sin detalles internos.

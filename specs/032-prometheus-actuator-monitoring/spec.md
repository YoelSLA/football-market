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

- Q: ¿Qué endpoints de Actuator deben quedar accesibles por HTTP y con qué protección? → A: Exponer `/actuator/prometheus` solo a identidades autorizadas, mantener `/actuator/health` público con estado mínimo, exponer `/actuator/info` públicamente sin datos sensibles y bloquear los demás endpoints de Actuator.
- Q: ¿Cómo debe quedar el acceso HTTP a `/actuator/info`? → A: Exponerlo públicamente, solo con datos no sensibles.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar métricas del backend (Priority: P1)

El equipo de operaciones consulta las métricas del backend en un formato compatible con Prometheus para observar su estado, recursos y rendimiento.

**Why this priority**: La consulta de métricas constituye el valor central requerido para monitorear la aplicación.

**Independent Test**: Solicitar las métricas con acceso autorizado y verificar que la respuesta sea legible por un recolector compatible con Prometheus y contenga métricas útiles de estado, recursos y rendimiento del backend.

**Acceptance Scenarios**:

1. **Given** que el backend está disponible y el solicitante está autorizado, **When** consulta `/actuator/prometheus`, **Then** recibe métricas del backend en un formato compatible con Prometheus.
2. **Given** que el backend publica sus métricas de monitoreo, **When** operaciones inspecciona el resultado, **Then** puede identificar métricas de estado, recursos y rendimiento sin consultar endpoints de dominio.

---

### User Story 2 - Proteger el acceso y la información de monitoreo (Priority: P1)

El equipo de operaciones accede a los endpoints de monitoreo autorizados, mientras que los solicitantes no autorizados y la información sensible quedan protegidos.

**Why this priority**: La disponibilidad de métricas no debe crear una vía de acceso público ni exponer secretos o detalles internos sensibles.

**Independent Test**: Comparar solicitudes autorizadas y no autorizadas a los endpoints de monitoreo expuestos y revisar las respuestas para confirmar que el acceso está restringido y no contiene información sensible.

**Acceptance Scenarios**:

1. **Given** un solicitante sin autorización, **When** intenta consultar métricas o endpoints de monitoreo protegidos, **Then** el acceso es denegado y la respuesta no revela métricas ni datos protegidos.
2. **Given** un solicitante autorizado, **When** consulta un endpoint de monitoreo expuesto, **Then** recibe solo la información de monitoreo permitida y no recibe secretos, credenciales, tokens ni configuración sensible.
3. **Given** un endpoint de Actuator que no forma parte de los endpoints de monitoreo permitidos, **When** cualquier solicitante intenta acceder por HTTP, **Then** el endpoint no queda expuesto.
4. **Given** un solicitante anónimo, **When** consulta `/actuator/info`, **Then** recibe únicamente información no sensible.

## Edge Cases

- Una solicitud a métricas sin credenciales válidas o con credenciales de una identidad no autorizada se rechaza sin revelar el contenido protegido.
- Si la aplicación no puede producir temporalmente una métrica particular, el endpoint no debe sustituir la ausencia de esa métrica con información sensible ni habilitar endpoints adicionales.
- `/actuator/health` permanece público solo con un estado mínimo; sus subendpoints y los demás endpoints de Actuator no quedan expuestos por HTTP.
- `/actuator/info` puede consultarse sin autenticación, pero su respuesta no debe incluir información sensible.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El backend MUST exponer `/actuator/prometheus` para que herramientas compatibles con Prometheus consulten métricas del backend; el acceso MUST limitarse a identidades autorizadas.
- **FR-002**: Las métricas expuestas MUST permitir monitorear el estado, los recursos y el rendimiento del backend.
- **FR-003**: El acceso a las métricas y a los endpoints de monitoreo protegidos MUST limitarse a identidades autorizadas mediante los mecanismos de seguridad existentes en la aplicación.
- **FR-004**: El backend MUST denegar el acceso no autorizado a los endpoints de monitoreo protegidos sin devolver su contenido.
- **FR-005**: La información expuesta por los endpoints de monitoreo MUST NOT incluir secretos, credenciales, tokens ni valores de configuración sensibles.
- **FR-006**: La exposición HTTP de Actuator MUST limitarse a `/actuator/prometheus`, `/actuator/health` y `/actuator/info`; los demás endpoints de Actuator, incluidos sus subendpoints, MUST permanecer inaccesibles por HTTP.
- **FR-007**: `/actuator/health` MUST permanecer accesible sin autenticación y limitar su respuesta a un estado mínimo, sin revelar detalles de componentes, dependencias o configuración.
- **FR-008**: Esta funcionalidad MUST reutilizar Actuator existente y MUST NOT incluir el despliegue de un servidor Prometheus ni la creación de dashboards de Grafana.
- **FR-009**: `/actuator/info` MUST permanecer accesible sin autenticación y MUST NOT exponer información sensible.

### Key Entities *(include if data involved)*

- **Métrica de monitoreo**: Medición del estado, los recursos o el rendimiento del backend presentada para su consulta por el equipo de operaciones.
- **Identidad operativa autorizada**: Identidad autenticada con permiso para consultar los endpoints de monitoreo protegidos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El endpoint `/actuator/prometheus` devuelve métricas que un recolector compatible puede interpretar, incluyendo las categorías de estado, recursos y rendimiento.
- **SC-002**: El 100 % de las solicitudes no autorizadas a métricas y endpoints protegidos son denegadas y no reciben el contenido protegido.
- **SC-003**: Ninguna respuesta HTTP de monitoreo contiene secretos, credenciales, tokens ni valores de configuración sensibles.
- **SC-004**: Por HTTP, `/actuator/prometheus` solo es accesible a identidades autorizadas, `/actuator/health` devuelve únicamente un estado mínimo, `/actuator/info` no expone información sensible y ningún otro endpoint de Actuator es accesible.

## Assumptions

- El backend ya cuenta con Actuator y con mecanismos de autenticación y autorización que deben reutilizarse.
- El acceso de operaciones se concede a identidades autorizadas por la seguridad existente; no se define un nuevo mecanismo de autenticación ni un nuevo rol de dominio en esta feature.
- Las métricas se consultan desde el backend por herramientas compatibles; instalar, desplegar u operar un servidor Prometheus queda fuera del alcance.
- Los endpoints de salud que deban seguir siendo públicos solo entregan un estado mínimo, sin detalles internos.

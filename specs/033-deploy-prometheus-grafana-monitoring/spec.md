# Feature Specification: Desplegar Prometheus y Grafana para monitoreo del backend

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `[feat/033-deploy-prometheus-grafana-monitoring]` — rama Git de la feature. El directorio de esta spec usa el nombre `033-deploy-prometheus-grafana-monitoring`.

**Created**: 2026-11-24

**Status**: Ready for implementation

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: User description: "Especifica la funcionalidad SPEC-033, ya materializada en `specs/033-deploy-prometheus-grafana-monitoring/`. Conserva la identidad y la rama existentes; no crees otra SPEC ni reasignes el identificador. Usa como fuente de alcance el issue #117 y el requisito REQ-S3-06. El equipo de operaciones necesita que Prometheus recolecte periódicamente las métricas ya expuestas por el backend en `/actuator/prometheus`, y que Grafana permita consultar mediante dashboards las métricas disponibles de CPU, JVM y solicitudes HTTP. La configuración necesaria y los dashboards quedan versionados de forma reproducible, y el acceso a Prometheus, Grafana y las métricas respeta las restricciones de seguridad acordadas. Reutiliza el endpoint y las métricas de SPEC-032 (#100); no dupliques ni amplíes su instrumentación. No agregues alertas, alta disponibilidad, nuevas métricas u otras capacidades que el issue no solicita. No des por decidido el mecanismo de despliegue, el contenido concreto de los dashboards ni los controles de acceso que el issue deja por aclarar."

**Trazabilidad**: REQ-S3-06 (issue #117). Dependencia funcional: SPEC-032 / REQ-S3-03 (issue #100).

## Clarifications

### Session 2026-11-24

- Q: ¿Qué contenido concreto deben ofrecer los dashboards? → A: Un dashboard sencillo con las visualizaciones que permite Grafana, para consultar las métricas disponibles de CPU, JVM y solicitudes HTTP.
- Q: ¿Qué mecanismo de despliegue debe utilizarse para Prometheus y Grafana? → A: Docker Compose para reproducir el despliegue de ambos servicios en un único host, sin alta disponibilidad.

### Session 2026-11-25

- Q: ¿Dónde se ejecutará y desde dónde se accederá a la solución? → A: Es una demostración local para cumplir el requisito universitario, no un despliegue remoto. Grafana solo se publica en `127.0.0.1`; Prometheus permanece dentro de la red de Compose. Se conserva la autenticación integrada de Grafana y no se versionan credenciales.
- Q: ¿Qué puertos se utilizan y qué autenticación requiere la recolección? → A: El backend existente se consulta en `host.docker.internal:8080`; Grafana se abre en `http://127.0.0.1:3000`; Prometheus usa su puerto interno `9090`, sin publicarlo en el host. `/actuator/prometheus` no requiere JWT, conforme a SPEC-032. Esto no cambia la autenticación de los demás endpoints del backend.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recolectar métricas del backend (Priority: P1)

El equipo de operaciones obtiene periódicamente las métricas ya expuestas por el backend, para consultar la evolución de CPU, JVM y solicitudes HTTP.

**Why this priority**: La recolección sostenida de métricas habilita el monitoreo operativo solicitado.

**Independent Test**: Observar más de una recolección en momentos distintos y confirmar que se dispone de muestras de las categorías de métricas ya publicadas por el backend.

**Acceptance Scenarios**:

1. **Given** que el backend publica sus métricas mediante `/actuator/prometheus` y el recolector puede alcanzarlo, **When** transcurre un ciclo de recolección configurado, **Then** el recolector obtiene y conserva las métricas disponibles sin requerir instrumentación adicional del backend.
2. **Given** que ocurren varios ciclos de recolección, **When** el equipo de operaciones consulta el resultado, **Then** puede distinguir muestras obtenidas en diferentes momentos para CPU, JVM y solicitudes HTTP.
3. **Given** que el endpoint del backend no está temporalmente disponible, **When** se intenta una recolección, **Then** no se presentan datos inexistentes como si se hubieran recolectado correctamente.

---

### User Story 2 - Consultar métricas mediante dashboards (Priority: P1)

El equipo de operaciones consulta desde dashboards las métricas recolectadas y su evolución temporal.

**Why this priority**: La consulta visual e histórica es el resultado operativo requerido junto con la recolección.

**Independent Test**: Abrir el dashboard sencillo, seleccionar un intervalo que contenga muestras y comprobar que permite consultar las métricas disponibles de CPU, JVM y solicitudes HTTP mediante las visualizaciones disponibles en Grafana.

**Acceptance Scenarios**:

1. **Given** que existen muestras recolectadas, **When** el equipo de operaciones consulta el dashboard sencillo para un intervalo con datos, **Then** puede consultar las métricas disponibles de CPU, JVM y solicitudes HTTP a lo largo del tiempo mediante las visualizaciones disponibles en Grafana.
2. **Given** que el origen de datos de monitoreo no está disponible o no contiene muestras en el intervalo seleccionado, **When** se consulta un dashboard, **Then** no se muestran valores como si fueran datos actuales válidos.

---

### User Story 3 - Reproducir localmente la demostración (Priority: P1)

La persona que prepara la demostración levanta Prometheus y Grafana localmente con configuración y dashboards versionados, y puede mostrar la recolección y consulta de las métricas.

**Why this priority**: La demostración local permite cumplir y mostrar el requisito universitario sin desplegar servicios remotos.

**Independent Test**: Levantar el proyecto con Docker Compose, abrir Grafana en `http://127.0.0.1:3000` y consultar el dashboard con muestras recolectadas del backend en el puerto `8080`.

**Acceptance Scenarios**:

1. **Given** el repositorio con la configuración y el dashboard versionados, **When** la persona ejecuta Docker Compose, **Then** puede reproducir Prometheus y Grafana localmente sin configuración manual no documentada.
2. **Given** que Grafana está levantado, **When** la persona abre `http://127.0.0.1:3000` en el mismo equipo, **Then** puede autenticarse y consultar el dashboard; el puerto no queda publicado en interfaces de red externas.
3. **Given** que Prometheus recolecta el endpoint de SPEC-032, **When** se realiza el scrape, **Then** no se requiere JWT y no se modifica la exposición ni autenticación del backend.
4. **Given** que la recolección y consulta están configuradas, **When** se inspecciona la solución, **Then** se reutilizan el endpoint y las métricas de SPEC-032 sin duplicar ni ampliar la instrumentación del backend.

### Edge Cases

- El endpoint del backend está temporalmente inaccesible o devuelve una recolección fallida; no se debe confundir ausencia de datos con una muestra válida.
- No existen muestras para el intervalo temporal seleccionado; el dashboard no debe presentar datos como si existieran.
- Se intenta acceder a Grafana desde otro equipo; la interfaz de demostración debe permanecer limitada al loopback del host.
- La solución se reproduce en un entorno sin configuración manual previa; los elementos necesarios para la reproducción deben estar versionados, excepto secretos o credenciales.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Prometheus MUST recolectar periódicamente las métricas disponibles del backend mediante el endpoint existente `/actuator/prometheus`.
- **FR-002**: La recolección MUST reutilizar exclusivamente el endpoint y las métricas definidas por SPEC-032; esta feature MUST NOT añadir, duplicar ni modificar instrumentación del backend.
- **FR-003**: Grafana MUST utilizar Prometheus como fuente para consultar las métricas recolectadas y permitir consultar su evolución a lo largo del tiempo.
- **FR-004**: Grafana MUST incluir al menos un dashboard sencillo que permita consultar, mediante visualizaciones disponibles en Grafana, las métricas disponibles de CPU, JVM y solicitudes HTTP.
- **FR-005**: Docker Compose MUST permitir reproducir en un único host el despliegue y la configuración de Prometheus y Grafana. La configuración necesaria y el dashboard requerido MUST quedar versionados; esta feature MUST NOT incluir alta disponibilidad.
- **FR-006**: Grafana MUST publicarse únicamente en `127.0.0.1:3000` y Prometheus MUST permanecer en la red de Compose, sin publicar su puerto en el host. El scrape de `/actuator/prometheus` MUST realizarse sin JWT, según SPEC-032; esta feature MUST NOT cambiar la autenticación ni exposición del backend. Los artefactos versionados MUST NOT exponer credenciales o secretos.
- **FR-007**: La funcionalidad MUST limitarse a una demostración local con Prometheus, Grafana y sus dashboards. MUST NOT incluir despliegue remoto, alertas, alta disponibilidad, nuevas métricas ni capacidades no solicitadas.

### Key Entities *(include if data involved)*

- **Muestra de métrica**: Valor de una métrica disponible del backend asociado al momento en que fue recolectado.
- **Dashboard**: Vista que permite al equipo de operaciones consultar, a lo largo del tiempo, las categorías de métricas acordadas.
- **Configuración de monitoreo**: Configuración versionada necesaria para reproducir localmente la recolección y la consulta, sin secretos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cada ciclo de recolección exitoso produce muestras de las métricas disponibles del backend, y las muestras de ciclos distintos se pueden distinguir por su momento de recolección.
- **SC-002**: Para un intervalo con datos, el equipo de operaciones puede consultar en un dashboard sencillo las métricas disponibles de CPU, JVM y solicitudes HTTP y observar su evolución temporal.
- **SC-003**: La configuración y el dashboard versionados permiten reproducir la solución en un único host sin configuración manual no documentada ni secretos en el repositorio.
- **SC-004**: Grafana solo acepta conexiones locales en `127.0.0.1:3000`, Prometheus no publica un puerto al host y el scrape de `/actuator/prometheus` funciona sin JWT conforme a SPEC-032.

## Assumptions

- El backend y `/actuator/prometheus` están disponibles conforme al alcance de SPEC-032; esta feature los consume y no implementa ni modifica ese endpoint.
- Las categorías solicitadas se limitan a las métricas ya disponibles en SPEC-032 para CPU, JVM y solicitudes HTTP; no se agregan métricas de negocio ni métricas personalizadas.
- El contenido del dashboard se limita a una vista sencilla con las visualizaciones disponibles en Grafana; no se requieren paneles, agrupaciones o filtros específicos adicionales.
- La demostración se ejecuta en el mismo equipo: Grafana se enlaza a `127.0.0.1:3000` y Prometheus no publica su puerto. No se configura acceso remoto.
- El backend disponible en el host usa el puerto `8080`; Prometheus lo alcanza mediante `host.docker.internal`.
- `/actuator/prometheus` es público y no requiere ni utiliza JWT, conforme a SPEC-032. Esta decisión se limita al endpoint de métricas y no cambia la autenticación de otros endpoints del backend.
- Docker Compose se utiliza para reproducir el despliegue en un único host; no se define una plataforma de alta disponibilidad.
- Las credenciales y demás secretos necesarios para operar los servicios no se incluyen en la configuración versionada.
- Alertas, alta disponibilidad y otras capacidades no mencionadas por el issue quedan fuera del alcance.

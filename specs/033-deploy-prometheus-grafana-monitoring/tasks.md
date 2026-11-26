# Tareas: Desplegar Prometheus y Grafana para monitoreo del backend

**Entrada**: artefactos de diseño en `specs/033-deploy-prometheus-grafana-monitoring/`.

**Prerrequisitos**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/` y `quickstart.md`.

**Alcance de verificación**: los criterios independientes describen la validación operativa que realizará el usuario según `quickstart.md`. Esta feature solo modifica configuración de despliegue; no requiere cambios ni tests de aplicación backend y no crea tests o infraestructura frontend. El agente no ejecuta tests ni el stack.

**Organización**: tareas agrupadas por User Story para mantener la trazabilidad con `spec.md`. La configuración base compartida precede a las historias.

## Fase 1: Setup

**Propósito**: inicialización del proyecto.

No se requieren tareas independientes de inicialización: el repositorio existente y la estructura del proyecto están disponibles. La infraestructura compartida de monitoreo se establece en la fase Foundational.

---

## Fase 2: Foundational

**Propósito**: crear la base compartida que necesitan las User Stories.

- [X] T001 Crear `monitoring/compose.yml` como proyecto Compose separado con servicios Prometheus y Grafana, imágenes fijadas a `prom/prometheus:v3.16.0` y `grafana/grafana:13.3.2`, red bridge privada que permita alcanzar el host (sin `internal: true`), volúmenes nombrados para `/prometheus` y `/var/lib/grafana`, y mounts de solo lectura para `monitoring/prometheus/prometheus.yml` en `/etc/prometheus/prometheus.yml` y para `monitoring/grafana/provisioning/` en `/etc/grafana/provisioning/`. Montar `monitoring/grafana/dashboards/` en `/etc/grafana/dashboards/`, mantener Prometheus sin puerto publicado en el host y preparar el acceso del contenedor al backend mediante `host.docker.internal:8080` y `host-gateway` cuando sea necesario.

**Dependencia**: T001 bloquea las tareas de las tres historias.

---

## Fase 3: User Story 1 — Recolectar métricas del backend (Priority: P1)

**Goal**: Prometheus recolecta periódicamente y conserva las métricas existentes del backend, sin modificar su instrumentación.

**Independent Test**: conforme a `spec.md` y `quickstart.md`, observar más de un ciclo de recolección y muestras de las métricas disponibles de CPU, JVM y HTTP con tiempos de recolección distintos. Si el endpoint no está disponible, no deben aparecer muestras nuevas como si el scrape hubiera sido exitoso.

### Implementación

- [X] T002 [P] [US1] Configurar `monitoring/prometheus/prometheus.yml` con un job estático para `host.docker.internal:8080`, ruta `/actuator/prometheus` e intervalo de recolección de `1m`; reutilizar el endpoint de SPEC-032 sin autenticación, métricas, exporters ni instrumentación adicionales.

**Checkpoint**: la recolección debe poder validarse de forma independiente de los dashboards.

---

## Fase 4: User Story 2 — Consultar métricas mediante dashboards (Priority: P1)

**Goal**: Grafana consulta Prometheus y ofrece un dashboard sencillo con la evolución temporal de las métricas disponibles de CPU, JVM y solicitudes HTTP.

**Independent Test**: abrir el dashboard provisionado en un intervalo con muestras y comprobar las visualizaciones temporales de las tres categorías. Si el datasource no está disponible o no existen muestras, Grafana debe indicar ausencia de datos o error, sin presentar valores inventados.

### Implementación

- [X] T003 [P] [US2] Aprovisionar `monitoring/grafana/provisioning/datasources/prometheus.yml` con Prometheus como fuente de datos mediante el DNS interno del servicio Compose y un UID estable `prometheus`.
- [X] T004 [P] [US2] Aprovisionar `monitoring/grafana/provisioning/dashboards/provider.yml` para cargar en modo de solo lectura el dashboard versionado bajo `/etc/grafana/dashboards`, usando un intervalo de actualización de 30 segundos.
- [X] T005 [P] [US2] Crear `monitoring/grafana/dashboards/backend-monitoring.json` con UID estable y visualizaciones temporales de las métricas disponibles `system_cpu_usage`, `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_threads_live_threads`, `jvm_gc_pause_seconds` y `http_server_requests_seconds_count`/`http_server_requests_seconds_sum` cuando estén disponibles. Referenciar el datasource UID `prometheus`; no agregar métricas, labels, percentiles, alertas ni rellenar datos ausentes con cero.

**Checkpoint**: el dashboard debe poder consultarse con muestras recolectadas, sin que su definición dependa de cambios manuales en la UI de Grafana.

---

## Fase 5: User Story 3 — Reproducir localmente la demostración (Priority: P1)

**Goal**: reproducir localmente Prometheus y Grafana desde configuración versionada, con Grafana accesible solo mediante loopback.

**Independent Test**: usar Docker Compose según `quickstart.md`; comprobar que Grafana abre en `http://127.0.0.1:3000` con autenticación, que no publica en interfaces externas, que Prometheus no publica un puerto al host y que no hay credenciales versionadas. Verificar que el scrape de `/actuator/prometheus` no requiere JWT conforme a SPEC-032.

### Implementación

- [X] T006 [P] [US3] Completar `monitoring/compose.yml` para publicar Grafana por HTTP únicamente en `127.0.0.1:3000`, requerir `GRAFANA_ADMIN_USER` y `GRAFANA_ADMIN_PASSWORD` externos, y deshabilitar el acceso anónimo y el registro abierto. No publicar Prometheus ni incluir secretos.

- [X] T007 [P] [US3] Actualizar `docs/backend/technologies.md` con Docker Compose v2 y las imágenes `prom/prometheus:v3.16.0` y `grafana/grafana:13.3.2`, documentando su propósito en la demostración local de SPEC-033.

**Checkpoint**: la reproducción depende de configuración versionada y de las credenciales locales de Grafana suministradas fuera del repositorio.

---

## Fase final: Polish y aspectos transversales

No se identifican tareas adicionales fuera de las historias: el versionado, la ausencia de secretos y los límites de alcance están cubiertos por las tareas anteriores y por `quickstart.md`.

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Fase 1)**: no requiere trabajo de inicialización.
- **Foundational (Fase 2)**: T001 crea la base compartida y bloquea las tareas de las historias.
- **User Stories (Fases 3–5)**: sus tareas de archivos distintos pueden avanzar en paralelo después de T001. La validación de US2 requiere muestras disponibles de US1; US3 se valida en el mismo equipo.
- **T007**: puede ejecutarse en paralelo con T006; ambos actualizan archivos distintos.
- **Polish (Fase final)**: no tiene tareas adicionales identificadas.

### Orden por User Story

- **US1 (P1)**: después de T001; no depende de otras historias.
- **US2 (P1)**: después de T001. Su configuración puede desarrollarse en paralelo con US1, aunque su validación con datos requiere muestras de US1.
- **US3 (P1)**: la configuración local de Compose puede desarrollarse después de T001; la validación integral requiere las configuraciones de US1 y US2 y las credenciales locales de Grafana.

### Oportunidades de paralelización

- Después de T001, T002, T003, T004, T005, T006 y T007 modifican archivos distintos y pueden realizarse en paralelo, salvo que T001 debe completarse antes de T006 porque ambos configuran `monitoring/compose.yml`.
- T003 y T005 usan el UID acordado `prometheus`; T004 apunta a la ruta acordada `/etc/grafana/dashboards`.
- No hay tareas paralelizables adicionales dentro de US1 o US3.

### Ejemplo de ejecución paralela: User Story 2

Después de T001, se pueden crear en paralelo:

- T003: datasource Prometheus en `monitoring/grafana/provisioning/datasources/prometheus.yml`.
- T004: proveedor de dashboards en `monitoring/grafana/provisioning/dashboards/provider.yml`.
- T005: dashboard JSON en `monitoring/grafana/dashboards/backend-monitoring.json`.

## Estrategia de implementación

### MVP

1. Completar T001 (infraestructura compartida).
2. Completar T002 (US1): recolección periódica desde el endpoint existente.
3. Validar US1 con el procedimiento que ejecutará el usuario; no ejecutar tests ni el stack desde el agente.

### Entrega incremental

1. T001 habilita la configuración independiente de las historias.
2. US1 entrega recolección y almacenamiento local de las métricas existentes.
3. US2 agrega datasource y dashboard versionados para consultarlas.
4. US3 completa el acceso local autenticado y el inventario tecnológico correspondiente.
5. La finalización de tareas no equivale a aprobación de tests, revisión ni cierre de la funcionalidad.

## Phase 6: Convergence

- [ ] T008 Revisar y justificar o retirar el `.dockerignore` raíz ajeno al alcance de SPEC-033 per FR-007 (unrequested)

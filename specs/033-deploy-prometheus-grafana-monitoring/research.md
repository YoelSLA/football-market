# Investigación: despliegue de Prometheus y Grafana

Investigación para PLAN de SPEC-033, consultada el 2026-11-24. Las decisiones desarrollan los
requisitos vigentes; no agregan alcance funcional.

## Decisiones

### 1. Proyecto Docker Compose independiente

- **Decisión**: ubicar el stack en `monitoring/compose.yml` y en esa carpeta sus configuraciones.
- **Rationale**: el `compose.yml` raíz existente administra PostgreSQL de desarrollo. Mantener dos
  proyectos evita acoplar el ciclo de vida y los datos de PostgreSQL con el despliegue operativo de
  monitoreo. La red bridge de Compose permite a Grafana resolver Prometheus por nombre de servicio.
- **Alternativas consideradas**: extender o sobreescribir el Compose raíz; descartado porque incluye
  una dependencia de desarrollo que no pertenece a la feature. No se agrega service discovery:
  existe un solo host y un target backend.
- **Fuentes**: [redes de Docker Compose](https://docs.docker.com/compose/how-tos/networking/),
  [nombres de proyecto Compose](https://docs.docker.com/compose/how-tos/project-name/),
  [configuración del Compose raíz](../../compose.yml).

### 2. Versiones fijadas de imágenes

- **Decisión**: iniciar con `prom/prometheus:v3.16.0` y `grafana/grafana:13.3.2`, versiones
  exactas disponibles en la fecha de planificación. No usar `latest`; actualizar cada versión como
  un cambio deliberado.
- **Rationale**: tags versionados hacen reproducible la configuración de un despliegue y evitan
  cambios implícitos entre ejecuciones. La imagen `grafana/grafana` corresponde a Grafana OSS.
- **Alternativas consideradas**: tags flotantes `latest`, descartados por permitir cambios
  inesperados; digests SHA256, que fijan también el contenido exacto de la imagen, pero requieren
  actualizar referencias opacas en cada parche. La versión exacta es suficiente para el alcance
  actual y los digests pueden adoptarse por política operativa futura.
- **Fuentes**: [Prometheus 3.16.0](https://github.com/prometheus/prometheus/releases/tag/v3.16.0),
  [Grafana 13.3.2](https://github.com/grafana/grafana/releases/tag/v13.3.2),
  [imagen Docker de Grafana](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/).

### 3. Scrape estático y periodicidad

- **Decisión**: una configuración Prometheus con un job para el backend, ruta
  `/actuator/prometheus`, target `host.docker.internal:8080` y un intervalo de scrape explícito de
  `1m`. En plataformas que lo requieran, `extra_hosts` mapeará `host.docker.internal` a
  `host-gateway`.
- **Rationale**: Prometheus documenta scrape estático y `1m` como valores predeterminados actuales.
  Usar el default no crea un SLA de frescura ni impone una carga de recolección más frecuente sin
  necesidad. El puerto 8080 proviene de la configuración efectiva del backend. Dentro del
  contenedor, `localhost` no es el host.
- **Alternativas consideradas**: service discovery, que agrega complejidad para un solo target;
  endpoint `localhost`, que resolvería al propio contenedor; cambiar el endpoint o añadir
  autenticación, prohibido por FR-006 y por el contrato vigente de SPEC-032.
- **Contrato de métricas**: SPEC-032 declara las categorías y el test de integración actual confirma
  las familias `system_cpu_usage`, `process_cpu_usage`, `jvm_memory_used_bytes`,
  `jvm_threads_live_threads`, `jvm_gc_pause_seconds` y `http_server_requests_seconds_count`.
  La pausa GC puede no tener muestras hasta que ocurra un evento. Los paneles se basarán solo en
  esas métricas y sus componentes de timer disponibles (`_count` y `_sum`), sin inventar métricas,
  labels ni percentiles.
- **Fuentes**: [configuración Prometheus](https://prometheus.io/docs/prometheus/latest/configuration/configuration/),
  [métricas Prometheus de Spring Boot](https://docs.spring.io/spring-boot/reference/actuator/metrics.html#actuator.metrics.export.prometheus),
  [redes y host-gateway de Compose](https://docs.docker.com/compose/how-tos/networking/),
  [evidencia de familias de SPEC-032](../../backend/src/test/java/footballmarket/features/monitoring/presentation/ActuatorMetricsHttpIntegrationTest.java),
  [spec de SPEC-032](../032-prometheus-actuator-monitoring/spec.md).

### 4. Persistencia local sin requisitos nuevos de retención

- **Decisión**: volumen nombrado de Compose para la TSDB bajo `/prometheus`; no establecer una
  retención propia ni almacenamiento remoto. Persistir también el estado local de Grafana.
- **Rationale**: las muestras deben seguir disponibles tras recrear contenedores. El volumen local
  satisface ese objetivo para el host único; la retención sigue el default de la versión fijada, sin
  inventar una duración de producto.
- **Alternativas consideradas**: filesystem efímero, que perdería muestras al recrear el contenedor;
  almacenamiento remoto o replicado, excluido por el alcance de un host sin alta disponibilidad.
- **Fuentes**: [almacenamiento de Prometheus](https://prometheus.io/docs/prometheus/latest/storage/),
  [volúmenes Docker](https://docs.docker.com/engine/storage/volumes/).

### 5. Aprovisionamiento declarativo del dashboard

- **Decisión**: provisionar una fuente de datos Prometheus y un dashboard JSON con la funcionalidad
  clásica de provisioning de Grafana. Asignar UIDs estables, impedir la edición del dashboard
  provisionado en UI y usar polling mayor a 10 segundos para detectar cambios a través de bind
  mounts.
- **Rationale**: Grafana soporta provisioning de fuentes y dashboards desde archivos versionables;
  no se precisa una herramienta de administración adicional. Los paneles mostrarán gráficas
  temporales de CPU, JVM y HTTP. Resultados ausentes y errores de datasource permanecerán visibles
  como “sin datos” o error; no se rellenarán con cero ni con valores actuales inventados.
- **Alternativas consideradas**: configurar la fuente y el dashboard manualmente en la UI, que no
  da reproducibilidad; Terraform o Git Sync, que agregan tooling no requerido; habilitar
  `allowUiUpdates`, que separa la edición de su fuente versionada.
- **Fuentes**: [provisioning de Grafana](https://grafana.com/docs/grafana/latest/administration/provisioning/),
  [datasource Prometheus de Grafana](https://grafana.com/docs/grafana/latest/datasources/prometheus/configure/),
  [manejo de staleness en Prometheus](https://prometheus.io/docs/prometheus/latest/querying/basics/#staleness).

### 6. Alcance local, acceso y credenciales

- **Decisión**: la demostración se ejecuta en el mismo equipo. Prometheus no publica puerto al host;
  Grafana enlaza HTTP a `127.0.0.1:3000`, mantiene su autenticación integrada y recibe la credencial
  inicial desde el entorno. El puerto `9090` de Prometheus queda solo en la red de Compose.
- **Rationale**: el acceso por loopback satisface la demostración sin desplegar servicios en una red
  operativa remota. No se necesita certificado TLS ni una regla de firewall externa para Grafana,
  porque el puerto no se enlaza a interfaces externas. Mantener Prometheus sin puerto publicado
  conserva una superficie mínima y Grafana basta para visualizar las muestras.
- **Conectividad al backend**: la red bridge de Compose debe permitir que Prometheus alcance
  `host.docker.internal:8080`; no debe configurarse como aislada con `internal: true`. En plataformas
  que lo requieran, `extra_hosts` mapeará `host.docker.internal` a `host-gateway`.
- **Límite heredado**: `/actuator/prometheus` permanece público y no requiere ni utiliza JWT,
  conforme a SPEC-032. El bind local de Grafana no cambia la exposición del endpoint del backend.
- **Alternativas consideradas**: publicar Prometheus al host, innecesario para la demostración por
  dashboard; enlazar Grafana a `0.0.0.0` o a una interfaz operativa, descartado porque la solución no
  se despliega remotamente; añadir TLS y firewall remoto, descartado por no haber acceso remoto.
- **Fuentes**: [redes y host-gateway de Compose](https://docs.docker.com/compose/how-tos/networking/),
  [publicación de puertos Docker](https://docs.docker.com/engine/network/port-publishing/),
  [SPEC-032](../032-prometheus-actuator-monitoring/spec.md).

## Parámetros operativos externos al repositorio

El equipo local debe proveer Docker Engine/Compose, el backend escuchando en el puerto `8080` y la
credencial inicial de Grafana fuera del repositorio. No se requieren valores de red operativa,
certificados TLS ni reglas de firewall externas.

# Implementation Plan: Desplegar Prometheus y Grafana para monitoreo del backend

**Branch**: `feat/033-deploy-prometheus-grafana-monitoring` | **Date**: 2026-11-24 | **Spec**: [spec.md](spec.md)

<!-- MUST usar rama real según Constitution §V y seguir docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente la arquitectura, diseño y decisiones
técnicas vigentes y aplicables al sistema (Constitución §V). El historial de decisiones técnicas
superadas se delega a Git. Forma parte de la tríada permanente obligatoria junto a spec.md y tasks.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `specs/033-deploy-prometheus-grafana-monitoring/spec.md`

## Summary

Levantar Prometheus y Grafana localmente en un proyecto Docker Compose independiente, reutilizando sin
cambios el endpoint público `/actuator/prometheus` de SPEC-032. Prometheus recolectará el backend cada
minuto y persistirá las muestras en un volumen local. Grafana aprovisionará desde archivos una fuente
de datos Prometheus y un dashboard sencillo para las métricas disponibles de CPU, JVM y solicitudes
HTTP.

Grafana será la única interfaz publicada por el stack, enlazada exclusivamente a `127.0.0.1:3000`,
con su autenticación integrada habilitada. Se usa HTTP porque la interfaz solo es accesible mediante
loopback y no forma parte de un despliegue remoto. Prometheus permanecerá en la red privada de Compose
y no publicará su puerto. El scrape llega al backend del host por `host.docker.internal:8080`, sin
JWT, conforme al contrato de SPEC-032. No se modifica el backend ni se agregan métricas, alertas o
alta disponibilidad.

## Technical Context

**Language/Version**: YAML para Docker Compose y configuración de Prometheus/Grafana; JSON para el
dashboard. No cambia el lenguaje de la aplicación.

**Primary Dependencies**: Docker Compose v2; `prom/prometheus:v3.16.0`;
`grafana/grafana:13.3.2` (Grafana OSS). El inventario tecnológico se mantiene en
`docs/backend/technologies.md`; no se agrega dependencia a la aplicación backend.

**Storage**: Volumen nombrado local para la TSDB de Prometheus y volumen para el estado local de
Grafana. No hay base de datos ni persistencia de dominio nuevas. Se conserva la retención
predeterminada de la versión fijada de Prometheus.

**Testing**: Validación manual por el usuario según [quickstart.md](quickstart.md). El agente no
ejecuta tests, builds ni el stack.

**Target Platform**: Equipo local con Docker Engine y Docker Compose v2. Prometheus debe poder
alcanzar el backend del host por `host.docker.internal:8080`; Grafana se consulta desde el mismo
equipo en `http://127.0.0.1:3000`.

**Project Type**: Configuración de despliegue de monitoreo para un único host.

**Performance Goals**: No hay objetivos nuevos de latencia, frescura ni carga. Se usa un ciclo de
recolección de 1 minuto, el valor predeterminado actual de Prometheus, sin convertirlo en un SLA.

**Constraints**: Demostración local en un host, sin despliegue remoto ni alta disponibilidad; solo un
target de backend; sin alertas, exporters, instrumentación adicional ni secretos versionados.
Grafana queda enlazado a loopback, conserva autenticación integrada y recibe su credencial inicial
fuera del repositorio. No se requiere certificado TLS ni regla de firewall para acceso remoto.

**Scale/Scope**: Dos servicios de contenedor, un target de métricas, una fuente de datos y un
dashboard. No se cambia el backend, el frontend ni la política de acceso de SPEC-032.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Antes de Phase 0

- **Principios I y VI — simplicidad y mínimo alcance**: PASS. Se reutilizan el endpoint y las
  métricas existentes; se emplean únicamente Compose, Prometheus y Grafana, sin proxy, exporter ni
  capa de aplicación adicional.
- **Principios II y VI — proporcionalidad**: PASS. El diseño cubre FR-001–FR-007; excluye
  explícitamente alertas, alta disponibilidad, métricas nuevas y capacidades no solicitadas.
- **Principio III — seguridad e integridad**: PASS. Grafana se enlaza únicamente a loopback y mantiene
  la autenticación integrada; Prometheus no publica un puerto al host y las credenciales quedan
  fuera del repositorio. No se configura TLS ni firewall remoto porque la interfaz no se expone a
  otras interfaces de red. Se conserva el acceso público sin JWT de `/actuator/prometheus`, conforme
  al contrato de SPEC-032; esta feature no cambia la seguridad del backend.
- **Principios IV y V — contratos y trazabilidad**: PASS. Se referencia el contrato vigente de
  SPEC-032 sin redefinirlo y se documenta únicamente la interfaz local nueva. La spec y el
  seguimiento no se modifican en PLAN.
- **Verificación y autorizaciones**: PASS. No se ejecutan tests ni builds; PLAN no autoriza
  IMPLEMENT ni fases posteriores.

## Project Structure

### Documentation (this feature)

```text
specs/033-deploy-prometheus-grafana-monitoring/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── monitoring-operator.md
└── tasks.md              # Fase TASKS; no se crea ni modifica en PLAN
```

### Configuración de despliegue (raíz del repositorio)

```text
monitoring/
├── compose.yml
├── prometheus/
│   └── prometheus.yml
└── grafana/
    ├── provisioning/
    │   ├── datasources/prometheus.yml
    │   └── dashboards/provider.yml
    └── dashboards/backend-monitoring.json
```

**Structure Decision**: Mantener `compose.yml` de la raíz, que actualmente gestiona PostgreSQL de
desarrollo, independiente del ciclo operativo de monitoreo. Agrupar el Compose propio y sus
configuraciones relativas bajo `monitoring/`. Compose conectará Grafana con Prometheus por DNS de
servicio. El target del backend se declarará en la configuración versionada de Prometheus; para el
backend local se usará `host.docker.internal:8080` con `extra_hosts: host-gateway` donde sea
necesario.

Grafana provisionará su fuente Prometheus y el dashboard desde archivos de solo lectura. Publicará
HTTP únicamente en `127.0.0.1:3000`; la cuenta administrativa se suministra por fuera del
repositorio. Prometheus mantendrá su puerto `9090` solo en la red Compose. La red bridge permitirá
que Prometheus alcance el backend del host por `host.docker.internal:8080`; no debe configurarse
como aislada mediante `internal: true`.

### Constitution Check después de Phase 1

- **Principios I y VI — simplicidad y mínimo alcance**: PASS. Dos servicios, una recolección
  estática y un dashboard; no se introduce un exporter para CPU del host ni una herramienta de
  aprovisionamiento adicional.
- **Principios II y VI — alcance**: PASS. Las visualizaciones se limitan a las familias de CPU,
  JVM y HTTP ya disponibles; no agregan alertas, percentiles, métricas de negocio ni alta
  disponibilidad. La ausencia de muestras se muestra como ausencia/error, nunca como cero válido.
- **Principio III — seguridad**: PASS. Grafana queda enlazado a loopback y conserva autenticación;
  Prometheus no publica un puerto al host y no hay secretos versionados. No se requieren TLS ni
  firewall remoto para una interfaz no expuesta fuera del equipo. El endpoint de SPEC-032 conserva su
  política pública sin JWT; no se modifica desde esta feature.
- **Principio IV — coherencia de contratos**: PASS. El contrato nuevo describe la interfaz
  operativa de Grafana y el aislamiento de Prometheus; el contrato HTTP del backend sigue siendo el
  de [SPEC-032](../032-prometheus-actuator-monitoring/contracts/actuator-monitoring.md).
- **Principio V — trazabilidad/documentación viva**: PASS. Modelo, research, contrato y quickstart
  describen el diseño vigente de esta spec. `tasks.md`, checklists, spec y seguimiento quedan fuera
  de esta fase.
- **Verificación y autorizaciones**: PASS. La guía es para ejecución humana. No se ejecutaron tests,
  builds, Compose ni controles de red; el plan no autoriza IMPLEMENT, CHECKLIST, TASKS ni ANALYZE.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No aplica: los gates constitucionales se satisfacen con la configuración propuesta y sus
prerrequisitos operativos documentados.

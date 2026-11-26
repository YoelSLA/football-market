# Modelo de datos: monitoreo del backend

La feature no introduce entidades de dominio, esquema relacional ni migraciones. Prometheus conserva
localmente series temporales en su TSDB; Grafana consume esas series y provisiona dashboards desde
archivos.

## Conceptos

### Muestra de métrica

- **Propósito**: valor observado de una métrica del backend en el momento del scrape.
- **Representación**: nombre y tipo de familia, valor, labels emitidos por SPEC-032 y timestamp de
  recolección asignado por Prometheus.
- **Cobertura**: métricas disponibles de CPU del sistema/proceso; memoria, hilos y pausas de GC de
  JVM; recuento y duración de solicitudes HTTP. No incluye métricas nuevas.
- **Ciclo de vida**: recolectada periódicamente por Prometheus y almacenada en su TSDB local, con la
  política de retención predeterminada de la versión fijada.

### Fuente de datos Prometheus

- **Propósito**: conexión aprovisionada que permite a Grafana consultar Prometheus.
- **Identidad**: UID estable mantenido en el archivo de provisioning.
- **Conectividad**: nombre DNS del servicio Compose y puerto interno; no requiere credenciales entre
  los servicios del stack.
- **Persistencia**: configuración versionada, no ingresada manualmente en UI.

### Dashboard de monitoreo del backend

- **Propósito**: consulta temporal sencilla de las métricas disponibles de CPU, JVM y solicitudes
  HTTP.
- **Representación**: JSON provisionado con UID estable; las consultas usan las familias
  disponibles de SPEC-032. No genera ni persiste métricas propias.
- **Ausencia de datos**: muestra el estado vacío o error de datasource; no sustituye datos ausentes
  por cero.
- **Persistencia**: archivo versionado; Grafana conserva su estado local en un volumen nombrado.

### Configuración del stack

- **Propósito**: describir los servicios, volúmenes, puertos y mounts requeridos para reproducir
  Prometheus y Grafana en un host.
- **Representación**: Compose YAML, configuración scrape de Prometheus y archivos de provisioning
  de Grafana.
- **Acceso local**: Grafana se enlaza a `127.0.0.1:3000`; Prometheus no publica su puerto al host.
- **Credenciales**: no contiene secretos. La cuenta inicial de Grafana se suministra externamente.

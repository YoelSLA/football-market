# Contrato operativo de monitoreo

Este contrato cubre la interfaz nueva de SPEC-033. El contrato HTTP de
`GET /actuator/prometheus` y su política de acceso pertenecen a
[SPEC-032](../../032-prometheus-actuator-monitoring/contracts/actuator-monitoring.md) y no se
redefinen aquí.

## Grafana

- **Interfaz**: UI web de Grafana, servida por HTTP en `127.0.0.1:3000` para demostración local.
- **Acceso**: únicamente desde el mismo equipo mediante loopback; no se publica en interfaces de red
  externas. Prometheus no publica su puerto al host.
- **Autenticación**: requiere la autenticación incorporada de Grafana. No se habilita el acceso
  anónimo ni el registro abierto. La credencial inicial se entrega por el entorno y no forma parte
  de los archivos versionados.
- **TLS**: no se configura para esta interfaz local, que no acepta conexiones por interfaces de red
  externas.
- **Dashboard**: ofrece visualizaciones temporales de las métricas disponibles de CPU, JVM y HTTP.
  Su definición y fuente de datos están aprovisionadas desde archivos versionados y no se editan
  desde la UI como fuente de verdad.
- **Sin datos o datasource no disponible**: Grafana conserva su indicación de “sin datos” o error;
  no muestra un cero ni un valor fabricado como medición actual.

## Prometheus

- **Función**: recolecta el backend por el endpoint existente `/actuator/prometheus` y almacena las
  muestras en TSDB local.
- **Acceso de red**: permanece dentro de la red Compose; no se publica un puerto al host. Grafana
  consulta su servicio por DNS interno. La UI/API directa de Prometheus no es una interfaz de
  operador cubierta por esta feature.
- **Acceso al target**: se recolecta `host.docker.internal:8080/actuator/prometheus` en la red bridge
  de Compose, preparada para que Prometheus alcance el host.
- **Autenticación del scrape**: `/actuator/prometheus` no requiere ni utiliza JWT, conforme a
  SPEC-032. Esta decisión no cambia la autenticación de los demás endpoints del backend.
- **Target temporalmente inaccesible**: los ciclos fallidos no se representan como muestras nuevas
  válidas; los paneles reflejan ausencia de datos.

## Límite heredado de las métricas del backend

El endpoint del backend mantiene el acceso público sin JWT que especifica SPEC-032 para cualquier
cliente que pueda alcanzar el backend. El bind local de Grafana y el aislamiento de Prometheus no
cambian ni restringen la exposición del endpoint del backend.

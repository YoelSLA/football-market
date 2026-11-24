# Contrato HTTP: monitoreo Actuator

Este contrato cubre la superficie HTTP de Actuator correspondiente a SPEC-032. Los endpoints de
dominio conservan sus contratos propios.

## `GET /actuator/prometheus`

- **Propósito**: exponer mediciones del backend en formato scrape compatible con Prometheus.
- **Autenticación**: se requiere un JWT Bearer válido aceptado por el mecanismo existente. Cualquier
  identidad autenticada puede acceder; no se exige un rol ni permiso adicional.
- **Respuesta satisfactoria**: `200`; contenido de texto en formato Prometheus con métricas de
  recursos y rendimiento estándar de Spring Boot/Micrometer: uso de CPU del sistema y del proceso;
  memoria, hilos y pausas de GC de la JVM cuando se producen eventos de GC; y recuento y duración de solicitudes HTTP. Los
  resultados HTTP permiten obtener la tasa de errores. No se requieren métricas personalizadas,
  métricas de negocio ni percentiles de latencia.
- **Sin autenticación válida**: solicitante anónimo o token inválido recibe `401`, sin métricas. No
  se distingue entre identidades autenticadas por roles o permisos y no se exige `403` por ese motivo.
- **Seguridad de contenido**: no incluye secretos, credenciales, tokens ni configuración sensible.

## `GET /actuator/health`

- **Autenticación**: no requerida.
- **Respuesta satisfactoria**: `200` con estado general mínimo, por ejemplo
  `{"status":"UP"}`; no incluye componentes, dependencias ni configuración.
- **Propósito**: informar por separado el estado general del backend; no se exige duplicarlo como
  métrica de Prometheus.
- **Subendpoints**: `/actuator/health/**` no se exponen por HTTP.

## `GET /actuator/info`

- **Autenticación**: no requerida.
- **Respuesta satisfactoria**: `200` con el nombre y descripción públicos estáticos:
  `{"app":{"name":"Football Market","description":"API de mercado de jugadores de fútbol"}}`.
  No añadir datos de build, entorno u otra configuración.
- **Subendpoints**: no se exponen por HTTP.

## Resto de Actuator

Los demás endpoints, enlaces raíz `/actuator` y subendpoints no descritos arriba no se exponen por
HTTP. El inventario HTTP queda limitado a `prometheus`, `health` e `info`; su configuración no
utiliza comodín ni habilitadores que permitan ampliar esa lista accidentalmente.

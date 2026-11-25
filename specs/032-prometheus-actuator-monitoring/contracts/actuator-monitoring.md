# Contrato HTTP: monitoreo Actuator

Este contrato cubre la superficie HTTP de Actuator correspondiente a SPEC-032. Los endpoints de
dominio conservan sus contratos propios.

## `GET /actuator/prometheus`

- **Propósito**: exponer mediciones del backend en formato scrape compatible con Prometheus.
- **Autenticación**: no requiere JWT, identidad, rol ni permiso de aplicación. Cualquier cliente que
  alcance el backend puede consultar el endpoint. La presencia o validez de un JWT no afecta el
  acceso; el endpoint no lo requiere ni lo utiliza.
- **Respuesta satisfactoria**: `200`; contenido de texto en formato Prometheus con métricas
  estándar de Spring Boot/Micrometer: uso de CPU del sistema y del proceso; memoria,
  hilos y pausas de GC de la JVM cuando se producen eventos de GC; y recuento y duración de
  solicitudes HTTP. Los resultados HTTP permiten obtener la tasa de errores. No se requieren
  métricas personalizadas, métricas de negocio ni percentiles de latencia.
- **Acceso anónimo**: solicitudes sin encabezado de autorización reciben `200` con las métricas
  disponibles; un encabezado JWT no es necesario.
- **JWT presentado**: solicitudes con JWT válido, vencido o inválido reciben el mismo `200` y las
  mismas métricas que una solicitud sin JWT.
- **Seguridad de contenido**: no incluye secretos, credenciales, tokens ni configuración sensible.

## `GET /actuator/health`

- **Autenticación**: no requerida.
- **Respuesta satisfactoria**: `200` con estado general mínimo, por ejemplo
  `{"status":"UP"}`; no incluye componentes, dependencias ni configuración.
- **Configuración**: detalles y componentes permanecen ocultos.
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
utiliza comodín ni habilitadores que permitan ampliar esa lista accidentalmente. Esta restricción
no modifica la autenticación de los endpoints de dominio.

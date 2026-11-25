# Modelo de datos: métricas Prometheus y monitoreo seguro

La feature no introduce datos persistidos, tablas, migraciones ni entidades de dominio. Actuator y
Micrometer producen las mediciones en memoria y las serializan para la consulta HTTP.

## Conceptos expuestos

### Métrica de monitoreo

- **Propósito**: medición de recursos o rendimiento del backend. El estado general se consulta
  separadamente mediante `/actuator/health`.
- **Representación**: nombre, tipo, valor y etiquetas según el formato de exposición de Micrometer
  compatible con Prometheus.
- **Cobertura**: CPU del sistema y del proceso; memoria, hilos y pausas de GC de JVM; recuento y
  duración de solicitudes HTTP. La tasa de errores se obtiene de los resultados de las solicitudes.
- **Ciclo de vida**: observada por la instrumentación estándar de Spring Boot/Micrometer durante la
  ejecución de la aplicación; consultable en `/actuator/prometheus`.
- **Persistencia**: ninguna en esta feature.

### Cliente solicitante

- **Propósito**: consumidor que alcanza el backend y consulta las métricas públicas.
- **Autenticación**: no requiere JWT, identidad, rol ni permiso de aplicación para acceder a
  `/actuator/prometheus`.

No se define relación persistida entre métricas y clientes; las métricas no se asocian a una
identidad.

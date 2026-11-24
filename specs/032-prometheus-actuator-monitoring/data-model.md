# Modelo de datos: métricas Prometheus y monitoreo seguro

La feature no introduce datos persistidos, tablas, migraciones ni entidades de dominio. Actuator y
Micrometer producen las mediciones en memoria y las serializan para la consulta HTTP.

## Conceptos expuestos

### Métrica de monitoreo

- **Propósito**: medición del estado, los recursos o el rendimiento del backend.
- **Representación**: nombre, tipo, valor y etiquetas según el formato de exposición de Micrometer
  compatible con Prometheus.
- **Ciclo de vida**: observada por la instrumentación existente durante la ejecución de la
  aplicación; consultable en `/actuator/prometheus`.
- **Persistencia**: ninguna en esta feature.

### Identidad operativa autenticada

- **Propósito**: identidad con un JWT válido aceptado por la autenticación existente, habilitada
  para consultar las métricas sin roles ni permisos adicionales.
- **Representación y validación**: JWT Bearer validado por el Resource Server y el
  `PersistedIdentityValidator` existentes. No se crea una identidad, rol ni almacenamiento nuevo.

No se define relación persistida entre métricas e identidades; la identidad solo gobierna el acceso
al endpoint.

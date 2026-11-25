# Validación manual: métricas Prometheus y monitoreo seguro

Esta guía describe escenarios para que el usuario valide SPEC-032. No se ejecuta como parte de
PLAN. Requiere JDK 21 y las variables de entorno de desarrollo del backend.

1. Desde `backend/`, iniciar la aplicación con la configuración habitual:

   ```bash
   ./gradlew bootRun
   ```

2. Consultar `/actuator/prometheus` sin credenciales. Se espera `200` y métricas en formato
   compatible con Prometheus, sin encabezado de autorización.

   ```bash
   curl -i http://localhost:8080/actuator/prometheus
   ```

3. Consultar el cuerpo de la respuesta y confirmar las métricas estándar disponibles de CPU, JVM y
   solicitudes HTTP. Las métricas de pausa GC se registran después de que ocurra una pausa desde el
   inicio de la aplicación. No deben aparecer secretos, credenciales, tokens ni configuración
   sensible; inspeccionar la respuesta obtenida en el paso anterior.

4. Consultar health e info anónimamente. Health debe exponer solo el estado general. Info debe
   mostrar el nombre `Football Market` y la descripción `API de mercado de jugadores de fútbol`, sin
   revelar otros valores del entorno, secretos, credenciales, tokens ni configuración sensible.

   ```bash
   curl -i http://localhost:8080/actuator/health
   curl -i http://localhost:8080/actuator/info
   ```

5. Intentar las rutas `/actuator`, `/actuator/health/<subpath>`,
   `/actuator/info/<subpath>`, `/actuator/metrics` y `/actuator/env`. Se espera que ninguna quede
   expuesta por HTTP, incluso con una identidad autenticada.

La validación automatizada correspondiente debe cubrir acceso anónimo a Prometheus, contenido de
health/info y ausencia de acceso a endpoints no permitidos, usando las convenciones de pruebas
backend. El agente no ejecuta tests.

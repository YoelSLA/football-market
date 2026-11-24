# Validación manual: métricas Prometheus y monitoreo seguro

Esta guía describe escenarios para que el usuario valide SPEC-032. No se ejecuta como parte de
PLAN. Requiere JDK 21, variables de entorno de desarrollo del backend y credenciales JWT válidas
emitidas por el mecanismo existente.

1. Desde `backend/`, iniciar la aplicación con la configuración habitual:

   ```bash
   ./gradlew bootRun
   ```

2. Consultar `/actuator/prometheus` sin credenciales. Se espera rechazo sin cuerpo de métricas.

   ```bash
   curl -i http://localhost:8080/actuator/prometheus
   ```

3. Consultar `/actuator/prometheus` con cualquier JWT válido aceptado por la autenticación existente.
   No se requiere un rol o permiso adicional. Se espera `200`,
   formato de exposición compatible con Prometheus y métricas estándar disponibles de CPU, JVM y
   solicitudes HTTP. Las métricas de pausa GC se registran después de que ocurra una pausa desde
   el inicio de la aplicación.

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN_VALIDO>" \
      http://localhost:8080/actuator/prometheus
   ```

4. Consultar `/actuator/prometheus` con token inválido. Se espera `401`, sin métricas.

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN_INVALIDO>" \
     http://localhost:8080/actuator/prometheus
   ```

5. Consultar health e info anónimamente. Health debe exponer solo el estado general. Info debe
   mostrar el nombre `Football Market` y la descripción `API de mercado de jugadores de fútbol`, sin
   revelar otros valores del entorno, secretos, credenciales, tokens ni configuración sensible.

   ```bash
   curl -i http://localhost:8080/actuator/health
   curl -i http://localhost:8080/actuator/info
   ```

6. Intentar las rutas `/actuator`, `/actuator/health/<subpath>`,
   `/actuator/info/<subpath>`, `/actuator/metrics` y `/actuator/env`. Se espera que ninguna quede
   expuesta por HTTP, incluso con una identidad autenticada.

La validación automatizada correspondiente debe cubrir exposición, autenticación y ausencia de
acceso a endpoints no permitidos, usando las convenciones de pruebas backend. El agente no ejecuta
tests.

---

description: "Tareas de implementación de monitoreo Prometheus y Actuator"
---

# Tasks: Métricas Prometheus y monitoreo seguro

**Input**: Diseño de `specs/032-prometheus-actuator-monitoring/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`).

**Prerequisites**: `spec.md` y `plan.md`; checklist general y checklist de monitoreo evaluados.

**Tests**: Incluir pruebas backend de HTTP y seguridad conforme a `docs/backend/testing.md`; su ejecución corresponde al usuario. No crear tests frontend ni tareas para que el agente ejecute tests.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Las tareas técnicas viven exclusivamente en este archivo; no son Issues ni elementos de seguimiento. Mantener esta documentación viva limitada al alcance activo de SPEC-032. Completar tareas no equivale a cerrar la funcionalidad ni autoriza su implementación.

**Organización**: Tareas agrupadas por User Story para mantener trazabilidad y permitir su verificación independiente.

## Phase 1: Setup

**Propósito**: Incorporar el exportador Prometheus requerido y mantener sincronizado el inventario tecnológico.

- [X] T001 Añadir `io.micrometer:micrometer-registry-prometheus` con versión administrada por Spring Boot en `backend/build.gradle.kts` y actualizar su uso en `docs/backend/technologies.md`.

---

## Phase 2: Foundational

**Propósito**: Establecer la exposición y protección HTTP compartida por las historias.

- [X] T002 [P] Limitar `management.endpoints.web.exposure.include` exactamente a `prometheus,health,info`, sin comodines ni configuración que permita ampliar la lista, en `backend/src/main/resources/application.yml`.
- [X] T003 [P] Restringir las reglas HTTP de Actuator en `backend/src/main/java/footballmarket/security/SecurityConfig.java`: exigir la autenticación JWT existente para `/actuator/prometheus`, permitir acceso anónimo solo a `/actuator/health` y `/actuator/info`, y no abrir rutas raíz, subendpoints ni otros endpoints.

**Checkpoint**: La dependencia y las reglas compartidas están definidas; las historias pueden verificarse de forma independiente.

---

## Phase 3: User Story 1 — Consultar métricas del backend (Priority: P1) 🎯 MVP

**Goal**: Permitir a operaciones consultar métricas estándar compatibles con Prometheus y el estado general mínimo por separado.

**Independent Test**: Con cualquier JWT válido aceptado por la autenticación existente, consultar `/actuator/prometheus` y comprobar el formato y las métricas estándar de CPU, JVM y solicitudes HTTP; consultar `/actuator/health` anónimamente y comprobar que expone solo el estado mínimo.

### Pruebas backend

- [X] T004 [P] [US1] Añadir pruebas HTTP de Actuator para el formato Prometheus, las métricas estándar especificadas y el estado mínimo de `/actuator/health`; verificar que las respuestas de ambos endpoints no incluyan secretos, credenciales, tokens ni valores de configuración sensibles en `backend/src/test/java/footballmarket/features/monitoring/presentation/ActuatorMetricsHttpIntegrationTest.java`.

**Checkpoint**: La respuesta de métricas y el estado de salud cumplen independientemente los criterios de US1.

---

## Phase 4: User Story 2 — Proteger el acceso y la información de monitoreo (Priority: P1)

**Goal**: Rechazar solicitudes sin autenticación válida y limitar las respuestas públicas y la superficie HTTP de Actuator.

**Independent Test**: Comparar acceso anónimo, token inválido e identidad autenticada; comprobar además las respuestas públicas de health/info y que el resto de endpoints y subendpoints no esté expuesto.

### Pruebas backend

- [X] T005 [P] [US2] Añadir pruebas de seguridad HTTP para `/actuator/prometheus` que comprueben acceso autenticado (200), solicitante anónimo y token inválido (401), sin diferenciar por roles o permisos entre identidades autenticadas. Comprobar que `/actuator/info` es accesible anónimamente, devuelve el nombre y descripción estáticos acordados y no incluye otros valores del entorno, secretos, credenciales, tokens ni configuración sensible; verificar también la no exposición de otros endpoints y subendpoints de Actuator en `backend/src/test/java/footballmarket/features/monitoring/security/ActuatorMonitoringSecurityIntegrationTest.java`.

**Checkpoint**: Se demuestra que la protección y la superficie HTTP cumplen independientemente los criterios de US2.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Propósito**: Completar la validación documentada del alcance.

- [X] T006 Actualizar la comprobación manual para incluir el acceso con JWT válido sin roles adicionales, el rechazo de solicitudes anónimas o con token inválido, el acceso anónimo a `/actuator/info` y la ausencia de exposición de los subendpoints en `specs/032-prometheus-actuator-monitoring/quickstart.md`.

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Fase 1)**: T001 no depende de otras tareas.
- **Foundational (Fase 2)**: T002 y T003 dependen de T001; pueden realizarse en paralelo al modificar archivos distintos.
- **User Stories (Fases 3 y 4)**: T004 y T005 dependen de T001–T003 y pueden realizarse en paralelo; cada historia es P1 y no depende funcionalmente de la otra.
- **Polish (Fase 5)**: T006 depende de T001–T005 para documentar el comportamiento implementado y cubierto.

### Oportunidades de paralelización

- Después de T001, T002 y T003 pueden trabajarse en paralelo.
- Después de las tareas fundacionales, T004 y T005 pueden trabajarse en paralelo en archivos de test distintos.

### Ejemplo de paralelización para las historias

```text
Tras T001–T003:
  US1: T004 — pruebas HTTP de métricas y health
  US2: T005 — pruebas HTTP de autorización y superficie expuesta
```

---

## Estrategia de implementación

### MVP

El MVP recomendado es US1: completar T001–T004 para habilitar la consulta de métricas Prometheus con un JWT válido y el acceso al estado mínimo. T003 es prerrequisito compartido de seguridad y debe completarse antes de las historias; US2 sigue siendo alcance requerido, no opcional.

### Entrega incremental

Completar primero la configuración común y luego verificar US1 y US2 por separado. Las pruebas se escriben conforme a las normas backend; el usuario las ejecuta. No agregar servidor Prometheus, dashboards Grafana, métricas personalizadas ni infraestructura de pruebas frontend.


---

description: "Tareas de implementación de monitoreo Prometheus y Actuator"
---

# Tasks: Métricas Prometheus y monitoreo seguro

**Input**: Diseño de `specs/032-prometheus-actuator-monitoring/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`).

**Prerequisites**: `spec.md` y `plan.md`; checklist general y checklist de monitoreo evaluados.

**Tests**: Incluir pruebas backend de HTTP y seguridad conforme a `docs/backend/testing.md`; su ejecución corresponde al usuario. No crear tests frontend ni tareas para que el agente ejecute tests.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Esta reanudación conserva IDs y avance existente del mismo issue; reabrir solo las tareas afectadas por el cambio aprobado a acceso público. Las tareas técnicas viven exclusivamente en este archivo; no son Issues ni elementos de seguimiento. Completar tareas no equivale a cerrar la funcionalidad ni autoriza su implementación.

**Organización**: Tareas agrupadas por User Story para mantener trazabilidad y permitir su verificación independiente.

## Phase 1: Setup

**Propósito**: Incorporar el exportador Prometheus requerido y mantener sincronizado el inventario tecnológico.

- [X] T001 Añadir `io.micrometer:micrometer-registry-prometheus` con versión administrada por Spring Boot en `backend/build.gradle.kts` y actualizar su uso en `docs/backend/technologies.md`.

---

## Phase 2: Foundational

**Propósito**: Establecer la exposición HTTP permitida y la seguridad de contenido compartida por las historias.

- [X] T002 [P] Limitar `management.endpoints.web.exposure.include` exactamente a `prometheus,health,info` y configurar en `backend/src/main/resources/application.yml` el nombre y descripción estáticos permitidos por `/actuator/info`, sin comodines ni exposición de otros valores del entorno.
- [X] T003 [P] Configurar `backend/src/main/java/footballmarket/security/SecurityConfig.java` para permitir anónimamente las rutas exactas `/actuator/prometheus`, `/actuator/health` y `/actuator/info`, sin abrir otros endpoints o subendpoints ni modificar la seguridad de la API de dominio.

**Checkpoint**: La dependencia y las reglas compartidas están definidas; las historias pueden verificarse de forma independiente.

---

## Phase 3: User Story 1 — Consultar métricas del backend (Priority: P1) 🎯 MVP

**Goal**: Permitir a cualquier cliente que alcance el backend consultar, sin JWT, métricas estándar compatibles con Prometheus y el estado general mínimo por separado.

**Independent Test**: Consultar `/actuator/prometheus` sin JWT y comprobar respuesta `200`, formato compatible y métricas estándar de CPU, JVM y solicitudes HTTP; consultar `/actuator/health` anónimamente y comprobar que expone solo el estado mínimo.

### Pruebas backend

- [X] T004 [P] [US1] Actualizar las pruebas HTTP de Actuator para comprobar que `/actuator/prometheus` devuelve `200` y las mismas métricas con JWT ausente, válido, vencido o inválido, y formato Prometheus con las métricas estándar especificadas; comprobar que `/actuator/health` es anónimo y mínimo, y que ninguna respuesta incluye datos sensibles en `backend/src/test/java/footballmarket/features/monitoring/presentation/ActuatorMetricsHttpIntegrationTest.java`.

**Checkpoint**: La respuesta de métricas y el estado de salud cumplen independientemente los criterios de US1.

---

## Phase 4: User Story 2 — Limitar la información y los endpoints de monitoreo (Priority: P1)

**Goal**: Limitar la información pública y la superficie HTTP de Actuator, preservando intacta la seguridad de los endpoints de dominio.

**Independent Test**: Comprobar que `/actuator/info` anónimo contiene únicamente el nombre y descripción acordados, y que otros endpoints/subendpoints de Actuator no se exponen incluso con una identidad autenticada; confirmar que las reglas de dominio no cambian.

### Pruebas backend

- [X] T005 [P] [US2] Actualizar las pruebas HTTP para comprobar que `/actuator/info` anónimo devuelve solo el nombre `Football Market` y la descripción `API de mercado de jugadores de fútbol`, que no se exponen otros valores del entorno, y que los endpoints/subendpoints no permitidos de Actuator no responden como expuestos incluso con identidad autenticada, mientras una ruta protegida de dominio conserva su autenticación, en `backend/src/test/java/footballmarket/features/monitoring/security/ActuatorMonitoringSecurityIntegrationTest.java`.

**Checkpoint**: Se demuestra que la protección y la superficie HTTP cumplen independientemente los criterios de US2.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Propósito**: Completar la validación documentada del alcance.

- [X] T006 Actualizar la comprobación manual para incluir el acceso anónimo a Prometheus y los valores estáticos públicos de info, y comprobar la ausencia de exposición de los subendpoints en `specs/032-prometheus-actuator-monitoring/quickstart.md`.

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Fase 1)**: T001 no depende de otras tareas.
- **Foundational (Fase 2)**: T002 y T003 dependen de T001; se reabren para ajustar la configuración al acceso público y pueden realizarse en paralelo en archivos distintos.
- **User Stories (Fases 3 y 4)**: T004 y T005 dependen de T001–T003 y pueden realizarse en paralelo; cada historia es P1 y no depende funcionalmente de la otra.
- **Polish (Fase 5)**: T006 está completada; documenta el contrato verificado de acceso público y no depende de las pruebas aún pendientes de actualización.

### Oportunidades de paralelización

- Después de T001, T002 y T003 pueden trabajarse en paralelo.
- Después de las tareas fundacionales, T004 y T005 pueden trabajarse en paralelo en archivos de test distintos.

### Ejemplo de paralelización para las historias

```text
Tras T001–T003:
  US1: T004 — pruebas HTTP de métricas y health
  US2: T005 — pruebas HTTP de contenido y superficie expuesta, incluida una ruta de dominio protegida
```

---

## Estrategia de implementación

### MVP

El MVP recomendado es US1: completar T001–T004 para habilitar la consulta anónima de métricas Prometheus y el acceso al estado mínimo. T003 es prerrequisito compartido y debe completarse antes de las historias; US2 sigue siendo alcance requerido, no opcional.

### Entrega incremental

Completar primero la configuración común y luego verificar US1 y US2 por separado. Las pruebas se escriben conforme a las normas backend; el usuario las ejecuta. No agregar servidor Prometheus, dashboards Grafana, métricas personalizadas ni infraestructura de pruebas frontend.

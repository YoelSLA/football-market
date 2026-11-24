# Implementation Plan: Métricas Prometheus y monitoreo seguro

**Branch**: `feat/032-prometheus-actuator-monitoring` | **Date**: 2026-11-24 | **Spec**: [spec.md](spec.md)

<!--
Documentación viva: este documento describe exclusivamente el diseño técnico vigente.
El historial de decisiones superadas se conserva en Git. PLAN no genera Tasks ni habilita
IMPLEMENT; siguen CHECKLIST, TASKS y ANALYZE.
-->

**Input**: Feature specification from `specs/032-prometheus-actuator-monitoring/spec.md`

## Summary

Exponer las métricas del backend en formato Prometheus mediante el Actuator existente,
restringiendo ese endpoint a identidades autenticadas por la seguridad actual. La exposición HTTP
se limita expresamente a `prometheus`, `health` e `info`: `health` es público con respuesta mínima;
`info` es público con nombre y descripción estáticos no sensibles; los demás endpoints y subendpoints de Actuator permanecen
inaccesibles. La solución reutiliza Spring Boot Actuator y la cadena JWT existente; agrega solo el
exportador Micrometer Prometheus requerido si no está ya presente en la configuración efectiva.
Se limita a las métricas estándar de CPU, JVM y solicitudes HTTP; no agrega métricas de negocio ni
instrumentación propia. El estado general se consulta por `/actuator/health`, y la tasa de errores
se obtiene de los resultados HTTP. No instala ni despliega un servidor Prometheus ni crea dashboards.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 4.1.1, Spring Boot Actuator ya declarado, Spring Security
con OAuth2 Resource Server/JWT ya configurado; registry Micrometer Prometheus requerido para
publicar `/actuator/prometheus` (versión administrada por Spring Boot).

**Storage**: N/A; esta feature expone métricas en memoria y no agrega persistencia.

**Testing**: Pruebas backend existentes con Spring Boot Test, JUnit Jupiter, MockMvc y Spring
Security; se añadirán/ajustarán pruebas de autorización y exposición conforme al contrato. El
agente no ejecuta tests.

**Target Platform**: Servicio backend Spring Boot, puerto HTTP compartido con la API.

**Project Type**: Servicio web backend.

**Performance Goals**: No se especifican nuevos objetivos cuantitativos ni percentiles de latencia;
exponer las métricas estándar de CPU, JVM y solicitudes HTTP, y consultar el estado general por
`/actuator/health`, sin introducir una ruta de dominio.

**Constraints**: Mantener el alcance HTTP mínimo y explícito; no usar exposición comodín. Solo
identidades autenticadas mediante el mecanismo existente acceden a Prometheus. Health conserva
`show-details: never` y `show-components: never`; info publica únicamente el nombre y descripción
estáticos configurados, sin habilitar exposición de otros valores del entorno. Sin nueva autenticación, rol de
dominio, puerto de administración, servidor Prometheus ni dashboards.

**Scale/Scope**: Un servicio backend y tres endpoints de Actuator (`/actuator/prometheus`,
`/actuator/health`, `/actuator/info`); no cambia el modelo de dominio ni la persistencia.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Antes de Phase 0

- **Principios I y VI — simplicidad y mínimo alcance**: PASS. Reutilizar Actuator, Micrometer y la
  seguridad existente; no crear componentes, mecanismo de identidad ni infraestructura externa
  innecesarios.
- **Principios II y VI — proporcionalidad**: PASS. El diseño cubre los requisitos FR-001–FR-009;
  excluye expresamente servidor Prometheus y Grafana, y no agrega SLAs ni métricas de negocio no
  requeridos.
- **Principio III — seguridad**: PASS CONDICIONADO AL DISEÑO. Métricas solo autenticadas;
  health/info públicos con superficie mínima; demás endpoints y subendpoints cerrados.
- **Principio IV — coherencia de contratos**: PASS. El contrato HTTP de monitoreo se documenta en
  `contracts/`; si se añade una dependencia, su inventario vigente se refleja en tecnologías del
  backend durante implementación.
- **Principio V — trazabilidad/documentación viva**: PASS. Los artefactos de esta spec describen
  solo el diseño vigente; no se modifica la especificación ni el seguimiento en esta fase.
- **Verificación/autorizaciones**: PASS. No se ejecutan tests ni builds; este plan no autoriza
  IMPLEMENT, CHECKLIST, TASKS ni fases posteriores.

### Después de Phase 1

- **Seguridad y exposición**: PASS. El contrato especifica autorización de Prometheus, excepciones
  públicas limitadas a health e info, y denegación del resto. La configuración deberá listar
  únicamente esos tres endpoints y la cadena de seguridad deberá proteger Prometheus sin abrir
  `/actuator/**`.
- **Simplicidad/tecnologías**: PASS. Una dependencia de registry solo si hace falta para el
  endpoint requerido; versión bajo gestión de Spring Boot. No se agrega persistencia ni capa
  arquitectónica propia.
- **Trazabilidad**: PASS. Contrato, modelo conceptual y guía de validación derivan de los
  requisitos de SPEC-032, sin redefinirlos.

No se identifican violaciones constitucionales que requieran justificar complejidad.

## Project Structure

### Documentation (this feature)

```text
specs/032-prometheus-actuator-monitoring/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── actuator-monitoring.md
```

`spec.md` y `tasks.md` son también miembros permanentes de la tríada/artefactos de la feature;
PLAN no genera ni modifica `tasks.md`.

### Source Code (repository root)

```text
backend/
├── build.gradle.kts
└── src/main/
    ├── java/footballmarket/security/SecurityConfig.java
    └── resources/application.yml
```

La feature no requiere una clase Controller, Service, Model, Repository, migración ni cambio de
frontend. El endpoint y las métricas los proporciona Actuator/Micrometer.

**Structure Decision**: Mantener la estructura backend existente. Ajustar la declaración de
dependencias para el registry Prometheus si la inspección de configuración efectiva confirma que
falta; limitar exposición y autorización en las configuraciones existentes de Spring Boot y Spring
Security.

## Complexity Tracking

No aplica: Constitution Check sin violaciones.

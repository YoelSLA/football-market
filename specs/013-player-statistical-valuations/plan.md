# Implementation Plan: Valoraciones estadísticas de jugadores

**Branch**: `feat/013-player-statistical-valuations` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-player-statistical-valuations/spec.md`

**Planning Status**: PLAN COMPLETO — Phase 0 y Phase 1 documentadas. CHECKLIST y TASKS están completos; corresponde repetir ANALYZE después de las correcciones documentales autorizadas. Este plan no autoriza IMPLEMENT.

## Summary

Calcular síncronamente dos valoraciones estadísticas versionadas para toda la última campaña JSON confirmada con cobertura completa. El caso de uso leerá las filas aplanadas de SPEC-012, aplicará respaldo por scraping solo ante nulos compartidos, derivará e imputará métricas, normalizará por posición, liga y temporada y publicará de forma coherente ambas estrategias por jugador y fecha. Los resultados se persistirán en dos tablas propias y el endpoint autenticado `POST /api/valuations/evaluate` devolverá únicamente un resumen agregado.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security/OAuth2 Resource Server, Flyway y pgJDBC. No se agrega ninguna dependencia.

**Storage**: PostgreSQL gestionado por Flyway. Se leen `player_statistics`, `player_statistics_extraction_runs`, `player_statistics_run_items`, `players`, `teams` y `leagues`; se agregan tablas independientes para valoración posicional y valoración de atributos, con unicidad por jugador, fecha y estrategia/version.

**Testing**: JUnit Jupiter, Mockito, AssertJ, MockMvc y Testcontainers con PostgreSQL 18. Se planifican pruebas Model, Repository, Service, Controller y Security conforme a `docs/backend/testing.md`; el usuario ejecutará los tests.

**Target Platform**: Backend Spring Boot del monolito existente, desplegable sobre JVM 21 y PostgreSQL.

**Project Type**: Servicio web backend.

**Performance Goals**: No aplica un objetivo temporal cuantitativo a esta feature. La petición síncrona y los patrones de consulta indexados son requisitos funcionales y de persistencia, no un SLA de rendimiento.

**Constraints**: Última fecha JSON con cobertura poblacional completa; respaldo de scraping del mismo jugador y fecha; cálculos con `BigDecimal`; persistencia a seis decimales `HALF_UP`; publicación atómica de las dos estrategias por jugador; continuidad ante fallos individuales; sin concurrencia contractual, scheduler, ranking, endpoints de lectura ni conversión a cotización.

**Scale/Scope**: Cinco ligas y aproximadamente 2100 jugadores por campaña; dos resultados por jugador evaluable; cuatro posiciones generales; percentiles aislados por posición, liga y temporada.

## Constitution Check

*GATE: evaluado antes de Phase 0 y nuevamente después de Phase 1.*

### Pre-Design Gate

| Regla | Evaluación |
| --- | --- |
| Simplicidad y calidad | PASS. Se reutiliza el monolito y el stack existente; no se agregan dependencias, scheduler, cola, caché ni abstracciones de proveedor. |
| Proporcionalidad del alcance | PASS. El diseño excluye lecturas HTTP, ranking, estrategia activa, precio y ejecución automática. |
| Seguridad e integridad | PASS. El endpoint conserva JWT y usuario persistido; no recibe datos de cálculo ni realiza llamadas externas. |
| Coherencia de contratos | PASS. El contrato HTTP, DTO, OpenAPI, seguridad y pruebas quedan identificados como una unidad de cambio. |
| Trazabilidad | PASS. SPEC-013, rama y artefactos comparten identidad; las dependencias sobre SPEC-012 están explícitas. |
| Idioma | PASS. Código, campos y rutas en inglés; documentación, mensajes y OpenAPI en español. |
| Verificación | PASS. PLAN no ejecuta tests ni builds; las verificaciones ejecutables quedan para fases posteriores según autorización. |
| Arquitectura backend | PASS. Controller → Service → Model/Repository; componentes internos del caso de uso no se exponen como Services independientes. |
| Requisitos académicos | PASS. Conforme a la aclaración del equipo docente registrada en la spec, cada estrategia configura métricas, fórmulas y ponderaciones diferenciadas al definirse; esos pesos quedan fijos por versión para asegurar trazabilidad y reproducibilidad. SPEC-013 implementa dos configuraciones materialmente distintas y cubre parcialmente REQ-S2-02/03 sin anticipar su selección activa ni la cotización. |

### Post-Design Gate

PASS. Phase 1 mantiene los límites anteriores: dos agregados persistentes simples, una fachada Service, cálculo de dominio sin DTO ni Repository, y acceso a datos/persistencia desde el rol Service. La atomicidad por jugador evita resultados parciales; la limpieza por fecha evita vigencia obsoleta. El contrato no expone causas internas ni datos sensibles. No quedan marcadores `NEEDS CLARIFICATION` ni violaciones que requieran Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/013-player-statistical-valuations/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── http.md
└── checklists/
    ├── requirements.md
    └── valuation.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/footballmarket/
│   ├── config/                         # Properties de mínimos e impactos
│   ├── controllers/                    # ValuationController
│   │   ├── dtos/responses/             # Resumen HTTP
│   │   └── mappers/                    # Model → Response DTO
│   ├── models/                         # Resultados, fórmulas y value objects
│   │   ├── enums/                      # Estrategia, posición, fuente y exclusión
│   │   └── records/                    # Resumen y candidatos internos
│   ├── repositories/                   # Estadísticas, resultados y proyecciones
│   └── services/
│       ├── ValuationEvaluationService.java
│       ├── impl/ValuationEvaluationServiceImpl.java
│       └── internal/                   # Selección, fallback, percentiles y publicación
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/                   # Esquema de ambas valoraciones
└── src/test/java/footballmarket/
    ├── controllers/
    ├── models/
    ├── repositories/
    ├── security/
    ├── services/
    └── support/
```

**Structure Decision**: `ValuationEvaluationService` será la fachada única del caso de uso. Los componentes de `services/internal/` componen selección de campaña, ensamblado con fallback, poblaciones de normalización y publicación dentro del mismo límite de aplicación; no son Services reutilizables ni justifican un Orchestrator. Las fórmulas deterministas, percentiles, redondeo e invariantes pertenecen a Model. Los Repository resuelven consultas y persistencia sin lógica de negocio.

## Phase 0: Research

Las decisiones, alternativas y validación del contrato de snapshot de SPEC-012 están consolidadas en [research.md](research.md). No quedan aclaraciones técnicas abiertas.

## Phase 1: Design & Contracts

- [data-model.md](data-model.md): entradas de SPEC-012, dos agregados de valoración, resumen, relaciones, índices y transiciones de publicación.
- [contracts/http.md](contracts/http.md): `POST /api/valuations/evaluate`, autenticación, respuesta agregada y errores.
- [quickstart.md](quickstart.md): guía de validación humana posterior a implementación, sin ejecutar controles en PLAN.

## Complexity Tracking

No hay violaciones constitucionales ni excepciones arquitectónicas que justificar.

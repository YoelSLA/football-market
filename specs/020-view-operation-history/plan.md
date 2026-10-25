# Implementation Plan: Historial personal de operaciones (SPEC-020)

**Branch**: `feat/020-view-operation-history` | **Date**: 2026-10-24 | **Spec**: [`spec.md`](spec.md)

**Input**: Feature specification from `specs/020-view-operation-history/spec.md`

## Summary

SPEC-020 expone una consulta backend autenticada del historial completo de operaciones confirmadas
del usuario. El endpoint `GET /api/users/me/transactions` obtiene la identidad exclusivamente del
JWT, lee los registros existentes de `MarketTransaction` donde esa persona es compradora o
vendedora, interpreta cada fila como `BUY` o `SELL` y devuelve sus valores históricos sin
recalcularlos ni modificar el mercado.

El diseño reutiliza el modelo, la persistencia y los índices actuales. Un Service de solo lectura
resuelve al usuario por el correo de la sesión y carga las transacciones con jugador en una consulta
ordenada por `executedAt DESC, id DESC`; un Mapper traduce la vista de dominio a DTO HTTP. No se
crean tablas, migraciones, fuentes financieras paralelas, paginación ni filtros.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security con JWT,
Hibernate, PostgreSQL, SpringDoc OpenAPI, Lombok, JUnit Jupiter, MockMvc y Testcontainers. Sin
dependencias nuevas.

**Storage**: PostgreSQL; lectura de `market_transactions`, `users` y `players`. Se reutilizan los
índices `idx_market_transactions_buyer` e `idx_market_transactions_seller`. Sin cambios de esquema.

**Testing**: JUnit Jupiter con `@Nested` y una `@Tag` literal por clase. Controller con `@WebMvcTest`
y Service/Repository con PostgreSQL real mediante Testcontainers, conforme a
`docs/backend/testing.md`. La ejecución corresponde al usuario.

**Target Platform**: Servidor Linux, JVM 21 y PostgreSQL.

**Project Type**: Servicio backend REST de una aplicación web.

**Performance Goals**: La definición y validación de SLA de rendimiento queda fuera del alcance de
SPEC-020 y se abordará como requisito transversal en el próximo sprint. Esta consulta debe usar una
lectura de conjunto, sin N+1, aprovechar los índices existentes y ordenar en base de datos.

**Constraints**: Solo lectura; identidad exclusiva de la sesión; colección completa sin paginación;
una fila por transacción; importes enteros en centésimas de crédito; valores históricos sin
recálculo; orden descendente estable; sin operaciones de escritura.

**Scale/Scope**: Una feature backend. Componentes previstos: Controller, Response DTO, Mapper,
Service e implementación, record de vista y ampliación acotada de `MarketTransactionRepository`.
Pruebas de Controller, Service y Repository. Sin frontend ni migraciones.

## Constitution Check

*GATE: evaluado antes de Fase 0 y nuevamente después de Fase 1.*

| Principio / regla | Verificación | Resultado |
| --- | --- | --- |
| I. Simplicidad y calidad | Reutiliza modelo, repositorio, seguridad e índices; agrega solo el caso de uso y su frontera HTTP | PASS |
| II. Proporcionalidad | Sin paginación, filtros, estadísticas, exportación ni alcance frontend | PASS |
| III. Seguridad e integridad | La identidad procede del JWT; no existe parámetro de usuario; la transacción es `readOnly` | PASS |
| IV. Coherencia de contratos | El contrato propio queda en `contracts/operation-history-api.md` y deberá mantenerse con DTO, OpenAPI y pruebas de Controller | PASS |
| V. Trazabilidad y fuente de verdad | `MarketTransaction` sigue siendo la única fuente financiera; SPEC-020 materializa REQ-S2-10 | PASS |
| Arquitectura backend | Flujo Controller → Service → Repository y DTO ↔ Mapper ↔ vista de Model, sin dependencias prohibidas | PASS |
| Autorizaciones SDD | Esta operación produce únicamente PLAN y diseño; no crea Tasks ni implementa código | PASS |
| Verificación | No se ejecutan tests ni builds en este cambio documental | PASS |

**Post-diseño**: PASS. El modelo, el contrato y la guía no introducen nuevas dependencias, fuentes de
datos ni requisitos. No hay violaciones que registrar en Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/020-view-operation-history/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── operation-history-api.md
└── tasks.md                         # Fase posterior; no se crea en PLAN
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── controllers/
│   ├── OperationHistoryController.java
│   ├── dtos/responses/OperationHistoryResponseDTO.java
│   └── mappers/OperationHistoryMapper.java
├── models/records/OperationHistoryEntry.java
├── repositories/MarketTransactionRepository.java
└── services/
    ├── OperationHistoryService.java
    └── impl/OperationHistoryServiceImpl.java

backend/src/test/java/footballmarket/
├── controllers/OperationHistoryControllerTest.java
├── repositories/OperationHistoryPersistenceTest.java
└── services/OperationHistoryServiceTest.java
```

**Structure Decision**: se conserva la estructura existente por roles. El Controller transporta el
principal y la respuesta; el Service resuelve identidad e interpretación de rol; el Repository
realiza la lectura ordenada; el Mapper solo convierte la vista a DTO. No se modifica frontend.

## Complexity Tracking

No aplica: el diseño no viola la Constitución ni requiere excepciones arquitectónicas.

## Evidencia de fase

- PLAN autorizado y ejecutado el 2026-10-24 sobre `feat/020-view-operation-history`.
- Fuentes revisadas: Constitution 9.0.0, workflow SDD, requisitos académicos, normas backend,
  `MarketTransaction`, su repositorio, seguridad JWT y contratos análogos.
- Fases completadas por este comando: Phase 0 (`research.md`) y Phase 1 (`data-model.md`, contrato y
  `quickstart.md`).
- No se ejecutaron tests, builds ni hooks; `.specify/extensions.yml` no existe.
- CHECKLISTS y TASKS fueron generadas y evaluadas. ANALYZE se repitió después de las remediaciones
  y finalizó sin blockers; la preparación global está completa. El siguiente paso permitido es
  solicitar IMPLEMENT AUTHORIZATION explícita, que todavía no fue concedida.

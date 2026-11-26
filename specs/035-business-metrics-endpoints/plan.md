# Implementation Plan: Endpoints de métricas de negocio

**Branch**: `feat/035-business-metrics-endpoints` | **Date**: 2026-11-26 | **Spec**: [spec.md](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente la arquitectura, diseño y decisiones
técnicas vigentes y aplicables al sistema (Constitución §V). El historial de decisiones técnicas
superadas se delega a Git. Forma parte de la tríada permanente obligatoria junto a spec.md y tasks.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `/specs/035-business-metrics-endpoints/spec.md`

## Summary

Se expondrán dos endpoints autenticados de solo lectura para métricas agregadas de negocio: una lectura histórica calculada sobre `market_transactions` y `token_listings`, y otra lectura de contadores Micrometer del proceso actual. Ambos compartirán el mismo Response DTO y contrato OpenAPI. La solución reutiliza los Services, Repositories, modelos, autenticación y Micrometer ya existentes, sin persistir las métricas de ejecución ni modificar el historial financiero.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1 (Web MVC, Data JPA, Security), Micrometer provisto por Spring Boot, PostgreSQL

**Storage**: PostgreSQL existente; sin nuevas tablas ni migraciones. Los totales históricos consultan las tablas vigentes; los contadores actuales viven en memoria.

**Testing**: Pruebas backend de Controller (contrato y seguridad), Service (agregación histórica y contadores) y Repository cuando la consulta SQL requiera validación propia. Las ejecuta el usuario, nunca el agente.

**Target Platform**: Servicio web backend Spring Boot existente

**Project Type**: API REST

**Performance Goals**: No se define un SLA nuevo; la agregación histórica se resuelve en consultas agregadas sobre tablas existentes.

**Constraints**: Mantener autenticación Bearer existente; cualquier cuenta autenticada accede. No exponer dimensiones individuales ni datos personales. Conservar separadas las métricas técnicas de Actuator.

**Scale/Scope**: Dos rutas GET, cinco agregados y el instante de consulta; sin cambios de frontend, despliegue de Prometheus/Grafana, esquema ni historial.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **PASS** — Alcance proporcional: dos consultas agregadas; se reutilizan arquitectura, seguridad, entidades y dependencias existentes.
- **PASS** — Seguridad e integridad: endpoints protegidos por la autenticación existente; respuestas y etiquetas sin identificadores de alta cardinalidad.
- **PASS** — Contratos: contrato HTTP normativo en `contracts/http.md`, OpenAPI desde Controller/DTO y pruebas de Controller requeridas por las normas backend.
- **PASS** — Persistencia: histórico de solo lectura desde filas financieras confirmadas y publicaciones efectivamente canceladas; sin alterar auditoría ni introducir almacenamiento derivado.
- **PASS** — Dependencias: Micrometer y Spring Boot ya forman parte del stack aprobado; no se incorpora tecnología.
- **PASS** — Verificación: se documentan pruebas necesarias para ejecución humana; no se ejecutan tests ni hooks incompatibles.

## Project Structure

### Documentation (this feature)

```text
specs/035-business-metrics-endpoints/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/http.md
└── tasks.md              # No se genera en PLAN
```

### Source Code (repository root)
```text
backend/src/main/java/footballmarket/features/businessmetrics/
├── application/                 # Service, interfaz y componentes de métricas
├── application/impl/            # Consultas y lecturas de agregados
└── presentation/
    ├── BusinessMetricsController.java
    └── dtos/response/            # Response DTO común

backend/src/main/java/footballmarket/features/market/infraestructure/
├── MarketTransactionRepository.java
└── TokenListingRepository.java

backend/src/main/java/footballmarket/features/market/application/impl/
├── BuyOrderServiceImpl.java
├── OfficialBuybackServiceImpl.java
└── TokenListingServiceImpl.java

backend/src/main/java/footballmarket/features/market/application/internal/
└── MarketSettlementExecutor.java

backend/src/test/java/footballmarket/features/businessmetrics/
├── application/
└── presentation/

backend/src/test/java/footballmarket/features/market/application/
├── BuyOrderServiceTest.java
├── OfficialBuybackServiceTest.java
└── TokenListingServiceTest.java
```

**Structure Decision**: Módulo backend por feature siguiendo `features/<feature>/{application,domain,infraestructure,presentation}`. El Controller delega a la interfaz Service; el Service agrega lecturas de los Repositories y contadores Micrometer. Los agregados SQL permanecen en los Repositories de mercado existentes; no se crea un rol arquitectónico ni una dependencia Service→Service.

La instrumentación de compras, recompras, cancelaciones y liquidaciones P2P se realiza en los componentes de mercado enumerados, solo después de completar la operación. Las pruebas de servicio correspondientes verifican esos incrementos.

## Diseño vigente

- Un Response DTO `record` común contiene los cinco contadores/volúmenes y `queriedAt`; Controller y Mapper no calculan negocio.
- El Service coordina una consulta histórica o la lectura de contadores Micrometer, y construye la respuesta con un único instante UTC de consulta.
- El histórico agrega `market_transactions` una sola vez por fila. La categoría se deriva de los roles: compra cuando el comprador es `USER`; venta cuando comprador es `SUPERUSER` y vendedor es `USER` (recompra oficial). Una liquidación P2P cuyo comprador es `USER` se clasifica como compra; no se duplica como venta.
- Tokens y créditos históricos se suman una vez por fila financiera; `total_amount` está almacenado en centésimas. Las cancelaciones se cuentan desde publicaciones con estado persistido `CANCELLED`; `EXPIRED` no se cuenta.
- La métrica de ejecución usa contadores Micrometer de cardinalidad fija y sin tags de identidad. Se incrementa por operación confirmada, no por request, y aplica la clasificación de compras y ventas definida en la spec: comprador `USER` es compra; comprador `SUPERUSER` y vendedor `USER` es venta, incluyendo la liquidación P2P solo como compra. La instrumentación ocurre en los puntos de aplicación que completan compras, ventas/recompras y cancelaciones, tras confirmar el estado de negocio; errores no incrementan. Los contadores se reinician con el proceso.
- La seguridad exige autenticación existente, sin rol adicional. OpenAPI documenta rutas, autenticación, campos, unidades y significado.

## Constitution Check post-diseño

- **PASS** — El diseño mantiene el alcance de SPEC-035 y excluye expresamente las métricas técnicas, el despliegue de monitoreo y cualquier desglose individual.
- **PASS** — Los dos endpoints comparten contrato, autenticación y formato; las fuentes históricas son de solo lectura y el estado de ejecución no se persiste.
- **PASS** — Se reutilizan dependencias y roles arquitectónicos vigentes. No se requieren nuevas tablas, tecnologías, validaciones ajenas al contrato ni adaptaciones de frontend.
- **PASS** — El plan deja pruebas de backend para la ejecución del usuario y no ejecuta tests, builds ni hooks.

## Complexity Tracking

No se identifican violaciones constitucionales ni complejidad adicional que justificar.

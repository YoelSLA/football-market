<!-- Documentación viva: describe únicamente el diseño vigente. La tríada permanente es
spec.md, plan.md y tasks.md. PLAN no genera Tasks ni habilita IMPLEMENT. -->
# Plan de implementación: Métricas de tendencias y ajuste de oferta-demanda

**Rama**: `feat/028-market-trends-and-demand-pricing` | **Fecha**: 2026-11-23 | **Spec**: [spec.md](spec.md)

## Resumen

Agregar una consulta autenticada de tendencias y reutilizar las cotizaciones, transacciones y la ejecución de recálculo existentes. Una única ventana configurable (24 horas por defecto) alimentará las métricas y la demanda de ambos recálculos. Las compras P2P se contabilizan solo como compras. El recálculo habitual conserva el score deportivo; el nuevo recálculo manual no lo consulta. Ambos conservan la auditoría por jugador, el límite de variación y el piso actuales.

## Contexto técnico

**Lenguaje/versión**: Java 21.
**Dependencias principales**: Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Spring Security, Jakarta Validation y Flyway existentes; no se agregan dependencias.
**Persistencia**: PostgreSQL; tablas existentes `market_transactions`, `player_quotes` y `quote_executions`.
**Pruebas**: backend; ampliar pruebas de Model, Repository, Service, Controller y seguridad según la responsabilidad. El agente no las ejecuta.
**Plataforma**: servicio web backend existente.
**Objetivos de rendimiento**: no se fijan objetivos nuevos; las agregaciones deben resolverse en persistencia, no materializando el historial completo en memoria.
**Restricciones**: importes en centésimas de crédito; operaciones de mercado append-only; cotización diaria existente; misma ventana temporal configurable; rutas API bajo `/api/`; ningún cambio frontend ni rol nuevo.
**Alcance**: consulta global de actividad; ajuste de precio por demanda en recálculo habitual y manual separado; auditoría de cada cotización y ejecución.

## Constitution Check — antes de Phase 0

- **PASS — mínimo alcance**: reutilizar contratos, tablas, autenticación y ejecución de cotizaciones existentes; sin nueva dependencia, scheduler o almacenamiento de transacciones.
- **PASS — integridad y seguridad**: no modificar transacciones confirmadas; endpoints protegidos por JWT y autorización vigente. No crear roles.
- **PASS — coherencia**: contratos HTTP, modelo, migraciones, implementación y pruebas backend deberán mantenerse sincronizados durante IMPLEMENT.
- **PASS — verificación**: no se ejecutan tests en PLAN ni en etapas posteriores por el agente.
- **PASS — permisos**: PLAN no habilita implementación; quedan pendientes Checklist, Tasks, Analyze y autorización humana de implementación.

## Diseño y decisiones

1. Usar `market_transactions` como fuente única. Clasificar como compra toda operación cuyo comprador sea `USER`, incluyendo compra directa y liquidación P2P; clasificar como venta una operación cuyo comprador sea `SUPERUSER` y vendedor `USER`. Cada fila cuenta una sola vez.
2. Agregar transacciones confirmadas por jugador de `[startedAt - demandWindow, startedAt)`, usando el mismo valor de `market.quote.demand-window` (default `PT24H`) en consulta y recálculos. Capturar un único instante por solicitud/ejecución. El sentimiento y volumen general del endpoint son agregados de todo el mercado.
3. Extender `QuoteProperties` con la ventana y `MarketTransactionRepository` con agregaciones por tipo y jugador. No crear tabla de métricas ni cache.
4. Mantener `QuoteRecalculationService` como fachada; componer cálculo habitual y de solo mercado sobre el publicador por jugador y el límite `QuoteAdjustment` existente. El recorrido exclusivo de mercado no invoca `QuoteScoreResolver`.
5. Distinguir tipo de ejecución y procedencia de cotización con campos explícitos. Persistir los componentes de precio por cotización; no inferir impacto deportivo para corridas solo de mercado ni inventarlo para filas históricas.
6. Aplicar multiplicadores de dominio; el multiplicador combinado pasa por el límite existente y el precio final respeta el piso. Mantener actualización de fila diaria y bloqueo por jugador.
7. Persistir el tipo de ejecución y sus parámetros de mercado en `quote_executions`; permitir ausencia de estrategia/factor deportivo solo para ejecuciones `MARKET_DEMAND_ONLY`. Mantener cap, piso, inicio/fin y contadores existentes.
8. Reutilizar JWT y autorización vigentes para operaciones administrativas del mercado; no introducir rol.

## Constitution Check — después de Phase 1

- **PASS condicionado a respetar el diseño**: no altera el ledger financiero; los cambios de esquema se limitan a la trazabilidad de recálculo.
- **PASS**: se mantiene la separación Controller → DTO/Mapper → Service → Model/Repository definida en arquitectura backend.
- **PASS**: los cambios HTTP requieren contratos en `contracts/`, anotaciones OpenAPI y pruebas Controller/seguridad backend; no se modifican frontend ni tecnologías.
- **PASS**: filas anteriores a la nueva auditoría conservan los campos nuevos nulos cuando no sea posible reconstruirlos; no se inventa historial.
- **Preparación completada**: checklists evaluadas, tareas descompuestas y ANALYZE vigente sin blockers. La autorización de IMPLEMENT se registra por separado y se limita al alcance humano autorizado.

## Estructura del proyecto

### Documentación de esta feature

```text
specs/028-market-trends-and-demand-pricing/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── market-trends-api.md
│   └── market-demand-recalculation-api.md
└── tasks.md                 # Permanente; fase TASKS, no se genera en PLAN
```

### Código existente afectado

```text
backend/src/main/java/footballmarket/features/market/
├── domain/models/MarketTransaction.java
├── infraestructure/MarketTransactionRepository.java
└── application/ y presentation/             # consulta y agregación
backend/src/main/java/footballmarket/features/quote/
├── application/QuoteRecalculationService.java
├── application/impl/QuoteRecalculationServiceImpl.java
├── application/internal/QuotePlayerPublisher.java
├── domain/models/PlayerQuote.java
├── domain/models/QuoteExecution.java
├── domain/models/QuoteAdjustment.java
├── infraestructure/config/QuoteProperties.java
└── presentation/QuoteController.java
backend/src/main/resources/db/migration/     # migración aditiva de auditoría
backend/src/test/java/footballmarket/features/{market,quote}/
```

**Decisión de estructura**: ampliar roles y fachadas existentes de `features/market` y `features/quote`; no crear una capa técnica nueva. Los tests reflejan roles y categorías productivos según `docs/backend/testing.md`.

## Complejidad

No se declaran violaciones a la Constitución ni nuevas dependencias. La ampliación de persistencia es necesaria para la auditoría diferenciada de FR-005/FR-008 y para identificar ejecuciones solo de mercado sin atribuirles scores.

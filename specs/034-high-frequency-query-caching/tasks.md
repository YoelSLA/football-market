# Tasks: Caché de consultas frecuentes del backend

**Input**: Diseño de `specs/034-high-frequency-query-caching/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/` y `quickstart.md`).

**Prerequisitos**: `plan.md` y `spec.md`.

**Tests**: Incluir tests backend según `docs/backend/testing.md`; su ejecución corresponde al usuario. No crear ni ejecutar tests frontend. La feature no modifica endpoints ni contratos HTTP.

**Organización**: Las tareas están agrupadas por User Story y cubren las tres historias P1. La infraestructura común de caché precede a las historias; la invalidación de eventos depende de las lecturas cacheadas de US1 y US2.

## Phase 1: Setup

**Propósito**: Incorporar las dependencias que requiere la caché local.

- [X] T001 Agregar Spring Cache y Caffeine al catálogo de dependencias y al build backend, conservando las versiones administradas por Spring Boot, en `backend/gradle/libs.versions.toml` y `backend/build.gradle.kts`; actualizar el inventario tecnológico en `docs/backend/technologies.md`.

---

## Phase 2: Foundational

**Propósito**: Proveer políticas de caché diferenciadas e invalidación interna compartida para las historias.

- [X] T002 [P] Crear tests de configuración para nombres, expiración y capacidad de las seis cachés en `backend/src/test/java/footballmarket/common/config/QueryCacheConfigurationTest.java`.
- [X] T003 Implementar la configuración de Spring Cache y las seis políticas Caffeine de FR-004 en `backend/src/main/java/footballmarket/common/config/QueryCacheConfiguration.java`.
- [X] T004 Crear tests del listener que comprueben la limpieza exclusiva de las cachés indicadas por cada evento y la aplicación posterior al commit en `backend/src/test/java/footballmarket/common/config/QueryCacheInvalidationListenerTest.java`.
- [X] T005 Implementar el evento interno de actualización en `backend/src/main/java/footballmarket/common/config/QueryCacheInvalidationEvent.java` y el listener de infraestructura en `backend/src/main/java/footballmarket/common/config/QueryCacheInvalidationListener.java`, que invalida únicamente las cachés nombradas por el evento.

**Checkpoint**: Las políticas y el mecanismo común de invalidación están disponibles; ninguna historia añade persistencia o endpoints.

---

## Phase 3: User Story 1 — Consultar rankings y jugadores (Priority: P1) 🎯 MVP

**Goal**: Reutilizar lecturas equivalentes de ranking y catálogo con claves que representen los criterios efectivos.

**Independent Test**: Para ranking, filtros, listado y detalle, repetir la misma consulta, variar criterios que cambien la respuesta y consultar con caché ausente o vencida; cada respuesta debe corresponder a sus parámetros y el fallback debe conservar el comportamiento actual de PostgreSQL.

### Tests para User Story 1

- [X] T006 [P] [US1] Probar misses iniciales desde PostgreSQL, aciertos, separación por criterios efectivos y páginas vacías para el ranking en `backend/src/test/java/footballmarket/features/ranking/application/ScoreRankingQueryServiceTest.java`.
- [X] T007 [P] [US1] Probar misses iniciales desde PostgreSQL, reutilización y separación de filtros, paginación y detalle del jugador en `backend/src/test/java/footballmarket/features/catalog/application/PlayerCatalogQueryServiceTest.java`.

### Implementation for User Story 1

- [X] T008 [P] [US1] Aplicar la caché de ranking con clave determinística basada en los criterios efectivos de `ScoreRankingQuery` en `backend/src/main/java/footballmarket/features/ranking/application/impl/ScoreRankingQueryServiceImpl.java`.
- [X] T009 [P] [US1] Aplicar cachés separadas para filtros, listado y detalle usando criterios normalizados e ID de jugador en `backend/src/main/java/footballmarket/features/catalog/application/impl/PlayerCatalogServiceImpl.java`.

**Checkpoint**: Los cuatro GET de ranking y catálogo conservan el contrato y no cruzan resultados entre claves distintas.

---

## Phase 4: User Story 2 — Consultar mercados con datos pertinentes al usuario (Priority: P1)

**Goal**: Cachear consultas oficiales y P2P según filtros y paginación; aislar las respuestas P2P por `viewerId` autenticado.

**Independent Test**: Repetir consultas oficiales y P2P con los mismos criterios, variar parámetros y comparar dos usuarios autenticados distintos; la respuesta P2P nunca se comparte entre identidades.

### Tests para User Story 2

- [X] T010 [P] [US2] Probar clave, reutilización, separación por criterios y ausencia/expiración de caché del mercado oficial en `backend/src/test/java/footballmarket/features/market/application/OfficialMarketCatalogServiceTest.java`.
- [X] T011 [P] [US2] Probar clave P2P con criterios y `viewerId`, reutilización para el mismo usuario, aislamiento entre usuarios y recuperación por el flujo productivo desde PostgreSQL ante un cache miss, en `backend/src/test/java/footballmarket/features/market/application/P2PMarketCatalogServiceTest.java`.

### Implementation for User Story 2

- [X] T012 [P] [US2] Aplicar la caché del mercado oficial con todos los criterios efectivos de `OfficialMarketQuery` en `backend/src/main/java/footballmarket/features/market/application/impl/OfficialMarketCatalogServiceImpl.java`.
- [X] T013 [P] [US2] Resolver la identidad autenticada existente antes de consultar la caché P2P e incluir `viewerId` y todos los criterios en la clave, sin aceptar identidad del cliente, en `backend/src/main/java/footballmarket/features/market/application/impl/P2PMarketCatalogServiceImpl.java`.

**Checkpoint**: Los dos GET de mercado respetan parámetros y autenticación vigentes; P2P no entrega resultados personalizados de otro usuario.

---

## Phase 5: User Story 3 — Mantener lecturas coherentes tras cambios y recuperar datos faltantes (Priority: P1)

**Goal**: Invalidar únicamente las lecturas afectadas tras completar correctamente recálculos, sincronizaciones y mutaciones; recargar desde PostgreSQL ante falta, expiración o reinicio de la caché.

**Independent Test**: Precargar las lecturas afectadas, completar cada operación exitosa y consultar de nuevo; confirmar que operaciones fallidas o revertidas no se comunican como completadas y que las lecturas vuelven a la fuente persistente cuando no hay entrada vigente.

### Tests para User Story 3

- [X] T014 [P] [US3] Probar invalidación de ranking y mercado oficial tras recálculo exitoso, sin invalidación al fallar, en `backend/src/test/java/footballmarket/features/quote/application/QuoteRecalculationServiceTest.java`.
- [X] T015 [P] [US3] Probar invalidación de filtros, listado y detalle solo después de confirmar una sincronización exitosa en `backend/src/test/java/footballmarket/features/catalog/application/PlayerCatalogServiceTest.java`.
- [X] T016 [P] [US3] Probar invalidaciones de mercado P2P tras publicar/cancelar una oferta y tras completar su compra (venta para el vendedor), además de las invalidaciones de compra/venta oficial; comprobar también la ausencia de invalidación ante fallo/rollback, en `backend/src/test/java/footballmarket/features/market/application/TokenListingServiceTest.java`, `backend/src/test/java/footballmarket/features/market/application/BuyOrderServiceTest.java` y `backend/src/test/java/footballmarket/features/market/application/OfficialBuybackServiceTest.java`.

### Implementation for User Story 3

- [X] T017 [US3] Publicar la invalidación de ranking y mercado oficial únicamente tras completar correctamente el recálculo en `backend/src/main/java/footballmarket/features/quote/application/impl/QuoteRecalculationServiceImpl.java`.
- [X] T018 [US3] Publicar la invalidación de filtros, listado y detalle solo después de que la transacción de sincronización de jugadores confirme su commit, en `backend/src/main/java/footballmarket/features/catalog/application/impl/PlayerCatalogServiceImpl.java`.
- [X] T019 [US3] Publicar invalidaciones solo tras éxito y commit: mercado P2P al publicar/cancelar una oferta en `backend/src/main/java/footballmarket/features/market/application/impl/TokenListingServiceImpl.java` y al completar su compra (venta para el vendedor) en `backend/src/main/java/footballmarket/features/market/application/impl/BuyOrderServiceImpl.java`; mercado oficial tras compra/venta en `backend/src/main/java/footballmarket/features/market/application/impl/BuyOrderServiceImpl.java` y `backend/src/main/java/footballmarket/features/market/application/impl/OfficialBuybackServiceImpl.java`.

**Checkpoint**: Recálculo invalida ranking y mercado oficial; sincronización invalida catálogo; cada mutación invalida solo su mercado después del éxito. Ausencia, expiración y reinicio conservan la lectura existente desde PostgreSQL.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Propósito**: Confirmar coherencia de la solución con contratos, límites y escenarios de validación definidos.

- [X] T020 [US3] Verificar en `backend/src/test/java/footballmarket/common/config/QueryCacheStartupTest.java` que un contexto nuevo del backend inicia con las seis cachés vacías; combinarlo con las pruebas de cache miss de T006, T007, T010 y T011 para demostrar la recarga desde PostgreSQL, sin repetir assertions HTTP. La validación manual de reinicio del proceso y respuestas de los seis endpoints está descrita en `specs/034-high-frequency-query-caching/quickstart.md`.
- [ ] T021 Revisar la cobertura de las seis lecturas, claves, TTL/capacidades, aislamiento P2P, invalidaciones y fallback frente a `specs/034-high-frequency-query-caching/spec.md`, `specs/034-high-frequency-query-caching/contracts/http.md` y `specs/034-high-frequency-query-caching/quickstart.md`; actualizar solo anotaciones OpenAPI o contratos si el cambio los afecta.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 incorpora las dependencias requeridas.
- **Foundational (Phase 2)**: T002–T005 requieren T001 y bloquean las historias.
- **User Stories (Phase 3+)**: US1 y US2 requieren la infraestructura foundational y son independientes entre sí.
- **US3**: requiere US1 y US2 para que existan las lecturas que se invalidan; sus puntos de integración respetan los límites de transacción existentes.
- **Polish (Phase 6)**: T020 y T021 requieren las tres historias.

### User Story Dependencies

- **US1 (P1)**: después de Foundational; no depende de otras historias.
- **US2 (P1)**: después de Foundational; no depende de US1.
- **US3 (P1)**: después de US1 y US2, porque integra invalidaciones con las cachés de consulta implementadas por ambas.

### Parallel Opportunities

- T002 y T004 pueden prepararse en paralelo después de T001 porque cubren archivos y responsabilidades distintos; T003 sigue a T002 y T005 sigue a T004.
- En US1, T006 y T007 son independientes; T008 y T009 tocan Services distintos.
- En US2, T010 y T011 son independientes; T012 y T013 tocan Services distintos.
- US1 y US2 pueden implementarse en paralelo tras Foundational si se coordinan las tareas compartidas de configuración.
- En US3, T014 y T015 son independientes de T016; T017, T018 y T019 afectan áreas distintas, una vez acordado el evento de invalidación de T005.

### Parallel Example: User Story 1

```text
T006: tests de consultas de ranking
T007: tests de consultas de catálogo
T008: caché en ScoreRankingQueryServiceImpl
T009: cachés en PlayerCatalogServiceImpl
```

## Implementation Strategy

### MVP First

1. Completar Setup y Foundational.
2. Completar US1 para entregar caché de ranking y catálogo.
3. Revisar y validar el incremento de forma independiente; la ejecución de tests corresponde al usuario.

### Incremental Delivery

1. Completar Setup + Foundational.
2. Implementar US1 y US2 en paralelo o en orden P1; ambas entregan incrementos independientes.
3. Implementar US3 para completar invalidaciones y coherencia ante actualizaciones.
4. Completar Polish; tests aplicables permanecen pendientes de ejecución humana. Las tareas no conceden autorización de implementación, revisión, testing ni cierre.

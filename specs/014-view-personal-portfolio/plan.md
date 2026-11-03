# Implementation Plan: Consultar portfolio personal (SPEC-014)

**Branch**: `feat/014-consultar-portfolio-personal` | **Date**: 2026-10-20 | **Spec**: [`spec.md`](spec.md)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Input**: Feature specification from `specs/014-view-personal-portfolio/spec.md`

## Summary

SPEC-014 expone la **consulta del portfolio personal** del usuario autenticado: sus posiciones de tokens
por jugador, la cantidad que posee, la cantidad reservada por publicaciones vigentes, el costo de
adquisición, el valor actual a la cotización vigente y el resultado realizado y no realizado, con un
resumen financiero global, filtro por estado de posición y paginación por número de página.

El diseño técnico **no inventa ni una sola regla contable**. Las cifras financieras —costo de
adquisición, precio medio derivado, resultado realizado, no realizado y total, porcentaje global y
agregados— las produce el **componente contable que SPEC-015 (REQ-S2-20) entrega**: un Service de
solo lectura, `FinancialPositionService`, cuyo contrato es la frontera entre ambas features. SPEC-014
consume ese contrato, lo agrega **sobre el conjunto completo** de posiciones antes de filtrar y
paginar, y lo lee dentro de una **transacción `REPEATABLE READ`** para que posiciones, tenencias,
reservas y cifras pertenezcan a un mismo estado confirmado del mercado.

Lo que SPEC-014 resuelve por su cuenta es todo lo que no es contable: el contrato HTTP, la forma de la
respuesta, la lectura agregada de tenencias y reservas, los datos descriptivos del jugador, el orden y
la paginación, y los porcentajes **por posición**, que son suyos por decisión funcional (FR-028,
FR-029). La consulta es de **solo lectura**: no altera operaciones, tenencias, reservas, publicaciones,
cotizaciones ni saldos.

**Artefactos de diseño**: [`research.md`](research.md) (decisiones de Fase 0),
[`data-model.md`](data-model.md) (modelo de la respuesta y su fuente),
[`contracts/portfolio-api.md`](contracts/portfolio-api.md) (contrato HTTP) y
[`quickstart.md`](quickstart.md) (guía de validación).

## Technical Context

**Language/Version**: Java 21 (toolchain del backend; `backend/build.gradle.kts:86-87`)

**Primary Dependencies**: Spring Boot 4.1.1 · Spring Data JPA · Spring Security (JWT) · Flyway ·
PostgreSQL · Lombok · SpringDoc OpenAPI · Gradle 9.6.0 (Kotlin DSL) · Testcontainers (PostgreSQL 18) ·
JUnit Jupiter, Mockito y AssertJ. **Sin dependencias nuevas**: el único cálculo no entero —los
porcentajes por posición— se resuelve con `java.math.BigDecimal` en escala explícita y
`RoundingMode.HALF_UP`, el mismo patrón que SPEC-015 emplea en el dominio contable.

**Storage**: PostgreSQL. Tablas existentes leídas: `player_tokens` (tenencias), `token_listings`
(publicaciones y reservas), `market_transactions` y `player_token_emissions` (a través del componente
contable de SPEC-015), `players`, `teams` y `leagues` (datos descriptivos). **Una migración nueva,
solo de índice** (R-08). Sin tablas, columnas ni vistas de negocio nuevas: la consulta no persiste
nada.

**Testing**: JUnit Jupiter con `@Nested` obligatorio y una etiqueta literal por clase contenedora
(`unit`, `lightweight`, `integration`, `e2e`), según `docs/backend/testing.md` §2, §16 y la sección de
clasificación por etiquetas. PostgreSQL real por Testcontainers para las categorías Repository y
Service; `MockMvc` con el Service mockeado para la categoría Controller. La ejecución de las pruebas
corresponde al usuario.

**Target Platform**: Servidor Linux con JVM 21; base de datos PostgreSQL.

**Project Type**: Backend REST (Spring Boot) con consumidor frontend React.

**Performance Goals**: No se fijan umbrales de latencia sin mediciones reales (SPEC-014 CL-41 y
`tp/enunciado.md` §5.3). El objetivo cualitativo es que la consulta se resuelva con un **número
acotado de consultas de conjunto**, sin accesos individuales por posición (FR-039, AC35), y que la
agregación financiera se calcule una sola vez sobre el conjunto completo (R-19).

**Constraints**: Solo lectura; MUST NOT alterar el comportamiento observable de SPEC-010 ni de
SPEC-011; MUST NOT redefinir ninguna regla contable ni duplicar la fuente de verdad contable
(FR-014, CL-12); los importes siguen siendo enteros en centésimas (CL-18); la ausencia de cotización
se distingue de la falla técnica (D03, D10); el usuario no ejecuta ni una prueba sin su autorización.

**Scale/Scope**: Una feature de backend. Componentes nuevos: un Controller, tres DTOs de respuesta y
un Mapper, un Service con su implementación, un registro de vista en Model y dos excepciones propias.
Componentes modificados: dos repositorios (consultas agregadas de tenencias y de reservas), el
repositorio de jugadores (lectura por lote de la página) y el manejador global de errores (mapeo de
las excepciones del componente contable y de las propias). Dos métodos de repositorio nuevos, una
migración de índice y las pruebas de las categorías Repository, Service y Controller.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / regla | Verificación | Resultado |
| --- | --- | --- |
| **§I Simplicidad y calidad** | Un endpoint, un caso de uso, un Mapper y las DTOs que el contrato exige. Ninguna capa, interfaz ni abstracción por costumbre: no se crea una interfaz propia para el componente contable de SPEC-015, se consume la suya. Sin dependencias nuevas | PASS |
| **§II Proporcionalidad del alcance** | Solo lo que exigen FR-001…FR-047. Los casos **recomendados** de `spec.md` (orden estable ante cambios concurrentes, detalle adicional por posición) no generan trabajo obligatorio. No se implementa ninguna fórmula contable: las delega en REQ-S2-20 | PASS |
| **§III Seguridad e integridad** | La consulta es de solo lectura y opera sobre el usuario de la sesión; no acepta identificador de usuario del cliente ni expone carteras ajenas (FR-002, FR-042, CL-30). No escribe operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos (FR-037) | PASS |
| **§IV Coherencia de contratos** | No se modifica ningún contrato HTTP ajeno. El contrato del componente contable de SPEC-015 se consume tal como está; el contrato propio de esta feature queda en `contracts/portfolio-api.md` y en las anotaciones OpenAPI (FR-041) | PASS |
| **§V Trazabilidad y fuente de verdad** | Cada cifra financiera tiene una única fuente: el componente contable de SPEC-015. Las tenencias y las reservas se leen del estado que las origina. No hay segunda fuente de verdad ni libro de costo en esta feature | PASS |
| **Matriz de dependencias de la arquitectura** | El Service de esta feature consume el Service de SPEC-015: una relación **Service → Service** que `docs/backend/architecture.md` §1.3 ya enumera de forma acotada —*«otro Service exclusivamente para la integración de portfolio descrita en §5»*— y que §5 documenta con sus componentes. Esta feature **no introduce la relación por su cuenta**: la consume como dependencia ya autorizada (R-11) | PASS — la matriz y el apartado de SPEC-015 están integrados en `dev` |
| **Flujo SDD §3 (autorizaciones)** | El plan no implementa, no habilita IMPLEMENT y no ejecuta pruebas; la autorización sigue siendo humana y explícita | PASS |
| **Flujo SDD §6 (Protocolo)** | El plan no redacta la especificación ni las tareas; no se avanzan fases | PASS |
| **AGENTS.md raíz (no tests)** | El plan describe pruebas; su ejecución y los resultados son del usuario | PASS |
| **AGENTS.md backend (build)** | El control permitido es `./gradlew build -x test` desde `backend/`, solo si se modifica código del área | PASS |

**Complejidad**: sin violaciones que justifiquen excepciones. El plan no introduce una capa
intermedia, ni un puerto propio para el componente contable, ni una segunda fuente de verdad.

**Post-diseño (re-evaluación tras Fase 1)**: PASS en los mismos principios. Los artefactos de Fase 1
(data-model, contrato y quickstart) no agregan piezas: el contrato documenta el endpoint y sus reglas,
el modelo describe la forma de la respuesta y su fuente, y el quickstart solo describe cómo validarla.

## Project Structure

### Documentation (this feature)

```text
specs/014-view-personal-portfolio/
├── spec.md              # Especificación funcional (SPECIFY + CLARIFY, lista para PLAN)
├── plan.md              # Este archivo (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/
│   └── portfolio-api.md # Phase 1 output: contrato HTTP del portfolio
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/src/main/java/footballmarket/
├── controllers/
│   ├── PortfolioController.java                    # NUEVO: GET /api/portfolio/me
│   ├── dtos/responses/
│   │   ├── PortfolioResponseDTO.java               # NUEVO: summary + positions + pagination
│   │   ├── PortfolioSummaryResponseDTO.java        # NUEVO: indicadores globales
│   │   ├── PortfolioPositionResponseDTO.java       # NUEVO: detalle por posición
│   │   └── PortfolioPaginationResponseDTO.java     # NUEVO: metadatos de la página
│   ├── exceptions/
│   │   ├── InvalidPortfolioPageException.java      # NUEVO: page/size fuera de rango
│   │   └── InvalidPortfolioStatusException.java    # NUEVO: valor de status no admitido
│   └── mappers/
│       └── PortfolioMapper.java                    # NUEVO: vista de dominio -> DTOs
├── models/
│   ├── records/
│   │   └── PortfolioView.java                      # NUEVO: vista de la consulta (con el record de posición anidado)
│   └── exceptions/
│       └── PortfolioIntegrityException.java        # NUEVO: divergencia entre tenencia y reconstrucción
├── services/
│   ├── PortfolioService.java                       # NUEVO: interfaz del caso de uso
│   └── impl/
│       └── PortfolioServiceImpl.java               # NUEVO: composición del portfolio
├── repositories/
│   ├── PlayerTokenRepository.java                  # MODIFICADO: tenencias del propietario
│   ├── TokenListingRepository.java                 # MODIFICADO: reservas del propietario agrupadas
│   ├── PlayerRepository.java                       # MODIFICADO: jugadores de la página por lote
│   └── projections/
│       ├── HoldingQuantity.java                    # NUEVO: proyección de tenencias (playerId, quantity)
│       └── ReservedQuantity.java                   # NUEVO: proyección de reservas (playerId, quantity)
└── services/impl/FinancialPositionServiceImpl.java  # DE SPEC-015: componente contable consumido

backend/src/main/resources/db/migration/
└── V<n>__index_player_tokens_owner.sql            # NUEVO: índice por propietario (R-08)

docs/backend/architecture.md                         # MODIFICADO por SPEC-015: §1.3 y §5 con la relación
                                                   # Service -> Service (R-11, dependencia)

backend/src/test/java/footballmarket/
├── controllers/
│   └── PortfolioControllerTest.java                # NUEVO: @Tag("lightweight")
├── services/
│   └── PortfolioServiceTest.java                   # NUEVO: @Tag("integration")
├── repositories/
│   └── PortfolioPersistenceTest.java               # NUEVO: @Tag("integration")
└── controllers/mappers/                            # Sin pruebas propias: los mappers se prueban
                                                   # por el contrato HTTP (testing.md §2)
```

**Sin cambios**: frontend, `integrations/QuoteSource` —que llega ya extendido por SPEC-015 con la
adquisición en lote y esta feature no lo modifica—, los contratos públicos de SPEC-010 y SPEC-011,
y los componentes contables de SPEC-015, que esta feature consume sin modificar.

**Structure Decision**: se conserva la estructura física existente del backend, que refleja los roles
arquitectónicos (`docs/backend/architecture.md` §2.1). Los componentes viven en los paquetes de su rol
(`controllers`, `controllers.dtos.responses`, `controllers.mappers`, `controllers.exceptions`,
`models.records`, `models.exceptions`, `services`, `services.impl`, `repositories`), igual que las
demás features. La única excepción es el subpaquete `repositories.projections`, que aloja las
proyecciones de consulta `HoldingQuantity` y `ReservedQuantity`: se extraen del repositorio que las
produce para que el servicio y las pruebas las usen sin depender de la qualification anidada del
repositorio, que era una referencia cruzada entre capas. Las proyecciones conservan su tipo, sus
aliases de consulta (`playerId`, `quantity`) y su comportamiento.

## Diseño técnico

### 1. Componente de consulta de portfolio

| Componente | Rol | Responsabilidad | Consumido por |
| --- | --- | --- | --- |
| `PortfolioController` | Controller | Frontera HTTP: valida `page`, `size` y `status`, resuelve el usuario desde el JWT y transporta la respuesta | Cliente (frontend) |
| `PortfolioResponseDTO` + `PortfolioSummaryResponseDTO` + `PortfolioPositionResponseDTO` + `PortfolioPaginationResponseDTO` | DTO | Representación HTTP de los tres objetos de nivel superior: `summary`, `positions`, `pagination` (FR-004, CL-32) | Contrato HTTP |
| `PortfolioMapper` | Mapper | Traduce la vista de dominio a los DTOs, sin negocio ni cálculos | Controller |
| `PortfolioView` | Model (record) | Vista de la consulta: resumen, posiciones de la página y metadatos de paginación | Mapper |
| `PortfolioService` + `Impl` | Service | Caso de uso: compone el componente contable, el estado de mercado y la presentación; aplica filtro, orden y paginación | Controller |

El detalle de por qué el caso de uso vive en un Service propio y no dentro del Controller está en
[`research.md`](research.md) R-01.

### 2. Frontera contable: consumo del componente de SPEC-015

La separación de responsabilidades es explícita y no admite ambigüedad:

| Cifra | Dueño | Origen |
| --- | --- | --- |
| Costo de adquisición, precio medio, resultado realizado, valor actual, resultado no realizado, resultado total por posición | **SPEC-015** | `PlayerFinancialPosition` |
| Porcentaje global, agregados contables, `valuationComplete` | **SPEC-015** | Agregación de `FinancialPositionCalculator` (R-19) |
| Instante de la cotización, ciclo de tenencia, recuentos por rol, última operación | **SPEC-015** | Hechos del mismo plegado (R-18) |
| Cantidad de la posición, cantidad reservada, cantidad disponible | **SPEC-014** | `player_tokens` y `token_listings` |
| Porcentaje por posición: rendimiento no realizado y peso en el portfolio | **SPEC-014** | Fórmulas propias (FR-028, FR-029) |
| Posiciones abiertas y cerradas, filtro, orden, paginación, datos descriptivos del jugador | **SPEC-014** | Propio |

**Reglas de consumo que esta feature respeta** (contrato de SPEC-015 §3.2):

1. `acquisitionCost` es la cifra autoritativa; el precio medio nunca la reconstruye.
2. Una cifra nula es una cifra indefinida, nunca un cero.
3. El resultado realizado llega ya acumulado y no se reinicia.
4. El porcentaje global y los agregados contables **no se recalculan**: se aplican con la agregación
   de SPEC-015 sobre el **conjunto completo** de posiciones, nunca sobre una página.
5. Los porcentajes por posición no forman parte del contrato contable: son de esta feature.

El contrato de SPEC-015 **está sin publicar** (sus artefactos de plan son locales): esta feature lo
consume como frontera de capacidades, y su firma concreta se revisa al integrarse (R-10).

### 3. Orden de ejecución de la consulta

El orden de las operaciones es parte del diseño, no un detalle de implementación:

| Paso | Operación | Resultado |
| --- | --- | --- |
| 1 | `positionsOf(userId)` | Conjunto completo de posiciones calculadas |
| 2 | **Agregación contable sobre el conjunto completo** (R-19) | `historicalCost`, `investedCost`, `currentValue`, resultados, porcentaje global, `valuationComplete` |
| 3 | Lectura de tenencias y reservas del usuario | Cantidades y reservas por jugador |
| 4 | Cruce posición ↔ tenencia | Cantidad, reservada y disponible; detección de divergencia (R-04) |
| 5 | Filtro por `status` | Conjunto filtrado |
| 6 | Orden determinista (D07) | Conjunto ordenado |
| 7 | Paginação por número de página | Página solicitada |
| 8 | Lectura por lote de los jugadores de la página | Datos descriptivos |
| 9 | Cálculo de los porcentajes por posición | Rendimiento no realizado y peso |
| 10 | Mapeo a DTOs | Respuesta HTTP |

El resumen se calcula en el paso 2, **antes** de filtrar y paginar: por eso es idéntico con y sin
filtro y con cualquier página (AC14, AC15, AC23, AC24).

### 4. Modelo de la respuesta

| Objeto | Contenido | Fuente |
| --- | --- | --- |
| `summary` | `totalPositions`, `totalTokens`, `totalReservedTokens`, `totalAvailableTokens`, `totalInvested`, `totalCurrentValue`, `totalRealizedProfitLoss`, `totalUnrealizedProfitLoss`, `totalProfitLoss`, `totalProfitLossPercentage`, `totalBuyTransactions`, `totalSellTransactions`, `valuationComplete` | Agregación de SPEC-015 + conteos propios |
| `positions` | Ficha por jugador con datos descriptivos, estado, cantidades, cifras, porcentajes y fechas | Componente contable + estado propio |
| `pagination` | `page`, `size`, `totalElements`, `totalPages` | Propio, sobre el conjunto filtrado |

El detalle campo por campo, con su origen y su regla de nulabilidad, está en
[`data-model.md`](data-model.md) §3.

### 5. Coherencia de la lectura

La consulta completa —posiciones contables, tenencias, reservas y datos del jugador— se resuelve en
**una sola transacción de solo lectura con aislamiento `REPEATABLE READ`** (R-06), tal como exige la
coherencia de SPEC-014 (FR-038, AC34) y el propio contrato de SPEC-015 (R-15). Con el aislamiento por
defecto, cada sentencia vería una instantánea distinta y una venta confirmada entre dos lecturas
podría quedar fuera de la reconstrucción: las cantidades y los resultados no corresponderían a ningún
estado válido. Spring no cambia el aislamiento de una transacción que ya participa, de modo que la
declaración del aislamiento en esta feature es obligatoria, no opcional.

### 6. Coherencia entre la cantidad poseída y la reconstruida

El componente contable deriva la cantidad abierta de las operaciones y de la emisión; `player_tokens`
la persiste como tenencia. Ambas cifras proceden de la **misma instantánea**, de modo que son
comparables. Si divergieran, el estado del mercado sería inconsistente y las cifras informadas no
corresponderían a ningún estado real: la respuesta es un **error de integridad** (500), nunca una
elección silenciosa entre las dos fuentes (R-04, AC13, FR-040).

Esta comprobación no entra en conflicto con la regla de jugador eliminado (D38): el borrado físico de
un jugador con operaciones confirmadas está bloqueado por la integridad referencial de
`market_transactions`, y un jugador sin operaciones no produce posición. El caso vigente es la
**desactivación lógica**, en la que el jugador permanece y sus datos descriptivos se conservan; si
alguna vez faltaran, se informarían nulos sin inventar datos (FR-040).

### 7. Presentación de las posiciones cerradas y de los valores indefinidos

Las posiciones cerradas conservan su resultado realizado acumulado y **no** tienen valor actual ni
resultado no realizado: la respuesta las informa con esos campos nulos, con independencia de lo que
entregue el componente contable para ellas (R-05, FR-020, FR-022, D03). `lastTransactionAt` es nulo
cuando la posición no tiene ninguna operación confirmada —una posición abierta solo por emisión— y
ese nulo se propaga tal cual (R-05, P2). Ningún valor indefinido se sustituye por cero.

### 8. Porcentajes por posición

Dos porcentajes son de esta feature y solo de esta feature:

| Porcentaje | Fórmula | Indefinido cuando |
| --- | --- | --- |
| `unrealizedProfitLossPercentage` | `HALF_UP_2( unrealizedProfitLoss × 100 / acquisitionCost )` | El costo de adquisición es cero, o la posición carece de cotización |
| `portfolioWeightPercentage` | `HALF_UP_2( currentValue × 100 / totalCurrentValue )` | El valor total es cero o indefinido, o la posición carece de cotización o está cerrada |

El denominador del peso es el valor total del portfolio —**todas** las posiciones abiertas, no la
página consultada—, tal como exige D24 y el contrato de SPEC-015 en cuanto a que la agregación se
calcula sobre el conjunto completo. La precisión es de dos decimales con redondeo `HALF_UP`, el mismo
modo que usa SPEC-015 en el dominio contable (R-03).

### 9. Errores y mapeo HTTP

| Situación | Origen | Respuesta |
| --- | --- | --- |
| `page` negativa, `size` fuera de 1–100 o valor no entero | Propia | 400 `INVALID_PARAMETER_TYPE` |
| `status` con un valor distinto de `OPEN` o `CLOSED` | Propia | 400 `INVALID_PARAMETER_TYPE` |
| Sesión ausente, inválida o vencida | Cadena de seguridad | 401, sin cuerpo garantizado |
| El componente contable no puede reconstruir una posición | `FinancialPositionIntegrityException` (SPEC-015) | 500 `INTERNAL_ERROR` |
| Divergencia entre la tenencia y la reconstrucción | `PortfolioIntegrityException` (propia) | 500 `INTERNAL_ERROR` |
| Falla técnica de la fuente de cotizaciones | Excepción de integración (SPEC-015) | 503, sin datos financieros parciales |

Se reutiliza `INVALID_PARAMETER_TYPE`, ya vigente para parámetros de consulta inválidos, en lugar de
inventar un código nuevo; el mensaje distingue el caso. El manejador global de excepciones no tiene
un caso genérico de `FootballMarketException`, de modo que el mapeo de las excepciones del componente
contable es tarea explícita de esta feature (R-09, coherente con RA-7 del plan de SPEC-015).

### 10. Integración con el mercado

Componentes existentes que se reutilizan **sin modificación**:

| Componente | Uso en SPEC-014 |
| --- | --- |
| `PlayerToken` | Tenencia por jugador y propietario: cantidad poseída |
| `TokenListing` | Publicaciones: reservas por jugador, con su estado efectivo y su vencimiento |
| `Player`, `Team` | Datos descriptivos del jugador: nombre, equipo, posición deportiva, imagen y estado activo |
| `FinancialPositionService` (SPEC-015) | Cifras financieras por posición y agregación contable |
| `SecurityConfig`, `PersistedUserJwtAuthenticationConverter` | Autenticación: el usuario se resuelve desde el JWT, como en el resto del mercado |
| `GlobalExceptionHandler` | Se **modifica** para mapear las excepciones propias y las del componente contable |

Componentes nuevos: `PortfolioController`, sus DTOs, `PortfolioMapper`, `PortfolioView`,
`PortfolioService` y su implementación, `InvalidPortfolioPageException`,
`InvalidPortfolioStatusException` y `PortfolioIntegrityException`.

### 11. Estrategia de consultas

| # | Consulta | Cardinalidad | Soporte |
| --- | --- | --- | --- |
| 1 | Posiciones y agregados contables del usuario | 1 por usuario, por el componente de SPEC-015 | Internas de SPEC-015 |
| 2 | Tenencias del propietario | 1 por usuario | **Índice nuevo** `player_tokens (owner_user_id, player_id)` (R-08) |
| 3 | Reservas del propietario agrupadas por jugador | 1 por usuario | `idx_token_listings_seller (seller_user_id, created_at DESC)` cubre el filtro; la agrupación es un agregado sobre el conjunto filtrado |
| 4 | Jugadores de la página, con su equipo | 1 por página | Lectura por identificadores con carga del equipo |

**No hay ninguna consulta por posición** (FR-039, AC35). Dos precisiones sobre el conteo:

- La lectura de los **datos descriptivos** de los jugadores se resuelve **por lote**, en una sola
  consulta que cubre el **conjunto completo de posiciones** y no solo la página. El orden de
  presentación compara los nombres de los jugadores de las posiciones cerradas, de modo que los
  nombres deben conocerse antes de ordenar y antes de recortar la página. La consulta sigue siendo
  única y de conjunto: no hay acceso por posición.
- La **adquisición de cotizaciones** no se ejecuta en esta feature: ocurre **dentro del componente
  contable**, que la resuelve **en lote** mediante `QuoteSource.currentQuotes`, de modo que tampoco
  crece con la cantidad de jugadores distintos. No es una consulta por posición. Una fuente que no
  admita la adquisición en lote no puede atender el portfolio, y esa limitación se propaga como
  fallo técnico de cotizaciones (FR-023).

El conteo total de consultas de esta feature es **constante** (cuatro: tenencias, reservas, jugadores
y el componente contable); el del componente contable es el que SPEC-015 define (dos consultas de
conjunto y una adquisición de cotizaciones **en lote**). La consulta de tenencias **proyecta**
identificador de jugador y cantidad: no materializa las tenencias ni sus jugadores, de modo que no
abre un acceso por posición.

El índice sobre `token_listings (seller_user_id, player_id) WHERE status = 'ACTIVE'` **no se crea**:
el índice vigente por vendedor cubre el filtro y la agrupación es un agregado sobre ese conjunto. Se
revisará con medición, no por anticipación (R-08).

### 12. Estrategia de testing

Detalle en [`research.md`](research.md) R-13 y en [`quickstart.md`](quickstart.md). Resumen:

| Categoría | Contenido | Etiqueta |
| --- | --- | --- |
| Controller | Ruta, autenticación, validación de `page`, `size` y `status`, forma de la respuesta, códigos de error y no exposición de carteras ajenas | `lightweight` |
| Service | Composición del portfolio: agregación sobre el conjunto completo, filtro, orden, paginación, porcentajes por posición, divergencia de cantidades, nulos de las posiciones cerradas, ausencia de cotización y propagación del fallo técnico | `integration` |
| Repository | Consulta de tenencias del propietario, consulta agrupada de reservas y lectura por lote de jugadores | `integration` |
| Modelo de respuesta | Forma y nulabilidad de los DTOs, verificadas por el contrato HTTP | covered by Controller |

El test de Service usa el componente contable real de SPEC-015 una vez integrado; mientras la
dependencia esté pendiente puede aislarse con `@MockitoBean`, y esa condición MUST quedar registrada
en la evidencia de la fase, porque una prueba con el doble no acredita la regla contable.

No hay pruebas de frontend: el frontend está exento de testing.

### 13. Riesgos y decisiones arquitectónicas

| Riesgo | Descripción | Mitigación |
| --- | --- | --- |
| **RA-1 Dependencia contable** | Las cifras financieras las produce el componente de SPEC-015, ya implementado e integrado en `dev` | Se consume por interfaz, con el contrato como frontera y sin fórmulas paralelas. La parte financiera es ejecutable y no depende de un doble |
| **RA-2 Contrato contable** | El contrato de SPEC-015 ya está publicado e integrado; su interfaz y su agregación coinciden con lo previsto aquí | Se consume por capacidades (FR-027, FR-028, FR-031, FR-032, FR-010 de SPEC-015), no por firmas; cualquier cambio futuro vuelve a activar esta revisión |
| **RA-3 Relación Service → Service fuera de la matriz** | La matriz de `docs/backend/architecture.md` §1.3 no enumera esa relación | SPEC-015 la documenta y registra la aprobación humana; esta feature la declara como dependencia documental y no la introduce por su cuenta (R-11) |
| **RA-4 Mapeo HTTP de excepciones ajenas** | El manejador global no tiene un caso genérico de `FootballMarketException` | Esta feature mapea explícitamente las dos excepciones del componente contable, en el archivo compartido, de forma coordinada con SPEC-015, que no lo modifica (R-09) |
| **RA-5 Divergencia entre fuentes de cantidad** | La cantidad reconstruida y la tenencia persistida podrían divergir | Se comparan bajo una misma instantánea y la divergencia produce error de integridad, nunca una elección silenciosa (R-04) |
| **RA-6 Ausencia de cotización y falla técnica** | Son dos situaciones distintas y con respuestas distintas | Se propaga la ausencia como indefinido y la falla técnica como error, sin datos parciales (FR-021, FR-023) |
| **RA-7 Tamaño del conjunto de posiciones** | Para el superusuario, el conjunto coincide con el tamaño del catálogo emitido | La agregación y el filtrado son lineales sobre el conjunto recibido; el coste se mide y no se fijan latencias arbitrarias (R-15) |
| **RA-8 Dos migraciones de índice en paralelo** | SPEC-015 añade `player_token_emissions (owner_user_id)` y esta feature `player_tokens (owner_user_id, player_id)` | Se usan marcas de tiempo distintas y se coordinate la aplicación de ambas; el conflicto de archivos de migración se resuelve al integrar (R-08) |
| **RA-9 Divergencia de la regla de presentación nominal** | `openedAt`, `closedAt`, `lastTransactionAt` y los recuentos son hechos de SPEC-015 cuya forma nominal es de esta feature | Se consume el hecho sin reinterpretarlo; si esta feature cambiara su definición, el Model de SPEC-015 debe alinearse (R-05) |
| **RA-10 Evolución de la fuente de cotizaciones** | El puerto vigente admite ya la adquisición en lote (`QuoteSource.currentQuotes`), incorporada por SPEC-015; una fuente que no la implemente rechaza esa capacidad | El acceso a cotizaciones ocurre dentro del componente contable y no se ejecuta en esta feature: la sustitución por la fuente real de REQ-S2-03 no altera su comportamiento mientras conserve la adquisición en lote (R-15) |

Dependencias técnicas: SPEC-010 y SPEC-011 (dominio de mercado, tenencias, publicaciones y reservas),
SPEC-015 (componente contable y reglas contables), REQ-S2-03 (fuente definitiva de cotizaciones, hoy
sustituida por el simulador) y SPEC-002/007 (catálogo de jugadores). SPEC-014 es **consumidor**, no
dependencia.

## Archivos que deberán modificarse durante IMPLEMENT

| Archivo | Naturaleza | Alcance |
| --- | --- | --- |
| `backend/src/main/java/footballmarket/controllers/PortfolioController.java` | Nuevo | Endpoint `GET /api/portfolio/me` con validación de parámetros y resolución del usuario desde el JWT |
| `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioResponseDTO.java` | Nuevo | Respuesta con `summary`, `positions` y `pagination` |
| `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioSummaryResponseDTO.java` | Nuevo | Indicadores globales del portfolio |
| `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPositionResponseDTO.java` | Nuevo | Detalle por posición |
| `backend/src/main/java/footballmarket/controllers/dtos/responses/PortfolioPaginationResponseDTO.java` | Nuevo | Metadatos de la página |
| `backend/src/main/java/footballmarket/controllers/mappers/PortfolioMapper.java` | Nuevo | Traducción de la vista de dominio a los DTOs |
| `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioPageException.java` | Nuevo | Parámetros de paginación inválidos |
| `backend/src/main/java/footballmarket/controllers/exceptions/InvalidPortfolioStatusException.java` | Nuevo | Valor de `status` no admitido |
| `backend/src/main/java/footballmarket/models/records/PortfolioView.java` | Nuevo | Vista de dominio de la consulta |
| `backend/src/main/java/footballmarket/models/exceptions/PortfolioIntegrityException.java` | Nuevo | Divergencia entre la tenencia y la reconstrucción |
| `backend/src/main/java/footballmarket/services/PortfolioService.java` | Nuevo | Interfaz del caso de uso |
| `backend/src/main/java/footballmarket/services/impl/PortfolioServiceImpl.java` | Nuevo | Composición, filtro, orden, paginación y porcentajes por posición |
| `backend/src/main/java/footballmarket/repositories/PlayerTokenRepository.java` | Modificado | Consulta de tenencias del propietario |
| `backend/src/main/java/footballmarket/repositories/TokenListingRepository.java` | Modificado | Consulta agrupada de reservas del propietario por jugador |
| `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` | Modificado | Lectura por lote de los jugadores de la página, con su equipo |
| `backend/src/main/java/footballmarket/repositories/projections/HoldingQuantity.java` | Nuevo | Proyección de la cantidad poseída por jugador, sin materializar tenencias ni jugadores |
| `backend/src/main/java/footballmarket/repositories/projections/ReservedQuantity.java` | Nuevo | Proyección de la cantidad reservada por jugador, sin materializar publicaciones |
| `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` | Modificado | Mapeo de las excepciones propias y de las del componente contable |
| `backend/src/main/resources/db/migration/V<n>__index_player_tokens_owner.sql` | Nuevo | Índice `(owner_user_id, player_id)` |
| `backend/src/test/java/footballmarket/controllers/PortfolioControllerTest.java` | Nuevo | Categoría Controller |
| `backend/src/test/java/footballmarket/services/PortfolioServiceTest.java` | Nuevo | Categoría Service |
| `backend/src/test/java/footballmarket/repositories/PortfolioPersistenceTest.java` | Nuevo | Categoría Repository |

No se modifican el frontend, `integrations/QuoteSource` —que llega ya extendido por SPEC-015 con la
adquisición en lote—, los contratos públicos de SPEC-010 y
SPEC-011 ni los componentes contables de SPEC-015. La actualización de `docs/backend/architecture.md`
que documenta la relación Service → Service corresponde a SPEC-015 (R-11) y es dependencia de esta
feature, no archivo suyo.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones. No hay cuarto proyecto, ni patrón adicional, ni capa de compatibilidad: la feature
se apoya en el dominio de mercado existente y en el componente contable de SPEC-015, y añade solo los
componentes que su propio contrato exige.

## Verificación de alineación con SPEC-014

| Requisito o decisión | Cómo lo cubre el plan |
| --- | --- |
| FR-001…FR-012, FR-037, FR-042 | Endpoint de solo lectura sobre la identidad de la sesión, con los datos descriptivos, el estado y las cantidades (diseño §1, §10) |
| FR-013, FR-014 | Costo autoritativo y precio medio derivado tal como los entrega el componente contable; ninguna fórmula contable en esta feature (diseño §2) |
| FR-015…FR-022, FR-026 | Resultados y estado de valoración Straight-through del componente, con la ausencia propagada como indefinido y el fallo técnico propagado como error (diseño §2, §9) |
| FR-023, FR-034 | Mapeo HTTP explícito en el manejador global; fallo de cotizaciones a 503 sin datos parciales (diseño §9) |
| FR-024, FR-025, FR-029, FR-030 | Resumen global con los agregados de SPEC-015 y los conteos propios; totales de tokens con la identidad `totalTokens = totalReservedTokens + totalAvailableTokens` (diseño §4, data-model §3) |
| FR-027, FR-028 | Porcentaje global del componente de SPEC-015; porcentaje por posición propio, con denominador explícito (diseño §8) |
| FR-031, FR-032, FR-033 | Contadores por rol y fechas de la posición desde el componente; importes en centésimas enteras; porcentajes con dos decimales (diseño §2, §8) |
| FR-034 | Orden determinista con desempate por `playerId` (diseño §3, paso 6) |
| FR-035, FR-036 | Página fuera de rango responde 200 con `positions` vacío; portfolio vacío responde 200 con ceros (diseño §3) |
| FR-038 | Transacción única `REPEATABLE READ` (diseño §5) |
| FR-039 | Número acotado de consultas de conjunto, sin acceso por posición (diseño §11) |
| FR-040 | Jugador eliminado: datos descriptivos nulos; divergencia de estado: error de integridad (diseño §6, §7) |
| FR-041 | Contrato en `contracts/portfolio-api.md` y anotaciones OpenAPI (diseño §9) |
| FR-043, FR-044 | La emisión entra por el componente contable: cantidad de costo cero, no es compra, no cuenta como operación y participa de la valoración (diseño §2) |
| FR-045, FR-047 | Determinismo y reproducibilidad delegates en el componente contable; sin historial detallado de transacciones (diseño §2) |
| FR-046 | La cotización se consume del backend; esta feature no calcula cotizaciones ni depende de una implementación concreta (diseño §2, §10) |
| AC01…AC39 | Cada criterio tiene su mecanismo en el diseño y su escenario de validación en `quickstart.md` (35 escenarios); la correspondencia requisito→tarea se detalla en la fase Tasks |

**Trazado individual de los criterios de éxito** (mecanismo de diseño y escenario de validación):

| Criterio | Mecanismo | Escenario |
| --- | --- | --- |
| SC-001 | Composición en una sola consulta por conjunto, sin accesos por posición (diseño §11) | E7, E30 |
| SC-002 | Identidad `quantity = reservedQuantity + availableQuantity` en la posición y en el resumen (diseño §4) | E16, comprobación manual de tokens |
| SC-003 | Agregación sobre el conjunto completo antes de filtrar y paginar (diseño §3) | E15 |
| SC-004 | Ausencia de cotización propagada como indefinido, sin anular el resultado realizado (diseño §7) | E18 |
| SC-005 | Resultado realizado acumulado, que no se reinicia al reabrir (diseño §2) | E21 |
| SC-006 | Publicación no ejecutada sin efecto contable, aportada por el componente contable (diseño §2) | E34 |
| SC-007 | Orden determinista con desempate por `playerId` (diseño §3) | E14, E27 |
| SC-008 | Portfolio vacío como respuesta 200 con ceros (diseño §3) | E8 |
| SC-009 | Identidad obtenida del contexto de seguridad; ninguna vía de consulta ajena (diseño §1) | E2, E6 |
| SC-010 | Consulta de solo lectura, sin efectos sobre el mercado (diseño §10) | E34 |
| SC-011 | Número acotado de consultas de conjunto, sin acceso por posición (diseño §11) | E30 |
| SC-012 | Ausencia de cotización e indisponibilidad técnica como respuestas distintas (diseño §9) | E18, E19 |
| SC-013 | Importes enteros en centésimas, con signo preservado (diseño §4) | Comprobación manual de importes |
| SC-014 | Emisión inicial como cantidad de costo cero, que no es compra (diseño §2) | E20 |
| SC-015 | Determinismo y reproducibilidad delegates en el componente contable (diseño §5) | E27 |

## Estado de Git

- Rama: `feat/014-consultar-portfolio-personal`, publicada en `origin` con el commit `9335e75` y el
  Pull Request documental **#64** abierto hacia `dev`.
- Los artefactos de esta feature están versionados en la rama `feat/014-consultar-portfolio-personal`, cuyo Pull
  Request documental sigue abierto hacia `dev`. El plan no hace commit ni push sin autorización.
- La especificación de esta feature fue corregida tras la revisión de compatibilidad con SPEC-015
  (precisión del precio medio, orden contable, propiedad de los porcentajes y exposición del precio
  medio) y esos cambios tampoco están commiteados.

## Siguiente paso

El plan habilita la fase de **Checklists** evaluados y, después, `tasks.md`. No habilita IMPLEMENT: la
autorización de implementación sigue siendo un gate humano explícito y vigente, y la implementación de
las cifras financieras depende de que el componente contable de SPEC-015 exista.

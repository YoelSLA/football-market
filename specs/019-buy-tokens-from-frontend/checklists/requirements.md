# Evaluación documental: Specify / Clarify — SPEC-019

**Revisión manual**: 2026-10-23 | **Spec**: [`../spec.md`](../spec.md) | **Plan**: [`../plan.md`](../plan.md) | **Tasks**: [`../tasks.md`](../tasks.md)

Se conservan la estructura, la trazabilidad y los criterios de revisión. «Coherente» significa que el requisito está documentado, **no** que se haya implementado ni aprobado un gate. Esta revisión incorpora las seis decisiones posteriores del usuario; no se ejecutaron `/speckit.checklist` ni ANALYZE.

---

## 1. Completitud de Specify (según Constitution y SDD workflow)

| Sección | Estado documental | Observaciones |
| --- | --- | --- |
| **User Scenarios & Testing** | Coherente | Cuatro US (P1 × 3, P2 × 1); escenarios en `quickstart.md`. Cantidad inicial 1 como en la ref. 05. |
| **Requirements (Functional)** | Coherente | FR-01–FR-15 trazables a RF/CA. FR-02 lista jugadores visibles sin filtrar por stock: POST valida disponibilidad. FR-03 preserva estado/filtros existentes, no crea filtros. |
| **Key Entities** | Coherente | `MarketQuote` proviene de GET real (`price`, `effectiveDate`, `calculatedAt`); `BuyResult` de POST (`executedAt`); no se usan cotizaciones demo. |
| **Success Criteria** | Coherente | SC-03 se acota a diez códigos POST de FR-10, fallback desconocido/red/500, gestión 401 y GET `QUOTE_NOT_FOUND`; SC-01–SC-06 presentes. |
| **Assumptions** | Coherente | Sesión protegida por `AuthGuard` + `AppLayout`, cotización persistida y composición desde app explícitas. |
| **Clarificaciones** | Documentadas | Seis decisiones de la revisión incorporadas; no equivalen a un ANALYZE vigente ni aprueban implementación. |
| **Riesgos** | Coherente con límites | Precio variable, stock, doble envío y cotización ausente contemplados. Ref. 04 incluye datos sin fuente para esta feature; no inventarlos. |

---

## 2. Trazabilidad Issue #42 → Spec

| Issue RF/CA | Spec FR/US | Cobertura documental |
| --- | --- | --- |
| RF-01 Acceso a compra | FR-01, FR-02, US1 | Ruta protegida; jugadores visibles, sin filtro por stock. |
| RF-02 Información previa | FR-04, US1, US2 | Precio e instante reales del GET; estimación no es precio definitivo. |
| RF-03 Cantidad | FR-04, FR-05, US2 | Inicial 1; texto entero válido se convierte a number, error inline si inválido. |
| RF-04 Resumen | FR-04, US2 | Créditos y centésimas, estimación antes de comprar. |
| RF-05 Confirmación | FR-06–FR-08, US3 | POST sin `listingId`; cancelación sin POST. |
| RF-06 Resultado | FR-09, FR-10, FR-14, US3 | Éxito con importe ejecutado y `executedAt`; mensajes seguros. |
| RF-07 Consistencia | FR-11, US3 | Quote real invalidada por market; summary y holdings por app. |
| RF-08 Usabilidad | FR-12, FR-13, US4 | Foco, responsive, estado ocupado, prevención de doble envío. |
| CA-01 a CA-05 | US1, US2, US3 | Acceso, cotización, importe, confirmación y cancelación cubiertos en `quickstart.md`. |
| CA-06 a CA-09 | US3, US4 | Resultado, errores, bloqueo de envío y dispositivos cubiertos documentalmente; sin resultados ejecutados. |

**Fuera de alcance:** compra de publicaciones (`listingId`), venta, historial y gestión de cartera. La invalidación de Portfolio no implementa su funcionalidad.

---

## 3. Decisiones CLARIFY — Verificación de impacto

| # | Decisión | Spec FR afectados | Plan / Research / Contracts | Estado documental |
| --- | --- | --- | --- | --- |
| 1 | Botón Comprar en `/market/buy` | FR-01–FR-03, US1 | App router + `MarketBuyPage` | Coherente. |
| 2 | Notificaciones visuales (React-Toastify en diseño) | FR-09, FR-10, FR-14 | Provider app, coordinación market | Coherente; dependencia aún no instalada. |
| 3 | Market invalida quote propia; app invalida Portfolio | FR-11 | Keys mediante API raíz | Coherente; exports aún pendientes. |
| 4 | Créditos con centésimas secundarias | FR-04, US2 | `data-model.md` + contrato POST | Coherente. |
| 5 | 401 → login sin restaurar modal | Edge Cases | HTTP y `AuthGuard` | Coherente; comprobar tras implementación. |
| 6 | Componentes locales de Page sin Hooks | FR-02, FR-04 | `MarketBuyPage/MarketPlayerCard/` y `MarketBuyPage/BuyTokenModal/`, props desde Page/Hook | Coherente en diseño. |
| 7 | Features aisladas; app compone | FR-02, FR-11 | Exports mínimos solo en `index.ts` raíz de players/portfolio | Coherente en diseño. |
| 8 | Restituir foco al disparador | FR-13 | `shared/Modal`, `quickstart.md` | Coherente. |
| 9 | GET cotización real con `calculatedAt` | FR-04, FR-11 | Contrato SPEC-016; `executedAt` solo en POST | Coherente. |
| 10 | Listar sin filtrar por stock; preservar filtros/estado | FR-02, FR-03 | App lista; modal no remonta mercado ni crea filtros | Coherente con la decisión humana. |
| 11 | Inicial 1; texto numérico aceptado | FR-04, FR-05, US2 | Form Model `"1"` → number validado antes de POST | Coherente con ref. 05. |
| 12 | SC-03 y autenticación precisados | SC-03, Assumptions | Diez códigos POST + fallback/401/GET; `AuthGuard` + `AppLayout` | Coherente documentalmente, sin verificación funcional. |
| 13 | Roles de Hooks | FR-04, FR-09, FR-12 | Page Hook coordina Form/Query/Mutation; Form Hook no llama a Mutation Hook | Coherente en diseño. |
| 14 | Frontera del resultado | FR-09 | Service/Mapper devuelven `BuyResult` sin nombre; Page Hook compone el aviso con el jugador seleccionado | Coherente en diseño. |

---

## 4. Arquitectura Frontend — Validación

| Principio | Evaluación | Evidencia / límite |
| --- | --- | --- |
| **Aislamiento** | Market no importa players ni portfolio. | `plan.md` §Fronteras, arquitectura frontend arts. 6–7, 43–44. |
| **APIs públicas mínimas** | Pendiente de futura implementación. | `players/index.ts` expondrá `usePlayersQuery`, `PLAYERS_PAGE_SIZE`, `type Player`; `portfolio/index.ts` expondrá `portfolioQueryKeys`. Actualmente ambos solo exportan Pages. T-08. |
| **Composición** | App convierte Player a MarketPlayer e invalida Portfolio. `MarketBuyPage` conecta `useMarketBuyPage`, Query/Form/Mutation Hooks; los Components locales solo reciben props. | T-02/T-10/T-12/T-13/T-14/T-16; arquitectura frontend arts. 3–4 y 41. |
| **UI compartida** | Modal genérico sin negocio. | T-03/T-04; Button y Field existentes. |
| **Rutas y providers** | Ruta bajo `AuthGuard` + `AppLayout`; ToastContainer global planificado. | `AppRouter/AppRouter.tsx`, `AppProviders/AppProviders.tsx`, T-01/T-02. |
| **TanStack Query** | Key real propia en market, Portfolio desde app. | T-10/T-12/T-02; no se invalida serie simulada. |
| **HTTP** | GET/POST en Service/Mapper de market sobre infraestructura existente. | `plan.md`, `contracts/`; sin negocio en `infrastructure`. |

---

## 5. Contratos — Validación

| Artefacto | Estado documental | Observaciones |
| --- | --- | --- |
| `contracts/buy-tokens-frontend.md` | Coherente | GET current + POST buy autenticados, modelo de errores e invalidación; contratos fuentes SPEC-016/010. |
| GET real | Coherente | `QuoteResponseDTO`: `price` (long), `effectiveDate` (LocalDate), `calculatedAt` (Instant), 404 `QUOTE_NOT_FOUND`; ID en ruta solicitada. |
| POST real | Coherente | `OrderBuyResponseDTO`: ID, cantidad, precio, importe, `executedAt`; `listingId` omitido/nulo en compra directa. |
| DTO/Model/Mapper | Coherente en diseño | `BuyResult` sin `playerName`; nombre solo en notificación compuesta por Page Hook. Cálculo del GET no se confunde con ejecución del POST. T-05/T-06/T-09/T-12. |
| Validación | Coherente | `"1"` inicial; `"10"` en campo se acepta y se envía `quantity: 10`; vacío/no numérico/decimal/fuera de 1–100 se rechaza. Payload string ajeno a UI es inválido en backend. |
| Errores | Coherente | Diez códigos POST de FR-10, fallback genérico, 401 por sesión y GET 404 como ausencia de cotización. SC-03 mide ese conjunto cerrado. |
| Invalidación | Coherente en diseño | Quote real por market; summary + prefijo holdings por app mediante API pública. |

---

## 6. Riesgos — Estado

| Riesgo | Mitigación documentada | Estado de revisión |
| --- | --- | --- |
| Precio cambia entre GET y POST | `calculatedAt` real del GET y precio/`executedAt` definitivos del POST. | Coherente. |
| Doble envío | Guarda síncrona, busy, controles deshabilitados. | Coherente; verificación UI pendiente. |
| Stock desconocido en listado | Visibilidad por catálogo; disponibilidad por POST. | Aclarado en FR-02; verificación pendiente. |
| Foco/overlay/estado | Modal con restauración de foco sin remount de mercado; preservar filtros existentes. | Coherente; comprobación visual pendiente. |
| Referencias 04/05 | Jerarquía visual aplicable sin inventar métricas; cantidad inicial 1 conforme ref. 05. | Fidelidad humana pendiente. |
| Frontend sin tests | Escenarios manuales de `quickstart.md`; build no sustituye testing. | Pendiente del usuario tras implementación. |

---

## 7. Pendientes para PLAN → TASKS

`tasks.md` conserva T-00–T-19 sin tareas completas; esta tabla evalúa cobertura y dependencias, no replica progreso.

| IDs | Cobertura documental y dependencia |
| --- | --- |
| T-00/T-01/T-15 | React-Toastify, provider y notificación antes de T-12. |
| T-03/T-04 | Modal genérico antes de T-14. |
| T-05–T-07/T-09/T-10 | DTO, Model, formatos, GET real, mapper y query antes de compra/visualización. |
| T-08 | Exports mínimos raíz autorizados para app en T-02. |
| T-10/T-12–T-14/T-16 | Query Hook de quote por ids visibles, Form Hook sin mutación, Page Hook coordinador; ficha y modal locales sin Hooks antes de Page. Preservar filtros/estado existentes sin crearlos. |
| T-16 → T-11 → T-02 | Page, API raíz market y composición/ruta app sin ciclos ni filtrado de stock. |
| T-17/T-18/T-19 | Escenarios manuales a cargo del usuario y build permitido del agente después de T-02; no acreditados aquí. |

**Límite de filtros:** FR-03 obliga a no perder estado al abrir/cerrar, pero no incorpora controles de filtro nuevos. Los filtros ilustrados en ref. 04 sin fuente ni comportamiento definido requieren alcance separado si se solicitan como funcionalidad. El listado paginado y sus estados de carga/error/vacío sí tienen tareas T-02/T-16.

---

## 8. Conclusión

**Resultado: correcciones arquitectónicas incorporadas documentalmente.** Los checklists especializados de [arquitectura](architecture.md), [API y seguridad](api-security.md) y [UX](ux.md) están generados con marcadores sin evaluar: la revisión humana formal sigue pendiente. El aislamiento de market, la composición desde app, la cotización persistida con `calculatedAt` y las dependencias T-00–T-19 deben ser contrastados por un nuevo ANALYZE; esta revisión no lo sustituye.

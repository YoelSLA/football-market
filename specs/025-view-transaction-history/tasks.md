---

description: "Tareas de implementación de SPEC-025"
---

# Tasks: Visualizar historial personal de transacciones

**Input**: Documentos de diseño en `/specs/025-view-transaction-history/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/transaction-history.md`, `quickstart.md` y checklists evaluadas.

**Tests**: La funcionalidad es exclusivamente frontend y está alcanzada por la exención temporal:
no crear tests ni infraestructura de testing. Los controles permitidos y la validación funcional
corresponden al usuario en los gates definidos por `frontend/AGENTS.md`.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Estas tareas no autorizan
implementación ni acreditan revisión, testing o cierre. Después de TASKS corresponde
`/speckit.analyze`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede realizarse en paralelo porque usa archivos distintos y no depende de una tarea
  incompleta.
- **[Story]**: Historia de usuario trazada desde `spec.md`.
- Cada tarea identifica los archivos concretos que debe modificar o crear.

## Phase 1: Setup (estructura de la pantalla)

**Purpose**: Confirmar que no se necesita inicialización adicional: la feature `portfolio`, el
cliente HTTP, TanStack Query, router, layouts, componentes compartidos y tooling ya existen. No se
crean archivos o carpetas vacíos ni se agregan dependencias.

---

## Phase 2: Foundational (frontera de datos compartida)

**Purpose**: Implementar el flujo DTO → Mapper → Model → Service → Query Hook requerido por todas
las historias.

**⚠️ CRITICAL**: Ninguna historia puede iniciarse hasta completar esta fase.

- [ ] T001 [P] Declarar `OperationHistoryResponseDTO` y el array de respuesta en `frontend/src/features/portfolio/types/dtos.ts` y exponerlos selectivamente desde `frontend/src/features/portfolio/types/index.ts`
- [ ] T002 [P] Declarar `TransactionHistoryEntry` y `TransactionType` en `frontend/src/features/portfolio/types/models.ts` y exponer únicamente los modelos necesarios desde `frontend/src/features/portfolio/types/index.ts`
- [ ] T003 Implementar el mapeo DTO → Model que conserva campos, valores y orden en `frontend/src/features/portfolio/portfolio.mapper.ts`
- [ ] T004 Incorporar `getTransactionHistory(signal)` autenticado contra `/users/me/transactions` y aplicar el mapper en `frontend/src/features/portfolio/portfolio.service.ts`
- [ ] T005 [P] Agregar la key estable del historial en `frontend/src/features/portfolio/constants/portfolioQueryKeys.ts`
- [ ] T006 Crear `useTransactionHistoryQuery` con señal de cancelación en `frontend/src/features/portfolio/hooks/queries/useTransactionHistoryQuery.ts` y actualizar `frontend/src/features/portfolio/hooks/queries/index.ts` y `frontend/src/features/portfolio/hooks/index.ts`

**Checkpoint**: La frontera de datos queda disponible sin exponer DTO a la UI ni duplicar estado
remoto.

---

## Phase 3: User Story 1 — Consultar compras y ventas personales (Priority: P1) 🎯 MVP

**Goal**: Mostrar el historial confirmado, completo y ordenado con todos los datos financieros
disponibles y sin interacción sobre el jugador.

**Independent Test**: Con una sesión que tenga compras y ventas, la ruta muestra una entrada por
transacción, en el orden recibido, con tipo, ID, jugador, cantidad, precio, total y fecha/hora; el
nombre del jugador es texto plano.

### Implementation for User Story 1

- [ ] T007 [P] [US1] Crear la cabecera semántica local con título, descripción, contador singular/plural e icono consumido desde `frontend/src/shared/icons/index.ts` en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryHeader/TransactionHistoryHeader.tsx` y su API local en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryHeader/index.ts`, sin forzar `PageHeading` si su Box Model compartido no satisface la retícula máxima de 8 px
- [ ] T008 [P] [US1] Crear la representación semántica de una operación con `BUY → COMPRA`, `SELL → VENTA`, ID, jugador no interactivo, cantidad, importes mediante `formatCredits` y fecha mediante `formatDateTime` de `frontend/src/features/portfolio/utils/index.ts` en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEntry/TransactionHistoryEntry.tsx` y su API local en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEntry/index.ts`
- [ ] T009 [US1] Crear el contenido de datos que conserva orden y usa `transactionId` como key en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.tsx` y su API local en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/index.ts`
- [ ] T010 [US1] Orquestar Query Hook, cabecera y colección dentro de `PageLayout` en `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.tsx`
- [ ] T011 [US1] Exponer la Page desde `frontend/src/features/portfolio/pages/TransactionHistoryPage/index.ts`, `frontend/src/features/portfolio/pages/index.ts` y `frontend/src/features/portfolio/index.ts`, asociar `/portfolio/transactions` en `frontend/src/app/router/AppRouter/AppRouter.tsx`, habilitar la pestaña de Cartera y retirar el placeholder duplicado de Mercado en `frontend/src/app/navigation/sections.ts`

**Checkpoint**: US1 ofrece el incremento MVP de solo lectura con datos confirmados.

---

## Phase 4: User Story 2 — Comprender el estado de la consulta (Priority: P1)

**Goal**: Diferenciar consulta pendiente, error y recuperación sin presentar datos parciales u
obsoletos como vigentes.

**Independent Test**: Una consulta pendiente comunica carga sin indicar cero operaciones; un fallo
muestra alerta, aclara que no modificó operaciones y permite reintentar hasta obtener datos o vacío.

### Implementation for User Story 2

- [ ] T012 [P] [US2] Crear los bloques accesibles de carga y error con `role="status"`, `role="alert"` y `Button` de reintento en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryState/TransactionHistoryState.tsx` y su API local en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryState/index.ts`
- [ ] T013 [US2] Integrar `isPending`/`isFetching`, error con fallback mediante `getErrorMessage` de `frontend/src/infrastructure/http/index.ts`, `refetch`, `aria-busy` y ocultamiento de datos no vigentes en `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.tsx` y `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.tsx`

**Checkpoint**: US2 distingue carga, error y recuperación; el manejo de 401 sigue perteneciendo a
la sesión global.

---

## Phase 5: User Story 3 — Orientarse cuando todavía no hay operaciones (Priority: P2)

**Goal**: Presentar un resultado vacío exitoso y ofrecer acceso explícito al mercado.

**Independent Test**: Un `200 []` muestra el mensaje vacío, no una alerta, y la única acción lleva
a `/market/buy`.

### Implementation for User Story 3

- [ ] T014 [P] [US3] Crear el estado vacío anunciable con `Button` “Ir al mercado” en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEmptyState/TransactionHistoryEmptyState.tsx` y su API local en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEmptyState/index.ts`
- [ ] T015 [US3] Integrar la distinción `[]`/error y la navegación interna a `/market/buy` en `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.tsx` y `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.tsx`

**Checkpoint**: US3 completa los cuatro estados remotos sin agregar mutaciones.

---

## Mandatory Human Gate — Fase 1 frontend

Después de T015 se debe detener la implementación. El usuario revisa integridad de contratos,
datos, rutas, JSX semántico y los controles permitidos. No iniciar Phase 6 hasta recibir
autorización humana explícita para la maquetación fina conforme a `frontend/AGENTS.md`.

Este gate no es una Task técnica, no se marca en este archivo y no acredita CODE REVIEW ni
TESTING.

---

## Phase 6: User Story 4 — Consultar el historial en distintos tamaños (Priority: P2)

**Goal**: Aplicar el wireframe aprobado: tabla de seis columnas en escritorio, cuatro grupos en
tablet y tarjetas en móvil, preservando todos los datos sin scroll horizontal.

**Independent Test**: La misma colección conserva todos sus campos en 1920 × 913 px, 769–1024 px
y 360–768 px; escritorio no tiene scroll global y desplaza solo la región de filas.

### Implementation for User Story 4 — Fase 2 frontend

- [ ] T016 [P] [US4] Implementar el presupuesto vertical y la retícula máxima de 8 px de la Page en `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.module.scss`, y el panel, cabecera de columnas y región de scroll que pertenecen al contenido en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.module.scss`
- [ ] T017 [P] [US4] Implementar las seis columnas desktop, cuatro grupos tablet y tarjeta móvil con nombres de hasta dos líneas en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEntry/TransactionHistoryEntry.module.scss`
- [ ] T018 [P] [US4] Implementar jerarquía visual, contador y tratamiento responsive de la cabecera en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryHeader/TransactionHistoryHeader.module.scss`
- [ ] T019 [P] [US4] Implementar superficies y alineación accesible para carga, error y vacío en `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryState/TransactionHistoryState.module.scss` y `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEmptyState/TransactionHistoryEmptyState.module.scss`
- [ ] T020 [US4] Incorporar únicamente wrappers y `className` necesarios para la maquetación aprobada en `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.tsx`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryHeader/TransactionHistoryHeader.tsx`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEntry/TransactionHistoryEntry.tsx`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.tsx`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryState/TransactionHistoryState.tsx` y `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEmptyState/TransactionHistoryEmptyState.tsx`, sin alterar contratos, datos, eventos ni atributos accesibles validados en Phase 1

**Checkpoint**: US4 satisface el Box Model y las tres composiciones del wireframe sin duplicar
árboles de contenido ni introducir scroll horizontal.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Cerrar coherencia interna sin ampliar alcance.

- [ ] T021 Revisar y eliminar exports o estilos muertos introducidos por SPEC-025 en `frontend/src/features/portfolio/index.ts`, `frontend/src/features/portfolio/pages/index.ts`, `frontend/src/features/portfolio/hooks/index.ts`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/TransactionHistoryPage.module.scss`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryContent/TransactionHistoryContent.module.scss`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryHeader/TransactionHistoryHeader.module.scss`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEntry/TransactionHistoryEntry.module.scss`, `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryState/TransactionHistoryState.module.scss` y `frontend/src/features/portfolio/pages/TransactionHistoryPage/components/TransactionHistoryEmptyState/TransactionHistoryEmptyState.module.scss`
- [ ] T022 Revisar la trazabilidad final de FR-001–FR-020 y SC-001–SC-007 contra `specs/025-view-transaction-history/quickstart.md` y registrar cualquier desviación documental sin modificar requisitos para justificar la implementación

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: sin dependencias.
- **Phase 2**: no depende de setup adicional y bloquea todas las historias.
- **US1 (Phase 3)**: depende de Phase 2; entrega el MVP.
- **US2 (Phase 4)**: depende de la Page y el contenido creados por US1.
- **US3 (Phase 5)**: depende de la discriminación de estados de US2.
- **Mandatory Human Gate**: bloquea toda maquetación fina después de T015.
- **US4 (Phase 6)**: depende de US1–US3 y de autorización humana explícita.
- **Polish (Phase 7)**: depende de todas las historias requeridas.

### User Story Dependencies

```text
Foundation → US1 (MVP) → US2 → US3 → Human Gate → US4 → Polish
```

- **US1** es funcionalmente verificable con datos después de Foundation.
- **US2** reutiliza la Page de US1, pero su estado de error/carga se valida de forma aislada.
- **US3** agrega únicamente el resultado vacío y su destino aprobado.
- **US4** modifica solo presentación y wrappers permitidos una vez validada la estructura.

### Parallel Opportunities

- T001 y T002 pueden realizarse en paralelo; T005 también es independiente de ambos.
- T007 y T008 pueden realizarse en paralelo después de Foundation.
- T012 y T014 afectan componentes distintos, pero deben integrarse secuencialmente mediante T013 y
  T015 para preservar checkpoints por historia.
- T016–T019 pueden realizarse en paralelo después del gate humano; T020 integra sus clases.

---

## Parallel Example: User Story 1

```text
Task T007: crear TransactionHistoryHeader.tsx
Task T008: crear TransactionHistoryEntry.tsx
```

## Parallel Example: User Story 4

```text
Task T016: maquetar TransactionHistoryPage.module.scss y TransactionHistoryContent.module.scss
Task T017: maquetar TransactionHistoryEntry.module.scss
Task T018: maquetar TransactionHistoryHeader.module.scss
Task T019: maquetar los estados remotos
```

---

## Implementation Strategy

### MVP First

1. Completar Setup y Foundation.
2. Implementar US1 hasta T011.
3. Detenerse y revisar el incremento con datos confirmados.
4. Continuar con US2 y US3 para completar la estructura funcional.
5. Detenerse obligatoriamente después de T015 para el gate humano frontend.

### Incremental Delivery

1. Foundation establece una única frontera de datos tipada.
2. US1 entrega lectura con datos.
3. US2 agrega carga, error y recuperación.
4. US3 agrega vacío y acceso al mercado.
5. US4 aplica responsive y Box Model sin cambiar lógica aprobada.
6. Polish revisa coherencia y trazabilidad sin ampliar alcance.

### Límites operativos

- No modificar backend, endpoint ni contratos de SPEC-020.
- No crear tests frontend ni infraestructura asociada.
- No ejecutar tests desde el agente; los controles y la validación corresponden al usuario.
- No iniciar implementación sin `/speckit.analyze` satisfactorio y autorización explícita.
- No iniciar Phase 6 sin completar el gate humano definido después de T015.

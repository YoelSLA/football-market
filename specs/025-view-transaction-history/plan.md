# Implementation Plan: Visualizar historial personal de transacciones

**Branch**: `feat/025-view-transaction-history` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/025-view-transaction-history/spec.md`

## Summary

Incorporar en la feature frontend `portfolio` una pantalla protegida en
`/portfolio/transactions` que consuma el contrato existente
`GET /api/users/me/transactions`, transforme sus DTO a modelos propios y represente compras y
ventas confirmadas mediante tabla en escritorio, cuatro grupos en tablet y tarjetas en móvil.
La solución extiende las fronteras existentes de Service, Mapper, TanStack Query, router y
navegación; reutiliza layouts, `PageHeading`, `Button`, iconos, formatters y tokens actuales, sin
cambios backend, nuevas dependencias ni ampliaciones funcionales.

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0, SCSS Modules
**Primary Dependencies**: React Router DOM 7.18.4, TanStack Query 5.103.1, Axios 1.20.0,
Lucide React 1.59.0 mediante `shared/icons`
**Storage**: N/A; el historial es estado remoto en Query Cache y no se persiste en cliente
**Testing**: Frontend temporalmente exento de tests; el usuario ejecutará `npm run build` y
`npm run biome:fix` en los gates de implementación definidos por `frontend/AGENTS.md`
**Target Platform**: Navegadores web soportados por Vite; viewport mínimo aprobado de 360 px
**Project Type**: Aplicación web con frontend React y backend Spring Boot existente
**Performance Goals**: Mostrar la colección completa recibida sin recalcular, enriquecer ni
solicitar datos por operación; scroll limitado a la región de filas en escritorio
**Constraints**: Solo frontend; ruta autenticada; sin scroll global a 1920 × 913 px; espacios
locales de hasta 8 px; sin scroll horizontal; sin filtros, ordenamiento, paginación ni navegación
desde el jugador
**Scale/Scope**: Una Page nueva, una lectura HTTP existente y sus fronteras DTO/Model/Mapper,
query, componentes locales, estilos, ruta y navegación

## Constitution Check

*GATE inicial y posterior al diseño: PASS.*

- **Simplicidad y mínimo alcance**: se amplía `portfolio`, propietaria del concepto; no se crea
  feature, store, tabla genérica, dependencia ni capa nueva.
- **Proporcionalidad**: el plan cubre únicamente consulta, estados, navegación y responsive
  aprobados; excluye toda capacidad enumerada fuera de alcance.
- **Seguridad e integridad**: el Service usa `authenticated: true`; la identidad continúa
  derivándose exclusivamente del JWT y no existe entrada de usuario para sustituirla.
- **Coherencia de contratos**: DTO, Model y Mapper reflejan el endpoint existente sin modificarlo;
  `playerId` se conserva en la frontera de datos, pero no habilita navegación ni enriquecimiento.
- **Trazabilidad**: SPEC-025, issue #27, rama, wireframe, contrato y artefactos de diseño quedan
  enlazados.
- **Arquitectura frontend**: se conserva el flujo
  `Page/Component → Query Hook → Service → Mapper → Model`; router y navegación permanecen en
  `app`, y la presentación específica en `features/portfolio`.
- **Verificación**: PLAN es documental. No se ejecutan tests, builds ni hooks; los controles de
  implementación quedan reservados al usuario y a los gates del área.
- **Desacople UI**: la futura implementación deberá detenerse tras la fase de lógica, estado y
  estructura antes de iniciar la maquetación fina, conforme a `frontend/AGENTS.md`.

## Project Structure

### Documentation (this feature)

```text
specs/025-view-transaction-history/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── transaction-history.md
├── checklists/
│   └── requirements.md
└── tasks.md                       # futuro: /speckit.tasks, no creado por PLAN
```

### Source Code (repository root)

```text
frontend/src/
├── app/
│   ├── navigation/
│   │   └── sections.ts            # habilitar Cartera y retirar placeholder de Mercado
│   └── router/AppRouter/
│       └── AppRouter.tsx           # asociar la ruta protegida
└── features/portfolio/
    ├── constants/
    │   └── portfolioQueryKeys.ts   # key estable del historial
    ├── hooks/queries/
    │   └── useTransactionHistoryQuery.ts
    ├── pages/TransactionHistoryPage/
    │   ├── TransactionHistoryPage.tsx
    │   ├── TransactionHistoryPage.module.scss
    │   ├── components/             # cabecera, contenido/estados y operación
    │   └── index.ts
    ├── types/
    │   ├── dtos.ts
    │   └── models.ts
    ├── portfolio.mapper.ts
    └── portfolio.service.ts
```

**Structure Decision**: La pantalla pertenece a `portfolio` porque muestra actividad financiera
personal y comparte sus fronteras, formatters y navegación. Los componentes de tabla/tarjeta son
locales a `TransactionHistoryPage`; no se promueven a `shared` ni se fuerza la reutilización de
componentes de tenencias con semántica distinta.

## Design Decisions

### Flujo de datos

1. `useTransactionHistoryQuery` usa `portfolioQueryKeys.transactions` y entrega la señal de
   cancelación al Service.
2. `portfolioService.getTransactionHistory(signal)` realiza la lectura autenticada del endpoint
   existente y transforma el array mediante el mapper.
3. La Page consume directamente el Query Hook: no se crea Page Hook porque no existe coordinación
   suficiente más allá de carga, error, reintento y navegación declarativa.
4. Durante `isPending` o `isFetching` no se expone la colección como vigente. Ante error no se
   muestran filas anteriores; el reintento usa `refetch`.
5. El orden recibido y `transactionId` como key se conservan sin ordenar, deduplicar ni recalcular.

### Presentación y navegación

- `AppRouter` agrega `/portfolio/transactions` dentro de `AuthGuard` y `AppLayout`.
- `NAV_SECTIONS` asigna esa ruta a `Cartera > Historial de transacciones` y elimina el placeholder
  duplicado `Mercado > Historial de operaciones`.
- `PageLayout`, `PageHeading`, `Button`, `History`/`FileText`, `formatCredits` y
  `formatDateTime` se reutilizan cuando su contrato satisface la pantalla.
- Una única colección semántica cambia su composición visual por breakpoint: seis columnas en
  escritorio, cuatro zonas en tablet y tarjetas en móvil; no se duplican árboles simultáneos
  ocultos por CSS.
- `Ir al mercado` navega a `/market/buy`; el nombre del jugador siempre es texto.

### Estados y accesibilidad

- Carga: contenedor con `aria-busy="true"` y mensaje con `role="status"`.
- Error: bloque con `role="alert"`, mensaje estable y `Button` para reintentar.
- Vacío: bloque con `role="status"` y `Button`/navegación a mercado.
- Datos: encabezados visibles en escritorio, etiquetas explícitas en tarjetas y foco visible
  heredado de los controles compartidos.

## Post-Design Constitution Check

*PASS.* El diseño no introduce violaciones, incógnitas ni excepciones. La interfaz HTTP es de solo
lectura y ya existe; el contrato frontend documentado no altera backend. La solución respeta las
dependencias permitidas y el gate humano entre estructura y maquetación. No corresponde completar
Complexity Tracking.

## Complexity Tracking

No aplica: no existen violaciones constitucionales que justificar.

# Plan de implementación: Visualizar ranking de jugadores

**Rama observada**: `feat/018-paginated-rankings` (distinta de la rama histórica de SPEC-022; no se renombró) · **Fecha**: 2026-11-12 · **Spec**: [`spec.md`](spec.md)

**Input**: `specs/022-view-player-rankings/spec.md` — revisión vigente 2026-11-12.

## Resumen

Actualizar la vista de ranking en `/players/ranking` para consumir los rankings paginados de rendimiento y cotización. La tabla envía al backend paginación y filtros; el podio usa una consulta independiente sin filtros con `page=0&size=3`. Ambas vistas consumen la identidad `player` devuelta por sus contratos, conservan orden y posiciones globales, y presentan metadatos/opciones recibidos. No se incorpora lógica de negocio de ranking en el frontend ni dependencia nueva.

## Contexto técnico

| Aspecto | Diseño |
| --- | --- |
| Lenguaje/plataforma | TypeScript 7.0.2, React 19.3.0, Node 24.19.0; SPA web. |
| Dependencias | Axios, TanStack Query, React Router y Sass existentes; sin dependencias nuevas. |
| Estado/datos | Estado de query en TanStack Query; estado de criterios de tabla dentro de la Page/Hook permitido. Sin persistencia local específica. |
| Contratos | Score: SPEC-018 `GET /api/rankings/score`; quote: SPEC-016 `GET /api/rankings/quote`. Ver `contracts/player-rankings-consumption.md`. Son contratos objetivo; su estado de implementación se comprueba en sus specs y no se presume. |
| Verificación | Frontend exento temporalmente de tests. No crear ni ejecutar tests. Quickstart manual; sin build en PLAN. |
| Alcance | Una Page de ranking, componentes locales necesarios, coordinación de query, filtros/página, navegación y presentación. No tocar backend, APIs, persistencia ni catálogo. |
| Rendimiento | Consultas limitadas a una página de tabla y tres entradas del podio; no descargar listas completas. Sin SLA nuevo. |
| Restricciones | JWT por infraestructura HTTP existente; no exponer errores técnicos; conservar posiciones globales; accesibilidad y responsive según spec y arquitectura frontend. |

## Constitution Check — previo a Phase 0

| Principio | Evaluación | Resultado |
| --- | --- | --- |
| I. Simplicidad/calidad | Reutiliza transporte, queries, componentes/recursos y estilos existentes; sin nuevas dependencias o capas innecesarias. | PASS |
| II y VI. Proporcionalidad/mínimo alcance | Solo presenta y consulta contratos aprobados; paginación, filtros y podio son requisitos vigentes. No incluye enriquecimiento desde catálogo ni funcionalidades adicionales. | PASS |
| III. Seguridad/integridad | Conserva autenticación JWT existente; no manipula credenciales ni revela detalles técnicos. | PASS |
| IV. Coherencia contractual | Los dos contratos fuente se referencian; DTO/mapper/UI deben ajustarse conjuntamente durante implementación. La fase no afirma que sus endpoints objetivo estén desplegados. | PASS |
| V. Trazabilidad | Reutiliza SPEC-022, issue y rama existentes; la rama no se modifica. Estado remoto no verificado ni actualizado. | PASS |
| Verificación y autorizaciones | No tests/build en PLAN; frontend sin tests. PLAN no autoriza IMPLEMENT. | PASS |

Sin violaciones constitucionales; Complexity Tracking no requiere excepción.

## Investigación y decisiones (Phase 0)

Decisiones, rationale y alternativas están en [`research.md`](research.md): reutilizar TanStack Query/Axios; separar query de tabla y podio; consumir player y filterOptions del backend; mantener estado de filtros/paginación local a la Page; conservar posiciones globales; reutilizar tema, componentes y recursos. No quedan `NEEDS CLARIFICATION` técnicas que impidan el diseño conforme a los contratos actuales.

## Diseño y contratos (Phase 1)

- [`data-model.md`](data-model.md) define los DTOs conceptuales, filtros, estado de consulta y transformación a presentación.
- [`contracts/player-rankings-consumption.md`](contracts/player-rankings-consumption.md) referencia los contratos autoritativos de SPEC-018 y SPEC-016 y define los límites del consumidor.
- [`quickstart.md`](quickstart.md) aporta escenarios manuales, sin ejecutar verificaciones.

### Estructura objetivo

Seguir la arquitectura real de `frontend/src/features/players/`: una Page modular para el ranking con los Components locales que requieran responsabilidades distintas; Services/Mapper y Hooks en sus ubicaciones autorizadas; DTOs y Models separados en `types/`. La Page de la feature se publica desde los barrels correspondientes; rutas y navegación global se registran en `frontend/src/app/`. Los estilos propios usan módulos colocalizados y tokens/recursos existentes. No crear carpetas o componentes por uniformidad; consultar la matriz de dependencias y contratos de rol al implementar.

El alcance concreto de archivos se determina al descomponer Tasks desde el árbol vigente, no se fija una lista especulativa de componentes. No se modifican salidas generadas.

## Constitution Check — posterior al diseño

| Principio | Re-evaluación | Resultado |
| --- | --- | --- |
| Simplicidad y mínimo alcance | Dos consultas paginadas necesarias, sin dataset completo, catálogo adicional, nueva dependencia ni abstracción global. | PASS |
| Seguridad y contratos | Esquemas y errores delegados a contratos backend referenciados; UI consume datos tipados y conserva JWT común. | PASS |
| Arquitectura | Diseño respeta modularidad frontend, API pública, responsabilidades de roles y estilos colocalizados; ubicación fina se confirma en implementación conforme a matrices. | PASS |
| Verificación | Guía manual; no test, build ni control ejecutable realizado. | PASS |

## Evidencia de PLAN

| Elemento | Resultado |
| --- | --- |
| Setup oficial | `.specify/scripts/bash/setup-plan.sh --json` |
| `FEATURE_SPEC` | `specs/022-view-player-rankings/spec.md` |
| `IMPL_PLAN` | `specs/022-view-player-rankings/plan.md` |
| `SPECS_DIR` | `specs/022-view-player-rankings` |
| `BRANCH` devuelto por setup | `022-view-player-rankings`; `git branch --show-current`: `feat/018-paginated-rankings`. La rama activa no corresponde al scope de SPEC-022; se conserva intacta y se reporta la divergencia. |
| Artefactos de diseño | `plan.md`, `research.md`, `data-model.md`, `contracts/player-rankings-consumption.md`, `quickstart.md`. |
| Hooks | `.specify/extensions.yml` ausente; hooks before/after-plan omitidos según definición. |
| Límites | No se ejecutaron tests, build, checklists, Tasks, Analyze, implementación ni handoff. No se modificó `spec.md` ni seguimiento remoto. |

**Vigencia de esta evidencia**: la tabla anterior es una instantánea del cierre de PLAN el 2026-11-12. No describe el estado SDD posterior: CHECKLIST, TASKS y ANALYZE se registraron después en los artefactos de la Feature. Esta aclaración no altera el registro histórico de PLAN.

La fase PLAN termina aquí. El paso de proceso siguiente es evaluación de checklists; Tasks no se ejecuta automáticamente y no hay autorización de implementación derivada de este plan.

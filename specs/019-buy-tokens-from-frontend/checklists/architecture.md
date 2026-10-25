# Arquitectura Checklist: Comprar tokens desde el frontend

**Purpose**: Calidad de los requisitos y del diseño de aislamiento, roles y composición.
**Created**: 2026-10-23
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [tasks.md](../tasks.md)

**Review Ownership**: Checklist especializado del revisor; marcar `[x]` solo después de evaluación humana. La generación no acredita aprobación ni implementación.

## Encapsulación y composición

- [x] CHK001 ¿Está definido inequívocamente que `features/market` no depende directa ni indirectamente de `players` o `portfolio`? [Consistencia, Spec §FR-02/FR-11, Plan §Fronteras]
- [x] CHK002 ¿Están delimitados los exports mínimos de las APIs raíz de players, portfolio y market y el papel de `app` como único compositor entre ellas? [Completitud, Plan §Exports mínimos, Tasks T-02/T-08/T-11]
- [x] CHK003 ¿Queda claro que `infrastructure/http` no incorpora lógica específica de compra ni de cotización? [Consistencia, Plan §Resumen, Tasks T-09]

## Roles y ciclo de datos

- [x] CHK004 ¿Está documentada la pertenencia exclusiva de `MarketPlayerCard` y `BuyTokenModal` a `MarketBuyPage`, y que los Components locales reciben datos/acciones por props sin conectar Hooks? [Consistencia, Plan §Responsabilidades, Tasks T-13/T-14/T-16]
- [x] CHK005 ¿Se distingue la responsabilidad del Form Hook (validación), Query Hook (GET), Mutation Hook (POST) y Page Hook (coordinación, mensajes e invalidación), sin dependencia Form Hook → Mutation Hook? [Claridad, Plan §Fronteras, Tasks T-10/T-12/T-16]
- [x] CHK006 ¿Está especificado el flujo de consulta por los ids visibles y la reutilización de la misma query en ficha y modal, sin depender del snapshot de demostración? [Cobertura, Spec §FR-04, Plan §Fronteras]
- [x] CHK007 ¿Distingue el diseño DTO → Model dentro del Service de la composición del nombre del jugador en la notificación de presentación? [Consistencia, Plan §Fronteras, Contracts §Compra, Tasks T-06/T-09/T-12]

## Estado local y dependencias

- [x] CHK008 ¿Está acotado qué estado del mercado se preserva al abrir/cerrar el modal sin imponer filtros nuevos ni remontar el listado? [Claridad, Spec §FR-03, Tasks T-16]
- [x] CHK009 ¿Expresa el grafo T-00–T-19 un orden sin ciclos para los prerrequisitos de UI genérica, cotizaciones, formulario, composición en app y validación posterior? [Cobertura, Tasks §Dependencias]

## Notes

- Sin casillas aprobadas en nombre del revisor; no modifica `checklists/requirements.md` ni acredita un gate.

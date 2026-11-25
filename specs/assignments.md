# Registro de asignaciones explícitas de SPEC

Registro append-only de las asignaciones explícitas de identificador de SPEC.

Existe para satisfacer la trazabilidad exigida por la Constitución §8 y por
`docs/development/sdd-workflow.md` §5: la numeración automática es la predeterminada y toda
numeración explícita requiere una distribución previa documentada de funcionalidades entre
integrantes que trabajan en paralelo, junto con un **registro trazable del número, la
funcionalidad, el responsable y el motivo**, registrado antes de crear la SPEC.

Una fila se agrega al materializar una SPEC con identificador asignado explícitamente, antes de
cualquier escritura sobre el issue de origen. El registro no sustituye los artefactos de la SPEC,
sus gates SDD ni el estado de seguimiento en GitHub Issues y GitHub Projects.

## Convenciones

- **Identificador**: `SPEC-0XX`, con el mismo número que el de la carpeta de la spec.
- **Funcionalidad**: nombre de la funcionalidad asignada, tal como figura en el issue de origen.
- **Responsable**: responsable de la asignación, tomado de los asignados del issue de origen.
- **Issue de origen**: issue de GitHub que motiva la asignación.
- **Requisito**: clave `REQ-` rastreada en el issue de origen, cuando la correspondencia esté
  confirmada.
- **Rama**: rama asociada, con el formato `<type>/<spec-id>-<scope>`.
- **Fecha**: fecha de la asignación.

Las filas no se editan ni se eliminan. Una asignación que cambia de responsable o de funcionalidad
se corrige con una fila nueva que la referencie.

## Asignaciones

| Identificador | Funcionalidad | Responsable | Issue de origen | Requisito | Rama | Fecha |
| --- | --- | --- | --- | --- | --- | --- |
| SPEC-014 | Consultar portfolio personal | YoelSLA | #20 | REQ-S2-09 | feat/014-consultar-portfolio-personal | 2026-10-19 |
| SPEC-015 | Definir reglas contables de costo promedio y resultados financieros | YoelSLA | https://github.com/YoelSLA/football-market/issues/63 | REQ-S2-20 | feat/015-define-average-cost-and-financial-results-rules | 2026-10-19 |
| SPEC-016 | Calcular y persistir la cotización de tokens de jugadores con historial trazable (corrige la fila anterior del 2026-10-20, que quedaba limitada al disparo manual del recálculo) | Vaqui26 | https://github.com/YoelSLA/football-market/issues/39 | REQ-S2-11-A, REQ-S2-03, REQ-S2-04, REQ-S2-05, REQ-S2-08 | feat/016-ejecutar-recalculo-manual-de-cotizaciones | 2026-10-20 |
| SPEC-017 | Visualizar portfolio personal | YoelSLA | #26 | REQ-S2-15 | feat/017-view-personal-portfolio-ui | 2026-10-21 |
| SPEC-018 | Consultar rankings de jugadores por precio y por score | Vaqui26 | https://github.com/YoelSLA/football-market/issues/19 | REQ-S2-08 | feat/018-consultar-rankings-de-jugadores | 2026-10-23 |
| SPEC-019 | Comprar tokens desde el frontend | YoelSLA | #42 | REQ-S2-14-A | feat/019-buy-tokens-from-frontend | 2026-10-23 |
| SPEC-020 | Consultar historial de operaciones | Vaqui26 | https://github.com/YoelSLA/football-market/issues/21 | REQ-S2-10 | feat/020-view-operation-history | 2026-10-24 |
| SPEC-009 | Rediseñar detalle individual y estadísticas reales del jugador (dashboard WhoScored) | YoelSLA | #72 | REQ-S2-12 | feat/009-player-detail-whoscored-stats-dashboard | 2026-10-25 |
| SPEC-022 | Visualizar ranking de jugadores | YoelSLA | #25 | REQ-S2-13 | feat/022-view-player-rankings | 2026-10-27 |
| SPEC-023 | Programar recálculo automático de cotizaciones | Vaqui26 | https://github.com/YoelSLA/football-market/issues/40 | REQ-S2-11-B | feat/023-scheduler-quote-recalculation | 2026-11-03 |
| SPEC-025 | Visualizar historial de transacciones | YoelSLA | https://github.com/YoelSLA/football-market/issues/27 | REQ-S2-16 | feat/025-view-transaction-history | 2026-11-10 |
| SPEC-010 | Catálogo del Mercado Oficial con cotizaciones integradas | YoelSLA | https://github.com/YoelSLA/football-market/issues/90 | REQ-S2-06 | feat/010-buy-tokens | 2026-11-12 |
| SPEC-026 | Venta inmediata al superusuario (buyback) y precio flexible en P2P | Vaqui26 | https://github.com/YoelSLA/football-market/issues/94 | REQ-S2-FEAT-ADD | feat/026-market-instant-buyback-and-p2p-pricing | 2026-11-18 |
| SPEC-027 | UI de venta inmediata oficial y selector de precio flexible en P2P | YoelSLA | https://github.com/YoelSLA/football-market/issues/95 | REQ-S2-FEAT-ADD | feat/027-frontend-instant-buyback-and-p2p-pricing | 2026-11-18 |
| SPEC-028 | Métricas de tendencias de mercado y factor de demanda en cotizaciones | Vaqui26 | https://github.com/YoelSLA/football-market/issues/96 | REQ-S2-FEAT-ADD | feat/028-market-trends-and-demand-pricing | 2026-11-18 |
| SPEC-029 | Tablero de tendencias de mercado y visualización de demanda comunitaria | YoelSLA | https://github.com/YoelSLA/football-market/issues/97 | REQ-S2-FEAT-ADD | feat/029-frontend-market-trends-and-sentiment | 2026-11-18 |
| SPEC-030 | Visualizar y gestionar órdenes de venta personales (Mis órdenes) | YoelSLA | https://github.com/YoelSLA/football-market/issues/105 | REQ-S2-FEAT-ADD | feat/030-my-orders | 2026-11-19 |
| SPEC-031 | Dashboard interactivo y resumen ejecutivo de mercado (Bloomberg & Sorare) | YoelSLA | https://github.com/YoelSLA/football-market/issues/109 | REQ-S2-FEAT-ADD | feat/031-home-dashboard | 2026-11-20 |
| SPEC-032 | Configurar métricas con Spring Boot Prometheus y monitoreo con Actuator | Vaqui26 | https://github.com/YoelSLA/football-market/issues/100 | REQ-S3-03 | feat/032-prometheus-actuator-monitoring | 2026-11-24 |
| SPEC-033 | Desplegar Prometheus y Grafana para monitoreo del backend | Vaqui26 | https://github.com/YoelSLA/football-market/issues/117 | REQ-S3-06 | feat/033-deploy-prometheus-grafana-monitoring | 2026-11-24 |

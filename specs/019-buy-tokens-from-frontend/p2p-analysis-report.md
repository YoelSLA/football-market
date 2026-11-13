# Informe de análisis estático — ampliación P2P de SPEC-019

**Fecha:** 2026-11-04
**Alcance:** spec, plan, contrato consumidor, tareas y checklist P2P.
**Ejecución:** revisión documental; no se ejecutaron tests, builds, generadores ni hooks.

## Resultado

La ampliación es técnicamente viable con el backend vigente y mantiene la coherencia del contrato de
SPEC-011. No requiere modificar endpoints ni persistencia. La relación funcional justifica reutilizar
SPEC-019: ambas modalidades comparten ruta, Page, navegación, respuesta de compra e invalidaciones.

## Consistencia

| Área | Resultado |
|---|---|
| Requisito principal | Cubierto por US6, FR-27 y SC-07: el detalle presenta todas las publicaciones comprables del jugador. |
| Backend | `GET /api/listings` entrega colección plana completa; `POST /api/orders/buy` acepta `listingId`. |
| Datos de jugador | Cubiertos mediante API pública/caché de players; no se atribuyen al DTO de listings. |
| Vendedor | No se muestra; no existe dependencia de una fuente no disponible. |
| Arquitectura frontend | Compatible con Page Hook, Query/Mutation Hooks, Service, Mapper y Models vigentes. |
| Navegación | Query parameters preservan ruta global, recarga y Back/Forward. |
| Estados concurrentes | Cubiertos por códigos backend, refetch y ausencia de retry automático del POST. |
| Verificación | Separada en Fase P1, gate humano, Fase P2 y validación humana; sin tests frontend nuevos. |

## Hallazgos

### A1 — Contradicciones históricas resueltas

FR-15 y el último alcance visual excluían lógica P2P. La ampliación declara sustitución acotada y conserva
la compra oficial y la evidencia previa. No debe eliminarse el registro histórico durante implementación.

### A2 — Escalabilidad aceptada, no bloqueante

El contrato actual no pagina ni agrega. La estrategia aprobada descarga todas las publicaciones y pagina
jugadores localmente. Es suficiente para el alcance; una optimización backend futura no es condición de
implementación.

### A3 — Precisión del saldo, condicionado

El contrato observado de usuario puede no conservar fracciones de crédito. Por eso la spec no exige un
saldo posterior exacto sin una fuente suficientemente precisa. La compra y el error de saldo permanecen
autoritativos en backend. No bloquea el flujo P2P.

### A4 — Worktree con cambios previos, riesgo operativo

Existen cambios locales ajenos y superpuestos en market, router, estilos y autenticación. Antes de P-01 se
debe releer el diff y editar de forma incremental, sin revertir, incorporar ni atribuir esos cambios a esta
ampliación. Es un riesgo de integración, no una contradicción de requisitos.

## Gates y siguiente transición

- Spec Status permanece **Draft**; este informe no la promueve ni acredita seguimiento remoto.
- No hay blocker documental para preparar una revisión humana de la ampliación.
- La implementación solo puede comenzar con autorización explícita referida al alcance consolidado
  FR-20–FR-34 / P-01–P-08.
- Tras P-08 el agente debe detenerse en P-09 antes de cualquier maquetación fina.

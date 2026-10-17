# Consistencia — detección de discrepancias

## Principio

El estado de Trello debe **derivar** del estado real del proyecto. Al inspeccionar el tablero, contrastar contra lo decidido por `sdd-orchestration` y aplicar **solo** la corrección que resulte de esa decisión.

Si la corrección requiere una decisión que no puede derivarse del orquestador, **detenerse y reportar**. No resolver en silencio.

## Discrepancias a detectar

| Situación en Trello | Lectura |
| --- | --- |
| US en `DONE` sin evidencia de TESTING aprobada | `DONE` no acreditado; consultar y reflejar la transición indicada |
| US en `TESTING` sin CODE REVIEW aprobado | Transición saltada; corregir según la decisión vigente |
| US en `CODE REVIEW` con implementación incompleta | Estado prematuro; consultar |
| US en `IN PROGRESS` sin `IMPLEMENT AUTHORIZATION` aprobada | Estado sin base; consultar antes de corregir |
| US en `IN PROGRESS` con autorización `INVALIDATED` y sin nueva autorización | Transición sin base; consultar |
| Feature en `DONE` con alguna US no `DONE` | Cierre prematuro; consultar |
| Feature en `DONE` sin evidencia de CONVERGE final exitoso ni autorización final | Cierre sin base; consultar |
| Feature en `READY` con autorización de cierre ya registrada | Estado atrasado; consultar |
| Trello en `READY` con `READY` invalidado | Reflejar el retroceso indicado |
| US o SPEC en Trello sin equivalente en el repositorio | Tarjeta sin origen; no tratarla como requisito |
| Tarjeta en `BACKLOG` con spec y artefactos vigentes | Estado atrasado; consultar |
| Claves duplicadas o dos tarjetas para la misma entidad | Corregir reutilizando por clave, sin borrar historia |

## Comprobaciones por ciclo

- Cada clave es única y apunta a una spec real.
- Toda US afectada tiene su tarjeta.
- Cada cambio queda reflejado en el índice bidireccional.
- La columna refleja la decisión vigente.
- Las transiciones no cruzan columnas inexistentes.

## Qué no hacer

- No corregir una discrepancia suponiendo el estado del proyecto: preguntarlo.
- No borrar evidencia histórica para que la tarjeta parezca consistente.
- No reinterpretar un comentario `[SDD GATE]` para crear, aprobar o invalidar un gate.
- No forzar estados iguales entre Feature y US, ni deducir la Feature de una sola US.
- No ocultar ni archivar hallazgos, ni cambiar alcance o entrega para tapar discrepancias.
- No introducir estados de bloqueo nuevos.
- No resetear el tablero ante un cambio upstream: validar por impacto.

## Tratamiento de heredados

SPEC-001–005 conservan `DONE` histórico bajo el workflow anterior, sin gates retroactivos ni evidencia fabricada. No exigirles evidencia que no existe y no invalidarlos por defectos de forma.

Una reapertura material aplica el workflow vigente **solo al alcance afectado**. Conservar y explicar los estados heredados; los hallazgos técnicos siguen abiertos hasta su resolución comprobada.

`DONE` no sustituye el campo `Status` de la spec ni acredita tests.
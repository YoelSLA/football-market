# Checklist de calidad de la ampliación: Nombre de usuario y roles

**Propósito**: Validar la calidad de los requisitos añadidos de la ampliación antes de dar por cerrada
la fase de documentación.

**Creado**: 2026-10-14

**Alcance**: `FR-013` a `FR-031`, `SC-007` a `SC-012`, historias de usuario 3 y 4 de
[spec.md](../spec.md).

**Relación con el checklist original**: Este checklist es independiente.
[requirements.md](requirements.md) no se modifica: sigue validando la especificación original tal
como fue aprobada.

## Calidad del contenido

- [x] Los requisitos describen comportamiento verificable, no mecanismos de implementación.
- [x] Los requisitos están escritos para stakeholders no técnicos.
- [x] No quedan marcadores `[NEEDS CLARIFICATION]`.
- [x] Los términos de negocio están definidos: nombre de usuario, `USER`, `SUPERUSER`.
- [x] `SUPERUSER` está definido como dueño inicial de los tokens y no como administración general del sistema.

## Integridad de los requisitos

- [x] Cada requisito de la ampliación es comprobable y no admite interpretaciones incompatibles.
- [x] Las reglas de validación del nombre de usuario están completas: obligatoriedad, longitud, formato y unicidad.
- [x] El tratamiento de mayúsculas y minúsculas está especificado.
- [x] La unicidad del nombre de usuario está especificada con su respuesta HTTP y su código de error.
- [x] La unicidad del `SUPERUSER` está especificada a nivel de aplicación y de base de datos.
- [x] La creación del `SUPERUSER` está especificada como interna, configurable, idempotente y sin credenciales en el repositorio.
- [x] El comportamiento ante configuración incompleta está especificado.
- [x] El inicio de sesión y el JWT tienen requisitos de compatibilidad explícitos.

## Trazabilidad

- [x] Cada historia de usuario nueva tiene criterios de aceptación observables.
- [x] Los casos límite están identificados: duplicado exacto, duplicado por mayúsculas y minúsculas, nombre de usuario vacío, formato inválido, y el uso de nombre de usuario como credencial.
- [x] Los criterios de éxito nuevos son medibles.
- [x] Las dependencias y supuestos están declarados.
- [x] El alcance ampliado está delimitado y no invade el mercado de tokens.

## Historial

- [x] Los requisitos originales `FR-001` a `FR-012` se conservan sin reescritura.
- [x] Los criterios de éxito originales `SC-001` a `SC-006` se conservan.
- [x] Las historias de usuario originales se conservan.
- [x] La ampliación se registra en una sección de Amendments fechada.
- [x] El estado original de las tareas no se modifica y las pendientes `T010` y `T025` siguen abiertas.

## Preparación para implementación

- [x] Los contratos HTTP afectados están documentados.
- [x] El modelo de datos actualizado está documentado.
- [x] Las alternativas de diseño y las decisiones descartadas están registradas con su justificación.
- [x] Las categorías de testing afectadas están identificadas.
- [x] Los cambios de contrato incompatibles están declarados de forma explícita.

## Notas

- Como en el checklist original, la spec incorpora detalles de contrato aprobados durante la
  sincronización: códigos HTTP, códigos de error estables, estructura de `ErrorResponseDTO` y
  reglas de normalización. Por eso no satisface el control que exige ausencia total de detalles
  técnicos.
- Los criterios de éxito expresados como porcentajes absolutos no pueden acreditarse únicamente con
  los tests automatizados; requieren verificación del usuario.
- `SC-012` exige evidencia de rechazo del inicio de sesión con nombre de usuario, y `SC-011`
  requiere una comprobación explícita del acceso de los usuarios migrados. Ninguna de las dos está
  ejecutada por el agente.
# Monitoring Requirements Checklist: Métricas Prometheus y monitoreo seguro

**Purpose**: Revisar la calidad, claridad y cobertura de los requisitos de métricas y seguridad de monitoreo.
**Created**: 2026-11-24
**Feature**: [spec.md](../spec.md)

**Note**: Este checklist personalizado se genera mediante `/speckit.checklist` a partir del contexto de la feature.
**Review Ownership**: Este artefacto de calidad de requisitos pertenece al revisor. Marcar un ítem `[x]` solo cuando el revisor determine que el criterio de calidad está satisfecho.
**Marker Semantics**: `[x]` significa que se revisó y satisfizo el criterio de calidad de requisitos; no significa que la implementación esté completa.

## Completitud de requisitos y cobertura de escenarios

- [x] CHK001 ¿Los requisitos definen qué categorías de métricas deben poder observarse y con qué alcance, sin dejar “estado, recursos y rendimiento” indeterminados? [Clarity, Spec §FR-002, §SC-001; TP §5.1]
- [x] CHK002 ¿Los escenarios reflejan que cualquier cliente que alcanza el backend puede consultar `/actuator/prometheus` sin autenticación, JWT, roles ni permisos? [Coverage, Spec §US1–US2, §FR-003–FR-004]
- [x] CHK003 ¿Está definido el comportamiento requerido ante la ausencia temporal de una métrica sin ampliar la superficie expuesta ni revelar información sensible? [Edge Case, Spec §Edge Cases]
- [x] CHK004 ¿El alcance distingue con claridad los endpoints permitidos, sus subendpoints y el resto de Actuator, incluidos el acceso público y el protegido? [Completeness, Spec §FR-006–FR-009]

## Claridad y calidad del contrato HTTP

- [x] CHK005 ¿El contrato especifica inequívocamente el formato de respuesta de Prometheus y el contenido mínimo necesario para considerar útiles las métricas? [Clarity, Spec §FR-001–FR-002; Contract §GET /actuator/prometheus; TP §5.1]
- [x] CHK006 ¿El contrato especifica que `/actuator/prometheus` responde HTTP 200 en formato compatible con Prometheus sin exigir encabezado de autorización, JWT, roles ni permisos? [Clarity, Spec §FR-003–FR-004; Contract §GET /actuator/prometheus]
- [x] CHK007 ¿Los criterios de éxito permiten determinar objetivamente qué significa “estado mínimo” en `/actuator/health` y qué información queda excluida? [Measurability, Spec §FR-007, §SC-004; Contract §GET /actuator/health]
- [x] CHK008 ¿Los requisitos delimitan de forma verificable qué información puede contener `/actuator/info` y qué se considera sensible? [Clarity, Spec §FR-005, §FR-009; Contract §GET /actuator/info]

## Consistencia y seguridad de los requisitos

- [x] CHK009 ¿La regla de acceso público a Prometheus coincide entre las clarificaciones, los requisitos funcionales, los escenarios y el contrato HTTP? [Consistency, Spec §Clarifications, §FR-003–FR-004; Contract §GET /actuator/prometheus]
- [x] CHK010 ¿Las excepciones públicas de health e info están limitadas de manera coherente, sin contradecir la prohibición de exponer los demás endpoints o subendpoints? [Consistency, Spec §FR-006–FR-009; Contract §GET /actuator/health, §GET /actuator/info]
- [x] CHK011 ¿La prohibición de exponer secretos, credenciales, tokens y configuración sensible se aplica explícitamente a todas las respuestas HTTP de monitoreo? [Coverage, Spec §FR-005, §SC-003]
- [x] CHK012 ¿Las exclusiones de servidor Prometheus y dashboards de Grafana se mantienen consistentes con el valor de la feature y con sus límites de alcance? [Scope, Spec §FR-008, §Assumptions]

## Notes

- Los ítems sin marcar identifican criterios que requieren aclaración o mayor precisión en los requisitos.
- La generación del checklist no equivale a su evaluación ni a su aceptación.
- TP §5.1 especifica métricas clave como latencia y tasa de error; el revisor aclara que los datos sensibles incluyen datos de usuario, como contraseñas.
- El alcance vigente concreta las métricas estándar CPU/JVM/HTTP; el estado se consulta en `/actuator/health` y la tasa de errores se deriva de los resultados HTTP.
- `/speckit.implement` lee el estado de los checklists, pero no modifica sus marcadores.
- `checklists/requirements.md` tiene un ciclo de vida separado, mantenido por `/speckit.specify` y `/speckit.clarify`.
- Los ítems revisan la calidad de lo escrito en los requisitos y contratos; no verifican la implementación.

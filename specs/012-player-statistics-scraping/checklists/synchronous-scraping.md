# Calidad de requisitos: scraping síncrono sin lotes

**Purpose**: Revisión general y especializada de API, datos, fallos y seguridad del cambio del 2026-10-27; profundidad estándar antes de Tasks/Analyze.
**Created**: 2026-10-27
**Feature**: [../spec.md](../spec.md)

**Review Ownership**: checklist del revisor. `[x]` significa aceptación del criterio de calidad de requisitos, no implementación ni pruebas ejecutadas. Generado siguiendo `/speckit.checklist`; los 16 marcadores se completaron a solicitud explícita del usuario tras la evaluación documental. Esto no acredita implementación, pruebas ejecutadas ni autorización IMPLEMENT.

## Completitud y claridad general

- [x] CHK001 ¿Están definidos snapshot, orden y una tentativa por pendiente, sin máximo ni deadline total? [Completitud, Spec FR-011–FR-013]
- [x] CHK002 ¿Se distingue recorrer todos los pendientes de obtener cobertura completa, incluidos fallos y no-data? [Claridad, Spec FR-014, SC-001, SC-011]
- [x] CHK003 ¿Se distinguen decisiones vigentes e históricas sobre lotes, retries y cliente? [Consistencia, Spec §Clarifications, Research §Historial]
- [x] CHK004 ¿Está limitado el trabajo adicional a los tres síntomas observados, sin causas ni formatos numéricos supuestos? [Alcance, Spec §Session 2026-10-27, Plan §Investigación]

## API y consumidores

- [x] CHK005 ¿Están definidos el body con fecha opcional y el rechazo explícito de `maxPlayers` retirado, incluso null? [Compatibilidad, Spec FR-012, Contrato HTTP §Request scraping]
- [x] CHK006 ¿Está definido un solo POST del cliente, sin modos, máximo ni repetición automática tras progreso o error? [Consistencia, Contrato HTTP §Cliente operativo]
- [x] CHK007 ¿Están declarados unidad de conteos, casos sin pendientes y diferencia entre `status` y `coverageComplete`? [Medibilidad, Spec FR-050–FR-051, Contrato HTTP §Response]
- [x] CHK008 ¿Está documentado el riesgo de conexión prolongada y resultado incierto sin convertirlo en ejecución asíncrona? [Dependencia, Plan §Performance Goals, Contrato HTTP §Cliente operativo]

## Datos y recuperación

- [x] CHK009 ¿Están definidos no-data con contexto resuelto, valores presentes frente a ausencias y reconfirmación sin sobrescribir éxitos? [Integridad, Spec FR-013, FR-063, SC-003]
- [x] CHK010 ¿Se conserva fecha inicial al cruzar medianoche y se definen nuevos elegibles posteriores al snapshot? [Cobertura, Spec FR-013, Modelo §Invariantes]
- [x] CHK011 ¿La recuperación distingue ejecución viva de huérfana sin deducirla de antigüedad del POST? [Consistencia, Plan §Continuidad y recuperación, Modelo §Transiciones]
- [x] CHK012 ¿Se mantienen auditoría, confirmación atómica y ausencia de transacciones durante acceso externo? [Integridad, Spec FR-032–FR-041, Modelo §Garantías de esquema]

## Fallos, rendimiento y seguridad

- [x] CHK013 ¿Está distinguida la ausencia de deadline total de los timeouts y pacing externos preservados? [Claridad, Spec FR-012, FR-018, SC-012]
- [x] CHK014 ¿Están definidos continuidad ante fallos individuales, cierre global y limpieza sin perder confirmaciones? [Excepciones, Spec FR-038–FR-042, Plan §Ritmo y reintentos]
- [x] CHK015 ¿Se mantienen JWT/autorización de catálogo, secretos fuera de diagnósticos y separación de canales? [Seguridad, Spec FR-049, FR-052, Contratos §Reglas comunes]
- [x] CHK016 ¿Se conservan precisión decimal, ausencia distinta de cero y catálogo 45/57/42, sin cambiar JSON o identidad? [Consistencia, Spec FR-001, FR-022–FR-031, FR-057, FR-060]

## Evaluación asistida previa a Tasks — 2026-10-27

Encargo del usuario: completar la preparación pendiente hasta Analyze, sin código; incluye asistencia de evaluación de calidad, no aprobación de gates humanos ni marcado en nombre del revisor.

| Ítems | Evaluación documental | Evidencia |
| --- | --- | --- |
| CHK001–CHK004 | Cubiertos | FR-011–FR-014, SC-001/SC-011, bitácora histórica delimitada y tres síntomas en Plan/Research. |
| CHK005–CHK008 | Cubiertos | HTTP define retiro incompatible, conteos y conexión larga; Plan incluye todos los consumidores y un POST del CLI. |
| CHK009–CHK012 | Cubiertos | FR-063 y Modelo explicitan reconfirmación nula, snapshot/fecha, exclusión por canal de instancia única y transacciones breves. |
| CHK013–CHK016 | Cubiertos | Timeouts/pacing preservados, sin SLA nuevo, fallo individual distinto de global, cierre por hilo, seguridad y JSON/identidad sin cambios. |

Resultado de asistencia: 16/16 criterios con soporte documental; 0 carencias materiales detectadas en este checklist. Marcadores completados por solicitud explícita del usuario tras la evaluación documental; esto no acredita implementación, ejecución de tests ni autorización IMPLEMENT. Las marcas históricas de `requirements-quality.md` se preservan y no validan automáticamente este cambio.

Selección: este checklist combina calidad general con API, datos, resiliencia/rendimiento y seguridad, todas afectadas por el cambio. UX/accesibilidad no aplican: no cambia frontend ni interacción visual. No se crea infraestructura de pruebas ni nuevas exigencias de rendimiento.

## Notes

Solo el revisor decide los marcadores; `/speckit.implement` los lee, no los cambia. La investigación causal pendiente es trabajo técnico, no evidencia de corrección ni una causa ya establecida. Las reglas de autorización de la Constitución siguen vigentes.

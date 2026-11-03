# Estados de las especificaciones

El campo `Status` de cada `spec.md` indica el avance de esa feature. Se actualiza cuando se cumple el criterio del estado siguiente; no se deduce de que existan archivos de plan, tareas o código.

| Estado | Criterio |
| --- | --- |
| `Draft` | La especificación está en elaboración o tiene decisiones funcionales relevantes pendientes. |
| `Ready for planning` | El alcance, los requisitos y los criterios de aceptación están suficientemente claros para diseñar la solución. Todavía no implica que exista un plan. |
| `Ready for implementation` | Preparación global validada y ANALYZE vigente sin blockers de especificación, planificación o consistencia. Dependencias técnicas identificadas no son blockers por sí mismas. No autoriza IMPLEMENT: requiere autorización humana separada. |
| `Approved` | Todas las US requeridas DONE, trabajo necesario completo, CONVERGE final exitoso y FEATURE COMPLETION AUTHORIZATION humana explícita, vigente y persistida. No se deriva automáticamente de código, tareas, build ni CONVERGE. |

Una aclaración o cambio de alcance puede devolver una spec a un estado anterior si invalida decisiones de planificación o aceptación. El estado describe la **feature**, no sustituye los resultados de verificación ni el seguimiento detallado de `tasks.md`.

La invalidación se determina por impacto sobre el alcance evaluado, sin resetear entidades no afectadas. SPEC-001–005 conservan cierres históricos; no se exigen gates retroactivos. Una reapertura material sigue el workflow vigente.

# Estados de las especificaciones

El campo `Status` de cada `spec.md` indica el avance de esa feature. Se actualiza cuando se cumple el criterio del estado siguiente; no se deduce de que existan archivos de plan, tareas o código.

| Estado | Criterio |
| --- | --- |
| `Draft` | La especificación está en elaboración o tiene decisiones funcionales relevantes pendientes. |
| `Ready for planning` | El alcance, los requisitos y los criterios de aceptación están suficientemente claros para diseñar la solución. Todavía no implica que exista un plan. |
| `Ready for implementation` | La especificación y el plan están alineados, las decisiones necesarias para implementar están resueltas y se puede empezar a ejecutar las tareas. |
| `Approved` | La feature implementada fue revisada y aceptada explícitamente por el usuario. No equivale solo a haber creado código, completado tareas o ejecutado un build. |

Una aclaración o cambio de alcance puede devolver una spec a un estado anterior si invalida decisiones de planificación o aceptación. El estado describe la **feature**, no sustituye los resultados de verificación ni el seguimiento detallado de `tasks.md`.

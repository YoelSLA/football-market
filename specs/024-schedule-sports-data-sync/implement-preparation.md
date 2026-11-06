# Preparación de autorización de implementación — SPEC-024

## Resultado y referencia local

El ANALYZE del 2026-11-05 reportó ausencia de hallazgos y blockers, con 13 requisitos y criterios asociados a tareas y 12 tareas. Después del análisis se agregó únicamente la sección «Evidencia de ANALYZE» a `spec.md`; no cambió su contenido funcional. La siguiente instantánea identifica el estado local posterior a esa anotación, no pretende ser un hash calculado durante ANALYZE ni acredita por sí sola su vigencia ante cambios posteriores.

Hashes SHA-256 del contenido completo de cada archivo, calculados sobre los bytes locales:

```text
5c0f30c8c1ad11bafbd65f90cc36fd1fa9287d633b5950c0795cad5450392ebc  checklists/contracts-security.md
9a706316005c10bd378a1ba2374201dc99e15a51f2ed02d9a4ed29ae6e57858d  checklists/general.md
214e6f6998279b86bce08d585b9d4ffb2ff7a787a21a1db115a84ab2176c8b24  checklists/requirements.md
a574f33f734b614afc3a1bb9e966f5af6943286d7a4db8811f0bc9a986cf608a  contracts/scheduled-execution.md
d7e81fef2b779d02ff2fe583f6edec22876782e98a5fdb356b76b6a11f5be502  data-model.md
98e7c13c144e9efd3a6c2327706e1babf1af36e109a3d4612f9159281f0fec19  plan.md
8be8595e52c8000a2341745fd5bf64a921d0a4a506f33719ae5304ddbe6e873f  quickstart.md
1b1fe29ce93d9abcba067bff1b7064d52ba3b354ff6096cb88b17dad81cb3626  research.md
62e498c1793619e2eab075bff33c6fa114a8f85c3a28d55d60c1e65f189f5e54  spec.md
42a8cf28ea3b2dfdcfa113d3bc101e92de8d99f20580acd3695b049e35d66cb9  tasks.md
```

Las rutas son relativas a esta carpeta. Antes de implementar, comparar los hashes, comprobar si existen cambios adicionales relevantes en fuentes normativas o dependencias y evaluar su impacto; una diferencia no invalida automáticamente lo no afectado. Este archivo de preparación no forma parte de la instantánea. Los artefactos sin seguimiento deben conservarse y estar disponibles para la sesión posterior.

## Alcance propuesto: fases 1 y 2

- Fase 1: reutilizar el proyecto y las capacidades existentes; no hay tareas de inicialización.
- Fase 2: T001–T004, fundamentos compartidos de sincronización: entradas automáticas y señal interna de fallo global, adaptación de los recorridos existentes y tests backend de regresión, preservando contratos y comportamiento manuales.
- Dependencias: T001 y T002 preceden T003; T003 precede T004. Estos fundamentos habilitan el posterior scheduler, pero no lo implementan.
- Fuera de este alcance: T005–T012, disparador semanal, coordinación con evaluación, documentación posterior y demás fases. No se ejecutan tests; corresponden al usuario. La indisponibilidad del proveedor puede quedar oculta tras un resultado parcial o completo y debe propagarse conforme al plan sin alterar el contrato manual.

## Autorización

**IMPLEMENT AUTHORIZATION recibida del usuario el 2026-11-05.** Solicitud original: «autorizo a implementar phase 1 y 2», ratificada tras esta preparación mediante «Listo, podrias ejecutar la implementacion». Alcance: fases 1 y 2, T001–T004, con los límites y dependencias descritos arriba. No autoriza T005–T012, fases posteriores, tests ejecutados por el agente ni cierre.

Antes de iniciar se compararon los diez hashes de esta referencia: todos coinciden con los bytes locales actuales. La evidencia de ANALYZE en `spec.md` y la explicación de la anotación posterior se conservan; no se identifican cambios materiales adicionales en el diff ni contradicciones con las normas consultadas que invaliden el alcance. Las tres checklists están completas. GitHub Projects se observó en `In Progress`; no se requiere ni se realiza escritura remota. CODE REVIEW, TESTING y cierre permanecen pendientes.

### Ampliación de autorización recibida — 2026-11-05

El usuario declaró: «Perfecto. Tenes mi autorizacion para implementar US1 y US2 ahora». Se registra autorización explícita para el alcance de ambas historias de SPEC-024, T005–T011, conservando la autorización anterior de los fundamentos T001–T004. No aprueba CODE REVIEW, TESTING ni cierre y no acredita resolución de bloqueantes.

La comprobación previa encuentra T003–T004 aún pendientes y los dos hallazgos registrados en `implementation-report.md`, sección «Bloqueantes y siguiente decisión». US1 depende de los fundamentos y US2 de estos y del disparador de US1, conforme a `tasks.md`. No se inicia código de esas fases mientras falte resolver los hallazgos y acreditar la preparación vigente. La autorización recibida se conserva; la corrección upstream del desglose y el ANALYZE requerido no se ejecutan por esta solicitud limitada a IMPLEMENT. No se actualiza el seguimiento remoto ni se infiere su estado actual de la lectura histórica.

### Reanudación tras la corrección upstream — 2026-11-05

El usuario solicita: «Ya podes contiunar, corregi los errores que te bloqueaban en otro chat». Se contrasta con `plan.md` §Diseño 7–8, T003–T004 actualizadas y la actualización posterior de preparación de `implementation-report.md`: cierre JSON automático ante errores inesperados, preservación de interrupción/error principal/fallo de cierre y limitación explícita del caso no reproducible. Allí consta ANALYZE posterior sin bloqueantes de preparación. Esta solicitud contextual confirma continuar el alcance previamente autorizado con esos fundamentos corregidos; no se pide repetir la autorización de US1/US2.

Se reanuda IMPLEMENT para T003–T011, conservando T001–T002 y los cambios previos. La coherencia arquitectónica exigida por el mismo cambio se documentará sin ejecutar otra fase SDD; no se ejecutan tests, CONVERGE, handoffs, commits, push ni cierre. El estado remoto se verificará por lectura; esta anotación no lo actualiza ni aprueba gates humanos.

### Corrección P2 autorizada — 2026-11-06

El usuario reportó el doble fallo scraping/cierre oculto en logs y respondió «Autorizo a corregir» a la propuesta explícita de corregir la entrada automática, conservar el contrato manual y añadir regresiones de fachada y logs, sin ejecutar tests ni fases posteriores. Alcance: corrección de T003/T004/T009/T011 para comunicar `closeFailed=true` cuando ambos fallos ocurren; no cambia requisitos, plan ni arquitectura. Se conserva el resto de la implementación y no se infiere aprobación de CODE REVIEW, TESTING o cierre. Los informes se actualizan para el nuevo diff.

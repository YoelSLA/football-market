# Autorizaciones humanas — implementación, revisión, testing y cierre

## Autoridad y evidencia

Las autorizaciones humanas son **explícitas, específicas para el alcance evaluado y vinculadas a lo revisado**.

No son autorizaciones válidas:

- `ok`, `dale`, `seguí` u otra expresión ambigua;
- una tarjeta o registro en un estado concreto;
- un build exitoso, un commit, un PR o `tasks.md` completo;
- un resumen consolidado que no identifique el alcance aprobado.

Una respuesta puede aprobar **varios alcances en un solo mensaje**, siempre que identifique cuáles. La evidencia debe permitir saber quién autorizó, qué autorizó y con qué evidencia se compara.

## Registro de la autorización

La autorización MUST ser explícita, verificable y persistente en un soporte del proyecto. **No se prescribe formato canónico ni sistema específico**: el agente elige el soporte disponible y explícito —hilo del registro de seguimiento, Pull Request, sección del reporte correspondiente o el propio artefacto—.

- La evidencia ya persistida bajo el régimen anterior **puede reutilizarse** cuando sea suficiente para acreditar una autorización vigente. No se exige reemitirla ni duplicarla.
- Las invalidaciones se registran como eventos nuevos, con motivo y alcance, sin borrar evidencia histórica y **sin atribuirse a una aprobación humana**.
- Si la evidencia obligatoria no puede persistirse o verificarse, **no declarar la transición completada**.
- Un sistema de seguimiento inaccesible no habilita la transición dependiente, ni autoriza a afirmar que se sincronizó, ni habilita descartar cambios locales.

Las autorizaciones se consolidan **por alcance explícito**. Las User Stories se revisan y verifican en conjunto, con cobertura trazable desde `spec.md` hacia `tasks.md`; **no** son unidades de autorización ni requieren autorización propia.

## Preparación completa — no es un gate

No existe un gate READY. Antes de solicitar autorización de implementación, verificar:

- Feature identificada sin ambigüedad y Constitución consultada.
- `spec.md`, `plan.md`, `tasks.md` y checklists aplicables existen, están evaluados y son coherentes.
- Ambigüedades funcionales relevantes resueltas; criterios de aceptación cubiertos por tareas.
- ANALYZE completado, vigente y sin blockers; hallazgos no bloqueantes comunicados.
- Sin requisitos nuevos introducidos sin decisión humana.

La autorización de implementación se presenta junto con esta comprobación, en una sola solicitud. No se pide una confirmación humana adicional de "READY".

Si no se cumplen, detener o regresar a la fase afectada conforme a [`recovery.md`](recovery.md).

## Autorización IMPLEMENT

Presentar en una sola pantalla:

- Feature e identificador.
- Alcance propuesto a implementar y sus límites.
- Dependencias e independencia entre las partes del alcance.
- Prerrequisitos compartidos y a qué habilitan.
- Resumen de plan, tareas relevantes y resultado de ANALYZE.
- Riesgos conocidos.

El usuario autoriza **el alcance**. El agente decide el orden técnico.

Ejemplo suficiente:

> Autorizo IMPLEMENT del alcance de SPEC-014 descrito arriba, con los prerrequisitos compartidos identificados. No autorizo cambios de alcance.

Reglas:

- Advertir antes de implementar una combinación inconsistente.
- **Autorización ≠ inicio:** habilita el trabajo, que comienza cuando la implementación empieza efectivamente.
- Registrar la autorización con su alcance, conforme a la sección anterior.
- Una autorización puede recuperarse en otra sesión si la evidencia persistida es verificable y no fue invalidada por cambios materiales.
- No pedir autorizaciones repetidas mientras sigan vigentes.
- Si la autorización cubre trabajo que un cambio de alcance posterior deja fuera, solicitar autorización nueva **solo para lo afectado**.

## Autorización CODE REVIEW

**Es una autorización humana. El agente no puede aprobarla.**

Al completar la implementación del alcance autorizado, se entrega un **Code Review Report** completo con:

1. resumen de lo implementado;
2. cómo funciona;
3. archivos creados, por qué existen y qué responsabilidad tienen;
4. archivos modificados, qué cambió y por qué fue necesario;
5. decisiones importantes de implementación;
6. referencias concretas: funciones, secciones y líneas cuando sea práctico;
7. puntos concretos que conviene inspeccionar;
8. orden recomendado de revisión;
9. checklist humano obligatorio.

El report cubre **todo** el código del alcance, **incluidos los tests**. Puede acompañarse de un resumen consolidado para facilitar la revisión, sin sustituir el report ni su checklist.

Checklist mínimo:

```text
[ ] Revisé los archivos creados/modificados.
[ ] Entiendo qué cambió y por qué.
[ ] Revisé las decisiones importantes.
[ ] No detecté comportamiento fuera de la spec.
[ ] Apruebo avanzar a TESTING.
```

**Rechazo:** registrar el rechazo, volver a implementación de lo afectado, corregir y emitir un **nuevo** Code Review Report. Nunca saltar la segunda revisión.

## Autorización TESTING

**Es una autorización humana, separada del Code Review.** Revisar y aprobar código no sustituye revisar y aprobar pruebas.

Se entrega un **Testing Report** con:

1. tests creados;
2. tests modificados;
3. relación entre tests y US;
4. criterios de aceptación cubiertos;
5. casos relevantes;
6. mocks utilizados;
7. assertions importantes;
8. posibles falsos positivos o puntos delicados;
9. qué debería revisar el usuario;
10. orden recomendado de revisión;
11. comandos exactos que el usuario debe ejecutar;
12. estado: pendiente de validación humana.

El usuario revisa cobertura, **ejecuta los tests personalmente** y confirma el resultado. No es obligatorio pegar toda la salida cuando pasan; sí solicitarla cuando fallan.

El agente **nunca** ejecuta tests, ni directa ni indirectamente, ni presenta tests como pasados sin resultados aportados por el usuario.

### TESTING N/A

`TESTING` sigue siendo autorización universal. Si no hay tests automatizados razonablemente aplicables:

- **no crear tests artificiales** de poco valor;
- justificar la no aplicabilidad con el motivo;
- obtener **validación humana explícita** de esa justificación.

N/A no es ausencia de implementación ni autorización omitida.

### Fallos en TESTING

Clasificar primero la causa:

| Causa | Respuesta |
| --- | --- |
| Requiere cambiar código de producción o de pruebas | Volver a implementación, corregir, y recorrer CODE REVIEW y TESTING de nuevo |
| Problema externo sin cambio de código | El trabajo **permanece** en revisión, con bloqueo registrado |
| Dependencia pendiente | El trabajo permanece en revisión, con bloqueo por dependencia |

Se puede solicitar CODE REVIEW aunque el alcance no pueda probarse end-to-end por una dependencia. Al resolverse la dependencia sin cambios de código, no se repite CODE REVIEW.

El vocabulario de bloqueo es fijo: `BLOCKED` con `Reason`, o `BLOCKED BY DEPENDENCY` con `Blocked by`. No crear estados nuevos por causa.

## CONVERGE

CONVERGE **no es una autorización humana** y no requiere aprobación previa: se ejecuta cuando se cumplen sus prerrequisitos.

**Prerrequisito:** trabajo técnico necesario completo. No ejecutarlo antes; el comando lo exige y su escritura es append-only sobre `tasks.md`.

Naturaleza:

- diagnostica cobertura y consistencia;
- **anexa** tareas de remediación a `tasks.md` con su referencia de origen;
- **no** modifica código, tests, `spec.md` ni `plan.md`;
- **no** ejecuta tests;
- si todo está satisfecho, deja `tasks.md` sin cambios.

Si faltan tareas necesarias, el agente propone el plan acotado y **espera autorización específica** antes de implementar. Detalles del ciclo en [`recovery.md`](recovery.md).

Con CONVERGE satisfactorio, entregar el **reporte integral de la funcionalidad**:

- Feature, alcance y US implementadas, con relación a los criterios de aceptación.
- Cambios de código y archivos relevantes, decisiones y riesgos técnicos.
- Tests escritos o modificados, qué validan, qué queda sin verificar y **comandos para que el usuario los ejecute**.
- Evidencia de CONVERGE y limitaciones conocidas.
- Solicitud de revisión y confirmación humana de resultados de tests ya aportados.

## Autorización FEATURE COMPLETION

Es **independiente** del Code Review integral y de CONVERGE. CONVERGE exitoso no autoriza el cierre.

Condiciones acumulativas:

1. Todo el trabajo técnico necesario implementado, con evidencia consistente.
2. Todas las US requeridas implementadas y revisadas, con sus autorizaciones vigentes.
3. CONVERGE final satisfactorio y vigente respecto del código y artefactos revisados.
4. Reporte integral disponible y revisión humana de la implementación realizada.
5. Resultados de tests aportados por el usuario, o excepción validada que la Constitución permita.
6. FEATURE COMPLETION AUTHORIZATION humana explícita, referida a la Feature y al resultado evaluado, vigente y persistida.
7. Estado de seguimiento verificado por lectura tras la persistencia.

Ejemplo suficiente:

> Revisé la evidencia y CONVERGE de SPEC-014. Autorizo explícitamente el cierre de la Feature SPEC-014.

Solo tras verificar la persistencia se actualiza `Status: Approved` en la spec y el registro de seguimiento a `Done`.

## Invalidación por impacto

**Invalidar por impacto, nunca reiniciar todo indiscriminadamente.**

| Cambio | Efecto |
| --- | --- |
| `spec.md` cambia | Revisar plan → revisar o regenerar tasks → ANALYZE |
| `plan.md` cambia | Revisar o regenerar tasks → ANALYZE |
| `tasks.md` cambia materialmente | ANALYZE |
| Código cambia con impacto en alcance aprobado | CODE REVIEW afectado queda invalidado |
| Tests cambian con impacto en alcance aprobado | CODE REVIEW y TESTING afectados quedan invalidados |
| Requisitos cambian | Evaluar US y artefactos afectados; invalidar solo el estado downstream afectado |
| Constitución cambia | Si no afecta la Feature, continuar; si la afecta, actualizar y volver a evaluar |

Preservar las autorizaciones no afectadas. Registrar la invalidación como evento nuevo con su motivo, **sin atribuirla a una aprobación humana ficticia** y sin borrar la evidencia histórica.

Cambios editoriales inocuos no invalidan automáticamente una autorización. Toda modificación relevante invalida la revisión final anterior: preparar reporte actualizado y obtener nueva aprobación.

## Disposición transitoria

El tablero anterior fue retirado y GitHub Projects aún no está configurado. Mientras tanto, las autorizaciones de las funcionalidades en curso se rigen por su estado real y por la evidencia ya persistida, que conserva su validez y **puede acreditar** la autorización vigente. No se exige reemitirla ni duplicarla, y no se afirma sincronización de ninguna funcionalidad sin verificarla.
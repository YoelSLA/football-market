# Gates — autorizaciones, reviews y cierre

## Autoridad y evidencia

Las autorizaciones humanas son **explícitas, específicas para la entidad evaluada y vinculadas al alcance revisado**.

No son autorizaciones válidas:

- `ok`, `dale`, `seguí` u otra expresión ambigua;
- una tarjeta en un estado concreto;
- un build exitoso, un commit, un PR o `tasks.md` completo;
- un resumen consolidado que no identifique la US o Feature aprobados.

Una respuesta puede aprobar **varias US en un solo mensaje**, pero **cada US conserva su propia evidencia**. Un resumen consolidado ayuda a revisar; no sustituye el gate individual.

Persistir la evidencia mediante `trello-traceability`. Si no puede verificarse la evidencia obligatoria, no declarar la transición completada.

## Gate READY

Antes de declarar READY:

- Feature identificada sin ambigüedad y Constitución consultada.
- `spec.md`, `plan.md`, `tasks.md` y checklists aplicables existen, están evaluados y son coherentes.
- Ambigüedades funcionales relevantes resueltas; criterios de aceptación cubiertos por tareas.
- ANALYZE completado, vigente y sin blockers; hallazgos no bloqueantes comunicados.
- Sin requisitos nuevos introducidos sin decisión humana.

READY no autoriza implementar. No pedir una confirmación humana adicional de READY: se presenta junto con la solicitud de autorización de IMPLEMENT.

Si no se cumple, detener o regresar a la fase afectada conforme a [`recovery.md`](recovery.md).

## Gate IMPLEMENT AUTHORIZATION

Presentar en una sola pantalla:

- Feature e identificador.
- US disponibles con dependencias e independencia entre ellas.
- Prerrequisitos compartidos y a quién habilitan.
- Resumen de plan, tareas relevantes y resultado de ANALYZE.
- Riesgos conocidos.

El usuario elige **qué US y prerrequisitos autoriza**. El agente decide el orden técnico.

Ejemplo suficiente:

> Autorizo IMPLEMENT de US1, US2 y US3 de SPEC-014, con los prerrequisitos compartidos identificados. No autorizo cambios de alcance.

Reglas:

- Persistir la evidencia **en cada US autorizada**, con su propio comentario.
- **Autorización ≠ inicio:** la US autorizada permanece `READY` hasta que IMPLEMENT comience efectivamente.
- Advertir antes de implementar una combinación inconsistente.
- Una autorización puede recuperarse en otra sesión si la evidencia persistida es verificable y no fue invalidada por cambios materiales.
- No pedir autorizaciones repetidas mientras sigan vigentes.
- Si la autorización cubre trabajo que un cambio de alcance posterior deja fuera, solicitar autorización nueva solo para lo afectado.

## Gate CODE REVIEW

**Es un gate humano por US.** El agente **no** puede aprobarlo.

Al completar la implementación de una US, la US entra a `CODE REVIEW` y el agente entrega un **Code Review Report** completo con:

1. resumen de lo implementado;
2. cómo funciona;
3. archivos creados, por qué existen y qué responsabilidad tienen;
4. archivos modificados, qué cambió y por qué fue necesario;
5. decisiones importantes de implementación;
6. referencias concretas: funciones, secciones y líneas cuando sea práctico;
7. puntos concretos que conviene inspeccionar;
8. orden recomendado de revisión;
9. checklist humano obligatorio.

El report cubre **todo** el código de la US, **incluidos los tests**. Puede acompañarse de un resumen consolidado para facilitar la revisión, sin sustituir el report individual ni su checklist.

Checklist mínimo:

```text
[ ] Revisé los archivos creados/modificados.
[ ] Entiendo qué cambió y por qué.
[ ] Revisé las decisiones importantes.
[ ] No detecté comportamiento fuera de la spec.
[ ] Apruebo avanzar a TESTING.
```

**Rechazo:** persistir `Result: REJECTED`, devolver la US a `IN PROGRESS`, corregir y emitir un **nuevo** Code Review Report. Nunca saltar la segunda revisión.

## Gate TESTING

**Es un gate humano por US, separado del Code Review.** Revisar y aprobar código no sustituye revisar y aprobar pruebas.

Al pasar la US a `TESTING`, el agente entrega un **Testing Report** con:

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
12. estado: `PENDING HUMAN VALIDATION`.

El usuario revisa cobertura, **ejecuta los tests personalmente** y confirma el resultado. No es obligatorio pegar toda la salida cuando pasan; sí solicitarla cuando fallan.

El agente **nunca** ejecuta tests, ni directa ni indirectamente, ni presenta tests como pasados sin resultados aportados por el usuario.

### TESTING N/A

`TESTING` sigue siendo gate universal. Si no hay tests automatizados razonablemente aplicables:

- **no crear tests artificiales** de poco valor;
- justificar `Automated tests: N/A` con el motivo;
- obtener **validación humana explícita** de esa justificación.

N/A no es ausencia de implementación ni gate omitido.

### Fallos en TESTING

Clasificar primero la causa:

| Causa | Transición |
| --- | --- |
| Requiere cambiar código de producción o de pruebas | `TESTING → IN PROGRESS`, corregir, y recorrer otra vez CODE REVIEW y TESTING |
| Problema externo sin cambio de código | La US **permanece** en `TESTING`, con bloqueo registrado |
| Dependencia pendiente | La US permanece en `TESTING`, con bloqueo por dependencia |

Una US puede entrar a `CODE REVIEW` aunque no pueda probarse end-to-end por una dependencia. Al resolverse la dependencia sin cambios de código, no se repite CODE REVIEW.

El vocabulario de bloqueo es fijo: `Status: BLOCKED` con `Reason`, o `Status: BLOCKED BY DEPENDENCY` con `Blocked by`. No crear estados nuevos por causa.

## Definición de DONE de una US

Solo con:

1. Implementación completa de su alcance, con evidencia consistente.
2. CODE REVIEW aprobado por humano, vigente y persistido.
3. TESTING aprobado por humano, o N/A validado explícitamente, vigente y persistido.

La regla `tasks.md complete = Feature DONE` queda reemplazada por esta definición. Con aprobación humana explícita, la transición a `DONE` es mecánica.

## Gate CONVERGE

CONVERGE **no es un gate humano** y no requiere autorización previa: se ejecuta automáticamente cuando se cumplen sus prerrequisitos.

**Prerrequisitos:** todas las US requeridas `DONE` y trabajo técnico necesario completo. No ejecutarlo antes; el comando lo exige y su escritura es append-only sobre `tasks.md`.

Naturaleza:

- diagnostica cobertura y consistencia;
- **anexa** tareas de remediación a `tasks.md` con su referencia de origen;
- **no** modifica código, tests, `spec.md` ni `plan.md`;
- **no** ejecuta tests;
- si todo está satisfecho, deja `tasks.md` sin cambios.

Si faltan tareas necesarias, el agente propone el plan acotado y **espera autorización específica** antes de implementar. Detalles del ciclo en [`recovery.md`](recovery.md).

Con CONVERGE satisfactorio, entregar el **reporte integral de Feature**:

- Feature, alcance y US implementadas, con relación a los criterios de aceptación.
- Cambios de código y archivos relevantes, decisiones y riesgos técnicos.
- Tests escritos o modificados, qué validan, qué queda sin verificar y **comandos para que el usuario los ejecute**.
- Evidencia de CONVERGE y limitaciones conocidas.
- Solicitud de revisión y confirmación humana de resultados de tests ya aportados.

## Gate FEATURE COMPLETION AUTHORIZATION

Es **independiente** del Code Review integral y de CONVERGE. CONVERGE exitoso no autoriza el cierre: la Feature permanece en `TESTING`.

Condiciones acumulativas para `DONE` de Feature:

1. Todas las US requeridas y tareas obligatorias implementadas, con evidencia consistente.
2. Todas las US requeridas en `DONE` con sus gates vigentes.
3. CONVERGE final satisfactorio y vigente respecto del código y artefactos revisados.
4. Reporte integral disponible y revisión humana de la implementación realizada.
5. Resultados de tests aportados por el usuario, o excepción validada que la Constitución permita.
6. FEATURE COMPLETION AUTHORIZATION humana explícita, referida a la Feature y al resultado evaluado, vigente y persistida.
7. Transiciones de Trello verificadas tras la persistencia.

Ejemplo suficiente:

> Revisé la evidencia y CONVERGE de SPEC-014. Autorizo explícitamente el cierre de la Feature SPEC-014.

Solo tras verificar la persistencia se actualiza `Status: Approved` en la spec y la Feature a `DONE`.

## Invalidación por impacto

**Invalidar por impacto, nunca reiniciar todo indiscriminadamente.**

| Cambio | Efecto |
| --- | --- |
| `spec.md` cambia | Revisar plan → revisar o regenerar tasks → ANALYZE; recuperar READY |
| `plan.md` cambia | Revisar o regenerar tasks → ANALYZE |
| `tasks.md` cambia materialmente | ANALYZE |
| Código cambia con impacto en alcance aprobado | CODE REVIEW afectado queda invalidado |
| Tests cambian con impacto en alcance aprobado | CODE REVIEW y TESTING afectados quedan invalidados |
| Requisitos cambian | Evaluar US y artefactos afectados; invalidar solo el estado downstream afectado |
| Constitución cambia | Si no afecta la Feature, continuar; si la afecta, invalidar READY y actualizar |

Preservar aprobaciones de las US no afectadas. Registrar la invalidación como evento nuevo con `Reason`, **sin atribuirla a una aprobación humana ficticia** y sin borrar la evidencia histórica.

Cambios editoriales inocuos no invalidan automáticamente un gate. Toda modificación relevante invalida la revisión final anterior: preparar reporte actualizado y obtener nueva aprobación.
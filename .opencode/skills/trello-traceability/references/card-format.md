# Formato de tarjeta — descripciones breves orientadas al estado

## Principio

Trello es un **tablero operativo**, no un documento técnico ni un repositorio de auditoría.
Una tarjeta se consulta en segundos: debe responder **qué se desarrolla, en qué estado está,
qué está terminado, qué está bloqueado, qué falta y cuál es la próxima acción**.

Si para entender una tarjeta hay que leer decenas de comentarios, la tarjeta está mal.
El detalle técnico vive en los artefactos SDD; la tarjeta solo orienta.

## Presupuesto de tamaño

| Elemento | Objetivo | Máximo |
| --- | --- | --- |
| Descripción de US | 300–800 caracteres | 1.000 |
| Descripción de EPIC/SPEC | 600–1.200 caracteres | 1.800 |
| Comentario de evento | conciso | 800 |
| Comentario de evidencia obligatoria | lo que exija el gate | 1.500 |

Superar el máximo obliga a mover el detalle al artefacto SDD correspondiente, no a acortar
la evidencia obligatoria. Cuando el contenido lo justifique, agregar una tabla breve en lugar
de prosa larga.

## Estructura de User Story

```markdown
## <Título de la historia>

## Estado actual
- **Estado:** <columna>
- **Fase:** <SDD phase>
- **Prioridad:** <P1|P2> · US<nn> de SPEC-<id>

## Progreso
- **Completado:** <hechos verificables>
- **Pendiente:** <lo que falta>

## Bloqueos
<solo bloqueos vigentes, o "Ninguno">

## Próxima acción
<una acción concreta>

## Referencias
<rutas de spec/plan/tasks y nota de dónde vive la evidencia>
```

Omitir una sección si no aporta información. **No** completar secciones por simetría.

## Estructura de EPIC / SPEC

```markdown
## Objetivo
<qué cambia en el dominio o producto, en una o dos frases>

## Estado
<columna, avance global y una frase sobre la fase vigente>

## Progreso
<tabla breve US → columna → estado, o el corte equivalente>

## Bloqueos
<bloqueos principales de la Feature, no el detalle de cada US>

## Próxima acción
<una acción>

## Referencias
<rutas de la spec, índice y normativa>
```

La EPIC **no** repite el contenido de sus US: resume y enlaza.

## Qué NO va en la descripción

- Detalle técnico, diseño o listados de archivos: viven en `plan.md`, `contracts/`, `design`.
- Escenarios de aceptación, criterios, RF y tareas `Tnnn`: viven en `spec.md` y `tasks.md`.
- Historial de gates, autorizaciones, revisiones y resultados: viven en **comentarios**.
- Texto de la spec reescrito, resúmenes del enunciado, notas de migración o advertencias
  normativas genéricas que ninguna regla vigente exija en la tarjeta.
- Anticipación de estados: la descripción refleja lo acreditado, no lo esperado.

Enlace a la ruta del documento en lugar de copiarlo. Si un lector necesita el detalle, la
descripción debe decirle **dónde está**.

## Uso de comentarios

Los comentarios se reservan para **eventos relevantes y evidencias obligatorias**:

| Usar comentario | Usar descripción |
| --- | --- |
| Evidencia `[SDD GATE]`: autorización, Code Review, Testing, invalidación | Estado y fase vigentes |
| Decisión humana o funcional que cambia el rumbo | Bloqueo o próximo paso |
| Incidente o fallo operativo relevante | — |
| Bloqueo que se levanta o cambia | Estado actual |

Reglas:

1. **No duplicar** en la descripción lo que ya está en comentarios.
2. **No generar** comentarios extensos de diagnóstico cuando basta actualizar la descripción.
3. **No repetir** un evento ya registrado: si el estado no cambió, no hay comentario nuevo.
4. Un comentario nuevo que un anterior vuelve inútil no justifica borrar el anterior:
   la evidencia histórica se conserva.
5. Nunca usar un comentario para sustituir a la descripción como estado vigente.
6. No agregar comentarios para explicar limpiezas o reorganizaciones del tablero.

## Mantenimiento

- Actualizar la descripción cuando cambie el estado, el bloqueo o la próxima acción.
- No mover una tarjeta sin actualizar antes su descripción, y al revés.
- Si una tarjeta acumula historia que ya no orienta, **reescribir la descripción**, nunca
  borrar evidencia histórica.
- Ante contradicción entre tarjeta y artefacto, prevalece el artefacto; corregir la tarjeta
  con autorización y sin tocar la evidencia registrada.

## Compatibilidad normativa

Este formato **no** habilita ninguna transición, no declara ni deriva gates y no altera
`gates.md`, la Constitución ni el workflow. La simplificación es de presentación: la evidencia
obligatoria sigue persistida en comentarios y los artefactos SDD conservan la autoridad funcional.

Al reescribir una descripción se **MUST** preservar toda la información esencial que solo
existía en ella y no está en los artefactos ni en comentarios: claves, relaciones, origen y
advertencias vigentes. Antes de escribir, guardar el contenido original para poder comparar.
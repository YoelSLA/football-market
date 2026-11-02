# Git y GitHub — ramas, commits y Pull Requests

## Alcance y autoridad

Este documento es la **fuente única** de las convenciones de Git del proyecto. La Constitución
gobierna el principio de trazabilidad —cada resultado es trazable a su origen— y delega aquí su
procedimiento. Ante conflicto, la Constitución prevalece; ante conflicto entre reglas de este
documento y una instrucción puntual del usuario en el momento, prevalece la instrucción del
usuario.

Las reglas de Git se aplican a **toda rama creada en el repositorio**, con independencia de quién
la cree.

## Formato de ramas

```
<type>/<spec-id?>-<scope>
```

El segmento `<spec-id>` es opcional y solo se incluye cuando el cambio corresponde a una spec
existente.

- `<type>` indica la naturaleza del cambio y se toma de esta lista cerrada: `feat`, `fix`,
  `refactor`, `test`, `docs`, `chore`, `perf`, `ci`.
- `<spec-id>` es el identificador de la spec a la que pertenece el cambio.
- `<scope>` describe brevemente el cambio en `kebab-case` **y en inglés**.

Si existe una spec relacionada:

```
<type>/<spec-id>-<scope>
```

Si el cambio es pequeño, técnico o independiente y no justifica una spec propia:

```
<type>/<scope>
```

El nombre de la rama y el del directorio de la spec son cosas distintas: el directorio usa solo
`<spec-id>-<scope>`, sin el prefijo de tipo. Para derivar uno del otro, quitar el prefijo
`<type>/`. El `<scope>` es el mismo en la rama y en el directorio de la spec.

## Reglas obligatorias

1. Antes de crear una rama, el agente debe revisar las specs existentes y determinar si el cambio
   pertenece a alguna. No debe crear nombres de rama sin esa determinación previa.
2. Un cambio que implemente, modifique o complete una spec existente debe **reutilizar su
   identificador**. Reutilizar el identificador no autoriza crear una spec nueva para un cambio
   que ya se asocia correctamente a una existente.
3. No asociar artificialmente un cambio a una spec cuando no exista relación funcional clara.
4. `<scope>` siempre en `kebab-case` y en inglés. No se admiten `snake_case`, `camelCase` ni `PascalCase`.
5. No se admiten nombres personales ni descripciones genéricas o ambiguas. El nombre debe ser
   breve, descriptivo y representar con claridad el cambio.
6. Cuando la rama incluye un identificador de spec, el trabajo se vincula con el directorio
   correspondiente dentro de `specs/`, cuyos artefactos son la fuente principal de requisitos y
   contexto. La rama no sustituye ni reemplaza a los artefactos de la spec.
7. Se mantiene la trazabilidad entre spec, rama, commits y Pull Request. El Pull Request
   referencia la spec y la rama; los commits de la rama se asocian a esa spec.
8. El `<scope>` se escribe en **inglés**, aunque los issues, los títulos y la documentación del
   proyecto estén en español. El scope es un identificador técnico de la spec y de la rama, no un
   texto de negocio: se expresa en inglés la misma intención, sin transliterar ni traducir
   mecánicamente las palabras del título de origen. El directorio de la spec reutiliza ese mismo
   scope. Ejemplo: el issue `REQ-S2-15 | Visualizar portfolio personal` produce el scope
   `view-personal-portfolio-ui`, nunca `visualizar-portfolio-personal`.

## Nomenclatura de las herramientas de especificación

Las herramientas de creación de features del proyecto pueden definir su propia nomenclatura interna
para los directorios de spec. Cuando esa nomenclatura difiera del formato de rama anterior, el
formato de rama de este documento prevalece y la divergencia **se reporta** en lugar de resolverse
por cuenta propia.

## Creación y reanudación de una spec

Una spec nueva se crea con el flujo normal. Para retomar trabajo que pertenece a una spec
existente, se usa siempre la opción de reutilización de la herramienta, indicando el nombre corto
de esa spec. No debe provocarse un identificador nuevo para continuar trabajo de una spec existente:
sin reutilización, el flujo asigna el siguiente número y crea una spec distinta.

- El flujo intenta crear o reutilizar la rama asociada cuando el estado de Git lo permite. Es
  **idempotente**: si la rama ya existe localmente la reutiliza, si existe solo en remoto crea la
  rama local siguiendo la remota, y nunca genera ramas duplicadas ni sufijos adicionales.
- Un fallo u omisión de ese paso **no acredita** que la rama esté disponible.
- La reutilización **no descarta, sobrescribe ni hace `stash`** de cambios locales. Si hay cambios
  rastreados pendientes o `HEAD` está separado, el paso de rama se aborta con un mensaje y deja el
  estado de trabajo intacto; la spec se crea o se reutiliza igual.
- Comprobar el resultado con los campos de salida del comando —identificador, archivo, número de
  feature, nombre de rama, acción y estado de rama— en vez de asumirlo por el texto.
- Ninguno de esos parámetros descarta ni sobrescribe trabajo local preexistente.

El procedimiento exacto, sus opciones y sus condiciones están en
[`sdd-workflow.md`](sdd-workflow.md).

## Commits

Formato exigido por los hooks del repositorio:

```
<type>(<scope>): <descripción>
```

o

```
<type>: <descripción>
```

Los tipos admitidos son los de la lista cerrada anterior. El texto va en **inglés**.

- Declarar en el mensaje la spec, las User Stories o las tareas correspondientes, y el alcance
  compartido, con hashes reales cuando existan.
- No inventar hashes, resultados ni aprobaciones.
- No hacer `commit`, crear ramas ni `push` sin encargo explícito.

## Pull Requests

- El Pull Request referencia la spec, la rama, la evidencia y los pendientes.
- **Momento de creación:** El Pull Request se crea obligatoriamente al finalizar la fase de CLARIFY (una vez que la especificación y sus clarificaciones están registradas, confirmadas y subidas a la rama remota), dirigiéndose a la rama de integración del proyecto (`dev`) y vinculando el issue correspondiente.
- Vincularlo a la funcionalidad y a su registro de seguimiento **no** acredita implementación,
  revisión ni cierre.
- No inventar URL ni hash de Pull Request. La revisión del diff local basta para aprobar; la
  aprobación de un estado exacto no implica merge ni publicación.
- Los Pull Requests se dirigen a la rama de integración del proyecto.

## Revisión del estado de trabajo

Antes y después de cualquier cambio:

1. Revisar `git status --short` y el diff.
2. Preservar los cambios previos y los archivos sin seguimiento; no incorporarlos ni revertirlos
   accidentalmente.
3. Distinguir en el reporte los fallos introducidos por el cambio de los preexistentes
   demostrablemente ajenos y de los problemas de entorno.
4. Revisar el diff final, `git diff --check` y `git status --short` antes de entregar.

## Integridad de la evidencia

La existencia de código, un plan, tareas marcadas, un commit, un merge o un control técnico
satisfactorio **no acreditan** fases, revisiones ni cierres. La evidencia de integración reside en
Git, los Pull Requests y los controles automáticos; la evidencia funcional reside en los
artefactos de la especificación y en los informes de revisión y testing.
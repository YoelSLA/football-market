# Guía de trabajo para agentes

## Contexto mínimo

Leer la [constitución](.specify/memory/constitution.md) antes de modificar código o documentación normativa. Consultar solo las secciones aplicables de los documentos siguientes, incluyendo sus reglas generales y referencias necesarias.

| Tarea | Contexto adicional |
| --- | --- |
| Código, estructura o dependencias arquitectónicas | [Arquitectura común](docs/architecture.md): guía de lectura al inicio. |
| Stack, tecnologías o dependencias de herramientas/librerías | [Tecnologías del backend](docs/backend/technologies.md) o [tecnologías del frontend](docs/frontend/technologies.md), según el área afectada. |
| Área backend afectada | [Instrucciones del backend](backend/AGENTS.md) y el contexto que estas indiquen. |
| Área frontend afectada | [Instrucciones del frontend](frontend/AGENTS.md) y el contexto que estas indiquen. |
| Feature especificada | Artefactos relevantes de su carpeta en `specs/`; la numeración más alta no identifica necesariamente la tarea. |
| Creación o nombre de una rama | [Constitución §7](.specify/memory/constitution.md): formato, tipos y reglas obligatorias. |
| Paso de Spec Kit | Skill correspondiente en `.agents/skills/`; ejecutar solo el paso solicitado. |

No cargar instrucciones ni documentación del backend para una tarea exclusivamente frontend ni viceversa. Una tarea que afecte ambas áreas debe consultar ambos archivos específicos.

## Ramas y Git

- Antes de crear o proponer una rama, determinar si el cambio pertenece a una spec existente revisando las carpetas de `specs/`. Reutilizar su identificador cuando exista relación funcional clara; no crear una spec nueva para un cambio que ya se asocia a una existente, ni asociarlo artificialmente.
- Construir el nombre según la constitución §7: `<type>/<spec-id>-<scope>` cuando hay spec relacionada, `<type>/<scope>` cuando el cambio es pequeño, técnico o independiente. El `<scope>` va en `kebab-case`.
- El nombre de la rama y el nombre del directorio de la spec son cosas distintas: el directorio usa solo `<spec-id>-<scope>`, sin el prefijo de tipo. Para derivar uno del otro, quitar el prefijo `<type>/`.
- Cuando la tarea incluya el nombre de una rama, mantener la trazabilidad spec → rama → commits → Pull Request.

### Creación y reanudación de una spec

Una spec nueva se crea con el flujo normal, sin `--reuse`. Para retomar trabajo que pertenece a una spec existente, usar siempre `--reuse` con el `--short-name` de esa spec. **No provocar un identificador nuevo para continuar trabajo de una spec existente**: sin `--reuse` el flujo asigna el siguiente número y crea una spec distinta.

- El flujo garantiza que exista la rama asociada. Es idempotente: si la rama ya existe localmente la reutiliza, si existe solo en remoto crea la rama local siguiendo la remota, y nunca genera ramas duplicadas ni sufijos adicionales.
- `--reuse` reutiliza el `spec-id`, el nombre de spec y el directorio existentes, y no sobrescribe `spec.md` ni los demás artefactos. Resuelve por coincidencia exacta del nombre de directorio o del scope; sin coincidencia o con varias, falla y las lista, sin elegir por su cuenta. Combinado con `--number`, desambigua o valida el identificador, y falla si contradice la spec encontrada.
- `--reuse` es distinto de `--allow-existing-branch`: el primero localiza una spec existente por scope; el segundo solo permite que el flujo de creación caiga sobre un directorio ya existente.
- Ninguno de los dos descarta, sobrescribe ni hace `stash` de cambios locales. Si hay cambios rastreados pendientes o `HEAD` está separado, el paso de rama se aborta con un mensaje y deja el estado de trabajo intacto; la spec se crea o se reutiliza igual.
- Comprobar el resultado con `SPEC_ACTION` y `BRANCH_STATUS` en la salida en vez de asumirlo por el texto.

## Antes y durante el cambio

- Revisar `git status --short` y el diff. Preservar cambios previos y archivos sin seguimiento; no incorporarlos ni revertirlos accidentalmente.
- Buscar con `rg` / `rg --files` y leer implementación, consumidores, tests y configuración afectados antes de editar.
- Resolver decisiones rutinarias con el contexto disponible; consultar si falta una decisión funcional que cambie el resultado esperado.
- Mantener `tasks.md` cuando sea el seguimiento de la tarea. Marcar completado solo lo implementado y verificado; registrar las comprobaciones pendientes del usuario.
- No editar salidas generadas; modificar sus fuentes. Las instrucciones de cada área identifican sus ubicaciones específicas. Revisar el diff después del build.

Ubicaciones transversales: CI en `.github/workflows/` y hooks en `.githooks/`. Consultar versiones y scripts efectivos en la configuración.

## Verificación y entrega

El agente no ejecuta tests, directa ni indirectamente mediante builds, generadores o hooks, en ninguna tarea del repositorio.

Los únicos controles ejecutables permitidos son los builds definidos por los `AGENTS.md` específicos de cada área, y únicamente cuando se haya modificado código de esa área.

Para cambios exclusivamente documentales, revisar rutas, enlaces, comandos y coherencia; no ejecutar build. Las condiciones de entrega y los fallos preexistentes se rigen por la constitución.

Antes de entregar, revisar el diff final, `git diff --check` y `git status --short`. Informar cambios, verificaciones y pendientes, distinguiendo fallos del cambio de problemas preexistentes o del entorno. No afirmar resultados sin evidencia: el build no acredita tests y la existencia de CI o hooks no acredita su ejecución.

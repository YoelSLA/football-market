# Guía de trabajo para agentes

## Contexto mínimo

Leer la [constitución](.specify/memory/constitution.md) antes de modificar código o documentación normativa. Consultar solo las secciones aplicables de los documentos siguientes, incluyendo sus reglas generales y referencias necesarias.

| Tarea | Contexto adicional |
| --- | --- |
| Código, estructura o dependencias | [Arquitectura común](docs/architecture.md): guía de lectura al inicio. |
| Área backend afectada | [Instrucciones del backend](backend/AGENTS.md) y el contexto que estas indiquen. |
| Área frontend afectada | [Instrucciones del frontend](frontend/AGENTS.md) y el contexto que estas indiquen. |
| Feature especificada | Artefactos relevantes de su carpeta en `specs/`; la numeración más alta no identifica necesariamente la tarea. |
| Paso de Spec Kit | Skill correspondiente en `.agents/skills/`; ejecutar solo el paso solicitado. |

No cargar instrucciones ni documentación del backend para una tarea exclusivamente frontend ni viceversa. Una tarea que afecte ambas áreas debe consultar ambos archivos específicos.

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

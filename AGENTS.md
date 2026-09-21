# Guía de trabajo para agentes

## Contexto mínimo

Leer la [constitución](.specify/memory/constitution.md) antes de modificar código o documentación normativa. Consultar solo las secciones aplicables de los documentos siguientes, incluyendo sus reglas generales y referencias necesarias; no cargar backend para una tarea exclusivamente frontend ni viceversa.

| Tarea | Contexto adicional |
| --- | --- |
| Código, estructura o dependencias | [Arquitectura](docs/architecture.md): guía de lectura al inicio. |
| Cambiar comportamiento backend o crear, modificar o analizar sus tests | [Testing](docs/testing.md): reglas generales y categoría afectada. |
| Feature especificada | Artefactos relevantes de su carpeta en `specs/`; la numeración más alta no identifica necesariamente la tarea. |
| Paso de Spec Kit | Skill correspondiente en `.agents/skills/`; ejecutar solo el paso solicitado. |

## Antes y durante el cambio

- Revisar `git status --short` y el diff. Preservar cambios previos y archivos sin seguimiento; no incorporarlos ni revertirlos accidentalmente.
- Buscar con `rg` / `rg --files` y leer implementación, consumidores, tests y configuración afectados antes de editar.
- Ante cambios HTTP, revisar Controller, DTO, seguridad, contrato, tests, OpenAPI, REST Docs y Postman. Ante persistencia, revisar Model, Repository, migraciones y validación del esquema.
- Resolver decisiones rutinarias con el contexto disponible; consultar si falta una decisión funcional que cambie el resultado esperado.
- Mantener `tasks.md` cuando sea el seguimiento de la tarea. Marcar completado solo lo implementado y verificado; registrar las comprobaciones pendientes del usuario.
- No editar salidas generadas (`build/`, `dist/`, `*.tsbuildinfo`); modificar sus fuentes. Revisar el diff después del build.

Ubicaciones particulares: migraciones en `backend/src/main/resources/db/migration/`, REST Docs en `backend/src/docs/asciidoc/`, colección en `postman/collections/`, CI en `.github/workflows/` y hooks en `.githooks/`. Consultar versiones y scripts efectivos en la configuración; `backend/README.md` puede estar desactualizado.

## Verificación y entrega

**El agente no ejecuta tests, directa ni indirectamente mediante builds, generadores o hooks.** Si necesita resultados del backend, indicar al usuario los tests concretos y solicitar su salida. Por el momento no crear ni ejecutar tests de frontend ni incorporar infraestructura para ellos.

Como control ejecutable, realizar únicamente el build del área cuyo código se haya modificado:

| Directorio | Comando |
| --- | --- |
| `backend/` | `./gradlew build -x test` |
| `frontend/` | `npm run build` |

Para cambios exclusivamente documentales, revisar rutas, enlaces, comandos y coherencia; no ejecutar build. Las condiciones de entrega y los fallos preexistentes se rigen por la constitución.

Antes de entregar, revisar el diff final, `git diff --check` y `git status --short`. Informar cambios, verificaciones y pendientes, distinguiendo fallos del cambio de problemas preexistentes o del entorno. No afirmar resultados sin evidencia: el build no acredita tests y la existencia de CI o hooks no acredita su ejecución.

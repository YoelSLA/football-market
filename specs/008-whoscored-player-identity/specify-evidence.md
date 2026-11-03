# Evidencia de fase Specify

**Fecha**: 2026-10-13
**Responsable**: Agente, por solicitud del usuario.
**Alcance**: Identidad externa WhoScored; reutilización del número abandonado autorizada expresamente tras la división del alcance anterior.
**Resultado documental**: OK; spec redactada y calidad revisada 16/16. Sin implementación ni aceptación humana de gates.

## Entradas evaluadas

- Descripción funcional del usuario y autorización posterior para reutilizar 008 y reformular el índice.
- Constitution 6.1.0, AGENTS raíz/backend, workflow SDD y comando local Specify.
- Plantilla efectiva `.specify/templates/spec-template.md`; no hay directorios de presets/extensiones ni override seleccionado.
- Fuentes académicas `tp/enunciado.md` y `tp/entregas.md`, contrato HTTP oficial y convenciones OpenAPI.
- Specs existentes y estado/diff Git: no existe una spec vigente WhoScored. El alcance antiguo permanece solo como antecedente en el índice.

## Materialización real

Comando ejecutado una sola vez desde la raíz:

```bash
.specify/scripts/bash/create-new-feature.sh --json --short-name 'whoscored-player-identity' 'Registrar la identidad externa de WhoScored de jugadores locales mediante descubrimiento paginado, matching por nombre, revision de pendientes, pase historico complementario y auditoria; sin estadisticas ni cotizaciones.'
```

Salida observada:

```text
SPEC_NAME=008-whoscored-player-identity
SPEC_FILE=/c/Users/Vaqui/IdeaProjects/football-market/specs/008-whoscored-player-identity/spec.md
FEATURE_NUM=008
BRANCH_NAME=feat/008-whoscored-player-identity
SPEC_ACTION=created
BRANCH_STATUS=skipped-dirty-tree
```

La numeración se asignó por el script, sin `--number` ni carpeta creada manualmente.
Aunque se reutiliza un número histórico por autorización, `SPEC_ACTION=created`
es correcto: el directorio reformulado no existía y no es una reanudación de artefactos eliminados.
`.specify/feature.json` leído después: `feature_directory=specs/008-whoscored-player-identity`.
No se encontraron overrides de entorno SPECIFY en la sesión PowerShell.
La rama actual sigue siendo `feat/008-whoscored-player-statistics`; el script no creó
ni seleccionó la rama asociada debido a cambios rastreados pendientes. Alineación
de rama pendiente antes de operaciones que la necesiten; no hubo stash ni descarte.

## Validación y límites

- [Spec](spec.md): tres US, 24 requisitos funcionales, ocho criterios de éxito y supuestos explícitos.
- [Checklist evaluado](checklists/requirements.md): 16/16, una iteración documental. No hay marcadores de aclaración pendientes en la spec.
- `.specify/extensions.yml` ausente en las inspecciones previa y posterior; hooks `before_specify` y `after_specify` no aplicables.
- Hooks Git efectivos: configuración `core.hooksPath` sin valor; `.git/hooks` solo contiene archivos `.sample`, sin `post-checkout` ejecutable.
- No se consultó WhoScored ni se verificaron de nuevo los datos externos aportados.
- No se ejecutaron tests, builds ni fases posteriores. No se escribieron código, plan ni tareas.
- Trello no modificado: autorización recibida para el archivo local, sin autorización operativa explícita sobre el tablero. Feature, US y evidencia de fase pendientes de sincronización; no se trasladan US ni gates antiguos.
- Revisión final de spec, enlaces y diff realizada; `git diff --check` y comprobación de whitespace de los tres archivos nuevos sin diagnósticos. Estado Git conserva los cambios previos de `PlayerRepository.java` y `opencode.json`; los archivos nuevos siguen sin staging. Los enlaces rotos del alcance eliminado se conservan como antecedentes explícitos, sin fingir recuperación.

## Actualización posterior de requisitos

El usuario reemplazó la regla inicial de coincidencia por nombre único: toda asociación exige ahora coincidencia conjunta del nombre normalizado del jugador y del equipo mediante normalización o alias explícito. Se incorporó a la spec el catálogo normativo de 96 equipos persistidos frente a WhoScored para las cinco ligas, incluidos los tres casos de Serie A confirmados después: `Parma Calcio 1913` ↔ `Parma Calcio 1913`, `Venezia FC` ↔ `Venezia` y `US Lecce` ↔ `Lecce`.

La actualización afecta escenarios, casos límite, FR-005–007, SC-001 y supuestos, y sustituye la regla anterior que permitía asociar por nombre único. Se actualizó la nota de calidad correspondiente sin ejecutar CLARIFY ni otra fase posterior. La evaluación general conserva 16/16 porque la decisión elimina la ambigüedad y mantiene requisitos verificables; no acredita READY ni autorización de implementación.

El 2026-10-14 se incorporó evidencia reproducible del contrato observado del catálogo: solicitud mínima, aislamiento de headers, página actual, última página corta, página posterior vacía y página histórica. Las respuestas demuestran metadatos `paging` completos y que el proveedor sirve JSON válido con `Content-Type: text/html`. FR-002, FR-015 y FR-017 se precisaron para cerrar por `currentPage == totalPages`, rechazar metadatos ausentes o incoherentes y validar el cuerpo en vez del MIME aislado. Los cuerpos se conservan bajo `evidence/`; no se ejecutaron tests, builds ni implementación.

## Siguiente acción permitida

Solicitar `/speckit.clarify` y autorización operativa para sincronizar Trello.
**READY: NO**. IMPLEMENT AUTHORIZATION: NONE. FEATURE COMPLETION AUTHORIZATION: PENDING.
La conclusión de Specify se basa en contenido y evaluación documental, no en la mera existencia de archivos.

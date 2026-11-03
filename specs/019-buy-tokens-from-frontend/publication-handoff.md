# Publicación de avances — entrega para ejecución humana

## Resultado de la preparación

- Rama local: `feat/019-buy-tokens-from-frontend`. Origin: `git@github.com:YoelSLA/football-market.git`.
- Revisión de diff rastreado y lista de archivos nuevos: cambios relacionados con SPEC-019, sin backend ni salidas generadas. React-Toastify/tecnologías, Field/ref, Modal, estilos de paginación e iconos son prerrequisitos/consumidores del mercado; AppLayout aplica scroll contenido solo en esta ruta. Assignments agrega la fila de SPEC-019.
- tasks.md: 18/20 tareas originales; T-17/T-18 pendientes. implementation-report/future-work registran integración real y E2E pendientes, sin cierre.
- Build de publicación ejecutado: `npm run build` desde frontend, satisfactorio (2483 módulos, warning JS ~649 kB). Sin tests ni compras.
- `git diff --check` conserva tres avisos previos en barrels de players/portfolio/shared; no se afirma formato/lint completo aprobado.
- Issue #42 leída: OPEN. Consulta de PRs para esta rama: sin PR existente. Rama remota de feature observada en `2ac3554a75c8d9e49b32a3560ed26f54e9376ee0`; el agente no creó ese commit. No se modificó origin/dev ni se integró su avance localmente.

## Bloqueo constitucional

`core.hooksPath=.githooks` está activo y los hooks tienen permiso ejecutable:

- pre-commit ejecuta `./gradlew spotlessCheck --no-daemon` y `npm run biome:check`, controles fuera de los permitidos al agente.
- pre-push ejecuta `fastTest` ante backend/caso incierto/referencia nueva. La rama ya existe y los cambios actuales son frontend/docs, pero ese hecho no elimina el bloqueo de pre-commit.
- `.github/workflows/ci.yml` ejecuta `./gradlew build --stacktrace` (con tests) al abrir/sincronizar PR hacia dev, sin filtro de rutas ni exclusión de borradores. Crear PR también dispararía un control no permitido al agente.

Constitución y workflow exigen entregar estas operaciones al usuario, sin disparar los controles indirectamente ni deshabilitarlos. No se hizo stage, commit, push, PR, merge, cierre del issue ni cambio de configuración/hooks. **No hay hashes de commits nuevos ni URL de PR que informar.**

## Secuencia para la persona responsable

Ejecutar en la raíz, revisando el estado vigente antes de cada paso. Los comandos siguientes **no fueron ejecutados por el agente**. No usar bypass de hooks; si sus controles fallan, revisar sus resultados antes de publicar. La apertura del PR dispara CI con tests.

### 1. Código y prerrequisitos de SPEC-019

```bash
git status --short
git add -- docs/frontend/technologies.md frontend/package.json frontend/package-lock.json frontend/src/app/layouts/AppLayout/AppLayout.module.scss frontend/src/app/layouts/AppLayout/AppLayout.tsx frontend/src/app/navigation/sections.ts frontend/src/app/providers/AppProviders/AppProviders.tsx frontend/src/app/router/AppRouter/AppRouter.tsx frontend/src/app/market frontend/src/features/market frontend/src/features/players/index.ts frontend/src/features/portfolio/index.ts frontend/src/shared/components/Field/Field.tsx frontend/src/shared/components/Modal frontend/src/shared/components/Pagination/Pagination.module.scss frontend/src/shared/components/Pagination/Pagination.tsx frontend/src/shared/components/index.ts frontend/src/shared/icons/index.ts
git diff --cached --stat
git diff --cached --check
git commit -m "feat(spec-019): implement market buy frontend and quote guards" -m "Implement SPEC-019 T-00-T-16 with market layout, modal, query guards and app composition. Real quote integration and E2E validation remain pending. Refs #42."
```

### 2. Artefactos y evidencia de entrega parcial

```bash
git add -- specs/019-buy-tokens-from-frontend specs/assignments.md
git diff --cached --stat
git diff --cached --check
git commit -m "docs(spec-019): record partial delivery and pending quote integration" -m "Document T-19 build evidence, 18 of 20 original tasks and pending T-17/T-18. Preserve E2E and visual limitations without closing SPEC-019 or issue #42."
```

### 3. Publicación y Pull Request a dev

```bash
git status --short
git push -u origin feat/019-buy-tokens-from-frontend
gh pr create --base dev --head feat/019-buy-tokens-from-frontend --title "feat(spec-019): add market buy frontend progress" --body-file specs/019-buy-tokens-from-frontend/pull-request.md
gh pr view --json number,url,state,baseRefName,headRefName
gh issue view 42 --json number,state,url
```

No usar palabras de autocierre en el body: se usa `Refs #42`, no Closes/Fixes/Resolves. No ejecutar merge ni escribir directamente sobre dev. Si aparece un PR concurrente, reutilizarlo en vez de duplicarlo. Actualizar este informe con hashes/URL reales solo después de observarlos; no presentar esta secuencia como publicación realizada.

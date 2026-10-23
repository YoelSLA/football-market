# API Contract & Security Checklist: Consultar ranking de jugadores por score

**Purpose**: Validar la calidad de los requisitos de contrato HTTP y de seguridad/autorización de SPEC-018 antes de Tasks. No verifica implementación ni comportamiento del sistema.
**Created**: 2026-10-23
**Feature**: [spec.md](../spec.md) · [contracts/score-ranking-http.md](../contracts/score-ranking-http.md) · [plan.md](../plan.md)

**Note**: Esta checklist especializada fue generada por `/speckit.checklist` a partir del foco indicado por el revisor: contrato HTTP y seguridad/autorización, con profundidad de sanity check ligero. Complementa —no sustituye— `checklists/requirements.md`.
**Review Ownership**: This checklist is a reviewer-owned requirements-quality review artifact. Mark an item `[x]` only when the reviewer determines the requirements-quality criterion is satisfied.
**Marker Semantics**: `[x]` means the criterion has been reviewed and satisfied for requirements quality. It does not mean implementation work is complete.

## Contrato HTTP: Completitud

- [x] CHK001 ¿El contrato define todos los campos de la respuesta 200 —`strategy`, `quoteDate`, `ranking` y los de cada entrada— con tipo, nulabilidad y semántica? [Completeness, Spec §FR-008–§FR-010, Contract §1.2]
- [x] CHK002 ¿Está especificado que `ranking` siempre está presente y es un arreglo, incluso cuando no hay entradas? [Completeness, Spec §FR-010–§FR-011, Contract §1.2]
- [x] CHK003 ¿Se define de forma inequívoca la forma de la respuesta vacía —objeto con encabezado nulo frente a campos ausentes—? [Clarity, Spec §FR-010, Contract §1.2]
- [x] CHK004 ¿El contrato precisa el formato de `quoteDate` y `valuationDate` (ISO `yyyy-MM-dd`, sin hora ni zona) y la nulabilidad diferenciada de cada uno? [Clarity, Contract §1.2, §1.2.1]
- [x] CHK005 ¿Se documentan la escala y precisión de `score` junto con la prohibición de redondeo o transformación? [Clarity, Spec §FR-012, Contract §1.2.1]
- [x] CHK006 ¿El contrato enumera los valores admitidos de `strategy` y su correspondencia con el score devuelto? [Completeness, Spec §FR-004, Contract §1.2]

## Contrato HTTP: Consistencia y compatibilidad

- [x] CHK007 ¿La spec y el contrato coinciden en la ruta, el método, la ausencia de body y la ausencia de query parameters declarados para el ranking por score? [Consistency, Spec §FR-001, Contract §1, §1.1]
- [x] CHK008 ¿Está definido de forma consistente que los query parameters no declarados se ignoran y no alteran la estrategia, la población, el orden ni el contenido del ranking? [Ambiguity, Spec §FR-001, Contract §1.1]
- [x] CHK010 ¿Está definida la política sobre campos adicionales en la respuesta y la forma de detectar una desviación del contrato? [Gap, Contract §1.2, Spec §SC-010]

## Errores y estados vacíos

- [x] CHK011 ¿Están definidos el código HTTP, el `code` y la causa de cada error, distinguiendo explícitamente la ausencia de datos de un fallo? [Completeness, Spec §FR-014, Contract §1.4]
- [x] CHK012 ¿El contrato especifica si ante un fallo puede existir una respuesta parcial o si siempre se devuelve error? [Edge Case, Spec §FR-014, Contract §1.4]

## Seguridad y autorización

- [x] CHK013 ¿La exigencia de JWT válido para la ruta está formulada como requisito verificable y no solo como supuesto heredado? [Gap, Spec §FR-016/§SC-009, Contract §2]
- [x] CHK014 ¿Se especifica si la consulta requiere roles, permisos o autorización por recurso, y si todos los usuarios autenticados obtienen el mismo ranking? [Coverage, Spec §FR-016, Contract §2]
- [x] CHK015 ¿Está definido que la respuesta y los errores no exponen detalles internos —identificadores de valuación, versión de estrategia, trazas o mensajes técnicos—? [Security, Spec §FR-017/§SC-010, Contract §1.2.1, §1.4]
- [x] CHK016 ¿Se documenta que la ruta no recibe entradas externas y que, aun así, conserva el requisito de autenticación? [Consistency, Spec §FR-001, Contract §2]

## Notes

- Checklist generada el 2026-10-23; evaluación asistida solicitada por el revisor el 2026-10-23.
- La evaluación original comprendió 16 criterios: 11 estaban contemplados en `spec.md` y en el contrato, y 5 se corrigieron y re-verificaron el 2026-10-23.
  - `CHK004`: formato `yyyy-MM-dd` sin hora ni zona precisado en el contrato §1.2 y §1.2.1.
  - `CHK010`: esquema de respuesta cerrado y criterio de desviación declarados en el contrato §1.2 y `SC-010`.
  - `CHK013`: JWT válido y `401` incorporados como `FR-016` y `SC-009`; contrato §2 normativo.
  - `CHK014`: ausencia de roles/permisos y mismo ranking para todo usuario autenticado en `FR-016` y contrato §2.
  - `CHK015`: no exposición de datos internos en `FR-017`, contrato §1.2.1, §1.4 y `SC-010`.
- Remediación posterior a Analyze: CHK007 fue reformulado y CHK008 precisó la política de query parameters; ambos fueron reevaluados y aprobados por el revisor.
- CHK009 fue retirado, sin reutilizar su identificador, porque FR-015 y SC-008 se eliminaron por decisión explícita de alcance. El ranking por precio quedó fuera de SPEC-018 y no requiere verificación específica en esta feature.
- La reevaluación de CHK007 y CHK008 quedó completada conforme a `docs/development/sdd-workflow.md` §6.
- `[x]` acredita que el criterio de calidad de requisitos se satisface; no significa implementación completa ni cierre.
- `/speckit.implement` lee el estado de los marcadores como gate y no debe modificarlos.
- `checklists/requirements.md` mantiene su ciclo propio, gestionado por `/speckit.specify` y `/speckit.clarify`.

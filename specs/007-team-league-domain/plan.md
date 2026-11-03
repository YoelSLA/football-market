# Plan — SPEC-007 simplificada

Fuente: [spec.md](spec.md) y la decisión humana registrada allí y en [tasks.md](tasks.md). Solo DEV. Sin migración de datos legacy ni releases intermedios.

## Arquitectura final

PlayerSynchronizationOrchestrator coordina opacamente fetch por FootballDataPlayerService, aplicación por PlayerCatalogService y enriquecimiento por TeamEnrichmentService. Fetch/red antes de escritura. PlayerCatalogService posee una transacción REQUIRES_NEW/READ COMMITTED por intento para League/Team/Player, referencias, casos y actividad/vigencia; replay acotado solo para conflicto UNIQUE reconocido, reconstruyendo entidades desde snapshot. No continuar una transacción fallida.

TeamEnrichmentService consulta TheSportsDB después del commit principal y persiste intento/referencia/caso en una transacción corta independiente por Team. Mantener revalidación de nombre/referencia bajo bloqueo de Team y Retry-After. No coordinación distribuida.

Model preserva invariantes, matching, ranking y consolidación; Repository accede a DB; Integration adapta proveedor. Service no llama Service. Interfaces/implementaciones existentes conservadas. No nuevos componentes por uniformidad.

## Persistencia

Player.team obligatorio, Team.league obligatorio, League derivada. Nombres obligatorios, secuencias internas, referencias con identidad única y dueño inmutable. Team.current explícito. PendingReviewCase deduplicado con JSONB, sujeto/causa/fechas, sin status ni revisión. TeamResolutionAttempt distingue fallo técnico de evaluación válida.

No CatalogTransition, current_revision_id, revisión/resultados/miembros canónicos, backfill, espejo ni serializador JSONB artesanal. Casos no gobiernan elegibilidad de fotos posteriores. No Player persistido sin Team; observación irresoluble de un sujeto nuevo solo genera caso externo.

## Flyway limpio

V1 usuarios; V2–V4 sin modificar (identidad de players, referencias, opcionales, imágenes); V5 crea el esquema definitivo y retira los textos legacy. Sin V5.1/V6. El binario nuevo no se ejecuta sobre el historial aplicado anterior: recrear la base DEV es un paso manual y autorizado por separado. No repair ni compatibilidad productiva inexistente.

## Contratos y consumidores

GET mantiene cuatro campos internos y filtro activo/current; POST mantiene cinco contadores. Frontend ya adaptado a teamName/leagueName, sin nuevo comportamiento visual. Mantener integridad y semántica de imágenes existentes; no borrarlas por retirar mecanismos 007.

## Secuencia

1. Enmienda de spec/plan/research/modelo/tasks/contratos/checklist y trazabilidad.
2. Análisis de coherencia del nuevo alcance, sin tests ni aprobación de gates.
3. Retirar consumidores y componentes canónicos/legacy.
4. Reorganizar DDL para instalación limpia; obligatoriedad Team en Model/JPA/SQL.
5. Adaptar fixtures y tests, conservar regresiones de US01–US05 y 006.
6. Build backend permitido sin tests cuando corresponda; tests exclusivamente por usuario.
7. Recreación local solo con autorización específica; luego startup/sync/consulta por usuario.

## Constitución y validación

Sin modificación constitucional ni tecnológica. KISS/YAGNI, separación existente, DTO/Mapper/seguridad sin cambios de contrato. Pruebas Model puras; Service por APIs públicas con Spring/Repository/DB reales e Integration mock; queries/constraints/migraciones/concurrencia técnica en Repository con Testcontainers. No reflection de métodos privados como prueba de equivalencia SQL. No tests frontend.

Cobertura: identidad/rename/transferencia, asociaciones obligatorias, unicidad/cardinalidad, contradicciones y opcionales, foto incompleta/rollback, retirada/protección/regreso, TheSportsDB/elegibilidad/persistencia @MapsId, GET/POST, aislamiento de tests e instalación limpia V1–V5. Sin pruebas de mecanismos retirados ni gates operacionales legacy. Review/testing/cierre pendientes del humano.

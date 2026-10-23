# Modelo final — Player → Team → League

| Model / tabla | Estado e invariantes |
| --- | --- |
| League / leagues | ID interno por secuencia, name obligatorio. Sin current/temporadas. |
| Team / teams | ID interno por secuencia, name obligatorio, league_id NOT NULL/FK, current explícito. |
| Player / players | ID interno por secuencia, name/position/active existentes, team_id NOT NULL/FK. League derivada; sin textos legacy ni relación directa League. |
| Referencias Player | ID/propietario obligatorio inmutable; provider/external_id obligatorios; UNIQUE(provider,external_id). Varias por proveedor permitidas. |
| Referencias Team/League | Igual identidad única y UNIQUE(owner_id,provider). Alta sync nace con FOOTBALL_DATA. |
| PendingReviewCase / pending_review_cases | ID, category/cause_code, subject_type, subject_id interno cuando existe, provider/externalId para externo, case_key UNIQUE, first/last_detected_at, evidence JSONB. Sin status ni puntero/historial canónico. |
| TeamResolutionAttempt / team_resolution_attempts | team_id PK/FK @MapsId, última llamada/nombre/fallo técnico; última evaluación válida/nombre/resultado y retry_not_before. Persistable distingue alta/actualización. |

Constraints FK restringen relaciones rotas; no cascade de borrado de Team/League histórico. Nombres no son identidad. No nuevas claves de presentación. Índices players.team_id, teams.league_id y sujeto de casos; unicidades respaldadas por DB.

## Casos y evidencia

Categorías: PLAYER_TEAM_UNRESOLVED, TEAM_LEAGUE_UNRESOLVED, EXTERNAL_IDENTITY_CONFLICT, PLAYER_OPTIONAL_CONFLICT, INVALID_SUBJECT_DATA. No LEGACY_TEAM_ASSOCIATION. Clave estable por sujeto/problema/relación, no por nombre ni timestamp; reconciliar sujeto externo reconocido sin reasignar propietarios.

Evidencia guarda datos recibidos/estado previo necesarios, incluidos observaciones contradictorias y referencias. La última detección actualiza evidencia; primera fecha permanece. No promesa de archivo completo de respuestas ni reconstrucción histórica. Un caso no certifica problema vigente ni excluye asociado válido. SQL humano de solo lectura, sin endpoint.

## B-V5-01–05

Mantener cardinalidad, clasificación antes de mutar Player, comparación estricta de Team/name/position, ranking de originales equivalentes y opcionales por atributo. Estas reglas no necesitan columnas de estado derivado, revisiones, resultados/versiones ni miembros.

## Esquema limpio

V1 crea usuarios. V2–V4 no se modifican: players con identidad interna, referencias de Player, atributos opcionales e imágenes. V5 crea el esquema definitivo: retira los textos legacy de equipo y liga, añade leagues, teams, sus referencias, players.team_id obligatorio, pending_review_cases y team_resolution_attempts. Sin tablas de transición/canónicas, sin V5.1/V6 ni triggers de estos mecanismos. Solo instalación limpia tras recreación DEV autorizada; no actualiza el historial aplicado anterior.

# Modelo de datos: Dominio de equipos y ligas

Fuente: [spec.md](spec.md). Diseño técnico; nombres de tablas/campos son contratos internos para [quickstart.md](quickstart.md).

## Entidades e invariantes

| Entidad | Campos y relaciones |
|---|---|
| League | id Long por secuencia; name obligatorio/no vacío; referencias externas. Sin current, temporada ni historial. |
| Team | id Long por secuencia; name obligatorio/no vacío; league ManyToOne obligatoria; current boolean; referencias externas. |
| Player | id y referencias existentes conservados; team ManyToOne nullable únicamente durante transición o caso permitido; active conservado. Sin asociación directa a League. |
| Referencias de Player/Team/League | id por secuencia; propietario obligatorio e inmutable; ExternalProvider STRING; externalId no vacío. UNIQUE(provider,external_id) y UNIQUE(propietario,provider) por tipo. |

Team/League creados por sincronización nacen con referencia FOOTBALL_DATA. Cambios de nombre/liga conservan IDs (RF-009–RF-014). Una referencia existente para su propietario es actualización normal, no conflicto. No borrar físicamente entidades históricas; FK de Player a Team restringe borrado.

League.id no determina presentación. League.name es el nombre actual usado por el frontend para clasificar; nombres desconocidos usan neutral, sin continuidad histórica tras rename. No hay código, slug, clave visual ni mapa por leagueId agregado al modelo para esta finalidad.

## Foto y transición

Records inmutables transportan League/Team/Player externos, relaciones, presencia, anomalías y sujetos protegidos; no entidades JPA entre intentos. Identidad insuficiente que compromete presencias invalida foto. Opcionales ausentes no invalidan (RF-039).

Presentes válidos: referencias FOOTBALL_DATA resuelven Player y Team. Ausentes aún sin team_id: backfill estricto contra Teams vigentes calculados para la foto. Presentes inválidos: asociación/active previos, sin backfill. No asociados permanecen fuera del GET. Retirados históricos: active=false; un caso no equivale a inactividad. Existencia de caso tampoco excluye un Player asociado válido.

## PendingReviewCase: pending_review_cases

| Campo | Regla |
|---|---|
| id | PK por secuencia |
| category, cause_code | Categoría y código estable; descripción opcional no es clave |
| subject_type | PLAYER, TEAM o LEAGUE |
| subject_id | ID interno cuando existe; no inventar ID para nuevas entidades |
| subject_provider, subject_external_id | Referencia del sujeto no creado; guardar además referencia de origen conocida como evidencia |
| case_key | Clave canónica estable UNIQUE, no derivada de descripción libre |
| first_detected_at, last_detected_at | Instants; primera preservada, última actualizada |
| evidence | JSONB con datos tipados por categoría, sin secretos; no se usa para matching automático |

No status, RESOLVED, IGNORED ni cierre automático. Subject_id polimórfico se valida por Service; no se declara una FK imposible entre tres tablas. Evidencia preserva valores observados aunque cambien entidades.

| Categoría | Identidad funcional / evidencia |
|---|---|
| LEGACY_TEAM_ASSOCIATION | Player interno; causa NO_TEAM_MATCH o MULTIPLE_TEAM_MATCHES actualizable. originalTeamName inmutable, candidatos con id/nombre observado (vacío para cero). |
| PLAYER_TEAM_UNRESOLVED | Sujeto Player interno o externo + relación Team problemática; referencia y nombre del Team recibido, causa, asociación previa, texto original cuando aún sin asociación. |
| TEAM_LEAGUE_UNRESOLVED | Sujeto Team interno o externo + relación League problemática; liga/contexto recibidos y liga previa. |
| EXTERNAL_IDENTITY_CONFLICT | Tipo + proveedor + externalId disputado + sujeto pretendido; propietario actual y pretendido. |
| INVALID_SUBJECT_DATA | Sujeto identificable + causa estable de invalidez individual no cubierta por categorías anteriores; evidencia de datos necesarios faltantes. |

Una relación recibida distinta puede ser problema nuevo; cambio de motivo descriptivo no. Para relación sin ID pero sujeto conocido, usar marcador estable de identidad faltante, no nombre como identidad. Una observación sin sujeto estable usa ejecución/contexto, sin fusionarse entre ejecuciones. Si compromete completitud, solo diagnóstico técnico y ninguna fila de esta aplicación.

El reconocimiento de identidad interna posterior reconcilia claves externas previas bajo la misma referencia de origen; no crea duplicado por cambiar representación del sujeto. Constraint UNIQUE y upsert controlado preservan primera detección y evidencia original, actualizando última/evidencia actual; concurrencia no pierde original.

## TeamResolutionAttempt: team_resolution_attempts

PK/FK team_id; last_call_at, last_call_name, technical_result; last_valid_evaluation_at, last_valid_evaluation_name, valid_result nullable; retry_not_before nullable para Retry-After.

Resultados válidos: MATCH, NO_MATCH, AMBIGUOUS, COMPLETENESS_UNPROVEN. Técnicos: TIMEOUT_OR_TRANSPORT, RATE_LIMITED, SERVER_ERROR, INVALID_RESPONSE (incluye 4xx inválido). Solo evaluación válida consume el nombre. Comparar nombres con la normalización estricta; nunca reconsultar equipo con referencia resuelta. Mantener último fallo separado de última evaluación válida. Fallo de persistencia no consume: revierte registro y referencia juntos.

Red externa después del commit principal; revalidar nombre/referencia antes de commit por Team. Resultado obsoleto por cambio concurrente no asigna ni consume evaluación del nuevo nombre. Sin retries internos nuevos; respetar retry_not_before y pacing.

## Migraciones y convivencia

### Release 1: V5__create_team_league_domain.sql

Crear tablas/secuencias, constraints de referencias, casos e intentos. Añadir players.team_id nullable e índice. Mantener players.team/league y NOT NULL. No HTTP en Flyway ni generación de Team por texto.

Código intermedio mapea legacyTeam/legacyLeague a columnas antiguas y team a relación nueva. Para asociados, team es verdad y legacy espejo temporal; nuevas altas rellenan espejo para NOT NULL. Para no asociados, conservar texto, usado solo en backfill/evidencia, nunca GET nuevo. Capturar original necesario antes de modificar espejo. Datos originales no requeridos para revisión no tienen garantía histórica.

Marcador singleton catalog_transition(id=1, completed_at nullable): la primera foto completa aplica asociaciones/casos y marca completado en el mismo commit cuando todo Player tiene asociación válida o caso permitido con evidencia. Si falla, marcador y cambios revierten. Después no repetir backfill desde casos ni cerrar casos automáticamente. Nuevas altas ya nacen asociadas. Las actualizaciones normales válidas pueden resolver asociación sin cambiar el registro histórico de revisión.

### Gate humano

Usuario sincroniza y verifica invariantes, conteos e identidad, evidencia y preparación de backup. No generar/publicar V6 en Release 1. El marcador no sustituye revisión humana.

### Release 2: V6__drop_player_team_league_text.sql

Transacción PostgreSQL: comprobar marcador, cada Player con Team/League válidos o caso LEGACY_TEAM_ASSOCIATION / PLAYER_TEAM_UNRESOLVED permitido con evidencia original; referencias de origen e integridad. Si falla, ninguna eliminación. Eliminar columnas legacy y mapping final; conservar marcador para impedir backfill posterior. Sin DDL no transaccional. No bajar team_id a NOT NULL: los casos permitidos pueden seguir sin asociación.

Procedimiento forward-only y backup en quickstart. V5 y V6 son decisiones de entrega, no consecuencia normativa de RF-011.

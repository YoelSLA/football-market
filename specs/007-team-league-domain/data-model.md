# Modelo de datos: Dominio de equipos y ligas

Fuente: [spec.md](spec.md). Diseño técnico; nombres de tablas/campos son contratos internos para [quickstart.md](quickstart.md).

## Entidades e invariantes

| Entidad | Campos y relaciones |
|---|---|
| League | id Long por secuencia; name obligatorio/no vacío; referencias externas. Sin current, temporada ni historial. |
| Team | id Long por secuencia; name obligatorio/no vacío; league ManyToOne obligatoria; current boolean; referencias externas. |
| Player | id y referencias existentes conservados; team ManyToOne nullable únicamente durante transición o caso permitido; active conservado. Sin asociación directa a League. |
| Referencias de Player | id por secuencia; propietario obligatorio e inmutable; ExternalProvider STRING; externalId no vacío. UNIQUE(provider,external_id); sin UNIQUE(player_id,provider). Varias identidades distintas del mismo proveedor pueden pertenecer al mismo Player. |
| Referencias de Team/League | mismas invariantes de identidad y propietario; UNIQUE(provider,external_id) por tabla y UNIQUE(team_id,provider) / UNIQUE(league_id,provider), respectivamente. |

**B-V5-01 resuelto:** `UNIQUE(provider,external_id)` identifica un propietario único por identidad externa dentro de cada tipo; `UNIQUE(propietario,provider)` restringe la cantidad de referencias del propietario. No son equivalentes. La segunda solo aplica a Team/League. V5 no agrega unicidad por propietario/proveedor a Player ni modifica referencias legacy válidas. El alta repetida de la misma referencia de Player es idempotente; otra identidad del mismo proveedor se añade sin reemplazar las anteriores. La resolución por una referencia existente actualiza a su propietario, nunca lo reasigna.

Team/League creados por sincronización nacen con referencia FOOTBALL_DATA. Cambios de nombre/liga conservan IDs (RF-009–RF-014). Una referencia existente para su propietario es actualización normal, no conflicto. No borrar físicamente entidades históricas; FK de Player a Team restringe borrado.

League.id no determina presentación. League.name es el nombre actual usado por el frontend para clasificar; nombres desconocidos usan neutral, sin continuidad histórica tras rename. No hay código, slug, clave visual ni mapa por leagueId agregado al modelo para esta finalidad.

## Foto y transición

Resolución B-V5-05: opcionales se consolidan por atributo, sin efecto sobre el estado obligatorio. Conjunto de observaciones válidas del mismo Player por propietario: sin valor válido → conservar persistido o null en alta; único valor válido → usarlo; mismo valor repetido → usarlo; valores válidos incompatibles → conservar el persistido del Player existente o null en el nuevo, registrando el conflicto del atributo sin INVALID_SUBJECT_DATA. Ausentes/inválidos no compiten con valores válidos. La resolución de dateOfBirth no condiciona nationality ni a la inversa. GET devuelve el persistido. Sin columnas nuevas ni APIs de producto para pruebas.

Resolución B-V5-04: la clave semántica no se persiste como presentación. Selección pura entre textos originales equivalentes: diacríticos preservados, casing natural/mixto, desempate lexicográfico determinista. En Player existente, cada name/position persistido equivalente se conserva exactamente y no participa en un ranking para sustituirlo cosméticamente. Para significado nuevo o alta sin representación, usar ranking sobre observaciones equivalentes, no orden. GET devuelve el campo persistido sin recalcular. Sin columnas nuevas para normalización ni APIs de producto para pruebas. Los pendientes B-V5-04 anteriores quedan reemplazados por esta política.

B-V5-03 generaliza la inconsistencia por propietario a Team/name/position. Comparación de Team por ID interno, y de name/position mediante normalización estricta existente, sin nuevas reglas. Cualquier contradicción protege integralmente el Player y genera una clasificación INVALID_SUBJECT_DATA con evidencia del conjunto y atributos incompatibles, no una clasificación por atributo ni por referencia. Mantener asociación/active/name/position/espejo y todas las referencias válidas. No nueva bandera permanente en Player, ni uso de casos históricos para bloquear fotos coherentes futuras. El camino coherente aplica B-V5-04 resuelto y ranking de originales descritos arriba; una clave de comparación no es el texto persistido.

B-V5-02: agrupar por Player interno reconocido; múltiples referencias válidas con Teams válidos distintos son sujeto protegido INVALID_SUBJECT_DATA/CONFLICTING_PLAYER_TEAMS. Conservar asociación (también null transitorio), active, referencias y espejo previo; excluir todas sus observaciones antes de escrituras Player, sin backfill ni selección por orden. Evidencia canónica contiene referencias/Teams recibidos y asociación/actividad previas, más originalTeamName si aún no asociado. Clave estable por Player interno y causa, no por orden ni conjunto variable de Teams. Protección efímera por foto; no estado nuevo en Player ni cierre de caso; foto posterior coherente procesa normalmente.

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
| INVALID_SUBJECT_DATA | Sujeto identificable + causa estable de invalidez individual no cubierta por categorías anteriores; evidencia de datos necesarios faltantes o asociaciones contradictorias B-V5-02. |
| PLAYER_OPTIONAL_CONFLICT | Player interno + atributo opcional (dateOfBirth o nationality) con valores válidos incompatibles; valores recibidos y, si existen, los previos. No congela ni invalida al Player. |

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

La excepción delimitada de transición para un Player preexistente sin Team y protegido por
inconsistencia de snapshot también cubre B-V5-03: evidencia original y observaciones/atributos
contradictorios suficientes, sin admitir cualquier INVALID_SUBJECT_DATA genérico. Extender marcador
y lecturas de control junto con la implementación autorizada, sin crear ni aplicar V6.

Para el preexistente aún sin asociación protegido por B-V5-02, INVALID_SUBJECT_DATA con causa CONFLICTING_PLAYER_TEAMS y evidencia original/observaciones contradictorias es caso permitido de transición RF-018/RF-036; no cualquier invalidez genérica. Reflejar esta excepción delimitada en marcador y consultas de control. La futura guarda Release 2 debe conservarla, sin implementar V6 ahora.

Usuario sincroniza y verifica invariantes, conteos e identidad, evidencia y preparación de backup. No generar/publicar V6 en Release 1. El marcador no sustituye revisión humana.

### Release 2: V6__drop_player_team_league_text.sql

Transacción PostgreSQL: comprobar marcador, cada Player con Team/League válidos o caso permitido con evidencia original (LEGACY_TEAM_ASSOCIATION, PLAYER_TEAM_UNRESOLVED o la excepción delimitada B-V5-02 descrita arriba); referencias de origen e integridad. Si falla, ninguna eliminación. Eliminar columnas legacy y mapping final; conservar marcador para impedir backfill posterior. Sin DDL no transaccional. No bajar team_id a NOT NULL: los casos permitidos pueden seguir sin asociación.

Procedimiento forward-only y backup en quickstart. V5 y V6 son decisiones de entrega, no consecuencia normativa de RF-011.

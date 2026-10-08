# Validación y operación: Dominio de equipos y ligas

Guía futura, no evidencia de ejecución. [Spec](spec.md), [modelo](data-model.md), [contratos](contracts/api.md). Tasks generadas; implementación y tests/builds pendientes, sin evidencia de ejecución.

## Preparación y dos releases

Java 21, Node >=24, PostgreSQL y configuración externa existente. Claves fuera del repositorio y salidas. No presumir Premium. Consultar scripts/perfiles efectivos antes de arrancar; el perfil dev habilita Flyway. Arranque habitual desde backend: `./gradlew bootRun --args='--spring.profiles.active=dev'`. Tests backend son exclusivamente del usuario; esta guía no pide ejecutar tests al agente.

1. Registrar IDs de todos los Players y conteo completo en respaldo de control, no únicamente GET (excluye inactivos). Crear backup consistente antes del despliegue según procedimiento PostgreSQL del entorno.
2. Release 1 contiene V5, nunca V6. Desplegar código intermedio con legacy NOT NULL/mappings descritos en modelo. Tras arranque, GET usa solo asociaciones válidas: puede haber menos jugadores hasta transición; no fallback visual por texto del DTO antiguo.
3. Usuario invoca POST /api/players/sync con JWT. Foto incompleta o fallo técnico principal dejan estado previo. Verificar transición y SQL abajo. Repetir solo tras corregir causa si falla.
4. Gate usuario: ningún Player perdido/duplicado, IDs preservados, asociaciones/referencias válidas, casos permitidos con evidencia, marcador completado. Verificar también nuevos Players legítimos: conteo mayor no demuestra por sí solo ausencia de pérdidas.

B-V5-01: comparar todas las referencias de Player antes/después, incluidos sus IDs y propietarios; varias del mismo proveedor con externalId distintos son válidas y deben conservarse. V5 no añade UNIQUE(player_id,provider), no selecciona referencias ni exige regularización externa previa. Se mantiene UNIQUE(provider,external_id); Team/League sí tienen además unicidad por propietario/proveedor. Ninguna comprobación de conteos autoriza eliminar referencias.
5. Antes de Release 2: detener instancias antiguas/escrituras/sincronizaciones; backup consistente inmediatamente previo a V6 y procedimiento de recuperación preparado; comprobar binario final sin mappings legacy. No considerar marcador aprobación humana automática.
6. Release 2 publica V6 y código final. V6 valida condiciones dentro de transacción PostgreSQL y elimina columnas. Si falla guarda o DDL transaccional, esquema anterior intacto; no forzar historial Flyway. Mantener servicio final fuera de tráfico hasta migración/validación correctas.

## Recuperación

- Antes del commit principal: foto fallida sin cambios; aplicación fallida rollback total, salvo huecos de secuencias. Recuperación preferida hacia adelante y reintento.
- Tras commit antes de V6: dominio/casos/textos disponibles, pero actualizaciones de negocio ya confirmadas. Rollback de binario solo tras comprobar compatibilidad de espejo y esquema; no equivale a recuperar estado original.
- V6 fallida: rollback transaccional; corregir causa con procedimiento Flyway del entorno, sin editar migraciones aplicadas ni hacer repair a ciegas.
- V6 confirmada: forward-only ordinario. No downgrade directo a binario que requiere textos. Emergencia: restaurar respaldo y binario compatible, o migración compensatoria diseñada. Nombres actuales no recuperan valores originales. Documentar punto de recuperación y tratamiento de escrituras posteriores; restaurar puede perderlas.
- Backup: operador de base documenta herramienta aprobada (p. ej. pg_dump consistente o respaldo del servicio), ubicación protegida, retención, restauración aislada y comprobación. No publicar credenciales ni recomendar cuenta general de aplicación. No se declara backup realizado por esta guía.

## Consulta humana SQL de revisión

Operador autorizado usa rol separado de solo lectura sobre pending_review_cases y únicamente tablas auxiliares necesarias. DBA concede CONNECT/USAGE/SELECT acotados conforme despliegue; sin INSERT/UPDATE/DELETE/DDL ni credenciales generales de aplicación. No nuevo endpoint, frontend o comando. Repository no es mecanismo humano.

Las consultas siguientes corresponden al esquema diseñado; requieren implementación antes de poder utilizarse. evidence es JSONB; no contiene secretos. Son consultas de lectura, no instrucciones para resolver casos.

```sql
SELECT id, category, cause_code, subject_type, subject_id,
       subject_provider, subject_external_id,
       first_detected_at, last_detected_at, evidence
FROM pending_review_cases
ORDER BY last_detected_at DESC, id DESC
LIMIT 100 OFFSET 0;

SELECT id, subject_id, cause_code,
       evidence->>'originalTeamName' AS original_team,
       evidence->'candidateTeams' AS observed_candidates
FROM pending_review_cases
WHERE category = 'LEGACY_TEAM_ASSOCIATION'
ORDER BY id;

SELECT id, category, cause_code, subject_type, subject_id,
       subject_provider, subject_external_id, evidence
FROM pending_review_cases
WHERE category IN ('PLAYER_TEAM_UNRESOLVED', 'TEAM_LEAGUE_UNRESOLVED',
                   'EXTERNAL_IDENTITY_CONFLICT', 'PLAYER_OPTIONAL_CONFLICT',
                   'INVALID_SUBJECT_DATA')
ORDER BY id;
```

Los conflictos opcionales de B-V5-05 se consultan por atributo y valores recibidos; no impiden el procesamiento del Player ni completan su asociación:

```sql
SELECT id, subject_id, cause_code,
       evidence->>'attribute' AS attribute,
       evidence->'receivedValues' AS received_values
FROM pending_review_cases
WHERE category = 'PLAYER_OPTIONAL_CONFLICT'
ORDER BY last_detected_at DESC, id DESC;
```

Revisar propietario actual/pretendido, relación recibida/previa y texto original según categoría. Primera/última detección no certifican que problema siga presente. Sin cierre automático/manual en esta feature. Una nueva detección del mismo problema actualiza fila; no crea historial de cada ejecución.

## Lecturas de control del gate

```sql
SELECT completed_at FROM catalog_transition WHERE id = 1;

SELECT p.id
FROM players p
LEFT JOIN teams t ON t.id = p.team_id
LEFT JOIN leagues l ON l.id = t.league_id
WHERE p.team_id IS NOT NULL
  AND (t.id IS NULL OR l.id IS NULL);

SELECT p.id
FROM players p
WHERE p.team_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM pending_review_cases c
    WHERE c.subject_type = 'PLAYER' AND c.subject_id = p.id
      AND c.category IN ('LEGACY_TEAM_ASSOCIATION', 'PLAYER_TEAM_UNRESOLVED')
      AND nullif(btrim(c.evidence->>'originalTeamName'), '') IS NOT NULL
  )
  AND NOT EXISTS (
    SELECT 1 FROM pending_review_cases c
    WHERE c.subject_type = 'PLAYER' AND c.subject_id = p.id
      AND c.category = 'INVALID_SUBJECT_DATA'
      AND c.cause_code IN ('CONFLICTING_PLAYER_TEAMS', 'CONFLICTING_PLAYER_STATE')
      AND nullif(btrim(c.evidence->>'originalTeamName'), '') IS NOT NULL
      AND jsonb_array_length(c.evidence->'observations') > 1
  );
```

Últimas dos consultas deben no devolver filas. Revisar además identidad completa previa vs posterior, referencias FOOTBALL_DATA de Team/League, unicidad y evidencia candidatos. No eliminar textos solo por conteos o existencia de cualquier caso.

## Escenarios funcionales (usuario, fixtures para externos)

**B-V5-05:** comprobar alta sin opcionales válidos; un valor válido frente a otro ausente o inválido; mismo valor válido repetido; valores incompatibles con Player nuevo (null) y existente (valor persistido); conflicto de un atributo con el otro coherente, en ambos sentidos; ausencia de INVALID_SUBJECT_DATA y procesamiento completo de Team/name/position; independencia del orden de referencias; GET idéntico al persistido; y foto posterior coherente que completa el opcional según las reglas existentes. Sustituye el pendiente B-V5-05 anterior.

**B-V5-04 resuelto:** comprobar alta con una representación, José Pérez/JOSE PEREZ, casing mixto, varias formas equivalentes, permutaciones y desempate lexicográfico. El seleccionado pertenece a las observaciones originales. Tras persistir, fotos equivalentes conservan exactamente name/position anteriores, incluso si llega una forma de mayor calidad; GET los devuelve sin normalizar. Contradicciones semánticas siguen protegiendo según B-V5-03. Reemplaza pendientes B-V5-04 anteriores.

**B-V5-03:** mismo propietario con Team/name/position coherentes procesa normalmente; name o position incompatibles después de normalización estricta existente protegen todo el Player como INVALID_SUBJECT_DATA. Conflictos simultáneos producen una clasificación consistente, sin actualización parcial. Verificar conservación integral, todas las referencias, orden invertido, otros Players procesados y posterior foto coherente. Las formas textuales equivalentes no son conflictos. Su representación persistida/GET sigue B-V5-04 y el ranking aprobado del escenario anterior; esta guía no acredita su ejecución.

**B-V5-02:** preparar un Player con varias referencias FOOTBALL_DATA válidas. Mismo Team permite procesamiento normal; Teams válidos distintos conservan asociación/active/referencias/espejo y registran INVALID_SUBJECT_DATA/CONFLICTING_PLAYER_TEAMS, sin impedir a otros Players válidos. Repetir con referencias en orden inverso: mismo resultado/evidencia canónica. Foto posterior coherente procesa normalmente; el caso histórico no certifica invalidez vigente. Para preexistente aún sin Team conservar originalTeamName y observaciones como caso delimitado permitido, sin backfill.

1. **Identidad/consulta:** dos sincronizaciones conservan IDs de Team/League; GET expone cuatro campos y no referencias/textos. Transferencia de Player cambia asociación por identidad; renombrado/liga de Team conserva ID (RF-001–RF-014/RF-019/RF-028/RF-029; SC-001–SC-006/SC-011).
2. **Tres caminos:** presente válido contradice legacy y usa foto; ausente sin equipo recibe backfill exacto Arsenal/ARSENAL; Arsenal FC no coincide, cero/varios conservan evidencia; presente inválido mantiene team_id/active, sin fallback. Ningún Player perdido/duplicado; asociados previos no se remigran (RF-015–RF-018/RF-034/RF-036; SC-009/SC-010).
3. **Presencia/retirada:** inválidos identificables y dependientes protegidos no se inactivan por ausencia; Team ausente confirmado sale y jugadores se conservan; regreso válido reactiva. Liga fallida, respuesta faltante o identidad insuficiente no aplican foto (RF-025–RF-027/RF-032/RF-033/RF-039; SC-008).
4. **Atomicidad:** error de persistencia tras varias escrituras deja dominio/casos/active/current anteriores; no llamar TheSportsDB sin commit. Conflicto reconocido reaplica foto tras rollback. Fallo corto de enriquecimiento revierte solo resultado de Team (RF-024/RF-037/RF-038; SC-005/SC-006).
5. **TheSportsDB:** match con completitud demostrada en fixture asigna; dos identidades, cero o completitud desconocida no asignan. Orden no cambia resultado; duplicados de identidad coherentes no son ambigüedad. Timeout/429/5xx/4xx/estructura inválida permiten siguiente ejecución, respuesta válida consume nombre. Retry-After se respeta. Sin referencia catálogo válido; no imágenes (RF-020–RF-023/RF-030/RF-035/RF-038; SC-007/SC-012).
6. **Casos:** repetir problema estable actualiza fechas/evidencia sin duplicar; motivo legacy puede pasar cero↔varios sin perder original; externo luego interno se reconcilia. No status ni certificación de vigencia; SQL rol limitado entrega todos los tipos, incluidos sujetos no creados (RF-031/RF-036; SC-010).
7. **V5/V6:** nuevas altas respetan NOT NULL legacy; espejo no gobierna identidad. Gate fallido impide V6 y no elimina columnas; gate satisfecho permite binario final con ddl-auto: validate (RF-006/RF-017/RF-019/RF-036).
8. **HTTP/frontend:** vacío content=[]/totales cero; JWT/400 existentes; pendientes fuera o conservados según asociación válida, nunca error por caso. POST cinco campos originales. Tipos frontend consumen nuevos nombres. Para nombre de liga conocido, clasificación existente; para nombre desconocido tras rename, neutral aunque el cliente nunca haya visto el nombre anterior. leagueId no cambia y leagueName se actualiza; no se espera continuidad visual histórica ni mapa por ID. GET no agrega campos de clasificación. CHK014 se satisface por este alcance acordado.

## Dependencia operacional TheSportsDB

[Documentación](https://www.thesportsdb.com/documentation): Free/Premium tienen límites diferentes; no suponer clave Premium ni completitud por devolver una fila. Mientras searchteams.php no acredite completitud relevante, resultado COMPLETENESS_UNPROVEN sin referencia. Es evaluación válida distinguida de NO_MATCH/AMBIGUOUS y no incumple RF-022. Un cambio de nivel sin nombre nuevo no habilita automáticamente reintento bajo RF-035; esa ampliación no se incluye.

## Procedimiento operativo Release 1 (V5)

1. Verificar binario final compatible: el despliegue de Release 1 incluye solo V5 y mappings intermedios. Comprobar que el binario arranca contra el esquema ya migrado antes de recibir tráfico; no prometer reversión directa por un cambio aditivo.
2. Exclusión de escritores antiguos: detener instancias que no incluyan el modelo Team/League y ninguna sincronización ni job de imágenes concurrente antes de aplicar V5 y la primera foto.
3. Backup consistente inmediato previo a aplicar V5, con punto de recuperación registrado y procedimiento de restauración aislado ensayado. Esta guía no declara backup realizado.
4. Aplicación de V5: un único despliegue Flyway. Si falla, esquema anterior intacto por DDL transaccional de PostgreSQL; no forzar historial con repair a ciegas ni editar migraciones aplicadas.
5. Primera sincronización completa y verificaciones del gate (marcador, asociaciones, casos, conteos e IDs) antes de considerar Release 1 confirmado.
6. Cualquier fallo posterior es forward-only. No existe downgrade automático a binario previo ni restauración de textos eliminados en Release 2 sin backup explícito.

## Controles pendientes

Solo tras implementación y cuando corresponda: build backend `./gradlew build -x test`, frontend `npm run build`; tests backend específicos según plan a cargo del usuario. No ejecutar esos comandos en esta revisión exclusivamente documental ni marcar controles realizados.

# SPEC-007 — Dominio de equipos y ligas

**Branch**: `feat/007-team-league-domain` · **Created**: 2026-10-02
**Status**: DONE — cerrada por autorización humana el 2026-10-11

## Decisión humana de alcance — 2026-10-10

El proyecto existe exclusivamente en DEV, sin producción ni staging. El usuario acepta recrear únicamente la base PostgreSQL local dedicada al proyecto y autoriza implementar directamente Player → Team → League. La recreación NO está autorizada en este encargo.

Se retira US06 (migrar catálogo legacy), sin reutilizar su identificador. Se sustituyen las decisiones de dos releases, backfill, espejo legacy, marcador, revisión/materialización canónica, readiness histórico, resultados versionados y sus protocolos/triggers. US01–US05 permanecen. La decisión queda registrada en esta sección, en [research.md](research.md) y en [tasks.md](tasks.md), sin documentos históricos adicionales.

La autorización humana abarca actualización SDD y simplificación de código/tests/migraciones para instalación limpia. No aprueba CODE REVIEW, TESTING, cierre ni ejecución sobre PostgreSQL. Las validaciones del diseño anterior no acreditan el nuevo alcance.

## Objetivo

Team y League tienen identidades internas independientes de proveedores. Player pertenece a exactamente un Team; su League deriva exclusivamente de ese Team. La sincronización manual mantiene identidades, vigencia y referencias sin duplicados ni pérdida de entidades históricas. No se crean Players ni Teams con asociaciones irresolubles.

## Historias y aceptación

### US01 — Reconocer equipo y liga (P1)

Como consumidor, quiero identidades internas coherentes y una consulta local.

1. GET expone teamId, teamName, leagueId y leagueName obligatorios, derivados de las entidades.
2. Solo devuelve Players activos con Team vigente y League válida; no expone team/league legacy ni referencias externas Team/League.
3. Responde normalmente con proveedores indisponibles. Caso pendiente sobre una entidad válida no la excluye ni provoca error.
4. Conserva paginación, orden por Player.id, JWT, errores y opcionales existentes. Frontend usa teamName/leagueName, clasificación por nombre y fallback neutral; rename no promete continuidad visual.

### US02 — Sincronizar ligas, equipos y jugadores (P1)

Como usuario autenticado, quiero sincronizar sin duplicar identidades.

1. Se obtiene toda la foto requerida antes de escribir; se reconocen League y Team por sus referencias antes de Player.
2. Una foto completa se aplica con un único commit; fallo técnico revierte entidades, referencias, casos y actividad/vigencia.
3. Player identificable con Team irresoluble no se crea ni actualiza; el existente conserva íntegramente asociación y actividad, se registra y protege de ausencia. Team con League irresoluble no se crea ni actualiza y se registra cuando la foto sigue siendo aplicable.
4. Identidad insuficiente que impide conocer presencias o respuesta requerida incompleta aborta la foto sin escrituras. Inválidos identificables permiten procesar los demás.
5. Referencias repetidas conservan propietario e identidad; una transferencia válida actualiza Player→Team.
6. Player admite varias referencias distintas del mismo proveedor. Observaciones del mismo propietario con Teams/name/position contradictorios protegen todo el Player, independientemente del orden. Una foto posterior coherente vuelve al procesamiento normal; el caso histórico no lo bloquea.

### US03 — Conservar identidad ante cambios (P1)

Como usuario, quiero que renombrados y cambios de liga no creen entidades nuevas.

1. Renombrar Team o League conserva su ID y actualiza nombre actual.
2. Team cambia de League conservando ID; sus Players siguen asociados al mismo Team.
3. Identidad externa de otro propietario nunca se reasigna; se registra conflicto y continúan los sujetos válidos.
4. Los datos de Player coherentes respetan las decisiones de presentación y opcionales descritas abajo.

### US04 — Resolver TheSportsDB sin bloquear catálogo (P2)

Como usuario, quiero referencias Team TheSportsDB confiables y opcionales.

1. Solo exactamente una identidad Soccer con nombre principal estrictamente equivalente y completitud acreditada permite asignación. No fuzzy, sufijos, aliases ni selección del primer resultado.
2. Cero/múltiples/completitud no acreditada no asignan; Team y Players siguen válidos. No se presume Premium ni completitud por cantidad de filas.
3. Referencia conocida no se reevalúa, sustituye ni reasigna.
4. Red después del commit principal, sin escritura abierta; intento y referencia/caso se confirman juntos por Team. Fallo de enriquecimiento no revierte ni convierte en fallido el catálogo confirmado.
5. Una tentativa lógica por Team/ejecución; evaluación válida consume nombre normalizado; fallo técnico permite otra sincronización, respetando Retry-After y pacing existente.

### US05 — Retirada y regreso sin borrado (P2)

Como usuario, quiero conservar equipos y jugadores históricos.

1. Team ausente de una foto completa sale del catálogo conservando identidad/referencias. Players ausentes se inactivan, salvo protecciones.
2. Team/Player inválido identificable conserva estado previo; Team con plantel indeterminable protege sus Players previos.
3. Team válido que reaparece vuelve a vigente; solo Players válidos presentes se reactivan. No se activa todo el historial por inferencia.
4. Player protegido puede conservar active, pero GET nunca lo expone con Team no vigente. Cambiar configuración no modifica vigencia por sí solo.

### US06 — RETIRADA, no entregada

La obligación de migrar datos textuales existentes se retira por decisión humana del 2026-10-10. No se exige conservar el catálogo DEV anterior, ejecutar backfill, certificar originales legacy ni desplegar releases intermedios. US06 **no se entrega ni se cierra como DONE**: se retira del alcance, conservando su identificador y su tarjeta como trazabilidad histórica. Su equivalente funcional quedó cubierto por la instalación limpia de V5 sobre base recreada, conforme a Constitution 6.1.0 §2.1.

## Requisitos funcionales (IDs conservados)

| ID | Contrato vigente |
| --- | --- |
| RF-001 | Team y League con identidad interna propia asignada por FootballMarket. |
| RF-002 | Dominio no limitado estructuralmente a cinco ligas. |
| RF-003 | Ligas configurables; inicialmente PL, BL1, PD, SA, FL1. |
| RF-004 | Todo Player persistido tiene exactamente un Team obligatorio. Se retira la excepción legacy sin asociación. |
| RF-005 | League de Player deriva exclusivamente de Team, sin relación independiente. |
| RF-006 | No hay columnas, mappings, espejos ni fallback de texto de equipo/liga en Player. |
| RF-007 | Team pertenece a una League actual obligatoria. |
| RF-008 | Sin temporadas ni historial de ascensos/descensos. |
| RF-009 | Cambio de liga conserva identidad Team y asociaciones Player. |
| RF-010 | Player admite varias referencias distintas por proveedor; Team/League una por proveedor. |
| RF-011 | Team/League incorporados por sync nacen con referencia FOOTBALL_DATA. |
| RF-012 | UNIQUE(provider,externalId) por tipo y propietario inmutable; ID externo no es identidad pública. |
| RF-013 | Rename conserva ID interno y actualiza nombre actual. |
| RF-014 | Resolver League y Team antes de asociar/actualizar Player. |
| RF-015 | Player identificable irresoluble no se crea/actualiza; existente conserva estado y se protege; caso persistido. |
| RF-016 | Team sin League resoluble no se crea/actualiza; caso cuando la foto sea aplicable. |
| RF-017 | RETIRADO: migración de datos legacy. Conservación normal de entidades sigue RF-025–027. |
| RF-018 | No forzar asociaciones. Caso sobre Player asociado no quita asociación ni excluye por sí solo. Sin resolución manual ni cierre automático en 007; se retira excepción de Player legacy sin Team. |
| RF-019 | GET expone únicamente los cuatro campos derivados nuevos; adaptar consumidores en 007. |
| RF-020 | Team puede conservar THE_SPORTS_DB; ausencia no invalida. |
| RF-021 | Matching estricto: trim, colapso de espacios, case/diacríticos ignorados; Soccer/nombre principal, identidades distintas y completitud acreditada. |
| RF-022 | Falta TheSportsDB no bloquea catálogo ni Players. |
| RF-023 | Referencia Team TheSportsDB conocida no se sustituye/reasigna. |
| RF-024 | Conflicto externo Player/Team/League se registra sin reasignación; continúan casos válidos. |
| RF-025 | Team retirado conserva identidad/referencias; no borrado físico. |
| RF-026 | Players se conservan y desactivan según presencia/protección; GET exige Team vigente. |
| RF-027 | Regreso válido conserva identidad/referencias y reactiva según presencia válida. |
| RF-028 | GET local: Player activo + Team asociado vigente + League válida. |
| RF-029 | No exponer referencias/proveedores Team/League en GET. |
| RF-030 | No obtener/resolver/modificar imágenes; conservar comportamiento existente de 006. |
| RF-031 | Casos persistidos deduplicados, categoría/causa estable, sujeto, evidencia, primera/última detección, sin secretos/status/cierre. Consulta humana SQL de solo lectura. No historial canónico. |
| RF-032 | Foto incompleta no se aplica; fallo técnico local revierte todo el intento. |
| RF-033 | Team.current explícito: presente válido entra, ausente no protegido sale, protegido conserva estado. League sin current. |
| RF-034 | RETIRADO: backfill legacy. |
| RF-035 | Elegibilidad TheSportsDB por evaluación válida/nombre, resultado técnico separado y Retry-After. Timeout/transporte/429/5xx/4xx inválido/estructura inválida no consumen evaluación. |
| RF-036 | RETIRADO: certificación de transición antes de retirar textos. |
| RF-037 | Foto inmutable previa; commit único de dominio/referencias/casos/actividad. Replay acotado solo ante UNIQUE de identidad reconocida, con rollback y nueva transacción. Sin backfill/marcador. |
| RF-038 | Enriquecimiento posterior, independiente y atómico por Team; red fuera de escritura. |
| RF-039 | Distinguir presentes/procesables/protegidos. Inválido identificable conserva estado; identidad insuficiente o respuesta requerida incompleta aborta foto. Opcionales ausentes no invalidan. |

## Decisiones funcionales conservadas B-V5-01–05

Los IDs históricos se conservan aunque ya no denominen una migración operativa.

- B-V5-01: Player mantiene cardinalidad múltiple por proveedor; no UNIQUE(player_id,provider).
- B-V5-02/03: contradicción Team por ID interno, name/position por normalización estricta protege integralmente Player y todas sus referencias; otros sujetos continúan. Se recalcula por foto, no por casos históricos.
- B-V5-04: existente conserva name/position persistidos equivalentes. Alta o significado nuevo selecciona un original equivalente por diacríticos preservados, casing natural y desempate lexicográfico; sin elegir por orden ni normalizar GET.
- B-V5-05: dateOfBirth/nationality independientes. Sin válido conserva previo/null; único válido se usa; incompatibles conservan previo/null y registran conflicto opcional sin invalidar Player.

## Criterios de éxito

SC-001–008, SC-011–012 permanecen: asociación única/derivada, identidad y referencias estables, renombrado/cambio de liga sin duplicación, altas sin asociaciones inválidas, conflictos sin reasignación, TheSportsDB opcional, retirada/regreso sin borrado, contrato nuevo y ausencia de comportamiento nuevo de imágenes.

SC-009 y SC-010 se RETIRAN exclusivamente como criterios de migración legacy (conteos anteriores, casos con texto original de transición). No se elimina la conservación de entidades en sincronizaciones normales.

## Requisitos no funcionales y límites

PostgreSQL/Flyway y JPA con validate, consulta local paginada, JWT existente, timeouts/pacing existentes y atomicidad. Sin SLA nuevo, alta disponibilidad ni coordinación distribuida. Instancia única de sincronización; constraints DB siguen protegiendo identidad frente a carreras.

Fuera de alcance: migración de datos DEV antiguos, temporadas, fuzzy/aliases, imágenes nuevas, referencia TheSportsDB League, UI/endpoint/comando de revisión, cierre de casos, auditoría histórica canónica y despliegues productivos. La recreación local requiere autorización específica y nunca afecta bases de otros proyectos.

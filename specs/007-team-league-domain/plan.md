# Implementation Plan: Dominio de equipos y ligas

**Branch actual**: `007-team-league-domain` | **Fecha**: 2026-10-06 | **Spec**: [spec.md](spec.md)

## Summary

Identidades internas Team/League, Player asociado a Team y liga derivada. Foto Football-Data completa antes de escritura; un único commit principal. Backfill solo para ausentes sin asociación, casos deduplicados consultables por SQL y enriquecimiento TheSportsDB independiente posterior. Dos releases con gate humano entre V5 y V6; no generar tasks en esta revisión.

## Technical Context

- **Lenguajes:** Java 21; TypeScript 7.0.2/React 19.3.0.
- **Dependencias:** Spring Boot 4.1.1, Web/RestClient, JPA/Hibernate, Security, Flyway, PostgreSQL/pgJDBC, SpringDoc; frontend Vite y herramientas existentes. Sin dependencias nuevas.
- **Plataforma:** backend Linux y SPA navegador.
- **Persistencia:** PostgreSQL, Flyway y ddl-auto: validate; open-in-view desactivado.
- **Verificación prevista:** categorías backend existentes, PostgreSQL Testcontainers y REST Docs a ejecutar por usuario; sin tests frontend. Esta tarea documental no ejecuta tests ni builds.
- **Rendimiento/escala:** paginación existente size<=100; ligas configurables, sin asumir cinco; pacing TheSportsDB existente y Retry-After. Sin SLA nuevo.

## Constitution Check

Evaluación inicial y posterior al diseño: responsabilidades existentes Controller→Mapper→Service/Orchestrator, Model para invariantes/comparación, Repository para persistencia, Integration para contratos externos. DTO record, Mapper final/static, Service interfaz/implementación; no dependencias nuevas ni modificación normativa.

Transacciones y secretos respetan constitución; SQL humano usa mínimo privilegio. No claims de controles ejecutados. Scope incluye consumidores frontend, no spec 005. Complexity Tracking no justifica violaciones: decisiones técnicas abajo no son excepciones normativas.

**Advertencia preexistente:** nombre actual de rama sin prefijo type no cumple §7; no renombrarla automáticamente ni crear otra spec. No es una decisión funcional pendiente de la feature. La revisión cruzada final satisface los gates de diseño y alcance; no acredita implementación ni ejecución de controles.

## Project Structure

```text
specs/007-team-league-domain/
  spec.md research.md plan.md data-model.md quickstart.md
  contracts/api.md checklists/requirements-quality.md
backend/src/main/java/footballmarket/
  models/ League Team Player y referencias externas
          PendingReviewCase TeamResolutionAttempt TeamNameNormalizer
          enums/ExternalProvider
          records/ foto inmutable y resultados
  repositories/ entidades, referencias, casos e intentos
  services/ interfaces e impl de catálogo, transición y enriquecimiento
  integrations/FootballDataIntegration TheSportsDbIntegration
  orchestrators/PlayerSynchronizationOrchestrator
  controllers/ DTO/Mapper/Controller existentes
backend/src/main/resources/db/migration/
  V5__create_team_league_domain.sql       # Release 1
  V6__drop_player_team_league_text.sql   # solo Release 2
frontend/src/features/players/
  types/dtos.ts types/models.ts players.mapper.ts
  components/PlayerCard/PlayerCard.tsx constants/playerClassifications.ts
```

No crear servicios por uniformidad. Orchestrator solo transporta resultados opacos entre Services; no inspecciona modelos, usa Repository ni decide elegibilidad. Coordinación de aplicación/transición corresponde al Service de catálogo; enriquecimiento independiente justifica Service propio. Los contratos internos necesarios se precisan en data-model.

## Diseño principal

### Obtención y validación

FootballDataIntegration adapta contratos externos (competición, lista de equipos, planteles), preserva identidades/contexto y anomalías. Service consolida records inmutables de presentes/procesables/protegidos antes de abrir escritura. Ninguna respuesta requerida faltante ni identidad insuficiente que comprometa presencia permite aplicar foto. Opcionales ausentes mantienen reglas existentes. Lista vacía solo es completa si es respuesta válida del contrato, no sustituto de payload faltante (RF-039).

Para sujetos identificables inválidos registrar categoría apropiada y protección; para Team cuyo plantel no puede determinarse por invalidez individual proteger jugadores previos. Fallo de HTTP de plantel requerido invalida toda foto, no se confunde con invalidez individual. No inferir identidad por nombre.

### Commit principal

En un intento transaccional: resolver League/referencias; Team/referencias/liga; Player/referencias/equipo; backfill de ausentes sin asociación solo si transición no finalizada; casos/upsert; vigencia y actividad con protecciones; marcador de transición. Sin red.

Team válido presente current=true; ausente no protegido current=false. Player válido presente activa según reglas normales; ausente se inactiva conforme catálogo existente salvo protección; Team retirado arrastra jugadores salvo Player protegido que debe conservar active. El GET excluye Team no vigente aunque Player protegido siga activo. Backfill no altera active por sí mismo.

Foto inmutable se reaplica solo ante conflicto de identidad reconocido: rollback completo, exclusión/protección nueva, caso registrado en intento válido. Clasificación por SQLSTATE 23505/tabla/constraint, no catch general. Contadores reiniciados por intento; commit final único, sin reutilizar entidades JPA fallidas. Otros fallos técnicos revierten y se propagan. Serialización existente por instancia se conserva; multinstancia exige coordinación antes de habilitarse, no se presume soporte nuevo.

### TheSportsDB posterior

Tras commit, Service selecciona Team vigente sin referencia y sin evaluación válida para nombre actual, respetando retry_not_before. Red fuera de escritura, una tentativa lógica, sin retry interno nuevo. Adaptador devuelve contrato propio de integración con candidatos y evidencia de completitud, no entidades ni DTO HTTP.

Matcher puro filtra Soccer/nombre principal estricto y deduplica por id externo; duplicados contradictorios son respuesta inválida. Una coincidencia solo asigna con completitud acreditada. searchteams.php no tiene garantía acreditada en documentación revisada: resultado COMPLETENESS_UNPROVEN sin referencia. No inventar bandera de completitud a partir de Premium/cantidad de filas.

Timeout/transporte/429/5xx/4xx inválido/estructura inválida registran fallo técnico sin consumir evaluación. Respuesta válida consume incluso si completitud no acreditada, cero o varios candidatos. Retry-After persiste espera. Por equipo, transacción corta revalida nombre y referencia, guarda intento + referencia/caso de conflicto juntos. Errores de enriquecimiento se diagnostican y no convierten principal confirmado en fallo. Fallo de escritura corta revierte solo ese resultado; otros equipos pueden quedar enriquecidos.

### Casos y acceso

Deduplicación por clave funcional UNIQUE descrita en data-model; reconcilia sujeto externo a interno sin duplicar. Primera fecha/original legacy inmutables; última fecha/evidencia actual cambian. Sin status ni cierre; una fila no certifica problema vigente. SQL autorizado es capacidad humana; repositorios son internos. Sin endpoint ni contador HTTP.

### Dos releases

Release 1 incluye solo V5 y mappings intermedios; legacy NOT NULL se rellena en altas con nombre de entidades. Asociado usa relación como verdad, espejo legacy solo compatibilidad; no asociado usa texto solo para backfill/evidencia. Capturar original antes de sobrescribirlo en casos. GET nuevo nunca cae a texto. Compatibilidad de binario previo debe comprobarse, no prometer rollback directo por cambio aditivo.

Usuario obtiene foto/sincroniza/verifica invariantes y marcador. Release 2, solo después del gate, publica V6 y código sin mappings legacy. V6 transaccional valida cada asociación o caso permitido con evidencia y elimina columnas. Guardas y marcador no sustituyen gate operativo de conteos/identidades/backup. Forward-only después; sin DDL en caliente ni remoto desde Flyway.

### HTTP y frontend

GET según contracts/api.md; POST conserva cinco contadores de jugadores. Actualizar OpenAPI y fuentes REST Docs, no snippets generados. Adaptar DTO/modelo/mapper/tarjetas frontend sin modificar 005. Clasificación por leagueName con fallback neutral existente: rename desconocido puede cambiar el estilo, sin continuidad histórica garantizada, mapas por leagueId ni campo HTTP adicional. PlayerIdentityMatcher mantiene semántica de imágenes y lee Team.name; proteger selección de jugadores sin Team para no introducir null dereference.

## Estrategia de validación futura

| Responsabilidad | Cobertura prevista (no ejecutada) |
|---|---|
| Model | RF-001–RF-013, normalización, dos identidades vs duplicados, vigencia y casos |
| Integration | RF-021/RF-035/RF-039: campos, completos vs truncados/desconocidos, errores, Retry-After |
| Service | RF-014–RF-018/RF-024–RF-027/RF-031–RF-039: tres caminos, transferencias, protecciones, idempotencia, único commit, enriquecimiento separado |
| Persistencia | V5 con NOT NULL/altas, upsert casos, UNIQUE propietario/proveedor y proveedor/id, rollback principal y corto, marcador, guarda y rollback V6 |
| HTTP | RF-019/RF-028/RF-029: cuatro campos obligatorios, ausencia team/league, filtro completo, página vacía, JWT/400 existentes, POST sin campos adicionales |
| Operación | Gate entre releases, consultas SQL con rol limitado, backup/restauración y compatibility de binarios |
| Frontend | Build/manual por usuario cuando exista implementación; no infraestructura ni tests nuevos |

SC-001–SC-012 se cubren con consulta, identidad, transferencias, conflictos, ausencia/regreso, backfill y revisión. Referencias completas en quickstart. Tests reales y builds quedan pendientes de implementación/usuario.

Pruebas Service con @SpringBootTest, perfil test, Service/Repository/DB reales e Integration sustituida: Arrange/Act/Assert funcionales solo mediante API pública de Service, sin acceso directo a Repository/SQL. Constraints, guardas y atomicidad técnica se verifican en categorías de persistencia apropiadas; SQL operativo de quickstart no habilita eludir esa frontera. Mapper se cubre por Controller, no suite dedicada. Providers controlados sin Internet; tiempo y concurrencia controlados.

## Complexity Tracking

No violaciones autorizadas. Dos releases: decisión técnica aprobada que reduce complejidad frente tabla transitoria. Normalizador propio: semántica estricta distinta del matcher de imágenes. ExternalProvider: claridad aprobada, no obligación funcional ni migración. Casos e intentos: estado mínimo necesario, sin ciclo de resolución artificial.

## Revisión de coherencia final

La alternativa C retira la garantía visual histórica: identidad estable y nombre actual permanecen; el frontend clasifica por nombre con neutral para desconocidos. Spec, research, modelo, contrato y quickstart no requieren mapas ni un campo de clasificación. CHK014/CHK022/CHK030 quedan satisfechos por el alcance explícito, no por una solución de reconstrucción histórica. No quedan decisiones funcionales abiertas para generar tasks; su generación y la implementación siguen pendientes.

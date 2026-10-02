# Implementation Plan: Imágenes de jugadores

**Branch**: `006-player-images` | **Date**: 2026-10-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-player-images/spec.md`

## Summary

Enriquecer manualmente a los jugadores activos con identidad e imágenes de TheSportsDB, sin alterar el catálogo de Football-Data.org; mantener `Player.id` interno, referencia externa durable, dos URLs nullable, fallback visual y auditoría completa por run. Implementar un servicio de recorrido con adaptador del proveedor, matching conservador, control de cuota común, persistencia incremental y endpoints autenticados síncronos de ejecución e historial. Véase [research.md](research.md) para decisiones y alternativas.

## Technical Context

**Language/Version**: Java 21; TypeScript 7.0.2; Node 24.19.0

**Primary Dependencies**: Spring Boot, Spring Web/RestClient, Spring Data JPA, Spring Security/JWT, Flyway; React, Vite y Sass (versiones efectivas en `backend/gradle/libs.versions.toml`, `backend/build.gradle.kts`, `frontend/package-lock.json`)

**Storage**: PostgreSQL mediante JPA y migraciones Flyway; URLs y estados locales, nunca bytes de imágenes

**Testing**: JUnit/Mockito y pruebas de integración/HTTP del backend planificadas; frontend sin tests nuevos por instrucciones locales. Ninguna prueba se ejecuta en esta fase.

**Target Platform**: Backend Spring en instancia única con PostgreSQL; navegador para frontend React; uso local inicial

**Project Type**: Aplicación web cliente/servidor

**Performance Goals**: ≥2500 ms entre inicios de solicitudes al proveedor mediante un único control global (≤24 inicios en cualquier ventana móvil de 60 s, por debajo de 30/min); espera de `429` según cabecera válida o ≥60 s; catálogo no depende de red externa

**Constraints**: Sin sincronización automática; POST síncrono potencialmente largo; identidad externa inmutable una vez resuelta; `NOT_FOUND` no degrada; resultados individuales persistidos sobreviven a fallos globales

**Scale/Scope**: Todos los jugadores activos evaluados con item individual; historial e items paginados; sin despliegue multinodo ni nueva pantalla de administración

## Constitution Check

**Gate previo a Phase 0: aprobado en diseño.** Constitución §1–6 y documentos aplicables: arquitectura común, arquitectura y convenciones de ambos módulos, tecnologías y testing backend. La especificación sigue siendo la fuente funcional; las decisiones del diseño se basan en código/configuración vigentes, no en el plan anterior. Se reutilizan roles Controller/Mapper/Service/Repository/Model e integración externa conforme a matrices de dependencia; no se mezclan responsabilidades ni se agregan dependencias sin necesidad. Cambios HTTP deben cubrir DTO, seguridad, contrato, OpenAPI y REST Docs; migraciones Flyway deben mantener esquema verificable. Pruebas backend se diseñan, no se ejecutan; frontend no incorpora infraestructura de tests. No se modifica código en esta fase.

**Gate posterior a Phase 1: aprobado en diseño.** [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md) y [quickstart.md](quickstart.md) respetan las mismas reglas; el control de ritmo, la exclusión y las unidades de persistencia quedan encapsulados en responsables coherentes; la UI solo usa datos locales. No hay desviaciones constitucionales ni incógnitas funcionales pendientes: `active` al inicio de la evaluación individual determina la pertenencia (FR-020). La verificación de comportamiento y migraciones queda para implementación y ejecución por el usuario.

## Project Structure

### Documentation (this feature)

```text
specs/006-player-images/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/api.md
└── quickstart.md
```

`tasks.md` se generará únicamente en `/speckit.tasks`; no se toca aquí.

### Source Code (repository root)

```text
backend/
├── src/main/java/footballmarket/
│   ├── models/                    # Player, referencia y nuevas entidades de resolución/auditoría
│   ├── repositories/              # acceso paginado y persistencia de las entidades
│   ├── integrations/              # cliente/adaptador TheSportsDB y validación externa
│   ├── services/                  # matching, elegibilidad, coordinación de sync y política de ritmo
│   ├── controllers/               # catálogo, sync e historial autenticados
│   └── config/                    # propiedades validadas y recuperación al arrancar
├── src/main/resources/db/migration/ # nuevas migraciones posteriores a las existentes
├── src/test/java/footballmarket/  # pruebas diseñadas por responsabilidad
└── src/docs/asciidoc/              # contrato HTTP documentado
frontend/src/features/players/
├── types/                          # DTO y modelo con las dos URLs
├── components/PlayerCard/          # fallback por error de carga, retrato completo
└── ...                             # mapper actual del catálogo
```

**Structure Decision**: Conservar los módulos y roles existentes. `Player` ya tiene `imageUrl`; agregar `fallbackImageUrl` y exponer ambos en `GET /api/players`/mapper frontend. La migración `V3__decouple_player_external_identity.sql` y cambios no committeados del catálogo son trabajo existente y no se reemplazan; partir del estado efectivo del árbol al implementar. No reutilizar el diseño obsoleto que afirmaba que `players` y GET no cambian.

## Diseño de ejecución

1. Migrar `players` para añadir `fallback_image_url`, crear resolución 1:1 y run/items con unicidad `(run_id, player_id)`, integridad referencial e índices para elegibilidad/historial. Reutilizar el provider `THE_SPORTS_DB` existente definido por `002-player-catalog`, sin agregarlo nuevamente ni sustituir `FOOTBALL_DATA`; poblar `PENDING` para jugadores existentes y nuevos. La sincronización de Football-Data.org conserva imágenes/referencias/resolución y no consulta TheSportsDB.
2. El POST autenticado adquiere exclusión no bloqueante por instancia antes de crear un run; otra petición recibe `409`. Recorrer por id estable sin definir membresía mediante instantánea global: cuando llega el turno individual, leer `active`. Si es inactivo, excluirlo sin item, contadores ni solicitud externa; si es activo, abrir exactamente un item y sumar `evaluated`, también si luego resulta omitido. Un cambio posterior de `active` no cancela la evaluación iniciada ni revierte resultados; `processed` solo suma si se intentó consultar TheSportsDB. Resolver ausencias de resolución para los activos evaluados. Mantener resultados y items confirmados por jugador sin una transacción global que cubra llamadas externas. Al completar: `COMPLETED` si no hay errores individuales/conflictos, `PARTIAL` si los hay, `FAILED` solo por error global. Consulta y detalle paginados separados del POST.
3. Sin referencia: búsqueda con identidad disponible, evaluar todos los candidatos (nombre principal/alternativo, Soccer obligatorio, equipo y validadores de fecha/nacionalidad según caso), aceptar exactamente uno o `NOT_FOUND`; persistir identidad aunque no haya URL. Con referencia: lookup directo, sin rematching ni reasignación incluso forzado. Validar únicamente la identidad devuelta por ese ID con los datos fuertes presentes y las reglas existentes, sin fuzzy matching, relevancia ni búsqueda alternativa; ausencia de dato opcional no es contradicción, incompatibilidad inequívoca deja `FAILED` conservando referencia e imágenes. Conflictos de unicidad se capturan como `FAILED` individual, incrementan `conflicts`, no modifican al otro jugador y no abortan el recorrido.
4. Adaptar `strCutout`/`strThumb` a URLs HTTPS de host exacto o subdominio real de TheSportsDB. Priorizar principal/secundaria, no duplicar ni borrar imágenes válidas ante refresh vacío/inválido. Lookup válido sin resultados ⇒ `NOT_FOUND` incluso si se conservan URLs y referencia previas, sin retry técnico; lookup que sí devuelve la identidad esperada sin nuevas imágenes utilizables ⇒ `FOUND` si había imágenes válidas, `NOT_FOUND` si no. Fallback del navegador principal → secundaria → icono genérico; no bloquear lecturas del catálogo por TheSportsDB.
5. Todas las búsquedas/lookups/reintentos comparten un único control de intervalo configurable (mínimo y default 2500 ms): a ese ritmo se permanece por debajo del límite operativo documentado de 30/min sin algoritmo adicional de ventana móvil. Sin transacción abierta durante espera. `429` respeta `Retry-After` válido o espera al menos 60 s; hasta tres reintentos tras el inicial por defecto. Persistir `lastAttemptAt` y estados según [data-model.md](data-model.md); `NOT_FOUND` espera 30 días por defecto, `RETRYABLE_ERROR` es elegible en siguiente run, `FOUND`/`FAILED` solo con force.
6. Durante arranque cerrar runs huérfanos `RUNNING` como `FAILED`/`INTERRUPTED_BY_RESTART`; finalizar solo items inconclusos como `INTERRUPTED`, nunca sumarlos a `failed`. Retener historial por plazo configurable sin asumir default funcional; borrar items junto al run. Planificar pruebas de transiciones, parsing de respuestas nulas, matching múltiple, seguridad URL, cuota/retries, colisión de referencias, reinicio, contratos y fallback visual; ejecución a cargo del usuario.

## Complexity Tracking

No hay violaciones constitucionales que justificar.

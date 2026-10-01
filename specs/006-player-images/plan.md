# Implementation Plan: Imágenes de jugadores

**Branch**: `006-player-images` | **Date**: 2026-09-30 | **Spec**: [spec.md](spec.md)

**Input**: Especificación de `specs/006-player-images/spec.md`.

## Summary

Añadir a cada jugador una imagen opcional procedente de TheSportsDB, consultable desde el catálogo local y representada en la tarjeta con fallback genérico. Un endpoint autenticado de ejecución manual buscará retratos de forma secuencial y síncrona para el solicitante; espaciará las peticiones al menos dos segundos, conservará resultados parciales si el proveedor falla y no interferirá con la sincronización independiente de Football-Data.org.

## Technical Context

**Language/Version**: Java 21 y TypeScript 7.0.2; frontend React 19.3.0.

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC/RestClient, Spring Security, Spring Data JPA, Flyway; frontend Axios, TanStack Query y Vite existentes. Sin dependencias nuevas.

**Storage**: PostgreSQL; migración Flyway aditiva de `players` para URL y estado de resolución.

**Testing**: Diseño de pruebas de Model, Repository, Service, Integration, Controller y Config del backend para ejecutar por el usuario; sin tests nuevos de frontend. La guía manual cubre imagen y fallback.

**Target Platform**: Backend HTTP y frontend web local, una instancia de aplicación.

**Project Type**: Aplicación web con backend y frontend separados.

**Performance Goals**: Una solicitud a TheSportsDB cada ≥2 s; máximo 30 solicitudes en cualquier ventana de 60 s, reintentos incluidos. La consulta local de jugadores no realiza solicitudes al proveedor de imágenes.

**Constraints**: El endpoint manual permanece abierto hasta completar el lote; un `429` respeta `Retry-After` válido o espera al menos 60 s; segunda ejecución simultánea responde 409. La API v1 gratuita limita resultados de búsqueda y puede carecer de imágenes.

**Scale/Scope**: Jugadores activos de cinco ligas, volumen variable y potencialmente cientos/miles de búsquedas; una ejecución puede durar decenas de minutos. El timeout de lectura se configura en `TheSportsDbProperties` y no puede ser inferior al tiempo esperado de una ejecución.

## Constitution Check

*GATE antes de investigar y repetido tras el diseño: sin violaciones conocidas.*

| Regla aplicable | Decisión del plan |
| --- | --- |
| Responsabilidades y dependencias | Controller transporta DTO; Service recorre pendientes y coordina persistencia; Integration obtiene y valida datos de TheSportsDB, controla ritmo y reintentos; Model protege estado; Repository solo consulta/persistencia. No se introduce Orchestrator sin necesidad. El detalle por rol está en el Implementation Outline. |
| Contrato y sincronización | `imageUrl` se agrega de forma nullable al GET; POST nuevo con DTO de éxito y errores globales; actualizar OpenAPI, REST Docs, frontend y pruebas afectadas durante implementación. Las specs 002/005 quedan complementadas por 006. |
| Seguridad | Endpoint JWT como `/sync` existente; HTTPS, timeout y URL de imagen validada contra host/ruta esperados; no registrar tokens ni URLs arbitrarias. La clave pública 123 se configura para esta integración, sin credenciales privadas versionadas. |
| Dependencias y versiones | RestClient, JPA y herramientas existentes; ninguna tecnología nueva. Si el uso efectivo de una tecnología cambia sustancialmente, actualizar su inventario en implementación. |
| Verificación | Este paso es exclusivamente documental: revisar enlaces/diff. En implementación, solo builds permitidos sin tests; pedir al usuario resultados concretos de tests backend. |

## Phase 0: Research

Las decisiones y alternativas están en [research.md](research.md): búsqueda v1 y límites, identidad, persistencia/resumibilidad, transacciones, serialización/rate limit, retries, URLs, respuesta síncrona y fallo parcial.

## Phase 1: Design & Contracts

- [data-model.md](data-model.md): campos, estado de búsqueda, invariantes y actualización tras sincronizar jugadores.
- [contracts/api.md](contracts/api.md): comportamiento HTTP, ejemplos de respuesta y errores.
- [quickstart.md](quickstart.md): validación manual de ambos recorridos y comprobaciones que ejecutará el usuario.

## Implementation Outline

1. Ampliar entidad `Player` y esquema con campos opcionales para URL/estado de búsqueda. La sincronización de Football-Data.org actualiza los datos habituales sin sobrescribir imagen; si cambia el nombre o equipo, invalidar la asociación y habilitar nueva búsqueda. Consultar solo candidatos activos pendientes, paginados o por cursor estable.
2. Encapsular en `TheSportsDbIntegration` toda la comunicación con el proveedor, como en `FootballDataIntegration`: RestClient con timeouts, traducción de errores técnicos y validación del contrato externo. La Integration también encapsula el ritmo compartido entre llamadas, con un `Clock` inyectado que haga la espera determinista y verificable. Antes de cada envío, incluidos reintentos, se comprueba que han pasado al menos 2 s desde el inicio anterior y que no se superan 30 inicios en cualquier intervalo de 60 s; ante `429` se respeta un `Retry-After` válido (segundos o fecha HTTP) y, si falta o no es válido, se esperan al menos 60 s, con límite explícito de reintentos por búsqueda. Normalizar nombres/equipos de modo conservador; aceptar exactamente un candidato con ambos datos coincidentes y `strThumb` válido. Descartar homónimos o respuestas incompletas; evitar usar `strCutout` como sustituto implícito.
3. Coordinar desde Service la ejecución manual, la exclusión simultánea y el recorrido de jugadores activos pendientes. Solicitar a Integration el resultado de cada jugador, sin administrar peticiones HTTP ni pausas. No sostener transacción de BD ni locks de filas durante esperas o llamadas remotas. Persistir cada resultado individual en una transacción breve solo si nombre, equipo y estado pendiente siguen correspondiendo a la búsqueda (la sincronización de jugadores puede ocurrir a la vez). Reanudar los no resueltos en una invocación posterior. La exclusión se libera al salir por éxito o error.
4. Exponer `POST /api/players/images/sync` autenticado, bloqueante para el cliente hasta completar. Responder 200 con contadores de procesados/encontrados/sin imagen; 409 cuando ya esté en curso; 502 con `ErrorResponseDTO` estable ante ejecución incompleta por fallo del proveedor. Conservar en BD fotos procesadas antes del error. Si se corta la conexión, la recuperación debe depender de una nueva invocación manual, no de un trabajo automático.
5. Ampliar mapper, DTO, frontend Model/DTO/Mapper y tarjeta con URL opcional y fallback de carga; no añadir una pantalla de sincronización. Actualizar REST Docs y OpenAPI para contrato nuevo y ampliación de GET. Las verificaciones de backend y frontend corresponden a la fase de implementación.

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

### Archivos de implementación afectados (raíz del repositorio)

Solo se listan archivos que se crean o modifican según `tasks.md`; `➕` indica archivo nuevo y `✏️` indica archivo que se modifica. Los tests se incluyen aunque su ejecución corresponda al usuario. En el código, el proveedor se nombra `TheSportsdbService` para el Service del caso de uso y `TheSportsDb*` para Properties, ClientConfig e Integration; en la documentación descriptiva se conserva la forma oficial `TheSportsDB`.

```text
backend/src/main/java/footballmarket/
├── config/
│   ├── TheSportsDbProperties.java ➕
│   └── TheSportsDbClientConfig.java ➕
├── exceptions/
│   └── ApplicationException.java ➕
├── integrations/
│   ├── exceptions/
│   │   └── TheSportsDbUnavailableException.java ➕
│   └── TheSportsDbIntegration.java ➕
├── models/
│   ├── Player.java ✏️
│   └── enums/
│       └── PlayerImageResolution.java ➕
├── repositories/
│   └── PlayerRepository.java ✏️
├── services/
│   ├── TheSportsdbService.java ➕
│   ├── exceptions/
│   │   ├── PlayerImageSyncInProgressException.java ➕
│   │   └── PlayerImageSyncIncompleteException.java ➕
│   └── impl/
│       ├── PlayerCatalogServiceImpl.java ✏️
│       └── TheSportsdbServiceImpl.java ➕
└── controllers/
    ├── PlayerController.java ✏️
    ├── dtos/responses/
    │   ├── PlayerResponseDTO.java ✏️
    │   └── PlayerImageSyncResponseDTO.java ➕
    ├── mappers/
    │   └── PlayerMapper.java ✏️
    └── exceptions/
        └── GlobalExceptionHandler.java ✏️

backend/src/test/java/footballmarket/
├── config/
│   └── TheSportsDbPropertiesTest.java ➕
├── controllers/
│   └── PlayerControllerTest.java ✏️
├── integrations/
│   └── TheSportsDbIntegrationTest.java ➕
├── models/
│   └── PlayerTest.java ✏️
├── repositories/
│   └── PlayerRepositoryTest.java ➕
└── services/
    ├── PlayerCatalogServiceTest.java ✏️
    └── TheSportsdbServiceTest.java ➕

backend/src/main/resources/
├── db/migration/
│   └── V3__add_player_image.sql ➕
└── application.properties ✏️
backend/src/test/resources/
└── application-test.yml ✏️
backend/src/docs/asciidoc/
└── index.adoc ✏️

frontend/src/features/players/
├── types/
│   ├── dtos.ts ✏️
│   └── models.ts ✏️
├── players.mapper.ts ✏️
└── components/PlayerCard/
    ├── PlayerCard.tsx ✏️
    └── PlayerCard.module.scss ✏️
```

`backend/build.gradle.kts` y `SecurityConfig.java` se revisan en T001–T002, sin cambios previstos. `docs/backend/technologies.md` y `specs/005-players-page/spec.md` son ajustes documentales condicionales de T037–T038, fuera de este árbol de src/test. El endpoint se añade al `PlayerControllerTest.java` existente (T019), no a una clase de test separada.

## Post-Design Constitution Check

Se mantiene el cumplimiento de responsabilidades, seguridad, contrato y política de dependencias. La operación prolongada es una elección funcional explícita del usuario; la guía de validación advierte del tiempo de espera sin modificar el contrato a asíncrono. No hay excepciones constitucionales que justificar.

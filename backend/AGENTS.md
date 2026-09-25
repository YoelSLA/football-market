# Instrucciones para tareas del backend

Este archivo complementa las instrucciones del `AGENTS.md` raíz. No duplica sus reglas globales.

## Contexto

- Para código, estructura o dependencias arquitectónicas, consultar la [arquitectura del backend](../docs/backend/architecture.md), además de la arquitectura común indicada por el `AGENTS.md` raíz.
- Para convenciones de implementación, formato o documentación de código y endpoints, consultar las [convenciones del backend](../docs/backend/conventions.md).
- Para conocer el stack, las tecnologías, sus versiones o su propósito, y ante cambios de tecnologías o dependencias, consultar [tecnologías del backend](../docs/backend/technologies.md).
- Al cambiar comportamiento backend o crear, modificar o analizar sus tests, consultar [testing del backend](../docs/backend/testing.md): reglas generales y categoría afectada.

## Antes y durante el cambio

- Ante cambios HTTP, revisar Controller, DTO, seguridad, contrato, tests, OpenAPI, REST Docs y Postman.
- Ante cambios de persistencia, revisar Model, Repository, migraciones y validación del esquema.
- Las migraciones están en `backend/src/main/resources/db/migration/`, REST Docs en `backend/src/docs/asciidoc/` y la colección en `postman/collections/`.
- `backend/README.md` puede estar desactualizado.
- `backend/build/` es la salida generada específica del backend alcanzada por la regla global.

## Verificación

Si necesita resultados de tests del backend, el agente debe indicar al usuario los tests concretos y solicitar su salida.

El build permitido para cambios de código backend es `./gradlew build -x test` desde `backend/`.

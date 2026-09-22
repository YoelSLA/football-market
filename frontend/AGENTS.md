# Instrucciones para tareas del frontend

Este archivo complementa las instrucciones del `AGENTS.md` raíz. No duplica sus reglas globales.

## Contexto

- Para código, estructura o dependencias arquitectónicas, consultar la [arquitectura del frontend](../docs/frontend/architecture.md), además de la arquitectura común indicada por el `AGENTS.md` raíz.
- Para conocer el stack, las tecnologías, sus versiones o su propósito, y ante cambios de tecnologías o dependencias, consultar [tecnologías del frontend](../docs/frontend/technologies.md).

## Antes y durante el cambio

`frontend/dist/` y los archivos `*.tsbuildinfo` generados bajo `frontend/` son las salidas generadas específicas del frontend alcanzadas por la regla global.

## Verificación

Por el momento no crear tests de frontend ni incorporar infraestructura para ellos.

El build permitido para cambios de código frontend es `npm run build` desde `frontend/`.

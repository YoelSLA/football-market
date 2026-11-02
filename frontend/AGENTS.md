# Instrucciones para tareas del frontend

Este archivo complementa las instrucciones del `AGENTS.md` raíz. No duplica sus reglas globales.

## Contexto

- Para código, estructura o dependencias arquitectónicas, consultar la [arquitectura del frontend](../docs/frontend/architecture.md), además de la arquitectura común indicada por el `AGENTS.md` raíz. Su [mapa de consulta](../docs/frontend/architecture.md#mapa-de-consulta) identifica los documentos especializados por responsabilidad; consultar el rol afectado junto con su [matriz de dependencias](../docs/frontend/architecture/dependencies.md) y las [observaciones de consistencia](../docs/frontend/architecture.md#consistencia).
- Para naming, formato, estilos o convenciones de implementación, consultar las [convenciones del frontend](../docs/frontend/conventions.md).
- Para conocer el stack, las tecnologías, sus versiones o su propósito, y ante cambios de tecnologías o dependencias, consultar [tecnologías del frontend](../docs/frontend/technologies.md).

## Directiva de Desacople, Modelos y Control de Calidad Frontend

Toda tarea de interfaz de usuario (UI) DEBE ejecutarse en dos fases secuenciales y desacopladas, con un punto de control humano intermedio obligatorio:

### 1. Fase 1: Lógica, Estado y Estructura (Modelo Ligero / Rápido)
- **Perfil de modelo sugerido**: `Gemini 3.8 Flash` o `GPT Sol` / `Claude Sonnet 5.5` (según criticidad lógica).
- **Alcance**:
  - DTOs, modelos de dominio, mappers, servicios de transporte y TanStack Query hooks.
  - Árbol JSX semántico accesible (`aria-*`) y cableado de datos.
  - Hooks de coordinación de estado, filtros reactivos y paginación.
- **Regla estética**: No realizar maquetado fino ni CSS definitivo. La estructura JSX debe basarse estrictamente en el Box Model y contenedores definidos en los wireframes ASCII de la documentación, garantizando la jerarquía de contenedores padres e hijos.
- **Criterio de salida**: `npm run build` y `npm run biome:fix` en 0 errores.

### 🛑 Gate de Pausa Obligatorio (Revisión Humana Intermedia)
- Al completar la Fase 1, el agente DEBE **detener su ejecución de inmediato**.
- PROHIBIDO avanzar a la Fase 2 sin autorización humana explícita.
- El agente entregará un informe breve (máx. 300 palabras) confirmando la integridad de datos, rutas y build, quedando a la espera de la orden del desarrollador para continuar.

### 2. Fase 2: Maquetado Fino y Estética (Modelo Pesado / Especializado en Visión)
- **Perfil de modelo sugerido**: `Gemini 4 Argon` o `Claude Sonnet 5.5` / `Claude Opus 5.5` con soporte multimodal/visión.
- **Alcance**:
  - Refactorización exclusiva de módulos SCSS (`*.module.scss`) y clases de layout (Flexbox/Grid).
  - Ajuste visual pixel-perfect contrastando directamente contra los mockups de diseño (`docs/design/mockups/`) y wireframes ASCII.
  - Control estricto del presupuesto de altura vertical (*height budget*) para no desbordar el viewport útil del navegador.
  - Aplicación de tokens semánticos del design system (colores, radios de borde, tipografías y marcas de agua).
- **Regla funcional y de estructura**: PROHIBIDO modificar contratos, mappers, endpoints, props de negocio, bindings de datos, eventos (`onClick`), atributos de accesibilidad (`aria-*`) ni lógica/hooks ya validados en la Fase 1. Se permite únicamente agregar o reorganizar etiquetas puramente contenedoras (`div`, wrappers para `flex`/`grid`, `className={styles...}`) indispensables para el maquetado.
- **Verificación visual**: Doble chequeo. El agente audita multimodalmente su SCSS/JSX contra el mockup de referencia; el cierre definitivo de la fase requiere la confirmación visual humana del desarrollador en el navegador (`localhost`).
- **Criterio de salida**: Interfaz fiel a la referencia visual + `npm run build` y `npm run biome:fix` en 0 errores.

## Antes y durante el cambio

`frontend/dist/` y los archivos `*.tsbuildinfo` generados bajo `frontend/` son las salidas generadas específicas del frontend alcanzadas por la regla global.

## Verificación

Por el momento no crear tests de frontend ni incorporar infraestructura para ellos.

Los controles ejecutables permitidos para cambios de código frontend son `npm run build` y `npm run biome:fix` desde `frontend/`.

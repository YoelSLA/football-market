# Guía rápida de validación: Visualizar ranking de jugadores (SPEC-022)

**Feature**: `022-view-player-rankings` · **Contrato**: [`contracts/player-rankings-consumption.md`](contracts/player-rankings-consumption.md)

Guía para validación manual por el usuario. No acredita ejecución, aceptación ni autorización. No crear ni ejecutar tests frontend.

## Prerrequisitos

- Backend y frontend disponibles según configuración vigente.
- Usuario con sesión JWT válida.
- Datos de ranking disponibles para ambos modos, si se verifican respuestas con contenido.

## Escenarios

1. **Ruta y autenticación**: iniciar sesión y abrir `/players/ranking` desde la sección Jugadores y directamente por URL. Confirmar pestaña activa y solicitudes autenticadas.
2. **Modo rendimiento**: comprobar `GET /api/rankings/score`; validar query de tabla según controles, sobre y filas conforme a SPEC-018, estrategia/fecha visibles y score/fecha por entrada.
3. **Modo cotización**: cambiar a cotización y comprobar `GET /api/rankings/quote`; validar página, filtros, identidad, precio, vigencia y origen conforme a SPEC-016.
4. **Podio**: inspeccionar una consulta sin filtros `page=0&size=3` independiente de la tabla. Confirmar orden/posiciones recibidas, que se muestran solo las entradas disponibles si hay menos de tres, y que cambiar filtros de tabla no cambia parámetros de podio. No exigir snapshot simultáneo entre ambas consultas.
5. **Filtros y páginas**: combinar nombre, liga y posición; confirmar que los parámetros llegan al backend, opciones permanecen globales, el total es filtrado y posiciones no se renumeran. Cambiar filtros/tamaño/mode vuelve a página inicial; navegar conserva criterios.
6. **Estados**: distinguir carga, respuesta vacía global, filtro sin coincidencias, error recuperable y expiración de sesión conforme a los manejadores existentes.
7. **Identidad y responsive**: verificar que nombre y demás datos provienen de `player` de la respuesta, fallback si faltan datos opcionales y navegación a `/players/:id`. La adaptación responsive y la accesibilidad general siguen siendo requisitos de la Feature; la inspección visual de este delta queda a cargo del usuario según la decisión registrada en [`sdd-gates.md`](sdd-gates.md), y no se considera validada por esta guía ni por la implementación documental.

## Controles de fase

Esta fase PLAN no modifica código y no ejecuta build. En fase de código frontend posterior, usar solo controles permitidos en `frontend/AGENTS.md`; el agente no ejecuta tests y la exención frontend permanece vigente.

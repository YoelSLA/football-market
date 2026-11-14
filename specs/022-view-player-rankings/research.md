# Investigación y decisiones técnicas: Visualizar ranking de jugadores

**Feature**: `022-view-player-rankings`
**Fuentes**: [`spec.md`](spec.md), [`plan.md`](plan.md), contrato score de [SPEC-018](../018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md) y contrato quote de [SPEC-016](../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md).

## 1. Transporte y estado remoto

- **Decisión**: Reutilizar Axios y TanStack Query existentes a través del patrón Page → Hook → Service/Mapper → API. Consultar `GET /api/rankings/score` y `GET /api/rankings/quote`, enviando `page`, `size` y los filtros provistos por los contratos.
- **Rationale**: Mantiene el flujo y la infraestructura existentes y limita las respuestas a la página requerida. Query keys deben distinguir modo, página, tamaño y filtros para no mostrar resultados de otra consulta.
- **Alternativas**: Descargar el ranking entero y paginar/filtrar en el navegador (contradice los contratos y desperdicia datos); crear infraestructura HTTP o caché propia (innecesario).

## 2. Podio y tabla

- **Decisión**: Consultar el mismo endpoint del modo activo dos veces con propósitos distintos: tabla con sus criterios actuales, y podio global sin filtros con `page=0&size=3`. Consumir posiciones del servidor sin reordenar ni renumerar.
- **Rationale**: Es la forma definida en SPEC-018 y SPEC-016; ambas solicitudes no garantizan una instantánea compartida.
- **Alternativas**: Derivar el podio de la página/tabla (no garantiza los líderes globales); añadir una ruta de podio (no está en contrato).

## 3. Identidad y opciones de filtros

- **Decisión**: Renderizar la identidad `player` incluida en cada fila; poblar selectores desde `filterOptions` de la respuesta. Tratar campos opcionales conforme a nulabilidad contractual, con fallbacks visuales sin fabricar datos ni consultar el catálogo para enriquecer filas.
- **Rationale**: Los contratos proveen identidad por entrada y opciones globales elegibles; usar el catálogo agregaría una consulta y una dependencia no requeridas.
- **Alternativas**: Cargar `useAllPlayersQuery()` o consultar cada jugador (acoplamiento/solicitudes adicionales); derivar opciones de la página actual (incompleto y dependiente de filtros).

## 4. Estado de consulta y navegación

- **Decisión**: Mantener filtros y paginación en el alcance más reducido de la Page o su Hook autorizado. Cambios de filtro/tamaño/modo llevan a página cero; cambios de página retienen criterios. Usar cancelación/identidad de query existente para que respuestas de criterios anteriores no sustituyan a las actuales. Exponer carga, error reintentable, vacío global y vacío filtrado.
- **Rationale**: Cumple los escenarios de SPEC-022 y evita estado remoto duplicado.
- **Alternativas**: Estado global o lógica manual de peticiones en componentes (innecesarios frente a la infraestructura disponible).

## 5. Presentación y recursos

- **Decisión**: Reutilizar recursos y componentes públicos compartidos existentes, iconos desde su API autorizada, SCSS Modules y tokens del tema. No añadir dependencias. Los detalles de composición visual respetan mockups y normas de arquitectura; la planificación no crea componentes sin responsabilidad definida.
- **Rationale**: Reduce alcance y mantiene consistencia visual/accesible.
- **Alternativas**: Biblioteca UI/tabla, iconos importados directamente desde el paquete, copia de assets compartidos (no necesarios y contrarios a convenciones).

## 6. Verificación

- **Decisión**: No crear ni ejecutar tests frontend. La exención temporal sigue vigente. `quickstart.md` sirve de guía manual. Si en una fase autorizada se modifica código frontend, solo aplicar los controles permitidos por `frontend/AGENTS.md` y la Constitución; no ejecutar tests indirectamente.
- **Rationale**: Respeta las normas del repositorio.
- **Alternativas**: Añadir infraestructura de tests (expresamente prohibido por las instrucciones vigentes).

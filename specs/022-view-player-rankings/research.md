# Research & Technical Decisions: Visualizar ranking de jugadores (SPEC-022)

**Feature**: `022-view-player-rankings`
**Spec**: [`spec.md`](spec.md) · **Plan**: [`plan.md`](plan.md)

Este documento registra las decisiones técnicas, su justificación y las alternativas evaluadas para la implementación de la vista de ranking de jugadores en el frontend.

---

## 1. Consumo de endpoints y gestión de estado remoto

- **Decisión**: Consumir directamente los dos endpoints REST de ranking provistos por el backend mediante Axios en `players.service.ts`:
  1. `GET /api/players/ranking/score`: Ranking por rendimiento de la estrategia activa.
  2. `GET /api/players/ranking`: Ranking por cotización vigente de mercado.
  El estado remoto se administra con TanStack Query a través de hooks dedicados (`usePlayerScoreRankingQuery` y `usePlayerQuoteRankingQuery`).
- **Justificación**: Sigue la arquitectura canónica del frontend (`Page → Hook → Service/Mapper → API`). TanStack Query proporciona manejo nativo de estados de carga (`isPending`), refresco en segundo plano, deduplicación de solicitudes y caché coherente (con `staleTime` global de 30 s).
- **Alternativas consideradas**:
  - *Estado local con `useState` + `useEffect`*: Descartado; generaría código repetitivo, riesgo de condiciones de carrera y carece de caché global o reintentos automáticos.
  - *Unificar ambos contratos en un único endpoint en backend*: Descartado; violaría el principio de mínimo alcance y modificaría contratos cerrados de SPEC-018.

---

## 2. Hidratación y enriquecimiento de datos del jugador

- **Decisión**: Reutilizar el hook existente `useAllPlayersQuery()` de `@/features/players` para obtener el catálogo completo de jugadores y construir un mapa indexado por `playerId` (`Map<number, Player>`). Cada entrada del ranking (`playerId`, `position`, `score`/`price`) se enriquece en memoria con:
  - Nombre completo del jugador.
  - Imagen/avatar o fallback genérico.
  - Nombre y escudo del equipo.
  - Nombre y bandera de la liga.
  - Demarcación táctica (`position` deportiva: DEL, MED, DEF, POR).
- **Justificación**: El contrato de backend de ranking solo devuelve el identificador (`playerId`) para maximizar la velocidad y respetar el principio de responsabilidad única. Como la sección de Jugadores ya descarga y cachea el catálogo completo en TanStack Query, la hidratación en memoria es instantánea ($O(1)$) y no genera solicitudes de red adicionales.
- **Alternativas consideradas**:
  - *Consultar `/players/:id` para cada jugador visible*: Descartado por ser ineficiente (problema N+1 de red en el navegador).
  - *Modificar el backend para incluir campos visuales en el ranking*: Descartado por el principio de coherencia de contratos y alcance acotado.

---

## 3. Arquitectura del Podio Top 3 y Tabla de Clasificación

- **Decisión**: Implementar la presentación visual en dos niveles complementarios, tal como establecen los mockups normativos (`docs/design/mockups/players/raking/`):
  1. **Sección Podio (Top 3)**: Presenta de forma destacada a los 3 primeros clasificados con laureles (#1 central y aumentado con oro; #2 izquierda en plata; #3 derecha en bronce). Muestra fotografía ampliada, marca de agua con escudo del club, badge de posición, club, liga y valor. El podio es **fijo** en la modalidad activa y no se recalcula por filtros de tabla.
  2. **Tabla Detallada con Paginación**: Muestra todas las posiciones consecutivas (1 a N) con paginación local de 10 elementos por página, con columnas completas y estados visuales claros.
- **Justificación**: Proporciona una experiencia de usuario atractiva y gamificada (reconocimiento visual instantáneo de los líderes del mercado), complementada con la precisión de una tabla analítica exhaustiva.
- **Alternativas consideradas**:
  - *Solo tabla de posiciones*: Descartado por no cumplir la referencia visual aprobada en los mockups.
  - *Solo tarjetas de jugadores*: Descartado; la inspección y comparación de cientos de clasificaciones se vuelve ineficiente sin formato tabular.

---

## 4. Filtrado y búsqueda reactiva en cliente

- **Decisión**: Implementar la barra de herramientas de filtrado (búsqueda por nombre de jugador, selector desplegable de ligas y selector de posiciones) como filtrado reactivo en el cliente sobre los datos cacheados en memoria.
- **Justificación**: El backend no soporta ni admite query parameters en los endpoints de ranking (los ignora según el contrato). El filtrado en cliente es instantáneo (sin latencia de red), conserva intacta la numeración original de la posición (#) de cada jugador y permite al usuario ubicar rápidamente jugadores específicos dentro de la clasificación oficial.
- **Alternativas consideradas**:
  - *No incluir filtros*: Descartado; los mockups oficiales incluyen explícitamente la barra de filtros para usabilidad en listas de más de 2.000 jugadores.
  - *Reasignar posiciones del 1 al M en la lista filtrada*: Descartado por RF-06 y CA-05; la interfaz no debe recalcular posiciones ni simular rankings alternativos.

---

## 5. Paginación y control de rendimiento

- **Decisión**: Paginación en cliente sobre el conjunto filtrado, mostrando por defecto 10 jugadores por página, con selector para alternar a 20 o 50 filas, y controles de avance, retroceso y acceso a páginas extremas.
- **Justificación**: El conjunto de jugadores clasificados puede superar los 2.000 registros. Renderizar 2.000 nodos del DOM simultáneamente degradaría el rendimiento en dispositivos móviles. La paginación en cliente mantiene el DOM ligero (< 20 filas a la vez) con cambios de página instantáneos.
- **Alternativas consideradas**:
  - *Scroll infinito*: Descartado; dificulta acceder al pie de página y no coincide con el diseño de paginación explícito de los mockups.

---

## 6. Estilos, Tokens y Componentes

- **Decisión**:
  - Estructuración mediante **SCSS Modules** (`*.module.scss`) aislados por componente.
  - Uso exclusivo de variables semánticas de color y espaciado de `styles/abstracts/themes/_default.scss` y mixins tipográficos/responsivos.
  - Reutilización de componentes compartidos: `Navbar`, `NavIcon`, `LoadingSpinner`, `EmptyState`, badges de posición y botones existentes.
  - Iconos SVG de `lucide-react` acordes al diseño (trofeo, monedas, gráfico de rendimiento, laureles, lupa, banderas/escudos).
- **Justificación**: Respeta estrictamente las directrices de `frontend/conventions.md` y `docs/frontend/architecture.md`. No se agregan librerías de estilos ni paquetes externos.

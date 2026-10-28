# Research: Detalle individual del jugador

## Investigación vigente — rediseño 2026-10-25

Estas decisiones desarrollan exclusivamente el alcance vigente de `spec.md`. Las secciones R-01 y siguientes se conservan como investigación histórica; las decisiones sobre mocks, finanzas o herramientas retiradas no se aplican al rediseño.

| Decisión | Razón y evidencia | Alternativas descartadas |
| --- | --- | --- |
| Mantener identidad y consulta estadística separadas | `players.service.ts` ya ofrece `getById`; `usePlayerDetailPage` distingue identidad y consultas secundarias. Permite mantener identidad ante error de estadísticas y no cambia consumidores del catálogo | Agregar estadísticas obligatorias al DTO del catálogo; reemplazar el endpoint de identidad |
| Extender la feature `players`, sin feature estadística frontend nueva | La Page y sus recursos ya pertenecen a `features/players`; las normas prohíben dependencias entre features | Importar hooks de cartera/mercado; llevar lógica del jugador a `app` o `shared` |
| Query Hook → Service → Mapper → Model | Es el flujo normativo de roles/dependencies. Servicio usa cliente `http` autenticado y `AbortSignal`; hook de Page coordina errores independientes, pestaña y filtros | HTTP en Component; DTO en Hook; validar manualmente cada campo HTTP como actualmente hace código heredado |
| Constantes de metadatos y perfiles, no cálculos deportivos | La matriz de 60 claves de la spec fija nombres, categorías, orden y prioridad; cuatro perfiles declarativos seleccionan tarjetas y series. Valores siempre del contrato, sin normalización | Radar, percentiles, promedio de liga, SVG con datos simulados |
| Barras HTML/CSS accesibles, sin paquete nuevo | Gráficos son conjuntos pequeños de tasas de igual unidad. Longitud proporcional al máximo positivo visible es codificación visual, no un indicador publicado; texto conserva el valor original. Cero tiene etiqueta 0, ausencia no tiene barra; todo cero no divide por cero | Incorporar una librería; reutilizar el gráfico financiero con semántica de serie temporal; porcentajes de rendimiento |
| Page Hook existente como coordinador real | Ya coordina identidad, consultas y reintento. Separa estado de identidad del estado estadístico; clave estadística incluye ID y temporada solicitada. No usar `keepPreviousData` entre jugadores | Store global; ocultar identidad por un fallo estadístico; conservar ID antiguo mediante efecto durante un render |
| Componentes locales presentativos | `PlayerOverview`, breadcrumb y retrato permanecen en la carpeta de la Page. Nuevos paneles reciben modelos y callbacks, no conectan hooks ni realizan HTTP | Moverlos a shared preventivamente; hooks en componentes locales de Page |
| Preservar compatibilidad del catálogo | Catálogo aún consume la fuente de demostración: retirar únicamente las llamadas demo del detalle y revisar consumidores antes de eliminar archivos. Mantener firmas e identidad del catálogo | Borrar `demo/`, keys o hooks compartidos porque el detalle deja de usarlos |
| No cambiar layouts globales | `PageLayout` gestiona scroll desktop; móvil usa comportamiento vigente, sin altura fija. Se conserva navbar desde `AppLayout`, tokens y breakpoints existentes; columnas colapsan sin exceder ancho | Cambiar breakpoints globales para resolver una Page; introducir scroll vertical anidado o forzar 60 filas en un viewport |

No se añade selector de temporada a la UI. El contrato admite temporada explícita; la consulta sin selección resuelve el último contexto confirmado del jugador y devuelve su temporada real, sin hardcodear 2025/2026 ni 2026/2027. Esto operacionaliza el contexto existente sin ampliar la interacción.

### Decisiones de backend contrastadas

- **Decisión**: consulta dedicada con interfaz/implementación Service, Repository de estadísticas y Model de lectura puro; Mapper estático en Controller. **Razón**: matrices backend no permiten Service→Service general. Reutilizar `PlayerRepository.findVisibleById`, no invocar `PlayerCatalogService` desde el nuevo Service. **Alternativas**: usar selector poblacional de valuaciones o delegar composición en Mapper, incompatibles con spec/arquitectura.
- **Decisión**: confirmación exige fila `RESOLVED` e item asociado `COMPLETED`, con jugador, fecha y canal consistentes. **Evidencia**: `PlayerStatisticsWriter` publica ambos atómicamente; filas sin contexto pueden existir con item `NO_DATA`; `run_item_id` nullable en migración. **Alternativa**: confiar en existencia de fila o fecha, insuficiente.
- **Decisión**: seleccionar máximo de fecha por jugador/temporada y anclar por JSON, luego observación/ID; respaldar solo mismo contexto. **Evidencia**: unicidad `(extraction_date,source,player_id)` de `V20261018191335__flatten_player_statistics.sql`, columnas nullable y `MetricCatalog.metricValue`/`PlayerStatistics.metricValue`. **Alternativa**: mezclar valores históricos para maximizar cobertura, expresamente prohibida.
- **Decisión**: lectura de ancla/respaldo dentro de snapshot transaccional consistente. **Razón**: una extracción concurrente no debe introducir datos de otra campaña entre consultas. **Alternativa**: varias consultas independientes sin consistencia, riesgo de selección no reproducible.
- **Decisión**: sin migraciones nuevas inicialmente. **Razón**: columnas y clave de unicidad existentes bastan; índice de jugador/fecha ya existe. **Alternativa**: nueva tabla o materialización del dashboard, sin necesidad actual.
- **Decisión**: contrato de consulta nuevo, no el DTO de sincronización/coverage. **Razón**: necesita 60 valores/procedencias y ausencia exitosa, no auditoría de run ni operaciones de extracción. **Alternativa**: exponer entidades JPA o respuestas operativas al frontend.

Fuentes: `models/PlayerStatistics.java`, `models/MetricCatalog.java`, `repositories/PlayerStatisticsRepository.java`, `services/internal/PlayerStatisticsWriter.java`, `services/internal/LeagueContextSelection.java`, `services/internal/ValuationSnapshotSelector.java`, `controllers/PlayerController.java`, `config/SecurityConfig.java` y normas backend. La observación real de métricas ya registrada en spec no se convierte en un requisito de 57 valores para cada jugador.

Verificación documental: fuentes consultadas incluyen arquitectura común, frontend (roles, dependencias, composición, presentación), tecnologías, convenciones y los consumidores actuales. Sin llamadas a proveedor, pruebas, build ni cambios funcionales.

---

## Antecedente histórico de investigación

Resuelve las decisiones técnicas diferidas a Plan por las clarificaciones de 2026-10-12.
Cada decisión indica opciones evaluadas y motivo; no introduce requisitos nuevos.

## R-01. Visibilidad del detalle: reutilizar el criterio del catálogo

- **Decisión**: el backend resuelve `GET /api/players/{id}` con el mismo predicado de
  visibilidad del catálogo (jugador activo, equipo vigente, liga válida) y devuelve `404`
  cuando no hay coincidencia, exista o no el jugador.
- **Razón**: reutiliza `PlayerRepository.findByActiveTrue` como criterio único de visibilidad,
  evita revelar la existencia de jugadores inactivos y cumple FR-003 sin un endpoint de
  diagnóstico adicional.
- **Alternativas evaluadas**: (a) buscar por `id` y devolver `active` en el DTO, descartada
  porque revela estado y añade un campo fuera del contrato de catálogo; (b) responder `410`
  para inactivos, descartada por ser un estado nuevo sin precedente en el proyecto; (c) filtrar
  en el frontend, descartada porque el catálogo no puede ser la autoridad de visibilidad.

## R-02. Consulta individual: método de repositorio y error de aplicación

- **Decisión**: añadir un método de lectura al repositorio con la misma cláusula de visibilidad
  que el catálogo, devolverlo desde `PlayerCatalogService` y modelar la ausencia mediante una
  excepción de aplicación propia mapeada a `404` con un código estable.
- **Razón**: mantiene `Repository → Model`, `Service → Repository`, `Controller → DTO/Mapper/
  Service` y reutiliza el `GlobalExceptionHandler` y `ErrorResponseDTO` existentes, sin
  introducir reglas de negocio en Controller ni códigos nuevos fuera del patrón vigente.
- **Alternativas evaluadas**: (a) `Optional<Player>` con `orElseThrow` genérico, descartada
  por las convenciones que prohíben excepciones genéricas para errores funcionales; (b) que el
  Controller resuelva la ausencia, descartada porque el Controller no debe decidir visibilidad;
  (c) `GET /api/players?id=` reutilizando el listado, descartada porque no satisface la ruta
  individual exigida ni la semántica de recurso.

## R-03. Validación del identificador en la ruta

- **Decisión**: enlazar `id` como `Long` en la ruta y delegar en la conversión de Spring junto
  con el handler vigente de tipo de parámetro para el `400`.
- **Razón**: un id no numérico o con formato inválido produce `INVALID_PARAMETER_TYPE` con el
  handler vigente; no hace falta validación adicional ni un DTO de entrada.
- **Alternativas evaluadas**: (a) `@Positive` con `@Validated`, descartada porque introduce
  validación estructural duplicada para un único parámetro ya tipado; (b) `String` con parseo
  manual, descartada por responsabilidad de conversión duplicada.

## R-04. Fuentes de demostración: una única frontera simulada en el frontend

- **Decisión**: un módulo de datos de demostración dentro de `features/players` que produce
  estadísticas y series de cotización a partir del `id` del jugador y constantes de período,
  exponiendo Services de lectura propios; la Page consume esos Services mediante Query Hooks
  igual que la identidad real.
- **Razón**: cumple FR-012 (fuente centralizada y sustituible), respeta
  `Page/Component → Hook → Service`, mantiene DTO/Model separados y evita presentar datos
  ficticios como contrato HTTP confirmado.
- **Alternativas evaluadas**: (a) constantes por componente, descartada por no centralizar;
  (b) un backend de demostración, descartada porque la spec prohíbe endpoints financieros y
  estadísticos nuevos y la entrega debe funcionar sin dependencia del compañero; (c) generar
  datos en el Page/Component, descartada por responsabilidad de presentación.

## R-05. Determinismo de las simulaciones

- **Decisión**: derivar estadísticas y cotizaciones con una función hash determinista del
  `id` del jugador y de las constantes de temporada y período, sin generators aleatorios, sin
  dependencia de `Date.now()` ni del reloj del navegador; el período de treinta días consecutivos
  con una cotización diaria se define con fechas fijas independientes de la temporada deportiva.
- **Razón**: satisface FR-009/FR-010/FR-014 con valores distintos por jugador, estables entre
  recargas y coherentes entre catálogo y detalle al compartir la misma función.
- **Alternativas evaluadas**: (a) tabla de fixtures por id, descartada por mantenimiento manual
  creciente; (b) `Math.random()` con semilla, descartada por depender del orden de renderizado;
  (c) derivar del reloj, descartada por contradecir el período fijo acordado.

## R-06. Coherencia de la cotización actual con el historial

- **Decisión**: derivar precio actual y variación del último y penúltimo punto de la serie
  generada, sin valores independientes.
- **Razón**: garantiza la coherencia exigida por FR-014 y SC-006 por construcción, sin
  reconciliación posterior.
- **Alternativas evaluadas**: calcular la variación en el componente, descartada por lógica de
  presentación de datos; duplicar el cálculo en catálogo y detalle, descartada por riesgo de
  divergencia.

## R-07. Gráfico de evolución sin dependencia nueva

- **Decisión**: representar la serie como gráfico en línea con SVG propio, exponiendo los
  valores de forma accesible mediante texto o tabla equivalente.
- **Razón**: el proyecto no tiene librería de gráficos; Constitution §2 obliga a preferir las
  capacidades existentes y no agregar dependencias sin justificación técnica y aprobación.
- **Alternativas evaluadas**: (a) Recharts, evaluada como opción futura si la interacción pasa
  a requerir hovered tooltips o dataset grandes, pero rechazada hoy por añadir dependencia,
  peso de bundle y mantenimiento sin necesidad funcional actual; (b) Chart.js, descartada por
  renderizar en canvas, ofrecer peor accesibilidad y exigir acceso innecesario a una API de
  animación; (c) CSS puro, descartada por no representar una serie de datos de forma robusta y
  accesible.

## R-08. Composición de la navegación global

- **Decisión**: crear la navbar como componente transversal reutilizable y componerla desde
  `app` (área de layouts/router), dejando el detalle como consumidor; el breadcrumb del detalle
  permanece en `features/players` porque depende de su dato real.
- **Razón**: `docs/frontend/architecture.md` §1.1/§1.12 reserva a `app` la composición global y
  prohíbe que una feature dependa de otra; el detalle necesita identidad y cierre de sesión ya
  disponibles en `features/auth`, por lo que consumir esa feature desde otra está prohibido.
- **Alternativas evaluadas**: (a) navbar dentro de `features/players`, descartada por atar una
  capacidad global a una feature y bloquear su reutilización; (b) navbar en `shared/`, descartada
  porque necesitaría depender de la identidad de sesión, que pertenece a `infrastructure` y a la
  feature de auth; (c) replicar navegación en cada página, descartada por contradecir la
  decisión de globalidad.
- **Alcance**: SPEC-009 habilita el componente y su uso en el detalle. La decisión de producto del
  2026-10-13 (R-11) amplía su adopción a todas las rutas privadas y actualiza FR-018 en consecuencia.

## R-09. Estados de UI differentiation

- **Decisión**: derivar el estado de la pantalla a partir del Query Hook de identidad real
  (pending, error, 404, success) y presentar las secciones de demostración solo cuando la
  identidad se resolvió correctamente.
- **Razón**: evita presentar datos ficticios como respuesta real y materializa FR-007/FR-008
  con una sola fuente de verdad.
- **Alternativas evaluadas**: carga simultánea de todas las consultas, descartada por poder
  mostrar resúmenes sin identidad real.

## R-10. Contratos futuros de cotizaciones y estadísticas

- **Decisión**: documentar en `contracts/` un anexo de integración futura marcado explícitamente
  como no implementado y no confirmado, sin DTOs de producción ni endpoints en el código.
- **Razón**: cumple FR-021 y evita presentar como existente una API que otro integrante
  desarrolla, permitiéndole contrastar el documento con su contrato real.
- **Alternativas evaluadas**: omitir el anexo, descartada por perder la preparación conceptual;
  definir DTOs completos, descartada por inventar contratos no confirmados.

## R-11. Navegación global aplicada a todas las rutas privadas

- **Decisión** (2026-10-13): la navegación se compone en `app/layouts/AppLayout` y envuelve todas
  las rutas privadas (`/home`, `/players`, `/players/:id`). El componente presentativo vive en
  `shared/components/Navbar` y obtiene la sección activa mediante `NavLink` sobre una lista
  declarativa de destinos; no recibe `activeSection` ni depende de ninguna feature.
- **Razón**: una capacidad global que se aplica parcialmente produce divergencias entre pantallas
  y obliga a cada Page a repetir la navegación. La composición desde `app` conserva la prohibición
  de dependencias entre features, porque la identidad y el cierre de sesión se resuelven allí
  mediante las APIs públicas de `features/auth`.
- **Alternativas evaluadas**: (a) un layout por ruta, descartada por divergencia visual; (b) la
  navbar dentro de `features/players`, descartada por atar una capacidad global a una feature;
  (c) el componente en `app`, descartada porque `app` compone, no presenta.
- **Efecto sobre alcance**: sustituye la redacción de FR-018 del 2026-10-12. No elimina ninguna
  restricción del enunciado; lo contrario, prepara el cumplimiento de `tp/enunciado.md` §11.

## R-12. Layouts compartidos y capa de estilos

- **Decisión** (2026-10-13): `PageLayout` resuelve la estructura común de página (cabecera
  opcional, contenido, `aria-busy`) y `PaginatedPageLayout` añade la paginación compartida. Los
  estilos globales se separan en `styles/base` (reset) y `styles/abstracts` (breakpoints, tokens
  y temas), con `styles/index.scss` como único punto de entrada.
- **Razón**: evita duplicar estructura y breakpoints en cada Page, y centraliza los valores de
  presentación para que el tema oscuro del detalle y el catálogo compartan el mismo lenguaje visual.
- **Alternativas evaluadas**: (a) estilos por Page sin tokens, descartada por duplicación;
  (b) un único `styles/index.scss` con todos los símbolos, descartada por acoplar la capa global a
  un único consumidor.
- **Pendiente**: `PlayerDetailPage.module.scss` consume la capa de abstracts pero conserva colores
  literales. Se registra como divergencia de convención a corregir, no como decisión ratificada.

## R-13. Recursos gráficos: locales frente a externos

- **Decisión** (2026-10-13): los escudos de equipo son recursos **locales** importados en tiempo de
  build y resueltos por nombre normalizado contra un registro generado; las banderas de nacionalidad
  son un **servicio externo** consultado en runtime.
- **Razón**: los escudos son parte de la identidad visual estable de la tarjeta y no pueden depender
  de la red; las banderas son un adorno de baja consecuencia y no justifican incorporar y versionar
  cientos de imágenes. Ambos casos exigen marcador de posición accesible, conforme a
  `tp/enunciado.md` §8, que exige tolerar la falla de proveedores externos.
- **Alternativas evaluadas**: (a) servir también las banderas localmente, descartada por coste de
  inventario; (b) resolver banderas por emoji, descartada por inconsistencia entre plataformas;
  (c) eliminar el escudo por colisiones de nombre, descartada porque el marcador de posición cubre
  ese caso sin perder información textual.

## R-14. Paginación compartida con ventana ±1

- **Decisión** (2026-10-13): el catálogo usa el componente compartido `Pagination`, con ventana de
  la página anterior y la posterior más accesos a la primera y a la última, elipsis cuando hay
  páginas omitidas y total de páginas completo cuando el rango es pequeño.
- **Razón**: una ventana de ±1 con elipsis es más legible en pantallas estrechas, coherente con la
  adaptación a móvil del catálogo, y reutilizable por cualquier Page paginada futura.
- **Alternativas evaluadas**: (a) mantener el paginador propio del catálogo, descartada por duplicar
  lógica ya resuelta en `shared/`; (b) conservar la ventana ±3, descartada por desbordamiento en
  pantallas estrechas.
- **Efecto sobre alcance**: sustituye SPEC-005 FR-014 y el criterio SC-004 correspondiente.

## Dependencias identificadas

- Backend: `PlayerRepository`, `PlayerCatalogService`, `GlobalExceptionHandler`,
  `ErrorResponseDTO`, `PlayerMapper`, seguridad JWT vigente y OpenAPI.
- Frontend: `features/players` (servicio, mapper, query keys, hooks), `infrastructure/http`,
  `infrastructure/storage` (estado de sesión), `app/router`, `app/layouts` y componentes
  compartidos existentes.
- Normativa: Constitution v7.0.0, workflow SDD, arquitectura y convenciones de ambas áreas,
  `docs/backend/testing.md` §8 (REST Docs) y `docs/frontend/technologies.md` si el stack cambia
  (no se prevé cambio en esta feature).

No quedan decisiones técnicas bloqueantes; el diseño es implementable con los componentes y
convenciones vigentes.

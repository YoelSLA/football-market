# Research: Visualizar portfolio personal (SPEC-017)

**Feature**: SPEC-017 · **Rama**: `feat/017-view-personal-portfolio-ui` · **Fecha**: 2026-10-21
**Plan**: [`../plan.md`](../plan.md) · **Spec**: [`../spec.md`](../spec.md)

Decisiones técnicas de la fase Plan. No modifican el alcance aprobado en `spec.md`: ninguna altera un
requisito funcional, un criterio de aceptación ni una aclaración, y ninguna exige modificar otra SPEC.
La fuente de verdad del contrato es SPEC-014 y la de las reglas contables es SPEC-015; esta feature
consume ambas sin redefinirlas.

---

## R-01 — Ubicación y límites de la feature

**Decisión**: la feature vive en `frontend/src/features/portfolio/` y su API pública expone
**únicamente** las dos Pages.

**Fundamento**: `docs/frontend/architecture.md` §1.1 establece la feature como unidad principal de
organización funcional y §1.2 exige que cada feature declare explícitamente su API pública. El
precedente es `features/players/index.ts`, que exporta solo `PlayersPage` y `PlayerDetailPage`:
justamente lo que `app/router` necesita para asociar rutas.

**Alternativas descartadas**:

- **Una feature `portfolio` con subcarpetas por pestaña**: no corresponde al modelo del proyecto, donde
  una feature es una unidad funcional y las variantes de una misma feature se resuelven con rutas.
- **Colocar las pantallas en `features/players`**: mezclaría el catálogo público de jugadores con el
  portfolio privado del usuario; son datos de dominio y de autorización distintos.
- **Ubicar el detalle de la posición en `shared/`**: el panel expone conceptos exclusivos del portfolio
  (posición, tenencia, valoración) y `shared` no puede recibir conceptos de una feature (§1.1).

## R-02 — Dos rutas y dos Pages, no pestañas sin ruta

**Decisión**: `/portfolio` para *Mi portfolio* y `/portfolio/holdings` para *Mis tenencias*, con una
Page por ruta.

**Fundamento**: la navegación por pestañas del Navbar es **basada en rutas**. `NavTab.path` es la única
vía por la que la pestaña activa se resuelve: `resolveActiveNavigation` compara prefijos de ruta y
gana la coincidencia más específica. Una pestaña sin ruta no puede ser activa ni navegable, y el
modelo de datos de `NavTab` no contempla un estado interno alternativo: `path` es `string | null` y
`null` significa «funcionalidad futura».

**Alternativas descartadas**:

- **Una sola Page con pestañas internas**: obligaría a inventar un mecanismo de sincronización entre
  la pestaña visual y la del Navbar, que hoy no existe y que solo se usa para esta feature.
- **Una pestaña sin ruta pero navegable por estado**: contradice el contrato de `NavTab` y el
  tratamiento uniforme que el Navbar da a las funcionalidades futuras.

## R-03 — Integración HTTP: un Service, un Mapper, dos Query Hooks

**Decisión**: `portfolio.service.ts` es la única frontera HTTP y posee el endpoint
`GET /api/portfolio/me`; `portfolio.mapper.ts` transforma DTO → Model; dos Query Hooks
consumen el Service con parámetros distintos.

**Fundamento**: la arquitectura exige el flujo `Page / Component → Hook → Service` y prohíbe que una
Page o un Component usen un Service directamente (§1.3, §1.4). El Service debe ser dueño del endpoint
(§1.6). Se sigue el patrón tipado de `features/auth/auth.service.ts`: `http.get<PortfolioResponseDTO>`,
con `authenticated: true`, `params` y `signal`, seguido de transformación mediante Mapper.
Las validaciones heredadas de `players.mapper.ts` no se extienden a esta feature: prevalece §1.8.

Dos Query Hooks y no uno porque las claves de caché deben ser independientes: una clave única que
incluyera la página invalidaría el resumen cada vez que el usuario pagina, y la caché del resumen debe
ser estable porque el resumen no depende de la página.

**Alternativas descartadas**:

- **Un Query Hook con parámetros opcionales**: mezclaría dos invalidaciones de caché con formas de
  clave distintas y obligaría a distinguir «sin filtro» de «filtro Todas» por un valor mágico.
- **Que el Service devuelva el DTO y el Mapper se aplique en el Hook**: el DTO no puede salir de la
  frontera del Service (§1.8).

## R-04 — El resumen se consulta con el tamaño mínimo

**Decisión**: la vista de *Mi portfolio* solicita `page=0` y `size=1`, sin `status`.

**Fundamento**: el contrato garantiza que `summary` «representa siempre el **portfolio completo**: es
idéntico con y sin filtro y con cualquier página». El tamaño de página solo afecta a `positions` y a
los metadatos de paginación, que la vista de resumen no utiliza. Pedir seis posiciones para no
mostrarlas encarece el payload sin agregar información.

**Alternativas descartadas**:

- **`size=6` en ambas vistas**: pagaría posiciones que la vista de resumen descarta.
- **Un endpoint de resumen separado**: ampliaría el contrato de SPEC-014 sin necesidad y crearía una
  segunda fuente para los mismos indicadores.

## R-05 — Mapper de transformación tipada

**Decisión**: recibir `PortfolioResponseDTO` y producir Model separado mediante asignaciones
explícitas, preservando importes, nulos y orden. Sin type guards, comprobaciones de coherencia
runtime ni recálculos financieros.

**Fundamento**: arquitectura frontend §1.8 prescribe confiar en el contrato tipado. El Service de
auth ya aplica ese patrón. Las garantías financieras pertenecen a SPEC-014/SPEC-015, no al Mapper.

**Alternativas descartadas**: replicar las validaciones heredadas del catálogo o añadir schemas
locales: introducirían una estrategia de validación runtime ad hoc no aprobada por la arquitectura.
El tipado estático no detecta respuestas malformadas en runtime; no se promete su rechazo.

**Corrección autorizada**: esta decisión sustituye el diseño inicial de validación manual; se
mantienen los requisitos funcionales y la entrega de la navegación a tenencias con US2 (A1).

## R-06 — DTO y Model separados, con la ausencia de dato como `null`

**Decisión**: `types/dtos.ts` refleja el contrato HTTP y `types/models.ts` define los conceptos de la
interfaz. Los importes se modelan **en centésimas**, tal como llegan; la conversión a créditos ocurre
en `utils/`; todo dato ausente es `null` y nunca `0`.

**Fundamento**: §1.8 exige la separación conceptual y tipada aunque ambos tengan los mismos campos, y
prohíbe reutilizar un DTO como Model. La distinción entre ausencia y cero es el eje de FR-004,
FR-029 y AC05: un indicador que depende de una cotización ausente es `null` en el contrato, y la
interfaz debe mostrar un guion, no un cero. Modelar el cero y la ausencia como el mismo valor
destruiría esa distinción en la frontera.

**Alternativas descartadas**:

- **Un único tipo para DTO y Model**: prohibido de forma expresa por §1.8.
- **Modelar los importes ya convertidos en créditos**: desplazaría la conversión al Mapper, que no
  debe calcular, y perdería la fidelidad con el contrato.
- **Modelar la ausencia como `0`**: haría imposible distinguir «no hay dato» de «el valor es cero»,
  que es exactamente lo que el contrato distingue.

## R-07 — Claves de caché y política de datos

**Decisión**: las claves viven en `constants/portfolioQueryKeys.ts`:
`["portfolio", "summary"]` y `["portfolio", "holdings", status, page]`.

**Fundamento**: §1.13 exige centralizar por feature las claves de TanStack Query. La clave del resumen
no incluye parámetros porque su contenido no depende de ellos; la de las tenencias incluye el filtro y
la página porque ambos determinan el conjunto consultado.

La caché no necesita una estrategia de invalidación propia: `app/providers/SessionLifecycle` limpia la
Query Cache al iniciar, restaurar o cerrar sesión (§1.10, §1.11), de modo que ningún dato financiero de
una identidad sobrevive a otra. Un `staleTime` propio no aporta nada y contradice la configuración
global.

**Alternativas descartadas**:

- **Invalidación manual al cambiar de pestaña**: solo genera tráfico y no añade información.
- **Un `staleTime` propio por feature**: la configuración global ya resuelve el caso de uso.

## R-08 — Dónde vive el estado de la vista

**Decisión**: `usePortfolioSummaryPage` y `usePortfolioHoldingsPage` son los Page Hooks. El Page Hook de
tenencias coordina página, filtro, recorte de página inválida y posición abierta.

**Fundamento**: §1.3 admite un Page Hook solo cuando hay estado o coordinación real; aquí la hay: la
vista de tenencias coordina tres parámetros que interactúan. La posición abierta vive en el Page Hook y
no en la tarjeta porque la consumen dos elementos —la grilla y el panel—: es estado compartido de la
vista, y §1.10 reserva el estado local al Component, el Hook o la feature.

**Alternativas descartadas**:

- **Estado de la posición dentro de `PortfolioPositionCard`**: el panel no podría abrirse desde la
  tarjeta sin duplicar el estado o lifting manual.
- **Un store de feature**: no hay estado compartido por más de una unidad que lo justifique; §1.10
  lo desaconseja explícitamente.

## R-09 — Componentes: qué se extrae y qué no

**Decisión**: seis componentes de feature —`PortfolioMetricCard`, `PortfolioValueSection`,
`PortfolioPortrait`, `PortfolioPositionCard`, `PortfolioStatusFilter` y `PortfolioPositionDetail`—.
El retrato encapsula la degradación de imagen compartida por tarjeta y panel, y admite nombre
nulo con texto alternativo genérico, sin inventar datos descriptivos del jugador.

**Fundamento**: §1.1 exige que la extracción responda a una responsabilidad, cohesión o reutilización
real, y no al tamaño visual. `PortfolioValueSection` se usa en siete bloques con la misma forma —título
y filas de rótulo y valor—: los dos paneles del resumen y las cinco secciones del panel de detalle. Es
el precedente de `PlayerSportsSummary`. `PortfolioMetricCard` se usa cuatro veces con la misma forma de
indicador destacado.

No se extrae una «tarjeta base» genérica: las tarjetas del resumen, de las posiciones y las secciones
del panel tienen estructuras distintas y una abstracción común solo aportaría indirección.

**Alternativas descartadas**:

- **Un componente `Card` genérico con slots**: sin un segundo consumidor real, contradice §I y §1.1.
- **Marcar el panel con HTML semántico y estilos propios, sin componente**: mezclaría el detalle de la
  posición con el resto de la vista en un único archivo de gran tamaño y sin cohesión propia.

## R-10 — El panel lateral: superposición modal accesible

**Decisión**: el panel se superpone al contenido de la pestaña, que queda inerte; es un diálogo modal
con `role="dialog"`, `aria-modal`, foco gestionado, cierre con `Escape` y restitución del foco al
botón que lo abrió.

**Fundamento**: FR-020 fija la superposición y FR-035 exige el cierre con teclado, el foco gestionado y
la exposición accesible de diálogo. Marcar el contenido de fondo como inerte es lo que hace coherente
«el resto permanece inerte» con la realidad del teclado: sin ello, el filtro seguiría siendo alcanzable
con `Tab` detrás del panel.

**Alternativas descartadas**:

- **Un panel no modal**: el contenido de fondo seguiría siendo interactivo, contradiciendo FR-020.
- **Una ruta propia para el detalle**: el panel no es una pantalla con URL propia; la especificación
  exige que no modifique la ruta activa.

## R-11 — Reubicación del ícono genérico de jugador

**Decisión**: `Player Generic-Icon.png` se reubica de `features/players/assets/` a
`shared/assets/`, y los dos consumidores de la feature de jugadores actualizan su import.

**Fundamento**: una feature no puede depender de otra, ni siquiera por su API pública (§1.2 y la matriz
de §1.14). El recurso es ahora compartido por dos features, que es el criterio que §1.1 fija para que un
elemento viva en `shared`. La reubicación evita dos copias del mismo recurso, que pueden divergir.

El requerimiento de imágenes de jugadores ya establece el ícono genérico de FootballMarket como último
recurso del retrato, de modo que la cadena de la tarjeta del portfolio es `imageUrl` → ícono genérico: el
contrato del portfolio no expone `fallbackImageUrl` y la interfaz no puede inventarlo.

**Alternativas descartadas**:

- **Duplicar el archivo en `features/portfolio/assets/`**: duplica un recurso compartido y abre la
  posibilidad de que las dos copias divergan.
- **Crear una silueta de persona nueva**: la referencia visual la muestra, pero es un artefacto de la
  maqueta —igual que la etiqueta «Datos de ejemplo», omitida por DC-09— e introduciría un segundo icono
  de «sin imagen» en la aplicación.
- **Dejar el ícono en la feature de jugadores y referenciarlo por ruta**: equivaldría a una dependencia
  entre features.

**Consecuencia documental**: `docs/frontend/technologies.md` registra el inventario de recursos
gráficos locales y debe reflejar la nueva ubicación en el mismo cambio (Constitution §IV).

## R-12 — Paleta: superficies oscuras, delimitación de la tarjeta y nacimiento de cinco roles

**Decisión**: las tarjetas y el panel usan superficies oscuras existentes; se nacen cinco primitivos
—`$color-positive-on-dark`, `$color-negative-on-dark`, `$color-caution-on-dark`, `$color-error-on-dark`
y `$color-border-dark`— y sus cinco roles semánticos en el archivo de temas.

**Fundamento**: la referencia visual aprobada muestra tarjetas y panel oscuros sobre fondo oscuro, y la
decisión humana de esta fase lo confirma. La aplicación ya tiene superficies oscuras: `$color-neutral-800`
—fondo de la barra y la cápsula de paginación— y `$color-neutral-950` —paneles de la ficha de jugador—.

Los estados financieros, en cambio, no existen sobre superficie oscura. Los tres colores de estado
vigentes están calculados para superficies claras y no alcanzan contraste sobre `#1e293b`: el verde de
éxito `#17613b` y el rojo de error `#a32739` quedan por debajo de 3:1, y el ámbar de advertencia
`#b45309` también. Sin los nuevos roles, el resultado positivo, el negativo y el «Sin cotización»
serían ilegibles, lo que contradice FR-033 y FR-029.

**La tarjeta necesita un límite explícito.** La superficie de tarjeta `#1e293b` sobre el fondo de
página `#111b40` mide 1,14:1: sin borde, la tarjeta no se distingue del fondo. La referencia lo resuelve
con un borde sutil de 1,69:1, insuficiente para un límite con significado. Se adopta
`$border-surface-dark` sobre `#64748b`, que mide 3,52:1 frente al fondo y 3,07:1 frente a la tarjeta.

**El error de consulta no comparte color con la pérdida.** El rol de error vigente mide 2,02:1 sobre la
tarjeta, de modo que el mensaje de error sería ilegible. Y aunque un rojo nuevo lo resolviera, un fallo
de la consulta y una pérdida de valor son hechos distintos: comparten superficie, no semántica. Por eso
`$state-error-on-dark` es un rol propio.

El valor de ese rol lo fija el contraste, no la estética: el rojo de la referencia (`#ef526e`) mide
4,25:1 sobre la tarjeta y **no** alcanza el 4,5:1 que WCAG exige para texto normal. Se adopta
`#fda4af`, que mide 7,74:1 sobre la tarjeta, 8,85:1 sobre el fondo de página y 9,44:1 sobre el panel:
AA con holgura en todas las superficies donde puede aparecer el mensaje. Sigue siendo un valor
distinto del rojo del resultado negativo —1,46:1 entre ambos—, de modo que los dos roles no se
confunden; la prominencia del error la aportan el `role="alert"`, el borde del bloque y la acción de
reintento, no solo el tono.

El valor verde elegido es `#4ade80`, igual que `$color-accent-green`: la aplicación mantiene **un solo
verde**, el de la navegación activa, y la sección no introduce un segundo. Los cinco roles se registran
con su función y su contraste medido sobre la superficie oscura, como exige el archivo de tokens, que
prohíbe expresamente los roles speculativos.

**Alternativas descartadas**:

- **Reutilizar los roles de estado actuales sobre superficie oscura**: no alcanzan contraste; el texto
  del resultado sería ilegible.
- **Escribir los hexadecimales literales en los componentes**: prohibido por FR-031 y por §1.1.
- **Usar `$color-accent-green` directamente, sin primitivo propio**: su función documentada es el
  acento de navegación y su restricción prohíbe usarlo como color de marca general; el rol de estado
  financiero necesita su propia función documentada.
- **Reutilizar `$border-strong` (`#475569`) como borde de tarjeta**: evita un primitivo, pero mide
  2,21:1 frente al fondo y queda por debajo del 3:1 que corresponde a un límite con significado.
- **Reutilizar `$state-negative` para el mensaje de error**: resolvería el contraste, pero haría que
  un fallo técnico y una pérdida de valor fueran indistinguibles para quien lee la pantalla.

## R-13 — Formato de importes, porcentajes y fechas

**Decisión**: `utils/formatCredits.ts`, `utils/formatPercentage.ts` y `utils/formatDateTime.ts` son
funciones puras de la feature. El formato numérico usa `Intl.NumberFormat` con locale `es-AR` y dos
decimales fijos; la ausencia se representa con un guion.

**Fundamento**: §1.13 sitúa en `utils/` las operaciones puras sobre los Model. La conversión de centésimas
a créditos pertenece a la presentación: el Service y el Mapper no calculan. `Intl.NumberFormat` evita
escribir a mano la separación de miles y el separador decimal, y produce exactamente el formato de la
referencia visual.

**Alternativas descartadas**:

- **Formatear en el Mapper**: el Mapper transforma, no presenta.
- **Escribir el formato a mano en cada componente**: duplicaría la regla en cuatro sitios y la haría
  divergir.

## R-14 — El estado de la retícula: qué aporta el número de página

**Decisión**: el tamaño de página es siempre 6 y la retícula se readapta a dos o una columnas por
breakpoint; el número de página no cambia con el viewport.

**Fundamento**: la decisión aprobada y FR-034 fijan seis tarjetas por página, con la retícula en tres
columnas por dos filas en pantalla ancha y readaptación en pantalla estrecha. La decisión humana de esta
fase confirma la readaptación de columnas frente a la reducción del tamaño de página.

El tamaño de página es un parámetro del contrato (`size`, entre 1 y 100) y el backend pagina sobre
él. Reducirlo según el viewport haría que la paginación dependiera del dispositivo y que el total de
páginas mostrara un número distinto en cada pantalla.

**Alternativas descartadas**:

- **Reducir `size` en móvil**: el total de páginas y la numeración dependerían del dispositivo, y el
  filtro y la página no se comportarían igual entre pantallas.

## R-15 — Rejilla semántica

**Decisión**: la grilla de posiciones es una lista `ul`/`li` con `aria-label`.

**Fundamento**: la grilla es una colección de seis elementos repetidos, cada uno con un encabezado y una
acción. La semántica de lista comunica el número de elementos y la estructura a las tecnologías de
asistencia, que una `section` genérica no comunica. La desviación respecto del patrón de la grilla del
catálogo de jugadores es deliberada y está acotada a este componente.

**Alternativas descartadas**:

- **`section` con `aria-label`**, como en el catálogo: no comunica la cardinalidad de la colección.

## R-16 — Estrategia de pruebas

**Decisión**: esta feature **no crea tests** ni infraestructura de testing.

**Fundamento**: la Constitution establece una exención temporal de testing del frontend —«no se crean ni
se ejecutan tests del frontend, ni se incorpora infraestructura para ellos»— y `frontend/AGENTS.md` lo
reitera. La estrategia de validación es el control ejecutable permitido (`npm run build`, que ejecuta el
type-check estricto y el build) más la comprobación manual guiada por `quickstart.md`.

**Alternativas descartadas**:

- **Tests unitarios del Mapper y de los formateadores**: contradicen la exención vigente.
- **Tests end-to-end**: la entrega 2 tiene como único punto Core pendiente separar los perfiles de
  testing, de modo que introducir infraestructura de e2e en el frontend excedería el alcance.

## R-17 — Fiabilidad: qué se muestra cuando el backend falla

**Decisión**: un error de carga muestra el mensaje del backend cuando es válido y una acción de
reintento, y no muestra ningún dato financiero. La ausencia de cotización no es un error.

**Fundamento**: el contrato distingue la ausencia de cotización —200 con valoración incompleta— de la
falla técnica del servicio de cotizaciones —503 sin datos parciales—. La integridad es responsabilidad
del backend; el Mapper frontend no la revalida. `getErrorMessage` de `infrastructure/http` devuelve el mensaje del backend
cuando el error es un `ApiError` válido y el texto de respaldo en cualquier otro caso, sin que la
feature interprete códigos propios: el backend no define códigos funcionales para esta consulta.

**Alternativas descartadas**:

- **Mostrar el resumen aunque fallen las posiciones**: presentaría indicadores y detalle de momentos
  distintos, que es exactamente lo que el contrato de SPEC-014 garantiza que no ocurre.
- **Interpretar el código `QUOTE_SOURCE_UNAVAILABLE` en la feature**: el código pertenece al contrato
  global de errores; la feature solo necesita distinguir error de ausencia, que el estado HTTP ya
  expresa.

## R-18 — Comportamiento del panel de detalle en pantalla estrecha

**Decisión**: por debajo de 900 px el panel ocupa todo el ancho del área de contenido y toda la altura
disponible; por encima, es un panel lateral anclado al borde derecho de ancho fijo. En ambos casos la
cabecera —título y control de cierre— permanece fija, el cuerpo tiene desplazamiento propio, el panel
nunca excede la altura disponible, el foco queda confinado, `Escape` cierra y el contenido de la pestaña
sigue inerte.

**Fundamento**: el panel es un diálogo modal, y su semántica no depende del ancho. Lo que cambia es si
cabe un panel lateral: por debajo de 900 px, un panel de ancho fijo taparía la práctica totalidad de la
grilla y reduciría el espacio legible de las dos columnas de datos de la referencia. El ancho completo
conserva la lectura, mantiene visible la acción de cierre y conserva intactas las garantías de foco,
cierre e inactividad del fondo. El corte reutiliza el breakpoint existente de una sola columna
(`$bp-detail`), de modo que no se introduce un corte nuevo para un caso que ya está descrito.

**Alternativas descartadas**:

- **Un panel lateral estrecho en móvil**: comprimiría las dos columnas de la referencia hasta hacerlas
  ilegibles y dejaría la acción de cierre en un área de toque mínima.
- **Sustituir el panel por una pantalla completa con ruta propia**: contradice FR-020, que exige que el
  panel no modifique la ruta activa, y obligaría a gestionar el historial de navegación del navegador.
- **Un comportamiento distinto por ancho en la semántica del diálogo**: obligaría a mantener dos rutas
  de código para el foco, el cierre y la inactividad del fondo, con el riesgo de que divergieran.

## Conclusiones de la Fase 0

No quedan `NEEDS CLARIFICATION` sin resolver. Las decisiones que la especificación no podía tomar —la
superficie de las tarjetas, la ubicación del ícono genérico, el valor del color de error sobre fondo
oscuro y el comportamiento del panel en pantalla estrecha— se resolvieron con el responsable del
proyecto y quedan registradas en el plan (§11, §12 y §13) y en la investigación (R-11, R-12 y R-18).

La feature no requiere dependencias nuevas, no altera ningún contrato de backend y no reescribe
artefactos de otra SPEC. El único cambio transversal es la reubicación de un recurso gráfico, con su
actualización correspondiente del inventario documental.

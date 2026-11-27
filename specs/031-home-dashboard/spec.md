# Feature Specification: Dashboard interactivo y resumen ejecutivo de mercado (Bloomberg & Sorare)

**Feature Branch**: `feat/031-home-dashboard`  
**Estado**: Especificación viva del sistema.  
**Issue de origen**: [#109](https://github.com/YoelSLA/football-market/issues/109) (`SPEC-031 | Dashboard interactivo y resumen ejecutivo de mercado (Bloomberg & Sorare)`).  
**Requerimiento académico**: `REQ-S2-FEAT-ADD` / `REQ-S3-05` (Visualización de datos de usuario con gráficos, UX financiera y cockpit de mercado).  
**Referencias normativas**:
- [`tp/enunciado.md`](../../tp/enunciado.md) (Valoración de jugadores, gráficos y experiencia financiera de usuario)
- [`tp/entregas.md`](../../tp/entregas.md) (Visualización gráfica en frontend, entrega y justificación de producto)
- [`specs/014-view-personal-portfolio/spec.md`](../014-view-personal-portfolio/spec.md) (Reglas de negocio de posiciones y tenencias)
- [`specs/015-define-average-cost-and-financial-results-rules/spec.md`](../015-define-average-cost-and-financial-results-rules/spec.md) (Fórmulas de costo promedio, P&L realizado y no realizado)
- [`specs/016-ejecutar-recalculo-manual-de-cotizaciones/spec.md`](../016-ejecutar-recalculo-manual-de-cotizaciones/spec.md) (Historial y persistencia de cotizaciones en `player_quotes`)
- [`specs/022-view-player-rankings/spec.md`](../022-view-player-rankings/spec.md) (Rankings de jugadores por cotización y score)
- [`specs/030-my-orders/spec.md`](../030-my-orders/spec.md) (Órdenes de venta y mercado P2P)

---

## 1. User Scenarios & Testing

### User Story 1 — Financial Hero Cockpit: Consulta de Patrimonio Neto y Liquidez (Priority: P1)

Como usuario autenticado e inversor de Football Market, quiero que la pantalla principal me reciba con un resumen ejecutivo de mi posición patrimonial total para conocer de forma instantánea mi patrimonio neto, saldo líquido disponible, ganancia/pérdida acumulada y tokens en custodia.

**Independent Test**: Iniciar sesión con un usuario que posee créditos disponibles y tokens en cartera; acceder a `/home` y verificar que las 4 tarjetas superiores del Hero Cockpit exhiben con precisión matemática el Patrimonio Neto (`netWorth`), el P&L total con su variación porcentual, el efectivo disponible y el desglose de tokens poseídos vs. reservados en órdenes P2P, todo con feedback cromático semántico.

**Acceptance Scenarios**:
1. El acceso a la pantalla se realiza en la ruta `/home` desde la pestaña `"Dashboard"` de la sección `"Inicio"`.
2. La tarjeta de **Patrimonio Neto (Net Worth)** muestra el valor total consolidado en créditos:
   $$\text{Net Worth} = \text{Créditos Disponibles} + \sum (\text{Tokens Poseídos} \times \text{Cotización Oficial Vigente})$$
   Acompañada de un micro-sparkline SVG que refleja la trayectoria reciente del patrimonio.
3. La tarjeta de **Resultado Financiero (P&L Total)** desglosa el resultado no realizado (posiciones abiertas) y el resultado realizado (ventas históricas cerradas), indicando el retorno porcentual acumulado (ROI %) con badge verde esmeralda para ganancia o rojo rubí para pérdida.
4. La tarjeta de **Poder de Compra (Liquidez)** muestra el saldo exacto de créditos libres listos para operar en el mercado oficial o P2P.
5. La tarjeta de **Activos en Custodia** detalla la cantidad total de tokens en cartera, distinguiendo claramente entre *Tokens Disponibles* (libres para transferir o vender) y *Tokens Reservados* (bloqueados en publicaciones activas del mercado secundario).
6. Los importes numéricos en backend se informan en centésimas de crédito y el frontend los divide por 100 para representarlos con dos decimales exactos.

---

### User Story 2 — Analytics de Cartera: Gráficos de Asignación y Riesgo (Priority: P1)

Como usuario con múltiples jugadores en cartera, quiero visualizar la composición y distribución de mis inversiones mediante gráficos interactivos para analizar mi diversificación táctica y por ligas, así como evaluar el nivel de concentración de mi capital.

**Independent Test**: Con un portfolio compuesto por jugadores de distintas ligas y posiciones, interactuar con el gráfico Donut SVG nativo; conmutar entre la vista por Liga y por Posición táctica, verificar que los sectores circulares corresponden a los pesos porcentuales y que al posar el cursor sobre cada segmento se exhibe un tooltip accesible con el detalle del volumen en créditos y porcentaje.

**Acceptance Scenarios**:
1. El gráfico Donut interactivo SVG renderiza arcos de circunferencia calculados en función del valor actual de mercado de las posiciones de cada categoría.
2. El selector de dimensión permite alternar en tiempo real entre:
   - **Por Posición Táctica**: Delanteros, Mediocampistas, Defensas y Arqueros.
   - **Por Competición**: Premier League, La Liga, Serie A, Bundesliga y Ligue 1.
3. El centro del donut muestra el valor total de la cartera o la categoría destacada al interactuar.
4. El panel incluye una leyenda interactiva con colores normalizados del design system y porcentajes redondeados a dos decimales.
5. El bloque incorpora un indicador de **Concentración de Cartera**: si un único futbolista representa más del 40% del valor total de la cartera, se despliega una alerta preventiva de riesgo de concentración.
6. Todos los datos gráficos disponen de una tabla semántica accesible oculta o expandible bajo `<details>` para lectores de pantalla.

---

### User Story 3 — Market Pulse y Benchmark: Rendimiento vs. FMI y Top Movers (Priority: P1)

Como inversor cuantitativo de fútbol, quiero evaluar si el rendimiento de mis decisiones de inversión supera al mercado general comparando la curva de valor de mi cartera contra el índice general de la plataforma (*Football Market Index*), además de monitorear a los jugadores que más subieron o bajaron de cotización en el último recálculo.

**Independent Test**: Seleccionar distintos rangos de tiempo (1 Mes, 3 Meses, Histórico Completo) en el gráfico de evolución; comprobar que se dibujan con precisión ambas curvas (Cartera del usuario vs. Benchmark FMI) y que el panel de *Top Movers* presenta la tabla con los 5 jugadores con mayor apreciación (*Top Gainers*) y las 5 mayores caídas (*Top Losers*).

**Acceptance Scenarios**:
1. El gráfico de rendimiento temporal dibuja dos series en SVG nativo con gradientes y leyendas claras:
   - Serie A: Evolución del valor total de la cartera del usuario a través de las fechas de cotización registradas.
   - Serie B: Evolución del **Football Market Index (FMI)**, calculado como el promedio ponderado del valor de los jugadores cotizables de la plataforma.
2. Los selectores de rango temporal permiten filtrar entre `1M` (último mes), `3M` (último trimestre) y `ALL` (todo el historial disponible).
3. El bloque **Top Movers** expone dos pestañas o columnas paralelas:
   - **Top Gainers**: Los 5 futbolistas con mayor variación porcentual positiva en la última cotización respecto a su precio previo, con badge de suba (`+X.XX%`).
   - **Top Losers**: Los 5 futbolistas con mayor corrección negativa porcentual, con badge de baja (`-Y.YY%`).
4. Cada fila de Top Movers muestra retrato, nombre, club, precio actual en créditos y delta nominal y porcentual, funcionando como enlace al perfil detallado del jugador.

---

### User Story 4 — Bloomberg Market Ticker: Cinta Superior de Cotizaciones (Priority: P2)

Como usuario que sigue el mercado minuto a minuto, quiero ver una cinta horizontal continua de cotizaciones al estilo de las terminales bursátiles para conocer al instante los precios y variaciones de las principales estrellas de las 5 ligas europeas.

**Independent Test**: Observar la cinta superior ubicada inmediatamente bajo el Navbar; verificar que muestra cotizaciones de jugadores de referencia con sus deltas calculados, que permite pausar la animación al pasar el cursor y que los elementos son interactivos.

**Acceptance Scenarios**:
1. El ticker se ubica horizontalmente en el encabezado del Dashboard, abarcando el ancho completo de la vista.
2. Muestra al menos 10 jugadores destacados de las 5 grandes ligas (ej: Mbappé, Haaland, Kane, Bellingham, Lautaro Martínez, Vinicius Jr, etc.) con sus escudos de equipo y banderas de liga.
3. Para cada jugador se presenta: nombre/apellido, cotización oficial actual en créditos y badge con flecha y delta porcentual (`▲ +4.2%` en verde o `▼ -1.8%` en rojo).
4. El ticker permite desplazamiento continuo y suave, deteniendo el movimiento cuando el usuario sitúa el puntero sobre él para facilitar la lectura.
5. Al hacer clic sobre cualquier cotización del ticker, el usuario es redirigido a `/players/{id}`.

---

### User Story 5 — Showcase Deportivo Coleccionable (Sorare Style) (Priority: P2)

Como aficionado al fútbol y coleccionista, quiero explorar cartas de jugadores diseñadas con estética visual premium (retratos de alta calidad, escudos, banderas, scores WhoScored) organizadas por categorías estratégicas para tomar decisiones rápidas de compra o seguimiento.

**Independent Test**: Navegar por las pestañas del Showcase Deportivo; alternar entre *"Mis Joyas de Cartera"*, *"Radar de Oportunidades (Value Picks)"* y *"Estrellas en Racha"*; verificar la fidelidad de las cartas 3D y accionar el botón de compra rápida.

**Acceptance Scenarios**:
1. Las cartas coleccionables incorporan: retrato oficial del futbolista, escudo vectorial de su club, bandera nacional, posición táctica abreviada, score WhoScored con estrella dorada y precio vigente en créditos.
2. El selector de categorías permite conmutar entre:
   - **Mis Joyas (Top Holdings)**: Las 4 posiciones en cartera del usuario con mayor rentabilidad no realizada.
   - **Radar de Oportunidades (Value Picks)**: Los 4 jugadores del catálogo con mejor relación entre rendimiento deportivo WhoScored y cotización de mercado ($\text{Ratio} = \frac{\text{Score}}{\text{Precio}}$).
   - **Estrellas en Racha (Form Leaders)**: Los jugadores con mayor score obtenido en la última jornada evaluada.
3. Cada carta ofrece dos acciones inmediatas: *"Ver Perfil"* (redirige a `/players/{id}`) y *"Comprar Tokens"* (redirige al flujo de compra oficial `/market/buy?player={id}`).

---

### User Story 6 — Mercado Comunitario P2P, Arbitrajes y Feed de Transacciones (Priority: P2)

Como trader que busca oportunidades de arbitraje y quiere mantenerse al tanto de la actividad de la comunidad, quiero identificar órdenes P2P publicadas por debajo del precio oficial y observar las últimas transacciones confirmadas.

**Independent Test**: Visualizar el widget de *Best Deals P2P*; verificar que solo lista órdenes activas cuyo precio por token sea menor al precio oficial vigente, destacando el porcentaje de descuento; revisar el timeline de transacciones recientes y verificar los atajos de acción rápida.

**Acceptance Scenarios**:
1. El bloque **Best Deals P2P** presenta hasta 3 ofertas activas de la comunidad con descuento respecto a la cotización oficial vigente:
   $$\text{Descuento \%} = \frac{P_{\text{oficial}} - P_{\text{p2p}}}{P_{\text{oficial}}} \times 100$$
2. Cada oferta de arbitraje indica la cantidad de tokens disponibles en el lote, el precio unitario ofertado, el precio oficial de referencia y el porcentaje de ahorro destacado con un badge dorado/esmeralda.
3. Un botón *"Aprovechar oferta"* redirige directamente a la orden en el mercado P2P (`/market/p2p?listing={id}`).
4. El bloque **Live Activity Feed** lista las 5 transacciones confirmadas más recientes del usuario autenticado, indicando tipo (`COMPRA` / `VENTA`), jugador, cantidad de tokens, importe total y fecha/hora relativa (ej: *"hace 2 horas"*).
5. La barra de **Quick Actions** contiene accesos directos a las 4 rutas clave del sistema: Mercado Oficial (`/market/buy`), Mercado P2P (`/market/p2p`), Publicar Venta (`/market/sell`) y Mi Cartera (`/portfolio`).

---

### User Story 7 — Estados Vacíos, Carga Inicial y Resiliencia (Priority: P3)

Como usuario nuevo o con conexión intermitente, quiero que el Dashboard maneje de manera elegante los estados de carga y me guíe activamente si aún no he realizado inversiones en la plataforma.

**Independent Test**: Acceder al Dashboard con un usuario recién registrado (sin tokens en cartera ni transacciones); verificar que la interfaz no se rompe, las métricas patrimoniales reflejan el saldo de bienvenida en efectivo, los gráficos presentan un empty state ilustrado y motivador con llamada a la acción hacia el catálogo, y los bloques de mercado general (Ticker, Top Movers, Best Deals) continúan funcionando con normalidad.

**Acceptance Scenarios**:
1. Durante la carga de datos (`isLoading`), todos los bloques despliegan esqueletos de carga (*skeletons*) proporcionales a las tarjetas definitivas, evitando saltos de layout (*CLS = 0*).
2. Para un usuario nuevo con 0 tokens en cartera:
   - El Patrimonio Neto coincide exactamente con su saldo de créditos disponibles en efectivo.
   - El Donut de asignación y el gráfico de benchmark muestran un estado vacío visualmente agradable con el mensaje *"Tu cartera está esperando su primera estrella"* y un botón hacia `/players`.
   - El Ticker, los Top Movers y el Radar de Oportunidades continúan plenamente operativos mostrando la actividad global del mercado.
3. Ante un fallo de comunicación con el backend (HTTP 500/503), la pantalla presenta un panel de contingencia con mensaje amigable y botón de *"Reintentar"* que dispara la revalidación de las consultas sin forzar recarga completa del navegador.

---

## 2. Clarificaciones Incorporadas (Fase Clarify)

A partir de la fase interactiva de clarificación con los stakeholders, se fijan las siguientes decisiones de diseño técnico y funcional:

1. **Reconstrucción de la Curva Histórica de Rendimiento (`/api/dashboard/performance`):**
   - La serie temporal de patrimonio neto del usuario se reconstruye de forma fidedigna y determinista a partir de las transacciones históricas (`market_transactions`).
   - Para cada fecha de cotización disponible en el rango solicitado, el sistema reconstruye la tenencia neta de tokens que el usuario poseía efectivamente en esa fecha y la multiplica por la cotización oficial vigente en dicho instante, reflejando de manera realista la rotación histórica de la cartera frente al índice benchmark FMI.
2. **Política de Actualización y Refresco en Frontend:**
   - Se adopta la política estándar del proyecto con **TanStack Query**: `staleTime` de 30 segundos y revalidación automática al recuperar el foco de la pestaña (`refetchOnWindowFocus: true`).
   - Se descarta el polling continuo forzado para optimizar recursos de red y servidor, ofreciendo datos frescos cada vez que el usuario interactúa o regresa a la aplicación.
3. **Privacidad y Control de Acceso:**
   - La pantalla `/home` es **estrictamente privada** y requiere autenticación JWT activa bajo el `AuthGuard` de la aplicación.
   - Todo acceso anónimo es redirigido a `/login`, asegurando que la experiencia personalizada de cockpit financiero y tenencias opere siempre con una identidad validada.

---

## 3. Requisitos Funcionales y Técnicos

### Backend (`footballmarket.features.dashboard`)

- **FR-001**: El backend MUST exponer el endpoint `GET /api/dashboard/overview` protegido bajo JWT (`bearerAuth`), que retorne en una sola llamada estructurada:
  - `financialSummary`: Patrimonio neto, efectivo disponible, valor de cartera en tokens, P&L total y porcentual, desglose de tokens poseídos, disponibles y reservados.
  - `allocation`: Métricas de asignación por competición (5 ligas) y por posición táctica (4 categorías).
  - `marketPulse`: Capitalización total del mercado de tokens, volumen negociado en 24h, total de transacciones y conteo de órdenes P2P activas.
  - `topMovers`: Lista de 5 *Top Gainers* y 5 *Top Losers* calculados a partir de la última fecha efectiva en `player_quotes`.
  - `valuePicks`: Lista de 4 futbolistas con mayor ratio de rendimiento sobre precio ($\text{Score} / \text{Price}$).
  - `bestP2PDeals`: Lista de hasta 3 ofertas activas en `token_listings` con precio inferior a la cotización oficial vigente del jugador.
  - `recentActivity`: Lista de las últimas 5 transacciones de `market_transactions` asociadas al usuario autenticado.
- **FR-002**: El backend MUST exponer el endpoint `GET /api/dashboard/performance` con parámetro opcional `range` (`1M`, `3M`, `ALL`), reconstruyendo la tenencia histórica del usuario en cada fecha a partir de `market_transactions` y cruzándola con `player_quotes`, junto a la serie normalizada del Football Market Index (FMI).
- **FR-003**: El cálculo de `netWorth` MUST ser determinista y exacto: saldo en `user_credits` más sumatoria de `(quantity * current_price)` de todas las tenencias abiertas del usuario.
- **FR-004**: Los importes monetarios en el backend MUST gestionarse y persistirse en números enteros que representen centésimas de crédito, manteniendo la coherencia de precisión del resto del dominio financiero.
- **FR-005**: Las consultas de Top Movers y Best Deals MUST ejecutarse mediante queries SQL nativas optimizadas con índices existentes en `player_quotes` y `token_listings`.
- **FR-006**: Todos los contratos de endpoint y DTOs MUST estar documentados con anotaciones OpenAPI (Swagger v3).

### Frontend (`frontend/src/features/dashboard`)

- **FR-007**: La ruta protegida `/home` MUST renderizar el componente `DashboardPage` integrado en el `AppLayout`, exclusivamente accesible para usuarios autenticados.
- **FR-008**: Los importes monetarios recibidos en centésimas MUST ser transformados por mappers en frontend dividiendo por 100 y formateados mediante `formatCredits`.
- **FR-009**: Todos los estilos visuales MUST utilizar de forma estricta los tokens cromáticos centralizados de `@/styles/abstracts/tokens/_colors.scss`, sin declarar colores hexadecimales literales ni funciones prohibidas.
- **FR-010**: Las visualizaciones gráficas (Donut de asignación, Curva de rendimiento temporal y Sparklines) MUST implementarse en **SVG nativo propio**, sin incorporar librerías externas pesadas (Chart.js, Recharts, etc.), garantizando compatibilidad con React 19 y velocidad de carga instantánea.
- **FR-011**: Los componentes gráficos MUST incluir descripciones semánticas accesibles (`aria-label`, `<desc>`, `<title>`) y una tabla de datos de respaldo para lectores de pantalla.
- **FR-012**: Las cartas coleccionables MUST reutilizar los componentes atómicos del proyecto: `PlayerPortrait`, `TeamBadge`, `CompetitionLogo` y `NationalityFlag`.
- **FR-013**: Las peticiones de datos MUST gestionarse a través de TanStack Query con `staleTime: 30000` (30 segundos) y revalidación en foco (`refetchOnWindowFocus: true`).

---

## 4. Criterios de Aceptación (Checklist de Verificación)

- [ ] **CA-01:** `GET /api/dashboard/overview` responde con `200 OK` y payload completo para cualquier usuario autenticado con sesión válida.
- [ ] **CA-02:** Solicitudes no autenticadas hacia `/api/dashboard/*` son rechazadas con `401 Unauthorized`.
- [ ] **CA-03:** El valor de `netWorth` coincide exactamente con la suma del saldo libre de créditos y la valoración de mercado de las tenencias abiertas.
- [ ] **CA-04:** `topGainers` y `topLosers` reflejan exactamente la diferencia entre `price` y `previous_price` de la última cotización efectiva de cada jugador.
- [ ] **CA-05:** `bestP2PDeals` lista únicamente publicaciones con `status = 'ACTIVE'`, no expiradas, y con `unit_price < official_quote_price`.
- [ ] **CA-06:** La pantalla en `/home` carga armónicamente los 6 bloques visuales en resolución desktop (1920 × 913 px y adaptativo $\ge 1024$ px).
- [ ] **CA-07:** El Market Ticker superior presenta desplazamiento fluido y pausa al interactuar con el mouse.
- [ ] **CA-08:** El Donut Chart permite conmutar dinámicamente entre vista por Liga y por Posición táctica sin saltos visuales ni parpadeos.
- [ ] **CA-09:** El gráfico de benchmark grafica fielmente la evolución temporal de la cartera y del índice FMI.
- [ ] **CA-10:** Las cartas estilo Sorare exponen retrato, escudo, bandera, score y precio, con enlaces operativos a compra y detalle.
- [ ] **CA-11:** Un usuario nuevo sin tokens visualiza su saldo en efectivo en Net Worth y un estado vacío amigable en los gráficos de cartera sin arrojar excepciones en consola.
- [ ] **CA-12:** Los botones de Quick Actions redirigen correctamente a `/market/buy`, `/market/p2p`, `/market/sell` y `/portfolio`.
- [ ] **CA-13:** El código pasa el formateador y linter Biome en frontend (`pnpm biome:check`) y compila limpiamente en backend con Gradle (`./gradlew compileJava classes`).

---

## 5. Edge Cases y Contingencias

1. **Usuario sin tenencias ni transacciones (First-Time User):**
   - El saldo de créditos muestra el bono de bienvenida (ej: 10,000 créditos).
   - `totalCurrentValue` de la cartera es 0, P&L es 0 y los tokens totales son 0.
   - El gráfico Donut y el gráfico de rendimiento exhiben un estado vacío ilustrado con una llamada a la acción hacia el catálogo de jugadores (`/players`).
   - Los widgets globales (Ticker, Top Movers, Radar de Oportunidades y Ofertas P2P) muestran la información del mercado normalmente.
2. **Posición abierta con cotización no vigente o nula (Valoración Incompleta):**
   - Conforme a las reglas de SPEC-014/015, si algún jugador en cartera carece de cotización vigente, la valoración se marca como incompleta (`valuationComplete = false`).
   - El Dashboard muestra el importe de las posiciones con cotización disponible y acompaña el Net Worth con un badge informativo: *"Valoración parcial (cotización en actualización)"*.
3. **Primer recálculo del sistema (Sin cotizaciones previas):**
   - Si los jugadores solo tienen su cotización inicial (`origin = 'INITIAL'`) y `previous_price` es nulo, no existen deltas históricos.
   - El bloque Top Movers muestra los jugadores con mayor y menor cotización absoluta o score WhoScored, rotulando *"Precios base de emisión"* sin deltas porcentuales.
4. **Ausencia de ofertas con descuento en el mercado P2P:**
   - Si ninguna publicación P2P ofrece un precio inferior a la cotización oficial, el bloque *Best Deals* muestra un mensaje neutro: *"No hay órdenes con descuento en este momento"* junto a un botón *"Ver todas las ofertas P2P"* hacia `/market/p2p`.
5. **Cartera concentrada en una única posición (100% en un jugador):**
   - El gráfico Donut dibuja un círculo completo (360 grados) de un solo color correspondiente a la liga/posición de ese jugador, evitando divisiones por cero en el cálculo trigonométrico de arcos SVG.
   - Se activa el badge de concentración de cartera alta.
6. **Fallo de red o caída de la base de datos:**
   - TanStack Query reintenta automáticamente según su política de backoff. Si persiste el error, se muestra una tarjeta con botón de reintento manual que preserva el Navbar y el shell de la aplicación.

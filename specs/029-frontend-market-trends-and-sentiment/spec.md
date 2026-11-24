# Feature Specification: Tablero de tendencias de mercado y visualización de demanda comunitaria

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/029-frontend-market-trends-and-sentiment`  
**Estado**: Especificación viva del sistema.  
**Issue de origen**: [#97](https://github.com/YoelSLA/football-market/issues/97) (`SPEC-029 | Tablero de tendencias de mercado y visualización de demanda comunitaria`).  
**Requerimiento académico**: `REQ-S2-FEAT-ADD` (Visualización de datos mediante componentes gráficos en el frontend, métricas financieras de mercado y experiencia interactiva).  
**Referencias normativas**:
- [`tp/enunciado.md`](../../tp/enunciado.md) (Valoración de jugadores, gráficos de tendencias y experiencia financiera)
- [`tp/entregas.md`](../../tp/entregas.md) (Requisitos de entrega de visualización gráfica en frontend y consistencia visual)
- [`specs/028-market-trends-and-demand-pricing/spec.md`](../028-market-trends-and-demand-pricing/spec.md) (Métricas de tendencias, ventana de actividad y cálculo del factor de demanda en cotizaciones)
- [`specs/028-market-trends-and-demand-pricing/contracts/market-trends-api.md`](../028-market-trends-and-demand-pricing/contracts/market-trends-api.md) (Contrato HTTP `GET /api/market/trends`)
- [`specs/028-market-trends-and-demand-pricing/contracts/market-demand-recalculation-api.md`](../028-market-trends-and-demand-pricing/contracts/market-demand-recalculation-api.md) (Contrato de recálculo con factor de demanda y auditoría)
- [`specs/009-player-detail/spec.md`](../009-player-detail/spec.md) (Página de detalle individual de jugador `PlayerDetailPage` y visualización de cotizaciones)
- [`specs/010-buy-tokens/spec.md`](../010-buy-tokens/spec.md) y [`specs/026-market-instant-buyback-and-p2p-pricing/spec.md`](../026-market-instant-buyback-and-p2p-pricing/spec.md) (Catálogo oficial y mercado P2P)

---

## Clarifications

### Session 2026-11-24

- Q: ¿Cuál debe ser la política de refresco y actualización de datos para el tablero de tendencias y sentimiento de mercado? (FR-007) → A: Sincronización combinada: auto-refresco en segundo plano cada 60 segundos mientras la pestaña esté activa y visible, junto con un botón accesible de refresco manual para que el usuario pueda forzar la actualización inmediata en cualquier momento.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Tablero de Tendencias y Sentimiento del Mercado (Priority: P1)

Como usuario inversor de Football Market, quiero contar con un tablero visual dedicado a las tendencias del mercado dentro de la sección de Mercado, para consultar de forma inmediata el sentimiento general de compra versus venta, las métricas globales operadas y los activos más transaccionados por la comunidad.

**Why this priority**:
Constituye el núcleo funcional prioritario de la entrega (P1 / MVP): entrega el valor analítico macro del mercado y satisface directamente el requerimiento de visualización gráfica en el frontend, orientando la toma de decisiones de inversión del usuario.

**Independent Test**:
Navegar a la ruta `/market/trends` desde la pestaña "Tendencias" del módulo de Mercado. Comprobar que se renderiza la barra visual proporcional de compras vs. ventas con colores semánticos, el rótulo de sesgo de mercado (`Alcista`, `Bajista` o `Neutral`), las tarjetas de métricas cuantitativas consolidadas (volumen en créditos y cantidad de transacciones), el botón de refresco manual y la lista de hasta los 5 futbolistas más demandados (*Hot Assets*).

**Acceptance Scenarios**:

1. **Given** un usuario autenticado que navega por el sistema, **When** accede a la sección de Mercado y selecciona la pestaña "Tendencias" (`/market/trends`), **Then** se despliega el tablero de análisis macro de mercado con layout responsivo.
2. **Given** transacciones registradas dentro de la ventana de análisis vigente, **When** se cargan los datos de tendencias, **Then** la barra gráfica de sentimiento exhibe la proporción porcentual de compras frente a ventas (ej. 75% compras en verde vs. 25% ventas en rojo) acompañada del indicador de estado correspondiente (`Alcista / BULLISH`, `Bajista / BEARISH` o `Neutral / NEUTRAL`).
3. **Given** datos de actividad disponibles, **When** el usuario visualiza el resumen macro, **Then** se muestran dos tarjetas de métricas cuantitativas clave:
   - *Volumen total operado:* monto total de créditos transaccionados expresado con dos decimales exactos.
   - *Transacciones totales:* cantidad total de operaciones de compraventa concretadas en la ventana.
4. **Given** futbolistas con compras registradas en la ventana, **When** se consulta el bloque de "Jugadores Más Demandados" (*Hot Assets*), **Then** se despliega un ranking de hasta 5 futbolistas ordenados por mayor demanda, mostrando para cada uno su identidad (nombre, club, silueta/retrato), la cantidad de tokens adquiridos y el volumen en créditos acumulado.
5. **Given** un futbolista en el ranking de más demandados, **When** el usuario hace clic o activa su tarjeta en el ranking, **Then** la interfaz navega directamente a la página de detalle individual del jugador (`/players/:id`).
6. **Given** una ventana de tiempo sin transacciones (mercado en reposo sin actividad reciente), **When** se consulta el tablero, **Then** se presenta un estado neutral informativo ("Mercado en calma: sin operaciones registradas en las últimas 24 horas"), con porcentajes en estado neutral y ranking vacío amigable sin errores de cálculo.
7. **Given** que la información se encuentra cargando o el usuario acciona el botón de refresco manual, **When** se consultan o sincronizan los datos (incluyendo el ciclo de auto-refresco de 60 segundos con pestaña activa), **Then** se visualizan bloques de carga (*skeletons*) o feedback de refresco que respetan la geometría final sin parpadeos ni bloqueos.

---

### User Story 2 — Insignias de Alta Demanda en Catálogos Oficial y P2P (Priority: P2)

Como comprador que explora el Catálogo Oficial de emisión o el Mercado P2P secundario, quiero identificar de un vistazo mediante un distintivo visual llamativo qué futbolistas están experimentando una alta demanda comunitaria para descubrir oportunidades y tomar decisiones informadas de adquisición.

**Why this priority**:
Conecta la inteligencia de mercado macro generada en tendencias con los puntos transaccionales reales del catálogo oficial y del mercado secundario, incrementando la tracción y la liquidez de los activos más codiciados.

**Independent Test**:
Navegar al Catálogo Oficial (`/market/buy`) y al Mercado P2P (`/market/p2p`); verificar que aquellos jugadores que figuren en el Top 5 de más demandados de tendencias exhiben una insignia destacada (*"🔥 Alta Demanda"*) en sus respectivas tarjetas de jugador y publicaciones, mientras que los demás se presentan de manera estándar.

**Acceptance Scenarios**:

1. **Given** un futbolista listado en el Catálogo Oficial que forma parte del ranking vigente de más demandados, **When** el usuario visualiza su tarjeta en `/market/buy`, **Then** la tarjeta exhibe una insignia distintiva visual *"🔥 Alta Demanda"*.
2. **Given** una publicación de venta en el Mercado P2P correspondiente a un futbolista en tendencia comunitaria, **When** el usuario navega por `/market/p2p`, **Then** la tarjeta de la publicación incluye la insignia distintiva *"🔥 Alta Demanda"*.
3. **Given** un futbolista que no pertenece al grupo de más demandados de la ventana, **When** se visualiza su tarjeta en cualquiera de los catálogos, **Then** se presenta con el diseño regular sin la insignia de alta demanda.
4. **Given** una tarjeta con la insignia *"🔥 Alta Demanda"*, **When** el usuario posa el cursor o enfoca el badge mediante teclado, **Then** se despliega un tooltip contextual explicando que el activo se encuentra en el Top 5 de mayor demanda transaccional de la comunidad.
5. **Given** que ocurre un fallo transitorio de red al consultar las tendencias para los catálogos, **When** se renderizan las tarjetas de mercado, **Then** los catálogos continúan plenamente operativos mostrando las tarjetas sin badges, sin interrumpir la capacidad del usuario de operar compras o explorar precios.

---

### User Story 3 — Desglose de Cotización: Rendimiento Deportivo vs. Demanda Comunitaria (Priority: P2)

Como inversor analítico que examina la página de detalle individual de un jugador (`PlayerDetailPage`), quiero conocer con transparencia cómo se compone la cotización vigente de su token, distinguiendo la variación originada por su rendimiento deportivo real de la variación provocada por la presión de demanda del mercado.

**Why this priority**:
Proporciona transparencia y valor formativo financiero al usuario al clarificar la fórmula híbrida de cotizaciones de Football Market, diferenciando el valor fundamental (score WhoScored) de la presión comunitaria de oferta y demanda.

**Independent Test**:
Acceder a la página de detalle (`/players/:id`) de un jugador que cuente con cotización recalculada. Comprobar que en el encabezado o bloque de cotización se expone un indicador interactivo o bloque explicativo que desglosa con exactitud el Aporte Deportivo (+X.XX%) y la Presión de Mercado (+Y.YY%), explicando de forma pedagógica la variación observada.

**Acceptance Scenarios**:

1. **Given** un usuario que consulta la página de detalle individual de un jugador (`/players/:id`), **When** visualiza el bloque de cotización actual, **Then** se exhibe junto a la cotización un botón, enlace o tooltip interactivo con el rótulo "Desglose de Cotización" o "Ver Factores".
2. **Given** que el usuario interactúa con el elemento de desglose de cotización, **When** se despliega el panel informativo, **Then** se presentan con claridad ambos factores:
   - *Aporte Deportivo (Rendimiento WhoScored):* porcentaje de variación atribuible a la performance deportiva en la temporada (+X.XX% / -X.XX%).
   - *Presión de Mercado (Demanda Comunitaria):* porcentaje de variación atribuible al balance neto de compras y ventas de los usuarios (+Y.YY% / -Y.YY%).
3. **Given** una cotización generada mediante un recálculo exclusivo de demanda de mercado (sin actualización de score deportivo), **When** se consulta el desglose en el detalle del jugador, **Then** el aporte deportivo se expone como neutro (+0.00%) y se evidencia que el ajuste proviene íntegramente de la presión transaccional del mercado.
4. **Given** una cotización inicial histórica o previa al inicio de operaciones de mercado, **When** el usuario visualiza el desglose, **Then** el sistema indica que la cotización responde a la valuación base inicial sin variación por demanda de mercado.

---

### User Story 4 — Adaptabilidad Móvil, Resiliencia y Accesibilidad (Priority: P3)

Como usuario que accede a Football Market desde dispositivos móviles, tabletas o utilizando tecnologías de asistencia, quiero que el tablero de tendencias, los badges y los desgloses se adapten con fluidez estética a cualquier tamaño de pantalla y proporcionen alternativas textuales accesibles.

**Why this priority**:
Garantiza el cumplimiento de los estándares de accesibilidad visual, contraste del design system y cobertura responsiva integral del frontend sin dejar brechas de usabilidad.

**Independent Test**:
Verificar el tablero y los componentes en anchos de pantalla reducidos (320px a 480px), intermedios (768px) y amplios (1024px+); confirmar la inexistencia de scroll horizontal indeseado, comprobar contrastes cromáticos y validar que un lector de pantalla verbaliza adecuadamente los porcentajes de compras/ventas, el estado del sesgo y el badge de demanda.

**Acceptance Scenarios**:

1. **Given** un dispositivo con ancho de pantalla móvil (desde 320px), **When** se accede a `/market/trends`, **Then** los elementos del tablero se reorganizan verticalmente en una sola columna con espaciados confortables y sin desbordamientos horizontales.
2. **Given** una tecnología de asistencia (lector de pantalla o navegación por teclado), **When** se recorren la barra de sentimiento y los badges de alta demanda, **Then** cuentan con atributos de accesibilidad (`aria-label`, descripciones textuales) que transmiten el significado exacto de los porcentajes y del estado alcista, bajista o neutral sin depender únicamente del color.

---

## Edge Cases

- **Ausencia total de actividad en la ventana de mercado:** Si no existen transacciones en las últimas 24 horas, el backend reporta `topHotPlayers: []`, volúmenes en cero y porcentajes nulos. El frontend DEBE presentar un estado vacío elegante y pedagógico, sin arrojar NaN%, divisiones por cero ni barras gráficas quebradas.
- **Mercado unidireccional (100% compras o 100% ventas):** La barra de sentimiento DEBE renderizarse armónicamente cubriendo el 100% con su borde redondeado respectivo, sin colisiones de texto ni desbordes en los extremos.
- **Top de demandados con menos de 5 futbolistas:** Si solo 1, 2 o 3 jugadores registran transacciones de compra en la ventana, el ranking DEBE renderizar únicamente las tarjetas de los futbolistas existentes, ajustando limpiamente la grilla sin huecos rotos.
- **Fallo de conectividad o error 500 en endpoint de tendencias:** La pantalla de tendencias DEBE exhibir un banner o tarjeta de error informativo con acción clara de "Reintentar", permitiendo al usuario volver a solicitar los datos sin recargar manualmente toda la página web.
- **Catálogos con fallo en tendencias:** Si la consulta de tendencias falla mientras el usuario explora el Catálogo Oficial o P2P, los catálogos DEBEN seguir operando normalmente mostrando las tarjetas regulares sin los badges de demanda, asegurando que un fallo analítico nunca bloquee la operativa comercial.
- **Pestaña en segundo plano durante auto-refresco:** Si el usuario minimiza el navegador o cambia a otra pestaña, el temporizador de auto-refresco DEBE pausarse o evitar peticiones innecesarias en segundo plano, reanudando la consulta fresca al volver a enfocar la vista activa.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE proveer una sección dedicada a las tendencias del mercado, accesible bajo la ruta `/market/trends` y representada con su correspondiente pestaña en la barra de navegación del módulo de Mercado.
- **FR-002**: El tablero de tendencias DEBE visualizar el sentimiento colectivo del mercado mediante una barra gráfica proporcional que represente visual y porcentualmente la relación entre compras y ventas de la ventana de análisis vigente.
- **FR-003**: La barra de sentimiento DEBE indicar de forma cromática y textual el sesgo predominante del mercado: Alcista (`BULLISH` / compra mayoritaria), Bajista (`BEARISH` / venta mayoritaria) o Neutral (`NEUTRAL` / paridad o reposo).
- **FR-004**: El tablero DEBE exhibir tarjetas de métricas cuantitativas que resuman el volumen total operado en créditos (dividido por 100 y formateado con dos decimales) y la cantidad total de transacciones completadas en la ventana.
- **FR-005**: El sistema DEBE presentar un ranking destacado de hasta los 5 futbolistas más demandados de la comunidad (*Hot Assets*), informando para cada uno su nombre, club, tokens comprados y volumen operado en créditos.
- **FR-006**: Las tarjetas de los futbolistas en el ranking de más demandados DEBEN ser interactivas y permitir la navegación directa hacia la página de detalle individual del jugador correspondiente (`/players/:id`).
- **FR-007**: El sistema DEBE sincronizar periódicamente los datos de tendencias mediante una política combinada: auto-refresco en segundo plano cada 60 segundos mientras la pestaña se mantenga activa y visible, junto con un botón accesible de refresco manual que permita forzar la actualización inmediata. Mientras se obtienen o refrescan los datos, se DEBEN exhibir esqueletos visuales (*skeletons*) proporcionales.
- **FR-008**: El Catálogo Oficial (`/market/buy`) y el Mercado P2P (`/market/p2p`) DEBEN identificar si un futbolista exhibido figura entre los activos más demandados del momento y mostrar sobre su tarjeta una insignia visual distintiva (*"🔥 Alta Demanda"*).
- **FR-009**: La insignia de *"🔥 Alta Demanda"* DEBE incluir un texto descriptivo o tooltip contextual accesible que explique que el futbolista pertenece al Top 5 de mayor demanda transaccional comunitaria.
- **FR-010**: En la página de detalle individual del jugador (`PlayerDetailPage`), la sección de cotización DEBE proporcionar un desglose comprensible de los componentes de variación del precio, diferenciando el impacto del aporte deportivo (rendimiento WhoScored) de la presión de demanda comunitaria (balance neto de compras y ventas).
- **FR-011**: Cuando una cotización provenga de un recálculo exclusivo de demanda de mercado sin actualización deportiva, el desglose DEBE reflejar el aporte deportivo como neutro (+0.00%) y evidenciar la variación originada por la presión comunitaria.
- **FR-012**: La interfaz completa de tendencias, insignias y desgloses DEBE adaptarse fluidamente a dispositivos móviles, tabletas y computadoras de escritorio, y cumplir con los criterios de accesibilidad semántica y contraste visual del design system.

---

### Key Entities

- **Resumen de Tendencias de Mercado (Market Trends Summary)**: Entidad que consolida la actividad del mercado en una ventana temporal predeterminada (24 horas); agrupa la ventana de cálculo (`startsAt`, `endsAt`), porcentaje de compras, porcentaje de ventas, sesgo del mercado (`BULLISH`, `BEARISH`, `NEUTRAL`), total de transacciones, total de tokens transaccionados y total de créditos operados.
- **Activo Más Demandado (Hot Asset / Top Player)**: Representación de un futbolista destacado por volumen de adquisición comunitaria en la ventana; contiene identificador del jugador (`playerId`), nombre (`playerName`), cantidad de tokens adquiridos (`boughtTokens`) y volumen total en créditos operado en compras (`buyVolume`).
- **Desglose de Cotización (Quote Valuation Breakdown)**: Descomposición pedagógica de los factores concurrentes en la cotización vigente de un jugador; incluye cotización actual, cotización previa, variación porcentual total, factor porcentual de rendimiento deportivo y factor porcentual de demanda comunitaria.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden consultar el sentimiento del mercado y el ranking de activos más demandados en la pantalla de tendencias con un tiempo de carga percibido inferior a 1 segundo tras la recepción de la respuesta del servicio.
- **SC-002**: El 100% de los futbolistas que figuren en el ranking vigente de más demandados exhiben la insignia *"🔥 Alta Demanda"* de forma consistente y sincronizada en el Catálogo Oficial y en el Mercado P2P.
- **SC-003**: En la página de detalle individual del jugador, los usuarios pueden comprender con una única interacción el desglose porcentual exacto entre el componente de rendimiento deportivo y el componente de demanda de mercado.
- **SC-004**: La interfaz del tablero de tendencias es 100% responsiva, adaptándose fluidamente y sin scrolls horizontales involuntarios en pantallas desde 320 píxeles de ancho hasta monitores 4K.
- **SC-005**: Todas las representaciones gráficas del tablero (barra de sentimiento y badges) proveen alternativas textuales y etiquetas de accesibilidad completas, permitiendo su interpretación autónoma por lectores de pantalla y navegación por teclado.

---

## Assumptions

- La ventana temporal de cálculo de tendencias (por defecto 24 horas) y la lógica de cálculo del factor de demanda son gestionadas de forma centralizada por el backend a través del endpoint `GET /api/market/trends` y los servicios de recálculo (`SPEC-028`).
- La autenticación mediante sesión de usuario con token JWT activo es requerida para consultar las métricas de mercado; en caso de sesión no iniciada o vencida, el sistema redirige al login conforme a las directivas globales de seguridad de la aplicación.
- Los importes monetarios provistos en centésimas de crédito por el backend son normalizados en el frontend dividiendo por 100 para exhibir valores decimales con símbolo de créditos (`¢`).
- En caso de empate en el ranking de demanda, el ordenamiento respeta el criterio provisto por el backend (tokens comprados descendente, volumen descendente, identificador ascendente).
- Las insignias de alta demanda en los catálogos se alimentan del conjunto de jugadores obtenido en la consulta de tendencias más reciente disponible en caché de cliente.

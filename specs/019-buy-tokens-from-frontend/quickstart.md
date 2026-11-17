# Quickstart: validación manual de SPEC-019

**Ejecución a cargo del usuario.** El frontend no tiene tests automatizados y el agente no ejecuta tests. Esta guía valida comportamiento observable después de implementar y no acredita aceptación por sí sola.

## Prerrequisitos

- Backend disponible con `GET /api/market/official`, `GET /api/players/filters`, `GET /api/listings` y `POST /api/orders/buy`.
- Usuario no administrador con sesión válida, créditos y al menos un caso oficial comprable.
- Datos con jugador cotizado, jugador sin cotización, cotización inicial sin precio anterior y publicaciones P2P.
- Frontend iniciado por el usuario desde `frontend/` mediante `npm run dev`.
- DevTools disponibles para observar Network y TanStack Query sin exponer JWT ni datos sensibles.

## Gate de implementación frontend

1. Validar primero lógica, estado y estructura semántica sin exigir fidelidad visual final.
2. Confirmar DTOs, URL, query keys, filtros, compra e invalidaciones.
3. Aprobar explícitamente el gate humano de Fase 1.
4. Recién después validar SCSS y fidelidad al wireframe en Fase 2.

## Mercado Oficial: catálogo y contrato

1. Abrir `/market/buy`: verificar una única solicitud `GET /api/market/official?page=0&size=8&sort=id,asc` equivalente y ninguna solicitud individual `/players/{id}/quotes/current`.
2. Comparar la respuesta con las cards: identidad deportiva, precio, variación, precio anterior y `effectiveDate`. Confirmar que no se muestran `performanceScore`, `multiplier`, `calculationOrigin` ni `calculatedAt`.
3. Para `quote: null`, comprobar card del mismo tamaño, «Sin cotización» y compra deshabilitada.
4. Para variación `0`, comprobar indicador neutro real. Para `previousPrice` o `variationPercentage` nulos, comprobar ausencia neutral de referencia, no «0 %» inventado.
5. Verificar fallbacks accesibles cuando equipo, liga, nacionalidad, escudo o retrato sean nulos.

## Filtros, URL y paginación

1. Modificar búsqueda, liga, equipo, posición, nacionalidad, rango y orden: cada control actualiza automáticamente URL y consulta; no existen filtro de cotización, selector de tamaño ni botón Aplicar.
2. Seleccionar una liga: solo aparecen sus equipos. Cambiarla o limpiarla elimina un equipo incompatible.
3. Escribir rápidamente en búsqueda: observar una única consulta después del debounce, no una por pulsación.
4. Introducir mínimo mayor que máximo o valor negativo: aparece error junto a los campos y no se emite una solicitud inválida.
5. Cambiar cualquier filtro u orden desde una página posterior: la URL vuelve a `page=0`.
6. Verificar el tamaño oficial fijo de 8: la respuesta y la grilla muestran como máximo dos filas de cuatro sin scroll vertical en el viewport desktop aprobado. El total aparece solo sobre la grilla.
7. Recargar y recorrer Back/Forward: controles y resultados se reconstruyen desde `name`, `leagueId`, `teamId`, `position`, `nationality`, `minPrice`, `maxPrice`, `sort` y `page`.
8. Editar manualmente URL con `hasQuote`, `size`, página, sort o tipos inválidos: la UI retira los parámetros heredados mediante `replace`, aplica defaults seguros y no conserva un criterio imposible como válido.
9. Provocar error de red/500: mostrar error con reintento. Una combinación válida sin resultados debe mostrar «Sin resultados», distinta del mercado globalmente vacío.

## Compra oficial

1. Abrir un jugador cotizado: no aparece un GET adicional; el modal usa los datos consolidados y conserva URL, filtros, página y scroll.
2. Confirmar que el modal solo muestra identidad deportiva, precio actual, variación, precio anterior, vigencia, cantidad y resumen.
3. Verificar cantidad inicial `1`; `−/+` cambian una unidad y 1/5/10/50/100 reemplazan el valor por una cantidad absoluta.
4. Probar vacío, letras, decimal, 0 y 101: error inline, confirmación bloqueada y sin POST. Probar `10`: payload numérico `{ playerId, quantity: 10 }`.
5. Hacer clic repetido durante procesamiento: una sola solicitud. Cancelar, overlay y Escape permanecen bloqueados durante envío.
6. En éxito, comparar transacción, cantidad, `unitPrice`, `totalAmount` y `executedAt` con la respuesta. El precio ejecutado prevalece sobre la estimación.
7. Inspeccionar invalidaciones: prefijo oficial completo, usuario actual, resumen y todas las holdings. No invalidar datos demo ni generar GET individuales de cotización.
8. Verificar mensajes seguros para los códigos de [contrato de compra](contracts/buy-tokens-frontend.md); 401 activa sesión y no restaura el modal.

## Layout aprobado

1. En 1920 × 913 px comprobar navbar de 60 px, navegación secundaria de 48 px y página interior con 8 px de padding por lado.
2. Confirmar encabezado compacto sin total, filtros horizontales en dos filas, barra única de resultados, grilla desplazable y paginación fija fuera del scroll.
3. A 1920 y 1440 px verificar cuatro columnas; entre 1100 y 1439 px verificar tres columnas legibles.
4. En 375, 768 y 1440 px comprobar ausencia de scroll horizontal, controles operables y modal contenido con scroll propio cuando haga falta.
5. Validar foco inicial en cantidad, trampa de foco, Tab/Shift+Tab, Escape cuando está permitido, anuncios de carga/error y restitución de foco al botón Comprar.
6. Contrastar cajas, dimensiones, gaps y estados con `docs/wireframes/official-market.md` sin modificar lógica validada durante la fase visual.

## Mercado P2P: catálogo, URL y detalle

1. Abrir `/market/p2p`: comprobar una única lectura `GET /api/listings` sin query parameters y la lectura vigente de metadatos deportivos.
2. Verificar agrupación por jugador, conteo de publicaciones, suma de tokens y precio mínimo sobre la colección completa antes de filtrar o paginar.
3. Escribir en búsqueda: `search` se refleja en la URL después de 300 ms de debounce. Cambiar liga, posición u orden: `league`, `position` y `sort` se reflejan inmediatamente.
4. Desde una página posterior, cambiar filtro u orden: `page` vuelve a `0`. Navegar páginas conserva tamaño local fijo de 12 y actualiza `page` sin emitir otro GET de listings.
5. Recargar, compartir la URL y recorrer Back/Forward: filtros, orden y página reconstruyen el mismo catálogo.
6. Abrir un jugador: la URL agrega `player=<id>` conservando los demás parámetros; el detalle reemplaza catálogo/filtros/paginación y muestra todas las publicaciones ya recibidas, ordenadas por precio, vencimiento e ID.
7. Cerrar el detalle: se elimina solo `player` y se restaura el catálogo previo. Un jugador inexistente o sin ofertas muestra estado vacío con regreso, sin consulta adicional `/listings/{id}`.

## Compra P2P

1. Abrir una publicación: cantidad fija, precio unitario, total y vencimiento; sin campo de cantidad ni identidad del vendedor.
2. Confirmar que el payload contiene exclusivamente `{ listingId }` y que clics repetidos producen una sola solicitud.
3. En éxito, comparar los datos mostrados con `OrderBuyResponseDTO`, comprobar invalidaciones de listings, usuario, resumen y holdings, y confirmar que el estado permanece hasta cierre explícito.
4. Simular publicación vendida, vencida o retirada: mostrar indisponibilidad, refrescar catálogo y detalle y no afirmar compra.
5. Simular error de red sin respuesta: no reintentar el POST automáticamente ni mostrar éxito o rechazo concluyente.
6. Confirmar que ningún cambio P2P altera payload, modal, invalidaciones o navegación del Mercado Oficial.

## Registro de evidencia

Registrar manualmente viewport, URL, request/response saneados, resultado observable y desviaciones. No incluir tokens, cookies ni cuerpos sensibles. El build, si luego se ejecuta bajo las reglas del área, no sustituye estos escenarios ni la aprobación visual humana.

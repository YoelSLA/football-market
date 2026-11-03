# Quickstart: validación manual de publicación de ventas desde el frontend (SPEC-021)

**Ejecuta el usuario, no el agente.** Frontend exento de tests automatizados. Requiere backend con mercado inicializado y cotizaciones reales persistidas, JWT de usuario no superusuario, tenencia de tokens de un jugador visible, y `npm run dev` desde `frontend/` si corresponde. El build autorizado al agente para cambios futuros de código es `npm run build` (no equivale a testing).

## Prerrequisitos

- Backend levantado con mercado, cotizaciones y tenencias iniciales (SPEC-010/SPEC-011).
- Usuario autenticado con rol `USER` (no `SUPERUSER`) y tenencia de tokens de al menos un jugador visible en el catálogo.
- Cotización vigente disponible para ese jugador (`GET /api/players/{id}/quotes/current` devuelve 200 con `price` positivo).

## US1: mercado y apertura del modal de venta

1. Sin sesión, visitar `/market/sell`: `AuthGuard` redirige al login. Con sesión, visitar Mercado > Vender tokens: listado paginado de jugadores visibles, estado vacío/error con reintento. Un jugador visible con tenencia totalmente reservada sigue en el listado mostrando cero tokens disponibles (FR-F02).
2. Abrir el modal de venta de un jugador con cotización vigente: se muestra nombre, equipo, liga, posición, precio real de GET current, `calculatedAt` del backend; comparar precio e instante con respuesta HTTP. Nunca mostrar dato o `date` de la serie demo de players.
3. Cancelar, pulsar ESC y hacer click fuera: en los tres casos no hay POST, se mantiene URL, página, scroll, pestaña activa y valores de cualquier filtro existente del mercado; el mercado permanece visible bajo el overlay y sin reflow.
4. Al abrir cada nueva publicación el campo cantidad muestra `1`; con cotización disponible, el resumen muestra precio e importe estimados, claramente no definitivos. Confirmar inicia una relectura explícita de `GET /api/players/{id}/quotes/current` antes del POST. Borrar el campo o introducir `0`, `101`, `-5`, `3.5`, letras o exponentes: error inline, Confirmar deshabilitado y sin POST. `"10"`, `"1"` y `"100"` son entradas textuales válidas que deben enviarse como números 10, 1 y 100.
5. A 100 centésimas, cantidad inicial 5: «Importe estimado: 5,00 créditos (500 centésimas)». No presentar estimación como precio congelado ni garantizado.

## US2: cotización, cambio de precio y confirmación

1. GET current devuelve `404 QUOTE_NOT_FOUND`, precio inválido o falla de red: indicar falta de cotización/error; Confirmar deshabilitado; no sustituirla por demo.
2. Cambio de precio antes del POST: al activar Confirmar se fuerza un nuevo GET current. Si difiere del precio revisado, se actualiza la estimación y no se envía el POST hasta que el usuario la revise y confirme otra vez. Puede ocurrir otro cambio entre ese GET y el POST: el backend aplica el precio vigente y la respuesta del POST es autoritativa; mostrar esos valores definitivos y expiresAt en éxito.
3. Confirmar una publicación válida: único POST con `{playerId, quantity}` numérico, aun cuando el campo contenga texto `"10"`; sin `listingId`. Spinner y controles de cierre deshabilitados mientras envía (prevención de duplicados, CA-15, FR-F08).
4. Antes del POST, no presentar vencimiento definitivo; informar únicamente la regla de duración de 24 h si corresponde. En éxito: confirmación dentro del modal con los datos definitivos de la respuesta: jugador, cantidad publicada, unitPrice, totalAmount y expiresAt (FR-F09). La confirmación permanece visible hasta que el usuario navegue o refresque. El precio aplicado es el del POST (backend autoritativo), puede diferir de la estimación previa.
5. Cotejar uno por uno los códigos POST del contrato (`INVALID_REQUEST`, `INVALID_QUANTITY`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `SUPERUSER_CANNOT_SELL`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INTERNAL_ERROR`) con la tabla del [contrato](contracts/publish-token-sales-frontend.md): cada uno tiene mensaje definido sin detalles sensibles. Para desconocido, red/500: fallback genérico; 401: redirección sin recuperar modal. `QUOTE_UNAVAILABLE` no se interpreta como cambio de precio. Provocar casos manualmente cuando sea posible, sin afirmar que todos los errores hayan sido reproducidos.

## US3: errores, reintento y tenencias

1. Ante error de validación o rechazo de negocio: el modal permanece abierto, se conservan los datos de entrada válidos y el usuario puede corregir los campos inválidos sin perder selección de jugador ni cantidad (FR-F10). Tras un POST fallido con sesión activa, `market` llama `onRefreshHoldings(playerId)` y `app` invalida holdings para actualizar las cantidades visibles; 401 mantiene la redirección global y no restaura el modal. No hay reintento automático e inseguro.
2. Tras publicación exitosa: navegar a Portfolio (resumen y tenencias) y verificar que los tokens disponibles y reservados del jugador publicado reflejen la reserva (RF-F06, FR-F12). `market` invalida `currentQuote(id)`; `app` invalida `portfolioQueryKeys.summary` y todas las `holdings(...)` vía `onSellSuccess(playerId)`. Al abrir el modal y tras un POST fallido con sesión activa, verificar que `market` llama `onRefreshHoldings(playerId)` y `app` invalida holdings; 401 conserva el flujo global de sesión.
3. Inspeccionar TanStack Query: market invalida solo `currentQuote(id)` real; app invalida `summary` y las combinaciones `holdings(...)`. Nunca invalidar `demo-quotes`.
4. Doble click rápido en Confirmar mientras envía: un solo POST. Mientras se procesa, `aria-busy`, spinner visible, Confirmar/Cancelar/overlay/ESC no cierran ni envían otro POST.

## US4: accesibilidad y referencias visuales

1. Recorrer con teclado los controles semánticos del modal (cantidad, ajustes de cantidad, presets, Confirmar y Cancelar) y verificar que la navegación básica permite utilizarlos. El modal debe reutilizar el comportamiento accesible existente de `shared/Modal`.
2. Comparar con `referencias-imagenes/07-mercado-vender.png` y `referencias-imagenes/08-mercado-vender-modal-abierto.png` a 375, 768 y 1440 px: mercado visible detrás, overlay semitransparente, cantidad inicial `1`, sin scroll horizontal ni cambio de layout/scroll del mercado. Revisar también filtros de compra reutilizados, filtro por disponibilidad y ordenamiento de tenencias (FR-F11).
3. Usar el flujo en mobile, tablet y desktop y registrar los resultados manuales de teclado y adaptación visual contra CA-11/FR-F07.

Las imágenes son guías de disposición y estilo: no representar como reales saldos, órdenes pendientes, estadísticas ni variación 24h que esta feature no consulta.

Registrar resultados y tiempos (<200 ms apertura en condiciones del plan, ≤500 ms aviso tras respuesta) solo con evidencia observada. Estos escenarios quedan pendientes del usuario hasta que exista implementación y se ejecuten. **No se incorporan pruebas automatizadas ni nuevas dependencias de testing** (frontend exento, Constitution §19.199); la validación es exclusivamente manual, basada en los escenarios de este documento.

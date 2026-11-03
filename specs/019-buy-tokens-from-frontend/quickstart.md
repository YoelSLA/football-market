# Quickstart: validación manual de compra desde el frontend

**Ejecuta el usuario, no el agente.** Frontend exento de tests automatizados. Requiere backend con mercado inicializado y cotizaciones reales persistidas, JWT de usuario no superusuario, saldo y stock para un caso exitoso; `npm run dev` desde `frontend/` si corresponde. El build autorizado al agente para cambios futuros de código es `npm run build` (no equivale a testing).

## US1: mercado y modal

1. Sin sesión, visitar `/market/buy`: `AuthGuard` redirige al login. Con sesión, visitar Mercado > Comprar tokens: listado paginado de jugadores visibles, estado vacío/error con reintento, ficha propia y botón Comprar. Un jugador visible sin stock sigue en el listado; su POST devuelve `INSUFFICIENT_STOCK`.
2. Abrir una ficha con cotización vigente: nombre, equipo, liga, posición, precio real de GET current, `calculatedAt` del backend; comparar precio e instante con respuesta HTTP. Nunca mostrar dato o `date` de la serie demo de players.
3. Cancelar, pulsar ESC y hacer click fuera: en los tres casos no hay POST, se mantiene URL, página, scroll, pestaña activa y valores de cualquier filtro existente del mercado; el mercado permanece visible bajo el overlay y sin reflow. Este escenario no presupone crear filtros nuevos.

## US2: cantidad e importe

1. Al abrir cada nueva compra el campo muestra `1`; con cotización disponible, el resumen estima un token y Confirmar queda habilitado. Borrar el campo o introducir `0`, `101`, `-5`, `3.5`, letras o exponentes: error inline, Confirmar deshabilitado y sin POST. `"10"`, `"1"` y `"100"` son entradas textuales válidas que deben enviarse como números 10, 1 y 100.
2. A 125 centésimas, cantidad inicial 1: «Importe estimado: 1,25 créditos (125 centésimas)»; al editar a `"10"`: «Importe estimado: 12,50 créditos (1.250 centésimas)». No presentar estimación como precio garantizado.
3. GET current devuelve `404 QUOTE_NOT_FOUND`, precio inválido o falla de red: indicar falta de cotización/error; Confirmar deshabilitado; no sustituirla por demo.

## US3: compra y errores

1. Confirmar una compra válida: único POST con `{playerId,quantity}` numérico, aun cuando el campo contenga texto `"10"`; sin `listingId`. Spinner y controles de cierre deshabilitados mientras envía.
2. En éxito: toast no intrusivo con ID, jugador, cantidad, precio e importe ejecutados y `executedAt` de respuesta; si el precio cambió, prevalece el de POST. Cerrar manualmente o después de 2 s; modal no altera el mercado.
3. Inspeccionar TanStack Query: market invalida solo `currentQuote(id)` real; app invalida `portfolioQueryKeys.summary` y todas las combinaciones `holdings(...)`. Al navegar a Portfolio, resumen y tenencias se actualizan; nunca invalidar `demo-quotes`.
   Las cotizaciones de la ficha y el modal corresponden a la misma key por jugador; abrir el modal no crea una segunda petición GET. El toast agrega el nombre de la ficha al resultado del POST, pero el Model `BuyResult` y el Service solo contienen datos del backend.
4. Cotejar uno por uno los diez códigos POST de FR-10 (`INVALID_REQUEST`, `INVALID_QUANTITY`, `INSUFFICIENT_CREDITS`, `INSUFFICIENT_STOCK`, `QUOTE_UNAVAILABLE`, `INVALID_QUOTE_PRICE`, `SUPERUSER_CANNOT_BUY`, `PLAYER_NOT_FOUND`, `PLAYER_NOT_VISIBLE`, `INTERNAL_ERROR`) con la tabla del [contrato](contracts/buy-tokens-frontend.md): cada uno tiene mensaje definido sin detalles sensibles. Para desconocido, red/500: fallback genérico; 401: redirección sin recuperar modal. Para GET 404 `QUOTE_NOT_FOUND`: ausencia de cotización, no mensaje de jugador del POST. Provocar casos manualmente cuando sea posible, sin afirmar que todos los errores hayan sido reproducidos.

## US4: accesibilidad y referencias visuales

1. Click rápido repetido en Confirmar: un POST. Mientras se procesa, `aria-busy`, spinner visible, Confirmar/Cancelar/overlay/ESC no cierran ni envían otro POST.
2. Foco inicial en cantidad; Tab y Shift+Tab circulan en el diálogo, `role="dialog"`, `aria-modal="true"`, título y descripción asociados; tras cerrar regresa al botón Comprar que abrió el diálogo. Comprobar lector de pantalla básico.
3. Comparar con `referencias-imagenes/04-mercado-comprar.png` y `referencias-imagenes/05-mercado-comprar-modal-abierto.png` a 375, 768 y 1440 px: mercado visible detrás, overlay semitransparente, cantidad inicial `1` como en ref. 05, sin scroll horizontal ni cambio de layout/scroll del mercado.

Las imágenes son guías de disposición y estilo: no representar como reales saldos, órdenes pendientes, variación 24 h ni estadísticas que esta feature no consulta.

Registrar resultados y tiempos (<200 ms apertura en condiciones del plan, ≤500 ms aviso tras respuesta) solo con evidencia observada. Estos escenarios quedan pendientes del usuario hasta que exista implementación y se ejecuten.

## Ajuste de scroll exclusivo de mercado

### Diagnóstico sin compras

Comprobar las cuatro pestañas con iconos (carrito/etiqueta/pieza de órdenes/historial), margen inferior de Aplicar y scrollbar oscura solo en main. Confirmar sigue deshabilitado mientras cotización esté cargando/pausada/ausente o haya error, cantidad inválida o envío; su motivo aparece junto al botón. El estado inicial válido de cantidad es `"1"`, no exige editar el input si la cotización existe.

Inspeccionar GET current autenticado (URL con id, estado HTTP y cuerpo saneado, nunca JWT/cookies): 200 válido habilita con cantidad válida; 404 QUOTE_NOT_FOUND es ausencia; otro 404 es fallo HTTP; 401 activa gestión de sesión, 403 permisos; red/5xx y respuesta inválida muestran mensajes distintos y reintento de GET cuando corresponde. Refetch/pausa/error con datos previos en caché no habilitan compra. No pulsar Confirmar ni ejecutar inicialización/recálculo durante esta inspección. Sin esa evidencia no atribuir causa a falta de datos backend.

Sin ejecutar compras, inspeccionar `/market/buy` en 375/768/1440px (registrar también la altura efectiva): desplazar el listado hasta el último jugador; navbar, encabezado, métricas, panel y paginación no cambian de posición y body no se desplaza. Redimensionar alto/ancho: main ocupa el espacio restante por flex/grid, sin altura de navbar codificada ni scroll horizontal. En móvil el panel es un details nativo inicialmente plegado; abrir/cerrar sin desplazar encabezado/listado/paginación ni implementar filtros. Verificar accesibilidad por teclado y que el panel expandido no quede recortado, especialmente con poca altura o zoom.

Navegar después a `/players` y `/portfolio`: el modificador de contención de mercado desaparece; las reglas previas de scroll de cada ruta siguen vigentes. Abrir/cerrar modal de compra con Cancelar/ESC, sin confirmar: conserva su scroll propio y la posición del listado. Estos pasos no están ejecutados mientras la herramienta de navegador no tenga conexión disponible.

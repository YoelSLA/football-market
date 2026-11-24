# Quickstart: Guía de Validación Rápida de SPEC-027

Esta guía detalla los escenarios de validación interactivos y visuales para verificar la funcionalidad completa de la **SPEC-027** en la interfaz de usuario.

---

## Prerrequisitos de Ejecución

1. Backend levantado con soporte de SPEC-026:
   ```bash
   cd backend && ./gradlew bootRun
   ```
2. Frontend ejecutándose en entorno de desarrollo:
   ```bash
   cd frontend && npm run dev
   ```
3. Usuario autenticado no administrador (ej: `yoel@footballmarket.com`) con tokens libres de al menos un jugador cotizable (ej: *Erling Haaland* o *Kylian Mbappé*).
4. Viewport de navegador desktop con resolución $\ge 1024$ px (ancho recomendado: 1280 px o 1920 px).

---

## Escenario 1: Venta Inmediata Oficial (Buyback)

**Objetivo**: Validar la liquidación instantánea de tokens contra el superusuario con 10% de spread y acreditación inmediata de créditos.

1. Navegar a `/market/sell` o a `/portfolio`.
2. Localizar un jugador con tokens disponibles y pulsar el botón **"Vender"**.
3. **Verificación visual del modal**:
   - Comprobar que se presenta el selector con dos pestañas: *"Venta Inmediata Oficial"* y *"Publicación en Comunidad P2P"*.
   - Comprobar que *"Venta Inmediata Oficial"* está activa por defecto.
   - Observar el desglose dinámico: cotización oficial (ej: 1.00 crédito), spread del 10% (0.10 créditos), valor unitario neto (0.90 créditos).
4. Modificar la cantidad en el stepper (ej: seleccionar 5 tokens).
   - Comprobar que el total neto se actualiza en tiempo real a $4.50$ créditos ($5 \times 0.90$).
5. Pulsar **"Confirmar venta inmediata"**:
   - Verificar feedback de carga (`isBusy`), botones deshabilitados y spinner.
   - Al responder el backend, verificar la pantalla de comprobante (`SellModalSuccess`) indicando los créditos acreditados.
   - Verificar la aparición de la notificación toast global de éxito.
6. Cerrar el modal:
   - Comprobar que el saldo de créditos en la barra superior aumentó inmediatamente en $4.50$ créditos.
   - Comprobar que la cantidad disponible del jugador disminuyó en 5 tokens.

---

## Escenario 2: Publicación P2P con Precio Flexible y Límites de Banda

**Objetivo**: Validar el slider e input de precio flexible, su inicialización en -10% y el bloqueo estricto fuera de $\pm 20\%$.

1. Abrir nuevamente el modal de venta para un jugador con tokens disponibles.
2. Hacer clic en la pestaña **"Publicación en Comunidad P2P"**:
   - Verificar que desaparece el desglose de spread y aparece el control `P2PPriceSelector`.
   - Verificar que el control se inicializa automáticamente con **-10%** de descuento promocional (ej: si la cotización es 100 centésimas, el precio se sitúa en 90 centésimas) con el badge verde `🏷️ -10% Oferta`.
3. Interactuar con el slider y los botones rápidos:
   - Pulsar el botón **"-20%"**: el precio pasa al límite inferior (80 centésimas), el badge muestra `🏷️ -20% Oferta` y el total se recalcula.
   - Intentar forzar un valor menor a 80 en el input: el control bloquea la entrada en el tope mínimo.
   - Pulsar el botón **"+10%"**: el precio pasa a 110 centésimas y el badge conmuta a naranja `🔥 +10% Premium`.
   - Mover el slider a 100 centésimas (0%): el badge comparativo se oculta o indica precio oficial neutro.
4. Con un precio de oferta (ej: 85 centésimas, `-15%`) y cantidad 3 tokens, pulsar **"Publicar en P2P"**:
   - Verificar que se muestra feedback `isBusy`.
   - Tras la respuesta exitosa, verificar la pantalla de comprobante indicando publicación activa con precio congelado en 0.85 créditos.
   - Verificar que los 3 tokens pasan a estado reservado en `/portfolio`.

---

## Escenario 3: Badges de Oportunidad en el Catálogo P2P

**Objetivo**: Validar la visualización comparativa de ofertas en `/market/p2p`.

1. Navegar a la página `/market/p2p`.
2. Observar las tarjetas de publicaciones en el catálogo comunitario:
   - Comprobar que la orden creada con descuento en el Escenario 2 exhibe un badge verde destacado: `🏷️ -15% Oferta`.
   - Comprobar que publicaciones con precio superior al oficial exhiben el badge naranja/rojo: `🔥 +X% Premium`.
   - Comprobar que publicaciones publicadas al precio oficial exacto no muestran badge de oportunidad.
3. Posar el cursor sobre los badges y verificar contraste de color y legibilidad.

---

## Escenario 4: Manejo de Errores y Validaciones

**Objetivo**: Asegurar que errores de validación no cierren el modal ni descarten datos.

1. En el modal de venta, intentar ingresar una cantidad superior a los tokens libres:
   - Comprobar mensaje de advertencia y botón de confirmación deshabilitado.
2. Si el backend responde error (ej: falta de liquidez del superusuario o jugador no visible):
   - Comprobar que el modal permanece abierto.
   - Comprobar que el error se muestra en una caja de alerta legible.
   - Comprobar que los campos conservan los valores que el usuario ingresó.

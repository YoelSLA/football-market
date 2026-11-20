# Quickstart: Guía de Verificación y Escenarios de Prueba (SPEC-031)

**Feature Branch**: `feat/031-home-dashboard`  
**Fecha**: 2026-11-20  
**Spec de referencia**: [`spec.md`](spec.md)  
**Contrato API**: [`contracts/dashboard-api.md`](contracts/dashboard-api.md)

---

## 1. Prerrequisitos

1. **Backend operativo**: Base de datos PostgreSQL con migraciones Flyway aplicadas y datos de cotizaciones persistidos en `player_quotes`.
2. **Frontend ejecutándose**: Servidor de desarrollo Vite iniciado (`pnpm dev`) o build verificado (`pnpm build`).
3. **Usuarios de prueba**:
   - *Usuario A (Inversor activo)*: Cuenta con créditos disponibles, tokens de varias ligas/posiciones en cartera y transacciones recientes.
   - *Usuario B (Nuevo registro)*: Cuenta recién creada con saldo inicial de bienvenida pero sin compras ni transacciones registradas.

---

## 2. Escenarios de Validación End-to-End

### Escenario 1: Verificación de Integridad Estática y Builds
**Objetivo**: Asegurar que los contratos y componentes compilan limpiamente sin advertencias de tipado ni de formateador.
1. Ejecutar en terminal frontend:
   ```bash
   pnpm biome:check
   pnpm build
   ```
   *Resultado esperado*: 0 errores en linter Biome y build de Vite exitoso (`dist/` generado).
2. Ejecutar en terminal backend:
   ```bash
   ./gradlew compileJava classes
   ```
   *Resultado esperado*: Compilación Java 21 exitosa sin advertencias.

---

### Escenario 2: Cockpit Financiero y Tarjetas Hero (Usuario Inversor)
**Objetivo**: Validar la consistencia matemática de las métricas patrimoniales (US 1).
1. Iniciar sesión con el *Usuario A* y acceder a `/home`.
2. Verificar la tarjeta **Patrimonio Neto**:
   - Debe equivaler exactamente a: $\text{Créditos Disponibles} + \text{Valor actual de tokens poseídos}$.
3. Verificar la tarjeta **P&L Total**:
   - Comprobar que el valor y porcentaje reflejan el color semántico adecuado (verde esmeralda si es positivo, rojo si es negativo).
4. Comprobar la tarjeta de **Custodia**:
   - La suma de *Tokens Libres* + *Tokens Reservados* debe coincidir exactamente con el total de tokens poseídos.

---

### Escenario 3: Interacción con el Gráfico Donut de Asignación (US 2)
**Objetivo**: Comprobar la interactividad y accesibilidad del gráfico SVG de distribución de activos.
1. Localizar el panel de **Asset Allocation**.
2. Conmutar el selector a **"Por Liga"**:
   - El Donut debe reconfigurar suavemente los sectores circulares correspondientes a las 5 ligas europeas.
3. Posar el cursor sobre cualquier sector:
   - Debe desplegarse el tooltip indicando el nombre de la liga, el importe en créditos y el porcentaje sobre el total.
4. Conmutar el selector a **"Por Posición Táctica"**:
   - El gráfico se actualiza mostrando Delanteros, Mediocampistas, Defensas y Arqueros.
5. Inspeccionar el código HTML o abrir el elemento `<details>` de accesibilidad:
   - Debe existir una tabla semántica con los mismos valores para lectores de pantalla.

---

### Escenario 4: Gráfico de Rendimiento vs. Benchmark FMI (US 3)
**Objetivo**: Evaluar la renderización de la serie temporal y los filtros de tiempo.
1. Observar el panel **Performance vs Football Market Index**.
2. Verificar que se dibujan ambas curvas con sus leyendas identificativas (azul/cian para la cartera del usuario, dorado para el índice FMI).
3. Conmutar los botones de rango temporal (`1M`, `3M`, `ALL`):
   - El gráfico debe actualizar su escala y puntos sin errores en la consola del navegador.

---

### Escenario 5: Market Ticker Superior y Top Movers (US 4)
**Objetivo**: Probar la cinta de cotizaciones continuas y la tabla de mayor variación.
1. Observar el ticker horizontal bajo el Navbar:
   - Debe exhibir las cotizaciones de referencia con flechas verdes/rojas y porcentajes.
2. Posar el cursor sobre el ticker:
   - El desplazamiento debe pausarse automáticamente.
3. En la tabla de **Top Movers**, hacer clic en un jugador de *Top Gainers*:
   - Debe navegar limpiamente a `/players/{id}`.

---

### Escenario 6: Arbitrajes P2P y Quick Actions (US 6)
**Objetivo**: Verificar los atajos y el widget de mejores ofertas comunitarias.
1. En el bloque **Best Deals P2P**, identificar una orden con descuento.
2. Pulsar en *"Aprovechar oferta"*:
   - La aplicación redirige a `/market/p2p` preseleccionando la orden correspondiente.
3. Probar los botones de **Quick Actions**:
   - "Comprar en Mercado Oficial" $\rightarrow$ `/market/buy`
   - "Ver Mi Cartera" $\rightarrow$ `/portfolio`
   - "Publicar Venta" $\rightarrow$ `/market/sell`

---

### Escenario 7: Resiliencia y Usuario Nuevo (First-Time User) (US 7)
**Objetivo**: Garantizar que un usuario sin tenencias recibe una experiencia orientada a la acción sin errores.
1. Iniciar sesión con el *Usuario B* y navegar a `/home`.
2. Verificar:
   - Net Worth = Saldo inicial en créditos.
   - P&L = 0 créditos.
   - En lugar de gráficos rotos, se visualiza el **Empty State ilustrado** invitando a explorar el catálogo de jugadores.
   - El Market Ticker, los Top Movers y el Radar de Oportunidades se muestran completos con la información general del mercado.

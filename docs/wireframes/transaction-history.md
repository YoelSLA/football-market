# Wireframe: historial personal de transacciones

Propuesta visual para **SPEC-025**, derivada del issue
[#27](https://github.com/YoelSLA/football-market/issues/27) y del contrato backend vigente de
[SPEC-020](../../specs/020-view-operation-history/contracts/http.md). Define jerarquía,
composición, contenido, distribución vertical, estados y orden responsive; no agrega datos,
operaciones ni interacciones fuera de esos contratos.

> [!IMPORTANT]
> **Estado: propuesta visual aprobada el 2026-11-10.** La ubicación es
> `Cartera > Historial de transacciones`, la ruta es `/portfolio/transactions`, la presentación
> adapta tabla→grupos→tarjetas y el nombre del jugador es texto sin navegación.

Las cifras y nombres de ejemplo sirven únicamente para mostrar ubicación y formato. La aplicación
presenta exclusivamente valores devueltos por `GET /api/users/me/transactions`. Los importes
históricos se expresan en centésimas de crédito y se formatean para lectura, sin recalcularlos con
la cotización vigente.

---

## Contrato visual de datos

Cada fila o tarjeta representa exactamente una entrada del array devuelto por el backend.

| Campo backend | Representación visual | Regla |
| :--- | :--- | :--- |
| `transactionId` | `Transacción #92` | Identidad auxiliar, nunca reemplaza el nombre del jugador. |
| `type` | Píldora `COMPRA` o `VENTA` | Traducción visual de `BUY` / `SELL`; no implica ganancia o pérdida. |
| `playerName` | Nombre principal de la operación | El endpoint no provee retrato, equipo, liga ni posición. |
| `quantity` | `5 tokens` | Singular `1 token`; plural para el resto. |
| `unitPrice` | `1,25 créditos` | Valor histórico; entero backend dividido por 100 solo para formato. |
| `totalAmount` | `6,25 créditos` | Valor histórico; no se deriva en el frontend. |
| `executedAt` | Fecha y hora local | Se conserva el instante ISO-8601; se presenta en la zona local. |

La pantalla presenta únicamente el nombre del jugador como texto, sin navegación asociada. No se
muestran contraparte, foto, equipo, liga, estado pendiente/cancelado, comisiones, saldo, tenencia
resultante, cotización vigente ni resultado financiero porque el contrato no los expone.
Tampoco se incorporan filtros, búsqueda, ordenamiento, exportación, agregaciones o paginación:
el endpoint devuelve la colección completa ya ordenada por `executedAt DESC, transactionId DESC`.

---

## Ubicación aprobada y navegación

```text
Cartera
├── Mi portfolio                    /portfolio
├── Mis tenencias                   /portfolio/holdings
└── Historial de transacciones      /portfolio/transactions
```

La ubicación aprobada es `Cartera` porque el historial es información personal, financiera y
retrospectiva, complementaria del resumen y las tenencias. `Mercado` conserva las acciones de
compra y venta. La navegación actual contiene dos placeholders solapados —`Mercado > Historial de
operaciones` y `Cartera > Historial de transacciones`—; la propuesta habilita solo el segundo y
retira el duplicado del inventario cuando se implemente.

La pantalla permanece dentro de `AuthGuard` y `AppLayout`. El primer nivel del navbar conserva
`Cartera` activo y el segundo nivel marca `Historial de transacciones`.

---

## Presupuesto vertical y Box Model (Resolución de referencia: 1920 × 913 px)

En la resolución de referencia ($1920 \times 913\text{ px}$), la aplicación no presenta scroll
global en la ventana. Los dos niveles de navegación ocupan su altura contractual existente y la
página distribuye el resto mediante flexbox.

$$\begin{aligned}
\text{Viewport del navegador:} & \quad \mathbf{913\text{ px}} \\
\text{Navbar global fija (60 px + 48 px):} & \quad -\;108\text{ px} \\
\hline
\text{Altura neta disponible para TransactionHistoryPage:} & \quad \mathbf{805\text{ px}}
\end{aligned}$$

### Desglose vertical exacto (805 px)

| N° | Sección / Elemento | Tipo de altura | Altura neta | Box Model, padding y dimensiones clave |
| :-: | :--- | :---: | :---: | :--- |
| **1** | **Padding superior de página** | Fija | **8 px** | Parte de `padding: 8px` del contenedor principal. |
| **2** | **Cabecera de página** | Fija | **68 px** | `height: 68px; padding: 8px; box-sizing: border-box;` Icono de $52 \times 52\text{ px}$, título, descripción y contador derivado de `array.length`; `gap: 8px`. |
| **3** | **Separación** | Fija | **8 px** | `height: 8px; flex: 0 0 8px;` sin contenido ni borde. |
| **4** | **Panel del historial** | Flexible (`flex: 1`) | **713 px** | `min-height: 0; margin: 0; box-sizing: border-box; border: 1px; border-radius: 8px; overflow: hidden;` Ocupa todo el ancho útil. |
| **5** | **Padding inferior de página** | Fija | **8 px** | Parte de `padding: 8px` del contenedor principal. |
| — | **TOTAL COMPROBADO** | **Exacto** | **805 px** | **$8 + 68 + 8 + 713 + 8 = \mathbf{805\text{ px}}$** |

### Desglose interno del panel flexible (referencia: 713 px)

El panel ocupa el tramo flexible completo entre los paddings de la página, sin margen propio.

| Elemento interno | Tipo | Altura de referencia | Regla |
| :--- | :---: | :---: | :--- |
| Cabecera semántica de tabla | Fija | **40 px** | `min-height: 40px; padding: 8px; flex-shrink: 0;` Permanece visible. |
| Región de filas / estado | Flexible | **673 px** | `flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; scrollbar-width: thin;` Filas con `padding: 8px` y `gap: 8px`. |
| **Total de caja visible** | Exacto | **713 px** | $40\text{ px} + 673\text{ px} = 713\text{ px}$. |

> [!IMPORTANT]
> **Normas de Box Model y scroll**
>
> 1. `box-sizing: border-box` aplica a la página, cabecera, panel, tabla, filas, tarjetas y estados.
> 2. `713 px` y `673 px` son resultados para el viewport de referencia, no alturas
>    rígidas para CSS. La implementación usa `flex: 1; min-height: 0`.
> 3. En escritorio, navbar y cabecera permanecen fijas. Solo la región de filas se desplaza.
> 4. Nunca se usa scroll global en escritorio ni scroll horizontal para leer la tabla.
> 5. Tablet y móvil conmutan a altura automática y scroll documental natural.
> 6. Dentro del contenido propio de esta página, todo `padding`, `margin` y `gap` es de **8 px
>    como máximo**. La regla no altera las dimensiones internas del navbar global compartido.

---

## Composición ASCII completa — escritorio

```text
Viewport de referencia: 1920 × 913 px
Navbar global (Nivel 1 + Nivel 2): 108 px (fijo superior)
───────────────────────────────────────────────────────────────────────────────────────────────────────────
Altura neta TransactionHistoryPage: 805 px (sin scroll global)

┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐ ▲
│ FOOTBALL MARKET                    Inicio   Jugadores   Mercado   [Cartera]   Usuario          60 px │ │ 108 px
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ Mi portfolio       Mis tenencias       [Historial de transacciones]                       48 px │ ▼
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌─────┐  HISTORIAL DE TRANSACCIONES                              ┌─────────────┐    │ ▲
│ │  ↕  │  Compras y ventas confirmadas de tu cuenta.               │ 24 operaciones│    │ │ 68 px
│ └─────┘                                                              └─────────────┘    │ ▼
│                                                                                                            │ 8 px
│ ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────┐ │ ▲
│ │ OPERACIÓN      JUGADOR                  CANTIDAD       PRECIO UNITARIO       TOTAL          FECHA     │ │ 40 px
│ ├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤ ▼
│ │ [COMPRA]      Lionel Messi             5 tokens       1,25 créditos         6,25 créditos  10/11/2026│ ▲
│ │ Transacción #92                                                                          14:30    │ │
│ ├─────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ [VENTA]       Julián Álvarez          2 tokens       3,40 créditos         6,80 créditos  08/11/2026│ │ 673 px
│ │ Transacción #87                                                                          09:12    │ │ SCROLL
│ ├───────────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ [COMPRA]      Emiliano Martínez        1 token        2,80 créditos         2,80 créditos  03/11/2026│ │
│ │ Transacción #74                                                                          18:45    │ │
│ │                                      ⋮                                                                     │ │
│ └─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘ │ ▼
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

TOTAL VERTICAL: 8 px + 68 px + 8 px + 713 px + 8 px = EXACTAMENTE 805 px
```

### Jerarquía visual

1. **Título de página**: identifica la vista; usa el patrón de cabecera de Cartera, con icono
   `History` o `FileText`, sin crear un componente global nuevo si la variante existente alcanza.
2. **Nombre del jugador**: dato principal de cada operación, presentado como texto sin enlace; la
   identidad de transacción es secundaria y se ubica bajo la píldora de tipo.
3. **Importe total**: valor financiero con mayor peso dentro de la fila.
4. **Tipo**: píldora compacta. `COMPRA` usa el acento de ganancia existente; `VENTA` usa el
   acento dorado/cautela existente, nunca rojo, porque vender no significa perder.
5. **Cantidad, precio y fecha**: datos de lectura alineados por columna; los valores numéricos
   usan cifras tabulares cuando el navegador las soporte.

### Columnas y proporciones

La tabla usa `grid-template-columns` para conservar alineación sin una tabla de ancho rígido:

```text
Operación  15% | Jugador 27% | Cantidad 12% | Precio unitario 16% | Total 16% | Fecha 14%
```

Todas las columnas declaran `min-width: 0`. Los nombres largos pueden envolver en dos líneas; los
importes y fechas no se truncan. No se habilita desplazamiento horizontal.

---

## Estados de la consulta

La cabecera de página permanece visible en todos los estados. Cuando existen filas, la cabecera de
columnas ocupa 40 px y la lista dispone de los 673 px restantes. En carga, error o vacío no se
renderiza una cabecera tabular sin datos: el bloque de estado ocupa los 713 px completos del panel.

### Carga

```text
┌───────────────────────────────────────────────────────────┐
│ Consultando tu historial de transacciones…                 │
│                                                              │
│ [                 fila de carga accesible                 ]  │
│ [                 fila de carga accesible                 ]  │
│ [                 fila de carga accesible                 ]  │
└────────────────────────────────────────────────────────────┘
```

- El contenedor usa `aria-busy="true"` y el mensaje `role="status"`.
- El texto es suficiente; un skeleton visual puede reutilizar superficies existentes, sin
  introducir una dependencia ni ocultar el mensaje accesible.
- No se muestra `0 operaciones` durante la carga.

### Error

```text
┌───────────────────────────────────────────────────────────┐
│ No pudimos cargar tu historial.                            │
│ Tus operaciones no fueron modificadas.                     │
│                                                              │
│ [ Reintentar ]                                               │
└────────────────────────────────────────────────────────────┘
```

- El bloque usa `role="alert"` y el `Button` compartido para `Reintentar`.
- `401` queda gobernado por la sesión/autenticación global; la vista no revela datos parciales.
- No se conservan filas anteriores ante error porque el requerimiento no define una política de
  datos obsoletos. TanStack Query puede conservar caché internamente sin mostrarla como vigente.

### Historial vacío

```text
┌────────────────────────────────────────────────────────────┐
│                            ↕                                 │
│             Todavía no tenés transacciones                   │
│ Tus compras y ventas confirmadas aparecerán en este historial. │
│                                                              │
│                       [ Ir al mercado ]                       │
└───────────────────────────────────────────────────────────┘
```

- `[]` es un resultado exitoso, no un error.
- El bloque usa `role="status"`.
- `Ir al mercado` reutiliza `Button` y navega a `/market/buy`; no crea una operación ni amplía
  el contrato.

### Historial con datos

- Se muestra `{n} operación` o `{n} operaciones` usando `array.length`.
- Las filas conservan exactamente el orden recibido.
- La clave React es `transactionId`.
- No existe estado visual adicional para una operación: toda entrada es confirmada.

---

## Adaptación responsive

### Escritorio (desde 1025 px)

- Navbar global de 108 px, padding exterior de 8 px y cabecera de página de 68 px.
- Sin scroll global: el panel es `flex: 1; min-height: 0` y solo las filas usan `overflow-y: auto`.
- Presentación tabular en seis columnas; cabecera de columnas fija dentro del panel.
- La píldora, nombre, valores y fecha conservan alineación por grilla.

### Tablet (769–1024 px)

- El navbar puede usar su variante colapsable existente.
- El layout conmuta a `height: auto; overflow: visible`, igual que `AppLayout` vigente.
- La tabla conserva seis campos pero agrupa la fecha debajo del tipo y reduce a cuatro columnas
  visuales: `Operación/fecha | Jugador | Cantidad/precio | Total`.
- No existe scroll horizontal; los nombres pueden envolver.

```text
┌──────────────────────────────────────────────────────────┐
│ [COMPRA]       Lionel Messi         5 tokens     6,25 créditos │
│ 10/11/26 14:30 Transacción #92        1,25 c/u                   │
└───────────────────────────────────────────────────────────┘
```

### Móvil (hasta 768 px; referencia mínima 360 px)

- Scroll documental natural; no se fijan alturas de cabecera o panel.
- El segundo nivel del navbar conserva su desplazamiento horizontal existente, sin provocar
  overflow del documento.
- Cada operación pasa de fila a tarjeta semántica. No se renderiza una tabla escondida en CSS.
- Tarjetas en una sola columna, `width: 100%; min-width: 0`, con pares etiqueta/valor.
- El nombre admite hasta dos líneas; importes y fecha permanecen completos.

```text
┌────────────────────────────┐
│ FOOTBALL MARKET          ☰ │
├────────────────────────────┤
│ [Historial transacciones] │
├────────────────────────────┤
│ ↕ Historial transacciones │
│ Compras y ventas propias   │
│ 24 operaciones             │
│                            │
│ ┌────────────────────────┐ │
│ │ [COMPRA]  10/11/2026 │ │
│ │           14:30      │ │
│ │ Lionel Messi         │ │
│ │ Transacción #92      │ │
│ │                        │ │
│ │ Cantidad     5 tokens  │ │
│ │ Precio/u     1,25 cr   │ │
│ │ Total        6,25 cr   │ │
│ └────────────────────────┘ │
│                            │
│ ┌────────────────────────┐ │
│ │ [VENTA]   08/11/2026 │ │
│ │ Julián Álvarez      │ │
│ │ Cantidad     2 tokens  │ │
│ │ Precio/u     3,40 cr   │ │
│ │ Total        6,80 cr   │ │
│ └────────────────────────┘ │
└────────────────────────────┘
```

---

## Reutilización y límites de componentes

### Reutilizar

- `AppLayout`, `PageLayout`, `AuthGuard` y el sistema `Navbar` existente.
- Configuración declarativa de `NAV_SECTIONS` y resolución de pestaña activa.
- `Button` para `Reintentar` e `Ir al mercado`.
- Iconos desde `shared/icons`; nunca importarlos directamente desde Lucide.
- `formatCredits` y `formatDateTime` de portfolio si sus contratos actuales cubren estos valores.
- Cliente `http`, `getErrorMessage`, TanStack Query y políticas globales de caché.
- Tokens de color, espaciado, layout, breakpoints, foco, paneles y tablas existentes.
- Patrones accesibles actuales: `aria-busy`, `role="status"`, `role="alert"` y foco visible.

### No duplicar ni forzar reutilización

- No crear un segundo navbar, layout, cliente HTTP, formatter de créditos o botón.
- No reutilizar componentes de ranking o tenencias si obligan a props o semántica ajenas. Se
  reutilizan sus tokens y patrones; la fila de transacción conserva responsabilidad propia.
- No introducir una abstracción genérica de tabla por una sola pantalla.
- No consultar el catálogo por cada operación para completar imágenes o equipos ausentes.
- No duplicar la entrada de navegación en `Mercado` y `Cartera`.

---

## Decisiones visuales y arquitectónicas aprobadas

1. **Ubicación y ruta:** `Cartera > Historial de transacciones`, en
   `/portfolio/transactions`, retirando el placeholder duplicado de `Mercado` al implementar.
2. **Patrón responsive:** tabla densa en escritorio, agrupación de cuatro columnas en tablet y
   tarjetas en móvil. No se usa una tabla con desplazamiento horizontal.
3. **Jugador sin navegación:** el nombre se presenta como texto, sin enlace, botón, redirección
   ni consulta adicional.
4. **Estado vacío:** incluye `Ir al mercado`, que redirige a `/market/buy`.
5. **Retícula compacta:** el contenido propio de la página usa 8 px de padding exterior,
   separación, margen y gap; ningún padding, margen o gap local supera 8 px.

---

## Protocolo de evaluación de localización (SC-007)

La jerarquía visual se evalúa después de implementar la pantalla, sin convertir el wireframe en
evidencia anticipada de cumplimiento:

1. Participa una persona sin conocimiento previo del historial presentado.
2. El estado inicial es la pantalla completamente cargada con 20 operaciones ficticias
   representativas, incluidas compras y ventas.
3. Se indica una transacción por su identificador y se solicita informar jugador, tipo e importe
   total.
4. El cronometraje comienza al comunicar la consigna y termina con la respuesta de los tres datos.
5. El resultado es satisfactorio únicamente si los tres datos son correctos y el tiempo es menor
   o igual a 10 segundos.

---

## Límites

Este wireframe no autoriza cambios backend, filtros, búsqueda, ordenamiento, paginación,
exportación, estadísticas, agregaciones, contraparte, estados de orden, comisiones, saldo,
tenencias, rentabilidad, cotización actual, imágenes ni consultas adicionales por jugador. Tampoco
autoriza implementar código ni ejecutar `/speckit.specify`.

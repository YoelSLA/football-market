# Guía de validación: Detalle individual del jugador

**Estado**: Guía de validación vigente del sistema.
**Referencias normativas**: [spec.md](spec.md), [wireframe canónico](../../docs/wireframes/player-detail.md), [player-statistics.md](contracts/player-statistics.md) y [data-model.md](data-model.md).

---

## 1. Verificaciones automatizadas

Comandos ejecutables para verificar la integridad del código y contratos:

### Frontend

```bash
# Desde el directorio frontend/
npm run biome:check
npm run scss:check-imports
npm run build
```

### Backend

```bash
# Desde el directorio backend/
./gradlew test --tests 'footballmarket.controllers.PlayerControllerTest'
./gradlew test --tests 'footballmarket.controllers.PlayerStatisticsControllerTest'
```

---

## 2. Escenarios de validación funcional y visual

### Escenario 1: Cabecera compacta de tres zonas

1. Navegar a `/players/{id}` con un jugador visible.
2. Comprobar la disposición en tres zonas horizontales en escritorio:
   - **Identidad (izquierda)**: avatar de 90×90 px con borde sutil, nombre, equipo actual con escudo si está disponible, liga y posición.
   - **Métricas titulares (centro)**: tres micro-tarjetas de 94 px de altura que sintetizan los datos más relevantes según la posición del jugador.
   - **Cotización oficial (derecha)**: tarjeta de 94 px de altura que presenta el precio actual en créditos, insignia de variación (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`), fecha oficial y variación absoluta en créditos.

### Escenario 2: Tres pestañas ordenadas y navegación

1. Al acceder a la página, verificar que la pestaña inicial activa por defecto es **`[ Cotizaciones ]`**.
2. Comprobar el orden visual de las pestañas a todo el ancho:
   `[ Cotizaciones ]` → `[ Resumen deportivo ]` → `[ Todas las estadísticas ]`.
3. Navegar con teclado (flechas izquierda/derecha, Tab, Home, End): el foco y el indicador visual de pestaña activa deben responder de forma accesible.

### Escenario 3: Pestaña de Cotizaciones

1. En la pestaña `Cotizaciones`, inspeccionar el gráfico interactivo SVG de evolución histórica.
2. Pasar el cursor o interactuar con los puntos de cotización: debe mostrarse el tooltip con la fecha y el precio en créditos correspondiente.
3. Contrastar el último punto del gráfico con la cotización oficial mostrada en la cabecera.

### Escenario 4: Resumen deportivo (4 KPI + 2 paneles temáticos)

1. Seleccionar la pestaña `Resumen deportivo`.
2. Verificar las cuatro tarjetas KPI superiores (`SummaryKpiCard`):
   - Icono temático de 24 px en contenedor de 48×48 px.
   - Título de métrica en 12 px legible.
   - Valor numérico destacado en 28 px bold, sin unidades redundantes adosadas al número (ej. `0,4`, no `0,4 regates regates/partido`).
3. Verificar exactamente dos paneles temáticos (`SummaryPanel`) correspondientes al perfil posicional del jugador:
   - **Arquero**: *Atajadas por zona* y *Distribución y juego con pies*.
   - **Defensor**: *Acciones defensivas* y *Duelos y disciplina*.
   - **Mediocampista**: *Construcción y distribución* y *Posesión y equilibrio*.
   - **Delantero**: *Definición por zona* y *Juego ofensivo y generación*.
4. Comprobar que las filas de cada panel tienen una altura acotada entre 44 px y 58 px con espaciado armónico (`justify-content: space-evenly`).
5. Confirmar la ausencia total de barras de progreso relativas sin escala y de bloques redundantes de destacadas.

### Escenario 5: Catálogo completo de 60 métricas

1. Seleccionar la pestaña `Todas las estadísticas`.
2. Verificar las 8 categorías normativas (General 5, Ataque 19, Pases 10, Regates 5, Duelos 3, Defensa 10, Portería 4, Disciplina 4).
3. Utilizar el buscador de métricas en el encabezado: debe filtrar por texto o clave canónica sin distinguir mayúsculas ni acentos.
4. Seleccionar un filtro de categoría: debe actualizar el conteo visible y reflejar el estado con `aria-pressed`.
5. Presionar el botón de limpieza de filtros: debe restablecer la búsqueda y mostrar todas las categorías.
6. Verificar que las métricas con datos se ordenan por relevancia (Principal → Secundaria → Base) y que las métricas ausentes se ubican al final con guion (`-`).

### Escenario 6: Cero scroll en escritorio

1. En resoluciones de escritorio (1920×1080, 1920×913, 1366×768, 1280×800), abrir la pestaña `Resumen deportivo`.
2. Verificar que el contenido completo de la cabecera, pestañas, 4 KPI y 2 paneles encaja perfectamente en el viewport sin generar barra de desplazamiento vertical ni horizontal en la ventana principal.

### Escenario 7: Adaptación responsive

1. Redimensionar el viewport a tablet (768 px):
   - La cabecera se reorganiza en dos filas compactas.
   - Los 4 KPI se organizan en cuadrícula de 2 columnas.
   - El desplazamiento vertical natural funciona fluidamente sin overflow horizontal.
2. Redimensionar a móvil (360 px):
   - La cabecera apila la identidad y muestra las tres micro-tarjetas en fila simétrica.
   - Los KPI y paneles se apilan en una sola columna legible.

### Escenario 8: Manejo de estados y errores

1. **Identificador inválido**: Navegar a `/players/abc` o `/players/-1` → Mensaje de identificador inválido (HTTP 400) y botón para volver al catálogo.
2. **Jugador no encontrado o inactivo**: Navegar a un id inexistente o inactivo → Mensaje de jugador no encontrado (HTTP 404).
3. **Sin sesión**: Acceder sin credenciales → Redirección automática a `/login` por `AuthGuard`.
4. **Fallo de actualización (refetch)**: Si se interrumpe la conexión tras haber cargado la página, la interfaz debe conservar los datos previos exitosos de la Query Cache, mostrando un aviso visible de fallo con opción de reintento.

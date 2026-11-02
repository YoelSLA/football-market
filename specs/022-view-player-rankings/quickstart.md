# Guía Rápida de Validación: Visualizar ranking de jugadores (SPEC-022)

**Feature**: `022-view-player-rankings`
**Spec**: [`spec.md`](spec.md) · **Plan**: [`plan.md`](plan.md) · **Contratos**: [`contracts/player-rankings-consumption.md`](contracts/player-rankings-consumption.md)

Esta guía documenta los pasos para verificar y validar de extremo a extremo la funcionalidad de ranking de jugadores en el frontend.

---

## 1. Prerrequisitos

1. Backend Football Market iniciado y en ejecución (puerto por defecto `http://localhost:8080`).
2. Frontend con dependencias instaladas (`npm install` en `frontend/`).
3. Usuario autenticado con sesión activa (token JWT).

---

## 2. Escenarios de Validación

### Escenario 1: Acceso a la vista desde la navegación
1. Iniciar sesión en la plataforma y hacer clic en la pestaña **Jugadores** del Navbar principal.
2. En la barra secundaria de navegación interna, hacer clic en la pestaña **Ranking**.
3. **Resultado esperado**:
   - La URL cambia a `/players/ranking`.
   - La pestaña *Ranking* se muestra con estilo activo.
   - La cabecera presenta el título *"Ranking de jugadores"* con el selector toggle posicionado en *"Por rendimiento"*.

### Escenario 2: Podio Top 3 y tabla en Modo Rendimiento
1. En la vista `/players/ranking` (modo *Por rendimiento* activo):
2. Verificar el bloque superior de metadatos:
   - Debe exhibir la estrategia activa (ej. `POSITIONAL_PERFORMANCE` formateada), fecha de valuación y total de clasificados.
3. Observar la sección del Podio:
   - El puesto **#1** ocupa el centro con distintivo de laurel dorado, imagen del jugador, nombre, equipo, liga y score con 6 decimales (`0,XXXXXX`).
   - El puesto **#2** se ubica a la izquierda (laureles plateados).
   - El puesto **#3** se ubica a la derecha (laureles bronceados).
4. Observar la tabla de posiciones inferior:
   - Los jugadores se presentan ordenados consecutivamente desde el puesto 1 en adelante.
   - Las columnas `#`, `Jugador`, `Equipo`, `Liga`, `Posición` (pill DEL/MED/DEF/POR), `Score` y `Fecha de valuación` están completas.

### Escenario 3: Alternar a Modo Cotización
1. En la cabecera, hacer clic en el botón **Por cotización**.
2. **Resultado esperado**:
   - El botón *Por cotización* se activa visualmente con borde verde/destacado.
   - El podio se actualiza mostrando a los 3 jugadores de mayor cotización de mercado con su precio en créditos (ej. `3,50 cr.`).
   - La tabla muestra la columna de cotización y los badges de origen (`RECOMPUTED` o `CARRIED_FORWARD`).

### Escenario 4: Filtrado en cliente sobre la tabla
1. En la barra de herramientas, escribir el nombre de un jugador (ej. "Haaland") en *"Buscar jugador..."*.
2. Seleccionar una liga específica en el desplegable de Liga.
3. Seleccionar una posición táctica (ej. `DEL`).
4. **Resultado esperado**:
   - **El Podio Top 3 superior permanece invariable**, mostrando a los líderes globales absolutos.
   - La tabla inferior filtra reactivamente solo los jugadores coincidentes.
   - El número de posición ordinal `#` original de cada jugador se preserva (no se renumeran).
   - El contador del pie se actualiza ("Mostrando X de Y jugadores").
   - Al pulsar *"Limpiar filtros"*, la tabla restaura todos los elementos en la primera página.

### Escenario 5: Paginación
1. Con la tabla completa (sin filtros), verificar que se muestran 10 jugadores por página.
2. Hacer clic en la página `2` o en la flecha siguiente `>`.
3. **Resultado esperado**:
   - La tabla muestra las posiciones del 11 al 20 fluidamente sin recargar la página.
   - El botón de página actual se destaca.

### Escenario 6: Navegación al detalle individual
1. Hacer clic sobre el nombre, avatar o fila de cualquier jugador clasificado.
2. **Resultado esperado**:
   - La aplicación navega a `/players/:id`.
   - Carga la página de detalle individual del jugador correspondiente.

### Escenario 7: Validación de estados excepcionales (Vacío y Error)
1. Simular respuesta vacía (`ranking: []`):
   - La interfaz renderiza el componente `EmptyState` informando que no hay clasificaciones vigentes sin romper el diseño.
2. Simular fallo de servidor (HTTP 500):
   - La interfaz muestra una alerta de error accesible con el botón *"Reintentar"*. Al pulsarlo, se dispara nuevamente la petición.

---

## 3. Verificación de Compilación (Build)

Ejecutar desde el directorio `frontend/`:
```bash
npm run build
```
**Resultado esperado**:
- Compilación de TypeScript y Vite exitosa con código de salida `0` y sin errores de tipos ni de rutas.

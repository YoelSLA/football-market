# Guía de validación: Detalle individual del jugador

Esta guía describe escenarios manuales verificables. El agente no ejecuta tests; el usuario
ejecuta los comandos indicados.

## Prerrequisitos

- Node.js 24.19.0 o compatible con el requisito `>=24`.
- JDK y Gradle del backend, más una base de datos de desarrollo con el catálogo sincronizado y
  al menos un usuario válido. Constitution §2.1 permite datos descartables.
- Origen del frontend permitido por CORS cuando frontend y backend usan orígenes distintos.
- `frontend/.env` con:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

El contrato esperado está en [contracts/player-detail.md](contracts/player-detail.md); los
entidades y estados están en [data-model.md](data-model.md).

## Preparación y ejecución

```bash
# Backend, desde backend/
./gradlew bootRun

# Frontend, desde frontend/
npm ci
npm run dev
```

Abrir la URL informada por Vite, iniciar sesión y navegar a `/players`.

## Escenarios de validación

### Acceso y protección de la ruta

1. Sin sesión válida, abrir `/players/7821` con un id existente.
2. Confirmar que no se muestra el detalle y se navega a `/login`.
3. Iniciar sesión y volver a la URL; confirmar que la identidad se consulta con credenciales.
4. Invalidar o expirar la sesión durante la carga; confirmar retirada del detalle y retorno a
   `/login`.

### Identidad real desde el backend

1. Desde `/players`, seleccionar un jugador y confirmar la navegación a `/players/{id}`.
2. Recargar la página sin caché del catálogo y confirmar que la identidad se obtiene igual.
3. Contrastar nombre, equipo, liga, posición con la tarjeta del catálogo.
4. Verificar el orden de imagen: principal, alternativa y finalmente icono genérico.
5. Con un jugador sin fecha ni nacionalidad, confirmar «No disponible» y no datos inventados.

### Estados y errores

1. Abrir un id con formato inválido: mensaje de identificador inválido y regreso al catálogo.
2. Abrir un id inexistente: «Jugador no encontrado».
3. Con un jugador existente pero inactivo en la base: misma respuesta 404, sin revelar identidad
   ni estado.
4. Detener el backend y reintentar: mensaje de error con acción de reintento, sin bloques de
   demostración y sin identidad ficticia.
5. Cambiar rápidamente entre dos ids y confirmar que solo se presenta el último.

### Estadísticas de demostración

1. Anotar Partidos, Goles, Asistencias y Minutos de dos jugadores distintos.
2. Recargar tres veces y confirmar que los valores no cambian.
3. Contrastar la tarjeta «Rendimiento deportivo» del detalle con las tarjetas del catálogo:
   partidos, goles y asistencias coinciden para la temporada 2025/2026.
4. Confirmar la etiqueta «Datos de demostración» y que no existe selector de temporada.
5. Confirmar que un jugador sin minutos simulados muestra cero sin errores.

### Cotización y evolución

1. Verificar treinta cotizaciones diarias con fechas consecutivas dentro del período fijo.
2. Confirmar que el precio y la fecha mostrados coinciden con el último punto del gráfico.
3. Contrastar la variación con los dos últimos puntos. El caso de **precio anterior cero no es
   reproducible** con la fuente de demostración vigente: `tokenQuoteSeries.ts` fija `MIN_PRICE = 40`,
   por lo que `percentageChange` nunca se resuelve como `null` por base cero en la interfaz.
   Verificación controlada propuesta, sin alterar el generador aprobado: comprobar la rama
   `previous.price === 0` de `buildSnapshot` de forma aislada en una consola o sesión de depuración
   local, invocándola con una serie de dos puntos cuyo penúltimo precio sea `0`, y contrastar que
   devuelve `percentageChange: null` y `direction` coherente con la variación absoluta. Esa
   comprobación es puntual, del usuario y no forma parte de la serie de demostración ni de ningún
   requisito observable en la pantalla.
4. Cambiar la fecha del sistema del navegador y recargar: la serie no cambia.
5. Confirmar que los valores aparecen como precio del token en créditos y como demostración,
   nunca como valor de transferencia.

### Navegación y presentación

1. En escritorio, verificar navbar superior con marca, Inicio, Jugadores activo y sector de
   usuario autenticado; comprobar el retorno al catálogo desde el breadcrumb.
2. Confirmar que todos los controles visibles tienen comportamiento real y foco visible.
3. A 768 y 360 píxeles de ancho, usar el menú desplegable con teclado y confirmar ausencia de
   desplazamiento horizontal.
4. Verificar que la pantalla mantiene la jerarquía de la referencia: ficha azul, tarjeta
   «Rendimiento deportivo» superior derecha, evolución inferior izquierda y estadísticas
   inferiores derechas.
5. Recorrer la evolución mediante su representación textual accesible.
6. Recorrer `/home`, `/players` y `/players/:id` con sesión activa y confirmar que las tres
   pantallas presentan la misma navbar, con la sección correcta marcada como activa en cada una.
7. Navegar por la navbar entre secciones y comprobar que conserva la sesión y no provoca
   desplazamiento horizontal.
8. Bloquear la red y recargar el detalle de un jugador con nacionalidad y equipo: la bandera
   nacional **no se muestra en el detalle**. La bandera y el escudo son recursos decorativos
   opcionales del catálogo (SPEC-005 FR-022) y FR-023 de esta SPEC solo **permite**, sin exigir,
   acompañar la identidad con ellos; el detalle mantiene la identidad textual como canal único.
   Comprobar por ello la degradación de la bandera y la ausencia de escudo en la **tarjeta del
   catálogo** de `/players`, que es donde ambos recursos sí se representan.

> Nota de revisión documental — 2026-10-13: este escenario exigía en su redacción original escudo
> local y bandera degradando en `/players/:id`. La exigencia no se corresponde con un requisito
> vigente: FR-023 es una habilitación («puede»), no un deber, y ninguna US de SPEC-009 muestra
> escudo ni bandera en la ficha principal. Mostrarlos exigiría una decisión de producto sobre el
> diseño aprobado de la ficha, que este documento no sustituye.

## Comandos de verificación permitidos

El agente no ejecuta tests ni builds. Para esta feature:

| Área | Comando | Quién lo ejecuta |
| --- | --- | --- |
| Backend | `./gradlew build -x test` desde `backend/` | Agente, tras cambios de código backend |
| Backend | `./gradlew test` (suite) | Usuario |
| Frontend | `npm run build` desde `frontend/` | Agente, tras cambios de código frontend |
| Frontend | `npm run biome:check` | Usuario o agente según autorización |

El build y los tests no acreditan CODE REVIEW ni TESTING: corresponden a gates humanos
posteriores.

## Criterio de cierre de esta guía

Todos los escenarios deben cumplirse con evidencia observada. El incumplimiento de un
escenario devuelve la US a `IN PROGRESS`; la causa se registra en `tasks.md` y en Trello según el
workflow vigente.
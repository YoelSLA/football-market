# Guía de validación: Página de catálogo de jugadores

## Prerrequisitos

- Node.js 24.19.0 o una versión compatible con el requisito `>=24`.
- Backend local en ejecución con datos de jugadores y un usuario válido.
- Origen del frontend permitido por CORS cuando frontend y backend usen orígenes distintos.
- `frontend/.env` con `VITE_API_URL` apuntando a la base que incluye `/api`, por ejemplo:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

El contrato esperado está en [contracts/players-catalog.md](contracts/players-catalog.md) y los estados se detallan en [data-model.md](data-model.md).

## Preparación y ejecución

Desde `frontend/`, instalar exactamente el lockfile y arrancar el servidor de desarrollo:

```powershell
npm ci
npm run dev
```

Abrir la URL informada por Vite en un navegador. Iniciar sesión y navegar a `/players`. Para revisar
la composición responsive, repetir la validación a 1280, 768 y 360 píxeles de ancho.

## Escenarios de validación

### Acceso privado

1. Sin sesión válida, abrir `/players`.
2. Confirmar que el catálogo no aparece y se navega a `/login`.
3. Iniciar sesión y volver a `/players`; confirmar que la consulta incluye credenciales y la página es visible.
4. Invalidar o expirar la sesión durante un cambio de página; confirmar que se retira el catálogo y se vuelve a `/login`.

### Tarjetas y composición

1. Usar datos que cubran las cinco ligas y las cuatro posiciones documentadas.
2. Confirmar un máximo de 12 tarjetas, cuatro columnas y hasta tres filas a 1280×720, y que la cuadrícula se reorganiza sin desplazamiento horizontal a 768 y 360 píxeles.
3. Verificar en cada tarjeta el retrato o el icono genérico, el nombre, la liga y la posición, además de un bloque central con el equipo real y tres estadísticas etiquetadas como «Datos de demostración», cada atributo con su etiqueta.
4. Comparar las estadísticas de dos jugadores distintos y confirmar que derivan de su identificador; abrir el detalle de uno de ellos y confirmar que coinciden exactamente con los de su tarjeta.
5. Confirmar recursos y color suave por liga y su texto accesible; comprobar que la posición se identifica mediante una etiqueta textual visible, clara y legible sobre el fondo de cada liga.
6. Comprobar que los textos del equipo y de las estadísticas contrastan con el fondo, y que nombres largos no invadan estadísticas ni metadatos.
7. Pasar el puntero sobre cada tarjeta y confirmar que se eleva y refuerza su brillo sin desplazar la cuadrícula ni ocultar datos esenciales; al retirar el puntero recupera su posición. Confirmar además que `Activo` permanece visible sin hover.
8. Confirmar el escudo del equipo y la bandera de nacionalidad; bloquear la red para comprobar que la bandera degrada a marcador de posición sin ocultar el dato textual.

### Paginación y concurrencia

1. Preparar más de 12 jugadores y confirmar que aparece el paginador.
2. Desde una página intermedia, verificar la actual, la anterior, la posterior y los accesos a primera y última; confirmar que las páginas omitidas se comunican con una elipsis y siguen siendo alcanzables.
3. Confirmar que la página actual está identificada y que los destinos fuera de rango no son activables.
4. Cambiar de página rápidamente varias veces y verificar que solo la selección más reciente determina las tarjetas finales.
5. Reducir el catálogo de modo que la página seleccionada deje de existir; confirmar que se consulta y muestra la última página válida.
6. Confirmar que una única página o un catálogo vacío no muestra un paginador engañoso.

### Controles visuales

1. Escribir en búsqueda y cambiar liga y posición.
2. Confirmar que los controles conservan su interacción visual.
3. Verificar que no cambian tarjetas, orden, página, query ni solicitudes HTTP.

### Estados y fallbacks

1. Observar la carga inicial y durante un cambio de página; confirmar feedback visible y ausencia de datos atribuidos a la página equivocada.
2. Probar un catálogo sin jugadores; confirmar un estado vacío comprensible, sin cuadrícula ni paginador.
3. Simular un fallo recuperable; confirmar un mensaje visible y una acción de reintento que repite la misma página.
4. Probar una liga o posición desconocida; confirmar que se conserva el texto y se usa una presentación neutra legible.
5. Revisar navegación por teclado de controles y paginador, foco visible, `role="status"`/`aria-busy` durante carga y `role="alert"` en error.

## Verificación permitida

Después de implementar código frontend, ejecutar desde `frontend/`:

```powershell
npm run build
```

El build comprueba tipos y genera la aplicación, pero no ejecuta ni acredita tests. Por norma del repositorio no se crean ni ejecutan tests frontend; los escenarios anteriores requieren validación manual.

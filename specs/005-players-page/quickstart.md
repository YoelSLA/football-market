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

Abrir la URL informada por Vite en un navegador de escritorio con viewport de al menos 1280×720. Iniciar sesión y navegar a `/players`.

## Escenarios de validación

### Acceso privado

1. Sin sesión válida, abrir `/players`.
2. Confirmar que el catálogo no aparece y se navega a `/login`.
3. Iniciar sesión y volver a `/players`; confirmar que la consulta incluye credenciales y la página es visible.
4. Invalidar o expirar la sesión durante un cambio de página; confirmar que se retira el catálogo y se vuelve a `/login`.

### Tarjetas y composición

1. Usar datos que cubran las cinco ligas y las cuatro posiciones documentadas.
2. Confirmar un máximo de 12 tarjetas, cuatro columnas y hasta tres filas, sin desplazamiento horizontal a 1280×720.
3. Verificar en cada tarjeta icono genérico, nombre, equipo, liga, posición y las tres estadísticas comunes claramente identificadas como muestra.
4. Confirmar recursos y color suave por liga, recurso por posición y texto accesible para ambas clasificaciones.
5. Comprobar que nombres largos no invaden estadísticas ni metadatos.
6. Pasar el puntero sobre cada tarjeta y confirmar que `Activo` entra desde la izquierda sin desplazar la cuadrícula ni ocultar información esencial; al retirar el puntero debe ocultarse de nuevo.

### Paginación y concurrencia

1. Preparar más de 12 jugadores y confirmar que aparece el paginador.
2. Desde una página intermedia, verificar la actual, hasta tres anteriores, hasta tres posteriores y accesos a primera y última.
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

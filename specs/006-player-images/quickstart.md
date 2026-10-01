# Guía de validación: imágenes de jugadores

## Preparación

Consultar el [contrato HTTP](contracts/api.md) y los [estados persistidos](data-model.md). Se necesita PostgreSQL local, backend configurado con acceso a Football-Data.org, conexión HTTPS a TheSportsDB y un JWT válido para el usuario. Usar un cliente HTTP que permita esperas largas: cada jugador pendiente puede requerir al menos 2 segundos y una ejecución con cientos de jugadores puede durar muchos minutos. No utilizar un timeout corto del cliente ni esperar un endpoint de progreso.

Los comandos de arranque y configuración del entorno se encuentran en `backend/README.md` y en las instrucciones de desarrollo del proyecto; comprobar su vigencia antes de utilizarlos. Para validar manualmente esta feature no es necesario enviar solicitudes a TheSportsDB desde el navegador: el backend hace el enriquecimiento.

## Recorrido principal

1. Con un JWT válido, ejecutar `POST /api/players/sync` para poblar el catálogo si está vacío. Consultar `GET /api/players?page=0&size=12`: comprobar `imageUrl: null` en jugadores pendientes y que las tarjetas usan el recurso genérico.
2. Ejecutar `POST /api/players/images/sync` autenticado, sin body. Esperar a que concluya la respuesta. Comprobar `200` con `processed`, `found` y `withoutImage`; `processed = found + withoutImage`.
3. Volver a consultar las páginas: los jugadores encontrados exhiben una URL de retrato en `imageUrl` y una imagen visible; los restantes conservan `null` e ícono genérico. La paginación, los nombres y los equipos permanecen sin cambios.
4. Repetir el POST: debe evitar consultas para jugadores ya resueltos. Si se sincronizan de nuevo los mismos jugadores desde Football-Data.org, sus imágenes permanecen. Una modificación de nombre o equipo deja ese jugador pendiente de una nueva búsqueda manual.

## Casos de error y concurrencia

- Invocar el POST sin JWT: `401`, sin iniciar búsquedas. Durante una ejecución activa, invocarlo con otro JWT: `409` y `code: PLAYER_IMAGE_SYNC_IN_PROGRESS`, sin un segundo trabajo.
- Si una búsqueda devuelve otro equipo, ausencia de coincidencia o imagen inválida, no asignar URL; mostrar el ícono genérico.
- Simular un `429`: respetar `Retry-After` si existe; si no, esperar al menos 60 segundos antes de reintentar. Entre inicios de peticiones debe haber al menos 2 segundos y no más de 30 peticiones por ventana de 60 segundos, incluidos reintentos.
- Si el proveedor falla tras guardar varias fotos: `502` con `code: PLAYER_IMAGE_SYNC_INCOMPLETE`; las imágenes ya obtenidas siguen accesibles y los pendientes se reintentan mediante otra invocación manual.
- Si una URL de imagen ya guardada deja de cargar en el navegador, la tarjeta usa el ícono genérico sin afectar la lectura del catálogo.

## Verificación en implementación

La persona que ejecute las pruebas del backend deberá aportar los resultados de las clases concretas `PlayerTest`, `PlayerRepositoryTest`, `TheSportsdbServiceTest`, `TheSportsDbIntegrationTest`, `PlayerCatalogServiceTest`, `TheSportsDbPropertiesTest` y `PlayerControllerTest`. El `401` del endpoint de imágenes se verifica en `PlayerControllerTest`, por lo que no se requiere una prueba adicional en `security/`. El agente no ejecuta tests. Para código backend/frontend, los únicos builds permitidos al agente son `./gradlew build -x test` en `backend/` y `npm run build` en `frontend/`, respectivamente. Este paso de planificación es documental y no requiere build.

# Investigación: imágenes de jugadores

## Proveedor y datos de imagen

- **Decisión**: Usar la API gratuita v1 de TheSportsDB, `searchplayers.php?p=...`, con clave pública `123` y retrato `strThumb`. Consultar solo jugadores activos del catálogo local.
- **Motivo**: La búsqueda devuelve nombre (`strPlayer`), equipo (`strTeam`), deporte (`strSport`) y URL de retrato. El ID de TheSportsDB es distinto del ID de Football-Data.org. La versión gratuita limita la búsqueda a un resultado y 30 peticiones/minuto: una única respuesta no demuestra identidad si los datos no coinciden.
- **Alternativas**: Obtener plantillas por equipo añade emparejamiento de equipos e incertidumbre por planteles desactualizados; pedir `lookupplayer.php` requiere conocer el ID del otro proveedor; v2 es premium.
- **Referencia**: [Documentación oficial](https://www.thesportsdb.com/docs_api_guide), secciones Search Players, Images y Rate Limit.

## Identidad y repetición

- **Decisión**: Coincidencia conservadora de nombre y equipo, tras normalización mínima de espacios, mayúsculas y acentos; exigir `Soccer` y exactamente un resultado que cumpla. No inferir identidad solo por nombre. Guardar URL y un estado `PENDING`/`FOUND`/`NO_MATCH`; volver a `PENDING` si cambian nombre o equipo del jugador.
- **Motivo**: IDs heterogéneos, homónimos, transferencias y representaciones divergentes hacen incorrecto aceptar un resultado aproximado. El estado sin foto evita consultar reiteradamente a quienes no tienen coincidencia; los errores quedan pendientes para próximas invocaciones manuales.
- **Alternativas**: Coincidencia difusa o caché solo en memoria pierde precisión o reanudación tras reinicio. La normalización exacta elegida puede dejar más jugadores sin foto, resultado compatible con la spec.

## Límite, 429 y ejecución prolongada

- **Decisión**: Una sola ejecución por instancia, peticiones secuenciales espaciadas ≥2 s y contador de ventana móvil de 60 s antes de iniciar cada petición. Incluir reintentos en ambos límites. Ante 429, esperar `Retry-After` válido (segundos o fecha HTTP), o ≥60 s si falta/no es válido, sin violar el control de ventana; máximo tres reintentos por búsqueda y luego abortar ejecución con error incompleto.
- **Motivo**: Lanzar peticiones concurrentes no aumenta la cuota; exactamente dos segundos entre inicios podría situar 31 solicitudes en el borde de una ventana inclusiva. La guía oficial indica 30/minuto y esperar otro minuto después de 429, sin prometer cabecera `Retry-After`.
- **Alternativas**: Solo usar `sleep(2s)` no protege los límites de ventana; concurrencia multiplica los 429. Retry indefinido impediría cerrar la solicitud.

## Persistencia y atomicidad

- **Decisión**: Guardar cada resultado hallado o ausencia definitiva mediante una transacción corta independiente. Obtener siguientes pendientes desde BD sin retener transacciones durante HTTP/esperas. Un fallo externo conserva resultados previos y deja pendientes intactos; reinicio o desconexión exige nueva invocación manual. La sincronización de Football-Data.org conserva imagen de registros sin cambios y la invalida si varían nombre/equipo.
- **Motivo**: La spec exige avance parcial durable, reanudación y que el GET local permanezca disponible. Una transacción única de todo el lote produciría rollback y bloqueo innecesario durante minutos.
- **Alternativas**: Mantener una lista de todos los IDs solo en memoria no permite reanudar con seguridad; sincronización automática contradice aclaración del usuario.

## Contrato y límites operativos

- **Decisión**: `POST /api/players/images/sync` sin body responde solo al concluir, devuelve resumen numérico en 200, 409 para ejecución activa y 502 con error estándar para fallo de proveedor. No hay endpoint de progreso. El GET incorpora `imageUrl: string | null`. La sincronización de imágenes no modifica el resultado ni la ejecución del endpoint de Football-Data.org.
- **Motivo**: Reutiliza autenticación y patrón HTTP existentes. El cliente manual debe tolerar duración proporcional a cantidad de pendientes, pausas y posibles timeouts propios; si se interrumpe, repetir la invocación. El código de error permite distinguir operación incompleta sin revelar datos externos.
- **Alternativas**: Aceptar en segundo plano con 202/estado requiere otro contrato expresamente descartado; cancelar todo en una transacción contradice conservar fotos parciales.

## URL y vista

- **Decisión**: Admitir únicamente HTTPS en `r2.thesportsdb.com` con ruta de imagen de jugador `images/media/player/thumb/`, sin userinfo ni query/fragment; persistir solo URL validada. La tarjeta muestra el recurso o el ícono actual si URL nula o error al cargar. Mostrar la versión `/small` solo si la URL base pasó la validación.
- **Motivo**: El proveedor es dato no confiable y la página no debe renderizar enlaces arbitrarios. El respaldo local ya existe. Las imágenes tienen licencias individuales que deberán revisarse antes de cualquier publicación pública.
- **Alternativas**: Descargar/copiar binarios agrega almacenamiento y gestión de licencias sin necesidad para uso local; confiar ciegamente en cualquier URL es innecesario.

# Investigación: imágenes de jugadores

Fuente funcional: [spec.md](spec.md). Las decisiones siguientes son de diseño; las reglas funcionales no se infieren del diseño anterior. No se ejecutaron pruebas ni solicitudes reales al proveedor en este paso.

## Acceso a TheSportsDB y respuestas

- **Decisión**: Utilizar la API v1 gratuita: `searchplayers.php?p=...` únicamente si falta la referencia `THE_SPORTS_DB`; utilizar `lookupplayer.php?id=...` si ya existe. En v1 la clave se transmite en el segmento de ruta `/api/v1/json/{apiKey}/`, nunca en `X-API-KEY` (propio de v2/premium); no registrar la URL completa ni exponer la clave en logs, errores o respuestas. Aprovechar identidad e imágenes de la misma respuesta de búsqueda sin un lookup adicional. Adaptar las raíces `player` y `players` respectivamente; una raíz con valor `null` es resultado válido sin datos (`NOT_FOUND`, aun con imágenes previas conservadas, sin retry técnico). No confundirla con identidad esperada devuelta sin imágenes nuevas: `FOUND` si se conservan imágenes válidas previas, `NOT_FOUND` si no había ninguna.
- **Motivo**: Evita peticiones superfluas, distingue descubrimiento de identidad de enriquecimiento posterior y cumple la prohibición de rematching, también con `force=true`.
- **Alternativas descartadas**: buscar por nombre cada vez; hacer lookup después de cada búsqueda exitosa; usar la API v2 de pago. La documentación del proveedor indica que el plan gratuito de búsqueda puede limitar resultados a uno; aun así, procesar cualquier cantidad de candidatos devueltos y aceptar solo uno válido. Véase [guía v1 de TheSportsDB](https://www.thesportsdb.com/docs_api_guide).

## Matching y validación externa

- **Decisión**: Comparar nombre principal y alternativo con igual peso tras normalización conservadora de caja, diacríticos, espacios y guiones. Aplicar a equipos el mismo tratamiento más una lista explícita y acotada de sufijos futbolísticos conocidos (`FC`, `CF`, `AC`, `SC`, etc.), sin equivalencias arbitrarias. Fecha: igualdad por calendario ISO sin tolerancia; nacionalidad: normalización conservadora y tabla pequeña, explícita y revisable de alias equivalentes. Rechazar deporte ausente o diferente de `Soccer`, ignorar `relevance`. Con equipo coincidente, fecha/nacionalidad presentes en ambos lados vetan si contradicen; con equipo distinto, ambas deben estar presentes y coincidir. Evaluar todos los candidatos y aceptar exactamente uno.
- **Motivo**: La regla admite cambios de equipo solo con verificaciones fuertes, sin introducir puntuaciones ni fuzzy matching.
- **Alternativas descartadas**: match solo por nombre; comparación fuzzy; escoger el primer candidato; desambiguar por `relevance`. La lista inicial de alias/sufijos se documentará como dato controlado en implementación, no se expandirá automáticamente.

Para una referencia ya resuelta, validar solo la identidad consultada por el `externalId` persistido con los datos fuertes presentes (nombre principal/alternativo, equipo, fecha de nacimiento, nacionalidad y deporte `Soccer`) según FR-009–FR-012. Declarar `FAILED` únicamente ante incompatibilidad inequívoca; la falta de datos opcionales por sí sola no demuestra contradicción. No repetir matching, buscar otro candidato, usar relevancia ni alterar la referencia. La respuesta válida sin resultados se trata como `NOT_FOUND`, no como contradicción.

## Identidad persistida e imágenes

- **Decisión**: Mantener `Player.id` interno y reutilizar el provider `THE_SPORTS_DB` ya definido por `002-player-catalog` para `PlayerExternalReference`, independiente de las fotos; al resolver identidad, persistir la referencia incluso sin URL válida. No crear otro valor de provider. Agregar `fallbackImageUrl` nullable a `Player` existente. Elegir `strCutout` como principal y `strThumb` como alternativa si ambas son válidas y distintas; si solo la secundaria sirve, promoverla a principal sin duplicado. En refresh conservar URLs anteriores que no tienen sustitución válida; mantener `FOUND` con imagen persistida solo si la respuesta devuelve la identidad esperada (no si el lookup devuelve cero resultados: `NOT_FOUND`).
- **Motivo**: `002-player-catalog` ya define `imageUrl` y referencia externa; la spec exige ambas URLs y continuidad de imágenes y referencias ante fallos. El frontend hace fallback por error real de carga sin petición adicional del backend.
- **Alternativas descartadas**: guardar origen de cada URL; asociar identidad solo si hay imagen; borrar URLs al fallar un refresh.

## Seguridad de URLs y HTTP externo

- **Decisión**: Validar URI y comparar esquema `https` y hostname normalizado: `thesportsdb.com` o sufijo `.thesportsdb.com` con etiqueta real previa. No aceptar credenciales embebidas ni redirecciones automáticas hacia hosts no validados; no hacer HEAD/GET adicional para probar imágenes. Configurar cliente HTTP seguro, con timeouts y parámetros escapados; no registrar secretos ni respuestas completas.
- **Motivo**: Un substring del dominio no identifica un host confiable y el recurso se mostrará al navegador. La carga efectiva se maneja en la tarjeta mediante fallback.
- **Alternativas descartadas**: aceptar cualquier URL del proveedor; consultar la imagen para comprobar su contenido; validar por substring.

## Estado de enriquecimiento y creación de jugadores

- **Decisión**: Una resolución separada 1:1 con `Player` guarda estado y `lastAttemptAt`. Migrar jugadores ya existentes a `PENDING`; al crear un nuevo jugador desde Football-Data.org, crear su resolución `PENDING` en la misma operación de alta sin introducir dependencia externa. Si falta resolución por inconsistencia, repararla en el recorrido de imágenes. Calcular elegibilidad por estado y antigüedad de `NOT_FOUND` (30 días por defecto), sin reabrir `FOUND`/`FAILED` salvo `force=true`. Los jugadores inactivos al llegar su turno individual quedan excluidos, incluso si estaban activos al inicio del run.
- **Motivo**: Evita estado operativo en Player y hace duraderos `NOT_FOUND`, `RETRYABLE_ERROR`, `FAILED` y la recuperación.
- **Alternativas descartadas**: columna de estado en `players`; usar `imageUrl == null` como indicador de nunca buscado; crear resolución consultando TheSportsDB durante sync del catálogo.

## Rate limiting, 429 y errores

- **Decisión**: Un único control global de intervalo por instancia, compartido por búsqueda, lookup y todos los reintentos, separa los inicios de solicitudes por al menos el intervalo configurado (default y mínimo 2500 ms). Tras `429`, el próximo intento respeta tanto ese intervalo como `Retry-After` válido; si falta o no puede utilizarse, espera al menos 60 s. Máximo configurable de reintentos posteriores al inicial (default 3). Distinguir errores transitorios (timeout, red, `5xx`, `429`) de permanentes (`4xx` restantes, respuestas permanentemente inválidas, contradicciones/conflictos); no equiparar raíz nula o imagen inválida a error permanente.
- **Motivo**: A 2500 ms entre inicios hay como máximo 24 solicitudes en cualquier ventana móvil de 60 s (tomando una ventana semiabierta), por debajo del límite operativo documentado de 30/min. Un intervalo configurado mayor solo reduce el ritmo. No hace falta un algoritmo adicional de ventana móvil mientras la cuota y el despliegue por instancia sigan siendo los asumidos. Temporal agotado ⇒ `RETRYABLE_ERROR` inmediatamente elegible en próxima ejecución; permanente ⇒ `FAILED`, solo force. La espera es interrumpible/observable sin retener recursos de persistencia.
- **Alternativas descartadas**: intervalo antiguo de 2 s; ventana móvil de 30/60 s redundante con el intervalo mínimo actual; retries automáticos fuera del control de ritmo; abortar la ejecución por cualquier error individual.

## Concurrencia, recorrido y preservación parcial

- **Decisión**: Exclusión por instancia no bloqueante para la segunda solicitud (`409`) y recorrido por identificador estable sin paginación offset sobre elegibilidad mutable ni instantánea global de activos. Al llegar el turno individual, comprobar `active`: si es falso, excluir sin item, `evaluated`, `processed` ni solicitud externa, preservando datos previos; si es verdadero, comenzar formalmente su evaluación, crear exactamente un item y sumar `evaluated`, incluso si se omite por elegibilidad. Un cambio posterior de `active`, incluso durante una espera, no cancela esa evaluación: completar el item y conservar trabajo y resultados persistidos; `processed` solo cuenta si hubo intento real contra TheSportsDB. Persistir resultados de cada jugador y su item en unidades breves; registrar el run y cerrarlo de manera independiente. Nunca sostener una transacción durante llamadas/esperas remotas. Resolver choques de unicidad `(provider, externalId)` sin reasignar jugadores ni perder otros resultados.
- **Motivo**: La pertenencia por actividad al inicio de la evaluación individual (FR-020) da una frontera definida para cambios concurrentes; no exige volver a comprobar `active` después ni deja items huérfanos por inactivación. Garantiza `evaluated` igual a los items creados para activos evaluados, trazabilidad de skips y resultados parciales duraderos.
- **Alternativas descartadas**: mutex alrededor de operación bloqueante que hace esperar a la segunda solicitud; offset sobre conjunto de pendientes; una sola transacción para todo el run.

## Auditoría, reinicio y retención

- **Decisión**: Persistir run con estado, fechas, `force`, contadores diferenciados y `failureReason`; crear un item para cada activo inspeccionado, con estado anterior/final, resultado, flags y motivo de omisión. Un conflicto incrementa tanto `failed` (jugador) como `conflicts` (causa), sin contarlo dos veces en `processed`. `NOT_FOUND` permite `COMPLETED`; un error individual o conflicto ⇒ `PARTIAL`; fallo global ⇒ `FAILED`. Ante reinicio, cerrar runs `RUNNING` como `FAILED` con `INTERRUPTED_BY_RESTART` y solo items inconclusos como `INTERRUPTED`, conservando items terminados. La limpieza del historial es configurable y elimina conjuntamente run e items; no se impone en la spec default de días.
- **Motivo**: Distingue eventos funcionales de fallo global, no pierde evidencia y deja consultables los resultados de solicitudes largas.
- **Alternativas descartadas**: introducir `INTERRUPTED` como estado de resolución o sumarlo al contador `failed`; incluir todos los items en respuesta del POST; fijar 90 días como requisito.

## Contratos y frontend

- **Decisión**: Extender el DTO público del catálogo con `fallbackImageUrl` nullable (conservar `imageUrl` y todos los campos ya existentes). Exponer POST manual síncrono con `force=false` por defecto; GETs autenticados separados para historial paginado, resumen y items paginados. El POST devuelve solo resumen y `200` para `COMPLETED`/`PARTIAL`, `409` ante run activo, error HTTP para `FAILED`. Actualizar frontend DTO, Model y mapper; la tarjeta alterna `imageUrl` → `fallbackImageUrl` → icono genérico al fallar cada carga y muestra imagen completa sin recorte.
- **Motivo**: Son contratos observables ya fijados; los items pueden ser numerosos y no caben en una respuesta de resumen. No exponer resolución ni referencias externas en `GET /api/players`.
- **Alternativas descartadas**: añadir imagen por primera vez al catálogo; frontend consultando TheSportsDB; página nueva de administración no especificada.

## Paginación de auditoría

- **Decisión de diseño (no requisito de la spec)**: Para los GET de historial e items, seguir la convención de paginación del catálogo `002-player-catalog`: `page` por defecto 0, `size` por defecto 20, `size` entre 1 y 100; mantener orden estable en cada recurso.
- **Motivo**: Reutiliza convenciones de respuestas y validación que conocen los consumidores existentes; la spec solo exige paginación, no estos valores.
- **Alternativas descartadas**: carga de todos los items en una respuesta o valores diferentes sin justificación.

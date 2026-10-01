# Tareas: Imágenes de jugadores

**Entrada**: `specs/006-player-images/spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/api.md` y `quickstart.md`.

**Organización**: Las tareas se agrupan por historia de usuario. El agente no ejecuta tests: su implementación corresponde al usuario. Los tests nuevos usan `@ActiveProfiles("test")` y siguen las categorías de `docs/backend/testing.md`. No se crean tests de frontend.

**Formato**: `[ID] [P?] [US?] Descripción con ruta`. `[P]` indica que la tarea puede ejecutarse en paralelo con las otras marcadas en su mismo tramo, siempre que se hayan completado sus dependencias explícitas.

## Fase 1: Preparación

**Objetivo**: Confirmar la infraestructura existente antes de ampliar el catálogo.

### Backend

- [ ] T001 Verificar en `backend/build.gradle.kts` que RestClient, Spring Data JPA, Flyway, SpringDoc, REST Docs, Mockito y Testcontainers ya cubren esta feature; no añadir dependencias nuevas.
- [ ] T002 [P] Revisar en `backend/src/main/java/footballmarket/config/SecurityConfig.java` que `POST /api/players/images/sync` queda cubierta por la autenticación Bearer vigente sin exigir rol adicional, sin modificar roles.

## Fase 2: Base compartida

**Objetivo**: Persistir la imagen opcional y su estado sin alterar la identidad ni el comportamiento actual del catálogo.

**Bloqueo**: Completar esta fase antes de las historias de usuario.

### Backend

- [ ] T003 [P] Crear `backend/src/main/resources/db/migration/V3__add_player_image.sql` con URL nullable y estado de resolución no nulo, valor inicial `PENDING` para jugadores existentes y sin modificar la clave primaria ni `active`.
- [ ] T004 [P] Implementar en `backend/src/main/java/footballmarket/models/enums/PlayerImageResolution.java` el enum de estados `PENDING`, `FOUND` y `NO_MATCH` sin lógica de transición; las invariantes de URL pertenecen a `Player`.
- [ ] T005 Ampliar `backend/src/main/java/footballmarket/models/Player.java` con la imagen y su estado, un método para registrar una imagen hallada, otro para registrar ausencia de coincidencia y otro que devuelva a `PENDING` con URL nula cuando cambian nombre o equipo. El mapeo JPA debe usar las columnas `image_url` nullable y `image_resolution` no nula con `@Enumerated(EnumType.STRING)`, y el constructor vigente debe dejar el estado en `PENDING` sin URL, de modo que todo jugador nuevo o creado por la sincronización de Football-Data.org nazca pendiente.
- [ ] T006 [P] Ampliar `backend/src/test/java/footballmarket/models/PlayerTest.java` para las transiciones `PENDING`, `FOUND` y `NO_MATCH`, las invariantes de URL, la conservación de la imagen al actualizar otros campos y su invalidación cuando cambian nombre o equipo.
- [ ] T007 Ampliar `backend/src/main/java/footballmarket/repositories/PlayerRepository.java` con la consulta de jugadores activos en estado `PENDING` ordenados por identificador y con las operaciones de lectura y guardado necesarias, sin lógica de negocio.
- [ ] T008 Ampliar `backend/src/main/java/footballmarket/services/impl/PlayerCatalogServiceImpl.java` para que la aplicación de la sincronización de Football-Data.org conserve la imagen y su estado si nombre y equipo no cambian, y para que los invalide cuando sí cambien, sin alterar contadores ni estados activos.
- [ ] T009 [P] Ampliar `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` para verificar la conservación de la imagen y su invalidación por cambio de identidad durante una sincronización exitosa.

**Punto de control**: El esquema y el comportamiento de la imagen existen sin cambiar el contrato HTTP actual.

## Fase 3: Historia de usuario 1 — Reconocer al jugador por su imagen (P1, MVP)

**Objetivo**: Exponer `imageUrl` en el catálogo y mostrar el retrato o el ícono genérico en la tarjeta.

**Prueba independiente**: Con jugadores con imagen, sin imagen y con imagen que no carga, `GET /api/players` devuelve `imageUrl` nullable y la tarjeta muestra el retrato correcto o el recurso genérico sin perder nombre, equipo, estadísticas, liga y posición.

### Tests del backend

- [ ] T010 [P] [US1] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` para verificar `imageUrl` presente y `null` en la respuesta paginada, junto con los snippets REST Docs de `GET /api/players`. Verificar además que la consulta se atiende sin interactuar con `TheSportsDbIntegration`, conforme a SC-003.

### Implementación del backend

- [ ] T012 [P] [US1] Añadir el campo nullable documentado a `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerResponseDTO.java` sin alterar los campos existentes, y completar en `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` el mapeo de `imageUrl` en `toResponse(Player)` y `toResponse(Page<Player>)`, sin lógica de negocio.

### Implementación del frontend

- [ ] T013 [P] [US1] Ampliar `frontend/src/features/players/types/dtos.ts` y `frontend/src/features/players/types/models.ts` con la imagen opcional, manteniendo DTO y Model separados.
- [ ] T014 [US1] Actualizar `frontend/src/features/players/players.mapper.ts` para convertir la imagen del DTO al Model, sin HTTP ni lógica de negocio.
- [ ] T015 [US1] Actualizar `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` y su SCSS Module para mostrar la imagen cuando exista y volver al ícono genérico si falta o falla la carga, conservando la información textual de la tarjeta. Derivar la variante `/small` solo a partir de una `imageUrl` ya validada por el backend.

**Punto de control**: US1 entrega la tarjeta con retrato sin ninguna búsqueda externa.

## Fase 4: Historia de usuario 2 — Sincronizar imágenes manualmente (P1)

**Objetivo**: Buscar retratos de TheSportsDB bajo demanda manual, respetando la cuota y con exclusión de ejecuciones simultáneas.

**Prueba independiente**: Con jugadores activos pendientes, `POST /api/players/images/sync` busca en secuencia, respeta al menos 2 segundos entre inicios de petición y 30 por ventana de 60 segundos, guarda los resultados progresivamente, devuelve los contadores al terminar, responde `409` si ya hay una ejecución y `401` sin JWT.

### Tests del backend

- [ ] T016 [P] [US2] Crear `backend/src/test/java/footballmarket/integrations/TheSportsDbIntegrationTest.java` con `MockRestServiceServer` para coincidencia inequívoca de nombre y equipo, homónimos, respuestas vacías, URL `strThumb` inválida, HTTP de error y estructura inválida. Verificar con `Clock` controlado el espaciado mínimo de 2 s, la regla exacta de la ventana (máximo 30 inicios en cualquier intervalo de 60 s, medida antes de cada envío, reintentos incluidos), el `Retry-After` válido y la espera mínima de 60 s cuando falta, y el límite de reintentos por búsqueda.
- [ ] T017 [P] [US2] Crear `backend/src/test/java/footballmarket/services/TheSportsdbServiceTest.java` con `@SpringBootTest`, `@ActiveProfiles("test")`, inyección por `@Autowired` de la interfaz del Service, Integration con `@MockitoBean` y PostgreSQL real mediante Testcontainers. Cubrir el recorrido secuencial de pendientes, el `409` por ejecución activa, los contadores y la conservación de resultados previos ante fallo, sin duplicar las pruebas de ritmo propias de Integration.
- [ ] T018 [P] [US2] Crear `backend/src/test/java/footballmarket/repositories/PlayerRepositoryTest.java` con PostgreSQL real y Flyway aplicado para la consulta de jugadores activos en estado `PENDING` ordenados por identificador y para la conservación de la imagen al guardar, conforme a la categoría Repository de `docs/backend/testing.md`.
- [ ] T019 [P] [US2] Ampliar `backend/src/test/java/footballmarket/controllers/PlayerControllerTest.java` con el endpoint de imágenes: éxito con contadores, `409`, `502` de ejecución incompleta, `401` sin invocación del Service y snippets REST Docs. El `401` del nuevo endpoint queda cubierto aquí, por lo que no se requiere un test adicional en `security/`.
- [ ] T020 [P] [US2] Crear `backend/src/test/java/footballmarket/config/TheSportsDbPropertiesTest.java` para el binding de base HTTPS, ruta de búsqueda, clave pública, timeouts y límites de ritmo, sin versionar credenciales.

### Implementación del backend

- [ ] T021 [P] [US2] Crear `backend/src/main/java/footballmarket/config/TheSportsDbProperties.java` con base HTTPS, ruta de búsqueda de jugadores, clave pública, timeout de conexión, timeout de lectura y límites de ritmo (espaciado mínimo, máximo por ventana y reintentos por búsqueda), y enlazarlos con valores concretos en `backend/src/main/resources/application.properties` y en `backend/src/test/resources/application-test.yml`.
- [ ] T022 [P] [US2] Crear `backend/src/main/java/footballmarket/config/TheSportsDbClientConfig.java` con `RestClient` HTTPS sin redirecciones, timeout de conexión y timeout de lectura tomados de `TheSportsDbProperties`. El timeout de lectura no debe ser inferior a la duración esperada de una ejecución y no se habilitan reintentos automáticos.
- [ ] T023 [US2] Crear `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` para buscar por nombre, normalizar conservadoramente nombre y equipo, validar coincidencia única y `strThumb` (HTTPS, host y ruta esperados, sin credenciales, consulta ni fragmento), traducir errores técnicos y devolver la imagen válida o ausencia de coincidencia. Encapsular en la misma Integration el ritmo compartido entre llamadas, la ventana móvil de 30 inicios por 60 s medida antes de cada envío, y los reintentos de `429` con `Retry-After` y espera mínima de 60 s. El ritmo se apoya en un `Clock` inyectado para que la espera sea determinista y verificable, como en `FootballDataIntegration`.
- [ ] T026 [P] [US2] Crear `backend/src/main/java/footballmarket/exceptions/ApplicationException.java` como categoría base de errores controlados de la aplicación, y `backend/src/main/java/footballmarket/integrations/exceptions/TheSportsDbUnavailableException.java` para el fallo técnico del proveedor.
- [ ] T027 [US2] Crear la interfaz `backend/src/main/java/footballmarket/services/TheSportsdbService.java` y su implementación en `backend/src/main/java/footballmarket/services/impl/TheSportsdbServiceImpl.java` con la ejecución manual: exclusión de una sola ejecución por instancia, recorrido de candidatos pendientes en orden estable, delegación de cada búsqueda a Integration, persistencia de cada resultado en transacción breve, comprobación de identidad vigente antes de guardar y contadores `processed`, `found` y `withoutImage`.
- [ ] T028 [US2] Añadir en `backend/src/main/java/footballmarket/controllers/PlayerController.java` el endpoint `POST /api/players/images/sync` que delega en el Service y traduce el resultado con el Mapper, documentando `200`, `401`, `409` y `502` en OpenAPI.
- [ ] T029 [P] [US2] Crear `backend/src/main/java/footballmarket/controllers/dtos/responses/PlayerImageSyncResponseDTO.java` y ampliar `backend/src/main/java/footballmarket/controllers/mappers/PlayerMapper.java` para convertirlo sin lógica de negocio.
- [ ] T030 [P] [US2] Añadir `backend/src/main/java/footballmarket/services/exceptions/PlayerImageSyncInProgressException.java` y `backend/src/main/java/footballmarket/services/exceptions/PlayerImageSyncIncompleteException.java`, ambos heredando de `ApplicationException` porque el `409` y el `502` son decisiones del caso de uso y no fallos técnicos del proveedor. Traducir `TheSportsDbUnavailableException` a `PlayerImageSyncIncompleteException` en el Service, y ambas exceptions de aplicación a `409` y `502` en `backend/src/main/java/footballmarket/controllers/exceptions/GlobalExceptionHandler.java` con mensajes seguros en español.
- [ ] T031 [US2] Registrar en `backend/src/main/java/footballmarket/services/impl/TheSportsdbServiceImpl.java` el inicio y el resultado de la ejecución; registrar en `backend/src/main/java/footballmarket/integrations/TheSportsDbIntegration.java` los descartes por coincidencia dudosa, sin incluir credenciales ni datos sensibles.

**Punto de control**: La sincronización manual de imágenes funciona y no afecta la de jugadores.

## Fase 5: Historia de usuario 3 — Continuar tras resultados incompletos o límite de cuota (P2)

**Objetivo**: Mantener el catálogo consultable y reanudable ante `429`, errores del proveedor e interrupciones.

**Prueba independiente**: Ante `429` con y sin `Retry-After`, fallo de comunicación o corte de la solicitud, el catálogo sigue disponible, las imágenes ya guardadas permanecen y una nueva invocación procesa solo lo pendiente.

### Tests del backend

- [ ] T032 [P] [US3] Ampliar `backend/src/test/java/footballmarket/services/TheSportsdbServiceTest.java` con `429` propagado desde Integration, fallo a mitad de ejecución y reanudación de los pendientes, manteniendo `@ActiveProfiles("test")` y Testcontainers.
- [ ] T033 [P] [US3] Ampliar `backend/src/test/java/footballmarket/services/PlayerCatalogServiceTest.java` para verificar que un fallo del proveedor de imágenes no altera jugadores, estados ni resultado de la sincronización de Football-Data.org, y que la sincronización de jugadores no invoca `TheSportsDbIntegration` ni se ralentiza por ella.

### Implementación del backend

- [ ] T034 [US3] Ajustar `backend/src/main/java/footballmarket/services/impl/TheSportsdbServiceImpl.java` para liberar la exclusión de ejecución al salir por éxito o error y dejar pendientes solo lo no resuelto, sin trabajo automático posterior.
- [ ] T035 [US3] Comprobar en `backend/src/test/java/footballmarket/services/TheSportsdbServiceTest.java` que la interrupción del cliente no deja bloqueos ni transacciones abiertas y que la siguiente invocación reanuda desde lo pendiente.

**Punto de control**: Las tres historias son funcionales e independientes.

## Fase 6: Acabado y trabajo transversal

**Objetivo**: Dejar coherentes contrato, documentación y verificación.

### Backend

- [ ] T036 [P] Actualizar la fuente `backend/src/docs/asciidoc/index.adoc` con el endpoint de imágenes y el campo `imageUrl`, sin editar salidas generadas.
- [ ] T037 [P] Actualizar `docs/backend/technologies.md` si el uso de RestClient, JPA o alguna dependencia cambia de propósito o de versión en esta feature.

### Documentación de la feature

- [ ] T038 [P] Revisar `specs/005-players-page/spec.md` para que su requisito de ícono genérico quede complementado por la spec 006 sin contradicciones.

### Verificación del backend

- [ ] T039 Ejecutar `./gradlew build -x test` desde `backend/` y revisar que el build no agregue salidas generadas al diff.
- [ ] T040 Solicitar al usuario la salida de las pruebas de backend listadas en `specs/006-player-images/quickstart.md` y registrar los fallos preexistentes ajenos al cambio.

### Verificación del frontend

- [ ] T041 Ejecutar `npm run build` desde `frontend/` y revisar que el build no agregue salidas generadas al diff.

### Cierre documental

- [ ] T042 Revisar el diff final con `git diff --check` y `git status --short`.

## Dependencias y orden de ejecución

- **Fase 1**: sin dependencias; puede iniciar de inmediato.
- **Fase 2**: depende de la Fase 1 y bloquea todas las historias. T007 requiere su test en T018 antes de avanzar a la Fase 4.
- **Fases 3, 4 y 5**: dependen de la Fase 2. US1 es el MVP y puede validarse sin ninguna llamada a TheSportsDB; US2 y US3 requieren la persistencia de la imagen de la Fase 2.
- **Fase 6**: depende de las historias deseadas.

### Dependencias entre historias

- **US1 (P1)**: sin dependencias de otras historias.
- **US2 (P1)**: usa la imagen persistida de la Fase 2; no requiere US1.
- **US3 (P2)**: amplía el comportamiento de US2; puede validarse de forma aislada con el Service y la Integration.

### Dentro de cada historia

- Los tests se escriben antes de la implementación y deben fallar por la conducta faltante.
- Model antes que Services; Services antes que endpoints.
- La Integration, su control de ritmo y su validación de URL preceden a la persistencia del resultado.
- `ApplicationException` (T026) debe existir antes de las excepciones de aplicación de T030.

## Oportunidades de paralelismo

- Las tareas `[P]` de las Fases 1 y 2 pueden ejecutarse en paralelo solo si no comparten archivo y ya se cumplieron sus dependencias.
- En la Fase 4, las tareas backend en archivos distintos pueden avanzar en paralelo tras cumplir sus dependencias; el Service depende de la Integration, que encapsula el control de ritmo.
- Dentro de US1, las tareas de DTO backend y de tipos frontend pueden avanzar en paralelo tras la Fase 2; US2 puede avanzar en paralelo con US1 en archivos separados.

## Ejemplo de paralelismo: Historia de usuario 2

```text
Task: "Crear TheSportsDbProperties y enlazarlo en application.properties"
Task: "Crear TheSportsDbClientConfig con RestClient seguro"
Task: "Validar la URL de imagen en TheSportsDbIntegration"
Task: "Crear el DTO de respuesta de la ejecución y ampliar el Mapper"
```

Ninguna de estas cuatro tareas comparte archivo entre sí.

## Estrategia de implementación

### MVP primero (US1)

1. Completar Fases 1 y 2.
2. Completar la Fase 3.
3. **Detenerse y validar**: la tarjeta muestra retrato o ícono genérico sin llamadas externas.

### Entrega incremental

1. Fases 1 y 2 completadas.
2. US1: validar y mostrar.
3. US2: validar la sincronización manual con cuota y `409`.
4. US3: validar reanudación y `429`.
5. Fase 6: contrato, documentación y verificación.

## Notas

- `[P]` indica archivos distintos sin dependencias pendientes.
- La etiqueta `[US]` vincula cada tarea con su historia para trazabilidad.
- Los IDs conservan su numeración original; los huecos corresponden a tareas eliminadas al consolidar la normalización y el ritmo dentro de `TheSportsDbIntegration`.
- Los tests de frontend quedan fuera de alcance por la constitución §6.
- El agente no ejecuta tests; los del backend los ejecuta el usuario. La comprobación de código frontend usa `npm run build` en `frontend/`; la de backend usa `./gradlew build -x test` en `backend/`.
- Evitar ampliar el alcance a fuentes de imágenes adicionales, publicación pública o estadísticas reales.

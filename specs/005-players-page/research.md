# Investigación: Página de catálogo de jugadores

## Decisiones

### Consumir el catálogo existente sin cambios backend

- **Decision**: Consumir `GET /api/players?page={page}&size=12`, usando índice de página desde cero y autenticación Bearer gestionada por la infraestructura actual.
- **Rationale**: El endpoint ya devuelve los campos y metadatos requeridos, limita el tamaño entre 1 y 100, ordena establemente por identificador y solo incluye jugadores activos. La feature es de visualización y paginación.
- **Alternatives considered**: Crear un endpoint específico de UI o ampliar la respuesta con estadísticas y estado. Se descartan porque duplicarían el contrato y excederían el alcance.

### Encapsular la funcionalidad en una feature frontend

- **Decision**: Crear `frontend/src/features/players` con Page, Components, Query Hook, Page Hook, Service, Mapper, DTO, Model, constantes, utilidades y API pública según las responsabilidades efectivamente necesarias.
- **Rationale**: Respeta la arquitectura por features y el flujo obligatorio `Page/Component → Hook → Service`; mantiene HTTP y DTO fuera de la UI y permite que `app` solo componga la ruta.
- **Alternatives considered**: Implementar la página directamente en el router o reutilizar archivos de `auth`. Se descartan por mezclar composición global con lógica funcional o crear dependencia entre features.

### Reutilizar Axios, TanStack Query y el ciclo de sesión

- **Decision**: El Service usará el cliente Axios existente con `authenticated: true` y `AbortSignal`; el Query Hook tendrá una clave que incluya página y tamaño. El `401` seguirá finalizando la sesión mediante el interceptor y el guard dirigirá a `/login`.
- **Rationale**: Evita duplicar credenciales, caché, cancelación y navegación de sesión. Una clave por selección impide que una respuesta de otra página se presente como la actual, y la señal cancela trabajo obsoleto.
- **Alternatives considered**: Estado remoto en Zustand, `fetch` directo o redirección manual desde la feature. Se descartan por duplicar infraestructura y quebrar sus fronteras.

### Coordinar la página seleccionada en un Page Hook

- **Decision**: Mantener un índice de página seleccionado desde cero en un Page Hook que componga la query, exponga numeración de UI desde uno y corrija a `totalPages - 1` cuando una página válida al solicitarse quede fuera del total vigente. Si `totalPages` es cero, se conserva el estado vacío.
- **Rationale**: La corrección requiere coordinar estado local, resultado remoto y una segunda consulta sin trasladar esa responsabilidad al Service o a la Page. El backend acepta páginas fuera de rango y devuelve contenido vacío, por lo que la corrección corresponde al consumidor.
- **Alternatives considered**: Limitar solo los botones o modificar el backend. Los botones no cubren cambios concurrentes del total y modificar el endpoint está fuera de alcance.

### Mantener los filtros como estado visual aislado

- **Decision**: Búsqueda, liga y posición tendrán estado controlado local, pero sus valores no formarán parte de query keys, parámetros HTTP ni transformación del catálogo.
- **Rationale**: Permite interacción visual y accesible sin infringir el requisito que prohíbe filtrar, ordenar, consultar o cambiar página en esta feature.
- **Alternatives considered**: Controles deshabilitados o filtrado cliente. Se descartan porque los controles deben ser interactivos y los resultados deben permanecer inalterados.

### Separar DTO, Model y datos de presentación

- **Decision**: Modelar exactamente la respuesta HTTP en DTO, transformarla a `Player` y `PlayersPage`, e inferir `active: true` en el Mapper porque el catálogo solo devuelve activos. La tarjeta agrupa el equipo real del jugador con tres estadísticas de presentación etiquetadas como demostración; solo las estadísticas y sus etiquetas son datos de presentación, no campos del DTO.

  **Revisión 2026-10-13**: esas estadísticas dejaron de ser constantes `24`/`8`/`5` y pasaron a derivarse de forma determinista del identificador del jugador mediante el generador compartido con el detalle. La decisión satisface el requisito de coherencia entre catálogo y detalle, pero obliga a revisar el supuesto de unicidad: dos jugadores pueden coincidir en una métrica concreta, aunque la combinación de sus valores tienda a diferir.
- **Rationale**: Conserva el contrato real, hace explícito que el equipo procede del backend y que los valores numéricos son comunes a todas las tarjetas. Las etiquetas y el contraste de los textos respecto del fondo mantienen identificable cada atributo.
- **Alternatives considered**: Reutilizar DTO como Model o añadir estadísticas al contrato. Ambas opciones contradicen la arquitectura o falsean el origen de los datos.

### Tratar liga y posición como valores abiertos con fallback

- **Decision**: Conservar `league` y `position` como strings del backend y aplicar mappings visuales para las cinco ligas y cuatro posiciones conocidas; cualquier valor desconocido mantiene su texto y una apariencia neutra.

  **Revisión 2026-10-13 — decisión reemplazada parcialmente**: la liga conserva su recurso gráfico y su texto, pero la posición pasa a identificarse únicamente mediante etiqueta textual (SPEC-005 FR-009). El valor persistido sigue siendo un string abierto y nunca se descarta: el texto es la representación completa y accesible.
- **Rationale**: El contrato no define enums cerrados y la spec exige legibilidad cuando falta un recurso reconocido.
- **Alternatives considered**: Cast directo a unions cerradas u ocultar datos desconocidos. Se descartan por inseguridad tipada y pérdida de información.

### Incorporar recursos dentro de la feature

- **Decision**: Copiar en implementación los recursos runtime necesarios desde `referencias-imagenes/` a `frontend/src/features/players/assets/`; `Card Player.png` se usa solo como referencia visual. Las imágenes de clasificación conservarán texto visible o alternativa accesible.
- **Rationale**: Vite procesa de forma fiable recursos bajo `src`, la pertenencia es específica de la feature y color o imagen no deben ser la única fuente de información.
- **Alternatives considered**: Importar desde la carpeta externa o colocar recursos en `shared`. La primera no pertenece al árbol servido y la segunda anticipa una reutilización inexistente.

### Diseñar una cuadrícula exclusivamente de escritorio

- **Decision**: Optimizar la composición para un viewport mínimo de 1280×720, con cuatro columnas, hasta tres filas, sin scroll horizontal y con truncado o ajuste seguro de nombres largos. No se diseñará navegación táctil ni layout móvil.

  **Revisión 2026-10-13 — decisión reemplazada**: la página pasa a ser responsiva y se reorganiza desde cuatro columnas en escritorio hasta una columna en móvil, sin desplazamiento horizontal. Este cambio corrige una divergencia con `tp/enunciado.md` §11, que exige una aplicación web responsiva, y habilita además el uso de la navegación desplegable compartida.
- **Rationale**: Es el alcance explícito de la spec. El indicador de estado se revelará solo con `hover`, sin desplazar la cuadrícula ni ocultar datos esenciales.

  **Revisión 2026-10-13 — decisión reemplazada**: el hover produce únicamente un efecto de elevación y refuerzo de brillo (SPEC-005 FR-010). El indicador lateral quedó derogado porque no se ejecuta en pantallas táctiles, incoherente con el catálogo responsive, y porque el estado `Activo` ya es permanente por FR-011. Alternativas descartadas en su día y ahora reconsideradas: la activación por `focus` se mantiene válida para el recorrido por teclado.
- **Alternatives considered**: Grid responsive móvil o activación por click/focus. Se descartan porque ampliarían plataformas e interacciones fuera del contrato acordado.

### No incorporar tests ni dependencias

- **Decision**: No crear ni ejecutar tests frontend ni añadir infraestructura de testing. La comprobación automatizada permitida durante implementación será `npm run build`; los escenarios funcionales se validarán manualmente con `quickstart.md`.
- **Rationale**: Es una regla normativa expresa del repositorio y el stack actual basta para implementar la feature.
- **Alternatives considered**: Añadir Vitest/Testing Library. Se descarta por incumplir la gobernanza y ampliar el inventario tecnológico.

## Clarificaciones resueltas

No quedan decisiones técnicas marcadas como `NEEDS CLARIFICATION`. La URL relativa del Service será `/players`, asumiendo que `VITE_API_URL` incluye el prefijo `/api`, igual que en la configuración actual; `quickstart.md` hace explícito este requisito operativo.

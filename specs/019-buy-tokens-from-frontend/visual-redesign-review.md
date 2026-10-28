# Rediseño visual — alcance y evidencia

**Entrega parcial consolidada:** [implementation-report.md](implementation-report.md). Integración real de cotizaciones, escenarios humanos y aceptación visual pendientes; sin compra E2E ni cierre acreditados. [Trabajo requerido/futuro](future-work.md) distingue requisitos de cierre de controles visuales excluidos de alcance funcional. No se reescriben las evidencias históricas siguientes para presentarlas como verificaciones nuevas.

## Encargo posterior: scroll contenido en listado

El usuario autoriza ajustar scroll exclusivamente en `/market/buy`: navbar, encabezado, métricas, filtros y paginación estáticos, main de jugadores desplazable ocupando espacio restante. Autoriza posteriormente filtros plegables nativos en móvil y métricas compactas. Prohíbe cambios de lógica/HTTP y afectar scroll de otras rutas. Tasks S-01–S-03, criterio «Ajuste de scroll autorizado» de spec y diseño localizado de plan. No acredita revisión de código ni visual por el usuario.

### Preparación y resultado del ajuste

Prerrequisitos de ANALYZE resueltos por el script real para SPEC-019. Contraste spec/plan/tasks: alcance de contención de ruta cubierto por S-01, main desplazable y móvil plegable por S-02, verificación por S-03; sin cambios de contratos, sin tareas nuevas sin alcance, sin contradicciones bloqueantes en este ajuste. La decisión explícita de scroll exclusivo sustituye la disposición móvil de scroll natural solamente para mercado; las demás rutas conservan sus reglas. Sin nuevos hooks de extensión ni aprobación humana inferida.

Archivos del ajuste:

- `app/layouts/AppLayout/AppLayout.tsx` y SCSS: modificador activo únicamente para `/market/buy` (incluida barra final). Contención `100dvh` y overflow hidden posteriores a los breakpoints móviles; selectores html/body condicionados a ese modificador mediante `:has`, sin efectos JS, estilos inline ni necesidad de cleanup. El navbar mantiene su altura intrínseca y flex descuenta el espacio ocupado. No se modifica PageLayout compartido.
- `features/market/pages/MarketBuyPage/MarketBuyPage.tsx` y SCSS: encabezado/footer no encogen; contenido flex/grid con min-height:0; **solo main** tiene overflow-y:auto, scrollbar estable y overscroll contenido. Sale PageLayout como contenedor desplazable de toda la zona. El modal mantiene su portal y no cambia.
- `MarketHeader.module.scss`: métricas móviles compactas en tres columnas, sin cambiar datos.
- `MarketFilters.tsx` y SCSS: escritorio estático, móvil con details/summary nativo y panel superpuesto al listado; dos representaciones responsivas del mismo render de controles deshabilitados, con nombres de grupos de radio distintos. Sin lógica funcional nueva. Sin cambios a columnas de cards ni reglas de compra/paginación/HTTP.

`npm run build`: satisfactorio (TypeScript/Vite, 2477 módulos); advertencia de bundle de aproximadamente 645 kB, también presente en el build anterior. `git diff --check` conserva los tres avisos preexistentes de líneas vacías al final de barrels; no se corrigen cambios ajenos. No se ejecutaron tests, compras, commits, push ni merges.

**S-03 incompleta:** el navegador no tiene pestañas disponibles y la apertura de localhost/127.0.0.1 devuelve `ERR_FAILED`. No se verificaron visualmente 375/768/1440px ni se acreditó regresión de otras rutas. Especial atención pendiente al panel móvil expandido en viewports de poca altura/zoom y a la altura intrínseca del navbar. No declarar ausencia de recortes, fidelidad final ni cierre basándose solo en CSS/build.

## Autorización humana

Solicitud del usuario en esta sesión: «Rediseñá /market/buy» con máxima fidelidad a referencias 04/05, prioridad visual, filtros únicamente visuales, conservar lógica/HTTP de compra, sin lógica de players/portfolio/backend, sin datos ficticios, commits, push ni merges. Sustituye la ampliación funcional de filtros anterior. Controles nuevos sin lógica: deshabilitados y claramente pendientes.

## ANALYZE previo al código

Operación conforme a `.opencode/commands/speckit.analyze.md`, estrictamente de lectura durante el análisis. Prerrequisitos resueltos mediante `bash .specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks` (el archivo no tiene permiso ejecutable). FEATURE_DIR corresponde a SPEC-019. No existe `.specify/extensions.yml`; sin hooks.

| Alcance | Tareas | Cobertura |
| --- | --- | --- |
| VR-01 composición/fondo/métricas/cards | V-01, V-02 | Documentada |
| VR-02 controles visuales pendientes | V-01 | Documentada |
| VR-03 modal/overlay y compra existente | V-03 | Documentada |
| VR-04 imágenes/responsive/verificación | V-02, V-03, V-04 | Documentada |
| FR-01–FR-15 conservados | T-00–T-19 + V-01–V-04 | Sin cambio de contratos |

Resultado: cero hallazgos bloqueantes para el alcance visual; cobertura VR 4/4, cuatro tareas nuevas, sin tareas nuevas sin requisito, sin conflictos constitucionales. La sección de alcance vigente sustituye expresamente FR-16–FR-18/escenarios funcionales y la investigación de catálogo completo; no se implementan. Se conservan los marcadores históricos de checklists (arquitectura 9/9, API 8/8, UX 8/8); no equivalen a aprobar esta implementación. Los criterios de aislamiento/props/queries visibles permanecen aplicables y no se amplían APIs.

Observación editorial: los artefactos conservan antecedentes que mencionan filtros funcionales, señalados explícitamente como sustituidos por la decisión posterior. No habilitan trabajo fuera del rediseño.

Siguiente acción permitida: implementar únicamente V-01–V-03 bajo la autorización humana anterior y ejecutar V-04. No aprobar CODE REVIEW/TESTING/cierre; no afirmar sincronización con seguimiento remoto no consultado.

## Entrega parcial de implementación

- **MarketBuyPage**: wrapper oscuro, PageLayout existente con encabezado y footer fuera del scroll de contenido; grid de tres columnas en escritorio y adaptación a tablet/móvil. Sin cambios a AppLayout/Navbar.
- **MarketHeader/**: encabezado/icono y tres métricas; total real del catálogo, saldo y órdenes «No disponible» por falta de fuente conectada.
- **MarketFilters/**: panel completo de referencia (búsqueda, liga, posición, precio, orden, dirección y aplicar/limpiar), deshabilitado y rotulado pendiente, sin lógica de filtros.
- **MarketPlayerSummary/**: identidad compacta compartida localmente por ficha/modal, badge de posición real, iconos genéricos equipo/liga, estadísticas «Sin datos» y favorito deshabilitado. Retratos externos del catálogo con fallback local final `shared/assets/Player Generic-Icon.png`, empaquetado por Vite; no se realizan nuevas consultas a proveedores externos.
- **MarketPlayerCard/**: precio real, variación «Sin datos», botón verde; diferencia textual entre error de consulta y ausencia de quote sin cambiar HTTP.
- **MarketPagination/**: contador real y composición de Pagination compartido; tamaño actual 12 conservado, selector deshabilitado/pendiente. No duplica lógica de paginación.
- **BuyTokenModal/**: composición 05, liga/posición, ficha, precio real, resumen, campo editable y confirmación originales. −/+ y accesos rápidos deshabilitados/pendientes; spinner visual durante submit. Service/Mapper/Query/Mutation/Page Hook/formulario no modificados.
- **app/market/MarketBuyRoute/MarketBuyRoute.tsx**: pasa metadatos reales `totalElements/size` a UI; sin cambiar consulta, mutación ni invalidación.
- **shared/components/Modal/**: overlay sin blur, scroll/overscroll propios y título accesible con CSS local (la clase global anterior `sr-only` no estaba definida); foco inicial/restauración con `preventScroll`.
- **shared/components/Pagination/**: `className` opcional y variables CSS de superficies/bordes/activo, con defaults existentes preservados; sin cambio de navegación ni cálculo de páginas.
- **shared/icons/index.ts**: nuevos iconos utilizados por la UI, a través de la API central existente. Sin dependencias nuevas.
- **Barrels locales de market**: props de Components locales pasan a interfaces privadas según convenciones; API externa de market conserva Page y Model.
- **Spec/plan/research/tasks y este informe**: registran sustitución del alcance funcional previo por visual y evidencia real; checklists del revisor intactos.

### Verificaciones observadas

`npm run build` desde frontend: satisfactorio tras la última modificación de código (TypeScript y Vite, 2477 módulos). Advertencia de chunk JS de aproximadamente 644 kB; no es fallo de compilación y no se abordó con cambios de arquitectura ajenos al rediseño. Sin tests ni compras reales, commits, push o merges.

Inspección browser: `/market/buy` inicialmente redirigió a login. El usuario confirmó inicio de sesión; después la herramienta no expuso pestañas (`tabs: []`) y la apertura devolvió `ERR_FAILED`. El puerto local 5173 seguía en escucha. **No hay evidencia visual autenticada ni capturas 375/768/1440: V-04 permanece pendiente y no se declara la tarea terminada.** No se inventó sesión ni se desactivó AuthGuard.

`git diff --check`: detecta únicamente las tres líneas vacías finales ya observadas antes de esta tarea en `features/players/index.ts`, `features/portfolio/index.ts` y `shared/components/index.ts`. Se preservaron los cambios previos. Sin cambios nuevos de lógica de players/portfolio/backend ni edición de salidas generadas.

### Diferencias conocidas y próximas comprobaciones

- Se conservan 12 jugadores por página frente a los seis de la referencia, para no cambiar la lógica del catálogo. El contenido puede desplazarse dentro del viewport; se debe comprobar footer/nav y ausencia de recortes reales.
- Saldo/órdenes/estadísticas/variación carecen de fuente real conectada y no reproducen cifras ni gráficos del mockup; muestran ausencia explícita.
- Iconos genéricos de equipo/liga en lugar de escudos/banderas no provistos por el Model; favoritos/filtros/selector y accesos rápidos pendientes, sin simular éxito.
- Navbar existente sin alterar pestañas ni habilitar venta/órdenes/historial ajenos al alcance.
- Pendiente comparar proporciones, tipografía, colores y espaciado en ambos mockups con aplicación autenticada; inspeccionar foco, imagen fallback, modal sin reflow y scroll a los tres anchos. Los estilos y build no acreditan esa validación.

Orden de revisión recomendado: Page/composición → Header/Filters → Summary/Card → Pagination → Modal → API de estilos compartida → diff de documentación. Aprobaciones humanas de código, resultado visual y cierre permanecen pendientes; no se afirma estado ni sincronización remota de GitHub Projects.
